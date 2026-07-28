package com.lucky3d.app.feature.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lucky3d.app.domain.replay.ReminderType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UserPreferencesDataStoreTest {
    @Test
    fun reminderSwitchesAndDeliveredKeysPersistIndependently() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = DataStoreUserPreferencesRepository(context)
        repository.reset()

        repository.setReminderEnabled(ReminderType.DRAW, false)
        repository.setReminderEnabled(ReminderType.REPLAY, true)
        repository.setReminderEnabled(ReminderType.OMISSION, false)
        repository.setDefaultObservationWindow(50)
        repository.markDelivered(setOf("2026199:DRAW:latest"))

        val restored = DataStoreUserPreferencesRepository(context).preferences.first()
        assertThat(restored.drawReminderEnabled).isFalse()
        assertThat(restored.replayReminderEnabled).isTrue()
        assertThat(restored.omissionReminderEnabled).isFalse()
        assertThat(restored.defaultObservationWindow).isEqualTo(50)
        assertThat(restored.deliveredReminderKeys).contains("2026199:DRAW:latest")
    }
}
