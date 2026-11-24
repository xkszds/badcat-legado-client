# 第三类 CI 补充审计：#3358–#3397（2026-11-24）

- Repository: `xkszds/badcat-legado-client`
- Branch: `integration/legado-upstream-runtime`
- 200-run volume: `docs/required/CI-3200-3399-AUDIT.md`
- Scope: all real workflow runs #3358–#3397 inclusive (40 runs).
- Pinned upstream Legado SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Audit method: exact run/HEAD SHA/Job/steps/logs/commit diff/artifact metadata; no product source edits or reruns performed by this audit.

## 1. Per-run identity and result

All 40 runs are pushes on `integration/legado-upstream-runtime` through `.github/workflows/android-build.yml`, workflow “Android debug build”, one `assemble-debug` job per run. All corresponding commits touch only `docs/**`; none modifies application source, test source, Gradle configuration or UI.

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Conclusion | Step result |
|---:|---:|---:|---|---|---|---|
| #3358 | 45287802126 | 138088547817 | 2026-11-23 23:19:31 | `e7cd09f8d9e56807297cd7722dc7f700af4accbf` | success | 53/53 success |
| #3359 | 45287992056 | 138089185740 | 2026-11-23 23:20:48 | `15dd43098add567a3f263f5ba0086eb861cd07ce` | success | 53/53 success |
| #3360 | 45287998180 | 138089208142 | 2026-11-23 23:20:51 | `dbd10659f74266f6861062351e4601aea25a029f` | success | 53/53 success |
| #3361 | 45288231230 | 138089999268 | 2026-11-23 23:22:24 | `b8b69cc13db5688f75280d333b46b6eb90e56640` | failure | 22 success / 9 failure / 22 skipped |
| #3362 | 45288449273 | 138090741922 | 2026-11-23 23:23:53 | `e50fecb1c79f8a7412c1cb941912736f45ad6a11` | success | 53/53 success |
| #3363 | 45288624795 | 138091339098 | 2026-11-23 23:25:05 | `4ad97db6533d8acaf9accb2ff7f8f7fa1466a91a` | success | 53/53 success |
| #3364 | 45290000803 | 138096012467 | 2026-11-23 23:34:35 | `2b24040c42b1b30af2daf022974e303be0abf9de` | success | 53/53 success |
| #3365 | 45290051635 | 138096185424 | 2026-11-23 23:34:56 | `fb5b2dc2b6345a24edf8342693ff333e1385e797` | success | 53/53 success |
| #3366 | 45291186622 | 138100063783 | 2026-11-23 23:42:58 | `97b5eb09d2c619fabf43a6b78871554fa2178d9e` | success | 53/53 success |
| #3367 | 45291270407 | 138100347137 | 2026-11-23 23:43:34 | `3d81abbf86a8114325f7401968c0edf6ef7beee7` | success | 53/53 success |
| #3368 | 45292008251 | 138102856389 | 2026-11-23 23:48:53 | `1193b6f21c27a0f8f811605b32d8adbc4718ee23` | success | 53/53 success |
| #3369 | 45292056811 | 138103017723 | 2026-11-23 23:49:14 | `810ccdb3da9b7940f789f9daa361b878101a6cd9` | success | 53/53 success |
| #3370 | 45292479683 | 138104465624 | 2026-11-23 23:52:22 | `e77ac0d67966e13e2b9326c93c4c09d8c29e4f63` | success | 53/53 success |
| #3371 | 45292894762 | 138105880649 | 2026-11-23 23:55:27 | `365506a78e08064bd6ad4041e486b09873a2aee1` | success | 53/53 success |
| #3372 | 45292962520 | 138106113781 | 2026-11-23 23:55:57 | `8f698bc1c8eff146ad5f482f91a9fb10443dbb61` | success | 53/53 success |
| #3373 | 45294362598 | 138110874069 | 2026-11-24 00:06:10 | `08a0751cf9694adf6159fef5c3b863288cfa828c` | success | 53/53 success |
| #3374 | 45294619847 | 138111747826 | 2026-11-24 00:07:59 | `a1d1a6a4b3ebc3cf1505df5097696e8fad7eb94e` | success | 53/53 success |
| #3375 | 45294747788 | 138112187645 | 2026-11-24 00:08:54 | `b644b87e5176e00038444ca7df4402f6d25f58f8` | success | 53/53 success |
| #3376 | 45294930176 | 138112798406 | 2026-11-24 00:10:12 | `68cdb6e30942789ece226c6da18f8ed0ba2700d5` | success | 53/53 success |
| #3377 | 45295079466 | 138113288812 | 2026-11-24 00:11:13 | `5dcf515c095bf088825f4347d9845792d7917bbe` | success | 53/53 success |
| #3378 | 45296835198 | 138119267765 | 2026-11-24 00:24:01 | `d539ba4081adc5b91b2cda6d2c6469e3d8dd5629` | success | 53/53 success |
| #3379 | 45300190972 | 138130510573 | 2026-11-24 00:48:54 | `2c010fcf658c2a5c3c360d0fe0dda14e5675f742` | success | 53/53 success |
| #3380 | 45300234542 | 138130652929 | 2026-11-24 00:49:13 | `87c68fc3026fc1f7ffeab5426c20e2387b932809` | success | 53/53 success |
| #3381 | 45300949220 | 138133020977 | 2026-11-24 00:54:38 | `9274988b93df75a1a53bc500aadd0ddffd36f06f` | success | 53/53 success |
| #3382 | 45300953597 | 138133033640 | 2026-11-24 00:54:40 | `3dba848b13fae7ab37456d034268185b893bdc1c` | success | 53/53 success |
| #3383 | 45300971772 | 138133097598 | 2026-11-24 00:54:49 | `bc01e1170d928989117cac3ec5c165eaf451ca29` | success | 53/53 success |
| #3384 | 45300977345 | 138133114138 | 2026-11-24 00:54:51 | `31f9bab19233e509a71d22e294a310bcec4f1a11` | success | 53/53 success |
| #3385 | 45300996067 | 138133182441 | 2026-11-24 00:55:01 | `ba783df49a3b7b529094515643d874fad8d263ed` | success | 53/53 success |
| #3386 | 45301018142 | 138133247576 | 2026-11-24 00:55:09 | `850d75d2473296e3d55dc6c8cd0ec0c9b97cb766` | success | 53/53 success |
| #3387 | 45301035423 | 138133304173 | 2026-11-24 00:55:17 | `c6938694ec92478160089c63e82b3f2e1e4d2c03` | success | 53/53 success |
| #3388 | 45301040038 | 138133318291 | 2026-11-24 00:55:19 | `03a9266b09fe31e8faeb9d5e693de331ad3a38ec` | success | 53/53 success |
| #3389 | 45301053677 | 138133366150 | 2026-11-24 00:55:25 | `1473319092a24613dbd9146f31dc561d5d439b36` | failure | 48 success / 1 failure / 4 skipped |
| #3390 | 45301092582 | 138133495695 | 2026-11-24 00:55:43 | `cf069b1460b0cfba655c554426d3e3c9db2f73a3` | success | 53/53 success |
| #3391 | 45301133452 | 138133633602 | 2026-11-24 00:56:02 | `2465008c4f47c03d70571f341ca52d4053036177` | success | 53/53 success |
| #3392 | 45301137717 | 138133647289 | 2026-11-24 00:56:04 | `ab159aa9d033d1fae4d1e20b0085f292fe2e6f73` | success | 53/53 success |
| #3393 | 45301182637 | 138133796544 | 2026-11-24 00:56:25 | `4fc245c50d52b1eef1fdc0ea1c302fcb46fe9968` | success | 53/53 success |
| #3394 | 45301200505 | 138133856548 | 2026-11-24 00:56:34 | `31d4566f16cf7e1988531978ddf4c18abd903928` | success | 53/53 success |
| #3395 | 45301583165 | 138135173261 | 2026-11-24 00:59:41 | `04e308b34b1771d76aa6b4570b60b115ff0a9c71` | success | 53/53 success |
| #3396 | 45301643043 | 138135368032 | 2026-11-24 01:00:09 | `aa80cda78970c598ebe7d2cfcddc4087adff00a8` | success | 53/53 success |
| #3397 | 45301732938 | 138135653674 | 2026-11-24 01:00:46 | `f94a816d14b336368253a60b70cc7cbb2eb585cc` | success | 53/53 success |

Run URL pattern: `https://github.com/xkszds/badcat-legado-client/actions/runs/<Run-ID>`. Each Job ID above is the exact `assemble-debug` job inspected.

## 2. Functional coverage actually executed

For the 38 successful runs, the 53 returned steps all completed successfully. The logs show this configured coverage:

1. Checkout and pinned Legado source SHA verification; JDK 21, Android SDK/platform/build tools and Gradle setup.
2. Integration contract tests and authentic Legado runtime model-contract tests.
3. Generate pinned JavaScript API inventory and pinned source dependency closure.
4. Restore/verify pinned Rhino artifact and extension sources; verify RuleAnalyzer on Android runtime classpath.
5. Verify G2d stage-1 authentic source manifest and compile authentic rule/model source group; upload compiler probe evidence.
6. Run third-category app unit suite via `:app:testDebugUnitTest -PbadcatThirdCategoryTestProfile=true`, excluding Fourth-category tests.
7. Run real API28 CookieManager instrumentation and API35 WebView-provider instrumentation; upload RuleAnalyzer and WebView provider reports.
8. Capture compiled host JavaScript bridge JVM ABI.
9. Upload unit reports; assemble Debug APK; verify pinned RuleAnalyzer class definition in APK DEX; upload APK and DEX report.
10. Execute best-effort source checks: Nyasama bounded search, public repository endpoint sweep, external Python Bixiange and Pinshuzhai smoke scripts.

The JUnit report archive is attached to each successful run. This audit did not unpack each archive to recalculate the exact test count, so no unsupported numeric count is asserted.

## 3. The two failures: exact causes and impact

### #3361 — Kotlin Parcelize plugin resolution failure

- Run: [45288231230](https://github.com/xkszds/badcat-legado-client/actions/runs/45288231230)
- Job: `138089999268`
- Commit: docs-only update to `docs/required/03-LEGADO-RUNTIME.md`.
- The first contract test step failed while resolving plugin artifact `org.jetbrains.kotlin.plugin.parcelize:org.jetbrains.kotlin.plugin.parcelize.gradle.plugin:2.4.20` from plugin repositories.
- Job result: 22 success / 9 failure / 22 skipped. The contract test step failed; subsequent authentic runtime tests, API inventory, dependency closure, Rhino restore/verification, RuleAnalyzer runtime classpath, G2d probe, third-category app unit suite, API28/35 instrumentation and APK assembly were skipped or their conditional artifact uploads failed.
- Artifacts: no workflow artifacts were retained for this run.
- Classification: build/plugin dependency resolution failure; no evidence of an application-code regression and no third-category functional pass can be claimed for the skipped tests.

### #3389 — Maven Central HTTP 429 for Robolectric native runtime dependency

- Run: [45301053677](https://github.com/xkszds/badcat-legado-client/actions/runs/45301053677)
- Job: `138133366150`
- Commit: docs-only update to `docs/required/03-LEGADO-RUNTIME-OVERVIEW.md`.
- The failing task was `:app:testDebugUnitTest`. Gradle could not download `org.robolectric:nativeruntime-dist-compat:1.0.16` from Maven Central; the exact GET returned HTTP 429 Too Many Requests. The log shows the same dependency fetch failing in three attempts.
- Job result: 48 success / 1 failure / 4 skipped. The third-category app unit suite failed; Nyasama, public endpoint, Bixiange and Pinshuzhai live source steps were skipped. Contract/model tests, Rhino/classpath checks, API28/35 instrumentation, JVM ABI capture, Debug APK assembly and RuleAnalyzer DEX verification completed.
- Key artifacts retained: JUnit report `14879171591`, Debug APK `14878808852`, RuleAnalyzer DEX report `14878709265`. No live-source smoke reports were produced.
- Classification: external Maven dependency rate-limit failure, not a demonstrated product-code regression. It remains a failed CI run and must not be relabeled as success.

## 4. External source probes: what passed and what did not

For the 38 runs whose source steps executed:

- **Bixiange / 笔仙阁:** external Python HTTP/HTML script passed GBK POST search → detail → TOC (114 pages) → AJAX chapter → extracted content (~2,884 chars).
- **Pinshuzhai / 品书斋:** external Python HTTP/HTML script passed search → detail → TOC (1,650 chapters) → extracted content (~3,116 chars).
- **Nyasama:** bounded search probe returned 3/5 usable results. This is not a full book reading test.
- **Public source repository endpoint sweep:** 14 GET endpoints attempted; 7–9 non-empty HTTP 200 responses in these runs. Other attempts include observed 403, 404, 301 redirect-loop, 444 and malformed-URL errors. Endpoint reachability is not rule execution.
- Additional sampled public-source probes repeatedly failed for 404/301/malformed URL cases. These are recorded as source-probe failures, not Android Runtime test failures.

For #3361 and #3389 these source steps were skipped, not passed.

**Critical boundary:** Bixiange and Pinshuzhai checks are standalone Python scripts, not Android app execution and not Legado Runtime execution. They do not prove imported BookSource JSON runs through Rhino, JsExtensions, AnalyzeRule/AnalyzeUrl, BaseSource lifecycle, cookie/login/session, pagination, or full content-parity behavior. The CI green status must not be interpreted as complete Legado compatibility or Android source E2E.

## 5. Exact artifact IDs per run

Columns are artifact IDs for `badcat-unit-test-reports`, `badcat-legado-debug`, `legado-runtime-3.2f.46-apk-dex-class-definitions`, `badcat-live-source-smoke-report`, and `badcat-pinshuzhai-live-source-smoke-report`. Blank means the artifact was not present in the run's artifact list.

| CI | JUnit | Debug APK | RuleAnalyzer DEX | Bixiange report | Pinshuzhai report |
|---:|---:|---:|---:|---:|---:|
| #3358 | 14871939411 | 14871829556 | 14872698641 | 14872369251 | 14873920373 |
| #3359 | 14872678221 | 14872528491 | 14872693855 | 14873866226 | 14874010922 |
| #3360 | 14872578619 | 14872759039 | 14872878424 | 14873184163 | 14873224180 |
| #3361 | — | — | — | — | — |
| #3362 | 14873886249 | 14874410384 | 14873786693 | 14872718783 | 14872834386 |
| #3363 | 14873816764 | 14874305826 | 14873244419 | 14874620327 | 14873782208 |
| #3364 | 14874875516 | 14875030305 | 14874750921 | 14875130643 | 14874960705 |
| #3365 | 14874027869 | 14874477192 | 14873458146 | 14873947711 | 14873793319 |
| #3366 | 14874877236 | 14875291598 | 14875436397 | 14875825384 | 14874364854 |
| #3367 | 14873888892 | 14873773893 | 14873544416 | 14874857167 | 14874513251 |
| #3368 | 14875560805 | 14874693223 | 14874758033 | 14875062981 | 14875023026 |
| #3369 | 14876270024 | 14875222406 | 14875447149 | 14874683664 | 14875232506 |
| #3370 | 14874693927 | 14875576160 | 14874589364 | 14875476729 | 14876131301 |
| #3371 | 14876300685 | 14874659557 | 14875338546 | 14876101741 | 14875616744 |
| #3372 | 14875388068 | 14875238320 | 14874944134 | 14875024390 | 14875443090 |
| #3373 | 14874934872 | 14875124649 | 14875319433 | 14875578093 | 14876402148 |
| #3374 | 14877105418 | 14875434364 | 14875294575 | 14876267752 | 14876107886 |
| #3375 | 14875014950 | 14877060604 | 14877065568 | 14877250003 | 14876981197 |
| #3376 | 14875518604 | 14877021110 | 14877105860 | 14875728595 | 14876706834 |
| #3377 | 14876517239 | 14876901435 | 14876916457 | 14875793812 | 14875919404 |
| #3378 | 14877920611 | 14877915439 | 14876653960 | 14876809658 | 14876819555 |
| #3379 | 14878004257 | 14879615008 | 14878118942 | 14878417945 | 14877968838 |
| #3380 | 14878034121 | 14878353550 | 14878343481 | 14878459602 | 14879391348 |
| #3381 | 14879047136 | 14878449835 | 14879121924 | 14878194752 | 14878404286 |
| #3382 | 14879042259 | 14879136845 | 14879346592 | 14878894398 | 14879027782 |
| #3383 | 14878488494 | 14878398568 | 14878333750 | 14878523650 | 14878284167 |
| #3384 | 14879915225 | 14878149465 | 14878249165 | 14879796186 | 14879616998 |
| #3385 | 14878169089 | 14878654268 | 14878449596 | 14879331642 | 14879226722 |
| #3386 | 14878199909 | 14879845336 | 14879870302 | 14878733103 | 14878179286 |
| #3387 | 14879616602 | 14879760843 | 14879656262 | 14878034915 | 14878384260 |
| #3388 | 14879261556 | 14878484738 | 14879556288 | 14879791293 | 14879517407 |
| #3389 | 14879171591 | 14878808852 | 14878709265 | — | — |
| #3390 | 14878244258 | 14878942997 | 14877969733 | 14878169508 | 14877979892 |
| #3391 | 14878219223 | 14878298955 | 14879017332 | 14879151961 | 14878554704 |
| #3392 | 14879376255 | 14878569383 | 14879311391 | 14879596801 | 14879591718 |
| #3393 | 14878369082 | 14878204556 | 14878943387 | 14878729012 | 14878828823 |
| #3394 | 14879146940 | 14879985397 | 14878494824 | 14879023055 | 14879008024 |
| #3395 | 14879701807 | 14879652063 | 14879248197 | 14878574581 | 14878704333 |
| #3396 | 14880195174 | 14880350006 | 14879107392 | 14878834093 | 14880220230 |
| #3397 | 14879617557 | 14879502947 | 14879702053 | 14879352607 | 14880111017 |

All additional artifacts (API28/API35 instrumentation, WebView diagnostics/provider reports, Rhino host surface, JVM ABI, JS inventory, dependency closure, G2d probe and Maven diagnostics) remain attached to their exact run URLs.

## 6. Classification and acceptance

- Distinct runs: 40.
- Workflow conclusion: 38 success / 2 failure / 0 pending.
- Successful jobs: 38/38 have 53/53 returned steps success.
- #3361: plugin artifact resolution failure; later functional steps skipped.
- #3389: Maven Central HTTP 429 for Robolectric dependency; third-category app unit test failed; live-source steps skipped.
- Commit diffs: all documentation-only; no production code changes.
- Debug APKs: 39 uploaded; #3361 has no artifacts. These are CI debug artifacts, not release acceptance.
- External Python source scripts: 38 runs pass Bixiange and Pinshuzhai smoke; not Android Runtime E2E.
- Full imported-source Android Runtime E2E: not proven.
- Complete Legado compatibility: not proven.
- Stage-0: remains in progress / not accepted.
- Stage-1: remains NO-GO.

These 40 runs must be counted as third-category CI history because they execute the third-category Android workflow, even though their commits are docs-only bookkeeping. Their green status validates the configured checks for that snapshot, not 40 new Runtime features.
