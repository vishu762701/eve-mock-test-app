"""Tests for tools/port_gate.py enforcement contracts."""
import json
from pathlib import Path
import subprocess
import sys
import unittest

ROOT = Path(__file__).resolve().parents[3]
PORT_GATE = ROOT / "tools" / "port_gate.py"
MANIFEST = ROOT / "docs" / "port-manifest.json"


class PortGateContractTest(unittest.TestCase):
    def test_port_gate_check_passes_on_current_repo(self):
        res = subprocess.run([sys.executable, str(PORT_GATE), "--check"], cwd=ROOT, capture_output=True, text=True)
        self.assertEqual(res.returncode, 0, f"port_gate --check failed: {res.stderr}\n{res.stdout}")
        self.assertIn("PORT GATE CHECK PASSED", res.stdout)
        self.assertIn("WARN-KNOWN", res.stdout)

    def test_port_gate_rejects_missing_manifest(self):
        res = subprocess.run([sys.executable, str(PORT_GATE), "--check", "--manifest", "nonexistent.json"], cwd=ROOT, capture_output=True, text=True)
        self.assertNotEqual(res.returncode, 0)
        self.assertIn("not found", res.stderr)

    def test_port_gate_detects_unregistered_known_debt(self):
        data = json.loads(MANIFEST.read_text(encoding="utf-8"))
        data["known_debt_allowlist"] = []  # empty allowlist
        tmp_manifest = ROOT / "docs" / "test-port-manifest-bad.json"
        try:
            tmp_manifest.write_text(json.dumps(data), encoding="utf-8")
            res = subprocess.run([sys.executable, str(PORT_GATE), "--check", "--manifest", str(tmp_manifest)], cwd=ROOT, capture_output=True, text=True)
            self.assertNotEqual(res.returncode, 0)
            self.assertIn("not in allowlist", res.stderr)
        finally:
            if tmp_manifest.is_file():
                tmp_manifest.unlink()

    def test_should_run_smoke_detects_triggers(self):
        # When checking with --diff-ref HEAD, no unstaged changes in working copy trigger unless staged/working
        res = subprocess.run([sys.executable, str(PORT_GATE), "--should-run-smoke", "--diff-ref", "HEAD"], cwd=ROOT, capture_output=True, text=True)
        # Should exit 1 with no relevant changes
        self.assertEqual(res.returncode, 1)
        self.assertIn("PORT SMOKE CHECK", res.stdout)


if __name__ == "__main__":
    unittest.main()
