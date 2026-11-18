"""Regression test for Law 7 Anti-Shortcut and Anti-Wrapper Contract."""
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / "tools"))
import check_no_shortcuts


class NoShortcutsContractTest(unittest.TestCase):
    def test_check_no_shortcuts_script_executable_and_passes(self):
        script_path = ROOT / "tools" / "check_no_shortcuts.py"
        self.assertTrue(script_path.is_file(), "tools/check_no_shortcuts.py must exist")
        self.assertTrue(os.access(script_path, os.X_OK), "tools/check_no_shortcuts.py must be executable")

        result = subprocess.run(
            [sys.executable, str(script_path), "--root", str(ROOT)],
            capture_output=True,
            text=True
        )
        self.assertEqual(result.returncode, 0, f"check_no_shortcuts.py failed: {result.stderr or result.stdout}")
        self.assertIn("ANTI-SHORTCUT CHECK PASSED", result.stdout)

    def test_check_no_shortcuts_passes_on_live_repo(self):
        layout_violations = check_no_shortcuts.check_layouts(ROOT, legacy_widgets=True)
        self.assertEqual(layout_violations, [], f"Layout shortcut violations: {layout_violations}")

        source_violations = check_no_shortcuts.check_source_shortcuts(ROOT, strict=True)
        self.assertEqual(source_violations, [], f"Source shortcut violations: {source_violations}")

        forwarding_violations = check_no_shortcuts.check_telegram_forwarding_wrappers(ROOT)
        self.assertEqual(forwarding_violations, [], f"Forwarding wrapper violations: {forwarding_violations}")

        provenance_violations = check_no_shortcuts.check_org_telegram_provenance(ROOT)
        self.assertEqual(provenance_violations, [], f"Provenance violations: {provenance_violations}")

        duplicate_violations = check_no_shortcuts.check_duplicate_simple_names(ROOT)
        self.assertEqual(duplicate_violations, [], f"Duplicate simple name violations: {duplicate_violations}")

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

    def test_legacy_widgets_detected_in_xml_layout(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_root = Path(tmp_dir)
            layout_dir = tmp_root / "app" / "src" / "main" / "res" / "layout"
            layout_dir.mkdir(parents=True, exist_ok=True)
            (layout_dir / "test_legacy.xml").write_text(
                '<com.eve.app.ui.common.EveGlassToolbar android:id="@+id/toolbar" />\n',
                encoding="utf-8"
            )
            violations = check_no_shortcuts.check_layouts(tmp_root, legacy_widgets=True)
            self.assertTrue(any("EveGlassToolbar" in v for v in violations),
                            f"check_layouts should detect EveGlassToolbar, got: {violations}")

    def test_forwarding_wrapper_detected(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_root = Path(tmp_dir)
            fake_pkg = tmp_root / "app" / "src" / "main" / "java" / "com" / "eve" / "app" / "ui"
            fake_pkg.mkdir(parents=True, exist_ok=True)
            fake_kt = fake_pkg / "TelegramActionBar.kt"
            fake_kt.write_text(
                "package com.eve.app.ui\n\n"
                "open class TelegramActionBar : com.eve.app.designsystem.TelegramActionBar()\n",
                encoding="utf-8"
            )
            violations = check_no_shortcuts.check_telegram_forwarding_wrappers(tmp_root)
            self.assertTrue(any("Telegram forwarding wrapper in" in v for v in violations),
                            f"check_telegram_forwarding_wrappers should detect forwarding wrapper, got: {violations}")

    def test_forwarding_wrapper_java_detected(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_root = Path(tmp_dir)
            fake_pkg = tmp_root / "app" / "src" / "main" / "java" / "com" / "eve" / "app" / "ui"
            fake_pkg.mkdir(parents=True, exist_ok=True)
            fake_java = fake_pkg / "TelegramActionBar.java"
            fake_java.write_text(
                "package com.eve.app.ui;\n\n"
                "public class TelegramActionBar extends com.eve.app.designsystem.TelegramActionBar {\n"
                "    public TelegramActionBar(android.content.Context context) {\n"
                "        super(context);\n"
                "    }\n"
                "}\n",
                encoding="utf-8"
            )
            violations = check_no_shortcuts.check_telegram_forwarding_wrappers(tmp_root)
            self.assertTrue(any("Telegram forwarding wrapper in" in v for v in violations),
                            f"check_telegram_forwarding_wrappers should detect Java forwarding wrapper, got: {violations}")

    def test_telegram_class_with_own_members_not_flagged(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_root = Path(tmp_dir)
            pkg = tmp_root / "app" / "src" / "main" / "java" / "com" / "eve" / "app" / "ui"
            pkg.mkdir(parents=True, exist_ok=True)
            # Kotlin class with real method
            (pkg / "TelegramRealKt.kt").write_text(
                "package com.eve.app.ui\n\n"
                "open class TelegramRealKt : BaseView() {\n"
                "    fun customAction() = 42\n"
                "}\n",
                encoding="utf-8"
            )
            # Java class with real method
            (pkg / "TelegramRealJava.java").write_text(
                "package com.eve.app.ui;\n\n"
                "public class TelegramRealJava extends BaseView {\n"
                "    public TelegramRealJava() { super(); }\n"
                "    public void doWork() {}\n"
                "}\n",
                encoding="utf-8"
            )
            violations = check_no_shortcuts.check_telegram_forwarding_wrappers(tmp_root)
            self.assertEqual(violations, [], f"Classes with own members should not be flagged: {violations}")

    def test_unregistered_org_telegram_file_detected(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_root = Path(tmp_dir)
            tg_pkg = tmp_root / "app" / "src" / "main" / "java" / "org" / "telegram" / "ui"
            tg_pkg.mkdir(parents=True, exist_ok=True)
            docs_dir = tmp_root / "docs"
            docs_dir.mkdir(parents=True, exist_ok=True)
            (docs_dir / "telegram-verbatim-imports.json").write_text('{"imports": []}\n', encoding="utf-8")
            (tg_pkg / "UnregisteredClass.java").write_text(
                "package org.telegram.ui;\npublic class UnregisteredClass {}\n",
                encoding="utf-8"
            )
            violations = check_no_shortcuts.check_org_telegram_provenance(tmp_root)
            self.assertTrue(any("Unregistered org/telegram file" in v for v in violations),
                            f"check_org_telegram_provenance should detect unregistered file, got: {violations}")

    def test_duplicate_simple_names_detected(self):
        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_root = Path(tmp_dir)
            pkg1 = tmp_root / "app" / "src" / "main" / "java" / "pkg1"
            pkg2 = tmp_root / "app" / "src" / "main" / "java" / "pkg2"
            pkg1.mkdir(parents=True, exist_ok=True)
            pkg2.mkdir(parents=True, exist_ok=True)
            (pkg1 / "BackDrawable.kt").write_text("package pkg1\nclass BackDrawable\n", encoding="utf-8")
            (pkg2 / "BackDrawable.java").write_text("package pkg2;\npublic class BackDrawable {}\n", encoding="utf-8")
            violations = check_no_shortcuts.check_duplicate_simple_names(tmp_root)
            self.assertTrue(any("Duplicate class name 'BackDrawable'" in v for v in violations),
                            f"check_duplicate_simple_names should detect duplicates, got: {violations}")


if __name__ == "__main__":
    unittest.main()
