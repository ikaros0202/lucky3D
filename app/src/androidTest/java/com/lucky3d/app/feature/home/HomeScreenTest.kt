package com.lucky3d.app.feature.home

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.core.model.TrialSource
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.ui.theme.Lucky3DTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun crystalHomeShowsFocusedDrawAnalysisAndTargetedQueries() {
        val latest = draw("2026198", "685")
        val history = (1..30).map { index -> draw("2026${index.toString().padStart(3, '0')}", "111") }
        var openedIssue: String? = null
        var openedDate: String? = null

        setHomeContent(
            state = HomeUiState(
                latest = latest,
                insights = buildHomeInsights(listOf(latest) + history.asReversed()),
                trialNumber = trial("2026199", "232"),
            ),
            onOpenIssue = { openedIssue = it },
            onOpenDate = { openedDate = it },
        )

        composeRule.onNodeWithText("昨日开奖").assertDoesNotExist()
        composeRule.onNodeWithText("最近开奖").assertDoesNotExist()
        composeRule.onNodeWithText("胆码拖码").assertDoesNotExist()
        composeRule.onNodeWithText("柔 光 晶 体").assertIsDisplayed()

        composeRule.onNodeWithText("2026198期").performClick()
        composeRule.onNodeWithText("2026-07-27").performClick()
        composeRule.runOnIdle {
            assertThat(openedIssue).isEqualTo("2026198")
            assertThat(openedDate).isEqualTo("2026-07-27")
        }

        composeRule
            .onNodeWithContentDescription("开奖号 6，8，5")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("柔光晶体开奖台").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("和值 19").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("和尾 9").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("跨度 3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("组选 组六").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("奇偶 1:2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("大小 3:0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("012路 1:0:2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("连号 5-6").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("冷态命中 3").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("百位数字6，此前遗漏30，冷态，窗口30期")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("冷热刻度").assertIsDisplayed()
        composeRule.onNodeWithText("试机号").assertIsDisplayed()
        composeRule.onNodeWithText("彩经网模拟试机号").assertDoesNotExist()
        composeRule.onNodeWithText("非正式开奖").assertDoesNotExist()
        composeRule.onNodeWithText("每日16:35后检查当日试机号").assertDoesNotExist()
    }

    @Test
    fun localResultWithLeadingZeroRemainsVisibleBesideTrialFailure() {
        val latest = draw("2026198", "007")
        setHomeContent(
            HomeUiState(
                latest = latest,
                insights = buildHomeInsights(listOf(latest)),
                syncState = HomeSyncState.ERROR,
                lastSuccessEpochMillis = 100L,
                failureType = "NETWORK",
                trialState = LiveContentRefreshState.Failed(LiveContentFailure.NETWORK),
            ),
        )

        composeRule.onNodeWithContentDescription("开奖号 0，0，7").assertIsDisplayed()
        composeRule.onNodeWithText("暂不可用").assertIsDisplayed()
        composeRule.onNodeWithText("更新失败，可重试").assertIsDisplayed()
    }

    @Test
    fun cachedTrialRemainsVisibleWhenRefreshFails() {
        setHomeContent(
            HomeUiState(
                trialNumber = trial("2026199", "007"),
                trialState = LiveContentRefreshState.Failed(LiveContentFailure.NETWORK),
            ),
        )

        composeRule.onNodeWithContentDescription("007").assertIsDisplayed()
        composeRule.onNodeWithText("缓存内容可读，本次更新失败").assertDoesNotExist()
    }

    @Test
    fun beforeReleaseWindowExplainsWhenTrialCheckingStarts() {
        setHomeContent(
            HomeUiState(isBeforeTrialReleaseWindow = true),
        )

        composeRule.onNodeWithText("---").assertIsDisplayed()
        composeRule.onNodeWithText("每日16:35后检查当日试机号").assertDoesNotExist()
    }

    @Test
    fun invalidPayloadExplainsThatTheBatchWasNotWritten() {
        setHomeContent(
            HomeUiState(
                syncState = HomeSyncState.ERROR,
                failureType = "INVALID_PAYLOAD",
            ),
        )

        composeRule.onNodeWithText("更新失败，可重试").assertIsDisplayed()
        composeRule.onNodeWithText("官方数据格式异常，本批数据未写入。").assertDoesNotExist()
    }

    @Test
    fun officialCorrectionExplainsAffectedResultsWereRecalculated() {
        setHomeContent(
            HomeUiState(syncState = HomeSyncState.CORRECTED),
        )

        composeRule.onNodeWithText("官方数据有修正").assertIsDisplayed()
        composeRule.onNodeWithText("相关期号、指标和方案复盘已重新计算。").assertDoesNotExist()
    }

    private fun setHomeContent(
        state: HomeUiState,
        onOpenIssue: (String) -> Unit = {},
        onOpenDate: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            Lucky3DTheme {
                HomeScreen(
                    state = state,
                    onRefresh = {},
                    onRefreshTrial = {},
                    onOpenIssue = onOpenIssue,
                    onOpenDate = onOpenDate,
                    onOpenSettings = {},
                )
            }
        }
    }

    private fun draw(issue: String, number: String) = DrawRecord(
        issue = issue,
        drawDate = "2026-07-27",
        number = DrawNumber.parse(number),
        officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        officialFingerprint = "fingerprint-$issue",
    )

    private fun trial(issue: String, number: String) = TrialNumber(
        issue = issue,
        number = number,
        source = TrialSource.CJCP_SIMULATED,
        sourcePageUrl = "https://m.cjcp.cn/kjhsjh/3dls/",
        sourceLocalDate = LocalDate.parse("2026-07-31"),
        fetchedAtEpochMillis = 1L,
    )
}
