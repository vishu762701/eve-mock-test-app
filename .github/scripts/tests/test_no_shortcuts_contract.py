"""Regression test for Law 7 Anti-Shortcut and Anti-Wrapper Contract."""
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools"))
import check_no_shortcuts


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


class NoShortcutsContractTest(unittest.TestCase):
    def test_governance_files_contain_laws_5_6_7_and_anti_shortcut(self):
        governance_files = [
            "ENGINEERING_STANDARD.md",
            "AGENTS.md",
            "SOURCE_OF_TRUTH.md",
            "GEMINI.md",
            "CLAUDE.md",
        ]
        for rel in governance_files:
            content = read(rel)
            self.assertTrue(
                bool(re.search(r"\b(?:LAW|Law)\s*5\b", content)),
                f"{rel} missing Law 5 / LAW 5"
            )
            self.assertTrue(
                bool(re.search(r"\b(?:LAW|Law)\s*6\b", content)),
                f"{rel} missing Law 6 / LAW 6"
            )
            self.assertTrue(
                bool(re.search(r"\b(?:LAW|Law)\s*7\b", content)),
                f"{rel} missing Law 7 / LAW 7"
            )
            self.assertIn("ZERO-SHORTCUT", content, f"{rel} missing 'ZERO-SHORTCUT'")
            self.assertIn("check_no_shortcuts.py", content, f"{rel} missing 'check_no_shortcuts.py'")

    def test_check_no_shortcuts_script_executable_and_passes_governance(self):
        script_path = ROOT / "tools" / "check_no_shortcuts.py"
        self.assertTrue(script_path.is_file(), "tools/check_no_shortcuts.py must exist")
        self.assertTrue(os.access(script_path, os.X_OK), "tools/check_no_shortcuts.py must be executable")

        result = subprocess.run(
            [sys.executable, str(script_path), "--check-governance", "--root", str(ROOT)],
            capture_output=True,
            text=True
        )
        self.assertEqual(result.returncode, 0, f"check_no_shortcuts.py --check-governance failed: {result.stderr}")
        self.assertIn("ANTI-SHORTCUT CHECK PASSED", result.stdout)

    def test_check_no_shortcuts_passes_on_live_repo(self):
        violations = check_no_shortcuts.check_governance(ROOT)
        self.assertEqual(violations, [], f"Governance violations: {violations}")

        layout_violations = check_no_shortcuts.check_layouts(ROOT)
        self.assertEqual(layout_violations, [], f"Layout shortcut violations: {layout_violations}")

        source_violations = check_no_shortcuts.check_source_shortcuts(ROOT, strict=False)
        self.assertEqual(source_violations, [], f"Source shortcut violations: {source_violations}")

    def test_strict_mode_detects_fake_wrapper_subclass(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_root = Path(tmp_dir)
            fake_pkg = tmp_root / "app" / "src" / "main" / "java" / "com" / "eve" / "app" / "ui"
            fake_pkg.mkdir(parents=True, exist_ok=True)
            fake_kt = fake_pkg / "TelegramFakeWrapper.kt"
            fake_kt.write_text(
                "package com.eve.app.ui\n\n"
                "open class TelegramFakeWrapper : UiArcLegacyComponent() {\n"
                "    // shortcut wrapper\n"
                "}\n",
                encoding="utf-8"
            )

            violations = check_no_shortcuts.check_source_shortcuts(tmp_root, strict=True)
            self.assertTrue(any("Telegram class must not subclass legacy UiArc*" in v for v in violations),
                            f"Strict mode should have flagged fake subclass, got: {violations}")

    def test_typealias_shortcut_detected(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_root = Path(tmp_dir)
            fake_pkg = tmp_root / "app" / "src" / "main" / "java" / "com" / "eve" / "app" / "ui"
            fake_pkg.mkdir(parents=True, exist_ok=True)
            fake_kt = fake_pkg / "TelegramAliases.kt"
            fake_kt.write_text(
                "package com.eve.app.ui\n\n"
                "typealias TelegramView = UiArcView\n",
                encoding="utf-8"
            )

            violations = check_no_shortcuts.check_source_shortcuts(tmp_root, strict=False)
            self.assertTrue(any("Forbidden typealias shortcut" in v for v in violations),
                            f"Typealias shortcut should be detected, got: {violations}")


if __name__ == "__main__":
    unittest.main()
