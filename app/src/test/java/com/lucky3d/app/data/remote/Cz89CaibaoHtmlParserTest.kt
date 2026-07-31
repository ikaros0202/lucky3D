package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Cz89CaibaoHtmlParserTest {
    private val fixture = checkNotNull(javaClass.classLoader?.getResource("fixtures/cz89-a11.html")).readText()
    private val parser = Cz89CaibaoHtmlParser()

    @Test
    fun `a11 fixture maps the complete approved descriptor`() {
        assertThat(parser.parse(fixture)).isEqualTo(
            RemoteParseResult.Success(
                CaibaoRemoteDescriptor(
                    issue = "2026201",
                    edition = "A11",
                    title = "2026201期 - 彩吧彩报第三版",
                    sourcePageUrl = Cz89CaibaoDataSource.DEFAULT_ENDPOINT,
                    imageUrl = "https://tuku.cz89.com/ftp/app/2026201/A11.jpg",
                ),
            ),
        )
    }

    @Test
    fun `title edition source and image URL boundary violations are rejected`() {
        val cases = listOf(
            fixture.replace("彩吧彩报第三版", "其他彩报"),
            fixture.replace("A11.jpg", "A12.jpg"),
            fixture.replace("https://", "http://"),
            fixture.replace("tuku.cz89.com", "evil.example"),
            fixture.replace("/ftp/app/2026201/A11.jpg", "/ftp/app/2026200/A11.jpg"),
            fixture.replace("https://tuku.cz89.com", "https://user:pass@tuku.cz89.com"),
            fixture.replace("/ftp/app/2026201/A11.jpg", "/ftp/app/2026201/../A11.jpg"),
        )

        cases.forEach { html ->
            assertThat(parser.parse(html))
                .isEqualTo(RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        }
    }
}
