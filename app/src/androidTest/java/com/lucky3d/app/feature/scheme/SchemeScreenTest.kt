package com.lucky3d.app.feature.scheme

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lucky3d.app.data.repository.SavedReplay
import com.lucky3d.app.data.repository.SavedScheme
import com.lucky3d.app.data.repository.SavedTemplate
import com.lucky3d.app.data.repository.SchemeWithReplay
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.filter.SpanRange
import com.lucky3d.app.domain.filter.SumRange
import com.lucky3d.app.ui.theme.Lucky3DTheme
import org.junit.Rule
import org.junit.Test

class SchemeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptySchemesProvideAUsefulNextStep() {
        composeRule.setContent {
            Lucky3DTheme {
                SchemeScreen(
                    state = SchemeUiState(),
                    onShowSection = {},
                    onSelectScheme = {},
                    onSelectTemplate = {},
                    onSetBacktestRange = { _, _ -> },
                    onRunBacktest = {},
                    onCopyScheme = { _, _ -> },
                    onUpdateNote = { _, _ -> },
                    onDismissStatus = {},
                    onStartPick = {},
                )
            }
        }

        composeRule.onNodeWithText("还没有保存方案").assertIsDisplayed()
        composeRule.onNodeWithText("开始选号").assertIsDisplayed()
    }

    @Test
    fun confirmedCinnabarSchemeListKeepsCoreContentOnOneScreen() {
        composeRule.setContent {
            Lucky3DTheme {
                SchemeScreen(
                    state = SchemeUiState(schemes = schemes()),
                    onShowSection = {},
                    onSelectScheme = {},
                    onSelectTemplate = {},
                    onSetBacktestRange = { _, _ -> },
                    onRunBacktest = {},
                    onCopyScheme = { _, _ -> },
                    onUpdateNote = { _, _ -> },
                    onDismissStatus = {},
                    onStartPick = {},
                )
            }
        }

        composeRule.onNodeWithText("方案 2").assertIsDisplayed()
        composeRule.onNodeWithText("模板 0").assertIsDisplayed()
        composeRule.onNodeWithText("按期号、日期或标题查找").assertIsDisplayed()
        composeRule.onNodeWithText("全部").assertIsDisplayed()
        composeRule.onNodeWithText("待开奖").assertIsDisplayed()
        composeRule.onNodeWithText("已复盘").assertIsDisplayed()
        composeRule.onNodeWithText("1个方案已完成开奖复盘").assertIsDisplayed()
        composeRule.onNodeWithText("进行中").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("2026204期，周三定位方案，等待开奖")
            .assertIsDisplayed()
        composeRule.onNodeWithText("历史复盘").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("2026203期，和值筛选方案，包含开奖号685")
            .assertIsDisplayed()
        composeRule.onNodeWithText("数据来源").assertDoesNotExist()
    }

    @Test
    fun templateSectionMatchesTheConfirmedGroupedList() {
        composeRule.setContent {
            Lucky3DTheme {
                SchemeScreen(
                    state = SchemeUiState(
                        section = SchemeSection.TEMPLATES,
                        schemes = schemes(),
                        templates = templates(),
                    ),
                    onShowSection = {},
                    onSelectScheme = {},
                    onSelectTemplate = {},
                    onSetBacktestRange = { _, _ -> },
                    onRunBacktest = {},
                    onCopyScheme = { _, _ -> },
                    onUpdateNote = { _, _ -> },
                    onDismissStatus = {},
                    onStartPick = {},
                )
            }
        }

        composeRule.onNodeWithText("方案 2").assertIsDisplayed()
        composeRule.onNodeWithText("模板 2").assertIsDisplayed()
        composeRule
            .onNodeWithText("模板保存筛选条件，不绑定期号；可随时套用或回测。")
            .assertIsDisplayed()
        composeRule.onNodeWithText("常用模板").assertIsDisplayed()
        composeRule.onNodeWithText("和值跨度组合").assertIsDisplayed()
        composeRule.onNodeWithText("观察30期 · 2个条件").assertIsDisplayed()
        composeRule.onAllNodesWithText("历史回测")[0].assertIsDisplayed()
        composeRule.onAllNodesWithText("套用到本期")[0].assertIsDisplayed()
        composeRule.onNodeWithText("数据来源").assertDoesNotExist()
    }

    @Test
    fun replayedSchemeOpensAsTheConfirmedFullDetailPage() {
        composeRule.setContent {
            Lucky3DTheme {
                SchemeScreen(
                    state = SchemeUiState(
                        schemes = schemes(),
                        selectedSchemeId = "replayed",
                    ),
                    onShowSection = {},
                    onSelectScheme = {},
                    onSelectTemplate = {},
                    onSetBacktestRange = { _, _ -> },
                    onRunBacktest = {},
                    onCopyScheme = { _, _ -> },
                    onUpdateNote = { _, _ -> },
                    onDismissStatus = {},
                    onStartPick = {},
                )
            }
        }

        composeRule.onNodeWithText("方案详情").assertIsDisplayed()
        composeRule.onNodeWithText("2026203期 · 单选").assertIsDisplayed()
        composeRule.onNodeWithText("和值筛选方案").assertIsDisplayed()
        composeRule.onNodeWithText("已复盘").assertIsDisplayed()
        composeRule.onNodeWithText("包含开奖号").assertIsDisplayed()
        composeRule.onNodeWithText("12注").assertIsDisplayed()
        composeRule.onNodeWithText("1倍").assertIsDisplayed()
        composeRule.onNodeWithText("24元").assertIsDisplayed()
        composeRule.onNodeWithText("候选号码").assertIsDisplayed()
        composeRule.onNodeWithText("筛选条件").assertIsDisplayed()
        composeRule.onNodeWithText("备注").assertIsDisplayed()
        composeRule.onNodeWithText("已开奖方案不可覆盖，修改将创建副本").assertIsDisplayed()
        composeRule.onNodeWithText("复制到本期").assertIsDisplayed()
        composeRule.onNodeWithText("数据来源").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("更多操作").performClick()
        composeRule.onNodeWithText("编辑备注").assertIsDisplayed()
    }

    private fun schemes(): List<SchemeWithReplay> {
        val pending = scheme(
            id = "pending",
            issue = "2026204",
            title = "周三定位方案",
            isDrawn = false,
        )
        val replayed = scheme(
            id = "replayed",
            issue = "2026203",
            title = "和值筛选方案",
            isDrawn = true,
        )
        return listOf(
            SchemeWithReplay(pending, null),
            SchemeWithReplay(
                replayed,
                SavedReplay(
                    schemeId = replayed.id,
                    issue = replayed.issue,
                    winningNumber = DrawNumber.parse("685"),
                    covered = true,
                    matchedCandidate = DrawNumber.parse("685"),
                    revision = 1,
                    calculatedAtEpochMillis = 2,
                ),
            ),
        )
    }

    private fun templates(): List<SavedTemplate> = listOf(
        SavedTemplate(
            id = "template-1",
            name = "和值跨度组合",
            playType = PlayType.STRAIGHT,
            conditions = listOf(SumRange(14, 21), SpanRange(3, 7)),
            observationWindow = 30,
            ruleVersion = 1,
            updatedAtEpochMillis = 1,
        ),
        SavedTemplate(
            id = "template-2",
            name = "组选六基础过滤",
            playType = PlayType.GROUP6,
            conditions = listOf(SumRange(8, 20)),
            observationWindow = 50,
            ruleVersion = 1,
            updatedAtEpochMillis = 1,
        ),
    )

    private fun scheme(
        id: String,
        issue: String,
        title: String,
        isDrawn: Boolean,
    ) = SavedScheme(
        id = id,
        issue = issue,
        title = title,
        observationWindow = 30,
        templateId = null,
        playType = PlayType.STRAIGHT,
        conditions = emptyList(),
        candidates = listOf(DrawNumber.parse("685")),
        betCount = 12,
        multiplier = 1,
        amountYuan = 24,
        note = "",
        ruleVersion = 1,
        isDrawn = isDrawn,
        copiedFromSchemeId = null,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
}
