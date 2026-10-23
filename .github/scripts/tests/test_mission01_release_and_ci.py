"""Host-side Mission 01 CI anti-regression checks: executed before Android builds."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]
BUILD = ROOT / ".github/workflows/build.yml"
GRADLE = ROOT / "app/build.gradle.kts"
COLORS = ROOT / "app/src/main/res"
SRC_RESULT = ROOT / "app/src/main/java/com/eve/app/ui/result/ResultActivity.kt"
TEST_RESULT = ROOT / "app/src/test/java/com/eve/app/designsystem/TelegramResultSubtitleSourceTest.kt"
NATIVE_TEST = ROOT / "app/src/androidTest/java/com/eve/app/ui/LockedFoundationTest.kt"


class Mission01GateTest(unittest.TestCase):
    def test_compile_and_unit_assertions_gate_both_emulator_jobs(self):
        workflow = BUILD.read_text()
        build = workflow.split("\n  build:\n", 1)[1].split("\n  native-ui:\n", 1)[0]
        native = workflow.split("\n  native-ui:\n", 1)[1].split("\n  backend:\n", 1)[0]
        publish = workflow.split("\n  publish-apk:\n", 1)[1]
        for task in (":app:compileDebugKotlin", ":app:compileDebugUnitTestKotlin",
                     ":app:processDebugResources"):
            self.assertIn(task, build)
        self.assertLess(build.index("compileDebugUnitTestKotlin"),
                        build.index("gradle lintDebug testDebugUnitTest"))
        self.assertLess(build.index("gradle lintDebug testDebugUnitTest"),
                        build.index("gradle assembleDebug assembleDebugAndroidTest"))
        self.assertRegex(native, r"(?m)^    needs: build$")
        self.assertIn("matrix:\n        navigation: [gesture, three-button]", native)
        self.assertIn("reactivecircus/android-emulator-runner@v2", native)
        self.assertIn("--enforce", native)
        self.assertIn("needs: [build, native-ui, backend, firebase-functions]", publish)
        self.assertIn("if: ${{ success() }}", publish)
        self.assertNotIn("continue-on-error: true", workflow)

    def test_release_never_falls_back_to_debug_key(self):
        gradle = GRADLE.read_text()
        release = gradle.split('create("release") {', 1)[1].split("\n        }\n    }\n\n    buildTypes", 1)[0]
        self.assertIn("isReleaseTaskRequested", release)
        self.assertIn("!validKeystore || !validCredentials", release)
        self.assertIn("throw org.gradle.api.GradleException", release)
        self.assertIn("storeFile = file(storeFilePath!!)", release)
        self.assertNotIn('file("eve-debug.keystore")', release)
        self.assertIn('getByName("debug")', gradle)
        self.assertIn('signingConfig = signingConfigs.getByName("release")', gradle)

    def test_historical_unresolved_result_reference_is_declared_and_source_synced(self):
        source = SRC_RESULT.read_text()
        assert "val attemptDate = intent.getStringExtra(Constants.EXTRA_ATTEMPT_DATE)" in source
        self.assertIn("Attempted on $attemptDate", source)
        test = TEST_RESULT.read_text()
        self.assertIn("Attempted on ", test)
        self.assertNotIn('source.contains("attemptDate")', test)

    def test_theme_assertions_follow_source_derived_token_in_both_modes(self):
        native = NATIVE_TEST.read_text()
        self.assertIn("listOf(false, true)", native)
        self.assertIn("context.getColor(R.color.telegram_type_primary)", native)
        self.assertNotRegex(native, r"assertEquals\(\s*(?:Color\.BLACK|android\.graphics\.Color\.BLACK)")
        import xml.etree.ElementTree as ET
        for folder, expected in (("values", "#FF1A1D21"),
                                 ("values-night", "#FFFFFFFF")):
            root = ET.parse(COLORS / folder / "telegram_colors.xml").getroot()
            matches = [c.text.strip() for c in root.findall("color")
                       if c.get("name") == "telegram_type_primary"]
            self.assertEqual([expected], matches)


if __name__ == "__main__":
    unittest.main()
