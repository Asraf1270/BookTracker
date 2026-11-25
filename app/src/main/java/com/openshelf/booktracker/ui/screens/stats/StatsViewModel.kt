package com.openshelf.booktracker.ui.screens.stats

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openshelf.booktracker.data.local.dao.SessionDao
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
    val recentBooks: List<BookEntity> = emptyList(),
    val todayPages: Int = 0,
    val todayDurationSeconds: Long = 0,
    val readingDays: List<SessionDao.DayAggregate> = emptyList()
)

class StatsViewModel(
    private val bookRepo: BookRepository,
    private val prefsRepo: PreferencesRepository
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // 1) Combine the 5 flows that existed before the heatmap was added.
    //    All 5 have strongly typed overloads, so no casting is needed.
    private data class CoreStats(
        val stats: BookRepository.StatsSnapshot,
        val goals: ReadingGoals,
        val allBooks: List<BookEntity>,
        val todayPages: Int,
        val todaySecs: Long
    )

    private val coreStatsFlow = combine(
        bookRepo.observeStats(),
        prefsRepo.goals,
        bookRepo.observeAllBooks(),
        bookRepo.observeTodayPagesRead(),
        bookRepo.observeTodayDurationSeconds()
    ) { stats, goals, allBooks, todayPages, todaySecs ->
        CoreStats(stats, goals, allBooks, todayPages, todaySecs)
    }

    // 2) Combine the 6th (readingDays) on top of the typed core.
    val uiState: StateFlow<StatsUiState> =
        combine(coreStatsFlow, bookRepo.observeReadingDaysLastYear()) { core, readingDays ->
            StatsUiState(
                completedCount = core.stats.completedCount,
                totalBookCount = core.stats.totalBookCount,
                totalPagesInLibrary = core.stats.totalPagesInLibrary,
                totalPagesRead = core.stats.totalPagesRead,
                totalDurationSeconds = core.stats.totalDurationSeconds,
                currentStreakDays = computeStreak(core.stats.distinctDays),
                goals = core.goals,
                recentBooks = core.allBooks.sortedByDescending { it.updatedAt }.take(5),
                todayPages = core.todayPages,
                todayDurationSeconds = core.todaySecs,
                readingDays = readingDays
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
            }.onSuccess { _message.value = "Backup saved" }
                .onFailure { _message.value = "Export failed: ${it.message}" }
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
                        id = b.id, title = b.title, author = b.author,
                        totalPages = b.totalPages, currentPage = b.currentPage,
                        genre = b.genre, coverColor = b.coverColor,
                        coverImageUri = b.coverImageUri, rating = b.rating,
                        status = runCatching { ReadingStatus.valueOf(b.status) }
                            .getOrDefault(ReadingStatus.WANT_TO_READ),
                        startDate = b.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        finishDate = b.finishDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
                        createdAt = b.createdAt, updatedAt = b.updatedAt
                    )
                }
                val sessions = payload.sessions.map { s ->
                    ReadingSessionEntity(
                        id = s.id, bookId = s.bookId,
                        startedAt = s.startedAt, endedAt = s.endedAt,
                        durationSeconds = s.durationSeconds,
                        startPage = s.startPage, endPage = s.endPage, pagesRead = s.pagesRead
                    )
                }
                val notes = payload.notes.map { n ->
                    BookNoteEntity(
                        id = n.id, bookId = n.bookId,
                        type = runCatching { NoteType.valueOf(n.type) }
                            .getOrDefault(NoteType.NOTE),
                        content = n.content, pageNumber = n.pageNumber,
                        tags = n.tags, createdAt = n.createdAt, updatedAt = n.updatedAt
                    )
                }
                bookRepo.replaceAll(books, sessions, notes)
            }.onSuccess { _message.value = "Restore complete" }
                .onFailure { _message.value = "Import failed: ${it.message}" }
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
            } else if (d < cursor) break
        }
        return streak
    }
}