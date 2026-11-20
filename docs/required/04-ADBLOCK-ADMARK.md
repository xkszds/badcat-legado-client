# 第四类必读清单：Badcat AdBlock、AdMark 与 WebView 广告净化

> 开始第四类任务：读总流程 [00-MASTER-WORKFLOW.md](00-MASTER-WORKFLOW.md) + 本文件。若改到阅读器 WebView 生命周期，再读第二类。原版净化 evidence 是证据资料，不是例行必读。

## A. 第四类框架：大类 / 小类 / 完成情况

> 本节只建立与第三类相同的分层导航和完成情况索引。下方原有目标、原版证据、实现记录、安全要求、登记和历史记录均保留，不改写其原文。状态只表示已有证据中的实现进度，不代表分类验收。

### 大类 0：原版证据与规则语义
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 0.1 原版类/方法定位 | DEX 类、方法和设置键 | 已确认 ADBlockUtils、AdBlockPresenter、AdMarkDao、AdMarkHttpApi、AdMarkService、SettingAdBlockActivity 及相关方法/键 | 部分证据已确认 |
| 0.2 AdMark 数据结构 | protobuf family、DbAdMark 字段 | 已记录 Issued/Report、AdMarkItem/AdMarkRuleItem 和 DbAdMark 字段 | 部分证据已确认 |
| 0.3 URL matcher 语义 | urlPattern JSON integer array 到 Pattern matcher | 已确认路径模式拼接及 Pattern.matcher(url).find() 行为 | 已有静态证据；实现有局部覆盖 |
| 0.4 页面端协议 | getInfoFromJS、onDeleteAd 和 DOM 删除 | 已确认桥接调用名称和部分调用关系 | 未完成；页面脚本/DOM 删除算法未恢复 |

### 大类 1：AdBlock 设置与注入入口
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 1.1 开关与脚本设置 | key_is_ad_block、key_ad_block_js_inject | 已有独立开关和有界 injection script 设置 | 部分实现 |
| 1.2 注入生命周期入口 | Reader WebView page-finished hook | 已有 page-finished 注入选择 hook | 部分实现 |
| 1.3 脚本来源与更新 | 原版脚本恢复、更新与可信来源 | 已有本地脚本设置；原版完整脚本/规则更新尚缺 | 未完成 |
| 1.4 注入失败处理 | 诊断、回退和不影响正文 | 已明确注入失败不得影响正文的安全要求 | 未验收 |

### 大类 2：URL 匹配与安全隔离
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 2.1 Host 匹配 | 精确 host 与域名边界 | 已有 exact-host matcher | 局部测试/覆盖待完整核验 |
| 2.2 URL Pattern 匹配 | 已验证 urlPattern 数组解析与匹配 | 已有 URL-pattern matcher 和单条 payload helper | 部分实现 |
| 2.3 来源策略 | HTTP(S)、本地页面、malformed/credential URL | 已记录 .invalid synthetic origin 拒绝策略 | 部分实现 |
| 2.4 页面隔离 | 禁止向小说本地正文页面注入网页脚本 | 已有 .invalid 隔离规则和生产调用路径审查记录 | 部分实现；真实 Provider 验证待完成 |
| 2.5 资源/执行限制 | 来源、长度、时间、资源上限 | 已明确安全边界要求 | 未完成完整验证 |

### 大类 3：AdMark 数据与规则管理
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 3.1 本地目录 | AdMark catalog CRUD | 已有 app-private catalog 的 upsert/remove | 部分实现 |
| 3.2 导入导出 | JSON 导入/导出和规则保留 | 已有本地 import/export | 部分实现 |
| 3.3 规则语义 | host、urlPattern、markRules 的独立保留与执行 | 已有字段独立保留和 matcher；markRules 精确执行语义未确认 | 部分实现 |
| 3.4 服务端更新 | 授权服务协议、版本和更新流程 | 已确认存在 API/Service 符号，但没有授权协议和完整实现依据 | 未完成 |

### 大类 4：页面 JavaScript Bridge 与 DOM 删除
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 4.1 getInfoFromJS | 页面读取 URL-pattern list | 已确认方法名/用途线索 | 未完成页面端真实实现 |
| 4.2 onDeleteAd | 接收 markRule payload 并执行删除 | 已确认单条 markRule JSON payload helper 和原版调用线索 | 未完成页面端真实实现 |
| 4.3 DOM 规则执行 | markRules 匹配目标元素并删除 | 尚无已验证的 selector/DOM 算法 | 未完成；禁止猜测实现 |
| 4.4 Bridge 生命周期 | 页面切换、异步回调和资源释放 | 已有过期 page-finished callback 隔离修复 | 部分实现；异步回调与资源释放仍待验收 |

### 大类 5：用户标记与设置界面
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 5.1 长按标记 | 页面长按、生成标记信息和确认 | 已确认原版 WebView bridge handler 接收 JsAdMarkInfo、用户确认后保存的静态证据 | 未完成新客户端交互闭环 |
| 5.2 确认/取消 | 用户确认、取消及保存结果 | 已记录原版确认后保存流程线索 | 未完成新客户端交互闭环 |
| 5.3 白名单/黑名单 | 站点允许/拒绝设置与管理 | 已列为未完成项 | 未完成 |
| 5.4 设置页 | AdBlock/AdMark 设置与规则管理 UI | 已有设置开关/脚本设置基础 | 未完成完整设置页 parity |

### 大类 6：测试、真机与分类验收
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 6.1 JVM 契约测试 | 当前/过期/空白/null URL identity | 已有 3 项 JVM 测试覆盖当前、缺失/空白和不同 URL 不归一化 | 局部测试通过记录 |
| 6.2 Android WebView Provider | 真实 Provider、API 35、JS bridge 和页面回调 | 已登记真实 Provider instrumentation；既有运行因 page-finished latch timeout 失败，后续 CI 仍待终态 | 阻断中 |
| 6.3 跨域/重定向 | 来源变化、页面切换和注入隔离 | 已列入真机验证要求 | 未完成 |
| 6.4 原版闭环验收 | 真实网页标记、确认、删除和恢复 | 已有局部模型/入口，不具备完整闭环证据 | 未验收 |


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


## 2026-11-21 Fourth-2.6 registration — AdMark import/export fail-closed contract

- Starting branch/HEAD: `integration/legado-upstream-runtime` / `f38b2006704c5670500c4477a594250f2ecab899`.
- Scope: strengthen deterministic JVM contract coverage for the existing AdMark JSON codec's accepted input shapes and rejection behavior. This is a bounded subtask under 3.2 import/export and 6.1 JVM contract tests.
- Audited production paths: `BadcatPurificationActivity` routes pasted/file imports through `BadcatPurificationCoordinator.importAdMarks`; coordinator delegates to `BadcatAdMarkCatalog.importJson`; catalog decodes the full payload before merging and committing through Android `AtomicFile`. Export uses the same codec. The codec supports legacy flat arrays and the observed nested `markItems/ruleItems` shape, while deliberately ignoring server-supplied `adBlockJsInject`.
- Target file: `app/src/test/java/app/badcat/client/BadcatPurificationModelsTest.kt` only. No production source, persistence schema, UI, settings, network endpoint, or runtime injection behavior is in scope.
- Upstream/original reference: original Badcat AdMark DEX/protobuf shape recorded in `docs/badcat-original-purification-evidence.md`; no Legado SHA dependency applies.
- Data ownership/transaction: no changes. Existing app-private `AtomicFile` catalog remains the sole local AdMark record owner; import must decode/validate before the catalog merge/write path.
- Risks: malformed or duplicate entries must fail closed; import must not accept wrong JSON types or silently install server-provided scripts; test assertions must not invent unverified DOM/markRules semantics.
- Contract tests: nested and flat supported shapes; duplicate identity rejection; malformed group/rule/field types; invalid URL-pattern/rule values; imported payload containing `adBlockJsInject` must not expose or install that script; verify byte/item boundary guards where deterministic without allocating excessive memory.
- Acceptance: focused `:app:testDebugUnitTest` passes on exact implementation HEAD; full CI result must be recorded separately. This only validates import/export contracts and does not complete AdMark service sync, DOM deletion, or original purifier parity.
- Exclusions: no new format fields, no guessed server protocol, no DOM selectors, no remote script acquisition, no settings/UI changes, no Android Provider or device claim.


### Fourth-2.6 implementation checkpoint

- Test-only implementation is committed in `app/src/test/java/app/badcat/client/BadcatPurificationModelsTest.kt` at `e41fbe48810522cb02ec8e0a6280b4702c463feb`.
- Added four contract cases: duplicate identity rejection across nested/flat representations; malformed nested field/type/rule rejection; byte-limit rejection before JSON parsing; and proof that server-supplied `adBlockJsInject` is not imported or exported.
- No production code, settings, persistence, UI, network, or DOM behavior changed.
- Exact-HEAD CI run #3073 / ID `44822313338`, HEAD `e41fbe48810522cb02ec8e0a6280b4702c463feb`: job `assemble-debug` exists; setup steps succeeded; `Run contract unit tests` is currently `in_progress`. Job-log download currently returns BlobNotFound, so no test result is claimed yet.
- Acceptance remains pending until the focused/full unit-test step and workflow reach terminal status. This checkpoint does not claim AdMark category acceptance.


### Fourth-2.6 CI result update

- Exact implementation HEAD: `e41fbe48810522cb02ec8e0a6280b4702c463feb`; Actions run #3073 / ID `44822313338`.
- Verified from the job-step API: `Run contract unit tests` completed `success`; `Run authentic Legado runtime model contract tests` completed `success`; pinned JS API inventory generation/upload completed `success`.
- The overall workflow is still `in_progress`; dependency-closure generation is the active step at the latest poll. Therefore this is a focused unit-test pass, not a terminal CI result or category acceptance.
- Job-log download still returned BlobNotFound; the step API is the evidence for the completed test step.
