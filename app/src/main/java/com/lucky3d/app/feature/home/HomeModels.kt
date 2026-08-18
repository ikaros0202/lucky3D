package com.lucky3d.app.feature.home

import androidx.compose.runtime.Immutable
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import java.time.LocalDate
import java.time.ZoneId

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
    val insights: HomeInsights = HomeInsights.Empty,
    val syncState: HomeSyncState = HomeSyncState.LOCAL,
    val lastSuccessEpochMillis: Long? = null,
    val failureType: String? = null,
    val trialNumber: TrialNumber? = null,
    val trialState: LiveContentRefreshState = LiveContentRefreshState.Idle,
    val isBeforeTrialReleaseWindow: Boolean = false,
    val trialManualRefreshFailed: Boolean = false,
    val yunnanAnnouncement: YunnanAnnouncement? = null,
    val announcementRefreshFailed: Boolean = false,
)

enum class HistoryInputError {
    INVALID_ISSUE,
    INVALID_YEAR,
    INVALID_DATE_RANGE,
}

enum class HistoryAnnouncementState {
    IDLE,
    AVAILABLE,
    UNAVAILABLE,
}

internal fun historyToday(): LocalDate = LocalDate.now(ZoneId.of("Asia/Shanghai"))

@Immutable
data class HistoryUiState(
    val query: DrawQuery = DrawQuery.Recent(1),
    val availableIssues: List<DrawRecord> = emptyList(),
    val records: List<DrawRecord> = emptyList(),
    val selectedDraw: DrawRecord? = null,
    val inputError: HistoryInputError? = null,
    val yunnanAnnouncement: YunnanAnnouncement? = null,
    val announcementState: HistoryAnnouncementState = HistoryAnnouncementState.IDLE,
)
