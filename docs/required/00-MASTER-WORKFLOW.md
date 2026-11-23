# Badcat B′ 总流程必读清单（唯一总入口）

> 每次任务先读本文件，再读本次任务所属的一份分类清单。单类任务不需要重读其他四类；跨类任务只追加读取实际受影响的分类。历史审计文档是证据资料，不是例行必读清单。
> 分支：`integration/legado-upstream-runtime`（禁止写入 main）。Legado 固定行为基准：`330e606c4d57e08adc056ff8ab7a4132c784da9a`。

## 1. 总目标

交付独立运行的 Badcat 增强客户端：保留坏坏猫原版 UI、页面、导航、交互和五大类功能；在产品层后面实现固定 SHA 的完整 Legado 兼容运行时；不依赖另装 Legado App；不以 selector-only、空壳 API、no-op、假数据、只编译通过冒充完成。未经完整验收不称完整版本、不交付最终发布 APK。

### 1.1 产品边界与完整 Legado 书源兼容硬门槛（最高优先级）

- **产品主体始终是坏坏猫：** 原版坏坏猫的搜索入口/搜索页面、收藏夹、书架、导航、净化、书签、阅读器、阅读排版/CSS 和其他页面布局/交互均由 Badcat 保留并负责。不得用 Legado UI、Legado 书架或 Legado Reader 替换。
- **Legado 只作为书源运行能力来源，但能力必须完整：** 以本文件固定 SHA 的 Legado 上游为唯一行为基准，实现完整书源导入/导出、全部书源规则类型与执行语义、搜索/发现/分页、详情、目录、正文/多页正文、登录、Cookie/会话、HTTP/请求配置、Rhino/JsExtensions/JS 库、AnalyzeRule/AnalyzeUrl、变量/替换/上下文、缓存和 BaseSource 生命周期，以及这些链路实际依赖的全部上游模型、工具和服务。不能把“能导入 JSON”或“搜索→详情→目录→正文的一条样例成功”称为完整兼容。
- **“完整版一样”的精确定义：** 对固定 SHA 中所有书源相关公开能力、字段/默认值/null/序列化、规则语义、JS 扩展 API/overload、请求/会话行为和生命周期做逐项清点；每项必须有真实实现或经差分测试证明的等价适配。未实现项必须显式列为阻断项，不能降级、静默忽略或以占位 API 通过。
- **实现优先级：** 优先复用固定 SHA 的真实上游源码与依赖闭包；只允许薄适配层连接 Badcat，不允许用同名简化 API、shadow DTO、selector-only、no-op、假数据或仅能编译的替身冒充上游行为。若完整闭包不能在现有模块边界成立，必须重新比较独立 Android Runtime 模块/受控上游核心模块/Hybrid 方案，不得为了维持 B′ 而降低兼容性。
- **边界排除：** 不搬运 Legado 的整套 UI、书架产品流程和 Reader UI；不因“完整书源能力”而无依据地引入与书源执行无关的 RSS、TTS、音视频或其他产品页面。若某个上游组件是书源规则或生命周期的实际依赖，则必须纳入，而不是按目录名称随意排除。
- **验收定义：** 上游书源相关 API/符号覆盖清单 + 固定样本差分契约测试 + 脱敏回放 + 多类型真实书源 E2E + Android 集成测试；任何一项缺口都只能标记“部分实现/局部通过”，不能标记完整 Legado 兼容或发布就绪。

## 2. 当前正式架构方向：Runtime-3.0 受控上游核心移植

**当前唯一执行主线：坏坏猫原版产品层 + 固定 SHA Legado 真实书源执行核心 + 类型化宿主接口 + 独立验证的数据单一事实源。**

本节优先级高于本文件中保留的历史架构候选和历史任务记录。新任务必须以 Runtime-3.0 作为设计起点，不得让旧 B′、A、Hybrid 的既有边界或失败假设预先限定新方案。

### 2.1 产品与能力硬边界
- 坏坏猫是唯一产品主体。保留原版 UI、搜索、收藏/书架、小说、书签、导航、净化、Reader、CSS/排版和交互；绝不以 Legado UI/Reader 替换。
- Legado 固定 SHA 是书源执行行为基准，完整盘点并保留规则、模型、AnalyzeRule/AnalyzeUrl、Rhino/JsExtensions/jsLib、BaseSource 生命周期、HTTP、Cookie、登录、WebView/浏览器/验证码回调、搜索/发现/详情/目录/正文/分页/批量正文、缓存等真实依赖。
- 可以隔离 Android UI、数据库、网络、Cookie、登录、WebView、缓存和生命周期等平台边界，但必须使用类型化 Host Ports，保留上游输入输出、null/default、异常、线程/协程、取消、回调、状态、事务和资源释放语义。
- 禁止为方便编译而删除上游行为、no-op、空返回、假数据、shadow DTO、Map/Any? 万能桥接、同名简化 API，或将测试替身接入生产路径。
- 不搬运 Legado 产品 UI、书架流程或 Reader。若书源执行闭包确实依赖某组件，则按真实调用证据保留其行为或提供差分证明的宿主实现，不得按目录名称武断排除。
- 固定上游：`LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`。升级须独立登记并完整差分。

### 2.2 目标分层
1. **Badcat Product UI**：原版页面、导航、阅读器、净化、CSS、交互。
2. **Compatibility Facade**：集中处理类型、身份、序列化、错误和生命周期边界转换，不把适配逻辑散落在 UI。
3. **Controlled Legado Runtime**：尽可能原样复用固定 SHA 的真实上游源码及完整行为依赖。
4. **Typed Platform Host Ports**：Android Context、HTTP、Cookie、登录 UI、WebView/浏览器、缓存、数据库、线程/协程、资源生命周期。
5. **Single-source Data Ownership**：每个数据域一个事实源和写入者，事务、缓存失效、备份、迁移、回滚、重启恢复均可验证。数据库选型暂不预设。

### 2.3 数据策略
- Runtime-3.0 首先证明真实执行闭包和可构建边界；不先冻结 schema v1、不加 Room、不迁移用户数据、不双写。
- 审计 BookSource、Book、BookChapter、Progress、Chapter Cache 五域，并扩展 Cookie、登录态、源变量、收藏、书签、历史：字段、身份、owner、读者、写入者、事务、缓存 key、恢复和迁移。
- 旧 B′（Badcat-owned persistence）、限定 A、Hybrid、官方核心表+兼容门面等继续作为历史候选保留。只有基于新证据重新比较、重新登记并通过单写者/迁移/回滚 PoC 后才可启用。
- 不因旧 B′ 曾经优先就强迫 Runtime 塞进旧模块边界；也不因完整兼容的目标而直接搬入整个 Legado AppDatabase 或产品 UI。

### 2.4 实施阶段与门禁
| 阶段 | 目标 | 通过条件 |
|---|---|---|
| 0 架构总审查 | 固定 SHA 源码/声明/caller/依赖/Gradle/资源/反射/数据 owner 盘点 | 可追溯清单、调用图、依赖图、未知项和 GO/NO-GO |
| 1 Runtime 边界 PoC | 原样上游核心 + 类型化 Host Ports，排除 Legado 产品 UI/Reader | 可复现编译、架构守卫、真实规则/JS/请求闭环 |
| 2 模型与规则 | BookSource/Book/BookChapter/ReadConfig、规则、AnalyzeRule/AnalyzeUrl | 字段/序列化/规则固定 SHA 差分通过 |
| 3 JS 与生命周期 | Rhino、JsExtensions overload、BaseSource、jsLib、SharedJsScope | API/ABI/上下文/回调/取消/释放差分通过 |
| 4 网络与平台服务 | HTTP、Cookie、登录、WebView/浏览器/验证码、缓存、安全 | 真实服务语义和生命周期测试通过 |
| 5 书源业务链 | 搜索/发现/分页/详情/目录/正文/多页/批量正文 | 多类型书源全链路差分回放通过 |
| 6 数据所有权 PoC | 五域及关联状态单一事实源、事务、迁移、回滚、恢复 | 单写者、无丢失、重启/升级演练通过 |
| 7 Badcat 集成 | 接回原版 UI/Reader，不改变产品交互 | 原版 UI 回归、真机阅读和恢复通过 |
| 8 发布验收 | 契约/架构/回放/真实源/真机/APK/许可证 | exact HEAD 产物与所有关键门禁通过 |

### 2.5 历史方案保留规则
- 原 B′ 五域 PoC、限定 A、Hybrid、旧模块拆分、Runtime-1.x/2.x 的任务登记、CI、产物、失败记录完整保留，不删除、不改写其历史结论。
- 历史资料只用于追溯假设、失败原因和可复用证据，不自动支配 Runtime-3.0 的模块、数据或任务顺序。
- 重新启用历史候选必须新建决策记录，明确重启原因、仍适用的证据、变化的前提和新的可证伪验收标准。
- Runtime-2.11 嵌套 Shell 探针暂停并由 Runtime-3.0 架构总审查取代；原注册和历史记录保留，不代表执行或通过。


## 历史架构候选：方案 B′（保留原始论证，不再自动支配当前实施）

**Badcat 自有持久化 + Badcat 原有 UI + Legado 兼容运行时 + 严格隔离适配层 + 可退出的最小 PoC。** B′ 是此前阶段的首选验证路线；该历史结论保留供追溯，但自 Runtime-3.0 起不再是当前默认路线。只有在新架构决策中重新登记并通过门禁后才可恢复。

| 层 | 责任 | 禁止 |
|---|---|---|
| Badcat UI | 原版页面和交互 | 直接依赖官方 DAO |
| Badcat Repository/持久化 | Badcat 产品数据和经 PoC 确认的持久化事实源 | 同一数据域双写 |
| Compatibility Facade | 字段/身份/序列化/事务/缓存令牌/生命周期转换 | 适配逻辑泄漏到各处 |
| Legado Runtime | 固定 SHA 的官方模型语义和运行时 | 直接访问 Badcat DAO |

UI 是交互入口，不等于数据库事实源。每个数据域（BookSource、Book、BookChapter、进度、书签、缓存、Cookie、登录态、源变量）必须明确唯一事实源、唯一写入者、读投影、事务边界、缓存失效和重启恢复。禁止无规则双写。

### B′ 最小 PoC（五项）

只验证：BookSource、Book、BookChapter、阅读进度、章节缓存。必须提交：
- 逐字段映射：类型、默认值、null/缺失、规则 JSON 双形态、未知字段。
- 身份映射、去重/冲突、排序和缓存 key/version token。
- 单写多读、事务边界、缓存失效、重启恢复。
- 迁移备份、dry-run、幂等、失败回滚、旧版升级演练。
- 架构测试证明 Runtime 不引用 Badcat DAO、UI 不依赖官方 DAO。
- 脱敏录制回放、少量真实源搜索→详情→目录→正文→缓存、Android 真机验证。

**退出条件：** 适配层泄漏、双模型状态分裂、迁移不可回滚、缓存/身份语义无法一致、真实书源引擎链路失败（排除已证实的站点/网络故障）、真机数据无法恢复。触发后暂停扩大 B′，转限定数据层 A（只迁官方核心模型/DAO/必需存储，不搬 Reader UI、不引入无关 RSS/听书实体）；若仍不合适，再评估官方核心表 + 兼容门面、旧表只读保留。

## 3. 五大类入口

1. [第一类：Badcat UI](01-BADCAT-UI.md)
2. [第二类：阅读器](02-READER.md)
3. [第三类：Legado 运行时](03-LEGADO-RUNTIME.md)
4. [第四类：AdBlock/AdMark](04-ADBLOCK-ADMARK.md)
5. [第五类：PPRule](05-PP-RULE.md)

五类是产品工作线，不是 Legado 的五个内部模块。Legado 统一放第三类，不得用 Legado 完成度替代其他产品类。

## 0.1 全任务必读门禁（最高优先级，任何任务无例外）

- **开始任何工作之前，必须重新读取当前分支上的本总清单**，不得只依赖聊天记忆、历史摘要、上一次阅读结果或旧版本副本。
- 读取总清单后，必须判断本次任务涉及的分类，并在采取任何实质行动前读取对应的最新分类必读清单；跨类任务读取所有实际受影响的分类清单。
- 该门禁适用于所有工作，不因任务看起来简单而豁免：包括代码修改、测试/构建、CI 排查、APK/产物处理、GitHub 操作、文档整理/改写、架构调查、状态核验、故障排查、任务续做，以及用户只说“继续”的情况。
- 若任务不属于五大类，仍必须先读本总清单，再按任务性质读取相关规范/资料；若无法确定分类，先调查任务归属，但调查前仍须完成总清单阅读。
- **不得先操作、后补读清单。** 若当前无法读取总清单或相关分类清单，应暂停实质操作，说明阻断原因并请求恢复读取条件。
- 每次新任务/新一轮续做都重新执行上述步骤；同一轮内若任务范围发生变化或扩展，先补读新增分类清单，再继续。
- 完成清单阅读后，仍须执行第 4 节的分支/HEAD 核验、登记、源码审查、测试、差分审查和结果回写流程。本门禁不替代这些步骤。



## 0.2 连续任务接手与进度恢复（强制）

- **必读门禁优先于上下文恢复：** 每次收到新任务或“继续”，先重新读取当前分支的本总清单，再读取本次所属分类清单；不得先凭聊天记忆、摘要或上次结果开始操作。用户指定第三类时，必须读取本文件和 `03-LEGADO-RUNTIME.md`。
- **以仓库证据恢复进度：** 必读完成后，核对分支名、HEAD、最近提交和分支变化；从分类清单恢复所有 registered/in_progress 任务、各自的文件边界、依赖、实际结果、失败项和下一步，不要求用户重复提供已经记录的信息。
- **活动 CI 快照与并行任务规则：** CI 只读取其 exact SHA 的不可变快照；活动 CI 不再冻结整条分支。允许独立任务在同一分支形成后续提交并各自运行 CI。仅当文件路径不重叠、无共享可变状态、无未满足依赖且各自已 preregister 时才可并行；同文件/同依赖任务串行。分支 ref 更新必须用 expected SHA/CAS，不得 force-push、reset、rebase 或改写活动 CI 所依赖的历史。任何测试/CI 结果仅对其记录的 SHA 有效。
- **CI/产物必须按提交快照核实：** 每个 CI run 固定验证其触发时的 exact commit SHA；CI 运行期间允许在同一 integration 分支提交已独立登记、文件范围不冲突且无依赖冲突的其他任务。新提交产生新的 SHA/CI run，旧 run 只验证旧 SHA，绝不能替新提交背书。禁止 force-push、reset、rebase 改写活动 run 所依据的历史；分支更新必须使用 expected SHA/CAS，避免并发提交覆盖。对已终结 CI 检查 exact HEAD、job/step、失败日志、JUnit、报告和 APK/其他产物；不能只看 run 标题或“上传成功”推断验收通过。
- **状态不得混淆：** 分开记录已完成、局部通过、失败、待验证和阻断；失败测试不能记为成功，Debug APK 能构建不能代替测试验收，诊断 APK 不能当作交付 APK。不得重复执行已有明确通过证据的工作；若证据不足，先补核验。并行任务分别维护状态和 SHA，不得借用其他任务的通过证据。
- **按任务边界推进：** 每项任务必须独立登记唯一 ID、目标、允许文件、依赖、验收和排除项。文件范围重叠、共享可变状态或存在前置依赖的任务必须串行；文件与依赖边界完全隔离的任务可以并行登记、实施和验证。并行任务不得写同一文件；分类清单本身由单一提交顺序更新，避免互相覆盖。不得把其他分类的问题顺手混入当前类别。
- **完成后交接：** 把实际改动、提交 SHA、测试命令和精确结果、CI run/job/exact HEAD、产物 ID/哈希、设备结果、遗留问题和下一步写回对应分类清单；总清单只补充跨类规则或总状态，不复制技术流水。下一轮必须以最新仓库内容重新恢复，不以聊天中上一条回复代替。

## 4. 每次任务流程（强制）

1. 确认当前分支和起始 HEAD；只在 integration 分支工作。
2. 阅读本文件 + 一个相关分类清单；跨类才读额外类别。
3. 改代码前在分类清单登记：范围、目标文件/模块、上游 SHA、调用方、事实源/事务、风险、契约/回归测试、验收和排除项。
4. 审查实际源码、所有 caller、适配器、测试替身和旧实现；登记不完整先修清单。
5. 只实施已登记范围，不做占位、不顺手混类。
6. 跑 focused tests，再跑可行的完整测试/构建；检查真实 CI、JUnit、APK，不以发起任务或编译通过代替验收。
7. 审查 diff、依赖边界、数据安全和原功能回归。
8. 将实际变更、测试结果、CI run + exact HEAD、产物哈希、真源/设备结果、未完成项和清单自审写回分类清单。
9. 总清单只记录架构决策、跨类门禁和总状态，不重复粘贴技术流水。

登记模板：日期/任务；分类；起始 HEAD；目标与排除；上游来源/SHA；目标文件和全部 callers；事实源/写入者/事务；兼容与安全风险；契约/回归测试；验收/退出条件。

结果模板：实际变更；测试命令与精确结果；CI run/commit/status；APK/报告哈希；真实源/设备结果；遗留问题；是否触发退出条件；清单自审。

## 5. 自动维护十项强制制度

1. **契约测试优先：** 字段、默认值、null/缺失、序列化 round-trip、规则 JSON 双形态、缓存 token、Cookie/JS 生命周期自动化；文档不能代替测试。
2. **架构测试守边界：** Gradle 模块依赖、编译期接口隔离、静态分析/自定义 lint；禁止 Runtime→Badcat DAO、UI→官方 DAO，违规 CI fail。
3. **上游差异自动生成：** 自动比较候选 SHA 的 API/overloads、模型字段/默认值、Room entities/index/converter/migrations、JsExtensions 和依赖；生成报告，不自动升级。
4. **书源测试分层：** 脱敏录制回放为主，少量真实源联网冒烟为辅；区分网站故障与引擎缺陷。
5. **UI 回归分层：** 关键页面截图对比 + Espresso 冒烟 + 人工清单，不承诺所有 UI 全自动。
6. **机器可读版本清单：** JSON/YAML 由 CI 注入真实源码 commit、Legado SHA、adapter 版本、DB schema、测试报告哈希和构建信息，并进入 APK/CI 产物。
7. **迁移自动演练：** 备份、dry-run、幂等、失败回滚、旧版升级、重启恢复进 CI 或定期任务。
8. **安全默认开启：** HTTP timeout/cancel/redirect policy、JS 执行时间/资源/隔离、缓存容量/TTL/文件上限默认开启并有硬上限。
9. **明确维护责任：** 当前项目主维护者统筹；后续拆分 UI、Runtime、Adapter/Data、CI/Release 责任；任务要有负责人和结果。
10. **稳定优先：** 仅安全修复、关键兼容、明确功能/性能/稳定收益才升级；上游发版本身不是理由。

节奏：每次提交跑契约/架构/单元/模块测试；每月自动产出上游差异报告并人工评估；每次 pin 升级完整回归；每次 DB schema 升级自动迁移演练；每个正式 APK 附 manifest、报告、许可证和哈希。

## 6. 状态定义

未开始 / 调查中 / 部分实现 / 局部测试通过 / PoC 通过 / 分类已验收 / 发布就绪。CI pending、文档提交、Debug APK 可构建都不能升级状态。

## 7. 当前总状态（2026-11-20）

- 第一类 UI：部分实现，逐页视觉/交互 parity 与真机未验收。
- 第二类阅读器：开发版阅读/历史/书签/进度/CSS 有基础，原版阅读器和 CSS 兼容未验收。
- 第三类 Legado：多项局部运行时增量和真实源历史证据；B′ PoC 未验收，完整上游模型/服务闭包未完成。
- 第四类 AdBlock/AdMark：本地模型/存储、开关、注入入口和 matcher 有部分实现；DOM 删除闭环未完成。
- 第五类 PPRule：protobuf decoder 和有限 helper 有部分实现；完整 dispatch/JS/pagination/UI 未完成。
- 正式 Release、真机全面验收：未完成。


## 2026-11-20 清单体系建立记录

- 在 `integration/legado-upstream-runtime` 上建立总流程清单和五份分类必读清单，并更新 AGENTS 的必读导航。
- 将原有 Legado 需求/迁移/模型/JS 扩展审计文档、项目完成记录和五类总表标记为按需查阅的历史证据，避免每次重复阅读。
- 本次仅调整文档，没有修改运行时代码、测试或 APK。
- 本次整理的起始代码基线为 `11549e7824683553c4039befa2f714fabfb805c3`；最终文档 HEAD 以当前分支引用为准。文档提交没有对应的 CI run/status 证据，不宣称 CI 通过。


## 2026-11-20 B′ PoC-0.3 architecture registration

- Third-category report: `docs/required/03-LEGADO-RUNTIME.md`, section “PoC-0.3 registration — persistence ownership and rollback architecture decision”.
- Candidate for the next PoC implementation: Badcat-owned Room metadata database as the sole writer for BookSource, Book, BookChapter, reading progress and chapter-cache metadata; immutable generation-addressed chapter body files; one compatibility facade; preserve Badcat UI. This is a candidate, not a passed PoC.
- Do not import the entire upstream AppDatabase v113/26-entity product schema. Do not begin schema/runtime code until the exact entity/converter matrix, legacy backup/dry-run/rollback and all old readers/writers are registered.
- Corrected PoC-0.2 wording: upstream BookChapter PK `(url,bookUrl)` prohibits duplicate URL rows for one book; unique index `(bookUrl,index)` separately protects chapter order. URL-only host TOC dedup still needs behavior comparison, but must not be described as violating the upstream schema by itself.
- No runtime code or tests changed; B′ remains unpassed. This entry records the architecture gate and ordered next tasks only.


## 2026-11-20 B′ PoC-0.4 schema-contract audit

- Category-3 detailed report: `docs/required/03-LEGADO-RUNTIME.md`, section “PoC-0.4 audit findings — pinned schema and current host ownership”.
- Inspected pinned BookSource/Book/BookChapter declarations and rule model contracts at `330e606c4d57e08adc056ff8ab7a4132c784da9a`; recorded current Badcat source/progress/favorite/bookmark/history/TOC/cache ownership and a draft Badcat-owned Room schema.
- Key contract: BookSource PK `bookSourceUrl`; Book PK `bookUrl` plus upstream unique `(name,author)`; BookChapter PK `(url,bookUrl)`, unique `(bookUrl,index)`, cascade FK to Book. The B′ host schema draft scopes book/chapter keys by `sourceId` as an explicit host extension.
- No runtime code, Gradle, tests, database, migration or UI changed. Caller inventory is not yet exhaustive; Room schema and rollback are not implemented; B′ remains unpassed.
- Next: exhaustive caller/access scan → verify Gson/ReadConfig converters and defaults → register schema-only Room implementation separately → implement migration dry-run/backup/rollback before any cutover.


## 2026-11-20 B′ PoC-0.5 persistence ownership audit result

- Detailed category-3 report: `docs/required/03-LEGADO-RUNTIME.md`, section “PoC-0.5 audit results — current-tree caller scan and converter/schema gate”.
- Complete Git tree at the preregistered source head had 437 entries and was not truncated. Direct persistence owners and production wiring were identified: FileBookSourceCatalog/SourceCatalogActivity/BadcatReadingEngineFactory; BadcatReadingStateRepository and SourceBookActivity/BookmarkActivity/ReadingHistoryActivity/FavoritesActivity; Activity-memory TOC via SourceTocService; ChapterContentCache/FileChapterContentCache/SourceChapterContentService and engine factory injection.
- Confirmed separate stores still exist (source AtomicFile, progress/bookmark SharedPreferences, history SharedPreferences, ephemeral TOC, chapter AtomicFile cache), and app Gradle has no Room runtime/compiler/schema infrastructure. Adding Room before migration and reader/writer cutover would create a second source of truth.
- No runtime code, tests, Gradle or UI changed. Converter parity for all nested ReadConfig/default/null cases and executable migration/rollback remain open. B′ remains unpassed.
- Next order: pinned Gson fixtures → freeze schema v1 + constraints → separately preregister schema-only Room implementation → migration backup/dry-run/idempotency/rollback/restart tests → staged single-writer cutover.

## 2026-11-20 PoC-0.6 Gson contract checkpoint

- Registered the pinned Gson/rule fixture scope before test changes (11f9abdfc76f9298087fb27456ac36892aea28ea).
- Added actual-host contract tests in app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt (d634aa7be41f4407fa10e09eca3476a2af687b38).
- Direct normalized comparison against pinned Legado 330e606c4d57e08adc056ff8ab7a4132c784da9a found exact source matches for GsonExtensions and all six rule classes.
- Tests are NOT executed: workflow lookup returned no runs and no local Gradle runner was available through the repository connector. Do not claim tests/build/CI passed.
- Host lacks the actual pinned Book/BookSource/BookChapter model files; ReadConfig/ParagraphAnchor and complete model field/default/null round-trip cannot be tested without introducing forbidden shadow DTOs. Schema v1 freeze and Room implementation remain blocked.
- Next: integrate the authentic pinned model contract/dependencies, add ReadConfig/ParagraphAnchor + full model round-trip fixtures, execute focused and full CI, then freeze schema v1 and register schema-only Room code. B′ remains unpassed.

## 2026-11-20 PoC-0.7 ordered gate update

- Third-category detailed registration/result: `docs/required/03-LEGADO-RUNTIME.md`, “PoC-0.7 registration — authentic upstream model source-closure audit”.
- At exact pinned SHA `330e606c4d57e08adc056ff8ab7a4132c784da9a`, BookSource/Book/BookChapter are coupled to Android Parcelable/Room and upstream base contracts; Book additionally reaches app database and reading helpers. They cannot be copied blindly into the Android-free integration-contract module.
- Next ordered gate is a dependency-closure and caller audit to find the smallest faithful integration seam, with no shadow DTOs. Schema v1 freeze and Room remain blocked until authentic model serialization and executable fixtures pass.
- No code/tests/Gradle/UI/APK changed in this registration. Current HEAD has no reported CI statuses or workflow runs. B′ remains unpassed.


## 2026-11-20 Third-category PoC-0.7 source-closure checkpoint

- Category-3 checklist records the pinned source findings: BookSource requires Android/Room/Parcelable, BaseSource and rule/Gson contracts; Book additionally depends on app DB, BookHelp/ContentProcessor/AppConfig and reader helpers; BaseSource is a live login/Cookie/cache/Rhino lifecycle service, not a marker interface.
- Verbatim import into Android-free `:integration-contract` is rejected. Exact BookChapter closure, full host caller inventory and a minimal compile-ready authentic-model module boundary remain open; no schema v1 or Room work is authorized yet.
- This was a documentation/evidence checkpoint only; no runtime, tests, Gradle or UI changed. Prior exact baseline `981d064b50de1fcb68fc91213085b057a8561b13` had no reported status checks or workflow runs. B′ remains unpassed.
- Next: complete exact-path dependency and host caller inventory, then preregister a minimal authentic-model integration and executable serialization fixtures before proceeding to schema design.


## 2026-11-20 Third-category PoC-0.7 dependency closure inventory

- Completed a source/build graph inventory against Badcat's full non-truncated Git tree and pinned Legado source `330e606c4d57e08adc056ff8ab7a4132c784da9a`. The category-3 report records exact source paths, declarations, direct dependency families, host ownership paths and the remaining caller-scan gap.
- Authentic `BookSource` depends on `BaseSource` login/Cookie/cache/Rhino lifecycle; `Book` depends on app DB, BookHelp/ContentProcessor/AppConfig/ReadBook; `BookChapter` depends on app DB, AnalyzeUrl/RuleDataInterface, large-variable storage and Android app context. These are not isolated DTOs.
- Host `:integration-contract` is Kotlin/JVM and deliberately Android-free; its current engine/services/catalog use host projections. A verbatim import of the three upstream entities into that module is rejected. A dedicated Android source boundary is not yet shown to be isolated or buildable without pulling broad app services.
- PoC-0.7 remains not accepted; B′ remains unpassed. The next ordered action is a symbol-level host callsite scan and explicit B′ exit-option comparison before any model integration, schema v1 or Room work.
- This was a documentation/evidence inventory only: no runtime, Gradle, UI or tests changed, and no test/build/CI/APK pass is claimed.


## 2026-11-20 Category-3 caller scan and architecture comparison

- Verified host production call paths: `ReadingEngine` host projections → `SourceReadingEngine` → search/detail/TOC/content services → `SourceBookActivity`; progress/bookmarks/history through `BadcatReadingStateRepository`; source import/export through `FileBookSourceCatalog`/raw JSON codec; body cache through `FileChapterContentCache`; favorites through separate JSON owner.
- Confirmed semantic gaps: chapter content is currently loaded from URL and synthesized with index 0; host progress stores URL/scrollY rather than upstream ReadConfig/ParagraphAnchor; cache keys use sourceId/bookUrl/chapterUrl and lack upstream content-save fencing; persistence domains have separate owners and no single transaction.
- Category-3 checklist compares continued B′, constrained A, and hybrid official model/DAO ownership behind a Badcat façade. B′ is not passed; A is only a constrained fallback, not full compatibility; hybrid is the fidelity target but blocked by broad upstream app/DB closure and migration/rollback proof.
- Next gate is a five-domain field/identity mapping matrix plus exact upstream batch/cache/progress contract inspection, then a separately preregistered executable PoC. No schema v1, Room migration, broad source copy or UI change authorized yet.
- Documentation-only; no tests/build/CI/APK pass claimed.


## 2026-11-20 Category-3 batch/cache/progress contracts

- Inspected pinned `BatchContentContext`, `BookHelp.ContentSaveFence`, `CacheManager`, `ReadBookConfig` and upstream regression tests. Upstream batch identity is `(bookUrl,chapterIndex)`, URL-only matching must reject ambiguity, and stale asynchronous saves are fenced by per-chapter version tokens.
- Current Badcat UI/engine drops chapter index at content load (URL is passed and a fallback index 0 chapter is synthesized); current body cache is keyed by sourceId/bookUrl/chapterUrl and has no equivalent save token. Generic CacheManager TTL/LRU is a separate domain from chapter-body cache.
- Category-3 checklist now contains an initial five-domain mapping for BookSource, Book, BookChapter, reading progress and chapter body/cache, with acceptance assertions and an explicit note that it is not a frozen schema.
- Next candidate is an Android-free chapter-identity contract PoC carrying index through engine/service/UI while preserving URL navigation. It requires separate exact-scope preregistration and differential tests; no Room or schema changes authorized.
- Inspection/documentation only; no tests/build/CI/APK pass claimed.


## 2026-11-20 PoC-0.8 checkpoint

- Third-category chapter-index identity PoC is partially implemented on the integration branch: typed chapter-object content API, ambiguity-safe URL resolver, and index propagation through the primary TOC/bookmark/resume reader path. Detailed file/caller/test evidence and remaining legacy URL callers are recorded in `03-LEGADO-RUNTIME.md`.
- PoC-0.8 is not accepted; tests/build/CI are not verified (latest checked HEAD had no reported status checks or workflow runs). No APK or device validation.
- B′ remains unpassed; no schema v1, Room migration, cache fencing, or architecture switch is authorized by this checkpoint.


## 2026-11-20 PoC-0.9 checkpoint

- Legacy history/bookmark URL-only reader launches now resolve against the fresh TOC and proceed only on a unique match; stale/ambiguous identities fail closed without fetching a guessed chapter. Existing stored URL keys remain unchanged.
- PoC-0.8/0.9 implementation remains partial and unaccepted; current HEAD has no reported status checks or workflow runs, and no test/build/APK/device validation. Detailed evidence and next gates are in the third-category checklist.


## 2026-11-20 PoC-1.0 checkpoint

- Chapter-body cache now has a required typed `(sourceId, bookUrl, chapterIndex)` contract and in-process version fence; production AtomicFile cache separates same-URL chapters, checks URL metadata, and rejects stale asynchronous publication after invalidation. Legacy URL-keyed methods remain compatibility-only.
- PoC-1.0 is partial/not accepted: no test/build/CI/APK/device evidence, no direct replace/delete product operation, and no full upstream mutation lifecycle parity. B′ remains unpassed.


## 2026-11-20 PoC-1.0 regression fixture update

- Added JVM service coverage for same-URL chapters remaining separate by index and stale response publication being fenced. The required cache interface now forces implementations to provide typed/fenced operations. No tests were executed; CI remains unverified and PoC-1.0 is not accepted.

## 2026-11-20 用户最终确认：必须交付完整 Legado 书源兼容运行层

- 用户明确要求交付物必须是**完整的 Legado 书源兼容运行层**，不得以部分兼容、selector-only、常见规则子集或单条 Search→Detail→TOC→Content 演示链代替。详细能力范围、可行性审视、小跑证据和实施门禁以第三类必读清单 `03-LEGADO-RUNTIME.md` 最新章节为准。
- 坏坏猫仍是唯一产品外壳：保留原版搜索界面/流程、其他页面、净化、书架/收藏、书签、阅读器、CSS/排版和交互；只接入完整 Legado 书源导入与运行能力，不引入 Legado UI/Reader UI。
- 完整性必须以固定上游 SHA 的全部书源相关真实源码、API/overload、模型、规则、JS ABI、HTTP/Cookie/登录、BaseSource 生命周期及必要依赖闭包为准；逐项差分验收。未覆盖能力一律阻断完整兼容结论。
- 当前旧 selector-only/host projection 方案不满足最终交付门槛。优先验证独立 Android Runtime 模块/受控上游核心源码/Hybrid 方案；不得为了维持 Android-free contract 或 B′ 而削弱上游语义。此为最高优先级架构门禁。
- 本条为需求与流程记录，不代表完整 Runtime 已实现或验收；代码、构建、真机和 UI 状态以第三类报告的证据为准。

## 2026-11-20 Runtime-0.3 execution note

- Dedicated Android `:legado-runtime` currently passes its first authentic upstream rule-model/Gson contract gate (see `03-LEGADO-RUNTIME.md`); this is not full source-runtime compatibility.
- Added repeatable pinned Legado JavaScript extension API inventory and CI artifact generation. The next step is to consume that generated gap list and build the full upstream service/dependency closure. Do not route production search/reader callers or claim complete compatibility yet.

## 2026-11-20 Runtime-0.4 JS ABI and host-service audit

- Third-category report now contains the generated Kotlin API gap inventory and the source/caller audit for missing JS methods, browser/WebView/verification callbacks, legacy login JS and reader/theme configuration bridges. Current host projections and Login UI v2 do not implement those upstream contracts; do not patch placeholder methods or migrate production callers before the real upstream dependency-closure architecture is selected.
- Runtime-0.3 tooling correction and exact CI evidence are recorded in `03-LEGADO-RUNTIME.md`; its successful Debug build and 293 passing tests are local regression evidence only, not full Legado compatibility or B′ acceptance.

## 持续改进：任务结果闭环执行机制（所有类别、所有任务强制适用）

本节规定后续实际执行方法。目标是让每项工作从开始、执行、验证、记录到复核形成一个不可跳过的闭环；分类清单是项目状态的唯一工作记录，聊天内容和模型记忆不得代替它。

### A. 开始前建立本轮工作上下文
1. 重新读取当前分支的本总清单，再读取本轮所属分类清单；核验分支名和最新 HEAD。
2. 从分类清单确认上一项工作的最后状态、未完成事项、阻断条件和下一项已登记任务；不得凭聊天摘要猜测项目进度。
3. 若任务存在文件/依赖冲突，或下一项依赖其结果，先恢复该任务并取得结果、回写复核后再继续。若新任务与所有在途任务文件不重叠且无依赖冲突，允许在独立 preregister 后并行推进；不得因此跳过任何任务自身的结果闭环。
4. 本轮目标、允许修改范围或验收条件有变化时，先更新登记并复核，再开始实施。

### B. 执行中维护明确状态
1. 每项任务开始时，在所属分类清单登记唯一任务标题/编号、目标、范围、起始 HEAD、验证方法和验收条件。
2. 将工作状态明确区分为 registered、in_progress、blocked、completed；不得把“已发起”“等待 CI”“步骤尚未执行”写成完成。
3. 对 CI、测试、构建、打包、调查等异步工作，保存可追踪的 run/job/任务标识和 exact HEAD；只要仍在运行，就保持 pending/in_progress，并在同一任务内继续轮询和检查日志。
4. 若出现新失败、范围变化、证据冲突或额外修改需求，暂停扩展操作，先查明影响并登记必要的范围调整；不把未经登记的修复混入当前任务。

### C. 结果先回写，再开始下一项（硬停止门）
1. 一项工作产生可判定结果或明确阻断后，立即在所属分类清单记录：实际变更/未变更、测试命令及精确结果、CI run/job/ID/exact HEAD/conclusion、关键日志证据、产物及哈希（如有）、失败根因和影响范围、验收结论、遗留事项。
2. 若结果尚未产生，记录当前 pending/in_progress 状态、已完成步骤、尚未完成步骤和下一次检查目标；不得提前写通过或失败结论。
3. **串行任务的回写硬门：** 同一文件、同一根因或存在依赖关系的下一项实质工作，必须等前项结果回写并复核后开始。与在途任务文件范围完全隔离、无共享可变状态且无依赖冲突的任务，可在单独 preregister 后并行开展；并行不代表可跳过各自的结果记录。
4. 每项任务回写后必须重新读取刚更新的分类清单，逐项核对任务登记、真实证据、结论和遗留事项；确认记录与结果一致、没有遗漏、没有把 pending 当成通过，才解除该任务的串行依赖门。其他无冲突任务可以继续。
5. 若某项任务无法获取真实结果、无法写回或无法完成复核，只暂停该任务及其依赖任务；其他隔离任务可继续。不得用口头总结、历史聊天或推测替代。

### D. 每轮结束时做一次交接
1. 在分类清单明确写出当前最终状态、证据位置、尚未完成事项，以及唯一明确的下一步建议。
2. 下一轮开始时重新读取清单并从该状态继续，不重复已完成工作，也不跳过未完成的验证。
3. 只有分类清单已完成回写并复核，才允许结束本轮并转入其他任务。
4. 总清单只维护跨类别通用流程和架构门禁；具体技术证据、任务流水和结果始终写入所属分类清单，避免重复维护和状态分叉。

**执行原则：先确认状态，再开展工作；先取得证据，再下结论；先回写并复核，再推进下一项。任何任务都不得绕过此闭环。**

## 2026-11-24 CI 3300–3349 全量核查登记

- CI 全量账本：`docs/required/CI-3300-3349-AUDIT.md`。
- 已核对 3300–3349 共 50 次 Actions：43 success、5 failure、2 in_progress（补充轮询后快照；3346、3347 已成功，3348–3349 仍运行；审计写回触发的 3350–3352 另行追踪）。
- 五次失败已按日志归类：3315/3343/3345 为 Kotlin Parcelize 2.4.20 插件工件无法解析；3317/3319 为 Robolectric nativeruntime-dist-compat 1.0.16 转换失败，并伴随 Emulator/ADB 启动问题。
- 3348–3349 在补充轮询时仍运行中，必须继续轮询并回写最终结论，不得提前标记成功。
- CI 成功只代表对应 SHA 的工作流结论，不代表完整 Legado 兼容验收；Runtime-3.0 已知阻断仍按第三类清单和 Stage-0 审计执行。
- 本轮仅登记 CI 证据，无应用源码、测试源码、Gradle 或 UI 变更。

