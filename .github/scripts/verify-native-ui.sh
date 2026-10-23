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
  for fixture in ios26-nav-bar ios26-tab-bar ios26-menu ios26-sheet ios26-dialog home home-attached mock-test recovery-result-overview student-settings student-login; do
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
  adb logcat -d -s AndroidRuntime ThemeSwitchAnimator ThemeManager SystemBarHelper > ui-renderings/safe-runtime.log || true
  # Keep transaction evidence readable even when artifact storage cannot be downloaded.
  cat ui-renderings/safe-runtime.log || true
  # The hosted runner has grep but does not include ripgrep.
  adb shell dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp|mAppearance|appearance=|mNavBarColor|mNavigationBarColor' > ui-renderings/window-appearance.log || true
  adb shell dumpsys activity service SystemUIService | grep -Ei 'appearance|darkIntensity|lightBar|navigationMode|navBarColor|lightNavigation|navigationLight|navigationBarMode|mForce.*Scrim' > ui-renderings/systemui-appearance.log || true
  adb shell settings get secure navigation_mode > ui-renderings/navigation-mode.log || true
  # Disposable fixture emulator only: capture ANR categories, not application payloads.
  adb logcat -d -b events -s am_anr am_crash > ui-renderings/process-failures.log || true
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
status=0
instrumentation_log="ui-renderings/instrumentation-${mode}.log"
timeout 660s adb shell am instrument -w -r \
  -e package com.eve.app.ui -e navigationMode "$mode" \
  com.eve.app.test/androidx.test.runner.AndroidJUnitRunner \
  | tee "$instrumentation_log" || status=$?
# adb may return zero even when a test fails; parse each test and completion.
if ! python3 .github/scripts/ci_instrumentation_report.py "$instrumentation_log"; then
  [[ "$status" != "0" ]] || status=1
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
