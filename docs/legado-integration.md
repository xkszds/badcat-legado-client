# Legado adapter integration gates

## Integration options

1. **In-process engine** — intended direction for a self-contained client. It requires the Legado app-layer models, persistence, initialization, rule execution environment and transitive dependencies. The book/rhino Android libraries alone are not the full source engine.
2. **Legado app HTTP API** — requires a separately installed/running Legado app with its Web service enabled; it is a service integration, not an embedded engine.
3. **Legado ContentProvider** — requires the Legado app to be installed and provider permissions; it is not an in-process SDK.

The intended direction is an in-process adapter, subject to source, license and dependency review.

## Implemented groundwork

- Immutable upstream revision and build versions are recorded in `gradle/legado-upstream.properties`.
- `LegadoBookSourceCodec` imports single-object or array JSON exports, validates `bookSourceUrl` and `bookSourceName`, reads basic source metadata, and preserves the entire raw source object for round-trip export.
- The codec deliberately does not attempt to execute rules. Nested search/detail/TOC/content rules and JavaScript fields are retained intact for the future runtime adapter.


## Bounded source execution currently available

- The host has a CSS selector evaluator for `@text`, `@ownText`, `@html`, `@href`, `@src`, `@tag` and `@attr(name)`, plus the currently tested top-level `&&`, `||` and `%%` combination operators. This is a limited selector subset, not full Legado AnalyzeRule grammar.
- The search-result parser supports a CSS `bookList` and per-item CSS fields for name, book URL, author, cover, last chapter and intro. Combined `bookList` branches are normalized independently to element HTML before per-card parsing; scalar extraction suffixes on `bookList` are rejected rather than misread as card markup. Per-item fields support the bounded CSS suffix set above, including `@ownText` and `@tag`. Other parser types, filters, JS interpolation and complex rule chains remain unsupported unless explicitly handled by a separate adapter.
- Embedded `{{...}}` URL expressions can be evaluated through the bounded injected Rhino evaluator; `@js` URL rules and full Legado request bindings are still fail-closed.
- Book-source JSON import uses strict UTF-8 decoding and a 5 MiB byte limit. Local catalog reads and writes enforce the same limit; unknown JSON fields are retained for round-trip export.

These boundaries are intentionally explicit: an imported source can be stored even when some of its rules cannot yet be executed. A successful import is not a compatibility guarantee.

## Pinning checklist

- Record a full immutable upstream commit SHA; do not rely on a floating branch for a release.
- Review that commit's GPL-3.0 license and all bundled dependency licenses.
- Trace initialization required by source parsing and JavaScript execution.
- Trace persistence/database migrations and model serialization.
- Identify Android components and app globals that cannot be reused directly.
- List required network, parser, script-engine and coroutine dependencies.
- Build a minimal adapter proof before connecting it to host UI.

## Adapter acceptance tests

- Import/export source fixtures without losing rule fields.
- Search/detail/catalog/content parsing with deterministic HTML fixtures.
- JavaScript rule execution with a controlled test environment.
- Replacement rule ordering and scope.
- Network errors, empty results, invalid rules and cancellation map to typed engine errors.
- Page/video/catalog rules are not run against chapter text unless explicitly configured for that domain.

## Current status

The platform-neutral engine port, purification boundaries and source JSON codec are present. The Android host has a bounded Rhino evaluator with string bindings, but no complete Legado online rule runtime (AnalyzeUrl / AnalyzeRule host environment) or original Badcat UI integration yet. CI contract tests and debug APK assembly passed in [run #43707974288](https://github.com/xkszds/badcat-legado-client/actions/runs/43707974288); device installation and runtime validation remain outstanding.
## 2026-11-16: rule-mode dispatch expansion

- Added a shared Legado rule-expression dispatcher for explicit `@css:`, `@xpath:`, `@jsonpath:` / `@json:` prefixes.
- Unprefixed selectors default to CSS; unambiguous XPath (`/`, `//`) and JSONPath (`$`) expressions are inferred. Ambiguous expressions are not guessed as XPath or JSONPath.
- Search result item fields, book-detail scalar fields, TOC chapter scalar fields, and chapter-content/title scalar fields now use this dispatcher. The selector-stage adapter also infers parser modes when no explicit per-field override is supplied.
- Collection roots `bookList` and `chapterList` remain CSS element collections in this iteration. XPath/JSONPath collection-root semantics, regex extraction, JS rule execution, Legado chained rule operations, filters, variable state, and full `AnalyzeRule` behavior are not claimed.
- Tests cover default CSS, explicit XPath, explicit JSONPath, and fail-closed JS/regex dispatch, plus book-detail XPath/JSONPath fields.
- This is an incremental compatibility improvement, not complete Legado engine parity. The next gate is connecting the script runtime to the full source rule pipeline and broadening collection/pagination semantics.
