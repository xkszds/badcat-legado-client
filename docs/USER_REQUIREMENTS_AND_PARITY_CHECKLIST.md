> **范围说明（重要）**：本文件仅是 Legado 引擎技术迁移细项，不是用户所说的“坏坏猫复刻五大类”。坏坏猫产品级五大类要求以 [BADCAT_REPLICATION_FIVE_CATEGORIES.md](BADCAT_REPLICATION_FIVE_CATEGORIES.md) 为准；后续必须同时维护两份清单，不得用本文件替代 UI、阅读器、AdBlock/AdMark、PPRule 等复刻任务。


> **必读入口已调整（2026-11-20）**：本文件保留为详细历史需求/实施记录，不再要求每次开发重读。新的流程入口是 [总流程清单](required/00-MASTER-WORKFLOW.md)，对应分类唯一必读清单是 [第三类 Legado](required/03-LEGADO-RUNTIME.md)。分类清单已汇总当前状态、下一步和验收；只有追查本文件的具体历史细节时才打开。


# 用户要求：完整还原原版 Legado（长期执行清单）

> 本文件是本仓库的长期需求基线。后续会话、开发者和自动化任务必须先读本文件与 `docs/full-legado-runtime-migration.md`，再继续实现；不得把已完成的局部解析器移植误报为完整兼容。所有需求、缺口、测试证据和决策都必须持续更新在 GitHub，不依赖聊天记录。

## 总目标（不可缩减）

将 Badcat 做成**独立运行**的 Legado 书源兼容客户端：以官方 Android Legado 固定上游版本为行为标准，导入同一份书源数据后，搜索、详情、目录、正文和脚本行为尽可能与该版本一致。不得要求用户另装 Legado，不得以调用另一个 Legado App 的 ContentProvider/HTTP API 代替本地引擎，不得只实现常见 CSS 子集就宣称兼容。

**行为基准：**
- 上游仓库：https://github.com/LegadoTeam/legado
- 固定版本：`330e606c4d57e08adc056ff8ab7a4132c784da9a`
- 上游代码许可证：GPL-3.0；保留版权、许可证和来源说明。
- 不跟随浮动分支。升级上游必须单独审查版本差异、依赖和回归结果。

## 五大类完整需求

以下五类共同构成用户要求的完整范围；不能只做其中一类，也不能把测试工具或 UI 假实现算作引擎完成。

### 1. 书源数据、管理与持久化

- [ ] 兼容官方书源 JSON 的导入、导出、编辑、复制、启用/禁用、排序、分组、搜索、批量操作。
- [ ] 书源全部字段无损读写；未知字段必须保留，不能因 Badcat 内部模型转换而丢失。
- [ ] 兼容官方书源字段、规则对象、变量、请求头、登录配置、发现页、搜索规则、详情规则、目录规则、正文规则、书籍类型及扩展字段。
- [ ] 兼容书源分组、源 URL、源更新时间、启用状态、权重/排序、并发与相关设置。
- [ ] 源级 Cookie、登录状态、JS 键值存储、缓存和运行状态按源隔离；应用重启后按官方语义恢复。
- [ ] 导入导出往返测试：官方样本 JSON -> Badcat -> JSON，语义和字段不丢失。
- [ ] 对非法 JSON、字段类型变化、缺字段、未来扩展字段有稳定处理；错误可诊断，不静默清空。
- [ ] 书源管理 UI 与本地引擎使用同一份持久化数据，不维护两套互相漂移的模型。

### 2. HTTP、URL、Cookie、登录与网络行为

- [ ] 移植/适配上游 `AnalyzeUrl` 完整语义：URL 模板和变量插值、相对 URL、动态 URL、请求方法、请求体、表单、请求头、编码、超时、重定向、重试、缓存及请求选项。
- [ ] GET/POST/其他上游支持的方法，文本与二进制响应，状态码、响应头、最终 URL、字符集和错误对象均按上游语义传递。
- [ ] CookieJar/CookieStore：域、路径、过期、Secure、HttpOnly、同站策略及重定向链路行为；不同书源相互隔离。
- [ ] 登录规则与登录态：登录 URL、登录脚本、验证码/交互所需桥接、登录状态判断、退出/重新登录。
- [ ] 请求前后 JS、请求回调、响应回调、变量替换和源内共享状态。
- [ ] DNS、TLS、代理（如上游版本支持）、压缩、缓存、重试边界与网络错误分类。
- [ ] WebView/浏览器渲染能力：动态网页、JS 渲染、Cookie 共享、页面等待、脚本执行、DOM 读取；无法支持的平台能力必须明确报错，不能伪装成功。
- [ ] 所有网络能力可注入、可测试；不能把 HTTP 响应过早压成字符串而丢失上游对象信息。
- [ ] 使用 MockWebServer/本地测试服务器覆盖 GET/POST、编码、重定向、Cookie、超时、失败重试、分页和动态 URL。

### 3. 解析器与规则语言

- [ ] 与固定上游一致的 CSS/JSoup 选择器、元素集合、属性、文本、HTML、节点、相对链接和 base URI 处理。
- [ ] 完整支持上游规则操作符、链式选择器、嵌套规则、默认规则、规则组合、回退/交替、列表索引及排除语义。
- [ ] 完整支持 XPath/JsoupXpath、JSONPath、正则表达式及上游定义的各类提取器和格式。
- [ ] 支持文本规则、列表规则、节点规则、属性规则、过滤、替换、分割、拼接、去重、排序及空值处理。
- [ ] 支持规则内嵌 JS、`@js:`、`@get:`、`@put:` 等上游语法和表达式求值顺序。
- [ ] 保留上游错误分类、异常上下文和定位信息，便于显示具体源、规则和响应片段。
- [ ] 以 pinned upstream 的实现与测试为准；不要自行发明“看起来类似”的 CSS 解析语义。
- [ ] 回归用户已报告规则：`@.w_txt li.4@textNodes`，以及真实书源中的链式集合、冒号索引/排除、属性提取和回退表达式。
- [ ] 对 HTML 空白、实体、畸形 DOM、脚本/样式节点、相对链接和中文编码建立固定 fixtures。

### 4. JavaScript / Rhino 宿主运行时

- [ ] 集成固定上游 Rhino 版本、初始化方式、优化级别、执行上下文、线程/协程约束及异常映射。
- [ ] 集成上游 `JsExtensions` 和所有被书源实际使用的 Java/Android 宿主 API；逐个登记可用性，不可只支持少数字符串函数。
- [ ] 支持 `java`、`Packages`、`org.jsoup` 等上游公开桥接入口及其兼容对象/方法（按安全边界实现；不可用能力要明确列出）。
- [ ] Jsoup Document/Element/Elements 的常用 API：parse、select/selectFirst、size、get、text、html、attr、nextElementSibling、remove 等。
- [ ] 支持 `baseUrl`、`result`、`cookie`、`source`、`book`、`chapter` 等上下文变量及上游作用域。
- [ ] 支持 mainJs、jsLib、源内公共脚本、共享 JS scope、源级隔离、脚本持久化变量及正确的初始化/销毁时机。
- [ ] 支持异步/同步宿主调用边界、HTTP 请求回调、响应对象、内容设置、日志、异常和超时/执行限制。
- [ ] 覆盖上游加解密、哈希、编码、字符串/日期/集合等扩展方法；按固定上游源码建立 API 清单。
- [ ] 对 JS 脚本执行加资源和安全边界，但不得通过禁掉真实书源依赖的 API 来换取表面安全；每项限制需有兼容性记录。
- [ ] 测试 ES5/常见 Rhino 语法、Java 互操作、JSoup 桥接、全局变量、共享作用域、异步请求、异常与超时。

### 5. 搜索、发现、详情、目录、正文与阅读体验

- [ ] 用上游执行链实现搜索、发现/探索、书籍详情、目录、正文五个入口；每个入口的规则、状态和错误处理保持上游语义。
- [ ] 搜索：分页、关键字编码、结果列表、去重、字段补全、跨源并发、停止/取消和失败源隔离。
- [ ] 发现：分类、分页、分类 URL/参数、发现规则及源配置。
- [ ] 详情：字段提取、书籍 URL 规范化、封面/简介/作者/分类/字数/状态/最新章节等字段。
- [ ] 目录：分页加载、目录 URL 更新、卷/分卷节点、排序、倒序、去重、章节名清洗、VIP/付费/锁定等元数据。
- [ ] 正文：章节 URL、正文规则、内容分页/分段（contentBatch）、正文预处理、替换规则、图片/媒体内容及章节缓存。
- [ ] 阅读状态：章节索引、上/下一章、目录刷新、书籍更新、断点续读、缓存失效和失败重试。
- [ ] 保留官方替换规则能力及执行顺序；书籍/章节/正文的替换范围和作用域必须与上游一致。
- [ ] Badcat 搜索页、书籍详情页、目录页和阅读器必须全部接到同一套上游兼容引擎，禁止仅在测试入口使用新解析器。
- [ ] 真机端到端验收：导入源 -> 搜索 -> 详情 -> 全目录/分页 -> 打开正文 -> 下一章 -> 重启后恢复。
- [ ] 真实源覆盖不同网站结构、API 源、JS 源、需要登录/Cookie 的源、分页源和动态网页源。

## 质量门槛与完成定义

- [ ] 每项能力都要标注：上游源码位置、Badcat 对应实现、测试文件、测试结果、已知差异。
- [ ] 单元测试必须直接运行 Kotlin/Android 引擎代码；Python requests/BeautifulSoup 冒烟只能作为网站可用性探测，不能算 Badcat 引擎验收。
- [ ] CI 必须运行模块单元测试、Android 编译、导入导出测试、解析器测试、HTTP/JS 测试和端到端测试。
- [ ] 上传测试报告；失败时记录具体失败用例，不允许只看 workflow 的最终绿/红。
- [ ] 建立真实书源语料库并记录来源、版本、脱敏方式和覆盖维度；不得把少数样本描述成全量语料通过。
- [ ] 每个真实源至少验证搜索/发现（若支持）、详情、目录、正文；记录源 ID、规则类型、结果摘要和失败原因。
- [ ] 与上游同版本对照测试：相同输入 HTML/JSON/书源配置，比较提取结果和异常行为。
- [ ] 只有五大类所有必需项完成、核心差异清零或经用户明确接受、CI 通过、真机端到端通过，才能宣称“与该固定版本原版 Legado 等价”。
- [ ] 若某项暂不支持，必须在文档/UI 明确标记，不可吞掉规则、静默回退并返回空结果。

## 2026-11-19 增量验证记录

- 提交 `04d96e0336bfdf78cd0cc1c851241dff2ce13aa4`：扩展 Rhino JSoup 兼容回归，覆盖 `selectFirst()` 与带 base URI 的相对链接 `absUrl()`。
- GitHub Actions run `44391264577`：`:integration-contract:test` 对应的 contract unit tests 通过；`:app:testDebugUnitTest` 对应的 bounded Rhino replacement host tests 通过；`:app:assembleDebug` 通过，debug APK 已上传为 artifact `14469785604`（SHA-256 `7f444ea967152796dba6fc0b921fa3a4b0bf64b77a12bfb05ee1f6d1c1a2f06f`）。
- 同一提交的实时书源探测步骤仍在执行中；它们不是 Kotlin 引擎单测，也不能代替真机验收。
- 后续提交 `20c321510e6f075b3975d21d262b7518c796f4e6`：继续扩展 JSoup facade 回归，新增 `Element.hasClass()`、`Element.parent().tagName()`、`Elements.isEmpty()` 的调用覆盖；该提交的 CI 尚待执行。
- 旧记录中列出的集合解析/冒号索引/Bixiange 三项失败，已在 run `44391264577` 的 contract unit test 步骤中整体通过；仍需持续保留这些回归用例。

## 当前已知状态（2026-11-19）

- 已固定上游 submodule：`upstream/legado` -> `330e606c4d57e08adc056ff8ab7a4132c784da9a`。
- 已移植上游 RuleAnalyzer / AnalyzeByJSoup / SourceHtmlParser 的部分代码，并有 CSS adapter；不能据此声称完整规则兼容。
- 已接入共享规则分发路径的尝试仍未通过 CI，须修正后再继续。
- 最近 CI：run `44385813513`，失败于 `:integration-contract:test`，3 个失败用例：
  1. `LegadoRuleExpressionDispatcherTest.evaluatesRealWorldLegadoChainedCollectionRoots`：期望两条 HTML 列表项，实际输出格式/内容不一致。
  2. `LegadoRuleExpressionDispatcherTest.matchesUpstreamColonSeparatedLegacyIndexListsAndExclusions`：冒号索引/排除语义输出不一致。
  3. `PublicBookSourceCompatibilityCorpusTest.bixiangePublicCssSearchRulesParseChainedBookListAndFields`：期望两本书，实际只得到一本。
- 在这些失败修复并通过 CI 前，不得继续扩大“已兼容”声明；先修正集合解析与索引语义并加上游对照测试。
- 早期 Python/BeautifulSoup 对网站的探测成功，不代表 Badcat Kotlin 引擎已能读取这些源。
- 之前报告过的 `@.w_txt li.4@textNodes` 必须保留为永久回归测试。

## 每次会话/提交必须遵守

1. 开始前先读本文件、迁移总计划和最新 CI。
2. 先修复当前失败，再做下一项迁移；不允许忽略红灯。
3. 每次提交更新本文件中的状态、提交 SHA、测试结果和未完成项。
4. 代码必须以固定上游源码为准，尽量直接移植；平台耦合通过明确 adapter/SPI 处理。
5. 不允许以大段 TODO、空壳 API、固定假数据或“兼容层返回空值”标记为完成。
6. 不允许只写聊天总结；所有长期要求、决策、差异、失败和下一步都要写回 GitHub。
7. 不擅自合并到 main；在迁移分支完成并验收前持续在 `integration/legado-upstream-runtime` 开发。

## 2026-11-19 continued runtime compatibility increment

- Hardened the bounded chapter-cache reader against streams that return zero from a bulk read; it now falls back to a single-byte read to guarantee forward progress. Added exact-limit, over-limit, empty-input and zero-progress regression tests.
- Made `SourceScriptFileCacheStore.getBytes/putBytes` mandatory adapter operations instead of lossy UTF-8 defaults. Added tests for arbitrary binary payload preservation, defensive copies and source isolation; updated the injected test adapter accordingly.
- Normalized harmless repeated/trailing source-file separators while continuing to reject dot-segments and traversal. The folder symlink regression now exercises a repeated trailing separator.
- Matched pinned `JsExtensions.parseJsRequestHeaders` behavior for malformed JSON and null header values; added alias/parser regression coverage.
- Added optional timeout overloads for `java.get/head/post`, wired per-call timeouts through the Android request factory, and matched the upstream 30-second default for those helpers.
- CI run [#44422285115](https://github.com/xkszds/badcat-legado-client/actions/runs/44422285115), commit `36e62023f4006db47876c69bff1b550b776730b8`: contract tests, bounded Rhino tests and Debug APK assembly passed; best-effort live source probes were still running at last inspection. This run predates the HTTP timeout overload/factory changes.
- Latest implementation commit at this update: `beda18696217ec9c2c65af6a6c8bcc3b5f06f2f9`. Its CI run [#44422727859](https://github.com/xkszds/badcat-legado-client/actions/runs/44422727859) was pending at last inspection.
- These are compatibility/security increments only. They do not complete the full upstream `JsExtensions` host API, WebView/browser/verification UI, batch content context, font APIs, refresh events, or any of the original Badcat UI, reader, AdBlock/AdMark and PPRule work.

## 2026-11-19 HTTP redirect/timeout compatibility correction

- Split the pinned Legado HTTP helper request paths: `get/head/post` use a no-auto-redirect transport (including explicit timeout overloads), while `connect` and selector URL fetches use the redirect-aware transport. This preserves source rules that inspect 3xx responses and their `Location` headers.
- The first CI attempt after this split exposed one stale test fixture: the no-timeout `connect` failure test injected the old no-redirect callback. Updated it to inject the redirect-aware GET callback. This was a test-fixture mismatch, not a production regression.
- Run [#44423657512](https://github.com/xkszds/badcat-legado-client/actions/runs/44423657512), commit `adc6434cfa1166dc2b78a38e3a3ff5ccadc84fae`: contract tests and bounded Rhino tests passed; Debug APK assembly completed successfully. Best-effort live probes were still running at last inspection.

## 2026-11-19 redirect-aware download and batch request follow-up

- Extended the redirect-aware GET path beyond `connect`: `downloadFile`, ZIP/RAR/7z URL extraction, `ajaxAll`, the legacy one-argument `get(url)`, and URL-based selector fetches now use the redirect-following transport. The pinned two/three-argument `get/head/post` helpers remain no-auto-redirect so callers can inspect 3xx responses.
- Updated archive, charset, binary-download, cacheFile, ajaxAll, and source-runtime regression fixtures to inject the correct transport.
- CI run [#44425052095](https://github.com/xkszds/badcat-legado-client/actions/runs/44425052095), commit `83e8c1f9f4f028d34d07d82c76f8b067b7ca8390`: contract tests and bounded Rhino tests passed; Debug APK assembly completed. Best-effort live source probes were still running at last inspection.
- Earlier runs #44424684356 and #44424846495 failed because their commits contained the new redirect routing before all corresponding test fixtures were migrated; the fixture updates are included in the verified #44425052095 head.

## 2026-11-19 cache eviction and AtomicFile durability follow-up

- Added a shared cache-pruning helper that protects the just-written target from eviction, even when filesystem timestamp resolution ties it with older entries. Added deterministic tie-order and LRU eviction tests.
- Chapter and source-file caches now treat pruning as best-effort after `AtomicFile.finishWrite`; an eviction error cannot trigger `failWrite` and roll back a successfully committed cache entry.
- Source-file cache operations are serialized with a process-wide lock across multiple store instances. Successful reads refresh last-modified time so the shared budget can evict least-recently-used entries.
- CI run [#44426131263](https://github.com/xkszds/badcat-legado-client/actions/runs/44426131263) passed the cache-pruner tests, contract/Rhino suites and Debug APK assembly for the helper/test increment.
- Run [#44426760656](https://github.com/xkszds/badcat-legado-client/actions/runs/44426760656) exposed a Kotlin return-type inference issue after moving chapter pruning outside the AtomicFile write block. The explicit `Unit` result was added in the current head.
- Current implementation head: `bf790b0f912ee7a5d14abd41b100d4d585cd444d`. Run [#44427488271](https://github.com/xkszds/badcat-legado-client/actions/runs/44427488271), head `bf790b0f912ee7a5d14abd41b100d4d585cd444d`, passed contract tests, bounded Rhino tests and Debug APK assembly; best-effort live probes were still running at last inspection.

## 2026-11-19 cache and reader lifecycle increment

- Protected newly committed chapter/source-cache files from eviction when file timestamps tie; shared pruning now removes older entries first. Successful source-cache reads refresh recency, and file-store operations use a process-wide lock across multiple runtime/store instances.
- Cache pruning is now best-effort after `AtomicFile.finishWrite`, so a pruning error cannot call `failWrite` after a successful commit and discard the new cache entry.
- Added TOC-based saved-progress resolution. If a source removes/changes the saved chapter URL, the invalid progress key is cleared and reading starts from the first current chapter instead of opening a stale URL.
- Guarded reader background callbacks against updating activities after they are finishing/destroyed.
- Run [#44426131263](https://github.com/xkszds/badcat-legado-client/actions/runs/44426131263) passed cache-pruner regression tests, contract/Rhino suites and Debug APK assembly. Run [#44426760656](https://github.com/xkszds/badcat-legado-client/actions/runs/44426760656) exposed a return-type inference issue in the chapter cache; an explicit `Unit` was added.
- Current code head: `802bb3bbe9a797e12f103ee8b9fc622ab4d116f4`. Run [#44427488271](https://github.com/xkszds/badcat-legado-client/actions/runs/44427488271), code head `bf790b0f912ee7a5d14abd41b100d4d585cd444d`, passed contract tests, bounded Rhino tests and Debug APK assembly; best-effort uploaded-corpus reading was still running at last inspection. This run predates the newest saved-progress/lifecycle UI commits.


## 2026-11-19 bounded ajaxTestAll error-cause traversal

- Audited the pinned upstream `JsExtensions.kt` and `AnalyzeUrl.kt` at `330e606c4d57e08adc056ff8ab7a4132c784da9a`; confirmed the upstream test-mode transport mapping: timeout `-2`, unknown host `-3`, connect `-4`, socket `-5`, SSL `-6`, configured call-timeout `-1`, other I/O `-7`.
- Found that the host adapter walked `Throwable.cause` with an unbounded sequence. A cyclic cause graph supplied by a gateway wrapper could keep an `ajaxTestAll` worker stuck indefinitely. Cause traversal is now capped at 16 links; nested SocketTimeoutException and cyclic-cause regressions were added.
- Previous head `cacf216c3b65e11695260b8a73e138be74b22296` CI run [#44485135456](https://github.com/xkszds/badcat-legado-client/actions/runs/44485135456): contract tests passed; Android/Rhino test task was blocked before test execution because Maven Central returned HTTP 429 resolving `me.zhanghai.android.libarchive:library:1.1.6`. This is an external dependency-rate-limit failure, not a test assertion failure. Re-run verification after this increment.
- This is a defensive correction in Legado category 3 only. Full `JsExtensions`, `AnalyzeUrl`, WebView/DNS, upstream model/pipeline integration and Badcat categories 1, 2, 4 and 5 remain incomplete.


## 2026-11-19 analyzer dependency audit and CI completion

- Re-read the root `AGENTS.md`, this checklist and `docs/full-legado-runtime-migration.md` before continuing. Confirmed the active branch is `integration/legado-upstream-runtime`; upstream submodule gitlink remains exactly `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Workflow [#44550830295](https://github.com/xkszds/badcat-legado-client/actions/runs/44550830295), commit `6e1ffd1e513cc2e77581c889822e28e54f63558a`, completed with conclusion `success`. Contract tests, bounded Rhino host tests, Debug APK build/upload and best-effort live source checks for Nyasama, Feisuzhong, Pinshuzhai and four uploaded-corpus samples all completed successfully. The live checks are not device validation or full upstream parity.
- Unit-test report artifact `14542741454`: 253 tests, 0 skipped, 0 failures, 0 errors (parsed from the uploaded XML report in the preceding verification).
- Debug APK artifact `14543241920` is an intermediate debug build only. It is not labeled or delivered as the requested complete Legado APK.
- Added `docs/upstream-analyzer-integration-audit.md` after reviewing pinned `AnalyzeRule.kt`, `AnalyzeUrl.kt`, `JsExtensions.kt`, `BatchContentContext.kt`, upstream source models and the current host service graph. It records the real dependency families, exact lifecycle surface, host API gaps, pre-change risks and staged integration order.
- Confirmed the existing `:integration-contract` JVM source set compiles selected parser files only; current per-stage CSS services and the staging `BookSource` model are not the upstream lifecycle/models. Do not pass them to upstream analyzer classes or add fake application globals.
- Current incomplete areas remain: full upstream models/converters; real `AnalyzeUrl` transport semantics; `AnalyzeRule` lifecycle; all `JsExtensions` host API families; search/detail/TOC/content operation migration; batch save context and durable persistence; full upstream parity fixtures; Android device installation/reading validation.
- Next gate: model/serialization foundation and rule-entity graph inventory, followed by real host service contracts. No UI redesign.


## 2026-11-19 upstream model/serialization audit

- Added `docs/upstream-model-serialization-audit.md` after inspecting pinned `BookSource`, `BaseSource`, `BaseBook`, `Book`, `BookChapter`, `SearchBook`, `RssArticle` and all source rule entity declarations/converters.
- Confirmed the host `ReadingEngine.BookSource` and `BookChapter` are staging/UI projections, not upstream runtime entities. The source codec preserves raw JSON but does not provide typed rule accessors, `BaseSource` lifecycle, `RuleDataInterface`, or full chapter metadata.
- Recorded the module boundary risk: current `:integration-contract` is JVM-only, while upstream entities require Android Parcelable/Room and application services. Do not copy Android-bound entities into the JVM module or create fake `appCtx/appDb/BaseSource` implementations.
- Next code gate is an Android-side upstream model/runtime adapter with lossless raw JSON retained alongside typed runtime models. Before changing any model, re-read `AGENTS.md`, search all field consumers/callers/tests, record ABI/serialization risks and add object/string/null/default/unknown-field round-trip fixtures.


## 2026-11-19 session progress ledger (必读清单复核后更新)

### 已完成（仅审计/文档，不等于运行时功能完成）

- [x] 每次会话必读政策复核：完整重读根目录 `AGENTS.md`，并重读 Legado 技术清单、迁移总计划、分析器审计、模型序列化审计。
- [x] 坏坏猫五大类范围复核：重读五大类总清单、项目完成清单、UI 重建图、PPRule 证据、原版净化证据；确认五类仍需分别推进。
- [x] 固定上游模型依赖审计：核对 `BookSource`、`BaseSource`、`BaseBook`、`Book`、`BookChapter`、`SearchBook`、`RssArticle` 和六类嵌套规则及其 Gson/Room/Android 依赖。
- [x] 宿主差异登记：确认现有 `ReadingEngine.BookSource/BookChapter` 是导入/展示阶段模型，不是官方运行时实体；现有 raw JSON 保留不能替代规则对象、BaseSource 生命周期和 RuleDataInterface。
- [x] 下一阶段风险登记：JVM/Android 模块边界、Parcelable/Room、Gson 反序列化、ABI/defaults、未知字段保留、章节 index 身份和源级状态隔离已记录在 `upstream-model-serialization-audit.md`。

### 进行中

- [ ] 官方模型/规则实体的真实集成：当前只有依赖审计，尚未移植/接入完整实体图。
- [ ] 官方序列化适配：当前 codec 保留原始 JSON 并提取有限字段，尚未接入官方 `INITIAL_GSON/GSON` 语义和 Room converters。
- [ ] CI：run [#44554459580](https://github.com/xkszds/badcat-legado-client/actions/runs/44554459580)（head `913665c66af80ffdcfddd28fad9836e22210add5`）目前 pending，不能标记通过。较早 run [#44553806204](https://github.com/xkszds/badcat-legado-client/actions/runs/44553806204) 的 contract/Rhino 测试、Debug APK 组装/上传和前 4 个 live smoke 步骤已成功；4 个上传语料源的最后一个 live reading 步骤仍在运行。

### 明确未完成（不要误勾）

- [ ] 完整 `AnalyzeUrl` / `AnalyzeRule` 生命周期。
- [ ] 全量 `JsExtensions` 宿主 API 和真实 Android 服务映射。
- [ ] 搜索/发现/详情/目录/正文统一上游流程。
- [ ] `BatchContentContext` 与真实章节持久化。
- [ ] 五大类中的原版 UI、阅读器、AdBlock/AdMark、PPRule。
- [ ] 真机安装、端到端阅读验收、正式签名 Release APK。

### 下次唯一优先任务

在改代码前重新执行 AGENTS 第 8 条检查：确认分支和 pin、读取精确上游声明、全仓搜索受影响字段/适配器/调用者/测试，并写兼容风险。随后从 Android 模块的官方规则模型与 Gson 兼容层开始；先建立 object-form / string-encoded / null / defaults / unknown-field fixtures，再接入源 catalog。不得把 DTO 或 raw JSON codec 说成官方运行时模型。


## 2026-11-19 pinned rule-model implementation increment

- [x] Vendored the exact pinned upstream rule-model source files into the Android app under `io.legado.app.data.entities.rule`: `BookListRule`, `SearchRule`, `ExploreRule`, `BookInfoRule`, `TocRule`, `ContentRule`, `ReviewRule`, `ExploreKind`, `FlexChildStyle`, and `RowUi`.
- [x] Vendored upstream `GsonExtensions.kt` with `INITIAL_GSON`, `GSON`, rule deserializers, primitive string/int handling and helper conversions, unchanged from pinned revision `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- [x] Added the upstream Kotlin Parcelize plugin and Flexbox 3.0.0 dependency required by those exact model files.
- [x] Added `PinnedLegadoRuleModelsTest` covering all six BookSource nested rule classes, object-form and legacy JSON-string rule values, defaults/null, ExploreKind/RowUi, ReviewRule summary gating and ContentRule string representation.
- [x] Updated `THIRD_PARTY_NOTICES.md` and `third_party/legado/NOTICE.md` to record the vendored GPL-3.0 source paths and pinned revision.
- [ ] CI verification for this code commit is pending; do not mark tests/build passed until the workflow and reports are inspected.
- [ ] This does **not** integrate the upstream `BookSource`, `BaseSource`, `BaseBook`, `Book`, `BookChapter`, Room converters/database, source lifecycle, `AnalyzeRule`, `AnalyzeUrl`, or the complete reading engine. Raw JSON preservation remains the import/export boundary.
- Next: verify the new source/model build and test results, then implement the typed `BookSource` runtime adapter only after its `BaseSource/JsExtensions` service dependencies are mapped.

## 2026-11-19 本轮执行前记录：Legado 上游加密扩展依赖审查

> 本节在本轮任何运行时代码修改之前写入。执行顺序：先审查本清单及 AGENTS，再按以下范围调查；若审查发现范围/风险不准确，必须先修订本节再改代码。

### 本轮唯一工作流
- [ ] 仅推进 Legado；不切换到坏坏猫 UI、阅读器、AdBlock/AdMark 或 PPRule 的实现任务。
- [ ] 核对上一轮 JsExtensions/JsEncodeUtils 审计列出的 19 个缺失继承方法，优先检查上游 SymmetricCryptoAndroid、AsymmetricCrypto、Sign 的精确实现和 Hutool/相关依赖版本。
- [ ] 从固定上游提交 330e606c4d57e08adc056ff8ab7a4132c784da9a 的 Gradle/version catalog/锁定文件定位依赖来源；不得根据猜测添加版本或替代实现。
- [ ] 搜索宿主当前加解密 helper、同名方法、所有调用方、测试、JS 可见对象和安全边界，形成逐项兼容矩阵。
- [ ] 只有依赖、调用影响和测试边界确认后，才选择可独立验证的最小实现切片；不得把 AES/DES CBC helper 冒充完整上游 crypto API。
- [ ] 对新增/迁移 API 同时补上游语义测试和旧宿主行为回归测试；覆盖错误输入、空值、字节/字符编码、密钥/IV、模式/填充及 JS 互操作（按上游真实契约选取）。
- [ ] 运行聚焦测试、最广可行 Android 测试/构建和 CI；读取失败日志与产物信息。CI 未结束时明确标记待验证。

### 本轮开始前事实与阻塞
- 当前开发分支必须是 integration/legado-upstream-runtime；开始时 HEAD 为 6fa843febb293e95a9a0da44acc9e38b64776b48，禁止写入 main。
- 固定上游仍为 330e606c4d57e08adc056ff8ab7a4132c784da9a。
- 上一轮审计记录：上游 JsExtensions.kt 有 122 个函数声明，宿主桥接有 214 个；另有上游 JsEncodeUtils.kt 的 30 个继承声明。宿主缺少 18 个直接方法名和 19 个继承 crypto 方法名；同名方法也未等同于语义兼容。
- 当前宿主 RhinoLegadoSymmetricCrypto 是受限 AES/DES CBC helper，不覆盖任意 transformation、3DES、非对称加密或签名；不能直接映射上游工厂方法。
- 主要风险：Hutool 版本/Android 适配不一致、上游参数解析及 Base64/Hex 语义差异、Rhino 返回对象方法形状、既有脚本依赖和输入安全边界。
- 当前 HEAD 的 Actions run 44558528157 尚为 pending；不能据此宣称上一轮验证通过。

### 本轮完成定义（仅限本轮范围）
- [ ] 依赖坐标与版本由固定上游构建配置证实，并记录源文件位置。
- [ ] crypto API 与宿主现有实现/调用者/测试的冲突审查完成，审计矩阵更新。
- [ ] 若实施代码：新旧行为测试均通过，聚焦构建通过，完整 diff 复核通过。
- [ ] 本轮 CI 结果、提交 SHA、未完成事项写回本清单和迁移计划。
- [ ] 复审本节是否遗漏了上游行为、宿主调用方、安全边界或验证要求；若有遗漏，先更正清单再继续。

### 当前状态
- [x] 本轮范围、前置事实、主要风险和验收门槛已在开始代码工作前登记。
- [ ] 依赖审查尚未执行；本节的计划项不能计为代码功能完成。

## 2026-11-19 本轮 Legado 加密依赖审计结果与下一项预登记

### 已完成（仅限源码/依赖审计）
- [x] 从固定上游 app/build.gradle 与 gradle/libs.versions.toml 核实依赖：Hutool Crypto 5.8.22、Bouncy Castle Provider 1.86、Bouncy Castle PKIX 1.86；Badcat 当前 app 模块尚未引入这些依赖。
- [x] 检查固定上游 SymmetricCryptoAndroid、AsymmetricCrypto、Sign 和 JsEncodeUtils crypto 工厂/旧方法的实际源码。
- [x] 确认当前宿主 createSymmetricCrypto 返回受限 RhinoLegadoSymmetricCrypto（仅 AES/DES CBC），与上游 Hutool SymmetricCrypto 对象契约不同。
- [x] 检查 RhinoSourceScriptRuntime 的 java bridge 注入，以及现有 AES/DES、AES Base64、JavaImporter 安全边界测试。
- [x] 发现并记录上游旧方法中的历史行为差异；实现时须按 pinned source 复现，不得自行修正成“更合理”的行为。

### 审计发现与未完成
- [ ] GitHub code-search 响应 incomplete_results=true，不能证明无其他调用方；修改签名之前仍须做完整源码 grep。
- [ ] 尚未添加 Hutool/BC 依赖或上游 crypto 类；19 个缺失继承 API 仍未补齐。
- [ ] 尚未验证 Hutool 在当前 Android/JVM 配置下的算法提供者、RSA/签名和非默认 transformation 行为。
- [ ] 尚未完成 crypto 对象的 Rhino 暴露与方法可见性测试。

### 下一任务（必须在代码编辑前执行完整调用方 grep）
- [ ] 在当前分支完整搜索 createSymmetricCrypto、RhinoLegadoSymmetricCrypto、aesCrypt、所有 aes/des/tripleDES helpers、createAsymmetricCrypto、createSign、digest/HMac 的定义、调用方、测试和 JS 兼容 shim。
- [ ] 仅在调用方影响审查完成后，按 pinned upstream 引入 Hutool 5.8.22、BC provider/PKIX 1.86 和三份 crypto 类；保留受限 JavaImporter，不开放通用 Packages 访问。
- [ ] 逐个实现上游工厂 overload 与 19 个继承 API，按源码保留历史方法语义；对现有 AES/DES 用例补回归。
- [ ] 增加 RSA/签名、3DES、Hex/Base64 分流、空/无密钥随机初始化、非法密钥/IV/算法、Rhino 方法链和包访问边界测试。
- [ ] 聚焦测试、Android app unit tests、Debug APK build 和最新 CI 全部检查后再标记完成。

### CI 状态复核
- 当前 HEAD 6fa843f 对应 run 44558528157 已从 pending 变为 cancelled；不能算通过。
- run 44557806917（head 28cff4a）仍为 in_progress；contract tests、bounded Rhino tests、Debug APK build/upload 和前面 live smoke 步骤已通过，最后四源语料步骤当时仍在运行。
- 文档预登记提交为 73ade446aae4f37c46e742d43b76ecf3a1cfbe31；该提交后的 CI 尚未观察到结果。

### 必读清单自审
- [x] 清单已区分“依赖/源码审计完成”与“运行时代码完成”，没有将审计误标成 crypto 功能完成。
- [x] 已将旧 helper、动态 Rhino 调用、JavaImporter 安全边界及上游历史 quirks 纳入风险。
- [ ] 完整调用方 grep 和 Hutool Android 行为仍是代码实施前置条件；如后续发现清单遗漏，必须先改清单再编码。

### 补充调用方审查（本轮继续核对）
- [x] 已扫描 app/src/main/java/app/badcat/client/runtime 下全部 23 个 Kotlin 运行时源文件中的目标 crypto 标识符。
- [x] 已扫描 app/src/test 下 29 个 Kotlin 测试文件中的目标 crypto 标识符；现有动态调用集中在 RhinoSourceScriptRuntimeTest.kt，覆盖 AES/DES factory、AES Base64 helper、digest/HMAC 和 JavaImporter 安全边界。
- [x] 发现 RhinoLegadoJsReplacementEvaluator.kt 另有独立的 digestHex/digestBase64Str/HMacHex/HMacBase64 实现；它不调用 RhinoSafeSourceJavaExtensions 的 crypto factory，迁移时必须保持该替换运行时行为并纳入回归审查。
- [x] 确认 RhinoSourceScriptRuntime.kt 通过 LEGADO_CRYPTO_COMPAT_JS 提供窄化的 JavaImporter/Packages shim，并将 source-scoped RhinoSafeSourceJavaExtensions 实例绑定为 scope["java"]；AES JavaImporter 限制必须保留。
- [ ] 尚未完成 app UI 其他目录、integration-contract 与 legado-rhino 全仓范围的目标标识符 grep；GitHub code-search 的 incomplete_results=true 不能替代本地完整 grep。

### 必读清单自审修正
- [x] 将替换规则运行时的独立摘要/HMAC 实现补入旧调用方审查范围。
- [x] 将 LEGADO_CRYPTO_COMPAT_JS 与 source-scoped java bridge 的分层关系补入安全边界要求。
- [ ] 完整仓库 grep 仍是任何签名/返回类型变更前的阻塞项；不能因 app runtime 目录扫描完成就误标为全仓审计完成。

## 2026-11-19 CI 状态更正（本轮复核）
- run [44557806917](https://github.com/xkszds/badcat-legado-client/actions/runs/44557806917)，head 28cff4a9771fdbd32b4a085386c3d435db610b82：已完成且 conclusion=success。contract unit tests、bounded Rhino host tests、Debug APK assemble/upload、Nyasama/公开源/Feisuzhong/Pinshuzhai 与四源上传语料 live smoke 步骤均完成成功；产物包含 debug APK artifact 14546626986、unit test reports 14546077767 和 live smoke reports。
- run [44558528157](https://github.com/xkszds/badcat-legado-client/actions/runs/44558528157)，head 6fa843febb293e95a9a0da44acc9e38b64776b48：已结束为 cancelled，不计为通过。
- run 44557806917 验证的是旧代码提交 28cff4a，不是当前文档 head；不能把它写成当前 HEAD 的验证结果。当前文档提交后的新 CI 尚未检查到完成结果。
- 该 CI 成功只说明该提交上的测试/构建/所配置 live smoke 成功，不等于完整 Legado、真机安装或最终 APK 验收。

## 2026-11-19 完整仓库 crypto 调用方搜索结果与下一任务预登记

### 完整调用方搜索（代码编辑前）
- [x] 已按当前分支 Git tree 枚举全部 231 个 Kotlin 源/测试文件，扫描目标 crypto API/helper 标识符；覆盖 app main、app tests、integration-contract main/tests、pinnedLegado sources 与 legado-rhino。
- [x] 已枚举并扫描全部 21 个 Java 源文件。
- [x] 除已记录的 `RhinoSafeSourceJavaExtensions.kt` 实现、`RhinoSourceScriptRuntimeTest.kt` 动态脚本测试，以及 `RhinoLegadoJsReplacementEvaluator.kt` 独立摘要/HMAC 逻辑外，其他 Kotlin/Java 文件没有发现目标 crypto API/helper 的额外引用。
- [x] app UI、书源模型、integration-contract、pinnedLegado 与 legado-rhino 均已纳入本次搜索；此前“未扫描 UI/integration-contract/legado-rhino”的限制已关闭。
- [x] 保留搜索边界说明：扫描覆盖当前 tree 中全部 Kotlin/Java 文件；不把文档提及视为运行时调用。既有 replacement evaluator 的 digest/HMAC 与 JavaImporter shim 属于不同路径，仍需单独回归。

### 本轮搜索结论
- 旧 `createSymmetricCrypto` 的已知实际运行时消费者为 source-scoped `java` bridge 的 Rhino 测试；未发现 app UI 或 integration-contract 对 `RhinoLegadoSymmetricCrypto` 类型的静态依赖。
- `RhinoSourceScriptRuntime` 每个 source 创建并注入 bridge 到 `scope["java"]`；不得改为全局共享 crypto 实例。
- `LEGADO_CRYPTO_COMPAT_JS` 中受限 JavaImporter 仍有独立 AES/Java class allow-list；不能为了支持 Hutool 而开放任意 Packages/reflection。
- `RhinoLegadoJsReplacementEvaluator` 有独立 digest/HMAC 实现；本轮 crypto 工厂迁移不得改变它的作用域、算法、编码或超时行为。

### 下一项代码任务（在本记录之后方可实施）
- [ ] 依赖：在 Android app module 引入固定上游 Hutool Crypto 5.8.22、Bouncy Castle provider 1.86、PKIX 1.86；按上游依赖排除规则处理重复 BC 包，检查 Android D8/R8 与运行时 provider。
- [ ] 上游移植：按 pinned commit 原样移植 `SymmetricCryptoAndroid`、`AsymmetricCrypto`、`Sign`，并只移植其真实依赖的 Base64 NO_WRAP 与 `String.isHex()` 精确语义；保留 GPL-3.0 来源/版权声明。
- [ ] Bridge：将 `createSymmetricCrypto` 的两个现有宿主 overload 迁移到上游 Hutool-backed `SymmetricCryptoAndroid`；补齐 pinned `JsEncodeUtils` 的其余 overload 和 19 个缺失方法，按上游保留历史方法行为，不擅自修正源码 quirks。
- [ ] 安全：crypto factory 仅经每源 `java` bridge 暴露；不扩展 JavaImporter、Packages、反射或任意类加载白名单；为 key/data 输入增加边界时不得改变合法上游输入语义。
- [ ] 回归：保留现有 AES round-trip、DES decrypt、AES Base64、digest/HMAC、JavaImporter allow-list 测试；新增 3DES、RSA/非对称 encrypt/decrypt、Sign sign/verify、Hex/Base64 识别、key/IV overload、null/random key、错误算法/密钥和 Rhino 对象方法链测试。
- [ ] 复审：实现后重新搜索全仓所有受影响方法/helper/类型，审查 Rhino 可见签名、空值、编码、异常、provider 与 Android API；运行 focused tests、`:app:testDebugUnitTest`、`:app:assembleDebug` 和当前 CI，查看完整失败日志与 APK artifact。

### 清单自审
- [x] 此前全仓搜索范围不完整；现已补齐全部 Kotlin/Java 文件并更正，不再把 incomplete GitHub code-search 当作无调用方证据。
- [x] 明确区分 source `java` bridge、JavaImporter sandbox 和替换规则 evaluator 三条路径。
- [x] 已将下一次 runtime code change 的目标文件族、上游来源、调用方、风险、回归和验收门槛写在代码编辑之前。
- [ ] 若依赖检查或实现时发现新的调用者/ABI 风险，必须先修订本节再继续。


## 2026-11-19 Legado crypto implementation increment (verification pending)

- [x] Added pinned Hutool Crypto 5.8.22, Bouncy Castle provider 1.86 and PKIX 1.86 dependencies to the Android app module.
- [x] Vendored pinned upstream `SymmetricCryptoAndroid`, `AsymmetricCrypto`, `Sign`, `EncoderUtils` and exact `String.isHex()` semantics.
- [x] Replaced the host AES/DES-CBC-only `createSymmetricCrypto` facade with upstream factory overloads; added `createAsymmetricCrypto`, `createSign` and all 19 missing inherited crypto method names.
- [x] Preserved upstream historical quirks in AES/DES/3DES legacy methods.
- [x] Removed the obsolete `RhinoLegadoSymmetricCrypto` facade while retaining the separately bounded `aesCrypt` helper used by the JavaImporter shim.
- [x] Added Rhino regression tests for AES helpers/quirks, DES/3DES, random-key and factory overloads, RSA encrypt/decrypt and SHA256withRSA sign/verify.
- [x] Kept source-scoped `java` injection, the JavaImporter allow-list and the independent replacement-evaluator digest/HMAC path unchanged.
- [ ] Android unit tests, Hutool provider behavior, D8/R8 packaging, Debug APK assembly and CI are pending. Do not call this increment verified until logs and artifacts are inspected.
- [ ] Re-audit the full diff and affected callers after CI; resolve any overload, Android provider or Rhino reflection issue before proceeding to BaseSource.


## 2026-11-19 crypto security-boundary correction gate

### Diff-review finding
- [x] The first Hutool bridge increment restores the upstream API surface, but review found that the new Hutool object can process arbitrarily large byte arrays/streams. The removed `RhinoLegadoSymmetricCrypto` previously enforced a 2 MiB input/output bound. This is a host resource boundary and must not be silently lost.
- [x] Existing JavaImporter `aesCrypt` still has its own bounded AES-CBC path; that does not bound the new Hutool object returned by `createSymmetricCrypto`.

### Corrective task registered before further runtime edits
- [ ] Preserve the full upstream crypto method/object shape while adding a host-owned bounded subclass/wrapper: maximum 2 MiB input and output for symmetric/asymmetric/signature operations, including byte arrays, strings and InputStreams; InputStreams must be read with forward-progress checks and without closing caller-owned streams.
- [ ] Keep all valid upstream algorithms, transformations, key formats, IV behavior, Base64/Hex semantics and overloads within the bound. Do not revert to the AES/DES-only allow-list.
- [ ] Apply key-material bounds and output bounds to the asymmetric and Sign wrappers; preserve upstream exception behavior for invalid algorithms/keys as far as possible.
- [ ] Add regression tests for exactly-at-limit and over-limit inputs/outputs, InputStream bounds, existing AES/DES/3DES/RSA/signature behavior, and unchanged JavaImporter class/package restrictions.
- [ ] Re-run the full relevant unit tests and Android build/CI. Inspect provider/D8/R8 logs and test reports before marking the crypto slice verified.

### Self-review correction
- [x] The prior implementation checklist did not explicitly carry forward the old facade's 2 MiB crypto data limit. This omission is corrected here before any further runtime code change.
- [ ] Do not proceed to BaseSource/BookSource until this bounded-wrapper gate and the current crypto CI are resolved.


## 2026-11-19 crypto resource-boundary implementation (verification pending)

- [x] Added a host-owned bounded subclass around pinned `SymmetricCryptoAndroid`; symmetric encrypt/decrypt now caps byte-array, string-decoded and InputStream data/results at the previous 2 MiB boundary.
- [x] Added bounded InputStream reading with zero-progress fallback and no close of caller-owned streams.
- [x] Applied equivalent 2 MiB data/key/output checks to the upstream asymmetric wrapper and `Sign` wrapper while retaining the full Hutool algorithm surface.
- [x] Updated the source-scoped `createSymmetricCrypto` factory to construct the bounded upstream-compatible subclass; no AES/DES-only transformation allow-list was reintroduced.
- [x] Added exact-limit, over-limit, output-overflow, InputStream forward-progress, asymmetric key and signature-input tests.
- [ ] Current-head CI, Android provider/D8/R8 behavior and full Rhino regression remain pending; do not mark the crypto slice complete until results are inspected.


## 2026-11-19 post-commit crypto bridge/test audit correction

- [x] Diff re-review found the bridge factory still instantiated the unbounded `SymmetricCryptoAndroid` rather than the registered `RhinoBoundedSymmetricCryptoAndroid`; therefore the 2 MiB limit was not yet applied through Rhino. The previous checklist status overstated the factory wiring.
- [x] Diff re-review also found the newly inserted bounded test was spliced into the preceding overload test's assertion line, leaving malformed Kotlin test source. The test file must be repaired before CI can be trusted.
- [ ] Correct the factory construction and check key/IV sizes before creating the Hutool object.
- [ ] Rebuild the complete affected test section so each test method is syntactically separate and the original overload test remains intact.
- [ ] Inspect exact diff around factory and test insertion, then rerun CI; do not count the earlier pending/cancelled run as verification.
- [x] This correction is recorded before the next code edit; the previous implementation remains unverified.


## 2026-11-19 crypto bridge/test wiring correction applied (verification pending)

- [x] `createSymmetricCrypto` now checks key/IV sizes and constructs `RhinoBoundedSymmetricCryptoAndroid`; the unbounded class is no longer used by the source bridge.
- [x] Repaired the malformed test insertion. The overload test is restored as a complete method and the 2 MiB/InputStream regression test is a separate complete method.
- [x] Asymmetric Base64/Hex string decrypt now decodes with the pinned Hutool `SecureUtil.decode` path and checks decoded byte length before crypto processing.
- [ ] CI and full test suite remain pending; inspect current run logs and confirm no compile/runtime regressions before marking the crypto slice verified.


## 2026-11-20 Legado crypto verification and correction task — PRE-REGISTERED

### Current baseline (verified before this task)
- [x] Re-read root `AGENTS.md` in full and the required product/engine checklists, migration plan, model/serialization audit, analyzer audit, JsExtensions audit, UI map, PPRule evidence and original purification evidence.
- [x] Confirmed active branch `integration/legado-upstream-runtime`; current HEAD before this ledger update: `f924681f783628dedf6f835e191a701ba5ae6a16`. Never modify `main`.
- [x] Confirmed pinned upstream Legado revision `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- [x] Inspected latest code diff: bounded asymmetric encoded input now decodes through pinned Hutool `SecureUtil.decode` and applies the 2 MiB decoded-byte check.
- [x] Current crypto runtime has been implemented but is NOT verified. CI run `44564284230` (parent code head `24c39c4...`) was still running contract tests; run `44564458448` for current head `f924681...` was pending with no jobs yet.

### Task scope — verification first, then only evidence-driven fixes
- [ ] Poll run `44564284230` and current-head run `44564458448`; inspect every job step and full logs, especially app unit tests, Rhino crypto tests, Hutool/BC provider, D8/R8 and Debug APK assembly.
- [ ] If current-head run has not started or did not include the latest source commit, wait for/trigger a current-head verification only after confirming the workflow state; never infer success from the parent run.
- [ ] Inspect the exact current diff and all changed crypto files/tests for key/data/result bounds, String Base64/Hex decoding, caller-owned InputStream handling, Android provider registration and Rhino-visible overload behavior.
- [ ] If CI reveals a defect, register the precise corrective scope and regression test here BEFORE changing runtime code; then fix only that scope and rerun focused and broad CI.
- [ ] If CI passes, inspect test reports and APK artifact metadata; report only what the workflow actually verifies. Android physical-device/provider acceptance remains a separate open gate.
- [ ] After verification, re-audit affected legacy AES/DES/3DES helpers, RSA/signature, JavaImporter allow-list, source-scoped bridge and independent digest/HMAC replacement evaluator.

### Acceptance criteria
- [ ] Contract unit tests pass.
- [ ] Bounded Rhino host tests pass, including AES/DES/3DES/RSA/signature, exact-limit/over-limit and stream behavior.
- [ ] Android Debug APK assembles with Hutool/BC dependencies (no D8/R8 duplicate/provider packaging failure).
- [ ] Current HEAD—not merely an ancestor—has a completed successful CI run and inspectable reports/artifact.
- [ ] No relaxation of JavaImporter/Packages/reflection sandbox and no regression to existing crypto behavior.
- [ ] No claim of complete Legado or device validation from this crypto-only slice.

### Self-review of checklist accuracy
- [x] Corrected the previous session's stale HEAD report: current branch is `f924681...`, not `24c39c4...` or `41cb8083...`.
- [x] Explicitly separated the current pending run from the ancestor in-progress run.
- [ ] At task end, record actual CI status, inspected logs/artifacts, any code correction and the resulting commit/HEAD; re-evaluate whether this checklist omitted any test or compatibility risk.


## 2026-11-20 correct crypto encoded-output policy from original host source — PRE-REGISTERED BEFORE CODE EDIT

### Source evidence and checklist correction
- Re-read the pre-crypto host bridge at commit `9d9b2ef47e801e3c5b6bff8d4173d8c9073b3c2c`, including the complete original `RhinoLegadoSymmetricCrypto` class.
- The original helper capped UTF-8 plaintext input at 2 MiB; capped raw ciphertext input at 2 MiB; capped encoded ciphertext input strings at 4 MiB; and checked decoded Base64 ciphertext bytes at 2 MiB.
- The original `encryptBase64` and `encryptHex` simply encoded the result of `encrypt(data)` and did not cap the encoded output string. The original encrypt path also did not cap the ciphertext result. For example, a 2 MiB AES/CBC/PKCS5Padding plaintext legitimately produces 2 MiB + 16 ciphertext bytes; Base64/Hex output is correspondingly larger.
- Therefore the prior checklist requirement to cap symmetric encoded output strings at 2 MiB was inaccurate and would regress existing behavior. This correction supersedes the previous 2 MiB encoded-output requirement.

### Correct compatibility/resource policy
- Symmetric: preserve 2 MiB raw plaintext input, 2 MiB raw ciphertext input, 4 MiB encoded ciphertext input and 2 MiB decoded ciphertext input. Do not impose a new cap on symmetric encrypt output, Base64 output or Hex output; preserve upstream encoding semantics and the prior host behavior.
- Asymmetric and Sign are newly exposed host surfaces: retain 2 MiB raw data/key/signature/output bounds. Permit Base64/Hex string expansion up to 4 MiB (Hex can be 2x a 2 MiB raw result); do not cap encoded strings at 2 MiB.
- Keep InputStream forward-progress and caller-ownership behavior, factory overloads, algorithm support, and the JavaImporter allow-list unchanged.

### Next implementation task (registered before code edit)
- [ ] Remove the symmetric encryption-result and Base64/Hex encoded-output caps from `RhinoBoundedSymmetricCryptoAndroid`; retain input and decrypt-ciphertext bounds exactly as above.
- [ ] Change the shared encoded-result bound for new asymmetric methods to 4 MiB while retaining 2 MiB raw output/key/input bounds.
- [ ] Correct `preservesCryptoTwoMibibyteBoundsAndInputStreamProgress` so padded encryption of a 2 MiB plaintext is allowed and returns the upstream ciphertext expansion; test Base64/Hex expansion is not truncated/rejected.
- [ ] Replace `boundsSymmetricBase64AndHexEncodedOutputsAcrossOverloads` with regression coverage for every applicable ByteArray/String/Charset/InputStream overload: 2 MiB raw input accepted, 2 MiB + 1 raw input rejected, Base64/Hex output expansion allowed, raw/encoded ciphertext input limits preserved.
- [ ] Keep RSA Base64/Hex round-trip tests and the asymmetric raw/key bounds; document that no artificial 2 MiB encoded-output fixture is claimed.
- [ ] Re-run full app tests, contract tests and Debug APK CI on the final code head; inspect full logs/reports/artifact IDs.

### CI/test-source status at registration
- Run `44565856067` for head `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92` failed at `:app:compileDebugUnitTestKotlin` because the intermediate test source referenced `checkedRhinoCryptoEncodedOutput` twice. The log was inspected; APK assembly/upload and live-source steps were skipped.
- Commit `43baad71f6937f25baafc80541ec3e787d7731d6` removed those orphan test references. A full current test-file search found no remaining `checkedRhinoCryptoEncodedOutput` references.
- Run `44566457890` targets code commit `43baad71f6937f25baafc80541ec3e787d7731d6` and was in progress at registration. Current documentation head `03fd349af49424703718cb843d5c5b3caf7f26f7` has a pending documentation-only run `44566732977`. Neither is a pass yet.

### Self-review
- [x] Corrected the previous checklist's mistaken assumption that the old host helper capped encoded output strings.
- [x] Replaced the incomplete output-bound premise with exact source-proven raw/encoded input limits and compatibility behavior.
- [ ] After implementation, re-audit every symmetric overload and the new asymmetric/sign encoders against pinned Hutool 5.8.22 signatures and test results.

## 2026-11-20 restore pinned Legado Chinese conversion runtime — PRE-REGISTERED AFTER CI FAILURE REVIEW

### Confirmed CI failure and source audit
- Current-head predecessor CI run `44564706186` (head `d140b35d614cbad2afe8cffa81f106aaab639a1d`) failed at `:app:compileDebugKotlin` with `RhinoSafeSourceJavaExtensions.kt:1934:38 Unresolved reference 'LegadoChineseConversion'`; the APK step was skipped and no APK artifact was produced. Contract tests passed; this is not a crypto-test failure.
- The current source tree contains the call `LegadoChineseConversion.traditionalToSimple(value)` but no production declaration/file for `LegadoChineseConversion`; only the Chinese-conversion test file exists.
- The pinned upstream implementation is `upstream/legado/app/src/main/java/io/legado/app/utils/ChineseUtils.kt` at `330e606c4d57e08adc056ff8ab7a4132c784da9a`. It delegates `s2t/t2s` to quick-transfer-core and loads the exact `TRADITIONAL_TO_SIMPLE` exclusion dictionary before the first traditional-to-simplified conversion. Its excluded terms include `多士`, which the existing regression test requires to remain unchanged.
- The CI workflow's “Restore pinned Legado Rhino extension sources” step only restores Rhino XML implementation Java sources; it does not restore Chinese conversion helpers. Therefore the unresolved symbol is a missing host source integration, not a missing Rhino artifact.

### Registered correction scope
- Integrate the exact pinned upstream Chinese conversion utility behavior from `ChineseUtils.kt` (including lazy/one-time exclusion dictionary initialization and the full exclusion list) using the already declared quick-transfer-core dependency; preserve the current public `java.t2s` and `java.s2t` bridge behavior.
- Audit the current bridge imports/callers and all Chinese conversion tests before changing names or packages. Do not introduce a second converter with different dictionary semantics, silently drop exclusions, or change the dependency version.
- Keep this correction isolated from crypto algorithms, output bounds, Rhino JavaImporter allow-list and other source APIs.

### Required regression/acceptance
- Keep and pass the existing traditional/simplified conversion tests, including the excluded-term fixture `多士`.
- Add coverage for representative pinned upstream exclusion entries and repeated calls (dictionary initialization must be safe and idempotent).
- Run focused Chinese conversion tests, `:app:testDebugUnitTest`, `:integration-contract:test`, and `:app:assembleDebug`; inspect the complete CI steps/logs/reports/artifacts.
- Do not treat run `44564706186` as a pass. Inspect the new code-head CI run separately; no APK is accepted until the build artifact is actually uploaded and verified.

### Execution discipline
- This missing-source correction is registered before its code edit. After implementation, record exact source/test files, test outcomes, CI head/run/artifacts, commit SHA, and self-review of any additional missing source restoration dependencies.


## 2026-11-20 crypto encoded-output test compile correction — historical failure and source fix

- Run `44565690133` (head `75ae47aa3005418e01f47838ecc9e811b888538c`) and run `44565856067` (head `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92`) failed `:app:compileDebugUnitTestKotlin` because the intermediate test file referenced the nonexistent `checkedRhinoCryptoEncodedOutput` helper at two locations. Production `:app:compileDebugKotlin` passed; APK assembly/upload were skipped.
- Commit `43baad71f6937f25baafc80541ec3e787d7731d6` removed the two orphan assertions and retained direct tests of the real production output methods. A full scan of the current test file found zero remaining references to `checkedRhinoCryptoEncodedOutput`.
- Run `44566457890` targets code commit `43baad71f6937f25baafc80541ec3e787d7731d6` and was in progress when this record was written. It must be inspected before marking this compile correction verified.
- The earlier statement that the current head `e6bc27a` still contained the orphan references is historical/stale and is superseded by the `43baad7` source correction.
- No APK artifact or crypto completion claim is made until the current code-head CI passes and the artifact is inspected.

## 2026-11-20 crypto encoded-output test correction — implementation recorded, verification pending

- Corrective task was pre-registered in this file and the three companion required checklists before the test edit.
- CI run `44565690133` (head `75ae47aa3005418e01f47838ecc9e811b888538c`) and final predecessor run `44565856067` (head `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92`) both passed contract tests and production `:app:compileDebugKotlin`, but failed `:app:compileDebugUnitTestKotlin` on two unresolved references to the nonexistent `checkedRhinoCryptoEncodedOutput` helper at the encoded-output test. APK assembly/upload were skipped. Full logs were inspected; no unit-test report or APK artifact was uploaded for those runs.
- Removed the two orphan helper assertions from `app/src/test/kotlin/app/badcat/client/runtime/RhinoSourceScriptRuntimeTest.kt` in commit `43baad71f6937f25baafc80541ec3e787d7731d6`. The real production boundary is exercised directly by the existing exact/over-limit symmetric Base64 and Hex assertions; the redundant fake helper is no longer referenced.
- Existing RSA bridge test now includes Base64 and Hex encrypt/decrypt round trips (added in predecessor commit `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92`). A >2 MiB RSA encoded-output fixture is not claimed; current practical RSA fixture verifies both encodings and round trips, while production code performs the size check on encoded results.
- CI run `44566457890` targets code commit `43baad71f6937f25baafc80541ec3e787d7731d6`; it was queued/in progress at the time of this entry. It must complete and its full job steps/logs/artifacts must be inspected before marking the test correction or crypto CI gate passed.
- Current implementation status: compile-error source correction committed; focused test execution, full app unit tests, APK assembly/artifact verification and device/provider validation remain pending. No crypto completion or APK release claim is made.
- Checklist self-review: this correction accurately captured the actual compile failure and removed only the undeclared test helper; existing direct boundary tests remain. No runtime behavior or algorithm was changed. Next review must inspect all tests/logs on the exact code-head CI.


## 2026-11-20 crypto compatibility regression correction — PRE-REGISTERED BEFORE RUNTIME CODE EDIT

### Current branch and CI evidence
- Active branch: `integration/legado-upstream-runtime`; pre-registration parent HEAD: `bc18f521f5717bd8ead23677a22c0c9ae7b4e8fb`; pinned Legado upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Runs `44566457890` (code head `43baad71f6937f25baafc80541ec3e787d7731d6`) and `44566732977` (docs head `03fd349af49424703718cb843d5c5b3caf7f26f7`) both completed with failure in `:app:testDebugUnitTest`. Contract tests, pinned Rhino artifact restore/verification, pinned Rhino extension-source restore, and production Kotlin compilation succeeded. APK assembly/upload and live-source checks were skipped. Each run uploaded only unit-test reports: artifact `14552755775` and `14552826190`; neither uploaded an APK.
- The inspected report for run `44566457890` records 266 tests with 6 failures:
  1. `supportsPinnedLegadoAsymmetricCryptoAndSignFactories`: Android `Base64.encodeToString` stub was not mocked.
  2. `supportsPinnedLegadoDesAndTripleDesLegacyHelpers`: same Android Base64 stub failure.
  3. `supportsPinnedSymmetricCryptoOverloadsAndRandomKeySemantics`: Rhino cannot resolve the upstream overload call with `(String, null, null)`; it is ambiguous between nullable ByteArray and String overloads.
  4. `boundsSymmetricBase64AndHexEncodedOutputsAcrossOverloads`: Android Base64 stub was not mocked.
  5. `supportsLegadoSymmetricCryptoBase64AndHexEncryptionHelpers`: Android Base64 stub was not mocked.
  6. `supportsPinnedLegadoAesLegacyHelpersAndHistoricalQuirks`: Android Base64 stub was not mocked.
- The same failure pattern is present on the latest docs-head run; no successful current-code CI or APK is established.

### Pinned upstream and host source audit
- Pinned upstream `JsEncodeUtils.kt` exposes the same crypto factory overloads as the host: nullable `ByteArray?, ByteArray?`, non-null `ByteArray`, non-null `String`, and `String, String?`. The test expression passing two untyped JS nulls is ambiguous in Rhino and must not be mistaken for a production API regression.
- Pinned upstream `EncoderUtils.kt` uses Android `Base64`; the host JVM unit-test runtime currently throws “Method encodeToString in android.util.Base64 not mocked”. The host needs a tested, platform-neutral adapter that preserves the upstream flag/encoding semantics used by the crypto bridge, including NO_WRAP encoding and NO_WRAP decoding, rather than weakening the crypto tests or silently skipping the affected methods.
- `RhinoSafeSourceJavaExtensions.kt` has three legacy Base64 NO_WRAP decode call sites using the Android stub; these must use the same tested host codec/semantics.
- Current `RhinoBoundedSymmetricCryptoAndroid` caps both encryption results and Base64/Hex encoded output at 2 MiB. This contradicts the source-proven pre-crypto host behavior recorded above: the original symmetric bridge capped raw plaintext input at 2 MiB, raw ciphertext input at 2 MiB, encoded ciphertext input text at 4 MiB and decoded ciphertext at 2 MiB, but did not cap encryption result bytes or encoded output strings. A 2 MiB AES/CBC/PKCS5Padding plaintext legitimately produces a 2 MiB+16 ciphertext.
- Current `requireRhinoCryptoEncodedSize` also limits new asymmetric encoded outputs to 2 MiB despite the raw output bound being 2 MiB; Base64/Hex expansion must be allowed up to 4 MiB.

### Registered implementation scope — do not edit runtime code before this ledger commit
1. In `RhinoBoundedSymmetricCryptoAndroid.kt`, retain the 2 MiB raw encrypt input bound, 2 MiB raw decrypt input/output bound, 4 MiB encoded ciphertext input text bound, stream forward-progress and caller-owned stream behavior; remove the encrypt-result and symmetric Base64/Hex output caps. Preserve Hutool algorithms, transformations and every existing overload.
2. In `RhinoCryptoBounds.kt`, make the encoded-string bound 4 MiB for the newly exposed asymmetric Base64/Hex result surfaces; retain 2 MiB raw input/key/signature/output limits. Keep decoded asymmetric ciphertext and raw output limits unchanged.
3. In `AsymmetricCrypto.kt`, preserve Base64/Hex encoding behavior and permit encoded output expansion up to 4 MiB; keep raw key/data/output bounds. Do not add fake oversized RSA fixtures or claim a >2 MiB RSA ciphertext test where no practical supported fixture exists.
4. Replace the Android-only Base64 dependency in the host `EncoderUtils` adapter with a JVM-testable implementation that matches the pinned Android Base64 flags/semantics used by this code (especially default NO_WRAP encoding, URL-safe/no-padding flags if exposed, and DEFAULT/NO_WRAP decoding). Replace the three `AndroidBase64.decode(..., NO_WRAP)` legacy bridge calls in `RhinoSafeSourceJavaExtensions.kt` with the equivalent tested host decoder. Do not change JavaImporter allow-lists or expose Android classes to scripts.
5. In `RhinoSourceScriptRuntimeTest.kt`, remove only the invalid Rhino test invocation that passes untyped nulls to overloaded `createSymmetricCrypto`; preserve JS tests for String and ByteArray overloads. Verify random-key semantics directly through the real Kotlin upstream-compatible constructor with a round-trip test, not a fake JS API or a removed production overload.
6. Replace the output-cap test with regression tests covering every applicable ByteArray/String/Charset/InputStream overload: 2 MiB raw plaintext accepted, 2 MiB+1 rejected, AES/CBC padding expansion accepted, Base64/Hex output expansion accepted, raw ciphertext input still capped at 2 MiB, encoded ciphertext text capped at 4 MiB, decoded ciphertext capped at 2 MiB, and stream ownership/forward progress retained. Add focused tests for Base64 flag compatibility and the legacy NO_WRAP decode helpers.
7. Re-audit all crypto bridge methods, inherited Hutool overloads, source-scoped Rhino call sites, the independent replacement evaluator and the JavaImporter sandbox. Keep algorithms, key parsing, provider dependencies and source isolation unchanged.

### Required acceptance
- Focused crypto/encoder tests pass; all app unit tests pass; `:integration-contract:test` passes; `:app:assembleDebug` succeeds.
- Current final code HEAD has a completed successful CI run. Inspect every job step, complete failure logs, unit-test XML/HTML reports, APK artifact ID/size/digest and workflow head SHA.
- Verify the exact AES/CBC 2 MiB-to-(2 MiB+16) expansion and Base64/Hex outputs; verify all raw/encoded input boundaries and legacy AES/DES/3DES/RSA/Sign behavior.
- Do not mark crypto complete or provide an APK until the current-head CI and APK artifact are verified. Physical-device/provider validation and the broader full-Legado migration remain open.

### Execution discipline and self-review
- This task and its risks/tests/acceptance are registered before any runtime code edit. Only the above scope is authorized; if source inspection discovers another affected overload/caller, amend the ledger first.
- After implementation, append exact changed files, focused/full test results, CI run/head, report and APK artifact metadata, commit SHA, unresolved device/full-Legado gates, and a self-review of whether the registration captured every affected call path.


### Test-harness amendment — preserve pinned Android Base64 implementation
- Source review confirms the Android Base64 calls are part of the pinned upstream implementation. Replacing them with a locally invented codec would create avoidable behavioral drift and is not authorized.
- Amend item 4 above: keep production `EncoderUtils` and the three `AndroidBase64.decode(..., NO_WRAP)` bridge calls unchanged. Add Robolectric `4.14.1` as a test-only dependency and move only the six Android-Base64-dependent crypto tests into a dedicated `RhinoCryptoCompatibilityTest.kt` using `RobolectricTestRunner` with an explicit supported SDK and `Config.NONE`. This exercises the real Android Base64 implementation rather than changing it.
- The null/null overload failure is a test-expression ambiguity against the exact pinned upstream overloads, not a missing factory. Keep the String and ByteArray JS overload tests; test random-key semantics directly through the actual Kotlin `RhinoBoundedSymmetricCryptoAndroid(transformation, null)` constructor.
- All output-bound corrections, input-bound tests, stream tests, asymmetric raw/encoded policy, JavaImporter restrictions, and acceptance gates registered above remain unchanged.
- This amendment is recorded before the runtime/test/build-file edits; if Robolectric cannot run on the pinned CI toolchain, stop and register an alternative test-only approach before changing production Base64 semantics.


## 2026-11-20 crypto helper rename compile failure — PRE-REGISTERED BEFORE CORRECTION

- CI run `44568645734` for code head `534262df7feb30009bb7d62d4ce1e4617f09e955` completed with failure at `:app:compileDebugKotlin`: five remaining call sites in `RhinoBoundedSymmetricCryptoAndroid.kt` referenced `boundedBase64` / `boundedHex` after the helper declarations had been renamed to `encodeBase64` / `encodeHex`. Contract tests and pinned Rhino artifact verification passed; unit tests, APK assembly/upload and live-source steps did not run.
- The latest code head `6d1b3a946e877d2cce433332ca7ce536482bd3b4` contains the same incomplete rename; its workflow run `44568939955` was pending and cannot be treated as validation.
- Registered correction: update every remaining call site to the chosen helper names (or consistently revert the declaration rename), inspect the complete file for zero stale identifiers, then compile and run the focused/full tests. Do not change crypto semantics or output/input bounds in this correction.
- After correction, inspect the new exact-head CI logs and artifacts. This failure is a source compile defect, not an Android Base64/Robolectric test result; the six previous unit-test failures remain unverified until tests actually run.


## 2026-11-20 crypto regression test fixture correction — PRE-REGISTERED BEFORE TEST EDIT

- Latest-head run `44569202532` on `11a2f0c059dabf4a8c116fcd16585803b935e9f1` passed pinned-upstream verification, contract tests, Rhino artifact verification and source restoration. `:app:testDebugUnitTest` ran 266 tests: 264 passed, 2 failed. Unit-test report artifact: `14552993253` (SHA-256 `f85c44da84c3c56c85f25b95009231858c2327020740b3e354533e44bc52dd19`). APK assembly/upload was skipped.
- Failure 1: `supportsPinnedLegadoDesAndTripleDesLegacyHelpers` expected the raw DES ciphertext string's `desBinary.length` property to be positive, but Rhino exposed the Java String wrapper such that the property check evaluated false. Pinned/current `desEncodeToString` intentionally returns `String(ciphertextBytes)`; keep production code unchanged and make the assertion explicitly coerce with `String(desBinary).length > 0`.
- Failure 2: `allowsSymmetricEncodedOutputExpansionAndPreservesInputBounds` used `Base64.getEncoder().encodeToString(ByteArray(2 MiB + 1))`. The all-zero Base64 text consists only of `A` characters, and pinned `String.isHex()` classifies it as hexadecimal before Base64 decoding. The test therefore exercised the upstream Hex path and produced a cipher block-size `CryptoException`, not the expected decoded-size bound. Change only the fixture bytes to a deterministic non-hex Base64 alphabet (for example all `0xFF`, yielding `/` characters), then assert the decoded 2 MiB + 1 input is rejected by the raw ciphertext bound.
- Do not change `String.isHex()`, Hutool Hex/Base64 precedence, DES raw-ciphertext helper semantics, or crypto algorithms; those are pinned upstream behaviors. The test correction must verify the actual intended Base64 branch and the Java String coercion.
- Re-run the focused crypto class and all 266 app unit tests, inspect XML reports, then require a successful exact-head CI and APK artifact before marking the crypto gate passed.


## 2026-11-20 session implementation record and self-review (CI still pending)

### Actual changes
- Re-read `AGENTS.md` in full and reviewed all four mandatory checklists before work; all task scope and test risks were registered before code/test edits.
- `RhinoBoundedSymmetricCryptoAndroid.kt`: symmetric encrypt input remains capped at 2 MiB; encryption result bytes and Base64/Hex output strings are no longer incorrectly capped at 2 MiB. Raw decrypt input/output remains bounded; encoded decrypt input text remains 4 MiB. Stream reads preserve forward-progress and caller ownership.
- `RhinoCryptoBounds.kt`: asymmetric encoded output cap is 4 MiB; raw byte cap remains 2 MiB.
- `app/build.gradle.kts`: added test-only Robolectric 4.14.1 so crypto bridge tests execute Android Base64 shadows without changing pinned production Base64 behavior.
- `RhinoCryptoCompatibilityTest.kt`: moved six Android-Base64-dependent crypto tests under Robolectric; retained String/ByteArray JS overload checks and moved random-key verification to the real Kotlin constructor. Added symmetric output expansion/input-bound tests, including AES/CBC PKCS5 padding expansion and the 4 MiB encoded boundary.
- `RhinoSourceScriptRuntimeTest.kt`: removed the ambiguous Rhino `(String, null, null)` test expression; retained direct random-key verification and updated the 2 MiB padded AES/CBC expectation.
- Test-fixture follow-up in `RhinoCryptoCompatibilityTest.kt`: coerce the Java String wrapper before checking raw DES ciphertext string length; use a deterministic `0xFF` Base64 fixture and assert its decoded size so upstream `isHex()` does not route an all-`A` Base64 string through Hex.

### Relevant commits
- Pre-registration and test-harness amendment are recorded in the preceding entries of all four files.
- Initial runtime/test/build change: `534262df7feb30009bb7d62d4ce1e4617f09e955`.
- CBC padding regression fixture: `6d1b3a946e877d2cce433332ca7ce536482bd3b4`.
- Complete helper call-site rename: `11a2f0c059dabf4a8c116fcd16585803b935e9f1`.
- Latest test fixture correction: `66d43eb7931a3bdabdfe49a2cdfa3f6a4f895173`.

### CI evidence and current gate
- `44568645734` (`534262df`) and `44568939955` (`6d1b3a94`) failed compilation because the helper rename had stale call sites. This was registered and corrected in `11a2f0c0`.
- `44569202532` (`11a2f0c0`) passed pinned upstream verification, contract tests, Rhino artifact verification and source restore. App tests ran: 266 total, 264 passed, 2 failed due to the two test-expression/fixture issues described above. Report artifact `14552993253`, SHA-256 `f85c44da84c3c56c85f25b95009231858c2327020740b3e354533e44bc52dd19`. APK assembly/upload was skipped.
- `44570326349` is a stale docs-head run without the latest test fixture correction. `44570390532` targets the latest code head `66d43eb7` and was pending at the time of this note. This documentation update will trigger a new latest-head run; use that run, not stale runs, for acceptance.
- Do not mark the crypto task complete until latest-head focused/full tests pass and APK build/artifact are inspected. No APK is available or approved for delivery from this session yet.

### Remaining work
1. Inspect the workflow triggered by this documentation commit; verify its exact checked-out HEAD.
2. Inspect focused crypto test results and all app unit-test XML/HTML reports.
3. If tests pass, verify `:app:assembleDebug`, artifact ID, file size and digest; if any test fails, register the exact defect before changing code.
4. Physical-device installation/provider behavior, real-source checks and the broader full-Legado runtime/host migration remain open.

### Checklist self-review
- The registered scope correctly captured the source-proven distinction between raw input limits and uncapped symmetric encryption output, the 4 MiB encoded-output policy, and the need to test Android Base64 in an Android-aware JVM harness.
- The first test pass exposed two additional fixture errors: Java String wrapper property access and a Base64 string consisting only of valid Hex characters. Both are now corrected in the latest test commit without altering upstream behavior.
- The checklist remains accurate; acceptance is still pending latest-head CI and APK artifact validation. No item is marked complete based only on compilation, documentation, or a pending workflow.


## 2026-11-20 Android APK packaging collision — PRE-REGISTERED BEFORE BUILD CONFIG EDIT

### Verified CI evidence
- Code-head run `44570390532` (`66d43eb7931a3bdabdfe49a2cdfa3f6a4f895173`) and latest checklist-head run `44570879134` (`1677bc56baa7a1bf92eb5254c8df5bfb8630ed1d`) both passed:
  - pinned upstream source verification,
  - `:integration-contract:test`,
  - pinned Rhino artifact verification and source restoration,
  - bounded Rhino replacement host tests,
  - app `:testDebugUnitTest`.
- Downloaded and inspected unit-test report artifact `14554936230` from run `44570879134` (archive size 83,462 bytes; artifact digest `sha256:9c394b30fde857fad53fa4d221d9d7be8c74207cca8f8817e5e15f3b84763911`). XML totals: 266 tests, 266 passed, 0 failures, 0 errors, 0 skipped. `RhinoCryptoCompatibilityTest`: 6/6 passed; `RhinoSourceScriptRuntimeTest`: 94/94 passed.
- Both runs fail only at `:app:mergeDebugJavaResource` during `:app:assembleDebug`. AGP reports duplicate resource path `META-INF/INDEX.LIST` from `cn.hutool:hutool-crypto:5.8.22` and `cn.hutool:hutool-core:5.8.22`. No APK artifact was produced; APK upload and live-source steps were skipped.
- This is a Java archive index metadata collision, not duplicate application classes or a runtime crypto failure. The workflow's exact logs were inspected for both code and latest checklist heads.

### Registered correction scope — before changing build configuration
1. Modify only `app/build.gradle.kts` to exclude the duplicate non-runtime archive index resource path `/META-INF/INDEX.LIST` via Android Gradle Plugin's `android.packaging.resources.excludes` DSL.
2. Keep both pinned Hutool dependencies and versions, all classes/resources other than this duplicate archive index metadata, crypto algorithms, Android Base64 behavior, and runtime code unchanged.
3. Do not use broad `META-INF/**` exclusions, dependency removal, dependency substitution, `pickFirst` over application/runtime resources, or relax duplicate-class checks.
4. Re-run exact-head CI: confirm all 266 app tests and contract/host tests pass, `:app:assembleDebug` succeeds, inspect complete logs and APK artifact metadata (ID, size, SHA-256), and check no unrelated resource was excluded.
5. Only after a successful APK artifact can the build gate be marked passed. Physical-device install, provider validation, real-source checks, and complete Legado migration remain separate open acceptance requirements.

### Risk and compatibility audit
- The colliding path is `META-INF/INDEX.LIST`, archive classpath index metadata generated/packaged by JAR tooling; it is not a class, manifest, Android resource, crypto provider registration, or service-loader declaration.
- The exclusion is narrowly scoped to this one exact resource path at APK packaging time. Hutool classes, crypto provider classes, `META-INF/services/**`, manifests, licenses, and all other dependency payload remain included.
- Existing `app/build.gradle.kts` has no packaging customization block; add only the narrow resources exclusion under the existing `android` block. No other module or build logic is in scope.

### Acceptance and self-review
- Focused crypto tests and the full app unit-test suite are already green on run `44570879134`; this correction must not alter those test results.
- Require a successful latest-head workflow and a real uploaded debug APK before recording packaging success.
- This registration identifies the exact failing task, both colliding artifacts, the minimal metadata-only exclusion and the regression gates. If a new failure exposes another resource conflict, amend the ledger before broadening the exclusion or editing additional files.


## Pinned-upstream SHA ledger correction (2026-11-20)

A review of the registered APK packaging task exposed a malformed 39-character abbreviated value in the root `AGENTS.md` and several historical checklist entries. The actual submodule gitlink at this branch and the CI workflow's `expected` value both independently confirm the full, valid 40-character upstream commit SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`. The public LegadoTeam/legado commit endpoint also confirms that exact SHA and its commit metadata. Corrected only malformed 39-character references to the exact 40-character pinned revision; no submodule pointer or upstream source was changed. The pinned-source verification step had passed on both recent CI runs. This is a documentation integrity correction, not a change to the registered packaging-fix scope.


## 2026-11-20 APK packaging follow-up — Bouncy Castle license resource collision (PRE-REGISTERED BEFORE FURTHER BUILD EDIT)

- Run `44604734584` on code head `4a6e57f61fa9b9eddf7d1aec690589d36cb2af3a` passed pinned-upstream verification, contract tests, Rhino host tests, and all 266 app unit tests (266 passed; 0 failures/errors/skips). The test report is artifact `14568848137`, size 83,370 bytes, digest `sha256:fdfaeae8e01b2ac8f8754d3fd8494920d67794b2cabb21d28343011f07430041`.
- The narrow `/META-INF/INDEX.LIST` exclusion removed the previous Hutool conflict. APK assembly then exposed the next collision: `META-INF/LICENSE.md` from `bcpkix-jdk18on:1.86`, `bcutil-jdk18on:1.86`, and `bcprov-jdk18on:1.86`. The build still fails at `:app:mergeDebugJavaResource`; no APK artifact exists.
- Registered follow-up: inspect the exact contents and provenance of all three Bouncy Castle `LICENSE.md` entries before changing packaging. Preserve the complete applicable license notice(s) in the APK or an equivalent packaged legal-notices location. If all three are byte-identical, a narrow `pickFirsts += "/META-INF/LICENSE.md"` may preserve the shared notice. If they differ, do not silently pick one or exclude them; retain each distinct notice under a deterministic non-colliding path or another verifiable packaging solution.
- Do not add broad `META-INF/**` excludes, remove/replace Bouncy Castle modules, weaken duplicate-class checks, or drop license content. Keep the existing Hutool `INDEX.LIST` exclusion and crypto dependencies unchanged.
- Acceptance: verify license bytes/checksums and legal coverage, update only the narrowly necessary packaging configuration (and any generated license-resource handling if required), rerun all 266 tests plus contract/Rhino tests and `:app:assembleDebug`, inspect the final APK for retained license notices and required crypto provider classes, and verify the exact-head APK artifact ID/size/SHA-256.
- This amendment follows the registered rule to amend the ledger before broadening a packaging fix when a new conflict appears. Physical-device install, real-source checks and complete Legado migration remain open.


## 2026-11-20 Bouncy Castle packaging correction — exact-head verification record

- Code commit: `cc78171c7233db7a452606121010fca65681167a`; exact-head workflow run [44605770188](https://github.com/xkszds/badcat-legado-client/actions/runs/44605770188) checks out that SHA on `integration/legado-upstream-runtime`. The branch ref was verified to point to the same commit before this record.
- The narrowly scoped `META-INF/LICENSE.md` `pickFirst` resolved the Bouncy Castle resource collision. In the exact-head job, pinned-upstream verification, contract tests, Rhino artifact verification/source restore, bounded Rhino host tests, unit-report upload, `:app:assembleDebug`, and APK upload all report success. The run is still `in_progress` because the optional four-source uploaded-corpus smoke step has not completed; its report upload and post-job cleanup are pending. Do not record the overall workflow as completed.
- Downloaded and inspected unit-test artifact `14570890675` (83,576 bytes; ZIP SHA-256 `964dce365cf664e7ff48e1bc46584cb2e9679dcc33a6ff9d45c96fc08515b996`). All 31 JUnit XML files total 266 tests, 266 passed, 0 failures, 0 errors, 0 skipped; `RhinoCryptoCompatibilityTest` is 6/6 and `RhinoSourceScriptRuntimeTest` is 94/94.
- Debug APK artifact `14568734307` (ZIP 13,840,284 bytes; artifact SHA-256 `c03d78a09a40f6c23c6acccdaf87089a514df4e95d535962335a30ddc8fc45a1`) contains `app-debug.apk` (18,163,811 bytes; SHA-256 `9c8588f72dbadf0c767c0cea00227c33775a01363034b8754ba75cc1f4e71dbc`). APK inspection confirms `META-INF/LICENSE.md` is retained (1,171 bytes), `META-INF/INDEX.LIST` is absent as intentionally excluded archive metadata, and Bouncy Castle provider, AES/RSA engine and RSA signer symbols remain in DEX.
- Live smoke evidence already uploaded for this run: Pinshuzhai end-to-end search/detail/TOC/content passed (artifact `14570508163`); Bixiange GBK POST search/detail/TOC AJAX page/content passed (artifact `14571255565`). The workflow also reports success for Nyasama endpoint smoke, public source repository probes and Feisuzhong end-to-end reading. The separate four-source uploaded-corpus sample is still running; await its result/report before closing the workflow record.
- Not verified: physical Android-device installation/provider behavior; complete pinned Legado runtime/host migration; final full five-category parity. This CI APK is a debug build artifact, not the final complete-Legado release.
- Self-review: the pre-registered collision scope correctly limited the build change to the shared, source-verified Bouncy Castle license file and retained the full legal notice; the APK inspection confirms that notice and required crypto symbols remain. Acceptance remains explicitly open for the still-running uploaded-corpus smoke, device validation and full runtime migration.


## 2026-11-20 final exact-code-head CI and uploaded-corpus result — supersedes the pending note above

- Workflow run [44605770188](https://github.com/xkszds/badcat-legado-client/actions/runs/44605770188) has now **completed successfully** for code commit `cc78171c7233db7a452606121010fca65681167a` on `integration/legado-upstream-runtime`. The job and workflow conclusion are both `success`; all job steps, including post-job cleanup and the four-source report upload, are completed. The earlier statement that the run was still in progress is superseded.
- Exact-head verification passed pinned-upstream SHA verification, `:integration-contract:test`, Rhino artifact verification/source restoration, bounded Rhino host tests, `:app:testDebugUnitTest`, `:app:assembleDebug`, and debug APK upload.
- Unit-test artifact `14570890675` was downloaded and inspected: 31 JUnit XML files, 266 tests, 266 passed, 0 failures, 0 errors, 0 skipped. `RhinoCryptoCompatibilityTest` passed 6/6 and `RhinoSourceScriptRuntimeTest` passed 94/94. The XML/HTML test report bundle is 83,576 bytes; SHA-256 `964dce365cf664e7ff48e1bc46584cb2e9679dcc33a6ff9d45c96fc08515b996`.
- Debug APK artifact `14568734307` is a ZIP of 13,840,284 bytes (SHA-256 `c03d78a09a40f6c23c6acccdaf87089a514df4e95d535962335a30ddc8fc45a1`). Its `app-debug.apk` is 18,163,811 bytes (SHA-256 `9c8588f72dbadf0c767c0cea00227c33775a01363034b8754ba75cc1f4e71dbc`). Re-inspection confirms the 1,171-byte `META-INF/LICENSE.md` remains in the APK, `META-INF/INDEX.LIST` is absent, and Bouncy Castle provider, AES, RSA and signer symbols remain in DEX.
- End-to-end live source results are mixed and must not be generalized: Pinshuzhai passed search → detail → 1,650-chapter TOC → first chapter content (report artifact `14570508163`); Bixiange passed GBK POST search → detail → AJAX TOC pagination → chapter content (report artifact `14571255565`). In the separate four-source uploaded-corpus sample, only 精华书阁 completed search → detail → 20 TOC entries → chapter content; 唐三中文 and 零零小说 timed out at search, while 笔趣阁fun returned HTTP 404. The report is artifact `14569199256` (981 bytes; SHA-256 `b3eb93414fa661ddbe312c8142822e01c7fd9852c0ab4eb2e428d8567c0d8084`). Thus the sample result is 1/4, not 4/4.
- Other best-effort probes are likewise partial: the Nyasama sample returned usable results for 3/5 probes; two search endpoints returned empty results. Public-source probing encountered upstream HTTP 403/502, timeouts, a 404 and a redirect loop; several other source pages returned parseable candidate rows, but these probes are not full reading-flow validation. Treat these as source/network or source-data observations until individually reproduced; do not count them as engine passes.
- Not verified: installation or runtime behavior on a physical Android device; complete pinned Legado runtime/host integration; broad source-corpus compatibility; complete Badcat five-category parity; signed release build. This is a verified CI Debug APK only, not the requested final complete-Legado release.
- Self-review: the earlier pending status is now corrected with the final run conclusion. Build/test gates are recorded as passed only for this exact code head; live-source evidence is separated by source and stage, and external failures are not mislabeled as engine failures. Full runtime, device and product-parity gates remain open.


## 2026-11-20 next task pre-registration — complete JsExtensions contract matrix and BaseSource service boundary (AUDIT ONLY)

Before any further runtime code change, complete a source-level audit of the pinned JsExtensions/JsEncodeUtils contracts and the real BaseSource lifecycle dependencies. This is an audit task only; do not edit runtime/product code during this task.

- Upstream source of truth: upstream/legado/app/src/main/java/io/legado/app/help/JsExtensions.kt, JsEncodeUtils.kt, BaseSource.kt, and every directly referenced service/type at pinned revision 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Host targets: RhinoSafeSourceJavaExtensions.kt, RhinoSourceScriptRuntime.kt, RhinoSourceHttpResponse, RhinoSourceFileHandle, source descriptor creation/injection, batch/cache adapters, and all related app/integration-contract tests and callers.
- Produce a declaration-by-declaration matrix for all upstream methods and overloads: parameter types/defaults/nullability, return type and Rhino-visible object shape, exceptions, blocking/suspend/thread/coroutine behavior, lifecycle/context requirements, source identity/isolation, and the corresponding host implementation/callers/tests. Include inherited JsEncodeUtils declarations and distinguish true parity, deliberate security restriction, unsupported UI capability, and missing implementation.
- Trace BaseSource construction/inheritance and every service it requires (source model, Room/Gson converters, cookie/login state, script storage, cache, HTTP, browser/WebView, navigation context, refresh/verification/video callbacks, batch persistence). Do not invent null/fake services or no-op methods.
- Explicitly resolve the getSource(): BaseSource? versus host descriptor-map collision, BatchContentContext?/cacheContent persistence gap, response-wrapper ABI differences, and sandboxed file-handle difference. Search all static callers and Rhino script/test call sites before proposing any signature change.
- Compatibility/security risks: JVM descriptor and overload collisions; Rhino overload resolution and Java reflection; stable mutable source identity; per-source state leakage; coroutine cancellation/threading; HTTP body ownership/redirect semantics; Android UI lifecycle; filesystem sandbox; batch chapter-index/save-token identity.
- Acceptance: every pinned declaration is accounted for; exact upstream source/call-site references and host files are cited in the audit; no unexplained same-name method is assumed compatible; all existing host callers/tests are listed; each missing capability has a real host service requirement or explicit context-specific unsupported behavior; produce a bounded next implementation slice with regression tests and acceptance criteria.
- This registration is not evidence of implementation or compatibility. After the audit, update all four checklists with findings and pre-register a separate implementation task before touching runtime code.

## 2026-11-20 audit result — JsExtensions and BaseSource host boundary (audit complete; implementation open)

Audit was performed against pinned upstream commit 330e606c4d57e08adc056ff8ab7a4132c784da9a and host branch integration/legado-upstream-runtime at 32e0537048b1c82714e982c56fe6998ce6117703. No runtime/product code was changed in this audit.

- Inventory: pinned JsExtensions contains 122 function declarations (118 public declarations across 77 names; the remainder are internal/private helpers). JsEncodeUtils contains 30 public declarations across 27 names. The current RhinoSafeSourceJavaExtensions main bridge exposes 150 public declarations across 110 names. All 30 inherited JsEncodeUtils declaration slots and all 27 inherited names are present in the host declaration scan; this is declaration coverage, not proof of behavior parity.
- 17 public JsExtensions names (30 overload declarations) are absent: refreshBookInfo, refreshBookToc, refreshContent, webView, webViewGetSource, webViewGetOverrideUrl, openVideoPlayer, startBrowser, startBrowserAwait, getVerificationCode, openUrl, showBrowser, getReadBookConfig, getReadBookConfigMap, getThemeMode, getThemeConfig, getThemeConfigMap.
- Critical ABI mismatch: upstream java.getSource() returns BaseSource?; host java.getSource() returns a read-only Map descriptor. The current test explicitly expects the descriptor map, including nested and future JSON metadata. Preserve that useful descriptor under a separately named accessor when the source object contract is migrated; do not silently change the return type.
- Batch contract is not integrated: host getBatchContext() always returns null and cacheContent() always throws. Upstream BatchContentContext resolves chapter identity by chapter index, rejects ambiguous duplicate-URL strings, applies content replacement, and saves with BookHelp ContentSaveToken/version fencing. JsSourceEngine installs the context only during getContentBatch and falls back for chapters not saved. The host SourceChapterContentService has no corresponding batch path.
- Source object mismatch: the runtime binds source to RhinoSourceLoginExtensions, not a BaseSource-compatible object, and does not bind sourceApi. The helper covers key/variable/login-state methods only; source.get/put, getTag, dynamic header rules, login UI v1/v2 methods, login(), refreshExplore, refreshJSLib, putConcurrent and evalJS are not supplied by this object.
- Auth boundary finding: SourceLoginSessionRequestClient currently merges a source's saved login headers into every request with that sourceId. Pinned AnalyzeUrl only attaches login headers when the target URL is on the source's same site. This can disclose Authorization/custom login headers to third-party CDN or cross-origin targets. Also, host putLoginHeader does not synchronize Cookie into the cookie jar, removeLoginHeader does not clear the cookie jar, and host loginInfo is stored as plaintext while upstream AES-encrypts it using the first 16 bytes of androidId. Treat this as the next security-sensitive implementation slice.
- Targeted caller scan covered all 24 app runtime Kotlin files and all 20 app unit-test Kotlin files. The java.getSource descriptor call is currently exercised by RhinoSourceScriptRuntimeTest; batch default/failure behavior is exercised only by RhinoSafeSourceJavaExtensionsCompatTest; source login helpers are exercised by RhinoSourceScriptRuntimeTest and SourceLoginSessionTest. No other static Kotlin production caller of these bridge methods was found in that scan.
- CI run 44610074419 completed successfully on this documentation-registration head. This confirms the existing bounded tests/build workflow only; it does not close BaseSource, batch, UI/WebView, source-model, device-installation or full-runtime gaps.

### Next implementation task pre-registered (not started): source login/session parity and credential boundary

Before changing code, preserve the pinned BaseSource contract and implement only this bounded slice across RhinoSourceLoginExtensions, SourceLoginSessionRequestClient, SharedPreferencesSourceLoginSessionStore, SharedPreferencesSourceCookieJar and BadcatReadingEngineFactory:

1. Scope login headers to the same source site using pinned AnalyzeUrl domain semantics; never attach Authorization or custom login headers to unrelated hosts/CDNs.
2. Make putLoginHeader synchronize a Cookie value with the source cookie jar, and make removeLoginHeader clear the associated cookie state without affecting other sources.
3. Store loginInfo using the pinned AES-compatible androidId-derived key; define and test a safe migration for existing plaintext host records before switching reads/writes.
4. Preserve source isolation, cancellation behavior, header validation, secret redaction and current public scripts; align return/nullability semantics with pinned BaseSource where callers permit.
5. Add regression tests for same-site/subdomain/cross-origin/CDN requests, Cookie merge/removal, persisted encryption/migration, malformed data and two-source isolation.

Acceptance: no credential is sent cross-origin; cookie state and login-header state stay synchronized and source-isolated; persisted loginInfo is not plaintext; legacy stored data has an explicit tested migration; all focused tests and full CI pass. This task does not claim full BaseSource parity and does not authorize changing the java.getSource descriptor ABI or implementing UI/WebView/batch APIs. Those remain separately open.

### Authentication lifecycle clarification

The targeted host lifecycle audit also found that RhinoSourceScriptRuntime.clearSourceState clears source scopes, script key/value state, file cache and login-session state, but it has no source-cookie clear callback. SharedPreferencesSourceCookieJar has a clear operation, yet it is not invoked by this lifecycle path. The next login/session implementation must cover source removal/reset as well as removeLoginHeader, and prove that clearing one source cannot clear another source's cookies.

## 2026-11-20 implementation progress — login-header same-site boundary

Implemented the first bounded sub-slice in `SourceLoginSession.kt`: saved source login headers are attached only when the source key and initial request URL resolve to the same effective-TLD+1 domain, matching pinned `NetworkUtils.getSubDomainOrNull` and `AnalyzeUrl.isLoginHeaderSite`. IP literals are compared directly. As upstream does, malformed/unresolvable URLs fail open. Automatic redirect-chain stripping is not claimed; pinned AnalyzeUrl evaluates the initial URL.

Regression coverage added for a same-site subdomain, an unrelated CDN host, and invalid-source-key fallback. This does not complete the registered task: Cookie synchronization/cleanup and AES loginInfo encryption plus legacy migration remain open. No release or full Legado parity claim.


## 2026-11-20 source login/session implementation progress — verification pending

The previously registered BaseSource login/session slice now has implementation changes on `integration/legado-upstream-runtime`:

- `SourceLoginSessionRequestClient` retains the same-site/effective-TLD+1 login-header boundary from commit `c910ca461d26175125fcd02918b8680012c07037`.
- `RhinoSourceLoginExtensions.putLoginHeader` now forwards a supplied Cookie request-header value to the source-scoped cookie adapter before persisting the login header. `removeLoginHeader` removes the saved header and asks the adapter to remove cookies for that source's registrable domain.
- `SharedPreferencesSourceCookieJar` parses raw `Cookie: name=value; ...` request-header pairs as host-only cookies on the source host; replacing login cookies removes cookies for the source registrable domain and its subdomains while preserving unrelated CDN-domain cookies. Source reset is wired to clear only that source's jar.
- Persisted `loginInfo` now uses the pinned upstream `BaseSource.kt` algorithm: first 16 Android-ID bytes as key and `SymmetricCryptoAndroid("AES", key).encryptBase64(info)`. Existing JSON plaintext is returned and migrated to ciphertext on read when the Android ID is available. Missing keys preserve plaintext; malformed ciphertext returns null without deleting the stored value.
- Added regression coverage for Cookie header parsing, cookie-domain boundaries, login-header cookie writer/remover callbacks, source-reset cookie cleanup, AES round-trip, idempotent plaintext migration, missing key and malformed ciphertext.

Pinned upstream evidence inspected: `BaseSource.kt` at `330e606c4d57e08adc056ff8ab7a4132c784da9a` calls `CookieStore.replaceCookie(getKey(), cookie)`, `CookieStore.removeCookie(getKey())`, and uses `AppConst.androidId.encodeToByteArray(0, 16)` with `SymmetricCryptoAndroid("AES", key)`. `AppConst.androidId` reads Android's `ANDROID_ID` setting (fallback string `"null"`).

Verification status at this documentation update: exact-head CI for code head `c451b0c304703c2ffa84d855b8aa2d3a44a36ad1` is pending (run `44619334579`). Earlier per-commit workflow runs were cancelled by subsequent pushes and are not counted as passing. Do not mark this slice verified until the exact final head completes focused tests, full unit tests and Debug APK assembly and its logs are inspected.

Open review items: verify the source key passed by the runtime is always the imported source URL for every caller; exercise durable SharedPreferences cookie replace/remove across separately constructed jar instances; compare generated AES ciphertext/decryption against a pinned upstream fixture; verify migration with real Android preferences/device; inspect exact-head CI. Same-site login-header filtering checks the initial URL as upstream does; it does not claim redirect-chain header stripping. This increment does not complete BaseSource/JsExtensions, full Legado runtime, physical-device validation or Badcat five-category parity.


## 2026-11-20 review correction and current exact-head status

A final diff review caught an accidental self-recursive implementation in the new source-cookie-domain predicate. It was corrected in code commit `4fe83640cbaee4bbbb86875998838dd031ec1457`; the helper now compares the cookie domain to the source registrable domain or one of its subdomains. This correction is recorded explicitly rather than treating the earlier intermediate commit as valid.

Current code head: `4fe83640cbaee4bbbb86875998838dd031ec1457`. Exact-head workflow run `44619521355` is pending. The earlier run `44619447738` (documentation head `e19fa8ab197e4cf6a56f48b42e50f80b3dbbad08`) was cancelled by the subsequent code correction and is not a test result. No test/build success is claimed yet. Review the exact-head job logs before accepting this increment.


## 2026-11-20 exact-head CI failure and correction

The exact-code-head workflow `44619521355` for `4fe83640cbaee4bbbb86875998838dd031ec1457` failed during `:app:compileDebugKotlin`: the default lambda for `sourceLoginCookieRemover` had three parameters while its declared callback accepts two. The contract tests and pinned Rhino artifact verification had passed; the Android host test task failed at compilation, so no host test suite or APK result is claimed for that run.

Corrected the callback arity in code commit `4b6c485aa6934520dbc0665b6de6b69a85454608`. The previous failure is retained as a failure record, not overwritten. A new exact-head workflow must compile and run the full relevant tests before this implementation slice can be considered verified.

## 2026-11-20 pre-registered correction task — source login/session test failures (before further runtime edits)

Latest exact-head workflow 44620143558 checked out documentation head 6a4e0c727a7ddc648975c0501f8026bba3ff8d3f and completed with failure. Its :integration-contract:test and pinned Rhino artifact verification passed, but :app:testDebugUnitTest reported 276 tests with 3 failures; APK assembly was skipped. Uploaded report artifact: 14575969615.

### Confirmed failure evidence

1. SourceLoginSessionTest.loginInfoCipherMatchesPinnedAndroidIdAesRoundTripAndRejectsBadKeyOrCiphertext fails because pinned SymmetricCryptoAndroid.encryptBase64 calls android.util.Base64.encodeToString, which is not mocked in the plain Android JVM test runner. This is a test/runtime-boundary issue, not evidence that AES itself failed.
2. SourceLoginSessionTest.legacyPlainLoginInfoMigratesNonDestructivelyAndOnlyOnce fails because the same Android Base64 call throws and is caught by the migration's non-destructive runCatching. The plaintext remains unchanged, so the ciphertext-persisted assertion fails.
3. SourceLoginSessionTest.loginHeadersAreOnlyAttachedToThePinnedEffectiveTldPlusOneSite fails because an unrelated CDN request still receives login headers. The assertion at the CDN check fails. PublicSuffixDatabase resolution is falling through its fail-open path in this JVM test environment or otherwise failing to distinguish these valid registrable domains. Do not change upstream fail-open semantics for invalid/unresolvable URLs; diagnose the public-suffix data/resource loading and preserve the pinned NetworkUtils.getSubDomainOrNull contract.

### Source evidence inspected

Pinned SymmetricCryptoAndroid.kt at 330e606c4d57e08adc056ff8ab7a4132c784da9a implements encryptBase64(data) as EncoderUtils.base64Encode(encrypt(data)); pinned EncoderUtils.base64Encode(bytes) uses Android Base64.NO_WRAP. The host can use the same SymmetricCryptoAndroid.encrypt(plainText) ciphertext bytes and Java's basic padded Base64 encoder/decoder only if a byte-for-byte fixture proves equivalence with Android NO_WRAP; alternatively configure an Android-aware test runner. Decryption should feed decoded bytes to the upstream primitive's byte-array decrypt overload. Do not substitute a different AES mode, key, padding or plaintext encoding.

Pinned NetworkUtils.getSubDomainOrNull uses getBaseUrl, java.net.URL, isIPAddress, and PublicSuffixDatabase.get().getEffectiveTldPlusOne(host); any failure returns null. Host helper currently uses OkHttp HttpUrl parsing plus the same PublicSuffixDatabase and also returns null on PSL exceptions. Inspect dependency/resource loading and compare behavior with upstream before deciding whether to use the upstream helper directly or fix the missing test resource. Keep the invalid-URL fail-open behavior only where upstream does so.

A related production wiring review found SharedPreferencesSourceLoginSessionStore obtains ANDROID_ID using Settings.System.getString(..., Settings.Secure.ANDROID_ID); correct API is Settings.Secure.getString, matching Android's secure setting and the factory's existing provider. Verify fallback behavior against pinned AppConst.androidId before editing.

### Registered correction scope

- Inspect exact current implementations, source-key identity flow from BookSource.id through SourceRequestFactory into SourceHttpRequest.sourceId, all call sites, and the current tests before editing.
- Prove AES output parity with the pinned primitive and Android Base64.NO_WRAP; fix the JVM test boundary without changing crypto semantics. Add a deterministic known ciphertext fixture and retain round-trip, malformed ciphertext, missing key and plaintext-migration tests.
- Diagnose and fix the public-suffix lookup/resource problem so valid unrelated hosts do not receive saved login headers; retain same-site/subdomain behavior and upstream invalid URL fail-open behavior. Add explicit IPv4/IPv6 and multi-level suffix cases where supported by pinned upstream semantics.
- Correct the Android ID Settings API only after confirming pinned AppConst.androidId source and add a testable provider/fallback boundary.
- Preserve source isolation, cookie behavior, request cancellation, validation and secret redaction. No broad BaseSource ABI, WebView/UI or batch API changes.

### Acceptance gates

- Focused login/session tests pass, including the three previously failing cases.
- Existing login header merge/source isolation and Cookie jar parse/replace/remove/reset tests pass.
- Full :app:testDebugUnitTest, :integration-contract:test, pinned Rhino artifact verification and :app:assembleDebug pass on the exact final code head.
- Inspect JUnit XML/HTML and APK artifact; no success claim from queued or stale workflows.
- Re-audit the entire diff and source identity path; update all four checklists with actual outcomes and unresolved device/source validation.
- Physical Android device behavior, full Legado runtime and Badcat five-category parity remain separate open gates.

Self-review: this task corrects the three observed CI failures plus the directly related Android ID setting lookup. It does not broaden the previously registered login/session feature scope and does not authorize marking the full login/session or Legado runtime complete.


## 2026-11-20 checklist correction — Android ID setting namespace review

Correction to the pre-registered CI-failure note above: pinned AppConst.kt at revision 330e606c4d57e08adc056ff8ab7a4132c784da9a explicitly reads Android ID with Settings.System.getString(appCtx.contentResolver, Settings.Secure.ANDROID_ID) ?: "null". The host SharedPreferencesSourceLoginSessionStore uses the same Settings.System API and fallback, so this is not a production mismatch and must not be changed to Settings.Secure.getString. The separate BadcatReadingEngineFactory provider is not the loginInfo persistence provider and is not evidence to alter the pinned BaseSource contract. Remove the proposed Settings API correction from scope; preserve the pinned AppConst behavior exactly. This correction was made before any runtime code edit.

## 2026-11-20 preregistered correction: exact-head CI run 44621698553 for code head c2e1d0821a473fe93335c7fcdec15b2bdd0b9cb3 failed at :app:compileDebugKotlin because SourceLoginSession.kt imports io.legado.app.utils.NetworkUtils, which is not in the app module compile classpath. The pinned NetworkUtils.kt is an app-wide upstream source file; integration-contract compiles only selected parser sources and legado-rhino does not package this utility. AnalyzeUrl.kt is not integrated as a whole either.

Before further runtime edits, replace the unavailable import with a narrow host-side domain adapter matching the exact pinned NetworkUtils.getSubDomainOrNull contract used by AnalyzeUrl.isLoginHeaderSite: HTTP/HTTPS base URL validation, java.net.URL host parsing, upstream-equivalent IPv4/IPv6 handling, OkHttp PublicSuffixDatabase.getEffectiveTldPlusOne(host), null on parse/PSL exception, and the pinned host fallback where a valid host has no eTLD+1. Do not import the whole upstream app utility or its broad dependencies; do not invent a suffix list or change fail-open semantics. Audit all helper callers and preserve source-id identity.

Required tests: same host, same registrable-domain subdomain, unrelated CDN, multi-label suffix such as example.co.uk, IPv4/IPv6 as pinned upstream supports, malformed source/target URLs, and PSL resource failure. Keep Robolectric Android Base64 tests. Acceptance: focused login/session tests, full app unit tests, contract tests, pinned Rhino artifact verification, exact-head Debug APK assembly, and inspection of JUnit reports/APK artifact. No physical-device or full Legado completion claim. This corrects only the registered login/session module-boundary mistake; it does not authorize BaseSource ABI, batch, UI/WebView, or java.getSource changes.

## 2026-11-20 preregistered correction — OkHttp 5.4 Android public-suffix context initialization: exact-code-head run 44622851429, attempt 2, passed :integration-contract:test and pinned Rhino artifact verification, but :app:testDebugUnitTest reported 276 tests with one failure in SourceLoginSessionTest.loginHeadersAreOnlyAttachedToThePinnedEffectiveTldPlusOneSite: expected example.co.uk, got null. The helper catches the exception and returns null as pinned NetworkUtils does, so this is a PSL loader initialization issue, not a reason to change the upstream fail-open contract.

Pinned OkHttp 5.4.0 source was inspected: PublicSuffixList.android.kt selects AssetPublicSuffixList; AssetPublicSuffixList.listSource reads PlatformRegistry.applicationContext.assets and throws when the context is unset. OkHttp.android.kt exposes okhttp3.OkHttp.initialize(applicationContext), and PlatformInitializer normally supplies context through AndroidX Startup. The host test uses @Config(manifest = Config.NONE), which intentionally suppresses the merged manifest Startup provider. Thus the Robolectric test has no OkHttp application context and cannot load PublicSuffixDatabase.list.

Before further code edits, register the narrowly scoped test-harness correction: initialize OkHttp with RuntimeEnvironment.getApplication() before any PublicSuffixDatabase call in SourceLoginSessionTest (for example, @Before). Keep @Config(manifest = Config.NONE) unless there is a concrete reason to change it. Inspect the merged production manifest to verify the upstream AndroidX Startup initializer remains present; do not add redundant production initialization unless that inspection shows the initializer is absent. Do not vendor or hard-code the public suffix list, alter the domain algorithm, fail closed/open differently, change OkHttp version, or change the upstream helper. Acceptance: the domain tests resolve example.co.uk and third-party-cdn.net; same-site subdomain, CDN exclusion, IPv4/IPv6, invalid URL fail-open, AES and Cookie tests pass; full 276+ app tests, contract suite, pinned Rhino verification and Debug APK assembly pass on exact code head. Inspect reports and merged manifest. Device validation remains open.

## 2026-11-20 preregistered follow-up — run 44623818154 (head 5763af1e6aa3af9b13809553c800be65f748e712) confirmed :integration-contract:test and pinned Rhino artifact verification pass, while :app:testDebugUnitTest has 276 tests with only the PSL/eTLD+1 test failing at its first example.co.uk assertion. The @Before OkHttp.initialize call alone did not resolve the lookup.

Further pinned OkHttp 5.4.0 source inspection: PublicSuffixDatabase holds a singleton BasePublicSuffixList whose first load attempt is cached permanently, including IOException; Platform.resetForTests() both reselects the runtime Platform and resets PublicSuffixDatabase's singleton. OkHttp.initialize() only sets applicationContext when the current Platform implements ContextAwarePlatform. Therefore the Robolectric setup must reset Platform/PublicSuffixDatabase after Robolectric has established the Android SDK context, then call OkHttp.initialize(RuntimeEnvironment.getApplication()) before any suffix lookup. This specifically addresses stale platform selection or a cached first-load failure; do not change production domain matching or catch/fallback semantics.

The pinned OkHttp Android AAR manifest contributes androidx.startup.InitializationProvider metadata for okhttp3.internal.platform.PlatformInitializer, which assigns PlatformRegistry.applicationContext. The app's source manifest does not show this dependency-merged provider; verify the merged release/debug manifest after a successful assembly. Config.NONE intentionally removes manifest startup in this unit test, so explicit test initialization remains required.

Before further code edits, register this exact test harness correction: call okhttp3.internal.platform.Platform.resetForTests() before OkHttp.initialize(RuntimeEnvironment.getApplication()) in the Robolectric @Before setup. Keep the existing assertions. Re-run the exact-head full suite and inspect JUnit output. If still failing, capture the underlying IOException/cause from PublicSuffixDatabase rather than adding a suffix list, hardcoding domains, changing OkHttp, or altering pinned upstream fail-open behavior. Acceptance: example.co.uk and unrelated CDN eTLD+1 resolve; same-site/subdomain and cross-site header behavior, IPv4/IPv6 and malformed URL cases pass; all app tests, contract tests, pinned Rhino checks, APK assembly and merged manifest inspection pass. Physical device validation remains open.


## 2026-11-20 login-session PSL test diagnosis — diagnostic-only commit

- Re-read the mandatory project instructions and all four checklists on the active `integration/legado-upstream-runtime` branch before continuing. Pinned Legado revision remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- The exact-code-head CI run `44625634547` for `2f1a921a411665f331e42a3d215baa275fe352b9` completed with one failure in `SourceLoginSessionTest.loginHeadersAreOnlyAttachedToThePinnedEffectiveTldPlusOneSite`; 275 of 276 app unit tests passed. Contract tests and pinned Rhino artifact verification passed; APK assembly was skipped.
- Test report artifact `14579800870` confirms the first failed assertion is the effective-TLD+1 lookup for `www.example.co.uk`, which returned null. AES round-trip/migration, Cookie callback, source isolation, validation and other login-session tests passed in that run.
- Diagnostic-only test commit: `ab0150a2cf5ce070ec2ef8d322f5bd2edf6475cb`. It adds a direct OkHttp `PublicSuffixDatabase.getEffectiveTldPlusOne` assertion that preserves the underlying exception as the AssertionError cause. No production code, PSL behavior, URL parsing, fail-open semantics or pinned dependency was changed.
- CI run `44626750474` did not reach tests: Gradle failed resolving `org.jetbrains.kotlin.plugin.parcelize:2.4.20` from configured plugin repositories. This is recorded as dependency-resolution/build infrastructure failure, not as a test result. A rerun of the failed job was requested; its result is pending.
- Current acceptance remains open: obtain the direct PSL failure cause; resolve any demonstrated test asset/context issue without hardcoded suffixes or production semantic changes; pass exact-head app tests, contract tests, pinned Rhino verification and Debug APK assembly; inspect merged manifest and reports. Physical-device validation, full Legado runtime and five-category parity remain separate open gates.
- Self-review: this diagnostic increment stays within the preregistered PSL test-harness correction. It does not establish the login/session slice or broader runtime as complete.


## 2026-11-20 preregistered correction — Robolectric OkHttp Android PSL asset is absent

### Evidence from exact-head CI

- Diagnostic code head `ab0150a2cf5ce070ec2ef8d322f5bd2edf6475cb`, run `44626750474`, failed only `SourceLoginSessionTest.loginHeadersAreOnlyAttachedToThePinnedEffectiveTldPlusOneSite`; 276 tests ran, one failed. The direct PSL assertion preserved the root cause:
  - `IllegalStateException: Unable to load PublicSuffixDatabase.list resource.`
  - Caused by `java.io.FileNotFoundException: PublicSuffixDatabase.list`
  - Origin: OkHttp 5.4.0 `AssetPublicSuffixList.listSource` calling Robolectric `AssetManager.open("PublicSuffixDatabase.list")`.
- This confirms `Platform.resetForTests()` and `OkHttp.initialize(RuntimeEnvironment.getApplication())` correctly reach the Android asset loader but the unit-test sandbox does not contain the AAR-provided asset. It is not a PSL algorithm mismatch or a production domain fallback issue.
- Exact branch head at registration: `bf6bab98963b90eb5e4842031e7f448e843df2a6`; pinned Legado SHA remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.

### Registered test-harness-only correction

- Inspect OkHttp 5.4.0 published Android and JVM artifacts/Gradle module metadata and the Android AAR asset packaging. Keep production `implementation("com.squareup.okhttp3:okhttp:5.4.0")` unchanged.
- Make the Robolectric unit-test runtime consume the matching `com.squareup.okhttp3:okhttp-jvm:5.4.0` implementation (whose PSL loader reads the JVM classpath resource) instead of the Android AAR asset loader, using a narrowly scoped test-runtime dependency substitution/exclusion only if the published metadata confirms API/binary compatibility. Keep test compile API compatible; remove Android-only test initialization only if the JVM test runtime no longer exposes it. Do not let both duplicate OkHttp implementations coexist at test runtime.
- If the JVM artifact cannot be substituted cleanly, instead configure Robolectric to consume the exact dependency-provided Android AAR asset from the generated merged-assets output. Do not copy/vendor the PSL file, download a separate list, synthesize suffixes, or hard-code domains.
- Audit all usages of OkHttp internal test-only APIs and all app test runtime dependencies; production remains on Android AAR and must retain its AndroidX Startup initialization provider and `PublicSuffixDatabase.list` asset.
- Keep production `legadoSourceDomain`, `shouldAttachSourceLoginHeader`, pinned upstream fail-open behavior and login-header security semantics unchanged.

### Required regressions and acceptance

- Verify the selected test runtime resolves one OkHttp implementation only and loads its official 5.4.0 PSL resource successfully.
- Retain eTLD+1 checks for `www.example.co.uk`, `cdn.example.co.uk`, unrelated `third-party-cdn.net`, IPv4, IPv6, malformed URL and upstream invalid-source fail-open behavior.
- Run focused login/session tests, full `:app:testDebugUnitTest`, `:integration-contract:test`, pinned Rhino artifact verification and `:app:assembleDebug` on the exact final code head.
- Inspect JUnit XML/HTML, test runtime dependency graph, assembled APK contents for Android `assets/PublicSuffixDatabase.list`, and merged manifest for AndroidX Startup `PlatformInitializer`.
- No APK delivery until CI artifacts are verified; physical-device install, complete Legado runtime and Badcat five-category parity remain separate gates.

Self-review: this correction is strictly test-runtime resource wiring. It does not modify production PSL data/logic, the pinned upstream revision, OkHttp production version, cookie behavior, login state, or broaden the login/session feature scope.


## 2026-11-20 preregistered correction — Gradle test-runtime configuration is created lazily

- Exact code head `eb32dcc5b1b98a7297759126315c09473d035a3d`, Actions run `44628470076`: workflow stopped before tests because `configurations.named("debugUnitTestRuntimeClasspath")` was evaluated before Android Gradle Plugin had created that variant configuration. Error: `Configuration with name 'debugUnitTestRuntimeClasspath' not found.`
- This is a build-script configuration timing defect; no unit tests ran, no APK was assembled, and no conclusions can be drawn about the OkHttp JVM PSL substitution yet.
- Registered narrow correction: replace eager `configurations.named(...)` lookup with a lazy `configurations.configureEach` callback that applies the OkHttp Android-AAR exclusion only when the exact `debugUnitTestRuntimeClasspath` configuration is created. Keep `implementation("com.squareup.okhttp3:okhttp:5.4.0")` unchanged for production and keep `testRuntimeOnly("com.squareup.okhttp3:okhttp-jvm:5.4.0")`. Do not exclude OkHttp from production configurations or alter production login/domain behavior.
- Before editing, verify the active branch remains `integration/legado-upstream-runtime`, inspect the full Gradle dependency/configuration context and all affected test imports/callers. After editing, verify the debug unit-test runtime resolves exactly one OkHttp implementation (JVM), production runtime remains the Android AAR, and the official PSL resource is available to unit tests.
- Acceptance: focused login/session tests including multi-label eTLD+1 and unrelated CDN pass; full `:app:testDebugUnitTest`, `:integration-contract:test`, pinned Rhino artifact verification and `:app:assembleDebug` pass on the exact code head. Inspect JUnit reports, dependency graph, APK asset `assets/PublicSuffixDatabase.list` and merged AndroidX Startup initializer. Device install and broader Legado/five-category completion remain separate open gates.
- Self-review: this is a build configuration timing correction within the previously registered test-only OkHttp substitution. It does not change production runtime semantics or expand the login/session feature scope.


## 2026-11-20 pre-registered implementation scope — Stage 1 official models and lossless serialization (NO RUNTIME CODE EDITS YET)

### Current branch and evidence baseline
- Active branch: integration/legado-upstream-runtime; inspected branch head: 2b2bf91afca2dce8c07642eae4ae95f1a228937c. Do not modify main.
- Pinned upstream Legado revision: 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Latest exact-code-head Actions run: 44630795195, head 2b2bf91afca2dce8c07642eae4ae95f1a228937c; all reported job steps completed successfully, including contract tests, app unit tests, Rhino verification, Debug APK assembly and live source sampling. JUnit/APK/live-source artifacts still require inspection before recording detailed counts or source outcomes.
- Existing host boundary: integration-contract/.../ReadingEngine.kt defines simplified BookSource, BookCandidate, BookDetail, and BookChapter; LegadoBookSourceCodec parses selected source fields and stores rawJson, while the engine consumes selected fields. FileBookSourceCatalog persists descriptors and mutates enabled state through the codec. The rule entities currently present under app/src/main/java/io/legado/app/data/entities/rule/ are only a partial set of pinned models.
- Pinned upstream model sources identified: app/src/main/java/io/legado/app/data/entities/BookSource.kt, Book.kt, BookChapter.kt, and data/entities/rule/{ExploreRule,SearchRule,BookListRule,BookInfoRule,TocRule,ContentRule,ReviewRule}.kt. These models include Android/Room/Parcelable and runtime-facing contracts; they cannot be copied blindly into the platform-neutral module.

### Exact implementation target
Build a deliberate model/serialization compatibility boundary for Stage 1:
- Compare the exact pinned upstream declarations, nested types, default values, annotations/converters and serialization behavior for BookSource, Book, BookChapter and all source rule models.
- Inventory all current host model declarations, JSON codecs, catalog persistence/mutation/export paths, runtime constructors, UI adapters, tests and callers before changing any type or signature.
- Define and implement a lossless representation/adapter strategy that retains every official known field plus unknown/future fields across import -> in-memory representation -> persistence -> edit/toggle -> export. Preserve source identity semantics (bookSourceUrl) and book/chapter identity and ordering semantics.
- Map nested rule models using the pinned upstream contract; do not leave rule objects as opaque strings where runtime behavior needs typed models, and do not invent defaults that differ from upstream.
- Keep Android/Room/Parcelable concerns at the Android boundary; do not introduce an incompatible dependency or break the platform-neutral contract merely to copy upstream annotations.
- Keep existing UI and all current ReadingEngine callers source-compatible during the migration, or add explicit, tested adapters and migrate every caller in the same change.

### Compatibility/security risks to audit before edits
- Room annotations, Parcelable, ignored/transient properties, converters, lazy variables, BaseSource identity, and upstream helper dependencies may not compile or behave in the current host module.
- Gson/Kotlin serialization differences: missing vs null vs default values, numeric coercion, enum/unknown values, nested rule objects encoded as object or string, ordering, escaping, and unknown-field preservation.
- Existing code relies on BookSource.id/baseUrl for source identity and login/session scoping; do not change this identity path accidentally.
- Existing raw JSON preservation and import bounds/validation must not regress; mutations must alter only the intended fields and retain all other JSON members.
- Book/chapter key collisions, chapter indices, duplicate URLs, and runtime call sites must be audited before mapping to upstream Book / BookChapter.

### Required tests and acceptance
- Add pinned-upstream model contract tests for required/optional/default fields, nested rule types, unknown fields, null/missing values, enum/type edge cases, and identity/equality behavior where relevant.
- Add import/export round-trip fixtures from official upstream JSON, including future unknown keys and every rule family; verify semantic equality and field preservation.
- Add persistence round-trip and targeted mutation tests: import -> reload -> enable/disable or edit one field -> export; assert all unrelated fields remain byte/semantically intact.
- Add regression tests for every existing BookSource, BookChapter, LegadoBookSourceCodec, FileBookSourceCatalog, source-login identity, and ReadingEngine caller touched by the change.
- Run focused model/codec/catalog tests, :integration-contract:test, full :app:testDebugUnitTest, and :app:assembleDebug on the exact final code head; inspect XML reports and APK artifact. Verify all five rule models and all audited callers are represented in the adapter map.
- Acceptance is this Stage 1 slice only; it does not establish complete AnalyzeRule/AnalyzeUrl, full Rhino, unified runtime, batch support, UI parity, physical-device validation or release readiness.

### Explicit exclusions
No UI redesign; no changes to main; no selector/rule-engine rewrite; no Rhino extension expansion; no java.getSource() ABI change; no batch API/cache implementation; no unsupported stubs/no-op shims; no release APK claim.

### Required self-review before code
Re-read all five mandatory files and this registration; verify branch head again; inspect full pinned upstream model declarations and every affected host caller/adapter/test; list compatibility risks and test cases before editing. If inspection shows this registered scope is incomplete, amend this ledger first. After implementation, record exact changed paths, test results, CI run/head, artifact inspection, remaining gaps and a checklist self-review.


## 2026-11-20 mandatory reread and current Stage 1 implementation status

- Re-read `AGENTS.md` and all four mandatory project checklists on the active `integration/legado-upstream-runtime` branch before continuing. Also re-read `docs/BADCAT_REPLICATION_FIVE_CATEGORIES.md` and `docs/upstream-model-serialization-audit.md` because the former defines product parity separately and the latter governs the Android/JVM model boundary.
- Current branch head at this review: `5466caaf7cbbf2035b393b8dde70ef286d93a56e`. Pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`. `main` was not modified.
- This record supersedes the stale “NO RUNTIME CODE EDITS YET” baseline above: code commit `5466caaf7cbbf2035b393b8dde70ef286d93a56e` adds the upstream `BookSource.customOrder` field to the platform-neutral source projection, codec import/export mapping and regression fixtures. It is a narrow staging-model parity correction, not the official mutable Android `BookSource`/`BaseSource` runtime integration.
- The seven vendored source-rule entity files (`ExploreRule`, `SearchRule`, `BookListRule`, `BookInfoRule`, `TocRule`, `ContentRule`, `ReviewRule`) were compared by Git blob SHA with the exact pinned upstream files; all seven are byte-identical. This confirms source provenance only, not complete model/runtime behavior.
- Exact-code-head Actions run [#44633804009](https://github.com/xkszds/badcat-legado-client/actions/runs/44633804009), head `5466caaf7cbbf2035b393b8dde70ef286d93a56e`, is still `in_progress` at the final best-effort four-source uploaded-corpus sample. The contract tests, pinned Rhino verification, bounded Rhino host tests, app unit tests, Debug APK assembly/upload, Nyasama smoke, repository probe, Feisuzhong E2E and Pinshuzhai E2E steps are completed successfully; the last uploaded-corpus sample/report step is not yet complete.
- Downloaded unit-test XML artifact `14582903590`: 276 tests, 0 failures, 0 errors, 0 skipped.
- Downloaded Debug APK artifact `14582783770`: extracted APK size 18,163,811 bytes, SHA-256 `05b26ece98fda97321e94951e2303fbfa35cb85e3e6ddb185f50626d3650ddf9`. APK ZIP inspection confirms `assets/PublicSuffixDatabase.list` and `META-INF/LICENSE.md` are present. This is a Debug artifact, not a signed release and not physical-device validation.
- Live report artifacts `14583189639` and `14583199685` independently confirm Bixiange GBK POST search -> detail -> 114-page TOC -> chapter content, and Pinshuzhai search -> detail -> 1,650 chapters -> chapter content. The four-source uploaded-corpus sample remains pending; do not imply it passed.
- Next code work remains within Stage 1 and must follow the model audit: no isolated/fake `BookSource` or `BaseSource`; inspect the exact pinned service/model graph and all host callers, keep Android Room/Parcelable concerns out of the JVM contract, retain raw JSON for unknown-field losslessness, and pre-register a narrower adapter slice before runtime edits.
- Product parity remains separate: Badcat categories 1, 2, 4 and 5 are not completed by Legado category-3 progress. No full-client or final-APK claim is authorized.
- Self-review: the earlier Stage 1 preregistration's branch baseline and “no runtime edits yet” label became stale after commit `5466caa`; this record explicitly supersedes those statements. Checklist scope remains valid, but the next implementation must respect the more specific Android model/service boundary in `upstream-model-serialization-audit.md`.

## 2026-11-20 Stage 1 continuation — pinned model gap audit before adapter implementation

- Pre-registration baseline: branch `integration/legado-upstream-runtime`, exact head `eabbef43481e6c14b8bda6ea62f03c5213d1188d`; pinned Legado `330e606c4d57e08adc056ff8ab7a4132c784da9a`. `main` is out of scope.
- This increment is an audit/contract-definition slice, not a runtime feature claim. Compare the complete pinned `BookSource`, `Book`, `BookChapter`, `BaseSource` and nested rule declarations against the current host contract, codec, catalog, persistence and callers. Produce a field-by-field gap map including defaults, nullability, identity/equality, transient/ignored fields, converters, and app-service dependencies.
- Trace the existing `LegadoBookSourceCodec`, `ReadingEngine.BookSource/BookChapter`, `FileBookSourceCatalog`, all constructor sites, UI mappings, login/session source identity, and tests. Confirm the exact upstream path and dependency graph; do not assume the upstream models can be copied into the JVM contract.
- Risks: Kotlin/Gson constructor-default behavior, unknown JSON preservation, source identity (`bookSourceUrl`), book/chapter keys and chapter index ordering, Room/Parcelable/appDb/appCtx coupling, `BaseSource`/`JsExtensions` ABI and state lifecycle, accidental UI/engine caller breakage.
- Acceptance: audit document contains a verified field-level mapping and a concrete next bounded implementation registration; each unsupported dependency is evidenced by exact upstream callers. Add no fake DTO advertised as official model, no null/no-op service adapter, and no runtime code until the follow-up implementation scope is separately registered. This audit slice is accepted only when all current model/caller references are enumerated and reviewable.
- Required checks: exact branch head before/after; pinned upstream source files and nested rule models; repository-wide callers/tests; five-category checklist read; no product UI changes. After audit, record actual findings and CI status without implying feature completion.

## 2026-11-20 Stage 1 pinned-model audit result

- Audit result is recorded in `docs/upstream-model-serialization-audit.md`: exact pinned `BookSource`, `Book`, `BookChapter`, `BaseBook` and `BaseSource` contracts were inspected against the host `ReadingEngine`, `LegadoBookSourceCodec`, `FileBookSourceCatalog` and codec tests.
- Confirmed current source descriptor maps most BookSource scalar fields and preserves raw JSON, but does not provide upstream typed mutable rule instances or the BaseSource runtime lifecycle. Host Book/BookChapter are UI projections; BookChapter drops volume/book/base URL, VIP/pay/resource, position/fragments, variable and image metadata.
- Important identity correction: upstream BookChapter Room primary key is `(url, bookUrl)`, a unique index enforces `(bookUrl, index)`, while equals/hashCode compare URL only. Future cache/adapter work must preserve these distinct semantics.
- BaseSource depends on actual JsExtensions, Rhino bindings/shared scope, CookieStore, CacheManager, Android ID/AES login state, concurrent-rate limiting, JS library and explore-cache lifecycle. No fake adapter is acceptable.
- Audit-only task completed; no runtime source changed. Branch baseline at audit start: `eabbef43481e6c14b8bda6ea62f03c5213d1188d`; audit result commit: `e507eff16ae6c9e7de0241aa730e3f1739461317`.
- CI: code baseline run [#44636057145](https://github.com/xkszds/badcat-legado-client/actions/runs/44636057145) passed for `eabbef4`. Docs update run [#44637466823](https://github.com/xkszds/badcat-legado-client/actions/runs/44637466823) was pending when checked; intermediate docs runs were cancelled after newer commits. Do not report current docs head as CI-verified until its run finishes.
- Next gate: design and pre-register a real Android-side model/service adapter plus full caller inventory. Official Book/BookChapter persistence and round-trip fixtures, AnalyzeRule/AnalyzeUrl, full JsExtensions, unified reading, batch cache and Badcat categories 1/2/4/5 remain open.

## 2026-11-20 Android persistence/runtime boundary review

- Rechecked the current app tree and Gradle graph at baseline `0bce0f5862c516e4d35c89103010f6995c29594e`. The app currently has no Room dependency, database, DAO, official `Book`/`BookChapter` entity or pinned `JsExtensions` declaration. `FileBookSourceCatalog` uses bounded `AtomicFile` JSON storage for sources only; it is not an upstream Room database.
- The app's `legado-rhino` module is Rhino execution/sandbox infrastructure, not the pinned `io.legado.app.help.JsExtensions` API. The factory already supplies host HTTP, CookieJar, login/session, key-value and file-cache services, but does not supply the full upstream `CacheManager`, `CookieStore`, `SharedJsScope`, `RuleBigDataHelp`, `ReadBook`, `BookHelp`, `ContentProcessor`, `AppConfig` and app DB contracts required by `BaseSource`, `Book` and `BookChapter`.
- The pinned `BookSource` cannot be integrated alone because it implements `BaseSource : JsExtensions`. A look-alike BaseSource, a partial official Book/BookChapter class, or a fake Room wrapper is prohibited. Keep the Android-free `integration-contract` projections clearly separate.
- Detailed evidence and the dependency-safe decision are recorded in `docs/upstream-model-serialization-audit.md` (follow-up section dated 2026-11-20). No runtime code changed in this review.
- Next code slice is not yet authorized: first specify the complete upstream runtime module/service closure and migration path, including license/source manifest, source service adapters, Book/Chapter DB schema and migrations, caller inventory and pinned fixtures. Register that exact bounded slice in the four mandatory ledgers before code. Preserve current source JSON round-trip and existing UI.
- Current CI at the time of review: [#44637638029](https://github.com/xkszds/badcat-legado-client/actions/runs/44637638029) for head `0bce0f5` was pending; earlier documentation runs were cancelled by subsequent doc commits. Do not claim current docs head CI-verified.

## 2026-11-20 pinned runtime/database closure audit

- Inspected the exact pinned `JsExtensions`, `BaseSource`, `AppDatabase`, `DatabaseMigrations`, `CookieStore`, `CacheManager`, `SharedJsScope`, `BatchContentContext`, `BookHelp`, `ContentProcessor`, `AnalyzeRule` and `AnalyzeUrl` sources at `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- `JsExtensions` is a large Android/UI-facing API (42,899 source characters), not merely the already-present Rhino bridge. `BaseSource` requires real login/session, encrypted loginInfo, persistent variables, CookieStore, CacheManager, SharedJsScope, source helpers and lifecycle. `BookHelp`/`ContentProcessor`/`BatchContentContext` require DB-backed content persistence and per-chapter save tokens.
- Upstream `AppDatabase` is Room schema v113 with 26 entities, a view and a long explicit/automatic migration history (including custom migrations); it is not safe to copy as a tiny Book/BookChapter table. Current Badcat has no Room database; source JSON is persisted in AtomicFile.
- Detailed source/dependency facts are recorded in `docs/upstream-model-serialization-audit.md`. No runtime code changed in this audit.
- Before runtime code, select and pre-register either full upstream database closure (A) or a Badcat-owned Room schema with exact upstream model/serialization semantics and an AtomicFile migration (B). The latter is the recommended design direction, not yet approved or implemented. The registration must specify field/default/converter mapping, real login/cookie/cache/shared-scope/chapter-save adapters, all callers, migration/rollback tests and no-stub acceptance.
- Stage 1 remains incomplete. No fake BaseSource, partial Book/BookChapter entity, no-op service or claim of official AppDatabase compatibility is permitted.
