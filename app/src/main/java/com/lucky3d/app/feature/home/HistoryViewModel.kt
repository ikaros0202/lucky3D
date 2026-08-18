package com.lucky3d.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.remote.YunnanAnnouncementDataResult
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.OfficialDataSyncCoordinator
import com.lucky3d.app.data.repository.YunnanAnnouncementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: DrawRepository,
    private val yunnanRepository: YunnanAnnouncementRepository,
    private val officialDataSyncCoordinator: OfficialDataSyncCoordinator,
) : ViewModel() {
    constructor(repository: DrawRepository) : this(
        repository,
        EmptyYunnanAnnouncementRepository,
        OfficialDataSyncCoordinator(repository, EmptyYunnanAnnouncementRepository),
    )

    internal constructor(
        repository: DrawRepository,
        yunnanRepository: YunnanAnnouncementRepository,
    ) : this(
        repository,
        yunnanRepository,
        OfficialDataSyncCoordinator(repository, yunnanRepository),
    )

    private val query = MutableStateFlow<DrawQuery>(DrawQuery.Recent(1))
    private val selectedDraw = MutableStateFlow<DrawRecord?>(null)
    private val inputError = MutableStateFlow<HistoryInputError?>(null)
    private val announcementState =
        MutableStateFlow(HistoryAnnouncementState.IDLE)

    private val records = query.flatMapLatest(repository::observe)
    private val availableIssues = repository.allDrawsAscending
    private val yunnanAnnouncement = query.flatMapLatest { activeQuery ->
        when (activeQuery) {
            is DrawQuery.Issue -> yunnanRepository.observeByIssue(activeQuery.issue)
            else -> flowOf(null)
        }
    }

    private val baseUiState = combine(
        query,
        records,
        selectedDraw,
        inputError,
        availableIssues,
    ) { activeQuery, draws, selection, error, allDraws ->
        HistoryUiState(
            query = activeQuery,
            availableIssues = allDraws.asReversed(),
            records = draws,
            selectedDraw = selection,
            inputError = error,
        )
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        combine(baseUiState, yunnanAnnouncement) { state, announcement ->
            state.copy(
                yunnanAnnouncement = announcement,
                announcementState = if (announcement != null) {
                    HistoryAnnouncementState.AVAILABLE
                } else {
                    HistoryAnnouncementState.IDLE
                },
            )
        },
        announcementState,
    ) { state, fetchState ->
        state.copy(
            yunnanAnnouncement = if (fetchState == HistoryAnnouncementState.UNAVAILABLE) {
                null
            } else {
                state.yunnanAnnouncement
            },
            announcementState = if (fetchState == HistoryAnnouncementState.UNAVAILABLE) {
                fetchState
            } else if (state.yunnanAnnouncement != null) {
                HistoryAnnouncementState.AVAILABLE
            } else {
                fetchState
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HistoryUiState(),
    )

    fun showRecent(limit: Int) {
        if (limit !in setOf(10, 30, 50, 100) && limit !in 1..3334) {
            inputError.value = HistoryInputError.INVALID_DATE_RANGE
            return
        }
        inputError.value = null
        query.value = DrawQuery.Recent(limit)
    }

    fun searchIssue(value: String) {
        val normalized = value.trim()
        if (!normalized.matches(Regex("""\d{7}"""))) {
            inputError.value = HistoryInputError.INVALID_ISSUE
            return
        }
        inputError.value = null
        query.value = DrawQuery.Issue(normalized)
        announcementState.value = HistoryAnnouncementState.IDLE
        viewModelScope.launch {
            val resultState = when (
                val result = officialDataSyncCoordinator.syncIssue(normalized).announcementResult
            ) {
                is YunnanAnnouncementDataResult.Success -> if (result.announcements.isEmpty()) {
                    HistoryAnnouncementState.UNAVAILABLE
                } else {
                    HistoryAnnouncementState.AVAILABLE
                }
                YunnanAnnouncementDataResult.EmptyResponse -> HistoryAnnouncementState.UNAVAILABLE
                is YunnanAnnouncementDataResult.HttpFailure,
                is YunnanAnnouncementDataResult.InvalidPayload,
                is YunnanAnnouncementDataResult.NetworkFailure,
                -> HistoryAnnouncementState.UNAVAILABLE
            }
            if (query.value == DrawQuery.Issue(normalized)) {
                announcementState.value = resultState
            }
        }
    }

    fun searchYear(value: String) {
        val normalized = value.trim()
        if (!normalized.matches(Regex("""\d{4}"""))) {
            inputError.value = HistoryInputError.INVALID_YEAR
            return
        }
        inputError.value = null
        query.value = DrawQuery.Year(normalized)
    }

    fun searchDateRange(startDate: String, endDate: String) {
        val normalizedStart = startDate.trim()
        val normalizedEnd = endDate.trim()
        val range = runCatching { DrawQuery.DateRange(normalizedStart, normalizedEnd) }.getOrNull()
        if (range == null || !isSupportedDateRange(normalizedStart, normalizedEnd)) {
            inputError.value = HistoryInputError.INVALID_DATE_RANGE
            return
        }
        inputError.value = null
        query.value = range
    }

    /** Runs the date-first query as a single-day range. */
    fun searchDate(date: String) {
        searchDateRange(date, date)
    }

    fun selectDraw(draw: DrawRecord?) {
        selectedDraw.value = draw
    }

    private companion object {
        private val HistoryZone = ZoneId.of("Asia/Shanghai")
        private val HistoryMinimumDate = LocalDate.of(2017, 1, 1)

        fun isSupportedDateRange(startDate: String, endDate: String): Boolean {
            val today = LocalDate.now(HistoryZone)
            val start = runCatching { LocalDate.parse(startDate) }.getOrNull() ?: return false
            val end = runCatching { LocalDate.parse(endDate) }.getOrNull() ?: return false
            return !start.isBefore(HistoryMinimumDate) &&
                !end.isAfter(today) &&
                !start.isAfter(end)
        }
    }
}

private object EmptyYunnanAnnouncementRepository : YunnanAnnouncementRepository {
    override val latestAnnouncement: Flow<com.lucky3d.app.core.model.YunnanAnnouncement?> =
        flowOf(null)

    override fun observeByIssue(issue: String): Flow<com.lucky3d.app.core.model.YunnanAnnouncement?> =
        flowOf(null)

    override suspend fun refreshRecent(limit: Int): YunnanAnnouncementDataResult =
        YunnanAnnouncementDataResult.EmptyResponse

    override suspend fun refreshIssue(issue: String): YunnanAnnouncementDataResult =
        YunnanAnnouncementDataResult.EmptyResponse
}
