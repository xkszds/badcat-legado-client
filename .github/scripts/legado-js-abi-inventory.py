#!/usr/bin/env python3
"""Create a source-level inventory of pinned Legado JS extension APIs.

This intentionally records Kotlin source signatures, not claimed JVM bytecode
descriptors. It is a repeatable gap report to drive the subsequent real-runtime
integration; it never treats a missing method as an acceptable no-op.
"""
from __future__ import annotations

import argparse
import json
import re
from collections import Counter, defaultdict
from pathlib import Path


UPSTREAM_SCOPES = {
    "JsExtensions": "io/legado/app/help/JsExtensions.kt",
    "RssJsExtensions": "io/legado/app/ui/rss/read/RssJsExtensions.kt",
    "JsEncodeUtils": "io/legado/app/help/JsEncodeUtils.kt",
    "RegexJsExtensions": "io/legado/app/help/RegexJsExtensions.kt",
    "WebJsExtensions": "io/legado/app/help/webView/WebJsExtensions.kt",
    "SourceLoginJsExtensions": "io/legado/app/ui/login/SourceLoginJsExtensions.kt",
}
HOST_SCOPE = "RhinoSafeSourceJavaExtensions"
HOST_FILE = "app/badcat/client/runtime/RhinoSafeSourceJavaExtensions.kt"

# Concrete objects actually bound to Badcat Rhino scopes, plus objects returned
# by the main bridge and the @js: replacement evaluator. Keep this explicit:
# a class being present in the APK does not make it a bound JS API.
HOST_OBJECTS = {
    "scope.java": ("RhinoSafeSourceJavaExtensions", HOST_FILE),
    "scope.cookie": ("RhinoSourceCookieExtensions", "app/badcat/client/runtime/RhinoSourceCookieExtensions.kt"),
    "scope.source": ("RhinoSourceLoginExtensions", "app/badcat/client/runtime/SourceLoginSession.kt"),
    "scope.cache": ("RhinoSourceCacheExtensions", "app/badcat/client/runtime/RhinoSourceCacheExtensions.kt"),
    "replacement.java": ("LegadoRegexJsExtensions", "app/badcat/client/runtime/RhinoLegadoJsReplacementEvaluator.kt"),
    "return.file": ("RhinoSourceFileHandle", "app/badcat/client/runtime/RhinoSourceFileHandle.kt"),
    "return.httpResponse": ("RhinoSourceHttpResponse", HOST_FILE),
    "return.rawResponse": ("RhinoSourceRawResponse", HOST_FILE),
    "return.rawRequest": ("RhinoSourceRawRequest", HOST_FILE),
    "return.url": ("RhinoLegadoUrl", HOST_FILE),
    "return.element": ("LegadoSourceElement", "integration-contract/src/main/kotlin/app/badcat/engine/LegadoSourceElement.kt"),
    "return.elementList": ("LegadoSourceElementList", "integration-contract/src/main/kotlin/app/badcat/engine/LegadoSourceElement.kt"),
}

UPSTREAM_SCOPE_INHERITANCE = {
    "JsExtensions": ["JsEncodeUtils"],
    "RssJsExtensions": ["JsExtensions"],
    "WebJsExtensions": ["RssJsExtensions"],
    "SourceLoginJsExtensions": ["RssJsExtensions"],
    "RegexJsExtensions": ["JsEncodeUtils"],
}

SOURCE_RUNTIME_BINDINGS = {
    "RhinoSourceScriptRuntime": {
        "java": "RhinoSafeSourceJavaExtensions",
        "cookie": "RhinoSourceCookieExtensions",
        "source": "RhinoSourceLoginExtensions",
        "cache": "RhinoSourceCacheExtensions",
    },
    "RhinoLegadoJsReplacementEvaluator": {
        "java": "LegadoRegexJsExtensions",
        "additionalBindings": ["result", "book", "chapter"],
    },
}

RETURNED_EXTERNAL_TYPES = [
    "org.jsoup.nodes.Document",
    "org.jsoup.nodes.Element (inherited through Document)",
    "org.jsoup.select.Elements",
    "cn.hutool.crypto.symmetric.SymmetricCrypto",
    "io.legado.app.help.crypto.AsymmetricCrypto",
    "io.legado.app.help.crypto.Sign",
]


def find_scope(lines: list[str], symbol: str) -> tuple[int, int]:
    declaration = re.compile(
        rf"^(?:(?:public|internal|private)\s+)?"
        rf"(?:interface|class|object)\s+{re.escape(symbol)}\b"
    )
    start = next((i for i, line in enumerate(lines) if declaration.match(line)), None)
    if start is None:
        raise RuntimeError(f"Cannot find Kotlin declaration {symbol}")
    end = len(lines)
    top_level = re.compile(
        r"^(?:(?:public|internal|private)\s+)?"
        r"(?:interface|class|object|enum\s+class|data\s+class|sealed\s+class)\s+"
    )
    for i in range(start + 1, len(lines)):
        if top_level.match(lines[i]):
            end = i
            break
    return start, end


def _strip_line_comment(line: str) -> str:
    # Sufficient for declarations in this inventory: preserve URLs/strings by
    # only stripping comments outside a quoted string.
    quote = None
    escaped = False
    for i, char in enumerate(line):
        if quote:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quote:
                quote = None
        elif char == '"' or char == "'":
            quote = char
        elif line[i:i + 2] == "//":
            return line[:i]
    return line


def capture_signature(lines: list[str], start: int, limit: int) -> str:
    parts: list[str] = []
    depth = 0
    opened = False
    i = start
    while i < limit:
        raw = _strip_line_comment(lines[i])
        if i > start and depth == 0:
            stripped = raw.strip()
            if not stripped:
                i += 1
                continue
            if stripped.startswith(("{", "=", "}")):
                break
            if re.match(r"^(?:@|(?:override\s+)?fun\s+|private\s+|protected\s+|internal\s+|val\s+|var\s+)", stripped):
                break
        quote = None
        escaped = False
        close_at = None
        for pos, char in enumerate(raw):
            if quote:
                if escaped:
                    escaped = False
                elif char == "\\":
                    escaped = True
                elif char == quote:
                    quote = None
                continue
            if char == '"' or char == "'":
                quote = char
            elif char == "(":
                depth += 1
                opened = True
            elif char == ")":
                depth -= 1
                if opened and depth == 0:
                    close_at = pos
                    break
        if close_at is not None:
            # Keep a declared return type but omit the function body.
            tail = raw[close_at + 1:]
            body_pos = None
            quote = None
            for pos, char in enumerate(tail):
                if quote:
                    if char == quote:
                        quote = None
                elif char == '"' or char == "'":
                    quote = char
                elif char in ("{", "="):
                    body_pos = pos
                    break
            parts.append(raw[:close_at + 1] + (tail[:body_pos] if body_pos is not None else tail))
            if body_pos is not None:
                break
            i += 1
            # Return types may continue on the next line; a body or next
            # declaration is recognized at the top of the next iteration.
            if i >= limit:
                break
            next_line = _strip_line_comment(lines[i]).strip()
            if not next_line or next_line.startswith((":", "where ")):
                continue
            if next_line.startswith(("{", "=", "}")) or re.match(r"^(?:@|(?:override\s+)?fun\s+)", next_line):
                break
            # A continuation return type is allowed.
            parts.append(next_line)
            break
        parts.append(raw)
        i += 1
    signature = re.sub(r"\s+", " ", " ".join(parts)).strip()
    return signature


def collect_scope(root: Path, relative: str, symbol: str) -> list[dict]:
    path = root / relative
    lines = path.read_text(encoding="utf-8").splitlines()
    start, end = find_scope(lines, symbol)
    methods: list[dict] = []
    method_start = re.compile(r"^ {4}(?:(?:override|suspend|inline|operator|infix|tailrec|external)\s+)*fun\s+")
    for i in range(start + 1, end):
        if not method_start.match(lines[i]):
            continue
        signature = capture_signature(lines, i, end)
        name_match = re.search(r"\bfun\s+(?:<[^>]+>\s*)?([A-Za-z_][A-Za-z0-9_]*)\s*\(", signature)
        if not name_match:
            continue
        annotations = "\n".join(lines[max(start, i - 5):i])
        methods.append({
            "scope": symbol,
            "source": relative,
            "line": i + 1,
            "name": name_match.group(1),
            "signature": signature,
            "javascriptInterface": "@JavascriptInterface" in annotations,
        })
    return methods


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--upstream-root", required=True, type=Path)
    parser.add_argument("--host-root", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    upstream: list[dict] = []
    for scope, relative in UPSTREAM_SCOPES.items():
        upstream.extend(collect_scope(args.upstream_root, relative, scope))
    host = collect_scope(args.host_root, HOST_FILE, HOST_SCOPE)
    host_objects = {}
    for binding, (symbol, relative) in HOST_OBJECTS.items():
        host_objects[binding] = {
            "class": symbol,
            "source": relative,
            "methods": collect_scope(args.host_root, relative, symbol),
        }

    # JsExtensions inherits JsEncodeUtils; compare both against the host's
    # primary source Java extension object. Other UI-related scopes are
    # inventoried separately because they need Badcat-owned host callbacks.
    base = [m for m in upstream if m["scope"] in ("JsExtensions", "JsEncodeUtils")]
    upstream_counts = Counter(m["name"] for m in base)
    host_counts = Counter(m["name"] for m in host)
    upstream_by_name: dict[str, list[str]] = defaultdict(list)
    host_by_name: dict[str, list[str]] = defaultdict(list)
    for method in base:
        upstream_by_name[method["name"]].append(method["signature"])
    for method in host:
        host_by_name[method["name"]].append(method["signature"])

    names = sorted(upstream_counts)
    report = {
        "pinnedUpstream": "LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a",
        "comparisonKind": "Kotlin source declaration inventory; not a JVM bytecode descriptor proof",
        "comparison": {
            "upstreamBaseMethodDeclarations": len(base),
            "upstreamBaseMethodNames": len(upstream_counts),
            "hostMethodDeclarations": len(host),
            "hostMethodNames": len(host_counts),
            "missingNames": [name for name in names if name not in host_counts],
            "overloadCountDifferences": [
                {"name": name, "upstream": upstream_counts[name], "host": host_counts[name]}
                for name in names if upstream_counts[name] != host_counts[name]
            ],
            "sameNameSignatureDifferences": [
                {
                    "name": name,
                    "upstream": upstream_by_name[name],
                    "host": host_by_name[name],
                }
                for name in names
                if name in host_by_name and upstream_by_name[name] != host_by_name[name]
            ],
            "hostOnlyNames": sorted(name for name in host_counts if name not in upstream_counts),
        },
        "upstreamScopes": upstream,
        "hostScope": host,
        "upstreamScopeInheritance": UPSTREAM_SCOPE_INHERITANCE,
        "sourceRuntimeBindings": SOURCE_RUNTIME_BINDINGS,
        "hostObjects": host_objects,
        "returnedExternalTypes": RETURNED_EXTERNAL_TYPES,
        "limitations": [
            "Kotlin source signatures are not JVM descriptors; compiler-generated overloads, bridges, annotations and inherited Rhino APIs still require bytecode-level verification.",
            "RssJsExtensions is included as a direct declaration source; WebJsExtensions and SourceLoginJsExtensions inherit its scope family and remain separate callback contexts, not automatically compatible Badcat source-runtime bindings.",
            "Host object entries are source declarations only. The separate compiled/Rhino reflection artifact must be used for emitted JVM descriptors and the actual script-visible member set.",
            "External return types are listed as reachable surfaces but are not compiled from this host source inventory; their dependency bytecode and post-R8 behavior require separate capture.",
            "This report does not prove runtime behavior, parameter coercion, exceptions, coroutine context, return-object identity, or JS engine lifecycle.",
        ],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report["comparison"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
