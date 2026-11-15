## Latest implementation update (2026-11-15)

- Added per-chapter reading-position persistence to the development reader. The current ScrollView offset is saved when leaving the reader and before chapter navigation; each position is keyed by source ID, book URL and chapter URL using URL-safe Base64, then restored after the newly loaded chapter has been laid out. Switching chapters therefore restores that chapter's own last position rather than reusing the previous chapter's offset.
- This is scroll-position restoration for the current TextView/ScrollView prototype only. It does not provide paragraph anchors, pagination/page-turn gestures, background/line-spacing/brightness controls, or the original Badcat reading UI. No compilation, tests, device validation or APK generation is confirmed.

## Latest implementation update (2026-11-15)

- Extended the development reader with TOC-based previous/next chapter navigation, persistent last-read chapter URL, a book-detail `继续上次阅读` entry, and adjustable 14–32sp text size persisted across sessions. Progress keys use URL-safe Base64 of the source ID and book URL to avoid ordinary hash collisions. Chapter navigation is only available when the current chapter URL matches an entry in the fetched TOC.
- The reader still uses a plain selectable `TextView` in a `ScrollView`; it does not yet preserve intra-chapter scroll position, implement swipe/page-turn gestures, or match Badcat's original reading UI. Reading progress is chapter-level only.
- Changes are source edits only. No Gradle compilation, automated test run, device validation, or APK generation is confirmed.

- Extended the development search flow: search result rows now open `SourceBookActivity`, which requests book detail and TOC through `ReadingEngine`; selecting a chapter opens `SourceChapterActivity`, which fetches and displays chapter text. Both activities are registered as non-exported. This is an end-to-end UI path for the currently supported engine subset, not the original Badcat UI or a complete Legado reader.
- The reader currently displays extracted text only; paging/next-chapter navigation, typography controls, bookmarks, offline caching and the original reading gestures are not implemented. `chapterContent()` currently passes a URL-only chapter descriptor, so source-specific title fallback is handled in the UI.
- No live source fixture, Gradle build/test, CI/device validation or APK is confirmed.

- Added `SourceSearchActivity`, a minimal development search workflow: it loads enabled imported sources with a configured `searchUrl`, lets the user select a source and enter a keyword, invokes `ReadingEngine.search()` on a worker thread, and displays candidate metadata or typed errors. Registered it in the manifest and added a launcher-shell entry. This is not the original Badcat search UI.
- Declared `kotlinx-coroutines-android` directly in the app module because the Android host screens use coroutine APIs; the engine search call itself is run off the UI thread.
- Scope remains deliberately bounded: CSS parser subset and limited URL template evaluation only. Sources depending on unsupported `@js`, advanced Legado rules, or missing selector definitions may fail. No live source fixture, Gradle build, test execution, CI/device validation or APK is confirmed.

- Added a minimal `SourceSearchActivity` that loads enabled imported sources with a nonblank `searchUrl`, lets the user choose a source and enter a keyword, calls `ReadingEngine.search()` off the UI thread, and displays parsed candidate metadata/errors. Added a launcher-shell entry and manifest registration. This is a development validation screen, not the original Badcat search UI.
- Search remains limited to the implemented CSS parser and bounded URL-template path. Sources requiring unsupported `@js`, advanced Legado rules, or missing rule definitions will not work as in full Legado. No live source fixture, network test, Gradle build, device validation or APK is confirmed.

- Added `BadcatReadingEngineFactory` as the Android composition root: it creates one durable `FileBookSourceCatalog`, `BookSourceSearchUrlResolver`, a shared per-source-aware gateway, and the search/detail/TOC/chapter-content services behind `SourceReadingEngine`. Chapter-text purification remains an explicit injection and defaults to identity until Badcat's real rules are ported.
- `MainActivity` now constructs this engine graph at launch without changing the temporary screen layout or claiming the original Badcat UI has been restored. The factory exposes its catalog instance for import/export and source-management UI to share.
- This is dependency composition, not yet a user-facing search workflow. No Gradle build, tests, CI/device validation or APK generation is confirmed; the pinned HtmlUnit Rhino fork JAR remains a build blocker.

- Fixed the multi-source evaluator isolation hazard in the shared HTTP gateway: it now supports a per-request evaluator provider receiving the exact `BookSource` passed to `execute()`. The Android factory exposes `create()` for shared service graphs and retains `forSource()` for intentionally single-source graphs.
- Added a gateway regression test that sends requests for two distinct source IDs through one gateway and asserts the evaluator sees the matching source each time. This guards against capturing source A's runtime context for source B.
- This addresses source-context selection only; it does not add full Legado source bindings or wire the engine into the launcher/search UI. Tests are authored but not executed. No Gradle build, CI/device validation or APK generation is confirmed.

- Added `BookSourceSearchUrlResolver`, the concrete `SearchUrlResolver` for imported source metadata. It returns the configured `searchUrl` without prematurely substituting or encoding `{{key}}`; the HTTP gateway remains responsible for bounded template expansion before URI parsing/encoding.
- The resolver rejects disabled sources, blank keywords, missing search URLs and unsupported `@js` syntax. Added four contract tests for those cases and preservation of the raw template.
- This fills the missing search-URL resolution contract, but does not yet constitute an end-to-end Android search screen: the app still needs a source-aware engine composition path and UI wiring. The current gateway factory is source-scoped, so a single shared service graph must not reuse one source's evaluator for other sources.
- Tests are authored but not executed. No Gradle build, CI run, device validation or APK generation is confirmed. The pinned HtmlUnit Rhino fork JAR remains missing.

## Latest implementation update (2026-11-15)

- Added `BadcatSourceHttpGatewayFactory` as the Android-side composition entry point. It constructs a source-scoped gateway with the bounded Rhino embedded-template adapter and the OkHttp transport, while allowing the transport and script runtime to be injected for tests/host configuration.
- The factory opts into only the `{{...}}` URL-template subset and the string `key` binding. It does not enable `@js`, `bodyJs`, WebView, Java host objects, login/session services or full Legado AnalyzeUrl bindings. Directly constructed gateways remain fail-closed by default.
- This is a composition entry point, not evidence that the app's launcher/search UI calls it. `MainActivity` remains a temporary validation shell; no end-user search flow is wired.
- No Gradle build/test, CI run, device validation or APK generation has been performed. The pinned HtmlUnit Rhino fork JAR remains missing and blocks a verified build.

## Latest implementation update (2026-11-15)

- Connected the bounded `{{...}}` evaluator to `SourceHttpGateway` as an explicit opt-in dependency. The gateway expands expressions before URL resolution and query/form encoding, passes the search keyword as `key`, and aborts before transport on evaluator failure. The default gateway still rejects unresolved templates.
- Added an Android adapter that bridges the pinned bounded Rhino runtime to the template contract with only the string `key` binding. No Java host objects, HTTP, cookies, cache, login, proxy, or WebView bindings are exposed. The search service now forwards its keyword to the gateway so the template path can be used end to end when the host explicitly injects the adapter.
- Added gateway regression cases for fail-closed default behavior, Unicode/space key expansion before percent-encoding, and no network request after script failure. Corrected the template result conversion fallback for other primitive values.
- This is a narrow embedded-template path, not full Legado AnalyzeUrl: `@js`, `<js>`, `@result`, `page`/ `baseUrl` and shared host bindings, `js`/`bodyJs`, WebView, login/session and other host services remain unsupported. The Android adapter is available but is not yet wired into the app's active search construction path.
- No Gradle tests/build, CI run, device validation or APK generation has been performed. The pinned HtmlUnit Rhino fork JAR remains missing from the repository and is still a hard build blocker.

## Latest implementation update (2026-11-15)

- Repaired the URL encoder character-set definitions and aligned the query allowlist with the pinned Legado `NetworkUtils` bitset and `AnalyzeUrl` `RFC3986` codec. The query path preserves valid already-encoded strings; otherwise it percent-encodes with the configured charset. The form path retains the upstream `&` / first-`=` splitting and `URLEncoder`/`escape` branches.
- Integrated request preparation into `SourceRequestFactory` after source headers and URL-option headers are merged. GET/HEAD and other non-POST methods encode query parameters; POST text bodies become form fields only when no effective `Content-Type` is present and the body is neither JSON nor XML. Raw Unicode/space query values are encoded before `URI` resolution.
- Added resolver and request-factory regression cases for Unicode query values, GET encoding, POST form encoding, JSON bodies and explicit content types. Existing script-bearing URL rules remain rejected; embedded `{{...}}` evaluation is not yet connected to this pipeline.
- A standalone Kotlin compiler smoke harness passed eight checks covering UTF-8/GBK query encoding, form splitting, existing/malformed escapes, `escape`, GET/POST preparation, and explicit content type. The repository's Gradle/Kotlin test suite, full Android build, CI, device validation and installable APK remain unverified.

## Latest implementation update (2026-11-15)

- Added isolated `LegadoUrlParameterEncoder` matching the pinned upstream `AnalyzeUrl.encodeParams` and `NetworkUtils` behavior: RFC3986 query encoding, upstream already-encoded detection, form splitting at `&` and the first `=`, charset-specific encoding, and legacy `escape` handling.
- Added nine regression tests for safe query characters, UTF-8 and GBK encoding, literal/malformed percent sequences, form delimiters and empty segments, explicit charset re-encoding, escape mode, and invalid charsets. Tests are authored but not executed.
- Kept this encoder separate from `LegadoUrlResolver` and the template evaluator. URL preparation still does not orchestrate template expansion followed by GET-query/POST-form encoding, so JavaScript-bearing URLs remain fail-closed.
- Ran a standalone `kotlinc` smoke harness against a local copy of the isolated encoder: UTF-8/GBK query encoding, form splitting, existing/malformed percent escapes, escape mode, empty segments, and the query-safe set passed. This does not execute the repository's Kotlin test suite or compile the Android/Gradle project. No Gradle build, project test execution, CI success, device validation, or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Added isolated `LegadoEmbeddedUrlTemplate` and injected `LegadoEmbeddedScriptEvaluator` contracts. The helper processes complete `{{...}}` segments in order, passes only the search key, mirrors upstream null/string/integral-Double conversion and preserves surrounding literal text when an expression returns empty, leaves unmatched opening markers literal, and propagates evaluation failures without returning a partial URL.
- Added regression tests for multiple expressions, Unicode key input, null and integral numeric conversion, unmatched markers, and script failure. Tests are authored but not executed.
- Deliberately did not wire this helper into `LegadoUrlResolver` or search: upstream query/form encoding and already-encoded detection are not implemented yet. Existing script-bearing URLs continue to fail closed. No Gradle build, test run, CI, device validation, or APK is confirmed.

## Latest implementation update (2026-11-15)

- Added `docs/analyze-url-js-compatibility-gate.md` after re-reading the pinned upstream `AnalyzeUrl.kt`, `ScriptBindings.kt`, and `RhinoScriptEngine.kt`. It records the exact order of `@js`, embedded `{{...}}`, page tokens, URL-option parsing, and query/form encoding; it also records the upstream primitive conversion rules and why the current string-only sandbox is not yet safe to wire into search.
- Confirmed that upstream embedded JavaScript receives `key` plus additional host bindings, and that query/form encoding happens after expression evaluation. The current adapter exposes neither the complete binding model nor the exact encoding pipeline, so script-bearing URL rules remain fail-closed.
- This is a source-level compatibility audit only. No runtime behavior was enabled, and no Gradle build, tests, CI run, device validation, or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Added `LegadoRhinoSandbox` inside `:legado-rhino`: string-only host bindings, bounded script/binding sizes, primitive-only return values, and a coroutine timeout enforced through the upstream Rhino instruction observer.
- Added Android `RhinoSourceScriptRuntime` implementing the platform-neutral `SourceScriptRuntime` contract. It maps parse/unsupported/timeout outcomes and propagates caller cancellation. It deliberately does not expose Java/Android services, HTTP, cookies, cache, or WebView objects.
- Important build dependency correction: the pinned `org.htmlunit:htmlunit-core-js:5.3.0-legado.4` fork is **not** a Maven Central release. Upstream stores it in `third_party/maven`; the local Maven repository and POM metadata are now configured. The required JAR (1,900,654 bytes; upstream Git blob SHA `d240cdd66f5269ed33da53cde7e4e690cee6e162`) is not yet in this repository because the available GitHub integration cannot transfer binary blobs. The project must not substitute stock HtmlUnit 5.3.0, which lacks the pinned Legado fork changes. This missing JAR is a hard build blocker.
- Script execution is not yet wired into `LegadoUrlResolver`/search URL expansion. Full `AnalyzeUrl` compatibility still requires the source model, headers/login/cookies/proxy, URL options, HTTP semantics and reviewed host bindings. Existing JS-bearing URL rules remain rejected.
- No Gradle build or tests were run. No CI success, device validation, or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Added the pinned Legado Rhino runtime as a separate Android library module `:legado-rhino` and wired it into the app build. All 18 upstream Kotlin source files were copied from `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9`, including the class-shutter, wrap-factory, context and execution-gate code.
- Added the upstream Legado GPL-3.0 license and attribution. Also preserved the HtmlUnit Rhino dependency's MPL-2.0, Apache-2.0, source provenance, NOTICE and diff records under `third_party/htmlunit-core-js-5.3.0-legado.4/`. Original Oracle/Rhino source headers remain unchanged.
- Added the module's direct dependencies at upstream-pinned versions and included it in the app. This is a build integration step only: no Legado host bindings, AnalyzeUrl compatibility layer, or source-JS execution path is active. Existing fail-closed behavior must remain until security and compatibility tests are implemented.
- No Gradle build or test was run here. GitHub currently reports no commit status or workflow run for this revision. No device validation or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Vendored all 18 Kotlin source files from LegadoTeam/legado `modules/rhino` at pinned revision `330e606c4d57e08adc056ff8ab7a4132c784da9`, including `RhinoClassShutter`, `RhinoWrapFactory`, context handling and script engine implementation.
- Added an Android library module `:legado-rhino` with upstream-pinned dependencies (HtmlUnit Rhino 5.3.0-legado.4, coroutines 1.11.0, OkHttp 5.4.0, AndroidX Collection 1.6.0), included it in settings, and added it as an app dependency.
- Added the upstream GPL-3.0 license and third-party attribution. The upstream Kotlin sources are vendored; this is not yet a complete Legado source-rule integration. No host bindings or JS source-rule execution have been enabled. The app must continue to fail closed for JS until that work and security tests are complete.
- No Gradle build or test was run in this environment; no CI success, device validation, or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Expanded the pinned Rhino integration audit with the exact upstream Android-library configuration, four direct dependencies and versions, and the complete 18-file Kotlin runtime inventory. This confirms the class-shutter/wrap-factory protections are part of the module and must not be omitted.
- No runtime source has been copied into the app yet; no JavaScript capability has been enabled. This update is source inventory and integration planning only.

## Latest implementation update (2026-11-15)

- Added `docs/legado-rhino-integration-audit.md` after inspecting the pinned upstream `AnalyzeUrl`, `RhinoScriptEngine`, `ScriptBindings`, `RhinoContext`, `RhinoClassShutter` and `RhinoWrapFactory` sources. It records why the Maven Rhino artifact alone is insufficient and the exact security, binding, encoding, licensing and regression-test gates required before enabling source JavaScript.
- This is an upstream source audit and integration specification, not a Rhino implementation. JavaScript rules remain fail-closed; no build, CI, device test or APK is confirmed.

## Latest implementation update (2026-11-15)

- Corrected error classification for unsupported static URL-option HTTP methods: a syntactically valid but unimplemented method (for example DELETE) now returns `UNSUPPORTED_RULE`, while malformed option data continues to return `INVALID_SOURCE`. Updated the regression expectation accordingly.
- Clarified the older charset-validation note: the current resolver and request factory reject Legado's `escape` mode; only ordinary JVM-supported charsets are accepted. The older note saying `escape` was retained is historical and no longer describes current behavior.
- Tests are authored but not executed. No compile, CI run, device test or installable APK is confirmed.

## Source audit correction (2026-11-15)

- A follow-up source review found a compile-blocking stale reference in `LegadoUrlResolver`: the resolver's result constructor still referenced local `dnsIp` and `type` values after those options had been changed to fail closed. Removed the stale constructor arguments and removed the unused fields from `LegadoResolvedUrl`, so the contract no longer implies those unsupported transport features are available.
- This is a static source correction only. The project has not been compiled and the regression suite has not been executed; additional compile issues may remain.

## Latest implementation update (2026-11-15)

- Removed a misleading charset compatibility path: the transport previously accepted Legado's `escape` response mode but decoded the bytes as UTF-8. The URL resolver and shared request factory now reject this mode explicitly until the actual Legado escape decoder is implemented.
- Added resolver- and factory-level regression tests for this unsupported mode. Ordinary JVM-supported charsets remain validated and passed to the transport.
- Tests are authored but not executed. No compile, CI run, device test or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Tightened the static URL resolver so the base and final resolved address must be absolute HTTP(S) URLs with a host and no embedded credentials; this catches unsafe addresses before they reach the request factory.
- Closed a silent-compatibility gap: `dnsIp`, `resolveIp` and `type` were previously parsed into metadata but never applied by the HTTP transport. They now return explicit `UNSUPPORTED_RULE` instead of appearing to work. Retry values above the transport's 0–5 limit are rejected during URL resolution.
- Added regression tests for unsafe base/final URLs, unsupported transport options and excessive retry values. Tests are authored but not executed; no compile, CI run, device test or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Added validation at the shared request-factory boundary for transport options: retry count is limited to 0–5, timeout must be positive and fit the transport's supported millisecond range, and response charset names must be recognized (with Legado's explicit `escape` mode retained).
- Added regression tests for excessive retries, zero/oversized timeouts and invalid charset names. This prevents malformed imported source options from reaching OkHttp as late runtime failures.
- Tests are authored but not executed. No compile, CI run, device test or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Hardened the shared HTTP request factory: only absolute HTTP/HTTPS URLs with a host and without embedded URL credentials are accepted. Header names are restricted to HTTP token characters, and header values containing CR/LF or disallowed control characters are rejected before transport creation.
- Added regression tests for non-HTTP schemes, relative URLs, credential-bearing URLs, CRLF header injection and invalid header names. Corrected Kotlin character escape literals during source review.
- Tests are authored but not executed. No compile, CI run, device test or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Added method validation at the shared `SourceRequestFactory` boundary, so callers cannot bypass the URL resolver's transport-method allowlist by constructing requests directly. Supported methods are GET, HEAD, POST, PUT, PATCH, PROPPATCH and REPORT; unsupported methods return `UNSUPPORTED_RULE` before transport creation.
- Added a regression test for direct factory rejection of DELETE.
- Tests are authored but not executed. No compile, CI run, device test or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Added `UnsupportedSourceScriptRuntime` as an explicit fail-closed default for hosts that have not integrated the pinned Legado Rhino runtime. It returns `UNSUPPORTED_RULE` and never echoes script text as if it were an evaluated result.
- Added a coroutine-based contract test for the fail-closed behavior. This is a safety boundary, not a Rhino implementation; JavaScript-bearing book sources remain unsupported.
- Tests are authored but not executed. No compile, CI run, device test or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Aligned the static Legado URL-option method allowlist with the methods already implemented by the shared HTTP transport: GET, HEAD, POST, PUT, PATCH, PROPPATCH and REPORT. This removes an avoidable mismatch where the URL resolver rejected body methods the transport could already execute.
- Added regression tests for PUT/PATCH/PROPPATCH/REPORT forwarding and explicit rejection of DELETE, which is not implemented by the transport.
- Tests are authored but not executed. No compile, CI run, device test or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Added disabled-source regression tests for the TOC and chapter-content entry points. Together with the existing search and book-detail cases, the tests now exercise the shared source-enabled gate across all four reading operations; the fake transport fails immediately if any disabled operation reaches HTTP.
- Tests are authored but remain unexecuted. No compile, CI run, device test or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Expanded `LegadoUrlResolverTest` with regression cases for the `@js` URL form and the `js`, `bodyJs` and `webView` request options. Each case asserts an explicit `UNSUPPORTED_RULE` result, guarding against accidentally forwarding script-bearing source rules as ordinary URLs.
- These are authored regression tests only; no local Gradle installation/wrapper is available in this environment, so they have not been executed. No build, CI run, device test or APK is confirmed.

## Latest implementation update (2026-11-15)

- Centralized the enabled-source gate in `SourceReadingEngine.findSource`, so disabled sources are rejected consistently for search, book-detail, TOC and chapter-content operations rather than relying only on the HTTP gateway. Search still rejects blank keywords and blank resolved URLs before requesting.
- Added a regression test that verifies a disabled source cannot enter the book-detail request path; the existing search test verifies the URL resolver is not called for a disabled source. Tests are authored, not executed.
- Reviewed the pinned Legado Rhino artifact metadata: upstream uses an HtmlUnit Rhino fork (`org.htmlunit:htmlunit-core-js:5.3.0-legado.4`) plus Legado-specific context, bindings, class shutter and execution guards. Adding only the Maven artifact would not reproduce those safety/runtime semantics, so JavaScript URL evaluation remains disabled instead of introducing an unreviewed permissive script engine.
- No Android search UI or executable full Legado runtime is wired. No compilation, CI run, device test or installable APK is confirmed.

## Latest implementation update (2026-11-15)

- Hardened `SourceReadingEngine.search`: blank/whitespace-only keywords are rejected before source lookup; disabled sources are rejected before the URL resolver or HTTP service can run; blank resolved URLs are rejected before network access.
- Added unit-test cases proving disabled sources do not invoke the resolver and blank keywords are rejected before resolution. Tests are authored but have not been executed.
- Verified the pinned upstream Legado `AnalyzeUrl.kt` at commit `330e606c4d57e08adc056ff8ab7a4132c784da9`: `{{...}}` expressions are evaluated as JavaScript with `key` in the runtime bindings, before URL query/form encoding. Therefore a generic text substitution is not a faithful or safe implementation. Dynamic URL expressions remain unsupported until the pinned Rhino behavior and encoding pipeline are integrated.
- No search UI is wired to `SourceReadingEngine`; no compile, CI run, device test, or installable APK is confirmed.
## Latest implementation update (2026-11-15)

- Added per-source enable/disable controls to the temporary source catalog screen. Changes are persisted by `FileBookSourceCatalog.setEnabled`; the codec updates only the upstream `enabled` property and retains other JSON fields.
- Added a codec regression test for toggling the enabled flag while preserving unknown fields and nested search rules. The test is authored but has not been executed.
- The switch currently controls the catalog's enabled flag only. Search UI, source editing/deletion, and live source execution are not implemented. The temporary screen remains separate from the original Badcat UI.
- No compile, device test, CI result, or installable APK is confirmed.
## Latest implementation update (2026-11-15)

- Added a temporary Android `SourceCatalogActivity` with system document-picker import/export for Legado JSON, a local source list, and import/export result messages.
- Added a launcher entry into that screen and registered it in the manifest. The UI is explicitly a development utility screen, not a recreation of Badcat's original source-management UI.
- Import/export uses the durable `FileBookSourceCatalog`; the screen displays source names/IDs and now provides per-source enable/disable switches. The flag is persisted back into each source JSON while retaining unknown fields and nested rules. Source execution and search are not yet wired to this screen.
- These Android changes have not been compiled or exercised on a device. No CI run or APK is confirmed.
## Latest implementation update (2026-11-15)

- Added Android-host `FileBookSourceCatalog`: imports one Legado book-source JSON object or an array, merges by `bookSourceUrl`, persists the catalog in app-private storage through Android `AtomicFile`, exposes catalog entries through the existing `BookSourceCatalog` contract, and exports the stored entries as a Legado JSON array.
- Import validation is delegated to `LegadoBookSourceCodec`; full raw JSON is retained so unknown upstream fields survive round trips. The catalog stores source definitions only: it does not execute JavaScript, and it is not yet connected to the launcher or a source-management screen.
- The catalog implementation has not been compiled or tested. No CI run or APK is confirmed for this change.

# Build status

Last updated: 2026-11-15

## Latest implementation update

- Added `SourceReadingEngine`, a concrete orchestration adapter for the existing `ReadingEngine` contract. It connects source catalog lookup, injected search-URL resolution, search, book detail, TOC and chapter-content services; catalog failures are preserved and unknown source IDs return `NOT_FOUND`. Adapter tests cover catalog exposure and missing-source behavior.
- The adapter still requires a real persistent source catalog and a Legado-compatible search URL resolver to be supplied by the Android host. It is not yet wired into the placeholder `MainActivity` or the original Badcat UI.
- Corrected the `nextContentUrl` interpretation: it is same-chapter content pagination, not the next chapter. `ChapterContent.nextPageUrl` now reflects that distinction; chapter navigation remains the responsibility of the TOC.
- The chapter fetch service now accepts an injected `BookTextReplacement` and applies it only after chapter HTML has been parsed into novel text; replacement failures are returned as typed parse failures. Page HTML, catalog and video purification remain separate domains.
- Added `CssChapterContentParser` for the bounded CSS subset of Legado `ContentRule`: chapter body, optional title, and same-chapter next-page URL. HTML output is converted to readable text with script/style/iframe/noscript elements removed and paragraph/line breaks preserved.
- Added `SourceChapterContentService` to fetch all pages of one chapter through the shared HTTP gateway, follow `nextContentUrl` with cycle and 20-page guards, aggregate text, and apply the text-only replacement hook. Added tests for multi-page aggregation, next-link resolution, fallback title, and script-rule rejection.
- Explicit unsupported content features remain: `webJs`, `callBackJs`, JavaScript/interpolation, `sourceRegex`, `replaceRegex`, `payAction`, image decoding/style, content batching, and full upstream AnalyzeRule semantics. This is not yet full Legado content compatibility; ad purification remains a separate pipeline.
- Tests remain authored but unexecuted. No successful CI run or installable APK is confirmed.
- Added `CssTocParser` for the official Legado `TocRule` CSS subset: `chapterList`, `chapterName`, `chapterUrl`, and `nextTocUrl`. Chapter and next-page URLs are resolved against the response URL.
- Added `SourceTocService` to fetch TOC pages sequentially, follow extracted next-page links, prevent URL loops, cap pagination at 20 pages, deduplicate chapters by URL, and assign stable indices. Added tests for chapter extraction, relative URLs, pagination URL extraction, and script-rule rejection.
- TOC limitations remain explicit: no full AnalyzeRule, JavaScript/formatJs, volume/VIP/pay metadata, reverse-TOC preferences, or advanced pagination expressions. Tests are authored but not executed; no CI/APK result is available.
- Added `CssBookInfoParser` for the official Legado `BookInfoRule` field names (`name`, `author`, `intro`, `coverUrl`, `lastChapter`, `tocUrl`) using the current CSS selector subset. It resolves relative cover/catalog URLs, requires a parsed book name, and rejects unsupported script/template/replace expressions.
- Added `SourceBookDetailService` to fetch detail pages through the shared HTTP gateway, check HTTP status, and parse the result. `BookDetail` now exposes the parsed optional `tocUrl` for the subsequent TOC request.
- Added book-detail parser tests for field extraction, relative URLs, missing title, and script rejection. These tests are authored but not executed; the detail flow is CSS-only and does not yet implement full Legado `AnalyzeRule` or init/redirect behavior.
- Added a bounded CSS-only search result parser using the official Legado `SearchRule` field names (`bookList`, `name`, `author`, `intro`, `lastChapter`, `bookUrl`, `coverUrl`). It extracts each book card independently, resolves relative book/cover links, skips incomplete cards, and deduplicates by book URL.
- Added `SourceSearchService` to execute a caller-resolved search URL through the HTTP gateway, reject unresolved JavaScript/interpolation, check HTTP status, and pass successful responses to the CSS parser.
- Added parser tests for multiple cards, relative links, missing fields, duplicate URLs, and unsupported script rules. Tests are authored but not executed; URL keyword/script resolution and full Legado `AnalyzeRule` semantics remain unimplemented.
- Added `SourceHttpGateway` to connect the supported static URL resolver, source-header/request factory, and injectable HTTP transport into one executable request path. Disabled sources are rejected before network access; resolver/factory failures are returned without reaching the transport.
- Added gateway tests for page token resolution, static POST options/header merging, disabled-source rejection, and script-rule rejection. These tests are authored but have not been run.
- Wired resolved static Legado URL options through `SourceRequestFactory` into `SourceHttpRequest`, and through `OkHttpSourceRequestClient` into per-request read/call timeouts, redirect policy, bounded retry-on-I/O-failure (0–5), and explicit response charset decoding.
- POST bodies now honor an explicit `Content-Type`; otherwise JSON/XML-looking bodies get matching media types and ordinary form bodies default to URL-encoded form content.
- Added a factory test asserting that resolved method, headers, body, timeout, redirect, retry and charset fields reach the transport request.
- Still not implemented: Legado query/form percent-encoding rules, DNS IP override, binary/data URI `type`, URL-option JavaScript/bodyJs, WebView, and retry semantics beyond bounded transport I/O retries. The parsed DNS/type metadata is not executed by the transport.

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

1. Add exact Legado query/form encoding semantics and decide/implement supported DNS/data-URI options; then integrate the pinned Rhino runtime before enabling JavaScript URL templates.
2. Implement the actual Legado rule mode parser, replacement pipeline and safe Rhino initialization/bindings.
3. Build the end-to-end search, book-detail, TOC and chapter-content flow on top of the transport and rule runtime.
4. Port original Badcat UI/navigation and purification behavior without changing its established interaction structure.
5. Run CI, fix compiler/test failures, then produce and install a candidate APK for device validation.
