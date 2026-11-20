# Runtime-0.7 PoC-3 method-level service contract map

Date: 2026-11-20

## Immutable baseline and evidence

- Upstream source: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Method declarations inspected verbatim at the pinned SHA in BaseSource.kt, JsExtensions.kt, SharedJsScope.kt, JsSourceEngine.kt, AnalyzeRule.kt, AnalyzeUrl.kt, CookieStore.kt, CacheManager.kt, SourceLoginJsExtensions.kt, SourceVerificationHelp.kt, LoginUiV2.kt and BaseSourceExtensions.kt.
- Exact source closure and build ownership: `docs/audits/legado-runtime-0.7-poc2-architecture-gate.md`.
- PoC-1 compile diagnostics: `docs/audits/legado-runtime-0.7-poc1-compiler-diagnostics.md`.
- This is a source declaration and callsite sample audit. It does not claim a bytecode-complete ABI or exhaustive caller search.

## Method-level behavior map

### BaseSource.kt — source lifecycle, identity, login and variables

| Method group | Pinned public methods | Required state/side effects | Adapter boundary |
|---|---|---|---|
| Identity and JS bindings | `getKey()`, `getTag()`, `getSource()`, `evalJS(jsStr, bindingsConfig)` | Stable source identity, tag, source object exposure, exact Rhino bindings/context and exception behavior | Badcat BookSource identity + authentic Rhino runtime + typed runtime context |
| Login UI/rule parsing | `getLoginUiJs()`, `isLoginUiV2()`, `evalLoginUiV2()`, `evalLoginActionV2()`, `getLoginJs()`, `hasLoginForm()`, `hasLogin()`, `login()` | Inline JS extraction, V1/V2 shape, state/action evaluation and login lifecycle | Badcat-owned login surface implementing upstream V1/V2 contract |
| Login headers/session | `getHeaderMap()`, `getLoginHeader()`, `getLoginHeaderMap()`, `putLoginHeader()`, `removeLoginHeader()` | Header precedence, cookie extraction, session update/clear | Badcat-owned per-source session store with exact merge/precedence semantics |
| Login info | `getLoginInfo()`, `getLoginInfoMap()`, `putLoginInfo()`, `removeLoginInfo()`, `getStoredLoginInfoMap()` | AES encode/decode, Android ID-derived key, JSON field parsing, re-entry guard | Secure Badcat secret store; preserve key derivation and malformed/null behavior or explicitly prove equivalent |
| Source variables | `setVariable()`, `putVariable()`, `getVariable()`, `put(key,value)`, `get(key)` | Persistent source variables, cache invalidation and source-key namespace | Badcat source-state store; no duplicate writer |
| Source cache/lifecycle | `refreshExplore()`, `refreshJSLib()`, `putConcurrent()` | Explore cache clear, JS library/shared scope invalidation, concurrency config update | Runtime lifecycle coordinator + cache/scope ports |

### JsExtensions.kt — complete source JavaScript API surface

| Capability family | Pinned method names / overload families | Required behavior |
|---|---|---|
| Context and book lifecycle | `getSource`, `getTag`, `getSourceNavigationContext`, `getBatchContext`, `cacheContent`, `refreshBookInfo`, `refreshBookToc`, `refreshContent` | Source identity, coroutine/navigation context, batch chapter context, cache fence and refresh callbacks |
| HTTP and parallel requests | `ajax` (2), `ajaxAll` (2), `ajaxTestAll` (2), `connect` (3), `get` (2), `head` (2), `post` (2), `downloadFile` (2) | Request parsing, timeout, headers, cookies, rate limit, response type, cancellation, retries and body semantics |
| WebView/browser | `webView` (2), `webViewGetSource` (3), `webViewGetOverrideUrl` (3), `startBrowser` (2), `startBrowserAwait` (3), `showBrowser` (4), `getWebViewUA` | Real WebView lifecycle, injected JS, source extraction, redirect override, async completion and user interaction |
| Login/verification/navigation/media | `getVerificationCode`, `openUrl` (2), `openVideoPlayer` (2), browser APIs | Verification result synchronization, navigation confirmation, video callback and browser behavior; must be backed by Badcat-owned surfaces, not Legado UI |
| Cookie/cache/file | `cacheFile` (2), `getCookie` (2), `importScript`, `getFile`, `readFile`, `readTxtFile` (2), `deleteFile`, archive extraction and ZIP/RAR/7z content/byte APIs | Cookie/session access, cache TTL and path behavior, file safety, archive limits and encoding |
| Encoding and format | string/byte conversion, Base64, hex, URI, HTML, time format, T2S/S2T, URL parsing, chapter title parsing | Exact JS-visible argument types, nulls, return values and overloads |
| Diagnostics and utilities | `toast`, `longToast`, `log`, `logType`, `randomUUID`, `androidId`, `singleFlight` (2), `lock` (2), `tick` | Logging, UI notification, stable device identity, per-source concurrency and lock semantics |
| Font/reader config | `queryBase64TTF`, `queryTTF` (2), `replaceFont` (2), `getReadBookConfig`, `getReadBookConfigMap`, `getThemeMode`, `getThemeConfig`, `getThemeConfigMap` | Font parsing and JS-visible reader/theme settings. Map to Badcat's own reader/config; do not import Legado Reader/theme UI. |

The overload counts above are source declarations in the pinned interface, not bytecode descriptor counts. Every overload and return shape must be added to the automated ABI/differential fixture before acceptance.

### Engine, parser, HTTP, state and UI service contracts

| Upstream class | Public behavior observed | State and external effects | Badcat service port required |
|---|---|---|---|
| `SharedJsScope` | `getCryptoScope`, `getScope`, `remove`; compile/cache/shared-global helpers | JS library hash, shared scope identity, crypto scope, weak refs, locks, on-disk cache and lifecycle cleanup | `JsScopeStore` + exact Rhino scope registry and lifecycle |
| `JsSourceEngine` | `callFunction`, `callFunctionIfExists`, batch content call, result normalization | Script compilation, function dispatch, binding injection, batch context and result serialization | Authentic upstream engine retained; typed source/context and batch ports |
| `AnalyzeRule` | set content/base URL; CSS/XPath/JSONPath/JS selection; string/list/element extraction; source-rule split/replace; variables and JS evaluation; chapter/TOC refresh hooks | Rule semantics, cache, JS globals, source/book/chapter/batch context, WebView fallback | Authentic rule engine + HTTP/WebView/JS/context adapters |
| `AnalyzeUrl` | URL syntax/JS/page substitution; request method/body/headers/charset; timeout/retry/redirect/proxy/DNS/WebView; cookie; string/response/bytes/stream/upload | Network transport, cookie lifecycle, source concurrency, request cancellation, logging, media type and response normalization | `HttpTransport`, `CookieSessionStore`, `WebViewHost`, `ConcurrencyGate` |
| `CookieStore` | set/replace/get/remove cookie, web cookie, restore, map conversion, clear | Domain normalization, session-cookie merge, Android WebKit CookieManager and Room Cookie DAO | Badcat-owned cookie store + WebKit bridge with upstream merge semantics |
| `CacheManager` | typed put/get, memory/disk, TTL, file cache, delete, web cache, source-variable clear | Room Cache DAO, LRU, ACache, QueryTTF and file storage | Badcat cache store with exact TTL/key/fence and size limits |
| `LoginUiV2` | V2 detection, render parse, action parse, field resolution | JSON shape validation, unknown-key and malformed handling | Badcat login UI model/controller preserving exact JSON semantics |
| `SourceLoginJsExtensions` | login callback data/view updates, refreshExplore, copy, clear TTS cache | Activity weak reference, lifecycleScope, dialog, file/cache and UI callback | Badcat-owned login view host; do not copy Legado Activity/dialog |
| `SourceVerificationHelp` | register/attach/cancel verification attempts, browser start, result wait/complete/clear, shared-flight coordination | Activity/browser launch, thread/latch coordination, coroutine cancellation and timeout | Badcat WebView/verification host plus tested concurrency state machine |

## Caller and implementation evidence (sampled, not exhaustive)

- Pinned `App.kt` registers `BookSource`, `RssSource` and `HttpTTS` with `NativeBaseSource.factory`; `BaseSource` is a shared upstream base contract, not a book-only DTO.
- GitHub code search at the pinned repository source index shows `JsExtensions` implemented by `BaseSource`, `JsSourceEngine`, `WebJsExtensions`, `RssJsExtensions` and `SourceLoginJsExtensions`. `RegexJsExtensions` is a separate `JsEncodeUtils` consumer.
- `SourceLoginJsExtensions.Callback` is implemented by `SourceLoginDialog` and an inline callback in `ExploreAdapter`.
- `BackstageWebView` injects a source instance as a JavaScript interface; source navigation context is attached before injection.
- `AnalyzeRule`/ `AnalyzeUrl` are also consumed by web-book parsing, book content, book cover, RSS and dictionary code. The book-source engine must preserve its book-source path without importing those unrelated product surfaces wholesale.
- Search results are sampled; the full exhaustive callsite graph remains open and must be generated from the pinned source tree before production migration.

## Consolidated service-port contract (candidate only)

1. **Source model/context:** authentic BookSource and rule models; book/chapter identity; source tag/key; batch context; refresh callbacks.
2. **Persistence/state:** source variables, encrypted login info, login headers, cookie jar, shared JS state, cache entries and file cache; each state domain needs one Badcat-owned writer and restart semantics.
3. **JS runtime:** pinned Rhino, ScriptBindings, function overloads, shared library scopes, CryptoJS scope, execution context, cancellation and result normalization.
4. **HTTP/network:** OkHttp request/response types, headers, timeout hierarchy, retry, redirects, proxy/DNS, cookie attachment/save, concurrency, request cancellation and response body handling.
5. **WebView/browser/verification:** JS injection, navigation interception, browser wait, verification challenge, cookie sync, lifecycle and cross-thread result delivery.
6. **Login host:** V1/V2 parsing, render/action callbacks, session inputs, state persistence and UI refresh.
7. **Book/chapter lifecycle:** detail/TOC/content refresh, batch context, content cache fencing and reader config projection into Badcat.
8. **Environment/files/crypto:** app constants, logging, device identifier, AES key behavior, file/archive/TTF helpers, toast/open URL/video/browser capability mapping.

## Gate decision

- This map confirms a broad runtime service boundary, not a handful of missing imports. The most dangerous ownership conflicts are CacheManager/CookieStore/SharedJsScope and Book/BookChapter state; the most dangerous UI coupling is JsExtensions/verification/login/WebView.
- The next implementation PoC must choose one service port at a time, begin with a non-UI/non-DB source engine slice only if its full contract is demonstrably isolated, and add behavioral differential tests. Do not use a Map/Any? placeholder where upstream exposes typed models or response objects.
- Full compatibility requires every method in the JS ABI, including browser, verification, media and reader/theme configuration APIs, to have a real Badcat-backed behavior or be explicitly blocked. Omitting these because the current search UI is simpler is not allowed.
- No production code, production callers, persistence, database/schema, Badcat UI/Reader or `main` changed during this audit.
