# Runtime-3.0 Task 026P — BaseSource typed Host Port signature freeze

**Task:** R3-S0-T026-PHASE-B-HOST-PORT-SIGNATURE-FREEZE-026P  
**Status:** completed as a bounded read-only source/API-to-owner contract inventory. The exact method-to-owner matrix is frozen at source level; production Host Port wiring, compiled App ABI/reflection, cross-module attachment visibility and dynamic parity remain NO-GO.  
**Registration start HEAD:** facdb5f8e7c636680a3047daa5abefddf71d79a3  
**Review HEAD:** 637823a0203e7889df2f57133ff0ca13aa70f005 on local branch integration/legado-upstream-runtime.  
**Pinned upstream:** LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a.  
**Source scope:** read-only pinned upstream and current Badcat source/contracts. No production/build/test/Gradle/workflow/schema/UI/Reader edit or execution was performed.

## 1. Result and exact API accounting

The upstream declaration tables from Task 026O were rechecked against the pinned source and converted into the committed row-by-row matrix:

- BaseSource.kt: 29 public declarations.
- JsExtensions.kt: 118 public declarations.
- JsEncodeUtils.kt: 30 public declarations.
- Total declaration rows: **177**. BaseSource.getSource() and BaseSource.getTag() override the two inherited JsExtensions declarations, leaving **175 unique method signatures**.
- Direct @JavascriptInterface annotations across those three owners: **77** (BaseSource 9, JsExtensions 48, JsEncodeUtils 20).
- Exact source line, declaration signature, annotation flag, Runtime/Host disposition and semantic contract for every declaration are in legado-runtime-3.0-basesource-host-port-signature-matrix.tsv (177 data rows; SHA-256 2d1ff13fb9c3db5ebe474a731818993ca1102ac86743bb2cafb4aa2b967f748d).

This is a source declaration/API inventory, **not** compiled App ABI or Rhino reflection proof. The upstream App compile produced no App class outputs under the existing registered memory stop gate (§24.119); do not convert these source signatures into a claim that compiled descriptors, bridge annotations or Release R8 retention have passed.

## 2. BaseSource method-to-owner contract

Pinned path: app/src/main/java/io/legado/app/data/entities/BaseSource.kt, especially lines 96–425. The matrix is authoritative for each exact declaration; the following groups state the ownership contract.

| Method group | Methods | Required owner / typed boundary | Non-negotiable semantics |
|---|---|---|---|
| Identity | getTag, getKey, getSource | Runtime model | getSource() === this; getKey() is generic, not assumed to be BookSource URL; tag nullability differs between interface and concrete model. |
| Login rule/model logic | getLoginUiJs, isLoginUiV2, getLoginJs, hasLoginForm, hasLogin, evalLoginUiV2, evalLoginActionV2, login | Runtime core + Rhino + typed Book/BookChapter invocation | Keep inline-JS extraction, Login UI v2 protocol, action/state/form bindings, callback/cancellation context, nullable result and thrown error behavior. |
| Header/session | getHeaderMap, getLoginHeader, getLoginHeaderMap, putLoginHeader, removeLoginHeader | Runtime core + RuntimeConfigPort + SourceSessionPort + SourceCookiePort + notification | Default User-Agent; optional login-header merge; header write synchronizes Cookie before cache persistence; remove deletes header then source-domain cookies; preserve partial failure. |
| Encrypted login info | getLoginInfo, getLoginInfoMap, putLoginInfo, removeLoginInfo | Runtime AES logic + AppContextPort.androidId() + SourceSessionPort | Android-ID-derived AES key; get returns null on failure; put returns Boolean; dynamic Login UI defaults and recursion guard remain; not a plain JSON-map store. |
| Source variable namespace | setVariable, putVariable, getVariable | SourceVariablePort | sourceVariable_<sourceKey>; null deletes; get uses empty-string fallback. |
| Named source cache values | put, get | SourceCachePort | Separate v_<sourceKey>_<key> namespace; put returns written value; get uses empty-string fallback. Must not merge with sourceVariable_ or RuleData big values. |
| Explore invalidation | refreshExplore | ExploreCatalogPort | Must run off main thread. It calls clearExploreKindsCache(), which clears Explore result-cache entries; it does not prove eviction of the separate mutable InfoMap object LRU. |
| jsLib invalidation | refreshJSLib | Runtime-owned SharedJsScope plus SourceCachePort/SourceFilePort backing | Must run off main thread; library-hash scope/file/shared-global invalidation is distinct from source-key global invalidation and Explore result clearing. |
| Rate update | putConcurrent | Runtime-owned ConcurrentRateLimiter | Update keyed by generic getKey(); preserve malformed-rate behavior and live limiter state. No Android Host Port is required for the limiter algorithm. |
| JS execution | evalJS | Runtime Rhino + Runtime-owned SharedJsScope + SourceCookiePort + SourceCachePort | Returns raw Any?; binds java, source, sourceApi to the same BaseSource instance; baseUrl = getKey(); separate cookie/cache objects; preserves caller-provided typed bindings and shared-global state. |

The BaseSource declaration itself has Android/Room-independent identity logic mixed with cache, Cookie, Android ID, AppConfig, AppLog, Rhino and coroutine effects. Copying it unchanged without the typed sidecar is not an acceptable Runtime integration.

## 3. JsExtensions method groups

Pinned path: app/src/main/java/io/legado/app/help/JsExtensions.kt, lines 127–1385. Every overload is a separate matrix row. The grouped method names below cover all names in the 118 declaration rows.

| Capability | Methods | Owner / required port |
|---|---|---|
| Source/context/batch | getSource, getTag, getSourceNavigationContext, getBatchContext, cacheContent | Runtime identity/context; BatchContentPort for batch context and save. Carry full CoroutineContext, not only Job. Batch chapter matching uses typed BookChapter/index or a uniquely resolving URL; duplicate URLs must remain ambiguous; preserve ContentSaveToken/version fence and replacement-before-save. |
| Product refresh events | refreshBookInfo, refreshBookToc, refreshContent | ProductEventPort; retain distinct event names and dispatch ordering. |
| AnalyzeUrl HTTP | ajax, ajaxAll, ajaxTestAll, connect | SourceHttpPort + Runtime ConcurrentRateLimiter + RuntimeConfigPort.threadCount(). Preserve synchronous JS bridge, active coroutine/navigation context, test mode, timeout, skip-rate-limit, ordered results and the existing error-to-StrResponse/string behavior. |
| Jsoup HTTP | get, head, post | SourceHttpPort with a Runtime Jsoup-compatible response facade. This is a distinct direct-Jsoup path: redirects disabled, 30s default timeout, optional CookieJar marker and rate limit; preserve Connection.Response script-visible behavior, including multi-value headers and HEAD semantics. |
| Hidden WebView | webView, webViewGetSource, webViewGetOverrideUrl | HiddenWebViewPort. Accept nullable html/url/js combinations; preserve cacheFirst, capture regex, delay, headers, source tag, SSL/cookie synchronization, pooled WebView lifecycle, timeout and release. |
| Visible browser/verification | startBrowser, startBrowserAwait, getVerificationCode, showBrowser | Separate VisibleBrowserPort, verification coordinator and VerificationCodePort; showBrowser also needs host dialog dispatch. Preserve single-flight key (source key + source type + URL), correlation, UI lock, response-vs-refetch outcome, cancellation and default timeouts. |
| Product player/navigation | openVideoPlayer, openUrl | App HostUiPort / product action adapter. Keep Media3/player, deep-link import confirmation, URL confirmation, MIME type, source origin/name/type and navigation suppression out of Runtime. |
| Script/file download/cache | importScript, cacheFile, downloadFile | SourceFilePort + SourceCachePort + SourceHttpPort. Preserve URL-vs-private-file import path, MD5 URL key, TTL seconds, source-aware AnalyzeUrl stream, relative cache path, close and partial-file deletion on error. |
| Cookie | getCookie | SourceCookiePort; URL/domain lookup and optional key lookup, empty fallback. |
| Local file | getFile, readFile, readTxtFile, deleteFile | SourceFilePort; external-cache safe-root confinement, missing-file null/empty behavior, EncodingDetect vs explicit charset and recursive delete. |
| Archive | unzipFile, un7zFile, unrarFile, unArchiveFile, getTxtInFolder, getZipStringContent, getRarStringContent, get7zStringContent, getZipByteArrayContent, getRarByteArrayContent, get7zByteArrayContent | SourceArchivePort + SourceFilePort + (for URL inputs) SourceHttpPort. Preserve ZipInputStream vs libarchive RAR/7z, exact entry lookup, charset detection/override, output naming, folder-read order and deletion. Do not import archive product UI. |
| Font | queryBase64TTF, queryTTF, replaceFont | Runtime font parser + SourceHttpPort + separate SourceFontCachePort. Preserve URL/base64/ByteArray inputs, optional cache, QueryTTF LRU capacity 4, Unicode code-point remapping and filter semantics. |
| Pure conversion/utility | strToBytes, bytesToStr, base64Decode, base64DecodeToByteArray, base64Encode, hexDecodeToByteArray, hexDecodeToString, hexEncodeToString, timeFormatUTC, timeFormat, encodeURI, htmlFormat, t2s, s2t, toNumChapter, toURL, randomUUID | Runtime core; preserve every overload, nullable return and charset/flag/default. |
| Environment/config | getWebViewUA, androidId, getReadBookConfig, getReadBookConfigMap, getThemeMode, getThemeConfig, getThemeConfigMap | AppContextPort + RuntimeConfigPort + only the resource snapshot actually needed. No Android Context, generated R or Reader UI type crosses into Runtime. |
| Notification/log | toast, longToast, log, logType | RuntimeNotificationPort; preserve source tag prefix, UI-thread dispatch, Debug/AppLog routing, original value returned by log, and null/JVM class-name behavior. |
| Rhino concurrency helpers | singleFlight, lock, tick | Runtime-owned SourceLock; preserve source-key prefix, Rhino Function scope/this, timeout bounds, recursion and LRU counter cap. |

### AnalyzeUrl request envelope requirements

Pinned path: app/src/main/java/io/legado/app/model/analyzeRule/AnalyzeUrl.kt, constructor/options at lines 82–132 and 829–1030; execution paths at lines 395–730; cookie and Glide adapter paths at lines 749–804.

A typed HTTP request must retain, where present: method, URL, source identity, ordered headers, body/bodyJs, origin, proxy, read/call timeout, redirect policy, retry, charset, DNS IP, serverID, WebView enablement and delay, test/skip-rate-limit flags, CookieJar selection, response status/reason/final URL/multi-value headers/body bytes/body stream/call time and cancellation/close. The current Badcat SourceHttpRequest omits proxy, dnsIp, serverID, bodyJs, WebView fallback/delay and several AnalyzeUrl behaviors. Do not infer equivalence from a successful basic GET.

## 4. JsEncodeUtils remains Runtime core

Pinned path: app/src/main/java/io/legado/app/help/JsEncodeUtils.kt, lines 22–506. All 30 declaration rows (27 names, including four createSymmetricCrypto overloads) are Runtime-owned pure crypto/encoding surface, not App UI ports: MD5, symmetric/asymmetric crypto factories, Sign, AES/DES/3DES, digest, HMAC and byte/string conversions. Preserve Hutool/Bouncy Castle behavior, algorithm/transformation defaults, nullable key/IV and byte-array return types, annotations and overload resolution. RegexJsExtensions also inherits this helper surface and stays with the Runtime regex/replacement closure; replacement UI/editor remains App-side.

## 5. App-side inherited bridges are separate from the 175 Runtime signatures

These are in the 026P scope because they implement the same base JS class or carry source/login context, but they are not to be copied into Runtime.

- RssJsExtensions.kt (lines 45–319) extends JsExtensions. put/get use the source-keyed generic cache; searchBook, addBook, showPhoto, open and RSS/sort navigation are App product actions. Its AnalyzeRule convenience methods (setContent, setBaseUrl, setRedirectUrl, getStringList, getString, getElement, getElements) remain on this App bridge; do not treat this RSS-derived class as the Runtime java object.
- SourceLoginJsExtensions.kt (lines 21–73) is App-only. Its upLoginData/reLoginView callbacks use weak references; refreshExplore requests a UI refresh; copyText, TTS cache clearing and showBrowser are product UI actions. Login UI v2's real Runtime calls are BaseSource.evalLoginUiV2/evalLoginActionV2 with typed Book/BookChapter, invoked by SourceLoginV2Delegate on its lifecycle scope/IO context.
- WebJsExtensions.kt (lines 21–214) is a distinct WebView page RPC bridge, not normal Rhino source invocation. Its request dispatch runs in Activity lifecycle scope, correlates by ID, stores results in CacheManager memory and calls the WebView JS result callback; upConfig and WebView callbacks remain App-side. Do not fold this page RPC into SourceHttpPort or the BaseSource invocation envelope.
- SourceNavigationBridge.kt wraps BaseSource by original and injects SuppressSourceNavigation into a full CoroutineContext; it delegates getSource() to the original instance. The Runtime context contract must preserve this element and the original object identity.
- SourceLoginV2Delegate.kt/SourceLoginDialog.kt remain App UI. The delegate cancels render/action jobs and timers on destroy, evaluates with typed Book/BookChapter, and applies returned state/error/countdown/close commands. UI callback updates are dispatched to the main thread and must not outlive the dialog.

## 6. Frozen typed Host Port contract surface

The following Kotlin-like signatures freeze the required contract shape. They are not implemented interfaces and do not authorize adding stubs. A concrete port must retain typed values and explicit failure/cancellation, never a universal Map<String, Any?> or callback bag.

    interface SourceCachePort {
        fun put(key: String, value: Any, saveTimeSeconds: Int = 0)
        fun putMemory(key: String, value: Any)
        fun getFromMemory(key: String): Any?
        fun deleteMemory(key: String)
        fun get(key: String, onlyDisk: Boolean = false): String?
        fun getInt(key: String): Int?
        fun getLong(key: String): Long?
        fun getDouble(key: String): Double?
        fun getFloat(key: String): Float?
        fun getByteArray(key: String): ByteArray?
        fun putFile(key: String, value: String, saveTimeSeconds: Int = 0)
        fun getFile(key: String): String?
        fun delete(key: String)
    }

    interface SourceSessionPort {
        fun getLoginHeader(sourceKey: String): String?
        fun putLoginHeader(sourceKey: String, json: String)
        fun removeLoginHeader(sourceKey: String)
        fun getEncryptedLoginInfo(sourceKey: String): String?
        fun putEncryptedLoginInfo(sourceKey: String, encrypted: String)
        fun removeLoginInfo(sourceKey: String)
    }

    interface SourceVariablePort {
        fun getSourceVariable(sourceKey: String): String?
        fun putSourceVariable(sourceKey: String, value: String?)
        fun getValue(sourceKey: String, key: String): String?
        fun putValue(sourceKey: String, key: String, value: String)
    }

    interface SourceCookiePort {
        fun getCookie(url: String): String
        fun getCookieValue(url: String, key: String): String
        fun setCookie(url: String, cookie: String?)
        fun replaceCookie(url: String, cookie: String)
        fun setWebCookie(url: String, cookie: String)
        fun removeCookie(url: String)
        fun removeCookie(url: String, key: String)
        fun mergeCookies(vararg cookies: String?): String?
        fun mergeCookiesToMap(vararg cookies: String?): Map<String, String>
        fun getSessionCookie(domain: String): String?
        fun getCookieNoSession(url: String): String
        fun loadRequestCookies(url: String, explicitCookie: String?, useCookieJar: Boolean): List<SourceHeader>
        fun storeResponseCookies(url: String, setCookieHeaders: List<String>)
        fun applyToWebView(url: String)
        fun clearOkHttpCookieStore() // upstream CookieStore.clear() removes OkHttp DAO rows; it is not a WebView/session-cookie global clear
    }

    interface SourceHttpPort {
        suspend fun execute(request: SourceHttpRequest): SourceHttpResponse
    }

    interface HiddenWebViewPort {
        suspend fun execute(request: SourceWebViewRequest): SourceWebViewResult
    }

    interface VisibleBrowserPort {
        suspend fun launch(request: SourceBrowserRequest): UiDispatchOutcome
        suspend fun awaitVerification(request: SourceVerificationRequest): VerificationOutcome
    }

    interface VerificationCodePort {
        suspend fun awaitCode(request: VerificationCodeRequest): HostPortResult<String>
    }

    interface SourceFilePort {
        fun resolve(relativePath: String): java.io.File
        fun readBytes(relativePath: String): ByteArray?
        fun readText(relativePath: String, charsetName: String? = null): String
        fun delete(relativePath: String): Boolean
        fun writeFrom(relativePath: String, input: java.io.InputStream): String
    }

    interface SourceArchivePort {
        fun decompress(relativePath: String): String
        fun readEntry(bytes: ByteArray, format: ArchiveFormat, entryPath: String): ByteArray?
    }

    interface AppContextPort {
        fun androidId(): String
        fun externalCacheRoot(): java.io.File
        fun isMainThread(): Boolean
        fun defaultUserAgent(): String
        fun webViewUserAgent(): String
    }

    interface RuntimeConfigPort {
        fun requestThreadCount(): Int
        fun blockSourceNavigation(): Boolean
        fun readBookConfigJson(): String
        fun readBookConfigMap(): Map<String, Any>
        fun themeMode(): String
        fun themeConfigJson(): String
        fun themeConfigMap(): Map<String, Any?>
    }

    interface RuntimeNotificationPort {
        fun toast(sourceTag: String?, message: String, long: Boolean)
        fun log(sourceKey: String?, message: String?, error: Throwable? = null, persist: Boolean = true)
    }

    interface HostUiPort {
        suspend fun openUrl(request: OpenUrlRequest): UiDispatchOutcome
        suspend fun showBrowser(request: SourceBrowserRequest): UiDispatchOutcome
        suspend fun openVideo(request: OpenVideoRequest): UiDispatchOutcome
    }

    interface ProductEventPort {
        suspend fun publish(context: SourceExecutionContext, event: ProductRefreshEvent): UiDispatchOutcome
    }

    interface BatchContentPort {
        fun current(source: BaseSource): BatchContentContext?
        fun save(source: BaseSource, chapter: BatchChapterReference?, content: String): Boolean
    }

    interface ExploreCatalogPort {
        suspend fun <T> withSourceLock(sourceUrl: String, block: suspend () -> T): T
        fun getInfoMap(sourceUrl: String): InfoMap // sourceUrl-keyed object LRU capacity 99; stable mutable identity
        fun saveDirtyInfoMaps() // persist dirty objects from a snapshot
        fun getResults(sourceUrl: String, exploreUrl: String): List<ExploreKind>?
        fun putResults(sourceUrl: String, exploreUrl: String, results: List<ExploreKind>)
        fun clearResults(sourceUrl: String, exploreUrl: String? = null)
        fun invalidateInfoMap(sourceUrl: String)
    }

    interface SourceExecutionStatePort {
        fun attach(source: BaseSource): BaseSource
        suspend fun invalidateSource(sourceKey: String, reason: SourceInvalidationReason)
        suspend fun invalidateJsLibrary(jsLib: String?)
    }

    interface SourceFontCachePort {
        fun get(key: String): QueryTTF?
        fun put(key: String, value: QueryTTF)
        fun clear()
    }

    interface SourceImageTransportPort {
        suspend fun open(request: SourceImageRequest): SourceImageResponse
    }

Explore lock caveat: upstream Explore result locking uses a per-source Mutex, but the mutex registry itself is a lazily initialized plain HashMap. The contract preserves the observed source-keyed lock and does not promise registry thread-safety until a concurrent race test proves it. The separate ExploreAdapter InfoMap object LRU has capacity 99 and dirty objects are persisted on lifecycle save; clearing Explore result cache must not evict those objects.

Additional frozen ownership rules:

1. ConcurrentRateLimiter and SourceLock stay Runtime-owned pure Kotlin/coroutine components; do not add redundant App ports for them. Their configuration input (thread count/navigation policy) comes through RuntimeConfigPort.
2. SharedJsScope stays Runtime core. Its cache/file effects use the explicit cache/file ports; source-key global invalidation and jsLib-hash-wide invalidation remain separate operations.
3. SourceImageTransportPort is required by the AnalyzeUrl/Glide seam, but its request/response DTOs must be frozen in the image-specific task: source identity, Book binding, analyzed URL, headers/cookie policy, bytes/stream, decode kind, cancellation, response/stream close and decode failure. Glide/Drawable/Bitmap/GIF stay App-only.
4. SourceFontCachePort is distinct from generic CacheManager: the upstream QueryTTF cache is a four-entry typed LRU. It may be implemented by the App host, but no QueryTTF object should be serialized into the string cache.
5. RuleVariablePort for RuleData big values, Book/Chapter repositories, SearchBook persistence, catalog mutation origins, replacement rules and migration journals remain separate contracts from these BaseSource APIs; their previously registered owners/gates remain in §§24.82–24.91 and Task 027 stays blocked.
6. Source HTTP request/response DTOs must support multi-value response headers and body stream ownership. AnalyzeUrl URL options include proxy, DNS IP, serverID, bodyJs, WebView fallback/delay, retry, redirect, charset, method, origin, timeouts and CookieJar behavior. Jsoup direct get/head/post must remain a distinguishable request profile and return a compatible Connection.Response view.
7. Browser verification must preserve the source/type/URL flight key, response-vs-refetch result, shared-flight leases, UI lock, timeout and cancellation. Hidden WebView must allow nullable URL/HTML/JS combinations and own cookie/SSL/pool/release lifecycle. Cookie transport must merge explicit Cookie, persistent and session cookies, persist multi-value Set-Cookie responses and synchronize Android WebView cookies without conflating the OkHttp DAO clear with WebView/session clearing.

## 7. Per-source attachment and identity: remaining cross-module blocker

Task 026O selected a same-object per-source Host Services sidecar, not a wrapper, global service locator, ThreadLocal or map bridge. This remains the only source-level direction compatible with final BookSource and AnalyzeRule's concrete BookSource casts.

However, the selected 026O sketch used an **internal** owner interface. Kotlin internal is module-scoped and cannot be called by the App module as a normal source API. The cross-module attachment contract therefore remains unresolved. Candidate shape is a public Kotlin-visible but JVM-synthetic owner/accessor (or another explicitly public typed attachment API), with the Host Services object excluded from Room, Parcelize and Gson. This must be proven by a focused Kotlin cross-module compile, javap -p -v, Gson serialization, Room field processing, Parcelize round-trip and Rhino method enumeration before migration; @JvmSynthetic alone is not proof that Rhino reflection cannot discover it.

The owner/reattachment tests must cover:

- the exact same BookSource object passed to AnalyzeRule/AnalyzeUrl/JsSourceEngine and returned by getSource();
- java, source, sourceApi identity equality;
- body-property sidecar excluded from the 33 primary constructor fields, URL-based equality/hashCode, copy, Gson, Room schema and Parcelable payload;
- reattach after catalog parse/import, deserialization and parcel restore; reattach after BookSource.copy() because a body property is not copied;
- multiple runtime instances, same-source concurrent execution, source replacement/delete and stale in-flight script behavior;
- SourceNavigationBridge delegation resolving the original source's services.

All direct BaseSource implementers were confirmed: BookSource (Runtime candidate), RssSource, HttpTTS, BookCover.CoverRule (App product models), plus SourceNavigationBridge (delegating App wrapper). Moving BaseSource into Runtime must not break the App-only implementers; their attachment/compatibility path is an explicit unresolved item. Do not assume only BookSource implements BaseSource.

The existing SourceExecutionContext is insufficient as-is: it requires a nonblank source name and non-null Job, while upstream getTag() can be nullable and getSourceNavigationContext() defaults to EmptyCoroutineContext. It carries only Job plus an ALLOW/BLOCK enum and loses the full CoroutineContext element used by SourceNavigationBridge. The replacement context must carry generic source key, nullable tag, source type, caller kind, operation identity and the full active CoroutineContext; cancellation exceptions must propagate rather than become ordinary failures.

## 8. Current Badcat production gap and explicit non-equivalence

- Existing RuntimeHostPort.kt has five declarations (HostUiPort, HiddenWebViewPort, VisibleBrowserPort, VerificationCodePort, ProductEventPort) and no production App implementers/injection sites were found in the inspected tree.
- BadcatReadingEngineFactory currently creates the string-map SourceScriptRuntime and constructs FileBookSourceCatalog with a no-op default invalidation callback; SourceCatalogActivity separately wires clearSourceState.
- RhinoSourceScriptRuntime injects RhinoSourceLoginExtensions as source, RhinoSafeSourceJavaExtensions as java, and separate cookie/cache objects; it does not provide sourceApi or baseUrl. java.getSource() returns a Map. Its JS login bridge has 11 methods, not the 175-signature upstream contract.
- Current SourceRuntime.evaluate is shadow BookSource + Map<String,String> → EngineResult<String>; upstream BaseSource.evalJS is typed ScriptBindings → raw Any?.
- Current SourceHttpRequest omits AnalyzeUrl proxy/DNS/serverID/bodyJs/WebView fallback/delay and other exact options. Current login-session adapter return/null semantics differ. Existing file/chapter cache is not RuleData big-value storage. The process cookie bridge has no verified App-main caller and no verified Android WebView synchronization.
- These are source-level mismatches. No build, test, DEX/R8, device or dynamic behavior test was run under 026P.

## 9. Required differential/ABI acceptance before BaseSource or BookSource migration

1. Full source-vs-compiled API comparison for all 175 unique signatures: descriptors, overloads, default/synthetic/bridge methods, nullable returns, annotations (77), inherited methods and Rhino-visible reflection.
2. Cross-module owner attach/accessor visibility, no unintended JS-visible methods, Gson/Room/Parcelize exclusion and reattachment after copy/import/restore.
3. Cookie persistent/session/WebView synchronization, explicit Cookie merge, header precedence and login-header ordering.
4. CacheManager generic memory 50 MiB LRU, typed QueryTTF four-entry LRU, disk/file namespaces, TTL seconds, onlyDisk and deletion behavior.
5. Source variables vs v_<sourceKey>_<key> vs RuleData big values remain three separate domains.
6. AnalyzeUrl request option differential matrix and Jsoup get/head/post response compatibility; cancellation, stream close, bodyJs, redirects, retry, DNS/proxy/serverID and WebView fallback.
7. Hidden WebView lifecycle/SSL/cookie/capture tests; visible verification single-flight/correlation/refetch/cancel; image transport/decode and stream cleanup.
8. Explore result cache vs InfoMap object LRU identity/dirty-save/invalidation; concurrent same-source vs different-source execution; source delete/recreate and stale InfoMap.
9. Login UI v2 typed Book/BookChapter bindings, action results, nullable/default behavior, main-thread callbacks, lifecycle cancellation and no duplicate completion.
10. Batch content chapter identity/index, duplicate URL rejection, replacement and ContentSaveToken/version-fence behavior.
11. File root confinement, URL download partial cleanup, archive entry/charset semantics, notification/product event ordering and source navigation suppression.
12. Release R8/DEX retention for inherited/annotated bridge methods. Badcat currently has no established Release minification configuration; no R8 claim is available.

No further upstream App compile retry is authorized under §24.119. No dynamic test was run in 026P.

## 10. Disposition

- 026P static source/API mapping: **complete** (177 declaration rows / 175 unique signatures mapped).
- Typed port implementation and per-source attachment: **NOT MET**.
- Cross-module owner visibility and BaseSource implementer migration: **NOT MET**.
- Compiled App ABI/reflection and Release R8: **NOT MET**.
- Dynamic HTTP/Cookie/cache/Explore/WebView/browser/image/login/batch lifecycle parity: **NOT MET**.
- BookSource/BaseSource migration: **NO-GO** until a separately registered owner/attachment proof task and the required ABI/differential evidence pass.
- Task 027: **blocked**.
- Stage-0: in_progress / not accepted; Stage-1: **NO-GO**; complete Legado compatibility: **NOT ESTABLISHED**.

No production source, Gradle, test, workflow, schema, UI or Reader file was changed. No build, test, dependency resolution, DEX/R8, CI or device action was performed.
