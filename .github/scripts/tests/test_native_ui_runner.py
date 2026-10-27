"""Host-side contract tests: UI failures must remain failures and retain evidence."""
import os
import re
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'verify-native-ui.sh'
BASH = shutil.which('bash') or '/bin/bash'


class NativeUiRunnerTest(unittest.TestCase):
    def run_script(self, mode, status=0, observed=None, pending_reads=0, assertion_failure=False,
                   drain_instrument_stdin=False):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            log = root / 'commands.log'
            fixture_source = Path(__file__).resolve().parents[3] / 'app/src/androidTest/java/com/eve/app/ui/StudentUiRenderingTest.kt'
            fixture_copy = root / 'app/src/androidTest/java/com/eve/app/ui/StudentUiRenderingTest.kt'
            fixture_copy.parent.mkdir(parents=True)
            fixture_copy.write_text(fixture_source.read_text())
            for name, body in {
                'adb': f'''#!{BASH}
printf 'adb %s\\n' "$*" >> "$COMMAND_LOG"
case "$*" in
  'shell settings get secure navigation_mode')
    count=0
    [[ ! -f "$READ_COUNT" ]] || read -r count < "$READ_COUNT"
    echo "$((count + 1))" > "$READ_COUNT"
    if (( count < PENDING_READS )); then echo 2; else echo "$OBSERVED_MODE"; fi ;;
  'shell pm path com.android.deskclock') exit 1 ;;
  'shell pm path com.google.android.apps.messaging') echo 'package:/system/app/Messages.apk' ;;
  'shell am instrument '*)
    # Simulate adb shell consuming all available stdin.
    if [[ "$DRAIN_INSTRUMENT_STDIN" == "1" ]]; then cat >/dev/null; fi
    count=1
    [[ "$*" != *" -e notClass "* ]] || count=52
    for ((i=1; i<=count; i++)); do
      echo 'INSTRUMENTATION_STATUS: class=com.eve.app.ui.Fixture'
      echo "INSTRUMENTATION_STATUS: test=ok$i"
      echo "INSTRUMENTATION_STATUS: numtests=$count"
      echo 'INSTRUMENTATION_STATUS_CODE: 1'
      if [[ "$INSTRUMENT_FAIL" == "1" ]]; then
        echo 'INSTRUMENTATION_STATUS_CODE: -2'
      else
        echo 'INSTRUMENTATION_STATUS_CODE: 0'
      fi
    done
    echo 'INSTRUMENTATION_CODE: -1'
    exit "$GRADLE_STATUS" ;;
esac
''',
                'sleep': f'#!{BASH}\nexit 0\n',
            }.items():
                file = root / name
                file.write_text(body)
                file.chmod(0o755)
            env = dict(os.environ, PATH=f'{root}:{os.environ["PATH"]}', COMMAND_LOG=str(log),
                       OBSERVED_MODE=str(observed if observed is not None else (2 if mode == 'gesture' else 0)),
                       GRADLE_STATUS=str(status), INSTRUMENT_FAIL='1' if assertion_failure else '0',
                       PENDING_READS=str(pending_reads),
                       DRAIN_INSTRUMENT_STDIN='1' if drain_instrument_stdin else '0',
                       READ_COUNT=str(root / 'read-count'))
            result = subprocess.run(['bash', str(SCRIPT), mode], cwd=root, env=env,
                                    capture_output=True, text=True)
            commands = log.read_text()
            evidence = sorted(p.name for p in (root / 'ui-renderings').iterdir())
            return result, commands, evidence

    def test_each_mode_runs_full_suite_and_all_rendering_shards(self):
        for mode, overlay in [('gesture', 'gestural'), ('three-button', 'threebutton')]:
            with self.subTest(mode=mode):
                result, commands, evidence = self.run_script(mode)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertEqual(5, commands.count('adb shell am instrument -w -r'))
                self.assertIn('-e notClass com.eve.app.ui.StudentUiRenderingTest#allStudentScreensRenderInBothThemesAndPhoneSizes', commands)
                visual_lines = [line for line in commands.splitlines() if ' -e visualCases ' in line]
                self.assertEqual(4, len(visual_lines))
                source = (Path(__file__).resolve().parents[3] / 'app/src/androidTest/java/com/eve/app/ui/StudentUiRenderingTest.kt').read_text()
                block = re.search(r"private val cases = linkedMapOf\((.*?)\n    \)", source, re.S)
                self.assertIsNotNone(block)
                expected = re.findall(r'"([^"]+)"\s+to\s+R\.layout\.', block.group(1))
                actual = [case for line in visual_lines for case in line.split(' -e visualCases ', 1)[1].split(' ', 1)[0].split(',')]
                self.assertCountEqual(expected, actual)
                self.assertIn('adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk', commands)
                self.assertIn(f'-e navigationMode {mode}', commands)
                self.assertIn('-e package com.eve.app.ui', commands)
                self.assertIn(f'navbar.{overlay}', commands)
                self.assertLess(commands.index('am start -W -a android.settings.SETTINGS'),
                                commands.index('shell cmd overlay enable-exclusive'))
                self.assertLess(commands.index('shell cmd overlay enable-exclusive'),
                                commands.index('adb shell am instrument -w -r'))
                self.assertNotIn('force-stop', commands)
                self.assertNotIn('adb reboot', commands)
                self.assertIn('adb shell pm disable-user --user 0 com.google.android.apps.messaging', commands)
                self.assertIn('adb shell pm enable --user 0 com.google.android.apps.messaging', commands)
                self.assertIn('adb pull /sdcard/Pictures/eve-approved-ui', commands)
                self.assertIn('process-failures.log', evidence)

    def test_adb_stdin_cannot_consume_remaining_visual_shards(self):
        for mode in ['gesture', 'three-button']:
            with self.subTest(mode=mode):
                result, commands, _ = self.run_script(mode, drain_instrument_stdin=True)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertEqual(5, commands.count('adb shell am instrument -w -r'))
                self.assertEqual(4, commands.count(' -e visualCases '))

    def test_stdin_isolated_visual_shards_still_fail_on_assertions(self):
        # Verify stdin isolation never converts an on-device failure to PASS,
        # including when the adb subprocess consumes all inherited stdin.
        for mode in ['gesture', 'three-button']:
            with self.subTest(mode=mode):
                result, commands, evidence = self.run_script(
                    mode, assertion_failure=True, drain_instrument_stdin=True
                )
                self.assertNotEqual(0, result.returncode)
                self.assertIn('NATIVE_FAILURE', result.stderr)
                self.assertIn('safe-runtime.log', evidence)
                # Core failure must not silently advance to visual fixtures.
                self.assertEqual(1, commands.count('adb shell am instrument -w -r'))

    def test_instrumentation_failure_is_not_swallowed(self):
        for mode in ['gesture', 'three-button']:
            result, commands, evidence = self.run_script(mode, status=7)
            self.assertEqual(7, result.returncode)
            self.assertIn('adb pull /sdcard/Pictures/eve-approved-ui', commands)
            self.assertIn('safe-runtime.log', evidence)

    def test_assertion_failure_with_adb_exit_zero_still_fails(self):
        result, _, _ = self.run_script('gesture', status=0, assertion_failure=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('NATIVE_FAILURE', result.stderr)

    def test_wrong_navigation_mode_fails_before_instrumentation(self):
        result, commands, _ = self.run_script('three-button', observed=2)
        self.assertEqual(1, result.returncode)
        self.assertIn('Navigation configuration failed', result.stderr)
        self.assertNotIn('adb shell am instrument', commands)

    def test_asynchronous_mode_change_settles_before_running_tests(self):
        result, commands, _ = self.run_script('three-button', pending_reads=2)
        self.assertEqual(0, result.returncode, result.stderr)
        before_gradle = commands.split('adb shell am instrument -w -r')[0]
        self.assertEqual(3, before_gradle.count('shell settings get secure navigation_mode'))

    def test_invalid_mode_fails_and_collects_diagnostics(self):
        result, commands, _ = self.run_script('invalid')
        self.assertEqual(2, result.returncode)
        self.assertNotIn('adb shell am instrument', commands)
        self.assertIn('adb pull', commands)


if __name__ == '__main__':
    unittest.main()
