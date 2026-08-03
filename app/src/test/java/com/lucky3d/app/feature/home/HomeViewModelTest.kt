package com.lucky3d.app.feature.home

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.MainDispatcherRule
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.core.model.TrialSource
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.data.repository.SyncResult
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `local draw stays visible when last sync failed`() = runTest {
        val repository = FakeDrawRepository().apply {
            latest.value = draw("2026198", "007")
            metadata.value = DrawSyncMetadata(
                lastSuccessEpochMillis = 100L,
                latestIssue = "2026198",
                lastFailureType = "NETWORK",
            )
        }

        val viewModel = homeViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.latest?.number?.value).isEqualTo("007")
        assertThat(viewModel.uiState.value.syncState).isEqualTo(HomeSyncState.ERROR)
        assertThat(viewModel.uiState.value.lastSuccessEpochMillis).isEqualTo(100L)
    }

    @Test
    fun `manual refresh failure never clears local draw`() = runTest {
        val repository = FakeDrawRepository().apply {
            latest.value = draw("2026198", "685")
            refreshResult = SyncResult.Failed("INVALID_PAYLOAD")
        }
        val viewModel = homeViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.latest?.number?.value).isEqualTo("685")
        assertThat(viewModel.uiState.value.syncState).isEqualTo(HomeSyncState.ERROR)
        assertThat(viewModel.uiState.value.failureType).isEqualTo("INVALID_PAYLOAD")
    }

    @Test
    fun `manual refresh surfaces official correction`() = runTest {
        val repository = FakeDrawRepository().apply {
            latest.value = draw("2026198", "685")
            refreshResult = SyncResult.Updated(
                added = 0,
                corrected = 1,
                unchanged = 99,
                latestIssue = "2026198",
            )
        }
        val viewModel = homeViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.syncState).isEqualTo(HomeSyncState.CORRECTED)
    }

    @Test
    fun `new repository metadata replaces a stale manual refresh failure`() = runTest {
        val repository = FakeDrawRepository().apply {
            latest.value = draw("2026198", "685")
            refreshResult = SyncResult.Failed("NETWORK")
        }
        val viewModel = homeViewModel(repository)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()
        repository.metadata.value = DrawSyncMetadata(
            lastSuccessEpochMillis = 200L,
            latestIssue = "2026198",
            lastFailureType = null,
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.syncState).isEqualTo(HomeSyncState.UP_TO_DATE)
        assertThat(viewModel.uiState.value.failureType).isNull()
    }

    @Test
    fun `home state does not expose a recent draw collection`() {
        val stateFields = HomeUiState::class.java.declaredFields.map { it.name }

        assertThat(stateFields).doesNotContain("recent")
    }

    @Test
    fun `cached trial keeps leading zero and failure state`() = runTest {
        val live = FakeLiveContentRepository().apply {
            trial.value = trial("2026199", "007")
            trialState.value = LiveContentRefreshState.Failed(LiveContentFailure.NETWORK)
        }

        val viewModel = homeViewModel(FakeDrawRepository(), live)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.trialNumber?.number).isEqualTo("007")
        assertThat(viewModel.uiState.value.trialState)
            .isEqualTo(LiveContentRefreshState.Failed(LiveContentFailure.NETWORK))
        assertThat(viewModel.uiState.value.isBeforeTrialReleaseWindow).isFalse()
    }

    @Test
    fun `next day hides yesterday cached trial until current issue arrives`() = runTest {
        val draws = FakeDrawRepository().apply {
            latest.value = draw("2026204", "978")
        }
        val live = FakeLiveContentRepository().apply {
            trial.value = trial("2026204", "219", LocalDate.parse("2026-08-02"))
        }
        val viewModel = homeViewModel(
            repository = draws,
            live = live,
            clock = fixedBeijing("2026-08-03T10:00:00"),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.trialNumber).isNull()

        live.trial.value = trial("2026205", "007", LocalDate.parse("2026-08-03"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.trialNumber?.number).isEqualTo("007")
    }

    @Test
    fun `today trial not later than official draw stays hidden`() = runTest {
        val draws = FakeDrawRepository().apply {
            latest.value = draw("2026205", "123")
        }
        val live = FakeLiveContentRepository().apply {
            trial.value = trial("2026205", "007", LocalDate.parse("2026-08-03"))
        }
        val viewModel = homeViewModel(
            repository = draws,
            live = live,
            clock = fixedBeijing("2026-08-03T19:00:00"),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.trialNumber).isNull()
    }

    @Test
    fun `before 1830 exposes release-window state without inventing a trial`() = runTest {
        val viewModel = homeViewModel(
            repository = FakeDrawRepository(),
            clock = fixedBeijing("2026-07-31T18:29:00"),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.trialNumber).isNull()
        assertThat(viewModel.uiState.value.isBeforeTrialReleaseWindow).isTrue()
    }

    @Test
    fun `trial failure without cache remains explicit`() = runTest {
        val live = FakeLiveContentRepository().apply {
            trialState.value = LiveContentRefreshState.Failed(LiveContentFailure.INVALID_HTML)
        }
        val viewModel = homeViewModel(FakeDrawRepository(), live)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.trialNumber).isNull()
        assertThat(viewModel.uiState.value.trialState)
            .isEqualTo(LiveContentRefreshState.Failed(LiveContentFailure.INVALID_HTML))
    }

    @Test
    fun `home visible triggers at most one eligible trial refresh`() = runTest {
        val live = FakeLiveContentRepository()
        val viewModel = homeViewModel(FakeDrawRepository(), live)
        advanceUntilIdle()

        viewModel.onHomeVisible()
        viewModel.onHomeVisible()
        try {
            runCurrent()
            assertThat(live.trialRefreshTriggers)
                .containsExactly(LiveRefreshTrigger.HOME_VISIBLE)
        } finally {
            viewModel.onHomeHidden()
        }
    }

    @Test
    fun `failed foreground trial check retries after thirty minutes`() = runTest {
        val clock = MutableClock("2026-08-03T18:30:00")
        val live = FakeLiveContentRepository().apply {
            trialRefreshResults.addLast(
                LiveContentRefreshResult.Failed(LiveContentFailure.INVALID_ISSUE),
            )
            trialRefreshResults.addLast(LiveContentRefreshResult.Success)
        }
        val viewModel = homeViewModel(FakeDrawRepository(), live, clock)
        advanceUntilIdle()

        viewModel.onHomeVisible()
        try {
            runCurrent()
            assertThat(live.trialRefreshTriggers).hasSize(1)

            clock.advanceSeconds(30 * 60)
            advanceTimeBy(30 * 60 * 1000L)
            runCurrent()

            assertThat(live.trialRefreshTriggers).hasSize(2)
        } finally {
            viewModel.onHomeHidden()
        }
    }

    @Test
    fun `midnight tick hides previous day trial while app remains open`() = runTest {
        val clock = MutableClock("2026-08-03T19:00:00")
        val draws = FakeDrawRepository().apply {
            latest.value = draw("2026204", "978")
        }
        val live = FakeLiveContentRepository().apply {
            trial.value = trial("2026205", "007", LocalDate.parse("2026-08-03"))
        }
        val viewModel = homeViewModel(draws, live, clock)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.trialNumber?.number).isEqualTo("007")

        viewModel.onHomeVisible()
        try {
            runCurrent()
            clock.advanceSeconds(5 * 60 * 60)
            advanceTimeBy(5 * 60 * 60 * 1000L)
            runCurrent()

            assertThat(viewModel.uiState.value.trialNumber).isNull()
            assertThat(viewModel.uiState.value.isBeforeTrialReleaseWindow).isTrue()
        } finally {
            viewModel.onHomeHidden()
        }
    }

    private fun homeViewModel(
        repository: FakeDrawRepository,
        live: FakeLiveContentRepository = FakeLiveContentRepository(),
        clock: Clock = fixedBeijing("2026-07-31T18:31:00"),
    ) = HomeViewModel(repository, live, clock)

    private class FakeDrawRepository : DrawRepository {
        val latest = MutableStateFlow<DrawRecord?>(null)
        val metadata = MutableStateFlow<DrawSyncMetadata?>(null)
        val recent = MutableStateFlow<List<DrawRecord>>(emptyList())
        var refreshResult: SyncResult = SyncResult.Updated(0, 0, 0, null)

        override val latestDraw: Flow<DrawRecord?> = latest
        override val allDrawsAscending: Flow<List<DrawRecord>> = recent
        override val syncMetadata: Flow<DrawSyncMetadata?> = metadata

        override fun observeRecent(limit: Int): Flow<List<DrawRecord>> = recent

        override fun observe(query: DrawQuery): Flow<List<DrawRecord>> = recent

        override suspend fun refresh(): SyncResult = refreshResult.also { result ->
            metadata.value = when (result) {
                is SyncResult.Updated -> DrawSyncMetadata(
                    lastSuccessEpochMillis = 1L,
                    latestIssue = result.latestIssue,
                    correctedIssues = if (result.corrected > 0) {
                        setOf(result.latestIssue ?: "corrected")
                    } else {
                        emptySet()
                    },
                )
                is SyncResult.Failed -> (metadata.value ?: DrawSyncMetadata()).copy(
                    lastFailureType = result.failureType,
                    correctedIssues = emptySet(),
                )
                SyncResult.AlreadyRunning,
                SyncResult.Throttled,
                -> metadata.value
            }
        }

        override suspend fun syncOnForeground(): SyncResult = refreshResult
    }

    private class FakeLiveContentRepository : LiveContentRepository {
        val trial = MutableStateFlow<TrialNumber?>(null)
        val trialState = MutableStateFlow<LiveContentRefreshState>(LiveContentRefreshState.Idle)
        val trialRefreshTriggers = mutableListOf<LiveRefreshTrigger>()
        val trialRefreshResults = ArrayDeque<LiveContentRefreshResult>()

        override val trialNumber: Flow<TrialNumber?> = trial
        override val trialRefreshState: Flow<LiveContentRefreshState> = trialState
        override val caibaoDocument = MutableStateFlow(null)
        override val caibaoRefreshState =
            MutableStateFlow<LiveContentRefreshState>(LiveContentRefreshState.Idle)

        override suspend fun refreshTrial(
            trigger: LiveRefreshTrigger,
        ): LiveContentRefreshResult {
            trialRefreshTriggers += trigger
            return trialRefreshResults.removeFirstOrNull() ?: LiveContentRefreshResult.Success
        }

        override suspend fun refreshCaibao(
            trigger: LiveRefreshTrigger,
        ): LiveContentRefreshResult = LiveContentRefreshResult.Success

        override suspend fun readCaibaoImage(
            document: com.lucky3d.app.core.model.CaibaoDocument,
        ) = com.lucky3d.app.data.repository.CaibaoImageReadResult.Unavailable(
            LiveContentFailure.FILE_IO,
        )

        override suspend fun invalidateCaibaoImage(
            document: com.lucky3d.app.core.model.CaibaoDocument,
        ) = Unit

        override suspend fun cleanCaibaoCache() = Unit
    }

    private fun draw(issue: String, number: String) = DrawRecord(
        issue = issue,
        drawDate = "2026-07-27",
        number = DrawNumber.parse(number),
        officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        officialFingerprint = "fingerprint-$issue",
    )

    private fun trial(
        issue: String,
        number: String,
        sourceDate: LocalDate = LocalDate.parse("2026-07-31"),
    ) = TrialNumber(
        issue = issue,
        number = number,
        source = TrialSource.CJCP_SIMULATED,
        sourcePageUrl = "https://m.cjcp.cn/kjhsjh/3dls/",
        sourceLocalDate = sourceDate,
        fetchedAtEpochMillis = 1L,
    )

    private fun fixedBeijing(localDateTime: String): Clock {
        val instant = java.time.ZonedDateTime
            .parse("$localDateTime+08:00[Asia/Shanghai]")
            .toInstant()
        return Clock.fixed(instant, ZoneId.of("Asia/Shanghai"))
    }

    private class MutableClock(localDateTime: String) : Clock() {
        private var current = java.time.ZonedDateTime
            .parse("$localDateTime+08:00[Asia/Shanghai]")
            .toInstant()

        override fun getZone(): ZoneId = ZoneId.of("Asia/Shanghai")

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = current

        fun advanceSeconds(seconds: Long) {
            current = current.plusSeconds(seconds)
        }
    }
}
