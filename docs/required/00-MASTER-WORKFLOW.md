# Badcat B′ 总流程必读清单（唯一总入口）

> 每次任务先读本文件，再读本次任务所属的一份分类清单。单类任务不需要重读其他四类；跨类任务只追加读取实际受影响的分类。历史审计文档是证据资料，不是例行必读清单。
> 分支：`integration/legado-upstream-runtime`（禁止写入 main）。Legado 固定行为基准：`330e606c4d57e08adc056ff8ab7a4132c784da9a`。

## 1. 总目标

交付独立运行的 Badcat 增强客户端：保留坏坏猫原版 UI、页面、导航、交互和五大类功能；在产品层后面实现固定 SHA 的完整 Legado 兼容运行时；不依赖另装 Legado App；不以 selector-only、空壳 API、no-op、假数据、只编译通过冒充完成。未经完整验收不称完整版本、不交付最终发布 APK。

## 2. 正式架构候选：方案 B′（必须先 PoC）

**Badcat 自有持久化 + Badcat 原有 UI + Legado 兼容运行时 + 严格隔离适配层 + 可退出的最小 PoC。** B′ 是当前首选验证路线，不是已经证明的最优结论。

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
