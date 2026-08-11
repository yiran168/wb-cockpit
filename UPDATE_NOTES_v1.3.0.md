# v1.3.0 更新说明

1. 模板头部由固定 54dp 改为 80dp 布局，副标题固定两行但保留完整可见区域；底部操作按钮增高。
2. 最终打印确认框内部改为 ScrollView；常用打印参数全部在同页可调整。
3. PrinterManager 的预览改为从原始 raster 重新应用当前纸张设置，避免预览重复套用或参数变化后不刷新。
4. 内置模板条码占位改为条纹示意和数字文本，不再出现实心黑色大块。
5. RasterEncoder 新增 Atkinson、Jarvis-Judice-Ninke、Sierra Lite、Stucki；连同原有 Floyd、Ordered、Bayer、Threshold 共 8 种。
6. 首页 feature card 使用缩放/淡入式进入动画；四个主 Tab 支持左右滑动，且多指手势不触发 Tab 切换。
7. 新增 ReleaseInfoActivity，突出 Thisko / QrintPrint 开源致谢和参考参数。
8. App 内不再出现品牌比较式文案，统一使用功能说明/使用说明。
