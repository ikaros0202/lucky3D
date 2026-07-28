package com.lucky3d.app.data.repository

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.data.local.Lucky3dDatabase
import com.lucky3d.app.data.local.MIGRATION_1_2
import com.lucky3d.app.data.remote.OfficialFc3dDataSource
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assume.assumeTrue
import org.junit.Test

class ManualSyncDeviceSmokeTest {
    @Test
    fun manualRefreshAgainstOfficialEndpointCommitsMetadata() = runTest {
        val arguments = InstrumentationRegistry.getArguments()
        assumeTrue("Run explicitly with liveSync=true", arguments.getString("liveSync") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "live-sync-smoke.db"
        context.deleteDatabase(databaseName)
        val database = Room.databaseBuilder(context, Lucky3dDatabase::class.java, databaseName)
            .createFromAsset(Lucky3dDatabase.ASSET_PATH)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build()
        try {
            val coordinator = SyncCoordinator(
                remote = OfficialFc3dDataSource(OkHttpClient()),
                store = RoomSyncStore(database.drawDao()),
                replayRefresher = object : ReplayRefresher {
                    override suspend fun refresh(issues: Set<String>) = Unit
                },
                timeProvider = SystemTimeProvider,
            )

            val result = coordinator.sync(SyncTrigger.MANUAL)

            assertThat(result).isInstanceOf(SyncResult.Updated::class.java)
            assertThat(database.drawDao().syncMetadata()?.lastSuccessEpochMillis).isNotNull()
            assertThat(database.drawDao().latest()?.issue).isEqualTo("2026198")
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
