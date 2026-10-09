"""Host-side contract tests: UI failures must remain failures and retain evidence."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'verify-native-ui.sh'


class NativeUiRunnerTest(unittest.TestCase):
    def run_script(self, mode, status=0, observed=None):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            log = root / 'commands.log'
            for name, body in {
                'adb': '''#!/bin/bash
printf 'adb %s\\n' "$*" >> "$COMMAND_LOG"
case "$*" in
  'shell settings get secure navigation_mode') echo "$OBSERVED_MODE" ;;
  'shell pm path com.android.deskclock') exit 1 ;;
esac
''',
                'gradle': '''#!/bin/bash
printf 'gradle %s\\n' "$*" >> "$COMMAND_LOG"
exit "$GRADLE_STATUS"
''',
            }.items():
                file = root / name
                file.write_text(body)
                file.chmod(0o755)
            env = dict(os.environ, PATH=f'{root}:{os.environ["PATH"]}', COMMAND_LOG=str(log),
                       OBSERVED_MODE=str(observed if observed is not None else (2 if mode == 'gesture' else 0)),
                       GRADLE_STATUS=str(status))
            result = subprocess.run(['bash', str(SCRIPT), mode], cwd=root, env=env,
                                    capture_output=True, text=True)
            commands = log.read_text()
            evidence = sorted(p.name for p in (root / 'ui-renderings').iterdir())
            return result, commands, evidence

    def test_each_mode_runs_full_suite_once_and_retains_evidence(self):
        for mode, overlay in [('gesture', 'gestural'), ('three-button', 'threebutton')]:
            with self.subTest(mode=mode):
                result, commands, evidence = self.run_script(mode)
                self.assertEqual(0, result.returncode, result.stderr)
                self.assertEqual(1, commands.count('gradle connectedDebugAndroidTest'))
                self.assertIn(f'navigationMode={mode}', commands)
                self.assertIn('package=com.eve.app.ui', commands)
                self.assertIn(f'navbar.{overlay}', commands)
                self.assertNotIn('adb reboot', commands)
                self.assertIn('adb pull /sdcard/Pictures/eve-approved-ui', commands)
                self.assertIn('process-failures.log', evidence)

    def test_instrumentation_failure_is_not_swallowed(self):
        for mode in ['gesture', 'three-button']:
            result, commands, evidence = self.run_script(mode, status=7)
            self.assertEqual(7, result.returncode)
            self.assertIn('adb pull /sdcard/Pictures/eve-approved-ui', commands)
            self.assertIn('safe-runtime.log', evidence)

    def test_wrong_navigation_mode_fails_before_instrumentation(self):
        result, commands, _ = self.run_script('three-button', observed=2)
        self.assertEqual(1, result.returncode)
        self.assertIn('Navigation configuration failed', result.stderr)
        self.assertNotIn('gradle connected', commands)

    def test_invalid_mode_fails_and_collects_diagnostics(self):
        result, commands, _ = self.run_script('invalid')
        self.assertEqual(2, result.returncode)
        self.assertNotIn('gradle connected', commands)
        self.assertIn('adb pull', commands)


if __name__ == '__main__':
    unittest.main()
