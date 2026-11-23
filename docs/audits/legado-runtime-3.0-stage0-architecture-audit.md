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
