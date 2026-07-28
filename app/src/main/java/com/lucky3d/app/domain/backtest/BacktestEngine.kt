package com.lucky3d.app.domain.backtest

import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.NumberPool
import com.lucky3d.app.domain.filter.PlayConverter
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.scheme.FilterTemplate
import com.lucky3d.app.domain.scheme.HistoricalDraw

enum class BacktestStatus { INSUFFICIENT_SAMPLE, EVALUATED, CONDITION_CONFLICT }

data class BacktestResult(
    val targetIssue: String,
    val status: BacktestStatus,
    val candidateCount: Int,
    val covered: Boolean?,
)

data class BacktestReport(
    val templateId: String,
    val ruleVersion: Int,
    val results: List<BacktestResult>,
    val eligibleCount: Int,
    val coveredCount: Int,
    val coverageRate: Double?,
)

typealias BacktestCandidateGenerator = (
    historyBeforeTarget: List<HistoricalDraw>,
    template: FilterTemplate,
    target: HistoricalDraw,
) -> List<DrawNumber>

class BacktestEngine(
    private val candidateGenerator: BacktestCandidateGenerator = { _, template, _ ->
        NumberPool.filter(template.conditions).candidates
    },
) {
    fun run(
        template: FilterTemplate,
        draws: List<HistoricalDraw>,
    ): BacktestReport {
        require(draws.map(HistoricalDraw::issue).distinct().size == draws.size) {
            "Backtest draw issues must be unique"
        }
        val ordered = draws.sortedBy(HistoricalDraw::issue)
        val results = ordered.mapIndexed { index, target ->
            val history = ordered.subList(0, index)
            if (history.size < template.observationWindow) {
                BacktestResult(
                    targetIssue = target.issue,
                    status = BacktestStatus.INSUFFICIENT_SAMPLE,
                    candidateCount = 0,
                    covered = null,
                )
            } else {
                val candidates = candidateGenerator(history, template, target)
                val normalizedCandidates = candidates
                    .mapNotNull { normalizeOrNull(it, template.playType) }
                    .distinct()
                val winningNumber = normalizeOrNull(target.number, template.playType)
                BacktestResult(
                    targetIssue = target.issue,
                    status = BacktestStatus.EVALUATED,
                    candidateCount = normalizedCandidates.size,
                    covered = winningNumber != null && winningNumber in normalizedCandidates,
                )
            }
        }
        val evaluated = results.filter { it.status == BacktestStatus.EVALUATED }
        val coveredCount = evaluated.count { it.covered == true }
        return BacktestReport(
            templateId = template.id,
            ruleVersion = template.ruleVersion,
            results = results,
            eligibleCount = evaluated.size,
            coveredCount = coveredCount,
            coverageRate = evaluated.takeIf(List<BacktestResult>::isNotEmpty)
                ?.let { coveredCount.toDouble() / it.size },
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
