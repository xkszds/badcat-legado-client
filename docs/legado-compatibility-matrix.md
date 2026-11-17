# Legado compatibility matrix

Updated: 2026-11-16

This matrix describes the independent Badcat client implementation, not the full
upstream Legado application. "Partial" means the behavior is intentionally bounded
and must not be treated as full BookSource/AnalyzeRule compatibility.

| Area | Current state | Supported behavior | Known gaps / acceptance work |
|---|---|---|---|
| BookSource JSON | Partial | Single object or array import; strict UTF-8; 5 MiB, 2,000-source, 64-level nesting and 512-field-per-source limits; known metadata/rule type checks; duplicate URL rejection; unknown-field-preserving export and enabled edits | Validate remaining upstream fields against a real-world source corpus; source runtime is still partial |
| Source management | Partial | Imported sources can be stored, enabled/disabled, selected, searched, explored and exported | Verify bulk import/export and persistence on device; improve source diagnostics |
| Search URL | Partial | Absolute/relative HTTP(S), page tokens, URL options for method/headers/body/timeout/retry/redirects/charset; bounded query encoding; pure @js and <js> URL expressions with string-only key/page/result bindings | Full Legado java/http/cache/WebView objects, webView, dnsIp, resolveIp, type and complete URL-option js/bodyJs behavior are not supported |
| HTTP transport | Partial | Async OkHttp, redirects with HTTPS downgrade prevention, per-source cookie jar, durable app-private cookie persistence, headers, common methods, response size cap, charset, retry; Rhino java.getCookie(url) and java.getCookie(url, key) read cookies scoped to the current source and matching URL | Cookie set/replace/remove APIs, WebView cookie synchronization, Legado DNS overrides, WebView requests and advanced request-body semantics are not implemented; authenticated real-source validation remains required |
| Explore | Partial | Dedicated `exploreUrl` transport, separate `enabledExplore` gate and `ruleExplore.bookList` parsing through the same bounded search-card parser; exposed as `ReadingEngine.explore` and a per-source discovery action in source management | Explore filters, category discovery navigation, source-specific scripts and real-source fixtures remain outstanding |
| Search result fields | Partial | CSS or bounded regex collection for bookList; CSS/XPath/JSONPath and bounded regex scalar extraction; relative book/cover URL resolution | Regex and collection limits apply; JS, filters, interpolation and direct-detail detection are not supported |
| Book detail | Partial | Name/author/intro/cover/latest chapter/TOC URL; CSS/XPath/JSONPath and bounded regex scalar extraction | JS, replacement chains, variables and source-specific init scripts are not supported |
| TOC | Partial | CSS or bounded regex chapter collection; CSS/XPath/JSONPath and bounded regex scalar title, chapter URL and next-page link extraction; multi-page pagination and cycle guards | Regex budgets apply; volume grouping, VIP/pay and script formatting are not implemented |
| Chapter content | Partial | CSS/XPath/JSONPath content extraction; CSS/XPath/JSONPath and bounded regex title/next-page URL; HTML-to-readable-text conversion; pagination and replacement hook | Regex chapter-body transforms, JS/webJs/callBackJs, sourceRegex, replaceRegex, payAction, imageDecode and contentBatch are not implemented |
| Selector expressions | Partial | Explicit @css:, @xpath:, @jsonpath: / @json:; upstream @@ forced-default JSoup mode; default-mode chained @ selectors; bounded legacy children/class/tag/id/text heads and numeric index suffixes; unprefixed CSS default; unambiguous XPath and JSONPath inference | Full ElementsSingle index/range grammar, selector filters, all parser-mode chains and full AnalyzeRule semantics are not implemented |
| JavaScript | Partial | Pinned Rhino sandbox for bounded string-only script evaluation; URL @js/<js> supports key/page/result strings; replacement helper; execution timeout maps to RULE_TIMEOUT | Full Legado Java/http/cookie/cache/WebView bindings, source mainJs/jsLib and stage-specific AnalyzeRule execution are not integrated |
| Regex | Partial | Bounded regex replacement; staged collection roots for search/TOC; scalar capture-group extraction for search, detail, TOC and chapter title/next-page fields | Full upstream regex filter suffixes and every AnalyzeRule combination are not implemented |
| Replace rules | Partial | Scope, include/exclude matching, order, literal/regex replacement, optional JS evaluator hook | Not equivalent to all upstream Legado replace-rule and source-specific replacement behavior |
| Reader CSS profiles | Implemented | Multiple named profiles, notes, child selector rules, import/export, preview, sanitization and limits | Requires device UI/reader regression testing |
| Reading flow | Partial | Search → detail → TOC → chapter content → next-page aggregation → reading progress; parsed chapter cache with 12-hour freshness and stale offline fallback | Real-source corpus, live network behavior and physical-device acceptance remain outstanding |

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


### Search source enablement guard (2026-11-16)

- Search service rejects disabled sources before calling the HTTP gateway.
- A dedicated contract test verifies the disabled path makes zero transport requests and the enabled path still parses results and resolves relative book URLs.

### Durable per-source cookie persistence (2026-11-16)

- Android's default OkHttp source client now uses an app-private SharedPreferences cookie jar keyed by the source ID.
- Cookie name/value, domain, path, expiry, host-only, secure and HttpOnly attributes are restored across process restarts; source jars remain isolated.
- Android backup is disabled for the app, so the cookie preference file is not included in app backup.
- This does not implement Legado login UI, loginCheckJs, JavaScript cookie APIs, WebView cookies or full upstream cookie semantics. Validate against representative authenticated sources on-device.


### Parsed chapter cache (2026-11-16)

- The default Android engine stores parsed and purified chapter text in an app-private disk cache keyed by source, book and chapter.
- The cache is bounded to 64 MiB total and 4 MiB per chapter. Fresh entries (12 hours) avoid a network request; expired entries are used only as fallback when the network request fails.
- Cache write failures do not block successful reading. Cache behavior is covered by contract tests for fresh hits and offline fallback.
- This cache does not yet replace upstream Legado's complete Room cache, batch prefetch or cache-management UI.


### Recursive mixed selectors and JSONPath collection roots (2026-11-16)

- CSS, XPath and JSONPath scalar evaluators now use the recursive list-combination evaluator for mixed nested `&&`, `||` and `%%` expressions; regression tests cover mixed nesting.
- Search/explore collection roots now accept JSONPath expressions (including `@jsonpath:` and `@json:`) and serialize each selected JSON value as an independent fragment.
- Search result fields are evaluated against the selected fragment through the shared CSS/XPath/JSONPath dispatcher, so JSONPath bookList items can use JSONPath name, URL and author fields instead of being incorrectly parsed as HTML.
- This remains a bounded adapter: full upstream AnalyzeRule interpolation, JavaScript, chained transforms and every source-specific suffix are not claimed as complete.


### JSONPath script-marker disambiguation (2026-11-16)

- Script detection now requires the explicit `@js:` marker. It no longer mistakes `@jsonpath:` / `@json:` rules for JavaScript merely because the marker shares the `@js` prefix.
- The same guard is applied to search/explore, TOC, book-info and chapter-content parser paths.


## Login status check action (2026-11-17)

- The source catalog now exposes a dedicated **检查登录状态** action for every imported source that declares `loginCheckJs`, including sources that do not use Login UI v2.
- The action executes the source's `loginCheckJs` through the bounded per-source Rhino runtime and reports the boolean result or a typed error.
- Login UI v2 form/action handling remains a separate capability; this button does not claim to perform a login or infer authentication from arbitrary `loginUrl` content.
- The source-level service tests already cover true/false boolean parsing and rejection of non-boolean return values. Device validation against authenticated real sources remains required.


## Source authentication reset (2026-11-17)

- Source management provides an explicit confirmation action to clear one source's persisted login header, login info, and HTTP cookies.
- Cookie deletion is keyed to the source identity and synchronized with other cookie-jar instances, so subsequent requests reload the empty store rather than resurrecting an older in-memory snapshot.
- Script key/value storage and the source definition are intentionally retained; clearing authentication does not remove a source or unrelated source data.
- Unit coverage verifies that clearing one source's login header and login info leaves another source's authentication state untouched.


## Source-scoped cookie helper

- The pinned Legado JsExtensions contract exposes `java.getCookie(tag)` and `java.getCookie(tag, key)` for reading a cookie string or one cookie value.
- The host now implements these two read helpers through the current book source's persistent OkHttp cookie jar. URL matching retains OkHttp domain, path, expiry, and secure-cookie rules.
- Cookie lookup is isolated by source ID; a script cannot read cookies stored by a different book source. Invalid or non-HTTP(S) URL inputs return an empty string.
- Cookie mutation remains handled by the request/response cookie jar. The wider upstream CookieStore API is not exposed as a permissive global database API.


### Source-scoped JavaScript cookie lookup (2026-11-17)

- The pinned upstream JsExtensions.getCookie(tag) and getCookie(tag, key) signatures were checked. The bounded Rhino host now exposes both forms as java.getCookie(url) and java.getCookie(url, key).
- Cookie lookup reads the current source's persisted OkHttp cookie jar, applies OkHttp URL domain/path/secure matching, and never reads another source's jar.
- The key overload preserves values containing additional '=' characters and returns an empty string when the key is absent.
- This is read-only cookie lookup. Upstream CookieStore.setCookie, replaceCookie, and removeCookie plus WebView cookie behavior are not claimed as implemented.


### Upstream forced-default CSS mode (2026-11-17)

- Checked the current upstream `AnalyzeRule.SourceRule` implementation: a rule beginning with `@@` switches to the default JSoup mode and removes the two-character prefix before evaluation.
- The bounded dispatcher now resolves `@@selector` explicitly as CSS instead of treating it as a CSS selector containing an invalid leading at-sign.
- Scalar CSS extraction and collection extraction are covered by contract tests. Collection rules retain the upstream element-index suffix handling; explicit `@CSS:` continues to use the direct selector path.
- This is one additional AnalyzeRule syntax slice only. It does not complete every `ElementsSingle` feature, arbitrary nested parser-mode changes, or all-in-one regex and WebJS.


### Default-mode JSoup chains and legacy selector heads (2026-11-17)

- The upstream `AnalyzeByJSoup.getResultList` splits default-mode rules on `@`, evaluates each selector step, then extracts the final text/attribute mode. `ElementsSingle` also accepts legacy `children`, `class.name`, `tag.name`, `id.name` and `text.value` selector heads with numeric index suffixes.
- The bounded CSS adapter now evaluates chained scalar forms such as `div.book@a.title.0@href` and `@@class.book@tag.a.0@href`; each stage uses the bounded index parser and a final extraction suffix.
- The tokenizer ignores at-signs inside quoted CSS attribute values, brackets and parentheses. Regression tests cover chained selectors, legacy selector heads, indexes and quoted `@cover` attribute values.
- This increment does not yet implement every upstream `ElementsSingle` feature, arbitrary nested parser mode changes, full `@get` / `{{}}` semantics across all stages, or all-in-one regex and WebJS.


### Embedded URL variables and persistent source values (2026-11-17)

- Embedded URL templates now resolve `@put:{...}` JSON objects before `@get:{key}` tokens and `{{...}}` script expressions.
- `@put` parsing tracks JSON string quoting, escapes and balanced braces, so a valid string value containing literal `{` or `}` is not truncated by a delimiter regex. Incomplete directives remain literal.
- `@put` accepts a bounded JSON object whose values are strings; key/value lengths, operation count and total template size are limited. `@get` reads only the current source's persisted string store.
- The script context receives primitive strings for `result`, `baseUrl`, `sourceUrl` / `sourceId`, `sourceName`, `ruleName`, `key` and `page`. This does not expose the full upstream Java, HTTP, WebView, cache or Android objects.
- Contract tests cover both URL-template and source-runtime paths, including JSON string values containing braces. Full upstream variable semantics across every AnalyzeRule stage remain unclaimed.
