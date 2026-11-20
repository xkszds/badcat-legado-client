# Runtime-1.0 PoC-7i — G2c compiler diagnostic attribution and minimal faithful compile seam

Date: 2026-11-20  
Branch: `integration/legado-upstream-runtime`  
Registration commit: `1ff0d3d9d5f2462e9e76b60ba79a2e1f204db573`  
Code under test: `f3206fcfe8c46db8f7bca6312b7985b44b6697da`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## Decision

**G2 remains BLOCKED.** The failed compiler task is a valid result of the deliberately narrow two-file probe: it compiles unchanged upstream `BaseSource.kt` and `BookSource.kt` without their large in-app source closure. It does not show that the pinned upstream app itself fails to compile, and it does not yet prove that a smaller independent Android runtime closure exists.

The diagnostics are overwhelmingly attributable to omitted authentic source declarations and their package imports, with cascading Kotlin type diagnostics. Those declarations are real, but their dependency families include app database/Room, Android/Parcelable, WebView, login UI, reader/content services, cache/network and Rhino. Therefore adding only a handful of source files or Maven coordinates is not a demonstrated solution. Do not begin G3 service replacement, G4 persistence ownership, production wiring, schema or UI work.

## 1. Exact probe boundary

The CI artifact is `legado-runtime-0.7-compiler-probe`, artifact ID `14635151778`, SHA-256 `5c7219759ef1c5ee3d5e14bf2982d5cadb97d3cbb41225c4824daddbb94a04be`. Its `exit-code.txt` contains `1`. Raw `compiler.log` contains 211 Kotlin diagnostic lines.

The probe's own Gradle file, `legado-runtime-poc/build.gradle.kts`, at tested Badcat HEAD, configures an Android library and includes only these unchanged upstream source files:

- `upstream/legado/app/src/main/java/io/legado/app/data/entities/BaseSource.kt`
- `upstream/legado/app/src/main/java/io/legado/app/data/entities/BookSource.kt`

The task explicitly adds no host or upstream substitute classes. Its declared external dependencies are Room runtime, AndroidX annotation, Gson, Hutool crypto, BouncyCastle, JetBrains annotations, coroutines-android and Badcat's `:legado-rhino`. It does not add the missing upstream `app/src/main/java` declarations listed below.

Pinned source identities:

| File | Git blob SHA-1 |
|---|---|
| `BaseSource.kt` | `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa` |
| `BookSource.kt` | `7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c` |
| upstream `app/build.gradle` | `eb26ab098e63401a3c506fbd21ca8fdc95616aa7` |
| Badcat probe `legado-runtime-poc/build.gradle.kts` | `9b4064771bc46894ef0419f85d51e2f1445a0d9c` |

The upstream app build uses Android application, Kotlin, Parcelize, Room and KSP plugins and app-level configuration/resources. The probe is not compiling that app module or its full source set.

## 2. Diagnostic reconciliation

The raw 211 diagnostic lines reconcile as follows:

| Diagnostic class | Count | Interpretation |
|---|---:|---|
| Unresolved reference | 153 | 35 package/import path-segment diagnostics; 113 real source symbol/property/function references; 5 implicit lambda `it` references that cascade from missing receiver/element types |
| Cannot infer type | 41 | Follow-on type inference failures after missing rule/model/extension declarations |
| Candidate not applicable / receiver mismatch | 10 | Follow-on overload/receiver failures |
| Type mismatch | 2 | Follow-on type failures |
| Return type mismatch | 3 | Follow-on failures (expected String, inferred Any) |
| Argument type mismatch | 1 | Follow-on failure |
| No candidate applicable | 1 | Follow-on overload failure |
| **Total** | **211** | |

The 35 package-path diagnostics are import components, not 35 missing API classes:

| Import path segment | Occurrences |
|---|---:|
| `constant` | 5 |
| `rule` | 7 |
| `exception` | 1 |
| `help` | 9 |
| `model` | 5 |
| `utils` | 8 |
| **Total** | **35** |

These paths are present in the pinned source tree. The probe does not include the package's required source files. The simple names `rule`, `help`, `model` and `exception` also occur as unrelated identifiers elsewhere in the source graph; those name-only matches are not valid resolutions for the import paths.

The remaining 113 unresolved symbol occurrences map to 35 actual declarations/properties/extensions in the pinned source tree. The five unresolved `it` occurrences are lambda implicit parameters whose receiver types cannot be inferred after upstream rule/extension types fail to resolve. They are not five additional APIs.

> Correction to the preliminary checkpoint: a raw graph search found 140 simple-name declaration matches among 153 unresolved occurrences. That is not a sound count of actual symbol resolutions because package path segments can coincidentally match declarations in unrelated packages. The corrected classification above uses the source imports/call sites and exact declaring files.

## 3. Authentic declaration inventory

The following table accounts for all 113 real symbol/property/function occurrences. Hashes are Git blob SHA-1 values from the pinned G2b source graph (artifact `14634703611`, SHA-256 `1b3b430cd6fb39c63588aa5d8ce614579df548baa8ef25b6d4fe6d05247b088b`) and were checked against the exact pinned source tree.

| Pinned source path | Unresolved names (occurrences) | Dependency families recorded by graph |
|---|---|---|
| `io/legado/app/constant/AppConst.kt` — `9865ed0e3425e2990c4ead77f36b227a927c9a17` | AppConst (4) | Android |
| `io/legado/app/constant/AppLog.kt` — `1e6500d1e01e31c3eb9f648bec37e11685c329c2` | AppLog (3) | Android |
| `io/legado/app/constant/AppPattern.kt` — `dc3244b873c6f913f72fd5cf1151814dd1dad2e1` | AppPattern (6), JS_PATTERN (1) | — |
| `io/legado/app/constant/BookSourceType.kt` — `f7c1344290f96bb255a937c9535d3c13b737a6c7` | BookSourceType (1) | Android |
| `io/legado/app/data/entities/Book.kt` — `05024a6c71404eb49c23cd1df0482df365bc63fe` | Book (2) | Android, Room, Parcelable, database, network |
| `io/legado/app/data/entities/BookChapter.kt` — `9798267c77c9e14d0a673a822930491cd6e44cc1` | BookChapter (2) | Android, Room, Parcelable, activity/UI, database, network |
| `io/legado/app/data/entities/rule/BookInfoRule.kt` — `54fd639003b4b4780d095c5afa379eac25a8620b` | BookInfoRule (5) | Android, Parcelable |
| `io/legado/app/data/entities/rule/ContentRule.kt` — `74020cd33c89dc0af189bae2ed68afd2c52d1446` | ContentRule (5), contentBatch (1), maxBatchSize (2) | Android, Parcelable, Rhino/JS |
| `io/legado/app/data/entities/rule/ExploreRule.kt` — `6871515ea685dc4cb2245ecc45c952604634267d` | ExploreRule (5) | Android, Parcelable |
| `io/legado/app/data/entities/rule/ReviewRule.kt` — `2d56d4aa7766d5c8f82366640d3273bd02a86f92` | ReviewRule (3) | Android, Parcelable |
| `io/legado/app/data/entities/rule/RowUi.kt` — `d62c08d4928df39d8c035f22067fd58339baf368` | RowUi (1) | activity/UI |
| `io/legado/app/data/entities/rule/SearchRule.kt` — `ba8cda4543a4afb507224ba57f0f1f5ab4ad5594` | SearchRule (5), checkKeyWord (1) | Android, Parcelable |
| `io/legado/app/data/entities/rule/TocRule.kt` — `f525e912340257125059b6ad87d0cf7e73d6a1dc` | TocRule (5) | Android, Parcelable |
| `io/legado/app/exception/NoStackTraceException.kt` — `03e469283a3f9be711ebf08846d067a4691524b5` | NoStackTraceException (2) | — |
| `io/legado/app/help/CacheManager.kt` — `9575697d8ed60c0864aa3fd52d548ba059054205` | CacheManager (14) | Android, WebView, database, cache, Rhino/JS |
| `io/legado/app/help/ConcurrentRateLimiter.kt` — `312ab0c8e0952a40d4f6997e898efe4e57f85d17` | updateConcurrentRate (1) | — |
| `io/legado/app/help/JsExtensions.kt` — `f6808edd2a0d312e8bd13649935dea444cb82581` | JsExtensions (1) | Android, WebView, activity/UI, network, Rhino/JS |
| `io/legado/app/help/config/AppConfig.kt` — `4a6ac37e3c1ce33c036af28c63395e07c261a1eb` | AppConfig (1) | Android, database |
| `io/legado/app/help/crypto/SymmetricCryptoAndroid.kt` — `c98eb68d4f067edc5221c800cfb8701fd8a1f77b` | SymmetricCryptoAndroid (1) | Android |
| `io/legado/app/help/http/CookieStore.kt` — `09720d79f7009db052c080b54fc5fa2805257b64` | CookieStore (3) | Android, database, cache, network |
| `io/legado/app/help/source/BaseSourceExtensions.kt` — `4b70e9848dde0c3adad44f8cc5147a2abe128cb1` | getShareScope (1), getSharedGlobalStateKey (1) | Rhino/JS |
| `io/legado/app/help/source/BookSourceExtensions.kt` — `e81118c3dc32b6c708d5c762f44739a0346f53e5` | clearExploreKindsCache (1) | activity/UI, cache, Rhino/JS |
| `io/legado/app/model/SharedJsScope.kt` — `dab7770d746c851c06ad05c08a35a2866a6100b3` | SharedJsScope (1), remove (1) | Android, cache, network, Rhino/JS |
| `io/legado/app/model/jsSource/JsSourceEngine.kt` — `44452025f81d8751afee92d3ee1f2d52f9e55905` | JsSourceEngine (4) | Android, cache, network, Rhino/JS |
| `io/legado/app/model/login/LoginUiV2.kt` — `fd4fca1baa694a821400a46d08ff20d6607f4b96` | LoginUiV2 (1) | activity/UI |
| `io/legado/app/utils/GsonExtensions.kt` — `c0bebcb9bf072f04a680e269765bb5b2e76cd88d` | GSON (19) | — |
| `io/legado/app/utils/HandlerUtils.kt` — `5da7da42574bf63a4bed4b6f575005b5fdc7d8db` | isMainThread (2) | Android |
| `io/legado/app/utils/MapExtensions.kt` — `d035b2a7395e346967d777ae94f29d5dd09f2fa7` | has (1) | — |
| `io/legado/app/utils/StringExtensions.kt` — `694fe6d8f3a815261e034c1341109aae1b18bf2c` | splitNotBlank (6) | Android |

## 4. Why these declarations do not form a tiny independent model bundle

The direct import/call inspection reveals several dependency clusters:

1. **BaseSource is a live service contract, not a DTO.** It implements `JsExtensions`, exposes login/header/cookie/cache/source-variable APIs, evaluates JS with `java`, `source`, `sourceApi`, `cookie` and `cache` bindings, obtains shared JS scope/global state, applies concurrency limits and clears discovery caches. Replacing these with no-op methods or a reduced interface would violate the compatibility requirement.
2. **BookSource is coupled to rule models and runtime behavior.** Its getters instantiate Search/Explore/BookInfo/TOC/Content rules; it uses rule fields for keyword checking and content batching, group parsing and source type annotations. The five rule models are Parcelable and Gson-serialized; their fields/defaults and JSON behavior must remain authentic.
3. **Book and BookChapter are not portable DTOs.** The pinned Book source imports `appDb`, BookHelp/ContentProcessor, AppConfig, ReadBook and reader/book helpers. BookChapter imports `appDb`, RuleBigDataHelp, AnalyzeUrl/RuleDataInterface, Android app context and update helpers. Their graph families include Room/database, Parcelable, Android, network and activity/UI.
4. **CacheManager is tied to app persistence.** Its source imports `appDb` and the Room `Cache` entity, in addition to memory LRU, disk/file cache and WebView-facing behavior. A replacement backed only by a source-scoped map would not be equivalent.
5. **CookieStore is tied to the official cookie/database pipeline.** It imports `appDb`, the `Cookie` entity, CacheManager, CookieManager, NetworkUtils and cookie extensions; the source writes through `appDb.cookieDao` and updates memory cache.
6. **JS extensions and JS execution pull app services.** JsExtensions imports Android WebSettings/AppCompat, BackstageWebView, cookie/HTTP helpers, SourceHelp/SourceVerificationHelp, BookHelp and UI activities/dialogs. JsSourceEngine and SharedJsScope depend on Rhino, network/cache, Android context/assets and global-scope lifecycle.
7. **Login is not just a JSON data class.** BaseSource references LoginUiV2 and RowUi; LoginUiV2 uses Gson to validate/render version-2 login descriptions. RowUi includes view/style-related structure and is marked activity/UI in the source graph.

This confirms that a compiler-clean source closure must account for behavior-bearing app services, not merely add the 35 declaration files. The graph artifact reports 1,085 reachable files, 152,175 resolved lexical/import candidate edges, 25,950 ambiguous same-package references and 906 generated/dependency-classified unresolved imports. Lexical same-package edges are candidates, not compiler bindings; the graph is not a compiler-verified closure.

## 5. Minimal seam comparison

| Candidate | Evidence-based status |
|---|---|
| Add only the 35 declaration files | **No-go.** Many files have their own transitive appDb, Room, reader, UI, WebView, network and lifecycle dependencies. |
| Add only more Maven dependencies to the current probe | **No-go.** Most failures are same-app source declarations and service behavior, not external artifacts. |
| Depend directly on upstream `:app` | **No-go.** It is an Android application with Room/KSP and Legado UI/Reader/product service ownership; this violates the no-Legado-UI/Reader and no-second-database boundary. |
| Reuse `:modules:rhino` only | **Insufficient.** It supplies the JS engine, not BaseSource services, models, cookies, cache, login, HTTP/browser or source lifecycle. |
| Dedicated Android Runtime with authentic source plus typed, behavior-preserving ports | **Still the candidate to investigate, not proven.** The port surface must cover all actual upstream calls and retain login, cookie/cache, navigation, HTTP/WebView, JS scope and lifecycle semantics. |
| Import official core models/DAO under a constrained data owner | **Fallback only.** Requires separate architecture decision and single-writer/transaction/migration proof; not authorized by this audit. |

## 6. G2c outcome and next gate

**Outcome: diagnostic attribution complete; compiler closure not complete.** The probe failure is explained by its source selection, but the authentic source dependency closure reaches substantial upstream app behavior. No source or build changes are authorized by this result.

Recommended next registration: **G2d bounded incremental compile-closure experiment**, limited to an isolated non-product Android probe. It should add authentic source groups in dependency order, record each exact source/blob/build input, and stop at the first dependency that would require importing Legado UI/Reader, duplicating the upstream database, or substituting behavior with a stub. A successful slice must compile unchanged upstream declarations and run contract fixtures; it must not be described as the complete runtime. If the smallest faithful closure cannot be isolated, reopen the architecture comparison rather than weakening compatibility.

## 7. CI and live-source evidence

CI run: [44750305489](https://github.com/xkszds/badcat-legado-client/actions/runs/44750305489)  
Tested code HEAD: `f3206fcfe8c46db8f7bca6312b7985b44b6697da`  
Overall workflow/job conclusion: success.

- Scanner self-test, graph generation, contract tests, authentic model contract tests, Rhino host tests and Debug APK assembly/upload completed.
- JUnit artifact `14635605863`, SHA-256 `43aa3f3132a7f492d064d6af41ac2e708fea6eeda8a7c85f1af432a1f8c73c5`: 296 tests, 0 failures, 0 errors, 0 skipped.
- Debug APK artifact `14634713747`, SHA-256 `0d06b97f26792131f409a92408102b06c94b44916539847de63009ee67eeddba`: CI assembly/upload only; not release and not device verified.
- Compiler probe artifact `14635151778`: compile exit code 1 as detailed above. The workflow step succeeds because it captures this result.
- Uploaded-corpus live report artifact `14635792104`, SHA-256 `29b892d5c0899a935fed4181a9f71bc5b297ab9bfc1683d5f3f0d4f03a3f3e4f`: 1/4 candidates completed search → detail → TOC → chapter content. Three failed before engine evaluation (two timeouts and one HTTP 404); do not count them as engine failures or passes.
- Feisuzhong and Pinshuzhai live E2E steps completed; these remain bounded site samples, not proof of all-source compatibility.

## 8. Change and exclusion record

- Registration: `1ff0d3d9d5f2462e9e76b60ba79a2e1f204db573`.
- This audit adds only this report and the corresponding category-3 checklist result.
- No Kotlin/Java production source, Gradle/workflow configuration, database/schema, persistence, production caller, UI/Reader or release artifact was modified.
- No device verification, full Legado compatibility, B′ PoC pass or release readiness is claimed.
