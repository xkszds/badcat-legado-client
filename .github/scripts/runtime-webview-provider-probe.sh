#!/usr/bin/env bash
# Keep the entire emulator preflight, instrumentation, and report capture in
# one Bash process. android-emulator-runner dispatches its script input as
# individual shell commands, so stateful logic belongs in this checked-in file.
set +e

diagnostics_dir="build/runtime-2.3-webview"
mkdir -p "$diagnostics_dir"

adb version > "$diagnostics_dir/adb-version.txt" 2>&1
adb devices -l > "$diagnostics_dir/adb-devices-before.txt" 2>&1

device_state="$(adb -s "$ANDROID_SERIAL" get-state 2>&1)"
device_state_status=$?
boot_state="$(adb -s "$ANDROID_SERIAL" shell getprop sys.boot_completed 2>&1)"
boot_state_status=$?

printf 'serial=%s\nget_state_status=%s\ndevice_state=%s\nboot_property_status=%s\nsys.boot_completed=%s\n' \
  "$ANDROID_SERIAL" "$device_state_status" "$device_state" "$boot_state_status" "$boot_state" \
  > "$diagnostics_dir/adb-preflight.txt"

if [ "$device_state_status" -ne 0 ] || [ "$device_state" != "device" ] || \
   [ "$boot_state_status" -ne 0 ] || [ "$boot_state" != "1" ]; then
  echo "ADB preflight failed; capturing guest logcat before stopping"
  adb -s "$ANDROID_SERIAL" logcat -d -b all -t 2000 \
    > "$diagnostics_dir/adb-preflight-logcat.txt" 2>&1
  exit 1
fi

report_path="/data/local/tmp/runtime-2.3-webview-report.json"
adb -s "$ANDROID_SERIAL" shell rm -f "$report_path" \
  > "$diagnostics_dir/adb-report-cleanup.txt" 2>&1
cleanup_status=$?
if [ "$cleanup_status" -ne 0 ]; then
  echo "Could not clear stale WebView instrumentation report"
  exit "$cleanup_status"
fi

gradle --no-daemon :app:connectedDebugAndroidTest
test_status=$?

adb -s "$ANDROID_SERIAL" exec-out cat "$report_path" \
  > "$diagnostics_dir/runtime-2.3-webview-report.json" \
  2> "$diagnostics_dir/adb-report-capture.txt"
capture_status=$?

report_status=0
if [ "$capture_status" -ne 0 ] || [ ! -s "$diagnostics_dir/runtime-2.3-webview-report.json" ]; then
  echo "WebView instrumentation report was not exported or could not be retrieved after Gradle uninstall"
  report_status=1
else
  python3 -m json.tool "$diagnostics_dir/runtime-2.3-webview-report.json" >/dev/null
  json_status=$?
  if [ "$json_status" -ne 0 ]; then
    echo "WebView instrumentation report is not valid JSON"
    report_status=1
  else
    cat "$diagnostics_dir/runtime-2.3-webview-report.json"
  fi
fi

adb devices -l > "$diagnostics_dir/adb-devices-after.txt" 2>&1
if [ "$test_status" -ne 0 ] || [ "$report_status" -ne 0 ]; then
  adb -s "$ANDROID_SERIAL" logcat -d -b all -t 4000 \
    > "$diagnostics_dir/adb-post-test-logcat.txt" 2>&1
fi

if [ "$test_status" -ne 0 ]; then
  exit "$test_status"
fi
exit "$report_status"
