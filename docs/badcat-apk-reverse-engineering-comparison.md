# 坏坏猫 1.5.0.5 APK 实物核对与客户端差距报告

核对日期：2026-11-16  
核对对象：用户本轮上传的 `坏坏猫_1.5.0.5.apk`  
核对方式：直接读取 APK ZIP 目录、二进制 AndroidManifest、DEX 头信息/类名字符串及资源路径，并对照当前仓库 Android 页面代码。此报告不等同于完整反编译或真机截图验收。

## 1. APK 身份与完整性

| 项目 | 实际读取结果 |
|---|---|
| 文件大小 | 24,608,277 bytes |
| SHA-256 | `a62c16d99547854ff313f229649762f2d7898b4e2e0dc66574df4c43fa62480e` |
| APK ZIP 条目 | 3,177 |
| Manifest package | `com.hhm.verticalsearci` |
| versionName / versionCode | `1.5.0.5` / `150` |
| minSdk / targetSdk | 15 / 26 |
| Manifest Activity | 114 |
| `res/layout*` 编译布局资源 | 790 |
| DEX | `classes.dex` 至 `classes7.dex`，另有 assets 中的 DEX 载荷 |

主 DEX 文件中可直接读取到大量原始类名，例如 `com.search.verticalsearch.common.ui.activity.MainActivity`、`HomeFragment`、`SearchActivity`、`SearchResultActivity`、`BookDetailActivity`、`ChapterListActivity`、`FavoritesFolderActivity`、`ReadRecordActivity`、`SettingAdBlockActivity`。阅读器另有 `com.reader.reader.ui.reader.wap.WapReadActivity` 等组件。

APK 中还包含 `assets/libjiagu.so`，以及一个文件头不是标准 DEX magic 的 `assets/A3AEECD8.dex`。因此不能把 ZIP 内所有 DEX 载荷都当作可直接反编译的普通代码。五个主要 classes DEX 是标准 DEX 格式；这仍然只是编译产物，不是原始 Gradle 工程或完整源码。

## 2. 页面结构：APK 证据与当前实现逐项对照

| 页面/模块 | APK 中核实到的组件或资源 | 当前仓库实现 | 实际差距 |
|---|---|---|---|
| 首页/主导航 | `MainActivity`、`HomeFragment`；`fragment_home.xml`、`fragment_home_search_txt.xml`、`fragment_home_search_comic.xml`、`view_home_tab_item.xml` | `MainActivity.kt` 以代码动态拼出蓝色标题、搜索框、快捷卡片和底部四项导航 | 目前只是近似骨架；尚未按原始布局层级、首页内容模块、图标、间距和导航切换逐项还原 |
| 搜索入口 | `SearchActivity`；`activity_search.xml`、`fragment_search.xml`、`head_view_search.xml`、`view_searchbar.xml` | `SourceSearchActivity.kt` | 是书源引擎开发验证页，不等于原版搜索页；首页与搜索页的层级、搜索模式、历史/热词等还需核对 |
| 搜索结果 | `SearchResultActivity`；`activity_search_result.xml`、`fragment_search_result.xml`、`fragment_search_result_root.xml`、`item_book_search_result*.xml`、`item_comic_search_result.xml` | 当前搜索结果由书源检索流程展示 | 缺原版结果页布局、来源/类型切换、结果卡片样式、空态和加载状态的完整重建 |
| 书籍详情 | `BookDetailActivity`、`ComicDetailActivity`；`activity_book_detail.xml`、`layout_book_detail_head.xml` | `SourceBookActivity.kt` | 当前明确是“开发验证界面”，不能算原版详情页；收藏按钮和目录列表也尚未复刻原版结构 |
| 章节目录 | `ChapterListActivity`；`activity_chapter_lsit.xml`、`frag_chapterlist_catalog.xml`、`frag_chapterlist_catalog_wap.xml`、`item_chapter_list.xml`、`frag_chapterlist_bookmark.xml` | 当前目录由书源详情流程直接展示 | 缺原版目录页、书签目录、来源切换和目录操作布局 |
| 收藏/书架 | `FavoritesFolderActivity`；`FavoritesAdapter`、`FavoritesFolderEntity`、`DbBookShelf`、`DbBookCache`、`DbTxtCache`、`DbComicCache`；`activity_favorites.xml`、`activity_favorites_folder.xml`、`fragment_favorites_novel.xml`、`item_favorites_grid.xml`、`item_favorites_grid_folder.xml`、`dialog_favorites_folder.xml`、`dialog_favorites_rename.xml`、`popup_favorites_more.xml` | `FavoritesActivity.kt` 已改为三列文件夹/书籍卡片，支持新建、重命名、删除文件夹，移动/移出收藏 | 结构已向文件夹网格靠拢，但仍是新写的简化 UI；未核实原版卡片尺寸、排序筛选、弹窗细节、数据库实体字段、图片封面和空态布局，不能称为 UI 复刻 |
| 阅读记录 | `ReadRecordActivity`、`ReadRecordAdapter`、`ReadRecordEntity`；`activity_read_record.xml`、`fragment_read_record.xml`、`item_read_record.xml` | `ReadingHistoryActivity.kt` 已保存书源/书籍/最近章节并支持继续阅读、删除历史 | 核心记录能力已存在，但当前页面是新写的简化页面，不是原版阅读记录 UI；数据模型也未与原版记录格式对应 |
| 阅读器 | `WapReadActivity`、`XstLocalReadActivity`、`ComicTransReadActivity`；`activity_base_read.xml`、`activity_read_setting.xml`、`action_view_chapter_bar_wap.xml`、`layout_wapread.xml` | 当前书源章节阅读页与 CSS 自定义编辑器 | 阅读流程与 CSS 配置已有基础能力，但原版顶部/底部操作栏、翻页模式、阅读设置、字体/背景/亮度等尚未逐项对照复刻 |
| 净化/广告规则 | `SettingAdBlockActivity`；`activity_ad_block.xml`、`item_setting_ad_block.xml`；DEX 中可见 `ADBlockUtils`、`AdBlockPresenter`、`DbAdMark`、`sens.Pp.PPRule`、`sens.AdMark.AdMarkRuleItem` 等符号 | 当前主要是 Legado 正文替换规则与受限 JS 执行器 | 不能等同原版广告净化。WebView 注入、AdMark 服务和 PPRule 的真实数据/执行行为仍未迁移 |

## 3. 这次核对后确认的关键结论

1. **此前把收藏页仅按“书籍列表”推进是不够的。** APK 明确包含独立收藏文件夹页面、文件夹实体、三列网格项目、文件夹弹窗和更多菜单。接下来书架必须以“文件夹 + 网格书籍卡片 + 顶部筛选/菜单”为结构目标，而不是继续堆单列列表功能。
2. **原版首页、搜索页、搜索结果页是分开的组件体系。** APK 有 MainActivity/HomeFragment，也有独立 SearchActivity/SearchResultActivity。当前主界面与书源搜索页不能简单视作已复刻。
3. **原版阅读记录是独立页面。** 当前新增记录逻辑解决了“保存并继续阅读”，但还没达到原版 UI 和数据模型一致。
4. **详情、目录、阅读器都存在原版独立 Activity/布局。** 当前 SourceBook/SourceChapter 页面应继续标注为开发验证界面，直到按原版资源重建。
5. **Legado 替换规则不能替代坏坏猫净化。** 原版 APK 中确实存在独立 ADBlock、AdMark 和 PPRule 符号及资源，仍需方法级分析后分别迁移。

## 4. 反编译与源码上传边界

- APK 是编译后的二进制包，不包含可直接恢复的完整原始 Kotlin/Java 工程、Gradle 配置、构建脚本和全部资源源文件。
- 本次环境可直接读取 APK 目录、Manifest、标准 DEX 元数据/字符串和资源路径。进一步核对确认：`classes.dex` 至 `classes5.dex` 是标准 DEX；`classes6.dex`、`classes7.dex` 体积很小；`assets/AdDex.4.0.1.dex` 是标准 DEX；`assets/A3AEECD8.dex` 不是标准 DEX 文件头；同时存在 `assets/libjiagu.so`。类名字符串确认 `FavoritesFolderActivity` / `FavoritesAdapter` 位于 `classes2.dex`，`FavoritesFolderEntity` 位于 `classes.dex`，`SearchResultActivity` 的类描述也在标准 DEX 中。当前容器没有 JADX、Apktool、aapt2 等反编译工具，且不能从网络下载工具，因此**尚未完成 Java/Kotlin 方法体反编译、二进制 XML 解码或真机截图比对**。
- 用户已明确授权对其上传 APK 进行分析。该授权足以支持本项目对上传文件开展本地研究，但不自动证明 APK 内第三方 SDK/组件也可公开再分发。故当前仍不把原始 APK 或第三方完整反编译源码复制到公开 GitHub；已上传的是本项目自行整理的结构核对报告和可验证元数据。
- 后续若补齐反编译工具，将优先提取并分析与本项目有关的页面/规则调用链；只把自写的适配代码、接口说明、测试和不含原版受保护实现的结构笔记提交到仓库。

## 5. 下一轮执行顺序

1. 先按原版收藏资源重做书架：文件夹层级、三列卡片、文件夹管理、菜单和空态。
2. 再拆分首页与搜索入口，恢复 MainActivity/HomeFragment/SearchActivity/SearchResultActivity 的页面职责。
3. 重建详情和目录页面，并把现有 Legado 书源数据接入原版风格布局。
4. 对照原版阅读器布局和控制栏，保留已经完成的 CSS 多方案/子选择器自定义功能。
5. 最后按方法级证据迁移 ADBlock、AdMark、PPRule，不能用占位实现冒充完成。

## 6. 最近构建状态

书架改造提交 `519cdc24df6fc9b2e850164338a292043c1dedf7` 的 GitHub Actions run `43848666949` 已通过：契约测试、Rhino 宿主测试、Debug APK 组装和产物上传全部成功。上传的 APK Artifact 大小 4,782,473 bytes，Artifact SHA-256 为 `ba8e293e37fb8c91103e5d721eb3883d11b0ac88606995cb9314d1ff5e82dabe`。这仅证明 CI 通过，不代表真机验收或原版 UI 复刻完成。


## 7. 配套 DEX 归档交叉核验（2026-11-16）

本轮同时重新读取用户 Library 中的 `com.hhm.verticalsearch.zip`。它包含 7 个 DEX 文件，并非 Java/Kotlin 源码工程。与 APK 对照后，ZIP 的 classes.dex、classes2.dex 与 APK 对应文件大小和 DEX 头部 ID 数量相同，但 SHA-256 不同；ZIP classes4.dex/classes5.dex/classes6.dex 分别与 APK classes3.dex/classes4.dex/classes5.dex 在大小和头部数量上对应，但哈希也不同。只有 ZIP classes7.dex 与 APK classes6.dex（284 bytes）字节完全一致。ZIP 另含 11,193,804-byte 的 classes3.dex，头部 class_def 数量为 7，需独立检查，不能仅凭名称判断用途。

完整逐项哈希、头部计数、编号映射和限制见 [原版 APK 与配套 DEX 归档核对记录](reference-archive-comparison-2026-11-16.md)。

本轮没有把 APK/ZIP 二进制本体谎称为已提交到 GitHub：当前可用的 GitHub 写入通道虽支持文本和 Git blob，但本轮工具链没有将大型本地文件字节直接传入 blob 请求的通路。原始材料仍在用户上传文件库中；仓库已保存可复核的 SHA-256 和结构清单。后续方法体反编译、二进制 XML 解码与真机视觉对照仍待工具链补齐。
