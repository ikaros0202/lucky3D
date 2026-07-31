package com.lucky3d.app.data.remote

import java.net.URI

class Cz89CaibaoHtmlParser : CaibaoHtmlParser {
    override fun parse(html: String): RemoteParseResult<CaibaoRemoteDescriptor> {
        if (html.toByteArray(Charsets.UTF_8).size > MAX_HTML_BYTES) return invalid()
        val title = TITLE.find(html)?.groupValues?.get(1)?.let(::normalize) ?: return invalid()
        if (!title.contains(COLUMN_TITLE)) return invalid()
        val issues = ISSUE.findAll(normalize(html)).map { it.value }.toSet()
        if (issues.size != 1) return invalid()
        val issue = issues.single()
        val rawImageUrls = IMAGE_SRC.findAll(html)
            .map { it.groupValues[2] }
            .filter { it.contains("A11.jpg") }
            .toList()
        if (rawImageUrls.any { resolve(it) == null }) return invalid()
        val imageUrls = rawImageUrls
            .map { checkNotNull(resolve(it)) }
            .toSet()
        if (imageUrls.size != 1) return invalid()
        val imageUrl = imageUrls.single()
        if (!CaibaoRemoteRules.isApprovedImageUrl(imageUrl, issue)) return invalid()
        return RemoteParseResult.Success(
            CaibaoRemoteDescriptor(
                issue = issue,
                edition = EDITION,
                title = title,
                sourcePageUrl = CaibaoRemoteRules.SOURCE_PAGE_URL,
                imageUrl = imageUrl,
            ),
        )
    }

    private fun resolve(value: String): String? = try {
        URI(CaibaoRemoteRules.SOURCE_PAGE_URL).resolve(value).toASCIIString()
    } catch (_: Exception) {
        null
    }

    private fun normalize(value: String): String =
        value.replace(TAG, " ").replace("&nbsp;", " ").replace(WHITESPACE, " ").trim()

    private fun invalid() = RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload)

    companion object {
        const val MAX_HTML_BYTES = 1024 * 1024
        private const val EDITION = "A11"
        private const val COLUMN_TITLE = "彩吧彩报第三版"
        private val TITLE = Regex("(?is)<title\\b[^>]*>(.*?)</title>")
        private val IMAGE_SRC = Regex("(?is)<img\\b[^>]*?\\bsrc\\s*=\\s*(['\"])(.*?)\\1")
        private val TAG = Regex("(?is)<[^>]+>")
        private val WHITESPACE = Regex("\\s+")
        private val ISSUE = Regex("(?<!\\d)20\\d{5}(?!\\d)")
    }
}
