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
