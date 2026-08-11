# 预览覆盖

强制检查的 18 个页面：Text、Image、PDF、Office、Code、Canvas、Batch、Serial、Scan、Product、Web、Template、BuiltInTemplate、TemplateCode、History、Diagnostics、PaperSettings、Settings。

所有打印入口还必须经过 `PrinterManager -> Ui.showPrintPreview()` 的最终 384-dot 黑白 Raster 确认门。位图预览统一使用 `ZoomablePreviewView`，支持双指缩放、拖动和双击复位。
