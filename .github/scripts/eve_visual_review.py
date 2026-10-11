#!/usr/bin/env python3
"""Review native screenshot fixtures and flag Android XML geometry for human audit."""
import argparse
import html
import json
import re
import struct
from collections import defaultdict
from pathlib import Path
import xml.etree.ElementTree as ET

PNG_MAGIC = b"\x89PNG\r\n\x1a\n"
ANDROID = "{http://schemas.android.com/apk/res/android}"
APP = "{http://schemas.android.com/apk/res-auto}"
PAIRS = (
    "home", "mock-test", "recovery-home-360dp", "recovery-profile",
    "recovery-result-overview", "recovery-result-review",
    "recovery-result-leaderboard", "recovery-target-exams", "recovery-target-exams-footer",
)
THEME = re.compile(r"-(light|dark)(?:-(gesture|three-button))?$")
DP = re.compile(r"^(\d+(?:\.\d+)?)dp$")


def dimension(value):
    match = DP.fullmatch(value or "")
    return float(match.group(1)) if match else None


def images(captures, output):
    found, warnings, pairs = [], [], defaultdict(set)
    if not captures.exists():
        warnings.append("Native screenshot directory does not exist")
    for path in sorted(captures.rglob("*.png")):
        if output not in path.parents:
            warnings.append("Screenshot outside artifact directory: " + str(path))
            continue
        relative = path.relative_to(output).as_posix()
        try:
            with path.open("rb") as source:
                head = source.read(24)
            if len(head) != 24 or head[:8] != PNG_MAGIC or head[12:16] != b"IHDR":
                raise ValueError("invalid PNG header")
            width, height = struct.unpack(">II", head[16:24])
            if width < 1 or height < 1 or width > 20000 or height > 20000:
                raise ValueError("invalid image dimensions")
        except (OSError, ValueError) as exc:
            warnings.append(relative + ": " + str(exc))
            continue
        match = THEME.search(path.stem)
        group = path.stem[:match.start()] if match else path.stem
        theme = match.group(1) if match else "unknown"
        navigation = match.group(2) if match and match.group(2) else "unspecified"
        pairs[group].add(theme)
        found.append({"file": relative, "group": group, "theme": theme,
                      "navigation": navigation, "width": width, "height": height})
    missing = [f"{name}: {theme}" for name in PAIRS for theme in ("light", "dark")
               if theme not in pairs[name]]
    return found, warnings, missing


def xml_candidates(resources):
    """Potential issues, NOT confirmed defects; XML cannot validate runtime UI."""
    found = []
    for path in sorted(resources.rglob("*.xml")):
        if not path.parent.name.startswith("layout"):
            continue
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        for node in tree.iter():
            attrs = node.attrib
            width = dimension(attrs.get(ANDROID + "layout_width"))
            height = dimension(attrs.get(ANDROID + "layout_height"))
            stroke = dimension(attrs.get(APP + "strokeWidth"))
            radius = dimension(attrs.get(APP + "cardCornerRadius"))
            elevation = dimension(attrs.get(ANDROID + "elevation"))
            reasons = []
            if width is not None and width >= 200:
                reasons.append("Large fixed width: check smaller screens / large font")
            if stroke is not None and stroke >= 2:
                reasons.append("Heavy stroke: confirm decorative versus semantic role")
            if elevation is not None and elevation >= 8:
                reasons.append("High elevation: verify dark/light surface hierarchy")
            if radius is not None and height is not None and radius > height / 2:
                reasons.append("Corner radius exceeds half component height")
            for reason in reasons:
                found.append({"path": path.as_posix(), "element": node.tag.split("}")[-1],
                              "id": attrs.get(ANDROID + "id", ""), "review": reason,
                              "protected_test_geometry": "test" in path.stem.lower()})
    return found


def gallery(output, captures, missing, candidates, warnings):
    groups = defaultdict(list)
    for capture in captures:
        groups[capture["group"]].append(capture)
    sections = []
    for name, items in sorted(groups.items()):
        figures = []
        for item in sorted(items, key=lambda x: (x["theme"], x["navigation"], x["file"])):
            src = html.escape(item["file"], quote=True)
            label = html.escape(item["theme"] + " / " + item["navigation"] +
                                " / " + str(item["width"]) + "x" + str(item["height"]))
            figures.append(f'<figure><a href="{src}"><img loading="lazy" src="{src}" alt="{label}"></a>'
                           f'<figcaption>{label}</figcaption></figure>')
        sections.append(f'<section><h2>{html.escape(name)}</h2><div class="pairs">{"".join(figures)}</div></section>')
    gaps = "".join("<li>" + html.escape(x) + "</li>" for x in missing) or "<li>None</li>"
    notices = "".join("<li>" + html.escape(x) + "</li>" for x in warnings) or "<li>None</li>"
    queue = "".join("<li>" + html.escape(x["path"] + " / " + x["element"] + " / " + x["review"]) +
                    "</li>" for x in candidates[:100]) or "<li>No heuristic candidates</li>"
    document = f"""<!doctype html><html lang="en"><head><meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Eve Visual Evidence</title><style>
:root{{color-scheme:light dark;font-family:system-ui,sans-serif}}
body{{max-width:1100px;margin:auto;padding:16px;line-height:1.5}}
section{{border-top:1px solid #8885;padding-top:12px}}
.pairs{{display:grid;grid-template-columns:repeat(auto-fit,minmax(min(100%,230px),1fr));gap:12px}}
figure{{margin:0;min-width:0}}img{{width:100%;height:390px;object-fit:contain;border-radius:12px;background:#7772}}
figcaption{{text-align:center;font-size:.8rem}}li{{overflow-wrap:anywhere}}
</style></head><body><h1>Eve Visual Evidence</h1>
<p>Native fixtures, NOT authenticated production screenshots. Motion, hardware blur, live data and tactile feedback require device review.</p>
<p>{len(captures)} images; {len(missing)} required theme captures missing; {len(candidates)} source review candidates.</p>
<h2>Missing captures</h2><ul>{gaps}</ul><h2>Warnings</h2><ul>{notices}</ul>
{"".join(sections) or "<p>No captures available.</p>"}
<section><h2>Potential XML geometry risks (not proven defects)</h2><ol>{queue}</ol>
<p>Complete machine-readable report: <a href="visual-review.json">visual-review.json</a>.</p></section>
</body></html>"""
    (output / "visual-review.html").write_text(document, encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--captures", type=Path, default=Path("ui-renderings"))
    parser.add_argument("--output", type=Path, default=Path("ui-renderings"))
    parser.add_argument("--source", type=Path, default=Path("app/src/main/res"))
    parser.add_argument("--enforce", action="store_true")
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    screenshots, warnings, missing = images(args.captures.resolve(), args.output.resolve())
    candidates = xml_candidates(args.source)
    (args.output / "visual-review.json").write_text(
        json.dumps({"schema_version": 1, "capture_type": "native fixture",
                    "screenshots": screenshots, "missing_required_pairs": missing,
                    "warnings": warnings, "xml_review_candidates": candidates},
                   ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    gallery(args.output, screenshots, missing, candidates, warnings)
    print(f"Eve evidence: {len(screenshots)} screenshots, {len(missing)} missing pairs, "
          f"{len(warnings)} warnings, {len(candidates)} geometry candidates")
    if args.enforce and (missing or warnings):
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
