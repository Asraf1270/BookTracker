package com.openshelf.booktracker

import android.app.Application
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.ReadingStatus
import com.openshelf.booktracker.di.AppContainer
import com.openshelf.booktracker.notifications.NotificationHelper
import com.openshelf.booktracker.notifications.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BookTrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Create the notification channel up front so it exists before WorkManager fires
        NotificationHelper.ensureChannel(this)

        // Seed sample data — DEBUG builds only
        if (BuildConfig.DEBUG) {
            appScope.launch {
                val existing = container.bookRepository.observeAllBooks().firstOrNull()
                if (existing.isNullOrEmpty()) {
                    seedIfEmpty()
                }
            }
        }

        // Re-arm the reminder if the user had it enabled
        scheduleReminderIfEnabled()
    }

    private fun scheduleReminderIfEnabled() {
        appScope.launch {
            val settings = container.preferencesRepository.reminderSettings.firstOrNull()
                ?: return@launch
            if (settings.enabled) {
                scheduleReminder(settings.hour, settings.minute)
            }
        }
    }

    /**
     * Enqueues a one-time work item that fires at the next occurrence of
     * [hour]:[minute] local time. The worker re-schedules itself after running.
     */
    fun scheduleReminder(hour: Int, minute: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val delayMs = calendar.timeInMillis - System.currentTimeMillis()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(ReminderWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            ReminderWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelReminder() {
        WorkManager.getInstance(this).cancelUniqueWork(ReminderWorker.WORK_NAME)
    }

    private suspend fun seedIfEmpty() {
        val repo = container.bookRepository
        repo.addBook(
            BookEntity(
                title = "The Pragmatic Programmer",
                author = "Hunt & Thomas",
                totalPages = 352,
                currentPage = 120,
                genre = "Software",
                coverColor = 0xFF6750A4,
                status = ReadingStatus.CURRENTLY_READING
            )
        )
        repo.addBook(
            BookEntity(
                title = "Atomic Habits",
                author = "James Clear",
                totalPages = 320,
                currentPage = 320,
                genre = "Self-help",
                coverColor = 0xFF00897B,
                status = ReadingStatus.COMPLETED,
                rating = 4.5f,
                finishDate = LocalDate.now().minusDays(3)
            )
        )
        repo.addBook(
            BookEntity(
                title = "Deep Work",
                author = "Cal Newport",
                totalPages = 296,
                currentPage = 0,
                genre = "Productivity",
                coverColor = 0xFFD81B60,
                status = ReadingStatus.WANT_TO_READ
            )
        )
    }
}