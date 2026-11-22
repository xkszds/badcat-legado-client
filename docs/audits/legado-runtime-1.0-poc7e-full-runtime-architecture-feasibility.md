# Runtime-1.0 PoC-7e — full Legado source-runtime architecture feasibility audit

- Branch: `integration/legado-upstream-runtime`
- Starting HEAD: `eca91242c1f71dfad756ab1171b09ef81ea73f23`
- Registration commit: `6b2166976e86b41f296d81e73693908faa408e17`
- Fixed upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Task type: architecture/source-build-boundary audit only. No code, Gradle, schema or production changes.

## 1. Executive decision

**Architecture status: BLOCKED — no candidate is yet compiler-proven to host the complete pinned source runtime without importing Legado's product UI/Reader or creating a second persistence authority.**

Recommended investigation direction is **C: a dedicated Android runtime module with authentic pinned source code and explicit host-owned service ports**, but this is only the target to test next, not an accepted architecture and not proof that the existing `:legado-runtime` already achieves it.

Reject these shortcuts:
- B′ (Badcat persistence + current compatibility facade) as the complete runtime: it does not provide authentic live BaseSource/Book/BookChapter, CacheManager, CookieStore, SharedJsScope and full JS service semantics.
- Directly depending on upstream `:app`: it is an Android application module with its own Room database, product Activities/fragments, Reader, UI, resources, WebView and unrelated product features. It would violate the Badcat-only product shell and create a second data authority.
- Blindly copying the 658-file lower-bound closure into `:legado-runtime`: this is not a bounded core extraction and would import broad UI/database/reader/WebView/RSS/media coupling.

C can proceed only if the exact compiler-guided closure can be partitioned without replacing or weakening upstream behavior. If that cannot be demonstrated, the architecture remains blocked and the team must reassess a constrained official model/storage ownership design; it must not lower the compatibility requirement.

## 2. Verified Badcat build graph at the starting HEAD

The complete Badcat tree had 477 entries and was not truncated.

| Module | Current role and verified build facts | Architecture consequence |
|---|---|---|
| `:app` | Android application, compile/target SDK 36, minSdk 26, Kotlin/AGP 2.4.20/8.13.2, JVM 21. Depends on `:integration-contract`, `:legado-runtime`, `:legado-rhino`. | Product shell remains Badcat; runtime code is packaged into the app. |
| `:integration-contract` | Kotlin/JVM 21. Contains host projections/services and a `pinnedLegado` source set compiling selected original parser files from upstream app tree. | Useful for pure parser contracts, not a location for Android BaseSource or full runtime. |
| `:legado-rhino` | Existing dedicated Android library for the pinned Rhino fork. | Reuse candidate for authentic Rhino execution; does not supply JsExtensions/BaseSource or service graph. |
| `:legado-runtime` | Android library, minSdk 26/JVM 21. Its current explicit source inclusion compiles six upstream rule model classes and `GsonExtensions.kt`. README explicitly calls this Runtime-0.1 only. | It is a real module boundary but currently only a narrow rule-model/serialization slice, not the runtime. |
| `:legado-runtime-poc` | Android library probe with Room, annotation, Gson, Hutool/Bouncy Castle, coroutines and Rhino dependencies. It includes only upstream `BookSource.kt` and `BaseSource.kt`. | Prior actual compile probe failed on unresolved app-internal closure; keep as diagnostic evidence, not production runtime. |

Badcat `settings.gradle.kts` includes the above modules only. No upstream `:app`, `:modules:book` or upstream `:modules:rhino` Gradle project is included directly. Badcat app currently declares parser/network dependencies separately and packages the runtime/rhino libraries.

## 3. Verified pinned upstream Gradle and source structure

At the fixed SHA:
- Upstream `:app` is an Android application module (not a library SDK) with compile SDK 36, minSdk 23, target SDK 36, JVM 21, Room 2.8.4 + KSP, Kotlin Parcelize, BuildConfig/resources, app manifest, and large AndroidX/UI dependencies.
- Upstream `:app` directly depends on `:modules:book` and `:modules:rhino`, plus Room, lifecycle, WebKit, AppCompat, Material, LiveEventBus, OkHttp, parsers, crypto, Glide, media/video, protobuf, server, and other app product dependencies.
- Upstream `:modules:rhino` is a relatively isolated Android library (40 source-tree entries) with authentic HtmlUnit Rhino fork, coroutines, OkHttp and AndroidX collection. It is useful and is already represented by Badcat's `:legado-rhino`; it is not the Legado source engine.
- Upstream `:modules:book` has 163 source-tree entries and is principally EPUB/book-format infrastructure. It is not a self-contained source-runtime module and does not resolve BaseSource's app service dependencies.
- The pinned upstream app tree contains 1,078 Kotlin/Java files under `app/src/main` (per prior complete-tree inventory). Including `:app` wholesale would import much more than source execution.

## 4. Authentic model/service coupling that blocks a trivial module split

Direct source inspection at the pinned SHA confirms:
- `BookSource` is both a Room entity and Parcelable and implements live `BaseSource`. It owns all six rule model properties plus login, headers, cookie, source variables and JS library fields.
- `BaseSource` directly calls CacheManager, CookieStore, AppConfig, SharedJsScope, JsSourceEngine, LoginUiV2, concurrency limiter, Android JavascriptInterface and Android thread helpers. It is not a plain interface boundary.
- `Book` is a Room/Parcelable entity but also imports `appDb`, BookHelp, ContentProcessor, AppConfig, ReadBookConfig and reader helpers. It includes reading config and mutable variable/reader state.
- `BookChapter` is a Room/Parcelable entity whose mutation methods write through global `appDb.bookChapterDao`; it also reaches AnalyzeUrl, RuleDataInterface, RuleBigDataHelp, AppConfig, Android app context, regex, URL and content helpers.
- `JsSourceEngine` binds the same real BookSource as both `source` and `sourceApi`, plus `CookieStore` and `CacheManager`; it uses SharedJsScope, BatchContentContext and authentic Rhino bindings.
- AnalyzeRule/AnalyzeUrl further depend on BaseSource, Book/BookChapter, request context, cache, CookieStore, network and WebView behavior.

This coupling is the reason a compatibility facade around descriptor Maps cannot become the full runtime by adding more method names.

## 5. Reconciled compiler and dependency-closure evidence

Prior pinned evidence remains valid and is incorporated into this decision:
- The directed import graph from authentic BookSource.kt + BaseSource.kt reached 658 upstream source nodes across 6,303 internal import edges. Family counts overlap: Android 460, Activity/UI 288, Database 141, Network 84, Rhino/JS 59, Cache 59, Room 54, WebView 32 and Parcelable 27. This is a lower-bound source graph, not a complete compiler closure.
- The actual two-file Android compiler probe configured Gradle/Android resources and compiled the Rhino module, but compilation of the two unchanged upstream source files failed with exit code 1 and 211 diagnostic lines. Missing symbols included app constants/logging, rule types, CacheManager, CookieStore, JsExtensions, SharedJsScope, JsSourceEngine, LoginUiV2, Book/BookChapter and multiple app helpers.
- The prior caller inventory verified BaseSource construction, JS bindings, search/detail/TOC/content, cache/cookie/login/verification/WebView consumers, but explicitly did not claim exhaustive BookSource/BookChapter whole-app caller coverage.
- The current `:legado-runtime` model slice and `:integration-contract` parser slice are genuine reuse assets, but neither closes the source runtime.

References: `docs/audits/legado-runtime-0.7-booksource-basesource-closure.md`, `legado-runtime-0.7-poc1-compiler-diagnostics.md`, `legado-runtime-0.7-poc3-method-service-contracts.md`, and `legado-runtime-0.7-poc4-caller-inventory.md`.

## 6. Architecture candidate comparison

| Candidate | Fidelity potential | Main blocker | Decision |
|---|---|---|---|
| A. Constrained official model/DAO/core ownership | Highest for model/DB semantics if authentic entities and DAO are retained | Book/BookChapter directly depend on upstream appDb and reader/content helpers; official DB migration/ownership and app service closure are large. It does not automatically solve BaseSource/JS/WebView services. | Keep as fallback if C cannot preserve behavior; not yet viable or authorized. |
| B′. Badcat-owned persistence + compatibility facade | Preserves Badcat product data ownership and UI | Current host projections and source-id stores are semantically unequal to live BaseSource, upstream global CacheManager, domain CookieStore, SharedJsScope and real Book/Chapter lifecycle. A facade cannot invent those semantics. | Rejected as a complete Legado runtime route. Existing B′ persistence PoC remains unpassed; do not perform Room migration on this evidence. |
| C. Dedicated Android runtime + authentic upstream source closure + typed host service ports | Can keep Badcat UI/Reader and potentially preserve upstream JS/API semantics while delegating app-specific UI/persistence/network edges to Badcat. | Requires proving exact source closure, service interface seam and storage semantics; pinned upstream uses concrete global app services and direct entity writes. A two-file compile probe already failed. | Preferred investigation target only; blocked until compiler-guided extraction and differential tests prove it. |
| D. Consume upstream `:app` as a dependency | Maximum reuse of upstream app implementation | It is an application plugin/module, not a reusable SDK; imports Legado Activities/Reader/UI/resources and Room DB, risks duplicate app identity/data owner, manifest and resource conflicts. | Reject. Do not include upstream `:app` in Badcat settings. |

### Required module direction if C becomes feasible

`Badcat UI / Reader` → `Badcat application adapters` → `legado-runtime (authentic source runtime)` → `legado-rhino + pinned libraries`.

The runtime must not depend on Badcat Activities, DAO or Reader classes. It can depend on stable, narrow runtime-owned interfaces implemented by Badcat adapters, but those interfaces must preserve upstream contracts and not replace upstream models with shadow DTOs. If authentic upstream classes call static/global app services directly, the extraction must preserve their exact semantics or be rejected; merely wrapping them in similarly named interfaces is insufficient.

Badcat remains the only product UI and reader. Runtime host ports may invoke Badcat-owned navigation/login/verification surfaces, but no Legado Activities, Reader, bookshelf, dialogs or product flows are brought across.

## 7. Ordered feasibility gates (no implementation authorized yet)

1. **G1 — source manifest:** generate a machine-readable list of every source/resource/manifest/Gradle/generated symbol required by BaseSource → JsSourceEngine → AnalyzeRule/AnalyzeUrl → BookSource/Book/BookChapter → CookieStore/CacheManager/SharedJsScope/HTTP/login/WebView. Include all overloads and actual callers; do not use import graph alone.
2. **G2 — compiler partition:** compile unchanged pinned sources in isolated Gradle targets, adding only registered authentic files/dependencies per iteration. Every unresolved symbol must be classified as reusable upstream core, Android platform, product UI, persistence, or generated resource. No fake/stub/no-op types.
3. **G3 — service extraction proof:** prove how each direct concrete/global dependency (appDb, AppConfig, CacheManager, CookieStore, SharedJsScope, AppLog, Activity/WebView, source login/verification, BatchContentContext) can be supplied without altering observable source behavior. Any unextractable direct dependency is a blocker.
4. **G4 — data ownership:** explicitly choose a single writer for BookSource/Book/BookChapter/progress/cache/cookie/login/source variables. Prove authentic model mutation and restart behavior. No dual Room DB writes or implicit split-brain.
5. **G5 — behavior parity:** pinned ABI/overload + serialization/default/null tests; rule corpus differential; JS binding/object-identity tests; cookie/session/cache/login lifecycle tests; recorded HTTP replay; Android lifecycle tests.
6. **G6 — product isolation:** architecture test that runtime has no dependency on Badcat UI/Reader and no Legado UI/Reader product classes are packaged; verify app manifest/resources and APK contents.
7. **G7 — only then authorize production wiring:** register a separate implementation task with exact files/callers/tests. No source/UI/DB/Room work is authorized by this audit.

**Stop condition:** if compiler-guided closure requires broad Legado product UI/Reader or requires replacing live upstream model/service semantics with a facade, C fails. Reassess A or another constrained official-core design; never mark complete compatibility by shrinking the required API surface.

## 8. Result

- Completed: compared A/B′/C/D using exact Badcat and pinned upstream Gradle/module/source facts; reconciled compiler and closure evidence; selected C as investigation target but declared architecture blocked pending G1–G6.
- Actual changes: registration and this audit report only. No Kotlin/Java source, Gradle, tests, DB/schema, persistence, production caller, UI/Reader, APK or `main` changed.
- No tests, build, CI, source-smoke or device result is claimed for this task.
- B′ remains unpassed. Full Legado compatibility remains blocked and is not downgraded.
