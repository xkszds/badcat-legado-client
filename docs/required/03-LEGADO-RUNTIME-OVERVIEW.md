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
