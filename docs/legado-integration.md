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
