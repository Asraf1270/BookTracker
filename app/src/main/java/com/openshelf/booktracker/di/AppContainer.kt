package com.openshelf.booktracker.di

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.openshelf.booktracker.BookTrackerApp
import com.openshelf.booktracker.data.local.database.BookTrackerDatabase
import com.openshelf.booktracker.data.repository.BookRepository
import com.openshelf.booktracker.data.repository.PreferencesRepository
import com.openshelf.booktracker.widget.WidgetUpdateWorker

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: BookTrackerDatabase by lazy {
        BookTrackerDatabase.getInstance(appContext)
    }

    /**
     * Debounced widget refresh. Called after every successful write.
     * Runs on the main thread (WorkManager requires that for enqueue) and
     * enqueues a unique one-time work — rapid edits coalesce into one refresh.
     */
    private val widgetRefreshCallback: () -> Unit = {
        Handler(Looper.getMainLooper()).post {
            WorkManager.getInstance(appContext).enqueueUniqueWork(
                WidgetUpdateWorker.WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WidgetUpdateWorker>().build()
            )
        }
    }

    val bookRepository: BookRepository by lazy {
        BookRepository(
            bookDao = database.bookDao(),
            sessionDao = database.sessionDao(),
            noteDao = database.noteDao(),
            onDataChanged = widgetRefreshCallback
        )
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(appContext)
    }
}