package com.lucky3d.app.data.remote

import java.time.LocalDate
import java.time.format.DateTimeParseException

class CaibaTrialHtmlParser : TrialHtmlParser {
    override fun parse(html: String): RemoteParseResult<TrialRemoteRecord> =
        when (val result = parseAll(html)) {
            is RemoteParseResult.Success -> result.value.firstOrNull()
                ?.let { RemoteParseResult.Success(it) }
                ?: invalidPayload()
            is RemoteParseResult.Failure -> result
        }

    override fun parseAll(html: String): RemoteParseResult<List<TrialRemoteRecord>> {
        if (html.toByteArray(Charsets.UTF_8).size > MAX_HTML_BYTES) return tooLarge()
        if (html.isBlank()) return invalidPayload()
        val document = html.replace(COMMENT, "").replace(SCRIPT, "")
        val candidates = TABLE.findAll(document).mapNotNull { match ->
            approvedTable(match.groupValues[1])
        }.toList()
        if (candidates.size != 1) return invalidPayload()
        val candidate = candidates.single()
        val records = linkedMapOf<String, TrialRemoteRecord>()
        ROW.findAll(candidate.table).forEach { row ->
            val cells = DATA_CELL.findAll(row.groupValues[1])
                .map { normalize(it.groupValues[1]) }
                .toList()
            if (cells.isEmpty()) return@forEach
            if (cells.size <= candidate.maxRequiredColumn) return invalidPayload()
            val issue = cells[candidate.issueColumn]
            val sourceDateText = cells[candidate.dateColumn]
            val number = normalizeNumber(cells[candidate.trialColumn]) ?: return invalidPayload()
            if (!ISSUE.matches(issue)) return invalidPayload()
            val sourceDate = try {
                LocalDate.parse(sourceDateText)
            } catch (_: DateTimeParseException) {
                return invalidPayload()
            }
            if (issue.take(4) != sourceDate.year.toString()) return invalidPayload()
            val record = TrialRemoteRecord(issue = issue, number = number, sourceDate = sourceDate)
            val previous = records[issue]
            if (previous != null && previous != record) return invalidPayload()
            records[issue] = record
        }
        return records.values.takeIf { it.isNotEmpty() }
            ?.let { RemoteParseResult.Success(it.toList()) }
            ?: invalidPayload()
    }

    private fun approvedTable(table: String): ApprovedTable? {
        val approvedHeaders = ROW.findAll(table).mapNotNull { row ->
            val headers = HEADER_CELL.findAll(row.groupValues[1]).map { header ->
                HeaderCell(
                    text = normalize(header.groupValues[2]),
                    colspan = COLSPAN.find(header.groupValues[1])
                        ?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                )
            }.toList()
            if (headers.isEmpty()) return@mapNotNull null
            val starts = mutableListOf<Int>()
            var cursor = 0
            headers.forEach { header ->
                starts += cursor
                cursor += header.colspan
            }
            val issues = headers.indices.filter { headers[it].text == "期号" }
            val dates = headers.indices.filter { headers[it].text == "日期" }
            val trials = headers.indices.filter { headers[it].text == "试机号" }
            if (issues.size != 1 || dates.size != 1 || trials.size != 1) return@mapNotNull null
            ApprovedTable(
                table = table,
                issueColumn = starts[issues.single()],
                dateColumn = starts[dates.single()],
                trialColumn = starts[trials.single()],
            )
        }.toList()
        return approvedHeaders.singleOrNull()
    }

    private fun normalizeNumber(raw: String): String? {
        if (!RAW_NUMBER.matches(raw)) return null
        return raw.replace(NUMBER_SEPARATOR, "").takeIf(NUMBER::matches)
    }

    private fun normalize(value: String): String =
        decodeEntities(value.replace(TAG, " ")).replace(WHITESPACE, " ").trim()

    private fun decodeEntities(value: String): String = value
        .replace("&nbsp;", " ", ignoreCase = true)
        .replace(NUMERIC_ENTITY) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
        }

    private fun invalidPayload() = RemoteParseResult.Failure(LiveContentRemoteFailure.InvalidPayload)
    private fun tooLarge() = RemoteParseResult.Failure(LiveContentRemoteFailure.TooLarge)

    private data class HeaderCell(val text: String, val colspan: Int)

    private data class ApprovedTable(
        val table: String,
        val issueColumn: Int,
        val dateColumn: Int,
        val trialColumn: Int,
    ) {
        val maxRequiredColumn: Int = maxOf(issueColumn, dateColumn, trialColumn)
    }

    companion object {
        const val MAX_HTML_BYTES = 1024 * 1024
        private val COMMENT = Regex("(?is)<!--.*?-->")
        private val SCRIPT = Regex("(?is)<script\\b[^>]*>.*?</script>")
        private val TABLE = Regex("(?is)<table\\b[^>]*>(.*?)</table>")
        private val ROW = Regex("(?is)<tr\\b[^>]*>(.*?)</tr>")
        private val HEADER_CELL = Regex("(?is)<th\\b([^>]*)>(.*?)</th>")
        private val DATA_CELL = Regex("(?is)<td\\b[^>]*>(.*?)</td>")
        private val COLSPAN = Regex("(?i)colspan\\s*=\\s*[\"']?(\\d+)")
        private val TAG = Regex("(?is)<[^>]+>")
        private val WHITESPACE = Regex("\\s+")
        private val NUMERIC_ENTITY = Regex("&#(\\d+);")
        private val ISSUE = Regex("20\\d{5}")
        private val RAW_NUMBER = Regex("[0-9](?:\\s*[,，]\\s*[0-9]){2}|[0-9]{3}")
        private val NUMBER_SEPARATOR = Regex("[\\s,，]")
        private val NUMBER = Regex("[0-9]{3}")
    }
}
