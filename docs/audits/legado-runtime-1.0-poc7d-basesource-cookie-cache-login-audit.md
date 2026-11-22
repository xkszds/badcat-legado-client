# Runtime-1.0 PoC-7d — BaseSource/CookieStore/CacheManager/login lifecycle audit

- Branch: `integration/legado-upstream-runtime`
- Registration commit: `23e4c453b57d5c9860a8bd865a284fbcd0989ba8`
- Starting HEAD: `99813860dceda1555328bbc81cd050cfd643e3fe`
- Fixed upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: source/state ownership audit only. No implementation or test changes.

## 1. BaseSource is a live execution object, not a source descriptor

Pinned `BaseSource.kt` is an interface extending `JsExtensions`. It exposes mutable source configuration (`concurrentRate`, `loginUrl`, `loginUi`, `header`, `enabledCookieJar`, `jsLib`), source identity (`getKey`, `getTag`), and executable/stateful behavior:
- login UI v1 parsing and inline JS extraction; Login UI v2 detection/evaluation/action evaluation;
- `login()` which evaluates the configured login script and calls its `login` function;
- dynamic header evaluation, optional login-header merge, login header get/put/remove;
- encrypted login-info get/put/remove and lazy login form defaults;
- persistent source variable and arbitrary per-source key/value get/put;
- `refreshExplore()`, `refreshJSLib()`, concurrent-rate updates and arbitrary `evalJS()`;
- JS bindings for the real `CookieStore` and `CacheManager`, plus source shared scope.

The pinned pure-JS source engine binds the same live source object as `source` and `sourceApi`, and binds `java` to a JsExtensions implementation. It separately binds the global `cookie` and `cache` objects. These names have distinct roles; they are not interchangeable copies of a Map.

### Badcat mismatch

Badcat `RhinoSourceScriptRuntime` materializes `source` as a JS-friendly descriptor Map from imported raw JSON, and separately exposes `RhinoSourceLoginExtensions` as `source`. It does not expose a pinned BaseSource instance or an equivalent complete mutable object. Its `java` helper is allow-listed, and `cookie`/`cache` are separate wrappers.

Consequences:
- `source`/ `sourceApi` do not have the upstream live BaseSource methods/properties or same-object identity.
- Mutable source fields and dynamic behavior are not represented by the descriptor Map.
- Login helpers implemented on the Badcat source wrapper cover only a subset; missing BaseSource methods and return/overload semantics remain.
- Upstream `getSource(): BaseSource?` is still not satisfied by returning a descriptor map or login-only facade.
- No source-object differential test currently proves property mutation, method invocation, null/default behavior or object identity.

## 2. CookieStore: domain-global upstream state vs source-isolated host state

### Pinned upstream behavior
`CookieStore` is a process object implementing `CookieManagerInterface`:
- Persistent OkHttp cookie string is stored in upstream `Cookie` DB rows keyed by registrable/subdomain result from `NetworkUtils.getSubDomain(url)`.
- A process memory cache mirrors persisted cookies; session cookies are separately merged from `CookieManager`.
- `getCookie(url)` combines persistent and session cookies, merges by cookie name and caps the resulting header at 4096 chars by removing cookies.
- `setCookie` writes/overwrites the domain row; `replaceCookie` merges supplied pairs into existing cookie values rather than replacing all cookies applicable to the URL.
- `removeCookie(url)` removes the domain row, memory/session cookie state and Android WebView cookie for that URL.
- `setWebCookie` has a separate Android WebView CookieManager path.
- Cookie operations are domain-based, not book-source-id based. Different sources using the same site can share the same upstream cookie domain state.

### Badcat behavior
Badcat `SharedPreferencesSourceCookieJar` stores a durable OkHttp CookieJar under a sourceId-specific SharedPreferences key. It retains domain/path/expiry/hostOnly/secure/HttpOnly attributes, enforces per-cookie/storage bounds, reloads under a shared lock across jar instances, removes expired cookies, and applies OkHttp URL matching. The runtime's `cookie` wrapper delegates to that source-specific jar. Login-header Cookie import replaces cookies in the source registrable domain; source removal clears the entire source jar.

### Compatibility decision
This is a deliberate security/isolation design, but not upstream behavior parity:
- Same-domain cookies are isolated per Badcat source rather than shared by registrable domain.
- Upstream plain Cookie header persistence and session-cookie merging do not match OkHttp CookieJar Set-Cookie parsing and URL matching.
- Upstream `replaceCookie` merges names; Badcat `replaceCookie` removes all cookies matching the URL before adding parsed Set-Cookie values.
- Upstream `removeCookie` affects domain/session/WebView state; Badcat removal scopes to URL or source jar and does not operate the upstream global WebView cookie store.
- WebView cookie synchronization and session-cookie lifecycle are not equivalent.
- Therefore neither security nor more detailed RFC cookie matching can be used as evidence of Legado compatibility. Need pinned fixture tests for domain/subdomain sharing, path, expiry, session, duplicate names, header merge, replace/remove and WebView synchronization before deciding a safe adapter.

## 3. CacheManager, AppCacheManager and source variable state are separate contracts

### Pinned upstream
- `CacheManager` is a global, arbitrary-key API, not intrinsically source-scoped. String values use Room `Cache` rows plus a 50 MiB process LRU for non-expiring entries; expiring values are disk-backed without memory promotion. `ByteArray` and `putFile/getFile` use ACache file storage. Numeric getters parse or return memory values; `get(key, onlyDisk)` changes lookup behavior. Delete removes Room, memory and file entries.
- `WebCacheManager` exposes a WebView-JS subset of this global cache API.
- `AppCacheManager` is a distinct internal cache: QueryTTF LRU plus source/login/variable memory invalidation prefixes.
- BaseSource source variables, user info and login headers are stored through CacheManager under prefixed keys. User info is AES encrypted using the pinned Android-ID-derived key.
- Shared JS libraries are managed by `SharedJsScope`: compiled library scopes are keyed by MD5(jsLib), weak-reference LRU capped at 16, synchronized scope creation, shared-global-state namespacing, and removal of imported JS library cache files and globals when the library is refreshed/removed.

### Badcat
- The runtime binds `cache` to `RhinoSourceCacheExtensions`, which delegates to source-keyed `SourceScriptKeyValueStore` and `SourceScriptFileCacheStore`, not upstream global CacheManager.
- Production factory injects `SharedPreferencesSourceScriptKeyValueStore` and `FileSourceScriptCacheStore`; the runtime class defaults to in-memory stores when used without production injection.
- Persistent login header/info use a separate `SharedPreferencesSourceLoginSessionStore`; login info is encrypted in the pinned BaseSource Android-ID AES format and legacy plaintext can be read/migrated.
- Per-source Rhino globals, JS library/main-script hashes and extension wrappers are cached by sourceId; clearSourceState serializes against evaluation and clears scope, globals, script stores, files, login state and source cookie jar.
- This is a useful explicit per-source isolation boundary, but does not implement the upstream global CacheManager/WebCacheManager/AppCacheManager APIs, arbitrary shared key namespace, TTL and type behavior, 50 MiB LRU, ACache file API, or SharedJsScope library-content sharing semantics.

## 4. Login lifecycle and state ownership

| State | Pinned upstream owner | Badcat owner | Audit result |
|---|---|---|---|
| Login header JSON | CacheManager key `loginHeader_<sourceKey>` | SharedPreferences login session store | Persistent but different storage/ABI |
| Login Cookie | CookieStore domain row + session/WebView cookie stores | SourceId-specific OkHttp CookieJar | Not equivalent; domain sharing and session semantics differ |
| Login info | CacheManager `userInfo_<key>`, AES encrypted | SharedPreferences store, AES encrypted with compatible key derivation | Cipher format appears intentionally aligned; no byte-level cross-version fixture was run |
| Source variable | CacheManager `sourceVariable_<key>` | SourceScriptKeyValueStore fixed per-source key | Persistent but API/namespace and clear behavior differ |
| Arbitrary `source.get/put` | CacheManager `v_<key>_<name>` | SourceScriptKeyValueStore | Separate per-source store; key collision/TTL semantics differ |
| JS library shared scope | SharedJsScope keyed by JS library hash | Rhino source scope keyed by sourceId | Different reuse/sharing model |
| Runtime cache/file cache | CacheManager + ACache | Source-keyed stores and source cache directory | Different namespace and persistence semantics |

Additional differences:
- Upstream `getLoginInfo()` returns nullable raw JSON; Badcat login wrapper returns an empty string for absent values. Upstream `putLoginInfo` returns Boolean; Badcat wrapper returns normalized JSON string.
- Upstream `putLoginHeader` stores the original header JSON, synchronizes any Cookie field through CookieStore, and returns Unit. Badcat validates/normalizes JSON, writes cookie state through a source-scoped jar and returns normalized JSON.
- Upstream `removeLoginHeader` removes login header and calls CookieStore.removeCookie(sourceKey), which has domain/session/WebView side effects. Badcat removes the login header and invokes source-domain cookie removal only within that source's jar.
- Upstream `getLoginInfoMap` can derive/persist v1 login UI defaults, with a re-entry guard; v2 deliberately returns an empty map. The Badcat V2 service has render/action/checkLogin entry points but does not implement the upstream V1 dynamic RowUi/login WebView lifecycle.
- Upstream `refreshExplore` clears explore-kind cache and refreshes UI-facing state; `refreshJSLib` removes the JS library shared scope and associated cached library files. Badcat source clearing is broader (all per-source runtime state) and is not a substitute for either method.
- Upstream `login()` evaluates configured JS in BaseSource context; Badcat Login UI v2 render/action calls are separate explicit service calls and do not reproduce the full source login extension callbacks, browser/WebView flow or all BaseSource methods.

## 5. State invalidation and lifecycle observations

Badcat's production factory injects durable source cookie jar, login store, key-value store and file cache into the runtime. Its `clearSourceState(sourceId)` takes the per-source Mutex and removes script scope, JS globals, library/main hashes, shared global state, Java/source/cache wrappers, source KV/file cache, physical source cache, login info/header and cookie jar. This is a concrete host cleanup path, but this audit did not find evidence that every source import/update/disable/delete caller invokes it at the correct lifecycle point. Do not infer complete lifecycle wiring from the existence of the method.

Upstream source lifecycle is split across BaseSource, CookieStore, CacheManager, AppCacheManager, SharedJsScope, source configuration and UI/login activities. Updating/removing a source may independently invalidate login headers/info, cookie state, source variables, shared JS globals, explore kinds, JS library cache and rate limiter state. A single Badcat clearSourceState call is not proven equivalent to all these operations.

## 6. Architecture and compatibility decisions

1. Do not create a shadow BaseSource interface or expand the current descriptor Map to imitate methods. The real BaseSource is coupled to JsExtensions, Rhino bindings, CookieStore, CacheManager, AppConfig, SharedJsScope, Book/BookChapter and app lifecycle services.
2. Preserve sourceId isolation as a security candidate, but do not claim it is compatible with upstream domain-global cookies. A future design must choose explicit source-domain sharing/partitioning semantics and validate multi-source behavior.
3. Keep distinct adapters for persistent login header/info, source variables, arbitrary source cache, CookieStore, WebView cookies, CacheManager file/memory APIs and SharedJsScope. Collapsing them into one generic key-value store is lossy.
4. Before implementation, run pinned differential fixtures for BaseSource return types/nullability, login UI v1/v2, cookie merge/session/domain behavior, cache saveTime/onlyDisk/byte/file behavior, source variables, SharedJsScope reuse/invalidation and source update/delete lifecycle.
5. Architecture gate remains: a dedicated Android runtime or controlled upstream core extraction must host the authentic BaseSource and service closure. Android-free host facades alone cannot prove full behavior.

## 7. Result

- Completed: pinned BaseSource/CookieStore/CacheManager/SharedJsScope/LoginUiV2 source audit and Badcat runtime/store/factory mapping.
- Incomplete: exhaustive upstream source lifecycle caller scan across all update/delete/disable flows; byte-for-byte login-info cipher fixture; cookie/cache/shared-scope differential tests; device restart/session/WebView validation; compiled upstream object/ABI parity.
- Actual changes: audit documentation only. No production Kotlin/Java, Gradle, tests, DB/schema, persistence, UI, APK or `main` changed.
- No unit tests, build, CI, source-smoke or device result is claimed.
- PoC-7d is audit-only. BaseSource live object, upstream CookieStore, CacheManager/WebCacheManager and full login lifecycle remain missing or semantically unequal. Full Legado compatibility and B′ remain unpassed.
