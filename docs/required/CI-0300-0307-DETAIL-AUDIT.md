# 第三类历史 CI 详细审计：#0300–#0307（8 次运行）

- Repository: xkszds/badcat-legado-client
- Branch/event: main / push
- Workflow: Android debug build
- Outcome: 0 success / 8 failure / 0 pending.
- All eight jobs failed at Android SDK setup Step 4; Steps 5–9 (platform, Gradle, contract tests, APK assembly/upload) skipped. Sampled logs around #300 and #307 show missing SDK package tools and sdkmanager exit code 1.
- No compile, tests, JUnit, APK, DEX, instrumentation or live-source evidence was produced.

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Actual change |
|---:|---:|---:|---|---|---|
| #300 | 43703204334 | 132778608152 | 2026-11-15 10:15:37 | 1f44e801a8852fab4f908403a55a26de3b04960e | Added per-source URL script evaluator provider to SourceHttpGateway (+5/-2); not compiled |
| #301 | 43703207291 | 132778616816 | 2026-11-15 10:15:40 | 144c9bdaaa2a7ff8ff03695638c06d13d09198cc | Added source-isolated URL evaluator selection tests; not executed |
| #302 | 43703210005 | 132778625111 | 2026-11-15 10:15:42 | 9e7e3122d3d0c987ec7461d5943720d403cfbe25 | Added shared multi-source Android gateway factory (+15/-8); not compiled |
| #303 | 43703213094 | 132778634682 | 2026-11-15 10:15:44 | b06601de8066916113d2323e618551002bd148df | Documented multi-source evaluator isolation fix; docs only |
| #304 | 43703304959 | 132778921759 | 2026-11-15 10:16:54 | ec1d71ef47200f814576e106a447e677711c0cc4 | Added Android reading engine composition root (54 lines); not compiled |
| #305 | 43703308055 | 132778931380 | 2026-11-15 10:16:56 | c801f8bc9871c9ad6b49512ce67a9ce7d165f575 | Constructed reading engine graph at app launch in MainActivity (+6/-1); not compiled |
| #306 | 43703318607 | 132778962715 | 2026-11-15 10:17:04 | 772884da8189d1637c6c067ff439a6b42499b408 | Documented Android reading engine composition; docs only |
| #307 | 43703434898 | 132779316777 | 2026-11-15 10:18:31 | 248746b958b7158715ca5cd60e8d1a60f69a1f04 | Added minimal source-aware search validation screen (148 lines); not compiled |

## Functional interpretation

The code attempts are:
- per-source URL script evaluator selection and shared multi-source HTTP gateway composition;
- Android ReadingEngine composition root and app-launch wiring;
- a source-aware search validation screen.

These are source-runtime integration and UI entry points, but **none was compiled or tested**. The screen, app launch graph, and gateway must not be counted as working. No source search was executed and no Android Runtime E2E occurred.

## Reconciliation

- 8/8 workflow failure at SDK setup Step 4.
- 8/8 Gradle, test, APK and source-probe steps skipped.
- No JUnit/APK/DEX artifacts.
- No product feature pass or compatibility acceptance.
