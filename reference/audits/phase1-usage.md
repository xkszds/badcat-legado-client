# 坏坏猫 v1.5.0.5：首轮重建脚本使用说明

本包包含静态审计报告、StubApp 兼容桩源码、DEX 头部校验工具和 Apktool 构建脚本。

## 重要边界

- 这是**首轮隔离测试构建脚本**，不是已完成、已签名或已验证可运行的 APK。
- 脚本不会修改原始 APK 和 DEX ZIP；输出到新文件。
- 脚本会保留 APK 中原有的资源、Manifest 其他项目、`assets/libjiagu.so`、`libA3AEECD8.so` 以及原有 native 库；首轮不清理壳资源。
- Manifest 的 Application 入口会改为 `HuaYueApplication`，并把原壳 `StubApp` 替换为 `StubApp_compat.smali` 所定义的兼容类。
- 兼容桩的 no-op 行为仍需真机验证；不能把静态调用分析当作运行通过的证明。
- 新签名与原 APK 的签名证书不同，不能覆盖原安装版。不要卸载主手机里的正式版，也不要清除 `/data/data/com.hhm.verticalsearch/`。

## 构建环境

需要：

1. Java 运行环境（Apktool 官方安装文档要求至少 Java 8）；
2. 可从命令行调用的 Apktool（建议使用当前 3.x 版本，支持 `smali_classes7` 多 DEX 目录）；
3. Python 3.9 或更新版本；
4. 签名阶段需要 Android SDK Build Tools 中的 `zipalign` 和 `apksigner`。

截至本说明编写时，Apktool 官方发布页列出的最新版本为 3.0.3。Linux 可按官方安装页下载 jar 并配置命令包装器；下面给出一种示例安装方式（服务器需要能访问 GitHub）：

```bash
sudo apt-get update
sudo apt-get install -y openjdk-17-jre-headless python3 curl unzip
sudo mkdir -p /usr/local/lib/apktool
sudo curl -fL \
  https://github.com/iBotPeaches/Apktool/releases/download/v3.0.3/apktool_3.0.3.jar \
  -o /usr/local/lib/apktool/apktool.jar
printf '#!/bin/sh\nexec java -jar /usr/local/lib/apktool/apktool.jar "$@"\n' \
  | sudo tee /usr/local/bin/apktool >/dev/null
sudo chmod +x /usr/local/bin/apktool
apktool --version
```

如服务器无法访问 GitHub，按 Apktool 官方安装页的 Bitbucket 下载入口获取同版本 jar，再放到上述位置。

## 生成未签名测试 APK

将以下文件放在同一目录：

- `build_unwrapped.py`
- `StubApp_compat.smali`
- 原始 `坏坏猫_1.5.0.5.apk`
- 原始 `com.hhm.verticalsearch.zip`

在该目录运行：

```bash
python3 build_unwrapped.py \
  --apk "坏坏猫_1.5.0.5.apk" \
  --dex-zip "com.hhm.verticalsearch.zip" \
  --out "坏坏猫_1.5.0.5_unwrapped_unsigned.apk"
```

构建布局：

- APK `classes.dex` ← Epic ZIP `classes.dex`
- APK `classes2.dex` ← Epic ZIP `classes2.dex`
- APK `classes3.dex` ← Epic ZIP `classes4.dex`
- APK `classes4.dex` ← Epic ZIP `classes5.dex`
- APK `classes5.dex` ← Epic ZIP `classes6.dex`
- APK `classes6.dex` ← Epic ZIP `classes7.dex`
- APK `classes7.dex` ← 用 Apktool 从 `StubApp_compat.smali` 编译出的 DEX

Epic ZIP `classes3.dex` 是原 APK 壳 DEX，不复制到重建包。

## 对齐并用独立测试密钥签名

以下命令只用于隔离测试。不要把测试密钥当成原应用签名密钥。

```bash
keytool -genkeypair -v \
  -keystore badcat-test.jks \
  -alias badcat-test \
  -keyalg RSA -keysize 2048 -validity 10000

zipalign -p -f 4 \
  "坏坏猫_1.5.0.5_unwrapped_unsigned.apk" \
  "坏坏猫_1.5.0.5_unwrapped_aligned.apk"

apksigner sign \
  --ks badcat-test.jks \
  --ks-key-alias badcat-test \
  --out "坏坏猫_1.5.0.5_unwrapped_test.apk" \
  "坏坏猫_1.5.0.5_unwrapped_aligned.apk"

apksigner verify --verbose --print-certs \
  "坏坏猫_1.5.0.5_unwrapped_test.apk"
```

先在**没有安装正式版、没有重要数据的备用测试设备**上安装。此脚本保留原包名 `com.hhm.verticalsearch`，所以与正式版同包名；不同签名会导致无法覆盖正式版，也不能在同一台设备上与正式版并存。

## 验证顺序

1. 检查 APK ZIP 完整性及是否包含 `classes.dex` 到 `classes7.dex`。
2. 检查 Manifest 的 Application 是否是 `HuaYueApplication`。
3. 安装后记录完整启动日志（`logcat`），依次验证冷启动、登录、书架、书籍详情、搜索、权限申请、阅读器和网络请求。
4. 若崩溃，保存崩溃堆栈及发生步骤；不要直接删除 native 库或再改多个变量。
5. 只有首轮测试通过后，才讨论清理 `assets/libjiagu.so` 或 `libA3AEECD8.so`。

## 当前尚未做的事

当前执行环境没有安装 Apktool、smali/baksmali、D8、`zipalign` 或 `apksigner`，也不能访问你的手机。因此此处没有生成、签名或安装测试 APK。脚本已通过 Python 语法检查，并使用模拟 Apktool 的输入/输出流程验证了 DEX 重排、Manifest 替换和 ZIP 归档逻辑；这不等于真实 Apktool 回编译，也不代表最终去壳成功。