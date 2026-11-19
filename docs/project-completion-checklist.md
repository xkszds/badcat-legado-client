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
- [x] Added the pinned `INITIAL_GSON/GSON` implementation and custom rule deserializers from upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- [x] Added Parcelize plugin/Flexbox dependency, source notices and focused tests for object/legacy-string decoding, null/default behavior and rule model semantics.
- [ ] Current commit CI not yet complete. Full source entity graph, BaseSource, Room persistence and complete engine remain P0 blockers.

## 2026-11-19 本轮清单复读与 crypto caller scan
- [x] 本轮开始前重读根 `AGENTS.md` 全文、Legado 技术清单、五大类清单、项目完成清单、迁移计划、模型/AnalyzeRule/JsExtensions 审计、UI 重建图、PPRule 与原版净化证据。
- [x] 确认分支 `integration/legado-upstream-runtime`，HEAD `41cb8083a04ef5c9524326ec374d0d0544a96c44`；固定上游 `330e606c4d57e08adc056ff8ab7a4132c784da9a` 未变。
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


## 2026-11-20 next task pre-registration — upstream JsExtensions/BaseSource compatibility audit (no code edits)

- [ ] Audit all pinned JsExtensions.kt and inherited JsEncodeUtils.kt declarations/overloads against the host bridge, including types/defaults/nullability, Rhino-visible return-object shape, exceptions, threading/coroutines, lifecycle/context and source isolation.
- [ ] Trace pinned BaseSource.kt and all required models/services: Room/Gson, cookies/login, script storage, cache, HTTP, WebView/browser, navigation, refresh/verification/video and batch persistence.
- [ ] Search all repository callers/tests for getSource, source descriptor, HTTP response, file handle, getBatchContext, cacheContent, and all missing or semantically divergent methods.
- [ ] Resolve and document the getSource(): BaseSource? versus descriptor-map collision, BatchContentContext/durable cache gap, HTTP response ABI and sandboxed file-handle boundary.
- [ ] Record exact pinned upstream declarations/call sites and host source/test paths; classify each method as behaviorally compatible, deliberately restricted for security, unsupported in the current UI context, or unimplemented.
- [ ] Do not change runtime/product code in this audit. Do not add fake BaseSource objects, null service placeholders, no-op UI methods or a simplified interface.
- [ ] Acceptance: complete method/overload matrix, complete BaseSource service dependency map, known-caller list, explicit risks and one bounded next implementation slice with regression tests. Pre-register that implementation separately in all four checklists before any code edit.

This gate exists because the current audit is declaration-count complete but its overload/behavior matrix is not complete; the crypto CI/build gate is now independently verified on the latest documentation head, while physical-device validation and full Legado migration remain open.

## 2026-11-20 completion audit — JsExtensions/BaseSource boundary

**Audit complete.**

- [x] Re-read AGENTS.md and all four required project documents before this task.
- [x] Confirm pinned upstream SHA and inspect JsExtensions, JsEncodeUtils, BaseSource, BookSource/RssSource, AnalyzeUrl, BatchContentContext, JsSourceEngine, AnalyzeRule and supporting services.
- [x] Compare public declarations and overload counts with RhinoSafeSourceJavaExtensions.
- [x] Trace current host source/global bindings, cookie/login/cache/file/HTTP services and targeted runtime/test callers.
- [x] Register this audit in all four required checklists before work.
- [x] Record findings and the next implementation task in all four checklists.
- [ ] Implement full BaseSource/JsExtensions parity.
- [ ] Resolve java.getSource ABI without losing the existing descriptor-map use case.
- [ ] Integrate BookSource/RssSource models and Room/GSON lifecycle.
- [ ] Integrate real batch-content context and durable chapter persistence.
- [ ] Implement or explicitly support UI/WebView/navigation/config APIs.
- [ ] Validate runtime on a physical Android device and complete source-corpus tests.

The audit found 17 public JsExtensions method names (30 overload declarations) absent from the host bridge. JsEncodeUtils has all 30 declarations/27 names present at source-declaration level. The largest ABI mismatch is getSource(): BaseSource? upstream versus Map descriptor in host. The host source global is a login helper, not BaseSource; sourceApi is absent. BatchContentContext/cacheContent are not connected. The host response wrapper and file facade are intentionally narrower than Jsoup Connection.Response and java.io.File.

Critical security finding: SourceLoginSessionRequestClient attaches saved login headers to every URL for a sourceId, but pinned AnalyzeUrl only attaches them for the source's same site. Cookie synchronization/removal and AES encryption of loginInfo also differ. This is the next bounded implementation task, registered separately below.

### Next implementation pre-registration — source login/session parity and credential boundary (not started)

- [ ] Restrict saved login-header propagation to the source site, matching pinned AnalyzeUrl semantics; never send Authorization/custom login headers to unrelated hosts or CDNs.
- [ ] Synchronize Cookie from putLoginHeader into the source cookie jar; remove associated cookie state when removeLoginHeader is called.
- [ ] Encrypt loginInfo using the pinned androidId-derived AES key and add a tested migration for existing plaintext host records.
- [ ] Preserve source isolation, cancellation, header validation and secret redaction.
- [ ] Add tests for same-site/subdomain/cross-origin/CDN requests, cookie merge/removal, encryption/migration, malformed values and two-source isolation.
- [ ] Run focused tests and full CI before marking any implementation item complete.

CI run 44610074419 passed on the earlier documentation-registration head 32e0537048b1c82714e982c56fe6998ce6117703, including contract tests, Rhino tests, Debug APK assembly/upload and best-effort live source smoke steps. It is not the final status of later documentation-only commits and does not prove physical-device installation or full Legado migration.

The CI run 44610074419 passed on the earlier documentation-registration head 32e0537048b1c82714e982c56fe6998ce6117703; it is not the final status of later documentation-only commits. The later audit-document commits have their own workflow runs.

Additional lifecycle gap: clearSourceState currently clears the script stores, file cache and login-session store but does not clear SharedPreferencesSourceCookieJar. The next auth/session implementation must add per-source cookie cleanup for source removal/reset as well as removeLoginHeader, without affecting other sources.


## 2026-11-20 registered implementation gate — login/session credential safety

- [ ] Restrict source login-header propagation to pinned AnalyzeUrl same-site semantics; test same host, subdomain, cross-origin and CDN targets, including redirects.
- [ ] Synchronize Cookie values from putLoginHeader with the source-scoped cookie jar; clear the corresponding cookie state on removeLoginHeader and source reset.
- [ ] Encrypt persisted loginInfo using the pinned androidId-derived AES-compatible format; test legacy plaintext migration, encrypted round-trip, corrupt ciphertext and unavailable key handling without data loss.
- [ ] Prove two-source cookie/login isolation and secret redaction; preserve cancellation and existing JS scripts.
- [ ] Re-audit all callers/adapters and run focused tests plus full CI; record commit/run/artifacts before marking this gate complete.

Scope is limited to login/session security and lifecycle wiring. It does not close the upstream BaseSource model, batch persistence, AnalyzeUrl parity, WebView/UI callbacks, complete reading flow or physical-device validation. No runtime changes have been made as part of this registration.

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
