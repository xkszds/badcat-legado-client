# Pinned upstream model and serialization audit


> **必读入口已调整（2026-11-20）**：本文件保留为详细证据/审计资料，不再要求每次开发重读。新的流程入口是 [总流程清单](required/00-MASTER-WORKFLOW.md)，对应分类唯一必读清单是 [第三类 Legado](required/03-LEGADO-RUNTIME.md)。分类清单已汇总任务范围、当前状态、缺口和验收；只有需要核对本文件中的具体字段/API/证据时才打开。


Audit date: 2026-11-19  
Host head reviewed: `8c17d59a48c5d4380bba014a87a492522feabe44`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9`

This audit is the model/serialization gate that must precede wiring `AnalyzeRule` or the operation pipeline.

## 1. Source model graph

### BookSource

Pinned file: `app/src/main/java/io/legado/app/data/entities/BookSource.kt`.

The upstream entity is a mutable, Parcelable, Room entity and implements `BaseSource`. Its persisted source fields include:
- Identity/display: `bookSourceUrl`, `bookSourceName`, `bookSourceGroup`, `bookSourceType`, `bookUrlPattern`, `customOrder`, `enabled`, `enabledExplore`.
- Runtime/network: `jsLib`, nullable `enabledCookieJar`, `concurrentRate`, `header`, `loginUrl`, `loginUi`, `loginCheckJs`, `coverDecodeJs`.
- Metadata: `bookSourceComment`, `variableComment`, `lastUpdateTime`, `respondTime`, `weight`.
- Explore/search: `exploreUrl`, `exploreScreen`, `ruleExplore: ExploreRule?`, `searchUrl`, `ruleSearch: SearchRule?`.
- Detail/reading: `ruleBookInfo: BookInfoRule?`, `ruleToc: TocRule?`, `ruleContent: ContentRule?`, `ruleReview: ReviewRule?`.
- Script/host events: `mainJs`, `eventListener`, `customButton`.

It also provides source identity, equality/hash behavior, lazy default rule accessors, source helper methods and Room converters for each nested rule object. The converters use upstream Gson behavior; they are not equivalent to storing arbitrary JSON strings and parsing only selected keys at the call site.

### Rule entity graph

Pinned package: `io.legado.app.data.entities.rule`.

| Type | Fields / responsibility |
|---|---|
| `BookListRule` | Common list/name/author/intro/kind/lastChapter/updateTime/bookUrl/coverUrl/wordCount contract |
| `SearchRule` | `checkKeyWord` plus the common book-list fields |
| `ExploreRule` | Common book-list fields |
| `BookInfoRule` | init/name/author/intro/kind/lastChapter/updateTime/coverUrl/tocUrl/wordCount/canReName/downloadUrls |
| `TocRule` | preUpdateJs/chapterList/chapterName/chapterUrl/formatJs/isVolume/isVip/isPay/updateTime/nextTocUrl |
| `ContentRule` | content/subContent/title/nextContentUrl/webJs/sourceRegex/replaceRegex/imageStyle/imageDecode/payAction/callBackJs/contentBatch/maxBatchSize |
| `ReviewRule` | review/summary/detail/reply URL and field rules, pagination and enabled state |
| `ExploreKind` | URL/text/button/toggle/select category definition, action, chars/default/viewName/style |
| `FlexChildStyle` | Explore category layout/style data |
| `RowUi` | Custom source-defined row controls and UI metadata |

The first six rule types are nested directly in `BookSource`. `ExploreKind`, `FlexChildStyle` and `RowUi` are part of the source-driven explore/custom UI graph and must not be dropped when those paths are enabled.

### BaseSource / BaseBook and book entities

- `BaseSource` is not a marker interface: it extends `JsExtensions` and supplies source key/name, source script execution, persistent variables, login state, shared scope, cookie/cache behavior and source lifecycle.
- `BaseBook` extends `RuleDataInterface`; it includes mutable metadata and variable storage plus big-variable persistence.
- `Book` implements `BaseBook` and contains book identity, TOC URL, origin, source/user metadata, current chapter/volume/progress, update/sync state, variable JSON and reader config. It also calls application DB, content processor and reader configuration helpers.
- `BookChapter` implements `RuleDataInterface` and contains URL/title/volume/base URL/book identity/index/VIP/pay/resource/tag/word count/position/fragments/variables/image metadata. Its methods update Room, resolve URLs and store normal/big variables.
- `SearchBook` is a distinct result model with conversion to `Book`; `RssArticle` is another rule-data type accepted by `AnalyzeRule`.

## 2. Existing host representation and known mismatches

Current host model: `integration-contract/src/main/kotlin/app/badcat/engine/ReadingEngine.kt` (`BookSource`, `BookCandidate`, `BookDetail`, `BookChapter`, `ChapterContent`).

Current import representation: `LegadoBookSourceCodec.kt` stores recognized scalar fields, nested rule JSON strings and the full `rawJson`.

This is a safe staging/import-export boundary, but it is not the upstream runtime model:
- Host `BookSource` does not expose the typed mutable upstream rule properties or their default accessors.
- It has a host-only `baseUrl` abstraction instead of upstream source identity plus `BaseSource.getKey()`.
- It does not expose the upstream `customOrder` field as a runtime property.
- It does not implement `BaseSource`, `JsExtensions`, source-scoped shared state or lifecycle.
- Host `BookChapter` has only index/title/url, whereas upstream chapters are rule data with book/base URL, volume/VIP/pay and variable state.
- Host `BookDetail` and `BookCandidate` are UI-oriented projections, not upstream `Book` / `SearchBook` models.
- Keeping `rawJson` preserves unknown fields for export but does not make those fields available to source scripts or upstream methods.

## 3. Serialization behavior that must be preserved

- `BookSource.Converters` has explicit bidirectional converters for ExploreRule, SearchRule, BookInfoRule, TocRule, ContentRule and ReviewRule.
- Each rule type defines a custom Gson deserializer that accepts either a JSON object or a JSON primitive containing serialized JSON; other JSON types map to null.
- Default rule accessors lazily create mutable rule objects when a stored nested rule is null.
- Upstream `BookSource` has `MAX_CONTENT_BATCH_SIZE = 50`; batch size limits are model behavior, not an arbitrary host parser limit.
- The host import codec has its own bounded-input and validation policy. Preserve these safety limits, but do not let them alter the semantic contents of valid upstream source JSON.
- Source JSON import/export round-trip and runtime-model conversion are separate acceptance tests. Passing one does not imply the other.

## 4. Integration constraints and pre-change risk register

1. The current `:integration-contract` module is Kotlin/JVM; the upstream entities use Android Parcelable, Room annotations/converters, app DB, app context and host services. Copying the files unchanged into that module will not compile.
2. The app module can host Android-bound models, but the existing source engine interfaces live in the JVM module. Avoid a circular dependency by defining an explicit app-side upstream runtime adapter boundary; do not make the JVM contract depend on the Android app.
3. Do not create fake `BaseSource`, `BaseBook`, `appCtx`, `appDb`, CookieStore or CacheManager objects just to satisfy constructors.
4. Preserve Java/Kotlin ABI details where scripts or reflection use them: mutable property names, getters/setters, nullable fields, default values, overloads, equals/hashCode and Parcelable/Room behavior.
5. Search the host for every field and rule JSON consumer before replacing the staging model. Search import/export, source catalog, request factory, all stage parsers, Android UI, persistence and tests.
6. Keep unknown JSON fields round-trippable while typed models are introduced; do not rewrite source JSON from a partial typed projection.
7. BookChapter identity for batch persistence is `index` within a book; URL is not unique. Do not use host URL-only identity for upstream cache writes.
8. Add upstream fixture parity for object-form and string-encoded rule JSON, null/empty rule values, missing fields/defaults, unknown fields, round-trip, custom ordering and source/book/chapter variables.

## 5. Recommended next implementation

Create the Android-side model/runtime module boundary first, with the exact pinned upstream rule entities and serialization behavior. Then adapt the host's source catalog to expose both:
- the lossless raw JSON record for import/export and unknown-field retention; and
- an upstream-compatible runtime source model created through the explicit adapter.

Do not change the public JVM `ReadingEngine` contract or Badcat UI until the model adapter has fixture parity. After the model layer is real, implement the upstream `BaseSource` service methods against injectable host services, then wire `AnalyzeUrl` and `AnalyzeRule`.

## 6. Status

No runtime model code was changed in this audit. This is intentional: the dependency boundary is now explicit, and the next code change must not invent a JVM look-alike that pretends to be the Android upstream entity. The analyzer audit is in [upstream-analyzer-integration-audit.md](upstream-analyzer-integration-audit.md).


## 2026-11-19 status labels

- [x] **Source-level audit complete:** exact pinned source model/rule declarations, field defaults, Gson deserializer/converter families, Android/Room dependencies, host model mismatches and compatibility risks are documented.
- [ ] **Runtime model integration not started:** this document is not evidence that official models are compiled or used by Badcat.
- [ ] **Serialization parity not complete:** host codec retains raw JSON and extracts selected fields; official `INITIAL_GSON/GSON` and Room converter behavior are not yet wired.
- [ ] **Fixture parity not complete:** still required for object-form, legacy string-encoded, null, missing/default fields, unknown nested fields, all six rule models, and round-trip semantics.
- **Next task:** inspect the exact upstream `INITIAL_GSON/GSON` implementation and Android module compatibility, then implement the smallest real upstream-compatible rule-model/serialization slice. Re-audit all codec/catalog callers before changing their contracts.


## 2026-11-19 implementation status update

- [x] Initial exact source-rule model slice is now vendored in the Android app: `BookListRule`, `SearchRule`, `ExploreRule`, `BookInfoRule`, `TocRule`, `ContentRule`, `ReviewRule`, `ExploreKind`, `FlexChildStyle`, and `RowUi`.
- [x] Exact pinned `GsonExtensions.kt` supplies `INITIAL_GSON/GSON` and the six upstream rule deserializers.
- [x] Source attribution and GPL-3.0 notice updated; Parcelize and Flexbox build requirements added.
- [ ] This does not make the source model complete: upstream `BookSource` and `BaseSource` remain absent, as do Room converters/database, `BaseBook`, `Book`, `BookChapter`, and runtime service dependencies.
- [ ] New tests are authored but await CI execution on the code commit.
- **Next:** inspect CI and fix any compile/test regressions; then map `BaseSource` and `JsExtensions` host service contracts before porting `BookSource`.


## 2026-11-19 post-CI BookSource/BaseSource dependency re-audit

- [x] The pinned upstream declarations were re-opened after the rule-model increment: `BookSource.kt`, `BaseSource.kt`, `JsExtensions.kt`, and the `BookList.kt` / `BookInfo.kt` operation call sites.
- [x] Confirmed that `BookSource` cannot be integrated independently: it implements `BaseSource`, uses Room and Parcelable, owns six typed rule properties and converters, and calls source helper methods used by the operation pipeline.
- [x] Confirmed that `BaseSource` is a runtime contract, not a marker: it extends `JsExtensions`, manages source key/tag, login/header/cookie state, script evaluation/shared state and cache behavior.
- [ ] No fake BaseSource, null host services or no-op JsExtensions APIs will be introduced to force compilation.
- [ ] Compatibility risks before the next code change: method-name/overload collisions with `RhinoSafeSourceJavaExtensions`; host DTO vs upstream mutable entity semantics; source-scoped shared JS scope and persistent variables; login-header/CookieStore ordering; Room converter parity; Android UI callback lifecycle; cancellation/threading; and source identity by `bookSourceUrl`.
- **Next implementation gate:** finish the per-method JsExtensions inventory and host capability mapping, then integrate the real upstream BaseSource/BookSource dependency slice and re-audit every existing bridge/caller/test. Do not wire AnalyzeRule/AnalyzeUrl before these dependencies are real.


## 2026-11-20 mandatory reread and current Stage 1 implementation status

- Re-read `AGENTS.md` and all four mandatory project checklists on the active `integration/legado-upstream-runtime` branch before continuing. Also re-read `docs/BADCAT_REPLICATION_FIVE_CATEGORIES.md` and `docs/upstream-model-serialization-audit.md` because the former defines product parity separately and the latter governs the Android/JVM model boundary.
- Current branch head at this review: `5466caaf7cbbf2035b393b8dde70ef286d93a56e`. Pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`. `main` was not modified.
- This record supersedes the stale “NO RUNTIME CODE EDITS YET” baseline above: code commit `5466caaf7cbbf2035b393b8dde70ef286d93a56e` adds the upstream `BookSource.customOrder` field to the platform-neutral source projection, codec import/export mapping and regression fixtures. It is a narrow staging-model parity correction, not the official mutable Android `BookSource`/`BaseSource` runtime integration.
- The seven vendored source-rule entity files (`ExploreRule`, `SearchRule`, `BookListRule`, `BookInfoRule`, `TocRule`, `ContentRule`, `ReviewRule`) were compared by Git blob SHA with the exact pinned upstream files; all seven are byte-identical. This confirms source provenance only, not complete model/runtime behavior.
- Exact-code-head Actions run [#44633804009](https://github.com/xkszds/badcat-legado-client/actions/runs/44633804009), head `5466caaf7cbbf2035b393b8dde70ef286d93a56e`, is still `in_progress` at the final best-effort four-source uploaded-corpus sample. The contract tests, pinned Rhino verification, bounded Rhino host tests, app unit tests, Debug APK assembly/upload, Nyasama smoke, repository probe, Feisuzhong E2E and Pinshuzhai E2E steps are completed successfully; the last uploaded-corpus sample/report step is not yet complete.
- Downloaded unit-test XML artifact `14582903590`: 276 tests, 0 failures, 0 errors, 0 skipped.
- Downloaded Debug APK artifact `14582783770`: extracted APK size 18,163,811 bytes, SHA-256 `05b26ece98fda97321e94951e2303fbfa35cb85e3e6ddb185f50626d3650ddf9`. APK ZIP inspection confirms `assets/PublicSuffixDatabase.list` and `META-INF/LICENSE.md` are present. This is a Debug artifact, not a signed release and not physical-device validation.
- Live report artifacts `14583189639` and `14583199685` independently confirm Bixiange GBK POST search -> detail -> 114-page TOC -> chapter content, and Pinshuzhai search -> detail -> 1,650 chapters -> chapter content. The four-source uploaded-corpus sample remains pending; do not imply it passed.
- Next code work remains within Stage 1 and must follow the model audit: no isolated/fake `BookSource` or `BaseSource`; inspect the exact pinned service/model graph and all host callers, keep Android Room/Parcelable concerns out of the JVM contract, retain raw JSON for unknown-field losslessness, and pre-register a narrower adapter slice before runtime edits.
- Product parity remains separate: Badcat categories 1, 2, 4 and 5 are not completed by Legado category-3 progress. No full-client or final-APK claim is authorized.
- Self-review: the earlier Stage 1 preregistration's branch baseline and “no runtime edits yet” label became stale after commit `5466caa`; this record explicitly supersedes those statements. Checklist scope remains valid, but the next implementation must respect the more specific Android model/service boundary in `upstream-model-serialization-audit.md`.

## 2026-11-20 exact pinned model gap audit — Stage 1 continuation

Audit baseline: Badcat branch head at start of audit `eabbef43481e6c14b8bda6ea62f03c5213d1188d`; pinned upstream `330e606c4d57e08adc056ff8ab7a4132c784da9a`. The four required checklists and this audit were re-read before work. No runtime code was changed in this audit slice.

### Verified upstream-to-host mapping

- `BookSource` is a mutable Room/Parcelable entity implementing `BaseSource`. Its persisted fields cover identity/display, source type/order/enabled flags, login/network state, metadata/weight, explore/search, six typed rules, mainJs and event/custom-button flags. The current host descriptor maps most of these source-level scalar fields, including `customOrder`, and preserves the full raw JSON for round-trip. The runtime contract still exposes rule JSON strings rather than typed mutable upstream rules and has host-only `baseUrl`; therefore it is not an upstream BookSource.
- `BookSource.getKey()` and source equality/hash are based on `bookSourceUrl`; current codec maps that value to both host `id` and `baseUrl`. Keep this exact identity bridge for login/session scoping.
- The upstream six rule converters use the pinned Gson helper and accept both JSON object and string-encoded JSON forms. Current codec accepts those two forms and retains the original raw JSON, but runtime execution does not receive upstream typed mutable rule instances. The seven vendored rule entity files being byte-identical to upstream establishes provenance only, not the model lifecycle.
- Upstream `Book` is a Room/Parcelable `BaseBook` entity with primary identity `bookUrl`. Its persisted constructor includes `tocUrl`, `origin`, `originName`, name/author/kind/wordCount, custom tag/cover/intro, local charset/type/group, latest/update timestamps, chapter count and reading position (chapter/volume/index/position/time), update/order/originOrder, variable JSON, `ReadConfig`, sync time and persisted cover URL. It has transient/ignored runtime fields (`variableMap`, info/toc HTML, download URLs, folder cache) and methods coupled to app DB, reader configuration, content processor, network URL parsing and local-file helpers. Host `BookCandidate` and `BookDetail` are search/UI projections; no host persisted Book model is present.
- Upstream `BookChapter` is a Room/Parcelable `RuleDataInterface` entity. Constructor fields: url, title, isVolume, baseUrl, bookUrl, index, isVip, isPay, resourceUrl, tag, wordCount, start/end offsets, start/end fragment IDs, variable JSON and imgUrl. It has lazy variableMap, lyric/danmaku/image mutation, big-variable persistence and Room update behavior. Room primary key is the pair `(url, bookUrl)`; a separate unique index enforces `(bookUrl, index)`. Its Kotlin equals/hashCode use URL only. The host `BookChapter(index,title,url)` drops all other fields and is not an upstream chapter. Any adapter/cache design must distinguish persistence identity, equality semantics and stable chapter ordering rather than treating URL as globally unique.
- `BaseSource` is an interface extending the full `JsExtensions` API. Its default/runtime methods bind source script execution to Rhino `ScriptBindings`, `CookieStore`, `CacheManager`, Android ID/AES login storage, source-keyed variables, concurrent-rate limiter, shared JS scope, jsLib lifecycle and Explore cache invalidation. It also includes Login UI v2 evaluation with optional Book/BookChapter context. It is not safe to instantiate BookSource without this real service lifecycle.

### Integration boundary conclusion

The existing platform-neutral `:integration-contract` is intentionally Android-free. Directly copying upstream Book/BookChapter/BaseSource there would import Room, Parcelable, appDb/appCtx, reader/config, cache, cookie and Rhino dependencies or create a circular dependency. A fake BaseSource or DTO that merely resembles the upstream entity would violate the project contract. The next code task must be an Android-side integration seam backed by real host services, while retaining the JVM contract and lossless raw source record; the seam must be registered only after its service dependency map and every caller are reviewed. Do not begin AnalyzeRule/AnalyzeUrl integration until that dependency seam is real.

### Audit-only acceptance and remaining gates

- [x] Exact pinned BookSource, Book, BookChapter and BaseSource source declarations inspected.
- [x] Host ReadingEngine contract, source codec, durable catalog and codec tests inspected.
- [x] Source identity and BookChapter Room/equality/index distinctions recorded.
- [x] Android/Room/Parcelable/app service boundary and no-fake-adapter constraint confirmed.
- [ ] A concrete Android-side model/service adapter implementation and complete caller inventory remain to be designed and separately pre-registered.
- [ ] Official Book/BookChapter persistence and round-trip fixtures remain unimplemented.
- [ ] Full rule engine, full JsExtensions closure, unified lifecycle, batch cache, Badcat categories 1/2/4/5, physical device and release acceptance remain open.

Self-review: this audit found that “BookChapter identity is index” is incomplete if stated without context: Room persists by (bookUrl,url), separately enforces unique (bookUrl,index), while equals/hashCode compare URL only. Preserve all three facts in future cache and adapter design. This audit authorizes no runtime code edits and makes no feature-completion claim.

## 2026-11-20 persistence and Android integration boundary follow-up

### Current host evidence (re-read at branch head 0bce0f5862c516e4d35c89103010f6995c29594e)

- The app module has no Room dependency, no Room database, no DAO, and no Book/BookChapter entity files. The only durable source catalog is `FileBookSourceCatalog`, which stores a JSON array in app-private `AtomicFile` storage. This is useful for lossless source import/export, but it is not an upstream BookSource Room repository and it does not persist books or chapters.
- `integration-contract` is an Android-free Kotlin module. Its `ReadingEngine.BookSource`, `BookCandidate`, `BookDetail`, and `BookChapter` are presentation/engine projections. `BookChapter` currently contains only index/title/url. They must remain clearly distinguished from official entities.
- The app already vendors pinned rule entity classes under `io.legado.app.data.entities.rule` and has a focused model test, but the source codec still stores rule JSON as strings in the platform-neutral descriptor. The seven model files' provenance does not make the full BookSource/BaseSource runtime present.
- The current app Gradle dependencies include Gson, Flexbox, Rhino host, OkHttp and parser/crypto dependencies, but no Room runtime/compiler or upstream database stack. The source catalog's AtomicFile writes are atomic and bounded; that does not supply upstream Book/BookChapter persistence semantics.
- The current `BadcatReadingEngineFactory` wires real host implementations for HTTP, CookieJar, login state, key-value storage, file cache and the existing Rhino bridge. It does not provide the full upstream `CacheManager`, `CookieStore`, `SharedJsScope`, `RuleBigDataHelp`, `ReadBook`, `BookHelp`, `ContentProcessor`, `AppConfig`, or application database contracts required by pinned entities and `BaseSource`.
- The host `legado-rhino` module provides Rhino execution/sandbox infrastructure, but does not define the pinned `io.legado.app.help.JsExtensions` API. A source-only copy of `BookSource` would therefore fail its actual supertype/runtime contract; replacing BaseSource with an empty interface or changing `getSource()` to suit the host would be a fake upstream port.

### Dependency-safe implementation decision

Do not add isolated Room annotations to a DTO and do not port `BookSource` before its actual `BaseSource : JsExtensions` dependency exists. The next implementation must establish an Android-side Legado runtime boundary that owns the exact pinned models and their services. It must connect to the existing host services through explicit adapters (network, cookies, source-keyed persistent values, AES login info, cache, shared JS scope, concurrency and reader context) while preserving the exact upstream API and lifecycle. The JVM contract stays Android-free and receives deliberate projections only.

Before runtime edits, separately pre-register the first concrete module slice with:
1. an exact upstream source manifest and license/provenance for every copied model/service file;
2. a complete dependency closure for BookSource/BaseSource/JsExtensions and Book/BookChapter/BaseBook/RuleDataInterface, with no unresolved app singleton or helper;
3. a migration design for current AtomicFile source catalog and future Room books/chapters, including schema keys, migrations and unknown-field storage;
4. a complete caller list for codec/catalog/source activities, ReadingEngine, login/session and chapter persistence/cache;
5. pinned fixtures proving official Gson defaults, typed rule mutation, unknown-field round-trip, Book/Chapter identity and persistence behavior.

### New explicit blockers

- [ ] No Android database/Room infrastructure or official Book/BookChapter persistence.
- [ ] No complete pinned JsExtensions type in the app source tree; the Rhino runtime is infrastructure, not the upstream host API.
- [ ] No full BaseSource service lifecycle or its persistent state adapters.
- [ ] No official Book/BookChapter implementation; host projections are intentionally lossy.
- [ ] No complete caller inventory for future Book/Chapter persistence because no persistence path currently exists.

No runtime code changed during this follow-up. The source catalog's existing JSON preservation is retained and must not be regressed.

## 2026-11-20 pinned runtime dependency closure and database migration audit

### Exact source declarations inspected

Pinned upstream files inspected at `330e606c4d57e08adc056ff8ab7a4132c784da9a`:
- `app/src/main/java/io/legado/app/help/JsExtensions.kt` (42,899 source characters)
- `app/src/main/java/io/legado/app/data/entities/BaseSource.kt` (12,726 characters)
- `app/src/main/java/io/legado/app/data/AppDatabase.kt` (14,061 characters)
- `app/src/main/java/io/legado/app/data/DatabaseMigrations.kt` (28,881 characters)
- `app/src/main/java/io/legado/app/help/http/CookieStore.kt`
- `app/src/main/java/io/legado/app/help/CacheManager.kt`
- `app/src/main/java/io/legado/app/model/SharedJsScope.kt`
- `app/src/main/java/io/legado/app/model/BatchContentContext.kt`
- `app/src/main/java/io/legado/app/model/analyzeRule/RuleDataInterface.kt`
- `app/src/main/java/io/legado/app/help/book/BookHelp.kt`, `ContentProcessor.kt`, `model/ReadBook.kt`, `AnalyzeRule.kt`, and `AnalyzeUrl.kt`.

### Dependency closure — not just model files

- `JsExtensions` is a large Android-facing interface extending `JsEncodeUtils`. Its API surface directly references Android WebView, activities/dialogs, UI events, verification/browser flows, `AnalyzeUrl`, HTTP response/Jsoup types, archive/file/encoding helpers, font/TTF query, book/reader configuration, video playback, and `BatchContentContext`. It also carries overload/default behavior for networking, file/archive access, cookies, browser, URL helpers, locking/single-flight, theme/read configuration and JS lifecycle. Copying this interface without its real implementations would leave compile-time or behavioral gaps.
- `BaseSource` adds source identity and mutable source settings plus login UI v2, login headers, AES-encrypted loginInfo, source-scoped variables, persistent key/value access, JS evaluation, jsLib refresh and explore cache invalidation. It calls `ScriptBindings`, `RhinoScriptEngine`, `SharedJsScope`, `CacheManager`, `CookieStore`, `AppConfig`, source helpers and Android crypto.
- `CookieStore` is not just an OkHttp CookieJar: it uses the upstream database CookieDao, domain extraction, memory cache, session cookies, WebView cookie manager and cleanup semantics.
- `CacheManager` is not just a file cache: it provides persistent database string/int/long/float/double values, expiry, in-memory LRU, binary/file cache and source-variable cleanup. `SharedJsScope` adds shared compiled jsLib scopes, CryptoJS installation, per-scope global state, source lifecycle and synchronization.
- `BookHelp` / `ContentProcessor` own chapter content persistence and replacement, cache version tokens, file naming, local book behavior, and book/chapter DB interactions. `BatchContentContext` resolves chapter objects/URLs against the current batch, rejects ambiguous duplicate URLs, applies the real content replacement rule, and persists with a per-chapter save token. These are not replaceable with a generic cache write.
- `AnalyzeRule` and `AnalyzeUrl` depend on this service graph and source model. They cannot be ported before the real source and persistence boundary is in place.

### Database/schema facts

- Pinned upstream `AppDatabase` is Room schema version **113**, with 26 entity classes and a `book_sources_part` view. It contains Book, BookSource, BookChapter plus many other product tables (cookie/cache, search, reading records, bookmarks, rules, highlights, memos, RSS and more).
- Its schema history includes a long migration chain: explicit migrations from 10→43 and 100→101→…→113, auto migrations from 43→100 (with custom specs), 101→104 and 110→112, plus custom 112→113. Versions 1–9 are destructive-migration fallbacks in the upstream app. This is not a small isolated schema that can be safely copied as a few entity annotations.
- The current Badcat app has no Room database/dependency/DAO. Its AtomicFile source catalog is a separate, private JSON store. Importing the entire upstream AppDatabase migration chain would also import a broad unrelated product data model and is not authorized by the current Stage 1 scope.
- Therefore the implementation boundary must choose and document one of two deliberate approaches before code: (A) port the complete upstream Room DB/entity/DAO/migration closure needed for the requested behavior, with verified upstream schemas and migration tests; or (B) keep a new, explicitly Badcat-owned Room schema and implement exact upstream model/serialization semantics through adapters, with a proven migration from the current AtomicFile source catalog and equivalent book/chapter identity/persistence behavior. It must not falsely claim to be upstream AppDatabase if choosing B.

### Decision and next registration requirement

The runtime service closure is larger than the model audit initially assumed. No code should be copied into the app until the project explicitly selects A or B, inventories every imported upstream file/license and every host caller, and specifies migration/rollback tests. The safest likely incremental route is B (host-owned database schema with exact upstream model behavior at the Android boundary), but this is a design recommendation, not an implementation decision or completion claim. The next pre-registration must include:
1. schema choice and rationale;
2. exact field mapping and converter/default behavior for BookSource, Book, BookChapter and six rule entities;
3. real adapters for source login/cookie/cache/shared JS state and chapter persistence, with no null/no-op implementations;
4. migration from current AtomicFile catalog preserving raw JSON and IDs;
5. caller inventory and tests for import/export/edit/toggle/reload, book/chapter identity, content save tokens and restart persistence;
6. explicit exclusions for unrelated upstream product tables unless their callers are part of the required feature.

No runtime code changed in this audit. This is a dependency/design gate, not Stage 1 model completion.
