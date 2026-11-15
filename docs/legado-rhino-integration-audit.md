# Pinned Legado Rhino runtime integration audit

Audit date: 2026-11-15  
Upstream repository: https://github.com/LegadoTeam/legado  
Pinned upstream revision: `330e606c4d57e08adc056ff8ab7a4132c784da9`

## Decision

Do not enable JavaScript book-source rules by adding the Rhino Maven artifact alone. The repository now vendors the pinned Rhino runtime and wires a bounded adapter for replacement-rule `@js` expressions only. This adapter is not a general Legado book-source script runtime.

## Confirmed upstream implementation surface

The pinned upstream tree places the URL analyzer in `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`, while the script engine is a separate module under `modules/rhino/src/main/java/com/script/`.

The relevant runtime pieces include:

- `com.script.rhino.RhinoScriptEngine`: the single eval/compile entry point; installs a `RhinoContext`, marks script execution as allowed only during the engine call, checks recursion and coroutine cancellation, and provides suspend evaluation.
- `com.script.ScriptBindings`: isolated script scopes, standard-object initialization, per-source shared-global state, and explicit binding insertion.
- `com.script.rhino.RhinoContext`: compatibility parsing/compilation behavior and coroutine-aware execution state.
- `com.script.rhino.RhinoClassShutter`: blocks access to sensitive Java/Android classes and packages.
- `com.script.rhino.RhinoWrapFactory`: applies the class-visibility policy when Java objects and classes are exposed to scripts.
- `AnalyzeUrl`: not a standalone string-template helper. Its constructor and URL evaluation depend on source models, source headers/login state, cookies, proxy selection, network helpers, request methods, WebView, URL option parsing, encoding utilities, and script bindings.

## Integration requirements before enabling scripts

1. Vendor or otherwise integrate the exact pinned upstream Rhino source with its applicable license headers and notices. Record the upstream revision and preserve GPL-3.0 obligations for distributed combined work.
2. Bring in the complete direct dependency closure of the selected Rhino files, including the pinned HtmlUnit Rhino fork and AndroidX/coroutines dependencies. Verify the exact versions against the pinned upstream version catalog rather than selecting a newer arbitrary artifact.
3. Initialize the upstream context factory, class shutter, wrap factory, instruction/recursion limits, and cancellation behavior before any script can execute. Do not expose a raw Rhino `Context` or a generic unrestricted Java bridge to book-source scripts.
4. Implement a narrow host binding adapter for the actual supported Legado objects and functions. The replacement adapter's limited context objects are insufficient for the upstream `key`, source, response, document, JS library, and helper-object semantics.
5. Port URL query/form encoding and option processing as a separate compatibility layer. Do not replace upstream behavior with naive `{{...}}` string substitution.
6. Add deterministic tests for isolation between sources, shared-global invalidation, forbidden class access, recursion/instruction limits, coroutine cancellation, URL encoding, and script error mapping.
7. Do not route source search/detail/TOC/content scripts through the replacement adapter. Keep unsupported source-script features fail-closed until the complete source runtime and host services are implemented.

## Current project boundary

The repository now includes `RhinoLegadoJsReplacementEvaluator` for replacement-rule `@js` expressions. It exposes `result`, `chapter`, `book`, and a limited per-rule `java` helper (logging, key/value state, MD5, digest, HMAC), with script/context size caps and a 10-second maximum timeout. The vendored runtime installs a class shutter, wrap factory, interpreted mode, instruction observer, recursion guard, and coroutine cancellation checks. This is still a narrow replacement host: it does not implement the upstream `AnalyzeUrl` request pipeline, full `ScriptBindings` source semantics, JS library lifecycle, WebView bindings, response/document objects, or complete `RegexJsExtensions` / `JsEncodeUtils` helpers. The selector runtime remains a bounded CSS/JSONPath/XPath subset, not the complete upstream `RuleAnalyzer`. `LegadoUrlResolver` continues to reject unsupported source-script and WebView options.

The Android host still uses a temporary development launcher; the persistent source catalog is not wired to a full search/detail/TOC/content UI. No original Badcat navigation/UI migration is complete.

## Verification

This audit is a source-level integration assessment against the pinned upstream tree. CI validates compilation and the bounded replacement-host tests, but it does not establish device installation, visual fidelity, or full Legado compatibility.


## Verified module dependency and source inventory

The pinned upstream `modules/rhino/build.gradle` is an Android library (namespace `com.script`, minSdk 21, Java/Kotlin toolchain 21). Its direct dependencies are:

| Dependency | Pinned version | Upstream declaration |
|---|---:|---|
| `org.htmlunit:htmlunit-core-js` | `5.3.0-legado.4` | API |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | `1.11.0` | implementation |
| `com.squareup.okhttp3:okhttp` | `5.4.0` | implementation |
| `androidx.collection:collection` | `1.6.0` | implementation |

The source inventory at the pinned revision contains 18 Kotlin files plus the module Gradle file. The Kotlin files are:

- `com/script/CompiledScript.kt`
- `com/script/ScriptBindings.kt`
- `com/script/ScriptBindingsExtensions.kt`
- `com/script/ScriptException.kt`
- `com/script/rhino/CatchableNativeJavaObject.kt`
- `com/script/rhino/ClassNameMatcher.kt`
- `com/script/rhino/CollectionExtensions.kt`
- `com/script/rhino/JavaObjectWrapFactory.kt`
- `com/script/rhino/ProtectedNativeJavaClass.kt`
- `com/script/rhino/ReadOnlyJavaObject.kt`
- `com/script/rhino/RhinoClassShutter.kt`
- `com/script/rhino/RhinoCompiledScript.kt`
- `com/script/rhino/RhinoContext.kt`
- `com/script/rhino/RhinoContextElement.kt`
- `com/script/rhino/RhinoErrors.kt`
- `com/script/rhino/RhinoExtensions.kt`
- `com/script/rhino/RhinoScriptEngine.kt`
- `com/script/rhino/RhinoWrapFactory.kt`

This inventory is important: the class shutter and wrap factory are not optional convenience files. They are part of the upstream execution boundary. The upstream module also declares OkHttp and coroutines internally, so the host must check dependency alignment with its own transport and coroutine versions before packaging.

## Integration sequencing decision

The safest next implementation increment is to vendor the complete 18-file runtime module at the pinned revision as a distinct Android library module, preserving each upstream file's copyright/license headers and adding the upstream GPL-3.0 license and source attribution. Then adapt the host runtime in a separate package; do not modify upstream security classes to weaken restrictions. The host adapter must expose only explicitly approved objects and functions.

The runtime module alone still does not provide the app-level Legado source pipeline or `AnalyzeUrl`. URL parsing and rule execution remain separate integration tasks. The source catalog and current CSS/JSONPath/XPath subset must not be presented as complete Legado compatibility.


## Current bounded replacement adapter status (2026-11-15)

The app-side RhinoLegadoJsReplacementEvaluator is injected into the chapter replacement path. Each replacement rule session owns its helper state. The adapter rejects blank or oversized scripts, caps individual and aggregate context values, clamps execution to at most 10 seconds, and maps timeout or script errors to fail-closed rule failures.

The vendored RhinoScriptEngine source was inspected: it initializes a global ContextFactory with interpreted mode, RhinoClassShutter, RhinoWrapFactory, an instruction observer, and a maximum interpreter stack depth. RhinoContext.ensureActive() checks coroutine cancellation and the engine gates top-level script execution. These are meaningful safeguards, but they do not prove the entire host boundary safe by themselves.

Regression tests now cover:
- infinite-loop interruption at a short rule timeout;
- rejection of oversized script and context input;
- rejection of Java Runtime class access;
- isolation of helper state between independent rule sessions.

Remaining security/compatibility work:
- Verify the class shutter and wrap factory against reflection, Packages, JavaAdapter, getClass, and alternate Java bridge paths.
- Verify timeout recovery leaves no worker thread or Rhino context running.
- Add cancellation and repeated-timeout regression tests.
- Compare the adapter's @js input/output behavior and helper semantics with the pinned upstream RegexJsExtensions and JsEncodeUtils; only a subset is implemented.
- Keep source scripts, WebView rules, network bindings, and the full AnalyzeUrl pipeline disabled until their host services and tests exist.

The bounded adapter is a real implementation for its supported replacement-rule surface only. It must not be marketed as full Legado JavaScript compatibility.