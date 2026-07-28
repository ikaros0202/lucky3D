package com.lucky3d.app.feature.pick

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.filter.GlobalExcludedDigits
import com.lucky3d.app.domain.filter.GlobalRequiredDigits
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.filter.SpanRange
import com.lucky3d.app.domain.filter.SumRange
import org.junit.Test

class PickViewModelTest {
    @Test
    fun `conditions update candidates impacts and amount immediately`() {
        val viewModel = PickViewModel()

        viewModel.addCondition(GlobalRequiredDigits(setOf(1, 2)))
        viewModel.addCondition(SumRange(6, 15))
        viewModel.addCondition(SpanRange(1, 8))
        viewModel.setMultiplier(3)

        val state = viewModel.uiState.value
        assertThat(state.conditions).hasSize(3)
        assertThat(state.conditions.all { it.excludedCount >= 0 }).isTrue()
        assertThat(state.betCount).isEqualTo(state.candidates.size)
        assertThat(state.amountYuan).isEqualTo(state.betCount * 2 * 3)
        assertThat(state.candidates.all { it.value.length == 3 }).isTrue()
    }

    @Test
    fun `condition lifecycle supports disable edit delete and undo conflict`() {
        val viewModel = PickViewModel()
        viewModel.addCondition(GlobalRequiredDigits(setOf(1)))
        val excludedId = viewModel.addCondition(GlobalExcludedDigits(setOf(1)))

        assertThat(viewModel.uiState.value.conflict?.code)
            .isEqualTo("REQUIRED_EXCLUDED_OVERLAP")

        viewModel.undoLastChange()
        assertThat(viewModel.uiState.value.conflict).isNull()
        assertThat(viewModel.uiState.value.conditions).hasSize(1)

        val requiredId = viewModel.uiState.value.conditions.single().id
        viewModel.editCondition(requiredId, GlobalRequiredDigits(setOf(2)))
        assertThat(viewModel.uiState.value.conditions.single().condition)
            .isEqualTo(GlobalRequiredDigits(setOf(2)))

        viewModel.setConditionEnabled(requiredId, false)
        assertThat(viewModel.uiState.value.betCount).isEqualTo(1000)
        viewModel.removeCondition(requiredId)
        assertThat(viewModel.uiState.value.conditions).isEmpty()
        assertThat(excludedId).isNotEmpty()
    }

    @Test
    fun `play conversion and multiplier boundaries are exact`() {
        val viewModel = PickViewModel()

        viewModel.setPlayType(PlayType.GROUP6)
        assertThat(viewModel.uiState.value.betCount).isEqualTo(120)
        assertThat(viewModel.uiState.value.candidatePermutations.first()).hasSize(6)

        viewModel.setMultiplier(99)
        assertThat(viewModel.uiState.value.amountYuan).isEqualTo(120 * 2 * 99)
        viewModel.setMultiplier(100)
        assertThat(viewModel.uiState.value.multiplier).isEqualTo(99)
        viewModel.setMultiplier(0)
        assertThat(viewModel.uiState.value.multiplier).isEqualTo(1)
    }

    @Test
    fun `dan tuo keeps groups canonical and explains overlap`() {
        val viewModel = PickViewModel()
        viewModel.setPlayType(PlayType.GROUP6)
        viewModel.setMode(PickMode.DAN_TUO)
        viewModel.setDanDigits(setOf(0))
        viewModel.setTuoDigits(setOf(1, 2, 3))

        assertThat(viewModel.uiState.value.candidates.map { it.value })
            .containsExactly("012", "013", "023")

        viewModel.setTuoDigits(setOf(0, 1, 2))
        assertThat(viewModel.uiState.value.conflict?.code).isEqualTo("DAN_TUO_OVERLAP")
    }
}
