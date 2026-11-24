# 第三类 CI 补充审计：#3350–#3357（2026-11-24）

- Repository: `xkszds/badcat-legado-client`
- Branch: `integration/legado-upstream-runtime`
- Parent ledger: `docs/required/CI-3200-3399-AUDIT.md`（#3200–#3399）
- Pinned Legado SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: #3350–#3357 inclusive; 8 distinct push runs, attempt 1.
- Audit status: complete for run identity, exact SHA, job/step states, commit scope, logs, artifacts and actual coverage boundaries.

## 1. Per-run identity

All eight runs used workflow `.github/workflows/android-build.yml` (“Android debug build”), one `assemble-debug` job, and push events on `integration/legado-upstream-runtime`. All workflows concluded success. Each job returned 53 steps; all 53 were completed/success. No failed, skipped, cancelled or pending step was found.

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Commit scope | Result |
|---:|---:|---:|---|---|---|---|
| #3350 | 45287185482 | 138086438561 | 2026-11-23 23:15:22 | `14aea7dfe02a6443b4df71cf9cf8ae31faa75ffd` | `docs/required/CI-3300-3349-AUDIT.md` | success |
| #3351 | 45287203642 | 138086499186 | 2026-11-23 23:15:29 | `ef9ce7f46774a507afbb97dda2ca0638af31fffa` | `docs/required/00-MASTER-WORKFLOW.md` | success |
| #3352 | 45287253646 | 138086668600 | 2026-11-23 23:15:49 | `4acef7d173525595daf9b9b61fecd781d2232bd9` | `docs/required/CI-3300-3349-AUDIT.md` | success |
| #3353 | 45287259506 | 138086686021 | 2026-11-23 23:15:51 | `9ed3ddc942891d0fe1b275fac582521ef80ca6a3` | `docs/required/00-MASTER-WORKFLOW.md` | success |
| #3354 | 45287309333 | 138086853584 | 2026-11-23 23:16:10 | `86cd4597916076a48c9106874685a9709c425369` | `docs/required/CI-3300-3349-AUDIT.md` | success |
| #3355 | 45287326843 | 138086915012 | 2026-11-23 23:16:18 | `22ed6dbbad4201e6882feb3e4a8f5741642b25eb` | `docs/required/00-MASTER-WORKFLOW.md` | success |
| #3356 | 45287351483 | 138086998371 | 2026-11-23 23:16:27 | `817a221d3b8dd7bf12bc5f7e46b612a91c4641f2` | `docs/required/00-MASTER-WORKFLOW.md` | success |
| #3357 | 45287382801 | 138087106504 | 2026-11-23 23:16:40 | `d6fc9d05165fd9132ebd9fd8f67731e5c140b0f3` | `docs/required/CI-3300-3349-AUDIT.md` | success |

Run URL pattern: `https://github.com/xkszds/badcat-legado-client/actions/runs/<Run-ID>`. Job IDs above identify the exact `assemble-debug` jobs inspected.

## 2. Actual functional coverage

For every one of these eight runs, logs and step summaries show successful execution of:

1. Pinned Legado source verification, JDK 21 / Android SDK / Gradle setup.
2. `:integration-contract:test` and authentic `:legado-runtime:testDebugUnitTest` model-contract test invocations.
3. Pinned JavaScript API inventory generation and source dependency-closure generation.
4. Rhino artifact/source restoration and verification; RuleAnalyzer Android runtime-classpath verification.
5. G2d authentic source manifest verification and authentic rule/model source-group compile probe.
6. Third-category-only app JVM unit suite (`:app:testDebugUnitTest -PbadcatThirdCategoryTestProfile=true`), excluding Fourth-category tests.
7. API 28 CookieManager instrumentation and API 35 WebView-provider instrumentation, with RuleAnalyzer evidence and provider runtime reports.
8. Compiled host JavaScript bridge JVM ABI capture.
9. Debug APK assembly, authentic RuleAnalyzer DEX class-definition verification, unit-report and APK upload.
10. Best-effort Nyasama/public endpoint probes and external Python Bixiange/Pinshuzhai source scripts.

These prove the configured test/build/instrumentation/packaging steps ran successfully for each exact SHA. They do not prove every pinned upstream API is implemented or behaviorally equivalent. The JUnit report artifacts exist for all eight runs; this audit does not assert a numeric test count without independently unpacking each report archive.

## 3. Real-source evidence and limits

- **Bixiange / 笔仙阁:** logs for all eight inspected jobs show GBK POST search, detail, 114-page TOC, AJAX chapter fetch and extracted chapter content (~2,884 characters).
- **Pinshuzhai / 品书斋:** logs show search, detail, 1,650-chapter TOC and extracted chapter content (~3,116 characters).
- These two scripts are standalone Python/HTTP/HTML parsers. They do not launch Badcat Android or the embedded Legado Runtime; they are external source smoke tests, not Android Runtime E2E.
- Nyasama is only a bounded search probe. Public repository checks are only bounded GET endpoint reachability/extraction probes. Neither proves full source-rule execution.
- No evidence here covers full Rhino/JsExtensions execution against imported source JSON, BaseSource lifecycle, login/Cookie/session behavior, multi-page content parity, or complete Legado compatibility.

## 4. Exact artifacts (key artifact IDs)

| CI | JUnit report | Debug APK | RuleAnalyzer DEX report | Bixiange smoke report | Pinshuzhai smoke report |
|---:|---:|---:|---:|---:|---:|
| #3350 | 14872092919 | 14871649970 | 14871754378 | 14871998783 | 14872703186 |
| #3351 | 14872088921 | 14873470682 | 14871919711 | 14872963527 | 14872598872 |
| #3352 | 14872732709 | 14871674934 | 14872823547 | 14874220183 | 14873630902 |
| #3353 | 14873276122 | 14872618076 | 14873675457 | 14872429830 | 14873341277 |
| #3354 | 14872988088 | 14874035853 | 14874450084 | 14873617060 | 14874380370 |
| #3355 | 14872374336 | 14873330967 | 14873410861 | 14873890423 | 14872379116 |
| #3356 | 14872912974 | 14873172669 | 14872357808 | 14872653484 | 14872273778 |
| #3357 | 14872568537 | 14873810396 | 14872353826 | 14871904635 | 14872024402 |

Additional artifacts for each run include API28/API35 RuleAnalyzer instrumentation, WebView provider runtime reports, emulator diagnostics, Rhino host surface, JVM ABI, JavaScript API inventory, dependency closure and G2d compile-probe evidence. They are attached to each run URL above.

## 5. Commit scope and classification

All eight commit diffs are documentation-only updates to the CI audit ledger or master workflow checklist. No app source, test source, Gradle or UI file changed. These are **bookkeeping/documentation CI runs** triggered by recording or reconciling the #3300–#3349 audit. They must be included in the third-category historical CI record because they run the third-category workflow, but must not be counted as eight new runtime feature implementations.

## 6. Reconciliation

- Distinct runs: 8.
- Workflow results: 8 success / 0 failure / 0 pending.
- Jobs: 8 `assemble-debug`, all success.
- Returned steps: 53 per job, all completed/success.
- Exact snapshots: all eight full HEAD SHAs recorded above.
- Commit changes: documentation-only; no production implementation changes.
- Debug APK and reports: uploaded; debug artifacts are not release acceptance.
- Android Runtime real-source E2E: not run / no evidence.
- Complete Legado compatibility: not proven.
- Stage-0: remains in progress / not accepted.
- Stage-1: remains NO-GO.

Audit conclusion: #3350–#3357 are no longer “metadata-only pending.” Their exact run/job/SHA, step outcomes, feature coverage, docs-only nature, source-flow boundaries and key artifact IDs are now recorded. This changes the CI bookkeeping status only, not Runtime implementation acceptance.
