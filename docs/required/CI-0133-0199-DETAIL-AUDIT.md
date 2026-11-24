# 第三类历史 CI 补充审计：#0133–#0199（实际存在的 52 次运行）

- Repository: `xkszds/badcat-legado-client`
- Historical branch/event: `main / push`
- Workflow: `Android debug build`
- Fixed upstream SHA: `330e606c4d57e08adc056ff8ab7a4132c784da9a`
- Scope: actual candidate runs #133–#199 from `CI-0001-0199-AUDIT.md`; missing numbers are not fabricated.
- All 52 runs are attempt 1, workflow `completed/failure`. Each has one `assemble-debug` job. Step 1–3 succeeded, Step 4 “Set up Android SDK” failed, Steps 5–9 were skipped, and post-job cleanup succeeded.
- Logs sampled across the beginning, middle and end of the range (#133, #150, #177, #199) all show the same direct cause: preinstalled sdkmanager version mismatch; `Warning: Failed to find package 'tools'`; cmdline-tools/16.0 sdkmanager exit code 1. Job step matrices for all 52 runs confirm the same failed/skipped stage pattern.
- No Gradle/test/compile/APK/instrumentation/source step ran. No JUnit or APK artifacts were generated.

## 1. Per-run identity, exact SHA and code/document diff

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Commit change (diff inspected) |
|---:|---:|---:|---|---|---|
| #133 | 43695642819 | 132755437653 | 2026-11-15 08:40:57 | `6231fa94d697130f7a4f3f0af074d330d5aebc79` | Updated `docs/build-status.md`: chapter text purification hook status; docs only |
| #134 | 43695684145 | 132755565932 | 2026-11-15 08:41:30 | `9d81d0a1b0279fc80133cd770ff02389ba202527` | Renamed TOC/content pagination URL field in `ReadingEngine.kt`; not compiled |
| #135 | 43695700327 | 132755616063 | 2026-11-15 08:41:43 | `f464d3b2f48288c8bf4c93d97012a8691b7a2938` | Updated `SourceChapterContentService.kt` (+61/-19) for same-chapter content pagination; not compiled |
| #137 | 43695735057 | 132755722737 | 2026-11-15 08:42:11 | `541ade91b4b0a724ff3552c3788b89ad1761c016` | Added 59 lines of multi-page chapter/replacement hook tests; not executed |
| #138 | 43695754220 | 132755782773 | 2026-11-15 08:42:26 | `2ecf348c4e7645d9a880599e5f1028397faea67a` | Updated `docs/build-status.md`; docs only |
| #139 | 43695792839 | 132755900433 | 2026-11-15 08:42:56 | `df12d0f7d82cb436b825090f70fe2353ef73886e` | Added 87 lines to `SourceReadingEngine.kt`: source service orchestration adapter; not compiled |
| #140 | 43695804647 | 132755937300 | 2026-11-15 08:43:06 | `3163dffc046f08e8f32feb86a4ba68dbf2d8c833` | Updated `SourceReadingEngine.kt` (+8/-8) to preserve source catalog lookup failures; not compiled |
| #141 | 43695822107 | 132755994632 | 2026-11-15 08:43:21 | `4318e2d0490c07b715e563dabe6ab16d13078303` | Updated `SourceReadingEngine.kt` (+12/-3) for source engine lookup handling; not compiled |
| #142 | 43695837579 | 132756043335 | 2026-11-15 08:43:33 | `c87922caf07be4b3b0a2a80e81e4cb431578979e` | Commit message “Repair source engine Kotlin syntax”; GitHub commit file list empty, no code diff |
| #143 | 43695857055 | 132756103425 | 2026-11-15 08:43:48 | `3282295023d259a497ea6ead6ec33aa1d0a78f91` | Updated `SourceReadingEngine.kt` (+9/-22) to repair/simplify orchestration; not compiled |
| #144 | 43695872103 | 132756148552 | 2026-11-15 08:44:00 | `50cec0d84853859b8b356ae88dd347be3e93dfa9` | Added 52 lines to `SourceReadingEngineTest.kt`; not executed |
| #145 | 43695883405 | 132756183726 | 2026-11-15 08:44:09 | `6776659fda4f565271baeef681f7de0cf81b4703` | Updated `docs/build-status.md`; docs only |
| #146 | 43695900448 | 132756239434 | 2026-11-15 08:44:23 | `224cf6aee234ae17a02f9ff918c6778bcd95e983` | Changed one line in `MainActivity.kt` to update scaffold status/ReadingEngine adapter integration; not compiled |
| #147 | 43696328991 | 132757563158 | 2026-11-15 08:50:00 | `0d387c8428b3c929850b8538cc839c61b1e63bd5` | Added 112 lines to `FileBookSourceCatalog.kt`: durable source catalog; not compiled |
| #148 | 43696345474 | 132757610238 | 2026-11-15 08:50:10 | `9c86f3a93c852fee268f518f1802814da95cafcf` | Updated `docs/build-status.md`; docs only |
| #150 | 43696371091 | 132757687115 | 2026-11-15 08:50:28 | `c7902ae61699db6b60e8d8eec10031b170725414` | Updated `FileBookSourceCatalog.kt` (+4/-7) to deduplicate imported sources by URL; not compiled |
| #151 | 43696499494 | 132758097238 | 2026-11-15 08:52:10 | `aa53af446e6af822a988e7b40310d81528b111e8` | Added 165 lines to `SourceCatalogActivity.kt`: source catalog import/export screen; not compiled |
| #152 | 43696515983 | 132758147424 | 2026-11-15 08:52:22 | `2670c07a206d7699f1159f2b239ec273d37190e0` | Updated `MainActivity.kt` (+15/-1) to expose source catalog screen; not compiled |
| #153 | 43696521339 | 132758164379 | 2026-11-15 08:52:26 | `b2587a7bd6b3bedecdc0a05c40976103fef30e9b` | Registered source catalog Activity in AndroidManifest; not compiled |
| #154 | 43696530545 | 132758193547 | 2026-11-15 08:52:34 | `c58681404e5bbdd18c730756b9c373a0db28a9ff` | Updated `docs/build-status.md`; docs only |
| #155 | 43696646121 | 132758563207 | 2026-11-15 08:54:08 | `a6046cd48a835e8dfcd9c2178d068810fbce70b3` | Added 30 lines to `FileBookSourceCatalog.kt` to persist source enabled state; not compiled |
| #156 | 43696655549 | 132758593079 | 2026-11-15 08:54:16 | `c394155c5e1ee1a10cbc5924a4d24e49d9ea06b8` | Added 6 lines to `LegadoBookSourceCodec.kt`: lossless enabled-state codec helper; not compiled |
| #158 | 43696688606 | 132758697018 | 2026-11-15 08:54:42 | `5dc62176b5da4feca041d7597216f05f839925e8` | Updated `SourceCatalogActivity.kt` (+51/-19) for per-source enable/disable controls; not compiled |
| #159 | 43696705033 | 132758748105 | 2026-11-15 08:54:55 | `9675c444634937d605d9a138d07d11ece45b9df5` | Added 16 lines of lossless enabled-state tests; not executed |
| #160 | 43696722405 | 132758799713 | 2026-11-15 08:55:09 | `fdc52ce99cd0e2b3b094f3018d4569d36c713785` | Updated `docs/build-status.md`; docs only |
| #161 | 43696843621 | 132759178148 | 2026-11-15 08:56:46 | `bc290554db1e1a5e88ce59ac62585dd9ce29d7bf` | Added blank-query/disabled-source guards to `SourceReadingEngine.kt`; not compiled |
| #162 | 43696849027 | 132759194721 | 2026-11-15 08:56:50 | `2dfa7f14b37d80c60b173a055a24fa22271541d1` | Added 42 lines of disabled-source/blank-search tests; not executed |
| #163 | 43696857925 | 132759223419 | 2026-11-15 08:56:58 | `6666b92b3e0dbb66cbcca9c447115fd25c908031` | Updated `docs/build-status.md`; docs only |
| #164 | 43696953857 | 132759529888 | 2026-11-15 08:58:19 | `f67a0002b436274635cf5f4242c5e56f7f0b34a9` | Updated `SourceReadingEngine.kt` (+15/-12) to apply enabled-source policy across reading operations; not compiled |
| #165 | 43696961789 | 132759555615 | 2026-11-15 08:58:26 | `5d583e967be0f121e616eee4a00d502bb30ed2e1` | Added 21 lines of disabled-source operation tests; not executed |
| #166 | 43696968158 | 132759577566 | 2026-11-15 08:58:31 | `e6fcd24184395c0e4f28a93d72f230b553b67f74` | Updated `docs/build-status.md`; docs only |
| #167 | 43697132841 | 132760082328 | 2026-11-15 09:00:38 | `673a1114c1b530eae4df0ddaaa83846b06cf6fbf` | Added 25 lines of unsupported Legado URL script-mode tests; not executed |
| #168 | 43697140123 | 132760101682 | 2026-11-15 09:00:43 | `04ee3252e95f4ae574a69b7cc14155eef82e6f76` | Updated `docs/build-status.md`; docs only |
| #169 | 43697220818 | 132760335981 | 2026-11-15 09:01:37 | `8aa50744712e89b8d9661059ad90d7ef10bd6062` | Added 46 lines of disabled-source TOC/content gate tests; not executed |
| #170 | 43697226108 | 132760352453 | 2026-11-15 09:01:41 | `e3bb0bfc9f5f5863c16ae7ecc2e63c3a8f61757e` | Updated `docs/build-status.md`; docs only |
| #171 | 43697346408 | 132760722223 | 2026-11-15 09:03:10 | `84dbd628746cc30d5f0fc5557189dba8cae88260` | Updated `LegadoUrlResolver.kt` (+1/-3) to align static URL methods with supported HTTP transport; not compiled |
| #172 | 43697354507 | 132760745392 | 2026-11-15 09:03:15 | `69af34a360bd3df71770de7068ce4b48d97c603a` | Added 24 lines of static URL HTTP method compatibility tests; not executed |
| #173 | 43697361506 | 132760765572 | 2026-11-15 09:03:20 | `96ff5c754dd9b8d90616fe54bb4e232035392f78` | Updated `docs/build-status.md`; docs only |
| #174 | 43697580210 | 132761415763 | 2026-11-15 09:05:55 | `688f0f88a60fe337144f31589723441ee886dcc8` | Added 16 lines to `SourceRuntime.kt`: fail-closed script runtime when Rhino unavailable; not compiled |
| #175 | 43697585301 | 132761430684 | 2026-11-15 09:05:59 | `0033daa48cdd3240be7acb57e46cc8a93de5a2b3` | Added 18 lines of fail-closed script runtime tests; not executed |
| #176 | 43697589864 | 132761445269 | 2026-11-15 09:06:03 | `91a779d8245190ab8964fbd8a2cccdada19647a2` | Updated `docs/build-status.md`; docs only |
| #177 | 43697660397 | 132761665630 | 2026-11-15 09:06:56 | `5fe5af070597334ce89298b5b8528287432be1a0` | Updated `SourceRequestFactory.kt` (+13/-1) to validate HTTP methods; not compiled |
| #180 | 43697729641 | 132761874462 | 2026-11-15 09:07:46 | `d2eb16714bf7c5dad32accb36e513368ac5e66f8` | Added 30 lines to `SourceRequestFactory.kt` for URL/header validation; not compiled |
| #181 | 43697736981 | 132761898940 | 2026-11-15 09:07:52 | `f0ffb5e77d732149e56cbd56ac36b6679ac3b6ae` | Added 37 lines of URL/header validation tests; not executed |
| #184 | 43697763112 | 132761980221 | 2026-11-15 09:08:12 | `f57fc18072ced8706876c9f1a497a4f0753d42f2` | Updated `docs/build-status.md`; docs only |
| #188 | 43697924500 | 132762470141 | 2026-11-15 09:10:14 | `3646fd3ecbf769a83f5391524b0bdc82b7b2bc05` | Updated `LegadoUrlResolver.kt` (+24/-6) to fail closed on unsupported URL transport options; not compiled |
| #189 | 43697946405 | 132762534115 | 2026-11-15 09:10:29 | `d6865acc4912cd0f9079a511ed337a5f53976c52` | Added 46 lines of fail-closed URL option/origin validation tests; not executed |
| #190 | 43697957850 | 132762569086 | 2026-11-15 09:10:37 | `6afb4c75c23a541c8d164854e81557f9454e43ba` | Updated `docs/build-status.md`; docs only |
| #191 | 43698031681 | 132762788197 | 2026-11-15 09:11:29 | `5edcccc437904bde787c03781d1589d21981a98f` | Updated `LegadoUrlResolver.kt` (+6/-1) to reject unsupported escape response decoding; not compiled |
| #196 | 43698181588 | 132763238610 | 2026-11-15 09:13:18 | `9a4dda14fc5b368e2cd946c1ec68c1f0770eba10` | Removed two unresolved URL resolver metadata references; not compiled |
| #197 | 43698210464 | 132763326035 | 2026-11-15 09:13:39 | `fe7588a656242496cdb7bd7c182ed6f180f5b556` | Removed two unsupported transport metadata references from resolved URL contract; not compiled |
| #199 | 43698401662 | 132763903016 | 2026-11-15 09:15:57 | `2fd7301ee54a05f0f06f01eb5d7495367a8c7add` | Updated URL resolver (+1/-1) to classify unsupported URL methods explicitly; not compiled |

Run URL pattern: `https://github.com/xkszds/badcat-legado-client/actions/runs/<Run-ID>`.

## 2. Job/Step evidence and failure

For every run in §1:
- Step 1 Set up job: success.
- Step 2 Check out source: success; exact checkout SHA matches §1.
- Step 3 Set up JDK 21: success.
- Step 4 Set up Android SDK: failure.
- Step 5 Install Android platform/build tools: skipped.
- Step 6 Set up Gradle: skipped.
- Step 7 Run contract unit tests: skipped.
- Step 8 Assemble debug APK: skipped.
- Step 9 Upload debug APK: skipped.
- Post JDK / Post checkout / Complete job: cleanup success.

Representative exact logs from #133, #150, #177 and #199 all show:
- preinstalled sdkmanager reports a wrong version;
- `Warning: Failed to find package 'tools'`;
- cmdline-tools/16.0 sdkmanager exits with code 1.

The Job matrices for all 52 runs confirm the same failed/skipped stage pattern. The root cause is Android SDK setup/package configuration, not a demonstrated Runtime test failure.

## 3. Functional interpretation and hard boundary

These commits represent historical implementation attempts across:
- chapter pagination and chapter text replacement;
- source orchestration adapter and source catalog lookup error propagation;
- durable BookSource catalog, deduplication, import/export UI, app-shell/manifest wiring and enabled-state persistence/toggles;
- source enabled/disabled policy across search/TOC/content;
- unsupported URL script-mode handling and static HTTP method alignment;
- fail-closed script runtime when Rhino is absent;
- HTTP method, URL and header validation;
- fail-closed URL option/origin handling and unsupported response decoding.

**None of these source changes was compiled or tested by these 52 CI runs.** The added tests in #137, #144, #159, #162, #165, #167, #169, #172, #175, #181 and #189 were never executed. There are no JUnit reports, APKs, DEX reports, Android instrumentation reports or live-source results from this range.

Some commits cross the Badcat app-shell/source-management UI boundary (#146, #151–#153, #158), but they remain third-category related because they expose the source catalog/runtime integration. Their UI or persistence behavior is not CI-validated here.

The fail-closed script runtime (#174–#176) is not Rhino execution; it only describes behavior when Rhino is absent. The source catalog and source reading engine code are not proof of a complete Legado runtime. No Android Runtime E2E, imported BookSource execution, Rhino/JsExtensions, BaseSource lifecycle, login/Cookie/session, real search/TOC/content or complete compatibility was tested.

## 4. Reconciliation

- Actual runs: 52.
- Workflow results: 0 success / 52 failure / 0 pending.
- Common failure: Android SDK setup Step 4; missing `tools` package and sdkmanager exit code 1.
- Gradle/compile/test/APK/instrumentation/live-source steps: skipped.
- JUnit/APK/artifacts: none produced.
- All submitted code/test changes remain unvalidated at their respective exact SHAs.
- No product acceptance or full Legado compatibility claim is made.
