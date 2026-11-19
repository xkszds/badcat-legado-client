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


## 2026-11-19 source-runtime compatibility follow-up

- Bounded `ajaxTestAll` exception-cause traversal to 16 links to prevent a cyclic Throwable cause chain from hanging a batch worker. Added tests for nested SocketTimeoutException mapping (`-2`) and cyclic cause fallback (`-7`).
- Upstream reference: pinned `JsExtensions.kt` / `AnalyzeUrl.kt` at `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Previous head `cacf216c3b65e11695260b8a73e138be74b22296` CI run [#44485135456](https://github.com/xkszds/badcat-legado-client/actions/runs/44485135456): contract tests passed; :app test task did not execute because Maven Central returned HTTP 429 for `me.zhanghai.android.libarchive:library:1.1.6`. Retry verification after the current commit.
- The fix is category 3 only; full product parity remains incomplete.


## 2026-11-19 CI compile correction

- The first verification of the bounded `ajaxTestAll` cause-chain change reached app unit-test compilation and exposed two test-source API mistakes: the archive test omitted the JUnit `assertThrows` import, and the connect-error assertion treated the Java `body()` accessor as a Kotlin property.
- Corrected only those test call sites; production behavior is unchanged. Contract tests passed on the previous run, but the Rhino test task and APK assembly did not run because Kotlin test compilation stopped first. Re-run CI is required before treating this increment as verified.


### AnalyzeRule selector context propagation (2026-11-19)

- The Rhino host now preserves the active AnalyzeRule `key` and one-based `page` bindings when Java selector helpers re-enter the shared Legado rule dispatcher. The same context is retained for overloads that supply an explicit content fragment.
- A focused contract test verifies that the selector document adapter retains URL, body, keyword and page. This is context propagation only; it does not claim complete upstream AnalyzeRule stage semantics or close the separately tracked Java/Android API gaps.


### JsExtensions navigation-context default (2026-11-19)

- Added the pinned upstream JsExtensions default `getSourceNavigationContext()` to the Rhino host bridge. Until the app supplies a source-navigation coroutine context, it returns Kotlin `EmptyCoroutineContext`, matching the upstream interface default rather than inventing cancellation or navigation state.
- Added a compatibility test for the default. Batch-content context, UI refresh events, WebView/browser/video/verification flows, font APIs and reader/theme configuration remain unimplemented host integrations and are not represented by stubs.


## 2026-11-19 本轮状态（必读清单复核后）

### 已完成
- [x] 完整重读 AGENTS 和所有必读技术/产品清单。
- [x] 完成官方 Legado source/rule model graph 与当前 host model 的差异审计；详见 `docs/upstream-model-serialization-audit.md`。
- [x] 完成 AnalyzeRule/AnalyzeUrl/JsExtensions 依赖图审计；详见 `docs/upstream-analyzer-integration-audit.md`。
- [x] 将审计结果、风险和下次任务写回迁移清单。

### 仍未完成
- [ ] 真实官方模型和规则实体集成（不是 JSON staging DTO）。
- [ ] 官方 Gson converters/deserializers 和 Room 持久化语义。
- [ ] 完整 AnalyzeRule/AnalyzeUrl 与 JsExtensions。
- [ ] 完整阅读流程和真实源/真机验收。
- [ ] 原版 UI、阅读器、AdBlock/AdMark、PPRule 五类完整复刻。
- [ ] 正式签名 Release APK。

### CI 事实
- 最新文档提交对应 run [#44554459580](https://github.com/xkszds/badcat-legado-client/actions/runs/44554459580) 仍为 pending。
- run [#44553806204](https://github.com/xkszds/badcat-legado-client/actions/runs/44553806204) 的 contract tests、bounded Rhino tests、Debug APK build/upload 及已完成 live steps 均成功；最后一个四源语料 live reading 步骤仍在运行。
- 以上不构成设备验收或完整 Legado 完成证明。


## 2026-11-19 pinned upstream rule model increment

- [x] Added exact pinned Legado source rule entities (`BookListRule`, `SearchRule`, `ExploreRule`, `BookInfoRule`, `TocRule`, `ContentRule`, `ReviewRule`, `ExploreKind`, `FlexChildStyle`, `RowUi`) to the Android app.
- [x] Added the pinned `INITIAL_GSON/GSON` implementation and custom rule deserializers from upstream `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- [x] Added Parcelize plugin/Flexbox dependency, source notices and focused tests for object/legacy-string decoding, null/default behavior and rule model semantics.
- [ ] Current commit CI not yet complete. Full source entity graph, BaseSource, Room persistence and complete engine remain P0 blockers.

## 2026-11-19 本轮清单复读与 crypto caller scan
- [x] 本轮开始前重读根 `AGENTS.md` 全文、Legado 技术清单、五大类清单、项目完成清单、迁移计划、模型/AnalyzeRule/JsExtensions 审计、UI 重建图、PPRule 与原版净化证据。
- [x] 确认分支 `integration/legado-upstream-runtime`，HEAD `41cb8083a04ef5c9524326ec374d0d0544a96c44`；固定上游 `330e606c4d57e08adc056ff8ab7a4132c784da9` 未变。
- [x] 完成当前仓库 231 个 Kotlin 与 21 个 Java 文件的目标 crypto 调用方搜索；结果和下一任务范围已写入 Legado 清单与审计。
- [ ] crypto 运行时代码尚未修改；下一项是已预登记的 Hutool/BC 与上游 crypto API 迁移。
- [ ] 当前 HEAD CI run `44560647609` 正在执行 `:app:testDebugUnitTest`（bounded Rhino host tests）；未结束前不标记通过。
- [x] 旧代码 run `44557806917`（head `28cff4a`）为 success；它不覆盖当前 HEAD。run `44559292311`（head `73ade446`）也已 success；均不能替代当前 HEAD 验证。
- [ ] 复审结论：原 checklist 的“剩余目录未扫描”已关闭；当前 crypto API 语义、Android provider 和 Rhino regression 仍是实施/验收门槛。


## 2026-11-19 Legado crypto implementation increment (pending verification)

- Added pinned upstream Hutool 5.8.22 / Bouncy Castle 1.86 dependencies and the upstream symmetric, asymmetric and signature helper classes plus Base64/Hex helpers.
- Expanded the source-scoped Rhino `java` bridge to the upstream crypto factories and legacy AES/DES/3DES methods; removed the obsolete narrow symmetric facade while preserving the JavaImporter AES sandbox.
- Added focused Rhino tests for AES, DES, 3DES, RSA, signing and factory overloads. CI and Android runtime/provider verification are pending.
- This is a category-3 Legado crypto slice only. It does not complete full `JsExtensions`, `BaseSource`, `BookSource`, `AnalyzeRule/AnalyzeUrl`, the reading pipeline or any other Badcat category.


## 2026-11-19 Legado crypto safety correction (pending)

- Review of the new Hutool crypto object identified that the prior 2 MiB host crypto input/output bound must be retained around the full upstream object surface. The separate JavaImporter AES cap does not cover Hutool factory objects.
- The corrective bounded-wrapper task is registered in the Legado checklist before further code edits. The crypto increment remains unverified and is not complete.


## 2026-11-19 Legado crypto resource-boundary implementation (pending verification)

- Added host-owned 2 MiB bounded wrappers for symmetric, asymmetric and signing operations while retaining the pinned upstream Hutool API surface and algorithm support.
- Bounded InputStream reads handle zero-progress and do not close caller-owned streams. Added exact-limit and over-limit regression tests.
- The JavaImporter AES sandbox remains separate and unchanged. CI and Android provider/runtime verification are pending.


## 2026-11-19 crypto wiring/test correction (pending)

- Post-commit review found the factory still returning an unbounded crypto object and a malformed adjacent Kotlin test insertion. Both are registered for correction before any verification claim.
- Crypto implementation remains pending; no successful CI for the current crypto code is claimed.


## 2026-11-19 crypto wiring correction applied (pending CI)

- Corrected the source bridge to instantiate the bounded Hutool-compatible symmetric subclass and validate key/IV size.
- Repaired the malformed test insertion and added the bounded stream/limit regression as a separate test.
- Added a decoded-byte check for asymmetric Base64/Hex ciphertext before decryption. CI remains pending.


## 2026-11-20 Legado crypto current-head verification — PRE-REGISTERED

- [ ] Inspect current-head run `44564458448` for `f924681f783628dedf6f835e191a701ba5ae6a16`; its initial state was pending/no jobs.
- [ ] Inspect ancestor run `44564284230` separately; it does not validate the current asymmetric decoded-byte correction.
- [ ] Verify focused tests, app unit tests, Hutool/BC dependency/provider packaging, Debug APK assembly, reports and artifacts.
- [ ] Review the exact current diff for bounded symmetric/asymmetric/signature operations, decoded ciphertext, InputStream handling and sandbox preservation.
- [ ] Register any discovered runtime correction and regression tests before editing code.
- [ ] Record actual results and self-audit this checklist after verification. Crypto slice remains unverified until current-head CI succeeds; complete Legado and device acceptance remain open.


## 2026-11-20 encoded crypto output-bound correction — PRE-REGISTERED BEFORE CODE EDIT

### Finding from current-head source review
- The 2 MiB boundary checks raw byte-array outputs in `RhinoBoundedSymmetricCryptoAndroid.encrypt/decrypt` and `AsymmetricCrypto.encrypt/decrypt`, but inherited/explicit Base64 and Hex encoders can expand those bounded byte arrays into strings larger than 2 MiB.
- Confirmed affected surfaces: Hutool `SymmetricCrypto` encoded helpers (`encryptBase64` / `encryptHex`, including ByteArray, String/Charset and InputStream overloads) and host `AsymmetricCrypto.encryptBase64/encryptHex`. The upstream Hutool API exposes these as separate string-returning helpers; bounding only the byte-returning primitive does not bound the returned string.
- This is a confirmed resource-boundary omission, not a CI failure and not a claim that current CI has passed.

### Registered implementation scope
- Keep all upstream algorithms, overloads, Base64 encoding semantics, Hex semantics, transformations, and source-scoped Rhino exposure unchanged.
- Enforce a maximum of 2 MiB on encoded string results returned to Rhino, after encoding, for every supported symmetric and asymmetric Base64/Hex helper path.
- Preserve raw byte-array 2 MiB bounds, decoded Base64/Hex input bounds, caller-owned InputStream ownership, zero-progress handling, key/IV bounds, and Sign behavior.
- Audit inherited Hutool overloads and Rhino reflection visibility so no overload bypasses the output check. Do not narrow the algorithm allow-list or alter JavaImporter package/class allow-list.

### Required regression coverage / acceptance
- For symmetric Base64 and Hex: test each applicable ByteArray, String and InputStream overload at/below the encoded-output limit and over the limit; include padding expansion and a deterministic large payload.
- For asymmetric Base64 and Hex: test encoded output limit and over-limit behavior with a deterministic supported key fixture where feasible; retain RSA encrypt/decrypt tests.
- Retain exact 2 MiB raw-output, raw-input, decoded ciphertext, key/IV, stream forward-progress/ownership, DES/3DES, signature, legacy-helper, JavaImporter sandbox and replacement-evaluator regression tests.
- Run focused crypto/Rhino tests, full app unit tests, Debug APK assembly, and inspect current-HEAD CI job steps/logs/reports/artifacts. Do not mark passed while CI is pending or logs are unavailable.
- Review full diff and confirm no bypass via inherited overloads; device/provider verification remains a separate open gate.

### Execution discipline
- This task is registered before any runtime code edit. Only after this ledger commit may the bounded encoder overrides/wrappers and regression tests be changed.
- After implementation, update all relevant checklists with exact files, test commands/results, CI run/head/artifacts, commit SHA, remaining device and full-Legado work, and a self-review of whether this registration captured all affected overloads.


## 2026-11-20 restore pinned Legado Chinese conversion runtime — PRE-REGISTERED AFTER CI FAILURE REVIEW

### Confirmed CI failure and source audit
- Current-head predecessor CI run `44564706186` (head `d140b35d614cbad2afe8cffa81f106aaab639a1d`) failed at `:app:compileDebugKotlin` with `RhinoSafeSourceJavaExtensions.kt:1934:38 Unresolved reference 'LegadoChineseConversion'`; the APK step was skipped and no APK artifact was produced. Contract tests passed; this is not a crypto-test failure.
- The current source tree contains the call `LegadoChineseConversion.traditionalToSimple(value)` but no production declaration/file for `LegadoChineseConversion`; only the Chinese-conversion test file exists.
- The pinned upstream implementation is `upstream/legado/app/src/main/java/io/legado/app/utils/ChineseUtils.kt` at `330e606c4d57e08adc056ff8ab7a4132c784da9`. It delegates `s2t/t2s` to quick-transfer-core and loads the exact `TRADITIONAL_TO_SIMPLE` exclusion dictionary before the first traditional-to-simplified conversion. Its excluded terms include `多士`, which the existing regression test requires to remain unchanged.
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


## 2026-11-20 crypto encoded-output test compile correction — PRE-REGISTERED BEFORE TEST EDIT

### CI evidence / confirmed finding
- Run `44565690133` targets intermediate head `75ae47aa3005418e01f47838ecc9e811b888538c` and completed with failure. Contract tests passed; the pinned Rhino artifact restore/verification steps passed; production `:app:compileDebugKotlin` passed.
- The failing step was `Run bounded Rhino replacement host tests`; its log reports `RhinoSourceScriptRuntimeTest.kt:1644:13` and `:1647:13 Unresolved reference 'checkedRhinoCryptoEncodedOutput'`, causing `:app:compileDebugUnitTestKotlin` to fail. The test-report upload found no reports; APK assembly/upload and live-source steps were skipped.
- Final branch head `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92` still contains those two unresolved helper calls in the test file; the follow-up commit only adds RSA Hex coverage and does not remove the missing helper references.
- The actual production output-bound implementation lives in `RhinoBoundedSymmetricCryptoAndroid` and `io.legado.app.help.crypto.AsymmetricCrypto`; the test must exercise those real methods, not refer to an undeclared helper.

### Registered correction scope
- Replace the two orphan helper assertions with direct assertions against the real bounded crypto API. Keep exact 2 MiB Base64/Hex boundary and over-limit checks across supported symmetric overloads.
- Add/retain deterministic asymmetric RSA Base64 and Hex round-trip coverage through the Rhino bridge. Do not claim a >2 MiB asymmetric ciphertext case unless a practical supported fixture can exercise it; document that limitation instead of introducing a fake helper or weakening production limits.
- Inspect the full test class for other unresolved symbols and re-audit the production encoded-output overrides against the exact Hutool 5.8.22 overload signatures before editing.
- Keep this change limited to test correctness/coverage; do not change crypto algorithms, key parsing, encoded output behavior, Chinese conversion, JavaImporter sandbox or other runtime APIs unless a separately registered concrete defect is found.

### Required acceptance
- Focused crypto/Rhino unit tests compile and pass, including exact/over-limit symmetric Base64 and Hex overload tests, RSA Base64 and Hex decrypt round trips, DES/3DES, signatures, stream ownership/progress and JavaImporter restrictions.
- Run `:integration-contract:test`, `:app:testDebugUnitTest` and `:app:assembleDebug`; inspect full job steps/logs, reports and artifacts for the exact final HEAD.
- Poll run `44565856067` (head `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92`) separately; its initial state was pending/no jobs and it does not validate a later correction.
- Do not mark crypto complete or provide an APK until the final code-head CI is green and the APK artifact is verified. Physical-device crypto/provider validation remains open.

### Execution / review
- This corrective test task is registered before editing. Afterward record exact changed files, CI run/head, test results, artifact IDs and the checklist self-review. Ensure no helper reference remains unresolved.


## 2026-11-20 crypto encoded-output test correction — implementation recorded, verification pending

- Corrective task was pre-registered in this file and the three companion required checklists before the test edit.
- CI run `44565690133` (head `75ae47aa3005418e01f47838ecc9e811b888538c`) and final predecessor run `44565856067` (head `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92`) both passed contract tests and production `:app:compileDebugKotlin`, but failed `:app:compileDebugUnitTestKotlin` on two unresolved references to the nonexistent `checkedRhinoCryptoEncodedOutput` helper at the encoded-output test. APK assembly/upload were skipped. Full logs were inspected; no unit-test report or APK artifact was uploaded for those runs.
- Removed the two orphan helper assertions from `app/src/test/kotlin/app/badcat/client/runtime/RhinoSourceScriptRuntimeTest.kt` in commit `43baad71f6937f25baafc80541ec3e787d7731d6`. The real production boundary is exercised directly by the existing exact/over-limit symmetric Base64 and Hex assertions; the redundant fake helper is no longer referenced.
- Existing RSA bridge test now includes Base64 and Hex encrypt/decrypt round trips (added in predecessor commit `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92`). A >2 MiB RSA encoded-output fixture is not claimed; current practical RSA fixture verifies both encodings and round trips, while production code performs the size check on encoded results.
- CI run `44566457890` targets code commit `43baad71f6937f25baafc80541ec3e787d7731d6`; it was queued/in progress at the time of this entry. It must complete and its full job steps/logs/artifacts must be inspected before marking the test correction or crypto CI gate passed.
- Current implementation status: compile-error source correction committed; focused test execution, full app unit tests, APK assembly/artifact verification and device/provider validation remain pending. No crypto completion or APK release claim is made.
- Checklist self-review: this correction accurately captured the actual compile failure and removed only the undeclared test helper; existing direct boundary tests remain. No runtime behavior or algorithm was changed. Next review must inspect all tests/logs on the exact code-head CI.



## 2026-11-20 crypto encoded-output policy correction (pending implementation)

- Re-audit of the pre-crypto host helper shows no symmetric encryption-result or Base64/Hex output cap. The prior 2 MiB encoded-output requirement was inaccurate; preserving the original 2 MiB raw input and 4 MiB encoded ciphertext input limits is the compatibility requirement.
- Next registered change removes the symmetric output cap and allows encoded expansion, while retaining bounded raw inputs and asymmetric/signature raw limits. CI and APK verification remain pending.


## 2026-11-20 crypto compatibility regression correction — PRE-REGISTERED BEFORE RUNTIME CODE EDIT

### Current branch and CI evidence
- Active branch: `integration/legado-upstream-runtime`; pre-registration parent HEAD: `bc18f521f5717bd8ead23677a22c0c9ae7b4e8fb`; pinned Legado upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9`.
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
