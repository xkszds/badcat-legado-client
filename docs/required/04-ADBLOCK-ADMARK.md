# 第四类必读清单：Badcat AdBlock、AdMark 与 WebView 广告净化

> 开始第四类任务：读总流程 [00-MASTER-WORKFLOW.md](00-MASTER-WORKFLOW.md) + 本文件。若改到阅读器 WebView 生命周期，再读第二类。原版净化 evidence 是证据资料，不是例行必读。

## 目标和边界

独立恢复原版 WebView 广告过滤、AdMark 标记/删除和设置流程。不能用 Legado 正文替换、普通 CSS 或 PPRule 替代。原 APK/DEX 只读；不得猜私有服务协议、伪造接口或执行未经验证的 JS。

## 已确认原版证据

- DEX symbols：ADBlockUtils、AdBlockPresenter、AdMarkDao、AdMarkHttpApi、AdMarkService、SettingAdBlockActivity。
- 相关方法/键：handleAdBlock、handleAdMark、handleInjectAdBlock、getAdBlockJsInject、key_ad_block_js_inject、key_is_ad_block、splashAdBlock。
- AdMark protobuf family：Issued/Report request/response、AdMarkItem、AdMarkRuleItem；线索字段 markItems、markRules、isFilter、isBlock、host、urlPattern。
- DbAdMark 字段：host、id、isServer、markRule、updateTime、urlPattern、version。
- URL matcher：urlPattern JSON integer array → 拼接路径模式 → Pattern.matcher(url).find()。
- WebView bridge handler `adMark` 接收 JsAdMarkInfo，空 markRule 被拒绝，用户确认后保存；保存成功后单条 markRule 包成 JSON array 调用 `onDeleteAd`。
- `getInfoFromJS` 用于读取 URL-pattern list；页面端完整脚本和 DOM 删除实现未恢复。

## 已有实现记录（非完整复刻）

- 独立开关 key_is_ad_block 和有界 injection script 设置 key_ad_block_js_inject。
- Reader WebView page-finished 有注入选择 hook；仅开关启用且 script 非空时考虑注入。
- 独立 app-private AdMark catalog：本地 upsert/remove/import/export。
- host、urlPattern、markRules 独立保留；exact-host + 已验证 URL-pattern matcher。
- 有单条 markRule JSON payload helper。
- Legado HTTP/Cookie/Rhino/正文替换保持独立。
- reader.invalid synthetic origin 的 apex/subdomain 已有拒绝规则记录。

## 未完成

原版完整注入脚本/规则更新；页面端 getInfoFromJS/onDeleteAd 和 DOM 删除；markRules 精确执行语义；设置页和白名单/黑名单；页面切换/资源释放；真机 WebView callback、跨域/重定向和注入隔离。私有服务仅在有授权、真实协议证据时接入。

## 安全和验收

默认关闭远程注入；脚本有来源/域名策略、长度/时间/资源上限；不执行未验证 markRules；不猜 CSS selector 或 querySelector 删除逻辑；注入失败可诊断且不影响正文。测试覆盖 protobuf/JSON、matcher、确认/取消、callback payload、DOM 行为、页面隔离和真机流程。

**当前状态：部分实现，未验收；原版 page script 与 DOM 删除闭环缺失。**

## 2026-11-21 Fourth-2.4 registration — stale WebView callback isolation

- Starting branch/HEAD: `integration/legado-upstream-runtime` / `4238d86069f7373b2ba2172d90a0d1bdbf529794`.
- Scope: harden the existing Badcat WebView injection adapter against stale `onPageFinished` callbacks and asynchronous JavaScript callbacks after navigation; do not broaden which origins are trusted.
- Audited production caller: `SourceChapterActivity` in `app/src/main/java/app/badcat/client/SourceBookActivity.kt`; its WebView uses `loadDataWithBaseURL` with `https://reader.invalid/<version>/` and renders locally generated chapter HTML. No production remote-page WebView caller was found in the current app source tree.
- Target files: `app/src/main/java/app/badcat/client/runtime/BadcatPurificationCoordinator.kt`; add real-provider instrumentation coverage under `app/src/androidTest/kotlin/app/badcat/client/runtime/`; update this checklist with exact results.
- Upstream reference: original Badcat 1.5.0.5 DEX evidence recorded in `docs/badcat-original-purification-evidence.md`; no Legado upstream dependency applies to this category.
- Data ownership: no persistence or schema changes. Existing settings store remains the sole owner of `key_is_ad_block` and `key_ad_block_js_inject`; AdMark catalog remains unchanged.
- Security constraints: preserve rejection of `.invalid`, local/non-web schemes, malformed URLs and credential-bearing URLs; never inject a stored script into the synthetic chapter document; do not infer `markRules` selectors or implement guessed `getInfoFromJS`/`onDeleteAd` callbacks.
- Contract tests: deterministic JVM tests cover stale/current/null/blank URL identity before any injection decision. Real-provider coverage is deferred until the existing Runtime-2.3 WebView harness is repaired: CI run #3059 at the starting HEAD failed because the existing `loadDataWithBaseURL("https://localhost/")` page-finished latch timed out (0/1 instrumentation test completed at first poll, then 1 failure); the report artifact was not produced. Do not claim provider instrumentation passed. The future provider test must prove stale callback rejection, current eligible HTTP(S) injection, `.invalid` isolation and JavaScript-setting restoration.
- Acceptance for this bounded implementation: focused JVM contract tests pass on exact HEAD; provider instrumentation remains a blocking follow-up and this task cannot be marked fully accepted until that harness runs. This task only closes a lifecycle race and does not complete original AdBlock/AdMark behavior.
- Exclusions: remote script acquisition, private `adMark/issued` or `adMark/report` service calls, page-side DOM deletion, original long-press marking UI, allowlist/denylist settings, and full original purifier parity.


## 2026-11-21 Fourth-2.5 registration — Actions run queue / missing job records

- Starting branch/HEAD: `integration/legado-upstream-runtime` / `6946e17dbd19f51b591c6bf475e8db67a2c1224c`.
- Scope: diagnose and remove the workflow-level concurrency bottleneck that leaves the newest push run in `pending` with `jobs=[]`; preserve independent verification for every pushed commit. This is CI orchestration only.
- Evidence: run #3060 (`44818416936`, HEAD `2d5fb2d626a2b2cc25a5a84945f6d37fe7746605`) remained `in_progress` in step 21, “Run real Android WebView provider instrumentation (API 35)”. Run #3061 (`44818546681`) and #3062 (`44818572207`) were cancelled; run #3063 (`44818593204`, exact HEAD `6946e17dbd19f51b591c6bf475e8db67a2c1224c`) remained `pending` with zero jobs. All share the same branch-level concurrency group.
- Root cause: `.github/workflows/android-build.yml` uses `group: badcat-android-build-${{ github.ref }}` with `cancel-in-progress: false`. GitHub Actions concurrency permits one running and at most one pending run per group; newer pending runs replace/cancel older pending runs, and a new run cannot create its job until the running group member releases the slot. The comment claiming this preserves every pushed commit is therefore incorrect.
- Target files: `.github/workflows/android-build.yml` and this checklist only.
- Change: make the concurrency group unique per commit SHA (retain ref plus SHA) so each push has its own run group; update the comment to describe the actual behavior. GitHub runner capacity may still queue jobs, but queued jobs should be represented in the run/job API instead of being hidden behind the same concurrency group.
- Data ownership/transactions: none; no app data, schema, runtime or product behavior changes.
- Contract/verification: inspect exact workflow diff; after push, verify new run is bound to exact resulting HEAD, run status is not blocked by the previous branch-wide group, and `jobs_url` exposes a job record. Confirm earlier #3060 is not cancelled by this change. Verify the prior #3063 state separately; do not treat pending or a job record alone as test success.
- Acceptance: orchestration fix is accepted only after a new exact-HEAD run exposes its job and reaches a terminal result. Android instrumentation, unit tests, APK and product acceptance remain separate gates.
- Exclusions: do not cancel or mutate run #3060; no test behavior, app source, emulator test, or runtime code changes.
