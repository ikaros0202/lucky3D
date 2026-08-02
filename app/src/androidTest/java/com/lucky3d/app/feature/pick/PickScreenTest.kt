package com.lucky3d.app.feature.pick

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import com.lucky3d.app.domain.attributes.DrawNumber
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
    fun manualPickIsTheCompactDefaultAndMoreMethodsAreProgressive() {
        val state = PickUiState(targetIssue = "2026199")

        composeRule.setContent {
            Lucky3DTheme {
                TestPickScreen(state)
            }
        }

        composeRule.onNodeWithText("普通单选").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_ball_0").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_ball_1").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_ball_2").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_digit_0").assertIsDisplayed()
        composeRule.onNodeWithTag("manual_digit_9").assertIsDisplayed()
        composeRule.onNodeWithText("仅用于方案核对").assertIsDisplayed()
        composeRule.onNodeWithText("从走势带入").assertDoesNotExist()
        composeRule.onNodeWithText("套用模板").assertDoesNotExist()

        composeRule.onNodeWithText("更多选号方式").performClick()
        composeRule.onNodeWithText("组选3").assertIsDisplayed()
        composeRule.onNodeWithText("组选6").assertIsDisplayed()
        composeRule.onNodeWithText("条件筛选").assertIsDisplayed()
        composeRule.onNodeWithText("胆码拖码").assertIsDisplayed()
    }

    @Test
    fun manualNumberKeepsLeadingZero() {
        val number = DrawNumber.parse("007")
        val state = PickUiState(
            targetIssue = "2026199",
            manualDigits = listOf(0, 0, 7),
            manualBets = listOf(number),
            candidates = listOf(number),
            candidatePermutations = listOf(listOf(number)),
            amountYuan = 2,
        )

        composeRule.setContent {
            Lucky3DTheme {
                TestPickScreen(state)
            }
        }

        composeRule.onNodeWithText("007").assertIsDisplayed()
        composeRule.onNodeWithText("1 注 · 1 倍 · ¥2").assertIsDisplayed()
    }

    @Test
    fun requiredDigitsSumAndSpanFlowShowsExactTotal() {
        val viewModel = PickViewModel().apply {
            setMode(PickMode.FILTER)
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
                    onSelectManualPosition = {},
                    onSelectManualDigit = {},
                    onRemoveManualBet = {},
                    onClearManual = {},
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

    @Composable
    private fun TestPickScreen(state: PickUiState) {
        PickScreen(
            state = state,
            onSetTargetIssue = {},
            onSetObservationWindow = {},
            onSetPlayType = {},
            onSetMode = {},
            onSelectManualPosition = {},
            onSelectManualDigit = {},
            onRemoveManualBet = {},
            onClearManual = {},
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
