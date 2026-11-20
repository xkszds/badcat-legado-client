#!/usr/bin/env python3
"""Inventory pinned Legado book-source source imports and dependency families.

This is a deterministic source-level import graph, not a Kotlin compiler graph.
It deliberately reports ambiguity and blind spots rather than guessing that a
missing import is or is not a build failure.
"""
from __future__ import annotations

import argparse
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
PACKAGE_RE = re.compile(r"^\s*package\s+([A-Za-z_][A-Za-z0-9_.]*)", re.MULTILINE)
DECL_RE = re.compile(
    r"^\s*(?:(?:public|internal|private|protected|open|abstract|sealed|data|enum|annotation|expect|actual|inline|value)\s+)*"
    r"(?:class|interface|object|typealias|fun|val|var|enum\s+class|data\s+class)\s+"
    r"([A-Za-z_][A-Za-z0-9_]*)",
    re.MULTILINE,
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
    return set(DECL_RE.findall(text))


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
    def __init__(self, roots: list[Path]):
        self.roots = [root.resolve() for root in roots]
        self.files: dict[str, Path] = {}
        self.texts: dict[str, str] = {}
        self.packages: dict[str, list[str]] = {}
        self.declarations: dict[str, set[str]] = {}
        for root in self.roots:
            if not root.exists():
                continue
            for path in sorted(root.rglob("*")):
                if not path.is_file() or path.suffix not in (".kt", ".java"):
                    continue
                rel = path.relative_to(root).as_posix()
                # Keep root identity in keys so equal relative paths cannot collide.
                key = f"{root.name}:{rel}"
                text = _read_text(path)
                package = _package(path, text)
                self.files[key] = path
                self.texts[key] = text
                self.packages.setdefault(package, []).append(key)
                self.declarations[key] = _decls(text)
        for values in self.packages.values():
            values.sort()

    def resolve(self, imported: str) -> tuple[list[str], str]:
        wildcard = imported.endswith(".*")
        target = imported[:-2] if wildcard else imported
        if wildcard:
            matches = self.packages.get(target, [])
            return list(matches), "wildcard" if matches else "unresolved"

        # Exact file/class candidates first. Progressive truncation handles
        # nested types such as Book.ReadConfig -> Book.kt.
        segments = target.split(".")
        for cut in range(len(segments), 0, -1):
            stem = "/".join(segments[:cut])
            for suffix in (".kt", ".java"):
                relative = stem + suffix
                matches = [key for key in self.files if key.endswith(":" + relative)]
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
            })
    return imports


def build_report(root: Path) -> dict:
    roots = [root / rel for rel in SOURCE_ROOTS]
    index = SourceIndex(roots)
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
                    unresolved.append({
                        "from": key,
                        "line": item["line"],
                        "import": name,
                        "alias": item["alias"],
                        "reason": "internal_namespace_unresolved",
                    })
                    continue
                for target in matches:
                    edges.append({
                        "from": key,
                        "to": target,
                        "import": name,
                        "alias": item["alias"],
                        "resolution": kind,
                    })
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
    edges.sort(key=lambda item: (item["from"], item["to"], item["import"]))
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
            "seedCount": len(SEEDS),
            "resolvedSeedCount": len(seed_keys),
            "reachableSourceFileCount": len(visited),
            "internalImportEdges": len(edges),
            "unresolvedInternalImportCount": len(unresolved),
            "externalImportCount": len(external),
            "resolutionKinds": dict(sorted(resolution_counts.items())),
            "externalPackageGroups": dict(sorted(external_groups.items())),
            "dependencyFamilyFileCounts": dict(sorted(Counter(
                family for node in nodes for family in node["dependencyFamilies"]
            ).items())),
        },
        "nodes": nodes,
        "edges": edges,
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
        for path, body in (
            (a, "package demo.a\nimport demo.b.B as Bee\nimport demo.b.*\nimport demo.b.helper\nimport demo.missing.Nope\nclass A"),
            (b, "package demo.b\nimport demo.a.A\nclass B"),
            (c, "package demo.b\nfun helper() = Unit"),
            (d, "package demo.b\nclass D"),
        ):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body, encoding="utf-8")
        idx = SourceIndex(roots)
        resolved, kind = idx.resolve("demo.b.B")
        assert kind == "file" and any(item.endswith(":demo/b/B.kt") for item in resolved)
        resolved, kind = idx.resolve("demo.b.helper")
        assert kind == "top_level_symbol" and any(item.endswith(":demo/b/Helpers.kt") for item in resolved)
        resolved, kind = idx.resolve("demo.b.*")
        assert kind == "wildcard" and len(resolved) == 3
        report = build_report(base)
        # The production seed list is intentionally absent in this fixture;
        # graph resolver behavior is tested directly and missing seeds surfaced.
        assert len(report["seeds"]["missingOrAmbiguous"]) == len(SEEDS)
        assert "Same-package references" in " ".join(report["limitations"])
        assert idx.resolve("demo.missing.Nope")[1] == "unresolved"
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
