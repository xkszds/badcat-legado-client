# Runtime-0.7 PoC-2 source-closure and architecture gate

Date: 2026-11-20

## Inputs and immutable baseline

- Legado: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Badcat source: `integration/legado-upstream-runtime`.
- PoC-1 compile evidence: run `44731457289`, exact HEAD `5a84c0f43add8211e5d7ab8650337fdbf5309a82`; compiler artifact ID `14627060858`, SHA-256 `cad39a76f8d67b7dc65d778353495f6e6a475102c850c9f8eeab94e7f5dc6405`; compiler exit code 1.
- Full 16-seed dependency graph artifact: `legado-runtime-dependency-closure`, ID `14625739928`, SHA-256 `77b19a4e866fabde1480f24307b033c01495c17aa02a1de9c6d685a54c128bce`.

## Source-closure result

The pinned scanner indexes 1,186 upstream source files, resolves all 16/16 declared seeds and reaches 661 source files over 6,303 internal import edges. From the exact `BookSource.kt` + `BaseSource.kt` roots, the directed graph reaches 658 source files.

| Family | Reachable files from BookSource + BaseSource |
|---|---:|
| Android platform | 460 |
| Activity/UI | 288 |
| Database | 141 |
| Network | 84 |
| Rhino/JS | 59 |
| Cache | 59 |
| Room | 54 |
| WebView | 32 |
| Parcelable | 27 |

Families overlap. The graph is still not a compiler-complete closure: same-package references, reflection, generated symbols/resources and Gradle variant wiring are limitations. It is nevertheless corroborated by the PoC-1 compiler errors and direct inspection of the source files below.

## Exact source/build ownership

### Official reusable modules

- `modules/book/build.gradle`: Android library, namespace `me.ag2s`, Java/Kotlin 21, minSdk 21; declared dependency is AndroidX annotation. This is a small helper/library module, not the BookSource/BaseSource/AnalyzeRule runtime.
- `modules/rhino/build.gradle`: Android library, namespace `com.script`; API dependency HtmlUnit Core JS; implementation dependencies coroutines core, OkHttp and AndroidX collection. Badcat already restores this pinned Rhino module in its own build.
- The source model, BaseSource lifecycle, AnalyzeRule/AnalyzeUrl, JsExtensions, CookieStore, CacheManager, login and verification services live under the upstream `:app` source tree rather than a standalone official book-source runtime module.

### Upstream app build ownership

The pinned upstream `app/build.gradle` applies Android application, Kotlin Android/Parcelize, Room and KSP plugins; enables BuildConfig and ViewBinding; and declares app-wide Room runtime/ktx/compiler, AppCompat/Activity/Fragment/Preference, WebKit, lifecycle, Material, media/Media3/GSY video, Splitties app context/system services/views, Gson, OkHttp, Jsoup/JsoupXpath, Hutool, Bouncy Castle and many additional product dependencies. Blindly making the runtime PoC depend on the app project would import the very UI/product/database surface this project must keep out of Badcat.

## Compiler blockers mapped to actual source ownership

| Contract | Pinned source | Why it cannot be treated as a trivial missing import |
|---|---|---|
| Constants/config/logging | `constant/AppConst.kt`, `AppLog.kt`, `AppPattern.kt`, `BookSourceType.kt` | AppConst uses generated BuildConfig and AppVariant; AppLog reaches AppConfig, LogUtils, event/toast helpers and app context. |
| Source rule/model contract | six `data/entities/rule/*Rule.kt` files, `RowUi.kt`, `model/AudioPlay.kt` | These types are serialized and consumed by source rules; RowUi reaches UI style metadata; AudioPlay is a model dependency of BookSource. |
| Book models | `data/entities/Book.kt`, `BookChapter.kt` | Real Room/Parcelable entities; Book reaches app DB, BookHelp, ContentProcessor and reader state; BookChapter reaches DB, AnalyzeUrl/RuleDataInterface, large-variable and Android context helpers. |
| Cache/concurrency | `help/CacheManager.kt`, `ConcurrentRateLimiter.kt` | CacheManager reaches appDb/cache entities and cache utilities; rate limiter references BaseSource and AnalyzeUrl.ConcurrentRecord. |
| JS ABI | `help/JsExtensions.kt`, `JsEncodeUtils.kt`, `help/RegexJsExtensions.kt` | JsExtensions has 94 imports in the source graph and reaches Android WebSettings/AppCompatActivity, browser, media and many helper contracts. It is not a small static helper. |
| HTTP/session | `help/http/CookieStore.kt`, `CookieManager.kt`, `HttpHelper.kt`, `BackstageWebView.kt` | CookieStore reaches appDb and Cookie entities; WebView/network code depends on Android WebView, HTTP clients, navigation and cookie lifecycle. |
| JS runtime lifecycle | `model/SharedJsScope.kt`, `model/jsSource/JsSourceEngine.kt`, `help/rhino/NativeBaseSource.kt` | Uses Rhino bindings, source models, HTTP response handling, shared globals, caches and upstream lifecycle. |
| Login/verification | `model/login/LoginUiV2.kt`, `ui/login/SourceLoginJsExtensions.kt`, `help/source/SourceVerificationHelp.kt` | These are real source login and verification flows with Activity/WebView/dialog callbacks, not safe no-op candidates. |
| Rule execution | `model/analyzeRule/AnalyzeRule.kt`, `AnalyzeUrl.kt`, `help/source/BaseSourceExtensions.kt` | These own actual parsing, URL/HTTP, JS, cookie, cache, batch and source behavior. |

## Architecture comparison

### A — controlled upstream-core extraction

- Fidelity potential: high if behavior-bearing upstream model/rule/engine implementations are retained and only platform calls are moved behind typed host ports.
- Cost/risk: substantial controlled fork; source seam must preserve method signatures, overloads, JS object shapes, callback timing, exception/null behavior, cache/session semantics and lifecycle.
- Boundary: feasible only after method-level dependency/caller mapping identifies the exact service contracts. Not proven by current PoC.

### B — dedicated Android runtime compiling the upstream app closure

- Fidelity potential: highest source reuse if the entire real closure is compiled.
- Boundary result: unacceptable as a production architecture in its naive form. The 658-file closure includes 288 Activity/UI and 141 database family files, with WebView, reader, RSS and media paths. Depending on upstream app database/DAO would also introduce a second persistence authority and conflict with Badcat-owned UI/Reader/data.
- A smaller B source slice is not yet demonstrated.

### C — hybrid authentic engine + Badcat-owned platform services

- Fidelity potential: high if all upstream behavior remains and platform services are implemented as real, contract-complete Badcat adapters.
- Current blockers: BaseSource, JsExtensions, CookieStore, CacheManager, SharedJsScope, JsSourceEngine, AnalyzeRule/AnalyzeUrl and login/verification APIs have extensive service dependencies. Their callback/service seams are not currently isolated.
- This remains the strongest candidate, but **is not selected or passed**. It must not be implemented as a set of simplified same-name classes.

## Gate decision

1. Do not expand `:legado-runtime-poc` by simply adding every unresolved file from PoC-1.
2. Do not make it depend on upstream `:app`, import upstream DAO/database ownership, or bring in Legado UI/Reader.
3. Before another compile source slice, complete a method-level contract map for `BaseSource`, `JsExtensions`, `SharedJsScope`, `JsSourceEngine`, `AnalyzeRule`, `AnalyzeUrl`, Cookie/cache and login/verification. For each method, record callers, state owner, side effects, callback threading/lifecycle, errors and the exact upstream implementation path.
4. Identify which platform calls can be backed by Badcat's existing services without changing their behavior; identify any calls that cannot be faithfully supported. Unknowns remain blockers.
5. Only after that map, register a PoC-3 source/service seam with exact files, dependencies, generated resources, tests and rollback boundary, then compile authentic upstream sources. No production caller migration or DB schema work yet.

## Status

- PoC-1: failed as a two-file boundary, with reproducible compiler evidence.
- PoC-2: source/build closure and A/B/C comparison completed as an audit; no final architecture selected.
- PoC-3: not yet registered for implementation; method-level service contract map is the next gate.
- Full Legado compatibility, B′, production runtime wiring and release readiness remain unaccepted.
- No production Kotlin/Java, Badcat UI/Reader, database, persistence, cache, source callers or `main` were changed by this audit.
