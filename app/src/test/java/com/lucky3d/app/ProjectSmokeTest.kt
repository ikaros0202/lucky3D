package com.lucky3d.app

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProjectSmokeTest {
    @Test
    fun testInfrastructureRuns() {
        assertThat("Lucky3D").isNotEmpty()
    }
}
