# Lucky3D 当前架构

> 适用版本：V1.4.0<br>
> 更新日期：2026-08-18

## 工程边界

- 单 `app` 模块、单 Activity，Kotlin + Jetpack Compose + Material 3。
- Navigation 3 管理根页面和二级页面；五个一级入口固定为首页、走势、选号、方案、彩报。
- Hilt 负责依赖注入；Coroutines、Flow、StateFlow 负责并发和不可变界面状态。
- `minSdk 26`、`compileSdk 37`、`targetSdk 37`，JDK 17。

## 分层

```text
remote / bundled assets
        ↓ 校验与类型化失败
repository / sync coordinator
        ↓ 单事务写入
Room（运行时唯一事实来源）
        ↓ Flow
ViewModel（不可变 UiState）
        ↓
Compose UI
```

- `core`：跨功能领域模型和通用 UI。
- `domain`：无 Android 依赖的号码属性、遗漏、筛选、回测和刷新规则。
- `data/remote`：官方开奖、云南公告、试机号和彩报的独立适配器。
- `data/local`：Room entity、DAO、schema 与非破坏性迁移。
- `data/repository`：本地优先读取、远端校验、事务提交和跨来源同步编排。
- `feature`：页面 ViewModel、`UiState` 和 Compose 实现；不得直接依赖 DAO 或网络 DTO。

## 数据一致性

- 正式开奖号先完成整批结构、期号、日期、号码范围、重复和连续性校验，再写入 Room。
- 云南公告与本地开奖号按期号、日期和三位号码对齐；自动和手动刷新均按“开奖号→云南公告”串行执行。
- 试机号和彩报是独立第三方内容，不进入 `draws`，也不参与指标、筛选、回测或复盘。
- 三位号码在所有层保持字符串/位模型语义，`007` 不得退化为 `7`。
- 历史回测目标期只读取此前数据，禁止未来数据泄漏。

## 持久化与升级

- APK 内置 `2017001—2026198` 共 3334 期正式开奖基线和 555 条试机号基线。
- 当前 Room 版本为 7；`app/schemas/.../1.json` 至 `7.json` 与全部迁移测试必须保留，它们是升级兼容输入，不属于可删除的旧产物。
- 用户方案、模板、备注、复盘、设置和下载的彩报文件只保存在本机。

## 当前实现入口

- 产品范围：`PRD-lucky3D.md`
- 数据同步：`docs/data-source-and-sync.md`
- 计算口径：`docs/calculation-rules.md`
- UI 规则：`docs/design-spec.md`
- 云南公告：`docs/specs/2026-08-12-home-official-announcement-and-date-query.md`
- 发布状态：`docs/releases/v1.4.0.md`
- 当前任务：`tasks/todo.md`

历史原型、设计概念图、旧截图和阶段实施计划已从当前工作树清理；如需追溯，使用 Git 历史和 GitHub Release。


## 2026-10-11 独立仓库与共享资源

仓库身份与共享资源位置见 [拆分决策](repository-separation.md)。共用工具和黄金样例由 SanXu 的 `shared/fc3d-tools` 唯一维护，通过公开版本化资源包与用户缓存复用；不再依赖相邻 fc3d-resources 仓库。运行 tools/setup-shared-resources.ps1 可获取并校验当前项目固定版本。


## 当前公共资源方式（2026-10-11）

共用工具和黄金样例由 SanXu 的 `shared/fc3d-tools` 唯一维护，通过公开版本化资源包与用户缓存复用；不再依赖相邻 fc3d-resources 仓库。运行 tools/setup-shared-resources.ps1 可获取并校验当前项目固定版本。 具体流程见[版本化工具包方案](shared-resource-packages.md)。
