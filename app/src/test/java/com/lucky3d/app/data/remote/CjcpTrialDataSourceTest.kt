package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class CjcpTrialDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var redirectServer: MockWebServer
    private lateinit var fixture: String

    @Before fun setUp() { server = MockWebServer().also { it.start() }; redirectServer = MockWebServer().also { it.start() }; fixture = checkNotNull(javaClass.classLoader?.getResource("fixtures/cjcp-trial-history.html")).readText() }
    @After fun tearDown() { server.shutdown(); redirectServer.shutdown() }

    @Test fun `production endpoint is fixed`() { assertThat(CjcpTrialDataSource.DEFAULT_ENDPOINT).isEqualTo("https://m.cjcp.cn/kjhsjh/3dls/") }

    @Test
    fun `successful page uses injected endpoint and parser`() = runTest {
        server.enqueue(MockResponse().setBody(fixture).setHeader("Content-Type", "text/html; charset=UTF-8"))
        val source = CjcpTrialDataSource(OkHttpClient(), server.url("/trial"))

        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Success(TrialRemoteRecord("2026201", "007")))
        assertThat(server.takeRequest().path).isEqualTo("/trial")
    }

    @Test
    fun `http empty oversized and redirected pages are typed failures`() = runTest {
        val source = CjcpTrialDataSource(OkHttpClient(), server.url("/trial"))
        server.enqueue(MockResponse().setResponseCode(503))
        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.Http))
        server.enqueue(MockResponse().setBody(""))
        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        server.enqueue(MockResponse().setBody("x").setHeader("Content-Length", CjcpTrialDataSource.MAX_HTML_BYTES + 1))
        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.TooLarge))
    }

    @Test
    fun `cancellation from the parser is not converted`() = runTest {
        server.enqueue(MockResponse().setBody(fixture))
        val source = CjcpTrialDataSource(OkHttpClient(), server.url("/trial"), TrialHtmlParser { throw CancellationException("stop") })

        var cancellation: CancellationException? = null
        try { source.fetchLatest() } catch (error: CancellationException) { cancellation = error }
        assertThat(cancellation).isNotNull()
    }

    @Test
    fun `redirect to another origin is rejected`() = runTest {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", redirectServer.url("/other")))
        redirectServer.enqueue(MockResponse().setBody(fixture))

        assertThat(CjcpTrialDataSource(OkHttpClient(), server.url("/trial")).fetchLatest())
            .isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidSource))
        assertThat(redirectServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `body timeout is a typed network failure`() = runTest {
        server.enqueue(MockResponse().setBodyDelay(1, TimeUnit.SECONDS).setBody(fixture))
        val client = OkHttpClient.Builder().callTimeout(100, TimeUnit.MILLISECONDS).build()

        assertThat(CjcpTrialDataSource(client, server.url("/trial")).fetchLatest())
            .isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.Network))
    }

    @Test
    fun `non UTF8 HTML declaration is rejected instead of guessing bytes`() = runTest {
        server.enqueue(MockResponse().setBody(fixture).setHeader("Content-Type", "text/html; charset=GBK"))

        assertThat(CjcpTrialDataSource(OkHttpClient(), server.url("/trial")).fetchLatest())
            .isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidPayload))
    }

    @Test
    fun `cancelling an in-flight network fetch reaches the caller as cancellation`() = runTest {
        server.enqueue(MockResponse().setBodyDelay(5, TimeUnit.SECONDS).setBody(fixture))
        val deferred = async(Dispatchers.Default) { CjcpTrialDataSource(OkHttpClient(), server.url("/trial")).fetchLatest() }
        assertThat(server.takeRequest(1, TimeUnit.SECONDS)).isNotNull()

        deferred.cancel()
        var cancellation: CancellationException? = null
        try { deferred.await() } catch (error: CancellationException) { cancellation = error }
        assertThat(cancellation).isNotNull()
    }
}
