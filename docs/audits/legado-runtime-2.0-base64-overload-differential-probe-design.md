# Runtime-2.0 — base64Decode Rhino/WebView differential probe design

Date: 2026-11-21

## 1. Scope and starting evidence

- Registration: Runtime-2.0 in docs/required/03-LEGADO-RUNTIME.md, registration commit 292f41d0f4414486e3ef31b37535584412952ea7.
- Starting HEAD: 4cc870d3ccf0cae8a7aa0f7b1cd8f6088c2ca014.
- Fixed upstream: LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a.
- App Gradle file blob: app/build.gradle.kts = 8da3f52b5ec604b1ea7307ea20bfc54893c150be. Current app minSdk 26, targetSdk 36, compileSdk 36. It has JUnit 4.13.2 and Robolectric 4.14.1 unit-test dependencies.
- Rhino module Gradle blob: legado-rhino/build.gradle.kts = b94023a6eb61e3853ecd55ccee81ba9d540787e6. It exposes api dependency org.htmlunit:htmlunit-core-js:5.3.0-legado.4.
- Rhino engine source blob: legado-rhino/src/main/java/com/script/rhino/RhinoScriptEngine.kt = 84552db27cfff78d52905377fa6774dbc5dea34e.
- ScriptBindings source blob: legado-rhino/src/main/java/com/script/ScriptBindings.kt = 2b34712e911cc941b56f16257cbd560e1d31f64f.
- RhinoClassShutter source blob: legado-rhino/src/main/java/com/script/rhino/RhinoClassShutter.kt = e9033fa47753038751191926a4f898bf5680c05b.
- Existing host bridge source blob: app/src/main/java/app/badcat/client/runtime/RhinoSafeSourceJavaExtensions.kt = 6a00e649895c4c1d223b768c6b288632ec033de9. It contains both base64Decode(String, String) and base64Decode(String, Int) overloads, matching the upstream overload shape.
- Existing JVM tests include RhinoSourceScriptRuntimeTest.kt and RhinoSafeSourceJavaExtensionsCompatTest.kt. There is no app/src/androidTest source tree, no testInstrumentationRunner declaration in app/build.gradle.kts, and no AndroidX instrumentation runner/test dependencies in the app module. Robolectric is configured for local JVM tests, but it is not evidence of the installed Android WebView provider's JavaBridge overload behavior.

## 2. Minimal common fixture

Use one public test-only fixture class in package app.badcat.client.runtime so the existing Rhino class shutter does not reject its class name. Bind the same fixture object to Rhino as a ScriptBindings property and to WebView via addJavascriptInterface.

Fixture shape (test-only, not production):

~~~kotlin
class OverloadProbe {
    @android.webkit.JavascriptInterface
    fun select(value: String, second: String): String = "STRING"

    @android.webkit.JavascriptInterface
    fun select(value: String, second: Int): String = "INT"
}
~~~

The fixture intentionally returns a marker rather than performing real Base64 decoding, network access, file access or any other side effect. Both methods have the same name and two parameters, and both carry runtime JavascriptInterface annotations for the WebView half. Rhino does not require that annotation but uses the same exact public JVM overload set.

### Rhino half

- Run under the existing app JVM test source set, using the already configured JUnit/Robolectric test environment because the engine and class shutter touch Android APIs.
- Create one OverloadProbe and expose it through RhinoScriptEngine.eval(script) { put("probe", probe) } using ScriptBindings. ScriptBindings.set wraps the Java object through Context.javaToJS; RhinoWrapFactory checks the fixture's class name through RhinoClassShutter.
- Evaluate each expression in a fresh ScriptBindings scope to avoid accidental global state:
  - probe.select('x', '7') → expected STRING.
  - probe.select('x', 7) → expected INT.
  - probe.select('x', null) → expected STRING; primitive int is inapplicable.
  - probe.select('x', undefined) → expected STRING; undefined is accepted for String/Object, not primitive int.
- Assert selected marker and preserve the thrown exception details if any. These expected results follow the exact shipped DEX conversion weights from Runtime-1.9, not merely the public source-family model.

### Android WebView half

- Must run as Android instrumentation against an actual installed WebView provider, not Robolectric.
- Register the same fixture object with WebView.addJavascriptInterface(probe, "probe") before loading a local HTML document with JavaScript enabled.
- Invoke the same four expressions inside JS try/catch wrappers and return a JSON array through evaluateJavascript.
- Run all WebView creation, settings, load and evaluateJavascript operations on the main thread. Await onPageFinished and the evaluateJavascript callback with bounded latches; always destroy the WebView on the main thread in a finally block.
- Record WebView.getCurrentWebViewPackage() package name/version, Build.VERSION.SDK_INT, app targetSdk and app version alongside each result. App minSdk 26 makes getCurrentWebViewPackage available on the supported app range.
- The JavaScript bridge call itself executes on a WebView bridge background thread. The fixture is thread-safe and side-effect-free; do not touch Views from fixture methods.
- For WebView, do not assert which same-arity overload must win. The published platform contract says same-arity overload choice may be arbitrary. Record whether each call returned STRING, INT, null/undefined, or a JS error; compare the result vector with Rhino and preserve provider-specific observations.

## 3. Required assertions and result classification

| Input | Exact shipped Rhino expectation | WebView assertion |
|---|---|---|
| second arg is JS String | STRING | record selected marker/error; do not assume type-aware selection |
| second arg is JS Number | INT | record selected marker/error; do not assume type-aware selection |
| second arg is null | STRING | record selected marker/error and null conversion |
| second arg is undefined | STRING | record selected marker/error and undefined conversion |

Acceptance logic:
1. Rhino must return STRING / INT / STRING / STRING in that order. Any mismatch blocks the fixture because the test is no longer exercising the expected exact engine behavior.
2. WebView must return a valid captured result or a clearly captured JS error for every expression; callback timeout, page-load timeout or malformed JSON fails the test.
3. Store provider package/version and API level in test output. A run without provider identity is incomplete.
4. If WebView results differ from the Rhino vector, record a confirmed behavioral divergence for that provider. If they happen to match, do not infer the platform is type-aware; the official contract still permits arbitrary same-arity selection. Repeat on another provider/version before making a broad compatibility statement.
5. If WebView picks the int overload for a String or null/undefined input and returns an error/default conversion, preserve that exact result; do not normalize it to STRING.
6. No test result from Robolectric may be reported as a real Android WebView JavaBridge result.

## 4. Harness prerequisites and gaps

| Item | Current state | Required next change |
|---|---|---|
| Exact Rhino .4 dependency | Present through :legado-rhino | no dependency change |
| Rhino script engine + injectable bindings | Present | test-only fixture and unit test |
| JVM test runner | JUnit 4.13.2 + Robolectric 4.14.1 | likely no Gradle change for Rhino half |
| Android instrumentation source set | Absent | add app/src/androidTest test source in a separately registered implementation task |
| Instrumentation runner | Not declared | add testInstrumentationRunner to app defaultConfig |
| AndroidX test runner/ext-junit | Not configured | add minimal test-only dependencies in separately registered task |
| Installed WebView provider | Not known in CI/local metadata | device/emulator run must log package/version |
| CI/device availability | Not established by this read-only audit | no CI/device claim until actual run evidence exists |

Robolectric's ShadowWebView may model portions of WebView's API but does not substitute for a real provider's JavaBridge reflection and overload selection. The test must not use a shadow result to close the WebView platform gate.

## 5. Test implementation sequencing

1. Separate task: add only the test fixture and Rhino JVM test; run the focused unit test. This verifies the pinned HtmlUnit dependency behavior in the repository's actual JVM test classpath.
2. Separate task: add the minimal instrumentation runner/dependencies and WebView instrumentation test; run it on at least one API 26+ device/emulator with a real provider. Capture provider package/version.
3. Repeat the instrumentation test on a second provider/version if available. A single provider run is provider-specific evidence only.
4. Keep the test-only fixture and tests separate from production bridge code. Do not “fix” the overload collision as part of the test task.
5. After results are reviewed, register any compatibility fix as a separate task with an explicit API compatibility decision (e.g. distinct method names or an explicit single-dispatch function); do not silently remove an upstream overload or change a source-visible method contract.

## 6. Decision

**Design accepted as an executable bounded probe specification; no probe has been implemented or run.** The repository already has the exact Rhino dependency and a public binding injection path. It lacks an Android instrumentation source set, instrumentation runner declaration and AndroidX test dependencies. Therefore the Rhino half is locally feasible with existing unit-test infrastructure; the WebView half requires a separately registered test-only Gradle/test-harness change and a real Android device/emulator.

The next permitted action is a separate registration to implement and run the Rhino-only test first, followed by a separately scoped Android instrumentation test task. No production bridge integration is authorized by this design.

## 7. Validation and actual changes

- Read-only inspection of app/module Gradle configuration, existing Rhino unit tests, RhinoScriptEngine, ScriptBindings, RhinoClassShutter and host overload declarations.
- No test source or fixture was added. No Gradle file changed. No compile, Gradle task, test execution, CI, device/emulator run, WebView provider probe or APK build was performed.
- No production code, database/schema/migration/persistence, caller, UI/Reader or main changed.
- Only this design report and the Runtime checklist result are authorized.
- Runtime-2.0 is **DESIGN COMPLETE / NOT EXECUTED**. Runtime remains partial; G2 remains BLOCKED/partial; B′ and full Legado compatibility remain unpassed.
