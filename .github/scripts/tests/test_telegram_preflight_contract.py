"""Host-side regression guards for Telegram Mission 01.

These checks supplement, not replace, Kotlin unit-test compilation and
both native UI instrumentation navigation modes.
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[3]
RES = ROOT / "app/src/main/res"


class TelegramPreflightContractTest(unittest.TestCase):
    def test_native_default_text_resolves_telegram_ink_in_both_themes(self):
        source = (ROOT / "app/src/androidTest/java/com/eve/app/ui/LockedFoundationTest.kt").read_text()
        self.assertIn("for (dark in listOf(false, true))", source)
        self.assertRegex(source,
            r"assertEquals\(\s*context\.getColor\(R\.color\.telegram_type_primary\),\s*text\.currentTextColor\s*\)")
        values = []
        for qualifier in ("values", "values-night"):
            colors = ET.parse(RES / qualifier / "telegram_colors.xml").getroot()
            by_name = {node.attrib["name"]: (node.text or "").strip()
                       for node in colors.findall("color")}
            value = by_name["telegram_type_primary"]
            self.assertRegex(value, r"^#[0-9A-Fa-f]{8}$")
            values.append(value)
        self.assertNotEqual(*values)

    def test_compile_unit_test_sources_before_expensive_assemble(self):
        workflow = (ROOT / ".github/workflows/build.yml").read_text()
        preflight = workflow.index("name: Preflight Android resources and Kotlin unit-test compilation")
        step = workflow[preflight:preflight + 260]
        self.assertIn(":app:compileDebugUnitTestKotlin", step)
        self.assertIn(":app:processDebugResources", step)
        self.assertLess(preflight, workflow.index("name: Build debug and instrumentation APKs"))
        self.assertIn("needs: [build, native-ui, backend, firebase-functions]", workflow)


if __name__ == "__main__":
    unittest.main()
