# Full upstream Legado runtime migration

> 长期用户需求与五大类完整验收清单：[docs/USER_REQUIREMENTS_AND_PARITY_CHECKLIST.md](USER_REQUIREMENTS_AND_PARITY_CHECKLIST.md)。每次继续开发前必须先读该清单并更新完成状态。

## Goal

Make Badcat's online book-source execution use the same implementation and semantics as the pinned Legado Android app, rather than extending the bounded in-house parser. The compatibility target is the upstream application at:

- Repository: https://github.com/LegadoTeam/legado
- Immutable revision: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- License: GPL-3.0

The source is checked out at `upstream/legado` as a Git submodule. Do not move the pin to a floating branch. Upgrade only through a reviewed pin change.

## What the source audit found

At this revision, the upstream app contains 2,078 files under `app/src/main`, including 1,078 Kotlin/Java source files. The online source pipeline is in the app layer, not in `:modules:book` or `:modules:rhino` alone.

The upstream execution path is:

1. `BookList` — search and explore list evaluation.
2. `BookInfo` — book detail parsing and source initialization.
3. `BookChapterList` — TOC extraction, pagination, ordering, deduplication and chapter metadata.
4. `BookContent` — chapter retrieval, content rules and replacement.
5. `AnalyzeRule` — CSS/JSoup, XPath, JSONPath, regex, JS, selector chains and rule data bindings.
6. `AnalyzeUrl` — URL interpolation, headers, cookies, request method/body, charset, timeout, redirects, retries, WebView and request options.
7. `JsExtensions` and the upstream Rhino runtime — JavaScript host APIs and per-source/shared script state.

These components depend on upstream source entities, Room persistence, cache/state services, application initialization, HTTP clients, WebView services, and coroutine context. Copying only `AnalyzeRule.kt`, adding Jsoup, or adding the two Android library modules would still not provide parity.

## Integration decision

Use the upstream Android application source as the behavior reference and migrate its source-execution slice in-process. Keep Badcat's existing UI as a host adapter, but remove its custom parser as the authority for BookSource execution.

Do not use the Legado HTTP API or ContentProvider as the final implementation: both require a separately installed Legado app and therefore do not satisfy a self-contained Badcat app.

The upstream `App` initialization and global services must be adapted explicitly. Do not instantiate upstream parser classes with fake/null `appCtx`, `appDb`, CookieStore, CacheManager or WebView dependencies.

## Staged implementation gates

### Gate 0 — immutable source and build baseline
- [x] Pin exact upstream commit and license.
- [x] Add the upstream source as a submodule.
- [x] Make CI check out the pinned source.
- [x] Verify the submodule commit and GPL license in CI.
- [ ] Record and verify upstream source and dependency checksums.

### Gate 1 — upstream model and runtime build
- [ ] Integrate upstream `BookSource`, rule entities, `BaseSource`, `BaseBook`, `Book`, `BookChapter`, and serialization converters without dropping fields.
- [ ] Integrate the exact upstream Rhino module and initialization.
- [x] Port the pinned upstream `RuleAnalyzer`, `AnalyzeByJSoup`, and `SourceHtmlParser` verbatim; add a production adapter and regression tests for the reported `@.w_txt li.4@textNodes` expression, legacy index ordering/exclusion, selector chains, and rule combination.
- [ ] Integrate the remaining XPath, JSONPath, regex and all other rule parser implementations at the upstream versions.
- [ ] Run original upstream unit tests relevant to the source engine.

### Gate 2 — request and host services
- [ ] Adapt `AnalyzeUrl` to Badcat's injectable HTTP transport while retaining upstream URL-option semantics.
- [ ] Provide source-scoped CookieStore, login state, script key/value storage, cache, WebView and DNS behavior.
- [ ] Preserve upstream request/response objects and callback behavior; do not replace them with string-only bindings.

### Gate 3 — full reading vertical slice
- [ ] Integrate upstream search/explore, detail, TOC and content pipeline as a coherent slice.
- [ ] Preserve TOC pagination, volume nodes, ordering, duplicate handling, VIP/pay metadata, contentBatch, replacements and chapter state.
- [ ] Adapt persistence and cache with migrations and deterministic tests.

### Gate 4 — Badcat UI adapter and acceptance
- [ ] Connect Badcat source catalog and reader screens to the upstream pipeline.
- [ ] Keep imported BookSource JSON round-trip lossless.
- [ ] Run source fixtures for CSS, XPath, JSONPath, regex, JS, mainJs/jsLib, login, cookies, POST, pagination and dynamic WebView.
- [ ] Run real-source Android end-to-end tests across a broad, diverse source set.
- [ ] Confirm search → detail → TOC → first chapter → next chapter on device.
- [ ] Only then describe source compatibility as equivalent to the pinned upstream revision.

## Current status

This branch includes the immutable upstream source baseline and an attempted integration of upstream JSoup rule evaluation into the shared dispatcher. The latest CI run `44385813513` is **failed** at `:integration-contract:test`: chained collection root extraction, legacy colon-separated index/exclusion semantics, and the Bixiange public source search fixture all fail. Fix these before extending parser coverage. This branch does **not** integrate the complete runtime and does not claim compatibility. The full five-category user scope and per-session requirements are tracked in `USER_REQUIREMENTS_AND_PARITY_CHECKLIST.md`.


## 2026-11-19 dependency audit update

The detailed source-level dependency audit is now recorded in [upstream-analyzer-integration-audit.md](upstream-analyzer-integration-audit.md). It confirms that the pinned `AnalyzeRule` and `AnalyzeUrl` classes are app-layer lifecycle components, not standalone parsers: they depend on upstream entities, source-scoped Rhino state, `JsExtensions`, HTTP/cookie/cache/WebView services, coroutine context and batch persistence. The current JVM module compiles selected parser files only, while the app's per-stage CSS services and JSON staging model remain separate.

CI run [44550830295](https://github.com/xkszds/badcat-legado-client/actions/runs/44550830295) for head `6e1ffd1e513cc2e77581c889822e28e54f63558a` completed successfully, including tests/build and best-effort live source probes. This does not change the incomplete runtime status or satisfy device validation.

**Next gate:** integrate the upstream model/rule entity graph and serialization first. Do not copy `AnalyzeRule.kt`/ `AnalyzeUrl.kt` into the host until their real dependencies and service contracts are integrated.


## 2026-11-19 session ledger

- [x] Re-read root `AGENTS.md`, both product/engine checklists, project completion checklist, UI map, PPRule evidence and original purification evidence before continuing.
- [x] Completed the pinned source/rule model dependency audit and recorded it in `upstream-model-serialization-audit.md`.
- [ ] Model integration remains open: no official runtime entities, official Gson converters or Room-backed runtime source model have been integrated yet.
- [ ] Next: implement the Android-side model/serialization adapter with exact upstream behavior and fixtures; preserve raw JSON in parallel. Re-run the complete pre-change compatibility search before editing.
- Latest head CI run [#44554459580](https://github.com/xkszds/badcat-legado-client/actions/runs/44554459580) is pending. Earlier run [#44553806204](https://github.com/xkszds/badcat-legado-client/actions/runs/44553806204) passed contract/Rhino tests and Debug APK build/upload, while the final uploaded-corpus live step was still in progress.


## 2026-11-19 first upstream rule-model code slice

- Vendored the exact pinned source-rule entities and `GsonExtensions.kt` into the Android module, preserving their upstream package names and behavior.
- Added Android-side test coverage for six nested BookSource rule types, object-form and legacy string-encoded JSON, null/default behavior and selected rule-specific semantics.
- Added Parcelize plugin and Flexbox dependency matching pinned upstream versions. GPL-3.0 provenance is recorded in `THIRD_PARTY_NOTICES.md`.
- This is only the rule-model/JSON foundation. No claim is made that BookSource/BaseSource, Room converters, AnalyzeRule/AnalyzeUrl or the complete reading lifecycle are integrated.
- CI for the new code commit must be inspected before this increment is considered verified.


## 2026-11-19 verified model-slice CI and next Legado gate

- [x] CI run [44556688812](https://github.com/xkszds/badcat-legado-client/actions/runs/44556688812), head `8b9d07e617fe7b2469af70aaa17ee28e79c7a0f2`: contract unit tests passed; bounded Rhino host tests passed; Debug APK assembly passed and the APK artifact was uploaded.
- [ ] Workflow live-source smoke steps were still running at the time of this ledger update. Do not report the whole workflow as completed, and do not treat a CI APK as final complete-Legado release.
- [x] Re-read pinned upstream `BookSource.kt` and `BaseSource.kt`: BookSource is a Room/Parcelable entity implementing BaseSource; BaseSource extends the 1,390-line JsExtensions API and carries source lifecycle, login, cookie, script and cache behavior.
- [ ] Next gate is the full BaseSource/JsExtensions host-service dependency map. Do not copy BookSource with a fake BaseSource or no-op methods. Map every dependency to an actual host service or exact upstream unsupported/error semantics, then vendor the upstream entity graph and converters with compatibility tests.
- [ ] Existing host BookSource/ReadingEngine DTOs remain projections; raw JSON retention is still required for lossless import/export. They are not upstream runtime entities.


## 2026-11-19 JsExtensions declaration audit

A method-name comparison against the pinned upstream `JsExtensions.kt` is recorded in [upstream-js-extensions-compatibility-audit.md](upstream-js-extensions-compatibility-audit.md). It identifies 18 upstream method names absent from the current host bridge and a blocking `getSource()` return-type collision, plus the incomplete batch-context contract. Same-name methods are not yet considered behaviorally compatible. The next task is to complete the overload/semantic matrix and migrate the existing bridge around real host services before implementing upstream BaseSource/BookSource.


## 2026-11-19 complete JsExtensions inheritance audit

The declaration-level audit now includes all 122 pinned `JsExtensions` methods and all 30 inherited `JsEncodeUtils` methods. The current host bridge has 18 missing direct method names plus 19 missing inherited crypto method names. It also has incompatible return types for `getSource()`, batch context, Jsoup response wrappers and file handles. See [upstream-js-extensions-compatibility-audit.md](upstream-js-extensions-compatibility-audit.md). These differences are recorded as blockers, not accepted compatibility exceptions. The next code change must address a bounded, testable slice of this matrix and re-audit old callers; do not integrate BookSource on top of the current incompatible bridge.

## 2026-11-19 session gate — crypto dependency audit before implementation

Before changing runtime code, the mandatory checklist now records a Legado-only task to resolve the pinned upstream crypto dependency graph and re-audit the host bridge. The next bounded investigation is to locate the exact Hutool/dependency declarations used by upstream revision 330e606c4d57e08adc056ff8ab7a4132c784da9, inspect the pinned SymmetricCryptoAndroid, AsymmetricCrypto, and Sign contracts, and search every existing host helper/caller/test. No crypto method may be mapped to the current AES/DES-CBC-only helper unless its complete upstream semantics match. The current-head CI run 44558528157 is pending and must be inspected before treating the prior documentation increment as verified. This entry is a pre-work gate, not a claim that dependency or runtime work is complete.

## 2026-11-19 crypto dependency audit findings and next gate

The pinned upstream dependency coordinates are now verified: Hutool Crypto 5.8.22, Bouncy Castle provider 1.86, and Bouncy Castle PKIX 1.86. The host app currently has none of these dependencies. The exact upstream `SymmetricCryptoAndroid`, `AsymmetricCrypto`, `Sign`, and `JsEncodeUtils` crypto factory/legacy method contracts have been inspected and recorded in `upstream-js-extensions-compatibility-audit.md`.

The host's current `createSymmetricCrypto` returns an AES/DES-CBC-only host facade, while the upstream factory returns a Hutool `SymmetricCrypto` backed by `SymmetricCryptoAndroid`. Existing runtime tests exercise AES and DES helper paths; the runtime binds the source-scoped helper object as `java`. Existing JavaImporter tests also enforce that general `Packages.javax.crypto` and arbitrary Java classes remain unavailable. These are mandatory regression/security constraints.

**Next gate:** perform a complete repository grep for the factory/helper and all crypto API names before changing signatures. Then add the exact pinned dependencies and crypto classes, expose the upstream factories through the source-scoped `java` bridge only, preserve upstream legacy method quirks, and add Rhino/API/security regression tests. No runtime crypto implementation is claimed complete by this audit.

### Additional host call-path findings

All 23 Kotlin files under the app runtime package and all 29 app test Kotlin files were scanned for the relevant crypto method/helper identifiers. Existing dynamic source-script calls are in `RhinoSourceScriptRuntimeTest.kt`: AES/DES symmetric factory use, AES Base64 decoding, digest/HMAC helpers, and the restricted JavaImporter boundary. `RhinoSourceScriptRuntime.kt` binds the per-source `RhinoSafeSourceJavaExtensions` instance as `scope["java"]` and separately installs a narrow `LEGADO_CRYPTO_COMPAT_JS` Packages/JavaImporter shim. `RhinoLegadoJsReplacementEvaluator.kt` has its own digest/HMAC implementation and must be preserved/re-audited independently.

This is not yet a full repository grep: app UI directories, integration-contract, and legado-rhino have not been scanned for the target identifiers. Do not change signatures or remove old helpers until that final scope is checked.

## 2026-11-19 CI recheck

Run [44557806917](https://github.com/xkszds/badcat-legado-client/actions/runs/44557806917) for code head `28cff4a9771fdbd32b4a085386c3d435db610b82` has now completed successfully. Its contract tests, bounded Rhino host tests, Debug APK build/upload and configured live smoke steps (including the four-source uploaded-corpus step) all completed successfully. Artifacts include Debug APK `14546626986`, unit test reports `14546077767`, and live-source reports. This run does not cover the current documentation-only head.

Run `44558528157` for head `6fa843febb293e95a9a0da44acc9e38b64776b48` ended as cancelled, not passed. No CI result for the current documentation head is claimed. The successful earlier run does not establish full Legado parity or physical-device acceptance.

## 2026-11-19 full host crypto caller search and implementation gate

The pre-change caller scan is now complete across all 231 Kotlin files and all 21 Java files in the current repository tree (app main/tests, integration-contract main/tests, pinnedLegado sources, and legado-rhino). No additional references to the affected crypto factory/helper identifiers were found outside the already documented source bridge, Rhino runtime tests, and the separate `RhinoLegadoJsReplacementEvaluator` digest/HMAC path.

The next code task is now explicitly registered in the mandatory checklist: add the exact pinned Hutool Crypto 5.8.22 and Bouncy Castle 1.86 dependencies, port the pinned `SymmetricCryptoAndroid`, `AsymmetricCrypto`, and `Sign` classes plus only their exact Base64/hex helper semantics, and complete the missing `JsEncodeUtils` crypto methods through the source-scoped `java` bridge. Preserve the JavaImporter sandbox and the independent replacement evaluator. Add AES/DES/3DES/RSA/signature, overload, null/error, Hex/Base64 and old-behavior Rhino regression tests. Re-audit the complete repository after implementation and inspect Android build/CI/provider behavior before claiming compatibility.

This gate was recorded before runtime code edits. No runtime crypto implementation is claimed complete by the scan.


## 2026-11-19 pinned crypto runtime implementation (verification pending)

Implemented the registered crypto slice: app dependencies now include pinned Hutool Crypto 5.8.22 and Bouncy Castle 1.86 provider/PKIX; the Android app contains the pinned upstream `SymmetricCryptoAndroid`, `AsymmetricCrypto`, `Sign`, `EncoderUtils`, and exact `String.isHex()` helper. `RhinoSafeSourceJavaExtensions` now exposes the upstream symmetric factory overloads, asymmetric/signature factories, and the 19 missing legacy AES/DES/3DES crypto method names through the existing per-source `java` bridge. The old AES/DES-CBC-only facade was removed, while the separately bounded `aesCrypt`/JavaImporter path remains.

Regression tests were added for AES legacy helpers and historical quirks, DES/3DES, random key and factory overloads, RSA encrypt/decrypt, and SHA256withRSA sign/verify. The independent replacement evaluator digest/HMAC methods and JavaImporter sandbox were not changed. All verification is pending CI; no Android runtime/provider success is claimed.


## 2026-11-19 crypto resource-boundary correction gate

Post-implementation diff review found a security/resource regression risk: the removed host-only symmetric facade enforced a 2 MiB input/output cap, while a raw Hutool object is unbounded. The separate JavaImporter AES helper remains bounded but does not protect `createSymmetricCrypto` return objects.

Before any further runtime code edit, add a host-owned bounded wrapper/subclass around the pinned Hutool crypto objects. Preserve the upstream algorithm and method surface, but cap byte/string/InputStream input and output at the prior 2 MiB boundary; read caller-owned streams without closing them and guarantee forward progress. Apply equivalent bounds to asymmetric encryption and signing/verification. Add exact-limit/over-limit, stream, old crypto behavior and sandbox regression tests. This is a corrective host safety layer, not a return to the former AES/DES-only algorithm restriction.


## 2026-11-19 bounded crypto host adapter (verification pending)

The follow-up safety review is implemented: the upstream Hutool-backed symmetric object is now wrapped by `RhinoBoundedSymmetricCryptoAndroid`, preserving the upstream algorithm/method surface while retaining the former 2 MiB data/result limit. A shared bounded stream reader handles zero-progress reads and never closes caller-owned streams. The `AsymmetricCrypto` and `Sign` wrappers now apply the same data/key/signature bounds. The source bridge constructs the bounded symmetric subclass; it does not restore the old AES/DES-only transformation restriction.

Tests cover exact-limit AES/NoPadding, over-limit input, padding-induced output overflow, oversized InputStreams, zero-progress stream behavior, asymmetric key bounds and signature input bounds. CI/provider verification is pending.


## 2026-11-19 post-commit bridge/test audit correction gate

Diff review after the bounded-wrapper increment found two wiring defects before verification: `createSymmetricCrypto` still constructed the unbounded upstream class instead of the registered bounded subclass, and the bounded regression test had been inserted into the preceding overload test assertion, corrupting the Kotlin source. The next edit must correct the bridge factory to validate key/IV and instantiate `RhinoBoundedSymmetricCryptoAndroid`, then replace the malformed test section with two separate complete test methods. Re-inspect the exact diff and run CI; the current crypto increment is not verified.


## 2026-11-19 crypto bridge/test wiring correction applied

The bridge now checks key/IV sizes and constructs `RhinoBoundedSymmetricCryptoAndroid`, so the old 2 MiB limit is active through the actual source-scoped Rhino `java.createSymmetricCrypto` path. The corrupted adjacent test insertion was repaired into two independent test methods. Asymmetric string ciphertext is decoded through pinned Hutool `SecureUtil.decode` and checked against the decoded-byte limit before processing. Verification remains pending CI.


## 2026-11-20 crypto verification gate — PRE-REGISTERED BEFORE FURTHER IMPLEMENTATION

The current source commit is `f924681f783628dedf6f835e191a701ba5ae6a16` on `integration/legado-upstream-runtime`; pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9`. The immediate task is to verify the just-implemented bounded Hutool/BC crypto slice before beginning BaseSource/BookSource work.

- Inspect CI run `44564284230` (ancestor `24c39c4...`) and current-head run `44564458448` (`f924681...`) independently; inspect step summaries and full logs.
- Verify current-head contract/Rhino tests, Android dependency resolution, provider behavior visible in tests, D8/R8 packaging and Debug APK assembly.
- Review changed crypto implementation and tests for the 2 MiB input/key/output bounds, decoded Base64/Hex bytes, stream progress/ownership, all Hutool overloads and Rhino reflection visibility.
- Do not change runtime code until a concrete defect is found and that corrective task plus its tests is recorded here and in the mandatory user checklist.
- If verification passes, mark only the crypto slice's CI/build gates complete; physical-device validation, complete JsExtensions, BaseSource/BookSource, AnalyzeRule/AnalyzeUrl and end-to-end reading remain open.
- If verification fails, record the exact failing job/step/log evidence and pre-register a narrow correction before editing.

Checklist self-review: older status entries used stale branch heads and mixed pending/current/ancestor CI runs. This section distinguishes them and makes current-HEAD verification explicit.


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
