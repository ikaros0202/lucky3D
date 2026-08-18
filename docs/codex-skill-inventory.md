# Lucky3D Codex Skill 可用性基线

> 核对日期：2026-08-12
> 口径：`%USERPROFILE%\.codex\skills` 下包含 `SKILL.md`、且不是 `.system` 的直接子目录
> 状态：当前本机个人直装 Skill 快照；安装、删除或修改后必须重新核对

## 调用规则

1. 只有同时满足“列在本基线中”和“当前 Codex 会话实际暴露”两个条件，才可调用 Skill。
2. `.system`、插件缓存、临时插件仓库、恢复备份和历史计划中的名字不计入个人直装基线。
3. 历史文档中的 `REQUIRED SUB-SKILL` 不能覆盖当前可用性；对应 Skill 不在本基线时，按 `AGENTS.md`、当前任务文档和普通工具流程执行。
4. 不因文档提到某个 Skill 就自动安装、恢复或调用。安装或恢复必须由用户另行明确要求。
5. 只选择与当前任务直接相关的最小集合；用户明确点名的触发规则优先。

## 当前 15 个个人直装 Skill

### Android 与工程开发

1. `claude-android-ninja`
2. `compose-skill`（仅在用户明确点名时使用）
3. `incremental-implementation`
4. `source-driven-development`
5. `documentation-and-adrs`

### 代码质量、测试与交付

6. `code-review-and-quality`
7. `test-scenarios`
8. `design-qa-checklist`
9. `shipping-and-launch`

### UI/UX 与可视化

10. `data-visualization`
11. `oiloil-ui-ux-guide`
12. `ui-ux-pro-max`
13. `wireframe-spec`

### 报告与专用资产

14. `experiment-report`
15. `hatch-pet`

## Lucky3D 常用子集

| 任务 | 可用 Skill | 约束 |
|---|---|---|
| Android/Compose 实现 | `claude-android-ninja` | 只用于已批准实现；平台限定 Android 手机 |
| Compose 架构复核 | `compose-skill` | 必须由用户明确点名 |
| 多文件改动 | `incremental-implementation` | 分片修改、逐片验证，不顺带扩展 |
| 官方文档依据 | `source-driven-development` | 新依赖、新 Android API、行为敏感实现时使用 |
| 设计访谈或完整 UI 评审 | `oiloil-ui-ux-guide` | 按 `design`、`guide`、`review` 意图选择模式 |
| 页面信息架构和线框 | `wireframe-spec` | 先确定层级、位置、交互和状态 |
| 视觉 token 与 Compose 样式 | `ui-ux-pro-max` | 限定 Android 手机与 `jetpack-compose`，不照搬 Web 建议 |
| 图表和统计可视化 | `data-visualization` | 使用真实福彩 3D 数据，不能只靠颜色编码 |
| UI 完成验收 | `design-qa-checklist` | 必须基于当前实现截图和目标模拟器 |
| 测试场景 | `test-scenarios` | 写清前置条件、步骤和预期结果 |
| 合并前代码评审 | `code-review-and-quality` | 构建通过不等于功能正确 |

## 不属于当前个人基线的历史名称

以下名称曾出现在旧调研或历史实施计划中，但不在 2026-08-12 的个人直装清单里，不能作为当前必需 Skill 调用：

- `superpowers:subagent-driven-development`
- `superpowers:executing-plans`
- `edge-to-edge`
- `navigation-3`
- `testing-setup`
- `figma-generate-design`
- `figma-create-design-system-rules`
- `figma-implement-design`
- `imagegen`（系统能力不等于本项目个人直装 Skill 基线）

这些名称可作为历史来源或能力描述保留，但不能出现在当前执行链的强制步骤中。
