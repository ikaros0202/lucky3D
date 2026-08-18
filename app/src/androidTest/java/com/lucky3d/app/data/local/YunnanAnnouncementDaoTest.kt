package com.lucky3d.app.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class YunnanAnnouncementDaoTest {
    private lateinit var database: Lucky3dDatabase
    private lateinit var dao: YunnanAnnouncementDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            Lucky3dDatabase::class.java,
        ).setDriver(BundledSQLiteDriver()).build()
        dao = database.yunnanAnnouncementDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun announcementRowsPreserveLeadingZeroAndAreOrderedByIssue() = runTest {
        database.drawDao().upsertAll(
            listOf(
                draw(issue = "2024291", date = "2024-10-31", number = "006"),
                draw(issue = "2026213", date = "2026-08-11", number = "872"),
            ),
        )
        dao.commitValidated(
            listOf(
                announcement(issue = "2024291", number = "006"),
                announcement(issue = "2026213", number = "872"),
            ),
        )

        val latest = dao.observeLatest().first()
        assertThat(latest?.issue).isEqualTo("2026213")
        assertThat(latest?.prizePoolBalanceFen).isEqualTo(162_507_548L)
        assertThat(dao.observeByIssue("2024291").first()?.winningNumber).isEqualTo("006")
        assertThat(dao.recent(2).map { it.issue }).containsExactly("2026213", "2024291").inOrder()
    }

    @Test
    fun misalignedBatchIsRejectedWithoutPartialWrite() = runTest {
        database.drawDao().upsertAll(
            listOf(
                draw(issue = "2026212", date = "2026-08-10", number = "341"),
                draw(issue = "2026213", date = "2026-08-11", number = "872"),
            ),
        )

        dao.commitValidated(
            listOf(
                announcement(
                    issue = "2026212",
                    number = "341",
                    drawDate = "2026-08-10",
                ),
                announcement(
                    issue = "2026213",
                    number = "999",
                    drawDate = "2026-08-11",
                ),
            ),
        )

        assertThat(dao.recent(10)).isEmpty()
    }

    @Test
    fun correctedDrawHidesPreviouslyStoredAnnouncement() = runTest {
        val drawDao = database.drawDao()
        drawDao.upsertAll(listOf(draw(issue = "2026213", date = "2026-08-11", number = "872")))
        dao.commitValidated(listOf(announcement(issue = "2026213", number = "872")))
        assertThat(dao.observeByIssue("2026213").first()).isNotNull()

        drawDao.upsertAll(listOf(draw(issue = "2026213", date = "2026-08-11", number = "873")))

        assertThat(dao.observeByIssue("2026213").first()).isNull()
        assertThat(dao.observeLatest().first()).isNull()
    }

    @Test
    fun missingOrWrongDateDrawRejectsEntireBatch() = runTest {
        database.drawDao().upsertAll(
            listOf(draw(issue = "2026213", date = "2026-08-11", number = "872")),
        )

        val wrongDateAccepted = dao.commitValidated(
            listOf(
                announcement(
                    issue = "2026213",
                    number = "872",
                    drawDate = "2026-08-10",
                ),
            ),
        )
        val missingDrawAccepted = dao.commitValidated(
            listOf(
                announcement(
                    issue = "2026214",
                    number = "545",
                    drawDate = "2026-08-12",
                ),
            ),
        )

        assertThat(wrongDateAccepted).isFalse()
        assertThat(missingDrawAccepted).isFalse()
        assertThat(dao.recent(10)).isEmpty()
    }

    private fun announcement(
        issue: String,
        number: String,
        drawDate: String = if (issue == "2024291") "2024-10-31" else "2026-08-11",
    ) = YunnanAnnouncementEntity(
        issue = issue,
        drawDate = drawDate,
        winningNumber = number,
        salesAmountYuan = 1,
        winningTotalYuan = 1,
        prizePoolBalanceFen = if (issue == "2026213") 162_507_548L else null,
        playsJson = "[]",
        redemptionDeadline = null,
        sourceUpdatedAt = "source",
        fetchedAtEpochMillis = 1L,
        fingerprint = "fingerprint-$issue",
    )

    private fun draw(issue: String, date: String, number: String) = DrawEntity(
        issue = issue,
        drawDate = date,
        hundreds = number[0].digitToInt(),
        tens = number[1].digitToInt(),
        ones = number[2].digitToInt(),
        officialDetailUrl = "https://example.test/$issue",
        officialFingerprint = "draw-$issue-$number",
    )
}
