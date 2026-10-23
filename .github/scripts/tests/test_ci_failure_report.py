"""CI failure diagnostic unit tests: classify source causes, never mask failures."""
import sys
from pathlib import Path
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_failure_report import classify_gradle_log, summarize_native_xml, main


class FailureReportTest(unittest.TestCase):
    def test_kotlin_compiler_error_over_generic_gradle_stacktrace(self):
        category, details = classify_gradle_log(
            "> Task :app:compileDebugUnitTestKotlin FAILED\n"
            "e: file:///tmp/ExampleTest.kt:23:18 Unresolved reference: number\n"
            "FAILURE: Build failed with an exception.\n"
        )
        self.assertEqual("KOTLIN_OR_JAVA_SOURCE", category)
        self.assertIn("ExampleTest.kt", details[0])

    def test_resource_failure(self):
        category, _ = classify_gradle_log("Android resource linking failed\nAAPT: error: resource color/missing not found")
        self.assertEqual("AAPT_OR_RESOURCE", category)

    def test_junit_failure_is_retained(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "result.xml").write_text(
                '<testsuite><testcase classname="eve.ThemeTest" name="light">'
                '<failure message="Expected WHITE but saw BLACK"/></testcase></testsuite>'
            )
            found = summarize_native_xml(root)
            self.assertEqual(1, len(found))
            self.assertIn("eve.ThemeTest.light", found[0])
            self.assertIn("Expected WHITE", found[0])

    def test_empty_diagnostics_do_not_change_original_ci_failure(self):
        self.assertEqual(0, main(["nonexistent-ci-log.txt"]))
        self.assertEqual([], summarize_native_xml(Path("missing-native-test-reports")))


if __name__ == "__main__":
    unittest.main()
