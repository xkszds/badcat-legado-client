# Pinned Legado Rhino runtime integration audit

Audit date: 2026-11-15  
Upstream repository: https://github.com/LegadoTeam/legado  
Pinned upstream revision: `330e606c4d57e08adc056ff8ab7a4132c784da9`

## Decision

Do not enable JavaScript book-source rules by adding the Rhino Maven artifact alone. Keep `UnsupportedSourceScriptRuntime` as the default until the upstream runtime's execution controls and the Badcat host bindings are integrated and tested together.

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
4. Implement a narrow host binding adapter for the actual supported Legado objects and functions. The current `Map<String, String>` script binding contract is insufficient for the upstream `key`, source, response, document, JS library, and helper-object semantics.
5. Port URL query/form encoding and option processing as a separate compatibility layer. Do not replace upstream behavior with naive `{{...}}` string substitution.
6. Add deterministic tests for isolation between sources, shared-global invalidation, forbidden class access, recursion/instruction limits, coroutine cancellation, URL encoding, and script error mapping.
7. Only then replace `UnsupportedSourceScriptRuntime` with an explicitly injected production implementation. Keep unsupported JS rules fail-closed until that point.

## Current project boundary

The repository currently has `SourceScriptRuntime` and `UnsupportedSourceScriptRuntime`, but no executable Rhino implementation. `LegadoUrlResolver` intentionally rejects `{{...}}`, `@js`, `js`, `bodyJs`, and WebView options. The current selector runtime is also a bounded CSS/JSONPath/XPath subset, not the complete upstream `RuleAnalyzer`.

The Android host still uses a temporary development launcher; the persistent source catalog is not wired to a full search/detail/TOC/content UI. No original Badcat navigation/UI migration is complete.

## Verification

This audit is a source-level integration assessment against the pinned upstream tree. It is not a build or runtime test. The project has not been compiled in this environment; no CI success, device validation, or installable APK is confirmed.


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
