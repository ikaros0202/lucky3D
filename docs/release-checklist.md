# Lucky3D V0.1.0 历史 Release 检查清单

> 日期：2026-07-28<br>
> 结论：本地验收 Release 可安装；正式分发前需使用长期保管的生产发布密钥重新签名

当前 V1.4.0 GitHub 草稿测试版记录见 [releases/v1.4.0.md](releases/v1.4.0.md)，V1.2.0 历史阶段记录见 [releases/v1.2.0.md](releases/v1.2.0.md)。本文件只保留 V0.1.0 当时的验收事实，不用于描述当前版本。

## 构建与测试

- [x] `.\gradlew.bat clean test lint connectedDebugAndroidTest assembleRelease`
- [x] 单元测试通过。
- [x] Android 设备测试通过；API 26 全套中可选在线测试按设计跳过。
- [x] Android lint 通过。
- [x] Release 构建通过。
- [x] Room 1→2、2→3 迁移与预置数据库打开测试通过。

## 数据

- [x] `tools/validate-predevelopment-data.ps1` 通过。
- [x] 3334 期，范围 `2017001—2026198`。
- [x] 种子 SHA-256：`7D90B6074551476D5FDEDC989F001B20A9C3336476438F819E3F770C1757F4EA`。
- [x] 6 个计算黄金样例通过。
- [x] `tools/audit-fc3d-independent-sample.ps1` 重新联网执行，独立分层抽样 20/20 一致。
- [x] 预置数据库 v3 SHA-256 固定为 `89B2263DA8973DDDA3856382CCB8B939A7AC615631C769103D74FB71E81B66F2`。

## 安全与隐私

- [x] 源 Manifest 只声明 `android.permission.INTERNET`。
- [x] `android:allowBackup="false"`。
- [x] `android:usesCleartextTraffic="false"`。
- [x] 无账号、云同步、支付、购彩、外部存储、导出、分享或后台定位权限。
- [x] 打包产物另含 AndroidX 自动生成的应用内签名级 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`；它不是系统敏感权限，也不授予外部能力。
- [x] 未发现动态依赖、调试端点、API 密钥或密码。
- [x] 未发现 `Log.*`、`println`、`printStackTrace` 等记录用户备注或完整方案的代码。
- [x] 临时本地签名密钥已删除，仓库内 `.jks/.keystore/.p12` 数量为 0。

## APK 身份

| 项目 | 值 |
| --- | --- |
| 文件 | `artifacts/lucky3d-v0.1.0-release.apk`（本地构建产物，不进入 Git） |
| 包名 | `com.lucky3d.app` |
| `versionCode` | `1` |
| `versionName` | `0.1.0` |
| `minSdk` | `26` |
| `targetSdk` | `37` |
| APK SHA-256 | `D7AE063A765881181F53C5536D07660938A78F9F9C236602FDA307171A9E0835` |
| 证书 SHA-256 | `DDF2E4D899BFCB30986F872409AEF599F3359A90E769F90AD56F12AEB548A6A0` |
| 签名方案 | APK Signature Scheme v2、v3 |
| 签名用途 | 一次性本地安装验收，不是正式发布身份 |

## 安装验收

- [x] API 26 模拟器：320dp 窄屏、严格阻断 TCP、清数据后安装并首次启动；本地 `2026198 / 685` 可见，错误条说明本地数据未受影响。
- [x] API 37 模拟器：安装、五入口、在线前台同步、选号保存、强停重启恢复通过。
- [x] API 37 同版本 `adb install -r` 覆盖安装后，`release_acceptance` 方案仍存在。
- [x] Room 迁移测试覆盖历史 schema 升级且不清库。
- [x] 设置页返回首页、根页面返回桌面。

## 正式分发的后续决策

- 在安全位置生成并长期保管正式发布密钥，用该密钥重新签名。
- 在至少一台 API 26 附近设备和一台当前 Android 设备完成物理真机验收。
- 下一版本发布时再做真实 `versionCode 1 → 2` APK 升级安装；本轮只有同版本覆盖安装和 Room schema 迁移证据。
