# Runtime-1.0 PoC-7c — navigation/browser/WebView/verification host seam audit

- Branch: `integration/legado-upstream-runtime`
- Registration commit: `617c8690a2f967aa584d30626a8f80f68585385e`
- Starting HEAD: `f9d8c0176d84b6a3c510cd7a83894c16713f3ce3`
- Fixed upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: audit only; no runtime implementation or tests changed.

## 1. Upstream method families are not one generic navigation API

Pinned upstream source bodies inspected: `JsExtensions.kt`, `SourceHelp.kt`, `SourceVerificationHelp.kt`, `SourceNavigationBridge.kt`, `WebJsExtensions.kt`, and `SourceLoginJsExtensions.kt`.

| API family | Pinned behavior | Host implication |
|---|---|---|
| `openUrl(url[, mimeType])` | Source-attributed external URL action. Applies source UI permission checks, URL length validation, special Legado import/deep-link handling and a confirmation flow for ordinary links. | A plain ACTION_VIEW Intent is not equivalent and can bypass confirmation, source attribution and suppression policy. |
| `openVideoPlayer(url,title[,isFloat])` | Source-attributed video action with a default float-window preference; source UI permission is checked before delegating to SourceHelp/video playback. | Requires a real Badcat video capability and permission/foreground policy; no generic external-player shortcut. |
| `startBrowser(url,title[,html])` | Starts source-attributed browser UI for manual anti-bot verification; checks active Rhino context and source UI permission. | Requires a user-visible browser owner, safe URL policy, source attribution and result lifecycle. |
| `startBrowserAwait(...)` | Suspends source execution until a keyed browser verification result is returned; supports cancellation and optional refetch behavior. | Requires a cancellable request/result broker tied to source execution, not merely opening a browser Activity. |
| `getVerificationCode(...)` | Coordinates image/code verification UI and waits for a keyed response; cancellation/interruption clears the pending attempt. | Requires explicit pending-attempt identity, timeout/cancel cleanup and result delivery. |
| `showBrowser(...)` | Resolves a suitable top Activity, switches to UI thread and displays the source browser dialog with preload JS/config. | Requires Activity lifecycle ownership and main-thread dispatch; no safe equivalent was found. |
| `webView`, `webViewGetSource`, `webViewGetOverrideUrl` | Run BackstageWebView off the main thread under the source navigation coroutine context, using source headers/tag plus cache-first and delay/selector options. | These are browser-rendered network execution capabilities, not ordinary HTTP aliases. |
| `WebJsExtensions.request` | Async WebView bridge: named operation dispatch, six stringified args, request ID, Activity lifecycle scope, weak WebView/callback references, memory-cache result/error and JS promise resolution/rejection. | Requires an actual WebView and lifecycle-bound async bridge; no equivalent Badcat scope exists. |
| `SourceNavigationBridge` | WebView JS interface restores source coroutine context for login and gates URL/video actions with navigation suppression policy. | Rhino direct object binding and WebView `@JavascriptInterface` exposure are distinct ABIs and must both be supported where applicable. |
| `SourceLoginJsExtensions` | Adds login-data update, re-login view, refreshExplore, clipboard and browser/login UI callbacks; browser override is part of the login flow. | Current V2 JSON login rendering is only a subset; it is not a V1/WebView/login callback equivalent. |

## 2. Security and lifecycle constraints

1. Source navigation is policy-controlled. Upstream has source UI permission checks and a navigation-suppression context; a host bridge must not expose unrestricted intents or bypass user confirmation.
2. URL validation is part of the contract. The upstream browser path bounds URL length and rejects invalid/null source context; the host must validate schemes, length and source ownership before UI dispatch.
3. Browser verification is a request/response protocol, not fire-and-forget navigation. Each attempt has a unique result key and waiter, accepts a result once, supports cancellation/interruption and clears pending state in `finally`.
4. The verification result can include both the resulting URL and user-entered response. Browser success may trigger a refetch; that behavior must remain distinct from image-code verification.
5. UI dispatch is lifecycle-sensitive. Browser/dialog launch must target a live Activity and execute on the UI thread. Pending callbacks must be invalidated when their owner is destroyed.
6. `WebJsExtensions` is asynchronous: it uses request IDs, weak references and Activity lifecycle scope, and explicitly signals promise success/failure back into JS. A synchronous Rhino helper cannot substitute for it.
7. WebView methods carry source headers and source tag, run off-main-thread and inherit source navigation coroutine context. Dropping any of these changes session, permission, cancellation or anti-bot semantics.

## 3. Badcat host mapping

- `RhinoSafeSourceJavaExtensions` remains an explicit allow-listed Java helper. The PoC-5 ABI inventory reports the upstream navigation/browser/WebView/verification names absent from this class; there is no safe direct callback installed.
- `RhinoSourceScriptRuntime` binds separate `java`, `source`, `cookie` and `cache` objects; it does not expose the upstream live `BaseSource` navigation object.
- `LegadoSourceLoginUiV2Service` evaluates Login UI v2 render/action hooks and `loginCheckJs`; its own documentation explicitly says it does not emulate `java.ajax`, WebView or the full upstream CookieStore surface. It is not the V1 login WebView/browser bridge.
- Current Badcat source screens do not supply a source-scoped browser/verification result broker, BackstageWebView host, async WebView promise bridge, source navigation confirmation flow or video-player service equivalent.
- A source-code search/inspection is not proof that no third-party source library calls these public methods. Since these are pinned public source APIs, compatibility remains required even without an in-repository caller.

## 4. Compatibility decisions

| Capability | Decision |
|---|---|
| URL opening | Missing. Do not map to generic ACTION_VIEW. Requires typed source identity, URL validation, user confirmation, import deep-link handling and policy gate. |
| Video playback | Missing. Requires a real Badcat player/host capability, source attribution, float preference semantics and permission/lifecycle tests. |
| Browser and browser-await | Missing. Requires an Activity-owned browser UI plus cancellable keyed result broker and optional refetch. |
| Image verification | Missing. Requires keyed pending attempts, response parsing, interruption/cancel cleanup and UI lifecycle handling. |
| Backstage WebView selectors | Missing. Requires isolated WebView worker, source headers/tag, off-main-thread execution, cache/delay semantics and cancellation. |
| WebJsExtensions async bridge | Missing. Requires actual JS promise injection, request-ID correlation, six-argument conversion, weak callback lifecycle, cache result/error and UI-thread completion. |
| Login V1/WebView bridge | Unequal. Existing Login UI v2 support does not cover V1 callbacks, browser dialog, SourceNavigationBridge or full BaseSource login lifecycle. |

No partial helper should be added to the JS ABI until the complete host capability exists. Do not implement these as no-op, generic callbacks or a plain URL launcher.

## 5. Future host seam (design only)

A future implementation should inject an execution-scoped capability set rather than global Activity references:

- `SourceNavigationPort`: source identity + policy decision + validated URL + confirmation/deep-link result.
- `SourceVideoPort`: source identity + media URL/title + float preference + active-owner lifecycle.
- `SourceBrowserPort`: source identity + browser request ID + URL/HTML/title + user result + cancellation/refetch contract.
- `SourceVerificationBroker`: unique attempt ID, result payload, timeout, cancellation and guaranteed cleanup.
- `SourceWebViewPort`: source headers/tag, selector/override options, cache-first/delay, off-main-thread execution and cancellation.
- `SourceWebJsBridgePort`: request ID + allow-listed operation + bounded parameters + lifecycle-safe promise completion.

These are conceptual boundaries only, not accepted production types. Before implementation, register concrete Android interfaces, permission/security policy, request ownership, coroutine cancellation, Activity lifecycle behavior, JS argument/return/error contracts and tests. Never let a source script select arbitrary Java classes or obtain an unrestricted Activity/WebView reference.

## 6. Result

- Completed: pinned source method/body inspection and host mapping for navigation, video, browser, verification, BackstageWebView, WebJsExtensions and login WebView scopes.
- Incomplete: no compiled upstream-vs-host ABI parity, no full caller graph for external source libraries, no real Badcat browser/WebView/video/verification service, no device lifecycle or permission validation.
- Actual changes: audit documentation only. No Kotlin/Java, Gradle, tests, DB/schema, persistence, production callers, Badcat UI/Reader, APK or `main` changed.
- No unit test, build, CI, source-smoke or device result is claimed.
- PoC-7c is an audit checkpoint only. Navigation/browser/WebView/verification APIs remain missing or unequal; BaseSource and full Legado compatibility remain blocked; B′ remains unpassed.
