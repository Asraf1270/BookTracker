package com.openshelf.booktracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.openshelf.booktracker.data.local.dao.SessionDao
import com.openshelf.booktracker.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.firstOrNull

class ReadingWidget : GlanceAppWidget() {

    companion object {
        val STREAK_KEY = stringPreferencesKey("widget_streak")
        val TODAY_PAGES_KEY = stringPreferencesKey("widget_today_pages")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data BEFORE provideContent — heavy work must happen here [citation:9]
        val app = context.applicationContext as com.openshelf.booktracker.BookTrackerApp
        val repo = app.container.bookRepository
        val prefs = app.container.preferencesRepository

        val streak = computeStreak(repo)
        val todayPages = repo.observeTodayPagesRead().firstOrNull() ?: 0

        provideContent {
            GlanceTheme {
                WidgetContent(
                    streak = streak,
                    todayPages = todayPages
                )
            }
        }
    }

    private suspend fun computeStreak(repo: com.openshelf.booktracker.data.repository.BookRepository): Int {
        // Reuse the same streak logic as StatsViewModel
        val days = repo.observeDistinctReadingDays().firstOrNull() ?: emptyList()
        if (days.isEmpty()) return 0
        val parsed = days.mapNotNull { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
            .sortedDescending()
        if (parsed.isEmpty()) return 0
        val today = java.time.LocalDate.now()
        var cursor = today
        if (parsed.first() == today.minusDays(1)) cursor = today.minusDays(1)
        else if (parsed.first() != today) return 0
        var streak = 0
        for (d in parsed) {
            if (d == cursor) { streak++; cursor = cursor.minusDays(1) }
            else if (d < cursor) break
        }
        return streak
    }

    @Composable
    private fun WidgetContent(streak: Int, todayPages: Int) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Streak row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔥",
                    style = TextStyle(fontSize = 20.sp)
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = "$streak",
                    style = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    text = "day${if (streak == 1) "" else "s"}",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }

            Spacer(GlanceModifier.padding(2.dp))

            // Today's pages row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📖",
                    style = TextStyle(fontSize = 16.sp)
                )
                Spacer(GlanceModifier.width(6.dp))
                if (todayPages > 0) {
                    Text(
                        text = "$todayPages pages today",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.primary
                        )
                    )
                } else {
                    Text(
                        text = "No reading yet",
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}