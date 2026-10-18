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

    def test_authoritative_ios26_system_palette(self):
        expected_light = {
            'eve_system_red': '#FF383C',
            'eve_system_orange': '#FF8D28',
            'eve_system_yellow': '#FFCC00',
            'eve_system_green': '#34C759',
            'eve_system_mint': '#00C8B3',
            'eve_system_teal': '#00C3D0',
            'eve_system_cyan': '#00C0E8',
            'eve_system_blue': '#0088FF',
            'eve_system_indigo': '#6155F5',
            'eve_system_purple': '#CB30E0',
            'eve_system_pink': '#FF2D55',
            'eve_system_brown': '#AC7F5E',
            'eve_system_gray': '#8E8E93',
            'eve_system_gray2': '#AEAEB2',
            'eve_system_gray3': '#C7C7CC',
            'eve_system_gray4': '#D1D1D6',
            'eve_system_gray5': '#E5E5EA',
            'eve_system_gray6': '#F2F2F7',
            'eve_system_red_hc': '#D70015',
            'eve_system_orange_hc': '#C93400',
            'eve_system_yellow_hc': '#B25000',
            'eve_system_green_hc': '#248A3D',
            'eve_system_mint_hc': '#0C817B',
            'eve_system_teal_hc': '#008299',
            'eve_system_cyan_hc': '#0071A4',
            'eve_system_blue_hc': '#0040DD',
            'eve_system_indigo_hc': '#3634A3',
            'eve_system_purple_hc': '#8944AB',
            'eve_system_pink_hc': '#D30F45',
            'eve_system_brown_hc': '#7F6553',
            'eve_system_gray_hc': '#6C6C70',
            'eve_system_gray2_hc': '#8E8E93',
            'eve_system_gray3_hc': '#AEAEB2',
            'eve_system_gray4_hc': '#BCBCC0',
            'eve_system_gray5_hc': '#D8D8DC',
            'eve_system_gray6_hc': '#EBEBF0',
        }
        expected_dark = {
            'eve_system_red': '#FF4245',
            'eve_system_orange': '#FF9230',
            'eve_system_yellow': '#FFD600',
            'eve_system_green': '#30D158',
            'eve_system_mint': '#00DAC3',
            'eve_system_teal': '#00D2E0',
            'eve_system_cyan': '#3CD3FE',
            'eve_system_blue': '#0091FF',
            'eve_system_indigo': '#6D7CFF',
            'eve_system_purple': '#DB34F2',
            'eve_system_pink': '#FF375F',
            'eve_system_brown': '#B78A66',
            'eve_system_gray': '#8E8E93',
            'eve_system_gray2': '#636366',
            'eve_system_gray3': '#48484A',
            'eve_system_gray4': '#3A3A3C',
            'eve_system_gray5': '#2C2C2E',
            'eve_system_gray6': '#1C1C1E',
            'eve_system_red_hc': '#FF6961',
            'eve_system_orange_hc': '#FFB340',
            'eve_system_yellow_hc': '#FFD426',
            'eve_system_green_hc': '#30DB5B',
            'eve_system_mint_hc': '#63E6E2',
            'eve_system_teal_hc': '#40CBE0',
            'eve_system_cyan_hc': '#70D7FF',
            'eve_system_blue_hc': '#409CFF',
            'eve_system_indigo_hc': '#7D7AFF',
            'eve_system_purple_hc': '#DA8FFF',
            'eve_system_pink_hc': '#FF6482',
            'eve_system_brown_hc': '#B59470',
            'eve_system_gray_hc': '#AEAEB2',
            'eve_system_gray2_hc': '#8E8E93',
            'eve_system_gray3_hc': '#636366',
            'eve_system_gray4_hc': '#48484A',
            'eve_system_gray5_hc': '#3A3A3C',
            'eve_system_gray6_hc': '#2C2C2E',
        }
        light_colors = design.colors('values')
        for token, hex_val in expected_light.items():
            self.assertEqual(hex_val, light_colors[token], f'Light token mismatch: {token}')
        dark_colors = design.colors('values-night')
        for token, hex_val in expected_dark.items():
            self.assertEqual(hex_val, dark_colors[token], f'Dark token mismatch: {token}')

    def test_semantic_color_system_aliases(self):
        for theme in ['values', 'values-night']:
            colors = design.colors(theme)
            self.assertEqual(colors['eve_system_fill'], colors['eve_fill_primary'])
            self.assertEqual(colors['eve_secondary_system_fill'], colors['eve_fill_secondary'])
            self.assertEqual(colors['eve_tertiary_system_fill'], colors['eve_fill_tertiary'])
            self.assertEqual(colors['eve_quaternary_system_fill'], colors['eve_fill_quaternary'])
            self.assertEqual(colors['eve_link'], colors['eve_system_blue'])
            self.assertEqual(colors['eve_dark_text'], '#000000')
            self.assertEqual(colors['eve_light_text'], '#FFFFFF')
            self.assertEqual(colors['eve_clear'], '#00000000')

    def test_app_wide_locked_foundation_colors(self):
        expected_light = {
            'eve_color_background': '#EEF2F8',
            'eve_color_card': '#FFFFFF',
            'eve_color_text_primary': '#000000',
            'eve_color_text_secondary': '#6C6C70',
            'eve_color_hairline': '#1A000000',
            'eve_color_tab_track': '#29787880',
            'eve_color_accent': '#000000',
            'eve_color_on_accent': '#FFFFFF',
            'eve_color_success': '#34C759',
            'eve_color_error': '#FF383C',
            'eve_color_warning': '#FF8D28',
            'eve_color_floating_bar_bg': '#FFFFFF',
            'eve_color_card_shadow': '#1A000000',
        }
        expected_dark = {
            'eve_color_background': '#0B0F16',
            'eve_color_card': '#161B24',
            'eve_color_text_primary': '#FFFFFF',
            'eve_color_text_secondary': '#A1A1A6',
            'eve_color_hairline': '#24FFFFFF',
            'eve_color_tab_track': '#52787880',
            'eve_color_accent': '#FFFFFF',
            'eve_color_on_accent': '#000000',
            'eve_color_success': '#30D158',
            'eve_color_error': '#FF4245',
            'eve_color_warning': '#FF9230',
            'eve_color_floating_bar_bg': '#2C2C2E',
            'eve_color_card_shadow': '#00000000',
        }
        aliases = [
            'background', 'card', 'text_primary', 'text_secondary',
            'hairline', 'tab_track', 'accent', 'on_accent',
            'success', 'error', 'warning', 'floating_bar_bg', 'card_shadow'
        ]
        light_colors = design.colors('values')
        for token, hex_val in expected_light.items():
            self.assertEqual(hex_val, light_colors[token], f'Light app token mismatch: {token}')
        dark_colors = design.colors('values-night')
        for token, hex_val in expected_dark.items():
            self.assertEqual(hex_val, dark_colors[token], f'Dark app token mismatch: {token}')

        for theme in ['values', 'values-night']:
            colors = design.colors(theme)
            for role in aliases:
                app_token = f'eve_app_{role}'
                color_token = f'eve_color_{role}'
                self.assertEqual(colors[app_token], colors[color_token], f'{theme}: {app_token} must equal {color_token}')
