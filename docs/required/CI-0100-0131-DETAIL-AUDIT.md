# 第三类历史 CI 补充审计：#0100–#0131（实际存在的 24 次运行）

- Repository: `xkszds/badcat-legado-client`
- Historical branch/event: `main / push`
- Workflow: `Android debug build`
- Fixed upstream SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: actual candidate runs #100–#131 recorded in `CI-0001-0199-AUDIT.md`; missing numbers are not fabricated.
- All 24 runs are attempt 1 and completed/failure. Each has one `assemble-debug` job; Steps 1–3 success, Step 4 “Set up Android SDK” failure, Steps 5–9 skipped, cleanup success.
- Direct logs consistently show `Warning: Failed to find package 'tools'` and `sdkmanager` exit code 1. Gradle never starts; no compile, unit test, contract test, instrumentation, APK or real-source probe is executed. No JUnit/APK artifacts are produced.

## 1. Per-run identity and exact change

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Commit change (source diff inspected) |
|---:|---:|---:|---|---|---|
| #100 | 43694689713 | 132752528978 | 2026-11-15 08:28:46 | `81776387f55a5dfe6adefed3e23866e2bab23947` | Added 4 lines to `SourceRuntime.kt`: extend HTTP request contract with Legado URL options; not compiled |
| #101 | 43694703814 | 132752575496 | 2026-11-15 08:28:58 | `062c1f422bc77ce7d6baa09993d16b16b336bee6` | Updated `SourceRequestFactory.kt` (+39/-2) to connect resolved URL options to request creation; not compiled |
| #102 | 43694722815 | 132752636193 | 2026-11-15 08:29:14 | `0e135b63fc5321bf210ea4f4169d7d83059e5a9d` | Updated `OkHttpSourceRequestClient.kt` (+35/-8) for timeout/redirect/retry/charset options; not compiled |
| #104 | 43694740989 | 132752693557 | 2026-11-15 08:29:30 | `990c407263b9dbd168198ca62cf6155e16434df1` | Added 26 request-factory tests for resolved URL option flow; not executed |
| #107 | 43694775958 | 132752804072 | 2026-11-15 08:29:58 | `debb165383c0608109bd6681a3897066d9cddfba` | Updated `docs/build-status.md`; documentation only |
| #108 | 43694924533 | 132753240452 | 2026-11-15 08:31:44 | `df1c0770bf29dc7907ee9d89706186b6e5fce5cc` | Added 40 lines to `SourceHttpGateway.kt`: static source HTTP gateway; not compiled |
| #109 | 43694933274 | 132753266633 | 2026-11-15 08:31:51 | `86bcc777c736483e47db97ed817a43a9eb335f28` | Added 78 lines of HTTP gateway tests; not executed |
| #110 | 43694940535 | 132753288448 | 2026-11-15 08:31:56 | `b5b794d7c05b2241ce2927eaaf53535f22b2bd4a` | Updated `docs/build-status.md`; documentation only |
| #111 | 43695062943 | 132753659304 | 2026-11-15 08:33:28 | `24ed2bfaeb1e40873911f304f85cf6b4cfecf7b9` | Added 104 lines to `CssSearchResultsParser.kt`: bounded CSS search result parser; not compiled |
| #112 | 43695078152 | 132753704843 | 2026-11-15 08:33:39 | `1676a5ec48be04731f161b9d7fcfcd061ad0f5aa` | Updated CSS search parser (+25/-13) to safely handle unsupported/invalid search-field rules; not compiled |
| #113 | 43695089007 | 132753737832 | 2026-11-15 08:33:48 | `eb2d9788ef802fbe5beb38054765321ac2d3a303` | Added 70 lines to `CssSearchResultsParserTest.kt`; not executed |
| #114 | 43695097988 | 132753764739 | 2026-11-15 08:33:55 | `0bae783cc27ad15b8740392ae61a585416d6597d` | Added 39 lines to `SourceSearchService.kt`: bounded source search service; not compiled |
| #115 | 43695105107 | 132753786405 | 2026-11-15 08:34:00 | `9122fbae83c274f64c53e18689a404992bbf9b39` | Updated `docs/build-status.md`; documentation only |
| #117 | 43695237962 | 132754184883 | 2026-11-15 08:35:38 | `df66437fa4830a607447fc23c1357d910413779e` | Added TOC URL exposure to `ReadingEngine.kt`; not compiled |
| #122 | 43695382658 | 132754625882 | 2026-11-15 08:37:30 | `02cae009b710d96cef771e37ece1ff712aff4886` | Added 48 lines to `SourceTocService.kt`: guarded multi-page TOC loading; not compiled |
| #123 | 43695390826 | 132754652081 | 2026-11-15 08:37:37 | `846d7f2a830c9bccf609e1a24055452ba6a5481e` | Adjusted `CssTocParser.kt` to extract pagination hrefs as URLs; not compiled |
| #124 | 43695403609 | 132754693801 | 2026-11-15 08:37:48 | `64d6aadabbc9813fae9b966439bc627523a5e559` | Added 59 lines of CSS TOC/pagination tests; not executed |
| #125 | 43695414018 | 132754726273 | 2026-11-15 08:37:56 | `687fd7a03a740c859dd4b2eef98de18d99bb0e87` | Updated `docs/build-status.md`; documentation only |
| #126 | 43695539781 | 132755121031 | 2026-11-15 08:39:38 | `775767934ec24b248fdb814450e995892469aa41` | Added 115 lines to `CssChapterContentParser.kt`: bounded chapter content parser; not compiled |
| #127 | 43695550181 | 132755154994 | 2026-11-15 08:39:47 | `419944b02a22601416d9405bc9d8da63161f34b1` | Added 25 lines to `SourceChapterContentService.kt`: chapter content fetch service; not compiled |
| #128 | 43695564142 | 132755199719 | 2026-11-15 08:39:59 | `a2ad8e84e8bdf0a1aea6d6112235d28cece0dc6c` | Added 76 lines of CSS chapter content parser tests; not executed |
| #129 | 43695584935 | 132755257838 | 2026-11-15 08:40:13 | `9c88946e7ada4ebc5639df02565a7f8ca577bb4a` | Updated `docs/build-status.md`; documentation only |
| #130 | 43695609881 | 132755331633 | 2026-11-15 08:40:31 | `91588b4111347c2f8af2b5829ad4f31fb56f8668` | Updated `SourceChapterContentService.kt` (+7/-1) to apply book text replacements after parsing; not compiled |
| #131 | 43695623299 | 132755374543 | 2026-11-15 08:40:41 | `9476626e8155c471586b02a580ca05adb0de9264` | Updated `SourceChapterContentService.kt` (+11/-3) to contain text-replacement failures; not compiled |

Run URL pattern: `https://github.com/xkszds/badcat-legado-client/actions/runs/<Run-ID>`.

## 2. Job/Step and failure evidence

For each run in §1:
- Step 1 Set up job: success.
- Step 2 Check out source: success; checkout exact SHA matches §1.
- Step 3 Set up JDK 21: success.
- Step 4 Set up Android SDK: failure.
- Step 5 Install Android platform and build tools: skipped.
- Step 6 Set up Gradle: skipped.
- Step 7 Run contract unit tests: skipped.
- Step 8 Assemble debug APK: skipped.
- Step 9 Upload debug APK: skipped.
- Post JDK / Post checkout / Complete job: cleanup success.

Direct root cause: the runner reports a preinstalled sdkmanager version mismatch, invokes cmdline-tools/16.0 to install package `tools`, then emits `Warning: Failed to find package 'tools'` and exits with code 1. This is an Android SDK environment/package configuration failure before Gradle.

## 3. What this CI range does and does not prove

The commit diffs show the historical implementation attempts:
- Legado URL option fields flowing through SourceRuntime → SourceRequestFactory → OkHttpSourceRequestClient, including timeout, redirect, retry and charset options.
- A static-source HTTP gateway.
- Bounded CSS search-result parsing and source search orchestration.
- TOC URL exposure and guarded multi-page TOC loading.
- Bounded CSS chapter-content parsing and a content fetch service.
- Post-parse book text replacement and replacement-failure containment.

But **none of these changes was compiled or tested in these CI runs**. Tests in #104, #109, #113, #124 and #128 were added but not executed. No JUnit reports, Debug APKs, DEX reports, Android instrumentation or source-smoke reports were produced. No live source was contacted.

These source implementations are historical attempts and must not be counted as completed functionality, even when later documentation says they exist. Full Legado AnalyzeRule/Rhino/JsExtensions/BaseSource semantics, Android Runtime E2E, login/Cookie/session and multi-page real-source parity are not established by this range.

## 4. Reconciliation

- Actual runs: 24.
- Workflow results: 0 success / 24 failure / 0 pending.
- All 24 fail at Android SDK setup Step 4 for missing `tools` package.
- Gradle, compile, tests, APK, DEX, instrumentation and live-source steps: skipped.
- JUnit/APK/artifacts: none produced.
- No Runtime feature pass or product acceptance can be claimed.
