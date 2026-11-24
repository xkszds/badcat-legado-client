# 第三类 CI 补充审计：#3279–#3299（2026-11-24）

- Repository: `xkszds/badcat-legado-client`
- Branch: `integration/legado-upstream-runtime`
- Parent ledger: `docs/required/CI-3200-3399-AUDIT.md`（#3200–#3399）
- Pinned Legado SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Audit date: 2026-11-24
- Scope: #3279–#3299 inclusive; 21 distinct workflow runs, each attempt 1.
- Audit status: completed for run identity, exact SHA, job/step terminal state, commit diff and functional coverage profile. No reruns or production changes were made by this audit.
- Important: this is a supplemental detail file because the main 200-CI ledger is a large append-only evidence file. It belongs to that ledger's #3279–#3299 gap and must be counted there, not as a separate CI range.

## 1. Per-run identity and exact snapshot

All runs were push events on `integration/legado-upstream-runtime`, workflow `.github/workflows/android-build.yml` (“Android debug build”), with one `assemble-debug` job. All 21 workflows concluded `success`; every returned job step was `completed/success` (53 returned steps per job, including post-job steps). No failed, skipped, cancelled or pending steps were found in these jobs.

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Conclusion |
|---:|---:|---:|---|---|---|
| #3279 | 45256546654 | 137979760490 | 2026-11-23 20:10:16 | `7751292328a955ae70a3437804b4517f31d3e5c0` | success |
| #3280 | 45256616555 | 137979994925 | 2026-11-23 20:10:38 | `14af4fa4cbd48717f9c79e2c9eca8dc429217b40` | success |
| #3281 | 45256891932 | 137980956618 | 2026-11-23 20:12:11 | `3f4ce7bbd4fa30d295c1fa008fbac726b45a4b56` | success |
| #3282 | 45256935124 | 137981102465 | 2026-11-23 20:12:25 | `e0ad5d788b78b1cc22dee86e9add484ac09bb60e` | success |
| #3283 | 45257312904 | 137982417572 | 2026-11-23 20:14:32 | `3564a5e91551f0580ba137c8569cc3afb588f203` | success |
| #3284 | 45257351042 | 137982557901 | 2026-11-23 20:14:46 | `dc2a88330a3fcacf4ee26b656cff477bfd13bf5f` | success |
| #3285 | 45257655249 | 137983597074 | 2026-11-23 20:16:26 | `7b3e9a1698a1b014bc242bfcdab72ae8290ac931` | success |
| #3286 | 45257710335 | 137983796268 | 2026-11-23 20:16:45 | `f37b0a22aa040dcdbce0fe18fc397f67d4f5f258` | success |
| #3287 | 45257866956 | 137984330091 | 2026-11-23 20:17:37 | `5d21cac66a4c712ea317ae12998db39817a2fef5` | success |
| #3288 | 45257906970 | 137984474447 | 2026-11-23 20:17:52 | `0a204f0d7d21fa9f0867d58007a12c50b3aea9e6` | success |
| #3289 | 45258251108 | 137985687008 | 2026-11-23 20:19:50 | `b0f24c45d431c3e331657c64df7f95882cebc977` | success |
| #3290 | 45258320473 | 137985928866 | 2026-11-23 20:20:14 | `3f8d2aa715db7e539a9434ad7720d7619c973c28` | success |
| #3291 | 45258593943 | 137986900454 | 2026-11-23 20:21:51 | `509822c476e67eaa403ad0e4997a9e741f9305ab` | success |
| #3292 | 45258629832 | 137987026837 | 2026-11-23 20:22:03 | `b13cbbb1093a4dafe4f09311df1d8ea0bbc7502e` | success |
| #3293 | 45259412916 | 137989817136 | 2026-11-23 20:26:35 | `0a2d829d5057b39b79e3fa7b5030d282ea2baa4e` | success |
| #3294 | 45259468055 | 137990009814 | 2026-11-23 20:26:55 | `15d0580ec221bc850e3fedc467723ddeba787388` | success |
| #3295 | 45259643432 | 137990644478 | 2026-11-23 20:27:57 | `57990d809e0e8b9d45661d82c46122f46c6d74ea` | success |
| #3296 | 45259691094 | 137990814048 | 2026-11-23 20:28:14 | `fe003c0b1c5d62076e267d33da64df18311c4201` | success |
| #3297 | 45259878926 | 137991497850 | 2026-11-23 20:29:23 | `a2abd8e932ce1b62197bbc462416bc472f3849d9` | success |
| #3298 | 45259919200 | 137991644831 | 2026-11-23 20:29:38 | `fd5f357b9f1c38d604a9487a4f1374b214ef8a83` | success |
| #3299 | 45260226309 | 137992689049 | 2026-11-23 20:31:20 | `e9f9c116cd2a79269b9a055a994e35b9dcf7ca6c` | success |

Run links follow the stable form:
`https://github.com/xkszds/badcat-legado-client/actions/runs/<Run-ID>`.
The exact Job IDs above identify the `assemble-debug` job whose steps and logs were fetched.

## 2. What each CI actually ran and validated

For each of the 21 exact SHA snapshots, the returned `assemble-debug` job completed all 53 returned steps successfully. The common configured coverage is not inferred from the workflow title: the job logs show the following operations completing:

1. **Pinned-source/build setup:** checkout, verify pinned Legado source SHA, JDK 21, Android SDK/platform/build-tools and Gradle setup.
2. **Contract/runtime tests:** `:integration-contract:test` contract unit-test invocation and authentic `:legado-runtime:testDebugUnitTest` model-contract invocation completed successfully; log shows successful Gradle task completion. The Kotlin scope parser self-test also reports `passed`.
3. **Source inventory:** generated and uploaded the pinned Legado JavaScript API inventory and source dependency-closure report. This proves the inventory/report generation steps ran, not that every API has been implemented.
4. **Rhino and runtime classpath:** restored and verified the pinned Rhino artifact and extension sources; verified the pinned RuleAnalyzer Android runtime classpath and uploaded the corresponding report.
5. **G2d source probe:** verified the G2d stage-1 authentic source manifest and compiled the authentic rule/model source group; compiler probe evidence was uploaded.
6. **Third-category app tests:** `:app:testDebugUnitTest -PbadcatThirdCategoryTestProfile=true` ran as the Third-category Runtime suite, explicitly excluding Fourth-category tests; its Gradle step completed successfully.
7. **Android instrumentation:** API 28 real Android CookieManager instrumentation and API 35 WebView-provider instrumentation completed; RuleAnalyzer instrumentation evidence and provider runtime reports were uploaded for both API levels.
8. **JVM ABI:** captured and uploaded the compiled host JavaScript bridge JVM ABI.
9. **Packaging/DEX:** unit reports uploaded; `:app:assembleDebug` completed; authentic pinned RuleAnalyzer class definition in APK DEX was verified; Debug APK and DEX evidence were uploaded.
10. **Network/source probes:** Nyasama source search probe, public Legado source repository endpoint probe, Bixiange Python smoke script and Pinshuzhai Python smoke script executed as best-effort steps. Their precise coverage boundary is described below.

The logs show the app unit-test Gradle task and all the above build/instrumentation/DEX tasks completed without a step failure. Unit-test report artifacts were uploaded for each run. This audit did not independently unpack every JUnit archive to restate its test-case count; therefore no unverified per-run numeric JUnit count is asserted here.

## 3. Actual live-source outcomes and hard boundary

The two named “live end-to-end” steps are standalone Python scripts, not the Badcat Android application and not the embedded Legado Runtime:

- **Bixiange / 笔仙阁:** the script uses external Python HTTP/HTML handling. Logs show GBK POST search, detail, TOC (114 pages), AJAX chapter retrieval and content extraction (about 2,884 characters) passing in the inspected runs.
- **Pinshuzhai / 品书斋:** the script uses external Python HTTP/HTML handling. Logs show search, detail, TOC (1,650 chapters), and chapter content extraction (about 3,116 characters) passing in the inspected runs.
- **Nyasama:** a bounded search probe, not a full reading flow. Sampled run logs show 3/5 usable searches; do not generalize this to all 21 runs without each run's exact report.
- **Public repository endpoint scan:** a bounded GET probe, 14 endpoints attempted; inspected runs returned 7–9 non-empty HTTP 200 responses. This is endpoint reachability/extraction evidence only, not rule-engine compatibility.

These scripts do **not** start Badcat Android or Legado Runtime. They do not prove imported BookSource execution through Rhino, JsExtensions, AnalyzeRule/AnalyzeUrl, BaseSource lifecycle, Cookie/login/session handling, full pagination, or complete Legado compatibility. Their “E2E” labels are therefore external Python source smoke only.

## 4. Commit scope and product change interpretation

The 21 corresponding commit diffs were checked. They are documentation/audit updates under `docs/audits/` and `docs/required/`; no application source, tests, Gradle configuration or UI files were changed by these commits. The repeated green workflows therefore validate the same application/test code state against successive documentation-only exact SHAs; they are separate CI records but not 21 independent product feature implementations.

Examples of the documentation-only audit topics include pinned model/rule contracts, source execution caller/dependency graph, Gradle/package closure, source-runtime semantic parity, Rhino object exposure/sandbox ABI, upstream API field inventory, inherited data/batch contracts, and JavaScript bridge API differences. These topics are architecture-audit evidence, not runtime feature completion.

## 5. Per-run result matrix

| CI range | Workflow | Job/Step result | Actual feature coverage | Source-flow result | Product acceptance |
|---|---|---|---|---|---|
| #3279–#3299 (each run independently identified in §1) | 21/21 success | 21/21 jobs success; 53/53 returned steps success per job; no skip/failure/pending found | Contract/runtime model tests, JS API/dependency inventory generation, Rhino/runtime classpath, G2d authentic source compile probe, Third-category app unit suite, API28/35 instrumentation, JVM ABI, Debug APK assembly, RuleAnalyzer DEX definition check | External Python Bixiange and Pinshuzhai search→detail→TOC→content smoke passed in inspected logs; Nyasama/public probes are bounded and variable | **Not accepted** as complete Legado Runtime, Android Runtime E2E, Stage-0 GO, Stage-1 GO or release-ready |

## 6. Reconciliation

- Distinct runs: 21.
- Workflow conclusion: 21 success / 0 failure / 0 pending.
- Jobs: 21 `assemble-debug` jobs, all success.
- Returned steps: 53 per job, all completed/success.
- Commit scope: documentation-only audit changes; no production implementation changes in these 21 commits.
- Debug APK artifacts: uploaded by each successful workflow; these are CI debug artifacts, not release APK acceptance.
- JUnit reports: uploaded by each run; numeric test count intentionally not restated without independently unpacking each archive.
- Android Runtime real-source E2E: **not run / no evidence**. External Python/HTTP smoke must not be counted as Android Runtime E2E.
- Complete Legado compatibility: **not proven**.
- Stage-0 architecture review: remains in progress / not accepted.
- Stage-1 Runtime boundary PoC: remains NO-GO; do not start Stage-1 based on these green workflows.

## 7. Evidence links

For each CI, open its run URL using the Run ID in §1. The Job ID in §1 is the exact `assemble-debug` job inspected. The job log and run artifacts are attached to that run; artifact families observed include:
- `badcat-unit-test-reports`
- `badcat-legado-debug`
- `legado-runtime-3.2f.46-apk-dex-class-definitions`
- `legado-runtime-3.2f.46-runtime-classpath`
- `legado-runtime-dependency-closure`
- `legado-js-abi-inventory`
- `legado-host-rhino-surface`
- `legado-host-jvm-abi`
- `legado-runtime-g2d-stage1-probe`
- API 28 / API 35 RuleAnalyzer instrumentation and WebView provider runtime reports
- `badcat-live-source-smoke-report`
- `badcat-pinshuzhai-live-source-smoke-report`

Audit conclusion: the missing #3279–#3299 run-by-run identity and functional-coverage gap is now filled with exact snapshots, jobs, step outcomes, per-run source-flow boundary and explicit non-acceptance. This does not change the Runtime implementation state.
