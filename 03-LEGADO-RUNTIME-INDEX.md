# 第三类 Legado Runtime CI 总索引

> 凡历史 CI、代码改动、测试或验收与第三类能力有关，全部纳入，不因旧分类或旧名称排除。按原始 CI 编号/历史顺序登记；每册最多 200 个纳入条目。

## 第三类范围
1. 模型/书源兼容：BookSource、Book、BookChapter、ReadConfig 序列化、导入导出。
2. 规则引擎：AnalyzeRule/AnalyzeUrl、CSS、XPath、JSONPath、正则、集合/标量、变量与替换。
3. Rhino/Legado JS：java.*、java.ajax、URL/卡片脚本、jsLib/mainJs、JS 扩展、隔离与生命周期。
4. HTTP/URL/Cookie/登录态：编码、重定向、headers、cookies、TLS、状态码、重试、超时。
5. 搜索发现：GET/POST、bookList、字段规则、分页。
6. 书籍详情：元数据抽取、详情脚本、目录 URL 解析。
7. 目录：多页目录、章节 URL、排序、去重、增量更新。
8. 正文阅读：正文提取、内容脚本、上下章、解码、替换及搜索→详情→目录→正文完整链路。
9. WebView/Android 桥：异步桥、验证码/登录页面交互、API 级别差异。
10. 缓存生命周期：写入、恢复、失效、清理、异常恢复。
11. Android 集成：接回 Badcat、保留既有 UI、JVM/模拟器/API/安装运行/端到端验收。
12. 导入诊断与回归夹具：书源导入兼容、分阶段错误、fixture 与回归。

## 每个 CI 的现行标注字段
- 原始 CI 编号、第三类子类、纳入理由。
- GitHub Actions Run ID、Attempt、workflow、触发事件、时间、URL、结论。
- Commit SHA、分支。
- 每个 Job/Step 的状态；明确成功、失败、跳过。
- 实际跑通的功能/API/调用链（写明输入、处理、输出，不能只写测试通过）。
- 测试类/用例/fixture、设备与 Android API 级别、命令。
- 失败日志、未执行步骤；区分应用缺陷与上游 403/404/429/5xx、TLS、超时。
- 书源探测/搜索/详情/目录/正文分阶段结果。
- 编译、单元、集成、Android/API、真实端到端分层验收。
- 日志、artifact、测试报告、代码路径及复核日期。
- 最终状态：已核实通过 / 部分通过 / 失败 / 运行中 / 待核查 / 缺证据。

没有证据的字段必须写“待核查”，不得推断。编译通过或单一 JVM 测试通过不等于端到端通过。

## 分册规则
- 本索引记录总进度；每册最多 200 个第三类纳入序号。
- 0001–0200：03-LEGADO-CI-0001-0200.md；0201–0400：03-LEGADO-CI-0201-0400.md；后续每册递增 200。
- 分册序号不是 GitHub run number；每项必须保留原始 CI 编号。

## 历史回填基线（来自此前交接，尚未本次逐条取证）
- #3200–#3225：26 条曾登记，现行字段/证据待复核。
- #3300–#3349：50 条曾登记，现行字段/证据待复核。
- 合计 76 条历史登记线索；不得直接视为已核实通过。
- #3226–#3299：74 条待逐项检查并判断是否第三类。
- #3350–#3357：8 条待补证。
- #3358–#3399：42 条待确认是否纳入。
- #3129 及更早历史：必须继续向前回溯；CI #3156 的旧交接状态曾为运行中，需核实最终结果。

## 当前总状态
- 第三类范围已明确；历史全量回溯未完成。
- 76 条有历史登记线索，逐条现行证据复核尚未完成。
- 先审计第三类历史 CI，不推进非第三类工作。
- 真实逻辑与端到端调用链优先，不以编译通过代替验收。
## 2026-11-24 历史回溯补充
- 已从 GitHub Actions 实际 job/step 与日志回填早期 Run #43705149699（SDK setup 失败）、#43707974288（contract tests + pinned Rhino restore + APK build 成功）、#43719899764（另含 bounded Rhino replacement host tests，成功）。三者已分别标记失败/部分通过/部分通过；均未证明真实书源端到端。
- 新增 CI 实现提交归档：0178e2c、c2055f5、f9f2e6c、8848c89、2583c81、37a24fd、504cd0d、563a6bd；具体功能与状态见 03-LEGADO-CI-0001-0200.md。
- 当前第三类纳入序号 0001–0142 有历史/本轮登记线索；其中 0077–0079 是已核查 job/step 的 GitHub Actions run，0080–0090 是已检查代码/测试/文档的历史提交。0001–0076 仍待按同一证据标准逐条复核。
- 下一步继续回溯更早的提交和 Actions 记录；不得把历史摘要中的 CI #3200 等编号与 GitHub Actions Run ID 混用。
## 2026-11-24 第二轮历史回溯补充
- 第三类纳入序号已扩展至 0142；分册 0001–0200 当前有 142 条登记线索。
- 0077–0079：3 条真实 GitHub Actions run，已核对 job/step 与部分原始日志；1 条 SDK 环境失败、2 条部分通过；未有真实书源端到端通过。
- 0080–0090：11 条更早的书源字段映射、规则路由、规则 tokenizer/组合行为实现/测试/文档提交。测试提交对应 combined status 与 PR-triggered workflow 查询均为空，因此标记缺证据/待核查，未记通过。
- 历史范围仍需继续追查；本轮 GitHub 仓库可检索历史目前追溯到项目早期的模型/规则基础提交，未找到历史交接中 CI #3129 等编号与 GitHub Actions Run ID 的映射关系，二者继续分开保存。

## 2026-11-24 第三轮历史补录
- 第三类纳入序号扩展至 0154；第一分册当前登记 154 条，剩余 46。
- 本轮补录 0143–0154：早期搜索服务、多页目录分页、CSS TOC 测试、持久化书源目录、搜索 URL 解析/测试、Android 开发阅读入口、search→detail→TOC→chapter fixture 测试、导入重复 ID 与字段类型校验。
- 上述提交的 combined status 均为空或未取得；测试代码与 fixture 链路不等于 CI 已运行，更不等于真实书源端到端验收。
- AGENTS.md 已重新读取；继续按其要求将搜索、详情、目录、正文分阶段核验，并区分应用缺陷与上游站点/网络故障。


## 2026-11-24 大规模回溯第四轮
- 第一分册已满：0001–0200，共 200 条。
- 本轮补录 0155–0200 共 46 条，按 GitHub commit created_at 顺序；范围包括 JSONPath/XPath 求值与组合边界、Legado URL 参数编码、regex deadline、书源导入去重/严格 UTF-8/5 MiB 限制、搜索 fixture 接线、章节安全 WebView 与阅读进度同步。
- 本轮检索 20 组关键词，初步得到 368 个去重提交候选；仅对其中按历史时间靠前且与第三类直接相关的 46 条完成本册登记，剩余候选继续去重、审 diff、分册登记。
- 新增记录未发现真实书源端到端验收通过证据；无 Actions 证据处保持待核查/缺证据。Run #43719899764 与已有纳入序号 0079 交叉引用，不重复当作新的 run 通过记录。
- 下一分册：03-LEGADO-CI-0201-0400.md，序号从 0201 开始。


## 2026-11-24 大规模回溯第五轮
- 第二分册已创建：03-LEGADO-CI-0201-0400.md。
- 新增 0201–0230（30 条），补回 platform-neutral ReadingEngine contract、CSS selector evaluator、source-scoped OkHttp/Cookie、详情服务、Android composition root、TOC/详情 URL 安全校验、详情→目录→正文本地 fixture。
- fixture 使用 fake HTTP，不能等同真实书源或 Android 设备端到端；无 Actions 证据处保持待核查/缺证据。
- 本轮另有 15 组检索、228 个去重命中；后续重点核查 TOC 安全/分页、Cookie 持久化与 Rhino bridge、mainJs/jsLib、PPRule、规则引擎和真实 source smoke。
- 本批为历史补录，部分提交日期早于第一分册末尾；纳入序号维持唯一追加，真实 commit 时间单独保留。需要继续制作全量时间线校正表。


## 2026-11-24 Actions 深度核查补充
- 新增 8 条独立 Actions 运行记录：原始 CI/run_number #3350–#3357，第三类纳入序号 0231–0238；每条保留 Run ID、commit SHA、Job ID、时间和 workflow URL。
- 8/8 assemble-debug jobs success；第三类 runtime unit suite、authentic Legado runtime model contract tests、RuleAnalyzer Android instrumentation、CookieManager API 28、WebView provider API 35、APK assemble/DEX RuleAnalyzer class verification steps 均 success。
- 8 次日志均确认笔仙阁 GBK POST search→detail→TOC AJAX→chapter content 与品书斋 search→detail→1650-chapter TOC→chapter content 两条指定 live smoke PASS。#3350 artifacts 已检查，包含 13.45 MB APK、unit test reports、API28/API35 RuleAnalyzer instrumentation reports、JS ABI/dependency closure 与 live source smoke reports。
- 同一公共源 sweep 中仍存在站点 403/444/404、TLS/timeout、redirect loop 和空结果；这些被分阶段记录，不归因于引擎功能，也不把单个 PASS 泛化为所有书源兼容。
- 第二分册目前登记 38 条（0201–0238）；继续大规模关键词与 CI 历史回溯。
