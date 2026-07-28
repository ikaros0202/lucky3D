# Lucky3D UI设计技能调研

> 日期：2026-07-28
> 状态：候选筛选完成并已安装
> 目标：为福彩3D安卓应用建立“设计发现—视觉规范—数据图表—原生实现—设计验收”的技能链

## 1. 搜索范围

本次搜索覆盖：

1. 当前Codex已安装技能。
2. OpenAI官方curated技能目录。
3. Google官方[Android Skills](https://github.com/android/skills)。
4. GitHub上活跃的UI/UX、Android、设计系统和设计评审技能仓库。
5. 聚合目录只用于发现候选，最终判断以候选的GitHub源仓库、`SKILL.md`、许可证和脚本内容为准。

筛选维度：

- 是否适合Android原生而非只适合网页。
- 是否能产出可执行的设计规范，而不是只给审美形容词。
- 是否覆盖走势图等高密度数据界面。
- 是否包含状态设计、无障碍和真实设备验收。
- 是否与现有技能重复。
- 来源可信度、许可证、维护状态和脚本风险。

## 2. 当前已经拥有的技能

### 2.1 `claude-android-ninja`

状态：已安装。

适合：

- Material 3主题、间距token和动态颜色。
- Compose组件、状态提升和性能。
- Android无障碍、TalkBack、触控目标。
- Navigation 3、Room、前台同步和Compose UI测试。

判断：这是原生实现阶段的主技能，但它不是完整的视觉设计咨询流程。应保留，负责把确认后的设计正确实现成Android界面。

### 2.2 `compose-skill`

状态：已安装，但只有用户明确说“使用compose-skill”时才会启用。

适合：

- Compose架构、状态管理、组件边界、动画、性能和可访问性。
- 对现有Compose代码做结构化评审。

判断：与`claude-android-ninja`存在部分重叠。可以作为实现复核技能，不需要再安装同类Compose社区技能。

### 2.3 `imagegen`

状态：系统已提供。

适合：

- 生成不同视觉方向的静态首页、走势、选号、方案和彩报页概念稿。
- 在确定色彩、层级和组件风格时做快速视觉比较。

判断：适合作为设计探索工具，但图片不能直接作为组件尺寸和交互规范。

## 3. 推荐安装的核心技能

### 3.1 `oiloil-ui-ux-guide`

- 来源：[oil-oil/ui-ux-guide](https://github.com/oil-oil/ui-ux-guide)
- 许可证：Apache-2.0
- 安装路径：`skills/oiloil-ui-ux-guide`
- 仓库状态：未归档，近期仍有维护。
- 可执行脚本：未发现。

能力：

- 先检查项目所处阶段，再通过逐项设计决策建立风格。
- 覆盖`design`、`guide`、`review`三种模式。
- 强制定义加载、空、错误、成功和权限不足等状态。
- 最终产出项目自己的`design-spec.md`，并要求用真实业务页面而非通用模板确认方向。

对Lucky3D的价值：

- 适合在写Compose代码之前完成UI需求访谈。
- 能约束首页、走势、选号、方案、彩报五个页面形成同一视觉语言。
- 能把“专业数据工具”与“博彩网站式视觉”区分开。

注意：

- 技能示例偏Web项目，但核心设计流程和设计规范与平台无关。
- 业务化预览需要明确改为Android手机尺寸和Material 3组件语义。

结论：**推荐作为UI设计流程的主控技能。**

### 3.2 `ui-ux-pro-max`

- 来源：[nextlevelbuilder/ui-ux-pro-max-skill](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill)
- 许可证：MIT
- 安装路径：`.claude/skills/ui-ux-pro-max`
- 仓库状态：活跃，有版本发布和安全说明。

能力：

- 包含样式、配色、字体、图表、动效、产品类型和UX规则数据库。
- 包含专门的`jetpack-compose.csv`，覆盖Compose状态、Material 3、布局、性能和无障碍建议。
- 可生成并持久化设计系统主规范和页面级覆盖规则。

脚本快速检查：

- Python脚本主要读取技能内CSV。
- `--persist`模式会把设计系统写入当前项目。
- 未发现网络请求、子进程执行、`eval`或`exec`。
- 安装前仍需按实际下载版本复查。

对Lucky3D的价值：

- 为颜色、字体、间距、圆角、动效和图表样式提供大量候选。
- 能给高密度数据应用生成比通用Material默认主题更明确的设计方向。
- 可以用Jetpack Compose规则检查最终规范是否能落地。

注意：

- 数据库很大，不能让它替代产品判断。
- 部分建议面向Web；使用时必须限定技术栈为Jetpack Compose、平台为Android手机。
- 不应直接采用“霓虹、赌场、炫彩中奖”等与产品定位冲突的风格。

结论：**推荐作为视觉系统和Compose设计知识库。**

### 3.3 `data-visualization`

- 来源：[Owl-Listener/designer-skills](https://github.com/Owl-Listener/designer-skills)
- 具体路径：`ui-design/skills/data-visualization`
- 许可证：MIT
- 形式：纯Markdown技能。

能力：

- 图表类型选择、数据墨水比、标签和图例。
- 连续、分类、发散色彩规则。
- 色觉无障碍，禁止只依赖红绿或单一颜色编码。
- 移动端简化、触摸提示和图表的文本替代。

对Lucky3D的价值：

- 走势是应用核心，不能只用通用UI技能处理。
- 可指导百十个位走势图、遗漏排行、冷热分布和窗口切换。
- 强调真实数据验证，适合用3334期数据测试密度和极端情况。

结论：**推荐单独安装，不安装整个designer-skills大包。**

### 3.4 `wireframe-spec`

- 来源：[Owl-Listener/designer-skills](https://github.com/Owl-Listener/designer-skills)
- 具体路径：`prototyping-testing/skills/wireframe-spec`
- 许可证：MIT
- 形式：纯Markdown技能。

能力：

- 输出带优先级、组件位置、交互和状态注释的中低保真线框。
- 要求同时考虑加载、空、内容、错误等状态。
- 先解决信息层级，再讨论颜色。

对Lucky3D的价值：

- 可把当前文字线框升级成可验收的页面结构。
- 尤其适合确定走势图筛选区、选号条件区和候选列表的空间关系。

结论：**推荐安装。**

### 3.5 `design-qa-checklist`

- 来源：[Owl-Listener/designer-skills](https://github.com/Owl-Listener/designer-skills)
- 具体路径：`design-ops/skills/design-qa-checklist`
- 许可证：MIT
- 形式：纯Markdown技能。

能力：

- 从颜色、字体、间距、布局、交互状态、真实内容、无障碍和设备适配进行设计验收。
- 要求用截图对比设计和实现。

对Lucky3D的价值：

- 可以补充开发计划中Task 11—16的UI验收门槛。
- 可防止“功能完成但页面拥挤、状态缺失、字号放大后截断”。

注意：原文含少量Web术语，Android项目需把键盘、ARIA等检查替换为TalkBack、Compose semantics和系统字体缩放。

结论：**推荐安装并做轻量Android适配。**

## 4. 建议在编码阶段安装的Google官方技能

Google在[Android Skills官方说明](https://developer.android.com/tools/agents/android-skills)中说明，这些技能遵循开放Agent Skills标准，可用于Codex等第三方Agent。

### 4.1 `edge-to-edge`

- 来源：[android/skills/system/edge-to-edge](https://github.com/android/skills/tree/main/system/edge-to-edge)
- 价值：处理状态栏、导航栏、IME遮挡和系统栏可读性。
- 使用时机：Android工程建立、页面开始在模拟器运行后。

### 4.2 `navigation-3`

- 来源：[android/skills/navigation/navigation-3](https://github.com/android/skills/tree/main/navigation/navigation-3)
- 价值：五个一级入口、多返回栈、状态保存和类型安全导航。
- 使用时机：实现应用外壳与五入口导航时。

### 4.3 `testing-setup`

- 来源：[android/skills/testing/testing-setup](https://github.com/android/skills/tree/main/testing/testing-setup)
- 价值：Compose UI测试、截图测试和端到端测试基础设施。
- 使用时机：工程初始化时即加入，避免UI完成后才补测试。

判断：这三个技能权威性最高，但解决的是Android正确落地和验证，不负责决定视觉风格。

## 5. 可选的Figma工作流

OpenAI官方curated目录中存在：

- [figma-generate-design](https://github.com/openai/skills/tree/main/skills/.curated/figma-generate-design)
- [figma-create-design-system-rules](https://github.com/openai/skills/tree/main/skills/.curated/figma-create-design-system-rules)
- [figma-implement-design](https://github.com/openai/skills/tree/main/skills/.curated/figma-implement-design)

优点：

- 可以把页面设计、组件变量和实现交接集中在Figma。
- `figma-implement-design`要求截图和结构化设计上下文同时存在后再写代码。

限制：

- 需要连接Figma MCP。
- 需要Figma文件或团队设计系统。
- Android输出仍需转换成Compose和本项目组件约定。

判断：如果用户希望亲自在Figma里审图、调整布局，值得安装；如果只想在Codex里看概念稿并直接开发，当前阶段不是必需。

## 6. 搜索后不建议作为首选的候选

### `mobile-android-design`

来源：[wshobson/agents](https://github.com/wshobson/agents)

优点：明确覆盖Material 3、Compose、48dp触控目标和Android导航。

不首选原因：

- 与已安装的`claude-android-ninja`高度重叠。
- 部分建议仍以Navigation Compose和`WindowSizeClass`为主，需要对照最新官方文档修正。

### `impeccable`

来源：[pbakaus/impeccable](https://github.com/pbakaus/impeccable)

优点：视觉审查、信息层级、动效、文案和截图迭代流程很强。

不首选原因：

- 核心工作流更偏浏览器前端和`npx`工具链。
- 对原生Android和走势图领域的直接指导弱于推荐组合。

可在应用已经能运行后，作为额外视觉评审参考，而不是主设计技能。

### Google `styles`

来源：[android/skills/jetpack-compose/theming/styles](https://github.com/android/skills/tree/main/jetpack-compose/theming/styles)

不首选原因：

- 技能自身标明为实验性。
- 要求Compose alpha版本和实验API。
- 明确不支持Material Design组件Styles。

首版应使用稳定Material 3主题和项目token，不为该技能引入alpha依赖。

### Google `adaptive`

来源：[android/skills/jetpack-compose/adaptive](https://github.com/android/skills/tree/main/jetpack-compose/adaptive)

不立即使用原因：

- Lucky3D V1当前限定手机竖屏。
- 该技能的平板、折叠屏、多栏布局价值应放到V1主流程稳定后评估。

## 7. 最适合Lucky3D的技能组合

### 现在安装

1. `oiloil-ui-ux-guide`：负责设计访谈、方向确认和`design-spec.md`。
2. `ui-ux-pro-max`：负责视觉候选、设计token、图表和Jetpack Compose知识检索。
3. `wireframe-spec`：负责四个页面及关键状态的线框。
4. `data-visualization`：负责走势图和统计图表。
5. `design-qa-checklist`：负责设计实现验收。

### 直接使用现有技能

1. `claude-android-ninja`：负责Material 3和Compose实现。
2. `compose-skill`：需要用户明确点名时，用于Compose结构复核。
3. `imagegen`：生成2—3套业务页面视觉方向图。

### Android工程建立后安装

1. Google `testing-setup`
2. Google `navigation-3`
3. Google `edge-to-edge`

### 用户选择Figma时再安装

1. OpenAI `figma-generate-design`
2. OpenAI `figma-create-design-system-rules`
3. OpenAI `figma-implement-design`

## 8. 建议的UI设计流程

```text
确认安装核心技能
  → oiloil-ui-ux-guide检查项目并开展设计访谈
  → wireframe-spec完善四页面和全部状态
  → ui-ux-pro-max给出2—3套视觉系统候选
  → data-visualization单独设计走势与统计规则
  → imagegen生成真实Lucky3D业务页面概念稿
  → 用户选择并修改
  → 生成docs/design-spec.md
  → 批准设计规范
  → claude-android-ninja实现Compose界面
  → design-qa-checklist + Google testing-setup截图验收
```

设计规范至少应明确：

- 产品气质：数据工具而非博彩宣传页。
- 首页、走势、选号、方案、彩报五个页面的内容优先级。
- 色彩、字体、间距、圆角、图标和动效token。
- 数字、开奖号、冷热、命中、错误、待更新等语义颜色。
- 图表在颜色之外使用数字、形状、线型或标签编码。
- 加载、空、无网、同步失败、数据修正、无候选和样本不足状态。
- 系统字体放大、TalkBack、48dp触控目标和窄屏规则。
- 真实3334期数据下的走势密度和性能要求。

## 9. 安装安全原则

- 不一次安装整个大型技能合集，只安装明确需要的子目录。
- 安装前重新检查当前提交的`SKILL.md`、引用文件和脚本。
- 第三方技能不得覆盖项目需求、Android官方规范或用户已经确认的设计决策。
- 含脚本的技能只允许在明确任务中运行；不自动执行全局安装、网络发布或破坏性命令。
- 将最终设计规范保存在项目中，技能只是生成和审查过程，不作为长期唯一事实来源。
