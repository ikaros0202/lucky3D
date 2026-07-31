package com.lucky3d.app.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveContentCleanupPersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "cleanup-${UUID.randomUUID()}.db"
    private var database: Lucky3dDatabase? = null

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun cleanupFailureSurvivesDatabaseRecreationAndSuccessfulClearPersists() = runTest {
        val failure = cleanupMetadata(lastFailureType = "FILE_IO")
        openDatabase().liveContentDao().recordRefreshMetadata(failure)
        recreateDatabase()

        val recreatedDao = requireNotNull(database).liveContentDao()
        assertThat(recreatedDao.refreshMetadata("CAIBAO_CLEANUP")).isEqualTo(failure)
        assertThat(recreatedDao.observeRefreshMetadata("CAIBAO_CLEANUP").first())
            .isEqualTo(failure)

        val cleared = failure.copy(lastFailureType = null)
        recreatedDao.recordRefreshMetadata(cleared)
        recreateDatabase()

        assertThat(requireNotNull(database).liveContentDao().refreshMetadata("CAIBAO_CLEANUP"))
            .isEqualTo(cleared)
    }

    private fun openDatabase(): Lucky3dDatabase =
        Room.databaseBuilder(context, Lucky3dDatabase::class.java, databaseName)
            .setDriver(BundledSQLiteDriver())
            .build()
            .also { database = it }

    private fun recreateDatabase() {
        database?.close()
        database = null
        openDatabase()
    }

    private fun cleanupMetadata(lastFailureType: String?) =
        LiveContentRefreshMetadataEntity(
            contentType = "CAIBAO_CLEANUP",
            attemptLocalDate = "2026-07-31",
            autoAttemptCount = 0,
            lastAttemptEpochMillis = 100L,
            lastSuccessLocalDate = null,
            lastSuccessEpochMillis = null,
            nextAllowedAutoAttemptEpochMillis = null,
            lastFailureType = lastFailureType,
        )
}
