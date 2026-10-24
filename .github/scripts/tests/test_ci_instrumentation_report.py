"""Direct instrumentation must not turn failing tests into a green build."""
import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from ci_instrumentation_report import analyze


def output(statuses, total=None, terminal=-1):
    lines = []
    for i, code in enumerate(statuses):
        lines.extend([
            "INSTRUMENTATION_STATUS: class=com.eve.app.ui.Fixture",
            f"INSTRUMENTATION_STATUS: test=test{i}",
            f"INSTRUMENTATION_STATUS: numtests={len(statuses) if total is None else total}",
            "INSTRUMENTATION_STATUS_CODE: 1",
            f"INSTRUMENTATION_STATUS_CODE: {code}",
        ])
    if terminal is not None:
        lines.append(f"INSTRUMENTATION_CODE: {terminal}")
    return lines


class ParseInstrumentationTest(unittest.TestCase):
    def test_success(self):
        ok, done, total, details = analyze(output([0, 0]))
        self.assertTrue(ok, details)
        self.assertEqual((done, total), (2, 2))

    def test_errors_skips_and_truncated_runs_fail_closed(self):
        for code in (-1, -2, -3, -4):
            self.assertFalse(analyze(output([code]))[0])
        self.assertFalse(analyze([])[0])
        self.assertFalse(analyze(output([0], total=2))[0])
        self.assertFalse(analyze(output([0], terminal=None))[0])
        self.assertFalse(analyze(output([0], terminal=0))[0])


if __name__ == "__main__":
    unittest.main()
