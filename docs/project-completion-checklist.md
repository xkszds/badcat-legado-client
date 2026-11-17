# 项目完成情况与剩余工作清单

更新时间：2026-11-17（第一大类与第二大类并行推进，UTC+8）

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
- [x] 阅读记录页：首页最近阅读面板显示最多三条，点击记录继续阅读，全部入口打开完整记录页；记录页长按可仅删除历史记录（不删除书签/收藏）。
- [x] 阅读 CSS 多方案、子选择器命名/备注、启停、复制、排序、预览、冲突提示、导入导出和资源上限。
- [x] CSS 选择器、净化器、规则顺序与输出大小的单元测试。
- [x] 先前基线提交 `e226dd6b559c` 的 CI 曾全部通过：契约测试、受限 Rhino 宿主测试、Android Debug APK 组装和产物上传均成功。Actions：[run #43845483951](https://github.com/xkszds/badcat-legado-client/actions/runs/43845483951)。该产物不是当前分支构建。
- [ ] 当前分支最新 UI/阅读记录改动仍需完整 CI 验证；最近一次可确认通过的构建是 d67dc03779ad 对应 Actions run #44061473137（该版本不包含后续首页布局改动）。

## 尚未完成（主要交付阻塞项）

### P0：必须完成才能称为目标客户端

- [ ] **原版 UI 还原**：逐页重建搜索首页/结果、书籍详情、目录、书架/文件夹、阅读页和设置页；以原 APK 资源和参考截图逐屏比对。首页已从空白占位改为真实最近阅读面板，搜索栏位置设置位于「设置 → 布局」，但整页结构和截图仍未达到原版验收。
- [ ] **完整 Legado 规则运行时**：补齐上游模型和规则实体、AnalyzeRule/AnalyzeUrl、CSS/XPath/JSONPath 等解析、JS 绑定、请求头/Cookie/缓存/代理等宿主环境，以及初始化/持久化依赖。当前 CSS 解析器和 Rhino 都只是有界子集。
- [ ] **端到端适配**：将完整引擎接入原版搜索、详情、目录、阅读和书架 UI，验证来源切换、错误呈现、章节导航和离线/缓存策略。
- [ ] **原版净化功能迁移**：通过方法级证据确认 ADBlockUtils、AdBlockPresenter、AdMark 服务、PPRule 的真实数据结构、调用时机与行为；分别实现 WebView/页面广告过滤、AdMark、目录/分页/视频规则。现有 Legado 正文替换不能代替这些功能。
- [ ] **真机验收**：在目标 Android 设备安装独立包，完成导入书源、搜索、详情、目录翻页、章节分页、阅读、CSS、书签、进度、异常/断网、净化开关等回归测试；截图核对 UI。
- [ ] **发布包**：完成包名/版本/权限/崩溃检查、正式签名流程、可复现 Release 构建和安装升级验证。当前产物是 Debug APK。

### P1：分发前必须完成

- [ ] **GPL-3.0 对应源码交付**：将 CI 下载的上游 Rhino JAR/扩展源码及修改记录纳入可复现的源码交付方案，提供构建脚本、许可证、版权和修改声明。
- [ ] **第三方资产与许可证审查**：核对图标、字体、图片、依赖库和原版资源使用边界。
- [ ] **安全与稳定性验收**：审计 JS 沙箱逃逸面、资源上限、网络重定向、文件导入、WebView、存储迁移和异常恢复。

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

- Search position settings are nested under Settings → Layout. The home search top spacing defaults to 46dp and offers 34/46/58dp presets; old unsupported values are migrated to 46dp. MainActivity refreshes its header when returning from layout settings.
- The home search entry navigates to the dedicated book-search page and requests focus/keyboard display. The results page reuses one search bar and has no duplicate “搜索” title.
- The formerly blank home content region now shows up to three real recent-reading records and links to the full history page; no removed leaderboard/lottery/cash/Cat Star shortcuts are restored.
- Latest verified CI success: commit d67dc03779ad5c978a6f6bd87f931d6fe2f5f13d, run [#44061473137](https://github.com/xkszds/badcat-legado-client/actions/runs/44061473137). This run predates the home spacing and recent-reading changes; current HEAD must pass a fresh full run before its APK is offered.
- Remaining work is unchanged at the major gates: original screenshot/device parity, full upstream AnalyzeRule/AnalyzeUrl and host APIs, long-press AdMark and DOM deletion, complete source corpus compatibility, real-device regression and release signing.
