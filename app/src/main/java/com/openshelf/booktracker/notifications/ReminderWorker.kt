package com.openshelf.booktracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.openshelf.booktracker.BookTrackerApp
import kotlinx.coroutines.flow.first

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? BookTrackerApp ?: return Result.failure()

        return runCatching {
            val settings = app.container.preferencesRepository.reminderSettings.first()
            if (!settings.enabled) return Result.success()

            val pagesToday = app.container.bookRepository
                .observeTodayPagesRead()
                .first()

            if (pagesToday == 0) {
                NotificationHelper.showReminder(applicationContext)
            }

            // Re-schedule for tomorrow at the same time.
            // WorkManager's PeriodicWorkRequest can't hit an exact wall-clock time,
            // so we chain OneTimeWorkRequests instead.
            app.scheduleReminder(settings.hour, settings.minute)

            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val WORK_NAME = "reading_reminder_worker"
    }
}