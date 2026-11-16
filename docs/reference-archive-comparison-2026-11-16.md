# DEX 归档核对补充与更正

核对日期：2026-11-16。

> 本文件的初版曾把 ZIP 中的 `classes3.dex` 误写为未定用途的附加 DEX。进一步对照旧壳 APK、重建未签名包和当前 24.6 MB APK 后，已确认它是旧壳 APK 的原始 `classes.dex`。请以 [APK、DEX 归档、重建包与原版界面对照](reference-build-chain-and-visual-diff.md) 为准。

## 更正后的 DEX 结构

- 旧壳 APK `坏坏猫_1.5.0.5(1).apk` 的唯一根 DEX 是 11,193,804-byte 的壳 DEX。
- DEX ZIP 的 `classes3.dex` 与该旧壳 APK 的 `classes.dex` 逐字节完全相同。
- ZIP 中真正的五个业务 DEX 是 `classes.dex`、`classes2.dex`、`classes4.dex`、`classes5.dex`、`classes6.dex`。
- ZIP `classes7.dex` 是 284-byte 的 `LEmpty;` 占位 DEX。
- 重建包把五个业务 DEX 放入 APK `classes.dex` 至 `classes5.dex`，把占位 DEX 放入 `classes6.dex`，并把新编译的 StubApp 兼容类放入 `classes7.dex`。

## 交叉验证结果

1. 重建 APK 中 `classes.dex` 至 `classes6.dex` 与 ZIP 映射来源在 byte offset 32 至 EOF 完全相同；DEX 头部 SHA-1 signature 和 Adler-32 字段经重算后有效。
2. 当前 24.6 MB APK 与重建未签名包的七个 DEX 文件哈希逐个完全相同。
3. 当前 APK 与重建未签名包仅在 `AndroidManifest.xml`、`resources.arsc` 内容上不同；当前 APK 另有原签名文件。
4. 重建测试签名包与未签名包的全部原有条目完全相同，只增加三项 JAR/v1 测试签名文件；`jarsigner -verify` 返回 `jar verified`。

## 原始材料哈希

- 原版 24.6 MB APK：`a62c16d99547854ff313f229649762f2d7898b4e2e0dc66574df4c43fa62480e`
- 旧壳 22.46 MB APK：`fb35e9e4c876e5cb99e00f7010a40c2f060d20b2aa392500983862ab7e91ff33`
- DEX ZIP：`f747fa1c684ef568e3da1d857dbebdcbb7687e2db4c58a4390282948f5681d10`
- 重建未签名 APK：`a3fd8273e79efe76f3b6a3662b03ec6870e76741ead60da36edc176533afcbb7`
- 重建测试签名 APK：`4862de046e50a19fdbf0acd0a567a23c2bf8acaf3b8006445525a7e16e24ba8e`

## 上传说明

私有仓库已保存构建脚本、兼容桩、DEX 头部工具和静态审计文档。原始 APK/ZIP/JPG 二进制本体仍在用户上传文件库；本轮没有把它们冒充为已提交到 GitHub。
