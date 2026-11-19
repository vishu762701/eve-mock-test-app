"""Regression test for tools/check_no_stubs.py anti-stub contract."""
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools"))
import check_no_stubs


class CheckNoStubsContractTest(unittest.TestCase):

    def test_genuine_host_adapter_accepted(self):
        """A genuine TelegramHostActivity adapter with lifecycle and fragment delegates is accepted."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            file_path = Path(tmp_dir) / "PasscodeActivity.kt"
            file_path.write_text(
                "/*\n"
                " * Derived from Telegram Android PasscodeActivity.java\n"
                " */\n"
                "package com.eve.app.ui.settings\n\n"
                "import android.content.Intent\n"
                "import android.os.Bundle\n"
                "import com.eve.app.ui.common.TelegramHostActivity\n"
                "import org.telegram.ui.ActionBar.BaseFragment\n"
                "import org.telegram.ui.ActionIntroActivity\n\n"
                "class PasscodeActivity : TelegramHostActivity<BaseFragment>() {\n"
                "    override fun onCreate(savedInstanceState: Bundle?) {\n"
                "        super.onCreate(savedInstanceState)\n"
                "    }\n"
                "    override fun createFragment(): BaseFragment {\n"
                "        val mode = intent.getIntExtra(\"mode\", 0)\n"
                "        return org.telegram.ui.PasscodeActivity(mode).apply {\n"
                "            setEveNavigator { next -> openMode(next) }\n"
                "        }\n"
                "    }\n"
                "    private fun openMode(next: Int) {\n"
                "        val intent = Intent(this, PasscodeActivity::class.java)\n"
                "        startActivity(intent)\n"
                "        finish()\n"
                "    }\n"
                "}\n",
                encoding="utf-8"
            )
            errors = check_no_stubs.check_file(file_path)
            self.assertEqual(errors, [], f"Genuine host adapter should be accepted, got: {errors}")

    def test_empty_subclass_rejected(self):
        """An empty subclass claiming to be derived from Telegram must be rejected."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            file_path = Path(tmp_dir) / "FakeActivity.kt"
            file_path.write_text(
                "// Derived from Telegram Android\n"
                "package com.eve.app.ui\n"
                "class FakeActivity : TelegramHostActivity<BaseFragment>() {}\n",
                encoding="utf-8"
            )
            errors = check_no_stubs.check_file(file_path)
            self.assertTrue(any("Empty subclass" in e for e in errors),
                            f"Empty subclass should be rejected, got: {errors}")

    def test_incomplete_adapter_missing_create_fragment_rejected(self):
        """A TelegramHostActivity subclass that does not override createFragment must be rejected."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            file_path = Path(tmp_dir) / "IncompleteActivity.kt"
            file_path.write_text(
                "/*\n"
                " * Derived from Telegram Android\n"
                " */\n"
                "package com.eve.app.ui\n"
                "import com.eve.app.ui.common.TelegramHostActivity\n"
                "class IncompleteActivity : TelegramHostActivity<BaseFragment>() {\n"
                "    val line1 = 1\n"
                "    val line2 = 2\n"
                "    val line3 = 3\n"
                "    val line4 = 4\n"
                "    val line5 = 5\n"
                "    val line6 = 6\n"
                "    val line7 = 7\n"
                "    val line8 = 8\n"
                "    val line9 = 9\n"
                "    val line10 = 10\n"
                "    val line11 = 11\n"
                "    val line12 = 12\n"
                "    val line13 = 13\n"
                "    val line14 = 14\n"
                "}\n",
                encoding="utf-8"
            )
            errors = check_no_stubs.check_file(file_path)
            self.assertTrue(any("Missing override of createFragment()" in e for e in errors),
                            f"Missing createFragment should be rejected, got: {errors}")

    def test_fake_adapter_without_telegram_reference_rejected(self):
        """A host adapter that does not instantiate or reference any Telegram fragment must be rejected."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            file_path = Path(tmp_dir) / "NoTgRefActivity.kt"
            file_path.write_text(
                "/*\n"
                " * Derived from Telegram Android\n"
                " */\n"
                "package com.eve.app.ui\n"
                "import com.eve.app.ui.common.TelegramHostActivity\n"
                "class NoTgRefActivity : TelegramHostActivity<MyFragment>() {\n"
                "    override fun createFragment(): MyFragment {\n"
                "        val dummy1 = 1\n"
                "        val dummy2 = 2\n"
                "        val dummy3 = 3\n"
                "        val dummy4 = 4\n"
                "        val dummy5 = 5\n"
                "        val dummy6 = 6\n"
                "        val dummy7 = 7\n"
                "        val dummy8 = 8\n"
                "        val dummy9 = 9\n"
                "        val dummy10 = 10\n"
                "        return MyFragment()\n"
                "    }\n"
                "}\n",
                encoding="utf-8"
            )
            errors = check_no_stubs.check_file(file_path)
            self.assertTrue(any("Does not instantiate or reference any Telegram UI fragment" in e for e in errors),
                            f"No Telegram reference should be rejected, got: {errors}")

    def test_trivial_adapter_under_15_lines_rejected(self):
        """A trivial host adapter with fewer than 15 lines of implementation is rejected."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            file_path = Path(tmp_dir) / "ShortActivity.kt"
            file_path.write_text(
                "/*\n"
                " * Derived from Telegram Android\n"
                " */\n"
                "package com.eve.app.ui\n"
                "import com.eve.app.ui.common.TelegramHostActivity\n"
                "class ShortActivity : TelegramHostActivity<BaseFragment>() {\n"
                "    override fun createFragment(): BaseFragment = org.telegram.ui.PasscodeActivity(0)\n"
                "}\n",
                encoding="utf-8"
            )
            errors = check_no_stubs.check_file(file_path)
            self.assertTrue(any("Only" in e and "minimum 15 required" in e for e in errors),
                            f"Under 15 lines should be rejected, got: {errors}")

    def test_telegram_port_under_40_lines_rejected(self):
        """A non-adapter Telegram port under 40 lines must be rejected."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            file_path = Path(tmp_dir) / "ShortComponent.kt"
            file_path.write_text(
                "// Derived from Telegram Android\n"
                "package org.telegram.ui.Components\n"
                "class ShortComponent {\n"
                "    val a = 1\n"
                "    val b = 2\n"
                "}\n",
                encoding="utf-8"
            )
            errors = check_no_stubs.check_file(file_path)
            self.assertTrue(any("minimum 40 required for Telegram ports" in e for e in errors),
                            f"Under 40 lines port should be rejected, got: {errors}")

    def test_false_feature_claim_rejected(self):
        """A file claiming a feature in comments that no code implements must be rejected."""
        with tempfile.TemporaryDirectory() as tmp_dir:
            file_path = Path(tmp_dir) / "FakeFeature.kt"
            file_path.write_text(
                "/*\n"
                " * Derived from Telegram Android\n"
                " * Implements gooey effect for profile header\n"
                " */\n"
                "package com.eve.app.ui\n"
                "class FakeFeature {\n"
                + "\n".join(f"    val line{i} = {i}" for i in range(45))
                + "\n}\n",
                encoding="utf-8"
            )
            errors = check_no_stubs.check_file(file_path)
            self.assertTrue(any("Comment claims feature 'gooey'" in e for e in errors),
                            f"False feature claim should be rejected, got: {errors}")

    def test_live_repo_passes_check_no_stubs(self):
        """Live repository app/src/main/java must pass check_no_stubs with zero errors."""
        errors = []
        for p in (ROOT / "app" / "src" / "main" / "java").rglob("*.kt"):
            errors.extend(check_no_stubs.check_file(p))
        for p in (ROOT / "app" / "src" / "main" / "java").rglob("*.java"):
            errors.extend(check_no_stubs.check_file(p))
        self.assertEqual(errors, [], f"Live repo has stub errors: {errors}")


if __name__ == "__main__":
    unittest.main()
