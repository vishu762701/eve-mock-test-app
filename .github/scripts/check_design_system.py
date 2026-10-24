"""Resolve Android aliases and composite alpha before measuring semantic contrast."""
from pathlib import Path
import json
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / 'app/src/main/res'

def colors(qualifier):
    result = {}
    for folder in ['values'] + ([] if qualifier == 'values' else [qualifier]):
        for path in (RES / folder).glob('*.xml'):
            for node in ET.parse(path).getroot().findall('color'):
                result[node.attrib['name']] = node.text.strip()
    def resolve(name, seen=()):
        if name in seen:
            raise ValueError('Cyclic color alias: ' + name)
        value = result[name]
        if value.startswith('@color/'):
            return resolve(value[7:], seen + (name,))
        if value == '@android:color/transparent':
            return '#00000000'
        return value
    return {name: resolve(name) for name in result}

def rgba(value):
    raw = value.lstrip('#')
    if len(raw) == 6:
        raw = 'FF' + raw
    a, r, g, b = [int(raw[i:i+2], 16) / 255 for i in range(0, 8, 2)]
    return r, g, b, a

def composite(fg, bg):
    return tuple(fg[i] * fg[3] + bg[i] * (1-fg[3]) for i in range(3)) + (1.,)

def luminance(color):
    linear = [v/12.92 if v <= .04045 else ((v+.055)/1.055)**2.4 for v in color[:3]]
    return sum(a*b for a,b in zip(linear, [.2126,.7152,.0722]))

def contrast(fg, bg, canvas='#FFFFFF'):
    background = composite(rgba(bg), rgba(canvas))
    a, b = sorted([luminance(composite(rgba(fg), background)), luminance(background)])
    return (b+.05)/(a+.05)

def verify():
    rows = []
    for theme in ['values', 'values-night']:
        c = colors(theme)
        for fg in ['text_primary', 'text_secondary']:
            for bg in ['bg_app', 'surface', 'surface_inset']:
                ratio = contrast(c[fg], c[bg], c['bg_app'])
                rows.append((theme, fg, bg, ratio, 4.5))
                assert ratio >= 4.5, (theme, fg, bg, ratio)
        ratio = contrast(c['on_accent'], c['accent'], c['bg_app'])
        rows.append((theme, 'on_accent', 'accent', ratio, 4.5))
        assert ratio >= 4.5, (theme, 'on_accent', ratio)
        for hue in ['red','green','blue','indigo','orange','purple','yellow']:
            fg = 'system_' + hue
            ratio = contrast(c[fg], c['surface'], c['bg_app'])
            rows.append((theme, fg, 'surface', ratio, 4.5))
            if ratio < 4.5:
                print(f'LOCKED SYSTEM HUE: {theme} {fg}/surface = {ratio:.2f}:1; preserve the specified color')
    return rows

if __name__ == '__main__':
    rows = verify()
    print(f'PASS: neutral role contrast; {len(rows)} measured pairs, exact system-hue exceptions listed above.')
