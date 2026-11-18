# Full upstream Legado runtime migration

> 长期用户需求与五大类完整验收清单：[docs/USER_REQUIREMENTS_AND_PARITY_CHECKLIST.md](USER_REQUIREMENTS_AND_PARITY_CHECKLIST.md)。每次继续开发前必须先读该清单并更新完成状态。

## Goal

Make Badcat's online book-source execution use the same implementation and semantics as the pinned Legado Android app, rather than extending the bounded in-house parser. The compatibility target is the upstream application at:

- Repository: https://github.com/LegadoTeam/legado
- Immutable revision: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- License: GPL-3.0

The source is checked out at `upstream/legado` as a Git submodule. Do not move the pin to a floating branch. Upgrade only through a reviewed pin change.

## What the source audit found

At this revision, the upstream app contains 2,078 files under `app/src/main`, including 1,078 Kotlin/Java source files. The online source pipeline is in the app layer, not in `:modules:book` or `:modules:rhino` alone.

The upstream execution path is:

1. `BookList` — search and explore list evaluation.
2. `BookInfo` — book detail parsing and source initialization.
3. `BookChapterList` — TOC extraction, pagination, ordering, deduplication and chapter metadata.
4. `BookContent` — chapter retrieval, content rules and replacement.
5. `AnalyzeRule` — CSS/JSoup, XPath, JSONPath, regex, JS, selector chains and rule data bindings.
6. `AnalyzeUrl` — URL interpolation, headers, cookies, request method/body, charset, timeout, redirects, retries, WebView and request options.
7. `JsExtensions` and the upstream Rhino runtime — JavaScript host APIs and per-source/shared script state.

These components depend on upstream source entities, Room persistence, cache/state services, application initialization, HTTP clients, WebView services, and coroutine context. Copying only `AnalyzeRule.kt`, adding Jsoup, or adding the two Android library modules would still not provide parity.

## Integration decision

Use the upstream Android application source as the behavior reference and migrate its source-execution slice in-process. Keep Badcat's existing UI as a host adapter, but remove its custom parser as the authority for BookSource execution.

Do not use the Legado HTTP API or ContentProvider as the final implementation: both require a separately installed Legado app and therefore do not satisfy a self-contained Badcat app.

The upstream `App` initialization and global services must be adapted explicitly. Do not instantiate upstream parser classes with fake/null `appCtx`, `appDb`, CookieStore, CacheManager or WebView dependencies.

## Staged implementation gates

### Gate 0 — immutable source and build baseline
- [x] Pin exact upstream commit and license.
- [x] Add the upstream source as a submodule.
- [x] Make CI check out the pinned source.
- [x] Verify the submodule commit and GPL license in CI.
- [ ] Record and verify upstream source and dependency checksums.

### Gate 1 — upstream model and runtime build
- [ ] Integrate upstream `BookSource`, rule entities, `BaseSource`, `BaseBook`, `Book`, `BookChapter`, and serialization converters without dropping fields.
- [ ] Integrate the exact upstream Rhino module and initialization.
- [x] Port the pinned upstream `RuleAnalyzer`, `AnalyzeByJSoup`, and `SourceHtmlParser` verbatim; add a production adapter and regression tests for the reported `@.w_txt li.4@textNodes` expression, legacy index ordering/exclusion, selector chains, and rule combination.
- [ ] Integrate the remaining XPath, JSONPath, regex and all other rule parser implementations at the upstream versions.
- [ ] Run original upstream unit tests relevant to the source engine.

### Gate 2 — request and host services
- [ ] Adapt `AnalyzeUrl` to Badcat's injectable HTTP transport while retaining upstream URL-option semantics.
- [ ] Provide source-scoped CookieStore, login state, script key/value storage, cache, WebView and DNS behavior.
- [ ] Preserve upstream request/response objects and callback behavior; do not replace them with string-only bindings.

### Gate 3 — full reading vertical slice
- [ ] Integrate upstream search/explore, detail, TOC and content pipeline as a coherent slice.
- [ ] Preserve TOC pagination, volume nodes, ordering, duplicate handling, VIP/pay metadata, contentBatch, replacements and chapter state.
- [ ] Adapt persistence and cache with migrations and deterministic tests.

### Gate 4 — Badcat UI adapter and acceptance
- [ ] Connect Badcat source catalog and reader screens to the upstream pipeline.
- [ ] Keep imported BookSource JSON round-trip lossless.
- [ ] Run source fixtures for CSS, XPath, JSONPath, regex, JS, mainJs/jsLib, login, cookies, POST, pagination and dynamic WebView.
- [ ] Run real-source Android end-to-end tests across a broad, diverse source set.
- [ ] Confirm search → detail → TOC → first chapter → next chapter on device.
- [ ] Only then describe source compatibility as equivalent to the pinned upstream revision.

## Current status

This branch includes the immutable upstream source baseline and an attempted integration of upstream JSoup rule evaluation into the shared dispatcher. The latest CI run `44385813513` is **failed** at `:integration-contract:test`: chained collection root extraction, legacy colon-separated index/exclusion semantics, and the Bixiange public source search fixture all fail. Fix these before extending parser coverage. This branch does **not** integrate the complete runtime and does not claim compatibility. The full five-category user scope and per-session requirements are tracked in `USER_REQUIREMENTS_AND_PARITY_CHECKLIST.md`.
