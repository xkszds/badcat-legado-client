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
    {
      echo "attempts=$attempt"
      echo "status=pass"
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
