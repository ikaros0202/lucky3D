package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
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
}
