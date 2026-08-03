package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class CaibaTrialHtmlParserTest {
    private val fixture = checkNotNull(
        javaClass.classLoader?.getResource("fixtures/caiba-trial-list.html"),
    ).readText()
    private val parser = CaibaTrialHtmlParser()

    @Test
    fun `approved table preserves full issue date and leading zero`() {
        assertThat(parser.parse(fixture)).isEqualTo(
            RemoteParseResult.Success(
                TrialRemoteRecord(
                    issue = "2026205",
                    sourceDate = LocalDate.parse("2026-08-03"),
                    number = "007",
                ),
            ),
        )
    }

    @Test
    fun `history returns all rows in source order and normalizes Chinese comma`() {
        assertThat(parser.parseAll(fixture)).isEqualTo(
            RemoteParseResult.Success(
                listOf(
                    TrialRemoteRecord("2026205", "007", LocalDate.parse("2026-08-03")),
                    TrialRemoteRecord("2026204", "219", LocalDate.parse("2026-08-02")),
                ),
            ),
        )
    }

    @Test
    fun `identical duplicate is collapsed but conflicting duplicate rejects whole table`() {
        val row = "<tr><td>2026205</td><td>2026-08-03</td><td>0,0,7</td><td>2:1</td><td>7</td><td>7</td><td>1,2,3</td><td>2:1</td><td>6</td><td>2</td></tr>"
        val identical = fixture.replace("</tbody>", "$row</tbody>")
        val conflicting = fixture.replace(
            "</tbody>",
            "${row.replace("0,0,7", "1,1,8")}</tbody>",
        )

        assertThat(parser.parseAll(identical)).isEqualTo(parser.parseAll(fixture))
        assertThat(parser.parseAll(conflicting)).isEqualTo(
            RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload),
        )
    }

    @Test
    fun `missing unique date or trial group rejects page`() {
        val missingDate = fixture.replace("<th rowspan=\"2\">日期</th>", "<th rowspan=\"2\">星期</th>")
        val duplicateTrial = fixture.replace(
            "<th colspan=\"4\">开奖号</th>",
            "<th colspan=\"4\">试机号</th>",
        )

        listOf(missingDate, duplicateTrial).forEach { html ->
            assertThat(parser.parseAll(html)).isEqualTo(
                RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload),
            )
        }
    }

    @Test
    fun `invalid issue date number empty and oversized payload are rejected`() {
        val invalidPages = listOf(
            fixture.replace("2026205", "205"),
            fixture.replace("2026-08-03", "2026-02-31"),
            fixture.replace("0,0,7", "07推荐"),
            "",
        )
        invalidPages.forEach { html ->
            assertThat(parser.parseAll(html)).isEqualTo(
                RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload),
            )
        }
        assertThat(parser.parseAll("x".repeat(CaibaTrialHtmlParser.MAX_HTML_BYTES + 1))).isEqualTo(
            RemoteParseResult.Failure(LiveContentRemoteFailure.TooLarge),
        )
    }

    @Test
    fun `scripts comments and unrelated tables cannot supply approved fields`() {
        val noApprovedTable = fixture.replace("<table class=\"data-tab\">", "<div>")
            .replace("</table>\n</body>", "</div>\n</body>")
        val injected = noApprovedTable.replace(
            "<body>",
            "<body><!-- ${fixture.substringAfter("<body>").substringBeforeLast("</body>")} -->",
        )

        assertThat(parser.parseAll(injected)).isEqualTo(
            RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload),
        )
    }
}
