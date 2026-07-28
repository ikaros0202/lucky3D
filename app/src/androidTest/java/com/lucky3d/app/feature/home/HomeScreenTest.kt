package com.lucky3d.app.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.ui.theme.Lucky3DTheme
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun localResultWithLeadingZeroRemainsVisibleBesideRetryState() {
        composeRule.setContent {
            Lucky3DTheme {
                HomeScreen(
                    state = HomeUiState(
                        latest = DrawRecord(
                            issue = "2026198",
                            drawDate = "2026-07-27",
                            number = DrawNumber.parse("007"),
                            officialDetailUrl = "https://www.cwl.gov.cn/c/2026198.shtml",
                            officialFingerprint = "fingerprint",
                        ),
                        syncState = HomeSyncState.ERROR,
                        lastSuccessEpochMillis = 100L,
                        failureType = "NETWORK",
                    ),
                    onRefresh = {},
                    onOpenHistory = {},
                    onOpenTrend = {},
                    onOpenPick = {},
                    onOpenSchemes = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("开奖号 007").assertIsDisplayed()
        composeRule.onNodeWithText("更新失败，可重试").assertIsDisplayed()
        composeRule.onNodeWithText("重试").assertIsDisplayed()
    }

    @Test
    fun invalidPayloadExplainsThatTheBatchWasNotWritten() {
        setHomeContent(
            HomeUiState(
                syncState = HomeSyncState.ERROR,
                failureType = "INVALID_PAYLOAD",
            ),
        )

        composeRule.onNodeWithText("官方数据格式异常，本批数据未写入。").assertIsDisplayed()
    }

    @Test
    fun officialCorrectionExplainsAffectedResultsWereRecalculated() {
        setHomeContent(
            HomeUiState(
                syncState = HomeSyncState.CORRECTED,
            ),
        )

        composeRule.onNodeWithText("官方数据有修正").assertIsDisplayed()
        composeRule.onNodeWithText("相关期号、指标和方案复盘已重新计算。").assertIsDisplayed()
    }

    private fun setHomeContent(state: HomeUiState) {
        composeRule.setContent {
            Lucky3DTheme {
                HomeScreen(
                    state = state,
                    onRefresh = {},
                    onOpenHistory = {},
                    onOpenTrend = {},
                    onOpenPick = {},
                    onOpenSchemes = {},
                    onOpenSettings = {},
                )
            }
        }
    }
}
