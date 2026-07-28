package com.lucky3d.app.domain.replay

enum class ReminderType { DRAW, REPLAY, OMISSION }

data class ReminderEvent(
    val issue: String,
    val type: ReminderType,
    val subject: String,
    val message: String,
) {
    init {
        require(issue.matches(Regex("""\d{7}""")))
        require(subject.isNotBlank())
        require(message.isNotBlank())
    }

    val key: String get() = "$issue:${type.name}:$subject"
}

data class ReminderPreferences(
    val drawEnabled: Boolean = true,
    val replayEnabled: Boolean = true,
    val omissionEnabled: Boolean = true,
)

data class ReminderDecision(
    val toShow: List<ReminderEvent>,
    val deliveredKeys: Set<String>,
)

object InAppReminderCoordinator {
    fun evaluate(
        events: List<ReminderEvent>,
        preferences: ReminderPreferences,
        deliveredKeys: Set<String>,
    ): ReminderDecision {
        val unseen = events
            .distinctBy(ReminderEvent::key)
            .filter { event ->
                event.key !in deliveredKeys && when (event.type) {
                    ReminderType.DRAW -> preferences.drawEnabled
                    ReminderType.REPLAY -> preferences.replayEnabled
                    ReminderType.OMISSION -> preferences.omissionEnabled
                }
            }
        return ReminderDecision(
            toShow = unseen,
            deliveredKeys = deliveredKeys + unseen.map(ReminderEvent::key),
        )
    }
}
