# Runtime-3.2f.45：四个公开书源 Android 真实运行时测试结果

- 日期：2026-11-24
- 分支：`integration/legado-upstream-runtime`
- 测试 SHA：`3daebdec11e426c24376a9e603a0daee70e724f2`
- 固定 Legado 上游：`330e606c4d57e08adc056ff8ab7a4132c784da9a`

## CI 结果

| Run | Run ID | Job ID | Exact SHA | 结论 |
|---|---:|---:|---|---|
| #3239 | `45208786938` | `137811633095` | `269ef42590d60f817071ba2f7951f40cbc9f2257` | failure |
| #3240 | `45209463958` | `137814017415` | `3daebdec11e426c24376a9e603a0daee70e724f2` | failure |

#3240：`:app:assembleDebug` 成功；API 28 既有 CookieManager 仪器步骤成功；API 35 既有 WebView provider 四项观察均为 `ok=true`，但新增 E2E 测试失败。TestRunner：5 tests, 1 failed, 0 ignored。

## API 35 四源报告

报告从 artifact `14837045578` 的 `adb-legado-e2e-report-logcat.txt` 还原。诊断 ZIP SHA-256：`6d944cb70520919e3d3fc394e0e2d1adf929a4a5ec706fe755dd8399ba5f0047`。独立 E2E report artifact 未生成；上传步骤使用 `if-no-files-found: warn`，不能把 action success 当成报告存在。

| 书源 | 导入 | 搜索及后续 | 结论 |
|---|---|---|---|
| 唐三中文 | pass，63 ms | search 失败，22,117 ms，`network_transport_dns_tls_or_timeout` | 网络传输超时；无后续阶段 |
| 笔趣阁fun | pass，133 ms | 安全预检阻断未审查混淆动态 JS | blocked；没有执行源脚本 |
| 精华书阁 | pass，216 ms | 搜索前 `NoClassDefFoundError` | 宿主 APK 缺少 pinned RuleAnalyzer 定义 |
| 零零小说 | 未运行到 | 无阶段记录 | undetermined / not executed |

本轮 0/4 E2E pass。不得把网络超时、安全阻断、宿主类缺失和未执行混称为书源兼容性失败。

## RuleAnalyzer 打包证据

API 35 Logcat 异常：`NoClassDefFoundError: io.legado.app.model.analyzeRule.RuleAnalyzer`，调用链为 `PinnedLegadoRuleAnalyzer.split` → `CssSearchResultsParser` → `SourceSearchService.search`。

Debug APK artifact ID `14837280203`；上传 ZIP SHA-256 `b14c837d4afa588ddf62e2da2918196b9a9a7df788336bfc98a6bfb87010e7f8`；APK 32,024,734 bytes，SHA-256 `21c2689f7b649299391700cdcd93fe29429414fea33fa8449ea34e69dcd5f781`。检查 APK 的 14 个 DEX class-definition 表：存在 `app.badcat.engine.PinnedLegadoRuleAnalyzer`，但不存在 `io.legado.app.model.analyzeRule.RuleAnalyzer` 的 class definition。字符串表符号引用不代表类已打包。

`integration-contract/build.gradle.kts` 声明自定义 `pinnedLegado` source set、`implementation(pinnedLegado.output)` 和 `jar { from(pinnedLegado.output) }`；但最终 APK 缺类。当前证据尚不能区分 Kotlin source-set 编译、JAR 组装、Gradle variant 选择或 D8 输入环节，不能臆断具体根因。

## 结论

.45 已执行但验收失败。JSON fixture import 在 Android app sandbox 中成功；没有任何来源完成 Search → Detail → TOC → Content。Debug APK build 成功不是测试通过，也不是 release 或用户设备安装证明。

下一步单独登记 .46，追踪 pinned RuleAnalyzer 从 Kotlin 编译输出到 project JAR、Android runtime variant、D8 和最终 APK DEX 的完整链路；先修复类打包并用 DEX class-definition 检查和 API 35 设备端 Class.forName/analyzer smoke 证明。笔趣阁混淆 JS 安全阻断保持不变。