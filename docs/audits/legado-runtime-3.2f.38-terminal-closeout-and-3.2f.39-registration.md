# Runtime-3.2f.38 terminal CI closeout and Runtime-3.2f.39 registration

Date: 2026-11-23
Branch: integration/legado-upstream-runtime
Pinned Legado behavior baseline: 330e606c4d57e08adc056ff8ab7a4132c784da9a

## 1. Runtime-3.2f.38 — verified terminal result

### Exact code and CI

- Implementation commit: 80b0b93e2d25d133f27cfeac905b484d5a7e74ae
- Actions run: [45102448235](https://github.com/xkszds/badcat-legado-client/actions/runs/45102448235)
- Job: 137456145163, assemble-debug, completed / success.
- All 47 reported job steps completed with success; there were no non-success steps.
- The branch head immediately before this closeout was 3bdede81d5a99bfec7942cd23382b45e333aa83d, a documentation-only descendant. No production or test code changed after the exact implementation SHA.

### JUnit

- Artifact: badcat-unit-test-reports, ID 14786591736.
- ZIP SHA-256: 58b8e61e77ce863259bb4b86365a6ecc1eb829534dc8d1933243966577577917.
- Downloaded archive contained 101 XML files. Parsed totals: 41 suites, 335 tests, 0 failures, 0 errors, 0 skipped.
- Focused LegadoSourceLoginUiV2ServiceTest: 12/12 pass.

### Android instrumentation

- API 28 artifact 14785554364, ZIP SHA-256 ab80aa95a57e6abfbd023bc8eb840e83723f528affd8a083cea5e506f24104cf. String, number, null and undefined callback cases all reported ok=true. Emulator WebView provider: Chrome 69.0.3497.100.
- API 35 artifact 14786074093, ZIP SHA-256 a986b7a10a34f87fc47a1c7fea196d1cb14b530d3313c3342658c14c4e4e791e. The same four callback cases all reported ok=true. Emulator WebView provider: Android System WebView 124.0.6367.219.

### Debug APK artifact

- Artifact badcat-legado-debug, ID 14786128724.
- Archive SHA-256: bf0a307e332951579ecebe73c3bcc8c2fa29b7a3ecd612cff26c579375439434.
- Extracted app-debug.apk: 32,024,734 bytes.
- APK SHA-256: 1753f6c62d8dae6ccc5f659dfa81344307379ae177976d16aaeee9a8bf15399b.
- This is a CI debug artifact only. No release signing, user-device installation, or vivo-device test is claimed.

### Live-source evidence and upstream failures

- Bixiange artifact 14786780810, ZIP SHA-256 dcca66cf2af74ce23f4b3df152ad34b1ce564f434d89de66fc84ade35f33d9ac: search, detail, 114-page TOC and chapter content passed end-to-end; chapter body 2,884 characters.
- Pinshuzhai artifact 14786396891, ZIP SHA-256 767db05a2b1ac9b51dd6f85c260c2727a3d0fb4d7836a4a625a4fa5b2b112db1: search, detail, 1,650-chapter TOC and chapter content passed end-to-end; chapter body 3,116 characters.
- Uploaded corpus artifact 14785689997, ZIP SHA-256 21783104c5b2b14715977850acf72940dac1d58e12dcc7420ba72663afb81f02: 1/4 candidates completed search → detail → TOC → content. 精华书阁 passed; 唐三中文 and 零零小说 timed out after retries; 笔趣阁fun returned upstream HTTP 404. These failures are attributed to the external source/network results and are not counted as engine passes.

### Scope decision

Runtime-3.2f.38 is closed only for the preregistered Login UI v2 behavior: JS-source mainJs selection, once-per-source mainJs initialization, render/action return normalization, and the listed regression tests. It does not close Login UI v1, WebView/captcha login, dynamic request headers, full Legado runtime compatibility, real-device installation, release packaging, or final APK acceptance.

The production/test diff remained limited to:
- app/src/main/java/app/badcat/client/runtime/LegadoSourceLoginUiV2Service.kt
- app/src/test/kotlin/app/badcat/client/runtime/LegadoSourceLoginUiV2ServiceTest.kt

No generic Rhino runtime, request transport, Cookie, UI layout, original APK or user data was changed.

### Repository and original APK notes

- GitHub Pull Requests are disabled for this repository; the API returned HTTP 410, “Pull Requests disabled”. The previously mentioned PR #1 is not a usable review entry. Use the branch and exact Actions run as the evidence.
- The original APK binary was not mounted in this automation runtime. Library search surfaced historical static-analysis reports but not the APK bytes. No fresh APK scan was performed; no original APK or data was modified.

## 2. Runtime-3.2f.39 — registered read-only audit

### Scope

Audit dynamic source headers and login-header ordering before implementing any provider. This is a read-only audit, not a production implementation.

### Trigger and concrete risk

The Runtime-3.2f.37 audit found that the host request factory only handles static JSON headers, does not add the pinned default User-Agent, and does not provide consistent upstream dynamic-header behavior across all request paths.

A deeper lifecycle review found a critical re-entrancy risk: RhinoSourceScriptRuntime.evaluate holds a per-source Mutex while JavaScript java.ajax/get/head/post/connect callbacks execute inside that same source scope. A naive provider that calls SourceScriptRuntime.evaluate recursively for the same source may wait on the already-held Mutex. This must be resolved before implementation; creating a second runtime and re-running mainJs is not an acceptable shortcut.

### Pinned upstream evidence

Inspect at LegadoTeam/legado revision 330e606c4d57e08adc056ff8ab7a4132c784da9a:

- BaseSource.extractInlineJs, getHeaderMap, getLoginHeaderMap, putLoginHeader, removeLoginHeader.
- AppPattern.JS_PATTERN.
- AppConfig.userAgent and its preference/default source.
- AnalyzeUrl.headerMapF and same-site login-header eligibility.
- NativeBaseSource and CookieStore.replaceCookie/removeCookie.

### Host caller inventory to re-verify

- integration-contract/.../SourceRequestFactory.kt — pure request construction and static header parsing.
- integration-contract/.../SourceHttpGateway.kt — ordinary source rule request path.
- app/.../BadcatSourceHttpGatewayFactory.kt — gateway and script-backed requests.
- app/.../BadcatReadingEngineFactory.kt — direct callbacks sourceAjax, sourceGet, sourceHttpRequest and sourceHttpRequestWithTimeout.
- app/.../RhinoSourceScriptRuntime.kt — source-scoped evaluation and Mutex lifecycle.
- app/.../RhinoSafeSourceJavaExtensions.kt — ajax/get/head/post/connect callback paths.
- app/.../SourceLoginSession.kt — login-header storage and request wrapper.
- All affected existing tests and every direct SourceRequestFactory.create call.

### Required findings

1. Complete upstream and host caller graph; identify every direct request-construction path.
2. Re-entrant call graph, Mutex/context behavior, and whether the current Rhino context can safely evaluate the header script without a second runtime.
3. Exact upstream full-match inline JS syntax and result normalization.
4. Ordinary header JS/JSON errors are logged and swallowed; coroutine cancellation is propagated.
5. Default UA source: determine how configurable AppConfig.userAgent differs from the host WebView UA.
6. Exact precedence: configured/dynamic source headers → default UA if absent → same-site login headers → per-request URL-option headers.
7. Login Cookie synchronization/removal, same-site behavior, redirect/retry lifecycle and secret redaction.
8. A typed provider API design and exact target-file/regression-test matrix. If same-scope execution cannot be proved safe, the decision must remain NO-GO.

### Allowed files and exclusions

Allowed: this audit report and the category-3 checklist only.

No production/test/Gradle/workflow/UI/storage changes; do not execute user-imported source scripts; do not change the pinned upstream SHA, main, original APK or user data. Any later provider implementation requires its own preregistration.

### Acceptance

A complete upstream/host caller map, re-entrancy sequence, lock/cancellation/scope analysis, header/login/cookie precedence table, default-UA mapping decision, typed provider proposal, exact implementation/test matrix, and explicit GO/NO-GO decision.
