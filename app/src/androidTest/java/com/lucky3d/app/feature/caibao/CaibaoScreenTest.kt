package com.lucky3d.app.feature.caibao

import com.google.common.truth.Truth.assertThat
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.lucky3d.app.core.model.CaibaoDocument
import com.lucky3d.app.domain.livecontent.LiveContentFailure
import com.lucky3d.app.domain.livecontent.LiveContentRefreshState
import com.lucky3d.app.ui.theme.Lucky3DTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class CaibaoScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cachedCaibaoShowsOnlyUserFacingIssueAndReaderContent() {
        composeRule.setContent {
            Lucky3DTheme {
                CaibaoScreen(
                    state = cachedState(),
                    image = testImage(),
                    onRefresh = {},
                )
            }
        }

        composeRule.onNodeWithText("彩报").assertIsDisplayed()
        composeRule.onNodeWithText("2026204期").assertIsDisplayed()
        composeRule.onNodeWithText("A11第三版").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2026204期彩报").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("更新彩报").assertIsDisplayed()
        composeRule.onNodeWithText("理性购彩，量力而行").assertIsDisplayed()
        listOf(
            "当前为静态示意内容，不会自动更新",
            "这是一张随应用安装的报纸占位图，不是实际的福彩分析内容。",
            "数据来源",
            "缓存",
            "抓取时间",
            "牛彩网",
        ).forEach { forbidden ->
            composeRule.onNodeWithText(forbidden, substring = true).assertDoesNotExist()
        }
    }

    @Test
    fun refreshFailureKeepsCurrentCaibaoReadableAndOffersRetry() {
        composeRule.setContent {
            Lucky3DTheme {
                CaibaoScreen(
                    state = cachedState(
                        LiveContentRefreshState.Failed(LiveContentFailure.NETWORK),
                    ),
                    image = testImage(),
                    onRefresh = {},
                )
            }
        }

        composeRule
            .onNodeWithText("暂时无法更新，仍可查看当前彩报")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2026204期彩报").assertIsDisplayed()
        composeRule.onNodeWithText("重试").assertIsDisplayed()
    }

    @Test
    fun missingCaibaoUsesCompactRetryStateWithoutPlaceholderClaims() {
        composeRule.setContent {
            Lucky3DTheme {
                CaibaoScreen(
                    state = CaibaoUiState(
                        refreshState =
                            LiveContentRefreshState.Failed(LiveContentFailure.INVALID_IMAGE),
                    ),
                    image = null,
                    onRefresh = {},
                )
            }
        }

        composeRule.onNodeWithText("暂时没有可查看的彩报").assertIsDisplayed()
        composeRule.onNodeWithText("重试").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2026204期彩报").assertDoesNotExist()
        composeRule.onNodeWithText("静态示意", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("随应用安装", substring = true).assertDoesNotExist()
    }

    @Test
    fun currentIssueMenuScrollsToOlderPeriods() {
        val selected = mutableListOf<String>()
        composeRule.setContent {
            Lucky3DTheme {
                CaibaoScreen(
                    state = cachedState().copy(
                        selectedIssue = "2026204",
                        issueOptions = listOf(
                            "2026198",
                            "2026204",
                            "2026201",
                            "2026203",
                            "2026199",
                            "2026202",
                            "2026200",
                        ),
                    ),
                    image = testImage(),
                    onRefresh = {},
                    onSelectIssue = selected::add,
                    onPrevious = { selected += "previous" },
                    onNext = { selected += "next" },
                )
            }
        }

        composeRule.onNodeWithText("上一期").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("切换期号").assertDoesNotExist()
        composeRule.onNodeWithText("2026204期").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("2026198期").performScrollTo().assertIsDisplayed().performClick()

        assertThat(selected).containsExactly("previous", "2026198").inOrder()
    }

    private fun cachedState(
        refreshState: LiveContentRefreshState = LiveContentRefreshState.Idle,
    ) = CaibaoUiState(
        document = document(),
        imageBytes = byteArrayOf(1),
        refreshState = refreshState,
    )

    private fun document() = CaibaoDocument(
        issue = "2026204",
        edition = "A11",
        title = "牛彩网 - 彩吧彩报第三版",
        sourcePageUrl = "https://m.cz89.com/tuku/A11.htm",
        imageUrl = "https://tuku.cz89.com/ftp/app/2026204/A11.jpg",
        localFileName = "2026204-A11-test.jpg",
        sha256 = "A".repeat(64),
        mimeType = "image/jpeg",
        width = 640,
        height = 1280,
        cachedLocalDate = LocalDate.parse("2026-07-31"),
        fetchedAtEpochMillis = 1L,
    )

    private fun testImage() =
        Bitmap.createBitmap(32, 64, Bitmap.Config.ARGB_8888).asImageBitmap()
}
