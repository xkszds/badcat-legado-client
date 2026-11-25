# Runtime-3.0 BaseSource JS ABI and per-source Host Services contract

**Task:** `R3-S0-T026-PHASE-B-BASESOURCE-HOST-PORT-CLOSURE-026O`  
**Date:** 2026-11-25  
**Status:** source-level host attachment architecture selected; full Host Port implementation remains NO-GO pending the open contract/test gates.  
**Badcat start HEAD:** `3eea177532d100d0846349741fdc21e06524d576`  
**Pinned upstream:** `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## 1. Decision: preserve the concrete BookSource instance and attach typed services per instance

The viable architecture is **not** a wrapper around `BookSource` and not a global service locator. Keep the exact `BookSource : BaseSource` object as the source object passed through `AnalyzeRule`, `AnalyzeUrl` and `JsSourceEngine`; attach a typed, immutable `BaseSourceHostServices` aggregate to that same object through a non-persistent owner sidecar.

Proposed shape (design only; no source edit in 026O):

```kotlin
internal interface BaseSourceHostServicesOwner {
    fun requireBaseSourceHostServices(): BaseSourceHostServices
}

internal data class BaseSourceHostServices(
    val cache: SourceCachePort,
    val session: SourceSessionPort,
    val cookies: SourceCookiePort,
    val rateLimiter: SourceRateLimitPort,
    val http: SourceHttpPort,
    val files: SourceFilePort,
    val config: RuntimeConfigPort,
    val resources: ResourcePort,
    val context: AppContextPort,
    val notifications: RuntimeNotificationPort,
    val hostUi: HostUiPort,
    val hiddenWebView: HiddenWebViewPort,
    val visibleBrowser: VisibleBrowserPort,
    val verificationCode: VerificationCodePort,
    val productEvents: ProductEventPort,
    val batchContent: BatchContentPort,
    val explore: ExploreCatalogPort,
    val executionState: SourceExecutionStatePort,
)
```

- `BookSource` implements the internal owner interface with a body property, not a constructor property. Intended annotations: Room `@Ignore`, Parcelize `@IgnoredOnParcel`, Java `@Transient` (Gson exclusion), and `@get:JvmSynthetic` on the internal accessor so Rhino does not gain a new JS-visible method. The internal owner method should also be JVM-synthetic where supported. These annotation interactions must be proven by the ABI/serialization test. The property must not affect the 33 constructor fields, URL-based `equals/hashCode`, `copy`, `componentN`, Room schema, Parcelable payload or source JSON.
- `SourceExecutionStatePort` is injected into the App-owned `FileBookSourceCatalog`/composition root. Its `attach(source)` attaches a stable service aggregate for `source.getKey()` and returns the same instance; `clear(sourceKey,reason)` clears the per-source shared scope/cache/session/cookie/Explore state after replace/remove. The registry is owned by the injected port, not a static global lookup.
- `BaseSource` default methods obtain services through the internal owner on `getSource()`. This is important: upstream `SourceNavigationBridge` is `object : BaseSource by original` and its delegated `getSource()` returns the original source. Unwrapping through `getSource()` therefore preserves the original instance and host-service owner.
- `BookSource` is a final data class and `AnalyzeRule` has concrete `as? BookSource` callsites. A wrapper cannot subclass it and may fail those callsites. The same-object sidecar preserves the concrete type and `getSource() === this` for an ordinary BookSource.
- `BookSource.copy()` inside the candidate source closure is used by `checkContent()` to normalize a comparison copy; that copy does not execute BaseSource methods. No other BookSource execution copy callsite was found in the registered candidate closure. Any future copy/deserialization/parcel restore that is used for execution must call `attach` before side-effect methods.
- Pure identity/model methods (`getTag`, `getKey`, `getSource`, typed rule getters) may work before attachment. Any host-effect method called before attachment must fail with an explicit `HostServicesNotAttachedException`; never silently no-op.
- This is a **source-level architecture GO**, not a compile/test acceptance. The Room/Parcelize/Gson exclusion annotations and reattachment lifecycle still require focused contract tests before BookSource migration is authorized.

## 2. Exact upstream JS ABI inventory

The pinned source declares the following public method surface:

| API owner | Public declarations | `@JavascriptInterface` | Notes |
|---|---:|---:|---|
| `JsEncodeUtils` | 30 | 20 | Pure encoding/crypto helpers; 27 unique names, with crypto overloads. |
| `JsExtensions` | 118 | 48 | `@Keep` JS host API; 77 unique names, 34 names have overloads. |
| `BaseSource` | 29 | 9 | Source identity/login/session/cache/concurrency/JS methods. |
| **Combined unique signatures** | **175** | **77** | `BaseSource` overrides `JsExtensions.getSource/getTag`; do not double-count those two. |

Exact line-numbered signatures and annotations are retained in:

- `task026o-attempt1/jsencodeutils-api.tsv`
- `task026o-attempt1/jsextensions-api.tsv`
- `task026o-attempt1/basesource-api.tsv`

`JsExtensions` and `JsEncodeUtils` are not optional convenience APIs: Rhino scripts can call their public methods and WebView bridges rely on `@JavascriptInterface`. Preserve overload resolution, default arguments, nullable results, exception behavior and `@Keep`/annotation retention. The pure `JsEncodeUtils` algorithms can remain Runtime-owned; do not reimplement them with different encodings or crypto defaults.

## 3. BaseSource method behavior that must remain exact

The 29 methods are fully enumerated in `basesource-api.tsv`. Key invariants:

- `getTag()` is the source-visible name; `getKey()` is the generic source key; `getSource()` returns the same BaseSource object. A source key is **not always a BookSource URL**: BookSource uses `bookSourceUrl`, RssSource uses `sourceUrl`, HttpTTS uses `httpTts:$id`, and BookCover.CoverRule uses `searchUrl`.
- Login UI: `getLoginUiJs`, `isLoginUiV2`, `evalLoginUiV2`, `evalLoginActionV2`, `getLoginJs`, `hasLoginForm`, `hasLogin`, `login`. Preserve inline-JS extraction, v2 detection, state/form/book/chapter bindings and callback completion.
- `getHeaderMap(hasLoginHeader)` evaluates inline header JS or parses JSON, applies default User-Agent, merges login header only when requested, and rethrows coroutine cancellation while logging ordinary errors. `AnalyzeUrl` additionally gates saved login-header attachment to the same site.
- Login header mutation order: `putLoginHeader` writes the source-keyed cache entry and synchronizes its Cookie header through `CookieStore.replaceCookie`; `removeLoginHeader` deletes the header entry and clears source-domain cookies. Preserve ordering and partial-failure behavior.
- Login info: Android-ID-derived AES encryption/decryption; `getLoginInfo` returns null on failure, `putLoginInfo` returns Boolean, and `getLoginInfoMap` builds dynamic Login UI v2 defaults with a recursion guard. Do not reduce it to a stored JSON map only.
- Variables: source variables use the `sourceVariable_{sourceKey}` namespace; `put/get` use `v_{sourceKey}_{key}`. Null/remove and empty-string fallback semantics are distinct. Preserve the public JS method names and return types.
- `refreshExplore` must run off main thread and invalidates the source's Explore InfoMap state; `refreshJSLib` must run off main thread and clears the shared JS library cache; `putConcurrent` updates the source-keyed `ConcurrentRateLimiter`.
- `evalJS` returns `Any?`, uses the pinned Rhino engine and shared JS scope, and binds `java`, `source`, and `sourceApi` to the same `BaseSource` instance, plus `baseUrl = getKey()`, `cookie`, and `cache`. It is not a `String`-only evaluation API.

## 4. Other upstream BaseSource implementers and host-only callsites

Search-only inventory found these direct upstream implementers:

| Type | Ownership | Identity |
|---|---|---|
| `BookSource` | Runtime source model candidate | `bookSourceUrl` |
| `RssSource` | App-only RSS product model; exclude from Runtime | `sourceUrl` |
| `HttpTTS` | App-only TTS product model; exclude from Runtime | `httpTts:$id` |
| `BookCover.CoverRule` | App-only Reader/Cover model; exclude from Runtime | `searchUrl` |
| `SourceNavigationBridge` | App host delegation wrapper, not a data model | delegates `BaseSource` to original and overrides navigation/login methods |

The shared BaseSource contract must use `getKey()` as a generic key, not assume every implementation is a BookSource. These RSS/TTS/Cover models and the Review/Reader/Login/WebView UI callers remain host/product-side; this task does not authorize moving them into Runtime.

Actual callsite families include `AnalyzeRule`/`AnalyzeUrl`, `JsSourceEngine`, `SharedJsScope`, `WebBook`/`BookList`/`BookInfo`/`BookChapterList`/`BookContent`, `SourceLoginV2Delegate`/`SourceLoginRoute`/`SourceLoginDialog`, `SourceVerificationHelp`, `WebJsExtensions`, `ReviewController`/`ReviewDetailDialog`, and `ReadBookActivity`. `AnalyzeRule` contains concrete `BookSource` casts; preserve the concrete model type.

## 5. Required host ports and evidence-derived semantics

| Port / owner | Frozen upstream semantics | Current Badcat status / open requirement |
|---|---|---|
| `SourceCachePort` | Match CacheManager key/value, delete, memory cache, file cache and TTL semantics. CacheManager is App/Room/ACache-owned; preserve source prefixes and global-vs-source key scope. | Current SharedPreferences/ConcurrentHashMap cache is not the same: different namespacing, no equivalent global LRU and different file-cache scope. Freeze memory capacity/eviction and TTL units before implementation. |
| `SourceCookiePort` | Match `CookieStore.getCookie(url)`, `getKey(url,key)`, `setCookie`, `setWebCookie`, `replaceCookie`, `removeCookie`, `clear`; preserve domain normalization, persistent-vs-session cookie merge, WebView CookieManager synchronization and request/response cookie handling. | Current SharedPreferences jar has domain buckets and process session state, but `DefaultProcessCookieBridge` has no App-main caller; no verified Android WebView cookie sync. |
| `SourceSessionPort` (or CachePort-backed adapter) | Store source-keyed login header and encrypted login-info values; BaseSource owns AES/key derivation, nullable/Boolean/error behavior and dynamic Login UI defaults. | Current login extension has 11 methods, but return types differ (`getLoginInfo` non-null String, `putLoginHeader`/`putLoginInfo` return String); dynamic Login UI defaults are absent. SharedPreferences backend is not yet proven equivalent. |
| `SourceHttpPort` | Preserve AnalyzeUrl/StrResponse request options, source header and login-header precedence, cookie policy, redirect/proxy/timeout/retry/charset, body JS, DNS/server ID and WebView fallback. Synchronous JS methods must bridge suspend transport using the active coroutine/navigation context and propagate cancellation. | `SourceRequestClient.execute` exists and OkHttp is wired, but current `SourceHttpRequest` lacks `dnsIp`, `serverID`, `bodyJs`, `webView` and `webViewDelayTime`; SourceRequestFactory explicitly leaves script-generated options outside its scope. |
| `SourceRateLimitPort` or Runtime-owned limiter | `updateConcurrentRate(sourceKey,rate)` plus `ConcurrentRateLimiter.getConcurrentRecord()` semantics; key by generic `BaseSource.getKey()`. | `LegadoConcurrentRateLimiter` exists, but `BaseSource.putConcurrent` is missing from JS source object. Dynamic rate-update parity and malformed-setting behavior require a contract test. |
| `SharedJsScope` (Runtime core, not App port) | Preserve shared lexical/global scope, source identity, library hash invalidation and lifecycle; use direct `androidx.collection` dependency or a compatible Runtime implementation. | Current Rhino runtime keeps separate scope/hash maps; exact upstream SharedJsScope behavior is not established. |
| `ExploreCatalogPort` | Preserve sourceUrl-keyed `LruCache<String,InfoMap>(99)`, InfoMap object identity, dirty-only persistence and invalidation. | No wired Explore catalog port. |
| `AppContextPort` / `RuntimeConfigPort` / `ResourcePort` | Provide Android ID, external-cache root, default User-Agent, WebView UA, block-source-navigation policy, ReadBookConfig, ThemeConfig and source-visible resources. | Missing contracts; do not import `appCtx`, AppConfig or generated App `R` into Runtime. |
| `RuntimeNotificationPort` | Preserve toast/longToast and AppLog levels/formatting. | Missing; `HostUiPort` does not include toast/log. |
| `SourceFilePort` / archive port | Preserve `appCtx.externalCache` path confinement, text/file operations, archive entry lookup, encoding and cleanup. | Current file/archive adapters are stricter and differently rooted; compare all source-visible path semantics and keep implementations in App. |
| `HostUiPort` | `JsExtensions.openUrl` import-confirm/deep-link flow, ShowBrowserDialog, SourceHelp VideoPlay/Media3, navigation suppression and source context. | Declared but no implementation. Existing `HostUiAction` lacks explicit import-confirm/deep-link flow and toast/log/config actions. |
| `HiddenWebViewPort` | BackstageWebView pooled lifecycle, cookie sync, SSL handling, source/override URL capture, JS evaluation/retry, timeout, release and navigation context. | Declared but no implementation. Existing request requires nonblank URL and JavaScript; upstream `webView(html,url,js,...)` allows nullable combinations. |
| `VisibleBrowserPort` / verification coordinator | SourceVerificationHelp lock/single-flight behavior, correlation key, `refetchAfterSuccess`, result capture/refetch and cancellation. | Declared but no implementation. `SourceVerificationHelp` coalesces same source/type/URL requests; simple launch/await without a coordinator is insufficient. |
| `VerificationCodePort` | Await code entry with source context and exact cancel/timeout behavior. | Declared but no implementation. |
| `ProductEventPort` | EventBus refresh book-info/TOC/content events and event ordering. | Declared but no implementation. |
| `BatchContentPort` | Preserve BatchContentContext, URL→chapter mapping, content replacement, ContentSaveToken/version fence and BookHelp atomic save. | Missing. Current `getBatchContext()` is null and `cacheContent()` intentionally throws when no batch context is attached. |
| `SourceExecutionStatePort` | Attach stable per-source Host Services; clear shared JS scope/cache/session/cookie/variables/Explore state after replace/remove; preserve multiple runtime instances. | Missing; factory currently creates FileBookSourceCatalog with a no-op invalidation callback while SourceCatalogActivity separately wires `clearSourceState`. |

The exact upstream dependency-owner implementations were read only for methods directly called by BaseSource/JsExtensions. CacheManager is backed by App database/memory/file caches; CookieStore combines persistent storage and Android WebView session cookies; ConcurrentRateLimiter is keyed by BaseSource.getKey; SourceVerificationHelp owns a lock and in-flight request map; BackstageWebView owns a pooled WebView lifecycle; BatchContentContext delegates content persistence through BookHelp save tokens. Their concrete Android/database/UI implementations stay in App.

## 6. Current Badcat JS binding is not upstream BaseSource ABI

- Upstream `BaseSource.evalJS` binds `java`, `source`, and `sourceApi` to the same BaseSource instance, `baseUrl` to `getKey()`, and separate `cookie`/`cache` objects.
- Current `RhinoSourceScriptRuntime` binds `source` to `RhinoSourceLoginExtensions`, `java` to `RhinoSafeSourceJavaExtensions`, plus separate `cookie` and `cache`; there is no `sourceApi` or `baseUrl` binding. The `source` object is not the source model.
- Current `RhinoSourceLoginExtensions` exposes 11 methods. Fourteen BaseSource method names have no equivalent across current source/java objects: `evalJS`, `evalLoginActionV2`, `evalLoginUiV2`, `getHeaderMap`, `getLoginJs`, `getLoginUiJs`, `hasLogin`, `hasLoginForm`, `isLoginUiV2`, `login`, `putConcurrent`, `putVariable`, `refreshExplore`, `refreshJSLib`.
- Current `RhinoSafeSourceJavaExtensions.getSource()` returns `Map<String,Any?>`, not a BaseSource. `RhinoSourceJsAbiBytecodeTest` explicitly asserts this Map return. This is incompatible with upstream `JsExtensions.getSource(): BaseSource?` and the upstream source/java/sourceApi identity contract.
- Seventeen unique upstream JsExtensions method names are absent from the current Java bridge: `getReadBookConfig`, `getReadBookConfigMap`, `getThemeConfig`, `getThemeConfigMap`, `getThemeMode`, `getVerificationCode`, `openUrl`, `openVideoPlayer`, `refreshBookInfo`, `refreshBookToc`, `refreshContent`, `showBrowser`, `startBrowser`, `startBrowserAwait`, `webView`, `webViewGetOverrideUrl`, `webViewGetSource`.
- Current login signatures also differ: `getLoginHeader()`/`getLoginInfo()` return non-null empty strings rather than nullable values; `putLoginHeader()` and `putLoginInfo()` return strings rather than Unit/Boolean; `setVariable()` returns a string; dynamic Login UI defaults are absent.
- `RhinoHostSurfaceInventoryTest` records an allowlist/inventory but does not compare to the upstream ABI. `RhinoOverloadResolutionTest` checks a synthetic probe rather than all upstream overloads. `RhinoSourceJsAbiBytecodeTest` currently codifies the Map behavior and a fail-closed missing-batch behavior; those tests must be replaced/expanded before claiming parity.
- Current `SourceScriptRuntime.evaluate` takes the shadow `BookSource` DTO, string-only bindings and returns `EngineResult<String>`. Upstream `BaseSource.evalJS` accepts `ScriptBindings` and returns `Any?`. Preserve object/array/null/boolean/number/string values rather than flattening everything to String.

## 7. Host request-context corrections required

- `SourceExecutionContext` currently requires a non-null `Job` and nonblank source name. Upstream `getSourceNavigationContext()` defaults to `EmptyCoroutineContext`, and `getTag()` is nullable; make job/name optional or prove every callsite supplies them.
- Preserve the full active `CoroutineContext`, not only `Job` plus an ALLOW/BLOCK enum. SourceNavigationBridge adds navigation suppression context and `JsExtensions` combines it with the Rhino context for blocking/suspend host calls.
- `HiddenWebViewRequest` must support nullable URL/JavaScript and HTML-only/URL-only combinations from upstream `webView`.
- `VisibleBrowserPort` must preserve SourceVerificationHelp's single-flight/correlation and Refetch-vs-Response result, not only launch/await.
- SourceHttpRequest must add the missing AnalyzeUrl options above. Do not claim HTTP compatibility based only on the current static request subset.

## 8. Acceptance and disposition

- **Architecture decision:** per-instance `BaseSourceHostServicesOwner` sidecar on the exact BookSource instance is selected; no wrapper, global service locator, ThreadLocal service lookup or `Map<String,Any?>` bridge.
- **Implementation status:** NO-GO until the typed ports in §5 are implemented and the ABI/binding/request-context gaps in §§6–7 are closed. The current runtime remains a bounded subset, not full Legado compatibility.
- **Required tests before BookSource migration:** source object identity (`source === java === sourceApi`, `java.getSource() === source`); generic `getKey`; transient attachment excluded from Gson/Room/Parcelable; attach after catalog parse and parcel restore; `copy()`/reattach; multiple Runtime instances and same-source concurrency; exact 175-signature ABI/77 annotations/overload table; login nullable/Boolean/Unit return and AES compatibility; header/login-cookie precedence; CookieStore persistent/session/WebView sync; CacheManager TTL/LRU/global namespace; rate update; Explore LRU/dirty save; hidden WebView lifecycle; visible browser single-flight/refetch/cancel; batch content save tokens; all source HTTP options; Host UI/event ordering.
- **Task 026O:** complete read-only source/API/port closure. No production source/build/test/schema/UI/Reader edit; no Gradle/compiler/build/test/CI/device/network action; no App compile retry.
- **Next:** register the typed BaseSourceHostServices contract and minimal identity/serialization/reattachment proof task. Only after those tests pass, register the BookSource/BaseSource source migration and then port implementation in bounded slices.
- Phase A remains open; Phase B has started only with Task 026M's bounded DTO slice; Task 027 blocked; Stage-0 not accepted; Stage-1 **NO-GO**; full Legado compatibility **NOT ESTABLISHED**.
