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
