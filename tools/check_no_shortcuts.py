#!/usr/bin/env python3
"""
EVE Repository Anti-Shortcut & Anti-Wrapper Validator.
Enforces LAW 7 (Zero-Shortcut & Anti-Wrapper Enforcement) across all code, layouts, and governance.

Checks:
1. Layouts under app/src/main/res/layout/ contain zero occurrences of legacy widgets.
2. Strict ban on typealias aliasing (typealias Telegram* = UiArc* / Design*).
3. Strict ban on any Telegram*.kt from extending, referencing, or importing UiArc* or DesignCardView.
4. Fail if a Telegram* class only extends/forwards to another Telegram* or Eve class (fake wrapper).
5. Fail if a file under app/src/main/java/org/telegram has no entry in
   docs/telegram-verbatim-imports.json and no "// EVE-SHIM" header.
6. Fail if two classes share the simple name CubicBezierInterpolator, LayoutHelper, or BackDrawable.
"""

import argparse
import json
import os
import re
import sys
from pathlib import Path

TYPEALIAS_PATTERN = re.compile(r"typealias\s+Telegram\w*\s*=\s*(?:UiArc|Design|Material)\w*")
STRICT_WRAPPER_PATTERN = re.compile(r"class\s+Telegram\w*[\s\S]*?:\s*(?:UiArc|DesignCardView)\w*")
IMPORT_UIARC_PATTERN = re.compile(r"import\s+com\.eve\.app\.(?:designsystem|ui\.\w+)\.UiArc\w*")

LEGACY_LAYOUT_PATTERN = re.compile(
    r"</?(?:\w+\.)*(?:DesignCardView|MaterialCardView|UiArc\w*|EveGlassToolbar|DesignGlassLayout|"
    r"DesignInputLayout|DesignSegmentedTabs|DesignSpinner|DesignAutoComplete|"
    r"DesignIconActionButton|DesignLoadingView|DesignAccuracyRing|MaterialButton|"
    r"MaterialToolbar|AppBarLayout|TextInputLayout|TextInputEditText|"
    r"CircularProgressIndicator|Chip|TabItem|ShapeableImageView)\b"
)
BASIC_LAYOUT_PATTERN = re.compile(r"DesignCardView|MaterialCardView|UiArc")

PROTECTED_SIMPLE_NAMES = {"CubicBezierInterpolator", "LayoutHelper", "BackDrawable"}


def check_layouts(repo_root: Path, legacy_widgets: bool = True) -> list[str]:
    violations = []
    layout_dir = repo_root / "app" / "src" / "main" / "res" / "layout"
    if not layout_dir.is_dir():
        return violations

    shortcut_pattern = LEGACY_LAYOUT_PATTERN if legacy_widgets else BASIC_LAYOUT_PATTERN
    for xml_file in layout_dir.glob("*.xml"):
        content = xml_file.read_text(encoding="utf-8")
        matches = shortcut_pattern.findall(content)
        if matches:
            violations.append(f"Layout shortcut detected in {xml_file.name}: found {set(matches)}")
    return violations


def check_source_shortcuts(repo_root: Path, strict: bool = True) -> list[str]:
    violations = []
    src_dir = repo_root / "app" / "src" / "main" / "java"
    if not src_dir.is_dir():
        return violations

    for code_file in sorted(list(src_dir.rglob("*.kt")) + list(src_dir.rglob("*.java"))):
        content = code_file.read_text(encoding="utf-8")
        if code_file.suffix == ".kt":
            # 1. Ban typealias aliasing
            if TYPEALIAS_PATTERN.search(content):
                violations.append(f"Forbidden typealias shortcut in {code_file.relative_to(repo_root)}")

            # 2. In strict mode, ban any Telegram*.kt from extending or importing UiArc* / DesignCardView
            if strict and code_file.name.startswith("Telegram"):
                if STRICT_WRAPPER_PATTERN.search(content):
                    violations.append(
                        f"Strict anti-shortcut violation in {code_file.relative_to(repo_root)}: "
                        f"Telegram class must not subclass legacy UiArc* or DesignCardView"
                    )
                if IMPORT_UIARC_PATTERN.search(content):
                    violations.append(
                        f"Strict anti-shortcut violation in {code_file.relative_to(repo_root)}: "
                        f"Telegram class must not import legacy UiArc* classes"
                    )

        # 3. Ban hardcoded low-contrast text colors across app/src/main/java/
        if "0xFFE53935" in content:
            violations.append(
                f"Low-contrast text color violation in {code_file.relative_to(repo_root)}: "
                f"Hardcoded 0xFFE53935 is banned; use R.color.system_red_text"
            )
        if "setTextColor(0xFF34C759.toInt())" in content:
            violations.append(
                f"Low-contrast text color violation in {code_file.relative_to(repo_root)}: "
                f"Hardcoded setTextColor(0xFF34C759.toInt()) is banned; use R.color.system_green_text"
            )

    return violations


def strip_comments(text: str) -> str:
    text = re.sub(r"//.*", "", text)
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return text


def class_declares_no_own_members(content: str, is_kotlin: bool, class_name: str) -> tuple[bool, str]:
    """
    Returns (True, parent_class) if a Telegram* class only extends another class
    and declares no member of its own (only constructors / super calls).
    """
    clean = strip_comments(content)
    if is_kotlin:
        # Match class declaration: class ClassName ... : SuperClass(...) [ { ... } ]
        m = re.search(rf"\bclass\s+{re.escape(class_name)}\b([^{{]*)(?:\{{([\s\S]*)\}})?", clean)
        if not m:
            return False, ""
        header = m.group(1) or ""
        body = m.group(2)
        if ":" not in header:
            return False, ""
        colon_part = header.split(":", 1)[1].strip()
        super_match = re.search(r"([A-Za-z0-9_.]+)", colon_part)
        parent_class = super_match.group(1) if super_match else "super class"

        # If primary constructor defines properties with val or var, those are members
        if re.search(r"\b(val|var)\b", header):
            return False, ""

        # If there is no body, it only has primary constructor forwarding to super
        if body is None:
            return True, parent_class

        # If there is a body, strip constructor(...) calls, super(...) calls, and init { ... } blocks
        body_clean = re.sub(r"\binit\s*\{[^}]*\}", "", body)
        body_clean = re.sub(r"\bconstructor\s*\([^)]*\)(?:\s*:\s*super\([^)]*\))?\s*(?:\{[^}]*\})?", "", body_clean)

        # Check if any member keywords remain: fun, val, var, class, object, interface
        if re.search(r"\b(fun|val|var|class|object|interface)\b", body_clean):
            return False, ""

        remaining = re.sub(r"[\s;{}]", "", body_clean)
        return len(remaining) == 0, parent_class
    else:
        # Java: class ClassName ... extends SuperClass ... { ... }
        m = re.search(rf"\bclass\s+{re.escape(class_name)}\b[^{{]*\bextends\s+([A-Za-z0-9_.]+)[^{{]*\{{([\s\S]*)\}}", clean)
        if not m:
            return False, ""
        parent_class = m.group(1)
        body = m.group(2)

        # Remove constructors: [public|protected|private] ClassName(...) [throws ...] { super(...); }
        ctor_pattern = rf"(?:public|protected|private)?\s*{re.escape(class_name)}\s*\([^)]*\)\s*(?:throws\s+[^{{]+)?\{{[^}}]*\}}"
        body_no_ctor = re.sub(ctor_pattern, "", body)

        remaining = re.sub(r"[\s;{}]", "", body_no_ctor)
        return len(remaining) == 0, parent_class


def check_telegram_forwarding_wrappers(repo_root: Path) -> list[str]:
    """Fail if a Telegram* class (kt or java) declares no member of its own (only constructors / super call)."""
    violations = []
    src_dir = repo_root / "app" / "src" / "main" / "java"
    if not src_dir.is_dir():
        return violations

    for code_file in sorted(list(src_dir.rglob("*.kt")) + list(src_dir.rglob("*.java"))):
        if not code_file.name.startswith("Telegram"):
            continue
        content = code_file.read_text(encoding="utf-8")
        is_kotlin = code_file.suffix == ".kt"
        class_name = code_file.stem
        no_members, parent_class = class_declares_no_own_members(content, is_kotlin, class_name)
        if no_members:
            violations.append(
                f"Telegram forwarding wrapper in {code_file.relative_to(repo_root)}: "
                f"{class_name} only extends {parent_class} declaring no members of its own (constructors only)"
            )
    return violations


def check_org_telegram_provenance(repo_root: Path) -> list[str]:
    """Fail if a file under org/telegram has no entry in telegram-verbatim-imports.json and no // EVE-SHIM."""
    violations = []
    tg_dir = repo_root / "app" / "src" / "main" / "java" / "org" / "telegram"
    if not tg_dir.is_dir():
        return violations

    manifest_path = repo_root / "docs" / "telegram-verbatim-imports.json"
    registered_eve_paths = set()
    if manifest_path.is_file():
        try:
            data = json.loads(manifest_path.read_text(encoding="utf-8"))
            for entry in data.get("imports", []):
                eve_path = entry.get("eve", "")
                if eve_path:
                    registered_eve_paths.add(eve_path)
        except (json.JSONDecodeError, KeyError):
            violations.append("docs/telegram-verbatim-imports.json is malformed")
            return violations

    for code_file in sorted(list(tg_dir.rglob("*.kt")) + list(tg_dir.rglob("*.java"))):
        rel = str(code_file.relative_to(repo_root))
        content = code_file.read_text(encoding="utf-8")
        in_manifest = rel in registered_eve_paths
        has_shim_header = "// EVE-SHIM" in content
        if not in_manifest and not has_shim_header:
            violations.append(
                f"Unregistered org/telegram file: {rel} — "
                f"must be listed in docs/telegram-verbatim-imports.json or have a // EVE-SHIM header"
            )
    return violations


def check_duplicate_simple_names(repo_root: Path) -> list[str]:
    """Fail if two source classes share the simple name CubicBezierInterpolator, LayoutHelper, or BackDrawable."""
    violations = []
    src_dir = repo_root / "app" / "src" / "main" / "java"
    if not src_dir.is_dir():
        return violations

    name_map: dict[str, list[str]] = {}
    for code_file in sorted(list(src_dir.rglob("*.kt")) + list(src_dir.rglob("*.java"))):
        simple = code_file.stem
        if simple in PROTECTED_SIMPLE_NAMES:
            rel = str(code_file.relative_to(repo_root))
            name_map.setdefault(simple, []).append(rel)

    for name, paths in name_map.items():
        if len(paths) > 1:
            violations.append(
                f"Duplicate class name '{name}' found in {len(paths)} locations: {', '.join(paths)}"
            )
    return violations


def main():
    parser = argparse.ArgumentParser(description="EVE Anti-Shortcut and Anti-Wrapper Validator (Law 7)")
    parser.add_argument(
        "--strict",
        action="store_true",
        default=True,
        help="Enforce strict ban on any Telegram*.kt referencing or subclassing UiArc* (default: True)"
    )
    parser.add_argument(
        "--no-strict",
        dest="strict",
        action="store_false",
        help="Disable strict mode for Telegram*.kt class inspection"
    )
    parser.add_argument(
        "--basic-layouts-only",
        action="store_true",
        help="Check only basic shortcuts (DesignCardView, MaterialCardView, UiArc) during intermediate migration"
    )
    parser.add_argument(
        "--root",
        type=str,
        default=".",
        help="Root directory of the repository"
    )

    args = parser.parse_args()
    repo_root = Path(args.root).resolve()

    all_violations = []
    all_violations.extend(check_layouts(repo_root, legacy_widgets=not args.basic_layouts_only))
    all_violations.extend(check_source_shortcuts(repo_root, strict=args.strict))
    all_violations.extend(check_telegram_forwarding_wrappers(repo_root))
    all_violations.extend(check_org_telegram_provenance(repo_root))
    all_violations.extend(check_duplicate_simple_names(repo_root))

    if all_violations:
        print("ANTI-SHORTCUT CHECK FAILED — VIOLATIONS FOUND:")
        for v in all_violations:
            print(f"  ❌ {v}")
        sys.exit(1)
    else:
        print("ANTI-SHORTCUT CHECK PASSED: 0 shortcuts, wrappers, or governance omissions found.")
        sys.exit(0)


if __name__ == "__main__":
    main()
