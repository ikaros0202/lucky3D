package com.lucky3d.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.data.repository.DrawRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: DrawRepository,
) : ViewModel() {
    private val query = MutableStateFlow<DrawQuery>(DrawQuery.Recent(30))
    private val selectedDraw = MutableStateFlow<DrawRecord?>(null)
    private val inputError = MutableStateFlow<HistoryInputError?>(null)

    private val records = query.flatMapLatest(repository::observe)

    val uiState: StateFlow<HistoryUiState> = combine(
        query,
        records,
        selectedDraw,
        inputError,
    ) { activeQuery, draws, selection, error ->
        HistoryUiState(
            query = activeQuery,
            records = draws,
            selectedDraw = selection,
            inputError = error,
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
        if (range == null) {
            inputError.value = HistoryInputError.INVALID_DATE_RANGE
            return
        }
        inputError.value = null
        query.value = range
    }

    fun selectDraw(draw: DrawRecord?) {
        selectedDraw.value = draw
    }
}
