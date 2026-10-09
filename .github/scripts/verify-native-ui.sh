#!/usr/bin/env bash
set -euo pipefail
collect_evidence() {
  mkdir -p ui-renderings
  adb pull /sdcard/Pictures/eve-approved-ui ui-renderings/ || true
  adb logcat -d -s AndroidRuntime ThemeSwitchAnimator ThemeManager SystemBarHelper > ui-renderings/safe-runtime.log || true
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
adb shell cmd overlay enable-exclusive --category "com.android.internal.systemui.navbar.$overlay"
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
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
status=0
gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.eve.app.ui "-Pandroid.testInstrumentationRunnerArguments.navigationMode=$mode" --stacktrace || status=$?
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
