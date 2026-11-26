package com.openshelf.booktracker.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.openshelf.booktracker.BookTrackerApp
import com.openshelf.booktracker.ui.screens.detail.BookDetailViewModel
import com.openshelf.booktracker.ui.screens.library.LibraryViewModel
import com.openshelf.booktracker.ui.screens.stats.StatsViewModel

class ViewModelFactory(
    private val app: BookTrackerApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        val bookRepo = app.container.bookRepository
        val prefsRepo = app.container.preferencesRepository

        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
                LibraryViewModel(bookRepo) as T

            modelClass.isAssignableFrom(BookDetailViewModel::class.java) ->
                BookDetailViewModel(bookRepo, extras.createSavedStateHandle()) as T

            modelClass.isAssignableFrom(StatsViewModel::class.java) ->
                StatsViewModel(bookRepo, prefsRepo, app) as T

            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}