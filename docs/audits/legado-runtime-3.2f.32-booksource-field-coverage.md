# Runtime-3.2f.32 — BookSource model field coverage audit

- Audit branch / start HEAD: `integration/legado-upstream-runtime` / `5518804ad7b2e827212844de410aa878d336d911`.
- Fixed upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Scope: read-only source/model/codec audit; no production/test/Gradle/workflow/UI changes.
- Parallel context: .30 parser exact-SHA CI run #3192 / `45089612632` was active; this audit did not modify the codec or its tests.

## Executive result

The host's `LegadoBookSource` and engine-facing `BookSource` have a typed slot for **all 33 properties** declared in the pinned upstream `BookSource` data class (with explicit aliases for URL/name/group/type and six rule JSON fields). This is a useful model-shape coverage result, not proof of complete semantic compatibility.

Important distinctions:
- The six upstream nested rule models are represented in the host as nullable JSON strings (`rule*Json`), not as the upstream rule model types. Their runtime semantics, converter behavior and all nested fields are not proven by top-level property parity.
- The host keeps an extra `rawJson` field, which preserves unknown metadata but is not a substitute for typed runtime understanding.
- Upstream BookSource inherits `BaseSource`; the host duplicates selected inherited fields (header, jsLib, enabledCookieJar, loginUrl, loginUi, concurrentRate). Interface methods, inherited defaults and lifecycle behavior are not part of the 33 data-class property count.
- Type/default/null/coercion behavior differs in places and needs focused pinned-Gson fixtures.

## Property inventory

All 33 declared upstream properties have a corresponding field in both the host codec DTO and engine-facing projection:

| Upstream property | Host field | Upstream Kotlin default | Mapping |
|---|---|---|---|
| bookSourceUrl | sourceId / id + baseUrl | `""` | typed alias |
| bookSourceName | name | `""` | typed alias |
| bookSourceGroup | group | `null` | typed alias |
| bookSourceType | sourceType | `0` | typed alias |
| bookUrlPattern | bookUrlPattern | `null` | direct |
| customOrder | customOrder | `0` | direct |
| enabled | enabled | `true` | direct |
| enabledExplore | enabledExplore | `true` | direct |
| jsLib | jsLib | `null` | inherited BaseSource property |
| enabledCookieJar | enabledCookieJar | `true` (Room column default annotation is `"0"`) | inherited property; Kotlin/Room default distinction |
| concurrentRate | concurrentRate | `null` | inherited property |
| header | header | `null` | inherited property |
| loginUrl | loginUrl | `null` | inherited property |
| loginUi | loginUi | `null` | inherited property |
| loginCheckJs | loginCheckJs | `null` | direct |
| coverDecodeJs | coverDecodeJs | `null` | direct |
| bookSourceComment | bookSourceComment | `null` | direct |
| variableComment | variableComment | `null` | direct |
| lastUpdateTime | lastUpdateTime | `0` | direct |
| respondTime | respondTime | `180000L` | direct |
| weight | weight | `0` | direct |
| exploreUrl | exploreUrl | `null` | direct |
| exploreScreen | exploreScreen | `null` | direct |
| ruleExplore | ruleExploreJson | `null` | raw JSON representation |
| searchUrl | searchUrl | `null` | direct |
| ruleSearch | ruleSearchJson | `null` | raw JSON representation |
| ruleBookInfo | ruleBookInfoJson | `null` | raw JSON representation |
| ruleToc | ruleTocJson | `null` | raw JSON representation |
| ruleContent | ruleContentJson | `null` | raw JSON representation |
| ruleReview | ruleReviewJson | `null` | raw JSON representation |
| mainJs | mainJs | `null` | direct string; not JS config extraction |
| eventListener | eventListener | `false` | direct |
| customButton | customButton | `false` | direct |

The host DTO's only additional field in this projection is `rawJson`. The engine-facing projection additionally uses host naming (`id`, `baseUrl`) and the same `rawJson` preservation field.

## Serialization and semantic gaps

1. **Rule models:** pinned upstream stores six nullable typed rule models and Room converters serialize/deserialize each through configured GSON. Their custom deserializers accept either JSON objects or JSON primitives containing JSON text; arrays/other shapes return null for that rule. The host stores each rule as a JSON string and has a bounded known-field validator/parser. A string slot does not prove the nested model type/default/null/field coercion or execution behavior is equivalent.
2. **GSON coercion:** pinned `StringJsonDeserializer` maps primitives through `asString`, JSON null to null, and objects/arrays to compact JSON text. `IntJsonDeserializer` accepts numeric primitives and uses `toInt()`; other shapes return null. The host codec is intentionally stricter for many known fields. This is a confirmed semantic difference requiring per-field differential fixtures, not an automatic reason to loosen validation.
3. **Default vs database default:** `enabledCookieJar` Kotlin constructor default is `true`, while its Room `@ColumnInfo(defaultValue = "0")` annotation is `"0"`. Import/GSON defaults, newly constructed model defaults, and existing Room row defaults must not be conflated. The host parser's default must be verified against the specific import pathway, not Room migration semantics.
4. **Identity:** upstream `equals`/hashCode are based on `bookSourceUrl`; host uses `sourceId` as the corresponding identity and separately rejects duplicate IDs in a document. The parser's duplicate policy is a host import constraint and is not established as upstream UI parser parity.
5. **Unknown fields:** host `rawJson` is a preservation advantage for unknown JSON keys through metadata persistence/export, but runtime behavior for such fields is unproven.
6. **Inherited BaseSource behavior:** selected inherited properties are present in the host projection. This audit does not prove interface methods such as source tags/keys, request construction, cookies, script lookup, callbacks or lifecycle equivalence.
7. **Rule execution consumers:** typed projection presence alone does not prove each of the 33 fields is consumed in all upstream execution paths. In particular `eventListener`, `customButton`, review rules, file-source type, and JS-related metadata need caller/behavior-level parity tests.

## Decision and follow-up

- **GO:** retain the existing 33/33 top-level field projection as a useful transport layer; no missing top-level BookSource property was found in the audited data class.
- **NO-GO:** do not claim full BookSource model/runtime parity. The rule submodels, Gson coercion/null/defaults, BaseSource methods, and per-field runtime consumption remain open.
- **Recommended small implementation sequence:** (1) pinned-GSON differential fixtures for scalar/string/int/null/default fields; (2) typed rule model/converter parity for each of six rule classes; (3) field-specific consumer tests for inherited request/login/cookie and event/custom-button/review/JS paths. Each production change requires its own preregistration and exact-SHA validation.
- Do not change Room schema or persistence ownership as part of these codec compatibility tasks.

## Conclusion

Runtime-3.2f.32 closes as a read-only field coverage audit. **33/33 top-level BookSource properties are mapped into the host DTO and engine-facing projection, but nested rule semantics and full upstream behavior are not accepted.** No code, tests, build, CI or device results are claimed.
