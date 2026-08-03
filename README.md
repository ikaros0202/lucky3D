# Lucky3D

Lucky3D 是一款只服务于中国福利彩票“3D”玩法的 Android 本地数据分析工具，围绕“查看开奖 → 观察走势 → 设置筛选条件 → 保存方案 → 开奖后复盘”组织功能。

当前阶段版本为 **V1.2**，对应 Android `versionName 1.2.0` / `versionCode 2`。GitHub Release 中的 APK 是可安装的阶段测试版，使用 Android debug 证书签名，不代表生产签名或应用商店发行版本。

## 主要功能

- 首页：查看本地最新开奖、历史查询、同步状态和受控试机号内容。
- 走势：10/30/60/100 期切换，固定期号栏，完整属性列，双指中心锚定等比缩放，以及统一横纵 viewport 浏览。
- 选号：普通单选、组选、胆拖和条件筛选，并保留前导零。
- 方案：保存、模板、回测和开奖后复盘。
- 彩报：A11 彩报阅读、倒序期号菜单、历史期号滚动选择、图片缩放与本地缓存。

## 产品边界

- 仅支持福彩 3D，不提供其他彩种。
- 不提供账号、云同步、在线购彩、支付、充值、代购或兑奖。
- 不提供 AI 荐号、专家收费方案、“必中”承诺或未来中奖概率。
- 官方开奖结果与第三方试机号、彩报内容严格分离；第三方内容不参与指标、筛选、回测或复盘。
- 第三方彩报图片不提交到 Git，也不打包进 APK，只在设备私有目录中按规则缓存。

## 技术栈

- Kotlin、Jetpack Compose、Material 3、Navigation 3
- Room 3、OkHttp、kotlinx.serialization
- Hilt、Coroutines、Flow、StateFlow、Preferences DataStore
- 单 Activity、单向数据流、单 `app` 模块
- JDK 17，`minSdk 26`，`compileSdk 37`，`targetSdk 37`

## 本地构建

准备 JDK 17 和 Android SDK 后，在仓库根目录运行：

```powershell
.\gradlew.bat test lint assembleDebug
powershell -ExecutionPolicy Bypass -File tools/validate-predevelopment-data.ps1
```

设备测试需要已启动的 Android 模拟器或真机：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Debug APK 默认生成在 `app/build/outputs/apk/debug/app-debug.apk`。

## 文档入口

- [产品需求](PRD-lucky3D.md)
- [版本映射](docs/versioning.md)
- [V1.2 变更记录](CHANGELOG.md)
- [V1.2 阶段发布记录](docs/releases/v1.2.0.md)
- [数据来源与同步](docs/data-source-and-sync.md)
- [计算规则](docs/calculation-rules.md)
- [当前 UI 设计规格](docs/design-spec.md)
- [V1.2 交互规格](docs/superpowers/specs/2026-08-02-lucky3d-v1.2-trend-caibao-interactions.md)
- [任务计划](tasks/plan.md)与[任务状态](tasks/todo.md)

## 当前发布限制

- GitHub 中的 V1.2.0 APK 为 debug 签名测试包，不能作为长期生产升级身份。
- 当前验收证据来自 API 26 与 API 37 模拟器，不代表物理真机验收。
- 第三方彩报完整图片的公开展示仍需来源授权确认；因此当前 GitHub Release 保持草稿状态。
