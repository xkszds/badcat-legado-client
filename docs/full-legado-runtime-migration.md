# Full upstream Legado runtime migration


> **必读入口已调整（2026-11-20）**：本文件保留为详细历史需求/实施记录，不再要求每次开发重读。新的流程入口是 [总流程清单](required/00-MASTER-WORKFLOW.md)，对应分类唯一必读清单是 [第三类 Legado](required/03-LEGADO-RUNTIME.md)。分类清单已汇总当前状态、下一步和验收；只有追查本文件的具体历史细节时才打开。


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

Before changing runtime code, the mandatory checklist now records a Legado-only task to resolve the pinned upstream crypto dependency graph and re-audit the host bridge. The next bounded investigation is to locate the exact Hutool/dependency declarations used by upstream revision 330e606c4d57e08adc056ff8ab7a4132c784da9a, inspect the pinned SymmetricCryptoAndroid, AsymmetricCrypto, and Sign contracts, and search every existing host helper/caller/test. No crypto method may be mapped to the current AES/DES-CBC-only helper unless its complete upstream semantics match. The current-head CI run 44558528157 is pending and must be inspected before treating the prior documentation increment as verified. This entry is a pre-work gate, not a claim that dependency or runtime work is complete.

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

The current source commit is `f924681f783628dedf6f835e191a701ba5ae6a16` on `integration/legado-upstream-runtime`; pinned upstream remains `330e606c4d57e08adc056ff8ab7a4132c784da9a`. The immediate task is to verify the just-implemented bounded Hutool/BC crypto slice before beginning BaseSource/BookSource work.

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



## 2026-11-20 correction: symmetric encoded-output cap was not in the original host contract

Inspection of the complete pre-crypto `RhinoLegadoSymmetricCrypto` at commit `9d9b2ef47e801e3c5b6bff8d4173d8c9073b3c2c` proves the previous helper capped plaintext input at 2 MiB, raw ciphertext input at 2 MiB, encoded ciphertext input strings at 4 MiB, and decoded Base64 ciphertext at 2 MiB. It did **not** cap encryption result bytes or Base64/Hex output strings. Thus a 2 MiB AES/CBC/PKCS5Padding plaintext may produce a 2 MiB+16 ciphertext and a larger encoded string. The previous checklist's 2 MiB encoded-output requirement was incorrect and would break existing behavior.

Next change must remove symmetric encryption-result/Base64/Hex output caps, preserve the proven raw/encoded input limits, and permit up to 4 MiB encoded output for newly exposed asymmetric Base64/Hex helpers while retaining 2 MiB raw/key/signature limits. Replace the output-bound tests with expansion-allowed and input-bound regression tests. This is a checklist correction before code changes; current CI remains pending/failing on earlier heads.


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


## 2026-11-20 pre-registered next migration gate — JsExtensions/BaseSource host contract audit

The next gate is a code-free compatibility audit before any further runtime implementation. Inspect the pinned upstream JsExtensions.kt, inherited JsEncodeUtils.kt, BaseSource.kt, their call sites and service dependencies at 330e606c4d57e08adc056ff8ab7a4132c784da9a, and compare every declaration/overload against the current Android host bridge and all callers/tests.

Required output: a declaration-by-declaration matrix covering signature/default/nullability, Rhino-visible return-object shape, exception semantics, coroutine/threading, lifecycle/context, source identity/isolation and persistence. Trace the real services required by BaseSource (Room/Gson/model, cookies/login, script storage, cache, HTTP, WebView/browser, navigation, refresh/verification/video and batch save). Specifically resolve the getSource() descriptor-map collision, batch context/cacheContent persistence, response facade ABI and sandboxed file handle. No fake BaseSource, null global service or no-op API is allowed.

Known targets/callers include RhinoSafeSourceJavaExtensions, RhinoSourceScriptRuntime (scope["java"]), source descriptor injection, host HTTP/file wrappers, batch/cache adapters, and their tests. Risks include JVM/Rhino overload ambiguity, object ABI, mutable source identity, source-state leakage, coroutine cancellation, response/body ownership, Android UI lifecycle, filesystem boundaries and chapter-index/save-token correctness.

This is audit-only: no runtime code edits. Acceptance requires every upstream declaration and overload accounted for with exact source/caller references, every host mismatch classified, and a narrowly scoped implementation task plus regression/acceptance plan pre-registered in all required checklists before code changes.

## 2026-11-20 audit result — JsExtensions/BaseSource contract and service dependency map

Audit-only work completed against upstream SHA 330e606c4d57e08adc056ff8ab7a4132c784da9a and host HEAD 32e0537048b1c82714e982c56fe6998ce6117703. No runtime code was changed.

### Declaration and ABI inventory

- JsExtensions.kt: 122 function declarations total, 118 public declarations across 77 names.
- JsEncodeUtils.kt: 30 public declarations across 27 names.
- RhinoSafeSourceJavaExtensions main bridge: 150 public declarations across 110 names. All 30 inherited JsEncodeUtils declaration slots / 27 names are present in the source declaration scan. Runtime behavior remains separately subject to compatibility tests.
- Missing public JsExtensions names: refreshBookInfo, refreshBookToc, refreshContent, webView, webViewGetSource, webViewGetOverrideUrl, openVideoPlayer, startBrowser, startBrowserAwait, getVerificationCode, openUrl, showBrowser, getReadBookConfig, getReadBookConfigMap, getThemeMode, getThemeConfig, getThemeConfigMap (17 names, 30 overload declarations).
- Same-name is not same ABI: getSource returns BaseSource? upstream but Map in host; getFile returns File upstream but RhinoSourceFileHandle in host; toURL returns JsURL upstream but RhinoLegadoUrl in host; get/head/post return Jsoup Connection.Response upstream but RhinoSourceHttpResponse in host; connect/ajaxAll/ajaxTestAll return StrResponse arrays/upstream values but host wraps all in RhinoSourceHttpResponse. ajax/ajaxAll/ajaxTestAll overload shapes, timeFormat/timeFormatUTC parameter types, log/logType return types, htmlFormat nullability, hexEncodeToString nullability and getTag nullability also differ.
- Batch is absent: host getBatchContext always returns null and cacheContent always throws. The upstream implementation uses BatchContentContext, BookChapter.index identity, duplicate-URL ambiguity rejection, content replacement, ContentSaveToken/version fences and fallback for chapters not saved by getContentBatch.
- Source object is a limited login helper, not BaseSource; sourceApi is not bound. java.getSource is a descriptor map and its current test asserts that shape.

### Pinned dependency map

BaseSource is a public interface extending JsExtensions, implemented by both Room-backed BookSource and RssSource. It depends on AppConfig/AppConst/AppLog, GSON and Room entities/converters, CacheManager, CookieStore, ConcurrentRateLimiter, SharedJsScope, JsSourceEngine, LoginUiV2, and UI/navigation helpers. BaseSource methods include dynamic getHeaderMap, login UI parsing/evaluation, login(), encrypted loginInfo, login header/cookie synchronization, source variables and key/value storage, refreshExplore, refreshJSLib, putConcurrent and evalJS bindings (java, source, sourceApi, baseUrl, cookie, cache). AnalyzeRule and JsSourceEngine also implement JsExtensions with their own source/book/chapter/batch context and coroutine bindings.

The host instead uses a BookSource DTO and raw JSON catalog, with SharedPreferencesSourceScriptKeyValueStore, SharedPreferencesSourceLoginSessionStore, SharedPreferencesSourceCookieJar, FileSourceScriptCacheStore, OkHttpSourceRequestClient, SourceRequestFactory, LegadoUrlResolver and RhinoSourceScriptRuntime. This is a real bounded host service graph, but not the pinned Room/GSON entity graph or a BaseSource implementation. The current codec retains raw JSON for unknown fields; there is no RssSource runtime model.

Network calls use the host SourceRequestFactory/OkHttp transport, not pinned AnalyzeUrl. Static source headers are parsed as JSON; dynamic header-rule execution and the full AnalyzeUrl option surface (proxy/DNS/bodyJs/webJs/WebView/redirect/retry/coroutine semantics) are not equivalent. Pinned get/head/post are Jsoup response APIs with redirects disabled; connect/ajax use AnalyzeUrl/StrResponse semantics. The host response facades intentionally hide sockets/streams, but are not ABI-equivalent.

CookieStore is domain-oriented and backed by appDb plus Android WebView cookie state; the host uses source-isolated OkHttp cookie jars. CacheManager uses appDb, memory LRU and ACache; the host uses per-source key/value and file stores. These are deliberate host boundaries, but cookie/login/header semantics need explicit parity review. Host login headers are currently merged into every request for a sourceId, whereas pinned AnalyzeUrl scopes them to the source site; this is a credential-leak risk.

BatchContentContext requires Book, BookChapter, BookSource, BookHelp, ContentSaveToken, replacement rules and chapter persistence. JsSourceEngine installs the batch context for the duration of getContentBatch and clears it in finally; JsSourceBook snapshots per-chapter save tokens and retries unsaved chapters. None of that is connected to the current SourceChapterContentService.

File access is a deliberate security restriction: pinned getFile exposes java.io.File relative to the reading cache; host exposes a canonical-root-confined RhinoSourceFileHandle because Rhino blocks java.io.File. Preserve the sandbox and add compatible facade methods only when safely supported.

### Caller and test audit

A targeted scan covered all 24 app runtime Kotlin files and all 20 app unit-test Kotlin files. Current descriptor-map use is exercised by RhinoSourceScriptRuntimeTest; getBatchContext/cacheContent outside-batch behavior by RhinoSafeSourceJavaExtensionsCompatTest; source login helpers by RhinoSourceScriptRuntimeTest and SourceLoginSessionTest. No other static Kotlin production caller of these exact bridge methods was found in that scan.

### Next bounded implementation slice (pre-registered; not started)

Implement source login/session parity and credential boundaries only: same-site-only login header propagation; Cookie synchronization on putLoginHeader and cookie removal on removeLoginHeader; pinned AES-compatible loginInfo storage with tested migration of existing plaintext records; preserve per-source isolation and validate malformed input. Tests must cover same-site, subdomain, third-party CDN/cross-origin, cookie merge/removal, encryption/migration and two-source isolation. This does not implement the full BaseSource object, UI/WebView, batch, Room model or change java.getSource ABI. Those remain open.

### Authentication lifecycle clarification

RhinoSourceScriptRuntime.clearSourceState removes source scopes, script key/value data, file cache and login-session state, but it does not clear SharedPreferencesSourceCookieJar. The jar has a clear operation, but no cookie-clear callback is wired into this lifecycle. Source removal/reset therefore needs an explicit per-source cookie cleanup path in the next auth/session slice, with tests proving another source's cookie state is preserved.


## 2026-11-20 pre-change registration — source login/session credential boundary

Implementation scope (registered before runtime edits): use pinned Legado revision `330e606c4d57e08adc056ff8ab7a4132c784da9a` and inspect its BaseSource/AnalyzeUrl login-header, CookieStore and AES loginInfo behavior. Modify only the host login/session path: `RhinoSourceLoginExtensions`, `SourceLoginSessionRequestClient`, `SharedPreferencesSourceLoginSessionStore`, `SharedPreferencesSourceCookieJar`, `RhinoSourceScriptRuntime` lifecycle wiring and `BadcatReadingEngineFactory`, plus focused tests.

Required behavior: (1) attach saved login headers only to same-site targets according to pinned upstream domain semantics, never unrelated hosts/CDNs; (2) synchronize Cookie from putLoginHeader into the source-scoped cookie jar, remove associated cookie state on removeLoginHeader, and clear the source jar during clearSourceState; (3) encrypt loginInfo with the upstream androidId-derived AES-compatible scheme and provide a tested, non-destructive migration for existing plaintext values; (4) preserve source isolation, cancellation, validation, redaction and existing JS call contracts.

Before implementation, inventory every host caller, adapter, overload and test; document risks for public-suffix/domain matching, cookie precedence/expiry, Android ID key availability, migration rollback/corrupt ciphertext and lifecycle ownership. Tests must cover same host, subdomain, unrelated host, CDN, redirects, cookie set/merge/remove, two-source isolation, reset cleanup, plaintext migration, encrypted round-trip, malformed ciphertext and missing/invalid key. Acceptance requires focused tests plus full CI; no full BaseSource parity claim. Do not change java.getSource descriptor ABI, implement batch APIs or add UI/WebView methods in this slice.

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

## 2026-11-20 Stage 1 continuation — pinned model gap audit (PRE-REGISTERED; AUDIT FIRST)

### Baseline
- Branch: `integration/legado-upstream-runtime`; head verified from `refs/heads` as `eabbef43481e6c14b8bda6ea62f03c5213d1188d`.
- Pinned upstream: `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- `main` must remain untouched. Earlier `5466caa`/`2b2bf91` registration text is historical and must not be treated as current head.

### Registered scope
- Audit exact upstream `BookSource.kt`, `Book.kt`, `BookChapter.kt`, `BaseSource` and rule model declarations at the pinned SHA.
- Audit current host `ReadingEngine.kt`, `LegadoBookSourceCodec.kt`, `FileBookSourceCatalog.kt`, all model constructors, adapters, source identity/login/session paths, persistence and tests.
- Build a field-level matrix for type/default/nullability, JSON converter behavior, identity/equality, transient/ignored members, and dependencies on Room, Parcelable, app DB/context, CookieStore, cache, reader configuration and `JsExtensions`.
- Decide the next narrow implementation boundary only after this evidence; retain existing raw JSON preservation and host source compatibility.

### Risks and tests
- Do not copy Android upstream entities into `integration-contract` or create look-alike `BaseSource`/`Book`/`BookChapter` types and call them official runtime models.
- Preserve `bookSourceUrl` source identity and source-scoped login/Cookie behavior; chapter cache identity is book + chapter index, not URL.
- Inspect all host constructor/caller sites and current codec tests before any signature change. The audit-only slice must not alter runtime behavior; validate source paths, method/field inventory, and consistency of the audit with pinned source.

### Acceptance and exclusions
- Audit report must identify every field and every runtime service dependency for the three entities and `BaseSource`, identify host mappings/callers, and specify a dependency-safe adapter plan.
- This registration authorizes documentation/audit changes only. No runtime source changes, no UI changes, no main-branch writes, no fake or no-op APIs, no APK/release claims. Any code implementation requires a new pre-registration first.

## 2026-11-20 Stage 1 pinned-model audit result

- Audit result is recorded in `docs/upstream-model-serialization-audit.md`: exact pinned `BookSource`, `Book`, `BookChapter`, `BaseBook` and `BaseSource` contracts were inspected against the host `ReadingEngine`, `LegadoBookSourceCodec`, `FileBookSourceCatalog` and codec tests.
- Confirmed current source descriptor maps most BookSource scalar fields and preserves raw JSON, but does not provide upstream typed mutable rule instances or the BaseSource runtime lifecycle. Host Book/BookChapter are UI projections; BookChapter drops volume/book/base URL, VIP/pay/resource, position/fragments, variable and image metadata.
- Important identity correction: upstream BookChapter Room primary key is `(url, bookUrl)`, a unique index enforces `(bookUrl, index)`, while equals/hashCode compare URL only. Future cache/adapter work must preserve these distinct semantics.
- BaseSource depends on actual JsExtensions, Rhino bindings/shared scope, CookieStore, CacheManager, Android ID/AES login state, concurrent-rate limiting, JS library and explore-cache lifecycle. No fake adapter is acceptable.
- Audit-only task completed; no runtime source changed. Branch baseline at audit start: `eabbef43481e6c14b8bda6ea62f03c5213d1188d`; audit result commit: `e507eff16ae6c9e7de0241aa730e3f1739461317`.
- CI: code baseline run [#44636057145](https://github.com/xkszds/badcat-legado-client/actions/runs/44636057145) passed for `eabbef4`. Docs update run [#44637466823](https://github.com/xkszds/badcat-legado-client/actions/runs/44637466823) was pending when checked; intermediate docs runs were cancelled after newer commits. Do not report current docs head as CI-verified until its run finishes.
- Next gate: design and pre-register a real Android-side model/service adapter plus full caller inventory. Official Book/BookChapter persistence and round-trip fixtures, AnalyzeRule/AnalyzeUrl, full JsExtensions, unified reading, batch cache and Badcat categories 1/2/4/5 remain open.

## 2026-11-20 Android persistence/runtime boundary review

- Rechecked the current app tree and Gradle graph at baseline `0bce0f5862c516e4d35c89103010f6995c29594e`. The app currently has no Room dependency, database, DAO, official `Book`/`BookChapter` entity or pinned `JsExtensions` declaration. `FileBookSourceCatalog` uses bounded `AtomicFile` JSON storage for sources only; it is not an upstream Room database.
- The app's `legado-rhino` module is Rhino execution/sandbox infrastructure, not the pinned `io.legado.app.help.JsExtensions` API. The factory already supplies host HTTP, CookieJar, login/session, key-value and file-cache services, but does not supply the full upstream `CacheManager`, `CookieStore`, `SharedJsScope`, `RuleBigDataHelp`, `ReadBook`, `BookHelp`, `ContentProcessor`, `AppConfig` and app DB contracts required by `BaseSource`, `Book` and `BookChapter`.
- The pinned `BookSource` cannot be integrated alone because it implements `BaseSource : JsExtensions`. A look-alike BaseSource, a partial official Book/BookChapter class, or a fake Room wrapper is prohibited. Keep the Android-free `integration-contract` projections clearly separate.
- Detailed evidence and the dependency-safe decision are recorded in `docs/upstream-model-serialization-audit.md` (follow-up section dated 2026-11-20). No runtime code changed in this review.
- Next code slice is not yet authorized: first specify the complete upstream runtime module/service closure and migration path, including license/source manifest, source service adapters, Book/Chapter DB schema and migrations, caller inventory and pinned fixtures. Register that exact bounded slice in the four mandatory ledgers before code. Preserve current source JSON round-trip and existing UI.
- Current CI at the time of review: [#44637638029](https://github.com/xkszds/badcat-legado-client/actions/runs/44637638029) for head `0bce0f5` was pending; earlier documentation runs were cancelled by subsequent doc commits. Do not claim current docs head CI-verified.

## 2026-11-20 pinned runtime/database closure audit

- Inspected the exact pinned `JsExtensions`, `BaseSource`, `AppDatabase`, `DatabaseMigrations`, `CookieStore`, `CacheManager`, `SharedJsScope`, `BatchContentContext`, `BookHelp`, `ContentProcessor`, `AnalyzeRule` and `AnalyzeUrl` sources at `330e606c4d57e08adc056ff8ab7a4132c784da9a`.
- `JsExtensions` is a large Android/UI-facing API (42,899 source characters), not merely the already-present Rhino bridge. `BaseSource` requires real login/session, encrypted loginInfo, persistent variables, CookieStore, CacheManager, SharedJsScope, source helpers and lifecycle. `BookHelp`/`ContentProcessor`/`BatchContentContext` require DB-backed content persistence and per-chapter save tokens.
- Upstream `AppDatabase` is Room schema v113 with 26 entities, a view and a long explicit/automatic migration history (including custom migrations); it is not safe to copy as a tiny Book/BookChapter table. Current Badcat has no Room database; source JSON is persisted in AtomicFile.
- Detailed source/dependency facts are recorded in `docs/upstream-model-serialization-audit.md`. No runtime code changed in this audit.
- Before runtime code, select and pre-register either full upstream database closure (A) or a Badcat-owned Room schema with exact upstream model/serialization semantics and an AtomicFile migration (B). The latter is the recommended design direction, not yet approved or implemented. The registration must specify field/default/converter mapping, real login/cookie/cache/shared-scope/chapter-save adapters, all callers, migration/rollback tests and no-stub acceptance.
- Stage 1 remains incomplete. No fake BaseSource, partial Book/BookChapter entity, no-op service or claim of official AppDatabase compatibility is permitted.
