# Lucky3D

Lucky3D 是一款只服务于中国福利彩票“3D”玩法的 Android 本地数据分析工具，围绕“查看开奖 → 观察走势 → 设置筛选条件 → 保存方案 → 开奖后复盘”组织功能。

当前开发版本为 **V1.4**，对应 Android `versionName 1.4.0` / `versionCode 4`。源码已更新到 GitHub `main`，`v1.4.0` 以 Draft Release 提供 debug 签名测试 APK；它不代表生产签名或应用商店发行版本。

## 主要功能

- 首页：查看本地最新开奖、云南省开奖公告、同步状态和受控试机号内容；期号是唯一开奖详情入口，日期只作元数据。
- 开奖详情：按有效期号倒序切换，在页面内连续展示开奖结果、云南省开奖公告、奖池资金余额、玩法奖情/派奖和数据分析。
- 走势：10/30/60/100 期切换，期号与完整属性列共用统一横纵 viewport，并支持双指中心锚定等比缩放。
- 选号：普通单选、组选、胆拖和条件筛选，并保留前导零。
- 方案：保存、模板、回测和开奖后复盘。
- 彩报：A11 彩报阅读、标题同行版次、具备边界禁用状态的上一期/下一期、倒序期号菜单、图片缩放与本地缓存。

## 产品边界

- 仅支持福彩 3D，不提供其他彩种。
- 不提供账号、云同步、在线购彩、支付、充值、代购或兑奖。
- 不提供 AI 荐号、专家收费方案、“必中”承诺或未来中奖概率。
- 官方开奖结果与第三方试机号、彩报内容严格分离；第三方内容不参与指标、筛选、回测或复盘。
- 云南省开奖公告来自独立官方适配器，必须与本地开奖号按期号、日期和三位号码一致后才写入 Room。
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
.\gradlew.bat test lint assembleDebug assembleRelease -PwarningsAsErrors=true
powershell -ExecutionPolicy Bypass -File tools/validate-predevelopment-data.ps1
```

设备测试需要已启动的 Android 模拟器或真机：

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Debug APK 默认生成在 `app/build/outputs/apk/debug/app-debug.apk`；当前已核验交付文件为 `app/build/outputs/apk/debug/Lucky3D-1.4.0-test-debug.apk`。完整身份、哈希和验证结果见 [V1.4.0 草稿测试版发布记录](docs/releases/v1.4.0.md)。

## GitHub Release

- [GitHub Releases](https://github.com/ikaros0202/lucky3D/releases) 中保留历史 V1.2 草稿，并新增 `v1.4.0` 草稿测试版。
- `v1.4.0` 资产为 `Lucky3D-1.4.0-test-debug.apk`；源代码历史中不提交 APK/AAB。
- 因生产签名、物理真机与第三方彩报展示授权尚未关闭，当前草稿仅供维护者测试，不作为公开正式发行。

本机共享 Android SDK、JDK 和 Gradle 的公共位置见[Android 本地工具链记录](docs/android-toolchain.md)。

## 文档入口

- [产品需求](PRD-lucky3D.md)
- [版本映射](docs/versioning.md)
- [V1.4 变更记录](CHANGELOG.md)
- [V1.4.0 草稿测试版发布记录](docs/releases/v1.4.0.md)
- [V1.2 阶段发布记录](docs/releases/v1.2.0.md)
- [数据来源与同步](docs/data-source-and-sync.md)
- [计算规则](docs/calculation-rules.md)
- [当前 UI 设计规格](docs/design-spec.md)
- [V1.4 展示时序与页面导航规格](docs/superpowers/specs/2026-08-09-lucky3d-v1.4-display-and-navigation.md)
- [首页期号入口与云南省奖情规格](docs/specs/2026-08-12-home-official-announcement-and-date-query.md)
- [任务计划](tasks/plan.md)与[任务状态](tasks/todo.md)

## 当前发布限制

- 本地 V1.4.0 APK 与 GitHub 中的历史测试 APK 均为 debug 签名测试包，不能作为长期生产升级身份。
- V1.4 展示行为有 API 26/API 37 历史模拟器证据；本次完整发布回归在 API 37 模拟器执行，不代表物理真机验收。
- 第三方彩报完整图片的公开展示仍需来源授权确认；因此当前 GitHub Release 保持草稿状态。
- 选号与彩报的既有功能可用，但任务清单中的最终独立设计图验收仍未关闭。
