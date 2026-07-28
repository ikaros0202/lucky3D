package com.lucky3d.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.R
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.SchemeRepository
import com.lucky3d.app.domain.omission.OmissionCalculator
import com.lucky3d.app.domain.replay.InAppReminderCoordinator
import com.lucky3d.app.domain.replay.ReminderEvent
import com.lucky3d.app.domain.replay.ReminderPreferences
import com.lucky3d.app.domain.replay.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReminderUiState(
    val pending: List<ReminderEvent> = emptyList(),
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    drawRepository: DrawRepository,
    schemeRepository: SchemeRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                drawRepository.latestDraw,
                drawRepository.allDrawsAscending,
                drawRepository.syncMetadata,
                schemeRepository.schemes,
                preferencesRepository.preferences,
            ) { latest, draws, metadata, schemes, preferences ->
                val events = buildList {
                    if (latest != null && metadata?.lastSuccessEpochMillis != null) {
                        add(
                            ReminderEvent(
                                issue = latest.issue,
                                type = ReminderType.DRAW,
                                subject = "latest",
                                message = context.getString(
                                    R.string.reminder_draw_message,
                                    latest.issue,
                                ),
                            ),
                        )
                    }
                    schemes.mapNotNull { it.replay?.let { replay -> it.scheme to replay } }
                        .forEach { (scheme, replay) ->
                            add(
                                ReminderEvent(
                                    issue = replay.issue,
                                    type = ReminderType.REPLAY,
                                    subject = scheme.id,
                                    message = context.getString(
                                        R.string.reminder_replay_message,
                                        replay.issue,
                                        scheme.title,
                                    ),
                                ),
                            )
                        }
                    if (latest != null && draws.isNotEmpty()) {
                        val position = preferences.omissionPosition
                        val values: List<Int> = draws.map { record ->
                            when (position) {
                                com.lucky3d.app.domain.filter.Position.HUNDREDS ->
                                    record.number.hundreds
                                com.lucky3d.app.domain.filter.Position.TENS ->
                                    record.number.tens
                                com.lucky3d.app.domain.filter.Position.ONES ->
                                    record.number.ones
                            }
                        }
                        val current = OmissionCalculator.calculate(
                            values,
                            preferences.omissionDigit,
                        ).currentOmission
                        if (current >= preferences.omissionThreshold) {
                            add(
                                ReminderEvent(
                                    issue = latest.issue,
                                    type = ReminderType.OMISSION,
                                    subject = "${position.name}:${preferences.omissionDigit}:" +
                                        preferences.omissionThreshold,
                                    message = context.getString(
                                        R.string.reminder_omission_message,
                                        latest.issue,
                                        context.getString(preferences.omissionPosition.labelRes()),
                                        preferences.omissionDigit,
                                        current,
                                        preferences.omissionThreshold,
                                    ),
                                ),
                            )
                        }
                    }
                }
                val decision = InAppReminderCoordinator.evaluate(
                    events = events,
                    preferences = ReminderPreferences(
                        drawEnabled = preferences.drawReminderEnabled,
                        replayEnabled = preferences.replayReminderEnabled,
                        omissionEnabled = preferences.omissionReminderEnabled,
                    ),
                    deliveredKeys = preferences.deliveredReminderKeys,
                )
                decision.toShow
            }.collect { newEvents ->
                if (newEvents.isNotEmpty()) {
                    mutableState.update { current ->
                        current.copy(
                            pending = (current.pending + newEvents)
                                .distinctBy(ReminderEvent::key),
                        )
                    }
                    preferencesRepository.markDelivered(newEvents.map(ReminderEvent::key).toSet())
                }
            }
        }
    }

    fun dismissCurrent() {
        mutableState.update { it.copy(pending = it.pending.drop(1)) }
    }
}

private fun com.lucky3d.app.domain.filter.Position.labelRes(): Int = when (this) {
    com.lucky3d.app.domain.filter.Position.HUNDREDS -> R.string.trend_position_hundreds
    com.lucky3d.app.domain.filter.Position.TENS -> R.string.trend_position_tens
    com.lucky3d.app.domain.filter.Position.ONES -> R.string.trend_position_ones
}
