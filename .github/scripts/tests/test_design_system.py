import importlib.util
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / 'check_design_system.py'
spec = importlib.util.spec_from_file_location('design_system', SCRIPT)
design = importlib.util.module_from_spec(spec)
spec.loader.exec_module(design)

class DesignSystemTest(unittest.TestCase):
    def test_readable_roles_on_actual_surfaces(self):
        self.assertGreater(len(design.verify()), 100)

    def test_alpha_is_composited_before_contrast(self):
        self.assertAlmostEqual(1., design.contrast('#00FFFFFF', '#000000', '#000000'))
        self.assertLess(design.contrast('#80FFFFFF', '#000000'), design.contrast('#FFFFFF', '#000000'))

    def test_recovery_aliases_use_one_foundation(self):
        for theme in ['values','values-night']:
            colors = design.colors(theme)
            for old, canonical in [('eve_recovery_accent','eve_accent_text'),
                                   ('eve_recovery_text_secondary','eve_label_secondary'),
                                   ('eve_recovery_selected','eve_accent_container')]:
                self.assertEqual(colors[old], colors[canonical])

    def test_checked_in_protected_palette(self):
        import json
        baseline = json.loads((SCRIPT.parent / 'tests/fixtures/protected_design_colors.json').read_text())
        for theme, expected in baseline.items():
            actual = design.colors(theme)
            for token, value in expected.items():
                self.assertEqual(value, actual[token], f'{theme}: protected {token}')
