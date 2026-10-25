"""Keep indeterminate loading on the Telegram leaf while preserving determinate metrics."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
ANDROID = '{http://schemas.android.com/apk/res/android}'


class LoadingIndicatorContractTest(unittest.TestCase):
    def test_platform_and_material_circles_are_determinate_only(self):
        circles = []
        for path in (ROOT / 'app/src/main/res').glob('layout*/*.xml'):
            for view in ET.parse(path).iter():
                if view.tag in ('ProgressBar', 'com.google.android.material.progressindicator.CircularProgressIndicator'):
                    if 'Horizontal' in view.attrib.get('style', ''):
                        continue
                    circles.append(path.name)
                    self.assertEqual('false', view.attrib.get(ANDROID + 'indeterminate'), str(path))
        self.assertTrue(circles, 'Determinate result metrics must remain present')

    def test_no_platform_spinner_constructors_remain(self):
        for path in (ROOT / 'app/src/main/java').rglob('*.kt'):
            self.assertIsNone(re.search(r'(?<!\w)ProgressBar\s*\(', path.read_text()), str(path))

    def test_question_reformat_binding_matches_inflated_view_type(self):
        path = ROOT / 'app/src/main/res/layout/dialog_edit_question_details.xml'
        view = next(v for v in ET.parse(path).iter() if v.attrib.get(ANDROID + 'id') == '@+id/pbReformat')
        self.assertEqual('com.eve.app.designsystem.TelegramRadialProgressView', view.tag)
        source = (ROOT / 'app/src/main/java/com/eve/app/ui/admin/EditExamActivity.kt').read_text()
        self.assertIn('findViewById<TelegramRadialProgressView>(R.id.pbReformat)', source)
