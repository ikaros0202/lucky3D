package com.lucky3d.app.data.remote

import java.net.URI

data class CaibaoRemoteDescriptor(
    val issue: String,
    val edition: String,
    val title: String,
    val sourcePageUrl: String,
    val imageUrl: String,
)

fun interface CaibaoHtmlParser {
    fun parse(html: String): RemoteParseResult<CaibaoRemoteDescriptor>
}

interface CaibaoDataSource {
    suspend fun fetchLatestDescriptor(): CaibaoDescriptorResult

    suspend fun fetchImage(imageUrl: String): CaibaoImageResult
}

sealed interface CaibaoDescriptorResult {
    data class Success(val descriptor: CaibaoRemoteDescriptor) : CaibaoDescriptorResult
    data class Failure(val failure: LiveContentRemoteFailure) : CaibaoDescriptorResult
}

sealed interface CaibaoImageResult {
    data class Success(val bytes: ByteArray, val mimeType: String) : CaibaoImageResult
    data class Failure(val failure: LiveContentRemoteFailure) : CaibaoImageResult
}

internal object CaibaoRemoteRules {
    const val SOURCE_PAGE_URL = "https://m.cz89.com/tuku/A11.htm"
    const val IMAGE_HOST = "tuku.cz89.com"
    private val ISSUE_PATTERN = Regex("20\\d{5}")

    fun isApprovedImageUrl(value: String, issue: String? = null): Boolean {
        val uri = try {
            URI(value)
        } catch (_: Exception) {
            return false
        }
        val expectedIssue = issue ?: return false
        return uri.scheme == "https" &&
            uri.host == IMAGE_HOST &&
            uri.userInfo == null &&
            (uri.port == -1 || uri.port == 443) &&
            uri.rawQuery == null &&
            uri.rawFragment == null &&
            ISSUE_PATTERN.matches(expectedIssue) &&
            uri.rawPath == "/ftp/app/$expectedIssue/A11.jpg"
    }
}
