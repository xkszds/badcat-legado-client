# Build status

Last updated: 2026-11-15

## Latest implementation update

- Added a deterministic Legado URL resolver based on the pinned upstream `AnalyzeUrl.kt`: relative URL resolution, numbered page-list tokens such as `<1,2,3>`, and static JSON URL options for method, headers, body, timeout, redirect policy, retry count, charset and DNS IP fields.
- Added resolver tests for relative/page URL handling, static POST options, malformed option JSON and explicit rejection of script-bearing URL rules.
- Important boundary: parsed timeout/redirect/retry/charset/DNS metadata is currently returned as a typed result but is not yet wired into the OkHttp transport. JavaScript URL templates (`{{...}}`, `@js`), `js`, `bodyJs` and WebView options are explicitly rejected until the pinned Rhino/WebView runtime exists; they are not sent as literal URL text.

- Added `OkHttpSourceRequestClient` using the pinned Legado baseline OkHttp 5.4.0.
- HTTP requests now support static source/request headers, GET/HEAD and body-bearing methods, redirect following, final URL/status/headers/content type/body capture, configurable connect/read/write timeouts, and an 8 MiB default response limit.
- Added an in-memory cookie jar isolated by source ID. It respects cookie domain/path/secure/expiry matching; the existing per-source `enabledCookieJar` flag disables cookie storage and sending for that request.
- Added cookie-jar unit tests for domain/path/secure scoping and isolation between sources.
- Added `SourceRuleResolver.evaluateStage` and `SelectorLegadoRuleRuntime` to connect nested stage JSON to selector primitives when each field's selector mode is explicitly configured.
- These changes are committed to the connected GitHub repository. No successful test/build run has been observed; the latest commit has no reported workflow runs or status checks.

## Added

- Android application module and manifest; launcher remains a temporary build-validation shell.
- Kotlin/JVM integration contract module and JDK 21 toolchain.
- Kotlin 2.4.20 and Android Gradle Plugin 8.13.2 aligned with the pinned Legado upstream baseline.
- Legado book-source JSON import/export codec retaining full raw source JSON and mapping core metadata, URLs, headers, cookie flag, JS fields and nested explore/search/book-info/TOC/content rule JSON.
- Purification-domain interfaces that keep novel text replacement, page-ad filtering, catalog cleanup and video rules separate.
- Request-settings factory for static source headers and case-insensitive request overrides.
- Bounded rule-expression tokenizer and combination evaluator for `&&`, `||` and `%%`.
- CSS, JSONPath and XPath selector primitives, plus an explicit selector dispatcher.
- Nested source-stage rule object resolution and selector-only runtime adapter.
- GitHub Actions workflow intended to run contract tests and assemble a debug APK.
- Pinned upstream Legado commit and GPL-3.0 license/notice documentation.

## Important limitations

- The selector runtime is deliberately partial. It does not yet implement the full upstream `RuleAnalyzer`, embedded `{$...}` interpolation, regex suffixes, all Legado parser modes, chained rule grammar or replacement semantics.
- The HTTP adapter handles static headers and HTTP only. It does not yet evaluate Legado URL templates, request JS, source login scripts, custom encodings, or all upstream request options.
- Cookie persistence is currently in-memory only; it is lost when the process exits. It is source-isolated but not yet integrated with a persistent Android cookie store.
- There is no Rhino/JavaScript runtime, complete search/detail/TOC/content orchestration, or end-to-end source fixture against a live book source.
- Original Badcat UI/navigation has not yet been migrated into the new Android app. The current launcher is not a replica of the original UI.
- Badcat's actual purification rules/behavior have not yet been ported; only separated contracts exist.
- The original APK remains a read-only reference. No shell/signature/native protection bypass or patching has been performed.
- Only the upstream GPL-3.0 license text has been copied so far; upstream runtime source is not yet vendored. Any later distribution of incorporated Legado code must meet GPL-3.0 source and notice obligations.

## Verification status

- Unit tests have been authored for codecs, request settings, selectors, rule combination, source-stage dispatch and cookie matching, but have not been executed in this environment.
- Latest implementation commit before this status update: `91b2a1cd23921e77240cb572412322879823d554`.
- GitHub Actions query for that commit returned no workflow runs; combined commit status returned no checks. This is not a passing build.
- No installable APK has been produced, downloaded or installed.
- No Gradle wrapper is committed; the workflow provisions Gradle 8.14.4 when it runs.

## Next engineering steps

1. Wire resolved URL option metadata into the OkHttp request client, including timeout/redirect/body/header handling; then add the pinned Rhino runtime before enabling JavaScript URL templates.
2. Implement the actual Legado rule mode parser, replacement pipeline and safe Rhino initialization/bindings.
3. Build the end-to-end search, book-detail, TOC and chapter-content flow on top of the transport and rule runtime.
4. Port original Badcat UI/navigation and purification behavior without changing its established interaction structure.
5. Run CI, fix compiler/test failures, then produce and install a candidate APK for device validation.
