#!/usr/bin/env python3
"""Production UI source enforcement; deliberately not connected to CI.

Android's compiler/lint remain authoritative for external-library resources.
Deleted local resources are checked against the actual cleanup evidence.
Protected animation and bit-channel arithmetic are not UI color overrides.
"""
import hashlib
import json
import re
import sys
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / 'app/src/main'
RES = MAIN / 'res'
A = '{http://schemas.android.com/apk/res/android}'
errors = []

def fail(p, message):
    errors.append(f'{p.relative_to(ROOT)}: {message}')

protected = {MAIN / 'java/com/eve/app/util' / n for n in
             ('ThemeSwitchAnimator.kt', 'ThemeManager.kt')}
protected.add(MAIN/'java/com/eve/app/ui/home/TelegramMenuPopup.kt')
protected.update(RES/'drawable'/n for n in ('ic_theme_sun.xml', 'ic_theme_moon.xml'))
for p in protected:
    if not p.is_file():
        fail(p, 'protected file missing')
# Only actual canonical color definitions and the unchanged Lottie lookup palette.
color_tokens = {RES/q/n for q in ('values', 'values-night') for n in ('design_colors.xml', 'design_derived_colors.xml', 'design_protected_colors.xml')}
color_tokens.add(MAIN / 'java/com/eve/app/designsystem/DesignLottieTokens.kt')
hex_color = re.compile(r'#[0-9a-fA-F]{3,8}\b|\b0x[0-9a-fA-F]{6}(?:[0-9a-fA-F]{2})?\b')
old_token = re.compile(r'\b(?:eve_color_\w+|eve_app_\w+|HEX_APP_\w+|HEX_(?:LIGHT|DARK)_\w+|selector_eve_\w+)\b')
texts = {}
for p in sorted(MAIN.rglob('*')):
    if not p.is_file() or p.suffix not in {'.xml', '.kt', '.java'}:
        continue
    text = p.read_text()
    texts[p] = text
    # Comments do not alter runtime presentation.
    code = re.sub(r'<!--.*?-->|/\*.*?\*/', '', text, flags=re.S)
    if p.suffix in {'.kt', '.java'}:
        code = re.sub(r'//[^\n]*', '', code)
    if p not in color_tokens and p not in protected:
        for line in code.splitlines():
            # Stack-blur channel masks/packing are arithmetic, not color choices.
            # Restrict this exception to the unchanged protected reveal's algorithm.
            if p.name == 'FastBlurHelper.kt' and re.search(r'\band\s+0x|\(-0x1000000\)', line):
                continue
            if hex_color.search(line):
                fail(p, 'hex color outside a token file: ' + line.strip())
    # The preferences database eve_app_prefs is data, not a design resource.
    resource_code = re.sub(r'"eve_app_prefs"', '', code)
    if old_token.search(resource_code):
        fail(p, 'obsolete design token: ' + old_token.search(resource_code).group())
    if p in protected:
        continue
    if p.suffix in {'.kt', '.java'}:
        if re.search(r'\b(?:radius|cornerRadius|cardElevation|elevation|translationZ)\s*=\s*-?\d+(?:\.\d+)?[fF]?\b|\bset(?:CornerRadius|Radius|Elevation)\(\s*\d', code):
            fail(p, 'literal radius/elevation; use shared dimensions')
    if p.suffix != '.xml':
        continue
    try:
        tree = ET.fromstring(text)
    except ET.ParseError as exc:
        fail(p, str(exc)); continue
    is_layout = p.parent.name.startswith('layout')
    for node in tree.iter():
        for attr, value in node.attrib.items():
            key = attr.rsplit('}', 1)[-1]
            if is_layout and key in {'textSize', 'fontFamily', 'typeface'} and not value.startswith(('@', '?')):
                fail(p, f'literal {key}: {value}')
            if ('radius' in key.lower() or key in {'elevation', 'cardElevation', 'translationZ'}) and re.fullmatch(r'-?\d+(?:\.\d+)?(?:dp|px|sp)', value):
                fail(p, f'literal {key}: {value}')
        if node.tag == 'item' and re.search(r'(?:corner|radius|elevation|translationZ)', node.get('name', ''), re.I) and re.fullmatch(r'-?\d+(?:\.\d+)?(?:dp|px|sp)', (node.text or '').strip()):
            fail(p, 'literal corner/elevation style item')
        if not is_layout:
            continue
        ident = node.get(A+'id', '').lower()
        tag = node.tag.rsplit('.', 1)[-1]
        if tag in {'View', 'Space'}:
            width, height = node.get(A+'layout_width', ''), node.get(A+'layout_height', '')
            thin = {'0.5dp', '1dp', '2dp', '@dimen/design_hairline'}
            if 'divider' in ident or 'separator' in ident or (width in thin or height in thin) and node.get(A+'background'):
                fail(p, 'divider/separator view')

# Build the production resource index (including values aliases and qualified copies).
defined = set()
for p in RES.rglob('*'):
    if not p.is_file(): continue
    folder = p.parent.name.split('-')[0]
    if folder == 'values' and p.suffix == '.xml':
        for node in ET.parse(p).getroot():
            name = node.get('name')
            if name:
                kind = node.get('type') if node.tag == 'item' else node.tag
                defined.add((kind, name))
    elif folder != 'values':
        defined.add((folder, p.name.split('.')[0]))
proof = json.loads((ROOT/'docs/design-reset/cleanup-evidence.json').read_text())
deleted = set()
for group in proof['resource-deletion']:
    for item in group['resources']:
        kind, name = item['resource'].split('.', 1)
        if (kind, name) not in defined:
            deleted.add((kind, name))
for p, text in texts.items():
    refs = set(re.findall(r'@(?:\+)?([a-z_]+)/([\w.]+)', text))
    for kind, name in re.findall(r'(?<!android\.)\bR\.([a-z_]+)\.([\w]+)', text):
        # AAPT converts style periods into underscores in generated R symbols.
        matches = {(k, n) for k, n in deleted if k == kind and n.replace('.', '_') == name}
        refs.update(matches)
    for ref in sorted(refs & deleted):
        fail(p, 'reference to deleted resource ' + '.'.join(ref))

# The sole protected aliases must not spread into unprotected UI.
for p, text in texts.items():
    if p.name == 'ThemeManager.kt' or p.name == 'design_protected_colors.xml': continue
    if re.search(r'(?:@color/|R\.color\.)(?:eve_bg|eve_text)\b', text):
        fail(p, 'protected-only color alias used outside ThemeManager')

base = ET.parse(RES/'values/design_dimens.xml').getroot()
base_dims = {n.get('name'): n.text for n in base if n.tag == 'dimen'}
for qualifier in ('values-sw360dp', 'values-sw600dp'):
    dims = {n.get('name'): n.text for n in ET.parse(RES/qualifier/'design_dimens.xml').getroot() if n.tag == 'dimen'}
    if dims != base_dims:
        errors.append(qualifier + ': size tokens differ from locked defaults')
font_files = {p.name for p in (RES/'font').iterdir()}
if font_files != {'poppins.xml', 'poppins_regular.ttf', 'poppins_medium.ttf', 'poppins_bold.ttf'}:
    errors.append('font inventory differs from bundled Poppins 400/500/700 family')
# Verify the exceptions against actual baseline bytes; do not hide modifications.
baseline = json.loads((ROOT/'docs/design-reset/baseline-sha256.json').read_text())
unchanged = [p for p in protected if p.name != 'TelegramMenuPopup.kt']
unchanged.append(MAIN/'java/com/eve/app/util/FastBlurHelper.kt')
unchanged.extend(p for p in MAIN.rglob('*') if p.is_file() and ('raw' in p.parts or 'lottie' in p.parts))
for p in unchanged:
    key = str(p.relative_to(ROOT))
    if key not in baseline or hashlib.sha256(p.read_bytes()).hexdigest() != baseline[key]:
        fail(p, 'protected animation/resource differs from baseline')
if errors:
    print('\n'.join(errors)); print(f'FAIL ({len(errors)} violations)'); sys.exit(1)
print(f'Checked {len(texts)} production source/resource files; deleted resource references, old tokens, colors, type, geometry and dividers: zero violations.')
print('PASS')
