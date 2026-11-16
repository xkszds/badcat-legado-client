# Legado compatibility matrix

Updated: 2026-11-16

This matrix describes the independent Badcat client implementation, not the full
upstream Legado application. "Partial" means the behavior is intentionally bounded
and must not be treated as full BookSource/AnalyzeRule compatibility.

| Area | Current state | Supported behavior | Known gaps / acceptance work |
|---|---|---|---|
| BookSource JSON | Partial | Single object or array import; strict UTF-8; 5 MiB, 2,000-source, 64-level nesting and 512-field-per-source limits; known metadata/rule type checks; duplicate URL rejection; unknown-field-preserving export and enabled edits | Validate remaining upstream fields against a real-world source corpus; source runtime is still partial |
| Source management | Partial | Imported sources can be stored, enabled/disabled, selected, searched, explored and exported | Verify bulk import/export and persistence on device; improve source diagnostics |
| Search URL | Partial | Absolute/relative HTTP(S), page tokens, URL options for method/headers/body/timeout/retry/redirects/charset; bounded query encoding; pure @js and <js> URL expressions with string-only key/page/result bindings | Full Legado java/http/cookie/cache/WebView objects, webView, dnsIp, resolveIp, type, URL-option js/bodyJs and login/session state are not supported |
| HTTP transport | Partial | Async OkHttp, redirects with HTTPS downgrade prevention, per-source cookie jar, headers, common methods, response size cap, charset, retry | Legado DNS overrides, WebView requests, advanced request body and cookie semantics need coverage |
| Explore | Partial | Dedicated `exploreUrl` transport, separate `enabledExplore` gate and `ruleExplore.bookList` parsing through the same bounded search-card parser; exposed as `ReadingEngine.explore` | Explore filters, category discovery UI, script-driven exploration and real-source fixtures remain outstanding |
| Search result fields | Partial | CSS or bounded regex collection for bookList; CSS/XPath/JSONPath and bounded regex scalar extraction; relative book/cover URL resolution | Regex and collection limits apply; JS, filters, interpolation and direct-detail detection are not supported |
| Book detail | Partial | Name/author/intro/cover/latest chapter/TOC URL; CSS/XPath/JSONPath and bounded regex scalar extraction | JS, replacement chains, variables and source-specific init scripts are not supported |
| TOC | Partial | CSS or bounded regex chapter collection; CSS/XPath/JSONPath and bounded regex scalar title, chapter URL and next-page link extraction; multi-page pagination and cycle guards | Regex budgets apply; volume grouping, VIP/pay and script formatting are not implemented |
| Chapter content | Partial | CSS/XPath/JSONPath content extraction; CSS/XPath/JSONPath and bounded regex title/next-page URL; HTML-to-readable-text conversion; pagination and replacement hook | Regex chapter-body transforms, JS/webJs/callBackJs, sourceRegex, replaceRegex, payAction, imageDecode and contentBatch are not implemented |
| Selector expressions | Partial | Explicit @css:, @xpath:, @jsonpath: / @json:; unprefixed CSS default; unambiguous XPath and JSONPath inference | Complete upstream expression grammar, selector filters and mixed parser chains are not implemented |
| JavaScript | Partial | Pinned Rhino sandbox for bounded string-only script evaluation; URL @js/<js> supports key/page/result strings; replacement helper | Full Legado Java/http/cookie/cache/WebView bindings, source mainJs/jsLib and stage-specific AnalyzeRule execution are not integrated |
| Regex | Partial | Bounded regex replacement; staged collection roots for search/TOC; scalar capture-group extraction for search, detail, TOC and chapter title/next-page fields | Full upstream regex filter suffixes and every AnalyzeRule combination are not implemented |
| Replace rules | Partial | Scope, include/exclude matching, order, literal/regex replacement, optional JS evaluator hook | Not equivalent to all upstream Legado replace-rule and source-specific replacement behavior |
| Reader CSS profiles | Implemented | Multiple named profiles, notes, child selector rules, import/export, preview, sanitization and limits | Requires device UI/reader regression testing |
| Reading flow | Partial | Search → detail → TOC → chapter content → next-page aggregation → reading progress | Real-source corpus, live network behavior and physical-device acceptance remain outstanding |

## Parser dispatch increment (2026-11-16)

The shared rule-expression dispatcher now routes scalar extraction fields to CSS,
XPath or JSONPath based on explicit prefixes or unambiguous expression shape.
It is wired into search item fields, book detail fields, TOC chapter fields and
chapter content/title fields. The selector-stage adapter uses the same dispatcher. TOC next-page and chapter next-content links also use the shared dispatcher, so XPath/JSONPath scalar links are no longer hard-coded to CSS.

The scalar dispatcher handles explicit @regex: rules through the bounded capture-group evaluator and routes CSS/XPath/JSONPath through their dedicated parsers. Explicit @js: remains fail-closed in selector fields unless a specific script-capable execution path is wired. Collection roots have a separate explicit path: CSS selectors use Jsoup, while a leading colon or @regex: selects the bounded regex collection evaluator.

## Acceptance gate for declaring Legado engine complete

1. Import a curated corpus of real Legado BookSource JSON exports and preserve unknown fields on round-trip.
2. Exercise CSS, XPath, JSONPath, regex, JavaScript and chained rules for search, detail, TOC and content using source-level fixtures.
3. Verify URL option semantics, headers, cookies, redirects, encoding and error handling against a local HTTP fixture server.
4. Verify multi-page search/TOC/content, chapter ordering, duplicate suppression and reading progress.
5. Run the automated suite and build the debug APK successfully.
6. Test representative real sources on an Android device. A passing unit suite alone does not establish real-source compatibility.


## Bounded regex collection increment (2026-11-16)

Search `bookList` and TOC `chapterList` now accept Legado-style leading-colon
regex collection rules, as well as explicit `@regex:` roots. The evaluator
preserves full matches in source order and supports bounded multi-stage
`&&` extraction. It enforces input, pattern, stage, match-count, output-size
and execution-time budgets, returning a timeout or unsupported-rule result
rather than silently treating an unsafe rule as CSS. Scalar regex extraction is
covered separately below; full upstream regex filter syntax and other AnalyzeRule
modes remain outside this increment.


### Bounded regex scalar extraction (2026-11-16)

- Added an explicit `@regex:` scalar path matching the pinned Legado `AnalyzeByRegex.getElement` behavior: intermediate `&&` stages concatenate full matches; the final stage returns group 0 and capture groups from its first match.
- The scalar path shares the regex input, pattern, stage, match, output and deadline limits with collection extraction.
- Book-info scalar fields can now use bounded regex in addition to CSS, XPath and JSONPath.
- This does not implement every upstream regex/filter suffix or JavaScript transformation.
