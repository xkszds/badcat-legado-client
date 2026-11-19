# Pinned upstream model and serialization audit

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
