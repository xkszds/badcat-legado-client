# Runtime-3.0 stage-0 architecture and dependency-closure audit

- Audit ID: Runtime-3.2f.49
- Audit status: **partial evidence inventory; stage-0 NOT accepted; stage-1 NO-GO**
- Badcat branch: `integration/legado-upstream-runtime`
- Starting source HEAD: `f6596e62b4c6abd180f83db62454a2caa0448b3c`
- Audit documentation HEAD before this report: `c42593519eec8da316794e46c57964234f1a6b4a`
- Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Evidence mode: Git tree/source/build-file inspection only. No build, test, APK, device, runtime, or public-source probe was run by this audit.

## 1. Executive decision

**NO-GO for Runtime-3.0 stage 1 implementation at this point.** The current tree demonstrates useful authentic slices (pinned rule model/Gson sources, pinned RuleAnalyzer parser sources, and a separately copied Rhino engine), but it does not yet contain the complete book-source execution closure or a functioning typed Host Port boundary for the full upstream lifecycle.

This is not a rejection of Runtime-3.0. It is a stop-before-implementation decision: first finish the symbol/caller/behavior inventory and choose a reproducible Android source boundary that can carry authentic upstream behavior without pulling in Legado product UI/Reader or replacing Badcat's UI/Reader.

The evidence does **not** support a complete-compatibility claim. In particular, `:legado-runtime` currently has no production Kotlin/Java source under its own source tree; its build script injects only seven upstream rule-model files plus `GsonExtensions.kt`. The actual search/detail/TOC/content services still live in the Android-free host projection module and the app adapter layer.

## 2. Repository and build topology

### 2.1 Pinned upstream repository

The recursive Git tree for the pinned upstream SHA returned 3,298 entries and was not truncated. Its Gradle entry points are:

- `settings.gradle`
- `build.gradle`
- `app/build.gradle`
- `modules/book/build.gradle`
- `modules/rhino/build.gradle`
- `gradle/libs.versions.toml`

The upstream app is an Android application module (namespace `io.legado.app`, Java/Kotlin toolchain 21) and directly depends on both `:modules:book` and `:modules:rhino`, plus AndroidX, Room, Gson, Jsoup, JsonPath, JsoupXpath, OkHttp, coroutines, Flexbox, Hutool/crypto, Bouncy Castle, archive helpers, and other app libraries. The exact transitive/runtime closure has **not** yet been resolved from Gradle dependency reports; this list is an inspected direct-dependency sample, not a complete lockfile.

The upstream `:modules:book` module is not a book-source model module. Its inspected source tree contains EPUB/Umd reader/parser classes (`me.ag2s.epublib.*`, `me.ag2s.umdlib.*`) and helper classes. It is not automatically included in the book-source runtime: each candidate dependency must be justified by actual source callers. No decision is made here to import the whole module.

The upstream `:modules:rhino` module contains the authentic script engine abstractions and HtmlUnit CoreJS implementation, including:
- `com.script.CompiledScript`, `ScriptBindings`, `ScriptBindingsExtensions`, `ScriptException`
- `com.script.rhino.RhinoScriptEngine`, `RhinoContext`, `RhinoContextElement`, `RhinoCompiledScript`
- `RhinoClassShutter`, `JavaObjectWrapFactory`, `RhinoWrapFactory`, protected/read-only Java wrappers, and XML/E4X implementation classes.

Its upstream build exposes `org.htmlunit:htmlunit-core-js:5.3.0-legado.4`, with coroutines, OkHttp and AndroidX collection dependencies. This is an authentic dependency candidate, but exact JS ABI/security/behavior parity still requires tests.

### 2.2 Current Badcat modules

The Badcat root includes `:app`, `:integration-contract`, `:legado-rhino`, `:legado-runtime`, and `:legado-runtime-poc`.

| Module | Observed role | Stage-0 finding |
|---|---|---|
| `:app` | Badcat Android application, adapters, storage and UI | Owns production wiring and Android services; it currently hosts many runtime-specific adapters. |
| `:integration-contract` | Kotlin/JVM host contracts, services and selected pinned parser source set | Not a full Android Runtime. It compiles selected upstream parser files and local platform-adapted bridges. |
| `:legado-rhino` | Android library containing copied upstream Rhino/HtmlUnit CoreJS files | Substantial authentic engine slice; compare file hashes/source provenance and runtime config against pin before treating it as exact. |
| `:legado-runtime` | Android library intended for authentic upstream runtime | Own source tree contains only AndroidManifest.xml. Build injects seven rule model files and GsonExtensions from upstream. It is not yet the full runtime. |
| `:legado-runtime-poc` | Bounded upstream source-closure experiments | Explicitly experimental; dependency set and source include list are not the production closure. |

Important build details:
- `:legado-runtime` is Android library, minSdk 26, Java/Kotlin 21; dependencies currently include Gson and AndroidX annotation. Its Kotlin compile task injects `BookListRule`, `ExploreRule`, `SearchRule`, `BookInfoRule`, `TocRule`, `ContentRule`, `ReviewRule`, and `GsonExtensions`. No BookSource/Book/BookChapter/BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions service closure is present there.
- `:integration-contract` is Kotlin/JVM. Its custom `pinnedLegado` source set compiles upstream `AnalyzeByJSoup.kt`, `AnalyzeByRegex.kt`, `RuleAnalyzer.kt`, and `SourceHtmlParser.kt`, plus local adapted JSONPath/XPath/bridge sources. The build comment explicitly leaves AnalyzeRule's app-coupled lifecycle as a separate integration task.
- `:legado-rhino` is an Android library; it packages the copied engine implementation and depends on HtmlUnit CoreJS, coroutines, OkHttp and AndroidX collection.
- `:legado-runtime-poc` includes a small upstream rule/Gson slice and selected dependencies; its own comment says unresolved app-internal imports are intentionally not stubbed. It is not evidence that the production runtime closure has been solved.

## 3. Pinned upstream source/API inventory

The following exact source locations were inspected. The inventory is intentionally representative and **not yet a 100% symbol/overload list**.

### 3.1 Models and source lifecycle

- `app/src/main/java/io/legado/app/data/entities/BaseSource.kt`: source-scoped lifecycle object; its behavior reaches login, source variables, Cookie, cache, JS/Rhino and request context. It is not a marker interface and cannot be replaced with a data-only DTO.
- `app/src/main/java/io/legado/app/data/entities/BookSource.kt`: persistent source entity and source execution state; includes the source URL/name/group/type, enable flags, Cookie-jar flag, headers, JS library, login URL/UI/check script, cover decoding script, book URL pattern, explore/search URLs, six rule groups, event/custom button and ordering/weight fields. Exact field/default/nullability/converter inventory remains open.
- `app/src/main/java/io/legado/app/data/entities/Book.kt`: book entity and reading configuration; inspected dependencies reach app database, BookHelp, ContentProcessor/AppConfig and reader helpers. Not safe to transplant as a standalone DTO.
- `app/src/main/java/io/legado/app/data/entities/BookChapter.kt`: chapter entity; inspected dependencies reach app database, AnalyzeUrl/RuleDataInterface, large-variable storage and Android application context. Preserve upstream chapter identity/index semantics.
- Rule model files under `app/src/main/java/io/legado/app/data/entities/rule/`: `BookListRule.kt`, `ExploreRule.kt`, `SearchRule.kt`, `BookInfoRule.kt`, `TocRule.kt`, `ContentRule.kt`, `ReviewRule.kt`, and nested UI/style models such as `RowUi.kt` and `FlexChildStyle.kt`. The current Badcat runtime module compiles only a selected subset.

### 3.2 Rule and URL execution

- `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt`: rule state/context and selection/replacement orchestration; inspected declarations include the rule-engine class and context methods. Exact public/protected API and every call site still need extraction.
- `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`: URL/request execution path; inspected code includes URL parsing, source-aware request configuration, script evaluation, response handling and async/coroutine context. Full overload/caller inventory remains open.
- `app/src/main/java/io/legado/app/model/analyzeRule/RuleAnalyzer.kt`, `AnalyzeByJSoup.kt`, `AnalyzeByRegex.kt`, `SourceHtmlParser.kt`: selected authentic sources are compiled in `:integration-contract`; JSONPath/XPath bridges are adapted local sources. This is parser evidence, not proof that all rule types/context semantics are complete.
- Six rule groups are represented in the source model: Explore, Search, BookInfo, TOC, Content and Review. Their full nested model/default/serialization and behavior coverage is not yet mapped to tests.

### 3.3 Rhino, JsExtensions and shared JS scope

- `app/src/main/java/io/legado/app/help/JsExtensions.kt` is 42,899 bytes in the pinned revision and declares the `JsExtensions` interface. Inspected methods include `getSource()`, `getTag()`, `getSourceNavigationContext()`, `getBatchContext()`, content-cache/refresh callbacks, `ajax`, `ajaxAll`, `ajaxTestAll`, `connect`, multiple `webView` / `webViewGetSource` / `webViewGetOverrideUrl` overloads, browser start/await callbacks, verification-code retrieval, script import, file download/cache, Cookie access, GET/HEAD/POST, encoding/Base64/hex/time/URI helpers and additional declarations later in the file.
- A first declaration scan found many overloads and a large API surface; the exact count/signature/annotation/return-nullability inventory has not yet been completed. Do not use an approximate count as API coverage.
- `app/src/main/java/io/legado/app/model/SharedJsScope.kt`: shared JS library scopes, compiled CryptoJS, weak-reference/LRU lifecycle, concurrent creation locks, cache files and secure random binding. Source identity/thread/coroutine behavior requires explicit parity.
- `app/src/main/java/io/legado/app/help/http/BackstageWebView.kt`: background WebView execution with pooled WebViews, navigation context, Cookie integration, JS injection, timeout/cancellation and resource release. This is a real book-source dependency for WebView/browser rule APIs; cannot be omitted by labeling it UI.
- `app/src/main/java/io/legado/app/model/login/LoginUiV2.kt`: login form protocol parsing/validation; distinct from full login screen/product UI. Host must preserve the protocol and callback/session behavior without importing Legado's product screen.

### 3.4 HTTP, Cookie and cache services

- `app/src/main/java/io/legado/app/help/http/CookieStore.kt`: persistent Cookie storage currently writes through upstream `appDb.cookieDao`, updates memory cache, supports WebView Cookie synchronization and Cookie parsing/merging. A host port must preserve domain normalization, null/empty handling, session Cookie, WebView Cookie and persistence semantics.
- `app/src/main/java/io/legado/app/help/CacheManager.kt`: contains `AppCacheManager`, `CacheManager` and `WebCacheManager`; general key/value, memory/disk/file and source-variable cache behavior. Must be separated from chapter-body cache and mapped by exact key/TTL/expiry behavior.
- `AnalyzeUrl` and `JsExtensions` are direct callers/bridges into source-aware HTTP behavior, request headers, timeout, Cookie and rate limiting. Full OkHttp client configuration, redirect, TLS, response/body/error behavior and all callers still need a dedicated scan.

## 4. Current Badcat integration/caller boundary

Observed current production classes:
- `:integration-contract`: `SourceRuntime`, `RuntimeHostPort`, `SourceRequestFactory`, `SourceHttpGateway`, `SourceRuleResolver`, `SourceSearchService`, `SourceExploreService`, `SourceBookDetailService`, `SourceTocService`, `SourceChapterContentService`, `LegadoRuleExpressionDispatcher`, `SelectorLegadoRuleRuntime`, `LegadoBookSourceCodec`, `SourceReadingEngine`.
- `:app`: `BadcatReadingEngineFactory`, `BadcatSourceHttpGatewayFactory`, `RhinoSourceScriptRuntime`, `RhinoSafeSourceJavaExtensions`, `RhinoSourceCookieExtensions`, `RhinoSourceCacheExtensions`, `RhinoEmbeddedUrlScriptEvaluator`, `RhinoLegadoJsReplacementEvaluator`, `SourceLoginSession`, `LegadoSourceLoginUiV2Service`, `DefaultProcessCookieBridge`, `SharedPreferencesSourceCookieJar`, `FileChapterContentCache`, `SourceScriptFileCacheStore`, `SharedPreferencesSourceScriptKeyValueStore`, and archive/conversion helpers.
- The host `LegadoBookSourceCodec` retains `rawJson` to avoid dropping unknown JSON fields, but it creates a host projection. Its `toEngineSource()` maps `sourceId` into host `baseUrl`; this is not the authentic upstream mutable `BookSource` or `BaseSource`.
- Current app wiring still constructs host service objects. The current production path is not shown to instantiate the authentic upstream `BaseSource` and call upstream `BookSource` lifecycle methods end-to-end.
- Existing host ports/projections do not yet demonstrate preservation of every upstream parameter, result type, null/default, exception, coroutine/thread context, cancellation, callback ordering, resource release or transaction boundary.

## 5. Candidate typed Host Ports (not yet approved as final API)

These are candidate boundaries inferred from actual upstream dependencies; signatures must be derived from call sites and not invented before the full caller scan.

| Host Port domain | Upstream dependency/evidence | Semantics that must be preserved |
|---|---|---|
| Source persistence / model access | BookSource, Book, BookChapter, DAOs | identity, field defaults/nulls, serialization, transactional updates, ordering, conflict behavior |
| HTTP transport | AnalyzeUrl, JsExtensions GET/HEAD/POST/ajax/connect | request method/body/headers, timeout, redirects, response status/headers/body, exceptions, cancellation, source context |
| Cookie/session | BaseSource, CookieStore, CookieManager, WebView CookieManager | domain rules, source isolation, session vs persistent cookies, WebView sync, clear/replace semantics |
| Login interaction | BaseSource login flow, LoginUiV2, browser/WebView | render/action protocol, secure state, callbacks, cancellation, persistence and login-check ordering |
| Browser/WebView/verification | BackstageWebView, WebJsExtensions, SourceVerificationHelp | navigation, injected JS, callback results, timeout, pooled resource release, UI dispatch |
| JS runtime/bridge | RhinoScriptEngine, JsExtensions, SharedJsScope | ABI/overloads, object wrapping, scope lifetime, thread/coroutine propagation, timeout, class-shutter/security, exception conversion |
| Cache/key-value/files | CacheManager, WebCacheManager, AppCacheManager, JsExtensions | key namespaces, TTL/expiry, memory/disk/file behavior, atomicity, cleanup and source-variable lifecycle |
| Source lifecycle | BaseSource and business callers | initialize/login/use/release, source variable state, rate limits, callbacks, cancellation and cleanup |
| Chapter content/cache | BookChapter, BatchContentContext, ContentSaveFence | (bookUrl,index) identity, URL alias ambiguity, batch context, save tokens and stale-write fencing |

## 6. Data ownership snapshot

| Domain | Observed current Badcat owner/path | Pinned upstream owner/behavior | Finding |
|---|---|---|---|
| BookSource | `FileBookSourceCatalog` + JSON codec/AtomicFile | BookSource entity/DAO + BaseSource runtime state | Separate model and storage; no demonstrated single-source migration. |
| Book | Host BookCandidate/BookDetail projections and reader state | Book entity/DAO + BookHelp/content helpers | Full model and ReadConfig not integrated. |
| BookChapter/TOC | In-memory TOC service projection and chapter identity helpers | BookChapter entity/DAO, index and URL identity | Host identity PoC exists; complete upstream fields/DAO behavior not integrated. |
| Progress | `BadcatReadingStateRepository` / SharedPreferences-backed progress | Book.ReadConfig, ReadBookConfig, ParagraphAnchor and reader helpers | Semantics and single-writer parity unresolved. |
| Chapter body cache | `FileChapterContentCache`, typed key and in-process save fence | upstream content-save/cache lifecycle | Host PoC is not a complete upstream CacheManager or durable fence. |
| Cookie/login/source variables | app SharedPreferences/session bridges and host adapters | CookieStore/DAO, WebView CookieManager, BaseSource/AppCacheManager | Ownership and persistence semantics differ; no migration/transaction proof. |
| Favorites/bookmarks/history | separate Badcat stores/repository paths | product-level upstream stores not required merely to run source rules | Keep Badcat product ownership; do not import Legado UI/Reader or product stores without caller evidence. |

## 7. Exclusions and inclusion rules

- **Legado UI/Reader/product navigation:** excluded by project requirement. Do not include Activity/Fragment/Adapter product screens as a shortcut to source execution. Login UI protocol and browser/WebView callbacks are not excluded when called by source rules; provide typed host implementations.
- **EPUB/Umd reader module:** `:modules:book` source tree is EPUB/Umd-centric. Exclude only after a complete caller/dependency scan proves the source runtime does not call it; current tree inspection suggests it is not the book-source core, but the call graph is not yet complete.
- **RSS/TTS/audio/video/product features:** not automatically included. If any symbol is called by JsExtensions or source rules, retain the behavior behind a host port or prove the API is outside the source-compatibility contract.
- **Room/AppDatabase:** do not import the entire upstream AppDatabase or schema as a shortcut. However, authentic source execution dependencies on DAO-backed Cookie/cache/model data must be preserved by typed ports or a reviewed minimal persistence boundary.
- **Browser/WebView/verification:** cannot be excluded wholesale because upstream JsExtensions exposes these source-facing APIs and BackstageWebView is a concrete caller.
- **BookSource/Book/BookChapter/BaseSource:** cannot be excluded as "just models"; they participate in source lifecycle and request/context behavior.

## 8. Blockers / unknowns before stage 1

1. Complete API inventory: every public/protected source-facing type, field, constructor, overload, annotation, nullability, default and serialization contract.
2. Complete caller graph: all callers of BaseSource, AnalyzeRule, AnalyzeUrl, JsExtensions, SharedJsScope, CookieStore, CacheManager, BackstageWebView, login and source verification.
3. Gradle closure: actual resolved dependency reports for the pinned upstream app and a candidate isolated runtime; transitive artifacts, native libs, generated code, manifest/resources, ProGuard/R8 rules, reflection and service loading.
4. Authentic model feasibility: BookSource/Book/BookChapter plus BaseSource and DAO-dependent behavior cannot currently be built in `:legado-runtime`; prior broad source-closure experiment failure is recorded elsewhere and must be re-opened with exact failing symbols before selecting a boundary.
5. Rule semantics: full AnalyzeRule/AnalyzeUrl parser/selector/regex/XPath/JSONPath/JS/URL-template/replacement/error semantics and all nested rule types need fixed-SHA differential fixtures.
6. Rhino ABI: complete JsExtensions signature list and Java↔JS wrapping/overload resolution/exception behavior; source-scoped JS library, shared scope, context propagation, cancellation and release tests.
7. Network/session: request config, TLS/redirect/cookies, per-source rate limiting, login, WebView Cookie sync, browser callbacks and verification lifecycle.
8. Business chain: search/explore pagination, detail, TOC pagination/alias, multi-page and batch content, content save fencing and error propagation.
9. Data ownership: BookSource/Book/BookChapter/progress/cache/Cookie/login/source-variable writer map, transaction, restart, backup/rollback and migration behavior.
10. License/provenance: exact file-level upstream provenance and GPL-3.0 compliance inventory for every copied/modified source and resource.
11. Reproducibility: no Gradle dependency graph, tests, APK or device evidence was generated as part of this read-only audit.

## 9. Stage-0 completion plan and recommendation

**Stage 0 remains in progress.** Do not start stage 1 source import or change Gradle/module boundaries yet.

Ordered remaining audit slices:
1. Generate exact symbol/field/overload/API inventory for BookSource/Book/BookChapter/BaseSource, all six rule groups, AnalyzeRule/AnalyzeUrl, JsExtensions and Rhino bridge.
2. Search all upstream call sites for each symbol and build the transitive call graph, including reflection/string-based lookups.
3. Resolve upstream Gradle runtime classpath and assets/resources/manifest/native/proguard/generated-code closure from exact pinned source; distinguish direct and transitive dependencies.
4. Scan every Badcat caller and adapter, including legacy readers/storage, to establish the current production execution path and all host data owners.
5. Build a side-by-side semantic matrix for source lifecycle, JS ABI, HTTP/Cookie/login/WebView, cache and business chain; mark each behavior exact/adapter/unknown.
6. Compare three viable boundaries using the evidence: (A) dedicated Android Runtime module with authentic source groups and typed Host Ports; (B) controlled upstream core module/source-set extraction; (C) hybrid source reuse with strict API/behavior parity. No option is preselected until it passes closure and caller proof.
7. Produce a reproducible stage-0 acceptance bundle and only then decide whether stage 1 can begin.

**Current stage-1 recommendation: NO-GO.** The missing evidence is architectural, not a minor implementation task. No production change is authorized by this audit.

## 10. Scope and safety confirmation

- No production Kotlin/Java, upstream source, Gradle/dependency, workflow, source fixture/rule, UI/Reader, database/schema, or release file was modified.
- The four paused public-source fixtures/rules and their security preflight were not read for execution, edited, or run.
- No remote-code JS security control was relaxed.
- No build/test/CI/APK/real-source/device pass is claimed.

## 11. API inventory slice 1 — source declarations (2026-11-24)

A source-level declaration scan was performed directly against the pinned raw files. Counts below are lexical Kotlin `fun` declarations (including overloads and extension helpers); they are **not** a count of JavaScript-exported methods or a coverage percentage.

| Pinned source file | Lines | Function declarations | Notes |
|---|---:|---:|---|
| `help/JsExtensions.kt` | 1,390 | 118 | 48 `@JavascriptInterface` annotations. Includes JS source bridge, HTTP, WebView/browser/verification, Cookie/cache/file/archive, crypto/encoding, font, URL and UI/config methods. |
| `data/entities/BaseSource.kt` | 436 | 30 | Source lifecycle, login UI/action, login headers/info, variables, concurrent rate, JS evaluation/shared scope. |
| `data/entities/BookSource.kt` | 358 | 37 | Source identity/equality, six rule accessors/serializers, group operations, batch support and login JS. |
| `data/entities/Book.kt` | 548 | 57 | Reading configuration, book metadata, progress fields, migration, persistence and bookmark conversion. |
| `data/entities/BookChapter.kt` | 210 | 14 | Chapter metadata, variables, identity/equality, URL resolution and content-related helpers. |
| `model/analyzeRule/AnalyzeRule.kt` | 1,060 | 29 | Rule parsing/selection, context, replacement, JS, source and batch context propagation. |
| `model/analyzeRule/AnalyzeUrl.kt` | 1,053 | 53 | URL templates/options, request method/headers/body/timeout/redirect/DNS/WebView, sync/async response and upload paths. |

Important API groups visible in the exact `JsExtensions.kt` declarations:
- Source and execution context: `getSource`, `getTag`, `getSourceNavigationContext`, `getBatchContext`, `cacheContent`, `refreshBookInfo`, `refreshBookToc`, `refreshContent`.
- HTTP: `ajax`, `ajaxAll`, `ajaxTestAll`, `connect`, `get`, `head`, `post`, including overloads for timeout, headers and rate-limit behavior.
- Browser/verification: `webView`, `webViewGetSource`, `webViewGetOverrideUrl`, `startBrowser`, `startBrowserAwait`, `getVerificationCode`, `showBrowser`, `openUrl`.
- Source persistence/helpers: `getCookie`, `cacheFile`, `downloadFile`, file read/write/delete, archive extraction and ZIP/RAR/7z access.
- Runtime helpers: Base64/hex, byte/string conversion, URI/HTML conversion, simplified/traditional Chinese conversion, font query/replacement, source locks/single-flight/tick, theme/read config access.

The model declaration scan confirms that the host codec's currently modeled source fields are not the entire upstream model contract. Upstream `BookSource` also exposes behavior methods and mutable fields, and `Book` includes a nested `ReadConfig`/ `ParagraphAnchor` contract. A field-by-field default/null/annotation/serialization matrix and ABI export rules remain to be generated.

**Limitations of this slice:** it does not enumerate Java annotations/overload resolution in Rhino, reflected method lookups, every caller, generated accessors, inherited methods, or all rule-model nested properties. The 48 annotated methods must be reconciled with the remaining methods reachable through Rhino object wrapping and source rules; annotation count alone is not the JS ABI count. This slice narrows the inventory work but does not close it.


## 12. API inventory slice 2 — model fields, rule groups and contracts (2026-11-24)

All paths below are pinned to `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. The scan records source declarations and constructor defaults as written; it is not a generated ABI schema and does not prove serialization equivalence.

### 12.1 Book-source model and rule groups

- `app/src/main/java/io/legado/app/data/entities/BookSource.kt` — primary identity is `bookSourceUrl: String = ""`; `bookSourceName: String = ""`; nullable group and URL pattern; type defaults to text (0); order/enabled/explore flags; nullable JS library, Cookie-Jar flag (nullable but default `true`), concurrency/header/login fields; login-check and cover-decode JS; comments/variables; update/respond/weight fields; explore/search URLs and rule objects; book-info/TOC/content/review rule objects; `mainJs`; event-listener and custom-button flags. It is a `Parcelable` Room entity with a non-unique URL index and primary key. Equality/hash are based on `bookSourceUrl`.
- Behavioral methods are part of the contract, not just DTO fields: `getSearchRule`, `getExploreRule`, `getBookInfoRule`, `getTocRule`, `getContentRule`, grouping methods, invalid-group cleanup, source comparison, `isJsSource`, `supportContentBatch`, `contentBatchSize`, and `getLoginJs`.
- The six serialized rule groups and pinned paths:
  - `data/entities/rule/ExploreRule.kt`: book list, name, author, intro, kind, last chapter, update time, book URL, cover URL, word count.
  - `SearchRule.kt`: same book-list fields plus `checkKeyWord`.
  - `BookInfoRule.kt`: init, name, author, intro, kind, last chapter, update time, cover URL, TOC URL, word count, rename permission and download URLs.
  - `TocRule.kt`: pre-update JS, chapter list/name/URL, format JS, volume/VIP/pay flags, update time and next-TOC URL.
  - `ContentRule.kt`: content/sub-content/title, next-page URL, WebJS/source regex/replacement regex, image style/decode, pay action, callback JS, batch rule and optional maximum batch size. Its custom `toString()` deliberately preserves a legacy textual representation and conditionally appends batch fields; this is a compatibility-sensitive behavior.
  - `ReviewRule.kt`: review URL/avatar/content/time/quote; vote/post/delete URLs; enabled flag; summary URL/list/paragraph/count rules; detail pagination/list/id/avatar/name/badge/content and reply list/id/avatar/name/badge/content rules.
- Each rule model has a custom Gson `JsonDeserializer` accepting either a JSON object or a JSON primitive containing serialized JSON text; other JSON kinds return null. Converters in `BookSource.Converters` serialize/deserialize each group separately. A parity suite must cover both shapes, null, missing fields, unknown fields, and legacy `ContentRule.toString()`.

### 12.2 Book and chapter data contracts

- `Book.kt` is a `Parcelable` Room entity with primary key `bookUrl` and unique index `(name, author)`. Constructor fields include TOC URL, origin/name, name/author/kind, custom tag/cover/intro, source cover/intro, charset/type/group, latest chapter metadata, update/check counters, chapter count, current chapter title/index/volume/index-in-volume/position/time, word count, update flag, ordering, source variable JSON, `ReadConfig?`, sync time and persisted cover URL. Several persisted fields use explicit Room defaults; timestamp defaults use `System.currentTimeMillis()`, which is not a static schema default.
- `Book.ReadConfig` includes reverse TOC/display, TOC expanded, nullable page animation, re-segmentation, image style, nullable replace-rule override, deleted-tag bitset, TTS engine, long-chapter split, simulation, nullable start date/chapter, daily chapter count, audio credits/play mode/speed/global-skip, manual replacement-rule IDs and optional paragraph anchor. Nested `ParagraphAnchor` has chapter URL, item ID and chapter position. These fields are reader/product-adjacent but are embedded in the upstream Book model; their execution/data ownership needs a typed compatibility decision rather than silently dropping them.
- `BookChapter.kt` constructor fields: URL/title, volume flag, base URL, book URL, index, VIP/pay flags, nullable resource URL/tag/word count/start/end/fragment IDs/variable/image URL. It also has transient or derived state including `variableMap`, `titleMD5`, and chapter helper methods; equality/hash are URL-based. A URL-only identity projection is insufficient where duplicate chapter URLs or book scoping matter.
- `BaseBook.kt` extends `RuleDataInterface` and defines mutable name/author/bookUrl/kind/wordCount/variable plus infoHtml/tocHtml. Its default variable write persists JSON through the variable map; large variables route through `RuleBigDataHelp`.
- `RuleDataInterface.kt` defines variable-map ownership and the 10,000-character split: null removes normal and big values; short values stay in the map and clear big storage; long values leave the map and route to big storage. `getVariable` returns normal, then big, then empty string. This threshold and persistence side effect are semantic contract, not a generic map adapter.

### 12.3 BaseSource lifecycle surface

- `BaseSource.kt` declares nullable mutable `concurrentRate`, `loginUrl`, `loginUi`, `header`, `enabledCookieJar`, and `jsLib`; identity methods `getTag/getKey/getSource`; login UI v2 detection/render/action; login JS and form detection; `login()`; header map construction and login-header access/update; login-info read/write/remove; source variable set/put/get; keyed variable put/get; explore refresh; JS library refresh; concurrent-rate update; and `evalJS`.
- The source contract includes ThreadLocal login-info re-entry protection, cancellation propagation while parsing headers, source-scoped login/header state, and Rhino evaluation. Implementations must preserve nullable semantics, thread behavior, cancellation, and side effects, not only method names.

### 12.4 AnalyzeRule / AnalyzeUrl / JS bridge surface

- `AnalyzeRule.kt` constructor carries nullable `RuleDataInterface`, nullable `BaseSource`, pre-update-JS and book-info context flags. Mutable execution state includes chapter/next chapter/batch context/content/base URL/redirect URL/JSON-vs-text mode, selector parser caches, compiled script caches, local bindings, weak top-scope reference, evaluation count and coroutine context. Public entry points include content/base/redirect setup; string/list/element extraction; source-rule splitting; rule name; parameter count; key/value/local variable access; JS evaluation; source/batch context access; AJAX; and extension setters for coroutine context, rule data, next chapter, chapter and batch context.
- `AnalyzeUrl.kt` constructor includes URL template, key/page/speech inputs, base URL, source, rule data, chapter, read/call timeouts, coroutine context, optional headers, login-header policy, info map and extra params. It exposes mutable URL/type/server ID and header map, while request state includes body, charset, method, proxy, timeout configuration, redirect policy, retry, WebView/WebJS/bodyJS, DNS IP, Cookie-Jar decision, domain, WebView delay and concurrency limiter. `UrlOption` mutators/accessors cover method, charset, origin, retry, type, WebView, headers, body, WebJS, timeout, redirects, DNS, JS, body JS, server ID and WebView delay. Response surfaces include string/byte/input-stream/OkHttp response and Glide URL paths.
- `JsExtensions.kt` is an interface extending `JsEncodeUtils`, with 1,390 lines and 118 lexical `fun` declarations. Method groups include source/batch context, cache writes, refresh events, AJAX/connect/bulk requests, WebView and browser flows, verification, Cookie/cache/file/archive access, GET/HEAD/POST, byte/string and Base64/hex/URI/HTML helpers, font query/replacement, source synchronization/locks, read config and theme access. There are 48 explicit `@JavascriptInterface` annotations in this file, but Rhino wrapping and non-annotated interface methods mean this is not a complete JS ABI count.
- `AnalyzeUrl` and `AnalyzeRule` implement `JsExtensions`; `BaseSource` also extends it. The interface is therefore a cross-cutting host bridge, not a self-contained helper class.

### 12.5 Source blob identifiers and limitations

Pinned blob SHAs observed during this scan:
- `BookChapter.kt`: `9798267c77c9e14d0a673a822930491cd6e44cc1`
- `BaseSource.kt`: `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa`
- `BookSource.kt`: `7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c`
- `Book.kt`: `05024a6c71404eb49c23cd1df0482df365bc63fe`
- `AnalyzeRule.kt`: `7591a026a072ac8a6fe4902add0795812fe0a0a6`
- `AnalyzeUrl.kt`: `d898c88dfd844d73f06d9396d0b46f39939f7e6c`
- `JsExtensions.kt`: `f6808edd2a0d312e8bd13649935dea444cb82581`

This slice does not yet provide all generated Java/Kotlin accessors, every constructor overload at JVM bytecode level, every annotation/inherited method, all Gson adapter registrations, Room converter behavior under malformed input, or complete callers/reflection. The caller graph and resolved dependency/resource/native/R8 closure remain open. No claim of ABI completeness or runtime compatibility is made.


## 13. Caller graph slice 1 — source business chain and platform bridges (2026-11-24)

GitHub code search was run against the exact pinned repository revision using symbol/call-expression queries. Results below are discovery evidence, not a complete static call graph: search matches include declarations, tests, docs and incidental references, and the GitHub code-search index is not a compiler call hierarchy.

### 13.1 Direct business-chain callers

| Source entry / class | Observed pinned caller paths | Confirmed role |
|---|---|---|
| `AnalyzeRule(` | `model/webBook/BookList.kt`, `BookInfo.kt`, `BookChapterList.kt`, `BookContent.kt`, `model/webBook/WebBook.kt`; also `model/rss/Rss.kt`, `RssParserByRule.kt`, `ReviewRuleParser.kt`, `DictRule.kt`, `BookCover.kt`, `DirectLinkUpload.kt`, `BatchContentContext.kt`, reader/review paths and tests | Search/explore result parsing, book details, TOC, content, pre-update JS, batch processing and adjacent non-book-source features all construct the same analyzer. |
| `AnalyzeUrl(` | `model/webBook/WebBook.kt`, `BookChapterList.kt`, `BookContent.kt`, `model/jsSource/JsSourceEngine.kt` indirect request helpers, `model/BookCover.kt`, `help/JsExtensions.kt`, plus browser, Glide, local-book, audio/video, RSS, review, WebDAV and UI option paths | URL template parsing and request/response handling are reused outside the core text-book chain; the source-runtime boundary must retain source-facing request semantics without blindly importing all product callers. |
| Source lifecycle | `model/webBook/WebBook.kt` receives `BookSource`; `BookList.kt`, `BookInfo.kt`, `BookChapterList.kt`, `BookContent.kt` pass it to AnalyzeRule/AnalyzeUrl; `BaseSourceExtensions.kt`, `JsSourceEngine.kt`, `SourceNavigationBridge.kt` supply scope/context helpers | The runtime business chain is a composition of model, parser, URL and host-service behavior, not a single analyzer class. |

The concrete high-level business sequence is visible in these pinned files:
- `model/webBook/WebBook.kt`: search, explore, book-info, chapter-list, content, batch-content and precise-search suspend/async entry points; each constructs AnalyzeUrl for request work and AnalyzeRule for rule execution.
- `model/webBook/BookList.kt`: creates AnalyzeRule with the source and book/rule data to parse list results.
- `model/webBook/BookInfo.kt`: creates AnalyzeRule with Book and BookSource to parse detail metadata.
- `model/webBook/BookChapterList.kt`: builds AnalyzeUrl for TOC network calls and AnalyzeRule for chapter extraction and next-page handling.
- `model/webBook/BookContent.kt`: creates AnalyzeRule and AnalyzeUrl for chapter body and multi-page content; has a distinct batch-content path.
- `model/jsSource/JsSourceEngine.kt`: exposes source and batch context; its ScriptBindings install `CookieStore` and `CacheManager` objects, and it normalizes Rhino Scriptable results.

### 13.2 Host-service and lifecycle call edges

- `BaseSourceExtensions.kt` is the shared-scope bridge. Code search found `getShareScope` and `getSharedGlobalStateKey` in BaseSource, AnalyzeRule, AnalyzeUrl and JsSourceEngine as well as the extension implementation. This confirms source-scoped shared JS state is a cross-cutting dependency.
- `SourceNavigationBridge.kt` wraps a BaseSource via delegation and executes navigation-sensitive calls in the supplied coroutine context; it gates login/openUrl/video navigation using the source-navigation suppression policy. A Host Port must preserve context and suppression behavior, not merely expose a Browser callback.
- `BackstageWebView.kt` is called by JsExtensions and both analyzers. It installs the WebCacheManager JS interface, wraps BaseSource with the navigation context, writes the webview result through CacheManager, syncs CookieStore into WebView, enforces timeout and owns WebView destruction/cleanup. This is a required source-facing platform service where invoked by source rules.
- `CookieStore.kt` implements the CookieManager interface; its state is stored through CacheManager memory keys for domain cookies and session cookies, with set/replace/restore/merge/get/remove/map conversion and clear operations. CookieManager and WebView login paths are additional callers. Domain/key/session semantics need explicit differential tests.
- `CacheManager.kt` separates AppCacheManager, CacheManager and WebCacheManager. CacheManager has typed memory/disk/file operations and TTL parameters; WebCacheManager delegates to it. CacheManager is referenced by BaseSource, JsExtensions, AnalyzeUrl/AnalyzeRule, BackstageWebView, CookieStore and additional product subsystems. A single undifferentiated cache port would lose namespaces and storage distinctions.

### 13.3 Search evidence and limits

Pinned GitHub code-search query results (exact repository pin in query):
- `"AnalyzeRule("`: 22 matching files.
- `"AnalyzeUrl("`: 38 matching files.
- `"getShareScope("`: 5 matching files.
- `"getSharedGlobalStateKey("`: 5 matching files.
- `"BackstageWebView("`: 6 matching files.
- `"CookieStore"`: 19 matching files.
- `"CacheManager"`: 43 matching files.
- `"BaseSource"`: 50 matching files; `"JsExtensions"`: 44 matching files.

The result sets include the core business classes listed above plus RSS, dictionary, review, local book, audio/video, browser, Glide, UI/editor, test and documentation consumers. This is evidence that the analyzers and JS bridge have broader callers than text-book search alone. It is **not** proof that all those product features must be imported: each call edge must be traced to the symbol actually used, and product-only callers may remain outside the runtime if source rules do not reach them.

### 13.4 Open caller-graph blockers

1. Enumerate call expressions, extension receivers, constructors, function references and reflection/string-based dispatch for each source-facing method; distinguish production, tests, docs and product-only paths.
2. Trace AnalyzeRule's selector/parser helpers (XPath, Jsoup, JSONPath, regex, source-rule splitting, JS compilation) to exact source files and transitive dependencies.
3. Trace AnalyzeUrl's request execution into HTTP helper, Cronet/proxy/OkHttp, cookie jar, WebView and response adapters, including timeout/cancellation/redirect behavior.
4. Trace every JsExtensions method to implementation owner and classify it as required source API, conditional source API, or product-only API; reconcile explicit annotations with Rhino interface wrapping and generated methods.
5. Trace CookieStore/CacheManager/SharedJsScope persistence and cleanup through DAOs, AppCacheManager and app lifecycle; record writer/readers, transactions and restart semantics.
6. Repeat caller search for Badcat at exact HEAD using a complete repository tree plus source retrieval; GitHub code search returned zero with incomplete index for the Badcat repository, so that zero is not evidence of no callers.

**Status:** caller graph slice 1 is partial discovery evidence only. It does not close the caller/reflection blocker, does not establish dependency closure, and does not change the stage-1 NO-GO decision.


## 14. Gradle, resource, manifest and shrinker closure slice 1 — static declarations only (2026-11-24)

The following is a static inspection of the exact pinned Gradle and packaging source. No Gradle configuration or dependency-resolution task was executed during this read-only audit; therefore this is **not** a resolved runtimeClasspath graph.

### 14.1 Project topology and module edges

- `settings.gradle` includes exactly `:app`, `:modules:book`, and `:modules:rhino`. The repository tree at the pinned SHA is complete (3,298 entries, `truncated=false`).
- `:modules:rhino` is an Android library (namespace `com.script`, minSdk 21, Java/Kotlin toolchain 21). Its direct declared dependencies are `api org.htmlunit:htmlunit-core-js`, plus implementation dependencies for coroutines-core, OkHttp and AndroidX collection. Its consumer rules keep the `org.htmlunit.corejs.javascript.**` runtime package and suppress several optional JDK-linked warnings. The old local `rhino-1.7.14.jar` exists in the tree, but the fileTree dependency is commented out; the declared API is the HtmlUnit CoreJS artifact.
- `:modules:book` has only AndroidX annotation as a declared dependency and its source tree is EPUB/Umd plus supporting ZIP/PFD classes. Code search for EpubReader/UmdReader finds app-side local-book consumers (`model/localBook/EpubFile.kt`, `UmdFile.kt`) and no source-rule/analyzer caller in the result set. This is positive evidence for excluding this module from the source-runtime closure, subject to the remaining exhaustive caller and reflection scan.
- The app module depends on both modules and directly declares a much broader product classpath. It also owns Room/KSP, source parser libraries (Jsoup, JsonPath, JsoupXPath), OkHttp/Brotli/Cronet, Gson, Hutool crypto, BouncyCastle, archive, Glide, AndroidX WebKit, media/audio/video, app UI, and other product features. Reusing app classes wholesale therefore risks importing the entire product graph into Runtime.

### 14.2 Source-facing dependency pressure

- Pinned `AnalyzeRule.kt` imports AndroidX Keep, Jsoup nodes, HtmlUnit CoreJS Scriptable/NativeArray/NativeObject/TopLevel, Apache Commons Text, Rhino engine/bindings, Gson, URL/regex and coroutine context/timeout helpers; it constructs XPath, Jsoup and JSONPath analyzers and calls BackstageWebView.
- Pinned `AnalyzeUrl.kt` imports Android Base64, Media3 MediaItem, GlideUrl, Hutool codecs, Rhino bindings, OkHttp request/response/body types, Cronet and request helpers, CookieManager/CookieStore, BackstageWebView, Glide headers, ExoPlayer helper and multiple AppConfig/NetworkUtils helpers. The class therefore has media/image and app-service references even though its primary role is URL parsing/request execution.
- Pinned `JsExtensions.kt` imports AppCompatActivity, WebSettings, app source-verification/import/open-url UI, bottom WebView dialog, app config/theme, archive/file utilities, CookieStore, BackstageWebView, event bus, font helpers, Android context and other app-scoped facilities. This interface is not currently an isolated platform-neutral ABI.
- Pinned `BaseSource.kt` imports Android JavascriptInterface, Hutool AES, ScriptBindings/Rhino, AppConst/AppLog/AppConfig, CacheManager, CookieStore, crypto helpers, shared scope, LoginUiV2 and Gson. Its lifecycle methods directly touch app-owned services.
- These imports demonstrate why copying only the model/analyzer files into a fresh module cannot satisfy closure. A candidate runtime must either bring authentic source groups and their actual transitive behavior or replace each platform dependency with a typed Host Port while preserving behavior; deleting imports/methods is not acceptable.

### 14.3 Variants, generated code, resources and native packaging

- App build uses compile SDK from root configuration, minSdk 23, targetSdk 36, Java/Kotlin 21, Room KSP schema generation, Glide KSP, view binding and BuildConfig. The only declared product flavor in the inspected block is `app`; debug disables minification, release enables R8 and resource shrinking.
- `app/build.gradle` applies `download.gradle` and `cronet-loader.gradle`. The latter uses ASM 9.8 at build time and rewrites a pinned Cronet implementation JAR into a build output; it validates exact Cronet version and original JAR SHA before adapting dynamic native-library loading. The download script defines Cronet Java JARs and ABI-specific native libraries; the tree includes seven Cronet JARs under `app/cronetlib`. These are a separate network/native packaging closure and cannot be assumed to be unnecessary without examining the selected runtime request stack.
- App packaging excludes selected META-INF/proto/source/kotlin-builtins resources and preserves selected JNI symbols; it also sets test schema assets. The app manifest has many product activities/services/providers/permissions, including source login, browser, verification, file import, media/audio/video, book cache/export and MCP services. It must not be copied wholesale into Badcat; only source-runtime-required services/permissions/manifest metadata should be represented through explicit host integration.
- App ProGuard rules explicitly keep subclasses of JsExtensions, all data entities, CookieStore, CacheManager and StrResponse; parser libraries and selected serialization/reflection targets are also preserved. The Rhino module's consumer rules separately keep HtmlUnit CoreJS JavaScript classes. This is concrete evidence that reflection/JS exposure and model serialization depend on shrinker configuration.
- The tree contains separate `app/src/cronetRuntime` and `app/src/pdfRuntime` instrumentation manifests; these are test/runtime verification source sets, not ordinary production app manifest components. The app's `cronet-loader.gradle`, Cronet jars, native libraries and proguard rules require a dedicated runtime dependency decision.

### 14.4 Static closure inventory and unresolved items

| Area | Evidence at pinned SHA | Current conclusion |
|---|---|---|
| Gradle modules | settings.gradle; app, modules/book, modules/rhino build.gradle | Topology known; source runtime still app-coupled |
| Rhino engine | modules/rhino/build.gradle, consumer-rules.pro, source tree | Direct dependency surface identified; resolved transitives not generated |
| Rule parsers | app build deps: Jsoup, JsonPath, JsoupXPath; analyzer imports | Direct declarations identified; transitive graph unresolved |
| HTTP/Cronet | OkHttp, Brotli, local Cronet JARs, dynamic adapted JAR, native .so and proguard | Packaging closure complex; selected runtime graph not resolved |
| Persistence | Room runtime/ktx/KSP, generated schemas, entities/converters | App database is not automatically a source-runtime dependency; DAO caller closure still open |
| JS/reflection | JsExtensions keep rules, Rhino consumer rules, entity keeps | Shrinker requirements visible; all reflective names and generated accessors not inventoried |
| Resources/manifest | app main manifest, app packaging excludes, assets, Cronet scripts | Product manifest/resources too broad for wholesale reuse; exact minimal runtime resources not yet isolated |
| Build variants | debug/release, app flavor, instrumentation source sets | Exact selected variant dependency resolution and merged manifest not obtained |

**Stage-0 blocker:** obtain exact pinned resolved runtimeClasspath and compileClasspath reports for relevant variants, artifact coordinates/versions/checksums, transitive native libraries, generated source/schema outputs, merged manifest, resource merge inputs, R8 configuration and kept/reflected class inventory. Static declarations alone cannot determine what the APK actually packages.

**Status:** dependency closure slice 1 is partial. It confirms that the current upstream app module is not a safe minimal runtime boundary and that `:modules:book` is local-file product functionality rather than demonstrated source-rule core. It does not establish the final boundary or allow stage 1.


## 15. Badcat production callers and data owners — exact HEAD snapshot (2026-11-24)

- **Badcat inspected HEAD:** `dc2a88330a3fcacf4ee26b656cff477bfd13bf5f` (documentation-only descendants of the registered source baseline); recursive tree returned 551 entries, `truncated=false`.
- **Current modules and source inventories:** `:legado-runtime` contains README/build/consumer rules/manifest and a test, but no production Kotlin/Java source. `:integration-contract` contains host models/services, parsers, transport, selector/URL adapters and candidate ports. `:legado-rhino` contains copied Rhino engine sources plus a few local sandbox extensions. App runtime adapters are under `app/src/main/java/app/badcat/client/runtime/`; rule-model copies also exist under app's `io/legado/app/data/entities/rule` tree.
- **Dedicated runtime build script:** `legado-runtime/build.gradle.kts` declares Gson and AndroidX annotation, then injects exactly seven upstream rule model sources plus `GsonExtensions.kt` into Debug/Release Kotlin compilation. It does not inject upstream BookSource, Book, BookChapter, BaseSource, AnalyzeRule, AnalyzeUrl, JsExtensions, SharedJsScope, CookieStore or CacheManager. README explicitly calls this only the Runtime-0.1 first compile/contract gate and lists those areas as blockers.
- **Host contract boundary:** `integration-contract/.../RuntimeHostPort.kt` contains typed context/error/cancellation, hidden WebView and browser/verification request/result shapes. These are useful candidate Host Ports; the report does not treat their declarations as proof that every upstream API has a production implementation.
- **Host execution architecture:** `SourceRuntime.kt` defines host `SourceRequestClient`, request/response DTOs, six `SourceRuleStage` values, a `LegadoRuleRuntime.evaluate(...): EngineResult<List<Map<String,String>>>` contract and a separate `SourceScriptRuntime`. Its default script runtime returns `UNSUPPORTED_RULE` for script evaluation and persistent variables. This is explicit fail-closed behavior, not full upstream execution.
- **Projection boundary:** `LegadoBookSourceCodec.kt` retains `rawJson` to avoid dropping unknown imported fields, but `LegadoBookSource.toEngineSource()` maps to host `BookSource` and assigns `sourceId` to host `baseUrl`. It is not an authentic upstream `io.legado.app.data.entities.BookSource` instance or its BaseSource lifecycle.
- **Rhino bridge evidence:** `RhinoSafeSourceJavaExtensions.kt` has a large hand-authored surface. It returns `Map<String, Any?>` for `getSource()`, returns null for `getBatchContext()`, and returns `EmptyCoroutineContext` for `getSourceNavigationContext()`; these differ in type/semantics from the pinned upstream interface contract. The runtime constructor also has fail-closed defaults for unconnected HTTP/Cookie/Android UI operations. These are deliberate safety boundaries, but must be classified as unsupported rather than counted as parity.
- **Production graph:** `BadcatReadingEngineFactory.kt` constructs the host FileBookSourceCatalog, OkHttpSourceRequestClient, SourceLoginSessionRequestClient, RhinoSourceScriptRuntime, BadcatSourceHttpGatewayFactory, and the five host search/explore/detail/TOC/content services. Search and book screens call this factory. They do not show an end-to-end instantiation of upstream WebBook/BookList/BookInfo/BookChapterList/BookContent with authentic BookSource/BaseSource.
- **Request semantics to verify:** the factory supplies custom HTTP callbacks and applies bounded timeout/retry/response-size policies; `BadcatSourceHttpGatewayFactory` explicitly documents that full bodyJs, WebView and upstream Java host APIs remain unsupported. These safeguards must not be confused with equivalence to upstream AnalyzeUrl/JsExtensions semantics.
- **Temporary catalog UI:** `SourceCatalogActivity.kt` is explicitly labeled a temporary source-catalog utility screen; it directly creates its own RhinoSourceScriptRuntime, SharedPreferences cookie/session/variable stores and FileBookSourceCatalog. This is separate from Badcat's original UI boundary and requires product/UI acceptance outside this audit before any release claim.

### 15.1 Badcat data ownership matrix — observed writers

| Domain | Observed Badcat implementation | Owner/writer evidence | Remaining parity issue |
|---|---|---|---|
| BookSource catalog | `FileBookSourceCatalog` | app-private JSON file/AtomicFile; import/export/enable/remove operations | Host projection/rawJson, no upstream entity/DAO semantics or transactional cross-domain proof |
| Book/Book detail | host `BookCandidate`/`BookDetail` and source service results | host service pipeline; no complete upstream Book entity owner shown | missing upstream model fields, ReadConfig and DAO helpers |
| BookChapter/TOC | host `BookChapter` and `SourceTocService` | service result/projection and host chapter identity helpers | upstream metadata, URL alias/duplicate semantics and persistence not proven equivalent |
| Reading progress | `BadcatReadingStateRepository` | SharedPreferences-backed last chapter/scroll state | upstream Book.ReadConfig/ReadBookConfig/ParagraphAnchor semantics differ; no migration proof |
| Favorites/bookmarks/history | same reading-state repository and separate favorite stores | host JSON/SharedPreferences ownership | product ownership should remain Badcat; parity with source runtime not established |
| Chapter body cache | `FileChapterContentCache` | app-private file cache, digest-based key and save token/fence | not upstream CacheManager/content-save lifecycle; persistence/restart/eviction parity unproven |
| Cookie/session | `SharedPreferencesSourceCookieJar`, login session store | app-private SharedPreferences and OkHttp CookieJar adapter | upstream CookieStore/DAO/WebView session merge/restore semantics require differential tests |
| Source JS variables/files | `SharedPreferencesSourceScriptKeyValueStore`, `SourceScriptFileCacheStore` | app-private source-scoped stores and bounded file root | not upstream AppCacheManager/RuleBigDataHelp semantics; large variable behavior unresolved |
| Replacement rules | `FileReplaceRuleCatalog` | separate Badcat-owned catalog | preserve Badcat ownership; verify rule serialization/application parity independently |

**Assessment:** Badcat currently has explicit host-owned data stores and typed service contracts, but the source runtime remains projection-based and has several fail-closed API gaps. There is no evidence of a single-source migration or full data-owner/transaction parity with upstream.

### 15.2 Remaining Badcat audit work

1. Trace every production call from Badcat activities/view models to SourceReadingEngine services and adapters, including alternate construction paths and legacy stores.
2. Inventory the exact host model fields against upstream fields and all reader/UI consumers; identify values silently dropped, recomputed or synthesized.
3. Trace all writes/readers for source catalog, progress, chapter cache, Cookie/session, JS variables/files, bookmarks/history/favorites and replacement rules; identify transaction and cache invalidation boundaries.
4. Determine whether any current adapter API is used as a substitute for an upstream public method with different signature, return type, default, nullability or error behavior.
5. Establish a per-symbol semantic parity matrix before selecting Host Port signatures.

**Status:** Badcat production caller/data-owner slice is partial but identifies the current architecture as host-projection based. Stage-0 remains incomplete; stage-1 remains **NO-GO**.


## 16. Side-by-side semantic parity matrix — evidence-based first pass (2026-11-24)

Status meanings:
- **Authentic slice** — pinned upstream source is present, but this does not imply complete caller/dependency closure.
- **Host adapter / partial** — Badcat has a host implementation, but exact upstream semantics have not been demonstrated.
- **Missing / blocker** — required upstream behavior is not integrated or a concrete API gap is observed.
- **Unknown** — insufficient evidence to classify safely.

| Capability | Pinned upstream contract/evidence | Badcat current implementation/evidence | First-pass parity state |
|---|---|---|---|
| BookSource model | Room `BookSource`, BaseSource inheritance, six rules, source methods, identity and converters | Host `LegadoBookSource` + `rawJson`; maps into host `BookSource`; only seven rule-model sources injected in `:legado-runtime` | **Host adapter / partial** — raw JSON retention avoids unknown-field loss but does not supply authentic model behavior |
| Book / ReadConfig | Book entity, BaseBook/RuleDataInterface, nested ReadConfig/ParagraphAnchor, BookHelp and DAO behavior | Host BookCandidate/BookDetail and reader state | **Missing / blocker** — full model fields, defaults, methods and persistence semantics not integrated |
| BookChapter / TOC identity | BookChapter metadata, variables, URL identity, index, DAO and TOC update behavior | Host chapter projection, URL/alias helpers and TOC service | **Host adapter / partial** — duplicate URL/book scoping and complete metadata parity unresolved |
| Six rule models | Authentic Explore/Search/BookInfo/TOC/Content/Review models and Gson converters | Seven authentic upstream rule source files compiled in :legado-runtime and app copies; selected codecs | **Authentic slice** — field/serialization differential contract and enclosing BookSource lifecycle remain open |
| Selector/parser core | RuleAnalyzer, AnalyzeByJSoup, AnalyzeByRegex, SourceHtmlParser plus XPath/JSONPath/JS helpers | Selected authentic parser sources in :integration-contract; local adapted XPath/JSONPath bridges | **Authentic slice + host adapter / partial** — AnalyzeRule orchestration and full parser closure absent |
| AnalyzeRule | Source/data/context state, parser caches, replacement, JS, batch context and helper methods | No authentic AnalyzeRule production source in :legado-runtime; selected parser dispatcher instead | **Missing / blocker** — same-name selector/dispatcher adapters cannot stand in for complete AnalyzeRule lifecycle |
| AnalyzeUrl | URL templates, headers/body/method, proxy, timeouts, redirects, retry, DNS, Cookie, WebView, response/body APIs | Host URL resolver, SourceRequestFactory, OkHttp transport and custom bounded request options | **Host adapter / partial** — no exact AnalyzeUrl source; bodyJs/WebView/redirect/retry/response edge semantics not proven |
| Rhino engine | Pinned HtmlUnit CoreJS/Rhino source, ScriptBindings, contexts, wrappers, class shutter | Copied engine in :legado-rhino with local sandbox extensions | **Authentic slice** — hash/provenance, full context/lifecycle and security/ABI parity remain open |
| JsExtensions ABI | Large inherited interface with overloaded HTTP/browser/Cookie/cache/file/crypto/config methods; Rhino wrapping and annotations | Hand-authored RhinoSafeSourceJavaExtensions bridge | **Host adapter / partial** — observed type gaps: getSource Map not BaseSource, getBatchContext null, navigation context EmptyCoroutineContext |
| Shared JS scope / jsLib | BaseSource/SharedJsScope source-scoped libraries, shared state, weak/LRU and lifecycle | Per-source Rhino scopes/global state and jsLib/mainJs hashes in RhinoSourceScriptRuntime | **Host adapter / partial** — exact lexical scope, shared state key, reset and cross-operation lifetime parity not shown |
| BaseSource lifecycle | source identity, login/header/info, variables, rate limit, jsLib and evalJS | Host source DTO plus separate runtime/session/cookie/variable services | **Missing / blocker** — authentic BaseSource not integrated; lifecycle state split across adapters |
| HTTP transport | AnalyzeUrl + OkHttp/Cronet helpers, response status/headers/body, timeout/cancel/redirect, rate limits | OkHttpSourceRequestClient and custom callbacks, timeouts/retries/caps | **Host adapter / partial** — intentional limits differ from upstream behavior; no exact request differential matrix |
| Cookie/session | CookieStore/DAO, CookieManager, WebView cookie sync, source/session/domain handling | SharedPreferencesSourceCookieJar and login session adapter | **Host adapter / partial** — DAO persistence and full WebView/session merge/restore semantics unproven |
| Login UI / login JS | BaseSource login/loginUi/loginAction/loginCheck/header/info flows and LoginUiV2 protocol | LegadoSourceLoginUiV2Service, SourceLoginSession and separate utility screen | **Host adapter / partial** — UI v2 subset exists; BaseSource login lifecycle and exact state/callback ordering absent |
| Hidden WebView/browser/verification | BackstageWebView, WebJsExtensions, browser await/callback, verification code, timeout and pooled resource release | Typed HostPort request/result declarations; source-rule call-through and complete Android owner not established | **Unknown / blocker** — must not substitute ordinary HTTP fetch for WebView behavior |
| Cache / files / large variables | CacheManager/AppCacheManager/WebCacheManager, RuleBigDataHelp, source file/cache helpers | FileChapterContentCache, SourceScriptFileCacheStore, SharedPreferences key-value store | **Host adapter / partial** — separate stores exist; exact namespaces, TTL, large-value threshold, cleanup and recovery parity absent |
| Search / explore / detail / TOC | WebBook + BookList/BookInfo/BookChapterList orchestration and pagination | Host SourceSearch/Explore/BookDetail/TOC services | **Host adapter / partial** — host orchestration not upstream call sequence; error, cancellation and pagination parity unproven |
| Content / multi-page / batch | WebBook + BookContent, AnalyzeRule/AnalyzeUrl, next-page and contentBatch, BatchContentContext and cache save fence | Host SourceChapterContentService and FileChapterContentCache | **Host adapter / partial; batch blocker** — current JS bridge reports null batch context; full upstream contentBatch behavior not shown |
| Reader/UI/product features | Legado Activities/Reader and product DB | Badcat original product UI/Reader and Badcat stores | **Intentionally excluded** — preserve Badcat ownership; import only source-facing dependencies proven by call sites |
| License/provenance | GPL-3.0 upstream source and resources | copied Rhino/rule/parser files and local modifications | **Unknown / blocker** — file-level provenance/license inventory and distribution compliance review remain required |

No row is marked complete. “Authentic slice” describes source provenance/presence only and does not imply complete behavior parity.

## 17. Architecture boundary comparison — provisional, not a selection

| Candidate | Evidence-based advantages | Main blockers/risks | Conditions before selection |
|---|---|---|---|
| A. Dedicated Android Runtime module with authentic source groups + typed Host Ports | Fits the intended Badcat Product UI → Facade → Runtime → Host Ports layering; can keep Android services explicit and keep product UI out | Current :legado-runtime is almost empty; BookSource/BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions depend on app services, Room, UI and media/image classes; importing whole source tree would pull product graph | Prove minimal source groups and transitive closure; extract authentic model/analyzer lifecycle; implement typed ports with differential contracts; prove no product UI/Reader dependency |
| B. Controlled upstream core module/source-set extraction | Maximizes authentic source reuse and reduces hand-written compatibility surfaces; pinned upstream already has an Android Rhino module | Upstream book-source code is still inside app module; module split does not exist; direct dependencies and generated Room/Glide code are entangled with app; upstream module extraction is invasive and may require license/provenance management | Demonstrate a reproducible source-set/module split from the pinned SHA without importing UI/Reader; build exact variant and test behavior before any refactor |
| C. Hybrid source reuse with strict API/behavior parity | Can reuse proven parser/Rhino slices while retaining existing host transport/storage/UI; lowest short-term migration cost | Current hybrid already has semantic gaps: host projection DTOs, manual JsExtensions bridge, null batch context, no authentic BaseSource/AnalyzeRule/AnalyzeUrl, and multiple independent data owners | Only acceptable if every method/field/default/error/cancel/callback/lifecycle gap is enumerated and differential tests prove equivalence; unsupported APIs must remain explicit blockers |

**Provisional conclusion:** none of A/B/C is approved. Current evidence rules out treating the existing hybrid as complete; it does not yet prove that A or B can satisfy the full closure with acceptable module boundaries. Complete the caller/reflection graph, resolved Gradle/resource/native/R8 closure, Badcat all-caller/data-owner scan, and behavior matrix before registering a boundary decision. Stage 1 remains **NO-GO**.


## 18. Rhino Java-object exposure and reflection-sensitive ABI — caller scan slice 2 (2026-11-24)

### 18.1 Dynamic Java object wrapping is part of the public source ABI

- Pinned `modules/rhino/.../RhinoWrapFactory.kt` overrides both `wrapAsJavaObject` and `wrapJavaClass`. It selects custom factories by exact runtime Java class, otherwise wraps Lists/Maps/arrays/objects using HtmlUnit CoreJS native wrappers. This means source-visible properties and method overload behavior are not determined solely by `@JavascriptInterface`.
- Pinned `app/.../App.kt:initRhino()` registers custom wrappers:
  - `BookSource` → `NativeBaseSource.factory`
  - `RssSource` and `HttpTTS` → `NativeBaseSource.factory`
  - `ExploreRule`, `SearchRule`, `BookInfoRule`, `ContentRule`, `BookChapter`, and `Book.ReadConfig` → `ReadOnlyJavaObject.factory`
- `NativeBaseSource` exposes JavaBean-style setters as property assignment (hides `setX` when a corresponding property exists), special-cases `setVariable`, and only permits direct assignment to `variable`. `ReadOnlyJavaObject` hides setters and ignores all property writes. These wrapper policies affect script behavior and must be reproduced or proven equivalent.
- The observed wrapper registration does not list every rule type (for example TocRule/ReviewRule are not in this registration block); their JS exposure must be traced through actual binding paths rather than inferred from model existence.

### 18.2 Rhino execution/security configuration is behavioral contract

- Pinned `RhinoScriptEngine` initializes HtmlUnit CoreJS with ES6 language version, interpreted mode, `RhinoClassShutter`, `RhinoWrapFactory`, instruction observer threshold 10,000 and maximum interpreter stack depth 1,000.
- Its feature overrides enable E4X, Java Map access, Legado dynamic default `this`, and dynamic eval/Function realm behavior. Top-level calls require `allowScriptRun` and check coroutine cancellation; instruction observation calls `ensureActive()`.
- `RhinoContext` carries coroutine context, allow-run flag and recursion count; initializes E4X/XML support and forces JSON initialization before shared scopes. Compatibility source rewriting for legacy const/let/with behavior is part of the pinned engine.
- `RhinoClassShutter` blocks sensitive class names and types, including Class/ClassLoader/Runtime/ProcessBuilder, File and file streams, reflection/invoke, Android Context/Intent/Activity internals, Room/DAO/AppDatabase, filesystem APIs and internal engine packages. System `load`, `loadLibrary` and `exit` are protected. This is not a generic allow-list that can be approximated by hiding `java` from the JS global object.
- Pinned `SharedJsScope` plus `ScriptBindings` implement compiled shared scopes, source/library state handles, locks, weak-reference cache and removal/generation behavior. A per-source Rhino scope alone is not proof of equivalence.

### 18.3 Reflection and call-search results

Exact pinned code-search discovery:
- `RhinoWrapFactory.register`: 4 files (App registration plus compatibility/capability tests).
- `NativeBaseSource.factory`: 3 files (App registration plus source compatibility tests).
- `ClassNameMatcher`: 2 files (matcher and class shutter).
- `setClassShutter`: 1 file (RhinoScriptEngine).
- `java.lang.reflect`: 7 files (RhinoClassShutter, Gson extensions, app utilities/tests and license diff).
- `getDeclaredMethod`: 6 files, primarily menu helpers and lifecycle/UI tests; no evidence yet that these are source-rule dispatch points.
- `getMethod(`: 7 files, including AnalyzeUrl and app/UI helpers; most observed AnalyzeUrl matches are UrlOption getter calls, not reflective invocation.
- `Class.forName`: 5 files, including RhinoWrapFactory security commentary, Rhino tests and app file utilities. A comment mentioning Class.forName is not itself a call edge.

### 18.4 Badcat parity implications

- Badcat's `RhinoSafeSourceJavaExtensions` is a manually implemented class rather than the pinned `JsExtensions` interface + dynamic wrapper path. Its methods/overloads, property naming, object types, exception conversion and source-visible object wrappers need explicit comparison.
- Badcat has a local `LegadoRhinoSandbox`; its policy must be compared class-by-class with the pinned RhinoClassShutter and ProtectedNativeJavaClass behavior. A stricter policy may be a deliberate security restriction, but any source API it blocks must be registered as an incompatibility unless a differential contract proves otherwise.
- No exhaustive reflection/string-dispatch graph is closed by this slice. It narrows the critical JS ABI from “48 annotations” to a combined contract of interface methods, wrapper registration, JavaBean property conversion, object type factories, class shutter, context/feature flags and scope lifecycle.

**Status:** Rhino ABI/reflection inventory remains partial. Stage-0 is not accepted; stage-1 remains **NO-GO**.


## 19. Badcat Rhino source provenance comparison — exact blob evidence (2026-11-24)

The following files in Badcat `:legado-rhino` have byte-identical Git blob SHA values to the pinned upstream source files at `330e606c4d57e08adc056ff8ab7a4132c784da9a`:

| Source file | Upstream blob SHA | Badcat blob SHA | Result |
|---|---|---|---|
| `RhinoClassShutter.kt` | `e9033fa47753038751191926a4f898bf5680c05b` | `e9033fa47753038751191926a4f898bf5680c05b` | identical |
| `RhinoWrapFactory.kt` | `4a3d8c30329664e725cb64ae89a88a5dfe4ebc6a` | `4a3d8c30329664e725cb64ae89a88a5dfe4ebc6a` | identical |
| `RhinoContext.kt` | `41344c930e6050248f53dea6eddebd1e0af9da16` | `41344c930e6050248f53dea6eddebd1e0af9da16` | identical |
| `ScriptBindings.kt` | `2b34712e911cc941b56f16257cbd560e1d31f64f` | `2b34712e911cc941b56f16257cbd560e1d31f64f` | identical |
| `ReadOnlyJavaObject.kt` | `80095eca8d810c9fc0ac016076f21cb512f29870` | `80095eca8d810c9fc0ac016076f21cb512f29870` | identical |
| `JavaObjectWrapFactory.kt` | `283cff7b6e0d778cd6348a467382765c8cc7c274` | `283cff7b6e0d778cd6348a467382765c8cc7c274` | identical |

`RhinoScriptEngine.kt` is not byte-identical:
- Upstream blob: `19951c27c861af6815f43e4756935634135ac1f7`
- Badcat blob: `84552db27cfff78d52905377fa6774dbc5dea34e`
- Both retain the main eval/compile/context setup method set, while Badcat adds `callSourceFunction(action, scope)` and changes `topLevelThis(scope)` to read the JS-visible `globalThis` property before falling back to `TopLevel.globalThis`. The added callback method requires an active RhinoContext and invokes the supplied Function with the resolved top-level this object.
- This is a real local divergence, not a byte-for-byte upstream copy. It may be intentional for the Badcat source-scoped shared-global model, but its callback/this semantics require a pinned differential contract and an explicit provenance note. A symbol search in the Badcat code index was incomplete, so no claim is made here that all call sites were found.

**Conclusion:** the Rhino core contains a substantial exact upstream slice, but exact-file identity for six engine files does not imply full Rhino ABI parity. The modified engine entry point, custom Badcat extensions, missing upstream BaseSource/JsExtensions wrapper registrations and different source-scope wiring remain part of the compatibility blocker.


## 20. Core source-facing API/property inventory — pinned declarations slice 2 (2026-11-24)

This section records constructor and property-level evidence from the exact pinned Kotlin declarations. It is a source inventory, not a statement that Badcat has integrated these declarations.

### 20.1 BaseSource lifecycle contract

Pinned path: `app/src/main/java/io/legado/app/data/entities/BaseSource.kt`.

- `BaseSource : JsExtensions` and exposes mutable nullable `concurrentRate`, `loginUrl`, `loginUi`, `header`, `enabledCookieJar`, and `jsLib`.
- It requires `getTag(): String` and `getKey(): String`; `getSource()` returns the BaseSource itself.
- Lifecycle helpers include login UI v2 detection/evaluation, login action evaluation, login form/check, login header and login info read/write/clear, encrypted login info, source variables, key/value storage, explore refresh, JS library refresh, concurrency configuration and `evalJS`.
- `evalJS` builds source ScriptBindings, resolves `getSharedGlobalStateKey()`, and selects/reuses a SharedJsScope or a fresh top-level scope. Therefore source lifecycle includes login state, variable persistence, rate limiting, JS library and scope identity, not only source metadata.

### 20.2 BookSource constructor/property inventory

Pinned path: `app/src/main/java/io/legado/app/data/entities/BookSource.kt`; `BookSource : Parcelable, BaseSource`.

| Field | Type/default | Compatibility significance |
|---|---|---|
| bookSourceUrl | String = "" | primary source identity/key |
| bookSourceName | String = "" | display name |
| bookSourceGroup | String? = null | grouping |
| bookSourceType | Int = 0 | text/audio/image/download/video mode |
| bookUrlPattern | String? = null | detail URL matching |
| customOrder | Int = 0 | order |
| enabled / enabledExplore | Boolean = true | source and explore enablement |
| jsLib | String? = null | inherited source library |
| enabledCookieJar | Boolean? = true | automatic CookieJar behavior |
| concurrentRate | String? = null | request concurrency/rate policy |
| header | String? = null | common headers |
| loginUrl / loginUi | String? = null | login lifecycle |
| loginCheckJs / coverDecodeJs | String? = null | login and image decoding JS |
| bookSourceComment / variableComment | String? = null | metadata |
| lastUpdateTime | Long = 0 | ordering/update |
| respondTime | Long = 180000L | response-time ordering |
| weight | Int = 0 | smart ordering |
| exploreUrl / exploreScreen | String? = null | discovery |
| ruleExplore | ExploreRule? = null | explore stage |
| searchUrl | String? = null | search request |
| ruleSearch | SearchRule? = null | search stage |
| ruleBookInfo | BookInfoRule? = null | detail stage |
| ruleToc | TocRule? = null | TOC stage |
| ruleContent | ContentRule? = null | content and batch stage |
| ruleReview | ReviewRule? = null | paragraph review stage |
| mainJs | String? = null | JS-only source entry point |
| eventListener / customButton | Boolean = false | event callbacks/custom source buttons |

Important methods beyond data properties: `getSearchRule`, `getExploreRule`, `getBookInfoRule`, `getTocRule`, `getContentRule`, display/group mutation, error comment normalization, JS-source detection, `supportContentBatch()`, `contentBatchSize()`, login JS override, equality and per-rule Gson converters. These methods are behavior and serializer contract, not merely fields.

### 20.3 Rule model field inventory

Pinned paths under `app/src/main/java/io/legado/app/data/entities/rule/`:

- `ExploreRule`: nullable `bookList, name, author, intro, kind, lastChapter, updateTime, bookUrl, coverUrl, wordCount`.
- `SearchRule`: nullable `checkKeyWord` plus the same ten explore result fields.
- `BookInfoRule`: nullable `init, name, author, intro, kind, lastChapter, updateTime, coverUrl, tocUrl, wordCount, canReName, downloadUrls`.
- `TocRule`: nullable `preUpdateJs, chapterList, chapterName, chapterUrl, formatJs, isVolume, isVip, isPay, updateTime, nextTocUrl`.
- `ContentRule`: nullable `content, subContent, title, nextContentUrl, webJs, sourceRegex, replaceRegex, imageStyle, imageDecode, payAction, callBackJs, contentBatch, maxBatchSize`. `maxBatchSize` is nullable Int; null is explicitly the “batch unsupported” state.
- `ReviewRule`: nullable URL/rule fields for review URL, avatar/content/time, quote, vote up/down, post/delete, summary URL/list/paragraph/index/count, detail URL/pagination/list/id/avatar/name/badge/content, reply list/id/avatar/name/badge/content; `enabled: Boolean = false`.
- The rule models use custom Gson deserializers; the six model groups are serialized/deserialized through BookSource.Converters. JSON null/missing/default behavior therefore needs differential fixtures, not just Kotlin property-name matching.

### 20.4 Book and reading configuration inventory

Pinned path: `app/src/main/java/io/legado/app/data/entities/Book.kt`; `Book : Parcelable, BaseBook`.

Core persisted fields include bookUrl, tocUrl, origin, originName, name, author, kind, customTag, coverUrl/customCoverUrl, intro/customIntro, charset, type, group, latestChapterTitle/time, lastCheckTime/count, totalChapterNum, durChapterTitle/index, durVolumeIndex, chapterInVolumeIndex, durChapterPos, durChapterTime, wordCount, canUpdate, order, originOrder, variable, readConfig, syncTime and persistedCoverUrl. Many are nullable; others have meaningful constructor defaults and Room column defaults.

- `ReadConfig` includes reverseToc=false, reverseTocDisplay=false, tocExpanded=true, nullable pageAnim, reSegment=false, nullable imageStyle/useReplaceRule, delTag=0, nullable ttsEngine, splitLongChapter=true, readSimulating=false, nullable startDate/startChapter, dailyChapters=3, openCredits/closeCredits/playMode=0, playSpeed=1.0f, useGlobalAudioSkip=false, manualReplaceRuleIds=emptyList(), nullable paragraphAnchor.
- `ParagraphAnchor(chapterUrl, itemId, chapterPosition)` is a structured location anchor. A URL/scrollY-only Badcat state is not a field-for-field substitute.
- Book methods include lazy/default config behavior, display/custom metadata, read settings getters/setters, simulation/unread counts, bookmark creation, migration, save/delete and serialization converters. These are part of the model lifecycle and may trigger DAO/cache side effects.

### 20.5 BookChapter inventory

Pinned path: `app/src/main/java/io/legado/app/data/entities/BookChapter.kt`.

Constructor fields: `url, title, isVolume, baseUrl, bookUrl, index, isVip, isPay` with empty/false/zero defaults; nullable `resourceUrl, tag, wordCount, start, end, startFragmentId, endFragmentId, variable, imgUrl`. The entity also has lazy variable map, big-variable storage, update, equality/hash identity, display title, absolute URL and filename helpers. The URL equality/hash behavior and chapter index/book ownership are separate identity concerns that need explicit tests.

### 20.6 AnalyzeRule and AnalyzeUrl constructor/state contract

- `AnalyzeRule(ruleData: RuleDataInterface? = null, source: BaseSource? = null, preUpdateJs: Boolean = false, isFromBookInfo: Boolean = false)` owns mutable content/baseUrl/redirectUrl, parser instances for XPath/Jsoup/JSONPath, source-rule and regex caches, compiled script cache, local bindings, weak top-scope reference, eval call count, coroutine context, chapter, nextChapterUrl and BatchContentContext. Its API includes string/list/element extraction, rule splitting/modes, JS evaluation, source variable get/put, AJAX, book/TOC refresh and fluent context/data/chapter/batch setters.
- `AnalyzeUrl` constructor accepts URL, key, page, speak text/speed, base URL, BaseSource, RuleDataInterface, BookChapter, read/call timeout, coroutine context, optional header map/login-header flag, info map and extra parameters. It owns parsed URL/ruleUrl/header/body/method/proxy/timeout/redirect/retry/WebView/WebJS/bodyJS/DNS/cookie/rate-limit state. Its response APIs include string response, raw Response, byte array, input stream, upload, Glide URL and media item helpers.
- `UrlOption` supports method, charset, headers, body, origin, retry, type, WebView, WebJS, timeout, followRedirects, dnsIp (JSON alias `resolveIp`), URL JS, body JS, serverID and WebView delay. The URL parser's defaults/nullability and serialized alias are compatibility-sensitive.
- `AnalyzeRule` and `AnalyzeUrl` both implement JsExtensions; their actual JS-visible methods include context-specific overrides (source, tag, batch context, navigation context) and must be compared as a combined interface + wrapper contract.

### 20.7 JsExtensions source-facing method families

Pinned path: `app/src/main/java/io/legado/app/help/JsExtensions.kt`; 1,390 lines, 118 Kotlin function declarations, 48 explicit `@JavascriptInterface` annotations. Method families observed:

1. source/tag/navigation/batch context, cacheContent and refresh callbacks;
2. ajax/ajaxAll/ajaxTestAll/connect overloads;
3. hidden WebView, get-source/override-URL, browser start/await and video player;
4. verification code and source script import;
5. file cache/download, cookie access, HTTP GET/HEAD/POST overloads;
6. string/byte/base64/hex/URI/time/HTML/traditional-Chinese conversions;
7. safe file read/write/delete and ZIP/7z/RAR archive extraction;
8. ZIP/7z/RAR content access and TTF/font query/replacement;
9. URL construction, toast/log/type/random UUID/Android ID;
10. open URL, browser UI, singleFlight/lock/tick;
11. reader config/theme getters.

This is a broad host surface. The exact overload signatures, default parameters, nullable returns, thrown errors, thread/coroutine behavior and object return types must be inventoried per method. The 48 annotation count is not an ABI count because RhinoWrapFactory also wraps ordinary Java objects and applies custom property/setter policies.

**Status:** property/constructor inventory slice 2 is recorded. It does not yet close every inherited BaseBook/RuleDataInterface/JsEncodeUtils/RuleData/UrlOption field, every Gson converter/default, or every JsExtensions overload signature. Stage-0 remains partial; stage-1 remains **NO-GO**.


## 21. Inherited rule-data, batch-save and shared-scope contracts — pinned source slice 3 (2026-11-24)

### 21.1 RuleDataInterface / BaseBook data ownership

Pinned paths:
- `app/src/main/java/io/legado/app/model/analyzeRule/RuleDataInterface.kt`
- `app/src/main/java/io/legado/app/data/entities/BaseBook.kt`

`RuleDataInterface` owns a mutable `HashMap<String,String>` variable map and a two-tier value policy:
- null removes the inline value and calls `putBigVariable(key, null)`;
- values with `length < 10000` are kept inline and any previous big value is cleared;
- values with length >= 10000 are removed from the inline map and delegated to `putBigVariable`;
- reads resolve inline first, then big-value storage, then empty string.
This threshold is Kotlin String length (UTF-16 code units), not UTF-8 byte size.

`BaseBook` extends RuleDataInterface with name/author/bookUrl/kind/wordCount/variable plus infoHtml/tocHtml. Its default putVariable serializes the variable map back to `variable`; large values are owned by `RuleBigDataHelp` keyed by bookUrl. A replacement KV store must preserve key scoping, deletion, threshold, fallback and lifecycle cleanup, not just expose put/get.

### 21.2 BatchContentContext identity, concurrency and save-fence contract

Pinned path: `app/src/main/java/io/legado/app/model/BatchContentContext.kt`.

- Context binds one BookSource, Book and immutable chapter list, plus coroutine context and per-index ContentSaveToken map.
- Chapter identity is explicitly `BookChapter.index`; same URL may legitimately represent multiple chapters.
- URL aliases include raw URL, absolute URL and normalized URL. A URL identifier is accepted only for a single unique candidate; ambiguous same-URL entries must use the chapter object. Numeric identifiers are rejected to avoid confusing list position with chapter index.
- Shared mutable state (closed flag, saved-index set and cache/save status) is guarded by one lock because JS may call back concurrently from multiple threads/coroutines.
- `saveContent` checks cancellation, closed state, resolves chapter identity, applies content replacement and online line formatting, requires the original per-index save token, checks cancellation again, and delegates to BookHelp.saveContent. Successful save records the chapter index. Closing blocks later saves; missing chapters are returned for ordinary per-chapter fallback.
- This contract ties together source identity, book identity, chapter index, request-start version token, replacement rules, cancellation and persistence. A generic batch list callback or URL-keyed cache cannot replace it.

### 21.3 SharedJsScope lifetime and synchronization contract

Pinned path: `app/src/main/java/io/legado/app/model/SharedJsScope.kt`.

- JS-library scope cache key is MD5 of the jsLib text; a separate crypto scope is keyed by owner identity and current thread.
- Both use LRU caches of 16 entries containing weak references, plus per-key creation locks with reference counts to avoid duplicate concurrent scope creation.
- A library scope installs CryptoJS, evaluates the JS library, prevents extensions and stores the weak scope. Removing a library removes cached remote library files, cached scope and shared-global-state keys under the same creation lock.
- Scope reuse, source shared global-state namespace, thread ownership, weak-reference expiry, concurrent creation and invalidation are observable semantics. A per-source persistent global object with no equivalent eviction/reset is not automatically compatible.

### 21.4 JsEncodeUtils inherited ABI

Pinned path: `app/src/main/java/io/legado/app/help/JsEncodeUtils.kt`; JsExtensions inherits this interface.

Method groups include MD5 full/16-character digest, symmetric crypto factory overloads (String/ByteArray keys and optional IV), asymmetric crypto and signing factories, legacy AES/DES/TripleDES encode/decode overloads, digest hex/Base64 and HMAC hex/Base64. Some are explicitly annotated with `@JavascriptInterface`, while many are exposed through Rhino Java-object wrapping. Return types include crypto helper objects and byte arrays, not just strings.

This expands the JS compatibility surface beyond the JsExtensions declarations. Every inherited method, overload, helper object and nullable/byte-array conversion must be included in the source ABI inventory.

**Status:** inherited data, batch and scope contracts are now described at source level, but Badcat implementation-by-implementation differential evidence is still incomplete. Stage-0 remains partial; stage-1 remains **NO-GO**.


## 22. JsExtensions / JsEncodeUtils name-and-overload comparison — static declaration diff slice 4 (2026-11-24)

A lexical declaration diff was run against pinned upstream `JsExtensions.kt` + inherited `JsEncodeUtils.kt` and Badcat `RhinoSafeSourceJavaExtensions.kt`. This counts Kotlin `fun name(` declarations in the fetched source text; it does not resolve inherited JVM bridges, generated methods, visibility, overload signatures, annotations, Rhino wrapping or runtime dispatch.

| Inventory | Declarations | Unique names |
|---|---:|---:|
| Upstream JsExtensions | 122 | 81 |
| Upstream JsEncodeUtils | 30 | 27 |
| Combined upstream names (union) | — | 108 |
| Badcat RhinoSafeSourceJavaExtensions | 225 | 179 |

### 22.1 Upstream names with no same-name declaration in Badcat bridge

`canOpenSourceUi`, `getReadBookConfig`, `getReadBookConfigMap`, `getThemeConfig`, `getThemeConfigMap`, `getThemeMode`, `getVerificationCode`, `openUrl`, `openVideoPlayer`, `refreshBookInfo`, `refreshBookToc`, `refreshContent`, `showBrowser`, `startBrowser`, `startBrowserAwait`, `webView`, `webViewGetOverrideUrl`, `webViewGetSource`.

These missing names include browser/WebView/verification callbacks, product refresh events, reader/theme APIs and UI dispatch. Some may be intentionally unavailable in a host runtime, but each absence must be explicitly classified and source-rule reachability demonstrated before it can be excluded. For a complete Legado source API claim, unimplemented source-facing methods remain blockers.

### 22.2 Representative overload-count differences

| Method | Upstream declarations | Badcat bridge declarations |
|---|---:|---:|
| ajax | 2 | 2 |
| ajaxAll | 2 | 1 |
| ajaxTestAll | 2 | 1 |
| connect | 3 | 3 |
| webView | 2 | 0 |
| webViewGetSource | 3 | 0 |
| webViewGetOverrideUrl | 3 | 0 |
| startBrowser | 2 | 0 |
| startBrowserAwait | 3 | 0 |
| get | 2 | 3 |
| head | 2 | 2 |
| post | 2 | 2 |
| downloadFile | 2 | 2 |
| readTxtFile | 2 | 2 |
| getZipStringContent | 2 | 2 |
| queryTTF | 2 | 2 |
| replaceFont | 2 | 2 |
| openUrl | 2 | 0 |
| showBrowser | 4 | 0 |
| singleFlight | 2 | 2 |
| lock | 2 | 2 |
| tick | 1 | 1 |
| createSymmetricCrypto | 4 | 4 |

### 22.3 Interpretation

- Badcat has many extra helper declarations, most of which are private implementation utilities and must not be counted as source ABI parity.
- Same-name methods do not imply signature compatibility. For example, Badcat ajaxAll/ajaxTestAll have fewer overloads, while HTTP get has a different count; request/response types also differ (RhinoSourceHttpResponse vs upstream StrResponse/Jsoup Connection.Response).
- The bridge exposes a custom `getSource(): Map<String, Any?>` rather than `BaseSource?`; `getBatchContext()` returns null and navigation context is EmptyCoroutineContext. These are concrete return-value/behavior differences even where method names match.
- The absence of UI/WebView/browser methods may be a deliberate host safety choice, but without a complete source caller graph and a supported host implementation, it is not full upstream source compatibility. A typed Host Port may implement the behavior without copying Legado UI; silently omitting the methods is not equivalent.

**Status:** this diff provides a reproducible method-name blocker list and selected overload counts, not full signature parity. Stage-0 remains partial; stage-1 remains **NO-GO**.


## 23. Rhino sandbox policy parity — exact class-shutter evidence and bridge wiring (2026-11-24)

### 23.1 Exact upstream security implementation reuse

At Badcat HEAD `fd5f357b9f1c38d604a9487a4f1374b214ef8a83`, these Badcat files have the exact same Git blob SHA as the pinned upstream files:
- `legado-rhino/.../RhinoClassShutter.kt`: `e9033fa47753038751191926a4f898bf5680c05b`
- `legado-rhino/.../ProtectedNativeJavaClass.kt`: `b9105612cad4830c2bcae93a1ff8731501cd4bda`
- `legado-rhino/.../RhinoWrapFactory.kt`: `4a3d8c30329664e725cb64ae89a88a5dfe4ebc6a`
- `legado-rhino/.../ClassNameMatcher.kt`: Badcat implementation is present; exact pinned blob comparison is a separate provenance item.

The copied RhinoClassShutter retains upstream protected class names and package prefixes, protected runtime object types, Android/IO/reflection/Room/DAO/Okio restrictions and System.load/loadLibrary/exit restrictions. ProtectedNativeJavaClass retains blocked-member lookup/write behavior and invocation exception conversion. RhinoScriptEngine configures ES6 interpreted mode, that class shutter, the wrap factory, instruction observer threshold 10,000 and stack depth 1,000. This is positive evidence for this security slice; do not claim that the sandbox security core was replaced by a weaker custom policy.

### 23.2 Important difference: identical policy does not mean identical exposed ABI

- Upstream `RhinoWrapFactory` has a custom-factory registry keyed by exact Java class and falls back to typed wrappers for List/Map/arrays/objects. Upstream `App.initRhino()` registers NativeBaseSource for BookSource and ReadOnlyJavaObject for selected rule/entity types.
- Badcat tree contains ReadOnlyJavaObject but no `NativeBaseSource` source file. The inspected Badcat production path directly places `RhinoSafeSourceJavaExtensions` into the JS scope as `java`, and separately exposes custom `cookie`, `source` and `cache` objects. The source object is a host login extension; it is not the upstream BaseSource entity lifecycle.
- The custom `getSource(): Map<String,Any?>` and null `getBatchContext()` remain concrete API divergences. The fact that the same ClassShutter/WrapFactory classes are present does not restore missing object factories, model methods, properties, callback contracts or upstream service ownership.
- `LegadoRhinoSandbox` itself only accepts bounded primitive string bindings and primitive results; it documents that it is not the complete AnalyzeUrl adapter. The inspected production `RhinoSourceScriptRuntime` instead creates persistent per-source scopes and injects the custom Java/Cookie/source/cache bridges directly. These are distinct execution paths and must not be conflated.
- Code-search index for Badcat remains incomplete, so absence of registration calls across the whole repository is not asserted. The production factory wiring and source tree are the bounded evidence here.

**Assessment:** upstream Rhino class-shutter/wrap-factory security code has a verified exact-source slice, while source-visible API wiring and object identity remain host-projection based. Security-source reuse is not proof of complete JS ABI compatibility.

**Status:** sandbox parity slice partial; stage-0 remains incomplete and stage-1 remains **NO-GO**.


## 24. Runtime-3.2f.50 — inherited/generated API and source ABI closure slice (2026-11-24)

- **Badcat inspected HEAD:** `bfca5f67584522199240588740adbdcbb7152562` (the only intervening change from the prior inspected tree is the preregistration record in the third-category checklist).
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Evidence method:** read exact raw Kotlin source at the pinned SHA and current Badcat bridge/model adapter source. This is source-level evidence; no local compilation, bytecode dump, runtime reflection or new test was run.

### 24.1 Inheritance and source-visible types

| Upstream type/path | Exact source relationship / contract | Badcat-side observation | Status |
|---|---|---|---|
| `data/entities/BookSource.kt` | `@Parcelize data class BookSource : Parcelable, BaseSource`; constructor properties include the source identity, all six rule objects, JS/login/Cookie/rate/header and batch-related source settings. | Current host codec projects JSON into host `BookSource`/catalog types; it is not the authentic upstream entity. | Source contract confirmed; integration gap |
| `data/entities/BaseSource.kt` | `BaseSource : JsExtensions`; declares six mutable nullable properties (`concurrentRate`, `loginUrl`, `loginUi`, `header`, `enabledCookieJar`, `jsLib`), `getTag()`, `getKey()`, and default `getSource() = this`. Also owns login UI/action, login header/info, variable, concurrency, JS library and `evalJS` lifecycle methods. | Host bridge exposes a source descriptor Map and custom login/session services, not the upstream BaseSource identity/lifecycle. | Blocker |
| `data/entities/Book.kt` | `@Parcelize data class Book : Parcelable, BaseBook`; nested `ReadConfig` and `ParagraphAnchor` are also parcelable data contracts. Book carries reading state, source identity, metadata, variable map and persistence helpers. | Host projections and SharedPreferences/cache owners are separate; no authentic Book entity in `:legado-runtime`. | Source contract confirmed; integration gap |
| `data/entities/BookChapter.kt` | `@Parcelize data class BookChapter : Parcelable, RuleDataInterface`; chapter fields include `index`, URL/base URL/book URL, volume/VIP/pay flags, resource/media metadata and variable data. | Host chapter projection is not this upstream entity; current batch bridge returns null context. | Blocker |
| `data/entities/BaseBook.kt` + `model/analyzeRule/RuleDataInterface.kt` | BaseBook inherits RuleDataInterface and supplies large-variable storage; RuleDataInterface exposes `variableMap`, put/get and big-value hooks. Book and BookChapter therefore inherit behavior beyond their declared fields. | Host stores JS variables separately; the source-level threshold/ownership contract is not yet differentially proven against both host paths. | Partial / blocker |
| `model/analyzeRule/AnalyzeRule.kt` | `@Keep class AnalyzeRule(ruleData: RuleDataInterface? = null, source: BaseSource? = null, preUpdateJs: Boolean = false, isFromBookInfo: Boolean = false) : JsExtensions`; parser, replacement, JS, local variable, source and batch-context methods are part of the class surface. | Badcat uses a separately implemented host evaluator/bridge and pinned parser slices; this is not the complete upstream AnalyzeRule object. | Partial / blocker |
| `model/analyzeRule/AnalyzeUrl.kt` | `@Keep class AnalyzeUrl` has a 16-parameter primary constructor with defaults, a one-String secondary constructor, implements JsExtensions, and contains `UrlOption` and `ConcurrentRecord` data types. Sync and suspend request/response/body/stream/upload paths coexist. | Host HTTP gateway/request adapter has different types and must be compared for constructor, overload, cancellation and response semantics. | Partial / blocker |
| `help/JsExtensions.kt` + `help/JsEncodeUtils.kt` | JsExtensions inherits JsEncodeUtils. The former contains 48 `@JavascriptInterface` annotation occurrences; the inherited latter contains 20. Numerous other functions are intended to be exposed through Rhino Java-object wrapping, so annotation count is not the complete JS ABI. | `RhinoSafeSourceJavaExtensions` is a custom class, not an implementation of the upstream interface hierarchy. | Blocker |

### 24.2 Rule-model inheritance, defaults and serialization surface

- The six rule groups are `ExploreRule`, `SearchRule`, `BookInfoRule`, `TocRule`, `ContentRule` and `ReviewRule`. Each is an upstream `@Parcelize data class`; ExploreRule and SearchRule additionally implement `BookListRule`, whose ten nullable properties are inherited and overridden by the concrete data classes.
- The inspected fields default to null unless otherwise stated. `ReviewRule.enabled` defaults to false; `ContentRule.maxBatchSize` is nullable Int and null explicitly means batch unsupported. These are semantic defaults, not interchangeable with empty strings or zero.
- Each rule class has a custom Gson deserializer accepting a JSON object or JSON primitive string and returning null for other JSON shapes. BookSource.Converters defines the six corresponding object-to-JSON and JSON-to-object conversion pairs. Exact malformed/null/missing-field behavior still needs fixed fixtures against the pinned Gson setup; this source inspection is not a serializer parity test.
- `ContentRule.toString()` has compatibility-sensitive formatting: the batch fields are appended only when either `contentBatch` or `maxBatchSize` is non-null. This matters because JS `JSON.stringify(rule)` may observe that representation.

### 24.3 Generated JVM and reflection surface — explicit unresolved work

- Kotlin data classes generate members not written as source functions (componentN, copy/copy$default, equals/hashCode/toString and JavaBean accessors); Parcelize adds Parcelable machinery such as CREATOR/parcel read-write implementation. These members can be visible to Java reflection/Rhino depending on wrapper policy and compiler output.
- Kotlin default parameters generate synthetic default-call machinery; only explicit `@JvmOverloads` sites create Java overloads. The pinned AnalyzeRule has five `@JvmOverloads` sites (`setContent`, both `getStringList` forms, both `getString` forms, i.e. five annotated declarations); AnalyzeUrl has one on `getStrResponse`. The precise emitted JVM descriptors, interface DefaultImpls/default methods and Parcelize output are not proven by source inspection.
- JsExtensions default implementations (including navigation context, batch context and refresh methods) may have compiler-mode-dependent JVM representation. Exact visibility, bridge/synthetic flags, descriptors, inherited members and overload resolution require compiled pinned-upstream and Badcat class inspection.
- The current Badcat `RhinoSafeSourceJavaExtensions` source has a custom bridge surface (225 lexical `fun` occurrences including helpers, 14 `@JavascriptInterface` annotations and two `@JvmOverloads` sites in the fetched file). These counts are not directly comparable to upstream interface counts because private helpers and helper classes are included. Concrete semantic divergences remain: `getSource(): Map<String, Any?>`, `getBatchContext(): Any? = null`, and empty navigation context; the earlier 18-name and overload gap list in section 22 remains open.
- Actions #3301 is still pending on its exact SHA `4ea24ef01c7c9fa7f8414ec9d3a50e0b0b1c6a68`; its later JVM ABI capture step has not run yet. No result from that run is borrowed here.

### 24.4 Slice conclusion

This closes a further **source-level inventory slice only**: inheritance and inherited data ownership, six rule model relationships/defaults, source serializer entry points, selected overload annotations and the existence of compiler-generated/reflection-visible members are now explicit. It does **not** close Stage-0 ordered item 1: exact compiled JVM/Rhino ABI descriptors, full annotation/visibility/nullability matrix, all serializer edge fixtures and complete caller/reflection reachability remain open. Stage-0 remains in progress; Stage-1 remains **NO-GO**. No production, build, test, workflow, fixture, UI/Reader, database/schema or release file was changed.


### 24.5 Actions #3301 compiled host JVM ABI artifact — exact-SHA evidence

- **Run:** #3301 / run ID `45264329252`; **job:** `assemble-debug` / job ID `138007096785`; exact SHA `4ea24ef01c7c9fa7f8414ec9d3a50e0b0b1c6a68`; run and job both `completed / success`.
- The job's step 37 (`Capture compiled host JavaScript bridge JVM ABI`) and step 38 (artifact upload) completed successfully. Artifact: `legado-host-jvm-abi`, artifact ID `14863685695`, 165,211 bytes. ZIP contents: `RhinoSafeSourceJavaExtensions.javap.txt` (1,361,755 bytes) and `SHA256SUMS.txt`.
- The manifest records SHA-256 `7b711a33145eaea1a49a7e693c04ab4b16033bdde75ea0fcac2618fee2a355b2` for the javap report. The javap header records classfile SHA-256 `596cfa2a538f8d77c106058303b815d52601e28ebe7ffa0625d8c9325fd4d14d`, classfile major version 65 (Java 21), and `RhinoSafeSourceJavaExtensions` as the compiled source.
- **Compiled class facts:** `public final class app.badcat.client.runtime.RhinoSafeSourceJavaExtensions`; zero implemented interfaces; 70 fields; 234 classfile methods. The report includes 156 public and 76 private method declarations, one package-visible method, synthetic methods and annotation attributes. It confirms this is a standalone host bridge class, not a compiled implementation of upstream `JsExtensions` / `JsEncodeUtils`.
- **Exact descriptors observed:**
  - `getSource()`: `()Ljava/util/Map;` (generic signature Map<String,Object>), not upstream `BaseSource?`.
  - `getBatchContext()`: `()Ljava/lang/Object;`, with the host implementation returning null.
  - `getSourceNavigationContext()`: `()Lkotlin/coroutines/CoroutineContext;`.
  - `getTag()`: `()Ljava/lang/String;`.
  - `ajaxAll(Object, boolean)`: `(Ljava/lang/Object;Z)[Lapp/badcat/client/runtime/RhinoSourceHttpResponse;`; a one-argument Object overload is also emitted.
  - `ajaxTestAll(Object, int, boolean)`: `(Ljava/lang/Object;IZ)[Lapp/badcat/client/runtime/RhinoSourceHttpResponse;`; a two-argument Object/int overload is also emitted.
- The javap output shows 14 `JavascriptInterface` annotation occurrences on this class. It does not expose public `webView`, `startBrowser`, or `getVerificationCode` methods. The 18-name upstream gap list and broader overload differences in section 22 therefore remain valid.
- **Scope limitation:** this artifact proves only the compiled Badcat host bridge at #3301's exact SHA. It does not compile or prove the pinned upstream `BaseSource`, `BookSource`, `Book`, `BookChapter`, `AnalyzeRule`, `AnalyzeUrl`, or `JsExtensions` JVM ABI. Do not use this artifact as upstream ABI evidence or as a full compatibility pass.
- **Run closeout:** #3301 completed successfully, including API 35 WebView provider instrumentation, JVM ABI capture, debug APK assembly/DEX verification and the best-effort public-source probe steps. A successful best-effort probe step is not a blanket pass for every remote source. Each claim remains bound to SHA `4ea24ef...`; the newer documentation commits have separate CI snapshots.



### 24.6 Pinned upstream compiled DEX ABI — exact revision artifact evidence

- **Upstream workflow:** `LegadoTeam/legado` Test Release #487; run ID `41291314673`; exact head SHA `330e606c4d57e08adc056ff8ab7a4132c784da9a`; run completed/success. Jobs `prepare` (`124802981352`), `build (app, release)` (`124803029014`), `build (app, releaseA)` (`124803029075`) and `publish` (`124805234209`) all completed/success.
- **Artifact:** `legado.test.release`, artifact ID `13077223975`. The inspected universal APK is `legado_app_3.26110117_universal_新包名.apk`, SHA-256 `31e5a1db1ea47508a1c8193e8e9c1b5aeb49b585317e1e8466d2851e7038e0b3`. Its `META-INF/version-control-info.textproto` explicitly records revision `330e606c4d57e08adc056ff8ab7a4132c784da9a`; AGP metadata records 8.13.2. This binds the APK to the pinned source revision rather than merely relying on its version label.
- **Inspection method:** parsed the APK's three DEX files (`classes.dex`, `classes2.dex`, `classes3.dex`, DEX 035) for class definitions, interfaces, method IDs/prototypes, class-data access flags, generated methods and runtime-visible `JavascriptInterface` annotations. This is final release DEX evidence after R8, not an unminified pre-R8 JVM class dump.

#### Core compiled type and method inventory

| Pinned source type | Final DEX relationship / method evidence | Interpretation |
|---|---|---|
| `BaseSource` | Interface; 71 public methods; extends `JsExtensions`. | The source lifecycle interface is present in the release DEX. |
| `BookSource` | Implements `Parcelable` and `BaseSource`; 306 methods total / 304 public; 33 data-class components; 33-parameter primary constructor plus no-arg and synthetic default constructor. | Real upstream entity and inherited source API are present, not a host DTO. |
| `Book` | Implements `Parcelable` and `BaseBook`; 192 methods / 188 public; 34 components; 34-parameter primary constructor plus synthetic default constructor. | Real upstream book model and generated accessors/data-class ABI are present. |
| `BookChapter` | Implements `Parcelable` and the R8-obfuscated `RuleDataInterface`; 88 methods / 85 public; 17 components; 17-parameter primary constructor plus synthetic default constructor. | Real chapter identity/data model and inherited variable contract are present. |
| `BaseBook` / `RuleDataInterface` | BaseBook is an interface extending the obfuscated RuleDataInterface descriptor; the latter has five public abstract methods: getVariable, putVariable, getBigVariable, putBigVariable and getVariableMap. | Confirms the inherited variable-map and large-value hooks at the compiled type boundary. |
| `AnalyzeRule` | Implements `JsExtensions`; 213 methods / 199 public, including 140 bridge and 20 synthetic methods. | The parser class inherits a large concrete source-JS bridge surface; it is not selector-only. |
| `AnalyzeUrl` | Implements `JsExtensions`; 209 methods / 189 public, including 142 bridge and 12 synthetic methods. | Request parsing, synchronous/suspending response methods and JS bridge are in the compiled class. |
| `JsExtensions` | Interface; 148 public methods, including 30 synthetic bridge methods; extends the R8-obfuscated `JsEncodeUtils` descriptor. | The compiled JS bridge includes inherited methods, not only the 48 direct source annotations. |
| `JsEncodeUtils` | R8-obfuscated interface descriptor has 22 methods in this release DEX; source-to-DEX optimization/bridge mapping is not one-to-one. | Do not equate minified DEX interface count with source declaration count. |
| `BookListRule` | Interface with 20 public getters/setters for its ten inherited nullable properties. | Confirms ExploreRule/SearchRule inherited property ABI. |

#### Rule data-class generated ABI

| Rule model | DEX methods / public methods | Components | Primary constructor parameters |
|---|---:|---:|---:|
| ExploreRule | 44 / 42 | 10 | 10 |
| SearchRule | 47 / 45 | 11 | 11 |
| BookInfoRule | 50 / 48 | 12 | 12 |
| TocRule | 44 / 42 | 10 | 10 |
| ContentRule | 53 / 51 | 13 | 13 |
| ReviewRule | 105 / 103 | 30 | 30 |

All six rule classes are Parcelable in the inspected DEX and expose data-class component/copy/copy$default and synthetic default-constructor machinery. The ContentRule constructor uses `Integer` for nullable `maxBatchSize`; ReviewRule's `enabled` is a primitive boolean. These compiled types corroborate the source defaults already recorded in section 20; they do not replace null/missing/malformed JSON differential tests.

#### Exact compiled descriptors and annotation evidence

- Upstream `JsExtensions.getSource()`, `BaseSource.getSource()`, `BookSource.getSource()`, `AnalyzeRule.getSource()` and `AnalyzeUrl.getSource()` resolve to `io.legado.app.data.entities.BaseSource` (DEX descriptor `()Lio/legado/app/data/entities/BaseSource;`). This directly contrasts with Badcat's compiled `()Ljava/util/Map;`.
- Upstream `JsExtensions.getBatchContext()`, `AnalyzeRule.getBatchContext()` and `AnalyzeUrl.getBatchContext()` return the DEX descriptor `Lkp/d0;` (the R8-obfuscated type corresponding structurally to source `BatchContentContext`). Badcat's compiled method returns `Object` and its host implementation returns null.
- Upstream `BookSource.ajaxAll(String[])` and `ajaxAll(String[], boolean)` both return `StrResponse[]`; `ajaxTestAll(String[], int)` and `ajaxTestAll(String[], int, boolean)` also return `StrResponse[]`. Badcat's compiled bridge instead takes `Object` and returns `RhinoSourceHttpResponse[]`.
- Upstream DEX confirms `AnalyzeUrl.getStrResponse()` overloads with zero, one, two and three explicit arguments; `AnalyzeRule.getString` and `getStringList` likewise emit multiple Java-visible overloads from Kotlin defaults/`@JvmOverloads`. This validates why source-level function-name counts are insufficient.
- DEX annotation parsing found 68 `JavascriptInterface` method annotations on `JsExtensions`, 29 on `BaseSource` and 77 on `BookSource`. The 68 on JsExtensions reconcile with the 48 direct annotations in JsExtensions.kt plus 20 inherited annotations in JsEncodeUtils.kt; the additional nine BaseSource annotations account for the BaseSource/BookSource increase. The compiled Badcat bridge artifact has 14 such annotations.
- The R8 release obfuscates `RuleDataInterface` as `Llp/i0;`, `JsEncodeUtils` as `Lio/e1;` and the batch-context return type as `Lkp/d0;`. These mappings are inferred from the pinned source inheritance plus DEX relationships; the artifact did not include `mapping.txt`. The DEX descriptors are exact for this release artifact, but they are not a substitute for unminified source-name JVM descriptors.

#### Serializer entry points and limits

- The compiled `BookSource$Converters` has 13 public methods: constructor plus all 12 conversion methods for ExploreRule, SearchRule, BookInfoRule, TocRule, ContentRule and ReviewRule. `Book$Converters` has constructor plus the two ReadConfig conversion methods.
- This establishes that the converter surface is present in the pinned release APK. It does not prove edge-case parity for JSON null, missing fields, primitive-string payloads, malformed input or default preservation; fixed-SHA serializer differential fixtures remain outstanding.
- The DEX evidence proves final release runtime class/method presence and overloads after R8. It does not expose all pre-R8 generic signatures, Kotlin metadata, source nullability annotations or the exact unminified interface mapping. An unminified build or matching R8 mapping file is still needed to close those remaining compile-time ABI details.

**Assessment:** the pinned upstream release artifact supplies a strong, exact-SHA compiled runtime ABI reference for the core source/model/rule/JS classes. It confirms the authentic upstream inheritance and generated method surface and makes the host-vs-upstream return/overload differences concrete. Stage-0 ordered item 1 is materially advanced but remains open for the complete unminified ABI/nullability and serializer differential matrix; stage-0 remains in progress and stage-1 remains **NO-GO**.


### 24.7 Gson converter semantics and existing test coverage audit

- **Pinned source:** `BookSource.kt` lines 306–356 defines six `@TypeConverter` pairs: ExploreRule, SearchRule, BookInfoRule, TocRule, ContentRule and ReviewRule. Each `*ToString` uses `GSON.toJson`; each `stringTo*` uses `GSON.fromJsonObject<T>(json).getOrNull()`. The pinned APK DEX confirms the nested `BookSource$Converters` exposes all 12 conversion methods (13 public methods including constructor). `Book$Converters` exposes the two ReadConfig conversion methods.
- **Pinned Gson setup:** `INITIAL_GSON` registers Map, Int, String and LocalDate adapters, number strategy and HTML escaping behavior. `GSON` derives from INITIAL_GSON and registers all six rule deserializers; `GSONStrict` derives from GSON with strict JSON parsing. `fromJsonObject(String?)` returns a Result, fails for a null input string and catches parse/cast exceptions; the converter then collapses failures to null with `.getOrNull()`.
- **Rule deserializer branch:** each of the six rule deserializers calls INITIAL_GSON for a JSON object; for any JsonPrimitive it converts `asString` and parses that string as the rule object; all other shapes (including JSON arrays and JSON null) return null. This is broader than “legacy JSON string only”: numeric and boolean primitives also enter the primitive branch and require explicit behavior tests.
- **Existing Badcat test coverage inspected at HEAD `4079e292e43e18c79a3264c7b078d08b7cb63175`:**
  - `app/src/test/java/io/legado/app/data/entities/rule/PinnedLegadoRuleModelsTest.kt` has six tests covering object and legacy-string forms, selected fields across all six rule models, ContentRule/ReviewRule defaults, unknown fields, selected null behavior and numeric/string coercion. It directly checks `GSON.fromJson("null", ReviewRule::class.java)` returns null.
  - `legado-runtime/src/test/kotlin/io/legado/runtime/PinnedLegadoRuleModelContractTest.kt` has three tests covering object/string forms for all six models, selected ContentRule/ReviewRule defaults and unknown fields, and `GSON.fromJsonObject` on each model.
  - `LegadoBookSourceCodecTest` covers host descriptor JSON mapping, raw JSON retention, explicit null for enabledCookieJar and malformed known host fields. It tests the host codec, not the upstream `BookSource$Converters` or authentic BookSource entity.
- **Not covered by those tests:** direct invocation of all 12 BookSource converter methods; malformed JSON through each converter; null input string versus literal JSON `null`; array/boolean/number primitive inputs; explicit-null versus missing fields for every rule property; unknown nested values and round-trip behavior for each converter; and comparison of `toString()` / Rhino `JSON.stringify` output for all six models. The tests are useful evidence but do not constitute the full serializer differential matrix.
- **Semantic risk:** because converter `.getOrNull()` collapses parse failure and JSON-null/unsupported-shape results into a nullable rule, callers may not distinguish “rule intentionally absent” from “malformed rule payload”. Preserve this behavior only where upstream call sites expect it; do not normalize it into an empty rule without caller evidence.
- **Conclusion:** serializer source and final DEX converter entry points are now mapped, and current tests provide partial coverage. Serializer parity remains open until fixed-SHA differential fixtures exercise the listed edge cases and call sites establish null/failure expectations. No tests or production code were changed.


### 24.8 Exact source inventory, actual Rhino surface and test-report reconciliation

- **Source ABI artifact from #3301:** `legado-js-abi-inventory`, artifact ID `14862965256`; JSON SHA-256 `958621e650b7aaf79df7b5efa982319bc1984a9d6dd65b8bbd833ceab98d41e6`. Its `comparisonKind` explicitly says Kotlin source declaration inventory, not JVM descriptor proof. It records 148 upstream base declarations / 104 names versus 150 host declarations / 110 names, with 17 missing public names.
- **Correction to section 22:** the earlier lexical 18-name list included `canOpenSourceUi`. Pinned `JsExtensions.kt` line 1299 declares it `private`; it is an internal helper, not a source-facing API and must be excluded from the missing-public-name count. The corrected missing public-name list is 17: `getReadBookConfig`, `getReadBookConfigMap`, `getThemeConfig`, `getThemeConfigMap`, `getThemeMode`, `getVerificationCode`, `openUrl`, `openVideoPlayer`, `refreshBookInfo`, `refreshBookToc`, `refreshContent`, `showBrowser`, `startBrowser`, `startBrowserAwait`, `webView`, `webViewGetOverrideUrl`, `webViewGetSource`.
- **Actual Rhino surface artifact from #3301:** `legado-host-rhino-surface`, artifact ID `14862647140`; JSON SHA-256 `dcf93deee2be13a8cb405b784a1f50321d37a5264e14c8430559ea7ae06ff4c9`. It is a compiled Debug-runtime reflection and Rhino-wrapper inventory, not upstream parity evidence. The production runtime binds `java`, `cookie`, `source` and `cache` as four distinct objects. `RhinoSafeSourceJavaExtensions` has no implemented interfaces, is wrapped by `CatchableNativeJavaObject`, and exposes 154 declared public methods, 163 inherited public methods, 124 Rhino property IDs and 119 callable method names.
- Comparing the pinned release DEX `JsExtensions` public names with the actual Debug Rhino callable names on `scope.java` yields the same 17 missing public names as the source inventory. The union of the four host bindings still lacks those 17 names; they are not supplied by the separate cookie/login/cache objects. `getSource` and `getBatchContext` names exist but retain the already-recorded wrong return types/semantics. The source inventory also records overload differences for `ajaxAll`, `ajaxTestAll`, `get`, `timeFormat` and `timeFormatUTC`; exact types differ for several same-name methods.
- **Test report artifact from #3301:** `badcat-unit-test-reports`, artifact ID `14863755557`; ZIP SHA-256 `94243c86c72cf91b15f0d7c59771d4cb55b61c8b4bbcb95e390cb5f12ff3e787`. Direct ZIP enumeration and parsing found **41 JUnit XML suite files** and **335 tests**, 0 failures, 0 errors and 0 skipped, all bound to #3301 SHA `4ea24ef01c7c9fa7f8414ec9d3a50e0b0b1c6a68`. This corrects the earlier 40-file / 332-test count; the archive SHA-256 is unchanged.
- The report confirms `PinnedLegadoRuleModelsTest` (6 tests), `LegadoGsonCompatibilityTest` (6 tests) and `RhinoSourceJsAbiBytecodeTest` (3 tests) all passed. The Rhino tests cover compiled host descriptors/overloads, explicit script errors and null/scalar/object/array/no-argument invocation behavior.
- **Serializer coverage correction:** `LegadoGsonCompatibilityTest.allSixRuleConvertersAcceptObjectAndStringEncodedObject` exercises `GSON.fromJson` and string-encoded objects for all six models; despite its name, it does not directly invoke the 12 methods on `BookSource$Converters`. It also tests selected array/JSON-null cases, unknown fields, Gson coercion, LocalDate, HTML escaping and a ContentRule round-trip. This narrows the previous gap list but does not close it: direct BookSource converter invocation, malformed converter input, null input string versus literal JSON null, boolean/number primitives, explicit-null versus missing for all fields, all-six round-trip and all-six `toString()/JSON.stringify` remain untested.
- **Evidence limitation:** the Rhino surface artifact is from the Debug runtime and does not establish Release/R8 or Android WebView exposure; the upstream DEX artifact is Release/R8. These are complementary evidence, not interchangeable. The source inventory itself remains a source declaration scan.
- **Assessment:** the public API gap count is now corrected from 18 lexical names to 17 actual public names, and the same 17 are absent from the measured host Rhino callable scope. Host-side tests are green on #3301, but this is not upstream semantic parity. Stage-0 ordered item 1 remains open; Stage-1 remains **NO-GO**.


### 24.9 Additional inherited JS callback families — inventory boundary still open

- The #3301 source inventory artifact contains 190 direct declarations across six upstream scopes: JsExtensions 118, JsEncodeUtils 30, RssJsExtensions 17, WebJsExtensions 13, SourceLoginJsExtensions 6 and RegexJsExtensions 6. Inheritance is: JsExtensions -> JsEncodeUtils; RssJsExtensions -> JsExtensions; WebJsExtensions -> RssJsExtensions; SourceLoginJsExtensions -> RssJsExtensions; RegexJsExtensions -> JsEncodeUtils.
- The 17-name public gap in section 24.8 is specifically the base JsExtensions + JsEncodeUtils comparison; it is not the total gap count for every callback family.
- The current host source runtime binds four separate objects (`java`, `cookie`, `source`, `cache`); the replacement evaluator binds `LegadoRegexJsExtensions` plus `result`, `book` and `chapter`. There is no measured one-to-one mapping from these host objects to every upstream inherited callback scope.
- **WebJsExtensions (13 direct declarations):** includes `upConfig`, `request`, toast/log and HTTP helpers. The host `java` object has several same-name helpers but no `upConfig` or `request`; no caller/behavior proof establishes equivalence. The Web callback context remains unresolved.
- **SourceLoginJsExtensions (6 direct declarations):** includes `upLoginData`, `reLoginView`, `refreshExplore`, `copyText`, `clearTtsCache` and `showBrowser`. The host `source` object instead exposes key/variable/login-header/login-info operations. These are not same-name contracts; lifecycle and callback equivalence require caller mapping.
- **RegexJsExtensions (6 direct declarations):** includes `log`, `logType`, `t2s`, `s2t`, `get` and `put`. The replacement evaluator binds `LegadoRegexJsExtensions`, which exposes log/hash/crypto/get/put but not `t2s` or `s2t`; those two are present on the separate source `java` object, which is not the replacement evaluator's binding. This is a concrete context-specific gap unless a caller proves the methods unreachable or an equivalent binding is added later.
- **RssJsExtensions (17 direct declarations):** includes RSS/product-facing search/add/open/photo and related helpers. Do not automatically add RSS product UI or APIs; the master boundary excludes unrelated RSS product features. First prove whether any book-source execution or lifecycle caller actually depends on these methods.
- **Next dependency:** a complete caller/context graph is required to classify Web, SourceLogin, Regex and Rss scopes as required, host-adapted or safely excluded. This remains Stage-0 inventory work; no new scope is approved for implementation by this audit.


### 24.10 Direct JUnit artifact enumeration correction (2026-11-24)

- Directly downloaded artifact `14863755557` from #3301 and parsed every XML file. The archive SHA-256 is `94243c86c72cf91b15f0d7c59771d4cb55b61c8b4bbcb95e390cb5f12ff3e787`; it contains **41** `TEST-*.xml` suite files and **335** tests, with 0 failures, 0 errors and 0 skipped.
- The three directly relevant suites each passed: `PinnedLegadoRuleModelsTest` 6/6, `LegadoGsonCompatibilityTest` 6/6 and `RhinoSourceJsAbiBytecodeTest` 3/3.
- This corrects the previous 40 XML / 332 test count in section 24.8 and the category checklist. It is an enumeration correction only; no test result or #3301 exact SHA changed.


### 24.11 Pinned caller/context graph for inherited JS callback families

- **Pinned source:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. This slice traces concrete constructors and bridge injection points; it is not yet the exhaustive transitive caller/reflection graph required by Stage-0.
- **WebJsExtensions (13 direct declarations):**
  - Instantiated by `BackstageWebView.load` when a rule WebView is created; that path injects three separate bridges: the source wrapped with `withSourceNavigationContext`, `WebJsExtensions` as `java`, and `WebCacheManager` as `cache`. The navigation coroutine context is passed to the Web bridge.
  - Also instantiated in `BookInfoActivity` local-HTML intro, `WebViewActivity` local HTML, `BottomWebViewDialog` (with the dialog as callback), `VideoPlayerActivity`, and `ReadRssActivity`. Thus this is a live Android WebView bridge family, not a dead API inventory.
  - Its direct `request` method dispatches named operations (`run`, ajax/connect/get/head/post and WebView extraction) asynchronously in the Activity lifecycle scope; it normalizes up to six string parameters, resolves results and reports errors through the JavaScript callback registry. `upConfig` forwards config changes through a weak callback reference. The host runtime's measured source scope has no `request` or `upConfig` equivalent and no WebJsExtensions implementation.
- **SourceLoginJsExtensions (6 direct declarations):**
  - Instantiated in `SourceLoginDialog`, `ExploreAdapter`, `SourceCallBack`, `BookInfoViewModel`, `ReadBookActivity` and `VideoPlayerViewModel`. These are concrete book-source login, explore, book-info, reader, video and source event callback contexts.
  - The direct methods are `upLoginData`, `reLoginView`, `refreshExplore`, `copyText`, `clearTtsCache` and the overridden `showBrowser`. The first three forward UI data/re-render callbacks through a weak callback reference; `showBrowser` opens a `BottomWebViewDialog` with source/book type and custom-button correlation key. This family is not equivalent to the host's `RhinoSourceLoginExtensions`: that host class is a persistent login/session data bridge exposing source key/variables/login header/login info, not the upstream UI callback bridge.
- **RegexJsExtensions (6 direct declarations):**
  - Constructed by `RegexExtensions.replace` for production `@js:` replacement rules and by `ReplacePreview` for preview. The script context binds `result`, `chapter`, `book` and a fresh `RegexJsExtensions` as `java`; execution runs on IO with cancellation and a rule timeout.
  - The upstream object inherits `JsEncodeUtils` and adds `log`, `logType`, `t2s`, `s2t`, `get` and `put`. Badcat's `RhinoLegadoJsReplacementEvaluator` binds a standalone `LegadoRegexJsExtensions`, not the source-runtime `RhinoSafeSourceJavaExtensions`; its replacement scope has `log/logType/get/put` and crypto helpers but lacks `t2s/s2t`. The fact that the source-rule `java` object contains conversion helpers does not fill a different replacement-scope binding.
- **RssJsExtensions inheritance boundary:**
  - Directly instantiated by `ReadRssActivity` and subclassed by `WebJsExtensions`, `SourceLoginJsExtensions` and a legacy review adapter. It adds source-variable `put/get`, `searchBook`, `addBook`, `showPhoto`, `open` and AnalyzeRule-backed content/selector helpers.
  - Because Web and SourceLogin subclasses inherit these methods, the entire Rss product module is not automatically required, but these inherited JS methods are present in book-source Web/login contexts. The master exclusion of unrelated RSS product UI must not be used to silently remove a method that is reachable through a book-source subclass; classify method-by-method from callers.
- **Badcat runtime binding comparison at inspected source HEAD `d85e571068391298a30352a2d6f4062dbaa0b813`:**
  - `RhinoSourceScriptRuntime` binds `java = RhinoSafeSourceJavaExtensions`, `cookie = RhinoSourceCookieExtensions`, `source = RhinoSourceLoginExtensions`, and `cache = RhinoSourceCacheExtensions`.
  - The `java` bridge is a standalone class with no upstream interface. The `source` object does not implement upstream `BaseSource` or `SourceLoginJsExtensions`; it exposes persisted session/variable methods only. No `WebJsExtensions`, `SourceLoginJsExtensions` UI callback adapter or `RssJsExtensions` equivalent is wired into the source runtime.
  - `RhinoLegadoJsReplacementEvaluator` binds only `LegadoRegexJsExtensions` plus `result/book/chapter`; it has no conversion aliases from the separate source-rule Java bridge.
- **Host-port consequences (inventory only, no implementation authorization):** browser and WebView execution, verification-code UI, Activity/lifecycle ownership, callback delivery, source navigation gating, reader/book-info refresh events, login UI re-rendering, explore refresh, custom-button de-duplication, player launch, and replacement Chinese conversion all require explicit typed ports or an upstream-compatible callback owner. Their absence is not repaired by the existence of generic HTTP, Cookie, cache or login-session methods.
- **Scope decision:** this evidence proves Web and SourceLogin scopes are reached by book-source workflows and that RegexJsExtensions is used by the book-source replacement engine. They cannot be classified as unrelated RSS-only UI. RssJsExtensions' extra helpers are inherited into those scopes and need caller-by-caller classification. The full transitive callback/reflective dispatch graph remains open.
- **Test evidence:** upstream `SourceNavigationUiTest` exercises `java.openUrl`, `source.openUrl`, `java.startBrowser`, `java.showBrowser`, `java.openVideoPlayer` and the blocked `startBrowserAwait` path. Existing Badcat #3301 Rhino ABI tests cover host bridge dispatch only; they do not provide these upstream UI callback behaviors.
- **Conclusion:** the six-scope inventory now has concrete constructor/injection paths and a host binding comparison. Stage-0 ordered item 1 remains open for exact full ABI/nullability and serializer fixtures; item 2 remains open for exhaustive transitive/reflection call closure; Stage-1 remains **NO-GO**. No production code, tests, Gradle, workflow, UI, database or source fixture was changed.


### 24.12 Rss / Web / SourceLogin / Regex concrete scope reachability

- **Pinned source:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. This slice distinguishes ordinary source execution, explicit Rhino callback injection and Android WebView JavaScript-interface exposure. A method inherited by a class is not automatically exposed in every one of those contexts.
- **Ordinary source `java` is not RssJsExtensions:** `BaseSource.evalJS` binds `java`, `source` and `sourceApi` to the current BaseSource (BaseSource.kt:414–422). `JsSourceEngine.buildScope` binds `java` to the JsSourceEngine and `source/sourceApi` to its BookSource (JsSourceEngine.kt:104–115). Neither class extends RssJsExtensions. Do not model the 17 Rss declarations as universally available on every source-rule evaluation.
- **BookSource Rhino callback scope is confirmed:** `SourceLoginJsExtensions` extends `RssJsExtensions` (SourceLoginJsExtensions.kt:21–28). It is explicitly injected as `java` into BookSource callback scripts:
  - login UI button action and login callback in `SourceLoginDialog` (lines 797–807, 842–852);
  - BookInfo click action in `BookInfoViewModel` (625–632);
  - reader pay action and image-link click in `ReadBookActivity` (2258–2268, 2308–2318, 2347–2357);
  - video-player click in `VideoPlayerViewModel` (82–89);
  - source event callback in `SourceCallBack` (91–103);
  - Explore UI button callback in `ExploreAdapter` (578–589).
  These are real Rhino callback contexts, not just source-help examples. The scripts also receive contextual `result/book/chapter/event/infoMap/isLongClick` values, and the callback object carries Activity/bookType/callback references.
- **Book-source use of inherited Rss method is explicit:** pinned `jsHelp.md` lines 45–51 documents `java.searchBook(key, source)` as the “only this source” search form when called from a book source. `RssJsExtensions.searchBook(key: String, source: BookSource)` implements it by calling `SearchActivity.start(activity, source, key)` (RssJsExtensions.kt:81–84). This overload has no `@JavascriptInterface` annotation; its confirmed path is the injected Rhino `SourceLoginJsExtensions` object, not the Android WebView bridge. The separate `searchBook(key, searchScope)` overload is annotated and uses the general search scope (lines 73–79).
- **Other inherited Rss methods must be classified by context, not package name:**
  - `put/get` operate on the active BaseSource variables (lines 62–70).
  - `addBook`, `showPhoto` and `open` are annotated WebView-callable helpers; `open` includes `login`, `explore`, `search`, `rss` and `sort` navigation branches (lines 87–110 and following).
  - `setContent`, `setBaseUrl` and `setRedirectUrl` delegate to the active AnalyzeRule (lines 284–298). `getStringList`, the three-argument `getString`, `getElement` and `getElements` are public AnalyzeRule helpers but are not annotated in RssJsExtensions (lines 300–320); do not infer Android WebView visibility from Rhino reachability.
  - The pinned help guide says these Rss-specific helpers are for subscription `shouldOverrideUrlLoading` and inherited by SourceLoginJsExtensions (jsHelp.md:39–43). The explicit BookSource `searchBook(key, source)` exception above means the family is not wholly RSS-only. Do not import unrelated RSS product UI wholesale; retain a method/caller-specific compatibility decision.
- **WebView bridge is a separate runtime surface:** `WebJsExtensions` extends RssJsExtensions and is injected as `java` by `BackstageWebView.load` for rule HTML alongside a navigation-context-wrapped BaseSource as `source` and WebCacheManager as `cache` (BackstageWebView.kt:113–129; WebJsExtensions.kt:21–31). Its JavaScript-interface entry points `request(funName, jsParam, id)` and `upConfig(config)` are annotated. `request` dispatches 16 exact names: `run`, `ajaxAwait`, `connectAwait`, `getAwait`, `headAwait`, `postAwait`, `webViewAwait`, `webViewGetSourceAwait`, `decryptStrAwait`, `encryptBase64Await`, `encryptHexAwait`, `createSignHexAwait`, `downloadFileAwait`, `readTxtFileAwait`, `importScriptAwait`, and `getStringAwait` (WebJsExtensions.kt:49–160). Results/errors are stored by request ID in CacheManager memory and settled via `evaluateJavascript` (lines 161–167); work is launched in the Activity lifecycle scope (lines 49–52). This is not equivalent to a synchronous generic HTTP port.
- **SourceLogin UI callbacks are a separate scope:** direct methods are `upLoginData`, `reLoginView`, `refreshExplore`, `copyText`, `clearTtsCache` and the `showBrowser` override. Callback methods use a weak callback reference; browser launch preserves `bookType` and `customButtonKey` in BottomWebViewDialog. These methods are passed to Rhino callback bindings, not made equivalent by the existing Login UI v2 row/action service.
- **Regex replacement is another independent scope:** upstream `RegexExtensions.replace` recognizes the `@js:` prefix, binds `result/chapter/book/java` with `java = RegexJsExtensions`, evaluates on IO, and enforces timeout/cancellation (RegexExtensions.kt:28–75). Preview separately binds the same extension and enforces a preview deadline (ReplacePreview.kt:54–119). `RegexJsExtensions` provides `log/logType/t2s/s2t/get/put`; `t2s/s2t` are not supplied by Badcat's current replacement evaluator binding. The source-rule `java` bridge does not satisfy this separate replacement scope.
- **Host comparison:** the inspected Badcat runtime binds `java/cookie/source/cache` in source scripts and a distinct `LegadoRegexJsExtensions` in replacement scripts. It has no WebJsExtensions implementation or SourceLoginJsExtensions callback object. The host `source` object is a persistent login/session store bridge, not the upstream BaseSource live object or the SourceLogin UI callback owner.
- **Classification:** the SourceLogin-injected Rss family is a confirmed BookSource callback dependency; WebJsExtensions is a confirmed BookSource rule-WebView dependency; RegexJsExtensions is a confirmed replacement-rule dependency. Rss methods used only by subscription flows remain product-scope candidates requiring callsite/script evidence; no full RSS module/UI import is authorized. This slice narrows reachability but does not close all reflection, external source-script corpus or transitive caller paths.
- **Conclusion:** the “Rss excluded unless proven book-source reachable” assumption is superseded for the inherited callback scope and the `searchBook(key, BookSource)` overload. Stage-0 ordered item 1 and item 2 remain open; Stage-1 remains **NO-GO**. No production code, tests, Gradle, workflow, UI/Reader, database/schema or source fixtures were changed.


### 24.13 Method-level classification of RssJsExtensions.open

- **Pinned implementation:** `RssJsExtensions.open(name, url, title, origin)` runs inside the owning Activity's `lifecycleScope` on IO, resolves the active BaseSource and dispatches by route name (RssJsExtensions.kt:98–249).
- **BookSource-relevant branches:**
  - `login`: resolves a target BookSource or RssSource by `origin` (or uses the current source), checks `hasLogin()`, builds a `SourceLoginRoute` using the current `bookType`, then starts SourceLoginActivity on Main (lines 104–136). This is not just RSS navigation.
  - `search`: resolves `origin` through BookSourceDao and calls the BookSource overload `searchBook(title, source)`; otherwise falls back to the general search overload (lines 221–230).
  - `explore`: resolves the BookSource from `origin` or the current source, then starts ExploreShowActivity with the explore URL/title/source key on Main (lines 233–245).
- **RSS-product-only branches:** `sort` and `rss` require/resolve an RssSource and start RssSortActivity/ReadRssActivity (lines 138–219). These product screens remain outside the Legado Runtime core unless a separate product integration task authorizes their existing Badcat equivalent; do not import Legado RSS UI.
- **Other inherited methods:** `addBook` and `showPhoto` launch AddToBookshelfDialog and PhotoDialog (lines 87–95). They are inherited by SourceLoginJsExtensions, but no BookSource-specific script/caller evidence in this slice establishes that their UI is part of the minimum source runtime. Keep them as reachable extension methods with unresolved product-port requirements rather than silently classifying them as core runtime or RSS-only.
- **Implication:** compatibility must preserve the method's BookSource `login/search/explore` route semantics and BookType/source-origin propagation without importing the unrelated RSS product screens. A typed host action boundary must retain source key, origin, bookType, URL/title, lifecycle dispatch and route failure behavior.
- **Conclusion:** RssJsExtensions cannot be excluded wholesale, and it cannot be imported wholesale either. The current evidence supports method/route-level partitioning. This remains inventory evidence only; no product/UI port or implementation is authorized.


### 24.14 Operation-scoped source-navigation suppression and WebView context propagation

- **Pinned source:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Policy primitive:** `SuppressSourceNavigation` is a dedicated `CoroutineContext.Element`; suppression is `AppConfig.blockSourceNavigation && context[SuppressSourceNavigation] != null` (SourceNavigationPolicy.kt:5–12). The comment explicitly limits the restriction to the operation carrying that element so parallel reader/login work remains interactive.
- **How JS extensions obtain the operation context:** `JsExtensions.context` combines the active Rhino coroutine context with `getSourceNavigationContext()` (JsExtensions.kt:125–135, 169–171). The default is EmptyCoroutineContext. `WebJsExtensions` overrides the getter with its captured navigation context (WebJsExtensions.kt:21–31).
- **Actual operation roots carrying the marker:** `WebBook.searchBookAwait` wraps search in `withContext(SuppressSourceNavigation)` (WebBook.kt:53–59); CheckSourceService wraps source checking (CheckSourceService.kt:279); ChangeBookSourceViewModel marks search/change-source work (including lines 327, 581, 686, 728, 794); ChangeChapterSourceViewModel marks chapter-source content/cache work (lines 230, 328). This is operation-level policy, not a global UI disable.
- **Cross-thread WebView bridge:** `BackstageWebView.load(navigationContext)` wraps the BaseSource with `withSourceNavigationContext` before registering it as the WebView `source` interface. The wrapper is a delegating BaseSource and:
  - forwards `login()` through `runScriptWithContext(context)`, so nested login JS retains the originating operation context;
  - gates `openUrl` and `openVideoPlayer` against the current operation's suppression policy;
  - preserves all other BaseSource behavior through delegation (SourceNavigationBridge.kt:10–36; BackstageWebView.kt:113–129).
  WebView invokes JavaScript interfaces on a different thread, so this bridge is required to restore the original operation context across that boundary.
- **Additional gates:** JsExtensions itself checks `canOpenSourceUi()` before `openUrl`, `openVideoPlayer`, `startBrowser`, `startBrowserAwait` and `showBrowser`. The awaited browser path throws a specific blocked-navigation exception; non-awaited UI actions return without launching UI (JsExtensions.kt:377–425, 1238–1303).
- **Android instrumentation proof:** `SourceNavigationUiTest` verifies:
  - default-off preserves the pre-existing open behavior;
  - with the setting enabled, a real `WebBook.searchBookAwait` still returns the book while the nested `java.openUrl` is suppressed;
  - within a marked context, Java and source `openUrl`, start/show browser and video launch are suppressed; `startBrowserAwait` reports a blocked result; no Activity/Fragment is launched;
  - WebView's separate Java thread preserves the policy for both `java` and `source` bridges, while `source.login()` and source variable get/put remain functional;
  - after the marked search, ordinary detail login and explore operations remain interactive.
  The companion SourceNavigationPolicyTest verifies opt-in and operation-local context isolation.
- **Badcat comparison:** inspected `RhinoSafeSourceJavaExtensions.getSourceNavigationContext()` returns `EmptyCoroutineContext` (current host source file line 206); the runtime's measured source bridge does not carry an operation marker or an equivalent WebView context wrapper. The 17 missing public API list also includes the upstream navigation entry points. No host parity is established.
- **Required typed-port semantics (inventory only):** carry the operation CoroutineContext/suppression marker through the Rhino execution and WebView callback; preserve opt-in behavior, source identity and UI callback ownership; gate only the current operation; propagate context through nested login; keep storage/network work alive; preserve cancellation and exception behavior. A global boolean checked without operation identity is not equivalent.
- **Scope guard:** this is the pinned source-navigation behavior within the Runtime API/caller audit. It does not reopen or modify the separately paused security preflight. No security preflight, production code, tests, Gradle, workflow, UI/Reader, database/schema or source fixture was changed.
- **Conclusion:** source-navigation policy and its WebView thread bridge are a confirmed upstream runtime contract with Android instrumentation evidence. The current Badcat EmptyCoroutineContext path is a concrete semantic gap. Stage-0 ordered items 1 and 2 remain open; Stage-1 remains **NO-GO**.


## 24.15 WebJsExtensions dynamic-dispatch contract and Badcat host-scope gap (2026-11-24)

- **Pinned baseline:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. This is a source-level closure slice; it does not claim a complete external script-corpus scan.
- **Exact upstream source identities:**
  - `app/src/main/java/io/legado/app/help/webView/WebJsExtensions.kt` — blob `dd6532396b8f02e37376885aa2c6c1e430af1ad9`.
  - `app/src/main/java/io/legado/app/help/http/BackstageWebView.kt` — blob `285f229ff2f9f00e7588b221a8543a2879bafe86`.
  - `app/src/main/java/io/legado/app/ui/rss/read/RssJsExtensions.kt` — blob `8e01035e8d5802ed931ac46050cd45cfb0254701`.
  - `app/src/main/java/io/legado/app/ui/login/SourceLoginJsExtensions.kt` — blob `a14f01b3774998d09619a79ce977fc593b6a28f1`.
- **WebView injection is a real source execution path:** pinned `BackstageWebView.load` creates the source-rule WebView with three distinct JavaScript interfaces: a BaseSource wrapped by `withSourceNavigationContext(navigationContext)` as `source`, `WebJsExtensions` as `java`, and `WebCacheManager` as `cache`. The captured operation navigation context is passed into WebJsExtensions. This is not the ordinary Rhino source-script binding and must not be collapsed into it.
- **Direct WebJsExtensions API and dispatch:**
  - `upConfig(config)` is an annotated JavaScript interface callback; it de-duplicates the last forwarded config and invokes the weak callback reference (WebJsExtensions.kt:38–43).
  - `request(funName, jsParam, id)` is the annotated asynchronous dispatcher (lines 48–59). It normalizes six string parameters, dispatches the following **16 exact operation names**, and settles success/error by placing the result in CacheManager memory and invoking `window.$JSBridgeResult(id, success)` (lines 59–167):
    1. `run`
    2. `ajaxAwait`
    3. `connectAwait`
    4. `getAwait`
    5. `headAwait`
    6. `postAwait`
    7. `webViewAwait`
    8. `webViewGetSourceAwait`
    9. `decryptStrAwait`
    10. `encryptBase64Await`
    11. `encryptHexAwait`
    12. `createSignHexAwait`
    13. `downloadFileAwait`
    14. `readTxtFileAwait`
    15. `importScriptAwait`
    16. `getStringAwait`
  - The pinned injected JavaScript shim defines Promise wrappers for these operations, generates request IDs, stores resolve/reject callbacks, invokes `java.request`, then consumes/deletes the CacheManager result in `window.$JSBridgeResult` (WebJsExtensions.kt:241–375). This proves the string dispatch names are actively called by the upstream-generated source-WebView helper, not merely hypothetical reflection names.
- **Related inherited callback boundary:** SourceLoginJsExtensions extends RssJsExtensions and is injected into BookSource Rhino callbacks, while WebJsExtensions extends RssJsExtensions and is injected into rule WebViews. The Rss `open(name,...)` dispatcher has five route labels: `login`, `sort`, `rss`, `search`, `explore`; `login/search/explore` resolve or preserve BookSource origin/bookType, while `sort/rss` enter RssSource product flows. The BookSource `searchBook(key, BookSource)` overload is a distinct Rhino callback route, not the annotated WebView search-scope overload. This reinforces method/route-specific scope classification rather than wholesale Rss inclusion/exclusion.
- **Badcat exact host comparison at inspected branch HEAD `52f5581fefe5b6c7852a85ea8c4e782c81af498b`:**
  - `app/src/main/java/app/badcat/client/runtime/RhinoSafeSourceJavaExtensions.kt` — blob `6a00e649895c4c1d223b768c6b288632ec033de9`.
  - `RhinoSourceScriptRuntime.kt` — blob `879d6ed978894b16271cf5207348176fd040eda7`.
  - `RhinoLegadoJsReplacementEvaluator.kt` — blob `bc20e8b378ce65c8455b6d2763c881c66979dc47`.
  - Production Rhino source scope binds the custom `RhinoSafeSourceJavaExtensions` as `java`, plus separate `cookie`, `source` and `cache` objects. Replacement rules bind a distinct `LegadoRegexJsExtensions`. The inspected host tree contains no `WebJsExtensions` implementation or WebView `request(funName, params, id)` dispatcher.
  - The host bridge contains a no-argument `RhinoSourceRawResponse.request()` that returns raw request metadata; this is **not** the upstream three-argument WebJsExtensions dispatcher. There is no `upConfig` callback or matching `webViewAwait/webViewGetSourceAwait` operation family in the inspected host source runtime.
- **Compatibility consequence:** a generic host HTTP `ajax/get/post` method is not equivalent to the WebView bridge's 16-operation contract. In particular, the WebView-backed extraction, async Promise result/error settlement, temporary CacheManager result lifecycle, imported-script/text-file/download operations and config callback are distinct observable behaviors. They need explicit typed host ports or a source-faithful retained implementation; silently omitting the Web scope cannot be called full BookSource compatibility.
- **Boundary:** do not import Legado's WebView UI/product screens wholesale. Preserve the WebView rule-execution/callback semantics only where source execution reaches them, with operation context, source identity, lifecycle, cancellation, error and callback behavior explicitly mapped.
- **Status:** this closes one concrete dynamic-dispatch inventory slice and independently confirms the WebJsExtensions host gap. It does not close all inherited method callers, arbitrary external source scripts, reflection/generated ABI, serializer differential fixtures or complete dependency closure. Stage-0 remains in progress; Stage-1 remains **NO-GO**. No production code, tests, Gradle, workflow, database/schema, UI/Reader or source fixtures changed.


## 24.16 RssJsExtensions and SourceLogin method-level exposure matrix (2026-11-24)

- **Pinned source evidence:** `RssJsExtensions.kt` blob `8e01035e8d5802ed931ac46050cd45cfb0254701`; `SourceLoginJsExtensions.kt` blob `a14f01b3774998d09619a79ce977fc593b6a28f1`; source help `app/src/main/assets/web/help/md/jsHelp.md` blob `17f51c144ccfeb33bb228df29f0e5600206a1a98`. The help file explicitly documents method usage and the contexts in which SourceLogin-only callbacks are valid.
- **RssJsExtensions direct method surface (source lines 54–319):**
  - Source identity accessors: `getSource(): BaseSource?` and `getTag(): String?` (not `@JavascriptInterface`).
  - Source variables: `put(key,value)` and `get(key)` are annotated JavaScript-interface methods and operate on the active BaseSource variable store.
  - Search: annotated `searchBook(key, searchScope?)` opens general search; the distinct unannotated `searchBook(key, BookSource)` overload starts search constrained to that exact source. The pinned help explicitly documents `java.searchBook(key, source)` as “only this source” (jsHelp.md:45–51).
  - Product/UI helpers: annotated `addBook(bookUrl)` and `showPhoto(src)`; these launch Badcat-equivalent host actions if supported, but their UI effects cannot be replaced by empty methods.
  - Navigation: annotated `open(name,url,title,origin)`; its route labels and source-origin semantics are detailed in §24.13.
  - AnalyzeRule helpers: annotated `setContent`, `setBaseUrl`, `setRedirectUrl`, and the two-argument `getString(rule,unescape)`; public `getStringList`, three-argument `getString(rule,mContent,isUrl)`, `getElement` and `getElements` are not annotated. They are still public methods on the Rhino callback object; annotation visibility and Rhino Java-object reachability are separate questions.
- **SourceLoginJsExtensions callback surface (source lines 21–82):** it extends RssJsExtensions and adds `upLoginData`, `reLoginView`, `refreshExplore`, `copyText`, `clearTtsCache` and an overridden `showBrowser`. The pinned help documents these functions as valid only in login UI buttons/callbacks, explore-button functions, image-link click handlers and purchase-rule contexts (jsHelp.md:74–99). It also documents inherited `refreshBookInfo`, `refreshBookToc` and `refreshContent` callbacks. These are UI/event lifecycle operations, not aliases for persistent login/session key-value storage.
- **Host comparison at inspected branch HEAD `2a20653e7decad1c9d476c6587e523e4ad86b9ee`:**
  - The host has `put(key,value)` and `get(urlOrKey)`, but the latter is a host convenience that branches between HTTP GET and stored-string lookup; it is not proof of the exact upstream overload/owner behavior.
  - Host `setContent` writes the active content and returns a String; upstream returns the AnalyzeRule object and separately exposes `setBaseUrl` / `setRedirectUrl`. The host has no same-name `setBaseUrl` or `setRedirectUrl` method.
  - Host `getStringList` returns `Array<String>` and host element methods return safe wrapper types; upstream declares nullable/list/element return types tied to AnalyzeRule. Same-name methods are not signature or object-identity parity.
  - Host has no `searchBook`, `open`, `addBook`, `showPhoto`, SourceLogin callback methods, or matching refresh/browser UI callback owner in the inspected Rhino source scope.
- **Compatibility classification:** source-variable storage and selector helpers have partial host counterparts, but the method overloads, return objects, parser mutation semantics, BookSource-constrained search and UI callback lifecycle are not yet proven equivalent. The missing callbacks must become explicit typed Host Ports or remain documented blockers; no-op stubs and generic login-session storage are not substitutes.
- **Boundary:** this does not authorize importing Legado RSS product screens, login UI, Reader UI or dialogs. It identifies only methods actually inherited into BookSource callback/WebView contexts and their required behavior.
- **Status:** this closes a method/annotation/documented-context matrix for RssJsExtensions and SourceLoginJsExtensions. It does not close the external source-script corpus, every inherited JsExtensions callsite, exact compiled ABI/nullability or serializer/dependency gates. Stage-0 remains in progress; Stage-1 remains **NO-GO**. No production code, tests, Gradle, workflow, UI/Reader, database/schema or source fixtures changed.


## 24.17 AnalyzeRule preUpdateJs lifecycle operations — reGetBook / refreshTocUrl caller closure (2026-11-24)

- **Pinned source identities:** `AnalyzeRule.kt` blob `7591a026a072ac8a6fe4902add0795812fe0a0a6`; `WebBook.kt` blob `d7a924c2d687bc99d6e25099cefacc9d920e1386`; `BookInfoViewModel.kt` blob `eb0b5853a7e880eb62e33ea1cca4210262adbbb0`; `BookChapterList.kt` blob `dd2ae3950cee55a9c8e7880dc2c034d6f4c6f33d`; `MainViewModel.kt` blob `feb8d15067c05af068b1db8b55c3bd0063619135`.
- **Documented caller contract:** pinned `jsHelp.md` lines 150–157 documents `java.reGetBook()` and `java.refreshTocUrl()` specifically for use before refreshing the TOC, where a source may change a book URL or TOC URL. These names were absent from the direct JsExtensions/Rss/Web class declaration set because they are methods on the active AnalyzeRule object.
- **Exact precondition and side effects (AnalyzeRule.kt:985–1024):**
  - Both methods throw unless the active AnalyzeRule was constructed with `preUpdateJs = true`; they are not general-purpose methods available in every selector/JS context.
  - `reGetBook()` requires the active source to be a BookSource and rule data to be a Book. It runs `WebBook.preciseSearchAwait(bookSource, book.name, book.author)`, replaces `book.bookUrl` with the precise-search result, merges the returned variable map into the active Book, then calls `WebBook.getBookInfoAwait(bookSource, book, false)`. The operation is run in the active coroutine context with a 1,800,000 ms timeout.
  - `refreshTocUrl()` has the same `preUpdateJs` gate and BookSource/Book requirements, then calls `WebBook.getBookInfoAwait(bookSource, book, false)` under the same timeout. If `isFromBookInfo` is true, it logs and returns early to avoid duplicate detail-page loading.
- **Real lifecycle/caller chain (WebBook.kt:283–335):** `runPreUpdateJs` reads `bookSource.ruleToc.preUpdateJs`, constructs `AnalyzeRule(book, bookSource, true, isFromBookInfo)`, carries the current coroutine context and evaluates the script. `getChapterListAwait` runs it only when `runPerJs` is true and does so before fetching/parsing the chapter list; the JS-source BookSource path returns through JsSourceBook separately. The BookInfoViewModel and MainViewModel pass the detail/update context flags into this flow; BookChapterList also carries `isFromBookInfo` into AnalyzeRule during TOC parsing.
- **Host comparison:** Badcat's current `RhinoSafeSourceJavaExtensions` has no `reGetBook` or `refreshTocUrl` method. Its production JS scope is not an upstream AnalyzeRule instance, and the separately implemented host detail/TOC services do not make these methods callable through the source script's `java` object. No equivalent source identity mutation, precise-search/detail refresh, preUpdateJs-only gate, duplicate-detail suppression or coroutine/timeout behavior is established by the inspected host code.
- **Compatibility consequence:** these are source-lifecycle operations with observable Book identity, variable-map and detail/TOC side effects. A same-name no-op or a generic detail refresh is not equivalent. A faithful host port must preserve the preUpdateJs-only call gate, active Book/BookSource identity, precise-search result propagation, detail refresh, `isFromBookInfo` suppression, timeout and cancellation/error behavior.
- **Status:** this closes the documented source-script reachability and upstream caller path for two AnalyzeRule operations. It does not close all AnalyzeRule/AnalyzeUrl methods, dynamic script callers, complete JS ABI or host semantic parity. Stage-0 remains in progress; Stage-1 remains **NO-GO**. No production code, tests, Gradle, workflow, UI/Reader, database/schema or source fixtures changed.


## 24.18 AnalyzeUrl loginCheckJs execution scope and request-replay API (2026-11-24)

- **Pinned source identities:** `AnalyzeUrl.kt` blob `d898c88dfd844d73f06d9396d0b46f39939f7e6c`; `WebBook.kt` blob `d7a924c2d687bc99d6e25099cefacc9d920e1386`; source help `jsHelp.md` blob `17f51c144ccfeb33bb228df29f0e5600206a1a98`.
- **Documented scope:** jsHelp.md lines 118–124 explicitly says the listed AnalyzeUrl methods are callable through `java.` only in the source's login-check JS rule: `initUrl()`, `getHeaderMap()`, `getStrResponse(...)` and `getResponse()`. This is a different `java` object from the ordinary BaseSource JS scope, the SourceLogin callback object and the WebJsExtensions WebView bridge.
- **Exact AnalyzeUrl binding (AnalyzeUrl.kt:395–420):** `evalJS` binds `java = this` (the active AnalyzeUrl instance), plus `baseUrl`, `cookie`, `cache`, `page`, `key`, `speakText`, `speakSpeed`, `book`, `source`, `result` and `infoMap`; it also uses the source's shared JS scope when available. Therefore login-check scripts can mutate/request through the active request object, not through a detached metadata Map.
- **Method behavior:**
  - `initUrl()` resets `ruleUrl` from the original `mUrl`, reruns embedded `@js` URL fragments, key/page replacement and URL analysis (AnalyzeUrl.kt:175–183). It is an active request reconstruction operation after login/header changes.
  - `getHeaderMap()` returns the current request header map, accepting either a Map or a JSON string representation (lines 945–951).
  - `getStrResponse(...)` is a synchronous wrapper around `getStrResponseAwait` and returns the upstream `StrResponse` (lines 581–589); `getResponse()` is a synchronous wrapper around `getResponseAwait` and returns the raw OkHttp `Response` (lines 677–681). These are distinct return contracts.
- **BookSource caller path (WebBook.kt:322–347):** the engine constructs one AnalyzeUrl for the TOC request, fetches the initial response, then executes `bookSource.loginCheckJs` with that same AnalyzeUrl as `java` and the response as `result`. The error path also invokes loginCheckJs with an error StrResponse; a returned status 500 rethrows the original request failure. This makes the script a request/session recovery hook, not a generic post-response transform.
- **Concrete documented behavior:** jsHelp includes an example where a failed API response causes the script to call `source.login()`, merge `source.getHeaderMap(true)` into `java.getHeaderMap()`, then retry with `java.getResponse()`. The same helper section documents `initUrl()` for re-evaluating dynamic URL rules after login.
- **Host comparison:** Badcat's `RhinoSafeSourceJavaExtensions` does not expose `initUrl`, `getHeaderMap`, `getStrResponse` or `getResponse`; production `java` is not the active AnalyzeUrl object. Host HTTP helpers and `RhinoSourceRawResponse` metadata accessors do not provide the same mutable request/header/replay lifecycle or upstream Response/StrResponse return objects.
- **Scope guard:** `loginCheckJs` also appears on other upstream product models such as RssSource and HttpTTS; those product flows remain outside this BookSource-runtime task unless an actual BookSource dependency requires them. The BookSource WebBook path above is confirmed and must be preserved without importing unrelated RSS/TTS product UI.
- **Status:** this closes the documented login-check JS API and BookSource caller path for four AnalyzeUrl methods, including the same-object request replay/error recovery behavior. Other AnalyzeUrl overloads, all JsExtensions callers, compiled ABI/nullability and host semantic parity remain open. Stage-0 remains in progress; Stage-1 remains **NO-GO**. No production code, tests, Gradle, workflow, UI/Reader, database/schema or source fixtures changed.

## 24.19 AnalyzeRule selector/content semantics and AnalyzeUrl.UrlOption request mapping (2026-11-24)

- **Inspection snapshot:** Badcat branch `integration/legado-upstream-runtime` at `cd1d809bfdda8abf2a3200170fba92e5ab976f2c`; pinned upstream remains `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. This is a source/caller audit, not a production implementation or differential-test pass.
- **Exact source identities:**
  - Upstream `AnalyzeRule.kt` blob `7591a026a072ac8a6fe4902add0795812fe0a0a6`; `AnalyzeUrl.kt` blob `d898c88dfd844d73f06d9396d0b46f39939f7e6c`; `AnalyzeUrlNetworkOptions.kt` blob `00011436d0494b5ad1ca51d2835f2553b897e3e2`; `OkHttpUtils.kt` blob `d918f0efbee7bd36394e1218ed608a5ffaa99fd6`; `jsHelp.md` blob `17f51c144ccfeb33bb228df29f0e5600206a1a98`.
  - BookSource caller evidence: `BookList.kt` `c542264ebe0fbaba8bbd47fb939f51cd67dd6df1`; `BookInfo.kt` `e8c062baaccbe7a27b8ea9b9507b6fad036b5ff1`; `BookChapterList.kt` `dd2ae3950cee55a9c8e7880dc2c034d6f4c6f33d`; `BookContent.kt` `cbc353920315db9810f57aec935faeaca5310a13`; upstream JSoup compatibility test `SourceCompatibilityTest.kt` `d49e8001d3058e7fe6c8a9f619d0e75539b6e4b5`; network option test `AnalyzeUrlNetworkOptionsTest.kt` `d507381d8f7d163af491ff5079ae9dcd017a0ecc`.
  - Badcat host identities at this snapshot: `RhinoSafeSourceJavaExtensions.kt` `6a00e649895c4c1d223b768c6b288632ec033de9`; `RhinoSourceScriptRuntime.kt` `879d6ed978894b16271cf5207348176fd040eda7`; `LegadoSourceElement.kt` `768cd47e61cff1f4778c757126134749af191e7b`; `LegadoUrlResolver.kt` `7c70fe697d6ed1ce68967cdf70f22d1db5182599`; `SourceHttpGateway.kt` `8dde6479650379b280d7d237b25366654d69210b`; `SourceRequestFactory.kt` `cd70c32bc933a6716692e6043d9634dbcbf10e7e`; `OkHttpSourceRequestClient.kt` `25de6e42eec8146629060a6c1a142fa0befeacb2`; Android wiring `BadcatReadingEngineFactory.kt` `cd8946aaaffacef3d2a748a537736cf2798c96c8`; host resolver tests `LegadoUrlResolverTest.kt` `fbb5d6f9e21611f61ca8455ba673d57dd26f1236`; host runtime tests `RhinoSourceScriptRuntimeTest.kt` `803831e73835409155629a3255776cc3c808a8cf`.

### AnalyzeRule: documented source API, real BookSource callers, and host differences

- **Execution context:** pinned `AnalyzeRule.evalJS` binds `java = this` (the active AnalyzeRule instance), alongside source/book/content/baseUrl and cookie/cache context. `jsHelp.md` documents `getString`, `getStringList`, `setContent`, `getElement` and `getElements` for source JS. This is not the same object as Badcat's generic source Java bridge.
- **Core BookSource callers found:** `BookList` initializes content/base URL/redirect URL, uses `getElements` for result rows and `getStringList` for category values; `BookInfo` initializes the same parser context, can replace parser content with `getElement`, and uses `getStringList(..., isUrl=true)` for download links; `BookChapterList` uses `getElements` for TOC rows and `getStringList(..., isUrl=true)` for next-TOC links; `BookContent` uses `setContent`/redirect URL and `getStringList(..., isUrl=true)` for next-content URLs. These are production BookSource Search/Detail/TOC/Content call paths, not hypothetical JS API names.
- **Upstream method semantics:**
  - `setContent(Any?, String?) : AnalyzeRule` rejects null with `AssertionError`, stores the original content object, detects JSON vs Jsoup Node, updates base URL and invalidates XPath/Jsoup/JSON parser caches, then returns the same AnalyzeRule for chaining.
  - `setBaseUrl(String?) : AnalyzeRule` updates only for non-null values and returns the same object. `setRedirectUrl(String) : URL?` retains the prior redirect for data URLs or malformed URL input (logging the parse error); a valid URL becomes the base for relative URL resolution.
  - `getStringList(String?, Any?, Boolean) : List<String>?` can return null when the rule/result is absent; with `isUrl=true`, it resolves each extracted output item against `redirectUrl`, removes empty/duplicate absolute URLs and returns the URL list. `getString(String?, Any?, Boolean)` similarly treats `isUrl` as output URL resolution (blank output falls back to `baseUrl`), not as an instruction to fetch the supplied `mContent` URL.
  - `getElement(String) : Any?` may return null and otherwise returns the parser/JS result object; `getElements(String) : List<Any>` returns a non-null list and filters null/`Scriptable.NOT_FOUND` values. The actual JavaScript element objects retain the pinned Jsoup compatibility surface.
- **Host behavior is not signature/semantic parity:**
  - Host `setContent(Any?, String?)` stringifies content (null becomes empty text), returns `String`, and only writes content plus an optional base URL. It does not return the active Java bridge/AnalyzeRule object, preserve arbitrary content-object identity, expose `setBaseUrl` or `setRedirectUrl`, or reproduce the upstream redirect-base lifecycle. The host's fresh selector-document construction may avoid stale parser instances in its own path, but does not prove upstream parser-object semantics.
  - Host `getStringList` returns `Array<String>` with empty-array fallback, rather than nullable `List<String>`; its declared overloads differ from the upstream `@JvmOverloads` surface. Host `getElement` returns a custom `LegadoSourceElement` and substitutes an empty wrapper for null; host `getElements` returns `LegadoSourceElementList` containing custom wrappers rather than upstream Jsoup-backed objects.
  - A concrete argument-semantics conflict is present: host three-argument `getString(rule, content, isUrl=true)` / `getStringList(...)` treats `content` as a URL to fetch before parsing. Upstream treats `mContent` as the content to parse and `isUrl` as “resolve the extracted result as an absolute URL against redirectUrl”. Host test `RhinoSourceScriptRuntimeTest.kt` lines 2015–2021 explicitly asserts the fetch interpretation. This is a confirmed behavior mismatch, not merely an untested possibility.
  - Host element wrappers implement a useful subset (per-element text/html/attr, selection, parent/children and sibling navigation). They are still not the upstream object type/surface: the pinned upstream `SourceCompatibilityTest.jsoupElementsKeepLegacyAttributeAccessFromJavaBindings` explicitly calls `elements.attr()`, `elements.text()`, `elements.html()`, reads `elements.length`, accesses `elements[0]`, and assigns `elements[0] = 'replacement'`; the host list wrapper only implements Java `AbstractList` access plus `first/last/getLength`, and the element wrapper's API is not a drop-in Jsoup `Elements`/Element object.
- **Classification:** selector execution exists in Badcat but the public Java/JS contract remains partial. The missing fluent identity, nullability, overload/collection behavior, URL-result resolution, redirect context and Jsoup object compatibility block claiming AnalyzeRule parity. Existing host tests exercise selected selectors and wrapper chains; they do not establish the pinned differential contract.

### AnalyzeUrl.UrlOption: complete field inventory and BookSource transport mapping

- **Pinned option model fields:** `method`, `charset`, `headers`, `body`, `origin`, `retry`, `type`, `webView`, `webJs`, `timeout`, `followRedirects`, `dnsIp` (JSON alias `resolveIp`), `js`, `bodyJs`, `serverID`, and `webViewDelayTime`. Upstream parses strict JSON first, then a permissive Gson fallback, and applies the parsed fields to the live AnalyzeUrl request state.
- **Fields partially mapped by Badcat:** method, headers, body, timeout, followRedirects, retry and charset are parsed by `LegadoUrlResolver`, copied by `SourceRequestFactory` into `SourceHttpRequest`, and consumed by `OkHttpSourceRequestClient`. Query/form charset encoding is handled by `LegadoUrlParameterEncoder`; response charset is carried to response decoding. Host tests cover representative POST/header/body/timeout/redirect/retry cases.
- **Important semantic deltas in the mapped fields:**
  - **method:** pinned AnalyzeUrl maps only `POST` and `HEAD` specially; other values become `GET`. Host resolver accepts `GET/HEAD/POST/PUT/PATCH/PROPPATCH/REPORT`, so values such as `PUT` are executed rather than falling back to upstream GET.
  - **headers/body:** upstream header map values are converted with `toString()`; host parser only accepts an object or JSON-object string with scalar values. Upstream blank body becomes null; host's JSON parser can preserve an empty string as an empty body. These need differential cases.
  - **charset:** normal named charsets are mapped for query/form encoding and response decoding, but upstream `charset="escape"` has explicit escape encoding behavior; host resolver currently rejects it as unsupported.
  - **timeout:** upstream parses positive integral numeric/string values up to `Int.MAX_VALUE`, sets the request read timeout and derives a separate call timeout (at least 60 seconds or twice the read timeout unless separately set). Host passes one value to the transport, which sets connect/read/write/call timeouts all to that same value. Same numeric input therefore does not imply same timeout semantics.
  - **followRedirects:** the boolean parser is substantially aligned, but upstream sets both `followRedirects` and `followSslRedirects` to the option value. Host deliberately keeps `followSslRedirects(false)` even when ordinary redirects are enabled. This is a security-policy delta and must remain explicit rather than be described as exact parity.
  - **retry:** upstream retries any unsuccessful non-redirect HTTP response until the configured count is exhausted (negative values are clamped to zero); host restricts retry count to 0–5 and retries only idempotent GET/HEAD for selected transient HTTP codes or selected I/O errors. The retry behavior is not equivalent.
- **Explicitly unsupported request options:** upstream `dnsIp/resolveIp` scopes DNS resolution to the target host (and rejects use with a proxy); host resolver rejects both names. Upstream `type` changes text-response behavior to a hex representation of fetched bytes; host rejects it. Upstream `webView`, `webJs` and `webViewDelayTime` use BackstageWebView/JavaScript extraction and delay; host fails closed for `webView=true` or a `webJs` field and has no WebView delay implementation.
- **Script stages are only partially wired:** upstream `js` runs inside AnalyzeUrl's own JS binding where `java = this` (AnalyzeUrl) and can mutate/reconstruct the live request; upstream `bodyJs` transforms the non-WebView response body. Badcat's `SourceHttpGateway` has separate bounded `js`/ `bodyJs` stages, but its evaluator is the generic source-script runtime rather than an AnalyzeUrl object. More importantly, production `java.ajax` wiring in `BadcatReadingEngineFactory` directly calls resolver → request factory → HTTP client and returns response.body; that path does not execute the resolver's `js` or `bodyJs` stages. Thus the gateway's support must not be generalized to every BookSource request path.
- **UrlOption fields outside the BookSource HTTP request path:** `origin` is consumed by URL-to-bookshelf source-selection flows (`AddToBookshelfDialog`/`BookshelfViewModel`), and `serverID` is consumed by `WebDav.fromPath` to select a WebDAV server. They are part of the upstream UrlOption model but are not request transport fields in the BookSource Search/Detail/TOC/Content path; Badcat's URI resolver does not implement these separate product flows. They must be tracked in their owning product/runtime scope rather than silently counted as supported here.
- **Evidence boundaries:** upstream `OkHttpUtils.newCallResponse` retries all non-success non-3xx HTTP responses and does not retry an IOException in that loop; host OkHttp transport has a narrower GET/HEAD transient retry policy. Upstream `AnalyzeUrlNetworkOptionsTest` and host `LegadoUrlResolverTest` are source evidence only; no tests were executed during this audit.
- **Status:** this closes the source-level field inventory and the principal BookSource caller/transport mapping for this slice, but confirms material compatibility gaps. It does not close arbitrary third-party script behavior, all AnalyzeRule/AnalyzeUrl ABI metadata, every dynamic caller/reflection path, serializer differentials or the complete dependency graph. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**. No production source, tests, Gradle, workflows, database/schema, UI/Reader or source fixtures changed.
