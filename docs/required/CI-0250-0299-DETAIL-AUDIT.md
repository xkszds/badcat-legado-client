# 第三类历史 CI 详细审计：#0250–#0299（50 次运行）

- Repository: xkszds/badcat-legado-client
- Branch/event: main / push
- Workflow: Android debug build
- Scope: #250–#299 inclusive; 50 real runs.
- Outcome: 0 success / 50 failure / 0 pending.
- All 50 jobs fail at Android SDK setup Step 4. Platform install, Gradle setup, contract tests, Debug APK assembly and upload are skipped. Logs sampled at #250 and #300 show sdkmanager package tools missing, after preinstalled sdkmanager version mismatch; the step exits code 1. No JUnit/APK/DEX/instrumentation/source-smoke evidence is produced.
- The exact source/docs changes below were committed, but none was compiled or tested by these CI runs.

## Per-run identity and commit diff

| CI | Run ID | Job ID | Created UTC | Exact HEAD SHA | Actual change |
|---:|---:|---:|---|---|---|
| #250 | 43699774743 | 132768147190 | 2026-11-15 09:32:58 | fc75b8f1acca892e88da48eb38aa5e35aefbe279 | Added AnalyzeUrl JS compatibility gate documentation |
| #251 | 43699778269 | 132768157637 | 2026-11-15 09:33:00 | 305ca8f1d26811a57348dfced31f2f98e989c78d | Updated build-status docs for AnalyzeUrl audit |
| #252 | 43699924825 | 132768607709 | 2026-11-15 09:34:49 | 8fc681e9593dcba81bee3b56dba69dcc16a1781d | Added isolated Legado embedded URL template evaluator (81 lines) |
| #253 | 43699927477 | 132768616514 | 2026-11-15 09:34:51 | c94e5abab1c8e2a8cf89411a112de05bf514355a | Added embedded URL template compatibility tests (67 lines); not run |
| #254 | 43699965749 | 132768724986 | 2026-11-15 09:35:16 | 391cba4b8b411e0d1c85e31e5d1c562ffe6218c7 | Changed empty embedded expression semantics in template evaluator |
| #255 | 43699969391 | 132768735884 | 2026-11-15 09:35:18 | b7399cddfc16e956b9c63a10bf227c3835c39f24 | Updated template tests to use existing coroutine core; not run |
| #256 | 43699998837 | 132768825505 | 2026-11-15 09:35:40 | 7031c0abcda44df680449ed44e5e1a0624fcdfc6 | Added explicit numeric conversion branch in embedded template evaluator |
| #257 | 43700001634 | 132768834327 | 2026-11-15 09:35:42 | 17ab69f7ebcf48ad099f4ecb6ac8e276ac3f293e | Corrected empty template replacement expectation in tests; not run |
| #258 | 43700028556 | 132768918955 | 2026-11-15 09:36:02 | 4f5f3dcca8e0b76303bbd5c7207bc719eee8292a | Documented isolated embedded URL template helper |
| #259 | 43700031246 | 132768927613 | 2026-11-15 09:36:04 | a7ca5f999605b93e5296d979c28b54d20d71720b | Updated build-status docs for embedded template helper |
| #260 | 43700057836 | 132769008457 | 2026-11-15 09:36:25 | 5b1482e6d75dfa53e6b69685a661bbe09e57fcdd | Preserved literal text around empty embedded results |
| #261 | 43700059525 | 132769013339 | 2026-11-15 09:36:26 | 6074ffda9e732e72fc4375b033765be4f0bcae0d | Added upstream empty-result prefix-retention test; not run |
| #262 | 43700070807 | 132769048428 | 2026-11-15 09:36:34 | 2e92e32b7ef81de5204369ad68355a3a20322708 | Clarified upstream empty expression semantics in docs |
| #263 | 43700072693 | 132769054222 | 2026-11-15 09:36:36 | 1febb77ba12192df983db3313a96bf1fee1e6bce | Updated build-status docs for empty expression semantics |
| #264 | 43700663682 | 132770860482 | 2026-11-15 09:44:11 | 697c4dff1f3445178017700f07e5eda31cc1ec2d | Added Legado query/form parameter encoder (138 lines) |
| #265 | 43700688357 | 132770933458 | 2026-11-15 09:44:30 | 738c22168f51b0a8ac07a618989aa5c801ff27b3 | Aligned escape and query encoding semantics |
| #266 | 43700696173 | 132770958450 | 2026-11-15 09:44:36 | 82055152c8449f96a7298e2cae28bcee6120ad2e | Added URL parameter encoder regression tests (55 lines); not run |
| #267 | 43700716214 | 132771019393 | 2026-11-15 09:44:51 | fc52f144866cf444fd8b9571de130d3721d298d2 | Corrected URL encoder test vectors; not run |
| #268 | 43700804257 | 132771283493 | 2026-11-15 09:45:53 | 6368d9983098d3d411ea9ff8256dcb4f23bdb1f8 | Test-vector commit; GitHub commit file list empty, no code diff |
| #269 | 43700817969 | 132771325790 | 2026-11-15 09:46:04 | 5bdc90c3ace28bfb252a5c938c59a3301832e504 | Repaired URL encoder regression vectors; not run |
| #270 | 43700830038 | 132771364554 | 2026-11-15 09:46:13 | c2bc7dfc81ecb9ce1353a99eed163e1f01731d6c | Updated AnalyzeUrl URL parameter encoding gate docs |
| #271 | 43700837403 | 132771387671 | 2026-11-15 09:46:19 | 5e5a6c18d96975dc75fba6a9b6dd57c1ef6a4144 | Updated build-status docs for URL parameter encoder |
| #272 | 43700878245 | 132771517706 | 2026-11-15 09:46:50 | 5034aa370b9662dcf98b0e5c48df75e4a1299e50 | Fixed URL encoder character-set construction |
| #273 | 43700927535 | 132771671838 | 2026-11-15 09:47:27 | baa580bec1ec4ecf211ea0beccd14fc111317feb | Corrected UTF-8 query encoding test expectation; not run |
| #274 | 43700949309 | 132771738350 | 2026-11-15 09:47:44 | 884b7c75ea07acfe742ac78ab85470bd98548555 | Updated build-status docs for isolated Kotlin encoder |
| #275 | 43701332945 | 132772907006 | 2026-11-15 09:52:30 | d012fb2384647c5ab6246345b234508d3a33196b | Repaired URL encoder character sets and upstream branching |
| #276 | 43701357553 | 132772980371 | 2026-11-15 09:52:49 | 53ac1083d87f267ede0d4b6c7243210f2d112190 | Matched Legado query PercentCodec path |
| #277 | 43701423186 | 132773183436 | 2026-11-15 09:53:39 | e9b695bb5d1037b97e40d386863743f809ef4abf | Added method-aware URL and form preparation (37 lines) |
| #278 | 43701436001 | 132773221560 | 2026-11-15 09:53:48 | e451cf3f36802e8b8374765f52a7b28f35d165e8 | Wired query/form preparation into SourceRequestFactory |
| #279 | 43701461603 | 132773298470 | 2026-11-15 09:54:07 | 668dee64b9a19ce93702b441f639713ff11f6d2e | Added GET query / POST form preparation tests (52 lines); not run |
| #280 | 43701494303 | 132773405489 | 2026-11-15 09:54:34 | 00c0a4d7cd00485d8a5fde46292f28f1d3740622 | Corrected upstream URL-safe backslash character set |
| #281 | 43701510804 | 132773456702 | 2026-11-15 09:54:47 | ae9f93aa29917fc8fdaab3a566e25e945412e95b | Added raw URL query encoding before URI resolution |
| #282 | 43701519313 | 132773483390 | 2026-11-15 09:54:53 | 0cc5cf50c45f618bb5b583b41fa5cdd321512368 | Removed premature prepared-URL validation from SourceRequestFactory |
| #283 | 43701547486 | 132773568305 | 2026-11-15 09:55:14 | b91b148adc7af923280f34ca893bc16af8e63e4e | Aligned encoded-query allowlist with Legado NetworkUtils |
| #284 | 43701555667 | 132773593665 | 2026-11-15 09:55:20 | 4fd14f60eaf4b4206427fd7b890a29e31e78c296 | Added raw Unicode query handling tests; not run |
| #285 | 43701668494 | 132773939656 | 2026-11-15 09:56:48 | 6619f2fcb4d3d7c496205c4b8344b721d88dfa2e | Documented static query/form request integration |
| #286 | 43701679667 | 132773975266 | 2026-11-15 09:56:57 | c6fa6f6b0e7ded518f80bb02bd9871365524dd45 | Updated build-status docs for static URL request preparation |
| #287 | 43702117964 | 132775302401 | 2026-11-15 10:02:25 | 94fcff31a55ca632f03677f20f6687902cc8b983 | Wired opt-in embedded URL templates into SourceHttpGateway |
| #288 | 43702121854 | 132775313902 | 2026-11-15 10:02:28 | 00e114ccc0f335d2d207672d6f5593fc19c4440c | Added bounded URL template gateway integration tests; not run |
| #289 | 43702128706 | 132775334912 | 2026-11-15 10:02:34 | 91ddda95d98ccc48d5e4085c7a9504e3edb77152 | Added bounded Rhino URL template evaluator adapter (22 lines) |
| #290 | 43702166804 | 132775448950 | 2026-11-15 10:03:01 | 545224c1547be84915bd236b466a19411e3f7a82 | Passed search keyword into opt-in URL template evaluation |
| #291 | 43702169834 | 132775457988 | 2026-11-15 10:03:03 | f9aba0017c9ac9d7ca618838b483a38c672c4e1f | Forwarded search keyword to SourceReadingEngine |
| #292 | 43702196149 | 132775537121 | 2026-11-15 10:03:22 | ee7947d42a970ca7fbab4cee99055381c219adf9 | Preserved upstream string conversion for primitive template results |
| #293 | 43702216706 | 132775601156 | 2026-11-15 10:03:37 | 5f96f11d3cd95fa38b444d37c29784718fd3cb8e | Updated build-status docs for bounded URL template gateway |
| #294 | 43702219630 | 132775610939 | 2026-11-15 10:03:39 | 241b69dc788505d904f835477107543fb4945f32 | Documented bounded AnalyzeUrl template integration gate |
| #295 | 43702839115 | 132777496794 | 2026-11-15 10:11:10 | 87e2c5fcaa19574a289237cdc7cf89842c2ced77 | Added Android source HTTP gateway factory (31 lines) |
| #296 | 43702849737 | 132777527655 | 2026-11-15 10:11:18 | 8d077573bca4a32a0bfbbd81ff93c16bfa67ad3a | Documented Android gateway composition entry point |
| #297 | 43702955825 | 132777847908 | 2026-11-15 10:12:36 | 14a96c5f795bc3577df2fe7a60e88444cb888625 | Added bounded BookSource search URL resolver (43 lines) |
| #298 | 43702965348 | 132777877034 | 2026-11-15 10:12:42 | b8e64fcd6a14a9e17618a865dc15b372dcad9bd1 | Added bounded source search URL resolution tests (54 lines); not run |
| #299 | 43702977187 | 132777912645 | 2026-11-15 10:12:51 | ec0b4a222eaa033bb3d7ad107b138a581e0f72c7 | Documented concrete bounded search URL resolver |

## Functional coverage and hard boundary

The implementation attempts in this range form these groups:
1. **Rhino/AnalyzeUrl embedded URL template** (#250–#263, #287–#294): primitive expression substitution, empty expression semantics, source-isolated Rhino evaluator, search keyword forwarding and bounded template gateway.
2. **Legado URL query/form compatibility** (#264–#286): query percent encoding, GET query parameters, POST form bodies, raw Unicode handling, allowlist and URL resolution.
3. **Android source gateway and source search URL** (#295–#299): Android HTTP gateway factory and bounded source search URL resolver.

**None of these changes was compiled or tested.** The SDK setup failure occurs before Gradle. Tests in #253, #255, #257, #261, #266–#269, #273, #279, #284 and #288 were not executed. No evidence proves URL template evaluation runs in Rhino, query/form requests match Legado NetworkUtils, or Android source search works.

## Reconciliation

- 50/50 workflows failure at Android SDK setup Step 4.
- Gradle, contract tests, compile, Debug APK, DEX, instrumentation and live-source steps skipped.
- No JUnit/APK/DEX/source artifacts.
- All code changes are historical implementation attempts; not accepted Runtime features.
