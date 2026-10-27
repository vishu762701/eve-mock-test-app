#!/usr/bin/env bash
set -euo pipefail
collect_evidence() {
  mkdir -p ui-renderings
  adb pull /sdcard/Pictures/eve-approved-ui ui-renderings/ || true
  adb pull /sdcard/Pictures/eve-student-ui ui-renderings/ || true
  # Small deterministic shared-style fixtures only. Keep review possible through
  # decoded job logs when the reviewer cannot reach the artifact storage host.
  # Never stream live activity screenshots or application data into logs.
  for appearance in "" "-high-contrast"; do
    for theme in light dark; do
      fixture="shared-components${appearance}-${theme}.png"
      fixture_path="/sdcard/Pictures/eve-approved-ui/$fixture"
      if adb shell test -f "$fixture_path"; then
        echo "EVE_DESIGN_PNG_BEGIN $fixture"
        adb shell base64 "$fixture_path" || true
        echo "EVE_DESIGN_PNG_END $fixture"
      fi
    done
  done
  # Disposable production-layout fixtures only; publish no extra artifact.
  for fixture in ios26-nav-bar ios26-tab-bar ios26-menu ios26-sheet ios26-dialog source-main-tabs home home-attached mock-test recovery-result-overview student-settings student-login; do
    for theme in light dark; do
      for folder in eve-approved-ui eve-student-ui; do
        fixture_path="/sdcard/Pictures/$folder/$fixture-$theme.png"
        if adb shell test -f "$fixture_path"; then
          echo "EVE_DESIGN_PNG_BEGIN $fixture-$theme.png"
          adb shell base64 "$fixture_path" || true
          echo "EVE_DESIGN_PNG_END $fixture-$theme.png"
        fi
      done
    done
  done
  # The rendering matrix may stall without a JUnit result. Record only the
  # disposable fixture progress tag so the last completed stage is visible.
  adb logcat -d -s EveStudentUi:I > ui-renderings/student-fixture-progress.log || true
  tail -n 100 ui-renderings/student-fixture-progress.log || true
  adb logcat -d -s AndroidRuntime ThemeSwitchAnimator ThemeManager SystemBarHelper EveBulletin > ui-renderings/safe-runtime.log || true
  # Keep transaction evidence readable even when artifact storage cannot be downloaded.
  cat ui-renderings/safe-runtime.log || true
  # The hosted runner has grep but does not include ripgrep.
  adb shell dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp|mAppearance|appearance=|mNavBarColor|mNavigationBarColor' > ui-renderings/window-appearance.log || true
  # Surface the already-filtered focus evidence when an external window blocks tests.
  cat ui-renderings/window-appearance.log || true
  adb shell dumpsys activity service SystemUIService | grep -Ei 'appearance|darkIntensity|lightBar|navigationMode|navBarColor|lightNavigation|navigationLight|navigationBarMode|mForce.*Scrim' > ui-renderings/systemui-appearance.log || true
  adb shell settings get secure navigation_mode > ui-renderings/navigation-mode.log || true
  # Disposable fixture emulator only: capture ANR categories, not application payloads.
  adb logcat -d -b events -s am_anr am_crash > ui-renderings/process-failures.log || true
  cat ui-renderings/process-failures.log || true
  if [[ "${messaging_disabled:-0}" == 1 ]]; then
    adb shell pm enable --user 0 com.google.android.apps.messaging || true
  fi
}
trap collect_evidence EXIT
# Each matrix job owns a fresh emulator and runs exactly one navigation mode.
# Never reboot or switch modes between suites: run #227's second phase captured
# a SystemUI ANR. Isolation removes that cross-suite state, not the assertions.
mode="${1:?Usage: verify-native-ui.sh gesture|three-button}"
case "$mode" in
  gesture) overlay=gestural; expected_mode=2 ;;
  three-button) overlay=threebutton; expected_mode=0 ;;
  *) echo "Unsupported navigation mode: $mode" >&2; exit 2 ;;
esac
mkdir -p ui-renderings
# FULL run 40469943287 recorded a carrier-config ANR in the emulator's
# unrelated Messages app, whose system dialog obscured the test activity.
# This disposable emulator does not exercise SMS. Restore the package on exit;
# retain all EVE tests and fail on every instrumentation assertion as before.
messaging_disabled=0
if adb shell pm path com.google.android.apps.messaging | grep -q '^package:'; then
  adb shell pm disable-user --user 0 com.google.android.apps.messaging
  messaging_disabled=1
fi
adb shell getprop ro.build.fingerprint > ui-renderings/emulator-fingerprint.log
adb shell wm size > ui-renderings/display-size.log
adb shell wm density > ui-renderings/display-density.log
adb shell settings put system screen_off_timeout 2147483647
adb shell settings put global stay_on_while_plugged_in 7
adb shell settings put secure lockscreen.disabled 1
adb shell settings put secure user_setup_complete 1
adb shell settings put global device_provisioned 1
adb shell settings put secure gesture_setup_complete 1
adb shell settings put secure system_navigation_keys_migrated 1
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
# Keep a stock foreground window while Gradle prepares the APKs. Changing the
# navigation overlay over a still-starting Pixel Launcher produced external
# input-dispatch ANRs that obscured every subsequent application capture. Leave
# Launcher/Quickstep and SystemUI enabled; app ANRs and test failures still fail.
adb shell am start -W -a android.settings.SETTINGS > ui-renderings/emulator-foreground-setup.log
adb shell cmd overlay enable-exclusive --category "com.android.internal.systemui.navbar.$overlay"
# OverlayManager returns before SystemUI updates the secure setting. Run #228
# read 2 immediately, then captured 0 in its exit diagnostics. Wait for the
# requested state, with a hard bound; never run under the wrong mode.
actual_mode=""
for attempt in $(seq 1 30); do
  actual_mode="$(adb shell settings get secure navigation_mode | tr -d '\r')"
  [[ "$actual_mode" == "$expected_mode" ]] && break
  sleep 1
done
if [[ "$actual_mode" != "$expected_mode" ]]; then
  echo "Navigation configuration failed: expected $expected_mode, observed $actual_mode" >&2
  exit 1
fi
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
# Install precompiled, checksum-verified binaries; do not run Gradle per emulator.
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# The full student rendering matrix covers 44 fixtures x two themes x three
# viewport/font configurations. Running it inside the 53-test command exceeded
# the 660-second instrumentation bound in BOTH navigation modes (run 40107857698).
# Shard ONLY that expensive test; preserve every fixture and all other tests.
# Each invocation is independently checked for terminal completion and failures.
run_instrumentation() {
  local label="$1"
  shift
  local instrumentation_log="ui-renderings/instrumentation-${mode}-${label}.log"
  local run_status=0
  # adb shell can consume the loop stdin that holds subsequent visual shard names.
  # Disconnect stdin without altering test selection, timeout, or failure handling.
  timeout 660s adb shell am instrument -w -r \
    -e navigationMode "$mode" "$@" \
    com.eve.app.test/androidx.test.runner.AndroidJUnitRunner </dev/null \
    | tee "$instrumentation_log" || run_status=$?
  local report=""
  if ! report="$(python3 "$(dirname "${BASH_SOURCE[0]}")/ci_instrumentation_report.py" "$instrumentation_log")"; then
    run_status=1
  else
    echo "$report"
    # Fail closed if an exclusion accidentally drops another test, or if a
    # visualCases shard executes zero/multiple test methods.
    if [[ "$report" =~ ^NATIVE_PASS[[:space:]]([0-9]+)/([0-9]+) ]]; then
      local completed="${BASH_REMATCH[1]}"
      if [[ "$label" == core && "$completed" -lt 52 ]] || \
         [[ "$label" != core && "$completed" -ne 1 ]]; then
        echo "Unexpected instrumentation coverage for $label: $report" >&2
        run_status=1
      fi
    else
      echo "Unrecognized instrumentation report for $label: $report" >&2
      run_status=1
    fi
  fi
  return "$run_status"
}

status=0
# Keep the full existing package suite, excluding exactly the long rendering
# method. The 52-test minimum ensures the avatar contrast test is not lost.
if run_instrumentation core -e package com.eve.app.ui \
    -e notClass com.eve.app.ui.StudentUiRenderingTest#allStudentScreensRenderInBothThemesAndPhoneSizes; then
  :
else
  status=$?
fi

# Derive shards from the production-layout fixture registry itself: no copied
# case list can silently go stale when screens are added or renamed.
if [[ "$status" == 0 ]]; then
  case_batches="$(python3 - <<'PY'
from pathlib import Path
import re

source = Path("app/src/androidTest/java/com/eve/app/ui/StudentUiRenderingTest.kt").read_text()
match = re.search(r"private val cases = linkedMapOf\((.*?)\n    \)", source, re.S)
if not match:
    raise SystemExit("Cannot locate native student fixture registry")
names = re.findall(r'"([^"]+)"\s+to\s+R\.layout\.', match.group(1))
if len(names) < 44 or len(names) != len(set(names)):
    raise SystemExit(f"Invalid native student fixture inventory: {len(names)} cases")
for start in range(0, len(names), 11):
    print(",".join(names[start:start + 11]))
PY
  )"
  shard=0
  while IFS= read -r visual_cases; do
    [[ -n "$visual_cases" ]] || continue
    shard=$((shard + 1))
    echo "Native visual shard $shard: $visual_cases"
    if run_instrumentation "visual-$shard" \
        -e class com.eve.app.ui.StudentUiRenderingTest#allStudentScreensRenderInBothThemesAndPhoneSizes \
        -e visualCases "$visual_cases"; then
      :
    else
      status=$?
      break
    fi
  done <<< "$case_batches"
  if [[ "$shard" -lt 4 ]]; then
    echo "Native visual fixture shards incomplete: $shard" >&2
    status=1
  fi
fi
if [[ "$status" != "0" ]]; then
  adb shell cmd overlay lookup com.android.systemui com.android.systemui:color/light_mode_icon_color_single_tone > ui-renderings/systemui-light-icon-color.log || true
  adb shell cmd overlay lookup com.android.systemui com.android.systemui:color/dark_mode_icon_color_single_tone > ui-renderings/systemui-dark-icon-color.log || true
  if adb shell pm path com.android.deskclock | grep -q '^package:'; then
    adb shell am start -n com.android.deskclock/.DeskClock > ui-renderings/stock-clock-launch.log || true
    sleep 2
    adb shell screencap -p /sdcard/Pictures/eve-approved-ui/stock-clock-three-button-control.png || true
    adb shell dumpsys window displays | grep -E 'mCurrentFocus|mFocusedApp' > ui-renderings/stock-clock-focus.log || true
  fi
fi
exit "$status"
