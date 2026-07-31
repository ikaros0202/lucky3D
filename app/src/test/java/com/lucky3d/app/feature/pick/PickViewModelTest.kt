package com.lucky3d.app.feature.pick

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.GlobalExcludedDigits
import com.lucky3d.app.domain.filter.GlobalRequiredDigits
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.filter.SpanRange
import com.lucky3d.app.domain.filter.SumRange
import org.junit.Test

class PickViewModelTest {
    @Test
    fun `manual pick is the empty default`() {
        val state = PickViewModel().uiState.value

        assertThat(state.mode).isEqualTo(PickMode.MANUAL)
        assertThat(state.manualDigits).containsExactly(null, null, null).inOrder()
        assertThat(state.activeManualPosition).isEqualTo(0)
        assertThat(state.manualBets).isEmpty()
        assertThat(state.betCount).isEqualTo(0)
        assertThat(state.amountYuan).isEqualTo(0)
    }

    @Test
    fun `manual digits advance and automatically preserve 007`() {
        val viewModel = PickViewModel()

        viewModel.selectManualDigit(0)
        assertThat(viewModel.uiState.value.activeManualPosition).isEqualTo(1)
        viewModel.selectManualDigit(0)
        assertThat(viewModel.uiState.value.activeManualPosition).isEqualTo(2)
        viewModel.selectManualDigit(7)

        val state = viewModel.uiState.value
        assertThat(state.manualDigits).containsExactly(0, 0, 7).inOrder()
        assertThat(state.activeManualPosition).isEqualTo(0)
        assertThat(state.manualBets.map(DrawNumber::value)).containsExactly("007")
        assertThat(state.candidates.map(DrawNumber::value)).containsExactly("007")
        assertThat(state.candidatePermutations.single().map(DrawNumber::value))
            .containsExactly("007")

        viewModel.selectManualDigit(1)
        viewModel.selectManualDigit(2)
        viewModel.selectManualDigit(3)
        assertThat(viewModel.uiState.value.manualBets.map(DrawNumber::value))
            .containsExactly("007", "123")
    }

    @Test
    fun `manual bets deduplicate remove clear and recalculate amount`() {
        val viewModel = PickViewModel()
        viewModel.selectManualDigit(1)
        viewModel.selectManualDigit(2)
        viewModel.selectManualDigit(3)

        viewModel.addManualBet()
        assertThat(viewModel.uiState.value.manualBets.map(DrawNumber::value))
            .containsExactly("123")

        viewModel.setMultiplier(3)
        assertThat(viewModel.uiState.value.betCount).isEqualTo(1)
        assertThat(viewModel.uiState.value.amountYuan).isEqualTo(6)

        viewModel.removeManualBet(DrawNumber.parse("123"))
        assertThat(viewModel.uiState.value.betCount).isEqualTo(0)
        assertThat(viewModel.uiState.value.amountYuan).isEqualTo(0)

        viewModel.selectManualPosition(0)
        viewModel.selectManualDigit(0)
        viewModel.selectManualDigit(0)
        viewModel.selectManualDigit(7)
        viewModel.clearManual()
        assertThat(viewModel.uiState.value.manualDigits).containsExactly(null, null, null).inOrder()
        assertThat(viewModel.uiState.value.manualBets).isEmpty()
        assertThat(viewModel.uiState.value.activeManualPosition).isEqualTo(0)
    }

    @Test
    fun `conditions update candidates impacts and amount immediately`() {
        val viewModel = PickViewModel()
        viewModel.setMode(PickMode.FILTER)

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
        viewModel.setMode(PickMode.FILTER)
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
        viewModel.setMode(PickMode.FILTER)

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
