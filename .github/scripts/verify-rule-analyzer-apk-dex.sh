#!/usr/bin/env bash
set -euo pipefail

apk="${1:-app/build/outputs/apk/debug/app-debug.apk}"
output_dir="build/runtime-rule-analyzer-apk-dex"
mkdir -p "$output_dir"

status="fail"
error="verification did not complete"
apk_sha256=""
class_definition=""
smali_bytes=0
analyzer=""
dex_list="$output_dir/dex-files.txt"
defined_classes="$output_dir/dex-defined-classes.txt"
smali_file="$output_dir/rule-analyzer.smali"
report="$output_dir/apk-dex-verification.json"

write_report() {
  python3 - "$status" "$error" "$apk" "$apk_sha256" "$class_definition" "$smali_bytes" "$analyzer" "$dex_list" "$report" <<'PY'
import json
import sys
from pathlib import Path

status, error, apk, apk_sha256, class_definition, smali_bytes, analyzer, dex_list, report = sys.argv[1:]
dex_path = Path(dex_list)
dex_files = dex_path.read_text(encoding="utf-8").splitlines() if dex_path.exists() else []
apk_path = Path(apk)
payload = {
    "task": "Runtime-3.2f.47",
    "status": status,
    "error": error or None,
    "apk": apk,
    "apkBytes": apk_path.stat().st_size if apk_path.exists() else None,
    "apkSha256": apk_sha256 or None,
    "analyzer": analyzer or None,
    "dexFiles": dex_files,
    "definedClass": class_definition or None,
    "smaliBytes": int(smali_bytes),
    "definitionCheck": "apkanalyzer dex packages --defined-only",
}
Path(report).write_text(json.dumps(payload, ensure_ascii=False, indent=2) + chr(10), encoding="utf-8")
print(json.dumps(payload, ensure_ascii=False, indent=2))
PY
}
trap write_report EXIT

if [ ! -f "$apk" ]; then
  error="APK not found: $apk"
  exit 1
fi

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$sdk_root" ] || [ ! -d "$sdk_root/cmdline-tools" ]; then
  error="Android SDK command-line tools directory not found"
  exit 1
fi

analyzer="$(find "$sdk_root/cmdline-tools" -type f -name apkanalyzer -perm -u+x 2>/dev/null | sort -V | tail -n 1)"
if [ -z "$analyzer" ]; then
  error="apkanalyzer executable not found in Android SDK command-line tools"
  exit 1
fi

if ! "$analyzer" dex list "$apk" > "$dex_list"; then
  error="apkanalyzer could not list APK DEX files"
  exit 1
fi

if ! "$analyzer" dex packages --defined-only "$apk" > "$defined_classes"; then
  error="apkanalyzer could not inspect defined DEX classes"
  exit 1
fi

apk_sha256="$(sha256sum "$apk" | awk '{print $1}')"
class_definition="$(grep -E '^C d[[:space:]]+[0-9]+[[:space:]]+[0-9]+[[:space:]]+[0-9]+[[:space:]]+io[.]legado[.]app[.]model[.]analyzeRule[.]RuleAnalyzer$' "$defined_classes" | head -n 1 || true)"
if [ -z "$class_definition" ]; then
  error="pinned RuleAnalyzer class definition is absent from APK DEX; references alone do not satisfy this gate"
  exit 1
fi

if ! "$analyzer" dex code --class io.legado.app.model.analyzeRule.RuleAnalyzer "$apk" > "$smali_file"; then
  error="apkanalyzer could not decompile the defined RuleAnalyzer class"
  exit 1
fi

smali_bytes="$(wc -c < "$smali_file" | tr -d ' ')"
if [ "$smali_bytes" -le 0 ]; then
  error="RuleAnalyzer DEX class produced an empty code listing"
  exit 1
fi

status="pass"
error=""
