# 原版参考材料索引

本目录收录用户授权用于本项目对照的静态审计资料、构建辅助源码和资源清单。原始 APK/ZIP/JPG 二进制仍保留在用户上传文件库，当前 GitHub 工具链没有本地大文件字节上传通道；请以 SHA-256 为准核对原件。

## 恢复/校验工具源码

- [build_unwrapped.py](recovery-tools/build_unwrapped.py)：基于 Apktool 和 DEX ZIP 的隔离重建脚本；只输出新文件，不覆盖输入。
- [StubApp_compat.smali](recovery-tools/StubApp_compat.smali)：StubApp 兼容类适配源码；静态兼容方案，仍需真机验证。
- [recalc_dex_header.py](recovery-tools/recalc_dex_header.py)：重算 DEX SHA-1 signature 与 Adler-32，不负责修复非法字节码。

## 静态审计报告

- [第一阶段静态审计](audits/phase1-static-audit.md)
- [第一阶段脚本使用说明](audits/phase1-usage.md)
- [第二阶段静态验收记录](audits/phase2-static-acceptance.md)
- [第十一轮阅读/书源/净化资源清单](audits/round11-reading-source-purification-inventory.md)
- [第十二轮页面布局资源映射核查](audits/round12-layout-resource-mapping.md)

## 对照报告

- [APK、DEX 归档、重建包与原版 UI 对照](../docs/reference-build-chain-and-visual-diff.md)
- [DEX 归档核对更正](../docs/reference-archive-comparison-2026-11-16.md)

## 二进制原件 SHA-256

| 文件 | SHA-256 |
|---|---|
| 原版 24.6 MB APK | `a62c16d99547854ff313f229649762f2d7898b4e2e0dc66574df4c43fa62480e` |
| 旧壳 22.46 MB APK | `fb35e9e4c876e5cb99e00f7010a40c2f060d20b2aa392500983862ab7e91ff33` |
| DEX ZIP | `f747fa1c684ef568e3da1d857dbebdcbb7687e2db4c58a4390282948f5681d10` |
| 重建未签名 APK | `a3fd8273e79efe76f3b6a3662b03ec6870e76741ead60da36edc176533afcbb7` |
| 重建 v1 测试签名 APK | `4862de046e50a19fdbf0acd0a567a23c2bf8acaf3b8006445525a7e16e24ba8e` |
