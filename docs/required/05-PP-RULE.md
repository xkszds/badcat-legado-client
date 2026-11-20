# 第五类必读清单：Badcat 原版 PPRule（小说/漫画/视频）

> 开始第五类任务：读总流程 [00-MASTER-WORKFLOW.md](00-MASTER-WORKFLOW.md) + 本文件。若同时改阅读器交互，再读第二类。PPRule evidence 是按需查阅的证据资料。

## 目标和边界

恢复 Badcat 原版 PPRule 数据格式、导入导出、规则分派和小说/漫画/视频执行。PPRule 与 Legado ReplaceRule、Badcat AdMark/AdBlock 独立，不能互相替代。

## 已确认 wire schema

PPRule：ruleType（RE=0、XPATH=1、UNRECOGNIZED）、pageRule、nextPageRule、pageJs、chapterIsReverse、videoPathRule、videoEpisodeRule、pageJsForSingleHtml。

PPConfigItem：listRule、contentRule、wap2PcRule、pc2WapRule、dataType、bookNameRule、authorRule、isNotUseReferer。PPResponse：code、message、version、ppItems、scriptCode、scriptSignature、ppRuleVer、bookDetailScript、jsLibScript。

DEX constants 与 assets/trans.proto 交叉验证；样本含 1,583 PPConfigItem，既有 XPath 规则，也有省略 ruleType（默认 RE=0）的记录。不能把 pageRule 一律当 CSS。

## 已有局部实现（不等于完整 TransEngine）

- 有 bounded BadcatPpRuleProtoDecoder：解码 PPResponse/PPConfigItem/PPRule 八个字段。
- 未知 wire fields 原始片段保留；message/string/item/field size、varint、UTF-8 有边界校验。
- 有 regex scalar 提取（capture group 1）、XPath 首节点提取（meta content / 其他 text）和 collection selection。
- 有 chapterIsReverse helper、两阶段 video path/episode selector helper。
- 有视频 HTTP href 解析/标题回退/去重。
- 有漫画 data-original 优先、src 回退、相对 URL、安全 scheme 和去重 helper。
- 对应 protobuf、畸形输入、未知字段、RE/XPath、video/comic helper 有测试记录。

以上仅证明 decoder/helper 的局部能力，不代表原版完整分派/执行。

## 未完成/待证

pageJs/pageJsForSingleHtml 的原版 JS 执行；nextPageRule 语义（现有 DEX consumer 证据不足）；完整小说/漫画/视频 TransEngine dispatch、优先级和失败回退；漫画分页/排序/图片加载/UI；视频 path/episode 分组、播放地址/集数切换/UI；PPResponse 更新/版本/签名/备份；规则管理 UI/持久化/导入导出。

## 实施顺序与验收

1. 完成 protobuf 全字段/默认 enum/未知字段 round-trip fixture。
2. 完成 DEX 方法级调用图和 dataType dispatch 证据。
3. 分别实现小说、漫画、视频执行链。
4. JS 有沙箱、超时、取消和资源上限。
5. 实现分页、排序、去重、URL、图片/播放流程。
6. 完成规则管理、版本迁移、UI 和持久化。
7. 原版可观察行为/授权样本/Android 真机验收。

未确认的服务协议和字段语义必须标记待证，不得猜测。

**当前状态：部分实现，未验收；完整 dispatch、JS、pagination 和 UI 未完成。**
