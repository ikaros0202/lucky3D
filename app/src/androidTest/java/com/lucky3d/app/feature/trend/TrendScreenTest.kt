package com.lucky3d.app.feature.trend

import com.google.common.truth.Truth.assertThat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.geometry.Offset
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.omission.HeatLevel
import com.lucky3d.app.ui.theme.Lucky3DTheme
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class TrendScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun approvedLongTableKeepsThePrototypeStructureAndRequiredPeriods() {
        composeRule.setContent {
            Lucky3DTheme {
                TrendScreen(
                    state = normalState(),
                    onSetWindow = {},
                    onSelectPoint = {},
                )
            }
        }

        composeRule.onNodeWithText("Lucky3D").assertIsDisplayed()
        composeRule.onAllNodesWithText("走势").assertCountEquals(1)
        composeRule.onNodeWithText("本地数据").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("显示期数，当前30期")
            .assertIsDisplayed()
            .assertIsSelected()
            .performClick()
        composeRule.onNodeWithText("10期").assertIsDisplayed()
        composeRule.onNodeWithText("60期").assertIsDisplayed()
        composeRule.onNodeWithText("100期").assertIsDisplayed()
        composeRule.onNodeWithText("自定义").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("表头：期号、试机号、开奖号、百位、十位、个位", substring = true)
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("纵向长页、横向可滑动的百位十位个位遗漏走势图", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("纵向连续浏览期次", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("试机号　开奖号　百位", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("全览").assertDoesNotExist()
        composeRule.onNodeWithText("100%").assertDoesNotExist()
        composeRule.onNodeWithText("缩小").assertDoesNotExist()
        composeRule.onNodeWithText("放大").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("2026199期待开奖", substring = true)
            .assertExists()
        composeRule
            .onNodeWithContentDescription("表尾统计：出现次数、当前遗漏、平均遗漏、最大遗漏", substring = true)
            .assertExists()
        composeRule
            .onNodeWithContentDescription("当前点位：2026198期，百位数字6，遗漏0")
            .assertDoesNotExist()
        composeRule.onNodeWithText("回到最新").assertDoesNotExist()
        composeRule.onNodeWithText("数据来源").assertDoesNotExist()
        composeRule.onNodeWithText("内部窗口策略").assertDoesNotExist()
    }

    @Test
    fun trendDoesNotShowAPointSummaryBelowTheChart() {
        composeRule.setContent {
            Lucky3DTheme {
                TrendScreen(
                    state = normalState().copy(selectedPoint = null),
                    onSetWindow = {},
                    onSelectPoint = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("当前点位：2026198期，百位数字8，遗漏9")
            .assertDoesNotExist()
        composeRule.onNodeWithText("2026198期 · 开奖号", substring = true).assertDoesNotExist()
    }

    @Test
    fun emptyStateKeepsTheApprovedHeaderAndExplainsMissingData() {
        composeRule.setContent {
            Lucky3DTheme {
                TrendScreen(
                    state = TrendUiState(),
                    onSetWindow = {},
                    onSelectPoint = {},
                )
            }
        }

        composeRule.onNodeWithText("Lucky3D").assertIsDisplayed()
        composeRule.onNodeWithText("本地数据").assertIsDisplayed()
        composeRule.onNodeWithText("没有可用于走势图的数据").assertIsDisplayed()
        composeRule.onNodeWithText("请返回首页确认内置数据是否可读取。").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("纵向长页、横向可滑动的百位十位个位遗漏走势图", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun swipingUpOnTheRightTableScrollsTheWholePageVertically() {
        composeRule.setContent {
            Lucky3DTheme {
                TrendScreen(
                    state = normalState(),
                    onSetWindow = {},
                    onSelectPoint = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("纵向长页、横向可滑动的百位十位个位遗漏走势图", substring = true)
            .performTouchInput { swipeUp(durationMillis = 500) }
        composeRule
            .onNodeWithContentDescription("纵向长页、横向可滑动的百位十位个位遗漏走势图", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun twoPointersRequestWholeTableZoomWithoutAVisibleZoomButton() {
        val requestedScales = mutableListOf<Float>()
        composeRule.setContent {
            Lucky3DTheme {
                TrendScreen(
                    state = normalState(drawCount = 100),
                    onSetWindow = {},
                    onSetScale = { requestedScales += it },
                    onSelectPoint = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("纵向长页、横向可滑动的百位十位个位遗漏走势图", substring = true)
            .performTouchInput {
                val gestureY = height * 0.25f
                down(0, Offset(width * 0.20f, gestureY))
                down(1, Offset(width * 0.80f, gestureY))
                moveTo(0, Offset(width * 0.38f, gestureY), delayMillis = 50)
                moveTo(1, Offset(width * 0.62f, gestureY), delayMillis = 50)
                up(0)
                up(1)
            }

        composeRule.runOnIdle {
            assertThat(requestedScales).isNotEmpty()
            assertThat(requestedScales.min()).isLessThan(1f)
        }
    }

    @Test
    fun periodSelectorKeepsItsTextStableAtLargeScale() {
        composeRule.setContent {
            Lucky3DTheme {
                TrendScreen(
                    state = normalState().copy(scale = 2f),
                    onSetWindow = {},
                    onSelectPoint = {},
                )
            }
        }

        composeRule.onAllNodesWithText("走势").assertCountEquals(1)
        composeRule
            .onNodeWithContentDescription("显示期数，当前30期")
            .assertIsDisplayed()
        composeRule.onNodeWithText("30期").assertIsDisplayed()
    }

    @Test
    fun periodSelectorIsAlignedBesideTheTrendTitleInTheHeader() {
        composeRule.setContent {
            Lucky3DTheme {
                TrendScreen(
                    state = normalState(),
                    onSetWindow = {},
                    onSelectPoint = {},
                )
            }
        }

        val titleBounds = composeRule
            .onNodeWithText("走势")
            .fetchSemanticsNode()
            .boundsInRoot
        val selectorBounds = composeRule
            .onNodeWithContentDescription("显示期数，当前30期")
            .fetchSemanticsNode()
            .boundsInRoot

        assertThat(abs(titleBounds.center.y - selectorBounds.center.y)).isLessThan(2f)
        assertThat(selectorBounds.left).isGreaterThan(titleBounds.right)
    }

    @Test
    fun continuousPinchAccumulatesEveryTransformFrameWithoutExternalStateRoundTrips() {
        val requestedScales = mutableListOf<Float>()
        composeRule.setContent {
            Lucky3DTheme {
                TrendScreen(
                    state = normalState(),
                    onSetWindow = {},
                    onSetScale = { requestedScales += it },
                    onSelectPoint = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("纵向长页、横向可滑动的百位十位个位遗漏走势图", substring = true)
            .performTouchInput {
                val gestureY = height * 0.30f
                down(0, Offset(width * 0.20f, gestureY))
                down(1, Offset(width * 0.80f, gestureY))
                moveTo(0, Offset(width * 0.15f, gestureY), delayMillis = 20)
                moveTo(1, Offset(width * 0.85f, gestureY), delayMillis = 20)
                moveTo(0, Offset(width * 0.10f, gestureY), delayMillis = 20)
                moveTo(1, Offset(width * 0.90f, gestureY), delayMillis = 20)
                moveTo(0, Offset(width * 0.05f, gestureY), delayMillis = 20)
                moveTo(1, Offset(width * 0.95f, gestureY), delayMillis = 20)
                up(0)
                up(1)
            }

        composeRule.runOnIdle {
            assertThat(requestedScales).isNotEmpty()
            assertThat(requestedScales.last()).isGreaterThan(1.4f)
        }
    }

    private fun normalState(drawCount: Int = 10): TrendUiState {
        val draws = ((199 - drawCount)..198).map { suffix ->
            val issue = "2026${suffix.toString().padStart(3, '0')}"
            DrawRecord(
                issue = issue,
                drawDate = "2026-07-${(Math.floorMod(suffix - 172, 28) + 1).toString().padStart(2, '0')}",
                number = DrawNumber.of(suffix % 10, (suffix + 2) % 10, (suffix + 5) % 10),
                officialDetailUrl = "https://www.cwl.gov.cn/c/$issue.shtml",
                officialFingerprint = "fingerprint-$issue",
            )
        }
        val points = draws.flatMapIndexed { rowIndex, draw ->
            listOf(
                TrendPoint(draw.issue, rowIndex, TrendPosition.HUNDREDS, draw.number.hundreds, rowIndex),
                TrendPoint(draw.issue, rowIndex, TrendPosition.TENS, draw.number.tens, rowIndex + 1),
                TrendPoint(draw.issue, rowIndex, TrendPosition.ONES, draw.number.ones, rowIndex + 2),
            )
        }
        val selected = TrendPoint("2026198", draws.lastIndex, TrendPosition.HUNDREDS, 6, 0)
        return TrendUiState(
            visibleDraws = draws,
            tableRows = draws.mapIndexed { rowIndex, draw ->
                TrendTableRow(
                    issue = draw.issue,
                    drawNumber = draw.number.value,
                    omissions = List(30) { column -> (rowIndex + column) % 49 },
                )
            },
            points = points,
            selectedPoint = selected,
            statistics = listOf(
                TrendPositionStatistics(
                    position = TrendPosition.HUNDREDS,
                    requestedWindowSize = 30,
                    actualWindowSize = 30,
                    sampleComplete = true,
                    digits = listOf(
                        TrendDigitStatistics(
                            digit = 7,
                            currentOmission = 48,
                            averageOmission = 8.07,
                            maxOmission = 51,
                            occurrences = 0,
                            heatLevel = HeatLevel.COLD,
                        ),
                    ),
                ),
            ),
        )
    }
}
