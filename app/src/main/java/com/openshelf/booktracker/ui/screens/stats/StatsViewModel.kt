package com.openshelf.booktracker.ui.screens.stats

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.BookNoteEntity
import com.openshelf.booktracker.data.local.entity.NoteType
import com.openshelf.booktracker.data.local.entity.ReadingSessionEntity
import com.openshelf.booktracker.data.local.entity.ReadingStatus
import com.openshelf.booktracker.data.repository.BookRepository
import com.openshelf.booktracker.data.repository.PreferencesRepository
import com.openshelf.booktracker.data.repository.ReadingGoals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class StatsUiState(
    val completedCount: Int = 0,
    val totalBookCount: Int = 0,
    val totalPagesInLibrary: Int = 0,
    val totalPagesRead: Int = 0,
    val totalDurationSeconds: Long = 0,
    val currentStreakDays: Int = 0,
    val goals: ReadingGoals = ReadingGoals(),
    val recentBooks: List<BookEntity> = emptyList()
)

class StatsViewModel(
    private val bookRepo: BookRepository,
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val uiState: StateFlow<StatsUiState> = combine(
        bookRepo.observeStats(),
        prefsRepo.goals,
        bookRepo.observeAllBooks()
    ) { stats, goals, allBooks ->
        StatsUiState(
            completedCount = stats.completedCount,
            totalBookCount = stats.totalBookCount,
            totalPagesInLibrary = stats.totalPagesInLibrary,
            totalPagesRead = stats.totalPagesRead,
            totalDurationSeconds = stats.totalDurationSeconds,
            currentStreakDays = computeStreak(stats.distinctDays),
            goals = goals,
            recentBooks = allBooks.sortedByDescending { it.updatedAt }.take(5)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        StatsUiState()
    )

    fun saveGoals(yearly: Int, dailyPages: Int, dailyMinutes: Int) {
        viewModelScope.launch {
            prefsRepo.setGoals(ReadingGoals(yearly, dailyPages, dailyMinutes))
            _message.value = "Goals saved"
        }
    }

    fun exportTo(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val (books, sessions, notes) = bookRepo.getAllForBackup()
                val text = BackupManager.encode(books, sessions, notes)
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(text.toByteArray(Charsets.UTF_8))
                    } ?: error("Could not open output stream")
                }
            }.onSuccess {
                _message.value = "Backup saved"
            }.onFailure {
                _message.value = "Export failed: ${it.message}"
            }
        }
    }

    fun importFrom(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Could not open input stream")
                }
                val payload = BackupManager.decode(text)

                val books = payload.books.map { b ->
                    BookEntity(
                        id = b.id,
                        title = b.title,
                        author = b.author,
                        totalPages = b.totalPages,
                        currentPage = b.currentPage,
                        genre = b.genre,
                        coverColor = b.coverColor,
                        coverImageUri = b.coverImageUri,
                        rating = b.rating,
                        status = runCatching { ReadingStatus.valueOf(b.status) }
                            .getOrDefault(ReadingStatus.WANT_TO_READ),
                        startDate = b.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        finishDate = b.finishDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        createdAt = b.createdAt,
                        updatedAt = b.updatedAt
                    )
                }
                val sessions = payload.sessions.map { s ->
                    ReadingSessionEntity(
                        id = s.id,
                        bookId = s.bookId,
                        startedAt = s.startedAt,
                        endedAt = s.endedAt,
                        durationSeconds = s.durationSeconds,
                        startPage = s.startPage,
                        endPage = s.endPage,
                        pagesRead = s.pagesRead
                    )
                }
                val notes = payload.notes.map { n ->
                    BookNoteEntity(
                        id = n.id,
                        bookId = n.bookId,
                        type = runCatching { NoteType.valueOf(n.type) }
                            .getOrDefault(NoteType.NOTE),
                        content = n.content,
                        pageNumber = n.pageNumber,
                        tags = n.tags,
                        createdAt = n.createdAt,
                        updatedAt = n.updatedAt
                    )
                }
                bookRepo.replaceAll(books, sessions, notes)
            }.onSuccess {
                _message.value = "Restore complete"
            }.onFailure {
                _message.value = "Import failed: ${it.message}"
            }
        }
    }

    fun clearMessage() { _message.value = null }

    private fun computeStreak(days: List<String>): Int {
        if (days.isEmpty()) return 0
        val parsed = days.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .sortedDescending()
        if (parsed.isEmpty()) return 0

        val today = LocalDate.now()
        var cursor = today
        if (parsed.first() == today.minusDays(1)) cursor = today.minusDays(1)
        else if (parsed.first() != today) return 0

        var streak = 0
        for (d in parsed) {
            if (d == cursor) {
                streak++
                cursor = cursor.minusDays(1)
            } else if (d < cursor) {
                break
            }
        }
        return streak
    }
}