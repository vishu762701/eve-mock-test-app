#!/usr/bin/env bash
set -euo pipefail
collect_evidence() {
  mkdir -p ui-renderings
  adb pull /sdcard/Pictures/eve-approved-ui ui-renderings/ || true
  adb logcat -d -s AndroidRuntime ThemeSwitchAnimator SystemBarHelper > ui-renderings/safe-runtime.log || true
}
trap collect_evidence EXIT
adb shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.gestural
gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.eve.app.ui -Pandroid.testInstrumentationRunnerArguments.navigationMode=gesture --stacktrace
mkdir -p ui-renderings
cp -R app/build/reports/androidTests ui-renderings/gesture-android-tests
cp -R app/build/outputs/androidTest-results ui-renderings/gesture-test-results
adb shell cmd overlay enable-exclusive --category com.android.internal.systemui.navbar.threebutton
gradle connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.eve.app.ui.RepairRegressionTest#realThemeRecreationPreservesStateAndCapturesBothSystemBarsWithoutOverlays -Pandroid.testInstrumentationRunnerArguments.navigationMode=three-button --stacktrace
