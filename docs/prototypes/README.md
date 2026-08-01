# Lucky3D 已确认交互原型

本目录保存产品负责人已经确认、可脱离 Codex 内嵌预览独立打开的交互原型。
原型是后续 Jetpack Compose 页面实现和设计 QA 的基准，不是可直接打包进 Android
应用的 Web 页面。

## 走势图

- 文件：`lucky3d-trend-approved-interactive.html`
- 确认时间：2026-08-01
- 状态：已确认，禁止在原文件上继续试验或覆盖；新方案必须另存新文件。
- 来源：本对话中的 `lucky3d-long-scroll-trend-touch-fixed.html`，使用同一版本的
  Codex 可视化渲染器导出为完整独立网页。
- SHA-256：`45BA1A4BF3A3902E56031D7DB6AE53EC1484B680B9CBCD115ACC8BF868B520E6`

后续实现应以该网页核对走势图的整体视觉、信息密度、纵向长页、横向查看位置走势、
期数选择、两期未开奖空白区和纵横手势分流。网页中的“柔光晶体”切换仅用于当时的
方案对比；Android 走势图固定采用“流动朱砂”。真实字段、列顺序、计算口径和数据来源
仍以 `docs/design-spec.md`、`docs/calculation-rules.md` 和当前任务计划为准。
