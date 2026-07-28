package com.lucky3d.app.data.remote

import com.lucky3d.app.domain.attributes.DrawNumber
import java.net.URI
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class OfficialDraw(
    val issue: String,
    val drawDate: LocalDate,
    val number: DrawNumber,
    val detailUrl: String,
    val fingerprint: String,
)

data class OfficialDrawPage(
    val draws: List<OfficialDraw>,
    val total: Int?,
    val pageNumber: Int,
    val pageSize: Int,
)

sealed interface OfficialDataResult {
    data class Success(val page: OfficialDrawPage) : OfficialDataResult
    data object EmptyResponse : OfficialDataResult
    data class HttpFailure(val statusCode: Int) : OfficialDataResult
    data class InvalidPayload(val reason: String) : OfficialDataResult
    data class NetworkFailure(val reason: String) : OfficialDataResult
}

interface OfficialDrawDataSource {
    suspend fun fetchRecent(issueCount: Int = 100): OfficialDataResult

    suspend fun fetchRange(
        issueStart: String,
        issueEnd: String,
        pageNumber: Int,
        pageSize: Int = 100,
    ): OfficialDataResult
}

class OfficialFc3dDataSource(
    private val client: OkHttpClient,
    private val endpoint: HttpUrl = OFFICIAL_ENDPOINT.toHttpUrl(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : OfficialDrawDataSource {
    override suspend fun fetchRecent(issueCount: Int): OfficialDataResult {
        require(issueCount in 1..100) { "issueCount must be between 1 and 100" }
        return execute(
            endpoint.newBuilder()
                .addQueryParameter("name", "3d")
                .addQueryParameter("issueCount", issueCount.toString())
                .build(),
            pageNumber = 1,
            pageSize = issueCount,
        )
    }

    override suspend fun fetchRange(
        issueStart: String,
        issueEnd: String,
        pageNumber: Int,
        pageSize: Int,
    ): OfficialDataResult {
        require(ISSUE_PATTERN.matches(issueStart) && ISSUE_PATTERN.matches(issueEnd))
        require(issueStart <= issueEnd)
        require(pageNumber >= 1)
        require(pageSize in 1..100)
        return execute(
            endpoint.newBuilder()
                .addQueryParameter("name", "3d")
                .addQueryParameter("issueCount", "")
                .addQueryParameter("issueStart", issueStart)
                .addQueryParameter("issueEnd", issueEnd)
                .addQueryParameter("pageNo", pageNumber.toString())
                .addQueryParameter("pageSize", pageSize.toString())
                .addQueryParameter("systemType", "PC")
                .build(),
            pageNumber = pageNumber,
            pageSize = pageSize,
        )
    }

    private suspend fun execute(url: HttpUrl, pageNumber: Int, pageSize: Int): OfficialDataResult =
        withContext(ioDispatcher) {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("Referer", OFFICIAL_REFERER)
                .header("User-Agent", USER_AGENT)
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext OfficialDataResult.HttpFailure(response.code)
                    }
                    val body = response.body.string()
                    parse(body, pageNumber, pageSize)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                OfficialDataResult.NetworkFailure(exception.message ?: exception::class.java.simpleName)
            }
        }

    internal fun parse(body: String, pageNumber: Int, pageSize: Int): OfficialDataResult {
        val payload = try {
            JSON.decodeFromString<OfficialResponseDto>(body)
        } catch (exception: SerializationException) {
            return OfficialDataResult.InvalidPayload("JSON structure is invalid")
        } catch (exception: IllegalArgumentException) {
            return OfficialDataResult.InvalidPayload("JSON payload is invalid")
        }
        if (payload.state != 0) {
            return OfficialDataResult.InvalidPayload("Official state is ${payload.state}")
        }
        if (payload.result.isEmpty()) return OfficialDataResult.EmptyResponse

        val mapped = mutableListOf<OfficialDraw>()
        val issues = mutableSetOf<String>()
        payload.result.forEach { dto ->
            val draw = map(dto)
                ?: return OfficialDataResult.InvalidPayload("Invalid draw record ${dto.code}")
            if (!issues.add(draw.issue)) {
                return OfficialDataResult.InvalidPayload("Duplicate issue ${draw.issue}")
            }
            mapped += draw
        }
        return OfficialDataResult.Success(
            OfficialDrawPage(
                draws = mapped,
                total = payload.total,
                pageNumber = pageNumber,
                pageSize = pageSize,
            ),
        )
    }

    private fun map(dto: OfficialDrawDto): OfficialDraw? {
        if (!ISSUE_PATTERN.matches(dto.code)) return null
        val dateText = DATE_PREFIX.find(dto.date)?.value ?: return null
        val date = try {
            LocalDate.parse(dateText, STRICT_DATE)
        } catch (_: Exception) {
            return null
        }
        val parts = dto.red.split(',')
        if (parts.size != 3 || parts.any { it.length != 1 || !it[0].isDigit() }) return null
        val number = try {
            DrawNumber.parse(parts.joinToString(""))
        } catch (_: IllegalArgumentException) {
            return null
        }
        val detailUrl = resolveOfficialDetailUrl(dto.detailsLink) ?: return null
        val fingerprintInput = "${dto.code}|$dateText|${number.value}|$detailUrl"
        return OfficialDraw(
            issue = dto.code,
            drawDate = date,
            number = number,
            detailUrl = detailUrl,
            fingerprint = sha256(fingerprintInput),
        )
    }

    private fun resolveOfficialDetailUrl(link: String): String? {
        if (link.isBlank()) return null
        val resolved = try {
            URI(OFFICIAL_ORIGIN).resolve(link)
        } catch (_: Exception) {
            return null
        }
        if (resolved.scheme != "https" || resolved.host != OFFICIAL_HOST) return null
        return resolved.toASCIIString()
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02X".format(it) }

    private companion object {
        const val OFFICIAL_ENDPOINT =
            "https://www.cwl.gov.cn/cwl_admin/front/cwlkj/search/kjxx/findDrawNotice"
        const val OFFICIAL_ORIGIN = "https://www.cwl.gov.cn/"
        const val OFFICIAL_HOST = "www.cwl.gov.cn"
        const val OFFICIAL_REFERER = "https://www.cwl.gov.cn/"
        const val USER_AGENT = "Lucky3D Android/0.1"
        val ISSUE_PATTERN = Regex("""\d{7}""")
        val DATE_PREFIX = Regex("""^\d{4}-\d{2}-\d{2}""")
        val STRICT_DATE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT)
        val JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}

@Serializable
private data class OfficialResponseDto(
    val state: Int,
    val result: List<OfficialDrawDto>,
    val total: Int? = null,
)

@Serializable
private data class OfficialDrawDto(
    val code: String,
    @SerialName("date")
    val date: String,
    val red: String,
    val detailsLink: String,
)
