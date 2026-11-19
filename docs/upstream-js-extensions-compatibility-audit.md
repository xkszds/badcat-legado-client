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


## 2026-11-19 full declaration and inherited-encoding audit

The earlier 81-name comparison is now expanded from the pinned source declarations rather than name-only counts.

### Declaration counts

- Pinned `JsExtensions.kt`: 122 function declarations across 81 distinct names.
- Host `RhinoSafeSourceJavaExtensions.kt`: 214 function declarations across 168 distinct names.
- Pinned inherited `JsEncodeUtils.kt`: 30 declarations across 27 distinct names.
- The host class is not a subtype of upstream `JsExtensions`; same-name methods are not sufficient to establish interface compatibility.

### Exact same-name contract differences observed

| API | Pinned upstream | Current host | Result |
|---|---|---|---|
| `getSource()` | `BaseSource?` | `Map<String, Any?>` | Blocking return-type collision |
| `getTag()` | `String?` | `String` | Nullability / contract mismatch |
| `getBatchContext()` | `BatchContentContext?` | `Any?` | Wrong runtime type; no batch implementation |
| `ajaxAll` | `Array<String>` overloads returning `Array<StrResponse>` | `Any?` input and `Array<RhinoSourceHttpResponse>` | Input and response ABI differ |
| `ajaxTestAll` | Same upstream array/response contract | Host response facade | ABI differs |
| `connect` | `String?` header overload and upstream `StrResponse` | `Any?` headers and host response facade | Overload and response contract differ |
| `get/head/post` | Jsoup `Connection.Response` | `RhinoSourceHttpResponse` | Return type and request-object behavior differ |
| `getFile` | `java.io.File` | `RhinoSourceFileHandle` | Return type deliberately sandboxed; not source-compatible |
| `log` | `Any?` | `String` | Return contract differs |
| `logType` | `Unit` | `String` | Return contract differs |
| `hexEncodeToString` | `String?` | `String` | Nullability differs |
| `timeFormat` | `Long` | `Number` and `String` | Host overloads differ |
| `timeFormatUTC` | `Long` | `Number` and `String` | Host overloads differ |
| `htmlFormat` | non-null `String` args | nullable `String?` args | Nullability and behavior require audit |
| `toURL` | `JsURL` | `RhinoLegadoUrl` | Different object type / JS-visible methods |
| `replaceFont` | exact four-arg and three-arg overloads | Similar overloads | Verify return semantics and null handling |
| `ajax` | `Any` input, nullable String result, Long timeout overload | String and nullable Any overloads | Overload/nullability mismatch |

The upstream `get`, `head`, and `post` methods intentionally return Jsoup `Connection.Response`; the host facade must not be passed off as that object. The host file sandbox intentionally returns `RhinoSourceFileHandle` rather than exposing unrestricted `File`; this is a deliberate security boundary and requires an explicit compatibility strategy rather than silently changing it.

### Missing upstream methods (18 names)

`canOpenSourceUi`, `getReadBookConfig`, `getReadBookConfigMap`, `getThemeConfig`, `getThemeConfigMap`, `getThemeMode`, `getVerificationCode`, `openUrl`, `openVideoPlayer`, `refreshBookInfo`, `refreshBookToc`, `refreshContent`, `showBrowser`, `startBrowser`, `startBrowserAwait`, `webView`, `webViewGetOverrideUrl`, `webViewGetSource`.

### Inherited JsEncodeUtils gaps (19 names)

The host has no same-name declaration for these pinned inherited APIs:

- `aesBase64DecodeToByteArray`
- `aesDecodeArgsBase64Str`
- `aesDecodeToByteArray`
- `aesDecodeToString`
- `aesEncodeArgsBase64Str`
- `aesEncodeToBase64ByteArray`
- `aesEncodeToBase64String`
- `aesEncodeToByteArray`
- `aesEncodeToString`
- `createAsymmetricCrypto`
- `createSign`
- `desBase64DecodeToString`
- `desDecodeToString`
- `desEncodeToBase64String`
- `desEncodeToString`
- `tripleDESDecodeArgsBase64Str`
- `tripleDESDecodeStr`
- `tripleDESEncodeArgsBase64Str`
- `tripleDESEncodeBase64Str`

The host's current `RhinoLegadoSymmetricCrypto` is an allow-listed AES/DES CBC helper, not the pinned Hutool-backed `SymmetricCryptoAndroid` API. It does not cover arbitrary transformations, 3DES, asymmetric crypto or signing. Do not map these missing methods to the narrow AES helper or return dummy objects. Inspect the exact pinned `AsymmetricCrypto`, `Sign`, and `SymmetricCryptoAndroid` implementations and dependency versions before porting.

### Existing host callers and old behavior

- `sourceDescriptor` is injected into `RhinoSafeSourceJavaExtensions` and exposed by its map-returning `getSource()`. This API cannot coexist with upstream `getSource(): BaseSource?` under the same Kotlin/JVM signature.
- The source object exposed to scripts must be a stable per-source runtime object, not a freshly synthesized map. The host descriptor, if retained for diagnostics/UI, needs a separately named accessor.
- The host response facade is used by the host's own request/JS wrapper API. Migrating to upstream response types must preserve status, message, headers, final URL, body bytes, charset, request metadata and body ownership for existing scripts.
- The host file handle enforces a private root and virtual cache. Replacing it with raw `File` would widen filesystem access and violate the host security boundary.
- The host batch API currently has no injected batch context or persistence writer; existing fail-fast behavior must remain explicit until the real chapter-save lifecycle is wired.

### Compatibility risks before any implementation

1. Binary/source overload collisions across Kotlin defaults and JavaScript reflection.
2. Rhino-visible object type shape, not just Kotlin method names.
3. Upstream Hutool crypto transformation, key/IV parsing, Base64 behavior, asymmetric key formats and signature defaults.
4. The deliberate sandbox boundaries for filesystem, browser and Android UI.
5. Stable source identity and per-source shared JS scope.
6. Existing host-specific scripts that may rely on `sourceDescriptor`, `RhinoSourceHttpResponse` or `RhinoSourceFileHandle`.

### Updated next gate

- [x] Full function declaration count collected for `JsExtensions` and the host bridge.
- [x] Inherited `JsEncodeUtils` inventory collected.
- [x] Return-type and response/file facade conflicts recorded.
- [ ] Search and migrate every existing `getSource()` / descriptor caller before changing its return type.
- [ ] Port and test exact upstream crypto classes and their dependencies, including legacy helper semantics, without weakening input bounds.
- [ ] Build a stable upstream-compatible source runtime object with real host service injection.
- [ ] Only after that integrate `BaseSource` / `BookSource` and continue to `AnalyzeUrl` / `AnalyzeRule`.

## 2026-11-19 pinned crypto dependency and host call-path audit

### Exact upstream dependencies
- Pinned upstream `app/build.gradle` declares `implementation(libs.hutool.crypto)`, `implementation(libs.bouncycastle.provider)`, and `implementation(libs.bouncycastle.pkix)`.
- Pinned `gradle/libs.versions.toml` resolves Hutool to `cn.hutool:hutool-crypto:5.8.22`, Bouncy Castle provider to `org.bouncycastle:bcprov-jdk18on:1.86`, and PKIX to `org.bouncycastle:bcpkix-jdk18on:1.86`.
- These are source-verified dependency coordinates/versions; the Badcat host `app/build.gradle.kts` currently has none of these three dependencies.

### Pinned source contracts inspected
- `JsEncodeUtils.createSymmetricCrypto` returns Hutool `SymmetricCrypto`, instantiating the upstream `SymmetricCryptoAndroid` and applying non-empty IV bytes. Overloads accept nullable byte-array key/IV, byte-array key, string key, and string key plus nullable IV.
- `SymmetricCryptoAndroid` extends Hutool `SymmetricCrypto`; it overrides Base64 output to use upstream `EncoderUtils.base64Encode` (Android `Base64.NO_WRAP`) and `decrypt(String)` to classify all-hex input with the exact `String.isHex()` predicate, otherwise decode Base64.
- `AsymmetricCrypto` extends Hutool `AsymmetricCrypto`, adds byte/string public/private key setters, and accepts ByteArray/String/InputStream data for encrypt/decrypt plus Base64/hex output and a public/private key selector.
- `Sign` extends Hutool `Sign` and adds byte/string public/private key setters.
- Pinned legacy AES/DES/3DES methods contain source-specific quirks; preserve exact source behavior, including `aesEncodeToString` calling `decryptStr`, `desBase64DecodeToString` delegating to `decryptStr`, and the differing Base64 handling of legacy AES/3DES argument helpers. Do not silently normalize these.

### Existing host behavior and callers inspected
- Host app module currently depends on Gson, Flexbox, Jsoup/XPath/JSONPath, coroutines, OkHttp, archive and Chinese conversion libraries, but not Hutool or Bouncy Castle.
- `RhinoSafeSourceJavaExtensions.createSymmetricCrypto` currently returns the host-only `RhinoLegadoSymmetricCrypto`; only two non-null key/IV overloads are present. That helper permits AES/DES CBC PKCS5/PKCS7 only. `aesCrypt` is a separate bounded AES-CBC primitive used by the JavaImporter compatibility shim.
- `RhinoSourceScriptRuntime` constructs one bridge per source and binds it as `scope["java"]`; scripts therefore call these methods dynamically through Rhino.
- Existing runtime tests cover `java.createSymmetricCrypto` AES round-trip, DES-CBC decryption, `java.aesBase64DecodeToString`, and JavaImporter AES key/IV restrictions. Preserve these tests and the JavaImporter package/class boundary.
- GitHub code-search returned zero results with `incomplete_results=true`, so it is not evidence that no other callers exist. The known in-repository dynamic script call sites are in `RhinoSourceScriptRuntimeTest.kt`; a complete local/tree grep is still required before changing signatures.

### Implementation risks / next registered task
- Do not expose unrestricted `Packages.javax.crypto`, `java.lang`, or general reflection to scripts. Add the upstream crypto factories through the source-scoped `java` bridge only, and keep the existing restricted JavaImporter behavior.
- Preserve existing AES/DES script behavior while changing factory return type to an upstream-compatible Hutool object; audit all host callers and Rhino method visibility.
- Next task (not yet implemented): add only the pinned Hutool/BC dependencies, vendor the exact three upstream crypto classes with the minimal exact Base64/isHex helper semantics, implement the full pinned `JsEncodeUtils` crypto factory/legacy method surface, and add upstream-behavior plus old-host regression tests. Verify RSA/signing and transformation support against the pinned dependency behavior; do not claim complete JS host compatibility from this slice.
- Before code edits, run a complete repository grep for all crypto method names and current helper types, then update this audit/checklist if new callers or behavior conflicts are found.

### Further host call-site/security review

- Scanned all 23 Kotlin production files under `app/src/main/java/app/badcat/client/runtime/` and all 29 Kotlin test files under `app/src/test/` for the relevant crypto method/helper identifiers.
- Existing dynamic source-rule call sites are in `RhinoSourceScriptRuntimeTest.kt`: AES `createSymmetricCrypto` round-trip, DES-CBC decryption, `aesBase64DecodeToString`, digest/HMAC, and JavaImporter AES boundary tests.
- `RhinoSourceScriptRuntime.kt` binds the per-source bridge instance as `scope["java"]`; separately, `LEGADO_CRYPTO_COMPAT_JS` installs a narrow Packages/JavaImporter shim. Keep the JavaImporter boundary intact when adding upstream crypto factories.
- `RhinoLegadoJsReplacementEvaluator.kt` independently implements digest/HMAC helpers; these are separate replacement-script semantics and must be regression-tested rather than silently redirected to the new Hutool bridge.
- Remaining caller-search limitation: app UI directories, integration-contract, and legado-rhino have not been scanned. GitHub code search was incomplete, so no claim of a full repository grep is made.

### Complete repository caller scan (2026-11-19)

- Scanned all 231 Kotlin files and all 21 Java files in the current repository tree, including app UI/models, app runtime/tests, integration-contract main/tests, pinnedLegado source bridges, and legado-rhino.
- No additional static Kotlin/Java references to `createSymmetricCrypto`, `RhinoLegadoSymmetricCrypto`, the missing AES/DES/3DES helpers, `createAsymmetricCrypto`, `createSign`, or the corresponding crypto helper names were found outside the already recorded bridge and runtime test locations.
- The source bridge is instantiated per source and bound as `scope["java"]`; this must remain source-scoped.
- `RhinoLegadoJsReplacementEvaluator` owns an independent digest/HMAC implementation. Preserve and regression-test it independently.
- `LEGADO_CRYPTO_COMPAT_JS` is a separate narrow JavaImporter shim. Keep its class/package allow-list unchanged; adding Hutool must not grant arbitrary Java class access.
- This closes the previously open caller-scan scope for Kotlin/Java sources. Documentation references are not runtime call sites.


### 2026-11-19 crypto runtime implementation delta (verification pending)

- Added pinned upstream crypto classes and helper semantics under `io.legado.app.help.crypto` / `io.legado.app.utils`.
- Added Hutool 5.8.22 and Bouncy Castle 1.86 provider/PKIX dependencies to `app/build.gradle.kts`.
- The source bridge now returns Hutool `SymmetricCrypto` backed by `SymmetricCryptoAndroid`, and exposes upstream `AsymmetricCrypto`, `Sign`, and the missing legacy AES/DES/3DES methods.
- Removed the obsolete host-only `RhinoLegadoSymmetricCrypto` implementation. The separate restricted `aesCrypt` method and `LEGADO_CRYPTO_COMPAT_JS` allow-list are retained.
- Added Rhino tests for AES/legacy quirks, DES/3DES, random-key and overload semantics, RSA and signing. The independent replacement evaluator digest/HMAC path is unchanged.
- Pending: compile/test/CI and actual Android crypto provider/runtime verification. Same-name methods and Rhino reflection are not marked compatible until those checks pass.
