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
gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.eve.app.ui -Pandroid.testInstrumentationRunnerArguments.navigationMode=gesture --stacktrace
mkdir -p ui-renderings
cp -R app/build/reports/androidTests ui-renderings/gesture-android-tests
cp -R app/build/outputs/androidTest-results ui-renderings/gesture-test-results
adb shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton
gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.eve.app.ui.RepairRegressionTest#realThemeRecreationPreservesStateAndCapturesBothSystemBarsWithoutOverlays -Pandroid.testInstrumentationRunnerArguments.navigationMode=three-button --stacktrace
