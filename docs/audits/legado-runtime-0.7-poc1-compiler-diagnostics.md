# Runtime-0.7 PoC-1 compiler diagnostics and next closure gate

Date: 2026-11-20

## Immutable execution evidence

- Badcat branch: `integration/legado-upstream-runtime`
- Exact tested HEAD: `5a84c0f43add8211e5d7ab8650337fdbf5309a82`
- Fixed Legado source: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Workflow run: [44731457289](https://github.com/xkszds/badcat-legado-client/actions/runs/44731457289)
- Compiler diagnostic artifact: `legado-runtime-0.7-compiler-probe`, ID `14627060858`, SHA-256 `cad39a76f8d67b7dc65d778353495f6e6a475102c850c9f8eeab94e7f5dc6405`.
- Artifact files: `compiler.log` and `exit-code.txt`; the recorded compiler exit code is **1**.
- The workflow step itself reports success because it intentionally captures expected compiler failure and uploads diagnostics. This is not a successful Kotlin compile.
- At the time of inspection, the overall workflow remained `in_progress` on later best-effort live source probes. The compile-probe step and its artifact upload were already completed successfully.

## What the probe actually established

The isolated Android library configured successfully, Android resource tasks ran, the pinned `:legado-rhino` module compiled, and Gradle reached `:legado-runtime-poc:compileDebugKotlin`. Therefore this was not a Gradle module-registration or Android SDK setup failure.

The two unmodified upstream inputs:
- `upstream/legado/app/src/main/java/io/legado/app/data/entities/BookSource.kt`
- `upstream/legado/app/src/main/java/io/legado/app/data/entities/BaseSource.kt`

did **not** compile as a two-file source boundary. The compiler log contains 211 diagnostic lines, dominated by unresolved upstream internal packages/symbols and cascading type-inference errors. External Android/Room/Parcelize/Gson/Hutool/Rhino dependencies alone are insufficient.

## Compiler blocker families mapped to real pinned upstream sources

| Missing symbol family | Exact upstream source candidates confirmed at the pinned SHA | Boundary consequence |
|---|---|---|
| `constant` | `constant/AppConst.kt`, `constant/AppLog.kt`, `constant/AppPattern.kt`, `constant/BookSourceType.kt` | AppConst reaches BuildConfig/AppVariant; AppLog reaches AppConfig, logging/event/UI helpers. Not just constants. |
| `data.entities.rule` | `data/entities/rule/RowUi.kt` and six BookSource rule models | Rule model types are genuine source contracts; RowUi also references style/model types. |
| `exception` | `exception/NoStackTraceException.kt` (plus ConcurrentException from rate limiter) | Small exception source is separable, but does not resolve the service closure. |
| `help` | `help/CacheManager.kt`, `help/ConcurrentRateLimiter.kt`, `help/JsExtensions.kt`, plus config/crypto/http/source helpers | CacheManager reaches appDb/cache entities; rate limiter references BaseSource and AnalyzeUrl; JsExtensions reaches Android WebView/AppCompatActivity and many app helpers. |
| `model` | `model/SharedJsScope.kt`, `model/jsSource/JsSourceEngine.kt`, `model/login/LoginUiV2.kt`, plus authentic Book/BookChapter | Shared JS and source engine depend on network, cache, rules, models and BaseSource; Book/BookChapter reach app DB/reader helpers; LoginUiV2 is a real login contract, not a no-op. |
| `utils` | `utils/GsonExtensions.kt` and the referenced JSON/string/thread helpers | Requires exact utility functions and serialization behavior, not same-name stubs. |

Direct source inspection confirms important transitive edges:
- `AppConst.kt` imports Android settings/package APIs, Hutool digest, generated `BuildConfig` and `AppVariant`.
- `AppLog.kt` imports `AppConfig`, `LogUtils`, event/toast helpers and `splitties.init.appCtx`.
- `CacheManager.kt` imports `appDb`, Cache entities, QueryTTF and cache helpers.
- `ConcurrentRateLimiter.kt` imports `BaseSource`, `AnalyzeUrl.ConcurrentRecord` and coroutine delay.
- `JsExtensions.kt` imports Android WebSettings and AppCompatActivity and is a broad JS API implementation.
- `CookieStore.kt` imports `appDb` and app logging/pattern contracts.
- `SharedJsScope.kt` imports Rhino bindings, HTTP response helpers and upstream app logging/network contracts.
- `JsSourceEngine.kt` directly depends on authentic BaseSource/BookSource and Rhino bindings.
- `Book.kt` and `BookChapter.kt` are Room/Parcelable entities with further DB, reader and rule dependencies.

This confirms that adding every unresolved import's file is not a safe minimal patch: several of those files immediately pull database, reader, Android UI/WebView and app-wide service dependencies. The 658-node prior graph is consistent with the observed compiler failure.

## Decision and next gate

- PoC-1 result: **two-file boundary rejected by actual compiler evidence**. Do not call Runtime-0.7 passed.
- Do not add shadow models, stubs, fake services, no-op implementations or rewrite the upstream files.
- Do not blindly add the 658-node closure to production or PoC module.
- Next, produce an exact compiler-input manifest grouped into (1) authentic pure model/rule/serialization sources, (2) behavior-bearing JS/rule/network/cache services, and (3) Android app/UI/database platform services. For every boundary crossing, record the full upstream method contract and all callers.
- Then compare a constrained upstream-core extraction with a dedicated Android runtime and a hybrid service seam. A source slice may be expanded only after the exact files, Gradle dependencies, generated resources, service ownership and tests are preregistered in the category checklist.
- Production callers, Badcat UI/Reader, DB/schema, persistence owners and `main` remain untouched.

## Scope limitations

This is a compiler feasibility result for exactly two source inputs plus declared external dependencies. It is not a full source-closure compile, runtime execution, JS ABI pass, source compatibility pass, B′ acceptance or release APK validation.
