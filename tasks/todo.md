# Lucky3D 当前任务

> 当前版本：V1.4.0（`versionCode 4`）<br>
> 当前状态：源码与 debug 测试 APK 已进入 GitHub Draft Release；正式发行门禁未关闭

## 本轮仓库瘦身

- [x] 删除本地旧 APK、Gradle/Kotlin 缓存、历史 SDD 记录和构建目录。
- [x] 删除设计概念图、原型及其历史版本、旧验收截图和旧阶段发布文档。
- [x] 删除 `docs/superpowers` 历史规格/计划和根目录旧版 `design-spec.md`。
- [x] 删除 lint 已确认未使用的旧图片资源和字符串资源。
- [x] 新建当前架构入口，更新 README、AGENTS、UI 规格和第三方素材说明。
- [x] 将 `tasks/plan.md`、`tasks/todo.md` 压缩为当前状态和剩余门禁。
- [x] 确认所有本地 Markdown 链接与代码/文档路径引用有效。
- [x] 运行 JVM 测试、lint、Debug/Release 构建和数据校验。
- [ ] 完成误删审查，并再次移除验证过程重建的中间产物（2026-10-02：误删审查通过；递归删除被自动审批拒绝，`app/build/`、`.gradle/`、`.kotlin/` 仍待清理）。

2026-10-02 续接结果：经用户确认，另行删除已无引用的首页底图和 10 条字符串。清理后 253 项 JVM 测试通过，lint 为 0 错误、14 条警告、0 条未使用资源；Debug/Release 构建、设备测试代码编译和数据校验通过。Gradle/Kotlin 进程已停止；`HANDOFF.md` 保留未完成收尾步骤，完整验证记录见 `docs/releases/v1.4.0.md`。

## 正式发行待办

- [ ] 确认第三方彩报完整图片展示授权，或在正式版关闭该默认能力。
- [ ] 补齐并确认选号独立设计图，完成最终 UI 验收。
- [ ] 补齐并确认彩报独立设计图，完成最终 UI 验收。
- [ ] 配置长期生产签名并记录证书摘要、备份和恢复流程。
- [ ] 完成至少两台物理设备的安装、升级、无网和完整主流程验收。
- [ ] 处理或正式接受剩余 14 条 lint 警告（2026-10-02：未使用资源已归零）。
- [ ] 由产品负责人明确确认公开发布 `v1.4.0`。

## 保留约束

- `app/schemas` 中 Room 1→7 schema 与迁移测试不得按“旧版本”删除。
- `data/fc3d-seed.json`、预置数据库和试机号种子是离线能力输入，不是中间产物。
- 当前发布记录、Git 标签和 GitHub Release 保留历史身份；工作树内不再复制旧 APK。

## 2026-09-07 使用者体验调整（用户已批准）

- [x] 选号安全区域、固定保存汇总、直选号五列、返回编辑条件入口。
- [x] 首页内容布局、逐位置开奖前遗漏/冷热、深浅色与统一底部导航。
- [x] 走势最近行初始定位与位置快捷入口，保留完整表格和计算口径。
- [x] 完成30项聚焦单元测试、Debug lint/构建及23项API 37相关用例（含1项修正后复查）；手动核对深浅色、保存及重启，详见 `docs/ui-update-2026-09-07.md`。


## 2026-10-11 独立仓库与共享资源

仓库身份与共享资源位置见 [拆分决策](../docs/repository-separation.md)。共用工具和黄金样例由 SanXu 的 `shared/fc3d-tools` 唯一维护，通过公开版本化资源包与用户缓存复用；不再依赖相邻 fc3d-resources 仓库。运行 tools/setup-shared-resources.ps1 可获取并校验当前项目固定版本。

- [x] 独立 Git/GitHub 仓库与共享资源提取；两个项目数据校验及试机号确定性校验通过，记录见 `docs/repository-separation-verification.json`。本次未进行 Android 构建或 UI 验收。


## 当前公共资源方式（2026-10-11）

共用工具和黄金样例由 SanXu 的 `shared/fc3d-tools` 唯一维护，通过公开版本化资源包与用户缓存复用；不再依赖相邻 fc3d-resources 仓库。运行 tools/setup-shared-resources.ps1 可获取并校验当前项目固定版本。 具体流程见[版本化工具包方案](../docs/shared-resource-packages.md)。

- [x] 公共资源改为 SanXu 唯一源码＋公开 tools-v1.0.1 资源包＋用户缓存；旧共享仓库已归档。匿名下载、离线共用缓存、全新克隆重建及各自数据校验通过，见 `docs/resource-package-verification.json`。新 UI 的 Android 编译和验收未在本轮执行。
