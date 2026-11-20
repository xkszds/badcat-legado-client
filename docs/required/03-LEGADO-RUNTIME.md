# 第三类必读清单：Legado 核心模型、运行时与 B′ PoC

> **Legado 工作唯一必读分类清单。** 每次 Legado 任务只读总流程 [00-MASTER-WORKFLOW.md](00-MASTER-WORKFLOW.md) + 本文件。详细审计文档仅在追查具体源码时打开。
> 上游固定 SHA：`330e606c4d57e08adc056ff8ab7a4132c784da9a`；GPL-3.0；分支：`integration/legado-upstream-runtime`，不改 main。

## 1. 目标和范围

在 Badcat 自有持久化和原版 UI 后建立完整独立 Legado 兼容运行时。覆盖官方模型/序列化、AnalyzeRule/AnalyzeUrl、Rhino/JsExtensions、HTTP/Cookie/登录、source state、统一 search/detail/TOC/content 生命周期、batch context 和 durable cache。禁止 selector-only、no-op、null/unsupported 占位、同名简化 API、测试专用实现或仅凭编译宣称完成。

不负责重做 Badcat UI/Reader UI、AdBlock/AdMark、PPRule；跨类改动才追加读取相应分类。

## 2. B′ 约束与 PoC

候选架构：Badcat 自有持久化 + Badcat UI + Legado 兼容运行时 + 严格隔离适配层。Runtime 不得直接引用 Badcat DAO；UI 不得直接依赖官方 DAO。JVM integration contract 保持 Android-free，Android/Room/Parcelable 留在 Android 边界。

五项 PoC：BookSource、Book、BookChapter、阅读进度、章节缓存。逐字段映射、null/default/unknown preservation、identity/dedup、单写多读、事务、缓存失效、重启恢复、迁移 dry-run/幂等/回滚、架构测试、录制回放、真实源全链路和 Android 实机全部通过才可扩大。

任一出现适配层泄漏、双模型状态分裂、迁移不能回滚、缓存/身份语义不一致、真实源引擎内失败或真机数据无法恢复，暂停 B′，评估限定数据层 A（官方核心模型/DAO/必要存储，不迁移 Reader UI、不引入无关 RSS/听书实体）；再不适合则官方核心表 + 兼容门面、旧表只读保留。PoC 未通过前不得称 B′ 已实现。

## 3. 固定上游执行链和依赖

Search/Explore → BookInfo → BookChapterList → BookContent；依赖 AnalyzeRule、AnalyzeUrl、JsExtensions/Rhino、BaseSource、Room/持久化、CookieStore、CacheManager、SharedJsScope、BookHelp、ContentProcessor、BatchContentContext 和应用初始化。只搬 AnalyzeRule/Jsoup/Rhino 不等于完整运行时。

上游 AppDatabase 是 Room v113、26 entities、一个 view 和长 migration chain。当前 Badcat 没有 Room/DAO；书源 JSON 由 FileBookSourceCatalog + AtomicFile 保存。不能假装现有存储等价于官方数据库，不能单独复制 BookSource 而伪造 BaseSource。

## 4. 既有程序和证据（按范围，不夸大）

### 模型/序列化
- Android-free integration-contract 的 BookSource/BookCandidate/BookDetail/BookChapter 是投影，不是官方实体。
- LegadoBookSourceCodec/FileBookSourceCatalog 有选定字段映射和 raw JSON 保留；未形成完整官方 mutable BookSource/BaseSource。
- 七个 rule entity（ExploreRule/SearchRule/BookListRule/BookInfoRule/TocRule/ContentRule/ReviewRule）曾与 pin 逐字节核对；来源一致不等于运行时接线完成。
- BookSource.customOrder 曾加入 host projection/codec（commit 5466caaf7cbbf2035b393b8dde70ef286d93a56e），只是窄字段修正。
- BookChapter host projection 只有 index/title/url；官方字段远多于此。官方 Room PK=(url,bookUrl)，unique=(bookUrl,index)，equals/hashCode 按 URL 比较。

### Rhino / JsExtensions / JS
- 有 Rhino 执行/沙箱、source scoped bridge、部分 Java/JS helper、JS 上下文传播、加密工具和资源边界测试。
- pinned JsExtensions 审计为 118 public declarations；host 缺 17 个名称/30 个 overload。
- java.getSource() ABI 冲突：upstream 返回 BaseSource，host 旧路径返回 JSON Map；需兼容策略，不能破坏既有 caller。
- JsExtensions 涉及 WebView/activity/dialog/UI event、verification/browser、AnalyzeUrl、HTTP response/Jsoup、archive/file/encoding、font/TTF、reader config、video、batch context；不能用空 stub 填齐。
- BaseSource 是实际生命周期服务：登录、loginInfo 加密、源变量、Cookie、CacheManager、SharedJsScope、限流、jsLib 等，不是 marker interface。

### HTTP / Cookie / 登录 / 缓存
- 有 GET/HEAD/POST timeout overload、redirect 区分、redirect-aware GET 下载/archive/ajaxAll/selector 等历史增量，仍需逐项对照 pin 做契约验证。
- 有源登录/会话同站 header 边界、Cookie 同步/清除、加密 loginInfo、明文迁移、源隔离和 Robolectric 测试记录。
- 有 AtomicFile chapter/source cache、LRU/pruning、写入后清理容错、跨 store 锁、TOC 进度恢复等局部实现；不等于官方 CacheManager/BookHelp/ContentProcessor/BatchContentContext 和章节 save token。
- 中文转换、限流、source lock、URL script、正文替换等有局部实现；每个 API 必须按 caller/测试单独确认。

### 历史测试与真实书源（只对对应 commit）
- CI #44605770188：commit cc78171... 的构建/测试与 APK packaging 修复记录，不是当前 head。
- CI #44630795195：commit 2b2bf91... 的 login/session 阶段通过记录，不外推当前 head。
- JUnit artifact 14582903590：276 tests、0 failure/error/skip，属于历史模型窄切片。
- 历史 E2E：Bixiange GBK POST 搜索→详情→114 页 TOC→正文；Pinshuzhai 搜索→详情→1,650 章 TOC→正文。
- 真实源 403/404/429/5xx、TLS、超时、redirect loop 要与引擎缺陷区分。

## 5. 已知关键缺口

- 官方 BookSource/BaseSource/Book/BookChapter 完整模型和持久化未集成；规则对象 object/string 双形态、null/missing/default/unknown round-trip 需契约测试。
- BaseSource/JsExtensions 全部真实服务闭包缺失；17 names/30 overload gap 未关闭；java.getSource ABI 冲突未解决。
- 完整 AnalyzeRule/AnalyzeUrl selector/context/replacement/JS/request/response 语义仍未逐项验收。
- 搜索/发现→详情→目录→正文还需统一 source/session/script/http/cookie/state 生命周期。
- BatchContentContext、章节去重、save token、持久化 cache、invalidate/resume 未证明完整。
- 迁移、架构边界测试、manifest、自动上游 diff、录制回放框架和真机 PoC 未完成。

## 6. 强制自动维护

- 契约测试：字段/default/null/missing/round-trip/unknown fields/rule JSON 双形态/identity/cache token/Cookie/JS lifecycle。
- 架构测试：禁止 Runtime→Badcat DAO、UI→官方 DAO，CI fail。
- 上游自动差异：API/overload/model/default/Room migration/JsExtensions，人工决定，不自动升级。
- 录制回放为主、少量真实书源冒烟为辅。
- 迁移自动测试：备份/dry-run/idempotency/rollback/upgrade/restart。
- 安全默认限制：HTTP timeout/cancel、JS 时间/资源/隔离、缓存容量/TTL/文件上限。
- JSON/YAML manifest 注入源码 commit、Legado SHA、adapter version、DB schema、测试报告 hash 到 CI/APK。
- 每月自动差异报告；仅安全、关键兼容、明确功能/性能/稳定收益才升级，稳定优先。

## 7. 实施顺序（不可跳级）

1. B′ 五项 PoC 的字段/身份映射和事实源/事务设计。
2. 适配接口和禁止依赖架构测试。
3. 迁移工具及备份/dry-run/幂等/回滚/重启演练。
4. 五项 PoC 契约测试和脱敏录制回放。
5. 少量真实书源全链路和 Android 实机。
6. PoC 通过后，扩展官方模型和完整服务闭包。
7. 完整 AnalyzeRule/AnalyzeUrl。
8. Rhino/JsExtensions/BaseSource 缺口和 ABI。
9. 统一 search/detail/TOC/content。
10. BatchContentContext 和 durable cache/token。
11. 与第一/二类适配，真机验收和发布。

## 8. 每次任务登记/结果

改 runtime 前先登记日期、起始 HEAD、pin、精确 upstream 声明/call sites、host 全部 caller、目标文件、适配边界、事实源/事务、风险、契约/回归测试、验收/退出条件。完成后记录实际变更、所有 callers、focused/full tests、JUnit 数量、CI run+exact HEAD、APK/报告哈希、真实源/设备结果、遗留差距和清单自审。

## 9. 证据文档（按需查阅，不是每次必读）

- full-legado-runtime-migration.md：完整迁移流水/执行链。
- USER_REQUIREMENTS_AND_PARITY_CHECKLIST.md：原始 Legado 详细需求。
- upstream-js-extensions-compatibility-audit.md：118 declarations 逐项审计。
- upstream-model-serialization-audit.md：模型/数据库/服务依赖审计。
- project-completion-checklist.md：历史跨类任务、CI 和错误修复记录。
- BADCAT_REPLICATION_FIVE_CATEGORIES.md：产品五类历史定义。

**当前状态：部分实现；B′ PoC 未验收；完整 Legado 未完成。** 本次清单建立时 branch HEAD 为 11549e7824683553c4039befa2f714fabfb805c3，未确认该 HEAD 有新的 CI run；历史 CI 不得当作当前 head 证据。


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
