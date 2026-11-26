# Runtime-3.0 Phase B path-exact implementation addendum

**Task:** `R3-S0-T026-PHASE-B-ADDENDUM-FREEZE-026L`
**Date:** 2026-11-25
**Status:** original design freeze completed; read-only snapshot refreshed after Tasks 026M–026O and the Runtime POC quarantine review. Phase B has started only with the bounded 026M UI-model ownership slice; BookSource/BaseSource migration and Host Port production wiring remain NO-GO.
**Badcat branch/original addendum HEAD:** `integration/legado-upstream-runtime` / `07d0819ddb3802fd2e2e17b3de75f965da9fcea2`
**Current refresh HEAD:** `4e51c9c210c33e3784070e8a557b8c3f4ab54b9c`
**Pinned behavior baseline:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## 1. Purpose and hard constraints

This addendum converts audit §§24.70–24.132 and Tasks 026K–026O evidence into an implementation-ready file/port/module/test map. It does not authorize edits. It is deliberately conservative where the source ABI or host contract is not proven: unresolved signatures are named blockers, not guessed interfaces.

The product remains Badcat. Keep its original UI, navigation, bookshelf, reader, CSS/layout, purification and interaction. Legado contributes the real pinned source model, rule evaluator, JS/Rhino, source lifecycle and search/explore/detail/TOC/content behavior. Do not substitute the current selector parser or a simplified `BookSource` DTO for the upstream implementation. Do not introduce no-op behavior, permissive fallback, fake data, generic `Map<String, Any?>` bridges, or claim compatibility from compilation alone.

Data strategy remains the Runtime-3.0 §2.3 rule: prove source execution closure first; do not freeze Room schema v1, migrate user data, dual-write or add a new database in this task. Existing Badcat file-backed source catalog remains the current host-owned store until a separately registered data-ownership decision.

## 2. Current implementation reality (read-only inspection)

### 2.1 Current project graph and module state

`settings.gradle.kts` includes `:app`, `:integration-contract`, `:legado-rhino`, `:legado-runtime` and `:legado-runtime-poc`. App depends on `:integration-contract`, `:legado-runtime` and `:legado-rhino`; it has no dependency edge to `:legado-runtime-poc`. The POC is retained only as quarantined historical evidence, not as a production Runtime dependency.

- `:legado-runtime` is an Android library (compileSdk 36, minSdk 26, Java/Kotlin 21, Kotlin/Parcelize 2.4.20). After Task 026M it owns three ordinary Kotlin production models under `src/main`: `RowUi.kt`, `ExploreKind.kt` and `FlexChildStyle.kt`. Its `KotlinCompile` task also injects eight pinned upstream files from the upstream app tree: seven rule DTOs (`BookListRule`, `ExploreRule`, `SearchRule`, `BookInfoRule`, `TocRule`, `ContentRule`, `ReviewRule`) plus `GsonExtensions.kt`. The current production source input is three checked-in models plus eight injected upstream files. It declares Gson 2.14.0 and AndroidX annotation 1.10.0; it does not declare Room database/compiler/KSP or the full upstream model/runtime dependency closure.
- `:legado-runtime-poc` is a separate probe build. Its build script injects 12 pinned upstream source files into its own Debug/Release compile tasks; `src/main` contains only an empty Android manifest and `source-closure-stage1.json` is historical metadata. Keep it quarantined and out of App/production dependencies; do not merge its source set with `:legado-runtime`.
- `:legado-rhino` is an Android library containing the Rhino bridge/sandbox implementation. It exports `org.htmlunit:htmlunit-core-js:5.3.0-legado.4` through `api`; it implements coroutines 1.11.0, OkHttp 5.4.0 and AndroidX collection 1.6.0. Preserve the exported CoreJS ABI.
- `:integration-contract` is currently a Kotlin/JVM 21 module but contains substantial production code, not merely interfaces: source services, request/rule parsers, compatibility helpers, tests, and a custom `pinnedLegado` source set. The latter compiles original upstream `AnalyzeByJSoup.kt`, `AnalyzeByRegex.kt`, `RuleAnalyzer.kt`, `SourceHtmlParser.kt` plus adapted JSONPath/XPath/Jsoup bridges and `PrintOnDebug.kt`.
- `:app` currently composes the actual `SourceReadingEngine` graph through `BadcatReadingEngineFactory`; it also constructs independent `RhinoSourceScriptRuntime` instances from `SourceCatalogActivity` and `SourceLoginUiV2Activity`. It owns `FileBookSourceCatalog`, SharedPreferences cookie/session/key-value adapters, file caches and the visible UI.
- `gradle/libs.versions.toml` is absent in this checkout. The current root and module build files use literal plugin/dependency versions. Do not create a version catalog as an incidental part of this task.

### 2.2 Current production path and gaps

The real production entry points are:

- `app/src/main/java/app/badcat/client/runtime/BadcatReadingEngineFactory.kt`
- `app/src/main/java/app/badcat/client/runtime/BadcatSourceHttpGatewayFactory.kt`
- `app/src/main/java/app/badcat/client/runtime/RhinoSourceScriptRuntime.kt`
- App callers `SourceSearchActivity.kt`, `SourceBookActivity.kt`, `SourceCatalogActivity.kt`, `SourceLoginUiV2Activity.kt`, and `BadcatPurificationActivity.kt`.

`BadcatReadingEngineFactory` creates `SourceReadingEngine` and the search/explore/detail/TOC/content services. The UI callers use `ReadingEngine` for source list, search, explore, detail, chapters and chapter content. The catalog utility and Login UI v2 activity separately construct Rhino runtimes. `SourceCatalogActivity` explicitly wires source invalidation to `clearSourceState`; the default `FileBookSourceCatalog` callback is empty, so every production catalog construction must be audited for source-state cleanup.

`RhinoSourceScriptRuntime` has per-source mutexes and process-shared scope/global/hash maps, bounded script execution and explicit `clearSourceState`. Its constructor still exposes many callback lambdas with throwing defaults for HTTP, cookie writes, Android ID and toast. The main factory wires a substantial subset using `runBlocking(Dispatchers.IO)` from synchronous JavaScript extension callbacks; the login activity wires a narrower subset. These are not yet one typed, lifecycle-owned host bridge.

`SourceRuntime.kt` declares `SourceRequestClient`, `SourceHttpRequest/Response`, `SourceScriptRuntime` and related contracts. `UnsupportedSourceScriptRuntime` intentionally fails closed. `RuntimeHostPort.kt` declares `HostUiPort`, `HiddenWebViewPort`, `VisibleBrowserPort`, `VerificationCodePort` and `ProductEventPort`; no production implementer or injection site was found for these five ports in the inspected App/Runtime trees. Their existence is not evidence that WebView, browser, verification or UI callbacks are wired.

`PinnedLegadoRuleAnalyzer` currently wraps upstream `RuleAnalyzer.splitRule` and bounds expression size/nesting; it is not the full upstream `AnalyzeRule` / `AnalyzeUrl` execution pipeline. The current CSS/JSONPath/XPath/selector helpers and `Source*Service` classes provide useful partial behavior but must not be represented as complete source-rule compatibility.

`FileBookSourceCatalog` stores complete imported source JSON through `AtomicFile` and maps it into the current `app.badcat.engine.BookSource` host DTO. That DTO is not the exact upstream `io.legado.app.data.entities.BookSource`; it must not remain the Runtime's authoritative source model. Task 026N selected the pinned upstream BookSource as the sole eventual Runtime source model while retaining the App AtomicFile catalog as current persistence owner. Complete raw JSON retention is useful migration evidence, not a substitute for exact model fields/defaults/serialization or runtime semantics.

### 2.3 Current app-only boundary

- `AnalyzeUrl.getGlideUrl` is used only by `OkHttpStreamFetcher`; Glide stays in App.
- `AnalyzeUrl.getMediaItem` is used by `AudioPlayService` and `ReviewDetailDialog`; Media3 stays in App.
- `FlexChildStyle.apply(View)` is called only by login/explore UI renderers; keep its data/defaults in Runtime and the Flexbox/View mutation in App.
- `SourceLoginJsExtensions` is a host login bridge and currently extends product `RssJsExtensions`; do not move the full RSS product class into Runtime. Split only the exact shared behavior proven necessary for login scripts.
- `ExploreAdapter` owns `LruCache<String, InfoMap>(99)` keyed by sourceUrl. Runtime callers read/write the same mutable InfoMap and the adapter persists only dirty entries from a snapshot. Preserve capacity, key, object identity, dirty flag, snapshot-save and invalidation semantics through a dedicated port.
- Room schema/database ownership remains App-side. Do not import `appDb`, DAO, AppConfig, `splitties.appCtx`, generated App `R`, Activity, View, AppCompat, lifecycle, RSS UI or Reader UI into Runtime.

## 3. Pinned upstream candidate file disposition (33/33)

The paths below are exact relative paths under `upstream/legado`. Each has one disposition. “COPY_AS_IS” means preserve the pinned source/API/field/overload behavior and dependencies; it does not mean bypass the module/API checks. “SPLIT” means preserve the source-visible core portion and move only the host side effect to a typed App adapter. “CONDITIONAL” is a blocker until the stated contract is frozen.

| # | Pinned source path | Disposition | Destination / reason / non-negotiable behavior |
|---:|---|---|---|
| 1 | `app/src/main/java/io/legado/app/data/entities/BaseSource.kt` | `ADAPT_WITH_PORT` | Runtime source lifecycle. Replace direct source/session/cache/HTTP/WebView/UI effects with typed ports; preserve source identity, enabled/login flags, execution state, callback ordering and per-source lifetime. |
| 2 | `app/src/main/java/io/legado/app/data/entities/BookSource.kt` | `CONDITIONAL` | Exact upstream Room/Parcelable model is required; Room annotations and Parcelize must be preserved. Runtime may compile the entity model, but schema/database/compiler ownership stays App-side and is not authorized by this addendum. Freeze model-vs-file-catalog mapping and Room annotation dependency before copying. |
| 3 | `app/src/main/java/io/legado/app/data/entities/BaseBook.kt` | `ADAPT_WITH_PORT` | Preserve exact base book/source contract; BigData and rule-variable access must be explicit typed ports. |
| 4 | `app/src/main/java/io/legado/app/data/entities/Book.kt` | `SPLIT` | Preserve the upstream model/fields/defaults/Parcelable/Room identity; extract `appDb`, ReadConfig, context and resource operations to BookRepository/RuntimeConfig/AppContext/Resource host adapters. No schema v1. |
| 5 | `app/src/main/java/io/legado/app/data/entities/BookChapter.kt` | `SPLIT` | Preserve chapter entity identity, ordering, metadata and big-data behavior; extract `appDb`, BigData, context and rule-variable access to ChapterRepository/BigData/RuleVariable ports. |
| 6 | `app/src/main/java/io/legado/app/data/entities/SearchBook.kt` | `SPLIT` | Preserve Room/Parcelable result fields; move generated `R` label/resource lookups and search-result persistence to App adapters. |
| 7 | `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt` | `ADAPT_WITH_PORT` | Core rule engine. Preserve branch order, CSS/JSONPath/XPath/regex/JS dispatch, URL-vs-content behavior, result types, cookie/cache/WebView fallback and exceptions. No selector-only replacement. |
| 8 | `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt` | `SPLIT` | Preserve URL parsing, request construction, response and body-stream semantics. Keep only Glide image adapter and Media3 playback adapter in App; do not pull either product dependency into Runtime. |
| 9 | `app/src/main/java/io/legado/app/model/analyzeRule/RuleDataInterface.kt` | `COPY_AS_IS` | Exact rule-data API contract; preserve all properties, nullable values and overloads. |
| 10 | `app/src/main/java/io/legado/app/model/analyzeRule/RuleData.kt` | `COPY_AS_IS` | Exact rule-data model; retain package/API compatibility. |
| 11 | `app/src/main/java/io/legado/app/help/JsExtensions.kt` | `SPLIT` | Preserve JS-visible method names, overloads, return/null/error behavior and callback order. Move Activity/Dialog/clipboard/navigation/file/context/resource/WebView effects to typed App ports. |
| 12 | `app/src/main/java/io/legado/app/help/JsEncodeUtils.kt` | `COPY_AS_IS` | Pure encoding/decoding helper; preserve exact behavior and byte/charset overloads. |
| 13 | `app/src/main/java/io/legado/app/help/RegexJsExtensions.kt` | `COPY_AS_IS` | Pure regex JS helper; preserve `@Keep`/reflection reachability and exact exposed names. |
| 14 | `app/src/main/java/io/legado/app/ui/login/SourceLoginJsExtensions.kt` | `HOST_ONLY` | Keep as App login bridge. Preserve source-facing API and callback order; inject typed UI/browser/verification/session ports. Do not move Activity/AppCompat/lifecycle into Runtime. |
| 15 | `app/src/main/java/io/legado/app/ui/rss/read/RssJsExtensions.kt` | `EXCLUDE` | RSS/search/photo/database product bridge is outside source Runtime. If the login bridge needs a shared helper, extract only that helper after proving the exact call sites; never copy the RSS screen/API wholesale. |
| 16 | `app/src/main/java/io/legado/app/model/SharedJsScope.kt` | `ADAPT_WITH_PORT` | Preserve shared Rhino global/lexical scope identity, cache size/eviction and source lifetime. Replace static App context/cache/HTTP dependencies with explicit scope/cache/context/HTTP contracts. |
| 17 | `app/src/main/java/io/legado/app/model/jsSource/JsSourceEngine.kt` | `ADAPT_WITH_PORT` | Core JS source execution. Preserve Rhino Context lifecycle, source cache, cookies, compile/eval and callbacks; use `:legado-rhino` ABI. |
| 18 | `app/src/main/java/io/legado/app/model/jsSource/JsSourceBook.kt` | `ADAPT_WITH_PORT` | Preserve JS book orchestration and source/book identity. Replace generated resources, appCtx, source catalog and book persistence with typed ports. |
| 19 | `app/src/main/java/io/legado/app/model/jsSource/JsSourceMarshaller.kt` | `COPY_AS_IS` | Preserve exact Rhino/Gson marshaling, null/undefined, primitive/array/object and error behavior. |
| 20 | `app/src/main/java/io/legado/app/model/webBook/WebBook.kt` | `ADAPT_WITH_PORT` | Core source workflow; replace ExploreAdapter LRU, appDb, HTTP and Book/Chapter access with ExploreCatalog/HTTP/Book/Chapter ports. Preserve sourceUrl-keyed 99-entry InfoMap semantics. |
| 21 | `app/src/main/java/io/legado/app/model/webBook/BookList.kt` | `ADAPT_WITH_PORT` | Preserve search/explore ordering, duplicate handling, source result identity and `ExploreKind` semantics; replace appCtx/R/Book/source persistence with ports. |
| 22 | `app/src/main/java/io/legado/app/model/webBook/BookInfo.kt` | `ADAPT_WITH_PORT` | Preserve detail lookup/update ordering and fallback behavior; replace appCtx/R/Book persistence with ports. |
| 23 | `app/src/main/java/io/legado/app/model/webBook/BookChapterList.kt` | `ADAPT_WITH_PORT` | Preserve TOC pagination, chapter order, URL resolution, cache and empty/error behavior; replace appDb/config/context/resources with ports. |
| 24 | `app/src/main/java/io/legado/app/model/webBook/BookContent.kt` | `ADAPT_WITH_PORT` | Preserve content pipeline, multi-page behavior, chapter replacement, book/chapter state mutation and exact error/empty semantics; replace appDb/config/context/resources with ports. |
| 25 | `app/src/main/java/io/legado/app/help/source/BaseSourceExtensions.kt` | `COPY_AS_IS` | Core source JS binding helper; preserve exposed names and source extension behavior. |
| 26 | `app/src/main/java/io/legado/app/help/source/BookSourceExtensions.kt` | `ADAPT_WITH_PORT` | Preserve `ExploreKind` parsing and source extensions; replace direct ExploreAdapter LRU access with ExploreCatalogPort. |
| 27 | `app/src/main/java/io/legado/app/model/login/LoginUiV2.kt` | `COPY_AS_IS` | Keep the protocol/model and exact row/action JSON contract in Runtime; native UI rendering stays App-side. |
| 28 | `app/src/main/java/io/legado/app/help/book/ContentProcessor.kt` | `ADAPT_WITH_PORT` | Preserve replacement order, regex options, timeouts, disable checks and book-state effects; route config, BookRepository, notifications and context through typed ports. |
| 29 | `app/src/main/java/io/legado/app/data/entities/ReplaceRule.kt` | `SPLIT` | Preserve Room/Parcelable model and all fields/order; move generated R/resource/context and persistence methods to App adapters. No schema migration in this task. |
| 30 | `app/src/main/java/io/legado/app/utils/InfoMap.kt` | `COPY_AS_IS` | Preserve mutable fields, key lookup and dirty flag; its cache/persistence owner is the ExploreCatalogPort, not a duplicate Runtime cache. |
| 31 | `app/src/main/java/io/legado/app/data/entities/rule/RowUi.kt` | `COPY_AS_IS` | Preserve exact JSON fields, Type constants, equality/hashCode and default style fallback; renderer remains App-side. |
| 32 | `app/src/main/java/io/legado/app/data/entities/rule/ExploreKind.kt` | `COPY_AS_IS` | Preserve exact JSON fields, Type constants, equality/hashCode and style defaults; UI adapter remains App-side. |
| 33 | `app/src/main/java/io/legado/app/data/entities/rule/FlexChildStyle.kt` | `SPLIT` | Keep all style data/defaultStyle/alignSelf and serialized `layout_justifySelf` in Runtime; move only `apply(View)`/FlexboxLayout mutation to App, preserving application order. |

No candidate is silently dropped. `BookSource` remains `CONDITIONAL` until the exact source model and existing file-backed catalog can be reconciled without introducing an unapproved Room schema or retaining a shadow DTO.

## 4. Target module and production file plan

### 4.1 Target dependency direction

Target production graph (acyclic):

```text
:app  ───────────────► :legado-runtime ─────────► :legado-rhino
  │                           │
  └── Android host adapters   └── no App / DAO / UI / Reader dependency
```

`:legado-runtime` becomes the owner of the real pinned source model/rule/JS/source workflow and stable Runtime API. `:legado-rhino` remains the isolated Rhino engine/ABI module. `:app` owns Android Context, Room/database schema and DAO adapters (if separately approved), HTTP/Cookie persistence, WebView/browser/verification/UI, file/cache storage, image/media, Flexbox/View and Badcat product screens.

The current `:integration-contract` module is not yet a clean contract-only module: it contains most source execution implementation. Phase B must move the production Runtime implementation and its tests into `:legado-runtime`, then remove `implementation(project(":integration-contract"))` from App only after all public API types and callers have moved without duplicate/shadow models. Do not create a Runtime→App or Runtime→integration-contract cycle. Whether `:integration-contract` is retired or retained solely as a non-production compatibility fixture module is a build decision to resolve before editing; it must not remain a second production implementation or second source of truth.

### 4.2 `:legado-runtime` production source ownership

Add/migrate pinned source files under the exact upstream package paths in `legado-runtime/src/main/java/io/legado/app/...` (or `src/main/kotlin` where the source is Kotlin), including all 33 candidate paths according to §3 and the already selected pinned rule models currently injected from the upstream tree. Do not compile the whole upstream App source tree.

Move the current production engine implementation out of `integration-contract/src/main/kotlin/app/badcat/engine/` into `legado-runtime/src/main/kotlin/app/badcat/engine/`, preserving FQCNs during the first migration wherever that does not preserve a shadow Legado model. The core implementation set includes:

- `BookSourceSearchUrlResolver.kt`, `CatalogLegadoBookTextReplacement.kt`, `ChapterCatalogQuery.kt`, `ChapterContentCache.kt`, `ChapterContentSaveFence.kt`;
- `CssBookInfoParser.kt`, `CssChapterContentParser.kt`, `CssRuleEvaluator.kt`, `CssSearchResultsParser.kt`, `CssTocParser.kt`, `JsonPathRuleEvaluator.kt`;
- `LegadoBookSourceCodec.kt`, `LegadoElementIndex.kt`, `LegadoEmbeddedRuleTemplate.kt`, `LegadoEmbeddedUrlTemplate.kt`, `LegadoLiteralReplacement.kt`, `LegadoLoginUiV2.kt`, `LegadoPutRule.kt`, `LegadoRegexElementEvaluator.kt`, `LegadoReplaceRuleCodec.kt`, `LegadoReplaceRuleScope.kt`, `LegadoRuleExpressionDispatcher.kt`, `LegadoRuleReplacement.kt`, `LegadoSourceElement.kt`, `LegadoUrlParameterEncoder.kt`, `LegadoUrlResolver.kt`, `LegadoUrlScriptRule.kt`, `MainJsSourceGuard.kt`, `PaginationUrlIdentity.kt`, `PinnedLegadoRuleAnalyzer.kt`, `RuleCombinationEvaluator.kt`, `RuleExpressionTokenizer.kt`, `SelectorLegadoRuleRuntime.kt`, `SelectorRuleEvaluator.kt`, `SourceBookDetailService.kt`, `SourceChapterContentService.kt`, `SourceExploreService.kt`, `SourceHttpGateway.kt`, `SourceReadingEngine.kt`, `SourceRequestFactory.kt`, `SourceRuleResolver.kt`, `SourceSearchPlanner.kt`, `SourceSearchService.kt`, `SourceTocService.kt`, `WebUrlResolver.kt`, and `XPathRuleEvaluator.kt`.
- Move the original/adapted pinned analyzer files from `integration-contract/src/pinnedLegado/kotlin/io/legado/app/...` and the `pinnedLegado` source-set declarations from `integration-contract/build.gradle.kts` to the Runtime build. Preserve exact upstream source hash and clearly mark the small platform-adapted files; do not silently fork/rename the upstream classes.

Keep Badcat-only product implementations `BadcatPpRuleEvaluator.kt`, `BadcatPpRuleProtoDecoder.kt`, `Purification.kt` and `ReadingProgressStore.kt` outside the Legado Runtime migration. Do not move Fourth/Fifth-category behavior into the Third-category Runtime.

### 4.3 Stable Runtime API and host adapters

The current `ReadingEngine.kt` and `SourceRuntime.kt` API definitions need to be reconciled with exact upstream models before becoming the Runtime public API. The current `BookSource` DTO is not an acceptable authoritative substitute for upstream `io.legado.app.data.entities.BookSource`. Keep Badcat-specific presentation projections only at the product UI boundary; do not use them as Runtime source models or silently discard upstream fields.

`OkHttpSourceRequestClient` is a concrete host transport implementation and should move to App (or be split so only request/response contracts remain in Runtime). Keep `SourceRequestClient`, `SourceHttpRequest` and `SourceHttpResponse` as typed contracts only if their fields are proven to cover upstream request/response semantics; proxy, streaming, charset, redirects, retries, cookie policy, final URL, status reason and cancellation must be explicitly reconciled first.

Current App runtime files and target ownership:

| Current exact App path | Target |
|---|---|
| `app/src/main/java/app/badcat/client/runtime/BadcatReadingEngineFactory.kt` | Keep App; reduce to composition root wiring typed ports and Runtime implementation. |
| `app/src/main/java/app/badcat/client/runtime/BadcatSourceHttpGatewayFactory.kt` | Keep App transport composition only; move source-specific evaluator/core gateway logic to Runtime. |
| `app/src/main/java/app/badcat/client/runtime/RhinoSourceScriptRuntime.kt` | Move core scope/evaluation into Runtime; split all callbacks into typed ports. |
| `app/src/main/java/app/badcat/client/runtime/RhinoSafeSourceJavaExtensions.kt` | Split JS-visible pure API into Runtime and HTTP/file/context/UI effects into ports/adapters. |
| `app/src/main/java/app/badcat/client/runtime/RhinoSourceCacheExtensions.kt` | Move core API to Runtime; inject SourceCache/Variable ports. |
| `app/src/main/java/app/badcat/client/runtime/RhinoSourceCookieExtensions.kt` | Move JS-facing contract to Runtime; inject Cookie/Session port. |
| `app/src/main/java/app/badcat/client/runtime/RhinoEmbeddedUrlScriptEvaluator.kt` | Move to Runtime; preserve URL script evaluation semantics. |
| `app/src/main/java/app/badcat/client/runtime/RhinoLegadoJsReplacementEvaluator.kt` | Move to Runtime; preserve replacement JS behavior. |
| `app/src/main/java/app/badcat/client/runtime/RhinoRegexCompatibility.kt` | Move to Runtime; preserve exact regex compatibility. |
| `app/src/main/java/app/badcat/client/runtime/LegadoConcurrentRateLimiter.kt` | Move to Runtime; preserve per-source rate window and skip semantics. |
| `app/src/main/java/app/badcat/client/runtime/LegadoSourceLock.kt` | Move to Runtime; preserve source-scoped lock/single-flight/tick semantics. |
| `app/src/main/java/app/badcat/client/runtime/LegadoChineseConversion.kt` | Move to Runtime; preserve dictionary/version and conversion behavior. |
| `app/src/main/java/app/badcat/client/runtime/UpstreamLegadoCssRuleAdapter.kt` | Move to Runtime; bind to the pinned upstream analyzer. |
| `app/src/main/java/app/badcat/client/runtime/SourceScriptKeyValueStore.kt` | Move interface to Runtime; keep in-memory implementation test-only; persistent implementation remains App. |
| `app/src/main/java/app/badcat/client/runtime/SourceScriptFileCacheStore.kt` | Move interface to Runtime; keep in-memory implementation test-only; file implementation remains App. |
| `app/src/main/java/app/badcat/client/runtime/SourceLoginSession.kt` | Split typed session contract/JS semantics into Runtime; SharedPreferences/encryption and Cookie adapter remain App. |
| `app/src/main/java/app/badcat/client/runtime/LegadoSourceLoginUiV2Service.kt` | Move protocol execution into Runtime; keep native form rendering and Activity lifecycle in App. |
| `app/src/main/java/app/badcat/client/runtime/SharedPreferencesSourceCookieJar.kt` | Keep App; implements the eventual Cookie/Session host port. |
| `app/src/main/java/app/badcat/client/runtime/DefaultProcessCookieBridge.kt` | Keep App; WebView CookieManager synchronization is host-only. |
| `app/src/main/java/app/badcat/client/runtime/SharedPreferencesSourceScriptKeyValueStore.kt` | Keep App; implements Runtime key/value port. |
| `app/src/main/java/app/badcat/client/runtime/SourceLibArchiveReader.kt` | Split core archive contract from native/file implementation; keep libarchive/stream ownership in App unless a platform-neutral implementation is proven. |
| `app/src/main/java/app/badcat/client/runtime/RhinoSourceFileHandle.kt` | Split source-visible file handle API into Runtime and safe-root file operations into FileStoragePort/App. |
| `app/src/main/java/app/badcat/client/runtime/FileSourceScriptCacheStore.kt` | Keep App; file-backed source cache adapter. |
| `app/src/main/java/app/badcat/client/runtime/FileChapterContentCache.kt` | Keep App; file-backed chapter cache adapter, preserving atomic write/save fences. |
| `app/src/main/java/app/badcat/client/runtime/CacheFilePruner.kt` | Keep App; storage lifecycle/space policy. |
| `app/src/main/java/app/badcat/client/runtime/FlexStyleLayoutAdapter.kt` | Keep App-only; applies Runtime-owned `FlexChildStyle` data to Android View/Flexbox LayoutParams. Its source exists, but App compilation/runtime remains unverified under §24.119. |
| `app/src/main/java/app/badcat/client/runtime/BadcatPurificationCoordinator.kt` | Keep Badcat product layer; not a Legado Runtime dependency. |

### 4.4 `integration-contract` source ownership

The following current API concepts must have exactly one production owner after migration: `ReadingEngine`, the source-facing model types, `SourceRequestClient`/request-response contracts, `SourceScriptRuntime`, and the typed Host Port contracts. Prefer moving the canonical API and implementation together into `:legado-runtime` where exact upstream model types are required. Do not keep a second reduced `BookSource` or duplicate engine DTO as a compatibility shortcut.

`integration-contract` implementation and test files listed in §4.2 are migration sources, not proof of Runtime integration. After migration, its tests must either move to Runtime/App test source sets or become true external compatibility fixtures that consume the one Runtime API. The current Kotlin/JVM module cannot simply depend on an Android library variant; do not create that edge without a concrete Gradle variant proof. Retire the module from production settings only after callers/tests have been migrated and exact App references are updated.

## 5. Typed Host Port contract freeze

### 5.1 Existing declarations and exact current signatures

These signatures exist in `integration-contract/src/main/kotlin/app/badcat/engine/RuntimeHostPort.kt`; they are declarations only and currently have no production App implementer/wiring:

```kotlin
interface HiddenWebViewPort {
    suspend fun execute(request: HiddenWebViewRequest): HostPortResult<HiddenWebViewResult>
}
interface VisibleBrowserPort {
    suspend fun launch(request: BrowserVerificationRequest): UiDispatchOutcome
    suspend fun awaitVerification(request: BrowserVerificationRequest): HostPortResult<BrowserVerificationResult>
}
interface VerificationCodePort {
    suspend fun awaitCode(request: VerificationCodeRequest): HostPortResult<String>
}
interface HostUiPort {
    suspend fun dispatch(action: HostUiAction): UiDispatchOutcome
}
interface ProductEventPort {
    suspend fun publish(context: SourceExecutionContext, event: ProductRefreshEvent): UiDispatchOutcome
}
```

Existing source-facing service contracts also present in the current code:

```kotlin
interface SourceRequestClient {
    suspend fun execute(request: SourceHttpRequest): EngineResult<SourceHttpResponse>
}
interface SourceScriptRuntime {
    suspend fun evaluate(source: BookSource, script: String,
        bindings: Map<String, String> = emptyMap()): EngineResult<String>
    suspend fun getString(source: BookSource, key: String): EngineResult<String?>
    suspend fun putString(source: BookSource, key: String, value: String): EngineResult<Unit>
}
fun interface BookSourceCatalog {
    suspend fun all(): EngineResult<List<BookSource>>
}
```

The current `SourceScriptRuntime` default `getString`/`putString` implementations return `UNSUPPORTED_RULE`; that is fail-closed but cannot satisfy complete compatibility. `SourceExecutionContext` carries a coroutine `Job`; suspended port implementations must propagate `CancellationException`. A user-dismissed/host-shutdown `HostPortResult.Cancelled` is a domain result and must not swallow coroutine cancellation. `UiDispatchOutcome.Accepted` means dispatch accepted, not that a callback/result completed.

### 5.2 Required ports and freeze status

| Port / owner | Required operations and semantic contract | Freeze status |
|---|---|---|
| `BookSourceCatalogPort` / App file catalog | Read/import/export/update enabled/remove; exact sourceUrl identity, original JSON fields, ordering, duplicate replacement, atomic persistence and post-write invalidation. Current `BookSourceCatalog.all()` is too narrow for full lifecycle. | `PARTIAL`; exact canonical model and import/update/remove signatures need reconcile. |
| `SourceExecutionStatePort` | Acquire/release per-source execution state, invalidate after source replace/remove, preserve same-source serialization and cross-source concurrency. | `MISSING`; no lifecycle owner/signature frozen. |
| `SourceCookiePort` | Get/set/replace/remove-by-key/clear and HTTP/WebView cookie synchronization with source identity, domain/path/expiry, callback completion and persistence semantics. | `MISSING`; current cookie adapter APIs differ in sourceId/sourceKey/url identity. |
| `SourceSessionPort` | Login header/info get/put/remove/clear; preserve encrypted login-info storage and source-key semantics. | `PARTIAL`; current `SourceLoginSessionStore` is App-owned and lacks typed Runtime boundary. |
| `SourceHttpPort` | GET/HEAD/POST/connect/ajax, request header precedence, body bytes/stream, status/reason/final URL, charset, cookie policy, proxy, redirects, retry, per-call timeout and cancellation. | `PARTIAL`; `SourceRequestClient` exists, but proxy/stream/cookie/error equivalence and synchronous JS bridge semantics are not fully frozen. |
| `SourceImageTransportPort` | Source URL plus exact request headers/cookies and body stream for Glide adapter. | `MISSING`; keep Glide implementation App-side. |
| `SourceCachePort` | Cache read/write/remove/clear, source namespace, TTL, size limits and lifecycle. | `PARTIAL`; current script/file cache interfaces are App package APIs, not unified Runtime ports. |
| `SourceVariablePort` / `RuleVariablePort` | Persistent source variables and rule variables with exact key namespace, null/empty/remove semantics and lifetime. | `PARTIAL`; current key-value store covers only part of the surface. |
| `SharedJsScopePort` | Scope acquire/reset/clear, shared global/lexical identity, source script hash invalidation, same-source locking and cleanup. | `MISSING`; current process-static maps are implementation details, not an injectable lifecycle contract. |
| `AppContextPort` / `ResourcePort` | App-scoped paths, resource/string lookup and generated-R replacement without leaking Android Context or resource IDs. | `MISSING`; source values and exact resource behavior must be inventoried before signatures freeze. |
| `FileStoragePort` | Safe-root read/write/delete/list/archive operations, canonical-path confinement, size limits, cache/external-cache locations and stream closure. | `MISSING`; current `RhinoSourceFileHandle` and App file code are not a typed port. |
| `BookRepositoryPort` / `ChapterRepositoryPort` | Exact upstream queries, ordering, transactions, mutations, chapter identity, content/BigData access and cache invalidation. | `MISSING`; appDb call graph and data owner must be frozen; no schema v1 now. |
| `SearchResultRepositoryPort` | SearchBook result persistence, identity, duplicate/ordering and cleanup. | `MISSING`; no contract or owner confirmed. |
| `ExploreCatalogPort` | `InfoMap` acquire/lookup/update/invalidate and dirty-only persistence. Preserve sourceUrl key, 99-entry LRU, object identity, dirty flag and snapshot save. | `PROPOSED`; no production implementation. |
| `ReplacementRulePort` / `ReplacementRuntimePort` | Rule query/order, enabled/timeout/regex flags, replacement order and script evaluation. | `PARTIAL`; proposals exist, not wired to full upstream ContentProcessor. |
| `RuntimeConfigPort` / `RuntimeNotificationPort` | Exact ReadConfig/AppConfig values and toast/notification semantics without App resource/context types. | `PROPOSED`; source values/callers need exact mapping. |
| `LoginCallbackPort` | Login/browser/image-code callbacks, callback order, exactly-once completion, cancellation, Activity lifecycle and result/error propagation. | `MISSING`; `SourceLoginJsExtensions` currently host-only and existing UI ports have no implementer. |
| `FlexStyleLayoutAdapter` | Apply `FlexChildStyle` to actual host `FlexboxLayout.LayoutParams` in the same order/defaults. | `MISSING`; helper remains App-side and is not a Runtime port. |

Do not guess missing signatures. For each missing port, the next implementation preregistration must attach exact upstream call sites, argument/null behavior, result/callback order, threading/cancellation and ownership before defining the interface. Do not use a single universal `HostServices`, map, reflection bag or generic callback registry.

## 6. Dependency and build plan

### 6.1 Runtime dependencies to reconcile

Pinned upstream evidence (§24.124) identifies the following relevant coordinates/versions. Add only those required by the actual selected source closure; no version upgrades:

- Kotlin/AGP baseline: AGP 8.13.2; Kotlin Android/JVM/Parcelize 2.4.20; Java/Kotlin target 21.
- Core parsing/serialization: Gson 2.14.0; Jsoup 1.23.2; JsoupXpath 2.5.3; JsonPath 3.0.0; Commons Text 1.13.1; Hutool Crypto 5.8.22 and its exact Bouncy Castle 1.86 dependencies where source calls require them.
- Async/JS/network: coroutines 1.11.0; `:legado-rhino` with exported HtmlUnit CoreJS 5.3.0-legado.4; OkHttp 5.4.0 only where the final Runtime API/implementation truly owns transport types (otherwise keep OkHttp in App adapter).
- Android annotations/cache: AndroidX annotation 1.10.0 and AndroidX collection 1.6.0 are direct imports/leaks and must be declared directly if retained; do not rely on `implementation` transitives from `:legado-rhino`.
- Entity model support, conditional on §3: Room annotation artifact `androidx.room:room-common:2.8.4` and Parcelize plugin/runtime 2.4.20 may be needed to preserve exact entity annotations and Parcelable ABI. This does not authorize Room database/schema/compiler/KSP or migration work. Room runtime/compiler belongs to App only if a separate data decision authorizes a Room database.
- Host-only: AndroidX AppCompat 1.7.1, Lifecycle 2.10.0, Glide 5.0.7, Media3 1.10.1, Flexbox 3.0.0 View adapter, Splitties appctx 3.0.0, generated App `R`, App Database/DAO and App-specific file/cookie stores stay out of Runtime.
- `:modules:book` is excluded. `:modules:rhino` is not imported; use Badcat `:legado-rhino` and preserve its `api` CoreJS dependency.

### 6.2 Build file changes (future Phase B only)

| Exact file | Planned change |
|---|---|
| `settings.gradle.kts` | Keep `:app`, `:legado-runtime`, `:legado-rhino`; remove `:integration-contract` from production graph only after API/source/test migration. Keep POC out of App dependencies. |
| `legado-runtime/build.gradle.kts` | Replace the small eight-file injected source slice with explicit path-exact pinned sources under Runtime `src/main`; add only verified direct dependencies and pinned source metadata; keep Android library/Java 21/Parcelize as required by models. No whole-upstream sourceSet. |
| `legado-rhino/build.gradle.kts` | Preserve current pinned Rhino dependency/API surface; direct dependency ownership audit only, no version change. |
| `integration-contract/build.gradle.kts` | Remove production implementation and pinned source compilation only after those sources/tests move; no JVM→Android-library dependency guess. Retire or keep only as a true independent fixture after a variant proof. |
| `app/build.gradle.kts` | Replace `implementation(project(":integration-contract"))` with the one canonical Runtime dependency after API migration; keep Android-only dependencies and host adapters here. No App compile retry is allowed. |
| `build.gradle.kts` | Keep AGP/Kotlin pins unchanged; no version catalog creation in this task. |

No edge from Runtime to App, App DAO, App UI, `:modules:book` or product Reader. The candidate graph must be mechanically checked for cycles and forbidden package references in a separately registered architecture-test task before acceptance.

## 7. Exact focused test and acceptance matrix

These are required future paths/tests; no test was run or added by Task 026L. Existing tests are retained as evidence and should be moved/extended, not deleted to make the suite green.

### 7.1 Runtime tests

Create or migrate under `legado-runtime/src/test/kotlin/io/legado/runtime/`:

- `model/UpstreamSourceModelFieldParityTest.kt` — every upstream field, constructor default, nullable/absent distinction, equality/hashCode and sourceUrl identity.
- `model/UpstreamModelSerializationParityTest.kt` — Gson round-trip, unknown-field retention, nested rule JSON and null/default behavior.
- `model/UpstreamParcelableContractTest.kt` — Parcelable round-trip for BookSource/Book/BookChapter/SearchBook/ReplaceRule when the model build is approved; no schema creation.
- Existing `PinnedLegadoUiModelContractTest.kt` — Task 026M's exact RowUi/ExploreKind/FlexChildStyle field/default/equality/serialization contract; retain its 3/3 Runtime-only pass as bounded evidence, not as source-runtime acceptance.
- `analyze/AnalyzeRuleDifferentialTest.kt` — pinned CSS/JSONPath/XPath/regex/JS branch dispatch and exact output/error parity.
- `analyze/AnalyzeUrlRequestSemanticsTest.kt` — URL option parsing, header precedence, redirect, timeout, charset, status/body bytes and error semantics.
- `source/BaseSourceLifecycleContractTest.kt` — source state, login, cache, callback, cancellation and cleanup lifecycle.
- `js/JsExtensionsApiInventoryTest.kt` — every public JS method and overload against pinned API inventory; reflection/keep rules.
- `js/SharedJsScopeLifecycleTest.kt` — top-level `var`/`let`/`const`, globalThis, jsLib/mainJs initialization, source hash change, same-source serialization and clear/restart behavior.
- `js/SourceCookieSessionParityTest.kt` — cookie get/set/replace/remove and login header/info semantics across same source and new runtime instances.
- `js/SourceCacheVariableParityTest.kt` — key namespace, TTL, remove/prefix/clear, byte cache and lifecycle.
- `workflow/ExploreInfoMapLruDirtyPersistenceTest.kt` — sourceUrl key, exact 99-entry eviction, object identity, dirty-only snapshot persistence and invalidation.
- `workflow/SearchExploreDetailTocContentDifferentialTest.kt` — real pinned pipeline for all six rule stages, page progression, multi-page content and empty/error behavior.
- `workflow/ContentReplacementOrderParityTest.kt` — replacement order, regex options, JS rules, timeout/disabled rule and book-state mutation.
- `workflow/SourceExecutionCancellationTest.kt` — coroutine cancellation, timeout and callback exactly-once semantics.

### 7.2 App host adapter tests

Keep/extend under `app/src/test/kotlin/app/badcat/client/runtime/`:

- `SharedPreferencesSourceCookieJarTest.kt`, `DefaultProcessCookieBridgeTest.kt`, `SourceLoginSessionTest.kt` — cookie domain/path/expiry, WebView synchronization, persistence and login cleanup.
- `SharedPreferencesSourceScriptKeyValueStoreTest.kt`, `SourceScriptFileCacheStoreTest.kt`, `FileChapterContentCacheTest.kt`, `FileChapterContentCacheAtomicFileTest.kt`, `CacheFilePrunerTest.kt` — file root confinement, atomic writes, size limits, eviction and restart.
- `BadcatSourceHttpGatewayFactoryTest.kt` — same source context, HTTP headers, cookies, redirects, timeout and cancellation.
- `LegadoSourceLoginUiV2ServiceTest.kt` — Login UI v2 protocol only; host UI callback/lifecycle tests remain Android tests.
- `RhinoHostSurfaceInventoryTest.kt`, `RhinoOverloadResolutionTest.kt`, `RhinoSourceJsAbiBytecodeTest.kt` — move Runtime API/ABI assertions to Runtime tests; keep Android host adapter assertions here.
- Add `RuntimeHostPortAdapterContractTest.kt`, `SourceCatalogInvalidationIntegrationTest.kt`, `SourceHttpCookieHostAdapterTest.kt` and `FlexStyleLayoutAdapterTest.kt`.

### 7.3 Android integration tests

Create under `app/src/androidTest/kotlin/app/badcat/client/runtime/`:

- `HiddenWebViewPortLifecycleTest.kt` — actual WebView creation/reuse/destroy, page evaluation, resource/override URL capture, headers/cache/delay and timeout.
- `VisibleBrowserVerificationPortTest.kt` — launch, user verification, final URL/body delivery and Activity recreation.
- `VerificationCodePortTest.kt` — image-code callback, timeout, cancellation and exactly-once completion.
- `LoginCallbackLifecycleTest.kt` — Login UI/SourceLogin extension callback order and Activity destruction/recreation.
- `RuntimeHostPortCancellationTest.kt` — cancellation propagation across Android dispatch, HTTP and WebView.

### 7.4 Required source-level acceptance gates

1. **API/model parity:** generated report compares pinned upstream public symbols, overloads, fields, defaults, annotations, serializers and model equality. Zero unexplained missing symbols.
2. **Architecture:** Runtime has no App package, `appDb`, DAO, Activity/View, App `R`, Splitties appCtx, Glide, Media3, RSS UI, Reader UI or `:modules:book` dependency. Runtime depends only on Runtime API/core and `:legado-rhino`; App implements ports.
3. **Differential behavior:** same fixture inputs are run against pinned upstream and migrated Runtime; compare values, nulls, errors, side effects, cookies, requests, callback order and lifecycle. No “same output for one sample” shortcut.
4. **Real-source E2E:** multiple source types and rules (CSS, JSONPath, XPath, regex, JS, login, explore), search pagination, detail, TOC pagination, content, multi-page content, Cookie/session, file/archive helpers and source-state cleanup. Use sanitized recorded fixtures first, then a small live-source smoke set; classify site outage separately from engine defect.
5. **App UI parity:** original Badcat screens and Reader remain unchanged in layout/navigation/interaction; host adapter changes receive targeted Espresso/manual regression.
6. **Release:** only after source/API parity, tests, CI, exact commit SHA, R8/reflection/keep proof, real-device tests and artifact hashes. Debug compile or a passing unit test is not full compatibility.

### 7.5 Current evidence limitations and stop conditions

- App compiled Runtime ABI is unavailable: §24.121 found no App Runtime classes; §24.119 forbids any further App compile retry. Do not attempt App compile/assemble to “see if it works”.
- Existing ten upstream full-App dependency reports are metadata evidence only; they do not prove the isolated Runtime dependency graph.
- No current production implementer exists for the five declared UI/WebView/browser/verification/product ports.
- Full upstream `BaseSource`, `AnalyzeRule`, `AnalyzeUrl`, `JsExtensions`, `SharedJsScope`, `JsSourceEngine`, `JsSourceBook`, `WebBook`, `BookList`, `BookInfo`, `BookChapterList`, `BookContent`, `ContentProcessor` and exact entity ABI are not compiled in the current Badcat Runtime.
- Stop if any upstream public symbol, source lifecycle, host callback, model field/default, database ownership or dependency edge cannot be proven. Do not replace it with a default/no-op implementation or widen into unrelated App UI/Reader/RSS/product modules.

## 8. Phase B sequencing and prerequisites

Phase B implementation must be separately registered with exact files, callers, typed port signatures, source model mapping and test oracles. After 026M–026O, the sequence is:

1. Register Task 026P as a read-only exact typed Host Port signature freeze: owner/cross-module visibility, SourceExecutionState, SourceType, cache/cookie/session/HTTP, context/config/resource/file, UI/WebView/browser/verification/events/batch-content and Explore. Reconcile every signature to its upstream owner/callsite and null/error/thread/cancel/callback/lifecycle semantics.
2. Only after 026P commits and both required lists are reread, register a bounded owner implementation/proof task. The owner API must be Kotlin-visible to App implementers but hidden from Rhino/JS by a proven JVM-synthetic/ABI strategy; a Kotlin `internal` interface in Runtime is not implementable by App code. Prove same-instance identity, Room/Gson/Parcelable exclusion, copy/restore reattachment and multiple-runtime concurrency before moving BookSource/BaseSource.
3. Cover every direct BaseSource implementer (`BookSource`, App-only `RssSource`, `HttpTTS`, `BookCover.CoverRule`) and the `SourceNavigationBridge` delegation wrapper. Keep product models/UI in App; use generic `getKey()` identity.
4. Move the pure `ConcurrentRateLimiter` and `SharedJsScope` core to Runtime only after behavior/ownership tests. Do not treat a typed aggregate as a generic map or callback registry.
5. Implement the canonical upstream BookSource model and App AtomicFile catalog adapter only after the owner contract and Room/Gson/Parcelable exclusion tests pass. No Room schema v1, Room database/compiler/KSP or dual-write.
6. Move the canonical Runtime API and production services out of `integration-contract`; ensure no duplicate DTO/model source of truth.
7. Add the exact pinned core source closure and analyzer/JS/Rhino wiring to `:legado-runtime` with `:legado-rhino`; retain only host adapters in App.
8. Add contract/model/differential tests before enabling any source execution path; then wire host implementations and real-source E2E.
9. Run only separately authorized Runtime-focused tests and architecture checks. App compiled ABI remains unavailable and cannot be retried under §24.119; App/R8/device gates require a separately approved viable runner/evidence path.

**No source/build/test edit is included in this read-only snapshot refresh.** The user previously authorized Phase B in principle, but the master workflow requires this refreshed addendum and result records to be committed and both required lists reread before registering the next bounded implementation task. The reread is a mandatory gate, not permission to skip task registration.

## 9. Task 026L result and disposition

- Read-only inspected current module/build declarations, all `legado-runtime`, `legado-rhino` and `integration-contract` source/test inventories, current App Runtime implementation files, exact production facade callers, host port declarations/implementer search, and the Task 026K source/import/caller/dependency matrices.
- Confirmed the current production implementation is split across App + `integration-contract`; `:legado-runtime` owns the three 026M UI models plus eight injected pinned rule/serialization sources; `:legado-rhino` is the isolated Rhino module. `:legado-runtime-poc` remains a quarantined probe with no App dependency. Existing Host UI/WebView/browser/verification/product ports are not wired.
- Produced the 33-path disposition table, target module/file ownership, exact current App Runtime file destinations, Host Port freeze/blocker matrix, dependency/build plan, test paths and acceptance/stop gates.
- No source/build/test/manifest/schema/UI/Reader file changed. No Gradle invocation, dependency resolution, compile/build/test/assemble, CI, network or device action occurred. No App compile retry occurred.
- **Task 026L:** complete as a design/addendum freeze only. **Task 026 Phase A:** remains open because compiled App ABI, complete port contracts, exact source execution and R8/device evidence remain unresolved. **Phase B:** started only with Task 026M's bounded UI-model ownership slice; no BookSource/BaseSource or source-engine migration is authorized. **Task 027:** blocked. **Stage-0:** not accepted. **Stage-1:** NO-GO. **Full Legado compatibility:** NOT ESTABLISHED.
- **Next mandatory action:** commit this refreshed addendum and audit/overview result; reread `00-MASTER-WORKFLOW.md` and `03-LEGADO-RUNTIME-OVERVIEW.md` from the committed HEAD; only then register the next bounded Phase B implementation task. Do not start implementation until that registration exists.


## 10. Task 026O evidence delta and refreshed inspection snapshot

**Refresh identity:** Badcat `integration/legado-upstream-runtime` at `4e51c9c210c33e3784070e8a557b8c3f4ab54b9c`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`. Both tracked worktrees were clean at inspection. The changes since the original addendum snapshot include the bounded 026M model move and documentation-only 026N/026O/POC-scope commits; no App compile retry is authorized under audit §24.119.

### 10.1 Current Runtime ownership after Task 026M

- `:legado-runtime` now has three ordinary production source files: `RowUi.kt`, `ExploreKind.kt`, `FlexChildStyle.kt`. The module also injects the seven pinned rule DTOs and `GsonExtensions.kt` at Kotlin compile time. It still does **not** own `BaseSource`, `BookSource`, `JsExtensions`, `AnalyzeRule`, `AnalyzeUrl`, `SharedJsScope` or `JsSourceEngine`.
- Task 026M moved exact `RowUi` and `ExploreKind`; moved `FlexChildStyle` data/default/equality/align behavior and extracted its View/Flexbox mutation into App-only `FlexStyleLayoutAdapter.kt`. Runtime-only tests passed 6/6; the App adapter remains uncompiled/unverified because of §24.119. See audit §24.128.
- Task 026N selected pinned upstream `io.legado.app.data.entities.BookSource` as the only eventual Runtime source model, while retaining `FileBookSourceCatalog`/AtomicFile and raw JSON as the current App-owned persistence boundary. No Room schema/database/compiler/KSP or dual-write is authorized. See audit §24.130.

### 10.2 Runtime POC quarantine decision

- `settings.gradle.kts` includes `:legado-runtime-poc`, but `app/build.gradle.kts` has no dependency edge to it; App depends on `:legado-runtime`, `:legado-rhino` and the current `:integration-contract`.
- The POC build script injects 12 pinned upstream source files into its own Debug/Release Kotlin compile tasks and depends on probe libraries plus `:legado-rhino`. Its `src/main` contains only an empty Android manifest; `source-closure-stage1.json` is metadata for the historical G2d stage-1 probe, not production source.
- **Decision:** retain `:legado-runtime-poc` and its metadata as quarantined historical evidence for now, but exclude it from the production dependency graph. Never add it to App dependencies or merge its source set with `:legado-runtime`. Removing its settings include or deleting the historical artifact is outside this read-only refresh and requires a separate task. Evidence: `/root/stage0-runner/task026l-refresh-attempt1/poc-quarantine.txt` and `source-hashes.tsv`.

### 10.3 BaseSource/JS ABI and identity contract from Task 026O

| Upstream owner | Public declarations | `@JavascriptInterface` | Exact inventory |
|---|---:|---:|---|
| `JsEncodeUtils` | 30 | 20 | 27 unique names; crypto overloads |
| `JsExtensions` | 118 | 48 | 77 unique names; 34 names overloaded |
| `BaseSource` | 29 | 9 | source identity/login/session/cache/concurrency/JS |
| **Combined** | **175 unique signatures** | **77 annotations** | `BaseSource` overrides `JsExtensions.getSource/getTag` |

The exact method/overload/signature/annotation tables are in `/root/stage0-runner/task026-clean/task026o-attempt1/*-api.tsv`. Preserve all JS-visible names, overloads, default arguments, nullable/result/error behavior, `@Keep` and annotation retention. `BaseSource.evalJS` returns `Any?` and binds `java`, `source`, and `sourceApi` to the same BaseSource instance, `baseUrl` to `getKey()`, plus separate `cookie` and `cache` owners.

Current Badcat is not ABI-compatible: the `source` object has 11 login-only methods; 14 BaseSource names and 17 unique JsExtensions names are missing; `java.getSource()` returns `Map<String,Any?>`; `sourceApi` and `baseUrl` are absent; `SourceScriptRuntime` accepts a shadow DTO/string bindings and returns `EngineResult<String>`. Existing bytecode tests codify the Map behavior. Do not use those tests as upstream parity evidence. See `legado-runtime-3.0-basesource-host-port-contract.md` and audit §24.132.

### 10.4 Per-instance attachment design — selected concept, implementation contract still blocked

- Preserve the exact concrete `BookSource` instance. Do not wrap it: `BookSource` is a final data class, `AnalyzeRule` has concrete BookSource casts, `getSource()` returns the same object, `getKey()` is `bookSourceUrl`, and `equals/hashCode` are URL-based.
- A typed `BaseSourceHostServices` aggregate attached through a non-constructor body property remains the selected design direction. The attachment must be excluded from Room (`@Ignore`), Gson/JVM serialization (`@Transient`) and Parcelable (`@IgnoredOnParcel`), and must not become a JS-visible property/method. The aggregate contains individually typed ports only; it is not a generic service map or callback registry.
- **Cross-module visibility correction:** the earlier 026O sketch called `BaseSourceHostServicesOwner` Kotlin `internal`. That is not implementable by App classes across the `:legado-runtime` → `:app` module boundary. The owner API/aggregate must be public to Kotlin App implementers (or exposed through an equivalent public Runtime factory), while the JVM accessor/method must be synthetic/hidden from Rhino. This is a design constraint, not a proven bytecode result; add a focused compile/reflection/Rhino test before implementation acceptance.
- The owner cannot be BookSource-only. Direct BaseSource implementers are `BookSource`, App-only `RssSource`, App-only `HttpTTS`, and App-only `BookCover.CoverRule`; `SourceNavigationBridge` is an interface-delegation wrapper and must forward the original owner's services. All must retain their own generic `getKey()` identity. RSS/TTS/Cover/Reader/Login product models and UI remain App-side.
- `SourceExecutionStatePort` must attach the same `BaseSource` instance (not return a wrapper) and support every direct implementer; `attach(source: BookSource)` alone is too narrow. It must attach after catalog decode/restore/parcel/copy before any host-effect method. `BookSource.copy()` does not copy a body property; the known `checkContent()` copy is a comparison copy, but exhaustive execution-copy/deserialize/parcel reattachment callsites remain unproven.
- `BaseSourceExtensions.getSourceType()` currently branches on concrete `BookSource` and `RssSource`. Runtime cannot import App-only `RssSource`; add a typed `SourceTypePort` or a stable source-kind contract rather than reflection/type-name checks.
- `SourceExecutionContext` must preserve the full active `CoroutineContext`; upstream navigation context may be `EmptyCoroutineContext`, and source tag/name may be empty. A mandatory non-null `Job`/nonblank name is not yet justified.

### 10.5 Typed Host Port evidence and blockers

The corrected 026L refresh matrix is `/root/stage0-runner/task026l-refresh-attempt1/host-port-contract-matrix.tsv` (SHA-256 recorded in the refresh evidence). Exact owner observations that constrain the next interface design:

- **Cache:** upstream `CacheManager` has a 50 MiB memory LRU, Room cache persistence and ACache file/byte storage. `put(key,value,saveTime:Int=0)` uses seconds; memory values are `Any`; `getFromMemory` returns `Any?`; `getFile` returns `String?`; `putFile` accepts a String. `WebCacheManager` is a separate JS facade. The earlier candidate row incorrectly narrowed these signatures; do not freeze it as written. Current Badcat cache namespacing/LRU/file TTL are not equivalent.
- **Cookie:** `CookieStore.getCookie/getKey` return non-null String; `setCookie(url,cookie:String?)`, `setWebCookie`, `replaceCookie`, `removeCookie`, `clear`, `cookieToMap/mapToCookie` are distinct. It merges persistent domain cookies with process session cookies, synchronizes WebView CookieManager state, and removes random keys above 4096 characters. `clear()` deletes persistent OkHttp cookies only; session/WebView cleanup is separate. Current Badcat bridge defaults to empty/no-op readers/removers or throwing writers; no verified WebView sync exists.
- **Login/session:** preserve Android-ID-derived AES, nullable `getLoginHeader/getLoginInfo`, Unit `putLoginHeader`, Boolean `putLoginInfo`, source-key cache namespaces, cookie replacement/removal order, dynamic Login UI v2 default-map derivation and recursion guard. Current Rhino login bridge returns non-null empty strings/JSON strings and omits dynamic defaults.
- **HTTP:** current `SourceHttpRequest` omits upstream `dnsIp`, `serverID`, `bodyJs`, `webView`, `webViewDelayTime`; the request factory explicitly excludes script-generated options. Synchronous JS entry points must bridge suspend transport using the active coroutine context and propagate cancellation.
- **Rate/Explore/Shared JS:** `ConcurrentRateLimiter` can be Runtime core, but `BaseSource.putConcurrent` is missing from current JS source object. Preserve source-keyed dynamic update/wait/cancel semantics. Explore requires sourceUrl-keyed 99-entry LRU, same mutable `InfoMap` identity and dirty-only snapshot persistence. `SharedJsScope` is Runtime core but current Rhino maps are not proven equivalent.
- **Context/config/resources/notifications/files:** Android ID, cache root, default/WebView UA, navigation policy, ReadBookConfig, ThemeConfig, resources, toast/log levels, external-cache path confinement, text/file helpers, ZIP/RAR/7z behavior, encoding and cleanup need separate typed owners. Do not move `appCtx`, AppConfig, generated App `R`, Room, FileUtils or ArchiveUtils implementations into Runtime.
- **UI/WebView/browser/verification/events:** five current RuntimeHostPort interfaces have zero App-main implementers. `HostUiAction` lacks import-confirm/deep-link flow and toast/log/config; HiddenWebViewRequest wrongly requires nonblank URL and JS although upstream allows nullable combinations; SourceVerificationHelp needs lock/single-flight/correlation/refetch/cancel semantics; ProductEventPort must preserve refresh ordering.
- **Batch content:** `BatchContentContext`/`BookHelp` save-token/version-fence path is absent; current `getBatchContext()` is null and `cacheContent()` fails closed. A separate typed `BatchContentPort` is required.
- **Execution lifecycle:** `SourceExecutionStatePort` must coordinate attach/clear across runtime instances and source replace/remove. The current FileBookSourceCatalog default invalidation callback is a no-op while SourceCatalogActivity separately calls `clearSourceState`; this is not one lifecycle owner.

No exact port signature is frozen merely because a candidate appears in the TSV. Freeze each signature only with its upstream owner/caller, argument/null/result/error semantics, threading/cancellation and lifecycle. The five existing UI/WebView/browser/verification/product declarations remain unimplemented.

### 10.6 Required contract tests and disposition

Before BookSource/BaseSource migration, add focused Runtime/App contract tests for:

1. Full 175-signature/77-annotation API inventory, overloads, default arguments, `@Keep`, and JVM/Rhino visibility.
2. `source === java === sourceApi`, `java.getSource() === source`, generic `getKey()`, URL-based BookSource equals/hashCode and all four concrete BaseSource implementers.
3. Owner visibility across modules; owner accessor is synthetic/hidden from JS; Room/Gson/Parcelable exclusions; attach after catalog decode, parcel restore and execution copy; `copy()` starts detached; `SourceNavigationBridge` delegates the original owner.
4. Same-source concurrency across multiple Runtime instances, cross-source isolation, lifecycle clear and no global service locator/ThreadLocal.
5. Login nullable/Unit/Boolean/AES and dynamic Login UI defaults; header/cookie ordering; persistent/session/WebView cookie parity; CacheManager LRU/TTL/global namespace; rate updates; Explore LRU/dirty-save; all HTTP options; hidden WebView cancellation/release; visible browser single-flight/refetch/cancel; verification-code callback exactly once; batch content save tokens; UI/event ordering and file/archive parity.

**Task 026L refresh result:** read-only addendum refresh completed at the current snapshot. The refresh also classified `:legado-runtime-poc` as quarantined evidence, not production dependency. No production source/build/test/manifest/schema/UI/Reader edit; no Gradle/compiler/build/test/CI/device/network action; no App compile retry. Evidence files are under `/root/stage0-runner/task026-clean/task026l-refresh-attempt1/` (`snapshot.txt`, `poc-quarantine.txt`, corrected port matrix and 64-row source hash manifest).

**Disposition:** Task 026L snapshot refresh complete as documentation-only. Phase A remains open. Phase B has begun only with Task 026M's three UI-model ownership changes and Runtime-only 6/6 test pass; no BookSource/BaseSource or source-engine migration. Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

**Next:** after this addendum/audit/overview commit, reread the master and third-category required list at the committed HEAD, then register Task 026P for the read-only exact typed Host Port signature freeze, including the public cross-module owner contract, all implementers and reattachment lifecycle. Do not begin BookSource/BaseSource production migration until 026P and a separately registered owner proof task pass.

## 10.7 Task 026AA/026AB reconciliation — SharedJsScope HTTP policy and source-entry binding

**Decision recorded 2026-11-26 (Task 026AC):** the user selected the upstream-compatible option for the SharedJsScope shared-JS-library fetch path only. This is an explicit, narrowly scoped compatibility/security exception.

### Shared-library HTTP adapter contract

- Add a dedicated App-owned shared-library HTTP adapter/client for the Runtime-owned SharedJsScope manager. The exact public port signature remains to be frozen from the caller; do not assume that this paragraph implements or wires it.
- Request identity is URL-only GET. Do not attach a BookSource/source ID, source CookieJar, source cookies, source headers, login/session state or source-specific request configuration. Preserve the pinned upstream behavior of consuming response body text without validating HTTP status, stripping UTF-8 BOM, honoring a declared Content-Type charset and falling back to the existing EncodingDetect behavior.
- **TLS policy selected:** trust all server certificates and accept all hostnames for this dedicated client; permit redirects from HTTPS to HTTP, matching the pinned upstream shared-library client behavior. This intentionally accepts certificate/hostname interception and HTTPS-downgrade risk for remote shared JS library downloads.
- Preserve the other already-audited upstream fetch characteristics unless a separately reviewed contract proves a necessary adaptation: per-call retry count zero; 15s connect/write and 60s read/call timeouts; normal and SSL redirects enabled; cancellation of the underlying OkHttp Call when the awaiting coroutine is cancelled. The existing SharedJsScope runBlocking boundary does not inherit the caller CoroutineContext; do not silently claim caller cancellation propagation that upstream does not provide.
- The trust-all SSLSocketFactory/X509TrustManager and permissive HostnameVerifier must be confined to this client instance. Do not install them on a process/global OkHttp client, SourceRequestClient, BadcatSourceHttpGatewayFactory, WebView, login, TTS, image fetch or any ordinary source request path. Do not add a permissive fallback to a strict client.
- **Ordinary source HTTP remains strict:** keep source-scoped cookie handling, URL/credential/header validation and the existing HTTPS-to-HTTP downgrade refusal in SourceRequestClient. No source request may select the shared-library client.
- Shared-library cache remains a separate adapter/namespace (cacheDir/shareJs, MD5(URL), 50,000,000-byte decimal LRU budget, no TTL for these calls); it is not the sourceId-keyed SHA-256/AtomicFile cache. Diagnostics remain separate Debug/AppLog channels. No generic HTTP/cache/logger adapter may erase these distinctions.

### Security and compatibility tests required before implementation acceptance

- Positive compatibility cases: valid HTTPS; self-signed/expired certificate; hostname mismatch; HTTPS redirect to HTTP; UTF-8 BOM and declared charset; no-charset EncodingDetect; non-2xx response body; timeout and cancellation.
- Isolation cases: assert the shared client is selected only for SharedJsScope library fetches; ordinary source requests still reject invalid certificates/hostname mismatch and HTTPS downgrade; source CookieJar/headers are not sent on shared-library fetch; no global OkHttp defaults or WebView TLS policy changed.
- Tests must distinguish the deliberate trust-all compatibility behavior from a security regression in normal source HTTP. No such tests or implementation were performed by Task 026AC.

### BaseSource attachment entrypoints reconciled from Task 026AB

There is no universal existing binder. The future reusable typed binder/facade must be invoked at all outer runtime/host boundaries, including:
1. Before source-bound search/explore/info/TOC/content/review/rule/URL/image/login/callback/task operations.
2. Before any retained direct BaseSource host-effect call that is not routed through the facade (header/login/login-info/pay/click/callback/AutoTask/image/MCP callsites).
3. Before SourceLoginJsExtensions and WebJsExtensions dispatch.
4. Before constructing a source-bound AnalyzeUrl (its initializer synchronously calls source.getHeaderMap()).
5. For BackstageWebView's own BookSource DAO re-query, attach the exact newly materialized original object before registering SourceNavigationBridge or WebJsExtensions.
6. Reattach after copy, Gson/JSON/JS-config import, legacy conversion, replacement and backup restore only when the object crosses a Runtime/host-effect boundary; do not attach every DAO projection/read.

Preserve each invocation's distinct java/source/sourceApi identity. Null-source AnalyzeRule/AnalyzeUrl and JsSourceConfig do not require a BaseSource sidecar. Source/library invalidation remains separate from active-script cancellation and product Cookie/Session/Variable/File cleanup.

**Gate and limits:** this is a documentation-only policy/addendum update. It does not create a port, client, sidecar, marker filter or facade. Task 026AB closes the static entrypoint inventory only; actual attachment, Android Room/Gson/Parcelize/copy behavior, active-script lifecycle, Badcat App ABI/Release R8, exact minimal Runtime dependency closure and dynamic parity remain NOT ESTABLISHED. No App compile retry under audit §24.119. Phase A remains open; broad BookSource/BaseSource migration remains NO-GO; Task 027 remains blocked; Stage-0 remains in_progress/not accepted; Stage-1 remains **NO-GO**; full compatibility remains **NOT ESTABLISHED**.
