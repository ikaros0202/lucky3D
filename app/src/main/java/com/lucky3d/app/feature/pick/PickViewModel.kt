package com.lucky3d.app.feature.pick

import androidx.lifecycle.ViewModel
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.FilterCondition
import com.lucky3d.app.domain.filter.FilterConflict
import com.lucky3d.app.domain.filter.NumberPool
import com.lucky3d.app.domain.filter.PlayConverter
import com.lucky3d.app.domain.filter.PlayType
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PickMode { MANUAL, FILTER, DAN_TUO }

data class PickConditionItem(
    val id: String,
    val condition: FilterCondition,
    val enabled: Boolean = true,
    val excludedCount: Int = 0,
)

data class PickUiState(
    val targetIssue: String = "",
    val observationWindow: Int = 30,
    val playType: PlayType = PlayType.STRAIGHT,
    val mode: PickMode = PickMode.MANUAL,
    val manualDigits: List<Int?> = listOf(null, null, null),
    val activeManualPosition: Int = 0,
    val manualBets: List<DrawNumber> = emptyList(),
    val conditions: List<PickConditionItem> = emptyList(),
    val danDigits: Set<Int> = emptySet(),
    val tuoDigits: Set<Int> = emptySet(),
    val candidates: List<DrawNumber> = emptyList(),
    val candidatePermutations: List<List<DrawNumber>> = emptyList(),
    val multiplier: Int = 1,
    val amountYuan: Int = 0,
    val conflict: FilterConflict? = null,
    val canUndo: Boolean = false,
) {
    val betCount: Int get() = candidates.size
    val enabledConditionCount: Int get() = conditions.count(PickConditionItem::enabled)
}

@HiltViewModel
class PickViewModel @Inject constructor() : ViewModel() {
    private val mutableState = MutableStateFlow(PickUiState())
    val uiState: StateFlow<PickUiState> = mutableState.asStateFlow()

    private var undoState: PickUiState? = null

    fun setTargetIssue(issue: String) {
        mutableState.update { it.copy(targetIssue = issue.filter(Char::isDigit).take(7)) }
    }

    fun setObservationWindow(window: Int) {
        if (window <= 0) return
        mutableState.update { it.copy(observationWindow = window) }
    }

    fun setPlayType(playType: PlayType) = mutateAndRecalculate {
        copy(playType = playType)
    }

    fun setMode(mode: PickMode) = mutateAndRecalculate {
        copy(
            mode = mode,
            playType = if (mode == PickMode.MANUAL) PlayType.STRAIGHT else playType,
        )
    }

    fun selectManualPosition(index: Int) {
        if (index !in MANUAL_POSITIONS) return
        mutableState.update { current ->
            current.copy(activeManualPosition = index)
        }
    }

    fun selectManualDigit(digit: Int) {
        if (digit !in 0..9) return
        mutableState.update { current ->
            if (current.mode != PickMode.MANUAL) return@update current
            val isStartingNextNumber = current.activeManualPosition == MANUAL_POSITIONS.first &&
                current.manualDigits.toDrawNumberOrNull() != null
            val digits = if (isStartingNextNumber) {
                MutableList<Int?>(MANUAL_POSITIONS.count()) { null }
            } else {
                current.manualDigits.toMutableList()
            }.apply {
                this[current.activeManualPosition] = digit
            }
            val completedNumber = digits.toDrawNumberOrNull()
            recalculate(
                current.copy(
                    manualDigits = digits,
                    activeManualPosition = if (completedNumber == null) {
                        (current.activeManualPosition + 1).coerceAtMost(MANUAL_POSITIONS.last)
                    } else {
                        MANUAL_POSITIONS.first
                    },
                    manualBets = completedNumber?.let { number ->
                        (current.manualBets + number).distinct()
                    } ?: current.manualBets,
                ),
            )
        }
    }

    fun addManualBet() {
        mutableState.update { current ->
            val number = current.manualDigits.toDrawNumberOrNull() ?: return@update current
            recalculate(current.copy(manualBets = (current.manualBets + number).distinct()))
        }
    }

    fun removeManualBet(number: DrawNumber) {
        mutableState.update { current ->
            recalculate(current.copy(manualBets = current.manualBets - number))
        }
    }

    fun clearManual() {
        mutableState.update { current ->
            recalculate(
                current.copy(
                    manualDigits = listOf(null, null, null),
                    activeManualPosition = 0,
                    manualBets = emptyList(),
                ),
            )
        }
    }

    fun addCondition(condition: FilterCondition): String {
        val id = UUID.randomUUID().toString()
        mutateAndRecalculate {
            copy(conditions = conditions + PickConditionItem(id = id, condition = condition))
        }
        return id
    }

    fun editCondition(id: String, condition: FilterCondition) = mutateAndRecalculate {
        copy(
            conditions = conditions.map { item ->
                if (item.id == id) item.copy(condition = condition) else item
            },
        )
    }

    fun setConditionEnabled(id: String, enabled: Boolean) = mutateAndRecalculate {
        copy(
            conditions = conditions.map { item ->
                if (item.id == id) item.copy(enabled = enabled) else item
            },
        )
    }

    fun removeCondition(id: String) = mutateAndRecalculate {
        copy(conditions = conditions.filterNot { it.id == id })
    }

    fun setDanDigits(digits: Set<Int>) = mutateAndRecalculate {
        copy(danDigits = digits.filterTo(mutableSetOf()) { it in 0..9 })
    }

    fun setTuoDigits(digits: Set<Int>) = mutateAndRecalculate {
        copy(tuoDigits = digits.filterTo(mutableSetOf()) { it in 0..9 })
    }

    fun setMultiplier(multiplier: Int) {
        mutableState.update { current ->
            val normalized = multiplier.coerceIn(1, 99)
            current.copy(
                multiplier = normalized,
                amountYuan = PlayConverter.amountYuan(current.betCount, normalized),
            )
        }
    }

    fun undoLastChange() {
        val previous = undoState ?: return
        mutableState.value = previous.copy(canUndo = false)
        undoState = null
    }

    private fun mutateAndRecalculate(transform: PickUiState.() -> PickUiState) {
        val current = mutableState.value
        undoState = current.copy(canUndo = false)
        mutableState.value = recalculate(current.transform()).copy(canUndo = true)
    }

    private fun recalculate(state: PickUiState): PickUiState {
        val result = when (state.mode) {
            PickMode.MANUAL -> null
            PickMode.FILTER -> NumberPool.filter(
                conditions = state.conditions.filter(PickConditionItem::enabled)
                    .map(PickConditionItem::condition),
                playType = state.playType,
            )
            PickMode.DAN_TUO -> NumberPool.generateDanTuo(
                danDigits = state.danDigits,
                tuoDigits = state.tuoDigits,
                playType = state.playType,
            )
        }
        var impactIndex = 0
        val updatedConditions = state.conditions.map { item ->
            if (!item.enabled) {
                item.copy(excludedCount = 0)
            } else {
                item.copy(
                    excludedCount = result?.impacts?.getOrNull(impactIndex++)?.excludedCount ?: 0,
                )
            }
        }
        val candidates = result?.candidates ?: state.manualBets
        return state.copy(
            conditions = updatedConditions,
            candidates = candidates,
            candidatePermutations = candidates.map { candidate ->
                if (state.mode == PickMode.MANUAL) {
                    listOf(candidate)
                } else {
                    PlayConverter.toStraightPermutations(candidate, state.playType)
                }
            },
            amountYuan = PlayConverter.amountYuan(candidates.size, state.multiplier),
            conflict = result?.conflict,
        )
    }

    private fun List<Int?>.toDrawNumberOrNull(): DrawNumber? {
        if (size != MANUAL_POSITIONS.count() || any { it == null }) return null
        return DrawNumber.of(
            hundreds = get(0) ?: return null,
            tens = get(1) ?: return null,
            ones = get(2) ?: return null,
        )
    }

    private companion object {
        val MANUAL_POSITIONS = 0..2
    }
}
