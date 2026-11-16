"""Guard source-faithful implementation and portable build preflight."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]


def read(rel):
    return (ROOT / rel).read_text(encoding="utf-8")


class TelegramContinuousExecutionContractTest(unittest.TestCase):
    def test_authorized_telegram_work_needs_no_duplicate_approval(self):
        standard = read("ENGINEERING_STANDARD.md")
        self.assertIn("1 screen or 1 feature at a time", standard)
        self.assertNotIn("needs owner approval first", standard)

    def test_telegram_source_verification_is_pinned_and_official(self):
        workflow = read(".github/workflows/verify-official-telegram-imports.yml")
        script = read(".github/scripts/verify-official-telegram-imports.sh")
        self.assertIn("workflow_dispatch:", workflow)
        self.assertIn("https://github.com/DrKLO/Telegram.git", script)
        self.assertIn("verify_telegram_imports.py", script)

    def test_portable_gradle_launcher_has_no_termux_dependency(self):
        launcher = read("gradlew")
        self.assertIn("command -v gradle", launcher)
        self.assertIn("gradle-wrapper.jar", launcher)
        self.assertNotIn("/data/data/com.termux", launcher)


if __name__ == "__main__":
    unittest.main()
