package com.lucky3d.app.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class Cz89CaibaoDataSource(
    private val client: OkHttpClient,
    private val endpoint: HttpUrl = DEFAULT_ENDPOINT.toHttpUrl(),
    private val parser: CaibaoHtmlParser = Cz89CaibaoHtmlParser(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    internal val imageUrlPolicy: (String) -> Boolean = { imageUrl ->
        val issue = IMAGE_ISSUE.find(imageUrl)?.groupValues?.get(1)
        issue != null && CaibaoRemoteRules.isApprovedImageUrl(imageUrl, issue)
    },
) : CaibaoDataSource {
    override suspend fun fetchLatestDescriptor(): CaibaoDescriptorResult = withContext(ioDispatcher) {
        val request = Request.Builder().url(endpoint).header("Accept", "text/html").build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.Http)
                if (!samePage(response.request.url, endpoint)) {
                    return@withContext CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.InvalidSource)
                }
                when (val body = response.body.readUtf8Bounded(MAX_HTML_BYTES)) {
                    is BoundedRead.TooLarge -> CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.TooLarge)
                    is BoundedRead.Value -> when (val parsed = parser.parse(body.value)) {
                        is RemoteParseResult.Success -> CaibaoDescriptorResult.Success(parsed.value)
                        is RemoteParseResult.Failure -> CaibaoDescriptorResult.Failure(parsed.failure)
                    }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            CaibaoDescriptorResult.Failure(LiveContentRemoteFailure.Network)
        }
    }

    override suspend fun fetchImage(imageUrl: String): CaibaoImageResult = withContext(ioDispatcher) {
        if (!imageUrlPolicy(imageUrl)) return@withContext CaibaoImageResult.Failure(LiveContentRemoteFailure.InvalidSource)
        val request = Request.Builder().url(imageUrl).header("Accept", "image/jpeg, image/png").build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext CaibaoImageResult.Failure(LiveContentRemoteFailure.Http)
                if (response.request.url != request.url || !imageUrlPolicy(response.request.url.toString())) {
                    return@withContext CaibaoImageResult.Failure(LiveContentRemoteFailure.InvalidSource)
                }
                if (response.body.contentLength() > MAX_IMAGE_BYTES) {
                    return@withContext CaibaoImageResult.Failure(LiveContentRemoteFailure.TooLarge)
                }
                val mimeType = response.body.contentType()?.let { "${it.type}/${it.subtype}" }
                    ?: return@withContext CaibaoImageResult.Failure(LiveContentRemoteFailure.InvalidContentType)
                if (mimeType !in ACCEPTED_IMAGE_TYPES) {
                    return@withContext CaibaoImageResult.Failure(LiveContentRemoteFailure.InvalidContentType)
                }
                when (val body = response.body.readBytesBounded(MAX_IMAGE_BYTES)) {
                    is BoundedRead.TooLarge -> CaibaoImageResult.Failure(LiveContentRemoteFailure.TooLarge)
                    is BoundedRead.Value -> CaibaoImageResult.Success(body.value, mimeType)
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            CaibaoImageResult.Failure(LiveContentRemoteFailure.Network)
        }
    }

    companion object {
        const val DEFAULT_ENDPOINT = CaibaoRemoteRules.SOURCE_PAGE_URL
        const val MAX_HTML_BYTES = Cz89CaibaoHtmlParser.MAX_HTML_BYTES
        const val MAX_IMAGE_BYTES = 8 * 1024 * 1024
        private val IMAGE_ISSUE = Regex("/ftp/app/(20\\d{5})/A11\\.jpg$")
        private val ACCEPTED_IMAGE_TYPES = setOf("image/jpeg", "image/png")
    }
}
