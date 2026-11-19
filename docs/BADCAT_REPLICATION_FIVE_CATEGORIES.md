# 坏坏猫复刻：五大类用户目标与总验收清单

> **范围纠正（重要）**：用户所说的“五大类”是复刻坏坏猫原版客户端的产品要求，不是 Legado 引擎内部的五个技术模块。此前的 `USER_REQUIREMENTS_AND_PARITY_CHECKLIST.md` 只是 Legado 技术迁移细项，不能替代本文件，也不能称为用户的五大类。

本清单依据仓库既有的坏坏猫 APK 结构分析、项目完成清单、UI 重建图、净化证据和 PPRule 证据整理。当前仓库记录中的五条复刻工作线归纳如下；如用户提供过的原始五类名称/顺序与此不同，应以用户原始要求为准并在此修订，不能擅自缩小范围。

## 总目标

复刻坏坏猫 1.5.0.5 的原版产品体验，交付独立安装的增强客户端。以用户提供的原版 APK、界面截图、实际交互和方法级证据为验收基准；Legado 是书源/阅读能力的引擎，不得取代坏坏猫原有 UI、导航、净化和规则系统。原 APK 只读保留，新客户端使用独立包身份，不覆盖原应用及数据。

## 五大类（坏坏猫产品复刻范围）

### 第一类：原版 UI、页面结构与交互复刻

- [ ] 首页、主导航、四个底部入口、搜索入口和首页内容结构按原版逐屏还原。
- [ ] 搜索页、搜索结果页、类型切换、来源切换、历史记录、加载/空/错误状态按原版还原。
- [ ] 书籍详情页、漫画详情页、目录页、书签目录和来源切换按原版还原。
- [ ] 收藏夹/书架：作品与文件夹、文件夹网格、三列卡片、筛选、排序、更多菜单、创建/重命名/移动/删除等原版操作。
- [ ] 我的/喵星/设置等页面与入口按原版信息架构还原；不得擅自添加用户未要求的面板或快捷入口。
- [ ] 逐页对照原版截图、布局资源、控件尺寸、颜色、字体、图标、间距、弹窗和状态；建立截图回归。
- [ ] 所有按钮和菜单必须有真实行为；没有后端服务的推荐、AI 搜索、热榜等不得用假数据冒充。
- [ ] 原版导航流程和返回栈保持一致；新引擎只替换数据层，不重设计用户流程。

### 第二类：原版阅读器与阅读体验复刻

- [ ] 原版在线小说、漫画、本地阅读入口和对应阅读器行为分别还原。
- [ ] 阅读页面布局、顶部/底部工具栏、目录抽屉、章节切换、进度条、点击区域、翻页动画/模式按原版还原。
- [ ] 字体、字号、字重、行距、段距、页边距、背景、夜间/护眼模式、亮度、屏幕方向等设置和持久化与原版一致。
- [ ] 上一章/下一章、目录跳转、书签、阅读历史、进度恢复、自动翻页/滚动、听书等按原版实际支持情况实现。
- [ ] 阅读 CSS 自定义保留并融入原版阅读器：多方案、子选择器、启停、复制、排序、备注、预览、冲突提示、导入导出和安全限制。
- [ ] 小说/漫画内容模型、章节缓存、离线阅读、网络失败恢复和阅读记录生命周期正确。
- [ ] 用原版截图和真机操作验证，不能把临时开发阅读页称作原版阅读器。

### 补充独立验收项：坏坏猫自定义 CSS 阅读器（必须纳入第二类）

> 这是用户额外明确提出的独立验收要求。它归属阅读器产品线，但必须单独跟踪，不得因为已有一个开发版 CSS 编辑器就标记完成。

- [ ] 以坏坏猫原版自定义 CSS 阅读器的界面、操作和数据行为为基准，恢复样式方案管理、子选择器管理及阅读页实际应用的完整闭环。
- [ ] 方案级与子选择器级的新增、编辑、删除、启停、复制、排序、备注、预览、冲突提示均须在真实阅读器中生效并持久化。
- [ ] 导入/导出需验证原版数据格式、字段兼容、异常文件处理和多版本迁移；不能只验证新客户端自定义 JSON 格式。
- [ ] CSS 应用范围必须与原版阅读器实际 DOM/渲染结构对应；不能把仅作用于客户端生成的 .reader-paragraph 等归一化节点，宣称为原版任意网页 DOM 的 CSS 阅读器。
- [ ] 明确 CSS 阅读器与 Legado 书源 CSS/JSoup 解析规则、Legado 正文替换规则的边界；三者不可互相冒充。
- [ ] 验收必须覆盖样式方案开关、层叠顺序、选择器作用域、阅读主题/字号/行距联动、章节切换后持续生效、导入导出往返及恶意 CSS 安全边界。
- [ ] 在原版截图和真实书源章节上做逐项对照；开发版管理页和单元测试通过不等于原版阅读器复刻完成。

当前代码已有本地多方案、子选择器、启停、复制、排序、预览、冲突提示和 JSON 导入导出等基础；但现有实现明确只作用于新客户端归一化章节 DOM，且采用客户端私有 JSON 结构，尚未证明与原版 CSS 阅读器格式、UI、DOM 和行为兼容。因此状态仍为进行中。

## 第三类：Legado 书源、搜索与内容引擎

- [ ] 固定上游 Legado 版本并记录许可证、源码映射和差异。
- [ ] 书源 JSON 无损导入导出、管理、启停、分组、排序和字段扩展保留。
- [ ] 完整书源请求语义：AnalyzeUrl、URL 模板、GET/POST、headers/body、编码、重定向、Cookie、登录、缓存、超时和错误处理。
- [ ] 完整规则解析：CSS/JSoup、XPath、JSONPath、正则、组合规则、索引、替换和嵌入 JS。
- [ ] Rhino 与上游 JS 宿主 API、Jsoup Java 桥接、mainJs/jsLib、源级作用域和持久化。
- [ ] 搜索、发现、详情、目录、分页、正文、contentBatch、替换规则、章节缓存完整接入 UI。
- [ ] 真正通过 Badcat Kotlin/Android 引擎验证真实书源；Python 抓取不算引擎验收。
- [ ] 该类只负责书源/内容引擎，不能替代第一、二、四、五类。

### 第四类：坏坏猫原版广告净化、AdBlock 与 AdMark

- [ ] 根据原版 APK/DEX 的方法级证据还原 ADBlockUtils、AdBlockPresenter、AdMark 服务和设置页行为。
- [ ] 还原原版广告净化开关、规则加载、注入时机、WebView 生命周期、页面回调、规则更新和状态持久化。
- [ ] AdMark 数据结构、导入导出格式、URL 匹配、页面 DOM 标记/删除、长按/确认/取消流程与原版一致。
- [ ] 验证 markRules 的真实结构和调用链；不得猜测 JSON 结构或自行编写未经证实的 DOM 删除脚本。
- [ ] 规则作用范围、白名单/黑名单、域名匹配、资源释放和页面切换行为有单元测试及真机测试。
- [ ] Legado 正文替换规则不是坏坏猫广告净化的替代品。

### 第五类：坏坏猫原版 PPRule 规则系统（小说/漫画/视频）

- [ ] 还原 PPRule 的原版数据格式、导入导出、版本兼容、规则管理、启停和持久化。
- [ ] 还原小说规则、漫画规则、视频规则的真实分派和执行流程。
- [ ] 小说/漫画：按原版支持的 XPath、正则、内容/图片提取、URL 解析、下一页/章节等行为执行。
- [ ] 视频：视频源/路径/集数分组、标题与链接提取、相对 URL、播放地址选择、集数切换等按原版执行。
- [ ] 完整还原规则优先级、匹配失败、备用规则、异常处理和 UI 操作。
- [ ] 现有受限 href/title 提取 helper 仅是局部能力，不能算 PPRule 完成。
- [ ] 以原版 DEX 方法调用和真实规则样本建立测试；无法确认的行为明确标为待证，不编造实现。

## 全局交付与质量门槛

- [ ] 五大类均有单独进度、源码/证据位置、实现文件、测试、CI 和真机验收记录。
- [ ] 原版 APK 与用户参考截图只读保存；记录文件哈希，禁止覆盖或修改基线。
- [ ] 原版受保护代码/第三方资产的许可证边界必须遵守；以独立实现和可验证行为为目标，不公开未经授权的完整反编译代码。
- [ ] 单元测试、模块集成测试、Android 构建、UI 截图测试和真机端到端测试分开报告。
- [ ] CI 绿灯不等于 UI 复刻完成；截图相似不等于功能完成；书源跑通不等于净化/PPRule 完成。
- [ ] 不允许假按钮、固定假数据、空壳 API、静默回退或只在测试入口实现。
- [ ] 发布前完成独立包名、权限与安全审计、正式签名、可复现 Release 构建、安装升级和 GPL/第三方许可证交付。

## 当前仓库已知状态（2026-11-19）

- UI：有页面/资源调查和部分首页、收藏、搜索等增量，但原版逐页还原与截图验收未完成。
- 阅读器：有开发阅读流程、历史/书签/进度基础和阅读 CSS 管理；自定义 CSS 阅读器作为独立验收项继续推进。现有 CSS 只作用于归一化章节 DOM，原版 CSS 数据格式、原版 DOM/渲染作用域、完整界面行为和真机视觉验收未完成。
- Legado：已有受限引擎和上游解析器迁移工作；当前分支 CI 存在 3 项失败，完整 AnalyzeRule/AnalyzeUrl/JS runtime 未完成。
- AdBlock/AdMark：已有部分数据格式与 URL 过滤证据/实现；页面回调、DOM 删除等关键行为仍未完成。
- PPRule：已有部分 DEX 证据和受限视频链接提取；完整 wire format、小说/漫画/视频执行流程未完成。
- 当前不是完整复刻，不得对外宣称已完成。

## 每次继续开发必须执行

1. 先读本文件、`docs/project-completion-checklist.md`、`docs/badcat-ui-reconstruction-map.md`、`docs/badcat-pp-rule-runtime-evidence.md`、`docs/badcat-original-purification-evidence.md` 和最新 CI。
2. 五类并行推进，按当前阻塞选择可验证的小步；不能只做 Legado 后把其他四类搁置。
3. 每次代码提交都更新本文件与 `docs/project-completion-checklist.md`，写清本次改动、测试、CI、未完成事项。
4. 不要依赖聊天记录；仓库文档是持续任务清单的唯一长期事实来源。
5. 不擅自合并到 main，不发布未经真机验收的 APK。

## 2026-11-19 continued Legado engine increment

- The Legado engine track gained bounded cache-stream progress handling, mandatory binary cache adapter methods, source-folder separator normalization, stricter request-header parsing, and GET/HEAD/POST per-call timeout overloads with Android host transport wiring.
- Run [#44422285115](https://github.com/xkszds/badcat-legado-client/actions/runs/44422285115) passed contract tests, bounded Rhino tests and Debug APK assembly for commit `36e62023f4006db47876c69bff1b550b776730b8`; best-effort live source probes were still running at last inspection. That run does not include the newest timeout-overload changes.
- Latest engine commit: `beda18696217ec9c2c65af6a6c8bcc3b5f06f2f9`; verification run [#44422727859](https://github.com/xkszds/badcat-legado-client/actions/runs/44422727859) was pending.
- This is a narrow increment within category 3 only. Categories 1, 2, 4 and 5 remain incomplete; the client is not a finished Badcat replica.

## 2026-11-19 HTTP redirect semantics correction

- Legado category 3 now keeps distinct request semantics: JsExtensions `get/head/post` do not auto-follow redirects; `connect` and URL-based selector fetches use redirect-aware GET. Explicit timeout overloads preserve the no-redirect behavior.
- A CI test initially failed because the connect-error fixture still injected the old callback. The fixture was corrected. Run [#44423657512](https://github.com/xkszds/badcat-legado-client/actions/runs/44423657512) passed contract tests, Rhino host tests and Debug APK assembly; live probes remained in progress at last inspection.

## 2026-11-19 redirect-aware downloads and ajaxAll

- Redirect-following GET is now used for downloads, archive URL reads, `ajaxAll`, one-argument `get(url)`, and selector URL fetches. Pinned `get/head/post` overloads retain no-auto-redirect behavior.
- Regression fixtures were migrated to the correct callback. Run [#44425052095](https://github.com/xkszds/badcat-legado-client/actions/runs/44425052095) passed contract/Rhino tests and Debug APK assembly; live probes remained in progress at last inspection.
- This remains category 3 engine compatibility work; the full upstream host API and categories 1, 2, 4 and 5 remain incomplete.

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


## 2026-11-19 CI compile correction

- The first verification of the bounded `ajaxTestAll` cause-chain change reached app unit-test compilation and exposed two test-source API mistakes: the archive test omitted the JUnit `assertThrows` import, and the connect-error assertion treated the Java `body()` accessor as a Kotlin property.
- Corrected only those test call sites; production behavior is unchanged. Contract tests passed on the previous run, but the Rhino test task and APK assembly did not run because Kotlin test compilation stopped first. Re-run CI is required before treating this increment as verified.


### AnalyzeRule selector context propagation (2026-11-19)

- The Rhino host now preserves the active AnalyzeRule `key` and one-based `page` bindings when Java selector helpers re-enter the shared Legado rule dispatcher. The same context is retained for overloads that supply an explicit content fragment.
- A focused contract test verifies that the selector document adapter retains URL, body, keyword and page. This is context propagation only; it does not claim complete upstream AnalyzeRule stage semantics or close the separately tracked Java/Android API gaps.


### JsExtensions navigation-context default (2026-11-19)

- Added the pinned upstream JsExtensions default `getSourceNavigationContext()` to the Rhino host bridge. Until the app supplies a source-navigation coroutine context, it returns Kotlin `EmptyCoroutineContext`, matching the upstream interface default rather than inventing cancellation or navigation state.
- Added a compatibility test for the default. Batch-content context, UI refresh events, WebView/browser/video/verification flows, font APIs and reader/theme configuration remain unimplemented host integrations and are not represented by stubs.


## 2026-11-19 必读清单复核与进度标注

### 已完成（审计工作，不是产品功能验收）
- [x] 本轮完整重读 AGENTS、Legado 技术清单、迁移计划、项目完成清单、UI 重建图、PPRule 证据、原版净化证据。
- [x] 完成官方 Legado 模型/规则依赖审计并写入 `upstream-model-serialization-audit.md`。
- [x] 明确五类不能互相替代；本轮只推进第三类的模型审计，不将第一、二、四、五类标记为完成。

### 仍未完成
- [ ] 第一类：原版 UI 逐页复刻和真机截图验收。
- [ ] 第二类：原版阅读器/CSS DOM、数据格式和交互验收。
- [ ] 第三类：官方模型、AnalyzeRule/AnalyzeUrl、JS 宿主、统一阅读流程及真机书源验收。
- [ ] 第四类：原版 WebView 广告过滤、AdMark 页面回调/DOM 删除闭环。
- [ ] 第五类：完整 PPRule 分派、分页、漫画和视频流程。
- [ ] 正式 Release、签名、许可证交付和设备验收。

本轮 CI 状态：run [#44554459580](https://github.com/xkszds/badcat-legado-client/actions/runs/44554459580) 对最新文档 head 仍为 pending；run [#44553806204](https://github.com/xkszds/badcat-legado-client/actions/runs/44553806204) 的 contract/Rhino tests、Debug APK 和前四个 live checks 已通过，上传语料的最后一项仍在执行。不得把 pending/in-progress 写成全绿。


## 2026-11-19 Legado category 3 model slice

- [x] Category 3 initial source-rule model slice: exact pinned upstream rule classes and Gson helper were vendored into the Android app; object and legacy string forms have dedicated Android unit tests.
- [ ] Category 3 remains incomplete: official `BookSource/BaseSource`, `Book/BookChapter`, Room persistence, AnalyzeRule/AnalyzeUrl, full JsExtensions and end-to-end flow remain open.
- [ ] Categories 1, 2, 4 and 5 remain incomplete and were not changed by this model slice.
- CI for the code commit is pending; no new test/build success is claimed yet.

## 2026-11-19 Legado crypto caller scan (category 3 only)

- Completed the previously missing repository-wide caller scan: all 231 Kotlin and 21 Java files were checked for the pinned crypto factory/helper identifiers. No extra runtime callers were found outside the recorded source-scoped bridge, Rhino runtime tests and the independent replacement evaluator.
- The next category-3-only implementation gate is registered in the Legado checklist: exact pinned Hutool/BC dependencies and crypto classes, complete inherited crypto API surface, source-scoped Rhino exposure, old-behavior regression and unchanged JavaImporter sandbox.
- Categories 1, 2, 4 and 5 remain incomplete and unchanged. This audit is not a feature-completion claim.


## 2026-11-19 category 3 crypto runtime slice (verification pending)

- Implemented the pinned Legado crypto API slice: Hutool 5.8.22 / Bouncy Castle 1.86, upstream symmetric/asymmetric/signature classes, source-scoped bridge factories and missing legacy AES/DES/3DES helpers, with focused Rhino tests.
- Preserved the separate JavaImporter sandbox and replacement-rule digest/HMAC path. CI/Android provider validation is pending.
- Categories 1, 2, 4 and 5 remain incomplete and unchanged; this increment is not full Legado or product parity.


## 2026-11-19 category 3 crypto safety review

- Diff review found the removed AES/DES host facade's 2 MiB data bound was not yet preserved around the newly exposed Hutool objects. A bounded wrapper for symmetric/asymmetric/signature operations is now registered before further code edits.
- Category 3 crypto migration remains pending CI and safety-boundary correction. Other categories remain unchanged.
