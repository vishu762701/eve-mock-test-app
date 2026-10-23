"""Fail closed on AndroidJUnitRunner errors, skips, crashes and truncated output."""
from pathlib import Path
import re
import sys


def analyze(lines):
    totals = set()
    started = completed = 0
    failure = []
    final = []
    name = "unknown"
    for row in lines:
        line = row.strip()
        if line.startswith("INSTRUMENTATION_STATUS: class="):
            name = line.partition("=")[2]
        if line.startswith("INSTRUMENTATION_STATUS: test="):
            name += "#" + line.partition("=")[2]
        m = re.fullmatch(r"INSTRUMENTATION_STATUS: numtests=(\d+)", line)
        if m:
            totals.add(int(m.group(1)))
        m = re.fullmatch(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)", line)
        if m:
            status = int(m.group(1))
            if status == 1:
                started += 1
            elif status in (0, -1, -2, -3, -4):
                completed += 1
                if status != 0:
                    failure.append(f"NATIVE_FAILURE {name}: status={status}")
            else:
                failure.append(f"Unexpected status {status}")
        m = re.fullmatch(r"INSTRUMENTATION_CODE: (-?\d+)", line)
        if m:
            final.append(int(m.group(1)))
    if len(totals) != 1 or next(iter(totals), 0) < 1:
        failure.append("Missing/inconsistent test count")
    if final != [-1]:
        failure.append("Missing or failed terminal instrumentation code")
    if started < 1 or started != completed or (len(totals) == 1 and completed != next(iter(totals))):
        failure.append(f"Test run incomplete: started={started} completed={completed} totals={sorted(totals)}")
    return not failure, completed, next(iter(totals), 0), failure


def main(argv=None):
    args = sys.argv[1:] if argv is None else argv
    if len(args) != 1:
        return 2
    path = Path(args[0])
    if not path.is_file():
        print(f"NATIVE_FAILURE Missing instrumentation log: {path}", file=sys.stderr)
        return 1
    ok, completed, total, failures = analyze(path.read_text(errors="replace").splitlines())
    if ok:
        print(f"NATIVE_PASS {completed}/{total} tests, zero failures/skips")
        return 0
    for item in failures:
        print(item, file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
