#!/usr/bin/env python3
"""Record reproducible, non-certifying evidence for Android release dependencies.

Input is an actual Gradle :app:dependencies --configuration releaseRuntimeClasspath
output. POM license tags are *metadata clues*, not verified redistribution grants.
Never read/print credentials or signing material.
"""
import csv
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
import re
import subprocess
import sys
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "license-evidence"
PATTERN = re.compile(
    r"^([A-Za-z0-9_.-]+):([A-Za-z0-9_.-]+):([A-Za-z0-9_.+\-]+)"
    r"(?:\s+->\s+([A-Za-z0-9_.+\-]+))?"
)


def digest(path):
    h = hashlib.sha256()
    with path.open("rb") as src:
        for block in iter(lambda: src.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def coordinates(lines):
    result = set()
    unresolved = []
    for line in lines:
        if "--- " not in line:
            continue
        text = line.split("--- ", 1)[1].strip()
        if "FAILED" in text or "Could not resolve" in text:
            unresolved.append(text)
        if "(c)" in text or "(n)" in text:
            continue
        match = PATTERN.match(text)
        if match:
            group, artifact, requested, selected = match.groups()
            result.add((group, artifact, selected or requested))
    return sorted(result), unresolved


def pom_license_names_from_bytes(payload):
    try:
        root = ET.fromstring(payload)
    except ET.ParseError:
        return []
    for element in root.iter():
        if element.tag.split("}")[-1] == "licenses":
            found = []
            for license_node in element:
                if license_node.tag.split("}")[-1] != "license":
                    continue
                for node in license_node:
                    if node.tag.split("}")[-1] == "name" and node.text:
                        found.append(" ".join(node.text.split()))
            return sorted(set(found))
    return []


def pom_license_names(pom):
    try:
        return pom_license_names_from_bytes(pom.read_bytes())
    except OSError:
        return []


def repository_metadata(group, artifact, version):
    """Fetch public POM metadata only; no signing material, code or binary downloads."""
    rel = "/".join([group.replace(".", "/"), artifact, version, f"{artifact}-{version}.pom"])
    google = "https://dl.google.com/dl/android/maven2/"
    maven = "https://repo.maven.apache.org/maven2/"
    jitpack = "https://jitpack.io/"
    if group.startswith(("androidx.", "com.google.", "com.android.")):
        bases = (google, maven)
    elif group.startswith("com.github."):
        bases = (jitpack, maven)
    else:
        bases = (maven, google)
    for base in bases:
        try:
            request = Request(base + rel, headers={"User-Agent": "EveEvidenceAudit/1.0"})
            with urlopen(request, timeout=6) as result:
                payload = result.read(2_000_001)
            if len(payload) > 2_000_000:
                continue
            return {
                "pom_available": "yes", "metadata_source": base,
                "pom_sha256": hashlib.sha256(payload).hexdigest(),
                "labels": pom_license_names_from_bytes(payload),
            }
        except (HTTPError, URLError, TimeoutError, OSError):
            continue
    return {
        "pom_available": "no", "metadata_source": "NOT FOUND",
        "pom_sha256": "", "labels": [],
    }


def gather():
    graph = OUT / "releaseRuntimeClasspath.txt"
    if not graph.is_file():
        raise RuntimeError("Gradle graph missing")
    tree = graph.read_text(encoding="utf-8", errors="replace").splitlines()
    coords, unresolved = coordinates(tree)
    if unresolved:
        raise RuntimeError("Unresolved Gradle components: " + str(len(unresolved)))
    if not coords:
        raise RuntimeError("No Gradle release runtime components detected")

    cache = Path.home() / ".gradle/caches/modules-2/files-2.1"
    metadata = []
    no_pom = 0
    no_license = 0
    remote = {}
    with ThreadPoolExecutor(max_workers=12) as executor:
        for coordinate, details in zip(coords, executor.map(lambda c: repository_metadata(*c), coords)):
            remote[coordinate] = details
    for group, artifact, version in coords:
        directory = cache / group / artifact / version
        poms = sorted(directory.glob("**/*.pom")) if directory.is_dir() else []
        remote_data = remote[(group, artifact, version)]
        labels = list(remote_data["labels"])
        for pom in poms:
            labels.extend(pom_license_names(pom))
        labels = sorted(set(labels))
        if not poms and remote_data["pom_available"] != "yes":
            no_pom += 1
        if not labels:
            no_license += 1
        metadata.append({
            "group": group, "artifact": artifact, "version": version,
            "pom_cached": "yes" if poms else "no",
            "pom_downloaded": remote_data["pom_available"],
            "pom_metadata_source": remote_data["metadata_source"],
            "pom_sha256": remote_data["pom_sha256"],
            "pom_license_names": " | ".join(labels) if labels else "NOT DETERMINED",
            "review_status": "UNREVIEWED",
        })
    with (OUT / "runtime-component-license-metadata.csv").open("w", encoding="utf-8", newline="") as stream:
        fields = list(metadata[0])
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        writer.writerows(metadata)

    tracked = subprocess.check_output(
        ["git", "ls-files", "-z", "--", "app/src/main/res", "app/src/main/assets"],
        cwd=ROOT,
    ).decode("utf-8").split("\0")
    asset_lines = []
    for path_text in sorted(x for x in tracked if x):
        path = ROOT / path_text
        if path.is_file():
            asset_lines.append(digest(path) + "  " + path_text)
    (OUT / "tracked-resource-sha256.txt").write_text(
        "\n".join(asset_lines) + "\n", encoding="utf-8"
    )

    revision = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, text=True
    ).strip()
    summary = {
        "commit": revision,
        "release_configuration": "releaseRuntimeClasspath",
        "runtime_components": len(coords),
        "components_without_pom_metadata": no_pom,
        "components_without_pom_license_names": no_license,
        "tracked_resource_files_hashed": len(asset_lines),
        "dependency_graph_sha256": digest(graph),
        "disposition": "Evidence only: POM license tags do not certify binary compliance; no release approval implied.",
        "production_release_gate": "unchanged, requires EVE_GPL_RELEASE_APPROVED=true plus existing signing gates",
    }
    (OUT / "summary.json").write_text(
        json.dumps(summary, indent=2) + "\n", encoding="utf-8"
    )
    print("Runtime components:", len(coords), flush=True)
    print("POM metadata unavailable:", no_pom, flush=True)
    print("POM license names not determined:", no_license, flush=True)
    print("Tracked resources hashed:", len(asset_lines), flush=True)
    print("Evidence commit:", revision, flush=True)


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "--self-test":
        sample = [
            "+--- com.google.firebase:firebase-auth:23.0.0",
            "|    \\--- com.google.android.gms:play-services-basement:18.4.0 -> 18.5.0",
            "+--- org.example:platform:1.0 (c)",
        ]
        coords, issues = coordinates(sample)
        assert coords == [
            ("com.google.android.gms", "play-services-basement", "18.5.0"),
            ("com.google.firebase", "firebase-auth", "23.0.0"),
        ], coords
        assert not issues
        assert pom_license_names_from_bytes(b"<project><licenses><license><name>Apache-2.0</name></license></licenses></project>") == ["Apache-2.0"]
        print("SELF_TEST=PASS")
    else:
        try:
            OUT.mkdir(parents=True, exist_ok=True)
            gather()
        except (OSError, RuntimeError, subprocess.CalledProcessError) as exc:
            print("EVIDENCE_COLLECTION_FAILED: " + str(exc), file=sys.stderr)
            raise SystemExit(1)
