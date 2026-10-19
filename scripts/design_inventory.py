#!/usr/bin/env python3
"""Print reproducible source/resource counts for the Android production UI."""
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / 'app/src/main'
files = sorted(p for p in MAIN.rglob('*') if p.is_file())
text = {p: p.read_text(errors='replace') for p in files if p.suffix in {'.xml', '.kt', '.java', '.json'}}
styles = sum(len(ET.parse(p).getroot().findall('style')) for p in files if p.suffix == '.xml' and p.parent.name.startswith('values'))
counts = {
    'hex_color_occurrences': sum(len(re.findall(r'(?<![\w])(?:#[0-9a-fA-F]{8}|#[0-9a-fA-F]{6}|#[0-9a-fA-F]{4}|#[0-9a-fA-F]{3})(?![\da-fA-F])|\b0x[0-9a-fA-F]{8}\b', t)) for t in text.values()),
    'literal_dp_occurrences': sum(len(re.findall(r'(?<![\w])\d+(?:\.\d+)?dp\b', t)) for t in text.values()),
    'literal_sp_occurrences': sum(len(re.findall(r'(?<![\w])\d+(?:\.\d+)?sp\b', t)) for t in text.values()),
    'style_definitions_including_qualifiers': styles,
    'drawable_files_including_qualifiers': sum(p.parent.name.startswith('drawable') for p in files),
    'font_files_including_xml_families': sum(p.parent.name == 'font' for p in files),
    'layout_files_including_qualifiers': sum(p.parent.name.startswith('layout') for p in files),
    'kotlin_files': sum(p.suffix == '.kt' for p in files),
}
inventory = {
    'scope': 'app/src/main: XML/Kotlin/Java/JSON text; dp/sp count includes comments and token definitions; hex includes #RGB/#ARGB/#RRGGBB/#AARRGGBB and 0xAARRGGBB',
    'counts': counts,
    'layouts': [str(p.relative_to(ROOT)) for p in files if p.parent.name.startswith('layout')],
    'ui_sources': [str(p.relative_to(ROOT)) for p in files if p.suffix in {'.kt', '.java'} and ('ui' in p.parts or re.search(r'class\s+\w+\s*[^\n]*(?:Activity|Fragment|View|Dialog|Adapter|Worker|Service)', text.get(p, '')))],
    'fonts': [str(p.relative_to(ROOT)) for p in files if p.parent.name == 'font'],
}
print(json.dumps(inventory, indent=2))
