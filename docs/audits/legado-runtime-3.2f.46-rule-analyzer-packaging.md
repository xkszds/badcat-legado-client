# Runtime-3.2f.46：RuleAnalyzer Android 运行时变体打包诊断

- 日期：2026-11-24
- 分支：`integration/legado-upstream-runtime`
- 本轮起始 HEAD：`fb05284d711f3475f3f36010ed1a3336987651aa`
- 固定上游：`LegadoTeam/legado@330e606c4d57e08adc056ff8ab7a4132c784da9a`

## 已核实的失败证据

1. #3241 / run `45226036896` / job `137872657857`，exact SHA `54607dfb3aa0e6baa43c469dfdd3df71de917710`：`:integration-contract:verifyPinnedLegadoRuntimeJar` 成功；API 28 CookieManager 既有测试成功；API 35 新增 `PinnedLegadoRuleAnalyzerPackagingInstrumentedTest` 以 `ClassNotFoundException: io.legado.app.model.analyzeRule.RuleAnalyzer` 失败。
2. #3242 / run `45226425208` / job `137874040705`，exact SHA `027eb9f5f62d1c767b26adb5af8dcec3da2914bc`：Kotlin DSL 脚本编译失败，原因是把 `java.util.zip.ZipFile` 写在 Kotlin DSL 脚本中；该提交不是 Android 运行时验证结果。
3. #3243 / run `45226755463` / job `137875189373`，exact SHA `fb05284d711f3475f3f36010ed1a3336987651aa`：contract JAR 校验成功，API 28 和 API 35 仪器测试仍均因目标 APK 类加载失败而失败。
4. #3243 Debug APK artifact `14847140711` 下载后对其中全部 14 个 DEX 文件的 class_def 表进行检查：仅定义了 `app.badcat.engine.PinnedLegadoRuleAnalyzer`，没有定义 `io.legado.app.model.analyzeRule.RuleAnalyzer`。这与字符串引用/方法引用不同，证明类定义确实未进入目标 APK。

## 根因定位

原 .46 修复只把 `compilePinnedLegadoKotlin` 输出追加到 `:integration-contract:jar`。JAR 级检查通过，但 Android app 项目依赖所选用的运行时变体并未将该自定义 source-set 输出带入 DEX。因而“JAR 包含类”并不等于“Android runtime variant / APK 包含类”。

## 本轮最小修正

- 在 `:integration-contract` 的 `main` SourceSetOutput 中显式登记 `compilePinnedLegadoKotlin.destinationDirectory`，让 Gradle 的主输出/项目变体能消费该自定义编译输出，而不只是在 jar task 中追加。
- 在 `:app` 新增 `verifyPinnedLegadoRuleAnalyzerRuntimeClasspath`，直接扫描 Android `debugRuntimeClasspath` 的目录和 JAR，要求其中至少一个实际 artifact 定义完整类路径 `io/legado/app/model/analyzeRule/RuleAnalyzer.class`。
- 在 CI Android 仪器测试前运行该 runtime-classpath 验证；原有目标 APK Class.forName 仪器测试仍是最终设备验收，不被 classpath 检查替代。

## 本轮范围

只改动 `integration-contract/build.gradle.kts`、`app/build.gradle.kts`、`.github/workflows/android-build.yml`、本报告和第三类清单。未改生产 Kotlin 运行时行为、上游源代码、依赖版本、规则、书源夹具、UI、书架、阅读器或存储。

## 验收状态

本报告记录的是基于已下载 #3243 APK DEX class_def 的根因定位及下一轮验证方案。新修正尚未经过 CI，不能声称已修复。必须同时满足：contract JAR class check、Android debugRuntimeClasspath class check、API 28/35 仪器测试、最终 APK DEX class_def 和 Class.forName/真实 split 调用全部通过；否则 .46 保持未完成。


## Follow-up correction after #3244

- #3244 / run `45230159003`, job `137887209500`, exact SHA `1e57729b60ac5e95fd60da321aff12fd4782eeb8`, failed before Gradle task execution because Kotlin DSL resolves `java.util` as an unresolved script receiver in the unqualified expression `java.util.zip.ZipFile` in `app/build.gradle.kts`. No runtime classpath or emulator result was produced on that SHA.
- The next commit imports `java.util.zip.ZipFile` explicitly and uses `ZipFile(artifact)`, matching the working import pattern in `integration-contract/build.gradle.kts`. This is a build-script syntax correction only; it does not alter runtime behavior.


## Follow-up after #3245 — resolve Android classpath through AGP class-JAR artifact view

- #3245 / run `45230453615`, job `137888252481`, exact SHA `9b7c030ac9eb8deffe5d332c3ea5b49cb0c71b85`: the contract JAR verification passed, but the new app runtime-classpath guard failed before inspecting files. Gradle could not select among `:legado-runtime:debugRuntimeElements` artifacts because direct `Configuration.files` resolution did not request an artifact type; AGP exposed AAR metadata, art-profile, assets, classes-directory, classes-JAR and other variants as ambiguous.
- This is a validation-task artifact-selection bug, not new evidence that the RuleAnalyzer class is absent from the APK. The existing #3243 DEX class_def result remains the latest direct APK evidence.
- Narrow correction: request AGP's `android-classes-jar` artifact type through `debugRuntimeClasspath.incoming.artifactView`, then scan the resolved class JARs for the exact class definition. This makes the guard inspect the class-bearing artifacts rather than asking Gradle to materialize an unqualified multi-variant configuration.
- No new dependency, production Kotlin behavior, upstream source, source fixture, UI, reader, bookshelf or storage change.
- **Status:** correction committed for exact-SHA CI; not yet verified. APK DEX and API 28/35 class-loading/split tests remain mandatory.
