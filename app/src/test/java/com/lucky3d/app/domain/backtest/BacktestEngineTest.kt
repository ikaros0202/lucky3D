package com.lucky3d.app.domain.backtest

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.filter.PlayType
import com.lucky3d.app.domain.scheme.FilterTemplate
import com.lucky3d.app.domain.scheme.HistoricalDraw
import org.junit.Test

class BacktestEngineTest {
    @Test
    fun `insufficient samples do not enter coverage denominator`() {
        val draws = listOf(
            draw("2026001", "001"),
            draw("2026002", "002"),
            draw("2026003", "003"),
            draw("2026004", "004"),
        )
        val template = FilterTemplate(
            id = "template-1",
            name = "three draw warmup",
            playType = PlayType.STRAIGHT,
            conditions = emptyList(),
            observationWindow = 3,
            ruleVersion = 1,
        )

        val report = BacktestEngine().run(template, draws)

        assertThat(report.results.take(3).map { it.status })
            .containsExactly(
                BacktestStatus.INSUFFICIENT_SAMPLE,
                BacktestStatus.INSUFFICIENT_SAMPLE,
                BacktestStatus.INSUFFICIENT_SAMPLE,
            )
            .inOrder()
        assertThat(report.eligibleCount).isEqualTo(1)
        assertThat(report.coveredCount).isEqualTo(1)
        assertThat(report.coverageRate).isEqualTo(1.0)
        assertThat(report.averageBetCount).isEqualTo(1000.0)
        assertThat(report.cumulativeAmountYuan).isEqualTo(2000)
        assertThat(report.results.last().candidates).hasSize(1000)
    }

    @Test
    fun `candidate generation never receives target or future draws`() {
        val draws = listOf(
            draw("2026001", "001"),
            draw("2026002", "002"),
            draw("2026003", "003"),
            draw("2026004", "004"),
        )
        val seenHistoryByTarget = linkedMapOf<String, List<String>>()
        val engine = BacktestEngine { history, _, target ->
            seenHistoryByTarget[target.issue] = history.map(HistoricalDraw::issue)
            listOf(target.number)
        }

        engine.run(
            template = FilterTemplate("t", "no leak", PlayType.STRAIGHT, emptyList(), 1, 1),
            draws = draws,
        )

        assertThat(seenHistoryByTarget["2026002"]).containsExactly("2026001")
        assertThat(seenHistoryByTarget["2026003"]).containsExactly("2026001", "2026002").inOrder()
        assertThat(seenHistoryByTarget["2026004"])
            .containsExactly("2026001", "2026002", "2026003")
            .inOrder()
    }

    @Test
    fun `same template data and rule version are reproducible`() {
        val draws = (1..5).map { draw("202600$it", "00$it") }
        val template = FilterTemplate("stable", "stable", PlayType.STRAIGHT, emptyList(), 2, 1)
        val engine = BacktestEngine()

        assertThat(engine.run(template, draws)).isEqualTo(engine.run(template, draws))
    }

    @Test
    fun `default static conditions reuse the normalized candidate snapshot`() {
        val draws = (1..5).map { draw("202600$it", "00$it") }
        val template = FilterTemplate("static", "static", PlayType.STRAIGHT, emptyList(), 1, 1)

        val evaluated = BacktestEngine().run(template, draws).results
            .filter { it.status == BacktestStatus.EVALUATED }

        assertThat(evaluated).hasSize(4)
        assertThat(evaluated[0].candidates).isSameInstanceAs(evaluated[1].candidates)
        assertThat(evaluated[1].candidates).isSameInstanceAs(evaluated[2].candidates)
    }

    private fun draw(issue: String, number: String) = HistoricalDraw(
        issue = issue,
        number = DrawNumber.parse(number),
        officialFingerprint = "$issue:$number",
    )
}
