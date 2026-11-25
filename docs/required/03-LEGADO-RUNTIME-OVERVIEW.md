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
