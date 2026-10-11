# 版本化公共工具包

日期：2026-10-11。状态：用户批准。取代同日最初的相邻共享仓库方案。

## 维护与分发

SanXu 已获用户授权改为公开仓库。公共采集脚本、校验实现和黄金样例唯一源码位于 SanXu 的 `shared/fc3d-tools`。Lucky3D 保持独立应用仓库，只依赖公开工具包。原 fc3d-resources 仓库停止使用，保留历史后归档，不删除远程历史。

资源包为 SanXu GitHub Release `tools-v1.0.1` 的 `fc3d-tools-1.0.1.zip` 附件。工具包版本与 Android 版本独立；该 Release 不含 APK、第三方彩报图片或应用数据库，不表示 Android 应用正式发行。

项目根目录 `shared-resources.json` 固定版本、下载 URL、ZIP SHA-256 与逐文件 SHA-256。禁止 latest、动态版本及覆盖已发布的同版本包。公共资源更新必须提升版本，重新生成包与锁文件，各项目独立验证后升级，不要求同时升级。

## 使用

需要 Python 3.10+ 和 PowerShell。在任意克隆位置的项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File tools/setup-shared-resources.ps1
powershell -ExecutionPolicy Bypass -File tools/validate-predevelopment-data.ps1
```

工具入口会自动初始化。所有系统统一使用用户主目录下的 `~/.cache/fc3d-tools/<版本>-<ZIP哈希>/`，Windows 对应 `%USERPROFILE%/.cache/fc3d-tools/`，避免不同 PowerShell 进程的 LOCALAPPDATA 虚拟化产生两份缓存。可通过 `FC3D_CACHE_DIR` 指定缓存根目录。首次获取需要联网，无需 GitHub 登录；临时网络失败最多重试三次，每次连接超时 30 秒；已缓存的固定版本可离线运行。两个项目依赖相同包时共用缓存；不同版本可共存。

ZIP 整包与清单文件均校验。下载、解包或校验失败时不安装半成品；已有损坏缓存直接拒绝执行，不覆盖用户修改。缓存安装串行加锁、暂存后原子改名。进程被强制关闭可能留下安装锁，须确认没有安装进程后才能清理该锁或换用新缓存根目录。

本地 `tools/shared_resources.py` 是各项目能够独立启动所必需的依赖安装器；工具入口和安装器随各项目保存，公共业务脚本与黄金样例不复制。缓存和 ZIP 是分发产物，不是第二份维护源码。

## 发布流程

在 SanXu 修改 shared/fc3d-tools，更新 manifest.json 版本，运行 `python tools/package-shared-resources.py`。产物写入被 Git 忽略的 artifacts/resource-packages；将生成锁文件同步到需要升级的项目。先跑工具测试和两个项目数据验证，再将 ZIP 与 .sha256 上传到 tools-v<版本> Release，发布后重做匿名下载校验。打包采用固定文件清单、排序、时间戳、LF 换行规范化和 ZIP_STORED，重复打包结果应完全一致。

## 边界

开奖快照、试机号快照、数据库、迁移、应用功能和 UI 仍各自独立。用户未另行指定时工作默认在 SanXu；本次明确授权 Lucky3D 的依赖入口迁移。历史旧项目指纹与旧验收记录不重写，新 UI 仍待 Android 编译及运行验收。

官方依据：[GitHub Release 附件](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)、[Python ZIP 标准库](https://docs.python.org/3/library/zipfile.html)。
