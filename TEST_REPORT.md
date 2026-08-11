# v1.5.1 检查报告

- 静态发布审计：见 `FINAL_AUDIT.txt`。
- 纸宽换算 smoke：57mm 用户纸宽受 384-dot 打印头上限约束；30mm -> 约 240 dots。
- 协议 invariants：384 dots、48 bytes/行、1024-byte 分包、1ms 包间隔。
- 预览/打印：最终确认和真实发送共用 `PrinterManager.applyPaperSettings()`。
- 连续纸：有效内容边界决定最终长度。
- 标签：固定宽度/长度、自动等比 fit、垂直对齐。
- 二维码/条码：独立页面、规则校验、合法示例、quiet zone 保护。

限制：当前运行容器无完整 Android SDK、无实体蓝牙打印机，因此 APK 编译由 GitHub Actions 完成，纸张输出、走纸量和扫码率需在目标打印机上真机确认。
