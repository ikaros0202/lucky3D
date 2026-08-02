package com.lucky3d.app.data.remote

class CjcpTrialHtmlParser : TrialHtmlParser {
    override fun parse(html: String): RemoteParseResult<TrialRemoteRecord> {
        return when (val result = parseAll(html)) {
            is RemoteParseResult.Success -> result.value.firstOrNull()?.let { RemoteParseResult.Success(it) }
                ?: invalidPayload()
            is RemoteParseResult.Failure -> result
        }
    }

    override fun parseAll(html: String): RemoteParseResult<List<TrialRemoteRecord>> {
        if (html.toByteArray(Charsets.UTF_8).size > MAX_HTML_BYTES) return tooLarge()
        if (html.isBlank()) return invalidPayload()
        val document = html.replace(COMMENT, "").replace(SCRIPT, "").replace(HIDDEN_BLOCK, "")
        if (!hasPositiveSimulationNotice(document)) return invalidSource()
        val table = TABLE.find(document)?.groupValues?.get(1) ?: return invalidPayload()
        val headers = HEADER.findAll(table).map { normalize(it.groupValues[1]) }.toList()
        val issueColumns = headers.mapIndexedNotNull { index, header -> index.takeIf { header.contains("期号") } }
        val trialColumns = headers.mapIndexedNotNull { index, header -> index.takeIf { header.contains("试机号") } }
        if (issueColumns.size != 1 || trialColumns.size != 1) return invalidPayload()
        val issueColumn = issueColumns.single()
        val trialColumn = trialColumns.single()
        val records = mutableListOf<TrialRemoteRecord>()
        val issues = mutableSetOf<String>()
        ROW.findAll(table).forEach { row ->
            val cells = CELL.findAll(row.groupValues[1]).map { normalize(it.groupValues[1]) }.toList()
            if (cells.isEmpty()) return@forEach
            if (cells.size <= maxOf(issueColumn, trialColumn)) return invalidPayload()
            val rawIssue = cells[issueColumn]
            val rawNumber = cells[trialColumn]
            if (rawIssue.isBlank() && rawNumber.isBlank()) return@forEach
            val issue = rawIssue.takeIf { ISSUE_CELL.matches(it) }?.removeSuffix("期")
                ?: return invalidPayload()
            if (rawNumber.isBlank()) return@forEach
            val number = rawNumber.replace(WHITESPACE, "")
            if (!NUMBER.matches(number) || !issues.add(issue)) return invalidPayload()
            records += TrialRemoteRecord(issue, number)
        }
        return records.takeIf { it.isNotEmpty() }?.let { RemoteParseResult.Success(it) } ?: invalidPayload()
    }

    private fun hasPositiveSimulationNotice(document: String): Boolean =
        NOTICE_BLOCK.findAll(document).map { normalize(it.groupValues[2]) }.any { text ->
            POSITIVE_NOTICE.containsMatchIn(text) && !NEGATED_OR_AMBIGUOUS.containsMatchIn(text)
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
        private val COMMENT = Regex("(?is)<!--.*?-->")
        private val HIDDEN_BLOCK = Regex("(?is)<(p|div|span)\\b[^>]*(?:hidden|display\\s*:\\s*none)[^>]*>.*?</\\1>")
        private val TABLE = Regex("(?is)<table\\b[^>]*>(.*?)</table>")
        private val HEADER = Regex("(?is)<th\\b[^>]*>(.*?)</th>")
        private val ROW = Regex("(?is)<tr\\b[^>]*>(.*?)</tr>")
        private val CELL = Regex("(?is)<td\\b[^>]*>(.*?)</td>")
        private val NOTICE_BLOCK = Regex("(?is)<(p|div|span)\\b[^>]*>(.*?)</\\1>")
        private val TAG = Regex("(?is)<[^>]+>")
        private val WHITESPACE = Regex("\\s+")
        private val NUMERIC_ENTITY = Regex("&#(\\d+);")
        private val ISSUE_CELL = Regex("20\\d{5}期?")
        private val NUMBER = Regex("[0-9]{3}")
        private val POSITIVE_NOTICE = Regex("(?:试机号\\s*由\\s*彩经网\\s*模拟数据生成|(?:3D开机号和)?试机号\\s*是\\s*彩经网生成的模拟数据)")
        private val NEGATED_OR_AMBIGUOUS = Regex("不|非|没有|未|无法|疑似|可能|或许")
    }
}
