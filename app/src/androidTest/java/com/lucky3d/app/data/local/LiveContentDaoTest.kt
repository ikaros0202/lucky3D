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

class LiveContentDaoTest {
    private lateinit var database: Lucky3dDatabase
    private lateinit var dao: LiveContentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, Lucky3dDatabase::class.java)
            .setDriver(BundledSQLiteDriver())
            .build()
        dao = database.liveContentDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun trialHistoryPreservesPreviousRowsAndLatestStillUsesNewestRecord() = runTest {
        dao.upsertTrialAndMetadata(
            trial(issue = "2026201", number = "007", date = "2026-07-20"),
            metadata(contentType = "TRIAL_NUMBER", successDate = "2026-07-20"),
        )
        dao.upsertTrialAndMetadata(
            trial(issue = "2026202", number = "007", date = "2026-07-21"),
            metadata(contentType = "TRIAL_NUMBER", successDate = "2026-07-21"),
        )

        assertThat(dao.latestTrial()).isEqualTo(
            trial(issue = "2026202", number = "007", date = "2026-07-21"),
        )
        assertThat(dao.observeLatestTrial().first()?.sourceLocalDate).isEqualTo("2026-07-21")
        assertThat(dao.refreshMetadata("TRIAL_NUMBER")?.lastSuccessLocalDate).isEqualTo("2026-07-21")
        assertThat(dao.observeAllTrials().first().map { it.issue })
            .containsExactly("2026202", "2026201")
            .inOrder()
        assertThat(trialRowCount()).isEqualTo(2L)
    }

    @Test
    fun caibaoContentAndRefreshMetadataAreBothVisibleAfterAtomicUpsert() = runTest {
        val document = caibao(issue = "2026202", date = "2026-07-21")
        val refresh = metadata(contentType = "CAIBAO", successDate = "2026-07-21")

        dao.upsertCaibaoAndMetadata(document, refresh)

        assertThat(dao.latestCaibao()).isEqualTo(document)
        assertThat(dao.observeLatestCaibao().first()).isEqualTo(document)
        assertThat(dao.refreshMetadata("CAIBAO")).isEqualTo(refresh)
        assertThat(dao.observeRefreshMetadata("CAIBAO").first()).isEqualTo(refresh)
    }

    @Test
    fun caibaoAndMetadataBothRollBackWhenMetadataWriteFails() = runTest {
        database.useConnection(isReadOnly = false) { connection ->
            connection.usePrepared(
                """
                CREATE TRIGGER abort_caibao_metadata
                BEFORE INSERT ON live_content_refresh_metadata
                WHEN NEW.contentType = 'CAIBAO'
                BEGIN
                    SELECT RAISE(ABORT, 'metadata write failed');
                END
                """.trimIndent(),
            ) { statement -> statement.step() }
        }

        val failure = runCatching {
            dao.upsertCaibaoAndMetadata(
                caibao(issue = "2026202", date = "2026-07-21"),
                metadata(contentType = "CAIBAO", successDate = "2026-07-21"),
            )
        }.exceptionOrNull()

        assertThat(failure).isNotNull()
        assertThat(dao.latestCaibao()).isNull()
        assertThat(dao.refreshMetadata("CAIBAO")).isNull()
    }

    @Test
    fun deletingExpiredCaibaoDoesNotChangeDrawSchemeOrReplay() = runTest {
        database.drawDao().upsertAll(listOf(draw()))
        database.schemeDao().insertScheme(scheme())
        database.schemeDao().upsertReplay(replay())
        dao.upsertCaibaoAndMetadata(caibao(issue = "2026200", date = "2026-07-19"), metadata("CAIBAO"))
        dao.upsertCaibaoAndMetadata(caibao(issue = "2026201", date = "2026-07-20"), metadata("CAIBAO"))
        dao.upsertCaibaoAndMetadata(caibao(issue = "2026202", date = "2026-07-21"), metadata("CAIBAO"))

        val expiredIssues = dao.caibaoOlderThan("2026-07-20").map(CaibaoDocumentEntity::issue)
        assertThat(expiredIssues).containsExactly("2026200")
        dao.deleteCaibaoByIssues(expiredIssues)

        assertThat(dao.latestCaibao()?.issue).isEqualTo("2026202")
        assertThat(database.drawDao().byIssue("2026202")?.ones).isEqualTo(7)
        assertThat(database.schemeDao().schemeById("scheme-1")?.candidateNumbersJson).isEqualTo("[\"007\"]")
        assertThat(database.schemeDao().replayBySchemeId("scheme-1")?.winningNumber).isEqualTo("007")
    }

    private fun trial(issue: String, number: String, date: String) = TrialNumberEntity(
        issue = issue,
        number = number,
        source = "CJCP_SIMULATED",
        sourcePageUrl = "https://m.cjcp.cn/kjhsjh/3dls/",
        sourceLocalDate = date,
        fetchedAtEpochMillis = 100L,
    )

    private fun caibao(issue: String, date: String) = CaibaoDocumentEntity(
        issue = issue,
        edition = "A11",
        title = "福彩3D彩吧彩报第三版",
        sourcePageUrl = "https://m.cz89.com/tuku/A11.htm",
        imageUrl = "https://tuku.cz89.com/ftp/app/$issue/A11.jpg",
        localFileName = "$issue-A11.jpg",
        sha256 = "a".repeat(64),
        mimeType = "image/jpeg",
        width = 720,
        height = 1280,
        cachedLocalDate = date,
        fetchedAtEpochMillis = 100L,
    )

    private fun metadata(
        contentType: String,
        successDate: String? = null,
    ) = LiveContentRefreshMetadataEntity(
        contentType = contentType,
        attemptLocalDate = successDate,
        autoAttemptCount = 1,
        lastAttemptEpochMillis = 100L,
        lastSuccessLocalDate = successDate,
        lastSuccessEpochMillis = if (successDate == null) null else 100L,
        nextAllowedAutoAttemptEpochMillis = if (successDate == null) 200L else null,
        lastFailureType = null,
    )

    private fun draw() = DrawEntity(
        issue = "2026202",
        drawDate = "2026-07-21",
        hundreds = 0,
        tens = 0,
        ones = 7,
        officialDetailUrl = "https://www.cwl.gov.cn/c/2026202.shtml",
        officialFingerprint = "2026202:007",
    )

    private fun scheme() = SchemeEntity(
        id = "scheme-1",
        issue = "2026202",
        title = "保留方案",
        observationWindow = 30,
        templateId = null,
        playType = "STRAIGHT",
        conditionsJson = "{\"schemaVersion\":1,\"ruleVersion\":1,\"conditions\":[]}",
        conditionsSchemaVersion = 1,
        candidateNumbersJson = "[\"007\"]",
        betCount = 1,
        multiplier = 1,
        amountYuan = 2,
        note = "保留备注",
        ruleVersion = 1,
        isDrawn = true,
        copiedFromSchemeId = null,
        createdAtEpochMillis = 10L,
        updatedAtEpochMillis = 10L,
    )

    private fun replay() = ReplayEntity(
        schemeId = "scheme-1",
        issue = "2026202",
        schemeFingerprint = "scheme-fingerprint",
        officialFingerprint = "draw-fingerprint",
        winningNumber = "007",
        covered = true,
        matchedCandidate = "007",
        ruleVersion = 1,
        revision = 1,
        calculatedAtEpochMillis = 20L,
    )

    private suspend fun trialRowCount(): Long =
        database.useConnection(isReadOnly = true) { connection ->
            connection.usePrepared("SELECT COUNT(*) FROM trial_numbers") { statement ->
                assertThat(statement.step()).isTrue()
                statement.getLong(0)
            }
        }
}
