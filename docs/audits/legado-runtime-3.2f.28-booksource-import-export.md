# Runtime-3.2f.28 — BookSource import/export compatibility audit

- Audit branch/HEAD: `integration/legado-upstream-runtime` / `af316d65a5a7d7f68887ecdf4f4d2cdfc4bf97e3`
- Fixed upstream baseline: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: read-only source/catalog import and export behavior; no production or test code changed.

## Executive result

**NO-GO for full BookSource import/export compatibility.** The host can import and export a bounded subset of Legado JSON source documents and preserves the original JSON object for unknown-field round-trip. That is a useful metadata-preservation layer, not parity with the pinned upstream import/export workflow.

The clearest confirmed gaps are:
1. Upstream accepts a `sourceUrls` wrapper and recursively fetches listed source documents; host `LegadoBookSourceCodec.decode` accepts only one source object or an array.
2. Upstream import accepts remote URLs, local content URIs, and JavaScript source configuration text; host UI currently imports HTTP(S) JSON URLs or local JSON documents only.
3. Upstream allows a source with a nonblank `bookSourceUrl` and an empty/missing `bookSourceName` (the model default is empty); host codec requires a nonblank name.
4. Upstream export of exactly one JS source writes its `mainJs` as a `.js` file; host export always emits a JSON array of the whole catalog.
5. Upstream import is a review/selection workflow with timestamp comparison, manual selection, optional rule replacement, and keep-name/group/enabled preferences; host import immediately merges all decoded sources by ID and replaces existing records.

## Pinned upstream evidence

### Model and serialization

- `app/src/main/java/io/legado/app/data/entities/BookSource.kt`, blob `7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c`.
  - The data class defines the source identity/name/group/type, custom order, enabled flags, cookie-jar flag, concurrency/header/login fields, JS library and main script, explore/search URLs, six typed rule models, comments/metadata, timestamps/weight, event listener and custom button fields.
  - `bookSourceUrl` is the primary identity; `bookSourceName` defaults to an empty string; most optional metadata and rule models are nullable.
  - Rule JSON storage uses converters for ExploreRule, SearchRule, BookInfoRule, TocRule, ContentRule and ReviewRule.
- `app/src/main/java/io/legado/app/utils/GsonExtensions.kt`, blob `c0bebcb9bf072f04a680e269765bb5b2e76cd88d`.
  - `INITIAL_GSON` registers Map/Int/String/LocalDate adapters, LONG_OR_DOUBLE number strategy, disables HTML escaping and uses pretty printing.
  - `GSON` adds deserializers for all six rule model types.
  - The pinned rule deserializers accept either a JSON object or a JSON primitive containing JSON text; arrays/other JSON shapes return null for that rule.
  - `StringJsonDeserializer` converts any JSON primitive with `asString`, preserves JSON null as null, and serializes non-primitive JSON elements to JSON text. `IntJsonDeserializer` accepts numeric primitives and calls `toInt()`; other shapes return null.
- Rule deserializer blobs: SearchRule `ba8cda4543a4afb507224ba57f0f1f5ab4ad5594`; ExploreRule `6871515ea685dc4cb2245ecc45c952604634267d`; BookInfoRule `54fd639003b4b4780d095c5afa379eac25a8620b`; TocRule `f525e912340257125059b6ad87d0cf7e73d6a1dc`; ContentRule `74020cd33c89dc0af189bae2ed68afd2c52d1446`; ReviewRule `cb22a3b55e405e4303cd41987a8177000867cbc9`.

### Import path

- `app/src/main/java/io/legado/app/ui/association/BookSourceImport.kt`, blob `fe2ba68fd934571d6ea2949b2da86306c9406203`, `parseBookSourceJson`:
  - accepts a single object or array;
  - accepts a `sourceUrls` object wrapper when `allowSourceUrls=true`, rejects null/blank URL entries, and permits an empty list;
  - source objects are checked for nonblank `bookSourceUrl`; the parser does not require a nonblank source name.
- `app/src/main/java/io/legado/app/ui/association/ImportBookSourceViewModel.kt`, blob `65ef5602476f145e1f95a95eead1ac0551378549`:
  - accepts JSON object/array, absolute URL, content URI, or JS source text;
  - URL imports download and decompress content; a special `#requestWithoutUA` suffix changes the User-Agent behavior;
  - source URL lists are fetched one by one;
  - candidates are compared to local source `lastUpdateTime`, can be manually selected, can apply configured source replacement rules, and may preserve local name/group/enabled state and custom order.
- `app/src/main/java/io/legado/app/api/controller/BookSourceController.kt`, blob `cc5dccf4b38abf9c9d229e7753a712aa751ef391`:
  - API single-source save checks name and URL;
  - batch save inserts only rows with nonblank name and URL, skipping invalid rows rather than rejecting the whole batch;
  - API list/get/delete are separate paths from the import dialog.
- `app/src/main/java/io/legado/app/ui/book/source/manage/BookSourceViewModel.kt`, blob `f0d1230b2baef6573f958bbdf2bbfeac6c20a9d0`:
  - export uses the current adapter selection (the visible/selected source set);
  - exactly one JS source exports `mainJs` as a `.js` file;
  - other selections serialize BookSource objects using the configured GSON writer (pretty JSON).
- Upstream import tests: `app/src/test/java/io/legado/app/ui/association/BookSourceImportTest.kt`, blob `1032c5b4f1da9c72657aca98e4efc1d374eb57f4`; fixtures cover single object, missing URL, invalid array item, valid arrays and `sourceUrls` wrapper, plus selection/replacement policy tests. This is not a complete field-by-field export round-trip suite.

## Host evidence

- `integration-contract/src/main/kotlin/app/badcat/engine/LegadoBookSourceCodec.kt`, blob `2d6f22a65898a2eaa4a8ee6386dd8f71d116e0dd`:
  - root must be one JSON object or array; arrays are decoded atomically and duplicate source IDs are rejected;
  - each item requires nonblank `bookSourceUrl` **and** nonblank `bookSourceName`;
  - maps the known BookSource metadata/rule fields into a typed host projection, while retaining the complete normalized JSON object in `rawJson`;
  - rule values accept an object or a string containing a JSON object; malformed known-field types are rejected;
  - export re-parses each `rawJson` object and emits a JSON array; it applies 5 MiB, 2,000-source, 64-level nesting and 512-fields-per-source limits, and rejects duplicate IDs.
- Host engine model `integration-contract/src/main/kotlin/app/badcat/engine/ReadingEngine.kt`, blob `4777ceae6af13633c2561c7fc77c4cbde3a87480`: typed projection maps upstream URL/name/group to `id`/`baseUrl`/`name`, carries the six rule JSON strings and the source metadata fields, and carries `rawJson` through the engine-facing model. The projection is not the upstream Room entity or its complete type graph.
- `app/src/main/java/app/badcat/client/FileBookSourceCatalog.kt`, blob `1f3c9cd879e58f533f57352e3074c667c06c664b`:
  - imports a single object or array, merges by source ID, replaces existing IDs in place, writes with Android AtomicFile, then invalidates changed source runtime state;
  - exports the whole catalog as a JSON array in catalog order;
  - this class explicitly describes itself as a metadata catalog and does not claim to execute source JavaScript or full Legado compatibility.
- `app/src/main/java/app/badcat/client/SourceCatalogActivity.kt`, blob `20ca409aa192ce4e2f7a7d6e41de9a8fac059e6b`:
  - exposes network JSON URL import, local JSON document import and whole-catalog JSON export;
  - network fetch has bounded redirects, rejects HTTPS-to-HTTP downgrade and credentials, and enforces a 5 MiB body limit;
  - local import reads a JSON document; no JS source file import or sourceUrls wrapper processing is wired here.
- Existing host codec tests: `integration-contract/src/test/kotlin/app/badcat/engine/LegadoBookSourceCodecTest.kt`, blob `e5958153e82aa980835c5d875260f3222c2ae736`. They cover object/array decoding, defaults, nullable cookie flag, unknown-field retention, enabled-state mutation, malformed field/rule types, legacy string-encoded rule objects, size/depth/count limits and duplicate IDs. They do not establish parity with upstream's full import UI, sourceUrls fetch flow, JS-source import/export, selection policy or Gson coercion behavior.

## Differential matrix

| Behavior | Pinned upstream | Current host | Result |
|---|---|---|---|
| Single JSON object / array | Yes | Yes | Partial match |
| `sourceUrls` wrapper | Supported; fetch each URL | Rejected as a source object | Gap |
| URL / local URI import | URL, content URI | HTTP(S) URL, local JSON document | Partial match; URI path types differ |
| JS source text / `.js` import | Supported by JS source extractor | Not wired in catalog UI | Gap |
| Missing/blank source name | Model default empty; parser validates URL | Rejected | Compatibility difference |
| Unknown source fields | Gson entity may only materialize known fields | Original JSON retained in `rawJson` | Host preservation advantage for round-trip |
| Nested rule object / JSON-string object | Rule adapters accept both | Codec accepts both and retains raw form | Shape-level match; rule execution not covered here |
| Other malformed field types | Custom Gson adapters may coerce/convert some primitive/object values or null out invalid Ints | Explicit validation rejects many mismatches | Intentional strictness, not exact parity |
| Duplicate IDs in one input document | Entity equality is URL-based; import parser itself does not reject duplicate IDs | Codec rejects whole document | Unresolved policy difference; verify DAO/upsert outcome before changing |
| Existing-source update | Review/selection; timestamp and user preferences; replacement rules | Immediate replace by ID | Gap |
| Export selected sources | Yes; selected adapter rows | Whole catalog only | Gap |
| Export one JS source | `.js` containing `mainJs` | JSON array | Gap |
| JSON formatting | Configured pretty GSON output | compact JSON array from JsonElement | Formatting difference; semantic fields retained in raw JSON |

## Data and failure semantics

- Host `rawJson` is the strongest current preservation mechanism: unknown upstream keys survive import, catalog persistence, enabled-flag mutation and export unless a source is replaced by a new import. This does not mean the runtime understands or executes those fields.
- Host import rejects a malformed item or duplicate ID before writing any catalog update; it does not partially import valid siblings. Upstream UI parsing rejects an invalid item in a JSON array as a parse failure, but its Web API batch-save endpoint separately filters and inserts valid rows. These are distinct upstream entry points and must not be conflated.
- Host catalog writes are AtomicFile-backed and occur before runtime invalidation; cleanup failure is reported as a failure even though the catalog change has committed. This lifecycle behavior is outside upstream BookSource JSON shape parity.
- Exact null/missing behavior for every primitive and nested rule field needs executable differential fixtures against the pinned Gson runtime. The current host test suite covers selected fields only.

## Recommended next tasks

1. **Small, safe first step:** preregister differential fixtures for the confirmed empty-name acceptance difference and sourceUrls wrapper recognition. Do not yet fetch nested URLs until redirect, recursion depth, duplicate/cycle, partial failure and security semantics are separately specified.
2. Add a dedicated JS-source import/export contract task only after reviewing `JsSourceConfig.extract` and the upstream JS-source format; preserve the host's safe file/network boundaries.
3. Build a generated field matrix from the complete pinned BookSource model and compare every property against the typed host projection and raw JSON round-trip; include null/missing/primitive coercion tests using pinned GSON.
4. Do not change the host's immediate replace behavior or export-all UX without a separately approved product decision; these are workflow differences, not merely codec bugs.

## Conclusion

Runtime-3.2f.28 closes as a read-only audit with **partial JSON compatibility / no full import-export parity**. No production/test/Gradle/workflow/UI changes, build, CI or device tests were performed. The .24 and .26 CI runs were not used as evidence for this audit.
