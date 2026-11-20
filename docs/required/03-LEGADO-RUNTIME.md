# 第三类必读清单：Legado 运行时总框架与分级任务树

> **唯一入口：** 每次第三类任务先读 `00-MASTER-WORKFLOW.md`，再读本文件。本文按“总框架流程 → 大类 → 小类 → 程序/模块 → 验收门禁”组织；后半部分保留按日期记录的原始审计证据。
> **固定基准：** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`；GPL-3.0；只允许在 `integration/legado-upstream-runtime` 工作，禁止改 `main`。

## 最高优先级硬门槛：完整 Legado 书源运行层 + 坏坏猫产品外壳（不可降级）

这是第三类所有子任务的先决条件，优先级高于 B′、模块拆分、Room/迁移和任何局部实现进度。

1. **Badcat 产品层不换：** 原版坏坏猫搜索 UI、其他页面、净化系统、书架/收藏、书签、阅读器、CSS/排版和交互保持坏坏猫自身实现。Legado 不提供替代 UI/Reader。
2. **Legado 书源引擎必须按固定上游完整对齐：** 固定基准 `330e606c4d57e08adc056ff8ab7a4132c784da9a`。书源格式/字段/默认值/序列化、所有书源规则类型、AnalyzeRule/AnalyzeUrl、Rhino 与全部书源相关 JsExtensions/overload/jsLib、HTTP/请求配置/响应处理、Cookie/登录/源变量/会话、BaseSource 生命周期、搜索/发现/分页/详情/目录/正文/多页正文/批量内容、缓存及其依赖模型/工具/服务都纳入兼容范围。
3. **完整不是只跑通一条链：** Search → Detail → TOC → Content 的成功只是必要条件。还要证明所有书源公开 API、规则执行语义、边界行为和生命周期与固定上游一致。每个符号/能力必须登记：上游路径与签名、Badcat 接入点、实现来源、差异、测试证据、状态。
4. **只接受真实上游实现或经验证的等价实现：** 优先复用固定 SHA 源码和完整依赖闭包；适配器必须薄。禁止 shadow DTO 伪装官方模型、同名简化方法、selector-only、no-op、空返回、假成功、测试替身进入生产路径。不能完整实现时必须显式阻断，不得降级宣称兼容。
5. **模块架构为兼容性服务：** 如果当前 Android-free `:integration-contract`、B′ Badcat 自有数据方案或现有 host projection 阻碍完整上游闭包，必须重新审查并比较 Android Runtime 模块、受控上游核心模块和 Hybrid；不得为了保住 B′ 让 Legado 语义缩水。不得未经迁移/回滚审查就直接搬入整套 AppDatabase 或 Room schema。
6. **范围排除：** 不搬 Legado UI、书架产品流程或 Reader UI；RSS/TTS/音视频等只有在固定书源链路实际依赖时才纳入。坏坏猫阅读器只消费章节正文与必要元数据，负责自身净化、排版、CSS 和阅读交互。
7. **完成门禁：** 书源 API 符号覆盖清单 100% 盘点；模型/规则/JS/HTTP/Cookie/登录/生命周期差分契约；脱敏回放；多种真实书源全链路；Android 集成及 Badcat 原 UI 回归。任何未覆盖能力均阻止“完整兼容”结论。

## A. 第三类总框架流程（从源配置到可恢复阅读）

第三类不是把 Legado 的文件整包搬进来，而是让 Badcat 原版 UI 通过受控适配层，使用固定版本的 Legado 兼容运行时。必须按以下顺序推进，不得跳过前置门禁：

```text
0. 固定基线 / 上游差异 / 架构门禁
        ↓
1. 数据模型与序列化（BookSource / Book / BookChapter / Rule / ReadConfig）
        ↓
2. 书源解析规则（AnalyzeRule / AnalyzeUrl / CSS / XPath / JSON / 正则 / 替换）
        ↓
3. JS 运行时（Rhino / JsExtensions / BaseSource / JS 生命周期）
        ↓
4. 网络与会话（HTTP / Cookie / 登录 / Header / 重定向 / 限流）
        ↓
5. 书源业务链（导入 → 搜索/发现 → 详情 → 目录 → 正文）
        ↓
6. 章节身份与批量正文（index / URL alias / BatchContentContext）
        ↓
7. 缓存与持久化（章节缓存 / token fence / CacheManager / 失效）
        ↓
8. 阅读状态（进度 / 书签 / 历史 / ReadConfig / ParagraphAnchor）
        ↓
9. B′ 五域 PoC（单一事实源 / 单写者 / 事务 / 迁移回滚 / 重启）
        ↓
10. 自动化验收（契约 / 架构 / 回放 / 真实源 / 真机 / APK）
        ↓
11. 与第一、二类产品集成并完成发布验收
```

**依赖原则：** 前一阶段未满足契约，不得把后续阶段标记为完成。比如，章节 URL 可以用于导航，但不能替代 `(bookUrl, chapterIndex)` 身份；能搜索或能显示正文，不代表完整 Legado 生命周期已接通。

## B. 大类与小类任务树

### 大类 0：架构、固定上游与项目治理

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 0.1 固定版本基线 | pin SHA、GPL 来源、上游目录和构建版本 | 每个实现都可追溯到固定 SHA |
| 0.2 模块边界 | Badcat UI / Repository / Compatibility Facade / Runtime 分层 | Runtime 不引用 Badcat DAO；UI 不引用官方 DAO |
| 0.3 上游差异治理 | API、overload、模型字段、Room、JsExtensions 差异报告 | 自动生成报告、人工批准升级 |
| 0.4 退出/回退方案 | B′、限定 A、Hybrid 比较及退出条件 | 适配层泄漏或迁移不可逆时停止扩张 |

### 大类 1：数据模型、身份与序列化

| 小类 | 工作内容 | 主要对象 |
|---|---|---|
| 1.1 书源模型 | BookSource 字段、六类规则、BaseSource 生命周期、customOrder | BookSource / BookSourceCodec |
| 1.2 书籍模型 | Book 元数据、来源归属、用户覆盖字段、变量 | Book / ReadConfig |
| 1.3 章节模型 | BookChapter 完整字段、章节排序、重复 URL | BookChapter / chapter index |
| 1.4 规则序列化 | object/string 双形态、null/missing/default、未知字段 | Explore/Search/BookInfo/TOC/Content/Review |
| 1.5 阅读配置模型 | ReadConfig、ParagraphAnchor、日期与嵌套字段 | Book.ReadConfig |
| 1.6 身份与映射 | host projection ↔ upstream model、去重、冲突、来源范围 | sourceId / bookUrl / chapterIndex |

### 大类 2：规则解析与内容处理

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 2.1 AnalyzeRule | CSS、XPath、JSONPath、正则及上下文选择 | 与固定上游逐项差分 |
| 2.2 AnalyzeUrl | URL 模板、相对地址、变量、脚本、请求配置 | URL 语义与上游一致 |
| 2.3 规则上下文 | 变量作用域、父子节点、替换链、错误传播 | 无静默空结果/假成功 |
| 2.4 正文处理 | 分页 nextContentUrl、正文清洗、替换、图片/资源 | 章节完整且可重放 |
| 2.5 目录处理 | TOC 解析、排序、卷/付费/VIP/更新时间 | 不把 URL 当唯一身份 |

### 大类 3：JS 与书源运行时生命周期

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 3.1 Rhino 核心 | Context、执行隔离、超时、资源释放 | 沙箱边界和异常可测 |
| 3.2 JsExtensions API | 所有公开声明、参数、返回值、overload | 固定上游 API 覆盖率可量化 |
| 3.3 JS Bridge | Java/JS 对象桥接、source scoped bridge | 兼容旧 caller，无 ABI 偷换 |
| 3.4 BaseSource | 登录、源变量、Cookie、CacheManager、限流、jsLib | 完整生命周期，不是空接口 |
| 3.5 上下文传播 | SharedJsScope、协程/线程、回调和取消 | 请求间不串源、不泄漏上下文 |
| 3.6 辅助能力 | 加解密、编码、文件/压缩、字体、视频、WebView/UI 回调 | 逐项实现真实行为，不用 no-op 补 API |

### 大类 4：HTTP、Cookie、登录与安全

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 4.1 HTTP 基础 | GET/POST/HEAD、超时、取消、响应体 | 参数和错误语义匹配 |
| 4.2 重定向 | 普通跳转、下载、Ajax、循环和策略 | 不丢 header/cookie，循环受控 |
| 4.3 Cookie/会话 | 域隔离、同步、清除、持久化 | 源间隔离，重启行为明确 |
| 4.4 登录 | loginUrl/loginUi/loginInfo、加密和迁移 | 登录态可恢复，敏感数据安全 |
| 4.5 安全限制 | JS 时间/内存、HTTP 策略、文件和缓存上限 | 默认开启硬限制 |

### 大类 5：书源业务链与服务编排

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 5.1 书源目录 | 导入、启停、排序、导出、未知字段保留 | 原始 JSON 不被无意丢失 |
| 5.2 搜索/发现 | search、explore、分页、去重 | 请求、解析、会话统一 |
| 5.3 书籍详情 | detail、元数据覆盖、TOC URL 初始化 | 字段更新规则清晰 |
| 5.4 目录服务 | TOC 获取、分页/下一目录、章节对象 | 保留 index 和完整对象 |
| 5.5 正文服务 | 多页正文、替换、错误和离线回退 | typed chapter identity 全链路 |
| 5.6 统一编排 | Search → Detail → TOC → Content 生命周期 | source/session/script/http/cookie 一致 |

### 大类 6：章节身份、批量正文与导航兼容

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 6.1 章节身份 | `(bookUrl, chapterIndex)`；sourceId 为 host 隔离维度 | 同 URL 不同 index 不混淆 |
| 6.2 URL alias | 原始/绝对/规范化 URL 映射 | 只有唯一匹配才接受 URL-only |
| 6.3 旧链接兼容 | 历史/书签只存 URL 时重新查 TOC | 缺失或歧义时 fail closed |
| 6.4 BatchContentContext | URL 候选集、chapter object 精确匹配、缺失重试 | 与 pinned upstream 差分测试 |
| 6.5 章节顺序与筛选 | 搜索、倒序、翻页不改变真实 index | UI 显示序号不覆盖身份 |

### 大类 7：缓存、版本令牌与持久化

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 7.1 章节正文缓存 | 按 sourceId/bookUrl/index 存储正文 | 同 URL 不同 index 隔离 |
| 7.2 SaveToken/Fence | 请求开始取 token、替换/失效递增版本 | 旧异步写不能覆盖新正文 |
| 7.3 缓存失效 | 删除、替换、资源刷新、目录更新 | 所有 mutation caller 都触发失效 |
| 7.4 通用 CacheManager | source/runtime key-value、TTL、LRU、持久化 | 与章节正文缓存分域 |
| 7.5 存储原子性 | AtomicFile/数据库事务/容量/清理 | 崩溃恢复、上限、并发正确 |
| 7.6 迁移兼容 | 旧 URL key、旧文件、旧 JSON | 不猜 index；安全冷启动/回滚 |

### 大类 8：阅读状态与产品数据映射

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 8.1 阅读进度 | 当前章节 index、position、time、volume | 重启恢复且单写者 |
| 8.2 书签 | 章节身份、滚动位置、备注 | 旧 URL 可唯一解析 |
| 8.3 阅读历史 | 书籍身份、章节位置、更新时间 | 不与进度混为一个数据域 |
| 8.4 ReadConfig | 书籍级阅读配置、段落锚点 | 与全局 ReadBookConfig 区分 |
| 8.5 收藏/书架映射 | source/book identity、去重、导入导出 | 与 Book 主记录不双写 |

### 大类 9：B′ 五域持久化 PoC 与迁移

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 9.1 五域事实源 | BookSource / Book / BookChapter / Progress / Chapter Cache | 每域唯一事实源和唯一 writer |
| 9.2 事务边界 | 目录刷新、书籍更新、进度与缓存协调 | 无跨 store 假原子 |
| 9.3 架构守卫 | 模块依赖测试、静态扫描 | 禁止依赖违规 |
| 9.4 迁移工具 | 备份、dry-run、幂等、失败回滚 | 旧数据可完整恢复 |
| 9.5 重启恢复 | 进程重启、缓存恢复、进度续读 | 录制回放可重复 |
| 9.6 退出评估 | B′ → 限定 A → Hybrid | 触发条件时停止，不强行推进 |

### 大类 10：质量验证、真实源与交付

| 小类 | 工作内容 | 验收条件 |
|---|---|---|
| 10.1 单元/契约测试 | 字段、序列化、规则、身份、token | 固定上游 differential fixtures |
| 10.2 架构测试 | DAO/模块依赖隔离 | CI 强制失败 |
| 10.3 录制回放 | 脱敏请求/响应、异常和边界 | 不依赖站点实时稳定性 |
| 10.4 真实书源 | 搜索→详情→目录→正文→缓存 | 区分站点故障和引擎缺陷 |
| 10.5 Android 真机 | 导入、阅读、重启、恢复、兼容 | 原版 Badcat UI 无回归 |
| 10.6 CI/APK/manifest | 测试报告、版本清单、APK hash | exact HEAD 对应产物 |
| 10.7 发布验收 | GPL、来源、许可证、报告、哈希 | 所有前置门禁通过 |

## C. 已实现程序归类（按代码能力归位，不按提交时间排列）

> 状态含义：**已落代码**不等于已验收；**局部实现**不等于上游完整兼容；**待 CI 验证**不等于测试通过。此表只列有代码路径或明确历史证据的程序，不把计划/接口空壳算作完成。

### C1. 大类 1：数据模型、身份与序列化

| 小类 | 已落地程序/文件 | 当前状态与边界 |
|---|---|---|
| 1.1 书源模型 | `integration-contract` 的 BookSource projection；`LegadoBookSourceCodec`；`FileBookSourceCatalog` | 局部实现；raw JSON 可保留，非官方 mutable BookSource/BaseSource |
| 1.4 规则序列化 | 六类 rule deserializer + `INITIAL_GSON/GSON/GSONStrict`；`app/src/main/java/io/legado/app/utils/GsonExtensions.kt` | 源码与 pin 已核对一致；尚非完整 Book/BookSource/ReadConfig 序列化 |
| 1.4 规则序列化测试 | `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt` | 已补齐两处 fixture 编码错误；对应代码提交 `e278d9f` 的 CI 运行曾被取消，必须重新获得通过证据 |
| 1.6 身份投影 | `BookCandidate` / `BookDetail` / `BookChapter(index,title,url)` | 局部 projection；BookChapter 官方字段未覆盖，不能当官方实体 |

### C2. 大类 2：规则解析与内容处理

| 小类 | 已落地程序/文件 | 当前状态与边界 |
|---|---|---|
| 2.1/2.2 规则执行 | AnalyzeRule/AnalyzeUrl 相关选定上游源和 host 适配 | 局部实现；selector/context/replacement/JS/request/response 语义仍需逐项差分 |
| 2.4 正文处理 | `SourceChapterContentService`、CSS 正文 parser、文本替换接口、nextContentUrl 分页 | 有可运行服务路径；不能等同完整官方 ContentProcessor |
| 2.5 目录处理 | `SourceTocService`、TOC parser/章节投影 | 有服务路径；完整上游字段、批量和 URL alias 语义未验收 |

### C3. 大类 3：JS 与书源运行时生命周期

| 小类 | 已落地程序/文件 | 当前状态与边界 |
|---|---|---|
| 3.1 Rhino 核心 | `legado-rhino` / host Rhino runtime、沙箱和资源边界测试 | 有运行时和历史测试证据；须以当前 HEAD CI 重新验证 |
| 3.3 JS Bridge | source-scoped bridge、JS context propagation、Java/JS helper | 部分实现；`java.getSource()` ABI 冲突仍未解决 |
| 3.6 辅助能力 | 加解密、编码及部分 JS helpers | 局部实现；不得推断 118 declarations 已全部兼容 |

### C4. 大类 4：HTTP、Cookie、登录与安全

| 小类 | 已落地程序/文件 | 当前状态与边界 |
|---|---|---|
| 4.1 HTTP 基础 | GET/HEAD/POST timeout overload、取消/响应处理的既有实现 | 局部实现；需逐 API 对照固定上游 |
| 4.2 重定向 | redirect-aware GET、下载/archive/Ajax/selector 等历史修复 | 局部实现；需回归重定向链和 header/cookie |
| 4.3 Cookie/会话 | Cookie 同步/清除、同站 header 边界、源隔离 | 有历史实现和测试记录；当前 HEAD 未重新验收 |
| 4.4 登录 | 加密 loginInfo、登录态迁移/会话管理 | 局部实现；BaseSource 全生命周期仍缺 |
| 4.5 安全限制 | Rhino 资源边界、缓存大小上限、HTTP timeout | 部分能力已落地；统一硬限制和安全审计未完成 |

### C5. 大类 5：书源业务链与服务编排

| 小类 | 已落地程序/文件 | 当前状态与边界 |
|---|---|---|
| 5.1 书源目录 | `FileBookSourceCatalog`、`LegadoBookSourceCodec`、AtomicFile JSON 导入/导出/启停 | 局部实现；没有 Room/官方 DAO 事实源 |
| 5.2 搜索/发现 | `SourceSearchService`、`SourceExploreService`、`SourceReadingEngine` | 有 host 服务编排；脚本行为只支持已接通的范围 |
| 5.3 详情 | `SourceBookDetailService` | 有 host 服务路径；完整模型更新/用户覆盖规则未验收 |
| 5.4 目录 | `SourceTocService` | 有 host 服务路径；目录持久化与完整章节实体缺失 |
| 5.5 正文 | `SourceChapterContentService` | 有 host 服务路径；批量正文及官方 ContentProcessor 不完整 |
| 5.6 统一编排 | `SourceReadingEngine`、`BadcatReadingEngineFactory` | 已有 search/detail/TOC/content host orchestration；不代表完整 BaseSource/session lifecycle |

### C6. 大类 6：章节身份、批量正文与导航兼容

| 小类 | 已落地程序/文件 | 当前状态与边界 |
|---|---|---|
| 6.1 章节身份 | typed `chapterContent(BookChapter)`、`ChapterCatalogQuery.findByIdentity` | PoC-0.8 局部实现；index 已进入主要内容边界 |
| 6.2 URL alias | `ChapterCatalogQuery.findByUrl` | 仅唯一 URL 匹配成功；重复 URL 返回 null，尚未完整实现上游规范化 alias 候选映射 |
| 6.3 旧链接兼容 | `SourceChapterActivity.loadInitial` 的 legacy URL-only resolve | PoC-0.9 已落代码；旧历史/书签需唯一 TOC 匹配，缺失/歧义 fail closed |
| 6.4 BatchContentContext | 当前仅有上游契约审计和差分测试要求 | 未完整移植/接线；URL alias 候选列表、batch save/retry 仍缺 |

### C7. 大类 7：缓存、版本令牌与持久化

| 小类 | 已落地程序/文件 | 当前状态与边界 |
|---|---|---|
| 7.1 章节正文缓存 | `ChapterContentCache`、`FileChapterContentCache` | 已按 sourceId/bookUrl/chapterIndex 建 typed cache key；旧 URL 文件不猜 index |
| 7.2 SaveToken/Fence | `ChapterContentSaveFence`、`ChapterContentSaveToken`、`ChapterContentCacheKey` | 已有进程内版本 fence 和过期写拒绝；不是跨进程 durable token |
| 7.3 缓存失效 | `invalidate(sourceId,bookUrl,index)`、`replace` fencing | 基础 API 已有；当前产品无独立正文编辑/删除入口，所有 mutation caller 尚未闭环 |
| 7.4 通用 CacheManager | pinned 上游契约已审计 | 未完整实现；不要与章节正文缓存混为一谈 |
| 7.5 存储原子性 | `AtomicFile`、共享 Mutex、LRU/pruning、容量限制 | 局部实现；不代表五域统一事务 |

### C8. 大类 8：阅读状态与产品数据映射

| 小类 | 已落地程序/文件 | 当前状态与边界 |
|---|---|---|
| 8.1 阅读进度 | `BadcatReadingStateRepository` + `ReadingProgressStore` | 已抽出 Badcat 自有读写边界并保留旧 preference key；尚未映射官方 Book.ReadConfig/完整进度字段 |
| 8.2 书签 | `BadcatReadingStateRepository` bookmark methods | 局部持久化；旧 URL key 保持兼容，chapter index 迁移未完成 |
| 8.3 阅读历史 | repository facade + `ReadingHistoryActivity` | 历史列表有独立存储/投影；不能当作权威阅读进度 |
| 8.4 ReadConfig | pinned 源字段/依赖已审计 | 官方 Book.ReadConfig/ParagraphAnchor 尚未集成；禁止 shadow DTO 冒充 |
| 8.5 收藏/书架映射 | Favorites JSON owner / activity | 独立数据域；未并入统一 Book 事实源 |

### C9. 大类 9：B′ 五域持久化 PoC 与迁移

| 小类 | 已落地程序/证据 | 当前状态与边界 |
|---|---|---|
| 9.1 五域事实源 | 五域字段/identity mapping、当前 owner/caller inventory | 调查/映射已做；单一事实源和唯一 writer 未证明 |
| 9.2 事务边界 | AtomicFile/SharedPreferences/file cache ownership audit | 多 store 并存；没有跨域事务 |
| 9.3 架构守卫 | 模块边界调查 | 自动架构测试未完成 |
| 9.4 迁移工具 | dry-run/backup/idempotency/rollback 设计 | 只有设计；未实现/演练 |
| 9.5 重启恢复 | 旧 preference/file 行为和 cache restart 设计 | 未完成五域统一重启验证 |
| 9.6 退出评估 | B′、限定 A、Hybrid 比较 | 已比较但未批准切换；B′ 未通过 |

### C10. 大类 10：质量验证、真实源与交付

| 小类 | 已落地程序/证据 | 当前状态与边界 |
|---|---|---|
| 10.1 单元/契约测试 | Gson fixtures、chapter identity/fence tests、Rhino/crypto 历史 tests | 有测试代码；最新整套测试尚未验证通过 |
| 10.2 架构测试 | 边界要求和设计记录 | 未完成 |
| 10.3 录制回放 | 真实源历史 E2E 证据 | 系统化脱敏录制回放框架未完成 |
| 10.4 真实书源 | Bixiange/Pinshuzhai 历史搜索→详情→TOC→正文证据 | 只对对应历史 commit 有效，不外推当前 HEAD |
| 10.5 Android 真机 | 历史 APK/设备以旧 commit 为准 | 当前 PoC 未完成真机验收 |
| 10.6 CI/APK | 历史 workflow 和 artifact 记录 | 最新代码提交 `e278d9f` 对应 workflow `44703572220` 曾被取消；最新文档 workflow 不代表代码验证。当前无本次修正的通过证据 |
| 10.7 发布验收 | GPL/固定 pin/发布门禁 | 未完成；不得称正式发布就绪 |

## D. 全局验收门禁（任何小类都必须遵守）

1. **先登记再改代码：** 日期、起始 HEAD、上游 SHA、精确文件、所有 caller、事实源/事务、风险、测试、验收和退出条件。
2. **真实实现：** 禁止 selector-only、no-op、null/unsupported 占位、同名简化 API、测试专用替身冒充生产能力。
3. **字段与身份：** null/missing/default/unknown、序列化 round-trip、规则 object/string 双形态、重复 URL、chapter index 都要有契约测试。
4. **数据边界：** Runtime 不引用 Badcat DAO；UI 不引用官方 DAO；每个数据域明确唯一 writer。
5. **缓存：** 区分章节正文缓存与通用 CacheManager；token、invalidate、replace、容量和 TTL 分别验收。
6. **测试证据：** focused tests → 全量 tests → CI exact HEAD → APK/artifact hash → 真实源 → Android 真机；任何一步未做都明确标记。
7. **状态不得越级：** 文档已登记 ≠ 已实现；已落代码 ≠ 测试通过；测试通过 ≠ PoC 通过；Debug APK ≠ Release 就绪。

## E. 当前总状态与下一顺序

- **总体：** 第三类为“部分实现”；完整 Legado 运行时未完成；B′ 五域 PoC 未通过。
- **模型/序列化：** 六类规则和 Gson 有源代码/测试夹具；Book/BookSource/BookChapter/ReadConfig 官方完整模型未集成。
- **章节身份：** PoC-0.8/0.9 已有实现；URL alias 的完整规范化和 BatchContentContext 未完成。
- **缓存：** PoC-1.0 的 index key 和 in-process fence 已落代码；缓存 mutation 生命周期和 CI 验收未完成。
- **最新修正：** Gson 测试 fixture 修正已提交，提交 `e278d9f3715e2b4b9aeced1d897857c47c591e95`；对应运行 `44703572220` 被取消，不能宣称通过。最新清单提交为当前 branch HEAD。
- **禁止启动：** 在模型闭包/序列化、迁移和 rollback 门禁未通过前，不冻结 schema v1、不引入 Room cutover、不切换架构。
- **接下来优先顺序：**
  1. 重新运行最新代码 HEAD 的 CI，确认 Gson fixture 修正后测试结果。
  2. 根据真实 CI 结果修复下一项阻断，仍需先精确登记。
  3. 完成 ReadConfig/ParagraphAnchor 和官方模型闭包的可执行契约方案，不引入 shadow DTO。
  4. 补齐章节 URL normalization/alias ambiguity differential tests 与 BatchContentContext 行为。
  5. 完成缓存 mutation caller audit 和失效闭环。
  6. 再进入五域唯一事实源、迁移 dry-run/rollback 和架构守卫。
  7. 最后做真实书源、Android 真机和 APK 验收。

## F. 持续任务记录与历史证据

## 2026-11-20 必读清单集中化记录

- 将历史 Legado 程序、测试、固定 SHA、模型/服务缺口、B′ 五项 PoC、退出条件和自动维护要求集中到本文件。
- 详细审计资料仍保留为证据，不再要求每次 Legado 任务全部重读；例行入口仅为总流程清单 + 本文件。
- 本次仅整理文档，没有新增/修改 Legado runtime 代码，也没有新增测试或 APK。整理开始时的代码基线为 `11549e7824683553c4039befa2f714fabfb805c3`；不得将历史 CI 结果外推到后续 HEAD。


## 2026-11-20 PoC-0 registration — five-domain contract and ownership baseline (before runtime edits)

- **Starting HEAD:** `493cbd16f0b68d29d8c8eed466f542f5e0932da4`; active branch confirmed as `integration/legado-upstream-runtime`.
- **Pinned source:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Task/category:** Third category — B′ minimum PoC, baseline gate.
- **Exact scope:** Inspect and document the five PoC domains (BookSource, Book, BookChapter, reading progress, chapter cache): upstream field/identity/default/serialization contracts; current Badcat persistence and all existing model/codec/cache/progress interfaces; proposed single-writer ownership, adapter boundary, transaction and invalidation rules; migration/rollback proof plan. This first slice is an evidence and contract baseline, not a claim that B′ is implemented.
- **Target surfaces to audit:** `:integration-contract` ReadingEngine/BookSource/BookChapter/ChapterContentCache; LegadoBookSourceCodec and tests; app FileBookSourceCatalog, FileChapterContentCache, ReaderProgressMetrics and reader persistence callers; Gradle module/dependency graph; pinned upstream BookSource/Book/BookChapter, BaseSource/BaseBook, relevant DAO/cache/content persistence declarations.
- **Ownership hypothesis to challenge, not assume:** Badcat repository is the sole writer for each adopted domain; UI consumes Badcat projections; Legado runtime receives only typed compatibility interfaces/models and never imports Badcat DAO or storage classes. Do not decide the final database engine until the five-domain transaction and migration requirements are evidenced.
- **Risks:** source/book/chapter identity collision; URL uniqueness assumptions; host progress/reader state split; chapter cache key and upstream save-token mismatch; raw JSON vs typed rule object/string semantics; unknown-field loss; partial import; duplicate writes; cross-file/DB transaction impossibility; AtomicFile crash recovery; migration rollback; runtime dependency leakage.
- **Required evidence/tests for this slice:** complete field/identity mapping table; current caller inventory; source-code boundary inventory; proposed writer/read projection table; fixture requirements for object/string rule JSON, missing/null/default/unknown fields; deterministic migration dry-run/idempotency/rollback/restart scenarios; architecture test design and explicit fail conditions. No code edit is permitted until these contracts and target callers are sufficiently mapped.
- **Acceptance/exit:** This slice is accepted only when all five domains have named source-of-truth, sole writer, stable identity, transaction boundary, cache invalidation/recovery semantics, and a testable migration path. If a domain cannot be isolated or restored, stop B′ expansion and prepare the restricted data-layer A / hybrid comparison. No APK/release claim.
- **Explicit exclusions:** no UI changes; no changes to main; no runtime/API implementation, Room migration, cache rewrite, reader progress rewrite, selector/Rhino work, or source compatibility claims in this registration.


### PoC-0 investigation results — 2026-11-20 (evidence from pinned source and current host)

**Current host facts (HEAD at registration):**
- `:integration-contract` is the platform-neutral contract and currently defines projection-only `BookSource`, `BookCandidate`, `BookDetail`, `BookChapter(index,title,url)`, and `ChapterContent`. It is not an upstream entity module.
- `LegadoBookSourceCodec` retains the complete source JSON in `rawJson`, but its typed rule properties are nullable JSON strings; `objectOrNull` accepts object or stringified object and normalizes both to an object JSON string. The typed projection applies defaults while retaining raw JSON for export. This is staging preservation, not upstream mutable-model parity.
- `FileBookSourceCatalog` is the only currently inspected source catalog persistence implementation: one `AtomicFile` JSON document, process-wide Mutex, keyed by sourceId/bookSourceUrl. Import replaces matching IDs in place; setEnabled rewrites one field; export serializes stored raw JSON. No Room/DAO is present in `app/build.gradle.kts` or the current app module dependency list.
- `FileChapterContentCache` is a separate AtomicFile-per-chapter store under `filesDir/legado_chapter_cache`; key is SHA-256 of length-prefixed (sourceId, bookUrl, chapterUrl); per-process Mutex; 4 MiB entry limit and 64 MiB cache budget. It stores parsed/purified text and savedAt only. It does not currently carry upstream chapter-index identity or ContentSaveToken/version fencing.
- `ChapterContentCache` is only a get/put interface keyed by sourceId + bookUrl + chapterUrl.
- `ReadingHistoryActivity` stores history rows in SharedPreferences (`badcat_reading_history/records`) keyed for replacement by sourceId + bookUrl; it is a history list, not a durable per-book reading-position model. `ReaderProgressMetrics` is pure scroll/page math, not persistence. The inspected source set therefore does not establish an authoritative persisted reading-progress record for the five-domain PoC.
- The host UI/runtime consumes projections and file-backed services; no evidence currently proves a single transactional owner spanning source, book, chapter, progress and chapter content.

**Pinned upstream contracts verified:**
- `BookSource`: Room table `book_sources`; primary key `bookSourceUrl`; nested six rule entities use explicit Gson TypeConverters; mutable source model implements `BaseSource`; identity/equality/hash use `bookSourceUrl`; lazy accessors instantiate default mutable rule objects when null; content batch maximum is 50.
- `Book`: Room table `books`; primary key `bookUrl`; unique index `(name, author)`; includes source origin, metadata, custom overrides, book variables, readConfig, and reading progress fields `durChapterIndex`, `durVolumeIndex`, `chapterInVolumeIndex`, `durChapterPos`, `durChapterTime`; equality/hash use `bookUrl`. It also has transient runtime properties and methods that depend on app DB, BookHelp, ContentProcessor and AppConfig, so copying the entity alone would not supply its behavior.
- `BookChapter`: Room table `chapters`; composite primary key `(url, bookUrl)`; unique index `(bookUrl, index)`; foreign key to Book with cascade delete. It carries baseUrl, index, volume/VIP/pay, resourceUrl, tag, wordCount, position/fragments, variables and image metadata. Equality/hash compare URL only, while batch save identity is chapter index within a book. Duplicate chapter URLs are explicitly supported and therefore URL-only cache/batch identity is unsafe.
- `BatchContentContext`: maps URLs to lists of candidate chapters; accepts chapter object or uniquely matching URL; rejects ambiguous URL and numeric index; applies the upstream content replacement rule; uses per-index `ContentSaveToken`; tracks saved indexes under a lock and exposes missing chapters for normal retry.
- `BookHelp.ContentSaveFence`: save key is `(bookUrl, chapterIndex)`; monotonically increasing version prevents stale in-flight content from overwriting a newer chapter cache. A normal host cache timestamp is not an equivalent token.
- `CacheManager`: separate generic source/runtime key-value cache with memory LRU, DB persistence and TTL; this is distinct from the chapter content file cache. Do not conflate the two cache domains.

**Five-domain ownership status (no final architecture assumption):**

| PoC domain | Current observed host owner | Upstream identity/semantics | PoC decision still required |
|---|---|---|---|
| BookSource | FileBookSourceCatalog / AtomicFile JSON | `bookSourceUrl`; mutable BaseSource lifecycle; typed rules + Gson converters | Keep source JSON lossless while defining sole writer and runtime source object lifecycle |
| Book | No confirmed durable host Book entity in inspected paths; UI uses candidate/detail/favorite projections | `bookUrl`; unique `(name,author)`; source origin and many user/runtime fields | Decide durable Badcat Book record and how favorites/bookmarks/history relate without duplicate writers |
| BookChapter | TOC list and chapter service projections; no confirmed host chapter DAO | PK `(url,bookUrl)`, unique `(bookUrl,index)`; duplicate URL allowed | Define canonical book identity, chapter order, duplicate URL handling and update/refresh transactions |
| Reading progress | History SharedPreferences observed; scroll metrics are calculation only | Progress fields stored on upstream Book row, index + position + timestamp | Locate all reader progress writers/callers; define atomic single-writer progress record and restart restoration |
| Chapter cache | FileChapterContentCache / AtomicFile | Per-book chapter index, content save version token/fence, content files; separate CacheManager TTL KV | Add index-aware identity and stale-write fencing; define invalidation when TOC/content changes |

**Boundary assessment:** B′ is not yet demonstrated. Current source catalog, history and content cache are independent persistence mechanisms. The five-domain source-of-truth and transaction model cannot honestly be declared solved. The next implementation must not add a second mutable Book/Chapter copy or dual-write UI state before the ownership decision is complete.

**PoC-0 result:** investigation is partially complete; acceptance is **not met**. Remaining blockers: enumerate every reader progress/favorite/bookmark writer and caller; inspect current full host TOC/content persistence path; decide whether the PoC needs one Room database or a transactional repository over existing stores; specify rollback for migration from current JSON/SharedPreferences/files; create a compile-time module boundary and executable architecture test. No runtime code changed and no test/CI/APK result is claimed in this audit.


**Repository verification for this investigation update:**
- Finding record commit: `7afa123476c7a3abce4dbbb3d2e921454b7faa04`.
- Branch ref rechecked and points to that exact HEAD.
- Combined status checks: empty; commit-associated workflow runs: empty. Therefore this documentation/investigation commit has no CI pass evidence.
- No runtime source, tests, build configuration, or APK were changed/generated in this slice.


## 2026-11-20 PoC-0.1 registration — complete reader-state ownership and compatibility facade

- **Starting HEAD:** `674da5dac6f148f30600ce9002a178abce14dd06`; branch `integration/legado-upstream-runtime`.
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** close PoC-0 investigation blockers by enumerating current Badcat persisted reader state and establishing a single repository/facade boundary for reading progress, per-chapter scroll positions, chapter bookmarks and reading history, while retaining existing SharedPreferences keys and UI behavior. Also map current favorite ownership, TOC lifetime and chapter-cache contract. This is a compatibility-preserving repository extraction, not yet a database migration or official Book entity implementation.
- **Verified call sites at starting HEAD:**
  - `SourceBookActivity.beginReadingOrQueue/showChapters`: reads last chapter URL from `badcat_reading_progress`; removes stale last-chapter URL when it no longer exists in current TOC; reads per-chapter bookmark keys to render book-level bookmark entry; deletes bookmark entries.
  - `SourceChapterActivity` (same source file): reads/writes last chapter URL; saves/restores per-chapter scrollY; saves/loads/deletes chapter bookmark position/title/book/time; invokes `ReadingHistoryActivity.save` after successful chapter load. Scroll saves are throttled to about 1 second and also occur on pause/chapter switch.
  - `BookmarkActivity`: enumerates/deletes bookmark keys and companion title/book/time keys in `badcat_reading_progress`; opens a bookmark at saved scroll position.
  - `ReadingHistoryActivity`: owns `badcat_reading_history/records`; save replaces one row by `(sourceId,bookUrl)`; remove/clear are history-only.
  - `FavoritesActivity`: owns favorites/folders in `badcat_favorites`; reads reading-history timestamps for sort. Favorite identity is `(sourceId,bookUrl)`, not just official `bookUrl`.
  - `SourceBookActivity` loads detail and TOC concurrently. The TOC is held in activity memory; no durable TOC repository was found in the inspected source. Search candidates/detail are projections, not persisted Book rows.
  - `FileChapterContentCache`: independent AtomicFile-per-entry cache keyed by length-prefixed `(sourceId,bookUrl,chapterUrl)`; payload is purified content + timestamp. `ChapterContentCache` exposes only get/put and has no chapter index, content version fence or explicit invalidation contract.
- **Correction to PoC-0 wording:** reading progress is not wholly absent. It is currently stored in `badcat_reading_progress` as last chapter URL plus per-chapter scroll offsets. Reading history is a separate `badcat_reading_history` list and is not itself the authoritative progress record. The earlier statement that no persisted progress record was established should be read as “no unified typed progress repository/official Book projection is established,” not “no progress persistence exists.”
- **Proposed single-writer rule for this slice:** all reads/writes of the existing progress/bookmark/history preference schemas must pass through one Badcat-owned `ReadingStateRepository` implementation. UI activities remain presentation-only consumers. Preserve existing preference file names, keys, key encoding and JSON shapes during extraction; repository is the sole writer. Runtime may depend only on a platform-neutral compatibility interface, never on Activity, SharedPreferences, or Badcat DAO types. Favorites remain a separate Badcat-owned domain and are not silently merged into reading progress.
- **Transaction semantics:** each logical mutation affecting bookmark value + title + book + timestamp must use one SharedPreferences Editor commit/apply batch. History replacement/clear is one JSON-array write. Progress last-chapter and scroll position are distinct keys in one preference file; no cross-file atomicity is claimed between history and progress. A successful chapter display may update progress and history sequentially; recovery must tolerate one update succeeding without the other.
- **TOC/Book identity:** UI/host book identity remains composite `(sourceId,bookUrl)` to prevent same URL across sources colliding. Chapter persistence/cache identity must include source + book + chapter index and URL; upstream BookChapter's URL-only equals semantics must not be reused as a unique storage key because duplicate chapter URLs within one book are supported. Current TOC remains ephemeral; no chapter DAO is introduced in this slice.
- **Risks:** preserve legacy key encoding and companion-key cleanup; avoid parsing non-bookmark integer preference keys; do not remove scroll positions when deleting history; do not remove bookmarks when clearing history; stale TOC URL cleanup must remain; bookmark jump and restore must use the exact existing pixel offset; favorite sorting depends on history timestamps; SharedPreferences apply is asynchronous and not a multi-file transaction; repository extraction must not accidentally put UI settings in the reading-state store.
- **Required tests:** repository contract tests for identity isolation, last-chapter read/write/clear, scroll save/restore, bookmark CRUD/clear and companion metadata cleanup, history upsert/remove/clear and timestamp ordering, malformed legacy JSON/key handling, restart/re-instantiation compatibility with pre-existing preference fixtures; regression tests for favorite sort and chapter URL resume/bookmark lookup. Verify no direct legacy progress/history preference writes remain outside repository after extraction.
- **Acceptance:** all identified UI callers use the repository; existing legacy preference data remains readable and new writes remain backward compatible; repository tests and existing cache/TOC tests pass; no UI hierarchy or navigation changes. This slice does not prove official Book/BookChapter persistence, migration rollback, durable TOC, upstream ContentSaveToken, full B′, or real-source/device acceptance.
- **Explicit exclusions:** no Room dependency/schema migration in this slice; no Book/BookChapter DB implementation; no chapter cache key rewrite/token fencing; no UI redesign; no main branch; no AnalyzeRule/Rhino/JsExtensions work.


### PoC-0.1 implementation record — Badcat reading-state repository extraction

- **Implementation commits:** repository `1a910df7b0369e7d1828314a9ed080ed350aaffd`; reader progress/bookmark caller migration `f852106fdce09b1ea6c9c7a3800c538ae4a38f83`, `ea9b9f99f6310be6b31e743fa82f9c61317b5f9b`; bookmark manager `e335180b4787ec0f01059e9418dac8967e2a4b71`; reading history `510f3adb744af8fb50fb0f215d9574a26637535a`; favorite sort projection `fe5179048b9de80e1fa196015da1289f6119f16a`; legacy helper cleanup `3de794e86e5c86017e8f474b055e3eb1101b05f5`; tests `8a1bad32115cd5142f67e63674f559f89f8ab681`; explicit editor cleanup `9926af5f9a6c09bf0f5a7da27893dcf8e8faf434`; platform-neutral progress port `7339ff92708aa82f3ce3930b86e80b542824e826`; repository implements that port `0029fffef7434bd3d6504fdc659851f4a93d16c6`.
- **New files:** `app/.../BadcatReadingStateRepository.kt`; `integration-contract/.../ReadingProgressStore.kt`; `app/src/test/.../BadcatReadingStateRepositoryTest.kt`.
- **Migrated callers:** `SourceBookActivity` now delegates resume URL, stale URL cleanup, bookmark presence/removal to the repository; `SourceChapterActivity` delegates last chapter URL, per-chapter scroll persistence/restore, bookmark save/jump and reading-history upsert; `BookmarkActivity` delegates list/remove/clear; `ReadingHistoryActivity` delegates list/remove/clear/save; `FavoritesActivity` obtains last-read timestamps through the repository. Reader visual settings continue using their existing preferences and are not moved into reading-state APIs.
- **Compatibility retained:** same `badcat_reading_progress` and `badcat_reading_history` preference files, Base64 key encoding, legacy prefixes, history JSON shape, per-book key `(sourceId,bookUrl)`, per-chapter scroll/bookmark key `(sourceId,bookUrl,chapterUrl)`, bookmark companion metadata cleanup, one-editor bookmark writes, last-read sorting fallback for non-positive/missing timestamps. No existing UI layout/navigation was intentionally changed.
- **New platform-neutral port:** `ReadingProgressStore` exposes only source-scoped last chapter URL and per-chapter scroll position. Runtime can depend on this interface, not Android SharedPreferences or Activity classes. Bookmark/history management remains a Badcat product/UI concern and is not exposed as Legado runtime DAO.
- **Tests added:** six Robolectric tests for legacy preference read + repository recreation, bookmark metadata CRUD, source/book identity separation, history upsert/clear isolation from progress/bookmarks, last-read timestamp fallback, and malformed history JSON.
- **Review note:** the repository currently preserves Badcat's existing URL-based progress representation. It does not yet translate to official `Book.durChapterIndex/durChapterPos/durChapterTime`; index mapping requires the authoritative chapter model/TOC identity and is still an acceptance gap. No Book/BookChapter durable tables or cache save-token fence were introduced.
- **CI evidence at record time:** code head `0029fffef7434bd3d6504fdc659851f4a93d16c6` has Actions run `44645504233` queued/pending; no jobs or test results were available when this record was written. Earlier pushed commits' runs were cancelled by newer pushes. Therefore tests are **not yet verified** and this slice is not accepted.
- **Remaining PoC blockers:** inspect/verify all progress/history/bookmark access sites in final tree; run focused and full app tests on exact head; verify restart/reopen behavior; decide Book and BookChapter durable persistence and source/book/chapter identity mapping; add chapter-index-aware cache identity and ContentSaveToken-equivalent stale-write fencing; build migration dry-run/idempotency/rollback plan. B′ PoC remains **not passed**.


## 2026-11-20 PoC-0.2 registration — Book/BookChapter identity, TOC durability and chapter-cache fencing audit

- **Starting HEAD:** `ff01352a4d5a2a22a454ef07fc008cf7eee808d5`; branch `integration/legado-upstream-runtime`.
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Task/category:** Third category — continue B′ five-domain PoC; audit and specify Book/BookChapter/TOC/cache identity before adding persistence.
- **Exact scope:** inspect current platform-neutral BookCandidate/BookDetail/BookChapter and ReadingEngine contracts; SourceTocService/SourceChapterContentService; FileChapterContentCache; SourceBookActivity and reading-state/favorite/history/bookmark callers; Gradle module boundary; exact pinned upstream Book and BookChapter entities and their identity/persistence semantics. Determine whether the existing TOC and cache can preserve upstream duplicate chapter URLs, stable chapter index, source-scoped book identity and stale-write fencing. Register a concrete implementation slice only after the ownership and migration design is testable.
- **Files/callers inspected:** `ReadingEngine.kt`, `ChapterContentCache.kt`, `SourceTocService.kt`, `SourceChapterContentService.kt`, `SourceBookActivity.kt`, `BadcatReadingStateRepository.kt`, `FileChapterContentCache.kt`, `FileBookSourceCatalog.kt`, app and integration-contract Gradle files, TOC/cache tests; upstream `Book.kt` and `BookChapter.kt` at the pinned SHA.
- **Observed host facts:** current BookCandidate/BookDetail are transient UI projections; no durable Badcat Book or BookChapter repository/DAO is present in the inspected app/module graph. SourceBookActivity keeps `allChapters` in Activity memory. SourceTocService merges paginated TOCs using `distinctBy { it.url }` and reindexes; its regression test explicitly codifies URL deduplication. SourceChapterContentService reads/writes cache by `(sourceId, bookUrl, chapterUrl)`, with no chapter index or save-version token. FileChapterContentCache persists purified content and savedAt in AtomicFile entries, bounded to 4 MiB each/64 MiB total; it has no invalidation/version-fence API. The cache is injected as an optional dependency; failure is best-effort.
- **Pinned upstream facts re-confirmed:** Book primary key is `bookUrl`, equality/hash are bookUrl-based, and the entity carries source origin, user overrides, read config and durable reading fields. BookChapter primary key is `(url, bookUrl)`, unique index is `(bookUrl, index)`, foreign key cascades on Book deletion; it has substantially more fields than the host projection. Upstream BookChapter equality/hash compare URL only, but URL is not a safe unique storage/cache identity: the schema permits the same URL under different books, and the per-book index is separately unique. Book/BookChapter entities call app DB/runtime helpers and cannot be copied into the Android-free contract as-is.
- **Compatibility defect/risk surfaced:** host TOC URL-only deduplication is stronger than the upstream schema contract and can discard a distinct chapter row that shares a URL. The cache key also collapses chapters sharing the same URL within one book, and there is no stale in-flight write fence. This conflicts with the pinned upstream index-based batch/save identity. The existing TOC test currently asserts URL deduplication, so changing this behavior requires explicit fixture and caller review, not a silent one-line edit.
- **Ownership conclusion:** do not add a standalone Book/Chapter AtomicFile store or a second Room database yet. Book favorites, reading-state preferences, in-memory TOC and chapter cache are separate current stores; no transaction/rollback boundary spanning the PoC domains has been demonstrated. The existing source catalog remains a separate source JSON store. B′ is still unproven.
- **Risks to close before implementation:** composite Badcat book identity `(sourceId, bookUrl)` vs upstream `bookUrl`; duplicate chapter URLs vs `(bookUrl,index)`; TOC refresh/reorder/removal and bookmark/progress index migration; cache invalidation when chapter identity/content changes; version-token monotonicity across process restart; AtomicFile crash recovery and migration backup/rollback; old cache compatibility; no runtime dependency on app DAO/SharedPreferences.
- **Required next implementation tests (not yet added):** duplicate URL chapters retained with distinct indices; stable identity for same URL across books and sources; TOC refresh/reorder/removal mapping; chapter cache key includes source/book/index plus URL; stale older write cannot replace a newer token; token invalidation/restart behavior; legacy URL-only cache read/migration policy; bounded storage and AtomicFile crash recovery; regression tests for reader resume/bookmark selection and all existing TOC pagination fixtures.
- **Acceptance/exit:** no implementation or B′ acceptance is claimed by this audit. Before code, choose and document the single durable owner for Book/BookChapter/progress/cache, transaction boundary and rollbackable migration. If a coherent owner cannot be established without dual-write or lossy identity, pause B′ expansion and compare restricted data-layer A / hybrid. No UI changes, no main branch, no APK/release claim.
- **Status:** audit slice completed at source-inspection level; implementation scope remains blocked on persistence/transaction design. No runtime source or tests changed in this slice.

### PoC-0.2 audit result — identity and cache gap confirmation

1. **Book:** no durable host entity/DAO identified; current candidate/detail objects are request/UI results, not official Book records.
2. **BookChapter:** host projection `(index,title,url)` is insufficient to represent upstream fields and persistence identity.
3. **TOC:** Activity-memory only; current URL-only deduplication can erase rows the upstream schema permits.
4. **Reading progress:** PoC-0.1 repository preserves the legacy URL-based preference representation but has not mapped it to Book's chapter index/position/time.
5. **Chapter cache:** durable AtomicFile exists, but key lacks index and version token; cache and TOC do not share an authoritative identity source.
6. **B′ decision:** five-domain ownership, transaction and migration rollback still not solved. Do not label PoC passed.

### Exact-head CI evidence checked during PoC-0.2

- Branch ref at inspection points to `ff01352a4d5a2a22a454ef07fc008cf7eee808d5`.
- Combined commit status for `ff01352a4d5a2a22a454ef07fc008cf7eee808d5`: no status checks reported.
- Commit-associated workflow evidence is not available for this docs/code head. The nearby successful workflow run `44644671323` belongs to old commit `674da5dac6f148f30600ce9002a178abce14dd06`, not this head; it cannot verify PoC-0.1 code or the current branch head.
- No current-head JUnit report, APK artifact, device run or new real-source result was verified. No test is marked passed by this record.


## 2026-11-20 PoC-0.3 registration — persistence ownership and rollback architecture decision

- **Starting HEAD:** `e223b102a8089b09feab30bd91087305b3d6d94b`; active branch `integration/legado-upstream-runtime`.
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Stage:** B′ PoC architecture design, before any database/runtime source edit.
- **Exact scope:** define a single durable metadata owner and write/transaction rules for the five PoC domains (BookSource, Book, BookChapter, reading progress, chapter-cache metadata); define legacy AtomicFile/SharedPreferences/cache migration, idempotency, backup, rollback, restart recovery and architecture boundaries. Correct any overstatement from PoC-0.2 by comparing the exact upstream primary/unique keys. No Room dependency, schema, migration or runtime implementation is added in this registration.
- **Inspected baseline:** master workflow and category-3 checklist re-read; upstream model/serialization audit and project migration evidence reviewed; branch ref verified at starting HEAD. Existing host has no Room database/DAO. Source catalog is a JSON array in AtomicFile; reading progress is in SharedPreferences behind `BadcatReadingStateRepository`; chapter list is held in Activity memory; chapter content payloads are stored in bounded AtomicFile entries; these stores do not share a transaction.
- **Architecture candidate selected for PoC implementation:** **B′ with a Badcat-owned Room database as the sole durable metadata owner**, not the upstream 26-entity/version-113 AppDatabase. Room owns BookSource raw JSON + indexed identity/state, Book metadata, BookChapter metadata, reading progress and chapter-cache metadata/version state. Keep the chapter body as immutable, bounded file payloads addressed by opaque content-generation IDs; Room is authoritative for which generation is current. The existing Badcat UI remains the presentation layer. A single compatibility facade translates exact pinned upstream models/semantics to these rows. This is a candidate decision to test, not a claim that B′ has passed.
- **Domain identity rules to implement/test:**
  1. BookSource identity: exact upstream `bookSourceUrl`; preserve the original JSON object and unknown fields in a raw JSON column. Mutations patch only explicitly changed members.
  2. Badcat Book identity: composite `(sourceId, bookUrl)` to preserve source isolation. Compatibility facade maps to upstream Book's `bookUrl` within a source context; never assume bookUrl is globally unique across Badcat sources.
  3. BookChapter persistence identity: `(sourceId, bookUrl, chapterUrl)`, matching the upstream per-book pair `(bookUrl,url)` plus Badcat source scope. Enforce a separate unique `(sourceId,bookUrl,index)`. Preserve all upstream chapter fields. URL-only equals/hash semantics are not used as a database/cache identity.
  4. Reading progress: one row per `(sourceId,bookUrl)`, containing last chapter identity/index, chapter position, read timestamp and a compatibility URL field during migration. The Room row becomes the sole writer; legacy preference values are migration input only.
  5. Chapter cache metadata: keyed by `(sourceId,bookUrl,chapterUrl)`, associated with current chapter index and a monotonically increasing generation/token. The body file is immutable and generation-addressed. A writer publishes a body only by a transaction that confirms its token is still current; stale writers cannot replace the active pointer. Cache metadata and TOC identity must be updated/invalidate together.
- **Important upstream-key correction to PoC-0.2 wording:** pinned `BookChapter` Room primary key is `(url,bookUrl)`; this prohibits two persisted rows with the same URL for the same book. The separate unique index is `(bookUrl,index)`. Therefore the earlier phrase suggesting the upstream schema permits duplicate chapter URLs *within one book* was too broad and is corrected here. URL-only deduplication in the host TOC must still be compared against the exact upstream extraction/merge behavior and tested for pagination/duplicate-page replay, but cannot be called an upstream schema violation solely because it deduplicates identical URLs. The host cache key already contains source + book + URL and therefore represents the upstream primary-key dimensions; the missing index/generation is needed for ordering/refresh fencing and stale-write protection, not to claim a known collision among valid upstream rows.
- **Transaction/write ownership:**
  - One Room database and one repository/facade own all five PoC metadata domains. No parallel SharedPreferences/AtomicFile metadata writes after cutover.
  - Book/BookChapter/TOC/progress mutations run in Room transactions. TOC replacement is staged/validated, then committed atomically with progress remapping and cache metadata invalidation.
  - Cache body files are immutable generations. Write temp -> fsync/atomic rename -> Room token compare-and-publish. If token compare fails, file remains unreachable and is reclaimed by bounded orphan cleanup. Never mutate a body file currently referenced by Room.
  - Book deletion cascades chapters, progress and cache metadata; cleanup of immutable payload files is post-commit and retryable.
  - Favorites, bookmarks and reading-history presentation records remain separate Badcat product domains unless later evidence shows they are required for the five-domain engine contract; do not silently merge them into Legado Book fields.
- **Migration plan (must become executable before cutover):**
  1. Add versioned Room schema and migration metadata; do not change old stores or readers in the schema-only step.
  2. Snapshot legacy source AtomicFile, progress SharedPreferences and cache index/files into a migration backup manifest with sizes/checksums. Keep originals read-only until verification succeeds.
  3. Dry-run parses and validates every source/book/progress/cache record; report malformed/ambiguous rows without deleting or silently coercing them.
  4. Import source raw JSON and stable source IDs; import progress by exact source/book/last-chapter URL. Resolve chapter index only when the imported authoritative TOC yields one unambiguous match; otherwise retain URL progress and mark unresolved rather than guessing.
  5. Import legacy cache entries only when their source/book/chapter identity maps unambiguously; preserve old files as rollback input. Unknown/ambiguous entries remain cold-cache misses, never attach to a guessed chapter.
  6. Commit all metadata plus migration marker atomically. Re-running the same migration must be idempotent and checksum-verified.
  7. Verify counts, identities, raw JSON semantic equality, progress, chapter order and cache generation pointers. Only then switch all five readers/writers to Room in one release boundary.
  8. Retain rollback snapshot through at least one successful app restart/health check. On failure before cutover, old stores remain authoritative. On failure after cutover, restore the verified snapshot using an explicit rollback migration; do not attempt uncoordinated dual-write.
  9. Add bounded orphan payload cleanup and a recoverable migration state machine (NOT_STARTED / COPYING / VERIFIED / CUTOVER / ROLLBACK_REQUIRED / ROLLED_BACK).
- **Required implementation tests:** schema key/index/foreign-key contract; full upstream field and default/null/unknown JSON round-trip; same bookUrl under two sources isolation; same URL under two books isolation; duplicate page replay/idempotent TOC merge; stable chapter index across reorder/refresh; progress URL-to-index ambiguity handling; stale token cannot publish; process restart preserves current generation; crash between temp write/rename/DB publish recovers safely; migration dry-run, repeat/idempotency, injected mid-migration failure, rollback and restart; architecture test proving runtime depends on facade interfaces rather than UI/DAO and UI does not import official DAO.
- **Acceptance/exit:** this document registers the architecture candidate only. Before runtime code, produce the Room entity/schema draft and exact field/converter map, enumerate all old readers/writers again, verify migration from the actual legacy formats, and register the first schema/migration code slice separately. If source catalog losslessness, migration rollback or single-writer cutover cannot be proven, do not implement dual-write; stop B′ and evaluate restricted data-layer A / hybrid. No UI changes, no main branch, no APK claim.
- **Status:** architecture design registered; no source code or tests changed in this slice; B′ remains not passed.

### PoC-0.3 result and next ordered tasks

1. **Decision candidate:** Badcat-owned Room metadata store + immutable generation-addressed chapter files + one compatibility facade.
2. **Not selected:** importing the entire upstream AppDatabase v113 and its unrelated 26-entity product schema.
3. **First implementation task:** schema/entity/DAO draft and upstream field/identity/converter matrix; no Activity or UI migration yet.
4. **Second task:** executable legacy data dry-run/import/backup/rollback state machine with fault-injection tests.
5. **Third task:** switch BookSource catalog and reading progress behind one repository/facade, preserving old formats for rollback.
6. **Fourth task:** durable Book/BookChapter/TOC and progress index mapping.
7. **Fifth task:** chapter cache generation/token fence, batch-context integration and restart/recovery tests.
8. **Final PoC gate:** real-source search→detail→TOC→chapter→cache on Android device, plus architecture tests and exact-head CI. Until then B′ is not passed.


## 2026-11-20 PoC-0.4 registration — Room schema contract draft and legacy caller inventory

- **Starting HEAD:** `dbd1c76f06382d166e77c3170279de2951ebfaa9`; branch `integration/legado-upstream-runtime`.
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** source-audit and report-only schema contract draft for the selected B′ candidate. Inspect exact pinned upstream BookSource/Book/BookChapter/rule declarations, Room keys/indexes/converters relevant to those models, current host Gradle/database dependencies, current persistence stores and every reader/writer/caller for source catalog, progress, TOC, chapter cache, favorites/bookmarks/history. Produce a proposed Badcat-owned schema field map, constraints, converter strategy, ownership/caller matrix, unresolved ambiguities and an implementation test plan.
- **Target report:** this category-3 checklist only. No runtime code, Gradle dependencies, entities, DAOs, migrations, UI, or tests are changed in this audit slice.
- **Fact-source rule:** use only the exact pinned upstream SHA and current branch files. Do not infer an upstream default or converter behavior from a host projection. Preserve upstream BookChapter primary key `(url,bookUrl)`, unique `(bookUrl,index)`, and distinguish Kotlin equality from Room identity.
- **Caller inventory targets:** `FileBookSourceCatalog`, `LegadoBookSourceCodec`, source/login/session consumers, `BadcatReadingStateRepository`, all reader Activities, favorites/bookmarks/history, `SourceTocService`, `SourceChapterContentService`, `ChapterContentCache`, `FileChapterContentCache`, and all tests/fixtures.
- **Risks:** accidentally introducing a second database or dual-writer; changing source/session identity; losing unknown JSON members; inventing null/default conversion; incorrectly treating chapter URL, index or Kotlin equals as interchangeable identity; moving favorites/bookmarks/history into the engine schema without evidence; breaking legacy AtomicFile/SharedPreferences data.
- **Required output:** schema/table and index draft; per-field upstream→host mapping with unresolved fields explicitly marked; converters and JSON preservation strategy; single-writer/transaction map; legacy data migration inventory; all callers; regression and fault-injection tests; exact acceptance/exit gates.
- **Acceptance:** a reviewed, evidence-linked schema contract and complete known caller inventory sufficient to preregister a separate code-only schema slice. If upstream declarations or current callers cannot be established from the pinned/current tree, record the gap and do not invent a schema. B′ remains unpassed. No APK/release claim.


### PoC-0.4 audit findings — pinned schema and current host ownership

**Pinned upstream sources directly inspected**
- `BookSource.kt`: `book_sources`, PK `bookSourceUrl`, non-unique index on the same field. Its persisted constructor has 35 properties (including six nullable typed rules); equality/hash/key use `bookSourceUrl`. The six Room converters serialize rules with pinned `GSON.toJson` and deserialize with `GSON.fromJsonObject<T>(...).getOrNull()`. The rule models also define deserializers accepting either JSON object or a JSON string primitive; other JSON shapes become null.
- `Book.kt`: `books`, PK `bookUrl`, unique index `(name,author)`; 32 persisted constructor properties, including nested `ReadConfig`, reading index/position/time, source origin, user overrides, variables and sync/cover state. `variableMap`, HTML fields, download URLs, folder cache and computed config accessors are ignored/transient and are not columns. `ReadConfig` converter uses pinned Gson. Book equality/hash use only `bookUrl`.
- `BookChapter.kt`: `chapters`, PK `(url,bookUrl)`, non-unique index `bookUrl`, unique index `(bookUrl,index)`, FK `bookUrl -> books.bookUrl ON DELETE CASCADE`. Persisted constructor has 17 properties: `url,title,isVolume,baseUrl,bookUrl,index,isVip,isPay,resourceUrl,tag,wordCount,start,end,startFragmentId,endFragmentId,variable,imgUrl`. `variableMap`, `deferUpdates`, `titleMD5` are runtime/ignored state. Equality/hash compare URL only and must not be used as row identity.
- `AppDatabase.kt`: upstream DB is version 113, 26 entities + view and a long migration graph. This confirms the B′ choice not to import the whole unrelated DB.
- Rule entity source files inspected: ExploreRule, SearchRule, BookListRule, BookInfoRule, TocRule, ContentRule, ReviewRule. ReviewRule includes its own boolean default and extended review/summary/detail/reply rule fields; it must not be dropped merely because the current reader path does not execute it.

**Current host ownership and callers confirmed from branch tree/source**
- `FileBookSourceCatalog` is the sole current source catalog writer. It stores a JSON array in `legado-book-sources.json` via AtomicFile; import replaces by `sourceId`, enable mutates only raw JSON's `enabled`, remove/export operate on that file. The codec validates a bounded JSON document, maps selected scalar fields and retains full `rawJson`; it does not persist a typed upstream BookSource or nested mutable rule objects.
- `BadcatReadingStateRepository` is the single current access boundary for progress/bookmark/history preference data. Progress keys persist last chapter URL and per-chapter scrollY; bookmark companion metadata shares the progress preference file; history is a separate JSON array preference. These are not official Book fields and scrollY is not the same unit/meaning as upstream `durChapterPos`.
- `SourceBookActivity` owns the live chapter list in Activity memory and calls the ReadingEngine for detail/TOC. `SourceChapterActivity` reads/writes last chapter URL, chapter scrollY, bookmark state and history via the repository; it updates progress on chapter display and scroll persistence. No durable Book/BookChapter DAO was found.
- `FavoritesActivity` owns a separate Badcat favorites/folder preference domain and reads history timestamps for sorting. `BookmarkActivity` and `ReadingHistoryActivity` are presentation/callers of the repository, not Legado DAO owners.
- `SourceTocService` produces chapter projections; `SourceChapterContentService` uses `ChapterContentCache`; `FileChapterContentCache` owns bounded AtomicFile body entries. Cache metadata currently has no durable DB generation pointer/token or TOC foreign key.
- `:app` currently has no Room runtime/compiler/schema export dependency or Room database. `:integration-contract` remains Android-free and contains only projections.

### Proposed Badcat-owned schema contract (draft, not implemented)

| Table | Primary/unique identity | Required columns and constraints | Sole owner |
|---|---|---|---|
| `legado_sources` | PK `source_id` (= exact `bookSourceUrl`) | `raw_json` NOT NULL; mapped indexed columns for name/type/enabled/customOrder/group/search/explore; six rule JSON payloads retained as JSON values; updated/import metadata. Unknown source fields remain in raw JSON. | Source repository/facade |
| `library_books` | PK composite `(source_id,book_url)` | All 32 upstream Book persisted fields represented with upstream nullability/default metadata; source FK; optional `upstream_json` for future-field preservation; unique `(source_id,name,author)` rather than globally unique name/author to prevent cross-source collisions. | Book repository/facade |
| `library_chapters` | PK composite `(source_id,book_url,chapter_url)`; unique `(source_id,book_url,chapter_index)` | All 17 upstream BookChapter persisted fields; FK `(source_id,book_url)` to books with cascade; preserve chapter URL and index separately. | TOC repository/facade |
| `reading_progress` | PK `(source_id,book_url)` | `last_chapter_url`, nullable `chapter_index`, upstream-compatible `dur_chapter_pos` and `dur_chapter_time`, separate nullable `legacy_scroll_y`, unresolved-mapping flag/version. Do not convert pixel scrollY into character position. | Reading-state repository/facade |
| `chapter_cache_entries` | PK `(source_id,book_url,chapter_url)` | `chapter_index`, `generation` monotonic integer, `content_version`, immutable payload file ID/path, byte length, savedAt, checksum; FK to current chapter identity with explicit invalidation on TOC replacement. | Cache repository/facade |
| `migration_journal` | PK migration ID | state, source snapshot manifest/checksums, row counts, started/verified/cutover timestamps, error details. | Migration coordinator |

**Schema caveats requiring proof before implementation**
1. Upstream Book unique `(name,author)` is global. B′ composite source-scoped Book identity intentionally relaxes this constraint to preserve Badcat's already source-scoped identity; document this as a host extension, not byte-for-byte Room parity.
2. BookChapter PK and unique index mirror upstream keys but add source scope. TOC refresh must preserve URL/index semantics and avoid duplicate page replay; URL-only equality is not a storage key.
3. BookSource rule converters must accept both object and string-encoded JSON; preserve raw unknown members even when typed decode cannot understand them. Do not serialize a typed object over raw JSON unless mutation semantics explicitly identify the changed rule.
4. Book.ReadConfig contains nested nullable/default fields including LocalDate and ParagraphAnchor; converter compatibility must be verified against pinned Gson type adapters, not generic Gson assumptions.
5. Upstream Book constructor defaults that use `System.currentTimeMillis()` are dynamic. Schema defaults annotated as SQL zero do not equal Kotlin construction defaults; keep constructor/default and migration/default semantics separate.
6. BookSource `enabledCookieJar` is nullable but constructor default true and the Room column default is declared as 0. Do not silently normalize null/true/false.
7. BookChapter's variable big-data helpers and mutation methods call appDb/RuleBigDataHelp; the proposed row is storage shape only, not a complete upstream BookChapter runtime implementation.
8. Cache payload files remain immutable generation objects; the Room row only points to the published generation. File cleanup is retryable and must not run before the transaction commits.

**Caller inventory status**
- Confirmed direct persistence owners and UI consumers above.
- Still required before cutover: repository-wide search for all constructor sites, direct preference/file path access, favorite-folder and reader-state writes, cache creation/injection sites, all TOC list transforms and all tests/fixtures. The source audit was not a complete static call graph, so do not call the inventory exhaustive yet.

**PoC-0.4 result**
- [x] Pinned model table names, keys, indexes, persisted constructor fields and ignored runtime fields inspected for the three core entities.
- [x] Six typed BookSource rule columns and object/string converter behavior identified.
- [x] Current host durable owners and main Activity/repository/cache callers mapped.
- [x] Draft schema/identity and explicit differences from upstream recorded.
- [ ] Exhaustive source-level caller scan not complete.
- [ ] Room dependency, entities, DAOs, schema export and migration tests not implemented.
- [ ] Legacy snapshot/dry-run/rollback executable design and fault-injection tests not implemented.
- [ ] No test/build/CI/APK/device result is claimed for this documentation audit.

**Next ordered work**
1. Complete repository-wide caller/access scan (including search for preference names, AtomicFile names, cache constructors, TOC transforms and model constructors) and close the ownership matrix.
2. Verify pinned Gson `INITIAL_GSON`/runtime Gson adapters, ReadConfig LocalDate/ParagraphAnchor converters, and every nullable/default distinction with fixtures.
3. Freeze schema v1 SQL/Room entity definitions and exported schema JSON; register a separate schema-only code change with Room compiler/runtime and schema verification tests.
4. Only after schema-only CI passes, implement legacy backup + dry-run + idempotent import + rollback state machine; no production cutover in that step.

**Self-review:** this is an evidence-backed schema draft, not executable schema and not B′ acceptance. It deliberately does not map UI pixel scrollY to upstream character position, does not claim exhaustive caller discovery, and does not change any runtime code.


## 2026-11-20 PoC-0.5 registration — exhaustive persistence caller scan and converter contract verification

- **Starting HEAD:** `a905d953abab4e7c7513de79a65b937684a1e8e8`; active branch `integration/legado-upstream-runtime`.
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Scope:** finish the remaining static ownership scan for source catalog, progress/history/bookmarks/favorites, TOC and chapter cache; inspect pinned Gson/Room converter and nested ReadConfig serialization declarations; reconcile every verified caller and identity/transaction limitation in the category report. If repository evidence supports a safe, bounded schema-only implementation slice, register it separately; otherwise explicitly block implementation rather than add an unverified second store.
- **Targets:** all current source files found through the repository tree; exact persistence filenames/keys; all cache interface/implementation and construction sites; TOC list transforms and tests; source codec and raw JSON mutation paths; pinned upstream Gson initialization/type adapters and Room converter declarations.
- **Risks:** code search index may not cover this branch; a partial search result is not proof of zero callers. A schema-only Room addition without Android build/CI verification could break build or lock in incorrect defaults. Gson adapter behavior must be read from the pinned source, not inferred from generic Gson. Preserve scrollY as a distinct Badcat pixel offset; do not convert it to upstream character position.
- **Required checks:** recursive tree enumeration plus direct source inspection; search every exact legacy key/file/class name; inspect all callers and tests; verify nested converter/type adapter source; record which searches are exhaustive vs incomplete; compare Room version/Gradle dependencies; no schema/runtime edit unless all field and migration contracts are unambiguous.
- **Acceptance:** caller inventory is evidence-complete for the files actually present at the audited HEAD; converter behaviors and unresolved default/null cases are explicit; implementation blockers and next code-only slice are clear. No UI change, no main branch, no claim B′ passed. If the tool cannot retrieve the full tree or exact sources, report that limitation and do not invent a complete scan.


### PoC-0.5 audit results — current-tree caller scan and converter/schema gate

**Repository inventory method and coverage**
- Enumerated the complete Git tree at the preregistered source head `990b18773eed5adce92cf2c4066ad1e2ce29b90a`: 437 entries, GitHub reported `truncated=false`.
- Inspected the persistence implementation files, reader/catalog/favorites/history/bookmark activities, engine factory, TOC/content service interfaces and implementations, focused cache/TOC/content tests, and app/integration-contract Gradle files.
- GitHub code-search endpoint returned no indexed results for all queried exact identifiers (legacy preference names, cache classes, constructors, AtomicFile, RoomDatabase, SharedPreferences, getSource). This is treated as an index limitation, not evidence that identifiers are unused. Direct tree/source inspection is the evidence basis.
- Audit completeness is therefore **complete for the direct persistence owners and identified production wiring below**, but not a formal compiler-generated call graph. The report does not claim zero unlisted reflection/dynamic access.

**Verified production ownership and call sites**
1. **BookSource catalog**
   - Writer/reader: `FileBookSourceCatalog`; file `filesDir/legado-book-sources.json`; Android `AtomicFile`; process-wide coroutine Mutex; JSON array encoded/decoded by `LegadoBookSourceCodec`.
   - Operations: `all`, `importJson` (replace by sourceId, preserve insertion order), `setEnabled` (patch enabled field in raw JSON), `remove`, `exportJson`.
   - Engine construction: `BadcatReadingEngineFactory` owns one catalog and exposes it via `sourceCatalog()`; `SourceCatalogActivity` is the management UI surface. This remains a source-descriptor store, not a typed mutable upstream BookSource/BaseSource lifecycle.
2. **Progress, bookmarks and history**
   - Sole compatibility access boundary: `BadcatReadingStateRepository`.
   - Progress/bookmark file: SharedPreferences `badcat_reading_progress`; last chapter key is Base64 URL-safe encoded `(sourceId,bookUrl)`; scroll/bookmark identity is encoded `(sourceId,bookUrl,chapterUrl)`; bookmark value/title/book/time are written in one Editor operation.
   - History file/key: `badcat_reading_history/records`, JSON array; upsert identity `(sourceId,bookUrl)`; it is not official Book progress and is not a chapter-position store.
   - Callers: `SourceBookActivity` restores/clears last chapter, checks/deletes bookmark state, persists scroll, last chapter, history and bookmark; `BookmarkActivity` lists/removes/clears bookmark records; `ReadingHistoryActivity` lists/removes/clears history and its static save entry delegates to repository; `FavoritesActivity` reads latest history timestamps for ordering.
   - Favorites are separate direct SharedPreferences writers in `FavoritesActivity`: `badcat_favorites/books` and `folders`, keyed by composite `(sourceId,bookUrl)`. They are Badcat product state and intentionally excluded from Legado Book ownership.
   - Reader visual preferences remain separately stored and are not progress.
3. **TOC**
   - `SourceBookActivity.allChapters` is Activity-memory state; `SourceTocService` fetches pages and calls its progress callback with accumulated chapters. The service and final result currently `distinctBy { it.url }` and reassign indexes. `SourceBookActivity` also has a separate search-result `distinctBy { it.bookUrl }` projection.
   - Tests in `SourceTocServiceTest` cover pagination, redirects, cycles, scripts and error cases. Existing fixtures codify URL-only chapter deduplication. This behavior must be intentionally reviewed against exact upstream merge/update behavior before altering.
   - No durable TOC/BookChapter repository or DAO was found in the complete 437-entry tree.
4. **Chapter content cache**
   - Interface: `ChapterContentCache.get/put(sourceId,bookUrl,chapterUrl)`; no chapter index, generation token, invalidation or compare-and-publish method.
   - Implementation: `FileChapterContentCache` in `app/.../runtime`, AtomicFile per entry, process-wide Mutex, bounded 4 MiB entry/64 MiB budget, payload plus savedAt. Cache file identity hashes length-prefixed `(sourceId,bookUrl,chapterUrl)`.
   - Production wiring: `BadcatReadingEngineFactory` injects a new `FileChapterContentCache` into `SourceChapterContentService`.
   - Callers/tests: `SourceChapterContentService` reads a valid TTL cache before HTTP, falls back to cached content on request failure, writes after successful replacement; `FileChapterContentCacheTest` covers persistence/bounds; `SourceChapterContentServiceTest` covers cache hit/fallback/write failure. No stale-writer fence exists.
5. **Model construction and projection**
   - Production source UI and engine exchange `BookSource`, `BookCandidate`, `BookDetail`, `BookChapter` projections in the Android-free `:integration-contract` module. No production durable Book/BookChapter entity or constructor-to-DAO path exists.
   - App Gradle currently has Gson and parser/network libraries but no Room runtime/compiler, schema export configuration or migration infrastructure. `:integration-contract` is JVM-only and deliberately has no Android/Room dependencies.

**Converter/default contract status**
- Pinned model declarations and rule converter behavior recorded in PoC-0.4 remain the current evidence: BookSource has six typed rule payloads; the upstream rule deserializers accept object and string-encoded object forms; Book includes nested ReadConfig; constructor defaults and Room SQL defaults can differ (including dynamic timestamps and nullable `enabledCookieJar`).
- The current host codec's `objectOrNull` normalizes object and stringified object to an object JSON string for selected projection fields, but the complete raw source JSON is separately retained. Invalid/non-object forms become null in the projection; the raw JSON preservation prevents immediate export loss but does not provide typed runtime semantics.
- **Not verified in this audit:** byte-for-byte Gson adapter parity for every nested ReadConfig type (including LocalDate/ParagraphAnchor), every rule's missing/null/default semantics, all 32 Book and 17 BookChapter persisted constructor field mappings, and upstream converter behavior for malformed payloads. No schema converter or typed entity was added. These are hard blockers for schema v1, not silently assumed defaults.

**Ownership decision after full scan**
- The existing five domains still do not share a transaction: source catalog AtomicFile, progress/bookmarks SharedPreferences, history SharedPreferences, ephemeral TOC list and chapter AtomicFile cache are separate stores.
- Adding Room now would create a second source of truth until a migration state machine and all readers/writers are switched atomically. The audit does not support a safe production cutover in this slice.
- Therefore no Room dependency, entity, DAO, schema JSON, migration, dual-write, or runtime code was introduced. This is a deliberate safety stop, not an implementation completion.

**PoC-0.5 result**
- [x] Full Git tree enumerated without truncation.
- [x] Direct source catalog writer and engine/UI wiring identified.
- [x] Progress/bookmark/history writers and all identified Activity consumers listed.
- [x] Favorites kept separate as Badcat product state.
- [x] TOC projection and URL-dedup callers/tests identified.
- [x] Chapter cache interface, implementation, production injection and tests identified.
- [x] Current Gradle confirms Room is absent; no false Room-equivalence claim.
- [ ] Formal compiler-generated all-callers graph not available from GitHub connector; no-index search was not treated as proof.
- [ ] Full pinned Gson nested converter/default/null matrix remains open.
- [ ] Room schema and executable backup/dry-run/idempotent import/rollback remain unimplemented.
- [ ] No code/test/build/CI/APK/device result is claimed for this audit slice.

**Next stage gate (ordered, no skipping)**
1. Create pinned Gson fixtures from the actual upstream converter/type-adapter source and tests; include object/string rule JSON, missing/null/default/unknown members, ReadConfig LocalDate/ParagraphAnchor and round-trip.
2. Freeze the exact schema v1 field matrix, including upstream field counts and explicit host extensions; export schema JSON and test PK/unique/FK constraints.
3. Add Room runtime/compiler only in a separately preregistered schema-only change; no production reader/writer cutover in that commit.
4. Add deterministic migration journal + snapshot manifest + dry-run/import/idempotency/fault injection/rollback/restart tests; preserve all old stores until migration verification.
5. Only after those gates pass, switch one domain at a time behind repositories with a single writer and restart validation. No simultaneous dual-write.

**Audit self-review**
- The complete Git tree was retrieved and direct owners/wiring inspected. The code-search service returned empty results and was explicitly not treated as exhaustive evidence.
- Scope stayed within category 3; no UI, Reader redesign, AdBlock/AdMark, PPRule, main branch, runtime implementation or release packaging changed.
- B′ remains unpassed. This audit closes the caller inventory for known persistence owners but does not close the Gson contract, Room implementation, migration or runtime acceptance gates.


## 2026-11-20 PoC-0.6 registration — pinned Gson/rule serialization fixtures

- **Starting HEAD:** `76ae4fd57fe08e2a7f6a59dc27b4935acaf9d7ce`; branch `integration/legado-upstream-runtime`.
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Ordered position:** first gate after PoC-0.5 caller audit; before schema v1 freeze and Room code.
- **Exact scope:** compare current host `GsonExtensions.kt` and six rule model deserializers against the exact pinned source; add JVM/Robolectric contract fixtures for object-form and string-encoded object-form rule JSON, malformed/array/null payload behavior, unknown fields, Gson number coercion, string coercion, LocalDate string/legacy-object/invalid parsing and serialization round-trip. Verify current host copies match upstream source where applicable.
- **Target files:** `app/src/main/java/io/legado/app/utils/GsonExtensions.kt`; `app/src/main/java/io/legado/app/data/entities/rule/{ExploreRule,SearchRule,BookInfoRule,TocRule,ContentRule,ReviewRule}.kt`; new `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt`; this checklist.
- **Pinned declarations:** `INITIAL_GSON` registers Map<String?,Any?>, Int, String and LocalDate adapters, LONG_OR_DOUBLE object number strategy, HTML escaping disabled and pretty printing; `GSON` adds the six rule JSON deserializers. Each rule deserializer accepts JSON object or any JSON primitive by parsing its string value as a rule object; array/null -> null. Room converters for the six rule models call `GSON.toJson` and `GSON.fromJsonObject<T>(json).getOrNull()`.
- **Known boundary:** current host repository does not contain pinned `Book.kt`, `BookSource.kt`, or `BookChapter.kt` entities, so this slice cannot instantiate upstream `Book.ReadConfig` or `ParagraphAnchor`. The exact pinned ReadConfig fields and LocalDate adapter are documented, but a ReadConfig/ParagraphAnchor round-trip fixture must remain blocked until the real model contract is made available without inventing a shadow class.
- **Risks:** Gson primitive deserializer accepts more than string primitives and may throw on primitive values that are not valid JSON strings; Kotlin default constructor behavior can be bypassed/altered by Gson reflection; absent vs explicit null semantics differ; unknown fields are ignored by typed models unless raw JSON is separately retained. Do not claim raw JSON retention is equivalent to typed model round-trip.
- **Required tests:** all six rule types object/string input; array/null invalid behavior; malformed nested JSON; unknown-member tolerance plus raw-source preservation boundary; INITIAL_GSON scalar/map coercion; LocalDate ISO string, legacy object, invalid and null behavior; stable serialization round-trip and HTML escaping behavior.
- **Acceptance/exit:** focused tests pass on the exact host implementations; source comparison records exact differences; unresolved ReadConfig/ParagraphAnchor and complete Book/BookChapter field round-trip remain explicit blockers. No Room/schema/DAO/migration or production storage change in this slice. B′ remains unpassed.


### PoC-0.6 execution result — pinned Gson and rule JSON contract

- **Registration commit:** `11f9abdfc76f9298087fb27456ac36892aea28ea`.
- **Test commit:** `d634aa7be41f4407fa10e09eca3476a2af687b38`.
- **Pinned upstream:** `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Direct normalized source comparison against the pinned upstream confirms exact content matches for:
  - `GsonExtensions.kt` (9,479 chars);
  - `ExploreRule.kt` (1,126);
  - `SearchRule.kt` (1,120);
  - `BookInfoRule.kt` (1,095);
  - `TocRule.kt` (947);
  - `ContentRule.kt` (2,009);
  - `ReviewRule.kt` (2,350).
- Added `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt` against actual host Gson/rule classes. It covers six rule object/string forms, array/null deserialization, unknown-field behavior, map numeric coercion, String/Int adapters, LocalDate ISO/legacy-object/null/invalid behavior, HTML escaping and ContentRule serialization round-trip.
- Important behavior confirmed from source: rule deserializers route object JSON through INITIAL_GSON; any JSON primitive is passed to INITIAL_GSON as a string; arrays/null return null. GSON's six rule adapters and INITIAL_GSON adapter setup match the pinned source byte-for-byte after newline normalization.
- **Not covered / still blocking schema freeze:** this host source tree does not include upstream `Book.kt`, `BookSource.kt`, or `BookChapter.kt` entities. Thus no authentic `Book.ReadConfig` or `ParagraphAnchor` fixture can be compiled here. Pinned `Book.ReadConfig` contains nullable `startDate: LocalDate?`, nullable `paragraphAnchor: ParagraphAnchor?`, nullable/defaulted booleans/ints/strings and a `List<Long>`; `ParagraphAnchor` fields are `chapterUrl`, `itemId`, `chapterPosition`. Its Room converter is `GSON.toJson(config)` / `GSON.fromJsonObject<ReadConfig>(json).getOrNull()`. A locally invented shadow type is explicitly rejected.
- Unknown fields: typed Gson model parsing ignores unrecognized members and serialization drops them; source-catalog raw JSON preservation is a separate mechanism. It must not be confused with typed model unknown-field round-trip.
- **Execution evidence:** GitHub branch head verified at test commit `d634aa7be41f4407fa10e09eca3476a2af687b38`. The commit workflow lookup returned no workflow runs; no local Gradle runner was available through the repository connector. Therefore test source is added but **not executed**, and no test/build/CI pass is claimed.
- **Result:** partial gate. Source parity for the seven shared files is verified and fixture coverage is authored, but execution and ReadConfig/ParagraphAnchor coverage remain open. Do not freeze schema v1 or add Room yet.
- **Next ordered action:** obtain/bring in the exact pinned Book/BookSource/BookChapter model contract and its dependencies as real upstream-compatible sources (not local shadow DTOs), then add/run ReadConfig/ParagraphAnchor and model field/null/default round-trip tests. After that, complete schema v1 field map and schema-only preregistration. B′ remains unpassed.


## 2026-11-20 PoC-0.7 registration — authentic upstream model source-closure audit

- **Starting HEAD:** `981d064b50de1fcb68fc91213085b057a8561b13`; branch `integration/legado-upstream-runtime`.
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Ordered position:** immediately after PoC-0.6; before any schema v1 freeze, Room implementation, or production persistence changes.
- **Current evidence:** fetched the exact pinned `BookSource.kt`, `Book.kt`, `BookChapter.kt`, rule model sources, upstream app Gradle file and version catalog. The official entities are not standalone Kotlin DTOs: they directly depend on Android Parcelable/Parcelize, Room annotations/converters, BaseSource/BaseBook interfaces, constants, Gson helpers, and (for Book) application database, BookHelp/ContentProcessor/AppConfig/ReadBookConfig/NetworkUtils and model utilities. Upstream app also wires Room KSP, Gson, coroutines, parser/network modules and other app-level components. Blindly copying these files into the current Android-free `:integration-contract` would violate its boundary and would not compile as a faithful integration.
- **Exact scope:** complete a dependency-closure and integration-boundary audit for the three pinned entities, nested `Book.ReadConfig` / `ParagraphAnchor`, BaseSource/BaseBook contracts, six rule models and their converters. Identify which upstream sources can be imported unchanged, which dependencies already exist in Badcat, which require isolated upstream modules/adapters, and which pull in unrelated app/database/runtime services. Compare Badcat Gradle/Kotlin/Gson/Room configuration and every affected host model/codec/catalog/engine caller. Produce a concrete minimal integration plan with no shadow DTOs and no loss of official fields.
- **Target files:** source/dependency inventory only; report this registration and findings in this checklist. No runtime source, Gradle, database, schema, tests or UI edits are authorized by this audit registration.
- **Risks:** entity declarations may compile only as part of upstream module graph; annotation/compiler/version drift; transitive dependencies may pull in the full app DB or unrelated features; Room converters and Kotlin defaults may differ; adapting projections can discard official fields/unknown JSON; changing identity/equality can break source/login/cache scoping.
- **Required evidence:** exact pinned source paths and SHA; imports and direct/transitive dependency map; upstream module/Gradle graph; Badcat module boundary and dependency comparison; all current host declarations and identified production callers; recommended smallest faithful integration seam; unresolved closure items.
- **Acceptance:** establish a reproducible integration strategy that uses authentic pinned model declarations/contracts and can compile/test against the actual upstream Gson converters without local shadow model classes. If a minimal closure cannot be isolated safely, document the concrete blocker and invoke the B′ exit evaluation rather than silently replacing the models.
- **Explicit exclusions:** no Room/schema freeze, DAO, migration, data cutover, dual-write, AnalyzeRule/AnalyzeUrl, Rhino, unified reader lifecycle, batch/cache implementation, UI changes, main branch or APK release.
- **CI baseline:** current HEAD status query returned no status checks and no workflow runs; this is absence of evidence, not a pass.

### PoC-0.7 audit result — registration only

- Exact pinned model/build files were retrieved for inspection. `BookSource` is coupled to Parcelable/Room, BaseSource, rule models and Gson; `Book` additionally reaches application DB, BookHelp/ContentProcessor/AppConfig and other runtime helpers. The three entity files therefore cannot be dropped into the existing platform-neutral contract module unchanged.
- No implementation was made in this registration step. Dependency closure, all-callers audit, integration seam, and executable model fixtures remain pending.
- No tests/build/CI/APK result is claimed. B′ remains unpassed. The next action is the registered source-closure audit; do not proceed to schema v1 or Room until authentic model serialization and tests are executable.


### PoC-0.7 source-closure audit — findings checkpoint (2026-11-20)

- **Audit baseline:** category checklist was reread from the active integration branch; pinned upstream source reads used `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`. No runtime/Gradle/test/UI files were changed in this checkpoint.
- **Authentic upstream declarations inspected:** `BookSource.kt`, `Book.kt`, `BookChapter.kt`, `BaseSource.kt`, `BaseBook.kt`, and upstream version catalog. This is source inspection, not proof that the full transitive closure or every host caller has been enumerated.
- **BookSource closure:** the real entity imports Android Parcelable/Parcelize, Room entity/index/converter annotations, source type constants, six typed rule models, `BaseSource`, Gson helpers and group utilities. It implements mutable source lifecycle methods and URL-keyed equality/hash. Its six rule converters call the pinned Gson object parser/serializer. Copying only the entity would leave its base lifecycle and helper dependencies unresolved.
- **Book closure:** the real entity additionally imports `appDb`, BookHelp, ContentProcessor, AppConfig/ReadBookConfig, ReadBook, NetworkUtils and model helpers, plus LocalDate and Kotlin Parcelize/Room. It carries persisted reading state and nested `ReadConfig`; its runtime methods and lazy variable map are not a standalone data class contract. A faithful copy would pull application-level DB and reader behavior into the model boundary.
- **BaseSource/BaseBook closure:** BaseSource is an active service surface, not a marker: login and encrypted login-info storage, CookieStore/CacheManager access, source variables, JS binding/scope setup, Rhino execution, rate limiting and source lifecycle. BaseBook extends rule-data behavior and book-variable persistence. These contracts depend on broader runtime/application services.
- **BookChapter:** the pinned entity is a Room/Parcelable persistence model with book-scoped composite identity and more persisted state than the host `BookChapter(index,title,url)` projection. The current evidence does not establish a faithful compile-ready isolated source closure for it.
- **Host boundary:** the Android-free `:integration-contract` projection cannot accept these entities unchanged. The attempted host-file reads did not resolve the guessed paths, so this checkpoint does not claim a fresh exhaustive host caller/adapter scan. Prior PoC-0.5 direct persistence-owner inventory remains the current host-wiring evidence; its limits remain in force.
- **Dependency conclusion:** importing the three entities verbatim into `:integration-contract` is rejected. The narrowest viable seam still needs a verified source/module graph: either a dedicated Android-boundary compatibility module containing authentic pinned models plus only their required upstream contracts, or a separately justified official data-layer integration. Do not strip methods/annotations or recreate lookalike DTOs and call them upstream models.
- **Gate result:** PoC-0.7 source-closure audit is **partially evidenced, not accepted**. Full BookChapter import closure, exact upstream app Gradle graph, all host model/codec/catalog/engine callers, and a reproducible compile/test integration seam remain open. Do not freeze schema v1, add Room, or begin migration.
- **CI:** exact prior baseline `981d064b50de1fcb68fc91213085b057a8561b13` has empty combined statuses and no associated workflow runs. No tests/build/APK are claimed for this checkpoint.
- **Next ordered action:** finish the dependency/caller inventory using exact repository paths and full source ranges; identify the minimum authentic model module boundary and its dependency versions; only then preregister a narrowly scoped model-source integration and executable ReadConfig/ParagraphAnchor + all-model serialization fixtures. If the closure necessarily drags in unrelated app runtime/DB services, document the concrete trade-off and evaluate the B′ exit path (A/hybrid) before implementation. B′ remains unpassed.


### PoC-0.7 continuation registration — exact dependency/caller closure (2026-11-20)

- **Starting branch HEAD:** `a381d02625c40ffd788954a3776ea9457c808b85` (branch ref verified immediately before registration).
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Task:** continue the already registered PoC-0.7 source-closure audit; do not jump to schema v1 or Room.
- **Exact scope:** retrieve the full pinned source for BookSource/Book/BookChapter, BaseSource/BaseBook, ReadConfig/ParagraphAnchor and their directly referenced interfaces/helpers; inspect upstream app/module Gradle declarations and source-set/module inclusion; retrieve exact Badcat tree paths for host projections, codec/catalog, engine interfaces/factory/services, persistence owners, and all relevant tests; trace callers and dependency edges to identify a compile-ready minimal authentic integration boundary.
- **Targets:** pinned upstream entity/base/helper/build files and Badcat `:integration-contract`, `:app`, Gradle settings/catalog, source codec/catalog, engine factory/services, reader/source/book/chapter projections, and related unit tests. Record exact paths and imports rather than guessed paths or search-index absence.
- **Risks:** false isolation by dropping behavior-bearing BaseSource methods; app database/reader helper leakage; Room/Parcelable toolchain mismatch; unknown-field loss; breaking existing projection callers or source/login identity; importing unrelated upstream app subsystems.
- **Required evidence:** exact source paths and declarations; direct and transitive import/dependency closure; exact host production callers and tests; existing dependency versions; a minimal seam with a build/test strategy. Code search may supplement but not replace full-tree/path inspection.
- **Acceptance:** either identify a reproducible boundary that compiles/tests authentic pinned model declarations and converters without shadow DTOs or silently stripped methods, or document why no such narrow boundary exists and produce an explicit B′→A/hybrid decision comparison. No model implementation is authorized until this evidence is complete.
- **Tests/exit:** this is an audit-only registration. No tests are expected in this registration commit. Any implementation proposed next must be separately registered with source and caller map, round-trip fixtures and regression tests. If authentic closure necessarily imports broad app DB/runtime services, pause B′ and evaluate the documented exit route.
- **Exclusions:** no runtime code, Gradle, schema, Room, migration, UI, main branch or APK changes in this audit slice.


### PoC-0.7 continuation audit result — exact source/dependency/caller closure (2026-11-20)

**Repository and reproducibility evidence**
- Branch ref was verified at starting code HEAD `a381d02625c40ffd788954a3776ea9457c808b85`; the audit scope was preregistered before inspection in commit `ac63fb83d86a7cf3ec8922e244b01b00e51bc65b`. The code tree was read at that baseline; the only intervening commit is the registration documentation commit.
- Badcat recursive Git tree: 442 entries, `truncated=false`. `upstream/legado` is a Git submodule declared in `.gitmodules`, not a vendored source directory. Pinned upstream recursive tree: 3,298 entries, `truncated=false`, exact SHA `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Upstream exact file paths and blob identities inspected: `app/src/main/java/io/legado/app/data/entities/{BookSource,Book,BookChapter,BaseSource,BaseBook}.kt`; `app/src/main/java/io/legado/app/data/entities/rule/{BookListRule,ExploreRule,SearchRule,BookInfoRule,TocRule,ContentRule,ReviewRule}.kt`; `app/src/main/java/io/legado/app/utils/GsonExtensions.kt`; `app/build.gradle`; `gradle/libs.versions.toml`; `settings.gradle`; `modules/book/build.gradle`; `modules/rhino/build.gradle`. `ParagraphAnchor` and `ReadConfig` are nested declarations in upstream `Book.kt`, not separate source files.

**Authentic model declarations and closure**
- `BookSource.kt`: authentic mutable Room/Parcelable entity, 35 persisted constructor properties (including six typed rule properties); PK/equality/hash/key are `bookSourceUrl`. It implements `BaseSource`, provides mutable default rule accessors, source grouping and rule behavior, batch support (maximum 50), source comparison and six Gson TypeConverters. It is not just a JSON DTO.
- `Book.kt`: authentic Room/Parcelable entity with 32 persisted constructor properties, PK/equality/hash `bookUrl`, unique Room index `(name,author)`; persisted reading state includes chapter/volume indexes, position and timestamp. Nested `ReadConfig` has 20 constructor properties (including nullable `LocalDate`, nullable `ParagraphAnchor`, nullable Boolean/Int settings, Float speed and `List<Long>`); `ParagraphAnchor` has `chapterUrl`, `itemId`, `chapterPosition`. Book methods directly call `appDb.bookDao`, `BookHelp`, `ContentProcessor`, `ReadBook`, `AppConfig`, `NetworkUtils` and chapter/book helpers. The entity source is inseparable from application services without changing the upstream declaration/behavior.
- `BookChapter.kt`: authentic Room/Parcelable entity with 17 persisted constructor properties; composite PK `(url,bookUrl)`, unique index `(bookUrl,index)`, FK to Book with cascade delete. Beyond the host's three-field projection, it implements variable persistence, chapter URL resolution, display/title replacement, file/font naming and updates via `appDb.bookChapterDao`; it also depends on AnalyzeUrl, AppConfig, RuleBigDataHelp, ChineseUtils, regex/replace helpers and Android UI logging. Its equality/hash use URL only, not row identity.
- `BaseSource.kt`: 436 lines and a live service contract extending `JsExtensions`; methods include login UI v2, login JS, dynamic headers, encrypted login info, cookie/login-header updates, source variables and key-value cache, explore/jsLib invalidation, concurrency limiting and Rhino evaluation. Imports and behavior reach CookieStore, CacheManager, SharedJsScope, RhinoScriptEngine, JsSourceEngine, LoginUiV2, crypto, AppConfig, AppConst, JsExtensions and Android JavascriptInterface. Replacing it with an empty interface would remove required behavior.
- `BaseBook.kt`: extends `RuleDataInterface`, serializes variables, persists big variables via RuleBigDataHelp and provides book-rule behavior. It is not a passive marker.
- Rule model source set is complete at the pinned path: six concrete Parcelable rule data classes plus `BookListRule` interface. `BookSource.Converters` serialize all six with the pinned Gson; `Book.Converters` serialize nested ReadConfig using the same Gson. Unknown fields are discarded by typed Gson model round-trip; retaining raw source JSON separately does not solve typed model unknown-field preservation.

**Upstream build/module graph facts**
- Upstream uses AGP 8.13.2, Kotlin 2.4.20, KSP 2.3.12 and Room 2.8.4; Android app compile/target SDK 36 and Java/Kotlin 21. It includes `:app`, `:modules:book`, `:modules:rhino`. The `:modules:book` library build file itself only supplies Android library + Kotlin Android and androidx.annotation; it does not isolate Book/BookSource/BookChapter entities. Those entities remain under `:app` and import app-level services.
- Upstream app applies Android application, Kotlin Android, Parcelize, Room and KSP; uses Room runtime/ktx/compiler and a broad app dependency graph. App dependencies include `:modules:book` and `:modules:rhino`, plus AndroidX UI, media/video, storage, network, crypto, database, parsers and app services. The entities cannot be compiled by simply adding the small `:modules:book` project.
- Badcat settings currently include only `:app`, `:integration-contract`, `:legado-rhino`; root plugin versions align AGP 8.13.2/Kotlin 2.4.20/Parcelize. Badcat app uses Gson 2.14.0, Jsoup 1.23.2, JsoupXpath 2.5.3, JsonPath 3.0.0, OkHttp 5.4.0, coroutines 1.11.0 and selected crypto/archive dependencies, but no Room runtime/compiler or schema/migration graph. The Android-free integration-contract compiles selected original AnalyzeRule files from the upstream submodule in a custom `pinnedLegado` source set; it intentionally excludes Android entities and app-coupled AnalyzeRule lifecycle.

**Host caller and persistence inventory (exact code-tree paths)**
- Engine contracts/projections: `integration-contract/src/main/kotlin/app/badcat/engine/ReadingEngine.kt` declares projection-only `BookSource`, `BookCandidate`, `BookDetail`, `BookChapter(index,title,url)`; `ChapterContentCache.kt` and `ReadingProgressStore.kt` are platform-neutral interfaces.
- Source import/identity/persistence: `LegadoBookSourceCodec.kt` decodes selected scalar/rule JSON fields and retains `rawJson`; `app/badcat/client/FileBookSourceCatalog.kt` is the AtomicFile source writer/reader; `SourceCatalogActivity.kt` imports, exports, enables/removes and displays source descriptors; `BadcatReadingEngineFactory.kt` creates the catalog and injects it into SourceReadingEngine. Source identity is currently sourceId/bookSourceUrl.
- Reading flow: `SourceReadingEngine.kt` resolves source projections and calls `SourceBookDetailService`, `SourceTocService`, `SourceChapterContentService`; `SourceSearchService.kt` and `SourceExploreService.kt` cover search/explore. TOC remains in returned Activity/UI memory; the current engine's chapter content call constructs a new three-field `BookChapter(index=0,title="",url=chapterUrl)`, so the upstream BookChapter index/baseUrl/volume/variable state is not available to that call.
- Reader/product state: `BadcatReadingStateRepository.kt` is the progress/bookmark/history persistence facade used by `SourceBookActivity.kt`, `BookmarkActivity.kt` and `ReadingHistoryActivity.kt`; `FavoritesActivity.kt` has separate favorites/folder preferences. This does not establish an upstream Book row or a single transaction across all five PoC domains.
- Chapter content: `FileChapterContentCache.kt` is the AtomicFile implementation injected by `BadcatReadingEngineFactory.kt`; its `ChapterContentCache` key is sourceId+bookUrl+chapterUrl and has no chapter-index save token/generation fence. Tests present include `LegadoBookSourceCodecTest`, `BadcatReadingStateRepositoryTest`, `FileChapterContentCacheTest`, `SourceReadingEngineTest`, `SourceTocServiceTest`, `SourceChapterContentServiceTest` and the reader/source UI activities listed above.
- Search-index code search returned no matching host results and is not used as proof. The inventory above is based on the complete non-truncated Git tree and direct file reads of the identified production owners and wiring; it is not a compiler-generated call graph. All callers of the projection types outside the directly inspected files still need a build/compiler or source-wide local scan during implementation.

**Integration seam decision / B′ exit evaluation**
- **Rejected:** copy Book/BookSource/BookChapter into Android-free `:integration-contract`; that violates its platform-neutral boundary and cannot preserve authentic Android/Room/Parcelable + app-service methods.
- **Rejected:** import only the `:modules:book` upstream module as the model seam; its build does not contain these entities, and the entities still depend on the upstream app module.
- **Not accepted as B′:** invent stripped versions of the official entities, keep only constructor fields, or shadow them with local DTOs. That would fail the complete-upstream requirement and omit BookChapter/BaseSource behavior.
- **A/hybrid candidate:** evaluate a dedicated Android-boundary official data-layer module or official core entity/table integration, while keeping Badcat UI and current product state stores outside unrelated upstream Reader/RSS/media features. However, Book/BookChapter methods still pull appDb and application helpers; importing the full official app data graph may entail much of AppDatabase v113/26 entities and migration chain. A truly narrow official data layer has not yet been proven.
- **Current recommendation:** do not begin entity copying or schema v1. First perform a bounded A/hybrid feasibility comparison: (1) minimal official core tables/entities with exact pinned behavior and required app-service extraction, (2) official core database tables with a Badcat compatibility facade and old Badcat stores retained read-only during migration, (3) full upstream data layer if no narrower faithful option exists. Quantify required entity/DAO/migration/service closure and preserve the no-Reader-UI constraint. If no bounded closure exists, explicitly trigger B′ exit and narrow the product scope rather than silently weakening the models.

**Result and gate**
- PoC-0.7 exact source/dependency/caller audit is **substantially evidenced but integration boundary not accepted**. The central finding is that authentic BookSource depends on BaseSource service closure; Book/BookChapter directly depend on app DB and reader/runtime helpers. The pinned upstream has no standalone model module that can be added to Badcat as-is.
- No runtime source, Gradle, tests, schema, Room, migration, UI or APK changed in this audit. No tests/build were run or claimed.
- The audit registration commit `ac63fb83d86a7cf3ec8922e244b01b00e51bc65b` has empty combined statuses and no workflow runs. This is no CI evidence, not a pass.
- **Next ordered action:** preregister and perform the bounded A/hybrid feasibility comparison above, with exact entity/DAO/AppDatabase/migration/service closure and data ownership/rollback consequences. Do not start implementation until that decision is recorded. B′ remains unpassed; complete Legado remains uncompleted.


### PoC-0.8 registration — bounded official data-layer A/hybrid feasibility comparison (2026-11-20)

- **Starting HEAD:** `77af83178b49a1ab5543201ab6eba0620bab3565` (category-3 audit result commit; current branch ref to be rechecked before edits).
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Reason/order:** PoC-0.7 established that the authentic entities have no standalone model module and depend on app services. Before writing any entity/schema code, compare the narrow official data-layer routes and determine whether B′ should exit to A/hybrid.
- **Exact scope:** inspect pinned AppDatabase declaration, all registered entities/DAOs, BookSourceDao/BookDao/BookChapterDao, relevant migration chain/schema export, source/book/chapter helper dependencies and only the persistence services directly required by the five PoC domains. Classify each dependency as mandatory for faithful core models, separable service, unrelated product feature, or migration-only. Compare (A) minimum official core data layer, (B) official core tables + compatibility facade/Badcat UI and stores, (C) full official data layer. Record DB version/entity count, DAO/foreign-key/index dependencies, migration path, legacy-store backup/cutover/rollback implications, and whether Book/BookChapter methods can be separated without altering upstream semantics.
- **Target upstream files:** AppDatabase, DatabaseMigrations/schema JSON, BookSourceDao, BookDao, BookChapterDao, entities referenced by their foreign keys/converters, and direct helpers already identified (BookHelp/ContentProcessor/CacheManager/CookieStore/RuleBigDataHelp). Host target remains existing AtomicFile/SharedPreferences/cache owners and adapter boundary.
- **Risks:** accidentally import RSS/media/reader UI features; lose source login/Cookie/cache state; duplicate database writers; migrate before reversible snapshots; violate FK/identity/index contracts; treat Room entity field import as equivalent to the full official model behavior.
- **Required evidence:** exact dependency closure per option, minimum table/DAO/migration set, direct service closure, host data-domain ownership mapping, migration and rollback plan, decision matrix with explicit no-go conditions. No source implementation or Gradle changes in this comparison.
- **Acceptance:** choose B′ continuation only if a narrow faithful closure is reproducible; otherwise record explicit exit from B′ to A or hybrid with bounded included/excluded components and explain remaining compromises. Schema v1 and Room remain blocked until this decision is accepted.
- **Tests:** audit-only; no tests/build expected. Any subsequent implementation requires a separate registration and executable migration/contract tests.
- **Exclusions:** no runtime, Gradle, database, Room, DAO, schema, migration, data cutover, UI, main or APK changes.


### PoC-0.7 dependency/caller closure — completed inventory slice (2026-11-20)

#### Repository and build graph evidence

- Badcat tree at this branch was read recursively with `truncated=false` (442 entries). `.gitmodules` maps `upstream/legado` to `https://github.com/LegadoTeam/legado.git`; `gradle/legado-upstream.properties` pins `330e606c4d57e08adc056ff8ab7a4132c784da9a` and records upstream Gradle 8.14.4 / AGP 8.13.2 / Kotlin 2.4.20 / modules `app,modules:book,modules:rhino`.
- Badcat `settings.gradle.kts` includes only `:app`, `:integration-contract`, `:legado-rhino`; it does not include upstream Gradle modules. Host app uses AGP 8.13.2, Kotlin 2.4.20, Parcelize, compileSdk 36, minSdk 26, Java/Kotlin 21; `:integration-contract` is Kotlin/JVM 21.
- `:integration-contract` already defines a custom `pinnedLegado` source set rooted at `upstream/legado/app/src/main/java`, but only explicitly includes selected AnalyzeRule files. The source set comments explicitly leave AnalyzeRule's app-coupled lifecycle separate. It is therefore a possible mechanism for selected upstream source reuse, not a pre-existing upstream model integration.
- The pinned submodule is a Git link, not a complete source directory in the Badcat Git tree. GitHub source inspection used the pinned upstream repository directly. A local build must initialize the submodule at the exact pinned SHA before the source set can resolve those files.

#### Exact upstream model dependency closure

- `BookSource.kt` (11,355 chars / 358 lines): Android Parcelable + Parcelize; Room Entity/Index/PrimaryKey/ColumnInfo/TypeConverter/TypeConverters; `AppPattern`, `BookSourceType`; six typed rule models; `AudioPlay`; `GSON` / `fromJsonObject` / `splitNotBlank`; `BaseSource`. It owns source identity, six rule getters, grouping/error-comment helpers, content-batch policy, login JS and equality/key behavior. Its converters and behavior are not equivalent to the host's JSON projection.
- `BaseSource.kt` (12,726 chars / 436 lines): active `JsExtensions` contract and methods for login UI v2, login detection/execution, login headers/info encryption, source variables, shared JS scope, JS library refresh, concurrent rate updates and JS evaluation. Direct dependencies include ScriptBindings/buildScriptBindings, RhinoScriptEngine, Hutool AES, AppConst/AppLog/AppPattern, RowUi, CacheManager, ConcurrentRateLimiter, JsExtensions, AppConfig, SymmetricCryptoAndroid, CookieStore, source-scope helpers, SharedJsScope, JsSourceEngine, LoginUiV2, Gson helpers, coroutine and Android JavascriptInterface. Pulling it in is a runtime subsystem, not a lightweight model seam.
- `Book.kt` (15,595 chars / 548 lines): Parcelable/Parcelize + Room Entity/Index/PrimaryKey/ColumnInfo/Ignore/TypeConverters; BookType/PageAnim/AppPattern; `appDb`; BookHelp/ContentProcessor and local-book helpers; AppConfig/ReadBookConfig; ReadBook; Gson/NetworkUtils; LocalDate/Charset/math. It includes persisted metadata, user overrides, current reading position and nested ReadConfig, but also database save/delete/migrate, reader config mutation, bookmarks and file/local-book helpers. Extracting only the constructor while omitting those methods would not be verbatim upstream model reuse.
- `BookChapter.kt` (7,047 chars / 210 lines): Parcelable + Room composite primary key `(url, bookUrl)`, indexes and FK cascade to Book; imports appDb, AppLog/AppPattern, RegexTimeoutException, RuleBigDataHelp, AppConfig, AnalyzeUrl/RuleDataInterface, ChineseUtils/GSON/MD5/NetworkUtils and UI toast helper, coroutines and `splitties.init.appCtx`. It has persisted URL/title/volume/base/book/index/VIP/pay/resource/tag/wordCount/range/fragment/variable/image fields plus lazy variables, update/save semantics, regex/URL/display-title helpers. Host `BookChapter(index,title,url)` is only a projection and is not serialization-compatible with this model.
- `BaseBook.kt` extends RuleDataInterface and depends on RuleBigDataHelp, Gson and split helpers; Book and BookChapter additionally rely on shared RuleDataInterface variable semantics. The source model graph therefore includes AnalyzeRule data contracts and large-variable persistence.
- The nested `Book.ReadConfig` and `Book.ParagraphAnchor` are declared inside Book, not standalone top-level files. ReadConfig is Parcelable and contains persisted reader preferences and optional LocalDate/ParagraphAnchor; ParagraphAnchor carries chapterUrl/itemId/chapterPosition. Authentic serialization tests cannot be made by copying only a locally shaped DTO and claiming upstream parity.
- Rule model sources currently exist in Badcat's app source tree and tests, while the host codec stores raw JSON and maps to a separate `app.badcat.engine.BookSource`. This preserves unknown source JSON at interchange time, but does not establish upstream BookSource runtime identity, converter semantics, methods or mutation lifecycle.

#### Host production ownership and call path

- Source catalog: `FileBookSourceCatalog` -> `LegadoBookSourceCodec` -> platform-neutral `app.badcat.engine.BookSource`; durable file is AtomicFile JSON. Engine construction: `BadcatReadingEngineFactory` wires the host catalog to SourceSearch/Explore/Detail/TOC/Content services and Rhino host runtime. The adapter graph consumes host projection types, not pinned upstream BookSource/Book/BookChapter.
- Existing persisted owners remain separate: `BadcatReadingStateRepository` (progress/bookmark/history SharedPreferences), `FileChapterContentCache` (chapter body AtomicFile), source catalog AtomicFile, TOC service/Activity memory projection. No production owner in this call path currently accepts the authentic upstream Book/BookChapter entities.
- Existing `:integration-contract` data classes `BookSource` and `BookChapter` are intentionally platform-neutral and their signatures are consumed by engine/service/UI layers. Replacing them in place with upstream entities would violate the Android-free contract and ripple Android/Room/Parcelize dependencies into consumers.

#### Boundary decision and open caller scan

- **Rejected:** add the three upstream entities verbatim to `:integration-contract`; it is Kotlin/JVM and deliberately Android-free. Also rejected: hand-copy only constructor fields into local classes while labeling them upstream models, or remove behavior-bearing methods/annotations to force compilation.
- **Not yet proven:** a dedicated Android library/source set may compile the original entities, but the direct closure immediately crosses into app DB, reader, login, cookie/cache, Rhino, crypto, global app context and helper services. No bounded closure or host adapter exists yet. Importing all of upstream `:app`/DB is outside the agreed minimal model PoC and would not demonstrate B′ isolation.
- The full tree identifies production source ownership paths above; however, a complete symbol-level caller graph for every projection field/method, Room converter, and lifecycle method is not yet evidenced. This audit does not claim the caller scan complete. Exact path reads for the current engine factory, catalog, codec and contract were verified; remaining UI callsites and source rule model use need a structured grep/AST scan in the next slice.
- **Result:** no safe minimal authentic-model integration seam has been demonstrated. PoC-0.7 remains **not accepted**. The evidence strongly indicates that BookSource's BaseSource lifecycle and Book/BookChapter's app DB/reader helpers make the naive model-only seam non-isolated. Before any code integration, compare (1) a true dedicated upstream app-layer slice with explicit service ports, (2) official model/DB ownership plus Badcat compatibility façade (hybrid), and (3) constrained A with lossless raw JSON and Badcat-owned durable models. B′ remains unpassed.
- **No implementation or tests changed in this inventory slice.** No Gradle/test/build/APK/CI pass is claimed. The preceding documentation registration commit has no runtime validation implications.
- **Next ordered task:** finish symbol-level host callsite scan and compare B′ exit options against exact acceptance criteria; choose one narrow next PoC and register it before editing code. Do not start Room/schema v1 or migrations until the model/data ownership decision is explicit.


### PoC-0.7 symbol-level host caller scan and architecture exit comparison (2026-11-20)

#### Verified production callsites and identity flow

- `ReadingEngine.kt` is the shared contract consumed by app UI. It declares independent host `BookSource`, `BookCandidate`, `BookDetail`, `BookChapter(index,title,url)`, and `ChapterContent(title,text,...)`. The host BookChapter has no upstream bookUrl composite key, row metadata, VIP/pay/resource flags, variables or serialization converters.
- `SourceReadingEngine.kt` resolves a source by `BookSource.id`, then calls host Search/Detail/TOC/Content services. Its chapter-content path synthesizes `BookChapter(index=0,title="",url=chapterUrl)` from a URL; therefore the current runtime does not carry an upstream chapter index or full chapter identity into content loading.
- `SourceTocService.kt` accumulates and returns host `List<BookChapter>`; `SourceChapterContentService.kt` accepts that host chapter and reads/writes cache by `(source.id, bookUrl, chapter.url)`. This cache key is host-scoped and URL-based, not the pinned upstream `(bookUrl,index)` batch identity.
- `SourceBookActivity.kt` owns chapter lists, restores progress by chapter URL, opens chapter UI with `sourceId/bookUrl`, and passes host chapters to content. It persists last chapter URL, scrollY, history and bookmark state through `BadcatReadingStateRepository`. A fallback chapter is constructed from URL when a chapter object is unavailable.
- `SourceSearchActivity.kt` consumes host `BookSource` and `BookCandidate`; it de-duplicates candidates using `sourceId + "|" + bookUrl`, persists result state as JSON sourceId/bookUrl, and navigates to SourceBookActivity using those fields.
- `FileBookSourceCatalog.kt` is the durable source catalog owner; `LegadoBookSourceCodec` imports/exports raw JSON and maps it into the host projection. `SourceCatalogActivity.kt` drives source exploration and uses the host BookCandidate to open books.
- `BadcatReadingStateRepository.kt` implements `ReadingProgressStore` using legacy SharedPreferences keys/JSON. Its durable identity is encoded `(sourceId,bookUrl,chapterUrl)`; progress API exposes last URL and scrollY, not upstream Book/ReadConfig.
- `FavoritesActivity.kt` owns a separate favorites JSON model keyed by `(sourceId,bookUrl)`; `BookmarkActivity.kt` and `ReadingHistoryActivity.kt` render repository-owned projections and navigate with the same triple identity. These are real independent persistence owners that a migration/transaction design must reconcile.
- `FileChapterContentCache.kt` owns chapter-body AtomicFile data under a digest of `(sourceId,bookUrl,chapterUrl)`; it is not an upstream CacheManager generic KV store and has no content-save fencing token. `ChapterContentCache.kt` exposes only get/put with those three string keys.
- Exact caller evidence confirms sourceId/bookUrl/chapterUrl is a cross-UI identity contract. Any model/DB replacement must preserve this navigation and old persisted key compatibility, while resolving chapter URL to a book-scoped index without ambiguity.

#### Ownership/semantic gaps discovered

1. Search/detail/TOC/content are wired through host projections; authentic upstream BookSource/Book/BookChapter are not accepted by these public contracts.
2. Content load from a URL synthesizes index 0, so batch-content candidate matching and upstream save token semantics are absent at the current UI-to-engine boundary.
3. Progress persists URL + scrollY only; upstream ReadConfig and ParagraphAnchor are not represented in the host progress contract.
4. Chapter cache is per-host identity and atomic file storage, with no upstream generic CacheManager, durable content token or stale-write fencing.
5. Favorites, bookmarks/history, progress, TOC and body cache have distinct owners. There is no shared transaction or single-writer cutover today.
6. Existing raw source JSON retention is useful for lossless import/export, but nested typed rule behavior still depends on host runtime adapters and cannot substitute for upstream model semantics.

#### Architecture options compared

| Option | Faithfulness | Migration/ownership risk | Scope | Decision |
|---|---|---|---|---|
| Continue B′ unchanged (Badcat-owned persistence + compatibility runtime) | Good for existing Badcat UI and source JSON; incomplete for authentic Book/BookChapter, ReadConfig/ParagraphAnchor, batch identity/token unless substantial adapters are added | Medium-high: must reconcile multiple existing stores and implement identity resolution/fencing | Medium-high | Keep only as a time-boxed candidate; current PoC gates are not passed |
| A — constrained Badcat-owned data layer with lossless upstream JSON and explicit runtime ports | Preserves current UI and legacy key owners; can incrementally add complete fields/behavior contracts, but does not claim upstream Room entity identity | Lower initial cutover risk; semantic drift risk remains unless differential fixtures against pinned upstream are mandatory | Medium, incremental | Viable fallback for user-visible progress while runtime parity grows; not yet accepted as full compatibility |
| Hybrid — authentic upstream model/DAO ownership behind Badcat compatibility façade | Highest fidelity for Room converters, model identity, ReadConfig and chapter fields; UI can remain unchanged through façade | Highest integration/migration risk: requires upstream app DB/helper closure, a single writer, data reconciliation and rollback; no narrow module boundary yet | Very high | Most faithful long-term option if full model/DB parity is mandatory, but blocked pending buildable module and rollback proof |

#### Gate decision

- **Do not claim B′ passed.** Its core premise (Badcat persistence is the single fact source) is not currently true across the listed stores, and the authentic upstream model closure is not isolated. The B′ candidate remains open only if the next PoC can demonstrate one writer per data domain, faithful URL↔(bookUrl,index) identity resolution, ReadConfig mapping, content token fencing, and restart/rollback behavior.
- **Do not switch to A as “full Legado compatibility”.** A can be a safe incremental host-owned persistence fallback, but must be labeled a constrained adapter architecture and pass differential behavior fixtures; it cannot erase missing upstream runtime behavior.
- **Hybrid is the fidelity target but not yet authorized for implementation.** Before choosing it, prove the exact upstream Gradle module/source-set closure, local submodule buildability, required DB/helper graph, and no dual-writer rollback protocol. Importing all upstream app code without a scoped boundary is a no-go.
- **Immediate next gate:** inspect existing upstream batch-content/cache/progress contracts and host tests; create a machine-checkable field/identity mapping matrix for the five PoC domains; then preregister one executable, narrow PoC. No schema v1, Room migration, broad source copy or UI change until that matrix and architecture choice are accepted.
- **Evidence limits:** caller scan is targeted to the production search/detail/TOC/content and progress/favorites/bookmark/history path. It does not claim every app source file or every upstream caller has been exhaustively indexed. No tests/build/CI/APK were run in this documentation-only slice.


### Pinned batch-content/cache/progress contract inspection (2026-11-20)

- Pinned `BatchContentContext.kt` explicitly states that chapter URL is not identity: the same URL may represent multiple chapters; `BookChapter.index` is the identity for batch resolution and cache naming. It indexes original, absolute and normalized URL aliases to candidate lists; string URL callbacks are accepted only for a unique match; chapter objects resolve by index; numeric identifiers are rejected to avoid confusing list positions with chapter.index.
- `BatchContentContext.saveContent` applies the source's content replace rule, requires a per-index `ContentSaveToken`, checks coroutine cancellation and closed state under a lock, and delegates to `BookHelp.saveContent`. Unreturned chapters are retried through the ordinary single-chapter path.
- Pinned `BookHelp.kt` defines `ContentSaveKey(bookUrl, chapterIndex)`, `ContentSaveToken` and a versioned `ContentSaveFence`. Writes publish only when the request's expected version is current; replace increments the version even on write failure to fence older in-flight writes. This is distinct from simply atomically replacing a file.
- Pinned `CacheManager.kt` is a generic source/runtime key-value API with memory LRU, Room Cache rows and ACache file/byte-array paths; TTL is expressed in seconds and memory caching differs for expiring vs non-expiring values. It is not the same domain as chapter-body content storage.
- Pinned `ReadBookConfig.kt` is a global reader-preference/configuration subsystem with persisted JSON files, style configuration and migration behavior; it is not identical to nested `Book.ReadConfig`. The book model's nested ReadConfig and ParagraphAnchor must be mapped separately from global reader style settings.
- Pinned `BatchContentContextTest.kt` covers same-URL ambiguity, relative/absolute alias ambiguity, chapter-object exact matching, numeric rejection, missing-chapter retry order and replacement no-op. These are upstream regression contracts that should be ported as differential tests, not approximated with host URL-only behavior.
- Upstream tree includes Room schema history through version 113 and tests for BatchContentContext and CacheManager TTL. This confirms a substantial database/cache lifecycle surface; it does not authorize importing the entire database or its 26 entities into the host.

#### Five-domain field/identity mapping — initial contract, not yet implementation-ready

| Domain | Pinned identity / key | Required semantic payload | Existing Badcat owner / mismatch | Minimum next PoC assertion |
|---|---|---|---|---|
| BookSource | `bookSourceUrl` (upstream source key/equality) | all source fields, six typed rule models, converters, customOrder, BaseSource lifecycle and JSON unknown-field retention | AtomicFile raw JSON + host BookSource projection; rule JSON stored as strings; active BaseSource contract not identical | authentic pinned Gson/converter round-trip, unknown fields, URL identity/equality and source lifecycle port parity |
| Book | `bookUrl` | persisted metadata, user overrides, source relation, current chapter index/position, nested ReadConfig + ParagraphAnchor, behavior methods | no canonical durable Book owner in source reading path; progress/preferences split into SharedPreferences/UI state | field/null/default/unknown serialization and restart round-trip with authentic nested ReadConfig/ParagraphAnchor; explicit ownership of each field |
| BookChapter | composite `(bookUrl,index)` for batch/content identity; entity also has URL/bookUrl relationship | full chapter metadata, URL/baseUrl, index, resource/pay/VIP/fragment/variables and RuleDataInterface behavior | host BookChapter(index,title,url), TOC memory list; content path synthesizes index 0 from URL | same-URL multi-index identity; URL→candidate resolution; reject ambiguous URL; never collapse to URL-only cache key |
| Reading progress | book identity + chapter index/position; URL is navigation alias, not sole identity | durChapterIndex/durChapterPos, nested ReadConfig, ParagraphAnchor, scroll position and legacy URL restoration | SharedPreferences stores last chapter URL and scrollY keyed by sourceId/bookUrl/chapterUrl | map URL to exactly one chapter index; duplicate/unknown URL behavior explicit; restart and legacy key preservation |
| Chapter body/cache | `(bookUrl,chapterIndex)` plus version token; source-scoped context | content bytes/text, save token/fence, invalidation, batch save/partial retry and separate generic CacheManager TTL API | AtomicFile cache keyed by sourceId/bookUrl/chapterUrl; no fencing token; generic source KV absent or separate stores | stale writer rejected; same-URL chapters remain separate; invalidation/token monotonicity; TTL semantics tested separately from body cache |

- **Mapping status:** the table records contract gaps and target assertions; it is not a frozen schema. The identity conversion from current `(sourceId,bookUrl,chapterUrl)` to upstream `(bookUrl,index)` requires source context and a unique chapter resolution step. If a URL maps to multiple indices, operations must require a chapter object/index rather than guess.
- **Architecture implication:** the current Badcat content path is not safe to expand into upstream batch content by changing only the cache filename; the public engine/UI contract currently discards the chapter index at the chapter-content boundary. Any implementation must first carry the chapter object/index through ReadingEngine and SourceChapterActivity while preserving URL-based navigation compatibility.
- **Next implementation candidate (not yet authorized):** a narrowly scoped, Android-free identity/contract PoC that carries chapter index and a typed chapter identity through the host engine/service boundary, plus differential fixtures for duplicate URL and URL ambiguity. It must not claim upstream BookChapter entity integration or introduce Room. Register exact files/callers/tests separately before editing.
- No tests/build/CI/APK executed in this inspection slice.


### PoC-0.8 registration — chapter-index identity carried to content boundary (2026-11-20)

- **Starting HEAD:** `50d665023d2a1419c409c4b0981a35de291f50e4`; branch only `integration/legado-upstream-runtime`.
- **Pinned behavior baseline:** Legado `330e606c4d57e08adc056ff8ab7a4132c784da9a`. No upstream source copy or schema change.
- **Goal:** remove the URL→synthetic-index-0 loss at the production content boundary. Preserve legacy URL-based callers/navigation while adding a typed chapter-object path that carries the actual TOC `index/title/url`.
- **Registered files:** `integration-contract/src/main/kotlin/app/badcat/engine/ReadingEngine.kt`; `integration-contract/src/main/kotlin/app/badcat/engine/SourceReadingEngine.kt`; `integration-contract/src/main/kotlin/app/badcat/engine/ChapterCatalogQuery.kt`; `app/src/main/java/app/badcat/client/SourceBookActivity.kt` (only chapter-index intent propagation, no visual/layout changes); `integration-contract/src/test/kotlin/app/badcat/engine/SourceReadingEngineTest.kt`; `integration-contract/src/test/kotlin/app/badcat/engine/ChapterCatalogQueryTest.kt` (if present; otherwise add exact test path after tree check).
- **Verified callers:** `SourceBookActivity.openChapter(BookChapter)`, bookmark chapter launch, reader's `loadInitial`, reader chapter-catalog selection/navigation, `ReadingEngine.chapterContent(sourceId,bookUrl,chapterUrl,bookName)`, and `SourceReadingEngine.chapterContent`. Other legacy URL callers remain supported through the existing overload.
- **Identity rule:** typed path carries `BookChapter(index,title,url)`; book scope remains the existing `bookUrl` argument. URL-only resolution is accepted only when exactly one chapter in the supplied catalog matches; zero or multiple matches returns no identity (caller must not guess). The TOC index is not the display-list position and is not recalculated after filtering/reversal.
- **Facts/transactions:** no persistence writer, database, progress key, cache key or transaction changes in this slice. Existing cache still uses `(sourceId,bookUrl,chapterUrl)`; this PoC does not claim upstream `(bookUrl,index)` cache identity or ContentSaveFence parity. Those remain separately blocked.
- **Compatibility risks:** existing URL-based entry points may continue to synthesize index 0 only as a clearly legacy path; the production Badcat reader should pass its selected TOC chapter object/index. URL-only resume/bookmark routes must resolve against the freshly loaded TOC and reject ambiguity; no index inferred from row position. Keep intent URL/title extras for compatibility and add index only.
- **Tests:** pure JVM tests for unique/missing/ambiguous URL resolver; engine test proving typed overload forwards exact index/title/url into content service; legacy URL overload regression; app reader intent propagation/build test if the current test harness permits. Verify duplicate URL chapters are not collapsed by the resolver, even if the current TOC parser independently de-duplicates exact source URLs.
- **Acceptance:** compile contract and app; focused tests pass; no UI diff except metadata intent; existing URL callers still compile; typed path demonstrably delivers selected chapter index to content parser/replacement context. Do not claim upstream batch resolver, cache fencing, or B′ acceptance.
- **Rollback:** revert only this PoC commit; no persisted data/schema changes, so no data migration required.
- **Explicit exclusions:** Room/schema v1, cache token/fencing, TOC parsing/dedup policy changes, progress persistence migration, official BookChapter entity import, UI redesign, broad runtime/API work.
- **Status at registration:** preregistered, implementation not yet started; no tests/build/CI/APK evidence.


#### PoC-0.8 implementation checkpoint (2026-11-20)

- **Actual code changed:** added a typed `ReadingEngine.chapterContent(sourceId, bookUrl, BookChapter, bookName)` overload while retaining the legacy URL overload; `SourceReadingEngine` forwards the supplied chapter object unchanged to `SourceChapterContentService` and rejects negative indices/blank URLs on the typed path. The old URL overload remains a compatibility bridge and still uses index 0; it is not considered the production identity-safe route.
- `ChapterCatalogQuery.findByUrl` now returns a chapter only for exactly one URL match; `findByIdentity` resolves by stable chapter index and verifies the URL alias. Filtering/reverse display order does not rewrite chapter indices.
- `SourceBookActivity` now passes chapter index through the reader Intent for TOC selection and bookmark selection; reader initial content and next/previous navigation call the typed overload. The fallback rendered chapter retains the requested index. “Continue reading” now resolves the saved URL uniquely and refuses to guess if it is stale or ambiguous. No UI layout/styling changes were made.
- **Tests added/updated:** `ChapterCatalogQueryTest` covers duplicate-URL ambiguity, typed index resolution, stale URL mismatch; `SourceReadingEngineTest` covers the actual non-zero chapter index/title arriving in the content replacement context and updates the end-to-end flow to call the typed overload.
- **Review limits / remaining callers:** older URL-only entry points outside this activity (including legacy history/bookmark navigation) remain source-compatible and can still reach the legacy index-0 bridge. They must be migrated to fresh-TOC unique resolution or typed chapter identity before the whole app can claim identity-safe content loading. The current TOC parser still de-duplicates exact chapter URLs; this PoC only proves the resolver rejects ambiguous catalogs, not upstream BatchContentContext parity.
- **Validation:** no local Gradle runner or repository execution environment is exposed through the GitHub connector. At exact HEAD `87a508d059a89ae9833b9ca9bfd183a60dbb2da0`, combined commit statuses returned an empty list and associated workflow runs returned an empty list. Therefore tests/build/CI are **not executed / not verified**; no APK, report hash, real-source or device evidence exists for this checkpoint.
- **Acceptance status:** implementation is partial; PoC-0.8 is **not accepted** until focused tests and app/contract builds run, legacy history/bookmark callers are migrated, and selected index is verified on device. No upstream cache identity/token fencing, DB/schema work, or B′ pass is claimed.
- **Next ordered work:** inventory and migrate all URL-only reader entry points (history, bookmark manager, resume/deep links) through fresh TOC unique resolution; then run focused tests/build/CI; only after that preregister a separate content-cache identity/token-fence PoC.


### PoC-0.9 registration — legacy history/bookmark URL resolution (2026-11-20)

- **Starting HEAD:** `72e0f02317c8f8eec6912199bb9d974ca9322914`; integration branch only. Re-read both mandatory checklists before this slice.
- **Goal:** remove the remaining production reader fallback that turns URL-only history/bookmark/deep-link launches into chapter index 0. Keep old intents and persisted URL keys readable, but resolve them against the freshly loaded TOC and proceed only on one exact URL match.
- **Verified callers:** `BookmarkActivity.kt` opens `SourceChapterActivity` with sourceId/bookUrl/chapterUrl/title and jump-to-bookmark flag; `ReadingHistoryActivity.kt` opens the same Activity with sourceId/bookUrl/chapterUrl/title. Neither persists or supplies chapter index. `SourceChapterActivity.loadInitial` currently starts TOC and content concurrently and constructs a fallback chapter at index 0; this is the remaining identity-loss point.
- **Registered implementation file:** `app/src/main/java/app/badcat/client/SourceBookActivity.kt` (reader Activity portion only; no layout changes). Keep `BookmarkActivity.kt` and `ReadingHistoryActivity.kt` unchanged unless an exact caller contract requires a metadata-only change; their URL-only storage remains backward compatible.
- **Contract/test files:** `integration-contract/src/main/kotlin/app/badcat/engine/ChapterCatalogQuery.kt`; `integration-contract/src/test/kotlin/app/badcat/engine/ChapterCatalogQueryTest.kt`; add/extend a pure JVM test for legacy URL resolution semantics if the resolution helper is moved into the contract module.
- **Identity behavior:** when Intent has a non-negative chapter index, retain the current fast parallel TOC/content path and validate index+URL against TOC. When index is absent, await TOC, resolve URL using `findByUrl` (exactly one match), and only then call the typed `chapterContent(BookChapter)`; if missing or ambiguous, return a typed NOT_FOUND/PARSE failure and do not issue a content request or synthesize index 0. Numeric list positions are not identities.
- **Facts/transactions:** no persisted schema, progress/bookmark/history keys, cache key, writer, or transaction changes. No migration needed. URL-only legacy persistence stays as navigation alias.
- **Risks:** legacy history entries whose URL no longer appears in TOC will fail closed rather than opening a possibly wrong chapter. A slow/failed TOC will delay or prevent legacy URL-only launch; indexed TOC launches retain parallel behavior. Preserve jump-to-bookmark scroll restoration after successful resolution.
- **Tests:** exercise unique legacy URL resolution, missing URL, duplicate URL ambiguity, no content HTTP request on failed resolution, typed-index path still parallel, and bookmark/history launch compatibility. Run contract tests and app compile when runner is available.
- **Acceptance/rollback:** no URL-only launch may synthesize index 0; exact unique TOC chapter object must reach content service. Revert this isolated slice to roll back; no persisted data changes.
- **Exclusions:** changing bookmark/history data schema, Room, chapter cache identity/token fence, TOC URL de-dup policy, UI redesign, upstream BatchContentContext implementation.
- **Status at registration:** preregistered; implementation not started; PoC-0.8 remains partial/not accepted.


#### PoC-0.9 implementation checkpoint (2026-11-20)

- **Actual change:** `SourceChapterActivity.loadInitial` now preserves the indexed fast path for intents carrying a non-negative chapter index. For legacy history/bookmark intents without an index, it awaits the current TOC, calls `ChapterCatalogQuery.findByUrl`, and only calls typed `chapterContent(BookChapter)` if exactly one chapter matches. Missing/ambiguous URLs produce `NOT_FOUND` without requesting the requested chapter body.
- Stale or ambiguous catalog resolution no longer silently redirects to the first chapter. It reports that the saved chapter cannot be uniquely located and asks the user to return to the TOC. Existing `BookmarkActivity` and `ReadingHistoryActivity` URL-only intents and stored keys remain unchanged and are handled by this resolver.
- **No persistence change:** no bookmark/history/progress key, database, cache, transaction or UI layout changed. No migration required.
- **Validation:** code path and callers were reviewed from the integration branch. At exact HEAD `6fb7cd3d79f5bb9987b85aaae8d125105a576074`, GitHub combined statuses and associated workflow runs both returned empty lists. Tests/app build/CI remain not executed or verified; no APK/device/real-source evidence.
- **Acceptance:** behavior is implemented but PoC-0.9 is not accepted until the focused contract tests and app build execute, and the complete URL alias/normalization contract is compared with upstream BatchContentContext. This also does not prove cache identity or save-token fencing.
- **Next:** run the focused tests/build in an actual runner when available; then proceed to a separate preregistered cache identity + ContentSaveFence PoC. Do not begin Room/schema v1.


### PoC-1.0 registration — chapter cache index identity and stale-write fencing (2026-11-20)

- **Starting HEAD:** `4038ecf6e76ee8e5a4559a0a370a333dc437bc16`; integration branch only. Both mandatory checklists reread.
- **Pinned source contract:** Legado `330e606c4d57e08adc056ff8ab7a4132c784da9a`, `app/src/main/java/io/legado/app/help/book/BookHelp.kt` lines 59–125, 238–269, 359–369; `app/src/main/java/io/legado/app/model/BatchContentContext.kt` lines 31–145. Upstream key is `(bookUrl,chapterIndex)`; request-start tokens capture current version; normal writes publish only at expected version; replace increments version even if disk write throws; batch save requires token by chapter index and URL-only identifiers must be unique.
- **Registered files:** `integration-contract/src/main/kotlin/app/badcat/engine/ChapterContentCache.kt`; `integration-contract/src/main/kotlin/app/badcat/engine/SourceChapterContentService.kt`; add Android-free fence implementation under `integration-contract/src/main/kotlin/app/badcat/engine/`; `app/src/main/java/app/badcat/client/runtime/FileChapterContentCache.kt`; `app/src/test/kotlin/app/badcat/client/runtime/FileChapterContentCacheTest.kt`; `integration-contract/src/test/kotlin/app/badcat/engine/SourceChapterContentServiceTest.kt`.
- **Verified callers/implementations:** `SourceChapterContentService.load(source,bookUrl,BookChapter)` is the production content cache caller; `BadcatReadingEngineFactory` injects `FileChapterContentCache`; integration service tests contain inline fake cache implementations. Cache interface currently keys get/put by `(sourceId,bookUrl,chapterUrl)`; AtomicFile digest uses the same URL key; service performs cache get before network and put after successful parsing. There is no request token or stale-write fence.
- **Target identity:** body-cache key becomes `(sourceId,bookUrl,chapter.index)`; chapter URL remains stored navigation metadata and must match on cache read. Same book + same index + changed URL invalidates/rejects the stale entry. Same URL at two indices must not alias. Keep sourceId host scoping; do not conflate this with upstream generic CacheManager.
- **Token semantics:** acquire token before network; concurrent normal requests for one chapter share current generation; replacement/invalidation increments generation; publish succeeds only if token is still current and key/url identity matches. A stale request may return its fetched body to its caller but must not overwrite cache. Failed replace still advances version, fencing prior in-flight writers. Token is runtime coordination state, not yet durable across process restart; explicitly document this difference from full persistence/rollback.
- **Legacy cache compatibility:** cache is disposable and may be safely cold-started under the new index key; do not read old URL-only files as if their chapter index were known. Old files remain subject to bounded pruning. This avoids cross-index cache contamination; no user progress/bookmark data is touched.
- **Tests:** pure JVM fence tests for monotonic version, same-URL/different-index isolation, stale writer rejection, failed replace still invalidating, and concurrent publication; service tests proving token is captured before fetch and stale result is not published; Android cache tests for index-separated files, URL mismatch rejection, size bounds and AtomicFile behavior.
- **Acceptance:** focused contract + app cache tests and app build pass; actual content cache distinguishes chapter index; stale async writes cannot replace newer content; legacy progress/bookmark behavior unchanged. Do not claim upstream BatchContentContext or durable token parity.
- **Rollback:** revert the isolated PoC; cache entries are disposable and will simply be refetched. No schema or migration.
- **Exclusions:** Room/schema v1, official BookHelp import, batch source-script API, generic CacheManager TTL/LRU, TOC parsing/dedup policy, Book/BookChapter entity migration, UI changes.
- **Status at registration:** preregistered; no code edits yet; prior CI/test execution still unavailable.


#### PoC-1.0 implementation checkpoint (2026-11-20)

- **Actual code changed:** added platform-neutral `ChapterContentCacheKey(sourceId,bookUrl,chapterIndex)`, request-start `ChapterContentSaveToken`, and synchronized `ChapterContentSaveFence`. Normal publication is accepted only for the current generation; `replace` and `invalidate` advance generation first, including when a replacement write throws.
- `ChapterContentCache` now exposes typed chapter-object get/put, begin-token, currentness and invalidation methods while retaining legacy URL methods for source compatibility. `SourceChapterContentService` captures the token before cache/network work, checks token currentness before serving cached content or offline fallback, and publishes only through the typed token path. Invalid negative chapter indices are rejected.
- `FileChapterContentCache` now uses `(sourceId,bookUrl,chapterIndex)` digests for typed operations, stores chapter URL as metadata and rejects a cache entry whose stored URL no longer matches the current chapter. A process-wide fence is shared by all cache instances; invalidation increments generation and deletes the indexed entry under the shared cache mutex. Old URL-keyed files are not imported into the new identity space; they are disposable and pruned by the existing bounded cache policy.
- **Tests added:** `ChapterContentSaveFenceTest` covers current publication, stale write rejection, failed replacement fencing, independent chapter-index generations and invalidation. `SourceChapterContentServiceTest` simulates invalidation during an in-flight network request and verifies the body may return to the caller but is not published. `FileChapterContentCacheTest` covers index-separated digest keys and separation from legacy URL keys.
- **Important limits:** fence generations are in-process only, like the pinned upstream BookHelp fence; this is not a durable cross-process save-token protocol. The interface's legacy default methods preserve compatibility for old test/adapters but do not themselves provide fencing; production `FileChapterContentCache` overrides the typed methods. This PoC does not implement upstream batch `contentBatch`, source callback matching, generic CacheManager, or all invalidation triggers.
- **Validation:** no local Gradle runner is available through the repository connector. The code and test diffs were reviewed, but tests/build/CI are not executed or verified. No APK/device/real-source evidence.
- **Acceptance:** PoC-1.0 remains partial/not accepted until focused JVM + Android cache tests and app build execute, and all production cache mutation/clear paths are audited to invoke invalidation. The five-domain B′ PoC remains unpassed.
- **Next:** audit every chapter-cache delete/replace/clear callsite and wire explicit invalidation; then run tests/build/CI. Do not start schema v1 or Room.


#### PoC-1.0 contract tightening and mutation-path audit (2026-11-20)

- Tightened `ChapterContentCache`: index-keyed `beginSave/get/isCurrent/put/invalidate` are now required methods, not silent default fallbacks. Legacy URL-keyed methods remain explicit compatibility methods. Production `FileChapterContentCache` implements the required typed contract; test fakes were updated to do so.
- Re-read pinned `BookHelp.kt` mutation paths: upstream `saveContent` uses expected-version publication; direct replacement and `delContent` advance the fence before writing/deleting; resource refresh fences old chapter writes too. Host's current production graph has no separate chapter-cache clear/edit API or additional caller outside `SourceChapterContentService` and `FileChapterContentCache`; bounded pruning is the only other file deletion path. The new explicit `invalidate(sourceId,bookUrl,index)` is available for future mutation wiring, but no existing product UI action currently calls it.
- **Remaining semantic gap:** the host does not yet expose a user-driven direct replace/delete operation equivalent to upstream `BookHelp.saveContent(replace)` / `delContent`; the fence primitive and invalidation path are implemented and tested, but no equivalent product action was found in the current host reader. Do not claim complete mutation lifecycle parity.
- Latest code/test commits in this slice include `b6e5c350f6ba19fdc9e6affb1c883721440af568` and earlier sequential commits; the exact current HEAD must be used for validation below.
- Tests/build/CI have not been executed. PoC-1.0 remains partial/not accepted pending real runner execution and remaining mutation lifecycle review.


#### PoC-1.0 additional differential fixtures (2026-11-20)

- Tightened the JVM service fixtures so every fake cache must implement the required index-keyed/fenced contract. Added a service-level same-URL/different-index test: two chapters with one URL are stored under separate chapter indices, and re-reading the first chapter hits only its own cache entry. Added explicit service rejection for negative chapter indices.
- The pinned upstream mutation review found `BookHelp.saveContent` expected-version writes, direct replace/delete fencing, and resource-refresh fencing. The host currently has no independent chapter-body edit/delete UI or additional body-cache deletion caller; the explicit cache invalidation API is ready but no current product action invokes it. This is a known gap, not a completed parity claim.
- Latest implementation/test HEAD before this documentation update: `e6426f8f119f524ad9e386153c03b8f0cd97a4a9`. Combined statuses and associated workflow runs were empty; no test/build/CI/APK/device result is available.


### PoC-1.0 build-blocker correction registration (2026-11-20)

- CI run `44652321210` reached `:integration-contract:test` successfully, then failed at `:app:compileDebugKotlin`; APK assembly was skipped. Exact compiler errors: `BadcatReadingStateRepository.kt` missing `bookmarkKey`; `SourceBookActivity.kt` duplicate local `readingState` declarations at lines 415/445; `SourceBookActivity.kt` references out-of-scope `chapterTitle`; missing `EngineErrorCode` import. The first two repository/activity errors were pre-existing at the checked HEAD; the latter two were introduced by PoC-0.9.
- **Registered corrective scope:** `app/src/main/java/app/badcat/client/BadcatReadingStateRepository.kt` (restore the existing bookmark key encoding as `BOOKMARK_PREFIX + encodeIdentity(sourceId,bookUrl,chapterUrl)`, preserving current preference keys); `app/src/main/java/app/badcat/client/SourceBookActivity.kt` (remove duplicate local declaration, use the method's `fallbackTitle` parameter, add exact `EngineErrorCode` import). No other data format, UI or behavior change.
- **Acceptance:** app Kotlin compilation succeeds; contract tests remain passing; rerun Android debug workflow and only claim APK after `assembleDebug` and artifact upload complete.
- Status: preregistered after exact CI failure log inspection; no correction code changed yet.


### PoC-1.0 test-compilation blocker correction registration (2026-11-20)

- Latest code-head CI run `44652704662` confirms `:integration-contract:test` passed and `:app:compileDebugKotlin` now passed after the preceding fixes. It then failed at `:app:compileDebugUnitTestKotlin` only: `BadcatReadingStateRepositoryTest.kt:15` calls `RuntimeEnvironment.getApplication<android.app.Application>()`, but Robolectric 4.14.1 exposes a non-generic `getApplication()` method.
- **Registered scope extension:** `app/src/test/kotlin/app/badcat/client/BadcatReadingStateRepositoryTest.kt`, one-line test-harness compatibility correction only: call `RuntimeEnvironment.getApplication()` without type arguments. No production code or test assertions change.
- **Acceptance:** app unit-test sources compile; then complete the remaining Android tests and APK build in the same workflow. This is a test compile correction, not evidence of tests passing.
- Status: preregistered after exact CI log review; edit not yet made.


### PoC-0.6 CI failure triage and test-fixture correction registration (2026-11-20)

- **Starting HEAD:** `42e6215a177a0e965b75d93ff44f1e0f0ddbca22` on `integration/legado-upstream-runtime`; mandatory master and category-3 checklists reread before this task. Pinned upstream baseline remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Exact CI evidence:** Android workflow run `44653268320` for this HEAD. `:integration-contract:test`, pinned Legado verification, Rhino artifact verification, `:app:compileDebugKotlin`, and `:app:compileDebugUnitTestKotlin` all passed. `:app:testDebugUnitTest` ran 290 tests and failed 2; APK assembly was skipped. No APK exists for this run.
- **Failures:** `LegadoGsonCompatibilityTest.initialGsonUsesPinnedIntegerAndStringCoercion` failed at line 74 because the fixture deserializes into raw `Map::class.java`, which does not match the exact registered `TypeToken<Map<String?, Any?>?>` adapter. `allSixRuleConvertersAcceptObjectAndStringEncodedObject` failed at line 27 with EOF because the quoted-object fixtures are malformed JSON string literals (inner JSON quotes are not escaped). These are test-fixture defects to correct first; production Gson source is not changed in this registration.
- **Upstream comparison:** host `app/src/main/java/io/legado/app/utils/GsonExtensions.kt` and pinned upstream `app/src/main/java/io/legado/app/utils/GsonExtensions.kt` are byte-for-byte identical after LF normalization (host blob `c0bebcb9bf072f04a680e269765bb5b2e76cd88d`; upstream blob `c0bebcb9bf072f04a680e269765bb5b2e76cd88d`). Preserve this source; do not weaken adapters to satisfy invalid tests.
- **Registered exact scope:** only `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt`. Replace raw Map target with the same parameterized TypeToken used by INITIAL_GSON registration; construct each string-encoded rule fixture by JSON-encoding the inner object (for example, `GSON.toJson("""{"bookList":"li.book"}""")`) rather than malformed manually quoted text. Keep assertions for object-form and valid string-encoded-object form across all six rules, integer coercion, unknown fields, null/array handling, and serialization.
- **Callers/ownership:** test-only access to the actual host `INITIAL_GSON`/`GSON` and the six pinned rule models. No production caller, persistence writer, transaction, database, cache, UI, or Gradle change is in scope.
- **Risks and contract:** do not change expected pinned semantics to fit a raw/untyped fixture; string-wrapped JSON must be valid JSON at the outer layer and contain a complete object at the inner layer. Preserve actual upstream rule deserializers and Gson registrations.
- **Acceptance:** rerun workflow; both failing contract tests pass; all app unit tests pass; APK assemble/upload completes. Record exact run ID, test count, artifact SHA and any later live/device evidence. If another failure appears, preregister its exact scope before changing production code.
- **Rollback:** revert the isolated test-only commit; no user data or runtime behavior changes.
- **Status:** preregistered after exact CI log and pinned-source comparison; correction not yet implemented. PoC-0.6 and B′ remain unaccepted.


#### PoC-0.6 fixture correction implementation checkpoint (2026-11-20)

- **Actual change:** only `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt` changed at code commit `e278d9f3715e2b4b9aeced1d897857c47c591e95`. The number-map fixture now uses the exact parameterized `TypeToken<Map<String?, Any?>?>` registration. All six string-wrapped rule fixtures now use `GSON.toJson(innerJson)` to produce valid outer JSON string values; object-form fixtures and original semantic assertions remain.
- No production Gson/rule model, Gradle, data, cache, UI or persistence code changed. Pinned Gson implementation remains byte-identical to upstream at the recorded SHA.
- **Validation pending:** push-triggered workflow run `44703572220` was pending when this entry was written; no tests, APK or CI pass is claimed yet. Update this checkpoint with exact final run outcome and artifact evidence after completion.


### PoC-0.6 CI test-compilation blocker registration (2026-11-20)

- **起始 HEAD：** `8dfdf1fb93c5bed770f78b5d4a6b9c5eaa81fab7`；当前分支仅 `integration/legado-upstream-runtime`。
- **证据：** Android debug workflow run `44704579262`，job `136180875657`，在 `:app:compileDebugUnitTestKotlin` 失败；唯一 Kotlin 错误为 `LegadoGsonCompatibilityTest.kt:74:32 Cannot infer type for type parameter 'T'`。主代码 Java/Kotlin 编译已到达测试编译阶段；这不是运行时测试断言失败，也不能据此宣称测试通过。
- **精确范围：** 仅 `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt` 第 74 行附近的 `INITIAL_GSON.fromJson(..., mapType)` 调用；显式声明接收类型 `Map<String?, Any?>?` 或显式泛型类型参数，保留已登记的 TypeToken、JSON fixture 与全部断言。不得改生产 Gson、模型、Gradle、数据库、缓存或 UI。
- **调用方/所有权：** 仅该测试直接调用实际 host `INITIAL_GSON`；无生产 caller、持久化 writer、事务或 UI 影响。
- **上游/契约：** 固定 Legado SHA `330e606c4d57e08adc056ff8ab7a4132c784da9a`；保留上游 Gson adapter 注册和参数化 Map adapter 语义，不使用 raw Map 降级。
- **风险与测试：** Kotlin/Gson Java 泛型重载推断；验收必须由 Android CI 完成 `:app:testDebugUnitTest` 与 `:app:assembleDebug`，确认编译通过且六类规则、整数 coercion、null/unknown field 测试真实执行。检查 exact HEAD 对应 run 和产物；不得把编译成功当成完整 PoC 通过。
- **排除/退出：** 不触碰 Room/schema v1、模型接入或 B′架构切换；若显式类型仍无法匹配真实 adapter，则暂停并检查依赖/重载，不改生产语义迁就测试。
- **状态：** 已根据 run 日志登记，修正尚未实施；PoC-0.6 与 B′仍未验收。


#### PoC-0.6 CI compile blocker correction checkpoint (2026-11-20)

- **实际变更：** 在 `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt` 将 Map 反序列化结果显式声明为 `Map<String?, Any?>?`，为 Kotlin/Gson 的泛型重载提供目标类型；保留参数化 TypeToken、fixture 和原断言。代码提交：`44e8fc61cca6ac2305fbed2d81722924ec6e7f48`。
- **登记先后：** 精确修正范围已先写入本清单，登记提交 `1460e518083d7227917482b4a572c74e7b96fa1f`，之后才改测试代码。
- **排除项核对：** 未改生产 Gson、规则模型、Gradle、Room/数据库、缓存、持久化或 UI；固定上游 SHA 未变。
- **验证：** 该代码提交会触发 Android debug workflow；当前登记时尚无该 exact HEAD 的完成结果。等待 CI 对 `:app:compileDebugUnitTestKotlin`、单元测试和 debug assemble 给出结果，不预先宣称通过；无 APK/测试报告 hash 证据。
- **状态：** 仅修复 Kotlin 测试编译器类型推断问题；PoC-0.6、B′和完整运行时均未验收。


### PoC-0.7 ReadConfig / ParagraphAnchor authentic dependency-closure and caller audit (2026-11-20)

- **审计基线：** Badcat integration 分支；固定上游 `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`。本次仅审计，不修改运行时、Gradle、数据、UI 或测试。
- **名称澄清：** 逐书阅读配置实际是 `io.legado.app.data.entities.Book.ReadConfig`，不是 `io.legado.app.help.config.ReadBookConfig.Config`。后者是全局/共享阅读排版样式配置（背景、颜色、字体、间距、页眉页脚等），依赖 Android Drawable、AppConfig、Preference、文件/图片/压缩和全局 appCtx；不可拿它代替 Book.ReadConfig。
- **固定上游字段契约：** `Book.ReadConfig` 有 21 个字段：`reverseToc=false`、`reverseTocDisplay=false`、`tocExpanded=true`、`pageAnim: Int?=null`、`reSegment=false`、`imageStyle: String?=null`、`useReplaceRule: Boolean?=null`、`delTag=0L`、`ttsEngine: String?=null`、`splitLongChapter=true`、`readSimulating=false`、`startDate: LocalDate?=null`、`startChapter: Int?=null`、`dailyChapters=3`、`openCredits=0`、`closeCredits=0`、`playMode=0`、`playSpeed=1.0f`、`useGlobalAudioSkip=false`、`manualReplaceRuleIds: List<Long>=emptyList()`、`paragraphAnchor: ParagraphAnchor?=null`。不能把缺失 nullable 和缺失 non-null default 混为一谈。
- **段落锚点契约：** `Book.ParagraphAnchor` 是嵌套 Parcelable，字段为 `chapterUrl: String`、`itemId: String`、`chapterPosition: Int`。它不是单纯的 chapter URL/scrollY；ReadBook 和阅读页用它在段落重排、布局刷新、TTS 段落进度/恢复时定位。丢失 itemId 或 chapterPosition 会破坏语义。
- **真实模型依赖闭包：** 两个嵌套类型虽仅有数据字段，但声明在 `Book.kt`，而整个 Book 同时依赖 Android Parcelable/Parcelize、Room Entity/Index/PrimaryKey/ColumnInfo/TypeConverters、BaseBook/RuleDataInterface、BookType/AppPattern/PageAnim、appDb、BookHelp/ContentProcessor、AppConfig、ReadBook、GSON、NetworkUtils、LocalDate 及本地书籍/文件辅助函数。Book.Converters 以 `GSON.toJson(ReadConfig?)` 序列化，反向经 `GSON.fromJsonObject<ReadConfig>(json).getOrNull()`；Room Entity/DAO 也把 readConfig 作为 Book 列持久化。直接把 Book.kt 放进 Android-free `:integration-contract` 不成立；单独复制同名 DTO 会形成影子模型，禁止。
- **上游调用链证据：** `Book.config` 对 null lazily 创建 `ReadConfig(useGlobalAudioSkip=true)`（注意这与构造默认 `false` 不同）；BookDao 的更新/迁移路径读取和写回 readConfig JSON；`ReadBook` 的段落锚点流程将 `Book.ParagraphAnchor` 写入 `book.config.paragraphAnchor` 并在重新布局时校验 chapterUrl/chapterPosition；`BaseReadAloudService` 从 TTS speech item 生成锚点；阅读页消费锚点恢复段落。上游还存在 ReadConfig JSON 合并更新、备份恢复、LocalDate ISO 序列化和 manualReplaceRuleIds round-trip 测试。
- **Badcat 现状与调用方：** `BadcatReadingStateRepository` 只持有 lastChapterUrl、chapterScrollY、书签 scrollY、历史记录；键以 `sourceId + bookUrl + chapterUrl` 隔离。SourceBookActivity 通过目录恢复 URL；SourceChapterActivity 按 chapterUrl 存取 WebView scrollY、书签和历史；ReadingHistoryActivity/BookmarkActivity 只回传 URL/title。没有 Book.ReadConfig 或 ParagraphAnchor 的生产读写者，也没有逐书配置 JSON 的事实源。当前 scrollY 是像素坐标，不等价于 Legado 的段落 itemId/chapterPosition。
- **架构结论：** B′的 Android-free integration-contract 不能承载原样 Parcelable/Room Book 嵌套模型；Android host 直接引用上游 Book 又会把 Room/appDb/Reader 服务依赖带入产品边界。最小忠实接缝尚未证明。候选只能进一步比较：(1) 将 pinned 上游实体及必要依赖闭包置于独立 Android 模块并隔离 DAO/运行时边界；(2) 保留 Badcat 自有存储，使用显式 JSON compatibility facade，但必须证明字段/默认/null/unknown/LocalDate/锚点 round-trip，不伪造官方类型；(3) 触发 B′退出评估，限定 A/Hybrid。当前不能决定切换 A，也不能冻结 schema v1。
- **禁止事项：** 不新建简化 `ReadConfig`/`ParagraphAnchor` shadow DTO；不把 WebView scrollY 映射为 paragraphAnchor；不把全局 `ReadBookConfig.Config` 塞进 Book；不引入 Room/schema v1；不改现有 preference key。
- **下一门禁：** 精确检查上游 `Book.Converters`、GSON 的 LocalDate adapter、BookDao readConfig JSON property mutation、ReadBook/TTS 锚点恢复测试；同时枚举 Badcat 所有 progress/bookmark/history caller，形成字段/身份/null/default/unknown 对照和候选模块边界图。只有真实类型 round-trip 与 caller/单写者方案明确后，才能另行登记可执行 PoC。
- **状态：** 依赖闭包和关键调用链已定位；完整上游实体接入未实现；Badcat 映射未实现；测试/build/CI/APK/真机验收均未由本次审计提供。B′仍未通过。


### PoC-0.6 nullable Map test-assertion compilation correction registration (2026-11-20)

- **Exact evidence:** workflow run `44705280574` for code HEAD `44e8fc61cca6ac2305fbed2d81722924ec6e7f48` reached `:app:compileDebugUnitTestKotlin`; Kotlin then rejected direct `map["..."]` access at lines 78–80 because the explicit receiver type was nullable `Map<String?, Any?>?`. The earlier generic inference error is gone. No unit tests ran and no APK was produced.
- **Scope:** only `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt` in `initialGsonUsesPinnedIntegerAndStringCoercion`. Keep the exact parameterized TypeToken and fixture; make the test assert a non-null deserialized map before indexing (e.g. `requireNotNull(INITIAL_GSON.fromJson<Map<String?, Any?>>(json, mapType))`), retaining all existing value assertions. Do not change production Gson/adapters or expected coercion semantics.
- **Callers/ownership:** test-only; no production caller, persistence writer, transaction, database, cache or UI changes.
- **Risk/test:** Kotlin Gson overload/generic nullability; exact acceptance is unit-test source compilation, then all app unit tests pass and `:app:assembleDebug` completes on the same exact code HEAD. Check artifact and report hashes before claiming success.
- **Exclusions:** no ReadConfig model integration, Room/schema v1, persistence migration, UI or runtime code changes. Roll back this test-only change if explicit generic invocation does not bind to the registered TypeToken adapter.
- **Status:** preregistered from exact run log; correction not yet implemented; PoC-0.6 and B′ remain unaccepted.


#### PoC-0.6 nullable Map assertion correction implementation checkpoint (2026-11-20)

- **Actual change:** test now uses explicit `INITIAL_GSON.fromJson<Map<String?, Any?>>(json, mapType)` and wraps the result in `requireNotNull` before the existing index/value assertions. This preserves the registered parameterized TypeToken and makes the fixture's non-null precondition explicit. Code commit: `34f7dfd0ea4842ef75af1420a66231c16e4befaa`.
- **Registration order:** the exact nullable-receiver compile failure and one-file scope were recorded first in commit `c2d3a8249eb1f717cd4096b996c239710c2ccac6`; only then was test code changed.
- **Exclusions:** no production Gson, models, Gradle, Room, persistence, cache, UI or runtime code changed. The separate ReadConfig/ParagraphAnchor dependency audit is documentation-only and does not authorize model integration.
- **Validation:** workflow triggered by the code commit is pending at this checkpoint; no unit-test, assemble, APK, artifact hash or device pass is claimed. Exact next CI run must compile the test, execute the full suite and assemble debug.
- **Status:** test-only correction; PoC-0.6 and B′ remain unaccepted.


### PoC-0.6 Gson primitive-vs-boxed Int fixture mismatch registration (2026-11-20)

- **Exact evidence:** CI run `44706206576`, exact HEAD `b923d122bc8db27619d2cf51ee12e60148e070bb`, compiled all production and unit-test sources and executed 290 tests; 289 passed, 1 failed. The failing test is `LegadoGsonCompatibilityTest.initialGsonUsesPinnedIntegerAndStringCoercion`, line 85: `JsonSyntaxException: Expected an int but was BEGIN_OBJECT`. JUnit report artifact `badcat-unit-test-reports`, artifact ID `14615531751`, SHA-256 `05409ab85a0b045f071b32a610c00d1cdfd054b4a3c9c9ccf1d36de8dbac9eb6`.
- **Root cause from exact source:** production `INITIAL_GSON` registers `IntJsonDeserializer` against `Int::class.java` (primitive int class); test incorrectly requests `Int::class.javaObjectType` (boxed `java.lang.Integer`), bypassing the registered adapter. The failure is fixture/type mismatch, not evidence that production adapter fails its registered type.
- **Registered scope:** only the two assertions in `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt` change target class from `Int::class.javaObjectType` to exact registered `Int::class.java`. Keep malformed-object-to-null and numeric-truncation assertions. No production Gson, adapter registration, dependencies, models, Gradle, Room, persistence, cache or UI changes.
- **Callers/ownership:** test-only; no production data writer, transaction, runtime or UI impact.
- **Acceptance:** exact next code HEAD must compile, execute all app tests with all 290+ passing (including malformed object returns null and 7.9 coerces to 7 using primitive adapter), then complete `:app:assembleDebug`; capture exact run, report and APK hashes. If primitive-target test does not invoke the registered adapter as expected, stop and inspect Gson TypeAdapter lookup; do not widen production registration as a test workaround.
- **Status:** scope registered before implementation. Prior run's unit tests are not all passing; APK was not assembled. PoC-0.6 and B′ remain unaccepted.


#### PoC-0.6 primitive Int fixture correction implementation checkpoint (2026-11-20)

- **Actual change:** both Int contract assertions now request `Int::class.java`, exactly matching the primitive class used by production `INITIAL_GSON.registerTypeAdapter`; boxed `Int::class.javaObjectType` was removed from this fixture. No expected values or production behavior changed. Code commit: `d3f5efd224943781adb5ed7dc7fce39362252aa5`.
- **Registration order:** exact failing test and adapter-class mismatch were recorded first in `ac09f0a26957fffd2b843c2082a9eb32458b451c`.
- **Validation:** push-triggered Android CI is expected for the code commit; no result is claimed yet. Must see all app tests pass and debug APK assemble on exact HEAD. Previous 290-test run had 289 pass/1 fail and did not assemble APK.
- **Exclusions:** no production Gson, Gradle, model, Room, database, cache, runtime or UI change. B′ and PoC-0.6 remain unaccepted.


#### PoC-0.6 CI scheduling checkpoint after primitive fixture correction (2026-11-20)

- Code commit `d3f5efd224943781adb5ed7dc7fce39362252aa5` triggered run `44706931097`, but that exact run was cancelled after a subsequent documentation push entered the workflow concurrency group. This is a scheduler cancellation, not a test result.
- The earlier documentation-only run `44706902363` began before the code correction and therefore does not validate it.
- This checkpoint is intentionally the final documentation push before waiting for a fresh workflow on the current tree. The new workflow must include code commit `d3f5efd224943781adb5ed7dc7fce39362252aa5` and the exact resulting HEAD; do not push another commit until that run completes, to avoid cancelling it again.
- Required evidence remains: all unit tests pass and `:app:assembleDebug` completes; capture report/APK artifact IDs and SHA-256. No current pass or APK is claimed.


## 2026-11-20 重新定界注册：完整 Legado 书源运行能力（最高优先级）

- **用户确认的产品目标：** 坏坏猫继续作为唯一产品外壳；保留原版坏坏猫搜索界面/搜索流程、其他页面布局、净化、阅读器、CSS/排版和交互。Legado 只提供书源导入及完整书源解析运行能力，不搬 Legado UI、书架 UI 或 Reader UI。
- **不可降低的兼容标准：** 固定上游 SHA `330e606c4d57e08adc056ff8ab7a4132c784da9a` 的全部书源相关模型、规则、JS/Rhino/JsExtensions、HTTP、Cookie、登录、BaseSource 生命周期、搜索/发现、详情、目录、正文、多页/批量、缓存及实际依赖闭包。逐项符号/API 覆盖和差分测试是“完整”的定义；单条真实源 E2E 不能代替完整覆盖。
- **架构重新审视：** 当前 B′、Android-free `:integration-contract` 和 host projection 仅是实验路线，不享有优先于兼容性的地位。下一阶段必须审计现有生产实现与固定上游的逐符号差异，确认哪些真实上游源码/模块能直接复用，再比较独立 Android Runtime、受控上游核心模块和 Hybrid。禁止新增 shadow DTO、简化同名 API 或 no-op 来维持当前架构。
- **明确排除：** Legado UI/Reader UI、无书源依赖的 RSS/TTS/音视频产品功能；Badcat Reader 保留并消费引擎正文结果，继续负责净化与显示。
- **重新审视的实施顺序：** (1) 上游书源 API/依赖闭包清单；(2) Badcat 当前代码/调用方/测试替身逐项盘点；(3) 对每项标记真实复用、等价适配、缺失或错误语义；(4) 评估架构与数据所有权；(5) 先登记具体模块和测试，再改代码。不得先建 Room/schema v1 或做大规模搬运。
- **起始基线：** `integration/legado-upstream-runtime`，HEAD `5e79276badd050feaabb5c3002afa65b9938b0e8`。本注册只更新必读规则，不代表代码已达到新门槛。
- **既有 CI 证据（仅供准确记录）：** run `44707054980`，exact HEAD 同上，job 成功；日志显示 contract unit tests、bounded Rhino host tests 和 Debug APK assemble 均完成成功，APK artifact `badcat-legado-debug` ID `14615483023`，ZIP SHA-256 `f68f5e8eea5241e29a3cda0feaa230ac50d3abf3f687d57396a5dee0884ba802`。但真实源证据有限：Nyasama 搜索 3/5 返回可用结果；Pinshuzhai 的单源 Search→Detail→TOC→Content 通过；上传语料样本仅 1/4 完整跑通（3 个失败）。这不证明完整 Legado 兼容，也不证明 Badcat 真机/UI 验收。
- **本轮尚未修改运行时代码/Gradle/数据库/UI，也未冻结 schema v1；** 下一步是按上述顺序完成符号/API 和依赖闭包差异审计，再登记第一批实现变更。

---

## 2026-11-20 用户再次确认：交付目标必须是“完整的 Legado 书源兼容运行层”

**本条为不可降级的最终产品要求，优先级高于任何现有 PoC、模块边界、局部进度或实现成本。**

### 用户要求（原意明确记录）

- 交付物必须是**完整的 Legado 书源兼容运行层**，不是“部分兼容”、简化版、常见书源兼容版、selector-only 解析器或仅支持搜索/详情/目录/正文的演示实现。
- “完整”以固定上游 LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a 的全部书源相关运行语义为基准；必须真实复用上游实现及其完整必要依赖闭包，或以逐项差分测试证明完全等价。不得自行删减书源规则、JS API、HTTP/Cookie/登录行为、模型字段、执行生命周期或缓存语义。
- **坏坏猫仍是唯一产品 UI 和阅读器**：原版坏坏猫搜索界面/搜索流程、所有其他页面布局、净化系统、书架/收藏、书签、阅读器、CSS/排版与交互全部由坏坏猫保留。Legado 只提供完整书源导入、规则解析和运行能力，不带入 Legado UI、书架 UI 或 Reader UI。
- 不允许因“完整运行层”而擅自搬入与书源执行无关的 Legado 产品模块；但只要某个上游模型、服务、扩展或基础设施是完整书源能力的真实依赖，就必须纳入依赖闭包，不能以“非核心模块”为由排除。

### 完整运行层必须覆盖的能力

1. **书源模型与数据契约**：固定上游全部书源字段、嵌套规则模型、默认值、null/missing、未知字段、Gson 适配器、导入/导出和 round-trip 行为。
2. **规则解释器**：完整 AnalyzeRule、AnalyzeUrl、CSS/JSoup、XPath、JSONPath、正则、规则组合、变量、替换、嵌套规则、URL 模板、请求/响应规则和上下文语义。
3. **JavaScript ABI**：固定上游 Rhino 版本、全部书源相关 JsExtensions、静态/实例方法、重载签名、参数转换、返回值、异常、共享/隔离上下文、JS 库及源作用域变量。
4. **网络与会话**：完整 HTTP 请求选项、GET/POST 等方法、headers/body、编码、重定向、超时/取消、响应类型/字节、CookieJar、Cookie 同步、登录流程、会话和源隔离行为。
5. **书源生命周期**：真实 BaseSource 行为及其所有必要扩展/服务，包含初始化、登录、搜索、发现、分页、详情、目录、正文、多页正文、批量内容、替换、缓存保存/失效、异常恢复与清理。
6. **业务与辅助依赖**：只要被上述能力实际调用，相关模型、加解密/编码、HTML/网络工具、缓存、文件/存储、WebView/浏览器桥接和 Android 平台服务都必须纳入；逐项注明复用方式和隔离边界。
7. **Badcat 接口边界**：坏坏猫原有 UI 调用兼容门面；门面不得改变原有界面/交互，也不得用投影模型丢弃上游规则语义。阅读器只消费引擎输出及必要元数据，继续由坏坏猫执行净化、CSS、排版和阅读交互。

### 可行性审视与当前小跑结果（2026-11-20）

- **结论：目标可行，但现有实现/架构不能直接宣称达到目标；需要以真实上游源码闭包重构/重组 Runtime 集成边界。**
- 固定上游工程结构检查：settings.gradle 只 include :app、:modules:book、:modules:rhino；书源关键模型/服务并非集中在一个独立 SDK。BookSource、Book、BookChapter、BaseSource、JsExtensions、AnalyzeRule、AnalyzeUrl 分布在上游 app 源码，modules:rhino 只是 Rhino 模块，不能单独代表完整书源运行层。
- 当前 Badcat :integration-contract 是 Android-free Kotlin/JVM 边界；其中固定上游源码目前仅见少量 AnalyzeByJSoup/JSONPath/XPath 桥接文件，不能承载完整 Android/Room/Parcelable/Context/BaseSource 依赖闭包。
- 当前 SelectorLegadoRuleRuntime 源码明确标注为 bounded selector-only runtime，不是完整 AnalyzeRule；当前 SourceScriptRuntime 对尚未实现的源变量返回 UNSUPPORTED_RULE；UnsupportedSourceScriptRuntime 也明确 fail-closed。这些是诚实的局部实现，不满足最终交付要求。
- 当前 SourceRequestClient/host service 和投影模型属于适配边界，不能等同完整官方 HTTP、Cookie、登录及 BaseSource 生命周期。
- 历史 CI 小跑基线 5e79276badd050feaabb5c3002afa65b9938b0e8：integration-contract:test、:app:testDebugUnitTest 和 :app:assembleDebug 均成功；JUnit 报告为 290 项通过、0 失败。该证据只说明当前局部实现可构建/通过已有测试，不证明完整兼容。历史真实书源测试中 Pinshuzhai 单源 Search→Detail→TOC→Content 通过；上传语料 1/4 完整通过，另外样本有超时/404，需区分站点故障与引擎缺陷。
- 未执行完整上游源码闭包接入 PoC，未完成全符号差分，未证明所有 JsExtensions ABI/overloads、HTTP/Cookie/login 生命周期、全规则类型、缓存与批量正文语义等价；未进行当前最新文档 HEAD 的重新构建；未完成 Badcat 真机/UI 全面回归。因此不得标记“完整”或“完成”。

### 架构决定与后续实施门禁

1. 暂停继续扩展旧 SelectorLegadoRuleRuntime 作为主引擎的路线；它只能保留为明确标注的过渡/测试组件，不能作为完整运行层。
2. 先完成固定 SHA 书源相关**全部符号/API/overload/模型/资源/服务依赖闭包**清单，包含直接和传递依赖；每项记录上游路径、签名、caller、依赖、Badcat 接入点、实现来源、差异和测试。
3. 新建独立 Android Runtime 模块的最小真实源码 PoC：优先接入真实 BookSource → BaseSource → AnalyzeRule → AnalyzeUrl → Rhino/JsExtensions → HTTP/Cookie 依赖链。允许 Runtime 使用必要 Android 平台 API；不得为了保持 Android-free integration-contract 而削弱上游闭包。
4. 对比“受控上游核心源码模块”“独立 Android Runtime 模块”“Hybrid 平台服务适配”三条方案，选择能容纳完整闭包且不带入 Legado UI/Reader UI 的方案。任何方案都必须证明实际运行的是真实上游语义。
5. 完整 Runtime 通过后，才把坏坏猫原搜索入口和原阅读器接入兼容门面；先做原 UI/净化/CSS/阅读器回归，不重写坏坏猫产品层。
6. 完整验收必须包括：符号覆盖 100% 盘点；上游固定样本差分测试；模型序列化/规则/JS ABI/HTTP/Cookie/登录/生命周期/缓存/批量行为契约；脱敏录制回放；多类型可用真实书源 E2E；Android 集成测试；Badcat 原 UI/净化/阅读器真机回归。
7. 任何未覆盖能力必须列为阻断项。只允许报告“局部实现/部分兼容/PoC 通过”，不得将其表述为完整 Legado 兼容运行层。不得因用户希望一口气完成而跳过测试或把失败项改成成功。

**本次记录只更新报告，不修改运行时代码、Gradle、数据库、UI 或 APK；不代表完整运行层已实现。**

## 2026-11-20 Runtime-0.1 registration — isolated Android module / authentic upstream model-rule slice

- Scope: create a dedicated Android library module `:legado-runtime` without Legado UI/Reader. First compile gate is limited to the exact pinned upstream rule model classes and Gson contract as a dependency-closure probe; it is explicitly not a complete runtime and cannot be reported as such.
- Upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`, source roots under `app/src/main/java/io/legado/app/data/entities/rule` and `app/src/main/java/io/legado/app/utils/GsonExtensions.kt`. Preserve original package names and source contents; use upstream sources via the pinned submodule rather than shadow DTOs.
- Target module/configuration: add `:legado-runtime` as Android library with Kotlin Android + Parcelize, compile the authentic ExploreRule/SearchRule/BookInfoRule/TocRule/ContentRule/ReviewRule and their shared interfaces/Gson adapter. Do not add `BookSource`, `BaseSource`, `AnalyzeRule`, `AnalyzeUrl` or app UI classes to this first slice until their full dependency closure is registered.
- Tests: run `:legado-runtime:testDebugUnitTest` with upstream object/string dual-form fixtures and default/null/unknown-field round-trip checks. CI checkout must initialize the pinned submodule and verify its exact SHA before compiling.
- Exclusions for this slice: no production caller migration, no app UI changes, no database/Room schema, no reader changes, no replacement of current runtime. This is an authentic source/module feasibility gate only.
- Acceptance: exact upstream source files compile in the new Android module and executable tests exercise the actual classes. Failure must be recorded with missing dependency symbols; do not add same-name stubs or simplified replacements to force compilation.
- Status before implementation: registered only; no runtime code changed yet.