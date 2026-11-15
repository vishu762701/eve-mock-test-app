#!/usr/bin/env python3
"""
EVE Repository Anti-Shortcut & Anti-Wrapper Validator.
Enforces LAW 7 (Zero-Shortcut & Anti-Wrapper Enforcement) across all code, layouts, and governance.

Checks:
1. Governance files contain Law 5, Law 6, and Law 7 (LAW 7, ZERO-SHORTCUT, check_no_shortcuts.py).
2. Layouts under app/src/main/res/layout/ contain zero occurrences of DesignCardView, MaterialCardView, or UiArc.
3. Strict ban on typealias aliasing (typealias Telegram* = UiArc* / Design*).
4. In --strict mode, bans any Telegram*.kt from extending, referencing, or importing UiArc* or DesignCardView.
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
    ("ZERO-SHORTCUT", re.compile(r"ZERO-SHORTCUT")),
    ("check_no_shortcuts.py", re.compile(r"check_no_shortcuts\.py")),
]

TYPEALIAS_PATTERN = re.compile(r"typealias\s+Telegram\w*\s*=\s*(?:UiArc|Design|Material)\w*")
STRICT_WRAPPER_PATTERN = re.compile(r"class\s+Telegram\w*[\s\S]*?:\s*(?:UiArc|DesignCardView)\w*")
IMPORT_UIARC_PATTERN = re.compile(r"import\s+com\.eve\.app\.(?:designsystem|ui\.\w+)\.UiArc\w*")


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


def check_layouts(repo_root: Path) -> list[str]:
    violations = []
    layout_dir = repo_root / "app" / "src" / "main" / "res" / "layout"
    if not layout_dir.is_dir():
        return violations

    shortcut_pattern = re.compile(r"DesignCardView|MaterialCardView|UiArc")
    for xml_file in layout_dir.glob("*.xml"):
        content = xml_file.read_text(encoding="utf-8")
        matches = shortcut_pattern.findall(content)
        if matches:
            violations.append(f"Layout shortcut detected in {xml_file.name}: found {set(matches)}")
    return violations


def check_source_shortcuts(repo_root: Path, strict: bool = False) -> list[str]:
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
    parser.add_argument("--check-governance", action="store_true", help="Validate Law 5, Law 6, and Law 7 enshrinement in governance files")
    parser.add_argument("--strict", action="store_true", help="Enforce strict ban on any Telegram*.kt referencing or subclassing UiArc*")
    parser.add_argument("--root", type=str, default=".", help="Root directory of the repository")

    args = parser.parse_args()
    repo_root = Path(args.root).resolve()

    all_violations = []

    if args.check_governance:
        all_violations.extend(check_governance(repo_root))
    else:
        # Default run: checks governance, layouts, and source shortcuts
        all_violations.extend(check_governance(repo_root))
        all_violations.extend(check_layouts(repo_root))
        all_violations.extend(check_source_shortcuts(repo_root, strict=args.strict))

    if args.strict:
        # Also run strict source check if requested specifically
        all_violations.extend(check_source_shortcuts(repo_root, strict=True))

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
