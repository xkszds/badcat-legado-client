# Runtime-3.2f.41 — GitHub Actions #3201–#3225 unified inspection

Date: 2026-11-23
Repository: xkszds/badcat-legado-client
Branch: integration/legado-upstream-runtime
Branch HEAD at inspection: 06b9284379c6fbaa559724e5df23216d3bd1c349
Pinned Legado SHA: 330e606c4d57e08adc056ff8ab7a4132c784da9a

## Scope and method

This is the requested unified inspection of every Actions run strictly greater than #3200, as visible in the repository snapshot. 25 runs (#3201–#3225) were found. Each completed run has one assemble-debug job; the completed runs' job/step conclusions were checked. JUnit ZIP artifacts were downloaded and parsed for all completed runs #3201–#3222 (41 XML suites per run). Failure logs were checked for #3213–#3216. Debug APK artifacts were checked for #3217, #3221 and #3222; API 28/35 WebView callback reports and live-source/corpus reports were inspected for #3221 and #3222.

No Actions run was re-run. No source, test, build, workflow, UI or storage code was changed as part of this CI inspection.

## Run inventory

| Run | Run ID | Exact head SHA | Conclusion at inspection |
|---:|---:|---|---|
| 3201 | 45090836310 | eed5eac60f5ea18d3095613e73e068b7a8f195b5 | success |
| 3202 | 45090918122 | aad72671de37bf9104d1b605786a5a55009b2ae0 | success |
| 3203 | 45090951993 | 0971ddbe86eb534d4ee891984eb7870da3960a15 | success |
| 3204 | 45091181922 | 63017975238aaf55ec3dc517b69a415d11940846 | success |
| 3205 | 45091312073 | c13c0fdc0c048a8ce8c3e7a4d601c149788bcc2f | success |
| 3206 | 45093252549 | e8e26ba59a64015abed99ed7f356a91d443c7a21 | success |
| 3207 | 45093347465 | 6ff710efe976b76db4f37b00fe7915748cb279d5 | success |
| 3208 | 45093475926 | fea64118ed997e49f09de006bb9f81be2ce8811b | success |
| 3209 | 45093562993 | 02b55e900c261975eb18fe1a7abe7fa379f29f1c | success |
| 3210 | 45093601378 | d1f7736bb2fb5340a4096c551f7d427420ed1811 | success |
| 3211 | 45093628843 | b5cda4af4dd1ce7214a9e65cdd97ba7763db7c2c | success |
| 3212 | 45093718439 | b4afb55a0cb8ad5ba9acb6a128e7e81d05d86068 | success |
| 3213 | 45093761811 | 19dd06f09b1ce896d3c0464ca5907651b4fcd897 | failure |
| 3214 | 45093892515 | 6f166b267df193dd3533352149c4aa2aeffbc348 | failure |
| 3215 | 45093940981 | 638fa91933d1e9673ac76a5c35aff79435ec215c | failure |
| 3216 | 45102257055 | b03759ce9ac2623bbf6c399ae300619b6acc7ca8 | failure |
| 3217 | 45102448235 | 80b0b93e2d25d133f27cfeac905b484d5a7e74ae | success |
| 3218 | 45106878755 | 620d6553d06631e10016eadba9b8e8e50ae2d82f | success |
| 3219 | 45106957919 | 658a8eb1c6fd2d5d86001e700454f1937fe42afc | success |
| 3220 | 45106982334 | 249f72c4334df993e6125fff7448cc9e1c0ec962 | success |
| 3221 | 45193908754 | 88099116db0a86365ae162a67b1639d9e1415b6f | success |
| 3222 | 45193966686 | 2aa0b6524bccdf74fcf95f52691a7fd86fd4122b | success |
| 3223 | 45195161068 | bf4fa7f85be3afa2ff1eb5dda7d5e098f91b3c01 | in_progress |
| 3224 | 45195170807 | a45faf2104b220fe6712c9ab10f952abc2e76b3d | in_progress |
| 3225 | 45196110187 | 06b9284379c6fbaa559724e5df23216d3bd1c349 | in_progress |

At the status refresh, #3221 and #3222 had completed successfully. #3223, #3224 and #3225 still had an in-progress assemble-debug job and no JUnit or artifact set was available yet. Their conclusions remain pending; this report does not count them as passes.

## JUnit results

All parsed reports contain 41 XML suites.

| Runs | Tests per run | Failures | Errors | Skipped | Result |
|---|---:|---:|---:|---:|---|
| #3201–#3206 | 329 | 0 | 0 | 0 | pass |
| #3207–#3209 | 331 | 0 | 0 | 0 | pass |
| #3210–#3212 | 332 | 0 | 0 | 0 | pass |
| #3213–#3216 | 333 | 1 | 0 | 0 | fail |
| #3217–#3222 | 335 | 0 | 0 | 0 | pass |

The sole failure in each of #3213–#3216 is app.badcat.client.runtime.LegadoSourceLoginUiV2ServiceTest.mainJsInitializesLoginUiOnceInTheRealRhinoSourceScope, a ClassCastException where EngineResult.Failure was cast to EngineResult.Success. The failure is in the app JVM unit-test step (step 22); contract tests, pinned model tests, G2d source compilation, emulator instrumentation and debug APK assembly/upload completed in those jobs. These four failed runs are not accepted as passing CI.

The failure was corrected in a descendant implementation. #3217–#3222 each pass 335 tests with zero failures/errors/skips. #3217's exact-HEAD closeout was previously recorded; the downloaded JUnit artifacts for #3218–#3222 were independently parsed in this inspection.

## Job and step checks

- Every completed successful run (#3201–#3212 and #3217–#3222) has one assemble-debug job with no failed or skipped steps in the job-step summary.
- #3213–#3216 each fail only at “Run app unit suite for Third-category Runtime (exclude Fourth-category tests)”; later report uploads, Android instrumentation, debug APK assembly and upload continue successfully.
- The workflow is best-effort for selected live-source probes. Their step status being successful does not imply every uploaded source is reachable or every candidate passed.

## Artifacts and runtime probes

- JUnit artifacts were downloaded and parsed for every completed run #3201–#3222.
- Debug APK artifacts are present for the completed runs, including the four test-failing runs. These are CI debug builds, not release-signed delivery packages and not user-device installation evidence.
- APK verification:
  - #3217 artifact 14786128724: app-debug.apk 32,024,734 bytes; SHA-256 1753f6c62d8dae6ccc5f659dfa81344307379ae177976d16aaeee9a8bf15399b.
  - #3221 artifact 14827776140: app-debug.apk 32,024,734 bytes; SHA-256 338fb1ec82578b72f607b8986daaa2894f8c8bd4ebdb46b2c67134e982d498bf.
  - #3222 artifact 14827378900: app-debug.apk 32,024,734 bytes; SHA-256 5c4e5258a7ac09250693986b030c75c53b4cee0324a9f640c9291be7c8cc2376.
- #3221 and #3222 API 28 and API 35 provider-runtime reports each show all four callback cases (string, number, null, undefined) with ok=true and selected=INT. API 28 used Chrome 69 provider; API 35 used Android WebView 124 provider.
- #3221 and #3222 live Bixiange and Pinshuzhai search → detail → TOC → content flows pass.
- Uploaded corpus in both #3221 and #3222: 1/4 sources passed full reading (精华书阁); 唐三中文 and 零零小说 timed out, while 笔趣阁fun returned HTTP 404. This is partial live-source coverage, not full corpus success.

## Acceptance and handoff

- Completed successful CI runs: 18.
- Completed failed CI runs: 4.
- Still running at the last refresh: 3 (#3223–#3225).
- #3217–#3222 provide a stable 335/335 JVM unit-test pass sequence after the Login UI v2 correction. This does not certify full Legado compatibility or release readiness.
- #3223–#3225 must be revisited when terminal; inspect their exact SHA, job/step conclusions, JUnit, reports and APK artifacts. Do not borrow success from an earlier SHA.
- No development task was started as part of this inspection. Do not proceed to other tasks until the three pending runs are terminal and recorded.
