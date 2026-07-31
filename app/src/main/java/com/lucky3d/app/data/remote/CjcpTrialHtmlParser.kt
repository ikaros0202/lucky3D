package com.lucky3d.app.data.remote

class CjcpTrialHtmlParser : TrialHtmlParser {
    override fun parse(html: String): RemoteParseResult<TrialRemoteRecord> {
        if (html.toByteArray(Charsets.UTF_8).size > MAX_HTML_BYTES) return tooLarge()
        val pageText = normalize(html)
        if (pageText.isBlank()) return invalidPayload()
        if (!SIMULATION_NOTICE.containsMatchIn(pageText)) return invalidSource()
        val firstCandidate = ROW.findAll(html)
            .map { normalize(it.groupValues[2]) }
            .firstOrNull { text -> ISSUE.find(text) != null }
            ?: return invalidPayload()
        val issues = ISSUE.findAll(firstCandidate).map { it.value }.toSet()
        val numbers = TRIAL_NUMBER.findAll(firstCandidate).map { it.groupValues[1] }.toSet()
        if (issues.size != 1 || numbers.size != 1) return invalidPayload()
        val issue = issues.single()
        val number = numbers.single()
        if (!ISSUE.matches(issue) || !NUMBER.matches(number)) return invalidPayload()
        return RemoteParseResult.Success(TrialRemoteRecord(issue, number))
    }

    private fun normalize(value: String): String =
        decodeEntities(value.replace(TAG, " ")).replace(WHITESPACE, " ").trim()

    private fun decodeEntities(value: String): String =
        value.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(NUMERIC_ENTITY) { match -> match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value }

    private fun invalidPayload() = RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload)
    private fun invalidSource() = RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidSource)
    private fun tooLarge() = RemoteParseResult.Failure(LiveContentRemoteFailure.TooLarge)

    companion object {
        const val MAX_HTML_BYTES = 1024 * 1024
        private val ROW = Regex("(?is)<(tr|li|div)\\b[^>]*>(.*?)</\\1>")
        private val TAG = Regex("(?is)<[^>]+>")
        private val WHITESPACE = Regex("\\s+")
        private val NUMERIC_ENTITY = Regex("&#(\\d+);")
        private val ISSUE = Regex("(?<!\\d)20\\d{5}(?!\\d)")
        private val NUMBER = Regex("\\d{3}")
        private val TRIAL_NUMBER = Regex("试机号\\s*[:：]?\\s*([^\\s]+)")
        private val SIMULATION_NOTICE = Regex("模拟数据|模拟生成")
    }
}
