package com.lucky3d.app.feature.pick

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.data.repository.DrawRepository
import com.lucky3d.app.data.repository.SaveSchemeRequest
import com.lucky3d.app.data.repository.SaveTemplateRequest
import com.lucky3d.app.data.repository.SchemeRepository
import com.lucky3d.app.domain.filter.DanTuoCondition
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PickSaveStatus { IDLE, SAVING, SAVED, FAILED }

data class PickPersistenceState(
    val suggestedIssue: String = "",
    val saveStatus: PickSaveStatus = PickSaveStatus.IDLE,
)

@HiltViewModel
class PickPersistenceViewModel @Inject constructor(
    private val schemeRepository: SchemeRepository,
    drawRepository: DrawRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PickPersistenceState())
    val uiState: StateFlow<PickPersistenceState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            drawRepository.latestDraw.collect { draw ->
                mutableState.update {
                    it.copy(suggestedIssue = draw?.issue?.nextIssue().orEmpty())
                }
            }
        }
    }

    fun saveTemplate(name: String, pick: PickUiState) {
        if (name.isBlank() || pick.mode != PickMode.FILTER || pick.conditions.isEmpty()) {
            mutableState.update { it.copy(saveStatus = PickSaveStatus.FAILED) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(saveStatus = PickSaveStatus.SAVING) }
            runCatching {
                schemeRepository.saveTemplate(
                    SaveTemplateRequest(
                        name = name,
                        playType = pick.playType,
                        conditions = pick.conditions
                            .filter(PickConditionItem::enabled)
                            .map(PickConditionItem::condition),
                        observationWindow = pick.observationWindow,
                    ),
                )
            }.onSuccess {
                mutableState.update { it.copy(saveStatus = PickSaveStatus.SAVED) }
            }.onFailure {
                mutableState.update { it.copy(saveStatus = PickSaveStatus.FAILED) }
            }
        }
    }

    fun saveScheme(title: String, note: String, pick: PickUiState) {
        if (
            title.isBlank() ||
            !pick.targetIssue.matches(ISSUE_REGEX) ||
            pick.candidates.isEmpty()
        ) {
            mutableState.update { it.copy(saveStatus = PickSaveStatus.FAILED) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(saveStatus = PickSaveStatus.SAVING) }
            val conditions = when (pick.mode) {
                PickMode.MANUAL -> emptyList()
                PickMode.FILTER -> pick.conditions
                    .filter(PickConditionItem::enabled)
                    .map(PickConditionItem::condition)
                PickMode.DAN_TUO -> listOf(
                    DanTuoCondition(
                        danDigits = pick.danDigits,
                        tuoDigits = pick.tuoDigits,
                        playType = pick.playType,
                    ),
                )
            }
            runCatching {
                schemeRepository.saveScheme(
                    SaveSchemeRequest(
                        issue = pick.targetIssue,
                        title = title,
                        observationWindow = pick.observationWindow,
                        playType = pick.playType,
                        conditions = conditions,
                        candidates = pick.candidates,
                        multiplier = pick.multiplier,
                        note = note,
                    ),
                )
            }.onSuccess {
                mutableState.update { it.copy(saveStatus = PickSaveStatus.SAVED) }
            }.onFailure {
                mutableState.update { it.copy(saveStatus = PickSaveStatus.FAILED) }
            }
        }
    }

    fun dismissSaveStatus() {
        mutableState.update { it.copy(saveStatus = PickSaveStatus.IDLE) }
    }

    private companion object {
        val ISSUE_REGEX = Regex("""\d{7}""")
    }
}

private fun String.nextIssue(): String? = toIntOrNull()
    ?.plus(1)
    ?.toString()
    ?.padStart(7, '0')
