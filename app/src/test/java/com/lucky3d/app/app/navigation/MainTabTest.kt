package com.lucky3d.app.app.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MainTabTest {
    @Test
    fun `five tabs use the approved order and labels`() {
        assertThat(MainTab.entries.map(MainTab::label))
            .containsExactly("首页", "走势", "选号", "方案", "彩报")
            .inOrder()
    }
}
