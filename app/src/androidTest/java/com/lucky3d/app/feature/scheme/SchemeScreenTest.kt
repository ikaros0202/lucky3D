package com.lucky3d.app.feature.scheme

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lucky3d.app.ui.theme.Lucky3DTheme
import org.junit.Rule
import org.junit.Test

class SchemeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptySchemesProvideAUsefulNextStep() {
        composeRule.setContent {
            Lucky3DTheme {
                SchemeScreen(
                    state = SchemeUiState(),
                    onShowSection = {},
                    onSelectScheme = {},
                    onSelectTemplate = {},
                    onSetBacktestRange = { _, _ -> },
                    onRunBacktest = {},
                    onCopyScheme = { _, _ -> },
                    onUpdateNote = { _, _ -> },
                    onDismissStatus = {},
                    onStartPick = {},
                )
            }
        }

        composeRule.onNodeWithText("还没有保存方案").assertIsDisplayed()
        composeRule.onNodeWithText("开始选号").assertIsDisplayed()
    }
}
