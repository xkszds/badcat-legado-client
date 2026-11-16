# 坏坏猫 v1.5.0.5：360 去壳第一阶段静态审计

## 范围与限制

本报告只依据本次上传的两个文件进行：

- `坏坏猫_1.5.0.5.apk`
- `com.hhm.verticalsearch.zip`

审计过程没有修改这两个原始文件。APK 与 DEX ZIP 均通过 ZIP 完整性检查。

当前执行环境没有 JADX、Apktool、smali/baksmali、D8 或 Android SDK 构建工具。因此本阶段完成的是容器结构、DEX 表、调用引用、Manifest 与 ELF 符号的静态核查；没有生成可安装 APK，也没有执行真机启动验证。

## 一、APK 基本信息

| 项目 | 实际检查结果 |
|---|---|
| Package | `com.hhm.verticalsearch` |
| versionCode | `150` |
| versionName | `1.5.0.5` |
| minSdkVersion | `15` |
| targetSdkVersion | `26` |
| Manifest Application | `com.stub.StubApp` |
| Launcher Activity | `com.search.verticalsearch.common.ui.activity.SplashActivity` |
| Activity 数量 | `114` |
| APK 中的 classes*.dex | 只有 `classes.dex`，其内容为 360 StubApp 壳 DEX |
| Native ABI | `lib/armeabi/`（本包检查到的是 ARM 32-bit 库） |

纠正先前审计中的错误：`libjiagu.so` 确实存在，路径是 `assets/libjiagu.so`，大小 689,400 bytes。它不是 `lib/` 根目录下的常规 native 库。

同时存在 `lib/armeabi/libA3AEECD8.so`，其中可见导出符号包括 `FakeDvmVerifyClass`、`FakeDvmResolveClass`、`GetAbsoluteEncryptJar`。因此壳相关 native 组件至少包括 assets 中的 `libjiagu.so` 和 lib 目录中的 `libA3AEECD8.so`。

## 二、ZIP 中 DEX 的真实分组

ZIP 中有 7 个 DEX。`classes3.dex` 与原 APK 的 `classes.dex` 逐字节一致，因此它是原壳 DEX，而不是另一份业务 DEX。

| ZIP 文件 | class_defs | method_ids | 角色判断 |
|---|---:|---:|---|
| `classes.dex` | 4,569 | 48,019 | 恢复出的业务 DEX 1 |
| `classes2.dex` | 6,281 | 48,006 | 恢复出的业务 DEX 2 |
| `classes3.dex` | 7 | 497 | 原 APK 的 360 StubApp 壳 DEX（逐字节相同） |
| `classes4.dex` | 3,026 | 22,695 | 恢复出的业务 DEX 3 |
| `classes5.dex` | 5,396 | 48,027 | 恢复出的业务 DEX 4 |
| `classes6.dex` | 2,597 | 35,922 | 恢复出的业务 DEX 5 |
| `classes7.dex` | 1 | 0 | 仅含 `LEmpty;` 的小型 DEX |

除原壳 DEX 外，六个业务 DEX 合计 **21,870 个 class definition**。静态比较没有发现这六个业务 DEX 之间存在重复类定义。

每个 DEX 的 method_ids 数量都小于 65,535；不要把六个业务 DEX 合并成单个 DEX，保持 MultiDex 分包更稳妥。

### DEX 头部状态

六个主要业务 DEX 以及原壳 DEX 的 `file_size` 字段与实际长度一致，Adler-32 校验值通过；但它们的 DEX SHA-1 signature 字段与重新计算的 SHA-1 不一致。284-byte 的 `classes7.dex` 两项均通过。

这不等于已经证明所有字节码都有效。最终重建时，应由 smali/D8 重新生成 DEX，或对最终 DEX 重新计算标准 SHA-1 与 Adler-32，并再执行结构检查。附带的 `recalc_dex_header.py` 只重算头部值，不负责修复坏掉的类数据或指令。

## 三、Application 与 JNI 启动链

### HuaYueApplication

恢复出的 `HuaYueApplication` 位于业务 `classes.dex`，其父类是 `com.reader.baselib.BaseApplication`。

`HuaYueApplication.attachBaseContext(Context)` 的实际指令顺序为：

1. `invoke-super BaseApplication.attachBaseContext(Context)`
2. `MultiDex.install(Context)`
3. `return-void`

该方法本身没有直接调用 `StubApp`。这为把 Manifest 的 Application 入口改成 `HuaYueApplication` 提供了可行依据，但仍需验证父类和完整应用初始化。

`BaseApplication.onCreate()` 调用 `a()` 是虚调用；`HuaYueApplication` 覆写的 `a()V` 会调用 `JNIVerify.getSecret()`，随后继续执行 `e()`。所以 JNI 初始化路径必须保留。

### JNIVerify

`JNIVerify.<clinit>` 调用 `System.loadLibrary("verify")`。`JNIVerify.getSecret()` 是 `public static native String`。APK 的 `lib/armeabi/libverify.so` 确实导出：

`Java_com_hhmss_jni_JNIVerify_getSecret`

因此 `libverify.so` 不能按“360 壳库”处理，也不能删除。当前静态分析没有证明 `getSecret()` 的 native 逻辑是否检查包签名；需要在重签名后的测试包上观察结果。

另外业务代码中还有 `MainActivity.r()` 调用 `JNIVerify.getSecret()`。

## 四、StubApp 的调用点统计

对六个业务 DEX 的代码指令做了调用扫描，得到以下引用：

| StubApp 方法 | 调用指令数 | 主要调用位置 |
|---|---:|---|
| `getOrigApplicationContext(Context)` | 538 | Application 初始化、图像/视频/网络等业务和依赖库 |
| `interface11(int)` | 125 | 这 125 次全部位于类的 `<clinit>` 静态初始化方法 |
| `interface22(int, String[], int[])` | 10 | 这 10 次位于 `onRequestPermissionsResult` |
| `mark()` | 1 | `SplashActivity.onCreate()` |
| **总计** | **674** | — |

没有发现业务 DEX 直接读取 `StubApp` 的静态字段。调用扫描中发现的 `StubApp` 直接方法目标只有上述四个签名。

这支持在重建版中保留一个轻量的 `com.stub.StubApp` 兼容类，而不是把所有调用点逐一删除。附件 `StubApp_compat.smali` 是待编译的适配桩：

- `getOrigApplicationContext(context)` 原样返回传入的 Context；
- `interface11(int)` 空实现；
- `interface22(int, String[], int[])` 空实现；
- `mark()` 空实现。

这是基于静态调用形态提出的适配方案，不是已经通过真机验证的结论。应先生成测试包并检查启动、权限回调、相关 SDK 行为。

## 五、Native 库处理策略

首轮构建不要批量删 SO。优先保留原 APK 的整个 `lib/armeabi/` 目录，至少已明确需要关注：

- `libverify.so`：被 `JNIVerify` 在应用启动时加载；
- `libtobEmbedEncrypt.so`：导出 `TTEncryptUtils` 对应的 `ttEncrypt` / `ttDecrypt` JNI 符号；
- 其余原有播放、音频、加密、签名和厂商 SDK 库：在完成真实加载关系核对前先保留。

首轮也暂时保留 `assets/libjiagu.so` 和 `libA3AEECD8.so` 文件本身，但新 Application/兼容 StubApp 不应再执行原壳的加载初始化。确认新包能够启动且无代码依赖后，再单独制作清理版并进行 A/B 测试。

## 六、建议的首轮重建布局

不合并六个业务 DEX。建议输出 APK 中按顺序放置：

| 输出 APK DEX 名 | 输入来源 |
|---|---|
| `classes.dex` | ZIP `classes.dex` |
| `classes2.dex` | ZIP `classes2.dex` |
| `classes3.dex` | ZIP `classes4.dex` |
| `classes4.dex` | ZIP `classes5.dex` |
| `classes5.dex` | ZIP `classes6.dex` |
| `classes6.dex` | ZIP `classes7.dex`（保留 `LEmpty;`） |
| `classes7.dex` | 由 `StubApp_compat.smali` 编译的新兼容 DEX |

不把 ZIP `classes3.dex`（原壳 DEX）放进新包。Manifest 中将 `application android:name` 从 `com.stub.StubApp` 改为 `com.search.verticalsearch.common.HuaYueApplication`。其余资源、组件声明、权限、provider authority 和原生库首轮都先保留。

## 七、签名与用户数据保护

APK 的 `META-INF/HHM.RSA` 证书主题为 `CN=hhm`，SHA-256 指纹：

`F9:E0:52:9E:65:8F:F1:53:3D:93:21:FB:5F:CC:37:26:25:D9:85:3B:3C:F4:FA:26:26:71:4C:08:05:F3:07:E9`

本次只有公钥证书，没有对应私钥。若使用新的测试密钥签名，Android 不允许它直接覆盖同包名、不同签名的已安装版本。

因此：

1. 不要卸载当前正式版，不要覆盖 `/data/data/com.hhm.verticalsearch/`；
2. 第一轮在备用手机/测试设备上测试，或用单独的测试包名做隔离测试；
3. 需要迁移原书架和登录数据时，先有独立备份，再考虑最终替换；
4. 只有拿到原签名私钥，才能正常以同包名升级且保持 Android 签名身份。

## 八、当前完成状态

已完成：APK/ZIP 完整性、Manifest、DEX 表与类定义统计、重复类检查、StubApp 调用扫描、HuaYueApplication 生命周期入口核对、JNIVerify 与 ELF 导出符号对应。

已另外准备首轮构建脚本和使用说明。脚本通过 Python 语法检查，并用模拟 Apktool 的输入/输出流程验证了 DEX 重排、Manifest 替换和 ZIP 归档逻辑；这不是实际 Apktool 回编译。尚未完成：真实 smali 编译、DEX 重排与签名修复、APK 重建、签名、安装与 logcat 验证。当前环境缺少 Apktool、smali/baksmali、D8、zipalign 和 apksigner，也无法访问用户手机或云服务器。

**下一阶段重点：把 `StubApp_compat.smali` 编译为独立 DEX，按上表重排六个业务 DEX，修改 Manifest 为 HuaYueApplication，保留全部原资源和 native 库，生成隔离测试 APK。只有测试通过后再考虑清理壳文件。**