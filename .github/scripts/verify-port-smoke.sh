#!/usr/bin/env bash
set -euo pipefail

guest_crash_pid=""
guest_crash_log="ui-renderings/guest-crash-stream.log"
mkdir -p ui-renderings

cleanup() {
  local exit_code=$?
  if [[ -n "$guest_crash_pid" ]]; then
    kill -9 "$guest_crash_pid" 2>/dev/null || true
    wait "$guest_crash_pid" 2>/dev/null || true
  fi
  # Pull evidence if emulator is still accessible
  if [[ "$(timeout 5s adb get-state 2>/dev/null || true)" == "device" ]]; then
    timeout 12s adb pull /sdcard/Pictures/eve-approved-ui/. ui-renderings/ 2>/dev/null || true
    timeout 12s adb pull /sdcard/ui-renderings/. ui-renderings/ 2>/dev/null || true
  fi
  if [[ "$exit_code" -ne 0 ]]; then
    if [[ -f "${instrumentation_log:-}" ]]; then
      echo "=== INSTRUMENTATION LOG TAIL ==="
      tail -n 100 "$instrumentation_log" || true
    fi
    if [[ -f "${guest_crash_log:-}" ]]; then
      echo "=== GUEST CRASH LOG TAIL ==="
      tail -n 100 "$guest_crash_log" || true
    fi
  fi
  exit "$exit_code"
}
trap cleanup EXIT

echo "=== EVE PORT SMOKE VERIFICATION ==="

# 1. Verify emulator is alive
device_state="$(timeout 15s adb get-state 2>/dev/null || true)"
if [[ "$device_state" != "device" ]]; then
  echo "INFRA: Emulator is offline or not in device state (observed: '$device_state')." >&2
  exit 1
fi

# 2. Wake device, dismiss keyguard, and disable screen timeout
adb shell settings put system screen_off_timeout 2147483647
adb shell settings put global stay_on_while_plugged_in 7
adb shell settings put secure lockscreen.disabled 1
adb shell settings put secure user_setup_complete 1
adb shell settings put global device_provisioned 1
adb shell settings put secure gesture_setup_complete 1
adb shell settings put secure system_navigation_keys_migrated 1
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard

# 3. Configure gesture navigation mode
echo "Configuring gesture navigation mode..."
adb shell am start -W -a android.settings.SETTINGS > ui-renderings/emulator-foreground-setup.log || true
adb shell cmd overlay enable-exclusive --category "com.android.internal.systemui.navbar.gestural"
actual_mode=""
for attempt in $(seq 1 30); do
  actual_mode="$(adb shell settings get secure navigation_mode 2>/dev/null | tr -d '\r')"
  [[ "$actual_mode" == "2" ]] && break
  sleep 1
done

if [[ "$actual_mode" != "2" ]]; then
  echo "INFRA: Gesture navigation configuration failed (expected mode 2, observed '$actual_mode')." >&2
  exit 1
fi
echo "Gesture navigation verified (mode 2)."
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell input keyevent KEYCODE_BACK
adb shell input keyevent KEYCODE_HOME
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS || true

# 4. Install prebuilt APKs
echo "Installing prebuilt APKs..."
if ! adb install -r app/build/outputs/apk/debug/app-debug.apk; then
  echo "INFRA: Failed to install app-debug.apk." >&2
  exit 1
fi

if ! adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk; then
  echo "INFRA: Failed to install app-debug-androidTest.apk." >&2
  exit 1
fi

# 5. Push port manifest as additional asset location
if [[ -f docs/port-manifest.json ]]; then
  adb push docs/port-manifest.json /sdcard/port-manifest.json 2>/dev/null || true
fi

# 6. Clear logcat and start crash monitor
adb logcat -c || true
timeout 1200s adb logcat -b crash -b events -v threadtime > "$guest_crash_log" 2>&1 &
guest_crash_pid=$!

# 7. Run PortedScreensSmokeTest instrumentation
echo "Running PortedScreensSmokeTest..."
instrumentation_log="ui-renderings/port-smoke-instrumentation.log"
set +e
timeout 600s adb shell am instrument -w -r \
  -e navigationMode gesture \
  -e class com.eve.app.ui.PortedScreensSmokeTest \
  com.eve.app.test/androidx.test.runner.AndroidJUnitRunner </dev/null \
  | tee "$instrumentation_log"
run_status=$?
set -e

# 8. Check device status after test execution
post_state="$(timeout 5s adb get-state 2>/dev/null || true)"
if [[ "$post_state" != "device" ]]; then
  echo "INFRA: Emulator died or disconnected during instrumentation execution." >&2
  exit 1
fi

# 9. Analyze instrumentation results
if grep -qE "INSTRUMENTATION_FAILED|Process crashed" "$instrumentation_log"; then
  echo "INFRA: Test runner crashed or failed to start." >&2
  exit 1
fi

if grep -qE "FAILURES!!!|FAILURE in" "$instrumentation_log"; then
  echo "PORT SMOKE FAILURE: Test assertions or screen smoke checks failed." >&2
  exit 1
fi

if ! grep -qE "OK \([0-9]+ test" "$instrumentation_log" && ! grep -q "INSTRUMENTATION_CODE: -1" "$instrumentation_log"; then
  echo "INFRA: Instrumentation did not complete cleanly or ran 0 tests." >&2
  exit 1
fi

# 10. Pull and verify screenshots
echo "Pulling screenshots from emulator..."
mkdir -p ui-renderings
timeout 15s adb pull /sdcard/Pictures/eve-approved-ui/. ui-renderings/ 2>/dev/null || true
timeout 15s adb pull /sdcard/ui-renderings/. ui-renderings/ 2>/dev/null || true

# Verify that at least one port screenshot was saved
if ! compgen -G "ui-renderings/port-*.png" > /dev/null; then
  echo "PORT SMOKE FAILURE: No ui-renderings/port-*.png screenshots were generated." >&2
  exit 1
fi

echo "PORT SMOKE PASSED: All ported screens launched, rendered, rotated, and navigated back cleanly."
exit 0
