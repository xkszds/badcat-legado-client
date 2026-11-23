# Runtime-3.2f.31 — JS source import/export compatibility audit

- Audit branch / start HEAD: `integration/legado-upstream-runtime` / `4ff6b663d62b1b9eae762d068d7f16cc975e97b2`.
- Fixed upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Scope: read-only. No production/test/Gradle/workflow/UI files changed.
- Parallel context: .30 parser implementation SHA `41f95a6b72149d0ba6d00c2cf1dff7119a97d9f6`, CI run #3192 / `45089612632`, was active during this audit. This audit did not modify .30 codec or tests and does not borrow its CI result.

## Executive result

**NO-GO for JS source import/export parity.** The pinned upstream accepts a JavaScript source configuration program, evaluates and validates it into a BookSource model, preserves the original script in `mainJs`, and exports exactly one selected JS source as a `.js` file. The host catalog currently accepts JSON object/array documents only and exports the entire catalog as a JSON array. Its typed codec can retain a `mainJs` string inside a JSON BookSource, and the engine model carries that field, but this is not equivalent to importing a JS configuration script or exporting the script as a standalone file.

## Pinned upstream evidence

### JS configuration parser

- `app/src/main/java/io/legado/app/model/jsSource/JsSourceConfig.kt` at the fixed SHA:
  - Executes the supplied text through `RhinoScriptEngine.eval` in a runtime scope after installing CryptoJS.
  - Accepts top-level `config`; supports legacy top-level `source` when `config` is absent/incomplete. `config` wins when complete.
  - Normalizes the evaluated object to JSON, deserializes a BookSource using pinned GSON, and validates nonblank `bookSourceUrl` and `bookSourceName`.
  - Removes declarative `mainJs` and rule fields from the evaluated config object, normalizes array forms of `exploreUrl` and `loginUi`, then sets `source.mainJs = text` to preserve the complete original script.
  - Requires executable top-level functions: normal sources require `search`, `getChapters`, `getContent`; file sources require `search`, `getBookInfo`. Additional pairings are checked for explore, login/loginUi/loginAction, review functions and `getContentBatch`/maxBatchSize.
  - Propagates coroutine cancellation and Rhino interruption rather than converting cancellation into an ordinary parse error.
  - This is script evaluation, not a JSON-like text parser. The exact Rhino runtime behavior and all possible side effects are broader than the import-format audit; do not claim it is safe to evaluate arbitrary scripts without a separate security review.
- `app/src/main/java/io/legado/app/ui/association/ImportBookSourceViewModel.kt`:
  - For non-JSON input, calls `JsSourceConfig.extract(content, coroutineContext)`.
  - The result enters the same candidate comparison / manual selection / replacement flow as JSON sources.
- `app/src/test/java/io/legado/app/model/jsSource/JsSourceConfigTest.kt`:
  - Covers script metadata extraction and full `mainJs` retention, modern `const config`, legacy `source`, config precedence/fallback, required functions, file-source requirements, declarative rule stripping, explore/login normalization, maxBatchSize and review capabilities.
- `app/src/test/java/io/legado/app/ui/association/ImportBookSourceStateTest.kt`:
  - Confirms the non-JSON branch invokes `JsSourceConfig.extract`; state tests separately cover source selection behavior.
- `app/src/main/assets/js_source_template.js`:
  - Supplies a script template, not merely a JSON BookSource object.

### Export

- `app/src/main/java/io/legado/app/ui/book/source/manage/BookSourceViewModel.kt`:
  - Exports the adapter's current selection, described in source as exactly the visible and selected rows after filters.
  - Exactly one selected JS source is written as `<normalized source name>.js` with the stored `mainJs` content.
  - Other selections are serialized through configured GSON to a JSON file. A single non-JS source remains JSON.
  - Export is not always the entire catalog; selection is the export boundary.

## Host evidence

- `integration-contract/src/main/kotlin/app/badcat/engine/LegadoBookSourceCodec.kt`:
  - `decode` accepts a BookSource object or array; `.30` adds a typed `sourceUrls` manifest parser. Neither method evaluates JS source text.
  - `LegadoBookSource` maps `mainJs` as a nullable string and carries complete `rawJson`; this supports preservation of a script string already supplied as a JSON field, not parsing of a standalone `.js` configuration.
- `integration-contract/src/main/kotlin/app/badcat/engine/ReadingEngine.kt`:
  - Engine-facing source projection carries `mainJs`; field presence alone does not establish that a standalone JS import/export workflow exists.
- `app/src/main/java/app/badcat/client/FileBookSourceCatalog.kt`:
  - `importJson` calls `LegadoBookSourceCodec.decode`, merges by source ID and persists through AtomicFile.
  - `exportJson` serializes the whole catalog in catalog order. The class explicitly describes itself as metadata storage and says it does not execute source JavaScript.
- `app/src/main/java/app/badcat/client/SourceCatalogActivity.kt`:
  - The document picker requests `application/json`; local import reads bounded UTF-8 JSON then calls `catalog.importJson`.
  - Network import fetches bounded HTTP(S) content under redirect/credential/downgrade controls, then passes it to the same JSON catalog importer.
  - Export creates an `application/json` document and writes `catalog.exportJson()`. No `.js` file picker or script-config import branch is present in this activity.
- Host engine/runtime code may execute a persisted `mainJs` in selected runtime paths, but that is downstream execution capability, not JS config extraction/import parity. This audit does not certify the complete JS runtime or its sandbox/security properties.

## Differential matrix

| Behavior | Pinned upstream | Current host | Result |
|---|---|---|---|
| JSON source object/array import | Yes | Yes | Partial match |
| Standalone JS config text import | Evaluates with Rhino and extracts BookSource | Catalog accepts JSON only | Gap |
| `config` / legacy `source` selection | Both supported with precedence/fallback | No equivalent parser | Gap |
| Required JS function validation | search/getChapters/getContent; file source differs | Not part of catalog import | Gap |
| Explore/login/review/batch metadata normalization | Validates and normalizes script-derived config | Raw JSON field preservation only | Gap |
| Original full script retained as `mainJs` | Yes | Can preserve a JSON `mainJs` string | Partial field-level support only |
| Import JS into selection/review flow | Yes | Immediate JSON catalog merge | Gap |
| Export one selected JS source | Standalone `.js` containing `mainJs` | Whole catalog JSON array | Gap |
| Export selected multiple sources | Selected rows to JSON | Whole catalog | Gap |
| Export one selected non-JS source | JSON | Whole catalog JSON | Partial format match, selection mismatch |

## Risks and unresolved items

1. Upstream parser evaluates script code to obtain `config`; a future host implementation must not treat arbitrary JS text as inert JSON. It requires an explicit execution trust model, time/resource limits, cancellation, side-effect policy, and error mapping.
2. Upstream `JsSourceConfig.extract` is coupled to the real Rhino runtime, CryptoJS installation, SharedJsScope, BookSource model/GSON adapters, and rule/login/review helper behavior. A thin JSON codec extension cannot reproduce this.
3. Host source catalog currently has no selection-based export API; adding one is a product/workflow change and should be separately registered from script parsing.
4. A standalone JS file is not guaranteed to have a valid `config` or `source` object, required functions, or valid runtime APIs. Tests must use pinned upstream fixtures, not just check that a file ends in `.js`.
5. Existing `mainJs` raw JSON preservation does not prove round-trip equivalence for JS config extraction, nor that all JS metadata is consumed by the runtime.
6. Exact UI parity for import candidate review, timestamp comparison, replacement rules and selected export is a larger workflow task, not included here.

## Recommended next tasks

- First, a separately preregistered, read-only or test-only pinned fixture task can extract a compact set of upstream JS source examples and expected normalized BookSource metadata. Keep arbitrary-script execution out of the current metadata catalog until its security boundary is designed.
- Then separately decide whether to integrate a controlled pinned `JsSourceConfig` runtime into the Runtime-3.0 module boundary or to expose a narrowly typed host port. Do not duplicate a simplified parser.
- Separately register product workflow work for selecting/import-reviewing candidates and exporting selected sources, preserving Badcat UI rather than copying Legado UI.
- Any actual JS import must use the real pinned runtime behavior or a differential-proven equivalent; any standalone JS export must preserve the exact original `mainJs` bytes/text semantics and filename policy.

## Conclusion

Runtime-3.2f.31 closes as a read-only audit. JS source config import and standalone JS export remain **not implemented in the host catalog workflow**. The presence of `mainJs` in the host model is not acceptance evidence. No production/test/Gradle/workflow/UI code, storage format, upstream pin or `main` branch was changed; no build/CI/device test was run for this audit.
