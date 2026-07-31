package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class Cz89CaibaoDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var redirectServer: MockWebServer
    private lateinit var fixture: String

    @Before fun setUp() { server = MockWebServer().also { it.start() }; redirectServer = MockWebServer().also { it.start() }; fixture = checkNotNull(javaClass.classLoader?.getResource("fixtures/cz89-a11.html")).readText() }
    @After fun tearDown() { server.shutdown(); redirectServer.shutdown() }

    @Test fun `production endpoint is fixed`() { assertThat(Cz89CaibaoDataSource.DEFAULT_ENDPOINT).isEqualTo("https://m.cz89.com/tuku/A11.htm") }

    @Test
    fun `descriptor page is downloaded from the supplied page endpoint`() = runTest {
        server.enqueue(MockResponse().setBody(fixture))
        val source = Cz89CaibaoDataSource(OkHttpClient(), server.url("/A11.htm"))

        assertThat(source.fetchLatestDescriptor()).isInstanceOf(CaibaoDescriptorResult.Success::class.java)
        assertThat(server.takeRequest().path).isEqualTo("/A11.htm")
    }

    @Test
    fun `descriptor HTTP empty and oversized responses are typed failures`() = runTest {
        val source = Cz89CaibaoDataSource(OkHttpClient(), server.url("/A11.htm"))
        server.enqueue(MockResponse().setResponseCode(500))
        assertThat(source.fetchLatestDescriptor()).isEqualTo(CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.Http))
        server.enqueue(MockResponse().setBody(""))
        assertThat(source.fetchLatestDescriptor()).isEqualTo(CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        server.enqueue(MockResponse().setBody("x").setHeader("Content-Length", Cz89CaibaoDataSource.MAX_HTML_BYTES + 1))
        assertThat(source.fetchLatestDescriptor()).isEqualTo(CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.TooLarge))
    }

    @Test
    fun `page and image redirects do not contact their target origins`() = runTest {
        val pageSource = Cz89CaibaoDataSource(OkHttpClient(), server.url("/A11.htm"))
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", redirectServer.url("/other")))
        redirectServer.enqueue(MockResponse().setBody(fixture))
        assertThat(pageSource.fetchLatestDescriptor()).isEqualTo(CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.InvalidSource))
        assertThat(redirectServer.requestCount).isEqualTo(0)

        val imageUrl = server.url("/image.jpg").toString()
        val imageSource = Cz89CaibaoDataSource(OkHttpClient(), server.url("/A11.htm"), imageUrlPolicy = { it == imageUrl })
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", redirectServer.url("/image.jpg")))
        assertThat(imageSource.fetchImage(imageUrl)).isEqualTo(CaibaoImageResult.Failure(LiveContentRemoteFailure.InvalidSource))
        assertThat(redirectServer.requestCount).isEqualTo(0)
    }

    @Test
    fun `image content length mime and streamed limit are enforced before success`() = runTest {
        val testUrl = server.url("/image.jpg").toString()
        val source = Cz89CaibaoDataSource(OkHttpClient(), server.url("/A11.htm"), imageUrlPolicy = { it == testUrl })
        server.enqueue(MockResponse().setBody("x").setHeader("Content-Length", Cz89CaibaoDataSource.MAX_IMAGE_BYTES + 1))
        assertThat(source.fetchImage(testUrl)).isEqualTo(CaibaoImageResult.Failure(LiveContentRemoteFailure.TooLarge))
        server.enqueue(MockResponse().setHeader("Content-Type", "image/gif").setBody("image"))
        assertThat(source.fetchImage(testUrl)).isEqualTo(CaibaoImageResult.Failure(LiveContentRemoteFailure.InvalidContentType))
        server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setChunkedBody("x".repeat(Cz89CaibaoDataSource.MAX_IMAGE_BYTES + 1), 4096))
        assertThat(source.fetchImage(testUrl)).isEqualTo(CaibaoImageResult.Failure(LiveContentRemoteFailure.TooLarge))
    }

    @Test
    fun `jpeg image succeeds only after policy and MIME validation`() = runTest {
        val testUrl = server.url("/image.jpg").toString()
        val source = Cz89CaibaoDataSource(OkHttpClient(), server.url("/A11.htm"), imageUrlPolicy = { it == testUrl })
        server.enqueue(MockResponse().setHeader("Content-Type", "image/jpeg").setBody("jpeg"))

        val result = source.fetchImage(testUrl)

        assertThat(result).isInstanceOf(CaibaoImageResult.Success::class.java)
        assertThat((result as CaibaoImageResult.Success).mimeType).isEqualTo("image/jpeg")
        assertThat(result.bytes.toList()).containsExactly('j'.code.toByte(), 'p'.code.toByte(), 'e'.code.toByte(), 'g'.code.toByte()).inOrder()
    }

    @Test
    fun `empty images are invalid payload whether length is absent or zero`() = runTest {
        val testUrl = server.url("/image.jpg").toString()
        val source = Cz89CaibaoDataSource(OkHttpClient(), server.url("/A11.htm"), imageUrlPolicy = { it == testUrl })
        server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setChunkedBody("", 1))
        assertThat(source.fetchImage(testUrl)).isEqualTo(CaibaoImageResult.Failure(LiveContentRemoteFailure.InvalidPayload))
        server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setBody("").setHeader("Content-Length", 0))
        assertThat(source.fetchImage(testUrl)).isEqualTo(CaibaoImageResult.Failure(LiveContentRemoteFailure.InvalidPayload))
    }

    @Test
    fun `page and image timeout cancellation HTTP and empty-body paths are typed`() = runTest {
        val client = OkHttpClient.Builder().callTimeout(100, TimeUnit.MILLISECONDS).build()
        server.enqueue(MockResponse().setBodyDelay(1, TimeUnit.SECONDS).setBody(fixture))
        assertThat(Cz89CaibaoDataSource(client, server.url("/A11.htm")).fetchLatestDescriptor())
            .isEqualTo(CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.Network))

        val imageUrl = server.url("/image.jpg").toString()
        val source = Cz89CaibaoDataSource(client, server.url("/A11.htm"), imageUrlPolicy = { it == imageUrl })
        server.enqueue(MockResponse().setResponseCode(503))
        assertThat(source.fetchImage(imageUrl)).isEqualTo(CaibaoImageResult.Failure(LiveContentRemoteFailure.Http))
        server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setBody(""))
        assertThat(source.fetchImage(imageUrl)).isEqualTo(CaibaoImageResult.Failure(LiveContentRemoteFailure.InvalidPayload))

        server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setBodyDelay(1, TimeUnit.SECONDS).setBody("image"))
        assertThat(source.fetchImage(imageUrl)).isEqualTo(CaibaoImageResult.Failure(LiveContentRemoteFailure.Network))
    }

    @Test
    fun `non UTF8 HTML declarations are rejected instead of guessing bytes`() = runTest {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html; charset=GBK").setBody(fixture))
        assertThat(Cz89CaibaoDataSource(OkHttpClient(), server.url("/A11.htm")).fetchLatestDescriptor())
            .isEqualTo(CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.InvalidPayload))
    }

    @Test
    fun `cancelling an in-flight image fetch reaches the caller as cancellation`() = runTest {
        val imageUrl = server.url("/image.jpg").toString()
        val source = Cz89CaibaoDataSource(OkHttpClient(), server.url("/A11.htm"), imageUrlPolicy = { it == imageUrl })
        server.enqueue(MockResponse().setHeader("Content-Type", "image/png").setBodyDelay(5, TimeUnit.SECONDS).setBody("image"))
        val deferred = async(Dispatchers.Default) { source.fetchImage(imageUrl) }
        assertThat(server.takeRequest(1, TimeUnit.SECONDS)).isNotNull()

        deferred.cancel()
        var cancellation: CancellationException? = null
        try { deferred.await() } catch (error: CancellationException) { cancellation = error }
        assertThat(cancellation).isNotNull()
    }

    @Test
    fun `cancelling an in-flight page fetch uses a clean request queue`() = runTest {
        val cleanServer = MockWebServer().also { it.start() }
        try {
            cleanServer.enqueue(MockResponse().setBodyDelay(5, TimeUnit.SECONDS).setBody(fixture))
            val deferred = async(Dispatchers.Default) {
                Cz89CaibaoDataSource(OkHttpClient(), cleanServer.url("/A11.htm")).fetchLatestDescriptor()
            }
            assertThat(cleanServer.takeRequest(1, TimeUnit.SECONDS)).isNotNull()
            deferred.cancel()
            var cancellation: CancellationException? = null
            try { deferred.await() } catch (error: CancellationException) { cancellation = error }
            assertThat(cancellation).isNotNull()
        } finally {
            cleanServer.shutdown()
        }
    }
}
