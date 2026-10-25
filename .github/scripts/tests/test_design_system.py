import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / 'check_design_system.py'
spec = importlib.util.spec_from_file_location('design_system', SCRIPT)
design = importlib.util.module_from_spec(spec)
spec.loader.exec_module(design)

class DesignSystemTest(unittest.TestCase):
    def test_readable_roles_on_actual_surfaces(self):
        rows = design.verify()
        self.assertEqual({'values', 'values-night'}, {row[0] for row in rows})
        self.assertTrue(any(row[1] == 'text_secondary' and row[2] == 'surface_inset' for row in rows))

    def test_alpha_is_composited_before_contrast(self):
        self.assertAlmostEqual(1., design.contrast('#00FFFFFF', '#000000', '#000000'))
        self.assertLess(design.contrast('#80FFFFFF', '#000000'), design.contrast('#FFFFFF', '#000000'))



    def test_role_contrast_without_freezing_visual_values(self):
        for theme in ['values', 'values-night']:
            colors = design.colors(theme)
            self.assertNotEqual(colors['text_primary'], colors['text_secondary'])
            self.assertGreaterEqual(
                design.contrast(colors['on_accent'], colors['accent'], colors['bg_app']), 4.5
            )

    def test_theme_resolves_mono_primary_and_solid_surface(self):
        import xml.etree.ElementTree as ET
        root = ET.parse(design.RES / 'values/themes.xml').getroot()
        theme = root.find("style[@name='Theme.Eve.Foundation']")
        slots = {node.attrib['name']: node.text.strip() for node in theme}
        for name, token in [('colorPrimary','accent'), ('colorOnPrimary','on_accent'), ('colorSurface','surface'), ('android:windowBackground','bg_app')]:
            self.assertEqual('@color/' + token, slots[name])

    def test_app_wide_background_and_text_pairs(self):
        for theme in ['values','values-night']:
            colors = design.colors(theme)
            for bg in ['bg_app','surface','surface_inset']:
                self.assertGreaterEqual(design.contrast(colors['text_secondary'], colors[bg], colors['bg_app']), 4.5)

    def test_system_hues_and_containers_are_well_formed(self):
        for theme in ['values', 'values-night']:
            colors = design.colors(theme)
            for hue in ['red', 'green', 'blue', 'indigo', 'orange', 'purple', 'yellow']:
                token = 'system_' + hue
                self.assertRegex(colors[token], r'^#[0-9A-Fa-f]{6,8}$')
                self.assertRegex(colors[token + '_container'], r'^#[0-9A-Fa-f]{6,8}$')
