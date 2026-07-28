package com.lucky3d.app.domain.replay

import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.PlayConverter
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.scheme.HistoricalDraw
import com.lucky3d.app.domain.scheme.Scheme

data class ReplayRecord(
    val schemeId: String,
    val issue: String,
    val ruleVersion: Int,
    val schemeFingerprint: String,
    val officialFingerprint: String,
    val winningNumber: DrawNumber,
    val covered: Boolean,
    val matchedCandidate: DrawNumber?,
    val revision: Int,
)

object ReplayEngine {
    fun replay(
        scheme: Scheme,
        officialDraw: HistoricalDraw,
        existing: ReplayRecord? = null,
    ): ReplayRecord {
        require(scheme.issue == officialDraw.issue) {
            "Scheme and official draw must belong to the same issue"
        }
        if (
            existing != null &&
            existing.officialFingerprint == officialDraw.officialFingerprint &&
            existing.schemeFingerprint == scheme.contentFingerprint &&
            existing.ruleVersion == scheme.ruleVersion
        ) {
            return existing
        }

        val normalizedWinning = normalizeOrNull(officialDraw.number, scheme.playType)
        val normalizedCandidates = scheme.candidateNumbers
            .mapNotNull { normalizeOrNull(it, scheme.playType) }
            .distinct()
        val matched = normalizedWinning?.let { winning ->
            normalizedCandidates.firstOrNull { it == winning }
        }

        return ReplayRecord(
            schemeId = scheme.id,
            issue = scheme.issue,
            ruleVersion = scheme.ruleVersion,
            schemeFingerprint = scheme.contentFingerprint,
            officialFingerprint = officialDraw.officialFingerprint,
            winningNumber = officialDraw.number,
            covered = matched != null,
            matchedCandidate = matched,
            revision = (existing?.revision ?: 0) + 1,
        )
    }

    private fun normalizeOrNull(number: DrawNumber, playType: PlayType): DrawNumber? = when (playType) {
        PlayType.STRAIGHT -> number
        PlayType.GROUP3 -> runCatching {
            PlayConverter.canonical(number, PlayType.GROUP3)
        }.getOrNull()
        PlayType.GROUP6 -> runCatching {
            PlayConverter.canonical(number, PlayType.GROUP6)
        }.getOrNull()
    }
}
