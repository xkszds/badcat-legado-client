#!/usr/bin/env bash
set -euo pipefail

api="${1:?Usage: verify-rule-analyzer-instrumentation-results.sh <api-level>}"
case "$api" in 28|35) ;; *) echo "Unsupported emulator API: $api"; exit 2 ;; esac

output_dir="build/runtime-rule-analyzer-api$api"
mkdir -p "$output_dir"

python3 - "$api" "$output_dir" <<'PY'
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

api, output_dir = sys.argv[1], Path(sys.argv[2])
roots = [
    Path("app/build/outputs/androidTest-results/connected"),
    Path("app/build/reports/androidTests/connected"),
]
xml_files = sorted({p for root in roots if root.exists() for p in root.rglob("*.xml")})
target_class = "PinnedLegadoRuleAnalyzerPackagingInstrumentedTest"
target_method = "authenticPinnedRuleAnalyzerIsPresentAndCallableFromTargetApk"
matches = []
total_tests = total_failures = total_errors = total_skipped = 0
for path in xml_files:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    suites = [root] if root.tag == "testsuite" else root.findall(".//testsuite")
    for suite in suites:
        total_tests += int(suite.attrib.get("tests", "0"))
        total_failures += int(suite.attrib.get("failures", "0"))
        total_errors += int(suite.attrib.get("errors", "0"))
        total_skipped += int(suite.attrib.get("skipped", "0"))
    for case in root.iter("testcase"):
        classname = case.attrib.get("classname", "")
        name = case.attrib.get("name", "")
        if target_class in classname and name == target_method:
            matches.append({
                "file": str(path),
                "classname": classname,
                "name": name,
                "failures": len(case.findall("failure")),
                "errors": len(case.findall("error")),
                "skipped": len(case.findall("skipped")),
            })

if len(matches) != 1:
    raise SystemExit(f"Expected exactly one {target_class}.{target_method} result; found {len(matches)} in {len(xml_files)} XML files")
test = matches[0]
if test["failures"] or test["errors"] or test["skipped"]:
    raise SystemExit(f"RuleAnalyzer instrumentation did not pass: {test}")
if not xml_files:
    raise SystemExit("No Android instrumentation XML reports were produced")

report = {
    "task": "Runtime-3.2f.47",
    "apiLevel": int(api),
    "status": "pass",
    "test": test,
    "junitTotals": {
        "tests": total_tests,
        "failures": total_failures,
        "errors": total_errors,
        "skipped": total_skipped,
    },
    "xmlReports": [str(p) for p in xml_files],
}
report_path = output_dir / "rule-analyzer-instrumentation.json"
report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report, ensure_ascii=False, indent=2))
PY
