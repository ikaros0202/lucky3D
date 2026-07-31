package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CjcpTrialHtmlParserTest {
    private val fixture = checkNotNull(javaClass.classLoader?.getResource("fixtures/cjcp-trial-history.html")).readText()
    private val parser = CjcpTrialHtmlParser()

    @Test
    fun `first complete trial row preserves leading zeros`() {
        assertThat(parser.parse(fixture)).isEqualTo(
            RemoteParseResult.Success(TrialRemoteRecord(issue = "2026201", number = "007")),
        )
    }

    @Test
    fun `tags whitespace and entities do not change the first record`() {
        val html = fixture.replace("<b>007</b>", "\n <em>&#48;07</em> ")

        assertThat(parser.parse(html)).isEqualTo(
            RemoteParseResult.Success(TrialRemoteRecord(issue = "2026201", number = "007")),
        )
    }

    @Test
    fun `missing simulation provenance is rejected`() {
        assertThat(parser.parse(fixture.replace("模拟数据", "公开数据")))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidSource))
    }

    @Test
    fun `missing or conflicting first row fields are rejected without using later digits`() {
        val missing = fixture.replace("<b>007</b>", "")
        val conflict = fixture.replace("<b>007</b>", "007 试机号 008")
        val missingColumn = fixture.replace("试机号&nbsp;<b>007</b>", "开机号&nbsp;<b>007</b>")

        assertThat(parser.parse(missing))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        assertThat(parser.parse(conflict))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        assertThat(parser.parse(missingColumn))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
    }

    @Test
    fun `invalid number empty document and oversized document are rejected`() {
        assertThat(parser.parse(fixture.replace("<b>007</b>", "12A")))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        assertThat(parser.parse(""))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        assertThat(parser.parse("x".repeat(CjcpTrialHtmlParser.MAX_HTML_BYTES + 1)))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.TooLarge))
    }
}
