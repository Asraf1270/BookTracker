package com.openshelf.booktracker

import android.app.Application
import com.openshelf.booktracker.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BookTrackerApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // ---- SEED (disable in production or gate behind a flag) ----
        appScope.launch {
            if (container.bookRepository.observeAllBooks().first().isEmpty()) {
                seedIfEmpty()
            }
        }
    }

    private suspend fun seedIfEmpty() {
        val repo = container.bookRepository
        repo.addBook(
            com.openshelf.booktracker.data.local.entity.BookEntity(
                title = "The Pragmatic Programmer",
                author = "Hunt & Thomas",
                totalPages = 352,
                currentPage = 120,
                genre = "Software",
                coverColor = 0xFF6750A4,
                status = com.openshelf.booktracker.data.local.entity.ReadingStatus.CURRENTLY_READING
            )
        )
        repo.addBook(
            com.openshelf.booktracker.data.local.entity.BookEntity(
                title = "Atomic Habits",
                author = "James Clear",
                totalPages = 320,
                currentPage = 320,
                genre = "Self-help",
                coverColor = 0xFF00897B,
                status = com.openshelf.booktracker.data.local.entity.ReadingStatus.COMPLETED,
                rating = 4.5f,
                finishDate = java.time.LocalDate.now().minusDays(3)
            )
        )
        repo.addBook(
            com.openshelf.booktracker.data.local.entity.BookEntity(
                title = "Deep Work",
                author = "Cal Newport",
                totalPages = 296,
                currentPage = 0,
                genre = "Productivity",
                coverColor = 0xFFD81B60,
                status = com.openshelf.booktracker.data.local.entity.ReadingStatus.WANT_TO_READ
            )
        )
    }
}