package com.openshelf.booktracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ReadingGoals(
    val yearlyBookGoal: Int = 0,       // 0 = not set
    val dailyPageGoal: Int = 0,
    val dailyMinuteGoal: Int = 0
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val YEARLY_BOOK_GOAL = intPreferencesKey("yearly_book_goal")
        val DAILY_PAGE_GOAL = intPreferencesKey("daily_page_goal")
        val DAILY_MINUTE_GOAL = intPreferencesKey("daily_minute_goal")
    }

    val goals: Flow<ReadingGoals> = context.dataStore.data.map { prefs ->
        ReadingGoals(
            yearlyBookGoal = prefs[Keys.YEARLY_BOOK_GOAL] ?: 0,
            dailyPageGoal = prefs[Keys.DAILY_PAGE_GOAL] ?: 0,
            dailyMinuteGoal = prefs[Keys.DAILY_MINUTE_GOAL] ?: 0
        )
    }

    suspend fun setGoals(goals: ReadingGoals) {
        context.dataStore.edit { prefs ->
            prefs[Keys.YEARLY_BOOK_GOAL] = goals.yearlyBookGoal
            prefs[Keys.DAILY_PAGE_GOAL] = goals.dailyPageGoal
            prefs[Keys.DAILY_MINUTE_GOAL] = goals.dailyMinuteGoal
        }
    }

    suspend fun setYearlyBookGoal(value: Int) =
        context.dataStore.edit { it[Keys.YEARLY_BOOK_GOAL] = value }

    suspend fun setDailyPageGoal(value: Int) =
        context.dataStore.edit { it[Keys.DAILY_PAGE_GOAL] = value }

    suspend fun setDailyMinuteGoal(value: Int) =
        context.dataStore.edit { it[Keys.DAILY_MINUTE_GOAL] = value }
}