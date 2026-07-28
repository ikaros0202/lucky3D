package com.lucky3d.app.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lucky3d.app.MainActivity
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun caibaoTabShowsBundledReadOnlyContent() {
        composeRule
            .onNodeWithContentDescription("彩报静态预览")
            .performClick()
            .assertIsSelected()
        composeRule
            .onNodeWithText("当前为静态示意内容，不会自动更新")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("彩报静态占位图：桌面上的报纸")
            .assertIsDisplayed()
        composeRule.onNodeWithText("刷新").assertDoesNotExist()
    }

    @Test
    fun tabSwitchingRetainsPickPlayType() {
        composeRule
            .onNodeWithContentDescription("号码筛选")
            .performClick()
        composeRule.onNodeWithText("组选6").performClick().assertIsSelected()

        composeRule.onNodeWithContentDescription("首页").performClick()
        composeRule.onNodeWithText("Lucky3D").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("号码筛选").performClick()

        composeRule.onNodeWithText("组选6").assertIsSelected()
    }
}
