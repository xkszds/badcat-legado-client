# 第三类 Legado Runtime 总进度索引（唯一入口）

> 每次第三类任务必须先读 `docs/required/00-MASTER-WORKFLOW.md`，再读本索引；之后按任务读取本索引指向的技术要求/历史主文档及对应 CI 分册。此文件是第三类总进度、要求摘要和证据位置索引，不替代详细技术设计、逐 CI 证据或原始日志。

- 仓库：`xkszds/badcat-legado-client`
- 工作分支：`integration/legado-upstream-runtime`（禁止写入 main）
- 固定 Legado 行为基准：`LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- 第三类原始技术主文档/历史资料：[`03-LEGADO-RUNTIME.md`](./03-LEGADO-RUNTIME.md)
- CI 分册目录：本索引第 6 节
- 当前分册容量规则：每册严格覆盖连续 200 个 GitHub Actions workflow run 编号；例如 #3200–#3399。超过边界必须新建下一册，不把两个区间塞进同一文件。
- 编号定义：以 GitHub Actions workflow run 的显示编号为准；Run ID 是独立唯一标识，Attempt 也要区分。不能把 GitHub run 编号、Run ID、Job ID 混用。

## 1. 第三类总目标

交付完整 Legado 书源兼容运行层，同时保持坏坏猫是唯一产品外壳。Legado 提供真实书源执行能力，不替换坏坏猫原版 UI、搜索入口、收藏/书架、小说、书签、阅读器、CSS/排版和交互；不得引入 Legado UI/Reader UI。

完整兼容必须以固定 SHA 的全部书源相关真实源码和依赖闭包为行为基准。不能以 JSON 导入成功、selector-only、host projection、空壳 API、no-op、假数据、单一搜索→详情→目录→正文样例或 Debug APK 编译通过代替完整实现。

## 2. 必须覆盖的具体要求

### 2.1 真实上游源码与模型
- 逐项盘点固定 SHA 的书源相关公开 API、模型、字段、默认值、null/缺失语义、序列化、异常和生命周期。
- 优先复用上游真实源码与完整依赖闭包；适配层只能连接宿主，不得简化上游语义。
- 对 BookSource、Book、BookChapter、ReadConfig、规则数据、缓存身份、Cookie/登录态、源变量等逐字段核对身份、owner、读写者、事务、恢复和迁移。
- 未纳入或无法证明等价的上游能力必须列为阻断项，不能静默忽略。

### 2.2 规则、解析与 JS
- 覆盖全部书源规则类型和执行语义，包括 AnalyzeRule、AnalyzeUrl、变量、替换、上下文、规则链与实际依赖的工具。
- 覆盖 Rhino、JsExtensions 全部方法/overload、jsLib、SharedJsScope、JS ABI、上下文、线程/协程、取消、回调和资源释放。
- 禁止同名简化 API、shadow DTO、万能 Map/Any? 桥接、selector-only 或测试替身进入生产路径。

### 2.3 网络、会话与平台宿主
- 覆盖 HTTP 请求配置、headers、timeout、redirect、Cookie、登录、WebView/浏览器、验证码/用户交互回调、缓存和 BaseSource 生命周期。
- 使用类型化 Host Ports 隔离 Android 平台边界；保留上游输入输出、默认值、异常、线程、取消、回调和资源释放语义。
- 安全限制必须有明确默认值、硬上限和自动化验证。

### 2.4 书源业务链
- 真实实现并验证导入/导出、搜索、发现、分页、详情、目录、正文、多页正文、批量正文、登录与 Cookie/session。
- 测试必须覆盖多类型书源、脱敏回放、差分契约和真实联网场景；区分站点/网络故障与引擎缺陷。
- 外部 Python urllib/HTML 冒烟、GET 可访问性、selector 自测和规则解析探针均不得冒充 Android Badcat + Legado Runtime E2E。

### 2.5 数据单一事实源与 Badcat 集成
- 明确每个数据域唯一事实源、唯一写入者、读投影、事务、缓存失效、备份、迁移、回滚和重启恢复。
- 不得无证据冻结 schema v1、引入 Room、双写或迁移用户数据。
- 保持 Badcat 原版产品 UI/Reader 与交互；Runtime 通过兼容门面接入，不搬运 Legado 产品 UI。

### 2.6 验收门禁
- 上游书源相关 API/符号覆盖清单。
- 固定 SHA 样本差分契约测试（字段、规则、JS ABI、请求/会话、生命周期）。
- 脱敏录制回放与多类型真实书源 E2E。
- Android 集成/仪器测试、原版 UI 回归、真机验证。
- exact HEAD 对应 CI、JUnit/报告、APK/DEX/产物哈希和许可证。
- 任何缺口只能标记部分实现/局部通过/待验证；不能标记完整 Legado 兼容、分类验收或发布就绪。

## 3. 当前正式架构路线

**Runtime-3.0：坏坏猫原版产品层 + 固定 SHA Legado 真实书源执行核心 + 类型化宿主接口 + 独立验证的数据单一事实源。**

阶段门禁：
0. 架构总审查：固定 SHA 源码、声明、caller、依赖、Gradle、资源、反射、数据 owner。
1. Runtime 边界 PoC：原样上游核心、类型化 Host Ports、架构守卫、真实规则/JS/请求闭环。
2. 模型与规则：字段/序列化/规则固定 SHA 差分。
3. JS 与生命周期：Rhino、JsExtensions overload、BaseSource、jsLib、SharedJsScope。
4. 网络与平台服务：HTTP、Cookie、登录、WebView/浏览器/验证码、缓存、安全。
5. 书源业务链：搜索/发现/分页/详情/目录/正文/多页/批量正文。
6. 数据所有权 PoC：五域及关联状态单一事实源、事务、迁移、回滚、恢复。
7. Badcat 集成：原版 UI/Reader 回归、真机阅读和恢复。
8. 发布验收：契约/架构/回放/真实源/真机/APK/许可证。

## 4. 当前总体状态（以最近已核实的资料为准）

- 第三类整体：**未验收**；完整 Legado 书源兼容尚未成立。
- Runtime-3.0：当前执行主线；Stage-0 架构总审查和 Stage-1 Runtime PoC 的 GO 门禁仍未证明全部通过。
- B′ PoC：未通过；不得因历史局部增量宣称验收。
- 数据 schema v1 / Room 迁移：未获授权，不得先行冻结或实施。
- 真实 Runtime E2E：当前可见 CI 3300–3349 的 Python 网络脚本不启动 Badcat Android App/Legado Runtime，因此不构成真实 Runtime E2E。
- 2026-11-24 已审计 CI #3300–#3349：50 次，45 workflow success、5 failure、0 in_progress；50/50 有 exact SHA、Job/Step、日志及覆盖画像。#3323 workflow success 但 Bixiange Python 冒烟 HTTP 503；#3315/#3343/#3345 为 Parcelize 插件解析失败；#3317/#3319 为 Maven Central HTTP 429，App 单测失败，但后续 API 28/35 仪器测试各 5 项、JVM ABI、Debug APK、DEX 检查成功。
- #3279–#3300：旧第三类文档记录称 22 次 Android debug build success，已写入旧文档第 24 节；#3300 与新审计区间重叠，必须按 Run ID/Attempt 对账，不得重复计数。
- 原 `03-LEGADO-RUNTIME.md` 当前过大，GitHub 文件读取器返回空内容，故本轮未完成其正文迁移或逐行复核。其原文件必须保留；不可声称已完成全量拆分。

## 5. CI 记录强制标准（每个 Run 单独完整填写）

每个 CI 必须独立记录以下全部字段，不允许仅用汇总表或共享 Profile 替代逐 Run 条目：

1. 身份与快照：CI 编号、Workflow/事件、Run ID、Attempt、分支、exact SHA、触发时间、conclusion。
2. Job/Step 矩阵：逐项名称、状态、日志/报告证据；清楚标出跳过/未执行。
3. 功能覆盖映射：每个成功执行步骤实际验证的模块、API、行为、测试类型、断言和覆盖边界。
4. 失败与跳过：失败点、日志位置、根因类别、下游未验证项；未知根因写待确认。
5. 真实书源/E2E：源名称/类型、环境、请求与链路环节；Python 脚本/探测不得写成 Runtime E2E。
6. 分层结论：Workflow、Job/Step、测试覆盖、真实功能、产品验收分别判定。
7. 证据与复核：日志、JUnit、报告、产物路径/链接、Job ID、核查日期、复核状态。
8. 失败和部分结果不得抹去；同一编号不同 attempt 分开记录；CI 结果只对 exact SHA 有效。
9. 文档写回触发的 bookkeeping CI 单独标记，不计入应用功能 CI 统计。

每条记录完成后重新读取所在分册复核字段、证据和结论，再更新本索引中的分册统计。

## 6. CI 分册位置与覆盖区间

| 分册 | GitHub Actions Run 编号 | 文件位置 | 当前状态 |
|---|---|---|---|
| 01 | #3200–#3399 | [03-LEGADO-CI-3200-3399.md](./03-LEGADO-CI-3200-3399.md) | 已建立；#3300–#3349 已迁入逐 Run 条目；其余编号待从历史资料核查/补录 |
| 02 | #3400–#3599 | 尚未创建 | 达到/开始该区间时创建 |
| 03 | #3600–#3799 | 尚未创建 | 达到/开始该区间时创建 |
| 后续 | 每增加 200 个连续 Run 编号 | `03-LEGADO-CI-{起始}-{结束}.md` | 按顺序自动扩展 |

**边界规则：** 01 分册只容纳 #3200–#3399；02 从 #3400 开始；每册 200 个编号，不按 success 数量计数。若某编号不存在、被删除或不可访问，记录缺证据原因，不能压缩编号区间。

## 7. 现存证据与原始资料位置

- 第三类原始技术主文档/历史任务/架构记录：`docs/required/03-LEGADO-RUNTIME.md`
- 新第三类总进度与要求索引：`docs/required/03-LEGADO-RUNTIME-INDEX.md`
- CI 分册 01：`docs/required/03-LEGADO-CI-3200-3399.md`
- CI #3300–#3349 原始逐 Run 审计账本：`docs/required/CI-3300-3349-AUDIT.md`
- 总工作流程：`docs/required/00-MASTER-WORKFLOW.md`
- 旧 #3279–#3300 记录：原第三类清单第 24 节（读取恢复后再迁移核对）。

## 8. 下一步严格顺序

1. 恢复读取 `03-LEGADO-RUNTIME.md` 的全文/分段能力；核对其当前 blob SHA 与本索引建立时版本。
2. 迁移并逐条核对 #3279–#3299 旧记录，核对 #3300 两处记录的 Run ID/Attempt。
3. 补查 #3200–#3278 是否存在第三类相关 workflow run，按实际记录逐条填入本分册。
4. 核查 #3350–#3399；若 Run 不存在或不可访问，写明证据和原因。
5. 复核 #3300–#3349 的每条记录与原审计账本，必要时把逐 Job/Step 原始矩阵及日志定位补齐。
6. 更新本索引的计数、未核查区间和下一项唯一工作。
7. 上述文档迁移闭环后，才回到 Runtime-3.0 Stage-0/Stage-1 的技术任务；继续遵守总清单登记、测试、exact SHA 和结果回写门禁。

## 9. 维护约束

- 本索引维护总进度、需求、分册目录、状态和下一步；不复制每个 CI 的技术流水。
- CI 分册保存每个 Run 的完整逐项证据；原始审计账本和 GitHub 日志作为来源证据保留。
- 原第三类技术主文档不删除、不覆盖、不因新索引而丢弃历史。
- 分册按编号顺序创建，单册最多 200 个 workflow run 编号。
- 任何文件读取失败、证据缺失、状态冲突均明确登记，不允许猜测填补。
