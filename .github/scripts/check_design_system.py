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
    backgrounds = ['eve_system_background','eve_secondary_system_background','eve_tertiary_system_background',
                   'eve_system_grouped_background','eve_secondary_system_grouped_background','eve_tertiary_system_grouped_background',
                   'eve_input_fill','eve_recovery_surface']
    for theme in ['values','values-night']:
        c = colors(theme)
        for fg in ['eve_label_primary','eve_label_secondary','eve_label_tertiary','eve_label_quaternary','eve_placeholder_text','eve_accent_text','eve_destructive_text','eve_status_success_text','eve_status_error_text','eve_status_warning_text']:
            for bg in backgrounds:
                rows.append((theme,fg,bg,contrast(c[fg],c[bg],c['eve_canvas']),4.5))
        for fg,bg in [('eve_button_soft_text','eve_button_soft_bg'),
                      ('eve_disabled_text','eve_disabled_fill'),('eve_error_text','eve_error_bg'),('eve_offline_text','eve_offline_bg'),('eve_recovery_accent','eve_recovery_selected'),
                      ('eve_hc_accent','eve_accent_container'),('eve_hc_secondary','eve_input_fill')]:
            rows.append((theme,fg,bg,contrast(c[fg],c[bg],c['eve_canvas']),4.5))
        rows.append((theme,'eve_white','eve_primary',contrast(c['eve_white'],c['eve_primary'],c['eve_canvas']),3.))
        rows.append((theme,'eve_on_primary','eve_primary',contrast(c['eve_on_primary'],c['eve_primary'],c['eve_canvas']),3.))
        for bg in backgrounds:
            rows.append((theme,'eve_control_outline',bg,contrast(c['eve_control_outline'],c[bg],c['eve_canvas']),3.))
        assert c['eve_canvas'] == ('#FFFFFF' if theme == 'values' else '#000000')
    failures = [row for row in rows if row[3] < row[4]]
    if failures:
        raise AssertionError(json.dumps(failures,indent=2))
    return rows

if __name__ == '__main__':
    rows = verify()
    print(f'PASS: {len(rows)} composited semantic contrast pairs; minimum text {min(r[3] for r in rows if r[4] == 4.5):.2f}:1')
    for theme,fg,bg,ratio,_ in rows:
        if bg == 'eve_primary': print(f'{theme}: {fg} / {bg} = {ratio:.2f}:1')
