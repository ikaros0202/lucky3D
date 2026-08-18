package com.lucky3d.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
import com.lucky3d.app.data.remote.YunnanAnnouncementDataResult
import com.lucky3d.app.domain.attributes.DrawNumber
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfficialDataSyncCoordinatorTest {
    @Test
    fun `manual refresh always completes draw sync before announcement refresh`() = runTest {
        val events = mutableListOf<String>()
        val coordinator = OfficialDataSyncCoordinator(
            drawRepository = FakeDrawRepository(events),
            yunnanRepository = FakeYunnanRepository(
                events,
                YunnanAnnouncementDataResult.Success(listOf(announcement())),
            ),
        )

        val result = coordinator.sync(OfficialDataSyncTrigger.MANUAL)

        assertThat(events).containsExactly("draw-manual", "yunnan").inOrder()
        assertThat(result.announcementResult)
            .isEqualTo(YunnanAnnouncementDataResult.Success(listOf(announcement())))
        assertThat(coordinator.announcementRefreshState.value)
            .isEqualTo(AnnouncementRefreshState.READY)
    }

    @Test
    fun `automatic and manual refreshes are serialized across both sources`() = runTest {
        val events = mutableListOf<String>()
        val firstDrawStarted = CompletableDeferred<Unit>()
        val releaseFirstDraw = CompletableDeferred<Unit>()
        val draws = FakeDrawRepository(
            events = events,
            firstDrawStarted = firstDrawStarted,
            releaseFirstDraw = releaseFirstDraw,
        )
        val coordinator = OfficialDataSyncCoordinator(
            drawRepository = draws,
            yunnanRepository = FakeYunnanRepository(
                events,
                YunnanAnnouncementDataResult.Success(listOf(announcement())),
            ),
        )

        val automatic = async { coordinator.sync(OfficialDataSyncTrigger.AUTO) }
        firstDrawStarted.await()
        val manual = async { coordinator.sync(OfficialDataSyncTrigger.MANUAL) }
        runCurrent()

        assertThat(draws.calls).isEqualTo(1)
        assertThat(events).containsExactly("draw-auto-start")

        releaseFirstDraw.complete(Unit)
        automatic.await()
        manual.await()

        assertThat(events).containsExactly(
            "draw-auto-start",
            "draw-auto-end",
            "yunnan",
            "draw-manual",
            "yunnan",
        ).inOrder()
    }

    @Test
    fun `empty announcement response is exposed as an error state`() = runTest {
        val coordinator = OfficialDataSyncCoordinator(
            drawRepository = FakeDrawRepository(mutableListOf()),
            yunnanRepository = FakeYunnanRepository(
                events = mutableListOf(),
                result = YunnanAnnouncementDataResult.EmptyResponse,
            ),
        )

        coordinator.sync(OfficialDataSyncTrigger.AUTO)

        assertThat(coordinator.announcementRefreshState.value)
            .isEqualTo(AnnouncementRefreshState.ERROR)
    }

    @Test
    fun `already running draw refresh does not start announcement request`() = runTest {
        val events = mutableListOf<String>()
        val yunnan = FakeYunnanRepository(
            events,
            YunnanAnnouncementDataResult.Success(listOf(announcement())),
        )
        val coordinator = OfficialDataSyncCoordinator(
            drawRepository = FakeDrawRepository(
                events = events,
                automaticResult = SyncResult.AlreadyRunning,
            ),
            yunnanRepository = yunnan,
        )

        val result = coordinator.sync(OfficialDataSyncTrigger.AUTO)

        assertThat(yunnan.refreshCalls).isEqualTo(0)
        assertThat(result.announcementResult)
            .isInstanceOf(YunnanAnnouncementDataResult.NetworkFailure::class.java)
        assertThat(coordinator.announcementRefreshState.value)
            .isEqualTo(AnnouncementRefreshState.DRAW_ERROR)
    }

    private class FakeDrawRepository(
        private val events: MutableList<String>,
        private val firstDrawStarted: CompletableDeferred<Unit>? = null,
        private val releaseFirstDraw: CompletableDeferred<Unit>? = null,
        private val automaticResult: SyncResult = SyncResult.Throttled,
    ) : DrawRepository {
        var calls = 0
        override val latestDraw: Flow<DrawRecord?> = emptyFlow()
        override val allDrawsAscending: Flow<List<DrawRecord>> = emptyFlow()
        override val syncMetadata: Flow<DrawSyncMetadata?> = emptyFlow()

        override fun observeRecent(limit: Int): Flow<List<DrawRecord>> = emptyFlow()

        override fun observe(query: DrawQuery): Flow<List<DrawRecord>> = emptyFlow()

        override suspend fun refresh(): SyncResult {
            calls += 1
            events += "draw-manual"
            return automaticResult
        }

        override suspend fun syncOnForeground(): SyncResult {
            calls += 1
            if (firstDrawStarted != null && releaseFirstDraw != null && calls == 1) {
                events += "draw-auto-start"
                firstDrawStarted.complete(Unit)
                releaseFirstDraw.await()
                events += "draw-auto-end"
            } else {
                events += "draw-auto"
            }
            return automaticResult
        }
    }

    private class FakeYunnanRepository(
        private val events: MutableList<String>,
        private val result: YunnanAnnouncementDataResult =
            YunnanAnnouncementDataResult.Success(emptyList()),
    ) : YunnanAnnouncementRepository {
        var refreshCalls = 0
        override val latestAnnouncement: Flow<YunnanAnnouncement?> = emptyFlow()

        override fun observeByIssue(issue: String): Flow<YunnanAnnouncement?> = emptyFlow()

        override suspend fun refreshRecent(limit: Int): YunnanAnnouncementDataResult {
            refreshCalls += 1
            events += "yunnan"
            return result
        }

        override suspend fun refreshIssue(issue: String): YunnanAnnouncementDataResult = result
    }

    private companion object {
        fun announcement() = YunnanAnnouncement(
            issue = "2026214",
            drawDate = "2026-08-12",
            number = DrawNumber.parse("545"),
            salesAmountYuan = 18_650_174L,
            winningTotalYuan = 10_971_227L,
            plays = listOf(
                YunnanPlayAnnouncement(YunnanPlayType.SINGLE, 5_607L, 1_040L),
                YunnanPlayAnnouncement(YunnanPlayType.GROUP3, 6_426L, 346L),
                YunnanPlayAnnouncement(YunnanPlayType.GROUP6, 0L, 173L),
            ),
            redemptionDeadline = "2026-10-12",
            sourceUpdatedAt = "2026-08-13 08:57:54",
            fetchedAtEpochMillis = 1L,
            fingerprint = "2026214",
        )
    }
}
