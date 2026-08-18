package com.lucky3d.app.feature.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
import com.lucky3d.app.data.repository.DrawQuery
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.ui.theme.Lucky3DTheme
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun issueIsOnlyQueryEntryAndYunnanAnnouncementIsInline() {
        composeRule.setContent {
            Lucky3DTheme {
                HistoryScreen(
                    state = HistoryUiState(
                        query = DrawQuery.Issue("2026213"),
                        records = listOf(draw()),
                        yunnanAnnouncement = announcement(
                            payout = true,
                        ),
                    ),
                    onBack = {},
                    onSearchIssue = {},
                )
            }
        }

        composeRule.onNodeWithText("开奖详情").assertIsDisplayed()
        composeRule.onNodeWithText("选择期号").assertIsDisplayed()
        composeRule.onNodeWithText("2026213期").assertIsDisplayed()
        composeRule.onNodeWithText("2026-08-11").assertIsDisplayed()
        composeRule.onNodeWithText("云南省开奖公告").assertIsDisplayed()
        composeRule.onNodeWithText("销售额").assertIsDisplayed()
        composeRule.onNodeWithText("奖池资金余额").assertIsDisplayed()
        composeRule.onNodeWithText("1,625,075.48元").assertIsDisplayed()
        composeRule.onNodeWithText("单选").assertIsDisplayed()
        composeRule.onNodeWithText("组三").assertIsDisplayed()
        composeRule.onAllNodesWithText("组六").assertCountEquals(2)
        composeRule.onNodeWithText("中奖 12,155注").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1,040元/注").assertIsDisplayed()
        composeRule.onNodeWithText("【派奖】单选11,029注 组六13,214注").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("基础中奖").assertCountEquals(0)
        composeRule.onAllNodesWithText("官方数据").assertCountEquals(0)
        composeRule.onAllNodesWithText("本省销售额").assertCountEquals(0)
        composeRule.onAllNodesWithText("全国").assertCountEquals(0)
    }

    @Test
    fun payoutSummaryIsHiddenWhenNoPlayHasPositivePayout() {
        composeRule.setContent {
            Lucky3DTheme {
                HistoryScreen(
                    state = HistoryUiState(
                        query = DrawQuery.Issue("2024291"),
                        records = listOf(draw(issue = "2024291", date = "2024-10-31")),
                        yunnanAnnouncement = announcement(
                            issue = "2024291",
                            date = "2024-10-31",
                            payout = false,
                        ),
                    ),
                    onBack = {},
                    onSearchIssue = {},
                )
            }
        }

        composeRule.onNodeWithText("云南省开奖公告").assertIsDisplayed()
        composeRule.onAllNodesWithText("【派奖】", substring = true).assertCountEquals(0)
    }

    @Test
    fun currentIssueUses2026214PayoutAmountsInsteadOfPreviousIssueValues() {
        composeRule.setContent {
            Lucky3DTheme {
                HistoryScreen(
                    state = HistoryUiState(
                        query = DrawQuery.Issue("2026214"),
                        records = listOf(draw(issue = "2026214", date = "2026-08-12", number = "545")),
                        yunnanAnnouncement = announcement2026214(),
                    ),
                    onBack = {},
                    onSearchIssue = {},
                )
            }
        }

        composeRule.onNodeWithText("460元/注").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("142元/注").assertIsDisplayed()
        composeRule.onAllNodesWithText("246元/注").assertCountEquals(0)
        composeRule.onNodeWithText("【派奖】单选4,573注 组三5,596注").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun issueSelectorListsValidIssuesAndDateIsReadOnlyMetadata() {
        var searchedIssue: String? = null
        val selected = draw()
        val previous = draw(issue = "2026212", date = "2026-08-10")
        composeRule.setContent {
            Lucky3DTheme {
                HistoryScreen(
                    state = HistoryUiState(
                        query = DrawQuery.Issue("2026213"),
                        availableIssues = listOf(selected, previous),
                        records = listOf(selected),
                    ),
                    onBack = {},
                    onSearchIssue = { searchedIssue = it },
                )
            }
        }

        composeRule.onNodeWithText("选择期号").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("2026212期").assertIsDisplayed().performClick()
        assert(searchedIssue == "2026212")
        composeRule.onAllNodesWithText("开奖日期").assertCountEquals(0)
    }

    private fun draw(
        issue: String = "2026213",
        date: String = "2026-08-11",
        number: String = "872",
    ) = DrawRecord(
        issue = issue,
        drawDate = date,
        number = DrawNumber.parse(number),
        officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        officialFingerprint = "fingerprint",
        salesAmountYuan = 123_456_789L,
    )

    private fun announcement(
        issue: String = "2026213",
        date: String = "2026-08-11",
        payout: Boolean,
    ) = YunnanAnnouncement(
        issue = issue,
        drawDate = date,
        number = DrawNumber.parse("872"),
        salesAmountYuan = 123_456_789L,
        winningTotalYuan = 98_765_432L,
        prizePoolBalanceFen = 162_507_548L,
        plays = listOf(
            YunnanPlayAnnouncement(
                playType = YunnanPlayType.SINGLE,
                winningCount = 12_155L,
                prizePerBetYuan = 1_040L,
                payoutCount = if (payout) 11_029L else null,
                payoutPerBetYuan = if (payout) 246L else null,
            ),
            YunnanPlayAnnouncement(
                playType = YunnanPlayType.GROUP3,
                winningCount = 0L,
                prizePerBetYuan = 346L,
                payoutCount = null,
                payoutPerBetYuan = null,
            ),
            YunnanPlayAnnouncement(
                playType = YunnanPlayType.GROUP6,
                winningCount = 15_216L,
                prizePerBetYuan = 173L,
                payoutCount = if (payout) 13_214L else null,
                payoutPerBetYuan = if (payout) 60L else null,
            ),
        ),
        redemptionDeadline = null,
        sourceUpdatedAt = date,
        fetchedAtEpochMillis = 0L,
        fingerprint = "yunnan-fingerprint",
    )

    private fun announcement2026214() = YunnanAnnouncement(
        issue = "2026214",
        drawDate = "2026-08-12",
        number = DrawNumber.parse("545"),
        salesAmountYuan = 18_650_174L,
        winningTotalYuan = 10_971_227L,
        prizePoolBalanceFen = 162_507_548L,
        plays = listOf(
            YunnanPlayAnnouncement(
                playType = YunnanPlayType.SINGLE,
                winningCount = 5_607L,
                prizePerBetYuan = 1_040L,
                payoutCount = 4_573L,
                payoutPerBetYuan = 460L,
            ),
            YunnanPlayAnnouncement(
                playType = YunnanPlayType.GROUP3,
                winningCount = 6_426L,
                prizePerBetYuan = 346L,
                payoutCount = 5_596L,
                payoutPerBetYuan = 142L,
            ),
            YunnanPlayAnnouncement(
                playType = YunnanPlayType.GROUP6,
                winningCount = 0L,
                prizePerBetYuan = 173L,
            ),
        ),
        redemptionDeadline = "2026-10-12",
        sourceUpdatedAt = "2026-08-13 08:57:54",
        fetchedAtEpochMillis = 0L,
        fingerprint = "yunnan-fingerprint-2026214",
    )
}
