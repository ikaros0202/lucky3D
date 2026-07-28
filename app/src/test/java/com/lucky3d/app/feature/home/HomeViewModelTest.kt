package com.lucky3d.app.feature.home

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.MainDispatcherRule
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.SyncResult
import com.lucky3d.app.domain.attributes.DrawNumber
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
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

        val viewModel = HomeViewModel(repository)
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
        val viewModel = HomeViewModel(repository)
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
        val viewModel = HomeViewModel(repository)
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
        val viewModel = HomeViewModel(repository)
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

    private fun draw(issue: String, number: String) = DrawRecord(
        issue = issue,
        drawDate = "2026-07-27",
        number = DrawNumber.parse(number),
        officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        officialFingerprint = "fingerprint-$issue",
    )
}
