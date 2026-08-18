# Lucky3D UI 设计 Skill 当前使用规范

> 初始调研日期：2026-07-28
> 当前核对日期：2026-08-12
> 状态：已按本机个人直装 Skill 清单重整；旧候选安装建议不再构成执行要求

## 1. 当前事实来源

完整的 15 项个人直装 Skill 及可用性规则见 [`codex-skill-inventory.md`](codex-skill-inventory.md)。

本文件只规定 Lucky3D 的 UI/UX 任务如何从当前已安装 Skill 中选择最小集合。历史调研曾提到的 Google Android、Figma、ImageGen 和 Superpowers 名称，不在当前个人直装基线中，不能作为必需步骤调用。

## 2. 当前可用的 UI/Android Skill

### `oiloil-ui-ux-guide`

- 用于建立或调整项目级设计方向、完整 UI 评审和页面族规则。
- 现有页面评审使用 `review`；新设计系统使用 `design`；单一 surface 规则使用 `guide`。
- 必须先读现有代码、`docs/design-spec.md` 和当前截图，不能凭空提问或用旧截图替代现状。

### `wireframe-spec`

- 用于新页面、重要弹窗、信息架构重排和复杂筛选区。
- 输出内容优先级、组件位置、交互注释及加载、空、正常、错误状态。
- 在线框阶段不决定颜色、阴影和装饰。

### `ui-ux-pro-max`

- 用于颜色、字体、间距、圆角、图标、动效和 Compose 组件样式。
- 查询时平台固定为 Android 手机，技术栈固定为 `jetpack-compose`。
- Web、CSS、GSAP、ARIA 和浏览器断点建议必须转换为 Material 3、Compose semantics、系统栏和 Android 窗口规则。

### `data-visualization`

- 走势图、遗漏、冷热、次数、分布、排行和回测等图表任务必须使用。
- 使用真实 `data/fc3d-seed.json` 数据检查密度、标签、缩放、滚动和极端值。
- 颜色之外必须有数字、标签、形状或线型编码。

### `design-qa-checklist`

- UI 已实现、需要对比确认稿或准备宣布完成时必须使用。
- 必须检查当前实现截图、目标模拟器、真实内容、深浅色、触控反馈和 `48dp` 目标。
- 无障碍模式和系统字体缩放仍按项目现有范围约束，不由 Skill 自行扩大。

### `claude-android-ninja`

- 用于已批准的 Kotlin、Jetpack Compose、Material 3、Navigation 3、Room、Hilt 和 Android 测试实现。
- 负责原生落地，不替代设计访谈、线框确认和产品决策。

### `compose-skill`

- 仅当用户明确说“使用 compose-skill”时调用。
- 用于 Compose 架构、状态、组件边界、动画和性能复核。

### 配套工程 Skill

- `incremental-implementation`：UI 改动跨多个文件时分片实施和验证。
- `source-driven-development`：新增 Android API、依赖或行为敏感实现时，以官方资料为依据。
- `test-scenarios`：把已确认交互写成可执行验收场景。
- `code-review-and-quality`：合并前检查正确性、边界和回归风险。

## 3. 选择规则

### 新页面或重大改版

```text
oiloil-ui-ux-guide
  → wireframe-spec
  → ui-ux-pro-max（需要视觉 token 或组件样式时）
  → data-visualization（含图表时）
  → claude-android-ninja（进入已批准实现时）
  → design-qa-checklist
```

### 小范围视觉或交互调整

```text
ui-ux-pro-max
  → claude-android-ninja（进入已批准实现时）
  → design-qa-checklist
```

### 现有界面完整评审

```text
oiloil-ui-ux-guide（review）
  → data-visualization（含图表时）
  → design-qa-checklist（需要完成态验收时）
```

不得因为 Skill 已安装就同时调用全部；纯数据、领域逻辑、数据库和网络任务不触发 UI 设计 Skill。

## 4. Lucky3D 固定转换规则

- 产品气质是可信、克制的专业数据工具，不采用赌场霓虹、金币、中奖庆祝或诱导性表达。
- 首页、走势、选号、方案、彩报共享同一套字体、间距、圆角、图标和状态语义；已确认的页面差异化视觉方向继续以 `docs/design-spec.md` 为准。
- 先解决主任务、信息层级和模式切换，再决定颜色和装饰。
- 正常状态优先在一个手机屏幕内呈现核心内容；新增信息必须同步说明替换、折叠或下沉哪些低优先级内容。
- 页面必须定义加载、空、内容、错误、无网和重试；保存或同步还要定义成功状态。
- 真实截图和真实数据优先于历史概念图、占位数据和构建状态。

## 5. 已退役的历史调用规则

旧版调研中的以下内容已退役：

- 要求安装或调用 Google `edge-to-edge`、`navigation-3`、`testing-setup`；
- 把 `imagegen` 写入 Lucky3D 的固定设计链；
- 把 Figma curated Skill 写成后续必装步骤；
- 用 Superpowers 子技能作为历史实施计划的强制执行器。

对应能力现在由当前已安装 Skill、Android 官方资料、项目代码与现有测试共同承担。除非用户以后明确要求重新安装，否则不得根据历史记录恢复这些 Skill。

## 6. 维护要求

- 本机安装、删除或修改 Skill 后，先更新 `docs/codex-skill-inventory.md`，再同步本文件和 `AGENTS.md`。
- 任何执行计划如果点名未安装 Skill，必须改为普通流程说明或当前可用 Skill，不能保留 `REQUIRED` 误导。
- Skill 只参与生成、实现或审查过程；长期事实仍记录在 PRD、设计规格、计算规则、数据设计和任务文档中。
