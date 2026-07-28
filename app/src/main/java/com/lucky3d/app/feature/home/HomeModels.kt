package com.lucky3d.app.feature.home

import androidx.compose.runtime.Immutable
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.repository.DrawQuery

enum class HomeSyncState {
    LOCAL,
    UP_TO_DATE,
    UPDATING,
    ERROR,
    CORRECTED,
}

@Immutable
data class HomeUiState(
    val latest: DrawRecord? = null,
    val recent: List<DrawRecord> = emptyList(),
    val syncState: HomeSyncState = HomeSyncState.LOCAL,
    val lastSuccessEpochMillis: Long? = null,
    val failureType: String? = null,
)

enum class HistoryInputError {
    INVALID_ISSUE,
    INVALID_YEAR,
    INVALID_DATE_RANGE,
}

@Immutable
data class HistoryUiState(
    val query: DrawQuery = DrawQuery.Recent(30),
    val records: List<DrawRecord> = emptyList(),
    val selectedDraw: DrawRecord? = null,
    val inputError: HistoryInputError? = null,
)
