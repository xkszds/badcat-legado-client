# Runtime-3.2f.44：四个公开书源夹具来源与完整性审计

- 日期：2026-11-23
- 分支：`integration/legado-upstream-runtime`
- 起始 HEAD：`fe537260ec36e48aa67dbcaa41db5e176153ae3c`
- 固定 Legado 上游基准：`330e606c4d57e08adc056ff8ab7a4132c784da9a`
- 用户授权：用户明确要求由我们自行从网上寻找四个书源；本报告记录的是公开社区书源快照，不把它们称为用户原始上传语料。

## 结论

四个站点均找到完整 BookSource 对象，并以单元素 JSON 数组形式保存为只读 Android 测试资产。对象的字段和值未做规则改写；仅从上游多源数组/分片中提取目标对象并规范化 JSON 空白。四份文件均回读验证为合法 JSON，数组长度为 1，且均保留搜索、详情、目录、正文四组规则。未发现凭据字段；四份源的 `header` 与 `loginUrl` 均为空。零零小说的详情初始化规则包含原始内联 JS，已原样保留。

**这不是原始用户语料，也不是运行时兼容性结论。** 它只解决了可审计的公开测试配置来源问题。仍需单独验证 Android 真实运行时和站点当前可达性。

## 来源、许可证与固定版本

| 书源 | 原始来源（固定 commit） | 上游 Git blob | 许可证 | 源 ID / bookSourceUrl |
|---|---|---|---|---|
| 唐三中文 | [mumuceo/file01](https://github.com/mumuceo/file01/blob/66a9f09187b0c8642bd3f8f9262146c5ace31aa0/202609/14834_67143e8696757866bc238acc878865fe.json) @ `66a9f09187b0c8642bd3f8f9262146c5ace31aa0` | `e12b9b06330b5dfffb74e3d5fdd5a27355c39bab` | MIT（版权声明 mumuceo，2023） | `http://www.xtangsanshu.com` |
| 笔趣阁fun | [mumuceo/file01](https://github.com/mumuceo/file01/blob/66a9f09187b0c8642bd3f8f9262146c5ace31aa0/202503/4078_419b9c27f3161a953e0246be6b1e3ce9.json) @ `66a9f09187b0c8642bd3f8f9262146c5ace31aa0` | `588fbdfb928f50273b0f8046e2ed9d9c1e278084` | MIT（同上） | `https://www.bqg.fun` |
| 精华书阁 | [yolo52/Yuedu](https://github.com/yolo52/Yuedu/blob/9e9eb4ec08ffa5ac8986e2f07cae36c09f854d0a/shuyuan.json) @ `9e9eb4ec08ffa5ac8986e2f07cae36c09f854d0a` | `70870f3f3e92e5e1ad2aa71cc7a45c0ccde9fad4` | GPL-3.0 | `https://m.jhsssd.com/` |
| 零零小说 | [Mina-kk/yuedu-MCP](https://github.com/Mina-kk/yuedu-MCP/blob/e129d35e7cba08f8dcacc2cf05dc3e626b5965dd/app/src/main/assets/corpus/shards/f_0857.json) @ `e129d35e7cba08f8dcacc2cf05dc3e626b5965dd` | `74b6af8adcaef4f31751274eb12fe5f6c0dbd855` | GPL-3.0 | `https://www.00shu.la/#喜静` |

许可证核验：mumuceo/file01 根目录 LICENSE 为 MIT；yolo52/Yuedu 与 Mina-kk/yuedu-MCP 根目录 LICENSE 为 GNU GPL-3.0。原始文件是多书源数组或语料分片；仅抽取匹配的完整对象，不把相邻书源带入测试资产。

## 测试资产与校验

SHA-256 针对提交的规范化 UTF-8 文件字节计算（包含外层单元素数组和末尾换行）。

| 资产 | 文件字节数 | SHA-256 | Badcat Git blob |
|---|---:|---|---|
| `app/src/androidTest/assets/legado-sources/tangsan-chinese.json` | 1846 | `23b7391779b758e034a0b2993a0bbd2d12fe5a5740943a4f4f7b0712d2895650` | `eae344867364afce281663eecae1ae9aae7d652f` |
| `app/src/androidTest/assets/legado-sources/bqg-fun.json` | 5581 | `39026fb8f6d3f53e7b5f51111f9f966bfd9b93a020e1e5c50684dedbe1bff5d5` | `084fdad69dc1f6d52318d52a29b7fb5193dbc0d0` |
| `app/src/androidTest/assets/legado-sources/jinghua-shuge.json` | 4458 | `11875ff9cc00dc63403fb2ff5657ad6755100675547628a96313f32450c5971f` | `b4bcbe04b5e3b683e8fe5d053651a43f1c0d9bf0` |
| `app/src/androidTest/assets/legado-sources/lingling-xiaoshuo.json` | 2737 | `120a4cc9f46d9bfe2ed553f09cbde06b8263fd9a1807a17178e70fb8e67206b5` | `4c2e5b89b9a087fc78d6aa59d5e1a75e805e95a6` |

回读验证（提交 `fe537260ec36e48aa67dbcaa41db5e176153ae3c`）：四份文件均可 JSON 解析；每份恰有一个 BookSource；`ruleSearch`、`ruleBookInfo`、`ruleToc`、`ruleContent` 均存在。源字段数量分别为 27、18、18、23。所有 header/loginUrl 为空；无 Cookie/Authorization/密码/token/API key 配置。零零小说的 `ruleBookInfo.init` 内联 JS 保留，未执行或改写。

## 范围与限制

- 本次只增加四个不可变来源配置夹具；未修改生产 Kotlin、运行时、UI、存储、Gradle、工作流、Python 脚本或现有书源逻辑。
- `00shu.la` 的 source URL 带有上游原始标识片段 `##喜静`，保留该值以避免改变 source identity。
- 书源快照时间不同：唐三中文为 2026-09；笔趣阁fun 为 2025-03；精华书阁为 2025-05；零零小说为 2024-12。不能假设这些规则在当前站点仍有效。
- 夹具完整性不等于站点可达，不等于 Rhino/HTTP/Cookie 运行时兼容，也不代表 Search → Detail → TOC → Content 通过。
- .43 仍保留为“用户原始语料缺失”的阻断记录；.44 仅通过公开、可追溯的社区配置提供替代测试输入。
- 下一步必须另行 preregister Android instrumentation 任务，调用 `BadcatReadingEngineFactory` 创建的生产集成引擎；不得把本轮文件提交或 JSON 解析通过称为运行时测试通过。

## CI 状态

截至本报告检查，#3232 / run `45203418924`（SHA `2c4b459a5555c5f3deebd041e8a1a45f451bba97`）、#3233 / run `45203851043`（SHA `2c7fc5dca2b76bdceb25f60b755fc858d5779b4f`）、#3234 / run `45203927388`（SHA `f78e71d5f91d61e0637e89fea3207eb2d23eb890`）均仍为 `in_progress`。未把任何一个 run 的结果借给本报告或本次夹具提交；本次新提交触发的 CI 也必须按其自身 SHA 独立核验。
