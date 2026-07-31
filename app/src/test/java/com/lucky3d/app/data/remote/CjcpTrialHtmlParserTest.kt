package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CjcpTrialHtmlParserTest {
    private val fixture = checkNotNull(javaClass.classLoader?.getResource("fixtures/cjcp-trial-history.html")).readText()
    private val parser = CjcpTrialHtmlParser()

    @Test
    fun `header-directed row skips empty current record and preserves split leading zero`() {
        assertThat(parser.parse(fixture)).isEqualTo(
            RemoteParseResult.Success(TrialRemoteRecord(issue = "2026201", number = "007")),
        )
    }

    @Test
    fun `tags whitespace and entities do not change the first record`() {
        val html = fixture.replace("<b>07</b>", "\n <em>&#48;7</em> ")

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
        val missing = fixture.replace("<b>07</b>", "")
        val conflict = fixture.replace("<b>07</b>", "07<td>008</td>")
        val missingColumn = fixture.replace("<th>试机号</th>", "<th>开奖号</th>")

        assertThat(parser.parse(missing))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        assertThat(parser.parse(conflict))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        assertThat(parser.parse(missingColumn))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
    }

    @Test
    fun `invalid number empty document and oversized document are rejected`() {
        assertThat(parser.parse(fixture.replace("<b>07</b>", "2A")))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        assertThat(parser.parse(""))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        assertThat(parser.parse("x".repeat(CjcpTrialHtmlParser.MAX_HTML_BYTES + 1)))
            .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.TooLarge))
    }

    @Test
    fun `only a positive trial simulation notice block is accepted`() {
        val unrelated = fixture.replace("试机号由彩经网模拟数据生成，仅供参考。", "<script>试机号模拟数据</script><p>其他栏目模拟数据</p>")
        val negated = fixture.replace("试机号由彩经网模拟数据生成，仅供参考。", "试机号不是模拟数据。")

        assertThat(parser.parse(unrelated)).isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidSource))
        assertThat(parser.parse(negated)).isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidSource))
    }
}
