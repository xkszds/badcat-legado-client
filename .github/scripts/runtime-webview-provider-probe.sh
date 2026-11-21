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

gradle --no-daemon :app:connectedDebugAndroidTest
test_status=$?

# connectedAndroidTest may uninstall the test APK before returning. The test
# writes its bounded JSON report to logcat before teardown, so collect it from
# the still-running emulator rather than relying on UiAutomation shell stdout.
adb -s "$ANDROID_SERIAL" logcat -d -s BadcatWebViewProbe:I \
  > "$diagnostics_dir/adb-report-logcat.txt" 2>&1
capture_status=$?

report_status=0
if [ "$capture_status" -ne 0 ] || [ ! -s "$diagnostics_dir/adb-report-logcat.txt" ]; then
  echo "WebView instrumentation report logcat capture failed"
  report_status=1
else
  python3 - "$diagnostics_dir/adb-report-logcat.txt" "$diagnostics_dir/runtime-2.3-webview-report.json" <<'PY'
import base64
import json
import sys

log_path, report_path = sys.argv[1:]
with open(log_path, encoding="utf-8", errors="replace") as stream:
    encoded = [
        line.split("REPORT_BASE64=", 1)[1].strip()
        for line in stream
        if "REPORT_BASE64=" in line
    ]
if not encoded:
    raise SystemExit("No REPORT_BASE64 payload found in WebView probe logcat")
payload = base64.b64decode(encoded[-1], validate=True)
report = json.loads(payload)
if report.get("task") != "Runtime-2.3":
    raise SystemExit("Unexpected WebView report task marker")
observations = report.get("observations")
expected = {"string", "number", "null", "undefined"}
if not isinstance(observations, list) or {item.get("case") for item in observations} != expected:
    raise SystemExit("WebView report does not contain all four overload observations")
with open(report_path, "w", encoding="utf-8") as stream:
    json.dump(report, stream, ensure_ascii=False, indent=2)
print(json.dumps(report, ensure_ascii=False, indent=2))
PY
  report_status=$?
  if [ "$report_status" -ne 0 ]; then
    echo "WebView instrumentation report could not be decoded or validated"
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
