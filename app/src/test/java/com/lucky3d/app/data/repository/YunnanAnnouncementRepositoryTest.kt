package com.lucky3d.app.data.repository

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
import com.lucky3d.app.data.local.YunnanAnnouncementDao
import com.lucky3d.app.data.local.YunnanAnnouncementEntity
import com.lucky3d.app.data.local.DrawEntity
import com.lucky3d.app.data.mapper.toEntity
import com.lucky3d.app.data.remote.YunnanAnnouncementDataResult
import com.lucky3d.app.data.remote.YunnanOfficialDataSource
import com.lucky3d.app.domain.attributes.DrawNumber
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class YunnanAnnouncementRepositoryTest {
    @Test
    fun `successful refresh commits all announcements and exposes issue flow`() = runTest {
        val announcements = listOf(announcement("2026213"), announcement("2026212"))
        val dao = FakeDao()
        val repository = DefaultYunnanAnnouncementRepository(
            dao = dao,
            remote = FakeRemote(YunnanAnnouncementDataResult.Success(announcements)),
        )

        val result = repository.refreshRecent()

        assertThat(result).isEqualTo(YunnanAnnouncementDataResult.Success(announcements))
        assertThat(dao.commitCount.get()).isEqualTo(1)
        val stored = repository.observeByIssue("2026213").first()
        assertThat(stored).isEqualTo(announcements.first())
        assertThat(stored?.prizePoolBalanceFen).isEqualTo(162_507_548L)
    }

    @Test
    fun `failed refresh does not commit`() = runTest {
        val dao = FakeDao()
        val repository = DefaultYunnanAnnouncementRepository(
            dao = dao,
            remote = FakeRemote(YunnanAnnouncementDataResult.InvalidPayload("bad")),
        )

        assertThat(repository.refreshIssue("2026213"))
            .isEqualTo(YunnanAnnouncementDataResult.InvalidPayload("bad"))
        assertThat(dao.commitCount.get()).isEqualTo(0)
    }

    @Test
    fun `announcement misaligned with local draw is rejected without a write`() = runTest {
        val announcements = listOf(announcement("2026213"))
        val dao = FakeDao(alignmentAccepted = false)
        val repository = DefaultYunnanAnnouncementRepository(
            dao = dao,
            remote = FakeRemote(YunnanAnnouncementDataResult.Success(announcements)),
        )

        val result = repository.refreshIssue("2026213")

        assertThat(result).isEqualTo(
            YunnanAnnouncementDataResult.InvalidPayload(
                "Announcement does not match local draw",
            ),
        )
        assertThat(dao.commitCount.get()).isEqualTo(1)
        assertThat(repository.observeByIssue("2026213").first()).isNull()
    }

    private fun announcement(issue: String) = YunnanAnnouncement(
        issue = issue,
        drawDate = "2026-08-11",
        number = DrawNumber.parse("872"),
        salesAmountYuan = 19_051_910,
        winningTotalYuan = 18_799_307,
        prizePoolBalanceFen = 162_507_548L,
        plays = listOf(
            YunnanPlayAnnouncement(YunnanPlayType.SINGLE, 12_155, 1_040),
            YunnanPlayAnnouncement(YunnanPlayType.GROUP3, 0, 346),
            YunnanPlayAnnouncement(YunnanPlayType.GROUP6, 15_216, 173),
        ),
        redemptionDeadline = "2026-10-10",
        sourceUpdatedAt = "2026-08-11 21:48:32",
        fetchedAtEpochMillis = 1L,
        fingerprint = "fp-$issue",
    )

    private class FakeRemote(
        private val result: YunnanAnnouncementDataResult,
    ) : YunnanOfficialDataSource {
        override suspend fun fetchRecent(limit: Int): YunnanAnnouncementDataResult = result

        override suspend fun fetchIssue(issue: String): YunnanAnnouncementDataResult = result
    }

    private class FakeDao(
        private val alignmentAccepted: Boolean = true,
    ) : YunnanAnnouncementDao {
        private val state = MutableStateFlow<YunnanAnnouncementEntity?>(null)
        private val records = linkedMapOf<String, YunnanAnnouncementEntity>()
        val commitCount = AtomicInteger()

        override suspend fun upsertAll(announcements: List<YunnanAnnouncementEntity>) {
            announcements.forEach { records[it.issue] = it }
            state.value = records.values.maxByOrNull { it.issue }
        }

        override fun observeLatest(): Flow<YunnanAnnouncementEntity?> = state

        override fun observeByIssue(issue: String): Flow<YunnanAnnouncementEntity?> =
            MutableStateFlow(records[issue])

        override suspend fun byIssue(issue: String): YunnanAnnouncementEntity? = records[issue]

        override suspend fun recent(limit: Int): List<YunnanAnnouncementEntity> =
            records.values.sortedByDescending { it.issue }.take(limit)

        override suspend fun drawsForValidation(issues: Set<String>): List<DrawEntity> = emptyList()

        override suspend fun commitValidated(
            announcements: List<YunnanAnnouncementEntity>,
        ): Boolean {
            commitCount.incrementAndGet()
            if (alignmentAccepted) upsertAll(announcements)
            return alignmentAccepted
        }
    }
}
