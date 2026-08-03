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

class CaibaTrialDataSource(
    private val client: OkHttpClient,
    private val endpoint: HttpUrl = DEFAULT_ENDPOINT.toHttpUrl(),
    private val parser: TrialHtmlParser = CaibaTrialHtmlParser(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TrialDataSource {
    private val noRedirectClient = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override suspend fun fetchLatest(): TrialRemoteResult = when (val result = fetchHistoryPage(1)) {
        is TrialRemoteHistoryResult.Success -> result.records.firstOrNull()
            ?.let(TrialRemoteResult::Success)
            ?: TrialRemoteResult.Failure(LiveContentRemoteFailure.InvalidPayload)
        is TrialRemoteHistoryResult.Failure -> TrialRemoteResult.Failure(result.failure)
    }

    override suspend fun fetchHistoryPage(page: Int): TrialRemoteHistoryResult {
        if (page != 1) {
            return TrialRemoteHistoryResult.Failure(page, LiveContentRemoteFailure.InvalidSource)
        }
        return withContext(ioDispatcher) {
            val request = Request.Builder().url(endpoint).header("Accept", "text/html").build()
            try {
                noRedirectClient.newCall(request).execute().use { response ->
                    if (response.code in 300..399) {
                        return@withContext TrialRemoteHistoryResult.Failure(
                            page,
                            LiveContentRemoteFailure.InvalidSource,
                        )
                    }
                    if (!response.isSuccessful) {
                        return@withContext TrialRemoteHistoryResult.Failure(
                            page,
                            LiveContentRemoteFailure.Http,
                        )
                    }
                    if (!response.hasUtf8HtmlCharset() || !response.hasHtmlContentType()) {
                        return@withContext TrialRemoteHistoryResult.Failure(
                            page,
                            LiveContentRemoteFailure.InvalidPayload,
                        )
                    }
                    if (!samePage(response.request.url, request.url)) {
                        return@withContext TrialRemoteHistoryResult.Failure(
                            page,
                            LiveContentRemoteFailure.InvalidSource,
                        )
                    }
                    when (val body = response.body.readUtf8Bounded(MAX_HTML_BYTES)) {
                        is BoundedRead.TooLarge -> TrialRemoteHistoryResult.Failure(
                            page,
                            LiveContentRemoteFailure.TooLarge,
                        )
                        is BoundedRead.Value -> when (val parsed = parser.parseAll(body.value)) {
                            is RemoteParseResult.Success -> TrialRemoteHistoryResult.Success(page, parsed.value)
                            is RemoteParseResult.Failure -> TrialRemoteHistoryResult.Failure(page, parsed.failure)
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                TrialRemoteHistoryResult.Failure(page, LiveContentRemoteFailure.Network)
            }
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://www.55125.cn/3dshijihao/list-80.htm"
        const val MAX_HTML_BYTES = CaibaTrialHtmlParser.MAX_HTML_BYTES
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
    val charset = CHARSET.find(header("Content-Type").orEmpty())
        ?.groupValues?.get(1)?.trim()?.trim('"')
    return charset == null || charset.equals("utf-8", ignoreCase = true) ||
        charset.equals("utf8", ignoreCase = true)
}

internal fun Response.hasHtmlContentType(): Boolean {
    val value = header("Content-Type") ?: return true
    return value.substringBefore(';').trim().equals("text/html", ignoreCase = true)
}

private val CHARSET = Regex("(?i)charset\\s*=\\s*([^;]+)")
