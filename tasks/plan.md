# Lucky3D V1 实施计划

> 状态：V1—V1.3 已完成本地阶段交付；V1.3 未上传 GitHub，正式发行门禁仍未关闭
> 日期：2026-08-03
> 输入：`PRD-lucky3D.md` V1.3、开发前技术设计、实时内容设计、数据同步设计、计算规则V1

## 1. 概述

构建一个不依赖自建后端的原生Android应用。安装包内置2017001期起的福彩3D开奖，应用进入前台或手动刷新时直连福利彩票官方数据源增量更新。用户可以查看开奖和走势、组合条件筛选号码、保存方案与模板，并对历史和新开奖进行回测与复盘。

V1 已完成静态只读彩报占位页；V1.2 受控接入 55125.cn 福彩 3D 试机号和牛彩网 A11 彩报。仍不包含其他真实彩报聚合、云服务、账号系统、后台准点推送、在线购彩、支付、社区和AI荐号。

V1.3 将和值改为 `0—27` 完整走势组，实际和值以圆点连线呈现，其余和值格展示遗漏；和值与既有列共同进入统一 viewport。四行统计覆盖每个和值，出现次数使用可见窗口，其余统计使用完整历史，平均/最大遗漏只计算已结束遗漏段。

## 2. 当前已经完成

- [x] 中文社区需求调研和V1范围收敛。
- [x] PRD V0.3。
- [x] 数据源、启动同步与错误降级设计。
- [x] 计算公式和玩法口径V1。
- [x] 2017001—2026198共3334期内置数据。
- [x] 全量结构校验和计算黄金样例。
- [x] 跨年份独立抽样20期，日期和号码20/20一致。
- [x] 技术架构、数据模型和四个主页面线框。

## 3. 当前交付状态

V1 实现、自动化验证、双模拟器验收、数据复核和本地签名 Release 构建均已完成。当前仅剩产品负责人决定进入内测还是继续修改；正式分发前需改用长期保管的生产发布密钥重新签名。

## 4. 架构决策

- Kotlin、Jetpack Compose Material 3、单`app`模块。
- Navigation 3、Room、OkHttp、kotlinx.serialization、Hilt、DataStore。
- JDK 17、`minSdk 26`、`compileSdk 37`、`targetSdk 37`。
- Room是运行时唯一事实来源；网络响应通过校验并写入Room后才更新界面。
- 计算核心是无Android依赖的纯Kotlin代码，先写测试再实现。
- 首版不使用WorkManager，不在应用关闭期间联网。
- 依赖使用创建工程时官方文档列出的稳定版本，并在版本目录中锁定；禁止动态版本。

## 5. 依赖关系

```text
文档批准
  → 工程骨架与质量门禁
      → 计算核心
          ├─ 开奖属性与窗口统计
          ├─ 筛选与玩法
          └─ 模板、回测与复盘
      → Room与内置数据库
          → 官方网络适配器
              → 前台同步Repository
      → 导航与应用外壳
          → 首页/历史
          → 走势
          → 选号
          → 方案/回测/复盘
              → 全流程验收与发布构建
```

## 6. 任务清单

### 阶段A：准入和工程基础

## Task 0：确认产品与技术基线

**Description：**产品负责人评审并确认V1范围、无云架构、2017年数据起点、计算规则、默认包名和实施顺序。

**Acceptance criteria：**

- [x] `PRD-lucky3D.md`和技术设计被明确批准。
- [x] 包名`com.lucky3d.app`、`minSdk 26`和五个一级入口没有待定项。
- [x] 彩报联网更新及2017年以前数据保持在V1范围外；彩报静态占位页进入V1。

**Verification：**

- [x] `tasks/todo.md`中的“计划批准”被勾选。
- [x] 文档中不存在互相冲突的V1范围描述。

**Dependencies：**None

**Files likely touched：**

- `PRD-lucky3D.md`
- `docs/superpowers/specs/2026-07-28-lucky3d-predevelopment-design.md`
- `tasks/plan.md`

**Estimated scope：**S

## Task 1：创建Android工程和锁定工具链

**Description：**初始化Git仓库和单模块Compose工程，配置JDK 17、SDK 37、包名、版本目录、Hilt、Room、Navigation 3、OkHttp、序列化和DataStore。

**Acceptance criteria：**

- [x] Debug应用能安装并显示Lucky3D应用外壳。
- [x] 所有依赖使用已核验的官方稳定版本且没有`+`等动态版本。
- [x] 项目可使用仓库内Gradle Wrapper从干净环境构建。

**Verification：**

- [x] `.\gradlew.bat assembleDebug`
- [x] `.\gradlew.bat testDebugUnitTest`
- [x] `.\gradlew.bat lintDebug`

**Dependencies：**Task 0

**Files likely touched：**

- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `gradle/wrapper/*`

**Estimated scope：**M

## Task 2：建立持续质量门禁

**Description：**配置单元测试、Android测试、静态检查和本地统一验证入口，确保后续每个切片都能被重复验证。

**Acceptance criteria：**

- [x] 一条命令可以运行单元测试、lint和Debug构建。
- [x] 测试失败或lint错误会使验证命令非零退出。
- [x] 仓库忽略构建产物、本地SDK路径和签名材料。

**Verification：**

- [x] `.\gradlew.bat testDebugUnitTest lintDebug assembleDebug`
- [x] 人工确认`.gitignore`不忽略源代码和需求文档。

**Dependencies：**Task 1

**Files likely touched：**

- `.gitignore`
- `app/src/test/...`
- `app/src/androidTest/...`
- `README.md`

**Estimated scope：**S

### Checkpoint A：工程可持续构建

- [x] Debug构建、单元测试和lint全部通过。
- [x] 依赖版本与Android官方文档核对完成。
- [x] 人工评审后进入计算核心。

### 阶段B：纯计算核心

## Task 3：实现单期号码属性

**Description：**从黄金样例先建立失败测试，再实现和值、和尾、跨度、奇偶、大小、质合、012路、基本组选形态、连号和两码关系。

**Acceptance criteria：**

- [x] `000、007、112、123、685、999`全部黄金属性一致。
- [x] 组三、组六、豹子互斥并覆盖000—999。
- [x] 领域代码不依赖Android或数据库类。

**Verification：**

- [x] `.\gradlew.bat testDebugUnitTest --tests "*DrawAttributesTest"`
- [x] 枚举000—999验证形态覆盖和边界。

**Dependencies：**Task 2

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/domain/attributes/DrawNumber.kt`
- `app/src/main/java/com/lucky3d/app/domain/attributes/DrawAttributes.kt`
- `app/src/test/java/com/lucky3d/app/domain/attributes/DrawAttributesTest.kt`

**Estimated scope：**M

## Task 4：实现跨期属性、遗漏和冷热

**Description：**实现重号、邻号、当前/平均/最大遗漏、完成遗漏段和窗口冷热统计，严格执行全历史预热规则。

**Acceptance criteria：**

- [x] 黄金样例的重号、邻号和遗漏序列全部通过。
- [x] 30期与100期只改变窗口次数和冷热，不重置历史遗漏。
- [x] 无完成遗漏段时平均值和最大值返回可解释的空值。

**Verification：**

- [x] `.\gradlew.bat testDebugUnitTest --tests "*Omission*" --tests "*TrendStatistics*"`
- [x] 边界测试覆盖0/9邻号和样本不足。

**Dependencies：**Task 3

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/domain/omission/OmissionCalculator.kt`
- `app/src/main/java/com/lucky3d/app/domain/attributes/CrossDrawAttributes.kt`
- `app/src/test/java/com/lucky3d/app/domain/omission/OmissionCalculatorTest.kt`
- `app/src/test/java/com/lucky3d/app/domain/attributes/CrossDrawAttributesTest.kt`

**Estimated scope：**M

## Task 5：实现号码池、筛选和玩法转换

**Description：**实现000—999号码池、条件交集、冲突说明、胆码/杀码/定位/属性筛选、胆拖、组选规范化和单选排列。

**Acceptance criteria：**

- [x] 黄金样例中1000、729、271、54、28、10等精确数量全部通过。
- [x] 组选3生成3个单选排列，组选6生成6个，所有结果去重并保留前导0。
- [x] 冲突条件返回明确原因，不静默删除条件。

**Verification：**

- [x] `.\gradlew.bat testDebugUnitTest --tests "*Filter*" --tests "*PlayConversion*"`
- [x] 枚举测试覆盖全部1000个有序号码。

**Dependencies：**Task 3

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/domain/filter/FilterCondition.kt`
- `app/src/main/java/com/lucky3d/app/domain/filter/NumberPool.kt`
- `app/src/main/java/com/lucky3d/app/domain/filter/PlayConverter.kt`
- `app/src/test/java/com/lucky3d/app/domain/filter/NumberPoolTest.kt`

**Estimated scope：**M

## Task 6：实现方案、模板、回测和复盘逻辑

**Description：**建立纯领域模型和算法，确保回测只使用目标期之前的数据，并按规则版本生成可复现结果。

**Acceptance criteria：**

- [x] 样本不足期不进入覆盖率分母。
- [x] 相同模板、数据范围和规则版本得到相同结果。
- [x] 官方号码指纹变化会使对应复盘重新计算。

**Verification：**

- [x] `.\gradlew.bat testDebugUnitTest --tests "*Backtest*" --tests "*Replay*"`
- [x] 测试包含防止未来数据泄漏的反例。

**Dependencies：**Tasks 4、5

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/domain/backtest/BacktestEngine.kt`
- `app/src/main/java/com/lucky3d/app/domain/replay/ReplayEngine.kt`
- `app/src/test/java/com/lucky3d/app/domain/backtest/BacktestEngineTest.kt`
- `app/src/test/java/com/lucky3d/app/domain/replay/ReplayEngineTest.kt`

**Estimated scope：**M

### Checkpoint B：算法可信

- [x] 黄金测试和全部领域单元测试通过。
- [x] 任意相同输入可重复得到相同输出。
- [x] 人工核对至少3组筛选与回测样例。

### 阶段C：本地数据与联网同步

## Task 7：建立开奖Room数据库和预置资产

**Description：**定义开奖与同步元数据表、DAO和Repository，并把3334期JSON转换成可由Room `createFromAsset()`复制的预置数据库。

**Acceptance criteria：**

- [x] 无网首次启动可查询2017001—2026198共3334期。
- [x] 期号唯一、升序范围查询和最近N期查询正确。
- [x] 预置数据哈希和记录数在构建时可验证。

**Verification：**

- [x] Room DAO测试和预置数据库打开测试通过。
- [x] `tools/validate-predevelopment-data.ps1`通过。
- [x] 模拟器清数据后无网首启成功。

**Dependencies：**Task 2

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/data/local/DrawEntity.kt`
- `app/src/main/java/com/lucky3d/app/data/local/DrawDao.kt`
- `app/src/main/java/com/lucky3d/app/data/local/Lucky3dDatabase.kt`
- `app/src/main/assets/database/lucky3d.db`
- `app/src/androidTest/.../PrepackagedDatabaseTest.kt`

**Estimated scope：**M

## Task 8：持久化方案、模板和复盘

**Description：**增加方案、模板、复盘表与事务DAO，建立条件JSON版本化、外键和Room迁移测试。

**Acceptance criteria：**

- [x] 方案、模板和复盘可保存、读取、复制并保持关联。
- [x] 已开奖方案不能原地覆盖，编辑会创建副本。
- [x] 数据库迁移不会清除开奖或用户方案。

**Verification：**

- [x] DAO事务测试通过。
- [x] Room migration测试通过。
- [x] 进程重建后数据仍存在。

**Dependencies：**Tasks 6、7

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/data/local/SchemeEntity.kt`
- `app/src/main/java/com/lucky3d/app/data/local/SchemeDao.kt`
- `app/src/main/java/com/lucky3d/app/data/local/DatabaseMigrations.kt`
- `app/src/androidTest/.../SchemePersistenceTest.kt`

**Estimated scope：**M

## Task 9：实现官方数据源适配器

**Description：**按已验证契约实现官方JSON请求、DTO隔离、严格解析、域名限制和错误分类，不直接写数据库。

**Acceptance criteria：**

- [x] 正常、空响应、字段缺失、非法号码、重复期号和HTTP失败都有测试。
- [x] 任一记录非法时整批返回`InvalidPayload`。
- [x] 页面层无法引用网络DTO或接口URL。

**Verification：**

- [x] `.\gradlew.bat testDebugUnitTest --tests "*OfficialFc3dDataSource*"`
- [x] 使用保存的响应fixture完成离线契约测试。

**Dependencies：**Task 2

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/data/remote/OfficialFc3dDataSource.kt`
- `app/src/main/java/com/lucky3d/app/data/remote/OfficialDrawDto.kt`
- `app/src/main/java/com/lucky3d/app/data/mapper/DrawMapper.kt`
- `app/src/test/java/com/lucky3d/app/data/remote/OfficialFc3dDataSourceTest.kt`

**Estimated scope：**M

## Task 10：实现本地优先的前台同步

**Description：**协调Room与官方适配器，完成5分钟节流、手动刷新、最近30期修正检查、超过100期的分页补齐和复盘重算。

**Acceptance criteria：**

- [x] 本地数据先展示，联网结果仅在完整校验和事务提交后可见。
- [x] 新增、无变化、修正、长缺口和失败五类场景均保持数据库一致。
- [x] 同一时刻只运行一个同步，重复事件不重复提醒。

**Verification：**

- [x] Repository集成测试覆盖五类场景。
- [x] 断网和结构异常测试确认本地数据不变。
- [x] 模拟器完成前台自动同步和显式联网手动刷新冒烟测试。

**Dependencies：**Tasks 7、8、9

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/data/repository/DrawRepository.kt`
- `app/src/main/java/com/lucky3d/app/data/repository/SyncCoordinator.kt`
- `app/src/test/java/com/lucky3d/app/data/repository/SyncCoordinatorTest.kt`
- `app/src/main/java/com/lucky3d/app/app/LifecycleSyncObserver.kt`

**Estimated scope：**M

### Checkpoint C：数据链路可信

- [x] 无网首启、联网新增、官方修正和失败降级全部通过。
- [x] Room迁移与事务测试通过。
- [x] 应用数据库最近100期与官方数据逐条一致（2026-07-28，100/100）。

### 阶段D：用户功能切片

## Task 11：建立五入口导航和设计系统

**Description：**实现首页、走势、选号、方案、彩报五入口的导航外壳、Material 3主题、通用加载/错误组件和中文资源。彩报首版只展示内置静态占位图、来源和边界说明。

**Acceptance criteria：**

- [x] 五个入口可切换并保留各自主要页面状态。
- [x] 彩报页离线可见，且没有网络更新、翻页、收藏、分享或购买入口。
- [x] 深浅色模式和系统字体放大下无关键内容截断。
- [x] 同步错误组件允许重试且不遮挡已有数据。

**Verification：**

- [x] Compose导航测试通过。
- [x] Preview或截图覆盖深浅色和大字体。
- [x] 模拟器返回键行为正确。

**Dependencies：**Task 2

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/app/navigation/AppNavigation.kt`
- `app/src/main/java/com/lucky3d/app/core/ui/Lucky3dTheme.kt`
- `app/src/main/java/com/lucky3d/app/core/ui/ContentState.kt`
- `app/src/androidTest/.../AppNavigationTest.kt`

**Estimated scope：**M

## Task 12：首页与历史查询完整切片

**Description：**交付最新开奖、同步状态、手动刷新、历史范围/期号/日期查询和单期属性详情。

**Acceptance criteria：**

- [x] 无网时立即显示本地最新一期和上次更新时间。
- [x] 支持10/30/50/100、自定义、期号、日期和年份查询。
- [x] 点击任一期可查看与计算核心一致的完整属性。

**Verification：**

- [x] ViewModel与Compose UI测试通过。
- [x] 模拟器完成无网首启、联网刷新和历史详情流程。
- [x] 随机5期界面值与内置JSON一致。

**Dependencies：**Tasks 3、10、11

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/feature/home/HomeViewModel.kt`
- `app/src/main/java/com/lucky3d/app/feature/home/HomeScreen.kt`
- `app/src/main/java/com/lucky3d/app/feature/home/HistoryScreen.kt`
- `app/src/test/java/com/lucky3d/app/feature/home/HomeViewModelTest.kt`

**Estimated scope：**M

## Task 13：走势、遗漏和统计完整切片

> 本节记录 V1 初始交付范围。V1.1 的用户可见走势图由 Task 24 和已确认交互原型覆盖：
> 仅显示10/30期、固定期号/开奖号左栏、三位置30列共享横滑，不再暴露缩放、位置筛选或自定义期数。

**Description：**交付百十个位走势图、周期切换、缩放滚动、遗漏排行、次数和冷热统计；不在走势图下方增加当前点位详情摘要条。

**Acceptance criteria：**

- [x] 图表落点与对应期号位置号码一致。
- [x] 10/30/50/100及自定义周期驱动同一范围的统计。
- [x] 颜色不是唯一信息编码，缩放和横向滚动可用。

**Verification：**

- [x] 图表坐标与统计ViewModel测试通过。
- [x] 模拟器验证窄屏、100期、大字体和TalkBack标签。
- [x] 30期/100期统计与领域计算测试结果一致。

**Dependencies：**Tasks 4、7、11

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/feature/trend/TrendViewModel.kt`
- `app/src/main/java/com/lucky3d/app/feature/trend/TrendScreen.kt`
- `app/src/main/java/com/lucky3d/app/feature/trend/TrendChart.kt`
- `app/src/test/java/com/lucky3d/app/feature/trend/TrendViewModelTest.kt`

**Estimated scope：**M

## Task 14：选号与筛选完整切片

**Description：**交付玩法选择、条件添加/编辑/停用、冲突解释、候选列表、注数、倍数、金额和保存模板/方案入口。

**Acceptance criteria：**

- [x] 所有PRD V1筛选条件可组合且结果即时更新。
- [x] 每个条件显示排除数量，空结果显示冲突和撤销入口。
- [x] 前导0、组选排列、注数和金额与领域核心一致。

**Verification：**

- [x] ViewModel测试覆盖增删条件、冲突、倍数边界。
- [x] Compose UI测试完成典型“胆码+和值+跨度”流程。
- [x] 1000号码列表滚动无明显卡顿。

**Dependencies：**Tasks 5、8、11

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/feature/pick/PickViewModel.kt`
- `app/src/main/java/com/lucky3d/app/feature/pick/PickScreen.kt`
- `app/src/main/java/com/lucky3d/app/feature/pick/ConditionEditor.kt`
- `app/src/test/java/com/lucky3d/app/feature/pick/PickViewModelTest.kt`

**Estimated scope：**M

## Task 15：方案、模板、回测与复盘完整切片

**Description：**交付方案/模板列表、详情、复制、备注、历史回测、逐期结果和新开奖后的自动复盘。

**Acceptance criteria：**

- [x] 保存后重启应用可完整还原条件、候选、金额和备注。
- [x] 回测明确展示覆盖期数、样本不足、平均注数和累计金额，不显示未来概率。
- [x] 新开奖或官方修正会生成或更新对应复盘。

**Verification：**

- [x] ViewModel和数据库集成测试通过。
- [x] 模拟器完成“选号—保存—重启—同步—复盘”主流程。
- [x] 固定模板重复回测结果完全一致。

**Dependencies：**Tasks 6、8、10、11、14

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/feature/scheme/SchemeViewModel.kt`
- `app/src/main/java/com/lucky3d/app/feature/scheme/SchemeScreen.kt`
- `app/src/main/java/com/lucky3d/app/feature/scheme/BacktestScreen.kt`
- `app/src/test/java/com/lucky3d/app/feature/scheme/SchemeViewModelTest.kt`

**Estimated scope：**M

## Task 16：应用内提醒、设置和异常闭环

**Description：**实现开奖、复盘和遗漏应用内提醒开关，统一无网、同步失败、数据修正、空状态和诊断信息。

**Acceptance criteria：**

- [x] 用户可分别关闭三类提醒，关闭后其他类型不受影响。
- [x] 同一期同一事件只提醒一次。
- [x] 所有已定义错误都有可理解文案和可行下一步。

**Verification：**

- [x] DataStore与提醒去重测试通过。
- [x] UI测试覆盖无网、解析失败、数据修正和空方案。
- [x] 日志检查确认不包含用户备注或完整方案。

**Dependencies：**Tasks 10、11、15

**Files likely touched：**

- `app/src/main/java/com/lucky3d/app/feature/settings/UserPreferences.kt`
- `app/src/main/java/com/lucky3d/app/core/ui/SyncStatusUi.kt`
- `app/src/main/java/com/lucky3d/app/domain/replay/InAppReminderCoordinator.kt`
- `app/src/test/java/com/lucky3d/app/domain/replay/InAppReminderCoordinatorTest.kt`

**Estimated scope：**M

### Checkpoint D：V1主流程完整

- [x] 五个入口均可完成真实数据流程。
- [x] “查看—分析—筛选—保存—同步—复盘”端到端通过。
- [x] 无网、失败和数据修正不会破坏本地数据。

### 阶段E：发布准备

## Task 17：全量验收、性能、安全和Release构建

**Description：**完成PRD验收、数据复核、性能和无障碍检查、权限与网络安全审计、签名配置说明和可安装Release APK。

**Acceptance criteria：**

- [x] PRD主要验收场景全部通过并留存证据。
- [x] 应用只申请必要网络权限，Release无调试开关和签名材料泄露。
- [x] 生成可安装APK，记录包名、版本、签名证书摘要和SHA-256。

**Verification：**

- [x] `.\gradlew.bat clean test lint connectedDebugAndroidTest assembleRelease`
- [x] 重跑数据全量校验和独立20期抽样。
- [x] 模拟器完成安装、升级、无网首启和完整主流程。

**Dependencies：**Tasks 12—16

**Files likely touched：**

- `app/src/main/AndroidManifest.xml`
- `app/proguard-rules.pro`
- `docs/release-checklist.md`
- `docs/acceptance-report.md`

**Estimated scope：**M

### Checkpoint E：可交付

- [x] 所有自动化测试、lint和Release构建通过。
- [x] 需求、实现和验收证据一致。
- [x] APK可安装，数据、方案和主流程真实可用。
- [x] 已向产品负责人交付“进入内测或继续修改”的决策点。

## 7. 项目级完成定义

任何任务只有同时满足以下条件才可标记完成：

- 需求对应的测试先建立并通过。
- 相关代码可构建、lint无新增错误。
- 数据结构变化有迁移测试，不使用破坏性迁移。
- UI变化在模拟器上实际操作并检查错误/空/加载状态。
- 新依赖和Android API有官方文档依据。
- 没有把彩报联网更新、云服务或其他V1外功能带入实现。
- 文档与实际行为同步更新。

## 8. 主要风险

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 官方接口无公开SLA或结构变化 | 高 | 独立适配器、严格整批校验、预置本地数据、失败不污染Room |
| 走势图在手机小屏拥挤 | 高 | 先做100期交互原型，滚动/缩放/落点可用后再扩充统计 |
| 筛选条件组合复杂 | 高 | 纯函数和1000号码穷举测试先于UI |
| 回测误用未来数据 | 高 | 专门的反例测试，目标期只读此前数据 |
| 数据库升级损坏用户方案 | 高 | 每个版本迁移测试，禁止无条件破坏性迁移 |
| 应用关闭时无法及时更新 | 中 | 明确只保证前台/手动同步，显示最后同步时间 |
| 第三方抽样站数据也可能有错 | 中 | 第三方只用于发现异常，冲突时人工对照官方公告 |

## 9. 并行与顺序

首轮建议按顺序完成Tasks 0—2。之后：

- Tasks 3与7可在契约固定后并行，但首次开发建议先完成Task 3以建立领域模型。
- Tasks 4与5可以并行；Task 6等待二者完成。
- Task 9可与Tasks 7、8并行；Task 10等待三者完成。
- Tasks 12—14共享导航和通用模型，宜按首页、走势、选号顺序实现，减少反复改公共组件。
- Task 15依赖最多，不能提前实现。

## 10. 当前需要的决定

V1.1 UI 与实时内容范围已经批准；当前没有额外待决策项，按 Tasks 18—27 推进。

## 11. V1.1 任务清单

V1 完成记录保留。V1.1 依次实施以下任务：

1. **Task 18：文档与风险门禁** — 对齐产品、数据、设计和第三方授权边界，建立冲突扫描与发布门禁。
2. **Task 19：Room v4** — 增加实时内容完整存储契约：试机号来源、期号、三位号码、来源页与北京时间；彩报 A11 版次、标题、来源页、图片地址、本地文件名、SHA-256、MIME、尺寸、北京时间缓存日期与抓取时间；按内容类型保存尝试、成功、下次自动尝试与失败元数据，并提供 v3→v4 非破坏性迁移测试。
3. **Task 20：公开页面适配器** — 实现彩经网模拟试机号与牛彩网 A11 的公开页面解析、校验和失败模型。
4. **Task 21：刷新、缓存和清理** — 实现北京时间资格检查、冷却、最大尝试次数、私有文件缓存和三天清理（V1.1 历史范围；V1.2 已替换为 30 天）。
5. **Task 22：设计系统与导航** — 落实已确认设计 token、五入口导航及通用状态。
6. **Task 23：首页** — 实现柔光晶体首页、真实本地开奖信息和模拟试机号状态。
7. **Task 24：走势** — 严格按 `docs/prototypes/lucky3d-trend-approved-interactive.html` 实现流动朱砂纵向长表、固定左栏、10/30期和三位置共享横滑。
8. **Task 25：选号** — 实现流动朱砂选号页面与真实筛选、金额和保存闭环。
9. **Task 26：方案** — 实现方案、模板、回测和复盘的已确认页面流。
10. **Task 27：彩报与整体验收** — 实现 A11 最新缓存阅读、缩放与状态，并完成自动化和设备主流程验收。

V1.1 风险门禁：试机号和彩报不是官方开奖结果；不绕过站点访问控制；彩报图片不进入 Git 或 APK；公开发布完整图片展示前必须确认来源展示授权。

V1.1 UI 全局门禁：页面只展示用户完成任务所需的信息，不展示抓取来源、检查时间、缓存机制或内部边界；试机号只显示“试机号”和号码。正常状态必须按确认稿比例整屏展示全部核心内容。本版本不实施或验收无障碍模式和系统字体缩放布局。

## 12. V1.2 走势与彩报交互计划

V1.2 以 `docs/superpowers/specs/2026-08-02-lucky3d-v1.2-trend-caibao-interactions.md` 为唯一新增行为规格，V1.1 的列结构、期数范围和三天彩报缓存约束被本节替代。

版本映射：产品里程碑 `V1.2` 对应 Android `versionName 1.2.0` / `versionCode 2`；历史 V1 `0.1.0` / `versionCode 1` 验收记录保留不变。统一规则见 `docs/versioning.md`。

### 架构决策

1. `trial_numbers` 继续以 `issue` 为主键，历史试机号改为按期批量 Upsert，不再在每次刷新前清空表。
2. 试机号运行时只读取 55125.cn 最新 80 期列表；APK 内置 2025 全年和 2026 打包日前的历史基线并幂等导入 Room。试机号不进入 `draws` 和任何计算。
3. 走势图把表格列模型、固定栏和统一 viewport 变换分开：固定栏只绘制期号，横向偏移只作用于右侧，双指缩放在同一指针事件内原子更新比例与横纵偏移；未触边时保持双指中心，触边时夹紧内容范围。期数选择器移入标题栏并保持固定尺寸。
4. 彩报按期查询使用页面返回的真实图片地址；只允许 A11 页面和经过期号校验的 `/ftp/app/`、`/ftp/yuwang/` 图片路径。
5. 彩报缓存保留北京时间今天及此前29个自然日，清理同时删除 Room 元数据和对应私有文件；期号选择器只展示该窗口有效期号。

### 阶段A：试机号历史契约

1. **Task 28：试机号历史解析** — 解析多行历史记录、来源日期和重复/非法载荷失败模型。
   - 验收：`007` 保留前导零；重复期号、错误表头、超限载荷整页拒绝。
   - 验证：`CaibaTrialHtmlParserTest`、`CaibaTrialDataSourceTest`。
2. **Task 29：试机号按期缓存** — Room DAO、Repository 按期查询与批量写入，保留旧期历史。
   - 验收：批量写入不删除已有期号；页面失败不污染缓存；内置基线与运行时最新列表共同覆盖历史和增量。
   - 验证：LiveContent DAO/Repository 单元与迁移测试。

### 阶段B：走势图完整表格

3. **Task 30：走势图领域行模型** — 期数 10/30/60/100、试机号合并、`DrawAttributes` 属性列和缺失值。
   - 验收：行顺序、前导零、属性公式与黄金样例一致。
   - 验证：TrendViewModel 单元测试先红后绿。
4. **Task 31：走势图表头与固定栏** — 只保留期号固定栏，接入完整右侧列顺序和统计行。
   - 验收：表头没有固定开奖号，全部列顺序和未来行/统计行对齐。
   - 验证：TrendScreen 设备 UI 测试。
5. **Task 32：走势图 viewport 缩放** — 动态 fitScale、无按钮双指中心锚定的整体缩放、手势内原子累积比例与横纵偏移、统一纵向平移、命中坐标反算。
   - 验收：窄屏最小比例显示所有列；固定栏、表头、表体、字号、行高和图形等比变化；连续多帧和质心平移时未触边区域的逻辑位置不漂移，触边后无大面积空白；期数选择按钮与“走势”并排且文字不位移或消失；放大横滑和点选不乱位；手指位于右侧仍可上下浏览。
   - 验证：viewport 单元测试、API 26/API 37 手势验收。

### 阶段C：彩报30天缓存与换期

6. **Task 33：A11 按期数据源** — 指定期号页面解析、真实图片路径校验、缓存优先读取契约。
   - 验收：错期号、错栏目、错主机、错图片路径全部拒绝。
   - 验证：Cz89 数据源和解析器测试。
7. **Task 34：30天清理策略** — 30个北京时间自然日边界、按期查询和私有文件同步清理。
   - 验收：`today.minusDays(29)` 保留，前一天删除；数据库和文件不产生孤儿。
   - 验证：缓存边界、失败补偿和清理集成测试。
8. **Task 35：彩报期号选择与图片状态** — 安全区标题、上一期/较大当前期号按钮/下一期、可见最多6项且可滚动浏览全部有效期号的倒序候选菜单、切换重置缩放。
   - 验收：不显示独立“切换期号”按钮；缓存命中不联网，未缓存只抓取所选期号；图片缩放/拖动/复位可用。
   - 验证：CaibaoViewModel、CaibaoScreen API 26/API 37 设备测试。

### Checkpoint：V1.2 数据与交互

- [x] `test`、`lint`、`assembleDebug` 和种子数据校验通过。
- [x] 走势图 10/30/60/100 期、全列缩放、固定期号栏和完整列顺序通过 API 26/API 37 模拟器验收。
- [x] 彩报30天缓存边界、最近30个自然日期号切换、离线读取和失败恢复通过 API 26/API 37 全量设备自动化。
- [x] V1.2 目标交互通过窄屏与当前 Android 模拟器、48dp 触控、语义和真实数据密度检查，并经本轮产品确认。
- [ ] 正式发行前补充生产签名、物理真机、第三方彩报展示授权和最终公开发布确认。

## 13. V1.2 试机号来源修订

本节由 `docs/superpowers/specs/2026-08-03-lucky3d-caiba-trial-source-and-seed.md` 批准，替代此前彩经网分页来源、16:35 刷新时间和首页显示旧缓存的行为。

1. **Task 36：55125.cn 适配器** — 解析最新 80 期公开表格的完整期号、日期和三位试机号；校验主机、路径、MIME、UTF-8、体积、表头和重复冲突。
2. **Task 37：内置历史种子** — 构建时从 2025/2026 年度公开页生成确定性 JSON，内置完整 2025 年及 2026 打包日前记录，启动时事务化幂等导入 Room。
3. **Task 38：前台刷新与跨日显示** — 18:30 后仅在前台自动检查，失败 30 分钟后重试、每日最多三次；跨日后首页立即显示 `---`，仅展示来源日期为今天且晚于最新正式开奖期的数据。
4. **Task 39：交付验证** — 运行种子校验、数据黄金校验、JVM 测试、lint、Debug/Release 构建和相称的模拟器验收，固化 APK 身份与剩余风险。

## 14. V1.3 和值走势与版本对齐

- [x] 和值走势由原单格改为`0—27`完整值组；实际和值圆点连线，其余 27 格显示遗漏。
- [x] 和值与既有列共用统一 viewport、缩放、横纵拖动、固定期号栏和边界夹紧规则。
- [x] 四行统计覆盖每个和值：出现次数按可见窗口，当前遗漏、平均遗漏和最大遗漏按完整已确认历史；平均遗漏和最大遗漏只使用已结束遗漏段。
- [x] Android 映射更新为 `versionName 1.3.0` / `versionCode 3`；本地完成，未上传 GitHub。
