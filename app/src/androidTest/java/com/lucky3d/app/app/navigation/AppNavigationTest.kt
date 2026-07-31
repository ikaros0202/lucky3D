package com.lucky3d.app.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.lucky3d.app.MainActivity
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationShowsFiveAccessibleTabsWithMinimumTouchTargets() {
        listOf("首页", "走势分析", "号码筛选", "已保存方案", "彩报").forEach { label ->
            composeRule
                .onNodeWithContentDescription(label)
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }

        composeRule.onNodeWithContentDescription("首页").assertIsSelected()
        composeRule.onNodeWithContentDescription("走势分析").performClick().assertIsSelected()
        composeRule.onNodeWithContentDescription("首页").assertIsNotSelected()
    }

    @Test
    fun caibaoTabShowsLiveReaderWithoutDeveloperPlaceholderCopy() {
        composeRule
            .onNodeWithContentDescription("彩报")
            .performClick()
            .assertIsSelected()
        composeRule.onAllNodesWithText("彩报")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("更新彩报").assertIsDisplayed()
        composeRule
            .onNodeWithText("当前为静态示意内容，不会自动更新")
            .assertDoesNotExist()
        composeRule.onNodeWithText("随应用安装", substring = true).assertDoesNotExist()
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
