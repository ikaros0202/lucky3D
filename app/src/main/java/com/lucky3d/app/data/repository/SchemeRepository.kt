package com.lucky3d.app.data.repository

import com.lucky3d.app.data.local.DrawDao
import com.lucky3d.app.data.local.ReplayEntity
import com.lucky3d.app.data.local.SchemeDao
import com.lucky3d.app.data.local.SchemeEntity
import com.lucky3d.app.data.local.TemplateEntity
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.backtest.BacktestEngine
import com.lucky3d.app.domain.backtest.BacktestReport
import com.lucky3d.app.domain.filter.FilterCondition
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.scheme.FilterTemplate
import com.lucky3d.app.domain.scheme.HistoricalDraw
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

data class SavedTemplate(
    val id: String,
    val name: String,
    val playType: PlayType,
    val conditions: List<FilterCondition>,
    val observationWindow: Int,
    val ruleVersion: Int,
    val updatedAtEpochMillis: Long,
)

data class SavedScheme(
    val id: String,
    val issue: String,
    val title: String,
    val observationWindow: Int,
    val templateId: String?,
    val playType: PlayType,
    val conditions: List<FilterCondition>,
    val candidates: List<DrawNumber>,
    val betCount: Int,
    val multiplier: Int,
    val amountYuan: Int,
    val note: String,
    val ruleVersion: Int,
    val isDrawn: Boolean,
    val copiedFromSchemeId: String?,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

data class SavedReplay(
    val schemeId: String,
    val issue: String,
    val winningNumber: DrawNumber,
    val covered: Boolean,
    val matchedCandidate: DrawNumber?,
    val revision: Int,
    val calculatedAtEpochMillis: Long,
)

data class SchemeWithReplay(
    val scheme: SavedScheme,
    val replay: SavedReplay?,
)

data class SaveTemplateRequest(
    val name: String,
    val playType: PlayType,
    val conditions: List<FilterCondition>,
    val observationWindow: Int,
)

data class SaveSchemeRequest(
    val issue: String,
    val title: String,
    val observationWindow: Int,
    val templateId: String? = null,
    val playType: PlayType,
    val conditions: List<FilterCondition>,
    val candidates: List<DrawNumber>,
    val multiplier: Int,
    val note: String,
)

interface SchemeRepository {
    val schemes: Flow<List<SchemeWithReplay>>
    val templates: Flow<List<SavedTemplate>>

    suspend fun saveTemplate(request: SaveTemplateRequest): SavedTemplate

    suspend fun saveScheme(request: SaveSchemeRequest): SavedScheme

    suspend fun copyScheme(schemeId: String, newIssue: String): SavedScheme

    suspend fun updateNote(schemeId: String, note: String): SavedScheme

    suspend fun runBacktest(
        templateId: String,
        startIssue: String,
        endIssue: String,
    ): BacktestReport
}

@Singleton
class DefaultSchemeRepository @Inject constructor(
    private val schemeDao: SchemeDao,
    private val drawDao: DrawDao,
    private val timeProvider: TimeProvider,
) : SchemeRepository {
    override val schemes: Flow<List<SchemeWithReplay>> = combine(
        schemeDao.observeSchemes(),
        schemeDao.observeReplays(),
    ) { schemes, replays ->
        val replayByScheme = replays.associateBy(ReplayEntity::schemeId)
        schemes.mapNotNull { entity ->
            runCatching {
                SchemeWithReplay(
                    scheme = entity.toSavedScheme(),
                    replay = replayByScheme[entity.id]?.toSavedReplay(),
                )
            }.getOrNull()
        }
    }

    override val templates: Flow<List<SavedTemplate>> = schemeDao.observeTemplates()
        .map { entities -> entities.mapNotNull { runCatching { it.toSavedTemplate() }.getOrNull() } }

    override suspend fun saveTemplate(request: SaveTemplateRequest): SavedTemplate {
        require(request.name.isNotBlank()) { "Template name cannot be blank" }
        require(request.observationWindow > 0)
        val now = timeProvider.nowEpochMillis()
        val entity = TemplateEntity(
            id = UUID.randomUUID().toString(),
            name = request.name.trim(),
            playType = request.playType.name,
            conditionsJson = ConditionCodec.encode(request.conditions),
            conditionsSchemaVersion = ConditionCodec.SCHEMA_VERSION,
            observationWindow = request.observationWindow,
            ruleVersion = ConditionCodec.RULE_VERSION,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        schemeDao.upsertTemplate(entity)
        return entity.toSavedTemplate()
    }

    override suspend fun saveScheme(request: SaveSchemeRequest): SavedScheme {
        require(request.issue.matches(Regex("""\d{7}""")))
        require(request.title.isNotBlank()) { "Scheme title cannot be blank" }
        require(request.observationWindow > 0)
        require(request.multiplier in 1..99)
        require(request.candidates.isNotEmpty()) { "Scheme candidates cannot be empty" }
        val now = timeProvider.nowEpochMillis()
        val candidates = request.candidates.distinct()
        val entity = SchemeEntity(
            id = UUID.randomUUID().toString(),
            issue = request.issue,
            title = request.title.trim(),
            observationWindow = request.observationWindow,
            templateId = request.templateId,
            playType = request.playType.name,
            conditionsJson = ConditionCodec.encode(request.conditions),
            conditionsSchemaVersion = ConditionCodec.SCHEMA_VERSION,
            candidateNumbersJson = JSON.encodeToString(candidates.map(DrawNumber::value)),
            betCount = candidates.size,
            multiplier = request.multiplier,
            amountYuan = Math.multiplyExact(Math.multiplyExact(candidates.size, 2), request.multiplier),
            note = request.note.trim(),
            ruleVersion = ConditionCodec.RULE_VERSION,
            isDrawn = false,
            copiedFromSchemeId = null,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        schemeDao.insertScheme(entity)
        return entity.toSavedScheme()
    }

    override suspend fun copyScheme(schemeId: String, newIssue: String): SavedScheme {
        require(newIssue.matches(Regex("""\d{7}""")))
        val original = requireNotNull(schemeDao.schemeById(schemeId)) { "Scheme not found" }
        val now = timeProvider.nowEpochMillis()
        val copy = original.copy(
            id = UUID.randomUUID().toString(),
            issue = newIssue,
            title = "${original.title}（副本）",
            isDrawn = false,
            copiedFromSchemeId = original.id,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
        schemeDao.insertScheme(copy)
        return copy.toSavedScheme()
    }

    override suspend fun updateNote(schemeId: String, note: String): SavedScheme {
        val existing = requireNotNull(schemeDao.schemeById(schemeId)) { "Scheme not found" }
        val now = timeProvider.nowEpochMillis()
        val desired = existing.copy(note = note.trim(), updatedAtEpochMillis = now)
        val saved = schemeDao.saveDraftOrCopy(
            desired = desired,
            copyId = UUID.randomUUID().toString(),
            copiedAtEpochMillis = now,
        )
        return saved.toSavedScheme()
    }

    override suspend fun runBacktest(
        templateId: String,
        startIssue: String,
        endIssue: String,
    ): BacktestReport {
        require(startIssue.matches(Regex("""\d{7}""")) && endIssue.matches(Regex("""\d{7}""")))
        require(startIssue <= endIssue)
        val template = requireNotNull(schemeDao.templateById(templateId)) {
            "Template not found"
        }.toSavedTemplate()
        val draws = drawDao.range(FIRST_V1_ISSUE, endIssue).map { entity ->
            HistoricalDraw(
                issue = entity.issue,
                number = DrawNumber.of(entity.hundreds, entity.tens, entity.ones),
                officialFingerprint = entity.officialFingerprint,
            )
        }
        return withContext(Dispatchers.Default) {
            val fullReport = BacktestEngine().run(
                template = FilterTemplate(
                    id = template.id,
                    name = template.name,
                    playType = template.playType,
                    conditions = template.conditions,
                    observationWindow = template.observationWindow,
                    ruleVersion = template.ruleVersion,
                ),
                draws = draws,
            )
            val selectedResults = fullReport.results.filter {
                it.targetIssue in startIssue..endIssue
            }
            val evaluated = selectedResults.filter {
                it.status == com.lucky3d.app.domain.backtest.BacktestStatus.EVALUATED
            }
            val coveredCount = evaluated.count { it.covered == true }
            fullReport.copy(
                results = selectedResults,
                eligibleCount = evaluated.size,
                coveredCount = coveredCount,
                coverageRate = evaluated
                    .takeIf(List<com.lucky3d.app.domain.backtest.BacktestResult>::isNotEmpty)
                    ?.let { coveredCount.toDouble() / it.size },
                averageBetCount = evaluated
                    .takeIf(List<com.lucky3d.app.domain.backtest.BacktestResult>::isNotEmpty)
                    ?.map(com.lucky3d.app.domain.backtest.BacktestResult::candidateCount)
                    ?.average(),
                cumulativeAmountYuan = evaluated.sumOf {
                    it.amountYuan
                },
            )
        }
    }

    private companion object {
        val JSON = Json
        const val FIRST_V1_ISSUE = "2017001"
    }
}

private fun TemplateEntity.toSavedTemplate(): SavedTemplate = SavedTemplate(
    id = id,
    name = name,
    playType = PlayType.valueOf(playType),
    conditions = ConditionCodec.decode(conditionsJson),
    observationWindow = observationWindow,
    ruleVersion = ruleVersion,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun SchemeEntity.toSavedScheme(): SavedScheme = SavedScheme(
    id = id,
    issue = issue,
    title = title,
    observationWindow = observationWindow,
    templateId = templateId,
    playType = PlayType.valueOf(playType),
    conditions = ConditionCodec.decode(conditionsJson),
    candidates = Json.decodeFromString<List<String>>(candidateNumbersJson).map(DrawNumber::parse),
    betCount = betCount,
    multiplier = multiplier,
    amountYuan = amountYuan,
    note = note,
    ruleVersion = ruleVersion,
    isDrawn = isDrawn,
    copiedFromSchemeId = copiedFromSchemeId,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

private fun ReplayEntity.toSavedReplay(): SavedReplay = SavedReplay(
    schemeId = schemeId,
    issue = issue,
    winningNumber = DrawNumber.parse(winningNumber),
    covered = covered,
    matchedCandidate = matchedCandidate?.let(DrawNumber::parse),
    revision = revision,
    calculatedAtEpochMillis = calculatedAtEpochMillis,
)
