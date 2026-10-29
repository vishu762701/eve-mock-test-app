"""Machine-checkable regression tests for EVE engineering governance and CI safety gates."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]


def read(rel_path: str) -> str:
    return (ROOT / rel_path).read_text(encoding="utf-8")


class EngineeringGovernanceTest(unittest.TestCase):
    def test_authoritative_files_contain_g0_through_g8_gates(self):
        standard = read("ENGINEERING_STANDARD.md")
        agents = read("AGENTS.md")
        gates = ["G0", "G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8"]
        for gate in gates:
            self.assertIn(gate, standard, f"Gate {gate} missing from ENGINEERING_STANDARD.md")
            self.assertIn(gate, agents, f"Gate {gate} missing from AGENTS.md")

        # Specific gate semantics in AGENTS.md and ENGINEERING_STANDARD.md
        for term in (
            "PRE-FLIGHT",
            "OFFICIAL SOURCE",
            "SERIAL CHANGE",
            "FAST VALIDATION",
            "FUNCTIONAL ACCEPTANCE",
            "VISUAL ACCEPTANCE",
            "COMPLETION",
            "CONTINUITY",
            "SAFE AUTO-CORRECTION",
        ):
            self.assertIn(term, agents, f"Term '{term}' missing from AGENTS.md")

        # Source classification requirements
        for classification in ("EXACT", "ADAPTED", "NOT FOUND", "BLOCKED"):
            self.assertIn(classification, standard, f"Classification '{classification}' missing from ENGINEERING_STANDARD.md")
            self.assertIn(classification, agents, f"Classification '{classification}' missing from AGENTS.md")

    def test_intermediate_commits_forbidden_from_carrying_eve_full(self):
        standard = read("ENGINEERING_STANDARD.md")
        agents = read("AGENTS.md")
        self.assertIn("Never add [eve-full] to intermediate commits", standard)
        self.assertIn("Never append [eve-full] to intermediate commits", agents)

    def test_workflow_preserves_fast_vs_full_distinction(self):
        workflow = read(".github/workflows/build.yml")
        self.assertIn("full=false", workflow)
        self.assertIn('COMMIT_MESSAGE" == *"[eve-full]"*', workflow)
        self.assertIn('GITHUB_EVENT_NAME" == "workflow_dispatch"', workflow)
        self.assertIn("steps.verification_profile.outputs.full == 'true'", workflow)

    def test_no_ci_gate_disabled(self):
        workflow = read(".github/workflows/build.yml")
        # Ensure critical jobs exist
        for job in ("build:", "native-ui:", "backend:", "firebase-functions:", "publish-apk:"):
            self.assertIn(job, workflow, f"Critical job {job} missing from build.yml")

        # Ensure dependencies are strictly gated
        self.assertIn("needs: [build, backend, firebase-functions]", workflow)
        self.assertIn("needs: [build, native-ui, backend, firebase-functions]", workflow)
        self.assertIn("fail-on-cache-miss: true", workflow)
        self.assertIn("sha256sum --check --status", workflow)
        self.assertIn(":app:testDebugUnitTest", workflow)

    def test_mission_acceptance_template_exists_and_complete(self):
        template_path = ROOT / "docs/MISSION_ACCEPTANCE_TEMPLATE.md"
        self.assertTrue(template_path.is_file(), "docs/MISSION_ACCEPTANCE_TEMPLATE.md must exist")
        content = template_path.read_text(encoding="utf-8")
        required_fields = [
            "Baseline SHA",
            "Final SHA",
            "Mission Scope",
            "Sequential Microstep Commit Ledger",
            "FAST Run Result",
            "Upstream Telegram Provenance",
            "Functional Acceptance",
            "Visual Fidelity",
            "Regressions",
            "Exceptions",
            "Terminal FULL CI",
            "VERIFIED",
            "FAILED",
            "UNKNOWN",
            "BLOCKED",
            "Remaining Actions",
            "Resumable Continuation Checkpoint",
        ]
        for field in required_fields:
            self.assertIn(field, content, f"Field '{field}' missing from docs/MISSION_ACCEPTANCE_TEMPLATE.md")

    def test_agent_entrypoint_hierarchy_preserved(self):
        for path in ("AGENTS.md", "GEMINI.md", "CLAUDE.md"):
            content = read(path)
            self.assertTrue(
                content.startswith("Always read and follow ENGINEERING_STANDARD.md first. Then read SOURCE_OF_TRUTH.md."),
                f"{path} does not start with canonical instruction entrypoint",
            )


if __name__ == "__main__":
    unittest.main()
