# Runtime-3.2f.34 — BaseSource inherited API and host parity audit

- Branch / audit start HEAD: integration/legado-upstream-runtime / 0971ddbe86eb534d4ee891984eb7870da3960a15.
- Fixed upstream: LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Scope: read-only interface, caller, host-port and lifecycle audit. No production/test/Gradle/workflow/UI/storage changes.

## Executive result

Pinned BookSource inherits BaseSource, which in turn inherits JsExtensions and JsEncodeUtils. The effective contract is much larger than the six BaseSource fields projected into the host. This audit identified a substantial host subset for source identity, login state, variables, rate limiting, HTTP, cookies, Rhino utilities and file/archive helpers, but it is not a complete implementation of the upstream API or lifecycle.

**GO:** keep the current typed host ports and bounded Rhino bridge as partial support. Preserve source identity, per-source session ownership and explicit fail-closed behavior.

**NO-GO:** do not claim BaseSource/JsExtensions parity. Notable gaps/differences include dynamic header execution and default UA handling, complete WebView/browser/verification UI callbacks, exact CookieStore/WebView cookie semantics, full BaseSource refresh/login lifecycle, and unproven JS library shared-scope semantics.

## Pinned interface inventory

### BaseSource

Source blob: 9f5174d7ad6cb74a7e6b2d1f9260d90e3e8e6baa.

Properties: concurrentRate: String?; loginUrl: String?; loginUi: String?; header: String?; enabledCookieJar: Boolean?; jsLib: String?. Required identity methods: getTag(): String and getKey(): String. Default getSource() returns this.

Declared methods and behavior groups:

- Login UI/script: getLoginUiJs, isLoginUiV2, evalLoginUiV2, evalLoginActionV2, getLoginJs, hasLoginForm, hasLogin, login.
- Header/session: getHeaderMap(hasLoginHeader), getLoginHeader, getLoginHeaderMap, putLoginHeader, removeLoginHeader, getLoginInfo, getLoginInfoMap, putLoginInfo, removeLoginInfo.
- Source state: setVariable, putVariable, getVariable, generic put/get, refreshExplore, refreshJSLib, putConcurrent, evalJS.
- Helper outside the interface body: getStoredLoginInfoMap decrypts and parses stored login info; BaseSourceExtensions supplies getShareScope, getSharedGlobalStateKey, clearSharedGlobalState, clearSharedGlobalStateBySourceKey and getSourceType.

BaseSource-specific details: inline JS in loginUrl/loginUi is extracted; BookSource overrides getLoginJs to return mainJs for a JS source. getHeaderMap evaluates inline header JS, parses the resulting map, adds the configured default User-Agent when absent, and optionally merges saved login headers. Login header Cookie is also synchronized into CookieStore. Login info is AES-encrypted with the Android ID-derived key. Login UI v1 can derive default RowUi values from script; v2 is state/action based. refreshExplore clears discovery-kind caches; refreshJSLib invalidates the shared JS library scope. putConcurrent updates the shared limiter keyed by getKey(). evalJS binds java/source/sourceApi/baseUrl/cookie/cache and uses a shared jsLib scope/global-state key.

### JsExtensions and JsEncodeUtils

JsExtensions blob: f6808edd2a0d312e8bd13649935dea444cb82581; it extends JsEncodeUtils blob c75b61650db2abdd64a09d2bd8340ef14c2947b3. The JsExtensions file declares 118 function signatures including overloads; JsEncodeUtils adds 28 signatures. This inherited API surface must not be mistaken for only BaseSource's own methods.

JsExtensions API inventory by family:

- Context/source hooks: getSource, getTag, getSourceNavigationContext, getBatchContext, cacheContent, refreshBookInfo, refreshBookToc, refreshContent.
- HTTP/network: ajax, ajaxAll, ajaxTestAll, connect, get, head, post (with overloads for timeout, headers and rate-limit bypass).
- WebView/browser/verification: webView, webViewGetSource, webViewGetOverrideUrl, openVideoPlayer, startBrowser, startBrowserAwait, getVerificationCode.
- Cache/files/archive: importScript, cacheFile, downloadFile, getCookie, getFile, readFile, readTxtFile, deleteFile, unzipFile, un7zFile, unrarFile, unArchiveFile, getTxtInFolder, ZIP/RAR/7z text and byte-array helpers.
- Fonts and conversion: queryBase64TTF, queryTTF, replaceFont, strToBytes, bytesToStr, base64Decode/base64Encode variants, hex helpers, timeFormat/timeFormatUTC, encodeURI, htmlFormat, t2s, s2t, getWebViewUA, toNumChapter, toURL.
- UI/system/concurrency: toast, longToast, log, logType, randomUUID, androidId, openUrl, showBrowser, singleFlight, lock, tick, getReadBookConfig/getReadBookConfigMap, getThemeMode/getThemeConfig/getThemeConfigMap.
- JsEncodeUtils adds MD5 helpers, symmetric/asymmetric crypto constructors, AES/DES/3DES encrypt/decrypt, digest and HMAC helpers.

### Related upstream implementations and callers

- NativeBaseSource.kt blob 8313dedf737fe42cb4381535f6e74760c4f31786 controls Rhino property access: setter-style property writes are hidden except setVariable; assignment to variable is specially allowed.
- BaseSourceExtensions.kt blob 4b70e9848dde0c3adad44f8cc5147a2abe128cb1 implements jsLib scope/global-state identity using library hash + class + source-key hash.
- SharedJsScope.kt blob dab7770d746c851c06ad05c08a35a2866a6100b3 manages cached jsLib scopes, crypto scope, scope creation locks and shared global state.
- ConcurrentRateLimiter.kt blob 312ab0c8e0952a40d4f6997e898efe4e57f85d17 implements source-keyed rate windows and updateConcurrentRate.
- CookieStore.kt blob 09720d79f7009db052c080b54fc5fa2805257b64 uses Room CookieDao/domain buckets plus session and WebView CookieManager integration.
- LoginUiV2.kt blob fd4fca1baa694a821400a46d08ff20d6607f4b96 parses v2 marker, render rows, action envelopes and command validation.
- Direct caller examples include AnalyzeUrl and AnalyzeRule request construction, SourceLoginViewModel/SourceLoginV2Delegate, SourceLoginDialog, ReadRssViewModel, and BookSource's JS-source getLoginJs override. Upstream help docs expose getHeaderMap/login/session, variables, refresh and cookie APIs as public script contracts.

## Host mapping and differences

| Contract area | Host evidence | Finding |
|---|---|---|
| Identity/source descriptor | ReadingEngine.BookSource blob 70f1e0036a17b6d582744d612564842831b623f; RhinoSourceScriptRuntime blob 879d6ed978894b16271cf5207348176fd040eda7 | source ID/name and source fields are exposed; RhinoSafeSourceJavaExtensions supplies getTag/getSource. RhinoSourceLoginExtensions.getKey returns sourceKey (source ID is the imported source URL). Adapted, not a real BaseSource object. |
| Header | SourceRequestFactory blob cd70c32bc933a6716692e6043d9634dbcbf10e7e | Static JSON string headers and request overrides are merged with validation/limits. It does not evaluate header @js/<js> expressions or add the upstream default User-Agent/getHeaderMap(true) behavior. Login headers are attached by a separate wrapper and domain check. |
| Login JS/UI | SourceLoginSession.kt blob 47707cce281d9cdaf79da53d16237334261f69a0; LegadoSourceLoginUiV2Service.kt blob 22c9bdf88f46f20dcc592d921a3db2c305ee4aae | V2 render/action and loginCheckJs exist; login headers/info are persisted and login info encrypted. The v2 service resolves loginScript from loginUrl; it does not demonstrate BookSource.getLoginJs mainJs override for JS sources or full legacy v1 form derivation. |
| Login session/cookie | SharedPreferencesSourceCookieJar.kt blob e33895ff829006d5d6fa2fec34c0b5cab885695a0; RhinoSourceCookieExtensions.kt blob c299469c6fb3a70929ac06c7a09c49773d7fc42d | Per-domain name/value store, persistent/session split, explicit set/replace/remove and WebView import metadata exist. Source comments explicitly say this is not the upstream Room CookieDao schema or complete WebView cookie bridge. Cookie attribute/path/secure semantics differ from OkHttp's normal jar and require differential tests. |
| Rate limiting | LegadoConcurrentRateLimiter.kt blob ae1522e0c2db9a883d3fa29b50d004f19bd9c9d8 | Source-keyed limiter port exists. Invalid settings currently parse to null and bypass limiting; upstream updateConcurrentRate validates and throws for malformed nonzero values. Exact update/window/frequency/cancellation behavior needs differential fixtures. |
| JS libraries/state | RhinoSourceScriptRuntime.kt blob 879d6ed978894b16271cf5207348176fd040eda7; RhinoSourceCacheExtensions.kt blob b014c4f03f4161a49f1037993267934cfbcb2a5e | JS evaluation, per-source variables, memory/durable cache and cleanup exist. Upstream shared scope identity/cache behavior is more specific (jsLib hash + class + source key, thread-local crypto scope, shared globals); host lifecycle equivalence is not established by source-level API presence. |
| Java/JS helper APIs | RhinoSafeSourceJavaExtensions.kt blob 6a00e649895c4c1d223b768c6b288632ec033de9 | Broad helpers exist for HTTP, file/archive, crypto, TTF, conversion, locks, cookies and source descriptor. This is a host-specific bounded bridge, not proof every overload and side effect in the 118 JsExtensions / 28 JsEncodeUtils signatures matches. |
| WebView/browser/verification UI | No complete equivalent in the reviewed host runtime bridge | Upstream webView, browser launch/await, video player, verification-code UI and refresh callbacks depend on app lifecycle/UI callbacks. No full typed Host Port equivalent was found in the reviewed source set; classify as unsupported/unresolved rather than no-op parity. |
| Refresh lifecycle | BaseSource.refreshExplore/refreshJSLib and SharedJsScope.remove | Host has runtime clearSourceState and factory lifecycle cleanup, but no one-to-one script-exposed BaseSource refresh methods were found. Needs explicit source lifecycle contract. |

## Host test evidence inventory (not execution results)

Relevant host test files exist: SourceRequestFactoryTest, OkHttpSourceRequestClientTest, SharedPreferencesSourceCookieJarTest, SourceLoginSessionTest, LegadoConcurrentRateLimiterTest, LegadoSourceLoginUiV2ServiceTest, RhinoSourceScriptRuntimeTest, RhinoSafeSourceJavaExtensionsCompatTest, archive/TTF/cache extension tests. Their existence is not a statement that this audit ran tests or that every inherited overload has parity.

## Minimal follow-up sequence

1. Register differential contract tests for header JS/default UA/login-header merge and JS-source getLoginJs(mainJs) selection before changing request/login behavior.
2. Register a focused concurrentRate differential fixture for malformed/zero/positive N/M forms, setting updates, in-flight window behavior and cancellation.
3. Register cookie semantic differential tests for domain mapping, persistent/session lifetime, set/replace/remove, subdomain/path/secure/expiry and WebView import/export; do not claim Room/WebView parity without those tests.
4. Inventory the full 118 JsExtensions and 28 JsEncodeUtils signatures into a machine-readable API matrix; group each overload by required host port (network, file/archive, crypto, WebView/browser, UI callback, verification, cache, state).
5. Add typed lifecycle callbacks for refreshExplore/refreshJSLib and source disposal only after their ownership and cancellation semantics are specified.
6. Treat browser/WebView/verification UI as a separate host-port slice, not as an optional selector parser feature.

## Decision

- GO: host already has meaningful partial adapters for HTTP, cookies, login state, variables, rate limiting, Rhino and many utility functions.
- NO-GO: BaseSource/JsExtensions/JsEncodeUtils complete compatibility, dynamic header behavior, full CookieStore/WebView semantics, refresh lifecycle, and UI-bound browser/verification callbacks are not accepted.
- Read-only audit only. No tests/build/CI/APK/network-source/emulator/physical-device results are claimed.