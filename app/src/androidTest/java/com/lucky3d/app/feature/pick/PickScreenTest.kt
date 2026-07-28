package com.lucky3d.app.feature.pick

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.lucky3d.app.domain.filter.GlobalRequiredDigits
import com.lucky3d.app.domain.filter.SpanRange
import com.lucky3d.app.domain.filter.SumRange
import com.lucky3d.app.ui.theme.Lucky3DTheme
import org.junit.Rule
import org.junit.Test

class PickScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun requiredDigitsSumAndSpanFlowShowsExactTotal() {
        val viewModel = PickViewModel().apply {
            addCondition(GlobalRequiredDigits(setOf(1, 2)))
            addCondition(SumRange(6, 15))
            addCondition(SpanRange(1, 8))
            setMultiplier(2)
        }
        val state = viewModel.uiState.value

        composeRule.setContent {
            Lucky3DTheme {
                PickScreen(
                    state = state,
                    onSetTargetIssue = {},
                    onSetObservationWindow = {},
                    onSetPlayType = {},
                    onSetMode = {},
                    onAddCondition = {},
                    onEditCondition = { _, _ -> },
                    onSetConditionEnabled = { _, _ -> },
                    onRemoveCondition = {},
                    onSetDanDigits = {},
                    onSetTuoDigits = {},
                    onSetMultiplier = {},
                    onUndo = {},
                    onSaveTemplate = { _ -> },
                    onSaveScheme = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("全局胆码").assertIsDisplayed()
        composeRule.onNodeWithTag("pick_list").performScrollToNode(hasText("和值"))
        composeRule.onNodeWithText("和值").assertIsDisplayed()
        composeRule.onNodeWithTag("pick_list").performScrollToNode(hasText("跨度"))
        composeRule.onNodeWithText("跨度").assertIsDisplayed()
        val summary = "${state.betCount} 注 · ${state.multiplier} 倍 · ¥${state.amountYuan}"
        composeRule.onNodeWithTag("pick_list").performScrollToNode(hasText(summary))
        composeRule.onNodeWithText(summary).performScrollTo().assertIsDisplayed()
    }
}
