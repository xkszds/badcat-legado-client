# Runtime-3.0 BookSource canonical model and file-catalog boundary

**Task:** `R3-S0-T026-PHASE-B-BOOKSOURCE-CANONICAL-MODEL-026N`  
**Date:** 2026-11-25  
**Status:** read-only model/data-owner decision complete; migration is blocked pending BaseSource host bridge design.  
**Badcat start HEAD:** `9d6db504c5e878008d3cc4afbff79f8041b70aa7`  
**Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## 1. Decision

1. **The only acceptable canonical Legado source data model is the pinned upstream `io.legado.app.data.entities.BookSource`**, with all 33 properties, typed rule objects, defaults, nullability, annotations, helpers and URL-based identity. The current `app.badcat.engine.BookSource` and `LegadoBookSource` are temporary shadow DTOs. They must not remain the Runtime's authoritative source model.
2. **Keep persistence in the App host for now.** The current AtomicFile-backed `FileBookSourceCatalog` remains the only Badcat source catalog writer until a separate data-owner decision. It should eventually deserialize/execute the authentic upstream model and serialize it back losslessly. Do not add Room schema v1, Room database/compiler/KSP, AppDatabase, DAO or dual-write.
3. **Do not copy `BookSource.kt` alone yet.** Upstream `BookSource` implements `BaseSource`, and `BaseSource` contains 29 methods with JS, Rhino, Cookie, cache, login, Android-ID/AES, shared-scope, thread and source-lifecycle effects. A safe typed Host Port/delegate design preserving the JS-visible method names and `this` identity is not frozen. `BookSource` is a final data class, so a wrapper cannot simply subclass it. The exact concrete-type callers and `BaseSource` API must be reconciled before migration.
4. Room annotations are model metadata, not a decision to use Room. If the exact entity is later placed in `:legado-runtime`, the likely minimum compile dependency is `androidx.room:room-common:2.8.4`; the Runtime already applies Kotlin Parcelize 2.4.20. This does not authorize Room runtime, KSP compiler, AppDatabase, schema or migrations.
5. The current raw JSON retention is valuable and must not be lost. The final design may keep a raw `JsonObject`/unknown-field serialization envelope inside the App file adapter, but it must not become a second field-for-field source DTO or a second runtime model. Exact nested-rule unknown-field merge behavior remains to be proven.

## 2. Exact upstream BookSource contract

`BookSource.kt` is a 357-line Kotlin `data class` with 33 constructor properties. It is `@Parcelize`, `Parcelable`, `@Entity(tableName="book_sources")`, `@TypeConverters(BookSource.Converters::class)`, has a non-unique index on `bookSourceUrl`, and uses `bookSourceUrl` as `@PrimaryKey`.

| # | Upstream property | Kotlin type / constructor default | Room annotation/default | Current host representation | Parity issue |
|---:|---|---|---|---|---|
| 1 | `bookSourceUrl` | `String = ""` | `@PrimaryKey` | `LegadoBookSource.sourceId`; `BookSource.id` and `BookSource.baseUrl` | One upstream identity is duplicated into two host fields; codec requires nonblank instead of retaining the model's empty default. |
| 2 | `bookSourceName` | `String = ""` | — | `name` | Current codec accepts missing as empty but rejects explicit null; exact Gson null behavior is not differentially proven. |
| 3 | `bookSourceGroup` | `String? = null` | — | `group` | Direct field mapping only; upstream group helper methods are absent from DTO. |
| 4 | `bookSourceType` | `Int = 0` | `@BookSourceType.Type` | `sourceType` | Value mapped; source type annotation/API is not represented. |
| 5 | `bookUrlPattern` | `String? = null` | — | `bookUrlPattern` | Direct field mapping. |
| 6 | `customOrder` | `Int = 0` | `@ColumnInfo(defaultValue="0")` | `customOrder` | Direct field mapping; order tie semantics must remain stable. |
| 7 | `enabled` | `Boolean = true` | `@ColumnInfo(defaultValue="1")` | `enabled` | Direct field mapping. |
| 8 | `enabledExplore` | `Boolean = true` | `@ColumnInfo(defaultValue="1")` | `enabledExplore` | Direct field mapping. |
| 9 | `jsLib` | `String? = null` | — | `jsLib` | BaseSource JS-library lifecycle is not represented. |
| 10 | `enabledCookieJar` | `Boolean? = true` | `@ColumnInfo(defaultValue="0")` | `enabledCookieJar` | Important dual default: Kotlin constructor default is `true`, Room SQL default is `0`; preserve both independently. |
| 11 | `concurrentRate` | `String? = null` | — | `concurrentRate` | Value mapped; source-scoped limiter lifecycle is not part of DTO. |
| 12 | `header` | `String? = null` | — | `header` | Value mapped; JS/JSON evaluation, UA merge and login-header precedence are not represented. |
| 13 | `loginUrl` | `String? = null` | — | `loginUrl` | Value mapped; `getLoginJs` behavior is absent. |
| 14 | `loginUi` | `String? = null` | — | `loginUi` | Value mapped; Login UI v1/v2 parsing and callback lifecycle are absent. |
| 15 | `loginCheckJs` | `String? = null` | — | `loginCheckJs` | Value mapped; login-check execution is not equivalent to storing the script. |
| 16 | `coverDecodeJs` | `String? = null` | — | `coverDecodeJs` | Value mapped; execution path is not represented. |
| 17 | `bookSourceComment` | `String? = null` | — | `bookSourceComment` | Direct field mapping; error-comment helpers are absent. |
| 18 | `variableComment` | `String? = null` | — | `variableComment` | Direct field mapping; display helper is absent. |
| 19 | `lastUpdateTime` | `Long = 0` | — | `lastUpdateTime` | Direct field mapping. |
| 20 | `respondTime` | `Long = 180000L` | — | `respondTime` | Direct field mapping. |
| 21 | `weight` | `Int = 0` | — | `weight` | Direct field mapping. |
| 22 | `exploreUrl` | `String? = null` | — | `exploreUrl` | Direct field mapping; explore cache invalidation is separate behavior. |
| 23 | `exploreScreen` | `String? = null` | — | `exploreScreen` | Direct field mapping. |
| 24 | `ruleExplore` | `ExploreRule? = null` | `@TypeConverters` | `ruleExploreJson: String?` | Current host stores JSON text, not the typed upstream rule model or its default getter. |
| 25 | `searchUrl` | `String? = null` | — | `searchUrl` | Direct field mapping. |
| 26 | `ruleSearch` | `SearchRule? = null` | `@TypeConverters` | `ruleSearchJson: String?` | Current host stores JSON text, not typed upstream rule/default getter. |
| 27 | `ruleBookInfo` | `BookInfoRule? = null` | `@TypeConverters` | `ruleBookInfoJson: String?` | Current host stores JSON text, not typed upstream rule/default getter. |
| 28 | `ruleToc` | `TocRule? = null` | `@TypeConverters` | `ruleTocJson: String?` | Current host stores JSON text, not typed upstream rule/default getter. |
| 29 | `ruleContent` | `ContentRule? = null` | `@TypeConverters` | `ruleContentJson: String?` | Current host stores JSON text, not typed upstream rule/default getter. |
| 30 | `ruleReview` | `ReviewRule? = null` | `@TypeConverters` | `ruleReviewJson: String?` | Current host stores JSON text; upstream property exists although the convenience getter is commented out. |
| 31 | `mainJs` | `String? = null` | — | `mainJs` | Value mapped; upstream `getLoginJs()` returns `mainJs` for JS sources. |
| 32 | `eventListener` | `Boolean = false` | `@ColumnInfo(defaultValue="0")` | `eventListener` | Direct field mapping; event callback execution is absent. |
| 33 | `customButton` | `Boolean = false` | `@ColumnInfo(defaultValue="0")` | `customButton` | Direct field mapping; UI renderer remains App-side. |

All 33 upstream property values have a representation in the current codec/host DTO, but this is **not** model parity: six rule fields are raw JSON strings, `bookSourceUrl` is duplicated as `id` and `baseUrl`, DTO constructor requirements/defaults differ, and the upstream methods/inheritance/annotations/identity semantics are missing.

### 2.1 BookSource methods and identity

- `getTag()` returns `bookSourceName`; `getKey()` returns `bookSourceUrl`; `getSource()` returns the same `BaseSource` object.
- `equals()` and `hashCode()` use only `bookSourceUrl`. The current two DTOs are Kotlin data classes whose generated equality compares all fields (including raw JSON); this is observably different.
- `getSearchRule()`, `getExploreRule()`, `getBookInfoRule()`, `getTocRule()` lazily create and store typed default rule objects when null.
- `getLoginJs()` returns `mainJs` for a JS source and otherwise delegates to `BaseSource.getLoginJs()`.
- Other source-visible helpers include display/group management, invalid group removal, error comments, check keyword selection, JS-source detection, batch-content support/size, `checkContent()` and `equal(source)`.
- `checkContent()` deliberately normalizes `customOrder`, `enabled`, `enabledExplore`, `lastUpdateTime`, `respondTime` and `weight` before serializing; `equal(source)` compares a defined set of source metadata and typed rules. These must not be replaced by data-class equality.
- `BookSource.Converters` defines JSON↔typed-model converters for ExploreRule, SearchRule, BookInfoRule, TocRule, ContentRule and ReviewRule (12 conversion methods). The current codec's manual `JsonObject` parsing/validation is not a substitute for those typed models and converters.

## 3. BaseSource host-effect closure (29 methods)

`BaseSource` is not a plain marker interface. It extends `JsExtensions`, exposes Android `@JavascriptInterface` methods and owns the source identity and JS-facing methods below. It imports Android JavascriptInterface, Hutool AES, Rhino `ScriptBindings`/`RhinoScriptEngine`, App constants/logging/config, CacheManager, ConcurrentRateLimiter, CookieStore, SharedJsScope, JsSourceEngine, LoginUiV2, GSON utilities and coroutine/thread helpers.

| Method | Required behavior / owner |
|---|---|
| `getTag`, `getKey`, `getSource` | Preserve source name, `bookSourceUrl` key and self/source identity. |
| `getLoginUiJs`, `isLoginUiV2`, `getLoginJs`, `hasLoginForm`, `hasLogin` | Preserve inline-JS extraction, Login UI v2 protocol detection and source/login availability. |
| `evalLoginUiV2`, `evalLoginActionV2`, `login` | Execute source JS with exact state/action/form/book/chapter bindings; preserve exception and callback semantics. |
| `getHeaderMap` | Evaluate header JS or parse JSON, apply default UA, merge login header with exact precedence; rethrow `CancellationException`, log other errors. |
| `getLoginHeader`, `getLoginHeaderMap` | Read source-keyed stored header and decode map. |
| `putLoginHeader`, `removeLoginHeader` | Persist/delete login header and synchronize CookieStore from/to the `Cookie` header. |
| `getLoginInfo`, `getLoginInfoMap`, `putLoginInfo`, `removeLoginInfo` | Android-ID-derived AES storage, JSON map, legacy/dynamic Login UI defaults, recursion guard, exact error/null/Boolean behavior. |
| `setVariable`, `putVariable`, `getVariable`, `put`, `get` | Preserve source-keyed variable/cache namespaces, null/remove behavior and empty-string fallback. |
| `refreshExplore` | Must run off main thread and invalidate source Explore InfoMap cache. |
| `refreshJSLib` | Must run off main thread and clear shared JS library scope/cache. |
| `putConcurrent` | Update source-specific concurrency rate limiter. |
| `evalJS` | Build Rhino bindings with `java`, `source`, `sourceApi`, `baseUrl`, `cookie`, `cache`; chain shared global scope and evaluate. |

This yields explicit host obligations for source execution state, JS scope, HTTP, CookieStore, login/session, AES/Android ID, source/rule variables, cache, rate limiting, Explore invalidation, AppConfig/UA, logging and thread/cancellation. A plain `BookSource` data copy or the current reduced Rhino extension class does not satisfy the BaseSource API.

**Blocker:** a `BookSource` instance is a final data class implementing `BaseSource`. A wrapper cannot subclass it. Moving BaseSource as-is imports App/Android side effects; replacing methods with a global service locator or ThreadLocal would violate the typed, lifecycle-safe Host Port boundary. A per-source adapter/delegate may be viable, but it must preserve `getSource()`/`this` identity, all JS-visible names/overloads, same-source concurrency, cancellation, session/cookie state and every concrete-`BookSource` caller. This has not yet been proven.

## 4. Current Badcat source model and catalog behavior

### 4.1 Two shadow DTOs

- `integration-contract/.../ReadingEngine.kt` defines `app.badcat.engine.BookSource` with 35 properties: 33 upstream values plus `id`/`name`/`baseUrl` aliases and `rawJson`. `id` and `baseUrl` both receive `bookSourceUrl` from the codec but can diverge when manually constructed. Rules are six `String?` JSON fields, not upstream rule objects.
- `integration-contract/.../LegadoBookSourceCodec.kt` defines a second `LegadoBookSource` with 34 properties: source metadata plus `rawJson`; it also stores rules as JSON strings. `toEngineSource()` then creates the first DTO. Both are shadow models and must be retired once the canonical Runtime model and App repository adapter are ready.
- The codec manually validates known JSON types, applies defaults, rejects duplicate source URLs, and bounds imports at 5 MiB/2,000 sources/64 nesting/512 fields. It preserves the original top-level JSON in `rawJson`, so unknown fields survive current export. These security limits and strict type checks are current Badcat behavior; do not silently drop them, but compare each with the pinned importer before claiming exact compatibility.
- The current codec test includes a source-derived `sourceUrls` manifest parser fixture and tests field/default/null/unknown-field behavior. However, the production catalog's `importJson()` calls `LegadoBookSourceCodec.decode()`, which explicitly rejects `sourceUrls` manifests; it does not fetch manifest URLs. The actual upstream import UI/network fetch implementation was not read in 026N, so that behavior remains unverified.

### 4.2 FileBookSourceCatalog is App-owned persistence

`app/src/main/java/app/badcat/client/FileBookSourceCatalog.kt` stores `legado-book-sources.json` in `context.applicationContext.filesDir` using Android `AtomicFile`, a process-wide coroutine `Mutex`, bounded UTF-8 read/write and raw JSON objects.

| Operation | Current behavior |
|---|---|
| `all()` | Read file and return source DTOs sorted by `customOrder` ascending; Kotlin stable sorting preserves original order for ties. |
| `importJson()` | Accept object/array only; duplicate source URLs rejected; merge by sourceId in a LinkedHashMap, replace existing source in its original slot, append new sources; invalidate changed existing IDs after AtomicFile commit. |
| `setEnabled()` | Change only `enabled` in original JSON, preserving other raw fields; no state invalidation. |
| `remove()` | Remove source and commit first, then invalidate runtime state; absent ID returns false; cleanup failure is reported even though file mutation has already committed. |
| `exportJson()` | Emit sorted raw JSON array, preserving unknown top-level/rule fields. |

Current catalog surface is much smaller than upstream `BookSourceDao`: no `enableExplore`, group/order operations, search/group/type flows, BookSourcePart projection, check-state lifecycle or Room transactions. Some are product source-manager operations and should remain App-owned, not become Runtime DAO dependencies.

`BadcatReadingEngineFactory` constructs `FileBookSourceCatalog(context)` with the default no-op `onSourceStateInvalidated` callback and exposes it through `sourceCatalog()`. `SourceCatalogActivity` separately constructs a catalog with `sourceScriptRuntime.clearSourceState(sourceId)`, and `RhinoSourceScriptRuntime.clearSourceState` iterates live runtime instances. `SourceLoginUiV2Activity` only reads the file catalog. Centralize this lifecycle under `SourceExecutionStatePort`; do not rely on a particular Activity's callback or a no-op default for mutation paths.

### 4.3 Upstream persistence ownership (do not import wholesale)

- Pinned upstream `AppDatabase` is Room version 113 and includes `BookSource`, `BookSourceCheckState`, `BookSourcePart` view and many unrelated product entities. It is not part of Badcat's current data owner and must not be copied into Runtime.
- `BookSourceDao` owns Room queries and transactions: ordered `all`/enabled/explore/login/group/search projections, source lookup, insert with `OnConflictStrategy.REPLACE`, update with check-content comparison, begin/finish/complete check, delete, enable, enableExplore, upOrder and upGroup. `BookSourceCheckState` is separate metadata with a foreign key to BookSource and cascade delete. `BookSourcePart` is a Room view used by source-manager UI.
- These DAO/UI projections are App host/catalog responsibilities. Runtime receives a canonical source model and typed ports; it does not own AppDatabase or Room transactions.

## 5. Canonical data and Host Port requirements

The final design should have one authoritative upstream `BookSource` model in Runtime and one App-owned file repository. The App `ReadingEngine` may expose a small `BookSourceSummary` presentation projection if the original Badcat UI needs it, but it must not call a reduced `BookSource` object the Runtime source model. The file adapter can retain raw `JsonObject` serialization metadata to preserve unknown fields, provided the authentic `BookSource` remains the only typed model and writes merge typed known fields without losing unknown top-level or nested rule JSON.

Required contracts before implementation:

1. `BookSourceCatalogPort`: load/list, import/replace, export, enable/disable, remove and ordering operations needed by Badcat; preserve sourceUrl identity, raw JSON, duplicate behavior, stable order, AtomicFile transaction and post-commit failure semantics.
2. `SourceExecutionStatePort`: attach/acquire per-source BaseSource services, clear JS scope/cache/cookies/session/variables after replace/remove, and preserve same-source serialization and cross-source concurrency.
3. `SourceCookiePort`/`SourceSessionPort`: exact source-key/domain semantics; login-header Cookie replacement/removal; Android-ID AES login-info; HTTP/WebView cookie synchronization; persistence and clear lifecycle.
4. `SharedJsScopePort`/`SourceCachePort`/`SourceVariablePort`/`RuleVariablePort`: exact CacheManager namespaces, shared lexical/global scope, JS library hash invalidation, source variables and rate limiting.
5. `SourceHttpPort`/`HostUiPort`/`HiddenWebViewPort`/`VisibleBrowserPort`/`VerificationCodePort`: preserve JS-visible callback order, request headers, cancellation, timeout, response and host lifecycle.

No exact new signatures are frozen by 026N. They must be derived from BaseSource and all actual callers in the next dedicated port-contract task; do not invent signatures or use a generic service map.

## 6. Evidence and test boundary

Evidence files outside the repo: `/root/stage0-runner/task026-clean/task026n-attempt1/`:

- `booksource-field-matrix.tsv` — all 33 upstream fields and current mappings/defaults.
- `basesource-host-methods.tsv` — all 29 BaseSource methods and their behavior/host effects.
- `catalog-operation-gap.tsv` — current FileBookSourceCatalog vs upstream source-manager operation gaps.
- `source-hashes.tsv` — exact hashes for the inspected upstream models/DAO/DB, Badcat contracts/catalog/factory/adapters and corresponding tests.

Relevant existing tests: `LegadoBookSourceCodecTest.kt` covers JSON size/type/default/null/unknown fields and a source-derived sourceUrls parser fixture; `FileBookSourceCatalogLifecycleTest.kt` covers post-commit invalidation and cleanup failure; `FileBookSourceCatalogOrderTest.kt` covers customOrder/tie ordering; `PublicBookSourceCompatibilityCorpusTest.kt` covers several CSS/JSONPath selector fixtures only. These tests do not prove full upstream `BookSource`/`BaseSource`, importer UI, Room or JS behavior. 026N ran no tests; all results are read-only source evidence.

## 7. Result and disposition

- Canonical model decision: upstream `BookSource` is the only acceptable Runtime source model; current `BookSource` and `LegadoBookSource` are shadow DTOs and must be retired, not expanded.
- Persistence decision: keep `FileBookSourceCatalog` and AtomicFile in App for now; preserve full JSON/unknown fields; no Room schema, Room DB, KSP, AppDatabase or dual-write.
- Migration blocker: `BookSource : BaseSource` with 29 BaseSource methods, `JsExtensions` inheritance, Android/Rhino/Cache/Cookie/session side effects and self identity. The typed per-source BaseSource delegate and concrete BookSource caller closure are not frozen. The Room annotation/Parcelable dependency is manageable in isolation but does not solve this host bridge.
- Import/export gap: current production catalog rejects sourceUrls manifests even though the codec has a parser fixture; upstream import UI network resolution was not inspected and remains unverified. Current catalog also has a smaller operation surface than BookSourceDao.
- No production source/build/test/manifest/schema/UI/Reader file changed. No Gradle/compiler/build/test/assemble/CI/network/device action occurred. No App compile retry.
- **Task 026N:** complete as a read-only canonical model/data-owner decision. **Next:** register a dedicated BaseSource typed Host Port/delegate contract closure task before any BookSource/BaseSource migration. Then register the actual BookSource + file-catalog adapter migration only if the self-identity, callback, raw JSON and model parity gates are proven.
- Phase A remains open; Phase B has started only with the bounded 026M DTO slice; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
