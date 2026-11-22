# 坏坏猫 1.5.0.5 第二阶段静态验收记录

## 已完成

- 对未签名 APK 执行 ZIP CRC 完整性检查：通过。
- 解析二进制 AndroidManifest.xml：包名 `com.hhm.verticalsearch`，版本 `1.5.0.5`，Application 为 `com.search.verticalsearch.common.HuaYueApplication`。
- APK 含 7 个根目录 DEX；全部 DEX 的 magic、file_size、SHA-1 signature、Adler-32 校验均通过。
- DEX 类定义总数为 21,871，未发现重复类定义（21,870 个业务类 + 1 个 StubApp 兼容类）。
- `classes7.dex` 定义 `com.stub.StubApp`；业务 DEX 对该兼容类的引用需要保留，不能贸然删除。
- 已生成单独测试签名 APK，使用临时自签名 RSA 证书进行 JAR/v1 签名；`jarsigner -verify` 返回 `jar verified`。
- 签名后的 APK 再次通过 ZIP CRC 检查，7 个 DEX 头部校验仍通过。

## 尚未清理的加固残留

以下内容仍在 APK 中：

- `assets/libjiagu.so`
- `assets/A3AEECD8.dex`
- `lib/armeabi/libA3AEECD8.so`
- `classes5.dex` 中的 `com.qihoo.jiagu.*` 与 `com.qihoo.jiagutracker.*` 类
- DEX 字符串中仍出现 `libjiagu.so`、`initVerify`、`.jiagu.lock` 等标记
- `libA3AEECD8.so` 导出 `FakeDvmVerifyClass`、`FakeDvmResolveClass`、`GetAbsoluteEncryptJar`、`JNI_OnLoad` 等符号

这些是强烈的加固残留证据，但静态存在不等于运行时一定加载。当前没有足够证据证明删除它们不会触发类加载、反射、JNI 或初始化崩溃，因此本轮没有删除它们，也没有批量删除任何原生库。

## 签名包说明

- 文件：`坏坏猫_1.5.0.5_test_signed_v1.apk`
- SHA-256：`4862de046e50a19fdbf0acd0a567a23c2bf8acaf3b8006445525a7e16e24ba8e`
- 签名类型：JAR/v1，RSA 2048，SHA-256。
- 签名证书为临时自签名测试证书，不是原版 `CN=hhm` 证书。
- 当前环境没有 `apksigner` 和 `zipalign`，因此没有生成 v2/v3 签名，也没有执行 zipalign。
- 该包仅供隔离测试，不可作为正式发布包，也不能覆盖安装原版。

## 尚未完成的验证

当前环境无法连接用户手机或云服务器，没有 Android 运行时与 ADB，因此无法完成：
1. 安装后冷启动、二次启动与登录流程测试；
2. `HuaYueApplication` / `BaseApplication` 初始化及 `JNIVerify.getSecret()` 运行验证；
3. 权限请求回调、播放器、图片/视频、书架和搜索功能回归；
4. logcat 中 `UnsatisfiedLinkError`、`ClassNotFoundException`、`VerifyError`、`SecurityException` 等异常检查；
5. 运行时确认 `libjiagu.so` / `libA3AEECD8.so` 是否被加载，再决定是否制作清理版。

## 测试安全要求

- 不要在装有原版的主力手机上覆盖安装。
- 先在没有安装原版的备用设备上测试；同包名不同签名无法作为原版升级安装。
- 不要卸载原版，不要删除或覆盖原版应用数据目录。
- 若测试失败，提供完整启动阶段 logcat（从 `ActivityManager` 启动该包前后开始），并说明设备 Android 版本和 CPU ABI。