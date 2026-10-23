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

    def test_compile_gates_run_before_expensive_emulators(self):
        """Check that FAST compiles test sources and FULL emulators depend on that gate."""
        workflow = (ROOT / ".github/workflows/build.yml").read_text()
        build = workflow.split("\n  build:\n", 1)[1].split("\n  native-ui:\n", 1)[0]
        native = workflow.split("\n  native-ui:\n", 1)[1].split("\n  backend:\n", 1)[0]
        compile_step = build.index("name: FAST compile debug APK and JVM test sources")
        fast_command = ":app:assembleDebug :app:compileDebugUnitTestKotlin"
        full_command = ":app:compileDebugAndroidTestKotlin :app:lintDebug :app:testDebugUnitTest"
        self.assertIn(fast_command, build[compile_step:])
        self.assertIn(full_command, build[compile_step:])
        self.assertLess(build.index(fast_command), build.index(full_command))
        self.assertIn("needs: build", native)
        self.assertIn("navigation: [gesture, three-button]", native)
        self.assertIn("github.event.head_commit.message", native)
        self.assertIn("needs: [build, native-ui, backend, firebase-functions]", workflow)
        self.assertIn("name: Eve-debug-apk", workflow)


if __name__ == "__main__":
    unittest.main()
