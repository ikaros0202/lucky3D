package com.lucky3d.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class V1MainFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fivePrimaryTabsRemainUsableWithCurrentRoomBackedData() {
        composeRule.onNodeWithTag("home_live_data_layer").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("首页").assertIsSelected()

        composeRule.onNodeWithContentDescription("走势分析")
            .performClick()
            .assertIsSelected()

        composeRule.onNodeWithContentDescription("号码筛选")
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText("普通单选").assertIsDisplayed()
        composeRule.onNodeWithText("0 注 · 1 倍 · ¥0").assertIsDisplayed()
        composeRule.onNodeWithText("仅用于方案核对").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("已保存方案")
            .performClick()
            .assertIsSelected()

        composeRule.onNodeWithContentDescription("彩报")
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithContentDescription("更新彩报").assertIsDisplayed()
        composeRule
            .onNodeWithText("当前为静态示意内容，不会自动更新")
            .assertDoesNotExist()
    }
}
