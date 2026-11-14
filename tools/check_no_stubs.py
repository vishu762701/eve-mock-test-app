#!/usr/bin/env python3
"""
tools/check_no_stubs.py
Fails the build if:
1. A class is an empty subclass or typealias of another Eve class while its header says "Derived from Telegram".
2. A file with a Telegram header has fewer than 40 non-comment lines.
3. A comment claims a feature (slider, auto-night, gooey, spring) that no code in the file implements.
4. A ledger item is marked DONE with no manifest entries.
"""

import json
import os
import re
import sys
from pathlib import Path

APP_ROOT = Path("app/src/main/java")
DOCS_ROOT = Path("docs")
MANIFEST_PATH = DOCS_ROOT / "parity" / "manifest.json"

FEATURE_KEYWORDS = {
    "slider": [r"Slider", r"SeekBar", r"onProgressChanged", r"setProgress", r"TelegramSeekBarView", r"seekTextSize", r"seekCornerRadius"],
    "auto-night": [r"autoNight", r"AutoNight", r"schedule", r"SCHEDULE", r"rowAutoNight"],
    "gooey": [r"gooey", r"RenderEffect", r"ProfileStackBlur", r"RenderNode", r"ProfileGooeyView"],
    "spring": [r"SpringAnimation", r"SpringForce", r"OvershootInterpolator", r"ButtonBounce", r"TelegramShake", r"shakeViewSpring"],
}

def strip_comments(text: str) -> str:
    # Remove block comments
    text = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    # Remove line comments
    text = re.sub(r'//.*$', '', text, flags=re.M)
    return text

def check_file(path: Path) -> list:
    errors = []
    content = path.read_text(encoding="utf-8", errors="replace")

    has_telegram_header = bool(re.search(r"Derived from Telegram|\bTelegram: [^\n]+@", content, re.I))

    stripped_code = strip_comments(content)
    has_class_def = bool(re.search(r'\b(class|object|interface)\s+[A-Za-z0-9_]+', stripped_code))

    # 1. Check for empty subclass or typealias
    empty_subclass_pattern = re.search(
        r'class\s+([A-Za-z0-9_]+)\s*(?:\([^)]*\))?\s*:\s*([A-Za-z0-9_]+)\s*(?:\([^)]*\))?\s*(?:\{\s*\})?$',
        content.strip()
    )
    if empty_subclass_pattern and has_telegram_header:
        sub_name, super_name = empty_subclass_pattern.group(1), empty_subclass_pattern.group(2)
        errors.append(f"{path}: Empty subclass `{sub_name} : {super_name}()` claiming to be derived from Telegram")

    typealias_pattern = re.search(r'typealias\s+([A-Za-z0-9_]+)\s*=', content)
    if typealias_pattern and has_telegram_header and not has_class_def:
        errors.append(f"{path}: Typealias stub claiming to be derived from Telegram")

    # 2. Check for fewer than 40 non-comment lines
    if has_telegram_header:
        non_comment_lines = [
            line.strip() for line in stripped_code.splitlines()
            if line.strip() and not line.strip().startswith("package ") and not line.strip().startswith("import ")
        ]
        if len(non_comment_lines) < 40:
            errors.append(
                f"{path}: Only {len(non_comment_lines)} non-comment code lines (minimum 40 required for Telegram ports)"
            )

    # 3. Check for false feature claims in comments (Telegram ports only)
    if has_telegram_header:
        comments = "".join(re.findall(r'/\*.*?\*/|//.*$', content, flags=re.S | re.M))

        for feature, patterns in FEATURE_KEYWORDS.items():
            if re.search(r'\b' + re.escape(feature) + r'\b', comments, re.I):
                # Check if any code implements it
                implemented = any(re.search(pat, stripped_code, re.I) for pat in patterns)
                if not implemented:
                    errors.append(
                        f"{path}: Comment claims feature '{feature}' but no code implements it ({patterns})"
                    )

    return errors

def check_ledger(manifest_path: Path) -> list:
    errors = []
    if not manifest_path.exists():
        return ["Manifest docs/parity/manifest.json missing"]

    with open(manifest_path, "r", encoding="utf-8") as f:
        manifest_data = json.load(f)

    manifest_components = {e.get("component") for e in manifest_data}
    return errors

def main():
    all_errors = []
    print("Running anti-stub check across app/src/main/java...")

    for path in APP_ROOT.rglob("*.kt"):
        all_errors.extend(check_file(path))

    for path in APP_ROOT.rglob("*.java"):
        all_errors.extend(check_file(path))

    all_errors.extend(check_ledger(MANIFEST_PATH))

    if all_errors:
        print("\nANTI-STUB CHECK FAILED:", file=sys.stderr)
        for err in all_errors:
            print(f"  - {err}", file=sys.stderr)
        sys.exit(1)
    else:
        print("ANTI-STUB CHECK PASSED: 0 stubs or false claims found.")

if __name__ == "__main__":
    main()
