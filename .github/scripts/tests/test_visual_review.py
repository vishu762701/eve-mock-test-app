"""Standard-library test of native visual evidence generation."""
import json
from pathlib import Path
import struct
import subprocess
import sys
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / "eve_visual_review.py"
PAIRS = (
    "source-main-tabs",
    "ios26-nav-bar", "ios26-tab-bar", "ios26-menu", "ios26-sheet", "ios26-dialog","shared-components", "shared-components-high-contrast", "home", "mock-test", "recovery-home-360dp", "recovery-profile",
         "recovery-result-overview", "recovery-result-review",
         "recovery-result-leaderboard", "recovery-target-exams",
         "recovery-target-exams-footer",
    'student-home',
    'student-exam-tests',
    'student-login',
    'student-signup',
    'student-profile',
    'student-settings',
    'student-mock-test',
    'student-result',
    'student-review',
    'student-result-leaderboard',
    'student-statistics',
    'student-standing',
    'student-analytics',
    'student-leaderboard',
    'student-practice',
    'student-pyq',
    'student-syllabus',
    'student-history',
    'student-bookmarks',
    'student-mistakes',
    'student-performance',
    'student-notifications',
    'student-feedback',
    'student-premium',
    'student-premium-active',
    'student-payment',
    'student-payment-expired',
    'student-about',
    'student-content',
    'student-content-contact',
    'student-target-exams',
    'student-completed-exam',
    'student-category-picker',
    'student-payment-method',
    'student-report-question',
    'student-empty',
    'student-error',
    'student-login-large',
    'student-signup-large',
    'student-settings-large',
    'student-practice-large',
    'student-statistics-large',
    'student-bookmarks-large',
    'student-payment-large',
    'student-review-large',
    'student-profile-footer',
    'student-settings-footer',
    'student-result-footer',
    'student-analytics-footer',
    'student-premium-footer',
    'student-payment-footer',
    'student-report-question-footer',
    'student-home-drawer',
    'student-home-feedback',
    'student-home-loading',
    'student-feedback-error',
    'student-feedback-loading',
    'student-notifications-empty',
    'student-payment-success',
)


class VisualEvidenceTest(unittest.TestCase):
    def run_tool(self, root, enforce):
        command = [sys.executable, str(SCRIPT), "--captures", str(root / "captures"),
                   "--output", str(root / "captures"), "--source", str(root / "res")]
        if enforce:
            command.append("--enforce")
        return subprocess.run(command, capture_output=True, text=True)

    def test_required_pairs_and_source_candidates(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            images = root / "captures" / "eve-approved-ui"
            images.mkdir(parents=True)
            for pair in PAIRS:
                for theme in ("light", "dark"):
                    (images / f"{pair}-{theme}.png").write_bytes(
                        b"\x89PNG\r\n\x1a\n" + struct.pack(">I", 13) +
                        b"IHDR" + struct.pack(">II", 360, 800) + b"\x08\x06\x00\x00\x00")
            xml = root / "res" / "layout" / "activity_sample.xml"
            xml.parent.mkdir(parents=True)
            xml.write_text('<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android" '
                           'android:layout_width="240dp" android:layout_height="wrap_content"/>')
            result = self.run_tool(root, True)
            self.assertEqual(0, result.returncode, result.stderr)
            report = json.loads((root / "captures" / "visual-review.json").read_text())
            self.assertEqual(2 * len(PAIRS), len(report["screenshots"]))
            self.assertFalse(report["missing_required_pairs"])
            self.assertEqual(1, len(report["xml_review_candidates"]))
            self.assertIn("eve-approved-ui/home-light.png",
                          (root / "captures" / "visual-review.html").read_text())

    def test_corrupt_and_missing_capture_fails_without_hiding_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            folder = root / "captures"
            folder.mkdir()
            (folder / "broken-dark.png").write_bytes(b"bad")
            self.assertEqual(1, self.run_tool(root, True).returncode)
            self.assertEqual(0, self.run_tool(root, False).returncode)
            self.assertTrue((folder / "visual-review.json").exists())


if __name__ == "__main__":
    unittest.main()
