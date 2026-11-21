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
