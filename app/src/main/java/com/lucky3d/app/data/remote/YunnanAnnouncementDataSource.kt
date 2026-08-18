package com.lucky3d.app.data.remote

import com.lucky3d.app.core.model.YunnanAnnouncement
import com.lucky3d.app.core.model.YunnanPlayAnnouncement
import com.lucky3d.app.core.model.YunnanPlayType
import com.lucky3d.app.domain.attributes.DrawNumber
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest

interface YunnanOfficialDataSource {
    suspend fun fetchRecent(limit: Int = 5): YunnanAnnouncementDataResult

    suspend fun fetchIssue(issue: String): YunnanAnnouncementDataResult
}

sealed interface YunnanAnnouncementDataResult {
    data class Success(val announcements: List<YunnanAnnouncement>) : YunnanAnnouncementDataResult {
        val announcement: YunnanAnnouncement?
            get() = announcements.singleOrNull()
    }
    data object EmptyResponse : YunnanAnnouncementDataResult
    data class HttpFailure(val statusCode: Int) : YunnanAnnouncementDataResult
    data class InvalidPayload(val reason: String) : YunnanAnnouncementDataResult
    data class NetworkFailure(val reason: String) : YunnanAnnouncementDataResult
}

/** Adapter for the Yunnan welfare lottery's official 3D announcement pages. */
class YunnanAnnouncementDataSource(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl = DEFAULT_BASE_URL.toHttpUrl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: Clock = Clock.systemUTC(),
) : YunnanOfficialDataSource {
    private val noRedirectClient = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override suspend fun fetchRecent(limit: Int): YunnanAnnouncementDataResult {
        require(limit in 1..100) { "limit must be between 1 and 100" }
        return withContext(ioDispatcher) {
            when (val pageResult = requestPage(current = 1, size = limit)) {
                is PageResult.Success -> loadAnnouncements(pageResult.records.take(limit))
                is PageResult.Empty -> YunnanAnnouncementDataResult.EmptyResponse
                is PageResult.Failure -> pageResult.result
            }
        }
    }

    override suspend fun fetchIssue(issue: String): YunnanAnnouncementDataResult {
        if (!ISSUE_PATTERN.matches(issue)) {
            return YunnanAnnouncementDataResult.InvalidPayload("Invalid issue")
        }
        return withContext(ioDispatcher) {
            // The official endpoint supports title filtering. Still require an
            // exact prizeLog issue match so a fuzzy title result cannot leak a
            // neighboring issue into the domain model.
            when (val pageResult = requestPage(current = 1, size = 5, likeTitle = issue)) {
                is PageResult.Success -> {
                    val matches = pageResult.records.filter { it.prizeLog?.issueNumber == issue }
                    when {
                        matches.isEmpty() -> YunnanAnnouncementDataResult.EmptyResponse
                        matches.size > 1 -> YunnanAnnouncementDataResult.InvalidPayload("Duplicate issue $issue")
                        else -> loadAnnouncements(matches)
                    }
                }
                is PageResult.Empty -> YunnanAnnouncementDataResult.EmptyResponse
                is PageResult.Failure -> pageResult.result
            }
        }
    }

    /** Parses a page and one detail response; useful for fixture-level tests. */
    internal fun parsePageAndDetail(
        pageBody: String,
        detailBody: String,
    ): YunnanAnnouncementDataResult {
        val page = parsePageBody(pageBody)
        if (page !is PageResult.Success) return page.asDataResult()
        val record = page.records.singleOrNull()
            ?: return YunnanAnnouncementDataResult.InvalidPayload("Expected one record")
        val detail = parseDetailBody(detailBody)
        if (detail !is DetailResult.Success) return detail.asDataResult()
        return buildAnnouncement(record, detail.record)
            ?.let { YunnanAnnouncementDataResult.Success(listOf(it)) }
            ?: YunnanAnnouncementDataResult.InvalidPayload("Invalid announcement record")
    }

    internal fun parse(pageBody: String, detailBody: String): YunnanAnnouncementDataResult =
        parsePageAndDetail(pageBody, detailBody)

    private suspend fun loadAnnouncements(
        records: List<YunnanAnnouncementRecordDto>,
    ): YunnanAnnouncementDataResult {
        if (records.isEmpty()) return YunnanAnnouncementDataResult.EmptyResponse
        val issues = mutableSetOf<String>()
        val announcements = ArrayList<YunnanAnnouncement>(records.size)
        for (record in records) {
            val issue = record.prizeLog?.issueNumber
                ?: return YunnanAnnouncementDataResult.InvalidPayload("Missing issue")
            if (!issues.add(issue)) {
                return YunnanAnnouncementDataResult.InvalidPayload("Duplicate issue $issue")
            }
            val id = record.id.asStringOrNull()
                ?: return YunnanAnnouncementDataResult.InvalidPayload("Missing detail id")
            when (val detailResult = requestDetail(id)) {
                is DetailResult.Success -> {
                    val announcement = buildAnnouncement(record, detailResult.record)
                        ?: return YunnanAnnouncementDataResult.InvalidPayload("Invalid announcement $issue")
                    announcements += announcement
                }
                is DetailResult.Empty -> return YunnanAnnouncementDataResult.EmptyResponse
                is DetailResult.Failure -> return detailResult.result
            }
        }
        return YunnanAnnouncementDataResult.Success(announcements)
    }

    private suspend fun requestPage(current: Int, size: Int, likeTitle: String? = null): PageResult {
        val url = baseUrl.newBuilder()
            .addPathSegments(PAGE_PATH)
            .addQueryParameter("menuId", MENU_ID)
            .addQueryParameter("layout", "TABLE")
            .addQueryParameter("current", current.toString())
            .addQueryParameter("size", size.toString())
            .apply { likeTitle?.let { addQueryParameter("likeTitle", it) } }
            .build()
        val body = try {
            request(url)
        } catch (exception: HttpStatusException) {
            return PageResult.Failure(YunnanAnnouncementDataResult.HttpFailure(exception.statusCode))
        } catch (exception: InvalidPayloadException) {
            return PageResult.Failure(YunnanAnnouncementDataResult.InvalidPayload(exception.message.orEmpty()))
        } catch (exception: NetworkException) {
            return PageResult.Failure(YunnanAnnouncementDataResult.NetworkFailure(exception.message.orEmpty()))
        } ?: return PageResult.Empty
        return parsePageBody(body)
    }

    private suspend fun requestDetail(id: String): DetailResult {
        val url = baseUrl.newBuilder()
            .addPathSegments(DETAIL_PATH)
            .addQueryParameter("id", id)
            .build()
        val body = try {
            request(url)
        } catch (exception: HttpStatusException) {
            return DetailResult.Failure(YunnanAnnouncementDataResult.HttpFailure(exception.statusCode))
        } catch (exception: InvalidPayloadException) {
            return DetailResult.Failure(YunnanAnnouncementDataResult.InvalidPayload(exception.message.orEmpty()))
        } catch (exception: NetworkException) {
            return DetailResult.Failure(YunnanAnnouncementDataResult.NetworkFailure(exception.message.orEmpty()))
        } ?: return DetailResult.Empty
        return parseDetailBody(body)
    }

    private suspend fun request(url: HttpUrl): String? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Referer", DEFAULT_BASE_URL)
            .header("User-Agent", USER_AGENT)
            .build()
        return try {
            noRedirectClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw HttpStatusException(response.code)
                }
                if (!samePage(response.request.url, request.url)) {
                    throw InvalidPayloadException("Unexpected Yunnan response URL")
                }
                when (val body = response.body.readUtf8Bounded(MAX_JSON_BYTES)) {
                    is BoundedRead.TooLarge -> throw InvalidPayloadException(
                        "JSON response exceeds $MAX_JSON_BYTES bytes",
                    )
                    is BoundedRead.Value -> body.value
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: HttpStatusException) {
            throw exception
        } catch (exception: InvalidPayloadException) {
            throw exception
        } catch (exception: Exception) {
            throw NetworkException(exception.message ?: exception::class.java.simpleName)
        }
    }

    private fun parsePageBody(body: String): PageResult = try {
        val envelope = JSON.decodeFromString<YunnanPageEnvelope>(body)
        if (envelope.code != 200 || envelope.success == false) {
            PageResult.Failure(YunnanAnnouncementDataResult.InvalidPayload("Yunnan page status is invalid"))
        } else {
            val records = envelope.data?.records.orEmpty()
            if (records.isEmpty()) PageResult.Empty else PageResult.Success(records)
        }
    } catch (exception: SerializationException) {
        PageResult.Failure(YunnanAnnouncementDataResult.InvalidPayload("JSON structure is invalid"))
    } catch (exception: IllegalArgumentException) {
        PageResult.Failure(YunnanAnnouncementDataResult.InvalidPayload("JSON payload is invalid"))
    }

    private fun parseDetailBody(body: String): DetailResult = try {
        val envelope = JSON.decodeFromString<YunnanDetailEnvelope>(body)
        if (envelope.code != 200 || envelope.success == false) {
            DetailResult.Failure(YunnanAnnouncementDataResult.InvalidPayload("Yunnan detail status is invalid"))
        } else {
            val record = envelope.data
            if (record?.prizeLog == null) {
                DetailResult.Failure(YunnanAnnouncementDataResult.InvalidPayload("Missing prize log"))
            } else {
                DetailResult.Success(record)
            }
        }
    } catch (exception: SerializationException) {
        DetailResult.Failure(YunnanAnnouncementDataResult.InvalidPayload("JSON structure is invalid"))
    } catch (exception: IllegalArgumentException) {
        DetailResult.Failure(YunnanAnnouncementDataResult.InvalidPayload("JSON payload is invalid"))
    }

    private fun buildAnnouncement(
        page: YunnanAnnouncementRecordDto,
        detail: YunnanAnnouncementRecordDto,
    ): YunnanAnnouncement? {
        val pagePrize = page.prizeLog ?: return null
        val prize = detail.prizeLog ?: return null
        if (pagePrize.issueNumber != prize.issueNumber) return null
        if (pagePrize.lotteryDrawDate != prize.lotteryDrawDate) return null
        if (pagePrize.winningNumbers != prize.winningNumbers) return null
        if (pagePrize.amount != prize.amount) return null
        if (pagePrize.winningPrice != prize.winningPrice) return null
        val issue = prize.issueNumber.takeIf { ISSUE_PATTERN.matches(it) } ?: return null
        val drawDate = parseDate(prize.lotteryDrawDate) ?: return null
        val number = parseNumber(prize.winningNumbers) ?: return null
        val sales = parsePositiveLong(prize.amount) ?: return null
        val winningTotal = parsePositiveLong(prize.winningPrice) ?: return null
        val pagePrizePoolBalanceFen = parseNonNegativeAmountFen(pagePrize.nextAmount) ?: return null
        val prizePoolBalanceFen = parseNonNegativeAmountFen(prize.nextAmount) ?: return null
        if (pagePrizePoolBalanceFen != prizePoolBalanceFen) return null
        val plays = parsePlays(detail.prizeLogDetails) ?: return null
        val deadline = parseDeadline(prize.endTimeStr)
        val sourceUpdatedAt = sequenceOf(detail.updateTime, detail.releaseTime, page.updateTime, page.releaseTime)
            .mapNotNull { it?.takeIf(String::isNotBlank) }
            .firstOrNull()
            .orEmpty()
        val fingerprintInput = buildString {
            append(issue).append('|')
            append(drawDate).append('|')
            append(number.value).append('|')
            append(sales).append('|')
            append(winningTotal).append('|')
            append(prizePoolBalanceFen).append('|')
            append(plays.joinToString { play ->
                listOf(
                    play.playType.name,
                    play.winningCount,
                    play.prizePerBetYuan,
                    play.payoutCount,
                    play.payoutPerBetYuan,
                ).joinToString(":" )
            })
        }
        return YunnanAnnouncement(
            issue = issue,
            drawDate = drawDate,
            number = number,
            salesAmountYuan = sales,
            winningTotalYuan = winningTotal,
            prizePoolBalanceFen = prizePoolBalanceFen,
            plays = plays,
            redemptionDeadline = deadline,
            sourceUpdatedAt = sourceUpdatedAt,
            fetchedAtEpochMillis = clock.millis(),
            fingerprint = sha256(fingerprintInput),
        )
    }

    private fun parsePlays(details: List<YunnanPrizeDetailDto>?): List<YunnanPlayAnnouncement>? {
        if (details == null) return null
        val base = linkedMapOf<YunnanPlayType, YunnanPrizeDetailDto>()
        val payout = linkedMapOf<YunnanPlayType, YunnanPrizeDetailDto>()
        details.forEach { detail ->
            val play = when (detail.awardLevel) {
                "单选" -> YunnanPlayType.SINGLE
                "组选3", "组三" -> YunnanPlayType.GROUP3
                "组选6", "组六" -> YunnanPlayType.GROUP6
                "单选派奖" -> YunnanPlayType.SINGLE
                "组选3派奖", "组三派奖" -> YunnanPlayType.GROUP3
                "组选6派奖", "组六派奖" -> YunnanPlayType.GROUP6
                else -> return@forEach
            }
            if (detail.label != "3D") return null
            val destination = if (detail.awardLevel.endsWith("派奖")) payout else base
            if (destination.put(play, detail) != null) return null
        }
        if (base.keys != YunnanPlayType.entries.toSet()) return null
        return YunnanPlayType.entries.map { playType ->
            val baseDetail = base[playType] ?: return null
            val winningCount = parseNonNegativeLong(baseDetail.count) ?: return null
            val prize = parsePositiveAmount(baseDetail.price) ?: return null
            val payoutDetail = payout[playType]
            val payoutPair = if (payoutDetail == null) {
                null
            } else {
                val payoutCount = parseNonNegativeLong(payoutDetail.count) ?: return null
                val payoutPrice = parsePositiveAmount(payoutDetail.price) ?: return null
                if (payoutCount == 0L) null else payoutCount to payoutPrice
            }
            YunnanPlayAnnouncement(
                playType = playType,
                winningCount = winningCount,
                prizePerBetYuan = prize,
                payoutCount = payoutPair?.first,
                payoutPerBetYuan = payoutPair?.second,
            )
        }
    }

    private fun parseDate(value: String): String? {
        val date = DATE_PREFIX.find(value)?.value ?: return null
        return try {
            LocalDate.parse(date, STRICT_DATE).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun parseNumber(value: String): DrawNumber? {
        val parts = value.split(',')
        if (parts.size != 3 || parts.any { !DIGIT_PATTERN.matches(it) }) return null
        return runCatching { DrawNumber.parse(parts.joinToString(separator = "")) }.getOrNull()
    }

    private fun parseDeadline(value: String?): String? {
        val match = DEADLINE_PATTERN.find(value.orEmpty()) ?: return null
        val date = "${match.groupValues[1]}-${match.groupValues[2].padStart(2, '0')}-${match.groupValues[3].padStart(2, '0')}"
        return try {
            LocalDate.parse(date, STRICT_DATE).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun parsePositiveLong(value: String): Long? {
        if (!INTEGER_PATTERN.matches(value)) return null
        return value.toLongOrNull()?.takeIf { it > 0L }
    }

    private fun parseNonNegativeLong(value: Long): Long? = value.takeIf { it >= 0L }

    private fun parsePositiveAmount(value: String): Long? = try {
        if (!PRICE_PATTERN.matches(value)) return null
        BigDecimal(value).setScale(0, java.math.RoundingMode.UNNECESSARY)
            .longValueExact()
            .takeIf { it > 0L }
    } catch (_: Exception) {
        null
    }

    private fun parseNonNegativeAmountFen(value: String): Long? = try {
        if (!PRICE_PATTERN.matches(value)) return null
        BigDecimal(value)
            .movePointRight(2)
            .setScale(0, java.math.RoundingMode.UNNECESSARY)
            .longValueExact()
            .takeIf { it >= 0L }
    } catch (_: Exception) {
        null
    }

    private fun JsonElement.asStringOrNull(): String? =
        if (this == JsonNull) null else jsonPrimitive.content.takeIf { it.isNotBlank() }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02X".format(Locale.ROOT, it) }

    private sealed interface PageResult {
        data class Success(val records: List<YunnanAnnouncementRecordDto>) : PageResult
        data object Empty : PageResult
        data class Failure(val result: YunnanAnnouncementDataResult) : PageResult

        fun asDataResult(): YunnanAnnouncementDataResult = when (this) {
            is Success -> error("Success is not a failure")
            Empty -> YunnanAnnouncementDataResult.EmptyResponse
            is Failure -> result
        }
    }

    private sealed interface DetailResult {
        data class Success(val record: YunnanAnnouncementRecordDto) : DetailResult
        data object Empty : DetailResult
        data class Failure(val result: YunnanAnnouncementDataResult) : DetailResult

        fun asDataResult(): YunnanAnnouncementDataResult = when (this) {
            is Success -> error("Success is not a failure")
            Empty -> YunnanAnnouncementDataResult.EmptyResponse
            is Failure -> result
        }
    }

    private class HttpStatusException(val statusCode: Int) : Exception()
    private class InvalidPayloadException(message: String) : Exception(message)
    private class NetworkException(message: String) : Exception(message)

    private companion object {
        const val DEFAULT_BASE_URL = "https://www.ynflcp.cn/"
        const val PAGE_PATH = "biz-api/officia/v1/officialContent/page"
        const val DETAIL_PATH = "biz-api/officia/v1/officialContent/getById"
        const val MENU_ID = "2008885218918944770"
        const val USER_AGENT = "Lucky3D Android/1.4"
        const val MAX_JSON_BYTES = 2 * 1024 * 1024
        val ISSUE_PATTERN = Regex("20\\d{5}")
        val DIGIT_PATTERN = Regex("\\d")
        val INTEGER_PATTERN = Regex("\\d{1,15}")
        val PRICE_PATTERN = Regex("\\d{1,15}(?:\\.\\d{1,2})?")
        val DATE_PREFIX = Regex("^\\d{4}-\\d{2}-\\d{2}")
        val DEADLINE_PATTERN = Regex("(\\d{4})年(\\d{1,2})月(\\d{1,2})日")
        val STRICT_DATE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)
        val JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}

@Serializable
private data class YunnanPageEnvelope(
    val code: Int = 0,
    val success: Boolean? = null,
    val data: YunnanPageData? = null,
)

@Serializable
private data class YunnanPageData(
    val records: List<YunnanAnnouncementRecordDto> = emptyList(),
)

@Serializable
private data class YunnanDetailEnvelope(
    val code: Int = 0,
    val success: Boolean? = null,
    val data: YunnanAnnouncementRecordDto? = null,
)

@Serializable
private data class YunnanAnnouncementRecordDto(
    val id: JsonElement = JsonNull,
    val updateTime: String? = null,
    val releaseTime: String? = null,
    val prizeLog: YunnanPrizeLogDto? = null,
    val prizeLogDetails: List<YunnanPrizeDetailDto>? = null,
)

@Serializable
private data class YunnanPrizeLogDto(
    val issueNumber: String,
    val lotteryDrawDate: String,
    val amount: String,
    val nextAmount: String,
    val winningNumbers: String,
    val winningPrice: String,
    val endTimeStr: String? = null,
)

@Serializable
private data class YunnanPrizeDetailDto(
    val label: String,
    val count: Long,
    val price: String,
    val awardLevel: String,
)
