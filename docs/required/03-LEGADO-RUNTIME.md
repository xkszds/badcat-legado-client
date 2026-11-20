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
