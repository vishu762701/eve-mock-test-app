"""Regression tests for streamlined 1-screen/1-feature execution and real security/CI gates."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


class EngineeringGovernanceTest(unittest.TestCase):
    def test_canonical_policy_and_source_process(self):
        standard = read("ENGINEERING_STANDARD.md")
        for path in ("AGENTS.md", "backend/AGENTS.md", "GEMINI.md", "CLAUDE.md"):
            self.assertIn("ENGINEERING_STANDARD.md", read(path))
        self.assertIn("1 screen or 1 feature at a time", standard)

    def test_telegram_screen_requires_original_renderer_and_acceptance(self):
        standard = read("ENGINEERING_STANDARD.md")
        self.assertIn("original pinned Telegram Java Activity/Fragment", standard)
        self.assertIn("Do not begin another screen until accepted", standard)
        self.assertIn("NO KOTLIN/XML/COMPOSE SCREEN RECREATION", standard)
        self.assertIn("docs/telegram-profile-fidelity.json", standard)
        self.assertIn("TelegramMessenger/Telegram-Android", standard)

    def test_main_push_is_compile_only_and_manual_full_is_available(self):
        workflow = read(".github/workflows/build.yml")
        self.assertIn("branches: [ main ]", workflow)
        self.assertIn(":app:assembleDebug", workflow)
        self.assertIn("workflow_dispatch", workflow)

    def test_backend_rules_and_production_release_separation(self):
        deployment = read(".github/workflows/deploy-worker.yml")
        self.assertIn("workflow_dispatch:", deployment)
        self.assertNotRegex(deployment, r"(?m)^  push:")
        self.assertIn("if: ${{ inputs.confirm_production == true }}", deployment)

    def test_android_fast_full_and_apk_checks_preserved(self):
        workflow = read(".github/workflows/build.yml")
        for item in ("workflow_dispatch", ":app:testDebugUnitTest",
                     "navigation: [gesture, three-button]"):
            self.assertIn(item, workflow)

    def test_entrypoints_and_acceptance_template(self):
        for path in ("AGENTS.md", "GEMINI.md", "CLAUDE.md"):
            self.assertTrue(read(path).startswith(
                "Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md."))


if __name__ == "__main__":
    unittest.main()
