# Legado compatibility matrix

Updated: 2026-11-16

This matrix describes the independent Badcat client implementation, not the full
upstream Legado application. "Partial" means the behavior is intentionally bounded
and must not be treated as full BookSource/AnalyzeRule compatibility.

| Area | Current state | Supported behavior | Known gaps / acceptance work |
|---|---|---|---|
| BookSource JSON | Partial | Single object or array import; bounded strict UTF-8; required URL/name validation; known-field type validation; duplicate source URL rejection; raw JSON retained so unknown fields survive export; enabled flag edits preserve unknown fields | Validate more upstream field types and source schema variants against a real-world fixture corpus |
| Source management | Partial | Imported sources can be stored, enabled/disabled, selected and exported | Verify bulk import/export and persistence on device; improve source diagnostics |
| Search URL | Partial | Absolute/relative HTTP(S), page tokens, URL options for method/headers/body/timeout/retry/redirects/charset; bounded query encoding; pure @js and <js> rules with string-only key/page/result bindings | Full Legado java/http/cookie/cache/WebView objects, webView, dnsIp, resolveIp, type, URL-option js/bodyJs and login/session state are not supported |
| HTTP transport | Partial | Async OkHttp, redirects with HTTPS downgrade prevention, per-source cookie jar, headers, common methods, response size cap, charset, retry | Legado DNS overrides, WebView requests, advanced request body and cookie semantics need coverage |
| Search result fields | Partial | CSS or bounded regex collection for bookList; CSS/XPath/JSONPath scalar extraction dispatch; relative book/cover URL resolution | Collection root remains CSS-only; JS, regex, filters, chained rules, interpolation and direct-detail detection are not supported |
| Book detail | Partial | Name/author/intro/cover/latest chapter/TOC URL; CSS/XPath/JSONPath scalar extraction | JS, regex, replacement chains, variables and source-specific init scripts are not supported |
| TOC | Partial | CSS or bounded regex chapter collection; CSS/XPath/JSONPath scalar title, chapter URL and next-page link extraction; multi-page pagination and cycle guards | Collection root remains CSS-only; volume grouping, VIP/pay and script formatting are not implemented |
| Chapter content | Partial | CSS/XPath/JSONPath extraction, title and next-page URL; HTML-to-readable-text conversion; pagination and replacement hook | JS/webJs/callBackJs, sourceRegex, replaceRegex, payAction, imageDecode and contentBatch are not implemented |
| Selector expressions | Partial | Explicit @css:, @xpath:, @jsonpath: / @json:; unprefixed CSS default; unambiguous XPath and JSONPath inference | Complete upstream expression grammar, selector filters and mixed parser chains are not implemented |
| JavaScript | Partial | Pinned Rhino sandbox for bounded string-only script evaluation; URL @js/<js> supports key/page/result strings; replacement helper | Full Legado Java/http/cookie/cache/WebView bindings, source mainJs/jsLib and stage-specific AnalyzeRule execution are not integrated |
| Regex | Partial | Bounded regex replacement plus staged regex collection roots for search and TOC | Regex scalar-field extraction and full AnalyzeRule regex chains are not yet connected |
| Replace rules | Partial | Scope, include/exclude matching, order, literal/regex replacement, optional JS evaluator hook | Not equivalent to all upstream Legado replace-rule and source-specific replacement behavior |
| Reader CSS profiles | Implemented | Multiple named profiles, notes, child selector rules, import/export, preview, sanitization and limits | Requires device UI/reader regression testing |
| Reading flow | Partial | Search → detail → TOC → chapter content → next-page aggregation → reading progress | Real-source corpus, live network behavior and physical-device acceptance remain outstanding |

## Parser dispatch increment (2026-11-16)

The shared rule-expression dispatcher now routes scalar extraction fields to CSS,
XPath or JSONPath based on explicit prefixes or unambiguous expression shape.
It is wired into search item fields, book detail fields, TOC chapter fields and
chapter content/title fields. The selector-stage adapter uses the same dispatcher. TOC next-page and chapter next-content links also use the shared dispatcher, so XPath/JSONPath scalar links are no longer hard-coded to CSS.

The dispatcher deliberately fails closed for explicit @js: and @regex: forms. It
does not infer JavaScript or regex from arbitrary text. Search bookList, TOC
chapterList, and pagination link collection remain CSS element-selection paths
because they require node collections, not scalar values.

## Acceptance gate for declaring Legado engine complete

1. Import a curated corpus of real Legado BookSource JSON exports and preserve unknown fields on round-trip.
2. Exercise CSS, XPath, JSONPath, regex, JavaScript and chained rules for search, detail, TOC and content using source-level fixtures.
3. Verify URL option semantics, headers, cookies, redirects, encoding and error handling against a local HTTP fixture server.
4. Verify multi-page search/TOC/content, chapter ordering, duplicate suppression and reading progress.
5. Run the automated suite and build the debug APK successfully.
6. Test representative real sources on an Android device. A passing unit suite alone does not establish real-source compatibility.
