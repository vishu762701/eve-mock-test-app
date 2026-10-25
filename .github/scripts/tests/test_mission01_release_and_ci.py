"""Host-side Mission 01 CI anti-regression checks: executed before Android builds."""
from pathlib import Path
import hashlib
import re
import unittest

ROOT = Path(__file__).resolve().parents[3]
BUILD = ROOT / ".github/workflows/build.yml"
GRADLE = ROOT / "app/build.gradle.kts"
COLORS = ROOT / "app/src/main/res"
SRC_RESULT = ROOT / "app/src/main/java/com/eve/app/ui/result/ResultActivity.kt"
TEST_RESULT = ROOT / "app/src/test/java/com/eve/app/designsystem/UiBehaviorRegressionTest.kt"
NATIVE_TEST = ROOT / "app/src/androidTest/java/com/eve/app/ui/LockedFoundationTest.kt"


class Mission01GateTest(unittest.TestCase):
    def test_official_telegram_fidelity_contract_survives_rule_cleanup(self):
        """A source-exact mission needs per-role evidence beyond a green build."""
        entry = (ROOT / "AGENTS.md").read_text()
        for requirement in (
            "Telegram Mission 01 fidelity contract",
            "source-mapping table",
            "EVE path:line",
            "Telegram path:line",
            "verified source value or asset SHA-256",
            "Day/Night",
            "normal/pressed/disabled/selected/loading",
            "RTL",
            "accessibility",
            "NOT FOUND",
            "Green CI",
        ):
            self.assertIn(requirement, entry)
        self.assertIn("never block unrelated tasks", entry)

    def test_imported_icon_hashes_match_source_evidence(self):
        """Pin every imported density to the recorded upstream/EVE hash comparison.

        This checks committed evidence consistency, not access to the original ZIP
        or permission to redistribute Telegram artwork.
        """
        import csv

        def read_tsv(name):
            with (ROOT / "docs" / name).open(newline="") as stream:
                return list(csv.DictReader(
                    (line for line in stream if not line.startswith("#")),
                    delimiter="\t",
                ))

        registry = read_tsv("telegram-icon-source-registry.tsv")
        evidence = read_tsv("telegram-source-verification.tsv")
        matches = [row for row in evidence if row["result"] == "MATCH"]
        by_eve = {row["eve_asset"]: row for row in matches}
        self.assertEqual(len(matches), len(by_eve), "Duplicate source verification paths")

        expected_paths = set()
        imported = [
            row for row in registry
            if row["source_fidelity_status"] == "OFFICIAL_TELEGRAM_GPL_ART_IMPORTED"
        ]
        self.assertTrue(imported, "No imported Telegram icons registered")
        for row in imported:
            icon = row["eve_icon"]
            source_hdpi = row["telegram_zip_asset"]
            self.assertIn("drawable-hdpi/", source_hdpi)
            for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi"):
                path = f"app/src/main/res/drawable-{density}/{icon}.webp"
                expected_sha = row[f"sha256_{density}"]
                if expected_sha == "NOT FOUND":
                    self.assertFalse(
                        (ROOT / path).exists(),
                        f"Unverified artwork bundled for absent Telegram density: {path}",
                    )
                    self.assertNotIn(path, by_eve, f"Unexpected source match for {path}")
                    continue
                self.assertRegex(expected_sha, r"^[0-9a-f]{64}$")
                expected_paths.add(path)
                bundled = ROOT / path
                self.assertTrue(bundled.is_file(), f"Missing imported artwork: {path}")
                self.assertEqual(
                    expected_sha,
                    hashlib.sha256(bundled.read_bytes()).hexdigest(),
                    f"Bundled artwork drift: {path}",
                )
                self.assertIn(path, by_eve, f"Missing source evidence for {path}")
                verified = by_eve[path]
                self.assertEqual(
                    source_hdpi.replace("drawable-hdpi/", f"drawable-{density}/"),
                    verified["telegram_asset"],
                    f"Wrong upstream path for {path}",
                )
                self.assertEqual(expected_sha, verified["upstream_sha256"], path)
                self.assertEqual(expected_sha, verified["eve_sha256"], path)
        self.assertEqual(expected_paths, set(by_eve), "Unregistered source MATCH rows")

    def test_compile_and_unit_assertions_gate_both_emulator_jobs(self):
        """Verify functional CI gates, not obsolete Gradle command spellings."""
        workflow = BUILD.read_text()
        build = workflow.split("\n  build:\n", 1)[1].split("\n  native-ui:\n", 1)[0]
        native = workflow.split("\n  native-ui:\n", 1)[1].split("\n  backend:\n", 1)[0]
        publish = workflow.split("\n  publish-apk:\n", 1)[1]
        # Assembling plus unit tests transitively compile Kotlin/process resources.
        # The FULL gate compiles AndroidTest sources before emulator startup.
        # Check task-level semantics rather than a brittle exact command spelling.
        fast_step = build.split("      - name: FAST compile", 1)[1].split("      - name: FULL test", 1)[0]
        full_step = build.split("      - name: FULL test", 1)[1].split("      - name: Verify missing release", 1)[0]
        for task in (":app:assembleDebug", ":app:compileDebugUnitTestKotlin",
                     ":app:testDebugUnitTest", ":app:compileDebugAndroidTestKotlin"):
            self.assertIn(task, fast_step)
        for task in (":app:compileDebugAndroidTestKotlin", ":app:lintDebug",
                     ":app:testDebugUnitTest"):
            self.assertIn(task, full_step)
        self.assertLess(build.index("      - name: FAST compile"), build.index("      - name: FULL test"))
        self.assertLess(build.index("      - name: FULL test"), build.index("assembleRelease --dry-run"))
        self.assertIn("name: FULL test lint and compile AndroidTest sources", build)
        self.assertIn("github.event.head_commit.message", build)
        self.assertNotIn("GITHUB_RUN_NUMBER % 20", build)
        self.assertIn("id: verification_profile", build)
        self.assertIn("if: " + "$" + "{{ steps.verification_profile.outputs.full == 'true' }}", build)
        self.assertIn("gradle :app:assembleRelease --dry-run", build)
        self.assertIn("Refusing release build: a real RELEASE_STORE_FILE", build)
        # FAST push must not run native emulators or publish an unverified APK.
        # FULL is supported via manual dispatch or final main commit marker.
        self.assertIn("[eve-full]", workflow)
        self.assertIn("needs: [build, backend, firebase-functions]", native)
        self.assertIn("needs.build.outputs.full == 'true' && success()", native)
        self.assertIn("needs.build.outputs.full == 'true'", native)
        self.assertIn("matrix:\n        navigation: [gesture, three-button]", native)
        self.assertIn("reactivecircus/android-emulator-runner@v2", native)
        self.assertIn("--enforce", native)
        self.assertIn("needs: [build, native-ui, backend, firebase-functions]", publish)
        self.assertIn("needs.build.outputs.full == 'true'", publish)
        self.assertIn("&& success()", publish)
        self.assertIn("actions/cache/restore@v4", publish)
        self.assertIn("sha256sum --check --status", publish)
        self.assertNotIn("gradle assembleDebug", publish)
        self.assertIn("name: Eve-debug-apk", publish)
        self.assertIn("actions/cache/save@v4", build)
        self.assertIn(":app:assembleDebugAndroidTest", full_step)
        self.assertIn("test_apk_sha256:", build)
        self.assertIn("Restore prebuilt app and instrumentation APKs", native)
        self.assertIn("Verify original APK checksums", native)
        self.assertNotIn("setup-gradle", native)
        # A rerun of ONLY failed jobs must restore the exact built candidate.
        cache_key = "key: eve-apk-${{ github.run_id }}-${{ github.sha }}"
        self.assertEqual(1, build.count(cache_key))
        self.assertEqual(1, publish.count(cache_key))
        self.assertNotIn("github.run_attempt", workflow)
        self.assertIn("sha256sum app/build/outputs/apk/debug/app-debug.apk", build)
        self.assertNotIn("continue-on-error: true", workflow)
        self.assertNotIn("cancel-in-progress: true", workflow)

    def test_release_requires_explicit_gpl_distribution_clearance(self):
        workflow = (ROOT / ".github/workflows/release.yml").read_text()
        self.assertIn("Require reviewed GPL distribution clearance", workflow)
        self.assertIn("vars.EVE_GPL_RELEASE_APPROVED", workflow)
        self.assertIn('"$EVE_GPL_RELEASE_APPROVED" != "true"', workflow)
        self.assertLess(
            workflow.index("Require reviewed GPL distribution clearance"),
            workflow.index("Validate release signing secrets and decode keystore")
        )
        self.assertIn("Production release blocked", workflow)
        self.assertIn("docs/telegram-release-compliance-gate.md", workflow)

    def test_release_never_falls_back_to_debug_key(self):
        gradle = GRADLE.read_text()
        release = gradle.split('create("release") {', 1)[1].split("\n        }\n    }\n\n    buildTypes", 1)[0]
        self.assertIn("isReleaseTaskRequested", release)
        self.assertIn("!validKeystore || !validCredentials", release)
        self.assertIn("throw org.gradle.api.GradleException", release)
        self.assertIn("storeFile = file(storeFilePath!!)", release)
        self.assertNotIn('file("eve-debug.keystore")', release)
        self.assertIn('getByName("debug")', gradle)
        self.assertIn('signingConfig = signingConfigs.getByName("release")', gradle)

    def test_historical_unresolved_result_reference_is_declared_and_source_synced(self):
        source = SRC_RESULT.read_text()
        assert "val attemptDate = intent.getStringExtra(Constants.EXTRA_ATTEMPT_DATE)" in source
        self.assertIn("Attempted on $attemptDate", source)
        test = TEST_RESULT.read_text()
        self.assertIn("Attempted on ", test)
        self.assertNotIn('source.contains("attemptDate")', test)

    def test_theme_assertions_follow_source_derived_token_in_both_modes(self):
        native = NATIVE_TEST.read_text()
        self.assertIn("listOf(false, true)", native)
        self.assertIn("context.getColor(R.color.telegram_type_primary)", native)
        self.assertNotRegex(native, r"assertEquals\(\s*(?:Color\.BLACK|android\.graphics\.Color\.BLACK)")
        import xml.etree.ElementTree as ET
        resolved = []
        for folder in ("values", "values-night"):
            root = ET.parse(COLORS / folder / "telegram_colors.xml").getroot()
            matches = [c.text.strip() for c in root.findall("color")
                       if c.get("name") == "telegram_type_primary"]
            self.assertEqual(1, len(matches))
            self.assertRegex(matches[0], r"^#[0-9A-Fa-f]{8}$")
            resolved.append(matches[0])
        self.assertNotEqual(*resolved)


if __name__ == "__main__":
    unittest.main()
