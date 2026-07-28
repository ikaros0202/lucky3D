package com.lucky3d.app.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FeatureQueriesTest {
    @Test
    fun historyQueriesReturnNewestFirstAndRespectFilters() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, Lucky3dDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            val dao = database.drawDao()
            dao.upsertAll(
                listOf(
                    draw("2017001", "2017-01-01", 0, 0, 7),
                    draw("2025001", "2025-01-02", 1, 1, 2),
                    draw("2025002", "2025-01-03", 1, 2, 3),
                ),
            )

            assertThat(dao.observeRecent(2).first().map(DrawEntity::issue))
                .containsExactly("2025002", "2025001")
                .inOrder()
            assertThat(dao.observeByIssue("2017001").first()?.ones).isEqualTo(7)
            assertThat(dao.observeByYear("2025").first().map(DrawEntity::issue))
                .containsExactly("2025002", "2025001")
                .inOrder()
            assertThat(
                dao.observeByDateRange("2025-01-03", "2025-01-03").first().map(DrawEntity::issue),
            ).containsExactly("2025002")
        } finally {
            database.close()
        }
    }

    private fun draw(
        issue: String,
        date: String,
        hundreds: Int,
        tens: Int,
        ones: Int,
    ) = DrawEntity(
        issue = issue,
        drawDate = date,
        hundreds = hundreds,
        tens = tens,
        ones = ones,
        officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        officialFingerprint = "fingerprint-$issue",
    )
}
