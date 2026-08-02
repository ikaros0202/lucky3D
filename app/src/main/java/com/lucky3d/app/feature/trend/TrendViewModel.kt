package com.lucky3d.app.feature.trend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.LiveContentRepository
import com.lucky3d.app.data.repository.CaibaoImageReadResult
import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.domain.livecontent.LiveContentRefreshResult
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.domain.livecontent.LiveRefreshTrigger
import kotlinx.coroutines.flow.flowOf
import com.lucky3d.app.domain.omission.DigitPosition
import com.lucky3d.app.domain.omission.OmissionCalculator
import com.lucky3d.app.domain.omission.TrendStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TrendViewModel @Inject constructor(
    private val repository: DrawRepository,
    private val liveContentRepository: LiveContentRepository,
) : ViewModel() {
    constructor(repository: DrawRepository) : this(repository, EmptyLiveContentRepository)
    private val window = MutableStateFlow(30)
    private val visiblePositions = MutableStateFlow(TrendPosition.entries.toSet())
    private val selectedPoint = MutableStateFlow<TrendPoint?>(null)
    private val statisticsPosition = MutableStateFlow(TrendPosition.HUNDREDS)
    private val scale = MutableStateFlow(1f)

    val uiState: StateFlow<TrendUiState> = combine(
        combine(repository.allDrawsAscending, liveContentRepository.trialNumbers, ::Pair),
        window,
        visiblePositions,
        selectedPoint,
        combine(statisticsPosition, scale, ::Pair),
    ) { drawAndTrials, activeWindow, positions, selection, display ->
        val draws = drawAndTrials.first
        val trials = drawAndTrials.second
        buildTrendState(
            allDraws = draws,
            window = activeWindow,
            visiblePositions = positions,
            selectedPoint = selection,
            statisticsPosition = display.first,
            scale = display.second,
            trialNumbers = trials,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TrendUiState(),
    )

    init {
        viewModelScope.launch {
            val requiredIssues = repository.allDrawsAscending
                .first()
                .takeLast(window.value)
                .map { it.issue }
                .toSet()
            liveContentRepository.refreshTrialHistory(
                LiveRefreshTrigger.AUTO_FOREGROUND,
                requiredWindow = window.value,
                requiredIssues = requiredIssues,
            )
        }
    }

    fun setWindow(value: Int) {
        require(value in 1..3334) { "Trend window must be between 1 and 3334" }
        window.value = value
        selectedPoint.value = null
        viewModelScope.launch {
            val requiredIssues = repository.allDrawsAscending
                .first()
                .takeLast(value)
                .map { it.issue }
                .toSet()
            liveContentRepository.refreshTrialHistory(
                LiveRefreshTrigger.AUTO_FOREGROUND,
                requiredWindow = value,
                requiredIssues = requiredIssues,
            )
        }
    }

    fun togglePosition(position: TrendPosition) {
        val current = visiblePositions.value
        if (position in current && current.size == 1) return
        visiblePositions.value = if (position in current) current - position else current + position
        if (statisticsPosition.value !in visiblePositions.value) {
            statisticsPosition.value = visiblePositions.value.minBy(TrendPosition::ordinal)
        }
    }

    fun selectPoint(point: TrendPoint?) {
        selectedPoint.value = point
    }

    fun selectLatest() {
        selectedPoint.value = uiState.value.points
            .filter { it.rowIndex == uiState.value.visibleDraws.lastIndex }
            .minByOrNull { it.position.ordinal }
    }

    fun showStatistics(position: TrendPosition) {
        statisticsPosition.value = position
    }

    fun setScale(value: Float) {
        scale.value = value.coerceIn(0.1f, 2.5f)
    }
}

internal fun buildTrendState(
    allDraws: List<DrawRecord>,
    window: Int,
    visiblePositions: Set<TrendPosition>,
    selectedPoint: TrendPoint? = null,
    statisticsPosition: TrendPosition = TrendPosition.HUNDREDS,
    scale: Float = 1f,
    trialNumbers: List<TrialNumber> = emptyList(),
): TrendUiState {
    val visible = allDraws.takeLast(window)
    val positionOrder = TrendPosition.entries.filter(visiblePositions::contains)
    val omissionsByPosition = TrendPosition.entries.associateWith { position ->
        val fullValues = allDraws.map { it.digitAt(position) }
        (0..9).associateWith { digit ->
            OmissionCalculator.calculateVisibleWindow(
                values = fullValues,
                target = digit,
                visibleWindowSize = visible.size,
            ).omissionByDraw
        }
    }
    val points = positionOrder.flatMap { position ->
        visible.mapIndexed { rowIndex, draw ->
            val digit = draw.digitAt(position)
            TrendPoint(
                issue = draw.issue,
                rowIndex = rowIndex,
                position = position,
                digit = digit,
                omission = omissionsByPosition.getValue(position).getValue(digit)[rowIndex],
            )
        }
    }
    val tableRows = visible.mapIndexed { rowIndex, draw ->
        val attributes = com.lucky3d.app.domain.attributes.DrawAttributes.calculate(draw.number)
        val trial = trialNumbers.firstOrNull { it.issue == draw.issue }
        TrendTableRow(
            issue = draw.issue,
            drawNumber = draw.number.value,
            omissions = TrendPosition.entries.flatMap { position ->
                (0..9).map { digit ->
                    omissionsByPosition.getValue(position).getValue(digit)[rowIndex]
                }
            },
            trialNumber = trial?.number,
            sum = attributes.sum.toString(),
            sumTail = attributes.sumTail.toString(),
            span = attributes.span.toString(),
            oddEvenRatio = "${attributes.oddCount}:${attributes.evenCount}",
            bigSmallRatio = "${attributes.bigCount}:${attributes.smallCount}",
            routeRatio = attributes.routeCountPattern,
        )
    }
    val numbers = allDraws.map(DrawRecord::number)
    val statistics = positionOrder.map { position ->
        val activity = TrendStatistics.calculate(numbers, position.toDigitPosition(), window)
        val fullValues = allDraws.map { it.digitAt(position) }
        TrendPositionStatistics(
            position = position,
            requestedWindowSize = window,
            actualWindowSize = activity.actualWindowSize,
            sampleComplete = activity.sampleComplete,
            digits = activity.activities.map { digitActivity ->
                val omission = OmissionCalculator.calculate(fullValues, digitActivity.digit)
                TrendDigitStatistics(
                    digit = digitActivity.digit,
                    currentOmission = omission.currentOmission,
                    averageOmission = omission.averageOmission,
                    maxOmission = omission.maxOmission,
                    occurrences = digitActivity.occurrences,
                    heatLevel = digitActivity.level,
                )
            },
        )
    }
    return TrendUiState(
        window = window,
        visibleDraws = visible,
        visiblePositions = visiblePositions,
        tableRows = tableRows,
        points = points,
        statistics = statistics,
        selectedPoint = selectedPoint,
        statisticsPosition = statisticsPosition,
        scale = scale,
        trialNumbers = trialNumbers,
    )
}

private object EmptyLiveContentRepository : LiveContentRepository {
    override val trialNumber = flowOf<TrialNumber?>(null)
    override val trialRefreshState = flowOf<LiveContentRefreshState>(LiveContentRefreshState.Idle)
    override val caibaoDocument = flowOf<CaibaoDocument?>(null)
    override val caibaoRefreshState = flowOf<LiveContentRefreshState>(LiveContentRefreshState.Idle)
    override suspend fun refreshTrial(trigger: LiveRefreshTrigger) =
        LiveContentRefreshResult.Skipped(com.lucky3d.app.domain.livecontent.SkipReason.TRIGGER_NOT_APPLICABLE)
    override suspend fun refreshCaibao(trigger: LiveRefreshTrigger) =
        LiveContentRefreshResult.Skipped(com.lucky3d.app.domain.livecontent.SkipReason.TRIGGER_NOT_APPLICABLE)
    override suspend fun readCaibaoImage(document: CaibaoDocument) = CaibaoImageReadResult.Unavailable(com.lucky3d.app.domain.livecontent.LiveContentFailure.FILE_IO)
    override suspend fun invalidateCaibaoImage(document: CaibaoDocument) = Unit
    override suspend fun cleanCaibaoCache() = Unit
}

private fun DrawRecord.digitAt(position: TrendPosition): Int = when (position) {
    TrendPosition.HUNDREDS -> number.hundreds
    TrendPosition.TENS -> number.tens
    TrendPosition.ONES -> number.ones
}

private fun TrendPosition.toDigitPosition(): DigitPosition = when (this) {
    TrendPosition.HUNDREDS -> DigitPosition.HUNDREDS
    TrendPosition.TENS -> DigitPosition.TENS
    TrendPosition.ONES -> DigitPosition.ONES
}
