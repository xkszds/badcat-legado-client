# 第三类 CI 审计写回触发的 5 次工作流：#3463–#3467

- Repository: xkszds/badcat-legado-client
- Branch: integration/legado-upstream-runtime
- All commits are docs-only audit writebacks; no product source/test/Gradle/UI files changed.
- All five workflows completed success. Each assemble-debug job returned 53/53 steps completed/success.
- These runs are included in the new 200-number book CI-3400-3599-AUDIT.md because the branch workflow executes the Third-category profile. They are audit bookkeeping runs, not five new Runtime feature implementations.

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Commit change |
|---:|---:|---:|---|---|---|
| #3463 | 45377820497 | 138388665085 | 2026-11-24 10:45:39 | 1095ebc981c3a59fd61bb57f3d28a883b1c414cc | Added CI-3279-3299-DETAIL-AUDIT.md |
| #3464 | 45377917638 | 138389005994 | 2026-11-24 10:46:16 | aa62feda3a36880e6244f386952ecc83660e21c2 | Updated 03-LEGADO-RUNTIME-OVERVIEW.md |
| #3465 | 45378086416 | 138389595674 | 2026-11-24 10:47:22 | 6d794be956c08481dcc55dd51b19da39ae7bf871 | Added CI-3350-3357-DETAIL-AUDIT.md |
| #3466 | 45378882191 | 138392363304 | 2026-11-24 10:52:28 | bc6ce1cceb2e8184173559b254b171abf86c1886 | Added CI-3358-3397-DETAIL-AUDIT.md |
| #3467 | 45379438893 | 138394301665 | 2026-11-24 10:56:05 | e74118f7ae40bdb37428ee175a29c1279c19b81c | Added CI-3400-3422-DETAIL-AUDIT.md |

## Shared functional coverage actually run

All five jobs completed the same current Android workflow profile:
- pinned Legado source SHA verification, JDK 21 / Android SDK / Gradle setup;
- integration contract tests and authentic Legado runtime model-contract tests;
- pinned JavaScript API inventory and source dependency-closure generation;
- Rhino artifact/source and RuleAnalyzer runtime-classpath verification;
- G2d authentic source manifest and compile probe;
- Third-category-only app unit suite, excluding Fourth-category tests;
- API28 CookieManager instrumentation and API35 WebView-provider instrumentation;
- host JavaScript bridge JVM ABI capture;
- Debug APK assembly and RuleAnalyzer DEX class-definition verification;
- bounded Nyasama/public endpoint probes and external Python source smoke scripts.

Logs for each run show:
- Nyasama: 3/5 usable search results.
- Public source endpoint sweep: 14 GET attempts, 8–9 non-empty HTTP 200 responses.
- Bixiange: LIVE_BIXIANGE_PASS (GBK POST search → detail → TOC AJAX page → chapter content).
- Pinshuzhai: LIVE_PINSHUZHAI_E2E_PASS (search → detail → TOC → content).

These Bixiange/Pinshuzhai checks are standalone Python HTTP/HTML scripts, not Badcat Android or embedded Legado Runtime execution. They do not prove imported BookSource execution through Rhino/JsExtensions/BaseSource, full login/Cookie/session, pagination parity or complete Legado compatibility.

## Artifact IDs

| CI | JUnit report | Debug APK | RuleAnalyzer DEX | Bixiange report | Pinshuzhai report |
|---:|---:|---:|---:|---:|---:|
| #3463 | 14912023713 | 14912872536 | 14911479646 | 14912488225 | 14912243714 |
| #3464 | 14911544533 | 14912268169 | 14912103973 | 14914405181 | 14914540088 |
| #3465 | 14913455642 | 14913665085 | 14912957683 | 14913281903 | 14913042603 |
| #3466 | 14912664576 | 14914110218 | 14914165125 | 14913995766 | 14914835023 |
| #3467 | 14912809673 | 14914125745 | 14913293300 | 14914595641 | 14912859676 |

## Acceptance

- 5/5 workflow success; 5/5 jobs success; 53/53 returned steps success per job.
- All changes were docs-only. The CI success validates the workflow at each docs SHA but does not add product features.
- Debug APKs are CI debug artifacts, not release acceptance.
- External Python smoke is not Android Runtime E2E.
- Stage-0 remains in progress / not accepted; Stage-1 remains NO-GO.
