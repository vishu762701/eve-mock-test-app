"""Regression tests for streamlined batch execution without removing real security/CI gates."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


class EngineeringGovernanceTest(unittest.TestCase):
    def test_canonical_policy_and_source_process(self):
        standard = read("ENGINEERING_STANDARD.md")
        process = read("docs/telegram-integration-process.md")
        for term in ("maximum two successful implementation commits", "preferably one",
                     "CI-failed commits do not count", "owner approval for an exception",
                     "exact-SHA FAST", "minimum complete functional subsystem",
                     "full commit SHA", "EXACT", "ADAPTED", "NOT FOUND", "BLOCKED"):
            self.assertIn(term, standard)
        for term in ("latest suitable official revision", "transitive classes", "lifecycle",
                     "license", "runtime"):
            self.assertIn(term, process)
        for path in ("AGENTS.md", "backend/AGENTS.md", "GEMINI.md", "CLAUDE.md"):
            self.assertIn("ENGINEERING_STANDARD.md", read(path))
        for path in ("AGENTS.md", "ENGINEERING_STANDARD.md", "SOURCE_OF_TRUTH.md",
                     "backend/AGENTS.md", "docs/EVE_ONE_TIME_CHAT_STARTER.md"):
            self.assertNotRegex(read(path), r"(?i)no fixed commit count|no commit limit")

    def test_every_main_push_triggers_build(self):
        workflow = read(".github/workflows/build.yml")
        push = workflow.split("  push:\n", 1)[1].split("  pull_request:\n", 1)[0]
        self.assertIn("branches: [ main ]", push)
        self.assertNotIn("paths-ignore:", push)
        self.assertNotRegex(push, r"(?m)^    paths:")

    def test_backend_rules_and_production_release_separation(self):
        deployment = read(".github/workflows/deploy-worker.yml")
        standard = read("ENGINEERING_STANDARD.md")
        self.assertIn("separate explicit owner authorization", standard)
        self.assertIn("workflow_dispatch:", deployment)
        self.assertNotRegex(deployment, r"(?m)^  push:")
        self.assertIn("if: ${{ inputs.confirm_production == true }}", deployment)

    def test_android_fast_full_and_apk_checks_preserved(self):
        workflow = read(".github/workflows/build.yml")
        for item in ("full=false", "[eve-full]", ":app:testDebugUnitTest",
                     "navigation: [gesture, three-button]",
                     "needs: [build, backend, firebase-functions]",
                     "needs: [build, native-ui, backend, firebase-functions]",
                     "fail-on-cache-miss: true", "sha256sum --check --status"):
            self.assertIn(item, workflow)

    def test_entrypoints_and_acceptance_template(self):
        for path in ("AGENTS.md", "GEMINI.md", "CLAUDE.md"):
            self.assertTrue(read(path).startswith(
                "Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md."))
        template = read("docs/MISSION_ACCEPTANCE_TEMPLATE.md")
        for item in ("Batch Commit Ledger", "FAST CI Run ID", "Functional Acceptance",
                     "Visual Fidelity", "Terminal FULL CI", "Resumable Continuation Checkpoint"):
            self.assertIn(item, template)


if __name__ == "__main__":
    unittest.main()
