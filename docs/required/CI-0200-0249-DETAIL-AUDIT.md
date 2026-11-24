# 第三类历史 CI 详细审计：#0200–#0249（50 次运行）

- Repository: xkszds/badcat-legado-client
- Branch/event: main / push
- Workflow: Android debug build
- Scope: #200–#249 inclusive; 50 real runs.
- Outcome: 0 success / 50 failure / 0 pending.
- Shared failure: all 50 jobs fail at Android SDK setup Step 4. Steps 5–9 (platform install, Gradle, contract tests, Debug APK, upload) are skipped. The inspected #200 and #250 log samples show preinstalled sdkmanager version mismatch, missing package tools, and sdkmanager exit code 1. No Gradle/JUnit/APK/DEX/instrumentation/source-smoke evidence is produced.
- The exact code/docs changes below were committed at the recorded SHA, but the Android workflow never compiled or tested them.

## Per-run identity and commit diff

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Actual change |
|---:|---:|---:|---|---|---|
| #200 | 43698404923 | 132763913198 | 2026-11-15 09:15:59 | 1f274600b995ca8ab3afdf5200c941a6fef6aabb | Updated LegadoUrlResolverTest: unsupported URL method should be a rule failure; test not run |
| #201 | 43698408186 | 132763923358 | 2026-11-15 09:16:02 | 751b4c3322e3bc145d44b0c3f3b7f552db46765e | Updated build-status docs: URL method error classification |
| #202 | 43698847188 | 132765255889 | 2026-11-15 09:21:18 | 84d51f1db31e2710c72988123cba56995444ac55 | Added 42-line pinned Rhino integration requirements audit |
| #203 | 43698850018 | 132765263761 | 2026-11-15 09:21:20 | 8cabbde3f963f9bb4d7c0f524e62ac0f1a4fc532 | Updated build-status docs: Rhino audit registration |
| #204 | 43699016471 | 132765771734 | 2026-11-15 09:23:21 | 69f01f708ee4f2c393b7147a67b681141e09ed47 | Expanded pinned Rhino module dependency/source inventory by 41 lines |
| #205 | 43699019838 | 132765781776 | 2026-11-15 09:23:23 | c4affdc32f75e9f00d0d1be68931b6b34089a3dd | Updated build-status docs: Rhino dependency inventory |
| #206 | 43699155646 | 132766208624 | 2026-11-15 09:25:06 | fdf5a7b208fcc48d0f8a1422003c3717abd69435 | Added Rhino CompiledScript Kotlin runtime source (19 lines) |
| #207 | 43699159036 | 132766218956 | 2026-11-15 09:25:08 | daf378289148a1aca5996cd3dfb5ab2a9d08cae8 | Added Rhino ScriptBindings source (165 lines) |
| #208 | 43699163516 | 132766232122 | 2026-11-15 09:25:11 | 2efc5e5f83dbfd93809d7e28e2dc3e4a0990496b | Added ScriptBindingsExtensions source (14 lines) |
| #209 | 43699166547 | 132766241188 | 2026-11-15 09:25:14 | 95f5f0cb1a66cfe184ae6c062ddac55c1d920b8a | Added Rhino ScriptException source (54 lines) |
| #210 | 43699170354 | 132766252486 | 2026-11-15 09:25:17 | c9014fae111a3e8bf9284938be9161a0c42776ac | Added CatchableNativeJavaObject Rhino source (214 lines) |
| #211 | 43699173839 | 132766263393 | 2026-11-15 09:25:19 | ed6cd4ac8fe620d59e43f851be4d601649748ba6 | Added ClassNameMatcher source (41 lines) |
| #212 | 43699178379 | 132766277378 | 2026-11-15 09:25:23 | c6195a2fd144ff115f8e375d97def4f894c1e336 | Added Rhino CollectionExtensions source (22 lines) |
| #213 | 43699181527 | 132766287938 | 2026-11-15 09:25:25 | 3758cd43060c7ca33d2dcd1c773141885bfa604e | Added JavaObjectWrapFactory source (10 lines) |
| #214 | 43699185153 | 132766299719 | 2026-11-15 09:25:28 | 979d62b14c75567d3fd745d89631b622a18af3c5 | Added ProtectedNativeJavaClass source (51 lines) |
| #215 | 43699188716 | 132766310546 | 2026-11-15 09:25:30 | 48db664b68906e9e13fcd7f0cd23d92b24eaf993 | Added ReadOnlyJavaObject source (43 lines) |
| #216 | 43699212554 | 132766386973 | 2026-11-15 09:25:49 | efeccb8742acebc2108e947a97d4aa2bbd70418a | Added RhinoClassShutter source (191 lines) |
| #217 | 43699216560 | 132766398803 | 2026-11-15 09:25:52 | e0d830497465d0a9ebaa943d036717a8b30d9d54 | Added RhinoCompiledScript source (83 lines) |
| #218 | 43699220434 | 132766411552 | 2026-11-15 09:25:55 | 2e027a03c7fef2bb66f02bb5615e944ad005616a | Added RhinoContext source (354 lines) |
| #219 | 43699223145 | 132766420720 | 2026-11-15 09:25:57 | 9a5915c788e299473a70a713828d18b14f1d9ee0 | Added RhinoContextElement source (32 lines) |
| #220 | 43699227232 | 132766434034 | 2026-11-15 09:26:00 | c4ead5fc60252c377ae0f3593d434b5769e0693e | Added RhinoErrors source (5 lines) |
| #221 | 43699235141 | 132766458213 | 2026-11-15 09:26:06 | 69f91a6d4ca7cca5e0e71fe59ff666ffbf08b30b | Added RhinoExtensions source (57 lines) |
| #222 | 43699239467 | 132766471677 | 2026-11-15 09:26:10 | df4d9319afae8b226f2a0a8fd3b64d5794e108b8 | Added RhinoScriptEngine source (360 lines) |
| #223 | 43699242769 | 132766482991 | 2026-11-15 09:26:12 | 23b5a34d4249fcea25b604bcce263e395fe3f5a4 | Added RhinoWrapFactory source (126 lines) |
| #224 | 43699245569 | 132766492059 | 2026-11-15 09:26:14 | e383678a9667d01629fe80dc995adcfb829de29d | Added GPL-3.0 license for vendored Rhino module (674 lines) |
| #225 | 43699266597 | 132766560035 | 2026-11-15 09:26:32 | 59a7a8891063e013e1c043f5e5c47f5db876feda | Added Rhino module include to settings.gradle.kts |
| #226 | 43699268998 | 132766567630 | 2026-11-15 09:26:34 | a7e31f9cfbeaf87fd6c66a936c5074fd2538567a | Declared Android library plugin for Rhino module |
| #227 | 43699271075 | 132766574871 | 2026-11-15 09:26:35 | 58f4d3d6b9d4bb561ab380e11e7204c4ded0539f | Added Rhino module dependency to app Gradle |
| #228 | 43699273327 | 132766581533 | 2026-11-15 09:26:37 | fc8769b3dbcb1a45efd5ea81419190ec4ada4193 | Added Android library build configuration for legado-rhino |
| #229 | 43699275301 | 132766588033 | 2026-11-15 09:26:39 | 11938d88431a357f2678a61d6a550055409d97b0 | Added consumer ProGuard rules for Rhino module |
| #230 | 43699309051 | 132766694074 | 2026-11-15 09:27:04 | 8ef3ac9d3cc68bcecda16e9bf1a841076637557e | Added Rhino provenance/integration README (18 lines) |
| #231 | 43699311467 | 132766702092 | 2026-11-15 09:27:06 | 0cb8d3e6da0e8768ef2f628e5f7e7c50cfa00e0f | Added Rhino third-party attribution notice (12 lines) |
| #232 | 43699315004 | 132766713494 | 2026-11-15 09:27:08 | 9657818396a295f0ed08270b589cdf76a779e0a3 | Updated build-status docs for vendored Rhino module |
| #233 | 43699371885 | 132766899658 | 2026-11-15 09:27:54 | 9cd0b98dd4fe1a2078b0bef903fb3ec1605d2e22 | Added upstream Apache license notice (202 lines) |
| #234 | 43699374804 | 132766909078 | 2026-11-15 09:27:56 | 9778d23100a4e44d5efd628f53ed32e7f0111b9c | Added core-js license notice (6 lines) |
| #235 | 43699377783 | 132766919407 | 2026-11-15 09:27:59 | cf65b27edfbcf228ac4f3b2abde768eeecb37ff9 | Added upstream license file (375 lines) |
| #236 | 43699382523 | 132766933351 | 2026-11-15 09:28:02 | 599ba6945ba5d230e348412f7a45b782d2059bc5 | Added upstream tools notice (37 lines) |
| #237 | 43699386371 | 132766945079 | 2026-11-15 09:28:05 | a6fc61b07a144140bdab0d5fd6a85806daee174d | Added upstream notice file (59 lines) |
| #238 | 43699389884 | 132766955773 | 2026-11-15 09:28:08 | dfcd39c8f713d04c17121b2cd5fe4b6a84683858 | Added upstream source provenance file (35 lines) |
| #239 | 43699394247 | 132766969888 | 2026-11-15 09:28:11 | 45031e51e6800edc3ac159e8b9429e41c9552859 | Added Rhino source diff/provenance note (251 lines) |
| #240 | 43699417042 | 132767042236 | 2026-11-15 09:28:30 | e478e6ef05553d0445bcbcd4f75ce50ac1bf784f | Updated THIRD_PARTY_NOTICES for HtmlUnit Rhino dependency |
| #241 | 43699420074 | 132767052381 | 2026-11-15 09:28:32 | 23559f44decd1340da32fb4d55df18f83f3eb8b5 | Updated build-status docs for Rhino module |
| #242 | 43699559684 | 132767491722 | 2026-11-15 09:30:21 | 6a8d0be853086d1b536e4dfcb9cd11e2d0e38ab2 | Added local Maven repository for pinned Rhino fork |
| #243 | 43699563304 | 132767502844 | 2026-11-15 09:30:24 | 5f439f6f469eb6d2782907f1e1b9091277844e6b | Added upstream POM metadata for pinned Rhino engine |
| #244 | 43699565508 | 132767509550 | 2026-11-15 09:30:25 | dce3a2cec41e73522fe94f1a26556e33aed63cf2 | Documented exact missing custom Rhino binary dependency |
| #245 | 43699589226 | 132767579103 | 2026-11-15 09:30:42 | 661da1db0d178e10246a3f99c71ae9ab56cb2ec5 | Added 71-line bounded primitive-only Rhino sandbox bridge |
| #246 | 43699592296 | 132767587905 | 2026-11-15 09:30:44 | 5401cc0e180e78c33331f8e5d24a75a823e3d2b9 | Updated README with exact upstream custom Rhino JAR identity |
| #247 | 43699621382 | 132767674661 | 2026-11-15 09:31:05 | 63c42171ff7535712f4f00c116a0b066ccf2a99d | Added 51-line Android adapter for bounded Rhino execution |
| #248 | 43699624628 | 132767685345 | 2026-11-15 09:31:08 | 4db1679cc34e0c7d46d21a0ee2992091d53be770 | Documented bounded sandbox and Android host adapter |
| #249 | 43699632142 | 132767707481 | 2026-11-15 09:31:13 | b4842a91ee701b59f3f56107c6ac3c61c3f9eba1 | Documented bounded Rhino bridge and missing fork binary blocker |

## Functional coverage and hard boundary

The code-change clusters in this range are:
1. URL resolver error classification (#200–#201).
2. Pinned Rhino dependency/source audit (#202–#205).
3. Vendored Rhino runtime/security source files (#206–#223), including bindings, wrap factory, class shutter, context, and script engine.
4. Rhino module Gradle wiring and consumer ProGuard (#224–#229).
5. Third-party license/provenance inventory (#230–#244).
6. Primitive-only sandbox bridge and Android adapter (#245–#249).

**None of these was compiled or tested by these CI runs.** The Android SDK setup failed before Gradle. The Rhino code files and Android adapter are committed source only; the license/provenance files are documentation/compliance changes only. No evidence proves Rhino engine starts, sandbox restrictions work, imported BookSource executes, JsExtensions are exposed, or a book source search/TOC/content path runs.

## Reconciliation

- 50/50 workflows failed at Android SDK setup Step 4.
- 50/50 Gradle/test/APK/source steps skipped.
- No JUnit/APK/DEX/instrumentation/live-source artifacts.
- These are third-category implementation attempts and dependency/provenance records, not passed runtime features.
- No product acceptance or complete Legado compatibility can be claimed.
