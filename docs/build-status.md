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
