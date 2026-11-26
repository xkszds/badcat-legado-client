# 第三类总进度与 CI 分册索引：Legado Runtime

> **第三类唯一总入口。** 每次第三类任务必须先读 `docs/required/00-MASTER-WORKFLOW.md`，再读本文件；随后只读当前任务需要的 CI 分册或技术历史段。所有工作只在 `integration/legado-upstream-runtime`，禁止写入 main。
> 固定行为基准：`LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`；许可证 GPL-3.0。

## 1. 第三类总目标与不可降低的要求

交付独立运行的坏坏猫增强客户端：坏坏猫始终是唯一产品外壳，保留原版搜索 UI、页面、导航、收藏/书架、净化、书签、阅读器、CSS/排版和交互；Legado 只提供完整书源运行能力，不搬入 Legado UI、书架产品流程或 Reader UI。

完整书源兼容必须以固定 SHA 为唯一行为基准，逐项覆盖并差分验证：

1. BookSource/Book/BookChapter/ReadConfig 等模型字段、默认值、null/missing、序列化和未知字段。
2. 全部书源规则类型及 AnalyzeRule、AnalyzeUrl、CSS、XPath、JSONPath、正则、替换、上下文和错误语义。
3. Rhino、全部书源相关 JsExtensions 声明/参数/返回值/overload、jsLib、Java/JS Bridge、SharedJsScope。
4. HTTP/GET/POST/HEAD、请求配置、响应对象、header/cookie、多值 header、重定向、超时、取消和错误传播。
5. Cookie、登录/loginUi/loginInfo、源变量、会话隔离、BaseSource 完整生命周期、WebView/浏览器/验证码等源脚本实际使用的回调。
6. 导入/导出、搜索/发现/分页、详情、目录/目录分页、正文/多页正文、批量正文、章节身份与 URL alias。
7. 章节缓存、SaveToken/Fence、CacheManager、缓存失效、数据持久化和重启恢复。
8. 多类型真实书源 E2E、脱敏录制回放、Android 集成、坏坏猫原 UI 回归、exact HEAD APK/报告/哈希和许可证验收。

**禁止：** selector-only、shadow DTO 冒充官方模型、同名简化 API、no-op、空返回、假数据/假成功、测试替身进入生产路径；不得用单条 Search→Detail→TOC→Content 成功、外部 Python 抓取、Debug APK 构建或 CI workflow success 宣称完整兼容。缺少证据的能力必须标为未实现/待验证/阻断。

## 2. 固定实施顺序

0. 固定上游、API/依赖差异盘点与 Runtime-3.0 架构门禁。
1. 数据模型、身份与序列化。
2. AnalyzeRule/AnalyzeUrl 和全部解析/替换语义。
3. Rhino、JsExtensions、BaseSource、JS 上下文和生命周期。
4. HTTP、Cookie、登录、请求/响应和安全边界。
5. 书源导入、搜索/发现、详情、目录、正文及统一编排。
6. 章节身份、URL alias、BatchContentContext 和多页/批量正文。
7. 章节缓存、token fence、CacheManager、失效和原子性。
8. 阅读进度、书签、历史、ReadConfig/ParagraphAnchor 与 Badcat 数据映射。
9. 五域事实源、单写者、事务、迁移、回滚、重启恢复。
10. 契约/架构测试、脱敏回放、多类型真实源、真机、APK 和发布验收。
11. 与第一、二类集成并完成最终发布验收。

不得越过前置门禁；架构选择服务于完整兼容性，不得为了维持旧 B′ 或 Android-free 边界而缩减上游语义。不得未经单写者、备份、dry-run、幂等、回滚和恢复验证就冻结 schema v1 或切换 Room/数据事实源。

## 3. 任务树与逐项验收要求

| 大类 | 必须覆盖的具体要求 | 核心验收 |
|---|---|---|
| 0 架构/治理 | 固定 SHA、GPL 来源、模块边界、API/依赖差异、退出方案 | 每项可追溯；架构 GO/NO-GO 有证据 |
| 1 模型/序列化 | BookSource、Book、BookChapter、Rule、ReadConfig、身份映射 | 字段/默认/null/未知字段/双形态序列化差分 |
| 2 规则/内容 | AnalyzeRule、AnalyzeUrl、CSS/XPath/JSON/正则、变量、替换、分页 | 固定上游逐项差分，无静默空结果 |
| 3 JS/生命周期 | Rhino、JsExtensions 全 API/overload、Bridge、BaseSource、jsLib、上下文 | API/ABI/行为/回调/取消/释放差分 |
| 4 网络/会话 | HTTP、请求/响应、重定向、Cookie、登录、源变量、安全 | header/cookie/错误/超时/取消/源隔离一致 |
| 5 业务链 | 导入导出、搜索/发现/分页、详情、目录、正文、统一编排 | 多类型书源真实链路和回放通过 |
| 6 章节身份 | (bookUrl, chapterIndex)、URL alias、旧链接、批量正文、排序 | 重复 URL 不混淆；歧义 fail closed |
| 7 缓存/持久化 | 正文缓存、SaveToken/Fence、CacheManager、失效、原子性、旧数据迁移 | 旧异步写不能覆盖新内容；重启/并发正确 |
| 8 阅读状态 | 进度、书签、历史、ReadConfig、ParagraphAnchor、收藏映射 | 单一权威状态、重启恢复、旧链接兼容 |
| 9 五域 PoC | BookSource/Book/BookChapter/Progress/Cache、事务、架构守卫、迁移回滚 | 每域唯一事实源/写入者；迁移可回滚 |
| 10 验收交付 | 单元/契约、架构、回放、真实源、真机、CI/APK/manifest/GPL | exact HEAD 对应完整证据，所有关键门禁通过 |

## 4. 当前总体进度（2026-11-24，依据原第三类清单与最新记录）

<badge>总体：部分实现 / 未验收</badge>

- **完整 Legado 书源兼容：未完成。** Stage-0 架构/依赖闭包审查仍 in_progress/not accepted；Stage-1 仍 NO-GO。
- **模型/序列化：部分实现且存在运行时字段损失。** 六类规则类与 Gson adapter 声明已对齐固定上游并有局部测试；但 host BookSource 仍是 JSON 字符串 DTO，RuleResolver 丢弃 ContentRule.maxBatchSize / ReviewRule.enabled 等非字符串字段，官方 BookSource/Book/BookChapter/ReadConfig 完整模型闭包未完成（§24.51–§24.52 已补充实体证据，运行时与数据所有权仍未闭合）。详见 §24.50。
- **规则/JS API：未完成。** 104-name JsExtensions/JsEncodeUtils 盘点已建立，但签名、返回对象、caller、动态分派和行为差分尚未全部闭合；已发现多项真实 ABI/行为缺口。
- **HTTP/响应：部分实现且有阻断。** Rhino response facade 与 pinned Jsoup Response 在 bodyAsBytes、header/multiHeaders、cookie、response lifecycle 等方面仍有差异。
- **浏览器/验证码：未完成。** webView、startBrowser、startBrowserAwait、getVerificationCode、showBrowser、openUrl 等有真实书源 caller；Badcat 原生 typed callback 方案需独立登记实施。
- **Stage-0 最新证据：** 详细审计 §24.42–§24.53 已补充导航/浏览器、WebView、书源状态清理、jsLib 共享库、AnalyzeRule/AnalyzeUrl 四条业务链、RuleDataInterface 变量作用域、规则/BookSource/Book/BookChapter 模型，以及 Cache/CacheManager/ACache/BookCacheInfo 存储和清理语义；完整上游运行闭包、类型化数据所有权、缓存差分和动态验证仍待闭合。详见 `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md`。
- **业务链：局部实现。** Badcat host search/detail/TOC/content 编排存在，但不等于完整 BaseSource/session/规则执行生命周期。
- **章节身份：局部实现。** PoC-0.8/0.9 已将 index 带入主要内容边界并让旧 URL-only 只在唯一匹配时继续；完整 alias 候选与 BatchContentContext 未完成。
- **缓存：局部实现，未达兼容。** pinned Cache/CacheDao/CacheManager/ACache 的 Room + 50 MiB memory LRU + 文件 TTL 语义已审计（§24.53）；host 只有分离的 source KV/file cache 与 parsed chapter AtomicFile cache，generic CacheManager/ACache parity、完整 source/book mutation invalidation、跨实例 save-fence 与持久化差分仍未完成。
- **阅读状态/五域数据：未验收。** ReadConfig/ParagraphAnchor、单一事实源、单写者、跨域事务、迁移回滚和五域重启恢复未通过。
- **真实源/真机/发布：未验收。** 外部 Python 书源冒烟不算 Android + Legado Runtime E2E；真机、最终 APK 和发布门禁未完成。

## 4.1 Runtime-3.0 Stage-0 technical task registration — BookSource six rule models

- **Task ID:** R3-S0-MODEL-BOOKSOURCE-RULES-001
- **Status:** source-audit slice complete; runtime/model closure remains open.
- **Start HEAD:** e67add9c1c8bc4a031ef7a60e131c275c183fca2; branch `integration/legado-upstream-runtime`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** audit the six BookSource rule models (SearchRule, ExploreRule, BookInfoRule, TocRule, ContentRule, ReviewRule) and their JSON/Gson/serialization path against the pinned upstream; determine fields, nested objects, defaults, nullable/missing behavior, aliases, raw JSON retention and host codec loss.
- **Allowed files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and this overview only. Upstream and host source reads are evidence gathering; no source edits.
- **Callers / evidence:** locate all six upstream declarations, BookSource serialization/import/export and runtime accessors/callers; inspect host codec, source DTOs, rule resolver, fixtures and tests without changing them.
- **Acceptance:** source-path/line-backed field matrix for all six models; upstream-vs-host type/default/null/unknown-field/nested-value comparison; JSON string-vs-object handling and round-trip evidence; explicit confirmed facts vs static risks vs dynamic unverified items; duplicate check against existing audit sections.
- **Exclusions:** no production code, tests, Gradle, workflow, database/schema, UI or CI changes/runs; no claim of full model compatibility from raw JSON retention alone.
- **Result:** §24.50 records the six model field matrix, Gson/converter semantics, rawJson retention boundary, non-string field loss and Review caller gap. Next technical slice: inspect the outer pinned BookSource entity fields/defaults/identity/Room converter lifecycle against host catalog/DTO, without repeating six-rule findings.
 
## 4.2 Runtime-3.0 Stage-0 technical task registration — outer BookSource entity and persistence contract

- **Task ID:** R3-S0-MODEL-BOOKSOURCE-ENTITY-002
- **Status:** source-audit slice complete; runtime/entity/data-owner closure remains open.
- **Start HEAD:** 5566f49cdbad0be9a38016164e2b0fa897d8d71a; branch `integration/legado-upstream-runtime`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** inspect the complete pinned upstream BookSource entity outside the six nested rule models: every top-level property/type/default/nullability/identity/index, BaseSource inheritance and methods, Room converters/schema/migrations, import/export normalization, and actual caller/data-owner lifecycle; compare host DTO/codec/catalog/persistence without repeating §24.50 rule-model inventory.
- **Allowed files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and this overview only. Read source/test/Gradle/schema files for evidence, but do not edit them.
- **Dependencies:** §24.50 six-rule model audit; existing §24.37/§24.45/§24.46 source identity/catalog invalidation findings.
- **Acceptance:** line-backed full outer-field matrix; identity and uniqueness comparison; Room converter/schema/migration/import/export path; host DTO/rawJson/catalog persistence path; actual production callers and write order; explicit source facts vs static risks vs dynamic unverified; dedup with prior sections.
- **Exclusions:** no production code, tests, Gradle, workflow, database/schema, UI or CI changes/runs; no approval of Stage-1 and no claim that raw JSON persistence equals upstream entity semantics.
- **Result:** §24.51 records the 33-field outer entity, URL primary-key/Room schema and DAO transactions, Kotlin-vs-SQL enabledCookieJar defaults, host file-catalog boundary, strict coercion/duplicate-ID deltas and missing manifest/raw-JS import workflows. Next technical slice: audit pinned Book and BookChapter entity fields, identity/relations, variable persistence and host book/chapter DTO/storage closure; do not repeat §24.49's RuleData variable precedence.
 
## 4.3 Runtime-3.0 Stage-0 technical task registration — Book and BookChapter entity/data contract

- **Task ID:** R3-S0-MODEL-BOOK-CHAPTER-003
- **Status:** source-audit slice complete; Book/BookChapter/ReadConfig runtime and data-owner closure remains open.
- **Start HEAD:** c38367bcea78402c43bf836967a8908c632580d2; branch `integration/legado-upstream-runtime`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** audit pinned Book and BookChapter entity fields/defaults/nullability/identity/relations/indexes, Room converters and migrations, serialization/import paths, variable persistence, and compare Badcat's Book/BookChapter DTOs, storage and caller lifecycle.
- **Allowed files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and this overview only; inspect but do not edit production/test/schema files.
- **Dependencies:** §24.49 typed RuleData variable precedence; §24.51 BookSource source identity/Room ownership; existing chapter identity/cache findings.
- **Acceptance:** line-backed field/type/default/null matrix, Book and chapter composite identity/FK/index comparison, variable JSON + large-variable storage and write transaction path, host DTO/storage mapping, caller/data-owner lifecycle, explicit dynamic-unverified boundaries and dedup.
- **Exclusions:** no production code/tests/Gradle/workflow/database/schema/UI/CI changes or runs; no Stage-1 authorization.
- **Result:** §24.52 records the 34-field Book, 17-field BookChapter, nested 21-field ReadConfig, schema-113 identity/index/FK constraints, DAO preservation transactions and host's reduced DTO/SharedPreferences/cache ownership gaps. Next technical slice: inspect pinned Cache/BookCacheInfo/CacheManager storage and lifecycle against host FileChapterContentCache and cache invalidation callers; deduplicate existing §24.37/§24.48 cache findings.
 
## 4.4 Runtime-3.0 Stage-0 technical task registration — Cache / BookCacheInfo / CacheManager closure

- **Task ID:** R3-S0-CACHE-MODEL-004
- **Status:** source-audit slice complete; generic CacheManager/ACache compatibility and host cache invalidation remain NOT MET.
- **Start HEAD:** f6c1faafcf433a4f3a86a5d9faab1956584e0715; branch `integration/legado-upstream-runtime`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** inspect pinned Cache/BookCacheInfo entities, CacheManager/DAO/storage keys, expiry/eviction/clear semantics, source/book/chapter cache namespaces and all book-source caller lifecycles; compare host FileChapterContentCache and runtime invalidation without duplicating §24.37/§24.48/§24.52.
- **Allowed files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and this overview only; inspect but do not edit production/test/schema files.
- **Dependencies:** §24.37 BaseSource scope/cache lifecycle; §24.48 AnalyzeUrl/CacheManager caller coverage; §24.52 Book/BookChapter owner and host cache boundary.
- **Acceptance:** exact pinned key/owner/TTL/eviction/clear/write semantics; entity/DAO/schema fields; all book-source callers; host key and persistence mapping; stale-write/cancellation/invalidation comparison; source facts vs static inference vs dynamic unverified.
- **Exclusions:** no production code/tests/Gradle/workflow/database/schema/UI/CI changes or runs; no Stage-1 authorization.
- **Result:** §24.53 records the pinned Cache Room schema/DAO, CacheManager memory/disk/ACache TTL and delete semantics, BookCacheInfo cleanup projection/folder naming, startup expiry and BookHelp cleanup callers, plus host cache separation. No source/tests/Gradle/CI were changed or run. Stage-0 remains in_progress/not accepted; Stage-1 remains NO-GO.
 
## 5. 当前 CI 位置与分册规则

CI 审计按连续编号区间建立分册，每册最多 200 个 CI 编号，编号首尾包含且不得跨册重复。历史追溯已确认第三类 Legado Runtime 的 CI 至少从 #5 开始；#1–#4 暂无第三类关联证据。历史分册按以下顺序补建：
- 历史第 1 册：`docs/required/CI-0001-0199-AUDIT.md`（#0001–#0199；151/151 条第三类候选 CI 已逐项核验；#5–#29 的 22 条在主账本，#30–#99 的 53 条、#100–#131 的 24 条、#133–#199 的 52 条分别见 `CI-0030-0099-DETAIL-AUDIT.md`、`CI-0100-0131-DETAIL-AUDIT.md`、`CI-0133-0199-DETAIL-AUDIT.md`；151 次 workflow 均因旧 runner Android SDK Step 4 缺少 `tools` package 失败，Gradle/测试/APK/书源步骤未执行）。
- 历史第 2 册：`docs/required/CI-0200-0399-AUDIT.md`（#0200–#0399；待建立）。
- 后续历史册：#0400–#0599、#0600–#0799、#0800–#0999、#1000–#1199、#1200–#1399、#1400–#1599、#1600–#1799、#1800–#1999、#2000–#2199、#2200–#2399、#2400–#2599、#2600–#2799、#2800–#2999、#3000–#3199，均待按第三类边界核查后建立。
- 第 17 册：`docs/required/CI-3200-3399-AUDIT.md`（#3200–#3399；198/200 实际 run 已逐项登记，165 success / 33 failure；#3398–#3399 无真实 run）。
- 第 18 册：`docs/required/CI-3400-3599-AUDIT.md`（#3400–#3599；已创建，当前 19 次实际 run，16 success / 3 failure / 0 pending）。
- 每册只收第三类相关 CI；混合类别要标注交叉边界，纯其他类别 CI 不得计入第三类。历史初筛/元数据登记不等于逐 CI 详细审计完成。
- 原始分段审计文件暂时保留为证据来源：`CI-3200-3299-AUDIT.md`、`CI-3300-3349-AUDIT.md`。新 200 册已吸收现有逐 CI 详细记录；旧账本保留作来源副本，不删除。
- 技术实施历史与详细架构审计仍保留在本文件后续的技术历史文档：`03-LEGADO-RUNTIME.md`（旧版长档案，作为历史证据；日常入口以本总进度文件为准）。CI 功能流水以 200 册为准。

### 每一个 CI 必须逐项填写（不得只写 success/failure）

1. **身份与快照：** CI 编号、attempt、workflow/event、Run ID、分支、完整 exact HEAD SHA、触发时间、conclusion。
2. **Job/Step 矩阵：** 每个 Job/Step 名称、状态、是否实际执行、日志/报告是否存在。
3. **功能覆盖：** 每个成功执行的步骤具体跑通的模块/API/行为/产品功能；测试类型与覆盖边界；功能说明须由断言、日志或报告支持。
4. **失败/跳过：** 每项失败、跳过、取消、pending、未执行步骤；日志证据、直接失败点、已证实根因分类和未验证的下游范围。
5. **书源 E2E：** 源名称/类型、环境/时间、真实请求、搜索/发现/分页/详情/目录/正文/多页/登录/Cookie 实际到达环节；区分 App Runtime E2E、录制回放、外部 Python/HTTP 冒烟。
6. **分层结论：** 分别写 workflow、Job/Step、自动化测试、功能覆盖、真实书源、产品验收结论；禁止用 workflow success 代替功能完成。
7. **证据与复核：** 日志/报告/JUnit/产物链接或路径、Job ID、关键摘要、审计日期、复核状态；写完重新读取账本逐项复核。

**CI 状态纪律：** 不跨 SHA 借用结果；不把 skipped/pending 写通过；不把 continue-on-error 的脚本失败隐藏在 workflow success 下；不把 mock/fixture 当真实网站；不把外部 Python 解析当 Android Runtime；缺日志/报告必须标“证据不可得/未验证”，禁止推测。

## 5.2 历史第 2 册 #0200–#0399 审计检查点（2026-11-24）

- 分册已创建：`docs/required/CI-0200-0399-AUDIT.md`，状态 `in_progress`。
- #200–#249 五十条已按七组标准逐项核验并登记；五十条均为第三类 Legado URL/Rhino 相关 CI，attempt 1，唯一 Run ID/exact SHA 已写入第二册索引。
- 五十条共同结论：Job `assemble-debug` 在 Step 4 `Set up Android SDK` 失败，日志 `sdkmanager tools` / `Failed to find package 'tools'`；Step 5–9 skipped；无 Gradle、测试、JUnit、APK、artifact 或真实源 E2E 证据。
- #200 为 unsupported static URL method 错误码测试断言修改；#201 为 URL method/charset 边界文档；#202 为 pinned Rhino integration source audit；#203 为该 audit 的文档状态登记；#204 为 Rhino 依赖和 18-file source inventory；#205 为 inventory 文档状态登记；#206–#209 依次增加 CompiledScript 抽象接口、隔离 ScriptBindings/共享 source global、bindings 构造 helper、ScriptException 错误类型。以上 CI 均未证明所述功能运行通过。
- 下一条续查：#250；不得重复抓取 #200–#249。后续候选先确认第三类边界，再逐项取 Run/Job/Step/log/diff/artifact 证据。
- #210–#225 逐项审计：16/16；新增 Rhino Java wrapper/class shutter/context/script engine/Gradle module/license files；16 条均在 SDK setup Step 4 阻断，不能视为代码编译或功能通过。
- 本册 #0250–#0399 尚未完成筛查和逐项审计；不得提前标记整册完成。

## 5.1 CI 去重检查点与续查规则（2026-11-24）

**第 1 册 #0001–#0199：151/151 条已登记第三类实际 CI 完成逐项历史审计，编号唯一。** 已审计 CI 编号范围为：#5；#7–#18；#20–#24；#26–#49；#51–#53；#55–#57；#60–#62；#65–#66；#68–#69；#78–#94；#97–#102；#104；#107–#115；#117；#122–#131；#133–#135；#137–#148；#150–#156；#158–#177；#180–#181；#184；#188–#191；#196–#197；#199。

- 唯一详细档案：#5–#29 → `CI-0001-0199-AUDIT.md`；#30–#99 → `CI-0030-0099-DETAIL-AUDIT.md`（53/53）；#100–#131 → `CI-0100-0131-DETAIL-AUDIT.md`（24/24）；#133–#199 → `CI-0133-0199-DETAIL-AUDIT.md`（52/52）。
- 这些已审计 CI 不再重新抓取 workflow、Job、logs、commit diff 或 artifacts；后续只读取其已登记结论。只有新 attempt、新 Run ID/exact SHA、证据冲突或明确复核要求才新增取证。
- 本轮 #43–#94 的重复调取不产生新的审计项，按既有档案为准。
- 下一步进入 #0200–#0399 的历史候选发现；每次检查前必须先对照第 1 册及后续分册的唯一 CI/Run/Attempt/SHA 索引，跳过已登记项，只处理真正新增的第三类 CI。

## 6. 当前 CI 分册进度

### 第 1 册：#3200–#3399

- #3200–#3225：旧账本有 26 个逐次条目；22 success、4 failure、0 pending。失败 #3213–#3216 为 Login UI v2 测试 ClassCastException；#3217 后修复该特定问题并通过 335/335 测试。功能边界和外部 Python 冒烟限制见旧账本。
- #3226–#3249：24/24 已逐次审计并写入 CI 分册附录 C；12 workflow success / 12 failure。#3239–#3247 暴露 RuleAnalyzer APK DEX 缺类，#3249 已证明类定义进入 APK 并通过 API28/35 设备端 class-load/split smoke；但 #3249 workflow 仍因 Maven Central 429 失败，Android 四源 E2E 仍为 0/4。
- #3250–#3278：29/29 已逐项审计并写入 CI 分册附录 D；19 workflow success / 10 failure。#3250–#3253 Android 四源 E2E 为 0/4；#3254–#3261 仍运行 Python corpus 1/4，#3262 删除该步骤，#3273 首次验证暂停 host filter 自检通过。#3263–#3272 的 DEX failure 已证实为 verifier 正则 false-negative：10 份 DEX 报告均含 RuleAnalyzer，API28/API35 instrumentation 均 5/5；#3273 修复脚本后 workflow 恢复 success。
- #3279–#3299：21/21 已逐项审计并写入补充档案 `CI-3279-3299-DETAIL-AUDIT.md`；21 workflow success、21 Job success、每个 Job 的 53 个返回 Step 全部 success。21 个 commit 均为 docs-only；第三类 app 单测、API28/35 instrumentation、JVM ABI、Debug APK/DEX 检查均完成。Bixiange/Pinshuzhai 为外部 Python smoke，不是 Android Runtime E2E。
- #3300–#3349：50/50 已建立逐项功能覆盖档案，并已按每个 CI 独立条目迁入本 200 册附录 B；45 workflow success、5 failure、0 in_progress。#3315/#3343/#3345 为 Parcelize 插件解析失败；#3317/#3319 为 Maven Central HTTP 429 导致 Robolectric 依赖失败；#3323 workflow success 但 Bixiange 外部脚本实际 HTTP 503。所有这些均不构成完整 Legado E2E。
- #3350–#3357：8/8 已逐项审计并写入 `CI-3350-3357-DETAIL-AUDIT.md`；8 workflow/Job success、每个 Job 53/53 returned steps success；均为 docs-only/bookkeeping CI，真实书源部分仅外部 Python smoke。
- #3358–#3397：40/40 已逐项审计并写入 `CI-3358-3397-DETAIL-AUDIT.md`；38 success、2 failure。#3361 为 Kotlin Parcelize 插件解析失败；#3389 为 Robolectric 依赖 HTTP 429。#3398–#3399 未发现实际 run，不造记录。

**当前可确认统计：** #3200–#3399 第 17 册记录 198/200 个实际 CI（#3200–#3397），165 success / 33 failure / 0 pending；#3398–#3399 无真实 run。#3400–#3599 第 18 册记录 19 次实际 run（#3400–#3422 的 14 次 + #3463–#3467 的 5 次），16 success / 3 failure / 0 pending。两册 workflow 统计仅表示 CI 终态，不等于产品功能通过数。

## 7. 关联文件与读取顺序

1. `00-MASTER-WORKFLOW.md` — 全项目总门禁、分支/HEAD、任务登记、测试/CI/结果回写制度。
2. `03-LEGADO-RUNTIME-OVERVIEW.md` — 第三类总进度、硬要求、任务树、CI 分册位置（本文件）。
3. 当前 200 CI 分册 — 当前编号区间的逐 CI 功能证据。
4. 原始 CI 分段账本 — 仅在迁移/核查对应历史条目时读取。
5. 旧第三类长档案 — 仅在技术任务需要恢复某项历史实施/架构审计证据时按章节读取，不作为每次 CI 任务的整份必读材料。

## 8. 下一步唯一顺序

1. 历史第 1 册 #0001–#0199 已完成 151/151 候选审计；下一步建立/核验历史第 2 册 #0200–#0399，先检索真实 run 和第三类边界，不预造编号。
2. 第 17 册 #3200–#3399 已登记 198/200 实际 run：165 success / 33 failure；#3398–#3399 无真实 run。
3. 第 18 册 #3400–#3599 已建立，当前 19 次实际 run：16 success / 3 failure；完整状态见 CI-3400-3599-AUDIT.md。
4. #3463–#3467 已完成终态核验：5/5 success、每个 Job 53/53 steps success；详细证据见 CI-3463-3467-DETAIL-AUDIT.md。
5. 每次新发现真实第三类 CI，按对应 200 编号册登记 exact SHA、Run/Job/Step、日志/报告/产物及实际功能覆盖；无真实 run 的编号不造记录。
6. CI 审计与 Runtime 技术任务分开登记；Stage-0 仍 in_progress/not accepted，Stage-1 仍 NO-GO。

### Runtime-CI-HIST-3200-3299 progress checkpoint — #3226–#3278（2026-11-24）

- **状态：** `in_progress`；本 200 册现有逐 CI 记录 100/200。附录 C 已记录 24 个独立 run：12 success、12 failure、0 pending。
- **关键功能证据：** #3226–#3238 的标准 47-step workflow、335/335 JUnit、API28 CookieManager/API35 WebView callback、Rhino/JS ABI/依赖闭包/G2d 探针和 Debug APK 均按各自 SHA 通过；外部 Python Bixiange/Pinshuzhai 脚本通过但上传的四源样本仅 1/4。
- **关键失败与修复进展：** #3239–#3247 证明 contract JAR / Android runtime classpath 通过仍不足以将 RuleAnalyzer 放入 APK DEX；#3249 通过 classesDirs 的 ConfigurableFileCollection 发布修正，在 14 个 DEX 的 classes5.dex 找到 RuleAnalyzer 定义，并通过 API28/35 Class.forName + split smoke。#3249 workflow 仍因 Maven Central HTTP429 失败；Android 四源 E2E 0/4（两源网络超时、一源安全阻断、一源 source_rule_failure）。
- **历史快照（已被本页当前状态覆盖）：** 当时 #3250–#3299 仍在审计；现 #3200–#3397 已完成 198/200 实际 run 核查。Stage-0 仍 in_progress/not accepted，Stage-1 仍 NO-GO。


### Runtime-CI-HIST-3200-3299 progress checkpoint — #3250–#3278（2026-11-24）

- **状态：** in_progress；本 200 册现有逐 CI 记录 129/200。附录 D 已记录 29 个独立 run：19 success、10 failure、0 pending。
- **真实书源暂停边界：** #3250–#3253 的 API35 四源 Android E2E 为 0/4；#3254–#3261 虽排除了 Android E2E，却仍执行 Python uploaded-corpus 1/4；#3262 才移除上传 corpus step。#3265/#3266 的 host filter 与 self-test 因 DEX gate failure 未执行；#3273 首次真实执行并通过，之后四个暂停 host 均被跳过。本轮没有重新请求四个暂停书源。
- **DEX 验证纠正：** #3263–#3272 失败是 verifier 对 tab-separated apkanalyzer 输出的 false-negative；10 份 DEX report 均含 RuleAnalyzer，API28/API35 instrumentation 各 5/5。#3273 的 literal class-name matcher 修复 verifier，随后 53/53 steps success。
- **临时 Scriptable 实验：** #3257–#3259 337/337 测试通过，但 #3260 已撤销 broad normalization 及两项新增测试；不计入当前功能。
- **历史快照（已被本页当前状态覆盖）：** 当时 #3279–#3299 已补齐，#3350–#3357 待审计；现 #3200–#3397 已完成 198/200 实际 run 核查。Stage-0 仍 in_progress/not accepted，Stage-1 仍 NO-GO。


### Runtime-CI-HIST-0001-0199 progress checkpoint — #0005–#0160（2026-11-24）

- **状态：** 第一册已登记的 151/151 条第三类实际 CI 均已完成逐项历史审计；#5–#29 为 22 条，#30–#99 为 53 条，#100–#131 为 24 条，#133–#199 为 52 条。当前转为去重后的历史候选发现/补漏阶段，不再复查已登记 CI。
- **本轮审计范围：** #30–#49、#51–#53、#55–#57、#60–#62、#65–#66、#68–#69、#78–#94、#97–#102、#104、#107–#117、#122–#160（仅对存在真实 CI Run 的候选登记；编号缺口不虚构）。
- **共同证据结论：** 本批历史 Android debug workflow 的 Job 均在 Step 4 `Set up Android SDK` 失败，日志为 `sdkmanager tools` 找不到旧 `tools` package；Gradle、contract tests、APK assemble、artifact upload 均 skipped，故本批没有单测、APK 或真实功能通过证据。提交 diff 仅能证明代码变更意图/范围，不能代替执行验证。
- **功能边界发现：** 历史快照逐步形成 source metadata、selector 子集、HTTP transport、static URL resolver、CSS-only search/detail/TOC/content、SourceReadingEngine 和临时 source catalog UI；这些均非完整 Legado Runtime，且未完成 Badcat 原版 UI 集成。#109/#113/#124/#128/#137/#144 的 tests 为 fake/synthetic 或仅局部契约，不能记作真实书源 E2E。
- **下一步：** 第一册已登记 CI 不再重查；先查重，再从 #0200–#0399 发现尚未登记的第三类历史 CI。Stage-0 仍 in_progress/not accepted，Stage-1 仍 NO-GO。


## 4.5 Runtime-3.0 Stage-0 technical task registration — Badcat production caller and adapter closure

- **Task ID:** R3-S0-BADCAT-CALLER-CLOSURE-005
- **Status:** source-audit slice complete; shared runtime lifetime and host state ownership remain NOT MET.
- **Start HEAD:** `001a792475b15cc1de98255c26505d716098239e`; branch `integration/legado-upstream-runtime`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** close the remaining Badcat-side production call graph identified in audit §15.2(1): trace every production entry point from Badcat activities/view models into source-reading services, runtime factories, adapters and persistence/session/cache stores; identify alternate construction paths and legacy-store readers/writers; distinguish reachable production code from temporary/demo/test-only paths; map each call to the pinned upstream capability it intends to represent and flag signature/return/default/error/cancellation/lifecycle substitutions.
- **Allowed files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and this overview only. Source, test and build files may be read as evidence; do not edit them.
- **Dependencies:** audit §15/§15.1/§15.2, §16 and §24.37–§24.53; do not repeat already established per-symbol upstream semantics or cache/model inventories.
- **Inspection boundary:** enumerate app production Kotlin/Java entry points and constructor/factory call sites; follow reachable service and adapter edges through catalog, request, Rhino, login/Cookie, variables/files and chapter cache; inspect alternate/test/temporary UI wiring only to classify reachability. Record exact paths and line ranges plus exact HEAD.
- **Acceptance:** a path-backed entrypoint→factory→service→adapter→store graph; all discovered production construction sites and alternate paths; read/write owner map for stores actually reachable from those paths; explicit dead/temporary/test-only classification; mapping to already audited upstream contracts; separate confirmed facts, static risks and dynamic-unverified behavior; deduplicate against prior audit sections.
- **Exclusions:** no production/test/Gradle/workflow/database/schema/UI changes; no CI runs or CI history work; no Stage-1 authorization; no claim of runtime behavior without execution evidence.
- **Result:** §24.54 records launcher/search/catalog/book/chapter/login entrypoints, factory/service wiring, independent per-screen/per-action Rhino runtime construction, host persistence/navigation owners, and static-vs-dynamic boundaries. No source/tests/Gradle/CI changed or run. Stage-0 remains in_progress/not accepted; Stage-1 remains NO-GO.

## 4.6 Runtime-3.0 Stage-0 technical task registration — semantic parity matrix closure

- **Task ID:** R3-S0-SEMANTIC-PARITY-CLOSURE-006
- **Status:** static consolidation slice complete; Stage-0 architecture/dependency/ABI/provenance closure remains open.
- **Start HEAD:** `9961357eec4a8667a88f69e0397b6618a97f6195`; branch `integration/legado-upstream-runtime`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** turn the existing first-pass matrix (§16) and pinned source/host evidence (§24.37–§24.54) into a capability-by-capability semantic status matrix; separate authentic source, exact parity, typed adapter candidate, host-only behavior, missing behavior, unknowns and dynamic test requirements.
- **Allowed files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and this overview only; source/test/Gradle/schema reads solely to resolve cited uncertainty. No source edits.
- **Dependencies:** §16; §24.37–§24.54; overview §4.5; Stage-0 blockers §8–§9.
- **Acceptance:** each major inventoried capability has pinned evidence, reachable Badcat caller/adapter where established, honest parity state, contract deltas, unresolved dependencies and required differential tests; duplicate claims reconciled; A/B/C compared against same matrix; explicit Stage-0 exit checklist and GO/NO-GO rationale.
- **Result:** §24.55 records 20 capability rows, separates authentic source presence from behavior parity, compares A dedicated Android Runtime / B controlled upstream core extraction / C hybrid against common evidence, and enumerates residual Stage-0 exit artifacts. No row has exact parity evidence. No source/test/Gradle/schema/UI/workflow/CI was changed or run.
- **Exclusions:** no CI history/runs; no production/test/Gradle/workflow/database/schema/UI changes; no Stage-1 authorization; no complete-compatibility claim from static evidence.
- **Next:** register a separate Gradle dependency/packaging closure task before additional source/build inspection or any implementation decision.

## 4.7 Runtime-3.0 Stage-0 technical task registration — Gradle dependency and packaging closure

- **Task ID:** R3-S0-GRADLE-PACKAGING-CLOSURE-007
- **Status:** static inventory complete; exact-SHA runner restored and partial Gradle resolution/compile evidence obtained; final `:app:assembleDebug` result and packaging closure remain unconfirmed (see §4.7.1).
- **Start HEAD:** `b42d38bf20c28a2d191c5809fa70048e190e3bdb`; branch `integration/legado-upstream-runtime`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** establish the exact pinned upstream Gradle runtime/build closure and evaluate whether Runtime-3.0 controlled upstream-core extraction can exclude Legado product UI/Reader without severing source-runtime behavior.
- **Allowed files:** audit and overview documentation only. Read upstream/Badcat Gradle/settings/catalog/build scripts, manifests, source/resource inventories and prior closure evidence; no source/build/test/workflow/schema/UI edits.
- **Dependencies:** audit §8–§9, §16, §18, §24.24, §24.55; overview §2.2–§2.4 and §4.6.
- **Result:** §24.56 records exact module/plugin topology, static source/resource/asset/manifest inventories, direct dependency families, Cronet JAR/native download and ASM transformation, the current Badcat selected-source module boundaries, and historical 658-node / 661-file graph snapshots plus the 211-line narrow-probe failure. No resolved Gradle variant/artifact graph was generated. No CI was started and no Gradle/source/test/workflow/schema/UI file was changed.
- **Blocker:** verified in audit §24.60: OpenJDK 21 exists, but Gradle and its wrapper cache are absent; Android SDK/SDK manager/adb are absent; GitHub checkout fails DNS resolution (`Could not resolve host: github.com`). Therefore exact resolved configurations, generated outputs, manifest/resource merge and packaging evidence cannot be produced in this container. This is an environment blocker, not an upstream compiler failure. Do not infer resolved graph from version declarations.
- **Exclusions:** no CI history/runs; no production/test/Gradle/workflow/database/schema/UI changes; no dependency upgrades; no source import or module-boundary edits; no Stage-1 authorization.
- **Runner check:** audit §24.60 records OpenJDK 21 present; no Gradle executable/wrapper cache, no Android SDK/sdkmanager/adb/android.jar, and GitHub DNS unavailable. No build or CI was run. Resolved dependency/variant graph, generated outputs, manifest/resource merge and APK/DEX packaging remain NOT MET. **Next:** use a network-enabled exact-SHA Gradle + Android SDK runner; until then dependency closure stays blocked.


### 4.7.1 Task 007 — exact-SHA runner recovery and partial build evidence (2026-11-25)

- **Runner:** Ubuntu device `VM-0-5-ubuntu`; JDK 21.0.13; Gradle 8.14.4; Android SDK API 36 and Build Tools 36.0.0 installed. SDK licenses were accepted under the user's explicit authorization. Build Tools 35.0.0 and Platform-Tools 37.0.3 were also installed by SDK/AGP tooling during setup.
- **Checkout identity:** a fresh Git checkout of `integration/legado-upstream-runtime` resolved to exact HEAD `b644bef2bac68401804c5807a1ca79ae53a683a6`; the `upstream/legado` submodule resolved to `330e606c4d57e08adc056ff8ab7a4132c784da9a`. The checkout was clean when identity was checked.
- **Gradle evidence:** `gradle help` and `gradle projects` completed successfully; the project graph reported five projects. `:app:dependencies --configuration debugRuntimeClasspath` completed successfully, giving a real resolved debug runtime dependency configuration for this exact checkout.
- **Packaging/compile attempt:** the first `:app:assembleDebug` attempt stopped at a missing local Maven artifact, `org.htmlunit:htmlunit-core-js:5.3.0-legado.4`. The exact upstream submodule JAR was checked against its upstream `SOURCE.md` SHA-256 (`d720f34285515025e4ccc80a5b92aed42cb26c03a7715ca53583d2c74ae51df7`). A temporary copy was then used for a second `:app:assembleDebug` attempt, with shell-exit cleanup configured. The observed task output passed `:legado-rhino:compileDebugKotlin`, `:legado-runtime:compileDebugKotlin`, and entered `:app:compileDebugKotlin`.
- **Unresolved result:** the remote process-output request timed out while the second assemble was running. No final Gradle exit code, completed `:app:compileDebugKotlin`, APK, manifest/package result, DEX, R8, or ABI evidence has been recovered. Temporary JAR cleanup has not yet been verified. Do not call the build successful or failed until the process/log and filesystem are checked.
- **Scope:** no CI was triggered; no production source, test, Gradle, workflow, schema, or UI file was changed. This runner activity is evidence gathering only and does not authorize source migration or Stage-1.
- **Next:** recover the existing process/log state without starting a duplicate build; verify temporary JAR cleanup and any APK/DEX/manifest outputs; then complete the resolved variant/packaging and compiled ABI evidence. Keep Stage-0 not accepted until all remaining caller/data-owner, provenance/license SBOM, and reproducible acceptance requirements are met.

## 4.8 Runtime-3.0 Stage-0 technical task registration — reflection/JVM/Rhino ABI and source provenance inventory

- **Task ID:** R3-S0-REFLECTION-ABI-PROVENANCE-008
- **Status:** static source/provenance inventory complete for inspected slices; compiled ABI and full distribution-license acceptance remain NOT MET.
- **Start HEAD:** `e7f242273a00c1b050ce86dcaeb5c265207db1ee`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** add non-duplicative static evidence for reflection/string-based class loading, Rhino-visible Java bridge surface and JVM ABI-sensitive declarations in the pinned source-runtime closure; inventory exact upstream/local file provenance, hashes and license notices for already reused runtime/parser/Rhino source slices.
- **Allowed files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and this overview only. Read pinned upstream source/tree and Badcat source/build files as evidence; no source edits.
- **Dependencies:** audit §3, §8–§9, §16, §24.37–§24.56; overview §4.5–§4.7. Reuse prior caller/Gradle findings; do not repeat them.
- **Inspection boundary:** statically trace Class.forName/classloader/reflection/member lookup/annotation-driven registries/service loading/JNI/native declarations and JS-to-Java wrapper entrypoints in the identified source-runtime candidates; record public/protected declarations, overload/default-argument/bridge/synthetic/annotation risks only where source evidence supports them. Compare selected local copied/injected files to exact pinned upstream paths using content hashes and diff classification; inventory applicable license headers and repository license/notice files.
- **Acceptance:** path/line-backed reflection and bridge inventory; exact pinned/local provenance table with hash/equality or modification status; license/notice inventory with unresolved redistribution questions; separate source-level facts from compiled ABI and runtime behavior that cannot be established without the blocked exact-SHA Gradle runner; deduplicate prior audit sections.
- **Exclusions:** no production/test/Gradle/workflow/database/schema/UI changes; no CI runs or CI history work; no source copying or module-boundary edits; no Stage-1 authorization; no claim that static signatures prove compiled ABI or Rhino runtime parity.
- **Result:** audit §24.57 records source/blob provenance for the seven rule models + Gson, additional rule files, Rhino Kotlin/Java sources, HtmlUnit notices, parser adaptations, exact upstream submodule pin, JavaScript bridge annotations, Rhino wrapper/class-shutter security surface and license inventory. 17/18 copied Rhino Kotlin files and all 12 copied HtmlUnit Java files are exact blobs; local RhinoScriptEngine has a local-only callback method; AnalyzeByXPath is adapted. No compiled ABI/R8/DEX proof or legal review was produced. No source/test/Gradle/workflow/schema/UI/CI was changed or run. Audit commit: `ca69e26d6b0c75715bb3642d2eccd51e53b688de`.


## 4.9 Runtime-3.0 Stage-0 — core execution closure and typed host-port boundary

- **Task ID:** R3-S0-CORE-EXECUTION-CLOSURE-009
- **Status:** major source-level boundary map complete for inspected roots; transitive compiler/Gradle closure and all-port differential proof remain NOT MET.
- **Start HEAD:** `46ebc565a4ec8e81241e4c96b6417881b813edd5`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **User priority:** prioritize full source-runtime compatibility and defer minor/isolated edge defects; do not spend this task on CI history or duplicate prior findings.
- **Goal:** establish a major-capability, source-backed boundary map for the pinned execution path from BookSource/BaseSource through AnalyzeRule/AnalyzeUrl, Rhino/JsExtensions/jsLib, HTTP/Cookie/login/WebView/browser/verification, cache and source lifecycle. Classify each dependency as authentic runtime core, typed Android host port, Legado product UI/Reader-only, or unresolved; identify where the existing Badcat modules are structurally insufficient and what minimum authentic closure must be preserved.
- **Allowed files:** audit and overview documentation only. Read exact pinned upstream and Badcat source/build files as evidence. No production/test/Gradle/workflow/schema/UI edits; no CI.
- **Dependencies:** overview §§1–2, §§4.5–4.8; audit §§8–9, §16, §§24.37–24.57. Reuse existing model, cache, ABI, Gradle and caller findings; do not re-audit minor per-method differences.
- **Inspection boundary:** trace the critical upstream roots and their direct/transitive service calls; map corresponding Badcat production paths and module boundaries; capture exact source paths and caller edges for missing behavior; determine whether the current :legado-runtime, :integration-contract, :legado-rhino, and :legado-runtime-poc boundaries can contain the authentic closure without fake/shadow/no-op implementations or importing Legado product UI/Reader.
- **Acceptance:** one capability-level dependency/boundary matrix; a minimum authentic runtime source/service closure with unresolved transitive edges explicitly listed; typed host-port contract inventory including callbacks, threading/cancellation, identity, persistence and lifecycle semantics; evidence-backed architecture disposition and Stage-0 remaining GO/NO-GO items.
- **Exclusions:** no small defect sweep; no individual book-source site debugging; no CI runs/history; no implementation or module-boundary changes; no Stage-1 authorization; no claim of full compatibility from static graph evidence.
- **Result:** audit §24.58 maps BaseSource/BookSource, AnalyzeRule/AnalyzeUrl, JsExtensions, SharedJsScope/JsSourceEngine, CookieStore/CacheManager and browser/verification/navigation service edges against the current Badcat modules and typed ports. Confirms the production runtime source sets still lack the authentic BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions closure; parser modules and host selector/Map orchestration are not full execution parity. Formal direction remains authentic upstream core + typed host ports + Badcat-only UI; buildability remains unproven. No source/test/Gradle/workflow/schema/UI/CI changed or run. Audit commit: `38a7662f44fc8835e261abf7a1576330a297da34`.
- **Next:** use the closure map to resolve remaining Stage-0 architecture questions; only after Stage-0 acceptance register the Stage-1 runtime-boundary PoC and begin actual compatibility implementation.


## 4.10 Runtime-3.0 Stage-0 — behavior-bearing source closure decomposition and product-boundary proof

- **Task ID:** R3-S0-SOURCE-CLOSURE-DECOMPOSITION-010
- **Status:** root provenance 17/17 exact; root/caller/product-boundary decomposition complete for inspected G1 roots; full compiler/Gradle closure and differential port acceptance remain NOT MET.
- **Start HEAD:** `0935239977a736633e99236c7cb672d204cef30c`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Priority:** mainline closure work only. Defer isolated API edge cases, individual source/site issues and CI history. Do not repeat the already established 661-file/6,303-import or 1,085-file/9,983-import graph totals.
- **Goal:** decompose the 17 behavior-bearing roots in the pinned G1 manifest into (a) mandatory source-runtime core, (b) typed host-port implementations, (c) Badcat product/UI-only dependencies that must be replaced at the platform boundary, and (d) conditional/unrelated features requiring caller proof. Determine whether the complete book-source execution path can be preserved without importing Legado UI/Reader or a second authoritative database.
- **Allowed files:** audit and overview documentation only. Read the exact pinned upstream source and current Badcat modules/callers/build files as evidence; no source edits.
- **Dependencies:** overview §§1–2, §§4.5–4.9; audit §§24.55–24.58; G1 manifest and G2 graph historical reports. Treat old graph outputs as lower-bound/lexical navigation evidence, not compiler truth.
- **Inspection boundary:** inspected the 17 pinned roots, direct imports/declarations and real production caller anchors; traced search/explore/detail/TOC/content/batch/login/verification/HTTP/Cookie/cache/jsLib and classified Room/reader/media/UI edges as authentic model semantics or replaceable host services. Cross-checked current Badcat typed ports and production source sets.
- **Acceptance:** root-by-root classification with exact source paths/caller evidence; mandatory authentic source groups; explicit typed port contracts; product-only exclusion candidates with proof and unresolved dependencies; one evidence-backed minimum closure proposal and GO/NO-GO conditions. No dependency edge may be removed merely because its package name looks UI-related.
- **Exclusions:** no production/test/Gradle/workflow/database/schema/UI changes; no CI; no small-defect sweep; no source import or implementation; no Stage-1 authorization; no claim of complete compiler closure from import graphs.
- **Result:** audit §24.59 confirms all 17 G1 behavior-bearing roots still match exact pinned upstream blobs; maps their runtime/core vs typed host-port roles; traces WebBook→JsSourceBook/AnalyzeRule/AnalyzeUrl→BaseSource/JsExtensions/Rhino→HTTP/Cookie/Cache/WebView/verification→typed Book/BookChapter context; identifies RssJsExtensions as a transitive source-visible callback API without requiring RSS Reader UI; proposes controlled upstream core + typed Badcat host adapter + compatibility facade + Badcat-only UI. Current :legado-runtime/:integration-contract/:legado-rhino source sets remain structurally incomplete. No production/test/Gradle/workflow/schema/UI/CI changed or run. Audit commit: `655176b6aba78eb7f6034ea6f64c0cf3261f17e6`.

## 4.11 Runtime-3.0 Stage-0 — exit-evidence reconciliation and gate matrix

- **Task ID:** `R3-S0-EXIT-EVIDENCE-RECONCILIATION-011`
- **Status:** completed as a documentation/evidence-reconciliation slice; Stage-0 itself remains NOT accepted.
- **Start HEAD:** `6a1afb466db254979f69b0b8a6f1075ec06edc0f`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** reconcile Stage-0 exit criteria in audit §24.55 against the newer evidence in §24.56–§24.60 and tasks 006–010; distinguish completed static slices, partial inventories, environment-blocked evidence and prerequisites for Stage-1. Produce one non-contradictory GO/NO-GO matrix and a single next mainline task.
- **Allowed files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and this overview only. Read existing audit evidence; do not repeat completed source/CI inventories.
- **Dependencies:** overview §§1–2 and §§4.6–4.10; audit §§24.55–§24.60; pinned upstream SHA above.
- **Acceptance:** every Stage-0 exit row has a current evidence-backed status, exact evidence section/task, remaining artifact/proof and blocker owner/type; reconcile stale wording without rewriting historical findings; separate Stage-0 prerequisites from Stage-1 dynamic acceptance; preserve Stage-1 NO-GO until all Stage-0 gates are met.
- **Result:** audit §24.61 reconciles all nine Stage-0 exit rows against tasks 006–010 and evidence §§24.55–§24.60. It separates static inventory completion from missing resolved Gradle/compiled ABI/full caller closure/license SBOM/reproducible bundle. No exact-parity capability is established. The Gradle blocker is environmental, not a source compile failure. No source/test/Gradle/workflow/database/schema/UI/CI changed or run.
- **Disposition:** reconciliation task complete; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**. Next mainline task is the complete JsExtensions public API/overload/inherited-caller-to-host-port closure, not a minor defect sweep or CI audit.
- **Exclusions:** no production/test/Gradle/workflow/database/schema/UI changes; no CI; no new build claims; no Stage-1 authorization; no full-compatibility claim.

## 4.12 Runtime-3.0 Stage-0 — complete JsExtensions public API and caller closure

- **Task ID:** `R3-S0-JS-EXTENSIONS-API-CALLER-CLOSURE-012`
- **Status:** in_progress; pinned declaration/inheritance inventory is mapped, but complete source caller coverage and one-to-one host-port acceptance remain open.
- **Start HEAD:** `2f5bc46f4a69314b74b174e084d59f18a362f4f4`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** close the full pinned script-visible runtime API, not another isolated method fix: reconcile the existing 104-name inventory against exact declarations, overloads, parameter/return types, nullability/default/suspend semantics, inherited RssJsExtensions/WebJsExtensions/SourceLoginJsExtensions methods, real source-runtime callers and the current Badcat typed host-port mapping. Scope amendment: also include the pinned JsHelp.md object namespaces (`java`, `source`/`sourceApi`, `cookie`, `cache`, `book`, `chapter`, and `config`) because they are part of the script-visible binding contract even though they are not all JsExtensions methods.
- **Allowed files:** audit and overview documentation only. Inspect pinned upstream declarations/callers and current Badcat contracts as evidence; do not modify production/test/Gradle/workflow/schema/UI files.
- **Dependencies:** audit §§24.37–24.46, §§24.57–24.61; overview §§4.8–4.11; pinned upstream SHA above.
- **Acceptance:** complete declaration/overload and binding-namespace matrix with exact upstream path/line; inherited surface and caller classification; host-port disposition for every source-visible method/object; reconcile JsHelp.md contracts with source declarations; deduplicate already-recorded behavior deltas by linking prior audit sections; distinguish source facts, static risks and dynamic-unverified ABI/behavior; list remaining compiler/Gradle and differential requirements.
- **Exclusions:** no minor per-method defect sweep where prior evidence exists; no CI; no source implementation/module-boundary changes; no Stage-1 authorization; no claim of exact parity from static signatures.
- **Current result:** audit §§24.62–24.68 maps the base/inherited JS API, actual scope injection, JS-source dispatch/marshalling, 87 same-name base signatures (67 same-shape, 20 non-exact, 17 absent) and the 13 additional inherited Rss/Web/login names against current host objects/ports. Typed action, RPC, fluent AnalyzeRule and login callback gaps remain. This is static evidence, not runtime ABI or behavior proof. No code/test/build/CI changed or run.
- **Next:** consolidate the Stage-0 exit evidence matrix from tasks 006–012, marking each gate met/partial/not met/environment-blocked and linking unique audit evidence. Do not repeat completed API/method audits or run CI.


## 4.13 Runtime-3.0 Stage-0 — exit-evidence reconciliation after JS API closure

- **Task ID:** `R3-S0-EXIT-EVIDENCE-RECONCILIATION-013`
- **Status:** completed as a documentation/evidence-reconciliation slice; Stage-0 itself remains NOT accepted.
- **Start HEAD:** `af4e47ff575c63462388278aef2473e5691dad09`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** update the Stage-0 exit matrix from §24.61 using tasks 006–012 and audit §§24.55–24.68; add explicit typed Host Port and data-owner statuses without repeating prior technical inventories.
- **Allowed files:** audit and overview documentation only. No source, test, Gradle, workflow, schema or UI changes.
- **Dependencies:** overview §§4.6–4.12; audit §§24.50–24.68.
- **Acceptance:** every Stage-0 gate has one current status, unique evidence links, remaining proof and blocker classification; separate static completion, environment-blocked evidence and dynamic-unverified behavior; preserve Stage-0 NOT ACCEPTED and Stage-1 NO-GO.
- **Result:** audit §24.69 reconciles the Stage-0 matrix after the complete base/inherited JS API static slices. It retains partial status for source declarations/callers, transitive caller graph, compiled ABI, typed Host Port wiring and data ownership; confirms Gradle resolution is environment-blocked; keeps provenance/SBOM and reproducible acceptance bundle NOT MET; and marks dynamic behavior as not run/downstream of Stage-0. No exact parity row is established.
- **Disposition:** task 013 complete as a documentation-only reconciliation; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; complete Legado compatibility remains **NOT ESTABLISHED**. No source/test/Gradle/build/CI was changed or run. Commit uses `[skip ci]`; no CI was intentionally triggered.
- **Next:** register a documentation-only source invocation-context and typed Host Port wiring contract slice covering ordinary BookSource rules, payAction, image-click, callBackJs, loginUrl/loginUi and WebView bridge. Reconcile live Book/BookChapter identity, object namespace ownership, callback/result/error/cancellation/threading/lifecycle semantics. Do not repeat the completed 87-name signature or 13-name inherited API inventories; do not begin production implementation, Stage-1 or CI.


## 4.14 Runtime-3.0 Stage-0 — source invocation-context and typed Host Port contract closure

- **Task ID:** `R3-S0-SOURCE-INVOCATION-PORT-CONTRACT-014`
- **Status:** registered; not started.
- **Start HEAD:** `9bc9070d06942288371192427f6127a9bc714a8d`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** close the remaining caller-context and typed host-port contract gaps identified by task 012 and audit §24.69, without repeating the completed base method-signature or 13-name inherited API inventories.
- **Allowed files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and this overview only. Exact pinned upstream and Badcat production/test source may be read for evidence; no source/test/Gradle/workflow/schema/UI edits.
- **Dependencies:** audit §§24.63–24.69; overview §§4.12–4.13; pinned upstream SHA above.
- **Inspection boundary:** distinguish ordinary BookSource rule evaluation, payAction, image-click, callBackJs, loginUrl/loginUi, and WebView page bridge. For each context record exact caller, script source, bindings, bridge object identity, data owner, typed result/error contract, callback lifecycle, cancellation/threading and host-port routing. Trace live Book/BookChapter binding and source/cookie/cache ownership; classify Rss/WebView product UI-only actions separately from source-visible callbacks.
- **Acceptance:** one context-by-context matrix with exact pinned/host path+line evidence; explicit typed Book/BookChapter and callback payload contract; one-to-one required Host Port mapping with production injection status; source/cookie/cache owner and namespace boundaries; explicit exclusions/conditional APIs; remaining dynamic proof and Stage-0/Stage-1 gates. No no-op or same-name Map substitute may be treated as parity.
- **Exclusions:** no source implementation, no schema freeze/migration, no module-boundary change, no CI or build/test/device execution, no repeated API inventory, no Stage-1 authorization, no complete-compatibility claim.
- **Next:** read the registered sources at the pinned SHA and current Badcat HEAD, then append a documentation-only result to audit §24.70 and update this task's disposition. Do not start implementation or CI.


### 4.14.1 Task 014 result

- **Status:** completed as a documentation-only invocation-context/host-port contract slice.
- **Result:** audit §24.70 distinguishes BaseSource.evalJS, AnalyzeRule.evalJS, payAction, image-click, callBackJs, legacy login/Login UI v2 and WebView page RPC. It confirms that upstream `java` is caller-specific (BaseSource, AnalyzeRule or SourceLoginJsExtensions), `source/sourceApi` share BaseSource identity in BaseSource.evalJS, and Book/BookChapter are passed as live typed objects by reading/login callbacks. Current Badcat accepts only string bindings, injects a universal java bridge plus distinct source/cookie/cache wrappers, lacks sourceApi and typed Book, and does not pass SourceExecutionContext into SourceScriptRuntime. Host Ports remain declarations without production factory/runtime dispatch.
- **Disposition:** task 014 complete as static contract evidence; task 012 remains partial; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**. No production/test/Gradle/workflow/schema/UI change, build/test/device execution or CI.
- **Next:** task 015 registered below before further architecture/source inspection.

## 4.15 Runtime-3.0 Stage-0 — minimum authentic typed invocation facade and bridge ownership decision

- **Task ID:** `R3-S0-TYPED-INVOCATION-FACADE-DECISION-015`
- **Status:** registered; not started.
- **Start HEAD:** `72a8c304501648d040e3fad87442a4f56f5694d9`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** turn audit §24.70's invocation matrix into one minimal, non-contradictory architecture contract for a controlled authentic upstream source runtime plus typed Badcat host adapters. Decide which script objects are authentic runtime core, which are per-context compatibility facades, which are Android host ports, and which Legado product UI surfaces are excluded. This is a Stage-0 architecture decision, not implementation authorization.
- **Allowed files:** audit and overview documentation only. Read existing pinned/host source and module/build declarations as evidence; no production/test/Gradle/workflow/schema/UI edits.
- **Dependencies:** audit §§24.55–24.70; overview §§4.6–4.14; exact pinned upstream SHA.
- **Inspection boundary:** reconcile BaseSource/AnalyzeRule/SourceLoginJsExtensions caller-specific `java` ownership; `source/sourceApi/cookie/cache/config` namespace owners; typed Book/BookChapter facade and ScriptBindings values; SourceExecutionContext/HostPort result and cancellation contracts; production factory wiring; authentic upstream core vs Badcat-only UI/Room ownership. Compare A dedicated runtime module, B controlled upstream core extraction and C hybrid using the same source/caller/ABI/Gradle evidence. Treat unavailable resolved Gradle graph as a blocker, not a guess.
- **Acceptance:** one minimal target module/ownership map; per-context bridge object matrix; typed invocation and Host Port responsibilities; explicit no-shadow/no-no-op/no-second-database rules; A/B/C decision with buildability status; exact remaining Stage-0 GO/NO-GO artifacts. No source code changes and no claim of buildability without resolved Gradle evidence.
- **Exclusions:** no implementation, source copy, dependency upgrade, schema freeze/migration, UI work, build/test/device/CI execution, Stage-1 authorization or full-compatibility claim.
- **Next:** read the existing architecture and build-boundary evidence only, then append a documentation-only result to audit §24.71 and update this task's disposition. Do not modify production code or run CI.


### 4.15.1 Task 015 result

- **Status:** completed as a static architecture/ownership decision slice.
- **Decision:** controlled authentic upstream core extraction (B) remains the formal Runtime-3.0 direction; A is a possible physical module layout for B; C is rejected as the main runtime because shadow models/string-map adapters cannot preserve the upstream execution contract.
- **Minimum logical ownership:** authentic source models/orchestration/bridge in the runtime core; pinned Rhino/security/wrapper in :legado-rhino; platform-neutral typed contracts in :integration-contract; Badcat Android ports, catalog/progress persistence and product UI in :app; :legado-runtime-poc remains probe-only. This is a logical target, not a proven exact Gradle source graph.
- **Bridge contract:** per-context BaseSource/AnalyzeRule/SourceLogin java facade; source/sourceApi identity; typed Book/BookChapter; separate cookie/cache owner adapters; separate WebView page RPC; typed Host Port dispatch with caller identity, result/error/cancel/thread/lifecycle semantics.
- **Evidence:** audit §24.71 reconciles current module build declarations with pinned upstream topology and Stage-0 gates. Exact Gradle resolution remains environment-blocked; compiled ABI/R8/DEX, full caller/data-owner closure, license SBOM and reproducible acceptance bundle remain NOT MET.
- **Disposition:** task 015 complete as documentation-only; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; complete Legado compatibility remains **NOT ESTABLISHED**. No production/test/Gradle/workflow/schema/UI changes; no build/test/device execution or CI.
- **Next:** do not duplicate task 007 or trigger CI. Resume its exact-SHA Gradle dependency/variant/packaging closure only when a network-enabled runner with Gradle wrapper and Android SDK is available. Until then, do not freeze module boundaries, begin production source migration or authorize Stage-1.


### 4.7.2 Task 007 — recovery of the original assemble process state (2026-11-25)

- **Recovery:** the Ubuntu runner `VM-0-5-ubuntu` became responsive; no build was relaunched.
- **Identity:** the inspected checkout remains Badcat `b644bef2bac68401804c5807a1ca79ae53a683a6`, with upstream submodule `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Original log:** `/tmp/r3-assembleDebug-with-pinned-jar.log` is 8,034 bytes / 92 lines. Its last Gradle task is `:app:compileDebugKotlin`, followed by Kotlin/Java warnings. There is no `BUILD SUCCESSFUL`, `BUILD FAILED`, failure block, or final exit status. No Gradle assemble/daemon process remained at inspection time.
- **Artifacts/cleanup:** no APK, AAB or DEX was found under `app/build`; only `app/build/outputs/logs/manifest-merger-debug-report.txt` (16,229 bytes) was present. The temporary local HtmlUnit Maven JAR is ABSENT. Git working tree is clean.
- **Disposition:** the second assemble result remains UNKNOWN, not success or failure. Temporary-JAR cleanup is verified; final packaging, APK/DEX, R8 and compiled ABI remain NOT ESTABLISHED. No CI, build retry or source/configuration change was made. Stage-0 remains not accepted; Stage-1 remains NO-GO.
- **Next:** do not rerun merely to replace missing historical exit evidence. Any new exact-SHA packaging/ABI evidence task must be separately registered; no production migration or Stage-1.


### 4.7.3 Task 007 — exact-SHA debug compile-classpath resolution (2026-11-25)

- **Command:** `:app:dependencies --configuration debugCompileClasspath --console=plain` on the exact tested checkout `b644bef2bac68401804c5807a1ca79ae53a683a6` and pinned upstream submodule `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Result:** exit code 0; `BUILD SUCCESSFUL in 19s`. The `/debug` compile classpath report resolved successfully.
- **Evidence boundary:** together with the earlier successful `debugRuntimeClasspath` report, two debug dependency configurations are now resolved. This does not establish all variants, final APK packaging, R8/DEX or compiled ABI. No build retry, source/configuration change, test or CI was performed. Task 007 remains partial; Stage-0 not accepted; Stage-1 NO-GO.


## 4.16 Runtime-3.0 Stage-0 — existing compiled output and ABI evidence inventory

- **Task ID:** `R3-S0-COMPILED-OUTPUT-ABI-INVENTORY-016`
- **Status:** completed as a read-only inventory; see §4.16.1.
- **Start HEAD:** `432a5e8cadc5cb250460cb1d980c64636f22447e`; branch `integration/legado-upstream-runtime`; evidence checkout Badcat `b644bef2bac68401804c5807a1ca79ae53a683a6`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** inspect existing compiled outputs from the earlier exact-SHA runner attempt and extract bounded compiled ABI/signature evidence without rebuilding.
- **Allowed files:** audit and overview documentation only; runner access read-only.
- **Acceptance:** per-module class/JAR inventory, selected `javap` signature evidence and artifact hashes where available, explicit distinction from AAR/APK/DEX/R8/final build success, and a list of unmet gates.
- **Exclusions:** no build/assemble/test/CI rerun; no production/Gradle/configuration changes; no Stage-1 or complete-compatibility claim.
- **Next:** inspect existing artifacts read-only and record the result in audit §24.75 and this section's result entry.


### 4.7.4 Task 016 result — existing compiled output and JVM signature inventory

- **Identity:** read-only inspection of Badcat `b644bef2bac68401804c5807a1ca79ae53a683a6` and upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Outputs:** `:app` 314 class files; `:legado-runtime` 29; `:legado-rhino` 67; `:integration-contract` 265. Compile JARs exist for `:legado-runtime`, `:legado-rhino` and `:integration-contract`; no AAR/APK/DEX was found. App has only an R resource JAR, not a packaged app artifact.
- **Hashes:** runtime classes JAR `9994ba1487a733f4ba6eb0c5bdca572b0d964cab07e8e284e3fe01982e69cabc`; Rhino classes JAR `99b1a8bf65d8b7c3deba359f08f740237185aaf45da81a4376e1fc311b012d24`; integration-contract JAR `6ffcd72123c7f8de01947d1a9cb1f406e08f0c33a2f8c9dbfd4838f5fcdbd17c`.
- **JVM signatures:** `javap -public` confirmed representative compiled signatures for upstream `SearchRule`, `RhinoScriptEngine` and Badcat `SourceScriptRuntime`. The latter still exposes `evaluate(BookSource, String, Map<String,String>)`, evidence of the narrow string-map facade rather than the full typed upstream invocation contract. Full signatures and hashes are in audit §24.75.1.
- **Boundary:** class outputs and selected signatures are not equivalent to successful app compile task, AAR/APK packaging, R8/DEX, upstream ABI parity or runtime compatibility. Existing class timestamps correlate with the earlier build attempt, but the missing terminal Gradle status prevents declaring that task successful.
- **Disposition:** Task 016 complete as a read-only inventory. Task 007 partial; Stage-0 not accepted; Stage-1 NO-GO. No build/test/CI rerun or source/configuration change.
- **Next:** use the evidence to close the remaining authentic upstream source/dependency and typed host-port gaps; do not start Stage-1 or production migration.


## 4.17 Runtime-3.0 Stage-0 — fresh exact-HEAD Debug packaging verification

- **Task ID:** `R3-S0-EXACT-HEAD-DEBUG-PACKAGING-017`
- **Status:** cancelled before successful packaging; see §4.17.4.
- **Start HEAD:** `519d6c9d74401a1072d97457475d79cd2c59684c`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** run one fresh `:app:assembleDebug` against the exact registered HEAD and capture terminal status plus APK/manifest/DEX/R8 evidence.
- **Allowed files:** audit and overview documentation only; runner build outputs and ephemeral exact-SHA Maven artifact are permitted.
- **Acceptance:** exact checkout identity, complete build log and exit code, APK hash/size if created, packaging/DEX evidence, temporary artifact cleanup and clean Git status.
- **Exclusions:** no CI, source/test/Gradle/workflow/schema/UI edits, dependency upgrade, Stage-1 or complete-compatibility claim.
- **Next:** verify runner fast-forward and upstream pin, then execute one logged assemble attempt.


### 4.17.1 Task 017 execution checkpoint — Debug assemble pending

- **Exact test HEAD:** 1855cd77cb01efc13e59e119d3392027906d179e; upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Toolchain:** JDK 21.0.13.1 / Gradle 8.14.4 / Android API 36 / Build Tools 36.0.0.
- **Execution:** :app:assembleDebug --no-daemon --console=plain; log /tmp/r3-task017-assembleDebug.log. Pinned upstream HtmlUnit JAR copied temporarily and its expected SHA-256 verified; shell EXIT cleanup trap is active.
- **Last observed tasks:** app Kotlin compile, duplicate-class check and :app:mergeExtDexDebug. Gradle process PID 432816 remained pending; no APK was present at last inspection. Final exit, packaging and cleanup are not yet known.
- **Disposition:** task 017 remains in progress. No duplicate build, CI, or source/configuration edit.


### 4.17.2 Task 017 recovery checkpoint — runner calls timed out

- GitHub branch HEAD verified as `eddd93fc70ff9ee628b432fb285ab01eba2a2ea9`; required workflow, overview and audit reread.
- Runner `VM-0-5-ubuntu` reported Online, but heartbeat was 2 minutes old. All six outstanding Task 017 process/log/marker/process-list calls subsequently returned terminal device timeouts.
- These timeouts do not reveal the Gradle process's final state. Task 017 remains in progress with exit code, APK/manifest/DEX/R8 and temp-JAR cleanup unknown.
- No duplicate build, process termination, CI, or source/configuration change. Next action is read-only recovery after the runner responds.


### 4.17.3 Task 017 recovery result — bounded evidence, terminal status unknown

- Runner recovered. Exact checkout: Badcat `1855cd77cb01efc13e59e119d3392027906d179e`; upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`; Git working tree clean.
- Original log: 4,597 bytes / 87 lines; last task `:app:mergeExtDexDebug`; no terminal Gradle status or exit code. Original assemble shell/CLI/daemon PIDs are absent; a separate Gradle daemon remains but is not evidence of Task 017 completion.
- No APK/AAB/DEX/R8 mapping under `app/build`. Merged manifest intermediates (3 × 3,774 bytes) and the 16,229-byte manifest merger report exist; these do not prove APK packaging.
- Cleanup marker reads `ABSENT`, and the temporary HtmlUnit JAR is absent. Cleanup is verified.
- Task 017 remains partial / result unknown. No duplicate build, CI, process termination, or source/configuration edit. Stage-0 not accepted; Stage-1 NO-GO.


## 4.17.4 Task 017 daemon cancellation evidence

- The original Gradle daemon log confirms: client disconnection was detected and the build was cancelled; the daemon then stopped. The Gradle client exit code remains unavailable.
- The packaged manifest intermediate identifies `app.badcat.client`, version `0.1.0-integration`, minSdk 28 and targetSdk 36. This is not a packaged APK.
- No APK/AAB/DEX/R8 mapping exists in the inspected `app/build` tree. Temporary JAR cleanup remains verified (`ABSENT`).
- Task 017 remains cancelled before successful packaging / partial evidence. No retry or CI. Stage-0 not accepted; Stage-1 **NO-GO**.


## 4.18 Runtime-3.0 Stage-0 — resilient exact-HEAD Debug packaging verification

- **Task ID:** `R3-S0-RESILIENT-DEBUG-PACKAGING-018`
- **Status:** completed as bounded exact-HEAD Debug packaging evidence; see §4.18.1.
- **Registration start HEAD:** `424207fb4b441eb7a0e416ded180425fe434574c`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Execution snapshot:** the exact branch HEAD after both Task 018 registration entries are committed; record and verify that SHA, branch ref and upstream submodule before launching any process.
- **Goal:** obtain one terminally observable `:app:assembleDebug` result on that immutable execution snapshot. Run the launcher fully detached from the Remote Desktop Commander request so a tool-session/client disconnect cannot cancel the Gradle build; capture the complete log, final Gradle exit code, cleanup status and any APK/manifest/DEX/R8 evidence.
- **Allowed repository files:** this overview and the Stage-0 architecture audit only. On the runner, use the existing exact-SHA checkout after a verified fast-forward/fetch; ephemeral launcher/log/result files under `/tmp`, Gradle outputs and one temporary exact-upstream Maven JAR are allowed. No persistent build-environment or system configuration edits.
- **Dependencies:** Task 007 §§24.72–24.74; Task 016 §24.75.1; Task 017 §24.76.3–§24.76.4; fixed upstream SHA.
- **Preflight:** verify clean checkout and exact execution SHA, exact upstream submodule, Gradle/JDK/Android SDK/Build Tools, available disk/RAM/swap, and absence of another active assemble CLI. Do not kill the idle Gradle daemon or change system packages/network settings. If any prerequisite is not met, stop before launch.
- **Execution boundary:** use a detached `nohup`/new-session launcher with stdin disconnected and stdout/stderr redirected to stable files; the launcher itself must write an EXIT result marker containing exit code and timestamps after removing the temporary JAR and writing an explicit cleanup marker. Use a bounded Gradle worker count; run exactly one assemble attempt. Poll only the detached PID, log and markers; never start a duplicate while its state is pending.
- **Pinned temporary input:** copy the HtmlUnit CoreJS JAR only from the exact upstream submodule and verify SHA-256 `d720f34285515025e4ccc80a5b92aed42cb26c03a7715ca53583d2c74ae51df7`; EXIT cleanup must remove it on both success and failure.
- **Acceptance:** record execution SHA and clean Git status; toolchain versions; exact command; detached launcher PID; complete terminal log hash/size; explicit Gradle exit code and terminal outcome; APK hash/size if present; merged manifest identity; DEX/R8 outputs if present; temporary-JAR cleanup marker and absent path; post-run Git status. A successful assemble is only bounded packaging evidence, not full compatibility or Stage-0 acceptance.
- **Exclusions:** no CI/GitHub Actions; no production/test/Gradle/workflow/schema/UI/source changes; no dependency upgrades; no system apt/sudo/network changes; no Stage-1 authorization, module-boundary freeze or complete-compatibility claim.
- **Stop conditions:** wrong branch/SHA/submodule, dirty checkout, missing SDK/toolchain, insufficient runner capacity, active competing build, missing/hash-mismatched pinned JAR, inability to create a detached launcher/result marker, or unverified cleanup. Stop without launching or duplicating work.
- **Next:** do not repeat the assemble under Task 018. Use §4.18.1's bounded APK/DEX evidence while continuing only the still-open Stage-0 caller/data-owner, license/SBOM, reproducibility and behavioral gates; Stage-0 remains not accepted and Stage-1 NO-GO.


### 4.18.1 Task 018 result — detached exact-HEAD Debug packaging succeeded (2026-11-25)

- **Identity:** runner `VM-0-5-ubuntu`; checkout `/root/stage0-runner/repo-git`, detached at Badcat `4c9cf8ca41a29fcfacdb17fb3ec1d66d4b451343`; upstream submodule `330e606c4d57e08adc056ff8ab7a4132c784da9a`; clean before and after.
- **Toolchain:** Microsoft OpenJDK `21.0.13.1`; Gradle `8.14.4`; Android platform API `36`; Build Tools `36.0.0`.
- **Detached execution:** launcher PID `442062`, new session `442062`; Gradle CLI PID `442145`; single-use daemon PID `442181`. The build ran independently of the remote tool request and reached its own terminal marker. Start `2026-11-25T07:05:15+08:00`; finish `2026-11-25T07:08:33+08:00`.
- **Command:** `gradle -Dorg.gradle.jvmargs="-Xmx1g -Dfile.encoding=UTF-8" --no-daemon --max-workers=1 --no-parallel :app:assembleDebug --console=plain`.
- **Pinned input:** temporary HtmlUnit CoreJS JAR copied from the exact upstream submodule; source and copy both matched SHA-256 `d720f34285515025e4ccc80a5b92aed42cb26c03a7715ca53583d2c74ae51df7`. EXIT cleanup marker reports `ABSENT`; temporary path is absent.
- **Terminal result:** explicit result marker records Gradle exit code `0`; log ends with `BUILD SUCCESSFUL in 3m 16s`, `82 actionable tasks: 23 executed, 59 up-to-date`. Complete log: 157 lines / 11,031 bytes; SHA-256 `bc6e773718ef3b19b3620961278cd6dc3c412e1ad6bc7509820a52b43acc8d4f`.
- **APK:** `app/build/outputs/apk/debug/app-debug.apk`; 32,073,886 bytes; SHA-256 `c0b5227165944da96db2c592dff3fc0543f82da01cdd72d7cb3121d573ead7d2`. `aapt dump badging` reports package `app.badcat.client`, versionCode `1`, versionName `0.1.0-integration`, minSdk `28`, targetSdk/compileSdk `36`, application label `坏坏猫`, launch activity `app.badcat.client.MainActivity`, and debuggable=true. `apksigner verify --verbose` succeeds with APK Signature Scheme v2 (v1/v3/v3.1/v4 not present).
- **DEX/native packaging:** APK contains 14 DEX entries (`classes.dex` through `classes14.dex`) and `libarchive-jni.so` for `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`. Intermediate DEX files are present under `app/build/intermediates/dex/debug`.
- **Manifest/report hashes:** packaged manifest intermediate SHA-256 `815823deb697a5bdb26e42ea3267dc0e1a3de3b13bf45609a6dfa04e77e4d9be`; manifest merger report SHA-256 `d7462a5dcd6e9da5f35f0a2f8382382d3bffce5a226606915701b9a73fb62e92`; output metadata SHA-256 `c6a944fb73f38dc0bd33cbf3728bbd047cb839fc82d66dc5c70ca5dca49454a3`.
- **Boundaries:** Gradle reused 59 UP-TO-DATE tasks, so this was an incremental exact-HEAD assemble, not a clean-build/reproducibility proof. No AAB or R8 mapping file was produced/found; this build did not run unit/instrumentation tests, install the APK, exercise a device, or prove runtime/ABI/full Legado behavior. `libarchive-jni.so` could not be stripped and was packaged unchanged (Gradle warning). Peak observed memory pressure was high (available RAM briefly ~96 MiB; swap use peaked around 1.7 GiB), but the process completed; no OOM-killer entry was observed in the bounded check.
- **Disposition:** Task 018 complete as a successful, exact-SHA Debug packaging evidence task only. The result proves the current snapshot can produce a signed debug APK and DEX packaging; it does not satisfy full Stage-0 architecture, caller/data-owner, license/SBOM, reproducibility or behavioral gates. Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; full Legado compatibility remains **NOT ESTABLISHED**. No CI, tests, production source, Gradle/configuration, workflow, schema or UI changes occurred.


## 4.19 Runtime-3.0 Stage-0 — pinned source provenance, license and SBOM closure

- **Task ID:** `R3-S0-LICENSE-SBOM-PROVENANCE-019`
- **Status:** registered; not started.
- **Registration start HEAD:** `780fdce7b7a58539aec5f51846a6b2b40d678283`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** establish a traceable source/dependency provenance and license inventory for the Badcat snapshot and the pinned Legado runtime closure; identify whether the current project can produce a complete, accurate SBOM and required notices for a future distributable build.
- **Execution snapshot:** exact branch HEAD after both Task 019 registration entries are committed; record and verify the immutable SHA, branch ref and upstream submodule before inspection.
- **Allowed repository files:** this overview and `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` only. Source/build/license files may be read but not edited. Ephemeral reports/logs under `/tmp` are allowed.
- **Dependencies:** fixed Legado SHA; Task 015 module/ownership decision (§24.71); Task 018 exact-SHA APK/DEX evidence (§24.77.1).
- **Inspection scope:** upstream and Badcat LICENSE/COPYING/NOTICE headers and file-level source origins; Gradle module/plugin/repository/dependency declarations; direct and transitive dependency graphs for Debug compile/runtime configurations; local Maven artifacts and hashes; native libraries packaged in Task 018 APK; available POM/module license metadata, notices and attribution files; GPL-3.0 source/distribution obligations and gaps. Record dependency coordinates, resolved versions, origin, scope, license evidence and whether the item is actually packaged or merely declared.
- **Execution boundary:** read-only source/configuration inspection; Gradle built-in dependency-report queries may be run on the registered exact snapshot with offline mode and bounded workers. Do not invoke CI, build/assemble, tests, plugin/license generation, network installation, dependency updates or source edits. Do not infer a license from artifact name alone; mark missing/ambiguous evidence explicitly.
- **Acceptance:** exact snapshot identity; source provenance map; direct/transitive dependency inventory by configuration; local artifact/native-library provenance and hashes; license evidence links/paths with confidence; notices/attribution inventory; GPL-3.0 obligations and unresolved legal/technical questions; a machine-readable SBOM status (available / partial / absent) without claiming a complete SBOM unless every packaged component is covered; clear blockers and next action.
- **Exclusions:** no legal opinion; no claim of license compliance solely from a declared SPDX identifier; no release APK/AAB, no packaging rerun, no CI/Actions, no source/test/Gradle/workflow/schema/UI changes, no dependency upgrades or new tooling installs; no Stage-1 authorization.
- **Stop conditions:** branch/SHA/submodule mismatch, dirty checkout, dependency report requiring unavailable network artifacts, missing source provenance that cannot be resolved from pinned materials, or any need to change production/build configuration. Stop and record the blocker rather than guessing.
- **Next:** fast-forward the clean runner checkout to the registered exact HEAD, verify the upstream pin, inspect source/license files and Gradle declarations, then capture offline Debug compile/runtime dependency reports and reconcile the Task 018 packaged native/DEX evidence. Stage-0 remains not accepted and Stage-1 NO-GO.


### 4.19.1 Task 019 result — bounded source provenance, license and dependency inventory (2026-11-25)

- **Snapshot boundary:** registered branch snapshot `4eef7da9a566e34f3a43e41e26dcaf63b841ff97`; runner inspection checkout `4c9cf8ca41a29fcfacdb17fb3ec1d66d4b451343`; upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`. GitHub compare reports the ten commits between the inspection checkout and registered snapshot changed only the two registered documentation files, so source/build inputs are identical; the runner itself was not checked out to the documentation-only tip because its fetch request stalled. Runner working tree remained clean.
- **Dependency reports:** offline `:app:dependencies` reports for `debugCompileClasspath` and `debugRuntimeClasspath` both completed with exit code 0 (20s and 16s respectively), using JDK 21.0.13.1, Gradle 8.14.4, one worker and a 512 MiB single-use daemon. Combined log: 205 lines / 11,683 bytes; SHA-256 `724daacd532e28e2f805f7018f5799d6a8724fea2d297b133107a9887ab9e5e1`. Terminal result marker SHA-256 `d96234cda73723a5b3a5815994e155713236152d59e77ed7f7d0ef8ca3d49167`. These are dependency graph reports only, not artifact-file resolution or an SBOM.
- **Resolved runtime graph:** includes the three project modules `:integration-contract`, `:legado-runtime`, `:legado-rhino`; Gson, Flexbox, Hutool crypto/core, Bouncy Castle provider/PKIX/util, Jsoup, JsoupXpath, JsonPath/json-smart/accessors-smart/ASM, AndroidX annotation/collection/startup/tracing, Kotlin/annotations, coroutines, OkHttp/Okio, ANTLR, Commons Lang, SLF4J, quick-transfer-core, libarchive AAR and the custom HtmlUnit CoreJS fork. Gradle selected newer versions for several transitive requests (e.g. Jsoup 1.15.3 → 1.23.2, SLF4J 1.7.36 → 2.0.17, Kotlin stdlib 1.4.x/2.1.x/2.2.x → 2.4.20, AndroidX annotation requests → 1.10.0).
- **License evidence from POMs/parent metadata:** AndroidX, Flexbox, Gson, Error Prone annotations, JsonPath, JSON-smart/accessors-smart, Kotlin, coroutines, OkHttp/Okio, JSpecify and related modules declare Apache-2.0; Jsoup is MIT; JsoupXpath is Apache-2.0; Hutool 5.8.22 inherits Mulan Permissive Software License 2.0; quick-transfer-core 0.2.17 inherits Apache-2.0; Commons Lang 3.12.0 inherits Apache-2.0; ANTLR 4.7.2 inherits BSD; SLF4J 2.0.17 inherits MIT; ASM 9.7.1 declares BSD-3-Clause. Bouncy Castle POMs name the Bouncy Castle Licence and the packaged JAR contains an MIT-style `META-INF/LICENSE.md`; retain both records for reconciliation. These findings are metadata/source evidence, not a legal opinion.
- **Legado/Rhino provenance:** upstream Legado is GPL-3.0; `third_party/legado/LICENSE` and `third_party/legado-rhino/LICENSE` match the upstream GPL text. All 11 app rule-model/Gson source files listed in `THIRD_PARTY_NOTICES.md` compare byte-for-byte with the pinned upstream. Local Rhino contains the 30 upstream module files plus local `LegadoRhinoSandbox.kt`; only upstream `RhinoScriptEngine.kt` differs. The crypto files `AsymmetricCrypto.kt`, `SymmetricCryptoAndroid.kt`, `Sign.kt` and `EncoderUtils.kt` differ from upstream; the extracted local `String.isHex()` helper retains the upstream predicate. In `integration-contract/src/pinnedLegado`, `AnalyzeByJSonPath.kt` matches upstream, `AnalyzeByXPath.kt` differs, `PrintOnDebug.kt` has no same-path upstream file, and the three `PinnedLegado*Bridge.kt` files are local adapters. `THIRD_PARTY_NOTICES.md` does not currently enumerate these analyzer sources or enumerate the changed-file list, so source modification attribution is incomplete.
- **HtmlUnit custom fork:** exact upstream JAR remains in the pinned submodule only (1,900,654 bytes; SHA-256 `d720f34285515025e4ccc80a5b92aed42cb26c03a7715ca53583d2c74ae51df7`); the Badcat local Maven directory contains only POM/README, not the JAR. Its source record identifies packaging revision `3eb5071cdca4a357119d1063a53d5f2d47984ed2` and Rhino fork `76460c0312bfd351df6f2bb11168102cdb54170a`; engine MPL-2.0, packaging Apache-2.0 and additional V8/Sun notices are retained in `third_party/htmlunit-core-js-5.3.0-legado.4/`. Task 018's temporary copy was cleaned, so a fresh checkout cannot reproduce that assemble without an explicit artifact-staging mechanism.
- **Native library:** `me.zhanghai.android.libarchive:library:1.1.6` AAR is 3,980,193 bytes, SHA-256 `06d97a37872a9574ceb2bf94a3ad8dc38fcc89a60f2424067b20eb5efaa62189`; its POM declares Apache-2.0, but the AAR contains four `libarchive-jni.so` binaries and no license/notice file. The underlying native libarchive source/license has not been verified and must not be inferred from the wrapper POM. APK native hashes: arm64-v8a `92332de41e661c2a5ce05df65c8a0a6367a99a2cb201e3b3eec7039c9cbc669e`; armeabi-v7a `d8168c8e2e82fd7795ba8e0631fd3ca6a7614dec36e9e168c77eac0e38628cb9`; x86 `dd296b72e008aa9821a9bdb30627246fcf8d795016ec871b64404d5829fb6258`; x86_64 `b7ba663212c52c05f725e715dc635305dd641c526d770c2bf405aa289e377910`.
- **APK notice coverage:** Task 018 APK SHA-256 remains `c0b5227165944da96db2c592dff3fc0543f82da01cdd72d7cb3121d573ead7d2`. It contains exactly four license/notice paths: Bouncy Castle `META-INF/LICENSE.md`, AndroidX Annotation and Collection Apache license files, and Jsoup `META-INF/jsoup/LICENSE`. No Legado GPL license, `THIRD_PARTY_NOTICES.md`, HtmlUnit MPL/Apache/V8/Sun notices, or upstream `assets/licenses/` tree is present in the APK. The upstream submodule does contain those asset files, but app asset configuration does not include that tree. The repository root has no `LICENSE`, `COPYING` or `NOTICE` file declaring the Badcat project's own license.
- **SBOM disposition:** no SPDX/CycloneDX/SBOM file or generator/plugin was found. The manual notice index is partial; dependency POMs cover many artifacts, but native libarchive source licensing, some file-level provenance/modification notices, packaged GPL/HtmlUnit notices, and the Badcat project's own license remain unresolved. **SBOM status: ABSENT as a complete machine-readable artifact; manual provenance inventory: PARTIAL.**
- **Task disposition:** Task 019 is complete as a bounded read-only inventory only. No source/build configuration, notice, asset, test or CI files were changed; no build/assemble/test/CI was run. The APK license gaps and missing custom JAR in the Badcat Maven directory are release/reproducibility blockers, not a legal-compliance conclusion. Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; full Legado compatibility remains **NOT ESTABLISHED**.
- **Next:** register a separate, bounded remediation task before changing notice files or packaging configuration; separately continue the remaining Stage-0 caller/data-owner, reproducibility and behavioral evidence. Do not begin Stage-1 or claim release readiness.


## 4.20 Runtime-3.0 Stage-0 — exact Task 018 APK DEX and compiled runtime class inventory

- **Task ID:** `R3-S0-APK-DEX-ABI-CLOSURE-020`
- **Status:** registered; not started.
- **Registration start branch HEAD:** `db0afc0b6c10ec146cbceb303b25dd6b1d531dd6`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Artifact under inspection:** existing Task 018 APK from Badcat exact execution HEAD `4c9cf8ca41a29fcfacdb17fb3ec1d66d4b451343`, SHA-256 `c0b5227165944da96db2c592dff3fc0543f82da01cdd72d7cb3121d573ead7d2`; upstream submodule `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** inspect the 14 DEX files already packaged in the Task 018 APK and compare actual defined class descriptors against the behavior-bearing pinned Legado source/runtime declarations. Establish which upstream model, analyzer, BaseSource, JsExtensions and bridge classes are truly packaged versus absent; distinguish DEX presence from ABI parity and runtime behavior.
- **Allowed repository files:** this overview and the Stage-0 architecture audit only. APK/DEX/source reads are read-only; temporary extraction and reports under `/tmp` are allowed. The APK hash must be verified before inspection.
- **Dependencies:** Task 016 selected JVM class/JAR inventory (§24.75.1); Task 018 exact APK/14-DEX packaging evidence (§24.77.1); Task 019 source provenance inventory (§24.78.1); pinned upstream SHA.
- **Inspection plan:** (1) verify APK hash and 14 DEX entry names; (2) use only tools already installed on the runner (e.g. `apkanalyzer`, `dexdump`, `baksmali` if present) to inventory class definitions/descriptors; (3) derive the comparison target class set from exact pinned upstream source declarations/callers, not guessed names; (4) check presence/absence and package origin for representative models, BaseSource, AnalyzeRule/AnalyzeUrl, JsExtensions/inherited extension groups, SharedJsScope/JsSourceEngine, host adapters and Rhino; (5) inspect whether minification/obfuscation is evidenced and whether R8 mapping exists; (6) hash the extracted DEX and final reports; (7) reconcile with Task 016 JVM outputs and the Stage-0 gate matrix.
- **Acceptance:** exact APK/Badcat/upstream identities; per-DEX entry list and class counts; reproducible class-descriptor inventory; pinned source declaration → DEX class mapping for each selected behavior-bearing group; explicit absent/unmatched classes; R8/mapping status; tool versions and report hashes; limitations (class presence is not semantic or dynamic compatibility).
- **Execution boundary:** read-only inspection of the existing APK and pinned source only. No assemble/build/test, APK installation, device execution, CI/Actions, dependency resolution, tool installation, source/configuration/notice/asset/schema/UI edits or process termination.
- **Stop conditions:** APK hash mismatch, unavailable installed DEX inspection tools, corrupted/missing DEX entry, source snapshot mismatch, or need to install tools/rebuild the APK. Stop and record NOT ESTABLISHED rather than substituting guesses.
- **Exclusions:** no new APK; no R8 or release build; no Stage-1 authorization; no full ABI/behavior parity claim; no changes to the Task 018 APK.
- **Next:** verify the registered documentation HEAD and immutable Task 018 APK hash, inspect the DEX files read-only, then append the exact inventory and gate disposition. Stage-0 remains not accepted and Stage-1 NO-GO.


### 4.20.1 Task 020 result — exact APK DEX inventory and missing runtime closure (2026-11-25)

- **Artifact identity:** Task 018 APK SHA-256 `c0b5227165944da96db2c592dff3fc0543f82da01cdd72d7cb3121d573ead7d2`, size 32,073,886 bytes; Badcat artifact source commit `4c9cf8ca41a29fcfacdb17fb3ec1d66d4b451343`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`. GitHub compare from the artifact source commit to registered Task 020 branch tip `86fae25009afb2971b21c705ca7a01621c6190d9` shows only the two documentation files changed; the APK was not rebuilt or modified.
- **Tooling:** Android SDK Build Tools 36.0.0 `dexdump`; installed `apkanalyzer`; `baksmali 2.5.2`. No tool installation. All 14 extracted DEX files passed `dexdump -c` checksum verification.
- **DEX totals:** 14 entries (`classes.dex` through `classes14.dex`); 16,531 unique defined class names in the merged APK inventory. Per-DEX class-definition counts sum to 16,569 because 38 class names are each defined twice across separate DEX files. Package report: 168,207 lines / 17,423,719 bytes; SHA-256 `b792971a4cc6b40f6c13a2e12afc26a09e88605474fe580adabd736b4c22f379`. Unique class list SHA-256 `b7c22e70dd65afa363af4ec8a4118428f538431e842285cd9705598d9284ba02`. Per-DEX inventory SHA-256 `2cd590df155aff699cdb3c5dd4f4cbb54de4d6437f3e01596d282ea2d5916447`. Duplicate class list: 38 entries; SHA-256 `fcaea3380162290d0b2a960e261798f6f46f321546fa4051bc75e77350f9cb65`.
- **Per-DEX class counts:** `classes.dex` 7,244; `classes2.dex` 14; `classes3.dex` 3; `classes4.dex` 327; `classes5.dex` 50; `classes6.dex` 46; `classes7.dex` 104; `classes8.dex` 12; `classes9.dex` 15; `classes10.dex` 46; `classes11.dex` 284; `classes12.dex` 513; `classes13.dex` 8; `classes14.dex` 7,903. Every DEX's byte size and SHA-256 are preserved in the Task 020 temporary report `/tmp/r3-task020/per-dex.txt`.
- **Present source slices:** seven upstream rule-model types under `io.legado.app.data.entities.rule`; selected parser/evaluator types under `io.legado.app.model.analyzeRule` (including AnalyzeByJSoup/Regex/XPath/JSonPath, RuleAnalyzer, SourceHtmlParser and local PinnedLegado/UpstreamLegado adapters); Rhino `com.script.rhino.RhinoScriptEngine` and `LegadoRhinoSandbox`; Badcat `app.badcat.engine.BookSource`, `BookChapter`, `SourceScriptRuntime`; and Badcat `RhinoSourceScriptRuntime` / `RhinoSafeSourceJavaExtensions`.
- **Absent exact upstream behavior-bearing declarations:** `io.legado.app.data.entities.BaseSource`, `io.legado.app.data.entities.BookSource`, `io.legado.app.data.entities.BookChapter`, `io.legado.app.model.analyzeRule.AnalyzeRule`, `io.legado.app.model.analyzeRule.AnalyzeUrl`, `io.legado.app.help.JsExtensions`, `io.legado.app.ui.rss.read.RssJsExtensions`, `io.legado.app.help.webView.WebJsExtensions`, `io.legado.app.ui.login.SourceLoginJsExtensions`, `io.legado.app.model.SharedJsScope` and `io.legado.app.model.jsSource.JsSourceEngine`. The similarly named `app.badcat.engine.BookSource/BookChapter` are host-owned DTOs, not those upstream types. Parser presence does not supply the missing authentic source execution sequence.
- **Confirmed duplicate DEX definitions:** 38 class names occur exactly twice. They comprise seven rule-model class families (including synthetic/Companion/Creator classes) and eight `GsonExtensionsKt` helper classes. `baksmali list classes` and `dexdump` independently confirm, for example, `SearchRule`/related classes in both `classes5.dex` and `classes10.dex`, and `GsonExtensionsKt`/related classes in both `classes2.dex` and `classes9.dex`. The source/build boundary explains the overlap: app default sources contain copies under `app/src/main/java/io/legado/app/data/entities/rule/` and `app/src/main/java/io/legado/app/utils/GsonExtensions.kt`, while `:legado-runtime` separately adds the same seven upstream rule model files and GsonExtensions from the pinned submodule. Both modules produce class files. Representative `javap -p -s` text for SearchRule, BookListRule and GsonExtensionsKt is identical across app/runtime outputs, but their class-file hashes differ. This is a confirmed duplicate-definition packaging anomaly; its runtime class-loader effect was not tested and is not asserted.
- **R8:** no `mapping.txt`, `seeds.txt` or `usage.txt` found under app outputs. DEX class names remain readable, but absence of mapping output alone does not prove all release/minification configuration.
- **Task disposition:** Task 020 is complete as a read-only DEX/class inventory. It confirms that Task 018's successful assemble packages only a partial Legado source slice, omits the authentic BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions/SharedJsScope/JsSourceEngine core, and contains duplicate DEX class definitions caused by overlapping app/runtime source ownership. No source/build/test/CI/APK changes, installation or device execution occurred. Compiled ABI and runtime semantic parity remain NOT ESTABLISHED; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; full compatibility remains **NOT ESTABLISHED**.
- **Next:** register a separate source-ownership/duplicate-class resolution task before changing module source sets or production code. Do not treat the existing APK as a Stage-0 or release acceptance artifact.


### 4.20.2 Stage-0 gate delta after Tasks 018–020

| Gate | Current evidence | Disposition |
|---|---|---|
| Architecture direction | Task 015 selected fixed-SHA authentic Legado core plus typed Badcat Host Ports; preserve Badcat product shell | **Decision closed; implementation gate still separate** |
| Debug build/packaging | Task 018 `:app:assembleDebug` exit 0, but Task 020 found 38 duplicate class definitions across DEX and absent upstream runtime core classes | **Partial; not accepted** |
| Dependency graph | Task 019 offline Debug compile/runtime reports both exit 0 | **Partial; artifact-file and full build closure not established** |
| Source/runtime closure | Task 020 confirms parser/rule-model/Rhino slice, but BaseSource, upstream BookSource/BookChapter, AnalyzeRule/AnalyzeUrl, JsExtensions, SharedJsScope and JsSourceEngine are absent from APK | **Not met** |
| DEX/ABI | 14 DEX checksum-valid; 16,531 unique classes; 38 duplicate definitions; no R8 mapping; no full ABI inventory | **Partial; not accepted** |
| Caller/data ownership | Static maps and selected adapters exist; authentic upstream caller lifecycle and data-owner wiring are not present/verified in DEX | **Partial / not met** |
| License/SBOM | Task 019 manual inventory partial; APK lacks GPL/HtmlUnit notices; native libarchive license unresolved; machine-readable SBOM absent | **Not met** |
| Reproducibility | Task 018 required temporary manual staging of the pinned HtmlUnit JAR; no clean reproducible build or stable artifact bundle | **Not met** |
| Behavioral parity | No end-to-end upstream source execution or differential behavior suite established | **Not established** |

**Decision:** Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**. A successful assemble is now explicitly bounded by the duplicate DEX definitions and missing authentic upstream runtime classes. The next technical blocker is to define one canonical source owner for the duplicated rule/Gson classes and a reviewed path for the missing authentic runtime closure; no source-set or production changes are authorized by this reconciliation.


## 4.21 Runtime-3.0 Stage-0 — canonical source ownership and authentic runtime closure plan

- **Task ID:** `R3-S0-SOURCE-OWNERSHIP-CLOSURE-PLAN-021`
- **Status:** registered; not started.
- **Start branch HEAD:** `534e18a6cb95f5f98b0806039a15a1057a469c9f`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** resolve the next Stage-0 architecture blocker exposed by Task 020: identify one canonical owner for all 38 duplicated DEX class definitions (seven upstream rule-model families and eight Gson helper families, including generated/nested classes), and produce a reviewed source/dependency/caller closure plan for the absent authentic Legado runtime declarations. This is a read-only design/evidence task; it does not authorize source-set or production changes.
- **Allowed repository files:** this overview and `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` only. Source/build declarations and callers may be inspected read-only at the current Badcat source tree and pinned upstream SHA. Temporary analysis output may be kept under `/tmp`.
- **Dependencies:** Task 015 architecture decision (audit §24.71); Task 016 compiled class/JVM ABI inventory (audit §24.75.1); Task 018 APK packaging evidence (audit §24.77.1); Task 019 provenance/license inventory (audit §24.78.1); Task 020 DEX duplicate/missing-class evidence (audit §24.79.1–§24.79.2); fixed upstream SHA.
- **Inspection boundary:**
  1. Enumerate the exact 38 duplicated DEX class names and map each to its source file(s), Gradle source-set inclusion(s), producing module(s), and class-file hashes. Distinguish top-level declarations from generated Companion/Creator/synthetic classes.
  2. Trace every production/test/build reference to the duplicated rule-model and Gson helper families; identify which module currently consumes each family and whether app-local copies are required by app-only callers.
  3. Compare candidate canonical ownership options against the selected Runtime-3.0 direction (authentic core in `:legado-runtime`, Badcat UI/platform adapters in `:app`); document compile-time dependency implications and any required facade direction without assuming a source-set edit is safe.
  4. Build a pinned-upstream source-closure plan for the absent authentic `BaseSource`, `BookSource`, `BookChapter`, `AnalyzeRule`, `AnalyzeUrl`, `JsExtensions`/inherited extensions, `SharedJsScope` and `JsSourceEngine` declarations. For each, record direct/transitive imports, real BookSource callers, Android/Room/UI dependencies, required typed Host Ports, and whether the dependency is core, platform adapter, or excluded product UI.
  5. Identify source groups that must remain single-owner and those requiring typed adapters; record unresolved dependency/Gradle evidence as unknown rather than guessing.
- **Acceptance:** complete duplicate-name-to-source/module matrix covering all 38 DEX definitions; caller/reference map for each top-level family; one recommended canonical owner with explicit rationale and migration sequencing; authentic runtime class/source-closure matrix with pinned paths and dependency classes; typed Host Port and Badcat shell boundary; risks, unknowns, validation tasks, and stop conditions. Recommendation must preserve upstream behavior and avoid shadow DTOs, string-map substitutes, no-op stubs, duplicate class definitions, and Legado UI/Reader migration.
- **Exclusions:** no source, test, Gradle, source-set, manifest, dependency, workflow, license/notice, schema, database, UI or Reader changes; no build/assemble/test/device/CI/Actions; no tool installation or system/network changes; no Stage-1 authorization; no claim of complete compatibility or runtime behavior.
- **Next:** after this read-only plan is reviewed, register a separate implementation/remediation task with exact files, callers, focused duplicate-class/ABI tests, source-closure acceptance and reproducible build requirements. Do not modify production files under Task 021.


### 4.21.1 Task 021 result — canonical source ownership and runtime closure plan

- **Status:** complete as a bounded read-only design/evidence task; no production remediation authorized.
- **Identity:** registered at `534e18a6cb95f5f98b0806039a15a1057a469c9f`; source inspection used Badcat `4c9cf8ca41a29fcfacdb17fb3ec1d66d4b451343` and pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9`. Compare confirms only the two allowed docs paths changed between the source snapshot and registered branch state.
- **Duplicate ownership:** all 38 DEX duplicate names map to seven rule-model source files plus `GsonExtensions.kt`, each compiled from both `:app` default sources and `:legado-runtime`'s injected pinned source set. The eight source files are byte-identical to upstream. All 29 paired pre-D8 class files have identical `javap -p -s` output; nine additional names are D8-generated synthetic lambdas. This is a confirmed duplicate packaging/source-ownership anomaly, not a proven runtime crash.
- **Caller finding:** bounded search found no app production caller to the seven duplicated model types/Gson helpers outside those source files; app tests use them and app already depends on `:legado-runtime`. `:integration-contract`'s pinned parser source set has no direct imports of these model classes.
- **Recommendation:** `:legado-runtime` should be the single canonical owner of the seven rule models and Gson extensions. Add exact upstream `RowUi`, `ExploreKind` and `FlexChildStyle` to the same ownership review because BaseSource/LoginUiV2/BookSourceExtensions use them; keep Badcat screens and UI controllers in `:app`.
- **Authentic closure:** BaseSource, BookSource, Book, BookChapter, AnalyzeRule, AnalyzeUrl, JsExtensions/inherited APIs, SharedJsScope and JsSourceEngine form a coupled source/runtime closure. They cross Room/Parcelable, DB helpers, HTTP/Cookie, WebView/browser, cache, crypto, file/archive, coroutine and product callback boundaries. These require typed Host Ports; parser presence alone is not caller parity. See audit §24.80.1 for the source/caller/dependency matrix.
- **Boundary:** `:legado-runtime` must not depend on `:app`; Badcat UI/platform implementations stay in app, typed contracts in `:integration-contract`, Rhino in `:legado-rhino`, and the probe module remains non-production. The proposed owner/direction is a plan only; no source-set or module-boundary change was made.
- **Validation:** read-only grep/source/build inspection, SHA-256 of source/class files, and `javap -p -s` comparison. No build, test, CI, device run, source/configuration/notice/schema/UI change or process termination.
- **Disposition:** Task 021 complete as a design slice; actual duplicate removal, full authentic source closure, production Host Port wiring, exact Gradle dependency closure, ABI/runtime parity and reproducible packaging remain open. Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; full compatibility remains **NOT ESTABLISHED**.
- **Next:** register a separate implementation/remediation task before changing app/runtime source sets. Include all eight duplicate sources, the three adjacent models, app tests/callers, dependency-cycle proof, duplicate-class/ABI checks and a reproducible APK/DEX verification. Continue the full runtime closure as a separate explicit Stage-0 gate; do not start Stage-1.

### 4.22 Runtime-3.0 Stage-0 technical task registration — authentic model-to-storage dependency boundary

- **Task ID:** `R3-S0-MODEL-STORAGE-CLOSURE-022`
- **Status:** registered; not started.
- **Start branch HEAD:** `eb648ab0dff8880014dfd24e96b2dedfc6b47ce6`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Goal:** extend, not repeat, the existing BookSource and Book/BookChapter field/schema inventories (§§24.51–24.52) by tracing the exact pinned source dependency boundary from authentic BookSource/BaseSource, Book/BaseBook, BookChapter and RuleDataInterface into Room annotations/converters/DAOs/database registration, large-variable persistence and app/config/UI helpers. Separate the behavior-bearing model contract from storage implementations and unrelated Legado product database/UI dependencies. Identify the minimum typed Host Ports required to preserve upstream behavior without importing the whole Legado AppDatabase or creating a second Badcat data authority.
- **Allowed repository files:** this overview and `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` only. Pinned upstream and current Badcat source/build declarations may be read-only inspected; temporary reports under `/tmp` are allowed.
- **Dependencies:** Task 021 and audit §24.80.1; prior BookSource model/schema inventory §24.51; Book/BookChapter/ReadConfig and variable contract §§24.49/24.52; Cache storage semantics §24.53; core caller and module boundary §§24.58–24.71. Do not re-inventory fields already recorded in §§24.51–24.52.
- **Inspection boundary:**
  1. Starting from the authentic pinned BookSource/BaseSource, Book/BaseBook, BookChapter and RuleDataInterface declarations, enumerate direct imports and methods that cross persistence/config/file/network/UI boundaries.
  2. Trace every Room annotation, TypeConverter, DAO, database entity/view registration and migration/schema reference required by these models. Distinguish entity compile-time dependencies from runtime DAO/service calls; do not infer that Room requires the entire upstream AppDatabase.
  3. Trace BookChapter/RuleData variable reads/writes and large-variable storage to exact upstream helper/DAO/cache owners; map source/book/chapter keys, cleanup, transaction and coroutine semantics already known only where they affect this boundary.
  4. Inspect model-only, persistence-only, Android platform and Legado product-UI dependencies separately; identify which can be kept authentic, which require typed Badcat Host Ports, and which are not justified by actual BookSource callers.
  5. Compare the pinned dependency boundary with current Badcat modules and existing Host Port declarations. State any module-cycle/Gradle/variant question as unresolved unless directly proven from declarations or existing reports.
- **Acceptance:** path-backed dependency/caller matrix for the four model roots; exact Room entity/converter/DAO/database registration closure; explicit model-vs-storage-vs-platform-vs-product-UI classification; typed Host Port inventory for every required storage/config/file/UI effect; clear decision whether the authentic model sources can be reused without the whole AppDatabase; open questions and validation plan. Preserve exact upstream identity, field/default/null/serialization, RuleData scopes, `(bookUrl,index)`/chapter URL semantics, cleanup and transaction behavior. No shadow DTO or generic Map bridge.
- **Exclusions:** no source/test/Gradle/source-set/manifest/dependency/workflow/license/notice/schema/database/UI/Reader changes; no build/assemble/test/device/CI/Actions or dependency resolution; no tool installation, system/network change or process termination; no schema freeze, data migration, Stage-1 authorization or complete compatibility claim.
- **Stop conditions:** source snapshot or upstream pin mismatch; inability to inspect a required declaration; evidence requiring a build/dependency resolution; ambiguous caller reachability. Record the unresolved edge instead of guessing or expanding scope.
- **Next:** use the resulting closure matrix to define a separately registered architecture gate/remediation plan. Do not change source ownership, import AppDatabase or migrate data under Task 022.

### 4.22.1 Task 022 result — authentic model and storage dependency boundary

- **Status:** complete as a bounded read-only source/dependency audit; no production remediation authorized.
- **Identity:** Task 022 registered at `eb648ab0dff8880014dfd24e96b2dedfc6b47ce6`; registration commit `8256475d79beb87180bc687a932b664362e7633e`. Read-only runner Badcat snapshot `4c9cf8ca41a29fcfacdb17fb3ec1d66d4b451343`; upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`. Compare to current branch shows only the two allowed docs paths changed; runner checkout clean.
- **New closure evidence:** BookSource has six rule converter pairs and no direct `appDb` call; BaseSource is coupled to Rhino/HTTP/Cookie/cache/crypto/login/shared scope/coroutines; Book and BookChapter have direct DAO/global reader/config/replacement side effects; BaseBook/BookChapter big variables use RuleBigDataHelp with the exact 10,000-character inline threshold and MD5 file namespace.
- **Room boundary:** BookSource/Book/BookChapter entities and converters do not by themselves require the entire upstream AppDatabase. BookChapter FK requires Book; authentic SearchBook is used by WebBook/BookList/JsSourceBook/JsSourceMarshaller and has a BookSource FK; BookSourceCheckState and BookSourcePart support source check-state/catalog projection. Upstream AppDatabase v113 registers 26 entities plus one view, including unrelated product domains; no evidence justifies importing all of them.
- **Host Port gap:** existing RuntimeHostPort declares browser/WebView/verification/UI/product-event ports but no typed BookSource/Book/SearchBook/BookChapter repository, transaction, ReadConfig or large-variable storage port. Existing SourceExecutionContext is identity-only for source data, not the authentic mutable model caller context.
- **Recommendation:** keep authentic model identities/serialization/rule semantics under one runtime owner, and adapt persistence/config/file/replacement/UI side effects through typed Badcat Host Ports. Do not import whole AppDatabase or create a second authoritative store. Exact minimum Room subset, DAO replacement, module graph, transaction parity and runtime behavior remain NOT ESTABLISHED.
- **Validation:** source/build declaration and caller inspection with `find`/`grep`/`sed`/Python; no build, test, dependency resolution, device run, CI, source/build/schema/database/UI/notice change or tool installation.
- **Disposition:** Task 022 complete as a source-only evidence slice; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; full Legado compatibility remains **NOT ESTABLISHED**.
- **Next:** register a separate typed model/storage Host Port contract task before editing model side effects or choosing persistence. Include BookSource/SearchBook/Book/BookChapter identity, check-state revisions, transaction/cascade/cleanup, RuleData large values and ReadConfig while preserving Badcat data ownership and excluding Legado product UI.

### 4.23 Runtime-3.0 Stage-0 technical task registration — typed model and storage Host Port contract

- **Task ID:** `R3-S0-TYPED-MODEL-STORAGE-PORT-CONTRACT-023`
- **Status:** registered; not started.
- **Start HEAD:** `b43bcba6b9f7735d2d1989d96c1d8b3a327434a9`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Goal:** convert Task 022's model/storage dependency evidence into a concrete, typed, caller-oriented Host Port contract proposal. Specify exact model identity and lifecycle crossing points for BookSource, SearchBook, Book/BaseBook, BookChapter, ReadConfig and RuleData large variables; define source-check state, transaction/batch, cascade/cleanup and UI/config/replacement effects without choosing a database or implementing adapters.
- **Allowed repository files:** this overview and `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` only. Existing Badcat and pinned upstream source/build declarations may be read-only inspected; temporary reports under `/tmp` are allowed.
- **Dependencies:** Task 021/audit §24.80.1 and Task 022/audit §24.81.1; invocation context §24.70; Host Port and module boundary evidence §§24.71 and 24.80; prior model/schema inventories §§24.49–24.53. Do not repeat those inventories.
- **Inspection boundary:**
  1. Trace all existing Badcat BookSource/Book/BookChapter/ReadConfig/repository/database/cache/config/replacement interfaces and their production callers. Record whether each is a canonical owner, read projection, cache or UI-only helper.
  2. Trace upstream method/caller crossing points for source import/update/delete/check, SearchBook result merge, Book save/delete, BookChapter update/list/lookup, RuleData inline/big storage, ReadConfig, replacement rule and user notifications. Preserve exact types, null/default/error, coroutine/cancellation, ordering, transaction and identity semantics where evidenced.
  3. Propose narrow typed ports grouped by domain, including method signatures or Kotlin-like contract sketches, explicit input/output/result/error types and ownership. Avoid generic Map/Any bridges and avoid moving UI or database implementation into the runtime.
  4. Map each port to existing Badcat owner/candidate adapter and identify missing implementations, module direction/cycle risks, and whether an authentic upstream method requires a fork/adapter rather than direct reuse.
  5. Explicitly distinguish a contract proposal from a production implementation; do not assert that declarations are wired or that a data-owner decision/schema has been accepted.
- **Acceptance:** caller-to-port matrix with exact source paths; typed Kotlin contract sketch for every required operation; explicit single-owner/read/write/transaction/cache invalidation responsibilities; identity and lifecycle guarantees; exact unresolved decisions and focused contract/architecture/differential tests required before implementation. The result must be sufficient to register a separately scoped implementation task without guessing.
- **Exclusions:** no source/test/Gradle/source-set/manifest/dependency/workflow/license/notice/schema/database/UI/Reader changes; no build/assemble/test/device/CI/Actions or dependency resolution; no database choice, schema freeze, data migration, dual-write, Stage-1 authorization or complete compatibility claim; no import of Legado AppDatabase or product UI.
- **Stop conditions:** pinned source or branch snapshot mismatch; incomplete caller evidence; inability to identify the current Badcat data owner; any requirement for build/dependency resolution. Record unresolved edges rather than guessing.
- **Next:** after contract review, register a separate implementation/PoC task with explicit data-owner choice and no-cycle module proof. Do not implement ports or modify source ownership under Task 023.

### 4.23.1 Task 023 result — typed model/storage Host Port contract proposal

- **Status:** complete as documentation-only contract proposal; no implementation authorization.
- **Identity:** registration HEAD `b43bcba6b9f7735d2d1989d96c1d8b3a327434a9`; registration commit `eddc76878579bdab762a2dc247854801acf9950e`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Current owner findings:** `FileBookSourceCatalog` is an AtomicFile JSON source catalog but returns host DTOs; `BadcatReadingStateRepository` owns progress/bookmarks/history; `FileChapterContentCache` owns parsed content cache and fenced writes; `FileReplaceRuleCatalog` owns replacement JSON. No current Badcat Book/BookChapter repository, authentic SearchBook bridge or RuleData big-variable store was found in inspected production source.
- **Contract proposal:** typed BookSourceStore, BookStore, BookChapterStore, RuleVariableStore, RuntimeConfigPort, ReplacementRulePort and RuntimeNotificationPort using pinned upstream model types; SearchBook stays a transient authentic result projected to Badcat UI DTOs. Keep RuleDataInterface's inline threshold in upstream code; do not conflate RuleBigDataHelp with parsed chapter cache.
- **Module placement:** model-typed interfaces should live beside authentic model types in `:legado-runtime` (or a future Android-compatible runtime-contract module); `:app` implements them; `:legado-runtime` never depends on `:app`. Keep `integration-contract` model-neutral to avoid a runtime↔contract dependency cycle. This is a proposal, not a proven buildable graph.
- **Single owner:** preserve current Badcat progress and parsed chapter cache owners; source catalog requires an explicit parity/migration decision; Book/Chapter durable owner remains undecided; replacement JSON is a candidate but scope parity is unproven; large RuleData storage requires a new typed adapter.
- **Evidence:** audit §24.82.1 contains path-backed caller/owner matrix, proposed Kotlin-like signatures, transaction/identity semantics, module graph constraints and required focused tests.
- **Validation:** read-only inspection of existing source/contracts/callers; no build/test/device/CI/dependency resolution; no source/build/schema/database/UI changes.
- **Disposition:** Task 023 complete as contract design only. Book/Chapter storage owner, minimum persistence strategy, transaction/delete semantics, global config ownership, production wiring and build/module-cycle proof remain NOT ESTABLISHED. Stage-0 remains `in_progress / not accepted`; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** register Task 024 architecture decision/implementation-plan review before any production source or Gradle changes.


## 4.24 Runtime-3.0 Stage-0 — architecture decision and implementation-plan review

- **Task ID:** `R3-S0-ARCHITECTURE-DECISION-REVIEW-024`
- **Status:** registered; not started.
- **Start branch HEAD:** `802ceb9afcc7fc2afeea07721774a766a58ba0fb`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Goal:** reconcile the Task 020 DEX/source-ownership blocker with Tasks 021–023's authentic runtime closure, model/storage boundary and typed Host Port proposal. Produce a bounded architecture decision and implementation plan: one canonical source owner for each model/data domain, runtime-owned location for model-typed ports, module dependency direction, Book/BookChapter transaction and deletion lifecycle, and the minimum storage boundary. Distinguish logical ownership decisions from physical schema/API decisions that remain unproven.
- **Allowed repository files:** this overview and `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` only. Use existing Task 020–023 evidence; read-only source/build declarations only if a specific decision cannot be resolved from those records. No local stale checkout may be treated as the current branch snapshot.
- **Dependencies:** overview §§4.20.1–4.23.1; audit §§24.79.1–24.82.1; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Review questions:** (1) resolve the eight duplicate source files and adjacent model ownership; (2) separate authentic runtime model identity from Badcat UI DTO projections; (3) assign one logical owner to BookSource, SearchBook, Book, BookChapter, progress, parsed content cache, RuleData big values, replacement rules and config; (4) define the runtime/app/integration-contract/Rhino module graph without cycles; (5) specify transaction, cascade, active-reader detach and progress snapshot/restore semantics; (6) identify the minimum justified storage set without importing the full upstream AppDatabase or freezing an unproven schema; (7) keep missing BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions/SharedJsScope/JsSourceEngine closure as an explicit Stage-0 gate.
- **Acceptance:** evidence-backed ownership matrix; module graph with permitted dependency arrows; explicit Book/Chapter write/delete transaction sequence and rollback boundary; minimum storage recommendation and exclusions; duplicate-class remediation path; list of unresolved questions and separately scoped implementation tasks; explicit Stage-0/Stage-1 GO/NO-GO. Do not label a proposal as implemented or runtime-validated.
- **Exclusions:** no production source, test, Gradle/source-set, dependency, manifest, schema/database, UI/Reader, notice/license, workflow/CI, APK or device changes/runs; no database/schema freeze, data migration, dual-write, source-set remediation or Stage-1 authorization.
- **Stop conditions:** conflicting evidence, missing caller/owner proof, or a decision requiring new builds/dependency resolution. Record a conditional recommendation and blocker rather than guessing.
- **Next:** if the review yields a viable boundary, register separately scoped source-ownership and storage/Host Port implementation PoCs with exact files, callers, contract tests and rollback criteria. Do not implement under Task 024.

### 4.24.1 Task 024 result — Runtime-3.0 ownership and minimum-boundary decision

**Identity and evidence boundary**

- Task 024 registered at start HEAD `802ceb9afcc7fc2afeea07721774a766a58ba0fb`; overview registration commit `c4cc281b5fc338fce4c0e97e3bcc416e3d80299e`; audit registration commit/current start for this review `062c07e85658b950d49dcd2f3229981265b6883d`. Branch: `integration/legado-upstream-runtime`.
- Pinned behavior baseline: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- This review used the already-recorded bounded evidence in §§4.20.1–4.23.1 and audit §§24.79.1–24.82.1. The old runner checkout remains detached at source snapshot `4c9cf8ca41a29fcfacdb17fb3ec1d66d4b451343`; it was not treated as the current branch HEAD and no runner operation, source/build inspection, build, test, dependency resolution, CI or device execution was needed.
- Documentation-only result. No production/test/Gradle/source-set/dependency/manifest/schema/database/UI/Reader/license/notice/workflow/APK changes.

**Architecture decision — logical ownership**

| Model/data domain | Single logical owner decision | Runtime and Badcat boundary | Evidence / remaining condition |
|---|---|---|---|
| Pinned rule models (seven rule families) and Gson extensions | `:legado-runtime` is the sole canonical source/compile owner | `:app` consumes the authentic upstream types through its existing runtime dependency; no app-local duplicate declarations | Task 020/021 maps all 38 duplicate DEX names to the seven rule files plus GsonExtensions; no app production caller requires a second copy. Removal is a separate implementation task with app-test and DEX checks. |
| Adjacent source-contract models `RowUi`, `ExploreKind`, `FlexChildStyle` | `:legado-runtime` | Preserve exact pinned types; Badcat owns rendering/forms/screens in `:app` | Task 021 found exact upstream copies and real BaseSource/LoginUiV2/BookSourceExtensions callers. Flexbox/Android dependencies require explicit dependency closure; do not import Legado screens. |
| Authentic `BaseSource`, `BookSource`, `BaseBook`, `Book`, `BookChapter`, `SearchBook`, `AnalyzeRule`, `AnalyzeUrl`, `JsExtensions`, `SharedJsScope`, `JsSourceEngine` | `:legado-runtime` owns the authentic declarations and their source-visible behavior | Runtime passes authentic objects; Badcat DTOs are one-way UI projections only. Android/Room/HTTP/Cookie/WebView/cache/config/crypto/file/coroutine/UI effects cross typed ports | This is the target ownership, not current implementation. Full transitive closure, reflection/script reachability and buildable module closure remain NOT ESTABLISHED. |
| BookSource catalog and check state | One Badcat SourceRepository aggregate is the sole durable owner after migration | Runtime uses authentic BookSource through typed store; catalog UI/import/export remains Badcat | Current FileBookSourceCatalog is the only evidenced catalog owner, but it lacks authentic typed models and check-state transactions. Recommended target storage is a minimal Room-backed repository; migration and parity are not implemented. |
| SearchBook results | Runtime invocation scope; transient by default | Preserve authentic SearchBook and origin while projecting to Badcat search/detail DTOs | No durable SearchBook owner is justified by current caller evidence. If a real pinned caller requires durable search-result storage, register a separate evidence-backed extension; do not persist/merge into Book implicitly. |
| Book and BookChapter metadata/TOC | One Badcat BookRepository/ChapterRepository backed by the same minimal runtime-data store | Runtime owns model semantics; Badcat owns repository implementation and UI projection | No current durable Book/Chapter repository exists. Recommend Room tables for Book and BookChapter with pinned keys/index/FK; exact schema, converters and caller semantics remain open. |
| Reader progress, bookmarks, history | Existing BadcatReadingStateRepository remains sole owner | Runtime accesses through a typed adapter; no second upstream progress writer | Existing SharedPreferences authority remains. Do not move it into Room or duplicate it during this architecture task. |
| Parsed chapter content cache | Existing FileChapterContentCache remains sole owner | Runtime consumes a separate cache port | It is not RuleBigDataHelp and must not be reused for RuleData variables. Preserve its current version-fenced AtomicFile semantics. |
| RuleData large values | One new Badcat RuleVariableStore adapter, filesystem-backed initially | Authentic RuleDataInterface keeps inline/10,000-character/null semantics; typed port supplies only big-value backing | Preserve MD5 namespaces, bookUrl marker, synchronized access, null deletion and cleanup. RSS storage remains outside this book-runtime boundary unless a real BookSource caller proves otherwise. |
| Replacement rules | Existing FileReplaceRuleCatalog remains the sole current owner pending parity | Runtime uses a typed replacement port; Badcat owns editor/import/export UI | Exact pinned scope/order/target/timeout-disable and BookChapter title-normalization parity is unproven; do not claim it is a final compatible owner until differential tests pass. |
| Config | Split ownership: Book.readConfig belongs to the Book record; global source/runtime config belongs to one Badcat host config repository; Reader presentation preferences remain Badcat UI-owned | Runtime receives separate typed per-book and global-config ports | Never conflate Book.readConfig, global ReadBookConfig and Badcat reader preferences. Exact global config field list/serialization owner remains open. |
| Runtime notifications and product actions | Badcat host UI/event services | Typed callback ports only; no Activity/View retained by runtime | Notification failure is distinct from repository commit; preserve cancellation and failure outcomes. |

**Minimum justified storage boundary**

Recommend a dedicated, Badcat-owned Room database boundary for authentic runtime model metadata, not the upstream 26-entity AppDatabase and not an unproven schema freeze. The smallest currently justified relational set is:

1. `book_sources` — authentic BookSource identity and complete rule/config payload, keyed by `bookSourceUrl`.
2. `book_source_check_states` — source check-state keyed/related to BookSource with cascade and transactional begin/complete semantics.
3. `books` — authentic Book identity keyed by `bookUrl`, preserving the pinned unique `(name, author)` contract and typed ReadConfig converter.
4. `chapters` — authentic BookChapter with composite PK `(url, bookUrl)`, unique `(bookUrl, index)`, and FK cascade to Book.

A small host deletion-journal/outbox record is recommended if Book deletion spans Room plus SharedPreferences/filesystem cleanup, so post-commit cleanup can be retried after process death. Its exact representation is a PoC decision, not an approved schema. Room is recommended because the pinned model contracts already expose Room identity/index/FK semantics and chapter/book atomicity; this recommendation does not authorize adding Room now.

Explicitly exclude the upstream AppDatabase registration, unrelated bookshelf groups/memos/highlights/RSS/TTS/audio/video/server entities and product views/DAOs unless a traced BookSource execution caller proves a dependency. Do not include parsed body cache, progress, bookmarks/history, RuleData files or replacement rules in these four model tables. No database migration, schema version or table DDL is frozen by Task 024.

**Module graph and ownership direction**

```text
:app
 ├──> :legado-runtime
 ├──> :integration-contract
 └──> :legado-rhino (direct only where app platform wiring needs it)

:legado-runtime
 ├──> :integration-contract   (model-neutral invocation/platform contracts only)
 └──> :legado-rhino           (authentic Rhino engine/security implementation)

:integration-contract  -X-> :legado-runtime model types
:legado-runtime        -X-> :app
:legado-rhino          -X-> :app / product UI
:legado-runtime-poc    -X-> production dependency graph
```

Model-typed persistence/config/replacement/notification interfaces must live beside the authentic model declarations in `:legado-runtime` (or a future Android-compatible runtime-contract submodule that does not create a cycle). The existing `:integration-contract` remains Kotlin/JVM and model-neutral; it may carry invocation identity, cancellation and generic browser/UI ports but must not import runtime Android/Room/Parcelable model types if Runtime consumes it. `:app` implements runtime-owned typed ports. The graph is a logical recommendation only; exact Gradle variants, transitive dependencies and no-cycle compilation remain unproven.

**Book / BookChapter transaction and deletion protocol**

- **Book write:** load/retain the authentic Book object; validate `bookUrl` and the pinned unique `(name,author)` constraint; write Book and its typed ReadConfig in one repository transaction. Do not create a second Book DTO authority. A failed transaction leaves the previous Book/readConfig unchanged.
- **Chapter batch write:** acquire a per-book write fence; validate every row belongs to the requested `bookUrl`; perform the batch in one Room transaction while preserving PK `(url,bookUrl)`, unique `(bookUrl,index)`, ordering and FK. Apply the exact pinned conflict/replace policy only after the source caller audit; do not invent URL-only matching. Publish cache content only after the corresponding chapter/version fence is current.
- **Book delete prepare:** acquire the per-book lifecycle lock; stop accepting new chapter writes and cancel/quiesce active source/reader jobs; ask the Badcat reader lifecycle port to detach or pause and capture the current progress/bookmark/history snapshot. Do not clear the existing progress owner before the durable delete transaction commits.
- **Book delete commit:** in one Room transaction, record a pending cleanup journal (including the identity and the captured progress snapshot/reference) and delete the Book; the BookChapter FK cascade removes its chapter rows. Any failure before commit rolls back the database mutation and leaves progress/cache/variables available; the reader can be resumed from the snapshot.
- **Post-commit cleanup:** idempotently finalize reader detach, clear only the deleted book's Badcat progress/bookmark/history entries, invalidate its parsed chapter cache, and remove its RuleData big-value namespace. Mark the journal complete only after all cleanup steps succeed. A crash/failure after DB commit is a pending cleanup, not a database rollback; replay the journal on next startup. Keep the record until cleanup succeeds.
- **Source delete/check:** delete BookSource and its check-state in one repository transaction; after commit invalidate live runtime instances and source-scoped caches. Stale check completions must be rejected by a host revision/lease if required by the caller contract; do not claim the lease is an existing upstream token.
- This protocol is a target contract to validate with failure injection and restart tests. It does not claim current behavior or authorize implementation. Exact progress snapshot format and whether reader detach can be safely resumed are open PoC questions.

**Duplicate-class remediation path**

1. In a separately registered source-ownership task, make the seven rule files and GsonExtensions compile only from `:legado-runtime`; remove the redundant app source copies only after verifying all app production/test imports and test compile behavior.
2. Bring exact `RowUi`, `ExploreKind`, `FlexChildStyle` declarations under the same runtime owner when the authentic caller closure is integrated; keep Badcat rendering in app.
3. Add a static duplicate-FQCN guard at module/DEX boundary; require zero duplicate class definitions in the rebuilt APK and compare public JVM descriptors/serialized model fixtures before and after. Do not infer safety from the 29 matching `javap` reports alone.
4. Treat this as a packaging ownership fix only. It does not supply the missing BaseSource/BookSource/BookChapter/AnalyzeRule/AnalyzeUrl/JsExtensions/SharedJsScope/JsSourceEngine source closure.

**Separate implementation/verification tasks required**

- **Task 025 — canonical rule/model source ownership PoC:** exact eight duplicate source removals from app ownership; adjacent three model ownership review; app test/caller migration; Gradle no-cycle and duplicate-FQCN guard; exact APK/DEX zero-duplicate check. No broader runtime source closure bundled into this task.
- **Task 026 — authentic runtime source-closure PoC:** bring in the minimal real BookSource/BaseSource/Book/BaseBook/BookChapter/SearchBook/AnalyzeRule/AnalyzeUrl/JsExtensions/SharedJsScope/JsSourceEngine caller closure from the pinned SHA; typed ports for every platform side effect; no shadow DTO/no-op; exact symbol/caller/API and dependency closure; compile/runtime boundary proof. Keep Legado UI/Reader excluded.
- **Task 027 — minimum storage and typed Host Port PoC:** only after authentic model closure is buildable, implement the minimal Room repository and RuleVariableStore/config/replacement adapters, progress/cache single-owner bridges, transaction/delete journal and migration dry-run/rollback/restart tests. Do not dual-write or migrate user data before a separately accepted migration plan.
- These are proposed task boundaries, not registered implementation authorization. No implementation starts under Task 024.

**Unresolved blockers and gate decision**

- Authentic source closure and caller parity for BaseSource/BookSource/Book/BookChapter/SearchBook/AnalyzeRule/AnalyzeUrl/JsExtensions/inherited groups/SharedJsScope/JsSourceEngine remain NOT ESTABLISHED.
- Exact Gradle dependency/variant closure, Android/Room converters and Flexbox dependency closure, reflection/script-visible reachability, no-cycle compile proof and reproducible packaging remain NOT ESTABLISHED.
- SearchBook durable use, global ReadBookConfig field ownership, replacement-rule parity, BookChapter batch conflict policy, progress snapshot/restore semantics, deletion-journal schema and BookSource file-to-Room migration/rollback require focused PoCs.
- License/SBOM, reproducibility and behavioral differential gates remain open per §4.20.2. The old successful assemble remains bounded evidence only; the 38 duplicate DEX definitions and missing authentic runtime classes are not repaired.
- **Task 024 disposition:** complete as a bounded logical architecture decision and implementation plan. No source/build/test/schema/database/UI changes; no build/test/CI/device/dependency resolution performed.
- **Stage-0:** `in_progress / not accepted`. **Stage-1:** **NO-GO**. Complete Legado compatibility: **NOT ESTABLISHED**. No release readiness claim.
- **Next:** separately preregister Task 025 source ownership remediation and Task 026 authentic source-closure PoC; do not begin storage implementation or Stage-1 until authentic closure and its module boundary are proven.


## 4.25 Runtime-3.0 Stage-0 — canonical rule/model source ownership remediation PoC

- **Task ID:** `R3-S0-CANONICAL-MODEL-SOURCE-OWNERSHIP-025`
- **Status:** completed; implementation committed and focused build/test/duplicate-class verification passed on 2026-11-25. CI/Actions were intentionally not run per task exclusion.
- **Registration branch / start HEAD:** `926afed48e1f0a41f1a4722065e795a905aac75f`; branch `integration/legado-upstream-runtime`.
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9`.
- **Goal:** implement the narrow Task 024 ownership decision for the duplicate rule-model/Gson source family. Establish one compiler owner for the seven pinned rule models and `GsonExtensions.kt`, retain app callers through the existing `:app -> :legado-runtime` dependency, and verify no module cycle or duplicate FQCN remains. Review the adjacent `RowUi.kt`, `ExploreKind.kt`, and `FlexChildStyle.kt` contracts and move them only if their exact pinned dependencies are already satisfied without importing Legado product UI or adding an unreviewed dependency.
- **Primary allowed source changes:** delete only these eight redundant app-owned source copies after caller review:
  - `app/src/main/java/io/legado/app/data/entities/rule/BookListRule.kt`
  - `app/src/main/java/io/legado/app/data/entities/rule/BookInfoRule.kt`
  - `app/src/main/java/io/legado/app/data/entities/rule/ContentRule.kt`
  - `app/src/main/java/io/legado/app/data/entities/rule/ExploreRule.kt`
  - `app/src/main/java/io/legado/app/data/entities/rule/ReviewRule.kt`
  - `app/src/main/java/io/legado/app/data/entities/rule/SearchRule.kt`
  - `app/src/main/java/io/legado/app/data/entities/rule/TocRule.kt`
  - `app/src/main/java/io/legado/app/utils/GsonExtensions.kt`
- **Conditional adjacent-model scope:** `app/src/main/java/io/legado/app/data/entities/rule/RowUi.kt`, `ExploreKind.kt`, and `FlexChildStyle.kt` may be moved to the runtime owner only after source/caller/dependency inspection proves the existing runtime module can compile them without a new unreviewed dependency or any Badcat UI migration. If not, leave them untouched and record the exact blocker.
- **Build/test/guard scope:** inspect existing app tests and build scripts; only edit exact test/build files after their paths and reason are recorded in a Task 025 registration addendum before modification. Add a focused duplicate-FQCN guard if it can be implemented without CI/workflow changes. No workflow/Actions/CI edits or executions.
- **Dependencies:** Task 020 DEX duplicate inventory (§24.79.1); Task 021 source/caller ownership map (§24.80.1); Task 024 canonical ownership and module-direction decision (§24.83.1); fixed upstream SHA.
- **Required inspection:** verify branch/HEAD and clean worktree; confirm exact file hashes against the pinned upstream; trace app production and test imports; confirm `:app` already consumes `:legado-runtime`; inspect runtime source-set declarations and adjacent model dependencies; do not use the stale detached runner snapshot as current branch evidence.
- **Acceptance:** all eight duplicate app source copies removed or a path-specific stop reason recorded; app production/test references resolve to the runtime-owned declarations; runtime/app dependency graph has no cycle; focused compile/test evidence recorded; a duplicate-FQCN check exists and reports zero duplicate definitions, with exact APK/DEX evidence if a build can be run safely on an exact-HEAD checkout. A build or test not executed must remain NOT ESTABLISHED.
- **Exclusions:** no Task 026 authentic runtime source closure; no BaseSource/BookSource/Book/BookChapter/AnalyzeRule/AnalyzeUrl/JsExtensions/SharedJsScope/JsSourceEngine migration; no Room/schema/database/data migration; no dual-write; no product UI/Reader changes; no dependency upgrade; no CI/Actions/workflow changes or runs; no release/compatibility claim; no Stage-1 authorization.
- **Stop conditions:** missing/ambiguous caller evidence; adjacent model requiring unreviewed Android/Flexbox dependency; app test failure requiring broader product changes; Gradle cycle; inability to obtain exact current-HEAD checkout; or build-runner instability. Stop and record rather than widening the task.
- **Next:** after Task 025 is complete, register Task 026 for the authentic runtime source/caller closure. Task 027 storage implementation remains blocked until that closure is buildable.


**Task 025 inspection addendum (before any source/build edit)**

- Exact current-HEAD app source copies were fetched and compared byte-for-byte with `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9`: all seven rule files and `GsonExtensions.kt` are identical (same Git blob SHA for each pair). No local-only behavior will be lost by deleting these app copies.
- Existing app test caller: `app/src/test/kotlin/io/legado/app/utils/LegadoGsonCompatibilityTest.kt`. It imports all six concrete rule types and exercises `GSON`/`INITIAL_GSON`; it will remain unchanged and serve as a focused compatibility test. The app already declares `implementation(project(":legado-runtime"))`, and runtime already compiles the eight pinned upstream files.
- Task 021's path-backed caller audit found no app production caller to these duplicate declarations outside their own files; app tests are the identified consumers. This is cross-checked against the current branch's unchanged source/build tree (the current branch differs from source snapshot `4c9cf8ca41a29fcfacdb17fb3ec1d66d4b451343` only in the two registered documentation files).
- **Additional exact allowed build path:** `app/build.gradle.kts`, solely to add a standalone `verifyNoDuplicatePinnedLegadoRuleClasses` task. It will inspect the Android `debugRuntimeClasspath` class-JAR view plus app Debug class outputs, require exactly one definition for the seven rule model classes and the five top-level Gson helper classes, and fail on missing or duplicate definitions. It will not be wired into CI/workflows or run automatically as part of unrelated tasks.
- The adjacent `RowUi.kt`, `ExploreKind.kt`, and `FlexChildStyle.kt` are **not moved in Task 025**: `FlexChildStyle` imports Android View/Flexbox and `:legado-runtime` does not currently declare the app's Flexbox dependency. They remain in app until Task 026 source-closure dependency review; no dependency is added here.
- No test source edit is currently required. If compilation reveals one, stop and add the exact test path and rationale to this registration before editing it.


## 4.26 Runtime-3.0 Stage-0 — Task 025 result and verification (2026-11-25)

- **Task ID:** `R3-S0-CANONICAL-MODEL-SOURCE-OWNERSHIP-025`
- **Status:** `completed` (bounded source-ownership PoC only; not full Runtime compatibility).
- **Implementation commit:** `996f60d79ca6a84c02b5b06b3357dc3487ac5fb3` on `integration/legado-upstream-runtime`.
- **Exact tree verification:** GitHub commit tree `8b0019b295bb5a944b72065782bff1740811f7ae`; reconstructed validation worktree `git write-tree` returned the identical tree SHA. The runner's original detached base was not treated as current branch evidence; the two intervening documentation files were refreshed from the current branch before matching the full tree.
- **Implementation:** removed the eight duplicate app-owned rule/Gson source files registered in Task 025; added standalone Gradle task `verifyNoDuplicatePinnedLegadoRuleClasses` in `app/build.gradle.kts`. Adjacent `RowUi`, `ExploreKind`, and `FlexChildStyle` were not moved. No CI/workflow, dependency version, UI, schema, or database changes.
- **Focused verification command:** `gradle --no-daemon :app:verifyNoDuplicatePinnedLegadoRuleClasses :app:testDebugUnitTest --tests io.legado.app.utils.LegadoGsonCompatibilityTest --stacktrace`.
  - `:app:compileDebugKotlin`: success.
  - `:app:verifyNoDuplicatePinnedLegadoRuleClasses`: PASS; all 12 registered classes have exactly one compiler owner, each in `:legado-runtime`'s Debug runtime classes JAR.
  - `LegadoGsonCompatibilityTest`: 6 tests, 0 skipped, 0 failures, 0 errors. JUnit: `app/build/test-results/testDebugUnitTest/TEST-io.legado.app.utils.LegadoGsonCompatibilityTest.xml`.
- **Dependency environment note:** the first attempt stopped before compilation because the root local Maven mirror lacked `htmlunit-core-js-5.3.0-legado.4.jar`. The identical pinned artifact already present under `upstream/legado/third_party/maven` was temporarily symlinked into the expected local mirror path for verification, then the symlink was removed. This was a local-only runner adjustment; no repository source or dependency metadata was changed.
- **APK build:** `gradle --no-daemon :app:assembleDebug` — BUILD SUCCESSFUL. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `ed2a5d1abe4b4dd62553176a7c450167df49027909c1e53c3b639d432a3644d5`.
- **DEX verification:** APK contains 14 DEX files. `apkanalyzer dex packages --defined-only` found each of the 12 Task-025 target base classes exactly once across all DEX files (12/12 unique); `DEX_CLASS_DEFINITION_CHECK=PASS`.
- **CI:** not run, as explicitly excluded by Task 025. No CI pass is claimed. The Debug APK is verification evidence, not a release artifact.
- **Disposition:** Task 025 acceptance is met. No duplicate app source remains in the eight registered paths; runtime ownership, focused compatibility test, Gradle guard, Debug assembly and target DEX uniqueness are verified on the exact commit tree.
- **Next gate:** separately preregister Task 026 (authentic Runtime source/caller closure) before any implementation. Task 027 storage work remains blocked until Task 026 proves the authentic source closure and buildable module boundary. **Stage-0 remains in_progress/not accepted; Stage-1 remains NO-GO; full Legado compatibility remains NOT ESTABLISHED.**


## 4.27 Runtime-3.0 Stage-0 — Task 026 registered: authentic Runtime source/caller closure

- **Task ID:** `R3-S0-AUTHENTIC-RUNTIME-SOURCE-CLOSURE-026`.
- **Status:** `registered`; Phase A read-only inventory is authorized; production source edits are not yet authorized.
- **Start HEAD:** `088f7eb3935ecf508c3228f652a01debe4062731`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Registration commit:** `b0860c7f51561777d6ebff3785c0f4c39bdbd64d`; full task boundary is in audit §24.85.0.
- **Goal:** prove a buildable, behavior-preserving authentic Legado source/caller closure inside `:legado-runtime`, with typed Host Ports for platform effects and no Legado product UI/Reader migration.
- **Inventory targets:** BaseSource, BookSource, BaseBook, Book, BookChapter, SearchBook, AnalyzeRule, AnalyzeUrl, JsExtensions and actually reachable inherited extension groups, SharedJsScope, JsSourceEngine, RowUi, ExploreKind, FlexChildStyle; trace search/list, detail, TOC, content, explore, login, JS marshalling and lifecycle callers.
- **Execution gate:** first complete the read-only declaration/API/overload/caller/reflection and transitive dependency/Gradle-variant inventory. Then write and commit a path-exact source/Host-Port/test/build addendum and reread both required lists. Only that addendum may authorize Phase B source/build/test edits.
- **Hard exclusions:** no schema/Room/data migration, no dual-write, no Task 027 storage work, no Badcat UI/Reader changes, no Legado UI/Reader/product screens, no unproven broad AppDatabase/RSS/TTS/media dependencies, no CI/workflow run, no Stage-1.
- **Next action:** perform Phase A against the fixed upstream and current integration branch, then preregister the exact implementation file boundary before changing code. Stage-0 remains in_progress/not accepted; Stage-1 remains NO-GO; complete Legado compatibility remains NOT ESTABLISHED.


## 4.27.1 Task 026 Phase A checkpoint — read-only inventory in progress

- **Review HEAD:** `81e6d851f0ed7c3f372ba4f0700d71fc411d5d4a`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Pinned source inventory confirms that `BookSource`, `Book`, `BookChapter`, `SearchBook`, `BaseSource`, `AnalyzeRule`, `AnalyzeUrl`, `JsExtensions`, `SharedJsScope` and JS-source engine classes are deeply coupled to Room, Android, cache/files, HTTP/Cookie, WebView/browser, crypto, config, lifecycle and product callbacks.
- The 12-core-seed conservative resolved import closure reaches 484 upstream files (overlapping tags: 211 activity/UI, 101 database, 53 Room, 69 network, 28 WebView, 46 Rhino/JS, among others). This is an import-graph warning, not a proven runtime call graph; unused imports and product-only paths still need caller-level adjudication.
- `JsExtensions` has 119 non-private function declarations / 78 names / 41 additional overloads in the source scan; derived `RssJsExtensions`, `WebJsExtensions` and `SourceLoginJsExtensions` add further JS-visible/callback surfaces. Exact compiled ABI/reflection closure is not yet complete.
- Current `:legado-runtime` still owns only the eight Task-025 rule/Gson files and has no complete authentic source/runtime dependency closure. Existing Badcat model-neutral `:integration-contract` contracts and Map/Any JS adapters are not substitutes for authentic Legado model types.
- The runner checkout is stale and dirty relative to this review HEAD; no build/test evidence was produced. No production source, Gradle, test, workflow or CI files were changed.
- **Status:** Task 026 Phase A remains in progress/not closed; Phase B is not authorized. Next: close exact callers/reflection, dependency variants and typed-port semantics, then commit the path-exact implementation/test/build addendum and stop for review. Task 027 remains blocked; Stage-0 not accepted; Stage-1 NO-GO; full Legado compatibility NOT ESTABLISHED.


## 4.27.2 Task 026 Phase A continuation — JS/caller/Gradle closure checkpoint (read-only; not closed)

- Review branch `integration/legado-upstream-runtime` at HEAD `792837d8713099babc3927770b72302a82e8c6d6`; upstream is fixed at `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- The authentic JS surface is inherited: `JsExtensions` extends `JsEncodeUtils`; `WebJsExtensions` and `SourceLoginJsExtensions` extend `RssJsExtensions`. The source-level scan finds 119 non-private `JsExtensions` functions / 78 names / 41 additional overloads / 48 JS-interface annotations; `JsEncodeUtils` adds 30 / 27 / 3 and 20 annotations. The derived bridge annotations, `@JvmOverloads`, callbacks and inherited methods are separate contract surfaces. `SourceLoginJsExtensions` methods are Rhino-visible public methods, not Android `@JavascriptInterface` methods.
- `JsSourceEngine` binds `java`, `source`, `sourceApi`, `baseUrl`, `cookie` and `cache`. `WebJsExtensions.request` is lifecycle-scoped asynchronous JS callback dispatch. Login callbacks update UI on the main thread and preserve null-reset / delta-render behavior. `SourceNavigationBridge` carries the original coroutine context and applies per-operation navigation suppression. `SourceVerificationHelp` has single-flight, lock, timeout, cancellation and UI-release semantics.
- BookSource paths require the shared `login/search/explore`, add-book, photo, DOM-analysis and login/browser APIs as typed Badcat host actions. The `rss/sort` branches of `RssJsExtensions.open`, RSS read records/screens and RSS-specific caller graph are product-only and must not be imported. The nested review API bridge is also an app-side callback seam.
- A further real content-runtime path is confirmed: `BookChapter.getDisplayTitle` calls `utils/RegexExtensions.kt`; its `@js:` replacement path injects `RegexJsExtensions` into Rhino. Keep the replacement execution, timeout/cancellation and error semantics; exclude ReplacePreview/editor UI.
- The 484-file static import closure remains an upper-bound warning, not a runtime call graph. The concrete ExploreAdapter/InfoMap/ACache references are host seams. Media3/Glide callers exist, but their BookSource runtime reachability is not established; do not add media libraries to Runtime without a source caller and a port contract.
- Upstream Gradle has one app flavor and Debug/Release variants with common implementation dependencies. Offline dependency reports found 71 root entries / 90 unresolved entries for each runtime variant, 75 / 92 for each unit-test variant, and 8 / 4 for Debug AndroidTest. Release AndroidTest configuration is absent. These reports are partial: BUILD SUCCESSFUL from the dependency-report task does not mean the FAILED offline modules resolved. Online resolution stalled; no compile/test/build was run.
- A path-exact Phase B candidate list, typed Host Port families, bridge adapters and test paths are recorded in audit §24.85.2. It is a proposal for review only; no production, Gradle or test edit is authorized.
- **Status:** Task 026 Phase A remains in progress/not closed. Compiled ABI/reflection descriptors, complete transitive dependency resolution, media/image and explore-catalog boundaries, and exact Host Port signatures remain open. Task 027 remains blocked; Stage-0 not accepted; Stage-1 NO-GO; complete Legado compatibility NOT ESTABLISHED.

### 4.27.3 Phase A continuation — shrinker, module variants, image and Explore boundaries (2026-11-25)

- **Review HEAD:** 18d83aac4b1b50e27f88754829850e250ff3f38b; pinned upstream remains 330e606c4d57e08adc056ff8ab7a4132c784da9a. The branch advanced two documentation-only commits from checkpoint 792837d...
- **Release/R8:** pinned upstream Release enables minification/resource shrinking and has a ProGuard keep rule for classes extending JsExtensions, plus data-entity and annotation/signature retention. Badcat app currently declares no buildTypes/minify/proguardFiles; runtime consumer rules are empty and Rhino consumer rules contain only a comment. No Badcat Release R8 or upstream optimized DEX proof exists.
- **Variant closure:** direct declarations are mapped for pinned :modules:book/:modules:rhino and Badcat :legado-runtime/:legado-rhino/:app; full Debug/Release/unit/AndroidTest transitive resolution remains unverified because the prior offline report contained unresolved artifacts and online resolution did not complete.
- **Model boundary:** RowUi, ExploreKind and FlexChildStyle remain byte-identical to the pinned source. FlexChildStyle.apply(View) directly requires Android View/Flexbox; any Runtime move must preserve the exact model/API and declare the dependency, without importing Legado product screens.
- **Image/Explore:** BookSource cover/chapter image processing uses AnalyzeUrl.getGlideUrl and source-specific coverDecodeJs/imageDecode; keep Glide presentation in the host but expose a typed source-image capability. Explore execution depends on source-keyed InfoMap, per-source locking, ACache/LRU and invalidation; preserve these behind ExploreCatalogPort, not Explore UI.
- **Gate:** this is source/declaration evidence only. No source/build/test/Gradle/CI edits or execution. Phase A remains open; compiled ABI/R8, complete variant dependency closure and exact image/Explore Host Port signatures remain blockers. Phase B is not authorized; Stage-0 is not accepted; Stage-1 remains NO-GO.

See audit §24.86 for path-backed evidence and exact remaining proof.


### 4.27.4 Task 026 Phase A continuation — source-image and Explore typed-port refinement (2026-11-25)

- **Review HEAD before this documentation update:** `3ae580f6ee9e9c983a8fcf1d1f23ff14bc8b2b8c`; comparison against the branch ref was identical (0 ahead/behind). Pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **New source evidence:** `OkHttpStreamFetcher` resolves the active BaseSource from the image request's source origin, uses `AnalyzeUrl.getGlideUrl()` for URL analysis and Cookie/login headers, performs the host OkHttp fetch, then invokes `ImageUtils.decode` with source/book context. BookSource cover images use `coverDecodeJs`; chapter/manga images use `ContentRule.imageDecode`. Fetcher cancel/cleanup closes/cancels the call, coroutine, stream and response body. Glide presentation remains host-only; source-aware request preparation and decode remain Runtime capabilities.
- **Explore evidence:** `BookSource.exploreKinds()` uses result key MD5(source URL + explore URL), per-source Mutex, in-memory result map, persistent `ACache("explore")`, and a source-keyed `InfoMap` LRU (capacity 99). `WebBook.exploreBookAwait()` passes the same InfoMap into AnalyzeUrl, so this mutable object is shared source execution context, not just UI state. ExploreAdapter rendering/Flexbox/navigation stays host-side. Current mutex registry is a plain HashMap; thread-safety must not be assumed from the per-source Mutex alone.
- **Port proposal refinement:** source-image port must preserve analyzed URL, headers/cookies, source identity, Book binding, byte/stream decode, cancellation and close/error semantics without leaking Glide types. ExploreCatalogPort must separately preserve InfoMap identity, source-scoped serialization, result cache keying, persistent/memory cache and invalidation; result-cache clear does not currently clear InfoMap.
- **Audit record:** details, paths, behavior boundaries and required differential cases are in audit §24.87. This is source evidence and a contract proposal only; exact typed signatures and dynamic parity remain open.
- **Changes:** audit-only commit `df2a4ff6128f961ac4f33ef58d23f4ae272c64f3` adds §24.87. No production source, Gradle, tests, workflow, schema, UI or product files changed. No build/test/dependency resolution/DEX/R8/CI/device run.
- **Disposition:** Task 026 Phase A remains in progress/not closed; Phase B is not authorized; Task 027 remains blocked; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; complete Legado compatibility remains **NOT ESTABLISHED**.
- **Next:** continue Phase A only for compiled JVM/Rhino ABI/reflection descriptors and complete exact-HEAD Gradle variant resolution where an authorized clean runner is available. Then consolidate the path-exact Phase B proposal and stop for review; do not start source migration.


### 4.27.5 Task 026 Phase A continuation — Explore invalidation and source lifecycle callers (2026-11-25)

- **Review HEAD:** `31f439c4e0a5604c0f6698b2eee88a2a961aad36`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`. The branch was unchanged from the prior checkpoint before this documentation-only update.
- **New caller evidence:** `BaseSource.refreshExplore()` is a source-visible API that clears Explore result caches and rejects main-thread calls. `BookSourceEditViewModel.save`, `JsSourceUpsert.save` and `McpSourceStore.saveDeclarative` have distinct same-URL/changed-URL update sequences; they invalidate Explore results, SharedJsScope/jsLib, SourceConfig and concurrent-rate state in different combinations. `SourceHelp.deleteBookSourceKeys` wraps source-row + persisted source-variable deletion in a Room transaction, then removes SourceConfig and clears in-memory source variables; its helper clears source-keyed shared JS global state but does not explicitly evict Explore InfoMap or call Explore result invalidation.
- **Cache-state correction:** Explore result cache (keyed by source URL + explore URL), source-scoped InfoMap LRU, SharedJsScope/jsLib, SourceConfig, persisted/in-memory source variables and concurrent-rate state are distinct domains. `clearExploreKindsCache()` removes only the result cache entries. Do not merge them into one cache-clear port or claim source deletion currently evicts InfoMap.
- **Port proposal:** keep explicit operations for result invalidation, stable InfoMap acquisition, source execution state invalidation, source catalog transaction, SourceConfig/variable cleanup and concurrency-state reset. Source URL change is an old-key deletion plus new-key insert with target-conflict checks, not an unqualified rename.
- **Audit record:** path-backed caller details and remaining races/cleanup gaps are in audit §24.88. This is static source evidence only; exact typed signatures and dynamic parity remain open.
- **Changes:** audit-only commit `e5996d7458e2d777866a02becab2621f0492550d` adds §24.88. No production source, Gradle, tests, workflow, schema, UI or product files changed. No build/test/dependency resolution/DEX/R8/CI/device run.
- **Disposition:** Task 026 Phase A remains in progress/not closed; Phase B is not authorized; Task 027 remains blocked; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; complete Legado compatibility remains **NOT ESTABLISHED**.
- **Next:** continue Phase A only for remaining source lifecycle/SharedJsScope callers, compiled ABI/reflection evidence and exact Gradle variant resolution. Then consolidate path-exact Host Port signatures and a Phase B proposal and stop for review; do not start source migration.


### 4.27.6 Task 026 Phase A continuation — SharedJsScope and source mutation entry-point closure (2026-11-25)

- **Review HEAD:** `929a3053937e19216ebbee851e3d5a6ad513fb25`; pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`. Audit details are in §24.89.
- **Shared global invalidation:** source-key deletion calls `ScriptBindings.removeSharedGlobalStatesBySource(sourceClass, MD5(sourceUrl))`, removing source-specific global objects/generation state. `SharedJsScope.remove(jsLib)` is different: it removes the weak cached library scope, URL-backed jsLib file cache entries, and all shared global states under that jsLib hash prefix (potentially affecting multiple sources sharing the library). The former does not remove the library scope; neither operation cancels already-running source scripts by itself.
- **Explore InfoMap:** three layers are now explicit: persistent `CacheManager("infoMap_<sourceUrl>")`, generic cache memory LRU, and `ExploreAdapter.exploreInfoMapList` object LRU (99). Source deletion removes the persisted source-variable/cache rows and generic memory cache prefixes, but no production caller evicts the separate InfoMap object LRU. Exact explicit LRU removal appears only in ExploreRefresh instrumentation teardown. Delete/recreate under the same URL therefore has a static stale-object risk; dynamic manifestation remains unverified.
- **Source mutation callers:** in addition to edit UI, JS upsert and MCP declarative save, pinned production paths include direct `SourceHelp.insertBookSource` imports, HTTP `BookSourceController.saveSource/saveSources`, `RuleUpdate.cacheSource`, legacy import and backup restore direct DAO inserts, and generic BookSource DAO update/metadata paths. Direct DAO insert uses Room REPLACE and does not invoke the same invalidation sequence as the explicit upsert/delete orchestrators. Deletion callers from management, Explore, change-source, HTTP API and MCP converge on SourceHelp cleanup.
- **Host Port refinement:** preserve distinct source-key shared-global invalidation, jsLib-hash-wide scope/file/global invalidation, Explore result invalidation, InfoMap object identity/persistence, and caller-specific direct-insert versus orchestrated-upsert semantics. A centralized lifecycle service would be a deliberate compatibility change requiring differential proof.
- **No dynamic evidence:** no race, stale InfoMap, in-flight Rhino, or multi-source shared-jsLib behavior tests were run. No source/build/test/Gradle/workflow/schema/UI/CI changes or executions.
- **Status:** Task 026 Phase A remains open/not closed; Phase B is not authorized; Task 027 remains blocked; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; full Legado compatibility remains **NOT ESTABLISHED**.
- **Next:** close remaining generic update/restore/API caller semantics and exact Host Port signatures, consolidate the path-exact Phase B proposal, then stop for review. No source migration.


### 4.27.7 Task 026 Phase A continuation — DAO update and import/restore/API lifecycle closure (2026-11-25)

- **Review HEAD before this documentation update:** `c5e8d99b910e5af9f0e33e34af5344f0bc30b5d6`; branch unchanged from the previous checkpoint. Pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **DAO update semantics:** Room `BookSourceDao.insert()` is transactional REPLACE + check-state reset; `update()` updates an existing full row and resets check state only when `BookSource.checkContent()` changes. That projection normalizes order/enabled/explore-enabled/update-time/respond-time/weight. Narrow SQL methods handle enabled/explore/order. Group updates go through full update and group is part of checkContent, so group-only changes may reset source-check state without invalidating JS/Explore caches.
- **Caller closure:** BookSource management group operations, reader disable-source and change-source disable/reorder paths were classified. The generic ViewModel update wrapper has no direct production caller found in the searched tree. Direct write routes include manual import and RuleUpdate subscription through `SourceHelp.insertBookSource`, HTTP `saveSource/saveSources` direct DAO inserts, legacy `ImportOldData`, and backup `Restore`; these are distinct from edit UI, JS Upsert and MCP declarative orchestrators.
- **Import/API behavior:** SourceHelp import applies the 18+ filter and asynchronous order normalization; HTTP JSON save validates nonblank name/URL and batch save can accept only a subset of entries; legacy and backup restore directly REPLACE. These direct paths do not automatically run the same jsLib/Explore/SourceConfig/concurrent-rate cleanup as explicit upsert/edit/MCP.
- **Host Port decomposition:** audit §24.90 proposes separate contracts for catalog transaction/mutation origin, source-key shared-global invalidation, jsLib-wide scope/file/global invalidation, Explore result cache, stable InfoMap context, persisted+memory source variable/config cleanup, and concurrent-rate reset. Signatures remain provisional; preserve partial batch, transaction, cancellation and in-flight execution semantics.
- **No dynamic evidence:** same-URL direct REPLACE during active Rhino/Explore execution, state cleanup races, backup restore sequencing and API partial batch behavior were not run. No production/build/test/Gradle/workflow/schema/UI/CI changes or execution.
- **Status:** Task 026 Phase A remains open/not closed; Phase B is not authorized; Task 027 remains blocked; Stage-0 remains `in_progress / not accepted`; Stage-1 remains **NO-GO**; full Legado compatibility remains **NOT ESTABLISHED**.
- **Next:** consolidate path-exact Host Port signatures and the Phase B proposal using §§24.85–24.90, with compiled ABI/reflection and resolved Gradle variant blockers explicitly retained; then stop for review. No source migration.

### 4.27.8 Task 026 Phase A — path-exact Host Port signatures and conditional Phase B proposal (2026-11-25)

- **Review HEAD:** `a692f2e57ef70001bc3ef4fced1d66e1667b02f7`; comparison from the previous checkpoint is identical. Pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Contract placement:** current `RuntimeHostPort.kt` carries model-neutral `SourceIdentity`/`SourceExecutionContext` and browser/UI/verification ports; `SourceRuntime.evaluate()` remains string-map input/string output. Proposed model-typed ports belong under `:legado-runtime/src/main/kotlin/io/legado/runtime/host/`; `:integration-contract` remains model-neutral, `:legado-runtime` may depend on `:integration-contract` and `:legado-rhino` but never `:app`, and `:app` implements the adapters.
- **Proposed families:** source catalog mutation with origin/batch policy; source-key shared-global invalidation; separate jsLib-hash scope/file/global invalidation; Explore result cache vs stable mutable InfoMap; source variables vs RuleData big-value backing; login session vs CookieStore; CacheManager text/file/memory/TTL; HTTP stream/cancellation; source-image transport/decode; UI/login callbacks; replacement timeout/disable/restart. Signatures remain sketches, not frozen APIs.
- **Mutation policy:** manual import and RuleUpdate use SourceHelp's 18+ filter plus transactional REPLACE/check-state reset; HTTP JSON batch is per-item and may partially accept; legacy/backup restore directly REPLACE; edit/JS-upsert/MCP have distinct invalidation sequences. Preserve caller origin, old/new keys, transaction boundaries, cancellation and partial results; do not invent a universal upsert.
- **Phase B path proposal:** exact source candidates and exclusions are in audit §24.91.4. Candidate paths include `legado-runtime/build.gradle.kts`, `legado-runtime/src/main/**`, `legado-runtime/src/test/**`, `legado-runtime/consumer-rules.pro` after ABI review, `BadcatReadingEngineFactory.kt`, new app runtime-host adapters and narrowly necessary app tests/build wiring. Pinned upstream remains read-only; no Legado product UI/Reader, Glide or Media3 is imported by default.
- **Blockers:** compiled JVM/Rhino ABI/reflection and Release R8 proof; exact Debug/Release/unit/AndroidTest dependency variants; dynamic mutation/cache/in-flight/image/Explore lifecycle tests remain NOT MET. Prior dependency reports contain unresolved modules and the attached runner is stale/dirty.
- **Changes:** documentation-only. No source, Gradle, test, workflow, schema, UI or product file changed; no dependency resolution, build, test, DEX/R8, CI or device run.
- **Gate:** Task 026 Phase A remains open/not closed; Phase B is not authorized; Task 027 remains blocked; Stage-0 `in_progress / not accepted`; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Stop:** proposal is ready for review. Do not start migration until remaining Phase A evidence is closed and explicit authorization is given. See audit §24.91.


### 4.27.9 Task 026 Phase A continuation — exact-HEAD variant graph and bounded compile/ABI slice (2026-11-25)

- **Review HEAD:** `55b8f102e8b0b2e1c27a3d44530accf06d5a07e4`; pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`. Fresh runner checkout and upstream submodule were clean at these exact identities.
- **Variant reports:** 44 unique current-Badcat resolvable configurations completed successfully with zero unresolved `FAILED` entries: `:app`, `:legado-runtime`, `:legado-rhino`, and `:legado-runtime-poc` each have 10 (Debug/Release main, Debug/Release UnitTest, Debug AndroidTest compile/runtime); `:integration-contract` has 4 (main + test compile/runtime). No resolvable Release AndroidTest compile/runtime configuration was exposed. The three early “configuration not found” results came from incorrect app configuration names and are not dependency failures; the correct six app test configurations all passed.
- **Evidence:** 44-report manifest SHA-256 `211b033ba3efb592b862c6a90d404b45c5558e80491567ba6cd4bcd4e537aba9`; exact configuration inventory `configurations.log` SHA-256 `a2412fb8d092fcdee0d274b8c58b0f3116d02dca903e72a99b9fc69166835ce3`. One pinned upstream `:app:dependencies --configuration appDebugRuntimeClasspath` report also completed successfully with zero unresolved entries. These are dependency graph reports, not proof that every artifact file is staged or that the not-yet-imported authentic Runtime closure is resolved.
- **Bounded compile:** `:legado-runtime:compileDebugKotlin`, `:legado-runtime:compileReleaseKotlin` and `:app:compileDebugKotlin` completed with exit code 0; `BUILD SUCCESSFUL in 40s` (53 actionable tasks: 10 executed, 43 from cache). The HtmlUnit CoreJS JAR was temporarily symlinked from the exact upstream pin and removed by the exit cleanup; both paths matched the pinned SHA. No repository source/build file changed.
- **Compiled ABI boundary:** selected public JVM descriptors were captured for the existing rule models and Rhino `RhinoScriptEngine` / `ScriptBindings`. The current `:legado-runtime` JAR still lacks authentic upstream BaseSource/BookSource/BaseBook/Book/BookChapter/SearchBook/AnalyzeRule/AnalyzeUrl/JsExtensions/SharedJsScope/JsSourceEngine classes. Thus this is partial current-module ABI evidence, not proof of the missing upstream caller/reflection surface or Release R8 retention.
- **No behavior claim:** no tests, dynamic mutation/cache/in-flight/image/Explore lifecycle checks, APK/DEX/R8, CI or device run occurred. Badcat still has no app Release minification configuration from which to prove optimized bridge retention.
- **Gate:** current existing module variant graph is resolved; proposed authentic Runtime closure and compiled upstream ABI/reflection remain open. Phase A remains `in_progress / not closed`; user authorization for Phase B is acknowledged but the required path-exact addendum and Phase A gate must be completed before source/build/test edits. Task 027 remains blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** continue the non-production compiled ABI/reflection and caller-closure proof for the fixed upstream, then reconcile the exact path/port/test/build addendum. Do not start source migration or Task 027.


### 4.27.10 Pinned upstream Rhino ABI probe — app-level compile remains pending (2026-11-25)

- The exact pinned upstream `:modules:rhino` Debug module compiled; `:modules:book` Java compile/bundle tasks also completed. Upstream Rhino class JAR SHA-256: `54b988056172a61c3a5590dc2f463f4b6028db09b16d38566d6f87e6ca5b98f6`. Public descriptors for upstream `RhinoScriptEngine` and `ScriptBindings` are captured and hashed in audit §24.93.
- The full upstream app compile uses `:app:compileAppDebugKotlin` (the unqualified `:app:compileDebugKotlin` task name is ambiguous). The corrected command reached `:app:kspAppDebugKotlin` but stalled for over seven minutes while a Gradle included-build thread waited in an HTTPS artifact download. No app-level KSP outputs or target JS/BaseSource classes were produced. The run was stopped with SIGTERM; exit code 143 means **cancelled/incomplete**, not a source compile failure or pass.
- Upstream source remains clean at the pinned SHA. No production source/build/test changes, tests, APK, DEX/R8 or device validation occurred.
- **Gate:** compiled upstream Rhino module ABI is partially evidenced; app-level BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions ABI, inherited reflection and Release R8/DEX retention remain NOT MET. Phase A remains open; Phase B migration and Task 027 remain blocked; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** identify the exact stalled Gradle artifact transfer or use a narrower non-production compile probe, then complete caller-specific ABI/reflection and R8 retention evidence. Do not begin source migration.


### 4.27.11 Task 026 Phase A continuation — compiled Rhino ABI comparison and runner recovery (2026-11-25)

- **Review HEAD before this documentation update:** `ebe8674c6c1529ca7aa17f5702c335586e1289c3`; compared with the previous source checkpoint `55b8f102e8b0b2e1c27a3d44530accf06d5a07e4`, four intervening commits changed only the two audit/overview documents. Pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Runner recovery:** clean Task-026 checkout remains at source HEAD `55b8f102...`; upstream submodule exact pin and clean status verified. The cancelled upstream App compile exited 143 at 17:56:01 +08:00; no active Gradle client remains. A Gradle daemon/Kotlin daemon are resident. The saved thread dump confirms a Gradle included-build worker blocked reading an HTTPS artifact transfer, but the exact artifact coordinate/URL is not present in available logs. No process was killed and no build was retried.
- **Compiled ABI comparison:** pinned upstream Rhino Debug module compiled; Badcat and upstream `ScriptBindings` descriptor blocks match exactly. The common public RhinoScriptEngine method descriptors match, but Badcat's local engine adds `callSourceFunction(Function, VarScope)` and changes `topLevelThis` to resolve JS-visible `globalThis` before fallback. The added method is called by `RhinoSourceScriptRuntime.kt:455`; this local extension/semantic delta is now explicitly recorded in audit §24.94. Internal Kotlin helper names carry different module-mangled suffixes. This does not close upstream app-level ABI/reflection.
- **Limits:** no upstream App KSP output, BaseSource/AnalyzeRule/AnalyzeUrl/JsExtensions compiled classes, inherited reflection proof, Release R8/DEX, tests, APK, CI or device validation. Exact HTTPS artifact remains unidentified.
- **Changes:** audit-only evidence record §24.94 and this overview checkpoint. No production source, Gradle, test, workflow, schema or UI files changed.
- **Disposition:** Task 026 Phase A remains in_progress/not closed; Phase B has not started; user authorization intent is acknowledged but does not waive the Phase A gate; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue a bounded non-production method/reflection probe using the already compiled upstream Rhino artifacts and source caller maps; do not repeat the full upstream App compile until the HTTPS artifact transfer is identified or a bounded reproducible path is available.


### 4.27.12 Task 026 Phase A continuation — Rhino class-file visibility and synthetic method evidence (2026-11-25)

- **Inspection:** read-only `javap -p -v` on the already compiled Badcat and pinned upstream Debug Rhino JARs; no new build or dependency resolution.
- **Result:** Badcat `RhinoScriptEngine` has 15 declared methods vs upstream 14. Both have four ACC_SYNTHETIC entries; Kotlin default helpers/accessors are public static synthetic, while Kotlin `internal` `createScriptException$<module>` and `topLevelThis$<module>` compile as public final non-synthetic methods with module-specific names. Badcat's additional `callSourceFunction(Function, VarScope)` is public final, non-synthetic and has no upstream counterpart. Both `ScriptBindings` classes have 11 declared methods/five synthetic entries; their public descriptor blocks match, and source files are byte-identical. Details and dump hashes are in audit §24.95.
- **Boundary:** this is Debug class-file visibility evidence only; it does not prove runtime reflection, app-level inherited JsExtensions/BaseSource ABI or Release R8/DEX retention.
- **Changes:** audit-only §24.95 plus this checkpoint; no production/build/test/workflow/schema/UI edits and no build/test/CI/DEX/R8/device execution.
- **Disposition:** Task 026 Phase A remains open/not closed; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue the remaining pinned app-level caller/reflection closure without retrying the stalled full App compile until the external artifact transfer is identified or a reproducible bounded route is established.


### 4.27.13 Task 026 Phase A continuation — KSP transfer candidate narrowing (2026-11-25)

- **Finding:** pinned upstream app uses KSP 2.3.12 with Room compiler 2.8.4 and Glide KSP 5.0.7. The KSP plugin's compiled `KspAATask` constructs a detached compiler classpath containing `symbol-processing-aa-embeddable:2.3.12` and `kotlinx-coroutines-core-jvm:1.10.2` as well as KSP API/common dependencies.
- **Cache check:** API/common-deps and Kotlin stdlib JARs are cached; the AA-embeddable and coroutines entries have metadata only and no JAR; Room compiler 2.8.4 and Glide KSP 5.0.7 cache paths are empty. Four artifact candidates are therefore not locally staged for the KSP task.
- **Limit:** the earlier TLS thread dump did not record which of these artifacts was being downloaded. This narrows likely transfer candidates but does not identify the exact stalled artifact. No Gradle command or build was run in this diagnostic slice. Details and bytecode evidence are in audit §24.96.
- **Next:** use a separately registered, bounded diagnostic with explicit transfer logging or controlled artifact staging to identify the actual failed transfer before retrying the full upstream app compile. Keep exit 143 as cancelled/incomplete, not build failure or pass.
- **Disposition:** Phase A remains open/not closed; no Phase B source migration; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.


### 4.27.14 Task 026 Phase A — bounded KSP transfer diagnostic registered (2026-11-25)

- **Task ID:** `R3-S0-T026-KSP-TRANSFER-DIAGNOSTIC-026A`; status registered/not started; full scope and stop conditions in audit §24.97.
- **Snapshot:** branch HEAD before registration `83726efc09575bd7a58693bb6f8034dcf4597287`; runner source checkout `55b8f102...` remains clean and differs only by documentation-only commits; pinned upstream `330e606c...`.
- **Scope:** one HTTPS GET per four uncached KSP artifact candidates (KSP AA-embeddable 2.3.12, coroutines 1.10.2, Room compiler 2.8.4, Glide KSP 5.0.7) into `/tmp`, 10-second connect / 30-second total timeout, no retries. No Gradle/build/KSP/test/source edits or cache injection.
- **Next:** execute only this bounded artifact-transfer check; record status/size/hash/time and clean temporary downloads. It does not prove which artifact was stalled in the prior cancelled build. Keep Phase A open and Stage-1 NO-GO.


### 4.27.15 Task 026 Phase A continuation — KSP AA-embeddable transfer bottleneck reproduced (2026-11-25)

- **Diagnostic result:** one bounded HTTPS GET per four KSP artifact candidates. `symbol-processing-aa-embeddable:2.3.12` returned HTTP 200 but timed out at 30 seconds after 6,360,065 of 80,992,357 bytes; curl exit 28. Coroutines 1.10.2 (1,479,317 bytes), Room compiler 2.8.4 (1,670,310 bytes) and Glide KSP 5.0.7 (59,630 bytes) all downloaded successfully in under two seconds.
- **Assessment:** KSP AA-embeddable is the high-confidence likely cause of the previous App KSP stall: it is an exact KSP task dependency, was absent from cache, is ~81 MB, and its transfer is independently slow. The original thread dump still does not prove that this was the exact historical URL.
- **Cleanup:** all downloaded/partial JAR files were deleted after recording transfer metadata and hashes; no Gradle cache or repository source/build files were modified. Results file SHA-256 and per-artifact hashes are in audit §24.98.
- **Next:** preregister and perform a checksum-verified full JAR transfer/staging step before retrying App KSP. Do not treat the cancelled exit-143 app compile as a pass or source failure.
- **Disposition:** Phase A remains open/not closed; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.


### 4.27.16 Task 026 Phase A — verified KSP AA-embeddable staging preregistered (2026-11-25)

- **Task ID:** `R3-S0-T026-KSP-AA-ARTIFACT-STAGING-026B`; registered/not started; scope and stop conditions in audit §24.99.
- **Goal:** obtain the exact 80,992,357-byte `symbol-processing-aa-embeddable:2.3.12` JAR via bounded HTTPS range requests, using Maven module metadata as the authoritative size/SHA-256 source.
- **Boundaries:** download only to `/tmp`; verify each 5 MiB range and final checksum; no Gradle cache injection, local repo injection, build/KSP/test/source edit, or app compile retry. Stop on unsupported Range, non-206, timeout, wrong size or checksum mismatch.
- **Next:** execute the metadata + range staging probe only. If verified, separately preregister any Gradle staging/compile action; Phase A remains open and Stage-1 NO-GO.


### 4.27.17 Task 026 Phase A continuation — KSP range probe passes, checksum metadata missing (2026-11-25)

- **Result:** the Maven `.module` endpoint for KSP AA-embeddable returned HTTP 404, so the authoritative SHA-256 condition for full staging was not met. A single 1 MiB range request returned HTTP 206 with `Content-Range: bytes 0-1048575/80992357` in 1.87 seconds; byte-range retrieval is supported and total JAR size is confirmed as 80,992,357 bytes.
- **Cleanup:** the probe and 404 response were deleted after recording; only the small result record remains under `/tmp`. No repository or Gradle cache changed. See audit §24.100.
- **Next:** preregister a same-repository checksum-sidecar check (`.sha256`, then `.sha1` if necessary) before any full range download. Do not assemble or stage the JAR without a verifiable published checksum.
- **Disposition:** Phase A remains open/not closed; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.


### 4.27.18 Task 026 Phase A — KSP checksum-sidecar lookup registered (2026-11-25)

- **Task ID:** `R3-S0-T026-KSP-AA-CHECKSUM-SIDECAR-026C`; registered/not started; scope in audit §24.101.
- **Goal:** query only same-repository `.jar.sha256`, `.jar.sha512`, then `.jar.sha1` sidecars for the exact KSP AA-embeddable 2.3.12 artifact, one request each, bounded 10s connect/15s total, no retries.
- **Gate:** accept SHA-256/SHA-512 as integrity anchor; if only SHA-1 exists, stop for a separate decision; if none exists, stop without downloading the full JAR.
- **Next:** perform only this checksum-sidecar lookup. No full download, Gradle cache mutation, build or source edit.


### 4.27.19 Task 026 Phase A — KSP AA-embeddable checksum verified; chunk transfer registered (2026-11-25)

- **Checksum:** the exact Maven Central `.jar.sha256` sidecar returned HTTP 200 and authoritative SHA-256 `862f0042fc2467cdcf7752e15880f97396a29e2469741dc6453f37584f1b4c69` for `symbol-processing-aa-embeddable:2.3.12`. Expected artifact size remains 80,992,357 bytes.
- **Task ID:** `R3-S0-T026-KSP-AA-CHUNKED-TRANSFER-026D`; registered/not started. It permits 16 sequential 5 MiB HTTPS range requests, each bounded at 60 seconds, with exact HTTP 206/Content-Range/size validation and final SHA-256 verification. Full details in audit §24.103.
- **Boundary:** only `/tmp` artifact staging; no Gradle cache/local repo injection and no compile retry. If any chunk fails or final size/hash differs, delete partial data and stop.
- **Disposition:** Phase A remains open/not closed; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.


### 4.27.20 Task 026 Phase A — 5 MiB KSP AA transfer chunk failed safely (2026-11-25)

- **Result:** first 5 MiB range completed in 21.17s; second range timed out at 60s after 4,848,598/5,242,880 bytes. The transfer script stopped and deleted all chunks/headers/assembled output; no partial JAR remains. Failure manifest SHA-256 and exact ranges are in audit §24.104.
- **Next:** 2 MiB sequential range plan registered as R3-S0-T026-KSP-AA-CHUNKED-TRANSFER-026E, 45s per chunk, same verified Maven SHA-256. Full scope in audit §24.105.
- **Boundary:** no cache injection or compile retry; only a fully verified artifact may be retained in /tmp. Phase A remains open and Stage-1 NO-GO.


### 4.27.21 Task 026 Phase A — KSP AA-embeddable JAR fully verified (2026-11-25)

- **Result:** 39 sequential range chunks completed; exact 80,992,357-byte JAR SHA-256 matches Maven Central sidecar: 862f0042fc2467cdcf7752e15880f97396a29e2469741dc6453f37584f1b4c69. ZIP integrity passed. Chunk manifest SHA-256: 5ceb175a4a8bba47954a266cc8bc31447b94ca831c6f986337765449d3c37a74.
- **Location:** verified JAR retained only at /tmp/task026-ksp-aa-full/symbol-processing-aa-embeddable-2.3.12.jar; chunk files/headers removed; no repository or Gradle cache manually modified.
- **Next task:** R3-S0-T026-KSP-AA-LOCAL-STAGING-026F registered to prepend a temporary Maven repo via a one-run init script and execute only upstream :app:kspAppDebugKotlin. No full App compile retry. Full details in audit §24.107.
- **Disposition:** Phase A remains open/not closed; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 NO-GO; full Legado compatibility NOT ESTABLISHED.

### 4.27.22 Task 026F preflight amendment — central repository policy preserved (2026-11-25)

- Pinned upstream `settings.gradle` uses `FAIL_ON_PROJECT_REPOS`; a project-level repository injection would violate the build's repository policy.
- Task 026F is amended to add the temporary local Maven repository through the one-run init script at `settings.dependencyResolutionManagement.repositories`, first in order and filtered to exactly `com.google.devtools.ksp:symbol-processing-aa-embeddable`. No tracked settings/build file or Gradle cache is edited. Details in audit §24.108.
- The verified 80,992,357-byte JAR is present under `/tmp` with the expected Maven Central SHA-256 and passes ZIP integrity. Runner source checkout is clean at `55b8f102...`; upstream is clean at `330e606c...`. The stale `/root/stage0-runner/repo-git` checkout is dirty and must not be used.
- Next: copy exact cached POM + verified JAR + SHA sidecar to the temporary repository, then execute only `:app:kspAppDebugKotlin` with the amended filtered settings-level repository. No App compile/build/test/CI. Phase A remains open; Stage-0 not accepted; Stage-1 NO-GO.
### 4.27.23 Task 026F execution amendment — avoid wrapper bootstrap transfer (2026-11-25)

- First launch exited 143 before Gradle startup because `./gradlew` began downloading the Gradle 8.14.4 distribution; only 482,772 bytes arrived in 43 seconds and no KSP task ran. The temporary local Maven repo and init script were cleaned. This is a cancelled wrapper bootstrap, not a KSP failure.
- The runner already has Gradle 8.14.4 at `/root/stage0-runner/gradle-8.14.4/bin/gradle`; with `JAVA_HOME=/root/stage0-runner/jdk21` it reports Microsoft OpenJDK 21.0.13.1. Task 026F is amended to use this exact installed Gradle binary, preserving the same one-task `:app:kspAppDebugKotlin` boundary and filtered settings-level local Maven repository. Details in audit §24.109.
- Next: repeat only the KSP task with the installed toolchain. No `:app:compileAppDebugKotlin`, assemble, tests, CI, DEX/R8/APK/device or source migration. Phase A remains open; Stage-0 not accepted; Stage-1 NO-GO.
### 4.27.24 Task 026F result — filtered local repository works; JDK 21 was not passed to Gradle (2026-11-25)

- The single `:app:kspAppDebugKotlin` invocation added the exact filtered settings-level local Maven repository (marker present once) and made zero remote AA JAR fetches, but exited 1 before KSP task execution because Gradle daemon launched on Java 17 and could not find required Java 21.
- This is an invocation/toolchain selection failure, not an artifact, KSP processor or source failure. The 89 files found under `app/build/generated/ksp` pre-existed and are not credited as generated by this run. Log/result hashes and cleanup are in audit §24.110.
- Temporary local Maven repository/init script were removed; tracked source worktrees remain clean. The verified JAR remains in `/tmp`.
- Next: rerun only `:app:kspAppDebugKotlin` with `JAVA_HOME=/root/stage0-runner/jdk21` and matching PATH exported in the same process, using installed Gradle 8.14.4. Phase A remains open; Stage-0 not accepted; Stage-1 NO-GO.
### 4.27.25 Task 026F result — JDK 21 is selected; Android SDK variables missing (2026-11-25)

- The KSP-only invocation selected Gradle 8.14.4 under Microsoft OpenJDK 21.0.13.1 and resolved the exact task path, but exited 1 before task dependency resolution because `ANDROID_HOME`/`ANDROID_SDK_ROOT` were unset and no upstream `local.properties` exists.
- Existing SDK root is `/root/stage0-runner/android-sdk`; set both environment variables to that existing directory in the next invocation. Do not edit `local.properties` or install SDK components. This is an environment blocker, not a KSP/source failure. See audit §24.111.
- Temporary Maven repo/init script were removed; tracked worktrees clean; verified KSP JAR remains in `/tmp`. No KSP action or generated-output pass is claimed.
- Next: rerun only `:app:kspAppDebugKotlin` with JDK21 and both Android SDK environment variables set. Phase A remains open; Stage-0 not accepted; Stage-1 NO-GO.

### 4.27.25 Task 026F attempt 2 — Java 21 selected; Android SDK 36 missing (2026-11-25)

- Attempt 2 used the preinstalled Gradle 8.14.4 binary with Microsoft OpenJDK 21.0.13.1. It stopped before KSP task execution/dependency resolution with `SDK location not found`; exit 1. This is an environment prerequisite failure, not a KSP source/processor failure or pass. Log SHA-256: `ef9ed8acd223297d51a46c13b59e5c01bb64947c826ed05a56ec769881ed1039`.
- Pinned upstream requires compile SDK 36 and Java 21. Runner has no ANDROID_HOME/ANDROID_SDK_ROOT; `/usr/lib/android-sdk` contains only Debian build-tools 29.0.3, no platform 36/android.jar and no sdkmanager. No Android SDK package was installed.
- **Next prerequisite:** obtain explicit acceptance of the Android SDK license, then preregister an isolated temporary SDK 36 setup (platform + required build-tools) before rerunning only `:app:kspAppDebugKotlin`. Official command-line tools package/checksum and the exact environment evidence are in audit §24.111.
- **Disposition:** Task 026F blocked pending SDK prerequisite; Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.


### 4.27.26 Task 026F correction — runner SDK 36 already exists; export its environment (2026-11-25)

- Correction to §4.27.25: a runner-specific Android SDK was present at `/root/stage0-runner/android-sdk`; the earlier preflight inspected only the Debian SDK path. It contains API 36 platform (revision 2), build-tools 35.0.0 and 36.0.0, command-line tools 19.0 and SDK license files. No installation or license acceptance is needed or was run.
- Attempt 2 failed because `ANDROID_HOME`/`ANDROID_SDK_ROOT` were unset, not because SDK 36 was absent. Verified `android.jar` SHA-256 and component details are in audit §24.112.
- **Next:** same Task 026F, attempt 3; export `JAVA_HOME=/root/stage0-runner/jdk21`, both Android SDK variables to `/root/stage0-runner/android-sdk`, and matching PATH. Use the preinstalled Gradle 8.14.4 and exact filtered temporary Maven repo; execute only `:app:kspAppDebugKotlin`. No SDK install/license command, other task, App compile, test, CI or source migration.
- **Disposition:** Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.

### 4.27.27 Task 026F result — pinned upstream App KSP task passed (2026-11-25)

- **Status:** Task 026F complete as a bounded upstream KSP-generation probe; it does not close Task 026 Phase A.
- **Identity:** current integration branch HEAD 288d14f178bc807f7729711a77dad0611f487b07; runner source snapshot 55b8f102e8b0b2e1c27a3d44530accf06d5a07e4; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a. GitHub compare shows the 40 commits between runner source snapshot and current branch changed only docs/audits/legado-runtime-3.0-stage0-architecture-audit.md and this overview; production/build/test inputs are identical. Runner and upstream tracked worktrees were clean.
- **Attempt 3:** exited 1 before task execution because the temporary init script was absent when Gradle finally processed the invocation; log records a 10-minute daemon-start delay. This is an invocation/launcher lifecycle failure, not a KSP or source failure. No KSP action is credited.
- **Attempt 4:** used Microsoft OpenJDK 21.0.13.1, existing Android SDK 36, installed Gradle 8.14.4, one worker, no parallelism, and -Xmx1g. The settings-level init script placed a temporary Maven repository first and filtered it to exactly com.google.devtools.ksp:symbol-processing-aa-embeddable. Only :app:kspAppDebugKotlin was invoked.
- **Result:** exit code 0; BUILD SUCCESSFUL in 1m 41s; 45 actionable tasks (7 executed, 38 up-to-date). The exact staging marker appeared once and the log contained zero remote AA JAR fetch lines. Both Room and Glide KSP providers loaded; KSP generated 89 files (88 Java, 1 Kotlin), including AppDatabase_Impl, DAO implementations, Room migration implementations and GeneratedAppGlideModuleImpl.kt.
- **Evidence:** log /root/stage0-runner/task026-clean/task026f-attempt4/ksp-only-attempt4.log, SHA-256 92c792fb20362198ed35bff1b42389b9abd7071c14acf0f812bc30d244873e03; result SHA-256 d3ee7ea75ff7d832b0b7668c00d61ce4a14786e5e8797f0797518e89dface8ca; 89-file inventory SHA-256 4ec12171896375cd5842778b06ab02f04ad5e491273867d6e585aa0bebb84597. Verified AA JAR SHA-256 remains 862f0042fc2467cdcf7752e15880f97396a29e2469741dc6453f37584f1b4c69.
- **Cleanup:** temporary Maven repository and init script were removed after verification. The verified JAR remains in /tmp/task026-ksp-aa-full/; generated KSP outputs and logs remain as runner evidence. Both tracked worktrees report clean.
- **Boundary:** this proves upstream App KSP task execution and generated Room/Glide sources only. It does not prove app Kotlin/Java compilation, compiled BaseSource/AnalyzeRule/JsExtensions ABI, reflection, Release R8/DEX, tests, APK, runtime behavior or full Legado compatibility.
- **Disposition:** Task 026F acceptance met. Task 026 Phase A remains in_progress / not closed; Phase B remains blocked by the remaining Phase A gates; Task 027 remains blocked; Stage-0 remains not accepted; Stage-1 remains NO-GO; complete Legado compatibility remains NOT ESTABLISHED.
- **Next:** Task 026G is separately preregistered below for the exact pinned upstream app-level Kotlin compile probe. Do not widen it to assemble, tests, DEX/R8 or source migration.


### 4.27.28 Task 026G — pinned upstream app-level Kotlin compile probe registered (2026-11-25)

- **Task ID:** R3-S0-T026-APP-KOTLIN-COMPILE-PROBE-026G; **status:** registered / not started.
- **Start branch HEAD:** 288d14f178bc807f7729711a77dad0611f487b07; runner checkout is fast-forwarded to this exact branch head and clean; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Goal:** use the now-working KSP dependency staging to execute the pinned upstream :app:compileAppDebugKotlin task and capture the actual app-level Kotlin compile outputs for the authentic source/caller closure. This is a read-only compile probe, not production migration authorization.
- **Allowed changes:** no tracked repository files. Temporary filtered Maven repository, one-run init script, launcher, logs, result marker and output inventories may be created outside the worktree; generated Gradle build outputs are evidence.
- **Dependencies:** Task 026F KSP pass above; verified 80,992,357-byte KSP AA-embeddable JAR and its SHA-256; pinned upstream and current branch identity.
- **Execution boundary:** Microsoft OpenJDK 21.0.13.1; existing Android SDK at /root/stage0-runner/android-sdk; installed Gradle 8.14.4; one worker, no parallelism, bounded JVM heap; temporary settings-level Maven repository filtered to only com.google.devtools.ksp:symbol-processing-aa-embeddable; execute only :app:compileAppDebugKotlin (its declared dependencies may run, including KSP). Preserve exact artifact hash and ensure no remote AA JAR transfer is required.
- **Acceptance:** exact identities and clean tracked worktrees; Gradle exit 0 and BUILD SUCCESSFUL; record all executed task names, compile outputs and hashes; verify whether the pinned BaseSource/BookSource/Book/BookChapter/AnalyzeRule/AnalyzeUrl/JsExtensions/SharedJsScope/JsSourceEngine class outputs are actually emitted. Any absent class or unexecuted task remains explicitly unverified.
- **Stop conditions:** source/upstream mismatch, dirty tracked worktree, missing/mismatched JAR or SDK, unresolved dependency transfer, memory/process instability, or timeout. Stop and record the precise blocker; do not kill unrelated processes or silently retry.
- **Exclusions:** no full assemble, unit/instrumentation tests, DEX/R8, APK, CI/Actions, device, dependency upgrades, Gradle/source-set edits, production source migration, schema/database/Room work, UI/Reader changes, Phase B authorization or complete-compatibility claim.
- **Next:** execute only the registered compile probe, then record and reread its evidence before registering any ABI/reflection or broader validation task. Task 027 and Stage-1 remain blocked.

### 4.27.29 Task 026G result — App Kotlin compile stopped by memory-pressure gate (2026-11-25)

- **Status:** incomplete / acceptance not met; not a source compile failure and not a pass.
- The exact pinned `:app:compileAppDebugKotlin` task reached Kotlin compilation, but Gradle `-Xmx1g` plus a separately launched Kotlin daemon `-Xmx1g` exhausted the small runner's practical memory headroom. Kotlin daemon RSS reached about 1.19 GiB; available memory fell to about 99 MiB and swap use rose to about 1.3 GiB. No target App Kotlin `.class` output was produced. The process was stopped under the preregistered memory-pressure condition; exit code 143. Full evidence and hashes are in audit §24.115.
- Temporary filtered Maven repository and init script were removed; verified KSP JAR retained under `/tmp/task026-ksp-aa-full/`; tracked Badcat and upstream worktrees remain clean.
- **Next:** Task 026H is separately preregistered in audit §24.116 to run only `:app:compileAppDebugKotlin` with `-Pkotlin.compiler.execution.strategy=in-process` and a single Gradle JVM capped at 1 GiB, reusing the verified local KSP artifact. No tests/assemble/DEX/R8/APK/CI/device or source migration.
- **Disposition:** Task 026 Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.30 Task 026H result — in-process App compile reached Kotlin task but stopped on memory pressure (2026-11-25)

- **Status:** incomplete / acceptance not met; not a source compile failure and not a pass. The exact `:app:compileAppDebugKotlin` task was reached after `:app:kspAppDebugKotlin` loaded the Room and Glide providers.
- **Strategy:** in-process Kotlin compiler, single Gradle JVM `-Xmx1g`; no separate Kotlin daemon. The Gradle process reached about 1.26 GiB RSS; available RAM fell to about 115 MiB and swap use rose to about 400 MiB. It was stopped under the registered memory-pressure gate, exit 143. No App Kotlin `.class` outputs were produced. See audit §24.117 for hashes and complete evidence.
- Temporary filtered Maven repository and init script removed; verified KSP JAR retained under `/tmp/task026-ksp-aa-full/`; tracked Badcat and upstream worktrees remain clean.
- **Next:** Task 026I is preregistered in audit §24.118 to run only `:app:compileAppDebugKotlin` with the compiler in-process and Gradle heap capped at 768 MiB. This is the last bounded App compile resource experiment; no further compile retry after it.
- **Disposition:** Task 026 Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.31 Task 026I result — lower-heap in-process App compile stopped at memory threshold (2026-11-25)

- **Status:** incomplete / acceptance not met; not a source compile failure and not a pass. With `-Xmx768m` and in-process Kotlin compilation, Gradle reached `:app:compileAppDebugKotlin`; KSP was up-to-date and no remote AA JAR fetch occurred.
- Available memory crossed the registered 150 MiB stop threshold (sampled about 138 MiB); Gradle RSS reached about 1.31 GiB. The task was stopped at 22:12:10 +08:00, exit 143. No App Kotlin `.class` outputs were produced. Full evidence and hashes: audit §24.119.
- Temporary filtered Maven repository and init script removed; verified KSP JAR retained under `/tmp/task026-ksp-aa-full/`; tracked worktrees remain clean.
- **No further App compile retry is authorized.** Task 026J is preregistered in audit §24.120 for read-only extraction of existing class/JAR/AAR/DEX artifacts and JVM ABI metadata; no Gradle/compiler/build task.
- **Disposition:** Task 026 Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.32 Task 026J result — read-only existing artifact/ABI inventory (2026-11-25)

- **Status:** completed for the existing-artifact scope only. Six existing archives and 159 loose `.class` files were inventoried; all loose classes are in `modules/book` (96) or `modules/rhino` (63), none in `app/build`. No `.aar` or `.dex` was found. App build contains only Cronet native and generated R archives, not an App class archive.
- No requested App Runtime symbol was found in any archive or loose class file. `BaseSource`, `BookSource`, `BaseBook`, `Book`, `BookChapter`, `SearchBook`, `AnalyzeRule`, `AnalyzeUrl`, `JsExtensions`/inherited `JsEncodeUtils`, `SharedJsScope` and `JsSourceEngine` are all `COMPILED ABI NOT AVAILABLE`; exact source declaration owners are recorded in audit §24.121.
- Existing Rhino module bytecode remains available: `RhinoScriptEngine`, `ScriptBindings`, `SharedGlobalStateHandle` and `CompiledScript` were found in the Rhino classes JAR and `javap` evidence retained. This does not establish App Runtime ABI. Full hashes and inventories are in `/root/stage0-runner/task026-clean/task026j-attempt1/` and audit §24.121.
- **Next:** continue Phase A source/caller/Host-Port closure only. No further App compile retry is authorized.
- **Disposition:** Task 026 Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.33 Task 026 Phase A supplemental result — pinned upstream App dependency variants recovered (2026-11-25)

- **Status:** existing report evidence recovered and integrity-checked; no Gradle rerun. All ten available pinned upstream App configurations have `BUILD SUCCESSFUL`, exit 0 and zero `FAILED` dependency entries: Debug/Release compile + runtime, Debug/Release UnitTest compile + runtime, and Debug AndroidTest compile + runtime. No Release AndroidTest variant is exposed.
- The nine additional reports are under `/tmp/task026-55b8/upstream/`; the Debug runtime report is already recorded in audit §24.92. The report snapshot's superproject tree pins upstream exactly to `330e606c4d57e08adc056ff8ab7a4132c784da9a`; all report hashes were rechecked. Full matrix and hashes: audit §24.122.
- **Limit:** this verifies the full upstream App dependency metadata graph, not the minimal `:legado-runtime` source closure or compiled ABI. Do not conflate it with the separate Badcat 44-configuration graph.
- **Next:** reconcile exact Runtime source imports and typed Host Port ownership against these existing variant graphs; no repeated dependency report or App compile.
- **Disposition:** Task 026 Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.34 Task 026K preregistration — Runtime source/import dependency ownership matrix (2026-11-25)

- **Status:** registered; not yet executed. Read-only Phase A work only.
- **Goal:** reconcile the path-exact candidate Runtime sources and actual BookSource callers against their direct imports, pinned source owners, exact external Gradle coordinates and Debug/Release/test variants; classify each dependency as Runtime core, typed Host Port, product UI/Reader or conditional/excluded.
- **Scope:** candidates in audit §24.91.4 and actual callers in §§24.85–24.90. Upstream source/build files remain read-only; outputs stay outside the repository under `/root/stage0-runner/task026-clean/task026k-attempt1/`.
- **Evidence:** existing pinned upstream variant reports (§24.122), prior 1,085-node import graph (§24.85.1), and caller/Host-Port evidence (§§24.85–24.91). Do not rerun dependency reports or App compilation.
- **Exclusions:** no Gradle/compiler/build/test/CI/device, no source/build/test/UI/Reader/schema edits, no dependency upgrades, no Phase B or Task 027.
- **Next:** complete the import/coordinate/Host-Port matrix, then reconcile the path-exact implementation addendum and blockers. No further App compile retry is authorized.

- **Task 026K preflight correction:** the pinned upstream uses Groovy `app/build.gradle`, `modules/rhino/build.gradle`, `modules/book/build.gradle`, root `build.gradle`/`settings.gradle`, and `gradle/libs.versions.toml`; no `.gradle.kts` variants exist. Exact correction is recorded in audit §24.123.1. The initial preflight stopped before any build or source mutation.

### 4.27.35 Task 026K result — Runtime source/import and Host Port dependency closure (2026-11-25)

- **Status:** completed read-only. All 33 path-exact candidate source files exist. Their 703 direct imports were indexed against 1,186 pinned source files: 489 internal owners resolved, 23 external package groups mapped to exact upstream coordinates/versions, zero unresolved imports, and five overload sites resolved by actual call receiver. 88 upstream Gradle dependency declarations and 10 plugin declarations were recorded. Full evidence and hashes: audit §24.124 and `/root/stage0-runner/task026-clean/task026k-attempt1/`.
- The reused 16-seed recursive source graph contains 1,085 nodes/9,983 import edges, 1,055 App + 30 Rhino nodes, and no `modules/book` nodes. It is a broad upper bound, not a minimal Runtime closure; exclude `:modules:book` from `:legado-runtime`.
- Core DTO boundary: `RowUi` and `ExploreKind` remain Runtime models; `FlexChildStyle` fields/defaults remain Runtime while `apply(View)` moves to an app host adapter. Glide/Media3, RSS screens, login UI, generated R resources and appCtx remain host-owned. ExploreInfoMap LRU/dirty-save behavior must be preserved through `ExploreCatalogPort`.
- **Open ports:** AppContextPort, ResourcePort, FileStoragePort, BookRepositoryPort, ChapterRepositoryPort, SearchResultRepositoryPort, SharedJsScopePort, LoginCallbackPort and FlexStyleLayoutAdapter are missing/incomplete. Existing/proposed ports are not yet fully wired. Room/Parcelable models require Room 2.8.4 and Parcelize 2.4.20; DB/schema stays app-owned.
- **Next:** finalize the path-exact Phase B addendum and test/acceptance matrix. No App compile retry under §24.119; no production source edit before addendum commit and required-list reread.
- **Disposition:** Task 026 Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.36 Task 026L preregistration — path-exact Phase B addendum freeze (2026-11-25)

- **Status:** registered; read-only. Re-read master and this category overview before registration.
- **Goal:** compare the exact current `:legado-runtime`, `:legado-rhino`, `:integration-contract` and relevant App Host Port implementations/build files with the 33-file upstream source/import/caller/dependency map, then freeze one path-exact Phase B addendum: file dispositions, typed Host Port signatures, dependency graph, test paths and acceptance gates.
- **Allowed:** current module build files and source/tests in `legado-runtime`, `legado-rhino`, `integration-contract`, root settings/build/version catalog, and only App source files that implement or inject the Runtime Host Ports/facade. Upstream remains read-only; reuse Task 026K evidence rather than repeat scans.
- **Outputs:** standalone implementation addendum under `docs/audits/`, plus audit/overview result records. Every candidate file must be COPY/ADAPT/SPLIT/HOST_ONLY/CONDITIONAL/EXCLUDE with evidence. Missing port signatures remain explicit blockers.
- **Hard constraints:** no Runtime→app cycle; no `:modules:book`; use `:legado-rhino`; Room database/schema stays in app; Glide/Media3/RSS/login UI and Flexbox View mutation stay host-side; preserve RowUi/ExploreKind/FlexChildStyle data and ExploreInfoMap LRU/dirty-save semantics.
- **No code/build/test action in 026L.** The user previously authorized Phase B, but the path-exact addendum must be committed and both required lists reread before any production edit. No App compile retry under §24.119.
- **Disposition:** Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

- **026L scope correction:** the current production composition root is `app/.../runtime/BadcatReadingEngineFactory.kt` + `BadcatSourceHttpGatewayFactory.kt` + `RhinoSourceScriptRuntime.kt`, called by SourceSearch/SourceBook/SourceCatalog and login UI activities. These are included in the read-only addendum audit; `SourceRuntime`/`RuntimeHostPort` in `integration-contract` are not the only runtime surface. See audit §24.125.1.

### 4.27.37 Task 026L result — Phase B path-exact implementation addendum frozen (2026-11-25)

- **Status:** completed as read-only design work. New addendum: `docs/audits/legado-runtime-3.0-phase-b-implementation-addendum.md` (33 pinned candidate paths each have one explicit disposition; exact current App Runtime file destinations, target module graph, Host Port signature/blocker matrix, pinned dependency plan, test paths and acceptance gates are recorded).
- Current reality: App still composes the production source engine; `:integration-contract` contains most engine/parser implementation; `:legado-runtime` currently injects only seven upstream rule model classes plus `GsonExtensions.kt`; `:legado-rhino` is the isolated Rhino module. The five declared UI/WebView/browser/verification/product Host Ports have no production implementers/wiring in the inspected tree.
- Target: migrate the real pinned source/rule/JS/workflow implementation to `:legado-runtime`, preserve `:legado-rhino`, and keep HTTP/Cookie/Session/WebView/browser/verification/context/resources/files/database/UI/Reader adapters in App. Do not keep duplicate/shadow Legado models or a second production implementation in `:integration-contract`.
- Missing typed ports remain explicit blockers: source execution state, Cookie/Session, full HTTP, Cache/Variable, SharedJsScope, AppContext/Resource, FileStorage, Book/Chapter/SearchResult repositories, ExploreCatalog, LoginCallback, replacement/config/notification and FlexStyle adapter. No guessed signatures were added.
- No production source/build/test/schema/UI/Reader edits, Gradle/build/test/CI/device action or App compile retry occurred. This task is a design freeze, not Phase B implementation or compatibility proof.
- **Next mandatory gate:** commit addendum and records, reread master + this category overview, then register the first bounded Phase B implementation task before any production edit.
- **Disposition:** Phase A remains open; Phase B not started; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.38 Task 026M preregistration — first bounded Phase B source-model ownership slice (2026-11-25)

- **Task ID:** `R3-S0-T026-PHASE-B-CORE-UI-MODEL-026M`; status registered / not started. Start HEAD `088e8c5369d09f9e3d9e5bc4408a5d0cbd5315a0`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** move exact pinned `RowUi`, `ExploreKind` and `FlexChildStyle` data contracts to `:legado-runtime`, remove duplicate App model definitions, and split `FlexChildStyle.apply(View)` into an App-only adapter. Preserve every JSON field/default, Type constant, equality/hashCode, style fallback and layout mapping.
- **Exact files:** add the three models under `legado-runtime/src/main/java/io/legado/app/data/entities/rule/`; remove the three App duplicates under `app/src/main/java/io/legado/app/data/entities/rule/`; add `app/src/main/java/app/badcat/client/runtime/FlexStyleLayoutAdapter.kt`; add `legado-runtime/src/test/kotlin/io/legado/runtime/PinnedLegadoUiModelContractTest.kt`. Existing App test `PinnedLegadoRuleModelsTest.kt` remains unchanged.
- **Evidence:** the three current App files are byte-identical to pinned upstream (hashes and source caller evidence in audit §24.127). No current Badcat App production caller exists beyond the model declarations; upstream UI callsites are not imported as product UI.
- **Validation:** focused `:legado-runtime:testDebugUnitTest` only; source hashes, unique FQCN ownership, no App production model copy, no Runtime View/Flexbox import, model JSON/default/equality and adapter mapping review. **No App compile/test/assemble retry** under §24.119; host adapter compilation remains unverified.
- **Exclusions:** no other Runtime source migration, no Room/schema/database, no UI/Reader changes, no dependency upgrade, no CI, no Task 027 or Stage-1.
- **Next:** complete this isolated model ownership slice and its Runtime-only tests, write exact results, reread required lists, then register the next Phase B task. Stage-0 remains not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.

### 4.27.39 Task 026M result — canonical UI-model Runtime ownership slice (2026-11-25)

- **Status:** completed for the bounded model ownership scope. Exact pinned `RowUi.kt` and `ExploreKind.kt` moved to `:legado-runtime`; duplicate App definitions removed. `FlexChildStyle` data fields/defaults/equality/align mapping moved to Runtime; `apply(View)` moved to App-only `FlexStyleLayoutAdapter`. Runtime has no View/Flexbox imports. Exact source hashes and caller scope are in audit §24.128.
- Added `PinnedLegadoUiModelContractTest.kt`; `:legado-runtime:testDebugUnitTest` passed: 6 tests, 0 failures, 0 errors, 0 skipped. Gradle 8.14.4, JDK 21.0.13.1, offline, only Runtime tasks. Exact log/JUnit/classes JAR hashes are in audit §24.128.
- **Limit:** App test, App compile and adapter compilation/runtime were not run because §24.119 forbids any further App compile retry. The adapter is not wired to a current Badcat production caller; no Legado UI was imported. No CI, Room/schema, UI/Reader or data migration changes.
- **Next:** preregister a BookSource canonical-model/file-catalog/Room-annotation decision slice before moving BookSource or changing persistence. BaseSource lifecycle and remaining Host Ports stay blocked.
- **Disposition:** Phase A remains open; Phase B has begun with this one bounded slice; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.40 Task 026N preregistration — canonical BookSource model/file-catalog boundary (2026-11-25)

- **Task ID:** `R3-S0-T026-PHASE-B-BOOKSOURCE-CANONICAL-MODEL-026N`; registered / not started. Start HEAD `373e2440e5f1cfbc68472e773ce0cb9f9adb04f5`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** decide how the exact upstream `BookSource`/`BaseSource` model becomes the one Runtime source model while preserving the current App AtomicFile catalog, full JSON/import/export, source identity, flags, cookies/login state and all production callers.
- **Scope:** read-only upstream BaseSource/BookSource/BaseBook declarations and directly required persistence evidence; current `ReadingEngine`/`SourceRuntime`/`SourceReadingEngine`/`LegadoBookSourceCodec` plus App `FileBookSourceCatalog`, factory, Rhino runtime, cookie/session adapters and corresponding tests. Reuse audit §§24.51, 24.70, 24.82, 24.91–24.92, 24.124–24.128.
- **Outputs:** exact upstream field/default/null/Room/Parcelable/serialization matrix; current DTO/raw JSON codec comparison; import/export/enable/remove/replace/order and caller map; lossless canonical-model/file-adapter decision; BookSourceCatalog/SourceExecutionState/Cookie/Session port requirements; Room annotation dependency separated from schema/compiler decision; exact migration tests and blockers.
- **Hard constraints:** no shadow BookSource DTO, no lost unknown fields, no App DAO/Context/R/UI in Runtime, no Room schema v1, no database/data migration or dual-write. `BaseSource` cannot move without lifecycle Host Ports. No App compile retry under §24.119.
- **Exclusions:** no source/build/test/schema/UI/Reader edits, no Gradle/build/test/CI/device/network, no Task 027 or full-compatibility claim.
- **Next:** complete this read-only model/data-owner decision, then register a bounded model/catalog adapter migration only if the lossless mapping and mandatory Host Ports are proven. Stage-0 remains not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.

### 4.27.41 Task 026N result — canonical BookSource model/file-catalog boundary (2026-11-25)

- **Status:** completed read-only. Canonical Runtime source model must be pinned upstream `io.legado.app.data.entities.BookSource` (33 fields, six typed rule objects, URL-key identity and exact helper semantics). Current `app.badcat.engine.BookSource` and `LegadoBookSource` are shadow DTOs; six rules are raw JSON strings and defaults/equality/identity differ. Full field and 29-method BaseSource matrix: `docs/audits/legado-runtime-3.0-booksource-model-boundary.md`.
- **Blocker:** upstream BookSource is a final data class implementing BaseSource, whose 29 methods own JS/login/header/Cookie/AES/session/cache/shared-scope/HTTP/rate-limit/Explore side effects. A wrapper cannot subclass it; BaseSource cannot move as-is into Runtime. A typed per-source delegate preserving JS API and self identity must be frozen first.
- **Data owner:** keep App AtomicFile catalog and raw JSON preservation. Do not introduce Room schema v1, Room DB/compiler/KSP, AppDatabase or dual-write. `room-common:2.8.4` may satisfy annotations only; Parcelize 2.4.20 already exists in Runtime. The current catalog rejects sourceUrls manifests in its actual import path and has fewer management operations than upstream DAO.
- **Lifecycle:** factory catalog currently uses default no-op invalidation callback; SourceCatalogActivity separately wires `clearSourceState`. Centralize through SourceExecutionStatePort.
- **Next:** register a dedicated BaseSource typed Host Port/delegate contract closure task before moving BookSource/BaseSource. No source/build/test action in 026N.
- **Disposition:** Phase A remains open; Phase B has begun only with Task 026M; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.

### 4.27.42 Task 026O preregistration — BaseSource typed Host Port/delegate closure (2026-11-25)

- **Task ID:** `R3-S0-T026-PHASE-B-BASESOURCE-HOST-PORT-CLOSURE-026O`; registered / not started. Start HEAD `3eea177532d100d0846349741fdc21e06524d576`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** resolve the 29 BaseSource methods plus inherited JsExtensions surface and all actual callsites; decide whether a per-source typed Host Services attachment preserves `BookSource : BaseSource`, `getSource() === this`, JS method ABI and concurrency without a global service locator.
- **Scope:** read-only pinned BaseSource/BookSource/JsExtensions/SharedJsScope/JsSourceEngine/AnalyzeRule/AnalyzeUrl/source workflow/login bridge files; current Badcat Runtime Host Port/Rhino/cookie/session/cache/file/catalog adapters and matching tests. Reuse 026K–026N evidence; search-only BaseSource implementer/reference index allowed.
- **Outputs:** exact JS/JVM API/overload/annotation/callsite matrix; typed Host Port method signatures with null/error/thread/cancel/callback/lifecycle semantics; per-source attachment design comparison including Gson/Room/Parcelable/copy restoration; all implementers; exact tests and BookSource migration GO/NO-GO.
- **Hard constraints:** no `Map<String,Any?>`, reflection bag, global service locator, ThreadLocal shortcut, no-op or shadow DTO. Preserve source identity and all JS-visible behavior. No App compile retry under §24.119.
- **Exclusions:** no source/build/test/schema/UI/Reader changes, no Gradle/build/test/CI/device/network, no Room schema/data migration, no Task 027.
- **Next:** register BaseSource/BookSource implementation only if the per-source host bridge is proven; otherwise stop and reopen architecture decision. Stage-0 remains not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.

- **026O scope amendment:** search-only implementer inventory found additional shared `BaseSource` implementations in upstream `RssSource`, `HttpTTS`, anonymous `BookCover` and `SourceNavigationBridge` delegation, plus Review/Reader/Login/Verification/WebView callsites. Exact files are listed in audit §24.131.1 and are read only at BaseSource boundaries; these product features remain App-side and are not imported into Runtime.

- **026O scope amendment 2:** added exact read-only imported owner files for CacheManager, CookieStore/CookieManager, ConcurrentRateLimiter, AppConfig/Theme/ReadBookConfig, Android-ID/AES, SourceHelp, AppLog/EventBus, File/Archive/LibArchive, BackstageWebView/SSL, BatchContentContext and VideoPlay. Exact paths and ownership boundary are audit §24.131.2; read only methods used by BaseSource/JsExtensions.

### 4.27.43 Task 026O result — BaseSource JS ABI and per-source Host Port closure (2026-11-25)

- **Status:** completed read-only. Pinned upstream API has 175 unique method signatures across `BaseSource`/`JsExtensions`/`JsEncodeUtils`, including 77 `@JavascriptInterface` annotations. Exact method tables: `task026o-attempt1/`.
- Current Badcat source object is login-only (11 methods); 14 BaseSource methods and 17 JsExtensions names are missing. `java.getSource()` returns a Map, not BaseSource; `sourceApi` and `baseUrl` bindings are absent; current tests codify this reduced ABI. Current SourceScriptRuntime is DTO/String-only.
- **Selected design:** preserve the exact BookSource object and attach a typed per-instance Host Services sidecar, ignored by Room/Gson/Parcelable and hidden from Rhino. No wrapper/global service locator/ThreadLocal/Map bridge. Concrete BookSource casts in AnalyzeRule make wrapper substitution unsafe.
- **Still blocked:** five Host Port declarations have zero App implementations; HTTP options, WebView/browser/verification single-flight, CookieStore sync, cache LRU/TTL, dynamic rate update and batch-content save-token path remain incomplete. Full matrix and evidence: `docs/audits/legado-runtime-3.0-basesource-host-port-contract.md`.
- **Next:** execute Task 026L read-only Phase B addendum freeze using 026K–026O evidence; no more source edits until the addendum is committed and both required lists are reread.
- **Disposition:** Phase A remains open; Phase B limited to Task 026M DTO slice; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.

- **026L snapshot refresh:** current documentation HEAD is `8c95fed9728f88d2610755fd597e10eeb17c42c1`; upstream pin unchanged. Task 026L must consume 026K–026O evidence and freeze the full path-exact Phase B addendum read-only. No code/build/test edit is authorized in 026L.

- **026L scope amendment 3:** inspect `legado-runtime-poc` build, source-closure evidence and manifest read-only. It is included in settings but not depended on by App; final addendum must classify it as quarantined/excluded from production graph, without editing it.


### 4.27.44 Task 026L snapshot refresh result — Phase B addendum and POC quarantine (2026-11-25)

- **Status:** completed read-only at Badcat refresh start HEAD `4e51c9c210c33e3784070e8a557b8c3f4ab54b9c`; pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`. Both tracked worktrees were clean.
- Refreshed `docs/audits/legado-runtime-3.0-phase-b-implementation-addendum.md` (SHA-256 `be29cfe61ea2646e06bab8f74f5c34b5a4de0b7eae290d45c4028e95b16f256b`) to consume 026M–026O evidence and the POC scope amendment. Current `:legado-runtime` owns RowUi/ExploreKind/FlexChildStyle plus eight injected rule/serialization sources; the App FlexStyle adapter remains uncompiled under §24.119.
- `:legado-runtime-poc` remains a quarantined historical probe: included in settings, no App dependency edge, 12 injected pinned sources, no Kotlin `src/main` source. Excluded from production dependencies; no cleanup/edit.
- BaseSource ABI inventory: 175 unique signatures and 77 JS annotations. Current Badcat source/java bridge remains incomplete. Per-source Host Services is only a selected design direction; public cross-module owner visibility, all BaseSource implementers, source-type mapping, attachment lifecycle and typed port semantics remain unproven. No BookSource/BaseSource migration authorization.
- Corrected Host Port matrix and 64-source hash evidence are under `/root/stage0-runner/task026-clean/task026l-refresh-attempt1/`. Exact result and hashes: audit §24.125.4.
- **No** production source/build/test/manifest/schema/UI/Reader edit, Gradle/build/test/CI/device/network action or App compile retry. Phase B has begun only with Task 026M's bounded UI-model slice (Runtime tests 6/6); Phase A remains open, Task 027 blocked, Stage-0 not accepted, Stage-1 **NO-GO**, full compatibility **NOT ESTABLISHED**.
- **Next:** after this documentation commit, reread the master and third-category required lists, then register Task 026P for read-only exact typed Host Port signature closure. Do not begin BookSource/BaseSource migration before the signature freeze and separate owner proof.


### 4.27.45 Task 026P preregistration — exact typed BaseSource Host Port signature freeze (2026-11-25)

- **Task ID:** `R3-S0-T026-PHASE-B-HOST-PORT-SIGNATURE-FREEZE-026P`; registered / not started. Start HEAD `facdb5f8e7c636680a3047daa5abefddf71d79a3`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Goal:** freeze exact method-level Host Port signatures and semantics for BaseSource/JsExtensions before implementing the public per-source owner. Map all 175 API signatures to Runtime core, typed host owner or App-only product behavior; do not repeat completed inventories.
- **Scope:** exact pinned BaseSource/BookSource/RssSource/HttpTTS/BookCover/SourceNavigationBridge, JsExtensions/JsEncodeUtils, SharedJsScope/JsSourceEngine, AnalyzeRule/AnalyzeUrl and source workflow; CacheManager/CookieStore/CookieManager/rate limiter/config/identity/crypto/file/archive/WebView/browser/verification/EventBus/batch-content owners; current Badcat RuntimeHostPort/Rhino/session/cookie/cache/file/catalog adapters and exact callers/tests already registered in audit §§24.131–24.132.
- **Outputs:** method-level typed port matrix with exact owner/caller references, nullable/default/result/error, thread/CoroutineContext/cancel/timeout/callback/lifecycle semantics, all BaseSource implementers, cross-module owner visibility, source type and reattachment requirements, focused tests and GO/NO-GO. Candidate signatures without source evidence remain open.
- **Constraints:** read-only; no source/build/test/Gradle/CI/device/network/App compile changes, no BookSource/BaseSource migration, no Room/schema, no no-op/Map/global locator/ThreadLocal/wrapper shortcut, no POC cleanup, no Task 027/Stage-1. Preserve original Badcat product UI and pinned upstream behavior.
- **Dependency:** Task 026L addendum refresh committed at predecessor HEAD `b39a540c8d8f8854b5e2141177b7cd590b35d93d`; both required lists were reread from that committed HEAD before this registration.
- **Scope amendment:** include only `ui/rss/read/RssJsExtensions.kt` and `ui/login/SourceLoginJsExtensions.kt` as the shared App-side `JsExtensions` implementation and its Login UI subclass; read exact bridge/context methods only. See audit §24.125.6.
- **Next:** after this registration commit, reread both required lists from the new HEAD, then execute 026P read-only. A separate owner implementation/proof task must follow before BookSource/BaseSource migration.

### 4.27.46 Task 026P result — exact BaseSource Host Port signature freeze (2026-11-26)

- **Status:** completed as a bounded read-only source/API-to-owner mapping. Full method-level table: docs/audits/legado-runtime-3.0-basesource-host-port-signature-matrix.tsv (177 declaration rows / 175 unique signatures; 77 @JavascriptInterface annotations). Typed contract and exact semantics: docs/audits/legado-runtime-3.0-basesource-host-port-signature-freeze.md.
- **Key decision:** preserve exact BaseSource identity and a typed per-instance Host Services sidecar; no wrapper/global locator/ThreadLocal/Map. Keep ConcurrentRateLimiter, SourceLock and SharedJsScope in Runtime core. Keep Cookie/Session/Cache/Variable/HTTP/WebView/Browser/Verification/File/Archive/AppContext/Config/Notification/UI/ProductEvent/BatchContent/Explore as distinct typed capabilities.
- **Critical blocker:** the 026O internal sidecar owner cannot be called by App as a normal cross-module Kotlin API. A public Kotlin-visible/JVM-synthetic typed attachment API is only a candidate; Rhino reflection, cross-module compile, Gson/Room/Parcelize exclusion and copy/import/restore reattachment must be proven before BookSource migration. RssSource, HttpTTS and BookCover.CoverRule are additional App-side BaseSource implementers; SourceNavigationBridge delegates to the original instance.
- **Context correction:** current model-neutral SourceExecutionContext loses the full CoroutineContext/navigation-suppression element, requires a non-null Job and nonblank source name, and uses sourceId where upstream requires generic getKey(). The replacement must preserve nullable tag, source type, caller and the full active CoroutineContext.
- **App-side bridge separation:** RssJsExtensions, SourceLoginJsExtensions and WebJsExtensions remain App-only; WebView page RPC is not normal Rhino source execution. Login UI v2 remains App UI but calls typed BaseSource methods with Book/BookChapter.
- **No execution claim:** no source/build/test/Gradle/workflow/schema/UI/Reader edit, no App compile retry, build/test, dependency resolution, DEX/R8, CI or device run. No dynamic behavior parity is claimed.
- **Gate:** 026P static inventory complete; Host Port implementation, public sidecar ABI, compiled App ABI/reflection, Release R8 and dynamic HTTP/Cookie/cache/Explore/WebView/browser/image/login/batch tests remain NOT MET. BookSource/BaseSource migration remains NO-GO; Task 027 blocked; Stage-0 in_progress / not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** register the public typed sidecar visibility/serialization/reattachment proof as a separate bounded task before any BookSource/BaseSource migration. No further App compile retry under §24.119.

### 4.27.47 Task 026Q preregistration — public sidecar visibility and serialization mechanism probe (2026-11-26)

- **Task ID:** R3-S0-T026-PHASE-A-BASESOURCE-SIDECAR-ABI-PROBE-026Q; registered / not started. Start HEAD 51e7a9a0994d2b42b9b34a39eb0f29a647394db5; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9.
- **Goal:** prove or reject the candidate public Kotlin-visible/JVM-synthetic per-source Host Services attachment mechanism across a separate Runtime/App Kotlin module boundary, and inspect whether Rhino exposes it to JavaScript. Verify the body-property transient/Room-ignore/Parcelize-ignore and copy/reattachment mechanism with a minimal scratch model.
- **Allowed:** docs-only repository changes; temporary scratch source/classes/JARs/dumps/reports under /tmp/task026q-sidecar-probe/. Use only compiler/runtime/Gson/Room/Parcelize/Rhino artifacts already cached on the runner.
- **Probe:** compile separate Runtime and App consumer Kotlin modules; javap -p -v; Java reflection; pinned Rhino wrapper + JS visibility if its exact runtime JAR is already available; Gson serialization and body-property copy; Room Ignore and Parcelize IgnoredOnParcel metadata. Distinguish source-level Kotlin visibility, JVM synthetic flags, Java reflection and actual Rhino script visibility.
- **Hard stops:** no network/download/tool install/Gradle cache injection; no App compile retry, no production/test/build/Gradle/schema/UI/Reader edit, no full APK/DEX/R8, no CI/device, no BookSource/BaseSource migration or Task 027. Missing cached artifacts stop only the corresponding sub-check as NOT ESTABLISHED.
- **Acceptance:** exact scratch source and artifact hashes; cross-module compile, JVM descriptor/ACC_SYNTHETIC, Java reflection and Rhino JS visibility results; Gson/Room/Parcelize metadata; copy/reattachment finding; explicit remaining gap between minimal mechanism and real BookSource/all BaseSource implementers.
- **Dependency:** 026P static signature/owner freeze at commit 51e7a9a0994d2b42b9b34a39eb0f29a647394db5. 026P remains complete as static inventory; actual Host Port implementation and compiled App ABI/R8/dynamic parity remain NOT MET.
- **Next:** after this registration commit and required-list reread, execute only the bounded scratch probe. The result does not itself authorize production migration. Stage-0 remains in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.48 Task 026Q result — public @JvmSynthetic sidecar owner rejected by pinned Rhino (2026-11-26)

- **Status:** bounded scratch mechanism probe complete. Result and exact artifact hashes: /tmp/task026q-sidecar-probe/RESULT.md and RESULT.sha256. Pinned upstream remains 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Cross-module:** Kotlin 2.4.20 Runtime and App consumer compile succeeded; the App consumer called public @JvmSynthetic attach/read methods. Java reflection returns them as public synthetic methods. A separate internal-member consumer fails with “cannot access”; internal compiles to a public mangled non-synthetic JVM method.
- **Rhino:** actual pinned HtmlUnit CoreJS + pinned upstream RhinoWrapFactory/CatchableNativeJavaObject path returns JS types function|function|function for attachHostServices, requireHostServices and an ordinary method. Pinned JavaMembers has no Method.isSynthetic filtering; RhinoClassShutter only filters classes. **@JvmSynthetic is not sufficient to hide an owner API from source scripts; reject this design as-is.**
- **Sidecar annotations:** minimal private body property marked transient + Room Ignore + Parcelize IgnoredOnParcel is omitted from Gson and generated writeToParcel; Room Ignore appears in classfile metadata (BINARY retention, so Java reflection returns false). Data-class copy does not carry the sidecar, while equality/hash ignore it; explicit reattachment after copy is mandatory. Actual Android Parcel roundtrip, Room processor and real BookSource behavior were not run.
- **Scope:** minimal probe only, not real BookSource or all BaseSource implementers. No tracked source/build/test/Gradle/workflow/schema/UI/Reader change, no App compile/build/test/Room KSP/DEX/R8/CI/device/network.
- **Gate:** 026Q complete for the bounded probe; public synthetic attachment methods rejected with current Rhino. Sidecar body property remains a candidate only with real BookSource reattachment proof. Phase A remains open; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** register a read-only Rhino member-filter feasibility probe using a scratch wrapper/marker to determine whether host-only attachment methods can be hidden while all existing non-synthetic JS APIs remain visible. No production Rhino/Runtime edit in that probe.

### 4.27.49 Task 026R preregistration — Rhino host-member filter feasibility probe (2026-11-26)

- **Task ID:** R3-S0-T026-PHASE-A-RHINO-MEMBER-FILTER-PROBE-026R; registered / not started. Start HEAD a7d5b71ba798687355a0a1bf975536c7836c33e4; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Trigger:** 026Q showed public @JvmSynthetic owner methods are visible as JavaScript functions through the exact pinned Rhino wrapper.
- **Goal:** test a method-level marker and scratch CatchableNativeJavaObject/WrapFactory filter that hides only host-only attachment methods while preserving ordinary/inherited source-visible methods and overload behavior.
- **Allowed:** docs-only repository registration; all scratch source/classes/JARs/dumps/logs under /tmp/task026r-rhino-filter-probe/. Use only already-cached pinned Rhino/HtmlUnit/compiler artifacts.
- **Probe:** inspect pinned wrapper method lookup; compile a scratch object with ordinary methods plus marker-annotated @JvmSynthetic owner methods; wrap through a scratch filter based on the pinned CatchableNativeJavaObject; evaluate JS visibility and host-side Kotlin/Java invocation; test inherited and mixed-overload names.
- **Acceptance:** exact artifact hashes, filter interception point, JS output showing host methods hidden and ordinary APIs visible, inherited/overload behavior, and explicit real-BookSource limitations.
- **Hard stops:** no network/install/cache injection, no App compile retry, no production Rhino/Runtime/test/build/Gradle/schema/UI/Reader edits, no App/Runtime build, no APK/DEX/R8/CI/device, no migration or Task 027. If mixed overloads or existing script APIs cannot be preserved, reject the design.
- **Dependency:** Task 026Q result at commit a7d5b71ba798687355a0a1bf975536c7836c33e4: public @JvmSynthetic methods are Rhino-visible; minimal transient/Room Ignore/Parcelize body sidecar is not copied by data-class copy().
- **Next:** after registration commit and required-list reread, run only the scratch filter feasibility probe. No production code changes are authorized by a positive scratch result. Phase A remains open; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.

### 4.27.50 Task 026R result — method-level Rhino host-member filter feasibility (2026-11-26)

- **Status:** bounded scratch feasibility probe complete. Result and hashes: /tmp/task026r-rhino-filter-probe/RESULT.md and RESULT.sha256. Pinned upstream remains 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Positive:** pinned CatchableNativeJavaObject is open; a scratch subclass can filter runtime-retained marked methods in get/has/getIds while delegating visible methods to super and retaining CatchableJavaMethodCache behavior. Actual pinned Rhino JS hides unique marked methods (undefined, not in, not own property, absent from Object.keys); marked superclass and interface-default methods are also hidden. Ordinary getKey/getTag/ordinaryMethod remain callable. Separate App Kotlin consumer can still call the public @JvmSynthetic methods.
- **Required constraint:** a name with mixed marked/unmarked overloads fails open; both overloads remain callable. Host-only attachment/accessor names must be reserved, unique and non-overloaded. No collision with existing script-visible API names.
- **Marker placement:** define a runtime-retained function annotation in :legado-rhino (or neutral shared module); Runtime depends on Rhino, but Rhino must not depend on Runtime. Mark concrete implementation/override methods. Filter get, has and getIds. Preserve RhinoWrapFactory special factories and list/map/array paths.
- **Limit:** ordinary Java-object path only; not all 77 upstream JS methods, real BookSource/BaseSource implementers, Release R8 retention, marker retention under R8, performance or dynamic App behavior. No production changes/build/test/App compile/DEX/R8/CI/device/network.
- **Gate:** 026R complete as bounded scratch feasibility; candidate marker filter is viable only with unique method names and no overloads. No production Rhino/Runtime edit is authorized by this result. Phase A remains open; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** register a read-only BookSource creation/copy/Room/Parcelable/Gson/import/restore and BaseSource implementer attachment-site inventory before any sidecar implementation.

### 4.27.51 Task 026S preregistration — BookSource sidecar lifecycle and BaseSource attachment-site inventory (2026-11-26)

- **Task ID:** R3-S0-T026-PHASE-A-BOOKSOURCE-SIDECAR-LIFECYCLE-INVENTORY-026S; registered / not started. Start HEAD 8642e06b8e56f2a220eef34fad000170ba342a5b; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Goal:** map every BookSource creation/copy/Room/Gson/Parcelable/import/export/backup/restore/update/delete route and every BaseSource implementer/delegate; determine exact same-object Host Services attach/reattach/invalidation sites before implementation.
- **Allowed:** docs-only repository updates; read pinned upstream/current Badcat source and caller/build/test declarations. Temporary inventories under /tmp/task026s-sidecar-lifecycle/. No tracked source/test/build edit.
- **Probe:** constructors/factories; BookSource.copy and copy-like reconstruction; JSON/Gson adapters; Room DAO materialization/read/write callsites; Parcelable/Intent transport; import/export/API/MCP/legacy/backup restore; current Badcat shadow DTO/catalog/runtime factories; all BaseSource implementers and SourceNavigationBridge delegation.
- **Acceptance:** line-backed lifecycle matrix, DAO bypass/materialization analysis, BaseSource implementer matrix, attach/reattach/invalidate points, same-object java/source/sourceApi identity requirements, source key/tag/type/full CoroutineContext and focused dynamic tests still required.
- **Hard stops:** no production/test/Gradle/schema/UI/Reader changes; no build/test/Room processor/DEX/R8/CI/device/network; no App compile retry; no sidecar/filter implementation; no Task 027 or Stage-1.
- **Dependency:** 026P–026R results: 175 unique BaseSource/JsExtensions/JsEncodeUtils signatures; @JvmSynthetic alone is Rhino-visible; method-level marker filtering is a scratch candidate only with unique non-overloaded host names; data-class body sidecar is not copied and requires reattachment.
- **Next:** after registration commit and required-list reread, execute the read-only lifecycle inventory. A later implementation task must be separately registered and remain subject to Phase A gate. Stage-0 remains in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.52 Task 026S result — BookSource sidecar lifecycle inventory (2026-11-26)

- **Task:** R3-S0-T026-PHASE-A-BOOKSOURCE-SIDECAR-LIFECYCLE-INVENTORY-026S; bounded static inventory complete. Preregistered at HEAD 8685e759207b00d4f52a28425a056bf5b724eead; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Evidence:** /tmp/task026s-sidecar-lifecycle/RESULT.md (SHA-256 3ce8bab2f860c8817400128234aed9139c39717baa7f9fb3c36abe7d445c6db2); complete direct BookSource DAO caller list and constructor/copy/deserialization/BaseSource inventories are stored alongside it.
- **Key finding:** BookSource is an @Entity/@Parcelize BaseSource; source instances are created by direct constructors, Gson/JSON import and JS config, Room materialization, backup restore, legacy conversion, replacement roundtrip and synthetic AutoTask/MCP paths. A body sidecar is not copied by data-class copy (026Q only proved this on a minimal scratch model). BookSource implements BaseSource alongside RssSource, HttpTTS and BookCover.CoverRule; SourceNavigationBridge creates an anonymous BaseSource delegation proxy.
- **DAO closure:** 175 direct bookSourceDao references across 62 main-source files; 79 full BookSource entity-returning read references, 43 BookSourcePart/projection reads, remaining writes/aggregates/flows/checks. Room has no discovered sidecar attachment callback; SourceHelp.getSource alone is not a complete attach point. ReadBook/ReadManga/AudioPlay/VideoPlay hold active source references.
- **Rhino:** App.initRhino registers NativeBaseSource.factory for BookSource/RssSource/HttpTTS. NativeBaseSource already filters setter accessors and restricts variable writes; any 026R marker filter must compose with that factory and cover the navigation proxy. CoverRule follows a different wrapper path.
- **Parcelable:** BookSource is Parcelable, but no direct BookSource Intent/bundle parcel transport was found; routes pass keys/URLs/types and reload from Room.
- **Design candidates:** bind after runtime-bound Room reads, JSON/legacy/JS config/import/replacement/restore parses, edit copies and synthetic construction; invalidate active owners on delete/URL change/replacement with existing mutation-origin order; resolve SourceNavigationBridge to original owner. This is not implementation or dynamic proof.
- **Still NOT MET:** real BookSource Room/Gson/Parcelize/copy behavior, every Runtime entrypoint bind, active holder/script invalidation, NativeBaseSource filter integration, SourceNavigationBridge/java-source-sourceApi identity and product-specific RSS/TTS/CoverRule adapters. No build/test/Room processor/DEX/R8/CI/device/network was run.
- **Gate:** Phase A open; broad BookSource/BaseSource Phase B migration blocked (bounded Task 026M UI-model slice already completed); Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** register a separately scoped design/proof task for the BookSource-only Host Services binding and NativeBaseSource marker-filter integration; do not start production implementation under 026S.

### 4.27.53 Task 026T preregistration — NativeBaseSource marker-filter/delegation proof (2026-11-26)

- **Task ID:** R3-S0-T026-PHASE-A-NATIVEBASESOURCE-MARKER-DELEGATION-PROOF-026T; bounded scratch probe complete, production integration not authorized. Registration start HEAD fce2948464f0492f895609b06fecb1a1e36f5100; execution repository HEAD 221a2219ca8b2e054a519309d9f708e099e0b99f; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Goal:** test the 026R marker filter composed with the actual pinned NativeBaseSource special wrapper and a Kotlin BaseSource delegation proxy; preserve Rhino visibility, setter/variable semantics and original source/Host Services identity.
- **Allowed:** docs-only repository changes; scratch under /tmp/task026t-nativebase-filter-probe/; use only already-cached pinned Rhino/HtmlUnit/compiler artifacts. No network/download/cache injection.
- **Probe:** compose marker filtering into a scratch NativeBaseSource copy while preserving existing has/get/put; verify unique hidden host accessors, ordinary APIs, inherited methods, getIds/enumeration, setter hiding except setVariable, variable property write and mixed-overload fail-open; compile a Kotlin BaseSource by original proxy, inspect delegated method annotations/flags and prove it resolves the original source owner; compare registered special factory paths for BookSource/RssSource/HttpTTS with generic SourceNavigationBridge/CoverRule paths.
- **Acceptance:** exact hashes, pinned Rhino JS visibility output, NativeBaseSource setter/variable behavior, Kotlin delegation/JVM annotation evidence, original-source identity, and explicit limits versus real BookSource/Room/Parcelize/Release R8.
- **Exclusions:** no production Rhino/Runtime/App edits, App compile retry, Gradle/build/test, Room/KSP/Parcelize, DEX/R8/APK, CI/device/network, BookSource/BaseSource migration or Task 027. Positive scratch evidence does not authorize production integration.
- **Status correction:** Phase A remains open. The broad BookSource/BaseSource Phase B migration remains blocked; the only bounded Phase B implementation slice already completed is Task 026M's UI-model ownership work. Task 027 blocked; Stage-0 not accepted; Stage-1 NO-GO; full Legado compatibility NOT ESTABLISHED.
- **Result:** /tmp/task026t-nativebase-filter-probe/RESULT.md SHA-256 93ae91f75592c5d6647712fc71214433a3e8109a2d86fc4dd0334f5c6707cdc3; scratch hash manifest HASHES.txt SHA-256 57bae7e0483c54fbd19736bdf581e94d0a69440a3686f99ffc11833f009be6c3; Rhino output SHA-256 58720a5884985488f36fb07cee1b8d96bfa118e1228dc43891bfb1debb6ce089. Exact NativeBaseSource copy and pinned Rhino/HtmlUnit artifacts were used; all scratch compiles and Rhino execution exited 0. Unique marked Host Services names were hidden from read/has/own-property/enumeration; ordinary source methods remained callable; mixed overloads failed open; Kotlin delegation proxy retained marker+ACC_SYNTHETIC and resolved the original source/Host Services owner. setVariable and variable writes remained functional.
- **Caveat:** generated setKey/setTag reads are undefined but those names still appear in Object.keys(source), because pinned NativeBaseSource does not override getIds. Scratch BookSource/RssSource/HttpTTS/CoverRule types and navigation proxy are stand-ins; real model/SourceNavigationBridge/JsSourceEngine execution, Room/Gson/Parcelize, Release R8, and dynamic lifecycle are not proved. The java/source/sourceApi identity was explicitly assigned by the scratch runner, not verified through real production injection.
- **Gate:** Task 026T is complete only as bounded scratch feasibility evidence. Phase A remains open; broad BookSource/BaseSource Phase B migration remains blocked; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 NO-GO; full compatibility NOT ESTABLISHED.
- **Next:** after the result commit and rereading both required lists, register a narrowly scoped production design/compatibility task for marker-filter integration across NativeBaseSource and generic/delegated BaseSource paths, including an explicit decision on setter enumeration. Do not modify production code or start Task 027 under Task 026T.


### 4.27.54 Task 026T result — NativeBaseSource marker filter and delegated-source identity (2026-11-26)

- **Status:** bounded scratch feasibility probe complete; no production integration. Registration start HEAD fce2948464f0492f895609b06fecb1a1e36f5100; execution HEAD 221a2219ca8b2e054a519309d9f708e099e0b99f; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Evidence:** /tmp/task026t-nativebase-filter-probe/RESULT.md SHA-256 93ae91f75592c5d6647712fc71214433a3e8109a2d86fc4dd0334f5c6707cdc3; HASHES.txt SHA-256 57bae7e0483c54fbd19736bdf581e94d0a69440a3686f99ffc11833f009be6c3; Rhino output SHA-256 58720a5884985488f36fb07cee1b8d96bfa118e1228dc43891bfb1debb6ce089.
- Exact copied NativeBaseSource source matched the pinned file byte-for-byte. App.initRhino registers NativeBaseSource.factory for BookSource/RssSource/HttpTTS. Scratch Kotlin 2.4.20/JDK 21 compilation and pinned Rhino/HtmlUnit execution succeeded; this was not a Gradle build or project test.
- Positive: unique marked Host Services methods are hidden from Rhino get/has/own-property/enumeration; ordinary getKey/getTag/ordinaryMethod remain callable. Kotlin BaseSource delegation emits marked ACC_PUBLIC|ACC_SYNTHETIC methods; Rhino hides them; proxy getSource() and Host Services lookup resolve to the original object/owner. setVariable and variable writes work; unrelated tag assignment is ignored. Mixed marked/unmarked overloads fail open, so host-only names must be unique and non-overloaded.
- **Setter enumeration nuance:** generated setKey/setTag property reads are undefined but Object.keys(source) still lists these names. NativeBaseSource overrides has/get/put, not getIds. Do not claim setter names are completely absent from enumeration.
- Limit: model classes and delegation proxy are scratch stand-ins, not real BookSource/RssSource/HttpTTS/CoverRule/SourceNavigationBridge. The java/source/sourceApi alias was explicitly assigned by the scratch runner, not proven through real JsSourceEngine injection. No real Room/Gson/Parcelize/copy/reattachment, active lifecycle, Release R8/DEX, all JS API/overload, product or device validation.
- No tracked production/test/build/Gradle/workflow/schema/UI/Reader changes; no App compile retry, Gradle build/test, Room/KSP/Parcelize, APK/DEX/R8, CI, device or network.
- **Disposition:** Task 026T complete as bounded scratch feasibility only. Phase A remains open; broad BookSource/BaseSource Phase B migration remains blocked; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** register a separately scoped production design/compatibility task for NativeBaseSource and generic/delegated BaseSource filter integration, including setter enumeration behavior. No production edits or Task 027 under this result.

### 4.27.55 Task 026U preregistration — production Rhino host-member filter integration contract (2026-11-26)

- **Task ID:** R3-S0-T026-PHASE-A-RHINO-HOST-MEMBER-FILTER-INTEGRATION-CONTRACT-026U; completed as a bounded read-only design/compatibility audit, no production implementation. Registration start HEAD 5bb800baca4de2e59ebf4d7a8f9901ac9261efad; audit execution HEAD 851bdc0c16f55a6deb4c542fb1d51e39cab855da; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Goal:** turn 026R/026T scratch feasibility into a path-exact, read-only production integration contract for a runtime-retained host-only marker across NativeBaseSource, generic BaseSource wrappers and Kotlin delegation proxies, without changing code or weakening upstream JS compatibility.
- **Scope:** enumerate all BaseSource implementers and actual wrapper/factory routes; cross-check 026P's 175 unique signatures and inherited JS extensions for unique/non-overloaded host-only names; inspect Rhino get/has/getIds/own-property/enumeration behavior; decide whether NativeBaseSource setter enumeration is preserved or separately changed; define marker placement across interfaces/overrides/delegation; inspect ProGuard/R8 marker retention; trace actual JsSourceEngine java/source/sourceApi and SourceNavigationBridge owner identity; define production tests for BookSource/RssSource/HttpTTS/CoverRule/navigation paths.
- **Allowed:** docs-only audit/overview updates; read-only pinned/Badcat source and build-rule inspection; temporary inventory under /tmp/task026u-rhino-filter-contract/. No tracked source/test/build/configuration edits.
- **Acceptance:** line-backed implementer/factory matrix, method-name collision/overload table, exact Rhino filtering/enumeration contract, marker/R8 retention status, caller identity contract, production test matrix, exact candidate file paths and remaining Phase A GO/NO-GO.
- **Exclusions:** no production Rhino/Runtime/App edits, tests/build/Gradle/dependency resolution/KSP/Room/Parcelize/DEX/R8/APK/CI/device/network, BookSource/BaseSource migration, Task 027, schema/UI/Reader changes. Positive design evidence does not authorize implementation.
- **Dependencies:** 026P signature matrix/freeze; 026Q synthetic sidecar rejection; 026R marker-filter scratch; 026S BookSource lifecycle; 026T NativeBaseSource/delegation scratch result (§24.141).
- **Result:** /tmp/task026u-rhino-filter-contract/RESULT.md SHA-256 442137e97e4d6bc075794e8065aa5aeb8799f4da4d92fca6cb5b2de4af5f7b03; hash manifest HASHES.txt SHA-256 1db042e09ee208cf30a822f2f722842fa417979827d1262288f99771006c59e2; source hash manifest SOURCE-HASHES.txt SHA-256 23d475f0eb84b1adcc76929d2c2202ba51742a10a0c6f2204e27e98432089f06.
- **Key findings:** four concrete implementers (BookSource, RssSource, HttpTTS, BookCover.CoverRule); NativeBaseSource special factory for the first three; CoverRule uses generic CatchableNativeJavaObject. SourceNavigationBridge is actually an Android WebView bridge, not a Rhino wrapper; BaseSource.evalJS, JsSourceEngine and BackstageWebView have distinct java/source/sourceApi identities. Candidate attachHostServices/requireHostServices names are collision-free in the 026P matrix.
- **Critical design constraints:** filter only the CatchableNativeJavaObject object path (do not copy 026T's accidental List/Map/Array filter scope); preserve NativeBaseSource setter enumeration; test property descriptors; use runtime marker with recursive interface lookup and exact JVM descriptors; keep marker-bearing names safe from Release R8 collision; never annotate sidecar methods with Android @JavascriptInterface.
- **Gate:** Task 026U complete as read-only design evidence. Phase A remains open; broad BookSource/BaseSource Phase B migration blocked; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 NO-GO; full compatibility NOT ESTABLISHED.
- **Next:** continue independent Phase A compiled ABI/Release R8, exact dependency-variant and dynamic lifecycle gates. No production filter/sidecar implementation or Task 027 until those gates and a path-exact implementation addendum are reviewed.


### 4.27.56 Task 026U result — production Rhino host-member filter integration contract (2026-11-26)

- **Status:** bounded read-only design/compatibility audit complete; no implementation. Registration start HEAD 5bb800baca4de2e59ebf4d7a8f9901ac9261efad; execution HEAD 851bdc0c16f55a6deb4c542fb1d51e39cab855da; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Evidence:** /tmp/task026u-rhino-filter-contract/RESULT.md SHA-256 442137e97e4d6bc075794e8065aa5aeb8799f4da4d92fca6cb5b2de4af5f7b03; HASHES.txt SHA-256 1db042e09ee208cf30a822f2f722842fa417979827d1262288f99771006c59e2; SOURCE-HASHES.txt SHA-256 23d475f0eb84b1adcc76929d2c2202ba51742a10a0c6f2204e27e98432089f06.
- Four concrete BaseSource implementers: BookSource, RssSource, HttpTTS and BookCover.CoverRule. SourceNavigationBridge is one anonymous delegation proxy, not a persistent source model. App registers NativeBaseSource.factory for BookSource/RssSource/HttpTTS only; CoverRule uses generic CatchableNativeJavaObject.
- Correction: SourceNavigationBridge is used by BackstageWebView as Android WebView.addJavascriptInterface; WebJsExtensions is separately constructed with the original source. It is not a Rhino wrapper in the inspected production route. Host-only sidecar methods must not be annotated @JavascriptInterface.
- Identity: BaseSource.evalJS uses java/source/sourceApi = same BaseSource; JsSourceEngine uses java = JsSourceEngine and source/sourceApi = BookSource; BackstageWebView uses separate source proxy and WebJsExtensions objects.
- 026P matrix: 177 declaration rows / 175 unique signatures / 77 @JavascriptInterface declarations / 130 names / 37 overloaded names. Candidate attachHostServices and requireHostServices have no current declarations or source collisions. Mixed marked/unmarked overloads fail open; production host-only names must be unique/non-overloaded and match exact JVM descriptors.
- Filter belongs in CatchableNativeJavaObject get/has/getIds while preserving CatchableJavaMethodCache. NativeBaseSource inherits it and keeps its current setter/variable rules. NativeJavaObject.getIds delegates to JavaMembers member keys. Preserve pinned setter enumeration: setKey/setTag are hidden on get/has but still appear in Object.keys. Do not silently change it.
- ScriptableObject.getOwnPropertyDescriptor is inherited and uses querySlot; exact Java-member descriptor behavior is not dynamically tested. Add descriptor tests.
- 026T scratch filter was copied into Java Object, List, Map and Array wrappers due broad replacement; production should narrowly filter BaseSource object wrappers, not all collection wrappers.
- RUNTIME marker should live in :legado-rhino or neutral module; recursively inspect interface declarations because Java reflection does not inherit method annotations. Kotlin delegation methods carried marker and ACC_PUBLIC|ACC_SYNTHETIC in scratch. Badcat has no Release minification configuration or marker-retention proof; marker attributes, method retention and name-collision safety under R8 remain NOT ESTABLISHED.
- No tracked source/test/build/Gradle/configuration/schema/UI/workflow changes; no build/test/dependency resolution/DEX/R8/APK/CI/device/network execution. No production implementation or Task 027.
- **Disposition:** Task 026U complete as read-only design evidence. Phase A remains open; broad BookSource/BaseSource Phase B migration blocked; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue Phase A compiled ABI/Release R8, exact dependency-variant and dynamic lifecycle gates. No production filter/sidecar implementation until those gates and a path-exact implementation addendum are reviewed.

### 4.27.57 Task 026V preregistration — Rhino Java-wrapper descriptor/enumeration probe (2026-11-26)

- Task ID: R3-S0-T026-PHASE-A-RHINO-JAVA-WRAPPER-DESCRIPTOR-ENUMERATION-PROBE-026V; completed as bounded scratch dynamic Rhino evidence, no production integration. Registration start HEAD 97d0b963d72ede60d6596da8d15c890300fc4b2b; execution HEAD d4e85d84e4da7d33d00ffc6242a74cd60f7f2922; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Goal: dynamically inspect pinned HtmlUnit Rhino Java wrapper behavior for property descriptors, own-property checks and enumeration after marker filtering, beyond the get/has/Object.keys cases already recorded in 026T.
- Allowed: scratch-only runner/source under /tmp/task026v-rhino-descriptor-probe/; reuse existing compiled pinned Rhino Debug classes, HtmlUnit CoreJS and 026T scratch wrapper/classes. Repository changes limited to this audit and this overview.
- Cases: NativeBaseSource special wrapper and generic CatchableNativeJavaObject; marked Host Services methods, ordinary getKey/getTag, setKey/setTag, setVariable, variable and unrelated tag. Probe get/has/getIds, Object.getOwnPropertyDescriptor, Object.getOwnPropertyNames, Reflect.ownKeys, Object.keys, for..in and hasOwnProperty.
- Acceptance: exact outputs/errors, source/JAR hashes, whether JavaMembers descriptors are slot-backed, and whether own-property/enumeration APIs share getIds/has/get paths; distinguish Rhino engine facts from stand-in model limitations.
- Exclusions: no production source/test/build/Gradle/dependency/DEX/R8/APK/CI/device/network actions; no Room/KSP/Parcelize; no App compile retry; no SourceNavigationBridge/WebView claim; no Task 027.
- Gate: scratch evidence only; Phase A remains open and broad BookSource/BaseSource migration remains blocked pending compiled App ABI/R8, exact dependency and dynamic lifecycle gates.
- Result: /tmp/task026v-rhino-descriptor-probe/RESULT.md SHA-256 07d1bb1a2f8187f0f92fb1995cf5cca47ad8395d36256a819aba94e6fd37d482; HASHES.txt SHA-256 0deab0cb9bbb504e3ea1acc88ac69e9eeeb6f9de628c95969769adc85fba4760; Rhino output SHA-256 76929916e0c7516f5dd09e8612dccddcaa9f6c6559ab7f555b0bf79b11c18ed1.
- NativeBaseSource: marked methods absent from reads, membership, own-property, propertyIsEnumerable, Object.keys and for..in. Setter names setKey/setTag are hidden from reads/hasOwn/propertyIsEnumerable; Object.keys lists them but for..in omits them. Preserve this existing distinction.
- Generic wrapper: marked methods remain hidden; generated setters remain visible in Object.keys and for..in. Object.getOwnPropertyDescriptor, Object.getOwnPropertyNames and Reflect.ownKeys throw the same TypeError for visible and hidden Java members; exact output is retained in RESULT.md.
- Gate: scratch evidence only; Phase A remains open, production implementation and Task 027 remain blocked.
- Next: continue independent Phase A compiled App ABI/Release R8 and real Android lifecycle gates. No production code or Task 027.


### 4.27.58 Task 026V result — Rhino Java-wrapper descriptor/enumeration behavior (2026-11-26)

- **Status:** bounded scratch dynamic Rhino probe complete; no production integration. Registration start HEAD 97d0b963d72ede60d6596da8d15c890300fc4b2b; execution HEAD d4e85d84e4da7d33d00ffc6242a74cd60f7f2922; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Evidence:** /tmp/task026v-rhino-descriptor-probe/RESULT.md SHA-256 07d1bb1a2f8187f0f92fb1995cf5cca47ad8395d36256a819aba94e6fd37d482; HASHES.txt SHA-256 0deab0cb9bbb504e3ea1acc88ac69e9eeeb6f9de628c95969769adc85fba4760; Rhino log SHA-256 76929916e0c7516f5dd09e8612dccddcaa9f6c6559ab7f555b0bf79b11c18ed1.
- NativeBaseSource special wrapper: marked methods absent from reads, membership, hasOwnProperty, propertyIsEnumerable, Object.keys and for..in. Ordinary methods remain visible. Setter names setKey/setTag are hidden on reads/membership/own-property/propertyIsEnumerable; Object.keys lists them but for..in omits them. Preserve this upstream distinction.
- Generic CatchableNativeJavaObject: marked methods hidden; generated setters remain visible in Object.keys and for..in. This matches the distinct factory paths for NativeBaseSource and generic CoverRule.
- Object.getOwnPropertyDescriptor, Object.getOwnPropertyNames and Reflect.ownKeys throw the exact TypeError for visible and hidden Java wrapper members; no descriptor/key list is returned. Do not claim they return null.
- Reused 026T scratch classes and pinned Rhino/HtmlUnit artifacts; only a Java runner was compiled and executed. No project build/test or production changes.
- Limits: scratch stand-ins only; no real Android models, Room/Gson/Parcelize, WebView, Release R8/DEX, real caller injection or source E2E.
- **Disposition:** Task 026V complete as bounded Rhino behavior evidence. Phase A remains open; broad BookSource/BaseSource Phase B migration blocked; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue independently registered Phase A compiled ABI/Release R8 and real Android lifecycle gates. No production implementation until gates and path-exact addendum review.

### 4.27.59 Task 026W preregistration — scratch R8 host-marker retention probe (2026-11-26)

- Task ID: R3-S0-T026-PHASE-A-R8-HOST-MARKER-RETENTION-PROBE-026W; completed as bounded scratch R8 feasibility evidence, no production integration. Registration start HEAD 6141b1f7ffb55246468d81b6247ef88ae77c3aaa; execution HEAD 055cad276279c3b0eaa165b64dcf9c9297285caa; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Goal: test marker annotation retention and host-only method-name preservation with an already-cached R8 executable against 026T scratch classes only.
- Allowed: scratch-only inputs/outputs under /tmp/task026w-r8-marker-probe/; no downloads, Gradle or cache mutation; repository changes only this audit and overview.
- Cases: baseline minification; annotation-attribute retention; annotated method retention with obfuscation allowed; annotated method-name retention; mixed overload/name collision. Inspect mapping, classfile marker annotations, descriptors, synthetic flags and runtime reflection where possible.
- Acceptance: exact cached R8 version/hash, rules/command, output/mapping hashes, annotation survival and method-name collision result, candidate rule set and limitations versus actual Badcat Release.
- Exclusions: stop if R8 is not already cached; no network/download/cache injection, App compile/build, Gradle, project tests, APK/DEX/device/CI, production source/build/shrinker edits, Room/KSP/Parcelize or Task 027.
- Gate: scratch-only; cannot close actual Badcat Release R8 or compiled App ABI. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 NO-GO; full compatibility NOT ESTABLISHED.
- Result: /tmp/task026w-r8-marker-probe/RESULT.md SHA-256 95939c1f1f2fa874e1be8e3a02bd60dfb087d2ce7a415dbafebafab533444515; HASHES.txt SHA-256 966bbfb80b2c321655a0cc7bd3789fcb8c2153b6609ff6b68af28755702a9976; cached R8 9.1.29 JAR SHA-256 9a7d1e9e4c2d3ee05482b9695df5f4d8f92c22ebb74603d6be4eab0505467f1a.
- Case A: without annotation attributes, runtime marker annotations were stripped.
- Case B: with annotation attributes but marker method obfuscation allowed, host-only and ordinary methods collided under the same obfuscated name; a dedicated class mapped one marked and nine unmarked methods to a. The current fail-open filter would expose the marked method.
- Case C: marker-specific keepclassmembers preserved marked method names and annotations on interface, concrete and Kotlin delegation methods while ordinary methods remained obfuscatable.
- Candidate rules require both annotation-attribute retention and preserving marker-annotated method names; actual Badcat Release R8 remains unproved.
- Gate: scratch feasibility only; Phase A remains open; broad migration and Task 027 blocked.
- Next: continue actual App ABI/Release R8 and real Android lifecycle gates. No production changes.


### 4.27.60 Task 026W result — R8 host-marker annotation/name retention (2026-11-26)

- **Status:** bounded scratch R8 feasibility probe complete; no production integration. Registration start HEAD 6141b1f7ffb55246468d81b6247ef88ae77c3aaa; execution HEAD 055cad276279c3b0eaa165b64dcf9c9297285caa; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Evidence:** /tmp/task026w-r8-marker-probe/RESULT.md SHA-256 95939c1f1f2fa874e1be8e3a02bd60dfb087d2ce7a415dbafebafab533444515; HASHES.txt SHA-256 966bbfb80b2c321655a0cc7bd3789fcb8c2153b6609ff6b68af28755702a9976; cached R8 9.1.29 JAR SHA-256 9a7d1e9e4c2d3ee05482b9695df5f4d8f92c22ebb74603d6be4eab0505467f1a.
- Case A: annotation type retained but no annotation attributes caused RuntimeVisibleAnnotations to be stripped; reflection no longer detected the marker.
- Case B: annotation attributes preserved marker metadata, but allowing method obfuscation caused host-only and ordinary methods to share obfuscated names. A collision class mapped one marked method plus nine unmarked methods to the same name a. Name-based fail-open filtering would expose the marked method.
- Case C: marker-specific keepclassmembers preserved marker method names and annotations across BaseSource interface, concrete class and Kotlin delegation proxy; ordinary methods remained obfuscatable and no collision occurred.
- Candidate rule set needs annotation attribute retention, marker annotation class retention, and name-preserving keepclassmembers for marker methods. It remains a candidate only; actual Badcat Release rules/variants have not been built or validated.
- R8 emitted informational duplicate program/library definition notices for the scratch-modified Rhino wrapper classes; all scratch runs exited 0. This was classfile scratch R8, not Android Gradle Release R8.
- No tracked source/build/shrinker/test changes; no App compile/build, Gradle, APK/DEX, CI/device/network or Room/KSP/Parcelize.
- **Disposition:** Task 026W complete as bounded R8 feasibility evidence. Actual Badcat Release marker retention and App ABI remain NOT ESTABLISHED. Phase A open; broad BookSource/BaseSource Phase B migration blocked; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue actual App ABI/Release R8 and real Android lifecycle gates; no production marker/sidecar implementation until all Phase A gates and path-exact implementation addendum are reviewed.


### 4.27.61 Task 026X preregistration — exact-pinned upstream Release APK / DEX ABI artifact recovery

- Task ID: `R3-S0-T026-PHASE-A-UPSTREAM-RELEASE-APK-DEX-ABI-RECOVERY-026X`; read-only external CI artifact provenance/DEX inspection in progress. No build or R8 execution.
- Snapshot: Badcat HEAD `818f967e0d3d34e14f6b559258e90dbaaeeee950`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Purpose: determine whether an existing official Release APK from the exact upstream pin can supply real compiled App ABI/R8 evidence without violating §24.119's no-more-App-compile restriction.
- Allowed: existing public GitHub Actions artifact/job logs, SHA-256, ZIP inventory and read-only Android SDK `apkanalyzer` inspection; temporary files under `/tmp/task026x-upstream-release-artifact/`. Tracked changes only this overview and the architecture audit.
- Required: exact run/commit/artifact provenance; Release/R8 task result; APK/Dex hashes; target class/method/annotation retention; class-name obfuscation and mapping-file availability; explicit separation of upstream baseline from Badcat integration.
- Excluded: Gradle/compiler/build/test/dependency resolution/R8 execution/CI re-run/device and all production changes. No Task 027.
- Gate: may close only pinned-upstream compiled Release ABI/R8 baseline; Badcat integrated ABI, host marker retention and Android lifecycle remain separate.


### 4.27.62 Task 026X result — exact-pinned upstream Release APK / DEX ABI recovery (2026-11-26)

- **Status:** complete for the exact pinned-upstream Release ABI/R8 reference slice only. Registration/execution Badcat HEAD: `1d5f9b2911789840ef936cabaab446962672318c`; pinned upstream: `330e606c4d57e08adc056ff8ab7a4132c784da9a`. Full evidence: audit §24.149 and `/tmp/task026x-upstream-release-artifact/RESULT.md`.
- Official upstream GitHub Actions run [41291314673](https://github.com/LegadoTeam/legado/actions/runs/41291314673) built the exact pinned SHA. Release job 124803029014 checked out that exact ref, ran `:app:minifyAppReleaseWithR8` and `:app:assembleRelease`, and ended `BUILD SUCCESSFUL in 7m 6s`. Artifact ID 13077223975; ZIP SHA-256 `1c12b50cb5229afb78e5b26c7ac6ff07943bc976d3e1f37e3ad0d76280b611d4`.
- ARM APK SHA-256 `e5cfb391939d1f68ae846527503d115a10ed5cb94ba83ccf1ec58e1ab6566f3d`; universal APK SHA-256 `31e5a1db1ea47508a1c8193e8e9c1b5aeb49b585317e1e8466d2851e7038e0b3`. Both package `com.legado.app.release` version `3.26110117`; their 3-DEX class listings are byte-identical (211,032 lines, SHA-256 `63c00eaf89f3c29117d3fa55b0b703d55d90adc1fc32cb52285bf2537fad0067`).
- Release DEX preserves source-name classes BaseSource (71 methods), BookSource (306), RssSource (330), HttpTTS (223), JsExtensions (148), JsSourceEngine (161). BaseSource/BookSource/JsSourceEngine expose the expected key/tag/source/evalJS/variable members; BookSource.getSource() returns the same object (DEX returns p0), getKey reads bookSourceUrl, getTag reads bookSourceName. Runtime @JavascriptInterface annotations survive: BaseSource 29, JsExtensions 68, BookSource 77, RssSource 79, HttpTTS 77, JsSourceEngine 68. This reconciles the 026P 77-annotation source matrix with real upstream Release DEX.
- SharedJsScope, ScriptBindings and NativeBaseSource original class names are not found; no R8 mapping file was uploaded, so their obfuscated names cannot be mapped. Proposed RhinoHostOnly marker is absent from upstream and its Badcat Release retention remains unproved.
- **Gate delta:** pinned-upstream compiled ABI/Release R8 reference slice is now evidenced. Current Badcat integrated App ABI, Badcat Release R8/host-marker retention, sidecar serialization/reattachment and real Android lifecycle tests remain open. This artifact is not a Badcat APK and cannot close those gates.
- No local Gradle/compiler/build/test/R8/CI/device execution or production changes occurred. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**. Phase B remains limited to the already completed Task 026M model slice.
- **Next:** use the upstream Release DEX as the pinned ABI baseline while continuing low-memory static/scratch proof for Badcat's sidecar/filter/lifecycle contracts. Do not retry App compilation under §24.119.


### 4.27.63 Task 026Y preregistration — existing Badcat Debug DEX integration-gap audit

- Task ID: `R3-S0-T026-PHASE-A-BADCAT-DEBUG-DEX-INTEGRATION-GAP-026Y`; registered / not started. Start HEAD `bb2c969db641408a17371ca9dd8aa39f1933c90f`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Goal: inspect existing Badcat CI Debug APK/DEX and host JVM ABI artifacts from run `45600808333` (head `288d14f178bc807f7729711a77dad0611f487b07`) and compare source changes since that snapshot to current HEAD. Determine whether authentic upstream BaseSource/BookSource ABI is already integrated or the app still exposes only the custom/shadow host facade.
- Existing artifact IDs: Debug APK `15015902961`; DEX definitions `15016925348`; host JVM ABI `15016830848`; JS ABI inventory `15015078332`; Rhino surface `15015413030`.
- Allowed: existing artifact retrieval, hash verification and read-only `apkanalyzer` inspection; source diff/FQCN inspection; temporary files under `/tmp/task026y-badcat-debug-dex/`. No new build/CI/test/device action.
- Acceptance: exact historical run/artifact provenance, APK/DEX identities, class presence/absence, host facade ABI and current-source delta; explicitly mark the artifact as historical Debug evidence, not current HEAD or Release R8 proof.
- Excluded: Gradle/compiler/build/test/dependency resolution/CI re-run/device and all production changes; no App compile retry; no Task 027.
- Gate: this task can confirm the integration gap only; current Badcat App ABI/R8 and Android lifecycle remain open.


### 4.27.64 Task 026Y result — existing Badcat Debug DEX confirms the upstream integration gap (2026-11-26)

- **Status:** complete for the historical Debug artifact and current source-gap reconciliation only. Registration/current source HEAD `515fe8a6bb1db3faa58a9ecd6f23a7d605c960c1`; historical Badcat CI HEAD `288d14f178bc807f7729711a77dad0611f487b07`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`. Full evidence: audit §24.151 and `/tmp/task026y-badcat-debug-dex/RESULT.md`.
- Existing Badcat CI run [45600808333](https://github.com/xkszds/badcat-legado-client/actions/runs/45600808333), job `assemble-debug` 139158905915, succeeded at its exact historical SHA. It built a 14-DEX Debug APK, ran third-category/model contract tests, CookieManager API 28 and WebView API 35 instrumentation, captured host JVM ABI and verified RuleAnalyzer DEX. Live-source best-effort steps remain external Python probes, not Runtime E2E.
- APK SHA-256 `23fe1e5731d6f448abeccbcfe56ce8c87946e22e6d1fd33103f0fa1bc3a21241`; package `app.badcat.client` version `0.1.0-integration`. Its DEX has zero definitions for upstream BaseSource, BookSource, RssSource, HttpTTS, JsExtensions and JsSourceEngine. It instead contains custom `app.badcat.engine.BookSource`, `LegadoBookSource`, `SourceScriptRuntime`, `RhinoSafeSourceJavaExtensions` and related custom adapters.
- Existing JS ABI inventory: 148 upstream declarations/104 names vs 150 host declarations/110 names; 17 missing names, 22 overload-count differences, 38 same-name signature differences and 23 host-only names. Actual Rhino surface binds separate custom java/cookie/source/cache objects; scope.java is not BaseSource.
- Source diff from 288d14 to current HEAD adds only FlexStyleLayoutAdapter and moves the three UI/rule models; it adds no authentic BaseSource/BookSource/JsExtensions/JsSourceEngine or sidecar/filter implementation. The old APK is not an exact-current-HEAD build, and the new adapter remains uncompiled, but the current source tree still has the same core integration gap.
- **Gate:** this confirms the current integration gap; it does not close current Badcat ABI/R8, marker retention or Android lifecycle. The historical CookieManager/WebView tests do not cover BookSource sidecar, Room/Gson/copy/restore, active script cancellation, cache/Explore/InfoMap or resource lifecycle.
- No new build/test/CI/device run or production changes. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** continue the path-exact BaseSource/BookSource sidecar/filter implementation design and low-memory module/scratch verification; do not retry App compilation under §24.119 or claim Android lifecycle proof.


### 4.27.65 Task 026X supplementary result — compiled JS scope identity in upstream Release DEX

- Exact pinned upstream Release APK DEX now proves the BaseSource invocation contract: the compiled `BookSource.evalJS` bridge passes the same source instance to the R8-renamed BaseSource default helper; that helper binds the same `BaseSource` object as `java`, `source` and `sourceApi`. `baseUrl` is `getKey()`; CookieStore and CacheManager are bound.
- The compiled `JsSourceEngine.buildScope` contract differs intentionally: `java` is the JsSourceEngine instance; `source` and `sourceApi` are the same BookSource instance. This is now backed by DEX, not only source inspection. Full dump/hash: `/tmp/task026x-upstream-release-artifact/BaseSourceDefaultImpls.smali.txt`, SHA-256 `4210cb0df081902ca9dc47207a8d217e4df35d37ad236bfc00223fedacf8bed4`; updated RESULT.md § Compiled JS scope identity proof.
- Existing Badcat inventory has separate custom java/cookie/source/cache objects and no sourceApi binding, confirming a concrete identity/API mismatch that must be resolved by the future typed per-source Host Port integration.
- This closes only the pinned-upstream Release DEX binding-identity reference slice. Badcat integration, sidecar lifecycle, Release marker retention and Android lifecycle remain open; no build/test/CI/device or production changes.


### 4.27.66 Task 026Z preregistration — SharedJsScope / execution-state Host Port caller closure

- Task ID: `R3-S0-T026-PHASE-A-SHARED-JS-SCOPE-EXECUTION-STATE-PORT-CLOSURE-026Z`; registered / not started. Start HEAD `2578baba73e3386a204a117148bdd4ce92f68681`; upstream pin `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Goal: reconcile exact upstream SharedJsScope/BaseSource.evalJS/JsSourceEngine scope ownership, cache/hash/lock/invalidation and callers with Badcat's current RhinoSourceScriptRuntime. Determine whether SharedJsScope should remain Runtime-owned and whether a separate SharedJsScopePort is necessary; define only minimal typed execution-state/Host Port contracts supported by exact callers.
- Allowed: read-only pinned source/current Badcat source/caller and existing upstream Release DEX inspection; output under `/tmp/task026z-shared-scope-port/`; tracked changes only this overview and audit.
- Required: line-backed method/caller/owner matrix; same-source serialization/cross-source concurrency; scope/hash/cache invalidation; process/per-source cleanup; typed host side effects; exact DEX corroboration; dynamic tests still missing.
- Excluded: all production changes, build/compiler/test/dependency resolution/DEX/R8/CI/device/network and App compile retry; no Task 027.
- Gate: design refinement only; Phase A remains open; no production sidecar/filter or broad migration.


### 4.27.67 Task 026Z result — SharedJsScope is Runtime-owned; source execution is not globally serialized (2026-11-26)

- Task 026Z complete for static source/caller/ownership and existing Release DEX reconciliation only. Registration HEAD `2578baba73e3386a204a117148bdd4ce92f68681`; execution source HEAD `2a2f6eb64fa8473bc15be07651c9cc7d6d4999df`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`. Full result: `/tmp/task026z-shared-scope-port/RESULT.md` SHA-256 `1e92a7b73cbc4e6a4118435359f8e05af2c65ba8827e9b35dee38ea84e5b33f4`.
- **SharedJsScopePort decision:** do not create an App-facing port. Keep shared library scopes, weak/LRU caches, creation locks, CryptoJS owner/thread scopes and ScriptBindings shared-global generations in Runtime/Rhino. Inject only narrow App-owned CryptoJS asset, shared-library fetch, shared-library cache and diagnostics adapters; exact signatures remain open because upstream uses appCtx assets, source-less global OkHttp, ACache and Debug/AppLog.
- **SourceExecutionStatePort correction:** upstream does not globally serialize same-source scripts. BaseSource.evalJS, JsSourceEngine, AnalyzeRule and AnalyzeUrl each create a fresh ScriptBindings/VarScope; the shared jsLib scope is a parent and source globalThis is a separate generation-keyed ConcurrentNativeObject. SourceLock is explicit per-name JS locking; ConcurrentRateLimiter gates requests. Do not introduce a per-source mutex as an assumed upstream behavior.
- **Badcat mismatch:** current RhinoSourceScriptRuntime keeps one persistent ScriptBindings per sourceId and serializes evaluate/getString/putString/clear through a process-static Mutex. It initializes jsLib/mainJs once in that persistent lexical scope and resets on either hash change. This differs from upstream fresh invocation scopes and separate shared library/global state. Current SourceScriptRuntime has no SourceExecutionStatePort or SharedJsScopePort; clearSourceState is a concrete runtime method and catalog invalidation is split between a no-op default and SourceCatalogActivity's separate runtime callback.
- **JS identities:** BaseSource.evalJS binds java/source/sourceApi to the same BaseSource; JsSourceEngine binds java to itself and source/sourceApi to the same BookSource; AnalyzeRule/AnalyzeUrl have their own java objects. One attached BaseSource sidecar can serve source-bound host effects, but each call path must preserve its own bindings/context. JsSourceConfig has no source instance.
- **DEX:** existing upstream Release DEX corroborates fresh JsSourceEngine ScriptBindings allocation/mainJs evaluation and BaseSource identity; no SharedJsScope R8 mapping exists. Source test declarations cover concurrent global writes, library-scope initialization and invalidation but were not executed in this task.
- No build/compiler/test/dependency resolution/DEX/R8/CI/device/network run and no production changes. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** freeze exact App asset/fetch/cache/diagnostic adapters and separate same-object sidecar attachment from source/library invalidation before implementation; preserve fresh per-call scopes and upstream concurrency semantics. No App compile retry under §24.119.


### 4.27.68 Task 026AA preregistration — SharedJsScope host adapters and BaseSource lifecycle contract

- **Task ID:** R3-S0-T026-PHASE-A-SHARED-LIBRARY-HOST-ADAPTER-AND-SOURCE-LIFECYCLE-CONTRACT-026AA; registered / not started. Start HEAD 71e35bb35c7c72e2db1798f620bac06e2cbd9a52; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Goal:** freeze exact typed App adapter contracts for SharedJsScope's CryptoJS asset, source-less shared-library HTTP fetch, ACache storage and diagnostics; separately freeze same-object BaseSource sidecar attachment and source-key/jsLib invalidation lifecycle. Determine whether existing typed ports suffice without creating SharedJsScopePort or a same-source execution mutex.
- **Allowed:** read-only pinned/Badcat source and caller/build/test inspection; temporary evidence under /tmp/task026aa-shared-library-host-adapters/; tracked changes only audit and overview.
- **Required:** exact HTTP client/default/retry/redirect/cookie/header/charset/cancel semantics; ACache namespace/key/size/TTL/remove; resource/logging effects; existing port implementation comparison; sidecar identity/reattachment and all mutation-origin invalidation ordering; dynamic tests and unknowns.
- **Excluded:** all production/test/Gradle/dependency/shrinker/schema/UI changes and all build/test/DEX/R8/CI/device/network actions; no App compile retry; no adapter/sidecar implementation or Task 027.
- **Gate:** design-only; Phase A remains open, broad BookSource/BaseSource migration blocked, Task 027 blocked, Stage-0 not accepted, Stage-1 NO-GO, full compatibility NOT ESTABLISHED.


### 4.27.69 Task 026AA result — SharedJsScope host dependencies and BaseSource lifecycle boundary (2026-11-26)

- Task 026AA complete for static adapter/lifecycle contract closure only. Registration/execution HEAD 13bec4238d282adc7760441fa8b069662d515f37; upstream pin 330e606c4d57e08adc056ff8ab7a4132c784da9a. Full result: /tmp/task026aa-shared-library-host-adapters/RESULT.md SHA-256 f46094aa6976b540a06056c43f04e12ed3aedd6c752e2a6fd5b245bd55217c40.
- **No SharedJsScopePort:** keep scope/cache/lock/generation behavior Runtime-owned. Four App effects need narrow typed adapters: CryptoJS asset text, source-less shared-library HTTP, global shared-library cache and diagnostics. No such production ports exist today.
- **HTTP mismatch/security:** upstream fetch is URL-only GET, no source cookies/headers, status body consumed without status validation, charset/BOM decoding, 60s call timeout and cancellation-aware OkHttp await wrapped in runBlocking without caller context. Its global client follows HTTPS redirects and trusts all TLS certificates/hostnames. Badcat SourceRequestClient is source-scoped, cookie-aware, validates URLs and refuses HTTPS downgrade. Do not reuse as-is or weaken globally; explicit isolated TLS policy decision remains open.
- **Cache:** ACache path cacheDir/shareJs, MD5(URL) logical key, 50,000,000-byte decimal LRU budget, no TTL for these calls. This corrects §24.154/overview §4.27.67 shorthand of 50 MiB. Existing source file cache is sourceId-keyed SHA-256/AtomicFile with a 50 MiB budget and is not a direct substitute.
- **Diagnostics:** Debug.log and AppLog.putDebug are separate channels; AppLog is recordLog-gated and in-memory/event-based; HttpLogInterceptor is separately recordHttpLog-gated and redacted. No diagnostics port exists.
- **Sidecar:** four concrete BaseSource implementers remain BookSource/RssSource/HttpTTS/CoverRule; SourceNavigationBridge is a WebView delegation proxy, not Rhino. There is no production sidecar owner or attach/require method. 026S found 175 direct BookSource DAO references across 62 files, 79 full-entity reads and no central repository/materialization callback. Attach to original runtime-bound objects before execution and reattach after Room/Gson/import/restore/copy; exact central binder remains unproven.
- **Invalidation:** source class+key shared-global invalidation, jsLib-wide scope/cache/global invalidation and product cookie/session/variable/file cleanup are distinct. Existing mutation paths differ: SourceHelp delete clears source state before DAO; same-key edits can direct-delete/insert without source-key clear; jsLib changes remove old library; API/import/restore paths may bypass common cleanup. Active scripts are not cancelled by SharedJsScope invalidation.
- No build/compiler/test/dependency resolution/DEX/R8/CI/device/network run and no production changes. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** resolve isolated shared-library TLS policy and prove a central runtime-entry sidecar binder (or enumerate all entry points) before updating the implementation addendum. No App compile retry under §24.119.


### 4.27.70 Task 026AB preregistration — BaseSource runtime-entry binding closure

- Task ID: R3-S0-T026-PHASE-A-BASESOURCE-RUNTIME-ENTRY-BINDING-CLOSURE-026AB; registered / not started. Start HEAD 53fc7df485414746b65b5b9877bf7d1dc408b9db; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Goal: prove whether one Runtime/App boundary can attach typed Host Services before every BaseSource host-effect path, or enumerate the exact per-entry binder calls required. Distinguish Room/Gson materialization from runtime-bound object entry; preserve BaseSource identity and per-entry JS bindings.
- Required: direct/indirect BaseSource.evalJS callers; JsSourceEngine and AnalyzeRule/AnalyzeUrl constructors/callers; null-source/config paths; BackstageWebView separation; current Badcat composition roots; attachment after copy/import/restore only for Runtime-bound instances; closed binder callsite matrix.
- Allowed: read-only pinned/Badcat source and caller inspection, reusing 026K–026P and 026S–026AA evidence; temporary output under /tmp/task026ab-source-runtime-entry-binding/; tracked changes only audit and overview.
- Excluded: all production/test/build/Gradle/dependency/DEX/R8/CI/device/network work; no App compile retry; no sidecar/binder implementation, Task 027 or TLS policy decision.
- Gate: design-only; Phase A remains open, broad BookSource/BaseSource migration blocked, Task 027 blocked, Stage-0 not accepted, Stage-1 NO-GO, full compatibility NOT ESTABLISHED.


### 4.27.71 Task 026AB result — BaseSource runtime-entry binding closure (2026-11-26)

- Task 026AB complete for static source/caller closure. Registration/execution HEAD 53fc7df485414746b65b5b9877bf7d1dc408b9db; upstream pin 330e606c4d57e08adc056ff8ab7a4132c784da9a. Full result: /tmp/task026ab-source-runtime-entry-binding/RESULT.md SHA-256 6ce472b6ad3d9ffebf53688990074f1f8d4c4ff43debe681a4b82dc0e37e6f6d.
- No existing universal binder callsite exists. Upstream has 70 evalJS references across 24 files and 19 direct host-effect references across 10 files; 175 direct BookSource DAO references across 62 files and no central repository/materialization callback. A reusable typed binder is feasible, but it must be invoked at each outer runtime/host boundary.
- AnalyzeUrl init calls source.getHeaderMap synchronously before its constructor returns, so attach before constructing source-bound AnalyzeUrl. AnalyzeRule's web-JS path also calls source.getHeaderMap before BackstageWebView. BaseSource.evalJS, JsSourceEngine, AnalyzeRule and AnalyzeUrl have different java/source/sourceApi bindings; null-source AnalyzeRule/AnalyzeUrl and JsSourceConfig do not need a BaseSource sidecar.
- BackstageWebView re-queries BookSource by key from Room and constructs a new SourceNavigationBridge proxy plus WebJsExtensions with that new original object. Bind that exact re-queried instance before registering either bridge. SourceNavigationBridge is WebView plumbing, not Rhino.
- Current Badcat factory is one composition root for its custom DTO runtime, but SourceLoginUiV2Activity and SourceCatalogActivity create separate runtime instances; service/evaluator/gateway defaults can also create independent runtimes. No BaseSource binder exists in Badcat production code.
- Future shape: one BaseSourceRuntimeFacade for all source-bound operations, explicit binder before retained direct BaseSource calls, before SourceLogin/WebJsExtensions dispatch, before source-bound AnalyzeUrl construction, and after BackstageWebView's own DAO re-query. Do not bind every DAO read or only inside RhinoScriptEngine/SourceHelp.getSource.
- No build/compiler/test/dependency resolution/DEX/R8/CI/device/network run and no production changes. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** update the implementation addendum only after resolving the isolated shared-library TLS policy; no sidecar implementation or App compile retry under §24.119.

### 4.27.72 Task 026AC preregistration — isolated SharedJsScope TLS policy and addendum refresh (2026-11-26)

- **Task ID:** `R3-S0-T026-PHASE-A-SHARED-LIBRARY-TLS-POLICY-AND-ADENDUM-REFRESH-026AC`; registered / documentation-only at start HEAD `18ee9dbb70e16419a4a5dd038d8e21ae3ba26d13`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Decision:** the user selected option 1 for SharedJsScope shared-library downloads only: trust all TLS certificates and hostnames and allow HTTPS-to-HTTP redirects, matching pinned upstream. This deliberately accepts interception/hostname-spoofing and downgrade risk for this narrow request class.
- **Required isolation:** implement later as a dedicated URL-only shared-library HTTP client with no source cookies/headers/source identity. Do not reuse or weaken SourceRequestClient, ordinary source HTTP, login, TTS, image or other network clients; no global TLS policy change.
- **Scope:** document-only refresh of the path-exact Phase B addendum, reconciling Task 026AA §24.156 and Task 026AB §24.158. No production client, sidecar, marker filter or Host Port implementation.
- **Exclusions:** no source/test/Gradle/build/dependency/DEX/R8/CI/device/network action; no App compile retry under §24.119; no Task 027.
- **Next:** after this registration commit and required-list reread, record the selected TLS policy and explicit runtime-entry binding points in the addendum. Phase A remains open; broad BookSource/BaseSource migration blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.

### 4.27.73 Task 026AC result — isolated SharedJsScope TLS policy recorded (2026-11-26)

- **Status:** complete as a documentation-only decision/addendum refresh. Registration HEAD 18ee9dbb70e16419a4a5dd038d8e21ae3ba26d13; registration commit aa71295779b2ce0a0645cf42a5428dd7ee47f580; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9.
- **Decision:** option 1 selected for SharedJsScope shared-library downloads only: trust all TLS certificates and hostnames and allow HTTPS-to-HTTP redirects, matching pinned upstream. This deliberately accepts interception/hostname-spoofing and downgrade risk for that narrow path.
- **Isolation:** future dedicated URL-only shared-library HTTP client; no source ID, source CookieJar/cookies, source headers or login/session context. Do not reuse or weaken SourceRequestClient, ordinary source HTTP, login, TTS, image or WebView networking. Ordinary source HTTP retains strict TLS and HTTPS downgrade refusal. No global TLS policy change.
- **Addendum:** docs/audits/legado-runtime-3.0-phase-b-implementation-addendum.md §10.7 now records the TLS request contract, exact preserved upstream timeouts/retry/redirect/charset/cancellation behavior, security risk, required positive/isolation tests, and Task 026AB's explicit BaseSource runtime-entry bind points.
- **Validation boundary:** docs/diff review only. No production HTTP client, Host Port, sidecar, marker filter or facade was implemented. No build/test/Gradle/dependency resolution/DEX/R8/CI/device/network action; no App compile retry under §24.119.
- **Disposition:** Task 026AC complete for decision/addendum documentation. Phase A remains open; broad BookSource/BaseSource migration remains blocked; Task 027 blocked; Stage-0 in_progress / not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** register a separate bounded Phase A task for an unmet non-build proof; do not start production HTTP/sidecar/filter implementation or retry App compilation under §24.119.
### 4.27.74 Task 026AD preregistration — existing Runtime dependency evidence reconciliation (2026-11-26)

- **Task ID:** `R3-S0-T026-PHASE-A-EXISTING-DEPENDENCY-EVIDENCE-RECONCILIATION-026AD`; registered/read-only at start HEAD `f45344cb7a4845dd97fc148457813445a07f2c29`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Trigger:** Task 026AC §24.160 identifies the next Phase A action as a bounded non-build proof. Task 026K mapped the 33 candidate source files/703 direct imports/23 external package groups; Task 026L froze source dispositions; existing Badcat 44-configuration and pinned-upstream 10 App-configuration reports do not establish the proposed minimal authentic Runtime source-set dependency closure.
- **Goal:** reconcile only already-existing immutable reports, source/import matrices and Gradle declarations into a path-exact dependency-evidence matrix for the proposed authentic `:legado-runtime` closure. No Gradle or dependency-resolution rerun.
- **Allowed:** read-only audit/overview updates; inspect current Gradle declarations and existing Task 026K/026L/026.9/026.33 evidence. Temporary reconciliation output under `/tmp/task026ad-dependency-evidence/`.
- **Required:** map each candidate source/import and external coordinate to existing Badcat Runtime/App Debug/Release and pinned-upstream App Debug/Release/test metadata evidence; distinguish Runtime core from App Host Port dependencies; retain requested/selected versions and absent variants; identify missing reports, unresolved transitive edges and exact future checks.
- **Exclusions:** no Gradle/dependency resolution, build/compiler/test, artifact staging/download, DEX/R8/APK/CI/device/network, production/source/test/Gradle/shrinker/schema/UI/Reader edits, App compile retry under §24.119, BookSource/BaseSource migration, Task 027 or Stage-1.
- **Acceptance boundary:** a report-backed candidate dependency graph only. Metadata reports are not artifact-file resolution, compilation, ABI, R8 or build proof. Missing evidence must remain NOT ESTABLISHED.
- **Next:** record results in audit §24.162 and this overview, reread both required lists and result, then select the next unmet Phase A gate. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.

### 4.27.75 Task 026AD result — existing dependency evidence reconciled (2026-11-26)

- **Status:** complete for read-only report/source/declaration reconciliation only. Registration start HEAD f45344cb7a4845dd97fc148457813445a07f2c29; registration correction commit 04fce035893ee896bfb6445794d3bd1ab25cca0a; execution source HEAD 04fce035893ee896bfb6445794d3bd1ab25cca0a; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Evidence:** Task 026K provides 33 candidate paths, 703 direct imports, 489 internal owners, 23 external package groups, zero unresolved imports, five overload sites, 37 Host Port rows, 88 upstream dependency declarations and 10 plugin declarations. Full matrix and input hashes: /tmp/task026ad-dependency-evidence/RESULT.md, dependency-reconciliation.tsv, INPUT-HASHES.sha256 and CHECKSUMS.sha256.
- **Existing report integrity:** all 44 Badcat dependency reports (manifest SHA-256 211b033ba3efb592b862c6a90d404b45c5558e80491567ba6cd4bcd4e537aba9) and all ten pinned-upstream full-App dependency reports were hash-verified; every report contains BUILD SUCCESSFUL and zero FAILED markers. These are dependency metadata reports, not project builds or test executions. Badcat reports are from 55b8f102; current Gradle declaration files are unchanged since that snapshot. Upstream reports pin the exact upstream SHA.
- **Key gap:** current :legado-runtime graph has only Kotlin stdlib, Parcelize runtime, Gson and AndroidX annotation, and only eight reviewed rule/Gson files. It has no :legado-rhino project edge and lacks Room common, Commons Text, coroutines-core, AndroidX collection, Hutool and Jsoup on its own compile graph. App or Rhino implementation dependencies do not automatically flow backwards into Runtime.
- **Host split:** AppCompat/Lifecycle callbacks, Glide, Media3, Flexbox View mutation, Splitties appCtx, Android Context/resources and Room runtime/DAO/schema remain host-side or conditional. Preserve source-visible APIs through typed ports; do not import product UI/Reader modules.
- **Unresolved mismatch:** pinned upstream quick-transfer coordinate is com.github.liuyueyi.quick-chinese-transfer:quick-transfer-core:0.2.17; current Badcat declares com.github.liuyueyi:quick-transfer-core:0.2.17. Do not assume they are equivalent.
- **Disposition:** Task 026AD complete as a metadata/source evidence reconciliation; minimal authentic Runtime dependency closure remains NOT ESTABLISHED. No Gradle/dependency resolution, build/compiler/test/DEX/R8/CI/device/network action and no production changes. No App compile retry under §24.119.
- **Next:** a clean adequately provisioned runner and separately registered narrow Runtime variant-resolution/compile task are required to prove the actual source closure. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.

### 4.27.76 Task 026AE preregistration — clean runner readiness for Runtime-only gate (2026-11-26)

- **Task ID:** R3-S0-T026-PHASE-A-RUNTIME-ONLY-RUNNER-READINESS-026AE; registered/read-only at start HEAD bc3c0487a09a0e8e0a8a5034628247466de2fdb8; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Trigger:** Task 026AD completed existing dependency-report reconciliation but the authentic Runtime closure remains unproved. Its next step calls for a clean, adequately provisioned runner and a separately registered narrow Runtime-only variant-resolution/compile task. §24.119 permanently prohibits another App compile retry.
- **Goal:** read-only runner/toolchain/worktree/artifact preflight to decide whether a Runtime-only dependency/compile probe can be safely registered. No build or dependency resolution in this task.
- **Allowed:** device resource/toolchain/SDK/process inspection; Git branch/HEAD/status/submodule inspection; existing 026AD report/hash and cached-artifact inspection; temporary output under /tmp/task026ae-runner-readiness/. Tracked changes limited to audit and this overview.
- **Required:** memory/swap/disk/CPU, JDK/Gradle/SDK, active compiler/Gradle process state, exact Badcat/upstream identities and clean worktrees, candidate source/report availability, resource-based GO/NO-GO and exact next action.
- **Exclusions:** no Gradle invocation/dependency resolution/build/compiler/test/cache mutation/download/process termination/DEX/R8/APK/CI/device/network; no App compile retry; no production/source/test/Gradle/schema/UI/Reader change; no BookSource/BaseSource migration, Task 027 or Stage-1.
- **Next:** after registration commit and required-list reread, execute only the read-only preflight. If runner capacity is inadequate, stop and request a clean higher-memory runner; if adequate, separately preregister the Runtime-only probe. Phase A remains open; Stage-0 not accepted; Stage-1 NO-GO; full compatibility NOT ESTABLISHED.

### 4.27.77 Task 026AE result — connected runner is under-provisioned (2026-11-26)

- **Status:** read-only readiness preflight complete with a disclosed command-scope deviation. Registration start HEAD bc3c0487a09a0e8e0a8a5034628247466de2fdb8; registration commit/execution HEAD 3d9d53a7ac02d472234d14b8cecf14a8885f65cb; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Result documentation commit:** 10a855c9e1384f33425ffd230e0a37f6e0d13158.
- **Resources:** VM-0-5-ubuntu has 2 vCPU, 1.89 GiB RAM (~1.26 GiB available), 1.94 GiB swap (~274 MiB used), and 23 GiB free disk. No competing Gradle/Kotlin process. Only one connected runner is online.
- **Toolchain:** JDK 21.0.13.1, Gradle distribution 8.14.4 and Android SDK 36/Build Tools 35.0.0 + 36.0.0 are present. A read-only gradle --version probe selected system Java 17 because JAVA_HOME was not exported; direct JDK 21 was verified. This was a procedural deviation from the task's no-Gradle-invocation exclusion, disclosed in audit §24.164 and RESULT.md; no project task or dependency resolution ran.
- **Identity/evidence:** Badcat HEAD 3d9d53a7ac02d472234d14b8cecf14a8885f65cb and upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a are both clean. Task 026AD report checksums reverify. Full preflight report: /tmp/task026ae-runner-readiness/RESULT.md SHA-256 c7a700d6bc2737a9a5c79ba104e31c3ed1a33cf787540debfdc6ab8df4bba432.
- **Decision: NO-GO.** 1.89 GiB RAM / 2 vCPU is too close to the prior 1.19–1.31 GiB Gradle RSS and memory-stop envelope to call this a safe clean runner for the 33-path Runtime closure. Swap is not a substitute. No alternative higher-memory runner is connected.
- **Minimum next runner:** clean Ubuntu x86_64, 8 GiB RAM minimum (16 GiB preferred), 4 vCPU, 20 GiB free disk, JDK 21, Gradle 8.14.4, Android SDK 36/Build Tools 36.0.0, exact clean source identities, no competing daemons and network or verified immutable artifacts; explicitly export JAVA_HOME/PATH.
- No project Gradle task, dependency resolution, compiler/build/test, cache mutation, artifact staging/download, process termination, DEX/R8/APK/CI/device/network action; no production changes; no App compile retry under §24.119.
- **Disposition:** Task 026AE complete as readiness assessment with the disclosed deviation; Runtime-only dependency/compile gate is blocked by runner capacity. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** connect a clean higher-memory runner, then separately preregister the Runtime-only dependency/compile probe. No App compile retry.

### 4.27.78 Task 026AF preregistration — crypto/transliteration dependency caller closure

- **Task ID:** R3-S0-T026-PHASE-A-CRYPTO-AND-TRANSLITERATION-DEPENDENCY-CLOSURE-026AF; start HEAD f89bb62cb43b9988301aa2c5d5cf0bc37b603a82; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Trigger/goal:** 026AD left Hutool crypto, BouncyCastle and quick-transfer recursive caller/dependency disposition open. With no safe Gradle runner, perform a bounded read-only source/API/caller audit to classify Runtime-visible requirements versus App/product-only use; do not claim a resolved Gradle graph.
- **Allowed:** exact pinned source/import/caller reads; Gradle/catalog declaration reads; existing metadata and already-cached artifact metadata only; temporary report under /tmp/task026af-crypto-transliteration/; documentation-only tracked changes.
- **Required:** exact source identities, method-level callsites, public API types, Runtime-vs-App classification, coordinates and unresolved items with report hashes.
- **Excluded:** Gradle/dependency resolution/build/compiler/test/CI/DEX/R8/APK/device/network/download/cache mutation; App compile retry under §24.119; production/source/test/Gradle/shrinker/schema/UI/Reader changes; BaseSource migration or Task 027.
- **Procedure note:** preliminary grep/source scoping occurred before this registration commit while selecting the next bounded task; this ordering deviation is disclosed in audit §24.165. Substantive evidence collection follows registration and required-list reread.
- **Disposition at registration:** Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 NO-GO; full compatibility NOT ESTABLISHED.

### 4.27.79 Task 026AF result — crypto and transliteration dependency caller closure (2026-11-26)

- **Status:** bounded read-only source/API/caller and cached artifact audit complete. Registration start HEAD f89bb62cb43b9988301aa2c5d5cf0bc37b603a82; registration commit/evidence HEAD f319edcf7c384d853f0a6060276547039001c4c3; pinned upstream 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- **Procedure note:** preliminary grep/source scoping occurred before preregistration while selecting this task; the ordering deviation is disclosed in audit §24.165. Both required lists were reread after registration before the substantive evidence pass.
- **Hutool:** BaseSource login AES, JsEncodeUtils public crypto API/return types, AnalyzeUrl PercentCodec/RFC3986/HexUtil and MD5Utils runtime callers prove `hutool-crypto:5.8.22` + `hutool-core:5.8.22` are mandatory candidate Runtime dependencies unless an exact ABI-preserving facade is separately proven. App-only StringExtensions.encodeURI/EpubFile and mixed NetworkUtils are not reasons to move their whole host files.
- **BouncyCastle:** no direct candidate-source imports, but pinned App declares bcprov/bcpkix 1.86 for source-book crypto and Hutool bytecode contains BC provider/ASN.1/EC/OpenSSL/PKCS references. Keep both as provisional Runtime dependencies pending algorithm-level proof; exact PKIX-only use remains unresolved.
- **Quick transfer:** source-visible JsExtensions/RegexJsExtensions, ContentProcessor and BookChapter call the Chinese transliteration wrapper and its project-specific exclusion dictionary. The pinned coordinate `com.github.liuyueyi.quick-chinese-transfer:quick-transfer-core:0.2.17` is a Runtime dependency. Both cached coordinate JARs contain identical 10 class files; only Maven/manifest metadata differs, so binary code is equivalent for these cached artifacts but coordinate identity remains different. Prefer the pinned coordinate.
- **Evidence:** `/tmp/task026af-crypto-transliteration/RESULT.md` SHA-256 `eed1e45230a1c727665cf342249c3ecd47608bf4573f82d3f315884bbf6c7897`; input and per-class manifests are adjacent and checksum-verified.
- **Limits:** no Gradle/dependency resolution/build/compiler/test/CI/DEX/R8/APK/device/network action; no production changes; no App compile retry. Exact Runtime variants, crypto algorithm parity and transliteration differential tests remain unverified.
- **Disposition:** Task 026AF complete for source/API/caller classification. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
- **Next:** separately register a documentation-only addendum reconciliation for the newly classified dependencies, then continue independent non-build source/host dependency boundaries. Runtime variant resolution/compile waits for a higher-capacity clean runner.

### 4.27.80 Task 026AG preregistration — Phase B dependency addendum reconciliation (2026-11-26)

- **Task ID:** `R3-S0-T026-PHASE-A-PHASE-B-DEPENDENCY-ADDENDUM-RECONCILIATION-026AG`; registered / not started. Start HEAD `8fc4b5f47a9fded78d126f2b6b24b6d8a317eecf`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Trigger:** Task 026AF §24.166 closed source/API/caller classification for Hutool, BouncyCastle and Quick Chinese Transfer, but the existing Phase B addendum §6.1 still has incomplete/ambiguous dependency dispositions. Task 026AC already records the user's isolated SharedJsScope TLS choice in §10.7; this task must preserve it without reinterpreting or implementing it.
- **Goal:** update only the existing Phase B implementation addendum's dependency requirements so it reflects 026AF evidence and clearly separates mandatory Runtime candidates, provisional dependencies, pinned coordinate identity, and App-only host boundaries. This is documentation reconciliation, not dependency resolution or implementation authorization.
- **Allowed tracked files:** `docs/audits/legado-runtime-3.0-phase-b-implementation-addendum.md`, `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md`, and `docs/required/03-LEGADO-RUNTIME-OVERVIEW.md`. No other tracked path may change.
- **Required reconciliation:** (1) list `hutool-crypto:5.8.22` and `hutool-core:5.8.22` as mandatory Runtime dependency candidates because of BaseSource/JsEncodeUtils/AnalyzeUrl/MD5Utils callers and public Hutool return types; (2) retain `bcprov-jdk18on:1.86` and `bcpkix-jdk18on:1.86` as provisional pending algorithm-level source-script/provider proof, explicitly noting no direct imports and unresolved PKIX-specific use; (3) add the exact pinned Quick Chinese Transfer coordinate `com.github.liuyueyi.quick-chinese-transfer:quick-transfer-core:0.2.17`, preserve `ChineseUtils` wrapper and custom Traditional-to-Simplified exclusion dictionary, and record cached class-byte equivalence—but not Maven coordinate identity—with Badcat's differently named coordinate; (4) keep `NetworkUtils` and `StringExtensions`/EpubFile mixed or product-side, and keep App preload/UI lifecycle callsites in App; (5) preserve the existing §10.7 isolated shared-jsLib trust-all/hostname-accepting/HTTPS-downgrade policy and strict ordinary-source HTTP boundary without broadening it.
- **Acceptance:** update addendum §6.1 with explicit status/rationale/owner notes; reconcile the obsolete quick-transfer uncertainty from §4.27.75/§24.162 without deleting history; link 026AF evidence/hash; explicitly state that these are source-backed candidate requirements, not resolved Runtime variants or artifact-file/compile proof; verify no contradiction with §10.7 or existing host-only boundaries; inspect diff and `git diff --check`; commit and reread changed sections plus both required lists at the result HEAD.
- **Hard exclusions:** no source/test/Gradle/version-catalog/dependency declaration/shrinker/manifest/schema/UI/Reader/workflow/CI changes; no Gradle invocation, dependency resolution, download, artifact staging, build/compiler/test/DEX/R8/APK/device/network action; no App compile retry under audit §24.119; no BookSource/BaseSource migration, Task 027 or Stage-1 authorization.
- **Dependencies:** Task 026AF result §24.166 / overview §4.27.79; existing addendum §§6.1–6.2 and §10.7; Task 026AD §24.162; pinned upstream SHA above.
- **Next:** after registration commit, reread both required lists from the new HEAD, perform only the documentation reconciliation, then record the result and reread it. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full compatibility **NOT ESTABLISHED**.
### 4.27.81 Task 026AG result — Phase B dependency addendum reconciled (2026-11-26)

- **Status:** completed as a documentation-only dependency disposition update. Registration start HEAD `8fc4b5f47a9fded78d126f2b6b24b6d8a317eecf`; registration commit `b93e53e938313ead4e05c1c85ed28996f7fa915d`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Changed addendum:** `docs/audits/legado-runtime-3.0-phase-b-implementation-addendum.md` §6.1.1 now records caller-backed dependency dispositions: Hutool Crypto/Core 5.8.22 as mandatory Runtime candidates; BouncyCastle provider/PKIX 1.86 as provisional; exact pinned Quick Chinese Transfer coordinate and required custom exclusion dictionary; cached JAR class-byte equivalence limits; and the App-only NetworkUtils, EpubFile/StringExtensions and preload/UI lifecycle boundaries.
- **Coordinate correction:** Task 026AD's earlier unresolved quick-transfer note is reconciled, not erased. The cached JARs' ten class files are byte-identical, but Maven coordinates, POM parents and embedded metadata differ. The addendum prefers the exact pinned upstream coordinate and does not claim reproducible variant equivalence.
- **TLS preservation:** existing addendum §10.7 remains unchanged. The selected trust-all certificate/hostname and HTTPS-to-HTTP redirect policy is confined to the dedicated URL-only SharedJsScope shared-library fetch client; ordinary source HTTP, login, TTS, image and other clients remain strict. This task neither reopens nor implements that decision.
- **Validation:** documentation diff reviewed; `git diff --check` passed. No source/test/Gradle/dependency declaration/version catalog/shrinker/manifest/schema/UI/Reader/workflow/CI change; no Gradle invocation, dependency resolution, artifact download/staging, build/compiler/test/DEX/R8/APK/device/network action; no App compile retry under §24.119.
- **Evidence:** Task 026AF report `/tmp/task026af-crypto-transliteration/RESULT.md` SHA-256 `eed1e45230a1c727665cf342249c3ecd47608bf4573f82d3f315884bbf6c7897`; input and per-class manifests remain checksum-verified. The addendum is a source-backed candidate plan only: exact Runtime variants/artifact resolution, crypto algorithm parity and transliteration differential tests remain NOT ESTABLISHED.
- **Disposition:** Task 026AG complete for documentation reconciliation. Phase A remains open; broad BookSource/BaseSource migration remains blocked; Task 027 blocked; Stage-0 `in_progress / not accepted`; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue a separate non-build Phase A dependency/host-boundary audit; obtain a clean higher-capacity runner before registering Runtime-only variant resolution/compile. No App compile retry.
### 4.27.82 Task 026AH preregistration — remaining Runtime dependency and Host Port caller closure

- **Task ID:** `R3-S0-T026-PHASE-A-REMAINING-DEPENDENCY-HOST-PORT-CALLER-CLOSURE-026AH`; registered / not started. Start HEAD `1e36f609ed819d6f200120e2336eee06ea2f0203`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Trigger:** Tasks 026AD/026AF and addendum §6.1.1 establish a candidate dependency list, but several parser/model/platform packages still lack a caller-exact Runtime-vs-Host disposition. The current runner is NO-GO for Gradle resolution/compile; source/API/owner closure remains possible without a build.
- **Goal:** close exact source imports, caller paths, public API type leakage and host ownership for Commons Text, Room common annotations, AndroidX Collection, Jsoup/JsoupXpath/JsonPath, and OkHttp/Okio. Distinguish true Runtime engine/model dependencies from typed App Host Port dependencies; do not infer a resolved Gradle graph.
- **Allowed tracked files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and `docs/required/03-LEGADO-RUNTIME-OVERVIEW.md` only. Temporary evidence under `/tmp/task026ah-dependency-host-port/`. Read-only source/build declarations only.
- **Required:** exact pinned source paths/imports/callers; method and public signature exposure; direct vs transitive artifact ownership; whether each package is needed by Runtime source compilation or should remain behind a typed host seam; Room annotations vs Room database/compiler/KSP ownership; exact OkHttp/Okio request/response/cancellation/body types vs App HTTP adapter semantics; exact parser package/API use; version/coordinate evidence and unresolved variant/transitive questions.
- **Scope discipline:** reuse Task 026K import matrix and Task 026AD existing metadata reports; do not rerun broad imports or dependency reports. Reconcile only the unresolved package families listed above. Do not repeat the completed Hutool/BC/quick-transfer caller audit.
- **Exclusions:** no Gradle invocation/dependency resolution/variant resolution; no downloads/cache mutation/artifact staging; no build/compiler/test/CI/DEX/R8/APK/device/network action; no App compile retry under §24.119; no production/source/test/Gradle/shrinker/schema/UI/Reader/workflow edits; no BookSource/BaseSource migration, Task 027 or Stage-1.
- **Acceptance:** a path-backed dependency/owner table with exact versions and public API exposure; explicit Runtime-candidate / Host-Port / conditional / unresolved dispositions; Room schema/compiler and Android UI/product boundaries preserved; report/input hashes; no claims of resolved variants or compatibility; result recorded in audit and overview and reread at the result HEAD.
- **Procedure:** commit this registration first, then reread both required lists from that new HEAD before collecting substantive source/caller evidence.
- **Disposition at registration:** Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
### 4.27.83 Task 026AH result — remaining dependency and Host Port callers classified (2026-11-26)

- **Status:** completed for bounded read-only source/API/caller and existing-metadata reconciliation. Registration start HEAD `1e36f609ed819d6f200120e2336eee06ea2f0203`; registration commit/execution HEAD `3bacfaa94437fd881c62db4c9ef1539ba26e55d6`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Runtime candidates:** Commons Text 1.13.1 is mandatory from AnalyzeRule and BookContent HTML unescape callers; AndroidX Collection 1.6.0 is directly imported by Runtime-owned SharedJsScope and JsSourceEngine LruCache implementations; Jsoup 1.23.2, JsoupXpath 2.5.3 and JsonPath 3.0.0 are parser-engine dependencies, with ReadContext and Jsoup Connection.Response in source/JVM-visible signatures.
- **Room boundary:** `androidx.room:room-common:2.8.4` is the conditional compile requirement when retaining authentic Room-annotated BookSource/Book/BookChapter/SearchBook/ReplaceRule model sources. Room runtime/ktx, AppDatabase/DAO, schema/migrations and KSP remain App-owned. Whether App KSP can process entities compiled in the Runtime module is still unverified.
- **HTTP/Okio boundary:** AnalyzeUrl exposes `okhttp3.Response` in public methods and uses OkHttpClient internally; therefore OkHttp 5.4.0 remains a Runtime API dependency candidate unless an exact Response-compatible facade is proven. Actual client construction, source cookies/headers, retries, redirects, timeout and cancellation remain typed App Host Port responsibilities. JsExtensions imports `okio.use`; Okio 3.17.0 is recorded as OkHttp-transitive in 026AD metadata and must be available to the Runtime source compile if that import remains.
- **Module gap:** current Badcat App declares Jsoup, JsoupXpath, JsonPath and OkHttp, but current `:legado-runtime` declares none of this task's dependencies. `:legado-rhino` has OkHttp and AndroidX Collection as `implementation`, not exported API; that does not satisfy Runtime-owned direct imports. Commons Text and Room common are absent from the inspected Badcat graphs.
- **Evidence:** `/tmp/task026ah-dependency-host-port/RESULT.md` SHA-256 `6d8ffb70c47253b3b51ad2ac782a4680273086df06f781598b945fa74557d8a5`; `callsite-lines.txt` SHA-256 `9c406ddcd369193ffa30efabb8f38a1de710135673ec9f4059710079d9d2580e`; `INPUT-HASHES.sha256` SHA-256 `fedbd8147121633f55cda38f957affce45a8b584bb2d1c23edcdb09b61299a06`; all entries in `CHECKSUMS.sha256` passed.
- **Limits:** no Gradle invocation/dependency or variant resolution, download/cache mutation, build/compiler/test/CI/DEX/R8/APK/device/network action and no source/test/Gradle/shrinker/schema/UI/Reader/workflow change. No App compile retry under §24.119. This closes caller classification only; exact Runtime variants/transitives, Room KSP cross-module behavior, parser differential tests and HTTP/response/cancellation parity remain NOT ESTABLISHED.
- **Disposition:** Task 026AH complete for the bounded dependency/Host Port caller slice. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 `in_progress / not accepted`; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue a separate non-build closure for the remaining unclassified candidate package families/host effects; Runtime variant resolution/compile still requires a clean runner meeting Task 026AE minimums. No App compile retry.
### 4.27.84 Task 026AI preregistration — coroutine core and Android dispatcher boundary closure

- **Task ID:** `R3-S0-T026-PHASE-A-COROUTINE-CORE-ANDROID-DISPATCHER-CLOSURE-026AI`; registered / not started. Start HEAD `d8410f1c207ced590c28a192c81bb2bc9767fe27`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Trigger:** Task 026AD identified 44 direct coroutine import rows and a missing direct `kotlinx-coroutines-core` dependency on `:legado-runtime`; App's Android coroutine dependency and Rhino's implementation dependency do not prove Runtime ownership. Task 026Z also established that fresh per-invocation bindings and cancellation/concurrency semantics must be preserved.
- **Goal:** classify exact pinned coroutine imports, callers, signatures, scopes, dispatchers, cancellation/timeout/locking/Flow behavior and dependency ownership. Determine the minimum Runtime core dependency versus App-only Android Main/UI dispatcher adapters; do not claim dependency or variant resolution.
- **Allowed tracked files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and `docs/required/03-LEGADO-RUNTIME-OVERVIEW.md` only. Temporary evidence under `/tmp/task026ai-coroutine-host-boundary/`. Reuse Task 026K/026AD matrices; inspect exact pinned source and existing Badcat declarations.
- **Required:** exact source paths/import rows and callers; `kotlinx-coroutines-core` API use and public signature leakage; use of `Dispatchers.IO/Default/Main`, Android Handler/Main dispatcher and Android-specific coroutine APIs; structured scope/job ownership, cancellation propagation, timeout, mutex/locks, Flow/continuation bridges and `runBlocking` boundaries; pinned version/declaration ownership; current Badcat Runtime/Rhino/App graph gap; typed host dispatcher/lifecycle recommendations.
- **Scope discipline:** no repeat of Task 026Z shared-scope ownership or Task 026AB source binder work; only map coroutine mechanics and their dependency/host boundaries needed by the authentic source closure.
- **Exclusions:** no Gradle invocation/dependency or variant resolution, download/cache mutation, build/compiler/test/CI/DEX/R8/APK/device/network; no App compile retry under §24.119; no production/source/test/Gradle/shrinker/schema/UI/Reader/workflow changes; no Task 027, BookSource/BaseSource migration or Stage-1.
- **Acceptance:** path-backed coroutine API/caller/dispatcher matrix; explicit Runtime-core / App-Android / conditional / unresolved dispositions; exact cancellation/threading constraints and unknowns; report/input hashes; result committed and reread at result HEAD.
- **Procedure:** commit this registration first, then reread both required lists from the new HEAD before substantive source/caller inspection.
- **Disposition at registration:** Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
### 4.27.85 Task 026AI result — coroutine core and Android dispatcher boundary classified (2026-11-26)

- **Status:** completed for bounded read-only coroutine import/caller and dispatcher ownership classification. Registration start HEAD `d8410f1c207ced590c28a192c81bb2bc9767fe27`; registration/execution HEAD `c23a507714f6667adcc304d3f938ed1021507c4c`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Runtime dependency:** `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0` is a mandatory direct Runtime compile/API candidate. The reused Task 026K candidate matrix contains 44 coroutine import rows across 17 candidate files, including BaseSource, AnalyzeRule/AnalyzeUrl, JsExtensions, SharedJsScope, JsSourceEngine/JsSourceBook, WebBook and content pipeline helpers.
- **Android dispatcher:** `kotlinx-coroutines-android:1.11.0` is an Android host runtime provider, not a replacement for Runtime's direct core dependency. Upstream WebBook and the required `Coroutine.kt` helper default callback execution to `Dispatchers.Main` and use `MainScope`; the final Android App must provide a working Main dispatcher or an exactly equivalent typed dispatcher port. The current Badcat App already declares the Android artifact, but packaging/discovery is not verified.
- **Host lifecycle boundary:** SourceLoginJsExtensions and RssJsExtensions use Activity `lifecycleScope`; RSS navigation switches to Main. Keep these lifecycle/UI owners in App and expose typed callbacks. Do not migrate RSS UI or Activity lifecycle scope into Runtime.
- **Compatibility constraints:** preserve Coroutine<T> callbacks, Main callback defaults, caller CoroutineScope, IO context, optional Semaphore acquire/release, timeout, Flow, Mutex, Job and cancellation propagation. Upstream cancellation handling is not uniform: BaseSource and JsSourceEngine rethrow cancellation; BookChapter returns from its replacement loop; ContentProcessor swallows it in its replacement loop. Do not normalize these behaviors without differential review.
- **Module gap:** `:legado-runtime` declares no coroutine dependency; `:legado-rhino` and `:integration-contract` have core as `implementation` but do not export a Runtime compile contract. App declares `kotlinx-coroutines-android:1.11.0`. No dependency resolution or variant proof was performed.
- **Evidence:** `/tmp/task026ai-coroutine-host-boundary/RESULT.md` SHA-256 `29c19f3589278ea22a94f22a2fa7cdc594bb5175eb452e111f4cd0cc067fd02a`; `coroutine-imports.tsv` SHA-256 `68c02a4a9d0ed9cf5a62098ed03553a93d606cffca82063bb3e696b2991bc16e`; `callsite-evidence.txt` SHA-256 `789ac9b7e3abc907a136968d3e812927ba7e189a603cf4cee76214ace348b5a5`; `INPUT-HASHES.sha256` SHA-256 `2bfc0cfa522f9c2589384512f563d256f459a69712859344cde8881d14e6c732`; all entries in `CHECKSUMS.sha256` passed.
- **Limits:** no Gradle/dependency/variant resolution, download/cache mutation, build/compiler/test/CI/DEX/R8/APK/device/network action and no production/source/test/Gradle/shrinker/schema/UI/Reader/workflow change. No App compile retry under §24.119. Runtime coroutine variants, Main dispatcher provider discovery and cancellation/callback parity remain NOT ESTABLISHED.
- **Disposition:** Task 026AI complete for coroutine core/Android dispatcher caller classification. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 `in_progress / not accepted`; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue a separate non-build dependency/Host Port closure for the remaining Android/platform, AppContext/resource and media/image/utility source owners; Runtime variant resolution/compile still requires a clean higher-capacity runner. No App compile retry.
### 4.27.86 Task 026AJ preregistration — Android Context/Resources/AppContext and Core URI caller closure

- **Task ID:** `R3-S0-T026-PHASE-A-ANDROID-CONTEXT-RESOURCE-APPCONTEXT-CLOSURE-026AJ`; registered / not started. Start HEAD `d0b0d481a3a3b84d5e6afec5037c9ca76012f193`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Trigger:** Tasks 026AA/026AB and 026AD identify static `appCtx`, AppConfig, Android resources, AndroidX URI helpers and Activity/WebView host boundaries, but the remaining candidate source callsites need exact owner classification. Task 026AI closed coroutine core/dispatcher ownership and must not be repeated.
- **Goal:** classify exact pinned source imports/callers for Android Context/Resources, Splitties `appCtx`, AppConfig values, generated App `R` resources, file/cache directories and AndroidX Core URI helpers. Separate Android SDK compile needs, direct AndroidX Core dependency, Runtime-owned config semantics and App-owned Context/Resource/UI services.
- **Allowed tracked files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and `docs/required/03-LEGADO-RUNTIME-OVERVIEW.md` only. Temporary evidence under `/tmp/task026aj-appcontext-resource/`. Reuse Task 026K candidate/import/Host Port matrices and Task 026AA/026AB findings.
- **Required:** exact source paths/imports/callers and property/resource IDs; distinguish Android SDK types from Maven dependencies; identify direct `androidx.core:core-ktx` ownership if source imports it; enumerate only source-runtime AppConfig settings and their defaults; locate static appCtx and Context/Resources/file operations; classify `R`/string/drawable/UI resources; recommend typed AppContext/Resource/Config ports with lifecycle/threading semantics; preserve URI parse/encode behavior.
- **Scope discipline:** do not repeat Task 026AA SharedJsScope fetch/cache/log adapter work, Task 026AB BaseSource binder lifecycle closure, Task 026AF NetworkUtils/StringExtensions classification or Task 026AI coroutine analysis. Reconcile only Context/Resource/AppConfig/URI accessors.
- **Exclusions:** no Gradle invocation/dependency or variant resolution, downloads/cache mutation, build/compiler/test/CI/DEX/R8/APK/device/network; no App compile retry under §24.119; no source/test/Gradle/shrinker/schema/UI/Reader/workflow edits; no broad migration, Task 027 or Stage-1.
- **Acceptance:** path-backed Context/resource/config/URI callsite matrix; direct Runtime dependency vs Android Host Port disposition; exact AppConfig defaults and generated-resource boundary; report/input hashes; result committed and reread at result HEAD.
- **Procedure:** commit registration first, then reread both required lists from that new HEAD before substantive source/caller inspection.
- **Disposition at registration:** Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 in_progress/not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
### 4.27.87 Task 026AJ result — AppContext, Resources and RuntimeConfig callers classified (2026-11-26)

- **Status:** completed for bounded read-only Context/resource/config/URI caller closure. Registration start HEAD `d0b0d481a3a3b84d5e6afec5037c9ca76012f193`; registration/execution HEAD `2c4c985f48d1bb79ec4d244b8ae118606cd42fae`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Exact imports:** reused Task 026K matrix shows 10 `splitties.init.appCtx` imports, 9 AppConfig imports, one `androidx.core.net.toUri` import and one direct Android `Context` import across the candidate slice. No direct `android.content.res.Resources` import; resources are reached through Context/AppConfig/ThemeConfig and generated App R.
- **RuntimeConfigPort settings:** preserve `userAgent` (blank preference falls back to Chrome/<Cronet_Main_Version> UA and updates on preference change), `threadCount` (default 32), `blockSourceNavigation` (false), `chineseConverterType` (0), `tocCountWords` (true), `replaceEnableDefault` (true), `adaptSpecialStyle` (true, listener-updated) and `themeMode` ("0", listener-updated). Do not move all of AppConfig or its audio/RSS/Reader settings into Runtime.
- **AppContext/Resource host boundary:** replace static appCtx with typed file/cache/asset/resource/feedback/navigation/WebView/theme services. Preserve SharedJsScope cache/assets per Task 026AA; JsExtensions externalCache safe-path checks, default WebView UA, toast/longToast, openUrl route and source metadata; localized `R.string` errors; and ThemeConfig JSON/map shape. App-generated `io.legado.app.R` cannot be imported from the independent Runtime module.
- **URI dependency:** `androidx.core.net.toUri` in JsExtensions requires `androidx.core:core-ktx:1.18.0` if retained verbatim; platform `Uri.parse` is an alternative only after encoded/opaque URI parity proof. Android Context/WebSettings/Uri are SDK API 36 platform types, not Maven dependencies.
- **App-only methods:** SearchBook.trimIntro(context) is display formatting; SourceLoginJsExtensions clear-cache message and Activity cache path are login UI; RssJsExtensions RSS record preference remains RSS UI. Keep these in App or an exact presentation adapter.
- **Evidence:** `/tmp/task026aj-appcontext-resource/RESULT.md` SHA-256 `d1026f88ce89191dc65660ff76a3a5c0c903f82b15891d84cf1405d33166929a`; `callsite-lines.txt` SHA-256 `05149fc9068fd43de33b4dc0f31077973b64ab75d5ab629a1e635349060a7afa`; `INPUT-HASHES.sha256` SHA-256 `aba3f18134059edcc0c22300fadacac2d56a29de7da9af32273ca4b7533f3be1`; all entries in `CHECKSUMS.sha256` passed.
- **Limits:** no Gradle/dependency/variant resolution, downloads/cache mutation, build/compiler/test/CI/DEX/R8/APK/device/network action; no source/test/Gradle/shrinker/schema/UI/Reader/workflow changes; no App compile retry under §24.119. Exact Runtime port contracts, resource localization, appCtx replacement lifecycle, path security and ThemeConfig parity remain NOT ESTABLISHED.
- **Disposition:** Task 026AJ complete for Context/Resource/AppConfig/Core URI caller classification. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 `in_progress / not accepted`; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue a separate non-build closure for remaining image/media/utility and AppCompat/lifecycle source owners without pulling Reader/RSS product UI into Runtime; Runtime variant resolution/compile still waits for a clean higher-capacity runner. No App compile retry.
### 4.27.88 Task 026AK preregistration — source image decode, Glide/Media3 and AppCompat lifecycle host boundary

- **Task ID:** `R3-S0-T026-PHASE-A-IMAGE-MEDIA-APPCOMPAT-LIFECYCLE-CLOSURE-026AK`; registered / not started. Start HEAD `b3da915da7fa81e950e0c3e796fd3acd437cb4d6`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Trigger:** Task 026AJ next-step recommendation and Task 026K matrices identify `AnalyzeUrl.getGlideUrl()` / `getMediaItem()`, source image decoding, Glide fetcher, Media3 playback, and AppCompat/lifecycle bridge owners. These have not been closed by the Context/Resource or coroutine tasks.
- **Goal:** classify exact source/runtime behavior versus Android product adapter ownership for image URL resolution/decryption, Glide/OkHttp image loading, Media3 playback helpers, and AppCompat/lifecycle-backed JS bridges. Preserve source image decode semantics while keeping Glide, Media3 playback and RSS/login UI out of the Runtime module.
- **Allowed tracked files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and `docs/required/03-LEGADO-RUNTIME-OVERVIEW.md` only. Temporary evidence under `/tmp/task026ak-image-media-lifecycle/`. Reuse Task 026K import/Host Port matrices, Task 026AB BaseSource binding closure and Task 026AJ Context/AppConfig findings.
- **Required:** exact callers/signatures for AnalyzeUrl GlideUrl/MediaItem methods; Glide custom loader/fetcher lifecycle, cancellation, source origin, cookies/headers, Wi-Fi/manga options and decode path; ImageUtils coverDecodeJs/contentRule.imageDecode semantics and all callers; distinguish source-runtime decryption from display/loading adapters; Media3 callers and playback-only scope; AppCompatActivity/lifecycleScope references in JsExtensions, SourceLoginJsExtensions and RssJsExtensions; superclass/API inheritance and callback lifetime; exact pinned dependency declarations and current Badcat module ownership; typed image request/decode/navigation/UI host seams.
- **Scope discipline:** do not repeat general AnalyzeUrl HTTP/OkHttp response semantics (Task 026AH), BaseSource runtime-entry binder closure (Task 026AB), Context/appCtx/Resource/AppConfig (Task 026AJ), or coroutine core/cancellation analysis (Task 026AI). Reuse their findings and only inspect the image/media/lifecycle-specific crossings.
- **Exclusions:** no Gradle invocation/dependency/variant resolution, download/cache mutation, build/compiler/test/CI/DEX/R8/APK/device/network action; no App compile retry under §24.119; no production/source/test/Gradle/shrinker/schema/UI/Reader/workflow changes; no moving Reader, RSS, login, TTS, audio/video playback or Glide UI into Runtime; no BookSource/BaseSource migration, Task 027 or Stage-1.
- **Acceptance:** path-backed source-vs-host table; all image decode and Glide/Media3 callers accounted for; exact method/public type leakage; AppCompat/lifecycle bridge ownership and callback semantics; dependency candidate vs host-only disposition; evidence/input hashes; result recorded and reread at result HEAD. No build/compatibility claim.
- **Procedure note:** initial task scoping used the existing matrices and a bounded preliminary grep/caller inspection after the required lists were read but before this registration. This ordering deviation will be retained explicitly; substantive closure and evidence collection continue only after registration and a fresh full reread of both required lists.
- **Disposition at registration:** Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 `in_progress / not accepted`; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
### 4.27.89 Task 026AK result — source image decode, Glide/Media3 and AppCompat lifecycle classified (2026-11-26)

- **Status:** completed for bounded read-only source-vs-host classification. Registration start HEAD `b3da915da7fa81e950e0c3e796fd3acd437cb4d6`; registration/execution HEAD `8bd48539fb52d55990a9b6a2535d62902b55a547`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Source image decode:** `ImageUtils` executes BookSource `coverDecodeJs` for covers and `ContentRule.imageDecode` for content/manga images via `BaseSource.evalJS`, binding `book`, `result` and `src`. Blank rule passes through bytes/stream; script failure logs and returns null. This is source-runtime behavior, not merely Glide UI. `RssSource.coverDecodeJs` is the RSS-specific host branch.
- **Glide boundary:** `AnalyzeUrl.getGlideUrl(): GlideUrl` has one caller (`OkHttpStreamFetcher`) and calls `setCookie()` first. That merges CookieStore cookies and updates the cookie-jar marker header; preserve this source/session side effect. GlideUrl/GlideHeaders, model loader/fetcher, Wi-Fi/manga options, OkHttp callbacks, cancellation and image UI are App adapters. Use a typed resolved-image-request seam; do not make Glide a Runtime dependency.
- **Media3 boundary:** `AnalyzeUrl.getMediaItem(): MediaItem` has only two direct callers, AudioPlayService and ReviewDetailDialog, both playback paths. It calls `setCookie()` and packages URL/headers for ExoPlayer. Keep Media3 1.10.1 and playback helper in App; do not leak MediaItem into Runtime.
- **AppCompat/lifecycle:** JsExtensions.showBrowser uses top AppCompatActivity, UI thread, Activity/FragmentManager lifecycle checks and BottomWebViewDialog. RssJsExtensions/SourceLoginJsExtensions/WebJsExtensions are App bridge classes; SourceLogin inherits the Rss superclass method surface. Their Activity `lifecycleScope`, IO/Main switching, weak references, callback order, clipboard/dialog and RSS/login navigation remain host-owned. `ReviewController.LegacyReviewJsExtensions` also subclasses RssJsExtensions with null Activity and a callback-only browser implementation, so the entire superclass cannot be dismissed as RSS UI.
- **Dependency versions:** pinned App uses Glide 5.0.7 + Glide OkHttp integration/KSP; Media3 1.10.1; AppCompat 1.7.1; Lifecycle 2.10.0. These are App host/playback dependencies, not Runtime dependencies. Inspected current Badcat App/Runtime/Rhino/contract Gradle declarations contain no direct declaration for these four families; no variant/transitive resolution was performed.
- **Evidence:** `/tmp/task026ak-image-media-lifecycle/RESULT.md` SHA-256 `e5e0bf8073001590f912b311e3dc6adaf451a474d87b71364e45091fa0e916d1`; `callsite-evidence.txt` SHA-256 `37aa7e6fecd2e9849c308aff0a5c1846b4692c366f1eede557282657417a3c8d`; `INPUT-HASHES.sha256` SHA-256 `6557f226df412ae84551aeeae0541504405bba4dae7545ce4144ab3942b369c6`; all entries in `CHECKSUMS.sha256` passed.
- **Procedure deviation:** the task registration disclosed preliminary matrix/grep/caller scoping before registration. The registered evidence collection began after commit `8bd48539...` and a full reread of both required lists. No source/build/test changes were made.
- **Limits:** no Gradle/dependency/variant resolution, downloads/cache mutation, build/compiler/test/CI/DEX/R8/APK/device/network action; no production/source/test/Gradle/shrinker/schema/UI/Reader/workflow changes; no App compile retry under §24.119. Runtime image decoder parity, resolved dependency variants, Glide cookie/header/cancellation parity, and lifecycle callback parity remain NOT ESTABLISHED.
- **Disposition:** Task 026AK complete for image/media/AppCompat/lifecycle caller classification. Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 `in_progress / not accepted`; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
- **Next:** continue only the remaining unclassified non-build host utility/platform edges or consolidate the dependency/Host Port closure into a source-backed matrix; wait for a clean higher-capacity runner for Runtime variant resolution/compile. No App compile retry.
### 4.27.90 Task 026AL preregistration — archive extraction and charset utility JS API closure

- **Task ID:** `R3-S0-T026-PHASE-A-ARCHIVE-CHARSET-JS-UTILITY-CLOSURE-026AL`; registered / not started. Start HEAD `0b12f499e013da0643733bff06999a1157f5a5b6`; branch `integration/legado-upstream-runtime`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- **Trigger:** Task 026K import matrix shows JsExtensions directly calls `ArchiveUtils`, `LibArchiveUtils`, `EncodingDetect` and `FileUtils`; JS-visible archive methods include ZIP/RAR/7z extraction and byte/string entry reads. Existing dependency evidence lists Android libarchive, but Runtime-vs-host ownership and exact behavior are not closed.
- **Goal:** classify source-visible archive utility semantics and exact dependency/Host Port boundary for ZIP/RAR/7z extraction and archive-entry reads, including charset detection, file path constraints, network fetch and error/empty-result behavior. Preserve JS API names/overloads and do not pull unrelated file-manager UI into Runtime.
- **Allowed tracked files:** `docs/audits/legado-runtime-3.0-stage0-architecture-audit.md` and `docs/required/03-LEGADO-RUNTIME-OVERVIEW.md` only. Temporary evidence under `/tmp/task026al-archive-charset/`. Reuse Task 026K matrices, Task 026AH HTTP/OkHttp caller closure and Task 026AJ file/cache/context boundary.
- **Required:** exact JsExtensions archive API signatures/overloads/callers; ArchiveUtils vs LibArchiveUtils format coverage and native/JNI dependency; archive entry matching/path sanitization/stream closure/size handling; EncodingDetect charset detection and defaults; AnalyzeUrl fetch and source cookies/headers/context; FileUtils path/root/relative path semantics only where archive methods depend on them; pinned dependency declarations and current Badcat module ownership; error/empty/null/exception and lifecycle semantics.
- **Scope discipline:** do not repeat general HTTP/OkHttp semantics (026AH), Context/cache-root path audit (026AJ), image/media adapter work (026AK), or unrelated FileUtils APIs. Reuse existing reports and inspect only archive/charset callers needed by source-visible JS methods.
- **Exclusions:** no Gradle invocation/dependency/variant resolution, downloads/cache mutation, build/compiler/test/CI/DEX/R8/APK/device/network action; no App compile retry under §24.119; no production/source/test/Gradle/shrinker/schema/UI/Reader/workflow changes; no broad file-system migration, no moving archive browser UI, Reader or unrelated import/export UI into Runtime; no BookSource/BaseSource migration, Task 027 or Stage-1.
- **Acceptance:** path-backed JS method and archive-format matrix; exact dependency/native/JNI disposition; path/charset/error/cancellation semantics; direct Runtime candidate vs typed ArchivePort decision; report/input hashes; result committed and reread. No resolved graph or runtime parity claim.
- **Procedure note:** preliminary task scoping used only the existing Task 026K import matrix and a grep of the JS archive method names before registration. This ordering deviation is disclosed; substantive source and caller inspection begins after registration and a fresh full reread of both required lists.
- **Disposition at registration:** Phase A remains open; broad BookSource/BaseSource migration blocked; Task 027 blocked; Stage-0 `in_progress / not accepted`; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.