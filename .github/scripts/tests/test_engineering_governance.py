"""Regression tests for streamlined batch execution without removing real security/CI gates."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


class EngineeringGovernanceTest(unittest.TestCase):
    def test_g0_g8_and_source_authenticity(self):
        entry = read("AGENTS.md")
        standard = read("ENGINEERING_STANDARD.md")
        for gate in ("G0", "G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8"):
            self.assertIn(gate, entry)
            self.assertIn(gate, standard)
        for label in ("EXACT", "ADAPTED", "NOT FOUND", "BLOCKED"):
            self.assertIn(label, entry)
            self.assertIn(label, standard)
        self.assertIn("Never use or build an alternative", entry)

    def test_batch_commits_without_microstep_tax(self):
        standard = read("ENGINEERING_STANDARD.md")
        entry = read("AGENTS.md")
        self.assertIn("No fixed commit count", standard)
        self.assertIn("Multiple coherent commits may be prepared and pushed together", standard)
        self.assertIn("Multiple coherent commits may be prepared and pushed together", entry)
        self.assertIn("final SHA of each pushed batch", standard)
        self.assertIn("one FAST gate on its final SHA", standard)
        self.assertIn("CI_RED halts", standard)
        self.assertIn("focused forward-only repair commit", standard)
        self.assertIn("Never disable, suppress, or delete legitimate test assertions", standard)
        self.assertIn("terminal", standard.lower())
        self.assertIn("[eve-full]", entry)

    def test_no_obsolete_commit_ceiling_in_active_entrypoints(self):
        for path in ("AGENTS.md", "ENGINEERING_STANDARD.md", "SOURCE_OF_TRUTH.md",
                     "backend/AGENTS.md", "GEMINI.md", "CLAUDE.md",
                     "docs/EVE_ONE_TIME_CHAT_STARTER.md"):
            content = read(path).lower()
            for obsolete in (r"\bone commit per feature\b", r"\bone feature per commit\b",
                             r"\beach commit (?:must|needs to) (?:pass|have) (?:fast|ci)\b"):
                self.assertNotRegex(content, obsolete, path)
        self.assertIn("minimum complete functional subsystem", read("ENGINEERING_STANDARD.md"))
        self.assertIn("official Git checkout", read("AGENTS.md"))

    def test_every_main_push_triggers_build(self):
        workflow = read(".github/workflows/build.yml")
        push = workflow.split("  push:\n", 1)[1].split("  pull_request:\n", 1)[0]
        self.assertIn("branches: [ main ]", push)
        self.assertNotIn("paths-ignore:", push)
        self.assertNotRegex(push, r"(?m)^    paths:")

    def test_backend_rules_and_production_release_separation(self):
        backend = read("backend/AGENTS.md")
        deployment = read(".github/workflows/deploy-worker.yml")
        standard = read("ENGINEERING_STANDARD.md")
        for phrase in ("npm test", "npm run build", "server-authoritative scoring",
                       "old-client", "manual", "confirm_production"):
            self.assertIn(phrase, backend)
        self.assertIn("Backend/server synchronization", standard)
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
