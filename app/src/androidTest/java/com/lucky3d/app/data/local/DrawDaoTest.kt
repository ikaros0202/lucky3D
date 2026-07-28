package com.lucky3d.app.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class DrawDaoTest {
    private lateinit var database: Lucky3dDatabase
    private lateinit var dao: DrawDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, Lucky3dDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.drawDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertRangeAndLatestQueriesAreOrderedByIssue() = runTest {
        dao.upsertAll(
            listOf(
                entity("2026003", 3),
                entity("2026001", 1),
                entity("2026002", 2),
            ),
        )

        assertThat(dao.count()).isEqualTo(3)
        assertThat(dao.latest()?.issue).isEqualTo("2026003")
        assertThat(dao.range("2026001", "2026002").map(DrawEntity::issue))
            .containsExactly("2026001", "2026002")
            .inOrder()
        assertThat(dao.recent(2).map(DrawEntity::issue))
            .containsExactly("2026003", "2026002")
            .inOrder()
    }

    private fun entity(issue: String, digit: Int) = DrawEntity(
        issue = issue,
        drawDate = "2026-01-01",
        hundreds = digit,
        tens = digit,
        ones = digit,
        officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        officialFingerprint = "$issue:$digit$digit$digit",
    )
}
