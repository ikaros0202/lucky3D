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
    fun migrationThreeToFourPreservesExistingDataAndAddsEmptyLiveContentTables() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseFile = File(context.cacheDir, "lucky3d-migration-3-to-4.db")
        databaseFile.delete()
        val helper = MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = databaseFile,
            driver = BundledSQLiteDriver(),
            databaseClass = Lucky3dDatabase::class,
        )
        helper.createDatabase(3).use { connection ->
            connection.execSQL(
                """
                INSERT INTO draws VALUES (
                    '2026201', '2026-07-20', 0, 0, 7,
                    'https://www.cwl.gov.cn/c/2026201.shtml', 'draw-fingerprint'
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO schemes VALUES (
                    'scheme-1', '2026201', '保留方案', 30, NULL, 'STRAIGHT',
                    '{"schemaVersion":1,"ruleVersion":1,"conditions":[]}', 1,
                    '["007"]', 1, 1, 2, '保留备注', 1, 1, NULL, 10, 10
                )
                """.trimIndent(),
            )
            connection.execSQL(
                """
                INSERT INTO replays VALUES (
                    'scheme-1', '2026201', 'scheme-fingerprint', 'draw-fingerprint',
                    '007', 1, '007', 1, 1, 20
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(4, listOf(MIGRATION_3_4)).use { connection ->
            assertCount(connection, "draws", 1)
            assertCount(connection, "schemes", 1)
            assertCount(connection, "replays", 1)
            connection.prepare(
                "SELECT issue, drawDate, hundreds, tens, ones, officialFingerprint FROM draws",
            ).use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getText(0)).isEqualTo("2026201")
                assertThat(statement.getText(1)).isEqualTo("2026-07-20")
                assertThat(statement.getLong(2)).isEqualTo(0L)
                assertThat(statement.getLong(3)).isEqualTo(0L)
                assertThat(statement.getLong(4)).isEqualTo(7L)
                assertThat(statement.getText(5)).isEqualTo("draw-fingerprint")
            }
            connection.prepare(
                "SELECT issue, title, candidateNumbersJson, note, isDrawn FROM schemes WHERE id = 'scheme-1'",
            ).use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getText(0)).isEqualTo("2026201")
                assertThat(statement.getText(1)).isEqualTo("保留方案")
                assertThat(statement.getText(2)).isEqualTo("[\"007\"]")
                assertThat(statement.getText(3)).isEqualTo("保留备注")
                assertThat(statement.getLong(4)).isEqualTo(1L)
            }
            connection.prepare(
                "SELECT issue, winningNumber, matchedCandidate, covered FROM replays WHERE schemeId = 'scheme-1'",
            ).use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getText(0)).isEqualTo("2026201")
                assertThat(statement.getText(1)).isEqualTo("007")
                assertThat(statement.getText(2)).isEqualTo("007")
                assertThat(statement.getLong(3)).isEqualTo(1L)
            }
            for (table in listOf("trial_numbers", "caibao_documents", "live_content_refresh_metadata")) {
                assertCount(connection, table, 0)
            }
            assertThat(indexExists(connection, "schemes", "index_schemes_issue")).isTrue()
            assertThat(foreignKeyCheckIsClean(connection)).isTrue()
        }
        databaseFile.delete()
    }

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

    @Test
    fun migrationTwoToThreePreservesSchemesAndAddsSnapshotFields() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseFile = File(context.cacheDir, "lucky3d-migration-2-to-3.db")
        databaseFile.delete()
        val helper = MigrationTestHelper(
            instrumentation = InstrumentationRegistry.getInstrumentation(),
            file = databaseFile,
            driver = BundledSQLiteDriver(),
            databaseClass = Lucky3dDatabase::class,
        )
        helper.createDatabase(2).use { connection ->
            connection.execSQL(
                """
                INSERT INTO schemes VALUES (
                    'scheme-1', '2026199', NULL, 'STRAIGHT',
                    '{"schemaVersion":1,"ruleVersion":1,"conditions":[]}', 1,
                    '["007"]', 1, 1, 2, '保留的备注', 1, 0, NULL, 10, 10
                )
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(3, listOf(MIGRATION_2_3)).use { connection ->
            connection.prepare(
                "SELECT title, observationWindow, note FROM schemes WHERE id = 'scheme-1'",
            ).use { statement ->
                assertThat(statement.step()).isTrue()
                assertThat(statement.getText(0)).isEqualTo("")
                assertThat(statement.getLong(1)).isEqualTo(30L)
                assertThat(statement.getText(2)).isEqualTo("保留的备注")
            }
        }
        databaseFile.delete()
    }

    private fun assertCount(connection: androidx.sqlite.SQLiteConnection, table: String, expected: Long) {
        connection.prepare("SELECT COUNT(*) FROM `$table`").use { statement ->
            assertThat(statement.step()).isTrue()
            assertThat(statement.getLong(0)).isEqualTo(expected)
        }
    }

    private fun indexExists(
        connection: androidx.sqlite.SQLiteConnection,
        table: String,
        indexName: String,
    ): Boolean = connection.prepare("PRAGMA index_list(`$table`)").use { statement ->
        generateSequence { if (statement.step()) statement.getText(1) else null }.any { it == indexName }
    }

    private fun foreignKeyCheckIsClean(connection: androidx.sqlite.SQLiteConnection): Boolean =
        connection.prepare("PRAGMA foreign_key_check").use { statement -> !statement.step() }
}
