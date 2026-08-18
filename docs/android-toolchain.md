# Android 本地工具链记录

本文件记录 Lucky3D 使用的共享 Android 工具链位置。工具实际存放在公共目录，不放在某个 Codex 账号的用户缓存目录中，便于同一台电脑上的其他 Android 应用复用。

## 公共位置

| 工具 | 路径 |
| --- | --- |
| Android SDK 根目录 | `C:\Users\Public\Android\Sdk` |
| Android Platform Tools | `C:\Users\Public\Android\Sdk\platform-tools` |
| Android Emulator | `C:\Users\Public\Android\Sdk\emulator` |
| Android SDK Command-line Tools | `C:\Users\Public\Android\Sdk\cmdline-tools\latest\bin` |
| JDK 17 | `C:\Users\Public\Android\Jdk\jdk-17.0.19+10` |
| Gradle 9.5.1 | `C:\Users\Public\Android\Gradle\gradle-9.5.1` |

当前 SDK 内已存在 Android 26、35、37.0 系统镜像，编译平台为 `android-37.0`，Build Tools 包含 `36.0.0` 和 `37.0.0`。

## Lucky3D 工程配置

- `local.properties`（本机文件，不提交 Git）中的 `sdk.dir` 指向 `C:/Users/Public/Android/Sdk`。
- `gradlew.bat` 在当前进程的 `JAVA_HOME` 未设置或已失效时，回退到公共 JDK 17。
- Gradle Wrapper 的下载缓存仍由 `GRADLE_USER_HOME` 管理，默认位于用户的 `.gradle` 缓存；公共目录中的 Gradle 9.5.1 是可直接调用的 Gradle 安装。

## 用户级环境变量

新启动的终端、Android Studio 或 Codex 进程应继承以下用户级变量：

```text
ANDROID_HOME=C:\Users\Public\Android\Sdk
ANDROID_SDK_ROOT=C:\Users\Public\Android\Sdk
JAVA_HOME=C:\Users\Public\Android\Jdk\jdk-17.0.19+10
GRADLE_HOME=C:\Users\Public\Android\Gradle\gradle-9.5.1
```

用户级 `PATH` 也加入了 JDK、`platform-tools`、`emulator`、`cmdline-tools\latest\bin` 和 Gradle 的 `bin` 目录。已经打开的应用不会自动刷新环境变量，迁移后需要重新打开终端或应用。

## 兼容性说明

原来的以下路径已变为目录联接，实际文件仍只有公共目录中的一份：

- `%USERPROFILE%\.cache\newmybook-toolchain\android-sdk`
- `%USERPROFILE%\.cache\xinyue-jdk\runtime\jdk-17.0.19+10`
- `%USERPROFILE%\.cache\newmybook-toolchain\gradle-9.5.1`

这样已有工程中的旧路径引用不会立即失效。Android 模拟器的 AVD 配置仍保留在 `%USERPROFILE%\.android\avd`，它属于当前 Windows 用户的模拟器运行数据，不是 SDK 工具本体。

## 快速核对

```powershell
$env:ANDROID_HOME = 'C:\Users\Public\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:JAVA_HOME = 'C:\Users\Public\Android\Jdk\jdk-17.0.19+10'
& "$env:ANDROID_HOME\platform-tools\adb.exe" version
& 'C:\Users\Public\Android\Gradle\gradle-9.5.1\bin\gradle.bat' --version
.\gradlew.bat --version
```
