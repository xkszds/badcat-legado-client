# Runtime-1.2 — C cut-set and typed-port contract audit

- Registration commit: df83f3cf46227fe1d69eacc0b3b67bff34524687
- Audited starting HEAD: df83f3cf46227fe1d69eacc0b3b67bff34524687
- Branch: integration/legado-upstream-runtime
- Pinned behavior baseline: LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a
- Scope: source/caller/dependency/API audit and documentation only.

## 1. Decision

**NO-GO for a C extraction compiler PoC on current evidence.** The inspected source establishes real platform/data coupling and concrete JS API gaps, but it does not establish a compiler-resolved minimum cut set, complete dynamic JS ABI, or full persistence/lifecycle equivalence. A compile experiment now would risk hiding behavior behind incomplete ports.

This does not prove C impossible. It means Runtime-1.2 acceptance is not met. The next step must be a separately registered static-contract completion task. No production integration is authorized.

## 2. Pinned source roots and exact blobs

The pinned upstream recursive tree has 3,298 entries and was not truncated. The following source blobs were inspected:

| Root | Pinned path | Blob SHA |
|---|---|---|
| BaseSource | app/src/main/java/io/legado/app/data/entities/BaseSource.kt | 9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa |
| BaseBook | app/src/main/java/io/legado/app/data/entities/BaseBook.kt | a469dcd99da68d3951a034c5715327e9f4c91900 |
| BookSource | app/src/main/java/io/legado/app/data/entities/BookSource.kt | 7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c |
| Book | app/src/main/java/io/legado/app/data/entities/Book.kt | 05024a6c71404eb49c23cd1df0482df365bc63fe |
| BookChapter | app/src/main/java/io/legado/app/data/entities/BookChapter.kt | 9798267c77c9e14d0a673a822930491cd6e44cc1 |
| JsExtensions | app/src/main/java/io/legado/app/help/JsExtensions.kt | f6808edd2a0d312e8bd13649935dea444cb82581 |
| JsSourceEngine | app/src/main/java/io/legado/app/model/jsSource/JsSourceEngine.kt | 44452025f81d8751afee92d3ee1f2d52f9e55905 |
| SharedJsScope | app/src/main/java/io/legado/app/model/SharedJsScope.kt | dab7770d746c851c06ad05c08a35a2866a6100b3 |
| BatchContentContext | app/src/main/java/io/legado/app/model/BatchContentContext.kt | 68cad877f048b171e4f0f6cf22b1e0dd1e6481f9 |
| CacheManager | app/src/main/java/io/legado/app/help/CacheManager.kt | 9575697d8ed60c0864aa3fd52d548ba059054205 |
| CookieStore | app/src/main/java/io/legado/app/help/http/CookieStore.kt | 09720d79f7009db052c080b54fc5fa2805257b64 |
| BookHelp | app/src/main/java/io/legado/app/help/book/BookHelp.kt | 78e9e82b67145b835678c7aa1111eaf1631b77d2 |
| AnalyzeRule | app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt | 7591a026a072ac8a6fe4902add0795812fe0a0a6 |
| AnalyzeUrl | app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt | d898c88dfd844d73f06d9396d0b46f39939f7e6c |
| LoginUiV2 | app/src/main/java/io/legado/app/model/login/LoginUiV2.kt | fd4fca1baa694a821400a46d08ff20d6607f4b96 |
| AppDatabase | app/src/main/java/io/legado/app/data/AppDatabase.kt | d3d516c7b3e73ef4ab3beb373e8e8ec3d03c204c |

Additional inspected upstream blobs:
- BookSourceExtensions.kt: e81118c3dc32b6c708d5c762f44739a0346f53e5
- BaseSourceExtensions.kt: 4b70e9848dde0c3adad44f8cc5147a2abe128cb1
- SourceHelp.kt: 09145629dfb8d92772cf5fa339fda5ed33fb555d
- BackstageWebView.kt: 285f229ff2f9f00e7588b221a8543a2879bafe86
- SourceVerificationHelp.kt: d15769767513ca23bdbcc633709a348ae09c5b08
- CookieManager.kt: 5d90e52d008f7b71540a402d3e6dec4d6e501dba
- HttpHelper.kt: 2b69aae0123972a5b62e89e6d0b5ea99313d67b4
- ContentProcessor.kt: 4afcd448d4d02d121a2160ac1b1768b8bdd2d6ae

DAO blobs: BookSourceDao 1c4fbe213aceb96393f1795abe46be6d09249154; BookDao 1bbdd64cfe9ee60254837aef795a2063125cea82; BookChapterDao 91c812847908e637343613d7ef43248611efd3e9; CacheDao 2d6ababf7c317367c9d0fc71034e060ef6bc2ce3; CookieDao f08cb75969e964ab4f2494f6ec720f9164bc8b89.

## 3. Dependency groups and proposed cut edges

This is a source-level coupling map, not a compiler-proven SCC decomposition.

| Group | Verified relationship | Candidate port | Unresolved semantics |
|---|---|---|---|
| Source identity/lifecycle | BookSource implements BaseSource; BaseSource extends JsExtensions and invokes source JS, login, Cookie, cache and source state | SourceRepository + SourceRuntimeSession | authentic source identity, login lifecycle, JS object binding, cancellation/restart |
| Rule execution | AnalyzeRule dispatches selector/regex/XPath/JSONPath/JS; AnalyzeUrl handles URL directives and request context | RuleExecutionHost + RequestTransport | overloads, request options, redirects, status/final URL/charset, error/cancellation parity |
| JS runtime | JsSourceEngine + SharedJsScope + BaseSource.evalJS bind source/global objects and shared libraries | ScriptHost + JsExtensionFacade | complete function/overload ABI, reflection/dynamic names, scope invalidation and Rhino lifecycle |
| Batch content | JsSourceEngine batch dispatch → BatchContentContext → BookHelp save token/fence | BatchContentPort + ChapterBodyStore | context lifecycle, URL aliases, token capture, stale-write rejection, resource commit |
| Book/chapter persistence | Book and BookChapter directly call appDb DAOs; BookChapter also has app-context side effects | BookRepository + ChapterRepository | full DAO callers, transaction/invalidation and restart behavior |
| Cache/Cookie/source state | CacheManager uses Room KV plus memory/file cache; CookieStore uses cookie DAO and Android CookieManager | GenericCacheStore + CookieSessionStore + SourceStateStore | TTL, domain merge, WebView sync, encryption, cleanup and process restart |
| Platform/product callbacks | JsExtensions directly uses Activity, WebView, dialogs, browser/verification, navigation, video and reader/theme callbacks | WebViewHost + VerificationHost + BrowserHost + ProductCallbacks | UI-thread dispatch, foreground Activity, lifecycle, callback result/error and cancellation |

Imports, lexical reachability and declaration scans cannot prove Kotlin overload resolution, reflection, generated/resource references or runtime JS call graphs. The minimum cut set and true SCC groups therefore remain unknown.

## 4. JS API comparison and concrete behavior gaps

A declaration-name scan compared pinned JsExtensions.kt with Badcat RhinoSafeSourceJavaExtensions.kt at the audited HEAD. It found 122 upstream function declarations (overloads counted) and 225 host bridge declarations. This is only a name-level scan, not a full signature or Rhino reflection test.

The following 17 upstream public method groups were not found in the host bridge:
- refreshBookInfo, refreshBookToc, refreshContent
- webView, webViewGetSource, webViewGetOverrideUrl
- openVideoPlayer, startBrowser, startBrowserAwait
- getVerificationCode, openUrl, showBrowser
- getReadBookConfig, getReadBookConfigMap, getThemeMode, getThemeConfig, getThemeConfigMap

The inspected production factory does not wire equivalent WebView/browser/verification/refresh/reader-theme callbacks. They remain unresolved until every source-visible overload, JS exposure and result contract is mapped.

Additional concrete gaps:
- Upstream JsExtensions.cacheContent requires an active BatchContentContext, resolves chapter identity/URL and uses a current ContentSaveToken. Current host bridge obtains no batch context and throws for this operation. This is unsupported behavior, not parity.
- Upstream BaseSource.evalJS binds the authentic source object as java, source and sourceApi, plus baseUrl, cookie, cache and shared-scope behavior. Current host runtime uses source projection/facade objects; equivalence is unproven.
- Upstream BaseSource includes legacy RowUi login behavior as well as LoginUiV2. Current Badcat LegadoSourceLoginUiV2Service handles V2; legacy RowUi rendering/default extraction is not proven.
- Host ReadingEngine.BookSource is a projection, not the pinned upstream model. Its six rule fields are JSON strings rather than the six typed rule properties. A matching field count is not a round-trip contract.

## 5. Persistence and single-writer boundary

Pinned AppDatabase is Room v113 with 26 entities. BookSource, Book, BookChapter, Cache and Cookie use distinct DAO contracts. BookSourceDao includes CRUD, enable/explore/group/order/check-revision/flow operations; BookDao includes shelf/group/read-config/progress/metadata operations; BookChapterDao includes ordered index reads, insert/update/content metadata and deletion. CacheDao and CookieDao are separate stores.

At audited Badcat HEAD, inspected owners include:
- FileBookSourceCatalog: source JSON via AtomicFile and host projection.
- BadcatReadingStateRepository: progress/bookmarks/history via SharedPreferences.
- SourceTocService: in-memory TOC ownership.
- FileChapterContentCache: chapter body files and host save fence.
- RhinoSourceScriptRuntime and source-specific SharedPreferences stores: script variables, cache, Cookie and login state.

These separate backends may remain separate implementations, but each data domain needs a declared fact source, sole writer, consistency/invalidation behavior and crash/restart semantics. A generic key-value port is not equivalent to Room DAO contracts or chapter-body token fencing. No migration or single-writer cutover is authorized.

## 6. Typed port contract inventory

| Port | Minimum contract | Status |
|---|---|---|
| SourceRepository | full typed BookSource round-trip, stable URL identity, six rule objects, unknown JSON fields, enable/order/group/check revision and flows | incomplete |
| BookRepository | full Book fields, ReadConfig/ParagraphAnchor, unique name/author, save/delete/update preservation and transactions | incomplete |
| ChapterRepository | full BookChapter fields, (url,bookUrl) identity, unique (bookUrl,index), large variables and update suppression | incomplete |
| RequestTransport | method/body/header/options, bytes/charset/status/final URL, redirect/retry/timeouts, Cookie, cancellation and exceptions | incomplete |
| ScriptHost/RuleHost | exact JS globals, names/overloads, this/globalThis, per-source/shared scope, dynamic dispatch, result conversion and errors | incomplete |
| BatchContentPort | context lifecycle, chapter identity/URL aliases, token capture, stale-write fence, batch close/cancel and resource commit | incomplete |
| GenericCacheStore | memory LRU, Room KV TTL, file cache, source-variable namespaces, invalidation and cleanup | incomplete |
| CookieSessionStore | domain/session persistence, merge/replace/remove, OkHttp and Android WebView cookie synchronization | incomplete |
| LoginHost | legacy RowUi + V2, defaults, action/check result, encrypted info/header, Cookie propagation and restart | incomplete |
| WebView/Browser/VerificationHost | load modes, headers/cookies, JS eval, URL overrides, redirect, timeout, SSL, foreground Activity, UI dispatch and cancellation | incomplete |
| ProductCallbacks | refresh book/TOC/content, open URL/import, reader/theme, toast/log callbacks and script-visible results | incomplete |
| PlatformContext | application vs foreground Activity, paths, Android ID, permissions, dispatchers, lifecycle and process recreation | incomplete |

Exact Kotlin signatures cannot safely be frozen yet: overload inventory, dynamic invocation and caller semantics are incomplete. Provisional signatures would conceal unknown behavior, so none are presented as accepted contracts.

## 7. Root/cut inventory (machine-readable table)

| Root symbol | Exact pinned source | Caller/dependency evidence | Proposed port | Evidence class | Status |
|---|---|---|---|---|---|
| BookSource | entities/BookSource.kt | implements BaseSource; Room/Parcelable/rule models | SourceRepository + SourceRuntimeSession | declaration/import/inheritance | blocked |
| BaseSource | entities/BaseSource.kt | extends JsExtensions; source JS/login/cache/Cookie | SourceRuntimeSession + ScriptHost | declaration/direct calls | blocked |
| Book | entities/Book.kt | appDb Book DAO + BookHelp/reader helpers | BookRepository | direct DAO calls | blocked |
| BookChapter | entities/BookChapter.kt | appDb chapter/replace-rule DAO + app context | ChapterRepository + PlatformContext | direct DAO/platform calls | blocked |
| JsExtensions | help/JsExtensions.kt | WebView/browser/verification/refresh/reader callbacks | JS facade + product callback ports | declaration/import/method scan | blocked |
| JsSourceEngine | model/jsSource/JsSourceEngine.kt | mainJs dispatch + batch context | ScriptHost + BatchContentPort | source declaration/call chain | blocked |
| SharedJsScope | model/SharedJsScope.kt | shared library scope, cache and lifecycle | ScriptHost | source declaration/import | blocked |
| AnalyzeRule/AnalyzeUrl | model/analyzeRule/*.kt | parser/JS/HTTP/Cookie/cache/source context | RuleExecutionHost + RequestTransport | source declaration/import | blocked |
| CacheManager/CookieStore | help/CacheManager.kt; help/http/CookieStore.kt | direct Room DAO + memory/file/Android CookieManager | GenericCacheStore + CookieSessionStore | direct DAO calls | blocked |
| BatchContentContext/BookHelp | model/BatchContentContext.kt; help/book/BookHelp.kt | batch token and chapter save/resource commit | BatchContentPort + ChapterBodyStore | declaration/direct calls | blocked |
| LoginUiV2 | model/login/LoginUiV2.kt | V2 contract; BaseSource also retains legacy RowUi | LoginHost | declaration/call path partial | blocked |

Paths in this table are relative to app/src/main/java/io/legado/app. No row is claimed to be a compiler-resolved call graph proof.

## 8. Exact blockers and next gate

1. Enumerate every public/JS-visible JsExtensions signature and overload, including Rhino ScriptBindings/reflection and dynamic names.
2. Trace source search, explore, detail, TOC, content, batch, login, Cookie, cache and source-variable roots through all callers.
3. Inventory upstream and host reads/writes, transactions, invalidation and restart behavior for every data domain.
4. Produce a compiler-resolved dependency/call graph or reproducible equivalent, separating reflection/generated/resource/JS edges and marking unknown edges.
5. Freeze typed Kotlin port signatures only after exact null/error/thread/cancellation/lifecycle/state semantics are evidenced.
6. Prove the proposed boundary excludes Legado product UI/Reader and has no competing AppDatabase/DAO authority.

Only after these gates may a new registration authorize a disposable compiler PoC. The PoC must use exact pinned source blobs and cannot substitute shadow DTOs, stubs, no-op or behavior-reduced methods.

## 9. Validation and actual changes

- The upstream tree contained 3,298 entries and was not truncated. The Badcat tree was inspected as a full, non-truncated tree of 485 entries. Focused source/declaration scans do not constitute a compiler call graph.
- No build, tests, CI, APK, live-source run or device verification was performed for Runtime-1.2.
- No production source, tests, Gradle/workflow, Room/schema/migration, persistence, production caller, UI/Reader, release configuration or main changed.
- Runtime-1.2 acceptance for a minimum cut-set and complete typed-port proof is NOT MET. C compiler PoC: NO-GO. G2 remains BLOCKED/partial; B′ and full Legado compatibility remain unpassed.
