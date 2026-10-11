# Lucky3D 仓库瘦身交接

> 更新日期：2026-10-02<br>
> 工作目录：`D:\codexproject\lucky3D`<br>
> 分支：`main`<br>
> HEAD：`3ec8f12f9235604b6ca172a2d9427b543476cf98`

## 用户目标与当前边界

完成仓库瘦身：清理已被当前实现替代的历史文档、原型、截图、旧 APK、构建缓存和未使用资源。保留当前源码、用户已有修改、Room 迁移能力、确定性数据输入和维护文档。

用户在本次续接中明确确认：一并删除 9 月 7 日首页改版后已无引用的 `home_crystal_content_shell_redesign.png` 和 10 条未使用字符串；保留仍有代码引用的导航底图。这取代了旧交接中保留两张首页素材的要求。

工作树包含前轮瘦身修改以及 2026-09-07 已完成的使用体验调整。尚未 commit、push 或修改 GitHub Draft Release。不得整体 reset、checkout 或 restore。

## 已完成

- 历史概念图、原型、截图、旧阶段规格/计划/发布文档和原先未使用资源的删除全部保留。
- 新删除旧首页整页底图，以及 `strings_home_announcement.xml` 内 10 条未使用字符串；3 条仍使用的玩法标签保留。
- 当前架构、PRD、计算、同步、UI 规格和 V1.4.0 发布记录保留；任务清单与第三方素材记录已更新。
- 253 项 JVM 测试全部通过：0 失败、0 错误、0 跳过。
- Debug/Release 构建和设备测试 Kotlin 编译成功；当前 lint 为 0 错误、14 条警告、0 条 `UnusedResources`。
- 14 条警告为 7 条 Compose 参数顺序、6 条工具链/依赖版本提示和 1 条备份规则提示；未在本轮顺带升级依赖或改行为。
- 数据校验通过：3334 期，`2017001—2026198`，6 个黄金属性样例；种子 SHA-256 为 `7D90B6074551476D5FDEDC989F001B20A9C3336476438F819E3F770C1757F4EA`。
- 新 Debug/Release APK 中的数据库和试机号种子与源码资产逐项哈希一致；资源表中旧图片已消失，导航资源保留。
- 18 份现行 Markdown（不计本交接文件）与 20 个本地链接已检查，无缺失；资源 XML 可解析；已删除路径扫描仅出现描述删除动作的文字；`git diff --check` 通过。
- `.\gradlew.bat --stop` 已成功，随后复核本轮 Gradle PID 42804、Kotlin PID 5452 均已退出；本轮未启动模拟器。

## 本轮实际成功的命令

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.lucky3d.app.data.remote.CaibaTrialDataSourceTest.cancelling in flight call reaches caller" --rerun-tasks --console=plain
.\gradlew.bat test lint assembleDebug assembleRelease compileDebugAndroidTestKotlin -PwarningsAsErrors=true --console=plain
powershell -ExecutionPolicy Bypass -File tools\validate-predevelopment-data.ps1
.\gradlew.bat --stop
git diff --check
```

完整构建命令在新增资源清理前、后分别成功；最终结果对应资源清理后的工作树。测试与构建结果详见 `docs/releases/v1.4.0.md`。

## 唯一未完成的本轮收尾

递归清理构建目录被自动审批拒绝。先校验目标均为仓库内普通目录且无 reparse point；组合删除命令被拒绝后，改为单一显式目录的安全命令仍被拒绝：

```powershell
Remove-Item -LiteralPath 'D:\codexproject\lucky3D\app\build' -Recurse -Force
```

工具仅返回 `blocked by policy`，没有提供更具体理由。不得改用其他解释器或隐藏脚本绕过拒绝。用户已授权清理；无需再次索取相同范围的授权，但仍须尊重执行工具限制。

待清理目录：

| 目录 | 当前文件数 | 当前字节数 |
| --- | ---: | ---: |
| `app/build/` | 10025 | 345865342 |
| `.gradle/` | 14 | 14045340 |
| `.kotlin/` | 0 | 0 |
| 根 `build/` | 不存在 | 0 |

合计约 343.24 MiB。旧 `artifacts/`、`.superpowers/`、`.worktrees/` 不应重新生成。`tasks/todo.md` 最后一项清理仍未勾选。

## 下一步

1. 阅读本文件和 `tasks/todo.md`，检查实时 Git 状态，保留既有修改。
2. 若工作树没有新的源码/资源变化，无需再次跑整套测试或重新生成 APK；本次验证已成功。
3. 在允许执行的环境中完成上述显式目录清理。删除前再次核对目标都位于本仓库、没有链接到其他目录，且本任务编译进程已退出。
4. 检查下面列出的保留输入仍存在；确认目标目录已消失并检查 Git diff。
5. 更新任务清单和发布记录的收尾状态，最后删除本 `HANDOFF.md`。
6. 除非用户明确要求，不要 commit 或 push，也不要发布或修改 GitHub 草稿。

## 必须保留

- `app/schemas/com.lucky3d.app.data.local.Lucky3dDatabase/1.json` 至 `7.json`。
- `DatabaseMigrations.kt`、`Lucky3dDatabaseMigrationTest.kt` 和全部其他 Kotlin 源码/测试。
- `data/fc3d-seed.json`、`app/src/main/assets/database/lucky3d.db`、`app/src/main/assets/trial/caiba-55125-trial-seed.json`。
- 相邻共享仓库中的 `fc3d-resources/testdata/fc3d-golden-cases.json` 及 `tools/` 下确定性生成/校验脚本。
- `home_crystal_navigation_shell.png`、`ic_launcher.xml` 和其余当前引用资源。
- `local.properties`，Git 历史/标签，以及当前源码、规格、发布与任务文档。

## 临时验证证据

本轮日志、原始测试 XML、最终 lint XML、APK 内容核验结果、续接前文件哈希和清理前目录清单位于仓库外临时目录：

`C:\Users\jiqin\AppData\Local\Temp\lucky3d-cleanup-20261002-01a0fc7f`

临时证据可能被系统清理，正式摘要已写入项目文档。文件哈希审查确认续接前的 Kotlin 源码、数据库/schema、种子、黄金样例和工具均未被本轮修改。

## 正式发行仍未完成

本轮未新增模拟器或真机验收。第三方彩报展示授权、选号/彩报最终设计验收、生产签名、物理设备测试、剩余 lint 处理和公开发布确认继续按 `tasks/todo.md` 保持待办。
