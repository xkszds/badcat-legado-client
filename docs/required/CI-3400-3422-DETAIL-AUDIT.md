# 第三类 CI 补充审计：#3400–#3422（实际存在的 14 次运行）

- Repository: `xkszds/badcat-legado-client`
- Branch: `integration/legado-upstream-runtime`
- Pinned Legado SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: actual runs #3400–#3422 discovered on the branch. Missing run numbers within this interval are not fabricated.
- These runs document the historical third-category CI range #0001–#0199 and later audit/reconciliation work. Their commits touch only `docs/**`; they are included as third-category CI history because the Android workflow itself runs the third-category profile.

## 1. Per-run identity

All 14 runs used `.github/workflows/android-build.yml`, workflow “Android debug build”, on `integration/legado-upstream-runtime`, with one `assemble-debug` job. The job IDs below were fetched and inspected.

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Conclusion | Returned step states |
|---:|---:|---:|---|---|---|---|
| #3400 | 45303016026 | 138139943321 | 2026-11-24 01:10:30 | `685c0c339f5f43cab4e6e10cfc3f1d4f3f1ed83f` | success | 53/53 success |
| #3401 | 45303021727 | 138139962803 | 2026-11-24 01:10:32 | `7163dc26ee52886d5072beed4f5f464ed1e5b16b` | success | 53/53 success |
| #3404 | 45303217719 | 138140616787 | 2026-11-24 01:12:03 | `57aa0ed20cf1301856d4f3391d6e2f0622cf8d90` | success | 53/53 success |
| #3408 | 45303284284 | 138140845400 | 2026-11-24 01:12:35 | `30571d03b9f159af391070d0f931dbfebc90d8d9` | success | 53/53 success |
| #3409 | 45303319427 | 138140965474 | 2026-11-24 01:12:51 | `ad8ae20344f754a8a566f7900ea31d7791cbae93` | failure | 48 success / 1 failure / 4 skipped |
| #3410 | 45303402117 | 138141242395 | 2026-11-24 01:13:29 | `afecc5edbde6fc27fb8f67fa56947cc4c315e8bd` | failure | 47 success / 2 failure / 4 skipped |
| #3411 | 45303488466 | 138141524292 | 2026-11-24 01:14:08 | `122ccf31db8f99e1f22b34b04899a71e2c41e6f2` | success | 53/53 success |
| #3412 | 45303544416 | 138141720118 | 2026-11-24 01:14:36 | `22b33bcd893bbb66475f5670b75365912bf30dae` | success | 53/53 success |
| #3413 | 45303577503 | 138141831722 | 2026-11-24 01:14:51 | `6f2d8cc9d90e62321fa78c522402d58f1539ca0c` | success | 53/53 success |
| #3414 | 45303631471 | 138141997971 | 2026-11-24 01:15:13 | `48cd786568dc2a68fc25c3312e2d9c717748b480` | failure | 48 success / 1 failure / 4 skipped |
| #3417 | 45303707445 | 138142234559 | 2026-11-24 01:15:44 | `d97450cffe88d477e01d6dc2f9c7e6f51e8f191b` | success | 53/53 success |
| #3418 | 45303742446 | 138142356372 | 2026-11-24 01:16:01 | `8d436bba1a9d6d8863213807d50a3f205d716446` | success | 53/53 success |
| #3419 | 45303809785 | 138142585798 | 2026-11-24 01:16:33 | `d576e3fd4a87efbd1aa507b4d1d6eb77e0b2020b` | success | 53/53 success |
| #3422 | 45303968505 | 138143108663 | 2026-11-24 01:17:44 | `f6623813b28fc3b45af43fea8065c4b345e86cea` | success | 53/53 success |

Run URL pattern: `https://github.com/xkszds/badcat-legado-client/actions/runs/<Run-ID>`.

## 2. Actual coverage

For the 11 successful runs, all 53 returned steps completed successfully. Logs show the same configured coverage profile:

- Pinned upstream Legado source verification; JDK 21, Android SDK and Gradle setup.
- Integration contract tests and authentic pinned Legado runtime model-contract tests.
- JavaScript API inventory and dependency closure generation.
- Rhino artifact/source restore and verification; RuleAnalyzer Android runtime-classpath verification.
- G2d stage-1 authentic source manifest and rule/model compiler probe.
- Third-category app JVM unit suite, excluding Fourth-category tests.
- API28 CookieManager instrumentation and API35 WebView-provider instrumentation; RuleAnalyzer/provider reports.
- Host JavaScript bridge JVM ABI.
- Debug APK assembly and RuleAnalyzer DEX class-definition verification.
- Best-effort external source probes.

The logs for successful source probes show Bixiange and Pinshuzhai Python scripts passing search → detail → TOC → chapter content. Nyasama search is bounded (commonly 3/5 usable results); the public endpoint sweep attempts 14 endpoints and typically returns 8–9 non-empty HTTP 200 responses, with 403/404/301/444/timeouts and malformed URL errors among failed candidates.

These source scripts are external Python/HTTP/HTML smoke tests, not Badcat Android or embedded Legado Runtime execution. They do not prove imported BookSource execution through Rhino/JsExtensions/BaseSource, login/cookie/session semantics, pagination parity or full Legado compatibility.

## 3. Failure analysis

### #3409 — two app unit tests failed fetching a Robolectric Android-all artifact

- Run: [45303319427](https://github.com/xkszds/badcat-legado-client/actions/runs/45303319427)
- Job: `138140965474`
- Commit scope: documentation-only update to `docs/required/CI-0001-0199-AUDIT.md`.
- JUnit archive: artifact `14879439812`; exact result: **332 tests, 2 failures, 0 errors, 0 skipped**.
- Both failures are in `SharedPreferencesSourceScriptKeyValueStoreTest`: `removePrefixLeavesMalformedAndUnrelatedKeysWithinSourceNamespaceUntouched[28]` and `removePrefixDeletesOnlyCacheKeysForTheSelectedSource[28]`.
- Both report `Failed to fetch maven artifact org.robolectric:android-all-instrumented:9-robolectric-4913185-2-i7`, with IOException under MavenArtifactFetcher.
- Retry diagnostics artifact: `14878899652`; its summary says the retry condition was the exact `nativeruntime-dist-compat-1.0.16.jar` plus HTTP 429, but the observed failure was a different `android-all-instrumented` artifact and was classified `non_retryable_gradle_failure` after one attempt. No HTTP status code is present in this diagnostic, so this must **not** be mislabeled as a confirmed HTTP 429.
- Key other artifacts: Debug APK `14879708572`, DEX report `14879653754`.
- Source smoke steps were skipped after the failed app test step.

### #3410 — API 35 WebView emulator instrumentation failed; 332 unit tests passed

- Run: [45303402117](https://github.com/xkszds/badcat-legado-client/actions/runs/45303402117)
- Job: `138141242395`
- Commit scope: documentation-only update to `docs/required/CI-0001-0199-AUDIT.md`.
- Log reports `JUNIT_TESTS=332 JUNIT_FAILURES=0 JUNIT_ERRORS=0 JUNIT_SKIPPED=0`: all 332 app unit tests passed.
- Failure steps: “Run real Android WebView provider instrumentation (API 35)” and its provider report upload.
- Logs show ADB process exit code 1 and `[EmulatorConsole]: Failed to start Emulator console for 5554`; this is an emulator/ADB startup failure, not an app unit-test failure.
- Job result: 47 success / 2 failure / 4 skipped. The four live-source steps were skipped.
- Key artifacts: JUnit `14879664127`, Debug APK `14880207705`, DEX `14880042903`. No live-source reports.

### #3414 — 65 app unit tests failed on Robolectric Android-all artifact fetch

- Run: [45303631471](https://github.com/xkszds/badcat-legado-client/actions/runs/45303631471)
- Job: `138141997971`
- Commit scope: documentation-only update to `docs/required/CI-0001-0199-AUDIT.md`.
- JUnit archive: artifact `14880725826`; exact result: **332 tests, 65 failures, 0 errors, 0 skipped**.
- All 65 failures report `Failed to fetch maven artifact org.robolectric:android-all-instrumented:15-robolectric-12650502-i7`, with IOException under MavenArtifactFetcher. The affected tests span reading-state repository, source catalog lifecycle, CookieManager, chapter cache, Rhino crypto/overload, persistent/session cookies, and login session suites.
- Retry diagnostics artifact: `14880081900`; configured retry condition targets `nativeruntime-dist-compat-1.0.16.jar` plus HTTP 429, but this run failed to fetch the different `android-all-instrumented` artifact and was classified `non_retryable_gradle_failure` after one attempt. No HTTP status code is present; do not assert HTTP 429.
- Key other artifacts: Debug APK `14880820586`, DEX report `14880611182`.
- Source smoke steps were skipped after the app test failure.

## 4. Artifact IDs per run

Columns: JUnit report, Debug APK, RuleAnalyzer DEX report, Bixiange smoke report, Pinshuzhai smoke report. Blank means no artifact was present.

| CI | JUnit | Debug APK | DEX | Bixiange | Pinshuzhai |
|---:|---:|---:|---:|---:|---:|
| #3400 | 14880685360 | 14878914926 | 14880810023 | 14879374775 | 14879863223 |
| #3401 | 14879159061 | 14880461777 | 14879084336 | 14880382427 | 14880292514 |
| #3404 | 14880326679 | 14880087004 | 14880251830 | 14880452458 | 14879069839 |
| #3408 | 14880407376 | 14879209316 | 14880352481 | 14880143177 | 14880497557 |
| #3409 | 14879439812 | 14879708572 | 14879653754 | — | — |
| #3410 | 14879664127 | 14880207705 | 14880042903 | — | — |
| #3411 | 14879903371 | 14879898380 | 14879489965 | 14879833799 | 14881125136 |
| #3412 | 14880751206 | 14880641429 | 14880751272 | 14880781346 | 14880387227 |
| #3413 | 14880476913 | 14880037542 | 14880920562 | 14879774522 | 14879943864 |
| #3414 | 14880725826 | 14880820586 | 14880611182 | — | — |
| #3417 | 14880960679 | 14879948614 | 14879849022 | 14881025958 | 14879839513 |
| #3418 | 14881150087 | 14879958520 | 14879918599 | 14880991036 | 14879689765 |
| #3419 | 14879229518 | 14879174721 | 14880298091 | 14880936267 | 14880996214 |
| #3422 | 14881030583 | 14881125279 | 14879709316 | 14880577082 | 14880811424 |

All other instrumentation, WebView provider, emulator diagnostic, Rhino host, JVM ABI, JS inventory, dependency closure and G2d probe artifacts remain attached to their run URLs.

## 5. Reconciliation and acceptance

- Actual runs in this interval: 14.
- Workflow results: 11 success / 3 failure / 0 pending.
- Successful runs: 11 jobs with 53/53 steps success.
- #3409: 332 tests, 2 dependency-fetch failures.
- #3410: 332/332 unit tests pass; API35 emulator/ADB instrumentation failure.
- #3414: 332 tests, 65 dependency-fetch failures.
- All 14 commit diffs are docs-only; no application implementation changed.
- Debug APKs and DEX reports are CI artifacts, not release acceptance.
- Bixiange/Pinshuzhai smoke is external Python, not Android Runtime E2E.
- Stage-0 remains in progress / not accepted; Stage-1 remains NO-GO.

The missing #3400–#3422 actual-run gap is now documented with exact run/job/SHA, functional outcomes, artifact references and failure boundaries. Missing numbers in the interval remain absent; no fabricated CI entries are added.
