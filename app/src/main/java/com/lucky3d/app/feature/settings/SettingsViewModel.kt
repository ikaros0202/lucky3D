package com.lucky3d.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.domain.filter.Position
import com.lucky3d.app.domain.replay.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<UserPreferences> = repository.preferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferences(),
    )

    fun setDefaultObservationWindow(window: Int) {
        if (window !in 1..3334) return
        viewModelScope.launch { repository.setDefaultObservationWindow(window) }
    }

    fun setReminderEnabled(type: ReminderType, enabled: Boolean) {
        viewModelScope.launch { repository.setReminderEnabled(type, enabled) }
    }

    fun setOmissionRule(position: Position, digit: Int, threshold: Int) {
        if (digit !in 0..9 || threshold !in 1..999) return
        viewModelScope.launch { repository.setOmissionRule(position, digit, threshold) }
    }
}
