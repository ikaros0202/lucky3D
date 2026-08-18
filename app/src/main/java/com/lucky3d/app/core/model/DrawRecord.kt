package com.lucky3d.app.core.model

import com.lucky3d.app.domain.attributes.DrawNumber
import com.lucky3d.app.domain.scheme.HistoricalDraw

data class DrawRecord(
    val issue: String,
    val drawDate: String,
    val number: DrawNumber,
    val officialDetailUrl: String,
    val officialFingerprint: String,
    val salesAmountYuan: Long? = null,
) {
    fun asHistoricalDraw(): HistoricalDraw = HistoricalDraw(
        issue = issue,
        number = number,
        officialFingerprint = officialFingerprint,
    )
}
