package com.lucky3d.app.data.mapper

import com.lucky3d.app.core.model.DrawRecord
import com.lucky3d.app.data.local.DrawEntity
import com.lucky3d.app.domain.attributes.DrawNumber

fun DrawEntity.toRecord(): DrawRecord = DrawRecord(
    issue = issue,
    drawDate = drawDate,
    number = DrawNumber.of(hundreds, tens, ones),
    officialDetailUrl = officialDetailUrl,
    officialFingerprint = officialFingerprint,
    salesAmountYuan = salesAmountYuan,
)
