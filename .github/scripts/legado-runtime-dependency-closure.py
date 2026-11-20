#!/usr/bin/env python3
"""Inventory pinned Legado book-source source imports and dependency families.

This is a deterministic source-level import graph, not a Kotlin compiler graph.
It deliberately reports ambiguity and blind spots rather than guessing that a
missing import is or is not a build failure.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import tempfile
from collections import Counter, deque
from pathlib import Path
from typing import Iterable

PINNED = "LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a"
SOURCE_ROOTS = (
    "app/src/main/java",
    "modules/book/src/main/java",
    "modules/rhino/src/main/java",
)
SEEDS = (
    "io/legado/app/data/entities/BookSource.kt",
    "io/legado/app/data/entities/Book.kt",
    "io/legado/app/data/entities/BookChapter.kt",
    "io/legado/app/data/entities/BaseSource.kt",
    "io/legado/app/data/entities/BaseBook.kt",
    "io/legado/app/help/JsExtensions.kt",
    "io/legado/app/help/JsEncodeUtils.kt",
    "io/legado/app/model/analyzeRule/AnalyzeRule.kt",
    "io/legado/app/model/analyzeRule/AnalyzeUrl.kt",
    "io/legado/app/model/jsSource/JsSourceEngine.kt",
    "io/legado/app/model/SharedJsScope.kt",
    "io/legado/app/help/http/CookieStore.kt",
    "io/legado/app/help/http/BackstageWebView.kt",
    "io/legado/app/help/source/SourceVerificationHelp.kt",
    "io/legado/app/help/webView/WebJsExtensions.kt",
    "io/legado/app/ui/login/SourceLoginJsExtensions.kt",
)
INTERNAL_PREFIXES = (
    "io.legado.app",
    "io.legado",
    "com.script",
    "org.htmlunit.corejs.javascript",
)
IMPORT_RE = re.compile(r"^\s*import\s+([A-Za-z_][A-Za-z0-9_.*]*)(?:\s+as\s+([A-Za-z_][A-Za-z0-9_]*))?\s*$")
JAVA_IMPORT_RE = re.compile(r"^\s*import\s+(static\s+)?([A-Za-z_][A-Za-z0-9_.*]*)\s*;\s*$")
PACKAGE_RE = re.compile(r"^\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)", re.MULTILINE)
DECL_MODIFIERS = r"(?:(?:public|internal|private|protected|open|abstract|sealed|data|enum|annotation|expect|actual|inline|value|final|override|suspend|operator|infix|external|tailrec|const|lateinit|noinline|crossinline)\s+)*"
DECL_ANNOTATIONS = r"(?:(?:@[A-Za-z_][A-Za-z0-9_.]*(?:\([^\n]*?\))?)\s+)*"
TYPE_DECL_RE = re.compile(
    r"^\s*" + DECL_ANNOTATIONS + DECL_MODIFIERS
    + r"(?:class|interface|object|typealias|enum\s+class|annotation\s+class|fun\s+interface)\s+"
    + r"([A-Za-z_][A-Za-z0-9_]*)",
    re.MULTILINE,
)
FUNCTION_DECL_RE = re.compile(
    r"^\s*" + DECL_ANNOTATIONS + DECL_MODIFIERS
    + r"fun\s+(?:<[^>\n]+>\s*)?"
    + r"(?:(?:[A-Za-z_][A-Za-z0-9_?.<>!, ]*)\.)?"
    + r"([A-Za-z_][A-Za-z0-9_]*)\s*(?:<[^>\n]+>\s*)?\(",
    re.MULTILINE,
)
PROPERTY_DECL_RE = re.compile(
    r"^\s*" + DECL_ANNOTATIONS + DECL_MODIFIERS
    + r"(?:val|var)\s+(?:(?:[A-Za-z_][A-Za-z0-9_?.<>!, ]*)\.)?"
    + r"([A-Za-z_][A-Za-z0-9_]*)\b",
    re.MULTILINE,
)
GENERATED_ANDROID_SYMBOLS = (
    ("io.legado.app.R", "Android resource symbol generated from app resources"),
    ("io.legado.app.BuildConfig", "Android Gradle BuildConfig generation"),
    ("io.legado.app.databinding", "Android view/data-binding generated classes"),
)

DEPENDENCY_PROVIDED_SYMBOLS = (
    (
        "org.htmlunit.corejs.javascript",
        "org.htmlunit:htmlunit-core-js:5.3.0-legado.4; modules/rhino/build.gradle exposes libs.htmlunit.core.js and gradle/libs.versions.toml pins the version",
    ),
)

BLIND_SPOTS = [
    "Same-package references without import statements are not added as graph edges.",
    "Fully-qualified type/function references in declarations or string-evaluated scripts may be missed.",
    "Reflection, generated Kotlin/Java code, KSP/KAPT output, service-loader metadata and JNI are not resolved.",
    "Gradle variants, source-set exclusions, manifest/resource linking and dependency substitution are summarized only, not compiled.",
    "Wildcard imports conservatively include all source files in the exact imported package; nested subpackages are not implied.",
    "Import resolution is source-level and may not model Kotlin companion/object members, typealiases or Java static-import semantics perfectly.",
    "Dependency-family labels are heuristic path/import/text indicators, not proof that a runtime service is required.",
]


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8", errors="replace")


def _package(path: Path, text: str) -> str:
    match = PACKAGE_RE.search(text)
    return match.group(1) if match else ""


def _decls(text: str) -> set[str]:
    # Kotlin extension functions/properties and generic/reified declarations
    # are imported from their package just like ordinary top-level symbols.
    names: set[str] = set()
    for pattern in (TYPE_DECL_RE, FUNCTION_DECL_RE, PROPERTY_DECL_RE):
        names.update(pattern.findall(text))

    # Balance nested type parameters, e.g. <T, K : Comparable<K>>, then
    # resolve the final receiver/member segment before the parameter list.
    for line in text.splitlines():
        fun_match = re.search(r"\bfun\b", line)
        if not fun_match:
            continue
        tail = line[fun_match.end():].strip()
        if tail.startswith("<"):
            depth = 0
            generic_end = -1
            for index, char in enumerate(tail):
                if char == "<":
                    depth += 1
                elif char == ">":
                    depth -= 1
                    if depth == 0:
                        generic_end = index
                        break
            if generic_end >= 0:
                tail = tail[generic_end + 1:].strip()
        signature = tail.split("(", 1)[0].strip()
        if signature:
            member = signature.rsplit(".", 1)[-1].strip()
            match = re.match(r"\`?([A-Za-z_][A-Za-z0-9_]*)\`?(?:<|$)", member)
            if match:
                names.add(match.group(1))
    return names



def _declaration_records(text: str) -> list[dict]:
    records = []
    patterns = (
        ("type", re.compile(r"^\s*(?:(?:public|internal|private|protected|open|abstract|sealed|data|enum|annotation|expect|actual|value|final)\s+)*(?:class|interface|object|typealias|enum\s+class|annotation\s+class|fun\s+interface)\s+([A-Za-z_][A-Za-z0-9_]*)")),
        ("function", re.compile(r"^\s*(?:(?:public|internal|private|protected|open|inline|suspend|operator|infix|override|external|tailrec)\s+)*fun\s+(?:<[^>]*>\s*)?(?:(?:[A-Za-z_][A-Za-z0-9_?.<>!, ]*)\.)?([A-Za-z_][A-Za-z0-9_]*)\s*(?:<[^>]*>\s*)?\(")),
        ("property", re.compile(r"^\s*(?:(?:public|internal|private|protected|const|lateinit|override)\s+)*(?:val|var)\s+(?:[A-Za-z_][A-Za-z0-9_?.<>!, ]*\.)?([A-Za-z_][A-Za-z0-9_]*)\b")),
        ("java_type", re.compile(r"^\s*(?:(?:public|protected|private|abstract|final|static)\s+)*(?:class|interface|enum|record|@interface)\s+([A-Za-z_][A-Za-z0-9_]*)")),
    )
    for line_no, line in enumerate(text.splitlines(), 1):
        if line.lstrip().startswith(("//", "*", "/*", "import ", "package ")):
            continue
        for kind, pattern in patterns:
            match = pattern.search(line)
            if match:
                records.append({"name": match.group(1), "kind": kind, "line": line_no})
                break
    return records


def _mask_non_code(text: str) -> str:
    text = re.sub(r"/\*.*?\*/", lambda m: re.sub(r"[^\n]", " ", m.group(0)), text, flags=re.S)
    text = re.sub(r"//[^\n]*", lambda m: " " * len(m.group(0)), text)
    text = re.sub(r'"""[\s\S]*?"""|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',
                  lambda m: re.sub(r"[^\n]", " ", m.group(0)), text)
    return text



def _same_package_candidates(index: "SourceIndex", key: str, text: str) -> list[dict]:
    package = _package(index.files[key], text)
    masked = _mask_non_code(text)
    import_lines = {item["line"] for item in _parse_imports(text)}
    found = {}
    for line_no, line in enumerate(masked.splitlines(), 1):
        if line_no in import_lines or line.lstrip().startswith(("package ", "import ")):
            continue
        for match in re.finditer(r"\b[A-Za-z_][A-Za-z0-9_]*\b", line):
            symbol = match.group(0)
            candidates = index.symbols_by_package.get(package, {}).get(symbol, [])
            targets = sorted({record[0] for record in candidates if record[0] != key})
            if targets:
                found[(line_no, symbol, tuple(targets))] = {
                    "line": line_no, "symbol": symbol, "targets": targets, "ambiguous": len(targets) > 1
                }
    return [found[item] for item in sorted(found)]


def _classify_unresolved(name: str) -> tuple[str, str | None]:
    for prefix, evidence in GENERATED_ANDROID_SYMBOLS:
        if name == prefix or name.startswith(prefix + "."):
            return "generated_android_symbol", evidence
    for prefix, evidence in DEPENDENCY_PROVIDED_SYMBOLS:
        if name == prefix or name.startswith(prefix + "."):
            return "dependency_provided_symbol", evidence
    return "internal_namespace_unresolved", None


def _is_internal(name: str, prefixes: Iterable[str]) -> bool:
    return any(name == prefix or name.startswith(prefix + ".") for prefix in prefixes)


def _family_flags(path: str, imports: list[str], content: str) -> list[str]:
    probe = (path + "\n" + "\n".join(imports) + "\n" + content[:12000]).lower()
    checks = {
        "android": ("android.", "androidx."),
        "room": ("androidx.room", "@entity", "@dao", "@typeconverter"),
        "parcelable": ("parcelize", "parcelable", "parcel."),
        "webview": ("webview", "backstagewebview", "webcachemanager"),
        "activity_ui": ("activity", "fragment", "dialog", "viewbinding", "compose", "ui."),
        "database": ("appdb", "dao", "database", "roomdatabase", "sqlite"),
        "cache": ("cachemanager", "webcache", "contentcache", "cache."),
        "network": ("okhttp", "retrofit", "connection.", "networkutils", "cookie", "http."),
        "rhino_js": ("rhino", "javascript", "jsextensions", "sharedjsscope", "scriptengine"),
    }
    return [label for label, needles in checks.items() if any(needle in probe for needle in needles)]


class SourceIndex:
    def __init__(self, roots: list[Path], root_labels: list[str] | None = None):
        self.roots = [root.resolve() for root in roots]
        labels = root_labels or [root.name for root in roots]
        if len(labels) != len(roots):
            raise ValueError("root_labels must have one label per source root")
        self.files: dict[str, Path] = {}
        self.texts: dict[str, str] = {}
        self.packages: dict[str, list[str]] = {}
        self.by_relative: dict[str, list[str]] = {}
        self.declarations: dict[str, set[str]] = {}
        self.declaration_records: dict[str, list[dict]] = {}
        self.symbols_by_package: dict[str, dict[str, list[tuple[str, int, str]]]] = {}
        for root, root_label in zip(self.roots, labels):
            if not root.exists():
                continue
            for path in sorted(root.rglob("*")):
                if not path.is_file() or path.suffix not in (".kt", ".java"):
                    continue
                rel = path.relative_to(root).as_posix()
                # Keep root identity in keys so equal relative paths cannot collide.
                key = f"{root_label}:{rel}"
                text = _read_text(path)
                package = _package(path, text)
                self.files[key] = path
                self.texts[key] = text
                self.packages.setdefault(package, []).append(key)
                self.by_relative.setdefault(rel, []).append(key)
                self.declarations[key] = _decls(text)
                records = _declaration_records(text)
                self.declaration_records[key] = records
                symbols = self.symbols_by_package.setdefault(package, {})
                for record in records:
                    symbols.setdefault(record["name"], []).append((key, record["line"], record["kind"]))
        for values in self.packages.values():
            values.sort()
        for symbols in self.symbols_by_package.values():
            for values in symbols.values():
                values.sort()

    def resolve(self, imported: str) -> tuple[list[str], str]:
        wildcard = imported.endswith(".*")
        target = imported[:-2] if wildcard else imported
        if wildcard:
            matches = self.packages.get(target, [])
            if matches:
                return list(matches), "wildcard"
            # Java static wildcard imports target a class, not a package.
            for suffix in (".kt", ".java"):
                relative = "/".join(target.split(".")) + suffix
                owner_files = self.by_relative.get(relative, [])
                if owner_files:
                    return sorted(owner_files), "static_wildcard"
            return [], "unresolved"

        # Exact file/class candidates first. Progressive truncation handles
        # nested types such as Book.ReadConfig -> Book.kt.
        segments = target.split(".")
        for cut in range(len(segments), 0, -1):
            stem = "/".join(segments[:cut])
            for suffix in (".kt", ".java"):
                relative = stem + suffix
                matches = self.by_relative.get(relative, [])
                if matches:
                    return sorted(matches), "file"

        # Top-level Kotlin/Java members are imported from their package.
        if len(segments) > 1:
            package = ".".join(segments[:-1])
            symbol = segments[-1]
            candidates = self.packages.get(package, [])
            declared = [key for key in candidates if symbol in self.declarations[key]]
            if declared:
                return sorted(declared), "top_level_symbol"
        return [], "unresolved"


def _parse_imports(text: str) -> list[dict]:
    imports = []
    for line_no, line in enumerate(text.splitlines(), 1):
        match = IMPORT_RE.match(line)
        if match:
            imports.append({
                "line": line_no,
                "name": match.group(1),
                "alias": match.group(2),
                "language": "kotlin",
            })
            continue
        java_match = JAVA_IMPORT_RE.match(line)
        if java_match:
            imports.append({
                "line": line_no,
                "name": java_match.group(2),
                "alias": None,
                "language": "java",
                "static": bool(java_match.group(1)),
            })
    return imports


def build_report(root: Path) -> dict:
    roots = [root / rel for rel in SOURCE_ROOTS]
    index = SourceIndex(roots, list(SOURCE_ROOTS))
    seed_keys: list[str] = []
    missing_seeds: list[str] = []
    for seed in SEEDS:
        matches = [key for key in index.files if key.endswith(":" + seed)]
        if len(matches) == 1:
            seed_keys.append(matches[0])
        elif not matches:
            missing_seeds.append(seed)
        else:
            missing_seeds.append(seed + " (ambiguous: " + ", ".join(matches) + ")")

    visited: set[str] = set()
    queue = deque(seed_keys)
    parent: dict[str, str | None] = {key: None for key in seed_keys}
    edges: list[dict] = []
    unresolved: list[dict] = []
    external: list[dict] = []
    ambiguous: list[dict] = []
    edge_keys: set[tuple] = set()
    import_counts: Counter[str] = Counter()
    resolution_counts: Counter[str] = Counter()

    while queue:
        key = queue.popleft()
        if key in visited:
            continue
        visited.add(key)
        text = index.texts[key]
        imports = _parse_imports(text)
        path = index.files[key]
        for item in imports:
            name = item["name"]
            import_counts[name.split(".")[0]] += 1
            if _is_internal(name.removesuffix(".*"), INTERNAL_PREFIXES):
                matches, kind = index.resolve(name)
                resolution_counts[kind] += 1
                if not matches:
                    classification, evidence = _classify_unresolved(name)
                    resolution_counts[classification] += 1
                    unresolved.append({
                        "from": key,
                        "line": item["line"],
                        "import": name,
                        "alias": item["alias"],
                        "reason": classification,
                        "classification": classification,
                        "evidence": evidence,
                    })
                    continue
                for target in matches:
                    edge_key = (key, target, item["line"], name, "import")
                    if edge_key not in edge_keys:
                        edges.append({"from": key, "to": target, "line": item["line"], "symbol": name,
                                      "alias": item["alias"], "edgeKind": "import", "resolution": kind,
                                      "confidence": "source_import"})
                        edge_keys.add(edge_key)
                    if target not in parent:
                        parent[target] = key
                        queue.append(target)
            else:
                root_package = ".".join(name.split(".")[:2]) if "." in name else name
                external.append({
                    "from": key,
                    "line": item["line"],
                    "import": name,
                    "alias": item["alias"],
                    "packageGroup": root_package,
                })

        # Same-package references are lexical candidates, not compiler decisions.
        for candidate in _same_package_candidates(index, key, text):
            line_no, symbol, targets = candidate["line"], candidate["symbol"], candidate["targets"]
            if candidate["ambiguous"]:
                ambiguous.append({"from": key, "line": line_no, "symbol": symbol,
                                  "candidateTargets": targets, "edgeKind": "same_package_lexical",
                                  "classification": "ambiguous_same_package_symbol"})
            for target in targets:
                edge_key = (key, target, line_no, symbol, "same_package_lexical")
                if edge_key in edge_keys:
                    continue
                edges.append({"from": key, "to": target, "line": line_no, "symbol": symbol,
                              "alias": None, "edgeKind": "same_package_lexical",
                              "resolution": "declaration_candidate", "confidence": "lexical_candidate"})
                edge_keys.add(edge_key)
                if target not in parent:
                    parent[target] = key
                    queue.append(target)

    def shortest_path(key: str) -> list[str]:
        result = []
        current: str | None = key
        while current is not None:
            result.append(current)
            current = parent.get(current)
        return list(reversed(result))

    nodes = []
    for key in sorted(visited):
        text = index.texts[key]
        imports = [item["name"] for item in _parse_imports(text)]
        relative = key.split(":", 1)[-1]
        nodes.append({
            "id": key,
            "relativePath": relative,
            "package": _package(index.files[key], text),
            "declarations": sorted(index.declarations[key]),
            "declarationRecords": index.declaration_records[key],
            "contentSha256": hashlib.sha256(text.encode("utf-8")).hexdigest(),
            "importCount": len(imports),
            "dependencyFamilies": _family_flags(relative, imports, text),
            "shortestSeedPath": shortest_path(key),
        })

    # Build metadata helps locate the module/build ownership boundary without
    # pretending this scanner evaluates Gradle source-set resolution.
    build_files = []
    for candidate in (root / "settings.gradle", root / "settings.gradle.kts",
                      root / "build.gradle", root / "build.gradle.kts",
                      root / "gradle/libs.versions.toml"):
        if candidate.exists():
            build_files.append(candidate.relative_to(root).as_posix())
    for candidate in sorted(root.glob("**/build.gradle*")):
        if "upstream/legado/" in candidate.as_posix():
            build_files.append(candidate.relative_to(root).as_posix())

    external_groups = Counter(item["packageGroup"] for item in external)
    unresolved.sort(key=lambda item: (item["from"], item["line"], item["import"]))
    edges.sort(key=lambda item: (item["from"], item["to"], item.get("line", 0), item.get("symbol", "")))
    external.sort(key=lambda item: (item["from"], item["line"], item["import"]))

    return {
        "pinnedUpstream": PINNED,
        "inventoryKind": "recursive Kotlin/Java source import graph; not compiler-complete dependency closure",
        "sourceRoots": SOURCE_ROOTS,
        "seeds": {
            "declared": list(SEEDS),
            "resolved": sorted(seed_keys),
            "missingOrAmbiguous": missing_seeds,
        },
        "summary": {
            "indexedSourceFiles": len(index.files),
            "samePackageLexicalEdges": sum(1 for edge in edges if edge.get("edgeKind") == "same_package_lexical"),
            "importEdges": sum(1 for edge in edges if edge.get("edgeKind") == "import"),
            "ambiguousSamePackageReferences": len(ambiguous),
            "seedCount": len(SEEDS),
            "resolvedSeedCount": len(seed_keys),
            "reachableSourceFileCount": len(visited),
            "internalImportEdges": sum(1 for edge in edges if edge["edgeKind"] == "import"),
            "totalResolvedSourceEdges": len(edges),
            "unresolvedImportCount": len(unresolved),
            "unresolvedInternalImportCount": sum(1 for item in unresolved if item["classification"] == "internal_namespace_unresolved"),
            "generatedAndroidSymbolImportCount": sum(1 for item in unresolved if item["classification"] == "generated_android_symbol"),
            "dependencyProvidedImportCount": sum(1 for item in unresolved if item["classification"] == "dependency_provided_symbol"),
            "unresolvedByClassification": dict(sorted(Counter(item["classification"] for item in unresolved).items())),
            "externalImportCount": len(external),
            "resolutionKinds": dict(sorted(resolution_counts.items())),
            "externalPackageGroups": dict(sorted(external_groups.items())),
            "dependencyFamilyFileCounts": dict(sorted(Counter(
                family for node in nodes for family in node["dependencyFamilies"]
            ).items())),
        },
        "nodes": nodes,
        "edges": edges,
        "ambiguousReferences": sorted(ambiguous, key=lambda item: (item["from"], item["line"], item["symbol"])),
        "unresolvedInternalImports": unresolved,
        "externalImports": external,
        "buildOwnership": {
            "buildFilesFound": sorted(set(build_files)),
            "note": "Build files are listed for manual module/source-set review; Gradle configuration is not evaluated.",
        },
        "limitations": BLIND_SPOTS,
    }


def self_test() -> None:
    with tempfile.TemporaryDirectory(prefix="legado-closure-") as tmp:
        base = Path(tmp)
        roots = [base / "app" / "src" / "main" / "java",
                 base / "modules" / "book" / "src" / "main" / "java",
                 base / "modules" / "rhino" / "src" / "main" / "java"]
        a = roots[0] / "demo" / "a" / "A.kt"
        b = roots[1] / "demo" / "b" / "B.kt"
        c = roots[2] / "demo" / "b" / "Helpers.kt"
        d = roots[1] / "demo" / "b" / "D.kt"
        java = roots[2] / "demo" / "b" / "JavaHelpers.java"
        fixtures = (
            (a, "package demo.a\nimport demo.b.B as Bee\nimport demo.b.*\nimport demo.b.helper\nimport demo.missing.Nope\nimport static demo.b.JavaHelpers.VALUE;\nclass A"),
            (b, "package demo.b\nimport demo.a.A\nclass B"),
            (c, "package demo.b\nfun helper() = Unit"),
            (d, "package demo.b\nclass D"),
            (java, "package demo.b;\npublic class JavaHelpers { public static final int VALUE = 1; }"),
            (roots[0] / "demo" / "b" / "Extensions.kt", "package demo.b\n@JvmName(\"helpers\")\ninternal inline fun <reified T : Any> T?.asHelper(): T? = this\nfun String.helperProperty(): Int = length\n"),
            (roots[1] / "demo" / "b" / "NestedGeneric.kt", "package demo.b\nfun <T, K : Comparable<K>> List<T>.deepSearch(key: K): Int = 0\nfun HashMap<String, *>.hasEntry(key: String): Boolean = true"),
            (roots[0] / "demo" / "b" / "Parent.kt", "package demo.b\nopen class Parent"),
            (roots[0] / "demo" / "b" / "Marker.kt", "package demo.b\nannotation class Marker"),
            (roots[1] / "demo" / "b" / "Child.kt", "package demo.b\n@Marker class Child : Parent() { val item: D? = null; val duplicate: Duplicate? = null; fun invoke() = helper() }\ntypealias ParentAlias = Parent"),
            (roots[2] / "demo" / "b" / "DuplicateOne.kt", "package demo.b\ninterface Duplicate"),
            (roots[2] / "demo" / "b" / "DuplicateTwo.kt", "package demo.b\ninterface Duplicate"),
        )
        for path, body in fixtures:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body, encoding="utf-8")

        labels = list(SOURCE_ROOTS)
        idx = SourceIndex(roots, labels)
        resolved, kind = idx.resolve("demo.b.B")
        assert kind == "file" and any(item.endswith(":demo/b/B.kt") for item in resolved)
        resolved, kind = idx.resolve("demo.b.helper")
        assert kind == "top_level_symbol" and any(item.endswith(":demo/b/Helpers.kt") for item in resolved)
        resolved, kind = idx.resolve("demo.b.asHelper")
        assert kind == "top_level_symbol" and any(item.endswith(":demo/b/Extensions.kt") for item in resolved)
        resolved, kind = idx.resolve("demo.b.helperProperty")
        assert kind == "top_level_symbol" and any(item.endswith(":demo/b/Extensions.kt") for item in resolved)
        resolved, kind = idx.resolve("demo.b.deepSearch")
        assert kind == "top_level_symbol" and any(item.endswith(":demo/b/NestedGeneric.kt") for item in resolved)
        resolved, kind = idx.resolve("demo.b.hasEntry")
        assert kind == "top_level_symbol" and any(item.endswith(":demo/b/NestedGeneric.kt") for item in resolved)
        assert _classify_unresolved("io.legado.app.databinding.ActivityMainBinding")[0] == "generated_android_symbol"
        assert _classify_unresolved("org.htmlunit.corejs.javascript.Context")[0] == "dependency_provided_symbol"
        assert _classify_unresolved("io.legado.app.utils.notFound")[0] == "internal_namespace_unresolved"
        resolved, kind = idx.resolve("demo.b.*")
        assert kind == "wildcard" and len(resolved) == 6
        resolved, kind = idx.resolve("demo.b.JavaHelpers.VALUE")
        assert kind == "file" and any(item.endswith(":demo/b/JavaHelpers.java") for item in resolved)
        resolved, kind = idx.resolve("demo.b.JavaHelpers.*")
        assert kind == "static_wildcard" and any(item.endswith(":demo/b/JavaHelpers.java") for item in resolved)
        assert idx.resolve("demo.missing.Nope")[1] == "unresolved"

        parsed = _parse_imports(_read_text(a))
        assert any(item["name"] == "demo.b.B" and item["alias"] == "Bee" for item in parsed)
        assert any(item["name"] == "demo.b.JavaHelpers.VALUE" and item.get("static") for item in parsed)
        a_key = next(key for key in idx.files if key.endswith(":demo/a/A.kt"))
        b_key = next(key for key in idx.files if key.endswith(":demo/b/B.kt"))
        a_targets = set()
        for item in _parse_imports(idx.texts[a_key]):
            a_targets.update(idx.resolve(item["name"])[0])
        b_targets = set()
        for item in _parse_imports(idx.texts[b_key]):
            b_targets.update(idx.resolve(item["name"])[0])
        assert b_key in a_targets and a_key in b_targets, "cross-root import cycle must resolve"
        assert len(idx.files) == 12 and len(set(idx.files)) == 12, "source-root identities must not collide"
        child_key = next(key for key in idx.files if key.endswith(":demo/b/Child.kt"))
        child_edges = _same_package_candidates(idx, child_key, idx.texts[child_key])
        child_symbols = {edge["symbol"] for edge in child_edges}
        assert {"Marker", "Parent", "D", "helper", "Duplicate"}.issubset(child_symbols)
        assert any(record["name"] == "ParentAlias" and record["kind"] == "type" for record in idx.declaration_records[child_key])
        duplicate_edge = next(edge for edge in child_edges if edge["symbol"] == "Duplicate")
        assert duplicate_edge["ambiguous"] and len(duplicate_edge["targets"]) == 2

        report_a = build_report(base)
        report_b = build_report(base)
        assert len(report_a["seeds"]["missingOrAmbiguous"]) == len(SEEDS)
        assert json.dumps(report_a, sort_keys=True) == json.dumps(report_b, sort_keys=True)
        assert "Same-package references" in " ".join(report_a["limitations"])
    print("dependency closure scanner self-test: PASS")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--upstream-root", type=Path, help="pinned Legado checkout root")
    parser.add_argument("--output", type=Path)
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        self_test()
    if args.upstream_root is None and args.output is None:
        return
    if args.upstream_root is None or args.output is None:
        parser.error("--upstream-root and --output must be provided together")
    report = build_report(args.upstream_root)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"pinnedUpstream": PINNED, "summary": report["summary"],
                      "missingSeeds": report["seeds"]["missingOrAmbiguous"]},
                     ensure_ascii=False, indent=2))
    if report["seeds"]["missingOrAmbiguous"]:
        raise SystemExit("one or more declared dependency-closure seeds did not resolve")


if __name__ == "__main__":
    main()
