package com.lucky3d.app.data.remote

class CjcpTrialHtmlParser : TrialHtmlParser {
    override fun parse(html: String): RemoteParseResult<TrialRemoteRecord> {
        if (html.toByteArray(Charsets.UTF_8).size > MAX_HTML_BYTES) return tooLarge()
        if (html.isBlank()) return invalidPayload()
        val document = html.replace(SCRIPT, "")
        if (!hasPositiveSimulationNotice(document)) return invalidSource()
        val table = TABLE.find(document)?.groupValues?.get(1) ?: return invalidPayload()
        val headers = HEADER.findAll(table).map { normalize(it.groupValues[1]) }.toList()
        val issueColumn = headers.indexOfFirst { it.contains("期号") }
        val trialColumn = headers.indexOfFirst { it.contains("试机号") }
        if (issueColumn < 0 || trialColumn < 0) return invalidPayload()
        ROW.findAll(table).forEach { row ->
            val cells = CELL.findAll(row.groupValues[1]).map { normalize(it.groupValues[1]) }.toList()
            if (cells.size <= maxOf(issueColumn, trialColumn)) return@forEach
            val issue = ISSUE.find(cells[issueColumn])?.value ?: return@forEach
            val number = cells[trialColumn].replace(WHITESPACE, "")
            if (ISSUE.matches(issue) && NUMBER.matches(number)) {
                return RemoteParseResult.Success(TrialRemoteRecord(issue, number))
            }
        }
        return invalidPayload()
    }

    private fun hasPositiveSimulationNotice(document: String): Boolean =
        NOTICE_BLOCK.findAll(document).map { normalize(it.groupValues[2]) }.any { text ->
            text.contains("试机号") && SIMULATION_NOTICE.containsMatchIn(text) && !NEGATED_NOTICE.containsMatchIn(text)
        }

    private fun normalize(value: String): String =
        decodeEntities(value.replace(TAG, " ")).replace(WHITESPACE, " ").trim()

    private fun decodeEntities(value: String): String =
        value.replace("&nbsp;", " ").replace(NUMERIC_ENTITY) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
        }

    private fun invalidPayload() = RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload)
    private fun invalidSource() = RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidSource)
    private fun tooLarge() = RemoteParseResult.Failure(LiveContentRemoteFailure.TooLarge)

    companion object {
        const val MAX_HTML_BYTES = 1024 * 1024
        private val SCRIPT = Regex("(?is)<script\\b[^>]*>.*?</script>")
        private val TABLE = Regex("(?is)<table\\b[^>]*>(.*?)</table>")
        private val HEADER = Regex("(?is)<th\\b[^>]*>(.*?)</th>")
        private val ROW = Regex("(?is)<tr\\b[^>]*>(.*?)</tr>")
        private val CELL = Regex("(?is)<td\\b[^>]*>(.*?)</td>")
        private val NOTICE_BLOCK = Regex("(?is)<(p|div|span)\\b[^>]*>(.*?)</\\1>")
        private val TAG = Regex("(?is)<[^>]+>")
        private val WHITESPACE = Regex("\\s+")
        private val NUMERIC_ENTITY = Regex("&#(\\d+);")
        private val ISSUE = Regex("(?<!\\d)20\\d{5}(?!\\d)")
        private val NUMBER = Regex("[0-9]{3}")
        private val SIMULATION_NOTICE = Regex("模拟数据|模拟生成")
        private val NEGATED_NOTICE = Regex("不是\\s*模拟|非\\s*模拟")
    }
}
