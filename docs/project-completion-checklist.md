# 项目完成情况与剩余工作清单

更新时间：2026-11-19（继续原版 Legado 迁移并单列自定义 CSS 阅读器验收）

## 项目定位

目标是制作独立安装的坏坏猫 1.5.0.5 增强客户端：尽量保留原版坏坏猫 UI 和操作流程，在独立客户端中接入 Legado 书源/阅读能力，并分别恢复经证据确认的原版净化功能。原 APK 仅作只读基线，不覆盖原应用，不绕过签名、完整性或访问控制。

**当前结论：这是可由 CI 构建的增强原型，不是已完成的原版复刻或可发布成品。**

## 已完成（代码/文档层面）

- [x] Android 工程、独立客户端包结构和 GitHub Actions Debug 构建流程。
- [x] Legado 上游版本固定、部分依赖与许可证记录。
- [x] 书源 JSON 导入/导出和原始字段保留。
- [x] 有边界的 HTTP(S) 请求、重定向校验、URL 模板处理。
- [x] Android 默认书源 HTTP 客户端按书源隔离并持久化 Cookie（应用私有存储；不等于完整 Legado 登录系统）。
- [x] 正文解析后缓存到应用私有磁盘（64 MiB 总上限、12 小时新鲜期，网络失败时可回退到过期缓存）。
- [x] CSS/XPath/JSONPath 混合嵌套组合递归解析；搜索/发现列表支持 JSONPath 根及逐条 JSONPath 字段。
- [x] CSS 书源规则的受限解析子集，搜索、详情、目录、章节内容路径。
- [x] 目录与章节分页、重复 URL/重定向/fragment 循环保护及对应回归测试。
- [x] Legado 替换规则本地目录、JSON 编解码、启用/范围筛选、字面量与受限正则替换。
- [x] 有边界的 Rhino 替换规则执行器；仅支持受限上下文，不等同完整 Legado JS 环境。
- [x] 开发阅读页的章节阅读、进度/书签基础能力与 CSS 自定义方案管理。
- [x] 独立阅读历史页与历史记录持久化；历史记录页长按仅删除阅读历史（不删除书签/收藏）。首页不再显示未经用户要求添加的最近阅读面板。
- [x] 阅读 CSS 多方案、子选择器命名/备注、启停、复制、排序、预览、冲突提示、导入导出和资源上限。
- [x] CSS 选择器、净化器、规则顺序与输出大小的单元测试。
- [x] 先前基线提交 `e226dd6b559c` 的 CI 曾全部通过：契约测试、受限 Rhino 宿主测试、Android Debug APK 组装和产物上传均成功。Actions：[run #43845483951](https://github.com/xkszds/badcat-legado-client/actions/runs/43845483951)。该产物不是当前分支构建。
- [x] 当前统一开发分支 `fix/favorites-search-ui` 的提交 `106134128b648b57579cfb86431a1aef63b07d2d` 已通过完整 CI：362 项契约测试、受限 Rhino 宿主测试、Android Debug APK 组装及产物上传均成功。Actions：[run #44173422055](https://github.com/xkszds/badcat-legado-client/actions/runs/44173422055)。APK 仅为 Debug 构建，仍需真机验收。

## 尚未完成（主要交付阻塞项）

### P0：必须完成才能称为目标客户端

- [ ] **原版 UI 还原**：逐页重建搜索首页/结果、书籍详情、目录、书架/文件夹、阅读页和设置页；以原 APK 资源和参考截图逐屏比对。首页搜索栏位置设置位于「设置 → 布局」，当前不显示最近阅读面板；整页结构和截图仍未达到原版验收。
- [ ] **完整 Legado 规则运行时**：补齐上游模型和规则实体、AnalyzeRule/AnalyzeUrl、CSS/XPath/JSONPath 等解析、JS 绑定、请求头/Cookie/缓存/代理等宿主环境，以及初始化/持久化依赖。当前 CSS 解析器和 Rhino 都只是有界子集。
- [ ] **端到端适配**：将完整引擎接入原版搜索、详情、目录、阅读和书架 UI，验证来源切换、错误呈现、章节导航和离线/缓存策略。
- [ ] **原版净化功能迁移**：通过方法级证据确认 ADBlockUtils、AdBlockPresenter、AdMark 服务、PPRule 的真实数据结构、调用时机与行为；分别实现 WebView/页面广告过滤、AdMark、目录/分页/视频规则。现有 Legado 正文替换不能代替这些功能。
- [ ] **真机验收**：在目标 Android 设备安装独立包，完成导入书源、搜索、详情、目录翻页、章节分页、阅读、CSS、书签、进度、异常/断网、净化开关等回归测试；截图核对 UI。
- [ ] **发布包**：完成包名/版本/权限/崩溃检查、正式签名流程、可复现 Release 构建和安装升级验证。当前产物是 Debug APK。

### P1：分发前必须完成

- [ ] **GPL-3.0 对应源码交付**：将 CI 下载的上游 Rhino JAR/扩展源码及修改记录纳入可复现的源码交付方案，提供构建脚本、许可证、版权和修改声明。
- [ ] **第三方资产与许可证审查**：核对图标、字体、图片、依赖库和原版资源使用边界。
- [ ] **安全与稳定性验收**：审计 JS 沙箱逃逸面、资源上限、网络重定向、文件导入、WebView、存储迁移和异常恢复。

## 自定义 CSS 阅读器专项进度（独立验收，不与 Legado CSS 规则混为一谈）

- [x] 已有开发版多方案管理、子选择器名称/备注、启停、复制、排序、预览、重复选择器冲突提示、JSON 导入导出及安全限额。
- [x] 已有 CSS 选择器语法检查、属性白名单、危险值过滤、作用域隔离和章节 HTML 脚本/文件访问禁用。
- [ ] 对照坏坏猫原版 CSS 阅读器页面和交互，补齐原版管理 UI、控件状态与操作细节。
- [ ] 还原并验证原版 CSS 配置/备份数据结构，完成旧数据导入、导出和版本兼容；现有私有 JSON 格式不能视为原版兼容。
- [ ] 让样式规则作用于与原版一致的阅读 DOM/渲染节点；当前只对客户端生成的 .reader-root/.reader-content/.reader-paragraph 等归一化节点生效。
- [ ] 验证章节切换、主题/字号/行距、样式启停与层叠顺序在真实阅读流程中的联动。
- [ ] 使用原版截图、真实书源章节和真机操作完成 CSS 阅读器专项验收。

**当前结论：** CSS 自定义功能已有可用基础，但不是原版 CSS 阅读器的完整复刻。后续继续基于原版 Legado 阅读引擎迁移，不把 Legado 书源 CSS 解析规则或正文替换规则当作阅读器 CSS 功能。

## 重要差距说明

- CI 通过只说明自动化测试和 Debug APK 打包成功，不代表 APK 已在手机安装运行。
- 已有书源导入不代表所有 Legado 书源都能运行。
- 已有 CSS 阅读自定义仅针对客户端生成的阅读 DOM，不是任意网站原始页面 DOM。
- 已有 Legado 替换规则不代表坏坏猫原有广告净化、AdMark、PPRule 已移植。
- 已有书签/进度为开发阅读器基础能力，不代表原版阅读体验和完整数据模型。
- 原 APK 必须保持只读；新客户端使用独立包身份，不覆盖原应用或其数据。

## 建议执行顺序

1. 先完成原版 UI 逐页清单与资源/截图基线，搭出原版导航和页面骨架。
2. 同时按 Legado 上游固定版本逐层补齐规则运行时，优先完成可测试的搜索→详情→目录→章节完整纵向切片。
3. 用方法级证据恢复坏坏猫净化，不把字符串猜测当实现规格。
4. 接入 UI，做自动化截图/流程测试和真机回归。
5. 最后处理 GPL 对应源码、依赖许可证、正式签名和 Release 交付。


## Latest batch status (2026-11-16)

- Working branch: `fix/favorites-search-ui`; latest inspected head: `05e3a0238e077f011cfa5a7486078038f49d6f9a`.
- Pull request: [#2](https://github.com/xkszds/badcat-legado-client/pull/2), still open and mergeable at the last inspection.
- New code in this batch includes recursive mixed CSS/XPath/JSONPath scalar rules, JSONPath collection extraction for search/TOC, per-item JSONPath search fields, and explicit `@js:` marker disambiguation.
- Latest current-head CI run failed. Workflow log retrieval was unavailable in the latest attempt; do not infer that current-head tests or APK assembly passed.
- The full second category remains incomplete: full upstream AnalyzeRule/AnalyzeUrl semantics and JS host bindings, login/session helpers, source corpus compatibility, original UI integration, purification migration, device acceptance and release signing are still open gates.


## 2026-11-17 current batch update

- Working branch: `fix/favorites-search-ui`.
- Home layout settings now use three positions (8/24/40dp); MainActivity re-reads the saved position on return and applies the margin to the live search-bar view. Device verification is still required.
- The dedicated search page aligns the back control and search bar to the same 48dp row. Initial state shows search history only; source chips (without a separate “全部” chip) and pagination are hidden until an actual search is submitted.
- The home screen no longer adds a recent-reading panel that the user did not request.
- Bottom navigation uses a shared 24dp custom line-icon family and tap ripple/scale feedback. Personal-page feature icons use the same custom family; duplicate正文替换 entry, favorite count and avatar-top “我的” label are removed.
- Favorites overflow is a larger horizontal three-dot action.
- Legado runtime increment in progress: upstream `@@` forced-default CSS mode is now dispatched explicitly and covered by scalar and collection tests. This is not full AnalyzeRule compatibility.
- Badcat purification increment in progress: AdMark matching now rejects non-HTTP(S) schemes while preserving the verified exact-host + integer-array URL regex behavior; regression tests cover scheme and host boundaries. Original `markRules` DOM deletion semantics remain unimplemented because their exact contract is not recovered.
- The remaining parallel tracks are: complete more upstream AnalyzeRule/AnalyzeUrl semantics; recover and implement AdMark DOM deletion only from verified method-level evidence; continue PPRule/page-video runtime; and finish original UI/device parity, source corpus and release gates.
- The latest fully verified UI-only build was commit `1cdf5604380038b275e49f8b85a6a35f6391a20e`, Actions run [#44076883907](https://github.com/xkszds/badcat-legado-client/actions/runs/44076883907). Later parallel commits are not yet covered by that run; wait for a fresh build of the current HEAD before distributing another APK.


## 2026-11-17 parallel category work

- Legado: `@@` forced-default JSoup mode is dispatched; default-mode `@` chained selector stages, legacy `children/class/tag/id/text` heads and bounded numeric index suffixes are supported by the selector adapter. Contract tests cover chained and scalar forms. Remaining upstream AnalyzeRule gaps remain substantial.
- Badcat AdMark: the original APK/DEX ZIP were re-materialized read-only and their hashes verified. DEX method inspection confirms the `adMark` bridge handler, confirmation flow, one-item JSON payload sent to `onDeleteAd`, and `getInfoFromJS` callback for the page URL-pattern list. The client models/tests that payload and now imports both the original nested `markItems/ruleItems` JSON subset and the previous flat export; export uses the original nested shape. Server `adBlockJsInject` is intentionally not auto-installed. The client still does not call the page callback or remove DOM nodes.
- AdMark URL matching is restricted to HTTP(S), with exact host matching and the confirmed integer-array-to-regex `find()` semantics. `markRules` remains opaque until the page-side deletion contract is recovered.
- PPRule remains a separate pending track: DEX consumers now confirm the novel/comic/video dispatch paths, regex/XPath extraction, comic image URL fallback, and video path/episode selectors. The wire-compatible decoder and complete execution semantics remain unimplemented; see `docs/badcat-pp-rule-runtime-evidence.md`.
- These parallel changes are committed on `fix/favorites-search-ui`; they still require a fresh CI run against the current branch HEAD and, later, device-level validation.


## 2026-11-17 all-category parallel batch

- The shared main and fix/favorites-search-ui line is anchored at the unified CI-tested baseline; the active development branch now advances all workstreams together rather than waiting for one category to finish.
- Home/favorites/search UI: removed the two requested home shortcuts (排行榜、今日抽奖), retained the earn-cash shortcut, and kept the explicit-source-status cover badge in search results and favorites. Badge priority is 完结 → 连载 → 更新; missing/unrecognized source metadata produces no badge.
- Reader CSS: added regression coverage that rejects script-like declaration values and at-rules while retaining safe declarations.
- Legado rule runtime: added regression coverage for selector attribute values containing && and ||; this is a guard against incorrectly splitting quoted CSS attribute content, not a claim of full AnalyzeRule support.
- Badcat PPRule: added a bounded href/title extraction helper for the verified XPath/regex video element subset. It resolves relative HTTP(S) targets, deduplicates by URL and rejects unsafe schemes/blank labels. The caller supplies the specific rule; original video path/episode grouping and JavaScript semantics remain unimplemented.
- Badcat AdMark: the verified data codec, URL filtering and WebView injection preference path remain active; DOM deletion/long-press marking are still blocked on recovering the exact original callback semantics. No guessed DOM script was added.
- Validation gate: run the complete contract tests, bounded Rhino host tests and Android Debug APK assembly on the newest commit before calling this batch green. CI success does not replace Android device acceptance.


## 2026-11-17 parallel continuation (PPRule safety + UI/reader/Legado tracks)

- PPRule video href extraction now resolves links through URI resolution and accepts only absolute HTTP/HTTPS targets with a host and without embedded user credentials. Protocol-relative links are supported; malformed scheme-relative values and credential-bearing URLs are rejected.
- Added regression tests for malformed HTTP URI forms, credential-bearing links, protocol-relative links and duplicate resolved episode URLs.
- The existing novel-cover status badge is wired into both search results and favorites, and the favorite JSON model persists the source status; labels are based on explicit source metadata only (完结 > 连载 > 更新).
- The active branch continues to carry CSS safety regressions, quoted selector-operator regressions and bounded PPRule video extraction from the previous parallel batch. AdMark DOM deletion remains deliberately unimplemented until exact original semantics are recovered.
- The latest main-line build is still being checked independently; the next branch CI run must validate this new PPRule hardening before it is treated as green. Device/UI acceptance remains outstanding.


## 2026-11-17 all-workstream parallel batch (continued)

- Latest previously verified baseline `9f12c7c311aca66cb7a2067d90297412944bb03a` passed its contract/Rhino/Debug APK workflow and was fast-forwarded to `main` and `automation/chapter-redirect-cycle-guard-20261116`.
- New UI regression tests cover novel cover badge precedence (完结 > 连载 > 更新) and ensure unrelated source metadata does not produce invented labels.
- New reader CSS tests cover malformed nested selectors, commas inside `:not()`/attribute values, and scoping of comma-separated selector groups while retaining valid declarations.
- These tests join the PPRule video URL hardening/tests already included in `9f12c7c`; no DOM-removal behavior is inferred for opaque AdMark `markRules`.
- The current working branch contains this additional UI/CSS test batch; wait for its own CI before syncing it to the unified main line or publishing a new APK.
- Remaining project gates are still substantial: full upstream Legado AnalyzeRule/AnalyzeUrl and JS host compatibility, original UI page-by-page parity, complete Badcat AdMark/PPRule behavior, end-to-end source corpus testing, Android device acceptance, and release/source-license delivery.


## 2026-11-18 unified parallel continuation

- All five workstreams continue on the shared `fix/favorites-search-ui` branch; the verified unified CI baseline is `106134128b648b57579cfb86431a1aef63b07d2d` (run #44173422055).
- Legado contract tests (362), bounded Rhino host tests, Android Debug APK assembly and artifact upload all passed for that exact commit. Artifact SHA-256: `a0227b5b66675954a12e8b544a1d788a1b836bf00e5a57fa8999fcc3007b5339`.
- Reader/UI stability: cover image loading now needs a bounded pending-work queue; this is being added with an executor contract test to prevent unbounded task accumulation during rapid list scrolling.
- Passing CI does not close the remaining P0 gates: full upstream Legado compatibility, full original UI parity, complete AdMark/PPRule behavior, real-device acceptance and release signing remain incomplete.


## 坏坏猫复刻五大类总清单（长期基准）

用户所说的“五大类”是坏坏猫产品复刻范围，不是 Legado 的五个技术模块。统一入口见 [坏坏猫复刻五大类用户目标与总验收清单](BADCAT_REPLICATION_FIVE_CATEGORIES.md)。五类为：

1. 原版 UI、页面结构与交互复刻；
2. 原版阅读器与阅读体验复刻；
3. Legado 书源、搜索与内容引擎；
4. 坏坏猫原版广告净化、AdBlock 与 AdMark；
5. 坏坏猫原版 PPRule 规则系统（小说/漫画/视频）。

每轮开发须并行维护五类进度，不得用 Legado 引擎工作代替 UI、阅读器、AdBlock/AdMark 或 PPRule 的复刻。该总清单高于单个技术模块的局部迁移计划。

## 2026-11-19 continued runtime compatibility increment

- Improved chapter-cache bounded-reader forward progress and added zero-progress regression coverage.
- Required byte-preserving cache adapter methods and added binary fidelity/defensive-copy/source-isolation tests.
- Normalized repeated/trailing source-folder separators without accepting traversal; strengthened request-header parser validation.
- Added GET/HEAD/POST optional timeout overloads, wired per-call timeouts into the Android host request client, and aligned the default timeout to 30 seconds.
- Run [#44422285115](https://github.com/xkszds/badcat-legado-client/actions/runs/44422285115) passed contract tests, bounded Rhino tests and Debug APK assembly for commit `36e62023f4006db47876c69bff1b550b776730b8`; live best-effort probes remained in progress at last inspection. This run predates the newest timeout overloads.
- Latest commit `beda18696217ec9c2c65af6a6c8bcc3b5f06f2f9` has run [#44422727859](https://github.com/xkszds/badcat-legado-client/actions/runs/44422727859) pending. No physical-device verification or full Legado parity is claimed.

## 2026-11-19 HTTP redirect/timeout compatibility correction

- Kept JsExtensions `get/head/post` on the no-auto-redirect path and `connect` / URL selector fetches on the redirect-aware path, with explicit per-call timeout forwarding.
- Corrected the connect failure test fixture after the first CI attempt reported one stale callback expectation.
- Run [#44423657512](https://github.com/xkszds/badcat-legado-client/actions/runs/44423657512) passed contract tests, bounded Rhino host tests and Debug APK assembly; live smoke probes were still running at last inspection.

## 2026-11-19 redirect-aware download and ajaxAll follow-up

- Downloads, archive URL extraction, `ajaxAll`, one-argument `get(url)` and selector URL reads now follow redirects. Pinned Jsoup-style `get/head/post` overloads continue to expose redirect responses without auto-following.
- Updated runtime/archive/binary/charset/cacheFile/ajaxAll test fixtures for the split transport.
- Run [#44425052095](https://github.com/xkszds/badcat-legado-client/actions/runs/44425052095) passed contract tests, bounded Rhino tests and Debug APK assembly. Live smoke checks were still running at last inspection; no device acceptance or full Legado parity is claimed.

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
