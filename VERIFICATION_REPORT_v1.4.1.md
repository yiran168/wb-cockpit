# CuotiPrint v1.4.1 VERIFIED — 编译与功能核查报告

- 自动结构/功能检查：83 项
- 通过：83
- 失败：0
- Java 源文件：49
- Manifest Activity：26
- XML/Manifest：16

## 关键结论

源码不是空壳 UI：打印入口最终进入 `PrinterManager`，该类持有真实 `BluetoothSocket/InputStream/OutputStream`，执行 SPP RFCOMM 连接、状态预检、384-dot 点阵分包发送和完成 ACK 等待；最终预览和实际发送都走同一套纸张排版变换。

## 主要功能代码路径

| 功能 | 状态 | 代码入口 |
|---|---|---|
| 文字打印 | PASS | `TextPrintActivity.java` |
| 图片打印/8种抖动 | PASS | `ImagePrintActivity.java` |
| PDF真实渲染 | PASS | `PdfPrintActivity.java` |
| Office导入/热敏排版 | PASS | `OfficePrintActivity.java` |
| 网页WebView渲染 | PASS | `WebPrintActivity.java` |
| 独立二维码 | PASS | `QrCodeActivity.java` |
| 独立条形码 | PASS | `BarcodePrintActivity.java` |
| 自定义画布+一维条码 | PASS | `CanvasEditorActivity.java` |
| 数据批打 | PASS | `BatchPrintActivity.java` |
| 序列批打 | PASS | `SerialPrintActivity.java` |
| 扫码识别 | PASS | `ScanCodeActivity.java` |
| 商品数据库 | PASS | `ProductDatabaseActivity.java` |
| 内置模板 | PASS | `BuiltInTemplateActivity.java` |
| 历史重打 | PASS | `HistoryActivity.java` |
| 备份恢复 | PASS | `BackupActivity.java` |
| 蓝牙设备选择 | PASS | `DevicePickerActivity.java` |
| 诊断测试页 | PASS | `DiagnosticsActivity.java` |

## 自动检查明细

- [x] XML resources/manifest parse: 16 files
- [x] Activity source exists: ReleaseInfoActivity
- [x] Activity source exists: OfficePrintActivity
- [x] Activity source exists: TemplateCodeActivity
- [x] Activity source exists: MyDevicesActivity
- [x] Activity source exists: DiagnosticsActivity
- [x] Activity source exists: BackupActivity
- [x] Activity source exists: ProductDatabaseActivity
- [x] Activity source exists: BuiltInTemplateActivity
- [x] Activity source exists: PaperSettingsActivity
- [x] Activity source exists: WebPrintActivity
- [x] Activity source exists: ScanCodeActivity
- [x] Activity source exists: SerialPrintActivity
- [x] Activity source exists: BatchPrintActivity
- [x] Activity source exists: DevicePickerActivity
- [x] Activity source exists: CanvasEditorActivity
- [x] Activity source exists: TemplateActivity
- [x] Activity source exists: HistoryActivity
- [x] Activity source exists: SettingsActivity
- [x] Activity source exists: CodePrintActivity
- [x] Activity source exists: QrCodeActivity
- [x] Activity source exists: BarcodePrintActivity
- [x] Activity source exists: PdfPrintActivity
- [x] Activity source exists: ImagePrintActivity
- [x] Activity source exists: TextPrintActivity
- [x] Activity source exists: SplashActivity
- [x] Activity source exists: MainActivity
- [x] All local R.* references resolve
- [x] SharedPreferences write-only keys: 0
- [x] SharedPreferences read-only keys: 0
- [x] No implementation-stub/TODO/FIXME markers in app Java
- [x] No empty click listeners found
- [x] Activities with buttons have click wiring
- [x] Functional path present: 文字打印
- [x] Functional path present: 图片打印/8种抖动
- [x] Functional path present: PDF真实渲染
- [x] Functional path present: Office导入/热敏排版
- [x] Functional path present: 网页WebView渲染
- [x] Functional path present: 独立二维码
- [x] Functional path present: 独立条形码
- [x] Functional path present: 自定义画布+一维条码
- [x] Functional path present: 数据批打
- [x] Functional path present: 序列批打
- [x] Functional path present: 扫码识别
- [x] Functional path present: 商品数据库
- [x] Functional path present: 内置模板
- [x] Functional path present: 历史重打
- [x] Functional path present: 备份恢复
- [x] Functional path present: 蓝牙设备选择
- [x] Functional path present: 诊断测试页
- [x] Real printer path: BluetoothSocket
- [x] Real printer path: getOutputStream()
- [x] Real printer path: getInputStream()
- [x] Real printer path: createRfcommSocketToServiceRecord
- [x] Real printer path: createInsecureRfcommSocketToServiceRecord
- [x] Real printer path: writeChunked
- [x] Real printer path: waitAck
- [x] Real printer path: preflightInternal
- [x] Real printer path: showPrintPreview
- [x] Real printer path: applyPaperSettings
- [x] Final preview and actual output share media-layout transform
- [x] Custom canvas code quiet zones preserved
- [x] Custom canvas has validated 1D barcode insertion
- [x] Template QR print preserves quiet zone
- [x] Serial QR/Code128 preserves quiet zone
- [x] targetSdk36 does not declare Android 17-only local-network permission
- [x] Future LAN permission gate checks targetSdk >=37
- [x] Build config: compileSdk 36
- [x] Build config: targetSdk 36
- [x] Build config: minSdk 21
- [x] Build config: versionCode 141
- [x] Build config: versionName "1.4.1"
- [x] Build config: com.google.zxing:core:3.5.4
- [x] AGP 9.1.1 configured
- [x] GitHub workflow: gradle-version: '9.3.1'
- [x] GitHub workflow: java-version: '17'
- [x] GitHub workflow: platforms;android-36
- [x] GitHub workflow: build-tools;36.0.0
- [x] GitHub workflow: :app:testDebugUnitTest
- [x] GitHub workflow: :app:lintDebug
- [x] GitHub workflow: :app:assembleDebug
- [x] Obsolete android.useAndroidX=false removed
- [x] No suspicious Java compiler categories beyond missing Android/ZXing classpath

## 已执行的纯算法/协议测试

- Qring 核心协议：384 dots、48 bytes/row、1024-byte 分包、1ms 间隔、Raster header、走纸拆包、浓度、关机时间、状态解析：PASS。
- 203DPI 纸宽换算：57mm 被物理打印头限制为 384 dots；30mm→240 dots；384 dots≈48.05mm 可打印宽：PASS。
- 媒体排版：连续纸自动长度裁白、自动内容宽度、固定标签精确高度、固定标签不裁内容、preserveMargins、X 偏移、180°：PASS。
- 条码输入预校验：EAN-13/EAN-8/UPC-A/UPC-E/ITF/Code39/Code93/Code128/Codabar/Data Matrix/PDF417/Aztec：合法样例 PASS，非法位数/字符拒绝 PASS。

## 仍需真实环境确认

- Android 5.0+ only: minSdk 21; Android 4.x cannot be supported by this build.
- Office is real document parsing + thermal-text rendering, not pixel-perfect Microsoft Office page-layout reproduction.
- Camera scan uses system camera/photo + ZXing decode; it is not a continuous live-camera scanner.
- Physical printer output/ACK timing must still be verified on the actual 57mm/203DPI printer.
- This container has no Android SDK/Gradle, so the definitive APK compile gate remains GitHub Actions :app:testDebugUnitTest :app:lintDebug :app:assembleDebug.

## 结果

**PASS：当前源码未发现已知静态编译阻塞或“只有按钮没有实现”的空壳路径。最终 APK 仍应以 GitHub Actions 绿色构建 + 你的实体打印机回归为准。**