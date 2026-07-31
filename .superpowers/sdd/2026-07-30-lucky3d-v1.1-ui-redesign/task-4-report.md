# Task 4 报告 — 刷新策略、私有文件缓存、三日清理与 Repository

## 状态

完成。实现范围严格限定为 Task 4 brief 列出的策略、文件缓存、Repository、DAO 最小查询、DI 与生命周期触发；未修改 UI、导航、远端解析器、依赖、schema、migration 或 `docs/design-concepts/`，未加入真实彩报图片。

## 实现与文件

新增：

- `app/src/main/java/com/lucky3d/app/domain/livecontent/RefreshPolicy.kt`
- `app/src/main/java/com/lucky3d/app/data/file/CaibaoFileStore.kt`
- `app/src/main/java/com/lucky3d/app/data/repository/LiveContentRepository.kt`
- `app/src/main/java/com/lucky3d/app/data/repository/DefaultLiveContentRepository.kt`
- `app/src/test/java/com/lucky3d/app/domain/livecontent/RefreshPolicyTest.kt`
- `app/src/test/java/com/lucky3d/app/data/file/CaibaoFileStoreTest.kt`
- `app/src/test/java/com/lucky3d/app/data/repository/DefaultLiveContentRepositoryTest.kt`
- `app/src/test/java/com/lucky3d/app/app/LifecycleSyncObserverTest.kt`

修改：

- `app/src/main/java/com/lucky3d/app/data/local/LiveContentDao.kt`
  - 仅增加 `allCaibao()`，供孤儿和三日保留清理取得全部 Room 引用。
- `app/src/main/java/com/lucky3d/app/data/repository/RepositoryModule.kt`
  - 注入 `Clock.systemUTC()`、`filesDir/live-content/caibao` 文件存储和 `LiveContentRepository`。
- `app/src/main/java/com/lucky3d/app/app/LifecycleSyncObserver.kt`
  - 一次 `onStart` 独立触发官方开奖同步、试机号自动刷新和彩报清理。

## 分阶段 RED → GREEN

### 1. 纯刷新策略

RED：

```powershell
.\gradlew.bat testDebugUnitTest --tests "*RefreshPolicyTest" --console=plain
```

结果：失败，`RefreshPolicy`、公开触发/决策/metadata/失败类型尚不存在，测试编译报 unresolved reference。

GREEN：同一命令成功。14 个测试覆盖试机号 16:34:59/16:35:00、30 分钟边界、每日第 3/4 次、跨日、manual、错误触发，以及彩报 2 小时、每日次数、跨日、manual、错误触发。

### 2. 私有文件存储

RED：

```powershell
.\gradlew.bat testDebugUnitTest --tests "*CaibaoFileStoreTest" --console=plain
```

结果：失败，`CaibaoFileStore`、边界读取器、原子移动器、staged/stored 类型尚不存在。

GREEN：同一命令成功。8 个测试覆盖 JPEG/PNG、根目录与 tmp、MIME/签名不一致、空/截断/0 尺寸、8 MiB 精确边界、SHA-256 文件名、原子 commit、rollback、路径穿越及 tmp/孤儿清理。

### 3. Repository、并发、补偿与清理

RED：

```powershell
.\gradlew.bat testDebugUnitTest --tests "*DefaultLiveContentRepositoryTest" --console=plain
```

结果：首次失败于 Repository/Store 契约不存在；新增三日可见性断言后再次按预期失败，证明超过三自然日的 Room 缓存此前仍会被 Flow 发出。

GREEN：同一命令成功。16 个测试覆盖 Room Flow、固定写入、官方期号严格门禁、全部 remote failure 稳定映射、manual 不消耗自动次数、相同期号不下载、旧期号拒绝、文件先于 Room、Room 失败补偿、旧缓存保留、共享 in-flight、等待者取消、两类型并行、三日边界、孤儿/tmp 和删除失败保留 metadata。

### 4. 生命周期

RED：

```powershell
.\gradlew.bat testDebugUnitTest --tests "*LifecycleSyncObserverTest" --console=plain
```

结果：失败，既有 observer 只接受 `DrawRepository`，没有实时内容依赖和三项触发行为。

GREEN：同一命令成功。2 个测试证明一次 `onStart` 各触发一次，且官方同步抛错不阻止另外两项。

## 完整验证

```powershell
.\gradlew.bat testDebugUnitTest `
  --tests "*RefreshPolicyTest" `
  --tests "*CaibaoFileStoreTest" `
  --tests "*DefaultLiveContentRepositoryTest" `
  --tests "*LifecycleSyncObserverTest" `
  --console=plain
```

结果：成功，40/40，0 failure，0 error，0 skipped。

```powershell
.\gradlew.bat test --console=plain
```

结果：成功，135/135，0 failure，0 error，0 skipped。

```powershell
.\gradlew.bat lint --console=plain
.\gradlew.bat assembleDebug --console=plain
powershell -ExecutionPolicy Bypass -File tools/validate-predevelopment-data.ps1
git diff --check
```

结果：全部成功；数据校验为 3334 条、`2017001—2026198`、既有 seed SHA-256 未变化；`git diff --check` 无 whitespace error。当前 worktree 的空构建会话目录 `.kotlin/` 已用精确路径清理，主仓库旧目录未触碰。

## 自审

### 并发合并与取消

- 试机号和彩报各有独立共享 `Deferred`，同类型后到的 auto/manual 等待同一结果，不执行“Mutex 排队后重新请求”。
- Repository 自有 scope 执行共享请求；取消单个等待者不会取消底层请求，也不会触发第二次请求。
- 试机号和彩报使用不同 in-flight 槽，可并行；彩报刷新和清理另用同一 operation mutex，避免清理删除刷新中的 tmp 或已提交但尚未写 Room 的文件。
- 自审发现并修正宽 `catch (Exception)` 可能吞掉底层 scope 取消的问题；数据库、清理和补偿边界现在先重新抛出 `CancellationException`。

### 原子写入与补偿

- 图片写入与最终文件位于同一目录；写完执行 `flush` 与 `FileDescriptor.sync()`。
- commit 使用 `Files.move(..., ATOMIC_MOVE, REPLACE_EXISTING)`；不支持原子移动时返回 `FILE_IO`，不降级为非原子覆盖。
- 新文件原子提交后才进入 Room 事务；Room 失败立即删除本次最终文件，旧 Room 内容和旧文件保持不变。
- Room 内容 Flow 只来自 Room；Repository 不把 remote descriptor 或图片对象直接发给上层。

### 三日清理

- 北京时间仅保留 today、today-1、today-2；`cachedLocalDate < today-2` 才过期。
- 顺序为：读全部 Room 引用 → 删 tmp/孤儿 → 逐条先删过期文件 → 文件已不存在/删除成功后删 Room metadata。
- 删除失败保留对应 Room metadata 并记录 `FILE_IO`；超过三日的最新 Room 记录在清理完成前也不会被公开 Flow 发出。
- 所有显式删除只接受安全 basename，拒绝绝对路径、`..`、路径分隔符和根目录逃逸。

## 官方实现依据

- Android app-specific persistent files：<https://developer.android.com/training/data-storage/app-specific>
- `Files.move` / `ATOMIC_MOVE`：<https://developer.android.com/reference/java/nio/file/Files>
- `BitmapFactory.Options.inJustDecodeBounds`：<https://developer.android.com/reference/android/graphics/BitmapFactory.Options#inJustDecodeBounds>
- Coroutine `Deferred`：<https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-deferred/>
- Coroutine `Mutex`：<https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-mutex/>

## 未解决风险

- Task 3 的 remote 契约把空 HTML 与其他非法 HTML 都归为 `InvalidPayload`；本任务不能修改远端解析器，因此 Repository 对该上游类型稳定映射为 `INVALID_HTML`，`EMPTY_RESPONSE` 暂无可区分的上游信号。
- JVM 文件测试使用可注入 bounds reader 验证业务边界；生产默认实现已使用 Android `BitmapFactory` bounds-only API 并通过编译/lint，但本任务未新增设备端真实图片解码测试。
- Java 官方文档说明 `ATOMIC_MOVE` 下替换已存在目标的行为依赖实现；当前内容寻址文件名使新内容使用新目标名，同名表示相同 issue+hash，原子移动不受支持时安全失败。
