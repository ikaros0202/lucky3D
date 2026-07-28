package com.lucky3d.app.data.repository

import com.lucky3d.app.data.local.DrawDao
import com.lucky3d.app.data.local.ReplayEntity
import com.lucky3d.app.data.local.SchemeDao
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.replay.ReplayEngine
import com.lucky3d.app.domain.replay.ReplayRecord
import com.lucky3d.app.domain.scheme.HistoricalDraw
import com.lucky3d.app.domain.scheme.Scheme
import javax.inject.Inject
import kotlinx.serialization.json.Json

class RoomReplayRefresher @Inject constructor(
    private val drawDao: DrawDao,
    private val schemeDao: SchemeDao,
    private val timeProvider: TimeProvider,
) : ReplayRefresher {
    override suspend fun refresh(issues: Set<String>) {
        for (issue in issues) {
            val draw = drawDao.byIssue(issue) ?: continue
            val official = HistoricalDraw(
                issue = issue,
                number = DrawNumber.of(draw.hundreds, draw.tens, draw.ones),
                officialFingerprint = draw.officialFingerprint,
            )
            for (entity in schemeDao.schemesForIssue(issue)) {
                val scheme = runCatching {
                    Scheme(
                        id = entity.id,
                        issue = entity.issue,
                        playType = PlayType.valueOf(entity.playType),
                        candidateNumbers = JSON.decodeFromString<List<String>>(entity.candidateNumbersJson)
                            .map(DrawNumber::parse),
                        ruleVersion = entity.ruleVersion,
                        note = entity.note,
                    )
                }.getOrNull() ?: continue
                val old = schemeDao.replayBySchemeId(entity.id)?.let {
                    ReplayRecord(
                        schemeId = it.schemeId,
                        issue = it.issue,
                        ruleVersion = it.ruleVersion,
                        schemeFingerprint = it.schemeFingerprint,
                        officialFingerprint = it.officialFingerprint,
                        winningNumber = DrawNumber.parse(it.winningNumber),
                        covered = it.covered,
                        matchedCandidate = it.matchedCandidate?.let(DrawNumber::parse),
                        revision = it.revision,
                    )
                }
                val replay = ReplayEngine.replay(scheme, official, old)
                val now = timeProvider.nowEpochMillis()
                schemeDao.saveReplayAndMarkDrawn(
                    ReplayEntity(
                        schemeId = replay.schemeId,
                        issue = replay.issue,
                        schemeFingerprint = replay.schemeFingerprint,
                        officialFingerprint = replay.officialFingerprint,
                        winningNumber = replay.winningNumber.value,
                        covered = replay.covered,
                        matchedCandidate = replay.matchedCandidate?.value,
                        ruleVersion = replay.ruleVersion,
                        revision = replay.revision,
                        calculatedAtEpochMillis = now,
                    ),
                    updatedAtEpochMillis = now,
                )
            }
        }
    }

    private companion object {
        val JSON = Json
    }
}
