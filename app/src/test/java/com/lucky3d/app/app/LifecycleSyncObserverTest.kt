package com.lucky3d.app.app

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.DrawSyncMetadata
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.data.repository.SyncResult
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LifecycleSyncObserverTest {
    @Test
    fun `one onStart triggers draw sync trial auto refresh and caibao cleanup once each`() = runTest {
        val draws = FakeDrawRepository()
        val live = FakeLiveContentRepository()
        val observer = LifecycleSyncObserver(draws, live, CoroutineScope(coroutineContext))

        observer.onStart(UnusedLifecycleOwner)
        advanceUntilIdle()

        assertThat(draws.foregroundCalls).isEqualTo(1)
        assertThat(live.trialTriggers).containsExactly(LiveRefreshTrigger.AUTO_FOREGROUND)
        assertThat(live.cleanupCalls).isEqualTo(1)
    }

    @Test
    fun `a failing draw sync does not prevent either live content action`() = runTest {
        val draws = FakeDrawRepository(throwOnForeground = true)
        val live = FakeLiveContentRepository()
        val observer = LifecycleSyncObserver(draws, live, CoroutineScope(coroutineContext))

        observer.onStart(UnusedLifecycleOwner)
        advanceUntilIdle()

        assertThat(draws.foregroundCalls).isEqualTo(1)
        assertThat(live.trialTriggers).containsExactly(LiveRefreshTrigger.AUTO_FOREGROUND)
        assertThat(live.cleanupCalls).isEqualTo(1)
    }

    private class FakeDrawRepository(
        private val throwOnForeground: Boolean = false,
    ) : DrawRepository {
        var foregroundCalls = 0
        override val latestDraw: Flow<DrawRecord?> = emptyFlow()
        override val allDrawsAscending: Flow<List<DrawRecord>> = emptyFlow()
        override val syncMetadata: Flow<DrawSyncMetadata?> = emptyFlow()

        override fun observeRecent(limit: Int): Flow<List<DrawRecord>> = emptyFlow()

        override fun observe(query: DrawQuery): Flow<List<DrawRecord>> = emptyFlow()

        override suspend fun refresh(): SyncResult = SyncResult.Throttled

        override suspend fun syncOnForeground(): SyncResult {
            foregroundCalls += 1
            if (throwOnForeground) error("draw sync failed")
            return SyncResult.Throttled
        }
    }

    private class FakeLiveContentRepository : LiveContentRepository {
        var cleanupCalls = 0
        val trialTriggers = mutableListOf<LiveRefreshTrigger>()
        override val trialNumber: Flow<TrialNumber?> = emptyFlow()
        override val trialRefreshState: Flow<LiveContentRefreshState> = emptyFlow()
        override val caibaoDocument: Flow<CaibaoDocument?> = emptyFlow()
        override val caibaoRefreshState: Flow<LiveContentRefreshState> = emptyFlow()

        override suspend fun refreshTrial(
            trigger: LiveRefreshTrigger,
        ): LiveContentRefreshResult {
            trialTriggers += trigger
            return LiveContentRefreshResult.Success
        }

        override suspend fun refreshCaibao(
            trigger: LiveRefreshTrigger,
        ): LiveContentRefreshResult = LiveContentRefreshResult.Success

        override suspend fun readCaibaoImage(
            document: CaibaoDocument,
        ) = com.lucky3d.app.data.repository.CaibaoImageReadResult.Unavailable(
            com.lucky3d.app.domain.livecontent.LiveContentFailure.FILE_IO,
        )

        override suspend fun cleanCaibaoCache() {
            cleanupCalls += 1
        }
    }

    private object UnusedLifecycleOwner : LifecycleOwner {
        override val lifecycle: Lifecycle
            get() = error("Lifecycle is not read by the observer")
    }
}
