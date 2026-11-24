package com.openshelf.booktracker.di

import android.content.Context
import com.openshelf.booktracker.data.local.database.BookTrackerDatabase
import com.openshelf.booktracker.data.repository.BookRepository
import com.openshelf.booktracker.data.repository.PreferencesRepository

/**
 * Manual DI container. Instantiated once in Application.
 * Every ViewModel factory will pull dependencies from here.
 */
class AppContainer(context: Context) {

    private val database: BookTrackerDatabase by lazy {
        BookTrackerDatabase.getInstance(context)
    }

    val bookRepository: BookRepository by lazy {
        BookRepository(
            bookDao = database.bookDao(),
            sessionDao = database.sessionDao(),
            noteDao = database.noteDao()
        )
    }

    val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepository(context.applicationContext)
    }
}