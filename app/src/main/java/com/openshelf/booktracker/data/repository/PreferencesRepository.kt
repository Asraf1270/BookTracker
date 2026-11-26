package com.openshelf.booktracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ReadingGoals(
    val yearlyBookGoal: Int = 0,
    val dailyPageGoal: Int = 0,
    val dailyMinuteGoal: Int = 0
)

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0
)

class PreferencesRepository(private val context: Context) {

    // ---- Goal keys ----
    private object GoalKeys {
        val YEARLY_BOOK_GOAL = intPreferencesKey("yearly_book_goal")
        val DAILY_PAGE_GOAL = intPreferencesKey("daily_page_goal")
        val DAILY_MINUTE_GOAL = intPreferencesKey("daily_minute_goal")
    }

    // ---- Reminder keys ----
    private object ReminderKeys {
        val ENABLED = booleanPreferencesKey("reminder_enabled")
        val HOUR = intPreferencesKey("reminder_hour")
        val MINUTE = intPreferencesKey("reminder_minute")
    }

    // ---- Goals ----
    val goals: Flow<ReadingGoals> = context.dataStore.data.map { prefs ->
        ReadingGoals(
            yearlyBookGoal = prefs[GoalKeys.YEARLY_BOOK_GOAL] ?: 0,
            dailyPageGoal = prefs[GoalKeys.DAILY_PAGE_GOAL] ?: 0,
            dailyMinuteGoal = prefs[GoalKeys.DAILY_MINUTE_GOAL] ?: 0
        )
    }

    suspend fun setGoals(goals: ReadingGoals) {
        context.dataStore.edit { prefs ->
            prefs[GoalKeys.YEARLY_BOOK_GOAL] = goals.yearlyBookGoal
            prefs[GoalKeys.DAILY_PAGE_GOAL] = goals.dailyPageGoal
            prefs[GoalKeys.DAILY_MINUTE_GOAL] = goals.dailyMinuteGoal
        }
    }

    suspend fun setYearlyBookGoal(value: Int) =
        context.dataStore.edit { it[GoalKeys.YEARLY_BOOK_GOAL] = value }

    suspend fun setDailyPageGoal(value: Int) =
        context.dataStore.edit { it[GoalKeys.DAILY_PAGE_GOAL] = value }

    suspend fun setDailyMinuteGoal(value: Int) =
        context.dataStore.edit { it[GoalKeys.DAILY_MINUTE_GOAL] = value }

    // ---- Reminder ----
    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ReminderKeys.ENABLED] ?: false
    }

    val reminderSettings: Flow<ReminderSettings> = context.dataStore.data.map { prefs ->
        ReminderSettings(
            enabled = prefs[ReminderKeys.ENABLED] ?: false,
            hour = prefs[ReminderKeys.HOUR] ?: 20,
            minute = prefs[ReminderKeys.MINUTE] ?: 0
        )
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[ReminderKeys.ENABLED] = enabled
            prefs[ReminderKeys.HOUR] = hour.coerceIn(0, 23)
            prefs[ReminderKeys.MINUTE] = minute.coerceIn(0, 59)
        }
    }
}