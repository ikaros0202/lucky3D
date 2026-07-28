package com.lucky3d.app.data.local

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.execSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.Test

class Lucky3dDatabaseMigrationTest {
    @Test
    fun migrationOneToTwoPreservesDrawsAndAddsUserTables() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseFile = File(context.cacheDir, "lucky3d-migration-1-to-2.db")
        databaseFile.delete()
        val helper = MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = databaseFile,
            driver = BundledSQLiteDriver(),
            databaseClass = Lucky3dDatabase::class,
        )
        helper.createDatabase(1).use { connection ->
            connection.execSQL(
                """
                INSERT INTO draws VALUES (
                    '2026001', '2026-01-01', 0, 0, 1,
                    'https://www.cwl.gov.cn/c/2026001.shtml', 'fingerprint'
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(2, listOf(MIGRATION_1_2)).use { connection ->
            connection.prepare("SELECT COUNT(*) FROM draws").use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getLong(0)).isEqualTo(1L)
            }
            for (table in listOf("templates", "schemes", "replays")) {
                connection.prepare("SELECT COUNT(*) FROM `$table`").use { statement ->
                    assertThat(statement.step()).isTrue()
                    assertThat(statement.getLong(0)).isEqualTo(0L)
                }
            }
        }
        databaseFile.delete()
    }
}
