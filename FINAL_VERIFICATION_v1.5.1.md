# CuotiPrint Android v1.5.1 FINAL-VERIFIED — 最终验证报告

日期：2026-08-11

## 结论

本轮不是只做 UI 字符串检查，而是从 Android 构建结构、Java 解析、资源/Manifest、核心协议、纸张排版、二维码/条码规则、批量线程安全、真实 Bluetooth SPP 输出路径和所有主要功能入口做了最终验证。

**源码级结论：PASS。当前没有发现已知的源码语法/结构型编译阻断；所有主要用户功能都存在实际数据处理/渲染/持久化/蓝牙输出代码路径，不是只显示按钮或 Toast 的空壳 UI。**

仍然不能伪造的两项验证：
1. 当前本地容器没有 Android SDK / AAPT2 / D8 / Gradle，因此完整 `assembleDebug` 必须以 GitHub Actions 的绿色通过为最终 Android 编译结论。
2. 当前环境没有实体 Qring/BeePrt 打印机，因此真实纸张输出、具体固件兼容、走纸机械偏移和 ACK 必须由实体机器最终确认。

## 本轮发现并修复的真实运行风险

最终审计时额外发现了 4 个不是 UI 问题的运行风险，并已经修复：

1. `printGenerated()` 曾在后台线程直接打开 Android 打印预览 Dialog，存在 `CalledFromWrongThreadException` 风险；现在首条 Raster 仍在后台生成，但 Dialog 强制切回主线程。
2. Office 全量批打的后台 Raster 工厂曾读取 SeekBar 的字号值；现在进入任务前把字号/阈值/抖动模式做不可变快照。
3. Excel/CSV 批打曾在后台工厂读取 Activity 当前的 `table` 字段；若用户打印中重新导入表格可能换数据源。现在批打开始时快照当前 Table。
4. PDF 当前页分割批打曾长期持有 Activity 的 `current Bitmap`；用户切页可能回收该 Bitmap。现在每段从稳定 URI + 页码 + RenderConfig 重新渲染，不再依赖可变 Activity Bitmap。

这些风险同时写入 `tools/release_audit.py`，之后 GitHub Actions 会在构建前自动检查，避免回归。

## 编译阻断检查

- Java 主源码：51 个
- JUnit 测试源码：5 个
- Java 17 parser 实际解析：56 / 56，syntax errors = 0
- `javac` 无 Android SDK 诊断筛查：
  - `; expected` = 0
  - `illegal start` = 0
  - `unclosed string` = 0
  - `reached end of file while parsing` = 0
  - `non-static method ... from static context` = 0
  - `incompatible types` = 0
  - `cannot be applied to given types` = 0
  - duplicate definition = 0
- Resource XML：全部可解析
- Manifest Activity：26 / 26 有对应 Java 类
- Activity Java 类：26 / 26 已写入 Manifest
- App 自有 `R.*` 引用：0 缺失
- native `.so/.aar`：0
- 源码目录误带 `.class`：0
- TODO / FIXME / UnsupportedOperationException / “暂未实现 / 仅界面 / 模拟打印”空壳标记：0

## Android 构建链检查

项目配置：
- AGP 9.1.1
- Gradle 9.3.1
- JDK 17
- compileSdk 36
- targetSdk 36
- minSdk 21
- Build Tools 36.0.0
- applicationId `com.yiran168.cuotiprint`
- versionCode 151 / versionName 1.5.1

GitHub Actions 会依次执行：
`release_audit.py -> :app:testDebugUnitTest -> :app:lintDebug -> :app:assembleDebug`

## 不是空壳：21 条实际功能链路检查

以下入口都检查到真实实现，而非只有 UI：

1. 文本：文字排版 -> Bitmap -> 1-bit Raster -> 最终预览 -> PrinterManager
2. 图片：文件解码 -> 8 种抖动 -> Raster -> 最终预览 -> PrinterManager
3. PDF：PdfRenderer -> 裁切/旋转/分段 -> Raster -> 单页/批量 PrinterManager
4. Office：DOCX/PPTX/XLSX/CSV/TXT 本地解析 -> 文字版式 -> Raster -> 打印
5. QR：ZXing QR + UTF-8 + L/M/Q/H -> quiet zone -> Raster -> 打印
6. 条形码：一维/二维分类 -> 制式校验 -> ZXing -> quiet zone -> Raster -> 打印
7. 自定义画布：文字/图片/QR/条码/图形 -> 拖拽/缩放/XYWH/字体/粗细 -> 逐元素二值化 -> 打印
8. Excel/CSV 批打：DataTableReader -> 变量替换 -> 流式生成 Raster -> 暂停/续打
9. 序列批打：2/10/16/26/36 进制 -> 文字/QR/Code128 -> 流式打印
10. 扫码打印：图像解码 -> 识别 -> 重新生成标签 -> 打印
11. 商品库：本地商品数据 -> 搜索/扫码 -> 标签生成 -> 打印
12. 网页：WebView -> 页面渲染 -> Raster -> 打印
13. 内置模板：40 张 Canvas 模板实际生成 -> 预览 -> 参数 -> 打印
14. 我的模板：本地持久化 Bitmap -> 预览 -> 重打
15. 模板码：模板二维码/数据 -> 预览 -> 打印
16. 历史：保存 Raster/Bitmap -> 缩略图 -> 原 Raster 优先重打
17. 诊断：384-dot 测试页 -> PrinterManager -> 实机
18. 设备：经典蓝牙扫描/配对 -> RFCOMM SPP -> 自动重连
19. 备份：ZIP 导出/导入 -> 设置/模板/历史/商品库
20. 纸张设置：连续纸自动长度 / 固定标签长宽 / 内容宽度 / XY 校准 -> 同一最终 Raster
21. 设置：外观/触感/打印音效/浓度/份数等真实 SharedPreferences 并被打印/UI 层读取

21 / 21 功能路径检查通过。

## 真实打印协议路径

真实发送路径为：

`用户内容 -> RasterEncoder -> Ui 最终点阵确认 -> PrinterManager -> 状态预检 -> BluetoothSocket RFCOMM SPP -> OutputStream -> QringProtocol -> 1024-byte 分包 -> STOP -> 0xAA ACK / FF xx 故障帧`

关键协议常量测试：
- 384 dots
- 48 bytes / row
- 1024-byte chunk
- 1 ms chunk delay
- GS v 0 Raster Header
- ESC J 走纸拆包
- 缺纸/开盖/低电/过热状态位
- 浓度命令

纯 Java smoke：PASS。

## 纸张/标签与所见即所得

- 57mm 为耗材物理宽度；203DPI、384-dot 打印头的最大真实点阵宽度仍为 384 dots。
- 30mm 用户声明标签宽度换算为约 240 dots；20mm 为约 160 dots。
- 连续纸：实际黑色内容边界决定输出高度，可自动裁下部空白。
- 标签纸：用户声明宽度/固定长度，内容不够时按比例适配而不是偷偷裁掉。
- 预览和实际打印都调用同一 `applyPaperSettings()/layoutToMedia()`，不存在两套独立排版算法。
- QR/条码 `preserveMargins` 会保护静区，不受全局自动裁白边破坏。

## QR / 条码真实校验

实际执行 `BarcodeUtil.prepare()` smoke：PASS。
已覆盖 EAN-13、EAN-8、UPC-A、UPC-E、ITF、Code39、Codabar 的合法与非法输入；错误长度/字符会在 ZXing 前被用户友好的中文校验拦截。

## 打印音效

- 10 种固定本地 PCM 预设：全部非静音
- “随机使用 10 种”：真实随机选择预设
- “随机生成新音效”：真实程序即时合成 PCM
- AudioTrack 播放线程与打印 IO 线程分离
- 关闭音效不会创建 AudioTrack

纯 Java声音 pattern smoke：PASS。

## 最终判定

**源码实现：PASS，不是空壳。**
**发现的已知源码级编译阻断：0。**
**发现并已修复的最终线程/批量稳定性问题：4。**

只有两个结论必须等待外部事实：
- Android 真正 `assembleDebug`：以 GitHub Actions 绿色为准。
- 你的实体 57mm / 203DPI Qring/BeePrt：以诊断页和实际纸张输出为准。

只要 GitHub Actions 的 `test + lint + assembleDebug` 全绿，就可以把“Android 编译成功”从“高置信静态验证”升级为“真实构建验证”。实体打印机通过诊断页、文字、照片、QR、Code128、连续纸自动长度和固定标签 7 组实测后，才可以负责任地写成“该具体机型已全链路验证”。
