# Runtime-1.0 PoC-7f — G1 source/dependency manifest

- Badcat branch: `integration/legado-upstream-runtime`
- Exact Badcat inventory commit: `16a5752a76b1eb7f6643b5912f7ec21e7c036db5`
- Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Registration commit: `d4dbfd82290cec1b6ab27366441a30f6db3f450f`
- Machine-readable manifest: `docs/audits/legado-runtime-1.0-poc7f-source-dependency-manifest.json`
- Manifest commit: `809053677c5cc5b810f0f522bedc6348b533852b`

## 1. Inventory integrity

Both Git trees were fetched at immutable commits and returned `truncated=false`.

| Repository | Tree entries | Blob files |
|---|---:|---:|
| Badcat | 478 | 364 |
| Legado | 3,298 | 2,836 |

Pinned Legado contains:
- 1,078 Kotlin/Java source files under `app/src/main/java`.
- 773 files under `app/src/main/res` and `app/src/main/assets`.
- The manifest records the exact upstream AndroidManifest paths and blob SHAs.
- Eight root/module Gradle, version catalog, wrapper and properties build-input files are enumerated with blob SHAs.

These are repository inventory counts, not the count of files required by the source runtime.

## 2. Seventeen behavior-bearing source roots pinned by blob SHA

The JSON manifest records each root's exact path, blob SHA, package, source family and every direct import. The initial roots are:

| Source | Role |
|---|---|
| `data/entities/BaseSource.kt` | Live source object, login/headers/variables/lifecycle and JS extension binding |
| `data/entities/BookSource.kt` | Room/Parcelable source model and six rule models |
| `data/entities/Book.kt` | Room/Parcelable book model, DB and reader/content coupling |
| `data/entities/BookChapter.kt` | Room/Parcelable chapter model, DB writes and content/rule coupling |
| `help/JsExtensions.kt` | Public JS host API, HTTP, browser/WebView, file, utility and reader/theme methods |
| `model/jsSource/JsSourceEngine.kt` | Source JS function dispatch and binding construction |
| `model/SharedJsScope.kt` | Shared JS scope/cache/global lifecycle |
| `model/analyzeRule/AnalyzeRule.kt` | Rule parsing/execution |
| `model/analyzeRule/AnalyzeUrl.kt` | URL/request execution, HTTP/cookie/cache/WebView integration |
| `help/http/CookieStore.kt` | Persistent/session/WebView cookie behavior |
| `help/CacheManager.kt` | Typed memory/disk/cache API |
| `ui/login/SourceLoginJsExtensions.kt` | Login host callbacks and Activity lifecycle |
| `help/source/SourceVerificationHelp.kt` | Verification/browser async lifecycle |
| `help/source/BaseSourceExtensions.kt` | Source identity/shared-scope lifecycle |
| `help/http/HttpHelper.kt` | OkHttp clients, cookies, proxy and request stack |
| `help/http/BackstageWebView.kt` | WebView-backed request and navigation host |
| `help/webView/WebJsExtensions.kt` | Separate asynchronous WebView JavaScript bridge |

The manifest includes direct imports and same-basename source candidate paths. Candidate resolution is explicitly a navigation aid: it does not prove a declaration match, call resolution or compilation.

## 3. Verified high-impact dependency families

Direct imports from these roots show that the runtime closure crosses:

- Android framework: Parcelable, JavascriptInterface, WebView, WebSettings, Context/application context, UI thread and Android Base64.
- AndroidX: Room, Parcelize, AppCompatActivity, Lifecycle, Annotation, Collection/LruCache, Media3 and Glide.
- Persistence: `appDb`, Room entities/DAOs, Cache and Cookie tables, app configuration, ACache and file cache.
- JS: authentic Rhino bindings/engine/context, shared scopes, JS-visible host methods, WebView JavaScript bridge.
- Network: OkHttp, request/response/stream types, proxy, Cronet, SSL, redirects, cookies and request concurrency.
- Product services: source login Activity/dialog, verification Activity, browser Activity, confirmation/import Activity, video/player and reader/theme config.
- Utilities/resources/generated code: Gson, Hutool, Bouncy Castle, archive libraries, generated `R`/BuildConfig, Room/KSP and Parcelize output.

This is not a claim that all these families must be copied. G2 must decide which are authentic runtime dependencies and which can be supplied through a behavior-preserving host port.

## 4. Caller inventory anchors

The manifest records 17 existing Badcat caller/implementation anchors, including:
- Import/export and catalog: `FileBookSourceCatalog.kt`, `SourceCatalogActivity.kt`.
- Search/detail/reader: `SourceSearchActivity.kt`, `SourceBookActivity.kt`, `BadcatReadingEngineFactory.kt`.
- HTTP/login/session: `BadcatSourceHttpGatewayFactory.kt`, `LegadoSourceLoginUiV2Service.kt`, `SourceLoginSession.kt`.
- Existing JS host: `RhinoSourceScriptRuntime.kt`, `RhinoSourceCacheExtensions.kt`, `RhinoSourceCookieExtensions.kt`, `SharedPreferencesSourceCookieJar.kt`.
- Contract services: `SourceRuntime.kt`, `SourceSearchService.kt`, `SourceBookDetailService.kt`, `SourceTocService.kt`, `SourceChapterContentService.kt`.

Pinned upstream caller anchors independently verified in the exact tree include:
- Construction/registration: `App.kt`, `NativeBaseSource.kt`.
- Search/detail/TOC/content: `JsSourceBook.kt`, `model/webBook/BookList.kt`, `BookInfo.kt`, `BookChapterList.kt`, `model/webBook/BookContent.kt`.
- Persistence and lifecycle: `SourceHelp.kt`, `CookieManager.kt`, `SourceCallBack.kt`, `JsSourceConfig.kt`, `BookSourceExtensions.kt`.
- Login/verification/browser: `SourceLoginV2Delegate.kt`, `SourceLoginJsExtensions.kt`, `SourceVerificationHelp.kt`, `WebViewModel.kt`, `VerificationCodeActivity.kt`, `WebViewActivity.kt`.

This is a verified caller anchor list, not a complete all-reference call graph. The previous PoC-7c/7d caller searches also explicitly recorded that whole-repository BookSource/BookChapter coverage was not exhaustive.

## 5. Build ownership snapshot

Badcat at the pinned commit includes `:app`, `:integration-contract`, `:legado-rhino`, `:legado-runtime` and `:legado-runtime-poc`.
- `:integration-contract` is JVM-only and compiles a narrow selected upstream parser source set.
- `:legado-rhino` packages the pinned Rhino engine.
- `:legado-runtime` currently includes six authentic rule-model sources and Gson extensions only.
- `:legado-runtime-poc` compiles the two unchanged BookSource/BaseSource roots, which failed previously due to missing app-internal closure.
- The app consumes these modules, but the full source engine is not yet wired.

Upstream build graph:
- `:modules:rhino` is a standalone Android library useful for JS engine reuse, but not the source runtime.
- `:modules:book` is largely EPUB/book-format infrastructure and is not the source-runtime closure.
- Upstream `:app` is an Android application and owns the source runtime services alongside Room/KSP, app configuration, Activities, reader/UI, WebView, media and unrelated product features. Consuming it wholesale violates the product boundary and creates a second persistence authority.

## 6. Reconciliation with previous closure

Previous PoC-0.7 source graph: 658 reachable source nodes and 6,303 internal import edges from BookSource + BaseSource. That graph was a lower bound only. The new G1 manifest adds:
- 17 explicit behavior-bearing source roots (not just two model roots).
- Exact blob hashes and direct import inventories for each root.
- Android resource/asset and Gradle input inventory.
- Badcat and upstream caller anchors.

It does **not** claim recursive compiler closure. Known unmodeled edges remain:
- Same-package references and implicit Kotlin top-level extensions.
- Reflection, class names, service registration and dependency injection.
- BuildConfig, R, Room/KSP, Parcelize and other generated sources.
- Manifest merge, resource qualifiers, assets, JNI/native dependencies and consumer ProGuard rules.
- Transitive dependency variant selection and runtime lifecycle callbacks.

## 7. G1 outcome and G2 entry criteria

**G1 result: partial inventory gate completed; compiler closure not completed.**

The machine-readable JSON is deterministic for the pinned trees and includes root imports, source blob hashes, resource/asset counts, Gradle inputs and caller anchors. It is suitable as an input to G2 but must not be presented as a fully resolved closure.

G2 must:
1. Expand the source graph from these roots with package-aware declaration resolution, same-package references and Kotlin top-level declarations.
2. Include all source callers/implementers for the complete book-source execution path.
3. Add manifests/resources/assets/generated symbols and dependency coordinates/variants.
4. Compile unchanged pinned source in isolated Android modules incrementally; attach each compiler diagnostic to the missing authentic source/dependency or a genuinely external host service.
5. Refuse shadow types, no-op services, API deletion or source rewriting.
6. Stop if closure requires Legado product UI/Reader or a second database authority, and return to architecture review.

## 8. Scope/result statement

- Actual changes: this report, its JSON manifest and category checklist registration/result only.
- No Kotlin/Java runtime source, Gradle, tests, database/schema, persistence, production caller, UI/Reader, APK or `main` changed.
- No tests, Gradle build, CI, source-smoke or device behavior were run/claimed.
- B′ remains unpassed. Full Legado compatibility remains blocked.
