# Runtime-3.0 架构总审查与统一实施方案

- 日期：2026-11-21
- 仓库：`xkszds/badcat-legado-client`
- 分支：`integration/legado-upstream-runtime`
- 起始 HEAD：`85c1fc8fd11b61cf00c2e1b485c10fbb5b786f87`
- 固定上游：`LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- 当前状态：审查进行中；本文件先登记范围与历史证据，后续追加逐文件证据。不是生产实现授权。

## 1. 当前架构主线
坏坏猫原版产品层 + 固定 SHA Legado 真实书源执行核心 + 类型化宿主接口 + 独立验证的数据单一事实源。坏坏猫 UI/Reader 不替换；Legado 只提供真实书源执行能力。宿主边界必须保留上游类型、参数、返回、null/default、异常、线程/协程、取消、回调、状态、事务和资源生命周期。

## 2. 历史证据（保留，不自动沿用）
1. G2d 第一阶段 12 个固定上游源码文件原样编译成功；只覆盖有限规则模型/接口、Gson、常量和工具声明切片，不代表完整 BaseSource/BookSource/Runtime。
2. G2d 第二阶段因 BaseSource/BookSource 的真实依赖跨到 Legado Activity/Dialog/WebView、appDb/Room、CacheManager、CookieStore、SharedJsScope、JsSourceEngine、LoginUiV2 等而停止。禁止通过删方法、no-op、shadow DTO 或复制替身绕过。
3. Runtime-1.x 历史 A/B/C、B′ 五域 PoC、限定 A、Hybrid 记录继续保留，当前不把任何旧数据方案视为已通过。
4. 当前 Badcat 生产路径主要使用 host projection、自定义 Search/Detail/TOC/Content 服务和 Rhino Host bridge；并非完整上游执行链。
5. 历史审计已发现规则字段、JS 上下文/扩展、章节身份、BatchContentContext、缓存、数据 owner 等缺口；具体差异必须按当前源码复核，不能仅凭旧计数下结论。

## 3. 当前模块边界（待逐项核验）
- Badcat Product UI 位于 `app`。
- `integration-contract` 当前为 Kotlin/JVM Android-free 合约/服务实现，不应直接承载 Android Parcelable/Room/appDb 运行时实体。
- `legado-rhino` 提供 Rhino 引擎和包装能力，不等于完整 Legado source runtime。
- `legado-runtime` 当前生产源码较薄，部分上游规则模型由 Gradle 配置引入；模块名不是完整兼容证据。
- `app` 目前承载若干 Runtime factory、JS bridge、Cookie/cache/login 等宿主实现。
- 固定 SHA 上游的 BookSource/BaseSource/Book/BookChapter、JsExtensions、SharedJsScope、JsSourceEngine、CacheManager、CookieStore 与 AppDatabase/Activity/WebView 存在真实耦合；闭包必须用精确 caller、Gradle、资源和生成代码证据界定。

## 4. 目标层次
1. Badcat Product UI：原版页面、导航、阅读器、净化、CSS、交互。
2. Compatibility Facade：集中类型/身份/序列化/错误/生命周期转换。
3. Controlled Legado Runtime：尽可能原样复用固定 SHA 真实源码及行为依赖。
4. Typed Platform Host Ports：Android Context、HTTP、Cookie、登录 UI、WebView/浏览器、缓存、数据库、线程/协程、资源释放。
5. Single-source Data Ownership：每域唯一事实源/写入者，事务、失效、备份、迁移、回滚、重启恢复可验证；数据库选型不预设。

## 5. 阶段门禁
| 阶段 | 内容 | 交付 |
|---|---|---|
| 0 架构总审查 | 上游源码/caller/依赖/Gradle/资源/动态入口/数据 owner | 可追溯清单、调用图、依赖图、未知项、GO/NO-GO |
| 1 Runtime 边界 PoC | 原样上游核心 + 类型化 Host Ports | 可复现编译、架构守卫、真实规则/JS/请求闭环 |
| 2 模型与规则 | BookSource/Book/BookChapter/ReadConfig、AnalyzeRule/AnalyzeUrl | 固定 SHA 字段/序列化/规则差分 |
| 3 JS 与生命周期 | Rhino、JsExtensions overload、BaseSource、jsLib、SharedJsScope | API/ABI/上下文/回调/取消/释放差分 |
| 4 网络与平台 | HTTP、Cookie、登录、WebView/浏览器/验证码、缓存 | 真实语义和生命周期回放 |
| 5 书源业务链 | 搜索/发现/分页/详情/目录/正文/多页/批量 | 多类型书源 E2E 差分回放 |
| 6 数据所有权 PoC | 五域和关联状态单写者、事务、迁移、回滚、恢复 | dry-run、幂等、备份/回滚/重启测试 |
| 7 Badcat 集成 | 接回原版 UI/Reader | 原版 UI 回归、真机阅读和恢复 |
| 8 发布验收 | 契约/架构/回放/真实源/真机/APK/许可证 | exact HEAD 产物和完整报告 |

## 6. 本轮架构审查任务
### 6.1 上游闭包
对固定 SHA 的书源入口逐个清点模型、规则、AnalyzeRule/AnalyzeUrl、BaseSource、JsExtensions、JsSourceEngine、SharedJsScope、网络、登录、Cookie、缓存、WebView、浏览器、资源/文件和异常；记录路径、blob SHA、声明签名、imports、callers、反射/动态入口、资源/生成符号和 Gradle module。分别标注静态候选、编译器解析闭包、运行时动态闭包。

### 6.2 Badcat caller/provider
从 UI → ReadingEngine → service → parser/runtime → network/cache/store 追踪真实生产调用链。每个上游公开 JS 方法/overload 记录签名、Badcat provider、参数/返回类型、异常/线程/回调差异、真实 caller 和证据。不得仅凭同名方法认定兼容。

### 6.3 Typed Host Ports
每个候选接口定义输入输出、可空性、线程/协程/取消/超时、异常、side effects、持久化/事务、生命周期/释放/并发语义和固定上游差分 fixture。no-op、空实现、Any?/Map 万能接口、测试替身不得作为生产实现。

### 6.4 数据事实源
审计 BookSource、Book、BookChapter、Progress、Chapter Cache，以及 Cookie、登录态、源变量、收藏、书签、历史的 owner、读者、写入者、主键/身份、缓存 key、事务、恢复和迁移。此阶段禁止冻结 schema v1、添加 Room、双写或迁移用户数据。

## 7. 现阶段排除项
不改 main；不导入整套 Legado UI/Reader/AppDatabase；不删除上游行为或加空壳；不做 Room/schema v1/双写/迁移/cutover；不修第四类 AdMark；不混入 UI/Reader/PPRule；暂停 Runtime-2.11 嵌套 Shell 探针；不宣称完整兼容或发布就绪。

## 8. 审查证据日志
初始登记：只确认分支/HEAD、读取两份必读清单和最近 CI 终态。尚未完成新的逐文件源码/调用图/编译闭包审查。后续每次追加精确文件路径、blob SHA、caller、依赖分类、测试/命令、未决项及判断，不得把历史证据冒充本轮新核验。

## 9. GO/NO-GO 决策矩阵（待审查）
| 边界 | 当前判断 | 证据状态 |
|---|---|---|
| 排除 Legado 产品 UI/Reader | 目标硬约束 | 需证明闭包可行 |
| 原样上游书源核心 | 目标硬约束 | 完整闭包未证明 |
| 类型化 Host Ports | 目标设计 | 接口集合/语义待审计 |
| 单一数据事实源 | 目标硬约束 | 方案未选择，B′未通过 |
| 当前生产路径完整兼容 | NO-GO | 现有证据显示 host projection/定制服务为主 |
| 开始大规模生产移植 | NO-GO（当前） | 架构审查尚未完成 |


## 10. Runtime-3.0 evidence checkpoint — pinned upstream roots and actual host boundary

Inspection ref: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. The following are exact Git tree blob SHAs at that ref; they are source identity evidence, not a claim that the dependency closure is complete.

### 10.1 Pinned source roots and import evidence

| Root | Pinned blob SHA | Direct dependency evidence |
|---|---|---|
| `data/entities/BaseSource.kt` | `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa` | Extends `JsExtensions`; imports Android `JavascriptInterface`, `CacheManager`, `CookieStore`, `SharedJsScope`, `JsSourceEngine`, LoginUiV2, AppConfig, crypto, source-scope helpers and script bindings. Its methods include login UI/action, login state/header/info, source variables, explore refresh, JS library refresh and `evalJS`. |
| `data/entities/BookSource.kt` | `7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c` | Android Parcelable + Room Entity/Index/PrimaryKey/TypeConverters; owns all six rule-model fields and Gson converter methods; implements source key/tag/login JS and content-batch helpers. |
| `model/analyzeRule/AnalyzeRule.kt` | `7591a026a072ac8a6fe4902add0795812fe0a0a6` | Imports Android TextUtils/Keep; BaseBook/BaseSource/Book/BookChapter/BookSource/RssArticle; CacheManager, JsExtensions, BackstageWebView, CookieStore, BatchContentContext, SharedJsScope, network/Gson and HtmlUnit Rhino types. Has typed chapter, next-chapter, batch-context and coroutine-context setters. |
| `model/analyzeRule/AnalyzeUrl.kt` | `d898c88dfd844d73f06d9396d0b46f39939f7e6c` | Imports Android Base64/SuppressLint, Media3, Glide, AppConfig, BackstageWebView, CookieManager/CookieStore/Cronet, request/response helpers, concurrent limiter, SharedJsScope, OkHttp and Rhino. Supports suspend and blocking request/response/byte/input-stream/upload paths and WebView options. |
| `help/JsExtensions.kt` | `f6808edd2a0d312e8bd13649935dea444cb82581` | Imports AppCompatActivity, BackstageWebView, CookieStore, SourceHelp/SourceVerificationHelp, BookHelp, AnalyzeUrl, BottomWebViewDialog, online-import/open-URL activities, archive/file/config/theme helpers and Android app context. The public interface includes source navigation, batch/cache, refresh, WebView, browser, verification, HTTP, file/archive, encoding, font, TTF and reader/theme configuration methods. |
| `model/SharedJsScope.kt` | `dab7770d746c851c06ad05c08a35a2866a6100b3` | Uses app context, ACache/files, OkHttp, LruCache, Rhino ScriptBindings and shared global state; lifecycle includes get/remove/cache/create/evaluate. |
| `model/jsSource/JsSourceEngine.kt` | `44452025f81d8751afee92d3ee1f2d52f9e55905` | Uses BaseSource/BookSource, CacheManager, JsExtensions, CookieStore, source shared-scope helpers, BatchContentContext and Rhino; builds per-source scope and calls named JS functions. |
| `help/CacheManager.kt` | `9575697d8ed60c0864aa3fd52d548ba059054205` | Directly imports `appDb` and Room `Cache` entity in addition to in-memory/disk caches; generic cache and chapter-body cache are distinct domains. |
| `help/http/CookieStore.kt` | `09720d79f7009db052c080b54fc5fa2805257b64` | Directly imports `appDb` and Room `Cookie` entity; implements CookieManagerInterface plus web-cookie, merge, key lookup and clear operations. |
| `help/source/SourceHelp.kt` | `09145629dfb8d92772cf5fa339fda5ed33fb555d` | Directly imports `appDb`, BookSource/RSS source entities, source config/cache, ReadBook/ReadManga/VideoPlay, VideoPlayService/VideoPlayerActivity and app context. |
| `help/book/BookHelp.kt` | `78e9e82b67145b835678c7aa1111eaf1631b77d2` | Directly imports `appDb`, Book/BookChapter/BookSource, AppConfig, AnalyzeUrl, local/EPUB book implementations, Android document/file APIs, image/file/network helpers and coroutine/file synchronization. Contains content save fencing, content/cache/image IO and chapter identity matching. |
| `data/AppDatabase.kt` | `(tree entry verified; see pinned source tree)` | Room database owns Book, BookChapter, BookSource, Cookie, Cache and additional product entities/DAOs. Importing DAO-backed helpers unchanged would introduce the upstream database authority and broad product schema. |

### 10.2 Upstream Gradle/module boundary

At the pinned upstream revision, `settings.gradle` includes only `:app`, `:modules:book`, and `:modules:rhino`. `:modules:book` is a small Android library (minSdk 21, Java/Kotlin 21) with annotation dependency. `:modules:rhino` is an Android library exposing HtmlUnit core JS and depending on coroutines, OkHttp and AndroidX collection. The actual source runtime roots above live under the upstream `:app` source set, not in either reusable module. Upstream `:app` configures Room/KSP, AndroidX WebKit, Media3, Glide, Cronet, app context, archive/crypto and a broad set of app dependencies. This disproves the assumption that adding the two reusable modules alone imports the source runtime.

### 10.3 Badcat production caller evidence at audited HEAD

At Badcat HEAD `b1d16355543f47850d1a8a7cab9aa869b95caba0`:

- `app/.../runtime/BadcatReadingEngineFactory.kt` constructs `SourceSearchService`, `SourceBookDetailService`, `SourceTocService`, and `SourceChapterContentService`, passing a `SourceScriptRuntime` into these custom services.
- `app/.../SourceSearchActivity.kt` invokes `engine.searchPage(...)` and `engine.explore(...)`; result selection launches `SourceBookActivity` with projected source/book fields.
- `app/.../SourceBookActivity.kt` invokes the custom engine's chapter-content entry point for typed chapter and legacy URL-resolved paths.
- `integration-contract/.../SourceReadingEngine.kt` delegates to the four host services and has a legacy URL overload which synthesizes `BookChapter(index = 0, title = "", url = chapterUrl)`. Thus the live Badcat path is still host service/projection based, not upstream `BaseSource → AnalyzeRule/AnalyzeUrl → JsSourceEngine`.

### 10.4 Architectural consequence and status

A clean extraction cannot be achieved by merely adding `:modules:book` and `:modules:rhino`: the source roots reside in `:app` and statically call app-level services. Three dependency classes are now evidenced:

1. **Product/UI edge:** `JsExtensions` directly references browser/WebView/dialog/activity and source verification/import UI; `SourceHelp` references video player/service; these need behavior-preserving typed ports or Badcat-owned UI adapters, not omission/no-op.
2. **Data authority edge:** `CacheManager`, `CookieStore`, `SourceHelp`, and `BookHelp` reach `appDb`/Room entities. A controlled extraction must replace static DAO ownership with typed ports while preserving transactions, identity, cache and cookie semantics; it must not ship a second upstream database.
3. **Runtime state edge:** `BaseSource`, `AnalyzeRule`, `AnalyzeUrl`, `SharedJsScope`, and `JsSourceEngine` share source object identity, JS scopes, coroutine context, batch context, cancellation, login state and request state. A string-map facade or per-call isolated JS context is not behavior-equivalent.

**Interim decision:** GO only to finish static callsite/resource/Gradle and typed-port inventory under Runtime-3.0. NO-GO for broad source copy, production wiring, schema/Room work, or a compiler PoC until the exact patchable cut-set and required semantic ports are enumerated. The evidence makes a *controlled upstream-source fork with narrow injection seams* the leading candidate, but does not yet prove it buildable or complete. Static candidate graph, compiler-resolved closure and runtime dynamic closure remain separate and incomplete.

**Validation:** read-only pinned source/tree inspection and Badcat source inspection only. No build, tests, CI, runtime, device, APK or live-source validation was performed in this checkpoint. No production files changed.


## 11. Runtime-3.0 evidence checkpoint: static callers, dynamic entry points and host cut-set (2026-11-22)

Audit base: Badcat \`integration/legado-upstream-runtime\` at \`dc4950eebe93344efa1a5de206ce1c90b48c31f1\`; upstream \`LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a\`. Read-only source audit; no compiler-resolved or runtime closure is claimed.

### 11.1 Static caller map

- \`model/webBook/WebBook.kt\` is the book-source business entry. It dispatches JS sources to \`JsSourceBook\` and rule sources to \`AnalyzeRule\`/\`AnalyzeUrl\`. The JS route covers search, explore, detail, chapters, content and batch content.
- \`JsSourceBook\` calls named functions through \`JsSourceEngine\`; \`JsSourceReview\` is a separate summary/detail/replies path called by \`ReviewController\`, \`ReadBookActivity\` and \`ReviewDetailDialog\`.
- \`BaseSource.evalJS\` injects the same source object as \`source\` and \`sourceApi\`, together with \`java\`, \`baseUrl\`, \`cookie\` and \`cache\`. Login and rule execution use the same source identity.
- \`BatchContentContext\` is created by \`JsSourceBook.getContentBatchAwait\`, installed in \`JsSourceEngine.callContentBatch\`, read by \`JsExtensions.cacheContent\`, and persisted through \`BookHelp.saveContent\`.
- \`BackstageWebView\` is reached from \`JsExtensions\`, \`AnalyzeUrl\`/\`AnalyzeRule\` and \`CodeEditViewModel\`. \`WebViewPool\` is also used by browser, login, RSS, video, book-info and bottom-sheet UI surfaces.
- Login UI v2 flows from \`BaseSource.evalLoginUiV2/evalLoginActionV2\` into \`SourceLoginV2Delegate\`. Browser/captcha verification flows through \`SourceVerificationHelp\`; JS navigation is wrapped by \`SourceNavigationBridge\`.
- \`CacheManager\`, \`CookieStore\`, \`SourceHelp\`, \`BookHelp\` and \`BackstageWebView\` reach Room/appDb or Android framework state. These are concrete data/UI ownership edges, not abstract parser dependencies.

### 11.2 Dynamic and reflective entry points

1. \`JsSourceConfig\` uses Rhino AST \`Parser\` nodes (\`FunctionNode\`, \`VariableInitializer\`) to discover top-level source functions and validate login UI v2, review functions and \`getContentBatch\`/maxBatchSize pairing. Static grep alone cannot close this path.
2. \`JsSourceEngine.callFunction(name,args)\` resolves a function by runtime string name through \`ScriptableObject.getProperty\`, compiles a call expression and evaluates it in the source scope. Optional function absence has a distinct result.
3. \`NativeBaseSource\` overrides Rhino \`has/get/put\` for source properties. \`RhinoWrapFactory\` chooses wrappers by runtime/static type and \`RhinoClassShutter\` restricts script-visible classes. Replacing the source with a Map snapshot or creating a fresh object per call is not equivalent.
4. \`WebJsExtensions.request(funName,jsParam,id)\` dispatches 16 async names: \`run\`, \`ajaxAwait\`, \`connectAwait\`, \`getAwait\`, \`headAwait\`, \`postAwait\`, \`webViewAwait\`, \`webViewGetSourceAwait\`, \`decryptStrAwait\`, \`encryptBase64Await\`, \`encryptHexAwait\`, \`createSignHexAwait\`, \`downloadFileAwait\`, \`readTxtFileAwait\`, \`importScriptAwait\`, \`getStringAwait\`. It returns Promise resolution/rejection by callback ID and memory cache.
5. \`BackstageWebView\` resolves the source by tag from \`appDb.bookSourceDao\`, injects source/java/cache interfaces and navigation callbacks. \`WebJsExtensions\` uses generated interface names; \`WebViewPool.release\` removes them, resets the WebView or destroys it. Timeout/cancellation affects WebView lifetime.
6. \`SourceNavigationBridge\` re-enters the originating coroutine context from the WebView thread and applies navigation suppression to URL/video callbacks.
7. R8 rules keep \`JsExtensions\` descendants, entities, \`CookieStore\`, \`CacheManager\`, \`StrResponse\`, annotation and signature metadata. Runtime-injected \`@JavascriptInterface\` methods and dynamically looked-up members still require bytecode/shrinker validation.
8. The inspected reflection/security paths include Rhino class visibility/wrapping and \`FileDocExtensions\`' reflective \`TreeDocumentFile\` construction. \`AnalyzeUrl.UrlOption.getMethod()\` is a URL-option accessor, not Java reflection. No general reflective source-function router was established; generated/reflection closure remains unproven.
9. \`SharedJsScope\` loads \`assets/scripts/cryptojs.min.js\` and source \`jsLib\`; \`assets/js_source_template.js\` documents the JS source API; WebView bootstrap is assembled in code. The pinned inventory has 774 resource files (1 manifest, 648 res resources, 125 assets), but does not prove all are required by the runtime cut.
10. Pinned \`settings.gradle\` includes only \`:app\`, \`:modules:book\`, \`:modules:rhino\`. Runtime roots remain in \`:app\`; app build owns Room/KSP, WebKit, Media3, Glide/KSP, Cronet, OkHttp, archive/crypto and broad product dependencies. Importing only book/Rhino modules does not import the source engine.

### 11.3 CI-generated static inventory and ABI evidence

- Dependency candidate artifact ID \`14719910553\`, ZIP SHA-256 \`4809e1c8d1e7f78b4e62475102fffd4473dacbfcf5b9e7fbba1a8fbd24f820b5\`: 1,186 indexed source files, 16 resolved seeds, 1,085 reachable candidates, 152,175 lexical/import edges, 25,950 ambiguous same-package references and 906 generated/dependency unresolved imports. The artifact explicitly says this is a lexical candidate graph, not compiler-complete closure.
- JS source declaration artifact ID \`14719855857\`, ZIP SHA-256 \`caf29d6cebfc4c5c340922e0a7d56e47ad41b115492fa30e9a8d6d35eaa7baa8\`: 148 upstream declarations/104 names vs 150 host main-bridge declarations/110 names; 17 missing names, 22 overload-count differences and 38 same-name signature differences. It is source declaration inventory, not JVM descriptors. WebJsExtensions and SourceLoginJsExtensions are separate scopes.
- Concrete shape differences include upstream \`getSource(): BaseSource?\` vs host \`getSource(): Map<String, Any?>\`, and upstream \`getBatchContext(): BatchContentContext?\` vs host \`Any?\`. The method inventory does not establish runtime parameter coercion, object identity, exceptions, cancellation or side effects.

### 11.4 Candidate typed Host Port cut-set (not implemented)

| Port | Must preserve |
|---|---|
| Source repository/identity | Stable BaseSource/BookSource identity, source key/tag, import/enable/delete lifecycle; no Map substitute. |
| Rhino runtime/shared scope | Context/scope identity, sourceApi alias, NativeBaseSource property semantics, jsLib loading/removal, class shutter, cancellation and exceptions. |
| AnalyzeRule/AnalyzeUrl | Real rule context, URL resolution, embedded JS, request options, response types and rule defaults. Prefer authentic upstream engine; ports isolate platform calls rather than replace rules. |
| HTTP | Method/body/headers, source identity, redirect policy per entry point, timeout, rate limit, cancellation, response metadata/body and errors. |
| Cookie/session/login | Domain normalization, persisted/session cookies, WebView↔HTTP sync, merge/replace/remove, login header scoping, encrypted login info and source variables. |
| Generic cache/file/archive | Memory/disk split, LRU, TTL, ACache file semantics, jsLib invalidation, path confinement, archive/font behavior and size limits. Keep chapter-body storage separate. |
| Book/chapter/batch | Real Book/BookChapter identity, index and URL ambiguity, save token/fence, replacement, batch save, missing-chapter retry and cancellation. |
| WebView/browser | Pool lifecycle, injected-interface add/remove, navigation/sniffer callbacks, Cookie sync, timeout/cancel, Promise callback IDs and cleanup. |
| Login/verification/navigation UI | Login UI v2 render/action schema, errors/countdowns/close, browser/captcha handoff, refetch, coroutine re-entry and navigation suppression. Badcat supplies its own UI. |
| Product event/config | Refresh book/TOC/content events, read/theme config projection, open URL/video callbacks; retain Badcat UI/Reader. |
| Concurrency/context | Per-source rate/concurrency keys, lock/singleFlight/tick, coroutine context, cancellation and no cross-source scope leakage. |

### 11.5 Decision

- **GO:** continue the registered read-only Runtime-3.0 architecture audit and close method-level caller/provider mapping and explicit patch seams.
- **NO-GO:** broad upstream source import, production wiring, compiler PoC, Room/schema/migration, caller cutover or release claim. Static candidate edges and candidate ports do not prove a compiler boundary.
- Controlled upstream-source fork with narrow typed injection seams remains the viable direction, but buildability is not proven. No behavior may be dropped merely to make a cut compile.

### 11.6 Pinned upstream blob references

All paths below are pinned to \`330e606c4d57e08adc056ff8ab7a4132c784da9a\`:

- \`BaseSource.kt\` \`9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa\`; \`BookSource.kt\` \`7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c\`.
- \`AnalyzeRule.kt\` \`7591a026a072ac8a6fe4902add0795812fe0a0a6\`; \`AnalyzeUrl.kt\` \`d898c88dfd844d73f06d9396d0b46f39939f7e6c\`; \`JsExtensions.kt\` \`f6808edd2a0d312e8bd13649935dea444cb82581\`.
- \`SharedJsScope.kt\` \`dab7770d746c851c06ad05c08a35a2866a6100b3\`; \`JsSourceEngine.kt\` \`44452025f81d8751afee92d3ee1f2d52f9e55905\`; \`JsSourceBook.kt\` \`66d9878bed8671fe130d9c9c507608d89d393cf5\`; \`JsSourceReview.kt\` \`44ce6b029debc5d73df914627c18a820c598d92f\`; \`JsSourceConfig.kt\` \`871165021c2192b7d79ca2768bfdc6b4cfd35740\`.
- \`WebJsExtensions.kt\` \`dd6532396b8f02e37376885aa2c6c1e430af1ad9\`; \`WebViewPool.kt\` \`ea31fac447e2176c68f590413ff7e272730019d8\`; \`BackstageWebView.kt\` \`285f229ff2f9f00e7588b221a8543a2879bafe86\`.
- \`SourceNavigationBridge.kt\` \`82b7adeb3e105a3553ba6e5b4391c4f91c2e8c9f\`; \`SourceVerificationHelp.kt\` \`d15769767513ca23bdbcc633709a348ae09c5b08\`; \`LoginUiV2.kt\` \`fd4fca1baa694a821400a46d08ff20d6607f4b96\`; \`BatchContentContext.kt\` \`68cad877f048b171e4f0f6cf22b1e0dd1e6481f9\`.
- \`CacheManager.kt\` \`9575697d8ed60c0864aa3fd52d548ba059054205\`; \`CookieStore.kt\` \`09720d79f7009db052c080b54fc5fa2805257b64\`; \`SourceHelp.kt\` \`09145629dfb8d92772cf5fa339fda5ed33fb555d\`; \`BookHelp.kt\` \`78e9e82b67145b835678c7aa1111eaf1631b77d2\`; \`AppDatabase.kt\` \`d3d516c7b3e73ef4ab3beb373e8e8ec3d03c204c\`.
- \`NativeBaseSource.kt\` \`8313dedf737fe42cb4381535f6e74760c4f31786\`; \`RhinoWrapFactory.kt\` \`4a3d8c30329664e725cb64ae89a88a5dfe4ebc6a\`; \`RhinoClassShutter.kt\` \`e9033fa47753038751191926a4f898bf5680c05b\`.
- \`SourceLoginV2Delegate.kt\` \`2b5fb5b0e7efb5b1df36ff906a8ce17b672ac41c\`; \`WebBook.kt\` \`d7a924c2d687bc99d6e25099cefacc9d920e1386\`; \`ReviewController.kt\` \`0e5be8d8ec83094a9fdf4ae5bebf9370f64fdc7e\`.
- \`BookInfoViewModel.kt\` \`eb0b5853a7e880eb62e33ea1cca4210262adbbb0\`; \`ReadBookActivity.kt\` \`29d3c6dd3cf75178569416e5e97ec1005d1b1c1b\`; \`WebViewActivity.kt\` \`fec705d67831e07f6e25a94d94ede9b000ca088f\`; \`BottomWebViewDialog.kt\` \`5d354092597502bafe26424b3c39cdca667d33f6\`.
- \`FileDocExtensions.kt\` \`b0bc9f2765bc98cf4d39aa8da68c49623497b4fd\`; \`app/build.gradle\` \`eb26ab098e63401a3c506fbd21ca8fdc95616aa7\`; \`settings.gradle\` \`beb9639fbb751b1736ad5baf5afc509c6718c4ca\`; \`proguard-rules.pro\` \`2993b30d101be1bcf2e80545a6f3e89139004406\`; \`AndroidManifest.xml\` \`38246c5895d276ecc9ebc6dc4451a609c7e5ca07\`; \`js_source_template.js\` \`25ae7673c9f66c2b3959aa370f6c98a4efe3a1e4\`.


## 12. Runtime-3.0 evidence checkpoint: BatchContentContext full caller and lifecycle audit (2026-11-22)

Audit base: Badcat `integration/legado-upstream-runtime` at `833209165cb860557fad23944ab50e04039acb93`; upstream `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. Read-only source review. This checkpoint closes the main static caller map for batch chapter content; it does not claim compiler closure or runtime parity.

### 12.1 Complete statically confirmed execution paths

The upstream has two distinct batch entry paths which converge on the same `BatchContentContext` and `java.cacheContent` contract:

1. **JavaScript source:** `CacheBook` → `WebBook.getContentBatchAwait` → `JsSourceBook.getContentBatchAwait` → `JsSourceEngine.callContentBatch` → `JsExtensions.cacheContent` → `BatchContentContext.saveContent` → `BookHelp.saveContent`.
2. **Rule source:** `CacheBook` → `WebBook.getContentBatchAwait` → `BookContent.analyzeContentBatch` → `AnalyzeRule.setBatchContext` → `JsExtensions.cacheContent` → the same `BatchContentContext.saveContent` → `BookHelp.saveContent`.

`WebBook` chooses the JS route only for JS sources; ordinary rule sources use `BookContent.analyzeContentBatch`. Both paths install the context before script execution and close it in `finally`. A runtime implementation that only wires the JS path or only wires the CSS/rule path is incomplete.

### 12.2 Context identity and save semantics

`BatchContentContext` captures the actual `BookSource`, `Book`, ordered `BookChapter` list, originating `CoroutineContext`, and a per-chapter `ContentSaveToken` captured before the batch starts.

- Chapter identity is `BookChapter.index`, not URL. A chapter object is resolved against the current batch by index. A URL is accepted only when it uniquely identifies one chapter; raw, absolute and normalized URL aliases are indexed. Numeric identifiers are rejected to prevent confusing an array position with a chapter index.
- Duplicate URLs are valid in a TOC. Passing a URL shared by multiple chapters is an ambiguity error even if one twin has already been saved. Passing the actual chapter object remains unambiguous.
- The context applies the source's real `replaceRegex` through `AnalyzeRule`, with the chapter and its absolute URL as context; online text formatting is also applied before persistence.
- Save and close operations are synchronized. `saveContent` checks cancellation, rejects a closed context, resolves the chapter, rejects blank post-replacement content, requires the pre-captured save token, checks cancellation again, and only marks the chapter saved after the fenced save succeeds.
- `close()` prevents late asynchronous JS callbacks from publishing after the batch ends. Missing chapters remain ordered and are returned to the caller for ordinary single-chapter fallback.
- `BookHelp.saveContent` validates book URL, chapter index and cache-folder identity, then uses `ContentSaveFence.writeIfCurrent` with the captured version before publishing the chapter file. This prevents an old in-flight batch response from overwriting a newer authoritative edit/cache generation. A successful write posts the product `SAVE_CONTENT` event; optional chapter metadata update reaches the Room DAO.
- `CacheBook.runBatch` owns download tickets and fallback: it snapshots content tokens, preserves chapters saved before a script exception, rechecks token freshness, sends missing/blank chapters back to the single-chapter queue, propagates cancellation, and finishes every ticket in `finally`. This coordination is part of observable behavior, not an optional optimization.

### 12.3 Existing upstream test contract

Pinned upstream tests establish more than a happy path:

- `BatchContentContextTest`: duplicate-URL ambiguity, relative/absolute aliases, chapter-object identity, rejection of numeric/null/unknown identifiers, repeated save for a unique URL, and ordered missing-chapter calculation.
- `BatchContentDownloadTest`: raw/`<js>`/`@js:` wrappers; both rule and JS source routes; replacement and blank-output rejection; stale batch response cannot overwrite a user edit; reader/read-aloud joining a batch; cancellation of one waiter does not cancel the producer; restoring queued manual work after cancellation; partial script failure retaining successful writes and retrying only missing chapters; cancellation before lazy task startup; late callbacks rejected after close; and content/image cache coordination.

These pinned tests are a minimum porting/contract suite. Their existence in upstream is not evidence that Badcat currently passes them.

### 12.4 Badcat comparison at audited HEAD

- `RhinoSafeSourceJavaExtensions.getBatchContext()` returns `null`.
- `RhinoSafeSourceJavaExtensions.cacheContent()` throws an explicit `IllegalStateException`; it does not silently drop data.
- `CssChapterContentParser` explicitly rejects `contentBatch` (along with other unsupported rule fields). `SourceChapterContentService` has no batch-context installation/persistence route.
- The current Badcat source path is a custom search/detail/TOC/content service pipeline, not upstream `CacheBook → WebBook → JsSourceBook/BookContent`.
- Therefore neither upstream batch route is integrated. This is a known compatibility blocker, not an isolated missing helper method.

### 12.5 Required typed port cut-set for a future implementation

A behavior-preserving cut must provide all of the following; this is a contract inventory, not an implementation authorization:

| Required seam | Semantics that must survive |
|---|---|
| Batch execution scope | Install one context for the exact JS/rule invocation; expose it to `java.cacheContent`; isolate per source/batch; close in `finally`; reject late callbacks. |
| Book/chapter identity repository | Preserve Book/BookChapter identity and stable chapter index; never identify a chapter solely by a potentially duplicated URL. |
| Content replacement | Execute upstream `AnalyzeRule` replacement semantics with chapter/base URL and formatting context, not a simplified string replace. |
| Content cache publisher | Preserve file naming, book-folder identity, cache invalidation/version token, compare-and-save fencing, and reader-visible save event. |
| Download coordinator | Preserve batch tickets/claims, partial success, missing-chapter fallback, single-chapter retry, cancellation and waiter/producer ownership. |
| Metadata/event host | Preserve optional chapter metadata persistence and `SAVE_CONTENT` notification without introducing a second database authority. |

A generic `Any?` context, map-based chapter identity, no-op save, URL-only matching, or writing chapter text without the save fence would not satisfy this contract.

### 12.6 Pinned source and test blob references

All upstream entries are pinned to `330e606c4d57e08adc056ff8ab7a4132c784da9a`:

- `BatchContentContext.kt`: `68cad877f048b171e4f0f6cf22b1e0dd1e6481f9`.
- `JsSourceBook.kt`: `66d9878bed8671fe130d9c9c507608d89d393cf5`.
- `JsSourceEngine.kt`: `44452025f81d8751afee92d3ee1f2d52f9e55905`.
- `JsExtensions.kt`: `f6808edd2a0d312e8bd13649935dea444cb82581`.
- `BookHelp.kt`: `78e9e82b67145b835678c7aa1111eaf1631b77d2`.
- `BookContent.kt`: `cbc353920315db9810f57aec935faeaca5310a13`.
- `AnalyzeRule.kt`: `7591a026a072ac8a6fe4902add0795812fe0a0a6`.
- `BaseSource.kt`: `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa`.
- `WebBook.kt`: `d7a924c2d687bc99d6e25099cefacc9d920e1386`.
- `CacheBook.kt`: `f1c24dc905664b592a04daccc4e09ae6e8578409`.
- `BatchContentContextTest.kt`: `67207fecb4a96e8aae0a1bd7083d61cccbf657a9`.
- `BatchContentDownloadTest.kt`: `81e4debe9194d3a9c6590be29c4fdcc5565349f7`.

Badcat inspected blobs at the audit HEAD:

- `RhinoSafeSourceJavaExtensions.kt`: `6a00e649895c4c1d223b768c6b288632ec033de9`.
- `RhinoSourceScriptRuntime.kt`: `f1d8f0225a434e79fbd37d0d8c1ec132f5c9541f`.
- `CssChapterContentParser.kt` and `SourceChapterContentService.kt` were inspected at the same Badcat HEAD; the tree inventory is the source of truth for their exact blob IDs.

### 12.7 Decision and validation

- **GO:** static batch-context caller/lifecycle inventory is sufficiently mapped to inform a later typed-port design.
- **NO-GO:** no production batch wiring, broad upstream import, persistence redesign, schema/Room work, compiler PoC, caller cutover or complete compatibility claim.
- **Read-only validation only:** pinned upstream and Badcat source inspection plus GitHub code-search callsite cross-check. No build, unit/instrumentation test, CI, emulator, device, APK or live-source test was run.
- No production code, tests, Gradle, workflow, database, UI/Reader, AdMark, PPRule or `main` changed.


## 13. Runtime-3.0 evidence checkpoint — BaseSource identity, shared scope and lifecycle cut-set (2026-11-22)

### 13.1 Pinned upstream object and wrapper contract

All upstream evidence below is pinned to `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.

- `BookSource` is a Room entity whose primary key and `BaseSource.getKey()` are `bookSourceUrl`; equality/hash identity also use that URL. It is not an arbitrary catalog ID or a display name.
- `App.initRhino()` registers `NativeBaseSource.factory` for `BookSource`, `RssSource`, and `HttpTTS`; rule objects, `BookChapter`, and `Book.ReadConfig` use read-only wrappers. This registration is part of the source-object ABI, not optional UI decoration.
- `NativeBaseSource` changes property reflection: JavaBean-style `setX` methods are hidden as callable members when a matching property exists, and writes are constrained to the supported `variable` property. A generic Rhino Java wrapper or Map cannot be assumed equivalent.
- Binding identity differs by invocation entry:
  - `BaseSource.evalJS()`: `java`, `source`, and `sourceApi` are the same real BaseSource instance.
  - `JsSourceEngine`: `java` is the invocation engine; `source` and `sourceApi` are the same real BookSource instance.
  - `AnalyzeRule`: `java` is the AnalyzeRule instance and `source` is the real BaseSource; no separate `sourceApi` alias is installed.
  - `AnalyzeUrl`: `java` is the AnalyzeUrl instance and `source` is the real BaseSource; request bindings include typed page/key/result/infoMap context.
- `JsSourceEngine.buildScope()` creates invocation bindings, chains to the shared jsLib/global state and evaluates the source `mainJs` before invoking the named top-level function. This occurs per engine invocation; the shared library/global scope is a separate layer.

### 13.2 Shared scope and identity rules

- `SharedJsScope.getScope(jsLib)` keys the reusable library scope by MD5 of the complete jsLib text. The cache is an LRU of weak references (capacity 16), with per-key creation synchronization; jsLib is evaluated into the shared scope and extensions are restricted afterward.
- Per-source shared global state is separately keyed by `MD5(jsLib):sourceClassName:MD5(source.getKey())`. The source class/type is part of identity; using only a source URL or DTO id is incomplete.
- CryptoJS fallback scopes use owner object identity plus current Thread, which is distinct from the jsLib shared-scope key.
- `SharedJsScope.remove(jsLib)` removes the cached shared library scope, remote jsLib cache entries referenced by the library, and shared-global-state entries under that jsLib hash prefix. This can affect multiple sources sharing the same library.
- `AnalyzeRule`, `AnalyzeUrl`, `BaseSource.evalJS`, and `JsSourceEngine` have different invocation binding and scope construction paths. A single generic “source scope” does not preserve these contracts.

### 13.3 Upstream source lifecycle callers

- `JsSourceUpsert.save()` preserves user-managed state, compares old/new source identity and jsLib, invalidates the old jsLib scope when the library changes, and distinguishes same-key replacement from key migration. A key migration deletes the old source through `SourceHelp`; same-key replacement deletes/reinserts the Room row and updates source config.
- `BookSourceEditViewModel.save()` follows corresponding edit semantics; changing jsLib invalidates the old shared library; changing the source URL deletes the old source; same-key edits remove source config before reinsertion.
- `SourceHelp.deleteBookSourceInternal()` clears shared global state for the concrete source class/key, deletes the BookSource row, and deletes persistent source variables inside the surrounding Room transaction. RSS deletion also removes associated RSS articles.
- `SharedJsScope.remove()` is library-wide invalidation, while source deletion is source-class/key invalidation. They are not interchangeable.
- Login data is another independent lifecycle domain: `getLoginInfoMap()` reads saved info first; Login UI v2 returns an empty default map; v1 may evaluate inline login UI JS with `result`, `book`, and `chapter` bindings, filters button rows, derives defaults, and persists them. Login info, login Header, CookieStore, source variables and JS shared globals must not be indiscriminately cleared together.

### 13.4 Badcat delta at audited HEAD

- `LegadoBookSourceCodec` maps the imported source identity into the Badcat `BookSource.id` / `baseUrl` projection, but the production catalog stores JSON records in `AtomicFile`, not upstream Room `BookSource` entities.
- `FileBookSourceCatalog.importJson()` replaces records with matching source IDs; `remove()` removes a record. Neither path notifies `RhinoSourceScriptRuntime`.
- `SourceRuntime.SourceScriptRuntime` has no source-lifecycle invalidation method. `RhinoSourceScriptRuntime.clearSourceState()` exists only on the concrete class; no production caller was found in the audited tree.
- The concrete clear method serializes against evaluation using the per-source Mutex, then clears Rhino scopes/global state, bridge instances, KV data, file cache, physical cache, login session and Cookie state. This is much broader than upstream source deletion or jsLib invalidation and must not be wired blindly to every import/replace/delete event.
- Current Badcat Rhino bindings still use a login helper as `source`, omit upstream `sourceApi`, and expose a Map-like source descriptor from `java.getSource()`; they do not preserve NativeBaseSource object identity/property behavior.
- Badcat keeps one persistent VarScope per source ID, serializes same-source evaluations with a Mutex, and evaluates `mainJs` once per source version. Its tests explicitly assert this policy (`initializesMainJsOnceBeforeSourceRules`, `serializesConcurrentEvaluationForOneSource`, `clearsSourceGlobalsUnderTheEvaluationLock`). Those tests document Badcat behavior; they do not establish upstream equivalence. Upstream JS engine constructs per-invocation scope and evaluates mainJs on each invocation. The concurrency behavior therefore needs a differential contract, not an assumption that the Mutex is equivalent.

### 13.5 Required host patch seams (contract candidates, not implemented)

| Seam | Required behavior |
|---|---|
| Source identity/repository | Supply the correct source class and real source object; preserve `bookSourceUrl`/getKey identity and replacement/key-migration semantics. |
| Rhino wrapper registry | Register equivalent wrappers for mutable source objects and read-only rule/chapter/config objects; preserve property/method visibility and setter behavior. |
| Invocation scope factory | Separate BaseSource, JsSourceEngine, AnalyzeRule and AnalyzeUrl binding contracts; retain typed book/chapter/page/result/context objects and aliases. |
| Shared scope provider | Preserve jsLib-hash sharing, source-class/key global-state identity, CryptoJS owner/thread fallback, creation synchronization and invalidation fan-out. |
| Lifecycle coordinator | Distinguish source deletion, same-key replacement, key migration, jsLib change and ordinary metadata edit; perform only the matching invalidation and serialize it against in-flight evaluation. |
| Login/session ports | Preserve login UI v1/v2 default initialization, encryption/storage, Header, Cookie and source-variable boundaries. |
| Execution coordination | Differentially verify per-source concurrency, callback lifetime, cancellation, timeout and cleanup; do not equate a host Mutex with upstream semantics without tests. |

### 13.6 Decision and validation

- **GO:** source-object/shared-scope/lifecycle cut-set is now explicit enough to guide the next static API/provider audit.
- **NO-GO:** no production wiring, broad upstream import, compiler PoC, database/schema migration, caller cutover or complete compatibility claim.
- **Read-only evidence:** pinned upstream and Badcat source inspection, code-search caller cross-check, and source-test inventory. No tests/build/CI/device/APK/live-source validation was run in this audit step.
- No production code, tests, Gradle, workflow, database, UI/Reader, AdMark, PPRule or `main` changed.

Pinned inspected blobs:

- Upstream `BaseSource.kt`: `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa`; `BookSource.kt`: `7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c`.
- Upstream `NativeBaseSource.kt`: `8313dedf737fe42cb4381535f6e74760c4f31786`; `BaseSourceExtensions.kt`: `4b70e9848dde0c3adad44f8cc5147a2abe128cb1`.
- Upstream `SharedJsScope.kt`: `dab7770d746c851c06ad05c08a35a2866a6100b3`; `JsSourceEngine.kt`: `44452025f81d8751afee92d3ee1f2d52f9e55905`.
- Upstream `AnalyzeRule.kt`: `7591a026a072ac8a6fe4902add0795812fe0a0a6`; `AnalyzeUrl.kt`: `d898c88dfd844d73f06d9396d0b46f39939f7e6c`.
- Upstream `JsSourceUpsert.kt`: `cc4120734510dfcf2c5b45b5fbc5e0f8e4c26a9e`; `SourceHelp.kt`: `09145629dfb8d92772cf5fa339fda5ed33fb555d`; `App.kt`: `71b83735b9a41fe0a459fee3a2175fa7518215b8`.
- Upstream tests: `LoginInfoMapInitializationTest.kt` `26178c7f48865a949083d9870e5d4b252df20676`; `JsSourceEngineTest.kt` `165d2c1115f270b3690defc9af5063f67d7e9f1e`.
- Badcat `FileBookSourceCatalog.kt`: `dedcb159e0cc08d8e8488076bf3e9d2441ab17d1`; `RhinoSourceScriptRuntime.kt`: `f1d8f0225a434e79fbd37d0d8c1ec132f5c9541f`; `SourceRuntime.kt`: `c99001865c1c4eaeae7979af7d5c85daabe9f09c`; `RhinoSourceScriptRuntimeTest.kt`: `d2dbf0ac22735934fea10bdb71254b4a65bfd4da`.


## 14. Runtime-3.0 evidence checkpoint — method-level JS API, inherited scopes and Host Port mapping (2026-11-22)

Audit base: Badcat integration/legado-upstream-runtime at 0f15df328c31229627ec8cef6e398831e30c5299; upstream LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a. Static source/caller/API audit only. No compiler-resolved or runtime dynamic closure is claimed.

### 14.1 Corrected public-scope inventory

The CI #3109 API inventory artifact 14721576136 (ZIP SHA-256 a4aa3de9aea527a7edfaf56daeb16fb7abda89847cbf8cb5da11b2c214904a4e) is a Kotlin source-declaration inventory, not JVM ABI proof. It reports:
- JsExtensions: 118 declarations; JsEncodeUtils: 30.
- RegexJsExtensions: 6; WebJsExtensions: 13; SourceLoginJsExtensions: 6.
- It omitted the RssJsExtensions parent. The pinned parent declares 17 functions, two overrides (getSource/getTag) and 15 additional inherited methods: put, get, two searchBook overloads, addBook, showPhoto, open, setContent, setBaseUrl, setRedirectUrl, getStringList, two getString overloads, getElement and getElements.
- The inventory therefore undercounts this scope family by 15 declarations: 173 recorded + 15 inherited = 188 source declarations across the concrete scopes and shared helper base. This is not a count of unique JS names or JVM methods. WebJsExtensions and SourceLoginJsExtensions inherit the Rss parent surface; they do not expose only their locally declared 13/6 methods.
- RegexJsExtensions is a separate @js: replacement-rule scope; it implements JsEncodeUtils but is not the normal source JsExtensions bridge.

Pinned source blobs:
- JsExtensions.kt f6808edd2a0d312e8bd13649935dea444cb82581.
- RssJsExtensions.kt 8e01035e8d5802ed931ac46050cd45cfb0254701.
- RegexJsExtensions.kt b01d0bbc6b606832b390723269e4dbcbb834de69.
- WebJsExtensions.kt dd6532396b8f02e37376885aa2c6c1e430af1ad9.
- SourceLoginJsExtensions.kt a14f01b3774998d09619a79ce977fc593b6a28f1.

### 14.2 Main JsExtensions API mapped to actual providers

| API family | Pinned methods | Actual upstream provider/caller | Badcat at audited HEAD |
|---|---|---|---|
| Source identity/context and batch | getSource, getTag, getSourceNavigationContext, getBatchContext, cacheContent | BaseSource, AnalyzeRule, AnalyzeUrl, JsSourceEngine, BatchContentContext; JS and rule batch routes converge on BookHelp.saveContent | RhinoSafeSourceJavaExtensions exposes Map/Any? projections; batch context is null and cacheContent throws. Real BaseSource identity and batch lifecycle are absent. |
| Refresh/event | refreshBookInfo, refreshBookToc, refreshContent | EventBus refresh events consumed by product screens | Missing from main bridge; requires typed product-event port, not a no-op. |
| HTTP | ajax, ajaxAll, ajaxTestAll, connect, get, head, post | AnalyzeUrl, HTTP client, CookieStore, source-aware rate limiter; get/head/post preserve no-auto-redirect behavior while connect follows AnalyzeUrl semantics | Host transport callbacks and bounded Ajax helpers exist. Return types are RhinoSourceHttpResponse, not upstream Connection.Response/StrResponse; overload, timeout, redirect, error and response-object behavior still require differential proof. |
| WebView/browser/verification | webView, webViewGetSource, webViewGetOverrideUrl, startBrowser, startBrowserAwait, showBrowser, getVerificationCode | BackstageWebView, WebViewPool, SourceVerificationHelp, Activity/browser/captcha callbacks | No WebView/browser/verification provider is connected. These are required behavioral ports where reachable; do not delete or stub JS methods. |
| Navigation/video | openUrl, openVideoPlayer | source-navigation policy, confirmation/navigation Activity, VideoPlay/player Activity | No equivalent host methods. Badcat-owned navigation/player UI must implement callback semantics. |
| Cache/Cookie/files | cacheFile, getCookie, downloadFile, importScript, getFile, readFile, readTxtFile, deleteFile | CacheManager, CookieStore, AnalyzeUrl, FileUtils, source-scoped cache/file services | Cookie reader, file/cache storage, import and download helpers exist in part. Host getFile returns RhinoSourceFileHandle, not java.io.File; path, stream, mutation and expiry semantics need per-method tests. |
| Archive | unzipFile, un7zFile, unrarFile, unArchiveFile, getTxtInFolder, ZIP/RAR/7z string and byte-array readers | ArchiveUtils, LibArchiveUtils, EncodingDetect, file/cache providers | Archive helpers and bounds exist; format support, encoding, directory listing and failure/null behavior are not accepted against pinned source. |
| Encoding/crypto/URL | strToBytes, bytesToStr, Base64/hex, URI/HTML/time helpers, toURL, toNumChapter, plus 30 JsEncodeUtils declarations | pinned JsEncodeUtils, Hutool/Android crypto and URL utilities | Many helpers exist and Rhino crypto/encoding unit suites pass in #3109. Nullable inputs/outputs, flags, crypto object types, exception behavior and exact URL wrapper semantics remain to be compared. |
| Font | queryBase64TTF, queryTTF, replaceFont | QueryTTF, font cache/download and glyph mapping | TTF and replacement helpers exist; cache defaults, object identity, glyph fallback and byte limits need differential coverage. |
| Concurrency | singleFlight, lock, tick | source-keyed SourceLock and Rhino Function invocation | Host equivalents exist, but keying, timeout, return/exception and coroutine/thread behavior require differential tests. |
| Product config | getReadBookConfig, getReadBookConfigMap, getThemeMode, getThemeConfig, getThemeConfigMap | ReadBookConfig, ThemeConfig, AppConfig | Missing from main bridge; connect through a typed Badcat product-config port while keeping Badcat Reader/UI authoritative. |

### 14.3 Inherited and context-specific public surfaces

| Scope | Locally declared methods | Real caller/context | Badcat gap / boundary |
|---|---|---|---|
| RssJsExtensions parent | 15 additions listed in §14.1 | ReadRssActivity; also inherited by WebView and login bridges. open routes book/RSS/article navigation; search/add/photo methods invoke product UI; rule helpers delegate to AnalyzeRule. | Main bridge mixes some similarly named helpers but does not preserve parent context or all navigation semantics. RSS UI is not automatically in book-source runtime scope; inherited behavior remains required wherever Web/Login scripts receive this parent surface. |
| WebJsExtensions | upConfig, request, WebView-scoped ajax/connect/get/post/head/getStringList/getString, toast/log and navigation-context override (13 declarations including override) | BackstageWebView, WebViewActivity, ReadRssActivity, VideoPlayerActivity, BookInfoActivity and BottomWebViewDialog. Its request dispatcher has 16 async operations and Promise callback IDs. | No equivalent WebView bridge/Promise callback lifecycle in Badcat. WebView string-response methods are a distinct ABI from normal JsExtensions HTTP methods. |
| SourceLoginJsExtensions | upLoginData, reLoginView, refreshExplore, copyText, clearTtsCache, showBrowser override (6 declarations) | SourceLoginDialog, SourceCallBack, ReadBookActivity, BookInfoViewModel, VideoPlayerViewModel and ExploreAdapter; login UI and custom source buttons | Badcat has a partial login helper, not the full Rss-derived extension scope. Login callbacks, browser handoff, clipboard and refresh require typed host ports; TTS cache clearing is relevant only in its originating product context. |
| RegexJsExtensions | log, logType, t2s, s2t, get, put (plus inherited JsEncodeUtils) | RegexExtensions.replace and ReplacePreview for @js: replacement rules | Must remain a distinct replacement-rule scope with rule-variable storage; merging it into the per-source main bridge changes state ownership and context. |

### 14.4 Concrete API differences and unresolved semantics

The source inventory records 17 missing names, 22 overload-count differences and 38 same-name signature differences. The 17 missing names are:

getReadBookConfig, getReadBookConfigMap, getThemeConfig, getThemeConfigMap, getThemeMode, getVerificationCode, openUrl, openVideoPlayer, refreshBookInfo, refreshBookToc, refreshContent, showBrowser, startBrowser, startBrowserAwait, webView, webViewGetOverrideUrl, webViewGetSource.

The 22 overload-count differences affect ajaxAll, ajaxTestAll, get, the five missing config methods, getVerificationCode, openUrl, openVideoPlayer, the three refresh methods, showBrowser, startBrowser, startBrowserAwait, timeFormat, timeFormatUTC, webView, webViewGetOverrideUrl and webViewGetSource. Host get has an extra one-argument URL-or-key overload; this is an overload-resolution risk, not evidence of extra upstream compatibility.

Material same-name differences:
- getSource(): BaseSource? vs Map<String, Any?> loses real mutable source identity, NativeBaseSource wrapper behavior and source/sourceApi identity.
- getBatchContext(): BatchContentContext? vs Any? erases the concrete contract; current host returns null.
- getFile(path): File vs RhinoSourceFileHandle changes the Java object surface.
- Connection.Response/StrResponse vs RhinoSourceHttpResponse: custom wrapper parity is not proven.
- ajax has different nullable and option shapes; ajaxAll/ajaxTestAll use different list and overload shapes.
- getTag changes nullable to non-null; Base64/hex helpers change some nullable input/output contracts; log/logType change return shape; time methods accept Number/String instead of upstream Long; toURL returns RhinoLegadoUrl rather than JsURL.
- Main host bridge includes Rss-like get/put/getString/getStringList/setContent helpers in the same object, while upstream exposes these through a distinct Rss-derived scope. Host setContent returns String rather than a chainable AnalyzeRule; setBaseUrl and setRedirectUrl are not represented by the same API.
- Parameter-name/format-only differences are not automatically ABI breaks; separate them from JVM descriptors, nullability, return objects, overload resolution and runtime coercion.

### 14.5 Typed Host Port cut-set refined by method callers

1. Source model/object port: real BookSource/BaseSource object, stable key, source/sourceApi alias and NativeBaseSource wrapper registry.
2. Rule execution port: AnalyzeRule/AnalyzeUrl context, chainable RSS rule helper object, content/base/redirect state, URL and selector overloads.
3. HTTP port: distinct Ajax/StrResponse/Jsoup Response semantics, header coercion, timeout, redirect mode, Cookie, rate limit, cancellation and error propagation.
4. File/cache/archive port: File-vs-handle compatibility, source path sandbox, file IO, TTL/cacheFirst, importScript, ZIP/RAR/7z and encoding.
5. Batch/event port: typed BatchContentContext, chapter identity/save fence and three refresh events.
6. WebView/Promise port: pooled WebView ownership, JS interface registration/removal, 16 async dispatcher names, callback IDs, result-cache consume/cleanup, timeout/cancellation and navigation context.
7. Verification/browser port: start/show browser, captcha result/refetch and lifecycle-aware callback.
8. Login UI port: Login UI v1/v2 state, button callbacks, refresh, clipboard and source-specific browser routing.
9. Badcat product action/config port: search/add-to-shelf/photo/open/navigation/video, Reader/theme configuration and product events, all implemented by Badcat-owned UI.
10. Regex replacement port: isolated rule name/variables, @js: execution context and JsEncodeUtils behavior.
11. Concurrency port: source-class/key identity, singleFlight/lock/tick semantics, coroutine/thread context and cancellation.

No port above is implemented merely by appearing in this table. It is a contract boundary to be tested against the pinned upstream.

### 14.6 CI #3109 terminal evidence

- Run 44940353813, job 136955971824, exact HEAD 0f15df328c31229627ec8cef6e398831e30c5299; terminal conclusion: failure.
- Step 8 :app:testDebugUnitTest: 312 app unit tests, 2 failures. Both are Fourth-category AdMark:
  1. BadcatAdMarkCatalogTest.rejectedImportAndReplaceAllLeavePreviouslyPersistedCatalogUntouched — duplicate-record IllegalArgumentException at BadcatPurificationModels.kt:129, test line 98.
  2. BadcatPurificationModelsTest.adMarkCodecNeverImportsServerProvidedInjectionScript — expected [data-ad-container], got [.ad-card], test line 125.
- All 181 tests across Rhino-related, replacement, HTTP helper, ABI and source-login-session test classes in this report passed. The separate :legado-runtime:testDebugUnitTest pinned model contract suite passed 3/3. This does not prove complete upstream JS API compatibility.
- Step 20 is labelled Run bounded Rhino replacement host tests, but its failure is the two AdMark tests above; it is not a Rhino test failure.
- Step 22 API 35 WebViewOverloadResolutionInstrumentedTest failed because instrumentation shell returned empty stdout/stderr while exporting runtime-2.3-webview-report.json. The 80-byte report artifact contains only the missing-file diagnostic; four String/Number/null/undefined provider observations were not collected. Do not infer provider overload behavior.
- Step 18 G2d stage-1 authentic source-group compiler probe passed; it is not the full compiler-resolved runtime closure.
- Step 28 assembleDebug passed and artifact 14721362762 (badcat-legado-debug, 13,891,265 bytes; uploaded ZIP SHA-256 ad98aadfcd363a81b6ca4de8f70c3e7e387011bf4524d5a0da0f93de4a165d09) was uploaded. Diagnostic-only, not an accepted delivery APK.
- Test report artifact 14721273393 ZIP SHA-256 ced0b9928a536c9a456fec0b76f2773f111e1b31a900b0e79bbdbe151060dc35; WebView report artifact 14721298236 ZIP SHA-256 71dd703a064a68449cbe5f374734d3061106880f5d8d779aba38f38798a347b1; WebView diagnostics artifact 14721213741 ZIP SHA-256 f7f25debe59d2ee446d712b67339fdfc1ed4d2a9ea36afe8d7c0a2fc86b38b60.
- Dependency candidate artifact 14721601151 ZIP SHA-256 2760c91d4389b596cc0ddec34e17ded2364bc350d0d27efc4c68199dad705674: 1,186 indexed files, 1,085 reachable candidates, 152,175 lexical/import edges, 25,950 ambiguous same-package references and 906 generated/dependency-provided unresolved imports. It remains a lexical candidate graph, not compiler-resolved closure.
- No live-source steps ran; best-effort steps were skipped.

### 14.7 Decision and next step

- GO: first-pass method/provider/context mapping is sufficient to register the next static audit layer; retain corrected 188-declaration scope count and explicit missing/incompatible methods.
- NO-GO: no broad upstream copy, production wiring, compiler PoC, schema/Room migration, caller cutover or complete compatibility claim. The WebView overload experiment has no valid observation; the JS declaration report is not JVM ABI proof.
- Read-only source review and artifact inspection only. No production code, tests, Gradle, workflow, DB, UI/Reader, AdMark, PPRule or main changed in this audit step.
- Next single audit item: complete compiled JVM/Rhino-visible surface audit for inherited and overloaded scopes: annotations, generated overload bridges, return descriptors, wrapper registration, R8 keep rules and WebView interface exposure. Use reproducible compiler/bytecode inventory; do not rerun the broken WebView probe until its report handoff root cause is separately registered.
