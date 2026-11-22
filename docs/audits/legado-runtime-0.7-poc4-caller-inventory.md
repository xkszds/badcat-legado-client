# Runtime-0.7 PoC-4 — pinned caller inventory and source-slice gate

Date: 2026-11-20

## Scope and immutable baselines

- Badcat starting branch: `integration/legado-upstream-runtime`
- Badcat starting HEAD: `6aad7e17f5f50f8eee983c32a24806e3c25b2041`
- Legado behavior baseline: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Read first: `docs/required/00-MASTER-WORKFLOW.md` and `docs/required/03-LEGADO-RUNTIME.md`.
- This PoC-4 task is an audit and registration gate only. No runtime code, production callers, Gradle, database, UI, Reader, or `main` changes are authorized by this report.

## Exact pinned-tree inventory

Git tree API at the immutable Legado SHA returned `truncated=false`:
- 3,298 tree entries total.
- 1,675 Kotlin/Java source files across the repository.
- 1,078 Kotlin/Java files under `app/src/main`.
- All 12 prior PoC-3 seed classes resolve to one exact source path each:

| Class | Exact pinned path |
|---|---|
| BaseSource | `app/src/main/java/io/legado/app/data/entities/BaseSource.kt` |
| JsExtensions | `app/src/main/java/io/legado/app/help/JsExtensions.kt` |
| SharedJsScope | `app/src/main/java/io/legado/app/model/SharedJsScope.kt` |
| JsSourceEngine | `app/src/main/java/io/legado/app/model/jsSource/JsSourceEngine.kt` |
| AnalyzeRule | `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt` |
| AnalyzeUrl | `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt` |
| CookieStore | `app/src/main/java/io/legado/app/help/http/CookieStore.kt` |
| CacheManager | `app/src/main/java/io/legado/app/help/CacheManager.kt` |
| SourceLoginJsExtensions | `app/src/main/java/io/legado/app/ui/login/SourceLoginJsExtensions.kt` |
| SourceVerificationHelp | `app/src/main/java/io/legado/app/help/source/SourceVerificationHelp.kt` |
| LoginUiV2 | `app/src/main/java/io/legado/app/model/login/LoginUiV2.kt` |
| BaseSourceExtensions | `app/src/main/java/io/legado/app/help/source/BaseSourceExtensions.kt` |

## Caller/implementer search inventory

Repository-scoped GitHub code-reference searches returned these result counts. The matching source links identify the pinned commit SHA above. Counts include implementation, caller, tests and documentation matches; they are not all production callers.

| Symbol query | Results | Search cap reached |
|---|---:|---|
| JsExtensions | 44 | no |
| BaseSource | 50 | no |
| AnalyzeRule | 92 | no |
| AnalyzeUrl | 65 | no |
| CookieStore | 19 | no |
| CacheManager | 43 | no |
| JsSourceEngine | 12 | no |
| SharedJsScope | 13 | no |
| SourceLoginJsExtensions | 11 | no |
| SourceVerificationHelp | 7 | no |
| BookSource | 100 | yes |
| BookChapter | 100 | yes |

Narrower follow-up searches:
- `BookSource bookSource:`: 37 matches.
- `BookChapter(`: 53 matches.
- `import io.legado.app.data.entities.BookSource` and `import io.legado.app.data.entities.BookChapter` still hit the 100-result cap.
- Therefore the runtime-service caller map below is substantially mapped, but the full model/entity access inventory is **not exhaustive**. Do not describe this audit as a completed exhaustive whole-app call graph.

## Verified caller and implementer map

| Contract | Pinned implementations / direct callers verified from source |
|---|---|
| BaseSource construction and JS binding | `App.kt` registers `BookSource`, `RssSource` and `HttpTTS` with `NativeBaseSource.factory`; `BookSource` and `RssSource` implement `BaseSource`; `HttpTTS` extends it. `NativeBaseSource.kt` supplies the Rhino wrapper. |
| BaseSource service and lifecycle | `SourceHelp.kt` resolves and mutates source records through `appDb.bookSourceDao`; `BaseSourceExtensions.kt` delegates shared scope and source-type behavior; `BookSourceExtensions.kt`, `SourceCallBack.kt`, `JsSourceBook.kt`, `JsSourceConfig.kt` and login/view-model paths consume source behavior. |
| JsExtensions implementations | `BaseSource`, `JsSourceEngine`, `AnalyzeRule`, `AnalyzeUrl`, `WebJsExtensions`, `RssJsExtensions` and `SourceLoginJsExtensions` implement the interface. `RegexJsExtensions` is a separate `JsEncodeUtils` consumer, not another `JsExtensions` implementation. |
| Search/detail/TOC/content path | `JsSourceBook.kt` dispatches source JS functions; `BookList.kt` uses `AnalyzeRule`/`AnalyzeUrl` for discovery/search results; `BookInfo.kt` uses `AnalyzeRule` for detail; `BookChapterList.kt` uses both for TOC; `BookContent.kt` uses both for content and batch content. |
| Additional AnalyzeRule/AnalyzeUrl consumers | `BookCover.kt`, `DictRule.kt`, `ReviewRuleParser.kt`, web-book parsers, `Rss.kt`, `VideoPlay.kt`, `ReadBookActivity.kt` and web/browser paths also consume parser/network behavior. These are separate product surfaces; the book-source path must be preserved without importing unrelated UI or media products wholesale. |
| Shared JS scope | `BaseSourceExtensions.kt`, `BaseSource.kt`, `JsSourceEngine.kt`, `JsSourceConfig.kt`, `AnalyzeRule.kt`, `AnalyzeUrl.kt`, `JsSourceUpsert.kt`, source editors and MCP source update paths call or invalidate `SharedJsScope`. Its identity, invalidation and persistence semantics cannot be reduced to a per-call temporary scope. |
| Cookie/session | `CookieManager.kt` wraps `CookieStore` and `CacheManager`; `HttpHelper.kt`, `BaseSource.kt`, `JsSourceEngine.kt`, `JsExtensions.kt`, `AnalyzeUrl.kt`, `AnalyzeRule.kt`, `BackstageWebView.kt`, `WebViewLoginFragment.kt` and `Restore.kt` participate in cookie/session transfer or restoration. |
| Cache | `BaseSource.kt`, `JsSourceEngine.kt`, `JsExtensions.kt`, `AnalyzeRule.kt`, `AnalyzeUrl.kt`, `CookieManager.kt`, `BackstageWebView.kt`, `BookCover.kt` and unrelated app features use `CacheManager`. Its broad consumers mean a host adapter must partition source runtime cache semantics from unrelated product caches without changing TTL/key behavior. |
| Login | `SourceLoginJsExtensions.Callback` is implemented by `SourceLoginDialog` and `ExploreAdapter`; it is also constructed by `SourceCallBack`, `BookInfoViewModel`, `ReadBookActivity` and `VideoPlayerViewModel`. `SourceLoginV2Delegate` calls upstream `LoginUiV2` render/action parsing and field resolution. |
| Verification/browser | `SourceVerificationHelp` is invoked from `JsExtensions`; `WebViewModel`, `WebViewActivity`, `VerificationCodeActivity` and `VerificationCodeDialog` attach, complete or return verification results. This is a real asynchronous UI/WebView lifecycle, not a simple callback returning a string. |
| WebView JS bridge | `BackstageWebView`, `WebViewPool`, `WebViewActivity`, `BookInfoActivity`, `ReadRssActivity`, `VideoPlayerActivity` and `BottomWebViewDialog` install or consume `WebJsExtensions`/source JS interfaces. |

## Lowest-coupling authentic slice found

The pinned `RuleAnalyzer.kt` is pure Kotlin: its file has no imports beyond its package declaration and has no Android, database, network, UI, Rhino or persistence callback dependency. It is already compiled from the original upstream source by `integration-contract/build.gradle.kts`'s `pinnedLegado` source set, alongside authentic `AnalyzeByJSoup.kt`, `AnalyzeByRegex.kt` and `SourceHtmlParser.kt`. Existing `PinnedLegadoRuleAnalyzerParityTest` and `PinnedLegadoRuleAnalyzerTest` compare host entry-point output with the directly compiled upstream class.

This proves a small authentic rule-tokenization/parser slice is viable. It does **not** prove a service-port prototype, the full `AnalyzeRule` engine, JS ABI, HTTP/session, login, WebView or full source runtime. Re-registering this same parser as a new PoC would be redundant and must not be represented as progress on complete Legado compatibility.

## Architecture consequence and next gate

1. Keep the already-pinned parser slice as a reference/test fixture; do not widen it and call it the runtime.
2. `AnalyzeRule`/ `AnalyzeUrl` are the next behavior-bearing boundary, but their contracts reach BaseSource, JS engine/scope, Book/BookChapter/BatchContentContext, cache, CookieStore, HTTP and WebView callbacks. A source-only compile attempt must not be expanded until all those dependencies and side effects have exact owners.
3. CookieStore/CacheManager/SharedJsScope cannot be independently ported as simplistic in-memory interfaces; their upstream Room/file/memory/WebKit behavior and lifecycle need explicit ownership and differential fixtures.
4. Login and verification require Badcat-owned UI/WebView hosts that preserve upstream V1/V2 state/action semantics, callbacks, cancellation, timeout and thread behavior.
5. Continue the model/entity access scan with bounded queries and exact pinned file verification. BookSource/BookChapter query results are capped; do not claim complete caller coverage yet.
6. Only after that scan, register a separate PoC-5 source slice with exact upstream files, all imports/callers, typed service ports, tests, exclusions and compiler target before any code changes.

## Result

- Completed: exact pinned tree inventory; seed path verification; source-index caller/implementer mapping for the ten runtime service symbols; direct verification of the principal book-source search/detail/TOC/content path and key login/cookie/cache/scope callers; identification of the already-integrated pure upstream parser slice.
- Incomplete: exhaustive BookSource/BookChapter/model access graph; exhaustive method-level call graph for every JS extension overload; bytecode descriptor audit; service-port implementation; compiler-complete authentic BookSource/BaseSource runtime boundary.
- Changes in this task: audit documentation only. No Kotlin/Java, Gradle, database, persistence, UI/Reader, production caller, test, APK or `main` change.
- Acceptance: PoC-4 is a useful caller inventory checkpoint, **not** an exhaustive call graph and **not** a Runtime-0.7 or full-compatibility pass. PoC-1 compiler boundary remains failed; PoC-2 architecture remains unselected; PoC-3 method contract remains audit-only.


## Paginated entity-symbol reference inventory supplement

The first report version treated the convenience search's 100-result limit as a remaining entity-scan blocker. That limit was removed by using the GitHub code-search API's `page` parameter against the same pinned repository snapshot.

| Token | GitHub total_count | Page counts | Unique paths | `app/src/main/java` paths | `incomplete_results` |
|---|---:|---|---:|---:|---|
| `BookSource` | 382 | 100 / 100 / 100 / 82 | 382 | 141 | false |
| `BookChapter` | 154 | 100 / 54 | 154 | 92 | false |

The ten service-contract token searches also returned complete first-page result sets with `incomplete_results=false`: JsExtensions 44, BaseSource 50, AnalyzeRule 92, AnalyzeUrl 65, CookieStore 19, CacheManager 43, JsSourceEngine 12, SharedJsScope 13, SourceLoginJsExtensions 11, SourceVerificationHelp 7.

Interpretation:
- This closes the lexical source-file inventory for the two high-volume entity tokens and the ten service-contract class tokens in the pinned GitHub code-search index.
- It does not automatically classify every match as a runtime caller; each set contains definitions, tests, docs, reader/RSS/audio/video/product callers and API tooling. The report's verified caller matrix classifies the principal production paths.
- It is still not a compiler-level semantic call graph. Kotlin reflection, dynamic JavaScript method names, user-supplied JS libraries/rules and generated symbols require separate ABI/fixture/lifecycle coverage.
- Accordingly, the model/entity **textual reference scan is now complete in the pinned code-search index**; method-level dynamic JS API call coverage and source-level service dependency closure remain open.
