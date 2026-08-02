package com.lucky3d.app.data.remote

import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody

class CjcpTrialDataSource(
    private val client: OkHttpClient,
    private val endpoint: HttpUrl = DEFAULT_ENDPOINT.toHttpUrl(),
    private val parser: TrialHtmlParser = CjcpTrialHtmlParser(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TrialDataSource {
    private val noRedirectClient = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override suspend fun fetchLatest(): TrialRemoteResult = withContext(ioDispatcher) {
        val request = Request.Builder().url(endpoint).header("Accept", "text/html").build()
        try {
            noRedirectClient.newCall(request).execute().use { response ->
                if (response.code in 300..399) return@withContext TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidSource)
                if (!response.isSuccessful) return@withContext TrialRemoteResult.Failure(LiveContentRemoteFailure.Http)
                if (!response.hasUtf8HtmlCharset()) return@withContext TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidPayload)
                if (!samePage(response.request.url, endpoint)) {
                    return@withContext TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidSource)
                }
                when (val body = response.body.readUtf8Bounded(MAX_HTML_BYTES)) {
                    is BoundedRead.TooLarge -> TrialRemoteResult.Failure(LiveContentRemoteFailure.TooLarge)
                    is BoundedRead.Value -> when (val parsed = parser.parse(body.value)) {
                        is RemoteParseResult.Success -> TrialRemoteResult.Success(parsed.value)
                        is RemoteParseResult.Failure -> TrialRemoteResult.Failure(parsed.failure)
                    }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            TrialRemoteResult.Failure(LiveContentRemoteFailure.Network)
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://m.cjcp.cn/kjhsjh/3dls/"
        const val MAX_HTML_BYTES = CjcpTrialHtmlParser.MAX_HTML_BYTES
    }
}

internal sealed interface BoundedRead<out T> {
    data class Value<T>(val value: T) : BoundedRead<T>
    data object TooLarge : BoundedRead<Nothing>
}

internal fun ResponseBody.readUtf8Bounded(limit: Int): BoundedRead<String> =
    when (val bytes = readBytesBounded(limit)) {
        is BoundedRead.TooLarge -> BoundedRead.TooLarge
        is BoundedRead.Value -> BoundedRead.Value(bytes.value.toString(Charsets.UTF_8))
    }

internal fun ResponseBody.readBytesBounded(limit: Int): BoundedRead<ByteArray> {
    if (contentLength() > limit) return BoundedRead.TooLarge
    byteStream().use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            total += read
            if (total > limit) return BoundedRead.TooLarge
            output.write(buffer, 0, read)
        }
        return BoundedRead.Value(output.toByteArray())
    }
}

internal fun samePage(actual: HttpUrl, expected: HttpUrl): Boolean =
    actual.scheme == expected.scheme &&
        actual.host == expected.host &&
        actual.port == expected.port &&
        actual.encodedPath == expected.encodedPath &&
        actual.query == expected.query

internal fun Response.hasUtf8HtmlCharset(): Boolean {
    val charset = CHARSET.find(header("Content-Type").orEmpty())?.groupValues?.get(1)?.trim()?.trim('"')
    return charset == null || charset.equals("utf-8", ignoreCase = true) || charset.equals("utf8", ignoreCase = true)
}

private val CHARSET = Regex("(?i)charset\\s*=\\s*([^;]+)")
