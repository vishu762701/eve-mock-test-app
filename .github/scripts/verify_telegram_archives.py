#!/usr/bin/env python3
"""Verify repository-root Telegram reference ZIPs; extract only into /tmp."""
import csv
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import stat
import sys
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[2]
NAMES = ["Telegram-master.zip"] + [f"Telegram-master-rest-{i}.zip" for i in range(1, 6)]
EXCLUDED = [
    "TMessagesProj/config/release.keystore",
    "TMessagesProj/google-services.json",
    "TMessagesProj_App/google-services.json",
    "TMessagesProj_AppHockeyApp/google-services.json",
    "TMessagesProj_AppHuawei/google-services.json",
    "TMessagesProj_AppStandalone/google-services.json",
    "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java",
]
issues = []
report = {"zip_sha256": {}, "excluded": EXCLUDED, "issues": issues}
def sha(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()
def norm(name):
    p = PurePosixPath(name.replace("\\", "/"))
    if str(p).startswith("/") or ".." in p.parts or ".git" in p.parts:
        raise ValueError("Unsafe archive path: " + name)
    parts = p.parts
    if parts and parts[0] == "Telegram-master":
        parts = parts[1:]
    return "/".join(parts)
def load_manifest(path):
    m = {}
    for number, line in enumerate(path.read_text(encoding="utf-8-sig").splitlines(), 1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        match = re.fullmatch(r"\s*([0-9a-fA-F]{64})\s+\*?(.+?)\s*", line)
        if not match:
            raise ValueError(f"Malformed {path.name} line {number}")
        filename = norm(match.group(2).removeprefix("./"))
        if not filename or filename in m:
            raise ValueError(f"Invalid/duplicate filename at {path.name}:{number}")
        m[filename] = match.group(1).lower()
    if not m:
        raise ValueError(f"Empty manifest: {path.name}")
    return m
def compare_manifest(folder, actual, name):
    matches = [p for p in folder.rglob(name) if p.is_file()]
    if len(matches) != 1:
        issues.append(f"MANIFEST MISSING/AMBIGUOUS: {name} ({len(matches)} found)")
        return {"name": name, "status": "FAIL", "file_count": 0}
    try:
        expected = load_manifest(matches[0])
    except (ValueError, UnicodeError) as exc:
        issues.append(str(exc))
        return {"name": name, "status": "FAIL", "file_count": 0}
    bad = 0
    for filename, digest in expected.items():
        if actual.get(filename) != digest:
            bad += 1
            issues.append(f"MANIFEST {'MISSING' if filename not in actual else 'MISMATCH'} {filename}")
    return {"name": name, "status": "PASS" if not bad else "FAIL",
            "file_count": len(expected), "mismatches": bad}
def verify_icons(folder):
    with (ROOT / "docs/telegram-icon-source-registry.tsv").open(encoding="utf-8") as stream:
        rows = csv.DictReader((line for line in stream if line.strip() and not line.startswith("#")), delimiter="\t")
        imported = [r for r in rows if r["source_fidelity_status"] == "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED"]
    checks = 0
    for row in imported:
        name = row["eve_icon"]
        upstream = row["telegram_zip_asset"]
        if not upstream.startswith("res/drawable-hdpi/"):
            issues.append("REGISTRY BAD SOURCE PATH " + name)
            continue
        for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi"):
            digest = row["sha256_" + density]
            app = ROOT / "app/src/main/res" / ("drawable-" + density) / (name + ".webp")
            if digest == "NOT FOUND":
                if app.exists():
                    issues.append("REGISTRY UNEXPECTED DENSITY " + str(app.relative_to(ROOT)))
                continue
            source = folder / "TMessagesProj/src/main" / upstream.replace("drawable-hdpi/", f"drawable-{density}/", 1)
            for location, path in (("UPSTREAM", source), ("EVE", app)):
                checks += 1
                if not path.is_file():
                    issues.append(f"REGISTRY {location} MISSING {name}/{density} {path}")
                elif sha(path) != digest:
                    issues.append(f"REGISTRY {location} SHA MISMATCH {name}/{density} {path}")
    return {"imported_icons": len(imported), "file_hash_checks": checks}
def license_details(folder):
    license_path = folder / "LICENSE"
    if not license_path.is_file():
        issues.append("Telegram LICENSE missing")
        return {"status": "FAIL"}
    text = license_path.read_text(encoding="utf-8", errors="replace")
    is_v2 = "GNU GENERAL PUBLIC LICENSE" in text and bool(re.search(r"Version\s+2\s*,\s*June\s+1991", text))
    if not is_v2:
        issues.append("Root LICENSE is not recognized GPL v2 text")
    java = folder / "TMessagesProj/src/main/java"
    match_paths = []
    if not java.is_dir():
        issues.append("Java source root missing")
    else:
        for path in java.rglob("*.java"):
            if re.search(r"\bor later\b|\bany later version\b", path.read_text(encoding="utf-8", errors="replace"), re.I):
                match_paths.append(path.relative_to(folder).as_posix())
    return {"status": "PASS" if is_v2 else "FAIL",
            "license": "GNU GPL version 2 (June 1991)" if is_v2 else "UNKNOWN",
            "java_or_later_files": len(match_paths),
            "java_or_later_paths_sample": match_paths[:35],
            "note": "GPL-2 license text does not alone grant the later-version option for every file; file notices govern where that option applies."}
def verify():
    for name in NAMES:
        path = ROOT / name
        if not path.is_file():
            issues.append("ZIP MISSING " + name)
        else:
            report["zip_sha256"][name] = sha(path)
            print(f"ZIP_SHA256 {name} {report['zip_sha256'][name]} ({path.stat().st_size} bytes)", flush=True)
    if len(report["zip_sha256"]) != 6:
        return
    with tempfile.TemporaryDirectory(prefix="eve-telegram-check-") as temp_path:
        folder = Path(temp_path)
        actual = {}
        partial = {}
        for index, zipname in enumerate(NAMES):
            count = 0
            with zipfile.ZipFile(ROOT / zipname) as archive:
                for entry in archive.infolist():
                    filename = norm(entry.filename)
                    if not filename or entry.is_dir():
                        continue
                    if ((entry.external_attr >> 16) & 0o170000) == stat.S_IFLNK:
                        issues.append(f"SYMLINK IN ZIP {zipname}:{filename}")
                        continue
                    destination = folder / filename
                    if destination.is_file():
                        with archive.open(entry) as stream:
                            h = hashlib.sha256()
                            for block in iter(lambda: stream.read(1024 * 1024), b""):
                                h.update(block)
                        if h.hexdigest() != actual[filename]:
                            issues.append(f"CONFLICTING DUPLICATE {zipname}:{filename}")
                        continue
                    destination.parent.mkdir(parents=True, exist_ok=True)
                    digest = hashlib.sha256()
                    with archive.open(entry) as stream, destination.open("wb") as out:
                        for block in iter(lambda: stream.read(1024 * 1024), b""):
                            out.write(block)
                            digest.update(block)
                    actual[filename] = digest.hexdigest()
                    count += 1
            print(f"EXTRACTED {zipname}: {count} files", flush=True)
            report.setdefault("extracted_files", {})[zipname] = count
            if index == 0:
                partial = dict(actual)
        for filename in EXCLUDED:
            if filename in actual:
                issues.append("EXCLUDED FILE FOUND " + filename)
        report["file_count"] = len(actual)
        report["manifests"] = [
            compare_manifest(folder, partial, "EVE_MANIFEST.sha256"),
            compare_manifest(folder, actual, "EVE_MANIFEST_FULL.sha256"),
        ]
        report["icons"] = verify_icons(folder)
        report["license"] = license_details(folder)
try:
    verify()
except (ValueError, OSError, zipfile.BadZipFile, KeyError) as exc:
    issues.append(f"VERIFIER ERROR {type(exc).__name__}: {exc}")
report["status"] = "FAIL" if issues else "PASS"
print("VERIFICATION_RESULT=" + report["status"], flush=True)
for key in ("manifests", "icons", "license"):
    if key in report:
        print(key.upper() + "=" + json.dumps(report[key]), flush=True)
print("ISSUE_COUNT=" + str(len(issues)), flush=True)
for issue in issues[:75]:
    print("ISSUE " + issue, flush=True)
if len(issues) > 75:
    print(f"... {len(issues) - 75} additional issues (see artifact)")
(ROOT / "telegram-verification-result.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
sys.exit(1 if issues else 0)
