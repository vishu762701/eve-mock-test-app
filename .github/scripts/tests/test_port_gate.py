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

    def test_tampered_file_fails_check(self):
        data = json.loads(MANIFEST.read_text(encoding="utf-8"))
        data["screens"][0]["files"][0]["result_sha256"] = "0000000000000000000000000000000000000000000000000000000000000000"
        tmp_manifest = ROOT / "docs" / "test-port-manifest-tampered.json"
        try:
            tmp_manifest.write_text(json.dumps(data), encoding="utf-8")
            res = subprocess.run([sys.executable, str(PORT_GATE), "--check", "--manifest", str(tmp_manifest)], cwd=ROOT, capture_output=True, text=True)
            self.assertNotEqual(res.returncode, 0)
            self.assertIn("Result sha256 mismatch", res.stderr)
        finally:
            if tmp_manifest.is_file():
                tmp_manifest.unlink()

    def test_missing_recipe_fails_check(self):
        data = json.loads(MANIFEST.read_text(encoding="utf-8"))
        # Add a new non-debt CUT file without recipe
        data["screens"][0]["files"].append({
            "eve": "app/src/main/java/org/telegram/ui/Components/LayoutHelper.java",
            "upstream": "TMessagesProj/src/main/java/org/telegram/ui/Components/LayoutHelper.java",
            "mode": "CUT",
            "recipe_path": "",
            "recipe_sha256": "",
            "result_sha256": "eb94f013d5cb4376133190b837d89b1f9224c48a5f21e269396d189f0c534ec3",
            "eve_sha256": "eb94f013d5cb4376133190b837d89b1f9224c48a5f21e269396d189f0c534ec3",
            "upstream_sha256": "eb94f013d5cb4376133190b837d89b1f9224c48a5f21e269396d189f0c534ec3",
            "upstream_lines": 271,
            "kept_lines": 271,
            "originality_pct": 100.0,
            "removed_methods_reason": "",
            "known_debt": False
        })
        tmp_manifest = ROOT / "docs" / "test-port-manifest-norecipe.json"
        try:
            tmp_manifest.write_text(json.dumps(data), encoding="utf-8")
            res = subprocess.run([sys.executable, str(PORT_GATE), "--check", "--manifest", str(tmp_manifest)], cwd=ROOT, capture_output=True, text=True)
            self.assertNotEqual(res.returncode, 0)
            self.assertIn("mode CUT requires recipe", res.stderr)
        finally:
            if tmp_manifest.is_file():
                tmp_manifest.unlink()

    def test_recipe_replay_equals_file(self):
        import hashlib, tempfile
        tgcut = Path.home() / "tg-tools" / "tgcut.py"
        if not tgcut.is_file():
            self.skipTest("tgcut.py not installed")

        with tempfile.TemporaryDirectory() as tmp_src:
            up_java = Path(tmp_src) / "TMessagesProj" / "src" / "main" / "java" / "org" / "telegram" / "test"
            up_java.mkdir(parents=True, exist_ok=True)
            dummy_up = up_java / "Dummy.java"
            dummy_up.write_text("package org.telegram.test;\npublic class Dummy {\n  public void keep() {}\n  public void remove() {}\n}\n", encoding="utf-8")

            recipe_dir = ROOT / "docs" / "port-recipes"
            recipe_dir.mkdir(parents=True, exist_ok=True)
            test_recipe = recipe_dir / "Dummy.cut"
            test_recipe.write_text("upstream org/telegram/test/Dummy.java\nremove-method remove\n", encoding="utf-8")

            with tempfile.TemporaryDirectory() as tmp_repo:
                res = subprocess.run([sys.executable, str(tgcut), "build", str(test_recipe), "--repo", tmp_repo, "--src", tmp_src], capture_output=True, text=True)
                self.assertEqual(res.returncode, 0, f"tgcut build failed: {res.stderr}\n{res.stdout}")
                built_file = Path(tmp_repo) / "app" / "src" / "main" / "java" / "org" / "telegram" / "test" / "Dummy.java"
                self.assertTrue(built_file.is_file())
                self.assertNotIn("remove()", built_file.read_text(encoding="utf-8"))
                self.assertIn("keep()", built_file.read_text(encoding="utf-8"))

            if test_recipe.is_file():
                test_recipe.unlink()


if __name__ == "__main__":
    unittest.main()
