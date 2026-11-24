package com.openshelf.booktracker.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.openshelf.booktracker.BookTrackerApp
import com.openshelf.booktracker.ui.screens.library.LibraryViewModel

/**
 * Single factory that constructs all app ViewModels.
 * Wire this into every `viewModel(factory = ...)` call site.
 *
 * PHASE 4 STATE:
 *  - LibraryViewModel is active.
 *  - BookDetailViewModel (Phase 5) and StatsViewModel (Phase 6) are
 *    commented out because those classes don't exist yet.
 *  - When Phase 5 / 6 land, uncomment the corresponding branch AND
 *    its import at the top of the file.
 */
class ViewModelFactory(
    private val app: BookTrackerApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val bookRepo = app.container.bookRepository
        // Phase 6 needs prefsRepo as well:
        // val prefsRepo = app.container.preferencesRepository

        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
                LibraryViewModel(bookRepo) as T

            // -------- Phase 5: uncomment when BookDetailViewModel exists --------
            // modelClass.isAssignableFrom(BookDetailViewModel::class.java) ->
            //     BookDetailViewModel(bookRepo) as T

            // -------- Phase 6: uncomment when StatsViewModel exists --------------
            // modelClass.isAssignableFrom(StatsViewModel::class.java) ->
            //     StatsViewModel(bookRepo, prefsRepo) as T

            else -> throw IllegalArgumentException(
                "Unknown ViewModel: ${modelClass.name}"
            )
        }
    }
}