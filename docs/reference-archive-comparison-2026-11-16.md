# 原版 APK 与配套 DEX 归档核对记录

核对日期：2026-11-16  
仓库：`xkszds/badcat-legado-client`（Private）  
用途：记录原版 APK 与用户提供的 `com.hhm.verticalsearch.zip` 的可复核差异，作为后续页面、类和功能对照基线。

## 1. 输入文件与完整性

| 文件 | 大小 | SHA-256 | 当前可用位置 |
|---|---:|---|---|
| `坏坏猫_1.5.0.5.apk` | 24,608,277 bytes | `a62c16d99547854ff313f229649762f2d7898b4e2e0dc66574df4c43fa62480e` | 用户上传文件；本轮重新核对的 Library 副本哈希一致 |
| `com.hhm.verticalsearch.zip` | 19,708,863 bytes | `f747fa1c684ef568e3da1d857dbebdcbb7687e2db4c58a4390282948f5681d10` | 用户先前上传的 Library 文件 |

APK 副本与当前会话挂载的 APK SHA-256 完全一致。ZIP 可正常读取，包含 8 个条目（含目录项），7 个 DEX 文件，解压后总计 36,417,228 bytes。

## 2. APK 与 ZIP 内 DEX 清单

### 原版 APK

| 条目 | 文件大小 | DEX magic | SHA-256 |
|---|---:|---|---|
| `assets/A3AEECD8.dex` | 65,160 | 非标准 DEX magic（`cf774cc79b2101cd`） | `f0ead1ad60e0cc310c1a40685c28fc7a69aa346604552816c51dd3c1718a1e76` |
| `assets/AdDex.4.0.1.dex` | 171,696 | dex035 | `25380360545288fee582ce904d926952f7f0627f8f191b55643e5e876e56e56da9f` |
| `classes.dex` | 6,034,028 | dex035 | `fb06064cebf1b0acaa6ec2b17c732a054b847265b5d3c5616d6dc96957a44b59` |
| `classes2.dex` | 5,978,116 | dex035 | `a7c02ed2ab5aa42fcc0077bb375dc743df73bd330856e804762da4f89480b6b0` |
| `classes3.dex` | 3,525,160 | dex035 | `3de70eb07bfed2125ed08a911c18e4fc97b261b3369086a428c2f9e20d2f7905` |
| `classes4.dex` | 6,130,312 | dex035 | `3f4e3a4cf1c10b4f436e27c5ce108df7195b388266d0267d211635d5a75360ad` |
| `classes5.dex` | 3,555,524 | dex035 | `5a1b9243aff8c55ca565242e5e920ee8427d2e06e77b0492f85c26f1cb56d116` |
| `classes6.dex` | 284 | dex035 | `9f8898ce35a47aeafced99ea0d17c33e73037bb2307c7688e50819966f4ae939` |
| `classes7.dex` | 840 | dex035 | `df060795de10483f3aae19eac72c03578fb6de4b4e82ac445da4d02caaeb9ada` |

### 配套 ZIP

| 条目 | 文件大小 | DEX magic | SHA-256 |
|---|---:|---|---|
| `classes.dex` | 6,034,028 | dex035 | `215d6ed1af9be227ee8c61bbe166776cd0405620326a469d14ca1b331fbe3b3d` |
| `classes2.dex` | 5,978,116 | dex035 | `d8ec3436c76a961d788da62888f191ca0466333e737cb5a1e0fc18794c699c55` |
| `classes3.dex` | 11,193,804 | dex035 | `2acaa728cf44eb5fe2ee6e08a767ad8ea12162889b16e867a3f095bdb6aa89a2` |
| `classes4.dex` | 3,525,160 | dex035 | `0d33bc1d0153f9b4fb7255731b0461af25dbe87a628200dba24e2341a6c56e05` |
| `classes5.dex` | 6,130,312 | dex035 | `35ec662a215c83fc66c3c52a81ab998af96f2fd91ea76c0c9c0db450d8ca6114` |
| `classes6.dex` | 3,555,524 | dex035 | `76b83f3f1aadce9cf1b45fdb654905e951d8e3214bacb7af6232c9b1e0db416a` |
| `classes7.dex` | 284 | dex035 | `9f8898ce35a47aeafced99ea0d17c33e73037bb2307c7688e50819966f4ae939` |

## 3. DEX 对照结果

| 原版 APK DEX | ZIP 中可能对应项 | 可确认的关系 |
|---|---|---|
| `classes.dex` | `classes.dex` | 大小及 DEX 头部各 ID 数量一致；SHA-256 不同，不能视为字节相同 |
| `classes2.dex` | `classes2.dex` | 大小及 DEX 头部各 ID 数量一致；SHA-256 不同，不能视为字节相同 |
| `classes3.dex` | `classes4.dex` | 大小及 DEX 头部各 ID 数量一致；SHA-256 不同 |
| `classes4.dex` | `classes5.dex` | 大小及 DEX 头部各 ID 数量一致；SHA-256 不同 |
| `classes5.dex` | `classes6.dex` | 大小及 DEX 头部各 ID 数量一致；SHA-256 不同 |
| `classes6.dex` | `classes7.dex` | SHA-256 完全相同，确认字节一致 |
| `classes7.dex` | 无 | ZIP 中未发现对应的 840-byte DEX |
| `assets/AdDex.4.0.1.dex` | 无 | ZIP 中未发现对应项 |
| `assets/A3AEECD8.dex` | 无 | ZIP 中未发现对应项 |

ZIP 另有一个 11,193,804-byte 的 `classes3.dex`。它的标准 DEX 头部声明的 string/type/proto/field/method/class_def 数量分别为 874/74/345/23/497/7；文件大小远大于其 DEX 头部映射区所需规模。该现象需要进一步检查文件尾部、映射表和实际类定义，当前不能仅凭文件名或大小判断它是完整代码、壳数据还是拼接载荷。

原版 APK 的 `assets/A3AEECD8.dex` 不是标准 DEX magic，且 APK 同时包含 `assets/libjiagu.so`。这说明原版还存在不能直接按普通 DEX 处理的载荷；本轮没有尝试绕过壳保护或访问控制。

## 4. 当前能确认的类与页面证据

从原版 APK 标准 DEX 字符串表中，至少确认：

- `classes.dex`：`ADBlockUtils.java`、`FavoritesFolderEntity.java`、`SearchResultActivity` 类描述等。
- `classes2.dex`：`FavoritesFolderActivity.java`、`FavoritesFolderDialog.java`、`FavoritesFolderManager.java`、`FavoritesRenameDialog.java`、`FavoritesPopupMore.java`、`MainActivity` 及相关匿名内部类。
- APK 资源目录中存在收藏夹文件夹、收藏网格、重命名弹窗、首页、搜索、搜索结果、详情、目录和阅读器等编译布局资源。完整页面差距仍以 `badcat-apk-reverse-engineering-comparison.md` 为主报告。

配套 ZIP 的前两个 DEX 可读出与 APK 对应 DEX 相同的字符串数量和部分相同类名；但二进制哈希不同。该 ZIP 是 DEX 归档，不是可直接打开的 Java/Kotlin 源码工程：其中没有 Gradle 工程结构、源代码目录或资源源码树。

## 5. 上传范围与限制

本次可确认的原始二进制文件仍保存在用户上传文件的 Library/会话材料中。本轮 GitHub 连接器可写入 UTF-8 文本文件和 Git blob，但当前执行环境无法把这两个大型本地二进制文件直接流式传入 GitHub blob API；因此**没有声称 APK 或 ZIP 已上传到仓库**。仓库先保存本核对报告和哈希/结构证据，避免把 Base64 文本伪装成可直接安装或解压的原文件。

下一步如需把原始二进制本体放进私有仓库，需要可访问的二进制上传通道（例如 Git LFS 或 release asset 上传能力）。在此之前，这份记录足以让后续对比工作引用确切版本和 SHA-256，避免误用不同 APK/ZIP。

## 6. 结论

1. 原版 APK 与 ZIP 的主要 DEX 具有可建立的编号映射，但除一个 284-byte DEX 外，哈希均不相同；不能把 ZIP 直接当作 APK 的逐字节解包副本。
2. ZIP 没有提供 Java/Kotlin 源码；它提供的是 DEX 二进制材料。
3. 现在可以把 ZIP 当作第二份独立 DEX 样本，进一步比较字符串、类定义和方法调用线索。
4. 反编译方法体、资源 XML 解码、UI 截图级对照仍未完成；不会将静态字符串证据写成已确认的运行行为。
