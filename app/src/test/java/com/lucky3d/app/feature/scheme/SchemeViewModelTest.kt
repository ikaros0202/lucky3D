package com.lucky3d.app.feature.scheme

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.MainDispatcherRule
import com.lucky3d.app.data.repository.SaveSchemeRequest
import com.lucky3d.app.data.repository.SaveTemplateRequest
import com.lucky3d.app.data.repository.SavedScheme
import com.lucky3d.app.data.repository.SavedTemplate
import com.lucky3d.app.data.repository.SchemeRepository
import com.lucky3d.app.data.repository.SchemeWithReplay
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.backtest.BacktestReport
import com.lucky3d.app.domain.backtest.BacktestResult
import com.lucky3d.app.domain.backtest.BacktestStatus
import com.lucky3d.app.domain.filter.PlayType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SchemeViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `loads saved content and exposes deterministic backtest report`() = runTest {
        val fake = FakeSchemeRepository()
        val viewModel = SchemeViewModel(fake)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.schemes).hasSize(1)
        assertThat(viewModel.uiState.value.templates).hasSize(1)

        viewModel.selectTemplate("template-1")
        viewModel.setBacktestRange("2026001", "2026002")
        viewModel.runBacktest()
        advanceUntilIdle()

        assertThat(fake.backtestCalls).containsExactly("template-1:2026001:2026002")
        assertThat(viewModel.uiState.value.backtest?.eligibleCount).isEqualTo(1)
        assertThat(viewModel.uiState.value.backtest?.averageBetCount).isEqualTo(1.0)
    }

    private class FakeSchemeRepository : SchemeRepository {
        private val template = SavedTemplate(
            id = "template-1",
            name = "和值跨度",
            playType = PlayType.STRAIGHT,
            conditions = emptyList(),
            observationWindow = 1,
            ruleVersion = 1,
            updatedAtEpochMillis = 1,
        )
        private val scheme = SavedScheme(
            id = "scheme-1",
            issue = "2026002",
            title = "测试方案",
            observationWindow = 1,
            templateId = template.id,
            playType = PlayType.STRAIGHT,
            conditions = emptyList(),
            candidates = listOf(DrawNumber.parse("007")),
            betCount = 1,
            multiplier = 1,
            amountYuan = 2,
            note = "备注",
            ruleVersion = 1,
            isDrawn = false,
            copiedFromSchemeId = null,
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
        )
        override val schemes = MutableStateFlow(listOf(SchemeWithReplay(scheme, null)))
        override val templates = MutableStateFlow(listOf(template))
        val backtestCalls = mutableListOf<String>()

        override suspend fun saveTemplate(request: SaveTemplateRequest): SavedTemplate = template

        override suspend fun saveScheme(request: SaveSchemeRequest): SavedScheme = scheme

        override suspend fun copyScheme(schemeId: String, newIssue: String): SavedScheme =
            scheme.copy(id = "copy", issue = newIssue, copiedFromSchemeId = schemeId)

        override suspend fun updateNote(schemeId: String, note: String): SavedScheme =
            scheme.copy(note = note)

        override suspend fun runBacktest(
            templateId: String,
            startIssue: String,
            endIssue: String,
        ): BacktestReport {
            backtestCalls += "$templateId:$startIssue:$endIssue"
            return BacktestReport(
                templateId = templateId,
                ruleVersion = 1,
                results = listOf(
                    BacktestResult(
                        targetIssue = endIssue,
                        status = BacktestStatus.EVALUATED,
                        candidateCount = 1,
                        covered = true,
                        candidates = listOf(DrawNumber.parse("007")),
                        amountYuan = 2,
                    ),
                ),
                eligibleCount = 1,
                coveredCount = 1,
                coverageRate = 1.0,
                averageBetCount = 1.0,
                cumulativeAmountYuan = 2,
            )
        }
    }
}
