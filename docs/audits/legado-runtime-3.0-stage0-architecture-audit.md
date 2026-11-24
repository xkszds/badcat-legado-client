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

## 24.20 BaseSource identity and batch-content JS lifecycle — concrete caller closure (2026-11-24)

- **Inspection snapshot:** Badcat branch `integration/legado-upstream-runtime` at `9c1ebe8c4b387fdda8f12f5a7fb8669465e910dc`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. This closes a source/caller audit slice only; it is not a batch-runtime implementation or device test.
- **Exact upstream source identities:**
  - `JsExtensions.kt` `f6808edd2a0d312e8bd13649935dea444cb82581`; `BaseSource.kt` `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa`; `AnalyzeRule.kt` `7591a026a072ac8a6fe4902add0795812fe0a0a6`.
  - `BatchContentContext.kt` `68cad877f048b171e4f0f6cf22b1e0dd1e6481f9`; `BookContent.kt` `cbc353920315db9810f57aec935faeaca5310a13`; `BookSource.kt` `7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c`; `BookChapter.kt` `9798267c77c9e14d0a673a822930491cd6e44cc1`.
  - `WebBook.kt` `d7a924c2d687bc99d6e25099cefacc9d920e1386`; `JsSourceEngine.kt` `44452025f81d8751afee92d3ee1f2d52f9e55905`; `JsSourceBook.kt` `66d9878bed8671fe130d9c9c507608d89d393cf5`; `JsSourceConfig.kt` `871165021c2192b7d79ca2768bfdc6b4cfd35740`; `jsHelp.md` `17f51c144ccfeb33bb228df29f0e5600206a1a98`.
  - Caller and test identities: `ReadBook.kt` `2e83d46de8c4351580f0f65e13fe01310d99f3ee`; `CacheBook.kt` `f1c24dc905664b592a04daccc4e09ae6e8578409`; `BatchContentDownloadTest.kt` `81e4debe9194d3a9c6590be29c4fdcc5565349f7`.
- **Exact Badcat host identities:** `RhinoSafeSourceJavaExtensions.kt` `6a00e649895c4c1d223b768c6b288632ec033de9`; `RhinoSourceScriptRuntime.kt` `879d6ed978894b16271cf5207348176fd040eda7`; `SourceChapterContentService.kt` `8e2da87c300784fe4a0ca96e161c9597590e6e0f`; `SourceReadingEngine.kt` `70f1e0036a17b6d582744d612564842831b623f2`; host model `ReadingEngine.kt` `4777ceae6af13633c2561c7fc77c4cbde3a87480`; `RhinoSourceScriptRuntimeTest.kt` `803831e73835409155629a3255776cc3c808a8cf`; `SourceChapterContentServiceTest.kt` `a8592ea9c645c28e25240169282c6eab146982ca`.

### Upstream identity and execution contexts

- `JsExtensions.getSource(): BaseSource?` is the source-identity contract. `BaseSource.getSource()` returns `this`; `AnalyzeRule.getSource()` and `AnalyzeUrl.getSource()` return their active source; `JsSourceEngine.getSource()` returns the actual BookSource. The base `BaseSource.evalJS` binds `java`, `source` and `sourceApi` to the same BaseSource object. `AnalyzeRule.evalJS` binds `java=this` (the AnalyzeRule instance) and `source` to the active BaseSource, plus book/chapter/content and the active batch chapters. `JsSourceEngine.buildScope` binds `java=this` (JsSourceEngine) and both `source`/`sourceApi` to the live BookSource.
- `getBatchContext()` defaults to null outside batch execution. It is overridden by AnalyzeRule to return its temporary batchContext and by JsSourceEngine to return its temporary batchContext. This is lifecycle-scoped context, not a process-global batch flag.
- `cacheContent(chapter, content)` is a JavaScript-interface method. It checks coroutine cancellation, requires an active batch context (otherwise throws), unwraps Rhino `Wrapper` values, then delegates to `BatchContentContext.saveContent`. It is intentionally unavailable during ordinary single-chapter scripts.

### Real BookSource batch callers and lifecycle

- **Eligibility:** `BookSource.supportContentBatch()` requires a ContentRule and `maxBatchSize > 1`; conventional sources additionally require nonblank `contentBatch`. JS sources use the top-level `getContentBatch(chapters, book)` function instead. `contentBatchSize()` clamps the configured batch size to the upstream maximum (50). `JsSourceConfig` rejects one-sided configuration: `config.maxBatchSize` requires a top-level `getContentBatch` function, and a function without `maxBatchSize` is also rejected.
- **Production entry points:** `ReadBook` invokes batch pre-download when the active BookSource supports it; `CacheBook` invokes batch download for manual caching. Both go through `WebBook.getContentBatchAwait`, which dispatches conventional sources to `BookContent.analyzeContentBatch` and JS sources to `JsSourceBook.getContentBatchAwait`. Unwritten chapters are returned to ordinary single-chapter fallback.
- **Conventional source path:** `BookContent.analyzeContentBatch` creates `BatchContentContext(bookSource, book, chapters, coroutineContext, per-chapter save tokens)`, installs it on one AnalyzeRule, sets the current chapter/base URL, then evaluates the contentBatch JS with `chapters` as the result. `AnalyzeRule.evalJS` exposes `chapters = batchContext?.chapters` and `java = this`. The context is closed in `finally`; the caller then calculates missing chapters.
- **JS-source path:** `JsSourceBook.getContentBatchAwait` creates the same context and calls `JsSourceEngine.callContentBatch`. That method installs the context only for the `getContentBatch` call and clears it in `finally`; the outer caller closes the context and returns unsaved chapters. The JS-source config validation guarantees that the declared max size and function are paired.
- **Chapter identity and save safety:** `BatchContentContext` owns the BookSource, Book, chapter list, coroutine context and per-chapter content-save tokens. Chapter objects are resolved by `BookChapter.index`; URL strings are accepted only when exactly one chapter in the batch matches the raw/absolute/normalized URL. Duplicate URLs require passing the chapter object; numeric array indexes are deliberately rejected. It applies the source's `replaceRegex` before saving through `BookHelp.saveContent`, rejects blank transformed output, checks cancellation and the captured save token, serializes mutation under a lock, and rejects writes after close. A successful save records the chapter index; the remainder goes to single-chapter fallback.
- **Source-help and tests:** `jsHelp.md` explicitly limits `java.cacheContent` to the contentBatch rule, documents the `chapters`/`result` variables, object-vs-unique-URL identity and fallback semantics. Upstream `BatchContentDownloadTest` covers bare JS, `<js>`, `@js:`, consecutive blocks, replacement/blank rejection, stale writes for both conventional and JS sources, cancellation, partial-script failure and post-close callback rejection. This confirms the feature is an active reader/manual-cache workflow, not a dormant API.

### Badcat host comparison

- **Source object identity is not preserved:** the production host runtime binds `java` to `RhinoSafeSourceJavaExtensions`, `source` to a separate `RhinoSourceLoginExtensions` session bridge, and `cookie`/`cache` to separate objects. `RhinoSafeSourceJavaExtensions.getSource()` returns `Map<String, Any?>` copied from `sourceDescriptor`; the descriptor is materialized from imported raw JSON and identity/enabled overlays. It is not the live upstream BaseSource/BookSource object, has no BaseSource methods/lifecycle, and does not preserve upstream object identity. The host test `getSourceExposesNestedRulesAndUnknownBookSourceMetadata` proves JSON metadata projection only, not entity identity or mutability.
- **Batch context is absent:** host `getBatchContext(): Any?` always returns null; `cacheContent(chapter, content)` always throws `IllegalStateException`. The host bridge constructor has no batch-context provider/owner and no callback that writes a chapter by the upstream BookChapter/index identity. This reproduces fail-closed behavior outside a batch but cannot succeed inside a valid batch.
- **No production batch entry point:** host `SourceReadingEngine` exposes per-chapter `chapterContent`; `SourceChapterContentService.load(source, bookUrl, chapter, bookName)` accepts exactly one chapter and performs ordinary pagination, replacement and cache publication. Its `ChapterContentCache.beginSave/put` generation fence is useful single-chapter cache safety, but it is not a multi-chapter `BatchContentContext`, does not run `contentBatch` or `getContentBatch`, does not install `chapters`/batch context, and cannot receive `java.cacheContent` callbacks. The host BookSource model retains `ruleContentJson`/raw JSON but has no typed `contentBatch`/ `maxBatchSize` eligibility and dispatch path; retaining these fields in `java.getSource()` does not execute them.
- **Host test boundary:** the current host Rhino runtime test has a `getSource()` map-projection test but zero `cacheContent` or `getBatchContext` test references. Host SourceChapterContentService tests cover one-chapter pagination/cache/save fencing; they contain no batch-content or `cacheContent` cases. These are source inspections, not executed test results.
- **Classification:** BaseSource/BookSource identity and batch-content lifecycle are **blocking compatibility gaps**. A separate Map/session bridge plus a single-chapter cache cannot be treated as an equivalent BaseSource or as batch-content support. The host's explicit exception is safer than silently dropping writes, but it is not the upstream success path.

- **Status:** this closes the registered source/caller audit slice for source identity and the batch-content lifecycle. It does not close arbitrary third-party script corpus behavior, compiled ABI/nullability, serializer differentials, all remaining JsExtensions callers/reflection/dynamic dispatch or the complete dependency graph. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**. No production source, tests, Gradle, workflow, DB/schema, UI/Reader or source fixtures changed; no tests were executed during this audit.

## 24.21 Remaining JsExtensions/source-help callers — file, cookie/cache and source-UI API slice (2026-11-24)

- **Inspection snapshot:** Badcat branch `integration/legado-upstream-runtime` at `6904940af32e0b65c3691fe3ccc2315502879906`; fixed upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. This is a partial Stage-0 source/API caller audit. It does not close task §26.13 or authorize Stage-1.
- **Pinned source identities:** `JsExtensions.kt` blob `f6808edd2a0d312e8bd13649935dea444cb82581`; `JsEncodeUtils.kt` `c75b61650db2abdd64a09d2bd8340ef14c2947b3`; `BaseSource.kt` `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa`; `NativeBaseSource.kt` `8313dedf737fe42cb4381535f6e74760c4f31786`; `RhinoWrapFactory.kt` `4a3d8c30329664e725cb64ae89a88a5dfe4ebc6a`; `App.kt` `71b83735b9a41fe0a459fee3a2175fa7518215b8`; `jsHelp.md` `17f51c144ccfeb33bb228df29f0e5600206a1a98`; `CookieStore.kt` `09720d79f7009db052c080b54fc5fa2805257b64`; `CacheManager.kt` `9575697d8ed60c0864aa3fd52d548ba059054205`; `SourceNavigationUiTest.kt` `1debac6fec8b1e19b38b55c5ae02cd6bb9e129e2`; `ruleHelp.md` `e10d2e29e2b22614554a0fd700905eea987a8bf1`.
- **Badcat source identities:** `RhinoSafeSourceJavaExtensions.kt` blob `6a00e649895c4c1d223b768c6b288632ec033de9`; `RhinoSourceScriptRuntime.kt` `879d6ed978894b16271cf5207348176fd040eda7`; `RhinoSourceCookieExtensions.kt` `c299469c6fb3a70929ac06c7a09c49773d7fc42d`; `RhinoSourceCacheExtensions.kt` `b014c4f03f4161a49f1037993267934cfbcb2a5e`; `SourceScriptFileCacheStore.kt` `e2e6ca097ca837926851aae09b81291db28ac583`; `SharedPreferencesSourceCookieJar.kt` `2f389d365128529813020b2bd81e242298c298a5`; `DefaultProcessCookieBridge.kt` `cb4eeb5f004a5a7607ba5afb69a577ace865a7e4`; `LegadoSourceLoginUiV2Service.kt` `853db957d4470b67e307af668ff1bf410a3c4ef2`; host file/cache tests `RhinoSafeSourceJavaExtensionsArchiveTest.kt` `4ba2356f9267ca92301befa7c711a98ddfa12734` and `RhinoSafeSourceJavaExtensionsCompatTest.kt` `4786120ea30c7b51481ea193948fdcbd6df76869`.

### Java object scope and dynamic reachability

- Pinned `BaseSource.evalJS` binds `java`, `source` and `sourceApi` to the same live BaseSource instance (lines 414–420); BookSource itself implements `JsExtensions` and `getSource()` returns `this` (BaseSource lines 93–101). `App.initRhino` registers BookSource to `NativeBaseSource.factory` (App lines 242–252). That wrapper hides JavaBean `setX` methods when the matching property exists, with a special case for `setVariable` (NativeBaseSource lines 8–43); `RhinoWrapFactory.wrapAsJavaObject` chooses an exact-class registered factory before its generic Java-object wrapper (RhinoWrapFactory lines 51–120). Therefore the effective source API includes object wrapping/property behavior and is not defined only by the 48 `@JavascriptInterface` annotations.
- The pinned source help explicitly documents these functions as dynamic JS calls. Static Kotlin search cannot enumerate arbitrary third-party source scripts. The pinned Android `SourceNavigationUiTest` demonstrates live BookSource JS invoking `java.openUrl`, `java.openVideoPlayer` and `java.startBrowserAwait` (lines 63–106); `ruleHelp.md` uses `getVerificationCode()` in a login UI button action (lines 69–118). These are reachable source-script operations, not safely classifiable as unused merely because most callers are script text.
- This slice does not repeat the previously closed WebJsExtensions dispatch, RssJsExtensions/SourceLogin, preUpdateJs, loginCheckJs, AnalyzeRule/UrlOption or batch lifecycle evidence in §§24.15–24.20.

### File, script-import and archive helpers

- **Pinned contract:** `jsHelp.md` lines 275–299 documents `java.importScript` and `java.cacheFile(url[, saveTime])`; lines 354–370 documents `downloadFile`, `unArchiveFile/unzipFile/unrarFile/un7zFile`, `getTxtInFolder`, `readTxtFile` and `deleteFile`. `JsExtensions.importScript` dispatches HTTP(S)-looking paths to `cacheFile`, local paths to `readTxtFile`, and throws on blank content (lines 451–462). `cacheFile` hashes the URL into a CacheManager key, reuses a still-existing cached path or calls `downloadFile`, then returns decoded text (lines 464–493). `downloadFile` constructs `AnalyzeUrl(url, source=getSource(), coroutineContext=context)`, obtains its input stream and writes a source-context-aware download to the app cache (lines 512–540). Local file APIs resolve under the app external-cache tree and enforce a descendant check (lines 802–850); archive-named helpers converge on `ArchiveUtils.deCompress` (lines 853–895).
- **Host implementation present, but not identical:** the bridge has `downloadFile`, legacy hex overload, `readTxtFile`, `readFile`, `getFile`, `deleteFile`, archive extraction and `cacheFile/importScript` (host lines 301–390, 460–465, 586–761 and 1277–1307). Host downloads use an injected `httpGet` callback and retain original response bytes; paths are source-owned opaque tokens and an optional source-private physical root, not the upstream shared external-cache relative-path contract. With no physical root, arbitrary local cache paths cannot be read; only known download tokens/virtual archive paths resolve. With a physical root, canonical path confinement is enforced. Host archive extraction imposes explicit compressed/entry/total limits and uses ZIP plus a libarchive fallback, rather than calling the upstream `ArchiveUtils.deCompress` implementation. These are deliberate host/security choices but remain behavior differences until differential fixtures prove equivalence for the supported source corpus.
- **Cache identity differs:** upstream `cacheFile` keys CacheManager by MD5(URL) and checks the referenced file exists. Host `cacheFile` stores a source-namespaced `cacheFile_<md5>` deadline/path record and checks its source-owned download bytes/path before reuse (host lines 1272–1301). The host test verifies repeated URL reuse and `importScript` reuse (ArchiveTest lines 192–213), and additional tests cover invalidation after physical-file deletion and archive extraction. These prove host-local behavior, not upstream cache identity/path parity.
- **Current classification:** `downloadFile`, `cacheFile`, `importScript`, text-file access and archive methods are **partial**. They have real host behavior and focused host tests, but path identity, source cache namespace, transport path, archive implementation/limits and local-file availability differ from the pinned contract. Do not count a same-name method or host-only unit test as full source compatibility.

### Cookie and cache namespaces

- **Pinned contract:** `JsExtensions.getCookie(tag[, key])` delegates to `CookieStore.getCookie/getKey` (JsExtensions lines 495–510); the cookie object is a separate JS binding documented in `jsHelp.md` lines 606–620. Pinned CookieStore resolves a URL to its domain bucket, merges persistent and session cookies, caps the combined header at 4096 characters, supports replace/remove and imports WebView cookies (CookieStore lines 20–115). The separate `cache` binding is documented at `jsHelp.md` lines 622–641; importantly, `cache.get(key, onlyDisk=false)` is a two-argument contract, and the cache includes persistent, file and process-memory operations. CacheManager declares the `get(key, onlyDisk)` overload and file/memory helpers (CacheManager lines 60–160 and 172–204).
- **Host binding:** `RhinoSourceScriptRuntime` installs separate `java`, `cookie`, `source` and `cache` objects (runtime lines 285–337 and 457–476). The `cookie` object implements get/getKey/set/replace/remove but has no `setWebCookie` method (cookie extension lines 7–39), even though the pinned help documents it. Its write/remove callbacks default to throwing unless explicitly supplied; the runtime constructor's default cookie reader is empty and writer/replacer callbacks throw (runtime lines 175–207). A separate SharedPreferences domain cookie jar and DefaultProcessCookieBridge can persist and synchronize cookies (jar lines 84–155; bridge lines 129–153), but their existence does not prove that the JS cookie object is wired to those operations. `LegadoSourceLoginUiV2Service` itself explicitly states that its current runtime does not emulate the full upstream CookieStore/WebView surface (service lines 10–15).
- Host `java.getCookie(tag[,key])` calls the injected cookieReader and parses a key from the returned header (bridge lines 1313–1326); the upstream delegates to CookieStore.getKey, which separately merges the session-cookie view. Until the exact production callback wiring and session merge path are proven, classify this as **partial/unverified**, not parity. Host `cache.get(key)` has no `onlyDisk` overload (cache extension lines 36–57); its put/get/file/memory methods use host-specific stores and TTL metadata (lines 13–115). The documented two-argument cache read is therefore a **confirmed API gap**.

### Source UI, verification and settings callbacks

- Pinned `getVerificationCode(imageUrl)` suspends through `SourceVerificationHelp.getVerificationResult` and returns the verified response body (JsExtensions lines 440–449). `startBrowser/startBrowserAwait` run the source-verification flow and may refetch through AnalyzeUrl (lines 394–440). `openUrl` checks source-navigation permission, handles Legado import schemes specially and launches a source-attributed confirmation activity (lines 1238–1265); `openVideoPlayer` delegates to SourceHelp and `showBrowser` launches a source-attributed BottomWebViewDialog (lines 378–399 and 1268–1297). The help documents the verification/browser/open-url operations (jsHelp lines 251–258 and 644–658), and the instrumentation test proves the navigation methods are callable in BookSource scripts.
- The Badcat Java bridge has no methods named `getVerificationCode`, `startBrowser`, `startBrowserAwait`, `openUrl`, `openVideoPlayer` or `showBrowser`. The inspected `RhinoSourceScriptRuntime` constructor has no verification/browser/navigation/player Host Port; the Login UI v2 service explicitly excludes WebView. These remain **blocking source API/lifecycle gaps**. This is not permission to import Legado's product UI; the required behavior belongs behind typed host ports with explicit source identity, invocation context, cancellation and callback/result semantics.
- Pinned `getReadBookConfig`, `getReadBookConfigMap`, `getThemeConfig`, `getThemeConfigMap` and `getThemeMode` are public source-help methods (JsExtensions lines 1356–1386; help lines 260–274). No same-name methods exist on the inspected Badcat Java bridge. They remain unimplemented source-visible APIs unless later evidence proves a narrower source-script exclusion; the current task has not established such an exclusion.

### Diagnostics and remaining method families

- Host `toast/longToast`, `androidId`, `randomUUID`, `getWebViewUA`, `htmlFormat`, encoding and crypto helper families have same-name host declarations. Same-name existence is not parity: for example upstream `log` emits source-attributed Debug/AppLog events and returns its input (JsExtensions lines 1202–1211), while host `log` currently returns a String projection (bridge line 1328); upstream `logType` logs the type and returns Unit (lines 1214–1223), while host `logType` returns the class-name String (bridge line 1162). Upstream `getWebViewUA` returns the System WebView user agent; host returns the injected `webViewUserAgent` value (runtime lines 197–200 and bridge line 1311), so parity depends on the host provider actually supplying the active WebView UA.
- `singleFlight/lock/tick` have host counterparts (bridge lines 1082–1109), but this slice does not close their full callback `this`/scope/timeout/exception parity. Crypto helper object methods, JsEncodeUtils overloads, TTF/font replacement, JsURL property surface, byte/string/URI conversion and all remaining public overloads are still open for method-level comparison.
- **Test evidence boundary:** inspected host file/archive tests and navigation test source; no test command was run in this documentation-only audit slice. No production or test file was modified.
- **Status:** this closes only a partial documentation/caller/host comparison for file/cache/cookie/source-UI helpers. Task §26.13 remains **in_progress**. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**. No complete JsExtensions/JsEncodeUtils ABI or source-script compatibility claim is made.
- **Next ordered action:** finish the remaining method-by-method source-help matrix: inherited JsEncodeUtils crypto overloads/helper objects; TTF/font and archive-byte overloads; JsURL/URI/byte conversion; all callback and settings methods; cookie/cache overloads and dynamic dispatch/reflection reachability. Then re-read exact source and host blobs, classify every remaining method and update the task status only if all acceptance conditions are evidenced.

## 24.22 Remaining JsExtensions/JsEncodeUtils — crypto, font, archive-byte and JsURL surface (2026-11-24)

- **Inspection snapshot:** Badcat branch HEAD `4243e36f307551c344cfb7bb818db4daf1d40b65`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. Continues task §26.13; this is source-level comparison only, not a complete ABI acceptance.
- **Pinned source identities:** `JsEncodeUtils.kt` `c75b61650db2abdd64a09d2bd8340ef14c2947b3`; `SymmetricCryptoAndroid.kt` `c98eb68d4f067edc5221c800cfb8701fd8a1f77b`; `AsymmetricCrypto.kt` `2d9724f8ccdc0c480c2556fb0b7129d7cda2d5c8`; `Sign.kt` `8c671d920acf1275c4a1049dd4184a7cf44748ce`; `QueryTTF.java` `c5b9695c7ee72a8b53f8501dd723f053f48e5cbd`; `JsURL.kt` `7596765e8e2727d1834cfee86938e58ac1728bdd`; `ArchiveUtils.kt` `51672517425ae5606ec84b739aee7e2847553290`; `LibArchiveUtils.kt` remains in the pinned dependency/source closure.
- **Badcat source identities:** Java bridge `RhinoSafeSourceJavaExtensions.kt` `6a00e649895c4c1d223b768c6b288632ec033de9`; bounded symmetric wrapper `RhinoBoundedSymmetricCryptoAndroid.kt` `978131cbea54325271c20c7d8c50a53d7fbe9c73`; host `SymmetricCryptoAndroid.kt` `6770d94f9e9dad2cb072b878fa443831e8a87020`; host `AsymmetricCrypto.kt` `72e0c07ab089f3c08b5183d537548edfea77507f`; host `Sign.kt` `2180ed91a6948ed6b2cd0cba260828edf245ee85`; bounds `RhinoCryptoBounds.kt` `f840ffaeeafe744f6456db68a98f78cc683c3ef5`; archive reader `SourceLibArchiveReader.kt` `5fd007b2c1d9a353839dedda864f3671b6c0aaf1`; crypto tests `RhinoCryptoCompatibilityTest.kt` `b77557a1ed86dc6419f047b467a453dbb7c7d2a2`; font tests `RhinoSafeSourceJavaExtensionsTtfTest.kt` `ed196b968be19b2723e13c2cb67b4b640a98957a`.

### JsEncodeUtils inherited surface and crypto objects

- The pinned `JsExtensions` inherits `JsEncodeUtils`; its helper docs explicitly show the returned objects being used as `java.createSymmetricCrypto(...).decrypt/encrypt/encryptBase64/encryptHex` (JsEncodeUtils lines 32–91). The pinned public factory overloads are `createSymmetricCrypto(String, ByteArray?, ByteArray?)`, `(String, ByteArray)`, `(String, String)`, `(String, String, String?)`, plus `createAsymmetricCrypto(String)` and `createSign(String)`. The old AES/DES/3DES helpers, digest and HMAC methods remain public; the annotated surface is a subset, while the remaining public methods are still visible through the upstream Rhino Java-object wrapper. The host bridge reproduces these named factory and legacy helper overloads directly (host lines 1330–1788); its crypto compatibility tests call them through actual Rhino source scripts.
- **Object implementations:** upstream factories return Hutool-backed `SymmetricCryptoAndroid`, `AsymmetricCrypto` and `Sign` objects. Badcat returns the same public package/type names for the latter two and a `SymmetricCrypto` whose concrete instance is `RhinoBoundedSymmetricCryptoAndroid`. Host wrappers add 2 MiB input/output and 4 MiB encoded-output limits, bounded stream reads, and key/IV limits (bounds lines 6–33; wrappers above). This is an intentional resource boundary but is not identical behavior for inputs above those thresholds. Host digest algorithms are allow-listed to MD5/SHA-1/SHA-256/SHA-384/SHA-512 and HMAC SHA-1/SHA-256/SHA-384/SHA-512 (bridge lines 1332–1348 and 1939–1959); the upstream delegates to Hutool `DigestUtil`/`HMac` without this same explicit allow-list. Algorithm coverage therefore remains a parity gate.
- **Historical quirks preserved in tested paths:** host test `supportsPinnedLegadoAesLegacyHelpersAndHistoricalQuirks` explicitly verifies the upstream `aesEncodeToString` behavior that calls `decryptStr(data)`; DES/3DES, RSA/signature factories, symmetric overloads and random-key semantics also have source-script tests (crypto test lines 25–180). The host tests establish selected common-case behavior, not all inherited Hutool public methods/overloads or exact compiled JVM ABI. No test was run in this audit.

### QueryTTF and font replacement

- The upstream and Badcat `QueryTTF.java` files have the **same exact Git blob SHA** `c5b9695c7ee72a8b53f8501dd723f053f48e5cbd`; the parser/model implementation is therefore byte-identical at this pin.
- Pinned `queryTTF(data, useCache)` accepts `String` URL/base64 and `ByteArray`; URL reads go through `AnalyzeUrl(data, source=getSource(), coroutineContext=context).getByteArray()`; cache keys hash the original input string or byte array and use `AppCacheManager` (JsExtensions lines 1062–1109). Deprecated `queryBase64TTF` logs a migration message then delegates (lines 1048–1055). Pinned `replaceFont` maps source glyph outlines to Unicode and optionally filters missing glyphs (lines 1111–1160).
- Host `queryTTF` accepts ByteArray, HTTP(S), local host file token or base64; it uses injected `httpGet`, an 8 MiB bound and a source-ID-plus-content-hash cache key (bridge lines 108–156). It does not use upstream `AnalyzeUrl`/AppCacheManager or the same cross-source cache identity. Host `queryBase64TTF` simply delegates without the upstream deprecation log. Host `replaceFont` uses code-point iteration and mirrors the glyph mapping/filter intent (lines 158–191). Tests cover parsing, per-source cache isolation, source-transport fetching/replacement and size rejection (font test lines 16–87). Classify parser **exact**, helper acquisition/cache **partial**, replacement **source-level similar but not yet differential-tested**.

### Archive-byte helpers

- Pinned `getZipByteArrayContent` fetches absolute URLs via `AnalyzeUrl` and otherwise hex-decodes; it scans ZIP entries and logs when the target is absent (JsExtensions lines 978–1004). RAR/7z helpers also use `AnalyzeUrl` or hex input and call pinned `LibArchiveUtils.getByteArrayContent` (lines 1006–1040). The string helpers expose auto-detected and explicit-charset overloads (lines 928–976). The source help documents `java.get*StringContent(url,path[,charsetName])` and `java.get*ByteArrayContent(url,path)` (jsHelp lines 292–300).
- Host ZIP helper accepts HTTP(S) or hex, fetches through injected `httpGet`, bounds compressed archive to 8 MiB and expanded entry to 16 MiB, and returns null if no matching regular entry (bridge lines 631–669). RAR/7z use `SourceLibArchiveReader` (lines 672–721), which bounds header count, entries and expanded bytes and frees the native archive handle. String overloads exist with charset auto-detection and explicit charset (lines 680–731). The host uses a separate source reader and transport path rather than pinned AnalyzeUrl/LibArchiveUtils; host-specific resource caps also change behavior. These APIs are **partial** until URL-option/cookie/redirect/error and archive edge-case fixtures are differentially checked.

### JsURL and chapter-number conversion

- Pinned `java.toURL(url, baseUrl?)` returns `io.legado.app.utils.JsURL`, a Kotlin object with public `host`, `origin`, `pathname` and `searchParams` properties (JsURL blob above). Query values are URL-decoded and repeated keys collapse into a map.
- Host returns `RhinoLegadoUrl` (bridge lines 1800–1804, class lines 2093–2123) with JavaBean getters `getHost/getOrigin/getPathname/getSearchParams` plus legacy `getPath/getProtocol/getQuery` aliases. It parses with `java.net.URL` and exposes a getter-based class rather than the pinned Kotlin property class. Correction: host test `RhinoSourceScriptRuntimeTest.supportsLegadoJsUrlPropertiesAndRelativeResolution` (blob `803831e73835409155629a3255776cc3c808a8cf`, lines 777–798) evaluates `u.host`, `u.origin`, `u.pathname`, `u.searchParams.get(...)`, relative resolution with a base URL, and an absolute URL with null base. Common JS property exposure is therefore verified on the host. Malformed URLs, fragments, repeated query keys, default ports, empty base, and all JsURL edge cases remain un-differenced.
- Host `toNumChapter` uses a local regex, full-width normalization and a local Chinese-number parser (bridge lines 1789–1798), while upstream delegates to `AppPattern.titleNumPattern` and `StringUtils.stringToInt` (JsExtensions lines 1164–1175). The complete numeral grammar, punctuation and unmatched-title behavior have not been differentially compared.

### Classification and remaining work

- **Exact source file reuse:** `QueryTTF.java` only. **Partial:** crypto factories/common legacy helpers (selected paths tested, but explicit size and algorithm allow-lists plus unenumerated inherited Hutool ABI); queryTTF acquisition/cache; replaceFont; archive byte/string helpers; toNumChapter. **Unverified:** JsURL JS property shape and edge cases.
- **Still open under §26.13:** complete inherited crypto/Hutool method surface and all overloads; digest/HMAC algorithm coverage; source help/corpus caller inventory for crypto/font/archive/URL helpers; JavaImporter exposure and exact return-object wrapping; toURL JS property access and URL corner cases; QueryTTF cache identity and AnalyzeUrl behavior; all byte/string/URI/base64/hex helper flags and limits; archive URL options, hex, error and limit differential; chapter numeral grammar; source-visible methods not listed in jsHelp.
- **Test evidence boundary:** inspected host crypto/font test names and source bodies; no command executed. No production/test file modified. This section is a source comparison, not a green-test claim.
- **Status:** task §26.13 remains **in_progress**; Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.
- **Next ordered action:** continue exact method inventory for all remaining `JsExtensions` declarations and help/corpus references, then close callback/URL option/JS wrapper and compiled ABI edges. Do not begin Stage-1 or add compatibility stubs.

## 24.23 Public method inventory and undocumented JS surface — remaining utility families (2026-11-24)

- **Inspection snapshot:** Badcat branch HEAD `08805c01495f7ff10b4f1b65bc273b4df222bd9a`; pinned upstream remains `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. This is a lexical declaration/help/corpus inventory plus selected utility behavior review; it is not a complete compiled ABI or full caller closure.
- **Exact file identities:** upstream `JsExtensions.kt` `f6808edd2a0d312e8bd13649935dea444cb82581`, `JsEncodeUtils.kt` `c75b61650db2abdd64a09d2bd8340ef14c2947b3`, `jsHelp.md` `17f51c144ccfeb33bb228df29f0e5600206a1a98`, `ruleHelp.md` `e10d2e29e2b22614554a0fd700905eea987a8bf1`; host `RhinoSafeSourceJavaExtensions.kt` `6a00e649895c4c1d223b768c6b288632ec033de9`, `RhinoSourceScriptRuntime.kt` `879d6ed978894b16271cf5207348176fd040eda7`.

### Inventory results and help coverage

- The CI-generated source declaration inventory artifact from run #3344 (artifact ID `14871760152`, exact HEAD `4243e36f307551c344cfb7bb818db4daf1d40b65`) reports 148 upstream base-method declarations / 104 distinct names and 150 host bridge declarations / 110 distinct names. Its declared comparison kind is a Kotlin source inventory, not a JVM bytecode descriptor proof. The separate compiled host surface artifact (artifact ID `14871665970`) reports `scope.java` with 154 declared public methods, 163 inherited public methods, 124 Rhino property IDs and 119 callable names. Earlier raw lexical counting of 184 / 138 included non-public/internal helper declarations and must not be used as the public API count. Counts differ because these reports measure source declarations, compiled public members and Rhino-visible properties/callables respectively; none alone proves upstream parity.
- Names declared by upstream but not individually named in the source-help text include `getSource`, `getTag`, `getSourceNavigationContext`, `getBatchContext`, `queryBase64TTF`, `queryTTF`, `replaceFont`, `toNumChapter`, `readFile`, and the legacy AES/DES/3DES helpers (`aesDecodeToByteArray`, `aesDecodeToString`, `aesDecodeArgsBase64Str`, `aesBase64DecodeToByteArray`, `aesBase64DecodeToString`, `aesEncodeToByteArray`, `aesEncodeToString`, `aesEncodeToBase64ByteArray`, `aesEncodeToBase64String`, `aesEncodeArgsBase64Str`, `desDecodeToString`, `desBase64DecodeToString`, `desEncodeToString`, `desEncodeToBase64String`, `tripleDESDecodeStr`, `tripleDESDecodeArgsBase64Str`, `tripleDESEncodeBase64Str`, `tripleDESEncodeArgsBase64Str`). The help does document the archive byte/string families using wildcard notation, so they are not classified as undocumented solely because their expanded method names are absent.
- The `JsEncodeUtils` source comment says new methods must update help; nevertheless, the above legacy methods remain public. Because `JsExtensions` extends `JsEncodeUtils` (JsExtensions line 125) and `RhinoWrapFactory` selects a custom wrapper for BookSource then falls back to a generic Java-object wrapper, lack of an `@JavascriptInterface` annotation or a help entry is not proof that a public method is unreachable from Rhino. Treat undocumented public methods as compatibility surface until compiled wrapper/API inventory and imported-source corpus establish otherwise.
- The four imported BookSource fixtures currently present under `app/src/androidTest/assets/legado-sources` were scanned. Only `lingling-xiaoshuo.json` contains a Java call, `java.getString`, in a BookInfo init rule. That call is an AnalyzeRule/rule-evaluation helper, not a JsExtensions method. These four fixtures do not exercise the legacy crypto/font/URI utilities and are too small to prove those APIs unused. The pinned upstream `CryptoJsCompatibilityTest` primarily verifies the bundled CryptoJS library and four script entry points; it does not exhaust the legacy `JsEncodeUtils` helper overloads.

### Remaining source helper behavior comparison

- **Source identity/context helpers:** upstream `getSource()` returns `BaseSource?`; `getTag()` returns nullable `String?`; default `getSourceNavigationContext()` returns `EmptyCoroutineContext` (JsExtensions lines 127–135). Host `getSource()` remains a JSON-friendly Map projection (covered in §24.20), `getTag()` returns non-null `sourceTag` (bridge lines 208–210), and `getSourceNavigationContext()` returns `EmptyCoroutineContext` (lines 205–206). The default navigation-context behavior matches; `getTag` nullability/fallback and the live source object identity do not yet have full differential coverage. `AnalyzeRule.getTag()` may fall back to `ruleName` when `source` is null (pinned AnalyzeRule lines 944–954), while host source bridge uses a sourceTag string.
- **Byte/string/Base64/hex helpers:** pinned `strToBytes/bytesToStr` have UTF-8 and named-charset overloads; `base64Decode` has nullable-string and charset/flags overloads; `base64DecodeToByteArray` has nullable input and flags overloads; `base64Encode` has default and flags overloads; hex helpers delegate to Hutool `HexUtil` (JsExtensions lines 654–729). Host has corresponding overloads plus explicit input-size bounds and additional byte/URI helpers (bridge lines 1352–1382 and 1817–1849). The host implementation uses Java Base64 encoders/decoders with its own flag parser and strict validation/size limits; exact Android EncoderUtils/Hutool behavior for malformed, whitespace, URL-safe, no-padding, CRLF, null and boundary inputs remains to be differential-tested.
- **Time/URI/HTML/Chinese helpers:** pinned `timeFormatUTC(time: Long, format, sh)` interprets `sh` as a raw millisecond offset; `timeFormat(Long)` uses AppConst.dateFormat; `encodeURI` uses form-style `URLEncoder` and returns empty string on unsupported charset; `htmlFormat` delegates to HtmlFormatter; `t2s/s2t` delegate to ChineseUtils (JsExtensions lines 731–793). Host has Number/String time overloads, form-style encodeURI overloads, custom HTML normalization, and separate Chinese conversion implementations (bridge lines 1128–1143, 1179–1184, 1864–1937). Same-name presence is insufficient: date-format locale/pattern, malformed charset behavior, HTML cleanup/image URL handling and Chinese conversion exception tables need differential fixtures. Host `getWebViewUA()` returns an injected string (bridge line 1311); upstream reads the active Android System WebView default UA (JsExtensions lines 791–793), so host parity depends on the actual provider and is not established by the hardcoded default value.
- **Feedback/diagnostic helpers:** pinned toast methods check active Rhino context and dispatch source-tagged UI feedback; `log` writes both source Debug and AppLog events and returns the original Any?; `logType` logs the class name and returns Unit (JsExtensions lines 1189–1223). Host toast methods call an injected presenter whose runtime default throws if unconfigured; host `log` returns a string projection without the upstream Debug/AppLog side effects; host `logType` returns the class-name String instead of logging and returning Unit (bridge lines 96–106 and 1162–1163, 1328). These are confirmed behavior/return differences, not just missing names. `androidId` likewise depends on an injected provider whose default throws, while upstream reads AppConst.androidId (JsExtensions lines 1233–1235; runtime constructor lines 197–200).
- **Concurrency helpers:** upstream `singleFlight/lock` call a JS Function with its parent scope and top-level `globalThis` as `this`, use a 15-second default timeout, and scope keys by source key plus name (JsExtensions lines 1305–1354). Host has same-name overloads and delegates through a `functionInvoker` callback and a length-delimited source key (bridge lines 1082–1109). Exact JS `this`/scope behavior, timeout, cancellation, coalescing/error propagation and tick reclamation still require runtime differential tests; do not infer parity from signatures.
- **Already-covered separate scopes:** `refreshBookInfo/refreshBookToc/refreshContent` are on the separate source/login binding and were inventoried in §24.16; `webView/webViewGetSource/webViewGetOverrideUrl` are the separate WebJsExtensions path covered in §24.15. They are absent from the Java bridge but must not be double-counted as entirely missing without examining their actual separate binding.
- **Still explicit Java bridge blockers from the prior slice:** `getVerificationCode`, `startBrowser`, `startBrowserAwait`, `openUrl`, `openVideoPlayer`, `showBrowser`, `getReadBookConfig`, `getReadBookConfigMap`, `getThemeMode`, `getThemeConfig`, and `getThemeConfigMap` remain without equivalent host Java methods/typed host ports (§24.21). These are source-help reachable UI/settings APIs, not optional decorative methods.

### Status and next actions

- **Classification:** source/help method inventory **partial**; helper implementations for bytes/URI/time/HTML/Chinese, getTag, concurrency, WebView UA and toast/Android-ID ports **not fully differential-tested**; `log/logType` **confirmed behavior mismatch**; `getSource` identity and batch lifecycle **blocking** per §24.20; source UI/verification/settings methods **blocking** per §24.21. The four-fixture scan is a small sample only.
- **Test evidence boundary:** no tests executed in this inventory slice. No production or test file changed.
- **Still open under §26.13:** exact method-by-method signatures/defaults/nullability for every remaining declaration; source-help example and external-source caller coverage for undocumented methods; JavaImporter and full Rhino wrapper/reflection/overload dispatch; helper differential tests; compiled pre-R8 JVM ABI. Do not close task §26.13 based on lexical inventory counts.
- **Status:** task §26.13 remains **in_progress**; Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.
- **Next ordered action:** trace JavaImporter and actual Rhino wrapper dispatch for undocumented methods and returned crypto/JsURL objects, then finish per-method caller/overload matrix with external source examples. Keep UI/verification/config blockers and serializer/pre-R8 ABI as independent gates. No Stage-1 or production implementation.

## 24.24 JavaImporter/reflection closure and external source evidence — critical compatibility boundary (2026-11-24)

- **Inspection snapshot:** Badcat branch HEAD `740a21645ebdd3fa8fbff0a8b5543e6f38ac0e54`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. This section also corrects the earlier JsURL property-test statement in §24.22 and classification in §24.23.
- **Pinned Rhino source identities:** upstream `RhinoScriptEngine.kt` `19951c27c861af6815f43e4756935634135ac1f7`; `RhinoContext.kt` `41344c930e6050248f53dea6eddebd1e0af9da16`; `RhinoClassShutter.kt` `e9033fa47753038751191926a4f898bf5680c05b`; `RhinoWrapFactory.kt` `4a3d8c30329664e725cb64ae89a88a5dfe4ebc6a`; `CatchableNativeJavaObject.kt` `d53d606745c8e31a9feeff96b87c2c835b1dfcaa`; `ClassNameMatcher.kt` `b2aab11c65f59162a8d4b73a07e86e39698650a3`; `ProtectedNativeJavaClass.kt` `b9105612cad4830c2bcae93a1ff8731501cd4bda`; `NativeBaseSource.kt` `8313dedf737fe42cb4381535f6e74760c4f31786`.
- **Host Rhino source identities:** `RhinoScriptEngine.kt` `84552db27cfff78d52905377fa6774dbc5dea34e`; `RhinoContext.kt`, `RhinoClassShutter.kt`, `RhinoWrapFactory.kt`, `CatchableNativeJavaObject.kt`, `ClassNameMatcher.kt` and `ProtectedNativeJavaClass.kt` are each byte-identical to the pinned upstream blobs above; `LegadoRhinoSandbox.kt` `1c8177f078f524c6cd7cf87d155da7cae13c53ec`; `RhinoSourceScriptRuntime.kt` `879d6ed978894b16271cf5207348176fd040eda7`.
- **Host test identities:** `RhinoSourceScriptRuntimeTest.kt` `803831e73835409155629a3255776cc3c808a8cf`; `RhinoOverloadResolutionTest.kt` `a882d3e52a0fd4cbd4f50803b3076a5acd457e07`; `RhinoSourceJsAbiBytecodeTest.kt` `f3dc5c9d0d2b8edeaffa9b8500110f4e716439fd`; `RhinoHostSurfaceInventoryTest.kt` `aa7b2ebdcd37ce4e0d1c462b2dd236cf93a1395b`.

### Rhino core reuse versus host scope divergence

- `RhinoContext`, `RhinoClassShutter`, `RhinoWrapFactory`, `CatchableNativeJavaObject`, `ClassNameMatcher` and `ProtectedNativeJavaClass` are byte-identical to the pinned upstream files. Thus the core generic Java-object wrapper, overload method cache, exception translation, Java-class protection and class-name matcher are reused rather than reimplemented. The host `RhinoScriptEngine` differs from upstream by resolving `globalThis` through the active ScriptBindings scope and adding `callSourceFunction(action, scope)` (host lines 281–345); it calls callbacks with the source-scoped top-level `this` rather than always using the lexical top-level scope's `globalThis` field. Host tests exercise callback `this === globalThis` for `functionThisObj`, `singleFlight` and `lock`.
- Pinned `RhinoScriptEngine.newStandardTopLevel()` calls `Context.initStandardObjects()` (lines 147–155), then installs the pinned `RhinoClassShutter` and `RhinoWrapFactory` (lines 285–297). The pinned shutter is a blocklist: it hides dangerous classes such as `Class`, `ClassLoader`, `Runtime`, `ProcessBuilder`, `File`, `FileInputStream`, reflection/invoke and filesystem packages, but leaves many ordinary Java packages/classes visible. It does not replace the standard `Packages` or `JavaImporter` objects. `RhinoClassShutter` is identical in the host copy.
- **Host source scope is materially narrower:** `RhinoSourceScriptRuntime` executes `LEGADO_CRYPTO_COMPAT_JS` before source jsLib/mainJs/rule code (host runtime lines 477–495). That shim assigns a plain JavaScript object to global `Packages` and replaces global `JavaImporter` with a plain JS constructor that only implements `importPackage` by copying enumerable properties from those JS objects (constant lines 700–788). The shim's `Packages.java.lang` is empty; `Packages.java.util` exposes only Base64 and Arrays; `Packages.javax.crypto.spec` exposes only AES SecretKeySpec/IvParameterSpec; `Packages.javax.crypto.Cipher.getInstance` accepts only `AES/CBC/PKCS5Padding`; `Packages.org.jsoup` exposes Jsoup. It does not expose `java.io`, `java.util.zip`, `java.security`, `android.util`, `DESede`, `DESKeySpec`, `SecretKeyFactory`, `MessageDigest`, `InflaterInputStream`, `ByteArrayInputStream` or `ByteArrayOutputStream`. This is a small explicit compatibility shim, not upstream JavaImporter/Packages behavior.
- The host test `supportsRestrictedJavaImporterAesCbcDecryption` (RuntimeTest lines 1164–1204) proves only the narrow AES/CBC path using `java.util.Base64`, AES SecretKeySpec/IvParameterSpec and the custom Cipher shim. `exposesOnlyAllowListedSourceJavaHelpers` (lines 457–478) checks selected helpers and that a call to `Packages.java.lang.Runtime` is blocked (lines 448–455). No inspected host test exercises general Java package import, DESede, Android Base64, java.security.MessageDigest, java.io streams or java.util.zip classes.

### External BookSource evidence for the missing JavaImporter classes

- **Actual source JSON, stream decompression:** `entr0pia/MyLegadoSource@c8eb07c675a43a88d084951cee5cfaa96b7fe2ce`, `bookSource.json` blob `6261825aa1dbf68dcf9007e4020a571e4a119032`, line 2807. A real `ruleContent.content` script constructs `JavaImporter`, imports `Packages.java.lang`, `Packages.javax.crypto`, `Packages.javax.crypto.spec`, `Packages.java.io`, `Packages.java.util` and `Packages.java.util.zip`, decrypts with AES/CBC, then uses `InflaterInputStream(new ByteArrayInputStream(...))` and `ByteArrayOutputStream`. Host shim supports only the AES piece and cannot resolve the imported IO/ZIP classes.
- **Actual source JSON, DESede/Android Base64:** `jing332/file@256aa154f114df4c2bc3ae322e7c8ba79d19cb0b`, `Legado_bookSource_220830_Namo.json` blob `b0f80d3fb7f7f8d5dc167fc74d8ca6a3f169006d`, line 227. The BookInfo init rule imports `Packages.android.util`, `Packages.javax.crypto.spec` and `Packages.javax.crypto`, then uses `Base64.decode`, `SecretKeySpec`, `IvParameterSpec` and `Cipher.getInstance("DESede/CBC/PKCS5Padding")`. The same file line 1784 uses `DESKeySpec` and `SecretKeyFactory`; line 282 uses Hutool ArrayUtil/IoUtil and `java.util.zip.InflaterInputStream`. None of those packages/classes/algorithms are exposed by the host shim.
- **Published rule documentation example, Java security:** `ljun223/legado2xsgg@2cb6e6ec04c29738fd64c54981b1b31ab9888c1a`, `docs/legado-书源规则.md` blob `6538c3d1a66f43d8883269827d0877072292906e`, lines 301–323. The documented BookSource JS uses `JavaImporter(Packages.java.lang, Packages.java.security)`, `MessageDigest.getInstance("MD5")`, `StringBuilder` and `Integer.toHexString`. Host `Packages.java.lang` is empty and has no `java.security` package, so this documented source pattern is not supported.
- These external source snapshots are evidence that the unsupported classes are used by public Legado source rules/examples, not hypothetical Java APIs. They are not a statistically complete or necessarily currently maintained corpus; nevertheless, one confirmed real source is enough to disprove a claim of general JavaImporter compatibility.

### Host overload/ABI test coverage and its limits

- `RhinoOverloadResolutionTest` tests a toy receiver with same-name/same-arity String and Int overloads and verifies String/number/null/undefined selection (lines 15–45). This is useful engine-level coverage but not the full overload matrix of JsExtensions, UrlOption, crypto helpers or source objects.
- `RhinoSourceJsAbiBytecodeTest` reflects the compiled host bridge and calls it through Rhino (lines 22–114); it explicitly says it does **not** claim upstream bytecode parity (lines 13–19). It asserts `getSource()` returns `Map`, `getBatchContext()` returns `Any`, `cacheContent(Any,String)` returns primitive Boolean, and records selected default-argument/overload descriptors. This confirms the host's actual ABI but also confirms the BaseSource identity gap in §24.20.
- `RhinoHostSurfaceInventoryTest` (lines 24–106) captures compiled JVM members and Rhino-visible member names for the four host scope objects and returned wrappers, including `RhinoLegadoUrl`, symmetric/asymmetric crypto and Sign. Its own report labels itself an inventory, not parity or safety approval (lines 24–28 and 78–97). The test was executed successfully in CI runs #3344, #3346 and #3347; the compiled surface JSON artifacts are recorded in §24.25.
- **Correction to §24.22/§24.23:** `RhinoSourceScriptRuntimeTest.supportsLegadoJsUrlPropertiesAndRelativeResolution` does test the host `toURL` property shape (blob `803831e73835409155629a3255776cc3c808a8cf`, lines 777–798): `host`, `origin`, `pathname`, `searchParams.get`, relative resolution against a base URL and absolute URL with null base all pass in the test's asserted result. Common-case Rhino property access is therefore **tested**, not unverified. Malformed URLs, fragments, repeated query keys, default ports, empty base and the full JsURL API/edge semantics remain open.

### Compatibility conclusion

- **Core Rhino wrapper:** exact source reuse for the listed core files; selected overload behavior has host unit tests. **Host source JavaImporter/Packages:** **blocking partial subset**; it demonstrably excludes public source-rule classes in the external examples above. **Returned objects:** selected crypto and JsURL paths have host tests, but bounds, class methods, edge cases and upstream JVM descriptors remain unclosed. **Source API surface:** still differs from BaseSource identity and the full JsExtensions methods as previously recorded.
- **Test evidence boundary:** source/test bodies inspected only; no tests executed in this documentation-only audit slice. No test pass is claimed.
- **Status:** task §26.13 remains **in_progress**; Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**. The restricted JavaImporter is a confirmed blocker for the full-compatibility goal, not an implementation task authorized by this audit registration.
- **Next ordered action:** finish the method-by-method external source corpus matrix, determine the minimum typed/secure host class surface needed for the observed imports (without broad unbounded reflection), and record every unsupported class/constructor/method. Keep this as Stage-0 architecture evidence; do not expand the shim or start Stage-1 until a separate implementation task is registered.

## 24.25 CI artifact-backed API surface, test and live-source evidence (2026-11-24)

- **Successful CI baseline:** run #3344 / ID `45284431635`, job `assemble-debug` / ID `138076936734`, exact HEAD `4243e36f307551c344cfb7bb818db4daf1d40b65`, completed **success**. Every job step completed successfully: pinned upstream verification, contract tests, authentic Legado runtime model tests, JS API inventory, source dependency closure, pinned Rhino verification, G2d source compile probe, third-category app unit suite, API 28 and API 35 Android instrumentation, host JVM ABI capture, APK assembly/DEX verification, and live-source smokes. Current branch HEAD at the time of this audit is `d6fc9d05165fd9132ebd9fd8f67731e5c140b0f3`; comparing from #3344's HEAD shows only documentation paths changed, so the source/build/test tree remains identical.
- **Unit/runtime test reports from #3344:** `badcat-unit-test-reports` artifact ID `14871382867`, ZIP SHA-256 `e857653be8305a15d23cb5573881d47552c35cf54118f9f910d80ea650b7c435`: `testDebugUnitTest` **332 tests / 0 failures / 0 ignored** (17.356 s); `RhinoSourceScriptRuntimeTest` **96 / 0 / 0** (2.627 s). The runtime test class includes `supportsRestrictedJavaImporterAesCbcDecryption`, `restrictedJavaImporterRejectsNonAllowListedCryptoAndJavaClasses` and `supportsLegadoJsUrlPropertiesAndRelativeResolution`.
- **Repeat successful run #3347:** run ID `45285509232`, job ID `138080682225`, exact HEAD `5e5909d9f21cb7971a5b4f1aa64bfed9cf2e6270`, conclusion **success**, all job steps completed successfully. Its unit report artifact ID `14872860825`, ZIP SHA-256 `f8e9bd6dcc6f15ded103581ac37be99270569bcd3da529e92b6a8fe7386c1e61`, again records **332 tests / 0 failures / 0 ignored** (17.705 s), including `RhinoSourceScriptRuntimeTest` **96 / 0 / 0** (2.684 s). Run #3346 / ID `45285083104`, exact HEAD `08805c01495f7ff10b4f1b65bc273b4df222bd9a`, job ID `138079195816`, also completed **success** with every job step successful.
- **Android instrumentation from #3344:** `legado-rule-analyzer-instrumentation-api35` artifact ID `14871347924`, ZIP SHA-256 `6eff0190aaf98469acc1171caf049910e6e14b67df3552fd67dacffd878b47dc`: API 35 emulator report **5 tests / 0 failures / 0 skipped** (2.102 s). It covers authentic pinned RuleAnalyzer packaging/callability and WebView cookie/overload behavior. API 28 CookieManager instrumentation step also completed successfully. #3346 and #3347 repeat both API 28 and API 35 instrumentation steps successfully.
- **Live source E2E from #3344:** `badcat-live-source-smoke-report` artifact ID `14872660260`, ZIP SHA-256 `4f78e856286e8fdd1249324402db392229a8024fe7811f89f01808dd3a78cb72`: Bixiange GBK POST search → detail → AJAX-paged TOC → chapter content **PASS**. `badcat-pinshuzhai-live-source-smoke-report` artifact ID `14872635301`, ZIP SHA-256 `749d7e6f8928fd887337ac7fecf1460e0d5aea5b8a0888d4c5a17c9966a91335`: Pinshuzhai search → detail → 1,650-chapter TOC → first chapter content **PASS**. #3347 repeats these two smoke reports with ZIP hashes `c57464240cd8563a511dc5c69b9e33a0f23b88b6c4be7f280544e9afb581e699` (Bixiange) and `5cd973d58d6dedcf239730536d52b3b27b211264983c6c7229e35cc35f6071b1` (Pinshuzhai). These are two concrete sources only; neither exercises the external JavaImporter imports in §24.24.
- **Source declaration inventory artifact:** `legado-js-abi-inventory` #3344 artifact ID `14871760152`, ZIP SHA-256 `2f1c94966a58616ab82b41601c1134d09b86244b6557d86d1100139249eef2a7`: 148 upstream base declarations / 104 names; 150 host declarations / 110 names; 17 missing names; 22 overload-count differences; 38 same-name signature-string differences. #3347 artifact ID `14872755053`, ZIP SHA-256 `9d3c8b056bcf7238070b7135d9c48e42678d8683b78883ec3effb4d6d196d9e4`, regenerated the same inventory. The 38 signature strings are not all semantic mismatches: some are parameter-name/format-only. Genuine type/return/overload deltas include `ajax/ajaxAll/ajaxTestAll`, `connect/get/head/post` response types, `getFile` (`File` vs `RhinoSourceFileHandle`), `getSource` (`BaseSource?` vs `Map`), `getBatchContext` (`BatchContentContext?` vs `Any?`), `getTag` nullability, `log/logType`, `timeFormat/timeFormatUTC`, `toURL`, and nullability of Base64/hex/HTML inputs or outputs.
- **Compiled Rhino surface inventory:** #3344 artifact `legado-host-rhino-surface` ID `14871665970`, ZIP SHA-256 `7d8526752abd193082e771755daeb95288f05aec0cd38cdfd9e461f4a5ebb2aa`; #3347 artifact ID `14871811557`, ZIP SHA-256 `198201820798f06872030d877a584cb72a0415dfdca0455dbaddcdb9fe061733`. The generated report records four production bindings: `java`, `cookie`, `source`, `cache`. `scope.java` has 154 declared public methods, 163 inherited public methods, 124 Rhino property IDs and 119 callable names. `scope.cookie` lacks `setWebCookie`; `scope.cache` has one-argument `get(key)` but no `get(key, onlyDisk)`; `scope.source` contains login/session key-value methods but no `refreshBookInfo/refreshBookToc/refreshContent`; `scope.java` lacks the read/theme settings APIs. Returned wrappers include `RhinoSourceFileHandle`, `RhinoSourceHttpResponse`, `RhinoLegadoUrl`, bounded symmetric crypto, asymmetric crypto and Sign. `RhinoLegadoUrl` exposes `host`, `origin`, `pathname`, `searchParams` and extra aliases; common property access is verified by the 96-test runtime suite.
- **Compiled host JVM ABI:** #3344 artifact `legado-host-jvm-abi` ID `14871163205`, ZIP SHA-256 `f69f1182aaf8fa7f4df29b0c15bd3196afb3132f5c68f84ecb226d011ba18e9d`; `RhinoSafeSourceJavaExtensions.javap.txt` SHA-256 `7b711a33145eaea1a49a7e693c04ab4b16033bdde75ea0fcac2618fee2a355b2`. It confirms host descriptors including `getSource(): Map`, `getBatchContext(): Object`, `cacheContent(Object,String): boolean`, `getFile(String): RhinoSourceFileHandle`, `ajaxAll(Object,boolean): RhinoSourceHttpResponse[]`, `timeFormat(Number/String)` and `toURL(String): RhinoLegadoUrl`. This is host pre-R8 ABI evidence only; no upstream compiled ABI comparison exists.

### Public source probe caveat in run #3347

- Run #3347's `Probe public Legado source repositories (best effort)` job step is marked success, but its log contains a sub-probe process exit code 1. The Nyasama sample returned usable search results for 3/5 checks and probed five extra endpoints. The separate public endpoint sweep attempted 14 GET endpoints and received 9 non-empty HTTP 200 responses; it attempted **0 static POST** sources. Its script explicitly excludes dynamic `@js`/`<js>` descriptors because they require Android Rhino and says this is bounded search-list extraction, not the full Legado JS/rule pipeline. The four user-paused source hosts were skipped. Therefore this step is endpoint smoke only, not a JavaImporter, JavaScript rule or complete source-compatibility test.
- The two Bixiange/Pinshuzhai E2E reports above are real rule-runtime smokes; they do not cover the three external JavaImporter source examples in §24.24. The public JavaImporter gap remains confirmed.

### CI states and acceptance

- #3345 / ID `45284865966`, exact HEAD `9c5ab05f53f3f5ec6da62d1edda64ddb5155c60a`, failed before test execution because Gradle could not resolve `org.jetbrains.kotlin.plugin.parcelize:2.4.20`; no artifacts were produced (root-cause evidence in §26.16).
- Direct follow-up poll supersedes the older CI-ledger snapshot: #3346 / ID `45285083104` (HEAD `08805c01495f7ff10b4f1b65bc273b4df222bd9a`), #3347 / ID `45285509232` (HEAD `5e5909d9f21cb7971a5b4f1aa64bfed9cf2e6270`) and #3348 / ID `45285708237` (HEAD `740a21645ebdd3fa8fbff0a8b5543e6f38ac0e54`, job `138081364891`) all completed **success**. #3348 had every job step successful, including contract tests, unit tests, API 28/API 35 instrumentation, ABI capture, APK/DEX verification and live source smokes. #3349 / ID `45286481422` (HEAD `540e2233a35964511401d062363f4d94ef1a6f83`) remains `in_progress`. Audit-writeback runs #3350–#3356 (IDs `45287185482`, `45287203642`, `45287253646`, `45287259506`, `45287309333`, `45287326843`, `45287802126`) were `in_progress` at the latest direct poll. The CI ledger's 3348-pending snapshot is stale; do not infer a result for #3349 or #3350–#3356.
- **Status:** #3344/#3346/#3347 green tests and two live-source E2E reports strengthen the host evidence but do not close the restricted JavaImporter/Packages source-class gap, missing read/theme settings methods, BaseSource identity, batch-content lifecycle, CookieStore/WebView callbacks or full public method/overload matrix. Task §26.13 remains **in_progress**; Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.
- **Next ordered action:** use the generated source declaration, compiled Rhino surface and host JVM ABI artifacts as the per-method baseline; complete the external JavaImporter caller matrix and identify the required typed/secure class facade. Keep this as Stage-0 architecture evidence and register a separate implementation task before any shim expansion.

## 24.26 Typed JavaImporter source-demand matrix — observed constructors and method requirements (2026-11-24)

- **Scope:** derive the minimum confirmed class/constructor/method surface from the three public source examples in §24.24. This is not a complete source corpus and not an implementation authorization. The upstream pinned class shutter is a denylist and does not block these ordinary byte-array, zip, digest, Base64 or cipher classes; the host's replacement Packages object is what currently makes them unreachable.

| Source evidence | Required Java-style surface | Current host shim status | Compatibility consequence |
|---|---|---|---|
| `entr0pia/MyLegadoSource@c8eb07c675a43a88d084951cee5cfaa96b7fe2ce`, `bookSource.json` line 2807 | `java.io.ByteArrayInputStream(byte[])`; `java.io.ByteArrayOutputStream()` and `(int)`; `java.util.zip.InflaterInputStream(InputStream)`; `read()`, `write(int)`, `toByteArray()`/`toString()`, `close()`; JavaImporter package import for `java.io` and `java.util.zip` | `Packages.java.io` and `Packages.java.util.zip` do not exist in the shim. `Packages.java.lang` is empty. `java.aesBase64DecodeToByteArray` exists, but the stream/decompression classes do not. | The rule can decrypt bytes through the existing helper but cannot instantiate the inflater/byte streams or complete compressed content extraction. This is a confirmed real source failure surface. |
| `jing332/file@256aa154f114df4c2bc3ae322e7c8ba79d19cb0b`, `Legado_bookSource_220830_Namo.json` lines 227, 282 and 1784 | `android.util.Base64.decode(String,int)`; `javax.crypto.Cipher.getInstance("DESede/CBC/PKCS5Padding")`, `init(mode,key,iv)`, `doFinal(byte[])`; `SecretKeySpec`, `IvParameterSpec`, `DESKeySpec`; `SecretKeyFactory.getInstance("DES").generateSecret(DESKeySpec)`; `cn.hutool.core.util.ArrayUtil` and `cn.hutool.core.io.IoUtil`; `InflaterInputStream` | No `Packages.android.util`, `DESede` transformation, `DESKeySpec`, `SecretKeyFactory`, `java.io`, `java.util.zip`, or Hutool package objects. Current Cipher shim accepts only AES/CBC/PKCS5Padding and returns a string-like wrapper from `doFinal`, not a Java byte array. | This source's DESede key derivation/decryption and decompression cannot run; the AES-only shim is not a substitute. Byte-array return identity is itself part of the contract. |
| `ljun223/legado2xsgg@2cb6e6ec04c29738fd64c54981b1b31ab9888c1a`, `docs/legado-书源规则.md` lines 301–323 | `java.security.MessageDigest.getInstance("MD5")`, `update(byte[])`, `digest()`; `java.lang.StringBuilder()`/`append()`/`toString()`; `java.lang.Integer.toHexString(int)`; JavaImporter import for `java.lang` and `java.security` | `Packages.java.lang` is empty and `Packages.java.security` is absent. | Even common MD5/signature helper rules documented by the ecosystem cannot import their required classes. |

### Minimum typed-facade requirements (not exhaustive)

- Preserve the observed source syntax: `new JavaImporter()` followed by `importPackage(Packages....)` and `with(javaImport) { ... }`. The import objects must expose class constructors/static members as Rhino-callable members, not plain placeholders.
- Required observed class surface at minimum: `java.io.ByteArrayInputStream`, `ByteArrayOutputStream`; `java.util.zip.InflaterInputStream`; `java.security.MessageDigest`; `java.lang.StringBuilder`, `Integer`; `android.util.Base64`; `javax.crypto.Cipher`, `SecretKeyFactory`; `javax.crypto.spec.SecretKeySpec`, `IvParameterSpec`, `DESKeySpec`; `java.util.Base64`, `java.util.Arrays`; `cn.hutool.core.util.ArrayUtil`, `cn.hutool.core.io.IoUtil`; and existing `org.jsoup.Jsoup`.
- Preserve Java-compatible value shapes: `Cipher.doFinal`, `MessageDigest.digest`, `ByteArrayInputStream` and `ByteArrayOutputStream.toByteArray` must exchange byte arrays, not UTF-8 string wrappers. Preserve integer constants such as cipher modes and Base64 flags, overload/constructor selection, null/undefined conversion, exception types/messages where source rules branch on them, and method chaining/return values.
- Keep the facade typed and bounded rather than exposing arbitrary reflection: allowlist constructors/methods/algorithms; cap encrypted/input bytes; cap decompressed output bytes and expansion ratio; bound stream read loops and execution time; honor cancellation; always close streams; reject unsupported cipher transformations explicitly. Preserve exact Android Base64 flag semantics and Java/Hutool byte-array semantics.
- These examples establish a **minimum observed compatibility set**, not a complete allowlist. A full decision still requires scanning a representative external BookSource corpus for `JavaImporter`, `Packages`, `java.*`, `javax.*`, `android.*`, `org.jsoup.*` and `cn.hutool.*` usage, then comparing every constructor/static/instance method against the pinned upstream class visibility and Rhino wrapper.

- **Classification:** current host JavaImporter remains a **blocking partial shim**. A typed secure facade design is required before implementation can be authorized; do not simply add arbitrary package reflection or broaden ClassShutter without a security/resource review. Task §26.13 remains **in_progress**; Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.
- **Next ordered action:** complete external source corpus sampling and a typed class/method/constructor matrix; then register a separate implementation task with explicit security boundaries, resource caps, exact source fixtures and upstream-vs-host differential tests. No code change in this section.

## 24.27 External BookSource corpus scan — JavaImporter import-path frequency and observed calls (2026-11-24)

- **Scope and snapshots:** static scan of two large public source repositories already identified in §24.24, using pinned repository commits and exact file blobs. `entr0pia/MyLegadoSource@c8eb07c675a43a88d084951cee5cfaa96b7fe2ce` file `bookSource.json` blob `6261825aa1dbf68dcf9007e4020a571e4a119032` (203,072 bytes); `jing332/file@256aa154f114df4c2bc3ae322e7c8ba79d19cb0b` file `Legado_bookSource_220830_Namo.json` blob `b0f80d3fb7f7f8d5dc167fc74d8ca6a3f169006d` (226,614 bytes). This is a source-file scan, not execution or a complete ecosystem corpus.
- **MyLegadoSource scan:** found 4 textual `JavaImporter` occurrences (the same decrypt pattern is repeated across multiple rule fields), with imported paths `java.lang`, `javax.crypto`, `javax.crypto.spec`, `java.io`, `java.util` and `java.util.zip`. Constructor/use tokens include `InflaterInputStream`, `ByteArrayInputStream` and `ByteArrayOutputStream`; the rule calls the existing Legado helper `java.aesBase64DecodeToByteArray(...)` and then performs stream decompression with `read()`, `write()`, `close()` and `toString()`. This independently confirms that the host's missing IO/ZIP packages block a real BookSource content/explore/search/TOC rule pattern.
- **jing332/file scan:** found 6 textual `JavaImporter` occurrences across the source JSON, with imported paths `android.util`, `javax.crypto`, `javax.crypto.spec`, `java.lang`, `java.util`, `java.util.zip.InflaterInputStream`, `cn.hutool.core.io.IoUtil` and `cn.hutool.core.util.ArrayUtil.resize`. Observed class/method tokens include `Base64.decode`, `Base64.encodeToString`, `Base64.getDecoder`, `Arrays.copyOfRange`, `Cipher.getInstance`, `SecretKeyFactory.getInstance`, `IoUtil.read`, `DESKeySpec` and `InflaterInputStream`. This is not just an AES/CBC-only requirement: the source set uses Android Base64 flags, DESede and DES key derivation, Hutool byte/IO utilities, and compressed-stream handling.
- **Cross-source minimum from static scan:** native package imports are not limited to the host's current `java.util.Base64`, `java.util.Arrays`, AES `SecretKeySpec`/`IvParameterSpec`, AES/CBC `Cipher` and Jsoup shim. The observed source rules require Java IO streams, ZIP inflater streams, Android Base64, DES/DESede transformations, SecretKeyFactory/DESKeySpec, Hutool ArrayUtil/IoUtil and (from the third ecosystem doc example in §24.24) Java security MessageDigest plus StringBuilder/Integer. This confirms the §24.26 minimum typed-facade list.
- **Host coverage status:** `Packages.java.lang` remains empty; `Packages.java.io`, `Packages.java.util.zip`, `Packages.java.security`, `Packages.android.util` and `Packages.cn.hutool` are absent; Cipher transformation support remains AES/CBC/PKCS5Padding only. No host runtime execution was attempted because this section is a documentation/source-scan task and the existing shim is known to reject the observed imports. The source files are data; no rule code from them was executed during this scan.
- **Coverage caveat:** repeated `JavaImporter` strings are not independent source counts; JSON may contain duplicated rules for one source, and the two repositories are not statistically representative of all current sources. Use this as confirmed minimum evidence, not a complete frequency estimate. The next corpus pass should add more independently maintained repositories and classify every imported class/member, not simply count occurrences.
- **Classification:** JavaImporter/Packages compatibility remains a **confirmed blocking gap** for full source compatibility. This section adds no production code and does not authorize a broad reflection escape hatch. Task §26.13 remains **in_progress**; Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.
- **Next ordered action:** expand the external corpus sample, normalize imports to exact FQCNs and member calls, then prepare a typed constructor/method matrix with byte-array and resource-limit semantics. Register implementation separately after architecture review.


## 24.28 Expanded public BookSource Java bridge corpus + pinned Rhino security boundary (2026-11-24)

### Scope and method

This is a read-only static scan of public Legado-format BookSource JSONs and a pinned-upstream instrumentation test. The sample set spans 11+ repository names and includes source rules, jsLib, loginUrl, content decoding and image/stream fixtures. Some repositories mirror the same AES source family; mirrors are not counted as independent feature evidence. No source script was executed, no remote book endpoint was contacted, and embedded credentials/tokens were intentionally not copied into this report. The corpus is discovery evidence, not a statistically representative census.

Pinned reference identities:
- Legado upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Upstream `JsExtensions.kt`: blob `f6808edd2a0d312e8bd13649935dea444cb82581`; `JsEncodeUtils.kt`: `c75b61650db2abdd64a09d2bd8340ef14c2947b3`; `BaseSource.kt`: `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa`; `NativeBaseSource.kt`: `8313dedf737fe42cb4381535f6e74760c4f31786`; upstream `RhinoClassShutter.kt`: `e9033fa47753038751191926a4f898bf5680c05b`; `RhinoWrapFactory.kt`: `4a3d8c30329664e725cb64ae89a88a5dfe4ebc6a`.
- Current Badcat branch snapshot before this audit write: `fb5b2dc2b6345a24edf8342693ff333e1385e797`; `RhinoSourceScriptRuntime.kt` blob `879d6ed978894b16271cf5207348176fd040eda7`; `RhinoSafeSourceJavaExtensions.kt` blob `6a00e649895c4c1d223b768c6b288632ec033de9`; its `RhinoClassShutter.kt` and `RhinoWrapFactory.kt` blobs match the pinned upstream files above.

### Source corpus samples (exact snapshots)

| Public repository @ commit | Source file / blob | Observed source-level Java calls |
|---|---|---|
| `aoaostar/legado@5d18c8458ead9e017a72befd2b07cfaf2133ca8a` | `sources/12212706.json` / `55a85413ffcb52ac28e609ff095f72bb3e85f713` | JavaImporter class arguments for `javax.crypto.Mac`, `SecretKeySpec`, `javax.xml.bind.DatatypeConverter`, `URLEncoder`, `String`, Android `Base64`; HmacSHA1 signing, `getBytes("UTF-8")`, `Mac.init/doFinal`, URL encoding and Base64 output. |
| `aikankankanhhh998/shuxiangzhijiayuan@af4209e5cc687d773a2d98d0f56ef8cb00a80d40` | `All.json` / `d93ba3ca424e48f020b1816a1cbfb297cc3ba009` | `JavaImporter.importPackage` for java.lang, javax.crypto.spec, javax.crypto and android.util; AES/CBC/PKCS5Padding encrypt/decrypt; Android Base64 decode/encode flags; repeated POST request signing. |
| `Mina-kk/yuedu-MCP@e129d35e7cba08f8dcacc2cf05dc3e626b5965dd` | `app/src/main/assets/corpus/shards/f_0976.json` / `72b449a987efe5bef7098d83ce177d6685662a56` | DESede Cipher, SecretKeySpec, Android Base64 decode/encode, Cipher init/doFinal; used across search/detail/TOC/content rules. |
| `ccyykkcyk/yd@54f7027c01c8a997f58995db56c75c6a0cd7216b` | `sy/namofree.json` / `51aafb8879e10d54a8195e98daeac8ce91e452dd` | DES with DESKeySpec + SecretKeyFactory; AES/CBC using Java Base64 decoder and Arrays.copyOfRange; Android Base64 NO_WRAP. |
| `yuedu520/yuedu@b5524f41d80fe4071c3d695fd1d6990cd6de97aa` | `250206.json` / `ec14861b6f312325519fa3f63f0ceb0339d40073` | Java Base64 getDecoder/decode, Arrays.copyOfRange, AES/CBC/PKCS5Padding. |
| `yuyuan-cc/shuyuan@faec02099a6671d3fdeb28dda25eb7d59c08bae0` | `13.json` / `a0274bc6930ae2ecbe1b072652cbb12b02bcf0dc` | `java.net.URL`, `openConnection`, GET, connect/read timeouts, request properties, InputStream read/close, disconnect, System.currentTimeMillis. |
| `qucomic/legado_source@4a082b9b46ee86d41f838269b78570d6a4d884fb` | `source/7818.json` / `fecb7f66937d78609d3ab5becf2fefa4882ffbe0` | Direct `Packages.java.net.URL(url).openConnection()` without JavaImporter; connect/read timeout, User-Agent, response code and disconnect. Confirms direct Packages access is a separate caller path. |
| `mumuceo/file01@8429f9d6e03112f77da35403dee2682edc67217b` | `202608/11512_785d0d7100d630c37bc9da1a6e68808f.json` / `9d19ba42a267e2092465bf8265a291c7f7e9a5b9` | ByteArrayOutputStream + InputStream read/write/toByteArray; Jsoup Connection HEAD/timeout/ignoreContentType/followRedirects/execute; Collections.reverse; Thread.sleep. |
| `mumuceo/file01@8429f9d6e03112f77da35403dee2682edc67217b` | `202503/4078_b61aed65cea737e54d4276c6edbc7e3f.json` / `adfa5e2dd332089e2adcc6a4c65c78e7b6187728` | DES/CBC/PKCS5Padding with DESKeySpec + SecretKeyFactory; `JavaImporter.importPackage`; global `importClass(org.jsoup.Jsoup)`; this is a distinct global importClass caller, not JavaImporter.importClass. |
| `shidahuilang/shuyuan-bak@aee8e77c15775d008e110c25b4bd7ac331c51a13` | `大灰狼订阅源.json` / `c6ae2d6b1eb649ca1a485537459daf43fe8abecd` | JavaImporter instance `importClass` with Android Base64, String, URL and okhttp3.HttpUrl; HttpUrl.parse/topPrivateDomain; Thread.sleep. This source also references `io.legado.app.ui.book.manga`, a product UI/manga dependency excluded from the Badcat no-Legado-UI requirement and not a core Java bridge acceptance target. |
| `aoaostar/legado@5d18c8458ead9e017a72befd2b07cfaf2133ca8a` | `sources/12212706.json` / `55a85413ffcb52ac28e609ff095f72bb3e85f713` | See HMAC sample above; the same JavaImporter class list and signing pattern is independently present in upstream Android instrumentation test `AndroidJsTest.testPackages`. |
| `MajoSissi/legado-source@87fcd7e63ed31a005dfcda4b28c7a39d57b1e947` | `books/Bot.json` / `1bb06046960ac0f1d00746df710464b53cddb33e`; `source/light-novel.json` / `275e4397ebab98c7e4d417f9c58de827056c0e82` | Jsoup.connect with Connection.Method.PUT, headers, requestBody, execute/body; direct URL.openConnection POST with setDoOutput, output stream, response code, InputStreamReader, BufferedReader, StringBuilder/readLine/close/disconnect; jsLib also imports okhttp3 and uses OkHttpClient, Request.Builder, RequestBody, MediaType, newCall/execute/body/code. |
| `xxxxuanran/BookSource@6f94ca8c36c5dba48d2ed10a1fc4384ea656cb9d` | `bookSource.json` / `9e8197c9c74d3d078c69bfa5eebf93ac7077def8` | Direct Packages access to String, MessageDigest.getInstance("MD5").digest, URLEncoder.encode, Java Base64 decoder, Arrays.copyOfRange and AES/CBC. |
| `qucomic/legado_source@4a082b9b46ee86d41f838269b78570d6a4d884fb` | `source/7846.json` / `7448c50f3bafe46675cbb92a7db7fb834f340ceb` | MessageDigest MD5; Java Base64; Arrays.copyOf/copyOfRange; AES/CBC; Java String byte-array constructors; java.util.zip.Inflater(true), setInput/inflate/end to inflate raw DEFLATE content. |
| `h11128/legado-cli@8a6effdc33bfb411af9a2149f4f84acd9736440e` | `fixtures/samples/sources/ximan_comic_source.json` / `5600225af2c0811e11e46980404b72b6769fefe7` | Packages.java.io.InputStream and ByteArrayOutputStream; InputStream instanceof/read loop, write, toByteArray; byte-oriented image/cover decode. |
| `yolo52/Yuedu@9e9eb4ec08ffa5ac8986e2f07cae36c09f854d0a` | `轻小说.json` / `63a4c986bd5e3b8fa01c02d49e64aa18000ece2e` | Packages.java.lang.Thread.sleep with repeated 1–10 second waits; cn.hutool.crypto.symmetric.AES and cn.hutool.core.util.RandomUtil. |

The repeated AES collection files in `xiangquyuedu` and `qingzhuanyuedu`, and mirrored DESede examples, are retained as corroborating snapshots but not counted as separate API families. `java.util.concurrent` is imported in one sample but no concrete class/method call was found there, so it is not promoted to a required method.

### Pinned upstream Android proof and class-shutter correction

- Pinned upstream `app/src/androidTest/java/io/legado/app/AndroidJsTest.kt`, blob `ab3b4fbd9dadc4ec641a7bba80162eb7a150741b`, lines 16–42, constructs `new JavaImporter(Packages.javax.crypto.Mac, Packages.javax.crypto.spec.SecretKeySpec, Packages.javax.xml.bind.DatatypeConverter, Packages.java.net.URLEncoder, Packages.java.lang.String, Packages.android.util.Base64)`; it executes HmacSHA1 Mac init/doFinal, explicit UTF-8 String.getBytes, Android Base64.NO_WRAP and URLEncoder.encode. This is direct pinned Android instrumentation evidence for that class-import/signing surface; DatatypeConverter is imported but its methods are not invoked in this test.
- Upstream `RhinoClassShutter.visibleToScripts(fullClassName)` denies the prefixes and classes listed in its protected matcher. In particular, `cn.hutool.core.io` is explicitly denied as a package prefix; `java.io.File`, FileInputStream/FileOutputStream, FileReader/FileWriter, ObjectInputStream/ObjectOutputStream, `java.net.URLClassLoader`, reflection, class loaders and other sensitive classes are also denied.
- **Correction to §24.26:** `cn.hutool.core.io.IoUtil` was observed in public source text, but it is blocked by the pinned upstream ClassShutter. It must not be treated as an upstream-supported compatibility requirement or added to the allowlist. The same rule applies to any other source reference under a denied class/package. By contrast, `cn.hutool.core.util.ArrayUtil/RandomUtil` and `cn.hutool.crypto.symmetric.AES` are not covered by the listed deny prefixes/names; class availability still needs exact classpath verification.
- `java.io.InputStream`, ByteArrayInputStream/ByteArrayOutputStream, BufferedReader/InputStreamReader, `java.net.URL`/URLConnection/HttpURLConnection, `java.lang.Thread`, `java.security.MessageDigest`, `javax.crypto.Mac`, `javax.crypto.*`, `java.util.zip.Inflater`, `org.jsoup.Connection` and `okhttp3.HttpUrl/OkHttpClient` are not blocked by the current listed matcher. This only establishes that the ClassShutter does not deny those names; it does not alone prove the classes exist on every Android API level or that the Badcat host exposes them.
- The pinned upstream ClassShutter is a denylist, not a narrow allowlist. Badcat must not “fix” parity by exposing unrestricted Java reflection or arbitrary package traversal. Preserve the security boundary and expose only the audited, permitted compatibility surface through typed facades.

### Badcat host delta (confirmed at exact current blobs)

- The host's `LEGADO_CRYPTO_COMPAT_JS` replaces global `Packages` with a plain JavaScript object. `Packages.java.lang` is empty; `java.util` contains only a limited Base64 decoder/encoder and Arrays.copyOfRange; `javax.crypto` contains only AES/CBC/PKCS5Padding Cipher; `javax.crypto.spec` contains only AES SecretKeySpec and IvParameterSpec; `org.jsoup` contains only Jsoup.parse.
- Its `JavaImporter` is a plain JS constructor with `importPackage` only. It does not implement instance `importClass`, nor does it reproduce native Java class/package wrappers and overload resolution. The constructor forwards its arguments to the same importPackage loop; class arguments such as those in upstream AndroidJsTest are not equivalent to importing a package. The host test currently covers only the no-argument constructor followed by importPackage(AES packages).
- Host `String.prototype.getBytes` ignores any charset argument and always calls UTF-8 encoding. This matches the UTF-8 default path but not arbitrary explicit charsets.
- The host AES shim only accepts AES/CBC/PKCS5Padding; it does not support DES, DESede, DESKeySpec, SecretKeyFactory, Mac/HmacSHA1, Android Base64 flags, or the upstream Java byte-array return semantics for all Cipher.doFinal calls. Its current doFinal adapter returns a narrow toString wrapper.
- No `Packages.java.io`, `java.net`, `java.security`, `java.util.zip`, `android.util`, `okhttp3` or `cn.hutool` namespace is exposed. `Packages.org.jsoup.Jsoup.connect` and `Packages.org.jsoup.Connection` are also absent; the host only delegates Jsoup.parse.
- Consequently, source code using URL.openConnection, HTTP output streams/readers, OkHttp, Jsoup Connection PUT/HEAD, Inflater, InputStream, MessageDigest, Mac, DES/DESede, Thread.sleep or JavaImporter.importClass is not covered by the current host shim. The existing host test suite proves its AES-only importer path and explicit rejection of unlisted Java classes; it does not prove these corpus paths.

### Required compatibility matrix additions and safety notes

1. **JavaImporter/Packages semantics:** distinguish native JavaImporter constructor class arguments, importPackage, JavaImporter.importClass, global importClass, and direct Packages references. Add fixtures for each actual call shape. Never treat the current JS object shim as a general JavaImporter implementation.
2. **Crypto and byte arrays:** HmacSHA1 Mac; DES/CBC/PKCS5Padding; DESede; DESKeySpec/SecretKeyFactory; Android Base64.decode/encodeToString flags; Java Base64; Arrays.copyOf/copyOfRange; Cipher byte[] return values; String(byte[], charset) constructors.
3. **Streams and compression:** InputStream, ByteArrayInputStream/ByteArrayOutputStream, BufferedReader/InputStreamReader, read/readLine/write/close/toByteArray; java.util.zip.Inflater and InflaterInputStream. Keep file-system classes denied. Apply byte count, decompressed-size and CPU limits.
4. **Network types:** URL.openConnection/HttpURLConnection request method, connect/read timeouts, request properties, output stream, response code, input stream, disconnect; OkHttpClient/Request/RequestBody/MediaType/Response; Jsoup Connection Method HEAD/PUT, headers, requestBody, timeout, redirects, ignoreContentType, execute/body. These must be typed adapters through the host network policy, not raw unrestricted socket/URLConnection exposure. Enforce HTTP(S), redirect/host policy, timeout, response-size, cancellation and cookie/session rules while preserving required source-visible behavior.
5. **General JDK utilities:** MessageDigest, URLEncoder, String/byte constructors, StringBuilder, System.currentTimeMillis, Collections.reverse, Thread.sleep, permitted Hutool utility/crypto classes. Thread.sleep is particularly risky: corpus rules can loop with 1–10 second sleeps and some waits span multiple minutes; it must cooperate with cancellation and script execution budgets, not block an unbounded worker.
6. **Out-of-scope or upstream-denied references:** do not implement `cn.hutool.core.io.IoUtil` or blocked file/class-loader/reflection APIs; do not expose `io.legado.app.ui.book.manga` or other Legado UI classes merely because a source references them. Keep manga/UI dependencies separately classified from the no-Legado-UI book-source Runtime requirement. Verify actual classpath availability separately from ClassShutter visibility.

### Decision

The Stage-0 evidence is now materially broader, but the JavaImporter/Packages parity gate remains **NO-GO**. The host shim currently proves only a small AES/CBC + limited Java Base64/Arrays + Jsoup.parse path. The corpus and pinned Android test establish additional source-visible APIs that must be mapped and tested before any compatibility claim. Do not implement an unrestricted Java package bridge; next complete the typed per-class/per-method/constructor/overload/return-value matrix and security mapping, then register a separate implementation task with exact tests. No application code was changed in this checkpoint.


## 24.29 Source-help blind spots, active legacy-method callers and production runtime wiring correction (2026-11-24)

### Method/help/caller discovery snapshot

- Pinned help file: `app/src/main/assets/web/help/md/jsHelp.md`, blob `17f51c144ccfeb33bb228df29f0e5600206a1a98`; pinned `JsExtensions.kt` and `JsEncodeUtils.kt` blobs are `f6808edd2a0d312e8bd13649935dea444cb82581` and `c75b61650db2abdd64a09d2bd8340ef14c2947b3`. A declaration scan still yields **148 public declarations / 104 distinct method names** across the two classes. A lexical comparison of those names with `java.*` references in the complete help file finds 35 names not explicitly named as `java.<method>`; this is a manual-review queue, not proof every one is intended as a source-callable API. Public methods can still be dynamically reachable through `NativeBaseSource` even without `@JavascriptInterface`.
- GitHub code-search counts below are discovery snapshots only: duplicate mirrors, comments, tests and generated fixtures can affect counts; counts are not unique-source counts and do not establish execution success.

| Method | Code-search discovery snapshot | Concrete source evidence inspected | Help status / caller classification |
|---|---:|---|---|
| `java.getCookie` | 89 results | `shidahuilang/shuyuan-bak@aee8e77c15775d008e110c25b4bd7ac331c51a13`, `shuyuan_data/6892.json` blob `bf029e27e2c5a298be98e6f9d5ce720e6d141253`; `Mina-kk/yuedu-MCP@e129d35e7cba08f8dcacc2cf05dc3e626b5965dd`, shard `f_1226.json` blob `821667b7c8637a8e87659e39d7da49a608d0771f`; `mumuceo/file01@8429f9d6e03112f77da35403dee2682edc67217b`, `202403/3664_45c64524391449b35e6e1aa14a6f6b77.json` blob `c2a99794600d5f5060de1aa9fcbfacd3bb7a9732`. | The help documents the separate `cookie.getCookie` object; the legacy `java.getCookie(tag[,key])` method is not the same binding and is not listed as such in the JsExtensions help. It has active login/search callers and must remain separately compatible. |
| `java.getZipStringContent` | 16 results | `Mina-kk/yuedu-MCP@e129d35e7cba08f8dcacc2cf05dc3e626b5965dd`, shard `f_0237.json` blob `2b77ecdc8f32f57994572be24bbb7f0b64183f0f`; a 51小说 book-info init rule reads `detail.json` from a ZIP URL. | Not named in jsHelp's Java extension method list; active, undocumented source caller. Upstream has both 2- and 3-argument public overloads. The 2-argument overload is not annotated but remains visible through the native wrapper. |
| `java.toNumChapter` | 44 results | `yuyuan-cc/shuyuan@faec02099a6671d3fdeb28dda25eb7d59c08bae0`, `21.json` blob `8c61593b7a0468a600994ade62c6658613e56fa6`; used in chapter-title normalization. | Not named in jsHelp; active source caller. |
| `java.queryTTF` / `java.replaceFont` | 3 / 3 results | `safino9/SourceGo@f581fc8ed2fa73feed68dbac83aee5341e07d3ac`, `book_source/💰正版/sub.json` blob `6d1085cb091a9f002c0c1d2a2897e4f6fb30b7ae` and `💰晋江.json` blob `bde7a5180b999adee5fe2b79b96e813c7c51d80b`; remote-font URL is passed to queryTTF and glyph mapping is passed to replaceFont. | Not named in jsHelp; active source caller. |
| `java.aesBase64DecodeToString` | 70 results | `yuedu520/yuedu@b5524f41d80fe4071c3d695fd1d6990cd6de97aa`, `250905.json` blob `489cf7630b96e88ccd770326c363e5c5c4ed093d`; used in init, TOC and content; `someok/booksources@1fbebca7730fa818c7405e756ede83d24ea7166c`, `src/👍 值得阅读.json` blob `802f776ba5fe75bfaaf41bfcaa308543ca7dcba5`; used in TOC/content. | Not named in jsHelp; active source caller. Same-name host method exists and delegates to bounded crypto; exact source fixture differential still needs to be added. |
| `java.HMacHex` | 4 results | `yuedu520/yuedu@b5524f41d80fe4071c3d695fd1d6990cd6de97aa`, `2502251.json` blob `c63a352a452cf7994661f6344de772a0d6e321f3`; HmacSHA1 signing. | Documented in JsEncodeUtils help and actively used. Host method exists and allowlists HmacSHA1; exact known-vector comparison against pinned Hutool HMac remains to be recorded. |

### Public method names with no production-source caller found in this search pass

The following are **not proven unused**; they are only not found by the current GitHub source search or sample set. Keep them visible in the method matrix rather than silently dropping them:
- `getRarByteArrayContent`, `get7zStringContent`, `getRarStringContent`, `get7zByteArrayContent`: no matching public source call found in this pass; remain public archive methods and must be classified/tested separately.
- `getZipByteArrayContent`: one result was a compatibility-test/coverage JSON, not a production source; it is also the implementation dependency of active `getZipStringContent`.
- `queryBase64TTF`: no direct public source call found; upstream marks it deprecated and delegates to queryTTF.
- `tripleDESDecodeStr`, `tripleDESDecodeArgsBase64Str`, `tripleDESEncodeArgsBase64Str`, `tripleDESEncodeBase64Str`, `aesEncodeToString`, `aesEncodeToByteArray`, `desDecodeToString`: no exact `java.<name>` production-source hit in this pass. This does not remove the separate direct DES/DESede Cipher caller requirement in §24.28.
- `readFile`, `getSource`, `getBatchContext`, `getSourceNavigationContext`: no matching production-source hit found. These include internal runtime/context methods and are not to be treated as safely removable based on search absence.
- `getFile` appeared in four compatibility/mock-manifest files, not an inspected production source. Upstream returns a File-shaped object subject to the upstream ClassShutter; host intentionally returns a source-scoped `RhinoSourceFileHandle`. Keep this as a typed host/file-boundary contract, not a reason to expose raw File.
- `getTag` appeared in two compatibility backup files, not an inspected production source; host returns its source tag. Classify as dynamic context API until wrapper reachability is fully closed.
- `getThemeConfig` and `getThemeMode` search results were test/advanced-inventory fixtures, not production source files. The help does document read/theme configuration APIs; absence of current public source callers does not close the documented API gap.
- `java.getCookie`, `java.getZipStringContent`, `java.toNumChapter`, `java.queryTTF`, `java.replaceFont` and `java.aesBase64DecodeToString` must not be placed in the “no production caller” bucket.

### Semantic re-checks that narrow earlier conclusions

- **queryTTF local-file correction:** the pinned implementation at `JsExtensions.kt` lines 1063–1095 only treats HTTP(S) strings as URLs; every other String is passed to `base64DecodeToByteArray`, and ByteArray is accepted directly. `StringExtensions.isAbsUrl()` at pinned blob `694fe6d8f3a815261e034c1341109aae1b18bf2c`, lines 38–41, recognizes only HTTP(S). Thus a plain path such as `/storage/emulated/0/Download/...` in the SourceGo sample is **not** a valid supported local-file input at this pin despite the queryTTF KDoc saying “local file”; the remote URL call is valid evidence, while the plain local-path call is an upstream documentation/implementation inconsistency and must not be made a parity requirement. Badcat's source-file-token support is a host extension, not upstream parity.
- **toNumChapter algorithm:** pinned AppPattern `titleNumPattern` is exactly `(第)(.+?)(章)`; pinned StringUtils `stringToInt` converts full-width characters/spaces, tries integer parsing and falls back to the Chinese numeral parser. The host uses the same pattern, full-width conversion and equivalent numeral map/accumulator. Static inspection indicates the common algorithm is closely mirrored; no differential fixture has yet executed, so retain “source caller confirmed; common algorithm statically aligned; edge-case parity unverified,” not “tested exact.”
- **getZipStringContent:** pinned upstream calls `getZipByteArrayContent`, obtains bytes through `AnalyzeUrl(url, source=getSource(), coroutineContext=context)` for HTTP(S) or hex-decodes input, then reads the named entry with `ZipInputStream`; the two-argument string overload uses `EncodingDetect`, the three-argument overload uses the requested charset. Host has both string overloads and a real ZIP entry reader, but downloads via the injected `httpGet` and uses `SourceLibArchiveReader`/bounded archive handling (8 MiB compressed, 16 MiB expanded entry). This is **partial** until differential tests cover source headers/cookies/redirect/rate-limit, duplicate entry names, missing entries, charset detection, malformed ZIPs and limit behavior.
- **aesBase64DecodeToString:** upstream delegates to Hutool-backed `createSymmetricCrypto(...).decryptStr(str)`; host has the same named method and delegates to its bounded crypto implementation. The real source examples prove the AES/CBC call shape, but the method-specific source fixture and output/null/invalid-padding differential remain open.
- **HMacHex:** upstream uses Hutool `HMac(algorithm, key.toByteArray()).digestHex(data)`; host uses JCA Mac after normalizing the algorithm and restricts it to HmacSHA1/256/384/512. The HmacSHA1 source caller is within the host allowlist; exact lowercase/alias/provider and output parity still need a known-vector differential test.

### Production runtime and cookie wiring correction

This section **supersedes the “runtime-to-cookie-jar wiring remains unproven” wording in §24.21 for the main reading-engine path**:
- Current app tree at pre-write HEAD `3d81abbf86a8114325f7401968c0edf6ef7beee7` contains production `BadcatReadingEngineFactory.kt` blob `cd8946aaaffacef3d2a748a537736cf2798c96c8`. `SourceSearchActivity` blob `67e60a5fd6ffe91ed6c4d446f379f8bbc96285f0` and `SourceBookActivity` blob `15c0585b3a4f6dd44f49792563601caaa39158a0` both construct this factory; it is not merely a test-only runtime.
- The factory creates a single `SharedPreferencesSourceCookieJar`, injects its `getCookie`, `setCookie`, `replaceCookie`, `removeCookie`, keyed removal and source-clear callbacks into the active `RhinoSourceScriptRuntime`, and wires login-cookie import/removal callbacks. It also supplies the source login session store, HTTP callbacks, source key-value store and file cache. The main reading-engine runtime therefore does not use the constructor's empty/throwing defaults.
- `SharedPreferencesSourceCookieJar.getCookie(url)` calls `readBoundedMerged(url)`; `readMerged` merges persistent preferences and the process session-cookie store by domain (cookie jar blob `2f389d365128529813020b2bd81e242298c298a5`, lines 131–155 and 284–300). The main engine's `java.getCookie(tag[,key])` callback therefore reads the merged cookie header and extracts a requested key in the host bridge. `SourceLoginSessionRequestClient` separately merges saved source login headers into requests only when `shouldAttachSourceLoginHeader(sourceId,url)` permits it (login-session blob `47707cce281d9cdaf79da53d16237334261f69a0`, lines 244–265).
- **Remaining separate-path caveat:** `SourceCatalogActivity` creates its own temporary `RhinoSourceScriptRuntime` for `LegadoSourceLoginUiV2Service` and passes reader/remover/clearer callbacks, but does not pass the main factory's cookie writer/replacer/login-cookie writer callbacks. Any login script mutation through that utility-only runtime may therefore still hit the runtime's default throwing callbacks. The main SourceSearch/SourceBook reading-engine path is fully wired as described; do not generalize the utility screen's narrower constructor to the main engine.
- `BadcatSourceHttpGatewayFactory` has a fallback runtime constructor with fewer injected callbacks, but `BadcatReadingEngineFactory` passes its already-wired active runtime into it. The production main engine path therefore uses the complete factory wiring rather than the fallback.

### Updated classification and next actions

- **Active, undocumented or under-documented callers:** `java.getCookie`, `getZipStringContent`, `toNumChapter`, `queryTTF`, `replaceFont`, `aesBase64DecodeToString`. Preserve these as explicit matrix rows and add exact source-rule fixtures; do not rely on help-file enumeration alone.
- **Implemented but partial/differential-open:** getZipStringContent (archive/transport semantics), queryTTF (source cache identity and input boundary), replaceFont (glyph edge cases), aesBase64DecodeToString (crypto vectors/error behavior), HMacHex (algorithm/provider aliases), toNumChapter (edge-case vectors). getCookie main-engine wiring is now evidenced, but its behavior still needs source-session and WebView differential tests.
- **Still open documented API:** read/theme configuration methods; source UI/browser/verification ports; complete source/cookie/cache callbacks and object identity; all remaining public overloads, returned object ABI, and NativeBaseSource dynamic dispatch. No source caller hit does not erase a documented method contract.
- **No implementation/test change:** this was source/help/production-callsite inspection only. No test command was run. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.
- **Next ordered action:** complete the remaining 104-name method matrix by marking each declaration as (a) documented and active, (b) undocumented with an inspected source caller, (c) documented but no caller found, (d) internal/context-only but dynamically reachable, or (e) host-only extension. Then attach exact overload/return/nullability and differential-test requirements. Do not implement in this read-only task.


## 24.30 Missing upstream UI/browser methods are active BookSource callers — Badcat-native adapter required (2026-11-24)

### Source caller evidence

The 17 upstream public methods absent from the main Badcat `RhinoSafeSourceJavaExtensions` class are not all unused UI helpers. A second public BookSource search found active callers for several browser/captcha APIs:

| Upstream method | GitHub code-search discovery snapshot | Inspected concrete source field and calls |
|---|---:|---|
| `webView` | 94 | `Mina-kk/yuedu-MCP@e129d35e7cba08f8dcacc2cf05dc3e626b5965dd`, shard `f_0896.json` blob `00966ee548132c552ec916d344438b032f33892f`: bookSourceComment and searchUrl; `DowneyRem/PixivSource@79251ad67dc986f7554f69cd153b927be40955ba`, `btsrk.json` blob `77479384eb227a2d4bac2a432b2fd5409513767d`: loginUrl; `mumuceo/file01@8429f9d6e03112f77da35403dee2682edc67217b`, `202604/4078_4b35feff2d8a476596729852b040635e.json` blob `36c8697327cd1612f531a3dbe9a161f525fa9324`: ruleContent. |
| `startBrowserAwait` | 217 | DowneyRem `btsrk.json` loginUrl; `Luoyacheng/yuedu@3ffc017cf6abb7151a127ba1610302ecfb617c96`, `8x.json` blob `6642f072a129df896b7d1b55443ccb951b2958e8` loginUrl; mumuceo 202604 source uses it in book-info init, TOC and content rules. |
| `startBrowser` | 125 | DowneyRem `btsrk.json` jsLib/loginUrl; Luoyacheng `8x.json` jsLib/loginUi/loginUrl; mumuceo 202604 source jsLib/loginUrl/content. |
| `getVerificationCode` | 70 | `Luoyacheng/yuedu@3ffc017cf6abb7151a127ba1610302ecfb617c96`, `qdzs.json` blob `6e417fe9e16b04fb9a4a8d1b9d3969fbd1a9033d`: ruleExplore.bookList; `8x.json`: exploreUrl and loginUrl. |
| `showBrowser` | 4 | Luoyacheng `qdzs.json` exploreUrl. |
| `openUrl` | 10 | Luoyacheng `8x.json` loginUrl; `mumuceo/file01` 202604 source jsLib. |
| `webViewGetSource`, `webViewGetOverrideUrl` | 0 exact matches | No direct caller found in this pass; retain as upstream public API pending full caller closure. |
| `openVideoPlayer` | 0 | No public BookSource caller found in this pass; classify as optional media/UI API, not core novel source acceptance evidence. |

Counts are GitHub Code Search discovery counts, not unique source counts; repositories include mirrors and some search results may be fixtures. The inspected JSON fields above were parsed structurally; no script was executed and no credential-bearing source values were copied.

### What these APIs do and why LoginUiV2 is not equivalent

- `webView(html,url,js[,cacheFirst])` runs a real WebView off the main thread, applies source headers/tag, optionally uses cache, and returns JS-selected content or page source. It is called from ordinary source fields such as searchUrl and ruleContent, not only a login dialog.
- `startBrowser(url,title[,html])` opens an interactive source verification browser. `startBrowserAwait(url,title[,refetchAfterSuccess[,html]])` blocks the source flow until user verification, then returns the verified response or refetches through AnalyzeUrl. These methods can occur inside loginUrl, book-info, TOC and content rules.
- `getVerificationCode(imageUrl)` opens an image captcha prompt and synchronously returns the user's text; source callers include exploreUrl/ruleExplore.
- `showBrowser(url[,html,preloadJs,config])` opens an interactive embedded browser asynchronously. `openUrl(url[,mimeType])` opens an external app/browser route with source-origin confirmation.
- Existing `LegadoSourceLoginUiV2Service` / `SourceLoginUiV2Activity` implement the structured `loginUi` form flow (text/password/select/toggle/button and login result). The service source explicitly says it does not emulate WebView; it does not implement the above APIs. It is therefore not a substitute for these browser/captcha behaviors.
- `BadcatReadingEngineFactory` wires the main script runtime's HTTP/cookie/session ports but does not pass WebView, browser, captcha, or external-open callbacks. The `BadcatSourceHttpGatewayFactory` source comment also explicitly states full bodyJs, WebView and upstream Java host APIs remain unsupported.

### No-Legado-UI constraint and required architecture

Do not transplant Legado's BottomWebViewDialog, BackstageWebView, SourceVerificationHelp, SourceHelp or player activities. The product requirement is to preserve Badcat's existing UI. However, do not silently omit active source APIs either. The compatibility path must be a **Badcat-native host port** with source-origin identity and explicit lifecycle:
- `webView`: a Badcat-owned isolated WebView service/component with source-scoped cookies/headers/user-agent, safe navigation policy, cache behavior, JS evaluation result, background-thread blocking bridge, lifecycle cancellation and bounded page/script/response time.
- `startBrowserAwait`: a cancellable request/response rendezvous with a Badcat-native verification browser; on success return the verified URL/body or perform the requested refetch through the same source request gateway; persist/import only the source's permitted cookies.
- `startBrowser` and `showBrowser`: asynchronous Badcat-owned browser surface; source name/origin must be visible and external navigation policy enforced.
- `getVerificationCode`: Badcat-native captcha image prompt and bounded user-input wait; cancellation/timeout must unwind the source execution rather than leave a worker blocked.
- `openUrl`: Badcat confirmation/intent dispatcher; no arbitrary intent or untrusted custom scheme without explicit policy.
- The script runtime must never block the Android main thread. UI waits must be cancellable when the Activity/engine is destroyed or the user cancels. Enforce HTTP(S) and redirect policy, source attribution, cookie partitioning, WebView file/content access restrictions, JS bridge allowlist, resource budgets and timeout. No raw Legado UI class should be exposed through Packages.

### Revised classification of the 17 missing method names

- **Active source caller / source-compatibility blocker until Badcat-native port:** `webView`, `startBrowser`, `startBrowserAwait`, `getVerificationCode`, `showBrowser`, `openUrl`.
- **No direct public caller found in this pass; keep in matrix:** `webViewGetSource`, `webViewGetOverrideUrl`, `openVideoPlayer`.
- **Reader/theme configuration APIs, not observed in public BookSource callers:** `getReadBookConfig`, `getReadBookConfigMap`, `getThemeConfig`, `getThemeConfigMap`, `getThemeMode`. Do not import Legado Reader settings/UI; decide whether to map to Badcat's own settings or return a documented source-safe result.
- **Reader refresh control APIs, no production BookSource caller found:** `refreshBookInfo`, `refreshBookToc`, `refreshContent`. Keep classified as UI/engine callback surface, not as a required UI transplant. Search hits for refreshBookInfo/refreshContent were from a research dataset, not an inspected live source corpus.
- **Media UI:** `openVideoPlayer` has no current source caller in the scan; retain as optional/out-of-core unless new evidence appears.

### Decision

The 17 missing names cannot be treated as 17 equally unused methods. Six have direct public BookSource callers and require Badcat-native UI/browser/captcha adapters for broad source compatibility. This remains a **read-only audit finding**: no UI, runtime, Activity, WebView, or source implementation was changed. Stage-0 remains in progress; Stage-1 remains NO-GO.


## 24.31 Normalized 104-name method matrix + compiled ABI and response-object mismatches (2026-11-24)

### Scope and counts

- This table covers the direct public method declarations in pinned `JsExtensions.kt` + `JsEncodeUtils.kt` against direct public declarations in the host's `RhinoSafeSourceJavaExtensions` class body. It does **not** claim to be the entire upstream source-object surface: `AnalyzeRule`, `BaseSource`, `WebJsExtensions`, returned Java/Jsoup objects and nested host facades add further members.
- Direct source declaration counts: upstream **148 declarations / 104 distinct names**; host **150 declarations / 110 distinct names**. Host has **17 upstream names missing** and **23 additional direct names**. Some additional names belong to AnalyzeRule-style or safe host helper APIs; they must not be counted as 23 upstream parity gains.
- The `jsHelp` flag is a lexical presence check for `java.<name>` in the complete pinned help file. “no” means the method is not named that way in help; it does not mean it is not dynamically callable or used by real sources.
- Caller status is intentionally conservative: only methods with inspected source examples are called active; internal methods and fixture-only methods are labelled; remaining methods stay “caller scan pending” rather than being assumed unused.
- Host overload counts below are Kotlin source declarations. The host bytecode test `RhinoSourceJsAbiBytecodeTest` blob `f3dc5c9d0d2b8edeaffa9b8500110f4e716439fd` verifies that `@JvmOverloads` creates Java-visible overloads for ajaxAll and ajaxTestAll. The test explicitly says it does not claim upstream bytecode parity.

### Complete name/caller matrix

| Method | Upstream declarations | Host direct declarations / ABI note | jsHelp `java.*` | Caller classification |
|---|---:|---:|:---:|---|
| `HMacBase64` | 1 | 1 | yes | caller scan pending |
| `HMacHex` | 1 | 1 | yes | active source HmacSHA1 caller (4 search hits) |
| `aesBase64DecodeToByteArray` | 1 | 1 | no | caller scan pending |
| `aesBase64DecodeToString` | 1 | 1 | no | active AES content callers (70) |
| `aesDecodeArgsBase64Str` | 1 | 1 | no | caller scan pending |
| `aesDecodeToByteArray` | 1 | 1 | no | caller scan pending |
| `aesDecodeToString` | 1 | 1 | no | caller scan pending |
| `aesEncodeArgsBase64Str` | 1 | 1 | no | caller scan pending |
| `aesEncodeToBase64ByteArray` | 1 | 1 | no | caller scan pending |
| `aesEncodeToBase64String` | 1 | 1 | no | caller scan pending |
| `aesEncodeToByteArray` | 1 | 1 | no | caller scan pending |
| `aesEncodeToString` | 1 | 1 | no | no exact public-source caller found in this pass |
| `ajax` | 2 | 2 | yes | caller scan pending |
| `ajaxAll` | 2 | 1 source declaration + @JvmOverloads JVM overload | yes | caller scan pending |
| `ajaxTestAll` | 2 | 1 source declaration + @JvmOverloads JVM overload | yes | caller scan pending |
| `androidId` | 1 | 1 | yes | caller scan pending |
| `base64Decode` | 3 | 3 | yes | caller scan pending |
| `base64DecodeToByteArray` | 2 | 2 | yes | caller scan pending |
| `base64Encode` | 2 | 2 | yes | caller scan pending |
| `bytesToStr` | 2 | 2 | yes | caller scan pending |
| `cacheContent` | 1 | 1 | yes | caller scan pending |
| `cacheFile` | 2 | 2 | yes | caller scan pending |
| `connect` | 3 | 3 | yes | caller scan pending |
| `createAsymmetricCrypto` | 1 | 1 | yes | caller scan pending |
| `createSign` | 1 | 1 | yes | caller scan pending |
| `createSymmetricCrypto` | 4 | 4 | yes | caller scan pending |
| `deleteFile` | 1 | 1 | yes | caller scan pending |
| `desBase64DecodeToString` | 1 | 1 | no | caller scan pending |
| `desDecodeToString` | 1 | 1 | no | no exact public-source caller found in this pass |
| `desEncodeToBase64String` | 1 | 1 | no | caller scan pending |
| `desEncodeToString` | 1 | 1 | no | caller scan pending |
| `digestBase64Str` | 1 | 1 | yes | caller scan pending |
| `digestHex` | 1 | 1 | yes | caller scan pending |
| `downloadFile` | 2 | 2 | yes | caller scan pending |
| `encodeURI` | 2 | 2 | yes | caller scan pending |
| `get` | 2 | 3 (2 JsExtensions + 1 AnalyzeRule-style key/URL get) | yes | caller scan pending |
| `get7zByteArrayContent` | 1 | 1 | no | no exact public-source caller found in this pass |
| `get7zStringContent` | 2 | 2 | no | no exact public-source caller found in this pass |
| `getBatchContext` | 1 | 1 | no | internal batch context; no external source hit |
| `getCookie` | 2 | 2 | no | active login/search callers (89 search hits) |
| `getFile` | 1 | 1 | no | test/compatibility fixtures only |
| `getRarByteArrayContent` | 1 | 1 | no | no exact public-source caller found in this pass |
| `getRarStringContent` | 2 | 2 | no | no exact public-source caller found in this pass |
| `getReadBookConfig` | 1 | 0 | yes | no exact public-source caller found in this pass |
| `getReadBookConfigMap` | 1 | 0 | yes | no exact public-source caller found in this pass |
| `getSource` | 1 | 1 | no | internal source-context object; no external source hit |
| `getSourceNavigationContext` | 1 | 1 | no | internal navigation coroutine context; no external source hit |
| `getTag` | 1 | 1 | no | internal source tag; only backup fixtures found |
| `getThemeConfig` | 1 | 0 | yes | test fixture only |
| `getThemeConfigMap` | 1 | 0 | yes | no exact public-source caller found in this pass |
| `getThemeMode` | 1 | 0 | no | test fixture only |
| `getTxtInFolder` | 1 | 1 | yes | caller scan pending |
| `getVerificationCode` | 1 | 0 | yes | active explore/login captcha callers (70) |
| `getWebViewUA` | 1 | 1 | yes | caller scan pending |
| `getZipByteArrayContent` | 1 | 1 | no | coverage fixture only; used internally by active getZipStringContent |
| `getZipStringContent` | 2 | 2 | no | active ZIP detail.json caller (16) |
| `head` | 2 | 2 | yes | caller scan pending |
| `hexDecodeToByteArray` | 1 | 1 | yes | caller scan pending |
| `hexDecodeToString` | 1 | 1 | yes | caller scan pending |
| `hexEncodeToString` | 1 | 1 | yes | caller scan pending |
| `htmlFormat` | 2 | 2 | yes | caller scan pending |
| `importScript` | 1 | 1 | yes | caller scan pending |
| `lock` | 2 | 2 | yes | caller scan pending |
| `log` | 1 | 1 | yes | caller scan pending |
| `logType` | 1 | 1 | yes | caller scan pending |
| `longToast` | 1 | 1 | yes | caller scan pending |
| `md5Encode` | 1 | 1 | yes | caller scan pending |
| `md5Encode16` | 1 | 1 | yes | caller scan pending |
| `openUrl` | 2 | 0 | yes | active login/jsLib caller (10) |
| `openVideoPlayer` | 2 | 0 | yes | no exact public-source caller found in this pass |
| `post` | 2 | 2 | yes | caller scan pending |
| `queryBase64TTF` | 1 | 1 | no | no exact public-source caller found in this pass |
| `queryTTF` | 2 | 2 | no | active remote-font caller (3); local-path example invalid upstream |
| `randomUUID` | 1 | 1 | yes | caller scan pending |
| `readFile` | 1 | 1 | no | no exact public-source caller found in this pass |
| `readTxtFile` | 2 | 2 | yes | caller scan pending |
| `refreshBookInfo` | 1 | 0 | yes | research dataset only |
| `refreshBookToc` | 1 | 0 | yes | no exact public-source caller found in this pass |
| `refreshContent` | 1 | 0 | yes | research dataset only |
| `replaceFont` | 2 | 2 | no | active font glyph mapping caller (3) |
| `s2t` | 1 | 1 | yes | caller scan pending |
| `showBrowser` | 4 | 0 | yes | active explore browser caller (4) |
| `singleFlight` | 2 | 2 | yes | caller scan pending |
| `startBrowser` | 2 | 0 | yes | active jsLib/login/content callers (125) |
| `startBrowserAwait` | 3 | 0 | yes | active login/content/TOC callers (217) |
| `strToBytes` | 2 | 2 | yes | caller scan pending |
| `t2s` | 1 | 1 | yes | caller scan pending |
| `tick` | 1 | 1 | yes | caller scan pending |
| `timeFormat` | 1 | 2 | yes | caller scan pending |
| `timeFormatUTC` | 1 | 2 | yes | caller scan pending |
| `toNumChapter` | 1 | 1 | no | active chapter normalization caller (44) |
| `toURL` | 2 | 2 | yes | caller scan pending |
| `toast` | 1 | 1 | yes | caller scan pending |
| `tripleDESDecodeArgsBase64Str` | 1 | 1 | no | no exact public-source caller found in this pass |
| `tripleDESDecodeStr` | 1 | 1 | no | no exact public-source caller found in this pass |
| `tripleDESEncodeArgsBase64Str` | 1 | 1 | no | no exact public-source caller found in this pass |
| `tripleDESEncodeBase64Str` | 1 | 1 | no | no exact public-source caller found in this pass |
| `un7zFile` | 1 | 1 | yes | caller scan pending |
| `unArchiveFile` | 1 | 1 | yes | caller scan pending |
| `unrarFile` | 1 | 1 | yes | caller scan pending |
| `unzipFile` | 1 | 1 | yes | caller scan pending |
| `webView` | 2 | 0 | yes | active search/login/content callers (94) |
| `webViewGetOverrideUrl` | 3 | 0 | yes | no exact public-source caller found in this pass |
| `webViewGetSource` | 3 | 0 | yes | no exact public-source caller found in this pass |

### Confirmed signature, return-type and object-shape deltas

1. **ajax timeout overload is missing.** Pinned upstream exposes `ajax(url: Any)` and `ajax(url: Any, callTimeout: Long?)`. Host exposes two one-argument methods (`ajax(urlAndOptions: String)` and `ajax(url: Any?)`) and no two-argument callTimeout overload. This remains a real method-signature gap even though the declaration counts match.
2. **ajaxAll/ajaxTestAll compiled overloads are present.** Host Kotlin source uses `@JvmOverloads`; its ABI test confirms `ajaxAll` parameter counts {1,2} and `ajaxTestAll` {2,3}. Do not misclassify these as missing just because the source contains one declaration with a default parameter.
3. **get overload is a union of two upstream surfaces.** Host has one-argument `get(keyOrUrl)` from AnalyzeRule-style persistent-variable/URL behavior plus the two JsExtensions HTTP-response overloads. The extra one-argument form is not an accidental conflict; preserve its dual key/URL semantics.
4. **timeFormat/timeFormatUTC:** upstream takes Long; host additionally exposes String overloads alongside Number overloads. These extra coercion forms are host conveniences, not evidence of exact upstream ABI. Keep numeric timestamp behavior and timezone/format defaults under differential tests.
5. **cacheContent return value:** pinned upstream `cacheContent(chapter, content)` has Unit/undefined script semantics; the host ABI test asserts the host method returns Boolean. This is a return-value mismatch and can be observed by scripts that test the result.
6. **getSource return type:** upstream returns its BaseSource object; host ABI test asserts the host returns a Map. This intentionally avoids exposing the full Legado object, but it is not object-identity/API parity. Internal-only/no-current-source-caller status does not remove the dynamic surface difference.
7. **getBatchContext:** upstream exposes the batch content context; host ABI test only establishes a generic Any return. Exact runtime type, nullability and methods are not matched/proven.
8. **getFile:** upstream returns java.io.File, which is denied by the pinned ClassShutter. Host returns a source-scoped `RhinoSourceFileHandle`; keep the safe facade and test the allowed file operations rather than exposing File.
9. **HTTP response shape:** upstream JsExtensions `get/head/post` return Jsoup `Connection.Response`; host returns `RhinoSourceHttpResponse`. The host wrapper has body, code/statusCode, message, callTime, url, header, headers, raw, parse, select and charset. A public source caller in `yuedu520/yuedu@b5524f41d80fe4071c3d695fd1d6990cd6de97aa`, `250427.json` blob `6d2fef386052ef35d1eebeae374e6efb3476aee0`, calls `bodyAsBytes()` in ruleContent; the host wrapper has no `bodyAsBytes()`. The transport response retains original `bodyBytes` in `SourceHttpResponse` (SourceRuntime.kt blob `c99001865c1c4eaeae7979af7d5c85daabe9f09c`, lines 27–39), so the byte data exists below the wrapper but is not exposed to Rhino. This is an active source-compatibility gap.
10. **Response header map shape:** host `headers()` currently returns `Map<String,List<String>>`, while Jsoup Response distinguishes single-value `headers()` from `multiHeaders()`. The host wrapper does not expose `multiHeaders()`; exact return shape and duplicate-header behavior need to be mapped before claiming Response parity.
11. **NativeBaseSource reachability:** pinned `NativeBaseSource` blob `8313dedf737fe42cb4381535f6e74760c4f31786` wraps a Java object through `CatchableNativeJavaObject` and delegates property/method lookup to Rhino's Java wrapper; it is not restricted to `@JavascriptInterface` methods. Therefore public, non-annotated methods and overloads are part of the dynamic compatibility surface. The host's direct-object ABI test likewise invokes methods without requiring Android WebView annotations.

### Matrix result

- 17 absent methods include six active source-facing UI/browser/captcha callers from §24.30; these require Badcat-native adapters, not Legado UI imports.
- Active undocumented helper callers remain: getCookie, getZipStringContent, toNumChapter, queryTTF, replaceFont, aesBase64DecodeToString. HMacHex is documented and active.
- Response `bodyAsBytes()` is a new confirmed active missing member.
- The method-name matrix is now complete for the 104 JsExtensions/JsEncodeUtils names; exact parameter/return/nullability and dynamic returned-object method matrix is still **incomplete**. Do not infer completion from the name table.
- No application source or tests changed; no tests were run. Stage-0 remains in_progress/not accepted; Stage-1 remains NO-GO.


## 24.32 Jsoup Connection.Response inherited ABI — exact host mismatch and bounded-byte evidence (2026-11-24)

### Pinned contract and return path

- **Fixed upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. Its version catalog pins **Jsoup 1.23.2** (`gradle/libs.versions.toml`, blob `88170041bfea4b98b3269955f5db3704691134aa`, line 56).
- **BookSource-facing return types:** pinned `JsExtensions.get`, `head` and `post` return `org.jsoup.Connection.Response`; source declarations and overloads are at `JsExtensions.kt` blob `f6808edd2a0d312e8bd13649935dea444cb82581`, lines 575, 602 and 629. Each has the two-argument/default-timeout and explicit-timeout form.
- **Response is a transitive public Java surface:** Jsoup 1.23.2 `Connection.Response extends Base<Response>` (`Connection.java` blob `ad92c46c8530cc77765e8f59d3d1df7e01c50dbc`, lines 545 and 936). Because the value is returned directly from public Rhino-visible JsExtensions methods, inherited public methods are part of the script-visible contract, not merely internal Jsoup details.

### Exact inherited method contract

- **Base<Response> identity and request metadata:** `url(): URL`, `url(URL): Response`, `method(): Connection.Method`, `method(Method): Response`.
- **Header methods:** `header(name): String?` (case-insensitive; multiple values comma-joined); `headers(name): List<String>`; `header(name,value): Response`; `addHeader(name,value): Response`; `hasHeader(name): boolean`; `hasHeaderWithValue(name,value): boolean`; `removeHeader(name): Response`; `headers(): Map<String,String>` (first value only); `multiHeaders(): Map<String,List<String>>`.
- **Cookie methods:** `cookie(name): String?`; `cookie(name,value): Response`; `hasCookie(name): boolean`; `removeCookie(name): Response`; `cookies(): Map<String,String>`.
- **Response-specific methods:** `statusCode()`, `statusMessage()`, `charset()`, `charset(String)`, `contentType()`, `parse(): Document`, `readBody()`, `body()`, `bodyAsBytes(): byte[]`, `readFully(): Response`, deprecated `bufferUp(): Response`, `bodyStream(): BufferedInputStream`, and `streamParser(): StreamParser`. Definitions are in the pinned Jsoup interface blob above, lines 936 onward. Implementation details for first-value `headers()`, all-value `multiHeaders()`, case-insensitive header lookup and cookie-map operations are in Jsoup `HttpConnection.java` blob `4e4a21066db6ac35faa1a62894c381ba6c5a393b`, lines 546–600.

### Badcat implementation observed at this exact branch snapshot

- Host wrapper: `RhinoSafeSourceJavaExtensions.kt` blob `6a00e649895c4c1d223b768c6b288632ec033de9`, `RhinoSourceHttpResponse` begins at line 2055; `RhinoSourceRawResponse` begins at line 2081.
- Actual host methods on `RhinoSourceHttpResponse`: `body/getBody`, `code/statusCode`, `isSuccessful`, `message`, `callTime/getCallTime`, `url/getUrl` (String), `header(name)`, `headers(name)` (both return the first value or empty string), `headers()` (returns `Map<String,List<String>>`), `raw/getRaw`, `parse()` (returns `LegadoSourceElement`), `select`, `charset()` and `toString`. `raw()` is a host-specific metadata facade, not an implementation of Jsoup's Response body/stream API.
- **Confirmed mismatches:** host `header(name)` does not comma-join duplicate values and returns empty string rather than upstream nullable result; host `headers(name)` returns String instead of List<String>; host no-argument `headers()` returns the multi-value map instead of Jsoup's first-value map; `multiHeaders()` is absent. The host has no Response cookie methods, header mutation/presence/removal methods, `url(URL)`, `method()`, `method(Method)`, `charset(String)`, `contentType()`, `readBody()`, `bodyAsBytes()`, `readFully()`, `bufferUp()`, `bodyStream()` or `streamParser()`. Its `url()` is a String rather than a `java.net.URL`; `parse()` returns a host element wrapper rather than Jsoup Document. Same-name helper methods therefore do not establish Response ABI parity.
- **Active real-source proof:** the public BookSource snapshot `yuedu520/yuedu@b5524f41d80fe4071c3d695fd1d6990cd6de97aa`, file `250427.json`, blob `6d2fef386052ef35d1eebeae374e6efb3476aee0`, calls `bodyAsBytes()` from `ruleContent`. This is not a hypothetical unused Jsoup member; its absence is an active source compatibility blocker.
- **Byte preservation below the wrapper:** host `SourceHttpResponse` declares nullable `bodyBytes: ByteArray?` (`SourceRuntime.kt` blob `c99001865c1c4eaeae7979af7d5c85daabe9f09c`, line 27). `OkHttpSourceRequestClient.readResponse` reads response octets under `maxResponseBytes`, builds the decoded body and retains the original bytes in `bodyBytes` (`OkHttpSourceRequestClient.kt` blob `25de6e42eec8146629060a6c1a142fa0befeacb2`, line 265 onward). Thus production transport has the byte data, but the Rhino response facade does not expose it. The nullable DTO field also means injected/custom transports and fixtures must explicitly define missing-byte behavior; re-encoding decoded text is not a byte-fidelity substitute.
- **Existing test boundary:** `RhinoSourceScriptRuntimeTest.exposesLegadoResponseMessageHeadersAndSafeRawRequestMetadata` (`RhinoSourceScriptRuntimeTest.kt` blob `803831e73835409155629a3255776cc3c808a8cf`, line 1695) checks a two-value header fixture through the current host-specific `headers()` and `headers(name)` shapes. It does not compare against Jsoup 1.23.2 and does not cover `bodyAsBytes`, `multiHeaders`, comma-joined header behavior, nullable missing headers, cookie helpers, method/URL objects, or stream/read lifecycle. It is host-local behavior evidence, not parity evidence.
- **Safety boundary:** do not expose unbounded raw streams or parser objects merely to close a name gap. Any future adapter must preserve the original bounded bytes, cancellation and resource ownership, and must explicitly classify stream/parser operations that cannot safely be reproduced. No production or test implementation is authorized by §26.13.

### Classification and next closure

- `bodyAsBytes()`: **active blocker; bytes exist in production transport but are not Rhino-visible**.
- `headers()/headers(name)/header(name)/multiHeaders()`: **confirmed signature and semantic mismatch**.
- Cookie/header mutation and presence helpers, URL/method object identity, charset setter, content type and read/stream methods: **missing or unverified inherited Response surface**.
- No source/test command was run in this documentation audit. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**. No complete Jsoup Response compatibility claim is made.
- **Next:** include this inherited Response/Base surface in the signature-level matrix; inspect every remaining JsExtensions method/overload and source-visible returned object; identify active BookSource callers and define differential fixtures for bytes, duplicate/missing headers, cookie maps, URL/method, nullability and body-consumption lifecycle. Keep this read-only until a separately registered implementation task authorizes exact files and safe facade semantics.


## 24.33 Inherited JsEncodeUtils — 30 exact source-signature comparisons (2026-11-24)

### Inheritance and source declaration inventory

- Pinned `JsExtensions` is an interface extending `JsEncodeUtils`, not a standalone class: `JsExtensions.kt` blob `f6808edd2a0d312e8bd13649935dea444cb82581`, line 127. Therefore the inherited methods are part of the same BookSource `java` bridge surface.
- Refreshed pinned source: `JsEncodeUtils.kt` blob `c75b61650db2abdd64a09d2bd8340ef14c2947b3`, 518 lines. It contains **30 public declarations / 27 distinct method names (three additional overload declarations)**. Host source: `RhinoSafeSourceJavaExtensions.kt` blob `6a00e649895c4c1d223b768c6b288632ec033de9`; the main bridge contains all 27 names and matching source-level parameter/return type shapes.
- This is a source declaration comparison only. It does not prove the upstream/host pre-R8 JVM descriptors, Kotlin metadata, generated default overloads, annotation parity or behavioral equivalence.

### Type/return signature groups

| Upstream method declarations (pinned source lines) | Host declarations (bridge source lines) | Signature result |
|---|---:|---|
| `md5Encode(String): String`, `md5Encode16(String): String` (22–27) | 1282–1302 | Same parameter and return types; parameter identifier differs. |
| `createSymmetricCrypto(String, ByteArray?, ByteArray?): SymmetricCrypto`; `createSymmetricCrypto(String, ByteArray): SymmetricCrypto`; `createSymmetricCrypto(String, String): SymmetricCrypto`; `createSymmetricCrypto(String, String, String?): SymmetricCrypto` (45–68) | 1337–1373 | Four overload shapes present with matching source types/nullability. |
| `createAsymmetricCrypto(String): AsymmetricCrypto`, `createSign(String): Sign` (80–87) | 1374–1399 | Matching source types/return classes. |
| AES decode/encode family: `aesDecodeToByteArray`, `aesDecodeToString`, `aesDecodeArgsBase64Str`, `aesBase64DecodeToByteArray`, `aesBase64DecodeToString`, `aesEncodeToByteArray`, `aesEncodeToString`, `aesEncodeToBase64ByteArray`, `aesEncodeToBase64String`, `aesEncodeArgsBase64Str` (106–280) | 1400–1589 | Matching four-/five-String parameter shapes and nullable ByteArray/String return types. |
| DES family: `desDecodeToString`, `desBase64DecodeToString`, `desEncodeToString`, `desEncodeToBase64String` (296–329) | 1590–1644 | Matching four-String parameter and nullable String return types. |
| 3DES family: `tripleDESDecodeStr`, `tripleDESDecodeArgsBase64Str`, `tripleDESEncodeBase64Str`, `tripleDESEncodeArgsBase64Str` (351–432) | 1645–1740 | Matching five-String parameter and nullable String return types. |
| `digestHex(String, String): String`, `digestBase64Str(String, String): String`, `HMacHex(String, String, String): String`, `HMacBase64(String, String, String): String` (456–506) | 1285–1301 | Matching source parameter/return types; parameter identifiers and formatting differ. |

- The six declaration strings whose parameter identifiers differ are `md5Encode`, `md5Encode16`, `digestHex`, `digestBase64Str`, `HMacHex` and `HMacBase64`. Parameter names do not change positional Rhino calls or JVM method descriptors; the type and nullability shapes match in the inspected Kotlin source.
- No inherited JsEncodeUtils method name is missing from the host main bridge in this source-level pass. Do not use this as evidence that the other 74 JsExtensions names, nested return objects or generated JVM ABI are closed.

### Behavioral classification remains partial

- Existing §24.22 evidence remains controlling: host crypto factories/helpers impose bounded input/output and allow-listed digest/HMAC algorithms and use host wrappers; these safeguards and supported algorithm sets differ from the complete Hutool/upstream object behavior. Selected host tests cover common symmetric, legacy AES, DES/3DES, RSA/sign and size-limit cases, not every overload, algorithm provider, null/error edge or inherited Hutool method.
- Active BookSource callers already evidenced in §24.29 include `aesBase64DecodeToString` for encrypted source content and `HMacHex` for HmacSHA1 signing. The absence of a public source hit for the remaining helper methods is not deletion authorization: `JsExtensions : JsEncodeUtils` makes them dynamically reachable through the Java wrapper.
- No test command was run in this signature inventory. Do not upgrade crypto compatibility from **partial** to exact based on matching declaration shapes. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.

### Next closure

- Continue with the direct `JsExtensions` declarations: exact Kotlin/JVM parameter and return/nullability shapes, @JavascriptInterface presence, inherited/default-generated overloads, caller context and side effects. Keep reflection/wrapper reachability distinct from help-documentation and public corpus hits.
- After source declarations are closed, use compiled upstream and host artifacts for pre-R8 ABI/Kotlin metadata comparison; keep that as a separate gate. No implementation or test changes are authorized by §26.13.


## 24.34 Correction — cacheContent return type is Boolean; batch behavior remains unimplemented (2026-11-24)

- **Correction to §24.31 item 5 and checklist §26.21:** the statement that pinned upstream `cacheContent(chapter, content)` has Unit/undefined script semantics is incorrect and is superseded here. Pinned `JsExtensions` declares `@JavascriptInterface fun cacheContent(chapter: Any?, content: String): Boolean` (`JsExtensions.kt` blob `f6808edd2a0d312e8bd13649935dea444cb82581`, lines 135–150). It returns `batchContext.saveContent(identifier, content)`; this is a Boolean result, not Unit.
- **Host declaration:** `RhinoSafeSourceJavaExtensions.cacheContent(chapter: Any?, content: String): Boolean` also returns Boolean at the source/JVM descriptor level (`RhinoSafeSourceJavaExtensions.kt` blob `6a00e649895c4c1d223b768c6b288632ec033de9`, line 201). Therefore this is **not a return-type mismatch**.
- **Actual behavior blocker remains:** host `getBatchContext(): Any? = null`, and host `cacheContent` unconditionally throws `IllegalStateException("java.cacheContent can only be called in a batch content rule")`. It does not have an active typed `BatchContentContext`, does not persist per-chapter batch content and cannot return the upstream save success/failure Boolean for an active batch. Pinned upstream only throws when no batch context is installed; inside a valid batch it delegates to `BatchContentContext.saveContent`.
- **Correct classification:** `cacheContent` signature return type **matches**; batch-context object type/identity, valid-batch execution, side effects, false/error paths and lifecycle remain **blocking behavior gaps**. `getBatchContext` remains a separate type/behavior mismatch (`BatchContentContext?` upstream versus host `Any?` that always returns null).
- **Evidence boundary:** source inspection only; no tests or implementation changes. Preserve the prior audit history but use this correction for all future matrices and acceptance decisions. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.


## 24.35 Direct JsExtensions — concrete source-signature gaps beyond the method-name matrix (2026-11-24)

### Inventory reconciliation

- Pinned `JsExtensions.kt` has **118 direct public declarations / 77 distinct names**; inherited `JsEncodeUtils.kt` adds **30 declarations / 27 names**; the two name sets are disjoint, yielding the previously recorded **148 declarations / 104 distinct names**. Sources: JsExtensions blob `f6808edd2a0d312e8bd13649935dea444cb82581`; JsEncodeUtils blob `c75b61650db2abdd64a09d2bd8340ef14c2947b3`. Host `RhinoSafeSourceJavaExtensions` has 150 direct public declarations in blob `6a00e649895c4c1d223b768c6b288632ec033de9`.
- The source inventory must keep three separate counts: source declarations, generated JVM overloads (e.g. `@JvmOverloads`), and Rhino-callable members through the actual wrapper. A matching name or declaration count is not an ABI or semantic match.

### Confirmed direct signature/return deltas

| Surface | Pinned upstream declaration | Host declaration | Classification |
|---|---|---|---|
| Source identity | `getSource(): BaseSource?` | `getSource(): Map<String, Any?>` | Return object/type and identity mismatch. |
| Source tag | `getTag(): String?` | `getTag(): String` | Nullability mismatch. |
| Batch context | `getBatchContext(): BatchContentContext?` | `getBatchContext(): Any?` (always null) | Return type and active context mismatch. |
| AJAX | `ajax(Any): String?`; `ajax(Any, Long?): String?` | `ajax(String): String`; `ajax(Any?): String` | Missing timeout overload; input and nullable return mismatch. |
| Concurrent AJAX | `ajaxAll(Array<String>): Array<StrResponse>`; `ajaxAll(Array<String>, Boolean): Array<StrResponse>` | `ajaxAll(Any?, Boolean): Array<RhinoSourceHttpResponse>` with generated default bridge | Parameter/response object mismatch; generated overload count alone is not parity. |
| Test AJAX | `ajaxTestAll(Array<String>, Int): Array<StrResponse>`; three-argument form returns `Array<StrResponse>` | `ajaxTestAll(Any?, Int, Boolean): Array<RhinoSourceHttpResponse>` with generated default bridge | Parameter/response object mismatch. |
| AnalyzeUrl connect | `connect(String): StrResponse`; header overloads use `String?`; returns `StrResponse` | `connect(String)`, `connect(String, Any?)`, `connect(String, Any?, Long?)`; returns `RhinoSourceHttpResponse` | Header representation and return object differ. |
| HTTP Response | `get/head/post(...): Connection.Response` | returns `RhinoSourceHttpResponse` | See §24.32 for inherited Response/Base surface gaps. |
| Base64 decode | upstream accepts nullable input on its String overloads | host String parameters are non-null | Null input compatibility differs; overloads/flags require differential tests. |
| Base64 encode | upstream returns `String?` | host returns `String` | Return nullability differs. |
| Hex encode | upstream `hexEncodeToString(String): String?` | host returns `String` | Return nullability differs. |
| Time formatting | upstream `Long` timestamp and nullable UTC result | host uses `Number`/extra `String` overloads and non-null `String` | JVM parameter and nullability differ; coercion convenience is not ABI parity. |
| HTML formatting | upstream arguments are non-null `String` | host accepts nullable `String?` arguments | Input nullability differs; output behavior still needs differential fixtures. |
| File handle | upstream `getFile(String): File` | host `getFile(String): RhinoSourceFileHandle` | Intentional safe facade; not type parity. Preserve class-shutter boundary and test allowed operations. |
| URL object | upstream `toURL(String[, String?]): JsURL` | host returns `RhinoLegadoUrl` | Returned object/property ABI differs; host common property tests do not establish full JsURL parity. |
| Logging | upstream `log(Any?): Any?`, `logType(Any?): Unit` | host returns `String` for both | Return and side-effect contracts differ. |

- The 17 missing upstream names remain as classified in §24.31: `refreshBookInfo`, `refreshBookToc`, `refreshContent`, `webView`, `webViewGetSource`, `webViewGetOverrideUrl`, `openVideoPlayer`, `startBrowser`, `startBrowserAwait`, `getVerificationCode`, `openUrl`, `showBrowser`, `getReadBookConfig`, `getReadBookConfigMap`, `getThemeMode`, `getThemeConfig`, `getThemeConfigMap`. The six active UI/browser/captcha callers are already proven in §24.30; the remaining settings/refresh methods must be explicitly classified rather than silently treated as irrelevant.
- **Correction carried forward from §24.34:** `cacheContent(Any?, String): Boolean` has the same declared Boolean return type upstream and host. Its blocker is missing batch context and save behavior, not a return-type mismatch.
- Some extra host methods intentionally support AnalyzeRule-style key/URL access or safe adapters; do not count host-only overloads as upstream compatibility gains. In particular host one-argument `get(keyOrUrl)` is an AnalyzeRule helper and does not replace the JsExtensions `get(url, headers[, timeout])` return object.

### Evidence and closure boundary

- Material upstream declarations are in JsExtensions blob `f6808edd2a0d312e8bd13649935dea444cb82581` (e.g. source identity/batch lines 127–150; ajax/connect 176–260; HTTP response 575–633; encoding/time 655–775; file/URL/logging 802, 1178–1217).
- Host declaration groups are in `RhinoSafeSourceJavaExtensions.kt` blob `6a00e649895c4c1d223b768c6b288632ec033de9`; the host ABI artifact and test scope remain those already recorded in §§24.25/24.31. No upstream compiled JVM ABI comparison has been performed in this slice.
- This is a **confirmed-delta summary**, not closure of all 104 method rows. Remaining same-name overloads, default argument bridges, annotations, nullability metadata, dynamic wrapper dispatch, active caller contexts and behavior/error/cancellation semantics still require row-by-row review.
- No application code or tests changed; no test command was run. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.
- **Next:** continue exact signature and caller review for the remaining direct JsExtensions methods, then re-check every source-visible returned object and dynamic dispatch path. Keep implementation blocked until §26.13 acceptance criteria are met.


## 24.36 Count correction — JsEncodeUtils overloads versus distinct names (2026-11-24)

- **Correction to §§24.33 and 24.35:** `JsEncodeUtils.kt` has **30 public declarations but 27 distinct method names**, because three declarations are additional overloads. The 77 distinct names declared directly by `JsExtensions.kt` and the 27 inherited `JsEncodeUtils` names are disjoint. The combined total is therefore **148 declarations / 104 distinct names**: 118 + 30 declarations, 77 + 27 names.
- **Verified from pinned blobs:** `JsEncodeUtils.kt` `c75b61650db2abdd64a09d2bd8340ef14c2947b3` has 30 `fun` declarations and 27 distinct names; `JsExtensions.kt` `f6808edd2a0d312e8bd13649935dea444cb82581` has 118 public direct declarations and 77 distinct names; their name-set intersection is empty.
- The earlier “30 names / three overlaps” wording was a counting error. The source-signature comparison of the 30 declarations remains unchanged; only the distinct-name arithmetic is corrected. Future matrices must count declarations and distinct names separately.


## 24.37 Runtime-3.2f.50 follow-up — BaseSource scope lifetime, request bindings and bridge identity (2026-11-24)

- **Audit base:** `d3d932ad00cd32c84c867fdca9a2bb9b63595bab` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** read-only source comparison of upstream `BaseSource.evalJS` / `buildScriptBindings` / `SharedJsScope` against Badcat `RhinoSourceScriptRuntime.evaluate` / `ScriptBindings` and source bridge installation. No production code, tests, Gradle, workflow, database or UI changes.

### Upstream facts confirmed

1. Every `BaseSource.evalJS` call invokes `buildScriptBindings { ... }`; pinned `ScriptBindingsExtensions.kt` constructs a **new `ScriptBindings()` for each call**. The per-call bindings include `java = this`, `source = this`, `sourceApi = this`, `baseUrl = getKey()`, `cookie = CookieStore` and `cache = CacheManager`.
2. The upstream `java`, `source` and `sourceApi` values are the same BaseSource instance. `BaseSource.getSource()` returns `this`; this is object identity, not merely equivalent descriptor data.
3. The fresh per-call bindings chain to a shared jsLib scope only when the source has a nonblank jsLib. The shared-global-state key is derived from jsLib hash, concrete source class name and source-key hash. The jsLib scope itself is cached by jsLib hash in an LRU of weak references (capacity 16), with synchronized creation and invalidation.
4. Without a shared jsLib scope, upstream creates a fresh standard top-level scope for that invocation. The per-invocation bindings object is not reused as the next invocation's lexical/request scope.

### Badcat facts confirmed at the audited HEAD

1. `RhinoSourceScriptRuntime` retains one `ScriptBindings` VarScope per source ID in `sourceScopes` and reuses it through `computeIfAbsent` across `evaluate` calls. It chains that scope to a source-global handle; the host lifecycle is therefore not the upstream fresh-bindings-per-call model.
2. Each call writes only the keys present in the incoming `bindings` map. The inspected path does not remove request-only keys that were supplied by an earlier call but are absent from the next call.
3. Each `evaluate` creates a new `RhinoSafeSourceJavaExtensions` instance, installs it as `scope["java"]`, and stores it in the runtime's bridge map. `scope["source"]` is a cached `RhinoSourceLoginExtensions`; no `sourceApi` binding is installed.
4. The Java bridge constructor receives closures over per-evaluation mutable values including current rule content and base URL. The source global scope is persistent, so JS can retain a reference to an earlier bridge in a global variable even after the global `java` property is replaced.

### Compatibility classification

- **Confirmed identity/API gap:** upstream `java === source === sourceApi` and `java.getSource() === java` are not represented by the host's three distinct/missing bindings and descriptor-map `getSource()`.
- **Confirmed scope-lifetime design difference:** upstream creates fresh invocation bindings; Badcat reuses a per-source VarScope. This is not automatically incompatible in every script, but it changes where request bindings and top-level declarations live.
- **Static stale-binding risk (not runtime-proven):** because host bindings are only overwritten when present, a key such as `page`, `key`, `result` or another caller-provided name can remain in the persistent scope when omitted by a later invocation. This must be verified with a differential probe before being described as an observed runtime failure.
- **Static retained-bridge risk (not runtime-proven):** a script can retain an earlier `java` object in a global variable. That object keeps its original per-evaluation closures; later use may observe the earlier request's base URL/content context. No runtime probe was run in this audit.
- **Not a simple instruction to remove persistent state:** upstream intentionally preserves selected jsLib/global state through a separately keyed shared scope. Any future host design must distinguish per-invocation bindings, source-global state, jsLib shared state, lexical declarations, and bridge/request context rather than flattening them into one scope.

### Required differential closure before implementation

- Compare two consecutive evaluations where the first supplies `page`/`key`/`result` and the second omits or changes each binding; inspect stale values and lexical `var`/`let`/`const` behavior.
- Compare no-jsLib, identical-jsLib and changed-jsLib source cases; verify source-key/class isolation, shared globals, invalidation and scope recreation.
- Verify `java === source`, `source === sourceApi`, `java.getSource() === java`, and property/method visibility against pinned upstream.
- Retain an earlier bridge reference in JS global state, run a second evaluation with a different base URL/content, then call the retained reference; compare closure context and cleanup behavior.
- Verify `clearSourceState` and source replacement invalidate every related scope, shared-global handle, bridge reference and request state without clearing another source's state.

**Decision:** this closes an additional Stage-0 source-evidence slice only. It does not authorize a production patch or test implementation. Register a separate, exact-file implementation task with typed scope/identity semantics and the differential tests above only after the remaining Stage-0 architecture/ABI gates are reviewed. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**. No CI was run.


## 24.38 Runtime-3.2f.50 follow-up — StrResponse and JsURL returned-object compatibility (2026-11-24)

- **Audit base:** `1bcb24aa4ab0b957b4b8a3071b4c4d7e17df9da0` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** source-level comparison of pinned `StrResponse` / `JsURL` with Badcat's Rhino response/URL facades and the already-confirmed `connect` / `ajaxAll` return paths. No tests or implementation were run.

### Pinned upstream object contracts

1. `JsExtensions.connect(...)`, `ajaxAll(...)` and `ajaxTestAll(...)` return `StrResponse` values (arrays for the batch methods). Pinned `StrResponse` exposes:
   - `raw: okhttp3.Response` (public getter; private setter);
   - nullable `body: String?`, nullable `errorBody: ResponseBody?`, and `callTime: Int`;
   - constructors for raw response + text body, URL + body (synthetic 200 response), and raw response + error body;
   - `raw()`, `body()`, `code()`, `message()`, `headers(): okhttp3.Headers`, `isSuccessful()`, `errorBody()`, `callTime()`, `url()`, the `url` property and `toString()`.
2. Pinned `JsURL` exposes Kotlin properties `searchParams`, `host`, `origin` and `pathname`. Its constructor resolves relative URLs against an optional base URL; query parsing stores decoded values in a map.

### Badcat surface confirmed at the audited HEAD

1. `connect` returns `RhinoSourceHttpResponse`, including for synthetic transport-error responses. `ajaxAll` and `ajaxTestAll` return arrays of that host class, not arrays of upstream `StrResponse`.
2. `RhinoSourceHttpResponse` exposes non-null `body()/getBody()`, status/code/message/success, callTime, URL, a single-header helper, `headers(): Map<String,List<String>>`, safe raw-response metadata, parse/select and charset. It does not expose upstream `errorBody()` or an `okhttp3.Response` raw object.
3. The host's safe raw response/request wrappers intentionally expose only selected metadata. This is a deliberate security boundary, but it is not the same object contract as the pinned `StrResponse.raw`.
4. `RhinoLegadoUrl` exposes method-style `getHost/getOrigin/getPathname/getSearchParams` plus host-only path/protocol/query aliases. The inspected class does not declare the upstream Kotlin properties `host/origin/pathname/searchParams` as properties.

### Compatibility classification

- **Confirmed returned-type mismatch:** `connect`, `ajaxAll` and `ajaxTestAll` do not return pinned `StrResponse` objects. The host class offers a useful overlapping subset, but its response, raw response and header types differ.
- **Confirmed declared-member mismatch:** upstream `StrResponse.errorBody()` and the raw `okhttp3.Response` surface are absent from the safe host facade. Do not expose unbounded raw response streams merely to copy the type; any equivalent must define bounded byte access, body-consumption, cancellation and resource ownership.
- **Nullability mismatch:** upstream `StrResponse.body` is nullable; host `body()` returns non-null `String`. The host synthetic failure response also uses a text body and status 200, but that alone does not prove every upstream constructor/error branch is equivalent.
- **URL property risk:** upstream declares properties while the host declares Java-style methods. Whether Rhino property lookup makes these script-equivalent must be tested against the actual pinned wrapper; source spelling alone is not enough to conclude a runtime failure.
- **Header contract remains open:** upstream returns OkHttp `Headers`; host returns a multi-value map. Case-insensitive lookup, duplicate values, iteration, null/missing behavior and script property access need differential fixtures.

### Required differential closure

- Exercise `connect`, `ajaxAll` and `ajaxTestAll` success, HTTP-error and transport-error paths; compare returned JS type, array element type, nullable body, code/message/callTime/url and `toString()`.
- Compare `headers()` iteration, duplicate header values, case-insensitive lookup, missing headers and direct header property access.
- Compare `raw` and `raw()` access separately; explicitly test which bounded metadata/byte operations are required by active source callers. Do not add arbitrary stream or socket access.
- Compare `JsURL` direct properties and method calls for absolute/relative URLs, explicit ports, empty query, repeated query keys, encoded values and malformed URLs.

**Decision:** another Stage-0 returned-object signature slice is closed; semantic and Rhino-dispatch parity remain unverified. This does not authorize widening the host facade or production code changes. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**. No CI was run.


## 24.39 Runtime-3.2f.50 follow-up — direct JsExtensions ajax overload and timeout propagation (2026-11-24)

- **Audit base:** `758086db99dd7b63396d42e0e4307813d3af71b5` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** read-only comparison of pinned `JsExtensions.ajax` and `AnalyzeUrl.callTimeout` with the host main bridge and runtime injection. No test or production implementation was run.

### Pinned upstream contract

1. Pinned `JsExtensions` declares both `ajax(url: Any): String?` and `ajax(url: Any, callTimeout: Long?): String?`. The one-argument overload delegates to the two-argument overload with null.
2. The two-argument path extracts the first element when the URL is a List, creates `AnalyzeUrl(urlStr, source = getSource(), callTimeout = callTimeout, coroutineContext = context)`, and returns `getStrResponse().body`. On failure it checks coroutine activity, logs the failure and returns the exception stack trace string.
3. Pinned `AnalyzeUrl` stores the explicit timeout as `callTimeoutMs`; its client builder uses `callTimeoutMillis = callTimeoutMs` when constructing the request client. This is a request execution timeout, not merely a URL suffix or a general default socket timeout.

### Badcat surface confirmed at the audited HEAD

1. `RhinoSafeSourceJavaExtensions` declares `ajax(urlAndOptions: String): String` and `ajax(url: Any?): String`; it has no two-argument `ajax(url, callTimeout)` overload.
2. The one-argument host method extracts a list/Scriptable first element, checks URL length and calls the injected `ajaxRequest(raw)` callback. Its callback type accepts only one String argument; the runtime closure also supplies only the current source and current base URL to `sourceAjax`. No explicit per-call timeout value can cross this path.
3. The host does expose a timeout-bearing `connect(url, headers, callTimeout)` method, but it returns the separate response facade and has different upstream semantics. It does not satisfy the missing `ajax(url, callTimeout)` signature or its nullable body-string return contract.
4. The host one-argument list handling uses `firstOrNull()?.toString().orEmpty()`; pinned upstream uses `firstOrNull().toString()`. For an empty list, the resulting input differs (`""` versus `"null"`) before URL parsing/error handling. This is a source-confirmed edge difference; exact surfaced JS error text still requires a runtime probe.

### Compatibility classification

- **Confirmed missing overload:** `ajax(Any, Long?): String?` is absent from the host source/JVM declaration surface. A String-only overload and a three-argument `connect` are not substitutes.
- **Confirmed timeout propagation gap:** the host ajax callback accepts only the URL string, so explicit upstream `callTimeout` cannot be forwarded through this API path. The timeout behavior of the host transport's default request configuration is a separate matter and does not establish parity.
- **Confirmed nullable-return declaration difference:** upstream declares `String?`; host declares non-null `String`. The runtime may return stack-trace text on failures, but the declared null contract is still different and should be included in ABI/behavior fixtures.
- **Static input edge difference:** empty-list conversion differs before request execution; no dynamic Rhino observation was collected.

### Required differential closure

- Invoke `java.ajax(url)` and `java.ajax(url, timeout)` for a normal URL, URL list, empty list, malformed URL, HTTP error, transport error and timeout.
- Compare actual timeout duration and cancellation behavior, returned null/body/error string, logging/exception propagation and source/base URL resolution.
- Capture the compiled upstream and host JVM descriptors and the Rhino-visible overload set; verify overload selection for JavaScript Number, numeric String, null and omitted second argument.
- Keep `ajax` separate from `connect`, `get/head/post` and URL/options suffix parsing in the method matrix.

**Decision:** one additional direct JsExtensions overload and request-timeout gap is confirmed from source. Caller prevalence and dynamic overload selection remain open. This does not authorize a production patch. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**. No CI was run.


## 24.40 Runtime-3.2f.50 follow-up — ZIP entry edge semantics (2026-11-24)

- **Audit base:** `e0a4210db552e1a5433178af3e3021779c9cbcea` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** narrow supplement to the archive-helper comparison already recorded above. No production code or tests changed.
- **Pinned ZIP behavior:** `getZipByteArrayContent(url, path)` compares each `entry.name` with the requested path without excluding directory entries. If no entry matches, it logs `getZipContent 未发现内容` and returns null (pinned `JsExtensions.kt`, lines 984–1004).
- **Host behavior:** the host matches only `!entry.isDirectory && entry.name == path`; if no regular-file entry matches, it returns null without the corresponding source log (host `RhinoSafeSourceJavaExtensions.kt`, lines 636–670).
- **Compatibility classification:**
  1. **Confirmed entry-selection difference:** a directory entry with an exact matching name can be selected by the pinned loop but is skipped by the host. Caller impact is not yet established.
  2. **Confirmed diagnostic difference:** pinned no-match handling emits a log message; host no-match handling is silent. This is an observability gap, not proof of a failed source rule.
  3. Host compressed/expanded archive limits remain a separate intentional safety constraint already recorded in the archive audit; this entry does not recommend removing them.
- **Required differential closure:** fixtures for a matching regular file, matching directory entry, missing entry and duplicate names; compare bytes/null, exceptions and source-attributed logs. Keep compressed/expanded resource limits independently tested.
- **Decision:** two narrow ZIP behavior deltas are source-confirmed; caller impact and dynamic Rhino observability remain unverified. No CI, tests, production source, Gradle, database or UI were changed. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.


## 24.41 Runtime-3.2f.50 follow-up — ajaxAll batch input and test-error semantics (2026-11-24)

- **Audit base:** `7dc15cc29d08fe1f64b5da3de365cda18488aa67` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** narrow supplement to the existing HTTP/batch method inventory (§22) and response-facade comparison (§24.38). It does not repeat the already recorded return-object mismatch or ajax single-request timeout gap. No production code or tests changed.

### Pinned upstream behavior

- `ajaxAll` accepts `Array<String>`; it passes each URL to `AnalyzeUrl` and invokes `getStrResponseAwait(skipRateLimit = ...)`. Parallelism is `AppConfig.threadCount` (pinned `JsExtensions.kt`, lines 200–214).
- `ajaxTestAll` accepts `Array<String>` plus an integer timeout and optional skip-rate-limit boolean. It passes the timeout into `AnalyzeUrl.callTimeout` and invokes `getStrResponseAwait(isTest = true, ...)` (lines 219–234).
- In pinned `AnalyzeUrl.executeStrRequest`, test-mode transport exceptions are classified by the **direct exception type** into negative call-time codes; non-test mode rethrows (pinned `AnalyzeUrl.kt`, `executeStrRequest`).

### Host behavior confirmed at the audited HEAD

- Host `ajaxAll/ajaxTestAll` accept `Any?` and first materialize bounded URL collections. Supported inputs include Rhino `Scriptable`, `Iterable`, arrays and JSON-array strings. The host enforces at most 20 URLs, 64 KiB total collection input and 16 KiB per URL (`RhinoSafeSourceJavaExtensions.kt`, lines 2011–2052; constants lines 1980–1982).
- Host then converts values to strings, trims whitespace and rejects blank URLs (lines 943–947, 972–976). Pinned declarations take `Array<String>` and do not perform this same pre-normalization in `JsExtensions`.
- Host batch worker concurrency is fixed at 4; pinned concurrency follows the mutable/configured `AppConfig.threadCount` (host lines 949–956, 978–1014; pinned lines 205 and 224).
- Host `ajaxTestAll` walks up to 16 nested `Throwable.cause` links before selecting the negative call-time code; pinned `AnalyzeUrl` classifies the caught exception itself. Wrapped transport exceptions can therefore be classified differently. Host also normalizes a nullable exception message to an empty string when constructing its synthetic response (host lines 985–1007); pinned passes `e.message` to `StrResponse` (upstream `AnalyzeUrl.kt`).
- Host uses `RhinoSourceHttpResponse` rather than pinned `StrResponse`; that broader facade difference is already tracked in §24.38 and is not recounted here.

### Classification and follow-up

1. **Confirmed input-contract difference:** accepted input types and pre-validation normalization differ. The host's bounded limits are intentional safety controls, not automatically defects; however, they narrow the upstream accepted domain and must be explicit in the compatibility policy and tests.
2. **Confirmed concurrency-policy difference:** fixed host cap 4 versus upstream configured thread count. Whether this changes observable source behavior under load needs controlled concurrency fixtures; do not remove the cap without a safety review.
3. **Confirmed error-classification difference:** direct exception matching upstream versus bounded cause-chain matching host can yield different `callTime` codes for wrapped exceptions. This requires fixtures with direct and wrapped instances of each transport exception family.
4. **Dynamic behavior remains unverified:** Rhino array/Iterable/JSON-string coercion, cancellation while worker futures are running, per-source rate limiter behavior, and response ordering/partial failure need differential tests against the pinned implementation.

- **Required differential closure:** test array and Scriptable inputs, empty arrays, whitespace/blank entries, >20 entries, oversized URL/aggregate inputs, configured concurrency values, direct versus wrapped timeout/DNS/connect/socket/SSL exceptions, null exception messages, cancellation, rate-limit skip flag, result ordering and mixed success/failure batches.
- **Decision:** this records additional narrow source-confirmed batch API and error-classification deltas; no claim is made that every delta is a product defect. No CI, tests, production source, Gradle, database or UI were changed. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.


## 24.42 Runtime-3.2f.50 follow-up — source-navigation caller evidence for missing host callbacks (2026-11-24)

- **Audit base:** `0eadb6b0f7b5cdc8fc4dd7ea5c5eae32f1995309` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** this is caller/host-binding evidence for the missing public API names already inventoried in §22.1. It does **not** create a second missing-name list or repeat the overall API gap count.

### Pinned caller evidence

- Pinned upstream `app/src/androidTest/java/io/legado/app/help/source/SourceNavigationUiTest.kt`, lines 81–96, executes `java.openUrl`, `java.startBrowser`, `java.showBrowser`, `java.openVideoPlayer` and `java.startBrowserAwait` from a source JS rule under `SuppressSourceNavigation`; the test asserts navigation is blocked and no UI navigation starts.
- The same test, lines 98–127, executes `java.openUrl` from a source header and from `@webjs`, alongside `source.openUrl`, and verifies the navigation policy changes the observed number of launched activities. This demonstrates that the `java.*` methods are not merely help-document examples; they participate in source/header/WebView JS execution paths.
- Pinned `JsExtensions.kt` defines these UI operations with source-navigation suppression and coroutine-context checks. `webView`, `webViewGetSource`, and `webViewGetOverrideUrl` also depend on the source's headers/tag and the BackstageWebView execution path; `startBrowserAwait` and `getVerificationCode` return user-mediated verification results.

### Host binding evidence at the audited HEAD

- `RhinoSourceScriptRuntime.kt`, lines 285–337, constructs `RhinoSafeSourceJavaExtensions` with HTTP, rate-limit, cookie, key-value and rule-reader callbacks; no WebView, browser, verification-code, external-navigation, video-player or refresh-event callback is supplied.
- The same runtime binds that instance directly as `scope["java"]` (lines 457–458); separate bindings are `cookie`, `source` and `cache` (lines 459–476). The source-login bridge's `source.openUrl` method is a different object/API and cannot stand in for `java.openUrl` or the other `java.*` methods.
- The compiled-host inventory test `RhinoHostSurfaceInventoryTest` inventories `scope.java` as `RhinoSafeSourceJavaExtensions` and explicitly states that the inventory is not upstream parity approval. The bridge's declaration inventory still has none of the §22.1 browser/navigation/refresh/read-theme method names.

### Classification and closure

1. **Caller reachability is confirmed upstream** for `openUrl`, `startBrowser`, `showBrowser`, `openVideoPlayer`, and `startBrowserAwait` through a pinned Android instrumentation test, including header and `@webjs` contexts.
2. **Host callback absence is source-confirmed** at the Java bridge construction/binding boundary. Calling these names through the current host `java` binding has no matching implementation. The exact Rhino error and user-visible outcome are not dynamically verified in this audit turn.
3. **Compatibility impact:** this is a real Stage-0 blocker for source rules that require browser verification or user navigation; it cannot be classified as safely irrelevant solely because Badcat is a different product. Any eventual implementation must use typed host ports, preserve source identity/context/cancellation and the source-navigation suppression policy, and must not transplant Legado UI.
4. **Required differential closure:** a host instrumentation suite for the same JS contexts (direct rule, source header, `@webjs`), both navigation-policy states, successful/cancelled browser verification, image-code return, and WebView response/body/URL behavior. Also verify refresh callbacks and read/theme config separately against Badcat-owned product state.
- **Decision:** adds caller evidence to an already recorded API gap; no duplicate API item was created. No CI, tests, production source, Gradle, database or UI were changed. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.


## 24.43 Runtime-3.2f.50 follow-up — refresh events and read/theme configuration callbacks (2026-11-24)

- **Audit base:** `daf8b0c058deac6c7c7b6d2d95a5904c68909e69` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** adds semantic evidence for the already inventoried missing refresh/read-theme/verification API names in §22.1 and §24.42. It does not repeat the missing-name inventory and does not assert that Badcat must reproduce Legado's product UI.

### Pinned upstream semantics

- `JsExtensions.refreshBookInfo()`, `refreshBookToc()`, and `refreshContent()` publish distinct EventBus events: `REFRESH_BOOK_INFO`, `REFRESH_BOOK_TOC`, and `REFRESH_BOOK_CONTENT` respectively (`JsExtensions.kt`, lines 157–167).
- The pinned `JsExtensionsRefreshContractTest` asserts that these three methods and their three event constants exist on the shared JS extension, and explicitly asserts that the same refresh methods are absent from `SourceLoginJsExtensions`. This establishes that these are source-runtime callbacks, not login-bridge methods.
- `getReadBookConfig()` serializes `ReadBookConfig.durConfig` to JSON; `getReadBookConfigMap()` returns the map projection of the same durable configuration (lines 1359–1365).
- `getThemeMode()` returns `AppConfig.themeMode`, defaulting null to string `"0"`; `getThemeConfig()` serializes `ThemeConfig.getDurConfig(appCtx)`; `getThemeConfigMap()` returns its map projection (lines 1371–1387). The string and map forms are separate callable surfaces and should not be collapsed into one approximate return contract.
- `getVerificationCode(imageUrl)` obtains the current source via `getSource()`, checks the Rhino context is active, then invokes `SourceVerificationHelp.getVerificationResult` with the source and source-navigation/coroutine context; it requires a `VerificationResult.Response` and returns the response's verification value (lines 442–449).
- Pinned `SourceVerificationHelp` requires a non-null source and background-thread invocation, serializes access to verification UI, observes coroutine cancellation, and has bounded waiting/flight coordination. Its tests cover source/type/origin isolation, cancellation, timeout, abandonment and concurrent verification coalescing. This is not equivalent to a generic synchronous string prompt.

### Host boundary evidence at the audited HEAD

- The production `scope["java"]` binding remains `RhinoSafeSourceJavaExtensions`; the host bridge declaration surface has no matching `refreshBookInfo`, `refreshBookToc`, `refreshContent`, read-config, theme-config/mode or `getVerificationCode` methods. This is consistent with the absence already recorded in §22.1; no duplicate missing-name item is created.
- The host runtime constructor currently supplies rule/HTTP, rate-limit, Cookie, key-value, and other source callbacks, but does not supply typed refresh-event, durable reader-config, theme-config, or user-verification ports. No equivalent host callback was found in the runtime binding path.
- The refresh methods are product-event dispatch in the pinned application. A Badcat implementation must translate them to Badcat-owned state/events through typed host ports; it must not import Legado's reader/product UI or blindly replay its EventBus.
- Read/theme settings are also product-owned state. If these APIs remain exposed to imported source scripts, Badcat must provide a documented Badcat-owned projection with compatible JSON/map/null/default semantics, or explicitly establish through a complete caller/compatibility policy that a specific method is intentionally unsupported. Silent omission is not parity.
- Verification requires an explicit user-mediated host flow, preserving source identity, coroutine cancellation, background-thread constraints, timeout, and concurrent-request semantics. A no-op, fixed string, or generic blocking prompt would not be equivalent.

### Classification and required closure

1. **Source-confirmed API semantics:** the refresh callbacks have three distinct event effects; read/theme JSON and map accessors have distinct return shapes/defaults; image verification is source-scoped and cancellation-aware.
2. **Host source-confirmed gap:** the current Java bridge and its constructor do not expose these APIs or corresponding typed ports. The exact Rhino error and user-visible effects have not been dynamically exercised in this turn.
3. **Caller evidence boundary:** refresh and configuration names are documented in pinned source help and covered by the upstream refresh contract test; this turn did not locate an independent imported third-party source fixture invoking every one of them. Do not claim broader real-source caller frequency without that evidence.
4. **Required differential closure:** test each refresh event dispatch and Badcat state effect; JSON versus Map shape and values for reader/theme settings including null/default theme mode; verification success/cancel/timeout, source isolation, background-thread enforcement, concurrent requests, and cancellation propagation. Verify the APIs through Rhino using the actual `scope.java` wrapper, not only direct Kotlin calls.
5. **Decision:** these are additional semantic details for existing §22.1 gaps, not new duplicate API entries. Badcat product ownership determines the adapter implementation, not permission to silently omit the source-facing contract. No CI, tests, production source, Gradle, database or UI were changed. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.


## 24.44 Runtime-3.2f.50 follow-up — WebView rule caller and BackstageWebView lifecycle contract (2026-11-24)

- **Audit base:** `4ee1cce2288beb7a17d421543f4d5fc8cf7b54ee` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Deduplication:** §24.15 already records the separate `WebJsExtensions` asynchronous WebView-page bridge and its 16-operation dispatcher; §24.31 already counts the three `JsExtensions` WebView names in the public API matrix. This section adds the concrete content-rule caller and the lower-level request/lifecycle contract only. It does not create another missing-method inventory.

### Concrete caller and business-chain evidence

- Pinned Android test asset `app/src/androidTest/assets/issue1180-content-rules.json` contains a BookSource fixture named `⚡📂书荒小说` whose content JS checks for an encrypted payload; on the fallback branch it invokes `java.webView(null, baseUrl, null)`, then reads `#txt@html` through `java.getString`. This is a concrete imported-source compatibility fixture demonstrating that `java.webView` is a content-rule fallback, not merely a help-page example. It is a test asset, not evidence that the external site currently works.
- Pinned `WebBook.getContentAwait` passes `ContentRule.webJs` and `ContentRule.sourceRegex` into `AnalyzeUrl.getStrResponseAwait` (WebBook.kt lines 458–461). The ordinary content response path therefore reaches the WebView execution branch through stored per-source content-rule fields.
- `AnalyzeUrl.executeStrRequest` chooses between network response and `BackstageWebView` when both request-level `useWebView` and the instance's `useWebView` are enabled. For POST it first obtains the HTTP response, then loads the returned URL/body into BackstageWebView; for other methods it loads the analyzed URL directly. It passes the active request headers, source key, webJs/jsStr, sourceRegex and configured delay (AnalyzeUrl.kt lines 472–516). This is the normal content-rule integration path, distinct from a direct call to `java.webView`.
- The same pinned WebBook fixture and call path do not establish that `webViewGetSource` or `webViewGetOverrideUrl` is commonly used by public third-party source JSON. Keep their API presence and test coverage in the matrix without inventing caller frequency.

### BackstageWebView request and return semantics

- The three `JsExtensions` methods require a background thread. They call `runBlocking(context)`, and each creates a BackstageWebView using `getSource()?.getHeaderMap(true)` plus `getSource()?.getKey()` as the request headers and source tag. The default `webView(html,url,js)` overload selects `cacheFirst=false`; the six-argument resource/override overloads default `cacheFirst=false` and `delayTime=0`.
- `webView` loads supplied non-empty HTML with `loadDataWithBaseURL(url, html, ...)`, or loads the URL directly when HTML is empty. If the caller supplies no JS, the default result expression is `document.documentElement.outerHTML`. A caller-supplied JS expression instead determines the returned value.
- `webViewGetSource` installs a sniffer client and compares each loaded resource URL with `requestUrl.matches(sourceRegex.toRegex())`; on the first match it returns a `StrResponse(originalUrl, matchedResourceUrl)` and releases the WebView. `webViewGetOverrideUrl` applies the regex to navigation requests via the corresponding `shouldOverrideUrlLoading` callback and returns `StrResponse(originalUrl, matchedNavigationUrl)`. These are not interchangeable with extracting page HTML or with an HTTP redirect-following request.
- The standard HTML client evaluates JS after page finish plus `100ms + delayTime`. Empty/JS-null results are retried with bounded backoff; after more than 30 retries it reports `js执行超时`. If navigation redirected, the returned StrResponse is constructed with a synthetic 302 prior response and a 200 final response; otherwise it is tied to the original URL. The source/override sniffer returns the matched URL in the response body, not a fetched response body.
- The request has a default 60-second timeout. Coroutine cancellation schedules destruction/release on the UI thread. Completion callbacks are accepted only while the captured navigation context remains active and the same pooled WebView instance is still owned by that request. The pinned `BackstageWebViewLifecycleTest` exercises cancellation before startup, stale HTML/sniffer callback isolation after WebView reuse, null-result retry, and successful resource/override URL capture.
- For source-tagged requests, the WebView client reads the WebView CookieManager cookie header after page finish and asynchronously writes it into the Legado CookieStore under that source tag. Request configuration moves User-Agent into WebSettings, filters CookieJar/proxy from additional HTTP headers, and preserves other headers. The active WebView's SSL-error callback calls `proceed()`; a Badcat-native adapter must make this security-sensitive policy explicit rather than silently inheriting it.
- BackstageWebView uses the shared WebView pool; release removes injected JS interfaces, stops loading, clears callbacks/listeners and recycles or destroys the view. This lifecycle and isolation are part of the observable host contract, not optional cleanup details.

### Host comparison at the audited HEAD

- `RhinoSafeSourceJavaExtensions` has no `webView`, `webViewGetSource` or `webViewGetOverrideUrl` declarations. Its class contract explicitly excludes WebView access. The `webViewUserAgent` string property only supplies a UA value; it is not a WebView implementation or host port.
- `RhinoSourceScriptRuntime` constructs the safe bridge with HTTP, Cookie, source-state, rule-reader and other callbacks, but no WebView execution, resource-sniffer, navigation-intercept, WebView-cookie synchronization, or WebView lifecycle port. The host tests for WebView cookie-domain invalidation and Android JavaBridge overload selection cover separate platform concerns; they do not exercise these three source-script APIs.
- Therefore the missing behavior is not fixed by ordinary HTTP GET/POST, the host WebView UA getter, or the separate cookie jar. The current source runtime cannot perform the pinned content-rule fallback shown in the fixture through `scope.java`.

### Classification and required closure

1. **Source-confirmed reachability:** a pinned content-rule fixture calls `java.webView`; the ordinary BookSource content pipeline passes `webJs/sourceRegex` into AnalyzeUrl and its BackstageWebView branch.
2. **Source-confirmed upstream contract:** background-thread precondition, source-specific headers/tag, HTML-or-URL loading, JS extraction, resource/navigation regex capture, delay/cache flags, response shape, bounded retry/timeout, CookieStore synchronization and cancellation-safe pooled-WebView release.
3. **Host source-confirmed gap:** no matching three methods or typed WebView port exists in the current host bridge/runtime. The exact Rhino exception and end-user failure on the fixture have not been dynamically executed in this turn.
4. **Required differential closure:** (a) direct `java.webView` HTML/URL/default-JS/custom-JS and background-thread cases; (b) `webViewGetSource` resource match and no-match/timeout; (c) `webViewGetOverrideUrl` navigation match and no-match; (d) content-rule `webJs/sourceRegex` GET and POST path; (e) source header/UA/cookie round trip and source isolation; (f) cancellation at startup/load/JS callback and pooled-view reuse; (g) redirects, response metadata, delay/cache flags, and explicit Badcat SSL policy. Verify through the actual Rhino `scope.java` object and a pinned source fixture, not direct adapter-only tests.
5. **Architecture boundary:** retain Badcat UI/product ownership. Implement a typed Badcat-native background WebView execution port or a demonstrably equivalent controlled engine; do not import Legado's browser/dialog UI or silently weaken the source-facing contract.
6. **Decision:** this adds caller and lifecycle evidence to the already recorded §24.15/§24.31 WebView gap; it is not a second missing-API entry. No CI, tests, production source, Gradle, database or UI were changed. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.


## 24.45 Runtime-3.2f.50 follow-up — BaseSource login execution, login-header injection and host session facade (2026-11-24)

- **Audit base:** 7cec89bfc2df7225a3d85f9df66964777795d0c8 on integration/legado-upstream-runtime; pinned upstream LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Deduplication:** §24.18 already covers the distinct AnalyzeUrl object used by loginCheckJs and its request replay methods. §24.16 inventories the SourceLogin UI callback subtype. §24.37 records BaseSource identity and per-evaluation scope lifetime. This section closes the ordinary BookSource source.login()/BaseSource login-header and login-info path, not the AnalyzeUrl replay or WebView login UI.

### Pinned BaseSource behavior and caller contract

1. **Login script execution is a BaseSource operation.** BaseSource.login() (BaseSource.kt:178–191) reads loginUrl through getLoginJs(), appends a call to the script's login() function with this as receiver, and evaluates it through BaseSource.evalJS. If a nonblank login rule does not define login, it throws Function login not implements!!!. The same BaseSource instance is bound as java, source and sourceApi in that evaluation (lines 414–422).
2. **Dynamic headers and saved login headers are two layers.** BaseSource.getHeaderMap(hasLoginHeader) evaluates an inline-JS header rule if present, parses it as a string map, adds the configured default User-Agent if absent, and only then merges saved login headers when hasLoginHeader=true (BaseSource.kt:197–222). Cancellation is rethrown rather than swallowed by the header-rule error handler.
3. **Login-header persistence has coupled Cookie side effects.** putLoginHeader(header) parses the supplied JSON map, calls CookieStore.replaceCookie(getKey(), cookie) when a Cookie member is present, and stores the original header JSON under loginHeader_<sourceKey> (BaseSource.kt:240–247). removeLoginHeader() deletes the stored header and calls CookieStore.removeCookie(getKey()), which removes the whole effective-domain cookie bucket, not just a Cookie field (lines 249–252).
4. **Login-info storage is separate from login-header storage.** getLoginInfo() decrypts userInfo_<sourceKey> using the first 16 bytes of Android ID as the AES key (BaseSource.kt:258–267). getLoginInfoMap() can derive defaults from the source login UI rule; for legacy login UI it evaluates the login script plus inline login-UI JS under a re-entry guard, excludes button rows, and persists the resulting name/default map (lines 276–304). putLoginInfo(info) AES-encrypts and stores the supplied value, returning Boolean success; remove deletes only that login-info key (lines 309–325).
5. **Source variables and arbitrary key/value state are separate namespaces.** putVariable/getVariable use sourceVariable_<sourceKey>; put/get use v_<sourceKey>_<key> (BaseSource.kt:331–375). Neither is the login-header, login-info or CookieStore namespace.
6. **Login headers are deliberately same-site only for requests.** AnalyzeUrl constructs its initial header map from source.getHeaderMap(hasLoginHeader && isLoginHeaderSite(mUrl)) (AnalyzeUrl.kt:139–147). The predicate compares the source key's registrable domain with the initial target URL, or base URL for a relative target; if either domain cannot be determined, it preserves the prior behavior and attaches the login header (lines 154–170). The decision is made against the initial URL before embedded @js rewrite. This prevents Authorization headers being sent to third-party cover/CDN hosts.

### Host implementation at the audited HEAD

- RhinoSourceScriptRuntime.evaluate binds scope.source to a cached RhinoSourceLoginExtensions instance, constructed with source.id as both sourceId and sourceKey (RhinoSourceScriptRuntime.kt:466–475). It is not the active upstream BaseSource object.
- The host RhinoSourceLoginExtensions exposes getKey, source variable get/set, login-header get/map/put/remove and login-info get/map/put/remove. It has no login(), no dynamic getHeaderMap(hasLoginHeader), and no login-rule evaluator bound to the same BaseSource. The ordinary source java binding is a separate RhinoSafeSourceJavaExtensions object, which also has no login, getHeaderMap or login-info/header methods. Thus source.login() and source.getHeaderMap(true) have no matching method on the inspected host source object.
- Host persistence is partial infrastructure: SharedPreferencesSourceLoginSessionStore stores login headers and encrypted login info keyed by sourceId; SourceLoginSessionRequestClient merges saved login headers into requests only when the sourceId-derived registrable domain matches the target. BadcatReadingEngineFactory wires login-header Cookie import to SharedPreferencesSourceCookieJar.mergeLoginCookieHeader, and removing login headers removes the source-key domain cookie bucket.
- The host Login UI v2 service has a separate checkLogin(source) path that evaluates source.loginCheckJs as a standalone source-script evaluation. It is not the upstream WebBook + AnalyzeUrl flow that binds java to the active request, passes StrResponse as result, and permits source.login() followed by header merge and request replay. The §24.18 request-replay gap remains open.
- Existing host tests establish storage/cipher behavior, source-specific session persistence, same-domain cookie merging, cross-domain cookie exclusion and selected WebView cookie import policies. They do not establish that a real BookSource loginUrl/login JS can call source.login(), that dynamic header rules are evaluated and merged into the same active AnalyzeUrl, or that the original failed request is replayed after login.

### Exact semantic deltas and risk classification

1. **Missing source-script execution methods — source-confirmed gap:** source.login() and source.getHeaderMap(true) are absent from the object installed as host source. The exact Rhino exception and a real imported-source failure were not dynamically executed in this audit.
2. **Request lifecycle mismatch — source-confirmed architectural gap:** upstream loginCheckJs uses one active AnalyzeUrl instance for initial request, error response, login, header merge and retry. Host standalone checkLogin plus a request-level saved-header decorator does not establish that same-object lifecycle or replay semantics.
3. **Header behavior only partially represented:** the host session decorator uses source-domain comparison and overlays saved headers onto request headers, but no dynamic BaseSource header-rule evaluation, default User-Agent, or per-invocation hasLoginHeader decision is exposed through source. Domain matching alone is not complete header parity.
4. **Storage API shape differs:** upstream putLoginHeader returns Unit and stores supplied JSON text after best-effort Cookie extraction; host putLoginHeader returns normalized JSON and rejects malformed/empty/non-string maps and invalid header values. Upstream putLoginInfo returns Boolean; host returns normalized JSON String. Upstream login-info/header getters are nullable where host getters normalize missing values to empty strings. These may be defensible host safety policies, but are observable contract differences requiring explicit compatibility tests and decisions.
5. **Cookie ownership is not source-isolated.** Upstream CookieStore is domain-bucketed, so multiple source keys on the same effective domain intentionally share cookie values; login header and login info remain source-keyed. Host follows the same broad separation, but source clear/removal must not treat sourceId as a registrable-domain key unless it is the actual source URL. The current factory clear callback passes sourceId into clearDomain; confirm the ID-to-source-key invariant before relying on it.
6. **No claim of full login support:** encrypted login-info storage, login-header persistence and a request decorator are partial infrastructure, not the complete BaseSource login execution lifecycle.

### Required differential closure

- Through the actual Rhino scope.source, run a pinned source fixture with loginUrl and login(); verify this identity, source/java/sourceApi bindings and persisted login info/header.
- Verify getHeaderMap(false/true) with static and inline-JS headers, default UA, case variants, login-header override, malformed JSON and coroutine cancellation.
- Replay a failed BookSource request through loginCheckJs: first request, source.login(), merge source.getHeaderMap(true), initUrl(), retry and status-500 propagation. Compare exact request count, URL, headers, response object and cancellation.
- Verify same-site/subdomain/third-party CDN/IP/unparseable-domain cases against the pinned initial-URL rule; include embedded @js cross-domain rewrite to ensure the host does not silently change the security boundary.
- Verify login-header Cookie replace/merge, logout's whole-domain Cookie removal, session-cookie merge, source-key isolation for login info/header/variables, two sources on one domain, and source clear after source ID/key remapping.
- Treat validation limits as explicit host policy and prove they do not reject legitimate pinned-source header/login-info fixtures; test nullable/Unit/Boolean return-shape differences at the Rhino boundary.

**Decision:** this is a new source-level login lifecycle/API closure, not a duplicate of §24.16, §24.18 or §24.37. It records partial host session infrastructure and the still-missing source-facing login execution/header API. No production source, tests, Gradle, workflow, database/schema, UI or CI were changed. Stage-0 remains in_progress/not accepted; Stage-1 remains NO-GO.


## 24.46 Runtime-3.2f.50 follow-up — source identity, catalog replacement/removal and runtime-state invalidation closure (2026-11-24)

- **Audit base:** `fedfdecdd671c1a22e0bc1f0818649b91bc18652` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Deduplication:** §24.37 already records upstream/host scope lifetime and required clear-state probes; §24.45 records login/session namespaces and the sourceId/sourceKey uncertainty. This section resolves the identity mapping and follows the actual catalog replacement/removal callback. It does not repeat the JS API inventory or claim the overall lifecycle is complete.

### Identity mapping: upstream source key and host source ID

1. Pinned upstream `BookSource.getKey()` returns `bookSourceUrl` (`app/src/main/java/io/legado/app/data/entities/BookSource.kt:110–112`). `BaseSource.evalJS` binds `baseUrl = getKey()` and the same BaseSource instance as `java/source/sourceApi` (`BaseSource.kt:414–422`).
2. Badcat `LegadoBookSourceCodec.decodeObject` takes nonblank `bookSourceUrl` as `sourceId` (`LegadoBookSourceCodec.kt:254–270`); `toEngineSource()` maps that value to both `BookSource.id` and `BookSource.baseUrl` (`LegadoBookSourceCodec.kt:51–55`). For the current imported BookSource path, host `source.id` is therefore the URL-backed upstream source key, not an unrelated generated database ID.
3. The factory's `sourceCookieClearer` maps `sourceId` to `SharedPreferencesSourceCookieJar.clearDomain(sourceId)` (`BadcatReadingEngineFactory.kt:92–100`). Given the mapping above, the input is the source URL and matches the cookie jar's domain-normalization input. The earlier §24.45 concern that the ID might be a non-URL surrogate is not supported for this codec/catalog path.

### Actual catalog invalidation path

1. `FileBookSourceCatalog.importJson` selects replacement IDs only for existing entries whose decoded source record differs; it writes the merged catalog first, then calls `invalidateRuntimeSources(replacedIds)` (`FileBookSourceCatalog.kt:67–82`). New sources and unchanged records do not trigger cleanup.
2. `FileBookSourceCatalog.remove` writes the remaining catalog first, then invalidates the removed source ID (`FileBookSourceCatalog.kt:122–139`). An absent ID does not trigger cleanup.
3. The production source-management screen constructs its catalog with `onSourceStateInvalidated = { sourceId -> sourceScriptRuntime.clearSourceState(sourceId) }` (`SourceCatalogActivity.kt:34–66`). Its import and delete operations therefore use the callback-enabled catalog. The activity runtime also wires Cookie clearing to the URL-backed source ID. This confirms the active management path is connected; do not characterize all catalog paths as unhooked.
4. `RhinoSourceScriptRuntime.clearSourceState` takes the process-wide per-source mutex also used by `evaluate`, removes source scope/global/hash state, then visits every live runtime instance and clears bridge maps, cache extension state, key/value store, file cache, physical cache, login session and cookie state (`RhinoSourceScriptRuntime.kt:561–600, 792–803`). The cross-runtime test verifies clearing from one runtime invalidates another live runtime's cache state (`RhinoSourceScriptRuntimeTest.kt:27–59`).
5. Catalog lifecycle tests verify changed-existing-only replacement invalidation, deletion invalidation, no invalidation for malformed imports or absent IDs, and that cleanup failure is reported after the catalog file has already committed (`FileBookSourceCatalogLifecycleTest.kt:27–88`).

### Remaining boundary and risk

- **Confirmed active-path mapping:** current imported `bookSourceUrl → sourceId → BookSource.id/baseUrl → sourceKey/Cookie clear` is consistent. This resolves the specific ID-versus-URL uncertainty from §24.45 for the present codec path.
- **Confirmed non-atomic catalog/cleanup sequence:** source replacement/removal is persisted before cleanup. If cleanup fails, the operation returns failure but the new catalog state (or deletion) remains committed. The existing test explicitly asserts this. This is an observable recovery/transaction limitation, not evidence that cleanup always fails.
- **Factory accessor caveat:** `BadcatReadingEngineFactory` constructs its private `FileBookSourceCatalog(context.applicationContext)` with the default no-op invalidation callback and exposes it via `sourceCatalog()` (`BadcatReadingEngineFactory.kt:41–48,247–249`). The inspected `SourceCatalogActivity` does not use this accessor; it constructs a callback-enabled catalog. Treat the accessor as a latent unsafe path unless/until a caller is found, not as proof that the current management screen skips cleanup.
- **Not yet dynamically closed:** no end-to-end test in this audit replaced/deleted a source after populating every state namespace and then asserted absence of stale login header/info, Cookie domain entries, source variables, JS lexical/global state, file cache and all runtime bridges across independently-created factories. The existing cross-runtime test covers cache invalidation only; catalog tests cover callback ordering, not the full state matrix.
- **Required differential closure:** (a) actual catalog replacement and deletion through the production callback; (b) populate all source-owned state namespaces, then clear and assert all stores and cookie domain are empty; (c) use two runtime/factory instances and prove both are invalidated; (d) force one cleanup subsystem to fail and verify aggregated errors, retry/recovery behavior, and catalog reconciliation; (e) wire the factory's exposed `sourceCatalog()` callback to its runtime or remove/guard that accessor before it becomes a mutation entrypoint; (f) verify URL normalization and source identity for scheme/host/path variants and ensure no unrelated same-domain source state is deleted.
- **Decision:** this adds source caller/identity/cleanup transaction evidence to §24.37 and §24.45 without duplicating them. It does not authorize production changes during Stage-0. No production source, tests, Gradle, workflow, database/schema, UI or CI were changed. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.


## 24.47 Runtime-3.2f.50 follow-up — jsLib JSON URL maps, shared-library scope cache and refresh invalidation (2026-11-24)

- **Audit base:** `8a5fac3aa29fc93c601b288f22f5ea29bf4bef69` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Deduplication:** §24.37 records the per-invocation bindings and per-source/global scope lifetime difference; this section is specifically about the *shared jsLib layer*: supported jsLib encodings, external-library download/cache, shared-scope identity, and refresh invalidation. It does not restate the general scope-lifetime issue.

### Pinned upstream behavior

1. `BaseSource.evalJS` builds fresh bindings, obtains `getShareScope()` (or a CryptoJS scope fallback), then chains the per-call bindings to that shared library scope using `getSharedGlobalStateKey()` (`BaseSource.kt:414–433`).
2. `BaseSource.getShareScope` delegates to `SharedJsScope.getScope(jsLib)`. The shared scope key is MD5 of the complete jsLib text; the scope cache is an LRU of 16 weak references. Creation is serialized per jsLib key (`BaseSourceExtensions.kt:13–15`; `SharedJsScope.kt:30–37,103–124,150–180`).
3. Upstream accepts two jsLib forms. Non-JSON text is evaluated as JS. If jsLib is a JSON object, `parseJsLibMap` decodes a string-to-string map; every absolute URL value is downloaded, the response body is cached in `ACache` under MD5(URL), and the downloaded script is evaluated in the shared scope. A failed download throws rather than silently treating the map as source code (`SharedJsScope.kt:231–274`).
4. The shared executable library scope and source-owned global state are deliberately separate. The source state key is `MD5(jsLib):sourceClassName:MD5(sourceKey)`; each BaseSource evaluation chains its own fresh bindings to the shared library while resolving global state through that source-specific handle (`BaseSourceExtensions.kt:17–21`; `BaseSource.kt:424–433`; `ScriptBindings.kt:32–44,86–93`). Thus identical jsLib text can share library definitions without intentionally sharing mutable per-source global state.
5. `SharedJsScope.remove(jsLib)` removes URL-backed cached bodies, removes the shared library scope from the LRU, and invalidates shared-global handles whose keys start with that jsLib hash prefix (`SharedJsScope.kt:126–148`). Upstream `BaseSource.refreshJSLib()` requires a background thread and delegates to this removal (`BaseSource.kt:391–400)); the pinned JS help documents `source.refreshJSLib()` as a public source API.

### Badcat host behavior at the audited HEAD

1. `RhinoSourceScriptRuntime.evaluate` reads `source.jsLib` as one raw string, computes a SHA-256 hash per `source.id`, installs one persistent `VarScope` per source, and evaluates `RhinoRegexCompatibility.normalize(jsLib)` directly once per source version (`RhinoSourceScriptRuntime.kt:237–271,479–495`).
2. No JSON-object jsLib decoder, URL-value downloader, MD5(URL) external-library body cache, or jsLib-hash-keyed shared LRU scope is present in this runtime path. The runtime's existing HTTP closures serve source-rule requests; the jsLib initialization path does not invoke them.
3. Identical jsLib text is therefore re-evaluated independently in each source scope instead of sharing one library scope. Host mutable global state is keyed by `GLOBAL_KEY_PREFIX + source.id`; it does not reproduce the upstream composite key containing jsLib hash, source class and source key (`RhinoSourceScriptRuntime.kt:244–270,790–803`).
4. On a source edit, the host resets that source's scope/global/hash state if its jsLib or mainJs hash changes. It does not expose an upstream-equivalent `refreshJSLib()` operation that evicts a shared jsLib scope and all URL-backed cached library bodies (`RhinoSourceScriptRuntime.kt:246–258,561–568`). The inspected source bridge exposes no corresponding typed refresh callback.
5. Existing tests cover inline JS jsLib evaluation, isolation from a different source with no jsLib, and per-source lexical reinitialization after an inline jsLib change (`RhinoSourceScriptRuntimeTest.kt:140–154,334–355,1023–1079`). They do not cover JSON URL maps, external library download/cache, identical jsLib shared-scope behavior, URL cache invalidation, or refresh across multiple sources.

### Compatibility classification and implications

- **Confirmed source/API gap:** upstream JSON URL-map jsLib is a supported input form with download/cache/execute semantics; the host treats the whole field as a script string. Imported sources using this form cannot rely on the pinned upstream initialization contract.
- **Confirmed shared-scope semantic difference:** upstream shares executable jsLib definitions by library text while keeping mutable global state keyed per source class/key/library. Host instead compiles/evaluates each source's jsLib into its own source scope. This is not necessarily a security defect, but it is not the pinned scope/cache contract and has different initialization cost and cross-source library refresh behavior.
- **Confirmed refresh API gap:** upstream publicly exposes `source.refreshJSLib()`; the inspected Badcat source bridge has no equivalent operation. A change to content behind an unchanged URL cannot be forced through an upstream-equivalent cache eviction path.
- **Dynamic outcome not established:** this audit did not run a Rhino script containing a JSON URL map against Badcat, nor a two-source same-jsLib/shared-state differential, nor a refresh-after-remote-library-change scenario. Therefore the precise Rhino parse error and user-visible failure are not claimed as observed.
- **Required differential closure:** (a) inline jsLib versus JSON URL map; (b) multiple URL entries, request failure, empty body, relative/non-absolute values and URL cache hit; (c) two source keys and two source classes sharing the same jsLib, verifying common library definitions but isolated mutable globals; (d) refresh after remote content changes, verifying URL body and shared scope invalidation; (e) edit/remove one source while another still uses the same library; (f) verify cancellation, timeout, headers/Cookie policy and bounded response size for external jsLib fetches before choosing the Badcat typed host port; (g) preserve Badcat's current script-size/security limits unless a measured compatibility requirement and explicit security decision justify a change.
- **Decision:** new Stage-0 evidence for jsLib input contract, cache ownership and refresh lifecycle. No production code, tests, Gradle, workflow, database/schema, UI or CI were changed. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.


## 24.48 Runtime-3.2f.50 follow-up — AnalyzeRule / AnalyzeUrl business caller coverage and host pipeline boundary (2026-11-24)

- **Audit base:** `84261f8cd2654b4de2f08b2f69ff7f7b1d9b5a7f` on `integration/legado-upstream-runtime`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** book-source business chain only: search, book detail, TOC, chapter content and their request/rule execution. RSS/video/other product-only callers are not included unless a shared source-runtime dependency is proven.
- **Deduplication:** §24.38–§24.43 record individual HTTP/JS bridge and browser contracts; §24.47 records jsLib/SharedJsScope. This section maps the actual AnalyzeRule/AnalyzeUrl business callers to Badcat service implementations and classifies what the current pipeline does and does not execute.

### Pinned upstream call graph

1. **Search:** `BookList` receives the configured `AnalyzeUrl`, response body and base URL, then constructs `AnalyzeRule(ruleData, bookSource)`, sets content/base/redirect/coroutine context, and resolves result elements and fields including name, author, kind, word count, latest chapter, intro, cover and book URL (`BookList.kt:36–58,97–124,194–218,220–282`). `AnalyzeUrl` is responsible for URL interpolation/options, request metadata and the actual request/response path (`AnalyzeUrl.kt:82–140,188–224,248–317,454–490`).
2. **Book detail:** `BookInfo.analyzeBookInfo` creates `AnalyzeRule(book, bookSource)`, sets response content, base URL, redirect URL and coroutine context, then evaluates book metadata, TOC URL, web-file download URLs and related fields (`BookInfo.kt:39–45,60–68,74–88,121–165`).
3. **TOC:** `BookChapterList` constructs `AnalyzeUrl` for subsequent TOC pages, fetches each page, and creates `AnalyzeRule(book, bookSource, false, isFromBookInfo)` to parse chapter elements, names, URLs, VIP/pay flags, volume state, update time and next-page URLs (`BookChapterList.kt:72–112,198–215,228–280`).
4. **Chapter content:** `BookContent` constructs `AnalyzeRule(book, bookSource)` with chapter and next-chapter context; it uses `AnalyzeUrl` for subsequent content pages and sub-content URLs, then evaluates content, title, next-page rules, replacements and batch-content JS (`BookContent.kt:63–94,131–175,232–268,297–320`).
5. The pinned `AnalyzeRule` is not a selector-only parser. Its rule execution dispatch includes Regex, WebJs, JavaScript, JSONPath, XPath and Jsoup/CSS branches for both text and element extraction (`AnalyzeRule.kt:204–235,314–350,422–475`); it also carries rule-data/source/chapter/batch context and local/script caches (`AnalyzeRule.kt:63–90`). Its JS evaluator binds the source-facing JS context and uses the upstream shared jsLib scope/state path (`AnalyzeRule.kt:896–923`; see §24.37 and §24.47).
6. Pinned `AnalyzeUrl` also is not a URL string interpolator only. It accepts source/rule-data/chapter, timeout, header, login-header, infoMap and extra params; evaluates embedded JS and {{...}} blocks; parses URL option JSON for method, headers, body, type, charset, retry, WebView, webJs/bodyJs, timeout, redirect policy and DNS; and executes rate-limited requests with cancellation/coroutine context (`AnalyzeUrl.kt:82–129,188–224,248–317,392–423,454–490`). The broader request execution details are cross-referenced to §24.38–§24.43 rather than duplicated here.

### Badcat host caller path at audited HEAD

1. `SourceReadingEngine` routes search/explore, detail, TOC and chapter-content operations to four separate services (`SourceReadingEngine.kt:14–22,26–48,51–76,79–91,94–150`). The factory supplies all four services and one shared HTTP gateway/runtime instance (`BadcatReadingEngineFactory.kt:221–244`).
2. **Search:** `SourceSearchService` executes the URL through `SourceHttpGateway` and then calls `CssSearchResultsParser.parse/parseWithScripts`; the optional JS path is not an upstream `AnalyzeRule` instance (`SourceSearchService.kt:7–37`). The URL resolver leaves the configured expression intact, while `RhinoEmbeddedUrlScriptEvaluator` exposes only key/page/result/baseUrl/source identity/name and no Java/network/Cookie/cache/WebView objects (`BookSourceSearchUrlResolver.kt:3–35`; `RhinoEmbeddedUrlScriptEvaluator.kt:8–49`).
3. **Detail:** `SourceBookDetailService` fetches via the gateway and calls `CssBookInfoParser.parse/parseWithScripts` (`SourceBookDetailService.kt:3–27`).
4. **TOC:** `SourceTocService` loops through fetched pages with URL-cycle and max-page guards, then calls `CssTocParser.parse/parseWithScripts` (`SourceTocService.kt:3–18,27–88`).
5. **Content:** `SourceChapterContentService` fetches paginated content, calls `CssChapterContentParser.parse/parseWithScripts`, then applies Badcat's separate book-text replacement pipeline and chapter cache (`SourceChapterContentService.kt:3–29,51–110,112–167`).
6. The generic host `LegadoRuleRuntime` contract is stage/document oriented; `SelectorLegadoRuleRuntime` explicitly describes itself as a bounded selector-only runtime, and dispatches configured selectors or a limited script call rather than instantiating pinned `AnalyzeRule` (`SourceRuntime.kt:52–68`; `SelectorLegadoRuleRuntime.kt:3–14,76–114`). `SourceRuleResolver` extracts string fields from nested stage JSON and routes them to that runtime; it does not model upstream `SourceRule` chains, typed `RuleDataInterface`, JS/Regex/WebJs/JSON/XPath/CSS composition or AnalyzeRule's mutable chapter/batch context (`SourceRuleResolver.kt:19–50,55–102`).
7. Host `SourceScriptRuntime` exposes `evaluate(source, script, Map<String,String>)` and separate persistent get/put string variables; it is not an AnalyzeRule or AnalyzeUrl object and cannot by itself reproduce their method/property surface, overloaded return types, source context, cancellation and request lifecycle (`SourceRuntime.kt:77–95`; see §24.37–§24.43).

### Compatibility classification

- **Confirmed architecture gap:** the host business services have entrypoints for the four major book flows, but those entrypoints are not wired to the pinned upstream `AnalyzeRule` / `AnalyzeUrl` classes or an adapter proven equivalent by differential tests. The current service/parser stack is a Badcat-owned bounded implementation.
- **Confirmed parser-coverage gap:** pinned `AnalyzeRule` dispatches multiple rule modes and chained `SourceRule` behavior; the current generic host runtime is explicitly selector-only and the stage services are CSS parser based with optional limited JS. Therefore “search/detail/TOC/content methods exist” is not evidence of full Legado rule compatibility.
- **Confirmed request-contract gap at abstraction level:** upstream `AnalyzeUrl` composes request options, source/login headers, source-aware JS context, WebView switches, body/web JS, redirect/retry/timeout/DNS and rate limiting in one source-aware object. Host has separately implemented HTTP/URL/JS adapters, but this audit has not established a complete field-by-field and caller-by-caller equivalence between that set and `AnalyzeUrl`.
- **Not claimed:** this section does not assert every real source fails, nor does it assert that existing CSS/limited JS sources cannot work. It identifies unclosed semantic coverage against the pinned public runtime contract; no dynamic side-by-side execution was performed in this audit.
- **Required differential closure:** (a) enumerate every pinned book-flow caller of AnalyzeRule/AnalyzeUrl, including fallback, next-page, web-file and batch paths; (b) create fixed fixtures for every AnalyzeRule mode and chained rule combination in search/detail/TOC/content; (c) compare null/empty/invalid content, URL resolution, redirects, request options, headers/login headers, cookies, retries, timeout, cancellation and WebView behavior; (d) verify chapter/batch/source context bindings and @get/@put/replace semantics; (e) compare exact results and exceptions against pinned upstream; (f) decide whether to transplant the real upstream execution classes/dependency closure or build a typed compatibility adapter, without changing product UI or weakening the completeness gate.
- **Decision:** this is a new business caller/coverage evidence slice following §24.47. No production code, tests, Gradle, workflow, database/schema, UI or CI were changed. Stage-0 remains **in_progress/not accepted**; Stage-1 remains **NO-GO**.

## 24.49 Runtime-3.2f.50 follow-up — RuleDataInterface, Book/BookChapter variable scopes and typed AnalyzeRule context (2026-11-24)

- **Audit base:** 67a5e377445e5a2b70b822020bbe95f199660b24 on integration/legado-upstream-runtime; pinned upstream LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Scope:** variable ownership and JS object/context handoff between upstream RuleDataInterface, BaseBook/Book, BookChapter, BaseSource, AnalyzeRule and Badcat's current script/parser boundary. This is a distinct data/context contract slice; §24.37 and §24.48 remain the references for general runtime scope and business callers.
- **Deduplication:** does not repeat jsLib shared-scope/cache issues (§24.47), broad AnalyzeRule mode coverage (§24.48), or source identity/catalog cleanup (§24.46).

### Pinned upstream data and variable semantics

1. RuleDataInterface defines a mutable HashMap<String,String> plus putVariable/getVariable/putBigVariable/getBigVariable. Values shorter than 10,000 characters are stored in the in-memory map; null removes; large values are delegated to the implementing object; reads fall back from map to big storage and then to empty string (RuleDataInterface.kt:3–36).
2. The fallback RuleData implementation is a request/rule-local holder whose putBigVariable simply stores in its map and whose serialized getVariable() returns null for an empty map (RuleData.kt:5–28). This is not the same owner as book, chapter or source variables.
3. BaseBook extends RuleDataInterface and defines book identity and metadata (name/author/bookUrl/kind/wordCount/variable), transient infoHtml/tocHtml, and book-scoped large-variable persistence via RuleBigDataHelp.putBookVariable/getBookVariable(bookUrl,key) (BaseBook.kt:8–39). Concrete Book is a Room entity whose primary key and equality/hash identity are bookUrl; it persists the variable JSON and lazily reconstructs variableMap from that JSON (Book.kt:39–55,121–148,130–140). It also carries TOC URL, origin/source, read config and current reading state; those are not disposable parser strings (Book.kt:40–49,90–129).
4. BookChapter independently implements RuleDataInterface. Its persistent identity is composite (url, bookUrl), with a per-book unique chapter index; its serialized variable map is restored from variable; large variables are delegated to RuleBigDataHelp keyed by both bookUrl and chapter URL. Its normal update path persists the chapter unless updates are explicitly deferred (BookChapter.kt:29–67,88–95; big-variable overrides follow at lines 110 onward). Therefore chapter variables are not merely another alias for book variables.
5. BaseSource separately exposes source-level get/put storage under a source-key namespace, and its evalJS binds the real source object as both source and sourceApi, alongside java=this, baseUrl=getKey(), CookieStore and CacheManager (BaseSource.kt:65–100,360–374,411–433). This source-owned namespace is distinct from Book, BookChapter and transient RuleData.
6. AnalyzeRule stores ruleData: RuleDataInterface?, identifies book by casting it to BaseBook, and independently tracks chapter, nextChapterUrl, batchContext, current content/base/redirect URL, local bindings and script caches (AnalyzeRule.kt:63–95). It does not flatten these into a single string dictionary.
7. The exact variable behavior is scope-aware:
   - put(key,value) attempts chapter first, then book, then request ruleData, then source (AnalyzeRule.kt:856–864).
   - get(key) checks local bindings first, special-cases bookName and chapter title, then reads chapter → book → ruleData → source, taking the first non-empty value (AnalyzeRule.kt:867–890).
   - evalJS passes actual object values for java, source, book, chapter, chapters, result and content/context values including baseUrl, title, src, nextChapterUrl, fromBookInfo, plus optional paraIndex, paraData and numeric page (AnalyzeRule.kt:896–935). The JavaScript-visible objects retain their upstream methods/properties; they are not JSON/string snapshots.
8. The content caller proves these objects are live business context: BookContent constructs AnalyzeRule(book, bookSource), then sets content, redirect URL, coroutine context, the exact BookChapter and next-chapter URL; its batch path builds BatchContentContext(bookSource, book, chapters, coroutineContext, saveTokens), sets chapter and batch context, and closes the batch context in finally (BookContent.kt:62–73,227–241,293–325). This is required behavior for chapter rules and batch content, not optional metadata.

### Badcat host model and execution boundary

1. Host ReadingEngine.kt defines a compact BookSource, BookCandidate, BookDetail, BookChapter and ChapterContent transport model. The host BookChapter currently contains index/title/url/VIP/pay/tag/baseUrl only; it has no bookUrl, serialized variable field, variable map or RuleDataInterface behavior (ReadingEngine.kt:94–180). There is no corresponding host Book/BaseBook context object in this engine contract; book information is returned as a value-only BookDetail.
2. The host parser API passes BookSource, SourceHttpResponse, URL/body and (for content) the compact chapter DTO. Its SourceDocument holds only URL, body, contentType, key and page (SourceRuntime.kt:67–75). No typed Book, BaseBook, RuleDataInterface, chapter variable owner, next-chapter object or batch context is supplied to the rule runtime.
3. SourceScriptRuntime.evaluate accepts only Map<String,String> and returns EngineResult<String>; its persistent getString/putString methods are explicitly source-scoped and default to unsupported (SourceRuntime.kt:77–91). RhinoSourceScriptRuntime.evaluate installs only these primitive/string request bindings in the per-source Rhino scope and returns stringified primitive/bounded-array results; request binding names are deleted after evaluation (RhinoSourceScriptRuntime.kt:219–276,495–510). It does not accept upstream Book/BookChapter/chapters objects or preserve arbitrary Rhino return values as typed values.
4. The active LegadoPutRule.resolve implementation writes every parsed @put assignment via runtime.putString(source,key,value) and resolves every @get via runtime.getString(source,key) (LegadoPutRule.kt:57–127). It therefore maps all rule variables to one source-level key-value namespace. It does not implement upstream AnalyzeRule.put/get precedence or book/chapter/rule-local storage.
5. Detail parsing produces a BookDetail value and search produces BookCandidate; TOC parsing creates chapter DTOs without persisted book-scoped variable maps; content parsing receives that DTO but does not bind it into Rhino as chapter or a Book as book. Its script calls pass result/baseUrl/source identity/name/ruleName/page only (CssChapterContentParser.kt:13–20,34–73,124–140; detail and TOC parser calls have the same primitive-binding shape, see §24.48).
6. The host content path also has no equivalent to the upstream batch context object or the batch-context lifecycle. Existing pagination and cache behavior is Badcat-owned and must not be confused with Legado's contentBatch rule semantics.

### Compatibility classification

- **Confirmed model/ownership gap:** upstream has four distinct variable owners (chapter, book, transient ruleData, source), plus local AnalyzeRule bindings; the current host @get/@put adapter stores everything in source scope. This changes both write destination and read precedence and can leak values between books/chapters that should be independently scoped.
- **Confirmed typed-context gap:** host script bindings are string maps and do not carry the real upstream book, chapter, chapters, java AnalyzeRule object or sourceApi object contract. Upstream JS can inspect/mutate those objects and call their methods; host's string-only return also cannot preserve arbitrary upstream object/list/element return values without explicit typed conversion.
- **Confirmed chapter identity/storage gap:** upstream chapter variables and state are scoped by composite book URL + chapter URL and persisted through chapter data/big-variable storage. Host chapter DTO has neither book URL nor variable storage. Passing bookUrl as a separate service argument does not create an equivalent RuleDataInterface object or persistence owner.
- **Confirmed batch-rule gap:** upstream creates a real BatchContentContext containing source, book, chapter list, coroutine context and save tokens; host has no equivalent object/lifecycle and current content parser explicitly rejects contentBatch (§24.48 / CssChapterContentParser.kt:26–32).
- **Not claimed:** this is static source-contract evidence, not a dynamic demonstration that a particular public source fails. No Rhino differential was run for variable shadowing, large-variable persistence, object identity, chapter mutations or batch execution.
- **Required closure:** (a) introduce or reuse exact pinned upstream RuleDataInterface/BaseBook/Book/BookChapter/RuleData semantics inside the controlled runtime; (b) define typed host ports for book/chapter persistence and big-variable storage, preserving 10,000-character threshold, null/removal and transaction/update behavior; (c) preserve identity and get/put precedence with tests for local→chapter→book→ruleData→source, empty values, missing values, removal, large values and same chapter URL under different books; (d) bind actual Rhino objects and AnalyzeRule JsExtensions surface, not serialized maps; (e) model nextChapterUrl, sourceApi and batch context/cancellation/close; (f) run pinned-upstream differential tests for single chapter, multi-chapter and contentBatch paths.
- **Decision:** new Stage-0 model ownership/context evidence. It reinforces §24.37/§24.48 without duplicating their generic runtime findings. No production code, tests, Gradle, workflow, database/schema, UI or CI were changed. Stage-0 remains in_progress/not accepted; Stage-1 remains NO-GO.

## 24.50 Runtime-3.0 Stage-0 — six BookSource rule models, Gson adapters and host codec/runtime field loss (2026-11-24)

- **Task:** R3-S0-MODEL-BOOKSOURCE-RULES-001; registered in the third-class overview §4.1 before source investigation.
- **Audit base:** branch integration/legado-upstream-runtime at 43d58c4eca6e37393b7fa4c2e39c0524f2c790cc; pinned upstream LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Scope:** six nested BookSource rule classes, their inheritance/field types/defaults, Gson adapters, Room converters, import/export representation and host execution handoff.
- **Deduplication:** §24.48 covers broad AnalyzeRule/AnalyzeUrl stage coverage and selector-only pipeline boundary; §24.49 covers typed Book/BookChapter/RuleData context and variable scopes. This section adds rule-model field inventory, string/object JSON compatibility and concrete typed-field loss at the resolver boundary.

### Pinned upstream model inventory

All six rule classes are mutable Kotlin data classes annotated @Parcelize. The five ordinary rule models use nullable strings except where noted; the fields are rule expressions or script/config strings, not nested rule objects.

| Model | Pinned fields and types | Default / inheritance |
|---|---|---|
| SearchRule | checkKeyWord:String? plus BookListRule fields bookList, name, author, intro, kind, lastChapter, updateTime, bookUrl, coverUrl, wordCount (all String?) | every field null by default; implements BookListRule |
| ExploreRule | same ten BookListRule String? fields | every field null by default; implements BookListRule |
| BookInfoRule | init, name, author, intro, kind, lastChapter, updateTime, coverUrl, tocUrl, wordCount, canReName, downloadUrls (all String?) | every field null |
| TocRule | preUpdateJs, chapterList, chapterName, chapterUrl, formatJs, isVolume, isVip, isPay, updateTime, nextTocUrl (all String?) | every field null |
| ContentRule | content, subContent, title, nextContentUrl, webJs, sourceRegex, replaceRegex, imageStyle, imageDecode, payAction, callBackJs, contentBatch (String?); maxBatchSize (Int?) | all null; maxBatchSize null explicitly means batch unsupported |
| ReviewRule | reviewUrl, avatarRule, contentRule, postTimeRule, reviewQuoteUrl, voteUpUrl, voteDownUrl, postReviewUrl, postQuoteUrl, deleteUrl, reviewSummaryUrl, summaryListRule, summaryParagraphIndexRule, summaryParagraphDataRule, summaryCountRule, reviewDetailUrl, reviewDetailNextPageUrl, detailListRule, detailIdRule, detailAvatarRule, detailNameRule, detailBadgeRule, detailContentRule, replyListRule, replyIdRule, replyAvatarRule, replyNameRule, replyBadgeRule, replyContentRule (all String?); enabled (Boolean) | all strings null; enabled defaults false |

- **Upstream declarations:** BookListRule.kt:6–17; SearchRule.kt:12–39; ExploreRule.kt:12–40; BookInfoRule.kt:12–42; TocRule.kt:9–34; ContentRule.kt:12–58; ReviewRule.kt:9–66.
- **BookSource ownership/defaults:** upstream BookSource.kt:31–104 stores these as typed nullable rule properties (ruleExplore/ruleSearch/ruleBookInfo/ruleToc/ruleContent/ruleReview), not strings. The first five getters at lines 122–155 lazily create and assign empty rule objects when their property is null; the Review getter is commented out (157–162), and ReviewController accesses nullable ruleReview directly. BookSource.supportContentBatch/contentBatchSize uses ContentRule.contentBatch/maxBatchSize (246–258).
- **Serialization:** BookSource.Converters at lines 306–354 serializes each rule to JSON text and deserializes via GSON.fromJsonObject<T>(). Upstream GsonExtensions.kt:32–55 builds INITIAL_GSON then GSON with six per-type deserializers. Each rule deserializer accepts a JSON object directly, or a JSON primitive by parsing its string value as JSON; array/other JSON shapes return null. Unknown model fields are ignored by reflective Gson deserialization. The separate source JSON remains the import/export document.
- **Actual Review caller:** ReviewController.getSummary/getDetail/getReplies reads ruleReview.enabled and its summary/detail/reply fields and dispatches ReviewRuleParser; ReviewRuleParser builds AnalyzeRule with actual Book, BaseSource and BookChapter context. See ReviewController.kt:181–219, 247–250, 331–388 and ReviewRuleParser.kt:50–170. ReviewRule is therefore a real source-facing contract, not dead metadata.

### Host model / codec / tests

1. The six host rule class files under app/src/main/java/io/legado/app/data/entities/rule/ were compared field-by-field with the pinned declarations; their inspected property names, Kotlin types, null defaults, SearchRule/ExploreRule inheritance and ContentRule.toString conditional batch suffix match the pinned versions. BookListRule.kt is also present. Host GsonExtensions.kt:32–62 registers the same six adapters. This is genuine rule-class/Gson model reuse, but does not establish a complete BookSource model.
2. There is no host app/src/main/java/io/legado/app/data/entities/BookSource.kt in the current tree. The platform-neutral ReadingEngine.BookSource is a separate transport DTO (ReadingEngine.kt:94–134): its six rule fields are nullable String JSON slots (ruleExploreJson/ruleSearchJson/ruleBookInfoJson/ruleTocJson/ruleContentJson/ruleReviewJson), not typed ExploreRule/SearchRule/BookInfoRule/TocRule/ContentRule/ReviewRule properties. LegadoBookSource (codec lines 14–50) likewise carries six nullable JSON strings plus rawJson.
3. Codec decodeObject (LegadoBookSourceCodec.kt:254–304) keeps the full source JSON tree as rawJson and extracts each rule object into a compact JSON string. objectOrNull (415–423) accepts an object or a string containing a JSON object; the extracted execution copy is normalized to object JSON. encode (203–221) exports rawJson rather than reserializing the transport DTO, and withEnabled mutates only enabled in that raw document (197–201). This preserves unknown source-level and nested-rule JSON members semantically across this catalog path, while canonicalizing JSON text/formatting; it is not byte-for-byte preservation and does not provide typed runtime semantics.
4. Codec validateKnownFieldTypes (307–387) only permits rule fields to be JSON null, object, or a string containing an object. Upstream six rule deserializers return null for array/other non-object, non-primitive shapes; the host instead rejects such a source document. This is a stricter import policy, not upstream-equivalent behavior. Valid object and string-encoded object forms are supported by the host codec and typed Gson adapters.
5. The decisive runtime loss is after import: SourceRuleResolver.resolveObject (11–53) and SelectorLegadoRuleRuntime.evaluate (lines 15–35) keep only JSON primitive string members. Thus ContentRule.maxBatchSize (Int?) and ReviewRule.enabled (Boolean) survive rawJson/rule JSON storage but are dropped from the active field map. SearchRule.checkKeyWord is retained as a string but is metadata, not one of BookListRule's extraction expressions; the generic selector runtime currently iterates all string fields without a six-model typed field policy. This creates a static dispatch/interpretation risk for metadata/action/replacement fields (e.g. sourceRegex, replaceRegex, imageStyle, payAction, callBackJs, contentBatch) beyond the selector subset already covered in §24.48.
6. ReadingEngine API (ReadingEngine.kt:9–91) exposes search/explore/detail/TOC/content operations but no review operations. SourceRuleStage has REVIEW and SourceRuleResolver can retrieve raw review JSON, but that enum/lookup is not equivalent to ReviewController's review summary/detail/reply request and typed result pipeline. ReviewRule.enabled is also absent from the generic string-only field map.
7. Existing tests are useful but bounded:
   - PinnedLegadoRuleModelsTest covers selected fields for all six models, object/string parsing for several, selected null/default cases and ContentRule batch fields; it does not assert every field of every model.
   - LegadoGsonCompatibilityTest checks object and string-encoded object for all six, null/array cases for selected classes, and unknown-field ignoring only for ExploreRule.
   - LegadoBookSourceCodecTest verifies raw unknown source-field retention, selected rule JSON retention, source enabled mutation and several import shapes; it does not round-trip all six typed rule models through a BookSource entity/converter path because that upstream BookSource entity/converter is not present in the host tree.
   - PublicBookSourceCompatibilityCorpusTest exercises selected search/detail/TOC/content fixtures; no Explore or Review full caller path is covered there.
   - Upstream ContentRuleCompatibilityTest verifies legacy Rhino-visible ContentRule.toString and contentBatch/maxBatchSize string compatibility; it is source evidence, not proof that Badcat host executes batch content.

### Compatibility classification and closure

- **Confirmed parity:** the six standalone rule class declarations and six Gson type adapters in the host app match the inspected pinned declarations. Object and JSON-string forms have focused host Gson tests. ContentRule's legacy toString special case is retained and upstream-tested in the pinned source.
- **Confirmed structural gap:** the host has no typed upstream BookSource entity/converter path; it carries rule JSON strings through transport DTOs. Raw JSON retention protects import/export unknown fields but does not preserve typed defaults/getters/caller behavior at runtime.
- **Confirmed field loss:** generic rule field extraction drops non-string fields, specifically ContentRule.maxBatchSize and ReviewRule.enabled. The former controls upstream batch capability and batch size; the latter gates review summary/detail/reply paths. Both remain present only in raw JSON unless a typed model is introduced at execution.
- **Confirmed import-policy delta:** array-shaped nested rule values become null through upstream rule adapters but are rejected by host codec validation. Keep this explicit as a deliberate stricter policy if desired; do not call it exact import parity.
- **Static dispatch risk:** generic iteration of all string members has no rule-model-specific field classification; metadata and action/replacement/script fields can be routed through a selector-oriented evaluator. Concrete per-source failure is not claimed without differential execution.
- **Review caller gap:** upstream has a complete ReviewController + ReviewRuleParser caller path; the host ReadingEngine API has no review operation. SourceRuleStage.REVIEW alone does not close this.
- **Not claimed:** no dynamic differential tests were run in this audit; no claim that all real sources fail or that every unknown field is lost on export. The codec's rawJson export does preserve unknown JSON fields semantically on the current catalog path.
- **Required closure:** (a) keep the six upstream rule model classes and Gson adapters as the typed source of truth; (b) restore typed rule objects at the runtime boundary instead of relying on Map<String,String>; (c) explicitly preserve Int?/Boolean/null/default fields, BookSource lazy getter behavior, ContentRule batch semantics and ReviewRule enabled gating; (d) define field-specific dispatch for expression, URL, JS, metadata, replacement and callback properties; (e) either implement the upstream Review caller contract with typed host ports or explicitly keep Review unsupported and mark the capability blocked; (f) add all-field reflection/serialization tests, object/string/null/array/malformed cases, unknown nested-field import/export tests, and pinned differential tests for all six rule types.
- **Decision:** this closes the six-rule model/codec source inventory only. Full BookSource/Book/BookChapter/ReadConfig model closure, runtime typed execution, Explore/Review caller parity and dynamic differential validation remain open. Task R3-S0-MODEL-BOOKSOURCE-RULES-001 source-audit slice complete; Stage-0 remains in_progress/not accepted; Stage-1 remains NO-GO. No production code, tests, Gradle, workflow, database/schema, UI or CI were changed/run.

## 24.51 Runtime-3.0 Stage-0 — outer BookSource entity, Room identity/schema and import/catalog persistence semantics (2026-11-24)

- **Task:** R3-S0-MODEL-BOOKSOURCE-ENTITY-002; registered in overview §4.2 before inspection.
- **Snapshot:** Badcat branch `integration/legado-upstream-runtime` at `9ddbc024f07b593a62bd14472c8f3416f73d6a0e`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Deduplication:** §24.37/§24.45/§24.46 already cover BaseSource identity, source-key namespaces and runtime-state invalidation; §24.50 covers the six nested rule classes and resolver's typed-field loss. This section records the outer BookSource field/schema contract, Room ownership and additional import/catalog deltas only.

### Pinned upstream BookSource outer model

Upstream `app/src/main/java/io/legado/app/data/entities/BookSource.kt:24–104` is a `@Parcelize`, Room `@Entity(tableName="book_sources")`, `@TypeConverters(BookSource.Converters::class)` data class implementing `BaseSource`. It declares 33 top-level properties (the six nested rule properties are already inventoried in §24.50):

| Fields | Kotlin type / default | Room / semantic notes |
|---|---|---|
| bookSourceUrl | String = `""` | `@PrimaryKey`; canonical source identity and BaseSource key |
| bookSourceName | String = `""` | required display name |
| bookSourceGroup | String? = null | comma/group membership text |
| bookSourceType | Int = 0 | `@BookSourceType.Type` |
| bookUrlPattern | String? = null | book URL matching |
| customOrder | Int = 0 | Room default 0; ordering key |
| enabled | Boolean = true | Room default 1 |
| enabledExplore | Boolean = true | Room default 1 |
| jsLib | String? = null | inherited BaseSource property |
| enabledCookieJar | Boolean? = true | nullable inherited BaseSource property; Room column default is 0, not Kotlin default |
| concurrentRate | String? = null | inherited BaseSource request-rate setting |
| header | String? = null | inherited BaseSource header |
| loginUrl / loginUi | String? = null | inherited BaseSource login URL/UI |
| loginCheckJs | String? = null | login state script |
| coverDecodeJs | String? = null | cover decode script |
| bookSourceComment | String? = null | comment |
| variableComment | String? = null | source-variable description |
| lastUpdateTime | Long = 0 | ordering/update metadata |
| respondTime | Long = 180000L | response-time metadata |
| weight | Int = 0 | smart ordering |
| exploreUrl / exploreScreen | String? = null | discovery URL/filter |
| searchUrl | String? = null | search URL |
| mainJs | String? = null | non-empty value switches to pure JS source flow |
| eventListener | Boolean = false | Room default 0 |
| customButton | Boolean = false | Room default 0 |

The six typed nullable rule properties are `ruleExplore`, `ruleSearch`, `ruleBookInfo`, `ruleToc`, `ruleContent`, `ruleReview`; their exact model contracts are in §24.50. The upstream `BaseSource` contract at `BaseSource.kt:65–102` contributes the mutable shared JS-facing properties `concurrentRate/loginUrl/loginUi/header/enabledCookieJar/jsLib`, `getTag()`, `getKey()` and `getSource() = this`; the full JS/login/request behavior is already audited in §§24.37/24.45 and is not re-counted here.

### Identity, entity methods and Room persistence

- `BookSource.kt:27–30,33–34,110–120`: `bookSourceUrl` is the primary key; an additional non-unique index is declared on the same column. `getKey()` returns `bookSourceUrl`; `equals/hashCode` use only this URL. A name, group, base host or rule change does not create a new entity identity.
- `BookSource.kt:122–155`: search/explore/detail/TOC/content getters lazily create and assign empty typed rule instances when absent; Review's getter is commented out (157–162). Batch support and maximum size are derived from typed ContentRule (246–258; semantic field details in §24.50).
- `BookSource.kt:306–354`: all six typed rule properties have explicit Room string converters through pinned GSON. The database stores rule columns as TEXT, while runtime entity properties are typed nullable objects.
- Pinned exported schema `app/schemas/io.legado.app.data.AppDatabase/113.json` defines the version-113 `book_sources` table with 33 columns, `bookSourceUrl` TEXT NOT NULL primary key, and a non-unique index on that same key. It records `enabled`/ `enabledExplore` default 1, `customOrder` default 0, `enabledCookieJar` nullable INTEGER default 0, `eventListener`/ `customButton` default 0; other non-null numeric fields have no SQL default in this snapshot. This is distinct from Kotlin constructor defaults (notably enabledCookieJar=true vs SQL default 0); a future Room port must preserve both construction and migration/database insertion semantics rather than collapsing them.
- Pinned `AppDatabase.kt:70–90, 90–153` uses Room version 113, includes BookSource as an entity and BookSourcePart as a database view, and registers the migration chain. Relevant BookSource evolution in `DatabaseMigrations.kt`: 19→20 adds bookSourceComment (220); 34→35 adds concurrentRate (389); 35→36 adds loginUi/loginCheckJs (393–396); 37→38 adds respondTime with SQL default 180000 (408–411); 64→65 deletes the obsolete enabledReview column (489–494). Migration 105→106 creates book_source_check_states keyed by bookSourceUrl with FK to book_sources and ON DELETE CASCADE, seeds NEEDS_CHECK rows, and recreates the book_sources_part view (108–121). This is source/schema evidence; no migration was executed in this audit.
- `BookSourceDao.kt:254–277,288–306,344–370,382–406`: lookup and mutation key on bookSourceUrl; insert uses REPLACE; insert transaction also writes check-state rows; update transaction compares `checkContent()` and resets check state when rule-relevant content changes; deletion and enable/order mutations are DAO operations. `SourceHelp.kt:91–95,141–154,174–185` adds source-variable/shared-global cleanup on deletion and adjusts duplicate/extreme order values asynchronously after import.
- Upstream `BookSourcePart.kt:11–32` is a Room view, not the canonical entity. It projects identity/name/group/order/enabled/discovery/login/update/response/weight/event/type/JS and check-state fields from book_sources plus book_source_check_states; its URL-based equality and getBookSource lookup are at 68–85. Do not mistake this lightweight projection for the complete runtime model.

### Host DTO, codec and catalog ownership

- Host `ReadingEngine.kt:94–134` defines an immutable platform-neutral DTO. It carries all currently enumerated BookSource outer values, mapping `bookSourceUrl → id` and also `baseUrl`, `bookSourceName → name`, `bookSourceGroup → group`; `concurrentRate` and `customOrder` are appended. The six rule fields are JSON strings, not typed upstream rule objects (§24.50). The host DTO does not implement `BaseSource`, `Parcelable`, Room Entity, upstream equality/hash behavior, lazy typed-rule getters or the upstream entity methods.
- Host `LegadoBookSourceCodec.kt:14–50,51–87,254–304` keeps selected top-level projections plus rawJson, then maps to the immutable DTO. Unknown fields are semantically retained on export because `encode` writes rawJson (203–221); this does not make the raw JSON an executable typed entity.
- Host import validation at `LegadoBookSourceCodec.kt:307–387` is stricter than pinned Gson's global coercion adapters. Upstream `GsonExtensions.kt:32–55,140–151` registers `StringJsonDeserializer`, which converts any JSON primitive to its string form, null to null, and object/array to compact JSON text; host validates known string fields as JSON string or null only (rejecting numeric/boolean/object/array values). Upstream `IntJsonDeserializer` (159–177) accepts numeric primitives via `toInt()` and otherwise returns null; host validates integer fields as exact integral JSON numbers (including range checks), rejecting numeric strings and fractional numbers rather than allowing upstream's adapter/fallback path. This is a confirmed import-policy delta; no dynamic fixture comparison was run.
- Host `FileBookSourceCatalog.kt:19–34,36–50,53–93,95–149,151–167,182–218,221–225` owns one app-private `filesDir/legado-book-sources.json` file through Android AtomicFile and a process-wide Mutex. It decodes a bounded catalog, replaces existing entries by sourceId, writes atomically, clears runtime state after replacement/removal, toggles enabled by editing rawJson, and exports ordered rawJson. Its 5 MiB / 2,000-source / 64-level nesting / 512-field bounds are deliberate host safety policy, not upstream Room limits. Catalog state is file-backed, not Room-backed; no BookSource DAO, relational check-state table or Room migration port exists in this host path.
- The host's `orderBookSourcesForCatalog` sorts by customOrder (221–225); the pinned DAO's `all` query orders customOrder ASC (BookSourceDao.kt:205–206). Host test `FileBookSourceCatalogOrderTest.kt:7–32` verifies stable input order for ties; upstream SQL does not specify a tie-breaker, so exact tie ordering is not guaranteed upstream.
- Host duplicate source IDs are rejected during decode (codec 187–194); upstream JSON parser accepts source objects/arrays and validates nonblank URL (BookSourceImport.kt:88–125), while DAO insert uses REPLACE keyed by URL (BookSourceDao.kt:276–292). Thus duplicate IDs within one import document have a different outcome: host rejects the document; upstream parsing can pass entries onward and Room replacement resolves key collisions by insertion order.
- Upstream import supports `sourceUrls` manifests and recursively fetches each URL, and supports raw JS source text via `JsSourceConfig.extract` (`BookSourceImport.kt:88–125`; `ImportBookSourceViewModel.kt:194–229,259–294`). Host codec can parse a manifest through `decodeImportDocument`, but the actual `FileBookSourceCatalog.importJson` path calls `decode()` with manifests disallowed (codec 114–120); both local and network UI import call this catalog method (`SourceCatalogActivity.kt:363–429,467–500`). Consequently the current UI rejects sourceUrls manifests instead of recursively importing them, and accepts only JSON source objects/arrays—not the upstream raw-JS-source import format.
- Upstream import also presents candidate selection and optional keep-local-name/group/enabled behavior before transactional insert (`ImportBookSourceViewModel.kt:129–183)); host catalog currently replaces imported source JSON by sourceId without these merge/selection controls. Upstream SourceHelp filters known 18+ hosts before insert (141–169); no equivalent filter appears in the inspected host catalog import path. These are import-workflow deltas, not claims about the six rule model semantics.

### Compatibility classification

- **Confirmed parity:** the host projection includes the inspected current BookSource outer scalar fields and maps URL/name/group into its transport DTO; rawJson export preserves unknown source-level fields. Source identity is represented by the upstream bookSourceUrl value, consistent with §24.46's source-key mapping.
- **Confirmed structural gap:** host has no typed BookSource implementing BaseSource and no Room entity/converter/DAO/check-state view path. A file catalog and immutable DTO are not equivalent to the upstream entity + DAO lifecycle.
- **Confirmed policy deltas:** strict host JSON type validation, duplicate-ID rejection, missing sourceUrls recursive import and raw-JS import, and absent upstream import-selection/keep-local metadata behavior.
- **Confirmed data-model nuance:** Kotlin enabledCookieJar default true and Room SQL default 0 coexist upstream; migration/database semantics cannot be inferred from the Kotlin constructor alone.
- **Not claimed:** no Room migration was run; no JSON differential runtime test or Android BookSource E2E was run; no claim that every BookSource method or every historical migration was exhausted beyond the pinned source/schema evidence cited here.
- **Required closure:** (a) choose whether Runtime-3.0 will reuse the actual pinned BookSource/BaseSource/Converters/DAO closure or provide a fully typed, differential-equivalent adapter; (b) preserve URL primary-key/equality/REPLACE/check-state/variable/global cleanup semantics and define the one canonical host data owner; (c) reconcile Kotlin vs SQL defaults, Gson coercion and null/malformed field handling; (d) implement or explicitly block sourceUrls/raw-JS import and import merge/selection behavior; (e) provide a pinned version-113 schema/migration compatibility plan without prematurely freezing Badcat's own schema; (f) add all-field entity projection tests, coercion fixtures, duplicate-ID tests, sourceUrls recursion tests, import merge tests and transaction/restart/cleanup fault tests.
- **Decision:** outer BookSource source-audit slice complete. This does not close Book/BookChapter/ReadConfig models, BaseSource runtime behavior, the complete Room dependency closure, or dynamic import/data-owner parity. Task R3-S0-MODEL-BOOKSOURCE-ENTITY-002 complete as a read-only audit slice; Stage-0 remains `in_progress/not accepted`; Stage-1 remains `NO-GO`. No production source, tests, Gradle, workflow, database/schema, UI or CI were changed/run.

## 24.52 Runtime-3.0 Stage-0 — Book, BookChapter, ReadConfig and reading-progress entity/data closure (2026-11-24)

- **Task:** R3-S0-MODEL-BOOK-CHAPTER-003; registered in overview §4.3 before source inspection.
- **Snapshot:** Badcat branch `integration/legado-upstream-runtime` at `abb703f90efdb899bd1ada7419d020408bb27f7a`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Deduplication:** §24.49 already records BaseBook/Book/BookChapter RuleData variable ownership, big-variable storage namespaces, chapter/book variable precedence and typed AnalyzeRule context. Existing chapter-index/cache sections cover the host's identity-safe cache token. This section adds the full persisted entity fields, Room constraints, Book-level ReadConfig/progress contract, DAO preservation transactions and the current Badcat data-owner comparison; it does not repeat variable lookup precedence.

### Pinned Book entity and ReadConfig

Upstream `Book.kt:33–130` is a Room `books` entity, `@Parcelize`, with `Book.Converters`; it implements BaseBook. Its 34 persisted properties are:

| Property | Type / Kotlin default |
|---|---|
| bookUrl | String = `""` |
| tocUrl | String = `""` |
| origin | String = `BookType.localTag` |
| originName | String = `""` |
| name / author | String = `""` |
| kind | String? = null |
| customTag | String? = null |
| coverUrl / customCoverUrl | String? = null |
| intro / customIntro | String? = null |
| charset | String? = null |
| type | Int = `BookType.text` |
| group | Long = 0 |
| latestChapterTitle | String? = null |
| latestChapterTime / lastCheckTime / durChapterTime | Long = `System.currentTimeMillis()` |
| lastCheckCount / totalChapterNum / durChapterIndex / durVolumeIndex / chapterInVolumeIndex / durChapterPos | Int = 0 |
| durChapterTitle | String? = null |
| wordCount | String? = null |
| canUpdate | Boolean = true |
| order / originOrder | Int = 0 |
| variable | String? = null |
| readConfig | Book.ReadConfig? = null |
| syncTime | Long = 0L |
| persistedCoverUrl | String? = null |

- **Identity and constraints:** `Book.kt:35–38,41–43,132–141` makes bookUrl the primary key and adds a unique composite index on (name, author); equals/hashCode use bookUrl only. Thus URL identity is canonical, but name+author is also a database uniqueness constraint and can replace a different URL on import.
- **Inherited contract:** `BaseBook.kt:8–18` exposes name, author, bookUrl, kind, wordCount, variable, infoHtml and tocHtml; persisted properties overlap the Book columns, while infoHtml/tocHtml are transient/runtime fields. §24.49 already records BaseBook RuleData variable behavior and external big-variable ownership.
- **Transient/computed fields:** `Book.kt:143–168` excludes lazy variableMap, infoHtml, tocHtml, downloadUrls and folderName from Room/parcel persistence; lastChapterIndex is computed from totalChapterNum.
- **ReadConfig:** nested `Book.ReadConfig` at `Book.kt:499–530` has 21 fields: reverseToc, reverseTocDisplay, tocExpanded, pageAnim, reSegment, imageStyle, useReplaceRule, delTag, ttsEngine, splitLongChapter, readSimulating, startDate, startChapter, dailyChapters, openCredits, closeCredits, playMode, playSpeed, useGlobalAudioSkip, manualReplaceRuleIds and paragraphAnchor. ParagraphAnchor has (chapterUrl, itemId, chapterPosition). Defaults include false/true/null values, dailyChapters=3, playSpeed=1.0f, empty manualReplaceRuleIds and null paragraphAnchor.
- **Default nuance:** `Book.kt:197–204` lazily materializes a missing config as `ReadConfig(useGlobalAudioSkip = true)`, although the nested data-class constructor default is false (line 526). This lazy getter behavior is part of the upstream contract; a null readConfig column is not equivalent to blindly constructing ReadConfig().
- **Converters:** `Book.kt:532–539` serializes ReadConfig as JSON TEXT via GSON. The version-113 Room schema stores `readConfig` and `variable` as nullable TEXT.
- **Version-113 schema:** `app/schemas/io.legado.app.data.AppDatabase/113.json` has 34 Book columns, primary key bookUrl, unique index (name,author). SQL defaults include bookUrl/tocUrl/originName/name/author, type/group/progress counters and order values; canUpdate defaults 1. The three Kotlin timestamp defaults above are current time, whereas the SQL defaults in this schema are 0. Constructor defaults and DB migration defaults must remain separately represented.

### Pinned BookChapter entity and BookProgress

- **BookChapter fields:** upstream `BookChapter.kt:29–60` declares 17 fields: url:String="", title:String="", isVolume:Boolean=false, baseUrl:String="", bookUrl:String="", index:Int=0, isVip:Boolean=false, isPay:Boolean=false, resourceUrl:String?=null, tag:String?=null, wordCount:String?=null, start:Long?=null, end:Long?=null, startFragmentId:String?=null, endFragmentId:String?=null, variable:String?=null, imgUrl:String?=null.
- **Database identity:** `BookChapter.kt:30–41` and schema 113 define composite primary key (url,bookUrl), non-unique index bookUrl, unique index (bookUrl,index), and FK bookUrl→books.bookUrl with ON DELETE CASCADE. A chapter URL may therefore exist in different books; index is unique within a book.
- **Domain equality nuance:** despite the composite Room identity, `BookChapter.kt:116–123` implements equals/hashCode using url only. This is the pinned domain equality contract and is not identical to the SQL primary key; caller effects of URL-only equality across books have not been dynamically exercised.
- **Chapter mutation:** `BookChapter.kt:69–105` updates imgUrl/lyric/danmaku and serializes variable changes; `update()` persists through BookChapterDao unless deferUpdates is true (88–95). Big variables use the bookUrl + chapter URL namespace already described in §24.49.
- **DAO:** `BookChapterDao.kt:13–53` queries by bookUrl and index/title, inserts with REPLACE, updates rows, updates resource metadata/variable and deletes all chapters by bookUrl. Foreign-key cascade also removes chapters when the parent Book row is deleted.
- **BookProgress:** `BookProgress.kt:3–19` is a derived six-field snapshot (name, author, durChapterIndex, durChapterPos, durChapterTime, durChapterTitle), not an independent Room entity. Reading progress is principally stored in the Book row.
- **Book DAO preservation semantics:** `BookDao.kt:125–129,208–244,272–287,327–403` supports lookup by bookUrl and name+author; insert uses REPLACE under a transaction, and the unique name+author collision can replace a different bookUrl while moving the newer memo. `upsertPreservingVariable` retains existing variable data by URL or name+author; updatePreservingCustomCoverUrl and updatePreservingReadConfig preserve user overrides/config; replace carries custom/persisted cover and ReadConfig and migrates memo data; delete removes memo then the Book row (and chapter FK cascade). These are more than simple DTO CRUD.
- **Migration evidence:** pinned `DatabaseMigrations.kt:171–186` (13→14) rebuilds books and adds the unique name+author index; 21→22 (230–251) rebuilds books to add readConfig while preserving old columns; 22→23 (254–257) adds chapter baseUrl; 29→30 (320–329) adds EPUB fragment IDs; 39→40 (420–424) adds VIP/pay flags; 42→43 (443–446) adds isVolume. Version-113 schema is the current endpoint; no migration was executed here.

### Badcat host data projection and ownership

- **No persisted Book entity:** host source tree has `BookCandidate` and `BookDetail` value DTOs in `ReadingEngine.kt:136–159`, but no corresponding Book entity/DAO. They carry search/detail display fields, not the 34 persisted Book fields, unique (name,author) constraint, origin/local-file semantics, mutable progress, user cover/intro/tag overrides, variable or ReadConfig.
- **Host chapter DTO:** `ReadingEngine.kt:161–173` carries only index, title, URL, isVip, isPay, tag and baseUrl. It omits upstream isVolume, resourceUrl, wordCount, start/end, startFragmentId/endFragmentId and imgUrl; bookUrl is supplied separately to operations rather than being part of the chapter object. Variable/bookUrl scope gaps are already recorded in §24.49. The host DTO is a data class with generated all-property equality, unlike upstream BookChapter's URL-only equals/hashCode.
- **Host chapter cache is not a chapter entity:** `ChapterContentCache.kt:3–51` stores parsed ChapterContent and a timestamp with a host key (sourceId, bookUrl, chapterIndex); `FileChapterContentCache.kt:32–94,96–177` writes bounded AtomicFile JSON containing title/text/next/previous page URLs and savedAt (the index path also keeps chapterUrl as a guard). It does not persist the 17-field BookChapter row, TOC metadata or per-chapter variables.
- **Reading progress boundary:** host `ReadingProgressStore.kt:3–21` exposes only lastChapterUrl and chapterScrollY operations. `BadcatReadingStateRepository.kt:18–23,44–62,133–199,205–234` stores these, plus bookmark/history records, in SharedPreferences. It has no typed Book, chapter index/volume position, durChapterPos, durChapterTime, totalChapterNum, ReadConfig or BookProgress snapshot.
- **Favorite identity:** `FavoritesActivity.kt:25–27,47–90,168–176` stores FavoriteBook JSON in SharedPreferences with (sourceId,bookUrl) identity and name/folder/cover/status/addedAt. Upstream Book identity is bookUrl alone and its name+author unique index; favorites are a separate host product list, not an upstream Book row or shelf-state equivalent.
- **Settings:** `ReaderSettingsActivity.kt:16–19,65–98,104–115,149–152` persists global reader controls in its own SharedPreferences. It is not an implementation of per-Book `Book.ReadConfig` or its converter/restore semantics.
- **Data ownership conclusion:** Badcat currently has separate preference owners for favorites, history/progress/bookmarks and global reader settings, plus a separate file cache. There is no single host Book/BookChapter/ReadConfig entity owner corresponding to the upstream relational Book + chapter + config model. This is a confirmed structural difference; migration/transaction/restart parity is not demonstrated.

### Compatibility classification

- **Confirmed source facts:** upstream Book has 34 persisted fields and 21-field nested ReadConfig; BookChapter has 17 persisted fields; BookProgress is a derived six-field projection. Room identity, unique indexes, FK cascade, converter and DAO preservation transactions are source-confirmed by pinned code/schema.
- **Confirmed host gaps:** no persisted Book entity or typed ReadConfig; reduced BookChapter DTO; URL-only progress representation; separate SharedPreferences/file data owners; no upstream relational transaction/constraint/migration path.
- **New nuance:** BookChapter domain equality uses URL only while Room primary key is (url,bookUrl); host data-class equality differs. Book name+author uniqueness can replace another URL, which the host favorites identity does not model.
- **Not claimed:** no database migration, restart/fault injection, ReadConfig serialization differential, entity CRUD differential or Android reader E2E was run; no claim every host preference feature is absent beyond the inspected files.
- **Required closure:** (a) decide whether to reuse pinned Book/BookChapter/ReadConfig/DAO/converters or build typed adapters proven differential-equivalent; (b) preserve both Book identity constraints, memo/variable/cover/ReadConfig retention and transactional replace/delete; (c) preserve all 17 chapter fields, composite key, unique per-book index, FK cascade and update/defer semantics; (d) map URL progress to chapter index/position/volume/time and reconcile host source-scoped identity with Book.bookUrl; (e) implement per-book ReadConfig and ParagraphAnchor serialization/default/lazy getter behavior; (f) define one authoritative owner and migration/restart strategy for favorites/history/bookmarks/progress/chapter rows/cache without double-writing; (g) add schema-113 entity projection, field/null/default, equality/identity, DAO transaction and restart/failure differential tests.
- **Decision:** task R3-S0-MODEL-BOOK-CHAPTER-003 source-audit slice complete. Stage-0 remains `in_progress/not accepted`; Stage-1 remains `NO-GO`. No production source, tests, Gradle, workflow, database/schema, UI or CI were changed/run.


## 24.53 Runtime-3.0 Stage-0 — Cache / BookCacheInfo / CacheManager storage and lifecycle closure (2026-11-24)

- **Task:** R3-S0-CACHE-MODEL-004; registered in overview §4.4 before source inspection.
- **Inspection snapshot:** Badcat branch `integration/legado-upstream-runtime` at `ba578c7c6263aa30745c137e8c5e0eff89f44291`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Deduplication:** §24.37 already covers BaseSource scope bindings and runtime cache lifecycle; §24.48 covers CacheManager exposure to AnalyzeRule/AnalyzeUrl; historical audit `legado-runtime-3.2f.12-cache-mutation-invalidation-audit.md` inventories generic CacheManager callers, host invalidation gaps and chapter save-fence defects. §24.52 already states that host FileChapterContentCache is not a BookChapter entity. This section adds the pinned Cache entity/schema/DAO contract, exact ACache namespace/TTL behavior, BookCacheInfo projection and startup/book-cache cleanup ownership. It does not repeat the generic caller inventory or prior host invalidation findings.

### Pinned Cache entity, Room schema and DAO

- `Cache.kt:8–13` is a Room entity named `caches`: primary key `key:String=""`, nullable `value:String?`, and `deadline:Long=0L`. Schema `app/schemas/io.legado.app.data.AppDatabase/113.json` confirms three columns (`key` TEXT NOT NULL, `value` TEXT nullable, `deadline` INTEGER NOT NULL), primary key `key`, and unique index `index_caches_key`. This is a generic key/value/deadline table, not a chapter-content entity.
- `CacheDao.kt:12–22` supports full-row lookup, time-filtered value lookup (`deadline=0 OR deadline>now`), REPLACE insert and per-key delete. `CacheDao.kt:44–45` deletes rows with positive deadlines strictly less than `now`; an expired row read is hidden but not itself removed by the CacheManager read path.
- `CacheDao.kt:24–42` defines a prefix allowlist for runtime-source backup: `v_`, `userInfo_`, `loginHeader_`, `sourceVariable_`, `infoMap_`. Its source-delete predicate removes `v_<source>_*` and exact `userInfo_<source>`, `loginHeader_<source>`, `sourceVariable_<source>`, `infoMap_<source>` keys. These are SQL key conventions, not foreign-key relationships to BookSource.
- `App.kt:111–123` calls `cacheDao.clearDeadline(System.currentTimeMillis())` during asynchronous application initialization, independently of the optional one-day search-result cleanup preference. Thus generic Room cache expiry cleanup is startup-driven in the inspected path; ordinary get() expiry checks do not prove immediate row reclamation.

### CacheManager and ACache semantics

- `CacheManager.kt:14–23` has a process-level memory LRU capped at 50 MiB by `value.toString().memorySize()`; the separate QueryTTF cache is a four-entry LRU. `CacheManager.put(key,value,saveTime)` converts positive seconds to an absolute millisecond deadline; zero means no deadline. Non-byte values are stringified and REPLACE-persisted in Room. Permanent values are also put in memory; timed writes remove the matching memory entry so a stale permanent in-memory value cannot shadow a timed replacement (`CacheManager.kt:55–75`).
- `CacheManager.get(key)` checks memory first, then Room, and promotes only permanent string values into memory (`CacheManager.kt:91–105`). `get(key, onlyDisk=true)` bypasses memory (`108–117`); numeric helpers can read typed in-memory values before parsing the disk string (`119–144`). `delete(key)` removes the Room row, memory entry and ACache file with the same key (`159–164`). These three storage surfaces form one public generic delete contract.
- ByteArray values and `putFile/getFile/getByteArray` use ACache rather than the Room value column (`CacheManager.kt:64,147–157`). Pinned `ACache.get()` defaults to app-private `cacheDir/ACache`, a 50,000,000-byte capacity and `Int.MAX_VALUE` count limit (`ACache.kt:32–48)); per-process manager instances are cached by directory + PID (`57–64`). String and byte TTL metadata is embedded in the stored payload, measured in seconds; reads strip the metadata and remove a file when it is recognized as expired (`ACache.kt:96–148,239–286,499–545`). ACache is therefore distinct from Room deadline rows and from BookHelp's downloaded chapter files.
- `WebCacheManager` is a JavaScript-interface facade delegating the generic CacheManager methods (`CacheManager.kt:167–175` and following); the separate WebView and Rhino exposure/caller inventory is already captured in §24.37/§24.48 and the historical §3.2f.12 audit.
- Existing upstream `CacheManagerTtlContractTest.kt` is a source-contract test for seconds conversion, memory invalidation/promotion, disk-only reads and synchronization. It does not establish Room/ACache device persistence, process-death recovery or runtime behavior under concurrent Android callers. No tests were run in this read-only task.

### BookCacheInfo is a cleanup projection, not a cache entity

- `BookCacheInfo.kt:8–19` is a five-field projection (`bookUrl`, `name`, `origin`, `originName`, `type`) plus a cleanup snapshot pairing these rows with optional full image Books. It is not persisted and has no cache payload, TTL or DAO identity of its own.
- `BookCacheInfo.getFolderName()` sanitizes the book name, takes at most nine characters, and appends a 16-character MD5 of bookUrl (`BookCacheInfo.kt:21–25`). EPUB classification uses legacy origin tags when type is zero, otherwise the local type bit, and checks the .epub suffix case-insensitively (`27–37`).
- `BookDao.kt:156–168` builds the cleanup snapshot transactionally: a lightweight projection of all books plus full image-book rows only when requested. `BookHelp.clearInvalidCache()` uses this snapshot to retain valid per-book folders and EPUB extraction folders, prune orphaned directories, clear archive temp files and stale exported JSON files (`BookHelp.kt:171–200`). Image cleanup separately retains image resources for a configured chapter window (`BookHelp.kt:203–236`). `BookHelp.clearCache(book)` deletes only that book's downloaded-content directory; it is not generic CacheManager.delete and does not imply deletion of Room Cache rows or source JS caches (`140–149`).
- This cleanup path is about downloaded chapter/image/EPUB resources tied to Book rows. It must remain separate from generic source key/value cache, source script file cache, and the host's parsed chapter-content cache.

### Host mapping and net-new compatibility findings

- Host `FileChapterContentCache.kt` stores parsed/purified chapter content in bounded AtomicFile JSON under `filesDir/legado_chapter_cache`; keys are sourceId + bookUrl + chapterIndex (with chapterUrl guard), TTL is enforced by the content service, and file LRU budget is 64 MiB with a 4 MiB entry cap. It is neither a port of the pinned generic `Cache` Room entity nor ACache's general typed/file API.
- Host `RhinoSourceCacheExtensions.kt` provides a separate sourceId-namespaced string KV/file/memory surface with bounded keys/values/TTL. Its documented key namespace and stores differ from the upstream global CacheManager key namespace. Historical §3.2f.12 already records that host delete/clear and source-removal lifecycle do not fully match upstream CacheManager and source invalidation; no duplicate gap is restated here.
- The host cache's bounded AtomicFile chapter payload is a deliberate, distinct storage design, but it does not establish parity for arbitrary source cache values, typed in-memory values, Room deadline cleanup, ACache ByteArray/file behavior, WebCacheManager bindings or runtime-source backup.
- **Confirmed source facts:** pinned Cache row fields/schema/index, DAO deadline and source-prefix queries, CacheManager's Room + memory LRU + ACache split, ACache private cache directory/default size/TTL envelope, BookCacheInfo projection/folder naming, and BookHelp cleanup snapshot/caller.
- **Confirmed host structure:** separate parsed chapter content cache and source-scoped Rhino KV/file cache; neither implements the pinned generic Room Cache/ACache contract as a whole.
- **Static lifecycle risks already recorded elsewhere:** source removal/replacement does not yet provide a complete host invalidation path; generic `cache.delete` parity across KV + file stores and chapter cache invalidation remain incomplete (§24.37/§24.46 and historical §3.2f.12). This audit does not claim a new caller absence beyond those prior inspected paths.
- **Dynamic-unverified:** no Room schema migration, CacheManager/ACache TTL integration test, process-restart or process-death recovery test, cache backup/restore differential, concurrent cache mutation test, or Android source-script E2E was run. Static tests in the upstream repository were read but not executed.
- **Required closure:** (a) choose whether Runtime-3.0 reuses the pinned Cache/CacheDao/CacheManager/ACache behavior or supplies a fully typed differential-equivalent host port; (b) preserve zero/permanent vs positive/seconds TTL, memory promotion and timed-write invalidation, disk-only semantics, typed-memory numeric reads, delete across all stores and startup expiry cleanup; (c) define exact source-key cleanup/backup ownership and login/cookie state boundaries; (d) keep BookCacheInfo folder/EPUB cleanup semantics separate from generic source cache and parsed chapter content; (e) add fixed fixtures and Android persistence/restart/concurrency tests for Room + ACache + source cache + chapter cache; (f) close host cross-instance save-fence, source removal/reimport and mutation invalidation items already identified in §3.2f.12 before any Stage-1 authorization.
- **Decision:** task R3-S0-CACHE-MODEL-004 source-audit slice complete. Generic CacheManager/ACache compatibility and host cache invalidation remain NOT MET; chapter-content cache remains PARTIAL. Stage-0 remains `in_progress/not accepted`; Stage-1 remains `NO-GO`. No production source, tests, Gradle, workflow, database/schema, UI or CI were changed or run.


## 24.54 Runtime-3.0 Stage-0 — Badcat production entrypoint, engine-construction and host-store caller closure (2026-11-24)

- **Task:** R3-S0-BADCAT-CALLER-CLOSURE-005; registered in overview §4.5 before production caller inspection.
- **Inspection snapshot:** Badcat branch `integration/legado-upstream-runtime` at exact HEAD `c8cdbf434125c4c2e26f40a625832d5bfe72d824`; recursive tree returned 569 entries (`truncated=false`), including 90 app production Kotlin/Java files. Pinned upstream remains `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Deduplication:** §15/§15.1 already identifies the host stores and projection boundary; §24.37–§24.53 already audits individual upstream contracts and selected host adapters. This slice adds the concrete reachable Badcat UI→factory→service→adapter/store construction graph, identifies independent runtime construction sites and UI-only/legacy paths, and does not repeat per-symbol upstream semantics, cache schema or model field inventories.

### Badcat product navigation and runtime entrypoints

- **Launcher/search entry:** `app/src/main/AndroidManifest.xml:14–22` registers SourceCatalogActivity, SourceLoginUiV2Activity, SourceSearchActivity, SourceBookActivity and SourceChapterActivity as internal activities; MainActivity is the exported launcher. `MainActivity.kt:111–153,180–207` routes the home search bar to SourceSearchActivity and recent-history rows to SourceChapterActivity. Thus the Badcat product shell remains the entrypoint; these screens are not launched by a Legado product UI.
- **Search execution:** `SourceSearchActivity.kt:104–108` constructs `BadcatReadingEngineFactory(applicationContext).create()`. `loadSources()` calls `engine.sources()`, then filters enabled sources and search-capable novel sources (`284–320`). Search plans operations through `SourceSearchPlanner` and calls `engine.searchPage` or `engine.explore` (`358–446`). It runs up to six operations concurrently, applies a 15-second per-operation timeout, translates timeout/other exceptions to host EngineResult failures, and propagates cancellation (`358–446`). The Activity cancels the active Future and shuts down its worker on destroy (`854–878`). These are host orchestration policies, not evidence of pinned WebBook/AnalyzeRule equivalence. Comic mode explicitly returns a not-connected PPRule notice instead of invoking novel sources (`358–380`).
- **Book detail / TOC:** `SourceBookActivity.kt:62–74` creates another factory and engine. `loadBook()` concurrently calls `bookDetailWithFallback` and `chaptersIncrementalWithFallback`, rendering incremental TOC callbacks (`254–330`). Chapter selection passes sourceId/bookUrl/chapter URL/index/title to SourceChapterActivity (`488–498`). Favorites are written through the separate `FavoriteBooks` store, not through the engine catalog or a Book entity (`91–105`).
- **Reader / chapter content:** the nested `SourceChapterActivity` creates a third factory/engine instance (`556–620`). It resolves legacy URL-only launch intents against a freshly loaded TOC and refuses ambiguous/missing matches; indexed intents call `chapterContent` with an index-bearing host BookChapter (`773–854`). Chapter changes call the same service path (`1017–1066`). The product WebView is a Badcat reader rendering surface with JavaScript disabled and local HTML; its onPageFinished callback applies Badcat purification and restores scroll/bookmark position (`605–690`). It is not the upstream hidden source-rule BackstageWebView audited in §24.44.
- **Catalog and source management:** SourceCatalogActivity constructs FileBookSourceCatalog with a callback that clears only its own lazily constructed RhinoSourceScriptRuntime (`SourceCatalogActivity.kt:39–69`). Its Explore action constructs a fresh BadcatReadingEngineFactory/engine for each invocation (`301–320`); import/export and enable/remove operations use the file catalog. Login UI v2 navigation opens SourceLoginUiV2Activity (`225–255`). The catalog screen is the temporary utility UI already identified in §15; its manifest registration and reachable import/export controls are facts, not product UI parity acceptance.
- **Login UI v2:** SourceLoginUiV2Activity independently creates RhinoSourceScriptRuntime, SharedPreferencesSourceLoginSessionStore and SharedPreferencesSourceCookieJar, loads the source from a new FileBookSourceCatalog, and calls LegadoSourceLoginUiV2Service (`SourceLoginUiV2Activity.kt:24–100` and service `LegadoSourceLoginUiV2Service.kt:18–114`). This is a dedicated native host for the declarative Login UI v2 subset; the service itself documents that java.ajax, WebView and the full upstream CookieStore host are not emulated. It is not wired through BadcatReadingEngineFactory or a shared app-level runtime owner.

### Engine construction and runtime-scope topology

- `BadcatReadingEngineFactory.kt:41–60` accepts optional request-client, script-runtime, replacement and login-session dependencies, but its observed production callers use the default constructor. Each `create()` call creates a transport wrapper, a new RhinoSourceScriptRuntime (unless explicitly injected), a new gateway and new Search/Explore/Detail/TOC/Content service instances (`61–104,221–244`). The factory itself also creates per-instance catalog/replacement/purification helpers (`48–54`).
- The observed call sites are SourceSearchActivity, SourceBookActivity, SourceChapterActivity and the SourceCatalogActivity Explore action. The latter is a per-action construction. SourceCatalogActivity and SourceLoginUiV2Activity create additional Rhino runtimes directly rather than sharing the factory-created runtime. No app-scoped singleton/runtime registry or centralized lifecycle owner is established by these paths.
- **Compatibility implication:** the host has several independent per-Activity/per-action script-scope owners, while the pinned source contract relies on BaseSource/jsLib/shared-scope identity and explicit invalidation/lifecycle semantics (§24.37, §24.46, §24.47). This graph proves the construction split; it does not by itself prove a user-visible failure. Whether source globals, login state, cache invalidation and source replacement must cross these owners requires an explicit Runtime-3.0 lifetime decision and differential tests.
- The catalog invalidation callback is attached only to the SourceCatalogActivity-owned runtime (`SourceCatalogActivity.kt:63–69`); the other Activity-created runtimes are not references held by that callback. The source-removal/reimport lifecycle gap and factory default invalidation behavior were already recorded in §24.46; this section pins the actual production construction topology and does not claim a new independent cache implementation gap.

### Host persistence and navigation owners reachable from product screens

- **Catalog:** FileBookSourceCatalog uses app-private AtomicFile JSON and the source catalog screens read/write it; it remains a host projection/rawJson store, not the upstream Room BookSource entity/DAO (§24.51).
- **Login/session/Cookie:** the factory's default request path uses SharedPreferencesSourceCookieJar and SourceLoginSessionRequestClient; SourceCatalogActivity and SourceLoginUiV2Activity separately instantiate their own session/cookie/script stores. The source-management “clear login” action explicitly clears the session store and CookieJar by source key (`SourceCatalogActivity.kt:237–255`). This is multiple construction paths over preference-backed stores, not an upstream CookieDao/WebView merge proof (§24.45/§24.53).
- **Progress/bookmarks/history:** SourceChapterActivity uses BadcatReadingStateRepository for last chapter, scrollY, bookmarks and history; the repository writes SharedPreferences keys partitioned by (sourceId,bookUrl,chapterUrl) and keeps history JSON in a separate preference file (`BadcatReadingStateRepository.kt:16–240`). ReadingHistoryActivity and BookmarkActivity read/write through the repository; MainActivity independently reads the same history preference JSON directly (`MainActivity.kt:130–207`). The reader's display preferences are a separate global SharedPreferences owner. This remains URL/scroll-based host state, not the upstream Book/ReadConfig/ParagraphAnchor/BookProgress relational model (§24.52).
- **Favorites:** SourceBookActivity writes favorites through FavoriteBooks; FavoritesActivity reads and navigates back to SourceBookActivity. This is a separate Badcat product collection keyed by (sourceId,bookUrl), not upstream Book DAO shelf persistence (§24.52 and §15.1).
- **Bookmarks/history screens:** BookmarkActivity and ReadingHistoryActivity navigate to SourceChapterActivity with source/book/chapter URL data. These are product navigation projections; they do not own or instantiate a distinct reading engine.
- **Reader WebView and purification:** SourceChapterActivity owns the visible reader WebView and obtains BadcatWebViewPurificationAdapter from its factory. This path must remain Badcat product UI/reader functionality; it is not a substitute for the source-runtime hidden WebView/browser Host Port.

### Classification and limits

- **Confirmed static facts:** Badcat launcher/search/catalog/book/chapter/login UI routes; the three screen-level factory construction sites plus per-action Explore construction; direct runtime construction in catalog/login UI; host service wiring; timeout/concurrency/cancellation policy in search; host persistence owner and navigation paths described above.
- **Static risks / design gaps:** no shared app-scoped runtime owner is demonstrated; runtime scopes are constructed independently; source invalidation callback is attached to one catalog-local runtime; history has both repository and direct JSON readers; favorite/progress identities remain host-specific; temporary source catalog/login UI paths are distinct from original product UI.
- **Not claimed / dynamic-unverified:** no Android execution trace, source script global persistence test across Activity transitions, source replacement/removal test while multiple runtime instances are alive, Cookie/login restoration test across process death, cancellation race test, concurrent service/cache test, or reader E2E was run. No conclusion is made that every independent scope necessarily breaks every source; exact expected lifetime must be settled against pinned caller semantics.
- **Required closure:** (a) decide and document the Runtime-3.0 owner/lifetime for BaseSource, per-source scope, shared jsLib, login/session, Cookie and caches; (b) ensure source replacement/removal invalidates every reachable owner or make a single owner authoritative; (c) route all product readers/writers through one typed state facade while preserving Badcat product UI; (d) reconcile the direct MainActivity history read with the repository contract; (e) map host URL/scroll progress and favorites/bookmarks/history to the upstream model semantics without introducing dual writers; (f) add Android tests for cross-screen scope continuity, source removal/reimport, login/Cookie restart, cancellation and reader state restore.
- **Decision:** R3-S0-BADCAT-CALLER-CLOSURE-005 source-audit slice complete. Stage-0 remains `in_progress/not accepted`; Stage-1 remains `NO-GO`. No production source, tests, Gradle, workflow, database/schema, UI or CI were changed or run.


## 24.55 Runtime-3.0 Stage-0 — semantic parity closure matrix and architecture boundary comparison (2026-11-24)

- **Task:** R3-S0-SEMANTIC-PARITY-CLOSURE-006; registered in overview §4.6 before consolidation.
- **Inspection snapshot:** branch `integration/legado-upstream-runtime`, start HEAD `9961357eec4a8667a88f69e0397b6618a97f6195`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** consolidate existing pinned-source and Badcat caller evidence in §16 and §24.37–§24.54. No source behavior is re-inferred from task names; existing caller and store evidence is referenced rather than duplicated. “Authentic source present” is deliberately distinguished from executable/behavioral parity.

### Parity vocabulary and evidence rules

| State | Meaning | What it does not mean |
|---|---|---|
| Authentic source slice | Exact pinned upstream source file(s) are present/copied or compiled in a limited source set | Not proof of complete dependency closure, caller integration, ABI exposure or runtime behavior |
| Exact parity | Same externally observable values, defaults, ordering, errors, cancellation, lifecycle and persistence semantics established by differential evidence | No current capability is promoted to this state by source presence alone |
| Typed adapter candidate | Explicit typed host boundary exists and can potentially preserve semantics | Declaration alone is not a connected or behaviorally equivalent implementation |
| Host partial | Reachable Badcat implementation exists but is a projection, subset or policy variation | Not equivalent to pinned upstream |
| Missing/blocker | Required upstream behavior is absent, fail-closed, null/no-op, or has a known incompatible signature/return contract | Must not be represented as successful compatibility |
| Unknown/dynamic-unverified | Available static evidence cannot decide behavior, or no Android/runtime differential test was run | Not a pass and not a proven failure |

### Consolidated capability matrix

| Capability / pinned contract | Badcat reachable path and evidence | Current parity state | Known delta / unresolved dependency | Required closure evidence |
|---|---|---|---|---|
| BookSource outer entity, identity, inheritance and Room lifecycle | SourceCatalogActivity → FileBookSourceCatalog; search/detail factory reads host catalog; §24.51, §24.54 | Host partial | Host DTO/rawJson is not authentic Room BookSource/BaseSource; URL→sourceId projection; SQL/Kotlin defaults, duplicate semantics, manifest/raw-JS import differ | Field/default/null/serialization differential; source replacement and import/export transaction tests |
| Six nested rule models | Host codec/resolver and selected injected upstream model sources; §24.50, §16 | Authentic source slice + host partial | Authentic declarations do not close outer entity, nested converters, typed runtime access or all nested values; selected non-string fields are lost | Full nested object/string JSON round-trip and every field/default/null differential |
| Book / ReadConfig / ParagraphAnchor | Reader host DTO and BadcatReadingStateRepository; §24.52, §24.54 | Host partial; upstream model behavior missing | No authentic Book entity/DAO/ReadConfig; URL+scroll state is not relational progress/anchor contract | Field and behavior differential; migration/restart/anchor restore tests |
| BookChapter / TOC identity | SourceTocService and chapter DTO; indexed chapter content path; §24.52, §24.54 | Host partial | Reduced metadata/variables; no upstream entity/DAO/FK/index/update semantics; alias and duplicate URL behavior incomplete | Duplicate URL, book scoping, index, alias, reorder and persistence differential |
| RuleDataInterface / typed analysis context | SourceScriptRuntime receives Map<String,String>; §24.49 | Missing/blocker | No authentic mutable RuleData/BaseBook/Book/BookChapter context and owner-specific big-variable lifecycle | Object identity, variable precedence, large variable, batch context and cleanup differential |
| AnalyzeRule | Host parser dispatcher and SourceRule services; §24.48–§24.49, §16 | Missing/blocker for full contract | Authentic orchestration/context/replacement/cache/JS/batch lifecycle not integrated; parser subset is not AnalyzeRule | Full upstream-vs-host fixture corpus across CSS/XPath/JSONPath/regex/JS/replacement/error/context |
| AnalyzeUrl | SourceUrlResolver → SourceRequestFactory → OkHttpSourceRequestClient; §24.39, §24.48, §24.54 | Host partial | No authentic AnalyzeUrl; bodyJs/WebView, redirect/DNS/retry/cookie/error/response semantics differ or remain unproven | Request/response differential for every method, headers, body, timeout, redirect, cancellation and error |
| Rhino engine core | :legado-rhino copied engine classes and local runtime; §24.24 and §16 | Authentic source slice | Source provenance exists for selected classes; engine lifecycle, generated ABI, class shutter/wrapper and full dependency closure not established | Exact class/hash inventory, compiled ABI reflection, script/context lifecycle and sandbox tests |
| JsExtensions Java↔JS ABI | RhinoSafeSourceJavaExtensions is created by production factory; §24.38–§24.44, §24.24 | Host partial; multiple blockers | Known return/type gaps (source Map vs BaseSource, null batch context, empty navigation context), overload and exposed-member differences; upstream full surface not integrated | Compiled JVM descriptor + Rhino-visible API inventory; overload/null/error/exception differential for every reachable symbol |
| BaseSource lifecycle / source identity | Host source DTO, runtime scope, catalog invalidation; §24.37, §24.46, §24.54 | Missing/blocker | No authentic BaseSource; per-Activity/per-action runtime owners split source state; catalog callback clears only local runtime | One authoritative owner/lifetime design; cross-screen globals, replacement/removal and source identity tests |
| SharedJsScope / jsLib | Per-source Rhino scope/hash cache in host runtime; §24.47, §24.54 | Host partial | Upstream JSON URL map, shared scope, weak/LRU and refreshJSLib semantics absent or different; runtime instances isolated | Same-source cross-operation and cross-screen scope tests; library refresh and invalidation differential |
| HTTP transport and StrResponse | OkHttpSourceRequestClient + Rhino response facade; §24.38–§24.41, §24.54 | Host partial | Response body/bytes/header/multiHeaders/cookie/raw response semantics differ; timeout/batch error behavior differs | Byte/header/cookie/body lifecycle and ajax overload/batch failure differential |
| Cookie/session/login | SharedPreferencesSourceCookieJar, SourceLoginSessionRequestClient, Login UI v2 service/activity; §24.45, §24.54 | Host partial | Separate stores/owners; no complete CookieStore/DAO/WebView merge/restore; Login UI v2 is subset and separate runtime | Cookie domain/path/expiry, login callback order, session isolation, process restart and WebView synchronization tests |
| WebView/browser/verification | RuntimeHostPort request/result declarations; visible reader WebView is Badcat renderer; §24.42–§24.44 | Typed adapter candidate; production bridge missing/blocker | Source-rule hidden WebView/browser/verification caller is not Badcat reader WebView; typed declarations alone do not provide lifecycle or callback execution | Connected Android Host Ports; URL override, JS bridge, timeout, cancellation, verification and resource release tests |
| CacheManager / ACache / WebCacheManager | Host Rhino KV/file cache + FileChapterContentCache; §24.53 | Host partial; generic cache blocker | No Room Cache + memory LRU + ACache unified semantics; TTL units, typed memory values, disk-only, delete-all-stores and source cleanup differ | TTL/eviction/promotion/delete/source invalidation differential; persistence/restart/concurrency tests |
| BookCacheInfo / BookHelp cleanup | No corresponding full upstream Book DAO cleanup owner in host; §24.53 | Missing/host-specific | Host chapter cache is not downloaded book folder/EPUB/archive/image cleanup contract | Define whether behavior is required by reachable source-runtime callers; otherwise explicitly exclude with caller proof |
| Search / explore / detail / TOC orchestration | SourceSearchActivity → factory → host Search/Explore/Detail/TOC services; §24.48, §24.54 | Host partial | Host service orchestration, timeout and concurrency policy is not pinned WebBook call sequence; no full typed models/context | Multi-source pagination/error/cancellation/duplicate and fixed fixture differential |
| Content / multi-page / batch | SourceChapterActivity → host ContentService → FileChapterContentCache; §24.48, §24.54 | Host partial; batch blocker | No authentic BookContent/BatchContentContext; getBatchContext returns null; save-fence and content lifecycle gaps | Multi-page, batch context, chapter save-token/fence, cancellation and stale-write differential |
| Catalog import/export and source mutation | SourceCatalogActivity + FileBookSourceCatalog; §24.51, §24.54 | Host partial | Host JSON catalog workflows differ from upstream sourceUrls manifest/raw JS import, DAO REPLACE and mutation transactions | Import format matrix, unknown-field retention, duplicate/replace, atomic failure and invalidation tests |
| Progress/history/bookmarks/favorites | BadcatReadingStateRepository, FavoriteBooks and direct MainActivity history read; §24.52, §24.54 | Badcat-owned host behavior; not upstream parity | Multiple readers of history; product identities differ from Book/ReadConfig/ParagraphAnchor; owner consolidation open | Single typed facade, single writer/read contract, old data migration and restart/navigation tests |
| Browser UI and original product UI boundary | Badcat launcher and original product screens remain host; §24.54 | Product ownership preserved; compatibility not applicable | Temporary catalog/login screens are utility surfaces and are not proof of original UI integration | Product UI acceptance separately; retain Badcat reader WebView distinct from source hidden WebView |
| License / provenance | Copied Rhino/rule/parser sources; §24.24 and §16 | Unknown/blocker | File-level origin, modification, license notices and distribution obligations not yet closed for all imported resources | Generate exact pinned file provenance manifest, diff hashes, license inventory and distribution review |

**Matrix conclusion:** no row is currently demonstrated as exact parity. The table separates (i) authentic upstream source slices, (ii) reachable Badcat host implementations, and (iii) actual behavior equivalence; these are three different claims. The strongest current evidence is static source/caller evidence. No Android runtime trace or fixed-SHA differential suite was executed in this task.

### Architecture boundary comparison against the same evidence

| Candidate | Potential strengths | Evidence-based blockers / risks | Current assessment |
|---|---|---|---|
| A — dedicated Android Runtime module with authentic source groups + typed Host Ports | Isolates source runtime from Badcat product UI and can give one app-scoped owner to source state; typed platform boundaries can retain Android-dependent behavior | Existing :legado-runtime has no production Kotlin/Java sources and only injects seven rule models + Gson extensions; broad model/BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions closure and Gradle dependency graph are unresolved | Viable candidate, not proven buildable or complete |
| B — controlled upstream core module/source-set extraction | Best opportunity to preserve upstream call sequence, authentic models and behavior with thin compatibility facade; aligns with current Runtime-3.0 formal direction | Must prove UI/Reader exclusion without severing required callers; upstream AppDatabase/Room, Android resources, manifest, native/generated code, reflection and platform services may be entangled; exact resolved dependency graph absent | Strategic direction, but module boundary and buildability not yet proven |
| C — hybrid reuse of selected upstream files plus local host reimplementations | Can reduce immediate Android/Gradle coupling and reuse parser/Rhino/model slices incrementally | High risk of shadow DTOs, same-name simplified APIs, split lifecycle and silent semantic drift; current host architecture already demonstrates these gaps; violates hard gate if used as substitute for authentic behavior | Highest parity-drift risk; only acceptable for individually proven thin adapters, not as a substitute runtime |

**Decision:** do not select a build boundary or authorize Stage-1 from this matrix alone. The master workflow's formal Runtime-3.0 direction remains “controlled upstream core migration”; this is the target to test, not evidence that the current :legado-runtime boundary already works. A/B must be evaluated by the same exact dependency/caller/ABI/provenance proof. C may exist only as a typed, differential-equivalent platform adapter around authentic upstream behavior, never as a shadow runtime.

### Residual Stage-0 exit checklist

| Exit evidence | Current status | Required artifact / proof |
|---|---|---|
| Pinned source declaration/model/caller inventory | Partial; multiple slices audited | Machine-readable symbol/field/overload inventory, including inherited/generated/reflection-visible surface |
| Complete upstream transitive caller graph | Partial | Caller graph for BaseSource, AnalyzeRule, AnalyzeUrl, JsExtensions, SharedJsScope, Cookie/cache, WebView/browser and content batch |
| Resolved Gradle dependency closure | Not done | Exact pinned upstream resolved runtime classpath plus candidate Runtime graph; direct/transitive/native/generated/resources/manifest/R8/service-loader data |
| Reflection/JVM/Rhino ABI closure | Partial | Compiled pinned and candidate descriptors, annotations, bridge/synthetic/default methods, Rhino wrapper-visible methods and overload resolution |
| Badcat reachable production caller/store graph | Source-audit slice complete | Dynamic lifetime and mutation invalidation tests still absent |
| Semantic parity matrix | Static consolidation complete for inventoried surface; no exact parity rows | Expand only when new symbols/callers are found; do not call runtime compatible |
| Architecture option comparison | Static comparison complete; buildability undecided | Reopen only with dependency and caller evidence; no option is GO |
| File-level provenance/license | Not done | Exact upstream path/SHA, local hash/diff, license header/resource/license obligations |
| Reproducible Stage-0 acceptance bundle | Not done | One reproducible bundle containing pin, inventory, dependency graph, ABI, provenance, matrix, known blockers and reproducible commands |
| Dynamic source-runtime evidence | Not done in Stage-0 | Stage-1 PoC prerequisite: real build + differential fixtures + Android host integration; not replaceable by docs |

**Stage-0 decision remains:** `in_progress / not accepted`; **Stage-1 remains NO-GO**. The matrix task closes the documentation consolidation slice only. It does not close Gradle closure, reflection/provenance, reproducible bundle, or dynamic behavior.

### Task disposition and next registration boundary

- `R3-S0-SEMANTIC-PARITY-CLOSURE-006` is complete as a static consolidation slice: all currently inventoried major capability rows have upstream evidence, reachable host path where established, honest parity state, contract delta and required differential evidence; A/B/C have been compared against the same matrix.
- No source/test/Gradle/schema/UI/workflow/CI file was changed or run.
- Next work must be separately registered. The next highest-value Stage-0 slice is exact pinned-upstream Gradle dependency and packaging closure, including direct/transitive artifacts, Android resources/manifest, generated code, native libraries, R8/ProGuard, reflection and service loading. This must resolve the existing “broad source-closure experiment” failure against exact symbols before any module-boundary decision.


## 24.56 Runtime-3.0 Stage-0 — pinned Gradle/source/resource/packaging closure, static pass (2026-11-24)

- **Task:** R3-S0-GRADLE-PACKAGING-CLOSURE-007; registered in overview §4.7.
- **Inspection snapshot:** branch `integration/legado-upstream-runtime`, start HEAD `b42d38bf20c28a2d191c5809fa70048e190e3bdb`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Method:** Git tree inventory and direct inspection of pinned `settings.gradle`, root `build.gradle`, `gradle/libs.versions.toml`, `app/build.gradle`, `modules/book/build.gradle`, `modules/rhino/build.gradle`, app manifest, Cronet build scripts and Badcat's five module build files. This is a **static declaration/source inventory**, not a resolved Gradle graph.

### Module and build topology

| Item | Pinned upstream | Badcat current branch | Implication |
|---|---|---|---|
| Included Gradle projects | `:app`, `:modules:book`, `:modules:rhino` | `:app`, `:integration-contract`, `:legado-rhino`, `:legado-runtime`, `:legado-runtime-poc` | Current Badcat has five modules, but its runtime module is not the upstream app core extraction |
| Main app plugins | Android application, Kotlin Android, Parcelize, Room, KSP, Google Services | app: Android application, Kotlin Android, Parcelize | Upstream Room/KSP/Google Services build integration is not present as an equivalent runtime module graph |
| Root plugin baseline | AGP 8.13.2, Kotlin 2.4.20, KSP 2.3.12, Room 2.8.4 (version catalog) | AGP 8.13.2, Kotlin 2.4.20; no KSP/Room plugin in current root plugin declaration | Version alignment is partial; plugin presence does not imply generated code closure |
| Java/Kotlin target | Java 21 / Kotlin JVM toolchain 21 in app and libraries | Java 21 / Kotlin JVM 21 in app and Android runtime libraries | Language target aligns at declaration level |
| Runtime/source extraction | Upstream app owns most source and service classes; `:modules:book` and `:modules:rhino` are small Android libraries | `:legado-runtime` injects seven rule model sources + GsonExtensions; `:integration-contract` separately compiles selected parser files and local adapted bridges; `:legado-rhino` reuses Rhino dependency/source set | No single module currently contains authentic BookSource/BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions runtime closure |
| Product UI/Reader boundary | Product activities, app manifest, Room DB and resources live in `:app` | Badcat app manifest and UI remain host-owned | A controlled extraction must retain source-required app services without importing Legado product activities/Reader |

### Exact pinned source inventory (Git tree)

The recursive upstream tree at the fixed SHA returned 3,298 entries (`truncated=false`). Static path counts:
- `app/src/main/java/`: 1,245 entries;
- `modules/book/src/main/java/`: 93 entries;
- `modules/rhino/src/main/java/`: 38 entries;
- `app/src/main/res/`: 673 entries;
- `app/src/main/assets/`: 155 entries.
- App manifests: `app/src/main/AndroidManifest.xml`, `app/src/cronetRuntime/AndroidManifest.xml`, `app/src/pdfRuntime/AndroidManifest.xml`.
- Library manifests for `:modules:book` and `:modules:rhino` are empty manifest shells.
- Packaging/consumer rules: `app/proguard-rules.pro`, `app/cronet-proguard-rules.pro`, `modules/book/consumer-rules.pro`, `modules/rhino/consumer-rules.pro`.
- Tracked JAR/native-adjacent artifacts include seven Cronet Java JARs under `app/cronetlib`, Rhino 1.7.14 under `modules/rhino/lib`, and the pinned HtmlUnit CoreJS JAR under `third_party/maven`. The tree listing did not provide a checked-in `.so` payload in the selected path inventory; `app/download.gradle` defines downloads for five ABI-specific Cronet shared libraries, so the build has external native artifact inputs.
- No `META-INF/services/` service-loader files were found in the recursive tree path inventory. This does not exclude reflection, generated registries, JNI or string-based class loading.

The main app manifest declares the Legado Application and extensive product activities/services/permissions, including its own welcome/main/reader/book/source/RSS/video/audio and other UI surfaces. It is not an acceptable runtime manifest to transplant wholesale into Badcat. The two reusable library manifests are empty, but this does not prove that all source-runtime APIs are free of app resources or generated symbols.

### Build-script dependency and packaging evidence

- **Pinned upstream root:** plugin management adds the R8 9.1.29 buildscript classpath from Google's R8 raw repository; repositories include Google, Maven Central, Gradle Plugin Portal and a local vendored HtmlUnit Maven repository. The build includes `:app`, `:modules:book`, `:modules:rhino`.
- **Pinned upstream app:** applies Android application, Kotlin Android, Parcelize, Room, KSP and Google Services plugins; applies `download.gradle` and `cronet-loader.gradle`; enables BuildConfig and ViewBinding; uses Room KSP schema generation; enables Java 21; configures product flavor `app`; has debug/release ProGuard and resource shrinking behavior.
- **Pinned upstream direct dependency families:** AndroidX UI/lifecycle/preferences/webkit/documentfile, Material/Flexbox, Room runtime/ktx/compiler/testing, Gson, coroutines, Jsoup/JsoupXpath/JsonPath, project book/Rhino modules, OkHttp/Brotli, Cronet JARs and generated native loader JAR, protobuf, Glide/KSP, NanoHTTPD/WebSocket, Ktor/MCP server, libarchive, Hutool crypto, Bouncy Castle, Quick Chinese Transfer, PDFBox, Firebase, media/video/audio, editor, QR and other product dependencies. The app dependency list is substantially broader than the source-runtime capability set.
- **Pinned Rhino library:** exposes HtmlUnit CoreJS as API; includes coroutines, OkHttp and AndroidX collection.
- **Pinned book library:** depends on AndroidX annotation; its own manifest is empty. Its source inventory is 93 paths and must be filtered by actual source-runtime callers, not module name alone.
- **Cronet build-time closure:** `download.gradle` downloads seven Java JARs and five ABI native libraries (armeabi-v7a, arm64-v8a, riscv64, x86, x86_64), creates `cronet.json`, and declares remote artifact inputs. `cronet-loader.gradle` uses ASM 9.8 as buildscript tooling to transform the pinned Cronet native Java JAR at a hash/version guard. These are not generic Legado runtime dependencies unless caller tracing proves they are required by the selected source execution path.
- **Badcat current runtime modules:** `:legado-runtime` declares Gson and AndroidX annotation and adds only seven rule model files plus GsonExtensions to Debug/Release Kotlin compilation. `:integration-contract` separately compiles authentic AnalyzeByJSoup, AnalyzeByRegex, RuleAnalyzer and SourceHtmlParser plus local adapted XPath/JSONPath bridges, with explicit parser dependencies. `:legado-rhino` exposes HtmlUnit CoreJS and includes coroutines, OkHttp and AndroidX collection. `:legado-runtime-poc` declares Room/Gson/Hutool/BouncyCastle/coroutines and Rhino but injects a selected pure/rule model source group; it is a probe, not the production runtime.
- **Badcat custom packaging:** app build has packaging exclusions/pickFirst for META-INF collisions and a custom Android runtime-classpath verifier for RuleAnalyzer. These are host build accommodations, not evidence that the upstream app dependency/packaging closure is reproduced.

### Historical source-closure experiment recovery

The prior records are reconciled without treating their graph counts as interchangeable:

1. Runtime-0.6 / Runtime-0.7 two-root source import graph from `BookSource.kt` + `BaseSource.kt` reported **658 reachable upstream source nodes** and overlapping dependency families including Android (460), Activity/UI (288), database (141), Room (54), WebView (32), Rhino/JS (59), network (84), Parcelable (27) and cache (59). The source graph explicitly documents lexical/same-package/reflection/generated-code blind spots.
2. The exact unchanged two-file Kotlin probe compiled those upstream files in `:legado-runtime-poc` with a deliberately narrow dependency set and failed with **211 compiler diagnostic lines**: 153 unresolved-reference occurrences, 42 distinct unresolved names, plus cascading inference/type/overload errors. The corrected G2c attribution identified 113 real source symbol/property/function occurrences across 35 pinned declarations; remaining diagnostics included package-path segments and cascades. This proves the two-file isolated probe is insufficient, not that the pinned upstream app itself is broken.
3. A later package-aware G2 graph snapshot reports **661 files / 6,303 internal import edges**. This is a separate graph iteration and must not be arithmetically merged with the 658-node earlier report.
4. Current `legado-runtime-poc/source-closure-stage1.json` records the G2d source group: six rule models, BookListRule, GsonExtensions, AppPattern, BookSourceType, NoStackTraceException and MapExtensions, plus a declared dependency set. Its own note says this is an independently compilable first source group, not full BookSource/BaseSource runtime. The current probe build file reintroduces that group, not the full closure.
5. The current repository includes `.github/scripts/legado-runtime-dependency-closure.py`, which labels itself a deterministic **source-level import graph**, not a Kotlin compiler graph. It explicitly does not resolve reflection, generated Kotlin/Java, KSP/KAPT output, service loader, JNI, Gradle variants, manifest/resources or actual compiler binding. Therefore its output cannot be used as the resolved dependency closure required for Stage-0.

### Confirmed facts, static risks and dynamic-unverified boundaries

- **Confirmed static facts:** exact included module sets; declared plugins and versions; selected source roots; source/resource/asset counts; manifest variants; direct dependency families; Cronet external JAR/native download and ASM transformation path; prior two-file compiler diagnostics; and the current script's stated analysis limitations.
- **Static architecture risk:** a faithful BookSource/BaseSource source closure reaches app-owned services and data models (Room/CacheManager/CookieStore/SharedJsScope/login/WebView and reader/content helpers) and is not currently demonstrated as a small pure module. The upstream app build includes extensive product UI/Reader and unrelated features; wholesale app module reuse violates the Badcat-only product boundary.
- **Dynamic/unresolved:** no exact resolved Gradle `runtimeClasspath` or variant graph was generated for the pinned upstream checkout or a candidate isolated runtime; no artifact metadata/variant selection report, dependency insight, generated source report, resource merge report, manifest merger report, APK DEX/native packaging report or R8 mapping was produced in this task. Static version declarations are not resolved artifacts.
- **Tooling boundary:** GitHub source/tree inspection does not execute Gradle or expose a checked-out working tree. No local pinned checkout/Gradle invocation or reproducible dependency-resolution environment was available in this task. No CI was started.
- **Decision:** R3-S0-GRADLE-PACKAGING-CLOSURE-007 is **partial/static inventory complete, resolved graph blocked by execution environment**. Do not mark dependency closure complete. Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**. No source, test, Gradle, workflow, schema, UI or CI file was changed or run.

### Remaining acceptance work

1. Run dependency resolution against an exact pinned checkout and capture `:app:debugRuntimeClasspath`, `:modules:book:debugRuntimeClasspath`, `:modules:rhino:debugRuntimeClasspath` and candidate runtime configurations with artifact coordinates, variants and transitive paths.
2. Capture Room/KSP/Parcelize/BuildConfig/ViewBinding generated outputs and manifest/resource merge inputs; classify which are truly required by source-runtime callers.
3. Resolve Cronet/native/JNI scope and distinguish source-runtime-required HTTP behavior from product-level Cronet integrations; inspect all reflection/service-loader/string-based class references.
4. Re-open the G2c diagnostic set and map each of the 35 declarations to current exact source and dependency edges, preserving the no-fake/no-no-op rule.
5. Generate a candidate controlled upstream-core extraction graph that preserves required BookSource/BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions behavior while excluding Legado product UI/Reader only where caller proof permits.


## 24.57 Runtime-3.0 Stage-0 — reflection/JVM/Rhino ABI and source provenance static inventory (2026-11-24)

- **Task:** `R3-S0-REFLECTION-ABI-PROVENANCE-008`; registered in overview §4.8 before this inspection.
- **Inspection snapshot:** branch `integration/legado-upstream-runtime`, starting source HEAD `e7f242273a00c1b050ce86dcaeb5c265207db1ee`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Method:** exact Git tree/blob SHA comparisons between the Badcat branch and pinned upstream tree/submodule; static inspection of upstream `JsExtensions`, `NativeBaseSource`, Rhino wrapper/class-shutter sources and local Rhino engine; license/notice file inventory. No compilation or runtime execution.

### Source provenance and exact-file comparison

| Local slice | Pinned upstream counterpart | Result |
|---|---|---|
| Seven rule models (`BookListRule`, `ExploreRule`, `SearchRule`, `BookInfoRule`, `TocRule`, `ContentRule`, `ReviewRule`) + `GsonExtensions.kt` | Same paths under upstream `app/src/main/java` | All 8 files have identical Git blob SHA at the pin |
| Additional rule UI/model files `ExploreKind.kt`, `FlexChildStyle.kt`, `RowUi.kt` | Same upstream paths | All 3 blobs identical; their presence does not enlarge the runtime module's declared seven-model compile set |
| `legado-rhino/src/main/java/com/script/**` | `modules/rhino/src/main/java/com/script/**` | 18 Kotlin files compared: 17 identical; `RhinoScriptEngine.kt` differs |
| Local `legado-rhino/src/main/java/org/htmlunit/corejs/javascript/**` | Same relative paths in upstream `:modules:rhino` | All 12 Java files have identical Git blob SHA |
| Local HtmlUnit license/source bundle | Upstream `app/src/main/assets/licenses/htmlunit-core-js/5.3.0-legado.4/` | Seven records (`LICENSE-APACHE-2.0.txt`, `LICENSE-core-js.txt`, `LICENSE.txt`, `NOTICE-tools.txt`, `NOTICE.txt`, `SOURCE.md`, `rhinoDiff.txt`) have identical Git blob SHA |
| `AnalyzeByJSonPath.kt` | Pinned upstream same path | Identical Git blob SHA |
| `AnalyzeByXPath.kt` | Pinned upstream same path | Modified adapter: removes Android `TextUtils` import and replaces `TextUtils.join("\\n", it)` with Kotlin `it.joinToString("\\n")`; source behavior equivalence has not been differentially tested |
| `AnalyzeByJSoup.kt`, `AnalyzeByRegex.kt`, `RuleAnalyzer.kt`, `SourceHtmlParser.kt` | Pinned upstream app source tree | Not copied into the Badcat main tree; `integration-contract/build.gradle.kts` compiles these from `$rootDir/upstream/legado/app/src/main/java`. The `upstream/legado` gitlink and `gradle/legado-upstream.properties` both pin exact SHA `330e606c4d57e08adc056ff8ab7a4132c784da9a`. |
| `LegadoRhinoSandbox.kt` | No upstream counterpart | Badcat-local bounded primitive-string sandbox; explicitly documents it is not the complete Legado AnalyzeUrl adapter |

The local `RhinoScriptEngine.kt` public/source declaration inventory has all inspected upstream declarations present and adds one local-only public method, `callSourceFunction(action: Function, scope: VarScope): Any?` (local lines 292–299). The local file imports Rhino `Function` for this bridge. No upstream declaration with that signature was found in the pinned file. This is a local extension, not upstream API parity evidence; its callers and bytecode exposure still require compiled caller/ABI verification. The source comparison establishes at least this intentional difference; it does not substitute for a complete compiled class diff.

### Reflection and JavaScript bridge evidence

- Pinned `app/src/main/java/io/legado/app/help/JsExtensions.kt` is a 1,390-line class marked with `@Keep` (line 123). It contains **48 `@JavascriptInterface` annotation occurrences** and numerous overloaded Java-callable functions. The count is an annotation-occurrence count, not a count of unique JS API names or overload descriptors.
- Pinned `app/src/main/java/io/legado/app/help/rhino/NativeBaseSource.kt` (lines 8–44) subclasses `CatchableNativeJavaObject` and overrides Rhino `has/get/put`: setter-like names are hidden when a matching property exists, and writes are specially restricted to `variable`. This is observable JS property-resolution behavior, not a passive DTO wrapper.
- Pinned `modules/rhino/.../CatchableNativeJavaObject.kt` (lines 17–43, 148–208) wraps Java members through `NativeJavaMethod`/Rhino `Function` wrappers and maintains a per-name method cache. Java method discovery/overload binding and error unwrapping therefore form part of the script-visible ABI.
- Pinned `RhinoClassShutter.kt` (lines 34, 47–49, 104–139) uses `java.lang.reflect.Member` internally while denying script access to `java.lang.reflect`, `java.lang.invoke`, `dalvik.system` and other protected packages; it also protects `ClassLoader`, `Class`, `Member`, Context and object-stream classes. The protected system method names include `load`, `loadLibrary` and `exit` (lines 123–125). This is a security boundary and must be retained/tested when the authentic Rhino wrapper is integrated.
- In the inspected pinned files (`JsExtensions`, `BaseSource`, `NativeBaseSource`, `RhinoScriptEngine`, `CatchableNativeJavaObject`, `JavaObjectWrapFactory`, `RhinoClassShutter`), no `Class.forName`, `ClassLoader.loadClass`, `ServiceLoader`, JNI `native fun`, or `System.load/loadLibrary` call was found. This is limited to these inspected files, not a repository-wide proof that the complete runtime closure has no dynamic loading.
- The current Badcat tree contains no `JsExtensions.kt` or `NativeBaseSource.kt` counterpart. Existing host JS and typed adapters do not thereby provide the full upstream bridge surface. This reinforces the already-recorded runtime gap; it does not re-open the earlier per-method inventory.
- `legado-rhino/consumer-rules.pro` currently contains comments only. The upstream `@Keep` and `@JavascriptInterface` annotations are on upstream application code not currently copied as a complete bridge class. Whether R8 retains the final integrated bridge and its methods cannot be inferred from the current empty consumer rules or from source annotations alone.

### License and notice provenance

- Pinned upstream root `LICENSE` is GPL-3.0. Both local `third_party/legado/LICENSE` and `third_party/legado-rhino/LICENSE` have the exact same Git blob SHA as that upstream license (`ef08d94b4bf3cd935a545be060e001f872a3090c`).
- Local `THIRD_PARTY_NOTICES.md` records the Legado pin, Rhino source module, GPL-3.0-only notice, HtmlUnit artifact source revisions, MPL-2.0/Apache-2.0 and third-party notice bundle, plus the selected model/Gson and crypto helper source attribution.
- The HtmlUnit license/notice/source records listed above are byte-for-byte Git-blob-identical to the pinned upstream asset copies. The source tree also contains `third_party/htmlunit-core-js-5.3.0-legado.4/SOURCE.md` with the packaging and Rhino fork source revisions and a recorded artifact SHA-256.
- The repository tree has no root-level `LICENSE` file; the GPL text is retained under `third_party/legado/` and `third_party/legado-rhino/`. This inventory does not constitute legal review of GPL combination/distribution obligations, the exact applicability of file-level Oracle/Rhino notices, MPL source-offer obligations, or the full transitive dependency license set. Those remain release blockers until a complete artifact/dependency SBOM and distribution review exist.

### ABI limits, status and remaining proof

- Source signatures and Git blob equality do **not** establish compiled JVM descriptors, Kotlin `$default` bridges, suspend continuation descriptors, synthetic/bridge methods, parameter annotations, generic signatures, R8 keep behavior, Rhino overload selection or Android DEX exposure. No compiled ABI dump, reflection test, R8 mapping or APK DEX inspection was produced in this task.
- No Gradle/build/test/CI was run. This is static evidence only; do not state a dynamic reflection or runtime failure.
- **Task 008 disposition:** static source/provenance inventory complete for the inspected slices; compiled ABI and full distribution-license acceptance remain **NOT MET**. No source/test/Gradle/workflow/schema/UI/CI file changed.
- **Stage-0 remains** `in_progress / not accepted`; **Stage-1 remains NO-GO**. Task 007 resolved Gradle dependency/variant closure remains independently blocked and NOT MET.
- **Next acceptance:** obtain a reproducible exact-SHA Gradle runner; generate pinned and candidate class/JVM ABI inventories, inspect R8/DEX retention and Rhino reflection behavior, finish full dependency/license SBOM and reproducible Stage-0 bundle. Do not repeat the blob comparisons above unless the pin or local source files change.


## 24.58 Runtime-3.0 Stage-0 — core execution closure and typed host-port boundary (2026-11-24)

- **Task:** `R3-S0-CORE-EXECUTION-CLOSURE-009`; registered in overview §4.9.
- **Snapshot:** branch `integration/legado-upstream-runtime`, start HEAD `46ebc565a4ec8e81241e4c96b6417881b813edd5`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Priority:** this is a mainline compatibility-boundary investigation, not a minor-defect sweep. Existing per-method and CI history findings are reused rather than repeated.
- **Method:** static source import/declaration inspection of pinned BaseSource, BookSource, AnalyzeRule, AnalyzeUrl, JsExtensions, SharedJsScope, JsSourceEngine, CookieStore, CacheManager, SourceHelp, SourceNavigationBridge and SourceVerificationHelp; comparison with current Badcat Gradle source sets, RuntimeHostPort, SourceRuntime, service orchestration and host implementations. No compilation or runtime execution.

### 24.58.1 Pinned upstream execution roots are mutually coupled service objects

| Root | Source-backed role and major edges | Compatibility consequence |
|---|---|---|
| `BaseSource.kt` | `BaseSource : JsExtensions` (line 65); login/session methods (179, 276–356); source variable APIs (331–356); explore/jsLib refresh and concurrent policy (380–406); JS evaluation (414). Imports Rhino ScriptBindings/engine, AppConst/AppLog, CacheManager, ConcurrentRateLimiter, JsExtensions, AppConfig, Android crypto, CookieStore, source cache/shared-scope helpers, SharedJsScope, JsSourceEngine, LoginUiV2 and Gson utilities. | It is an executable source/session contract, not a transport DTO. Removing methods or replacing state with a string map changes script-visible state, identity, lifecycle and login semantics. |
| `BookSource.kt` | Room entity + Parcelable; inherits BaseSource; owns typed Search/Explore/BookInfo/TOC/Content/Review rules and Gson/Room converters; source identity and rule-derived helpers. | The authentic BookSource model and BaseSource behavior must travel together. JSON-only projection does not reproduce entity, rule, inheritance or converter behavior. |
| `AnalyzeRule.kt` | `setContent` (104); selector dispatch XPath/Jsoup/JSONPath (140–168); extraction APIs (205–453); replacement/rule state (507+); variable/eval APIs (856–896); typed source and batch context access (944–952); refresh callbacks (985, 1010). Imports BaseBook/Book/BookChapter/BookSource/RssArticle, CacheManager, JsExtensions, BackstageWebView, CookieStore, SharedJsScope, BatchContentContext, WebBook and NetworkUtils. | Selector engines alone are not AnalyzeRule. The runtime needs authentic typed context, rule/variable/replacement state, JS bindings, source/session/cache and browser callbacks. |
| `AnalyzeUrl.kt` | URL initialization/field encoding (175+); JS (395); suspend HTTP response execution (454, 594); byte/input-stream/upload helpers (699–731); source bridge and URL option model. Imports BaseSource, Book/BookChapter, CacheManager, ConcurrentRateLimiter, JsExtensions, AppConfig, BackstageWebView, CookieManager/CookieStore, Cronet, StrResponse, OkHttp request/response helpers, SharedJsScope, NetworkUtils, Glide/media helpers and Android/Hutool APIs. | This is the central HTTP/JS/WebView execution service, not a thin OkHttp wrapper. Must preserve method, headers, body, cookies, redirects, timeout, retry, DNS, WebView, response/error, binary/upload, cancellation and lifecycle contracts. |
| `JsExtensions.kt` | `JsExtensions : JsEncodeUtils` (line 125); overload families include ajax/connect (176–260), WebView (279–352), navigation/browser (378–420), verification (442), cache/cookie/files (470–554), GET/HEAD/POST (575–633), plus encoding/archive/file helpers. Imports AppCompatActivity, Android JavascriptInterface/WebSettings, BackstageWebView, CookieStore, SourceHelp/SourceVerificationHelp, BookHelp, BatchContentContext, VideoPlay, UI Activities/dialogs, ReadBookConfig/ThemeConfig and broad utilities. | This interface is the public script ABI. Product UI must not be transplanted, but every source-visible method needs a behavior-preserving typed host implementation or a proved source-runtime-only implementation. Omitting an overload is not harmless. |
| `SharedJsScope.kt` / `JsSourceEngine.kt` | Shared JS library and crypto scope, compiled scripts, source JS functions, shared global state, cache, Rhino bindings, source-scoped lifecycle. | A per-screen ephemeral evaluator or primitive key/value store is not a substitute for authentic scope/cache/global-state semantics. |
| `CookieStore.kt` / `CacheManager.kt` | CookieStore uses appDb Cookie entity, CacheManager and CookieManager; CacheManager uses appDb Cache entity, memory LRU, ACache and WebView-facing cache. | Cookie and generic cache ownership are service/data contracts; host stores must match identity, TTL, invalidation, cross-instance visibility and persistence or be explicitly adapted and differentially tested. |
| `SourceNavigationBridge.kt` / `SourceVerificationHelp.kt` / `SourceHelp.kt` | BaseSource context wrapper, navigation suppression, login/video/openUrl; verification uses WebView/VerificationCode activities, flight coordination, wait/cancel/complete; SourceHelp owns source lookup/delete/enable and book-source cleanup. | Browser/verification callbacks include context propagation, wait/return, cancellation and state transitions; a one-way UI launch callback is insufficient. Catalog mutations must coordinate all runtime instances and state stores. |

### 24.58.2 Current Badcat module boundaries do not contain this execution closure

| Current module/path | What is actually compiled or represented | What it does not establish |
|---|---|---|
| `:legado-runtime` | Android library with Parcelize; explicitly injects seven upstream rule model files + GsonExtensions. | No authentic BaseSource/BookSource outer entity, AnalyzeRule, AnalyzeUrl, JsExtensions, SharedJsScope, JsSourceEngine, CookieStore or CacheManager closure. |
| `:integration-contract` | JVM module compiles upstream AnalyzeByJSoup, AnalyzeByRegex, RuleAnalyzer, SourceHtmlParser and selected adapted XPath/JSONPath files. | Its own build comment says AnalyzeRule app-coupled lifecycle is separate. Parser compilation is not the upstream execution runtime. |
| `:legado-rhino` | Android library with Rhino/HtmlUnit sources and host engine wrapper. | Does not provide authentic BaseSource/JsExtensions/AnalyzeUrl/SharedJsScope caller and lifecycle closure; its local RhinoScriptEngine has a Badcat-only callback extension as recorded in §24.57. |
| `:legado-runtime-poc` | Historical isolated source/compile probe and source closure manifest. | Explicitly not production runtime; does not authorize a runtime boundary or host integration. |
| Badcat host services | SourceRequestClient, SourceHttpGateway, SourceScriptRuntime, selector services, catalog/search/detail/TOC/content orchestration, cookie/file/KV stores and browser-related ports exist. | These are host implementations and selected adapters, not evidence that the pinned upstream roots are wired or that every source-visible contract is represented. |

### 24.58.3 Existing typed host ports — useful foundation, not yet a complete boundary

- `RuntimeHostPort.kt` declares execution identity/context, typed host result/error/cancellation, HiddenWebViewPort, VisibleBrowserPort, VerificationCodePort, HostUiPort and ProductEventPort (notably lines 118, 163, 222–296).
- `SourceRuntime.kt` declares SourceRequestClient and SourceHttpRequest/Response (lines 10+), LegadoRuleRuntime (58+) and SourceScriptRuntime (78+). The current rule method returns `List<Map<String, String>>`; the script state surface uses string keys/values. These signatures do not model upstream AnalyzeRule's `Any?`/Scriptable element objects, typed RuleDataInterface/Book/BookChapter/BatchContentContext, full ScriptBindings or the source's actual JsExtensions return types.
- The current HTTP request contract is a host request DTO with string-valued headers and optional body bytes in its response. It does not, by its declaration alone, prove parity with upstream Request/Response/StrResponse/Jsoup Connection.Response, multi-header behavior, response ownership/close lifecycle, cookie store, proxy/DNS/Cronet/WebView selection or all URL option semantics.
- Existing browser/verification ports are a valuable typed seam, but the upstream behavior additionally includes callback return payloads, browser result URL/refetch semantics, WebView source/override-URL extraction, UI lifecycle and cancellation/wait coordination. The port contracts must be checked against every upstream caller before being accepted.
- Other mandatory typed service seams not yet demonstrated as one complete, wired contract set: CookieStore/CookieManager; CacheManager/ACache and QueryTTF; source login form/UI and login-info persistence; source variable map/large-variable owner; SharedJsScope/jsLib download/cache/global state; RuleDataInterface and typed Book/BookChapter/BatchContentContext; file/archive/crypto/encoding utilities; source catalog mutation/invalidation; refresh events and navigation policy. Some local implementations exist, but no complete upstream-to-host mapping or parity proof has been established.

### 24.58.4 Capability-level boundary disposition

| Capability group | Required runtime behavior | Current disposition |
|---|---|---|
| Source model + BaseSource | Authentic entity/rules/inheritance, identity, source JS, login, variables, scope and invalidation | **Missing as a production authentic closure** |
| Rule engine | AnalyzeRule/AnalyzeUrl, CSS/XPath/JSONPath/regex, replacements, typed context, redirects, errors, batch semantics | **Partial parser pieces; authentic orchestration/context missing** |
| JS bridge | Rhino + ScriptBindings + all JsExtensions overloads + NativeBaseSource property rules + jsLib/shared scope | **Rhino foundation present; full upstream bridge missing** |
| HTTP and response | Request variants, cookie merge, redirects, retry/timeout/cancel, WebView/Cronet/OkHttp, StrResponse and binary/upload | **Host transport partial; upstream AnalyzeUrl behavior not wired** |
| Cookie/login | Cookie DB/memory/session behavior, LoginUiV2 and login headers/info, source isolation | **Host substitutes exist; complete parity and shared ownership unproven** |
| Browser/verification/navigation | Hidden WebView, visible browser, source navigation, verification wait/result/refetch/cancel | **Typed port foundation; caller-complete mapping and execution proof missing** |
| Cache/files/crypto | Generic cache/ACache, query TTF, source/book cache invalidation, file/archive and crypto behavior | **Several host utilities exist; upstream service semantics not unified** |
| Search/explore/detail/TOC/content/batch | Full BaseSource + AnalyzeRule + AnalyzeUrl + typed book/chapter context lifecycle | **Host service orchestration partial; not complete Legado execution chain** |

### 24.58.5 Architecture finding and gate

1. The existing module layout is not yet a controlled upstream runtime: the authentic runtime root `BaseSource` is absent from production compile source sets, and its central `JsExtensions`/AnalyzeRule/AnalyzeUrl service graph is not assembled.
2. Simply adding more parser files, seven rule models or the current Rhino module cannot close the execution contract. A no-op or same-name simplified replacement for the missing services would violate the master workflow's hard compatibility gate.
3. Directly depending on the upstream Android `:app` is also not an acceptable shortcut: it owns Legado product UI/Reader, broad app services and Room database. Any reuse must extract the authentic source-runtime behavior while explicitly porting Android/UI/database boundaries and proving all caller edges.
4. The evidence supports continuing the formal Runtime-3.0 direction: **controlled authentic upstream core + typed host ports + Badcat-only product UI**. It does not yet prove that the closure is buildable or small enough; exact Gradle variants/generated outputs and compiler ABI remain unavailable.
5. Do not implement production compatibility by expanding the current selector/Map runtime. The next implementation can begin only after Stage-0 establishes the minimum authentic closure and Stage-1 PoC is separately registered/authorized under the workflow.
6. **Task 009 disposition:** major source-level boundary map complete for the inspected roots; transitive compiler/Gradle closure and all-port differential proof remain NOT MET. No source/test/Gradle/workflow/schema/UI/CI file changed or run.
7. **Stage-0 remains** `in_progress / not accepted`; **Stage-1 remains NO-GO**. This is a major compatibility blocker, not a minor issue.


## 24.59 Runtime-3.0 Stage-0 — behavior-bearing source closure decomposition (2026-11-24)

- **Task:** `R3-S0-SOURCE-CLOSURE-DECOMPOSITION-010`; registered in overview §4.10.
- **Snapshot:** start HEAD `0935239977a736633e99236c7cb672d204cef30c`; registration commit `acdd85d035992231f335e5cfafb850f982460c34`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Method:** reconciled the historical G1 17-root manifest against the immutable upstream recursive tree; inspected root declarations/imports and actual caller anchors; re-read current Badcat module source sets and host contracts. All 17 root Git blob SHAs match the pinned upstream tree exactly. The old manifest is a source navigation artifact, not a current compiler graph.

### 24.59.1 Exact roots and boundary classification

| Pinned upstream root | Evidence / role | Classification |
|---|---|---|
| `data/entities/BaseSource.kt` | `BaseSource : JsExtensions` (65); login/session, headers, variables, source scope and JS bindings (414–425). | **Mandatory authentic core** — preserve identity, login, variables, JS bindings, shared scope and cleanup. |
| `data/entities/BookSource.kt` | Room/Parcelable entity (24–31), URL primary key (33–41), inherits BaseSource (104), six typed rule models. | **Mandatory authentic model/core** — host may own storage, but JSON-only DTO is not equivalent. |
| `data/entities/Book.kt` | Typed BaseBook/RuleData context; Room and reader/content helper coupling. | **Mandatory model semantics** — preserve actual rule-context behavior; port data access, do not import Reader UI. |
| `data/entities/BookChapter.kt` | Typed chapter/context identity, RuleData and AnalyzeUrl dependency; Room relation semantics in §24.52. | **Mandatory model semantics** — reduced chapter DTO is not sufficient for upstream execution. |
| `help/JsExtensions.kt` | `@Keep` public interface (123–125), source/batch context (127–145), HTTP/WebView/browser/verification/cache/file/refresh API. | **Mandatory authentic script ABI** — retain complete methods/overloads; platform actions use typed host ports. |
| `model/jsSource/JsSourceEngine.kt` | Implements JsExtensions (40); binds java/source/sourceApi/baseUrl/cookie/cache and shared scope (104–124); supports contentBatch. | **Mandatory authentic core** — current String-only evaluator is not equivalent. |
| `model/SharedJsScope.kt` | Shared jsLib/crypto scopes, compiled script cache, shared global state and invalidation. | **Mandatory authentic core/service** — not a generic key/value store. |
| `model/analyzeRule/AnalyzeRule.kt` | Typed RuleDataInterface/BaseBook/Book/BookChapter context, selector dispatch, JS, variables, batch and refresh behavior (63–90). | **Mandatory authentic rule engine** — parser sources alone are insufficient. |
| `model/analyzeRule/AnalyzeUrl.kt` | Source/RuleData/chapter request context (82–99), URL options, HTTP, Cookie, redirect/retry, WebView and cancellation. | **Mandatory authentic request engine** — connect to host services without changing URL semantics. |
| `help/http/CookieStore.kt` | Cookie DAO + CacheManager/session/WebView integration (21, 37–38). | **Mandatory Cookie behavior; host persistence port.** |
| `help/CacheManager.kt` | Cache entity/DAO, memory LRU, ACache, file and QueryTTF APIs (53 onward). | **Mandatory cache behavior; host persistence port.** |
| `ui/login/SourceLoginJsExtensions.kt` | Extends RssJsExtensions (26); callback/login UI data and custom button/browser behavior (29–80). | **Required source callback/login bridge; not a reason to import RSS UI.** |
| `help/source/SourceVerificationHelp.kt` | Verification flight registry, await/complete/fail/abandon, browser/verification UI and refetch. | **Required state machine + host UI port** — preserve single-flight, wait, result, cancellation and refetch. |
| `help/source/BaseSourceExtensions.kt` | Source-type dispatch, shared scope/global state identity and invalidation (13–37). | **Mandatory lifecycle helper.** |
| `help/http/HttpHelper.kt` | OkHttp client, CookieJar, proxy/DNS/network/cache integration. | **Mandatory HTTP behavior; host network implementation.** |
| `help/http/BackstageWebView.kt` | Hidden WebView lifecycle/cancel/release (54, 74–105); source is reloaded via `appDb.bookSourceDao` and JS interfaces attached (120–128). | **Required WebView semantics; host implementation** — replace DAO lookup with typed source-catalog port. |
| `help/webView/WebJsExtensions.kt` | WebView JS bridge, rule/network APIs; extends RssJsExtensions. | **Required WebView script surface; host implementation**, not RSS Reader UI. |

### 24.59.2 Production caller chain

- `WebBook.kt` dispatches search/explore/detail/TOC/content/batch (search at 39, `searchBookAwait` at 53–62); JS sources dispatch to `JsSourceBook`.
- `JsSourceBook.kt` constructs `JsSourceEngine` for search, explore, detail, TOC, content and batch (calls around 28–163).
- `BookList.kt`, `BookInfo.kt`, `BookChapterList.kt`, `BookContent.kt` construct `AnalyzeRule`/`AnalyzeUrl` for selectors, request, pagination, replacement and contentBatch. Their caller detail is already in §24.48; not re-audited here.
- `SourceCallBack.kt` constructs `SourceLoginJsExtensions` for event callbacks (line 91); code search also finds SourceLoginDialog, ExploreAdapter, BookInfoViewModel, ReadBookActivity and VideoPlayerViewModel callers.
- `RssJsExtensions.kt` is a real transitive parent of SourceLoginJsExtensions and WebJsExtensions. It exposes script-visible `put/get`, `searchBook`, `addBook`, `showPhoto`, `open` and rule parsing helpers (45, 62–100, 286–319). This does not prove the full RSS product is needed: map inherited source-facing methods to Badcat host actions, and exclude RSS product screens/data only after caller/API classification.
- `BackstageWebView` is called from AnalyzeUrl, AnalyzeRule and JsExtensions for rule/bodyJs/WebView execution. Its separate CodeEditViewModel caller is editor-only and must not pull code-editor UI into Runtime.
- Therefore the actual chain is `WebBook` → `JsSourceBook` or `AnalyzeRule/AnalyzeUrl` → `BaseSource/BookSource` + `JsExtensions` + Rhino/shared scopes → HTTP/Cookie/Cache/WebView/verification → typed Book/BookChapter/BatchContentContext. Parser-only or bounded URL-evaluator implementations do not close it.

### 24.59.3 Product boundary and typed host ports

**Retain authentic behavior/source:** BookSource/BaseSource and rule models; AnalyzeRule/AnalyzeUrl and authentic parser sources; JsExtensions/JsEncodeUtils; JsSourceEngine/SharedJsScope/BaseSourceExtensions; typed RuleDataInterface/BaseBook/Book/BookChapter/BatchContentContext; Rhino and required pure/runtime utilities. Preserve FQCN, overloads, null/defaults, object identity, coroutine/cancellation, variable precedence, shared scope and lifecycle.

**Replace Android/product implementations with typed Badcat host ports:**
1. **Catalog/data:** exact source-key lookup/mutation/import/export/invalidation; typed Book/BookChapter access, transactions and single-owner persistence. BackstageWebView currently reaches `appDb.bookSourceDao` directly.
2. **HTTP:** method/headers/multi-headers/body, cookie jar, proxy/DNS, redirect/retry, read/call timeout, WebView fallback, binary/upload, response metadata/error/close and cancellation.
3. **Cookie/cache:** persistent cookie/session state, WebView CookieManager synchronization, CacheManager/ACache/QueryTTF, TTL/eviction/files and cross-runtime invalidation.
4. **Hidden WebView:** real page execution, injected source/java/cache bridges, navigation context, cookie, source/override URL capture, pooling, timeout/cancel and guaranteed release.
5. **Login/browser/verification:** LoginUiV2 protocol, login header/info persistence, visible browser, verification single-flight/wait/complete/refetch/cancel and custom actions.
6. **Product actions/events:** search/add-book/photo/open URL/video/refresh callbacks map into Badcat UI/events; no Legado Activity/Dialog/Reader is imported into product layer.
7. **Files/crypto/config:** source-relative file/archive/encoding/crypto, user-agent/config, resource/permission and bounded security behavior.

Do not over-exclude based on package names:
- BookSource references the AudioPlay model type; this alone does not prove the full audio player is required.
- Book/BookChapter depend on reader/content helpers and Room; retain model behavior used by rule execution, not the full Reader UI.
- JsExtensions imports BookHelp, ReadBookConfig, ThemeConfig, VideoPlay and Activity/dialog helpers; every source-visible action needs a Badcat host mapping or proved non-source-only classification.
- SourceLoginJsExtensions/WebJsExtensions inherit RssJsExtensions; do not import RSS Reader wholesale, but do not silently delete inherited JS methods.

### 24.59.4 Minimum controlled-runtime boundary proposal

Proposal only; no module created and no implementation authorized:

- **Controlled Legado Runtime Core:** unchanged pinned BookSource/BaseSource/rule models; required BaseBook/Book/BookChapter/RuleDataInterface/BatchContentContext semantics; AnalyzeRule/AnalyzeUrl and parser sources; JsExtensions/JsEncodeUtils, JsSourceEngine, SharedJsScope, BaseSourceExtensions; pinned Rhino and authentic runtime utilities.
- **Typed Android Host Adapter:** Badcat source catalog/data, HTTP/response, CookieStore, CacheManager/ACache, WebView, browser/verification/login, product actions/events, files/crypto/config, coroutine/cancellation and resource lifecycle.
- **Compatibility Facade:** central identity, entity projection, serializer/error/response and lifecycle conversion. It must not collapse upstream Scriptable/Book/BookChapter/RuleData into Map<String,String> or String-only bindings.
- **Badcat Product UI:** keeps Badcat search/favorites/bookshelf/reader/navigation/purification/bookmarks/CSS; runtime callbacks use typed actions, never Legado UI/Reader.

This does not yet define the final file list or prove buildability. Historical 661-file/6,303-import and 1,085-file/9,983-import graphs are lexical evidence with known blind spots; the unchanged two-file BookSource/BaseSource compiler probe still fails as recorded in G2 diagnostics. Exact Gradle variants, generated sources/resources, compiler bindings, reflection ABI and complete callers remain outstanding.

### 24.59.5 Current gap and disposition

- Current `:legado-runtime` injects only seven rule models + GsonExtensions (build lines 39–53); `:integration-contract` compiles selected parsers and explicitly leaves AnalyzeRule lifecycle separate (11–36); `:legado-rhino` packages Rhino/HtmlUnit but not the upstream execution roots (28–34).
- Current `SourceRuntime.kt` still uses `LegadoRuleRuntime.evaluate(...): EngineResult<List<Map<String,String>>>` and `SourceScriptRuntime.evaluate(... bindings: Map<String,String>): EngineResult<String>` (58–90). These are structurally not the upstream typed RuleData/Scriptable/JsExtensions contracts.
- `RuntimeHostPort.kt` is a useful foundation, but no complete one-to-one wired mapping yet exists for catalog/data, HTTP, Cookie, cache, WebView, login, verification, navigation, product actions, file/crypto and lifecycle.
- **Task 010 disposition:** root provenance 17/17 exact; root/caller/product-boundary decomposition complete for the inspected G1 set; minimum controlled-runtime boundary proposed. Full recursive compiler/Gradle closure, complete API/caller coverage, exact platform-port acceptance and differential execution remain **NOT MET**. No production/test/Gradle/workflow/schema/UI/CI file changed or run.
- **Stage-0 remains** `in_progress / not accepted`; **Stage-1 remains NO-GO**. This is a major compatibility blocker, not a small defect.
- **Next:** consolidate Stage-0 exit evidence and determine whether authentic core + typed host ports can compile without Legado product UI/Reader or a second DB authority. Exact-SHA Gradle execution and compiled ABI remain required; do not start production migration before Stage-1 is explicitly opened.


## 24.60 Runtime-3.0 Stage-0 — exact-SHA Gradle runner availability check (2026-11-24)

- **Related task:** `R3-S0-GRADLE-PACKAGING-CLOSURE-007`; this supplements §24.56 and overview §4.7. It is not a new implementation task.
- **Snapshot:** branch `integration/legado-upstream-runtime`; source/documentation start HEAD `79b669b13cf67c7993dbaa5dbbdbb5cc3ffd195d`; upstream pin remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Purpose:** verify whether the current execution container can supply the exact-SHA Gradle evidence required by Stage-0, without starting CI or changing the repository.

### Environment facts observed

| Capability | Observed result |
|---|---|
| Java | OpenJDK 21.0.12.1 available |
| Git | `/usr/bin/git` available |
| Gradle executable | `gradle` not installed |
| Gradle wrapper cache | `$HOME/.gradle` / wrapper distribution cache absent |
| Android SDK | `ANDROID_HOME` and `ANDROID_SDK_ROOT` unset; no `sdkmanager`, `adb` or Android SDK platform `android.jar` found under inspected `/opt` and `/usr/local` paths |
| GitHub checkout access | `git ls-remote https://github.com/xkszds/badcat-legado-client.git refs/heads/integration/legado-upstream-runtime` failed: `Could not resolve host: github.com` |
| CI | Not started; no workflow dispatch or CI run |

### Consequence

- The container has a suitable Java major version but cannot check out the Badcat/upstream exact commits, download the Gradle wrapper distribution/dependencies, or resolve Android SDK components.
- A Gradle wrapper invocation would not cure the blocker: wrapper distribution and Maven/Google repository artifacts are not cached, and external GitHub DNS resolution failed. No successful or failed Gradle task was produced.
- This confirms the execution-environment blocker in §24.56; it is **not** evidence of an upstream source compile failure and must not be recorded as one.
- **Task 007 remains:** static inventory complete; resolved Gradle configurations/variants/artifact graph/generated outputs/manifest-resource merge/DEX packaging remain NOT MET.
- **No source, test, Gradle, workflow, database/schema, UI or CI files changed.** No build/test/CI was run.
- **Stage-0 remains** `in_progress / not accepted`; **Stage-1 remains NO-GO**. Exact-SHA dependency and compiled ABI evidence requires a network-enabled reproducible Gradle+Android SDK runner.

## 24.61 Runtime-3.0 Stage-0 — exit-evidence reconciliation and gate matrix (2026-11-25)

- **Task:** `R3-S0-EXIT-EVIDENCE-RECONCILIATION-011`; registered in overview §4.11 at start HEAD `6a1afb466db254979f69b0b8a6f1075ec06edc0f`; registration commit `a53f1fb8804b5d9621fa991bf788d617ad81b7fd`.
- **Reconciliation snapshot:** branch `integration/legado-upstream-runtime`; source/documentation start for this result `a53f1fb8804b5d9621fa991bf788d617ad81b7fd`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** reconcile the existing Stage-0 exit matrix (§24.55) with the later Gradle, ABI/provenance, caller-boundary, root-decomposition and runner evidence (§§24.56–24.60). This is a documentation/status reconciliation, not a new source audit.

### 24.61.1 Current Stage-0 exit evidence

| Exit evidence | Reconciled status | Evidence and remaining proof |
|---|---|---|
| Pinned source declarations, models and callers | **PARTIAL** | Tasks 006/008/010 and §§24.50–24.59 establish a 20-capability static matrix and 17/17 exact G1 root blob matches. Full machine-readable declaration/field/overload inventory, inherited/generated/reflection-visible surface and all caller edges remain incomplete. |
| Complete upstream transitive caller graph | **PARTIAL** | §§24.58–24.59 trace the principal BookSource/BaseSource → AnalyzeRule/AnalyzeUrl/JsExtensions/Rhino → HTTP/Cookie/Cache/WebView/verification → typed book/chapter path. Complete transitive compiler/caller closure and all source-visible API callers are not proven. |
| Resolved Gradle dependency/variant/packaging closure | **STATIC INVENTORY COMPLETE; RESOLUTION BLOCKED** | §24.56 records modules, plugins, source/resource/assets/manifests, declared dependency families, Cronet/native inputs and packaging scripts. §24.60 confirms no Gradle/wrapper cache, Android SDK or outbound GitHub DNS in the runner. Exact runtime classpaths, generated outputs, merged resources/manifests, native/DEX/R8 outputs remain NOT MET. Environment blocker is not a source compile failure. |
| Reflection/JVM/Rhino compiled ABI | **STATIC PARTIAL; COMPILED ABI NOT MET** | §24.57 records 48 JavascriptInterface annotation occurrences, NativeBaseSource/Rhino reflection behavior, selected source provenance and security surface. No compiled descriptors, Kotlin default/suspend/bridge methods, R8 mapping, DEX exposure or runtime overload-resolution proof exists. |
| Badcat production caller/store graph | **STATIC INSPECTED-SLICE COMPLETE; DYNAMIC LIFECYCLE NOT MET** | §24.54 and §§24.58–24.59 trace product entrypoints, runtime construction and inspected store/callback paths. Cross-instance mutation invalidation, persistence transaction and restart behavior have no dynamic proof. |
| Semantic parity matrix | **STATIC CONSOLIDATION COMPLETE FOR INVENTORIED SURFACE; EXACT PARITY NONE** | Task 006 / §24.55 has 20 capability rows and explicit status vocabulary. No row is proven exact parity; newly discovered symbols/callers must be added without re-auditing known slices. |
| Architecture option comparison | **STATIC COMPARISON COMPLETE; BUILDABILITY UNDECIDED** | §24.55 compares dedicated Android Runtime, controlled upstream core and hybrid against the same matrix; §§24.58–24.59 favor controlled authentic core + typed host ports + Badcat-only UI as the formal direction, but no candidate has a buildability GO. |
| File provenance, license and distribution obligations | **STATIC PARTIAL; FULL SBOM/LEGAL ACCEPTANCE NOT MET** | §24.57 confirms selected exact blob provenance (including 17/18 Rhino Kotlin files, 12/12 HtmlUnit Java files and license/notice records) and GPL-3.0 source pin. Complete closure-wide file provenance, resolved transitive license SBOM and distribution/legal review remain absent. |
| Reproducible Stage-0 acceptance bundle | **NOT DONE** | No single reproducible bundle yet combines exact pin, complete inventories, resolved dependency graph, compiled ABI, provenance/license SBOM, parity matrix, blockers and reproducible commands. |
| Dynamic source-runtime behavior | **NOT RUN; STAGE-1 PREREQUISITE** | No dynamic runtime compatibility claim is made. Real exact-SHA build, differential fixtures and Android host integration belong to the separately gated Stage-1 PoC after Stage-0 GO; they cannot be inferred from static evidence. |

### 24.61.2 Gate interpretation

1. Completed tasks 006–010 close only their explicitly scoped static slices; they do not collectively imply Stage-0 acceptance.
2. The immediate Stage-0 blockers are complete source/API/caller closure, resolved exact-SHA Gradle/variant/packaging evidence, compiled JVM/Rhino ABI and R8/DEX evidence, complete provenance plus resolved license SBOM/review, and a reproducible acceptance bundle. The architecture must remain demonstrably extractable without importing Legado product UI/Reader or creating a second authoritative data owner.
3. The current runner failure is classified as **environment-blocked**. It is neither a successful Gradle resolution nor a failed upstream compilation. Do not start CI to work around this; obtain an explicitly available network-enabled exact-SHA Gradle + Android SDK runner when permitted.
4. Stage-1 dynamic differential/Android execution is downstream of Stage-0 GO and must be registered separately. No circular claim is made that Stage-1 tests have already passed.
5. **Decision:** Stage-0 = `in_progress / not accepted`; Stage-1 = **NO-GO**; complete Legado compatibility = **NOT ESTABLISHED**.

### 24.61.3 Next mainline slice

Register and perform a non-duplicative full public `JsExtensions` API closure: reconcile the existing 104-name inventory with exact pinned declarations, overloads, parameter/return/nullability/default/suspend semantics, inherited `RssJsExtensions` and `WebJsExtensions` surface, actual source-runtime callers, and Badcat typed host-port mapping. Reuse existing per-method findings (§§24.37–24.46) rather than re-recording them; focus on missing API/caller coverage and unresolved inherited surface. Documentation-only until Stage-0 is accepted.

- **Task 011 disposition:** evidence matrix reconciled and recorded. No production/test/Gradle/workflow/database/schema/UI/CI files changed or run. No CI started. No Stage-0 or Stage-1 gate was advanced.

## 24.62 Runtime-3.0 Stage-0 — JsExtensions declaration, inheritance and host-port coverage (2026-11-25)

- **Task:** `R3-S0-JS-EXTENSIONS-API-CALLER-CLOSURE-012`; registered in overview §4.12 at start HEAD `2f5bc46f4a69314b74b174e084d59f18a362f4f4`; registration commit `c65928903cc127adab2cb2f44a08b981d1b8751b`.
- **Inspection snapshot:** pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`; branch `integration/legado-upstream-runtime`; source/API inventory verified from the exact pinned raw files.
- **Method:** parsed Kotlin public declarations and overload locations from JsExtensions, JsEncodeUtils, RssJsExtensions, WebJsExtensions, SourceLoginJsExtensions and RegexJsExtensions; inspected the actual pinned implementation/caller anchors and the current Badcat RuntimeHostPort/SourceRuntime declarations. This is static source evidence, not compiled ABI or behavior parity.

### 24.62.1 Exact declaration and inheritance inventory

| Surface | Pinned source / line anchors | Inventory result |
|---|---|---|
| `JsExtensions` | `app/src/main/java/io/legado/app/help/JsExtensions.kt:125–1385` | Extends `JsEncodeUtils`; 77 public unique method names and 118 public declarations/overloads in the interface. The 81-name lexical count also includes one top-level helper and three private helpers, which are not public API names. |
| `JsEncodeUtils` | `app/src/main/java/io/legado/app/help/JsEncodeUtils.kt:19–506` | 27 public unique names / 30 declarations; includes four `createSymmetricCrypto` overloads. Together with the 77 JsExtensions names, this reconciles the historical **104-name** base inventory. |
| `RssJsExtensions` | `app/src/main/java/io/legado/app/ui/rss/read/RssJsExtensions.kt:45–319` | Implements `JsExtensions`; adds 13 unique inherited names beyond the base inventory: `put`, `get`, `searchBook`, `addBook`, `showPhoto`, `open`, `setContent`, `setBaseUrl`, `setRedirectUrl`, `getStringList`, `getString`, `getElement`, `getElements`. `searchBook` and `getString` each have overloads. |
| `WebJsExtensions` | `app/src/main/java/io/legado/app/help/webView/WebJsExtensions.kt:21–219` | Extends RssJsExtensions; adds two unique names, `upConfig` and `request`, and overrides navigation context, logging, HTTP and rule helper methods. Its `ajax/connect/get/post/head` signatures/return values are not interchangeable with the base interface overloads. |
| `SourceLoginJsExtensions` | `app/src/main/java/io/legado/app/ui/login/SourceLoginJsExtensions.kt:21–80` | Extends RssJsExtensions; adds five unique extension methods: `upLoginData`, `reLoginView`, `refreshExplore`, `copyText`, `clearTtsCache`; overrides `showBrowser`. Its nested Callback separately declares `upUiData` and `reUiView` (not methods on the extension object). |
| `RegexJsExtensions` | `app/src/main/java/io/legado/app/help/RegexJsExtensions.kt:16–49` | Separate conditional rule extension implementation with `log`, `logType`, `t2s`, `s2t`, `get`, `put`; names overlap existing surfaces but semantics/owner differ. Do not merge its variable owner with BaseSource/Rss KV semantics. |

Across the base and inherited class surfaces, the static inventory therefore contains **124 unique public names** (104 base + 13 Rss + 2 Web + 5 login-specific), before counting overloads. This is a source declaration/name count, not a claim that every method is exposed identically through every Rhino/WebView bridge. The upstream JsExtensions file has 48 `@JavascriptInterface` annotation occurrences (§24.57); Rhino's `NativeBaseSource`/`CatchableNativeJavaObject` reflection wrapper makes annotation count alone insufficient to define the Rhino-visible API.

### 24.62.2 Base API groups and overload hotspots

| API group | Public names (source line range) | Notable overload/contract surface |
|---|---|---|
| Source/context/batch/events | `getSource`, `getTag`, `getSourceNavigationContext`, `getBatchContext`, `cacheContent`, `refreshBookInfo`, `refreshBookToc`, `refreshContent` (127–165) | Typed BaseSource identity, CoroutineContext, BatchContentContext, chapter object and event semantics; `cacheContent(chapter: Any?, content: String)` is not a string-map operation. |
| HTTP helpers | `ajax`, `ajaxAll`, `ajaxTestAll`, `connect` (176–260); `get`, `head`, `post` (575–633) | `ajax(Any)`/`ajax(Any, Long?)`; batch Array<String> + skipRateLimit; test timeout + skipRateLimit; connect 1/2/3-argument forms; GET/HEAD/POST each have timeout overloads and return Jsoup `Connection.Response`; header input is `Any?`. |
| Hidden WebView | `webView`, `webViewGetSource`, `webViewGetOverrideUrl` (279–372) | Each family has cache/delay variants; URL-capture methods return extracted resource/override URL and are not ordinary HTTP fetches. |
| Browser/navigation/verification | `openVideoPlayer`, `startBrowser`, `startBrowserAwait`, `getVerificationCode`, `openUrl`, `showBrowser` (378–452, 1239–1298) | Browser-await has three overloads including `refetchAfterSuccess` and optional HTML; `showBrowser` has four forms and optional preload/config; preserve result URL/body/refetch, context, wait/cancel and navigation suppression. |
| Source file/cache/cookie | `importScript`, `cacheFile`, `getCookie`, `downloadFile` (455–554) | Cache TTL overload, cookie namespace/key overload, download from URL vs supplied content+URL; source-relative file and CookieStore behavior. |
| Encoding/URL/text | `strToBytes`, `bytesToStr`, `base64Decode`, `base64DecodeToByteArray`, `base64Encode`, `hexDecodeToByteArray`, `hexDecodeToString`, `hexEncodeToString`, `timeFormatUTC`, `timeFormat`, `encodeURI`, `htmlFormat`, `t2s`, `s2t`, `toNumChapter`, `toURL`, `getWebViewUA` (655–791, 1167–1185) | Charset/flags/redirect URL/base URL overloads; nullable byte/string returns and exact default encodings matter. |
| File/archive | `getFile`, `readFile`, `readTxtFile`, `deleteFile`, `unzipFile`, `un7zFile`, `unrarFile`, `unArchiveFile`, `getTxtInFolder`, `getZipStringContent`, `getRarStringContent`, `get7zStringContent`, `getZipByteArrayContent`, `getRarByteArrayContent`, `get7zByteArrayContent` (802–1051) | Local source-relative file access, ZIP/RAR/7z archive decoding, charset overloads and byte-array return semantics. |
| Font/TTF | `queryBase64TTF`, `queryTTF`, `replaceFont` (1052–1165) | Deprecated compatibility entrypoint; Any?/String/URL/file/bytes input dispatch, cache toggle and typed QueryTTF objects. |
| Logging/device/concurrency/config | `toast`, `longToast`, `log`, `logType`, `randomUUID`, `androidId`, `singleFlight`, `lock`, `tick`, `getReadBookConfig`, `getReadBookConfigMap`, `getThemeMode`, `getThemeConfig`, `getThemeConfigMap` (1189–1385) | Source-scoped UI/log actions, device identity, lock timeout/default overloads, concurrency key lifecycle, and JSON + Map configuration projections. |

`JsEncodeUtils` adds MD5, symmetric/asymmetric crypto creation, signing, AES/DES/3DES encode/decode, digest and HMAC methods (27 names; declarations at lines 22–506). The returned `SymmetricCrypto`/`AsymmetricCrypto`/`Sign` objects and their callable methods are part of the JS object surface; flattening these to String results is not equivalent.

### 24.62.3 Caller and implementation anchors

- `BaseSource` implements `JsExtensions` (pinned `data/entities/BaseSource.kt`, class declaration around line 65); this is the source identity/session implementation, not just a DTO.
- `JsSourceEngine` implements `JsExtensions` (`model/jsSource/JsSourceEngine.kt:40`) and builds ScriptBindings at 104–118, including source/sourceApi and SharedJsScope. It is used by the JS-source search/explore/detail/TOC/content/batch chain recorded in §24.59.
- `AnalyzeRule` implements `JsExtensions` (`model/analyzeRule/AnalyzeRule.kt:68`); it invokes BackstageWebView (187), binds CookieStore/CacheManager (899–900), evaluates JS (896+) and exposes BatchContentContext (948).
- `AnalyzeUrl` implements `JsExtensions` (`model/analyzeRule/AnalyzeUrl.kt:99`); its request/response path spans 454–587, WebView fallback 497–516, CookieStore/CacheManager interaction 757–786 and URL option handling later in the file.
- `BackstageWebView` constructs `WebJsExtensions` and attaches it as the Java JS interface (`help/http/BackstageWebView.kt:120–127`); it also directly reloads BookSource through `appDb.bookSourceDao` at line 122, reinforcing the catalog host-port gap already recorded in §24.59.
- `SourceCallBack` constructs `SourceLoginJsExtensions` at `model/SourceCallBack.kt:91`; its callback methods bridge source login UI state and browser behavior.
- `RssJsExtensions` is not an unrelated dead parent: WebJsExtensions and SourceLoginJsExtensions inherit it. Its source-visible parsing/state/product-action methods must be mapped, while its Activity/Dialog/Explore/Search/RSS UI implementations must be replaced by Badcat host actions rather than importing Legado product screens.

### 24.62.4 Current Badcat host-port mapping

| API capability | Existing Badcat seam | Current disposition |
|---|---|---|
| Source identity/context/cancellation | `SourceIdentity`, `SourceExecutionContext`, `HostPortResult` in `RuntimeHostPort.kt` | Useful typed foundation; not yet bound to authentic BaseSource identity, ScriptBindings, object lifetime and shared-scope invalidation. |
| HTTP/response | `SourceRequestClient`, `SourceHttpRequest`, `SourceHttpResponse` in `SourceRuntime.kt` | Partial transport seam only; response is not Jsoup `Connection.Response`/StrResponse and does not by declaration prove all overload, header, cookie, response ownership, WebView fallback or sync/blocking semantics. |
| Hidden WebView | `HiddenWebViewPort` + `HiddenWebViewRequest/Result` | Typed operations exist, but full WebJsExtensions bridge injection, page/cookie/navigation context, cache/capture semantics and release/cancel parity are not proven. |
| Browser/verification/navigation | `VisibleBrowserPort`, `VerificationCodePort`, `HostUiPort`, typed HostUiAction | Foundation exists; startBrowserAwait/refetch/StrResponse, source verification single-flight, login callback protocol, custom buttons and all inherited UI methods are not mapped one-to-one. |
| Refresh events | `ProductEventPort`, `ProductRefreshEvent` | Event names exist; upstream EventBus timing/target/current-content behavior and production wiring are not proven. |
| Rule/parser/context | `LegadoRuleRuntime.evaluate(...): List<Map<String,String>>`; `SourceScriptRuntime.evaluate(... bindings: Map<String,String>): String` | Structurally incompatible with AnalyzeRule/AnalyzeUrl, typed RuleDataInterface/Book/BookChapter/BatchContentContext, Scriptable/Any? values and authentic JsExtensions object returns. |
| Cookie/cache/files/archive/crypto/TTF/config/concurrency | No complete typed contract set in the inspected production runtime boundary | Missing one-to-one ports for CookieStore/CookieManager, CacheManager/ACache/QueryTTF, source file/archive, crypto object factories, font replacement, read/theme config, source locks and source KV. |
| RSS/login product actions | `ProductEventPort` and `HostUiPort` are generic action seams | No complete mapping for searchBook/addBook/showPhoto/open, login form/UI-data callbacks, copyText, clearTtsCache or WebJsExtensions request/upConfig RPC. Must map to Badcat UI without importing Legado Activity/Dialog/Reader/RSS screens. |

### 24.62.5 Findings and remaining work

1. The historical 104-name inventory is a **base-interface + encoding-interface** count only. It omits inherited Rss/Web/login-specific surfaces; the static public name inventory across those classes is 124 names. The previous 104-name count is not wrong, but it is not a complete effective source-facing extension inventory.
2. The method declaration surface is much larger than the unique-name count because of overloads and inherited overrides. Exact source signatures/defaults/nullability are source-inspectable; compiled JVM descriptors, Kotlin `$default`/bridge/suspend methods, R8 retention and Rhino overload resolution remain dynamic/compiler-unverified under the runner blocker.
3. Existing per-method findings in §§24.37–24.46 remain authoritative for ajax, response, navigation, refresh, cookie/session, cache/jsLib and lifecycle deltas. This task does not duplicate or reclassify those findings.
4. The current typed host ports cover only selected WebView/browser/verification/UI/event seams and a partial HTTP request DTO. They do not yet form a complete one-to-one adapter for the 124-name class surface, especially inherited Rss actions, source login callbacks, file/archive/crypto/font/config/concurrency and typed JS objects.
5. **Task 012 disposition:** static declaration/inheritance inventory and principal implementation/caller anchors complete for the inspected classes; complete per-method caller closure, full typed host-port mapping, compiled ABI and differential behavior remain **NOT MET**. No source/test/Gradle/workflow/schema/UI/CI file changed or run. No CI started.
6. **Stage-0 remains** `in_progress / not accepted`; **Stage-1 remains NO-GO**; complete Legado compatibility remains NOT ESTABLISHED.

### 24.62.6 Next

Continue task 012 by closing method-family caller/host-port mappings for the inherited Rss/Web/login methods and remaining base groups, using exact pinned source callers and the existing host contracts. Prioritize missing behavior-bearing ports (source KV/catalog, Cookie/cache, login, product actions, file/archive/crypto/config, concurrency) over rechecking known isolated method deltas. Do not modify production code or start CI before Stage-0/authorization gates.

### 24.62.7 Inherited product-action and asynchronous bridge semantics

- **Rss source state:** `RssJsExtensions.put/get` delegate to `BaseSource.put/get` (lines 63–71); these are source-owned variables and must not be redirected to an unrelated global preference store.
- **Search/add/photo/open actions:** `searchBook` launches Badcat-equivalent search with either a search scope or a typed BookSource (75–85); `addBook` opens AddToBookshelfDialog (87–90); `showPhoto` opens PhotoDialog with source identity (92–95). `open` is a dispatcher (100–180+) whose pinned branches include login routing via BookSource/RssSource DAO lookup and SourceLoginActivity (105–135), RSS sorting (138–151) and RSS reading/open URL behavior (153+). These are script-visible actions inherited by WebJsExtensions and SourceLoginJsExtensions. Preserve the source-facing action contract, but route it to Badcat UI; do not import Legado Activities, dialogs, RSS Reader or DAO ownership.
- **Typed parser object contract:** `setContent(Any?, baseUrl?)` returns AnalyzeRule; `setBaseUrl`, `setRedirectUrl` and extraction helpers operate on AnalyzeRule. `getStringList` returns `List<String>?`; `getElement` returns `Any?`; `getElements` returns `List<Any>`. Replacing these with `String` or `List<Map<String,String>>` changes Rhino object identity and extraction semantics.
- **Web bridge RPC:** `WebJsExtensions.request(funName, jsParam, id)` is an asynchronous RPC dispatcher (lines 48–160), not a generic UI callback. It routes `run`, `ajaxAwait`, `connectAwait`, `getAwait`, `headAwait`, `postAwait`, `webViewAwait`, `webViewGetSourceAwait`, crypto/signing, file download/read/import and `getStringAwait`; result/error is stored in CacheManager under the request ID and returned through `window.$JSBridgeResult(id, success)` (161–166). `upConfig` deduplicates the last config and forwards it to a callback (38–43). No current typed host port models the full RPC method/argument/result/error/cache/callback protocol.
- **Login bridge:** `SourceLoginJsExtensions` forwards login UI data (`Map<String, Any?>?`) and view refresh delta callbacks (29–45), exposes clipboard copy and HttpTTS-specific cache clearing (47–61), and overrides `showBrowser` to launch a BottomWebViewDialog with source key/book type/preload/config/custom-button state (64–81). Generic `HostUiPort`/`VisibleBrowserPort` does not establish these LoginUiV2 and TTS/file lifecycle semantics.
- **Host boundary consequence:** `RuntimeHostPort.kt` has typed hidden WebView, visible browser, verification, generic UI and product-event ports, but no complete typed contracts for source KV, catalog lookup, WebJsExtensions RPC, login UI state, CacheManager result rendezvous, TTS cache cleanup, RSS action routing, file/archive, crypto object creation, read/theme configuration or source concurrency locks. The HTTP interface in SourceRuntime remains a partial DTO boundary.

### 24.62.8 Updated task disposition

The declaration/inheritance inventory and these additional inherited action/RPC semantics are now source-backed. The complete mapping remains open because not every base and inherited method has a fully traced production caller, typed host contract, and Badcat implementation owner. Static source inspection cannot settle compiled Rhino method exposure, coroutine/blocking behavior, callback ordering, or dynamic object return parity. Keep task 012 `in_progress`; no implementation is authorized by this audit.

### 24.62.9 Base method-family service ownership and host mapping

| Method family | Pinned upstream owner/dependency | Badcat host mapping result |
|---|---|---|
| Source identity, tag, navigation context and batch context | BaseSource, CoroutineContext and BatchContentContext; JsSourceEngine/AnalyzeRule/AnalyzeUrl provide execution-specific contexts | SourceIdentity/SourceExecutionContext exists, but there is no typed BaseSource/RuleDataInterface/Book/BookChapter/BatchContentContext bridge or proof of identical scope/object lifetime. |
| Batch chapter cache and refresh | `cacheContent` delegates to batch context; refresh methods post distinct EventBus refresh events | ProductEventPort covers three event labels, but typed chapter/index identity, batch write-back, event ordering and production page targeting are unproven. |
| HTTP and rate limiting | AnalyzeUrl + HttpHelper/OkHttp/Jsoup, StrResponse, CookieStore and ConcurrentRateLimiter | SourceRequestClient is partial; no authentic AnalyzeUrl contract, Jsoup Response/StrResponse object, full multi-value header/cookie/redirect/retry/WebView selection or sync wrapper semantics. |
| Hidden WebView execution/capture | BackstageWebView, WebViewPool and WebJsExtensions | HiddenWebViewPort has page/resource/override operations but full JS bridge injection, CacheManager result rendezvous, source object, cookies, navigation and pooled-resource lifecycle are not wired/proven. |
| Browser, verification and video | SourceHelp, SourceVerificationHelp, VideoPlay, Activity/dialog and source navigation policy | Generic UI/browser/verification ports cover some actions; typed video/product action exists, but source verification wait/refetch/error and navigation suppression need full caller-level adapter proof. |
| Cookie/session | CookieStore and CookieManager, including BaseSource/session and WebView synchronization | No complete CookieStore/CookieManager port or single-owner persistence/invalidation contract is declared in the inspected host modules. |
| Source file/archive | app externalCache, FileUtils, ArchiveUtils, LibArchiveUtils, encoding detection and source-relative path utilities | No typed file/archive port; direct Android cache/filesystem assumptions cannot be satisfied by SourceRequestClient or generic UI ports. |
| Font/TTF/cache | QueryTTF, CacheManager and font replacement helpers | No typed QueryTTF/font/cache service contract; host chapter cache and source KV cache are not substitutes for CacheManager/ACache/TTF cache. |
| Crypto/signing | JsEncodeUtils, SymmetricCryptoAndroid, AsymmetricCrypto, Sign and Hutool digest/HMAC | No typed crypto object factory/operation port; returning primitive strings would not preserve returned Java crypto object methods. |
| Read/theme/source config | ReadBookConfig, ThemeConfig, AppConfig | No host config port preserving both JSON and Map return forms, defaults, nulls and current Badcat preference ownership. |
| Logging/device/concurrency | AppLog, AppConst.androidId, SourceLock keyed by source identity | No diagnostics/device identity/concurrency lock port with source-key isolation, timeout, nested/reentrant and cancellation semantics. |
| Source variables and inherited product actions | BaseSource.put/get; RssJsExtensions search/add/photo/open; Login callback and Web RPC | No complete source-variable port or typed product-action/login/RPC contracts; generic HostUiAction and ProductEventPort are insufficient for all payloads and result channels. |

This mapping explains why a list of method names alone is not a usable compatibility boundary: methods return typed Java/Rhino objects, invoke stateful services, or cross asynchronous Android UI/WebView boundaries. Each corresponding host port must preserve arguments, return object, errors, null/default behavior, callback ordering, coroutine cancellation, source identity and resource release.

### 24.62.10 Current task progress

- **Completed in this slice:** base + inherited declaration/name counts; class inheritance; principal runtime implementers/callers; Rss product-action semantics; Web RPC protocol; login callback behavior; method-family service ownership and missing host-port groups.
- **Still open:** exact per-method external source/caller examples and full caller search across all relevant pinned files; full method-by-method typed port schema and ownership; compiled JVM/Rhino ABI, R8/DEX and dynamic behavior. The pinned source files establish declared behavior but not compiled exposure or runtime parity.
- **Task 012 remains `in_progress / partial static closure`.** No production/test/Gradle/workflow/schema/UI/CI files changed or run; no CI started.

### 24.62.11 Pinned script binding namespaces beyond java.*

- **Authoritative documentation source:** `app/src/main/assets/web/help/md/jsHelp.md` at the same pinned SHA. It documents the source-facing runtime as more than the `java` object or the 124 class-method names.

| Injected object / contract | Pinned documentation anchors | Required semantics | Badcat mapping status |
|---|---|---|---|
| `book` | JsHelp.md:492–531 | Large typed Book projection (URLs, source identity, metadata, progress, user overrides, variable) plus `putVariable/getVariable`; not a search-result `Map<String,String>`. | Current BookCandidate/BookDetail projections are not the complete upstream Book object or its mutable variable contract. |
| `chapter` | JsHelp.md:533–558 | Typed chapter URL/title/baseUrl/bookUrl/index/resourceUrl/tag/start/end/variable; variable access, update lifecycle, lyric and image URL methods. | Current reduced BookChapter DTO does not expose the full typed object or variable/update/media helper behavior. |
| `source` / `sourceApi` | JsHelp.md:560–605, 732–741 | Source key; source variables; custom key/value store; login headers and login info read/write/remove; refreshExplore/refreshJSLib. `sourceApi` is an explicit compatibility alias; old scripts declaring `var source = {...}` must continue to resolve the runtime entity through `sourceApi`. | No complete typed source entity/alias binding, login-info/header persistence, source variable owner or shared-jslib refresh contract has been proven in the production host runtime. |
| `cookie` | JsHelp.md:606–620 | `getCookie`, `getKey`, `setCookie`, `replaceCookie`, `removeCookie`, `setWebCookie`; includes WebView CookieManager synchronization. | No complete CookieStore/CookieManager typed host port or cross-runtime/WebView parity proof. |
| `cache` | JsHelp.md:622–642 | `put/get/delete`, `putFile/getFile`, `putMemory/getFromMemory/deleteMemory`; TTL in seconds, disk-vs-memory selection and large-file behavior. | Host source KV/file/chapter caches are separate implementations; no unified CacheManager/ACache contract or namespace parity. |
| `config` and source-function ABI | JsHelp.md:732–780 | Distinguishes config from source entity; lists source type values and BookType bit values; defines search/explore/detail/chapters/content/contentBatch/login/loginUi/loginAction return contracts. | Existing source DTO and orchestration do not yet prove full pinned source object and all function return/object-marshalling semantics. |

Additional script caller/contract evidence in the same pinned guide:
- Book and chapter are passed as live typed objects into source functions; `getContent(chapter, book, nextChapterUrl)` receives the next URL as nullable at the last chapter (885–898).
- `getContentBatch(chapters, book)` receives a chapter array; each `java.cacheContent(chapters[i], content)` must preserve chapter identity. URL-string cache-back is allowed only when unique within the batch; duplicate URLs must use the chapter object (900–935). The guide states cached content receives replaceRegex and uncached chapters fall back to `getContent`.
- Login documentation writes login headers via `source.putLoginHeader`, stores login info via `source.putLoginInfo`, and asks the login UI to call `java.upLoginData` (782–843). This joins the source entity and SourceLoginJsExtensions callback contract; a one-way UI launch callback does not close it.

### 24.62.12 Documentation/source declaration drift

| Surface | Pinned JsHelp.md | Pinned Kotlin declaration | Disposition |
|---|---|---|---|
| `java.ajax` timeout | Describes `callTimeout: Int?` (around line 203) | Base JsExtensions overload is `Long?` (lines 176–180); WebJsExtensions wrapper accepts `Int` (183+) | Documented call contract spans different concrete bridge classes; must be verified against Rhino/WebView overload dispatch, not normalized by assumption. |
| `java.upLoginData` | Describes `Map<String, String?>?` (line 86) | SourceLoginJsExtensions uses `Map<String, Any?>?` (lines 29–36) | Static type/documentation mismatch; preserve actual accepted script payloads and test marshalling. |
| `java.cacheContent` | Guide shows both chapter object and URL-string usage (377, 391–407, 921–930) | JsExtensions has one `cacheContent(chapter: Any?, content: String)` declaration (149+) | URL string is a value accepted by the Any? parameter under documented uniqueness constraints, not a second Kotlin overload. |

These are source/documentation facts and unresolved bridge-contract questions, not observed dynamic failures. The source guide itself is part of the pinned compatibility evidence and must be versioned with the code pin.

### 24.62.13 Expanded ABI scope and task status

The 124-name count covers the Java extension class surfaces only. The effective script ABI also includes the injected `source/sourceApi`, `cookie`, `cache`, `book`, `chapter`, `config` namespaces and the JavaScript source function protocol. These are independently typed/stateful objects, not aliases for `Map<String,String>` or the existing `SourceRuntime` DTOs. Task 012 therefore remains **in_progress / partial static closure** until each namespace has an explicit owner, typed host port, lifecycle and differential contract. No production/test/Gradle/workflow/schema/UI/CI files changed or run; no CI started; Stage-0 remains not accepted and Stage-1 remains NO-GO.

## 24.63 Runtime-3.0 Stage-0 — script binding namespace ownership and actual injection closure (2026-11-25)

- **Task:** `R3-S0-JS-EXTENSIONS-API-CALLER-CLOSURE-012`; continuation of overview §4.12, no new task or implementation authorization.
- **Inspection baseline:** Badcat branch `integration/legado-upstream-runtime` at start HEAD `2fcce5a0ba9465ce771b8023f69f686d0c8df280`; pinned upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Method:** inspected the actual `RhinoSourceScriptRuntime` injection code, production factory wiring, source/login/cookie/cache adapters, book/chapter DTO and caller pipeline, host surface inventory test, plus pinned BaseSource/CookieStore/CacheManager/Book/BookChapter/RuleDataInterface and JsHelp.md. Findings below are static source facts unless explicitly labelled dynamic-unverified.

### 24.63.1 Actual script-scope injection is narrower than the documented namespace set

| Namespace | Pinned contract | Actual Badcat source-runtime injection | Disposition |
|---|---|---|---|
| `java` | BaseSource.evalJS binds the current source object as `java` (upstream `BaseSource.kt:413–433`) and the public JsExtensions object surface | `RhinoSourceScriptRuntime.kt:285–457` injects `RhinoSafeSourceJavaExtensions` | Present as a restricted host facade; not the authentic BaseSource/JsExtensions object. Existing method-level deltas remain in §§24.37–24.46 and §24.62. |
| `source` | Upstream binds `BaseSource` itself (`BaseSource.kt:416–420`); JsHelp.md:560–605 describes source key, source variables, custom KV, login headers/info and refresh methods | `RhinoSourceLoginExtensions` is injected at `RhinoSourceScriptRuntime.kt:466–475` | Partial login/source-state facade, not the upstream entity. It currently declares `getKey`, `getVariable/setVariable`, login header/map methods and login info/map methods; it does not declare documented `source.put(key,value)`, `source.get(key)`, `refreshExplore()` or `refreshJSLib()` (`SourceLoginSession.kt:151–242`). |
| `sourceApi` | Pinned BaseSource.evalJS explicitly binds both `source` and `sourceApi` to the same object (`BaseSource.kt:416–420`); JsHelp.md:732–741 says this alias preserves older scripts that declare a `source` config object | No `scope["sourceApi"]` assignment exists in the inspected source-runtime injection block (`RhinoSourceScriptRuntime.kt:457–476`) | Static missing-binding gap. No dynamic script compatibility result is claimed. |
| `cookie` | JsHelp.md:606–620 includes `getCookie/getKey/setCookie/replaceCookie/removeCookie/setWebCookie`; pinned CookieStore also synchronizes WebView cookies (`CookieStore.kt:41–53`) | `RhinoSourceCookieExtensions` is injected (`RhinoSourceScriptRuntime.kt:459–465`) and exposes getCookie/getKey/setCookie/replaceCookie/removeCookie(url)/removeCookie(url,key) (`RhinoSourceCookieExtensions.kt:7–40`) | `setWebCookie` is absent from the script object. `DefaultProcessCookieBridge` and `SharedPreferencesSourceCookieJar` provide separate host synchronization operations, but the inspected script object has no `setWebCookie` method and this is not proof that the documented script call is wired. |
| `cache` | JsHelp.md:622–642 documents put/get/delete, putFile/getFile, putMemory/getFromMemory/deleteMemory and `get(key, onlyDisk)` | `RhinoSourceCacheExtensions` is injected (`RhinoSourceScriptRuntime.kt:476`) | Partial source-scoped adapter; it has no `get(key, onlyDisk)` overload or ByteArray API. Its memory map is per extension instance, durable values use source KV, and file values use the separate host file cache (`RhinoSourceCacheExtensions.kt:13–150`). This is not the upstream global 50 MiB LRU + Room CacheDao + ACache behavior already audited in §24.53. |
| `book` / `chapter` | JsHelp.md:492–558 describes live Book/BookChapter fields and mutable variable/update methods; upstream `BaseSource.evalLoginUiV2/evalLoginActionV2` also binds typed Book/BookChapter when supplied (`BaseSource.kt:120–155`) | No `scope["book"]` or `scope["chapter"]` assignment appears in `RhinoSourceScriptRuntime.kt:457–476`; its generic request bindings are string-valued and only `page` is converted to Int (`:219–277`) | Static missing typed binding in the source-runtime path. Host `ReadingEngine.BookCandidate/BookDetail/BookChapter` are reduced DTOs (`ReadingEngine.kt:136–173`), not upstream entities. `RhinoLegadoJsReplacementEvaluator` binds separate replacement-only context snapshots (`:65–69`), which does not satisfy source function Book/Chapter object ABI. |
| `config` | JsHelp.md:732–780 explicitly describes `config` as a normal object declared by the source script; it is not one of the host-injected BaseSource objects. | `RhinoSourceScriptRuntime` evaluates nonblank `mainJs` into the persistent source scope (`:489–494`); it does not inject a host `config` property. | Do not classify absence of `scope["config"]` as a host-port defect. The unresolved issue is whether the source declared function protocol is actually invoked; see §24.64. `sourceDescriptor()` is a separate read-only map exposed through `java.getSource()` (`:100–148`, `RhinoSafeSourceJavaExtensions.kt:208–212`). |

The current Debug host-surface inventory test reports four `RhinoSourceScriptRuntime` objects (`java`, `cookie`, `source`, `cache`) and separately lists `result/book/chapter` under `RhinoLegadoJsReplacementEvaluator` (`RhinoHostSurfaceInventoryTest.kt:80–90`). The latter must not be misread as source-runtime injection evidence. The test itself explicitly says it inventories the current host surface and does not establish upstream parity (`:25–27`, `:93–96`).

### 24.63.2 Source namespace ownership and missing methods

- Upstream `BaseSource.putVariable/getVariable` uses CacheManager key `sourceVariable_${getKey()}`; generic `put/get` uses a distinct `v_${getKey()}_${key}` namespace (`BaseSource.kt:327–375`). `refreshExplore` and `refreshJSLib` have their own thread/lifecycle effects (`:377–401`).
- Host `RhinoSourceLoginExtensions.getVariable/setVariable` uses one private key `__legado_source_variable__` in `SourceScriptKeyValueStore` (`SourceLoginSession.kt:162–173`). Login headers and login info are separately stored through `SourceLoginSessionStore` (`:174–202`). The host class does not implement the generic source KV namespace or either refresh method.
- `RhinoSafeSourceJavaExtensions` has its own `java.get/put` storage helpers (for example `:851`, `:1126`), but a `java` namespace method is not the documented `source.get/put` contract and must not be counted as closing it.
- `BadcatReadingEngineFactory` wires persistent source KV, login session storage and Cookie operations into the runtime (`BadcatReadingEngineFactory.kt:56–103`). This establishes selected production dependencies, not complete upstream namespace parity. Source catalog import/replacement invalidation is separately wired through `FileBookSourceCatalog`; its constructor callback defaults to no-op (`FileBookSourceCatalog.kt:28–31`) and production callback ownership must be considered together with the multi-runtime invalidation finding in §24.46/§24.54.

### 24.63.3 Cookie and cache host-service ownership

- Pinned CookieStore uses the registrable-domain bucket, DB persistence, CacheManager memory cache and WebView CookieManager synchronization (`CookieStore.kt:34–53`, `:55–100`, `:110–152`). It also exposes `setWebCookie` as a host operation.
- Badcat `SharedPreferencesSourceCookieJar` is a deliberate host persistence projection with persistent and process-session maps (`SharedPreferencesSourceCookieJar.kt:25–83`, `:93–145`); it supports merge/import/removal and domain clear (`:147–205`, `:259–282`). `DefaultProcessCookieBridge` is a separate default-process bridge around Android WebView CookieManager (`DefaultProcessCookieBridge.kt:86–96`, `:130–159`). The runtime factory passes Cookie reader/writer/replacer/remover/key-remover delegates (`BadcatReadingEngineFactory.kt:80–103`), but the injected `RhinoSourceCookieExtensions` still has no `setWebCookie` callable. The exact production call sites for the separate bridge do not change this method-surface fact.
- Pinned CacheManager has synchronized Room-backed put/get/delete, `onlyDisk`, primitive getters, ByteArray/ACache paths, global LRU memory and file APIs (`CacheManager.kt:53–165`); WebCacheManager is a JS-visible wrapper (`:167–207`). Badcat `RhinoSourceCacheExtensions` stores cache values in source-prefixed `SourceScriptKeyValueStore`, uses a per-object ConcurrentHashMap for memory and a bounded file store (`RhinoSourceCacheExtensions.kt:13–150`). This is an intentional isolated implementation slice, not the same global CacheManager data owner or full API.

### 24.63.4 Book/chapter and config caller distinction

- The pinned JS guide passes Book and BookChapter objects directly to `getBookInfo`, `getChapters`, `getContent(chapter, book, nextChapterUrl)` and `getContentBatch(chapters, book)` (JsHelp.md:855–935); batch cache-back must preserve chapter identity, including duplicate URL cases (`:900–933`).
- Badcat source business callers currently pass source/response plus key/page to parser `parseWithScripts` (`SourceSearchService.kt:35–37`, `SourceExploreService.kt:42–48`, `SourceBookDetailService.kt:25–26`, `SourceTocService.kt:54–55`, `SourceChapterContentService.kt:88–92`). The chapter-content replacement path separately constructs `LegadoReplacementBookContext` and `LegadoReplacementChapterContext` snapshots (`SourceChapterContentService.kt:113–128`), and the replacement evaluator binds those to `book/chapter` (`RhinoLegadoJsReplacementEvaluator.kt:65–73`). These replacement-only snapshots are not the upstream mutable Room entities or the script function input objects.
- Host `RhinoSafeSourceJavaExtensions.getBatchContext()` returns null and `cacheContent()` throws when invoked (`RhinoSafeSourceJavaExtensions.kt:193–203`); therefore the pinned batch-content contract is not closed by the existence of a chapter cache or the separate replacement context.

### 24.63.5 Task disposition and next

1. **Completed in this slice:** exact current source-runtime injected object list; namespace-by-namespace method/owner comparison for source/sourceApi, cookie, cache, book, chapter and config; distinction between replacement-only Book/Chapter snapshots and source-function objects; actual search/explore/detail/TOC/content caller anchors; host KV/login/cookie/cache service ownership.
2. **Still NOT MET:** authentic BaseSource and typed Book/BookChapter runtime objects; sourceApi/config bindings; complete source methods and refresh lifecycle; cookie.setWebCookie script method; CacheManager API/data-owner parity; BatchContentContext and chapter-identity cache-back; compiled JVM/Rhino ABI, WebView bridge exposure, Release/R8 and differential execution.
3. **No dynamic failure claim:** the above are static declarations/injection/caller findings. Existing host tests/inventory were read as source evidence only; no test command, Gradle build, device test or CI run was performed in this slice.
4. **Task 012 remains** `in_progress / partial static closure`; **Stage-0 remains** `in_progress / not accepted`; **Stage-1 remains NO-GO**; complete Legado compatibility is not established.
5. **Next slice:** reconcile the script function protocol (`search/explore/getBookInfo/getChapters/getContent/getContentBatch/login/loginUi/loginAction`) against each current service/parser caller and determine which functions are actually invoked, which return shapes are supported, and which Badcat adapters are absent. Reuse §24.63 and §24.49–§24.52; do not repeat object injection findings.

### 24.63.6 Clarification — config is source-declared, not a host-injected namespace

The pinned JsHelp.md:732–741 states that config is an ordinary configuration object declared by the source script, while source/sourceApi refer to the runtime BookSource entity. Therefore, the lack of a host config assignment is **not itself a compatibility defect**. Badcat does evaluate mainJs once into the source scope (RhinoSourceScriptRuntime.kt:489–494), so a config object declared there can exist. The remaining question is whether the named JS-source functions that consume config are dispatched with the correct arguments and return contracts; §24.64 audits that separately.

## 24.64 Runtime-3.0 Stage-0 — declarative rule scripts versus the pinned JS-source function protocol

- **Task:** continuation of R3-S0-JS-EXTENSIONS-API-CALLER-CLOSURE-012 (overview §4.12).
- **Pinned source:** JsSourceEngine.kt, JsSourceBook.kt, WebBook.kt, AnalyzeRule.kt, AnalyzeUrl.kt and JsHelp.md at 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Host source:** current SourceReadingEngine, search/explore/detail/TOC/content services, parsers, SourceScriptRuntime, MainJsSourceGuard and Login UI v2 service at branch HEAD 98a09a2af19b62499d9a3f574d169a93fef0594d.

### 24.64.1 Pinned JS-source execution protocol

- JsSourceEngine is an explicit top-level function executor. It builds a fresh scope per invocation, binds java, source, sourceApi, baseUrl, cookie, cache and typed function args, chains the shared source scope, evaluates mainJs, then evaluates the named function call (JsSourceEngine.kt:31–125).
- callFunction requires a function and throws if missing; callFunctionIfExists reports absence distinctly. callContentBatch installs BatchContentContext only for the duration of getContentBatch, then clears it in finally (JsSourceEngine.kt:42–64, :67–102).
- JsSourceBook dispatches: search(key,page) (:27–42); explore(url,page) (:45–56); optional getBookInfo(book) with merge/fallback (:59–85); required getChapters(book) with typed Book (:88–110); getContent(chapter,book,nextChapterUrl) with typed objects and nullable next URL (:112–141); optional getContentBatch(chapters,book) with typed chapter list and per-chapter save tokens (:143–180).
- WebBook chooses JsSourceBook when bookSource.isJsSource(); declarative sources instead use AnalyzeUrl/AnalyzeRule and normal rule parsing (WebBook.kt:53–75, :131–160, :202–245, :395–430, :510–528). These are distinct upstream execution modes, not two spellings for a CSS rule.

### 24.64.2 Current Badcat path is a per-field @js rule evaluator, not JsSourceEngine

| Upstream JS-source function | Pinned caller/argument contract | Current Badcat production path | Static disposition |
|---|---|---|---|
| search(key,page) | JsSourceBook.searchAwait calls JsSourceEngine.callFunction("search", key/page); WebBook routes JS sources to it | BookSourceSearchUrlResolver requires a nonblank searchUrl; SourceSearchService performs HTTP first, then CssSearchResultsParser.parseWithScripts (BookSourceSearchUrlResolver.kt:10–35; SourceSearchService.kt:12–37) | No named search function dispatch is present in the inspected host service path. The parser runs declarative ruleSearch fields and individual @js expressions. |
| explore(url,page) | JsSourceBook.exploreAwait calls named explore; WebBook routes JS sources separately | SourceExploreService requires exploreUrl, performs HTTP, then calls the same CSS/field parser with EXPLORE stage (:12–51) | No named explore function dispatch in this service path. |
| getBookInfo(book) | Optional named function receives live upstream Book and merges returned detail fields; null/missing falls back (JsSourceBook.kt:59–85) | SourceBookDetailService fetches bookUrl and invokes CssBookInfoParser.parseWithScripts (SourceBookDetailService.kt:9–26) | No named function call or live Book argument. The host parser extracts rule fields into reduced BookDetail DTO. |
| getChapters(book) | Required named function receives live Book and returns chapter array (JsSourceBook.kt:88–110) | SourceTocService fetches tocUrl/book URL, follows CSS next-page links, then CssTocParser (SourceTocService.kt:10–88) | No named function call or live Book argument. It is a declarative TOC parser path. |
| getContent(chapter,book,nextChapterUrl) | Required named function receives live BookChapter + Book and nullable next-chapter URL (JsSourceBook.kt:112–141) | SourceChapterContentService fetches chapter URL and runs CssChapterContentParser.parseWithScripts per page (:17–97) | No named function call or live Book/BookChapter objects; per-field script bindings are result/baseUrl/source metadata/ruleName/page. |
| getContentBatch(chapters,book) | Optional named function receives typed chapter list + Book under BatchContentContext; missing function falls back to single chapter (JsSourceBook.kt:143–180) | CssChapterContentParser explicitly rejects nonblank contentBatch; RhinoSafeSourceJavaExtensions.getBatchContext() returns null and cacheContent() throws (CssChapterContentParser.kt:24–32; RhinoSafeSourceJavaExtensions.kt:193–203) | Batch JS-source function and typed per-chapter cache-back are not implemented in this path. |
| login()/loginUi(state)/loginAction(action,state,form) | JsHelp.md:765–843 describes legacy login and dynamic Login UI v2; upstream BaseSource binds source plus optional typed Book/BookChapter for v2 (BaseSource.kt:120–155) | LegadoSourceLoginUiV2Service explicitly evaluates loginUi(...) and loginAction(...) through SourceScriptRuntime; checkLogin evaluates loginCheckJs (LegadoSourceLoginUiV2Service.kt:19–115) | Dynamic v2 hooks are a partial named-call implementation. It does not bind typed Book/BookChapter in the invocation and its class documentation states HTTP/WebView/full CookieStore are not a complete authentication host. No matching top-level legacy login() invocation was found in this inspected service. |

MainJsSourceGuard only allows a nonblank mainJs when a generic script runtime is present (MainJsSourceGuard.kt:8–19). It does not check whether a JS-source function exists or dispatch it. RhinoSourceScriptRuntime evaluates mainJs once, then evaluates the supplied field script (RhinoSourceScriptRuntime.kt:489–495); this is not equivalent to JsSourceEngine.callFunction(name,args).

### 24.64.3 Per-field rule invocation shape

- CssSearchResultsParser.parseWithScripts invokes SourceScriptRuntime.evaluate for a bookList @js expression with string bindings result/baseUrl/sourceUrl/sourceId/sourceName/ruleName/key/page (CssSearchResultsParser.kt:141–159); individual result-field @js expressions use a similar string binding set (:259–303).
- CssBookInfoParser.parseWithScripts evaluates init and scalar field @js expressions with result/baseUrl/source metadata/ruleName, not getBookInfo(book) (CssBookInfoParser.kt:32–66, :70–120).
- CssChapterContentParser.parseWithScripts evaluates content/title/nextContentUrl/replaceRegex expressions with string bindings and explicitly rejects webJs/sourceRegex/subContent/imageDecode/payAction/callBackJs/contentBatch (CssChapterContentParser.kt:24–32, :55–73, :88–185).
- The host SourceScriptRuntime contract is evaluate(source, script, bindings: Map<String,String>), while upstream JS-source calls accept Any? values and bind actual Book/BookChapter/list objects (SourceRuntime.kt:78–93; JsSourceEngine.kt:55–115). This is a structural protocol difference, not merely a missing overload.

### 24.64.4 Task disposition

1. **Completed:** mapped all pinned JS-source function names, required/optional status, typed arguments, fallback behavior and batch lifecycle; traced current Badcat service callers and parser dispatch shape; separated dynamic Login UI v2 partial support from the full JS-source protocol.
2. **NOT MET:** JsSourceEngine equivalent and function dispatch for search/explore/detail/TOC/content/batch; source-specific result marshalling to upstream Book/BookChapter; typed Book/Chapter function arguments; BatchContentContext/cache-back; legacy login function invocation; complete login/auth host integration.
3. **Important scope correction:** config is source-declared in mainJs, not a missing host-injected object. The actual blocker is that the current Badcat production path does not dispatch the declared JS-source functions that consume it.
4. **Evidence type:** static source/caller evidence only. No test, Gradle build, CI or device execution was performed; no dynamic failure claim is made.
5. **Task 012 remains** in_progress / partial static closure; Stage-0 remains in_progress / not accepted; Stage-1 remains NO-GO.
6. **Next:** map the JS-source return marshalling and source-type routing gates (isJsSource, config/source type and import normalization), including missing/malformed/empty result behavior and host BookCandidate/BookDetail/BookChapter conversion. Reuse §24.64 and §24.50–§24.52; do not re-audit method declarations.
