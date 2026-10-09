#!/usr/bin/env bash
set -euo pipefail
collect_evidence() {
  mkdir -p ui-renderings
  adb pull /sdcard/Pictures/eve-approved-ui ui-renderings/ || true
  adb logcat -d -s AndroidRuntime ThemeSwitchAnimator SystemBarHelper > ui-renderings/safe-runtime.log || true
  # The hosted runner has grep but does not include ripgrep.
  adb shell dumpsys window windows | grep -E 'mCurrentFocus|mFocusedApp|mAppearance|appearance=|mNavBarColor|mNavigationBarColor' > ui-renderings/window-appearance.log || true
  adb shell dumpsys activity service SystemUIService | grep -Ei 'appearance|darkIntensity|lightBar|navigationMode|navBarColor|lightNavigation|navigationLight|navigationBarMode|mForce.*Scrim' > ui-renderings/systemui-appearance.log || true
  adb shell settings get secure navigation_mode > ui-renderings/navigation-mode.log || true
}
trap collect_evidence EXIT
adb shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural
gesture_status=0
gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.eve.app.ui -Pandroid.testInstrumentationRunnerArguments.navigationMode=gesture --stacktrace || gesture_status=$?
mkdir -p ui-renderings
cp -R app/build/reports/androidTests ui-renderings/gesture-android-tests
cp -R app/build/outputs/androidTest-results ui-renderings/gesture-test-results
adb shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton
# Boot into the chosen navigation configuration. Hot-swapping from gesture
# navigation left this API35 image's button drawables dim even when both the
# focused app and SystemUI reported dark icons disabled / darkIntensity=0.
# Keep the rendered contrast assertion; don't compensate in application colors.
adb reboot
timeout 90 adb wait-for-device
boot_ready=false
for attempt in $(seq 1 90); do
  if [[ "$(adb shell getprop sys.boot_completed | tr -d '\r')" == "1" ]]; then
    boot_ready=true
    break
  fi
  sleep 1
done
[[ "$boot_ready" == "true" ]]
adb shell input keyevent 82
[[ "$(adb shell settings get secure navigation_mode | tr -d '\r')" == "0" ]]
three_button_status=0
gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.eve.app.ui.RepairRegressionTest#realThemeRecreationPreservesStateAndCapturesBothSystemBarsWithoutOverlays -Pandroid.testInstrumentationRunnerArguments.navigationMode=three-button --stacktrace || three_button_status=$?
if [[ "$three_button_status" != "0" ]]; then
  adb shell cmd overlay lookup com.android.systemui com.android.systemui:color/light_mode_icon_color_single_tone > ui-renderings/systemui-light-icon-color.log || true
  adb shell cmd overlay lookup com.android.systemui com.android.systemui:color/dark_mode_icon_color_single_tone > ui-renderings/systemui-dark-icon-color.log || true
  if adb shell pm path com.android.deskclock | grep -q '^package:'; then
    adb shell am start -n com.android.deskclock/.DeskClock > ui-renderings/stock-clock-launch.log || true
    sleep 2
    adb shell screencap -p /sdcard/Pictures/eve-approved-ui/stock-clock-three-button-control.png || true
    adb shell dumpsys window displays | grep -E 'mCurrentFocus|mFocusedApp' > ui-renderings/stock-clock-focus.log || true
  fi
fi
if [[ "$gesture_status" != "0" || "$three_button_status" != "0" ]]; then exit 1; fi
