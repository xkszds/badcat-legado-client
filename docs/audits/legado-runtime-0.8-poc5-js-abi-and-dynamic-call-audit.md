# Runtime-0.8 PoC-5 — JS ABI and dynamic-call contract audit

Date: 2026-11-20

## Scope and immutable baselines

- Badcat branch: `integration/legado-upstream-runtime`
- Badcat starting HEAD: `728b2e2ef4463528353cc5ad37e075306b9a8a8e`
- Audit registration commit (before this report): `a6db97086b68e9873005fa40ddf110beeef08592`
- Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- This is an audit only. No runtime implementation or production caller is changed.

## 1. Source-level ABI inventory — what the existing report actually covers

The current `.github/scripts/legado-js-abi-inventory.py` inventories these five upstream declarations:
- `JsExtensions`
- `JsEncodeUtils`
- `RegexJsExtensions`
- `WebJsExtensions`
- `SourceLoginJsExtensions`

However, its comparison set is only `JsExtensions + JsEncodeUtils` versus the single host class `RhinoSafeSourceJavaExtensions`. It does not compare the separately inventoried WebView/login scopes to host equivalents, and it does not include `BaseSource` (the object exposed as `source` and also used as the `java`/ `sourceApi` object upstream). It also does not inspect the JS string-dispatch names in `WebJsExtensions.request`, or prove Rhino reflection/bytecode behavior.

The prior source-level artifact (workflow `44731457289`, exact code HEAD `5a84c0f43add8211e5d7ab8650337fdbf5309a82`, artifact `legado-js-abi-inventory` ID `14626776728`, SHA-256 `b406406d874533de6fecbaadf823cc65647dd82773dc64eea25da32e677de501`) records:
- Upstream base interface: 148 declarations / 104 names.
- Host `RhinoSafeSourceJavaExtensions`: 150 declarations / 110 names.
- 17 upstream names absent in the host class.
- Five overload-count differences and additional return/signature differences.
The host runtime sources inspected at current HEAD retain the same relevant declarations as that audited code. These counts are a source-declaration comparison, not JVM descriptor or runtime proof.

## 2. All 17 missing JsExtensions names and their real upstream behavior

The following names are present in pinned `JsExtensions.kt` and absent from current `RhinoSafeSourceJavaExtensions.kt`:

| Capability | Missing method names | Pinned behavior and real host requirement |
|---|---|---|
| Badcat reader/theme bridge | `getReadBookConfig`, `getReadBookConfigMap`, `getThemeMode`, `getThemeConfig`, `getThemeConfigMap` | ReadBook config serializes `ReadBookConfig.durConfig`; theme methods return `AppConfig.themeMode` and `ThemeConfig.getDurConfig(appCtx)`. String and Map variants are separate source contracts. They must map to Badcat's reader settings/theme model, not import Legado Reader. Current `BadcatTheme` only provides a light/dark appearance palette and is not equivalent to the full reader/theme config contract. |
| Navigation / external actions | `openUrl`, `openVideoPlayer` | `openUrl` has one- and two-argument forms, enforces a URL length bound, checks source UI permission, routes Legado deep-link imports specially, and otherwise opens a source-attributed confirmation flow. `openVideoPlayer` has two- and three-argument forms and uses the source context plus default float-window preference. Badcat currently supplies no equivalent callback on the Java bridge. |
| Current-book invalidation | `refreshBookInfo`, `refreshBookToc`, `refreshContent` | These post distinct EventBus events. Pinned BookInfoActivity handles book-info and TOC refresh only while STARTED; ReadBookActivity handles content refresh and TOC reload only while STARTED. A generic refresh callback would lose which state to invalidate and when to reload. |
| Browser / WebView / verification | `webView`, `webViewGetSource`, `webViewGetOverrideUrl`, `startBrowser`, `startBrowserAwait`, `getVerificationCode`, `showBrowser` | WebView methods call `BackstageWebView` with source headers/tag and cache/delay options, reject main-thread calls, and run under the Rhino plus source-navigation coroutine context. Browser methods invoke source-attributed verification UI; Await returns either the submitted response or a refetched HTTP response and respects cancellation/refetch flags. Image verification waits for a verification response. `showBrowser` resolves the top AppCompatActivity, switches to UI thread and displays the browser dialog with preload JS/config. No equivalent Badcat WebView/browser/verification service is installed in the current runtime. |

The three WebView selector helpers have 2/3/3 source overloads respectively; `webView` has two overloads; `startBrowser` has two; `startBrowserAwait` has three; `openUrl` and `openVideoPlayer` each have two. The prior ABI artifact compares declaration counts, but these overloads also require JS-to-Kotlin argument coercion, nullable values, default-argument behavior and coroutine/thread behavior to be tested.

## 3. Signature and return-shape mismatches even where names exist

| API | Pinned upstream | Current host | Consequence |
|---|---|---|---|
| `getSource()` | `BaseSource?` | `Map<String, Any?>` snapshot | JS loses the live source object, its methods, properties, identity and lifecycle. Full raw JSON in a map is not an equivalent BaseSource. |
| `getBatchContext()` | `BatchContentContext?` | `Any?` always null | Batch content rules cannot inspect the real typed context. |
| `cacheContent(chapter, content)` | Saves through the active batch context and returns success | Always throws | Real `contentBatch` chapter writes are unavailable; this is an explicit blocker, not a compatible fail-safe. |
| `getFile(path)` | `java.io.File` | `RhinoSourceFileHandle` | A restricted file facade may be a deliberate security boundary, but it is not API-equivalent until all exposed File operations and mutation semantics are differentially covered. |
| `get(url, headers, timeout)` | `org.jsoup.Connection.Response` | `RhinoSourceHttpResponse` | Wrapper method names, headers, raw request/response, charset and JS property/method behavior require parity tests. |
| `ajax` | nullable String body | non-null String host path | Null/error behavior differs. |
| `log` | `Any?` | String | Return value and null preservation differ. |
| `base64Encode` | nullable String | non-null String | Nullability/error contract differs. |
| `ajaxAll` | 2 overload declarations | 1 Kotlin declaration with default arg / generated overload | Source/JVM declaration and return wrapper differ; verify exact Rhino resolution. |
| `ajaxTestAll` | 2 overload declarations | 1 Kotlin declaration with default arg / generated overload | Same issue; host returns a custom response wrapper array. |
| `get` | 2 upstream declarations | 3 host declarations | Host adds `get(urlOrKey)` dual HTTP/key-value behavior not present as the same upstream overload; possible ambiguous Rhino dispatch. |
| `timeFormat` | 1 upstream declaration | 2 host declarations | Host adds String overload; source-level count and overload resolution differ. |
| `timeFormatUTC` | 1 upstream declaration | 2 host declarations | Host adds String overload; source-level count and overload resolution differ. |

Other same-name differences in the prior artifact include nullable/return-type changes and the custom response wrapper. A method name match is not accepted as compatibility.

## 4. BaseSource is a separate and currently under-counted JS object contract

Pinned `BaseSource` is explicitly documented as callable from JS (`source.xxx()`) and extends `JsExtensions`. Its source-level surface includes mutable source properties (`concurrentRate`, `loginUrl`, `loginUi`, `header`, `enabledCookieJar`, `jsLib`) and methods for:
- identity/source object: `getTag`, `getKey`, `getSource`;
- login lifecycle: `getLoginUiJs`, `isLoginUiV2`, `evalLoginUiV2`, `evalLoginActionV2`, `getLoginJs`, `hasLoginForm`, `hasLogin`, `login`;
- request headers/session: `getHeaderMap`, `getLoginHeader`, `getLoginHeaderMap`, `putLoginHeader`, `removeLoginHeader`;
- encrypted login information: `getLoginInfo`, `getLoginInfoMap`, `putLoginInfo`, `removeLoginInfo`;
- source variables/shared data: `setVariable`, `putVariable`, `getVariable`, `put`, `get`;
- source lifecycle: `refreshExplore`, `refreshJSLib`, `putConcurrent`, `evalJS`.

Current Badcat installs four distinct JS globals in `RhinoSourceScriptRuntime`: `java` = `RhinoSafeSourceJavaExtensions`, `cookie` = `RhinoSourceCookieExtensions`, `source` = `RhinoSourceLoginExtensions`, and `cache` = `RhinoSourceCacheExtensions`. This is not the pinned upstream binding shape: upstream BaseSource's `evalJS` binds the same live BaseSource object as `java`, `source`, and `sourceApi`, and binds the actual CookieStore and CacheManager objects.

The host source object implements a subset (key, variable, login header/info storage), but the exact contract differs:
- Host `getLoginInfo()` returns empty string when absent; upstream returns nullable decrypted content.
- Host `putLoginInfo()` returns normalized JSON string; upstream returns Boolean success/failure.
- Host `putLoginHeader()` returns normalized JSON string; upstream returns Unit and also synchronizes the CookieStore.
- Host `getLoginHeader()` returns empty string when absent; upstream returns nullable string.
- Host does not expose the full BaseSource live object, its mutable properties, `login()`, dynamic header-rule evaluation, `refreshExplore()`, `refreshJSLib()`, `putConcurrent()`, or its `evalJS` lifecycle as the same JS API.
- Host `source.setVariable` is a helper with its own return/null behavior; upstream has both `setVariable` and the JS-facing `putVariable` contract. Generic upstream `source.put(key,value)`/`source.get(key)` are not the same as the host's split `java` and `source` objects.

The host's `sourceDescriptor(source)` materializes raw imported JSON into a Map, and `java.getSource()` returns that Map. This is useful read-only metadata, but it is not a behavior-preserving replacement for `BaseSource?`.

## 5. Two additional upstream JS scopes are not compared by the current ABI report

### WebJsExtensions

Pinned `WebJsExtensions` is a separate WebView JavaScript interface with `@JavascriptInterface` methods `upConfig` and `request`, plus web-scoped wrappers for toast/log/ajax/connect/get/post/head/getStringList/getString. The `request(funName, jsParam, id)` dispatcher accepts named async operations including:
`run`, `ajaxAwait`, `connectAwait`, `getAwait`, `headAwait`, `postAwait`, `webViewAwait`, `webViewGetSourceAwait`, `decryptStrAwait`, `encryptBase64Await`, `encryptHexAwait`, `createSignHexAwait`, `downloadFileAwait`, `readTxtFileAwait`, `importScriptAwait`, `getStringAwait`.

It captures a weak WebView/callback reference, launches work in the Activity lifecycle scope, stores completion/error values in the shared memory cache by request ID, then calls `evaluateJavascript` to resolve/reject the injected JS promise. This is an asynchronous browser bridge, not merely aliases for the Rhino `java` object. Current Badcat tree has no WebView host/bridge counterpart.

### SourceLoginJsExtensions and SourceNavigationBridge

Pinned `SourceLoginJsExtensions` adds `upLoginData`, `reLoginView(deltaUp)`, `refreshExplore`, `copyText`, `clearTtsCache`, and overrides `showBrowser` to display the source login WebView dialog. Its callbacks are weak references to the login UI owner.

Pinned `SourceNavigationBridge` separately re-exposes `login`, both `openVideoPlayer` overloads and both `openUrl` overloads as WebView JavascriptInterface methods while enforcing the source-operation permission check and restoring the original source coroutine context for login.

Badcat has a V2 login script service (`LegadoSourceLoginUiV2Service`) and an Android UI activity, but that only renders/dispatches Login UI v2 JSON and runs loginCheckJs. It does not supply the full BaseSource login API, V1 dynamic login UI callback, browser dialog, WebView promise bridge, navigation bridge, or verification result lifecycle.

## 6. Behavior/caller evidence and host gap matrix

| Pinned path | Direct behavior/caller evidence | Current Badcat mapping | Audit status |
|---|---|---|---|
| `JsExtensions.kt` refresh methods | EventBus `REFRESH_BOOK_INFO/TOC/CONTENT`; BookInfoActivity refreshes book/TOC; ReadBookActivity refreshes current content/reloads TOC, only when lifecycle STARTED | No equivalent callbacks on Java bridge | Missing |
| `JsExtensions.kt` WebView methods | `BackstageWebView` uses source headers/tag, cache-first, delay and JS; must run off main thread under source navigation context | No WebView runtime bridge | Missing |
| `JsExtensions.kt` browser/verification methods | `SourceVerificationHelp` starts WebViewActivity and coordinates keyed pending attempts, UI result, timeout/cancel/refetch | No verification host/service | Missing |
| `JsExtensions.kt` openUrl/video | `SourceHelp`, confirmation activity and video UI, with source attribution and permission checks | No Badcat source action callback | Missing |
| `JsExtensions.kt` read/theme config | `ReadBookConfig.durConfig`, `ThemeConfig.getDurConfig`, `AppConfig.themeMode` | `BadcatTheme` palette only | Missing/unequal |
| `BaseSource.kt` | live BaseSource instance bound as java/source/sourceApi; login/header/variable/cache/shared-scope lifecycle | split `java`/ `source`/ `cookie`/ `cache` adapters; partial login/session methods | Unequal |
| `WebJsExtensions.kt` | async WebView request bridge and JS promise result dispatch | none | Missing |
| `SourceLoginJsExtensions.kt` | V1 login form callbacks, refresh, clipboard, browser | V2-only login service | Unequal |
| `SourceNavigationBridge.kt` | WebView bridge for login/open URL/video | none | Missing |

## 7. ABI annotation and reflection caveat

- `@JavascriptInterface` is material to Android WebView's exposed JavaScript surface. The source-source Rhino object is a separate surface: Badcat assigns ordinary Kotlin objects directly to Rhino `ScriptBindings`; the fact that a function is not annotated does not make it irrelevant to Rhino compatibility.
- The current ABI script parses Kotlin source declarations, not JVM descriptors, synthetic `@JvmOverloads` methods, bridges, JavaScript coercion or the actual Rhino reflection result. The WebView bridge also depends on Activity/WebView lifecycle and JavaScript promise dispatch.
- Therefore neither annotation counts nor same-name declaration counts can be treated as complete JS ABI proof. A future implementation gate needs compiled bytecode descriptor inspection and runtime Rhino invocation fixtures against both pinned upstream and host.

## 8. Required next implementation gate (not authorized by this audit)

1. Build a bytecode-level declaration inventory for the exact pinned upstream and host, including Kotlin default-argument synthetic methods, `@JvmOverloads`, JavaScriptInterface annotations and inherited members.
2. Create a runtime fixture that invokes each method from Rhino using representative JS argument types (missing, null, string, number, boolean, arrays, objects), and compare value shape, exception type/message class, side effects and cancellation.
3. Design typed Badcat-owned services for book refresh/invalidation, reader/theme config, navigation/video, browser/WebView/verification and V1/V2 login UI. Keep the Badcat Reader/UI as the owner; do not import Legado Reader/UI.
4. Resolve BaseSource identity/state: a Map projection cannot stand in for the live source object; determine the real source object/model/service closure and its unique state owner.
5. Add full WebJsExtensions and SourceLoginJsExtensions scope coverage or document a proven equivalent; include async request IDs, weak callback lifecycle, UI-thread transitions and promise resolution.
6. Only after exact host services and differential fixtures are registered, preregister a code PoC with compiler target and source list. No stubs/no-ops, no blanket allow-all reflection, no dropping browser/login/theme methods.

## Result

- Completed: exact pinned declaration/body inspection for JsExtensions, JsEncodeUtils, WebJsExtensions, SourceLoginJsExtensions, BaseSource, SourceNavigationBridge, SourceVerificationHelp, LoginUiV2 and relevant Badcat adapters; classified the 17 missing base names; identified BaseSource and WebView/login scopes omitted from the old comparison; mapped concrete lifecycle/caller semantics.
- Incomplete: compiled JVM descriptor inventory; Rhino reflection/coercion tests; full method-by-method caller graph for user-supplied JS source libraries; real WebView/browser/verification service adapter; exact source model object/state equivalence; full ABI compatibility.
- Actual changes: audit report only. No Kotlin/Java implementation, tests, Gradle, DB/schema, persistence, UI/Reader, production caller, APK or `main` change. No CI/test/build pass is claimed by this audit.
- Status: PoC-5 is an audit checkpoint, not a service-port prototype and not a full Legado compatibility pass. The authentic BookSource/BaseSource two-file compiler probe still has exit code 1; B′ and Runtime-0.8 remain unpassed.
