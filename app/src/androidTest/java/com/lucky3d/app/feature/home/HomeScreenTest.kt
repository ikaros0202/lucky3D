package com.lucky3d.app.feature.home

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.core.model.TrialNumber
import com.lucky3d.app.core.model.TrialSource
import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
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
    fun confirmedHomeUsesCrystalArtworkBehindLiveData() {
        val latest = draw("2026198", "685")

        setHomeContent(
            state = HomeUiState(
                latest = latest,
                insights = buildHomeInsights(listOf(latest)),
                trialNumber = trial("2026199", "232"),
            ),
        )

        composeRule.onNodeWithTag("home_crystal_artwork").assertIsDisplayed()
        composeRule.onNodeWithTag("home_live_data_layer").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("开奖号 6，8，5").assertIsDisplayed()
        composeRule.onNodeWithText("2026198期").assertIsDisplayed()
        composeRule.onNodeWithText("232").assertIsDisplayed()
    }

    @Test
    fun crystalHomeShowsFocusedDrawAnalysisAndTargetedQueries() {
        val latest = draw("2026198", "685")
        val history = (1..30).map { index -> draw("2026${index.toString().padStart(3, '0')}", "111") }
        var openedIssue: String? = null

        setHomeContent(
            state = HomeUiState(
                latest = latest,
                insights = buildHomeInsights(listOf(latest) + history.asReversed()),
                trialNumber = trial("2026199", "232"),
            ),
            onOpenIssue = { openedIssue = it },
        )

        composeRule.onNodeWithText("昨日开奖").assertDoesNotExist()
        composeRule.onNodeWithText("最近开奖").assertDoesNotExist()
        composeRule.onNodeWithText("胆码拖码").assertDoesNotExist()
        composeRule.onNodeWithText("柔 光 晶 体").assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("按期号查看第2026198期，开奖日期2026-07-27")
            .performClick()
        composeRule.runOnIdle {
            assertThat(openedIssue).isEqualTo("2026198")
        }

        composeRule
            .onNodeWithContentDescription("开奖号 6，8，5")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("home_crystal_artwork").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("和值 19").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("和尾 9").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("跨度 3").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("组选 组六").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("奇偶 1:2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("大小 3:0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("012路 1:0:2").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("连号 5-6").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("冷态命中 3").assertDoesNotExist()
        composeRule.onNodeWithText("云南省开奖公告").assertIsDisplayed()
        composeRule.onNodeWithText("开奖公告更新中").assertIsDisplayed()
        composeRule.onNodeWithText("基础中奖").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("百位数字6，此前遗漏30，冷态，窗口30期")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("冷热刻度").assertIsDisplayed()
        composeRule.onNodeWithText("试机号").assertIsDisplayed()
        composeRule.onNodeWithTag("home_trial_number").assertIsDisplayed()
        composeRule.onNodeWithTag("home_refresh_icon").assertIsDisplayed()
        composeRule.onNodeWithTag("home_settings_icon").assertIsDisplayed()
        composeRule.onNodeWithTag("home_announcement_issue").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("刷新开奖号和试机号")
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithContentDescription("设置")
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("彩经网模拟试机号").assertDoesNotExist()
        composeRule.onNodeWithText("非正式开奖").assertDoesNotExist()
        composeRule.onNodeWithText("每日16:35后检查当日试机号").assertDoesNotExist()
    }

    @Test
    fun yunnanAnnouncementShowsProvinceTotalsAndSupportedPlayResults() {
        val latest = draw("2026198", "685")
        setHomeContent(
            state = HomeUiState(
                latest = latest,
                insights = buildHomeInsights(listOf(latest)),
                yunnanAnnouncement = yunnan(
                    issue = latest.issue,
                    number = latest.number.value,
                    singlePayout = 11_029L,
                    group6Payout = 13_214L,
                ),
            ),
        )

        composeRule.onNodeWithText("云南省开奖公告").assertIsDisplayed()
        composeRule.onNodeWithText("销售额").assertIsDisplayed()
        composeRule.onNodeWithText("19,051,910元").assertIsDisplayed()
        composeRule.onNodeWithText("18,799,307元").assertIsDisplayed()
        composeRule.onNodeWithText("12,155注").assertIsDisplayed()
        composeRule.onNodeWithText("1,040元/注").assertIsDisplayed()
        composeRule.onNodeWithText("【派奖】单选11,029注  组六13,214注").assertIsDisplayed()
        composeRule.onNodeWithText("基础中奖").assertDoesNotExist()
        composeRule.onNodeWithText("官方数据").assertDoesNotExist()
        composeRule.onNodeWithText("本省销售额").assertDoesNotExist()
        composeRule.onNodeWithText("全国销售总额").assertDoesNotExist()
    }

    @Test
    fun yunnanAnnouncementOmitsPayoutRowWhenNoSupportedPlayHasPayout() {
        val latest = draw("2026198", "685")
        setHomeContent(
            state = HomeUiState(
                latest = latest,
                insights = buildHomeInsights(listOf(latest)),
                yunnanAnnouncement = yunnan(latest.issue, latest.number.value),
            ),
        )

        composeRule.onNodeWithText("云南省开奖公告").assertIsDisplayed()
        composeRule.onNodeWithText("【派奖】", substring = true).assertDoesNotExist()
    }

    @Test
    fun staleYunnanAnnouncementIsHiddenUntilLatestIssueIsAvailable() {
        val latest = draw("2026214", "545")
        setHomeContent(
            state = HomeUiState(
                latest = latest,
                insights = buildHomeInsights(listOf(latest)),
                yunnanAnnouncement = yunnan("2026198", "685", singlePayout = 11_029L),
            ),
        )

        composeRule.onAllNodesWithText("2026214", substring = true).assertCountEquals(2)
        composeRule.onNodeWithText("2026198", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("home_announcement_pending").assertIsDisplayed()
        composeRule.onNodeWithText("19,051,910", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("12,155", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("11,029", substring = true).assertDoesNotExist()
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
        composeRule.onNodeWithText("---").assertIsDisplayed()
        composeRule.onNodeWithText("更新失败，可重试").assertIsDisplayed()
    }

    @Test
    fun staleYunnanAnnouncementFailureDoesNotReportHomeAsUpdated() {
        val latest = draw("2026214", "545")
        setHomeContent(
            HomeUiState(
                latest = latest,
                insights = buildHomeInsights(listOf(latest)),
                syncState = HomeSyncState.UP_TO_DATE,
                announcementRefreshFailed = true,
            ),
        )

        composeRule.onNodeWithText("更新失败，可重试").assertIsDisplayed()
        composeRule.onNodeWithText("已更新").assertDoesNotExist()
    }

    @Test
    fun trialDigitsRemainPlaceholderWhileTodayTrialIsLoading() {
        setHomeContent(
            HomeUiState(
                trialState = LiveContentRefreshState.Refreshing,
            ),
        )

        composeRule.onNodeWithText("---").assertIsDisplayed()
    }

    @Test
    fun visibleRefreshRefreshesDrawAndTrialTogether() {
        var drawRefreshes = 0
        var trialRefreshes = 0
        setHomeContent(
            state = HomeUiState(),
            onRefresh = { drawRefreshes++ },
            onRefreshTrial = { trialRefreshes++ },
        )

        composeRule
            .onNodeWithContentDescription("刷新开奖号和试机号")
            .performClick()
        composeRule.runOnIdle {
            assertThat(drawRefreshes).isEqualTo(1)
            assertThat(trialRefreshes).isEqualTo(1)
        }
    }

    @Test
    fun approvedHomeVisibleRefreshRefreshesDrawAndTrialTogether() {
        val latest = draw("2026198", "685")
        var drawRefreshes = 0
        var trialRefreshes = 0
        setHomeContent(
            state = HomeUiState(
                latest = latest,
                insights = buildHomeInsights(listOf(latest)),
            ),
            onRefresh = { drawRefreshes++ },
            onRefreshTrial = { trialRefreshes++ },
        )

        composeRule
            .onNodeWithContentDescription("刷新开奖号和试机号")
            .performClick()
        composeRule.runOnIdle {
            assertThat(drawRefreshes).isEqualTo(1)
            assertThat(trialRefreshes).isEqualTo(1)
        }
    }

    @Test
    fun compactTrialDisplayIsNotClickable() {
        setHomeContent(state = HomeUiState())

        composeRule.onNodeWithText("试机号").assertHasNoClickAction()
    }

    @Test
    fun compactTrialShowsFailureOnlyForManualPostReleaseFailure() {
        setHomeContent(state = HomeUiState(trialManualRefreshFailed = true))

        composeRule.onNodeWithText("失败").assertIsDisplayed()
        composeRule.onNodeWithText("---").assertDoesNotExist()
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
    fun correctionUsesUserFacingUpdateCopy() {
        setHomeContent(
            HomeUiState(syncState = HomeSyncState.CORRECTED),
        )

        composeRule.onNodeWithText("开奖号已更新").assertIsDisplayed()
        composeRule.onNodeWithText("官方数据有修正").assertDoesNotExist()
        composeRule.onNodeWithText("相关期号、指标和方案复盘已重新计算。").assertDoesNotExist()
    }

    private fun setHomeContent(
        state: HomeUiState,
        onRefresh: () -> Unit = {},
        onRefreshTrial: () -> Unit = {},
        onOpenIssue: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            Lucky3DTheme {
                HomeScreen(
                    state = state,
                    onRefresh = onRefresh,
                    onRefreshTrial = onRefreshTrial,
                    onOpenIssue = onOpenIssue,
                    onOpenSettings = {},
                )
            }
        }
    }

    private fun draw(issue: String, number: String, salesAmountYuan: Long? = null) = DrawRecord(
        issue = issue,
        drawDate = "2026-07-27",
        number = DrawNumber.parse(number),
        officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
        officialFingerprint = "fingerprint-$issue",
        salesAmountYuan = salesAmountYuan,
    )

    private fun trial(issue: String, number: String) = TrialNumber(
        issue = issue,
        number = number,
        source = TrialSource.CJCP_SIMULATED,
        sourcePageUrl = "https://m.cjcp.cn/kjhsjh/3dls/",
        sourceLocalDate = LocalDate.parse("2026-07-31"),
        fetchedAtEpochMillis = 1L,
    )

    private fun yunnan(
        issue: String,
        number: String,
        singlePayout: Long? = null,
        group6Payout: Long? = null,
    ) = YunnanAnnouncement(
        issue = issue,
        drawDate = "2026-07-27",
        number = DrawNumber.parse(number),
        salesAmountYuan = 19_051_910L,
        winningTotalYuan = 18_799_307L,
        plays = listOf(
            YunnanPlayAnnouncement(
                playType = YunnanPlayType.SINGLE,
                winningCount = 12_155L,
                prizePerBetYuan = 1_040L,
                payoutCount = singlePayout,
                payoutPerBetYuan = singlePayout?.let { 246L },
            ),
            YunnanPlayAnnouncement(YunnanPlayType.GROUP3, 0L, 346L),
            YunnanPlayAnnouncement(
                playType = YunnanPlayType.GROUP6,
                winningCount = 15_216L,
                prizePerBetYuan = 173L,
                payoutCount = group6Payout,
                payoutPerBetYuan = group6Payout?.let { 60L },
            ),
        ),
        redemptionDeadline = "2026-10-10",
        sourceUpdatedAt = "2026-08-11 21:12:56",
        fetchedAtEpochMillis = 1L,
        fingerprint = "yunnan-$issue",
    )
}
