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

- Current host branch HEAD before ledger update: `f924681f783628dedf6f835e191a701ba5ae6a16`; pinned upstream: `330e606c4d57e08adc056ff8ab7a4132c784da9`.
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
- The pinned upstream implementation is `upstream/legado/app/src/main/java/io/legado/app/utils/ChineseUtils.kt` at `330e606c4d57e08adc056ff8ab7a4132c784da9`. It delegates `s2t/t2s` to quick-transfer-core and loads the exact `TRADITIONAL_TO_SIMPLE` exclusion dictionary before the first traditional-to-simplified conversion. Its excluded terms include `多士`, which the existing regression test requires to remain unchanged.
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
