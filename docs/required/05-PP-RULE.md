# 第五类必读清单：Badcat 原版 PPRule（小说/漫画/视频）

> 开始第五类任务：读总流程 [00-MASTER-WORKFLOW.md](00-MASTER-WORKFLOW.md) + 本文件。若同时改阅读器交互，再读第二类。PPRule evidence 是按需查阅的证据资料。

## A. 第五类框架：大类 / 小类 / 完成情况

> 本节只建立与第三类相同的分层导航和完成情况索引。下方原有目标、wire schema、实现记录、未完成项、实施顺序和历史记录均保留，不改写其原文。状态只表示已有记录中的局部实现，不代表完整 TransEngine 验收。

### 大类 0：原版 PPRule 基线与协议
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 0.1 protobuf schema | PPRule、PPConfigItem、PPResponse 字段与 enum | 已记录主要字段、ruleType enum 和 response schema | 已有静态 schema 证据 |
| 0.2 原版样本 | 默认值、省略字段、XPath/RE 样本 | 已交叉验证 DEX constants 与 assets/trans.proto；样本含 1,583 PPConfigItem，并确认存在 XPath 与省略 ruleType 样本 | 部分证据完成 |
| 0.3 wire 兼容 | 未知字段、边界、畸形输入 | 已有 bounded decoder，未知 wire fields 保留并对长度/varint/UTF-8 做边界校验 | 部分实现 |

### 大类 1：protobuf 解码与规则模型
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 1.1 PPResponse 解码 | response 字段与嵌套 item | 已有 PPResponse 解码器 | 部分实现 |
| 1.2 PPConfigItem 解码 | list/content/wap 等字段 | 已有 PPConfigItem 解码器 | 部分实现 |
| 1.3 PPRule 解码 | 八个字段及默认 enum | 已有 PPRule 八个字段解码 | 部分实现 |
| 1.4 round-trip 与未知字段 | 默认值、未知字段和序列化往返 | 已有未知字段原始片段保留及相关测试记录 | 未完成全字段 round-trip |

### 大类 2：小说规则提取与执行
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 2.1 RE 规则 | regex scalar/capture group 提取 | 已有 capture group 1 提取 helper | 局部实现 |
| 2.2 XPath 规则 | XPath 首节点和属性/text 提取 | 已有 XPath 首节点提取 helper，覆盖 meta content/其他 text | 局部实现 |
| 2.3 规则类型分派 | dataType、ruleType 和优先级 | 已确认 RE/XPath 局部 helper | 未完成完整 dispatch |
| 2.4 小说页面链 | 小说列表、详情、章节等完整流程 | 尚无完整 TransEngine 小说执行链验收记录 | 未完成 |

### 大类 3：JS 执行与分页规则
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 3.1 pageJs | pageJs 沙箱执行 | 已确认字段存在 | 未完成真实执行 |
| 3.2 pageJsForSingleHtml | 单 HTML 页面脚本执行 | 已确认字段存在 | 未完成真实执行 |
| 3.3 nextPageRule | 下一页规则与分页语义 | 已确认字段存在；现有 DEX consumer 证据不足 | 未完成；语义待证 |
| 3.4 JS 安全 | 沙箱、超时、取消和资源上限 | 已列入实施顺序 | 未完成验收 |

### 大类 4：漫画规则与资源加载
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 4.1 漫画选择器 | data-original 优先、src 回退 | 已有漫画图片选择 helper | 局部实现 |
| 4.2 图片 URL | 相对 URL 和安全 scheme | 已有相对 URL 与安全 scheme helper | 局部实现 |
| 4.3 图片去重 | 图片资源去重 | 已有去重 helper | 局部实现 |
| 4.4 漫画分页/排序/UI | 分页、排序、加载和展示 | 已列入未完成项 | 未完成 |

### 大类 5：视频规则与播放流程
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 5.1 视频路径 | videoPathRule 选择与解析 | 已有两阶段 video path/episode selector helper | 局部实现 |
| 5.2 视频集数 | videoEpisodeRule 选择和分组 | 已有两阶段 helper | 局部实现 |
| 5.3 播放地址 | HTTP href 解析、标题回退和去重 | 已有视频 HTTP href、标题回退和去重 helper | 局部实现 |
| 5.4 播放器流程 | 播放地址、集数切换和 UI | 已列入未完成项 | 未完成 |

### 大类 6：响应更新、规则管理与持久化
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 6.1 PPResponse 更新 | 响应更新、版本和签名 | 已确认 PPResponse 中版本/签名相关字段 | 未完成更新闭环 |
| 6.2 规则管理 UI | 规则查看、编辑和管理 | 已列入未完成项 | 未完成 |
| 6.3 持久化 | 规则保存、备份与恢复 | 已列入未完成项 | 未完成 |
| 6.4 导入导出/迁移 | 规则导入导出、版本迁移 | 已列入未完成项 | 未完成 |

### 大类 7：测试与分类验收
| 小类 | 工作内容 | 已完成什么 | 当前状态 |
|---|---|---|---|
| 7.1 protobuf 测试 | schema、畸形输入、未知字段和边界 | 已有 protobuf/畸形输入/未知字段测试记录 | 局部测试 |
| 7.2 规则 helper 测试 | RE、XPath、video、comic helper | 已有对应 helper 测试记录 | 局部测试 |
| 7.3 真实样本差分 | 原版可观察行为和授权样本 | 已列为验收要求 | 未完成 |
| 7.4 Android 真机 | 小说/漫画/视频完整执行链 | 已列为验收要求 | 未完成 |


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
