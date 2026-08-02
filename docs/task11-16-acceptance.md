# Lucky3D Tasks 11—16 验收记录

> 日期：2026-07-28<br>
> 结论：Tasks 11—16 与 Checkpoint D 通过<br>
> 设备范围：仅 Android 模拟器，不代表真机验收
>
> 本文是 V1 历史验收快照。Task 13 所述旧走势图交互已由 V1.1 Task 24 和
> `docs/prototypes/lucky3d-trend-approved-interactive.html` 取代，不作为当前 UI 验收口径。

## 1. 功能切片

| 任务 | 已交付 | 主要证据 |
| --- | --- | --- |
| Task 11 | 首页、走势、选号、方案、彩报五入口；Material 3 深浅色；通用加载、空、失败、离线、修正、成功状态 | `AppNavigationTest`、`MainTabTest`、深浅色与大字体截图 |
| Task 12 | 本地最新开奖、同步状态、手动刷新、历史 10/30/50/100/自定义、期号/日期/年份查询、属性详情 | `HomeViewModelTest`、`HomeScreenTest`、`FeatureQueriesTest`、无网首启截图 |
| Task 13 | 百十个位走势图、10/30/50/100/自定义窗口、滚动缩放、落点详情、遗漏/次数/冷热统计 | `TrendViewModelTest`、真实 30 期截图、图表 semantics |
| Task 14 | 单选/组选3/组选6、V1 条件编辑/停用/删除/撤销、冲突解释、胆拖、候选、注数/金额、保存入口 | `PickViewModelTest`、`PickScreenTest`、`NumberPoolTest`、条件与 1000 候选实机操作 |
| Task 15 | 方案/模板保存、重开、复制、备注、逐期回测、复盘与修正重算 | `SchemeViewModelTest`、`SchemePersistenceTest`、Room 2→3 迁移测试、保存重启截图 |
| Task 16 | 三类独立应用内提醒、DataStore 设置、去重、遗漏阈值、异常闭环 | `InAppReminderCoordinatorTest`、`UserPreferencesDataStoreTest`、错误/空状态 UI 测试 |

## 2. Checkpoint D 主流程

自动化与模拟器联合覆盖：

1. 首页从 Room 预置库显示 `2026198 / 685`。
2. 进入走势，30 期图表显示 90 个带位置语义的落点。
3. 进入选号，默认单选为 1000 注；添加胆码后显示 271 注并保留 `007` 等前导零。
4. 保存模板和方案；强制停止并重新启动后仍可读取。
5. 固定模板全量回测显示 3304 个有效期、覆盖/样本不足、平均注数与累计金额；没有未来概率或收益承诺。
6. Repository fixture 覆盖新增开奖、复盘和官方修正重算；真实当前期 `2026199` 尚未开奖，因此设备截图显示“等待开奖”。
7. 彩报页仅显示安装包静态占位图、来源与边界说明，无刷新、购买、分享、收藏或荐号入口。

设备测试：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

- API 26、320dp 窄屏：所有启用的设备测试通过，1 个显式联网冒烟测试按参数设计跳过。
- API 37、411dp：完整设备测试通过；另完成保存、重启、回测和五入口人工流程。
- `ManualSyncDeviceSmokeTest` 是 `liveSync=true` 才执行的可选在线测试；本轮 Release 前台启动在 API 37 显示官方同步成功，核心同步契约继续由 Task 10 测试覆盖。

## 3. 设计 QA

按 `design-spec.md` 和 Android 化后的 `design-qa-checklist` 检查：

- 视觉：页面使用统一 Material 3 语义色、字体、间距、圆角和边框；未引入赌场式霓虹、金币或庆祝视觉。
- 布局：验证 320dp、411dp、1.3× 系统字体；主要操作可见，长列表与详情可滚动。
- 内容：使用 3334 期真实基线；首页最近 5 期 UI 与 `data/fc3d-seed.json` 比对为 5/5。
- 交互：五页签切换和选号玩法状态保留测试通过；设置二级页返回首页、根页面返回桌面。
- 无障碍：五入口和图标按钮有内容描述；图表暴露“当前30期走势图，显示百位、十位、个位，共90个落点”；落点使用圆/方/菱形和文字，不只依赖颜色。
- 状态：加载、空、错误、无网、重试、成功、修正均有实现或测试注入；无网错误不遮挡本地开奖。

关键截图位于 `docs/evidence/task17/`：

- `home-offline-narrow-api26.png`
- `home-dark-large-font-api37.png`
- `trend-api37.png`
- `pick-filtered-api37.png`
- `scheme-after-restart-api37.png`
- `backtest-optimized-api37.png`
- `caibao-dark-large-font-api37.png`

## 4. 限制

- 本轮是 API 26/API 37 模拟器验收，没有物理设备结论。
- 已检查 TalkBack/无障碍语义树，未完成真实用户使用 TalkBack 的朗读体验研究。
- 复盘的当前期开奖设备流程要等待 `2026199` 官方开奖；新增/修正复盘行为已由隔离数据库 fixture 验证。
