# 坏坏猫 1.5.0.5：APK、DEX 归档、重建包与原版界面对照

核对日期：2026-11-16  
所有原始材料均来自用户上传文件；仓库为 Private。本文区分原始样本、恢复出的 DEX、重建测试包和当前 24.6 MB 变体，避免把不同包名/构建混为同一 APK。

## 1. 样本身份：目前实际存在四种不同包体

| 样本 | 大小 | SHA-256 | Manifest package | Application | 说明 |
|---|---:|---|---|---|---|
| 原始签名 APK `坏坏猫_1.5.0.5.apk`（Library；与 `(1).apk` 字节相同） | 22,460,988 | `fb35e9e4c876e5cb99e00f7010a40c2f060d20b2aa392500983862ab7e91ff33` | `com.hhm.verticalsearch` | `com.stub.StubApp` | 原签名 CN=hhm；仅有一个根目录 classes.dex，内容为壳 DEX |
| DEX 归档 `com.hhm.verticalsearch.zip` | 19,708,863 | `f747fa1c684ef568e3da1d857dbebdcbb7687e2db4c58a4390282948f5681d10` | — | — | 7 个 DEX：五个业务 DEX、一个原壳 DEX、一个空占位 DEX |
| 重建未签名包 `坏坏猫_1.5.0.5_unwrapped_unsigned.apk` | 24,412,961 | `a3fd8273e79efe76f3b6a3662b03ec6870e76741ead60da36edc176533afcbb7` | `com.hhm.verticalsearch` | `com.search.verticalsearch.common.HuaYueApplication` | 七个根 DEX；保留资源与 native 库 |
| 重建测试签名包 `坏坏猫_1.5.0.5_test_signed_v1.apk` | 24,740,194 | `4862de046e50a19fdbf0acd0a567a23c2bf8acaf3b8006445525a7e16e24ba8e` | `com.hhm.verticalsearch` | `HuaYueApplication` | 与未签名包内容一致，仅增加三项 META-INF v1 签名文件 |
| 当前会话 24.6 MB 变体 `坏坏猫_1.5.0.5.apk` / Library `坏坏猫_1.5.0.5(5).apk` | 24,608,277 | `a62c16d99547854ff313f229649762f2d7898b4e2e0dc66574df4c43fa62480e` | `com.hhm.verticalsearci` | `HuaYueApplication` | 与重建包七个 DEX 完全一致；Manifest package 与资源表不同；签名 CN=Android，不是原版 CN=hhm |

注意：Library 中原始文件 `坏坏猫_1.5.0.5.apk`（22,460,988 bytes）与 `坏坏猫_1.5.0.5(1).apk` 是同一 SHA-256；当前会话中同名 `坏坏猫_1.5.0.5.apk` 是另一份 24,608,277-byte 文件。两者不能按文件名视作同一 APK。\n\n`com.hhm.verticalsearch` 与 `com.hhm.verticalsearci` 是两个不同 package id。旧壳 APK 证书为 `CN=hhm`，SHA-256 指纹 `F9:E0:52:9E:65:8F:F1:53:3D:93:21:FB:5F:CC:37:26:25:D9:85:3B:3C:F4:FA:26:26:71:4C:08:05:F3:07:E9`；当前 24.6 MB 变体证书为 `CN=Android`，SHA-256 指纹 `A4:0D:A8:0A:59:D1:70:CA:A9:50:CF:15:C1:8C:45:4D:47:A3:9B:26:98:9D:8B:64:0E:CD:74:5B:A7:1B:F5:DC`。因此当前 24.6 MB 文件不是原签名 APK；不能把它当成原版签名身份。

## 2. DEX ZIP 的真实分组与重建映射

旧壳 APK 的 `classes.dex`（11,193,804 bytes）与 DEX ZIP 的 `classes3.dex` **逐字节完全一致**，确认 ZIP 中的 `classes3.dex` 是原壳 DEX，不是业务代码。

| DEX ZIP 条目 | 大小 | class_defs | 角色 |
|---|---:|---:|---|
| `classes.dex` | 6,034,028 | 4,569 | 业务 DEX 1 |
| `classes2.dex` | 5,978,116 | 6,281 | 业务 DEX 2 |
| `classes3.dex` | 11,193,804 | 7 | 原 APK 的 StubApp 壳 DEX |
| `classes4.dex` | 3,525,160 | 3,026 | 业务 DEX 3 |
| `classes5.dex` | 6,130,312 | 5,396 | 业务 DEX 4 |
| `classes6.dex` | 3,555,524 | 2,597 | 业务 DEX 5 |
| `classes7.dex` | 284 | 1 | 仅含 `LEmpty;` 的占位 DEX |

五个业务 DEX 合计 21,869 个 class definitions。加上 1 个 `LEmpty;` 占位类和 1 个新建 `StubApp` 兼容类，重建包共 21,871 个类定义。业务 DEX 的 method_ids 均低于 65,535，保持 MultiDex 分包是正确方向。

重建脚本使用的 DEX 映射：

| 重建 APK 条目 | 来源 DEX ZIP 条目 |
|---|---|
| `classes.dex` | `classes.dex` |
| `classes2.dex` | `classes2.dex` |
| `classes3.dex` | `classes4.dex` |
| `classes4.dex` | `classes5.dex` |
| `classes5.dex` | `classes6.dex` |
| `classes6.dex` | `classes7.dex` |
| `classes7.dex` | 从 `StubApp_compat.smali` 编译的新兼容 DEX |

已做逐字节比较：重建包的 classes.dex 至 classes6.dex 与上述来源 DEX 在 byte offset 32 至 EOF 完全相同；差异仅在 DEX header 的 SHA-1 signature / Adler-32 字段。重建输出重新计算后，七个 DEX 的 SHA-1 signature 和 Adler-32 均通过。重建 classes7.dex 字符串池包含 `Lcom/stub/StubApp;`、`getOrigApplicationContext`、`interface11`、`interface22`、`mark`，与兼容桩源码一致。

## 3. 重建包与当前 24.6 MB APK 的关系

当前 24.6 MB APK 与未签名重建包逐项比较：

- 两者共同拥有的 3,174 个条目中，3,172 个内容 SHA-256 完全相同。
- 仅有两个共同条目内容不同：`AndroidManifest.xml` 与 `resources.arsc`。
- 当前 APK 另有三个 `META-INF/ANDROID.*` 签名文件；未签名重建包没有签名文件。
- 七个根目录 DEX 的文件哈希逐个完全相同。

因此可以确认：**当前 24.6 MB APK 与重建未签名包使用相同的七份 DEX 字节码；差别集中在包名/Manifest、资源表和签名。** 这比单纯字符串相同更强，足以将当前 APK 的 DEX 作为重建链的代码对照基线。

重建测试签名包与未签名包相比，全部 3,174 个原有条目内容完全相同，仅增加三项 `META-INF/HHM-TEST.*` 签名文件。测试证书为 `CN=HHM Test Build`，SHA-256 指纹 `72:A5:E8:26:7F:D6:1A:DF:1F:0F:FA:68:7A:DC:0E:F8:8D:DA:2A:9B:55:85:CD:66:F4:35:19:22:60:8B:E9:7E`。当前容器使用 `jarsigner -verify` 对测试签名包复核，结果为 `jar verified`。这是 JAR/v1 自签名验证，不代表 Android v2/v3 签名或 zipalign 已完成，也不代表安装/启动通过。

## 4. 加固残留与运行验证边界

重建包仍保留原资源和 native 库；静态报告确认仍可见 `assets/libjiagu.so`、`assets/A3AEECD8.dex`、`lib/armeabi/libA3AEECD8.so`、`com.qihoo.jiagu.*` / `com.qihoo.jiagutracker.*` 等残留。它们的静态存在不等于运行时必定加载。没有完成运行时加载观察前，不应批量删除 SO、DEX 或 SDK 组件。

另有 `JNIVerify.getSecret()` / `libverify.so` 的业务 JNI 依赖，不能把所有 native 库都归为壳组件。完整方法调用链和初始化验证仍需后续处理。

当前已独立复核：APK ZIP CRC、七份重建 DEX 的 SHA-1/Adler-32、旧壳 DEX 与 ZIP 的字节一致性、重建 DEX 映射、测试签名包 JAR/v1 验证。**没有真机安装、冷启动、登录、搜索、阅读、播放器、权限回调或 logcat 回归证据。**

## 5. 原版 UI 截图对照（用户提供的 2026-11-15 截图）

### 首页：`Screenshot_20261115_065832.jpg`

原版可见结构：
- 蓝色顶部大区，标题“坏坏猫”；
- 搜索类型为“小说 / 漫画 / 综合”，标题下方有圆角搜索框；
- 搜索区下方三个圆形快捷入口：“排行 / 今日抽奖 / 赚取现金”；
- 底部四项导航：“搜索 / 收藏夹 / 喵星 / 我的”。

当前仓库 `MainActivity.kt` 是代码动态拼装的开发骨架：虽然有蓝色标题、类型切换和搜索框，但类型目前为“小说 / 漫画 / 全部”，快捷入口是书源管理、搜索书籍、阅读记录、替换规则等开发功能；缺原版三个快捷入口、原版首页留白/比例、图标和原版导航细节。**首页尚未复刻。**

### 收藏夹：`Screenshot_20261115_065846.jpg`

原版可见结构：
- 顶部左侧“筛选”下拉；
- 中间双标签“作品 / 书贴”；
- 右侧更多菜单；
- 三列网格卡片，包含“本地书籍 / 已看完 / 养书”等分类文件夹、具体书籍封面和新增卡片；
- 底部四项原版导航。

当前 `FavoritesActivity.kt` 已有三列文件夹/书籍卡片及创建、重命名、删除、移动能力，但顶部是“我的收藏 + 文件夹 + 更多”，没有原版筛选控件和“作品/书贴”标签；默认文件夹与原版分类名称/卡片组合也不同，封面图、卡片比例、数量、角标和空态未对齐。**收藏夹只是结构接近，仍不是原版 UI。**

### 搜索：`Screenshot_20261115_065838.jpg`

原版可见结构：
- 返回按钮、内容类型下拉、关键词输入、搜索按钮；
- 搜索渠道/入口标签：“咪搜 / 喵友圈 / 神马 / 360 / 百度 / 搜狗 / 免责声明”；
- 空结果时显示原版猫咪插画。

当前 `SourceSearchActivity.kt` 明确是书源引擎开发验证页，使用书源 Spinner、关键词输入、分页按钮和状态文本；没有原版搜索渠道标签、历史/热词布局、原版空态插画或原版顶部控件。**搜索 UI 未复刻，Legado 书源检索能力与原版搜索外观需分层接入。**

### 阅读器：`Screenshot_20261115_065939.jpg`

原版可见结构：
- 米黄色/护眼阅读底色，较大字号与宽行距；
- 顶部暗色半透明操作层，含返回、刷新/网页、听书和更多；
- 正文上方可显示“去网页”和来源 URL；
- 底部章节进度滑杆与目录、换源、亮度、设置入口；
- 右侧悬浮月亮按钮。

当前项目已具备书源章节阅读流程和独立 CSS 多方案/子选择器编辑器，但原版阅读器工具栏、章节进度条、字体/行距、护眼色、浮动按钮和操作菜单还没有逐项按截图还原。CSS 自定义属于增强功能，不代表原版阅读 UI 已完成。

## 6. 当前上传到私有仓库的参考材料

已上传文本/源文件：
- `reference/recovery-tools/build_unwrapped.py`
- `reference/recovery-tools/StubApp_compat.smali`
- `reference/recovery-tools/recalc_dex_header.py`
- `reference/audits/phase1-static-audit.md`
- `reference/audits/phase1-usage.md`
- `reference/audits/phase2-static-acceptance.md`
- `reference/audits/round11-reading-source-purification-inventory.md`
- `reference/audits/round12-layout-resource-mapping.md`

APK/ZIP 原始二进制及四张 JPG 截图仍保留在用户上传文件库；当前 GitHub 写入链路没有把这些大型本地二进制字节直接流式传入 Git blob 的通道，因此没有将它们伪装成已上传。本文保存精确哈希和分析结果。后续若接入 Git LFS 或 Release asset 上传通道，可再把二进制原件放入 Private 仓库。

## 7. 下一步按截图推进

1. 先改首页：恢复原版“小说/漫画/综合”、三个快捷入口、搜索框比例与四项底栏；开发功能放到原版入口对应页面，不要挤在首页。
2. 再改收藏夹：恢复“筛选 / 作品-书贴 / 更多”顶部结构、三列分类文件夹卡片、封面和新增卡片，再接回当前文件夹数据能力。
3. 重做搜索页视觉层：原版渠道标签与空态，底层继续调用 Legado 书源检索；不要把源 Spinner 作为最终界面。
4. 重建阅读器工具栏和底部控制层，同时保留已完成的 CSS 方案编辑能力。
5. 真机截图逐页对照后再宣称 UI 完成。
