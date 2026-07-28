package com.lucky3d.app.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PrepackagedDatabaseTest {
    @Test
    fun bundledDatabaseContainsTheApproved2017Baseline() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "prepackaged-lucky3d-test.db"
        context.deleteDatabase(databaseName)

        val database = Room.databaseBuilder(context, Lucky3dDatabase::class.java, databaseName)
            .createFromAsset(Lucky3dDatabase.ASSET_PATH)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build()
        try {
            val dao = database.drawDao()
            assertThat(dao.count()).isEqualTo(3334)
            assertThat(dao.range("2017001", "2017001").single().issue).isEqualTo("2017001")
            assertThat(dao.latest()?.issue).isEqualTo("2026198")
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }
}
