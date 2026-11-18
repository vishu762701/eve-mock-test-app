#!/usr/bin/env python3
"""
EVE Port Gate Validator and Manifest Generator.
Enforces LAW 2, LAW 3, and Telegram Porting Rules (R1–R10).

Modes:
  --record --upstream <dir> [--allow-known-debt]:
      Analyzes ported screens against the official Telegram upstream tree,
      calculates SHA-256, kept lines, and originality percentage, and writes
      docs/port-manifest.json. Exits 1 if any non-debt file has originality < 95%.
  --check:
      CI verification mode (no upstream directory required).
      Verifies that all org/telegram files are registered, hashes are current,
      originality >= 95% (or recognized as known pilot debt), host Activities
      bind the Telegram entry points in AndroidManifest.xml, and dead layouts
      are purged.
"""

import argparse
import hashlib
import json
import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = REPO_ROOT / "docs" / "port-manifest.json"

KNOWN_DEBT_ALLOWLIST = {
    "app/src/main/java/org/telegram/messenger/AndroidUtilities.java",
    "app/src/main/java/org/telegram/messenger/ApplicationLoader.java",
    "app/src/main/java/org/telegram/messenger/LocaleController.java",
    "app/src/main/java/org/telegram/messenger/R.java",
    "app/src/main/java/org/telegram/ui/ActionBar/ActionBar.java",
    "app/src/main/java/org/telegram/ui/ActionBar/ActionBarMenu.java",
    "app/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java",
    "app/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java",
    "app/src/main/java/org/telegram/ui/ActionBar/INavigationLayout.java",
    "app/src/main/java/org/telegram/ui/ActionBar/Theme.java",
    "app/src/main/java/org/telegram/ui/ActionBar/ThemeDescription.java",
    "app/src/main/java/org/telegram/ui/Components/EditTextBoldCursor.java",
}

EVE_ORIGINAL_ALLOWLIST = {
    "app/src/main/java/org/telegram/ui/Components/RLottieNative.java": "not a Telegram class; native Lottie bridge for EVE"
}

# Registered screen specifications for --record
SCREEN_SPECS = [
    {
        "screen": "ChangeNameActivity",
        "eve_host": "com.eve.app.ui.profile.ChangeNameActivity",
        "telegram_entry": "org.telegram.ui.ChangeNameActivity",
        "dead_layout_files": [
            "app/src/main/res/layout/activity_change_name.xml"
        ],
        "files": [
            {
                "eve": "app/src/main/java/org/telegram/ui/ChangeNameActivity.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ChangeNameActivity.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/LayoutHelper.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/LayoutHelper.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/CubicBezierInterpolator.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/CubicBezierInterpolator.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/Easings.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/Easings.java",
                "mode": "VERBATIM"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/RLottieNative.java",
                "upstream": "",
                "mode": "EVE-ORIGINAL",
                "removed_methods_reason": "not a Telegram class; native Lottie bridge for EVE"
            },
            # Pilot foundation shims (known debt until full foundation port)
            {
                "eve": "app/src/main/java/org/telegram/ui/Components/EditTextBoldCursor.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/EditTextBoldCursor.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/Theme.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/Theme.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ThemeDescription.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ThemeDescription.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/INavigationLayout.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/INavigationLayout.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/BaseFragment.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ActionBar.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBar.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ActionBarMenu.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenu.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarMenuItem.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/AndroidUtilities.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/AndroidUtilities.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/LocaleController.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/LocaleController.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/R.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/R.java",
                "mode": "CUT"
            },
            {
                "eve": "app/src/main/java/org/telegram/messenger/ApplicationLoader.java",
                "upstream": "TMessagesProj/src/main/java/org/telegram/messenger/ApplicationLoader.java",
                "mode": "CUT"
            }
        ]
    }
]


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    h.update(path.read_bytes())
    return h.hexdigest()


def normalize_code_line(line: str) -> str:
    line = line.strip()
    return re.sub(r"\s+", " ", line)


def is_visual_class(eve_path: str) -> bool:
    name = Path(eve_path).stem
    if name.startswith("ActionBar") or name.startswith("ActionBarMenu") or name == "EditTextBoldCursor":
        return True
    if name.endswith("Cell"):
        return True
    if "ui/Components" in eve_path:
        return True
    return False


def calculate_originality(eve_path: Path, upstream_path: Path) -> tuple[int, int, float]:
    """
    Returns (upstream_lines, kept_lines, originality_pct).
    originality_pct is the share of non-blank, non-comment eve lines
    (excluding // EVE-PATCH lines) that exist in the upstream file.
    """
    if not upstream_path or not upstream_path.is_file():
        eve_lines = len(eve_path.read_text(encoding="utf-8").splitlines())
        return 0, eve_lines, 0.0

    up_raw = upstream_path.read_text(encoding="utf-8").splitlines()
    eve_raw = eve_path.read_text(encoding="utf-8").splitlines()

    up_lines = set()
    for l in up_raw:
        s = l.strip()
        if not s or s.startswith("//") or s.startswith("/*") or s.startswith("*") or s.startswith("*/"):
            continue
        up_lines.add(normalize_code_line(l))

    evaluated_eve = []
    matched_eve = []
    in_patch_block = False
    for l in eve_raw:
        s = l.strip()
        if not s:
            continue
        if "// EVE-PATCH: BEGIN" in l:
            in_patch_block = True
            continue
        if "// EVE-PATCH: END" in l:
            in_patch_block = False
            continue
        if in_patch_block or "// EVE-PATCH" in l:
            continue
        if s.startswith("//") or s.startswith("/*") or s.startswith("*") or s.startswith("*/"):
            continue
        norm = normalize_code_line(l)
        evaluated_eve.append(norm)
        if norm in up_lines:
            matched_eve.append(norm)

    total_eval = len(evaluated_eve)
    matched = len(matched_eve)
    pct = round((matched / total_eval) * 100.0, 2) if total_eval > 0 else 100.0
    return len(up_raw), len(eve_raw), pct


def record_manifest(upstream_root: Path, output_path: Path, allow_known_debt: bool) -> int:
    pinned_rev = "21f7d109da6ab3506c9e9577f98c6a38771ab335"
    upstream_repo = "https://github.com/TelegramMessenger/Telegram-Android"

    failed_files = []
    manifest_screens = []

    for spec in SCREEN_SPECS:
        screen_entry = {
            "screen": spec["screen"],
            "eve_host": spec["eve_host"],
            "telegram_entry": spec["telegram_entry"],
            "dead_layout_files": spec.get("dead_layout_files", []),
            "files": []
        }

        for fspec in spec["files"]:
            eve_rel = fspec["eve"]
            mode = fspec["mode"]
            eve_file = REPO_ROOT / eve_rel

            if not eve_file.is_file():
                print(f"ERROR: Missing local file {eve_rel}", file=sys.stderr)
                return 1

            eve_hash = sha256_file(eve_file)
            up_rel = fspec.get("upstream", "")
            up_file = (upstream_root / up_rel) if up_rel else None

            up_hash = sha256_file(up_file) if (up_file and up_file.is_file()) else ""
            up_lines, kept_lines, orig_pct = 0, 0, 0.0

            if mode == "VERBATIM":
                if not up_file or not up_file.is_file():
                    print(f"ERROR: Upstream file missing for VERBATIM {eve_rel}: {up_rel}", file=sys.stderr)
                    return 1
                if eve_hash != up_hash:
                    print(f"ERROR: Hash mismatch for VERBATIM {eve_rel} vs {up_rel}", file=sys.stderr)
                    return 1
                up_lines = len(up_file.read_text(encoding="utf-8").splitlines())
                kept_lines = len(eve_file.read_text(encoding="utf-8").splitlines())
                orig_pct = 100.0
                is_known = False

            elif mode == "EVE-ORIGINAL":
                kept_lines = len(eve_file.read_text(encoding="utf-8").splitlines())
                orig_pct = 100.0
                is_known = False

            else:  # CUT
                up_lines, kept_lines, orig_pct = calculate_originality(eve_file, up_file)
                is_known = eve_rel in KNOWN_DEBT_ALLOWLIST

                if orig_pct < 95.0:
                    if not (allow_known_debt and is_known):
                        failed_files.append((eve_rel, orig_pct, "originality < 95.0%"))

            file_entry = {
                "eve": eve_rel,
                "upstream": up_rel,
                "mode": mode,
                "eve_sha256": eve_hash,
                "upstream_sha256": up_hash,
                "upstream_lines": up_lines,
                "kept_lines": kept_lines,
                "originality_pct": orig_pct,
                "removed_methods_reason": fspec.get("removed_methods_reason", ""),
                "known_debt": is_known if mode == "CUT" else False
            }
            screen_entry["files"].append(file_entry)

        manifest_screens.append(screen_entry)

    if failed_files:
        print("PORT GATE RECORD FAILED:", file=sys.stderr)
        for path, pct, reason in failed_files:
            print(f"  - {path}: {pct:.2f}% ({reason})", file=sys.stderr)
        print("Run with --allow-known-debt to record known pilot debt.", file=sys.stderr)
        return 1

    manifest_data = {
        "version": 1,
        "pinned_revision": pinned_rev,
        "upstream_repo": upstream_repo,
        "known_debt_allowlist": sorted(list(KNOWN_DEBT_ALLOWLIST)),
        "screens": manifest_screens
    }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(manifest_data, indent=2) + "\n", encoding="utf-8")
    print(f"Successfully recorded port manifest to {output_path}")
    return 0


def check_manifest(manifest_path: Path) -> int:
    if not manifest_path.is_file():
        print(f"ERROR: Manifest file not found: {manifest_path}", file=sys.stderr)
        return 1

    try:
        data = json.loads(manifest_path.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"ERROR: Corrupt manifest JSON: {e}", file=sys.stderr)
        return 1

    errors = []
    warnings = []

    # 1. Check all disk files under org/telegram are registered
    tg_dir = REPO_ROOT / "app" / "src" / "main" / "java" / "org" / "telegram"
    disk_files = set()
    if tg_dir.is_dir():
        for f in tg_dir.rglob("*.java"):
            disk_files.add(str(f.relative_to(REPO_ROOT)))
        for f in tg_dir.rglob("*.kt"):
            disk_files.add(str(f.relative_to(REPO_ROOT)))

    manifest_files = set()
    screens = data.get("screens", [])
    known_debt_allowlist = set(data.get("known_debt_allowlist", []))

    manifest_path_map = {}

    for screen in screens:
        screen_name = screen.get("screen", "Unknown")
        eve_host = screen.get("eve_host", "")
        telegram_entry = screen.get("telegram_entry", "")

        # Check host Activity
        host_parts = eve_host.split(".")
        host_rel = f"app/src/main/java/{'/'.join(host_parts)}.kt"
        host_file = REPO_ROOT / host_rel
        if not host_file.is_file():
            host_rel_java = f"app/src/main/java/{'/'.join(host_parts)}.java"
            host_file = REPO_ROOT / host_rel_java
            if not host_file.is_file():
                errors.append(f"Screen {screen_name}: Host activity file missing: {host_rel}")
            else:
                host_rel = host_rel_java

        if host_file.is_file():
            host_content = host_file.read_text(encoding="utf-8")
            entry_simple = telegram_entry.split(".")[-1]
            if telegram_entry not in host_content and entry_simple not in host_content:
                errors.append(
                    f"Screen {screen_name}: Host {host_rel} does not import or instantiate {telegram_entry}"
                )

        # Check AndroidManifest.xml
        manifest_xml = REPO_ROOT / "app" / "src" / "main" / "AndroidManifest.xml"
        if manifest_xml.is_file():
            xml_content = manifest_xml.read_text(encoding="utf-8")
            host_simple = host_parts[-1]
            if f".{host_simple}" not in xml_content and eve_host not in xml_content:
                errors.append(
                    f"Screen {screen_name}: Host {eve_host} is not registered in AndroidManifest.xml"
                )
        else:
            errors.append("AndroidManifest.xml missing")

        # Check dead layouts
        for dead_rel in screen.get("dead_layout_files", []):
            if (REPO_ROOT / dead_rel).is_file():
                errors.append(
                    f"Screen {screen_name}: Dead layout file still exists: {dead_rel}"
                )

        # Check screen files
        for f in screen.get("files", []):
            eve_rel = f.get("eve", "")
            manifest_files.add(eve_rel)
            manifest_path_map[eve_rel] = f

            local_path = REPO_ROOT / eve_rel
            if not local_path.is_file():
                errors.append(f"Manifest file missing on disk: {eve_rel}")
                continue

            current_hash = sha256_file(local_path)
            expected_hash = f.get("eve_sha256", "")
            if current_hash != expected_hash:
                errors.append(f"Stale hash for {eve_rel}: manifest={expected_hash[:8]} vs disk={current_hash[:8]}")

            mode = f.get("mode", "")
            orig_pct = f.get("originality_pct", 0.0)
            is_debt = f.get("known_debt", False)

            if is_debt:
                if eve_rel not in known_debt_allowlist:
                    errors.append(f"File marked known_debt is not in allowlist: {eve_rel}")
                else:
                    warnings.append(f"WARN-KNOWN: {eve_rel} (originality {orig_pct}%)")
            else:
                if mode == "CUT" and orig_pct < 95.0:
                    errors.append(f"Originality below 95.0% for {eve_rel}: {orig_pct}%")

            if mode == "EVE-ORIGINAL":
                reason = f.get("removed_methods_reason", "")
                if eve_rel not in EVE_ORIGINAL_ALLOWLIST and not reason:
                    errors.append(f"EVE-ORIGINAL file requires an allowlist reason: {eve_rel}")

            # Visual class ratio check (< 50% lines kept without reason)
            if is_visual_class(eve_rel):
                up_lines = f.get("upstream_lines", 0)
                kept_lines = f.get("kept_lines", 0)
                reason = f.get("removed_methods_reason", "")
                if up_lines > 0 and (kept_lines / up_lines) < 0.50 and not reason and not is_debt:
                    ratio = (kept_lines / up_lines) * 100.0
                    errors.append(
                        f"Visual class {eve_rel} kept ratio < 50% ({ratio:.1f}%) with no removed_methods_reason"
                    )

    # Check for unmanifested files
    unregistered = disk_files - manifest_files
    for unreg in sorted(unregistered):
        errors.append(f"Unregistered file under app/src/main/java/org/telegram: {unreg}")

    # Print results
    for w in warnings:
        print(f"  {w}")

    if errors:
        print("PORT GATE CHECK FAILED:", file=sys.stderr)
        for e in errors:
            print(f"  ERROR: {e}", file=sys.stderr)
        return 1

    print(f"PORT GATE CHECK PASSED: {len(screens)} screens, {len(manifest_files)} files verified.")
    return 0


def main():
    parser = argparse.ArgumentParser(description="EVE Port Gate tool")
    parser.add_argument("--record", action="store_true", help="Record manifest against upstream source")
    parser.add_argument("--upstream", type=str, help="Path to upstream Telegram source directory")
    parser.add_argument("--manifest", type=str, default=str(DEFAULT_MANIFEST), help="Path to port-manifest.json")
    parser.add_argument("--check", action="store_true", help="Verify repository against manifest")
    parser.add_argument("--allow-known-debt", action="store_true", help="Allow known pilot debt during record")

    args = parser.parse_args()
    manifest_path = Path(args.manifest)

    if args.record:
        if not args.upstream:
            print("ERROR: --upstream <dir> is required with --record", file=sys.stderr)
            return 1
        upstream_path = Path(args.upstream).resolve()
        if not upstream_path.is_dir():
            print(f"ERROR: Upstream directory not found: {upstream_path}", file=sys.stderr)
            return 1
        return record_manifest(upstream_path, manifest_path, args.allow_known_debt)

    elif args.check:
        return check_manifest(manifest_path)

    else:
        parser.print_help()
        return 1


if __name__ == "__main__":
    sys.exit(main())
