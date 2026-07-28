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
    val candidates: List<DrawNumber>,
    val amountYuan: Int,
)

data class BacktestReport(
    val templateId: String,
    val ruleVersion: Int,
    val results: List<BacktestResult>,
    val eligibleCount: Int,
    val coveredCount: Int,
    val coverageRate: Double?,
    val averageBetCount: Double?,
    val cumulativeAmountYuan: Int,
)

typealias BacktestCandidateGenerator = (
    historyBeforeTarget: List<HistoricalDraw>,
    template: FilterTemplate,
    target: HistoricalDraw,
) -> List<DrawNumber>

class BacktestEngine(
    private val candidateGenerator: BacktestCandidateGenerator? = null,
) {
    fun run(
        template: FilterTemplate,
        draws: List<HistoricalDraw>,
    ): BacktestReport {
        require(draws.map(HistoricalDraw::issue).distinct().size == draws.size) {
            "Backtest draw issues must be unique"
        }
        val ordered = draws.sortedBy(HistoricalDraw::issue)
        val staticCandidates = if (candidateGenerator == null) {
            NumberPool.filter(template.conditions, template.playType).candidates
                .mapNotNull { normalizeOrNull(it, template.playType) }
                .distinct()
        } else {
            null
        }
        val results = ordered.mapIndexed { index, target ->
            val history = ordered.subList(0, index)
            if (history.size < template.observationWindow) {
                BacktestResult(
                    targetIssue = target.issue,
                    status = BacktestStatus.INSUFFICIENT_SAMPLE,
                    candidateCount = 0,
                    covered = null,
                    candidates = emptyList(),
                    amountYuan = 0,
                )
            } else {
                val normalizedCandidates = if (candidateGenerator == null) {
                    checkNotNull(staticCandidates)
                } else {
                    candidateGenerator(history, template, target)
                        .mapNotNull { normalizeOrNull(it, template.playType) }
                        .distinct()
                }
                val winningNumber = normalizeOrNull(target.number, template.playType)
                BacktestResult(
                    targetIssue = target.issue,
                    status = BacktestStatus.EVALUATED,
                    candidateCount = normalizedCandidates.size,
                    covered = winningNumber != null && winningNumber in normalizedCandidates,
                    candidates = normalizedCandidates,
                    amountYuan = PlayConverter.amountYuan(normalizedCandidates.size, 1),
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
            averageBetCount = evaluated.takeIf(List<BacktestResult>::isNotEmpty)
                ?.map(BacktestResult::candidateCount)
                ?.average(),
            cumulativeAmountYuan = evaluated.sumOf(BacktestResult::amountYuan),
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
