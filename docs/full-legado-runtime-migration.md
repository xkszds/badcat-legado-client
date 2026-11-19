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


## 2026-11-19 dependency audit update

The detailed source-level dependency audit is now recorded in [upstream-analyzer-integration-audit.md](upstream-analyzer-integration-audit.md). It confirms that the pinned `AnalyzeRule` and `AnalyzeUrl` classes are app-layer lifecycle components, not standalone parsers: they depend on upstream entities, source-scoped Rhino state, `JsExtensions`, HTTP/cookie/cache/WebView services, coroutine context and batch persistence. The current JVM module compiles selected parser files only, while the app's per-stage CSS services and JSON staging model remain separate.

CI run [44550830295](https://github.com/xkszds/badcat-legado-client/actions/runs/44550830295) for head `6e1ffd1e513cc2e77581c889822e28e54f63558a` completed successfully, including tests/build and best-effort live source probes. This does not change the incomplete runtime status or satisfy device validation.

**Next gate:** integrate the upstream model/rule entity graph and serialization first. Do not copy `AnalyzeRule.kt`/ `AnalyzeUrl.kt` into the host until their real dependencies and service contracts are integrated.


## 2026-11-19 session ledger

- [x] Re-read root `AGENTS.md`, both product/engine checklists, project completion checklist, UI map, PPRule evidence and original purification evidence before continuing.
- [x] Completed the pinned source/rule model dependency audit and recorded it in `upstream-model-serialization-audit.md`.
- [ ] Model integration remains open: no official runtime entities, official Gson converters or Room-backed runtime source model have been integrated yet.
- [ ] Next: implement the Android-side model/serialization adapter with exact upstream behavior and fixtures; preserve raw JSON in parallel. Re-run the complete pre-change compatibility search before editing.
- Latest head CI run [#44554459580](https://github.com/xkszds/badcat-legado-client/actions/runs/44554459580) is pending. Earlier run [#44553806204](https://github.com/xkszds/badcat-legado-client/actions/runs/44553806204) passed contract/Rhino tests and Debug APK build/upload, while the final uploaded-corpus live step was still in progress.


## 2026-11-19 first upstream rule-model code slice

- Vendored the exact pinned source-rule entities and `GsonExtensions.kt` into the Android module, preserving their upstream package names and behavior.
- Added Android-side test coverage for six nested BookSource rule types, object-form and legacy string-encoded JSON, null/default behavior and selected rule-specific semantics.
- Added Parcelize plugin and Flexbox dependency matching pinned upstream versions. GPL-3.0 provenance is recorded in `THIRD_PARTY_NOTICES.md`.
- This is only the rule-model/JSON foundation. No claim is made that BookSource/BaseSource, Room converters, AnalyzeRule/AnalyzeUrl or the complete reading lifecycle are integrated.
- CI for the new code commit must be inspected before this increment is considered verified.


## 2026-11-19 verified model-slice CI and next Legado gate

- [x] CI run [44556688812](https://github.com/xkszds/badcat-legado-client/actions/runs/44556688812), head `8b9d07e617fe7b2469af70aaa17ee28e79c7a0f2`: contract unit tests passed; bounded Rhino host tests passed; Debug APK assembly passed and the APK artifact was uploaded.
- [ ] Workflow live-source smoke steps were still running at the time of this ledger update. Do not report the whole workflow as completed, and do not treat a CI APK as final complete-Legado release.
- [x] Re-read pinned upstream `BookSource.kt` and `BaseSource.kt`: BookSource is a Room/Parcelable entity implementing BaseSource; BaseSource extends the 1,390-line JsExtensions API and carries source lifecycle, login, cookie, script and cache behavior.
- [ ] Next gate is the full BaseSource/JsExtensions host-service dependency map. Do not copy BookSource with a fake BaseSource or no-op methods. Map every dependency to an actual host service or exact upstream unsupported/error semantics, then vendor the upstream entity graph and converters with compatibility tests.
- [ ] Existing host BookSource/ReadingEngine DTOs remain projections; raw JSON retention is still required for lossless import/export. They are not upstream runtime entities.


## 2026-11-19 JsExtensions declaration audit

A method-name comparison against the pinned upstream `JsExtensions.kt` is recorded in [upstream-js-extensions-compatibility-audit.md](upstream-js-extensions-compatibility-audit.md). It identifies 18 upstream method names absent from the current host bridge and a blocking `getSource()` return-type collision, plus the incomplete batch-context contract. Same-name methods are not yet considered behaviorally compatible. The next task is to complete the overload/semantic matrix and migrate the existing bridge around real host services before implementing upstream BaseSource/BookSource.


## 2026-11-19 complete JsExtensions inheritance audit

The declaration-level audit now includes all 122 pinned `JsExtensions` methods and all 30 inherited `JsEncodeUtils` methods. The current host bridge has 18 missing direct method names plus 19 missing inherited crypto method names. It also has incompatible return types for `getSource()`, batch context, Jsoup response wrappers and file handles. See [upstream-js-extensions-compatibility-audit.md](upstream-js-extensions-compatibility-audit.md). These differences are recorded as blockers, not accepted compatibility exceptions. The next code change must address a bounded, testable slice of this matrix and re-audit old callers; do not integrate BookSource on top of the current incompatible bridge.

## 2026-11-19 session gate — crypto dependency audit before implementation

Before changing runtime code, the mandatory checklist now records a Legado-only task to resolve the pinned upstream crypto dependency graph and re-audit the host bridge. The next bounded investigation is to locate the exact Hutool/dependency declarations used by upstream revision 330e606c4d57e08adc056ff8ab7a4132c784da9, inspect the pinned SymmetricCryptoAndroid, AsymmetricCrypto, and Sign contracts, and search every existing host helper/caller/test. No crypto method may be mapped to the current AES/DES-CBC-only helper unless its complete upstream semantics match. The current-head CI run 44558528157 is pending and must be inspected before treating the prior documentation increment as verified. This entry is a pre-work gate, not a claim that dependency or runtime work is complete.

## 2026-11-19 crypto dependency audit findings and next gate

The pinned upstream dependency coordinates are now verified: Hutool Crypto 5.8.22, Bouncy Castle provider 1.86, and Bouncy Castle PKIX 1.86. The host app currently has none of these dependencies. The exact upstream `SymmetricCryptoAndroid`, `AsymmetricCrypto`, `Sign`, and `JsEncodeUtils` crypto factory/legacy method contracts have been inspected and recorded in `upstream-js-extensions-compatibility-audit.md`.

The host's current `createSymmetricCrypto` returns an AES/DES-CBC-only host facade, while the upstream factory returns a Hutool `SymmetricCrypto` backed by `SymmetricCryptoAndroid`. Existing runtime tests exercise AES and DES helper paths; the runtime binds the source-scoped helper object as `java`. Existing JavaImporter tests also enforce that general `Packages.javax.crypto` and arbitrary Java classes remain unavailable. These are mandatory regression/security constraints.

**Next gate:** perform a complete repository grep for the factory/helper and all crypto API names before changing signatures. Then add the exact pinned dependencies and crypto classes, expose the upstream factories through the source-scoped `java` bridge only, preserve upstream legacy method quirks, and add Rhino/API/security regression tests. No runtime crypto implementation is claimed complete by this audit.
