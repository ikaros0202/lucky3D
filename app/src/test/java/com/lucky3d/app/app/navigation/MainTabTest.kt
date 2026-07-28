package com.lucky3d.app.app.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MainTabTest {
    @Test
    fun `five tabs use the approved order and resource labels`() {
        assertThat(MainTab.entries.map(MainTab::labelRes))
            .containsExactly(
                com.lucky3d.app.R.string.nav_home,
                com.lucky3d.app.R.string.nav_trend,
                com.lucky3d.app.R.string.nav_pick,
                com.lucky3d.app.R.string.nav_plans,
                com.lucky3d.app.R.string.nav_caibao,
            )
            .inOrder()
    }

    @Test
    fun `tabs expose stable saveable state keys`() {
        assertThat(MainTab.entries.map(MainTab::stateKey))
            .containsExactly("home", "trend", "pick", "plans", "caibao")
            .inOrder()
    }
}
