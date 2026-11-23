# Runtime-3.2f.37 — dynamic header and login-header caller audit

- Branch / audit HEAD: `integration/legado-upstream-runtime` / `6ff710efe976b76db4f37b00fe7915748cb279d5`.
- Fixed upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Scope: read-only caller and behavior audit. No production, test, Gradle, workflow, UI or storage code changed.

## Upstream contract

Pinned source evidence:

- `BaseSource.kt`: blob `9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa`.
- `BookSource.kt`: blob `7f63496ebf25c820b7d0c715d2fbce8e7bd29b9c`.
- `AnalyzeUrl.kt`: blob `d898c88dfd844d73f06d9396d0b46f39939f7e6c`.
- `NativeBaseSource.kt`: blob `8313dedf737fe42cb4381535f6e74760c4f31786`.

Observed behavior:

1. BaseSource extracts inline JS only when the complete trimmed rule matches the upstream JS_PATTERN; it uses the captured script body. Static rules are passed through as JSON.
2. `getHeaderMap(hasLoginHeader)` evaluates inline header JS through `evalJS`, normalizes its JS result, then deserializes a string map. Exceptions are logged and swallowed except coroutine cancellation, which is rethrown.
3. After the configured header is parsed, the upstream default User-Agent is inserted if no case-insensitive UA key exists.
4. Saved login headers are merged only when `hasLoginHeader` is true. AnalyzeUrl defaults this option to true but gates it with `isLoginHeaderSite(mUrl)`; the source and request target are compared using upstream domain logic. The login map is merged after configured headers, so it takes precedence for matching keys.
5. BookSource overrides `getLoginJs()`: JS sources use `mainJs`; other sources delegate to BaseSource's loginUrl inline-script extraction.
6. BaseSource `putLoginHeader` persists the supplied JSON and synchronizes a Cookie/cookie entry into CookieStore via `replaceCookie`; `removeLoginHeader` removes the saved login header and clears CookieStore for the source URL.
7. AnalyzeUrl supports an injected `headerMapF` override. Otherwise it calls `source.getHeaderMap(hasLoginHeader && isLoginHeaderSite(mUrl))` inside the script coroutine context. URL-option headers are then applied as request-specific overrides. Cookie merge and CookieJar enablement occur later in AnalyzeUrl's request setup.

## Host caller map

Host evidence at the audited HEAD:

- `SourceRequestFactory.kt`: blob `cd70c32bc933a6716692e6043d9634dbcbf10e7e`. It parses `BookSource.header` as a static JSON object only, merges request-specific headers case-insensitively, validates size/name/value, and builds `SourceHttpRequest`. Its KDoc explicitly says script-generated options remain outside the factory.
- `SourceHttpGateway.kt`: blob `8dde6479650379b280d7d237b25366654d69210b`. The normal source rule request path resolves a URL and calls `SourceRequestFactory.create(source, resolved)`.
- `BadcatReadingEngineFactory.kt`: blob `cd8946aaaffacef3d2a748a537736cf2798c96c8`. Its Rhino `java.ajax/get/connect` callbacks independently call SourceRequestFactory for each script request. The configured client is wrapped in `SourceLoginSessionRequestClient`; the Rhino bridge receives WebView UA for its WebView helper, not as the default source HTTP User-Agent.
- `BadcatSourceHttpGatewayFactory.kt`: blob `042bc1401e4b667e903ded5f03a34c27f70cb561`. It also constructs requests through SourceRequestFactory; its default client is a plain OkHttpSourceRequestClient unless a caller supplies another client.
- `SourceLoginSession.kt`: blob `47707cce281d9cdaf79da53d16237334261f69a0`. The wrapper domain-gates stored login headers and merges them into outgoing request headers. Login Cookie persistence is wired separately through the factory's cookie writer/remover callbacks.
- `RhinoSourceScriptRuntime.kt`: blob `879d6ed978894b16271cf5207348176fd040eda7`. It exposes raw `header`, `loginUrl`, `loginUi`, and `jsLib` fields to the source descriptor but does not implement BaseSource.getHeaderMap or its dynamic header evaluation contract.
- `SourceRequestFactoryTest.kt`: blob `075900cccb317cb2ef3ef7c67bbf7c076ab28027`. Existing tests cover static JSON header parsing, case-insensitive merge, validation/limits and URL option fields; they do not cover inline header JS, default UA insertion, upstream login-header eligibility or CookieStore side effects.

## Gap matrix

| Behavior | Pinned upstream | Host at audit HEAD | Finding |
|---|---|---|---|
| Static JSON header | parse to string map | static JSON parse | Partial parity; host adds stricter validation/limits |
| `@js:` / `<js>` header | evaluate in BaseSource JS context | no evaluation | Missing |
| JS result normalization / errors | normalize result; log ordinary error; propagate cancellation | invalid JSON returns request failure | Different failure contract |
| Default User-Agent | insert AppConfig UA if absent, case-insensitive | no default UA in SourceRequestFactory | Missing |
| Login header eligibility | same-site check at AnalyzeUrl construction | separate wrapper compares source/target eTLD+1 | Adapted, caller coverage differs |
| Login header precedence | merge after configured headers | wrapper merges after request factory | Partial parity; verify all client paths |
| JS source login script | BookSource.getLoginJs returns mainJs | login service currently resolves loginUrl | Separate unresolved gap; not solved by header path |
| Cookie side effect | putLoginHeader Cookie uses CookieStore.replaceCookie | factory callback merges Cookie header into host cookie bucket | Adapted; not full WebView/CookieStore parity |
| Request-specific headers | URL-option headers applied after source headers | factory merges resolved headers after static headers | Partial parity |

## Architecture recommendation

Do not put Rhino execution into `integration-contract/SourceRequestFactory`: it is a pure contract module and does not own the Android/JS lifecycle. Add a typed app-layer source-header provider/port that is invoked by every outgoing request path before the pure request factory. It must:

- resolve static or inline header rules with the pinned full-match syntax;
- execute JS in the source's correct context and preserve cancellation/error semantics;
- add the configured default UA only when no case-insensitive UA key exists;
- determine login-header eligibility using the same source/target site rule;
- merge login headers after configured headers but before per-request URL-option overrides;
- keep Cookie synchronization attached to the login-session store without logging secrets;
- be wired consistently into SourceHttpGateway, BadcatReadingEngineFactory script callbacks, and BadcatSourceHttpGatewayFactory.

The provider must not create a second JS runtime or bypass Rhino's existing source scope. Before implementation, trace whether one runtime evaluation can be reused across retries/redirects and specify per-request vs per-source lifecycle, cancellation, and error reporting. JS-source `mainJs` selection should remain a separately preregistered login-runtime task.

## Decision

- GO: static header parsing, case-insensitive request override, and the existing login-session wrapper can remain as partial host behavior.
- NO-GO: dynamic header, default UA, full same-site login-header merge parity, and consistent coverage across all outgoing request paths.
- Next: preregister a dedicated app-layer header provider task with all affected callers/tests; separately register JS-source mainJs login selection. This audit itself changed only this report and the category checklist.
