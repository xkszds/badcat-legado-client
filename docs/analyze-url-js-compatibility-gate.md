## Integrated bounded embedded-template path (2026-11-15)

The helper is now connected to `SourceHttpGateway` through an optional injected evaluator. The gateway expands complete `{{...}}` expressions before static URL parsing and parameter encoding, supplies the search keyword as `key`, and returns an evaluator failure without issuing HTTP. Without an injected evaluator, the gateway remains fail-closed. The Android `RhinoEmbeddedUrlScriptEvaluator` adapter uses the bounded Rhino runtime and supplies only `key`; it does not expose Java, network, cookie, cache, login, proxy, or WebView services. `SourceSearchService` forwards the search keyword into the gateway.

This does not enable the full upstream script pipeline. `@js`, `<js>`, `@result`, `page`/ `baseUrl` and shared host bindings, URL-option `js`/`bodyJs`, WebView, login/session and other host integrations remain unsupported. The Android adapter is not yet injected into the app's active search construction path. Tests are authored but not executed; no Gradle build, CI, device test, or APK is confirmed.

## Implemented isolated contract helper (2026-11-15)

Added `LegadoEmbeddedUrlTemplate` and an injected `LegadoEmbeddedScriptEvaluator` in the platform-neutral integration module. This helper:
- evaluates complete `{{...}}` segments sequentially and supplies the search `key` as the only explicit input;
- propagates an evaluator failure without returning a partial URL;
- mirrors the pinned replacement conversion for null, strings, integral `Double`, and other primitive values;
- follows the pinned `RuleAnalyzer.innerRule("{{", "}}")` empty-result behavior, retaining literal text around an empty expression and leaves an unmatched opening marker literal.

Regression tests were added for multiple expressions, Unicode key passthrough, null/integral numeric conversion, unmatched markers, and evaluator failure. These tests are authored only; they have not been run.

`LegadoUrlParameterEncoder` now ports the pinned `AnalyzeUrl.encodeParams` / `NetworkUtils.encodedQuery` / `encodedForm` behavior: RFC3986 query-safe characters, upstream already-encoded checks, segment-wise form encoding, charset selection, and legacy `escape` mode. It is wired into `SourceRequestFactory` after source and per-request headers are merged. Non-POST query strings are encoded before request dispatch; POST bodies are form-encoded only when the effective headers have no `Content-Type` and the body is neither JSON nor XML. The resolver also encodes raw Unicode/space query values before `URI` resolution. Regression tests cover UTF-8/GBK, delimiters, existing/malformed percent escapes, empty form segments, GET/POST branching, JSON and explicit content types. A standalone `kotlinc` smoke harness passed eight encoder/preparation checks; repository tests and the full Android build have not run. This is a static-request subset only: template evaluation remains unwired, and all script-bearing source URLs remain rejected.

# AnalyzeUrl JavaScript compatibility gate

Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9`

Inspected source:
- `app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt`
- `modules/rhino/src/main/java/com/script/ScriptBindings.kt`
- `modules/rhino/src/main/java/com/script/rhino/RhinoScriptEngine.kt`

## Confirmed URL processing order

The pinned `AnalyzeUrl.initUrl()` runs these phases in order:

1. `analyzeJs()`: handles the upstream `@js` / `<js>...</js>` form and `@result` chaining.
2. `replaceKeyPageJs()`: evaluates every embedded `{{...}}` expression through `RuleAnalyzer.innerRule`, then replaces page-list tokens.
3. `analyzeUrl()`: splits the URL from the optional JSON options, resolves it against the base URL, applies request options, and then encodes query or form parameters.

For embedded expressions, `evalJS(it)` receives a bindings scope containing at least `key`, `page`, `baseUrl`, `result`, and a number of host objects. The replacement converts null to an empty string, keeps strings as-is, formats integral doubles without a decimal suffix, and otherwise uses `toString()`.

The URL query/form encoding is a later phase, not part of the JavaScript replacement. Query and form parameters have different paths. The upstream implementation also preserves already encoded values under its own validation rules and supports the special `escape` charset mode.

## Why the current adapter stays gated

The repository's `LegadoRhinoSandbox` deliberately accepts only string bindings and primitive return values. It does not currently reproduce:
- `java`, `cookie`, `cache`, `source`, `book`, `infoMap`, or source-shared JavaScript state;
- the full upstream `RuleAnalyzer.innerRule` delimiter/parser behavior;
- the upstream numeric/null replacement conversions as a dedicated URL-template layer;
- the distinct query/form encoding pipeline and encoded-value detection;
- `@js`, URL-option `js`, `bodyJs`, login/session behavior, proxy, or WebView execution.

Consequently, enabling the sandbox directly inside the current static resolver would still be observably incompatible. In particular, evaluating a template and handing its raw output straight to the HTTP client can change query semantics.

## Safe implementation sequence

1. Keep the existing fail-closed behavior for all script-bearing URL rules.
2. Implement a separately testable template evaluator with only the reviewed `key` binding for the narrow embedded-expression subset.
3. Add an ordered template-expansion plus encoding pipeline with explicit, reviewed script bindings; the current encoder integration intentionally handles static requests only.
4. Test template expansion before encoding and preserve source-scoped failure behavior; do not expose host objects until each API has a reviewed contract.
5. Keep `@js`, URL-option `js`, `bodyJs`, WebView, login and shared-global behavior disabled until their independent semantics and tests exist.

## Verification

This document records a source-level audit only. It does not enable JavaScript source rules, and it does not claim a successful build, test run, CI run, device validation, or APK.
