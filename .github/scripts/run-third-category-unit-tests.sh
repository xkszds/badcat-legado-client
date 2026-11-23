#!/usr/bin/env bash
set -uo pipefail

work_dir="build/third-category-unit-test-retry"
mkdir -p "$work_dir"
summary="$work_dir/retry-summary.txt"

should_retry_exact_robolectric_429() {
  local log_file="$1"
  grep -Fq 'nativeruntime-dist-compat-1.0.16.jar' "$log_file" &&
    grep -Eq 'HTTP 429|status code 429' "$log_file"
}

if [ "${1:-}" = "--self-test" ]; then
  temp_dir="$(mktemp -d)"
  trap 'rm -rf "$temp_dir"' EXIT
  printf '%s\n' "Could not download nativeruntime-dist-compat-1.0.16.jar" "Received status code 429 from server" > "$temp_dir/exact-429.log"
  printf '%s\n' "Could not download unrelated-library.jar" "Received status code 429 from server" > "$temp_dir/other-429.log"
  printf '%s\n' "Execution failed for task" "AssertionError: expected true" > "$temp_dir/assertion.log"
  should_retry_exact_robolectric_429 "$temp_dir/exact-429.log" || { echo "Exact Robolectric 429 must be retryable"; exit 1; }
  ! should_retry_exact_robolectric_429 "$temp_dir/other-429.log" || { echo "Unrelated 429 must not be retryable"; exit 1; }
  ! should_retry_exact_robolectric_429 "$temp_dir/assertion.log" || { echo "Assertion failure must not be retryable"; exit 1; }
  echo "RETRY_CLASSIFIER_SELF_TEST=PASS"
  exit 0
fi

max_attempts=3
{
  echo "task=:app:testDebugUnitTest"
  echo "profile=badcatThirdCategoryTestProfile"
  echo "max_attempts=$max_attempts"
  echo "retry_condition=exact nativeruntime-dist-compat-1.0.16.jar plus HTTP 429"
  echo "status=running"
} > "$summary"

for attempt in $(seq 1 "$max_attempts"); do
  log_file="$work_dir/attempt-$attempt.log"
  echo "UNIT_TEST_ATTEMPT=$attempt/$max_attempts"
  gradle --no-daemon :app:testDebugUnitTest -PbadcatThirdCategoryTestProfile=true 2>&1 | tee "$log_file"
  status=${PIPESTATUS[0]}

  if [ "$status" -eq 0 ]; then
    if ! python3 - "$work_dir" <<'PY'
import json
import shutil
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

work_dir = Path(sys.argv[1])
report_dir = Path("app/build/test-results/testDebugUnitTest")
xml_files = sorted(report_dir.glob("TEST-*.xml")) if report_dir.exists() else []
if not xml_files:
    raise SystemExit("No JUnit XML reports found for :app:testDebugUnitTest")

tests = failures = errors = skipped = 0
for path in xml_files:
    root = ET.parse(path).getroot()
    suites = [root] if root.tag == "testsuite" else (
        list(root.findall("testsuite")) if root.tag == "testsuites" else list(root.findall(".//testsuite"))
    )
    for suite in suites:
        tests += int(suite.attrib.get("tests", "0"))
        failures += int(suite.attrib.get("failures", "0"))
        errors += int(suite.attrib.get("errors", "0"))
        skipped += int(suite.attrib.get("skipped", "0"))
if tests <= 0:
    raise SystemExit("JUnit reports contain no executed tests")
if failures or errors:
    raise SystemExit(f"JUnit reports contradict Gradle success: {failures} failures, {errors} errors")

junit_dir = work_dir / "junit"
junit_dir.mkdir(parents=True, exist_ok=True)
for path in xml_files:
    shutil.copy2(path, junit_dir / path.name)
report = {
    "task": ":app:testDebugUnitTest",
    "profile": "badcatThirdCategoryTestProfile",
    "status": "pass",
    "tests": tests,
    "failures": failures,
    "errors": errors,
    "skipped": skipped,
    "xmlFiles": [path.name for path in xml_files],
}
(work_dir / "junit-summary.json").write_text(
    json.dumps(report, ensure_ascii=False, indent=2) + "\n",
    encoding="utf-8",
)
print(f"JUNIT_TESTS={tests} JUNIT_FAILURES={failures} JUNIT_ERRORS={errors} JUNIT_SKIPPED={skipped}")
PY
    then
      sed -i 's/^status=running$/status=fail/' "$summary"
      {
        echo "attempts=$attempt"
        echo "exit_code=1"
        echo "failure_class=junit_report_missing_or_inconsistent"
      } >> "$summary"
      exit 1
    fi
    sed -i 's/^status=running$/status=pass/' "$summary"
    {
      echo "attempts=$attempt"
      echo "exit_code=0"
    } >> "$summary"
    exit 0
  fi

  if [ "$attempt" -lt "$max_attempts" ] && should_retry_exact_robolectric_429 "$log_file"; then
    delay=$((attempt * 15))
    echo "RETRY_REASON=exact_robolectric_dependency_http_429"
    echo "RETRY_DELAY_SECONDS=$delay"
    sleep "$delay"
    continue
  fi

  {
    echo "attempts=$attempt"
    echo "status=fail"
    echo "exit_code=$status"
    if should_retry_exact_robolectric_429 "$log_file"; then
      echo "failure_class=external_maven_429_retry_budget_exhausted"
    else
      echo "failure_class=non_retryable_gradle_failure"
    fi
  } >> "$summary"
  exit "$status"
done

echo "status=fail" >> "$summary"
exit 1
