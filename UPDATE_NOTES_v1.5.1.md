# CuotiPrint Android v1.5.1 FINAL-VERIFIED

这是 v1.5.0 的最终稳定性核查修订版，不增加空壳功能，重点修复批量打印线程与可变状态风险：

- 修复后台线程直接打开最终打印 Dialog。
- Office 批量打印在任务开始前固定字号/阈值/抖动参数。
- Excel/CSV 批打固定本次数据表快照，避免打印中更换数据源影响任务。
- PDF 当前页分段打印改为稳定 URI + 页码 + RenderConfig 重渲染，不再长期持有 Activity 当前 Bitmap。
- 发布审计增加上述 4 条回归门槛。
- 重新完成 Java parser、XML/Manifest/R、21 条功能路径、协议/纸张/序列/声音、Barcode 规则验证。
