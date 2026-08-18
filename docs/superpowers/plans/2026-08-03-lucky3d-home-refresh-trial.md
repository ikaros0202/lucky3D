# Lucky3D 首页统一刷新与试机号失败状态实施计划

> **历史执行说明（2026-08-12 更新）：** 原 Superpowers 子技能依赖已退役。本文仅作为历史实施计划保留；复用时遵循仓库根 `AGENTS.md`、当前 Skill 基线和任务状态，不调用未安装 Skill。

**Goal:** 修复首页可见刷新入口不刷新试机号的问题，并按北京时间 18:30 与用户主动刷新结果区分 `---` 和“失败”。

**Architecture:** 保留 `LiveContentRepository` 的来源、校验、缓存和自动调度不变。在 `HomeViewModel` 增加仅供首页展示的手动试机号失败日期状态，由 `HomeUiState` 派生当日失败标志；普通紧凑首页和水晶首页的中央刷新入口同时调用开奖号与试机号刷新，试机号展示区只读。

**Tech Stack:** Kotlin、Jetpack Compose、Material 3、Coroutines/StateFlow、JUnit4、Compose UI tests、Gradle Wrapper。

## Global Constraints

- 保持 Android `versionName 1.3.0`、`versionCode 3`。
- 统一使用 `Asia/Shanghai`，试机号发布时间分界为每天 `18:30`。
- 试机号仍只接受今天且晚于最新正式开奖期的有效记录，三位号码保留前导零。
- 自动前台检查、30 分钟冷却、每日自动尝试次数、Repository、Room、55125.cn 来源和解析器不改动。
- 左侧试机号展示区不再可点击；详细状态区的“重试试机号”仍只刷新试机号。
- 本次不改走势图、彩报或其他一级页面，不新增依赖，不上传 GitHub，不创建 Release。

---

### Task 1: 为手动试机号失败状态补充领域状态测试

**Files:**
- Modify: `app/src/main/java/com/lucky3d/app/feature/home/HomeModels.kt`
- Modify: `app/src/main/java/com/lucky3d/app/feature/home/HomeViewModel.kt`
- Test: `app/src/test/java/com/lucky3d/app/feature/home/HomeViewModelTest.kt`

**Interfaces:**
- `HomeUiState` produces `trialManualRefreshFailed: Boolean`.
- `HomeViewModel.refreshTrial()` remains the existing public manual refresh entry and records only a post-18:30 manual failure date.

- [ ] **Step 1: Write failing tests**

Add tests to `HomeViewModelTest` using the existing `FakeLiveContentRepository`:

```kotlin
@Test
fun `post release manual trial failure is exposed as failed`() = runTest {
    val live = FakeLiveContentRepository().apply {
        trialRefreshResults.addLast(
            LiveContentRefreshResult.Failed(LiveContentFailure.NETWORK),
        )
    }
    val viewModel = homeViewModel(
        repository = FakeDrawRepository(),
        live = live,
        clock = fixedBeijing("2026-08-03T18:31:00"),
    )
    advanceUntilIdle()

    viewModel.refreshTrial()
    advanceUntilIdle()

    assertThat(viewModel.uiState.value.trialManualRefreshFailed).isTrue()
}

@Test
fun `pre release manual trial failure stays as dashes`() = runTest {
    val live = FakeLiveContentRepository().apply {
        trialRefreshResults.addLast(
            LiveContentRefreshResult.Failed(LiveContentFailure.NETWORK),
        )
    }
    val viewModel = homeViewModel(
        repository = FakeDrawRepository(),
        live = live,
        clock = fixedBeijing("2026-08-03T18:29:00"),
    )
    advanceUntilIdle()

    viewModel.refreshTrial()
    advanceUntilIdle()

    assertThat(viewModel.uiState.value.trialManualRefreshFailed).isFalse()
}

@Test
fun `automatic trial failure does not become manual failure`() = runTest {
    val live = FakeLiveContentRepository().apply {
        trialRefreshResults.addLast(
            LiveContentRefreshResult.Failed(LiveContentFailure.NETWORK),
        )
    }
    val viewModel = homeViewModel(
        repository = FakeDrawRepository(),
        live = live,
        clock = fixedBeijing("2026-08-03T18:31:00"),
    )
    advanceUntilIdle()

    viewModel.onHomeVisible()
    runCurrent()
    assertThat(viewModel.uiState.value.trialManualRefreshFailed).isFalse()
    viewModel.onHomeHidden()
}

@Test
fun `successful manual trial refresh clears previous failure`() = runTest {
    val live = FakeLiveContentRepository().apply {
        trialRefreshResults.addLast(
            LiveContentRefreshResult.Failed(LiveContentFailure.NETWORK),
        )
        trialRefreshResults.addLast(LiveContentRefreshResult.Success)
    }
    val viewModel = homeViewModel(
        repository = FakeDrawRepository(),
        live = live,
        clock = fixedBeijing("2026-08-03T18:31:00"),
    )
    advanceUntilIdle()

    viewModel.refreshTrial()
    advanceUntilIdle()
    viewModel.refreshTrial()
    advanceUntilIdle()

    assertThat(viewModel.uiState.value.trialManualRefreshFailed).isFalse()
}
```

- [ ] **Step 2: Run the focused tests and verify they fail**

Run:

```powershell
.\gradlew :app:testDebugUnitTest --tests "com.lucky3d.app.feature.home.HomeViewModelTest"
```

Expected: compilation/test failure because `HomeUiState.trialManualRefreshFailed` does not yet exist and `refreshTrial()` does not record the manual result.

- [ ] **Step 3: Implement the minimal state flow**

In `HomeModels.kt`, add:

```kotlin
val trialManualRefreshFailed: Boolean = false,
```

In `HomeViewModel.kt`:

1. Add `private val manualTrialFailureDate = MutableStateFlow<LocalDate?>(null)`.
2. Include it in the `uiState` `combine` and set `trialManualRefreshFailed` only when its date equals `today`, `beforeRelease` is false, and `currentTrial` is null.
3. Replace `refreshTrial()` with a coroutine that clears `manualTrialFailureDate`, calls `liveContentRepository.refreshTrial(LiveRefreshTrigger.MANUAL)`, and records `clock.instant().atZone(BEIJING).toLocalDate()` only for `LiveContentRefreshResult.Failed` at or after `TRIAL_RELEASE_TIME`.
4. Leave `runTrialRefreshSchedule()` unchanged; it must not set the manual failure date.

The resulting `copy` block must include:

```kotlin
trialManualRefreshFailed =
    manualFailureDate == today && !beforeRelease && currentTrial == null,
```

- [ ] **Step 4: Run the focused tests and verify they pass**

Run the same `:app:testDebugUnitTest --tests ...HomeViewModelTest` command. Expected: all existing and new HomeViewModel tests pass.

- [ ] **Step 5: Commit the state change**

```powershell
git add app/src/main/java/com/lucky3d/app/feature/home/HomeModels.kt app/src/main/java/com/lucky3d/app/feature/home/HomeViewModel.kt app/src/test/java/com/lucky3d/app/feature/home/HomeViewModelTest.kt
git commit -m "fix: track manual trial refresh failure"
```

### Task 2: Make both home layouts use one visible refresh action

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/java/com/lucky3d/app/feature/home/HomeScreen.kt`
- Modify: `app/src/main/java/com/lucky3d/app/feature/home/ApprovedCrystalHome.kt`
- Test: `app/src/androidTest/java/com/lucky3d/app/feature/home/HomeScreenTest.kt`

**Interfaces:**
- `HomeScreen` keeps `onRefresh` for official draws and `onRefreshTrial` for detailed retry; visible central refresh invokes both callbacks.
- `HomeUiState.trialManualRefreshFailed` controls compact and full-layout failure text.

- [ ] **Step 1: Write failing Compose tests**

Add a compact-layout test with separate counters:

```kotlin
@Test
fun visibleRefreshRefreshesDrawAndTrialTogether() {
    var drawRefreshes = 0
    var trialRefreshes = 0
    setHomeContent(
        state = HomeUiState(),
        onRefresh = { drawRefreshes++ },
        onRefreshTrial = { trialRefreshes++ },
    )

    composeRule
        .onNodeWithContentDescription("刷新开奖号和试机号")
        .performClick()
    composeRule.runOnIdle {
        assertThat(drawRefreshes).isEqualTo(1)
        assertThat(trialRefreshes).isEqualTo(1)
    }
}

@Test
fun compactTrialDisplayIsNotClickable() {
    setHomeContent(state = HomeUiState())

    composeRule.onNodeWithText("试机号").assertHasNoClickAction()
}

@Test
fun compactTrialShowsFailureOnlyForManualPostReleaseFailure() {
    setHomeContent(state = HomeUiState(trialManualRefreshFailed = true))

    composeRule.onNodeWithText("失败").assertIsDisplayed()
    composeRule.onNodeWithText("---").assertDoesNotExist()
}
```

Use the same callback-count assertion with a `latest` draw and `buildHomeInsights(listOf(latest))` to cover `ApprovedCrystalHome`; this verifies its central `DesignAction` also invokes both callbacks and its trial display area is not the refresh entry.

- [ ] **Step 2: Run the focused Compose tests and verify they fail**

Run:

```powershell
.\gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.lucky3d.app.feature.home.HomeScreenTest
```

Expected: the combined content description is absent, the compact trial node still has a click action, and the failure text is not rendered.

- [ ] **Step 3: Implement the visible bindings and copy**

In `strings.xml`, change `home_refresh` to `刷新开奖号和试机号` and add:

```xml
<string name="home_trial_failed">失败</string>
```

In `HomeScreen.kt`:

1. Remove `.clickable(onClick = onRefreshTrial)` from the left compact trial `Row`.
2. Render `home_trial_failed` when `state.trialManualRefreshFailed` is true and `state.trialNumber` is null; otherwise retain the existing number/`---` behavior.
3. Give the central clickable `Row` a merged content description from `home_refresh`, set the child refresh `Icon` content description to `null`, and invoke `onRefresh()` followed by `onRefreshTrial()` in the row click lambda.

In `ApprovedCrystalHome.kt`:

1. Remove the left trial `DesignAction` that currently calls `onRefreshTrial`; keep the trial title and value as display-only.
2. Render `state.trialManualRefreshFailed ? stringResource(R.string.home_trial_failed) : "---"` when no valid trial exists.
3. Change the central refresh `DesignAction` to invoke both `onRefresh()` and `onRefreshTrial()` and use the updated combined content description.

Do not change the detailed `TrialStatusRow` retry button.

- [ ] **Step 4: Run the focused Compose tests and verify they pass**

Run the same `connectedDebugAndroidTest` class command. Expected: compact and water-crystal layouts both refresh both data sources, display-only trial areas have no click action, and post-release manual failure renders `失败`.

- [ ] **Step 5: Commit the UI change**

```powershell
git add app/src/main/res/values/strings.xml app/src/main/java/com/lucky3d/app/feature/home/HomeScreen.kt app/src/main/java/com/lucky3d/app/feature/home/ApprovedCrystalHome.kt app/src/androidTest/java/com/lucky3d/app/feature/home/HomeScreenTest.kt
git commit -m "fix: refresh trial from visible home action"
```

### Task 3: Complete documentation and repository verification

**Files:**
- Modify: `tasks/todo.md`
- Verify: `PRD-lucky3D.md`, `docs/data-source-and-sync.md`, `docs/design-spec.md`, `docs/superpowers/specs/2026-08-03-lucky3d-home-refresh-trial-design.md`

- [ ] **Step 1: Record the completed fix in the current task list**

Add a checked V1.3 maintenance item under the existing V1.3 entries stating that the visible home refresh updates both official draws and trial data, left trial display is read-only, and post-18:30 manual failure displays `失败` while unpublished/automatic failure remains `---`.

- [ ] **Step 2: Run the complete verification gates**

Run:

```powershell
.\gradlew test
.\gradlew lint
.\gradlew assembleDebug
powershell -ExecutionPolicy Bypass -File tools/validate-predevelopment-data.ps1
```

Run the relevant `HomeScreenTest` connected test on API 26 and API 37 when emulators are available. Confirm `versionName 1.3.0`, `versionCode 3`, APK existence, and SHA-256. Expected: all commands complete successfully; no source, Room schema, or data-seed changes are present.

- [ ] **Step 3: Review the final diff and commit documentation**

Run:

```powershell
git diff --check
git status --short
git diff HEAD~1 --stat
```

Then commit the task-list update:

```powershell
git add tasks/todo.md
git commit -m "docs: record home trial refresh fix"
```
