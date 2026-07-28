package com.lucky3d.app.data.mapper

import com.lucky3d.app.data.local.DrawEntity
import com.lucky3d.app.data.remote.OfficialDraw

fun OfficialDraw.toEntity(): DrawEntity = DrawEntity(
    issue = issue,
    drawDate = drawDate.toString(),
    hundreds = number.hundreds,
    tens = number.tens,
    ones = number.ones,
    officialDetailUrl = detailUrl,
    officialFingerprint = fingerprint,
)
