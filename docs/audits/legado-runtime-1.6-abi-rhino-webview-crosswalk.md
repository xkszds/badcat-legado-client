# Runtime-1.6 — ABI crosswalk and Rhino/WebView dispatch audit

Date: 2026-11-21

## 1. Scope, baseline and provenance

- Registration: `docs/required/03-LEGADO-RUNTIME.md`, registration commit `9f761fda0bbcb392ec9ab30256c5acb4da86df30`.
- Registered starting code HEAD: `75f6623f60a535d2457d58f69184947a796b9d89`; the only intervening commit before evidence collection was the registration-only checklist commit.
- Upstream behavior baseline: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Official APK: `legado_app_3.26110117_universal_release_vc38521.apk`; 27,857,704 bytes; SHA-256 `31e5a1db1ea47508a1c8193e8e9c1b5aeb49b585317e1e8466d2851e7038e0b3`. Its embedded `META-INF/version-control-info.textproto` revision matches the pinned upstream SHA.
- Host ABI artifact: Actions run #3028 / Run ID `44787339966`, head `25dad001a1ae99fcc0763cfd7eb983ab584e422b`, artifact `legado-host-jvm-abi` / ID `14653689795`. ZIP SHA-256 `707fb4f19f1e70c93f3938c158e884a8b65b5bee6ec6af8530a1c96b0e8a6c4f`; javap report SHA-256 `ee881435ef053c96b7f09b25abafe045e7c1ea7040892408a1e164317284b60c`. Both hashes were recomputed locally and match the prior manifest/GitHub digest.
- The host source blob for `app/src/main/java/app/badcat/client/runtime/RhinoSafeSourceJavaExtensions.kt` is `6a00e649895c4c1d223b768c6b288632ec033de9` both at artifact HEAD `25dad001...` and current registered code HEAD `75f6623...`; therefore the captured class corresponds to the same bridge source currently under audit.
- Host class file: `app.badcat.client.runtime.RhinoSafeSourceJavaExtensions`, classfile major 65 (Java 21), 70 fields, 234 methods; 14 methods carry direct `@JavascriptInterface` annotations. The public/protected declaration scan used for this crosswalk found 156 signatures / 113 distinct method names.
- DEX parser used for this audit reads the DEX string/type/proto/method tables and each class_def class_data encoded method list. For `io.legado.app.help.JsExtensions`, it found 148 defined public methods across 104 distinct names. This is DEX class_data evidence, not a Kotlin source declaration count or a complete Rhino-visible set.

## 2. Descriptor crosswalk: upstream JsExtensions vs host bridge

The crosswalk compares the pinned APK's actual `JsExtensions` DEX method descriptors against the host class-file descriptors from the verified javap artifact. It compares the exact method name plus complete JVM descriptor (parameters and return type), not source spelling or parameter names.

| Result over 148 upstream method entries | Count |
|---|---:|
| Exact descriptor present in host class | 94 |
| Same method name exists, but no exact descriptor match for that overload | 24 |
| No host method with that name | 30 |
| Total upstream method entries | 148 |

Name-level summary: 104 upstream names; 113 host names; 87 names overlap; 17 upstream names are absent by name; 26 host names are host-only. Of the 87 overlapping names, 72 have at least one exact descriptor and 15 have name overlap but no exact descriptor match. Counts are based on DEX-defined JsExtensions methods and the host public/protected javap declarations; they are not a source API completeness percentage.

### 2.1 Upstream method names absent from host bridge

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

### 2.2 Representative exact descriptor and mismatch rows

```csv
method,upstream_Dex_descriptor,host_classfile_descriptor,status
base64Decode,(Ljava/lang/String;I)Ljava/lang/String;,(Ljava/lang/String;I)Ljava/lang/String;,exact_descriptor
createSymmetricCrypto,(Ljava/lang/String;[B[B)Lcn/hutool/crypto/symmetric/SymmetricCrypto;,(Ljava/lang/String;[B[B)Lcn/hutool/crypto/symmetric/SymmetricCrypto;,exact_descriptor
cacheContent,(Ljava/lang/Object;Ljava/lang/String;)Z,(Ljava/lang/Object;Ljava/lang/String;)Z,exact_descriptor_but_behavior_not_equivalent
ajax,(Ljava/lang/Object;Ljava/lang/Long;)Ljava/lang/String;,(Ljava/lang/Object;)Ljava/lang/String; or (Ljava/lang/String;)Ljava/lang/String;,overload_mismatch
ajaxAll,([Ljava/lang/String;)[Lio/legado/app/help/http/StrResponse;,(Ljava/lang/Object;)[Lapp/badcat/client/runtime/RhinoSourceHttpResponse;,parameter_and_return_mismatch
connect,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Long;)Lio/legado/app/help/http/StrResponse;,(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Long;)Lapp/badcat/client/runtime/RhinoSourceHttpResponse;,parameter_and_return_mismatch
get,(Ljava/lang/String;Ljava/lang/Object;)Lorg/jsoup/Connection$Response;,(Ljava/lang/String;Ljava/lang/Object;)Lapp/badcat/client/runtime/RhinoSourceHttpResponse;,return_type_mismatch
getBatchContext,()Lkp/d0;,()Ljava/lang/Object;,return_type_mismatch
getFile,(Ljava/lang/String;)Ljava/io/File;,(Ljava/lang/String;)Lapp/badcat/client/runtime/RhinoSourceFileHandle;,return_type_mismatch
getSource,()Lio/legado/app/data/entities/BaseSource;,()Ljava/util/Map;,return_type_mismatch
getSourceNavigationContext,()Llu/h;,()Lkotlin/coroutines/CoroutineContext;,return_type_mismatch
head,(Ljava/lang/String;Ljava/lang/Object;)Lorg/jsoup/Connection$Response;,(Ljava/lang/String;Ljava/lang/Object;)Lapp/badcat/client/runtime/RhinoSourceHttpResponse;,return_type_mismatch
log,(Ljava/lang/Object;)Ljava/lang/Object;,(Ljava/lang/Object;)Ljava/lang/String;,return_type_mismatch
logType,(Ljava/lang/Object;)V,(Ljava/lang/Object;)Ljava/lang/String;,return_type_mismatch
post,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)Lorg/jsoup/Connection$Response;,(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)Lapp/badcat/client/runtime/RhinoSourceHttpResponse;,return_type_mismatch
timeFormat,(J)Ljava/lang/String;,(Ljava/lang/Number;)Ljava/lang/String; or (Ljava/lang/String;)Ljava/lang/String;,parameter_mismatch
timeFormatUTC,(JLjava/lang/String;I)Ljava/lang/String;,(Ljava/lang/Number;Ljava/lang/String;I)Ljava/lang/String; or (Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;,parameter_mismatch
toURL,(Ljava/lang/String;)Lio/legado/app/utils/JsURL;,(Ljava/lang/String;)Lapp/badcat/client/runtime/RhinoLegadoUrl;,return_type_mismatch
```

An exact descriptor match is only binary-shape overlap. It does not prove the method has the same behavior, state owner, side effects, exception behavior, null handling or script-visible dispatch. `cacheContent` is a concrete counterexample: the descriptors match, but the host implementation throws `IllegalStateException` because the upstream batch-content context/save-token path is not connected. The host `getBatchContext()` returns null, whereas the upstream method returns its batch-context type. The host `getSource()` returns a read-only `Map`, whereas upstream returns the `BaseSource` object.

The host HTTP methods also intentionally use `RhinoSourceHttpResponse` instead of upstream `StrResponse` / Jsoup `Connection.Response`; matching names therefore do not preserve the upstream Java object contract. The crosswalk is a blocker inventory, not a proposed ABI patch.

## 3. Rhino receiver, wrapper and overload dispatch

### 3.1 Pinned upstream wrapper path

Source blobs at the pinned SHA:

| File | Blob |
|---|---|
| `modules/rhino/.../RhinoWrapFactory.kt` | `4a3d8c30329664e725cb64ae89a88a5dfe4ebc6a` |
| `modules/rhino/.../JavaObjectWrapFactory.kt` | `283cff7b6e0d778cd6348a467382765c8cc7c274` |
| `modules/rhino/.../CatchableNativeJavaObject.kt` | `d53d606745c8e31a9feeff96b87c2c835b1dfcaa` |
| `modules/rhino/.../RhinoScriptEngine.kt` | `19951c27c861af6815f43e4756935634135ac1f7` |
| `modules/rhino/.../RhinoContext.kt` | `41344c930e6050248f53dea6eddebd1e0af9da16` |
| `modules/rhino/.../RhinoClassShutter.kt` | `e9033fa47753038751191926a4f898bf5680c05b` |

Direct source findings:

1. `RhinoScriptEngine` enters the custom `RhinoContext`, compiles/evaluates the script, propagates the supplied coroutine context, and restores context/authorization/recursion state in `finally`. The context configures ES6 interpreted execution, class shutter, wrapper factory, instruction observation and stack-depth limits (the detailed constants remain in the pinned engine source).
2. `RhinoWrapFactory.wrapAsJavaObject` checks `RhinoClassShutter.visibleToScripts`, resolves static/runtime `TypeInfo`, applies registered per-class wrapper factories, then wraps List/Map/Array or ordinary objects with catchable Java wrappers.
3. `CatchableNativeJavaObject.get` wraps a returned `NativeJavaMethod` in `CatchableJavaFunction`; the latter selects the Java receiver and calls the wrapped function. It does **not** implement an overload-ranking algorithm itself.
4. `CatchableNativeJavaList.put(index,...)` uses `Context.jsToJava(value, elementType)` for typed list conversion; class/method invocation errors are unwrapped to script runtime exceptions through the catchable wrapper.

The host `legado-rhino` copies the pinned `RhinoWrapFactory.kt`, `CatchableNativeJavaObject.kt`, `RhinoContext.kt` and `RhinoClassShutter.kt` byte-for-byte (their Git blob IDs match). Host `RhinoScriptEngine.kt` is **not** byte-identical to upstream: host blob `84552db27cfff78d52905377fa6774dbc5dea34e`, upstream blob `19951c27c861af6815f43e4756935634135ac1f7`; do not describe the complete Rhino engine module as an unchanged upstream copy.

### 3.2 Overload resolution and JS-to-Java coercion: unresolved boundary

`CatchableJavaFunction` delegates the actual method call to HtmlUnit CoreJS/Rhino `NativeJavaMethod`. The inspected wrapper does not select an overload or define candidate ranking. Typed List element assignment explicitly calls `Context.jsToJava`, but this does not define the conversion policy for every overloaded Java method call.

The pinned source tree includes the HtmlUnit CoreJS dependency JAR, but this audit did not decompile/execute its overload resolver or run a differential JS invocation probe. The official APK DEX also does not preserve Kotlin compiler metadata. Consequently the following remain **UNRESOLVED**:

- overload selection for Number/String/Boolean/null/undefined/array/object arguments;
- primitive vs boxed widening and nullable argument selection;
- return-value wrapping/coercion for overloaded methods;
- Kotlin default-argument bridges, generated overloads and annotation copying;
- whether host bridge overload groups select the same overload for the same JavaScript value.

No overload behavior is inferred from method names or descriptors.

### 3.3 Receiver and global scope mismatch

Pinned upstream `App.initRhino` registers custom wrapper factories for `BookSource`, `RssSource`, `HttpTTS`, rule classes, `BookChapter` and `Book.ReadConfig` (source blob `71b83735b9a41fe0a459fee3a2175fa7518215b8`, lines 244–250). Pinned `JsSourceEngine.buildScope` binds:

- `java = this` (the source engine implementing the extension surface);
- `source = BookSource`;
- `sourceApi = source`;
- `baseUrl = source.getKey()`;
- `cookie = CookieStore`;
- `cache = CacheManager`.

Source blob `44452025f81d8751afee92d3ee1f2d52f9e55905`, lines 104–111.

The host `RhinoSourceScriptRuntime` instead installs a source-scoped `RhinoSafeSourceJavaExtensions` as `java`, a separate host cookie facade as `cookie`, a host login/session facade as `source`, and a host cache facade as `cache` (blob `f1d8f0225a434e79fbd37d0d8c1ec132f5c9541f`, lines 440–468). The inspected host scope does not install upstream `sourceApi` or the upstream `baseUrl` global by default. This is a different receiver/object graph, not a transparent substitution.

## 4. Android WebView registration and exposed method surface

### 4.1 Pinned registration sites

The following source sites directly register `WebJsExtensions` with `addJavascriptInterface`:

| Owner | Source blob | Line(s) | Conditions / companion objects |
|---|---|---:|---|
| `BackstageWebView.kt` | `285f229ff2f9f00e7588b221a8543a2879bafe86` | 120–127 | Rule HTML; source resolved from tag; registers BaseSource, WebJsExtensions and WebCacheManager |
| `WebViewActivity.kt` | `fec705d67831e07f6e25a94d94ede9b000ca088f` | 139–142 | Local HTML with a source; registers WebJsExtensions and WebCacheManager |
| `VideoPlayerActivity.kt` | `55cb088e9aae21ecfd3f5a3a94c33857d32b3b95` | 329–333 | Video source present; registers source and WebJsExtensions |
| `ReadRssActivity.kt` | `f87efffeef8462e66a46874916f1f8fcc6f00999` | 458–462 | Preload JS available; registers WebJsExtensions, RSS source and cache |
| `BookInfoActivity.kt` | `85be4a69b42f26b0d3710730c668dfeb22223c50` | 707–711 | Book source present; registers BaseSource and WebJsExtensions |
| `BottomWebViewDialog.kt` | `5d354092597502bafe26424b3c39cdca667d33f6` | 950–960 | Source present; registers WebJsExtensions, source and cache |

Interface names are randomized at runtime through `WebJsExtensions.Companion.nameJava/nameSource/nameCache`; the JS injection string binds the injected objects to local variables and deletes their original `window` properties.

### 4.2 Class hierarchy, direct annotations and async request dispatch

Pinned source blobs:

- `WebJsExtensions.kt`: `dd6532396b8f02e37376885aa2c6c1e430af1ad9`
- `RssJsExtensions.kt`: `8e01035e8d5802ed931ac46050cd45cfb0254701`
- `JsExtensions.kt`: `f6808edd2a0d312e8bd13649935dea444cb82581`

`WebJsExtensions : RssJsExtensions`; `RssJsExtensions : JsExtensions`. The Web receiver therefore inherits the RSS extension superclass and the interface surface. `WebJsExtensions` directly declares 12 methods annotated with `@JavascriptInterface`: `upConfig`, `request`, `toast`, `longToast`, `log`, `ajax`, `connect`, `get`, `post`, `head`, `getStringList`, `getString`. Five HTTP wrappers use `@JvmOverloads`; their wrapper argument/return shapes differ from the underlying Rhino API (e.g. JSON header string parsing, Int-to-Long timeout conversion, response body/header serialization).

The verified APK DEX has direct method annotation entries on `JsExtensions` (54), `WebJsExtensions` (12), `RssJsExtensions` (64), and `SourceLoginJsExtensions` (0). These are per-class direct DEX annotation counts. They are not a count of the effective method set exposed by a WebView instance. In particular, do not add interface annotations, superclass annotations and generated overloads together as though they were independent exposed methods.

`WebJsExtensions.request(funName, jsParam, id)` is an explicitly dynamic bridge:
- annotated JavaScript entry method receives a string function name, nullable string-array arguments and a callback ID;
- dispatches 16 known names: `run`, `ajaxAwait`, `connectAwait`, `getAwait`, `headAwait`, `postAwait`, `webViewAwait`, `webViewGetSourceAwait`, `decryptStrAwait`, `encryptBase64Await`, `encryptHexAwait`, `createSignHexAwait`, `downloadFileAwait`, `readTxtFileAwait`, `importScriptAwait`, `getStringAwait`;
- launches work in the owning Activity's `lifecycleScope`; required null parameters throw, numeric/boolean optional arguments are parsed from strings, unknown names throw;
- on success, stores the result in `CacheManager` under the callback ID and invokes `window.$JSBridgeResult(id, true)`; on error it stores localized error text and invokes the false callback.

The direct registration and annotated methods are source/DEX evidence. The effective Android WebView reflection behavior for inherited methods and generated overloads was not exercised on a device and is not claimed as verified.

### 4.3 Host-side WebView comparison

The host `RhinoSourceScriptRuntime` explicitly documents that Android Context/WebView APIs remain unavailable to source scripts (source blob `f1d8f0225a434e79fbd37d0d8c1ec132f5c9541f1`, lines 30–37). The audited host runtime constructs Rhino Java/cookie/source/cache facades; it does not register an Android `WebJsExtensions` object through `addJavascriptInterface`. Therefore the six upstream WebView registration paths and their asynchronous callback contract are not implemented by this host Rhino runtime. This is an architectural capability gap, not merely a missing annotation.

## 5. Dynamic entry points and effects

| Entry | Evidence / caller behavior | Effect and limitation |
|---|---|---|
| `JsSourceEngine.buildScope` | source blob `44452025...`, lines 104–111 | binds the six upstream globals; loads source `mainJs`; later named function calls are dynamic source-provided entry points |
| `BaseSource.evalJS` | pinned upstream source; source/login JS text and optional bindings | evaluates dynamic source/login script in source scope; arbitrary source text is open-world |
| `AnalyzeRule.evalJS` | pinned upstream rule engine source | rule-provided JS executes with rule/book/chapter/content context; distinct scope from JsSourceEngine |
| `SharedJsScope.evaluateJsLib` | prior Runtime-1.4 pinned source audit | inline or remote JS libraries are loaded/evaluated; remote contents cannot be statically enumerated |
| `JsExtensions.importScript` | pinned `JsExtensions.kt` | path-based source cache/resource import; target set is source-controlled |
| `WebJsExtensions.request` | `WebJsExtensions.kt`, lines 48–168 | string-dispatched async browser/WebView/network/crypto/file operations; lifecycle cancellation and callback ID flow |
| Host source runtime | `RhinoSourceScriptRuntime.kt`, lines 440–500 | host evaluates compatibility shim, jsLib and mainJs under timeout; request bindings are removed in `finally`; result/error mapped to host EngineResult |

Thread/cancellation evidence:
- Rhino evaluation is synchronous at `eval` entry and has a suspend path (`evalSuspend`); the engine restores coroutine context and execution flags in `finally`.
- WebView request dispatch is asynchronous in Activity `lifecycleScope`, with callback completion through `evaluateJavascript`; an Activity/WebView lifecycle end can prevent callback delivery.
- Host runtime wraps script execution in `withTimeout(timeoutMillis)`, rethrows cancellation, maps timeout and other exceptions into explicit engine failures, and deletes transient bindings in `finally`.
- This is source-level tracing, not a compiler-resolved complete caller graph or a device execution result.

## 6. Acceptance matrix and decision

| Requirement | Result | Reason |
|---|---|---|
| Verified host/upstream artifact provenance | PASS | APK and host ABI hashes verified; host bridge source blob matches artifact HEAD and current code baseline |
| DEX-to-host descriptor crosswalk | PARTIAL | 148 upstream JsExtensions methods classified; 94 exact descriptor overlaps, 24 overload/name-only mismatches, 30 method entries without a host name |
| Semantic parity for exact descriptor overlaps | FAIL / NOT PROVEN | exact descriptor does not prove body/state/exception/coercion; cacheContent is a concrete same-descriptor semantic blocker |
| Rhino wrapper receiver path | PARTIAL | wrapper/catchable dispatch source traced; receiver/global bindings differ from upstream |
| Exact Rhino overload resolution and JS coercion | UNRESOLVED | delegated to HtmlUnit CoreJS/Rhino NativeJavaMethod; no exact resolver analysis or differential probe |
| Upstream WebView registration map | PASS (source inventory) | six registration owners traced |
| Effective WebView inherited/generated exposure | UNRESOLVED | no device reflection test or authoritative Android runtime proof |
| Host WebView bridge parity | FAIL | host runtime intentionally excludes WebView and has no equivalent registered bridge |
| Dynamic source entry points/effects | PARTIAL | known source entry points mapped; arbitrary remote/source-provided JS remains open-world |

**Decision: NO-GO for claiming ABI/semantic compatibility or proceeding to production integration.** The crosswalk confirms meaningful descriptor overlap but also concrete missing/mismatched method contracts. Rhino overload selection remains delegated to an uninspected engine implementation, and Android WebView inherited/generated exposure remains unverified. The host receiver/global graph and WebView capability surface are not equivalent to the upstream runtime.

Next permissible action: separately register a narrow dependency/runtime evidence task to inspect the exact pinned HtmlUnit CoreJS `NativeJavaMethod` implementation and authoritative Android WebView annotation/reflection behavior, or to design explicit differential probes only if a matching, provenance-controlled runtime is available. Do not start a compiler experiment, production bridge rewrite, database/schema work, UI/Reader work, G3 or G4 based on this audit.

## 7. Validation and change log

- Performed read-only source inspection at the pinned upstream SHA and registered Badcat code HEAD.
- Recomputed SHA-256 for the existing host ABI ZIP, javap report and official APK; all match the recorded verified values.
- Parsed DEX class_data method entries and compared descriptors with the existing javap class-file report; no source/build/test code was executed.
- No compilation, Gradle task, test execution, CI run, APK build, device test, live-source test or compiler experiment was performed.
- No production Kotlin/Java, tests, Gradle/workflow, database/schema/migration, persistence, production caller, UI/Reader or `main` was changed.
- Only this audit report and the Runtime checklist result are authorized outputs.
- Runtime-1.6 is **PARTIAL / ACCEPTANCE NOT MET**. G2 remains BLOCKED/partial; B′ and full Legado compatibility remain unpassed.
