# Runtime-1.4 JVM / Rhino exposure and dynamic-dispatch audit

- **Result:** PARTIAL / ACCEPTANCE NOT MET / NO-GO for compiler-guided caller-graph experiment.
- **Branch:** `integration/legado-upstream-runtime`
- **Audit documentation starting HEAD:** `534c7e962a4b3db1e57c86666be336941232e170`
- **Registered code baseline:** `4a10facab44a06f914f70044c412c5dce32e52ff`
- **Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- **Scope:** the preregistered Runtime-1.4 audit only. No source/build/test/Gradle/workflow/production changes.

## 1. Method and evidence policy

This audit separates:
1. Kotlin source declarations;
2. JVM descriptors / generated bridges;
3. Rhino object properties and callable Java methods;
4. Android WebView methods annotated with `@JavascriptInterface`;
5. dynamic JS entry points and their host callbacks.

The declaration inventory below is a reproducible source-line scan of the pinned files: count member declarations whose declaration line begins at four-space class-member indentation and whose modifiers do not include `private`, `internal` or `protected`. It counts overload declarations separately. It is not a Kotlin compiler symbol table and does not establish JVM descriptors or runtime visibility. Multiline signatures are counted by their declaration start.

Evidence labels:
- **D** — direct source declaration or direct call/registration.
- **S** — statically inferred from source control flow; not compiler-resolved.
- **U** — unavailable / unresolved from permitted evidence.

## 2. Reproducible declaration inventory

Pinned upstream source blobs:

| Scope | Source blob | Public member declarations | Distinct declared names | Notes |
|---|---|---:|---:|---|
| `JsExtensions` | `f6808edd2a0d312e8bd13649935dea444cb82581` | 118 | 80 | Interface extends `JsEncodeUtils`; source scan also sees 3 private declarations (121 total starts) |
| `JsEncodeUtils` | `c75b61650db2abdd64a09d2bd8340ef14c2947b3` | 30 | 27 | Inherited by `JsExtensions` and `RegexJsExtensions` |
| `RegexJsExtensions` | `b01d0bbc6b606832b390723269e4dbcbb834de69` | 6 | 6 | Separate replacement-rule helper; inherits `JsEncodeUtils` |
| `WebJsExtensions` | `dd6532396b8f02e37376885aa2c6c1e430af1ad9` | 13 | 13 | 12 direct declarations annotated `@JavascriptInterface`; extends `RssJsExtensions` |
| `SourceLoginJsExtensions` | `a14f01b3774998d09619a79ce977fc593b6a28f1` | 6 | 6 | 0 direct `@JavascriptInterface` annotations; extends `RssJsExtensions` |
| `RssJsExtensions` | `8e01035e8d5802ed931ac46050cd45cfb0254701` | 17 | 17 | 10 direct declarations annotated `@JavascriptInterface`; implements `JsExtensions` |

Machine-readable declaration summary (source declaration starts, overloads included):

```csv
scope,source_blob,public_declarations,distinct_names,javascript_interface_annotations
JsExtensions,f6808edd2a0d312e8bd13649935dea444cb82581,118,80,48
JsEncodeUtils,c75b61650db2abdd64a09d2bd8340ef14c2947b3,30,27,0
RegexJsExtensions,b01d0bbc6b606832b390723269e4dbcbb834de69,6,6,0
WebJsExtensions,dd6532396b8f02e37376885aa2c6c1e430af1ad9,13,13,12
SourceLoginJsExtensions,a14f01b3774998d09619a79ce977fc593b6a28f1,6,6,0
RssJsExtensions,8e01035e8d5802ed931ac46050cd45cfb0254701,17,17,10
```

The base `JsExtensions` declaration names are:

```text
ajax, ajaxAll, ajaxTestAll, androidId, base64Decode, base64DecodeToByteArray,
base64Encode, bytesToStr, cacheContent, cacheFile, connect, deleteFile,
downloadFile, encodeURI, get, get7zByteArrayContent, get7zStringContent,
getBatchContext, getCookie, getFile, getRarByteArrayContent, getRarStringContent,
getReadBookConfig, getReadBookConfigMap, getSource, getSourceNavigationContext,
getTag, getThemeConfig, getThemeConfigMap, getThemeMode, getTxtInFolder,
getVerificationCode, getWebViewUA, getZipByteArrayContent, getZipStringContent,
head, hexDecodeToByteArray, hexDecodeToString, hexEncodeToString, htmlFormat,
importScript, lock, log, logType, longToast, openUrl, openVideoPlayer, post,
queryBase64TTF, queryTTF, randomUUID, readFile, readTxtFile, refreshBookInfo,
refreshBookToc, refreshContent, replaceFont, s2t, showBrowser, singleFlight,
startBrowser, startBrowserAwait, strToBytes, t2s, tick, timeFormat,
timeFormatUTC, toNumChapter, toURL, toast, un7zFile, unArchiveFile, unrarFile,
unzipFile, webView, webViewGetOverrideUrl, webViewGetSource
```

The inherited `JsEncodeUtils` declared names are:

```text
HMacBase64, HMacHex, aesBase64DecodeToByteArray, aesBase64DecodeToString,
aesDecodeArgsBase64Str, aesDecodeToByteArray, aesDecodeToString,
aesEncodeArgsBase64Str, aesEncodeToBase64ByteArray, aesEncodeToBase64String,
aesEncodeToByteArray, aesEncodeToString, createAsymmetricCrypto, createSign,
createSymmetricCrypto, desBase64DecodeToString, desDecodeToString,
desEncodeToBase64String, desEncodeToString, digestBase64Str, digestHex,
md5Encode, md5Encode16, tripleDESDecodeArgsBase64Str, tripleDESDecodeStr,
tripleDESEncodeArgsBase64Str, tripleDESEncodeBase64Str
```

### 2.1 The 122-vs-118 discrepancy

The Runtime-1.3 report recorded that Runtime-1.2 had counted 122 declarations in `JsExtensions`, while an independent line-start scan found 118. Repeating the source-line scan against the same pinned blob again yields **118 public declaration starts**, plus 3 private declaration starts (121 total). The earlier 122 count cannot be reproduced from the preserved report because its original counting script/output is not present there. The 4-count discrepancy is therefore **not reconciled**; 118 is the reproducible public count, not proof that 122 is a valid alternate count. A Kotlin PSI/compiler symbol inventory is required to close this item.

### 2.2 Host declaration comparison

Badcat source baseline:

| Scope | Source blob | Public declarations | Distinct public names |
|---|---|---:|---:|
| `RhinoSafeSourceJavaExtensions` | `6a00e649895c4c1d223b768c6b288632ec033de9` | 150 | 110 |
| `RhinoSourceCookieExtensions` | `49e9520f77e21bf8a36943ffc6b36b8d0eea0e0f` | 5 | 5 |
| `RhinoSourceCacheExtensions` | `4d0c76f1d85a22518f2bdca950c81442854f0762` | 12 | 12 |
| `LegadoSourceLoginUiV2Service` | `f1aff6cf4a8abc04b995d641277ebcea6c3ff9e2` | 3 | 3 |

The `RhinoSafeSourceJavaExtensions` main class scan covers lines 49–2011; helper classes after that range are excluded. Comparing the union of `JsExtensions` and inherited `JsEncodeUtils` source names with this host class gives 104 upstream distinct names, 110 host distinct names, 87 common names, 17 upstream names absent by name, and 23 host-only names. This is a source-name comparison, not an ABI or semantic equivalence claim.

The 17 upstream names absent from the host main bridge are:

```csv
missing_upstream_name
getReadBookConfig
getReadBookConfigMap
getThemeConfig
getThemeConfigMap
getThemeMode
getVerificationCode
openUrl
openVideoPlayer
refreshBookInfo
refreshBookToc
refreshContent
showBrowser
startBrowser
startBrowserAwait
webView
webViewGetOverrideUrl
webViewGetSource
```

The host bridge also exposes helper methods not in the upstream interface, including `getElement(s)`, `parseHtml`, `getVariable`, `putString`, `getStoredString`, `utf8Encode/Decode`, and bounded archive/file helpers. Similar names such as `ajax`, `connect`, `get`, `head`, `post`, `cacheFile`, `getCookie`, and crypto functions require parameter, return, exception and side-effect comparison; name overlap alone is insufficient.

Additional confirmed host gaps:
- `RhinoSafeSourceJavaExtensions.getBatchContext(): Any? = null`.
- Host `cacheContent` throws `IllegalStateException` rather than resolving and saving against the upstream `BatchContentContext`/save-token lifecycle.
- Host `RhinoSourceScriptRuntime` binds `java`, `cookie`, `source`, and `cache`; the `source` object is a login/session facade. The inspected scope does not install upstream `sourceApi` or the upstream `baseUrl` global by default.
- Upstream `JsSourceEngine` binds `java=this`, `source=BookSource`, `sourceApi=source`, `baseUrl=source.getKey()`, `cookie=CookieStore`, `cache=CacheManager`. Upstream `BaseSource.evalJS` installs the same six names. Upstream `AnalyzeRule.evalJS` has a different rule-specific scope (`java`, `cookie`, `cache`, `source`, `book`, `result`, `baseUrl`, `chapter`, `chapters`, `title`, `src`, `nextChapterUrl`, `rssArticle`, `fromBookInfo`, and conditional paragraph/page bindings). These are distinct receiver/scope contracts.

## 3. JVM descriptors and Kotlin compiler artifacts

**No authentic compiled extension class files were present in either inspected Git tree.**
- Pinned upstream tree: 3,298 entries, not truncated; 10 `.class/.jar/.aar` paths. The JARs are Gradle wrapper, Cronet, Rhino engine and HtmlUnit CoreJS dependencies; the tree contains no compiled app extension output for `JsExtensions`, `JsEncodeUtils`, `WebJsExtensions`, `SourceLoginJsExtensions`, `BaseSource` or `JsSourceEngine`.
- Badcat registered tree: 487 entries, not truncated; no `.class/.jar/.aar` artifacts.

The upstream `modules/rhino/lib/rhino-1.7.14.jar` and `third_party/maven/org/htmlunit/htmlunit-core-js/5.3.0-legado.4/htmlunit-core-js-5.3.0-legado.4.jar` are script-engine dependencies, not compiled Legado extension classes. Their existence does not prove app extension descriptors.

Consequently the following remain **U**:
- exact JVM descriptors and return types after Kotlin lowering;
- `@JvmOverloads` generated overloads and whether annotations are copied to each generated method;
- interface default-implementation lowering, inherited method annotations on concrete WebView bridge objects;
- `$default` bridges, synthetic accessors, Kotlin name mangling, nullability annotations and suspend continuation descriptors;
- exact descriptors for Badcat host adapters.

Required authentic evidence: compiled class files plus Kotlin module metadata for the pinned upstream app/runtime classes and the Badcat registered baseline, together with the exact Kotlin compiler version/options and Android toolchain metadata used to produce them. This registration forbids creating those artifacts by a new compilation; obtaining or separately authorizing an existing reproducible artifact is required.

## 4. Rhino runtime and method exposure

### 4.1 Context and wrapper behavior

Pinned source blobs:
- `RhinoWrapFactory.kt`: `4a3d8c30329664e725cb64ae89a88a5dfe4ebc6a`
- `JavaObjectWrapFactory.kt`: `283cff7b6e0d778cd6348a467382765c8cc7c274`
- `CatchableNativeJavaObject.kt`: `d53d606745c8e31a9feeff96b87c2c835b1dfcaa`
- `RhinoClassShutter.kt`: `e9033fa47753038751191926a4f898bf5680c05b`
- `RhinoScriptEngine.kt`: `19951c27c861af6815f43e4756935634135ac1f7`
- `RhinoContext.kt`: `41344c930e6050248f53dea6eddebd1e0af9da16`

Direct findings:
- **D:** `RhinoScriptEngine` configures ES6, interpreted mode, `RhinoClassShutter`, `RhinoWrapFactory`, instruction observer threshold 10,000 and maximum interpreter stack depth 1,000.
- **D:** `RhinoWrapFactory.wrapAsJavaObject` checks `visibleToScripts`, resolves runtime/static `TypeInfo`, and selects registered custom wrappers or special List/Map/Array wrappers before falling back to `CatchableNativeJavaObject`.
- **D:** `CatchableNativeJavaObject`, List, Map and Array wrappers intercept property get/put. Java methods are wrapped in `CatchableJavaFunction`; calls delegate to Rhino's `NativeJavaMethod` implementation after selecting the receiver. Invocation exceptions with the expected reflection wrapper prefix are unwrapped through `Context.throwAsScriptRuntimeEx`.
- **D:** typed List element writes use `Context.jsToJava(value, elementType)`; jsoup Elements can preserve incompatible index overrides in a compatibility path.
- **D:** `RhinoClassShutter` blocks a protected class/type list including `Class`, class loaders, process/file/reflection helpers, Android Context/Activity internals, database internals and selected filesystem types. The policy is implemented as a protected-name/type filter, not a complete allow-list of every class.
- **D:** `RhinoWrapFactory.register` supports class-specific wrapper factories.
- **U:** exact overload winner/conversion ranking is not implemented by the visible wrapper code; calls delegate to HtmlUnit CoreJS/Rhino `NativeJavaMethod`. No source or compiled class evidence for the exact pinned dependency dispatch implementation was inspected here. Do not infer overload selection from Kotlin declarations.

### 4.2 Rhino-visible receivers and globals

| Receiver / entry | Bindings or methods | Caller / lifecycle | Evidence |
|---|---|---|---|
| `JsSourceEngine` | `java=this`, `source`, `sourceApi`, `baseUrl`, `cookie`, `cache`; `JsExtensions` inherited surface | Per-call scope, main JS then dynamic named function call; batch context set only during `getContentBatch` and cleared in `finally` | D |
| `BaseSource.evalJS` | Same six globals; optional caller bindings | Source/login JS evaluation, shared source scope when configured | D |
| `AnalyzeRule.evalJS` | Rule-specific bindings listed in §2.2 | Rule JS can run against book/chapter/content/batch state; per-instance script cache | D |
| `RegexJsExtensions` | Replacement-rule helper plus inherited `JsEncodeUtils` | Separate replacement/purification JS helper | D |
| `SourceLoginJsExtensions` | Passed as `java` by `SourceLoginDialog`; inherits Rss/JsExtensions surface plus six login callbacks | Legacy login UI JS, weak callback reference to dialog | D |
| `WebJsExtensions` | Not a Rhino-global registration; separately injected into WebView | WebView JS interface only | D |

The same upstream `JsExtensions` interface is implemented by different receivers (`JsSourceEngine`, `AnalyzeRule`, `BaseSource` and inherited Rss/login objects). Method behavior therefore depends on receiver state: `getSource`, `getTag`, `getBatchContext`, navigation coroutine context, rule data and login callback ownership are not interchangeable.

## 5. Android WebView exposure — separate from Rhino

### 5.1 Direct registrations

Pinned source:
- `WebViewActivity.kt`: `fec705d67831e07f6e25a94d94ede9b000ca088f`
- `BackstageWebView.kt`: `285f229ff2f9f00e7588b221a8543a2879bafe86`
- `WebJsExtensions.kt`: `dd6532396b8f02e37376885aa2c6c1e430af1ad9`
- `JsExtensions.kt`: `f6808edd2a0d312e8bd13649935dea444cb82581`
- `RssJsExtensions.kt`: `8e01035e8d5802ed931ac46050cd45cfb0254701`

| WebView owner | Registered object | JS interface name | Conditions |
|---|---|---|---|
| `WebViewActivity` | `WebJsExtensions` | randomized `nameJava` | Local HTML with a source |
| `WebViewActivity` | `WebCacheManager` | randomized `nameCache` | Local HTML |
| `BackstageWebView` | `BaseSource` instance | randomized `nameSource` | Rule HTML with a source tag |
| `BackstageWebView` | `WebJsExtensions` | randomized `nameJava` | Rule HTML |
| `BackstageWebView` | `WebCacheManager` | randomized `nameCache` | Rule HTML |

Direct `WebJsExtensions` annotated methods (12): `upConfig`, `request`, `toast`, `longToast`, `log`, `ajax`, `connect`, `get`, `post`, `head`, `getStringList`, `getString`. Several carry default parameters and `@JvmOverloads`.

The `JsExtensions` interface contains 48 source declarations annotated `@JavascriptInterface`; `RssJsExtensions` contains 10 direct annotated declarations. `BackstageWebView` separately registers a `BaseSource` object, while `WebJsExtensions` extends `RssJsExtensions`. Whether inherited interface annotations and compiler-generated overloads appear on each concrete runtime method is **U** until class-file inspection. Do not treat all 118 Rhino source declarations as WebView methods.

### 5.2 String-dispatched browser API

`WebJsExtensions.request(funName, jsParam, id)` dynamically dispatches these string values:

```csv
funName
run
ajaxAwait
connectAwait
getAwait
headAwait
postAwait
webViewAwait
webViewGetSourceAwait
decryptStrAwait
encryptBase64Await
encryptHexAwait
createSignHexAwait
downloadFileAwait
readTxtFileAwait
importScriptAwait
getStringAwait
```

The method launches work in the Activity `lifecycleScope`, runs the selected branch, stores either result or localized error text in `CacheManager` memory under `id`, then invokes `window.$JSBridgeResult(id, success)` through `evaluateJavascript`. Missing required parameters throw; nullable parameters are parsed with `toLongOrNull`/`toIntOrNull` or `toBoolean`. Unknown `funName` throws. This is a dynamic dispatch edge, not an ordinary typed method caller graph.

## 6. Dynamic JS, resource and reflection entry points

| Entry | Dynamic input / behavior | Evidence and status |
|---|---|---|
| `JsSourceEngine.buildScope` | Imported `mainJs`, then generated call expression from function name and argument expressions | D; function names include template `search`, `getChapters`, `getContent`, optional `getBookInfo`, `explore`, review hooks and login `loginUi/loginAction` |
| `BaseSource.evalJS` | Arbitrary source/login JS text plus optional bindings | D |
| `AnalyzeRule.evalJS` | Arbitrary rule JS text with rule/book/chapter/content context | D |
| `SharedJsScope.evaluateJsLib` | Inline JS library or JSON map of absolute remote URLs; downloads missing libraries with `runBlocking`, caches by MD5, evaluates in shared scope | D |
| `SharedJsScope.loadCryptoJs` | Loads bundled `scripts/cryptojs.min.js` asset | D; asset blob `2c7dfcd2359e6130c42ae62a5a32b1f313dea803` |
| JS source template | Declares the expected JS-source functions and globals | D; blob `25ae7673c9f66c2b3959aa370f6c98a4efe3a1e4` |
| `JsExtensions.importScript` | Loads script content by path from source cache/resource handling | D; full dynamic import target set is source-dependent |
| `WebJsExtensions.request` | String-selected function dispatch listed in §5.2 | D |
| Login UI | Inline login JS, legacy RowUi button snippets, V2 `loginUi/loginAction` functions | D |
| Reflection / generated resource configuration | Protected class names and generated resource IDs/configuration are not exhaustively indexed across the whole repository | U |

The source tree contains additional web UI JavaScript bundles, help scripts and upload-book assets; those are not automatically book-source runtime entry points. The audit identifies the script-runtime-specific template and CryptoJS asset above. Imported remote JS libraries and source-provided JS remain open-world inputs; a finite static inventory cannot enumerate their runtime content without reproducible fixtures.

## 7. Method effects, threading, errors and cancellation

| Method family | Upstream effect | Thread / completion / failure behavior | Host status |
|---|---|---|---|
| `ajax`, `connect` | `AnalyzeUrl` request using source, headers/timeouts and navigation coroutine context | Synchronous API; catches request errors and returns stack/error text or `StrResponse`; checks Rhino coroutine cancellation on failure | Host transport is separately adapted; redirect/timeout/error parity not established |
| `ajaxAll`, `ajaxTestAll` | Concurrent URL requests using configured thread count and Flow | `runBlocking(context)`, `mapAsync`, `flowOn(IO)`; returns ordered response array | Name exists, exact behavior and cancellation parity unproven |
| `get/head/post` | Jsoup connection response with optional timeout | Synchronous response API; Rhino cancellation checks around network work | Same-name host overloads differ in parameter/return contracts; not proven equivalent |
| `cacheContent` | Resolve chapter object or unique URL in `BatchContentContext`, apply replacement, use per-index `ContentSaveToken`, save under synchronized lock | Checks coroutine cancellation before resolution/save; closed batch rejects; missing/ambiguous chapter throws; blank content returns false | Host `getBatchContext` returns null and `cacheContent` throws; not implemented |
| `refreshBookInfo/Toc/Content` | Posts product refresh events | EventBus side effect | Missing from host main bridge |
| Browser/navigation/video/verification | Opens browser/WebView/video or verification flow; checks source navigation policy and Activity lifecycle | UI-thread dispatch / Activity state checks; may no-op when UI navigation is blocked or Activity unavailable | Missing host bridge methods |
| `WebJsExtensions.request` | Dynamic branch invokes network, rule, crypto, file or WebView methods | Activity lifecycle coroutine; callback via WebView JS; error text cached and failure callback fired | No equivalent WebView runtime adapter in audited host baseline |
| Legacy login callbacks | `upLoginData`, `reLoginView`, `refreshExplore`, `copyText`, `clearTtsCache`, `showBrowser` | Weak callback reference; UI callback paths; cache clear launches IO and posts UI toast | Host has V2 service only; legacy RowUi/callback flow not equivalent |
| Login UI V2 | Render/action/check JS | Render/action jobs cancellable; script runs on IO; form/state JSON validation; errors surfaced to UI | Host has a service-level V2 path, but no full legacy login or product callback parity proof |
| JS library loading | Remote URL fetch + cache + JS evaluation | Uses `runBlocking` for download; shared-scope cache/locks | Host runtime uses per-source scopes and a compatibility JS shim; upstream shared-scope lifecycle not proven equivalent |

Additional cancellation evidence:
- `RhinoScriptEngine.eval` installs the supplied coroutine context into `RhinoContext`, restores it in `finally`, and exits the Rhino Context in `finally`.
- `RhinoContext.ensureActive` converts coroutine cancellation to `RhinoInterruptError`; instruction observer calls `ensureActive`.
- `RhinoScriptEngine` enforces script-run authorization and recursion count; observed recursion limit is 10.
- `AnalyzeRule` uses per-instance script cache and has long-running `runBlocking/withTimeout(1800000)` flows in the inspected file.
- `JsSourceEngine.callContentBatch` sets batch context and clears it in `finally`; `BatchContentContext.saveContent` serializes the save path under a lock and checks cancellation before save.
- These facts are direct source behavior, not a compiler-resolved complete caller graph.

## 8. Remaining evidence gaps and decision

1. **Declaration discrepancy not reconciled:** 122 historical count cannot be reproduced; current source-line parser gives 118 public declaration starts (121 total including three private starts). Need preserved original inventory script or Kotlin PSI/compiler inventory.
2. **No exact JVM ABI:** authentic extension class files/Kotlin metadata absent; descriptors, bridges, overload annotation copying and interface method lowering unavailable.
3. **No authoritative Rhino overload policy proof:** wrapper delegates to HtmlUnit CoreJS/Rhino `NativeJavaMethod`; exact conversion and overload selection need the pinned dependency implementation or a separately authorized executable artifact analysis.
4. **WebView runtime visibility unresolved:** source annotations and direct registrations are inventoried, but inherited annotation propagation and generated overload reflection require class files.
5. **Caller graph incomplete:** direct source call/registration edges and string dispatch are mapped, but no compiler/binding-resolved call graph, SCC or complete caller set exists.
6. **Dynamic source closure incomplete:** remote JS library contents, arbitrary imported scripts, source-supplied JS, reflection strings and generated resource IDs cannot be exhaustively closed from static source alone.
7. **Method effects are only partially mapped:** transport configuration, CookieStore/CacheManager persistence, UI lifecycle, callbacks, thread dispatch, exceptions and cancellation require method-by-method parity fixtures and/or resolved call graph evidence.
8. **Host parity fails source-name coverage:** 17 upstream method groups absent by name from the host main bridge; `getBatchContext`/ `cacheContent` are explicit semantic blockers; global binding and login/WebView behavior differ.

**Decision: NO-GO.** Runtime-1.4 acceptance is not met. A compiler/binding-resolved caller-graph experiment is not yet justified because the declaration baseline itself remains unreconciled and the exact engine overload/annotation semantics are not grounded. No production integration, database/schema work, UI/Reader work, G3/G4 or APK/device claim is authorized. Any compiler artifact acquisition or experiment needs a separate registration and explicit scope.

## 9. Validation and change log

- Source evidence only; source blobs are pinned above.
- Upstream tree: 3,298 entries, not truncated.
- Badcat registered tree: 487 entries, not truncated.
- No compile, build, test, workflow, APK, live-source or device execution was performed; none is claimed.
- No production source, test, Gradle, workflow, database, migration, UI or Reader file changed.
- This report is the only new audit artifact authorized by Runtime-1.4. The checklist result is updated separately.
