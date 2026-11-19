# Pinned Legado JsExtensions compatibility audit

Audit date: 2026-11-19  
Host branch: `integration/legado-upstream-runtime`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9`

## Scope and method

Compared the method-name inventory in the pinned upstream
`app/src/main/java/io/legado/app/help/JsExtensions.kt` against
`app/src/main/java/app/badcat/client/runtime/RhinoSafeSourceJavaExtensions.kt`.
This is a declaration-level inventory, not proof of behavioral compatibility.
Every same-name method still requires signature, overload, return-object,
threading, lifecycle and exception-semantic review.

- Upstream distinct method names found: 81.
- Host bridge distinct method names found: 168.
- Upstream names with no same-name host declaration: 18.
- A same-name declaration is not counted as compatible until its full contract is audited.

## Missing upstream method names

The current host bridge has no same-name declaration for:

- `canOpenSourceUi`
- `getReadBookConfig`
- `getReadBookConfigMap`
- `getThemeConfig`
- `getThemeConfigMap`
- `getThemeMode`
- `getVerificationCode`
- `openUrl`
- `openVideoPlayer`
- `refreshBookInfo`
- `refreshBookToc`
- `refreshContent`
- `showBrowser`
- `startBrowser`
- `startBrowserAwait`
- `webView`
- `webViewGetOverrideUrl`
- `webViewGetSource`

Do not add empty/null/no-op implementations. Each method must be mapped to an actual Badcat capability or retain the exact upstream failure/unsupported semantics for that execution context.

## Blocking same-name contract collisions

### `getSource()`

- Pinned upstream `JsExtensions.getSource()` returns `BaseSource?`.
- Host `RhinoSafeSourceJavaExtensions.getSource()` currently returns `Map<String, Any?>`.
- This is a return-type collision, not a harmless implementation detail. It prevents the host class from directly implementing the upstream interface and means existing scripts/callers may be relying on a host-only descriptor map.
- Required migration: introduce a real source runtime object implementing the upstream source contract; re-audit all script bindings and callers of the map-returning method. If a descriptor map is still needed, expose it under a distinct, explicitly host-owned name and update callers/tests.

### `getBatchContext()` and `cacheContent(...)`

- Upstream returns `BatchContentContext?` and `cacheContent` validates the active batch context, resolves chapter identity and persists through the content-save lifecycle.
- Host currently returns `Any? = null` and throws from `cacheContent` outside a batch.
- This is not batch support. The host must wire the real upstream batch context, save token, chapter-index identity and durable write semantics before claiming these APIs.

### Other same-name methods

The following families require signature and semantic comparison before interface implementation:

- Network: `ajax`, `ajaxAll`, `ajaxTestAll`, `connect`, `get`, `head`, `post`.
- HTTP response wrappers and request/response object shape.
- File/archive: `cacheFile`, `downloadFile`, `getFile`, `readFile`, `readTxtFile`, archive extraction and ZIP/RAR/7z access.
- Encoding/crypto: Base64 overloads, bytes/string charset overloads, URI encoding, digest/HMAC/AES and Chinese conversion.
- Script coordination: `singleFlight`, `lock`, `tick`, `functionThisObj`.
- Context and metadata: `getTag`, `getSourceNavigationContext`, `getWebViewUA`, `getCookie`, `androidId`.
- Batch: `getBatchContext`, `cacheContent`.
- TTF and number helpers: `queryTTF`, `queryBase64TTF`, `replaceFont`, `toNumChapter`, `toURL`.

Overloads must be checked individually. In particular, host HTTP wrappers are host-owned response types and must not be substituted for upstream Jsoup `Connection.Response` or `StrResponse` without an explicit, behavior-preserving adapter.

## Integration risks to carry into the next code change

1. Source object identity: scripts may expect the same mutable upstream `BaseSource` object across rule calls.
2. Source isolation: shared JS scope, persistent variables, login headers, cookies and cache must use the exact source key.
3. Type/ABI: nullable return types, overloads, JavaScript-visible names and Java reflection behavior.
4. Coroutine/threading: upstream blocking/suspend boundaries, navigation context and cancellation.
5. HTTP semantics: automatic redirects versus no-redirect Jsoup calls, response body ownership, headers/cookies and status metadata.
6. UI lifecycle: browser, WebView, verification, video and refresh events must call real host components on valid lifecycle owners.
7. Batch: chapter index within a book is the identity; URL is not unique. Writes need freshness/save-token semantics.
8. Existing callers: all references to the current map-returning `getSource()`, batch methods and host response wrappers must be searched and migrated.

## Status

- [x] Upstream/host method-name inventory generated from the pinned source and current bridge.
- [x] The 18 missing method names recorded.
- [x] The `getSource()` return-type collision and batch-context mismatch recorded.
- [ ] Full overload/signature/behavior matrix not yet complete.
- [ ] No upstream `JsExtensions` interface implementation has been claimed.
- [ ] No upstream `BaseSource`/BookSource lifecycle integration has been claimed.

## Next implementation gate

Do not copy `BookSource.kt` or `BaseSource.kt` into the app as isolated files. First finish the full overload/signature matrix, locate all host callers of the colliding methods, and define real host service implementations for network, cookie/login, source state, cache, browser/WebView, verification, refresh events and batch persistence. Then migrate the existing bridge without breaking its callers, add regression tests for both old and upstream behavior, and only then integrate the pinned upstream source entities.
