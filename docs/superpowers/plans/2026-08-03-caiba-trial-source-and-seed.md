# Caiba Trial Source and Seed Implementation Plan

> **历史执行说明（2026-08-12 更新）：** 原 Superpowers 子技能依赖已退役。本文仅作为历史实施计划保留；复用时遵循仓库根 `AGENTS.md`、当前 Skill 基线和任务状态，不调用未安装 Skill。

**Goal:** Replace the CJCP trial-number integration with validated Caiba 55125 data, bundle 2025 plus 2026-to-date history for offline trend use, and guarantee that Home shows `---` until today's trial number is published.

**Architecture:** A site-specific `CaibaTrialDataSource` parses the approved 55125 HTML table into issue/date/number records. A versioned JSON asset is generated from annual archive pages and imported idempotently into Room before foreground refresh; Home filters the cached record by Beijing date while Repository validates every online record before transactionally writing it.

**Tech Stack:** Kotlin, Coroutines/Flow, Hilt, Room 3, OkHttp/MockWebServer, kotlinx.serialization, Jetpack Compose, Python 3 build-time validation script, JDK 17.

## Global Constraints

- Keep `versionName 1.2.0` and `versionCode 2` unchanged.
- Work directly on `main`; the user explicitly requested no branches or worktrees.
- Room remains the only runtime source of truth.
- Trial numbers never enter `draws` or any omission, filter, backtest, replay, or official-result calculation.
- Automatic checks start at `18:30` in `Asia/Shanghai`, use a 30-minute cooldown, and allow at most three automatic attempts per day.
- Do not add WorkManager, AlarmManager, a background service, or persistent polling.
- Preserve legacy `CJCP_SIMULATED` enum decoding for already-installed databases, but perform no new CJCP request.

---

### Task 1: Caiba HTML adapter

**Files:**
- Create: `app/src/main/java/com/lucky3d/app/data/remote/CaibaTrialHtmlParser.kt`
- Create: `app/src/main/java/com/lucky3d/app/data/remote/CaibaTrialDataSource.kt`
- Modify: `app/src/main/java/com/lucky3d/app/data/remote/TrialRemoteModels.kt`
- Modify: `app/src/main/java/com/lucky3d/app/data/repository/RepositoryModule.kt`
- Create: `app/src/test/resources/fixtures/caiba-trial-list.html`
- Create: `app/src/test/java/com/lucky3d/app/data/remote/CaibaTrialHtmlParserTest.kt`
- Create: `app/src/test/java/com/lucky3d/app/data/remote/CaibaTrialDataSourceTest.kt`
- Delete: `app/src/main/java/com/lucky3d/app/data/remote/CjcpTrialHtmlParser.kt`
- Delete: `app/src/main/java/com/lucky3d/app/data/remote/CjcpTrialDataSource.kt`
- Delete: `app/src/test/java/com/lucky3d/app/data/remote/CjcpTrialHtmlParserTest.kt`
- Delete: `app/src/test/java/com/lucky3d/app/data/remote/CjcpTrialDataSourceTest.kt`
- Delete: `app/src/test/resources/fixtures/cjcp-trial-history.html`

**Interfaces:**
- Produces: `TrialRemoteRecord(issue: String, sourceDate: LocalDate, number: String)`.
- Produces: `CaibaTrialDataSource.fetchLatest()` from the first validated row and `fetchHistoryPage(1)` from at most 80 rows.
- Consumes: fixed production endpoint `https://www.55125.cn/3dshijihao/list-80.htm`.

- [ ] **Step 1: Add the failing parser tests**

```kotlin
@Test
fun `table parser preserves date and leading zero`() {
    assertThat(parser.parse(fixture)).isEqualTo(
        RemoteParseResult.Success(
            TrialRemoteRecord("2026205", LocalDate.parse("2026-08-03"), "007"),
        ),
    )
}

@Test
fun `conflicting duplicate issue rejects the whole table`() {
    val duplicate = fixture.replace("2026204", "2026205")
    assertThat(parser.parseAll(fixture + duplicate))
        .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
}
```

- [ ] **Step 2: Run the new parser test and verify failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*CaibaTrialHtmlParserTest"
```

Expected: FAIL because `CaibaTrialHtmlParser` and the dated remote record do not exist.

- [ ] **Step 3: Implement the parser and dated record**

```kotlin
data class TrialRemoteRecord(
    val issue: String,
    val sourceDate: LocalDate,
    val number: String,
)

class CaibaTrialHtmlParser : TrialHtmlParser {
    override fun parseAll(html: String): RemoteParseResult<List<TrialRemoteRecord>> {
        // Locate the unique table whose first header row contains 期号, 日期 and 试机号.
        // Normalize comma-separated trial digits to exactly three ASCII digits.
        // Collapse identical duplicates and reject conflicting duplicates.
    }
}
```

The implementation must require the unique approved column structure, ISO date, seven-digit issue, three digits after comma normalization, and a maximum 1 MiB payload.

- [ ] **Step 4: Add failing transport tests**

```kotlin
@Test
fun `production endpoint is fixed`() {
    assertThat(CaibaTrialDataSource.DEFAULT_ENDPOINT)
        .isEqualTo("https://www.55125.cn/3dshijihao/list-80.htm")
}

@Test
fun `redirect and non html are rejected`() = runTest {
    // Assert 302 is InvalidSource and JSON content type is InvalidPayload.
}
```

- [ ] **Step 5: Implement and wire `CaibaTrialDataSource`**

The source must reject redirects, non-UTF-8 HTML declarations, non-matching final URLs, responses over 1 MiB, and page values other than `1`. `fetchLatest()` returns the first parsed row; `fetchHistoryPage(1)` returns all validated rows.

- [ ] **Step 6: Run adapter tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*CaibaTrialHtmlParserTest" --tests "*CaibaTrialDataSourceTest"
```

Expected: PASS.

- [ ] **Step 7: Commit the adapter**

```powershell
git add app/src/main/java/com/lucky3d/app/data/remote app/src/main/java/com/lucky3d/app/data/repository/RepositoryModule.kt app/src/test/java/com/lucky3d/app/data/remote app/src/test/resources/fixtures
git commit -m "feat: switch trial source to caiba"
```

### Task 2: Bundled annual trial seed and idempotent Room import

**Files:**
- Create: `tools/fetch-caiba-trial-seed.py`
- Create: `app/src/main/assets/trial/caiba-55125-trial-seed.json`
- Create: `app/src/main/java/com/lucky3d/app/data/file/BundledTrialSeedDataSource.kt`
- Create: `app/src/test/java/com/lucky3d/app/data/file/BundledTrialSeedDataSourceTest.kt`
- Modify: `app/src/main/java/com/lucky3d/app/core/model/LiveContentModels.kt`
- Modify: `app/src/main/java/com/lucky3d/app/data/local/LiveContentDao.kt`
- Modify: `app/src/main/java/com/lucky3d/app/data/repository/DefaultLiveContentRepository.kt`
- Modify: `app/src/main/java/com/lucky3d/app/data/repository/LiveContentRepository.kt`
- Modify: `app/src/main/java/com/lucky3d/app/data/repository/RepositoryModule.kt`
- Modify: `app/src/test/java/com/lucky3d/app/data/repository/DefaultLiveContentRepositoryTest.kt`
- Modify: `app/src/androidTest/java/com/lucky3d/app/data/local/LiveContentDaoTest.kt`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Produces: `BundledTrialSeedDataSource.load(): BundledTrialSeedResult`.
- Produces: `LiveContentRepository.importBundledTrialSeed(): BundledTrialSeedImportResult`.
- Produces: `LiveContentDao.upsertTrials(trials: List<TrialNumberEntity>)` as one Room transaction.

- [ ] **Step 1: Add the build-time fetcher and generate the asset**

The Python script must request only the two approved annual URLs, parse the correct table, normalize `2，8，7` to `287`, collapse identical duplicate rows, reject conflicting duplicates, require 2025 issues `001..351`, require 2026 issues `001..latest`, and write deterministic UTF-8 JSON with sorted keys/records.

Run:

```powershell
python tools/fetch-caiba-trial-seed.py --output app/src/main/assets/trial/caiba-55125-trial-seed.json
python tools/fetch-caiba-trial-seed.py --verify app/src/main/assets/trial/caiba-55125-trial-seed.json
```

Expected: 555 unique records (`2025001..2025351` and `2026001..2026204`) for the 2026-08-03 build snapshot.

- [ ] **Step 2: Add failing asset parser tests**

```kotlin
@Test
fun `bundled seed preserves complete approved range`() {
    val seed = source.load().successValue()
    assertThat(seed.records).hasSize(555)
    assertThat(seed.records.first().issue).isEqualTo("2025001")
    assertThat(seed.records.last().issue).isEqualTo("2026204")
}
```

Also test duplicate issues, invalid dates, non-three-digit numbers, and a source URL outside `www.55125.cn`.

- [ ] **Step 3: Implement the asset parser**

Use `@Serializable` DTOs local to `BundledTrialSeedDataSource`, validate the whole payload before returning domain candidates, and expose typed invalid-payload failure without partial records.

- [ ] **Step 4: Add failing import tests**

```kotlin
@Test
fun `seed import replaces legacy rows but preserves newer caiba rows`() = runTest {
    // Given one CJCP row and one CAIBA_55125 row for seed issues,
    // import must replace only the legacy/missing record in one batch.
}

@Test
fun `second seed import is a no op`() = runTest {
    assertThat(repository.importBundledTrialSeed()).isEqualTo(Imported(555))
    assertThat(repository.importBundledTrialSeed()).isEqualTo(AlreadyCurrent)
}
```

- [ ] **Step 5: Implement transactional import**

```kotlin
enum class TrialSource {
    CJCP_SIMULATED,
    CAIBA_55125,
}

sealed interface BundledTrialSeedImportResult {
    data class Imported(val count: Int) : BundledTrialSeedImportResult
    data object AlreadyCurrent : BundledTrialSeedImportResult
    data class Failed(val failure: LiveContentFailure) : BundledTrialSeedImportResult
}
```

Compare the validated seed with `store.allTrials()`, insert missing rows, replace overlapping legacy-source rows, preserve existing `CAIBA_55125` rows, and commit all changes without touching daily refresh metadata.

- [ ] **Step 6: Extend Gradle seed verification**

Make `verifySeedAndPrepackagedDatabase` also verify the trial asset exists, has exactly 555 issue fields, starts at `2025001`, ends at `2026204`, and has the reviewed SHA-256 emitted by the generator.

- [ ] **Step 7: Run seed and Room tests**

Run:

```powershell
python tools/fetch-caiba-trial-seed.py --verify app/src/main/assets/trial/caiba-55125-trial-seed.json
.\gradlew.bat testDebugUnitTest --tests "*BundledTrialSeedDataSourceTest" --tests "*DefaultLiveContentRepositoryTest"
```

Expected: PASS.

- [ ] **Step 8: Commit seed support**

```powershell
git add tools/fetch-caiba-trial-seed.py app/src/main/assets/trial app/src/main/java/com/lucky3d/app/data/file app/src/main/java/com/lucky3d/app/data/local/LiveContentDao.kt app/src/main/java/com/lucky3d/app/data/repository app/src/main/java/com/lucky3d/app/core/model/LiveContentModels.kt app/src/test app/src/androidTest app/build.gradle.kts
git commit -m "feat: bundle caiba trial history"
```

### Task 3: Daily identity validation and Home placeholder

**Files:**
- Modify: `app/src/main/java/com/lucky3d/app/data/repository/DefaultLiveContentRepository.kt`
- Modify: `app/src/main/java/com/lucky3d/app/feature/home/HomeViewModel.kt`
- Modify: `app/src/test/java/com/lucky3d/app/data/repository/DefaultLiveContentRepositoryTest.kt`
- Modify: `app/src/test/java/com/lucky3d/app/feature/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: dated `TrialRemoteRecord` from Task 1.
- Produces: Home `trialNumber` only when `trial.sourceLocalDate == today` and `trial.issue > latest official issue`.

- [ ] **Step 1: Add failing repository freshness tests**

```kotlin
@Test
fun `yesterday first row is rejected without replacing cache`() = runTest {
    val remote = TrialRemoteRecord("2026204", LocalDate.parse("2026-08-02"), "219")
    val result = repositoryAt("2026-08-03T18:30:00", remote).refreshTrial(AUTO_FOREGROUND)
    assertThat(result).isEqualTo(Failed(LiveContentFailure.INVALID_ISSUE))
}
```

Also prove today's dated record later than the official issue is committed with source `CAIBA_55125` and the 55125 source URL.

- [ ] **Step 2: Implement Repository date/source validation**

Require `record.sourceDate == today` for the latest refresh, map new rows to `TrialSource.CAIBA_55125`, and make history page import preserve each row's parsed `sourceDate`.

- [ ] **Step 3: Add failing Home stale-cache tests**

```kotlin
@Test
fun `next day hides yesterday cached trial`() = runTest {
    draws.latest.value = draw("2026204", "978")
    live.trial.value = trial("2026204", "219", LocalDate.parse("2026-08-02"))
    assertThat(viewModelAt("2026-08-03T10:00:00").uiState.value.trialNumber).isNull()
}
```

Also prove a `2026205` trial dated 2026-08-03 displays and preserves `007`.

- [ ] **Step 4: Implement Home filtering**

Inside the existing `combine`, compute Beijing `today` from `Clock` and retain a trial only when its source date is today and it is later than the latest official draw. Do not delete historical Room rows.

- [ ] **Step 5: Run freshness tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*DefaultLiveContentRepositoryTest" --tests "*HomeViewModelTest"
```

Expected: PASS.

- [ ] **Step 6: Commit freshness behavior**

```powershell
git add app/src/main/java/com/lucky3d/app/data/repository/DefaultLiveContentRepository.kt app/src/main/java/com/lucky3d/app/feature/home/HomeViewModel.kt app/src/test/java/com/lucky3d/app/data/repository/DefaultLiveContentRepositoryTest.kt app/src/test/java/com/lucky3d/app/feature/home/HomeViewModelTest.kt
git commit -m "fix: hide stale daily trial number"
```

### Task 4: 18:30 foreground scheduling and retry

**Files:**
- Modify: `app/src/main/java/com/lucky3d/app/domain/livecontent/RefreshPolicy.kt`
- Modify: `app/src/main/java/com/lucky3d/app/feature/home/HomeViewModel.kt`
- Modify: `app/src/main/java/com/lucky3d/app/app/LifecycleSyncObserver.kt`
- Modify: `app/src/test/java/com/lucky3d/app/domain/livecontent/RefreshPolicyTest.kt`
- Modify: `app/src/test/java/com/lucky3d/app/feature/home/HomeViewModelTest.kt`
- Modify: `app/src/test/java/com/lucky3d/app/app/LifecycleSyncObserverTest.kt`

**Interfaces:**
- Produces: automatic eligibility exactly at `18:30` Beijing time.
- Produces: foreground order `import seed -> refresh trial` while draw sync and Caibao cleanup remain isolated best-effort actions.

- [ ] **Step 1: Change policy tests first**

```kotlin
@Test
fun `trial automatic refresh opens exactly at 18 30`() {
    assertTrialSkip(at("2026-08-03T18:29:59"), BEFORE_RELEASE_WINDOW)
    assertThat(trialDecision(at("2026-08-03T18:30:00"))).isEqualTo(Fetch)
}
```

- [ ] **Step 2: Change `RefreshPolicy.TRIAL_RELEASE_TIME` to 18:30**

Keep the current cooldown, success-day stop, daily-limit reset, and manual bypass logic unchanged.

- [ ] **Step 3: Add failing scheduler tests**

Use `runTest` virtual time to prove:

- 18:29:59 schedules a call one second later;
- a failed eligible call schedules another eligibility check after 30 minutes;
- a clock/date change rebuilds scheduling instead of being blocked by `homeVisibleRefreshScheduled`;
- repeated `onHomeVisible()` does not create concurrent loops.

- [ ] **Step 4: Implement a single cancellable Home scheduling loop**

Replace the permanent boolean with a `Job?`. The loop waits until the next release or retry boundary, calls `refreshTrial(HOME_VISIBLE)`, stops on success/already-success/daily-limit, and waits 30 minutes after a failed/old-page result. Rebuild the loop when Home becomes visible after the previous job completed. Update `beforeTrialReleaseWindow` from Beijing time on every iteration.

- [ ] **Step 5: Import seed before foreground trial refresh**

```kotlin
launchIsolated {
    liveContentRepository.importBundledTrialSeed()
    liveContentRepository.refreshTrial(LiveRefreshTrigger.AUTO_FOREGROUND)
}
```

The other foreground maintenance calls remain isolated so one failure does not suppress the others.

- [ ] **Step 6: Run scheduler tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*RefreshPolicyTest" --tests "*HomeViewModelTest" --tests "*LifecycleSyncObserverTest"
```

Expected: PASS.

- [ ] **Step 7: Commit scheduling**

```powershell
git add app/src/main/java/com/lucky3d/app/domain/livecontent/RefreshPolicy.kt app/src/main/java/com/lucky3d/app/feature/home/HomeViewModel.kt app/src/main/java/com/lucky3d/app/app/LifecycleSyncObserver.kt app/src/test/java/com/lucky3d/app/domain/livecontent/RefreshPolicyTest.kt app/src/test/java/com/lucky3d/app/feature/home/HomeViewModelTest.kt app/src/test/java/com/lucky3d/app/app/LifecycleSyncObserverTest.kt
git commit -m "fix: refresh daily trial after 1830"
```

### Task 5: Documentation, regression verification, and APK

**Files:**
- Modify: `PRD-lucky3D.md`
- Modify: `docs/data-source-and-sync.md`
- Modify: `docs/design-spec.md`
- Modify: `docs/third-party-assets.md`
- Modify: `docs/superpowers/specs/2026-07-30-lucky3d-live-content-design.md`
- Modify: `docs/superpowers/specs/2026-08-02-lucky3d-v1.2-trend-caibao-interactions.md`
- Modify: `tasks/plan.md`
- Modify: `tasks/todo.md`
- Modify: `docs/releases/v1.2.0.md`

**Interfaces:**
- Documents: source URLs, bundled range, 18:30 rule, foreground-only scheduling, stale placeholder, seed generation and verification.
- Produces: unchanged-version APK from the verified source tree.

- [ ] **Step 1: Replace obsolete product/document statements**

Remove active claims that the trial source is CJCP, that automatic checking begins at 16:35, or that history requires five CJCP pages. Preserve older documents as historical context only when explicitly labeled superseded; add the new approved spec as the active contract.

- [ ] **Step 2: Run the data validators**

```powershell
python tools/fetch-caiba-trial-seed.py --verify app/src/main/assets/trial/caiba-55125-trial-seed.json
powershell -ExecutionPolicy Bypass -File tools/validate-predevelopment-data.ps1
```

Expected: both succeed with no data-contract violation.

- [ ] **Step 3: Run full JVM and Android static/build gates**

```powershell
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Expected: BUILD SUCCESSFUL for all four commands.

- [ ] **Step 4: Run targeted device tests when an emulator is available**

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.lucky3d.app.feature.home.HomeScreenTest"
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.lucky3d.app.feature.trend.TrendScreenTest"
```

Expected: Home and Trend suites pass; if no device exists, report that device validation remains unexecuted instead of claiming success.

- [ ] **Step 5: Inspect APK identity**

Verify the output package is `com.lucky3d.app`, `versionName=1.2.0`, `versionCode=2`, contains the trial seed asset, and record SHA-256 for the selected APK.

- [ ] **Step 6: Commit documentation and verified implementation state**

```powershell
git add PRD-lucky3D.md docs tasks
git commit -m "docs: record caiba trial source"
```

- [ ] **Step 7: Review the final diff and repository state**

Run:

```powershell
git diff HEAD~5 --check
git status --short --branch
git log --oneline -6
```

Expected: clean `main`, no extra branch/worktree, and all intended commits present.
