package com.lucky3d.app.feature.trend

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.omission.HeatLevel
import com.lucky3d.app.ui.theme.Lucky3DTheme
import org.junit.Rule
import org.junit.Test

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
                    onTogglePosition = {},
                    onSelectPoint = {},
                    onShowStatistics = {},
                    onScaleChange = {},
                    onReturnLatest = {},
                )
            }
        }

        composeRule.onNodeWithText("Lucky3D").assertIsDisplayed()
        composeRule.onNodeWithText("走势").assertIsDisplayed()
        composeRule.onNodeWithText("本地数据").assertIsDisplayed()
        composeRule.onNodeWithText("10期").assertIsDisplayed()
        composeRule.onNodeWithText("30期").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText("50期").assertIsDisplayed()
        composeRule.onNodeWithText("100期").assertIsDisplayed()
        composeRule.onNodeWithText("自定义").assertIsDisplayed()
        composeRule.onNodeWithText("期号").assertIsDisplayed()
        composeRule.onNodeWithText("开奖号").assertIsDisplayed()
        composeRule.onNodeWithText("百位").assertIsDisplayed()
        composeRule.onNodeWithText("十位").assertIsDisplayed()
        composeRule.onNodeWithText("个位").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("纵向长页、横向可滑动的百位十位个位遗漏走势图")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2026199期待开奖").assertExists()
        composeRule.onNodeWithText("出现次数").assertExists()
        composeRule.onNodeWithText("当前遗漏").assertExists()
        composeRule.onNodeWithText("平均遗漏").assertExists()
        composeRule.onNodeWithText("最大遗漏").assertExists()
        composeRule
            .onNodeWithContentDescription("当前点位：2026198期，百位数字6，遗漏0")
            .assertIsDisplayed()
        composeRule.onNodeWithText("回到最新").assertDoesNotExist()
        composeRule.onNodeWithText("数据来源").assertDoesNotExist()
        composeRule.onNodeWithText("内部窗口策略").assertDoesNotExist()
    }

    @Test
    fun latestHundredsPointIsShownBeforeTheUserSelectsAnotherPoint() {
        composeRule.setContent {
            Lucky3DTheme {
                TrendScreen(
                    state = normalState().copy(selectedPoint = null),
                    onSetWindow = {},
                    onTogglePosition = {},
                    onSelectPoint = {},
                    onShowStatistics = {},
                    onScaleChange = {},
                    onReturnLatest = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("当前点位：2026198期，百位数字8，遗漏9")
            .assertIsDisplayed()
    }

    private fun normalState(): TrendUiState {
        val draws = (189..198).map { suffix ->
            DrawRecord(
                issue = "2026$suffix",
                drawDate = "2026-07-${(suffix - 171).toString().padStart(2, '0')}",
                number = DrawNumber.of(suffix % 10, (suffix + 2) % 10, (suffix + 5) % 10),
                officialDetailUrl = "https://www.cwl.gov.cn/c/2026$suffix.shtml",
                officialFingerprint = "fingerprint-2026$suffix",
            )
        }
        val points = draws.flatMapIndexed { rowIndex, draw ->
            listOf(
                TrendPoint(draw.issue, rowIndex, TrendPosition.HUNDREDS, draw.number.hundreds, rowIndex),
                TrendPoint(draw.issue, rowIndex, TrendPosition.TENS, draw.number.tens, rowIndex + 1),
                TrendPoint(draw.issue, rowIndex, TrendPosition.ONES, draw.number.ones, rowIndex + 2),
            )
        }
        val selected = TrendPoint("2026198", 9, TrendPosition.HUNDREDS, 6, 0)
        return TrendUiState(
            visibleDraws = draws,
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
