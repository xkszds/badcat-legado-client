# Runtime-1.7 — NativeJava overload and Android WebView platform audit

Date: 2026-11-21

## 1. Scope and evidence boundary

- Registration: `docs/required/03-LEGADO-RUNTIME.md`, Runtime-1.7 registration commit `386a967d6c2444c749bc916aae69b9bdcf45006f`.
- Starting code HEAD: `96dbe9df112f2e2b2b017949158be27e4b0fc40c`; registration-only commit is the only intervening change before this audit.
- Fixed Legado source baseline: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Upstream dependency declaration: `gradle/libs.versions.toml` blob `88170041bfea4b98b3269955f5db3704691134aa` declares `htmlunitCoreJs = "5.3.0-legado.4"` and `htmlunit-core-js = { module = "org.htmlunit:htmlunit-core-js", version.ref = "htmlunitCoreJs" }`.
- Checked-in dependency: `third_party/maven/org/htmlunit/htmlunit-core-js/5.3.0-legado.4/htmlunit-core-js-5.3.0-legado.4.jar`, Git blob `d240cdd66f5269ed33da53cde7e4e690cee6e162`, 1,900,654 bytes. Adjacent POM blob `ec4125a89902f4ade16a07d8fc201150e92c3e8c` identifies group `org.htmlunit`, artifact `htmlunit-core-js`, version `5.3.0-legado.4`, and URL `https://github.com/mgz0227/htmlunit-core-js`. The binary blob could not be decoded by the repository connector or downloaded in this audit; therefore no SHA-256, classfile hash, or exact source-to-binary comparison is claimed.
- `modules/rhino/build.gradle` blob `a1de2686a5a511bee3c6d88ccbc0302da8c1be38` exposes this dependency as `api libs.htmlunit.core.js`.
- App build blob `eb26ab098e63401a3c506fbd21ca8fdc95616aa7` sets `minSdk 23` and `targetSdk 36`.
- Public upstream source reference inspected for the engine algorithm: `HtmlUnit/htmlunit-rhino-fork@46d0904a3b4a1adc014a0d53d66a91b699a548de` (tag `5.3.0`). This is the public upstream 5.3.0 source baseline, **not proven byte-for-byte identical** to Legado's custom `5.3.0-legado.4` JAR. The suffix and checked-in artifact provenance are real; the exact .4 source delta is not established here.
- Relevant public source blobs at tag 5.3.0:
  - `NativeJavaMethod.java`: `8a90c70200c06b7bca05549a51e4a3dc1c742272`
  - `NativeJavaObject.java`: `37eaefe026f7cd91cdc006de8cb27013fd52da6e`
  - `WrapFactory.java`: `86e2731c03c8a4e95028170866c8e4b998dd5676`
- Pinned Legado wrapper source: `modules/rhino/src/main/java/com/script/rhino/CatchableNativeJavaObject.kt`, blob `d53d606745c8e31a9feeff96b87c2c835b1dfcaa`; the wrapper imports `org.htmlunit.corejs.javascript.NativeJavaMethod` and delegates `Function.call` to that object, rather than implementing overload ranking itself.

## 2. HtmlUnit/Rhino overload resolution — public 5.3.0 source model

The following describes the inspected public tag 5.3.0 source. Because the pinned Legado binary is `5.3.0-legado.4` and its bytecode was not decoded/compared, these rules are a strong candidate model, not yet a verified exact-binary statement for the pinned APK.

### 2.1 Dispatch path

1. `NativeJavaMethod.call` invokes `findCachedFunction(cx, args)` before invoking the selected executable.
2. For overloaded methods, `findCachedFunction` checks a bounded cache keyed by the runtime argument type classes; on a miss it calls `findFunction`.
3. `findFunction` filters candidates through `failFastConversionWeights`. Non-varargs candidates require exact arity. Varargs candidates require at least the fixed parameter count; trailing argument positions use the final varargs element type during preference comparison.
4. Applicable candidates are pairwise compared by `preferSignature`. Lower conversion weight wins per argument. Equal non-trivial weights can be broken by assignability/specificity; otherwise the comparison is ambiguous. Conflicting preferences across arguments leave multiple best-fit candidates and produce a method-ambiguous runtime error. It is not the Android WebView same-arity arbitrary-selection rule.
5. The chosen executable wraps arguments through `wrapArgsInternal`, resolves the Java receiver by unwrapping a `Wrapper` in the `this`/prototype chain, invokes the method, then wraps the return value through the configured `WrapFactory`.

### 2.2 Conversion-weight evidence

The inspected `NativeJavaObject.getConversionWeight` has explicit JS-value categories and type weights. Key branches:

| JavaScript-side value | Candidate Java type | Source-level ranking / applicability |
|---|---|---|
| `undefined` | `String` or exact `Object` | trivial weight 1; other types rejected |
| `null` | non-primitive | trivial weight 1; primitive rejected |
| Boolean | primitive `boolean`, boxed `Boolean`, `Object`, `String` | weights 1, 2, 3, 4 respectively |
| Number / BigInt | primitive numeric | double is first; other numeric primitives use size ranks; boolean rejected |
| Number / BigInt | `String`, `BigInteger`, exact `Object`, other Number type | weights 9, 10, 11, 2 respectively |
| String | `String`, compatible instance, `char`, numeric primitive | weights 1, 2, 3, 4 respectively |
| Wrapped Java object/array | assignable target, `String`, primitive numeric | assignable uses non-trivial weight 0; String 2; numeric conversion uses size rank; Java array to primitive is rejected |
| JS object / NativeArray | assignable non-Object type, Java array, exact `Object`, `String` | weights 1, 2, 3, 4; additional Date/interface adaptation branches exist |

Primitive numeric size ranks in this source are double=1, float=2, long=3, int=4, short=5, char=6, byte=7; boolean is not a numeric conversion target. Interface adaptation gives JS functions a better rank than NativeObject; other JS objects have a later fallback rank.

These are resolver weights, not a complete promise that every candidate can be invoked. The selected candidate then passes through `coerceTypeImpl`, which can still reject conversion or construct interface adapters/arrays.

### 2.3 Coercion behavior after selection

The inspected `coerceTypeImpl` source explicitly shows:
- null to reference types is null; null to primitive throws conversion error;
- undefined maps to literal string `"undefined"` for String/Object, otherwise conversion error;
- JS Boolean only converts to boolean/Boolean, Object, or String; it does not become a numeric primitive;
- Number/BigInt can convert to String, numeric/character targets, and Object (including an optional integral-number formatting feature);
- JS String converts to String/compatible CharSequence, a single-character char, or numeric/character targets; invalid conversions throw;
- Java wrappers are unwrapped and checked against the requested type; primitive numeric coercion is supported, boolean coercion is not;
- NativeArray can be converted element-by-element to Java arrays;
- NativeDate can convert to Date;
- JS functions/objects can be adapted to Java interfaces under the source's stated constraints.

The exact pinned Legado .4 binary must still be inspected to prove that its implementation retains these exact branches and that any Legado patch has not changed them.

## 3. Android WebView Java bridge — authoritative current contract

### 3.1 App API-level boundary

Pinned upstream `app/build.gradle` sets `minSdk 23`, `targetSdk 36`. Android's public `WebView.addJavascriptInterface` API documents that for apps targeting API 17/Jelly Bean MR1 or later, only public methods annotated with `android.webkit.JavascriptInterface` can be called by JavaScript. Since this app's minimum supported API is 23 and target is 36, the pre-API-17 legacy all-public-method rule is outside this app's supported device range. Public API reference: `https://developer.android.com/reference/android/webkit/WebView#addJavascriptInterface(java.lang.Object,java.lang.String)`.

### 3.2 Inheritance and overriding

Android's current native-bridge guide explicitly states:
- only explicitly annotated public methods are exposed;
- non-overridden public methods inherited from a superclass remain accessible when the superclass declaration has `@JavascriptInterface`;
- if a subclass overrides an annotated superclass method, the override must itself be annotated; the annotation does not transfer to the override;
- private/protected methods are not exposed, and standard reflection entry points such as `getClass()` are blocked.

Official source: `https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge`, section “Method and member accessibility”.

Independent Chromium Java Bridge instrumentation test source at commit `2c59a5e233bffe4b4f1d12cde0b4650d9a751884`, file `content/public/android/javatests/src/org/chromium/content/browser/JavaBridgeBasicsTest.java`, exercises both `testPublicInheritedMethod` and `testAnnotationDoesNotGetInherited`. The latter annotates `Base.base()`, overrides it in `Child` without the annotation, injects Child with `JavascriptInterface.class`, and asserts the method is unavailable. This is implementation test evidence, not a test run performed for this repository.

### 3.3 WebView overload and value-conversion rules differ from Rhino

The current Android native-bridge guide states that overloaded methods are resolved by **argument count only**, not argument types; when multiple overloads have the same argument count, one is chosen arbitrarily. Invalid argument counts raise a JS exception. Chromium's Java Bridge test at the commit above verifies 0/1/2-argument overload dispatch and shows null/undefined calls both enter the one-argument overload.

Android's documented bridge conversion contract also differs materially from Rhino:
- JavaScript primitive values and one-dimensional arrays have defined bridge mappings; multidimensional arrays and arbitrary JS object literals passed as custom Java classes are unsupported and become null.
- Boxed Java primitive parameters (Integer/Double/Boolean) are not accepted as primitive values; the guide says they become null.
- null/undefined passed to primitive parameters becomes a default primitive value (0/false/etc.), rather than Rhino's source-level rejection of null-to-primitive and undefined-to-non-String/Object.
- Java array return values are unsupported by the bridge (the guide documents undefined without executing the method); boxed primitive returns become opaque wrappers.
- The bridge method call is synchronous from JavaScript's perspective; Chromium's Java Bridge architecture documentation states Java calls execute on a private WebView background thread.

Official current Android guide: `https://developer.android.com/develop/ui/views/layout/webapps/native-api-access-jsbridge`; Chromium bridge architecture: `https://chromium.googlesource.com/chromium/src/+/refs/heads/main/android_webview/docs/java-bridge.md`; test source: `https://chromium.googlesource.com/chromium/src/+/2c59a5e233bffe4b4f1d12cde0b4650d9a751884/content/public/android/javatests/src/org/chromium/content/browser/JavaBridgeBasicsTest.java`.

**Consequence:** Rhino's type-sensitive conversion-weight resolver must not be used to model Android WebView method dispatch. They are two different bridge surfaces with different overload selection, coercion, return-value and threading contracts.

## 4. Applying the platform contract to pinned Legado WebJsExtensions

Pinned source evidence already inventoried in Runtime-1.6:
- `WebJsExtensions : RssJsExtensions`; `RssJsExtensions : JsExtensions`.
- Six upstream callsites pass WebJsExtensions to `addJavascriptInterface`: BackstageWebView, WebViewActivity, VideoPlayerActivity, ReadRssActivity, BookInfoActivity, BottomWebViewDialog.
- DEX direct method annotation entries: WebJsExtensions 12, RssJsExtensions 64, JsExtensions 54, SourceLoginJsExtensions 0. These are direct DEX annotations, not a computed final exposed method count.
- WebJsExtensions declares 12 directly annotated bridge methods and a string-dispatch `request(funName, jsParam, id)` that routes 16 named async operations.
- Inherited non-overridden annotated methods in RssJsExtensions/JsExtensions are eligible under the documented Android rule. Any override in a subclass must carry its own annotation; annotations do not automatically flow onto the override.
- A same-arity overload set is unsafe to assume deterministic by parameter type under WebView. Generated `@JvmOverloads` members count as separate JVM methods only if they survive DEX/R8 and are annotated/exposed; source annotation presence alone is not enough to assert every generated overload is exposed. The APK DEX direct-annotation inventory must be joined to exact DEX method descriptors before a per-overload exposure list can be finalized.

No device test was run. The Chromium test suite is external platform evidence and does not replace testing on the actual WebView provider/version installed on supported devices.

## 5. Evidence matrix and decision

| Requirement | Result | Reason |
|---|---|---|
| Exact pinned dependency coordinate/version/path | PASS | version catalog, module dependency and checked-in JAR/POM path identified |
| Exact JAR SHA-256 and exact .4 source mapping | BLOCKED | binary blob is present and Git blob ID/size known, but binary bytes/source commit delta were not inspectable in this audit |
| Public HtmlUnit 5.3.0 NativeJava resolver algorithm | PASS (public source tag only) | exact method source inspected; call path, weights, tie-breaking and coercion documented |
| Claim that pinned 5.3.0-legado.4 uses identical resolver | NOT PROVEN | no exact .4 bytecode/source comparison |
| Android target/min API boundary | PASS | minSdk 23 / targetSdk 36; legacy API<=16 behavior is out of supported range |
| Android WebView inherited annotated method rule | PASS (published contract) | official guide and Chromium test source explicitly cover inheritance and override annotation behavior |
| Android WebView same-arity overload rule | PASS (published contract) | official guide states count-only dispatch and arbitrary same-arity selection; test source covers arity dispatch |
| Per-method final exposure inventory for Legado hierarchy | PARTIAL | direct annotation counts exist; generated overload-to-annotation and complete override mapping not yet crosswalked |
| Device/provider-specific confirmation | NOT RUN | no Android device/WebView instrumentation test authorized or performed |

**Decision: NO-GO for a compatibility claim and NO-GO for production bridge integration.** The Android side is now sufficiently documented to define a precise static exposure matrix and test cases, but the pinned HtmlUnit .4 resolver is not proven identical to public 5.3.0 source. A differential probe is not yet justified as an acceptance gate until the exact .4 binary can be inspected with a DEX/classfile-capable tool or a provenance-matched source artifact. The next isolated action should be a narrowly registered, read-only exact .4 binary inspection / per-method DEX annotation crosswalk. Only after that can a separately registered controlled differential test be scoped.

## 6. Validation and actual changes

- Read pinned upstream version catalog, POM, app/module Gradle declarations and CatchableNativeJavaObject source.
- Inspected public HtmlUnit Rhino fork tag 5.3.0 source files for NativeJavaMethod, NativeJavaObject and WrapFactory. This is explicitly marked as a version-family reference, not exact .4 binary proof.
- Reviewed official Android WebView API reference and current native JavaScript bridge guide, plus Chromium Java Bridge architecture and instrumentation test source.
- No new compile, Gradle, test, CI, runtime execution, compiler experiment, APK build, device test or live-source test was performed.
- No production code, tests, Gradle/workflow, DB/schema/migration, persistence, callers, UI/Reader or `main` changed.
- Only this report and the Runtime checklist result are authorized.
- Runtime-1.7 is **PARTIAL / ACCEPTANCE NOT MET**. Runtime-1.6 remains PARTIAL / NO-GO; G2 remains BLOCKED/partial; B′ and full Legado compatibility remain unpassed.
