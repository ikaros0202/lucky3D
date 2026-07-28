package com.lucky3d.app.data.local

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.data.repository.DefaultSchemeRepository
import com.lucky3d.app.data.repository.SaveSchemeRequest
import com.lucky3d.app.data.repository.SaveTemplateRequest
import com.lucky3d.app.data.repository.TimeProvider
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.GlobalRequiredDigits
import com.lucky3d.app.domain.filter.PlayType
import kotlinx.coroutines.flow.first
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

    @Test
    fun repositoryRestoresCompleteTemplateSchemeAndBacktestAfterReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "scheme-repository-persistence-test.db"
        context.deleteDatabase(databaseName)

        open(context, databaseName).let { database ->
            val repository = DefaultSchemeRepository(
                schemeDao = database.schemeDao(),
                drawDao = database.drawDao(),
                timeProvider = TimeProvider { 200L },
            )
            val template = repository.saveTemplate(
                SaveTemplateRequest(
                    name = "包含0",
                    playType = PlayType.STRAIGHT,
                    conditions = listOf(GlobalRequiredDigits(setOf(0))),
                    observationWindow = 1,
                ),
            )
            repository.saveScheme(
                SaveSchemeRequest(
                    issue = "2026002",
                    title = "前导零方案",
                    observationWindow = 1,
                    templateId = template.id,
                    playType = PlayType.STRAIGHT,
                    conditions = listOf(GlobalRequiredDigits(setOf(0))),
                    candidates = listOf(DrawNumber.parse("007")),
                    multiplier = 2,
                    note = "本地备注",
                ),
            )
            database.drawDao().upsertAll(
                listOf(
                    draw("2026001", "2026-01-01", "001"),
                    draw("2026002", "2026-01-02", "007"),
                ),
            )
            database.close()
        }

        open(context, databaseName).let { database ->
            val repository = DefaultSchemeRepository(
                schemeDao = database.schemeDao(),
                drawDao = database.drawDao(),
                timeProvider = TimeProvider { 300L },
            )
            val restoredTemplate = repository.templates.first().single()
            val restoredScheme = repository.schemes.first().single().scheme

            assertThat(restoredTemplate.conditions)
                .containsExactly(GlobalRequiredDigits(setOf(0)))
            assertThat(restoredScheme.title).isEqualTo("前导零方案")
            assertThat(restoredScheme.observationWindow).isEqualTo(1)
            assertThat(restoredScheme.candidates.single().value).isEqualTo("007")
            assertThat(restoredScheme.amountYuan).isEqualTo(4)
            assertThat(restoredScheme.note).isEqualTo("本地备注")

            val first = repository.runBacktest(
                restoredTemplate.id,
                "2026002",
                "2026002",
            )
            val second = repository.runBacktest(
                restoredTemplate.id,
                "2026002",
                "2026002",
            )
            assertThat(first).isEqualTo(second)
            assertThat(first.results.single().status.name).isEqualTo("EVALUATED")
            assertThat(first.results.single().targetIssue).isEqualTo("2026002")
            database.close()
        }
        context.deleteDatabase(databaseName)
    }

    private fun open(context: Context, name: String): Lucky3dDatabase =
        Room.databaseBuilder(context, Lucky3dDatabase::class.java, name)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    private fun draw(issue: String, date: String, number: String) = DrawEntity(
        issue = issue,
        drawDate = date,
        hundreds = number[0].digitToInt(),
        tens = number[1].digitToInt(),
        ones = number[2].digitToInt(),
        officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        officialFingerprint = "$issue:$number",
    )

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
        title = "测试方案",
        observationWindow = 30,
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
