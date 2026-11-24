# 第三类历史 CI 补充审计：#0030–#0099（实际存在的 53 次运行）

- Repository: `xkszds/badcat-legado-client`
- Historical branch/event: `main / push`
- Workflow: `Android debug build`
- Audit date: 2026-11-24
- Fixed upstream SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: actual candidate runs #30–#99 from `CI-0001-0199-AUDIT.md`; missing numbers in this interval are not fabricated.
- All 53 runs are attempt 1, workflow `completed/failure`. Each exact Job has the same terminal pattern: Set up job / checkout / JDK21 success; Set up Android SDK failure; install Android platform/build-tools, Gradle setup, contract tests, Debug APK assembly and APK upload skipped; post-job cleanup success.
- The failure log in the inspected jobs consistently says `Warning: Failed to find package 'tools'`, followed by `sdkmanager` exit code 1. The runner found a preinstalled sdkmanager whose version was reported as wrong, then invoked cmdline-tools/16.0 to install the obsolete `tools` package, which was unavailable.
- No Gradle test, compile, APK assembly, JUnit, Android instrumentation or real-source step ran in these 53 workflows. No JUnit/APK artifacts were produced because their steps were skipped. These are build-environment failures, not evidence that the submitted code works or that the code itself is broken.

## 1. Per-run identity, exact snapshot and commit change

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Commit change (source diff inspected) |
|---:|---:|---:|---|---|---|
| #30 | 43691845685 | 132743964547 | 2026-11-15 07:52:00 | `6c6bbc1d3a428abb5f2f903e5354399038469439` | Added 14 lines to `ReadingEngine.kt`: expanded host source metadata contract; not compiled |
| #31 | 43691858961 | 132744001029 | 2026-11-15 07:52:10 | `122b4589f9e60d3089cf30234d0efa9672f2cebe` | Added 49 lines to `LegadoBookSourceCodec.kt`: source rule metadata mapping; not compiled |
| #32 | 43691870880 | 132744035898 | 2026-11-15 07:52:19 | `db8eb44552d9667edbe45806e58217036e63ba97` | Added 19 lines to `LegadoBookSourceCodecTest.kt`: rule metadata mapping tests; not executed |
| #33 | 43691883820 | 132744076166 | 2026-11-15 07:52:30 | `24aade53186e50b973e4614d430a569b27cf2054` | Corrected one escaped source-header test fixture; not executed |
| #34 | 43691892643 | 132744103038 | 2026-11-15 07:52:38 | `45cae213dd30e3ef4350e4a06e7caeaefaab7bf1` | Changed `LegadoBookSourceCodec.kt` cookie-jar default (6 deletions/1 addition); not compiled |
| #35 | 43691899618 | 132744123877 | 2026-11-15 07:52:44 | `0642924b29373089f794ff003da971b275364b45` | Added upstream cookie-jar default assertion; not executed |
| #36 | 43691910497 | 132744157078 | 2026-11-15 07:52:53 | `329791f69a0711bc3e90d3a59f421f70db432fee` | Updated `docs/build-status.md`; documentation only |
| #37 | 43692029681 | 132744513793 | 2026-11-15 07:54:31 | `9f6fb22dd1bee3ead27b2207e676e3b9ec9fa57c` | Added 69 lines to `SourceRuntime.kt`: source runtime adapter boundary; not compiled |
| #38 | 43692031560 | 132744519399 | 2026-11-15 07:54:33 | `54d8f8f12e16345c745c3949dfe0c72b00cf9a03` | Added 47 lines to `SourceRuntimeContractTest.kt`; not executed |
| #39 | 43692040222 | 132744546331 | 2026-11-15 07:54:40 | `464c8aa931fd3393fa38ff2849ca7f7f6471dd37` | Updated `docs/build-status.md`; documentation only |
| #40 | 43692237398 | 132745133634 | 2026-11-15 07:57:21 | `af0f61642618535dcbf4da9844d98f1a2ea00ecc` | Added 67 lines to `SourceRequestFactory.kt`: request-settings adapter; not compiled |
| #41 | 43692238962 | 132745139181 | 2026-11-15 07:57:23 | `195bbc68f8e87aadbec57b2dcedab87370af4eb1` | Added 69 lines to `SourceRequestFactoryTest.kt`; not executed |
| #42 | 43692247438 | 132745165041 | 2026-11-15 07:57:30 | `d3dd0e38d289f07a053b43f1d85b26b5a19bb96b` | Updated `docs/build-status.md`; documentation only |
| #43 | 43692403344 | 132745639136 | 2026-11-15 07:59:44 | `790b8e3b6201af22d9c50ea79cd3a1a3cd71f65a` | Added 42 lines to `SourceRuleResolver.kt`: stage-specific rule resolver; not compiled |
| #44 | 43692404569 | 132745642998 | 2026-11-15 07:59:45 | `60423cbadf6f558cda1d0ab5237bb0b43d241c97` | Added 54 lines to `SourceRuleResolverTest.kt`; not executed |
| #45 | 43692408759 | 132745656271 | 2026-11-15 07:59:49 | `63c3d1948c395cd08a535bf2133a31e880f74f5c` | Commit message “Document source rule routing progress”; GitHub commit file list empty; no file diff to validate |
| #46 | 43692553188 | 132746051142 | 2026-11-15 08:01:27 | `b8c6b500361ed77816551373d55658a59b7b021f` | Added pinned Jsoup selector dependency in `integration-contract/build.gradle.kts`; not resolved |
| #47 | 43692555431 | 132746058179 | 2026-11-15 08:01:29 | `63b0b2406c4cee70d0ff6106ad9e13512339f604` | Added 90 lines to `CssRuleEvaluator.kt`: bounded CSS selector evaluator; not compiled |
| #48 | 43692558035 | 132746066185 | 2026-11-15 08:01:31 | `cf561539a0f30a70f75004023ab9c8403c0c59cf` | Added 70 lines to `CssRuleEvaluatorTest.kt`; not executed |
| #49 | 43692562113 | 132746078805 | 2026-11-15 08:01:34 | `edb680afc48f7ad6552e87b1175d77c3d3c9087f` | Adjusted relative CSS URL attribute resolution in `CssRuleEvaluator.kt`; not compiled |
| #51 | 43693173554 | 132747892373 | 2026-11-15 08:09:24 | `fe26ed0534908e90b194ee6f3c8b8c8df7a04317` | Added 78 lines to `RuleExpressionTokenizer.kt`: top-level operator tokenizer; not compiled |
| #52 | 43693175454 | 132747898302 | 2026-11-15 08:09:25 | `8e3924597be5c25343a21e1ac5d63583615bf3a1` | Added 51 lines to tokenizer boundary tests; not executed |
| #53 | 43693183263 | 132747922194 | 2026-11-15 08:09:32 | `e2bcd9bc78808a6bbce9a5e872d449cfac21428d` | Updated `docs/build-status.md`; documentation only |
| #55 | 43693220663 | 132748039785 | 2026-11-15 08:10:02 | `f04be9218384cc85f51222a33f8c35ee5928e74f` | Updated `RuleExpressionTokenizer.kt` to detect first top-level operator; not compiled |
| #56 | 43693227903 | 132748059960 | 2026-11-15 08:10:06 | `2e81566736ada88d05c453e32e56131ab125a930` | Added 40 lines to `RuleCombinationEvaluator.kt`; not compiled |
| #57 | 43693230058 | 132748066909 | 2026-11-15 08:10:08 | `adb215e83695b0a5c7beb9bb52712c585181718a` | Added 47 lines to `RuleCombinationEvaluatorTest.kt`; not executed |
| #60 | 43693254971 | 132748141826 | 2026-11-15 08:10:26 | `81de89e5a8065d61b77a3f665187863f0b2d2c61` | Updated `docs/build-status.md`; documentation only |
| #61 | 43693265634 | 132748174514 | 2026-11-15 08:10:34 | `45ff92f4fa920b2c5840985f10ab25d3a536d34f` | Updated `CssRuleEvaluator.kt` for rule combinations; not compiled |
| #62 | 43693270000 | 132748187689 | 2026-11-15 08:10:37 | `f0a1f61fe47af1c71f5e89de148c60b3a7a70030` | Added CSS rule-combination integration tests; not executed |
| #65 | 43693385283 | 132748548136 | 2026-11-15 08:12:07 | `fcf3ae3c0e1db56514cc0bef77bfe773421008e3` | Aligned tokenizer balanced-group handling; not compiled |
| #66 | 43693397577 | 132748585632 | 2026-11-15 08:12:16 | `bcc2e7d96e72bc0775759117a7aac788cb4e54f1e` | Added upstream JSONPath balancing tests; not executed |
| #68 | 43693462528 | 132748791107 | 2026-11-15 08:13:08 | `30b9ae6f8a4164dd63f7c22e8989ffeaa49da43c` | Added 37 lines to `JsonPathRuleEvaluator.kt`; not compiled |
| #69 | 43693465043 | 132748799308 | 2026-11-15 08:13:10 | `d15a159e3697f4b0b026b1bd558c95d174c8699b` | Added 34 lines to `XPathRuleEvaluator.kt`; not compiled |
| #78 | 43693653537 | 132749377609 | 2026-11-15 08:15:34 | `0a8566b17062e1499e53c64e1da0f48e5022d81d` | Updated `docs/build-status.md`: parser vs runtime boundary; docs only |
| #79 | 43693716758 | 132749560818 | 2026-11-15 08:16:20 | `126053edaf8ece6bfd46c137bf561345d16de63a` | Added selector-type dispatch to `SelectorRuleEvaluator.kt`; not compiled |
| #80 | 43693719632 | 132749569474 | 2026-11-15 08:16:22 | `0f8ec545a86ffcdfc61bade1b48a0468743fab53` | Added selector dispatch tests; not executed |
| #81 | 43693892133 | 132750102646 | 2026-11-15 08:18:37 | `ccbe4e1416ae5089bd99a5d0ba23058bf83f43df` | Updated `SourceRuleResolver.kt` nested source-rule field dispatch; not compiled |
| #82 | 43693904847 | 132750143761 | 2026-11-15 08:18:48 | `739174aa03324ceaf4bfebf9b64fc4a956270ccf` | Added nested rule field dispatch tests; not executed |
| #83 | 43693925472 | 132750205645 | 2026-11-15 08:19:05 | `18c1529105f4c419d69ce148022599bcc968cfbc` | Added 62-line selector-only runtime adapter; this is a historical prototype and conflicts with current no-selector-only hard gate; not compiled |
| #84 | 43693940679 | 132750251574 | 2026-11-15 08:19:17 | `0cec0ea160ed7b04b93f3bbd6efc978700633c72` | Added selector-only adapter tests; not executed |
| #85 | 43693977271 | 132750360007 | 2026-11-15 08:19:44 | `d91a0d8ec3a4db5081223f404dc6cf14d2633458` | Added pinned coroutines runtime dependency for suspend adapter tests; not resolved |
| #86 | 43693996286 | 132750417191 | 2026-11-15 08:19:58 | `4e12dfa5f4950594c148ad825572b3c0b3973ed3` | Updated `docs/build-status.md`; documentation only |
| #87 | 43694016266 | 132750472883 | 2026-11-15 08:20:12 | `8f7ae70a2f3e00416738f33572c97746da0a0de6` | Added source-stage rule execution entry point to `SourceRuleResolver.kt`; not compiled |
| #88 | 43694031586 | 132750518684 | 2026-11-15 08:20:22 | `1c23492f80c65bdd5ea413aa38ba3aa5235efb40` | Added nested source stage dispatch tests; not executed |
| #89 | 43694055714 | 132750590473 | 2026-11-15 08:20:40 | `75cdfde420f1f0a28e9fb3ffc177115249c4005b` | Updated `docs/build-status.md`; documentation only |
| #90 | 43694153991 | 132750888063 | 2026-11-15 08:21:56 | `603640059cc95ed6804bedc9dacc5d034f4f8e1c` | Added pinned OkHttp transport dependency; not resolved |
| #91 | 43694194415 | 132751010701 | 2026-11-15 08:22:26 | `3d35a3fbea1f2e23d84458852a70d7ea5766a3bf` | Added 185-line source-scoped OkHttp transport and cookie jar; not compiled |
| #92 | 43694214756 | 132751074125 | 2026-11-15 08:22:43 | `bdfedce773e0e900146cfb5c3b289a204e855858` | Added source-isolated cookie persistence/matching tests; not executed |
| #93 | 43694283288 | 132751278566 | 2026-11-15 08:23:34 | `43a610e7bb188a6fd2d8d625b6ae729352244924` | Updated `docs/build-status.md`; documentation only |
| #94 | 43694295174 | 132751315374 | 2026-11-15 08:23:42 | `8ea7b28d1081fb867a933c84ac8b4dd066d9bd4f` | Small SourceRuntime transport boundary adjustment (3 additions/3 deletions); not compiled |
| #97 | 43694569670 | 132752148234 | 2026-11-15 08:27:08 | `e853e14862698cdb4a1c36bc9c68524b6d6a434e` | Added 197-line deterministic Legado URL option resolver; not compiled |
| #98 | 43694571894 | 132752154716 | 2026-11-15 08:27:10 | `286056c6417c43515bae553e5668d61232771b16` | Added URL resolver option parsing tests; not executed |
| #99 | 43694598095 | 132752237512 | 2026-11-15 08:27:31 | `0090913cf508704f372c35c4203d2957be3eb2d9` | Updated `docs/build-status.md`; documentation only |

## 2. Common Job/Step evidence and actual coverage

The inspected Jobs have the same step state pattern:

- Step 1 Set up job: success.
- Step 2 Check out source: success; checkout SHA matches the exact SHA in §1.
- Step 3 Set up JDK 21: success.
- Step 4 Set up Android SDK: failure.
- Step 5 Install Android platform and build tools: skipped.
- Step 6 Set up Gradle: skipped.
- Step 7 Run contract unit tests: skipped.
- Step 8 Assemble debug APK: skipped.
- Step 9 Upload debug APK: skipped.
- Post JDK / Post checkout / Complete job: cleanup success.

For the failed Step 4, the direct log evidence is `Warning: Failed to find package 'tools'` and `sdkmanager ... failed with exit code 1`. The runner also reports a wrong version in the preinstalled sdkmanager. This is a workflow environment failure.

**What each commit actually achieved at this SHA:** the commit diff above was added to the repository. That is the only CI-proven result. Production source additions/edits (source metadata contract, codec mapping, cookie defaults, source runtime adapter, request factory, rule resolver, CSS/JSONPath/XPath evaluators, tokenizer, rule combination, selector dispatch, OkHttp transport and URL resolver) were not compiled. Test files were not executed. Documentation commits only changed documentation. No commit in this group has a CI-proven Runtime feature pass.

## 3. Functional classification

- Source metadata contract / rule metadata / cookie defaults: code was committed; compilation and tests not executed.
- Source runtime adapter / request settings / stage-specific rule resolver: code was committed; no contract tests executed.
- CSS selector / JSONPath / XPath / tokenizer / rule combination / selector dispatch: code and some fixtures were committed; no evaluator or fixture test ran.
- Selector-only runtime adapter (#83–#84): historical prototype only; not validated and not accepted by current Runtime-3.0 no-selector-only hard gate.
- OkHttp source transport / source-isolated cookie jar: code and tests were committed; neither compiled nor executed.
- Legado URL option resolver: code and tests were committed; neither compiled nor executed.
- No real book source, Android Runtime E2E, Rhino/JsExtensions execution, login/Cookie/session, search/TOC/content, API28/35 instrumentation, Debug APK or DEX was tested in any of these 53 runs.

## 4. Reconciliation

- Actual candidate runs: 53.
- Workflow results: 0 success / 53 failure / 0 pending.
- Failure point: all 53 failed at Android SDK setup Step 4.
- Direct common root: obsolete/missing SDK package `tools`; `sdkmanager` exit code 1.
- Gradle/test/compile/APK/source steps: all skipped.
- JUnit/APK artifacts: none produced by these runs.
- The source-code changes in this range are historical implementation attempts, not validated features. Their existence must not be confused with successful functionality.
- The selector-only prototype in #83 is explicitly not accepted under the current hard requirements.
- No product acceptance or Legado compatibility claim is made.
