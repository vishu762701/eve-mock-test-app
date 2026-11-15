#!/usr/bin/env python3
"""
EVE Repository Anti-Shortcut & Anti-Wrapper Validator.
Enforces LAW 7 (Zero-Shortcut & Anti-Wrapper Enforcement) across all code, layouts, and governance.

Checks:
1. Governance files contain Laws 5, 6, 7, 8, and 9 (LAW 7, ZERO-SHORTCUT, check_no_shortcuts.py).
2. Layouts under app/src/main/res/layout/ contain zero occurrences of DesignCardView, MaterialCardView, UiArc,
   EveGlassToolbar, DesignGlassLayout, DesignInputLayout, DesignSegmentedTabs, DesignSpinner,
   DesignAutoComplete, DesignIconActionButton, DesignLoadingView, DesignAccuracyRing, MaterialButton,
   MaterialToolbar, AppBarLayout, TextInputLayout, TextInputEditText, CircularProgressIndicator,
   Chip, TabItem, or ShapeableImageView.
3. Strict ban on typealias aliasing (typealias Telegram* = UiArc* / Design*).
4. Strict ban on any Telegram*.kt from extending, referencing, or importing UiArc* or DesignCardView.
"""

import argparse
import os
import re
import sys
from pathlib import Path

GOVERNANCE_FILES = [
    "ENGINEERING_STANDARD.md",
    "AGENTS.md",
    "SOURCE_OF_TRUTH.md",
    "GEMINI.md",
    "CLAUDE.md",
]

OPTIONAL_GOVERNANCE_FILES = [
    "docs/EVE_ONE_TIME_CHAT_STARTER.md",
]

REQUIRED_GOVERNANCE_TERMS = [
    ("Law 5 / LAW 5", re.compile(r"\b(?:LAW|Law)\s*5\b")),
    ("Law 6 / LAW 6", re.compile(r"\b(?:LAW|Law)\s*6\b")),
    ("Law 7 / LAW 7", re.compile(r"\b(?:LAW|Law)\s*7\b")),
    ("Law 8 / LAW 8", re.compile(r"\b(?:LAW|Law)\s*8\b")),
    ("Law 9 / LAW 9", re.compile(r"\b(?:LAW|Law)\s*9\b")),
    ("ZERO-SHORTCUT", re.compile(r"ZERO-SHORTCUT")),
    ("check_no_shortcuts.py", re.compile(r"check_no_shortcuts\.py")),
]

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


def check_governance(repo_root: Path) -> list[str]:
    violations = []
    for rel_path in GOVERNANCE_FILES:
        file_path = repo_root / rel_path
        if not file_path.is_file():
            violations.append(f"Missing required governance file: {rel_path}")
            continue
        content = file_path.read_text(encoding="utf-8")
        for label, pattern in REQUIRED_GOVERNANCE_TERMS:
            if not pattern.search(content):
                violations.append(f"{rel_path}: Missing required rule identifier '{label}'")

    for rel_path in OPTIONAL_GOVERNANCE_FILES:
        file_path = repo_root / rel_path
        if file_path.is_file():
            content = file_path.read_text(encoding="utf-8")
            for label, pattern in REQUIRED_GOVERNANCE_TERMS:
                if not pattern.search(content):
                    violations.append(f"{rel_path}: Missing required rule identifier '{label}'")
    return violations


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

    for kt_file in src_dir.rglob("*.kt"):
        content = kt_file.read_text(encoding="utf-8")
        # 1. Ban typealias aliasing
        if TYPEALIAS_PATTERN.search(content):
            violations.append(f"Forbidden typealias shortcut in {kt_file.relative_to(repo_root)}")

        # 2. In strict mode, ban any Telegram*.kt from extending or importing UiArc* / DesignCardView
        if strict and kt_file.name.startswith("Telegram"):
            if STRICT_WRAPPER_PATTERN.search(content):
                violations.append(
                    f"Strict anti-shortcut violation in {kt_file.relative_to(repo_root)}: "
                    f"Telegram class must not subclass legacy UiArc* or DesignCardView"
                )
            if IMPORT_UIARC_PATTERN.search(content):
                violations.append(
                    f"Strict anti-shortcut violation in {kt_file.relative_to(repo_root)}: "
                    f"Telegram class must not import legacy UiArc* classes"
                )

    return violations


def main():
    parser = argparse.ArgumentParser(description="EVE Anti-Shortcut and Anti-Wrapper Validator (Law 7)")
    parser.add_argument(
        "--check-governance",
        "--governance-only",
        dest="governance_only",
        action="store_true",
        help="Validate Laws 5, 6, 7, 8, and 9 enshrinement in governance files"
    )
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

    if args.governance_only:
        all_violations.extend(check_governance(repo_root))
    else:
        # Default run: checks governance, layouts, and strict source shortcuts
        all_violations.extend(check_governance(repo_root))
        all_violations.extend(check_layouts(repo_root, legacy_widgets=not args.basic_layouts_only))
        all_violations.extend(check_source_shortcuts(repo_root, strict=args.strict))

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
