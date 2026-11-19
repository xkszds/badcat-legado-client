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
