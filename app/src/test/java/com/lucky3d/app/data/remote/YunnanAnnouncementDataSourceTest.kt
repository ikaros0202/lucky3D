package com.lucky3d.app.data.remote

import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.core.model.YunnanPlayType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

class YunnanAnnouncementDataSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var source: YunnanAnnouncementDataSource

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        source = YunnanAnnouncementDataSource(
            client = OkHttpClient(),
            baseUrl = server.url("/"),
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `recent announcement maps supported plays and payout`() = runTest {
        server.enqueue(MockResponse().setBody(pagePayload("detail-1", "2026213")))
        server.enqueue(MockResponse().setBody(detailPayload("2026213", withPayout = true)))

        val result = source.fetchRecent(1)
        val announcement = (result as YunnanAnnouncementDataResult.Success).announcements.single()

        assertThat(announcement.issue).isEqualTo("2026213")
        assertThat(announcement.drawDate).isEqualTo("2026-08-11")
        assertThat(announcement.number.value).isEqualTo("872")
        assertThat(announcement.salesAmountYuan).isEqualTo(19_051_910L)
        assertThat(announcement.winningTotalYuan).isEqualTo(18_799_307L)
        assertThat(announcement.prizePoolBalanceFen).isEqualTo(0L)
        assertThat(announcement.plays.map { it.playType })
            .containsExactly(YunnanPlayType.SINGLE, YunnanPlayType.GROUP3, YunnanPlayType.GROUP6)
            .inOrder()
        assertThat(announcement.plays[0].winningCount).isEqualTo(12_155L)
        assertThat(announcement.plays[0].payoutCount).isEqualTo(11_029L)
        assertThat(announcement.plays[0].payoutPerBetYuan).isEqualTo(246L)
        assertThat(announcement.hasPayout).isTrue()
        assertThat(announcement.payoutSummary).isNotNull()
    }

    @Test
    fun `2026214 accepts current 组三 and 组六 award names and payout aliases`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                pagePayload(
                    id = "detail-214",
                    issue = "2026214",
                    date = "2026-08-12 00:00:00",
                    numbers = "5,4,5",
                    amount = "18650174",
                    winningPrice = "10971227",
                    nextAmount = "1625075.48",
                    updateTime = "2026-08-13 08:57:54",
                    releaseTime = "2026-08-12 00:00:00",
                ),
            ),
        )
        server.enqueue(MockResponse().setBody(detailPayload2026214()))

        val result = source.fetchRecent(1)
        val announcement = (result as YunnanAnnouncementDataResult.Success).announcements.single()

        assertThat(announcement.issue).isEqualTo("2026214")
        assertThat(announcement.drawDate).isEqualTo("2026-08-12")
        assertThat(announcement.number.value).isEqualTo("545")
        assertThat(announcement.salesAmountYuan).isEqualTo(18_650_174L)
        assertThat(announcement.winningTotalYuan).isEqualTo(10_971_227L)
        assertThat(announcement.prizePoolBalanceFen).isEqualTo(162_507_548L)
        assertThat(announcement.sourceUpdatedAt).isEqualTo("2026-08-13 08:57:54")
        assertThat(announcement.plays.map { it.playType })
            .containsExactly(YunnanPlayType.SINGLE, YunnanPlayType.GROUP3, YunnanPlayType.GROUP6)
            .inOrder()
        assertThat(announcement.plays[0].winningCount).isEqualTo(5_607L)
        assertThat(announcement.plays[0].payoutCount).isEqualTo(4_573L)
        assertThat(announcement.plays[0].payoutPerBetYuan).isEqualTo(460L)
        assertThat(announcement.plays[1].winningCount).isEqualTo(6_426L)
        assertThat(announcement.plays[1].payoutCount).isEqualTo(5_596L)
        assertThat(announcement.plays[1].payoutPerBetYuan).isEqualTo(142L)
        assertThat(announcement.plays[2].winningCount).isEqualTo(0L)
        assertThat(announcement.plays[2].payoutCount).isNull()
        assertThat(announcement.redemptionDeadline).isEqualTo("2026-10-12")
        assertThat(announcement.hasPayout).isTrue()
    }

    @Test
    fun `recent golden samples include 2026213 and 2026212`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                pagePayloads(
                    pageRecord("detail-1", "2026213", "2026-08-11 00:00:00", "8,7,2", "19051910", "18799307"),
                    pageRecord("detail-2", "2026212", "2026-08-10 00:00:00", "5,6,1", "18075076", "15103363"),
                ),
            ),
        )
        server.enqueue(MockResponse().setBody(detailPayload("2026213", withPayout = true)))
        server.enqueue(MockResponse().setBody(detailPayload("2026212", withPayout = true)))

        val result = source.fetchRecent(2) as YunnanAnnouncementDataResult.Success

        assertThat(result.announcements.map { it.issue })
            .containsExactly("2026213", "2026212").inOrder()
        val previous = result.announcements[1]
        assertThat(previous.salesAmountYuan).isEqualTo(18_075_076L)
        assertThat(previous.winningTotalYuan).isEqualTo(15_103_363L)
        assertThat(previous.number.value).isEqualTo("561")
        assertThat(previous.plays[0].winningCount).isEqualTo(8_572L)
        assertThat(previous.plays[0].payoutCount).isEqualTo(7_059L)
        assertThat(previous.plays[0].payoutPerBetYuan).isEqualTo(385L)
        assertThat(previous.plays[2].winningCount).isEqualTo(15_138L)
        assertThat(previous.plays[2].payoutCount).isEqualTo(12_198L)
        assertThat(previous.plays[2].payoutPerBetYuan).isEqualTo(65L)
    }

    @Test
    fun `issue refresh uses exact likeTitle filter`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                pagePayload(
                    "detail-1",
                    "2024291",
                    date = "2024-10-31 00:00:00",
                    numbers = "4,2,6",
                    amount = "11888140",
                    winningPrice = "6167545",
                ),
            ),
        )
        server.enqueue(MockResponse().setBody(detailPayload("2024291", withPayout = false)))

        val result = source.fetchIssue("2024291")

        assertThat(result).isInstanceOf(YunnanAnnouncementDataResult.Success::class.java)
        val request = server.takeRequest()
        assertThat(request.requestUrl?.queryParameter("likeTitle")).isEqualTo("2024291")
        assertThat(request.requestUrl?.queryParameter("size")).isEqualTo("5")
    }

    @Test
    fun `no positive payout leaves payout fields and summary empty`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                pagePayload(
                    "detail-1",
                    "2024291",
                    date = "2024-10-31 00:00:00",
                    numbers = "4,2,6",
                    amount = "11888140",
                    winningPrice = "6167545",
                ),
            ),
        )
        server.enqueue(MockResponse().setBody(detailPayload("2024291", withPayout = false)))

        val result = source.fetchRecent(1)
        val announcement = (result as YunnanAnnouncementDataResult.Success).announcements.single()

        assertThat(announcement.issue).isEqualTo("2024291")
        assertThat(announcement.plays).hasSize(3)
        assertThat(announcement.plays.all { it.payoutCount == null && it.payoutPerBetYuan == null })
            .isTrue()
        assertThat(announcement.hasPayout).isFalse()
        assertThat(announcement.payoutSummary).isNull()
    }

    @Test
    fun `invalid supported play count rejects whole payload`() = runTest {
        server.enqueue(MockResponse().setBody(pagePayload("detail-1", "2026213")))
        server.enqueue(
            MockResponse().setBody(
                detailPayload("2026213", withPayout = true)
                    .replace("\"count\":12155", "\"count\":-1"),
            ),
        )

        assertThat(source.fetchRecent(1))
            .isInstanceOf(YunnanAnnouncementDataResult.InvalidPayload::class.java)
    }

    @Test
    fun `invalid issue date number and amount reject payload`() {
        val validDetail = detailPayload("2026213", withPayout = true)
        listOf(
            pagePayload("detail-1", "202621"),
            pagePayload("detail-1", "2026213", date = "2026-02-30 00:00:00"),
            pagePayload("detail-1", "2026213", numbers = "8,7"),
            pagePayload("detail-1", "2026213", amount = "0"),
        ).forEach { page ->
            val parsed = source.parsePageAndDetail(page, validDetail)
            assertThat(parsed).isInstanceOf(YunnanAnnouncementDataResult.InvalidPayload::class.java)
        }
    }

    @Test
    fun `invalid or mismatched prize pool balance rejects whole payload`() {
        val validPage = pagePayload("detail-1", "2026213", nextAmount = "0")
        val validDetail = detailPayload("2026213", withPayout = true)
        listOf(
            validPage.replace("\"nextAmount\":\"0\"", "\"nextAmount\":\"-1\""),
            validPage.replace("\"nextAmount\":\"0\"", "\"nextAmount\":\"1.234\""),
            validPage.replace("\"nextAmount\":\"0\"", "\"nextAmount\":\"invalid\""),
            validPage.replace("\"nextAmount\":\"0\"", "\"nextAmount\":\"1\""),
            validPage.replace("\"nextAmount\":\"0\",", ""),
        ).forEach { page ->
            assertThat(source.parsePageAndDetail(page, validDetail))
                .isInstanceOf(YunnanAnnouncementDataResult.InvalidPayload::class.java)
        }
        assertThat(
            source.parsePageAndDetail(
                validPage,
                validDetail.replace("\"nextAmount\":\"0\",", ""),
            ),
        ).isInstanceOf(YunnanAnnouncementDataResult.InvalidPayload::class.java)
    }

    @Test
    fun `equivalent prize pool text formats compare by fen`() {
        val page = pagePayload("detail-1", "2026213", nextAmount = "0.00")

        val result = source.parsePageAndDetail(
            page,
            detailPayload("2026213", withPayout = true),
        )

        val announcement = (result as YunnanAnnouncementDataResult.Success).announcement
        assertThat(announcement?.prizePoolBalanceFen).isEqualTo(0L)
    }

    @Test
    fun `page redirect is rejected without contacting the target`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .addHeader("Location", server.url("/redirected")),
        )
        server.enqueue(MockResponse().setBody(pagePayload("detail-1", "2026213")))

        val result = source.fetchRecent(1)

        assertThat(result).isEqualTo(YunnanAnnouncementDataResult.HttpFailure(302))
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `oversized page is rejected before JSON parsing`() = runTest {
        server.enqueue(MockResponse().setBody("x".repeat(2 * 1024 * 1024 + 1)))

        val result = source.fetchRecent(1)

        assertThat(result).isEqualTo(
            YunnanAnnouncementDataResult.InvalidPayload("JSON response exceeds 2097152 bytes"),
        )
        assertThat(server.requestCount).isEqualTo(1)
    }

    private fun pagePayload(
        id: String,
        issue: String,
        date: String = "2026-08-11 00:00:00",
        numbers: String = "8,7,2",
        amount: String = "19051910",
        winningPrice: String = "18799307",
        nextAmount: String = "0",
        updateTime: String = "2026-08-11 21:48:32",
        releaseTime: String = "2026-08-11 00:00:00",
    ) = pagePayloads(
        pageRecord(id, issue, date, numbers, amount, winningPrice, nextAmount, updateTime, releaseTime),
    )

    private fun pagePayloads(vararg records: String) =
        """{"code":200,"success":true,"data":{"records":[${records.joinToString(",")}]}}"""

    private fun pageRecord(
        id: String,
        issue: String,
        date: String,
        numbers: String,
        amount: String,
        winningPrice: String,
        nextAmount: String = "0",
        updateTime: String = "2026-08-11 21:48:32",
        releaseTime: String = "2026-08-11 00:00:00",
    ) = """{"id":"$id","updateTime":"$updateTime","releaseTime":"$releaseTime","prizeLog":{"issueNumber":"$issue","lotteryDrawDate":"$date","amount":"$amount","nextAmount":"$nextAmount","winningNumbers":"$numbers","winningPrice":"$winningPrice","endTimeStr":"\u672c\u671f\u5151\u5956\u622a\u6b62\u65e5\u4e3a2026\u5e7410\u670810\u65e5\u3002"}}"""

    private fun detailPayload(issue: String, withPayout: Boolean): String {
        val payout = when {
            !withPayout -> ""
            issue == "2026212" -> """,
                {"label":"3D","count":7059,"price":"385.00","awardLevel":"\u5355\u9009\u6d3e\u5956","sort":1},
                {"label":"3D","count":0,"price":"65.00","awardLevel":"\u7ec4\u90093\u6d3e\u5956","sort":3},
                {"label":"3D","count":12198,"price":"65.00","awardLevel":"\u7ec4\u90096\u6d3e\u5956","sort":5}"""
            else -> """,
                {"label":"3D","count":11029,"price":"246.00","awardLevel":"\u5355\u9009\u6d3e\u5956","sort":1},
                {"label":"3D","count":0,"price":"60.00","awardLevel":"\u7ec4\u90093\u6d3e\u5956","sort":3},
                {"label":"3D","count":13214,"price":"60.00","awardLevel":"\u7ec4\u90096\u6d3e\u5956","sort":5}"""
        }
        val base = when (issue) {
            "2024291" -> """{"label":"3D","count":4617,"price":"1040.00","awardLevel":"\u5355\u9009","sort":442},{"label":"3D","count":0,"price":"346.00","awardLevel":"\u7ec4\u90093","sort":443},{"label":"3D","count":7834,"price":"173.00","awardLevel":"\u7ec4\u90096","sort":444},{"label":"3D","count":705,"price":"10.00","awardLevel":"1D","sort":445}"""
            "2026212" -> """{"label":"3D","count":8572,"price":"1040.00","awardLevel":"\u5355\u9009","sort":0},{"label":"3D","count":0,"price":"346.00","awardLevel":"\u7ec4\u90093","sort":2},{"label":"3D","count":15138,"price":"173.00","awardLevel":"\u7ec4\u90096","sort":4},{"label":"3D","count":701,"price":"10.00","awardLevel":"1D","sort":6}"""
            else -> """{"label":"3D","count":12155,"price":"1040.00","awardLevel":"\u5355\u9009","sort":0},{"label":"3D","count":0,"price":"346.00","awardLevel":"\u7ec4\u90093","sort":2},{"label":"3D","count":15216,"price":"173.00","awardLevel":"\u7ec4\u90096","sort":4},{"label":"3D","count":629,"price":"10.00","awardLevel":"1D","sort":6}"""
        }
        val date = when (issue) {
            "2024291" -> "2024-10-31 00:00:00"
            "2026212" -> "2026-08-10 00:00:00"
            else -> "2026-08-11 00:00:00"
        }
        val numbers = if (issue == "2024291") "4,2,6" else if (issue == "2026212") "5,6,1" else "8,7,2"
        val amount = if (issue == "2024291") "11888140" else if (issue == "2026212") "18075076" else "19051910"
        val winningPrice = if (issue == "2024291") "6167545" else if (issue == "2026212") "15103363" else "18799307"
        val deadline = if (issue == "2024291") "\u672c\u671f\u5151\u5956\u622a\u6b62\u65e5\u4e3a2024\u5e7412\u670830\u65e5\u3002" else if (issue == "2026212") "\u672c\u671f\u5151\u5956\u622a\u6b62\u65e5\u4e3a2026\u5e7410\u670809\u65e5\u3002" else "\u672c\u671f\u5151\u5956\u622a\u6b62\u65e5\u4e3a2026\u5e7410\u670810\u65e5\u3002"
        return """{"code":200,"success":true,"data":{"id":"detail-1","prizeLog":{"issueNumber":"$issue","lotteryDrawDate":"$date","amount":"$amount","nextAmount":"0","winningNumbers":"$numbers","winningPrice":"$winningPrice","endTimeStr":"$deadline"},"prizeLogDetails":[$base$payout]}}"""
    }

    private fun detailPayload2026214() = """
        {"code":200,"success":true,"data":{"id":"detail-214","updateTime":"2026-08-13 08:57:54","releaseTime":"2026-08-12 00:00:00","prizeLog":{"issueNumber":"2026214","lotteryDrawDate":"2026-08-12 00:00:00","amount":"18650174","nextAmount":"1625075.48","winningNumbers":"5,4,5","winningPrice":"10971227","endTimeStr":"\u672c\u671f\u5151\u5956\u622a\u6b62\u65e5\u4e3a2026\u5e7410\u670812\u65e5\u3002"},"prizeLogDetails":[
            {"label":"3D","count":5607,"price":"1040.00","awardLevel":"\u5355\u9009","sort":0},
            {"label":"3D","count":6426,"price":"346.00","awardLevel":"\u7ec4\u4e09","sort":2},
            {"label":"3D","count":0,"price":"173.00","awardLevel":"\u7ec4\u516d","sort":4},
            {"label":"3D","count":4573,"price":"460.00","awardLevel":"\u5355\u9009\u6d3e\u5956","sort":1},
            {"label":"3D","count":5596,"price":"142.00","awardLevel":"\u7ec4\u4e09\u6d3e\u5956","sort":3},
            {"label":"3D","count":0,"price":"77.00","awardLevel":"\u7ec4\u516d\u6d3e\u5956","sort":5},
            {"label":"3D","count":960,"price":"10.00","awardLevel":"1D","sort":6}
        ]}}
    """.trimIndent()
}
