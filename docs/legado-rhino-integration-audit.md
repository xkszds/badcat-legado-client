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
