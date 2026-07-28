package com.lucky3d.app.domain.replay

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InAppReminderCoordinatorTest {
    @Test
    fun `disabled reminder category does not affect other categories`() {
        val events = listOf(
            ReminderEvent("2026199", ReminderType.DRAW, "latest", "新开奖号"),
            ReminderEvent("2026199", ReminderType.REPLAY, "scheme-1", "复盘完成"),
            ReminderEvent("2026199", ReminderType.OMISSION, "HUNDREDS:0", "遗漏达到阈值"),
        )
        val preferences = ReminderPreferences(
            drawEnabled = false,
            replayEnabled = true,
            omissionEnabled = true,
        )

        val decision = InAppReminderCoordinator.evaluate(events, preferences, emptySet())

        assertThat(decision.toShow.map(ReminderEvent::type))
            .containsExactly(ReminderType.REPLAY, ReminderType.OMISSION)
            .inOrder()
    }

    @Test
    fun `same issue type and subject is delivered only once`() {
        val event = ReminderEvent("2026199", ReminderType.REPLAY, "scheme-1", "复盘完成")
        val first = InAppReminderCoordinator.evaluate(
            listOf(event),
            ReminderPreferences(),
            emptySet(),
        )
        val second = InAppReminderCoordinator.evaluate(
            listOf(event, event),
            ReminderPreferences(),
            first.deliveredKeys,
        )

        assertThat(first.toShow).containsExactly(event)
        assertThat(second.toShow).isEmpty()
        assertThat(second.deliveredKeys).containsExactly(event.key)
    }

    @Test
    fun `same event type can be delivered for a new issue`() {
        val old = ReminderEvent("2026198", ReminderType.DRAW, "latest", "旧开奖")
        val next = ReminderEvent("2026199", ReminderType.DRAW, "latest", "新开奖")

        val decision = InAppReminderCoordinator.evaluate(
            listOf(next),
            ReminderPreferences(),
            setOf(old.key),
        )

        assertThat(decision.toShow).containsExactly(next)
    }
}
