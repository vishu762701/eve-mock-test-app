"""Guard autonomous but source-faithful implementation and portable build preflight."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]

def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")

class TelegramContinuousExecutionContractTest(unittest.TestCase):
    def test_authorized_telegram_work_needs_no_duplicate_approval(self):
        standard = read("ENGINEERING_STANDARD.md")
        process = read("docs/telegram-integration-process.md")
        self.assertIn("without asking for a second approval", standard)
        self.assertIn("Owner authorization is the feature requirement itself", process)
        self.assertIn("Never treat Python tests or a compiling APK as proof", process)
        self.assertIn("exact-SHA FAST", standard)
        self.assertNotIn("needs owner approval first", standard)
        source_of_truth = read("SOURCE_OF_TRUTH.md")
        self.assertIn("continuous Analysis, Mapping, Implementation and Verification", source_of_truth)
        self.assertNotIn("gated Analysis, Mapping, Approval", source_of_truth)
        self.assertIn("separate explicit owner authorization", standard)

    def test_telegram_source_verification_is_pinned_and_official(self):
        workflow = read(".github/workflows/verify-official-telegram-imports.yml")
        script = read(".github/scripts/verify-official-telegram-imports.sh")
        self.assertIn("workflow_dispatch:", workflow)
        self.assertIn("branches: [ main ]", workflow)
        self.assertIn("https://github.com/DrKLO/Telegram.git", script)
        self.assertIn("--filter=blob:none", script)
        self.assertIn("verify_telegram_imports.py", script)
        self.assertIn("--upstream", script)
        self.assertNotIn("|| true", script)

    def test_portable_gradle_launcher_has_no_termux_dependency(self):
        launcher = read("gradlew")
        self.assertIn("command -v gradle", launcher)
        self.assertIn("gradle-wrapper.jar", launcher)
        self.assertNotIn("/data/data/com.termux", launcher)

if __name__ == "__main__":
    unittest.main()
