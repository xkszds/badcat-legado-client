# Runtime-1.3 — static JS ABI and caller-contract audit

- Registration commit: `4089e70ca8957a4316ef3a4436279aa41d21dafb`
- Audited source HEAD: `4089e70ca8957a4316ef3a4436279aa41d21dafb`
- Branch: `integration/legado-upstream-runtime`
- Pinned behavior baseline: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: read-only source/API/caller audit and documentation. No compilation experiment, tests, Gradle changes or production edits.

## 1. Decision

**Runtime-1.3 is PARTIAL; acceptance NOT MET. NO-GO for a C extraction compiler PoC on this evidence.**

The audit now establishes several concrete behavior gaps and a much clearer set of source execution roots. It does not establish a compiler-resolved call graph, a complete JVM/Rhino ABI, or exhaustive dynamic/reflection/resource/generated entry points. In particular:

- 17 upstream `JsExtensions` public method groups are absent by name from the host bridge.
- Several same-name methods have materially different parameter/return types or semantics.
- Upstream exposes different concrete `java` receiver objects in BaseSource, JsSourceEngine, AnalyzeRule and AnalyzeUrl contexts; the host substitutes a generic allow-listed bridge and does not install the upstream `sourceApi` binding.
- Upstream batch context and cache-save fencing are not connected to the host bridge.
- WebView/browser/verification callbacks and legacy RowUi login are behavior-bearing upstream surfaces, not optional UI decoration.
- The static scan does not prove all source-script dynamic calls, reflection, generated code, resources or compiler-resolved overload selection.

This is not evidence that C is impossible. It is evidence that the next compiler PoC is still premature. Do not freeze port signatures from this report.

## 2. Exact source baseline and inspected blobs

The host tree at the audited HEAD contains 486 entries and is not truncated. Its `upstream/legado` gitlink points to the required exact SHA `330e606c4d57e08adc056ff8ab7a4132c784da9a`. The pinned upstream recursive tree contains 3,298 entries and is not truncated.

### Upstream source roots

| Concern | Pinned path (relative to upstream repository) | Blob SHA |
|---|---|---|
| JS extension interface | app/src/main/java/io/legado/app/help/JsExtensions.kt | f6808edd2a0d312e8bd13649935dea444cb82581 |
| inherited encoding helpers | app/src/main/java/io/legado/app/help/JsEncodeUtils.kt | c75b61650db2abdd64a09d2bd8340ef14c2947b3 |
| regex helper scope | app/src/main/java/io/legado/app/help/RegexJsExtensions.kt | b01d0bbc6b606832b390723269e4dbcbb834de69 |
| WebView JS scope | app/src/main/java/io/legado/app/help/webView/WebJsExtensions.kt | dd6532396b8f02e37376885aa2c6c1e430af1ad9 |
| login JS scope | app/src/main/java/io/legado/app/ui/login/SourceLoginJsExtensions.kt | a14f01b3774998d09619a79ce977fc593b6a28f1 |
| source lifecycle/model | app/src/main/java/io/legado/app/data/entities/BaseSource.kt | 9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa |
| source JS function engine | app/src/main/java/io/legado/app/model/jsSource/JsSourceEngine.kt | 44452025f81d8751afee92d3ee1f2d52f9e55905 |
| shared JS scope | app/src/main/java/io/legado/app/model/SharedJsScope.kt | dab7770d746c851c06ad05c08a35a2866a6100b3 |
| batch context | app/src/main/java/io/legado/app/model/BatchContentContext.kt | 68cad877f048b171e4f0f6cf22b1e0dd1e6481f9 |
| Login UI v2 contract | app/src/main/java/io/legado/app/model/login/LoginUiV2.kt | fd4fca1baa694a821400a46d08ff20d6607f4b96 |
| legacy RowUi model | app/src/main/java/io/legado/app/data/entities/rule/RowUi.kt | d62c08d4928df39d8c035f22067fd58339baf368 |
| rule execution | app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeRule.kt | 7591a026a072ac8a6fe4902add0795812fe0a0a6 |
| URL/request execution | app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt | d898c88dfd844d73f06d9396d0b46f39939f7e6c |
| search/detail/TOC/content orchestration | app/src/main/java/io/legado/app/model/webBook/WebBook.kt | d7a924c2d687bc99d6e25099cefacc9d920e1386 |
| search caller | app/src/main/java/io/legado/app/model/webBook/SearchModel.kt | b190ac7ddc9d14f64f2fa63f1d74bcb8d73e6426 |
| detail caller | app/src/main/java/io/legado/app/model/webBook/BookInfo.kt | e8c062baaccbe7a27b8ea9b9507b6fad036b5ff1 |
| TOC caller | app/src/main/java/io/legado/app/model/webBook/BookChapterList.kt | dd2ae3950cee55a9c8e7880dc2c034d6f4c6f33d |
| chapter content/batch caller | app/src/main/java/io/legado/app/model/webBook/BookContent.kt | cbc353920315db9810f57aec935faeaca5310a13 |
| chapter save/fence | app/src/main/java/io/legado/app/help/book/BookHelp.kt | 78e9e82b67145b835678c7aa1111eaf1631b77d2 |
| source explore extension | app/src/main/java/io/legado/app/help/source/BookSourceExtensions.kt | e81118c3dc32b6c708d5c762f44739a0346f53e5 |
| browser product entry | app/src/main/java/io/legado/app/ui/browser/WebViewActivity.kt | fec705d67831e07f6e25a94d94ede9b000ca088f |
| backstage WebView entry | app/src/main/java/io/legado/app/help/http/BackstageWebView.kt | 285f229ff2f9f00e7588b221a8543a2879bafe86 |
| legacy login UI caller | app/src/main/java/io/legado/app/ui/login/SourceLoginDialog.kt | 391c64dbd5cc3c4064b6b829208d9e832c71b845 |
| v2 login UI caller | app/src/main/java/io/legado/app/ui/login/SourceLoginV2Delegate.kt | 2b5fb5b0e7efb5b1df36ff906a8ce17b672ac41c |

### Badcat source roots

| Concern | Host path at audited HEAD | Blob SHA |
|---|---|---|
| allow-listed JS bridge | app/src/main/java/app/badcat/client/runtime/RhinoSafeSourceJavaExtensions.kt | 6a00e649895c4c1d223b768c6b288632ec033de9 |
| Rhino source runtime | app/src/main/java/app/badcat/client/runtime/RhinoSourceScriptRuntime.kt | f1d8f0225a434e79fbd37d0d8c1ec132f5c9541f |
| production factory/wiring | app/src/main/java/app/badcat/client/runtime/BadcatReadingEngineFactory.kt | e859bd115db06c0bcd777a388bfa2597e920055e |
| Login UI v2 service | app/src/main/java/app/badcat/client/runtime/LegadoSourceLoginUiV2Service.kt | f1aff6cf4a8abc04b995d641277ebcea6c3ff9e2 |
| host Login UI v2 data contract | integration-contract/src/main/kotlin/app/badcat/engine/LegadoLoginUiV2.kt | 612af472720c6feb88a7076bfeb0f6c4230b329c |
| host source orchestration | integration-contract/src/main/kotlin/app/badcat/engine/SourceReadingEngine.kt | 70f1e0036a17b6d582744d612564842831b623f2 |

## 3. JS ABI inventory and concrete gaps

### 3.1 Count caveat

Runtime-1.2 recorded a name-level count of 122 declarations in pinned `JsExtensions` (overloads counted) and 225 declarations in the host bridge. Re-reading the same pinned source with the current line-start scan yields 118 four-space function declaration starts in the interface body; the inherited `JsEncodeUtils` file adds a separate 30 declarations. The existing inventory script and the independent scan do not yet reconcile the 122/118 count. The parser also does not prove JVM descriptors, `@JvmOverloads` synthetic methods, bridges, Rhino exposure, or overload resolution. Therefore these totals are inventory diagnostics, not accepted ABI coverage percentages.

The source files additionally contain separate `RegexJsExtensions` (6 declarations), `WebJsExtensions` (13 declarations) and `SourceLoginJsExtensions` (6 declarations) scopes. They must not be silently folded into the base interface count.

### 3.2 Seventeen missing base method groups

The following upstream public method groups were not found by name in `RhinoSafeSourceJavaExtensions` at the audited host HEAD. Signatures below are source-level Kotlin declarations; they are not JVM descriptor proof.

| Method group | Pinned upstream declarations | Host result |
|---|---|---|
| refreshBookInfo | `()` | absent |
| refreshBookToc | `()` | absent |
| refreshContent | `()` | absent |
| webView | `(html: String?, url: String?, js: String?): String?`; same plus `cacheFirst: Boolean` | absent |
| webViewGetSource | `(html: String?, url: String?, js: String?, sourceRegex: String): String?`; plus `cacheFirst: Boolean`; plus `cacheFirst: Boolean, delayTime: Long` | absent |
| webViewGetOverrideUrl | `(html: String?, url: String?, js: String?, overrideUrlRegex: String): String?`; plus `cacheFirst: Boolean`; plus `cacheFirst: Boolean, delayTime: Long` | absent |
| openVideoPlayer | `(url: String, title: String)`; plus `isFloat: Boolean` | absent |
| startBrowser | `(url: String, title: String)`; plus `html: String?` | absent |
| startBrowserAwait | `(url: String, title: String): StrResponse`; plus `refetchAfterSuccess: Boolean`; plus `html: String?` | absent |
| getVerificationCode | `(imageUrl: String): String` | absent |
| openUrl | `(url: String)`; plus `mimeType: String? = null` | absent |
| showBrowser | `(url: String)`; plus `html: String?`; plus `preloadJs: String?`; full `config: String?` overload | absent |
| getReadBookConfig | `() : String` | absent |
| getReadBookConfigMap | `() : Map<String, Any>` | absent |
| getThemeMode | `() : String` | absent |
| getThemeConfig | `() : String` | absent |
| getThemeConfigMap | `() : Map<String, Any?>` | absent |

These methods are behavior-bearing: they trigger product refresh/navigation, WebView/browser/verification flows, or expose reader/theme state. The absence is not safely fixed by adding empty methods. Badcat must provide equivalent Badcat-owned callbacks or explicitly remain incompatible.

### 3.3 Same-name signature/semantic mismatches

Representative mismatches observed in the source declarations:

| API | Pinned upstream | Host bridge | Consequence |
|---|---|---|---|
| getSource | `BaseSource?` | `Map<String, Any?>` | not the authentic mutable source object or its model methods |
| getBatchContext | `BatchContentContext?` | `Any?`, always `null` | batch rule cannot receive upstream context |
| cacheContent | `(Any?, String): Boolean` | same-looking method, always throws | explicit unsupported behavior; no chapter save |
| ajax | `(Any): String?` and `(Any, Long?): String?` | host accepts nullable/alternate input shapes and returns non-null String | coercion/nullability/result contract differs |
| get/head/post | Jsoup `Connection.Response` | `RhinoSourceHttpResponse` | wrapper type and response methods are not proven equivalent |
| ajaxAll/ajaxTestAll | `Array<String>` inputs and `Array<StrResponse>` outputs, with overloads | `Any?` inputs and `Array<RhinoSourceHttpResponse>` | overload and returned object ABI differ |
| timeFormat | `Long` | `Number` and `String` overloads | accepted input surface differs |
| timeFormatUTC | `Long, String, Int` and nullable String result | `Number/String, String, Int` with non-null String | overload/nullability mismatch |

The host implementation deliberately uses a restricted allow-list rather than unrestricted Java reflection. That is a security boundary, but it is not automatically a compatibility-equivalent replacement. A safe design must implement the exact source-visible semantics through controlled ports.

## 4. Multiple upstream JS receiver contexts

The same inherited `JsExtensions` surface is exposed through multiple concrete receivers:

1. **BaseSource.evalJS** (`BaseSource.kt:414-430`): binds `java`, `source`, and `sourceApi` to the same `BaseSource` instance, plus `baseUrl`, upstream `CookieStore`, and `CacheManager`. Legacy login UI initialization also evaluates JS through this path.
2. **JsSourceEngine.buildScope** (`JsSourceEngine.kt:104-124`): binds `java` to the `JsSourceEngine` instance, while `source` and `sourceApi` are the authentic `BookSource`; it also binds `baseUrl`, `cookie`, and `cache`. During `getContentBatch`, the engine temporarily installs a `BatchContentContext` and clears it in `finally`.
3. **AnalyzeRule.evalJS** (`AnalyzeRule.kt:896-955`): binds `java` to the `AnalyzeRule` instance and also exposes source, book, result, baseUrl, chapter and chapters. Its overrides return the current source, batch context and tag; its `ajax` override delegates through AnalyzeUrl.
4. **AnalyzeUrl.evalJS** (`AnalyzeUrl.kt:395-415`): binds `java` to the `AnalyzeUrl` instance and exposes source, book, baseUrl, page, key and request-related values. It overrides source/tag and manages request Cookie behavior.

Thus the public helper API is context-sensitive. A single generic object with the same method names does not prove equivalent receiver identity, scope, per-call context or state effects.

### Host binding difference

At `RhinoSourceScriptRuntime.kt:452-469`, the host installs:
- `java` = `RhinoSafeSourceJavaExtensions`
- `cookie` = `RhinoSourceCookieExtensions`
- `source` = host login/session extension
- `cache` = host cache extension

The audited host runtime contains no `sourceApi` binding. Its `java.getSource()` returns a copied source descriptor Map, not a pinned `BaseSource`/BookSource object. Host `getBatchContext()` returns null, and `cacheContent()` throws. These are proven source differences, not merely missing tests.

## 5. Static caller and dynamic entry-point map

| Root / path | Statically evidenced caller path | Behavior reached | Evidence class | Remaining unknown |
|---|---|---|---|---|
| Search | `SearchModel.startSearch` → `WebBook.searchBookAwait` → `AnalyzeUrl` request/JS evaluation | search request, checkJs, redirects, Cookie and response parsing | direct source call chain | all source-script dynamic names and external script calls |
| Explore | `BookSourceExtensions.exploreKinds` → `BaseSource.evalJS` | explore URL script, source variables and shared JS | direct source call chain | source-defined dynamic callbacks and library imports |
| Detail | `BookInfo.analyzeBookInfo` → `AnalyzeRule` | init/name/author/kind/word-count/latest-chapter rule evaluation | direct source call chain | JS extension invocations selected at runtime |
| TOC | `BookChapterList.analyzeChapterList` → `AnalyzeRule` + `AnalyzeUrl` | TOC rules, request directives and chapter identity | direct source call chain | dynamic JS helper and reflection edges |
| Content | `BookContent.analyzeContent` → `AnalyzeRule` + `AnalyzeUrl` | content rules, next-page URLs, sub-content and replacements | direct source call chain | dynamically invoked helper overloads |
| Batch content | `BookContent.analyzeContentBatch` → `BatchContentContext` → `AnalyzeRule.setBatchContext` → JS `java.cacheContent` → `BookHelp.saveContent` | batch chapter resolution, token capture, stale-write fence and save | direct source call chain + explicit JS bridge | full runtime context/URL alias and resource commit parity |
| Legacy login | `SourceLoginDialog` constructs `SourceLoginJsExtensions`; callback methods update form data and view; `BaseSource.evalJS` executes inline login UI JS | legacy RowUi fields/defaults/actions, login header/info and browser dialog | direct constructor/callback call chain | all script-specific callback use and external login JS |
| Login UI v2 | `SourceLoginV2Delegate` → `BaseSource.evalLoginUiV2/evalLoginActionV2` → `LoginUiV2` | v2 render/action/state contract | direct source call chain | exact host parity for all v2 edge behavior |
| Browser WebView | `WebViewActivity` and `BackstageWebView` instantiate `WebJsExtensions` and add it as a JS interface | webView, request dispatch, navigation, JS result callbacks and cache | direct WebView injection | every page/script using the injected object and callback timing |

### WebView JS API

Pinned `WebJsExtensions` contains 13 function declarations (12 annotated with `@JavascriptInterface`). It is injected by `WebViewActivity` and `BackstageWebView`. Its `request(funName, jsParam, id)` method dynamically dispatches names including:

- `run`, `ajaxAwait`, `connectAwait`, `getAwait`, `headAwait`, `postAwait`
- `webViewAwait`, `webViewGetSourceAwait`
- `decryptStrAwait`, `encryptBase64Await`, `encryptHexAwait`, `createSignHexAwait`
- `downloadFileAwait`, `readTxtFileAwait`, `importScriptAwait`, `getStringAwait`

This dispatch is string-based and therefore cannot be recovered from ordinary typed call edges alone. The audited Badcat host tree has no equivalent source-runtime WebJsExtensions adapter; the original Badcat WebView/purification side is not proof that these Legado browser callbacks exist.

### Legacy login JS API

Pinned `SourceLoginJsExtensions` contains six public methods: `upLoginData`, `reLoginView`, `refreshExplore`, `copyText`, `clearTtsCache`, and `showBrowser`. `SourceLoginDialog` constructs this extension and implements the callback interface. The host `LegadoSourceLoginUiV2Service` explicitly returns unsupported when the source is not Login UI v2. The host parser's row model also omits upstream `RowUi` fields `chars`, `default`, `viewName`, and `style`. Login UI v2 support is therefore not evidence of legacy RowUi compatibility.

## 6. HTTP, Cookie, state and lifecycle evidence

- Upstream `AnalyzeUrl` uses `CookieStore` and `CacheManager` around request execution; it applies URL-option Cookie precedence and can persist CookieJar state after requests. Its request surface includes method, charset, origin, retry, type, WebView mode, headers, body, timeout, redirect, DNS, JS/body-JS and WebView delay configuration.
- Upstream `BaseSource` owns login header/info, encrypted login info, source variable, arbitrary source key/value storage, explore refresh, JS library refresh and concurrency configuration.
- Upstream `BookContent.analyzeContentBatch` captures a per-chapter `ContentSaveToken`, installs `BatchContentContext`, runs batch JS, closes the context in `finally`, and returns missing chapters for ordinary fallback. `BookHelp.saveContent` checks token identity/version before writing; resource metadata uses a DB transaction.
- Host production factory wires some HTTP and Cookie operations, but those are host transports and host stores, not proven upstream behavior parity. Host defaults for HTTP, Cookie writes, Android ID and Toast explicitly throw if not wired. There is no source-runtime WebView/browser/verification callback in the inspected factory wiring. Host JS evaluation is dispatched on `Dispatchers.IO` under a per-source Mutex and timeout; upstream call contexts use coroutine contexts passed through AnalyzeRule/AnalyzeUrl/JsSourceEngine and navigation-specific contexts. Equivalent dispatcher, reentrancy and cancellation semantics are not proven.

## 7. Machine-readable root/caller/port appendix

The following JSON is a compact evidence register. `direct-source` means an inspected typed source call/override/injection. `dynamic-string` means the edge is selected by a string or source script. `unresolved` must not be treated as a completed port.

```json
[
  {"root":"BaseSource.evalJS","upstream":"BaseSource.kt:414-430","caller":"legacy login initialization; BaseSource lifecycle","binding":"java/source/sourceApi all BaseSource; baseUrl/cookie/cache","host":"RhinoSourceScriptRuntime","candidatePort":"SourceRuntimeSession + ScriptHost","evidence":"direct-source","status":"semantic mismatch; sourceApi absent"},
  {"root":"JsSourceEngine.buildScope","upstream":"JsSourceEngine.kt:104-124","caller":"WebBook JS-source function execution; contentBatch","binding":"java=JsSourceEngine; source/sourceApi=BookSource","host":"RhinoSourceScriptRuntime","candidatePort":"ScriptHost + JsExtensionFacade","evidence":"direct-source","status":"receiver and binding mismatch"},
  {"root":"AnalyzeRule.evalJS","upstream":"AnalyzeRule.kt:896-955","caller":"BookInfo/BookChapterList/BookContent and rule parsing","binding":"java=AnalyzeRule; source/book/result/chapter/chapters","host":"LegadoRuleExpressionDispatcher + RhinoSourceScriptRuntime","candidatePort":"RuleExecutionHost + ScriptHost","evidence":"direct-source","status":"not equivalent receiver/context proof"},
  {"root":"AnalyzeUrl.evalJS","upstream":"AnalyzeUrl.kt:395-415,752-790","caller":"WebBook request/search/detail/TOC/content","binding":"java=AnalyzeUrl; source/book/baseUrl/page/key/cookie/cache","host":"SourceRequestFactory + RhinoSourceScriptRuntime","candidatePort":"RequestTransport + ScriptHost","evidence":"direct-source","status":"request and Cookie parity incomplete"},
  {"root":"JsExtensions.cacheContent","upstream":"JsExtensions.kt:132-154","caller":"BookContent.analyzeContentBatch → AnalyzeRule.setBatchContext","binding":"BatchContentContext + ContentSaveToken","host":"RhinoSafeSourceJavaExtensions.kt:194-205","candidatePort":"BatchContentPort + ChapterBodyStore","evidence":"dynamic-string","status":"unsupported; throws"},
  {"root":"WebJsExtensions.request","upstream":"WebJsExtensions.kt:49-167","caller":"WebViewActivity/BackstageWebView addJavascriptInterface","binding":"funName string dispatch + callback id","host":"no equivalent source-runtime adapter found","candidatePort":"WebViewHost + BrowserHost + VerificationHost","evidence":"dynamic-string","status":"unresolved"},
  {"root":"SourceLoginJsExtensions","upstream":"SourceLoginJsExtensions.kt:21-80","caller":"SourceLoginDialog.kt:69-108","binding":"legacy RowUi callbacks + Activity/browser","host":"Login UI v2 service only","candidatePort":"LoginHost + ProductCallbacks","evidence":"direct-source","status":"legacy behavior unsupported/unproven"},
  {"root":"JsExtensions UI callbacks","upstream":"JsExtensions.kt:157-167, browser/WebView methods","caller":"source rule JS calls by exposed java object","binding":"event/navigation/reader/theme methods","host":"no equivalent callbacks in audited factory","candidatePort":"ProductCallbacks + WebViewHost","evidence":"dynamic-string","status":"17 method groups missing"}
]
```

## 8. Exact unresolved edges and required next evidence

1. Reconcile the 122-vs-118 declaration-start discrepancy and generate a parser-validated complete source declaration inventory for all inherited and separately injected scopes.
2. Capture actual JVM descriptors, synthetic `@JvmOverloads` methods, annotations, inherited members and Rhino-visible properties using bytecode/reflection evidence. This registration did not authorize a compiler experiment, so this remains open.
3. Search all source sets, test fixtures, resource/assets and build-generated configuration for dynamic JS names, reflection/class lookup, WebView injected object names and resource callback keys. The inspected paths provide a starting graph, not an exhaustive graph.
4. Enumerate every public method's direct Kotlin callers and distinguish them from calls reachable only through JS strings or Rhino object exposure.
5. Specify exact nullability, JS coercion, return wrapper/object identity, exception mapping, dispatcher/thread, cancellation, navigation context, lifecycle, and state mutation for every API.
6. Trace source variable, login header/info, Cookie, generic CacheManager TTL/LRU, chapter-body cache, BatchContentContext and SaveToken ownership through process restart and invalidation.
7. Build a compiler/binding-resolved dependency graph and SCC/cut-set proof; this report deliberately does not claim one.
8. Only then freeze typed port signatures and register a disposable compiler PoC. A successful static audit alone does not authorize production integration.

## 9. Validation and actual changes

- Inspected the exact pinned upstream git tree (3,298 entries, not truncated) and exact Badcat tree at the audited HEAD (486 entries, not truncated).
- Inspected the source blobs listed above, including base JS interface, inherited helpers, web/login scopes, runtime roots, source business call chain, batch context/save fence, and host bridge/factory/service.
- No build or compiler experiment was run; no tests were added or executed; no workflow/CI was started; no APK was built or downloaded; no live-source or device verification was performed.
- No source, Gradle, workflow, test, database/schema/migration, persistence, production caller, UI/Reader, release configuration or `main` changed. Only this report and the registered checklist result are within scope.
- Runtime-1.3 acceptance: **NOT MET**. C compiler PoC: **NO-GO**. G2 remains BLOCKED/partial; B′ and full Legado compatibility remain unpassed.
