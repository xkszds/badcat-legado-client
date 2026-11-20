#!/usr/bin/env python3
"""Generate a source-level JsExtensions ABI gap report against the pinned Legado tree.

This is an audit aid, not proof of JVM/JavaScript ABI equivalence. It records
complete source declarations and highlights missing names/overloads/signatures.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


def collect_methods(path: Path, declaration: str) -> list[dict[str, object]]:
    lines = path.read_text(encoding="utf-8").splitlines()
    start = None
    for index, line in enumerate(lines):
        if declaration in line:
            start = index
            break
    if start is None:
        raise SystemExit(f"Could not find declaration {declaration!r} in {path}")

    # Only direct members at four-space indentation belong to the interface/class.
    methods: list[dict[str, object]] = []
    index = start + 1
    while index < len(lines):
        line = lines[index]
        if line and not line.startswith("    ") and line.strip() == "}":
            break
        if len(line) - len(line.lstrip()) != 4:
            index += 1
            continue
        match = re.match(r"^    (?:override\\s+)?fun\\s+([A-Za-z_][A-Za-z0-9_]*)\\s*\\(", line)
        if not match:
            index += 1
            continue
        name = match.group(1)
        parts = [line.strip()]
        depth = line.count("(") - line.count(")")
        while depth > 0 and index + 1 < len(lines):
            index += 1
            continuation = lines[index]
            parts.append(continuation.strip())
            depth += continuation.count("(") - continuation.count(")")
        signature = re.sub(r"\\s+", " ", " ".join(parts)).strip()
        methods.append({"name": name, "line": index + 1, "signature": signature})
        index += 1
    return methods


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--upstream-root", required=True, type=Path)
    parser.add_argument("--host-file", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args()

    upstream_app = args.upstream_root / "app/src/main/java/io/legado/app/help"
    upstream = collect_methods(upstream_app / "JsExtensions.kt", "interface JsExtensions")
    inherited = collect_methods(upstream_app / "JsEncodeUtils.kt", "interface JsEncodeUtils")
    host = collect_methods(args.host_file, "class RhinoSafeSourceJavaExtensions")
    upstream_all = inherited + upstream

    def by_name(items: list[dict[str, object]]) -> dict[str, list[str]]:
        result: dict[str, list[str]] = {}
        for item in items:
            result.setdefault(str(item["name"]), []).append(str(item["signature"]))
        return result

    upstream_map = by_name(upstream_all)
    host_map = by_name(host)
    missing = sorted(set(upstream_map) - set(host_map))
    extra = sorted(set(host_map) - set(upstream_map))
    overload_diffs = {
        name: {"upstream": len(upstream_map[name]), "host": len(host_map[name])}
        for name in sorted(set(upstream_map) & set(host_map))
        if len(upstream_map[name]) != len(host_map[name])
    }
    signature_diffs = {
        name: {"upstream": upstream_map[name], "host": host_map[name]}
        for name in sorted(set(upstream_map) & set(host_map))
        if sorted(upstream_map[name]) != sorted(host_map[name])
    }

    report = {
        "scope": "source-level declaration inventory; not a complete JVM descriptor proof",
        "upstream": {
            "JsExtensions": str(upstream_app / "JsExtensions.kt"),
            "JsEncodeUtils": str(upstream_app / "JsEncodeUtils.kt"),
            "method_count": len(upstream_all),
            "name_count": len(upstream_map),
            "methods": upstream_all,
        },
        "host": {
            "file": str(args.host_file),
            "method_count": len(host),
            "name_count": len(host_map),
            "methods": host,
        },
        "missing_names": missing,
        "extra_names": extra,
        "overload_count_differences": overload_diffs,
        "signature_differences": signature_diffs,
    }

    args.output_dir.mkdir(parents=True, exist_ok=True)
    (args.output_dir / "js-extensions-abi.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\\n", encoding="utf-8"
    )
    markdown = [
        "# Legado JsExtensions source ABI inventory",
        "",
        "> This is a source-level inventory only. It does not prove JVM descriptor, Rhino reflection, overload resolution, or runtime semantic equivalence.",
        "",
        f"- Upstream declarations: {len(upstream_all)} across {len(upstream_map)} names",
        f"- Host declarations: {len(host)} across {len(host_map)} names",
        f"- Missing upstream names: {len(missing)}",
        f"- Overload-count differences: {len(overload_diffs)}",
        f"- Signature differences: {len(signature_diffs)}",
        "",
        "## Missing upstream method names",
        "",
    ]
    markdown.extend([f"- `{name}`" for name in missing] or ["- None"])
    markdown += ["", "## Overload-count differences", ""]
    markdown.extend(
        [f"- `{name}`: upstream {values['upstream']}, host {values['host']}"
         for name, values in overload_diffs.items()] or ["- None"]
    )
    markdown += ["", "## Full upstream declarations", ""]
    markdown.extend([f"- `{item['signature']}`" for item in upstream_all])
    (args.output_dir / "js-extensions-abi.md").write_text(
        "\\n".join(markdown) + "\\n", encoding="utf-8"
    )
    print(
        f"JsExtensions inventory: upstream={len(upstream_all)} methods/"
        f"{len(upstream_map)} names, host={len(host)} methods/{len(host_map)} names, "
        f"missing={len(missing)}, overloadDiffs={len(overload_diffs)}"
    )


if __name__ == "__main__":
    main()
