package com.lucky3d.app.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SchemePersistenceTest {
    @Test
    fun templateSchemeAndReplaySurviveDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "scheme-persistence-test.db"
        context.deleteDatabase(databaseName)

        open(context, databaseName).let { database ->
            database.schemeDao().upsertTemplate(template())
            database.schemeDao().insertScheme(scheme())
            database.schemeDao().upsertReplay(replay())
            database.close()
        }

        open(context, databaseName).let { database ->
            assertThat(database.schemeDao().templateById("template-1")?.name).isEqualTo("基础模板")
            assertThat(database.schemeDao().schemeById("scheme-1")?.candidateNumbersJson)
                .isEqualTo("""["007","123"]""")
            assertThat(database.schemeDao().replayBySchemeId("scheme-1")?.covered).isTrue()
            database.close()
        }
        context.deleteDatabase(databaseName)
    }

    @Test
    fun editingDrawnSchemeCreatesCopyWithoutOverwritingOriginal() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, Lucky3dDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .build()
        try {
            val dao = database.schemeDao()
            dao.upsertTemplate(template())
            dao.insertScheme(scheme())
            dao.markSchemeDrawn("scheme-1")

            val copied = dao.saveDraftOrCopy(
                desired = scheme().copy(note = "修改后的备注"),
                copyId = "scheme-2",
                copiedAtEpochMillis = 200L,
            )

            assertThat(copied.id).isEqualTo("scheme-2")
            assertThat(copied.copiedFromSchemeId).isEqualTo("scheme-1")
            assertThat(copied.isDrawn).isFalse()
            assertThat(dao.schemeById("scheme-1")?.note).isEqualTo("原备注")
            assertThat(dao.schemeById("scheme-2")?.note).isEqualTo("修改后的备注")
        } finally {
            database.close()
        }
    }

    private fun open(context: Context, name: String): Lucky3dDatabase =
        Room.databaseBuilder(context, Lucky3dDatabase::class.java, name)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2)
            .build()

    private fun template() = TemplateEntity(
        id = "template-1",
        name = "基础模板",
        playType = "STRAIGHT",
        conditionsJson = """{"schemaVersion":1,"ruleVersion":1,"conditions":[]}""",
        conditionsSchemaVersion = 1,
        observationWindow = 30,
        ruleVersion = 1,
        createdAtEpochMillis = 100L,
        updatedAtEpochMillis = 100L,
    )

    private fun scheme() = SchemeEntity(
        id = "scheme-1",
        issue = "2026199",
        templateId = "template-1",
        playType = "STRAIGHT",
        conditionsJson = """{"schemaVersion":1,"ruleVersion":1,"conditions":[]}""",
        conditionsSchemaVersion = 1,
        candidateNumbersJson = """["007","123"]""",
        betCount = 2,
        multiplier = 1,
        amountYuan = 4,
        note = "原备注",
        ruleVersion = 1,
        isDrawn = false,
        copiedFromSchemeId = null,
        createdAtEpochMillis = 100L,
        updatedAtEpochMillis = 100L,
    )

    private fun replay() = ReplayEntity(
        schemeId = "scheme-1",
        issue = "2026199",
        schemeFingerprint = "STRAIGHT:1:123,456",
        officialFingerprint = "fingerprint",
        winningNumber = "123",
        covered = true,
        matchedCandidate = "123",
        ruleVersion = 1,
        revision = 1,
        calculatedAtEpochMillis = 150L,
    )
}
