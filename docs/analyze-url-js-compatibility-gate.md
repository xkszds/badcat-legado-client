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
3. Port the pinned upstream query/form encoding behavior and test Unicode, spaces, ampersands, equals signs, existing percent escapes, null, numeric and string results.
4. Integrate template evaluation and encoding as one ordered URL preparation pipeline; do not expose host objects until each API has a reviewed, source-scoped contract.
5. Keep `@js`, `js`, `bodyJs`, WebView, login and shared-global behavior disabled until their independent semantics and tests exist.

## Verification

This document records a source-level audit only. It does not enable JavaScript source rules, and it does not claim a successful build, test run, CI run, device validation, or APK.
