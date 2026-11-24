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
- **Status:** registered; not started.
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
- **Status:** registered; not started.
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
