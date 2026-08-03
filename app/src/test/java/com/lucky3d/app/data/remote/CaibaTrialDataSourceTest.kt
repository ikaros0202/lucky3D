package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class CaibaTrialDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var redirectServer: MockWebServer
    private lateinit var fixture: String

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        redirectServer = MockWebServer().also { it.start() }
        fixture = checkNotNull(javaClass.classLoader?.getResource("fixtures/caiba-trial-list.html")).readText()
    }

    @After
    fun tearDown() {
        server.shutdown()
        redirectServer.shutdown()
    }

    @Test
    fun `production endpoint is fixed`() {
        assertThat(CaibaTrialDataSource.DEFAULT_ENDPOINT)
            .isEqualTo("https://www.55125.cn/3dshijihao/list-80.htm")
    }

    @Test
    fun `latest and history share the approved bounded page`() = runTest {
        repeat(2) {
            server.enqueue(MockResponse().setBody(fixture).setHeader("Content-Type", "text/html; charset=UTF-8"))
        }
        val source = CaibaTrialDataSource(OkHttpClient(), server.url("/3dshijihao/list-80.htm"))
        val first = TrialRemoteRecord("2026205", "007", LocalDate.parse("2026-08-03"))

        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Success(first))
        assertThat(source.fetchHistoryPage(1)).isEqualTo(
            TrialRemoteHistoryResult.Success(
                1,
                listOf(first, TrialRemoteRecord("2026204", "219", LocalDate.parse("2026-08-02"))),
            ),
        )
        assertThat(server.takeRequest().path).isEqualTo("/3dshijihao/list-80.htm")
        assertThat(server.takeRequest().path).isEqualTo("/3dshijihao/list-80.htm")
    }

    @Test
    fun `unsupported history page is rejected without network`() = runTest {
        val source = CaibaTrialDataSource(OkHttpClient(), server.url("/3dshijihao/list-80.htm"))

        assertThat(source.fetchHistoryPage(2)).isEqualTo(
            TrialRemoteHistoryResult.Failure(2, LiveContentRemoteFailure.InvalidSource),
        )
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `http empty oversized charset and redirect are typed failures`() = runTest {
        val source = CaibaTrialDataSource(OkHttpClient(), server.url("/3dshijihao/list-80.htm"))
        server.enqueue(MockResponse().setResponseCode(503))
        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.Http))
        server.enqueue(MockResponse().setBody(""))
        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        server.enqueue(MockResponse().setBody("x").setHeader("Content-Length", CaibaTrialDataSource.MAX_HTML_BYTES + 1))
        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.TooLarge))
        server.enqueue(MockResponse().setBody(fixture).setHeader("Content-Type", "text/html; charset=GBK"))
        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", redirectServer.url("/other")))
        assertThat(source.fetchLatest()).isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidSource))
        assertThat(redirectServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `cancellation and timeout keep their distinct behavior`() = runTest {
        server.enqueue(MockResponse().setBody(fixture))
        val cancelling = CaibaTrialDataSource(
            OkHttpClient(),
            server.url("/3dshijihao/list-80.htm"),
            TrialHtmlParser { throw CancellationException("stop") },
        )
        var cancellation: CancellationException? = null
        try {
            cancelling.fetchLatest()
        } catch (error: CancellationException) {
            cancellation = error
        }
        assertThat(cancellation).isNotNull()

        server.enqueue(MockResponse().setBodyDelay(1, TimeUnit.SECONDS).setBody(fixture))
        val timeoutClient = OkHttpClient.Builder().callTimeout(100, TimeUnit.MILLISECONDS).build()
        assertThat(
            CaibaTrialDataSource(
                timeoutClient,
                server.url("/3dshijihao/list-80.htm"),
            ).fetchLatest(),
        ).isEqualTo(TrialRemoteResult.Failure(LiveContentRemoteFailure.Network))
    }

    @Test
    fun `cancelling in flight call reaches caller`() = runTest {
        server.enqueue(MockResponse().setBodyDelay(5, TimeUnit.SECONDS).setBody(fixture))
        val deferred = async(Dispatchers.Default) {
            CaibaTrialDataSource(OkHttpClient(), server.url("/3dshijihao/list-80.htm")).fetchLatest()
        }
        assertThat(server.takeRequest(1, TimeUnit.SECONDS)).isNotNull()

        deferred.cancel()
        var cancellation: CancellationException? = null
        try {
            deferred.await()
        } catch (error: CancellationException) {
            cancellation = error
        }
        assertThat(cancellation).isNotNull()
    }
}
