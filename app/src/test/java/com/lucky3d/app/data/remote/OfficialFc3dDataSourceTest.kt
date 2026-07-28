package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class OfficialFc3dDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var source: OfficialFc3dDataSource
    private lateinit var fixture: String

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        fixture = checkNotNull(
            javaClass.classLoader?.getResource("fixtures/official-fc3d-response.json"),
        ).readText()
        source = OfficialFc3dDataSource(
            client = OkHttpClient.Builder()
                .callTimeout(2, TimeUnit.SECONDS)
                .build(),
            endpoint = server.url("/findDrawNotice"),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `saved official fixture maps to isolated domain model`() = runTest {
        server.enqueue(MockResponse().setBody(fixture))

        val result = source.fetchRecent(100)

        assertThat(result).isInstanceOf(OfficialDataResult.Success::class.java)
        val draws = (result as OfficialDataResult.Success).page.draws
        assertThat(draws.map { it.issue }).containsExactly("2026198", "2026197").inOrder()
        assertThat(draws.first().drawDate.toString()).isEqualTo("2026-07-27")
        assertThat(draws.first().number.value).isEqualTo("685")
        assertThat(draws.first().detailUrl)
            .isEqualTo("https://www.cwl.gov.cn/c/2026/07/27/662242.shtml")
        assertThat(draws.first().fingerprint).matches("[0-9A-F]{64}")
        val request = server.takeRequest()
        assertThat(request.requestUrl?.queryParameter("name")).isEqualTo("3d")
        assertThat(request.requestUrl?.queryParameter("issueCount")).isEqualTo("100")
    }

    @Test
    fun `empty official result is explicit and not success`() = runTest {
        server.enqueue(MockResponse().setBody("""{"state":0,"result":[]}"""))

        assertThat(source.fetchRecent()).isEqualTo(OfficialDataResult.EmptyResponse)
    }

    @Test
    fun `missing required field rejects whole batch`() = runTest {
        server.enqueue(MockResponse().setBody(fixture.replace("\"red\": \"2,3,2\"", "\"blue\": \"2,3,2\"")))

        assertThat(source.fetchRecent()).isInstanceOf(OfficialDataResult.InvalidPayload::class.java)
    }

    @Test
    fun `invalid number rejects whole batch`() = runTest {
        server.enqueue(MockResponse().setBody(fixture.replace("\"red\": \"2,3,2\"", "\"red\": \"2,13,2\"")))

        assertThat(source.fetchRecent()).isInstanceOf(OfficialDataResult.InvalidPayload::class.java)
    }

    @Test
    fun `duplicate issue rejects whole batch`() = runTest {
        server.enqueue(MockResponse().setBody(fixture.replace("\"2026197\"", "\"2026198\"")))

        assertThat(source.fetchRecent()).isInstanceOf(OfficialDataResult.InvalidPayload::class.java)
    }

    @Test
    fun `non official detail host rejects whole batch`() = runTest {
        val body = fixture.replace(
            "\"/c/2026/07/26/662035.shtml\"",
            "\"https://example.com/fake.shtml\"",
        )
        server.enqueue(MockResponse().setBody(body))

        assertThat(source.fetchRecent()).isInstanceOf(OfficialDataResult.InvalidPayload::class.java)
    }

    @Test
    fun `http failure is classified without parsing body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody(fixture))

        assertThat(source.fetchRecent()).isEqualTo(OfficialDataResult.HttpFailure(503))
    }

    @Test
    fun `range request carries explicit page boundary`() = runTest {
        server.enqueue(MockResponse().setBody(fixture.replace("\"state\": 0,", "\"state\": 0,\n  \"total\": 202,")))

        val result = source.fetchRange("2026001", "2026198", pageNumber = 2)

        assertThat(result).isInstanceOf(OfficialDataResult.Success::class.java)
        val page = (result as OfficialDataResult.Success).page
        assertThat(page.total).isEqualTo(202)
        assertThat(page.pageNumber).isEqualTo(2)
        val request = server.takeRequest()
        assertThat(request.requestUrl?.queryParameter("issueStart")).isEqualTo("2026001")
        assertThat(request.requestUrl?.queryParameter("issueEnd")).isEqualTo("2026198")
        assertThat(request.requestUrl?.queryParameter("pageNo")).isEqualTo("2")
        assertThat(request.requestUrl?.queryParameter("pageSize")).isEqualTo("100")
    }
}
