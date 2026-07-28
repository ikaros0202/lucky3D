package com.lucky3d.app.feature.scheme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucky3d.app.data.repository.SavedTemplate
import com.lucky3d.app.data.repository.SchemeRepository
import com.lucky3d.app.data.repository.SchemeWithReplay
import com.lucky3d.app.domain.backtest.BacktestReport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SchemeSection { SCHEMES, TEMPLATES }

data class SchemeUiState(
    val section: SchemeSection = SchemeSection.SCHEMES,
    val schemes: List<SchemeWithReplay> = emptyList(),
    val templates: List<SavedTemplate> = emptyList(),
    val selectedSchemeId: String? = null,
    val selectedTemplateId: String? = null,
    val backtestStartIssue: String = "2017001",
    val backtestEndIssue: String = "2026198",
    val backtest: BacktestReport? = null,
    val isBacktesting: Boolean = false,
    val operationSucceeded: Boolean = false,
    val operationFailed: Boolean = false,
)

@HiltViewModel
class SchemeViewModel @Inject constructor(
    private val repository: SchemeRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SchemeUiState())
    val uiState: StateFlow<SchemeUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.schemes.collect { schemes ->
                mutableState.update { state ->
                    state.copy(
                        schemes = schemes,
                        selectedSchemeId = state.selectedSchemeId
                            ?.takeIf { id -> schemes.any { it.scheme.id == id } },
                    )
                }
            }
        }
        viewModelScope.launch {
            repository.templates.collect { templates ->
                mutableState.update { state ->
                    state.copy(
                        templates = templates,
                        selectedTemplateId = state.selectedTemplateId
                            ?.takeIf { id -> templates.any { it.id == id } }
                            ?: templates.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    fun showSection(section: SchemeSection) {
        mutableState.update { it.copy(section = section) }
    }

    fun selectScheme(id: String?) {
        mutableState.update { it.copy(selectedSchemeId = id) }
    }

    fun selectTemplate(id: String?) {
        mutableState.update { it.copy(selectedTemplateId = id, backtest = null) }
    }

    fun setBacktestRange(startIssue: String, endIssue: String) {
        mutableState.update {
            it.copy(
                backtestStartIssue = startIssue.filter(Char::isDigit).take(7),
                backtestEndIssue = endIssue.filter(Char::isDigit).take(7),
                backtest = null,
            )
        }
    }

    fun runBacktest() {
        val state = mutableState.value
        val templateId = state.selectedTemplateId ?: return
        if (
            !state.backtestStartIssue.matches(ISSUE_REGEX) ||
            !state.backtestEndIssue.matches(ISSUE_REGEX) ||
            state.backtestStartIssue > state.backtestEndIssue
        ) {
            mutableState.update { it.copy(operationFailed = true) }
            return
        }
        viewModelScope.launch {
            mutableState.update {
                it.copy(
                    isBacktesting = true,
                    operationFailed = false,
                    operationSucceeded = false,
                )
            }
            runCatching {
                repository.runBacktest(
                    templateId = templateId,
                    startIssue = state.backtestStartIssue,
                    endIssue = state.backtestEndIssue,
                )
            }.onSuccess { report ->
                mutableState.update {
                    it.copy(
                        backtest = report,
                        isBacktesting = false,
                        operationSucceeded = true,
                    )
                }
            }.onFailure {
                mutableState.update {
                    it.copy(isBacktesting = false, operationFailed = true)
                }
            }
        }
    }

    fun copyScheme(schemeId: String, newIssue: String) {
        if (!newIssue.matches(ISSUE_REGEX)) {
            mutableState.update { it.copy(operationFailed = true) }
            return
        }
        viewModelScope.launch {
            runCatching { repository.copyScheme(schemeId, newIssue) }
                .onSuccess { copied ->
                    mutableState.update {
                        it.copy(
                            selectedSchemeId = copied.id,
                            operationSucceeded = true,
                            operationFailed = false,
                        )
                    }
                }
                .onFailure {
                    mutableState.update { it.copy(operationFailed = true) }
                }
        }
    }

    fun updateNote(schemeId: String, note: String) {
        viewModelScope.launch {
            runCatching { repository.updateNote(schemeId, note) }
                .onSuccess { saved ->
                    mutableState.update {
                        it.copy(
                            selectedSchemeId = saved.id,
                            operationSucceeded = true,
                            operationFailed = false,
                        )
                    }
                }
                .onFailure {
                    mutableState.update { it.copy(operationFailed = true) }
                }
        }
    }

    fun dismissOperationStatus() {
        mutableState.update { it.copy(operationSucceeded = false, operationFailed = false) }
    }

    private companion object {
        val ISSUE_REGEX = Regex("""\d{7}""")
    }
}
