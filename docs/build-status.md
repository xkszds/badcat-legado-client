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
