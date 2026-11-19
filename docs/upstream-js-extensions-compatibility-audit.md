# Pinned Legado JsExtensions compatibility audit

Audit date: 2026-11-19  
Host branch: `integration/legado-upstream-runtime`  
Pinned upstream: `LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

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


### Crypto host resource-boundary correction (must precede further code changes)

- The previous host `RhinoLegadoSymmetricCrypto` capped input and output at 2 MiB. The first upstream Hutool migration removed that facade, so the new returned object is not yet bounded.
- The separate `aesCrypt` method used by the JavaImporter shim remains capped, but it does not bound `createSymmetricCrypto` objects.
- Before proceeding, add a bounded subclass/wrapper for the upstream `SymmetricCryptoAndroid`, `AsymmetricCrypto` and `Sign` surfaces. Preserve algorithms, overloads and upstream transformations; cap data/key material and results at 2 MiB, including InputStreams, without closing caller-owned streams.
- Add exact-limit/over-limit and stream forward-progress tests while retaining AES/DES/3DES/RSA/signature tests and the JavaImporter sandbox.
- This omission was not captured in the first implementation checklist; the checklist is corrected here before the next runtime edit.


### Crypto resource-boundary implementation delta

- The previous 2 MiB host resource boundary is now preserved around the upstream Hutool objects: bounded symmetric subclass, bounded asymmetric wrapper and bounded Sign wrapper.
- InputStreams are bounded during reads, zero-progress reads fall back to a single-byte read, and caller-owned streams are not closed.
- The `java.createSymmetricCrypto` bridge constructs the bounded upstream-compatible subclass; supported algorithms/transformation behavior is not reduced to the former AES/DES-only list.
- Added exact-limit, over-limit, output-overflow, stream, asymmetric-key and signature-input tests. Verification is pending CI.


### Post-commit correction required before crypto verification

- The bridge's `createSymmetricCrypto` still instantiates `SymmetricCryptoAndroid`; the bounded wrapper is not yet wired. Correct the factory and key/IV checks.
- The bounded test insertion corrupted the preceding overload test assertion. Rebuild that test section cleanly.
- This is a review finding, not a passing test. The next code edit must correct both defects before relying on CI.


### Bridge/test wiring correction applied

- `createSymmetricCrypto` now instantiates the bounded upstream-compatible subclass after validating key and IV sizes.
- The damaged overload test source was repaired and the bounded crypto test is now a separate method.
- Asymmetric string ciphertext is decoded through `SecureUtil.decode` and bounded before decryption.
- CI is still pending; these corrections are not a test pass.


### 2026-11-20 current-head crypto verification gate (pre-registered)

- Current host branch HEAD before ledger update: `f924681f783628dedf6f835e191a701ba5ae6a16`; pinned upstream: `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Run `44564284230` targets ancestor `24c39c4...` and must not be reported as verification of the current asymmetric decoded-input correction.
- Run `44564458448` targets current HEAD and was pending/no jobs at inspection. Poll it and inspect full job logs/steps, test reports and APK artifact.
- Verify the decoded Base64/Hex ciphertext bound as well as symmetric/asymmetric/signature bounds, stream ownership/progress, legacy helpers, JavaImporter restrictions and replacement-evaluator isolation.
- No additional runtime code change is authorized by this entry. If a defect is found, append a specific fix/test task to the required checklist before editing.
- The crypto increment remains pending until current-HEAD CI is successful. Physical-device crypto/provider checks and the remaining upstream runtime are still incomplete.


## 2026-11-20 encoded crypto output-bound correction — PRE-REGISTERED BEFORE CODE EDIT

### Finding from current-head source review
- The 2 MiB boundary checks raw byte-array outputs in `RhinoBoundedSymmetricCryptoAndroid.encrypt/decrypt` and `AsymmetricCrypto.encrypt/decrypt`, but inherited/explicit Base64 and Hex encoders can expand those bounded byte arrays into strings larger than 2 MiB.
- Confirmed affected surfaces: Hutool `SymmetricCrypto` encoded helpers (`encryptBase64` / `encryptHex`, including ByteArray, String/Charset and InputStream overloads) and host `AsymmetricCrypto.encryptBase64/encryptHex`. The upstream Hutool API exposes these as separate string-returning helpers; bounding only the byte-returning primitive does not bound the returned string.
- This is a confirmed resource-boundary omission, not a CI failure and not a claim that current CI has passed.

### Registered implementation scope
- Keep all upstream algorithms, overloads, Base64 encoding semantics, Hex semantics, transformations, and source-scoped Rhino exposure unchanged.
- Enforce a maximum of 2 MiB on encoded string results returned to Rhino, after encoding, for every supported symmetric and asymmetric Base64/Hex helper path.
- Preserve raw byte-array 2 MiB bounds, decoded Base64/Hex input bounds, caller-owned InputStream ownership, zero-progress handling, key/IV bounds, and Sign behavior.
- Audit inherited Hutool overloads and Rhino reflection visibility so no overload bypasses the output check. Do not narrow the algorithm allow-list or alter JavaImporter package/class allow-list.

### Required regression coverage / acceptance
- For symmetric Base64 and Hex: test each applicable ByteArray, String and InputStream overload at/below the encoded-output limit and over the limit; include padding expansion and a deterministic large payload.
- For asymmetric Base64 and Hex: test encoded output limit and over-limit behavior with a deterministic supported key fixture where feasible; retain RSA encrypt/decrypt tests.
- Retain exact 2 MiB raw-output, raw-input, decoded ciphertext, key/IV, stream forward-progress/ownership, DES/3DES, signature, legacy-helper, JavaImporter sandbox and replacement-evaluator regression tests.
- Run focused crypto/Rhino tests, full app unit tests, Debug APK assembly, and inspect current-HEAD CI job steps/logs/reports/artifacts. Do not mark passed while CI is pending or logs are unavailable.
- Review full diff and confirm no bypass via inherited overloads; device/provider verification remains a separate open gate.

### Execution discipline
- This task is registered before any runtime code edit. Only after this ledger commit may the bounded encoder overrides/wrappers and regression tests be changed.
- After implementation, update all relevant checklists with exact files, test commands/results, CI run/head/artifacts, commit SHA, remaining device and full-Legado work, and a self-review of whether this registration captured all affected overloads.


## 2026-11-20 restore pinned Legado Chinese conversion runtime — PRE-REGISTERED AFTER CI FAILURE REVIEW

### Confirmed CI failure and source audit
- Current-head predecessor CI run `44564706186` (head `d140b35d614cbad2afe8cffa81f106aaab639a1d`) failed at `:app:compileDebugKotlin` with `RhinoSafeSourceJavaExtensions.kt:1934:38 Unresolved reference 'LegadoChineseConversion'`; the APK step was skipped and no APK artifact was produced. Contract tests passed; this is not a crypto-test failure.
- The current source tree contains the call `LegadoChineseConversion.traditionalToSimple(value)` but no production declaration/file for `LegadoChineseConversion`; only the Chinese-conversion test file exists.
- The pinned upstream implementation is `upstream/legado/app/src/main/java/io/legado/app/utils/ChineseUtils.kt` at `330e606c4d57e08adc056ff8ab7a4132c784da9a`. It delegates `s2t/t2s` to quick-transfer-core and loads the exact `TRADITIONAL_TO_SIMPLE` exclusion dictionary before the first traditional-to-simplified conversion. Its excluded terms include `多士`, which the existing regression test requires to remain unchanged.
- The CI workflow's “Restore pinned Legado Rhino extension sources” step only restores Rhino XML implementation Java sources; it does not restore Chinese conversion helpers. Therefore the unresolved symbol is a missing host source integration, not a missing Rhino artifact.

### Registered correction scope
- Integrate the exact pinned upstream Chinese conversion utility behavior from `ChineseUtils.kt` (including lazy/one-time exclusion dictionary initialization and the full exclusion list) using the already declared quick-transfer-core dependency; preserve the current public `java.t2s` and `java.s2t` bridge behavior.
- Audit the current bridge imports/callers and all Chinese conversion tests before changing names or packages. Do not introduce a second converter with different dictionary semantics, silently drop exclusions, or change the dependency version.
- Keep this correction isolated from crypto algorithms, output bounds, Rhino JavaImporter allow-list and other source APIs.

### Required regression/acceptance
- Keep and pass the existing traditional/simplified conversion tests, including the excluded-term fixture `多士`.
- Add coverage for representative pinned upstream exclusion entries and repeated calls (dictionary initialization must be safe and idempotent).
- Run focused Chinese conversion tests, `:app:testDebugUnitTest`, `:integration-contract:test`, and `:app:assembleDebug`; inspect the complete CI steps/logs/reports/artifacts.
- Do not treat run `44564706186` as a pass. Inspect the new code-head CI run separately; no APK is accepted until the build artifact is actually uploaded and verified.

### Execution discipline
- This missing-source correction is registered before its code edit. After implementation, record exact source/test files, test outcomes, CI head/run/artifacts, commit SHA, and self-review of any additional missing source restoration dependencies.


## 2026-11-20 crypto encoded-output test compile correction — PRE-REGISTERED BEFORE TEST EDIT

### CI evidence / confirmed finding
- Run `44565690133` targets intermediate head `75ae47aa3005418e01f47838ecc9e811b888538c` and completed with failure. Contract tests passed; the pinned Rhino artifact restore/verification steps passed; production `:app:compileDebugKotlin` passed.
- The failing step was `Run bounded Rhino replacement host tests`; its log reports `RhinoSourceScriptRuntimeTest.kt:1644:13` and `:1647:13 Unresolved reference 'checkedRhinoCryptoEncodedOutput'`, causing `:app:compileDebugUnitTestKotlin` to fail. The test-report upload found no reports; APK assembly/upload and live-source steps were skipped.
- Final branch head `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92` still contains those two unresolved helper calls in the test file; the follow-up commit only adds RSA Hex coverage and does not remove the missing helper references.
- The actual production output-bound implementation lives in `RhinoBoundedSymmetricCryptoAndroid` and `io.legado.app.help.crypto.AsymmetricCrypto`; the test must exercise those real methods, not refer to an undeclared helper.

### Registered correction scope
- Replace the two orphan helper assertions with direct assertions against the real bounded crypto API. Keep exact 2 MiB Base64/Hex boundary and over-limit checks across supported symmetric overloads.
- Add/retain deterministic asymmetric RSA Base64 and Hex round-trip coverage through the Rhino bridge. Do not claim a >2 MiB asymmetric ciphertext case unless a practical supported fixture can exercise it; document that limitation instead of introducing a fake helper or weakening production limits.
- Inspect the full test class for other unresolved symbols and re-audit the production encoded-output overrides against the exact Hutool 5.8.22 overload signatures before editing.
- Keep this change limited to test correctness/coverage; do not change crypto algorithms, key parsing, encoded output behavior, Chinese conversion, JavaImporter sandbox or other runtime APIs unless a separately registered concrete defect is found.

### Required acceptance
- Focused crypto/Rhino unit tests compile and pass, including exact/over-limit symmetric Base64 and Hex overload tests, RSA Base64 and Hex decrypt round trips, DES/3DES, signatures, stream ownership/progress and JavaImporter restrictions.
- Run `:integration-contract:test`, `:app:testDebugUnitTest` and `:app:assembleDebug`; inspect full job steps/logs, reports and artifacts for the exact final HEAD.
- Poll run `44565856067` (head `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92`) separately; its initial state was pending/no jobs and it does not validate a later correction.
- Do not mark crypto complete or provide an APK until the final code-head CI is green and the APK artifact is verified. Physical-device crypto/provider validation remains open.

### Execution / review
- This corrective test task is registered before editing. Afterward record exact changed files, CI run/head, test results, artifact IDs and the checklist self-review. Ensure no helper reference remains unresolved.


## 2026-11-20 crypto encoded-output test correction — implementation recorded, verification pending

- Corrective task was pre-registered in this file and the three companion required checklists before the test edit.
- CI run `44565690133` (head `75ae47aa3005418e01f47838ecc9e811b888538c`) and final predecessor run `44565856067` (head `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92`) both passed contract tests and production `:app:compileDebugKotlin`, but failed `:app:compileDebugUnitTestKotlin` on two unresolved references to the nonexistent `checkedRhinoCryptoEncodedOutput` helper at the encoded-output test. APK assembly/upload were skipped. Full logs were inspected; no unit-test report or APK artifact was uploaded for those runs.
- Removed the two orphan helper assertions from `app/src/test/kotlin/app/badcat/client/runtime/RhinoSourceScriptRuntimeTest.kt` in commit `43baad71f6937f25baafc80541ec3e787d7731d6`. The real production boundary is exercised directly by the existing exact/over-limit symmetric Base64 and Hex assertions; the redundant fake helper is no longer referenced.
- Existing RSA bridge test now includes Base64 and Hex encrypt/decrypt round trips (added in predecessor commit `e6bc27ad767160c9d84e15e9bfe90fe5215c4b92`). A >2 MiB RSA encoded-output fixture is not claimed; current practical RSA fixture verifies both encodings and round trips, while production code performs the size check on encoded results.
- CI run `44566457890` targets code commit `43baad71f6937f25baafc80541ec3e787d7731d6`; it was queued/in progress at the time of this entry. It must complete and its full job steps/logs/artifacts must be inspected before marking the test correction or crypto CI gate passed.
- Current implementation status: compile-error source correction committed; focused test execution, full app unit tests, APK assembly/artifact verification and device/provider validation remain pending. No crypto completion or APK release claim is made.
- Checklist self-review: this correction accurately captured the actual compile failure and removed only the undeclared test helper; existing direct boundary tests remain. No runtime behavior or algorithm was changed. Next review must inspect all tests/logs on the exact code-head CI.



### 2026-11-20 source-proven encoded-output compatibility correction

- Original host `RhinoLegadoSymmetricCrypto` (pre-crypto commit `9d9b2ef`) caps plaintext input at 2 MiB, raw ciphertext input at 2 MiB, encoded ciphertext input text at 4 MiB, and decoded Base64 ciphertext at 2 MiB.
- It does not cap encrypt output bytes or Base64/Hex output strings. The newly added symmetric 2 MiB encoded-output cap is therefore a regression and must be removed.
- Preserve AES/CBC padding expansion: 2 MiB plaintext can yield 2 MiB+16 ciphertext; Base64/Hex outputs may exceed 2 MiB. Keep input caps and the exact upstream NO_WRAP/Base64/Hex semantics.
- For new asymmetric/signature bridge APIs, cap raw input/key/signature/output at 2 MiB but allow encoded Base64/Hex output expansion up to 4 MiB.
- Correct the checklist and tests before further runtime edits; the current code-head CI must be inspected independently.


## 2026-11-20 crypto compatibility regression correction — PRE-REGISTERED BEFORE RUNTIME CODE EDIT

### Current branch and CI evidence
- Active branch: `integration/legado-upstream-runtime`; pre-registration parent HEAD: `bc18f521f5717bd8ead23677a22c0c9ae7b4e8fb`; pinned Legado upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- Runs `44566457890` (code head `43baad71f6937f25baafc80541ec3e787d7731d6`) and `44566732977` (docs head `03fd349af49424703718cb843d5c5b3caf7f26f7`) both completed with failure in `:app:testDebugUnitTest`. Contract tests, pinned Rhino artifact restore/verification, pinned Rhino extension-source restore, and production Kotlin compilation succeeded. APK assembly/upload and live-source checks were skipped. Each run uploaded only unit-test reports: artifact `14552755775` and `14552826190`; neither uploaded an APK.
- The inspected report for run `44566457890` records 266 tests with 6 failures:
  1. `supportsPinnedLegadoAsymmetricCryptoAndSignFactories`: Android `Base64.encodeToString` stub was not mocked.
  2. `supportsPinnedLegadoDesAndTripleDesLegacyHelpers`: same Android Base64 stub failure.
  3. `supportsPinnedSymmetricCryptoOverloadsAndRandomKeySemantics`: Rhino cannot resolve the upstream overload call with `(String, null, null)`; it is ambiguous between nullable ByteArray and String overloads.
  4. `boundsSymmetricBase64AndHexEncodedOutputsAcrossOverloads`: Android Base64 stub was not mocked.
  5. `supportsLegadoSymmetricCryptoBase64AndHexEncryptionHelpers`: Android Base64 stub was not mocked.
  6. `supportsPinnedLegadoAesLegacyHelpersAndHistoricalQuirks`: Android Base64 stub was not mocked.
- The same failure pattern is present on the latest docs-head run; no successful current-code CI or APK is established.

### Pinned upstream and host source audit
- Pinned upstream `JsEncodeUtils.kt` exposes the same crypto factory overloads as the host: nullable `ByteArray?, ByteArray?`, non-null `ByteArray`, non-null `String`, and `String, String?`. The test expression passing two untyped JS nulls is ambiguous in Rhino and must not be mistaken for a production API regression.
- Pinned upstream `EncoderUtils.kt` uses Android `Base64`; the host JVM unit-test runtime currently throws “Method encodeToString in android.util.Base64 not mocked”. The host needs a tested, platform-neutral adapter that preserves the upstream flag/encoding semantics used by the crypto bridge, including NO_WRAP encoding and NO_WRAP decoding, rather than weakening the crypto tests or silently skipping the affected methods.
- `RhinoSafeSourceJavaExtensions.kt` has three legacy Base64 NO_WRAP decode call sites using the Android stub; these must use the same tested host codec/semantics.
- Current `RhinoBoundedSymmetricCryptoAndroid` caps both encryption results and Base64/Hex encoded output at 2 MiB. This contradicts the source-proven pre-crypto host behavior recorded above: the original symmetric bridge capped raw plaintext input at 2 MiB, raw ciphertext input at 2 MiB, encoded ciphertext input text at 4 MiB and decoded ciphertext at 2 MiB, but did not cap encryption result bytes or encoded output strings. A 2 MiB AES/CBC/PKCS5Padding plaintext legitimately produces a 2 MiB+16 ciphertext.
- Current `requireRhinoCryptoEncodedSize` also limits new asymmetric encoded outputs to 2 MiB despite the raw output bound being 2 MiB; Base64/Hex expansion must be allowed up to 4 MiB.

### Registered implementation scope — do not edit runtime code before this ledger commit
1. In `RhinoBoundedSymmetricCryptoAndroid.kt`, retain the 2 MiB raw encrypt input bound, 2 MiB raw decrypt input/output bound, 4 MiB encoded ciphertext input text bound, stream forward-progress and caller-owned stream behavior; remove the encrypt-result and symmetric Base64/Hex output caps. Preserve Hutool algorithms, transformations and every existing overload.
2. In `RhinoCryptoBounds.kt`, make the encoded-string bound 4 MiB for the newly exposed asymmetric Base64/Hex result surfaces; retain 2 MiB raw input/key/signature/output limits. Keep decoded asymmetric ciphertext and raw output limits unchanged.
3. In `AsymmetricCrypto.kt`, preserve Base64/Hex encoding behavior and permit encoded output expansion up to 4 MiB; keep raw key/data/output bounds. Do not add fake oversized RSA fixtures or claim a >2 MiB RSA ciphertext test where no practical supported fixture exists.
4. Replace the Android-only Base64 dependency in the host `EncoderUtils` adapter with a JVM-testable implementation that matches the pinned Android Base64 flags/semantics used by this code (especially default NO_WRAP encoding, URL-safe/no-padding flags if exposed, and DEFAULT/NO_WRAP decoding). Replace the three `AndroidBase64.decode(..., NO_WRAP)` legacy bridge calls in `RhinoSafeSourceJavaExtensions.kt` with the equivalent tested host decoder. Do not change JavaImporter allow-lists or expose Android classes to scripts.
5. In `RhinoSourceScriptRuntimeTest.kt`, remove only the invalid Rhino test invocation that passes untyped nulls to overloaded `createSymmetricCrypto`; preserve JS tests for String and ByteArray overloads. Verify random-key semantics directly through the real Kotlin upstream-compatible constructor with a round-trip test, not a fake JS API or a removed production overload.
6. Replace the output-cap test with regression tests covering every applicable ByteArray/String/Charset/InputStream overload: 2 MiB raw plaintext accepted, 2 MiB+1 rejected, AES/CBC padding expansion accepted, Base64/Hex output expansion accepted, raw ciphertext input still capped at 2 MiB, encoded ciphertext text capped at 4 MiB, decoded ciphertext capped at 2 MiB, and stream ownership/forward progress retained. Add focused tests for Base64 flag compatibility and the legacy NO_WRAP decode helpers.
7. Re-audit all crypto bridge methods, inherited Hutool overloads, source-scoped Rhino call sites, the independent replacement evaluator and the JavaImporter sandbox. Keep algorithms, key parsing, provider dependencies and source isolation unchanged.

### Required acceptance
- Focused crypto/encoder tests pass; all app unit tests pass; `:integration-contract:test` passes; `:app:assembleDebug` succeeds.
- Current final code HEAD has a completed successful CI run. Inspect every job step, complete failure logs, unit-test XML/HTML reports, APK artifact ID/size/digest and workflow head SHA.
- Verify the exact AES/CBC 2 MiB-to-(2 MiB+16) expansion and Base64/Hex outputs; verify all raw/encoded input boundaries and legacy AES/DES/3DES/RSA/Sign behavior.
- Do not mark crypto complete or provide an APK until the current-head CI and APK artifact are verified. Physical-device/provider validation and the broader full-Legado migration remain open.

### Execution discipline and self-review
- This task and its risks/tests/acceptance are registered before any runtime code edit. Only the above scope is authorized; if source inspection discovers another affected overload/caller, amend the ledger first.
- After implementation, append exact changed files, focused/full test results, CI run/head, report and APK artifact metadata, commit SHA, unresolved device/full-Legado gates, and a self-review of whether the registration captured every affected call path.


### Test-harness amendment — preserve pinned Android Base64 implementation
- Source review confirms the Android Base64 calls are part of the pinned upstream implementation. Replacing them with a locally invented codec would create avoidable behavioral drift and is not authorized.
- Amend item 4 above: keep production `EncoderUtils` and the three `AndroidBase64.decode(..., NO_WRAP)` bridge calls unchanged. Add Robolectric `4.14.1` as a test-only dependency and move only the six Android-Base64-dependent crypto tests into a dedicated `RhinoCryptoCompatibilityTest.kt` using `RobolectricTestRunner` with an explicit supported SDK and `Config.NONE`. This exercises the real Android Base64 implementation rather than changing it.
- The null/null overload failure is a test-expression ambiguity against the exact pinned upstream overloads, not a missing factory. Keep the String and ByteArray JS overload tests; test random-key semantics directly through the actual Kotlin `RhinoBoundedSymmetricCryptoAndroid(transformation, null)` constructor.
- All output-bound corrections, input-bound tests, stream tests, asymmetric raw/encoded policy, JavaImporter restrictions, and acceptance gates registered above remain unchanged.
- This amendment is recorded before the runtime/test/build-file edits; if Robolectric cannot run on the pinned CI toolchain, stop and register an alternative test-only approach before changing production Base64 semantics.


## 2026-11-20 crypto helper rename compile failure — PRE-REGISTERED BEFORE CORRECTION

- CI run `44568645734` for code head `534262df7feb30009bb7d62d4ce1e4617f09e955` completed with failure at `:app:compileDebugKotlin`: five remaining call sites in `RhinoBoundedSymmetricCryptoAndroid.kt` referenced `boundedBase64` / `boundedHex` after the helper declarations had been renamed to `encodeBase64` / `encodeHex`. Contract tests and pinned Rhino artifact verification passed; unit tests, APK assembly/upload and live-source steps did not run.
- The latest code head `6d1b3a946e877d2cce433332ca7ce536482bd3b4` contains the same incomplete rename; its workflow run `44568939955` was pending and cannot be treated as validation.
- Registered correction: update every remaining call site to the chosen helper names (or consistently revert the declaration rename), inspect the complete file for zero stale identifiers, then compile and run the focused/full tests. Do not change crypto semantics or output/input bounds in this correction.
- After correction, inspect the new exact-head CI logs and artifacts. This failure is a source compile defect, not an Android Base64/Robolectric test result; the six previous unit-test failures remain unverified until tests actually run.


## 2026-11-20 crypto regression test fixture correction — PRE-REGISTERED BEFORE TEST EDIT

- Latest-head run `44569202532` on `11a2f0c059dabf4a8c116fcd16585803b935e9f1` passed pinned-upstream verification, contract tests, Rhino artifact verification and source restoration. `:app:testDebugUnitTest` ran 266 tests: 264 passed, 2 failed. Unit-test report artifact: `14552993253` (SHA-256 `f85c44da84c3c56c85f25b95009231858c2327020740b3e354533e44bc52dd19`). APK assembly/upload was skipped.
- Failure 1: `supportsPinnedLegadoDesAndTripleDesLegacyHelpers` expected the raw DES ciphertext string's `desBinary.length` property to be positive, but Rhino exposed the Java String wrapper such that the property check evaluated false. Pinned/current `desEncodeToString` intentionally returns `String(ciphertextBytes)`; keep production code unchanged and make the assertion explicitly coerce with `String(desBinary).length > 0`.
- Failure 2: `allowsSymmetricEncodedOutputExpansionAndPreservesInputBounds` used `Base64.getEncoder().encodeToString(ByteArray(2 MiB + 1))`. The all-zero Base64 text consists only of `A` characters, and pinned `String.isHex()` classifies it as hexadecimal before Base64 decoding. The test therefore exercised the upstream Hex path and produced a cipher block-size `CryptoException`, not the expected decoded-size bound. Change only the fixture bytes to a deterministic non-hex Base64 alphabet (for example all `0xFF`, yielding `/` characters), then assert the decoded 2 MiB + 1 input is rejected by the raw ciphertext bound.
- Do not change `String.isHex()`, Hutool Hex/Base64 precedence, DES raw-ciphertext helper semantics, or crypto algorithms; those are pinned upstream behaviors. The test correction must verify the actual intended Base64 branch and the Java String coercion.
- Re-run the focused crypto class and all 266 app unit tests, inspect XML reports, then require a successful exact-head CI and APK artifact before marking the crypto gate passed.


## 2026-11-20 session implementation record and self-review (CI still pending)

### Actual changes
- Re-read `AGENTS.md` in full and reviewed all four mandatory checklists before work; all task scope and test risks were registered before code/test edits.
- `RhinoBoundedSymmetricCryptoAndroid.kt`: symmetric encrypt input remains capped at 2 MiB; encryption result bytes and Base64/Hex output strings are no longer incorrectly capped at 2 MiB. Raw decrypt input/output remains bounded; encoded decrypt input text remains 4 MiB. Stream reads preserve forward-progress and caller ownership.
- `RhinoCryptoBounds.kt`: asymmetric encoded output cap is 4 MiB; raw byte cap remains 2 MiB.
- `app/build.gradle.kts`: added test-only Robolectric 4.14.1 so crypto bridge tests execute Android Base64 shadows without changing pinned production Base64 behavior.
- `RhinoCryptoCompatibilityTest.kt`: moved six Android-Base64-dependent crypto tests under Robolectric; retained String/ByteArray JS overload checks and moved random-key verification to the real Kotlin constructor. Added symmetric output expansion/input-bound tests, including AES/CBC PKCS5 padding expansion and the 4 MiB encoded boundary.
- `RhinoSourceScriptRuntimeTest.kt`: removed the ambiguous Rhino `(String, null, null)` test expression; retained direct random-key verification and updated the 2 MiB padded AES/CBC expectation.
- Test-fixture follow-up in `RhinoCryptoCompatibilityTest.kt`: coerce the Java String wrapper before checking raw DES ciphertext string length; use a deterministic `0xFF` Base64 fixture and assert its decoded size so upstream `isHex()` does not route an all-`A` Base64 string through Hex.

### Relevant commits
- Pre-registration and test-harness amendment are recorded in the preceding entries of all four files.
- Initial runtime/test/build change: `534262df7feb30009bb7d62d4ce1e4617f09e955`.
- CBC padding regression fixture: `6d1b3a946e877d2cce433332ca7ce536482bd3b4`.
- Complete helper call-site rename: `11a2f0c059dabf4a8c116fcd16585803b935e9f1`.
- Latest test fixture correction: `66d43eb7931a3bdabdfe49a2cdfa3f6a4f895173`.

### CI evidence and current gate
- `44568645734` (`534262df`) and `44568939955` (`6d1b3a94`) failed compilation because the helper rename had stale call sites. This was registered and corrected in `11a2f0c0`.
- `44569202532` (`11a2f0c0`) passed pinned upstream verification, contract tests, Rhino artifact verification and source restore. App tests ran: 266 total, 264 passed, 2 failed due to the two test-expression/fixture issues described above. Report artifact `14552993253`, SHA-256 `f85c44da84c3c56c85f25b95009231858c2327020740b3e354533e44bc52dd19`. APK assembly/upload was skipped.
- `44570326349` is a stale docs-head run without the latest test fixture correction. `44570390532` targets the latest code head `66d43eb7` and was pending at the time of this note. This documentation update will trigger a new latest-head run; use that run, not stale runs, for acceptance.
- Do not mark the crypto task complete until latest-head focused/full tests pass and APK build/artifact are inspected. No APK is available or approved for delivery from this session yet.

### Remaining work
1. Inspect the workflow triggered by this documentation commit; verify its exact checked-out HEAD.
2. Inspect focused crypto test results and all app unit-test XML/HTML reports.
3. If tests pass, verify `:app:assembleDebug`, artifact ID, file size and digest; if any test fails, register the exact defect before changing code.
4. Physical-device installation/provider behavior, real-source checks and the broader full-Legado runtime/host migration remain open.

### Checklist self-review
- The registered scope correctly captured the source-proven distinction between raw input limits and uncapped symmetric encryption output, the 4 MiB encoded-output policy, and the need to test Android Base64 in an Android-aware JVM harness.
- The first test pass exposed two additional fixture errors: Java String wrapper property access and a Base64 string consisting only of valid Hex characters. Both are now corrected in the latest test commit without altering upstream behavior.
- The checklist remains accurate; acceptance is still pending latest-head CI and APK artifact validation. No item is marked complete based only on compilation, documentation, or a pending workflow.


## 2026-11-20 Android APK packaging collision — PRE-REGISTERED BEFORE BUILD CONFIG EDIT

### Verified CI evidence
- Code-head run `44570390532` (`66d43eb7931a3bdabdfe49a2cdfa3f6a4f895173`) and latest checklist-head run `44570879134` (`1677bc56baa7a1bf92eb5254c8df5bfb8630ed1d`) both passed:
  - pinned upstream source verification,
  - `:integration-contract:test`,
  - pinned Rhino artifact verification and source restoration,
  - bounded Rhino replacement host tests,
  - app `:testDebugUnitTest`.
- Downloaded and inspected unit-test report artifact `14554936230` from run `44570879134` (archive size 83,462 bytes; artifact digest `sha256:9c394b30fde857fad53fa4d221d9d7be8c74207cca8f8817e5e15f3b84763911`). XML totals: 266 tests, 266 passed, 0 failures, 0 errors, 0 skipped. `RhinoCryptoCompatibilityTest`: 6/6 passed; `RhinoSourceScriptRuntimeTest`: 94/94 passed.
- Both runs fail only at `:app:mergeDebugJavaResource` during `:app:assembleDebug`. AGP reports duplicate resource path `META-INF/INDEX.LIST` from `cn.hutool:hutool-crypto:5.8.22` and `cn.hutool:hutool-core:5.8.22`. No APK artifact was produced; APK upload and live-source steps were skipped.
- This is a Java archive index metadata collision, not duplicate application classes or a runtime crypto failure. The workflow's exact logs were inspected for both code and latest checklist heads.

### Registered correction scope — before changing build configuration
1. Modify only `app/build.gradle.kts` to exclude the duplicate non-runtime archive index resource path `/META-INF/INDEX.LIST` via Android Gradle Plugin's `android.packaging.resources.excludes` DSL.
2. Keep both pinned Hutool dependencies and versions, all classes/resources other than this duplicate archive index metadata, crypto algorithms, Android Base64 behavior, and runtime code unchanged.
3. Do not use broad `META-INF/**` exclusions, dependency removal, dependency substitution, `pickFirst` over application/runtime resources, or relax duplicate-class checks.
4. Re-run exact-head CI: confirm all 266 app tests and contract/host tests pass, `:app:assembleDebug` succeeds, inspect complete logs and APK artifact metadata (ID, size, SHA-256), and check no unrelated resource was excluded.
5. Only after a successful APK artifact can the build gate be marked passed. Physical-device install, provider validation, real-source checks, and complete Legado migration remain separate open acceptance requirements.

### Risk and compatibility audit
- The colliding path is `META-INF/INDEX.LIST`, archive classpath index metadata generated/packaged by JAR tooling; it is not a class, manifest, Android resource, crypto provider registration, or service-loader declaration.
- The exclusion is narrowly scoped to this one exact resource path at APK packaging time. Hutool classes, crypto provider classes, `META-INF/services/**`, manifests, licenses, and all other dependency payload remain included.
- Existing `app/build.gradle.kts` has no packaging customization block; add only the narrow resources exclusion under the existing `android` block. No other module or build logic is in scope.

### Acceptance and self-review
- Focused crypto tests and the full app unit-test suite are already green on run `44570879134`; this correction must not alter those test results.
- Require a successful latest-head workflow and a real uploaded debug APK before recording packaging success.
- This registration identifies the exact failing task, both colliding artifacts, the minimal metadata-only exclusion and the regression gates. If a new failure exposes another resource conflict, amend the ledger before broadening the exclusion or editing additional files.


## Pinned-upstream SHA ledger correction (2026-11-20)

A review of the registered APK packaging task exposed a malformed 39-character abbreviated value in the root `AGENTS.md` and several historical checklist entries. The actual submodule gitlink at this branch and the CI workflow's `expected` value both independently confirm the full, valid 40-character upstream commit SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`. The public LegadoTeam/legado commit endpoint also confirms that exact SHA and its commit metadata. Corrected only malformed 39-character references to the exact 40-character pinned revision; no submodule pointer or upstream source was changed. The pinned-source verification step had passed on both recent CI runs. This is a documentation integrity correction, not a change to the registered packaging-fix scope.


## 2026-11-20 APK packaging follow-up — Bouncy Castle license resource collision (PRE-REGISTERED BEFORE FURTHER BUILD EDIT)

- Run `44604734584` on code head `4a6e57f61fa9b9eddf7d1aec690589d36cb2af3a` passed pinned-upstream verification, contract tests, Rhino host tests, and all 266 app unit tests (266 passed; 0 failures/errors/skips). The test report is artifact `14568848137`, size 83,370 bytes, digest `sha256:fdfaeae8e01b2ac8f8754d3fd8494920d67794b2cabb21d28343011f07430041`.
- The narrow `/META-INF/INDEX.LIST` exclusion removed the previous Hutool conflict. APK assembly then exposed the next collision: `META-INF/LICENSE.md` from `bcpkix-jdk18on:1.86`, `bcutil-jdk18on:1.86`, and `bcprov-jdk18on:1.86`. The build still fails at `:app:mergeDebugJavaResource`; no APK artifact exists.
- Registered follow-up: inspect the exact contents and provenance of all three Bouncy Castle `LICENSE.md` entries before changing packaging. Preserve the complete applicable license notice(s) in the APK or an equivalent packaged legal-notices location. If all three are byte-identical, a narrow `pickFirsts += "/META-INF/LICENSE.md"` may preserve the shared notice. If they differ, do not silently pick one or exclude them; retain each distinct notice under a deterministic non-colliding path or another verifiable packaging solution.
- Do not add broad `META-INF/**` excludes, remove/replace Bouncy Castle modules, weaken duplicate-class checks, or drop license content. Keep the existing Hutool `INDEX.LIST` exclusion and crypto dependencies unchanged.
- Acceptance: verify license bytes/checksums and legal coverage, update only the narrowly necessary packaging configuration (and any generated license-resource handling if required), rerun all 266 tests plus contract/Rhino tests and `:app:assembleDebug`, inspect the final APK for retained license notices and required crypto provider classes, and verify the exact-head APK artifact ID/size/SHA-256.
- This amendment follows the registered rule to amend the ledger before broadening a packaging fix when a new conflict appears. Physical-device install, real-source checks and complete Legado migration remain open.


## 2026-11-20 Bouncy Castle packaging correction — exact-head verification record

- Code commit: `cc78171c7233db7a452606121010fca65681167a`; exact-head workflow run [44605770188](https://github.com/xkszds/badcat-legado-client/actions/runs/44605770188) checks out that SHA on `integration/legado-upstream-runtime`. The branch ref was verified to point to the same commit before this record.
- The narrowly scoped `META-INF/LICENSE.md` `pickFirst` resolved the Bouncy Castle resource collision. In the exact-head job, pinned-upstream verification, contract tests, Rhino artifact verification/source restore, bounded Rhino host tests, unit-report upload, `:app:assembleDebug`, and APK upload all report success. The run is still `in_progress` because the optional four-source uploaded-corpus smoke step has not completed; its report upload and post-job cleanup are pending. Do not record the overall workflow as completed.
- Downloaded and inspected unit-test artifact `14570890675` (83,576 bytes; ZIP SHA-256 `964dce365cf664e7ff48e1bc46584cb2e9679dcc33a6ff9d45c96fc08515b996`). All 31 JUnit XML files total 266 tests, 266 passed, 0 failures, 0 errors, 0 skipped; `RhinoCryptoCompatibilityTest` is 6/6 and `RhinoSourceScriptRuntimeTest` is 94/94.
- Debug APK artifact `14568734307` (ZIP 13,840,284 bytes; artifact SHA-256 `c03d78a09a40f6c23c6acccdaf87089a514df4e95d535962335a30ddc8fc45a1`) contains `app-debug.apk` (18,163,811 bytes; SHA-256 `9c8588f72dbadf0c767c0cea00227c33775a01363034b8754ba75cc1f4e71dbc`). APK inspection confirms `META-INF/LICENSE.md` is retained (1,171 bytes), `META-INF/INDEX.LIST` is absent as intentionally excluded archive metadata, and Bouncy Castle provider, AES/RSA engine and RSA signer symbols remain in DEX.
- Live smoke evidence already uploaded for this run: Pinshuzhai end-to-end search/detail/TOC/content passed (artifact `14570508163`); Bixiange GBK POST search/detail/TOC AJAX page/content passed (artifact `14571255565`). The workflow also reports success for Nyasama endpoint smoke, public source repository probes and Feisuzhong end-to-end reading. The separate four-source uploaded-corpus sample is still running; await its result/report before closing the workflow record.
- Not verified: physical Android-device installation/provider behavior; complete pinned Legado runtime/host migration; final full five-category parity. This CI APK is a debug build artifact, not the final complete-Legado release.
- Self-review: the pre-registered collision scope correctly limited the build change to the shared, source-verified Bouncy Castle license file and retained the full legal notice; the APK inspection confirms that notice and required crypto symbols remain. Acceptance remains explicitly open for the still-running uploaded-corpus smoke, device validation and full runtime migration.


## 2026-11-20 final exact-code-head CI and uploaded-corpus result — supersedes the pending note above

- Workflow run [44605770188](https://github.com/xkszds/badcat-legado-client/actions/runs/44605770188) has now **completed successfully** for code commit `cc78171c7233db7a452606121010fca65681167a` on `integration/legado-upstream-runtime`. The job and workflow conclusion are both `success`; all job steps, including post-job cleanup and the four-source report upload, are completed. The earlier statement that the run was still in progress is superseded.
- Exact-head verification passed pinned-upstream SHA verification, `:integration-contract:test`, Rhino artifact verification/source restoration, bounded Rhino host tests, `:app:testDebugUnitTest`, `:app:assembleDebug`, and debug APK upload.
- Unit-test artifact `14570890675` was downloaded and inspected: 31 JUnit XML files, 266 tests, 266 passed, 0 failures, 0 errors, 0 skipped. `RhinoCryptoCompatibilityTest` passed 6/6 and `RhinoSourceScriptRuntimeTest` passed 94/94. The XML/HTML test report bundle is 83,576 bytes; SHA-256 `964dce365cf664e7ff48e1bc46584cb2e9679dcc33a6ff9d45c96fc08515b996`.
- Debug APK artifact `14568734307` is a ZIP of 13,840,284 bytes (SHA-256 `c03d78a09a40f6c23c6acccdaf87089a514df4e95d535962335a30ddc8fc45a1`). Its `app-debug.apk` is 18,163,811 bytes (SHA-256 `9c8588f72dbadf0c767c0cea00227c33775a01363034b8754ba75cc1f4e71dbc`). Re-inspection confirms the 1,171-byte `META-INF/LICENSE.md` remains in the APK, `META-INF/INDEX.LIST` is absent, and Bouncy Castle provider, AES, RSA and signer symbols remain in DEX.
- End-to-end live source results are mixed and must not be generalized: Pinshuzhai passed search → detail → 1,650-chapter TOC → first chapter content (report artifact `14570508163`); Bixiange passed GBK POST search → detail → AJAX TOC pagination → chapter content (report artifact `14571255565`). In the separate four-source uploaded-corpus sample, only 精华书阁 completed search → detail → 20 TOC entries → chapter content; 唐三中文 and 零零小说 timed out at search, while 笔趣阁fun returned HTTP 404. The report is artifact `14569199256` (981 bytes; SHA-256 `b3eb93414fa661ddbe312c8142822e01c7fd9852c0ab4eb2e428d8567c0d8084`). Thus the sample result is 1/4, not 4/4.
- Other best-effort probes are likewise partial: the Nyasama sample returned usable results for 3/5 probes; two search endpoints returned empty results. Public-source probing encountered upstream HTTP 403/502, timeouts, a 404 and a redirect loop; several other source pages returned parseable candidate rows, but these probes are not full reading-flow validation. Treat these as source/network or source-data observations until individually reproduced; do not count them as engine passes.
- Not verified: installation or runtime behavior on a physical Android device; complete pinned Legado runtime/host integration; broad source-corpus compatibility; complete Badcat five-category parity; signed release build. This is a verified CI Debug APK only, not the requested final complete-Legado release.
- Self-review: the earlier pending status is now corrected with the final run conclusion. Build/test gates are recorded as passed only for this exact code head; live-source evidence is separated by source and stage, and external failures are not mislabeled as engine failures. Full runtime, device and product-parity gates remain open.


## 2026-11-20 pre-registered audit gate — complete upstream JsExtensions/BaseSource contract map

The declaration-level inventory is not yet a behavioral compatibility matrix. The next task is an audit-only pass; no runtime code changes are authorized until its findings and a separate implementation scope are registered.

Audit pinned upstream JsExtensions.kt, inherited JsEncodeUtils.kt, BaseSource.kt and directly called service/types at 330e606c4d57e08adc056ff8ab7a4132c784da9a. Compare every declaration and overload to RhinoSafeSourceJavaExtensions.kt, RhinoSourceScriptRuntime.kt, all host adapters, static callers and dynamic Rhino tests. Record parameter/default/nullability, JVM descriptor, JS-visible object shape, return/exception semantics, blocking/suspend/coroutine context, lifecycle, source identity/isolation and persistence.

Trace BaseSource's real dependency graph: BookSource and rule entities; Room/Gson converters and database; cookie/login state; script key/value storage; cache; HTTP request/response services; browser/WebView and navigation; refresh/verification/video UI callbacks; BatchContentContext and durable chapter save. Explicitly analyze the existing getSource(): BaseSource? vs descriptor-map collision, getBatchContext/cacheContent gap, Jsoup/host response-wrapper ABI, and sandboxed file handle. Known host entry points include source-scoped scope["java"], descriptor injection, HTTP/file wrappers, cache/batch adapters and all tests/callers.

Risks to enumerate: JVM/Rhino overload ambiguity, return-object reflection shape, mutable source identity and state leakage, coroutine/thread cancellation, response/body ownership and redirects, Android lifecycle ownership, filesystem sandboxing, and chapter-index/save-token persistence semantics. No fake BaseSource, null/fake service, no-op method or broader JavaImporter/Packages access.

Acceptance: every upstream declaration/overload is accounted for with exact source and host references; every mismatch is classified as compatible, deliberate security restriction, UI-context unsupported, or missing; complete caller/test inventory and dependency graph are documented; a bounded implementation slice and regression criteria are prepared. This task is audit-only; register the implementation in all four checklists before editing runtime code.

## 2026-11-20 declaration and behavior audit — findings and next gate

The audit-only task registered above is complete. No runtime/product code was changed.

### Declaration inventory and gaps

Pinned JsExtensions has 122 function declarations total (118 public across 77 names); inherited JsEncodeUtils has 30 public declarations across 27 names. The current RhinoSafeSourceJavaExtensions main bridge has 150 public declarations across 110 names. All 30 JsEncodeUtils declaration slots and 27 names exist in the host declaration scan, but source-level presence is not proof of semantic parity.

17 public JsExtensions names / 30 overload declarations are absent: refreshBookInfo, refreshBookToc, refreshContent, webView, webViewGetSource, webViewGetOverrideUrl, openVideoPlayer, startBrowser, startBrowserAwait, getVerificationCode, openUrl, showBrowser, getReadBookConfig, getReadBookConfigMap, getThemeMode, getThemeConfig, getThemeConfigMap.

The following same-name APIs are not ABI-equivalent:
| Upstream | Current host | Finding |
|---|---|---|
| getSource(): BaseSource? | getSource(): Map<String, Any?> | Critical collision. Existing runtime test requires nested/future source JSON map. Keep it under a descriptor accessor when migrating; do not silently replace it. |
| getTag(): String? | String | Nullability/return ABI differs. |
| getBatchContext(): BatchContentContext? | Any? (always null) | Outside-batch default matches; actual batch type/lifecycle is absent. |
| cacheContent(Any?, String): Boolean | same signature, always throws | No batch persistence implementation. |
| get/head/post -> Jsoup Connection.Response | RhinoSourceHttpResponse | Facade omits Jsoup request/cookie/body/stream semantics; not a drop-in response object. |
| connect/ajaxAll/ajaxTestAll -> StrResponse | RhinoSourceHttpResponse | Same host facade used for different upstream response types. |
| getFile(String): File | RhinoSourceFileHandle | Deliberate sandbox restriction; facade is not the complete File API. |
| toURL -> JsURL | RhinoLegadoUrl | Different object shape/method surface. |
| ajax/ajaxAll/ajaxTestAll/connect | overload/parameter/return differences | Input array/timeout/skip-rate-limit and response behavior need per-overload tests. |
| timeFormat/timeFormatUTC | Number/String overloads in host vs Long upstream | Type coercion and Rhino overload resolution differ. |
| log/logType | String return in host vs Any?/Unit upstream | Return semantics differ. |
| htmlFormat/hexEncodeToString | nullable and return-nullability differences | Need exact null/empty handling tests. |

Host-only methods and helper wrappers must not be counted as upstream parity.

### BaseSource and lifecycle findings

Pinned BaseSource is an interface extending JsExtensions. Both BookSource and RssSource implement it. BaseSource uses Room/GSON-backed models and depends on AppConfig/AppConst/AppLog, CacheManager, CookieStore, ConcurrentRateLimiter, SharedJsScope, JsSourceEngine, LoginUiV2, Android UI/navigation services and Book/BookChapter data. Its source evalJS binds java, source, sourceApi, baseUrl, cookie and cache; source and sourceApi refer to the actual BaseSource object. It implements dynamic header parsing, login UI v1/v2, login(), login header/cookie handling, encrypted loginInfo, source variables, source key/value put/get, refreshExplore, refreshJSLib, putConcurrent and evalJS.

The current host runtime uses a BookSource DTO plus raw JSON, not upstream BookSource/RssSource Room entities. It binds java to RhinoSafeSourceJavaExtensions, cookie to RhinoSourceCookieExtensions, source to RhinoSourceLoginExtensions and cache to RhinoSourceCacheExtensions. sourceApi is not bound. The source helper exposes getKey, variable and login-state helpers, but not the full BaseSource contract. sourceDescriptor() reconstructs a JS-friendly raw JSON map and overlays identity/enabled fields.

Host service mapping:
- SharedPreferencesSourceScriptKeyValueStore: durable source-scoped key/value state.
- SharedPreferencesSourceLoginSessionStore: durable source-scoped login header/info.
- SharedPreferencesSourceCookieJar: durable source-isolated OkHttp cookie state.
- FileSourceScriptCacheStore / RhinoSourceFileHandle: source file cache with canonical-root sandbox.
- OkHttpSourceRequestClient / SourceRequestFactory / LegadoUrlResolver: bounded host HTTP transport and URL resolution, not pinned AnalyzeUrl.
- RhinoSourceCacheExtensions: source-scoped cache semantics, not global CacheManager/appDb/ACache.
- BadcatReadingEngineFactory wires these real services; default constructors may still use in-memory stores for tests or callers that do not inject production adapters.

AnalyzeUrl in the pinned source carries BaseSource, ruleData, chapter, CoroutineContext, dynamic source headers, URL options, proxy/DNS/retry/redirect/timeouts, cookies, bodyJs/webJs and WebView behavior. The host uses a narrower custom resolver and SourceRequestFactory. SourceRequestFactory parses static JSON headers; it does not execute dynamic header rules.

CookieStore upstream uses domain-oriented DB and Android WebView cookie state; host uses per-source OkHttp jars. CacheManager upstream uses appDb, memory LRU and ACache; host uses per-source SharedPreferences/file stores. These are real host services with deliberate isolation, but behavior differences must be documented and tested.

### Batch lifecycle finding

Pinned BatchContentContext resolves chapter objects by BookChapter.index; URL strings are accepted only when unique, so duplicate chapter URLs cannot silently write to the wrong chapter. It applies the source content replacement rule, checks coroutine cancellation, requires the request-start ContentSaveToken and persists through BookHelp.saveContent's version fence. JsSourceEngine installs the context only for getContentBatch and clears it in finally; JsSourceBook returns unsaved chapters for ordinary single-chapter fallback. AnalyzeRule also carries a batchContext and binds chapters.

The host has no corresponding contentBatch flow in SourceChapterContentService. getBatchContext is always null and cacheContent always throws; only the outside-batch failure is tested. Do not implement a simplified URL-only cache write.

### Caller/test scan

Targeted scan covered all 24 host app runtime Kotlin files and all 20 app unit-test Kotlin files. Current java.getSource descriptor use is in RhinoSourceScriptRuntimeTest; batch default/failure is in RhinoSafeSourceJavaExtensionsCompatTest; source login scripts are in RhinoSourceScriptRuntimeTest and SourceLoginSessionTest. The scan found no other static Kotlin production caller of these bridge methods within those scopes.

### Next implementation task registration (not started): source login/session parity and credential boundary

1. Restrict SourceLoginSessionRequestClient login-header merging to the same site as the source, matching pinned AnalyzeUrl semantics; cross-origin/CDN requests must not receive Authorization/custom login headers.
2. Synchronize Cookie values from putLoginHeader into the source cookie jar; remove associated cookie state on removeLoginHeader without touching other sources.
3. Encrypt loginInfo using the pinned androidId-derived AES key and implement a tested migration for existing plaintext host records.
4. Preserve source isolation, cancellation, validation and secret redaction.
5. Test same-site/subdomain/cross-origin/CDN, cookie merge/removal, encrypted persistence/migration, malformed values and two-source isolation.

This next task is separate from full BaseSource/BookSource integration. It does not authorize changing java.getSource ABI, adding UI/WebView methods, or implementing batch persistence. All other migration gaps remain open.

### Additional auth-state lifecycle finding

RhinoSourceScriptRuntime.clearSourceState clears source scope/global state, script key/value storage, source file cache, physical source cache and login-session state. It does not clear the persisted SharedPreferencesSourceCookieJar, and the runtime has no cookie-clear callback. The jar's clear() method exists but is not connected to this lifecycle. This can leave source cookies persisted after source state is cleared; include explicit per-source cookie cleanup in the next auth/session slice and test that other sources remain untouched.


## 2026-11-20 implementation registration — BaseSource login/session contract

Pinned-source targets to inspect before editing: `BaseSource.kt` loginHeader/putLoginHeader/removeLoginHeader/loginInfo encryption and cookie behavior; `AnalyzeUrl.kt` same-site login-header attachment and redirect handling; upstream AES utility/key derivation; CookieStore behavior. Host targets/callers: `RhinoSourceLoginExtensions`, `SourceLoginSessionRequestClient`, `SharedPreferencesSourceLoginSessionStore`, `SharedPreferencesSourceCookieJar`, `RhinoSourceScriptRuntime.clearSourceState`, `BadcatReadingEngineFactory`, `SourceLoginSessionTest`, `RhinoSourceScriptRuntimeTest` and all repository-wide references to these symbols.

Compatibility/security risks: same-site domain matching and public suffixes; redirects changing origin; Cookie header precedence and cookie attributes; per-source state leakage; key derivation/platform identifier availability; plaintext-to-ciphertext migration and rollback; malformed/legacy records; reset lifecycle and cancellation. No widening of Rhino Java access and no fake BaseSource/service.

Required regression matrix: same origin, valid subdomain, unrelated origin, CDN, cross-origin redirect; Cookie import/merge/removal; source reset removes only that source's cookies; independent source IDs; encrypted persistence and decryption; plaintext migration idempotence; malformed ciphertext/missing key; secret redaction; existing login scripts and cancellation. This is a bounded implementation task, not proof of full BaseSource/JsExtensions parity.

## 2026-11-20 implementation progress — login-header same-site boundary

Implemented the first bounded sub-slice in `SourceLoginSession.kt`: saved source login headers are attached only when the source key and initial request URL resolve to the same effective-TLD+1 domain, matching pinned `NetworkUtils.getSubDomainOrNull` and `AnalyzeUrl.isLoginHeaderSite`. IP literals are compared directly. As upstream does, malformed/unresolvable URLs fail open. Automatic redirect-chain stripping is not claimed; pinned AnalyzeUrl evaluates the initial URL.

Regression coverage added for a same-site subdomain, an unrelated CDN host, and invalid-source-key fallback. This does not complete the registered task: Cookie synchronization/cleanup and AES loginInfo encryption plus legacy migration remain open. No release or full Legado parity claim.


## 2026-11-20 source login/session implementation progress — verification pending

The previously registered BaseSource login/session slice now has implementation changes on `integration/legado-upstream-runtime`:

- `SourceLoginSessionRequestClient` retains the same-site/effective-TLD+1 login-header boundary from commit `c910ca461d26175125fcd02918b8680012c07037`.
- `RhinoSourceLoginExtensions.putLoginHeader` now forwards a supplied Cookie request-header value to the source-scoped cookie adapter before persisting the login header. `removeLoginHeader` removes the saved header and asks the adapter to remove cookies for that source's registrable domain.
- `SharedPreferencesSourceCookieJar` parses raw `Cookie: name=value; ...` request-header pairs as host-only cookies on the source host; replacing login cookies removes cookies for the source registrable domain and its subdomains while preserving unrelated CDN-domain cookies. Source reset is wired to clear only that source's jar.
- Persisted `loginInfo` now uses the pinned upstream `BaseSource.kt` algorithm: first 16 Android-ID bytes as key and `SymmetricCryptoAndroid("AES", key).encryptBase64(info)`. Existing JSON plaintext is returned and migrated to ciphertext on read when the Android ID is available. Missing keys preserve plaintext; malformed ciphertext returns null without deleting the stored value.
- Added regression coverage for Cookie header parsing, cookie-domain boundaries, login-header cookie writer/remover callbacks, source-reset cookie cleanup, AES round-trip, idempotent plaintext migration, missing key and malformed ciphertext.

Pinned upstream evidence inspected: `BaseSource.kt` at `330e606c4d57e08adc056ff8ab7a4132c784da9a` calls `CookieStore.replaceCookie(getKey(), cookie)`, `CookieStore.removeCookie(getKey())`, and uses `AppConst.androidId.encodeToByteArray(0, 16)` with `SymmetricCryptoAndroid("AES", key)`. `AppConst.androidId` reads Android's `ANDROID_ID` setting (fallback string `"null"`).

Verification status at this documentation update: exact-head CI for code head `c451b0c304703c2ffa84d855b8aa2d3a44a36ad1` is pending (run `44619334579`). Earlier per-commit workflow runs were cancelled by subsequent pushes and are not counted as passing. Do not mark this slice verified until the exact final head completes focused tests, full unit tests and Debug APK assembly and its logs are inspected.

Open review items: verify the source key passed by the runtime is always the imported source URL for every caller; exercise durable SharedPreferences cookie replace/remove across separately constructed jar instances; compare generated AES ciphertext/decryption against a pinned upstream fixture; verify migration with real Android preferences/device; inspect exact-head CI. Same-site login-header filtering checks the initial URL as upstream does; it does not claim redirect-chain header stripping. This increment does not complete BaseSource/JsExtensions, full Legado runtime, physical-device validation or Badcat five-category parity.


## 2026-11-20 review correction and current exact-head status

A final diff review caught an accidental self-recursive implementation in the new source-cookie-domain predicate. It was corrected in code commit `4fe83640cbaee4bbbb86875998838dd031ec1457`; the helper now compares the cookie domain to the source registrable domain or one of its subdomains. This correction is recorded explicitly rather than treating the earlier intermediate commit as valid.

Current code head: `4fe83640cbaee4bbbb86875998838dd031ec1457`. Exact-head workflow run `44619521355` is pending. The earlier run `44619447738` (documentation head `e19fa8ab197e4cf6a56f48b42e50f80b3dbbad08`) was cancelled by the subsequent code correction and is not a test result. No test/build success is claimed yet. Review the exact-head job logs before accepting this increment.


## 2026-11-20 exact-head CI failure and correction

The exact-code-head workflow `44619521355` for `4fe83640cbaee4bbbb86875998838dd031ec1457` failed during `:app:compileDebugKotlin`: the default lambda for `sourceLoginCookieRemover` had three parameters while its declared callback accepts two. The contract tests and pinned Rhino artifact verification had passed; the Android host test task failed at compilation, so no host test suite or APK result is claimed for that run.

Corrected the callback arity in code commit `4b6c485aa6934520dbc0665b6de6b69a85454608`. The previous failure is retained as a failure record, not overwritten. A new exact-head workflow must compile and run the full relevant tests before this implementation slice can be considered verified.

## 2026-11-20 pre-registered correction task — source login/session test failures (before further runtime edits)

Latest exact-head workflow 44620143558 checked out documentation head 6a4e0c727a7ddc648975c0501f8026bba3ff8d3f and completed with failure. Its :integration-contract:test and pinned Rhino artifact verification passed, but :app:testDebugUnitTest reported 276 tests with 3 failures; APK assembly was skipped. Uploaded report artifact: 14575969615.

### Confirmed failure evidence

1. SourceLoginSessionTest.loginInfoCipherMatchesPinnedAndroidIdAesRoundTripAndRejectsBadKeyOrCiphertext fails because pinned SymmetricCryptoAndroid.encryptBase64 calls android.util.Base64.encodeToString, which is not mocked in the plain Android JVM test runner. This is a test/runtime-boundary issue, not evidence that AES itself failed.
2. SourceLoginSessionTest.legacyPlainLoginInfoMigratesNonDestructivelyAndOnlyOnce fails because the same Android Base64 call throws and is caught by the migration's non-destructive runCatching. The plaintext remains unchanged, so the ciphertext-persisted assertion fails.
3. SourceLoginSessionTest.loginHeadersAreOnlyAttachedToThePinnedEffectiveTldPlusOneSite fails because an unrelated CDN request still receives login headers. The assertion at the CDN check fails. PublicSuffixDatabase resolution is falling through its fail-open path in this JVM test environment or otherwise failing to distinguish these valid registrable domains. Do not change upstream fail-open semantics for invalid/unresolvable URLs; diagnose the public-suffix data/resource loading and preserve the pinned NetworkUtils.getSubDomainOrNull contract.

### Source evidence inspected

Pinned SymmetricCryptoAndroid.kt at 330e606c4d57e08adc056ff8ab7a4132c784da9a implements encryptBase64(data) as EncoderUtils.base64Encode(encrypt(data)); pinned EncoderUtils.base64Encode(bytes) uses Android Base64.NO_WRAP. The host can use the same SymmetricCryptoAndroid.encrypt(plainText) ciphertext bytes and Java's basic padded Base64 encoder/decoder only if a byte-for-byte fixture proves equivalence with Android NO_WRAP; alternatively configure an Android-aware test runner. Decryption should feed decoded bytes to the upstream primitive's byte-array decrypt overload. Do not substitute a different AES mode, key, padding or plaintext encoding.

Pinned NetworkUtils.getSubDomainOrNull uses getBaseUrl, java.net.URL, isIPAddress, and PublicSuffixDatabase.get().getEffectiveTldPlusOne(host); any failure returns null. Host helper currently uses OkHttp HttpUrl parsing plus the same PublicSuffixDatabase and also returns null on PSL exceptions. Inspect dependency/resource loading and compare behavior with upstream before deciding whether to use the upstream helper directly or fix the missing test resource. Keep the invalid-URL fail-open behavior only where upstream does so.

A related production wiring review found SharedPreferencesSourceLoginSessionStore obtains ANDROID_ID using Settings.System.getString(..., Settings.Secure.ANDROID_ID); correct API is Settings.Secure.getString, matching Android's secure setting and the factory's existing provider. Verify fallback behavior against pinned AppConst.androidId before editing.

### Registered correction scope

- Inspect exact current implementations, source-key identity flow from BookSource.id through SourceRequestFactory into SourceHttpRequest.sourceId, all call sites, and the current tests before editing.
- Prove AES output parity with the pinned primitive and Android Base64.NO_WRAP; fix the JVM test boundary without changing crypto semantics. Add a deterministic known ciphertext fixture and retain round-trip, malformed ciphertext, missing key and plaintext-migration tests.
- Diagnose and fix the public-suffix lookup/resource problem so valid unrelated hosts do not receive saved login headers; retain same-site/subdomain behavior and upstream invalid URL fail-open behavior. Add explicit IPv4/IPv6 and multi-level suffix cases where supported by pinned upstream semantics.
- Correct the Android ID Settings API only after confirming pinned AppConst.androidId source and add a testable provider/fallback boundary.
- Preserve source isolation, cookie behavior, request cancellation, validation and secret redaction. No broad BaseSource ABI, WebView/UI or batch API changes.

### Acceptance gates

- Focused login/session tests pass, including the three previously failing cases.
- Existing login header merge/source isolation and Cookie jar parse/replace/remove/reset tests pass.
- Full :app:testDebugUnitTest, :integration-contract:test, pinned Rhino artifact verification and :app:assembleDebug pass on the exact final code head.
- Inspect JUnit XML/HTML and APK artifact; no success claim from queued or stale workflows.
- Re-audit the entire diff and source identity path; update all four checklists with actual outcomes and unresolved device/source validation.
- Physical Android device behavior, full Legado runtime and Badcat five-category parity remain separate open gates.

Self-review: this task corrects the three observed CI failures plus the directly related Android ID setting lookup. It does not broaden the previously registered login/session feature scope and does not authorize marking the full login/session or Legado runtime complete.


## 2026-11-20 checklist correction — Android ID setting namespace review

Correction to the pre-registered CI-failure note above: pinned AppConst.kt at revision 330e606c4d57e08adc056ff8ab7a4132c784da9a explicitly reads Android ID with Settings.System.getString(appCtx.contentResolver, Settings.Secure.ANDROID_ID) ?: "null". The host SharedPreferencesSourceLoginSessionStore uses the same Settings.System API and fallback, so this is not a production mismatch and must not be changed to Settings.Secure.getString. The separate BadcatReadingEngineFactory provider is not the loginInfo persistence provider and is not evidence to alter the pinned BaseSource contract. Remove the proposed Settings API correction from scope; preserve the pinned AppConst behavior exactly. This correction was made before any runtime code edit.

## 2026-11-20 preregistered correction: exact-head CI run 44621698553 for code head c2e1d0821a473fe93335c7fcdec15b2bdd0b9cb3 failed at :app:compileDebugKotlin because SourceLoginSession.kt imports io.legado.app.utils.NetworkUtils, which is not in the app module compile classpath. The pinned NetworkUtils.kt is an app-wide upstream source file; integration-contract compiles only selected parser sources and legado-rhino does not package this utility. AnalyzeUrl.kt is not integrated as a whole either.

Before further runtime edits, replace the unavailable import with a narrow host-side domain adapter matching the exact pinned NetworkUtils.getSubDomainOrNull contract used by AnalyzeUrl.isLoginHeaderSite: HTTP/HTTPS base URL validation, java.net.URL host parsing, upstream-equivalent IPv4/IPv6 handling, OkHttp PublicSuffixDatabase.getEffectiveTldPlusOne(host), null on parse/PSL exception, and the pinned host fallback where a valid host has no eTLD+1. Do not import the whole upstream app utility or its broad dependencies; do not invent a suffix list or change fail-open semantics. Audit all helper callers and preserve source-id identity.

Required tests: same host, same registrable-domain subdomain, unrelated CDN, multi-label suffix such as example.co.uk, IPv4/IPv6 as pinned upstream supports, malformed source/target URLs, and PSL resource failure. Keep Robolectric Android Base64 tests. Acceptance: focused login/session tests, full app unit tests, contract tests, pinned Rhino artifact verification, exact-head Debug APK assembly, and inspection of JUnit reports/APK artifact. No physical-device or full Legado completion claim. This corrects only the registered login/session module-boundary mistake; it does not authorize BaseSource ABI, batch, UI/WebView, or java.getSource changes.

## 2026-11-20 preregistered correction — OkHttp 5.4 Android public-suffix context initialization: exact-code-head run 44622851429, attempt 2, passed :integration-contract:test and pinned Rhino artifact verification, but :app:testDebugUnitTest reported 276 tests with one failure in SourceLoginSessionTest.loginHeadersAreOnlyAttachedToThePinnedEffectiveTldPlusOneSite: expected example.co.uk, got null. The helper catches the exception and returns null as pinned NetworkUtils does, so this is a PSL loader initialization issue, not a reason to change the upstream fail-open contract.

Pinned OkHttp 5.4.0 source was inspected: PublicSuffixList.android.kt selects AssetPublicSuffixList; AssetPublicSuffixList.listSource reads PlatformRegistry.applicationContext.assets and throws when the context is unset. OkHttp.android.kt exposes okhttp3.OkHttp.initialize(applicationContext), and PlatformInitializer normally supplies context through AndroidX Startup. The host test uses @Config(manifest = Config.NONE), which intentionally suppresses the merged manifest Startup provider. Thus the Robolectric test has no OkHttp application context and cannot load PublicSuffixDatabase.list.

Before further code edits, register the narrowly scoped test-harness correction: initialize OkHttp with RuntimeEnvironment.getApplication() before any PublicSuffixDatabase call in SourceLoginSessionTest (for example, @Before). Keep @Config(manifest = Config.NONE) unless there is a concrete reason to change it. Inspect the merged production manifest to verify the upstream AndroidX Startup initializer remains present; do not add redundant production initialization unless that inspection shows the initializer is absent. Do not vendor or hard-code the public suffix list, alter the domain algorithm, fail closed/open differently, change OkHttp version, or change the upstream helper. Acceptance: the domain tests resolve example.co.uk and third-party-cdn.net; same-site subdomain, CDN exclusion, IPv4/IPv6, invalid URL fail-open, AES and Cookie tests pass; full 276+ app tests, contract suite, pinned Rhino verification and Debug APK assembly pass on exact code head. Inspect reports and merged manifest. Device validation remains open.

## 2026-11-20 preregistered follow-up — run 44623818154 (head 5763af1e6aa3af9b13809553c800be65f748e712) confirmed :integration-contract:test and pinned Rhino artifact verification pass, while :app:testDebugUnitTest has 276 tests with only the PSL/eTLD+1 test failing at its first example.co.uk assertion. The @Before OkHttp.initialize call alone did not resolve the lookup.

Further pinned OkHttp 5.4.0 source inspection: PublicSuffixDatabase holds a singleton BasePublicSuffixList whose first load attempt is cached permanently, including IOException; Platform.resetForTests() both reselects the runtime Platform and resets PublicSuffixDatabase's singleton. OkHttp.initialize() only sets applicationContext when the current Platform implements ContextAwarePlatform. Therefore the Robolectric setup must reset Platform/PublicSuffixDatabase after Robolectric has established the Android SDK context, then call OkHttp.initialize(RuntimeEnvironment.getApplication()) before any suffix lookup. This specifically addresses stale platform selection or a cached first-load failure; do not change production domain matching or catch/fallback semantics.

The pinned OkHttp Android AAR manifest contributes androidx.startup.InitializationProvider metadata for okhttp3.internal.platform.PlatformInitializer, which assigns PlatformRegistry.applicationContext. The app's source manifest does not show this dependency-merged provider; verify the merged release/debug manifest after a successful assembly. Config.NONE intentionally removes manifest startup in this unit test, so explicit test initialization remains required.

Before further code edits, register this exact test harness correction: call okhttp3.internal.platform.Platform.resetForTests() before OkHttp.initialize(RuntimeEnvironment.getApplication()) in the Robolectric @Before setup. Keep the existing assertions. Re-run the exact-head full suite and inspect JUnit output. If still failing, capture the underlying IOException/cause from PublicSuffixDatabase rather than adding a suffix list, hardcoding domains, changing OkHttp, or altering pinned upstream fail-open behavior. Acceptance: example.co.uk and unrelated CDN eTLD+1 resolve; same-site/subdomain and cross-site header behavior, IPv4/IPv6 and malformed URL cases pass; all app tests, contract tests, pinned Rhino checks, APK assembly and merged manifest inspection pass. Physical device validation remains open.


## 2026-11-20 login-session PSL test diagnosis — diagnostic-only commit

- Re-read the mandatory project instructions and all four checklists on the active `integration/legado-upstream-runtime` branch before continuing. Pinned Legado revision remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- The exact-code-head CI run `44625634547` for `2f1a921a411665f331e42a3d215baa275fe352b9` completed with one failure in `SourceLoginSessionTest.loginHeadersAreOnlyAttachedToThePinnedEffectiveTldPlusOneSite`; 275 of 276 app unit tests passed. Contract tests and pinned Rhino artifact verification passed; APK assembly was skipped.
- Test report artifact `14579800870` confirms the first failed assertion is the effective-TLD+1 lookup for `www.example.co.uk`, which returned null. AES round-trip/migration, Cookie callback, source isolation, validation and other login-session tests passed in that run.
- Diagnostic-only test commit: `ab0150a2cf5ce070ec2ef8d322f5bd2edf6475cb`. It adds a direct OkHttp `PublicSuffixDatabase.getEffectiveTldPlusOne` assertion that preserves the underlying exception as the AssertionError cause. No production code, PSL behavior, URL parsing, fail-open semantics or pinned dependency was changed.
- CI run `44626750474` did not reach tests: Gradle failed resolving `org.jetbrains.kotlin.plugin.parcelize:2.4.20` from configured plugin repositories. This is recorded as dependency-resolution/build infrastructure failure, not as a test result. A rerun of the failed job was requested; its result is pending.
- Current acceptance remains open: obtain the direct PSL failure cause; resolve any demonstrated test asset/context issue without hardcoded suffixes or production semantic changes; pass exact-head app tests, contract tests, pinned Rhino verification and Debug APK assembly; inspect merged manifest and reports. Physical-device validation, full Legado runtime and five-category parity remain separate open gates.
- Self-review: this diagnostic increment stays within the preregistered PSL test-harness correction. It does not establish the login/session slice or broader runtime as complete.


## 2026-11-20 preregistered correction — Robolectric OkHttp Android PSL asset is absent

### Evidence from exact-head CI

- Diagnostic code head `ab0150a2cf5ce070ec2ef8d322f5bd2edf6475cb`, run `44626750474`, failed only `SourceLoginSessionTest.loginHeadersAreOnlyAttachedToThePinnedEffectiveTldPlusOneSite`; 276 tests ran, one failed. The direct PSL assertion preserved the root cause:
  - `IllegalStateException: Unable to load PublicSuffixDatabase.list resource.`
  - Caused by `java.io.FileNotFoundException: PublicSuffixDatabase.list`
  - Origin: OkHttp 5.4.0 `AssetPublicSuffixList.listSource` calling Robolectric `AssetManager.open("PublicSuffixDatabase.list")`.
- This confirms `Platform.resetForTests()` and `OkHttp.initialize(RuntimeEnvironment.getApplication())` correctly reach the Android asset loader but the unit-test sandbox does not contain the AAR-provided asset. It is not a PSL algorithm mismatch or a production domain fallback issue.
- Exact branch head at registration: `bf6bab98963b90eb5e4842031e7f448e843df2a6`; pinned Legado SHA remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.

### Registered test-harness-only correction

- Inspect OkHttp 5.4.0 published Android and JVM artifacts/Gradle module metadata and the Android AAR asset packaging. Keep production `implementation("com.squareup.okhttp3:okhttp:5.4.0")` unchanged.
- Make the Robolectric unit-test runtime consume the matching `com.squareup.okhttp3:okhttp-jvm:5.4.0` implementation (whose PSL loader reads the JVM classpath resource) instead of the Android AAR asset loader, using a narrowly scoped test-runtime dependency substitution/exclusion only if the published metadata confirms API/binary compatibility. Keep test compile API compatible; remove Android-only test initialization only if the JVM test runtime no longer exposes it. Do not let both duplicate OkHttp implementations coexist at test runtime.
- If the JVM artifact cannot be substituted cleanly, instead configure Robolectric to consume the exact dependency-provided Android AAR asset from the generated merged-assets output. Do not copy/vendor the PSL file, download a separate list, synthesize suffixes, or hard-code domains.
- Audit all usages of OkHttp internal test-only APIs and all app test runtime dependencies; production remains on Android AAR and must retain its AndroidX Startup initialization provider and `PublicSuffixDatabase.list` asset.
- Keep production `legadoSourceDomain`, `shouldAttachSourceLoginHeader`, pinned upstream fail-open behavior and login-header security semantics unchanged.

### Required regressions and acceptance

- Verify the selected test runtime resolves one OkHttp implementation only and loads its official 5.4.0 PSL resource successfully.
- Retain eTLD+1 checks for `www.example.co.uk`, `cdn.example.co.uk`, unrelated `third-party-cdn.net`, IPv4, IPv6, malformed URL and upstream invalid-source fail-open behavior.
- Run focused login/session tests, full `:app:testDebugUnitTest`, `:integration-contract:test`, pinned Rhino artifact verification and `:app:assembleDebug` on the exact final code head.
- Inspect JUnit XML/HTML, test runtime dependency graph, assembled APK contents for Android `assets/PublicSuffixDatabase.list`, and merged manifest for AndroidX Startup `PlatformInitializer`.
- No APK delivery until CI artifacts are verified; physical-device install, complete Legado runtime and Badcat five-category parity remain separate gates.

Self-review: this correction is strictly test-runtime resource wiring. It does not modify production PSL data/logic, the pinned upstream revision, OkHttp production version, cookie behavior, login state, or broaden the login/session feature scope.


## 2026-11-20 preregistered correction — Gradle test-runtime configuration is created lazily

- Exact code head `eb32dcc5b1b98a7297759126315c09473d035a3d`, Actions run `44628470076`: workflow stopped before tests because `configurations.named("debugUnitTestRuntimeClasspath")` was evaluated before Android Gradle Plugin had created that variant configuration. Error: `Configuration with name 'debugUnitTestRuntimeClasspath' not found.`
- This is a build-script configuration timing defect; no unit tests ran, no APK was assembled, and no conclusions can be drawn about the OkHttp JVM PSL substitution yet.
- Registered narrow correction: replace eager `configurations.named(...)` lookup with a lazy `configurations.configureEach` callback that applies the OkHttp Android-AAR exclusion only when the exact `debugUnitTestRuntimeClasspath` configuration is created. Keep `implementation("com.squareup.okhttp3:okhttp:5.4.0")` unchanged for production and keep `testRuntimeOnly("com.squareup.okhttp3:okhttp-jvm:5.4.0")`. Do not exclude OkHttp from production configurations or alter production login/domain behavior.
- Before editing, verify the active branch remains `integration/legado-upstream-runtime`, inspect the full Gradle dependency/configuration context and all affected test imports/callers. After editing, verify the debug unit-test runtime resolves exactly one OkHttp implementation (JVM), production runtime remains the Android AAR, and the official PSL resource is available to unit tests.
- Acceptance: focused login/session tests including multi-label eTLD+1 and unrelated CDN pass; full `:app:testDebugUnitTest`, `:integration-contract:test`, pinned Rhino artifact verification and `:app:assembleDebug` pass on the exact code head. Inspect JUnit reports, dependency graph, APK asset `assets/PublicSuffixDatabase.list` and merged AndroidX Startup initializer. Device install and broader Legado/five-category completion remain separate open gates.
- Self-review: this is a build configuration timing correction within the previously registered test-only OkHttp substitution. It does not change production runtime semantics or expand the login/session feature scope.


## 2026-11-20 pre-registered implementation scope — Stage 1 official models and lossless serialization (NO RUNTIME CODE EDITS YET)

### Current branch and evidence baseline
- Active branch: integration/legado-upstream-runtime; inspected branch head: 2b2bf91afca2dce8c07642eae4ae95f1a228937c. Do not modify main.
- Pinned upstream Legado revision: 330e606c4d57e08adc056ff8ab7a4132c784da9a.
- Latest exact-code-head Actions run: 44630795195, head 2b2bf91afca2dce8c07642eae4ae95f1a228937c; all reported job steps completed successfully, including contract tests, app unit tests, Rhino verification, Debug APK assembly and live source sampling. JUnit/APK/live-source artifacts still require inspection before recording detailed counts or source outcomes.
- Existing host boundary: integration-contract/.../ReadingEngine.kt defines simplified BookSource, BookCandidate, BookDetail, and BookChapter; LegadoBookSourceCodec parses selected source fields and stores rawJson, while the engine consumes selected fields. FileBookSourceCatalog persists descriptors and mutates enabled state through the codec. The rule entities currently present under app/src/main/java/io/legado/app/data/entities/rule/ are only a partial set of pinned models.
- Pinned upstream model sources identified: app/src/main/java/io/legado/app/data/entities/BookSource.kt, Book.kt, BookChapter.kt, and data/entities/rule/{ExploreRule,SearchRule,BookListRule,BookInfoRule,TocRule,ContentRule,ReviewRule}.kt. These models include Android/Room/Parcelable and runtime-facing contracts; they cannot be copied blindly into the platform-neutral module.

### Exact implementation target
Build a deliberate model/serialization compatibility boundary for Stage 1:
- Compare the exact pinned upstream declarations, nested types, default values, annotations/converters and serialization behavior for BookSource, Book, BookChapter and all source rule models.
- Inventory all current host model declarations, JSON codecs, catalog persistence/mutation/export paths, runtime constructors, UI adapters, tests and callers before changing any type or signature.
- Define and implement a lossless representation/adapter strategy that retains every official known field plus unknown/future fields across import -> in-memory representation -> persistence -> edit/toggle -> export. Preserve source identity semantics (bookSourceUrl) and book/chapter identity and ordering semantics.
- Map nested rule models using the pinned upstream contract; do not leave rule objects as opaque strings where runtime behavior needs typed models, and do not invent defaults that differ from upstream.
- Keep Android/Room/Parcelable concerns at the Android boundary; do not introduce an incompatible dependency or break the platform-neutral contract merely to copy upstream annotations.
- Keep existing UI and all current ReadingEngine callers source-compatible during the migration, or add explicit, tested adapters and migrate every caller in the same change.

### Compatibility/security risks to audit before edits
- Room annotations, Parcelable, ignored/transient properties, converters, lazy variables, BaseSource identity, and upstream helper dependencies may not compile or behave in the current host module.
- Gson/Kotlin serialization differences: missing vs null vs default values, numeric coercion, enum/unknown values, nested rule objects encoded as object or string, ordering, escaping, and unknown-field preservation.
- Existing code relies on BookSource.id/baseUrl for source identity and login/session scoping; do not change this identity path accidentally.
- Existing raw JSON preservation and import bounds/validation must not regress; mutations must alter only the intended fields and retain all other JSON members.
- Book/chapter key collisions, chapter indices, duplicate URLs, and runtime call sites must be audited before mapping to upstream Book / BookChapter.

### Required tests and acceptance
- Add pinned-upstream model contract tests for required/optional/default fields, nested rule types, unknown fields, null/missing values, enum/type edge cases, and identity/equality behavior where relevant.
- Add import/export round-trip fixtures from official upstream JSON, including future unknown keys and every rule family; verify semantic equality and field preservation.
- Add persistence round-trip and targeted mutation tests: import -> reload -> enable/disable or edit one field -> export; assert all unrelated fields remain byte/semantically intact.
- Add regression tests for every existing BookSource, BookChapter, LegadoBookSourceCodec, FileBookSourceCatalog, source-login identity, and ReadingEngine caller touched by the change.
- Run focused model/codec/catalog tests, :integration-contract:test, full :app:testDebugUnitTest, and :app:assembleDebug on the exact final code head; inspect XML reports and APK artifact. Verify all five rule models and all audited callers are represented in the adapter map.
- Acceptance is this Stage 1 slice only; it does not establish complete AnalyzeRule/AnalyzeUrl, full Rhino, unified runtime, batch support, UI parity, physical-device validation or release readiness.

### Explicit exclusions
No UI redesign; no changes to main; no selector/rule-engine rewrite; no Rhino extension expansion; no java.getSource() ABI change; no batch API/cache implementation; no unsupported stubs/no-op shims; no release APK claim.

### Required self-review before code
Re-read all five mandatory files and this registration; verify branch head again; inspect full pinned upstream model declarations and every affected host caller/adapter/test; list compatibility risks and test cases before editing. If inspection shows this registered scope is incomplete, amend this ledger first. After implementation, record exact changed paths, test results, CI run/head, artifact inspection, remaining gaps and a checklist self-review.


## 2026-11-20 mandatory reread and current Stage 1 implementation status

- Re-read `AGENTS.md` and all four mandatory project checklists on the active `integration/legado-upstream-runtime` branch before continuing. Also re-read `docs/BADCAT_REPLICATION_FIVE_CATEGORIES.md` and `docs/upstream-model-serialization-audit.md` because the former defines product parity separately and the latter governs the Android/JVM model boundary.
- Current branch head at this review: `5466caaf7cbbf2035b393b8dde70ef286d93a56e`. Pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`. `main` was not modified.
- This record supersedes the stale “NO RUNTIME CODE EDITS YET” baseline above: code commit `5466caaf7cbbf2035b393b8dde70ef286d93a56e` adds the upstream `BookSource.customOrder` field to the platform-neutral source projection, codec import/export mapping and regression fixtures. It is a narrow staging-model parity correction, not the official mutable Android `BookSource`/`BaseSource` runtime integration.
- The seven vendored source-rule entity files (`ExploreRule`, `SearchRule`, `BookListRule`, `BookInfoRule`, `TocRule`, `ContentRule`, `ReviewRule`) were compared by Git blob SHA with the exact pinned upstream files; all seven are byte-identical. This confirms source provenance only, not complete model/runtime behavior.
- Exact-code-head Actions run [#44633804009](https://github.com/xkszds/badcat-legado-client/actions/runs/44633804009), head `5466caaf7cbbf2035b393b8dde70ef286d93a56e`, is still `in_progress` at the final best-effort four-source uploaded-corpus sample. The contract tests, pinned Rhino verification, bounded Rhino host tests, app unit tests, Debug APK assembly/upload, Nyasama smoke, repository probe, Feisuzhong E2E and Pinshuzhai E2E steps are completed successfully; the last uploaded-corpus sample/report step is not yet complete.
- Downloaded unit-test XML artifact `14582903590`: 276 tests, 0 failures, 0 errors, 0 skipped.
- Downloaded Debug APK artifact `14582783770`: extracted APK size 18,163,811 bytes, SHA-256 `05b26ece98fda97321e94951e2303fbfa35cb85e3e6ddb185f50626d3650ddf9`. APK ZIP inspection confirms `assets/PublicSuffixDatabase.list` and `META-INF/LICENSE.md` are present. This is a Debug artifact, not a signed release and not physical-device validation.
- Live report artifacts `14583189639` and `14583199685` independently confirm Bixiange GBK POST search -> detail -> 114-page TOC -> chapter content, and Pinshuzhai search -> detail -> 1,650 chapters -> chapter content. The four-source uploaded-corpus sample remains pending; do not imply it passed.
- Next code work remains within Stage 1 and must follow the model audit: no isolated/fake `BookSource` or `BaseSource`; inspect the exact pinned service/model graph and all host callers, keep Android Room/Parcelable concerns out of the JVM contract, retain raw JSON for unknown-field losslessness, and pre-register a narrower adapter slice before runtime edits.
- Product parity remains separate: Badcat categories 1, 2, 4 and 5 are not completed by Legado category-3 progress. No full-client or final-APK claim is authorized.
- Self-review: the earlier Stage 1 preregistration's branch baseline and “no runtime edits yet” label became stale after commit `5466caa`; this record explicitly supersedes those statements. Checklist scope remains valid, but the next implementation must respect the more specific Android model/service boundary in `upstream-model-serialization-audit.md`.

## 2026-11-20 Stage 1 dependency cross-check — BaseSource boundary

- Before closing Stage 1 model work, reconcile `BookSource : BaseSource` against this method/overload audit. `BaseSource` is a concrete runtime dependency surface, not a marker or a harmless interface shim; its inherited `JsExtensions`, source state, login, CookieStore, cache, shared JS scope and navigation context must be mapped to real host services.
- No isolated BookSource integration, fake BaseSource implementation, empty context, no-op extension or `null` service injection is authorized by the model-stage registration. Record each dependency and caller in `upstream-model-serialization-audit.md`; only then register an implementation slice.
- Baseline branch head: `eabbef43481e6c14b8bda6ea62f03c5213d1188d`; pinned upstream SHA remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
