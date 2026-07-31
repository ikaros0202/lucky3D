package com.lucky3d.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

class V1MainFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun fivePrimaryTabsRemainUsableWithCurrentRoomBackedData() {
        composeRule.onNodeWithContentDescription("柔光晶体开奖台").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("走势分析")
            .performClick()
            .assertIsSelected()

        composeRule.onNodeWithContentDescription("号码筛选")
            .performClick()
            .assertIsSelected()
        composeRule.onNodeWithText("1000 注 · 1 倍 · ¥2000")
            .performScrollTo()
            .assertIsDisplayed()

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
