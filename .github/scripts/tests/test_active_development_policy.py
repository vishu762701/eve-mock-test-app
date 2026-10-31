"""Keep owner-led UI freedom while preserving genuine release and test gates."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

class ActiveDevelopmentPolicyTest(unittest.TestCase):
    def test_principal_engineer_owns_changes(self):
        rules = read("ENGINEERING_STANDARD.md")
        self.assertIn("No UI/UX choice is permanently locked", rules)
        self.assertIn("Accountable principal-engineer execution", rules)
        self.assertIn("Automatic unused-file cleanup", rules)
        self.assertIn("Official design / source reuse", rules)

    def test_strict_owner_scope_and_official_only(self):
        standard = read("ENGINEERING_STANDARD.md")
        agent = read("AGENTS.md")
        architecture = read("SOURCE_OF_TRUTH.md")
        self.assertIn("Owner-authorized scope and exact official implementation", standard)
        self.assertIn("Never use an alternative unless the owner explicitly authorizes it", standard)
        self.assertIn("Do not silently add unrelated features", standard)
        self.assertIn("Never use or build an alternative", agent)
        self.assertIn("Do not delete unrelated orphan files", agent)
        self.assertIn("ONLY change scope", architecture)

    def test_no_blanket_source_requirement(self):
        entry = read("AGENTS.md")
        self.assertIn("Telegram ZIP is necessary ONLY", entry)
        self.assertIn("never block unrelated tasks", entry)

    def test_historical_ui_not_forbidden_in_ci(self):
        build = read(".github/workflows/build.yml")
        self.assertNotIn("Retired UI Studio references remain in active source", build)
        self.assertIn(":app:testDebugUnitTest", build)
        self.assertIn("navigation: [gesture, three-button]", build)

    def test_true_distribution_safeguards_still_present(self):
        release = read(".github/workflows/release.yml")
        self.assertIn("EVE_GPL_RELEASE_APPROVED", release)
        self.assertIn("Validate release signing secrets", release)

    def test_no_obsolete_fixed_root_palette_gate(self):
        kotlin = read("app/src/test/java/com/eve/app/designsystem/EveDesignSystemTest.kt")
        self.assertIn("semanticPaletteRolesAreValidInBothThemes", kotlin)
        self.assertIn("responsiveDimensionsRemainValidAcrossBreakpoints", kotlin)
        self.assertNotIn("rootPaletteMatchesLockedDayNightRoles", kotlin)

if __name__ == "__main__":
    unittest.main()
