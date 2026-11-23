#!/usr/bin/env bash
# Keep the entire emulator preflight, instrumentation, and report capture in
# one Bash process. android-emulator-runner dispatches its script input as
# individual shell commands, so stateful logic belongs in this checked-in file.
set +e

diagnostics_dir="${RUNTIME_WEBVIEW_REPORT_DIR:-build/runtime-2.3-webview}"
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
  python3 - "$diagnostics_dir/adb-report-logcat.txt" "$diagnostics_dir/runtime-2.3-webview-report.json" "${EXPECTED_WEBVIEW_SDK:-}" <<'PY'
import base64
import json
import sys

log_path, report_path, expected_sdk = sys.argv[1:]
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
if expected_sdk and report.get("sdkInt") != int(expected_sdk):
    raise SystemExit("WebView report SDK does not match expected emulator API")
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
legado_report_status=0
if [ "${EXPECTED_WEBVIEW_SDK:-}" = "35" ]; then
  adb -s "$ANDROID_SERIAL" logcat -d -s BadcatLegadoE2E:I \
    > "$diagnostics_dir/adb-legado-e2e-report-logcat.txt" 2>&1
  if [ ! -s "$diagnostics_dir/adb-legado-e2e-report-logcat.txt" ]; then
    echo "Legado public-source E2E report logcat capture is empty"
    legado_report_status=1
  else
    python3 - "$diagnostics_dir/adb-legado-e2e-report-logcat.txt" \
      "$diagnostics_dir/legado-runtime-3.2f.45-report.json" <<'PY'
import base64
import json
import re
import sys

log_path, report_path = sys.argv[1:]
with open(log_path, encoding="utf-8", errors="replace") as stream:
    lines = list(stream)
parts = {}
part_count = None
for line in lines:
    marker = "REPORT_PART="
    if marker not in line:
        continue
    value = line.split(marker, 1)[1].strip()
    header, separator, chunk = value.partition(":")
    if not separator or "/" not in header:
        continue
    index_text, count_text = header.split("/", 1)
    index, count = int(index_text), int(count_text)
    if part_count is not None and count != part_count:
        raise SystemExit("Inconsistent Legado report part count")
    part_count = count
    parts[index] = chunk
if part_count is None or set(parts) != set(range(part_count)):
    raise SystemExit("Legado E2E report is missing one or more logcat chunks")
encoded = "".join(parts[index] for index in range(part_count))
report = json.loads(base64.b64decode(encoded, validate=True))
if report.get("task") != "Runtime-3.2f.45" or report.get("sdkInt") != 35:
    raise SystemExit("Unexpected task or emulator API in Legado E2E report")
sources = report.get("sources")
if not isinstance(sources, list) or len(sources) != 4:
    raise SystemExit("Legado E2E report must contain exactly four source outcomes")
for source in sources:
    if not re.fullmatch(r"[0-9a-f]{64}", source.get("fixtureSha256", "")):
        raise SystemExit("Fixture SHA-256 is missing or malformed")
    if not isinstance(source.get("stages"), list) or not source.get("e2eStatus"):
        raise SystemExit("A source outcome is missing stages or e2eStatus")
with open(report_path, "w", encoding="utf-8") as stream:
    json.dump(report, stream, ensure_ascii=False, indent=2)
print(json.dumps(report, ensure_ascii=False, indent=2))
PY
    legado_report_status=$?
  fi
fi

if [ "$test_status" -ne 0 ] || [ "$report_status" -ne 0 ] || [ "$legado_report_status" -ne 0 ]; then
  adb -s "$ANDROID_SERIAL" logcat -d -b all -t 4000 \
    > "$diagnostics_dir/adb-post-test-logcat.txt" 2>&1
fi

if [ "$test_status" -ne 0 ]; then
  exit "$test_status"
fi
if [ "$report_status" -ne 0 ]; then
  exit "$report_status"
fi
exit "$legado_report_status"
