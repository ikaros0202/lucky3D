package com.lucky3d.app.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lucky3d.app.domain.filter.Position
import com.lucky3d.app.domain.replay.ReminderType
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.lucky3dPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "lucky3d_user_preferences",
)

data class UserPreferences(
    val defaultObservationWindow: Int = 30,
    val drawReminderEnabled: Boolean = true,
    val replayReminderEnabled: Boolean = true,
    val omissionReminderEnabled: Boolean = true,
    val omissionPosition: Position = Position.HUNDREDS,
    val omissionDigit: Int = 0,
    val omissionThreshold: Int = 10,
    val deliveredReminderKeys: Set<String> = emptySet(),
)

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setDefaultObservationWindow(window: Int)

    suspend fun setReminderEnabled(type: ReminderType, enabled: Boolean)

    suspend fun setOmissionRule(position: Position, digit: Int, threshold: Int)

    suspend fun markDelivered(keys: Set<String>)

    suspend fun reset()
}

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
) : UserPreferencesRepository {
    private val dataStore = context.lucky3dPreferencesDataStore

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { values ->
            UserPreferences(
                defaultObservationWindow =
                    values[Keys.DEFAULT_OBSERVATION_WINDOW]?.coerceIn(1, 3334) ?: 30,
                drawReminderEnabled = values[Keys.DRAW_REMINDER] ?: true,
                replayReminderEnabled = values[Keys.REPLAY_REMINDER] ?: true,
                omissionReminderEnabled = values[Keys.OMISSION_REMINDER] ?: true,
                omissionPosition = values[Keys.OMISSION_POSITION]
                    ?.let { runCatching { Position.valueOf(it) }.getOrNull() }
                    ?: Position.HUNDREDS,
                omissionDigit = values[Keys.OMISSION_DIGIT]?.coerceIn(0, 9) ?: 0,
                omissionThreshold = values[Keys.OMISSION_THRESHOLD]?.coerceIn(1, 999) ?: 10,
                deliveredReminderKeys = values[Keys.DELIVERED_REMINDERS].orEmpty(),
            )
        }

    override suspend fun setDefaultObservationWindow(window: Int) {
        require(window in 1..3334)
        dataStore.edit { it[Keys.DEFAULT_OBSERVATION_WINDOW] = window }
    }

    override suspend fun setReminderEnabled(type: ReminderType, enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[
                when (type) {
                    ReminderType.DRAW -> Keys.DRAW_REMINDER
                    ReminderType.REPLAY -> Keys.REPLAY_REMINDER
                    ReminderType.OMISSION -> Keys.OMISSION_REMINDER
                }
            ] = enabled
        }
    }

    override suspend fun setOmissionRule(position: Position, digit: Int, threshold: Int) {
        require(digit in 0..9)
        require(threshold in 1..999)
        dataStore.edit {
            it[Keys.OMISSION_POSITION] = position.name
            it[Keys.OMISSION_DIGIT] = digit
            it[Keys.OMISSION_THRESHOLD] = threshold
        }
    }

    override suspend fun markDelivered(keys: Set<String>) {
        if (keys.isEmpty()) return
        dataStore.edit { preferences ->
            preferences[Keys.DELIVERED_REMINDERS] =
                (preferences[Keys.DELIVERED_REMINDERS].orEmpty() + keys)
                    .sorted()
                    .takeLast(MAX_DELIVERED_KEYS)
                    .toSet()
        }
    }

    override suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.DEFAULT_OBSERVATION_WINDOW)
            preferences.remove(Keys.DRAW_REMINDER)
            preferences.remove(Keys.REPLAY_REMINDER)
            preferences.remove(Keys.OMISSION_REMINDER)
            preferences.remove(Keys.OMISSION_POSITION)
            preferences.remove(Keys.OMISSION_DIGIT)
            preferences.remove(Keys.OMISSION_THRESHOLD)
            preferences.remove(Keys.DELIVERED_REMINDERS)
        }
    }

    private object Keys {
        val DEFAULT_OBSERVATION_WINDOW = intPreferencesKey("default_observation_window")
        val DRAW_REMINDER = booleanPreferencesKey("draw_reminder")
        val REPLAY_REMINDER = booleanPreferencesKey("replay_reminder")
        val OMISSION_REMINDER = booleanPreferencesKey("omission_reminder")
        val OMISSION_POSITION = stringPreferencesKey("omission_position")
        val OMISSION_DIGIT = intPreferencesKey("omission_digit")
        val OMISSION_THRESHOLD = intPreferencesKey("omission_threshold")
        val DELIVERED_REMINDERS = stringSetPreferencesKey("delivered_reminders")
    }

    private companion object {
        const val MAX_DELIVERED_KEYS = 2_048
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UserPreferencesModule {
    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        repository: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository
}
