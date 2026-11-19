> **范围说明（重要）**：本文件仅是 Legado 引擎技术迁移细项，不是用户所说的“坏坏猫复刻五大类”。坏坏猫产品级五大类要求以 [BADCAT_REPLICATION_FIVE_CATEGORIES.md](BADCAT_REPLICATION_FIVE_CATEGORIES.md) 为准；后续必须同时维护两份清单，不得用本文件替代 UI、阅读器、AdBlock/AdMark、PPRule 等复刻任务。

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

- Re-read the root `AGENTS.md`, this checklist and `docs/full-legado-runtime-migration.md` before continuing. Confirmed the active branch is `integration/legado-upstream-runtime`; upstream submodule gitlink remains exactly `330e606c4d57e08adc056ff8ab7a4132c784da9`.
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
- [x] Vendored upstream `GsonExtensions.kt` with `INITIAL_GSON`, `GSON`, rule deserializers, primitive string/int handling and helper conversions, unchanged from pinned revision `330e606c4d57e08adc056ff8ab7a4132c784da9`.
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
- [ ] 从固定上游提交 330e606c4d57e08adc056ff8ab7a4132c784da9 的 Gradle/version catalog/锁定文件定位依赖来源；不得根据猜测添加版本或替代实现。
- [ ] 搜索宿主当前加解密 helper、同名方法、所有调用方、测试、JS 可见对象和安全边界，形成逐项兼容矩阵。
- [ ] 只有依赖、调用影响和测试边界确认后，才选择可独立验证的最小实现切片；不得把 AES/DES CBC helper 冒充完整上游 crypto API。
- [ ] 对新增/迁移 API 同时补上游语义测试和旧宿主行为回归测试；覆盖错误输入、空值、字节/字符编码、密钥/IV、模式/填充及 JS 互操作（按上游真实契约选取）。
- [ ] 运行聚焦测试、最广可行 Android 测试/构建和 CI；读取失败日志与产物信息。CI 未结束时明确标记待验证。

### 本轮开始前事实与阻塞
- 当前开发分支必须是 integration/legado-upstream-runtime；开始时 HEAD 为 6fa843febb293e95a9a0da44acc9e38b64776b48，禁止写入 main。
- 固定上游仍为 330e606c4d57e08adc056ff8ab7a4132c784da9。
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
