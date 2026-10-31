"""Keep owner-led UI freedom while preserving genuine release and test gates."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3]

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

class ActiveDevelopmentPolicyTest(unittest.TestCase):
    def test_principal_engineer_owns_changes(self):
        rules = read("ENGINEERING_STANDARD.md")
        for term in ("No UI/UX choice is permanently locked", "Own the complete authorized outcome",
                     "Remove only files proven obsolete", "Do not silently broaden product scope"):
            self.assertIn(term, rules)

    def test_official_source_scoped_to_requested_reuse(self):
        rules = read("ENGINEERING_STANDARD.md")
        for term in ("not unrelated work", "authenticate its official origin",
                     "Do not substitute an unofficial implementation without owner permission"):
            self.assertIn(term, rules)

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
