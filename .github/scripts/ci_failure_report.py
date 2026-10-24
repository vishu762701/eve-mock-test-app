#!/usr/bin/env python3
"""Summarize the first actionable CI error; never suppress or auto-retry failure.

No network calls, no credentials, no source mutation. Safe in an if: failure() step.
"""
from __future__ import annotations

import os
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ANSI = re.compile(r"\x1b\[[0-9;]*m")
CATEGORIES = (
    ("KOTLIN_OR_JAVA_SOURCE", re.compile(r"(?:^e: (?:file:|/|.+\.kt:)|\.kt:\d+(?::\d+)?: (?:error|unresolved reference)|\.java:\d+: error:)", re.I)),
    ("AAPT_OR_RESOURCE", re.compile(r"(?:Android resource linking failed|AAPT: error:|error: resource [\w/]+ not found)", re.I)),
    ("GRADLE_DEPENDENCY", re.compile(r"(?:Could not resolve all files|Could not find [\w.-]+:[\w.-]+|Could not resolve [\w.-]+:[\w.-]+|Could not GET .* from)", re.I)),
    ("UNIT_OR_NATIVE_TEST", re.compile(r"(?:Tests? .* FAILED|There were failing tests|AssertionError:|org\.junit\.ComparisonFailure)", re.I)),
    ("ANDROID_LINT", re.compile(r"(?:Lint found errors|LintError|lintDebug FAILED)", re.I)),
    ("SDK_OR_INFRASTRUCTURE", re.compile(r"(?:SDK location not found|Failed to find Build Tools|No space left on device|Read timed out|Connection reset by peer)", re.I)),
    ("FAILED_GRADLE_TASK", re.compile(r"^> Task \S+ FAILED$", re.I)),
)

def clean(line: str) -> str:
    text = ANSI.sub("", line).strip()
    text = re.sub(r"(?i)\b(token|password|secret|authorization|api[_-]?key)([=:]\s*)(\S+)", r"\1\2[REDACTED]", text)
    return text.replace("::", ": :")[:400]

def classify_gradle_log(log: str) -> tuple[str, list[str]]:
    lines = [clean(line) for line in log.splitlines()]
    for category, pattern in CATEGORIES:
        found = [line for line in lines if pattern.search(line)]
        if found:
            return category, found[:5]
    failure = [line for line in lines if "BUILD FAILED" in line or "FAILURE: Build failed" in line]
    return ("UNCLASSIFIED", failure[:3])

def summarize_native_xml(path: Path) -> list[str]:
    failures: list[str] = []
    if not path.is_dir():
        return failures
    for xml in sorted(path.rglob("*.xml")):
        try:
            root = ET.parse(xml).getroot()
        except (ET.ParseError, OSError):
            continue
        for case in root.iter("testcase"):
            for failure in list(case.findall("failure")) + list(case.findall("error")):
                message = clean(failure.get("message", "") or (failure.text or "").split("\n", 1)[0])
                failures.append(clean(f"{case.get('classname', '')}.{case.get('name', '')}: {message}"))
    return failures

def main(argv: list[str]) -> int:
    if not argv:
        print("Usage: ci_failure_report.py <gradle logs...> | --native <androidTest-results dir>")
        return 0  # This diagnostic never overrides the original failed job.
    lines = ["### EVE CI failure diagnostics", "", "Original failing job remains **FAILED**. These are investigation leads, not proof of a complete root cause.", ""]
    if argv[0] == "--native":
        paths = [Path(p) for p in argv[1:]]
        errors = [entry for path in paths for entry in summarize_native_xml(path)]
        if errors:
            lines.extend([f"**Native assertions:** {len(errors)}", "", *("- " + item for item in errors[:10])])
        else:
            lines.append("No readable JUnit failure XML. Inspect emulator startup, navigation mode, runner logs and device ANRs.")
    else:
        available = [(Path(name), Path(name).read_text(errors="replace")) for name in argv if Path(name).is_file()]
        if not available:
            lines.append("No captured Gradle log: inspect the earliest failing GitHub Actions step (checkout, SDK, setup or preflight).")
        for file, log in available:
            category, excerpts = classify_gradle_log(log)
            lines.extend([f"**{file.name} — {category}**", ""])
            if excerpts:
                lines.extend("- " + item for item in excerpts)
            else:
                lines.append("- No actionable compiler/test signature; inspect the complete job log.")
            lines.append("")
    report = "\n".join(lines) + "\n"
    print(report)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as f:
            f.write(report)
    return 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
