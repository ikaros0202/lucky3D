package com.lucky3d.app.app.navigation

import androidx.annotation.StringRes
import com.lucky3d.app.R

enum class MainTab(
    @StringRes val labelRes: Int,
    @StringRes val accessibilityLabelRes: Int,
    val stateKey: String,
) {
    HOME(R.string.nav_home, R.string.nav_home_a11y, "home"),
    TREND(R.string.nav_trend, R.string.nav_trend_a11y, "trend"),
    PICK(R.string.nav_pick, R.string.nav_pick_a11y, "pick"),
    PLANS(R.string.nav_plans, R.string.nav_plans_a11y, "plans"),
    CAIBAO(R.string.nav_caibao, R.string.nav_caibao_a11y, "caibao"),
}
