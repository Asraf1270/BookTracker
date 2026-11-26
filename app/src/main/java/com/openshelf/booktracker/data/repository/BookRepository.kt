package com.openshelf.booktracker.data.repository

import com.openshelf.booktracker.data.local.dao.BookDao
import com.openshelf.booktracker.data.local.dao.NoteDao
import com.openshelf.booktracker.data.local.dao.SessionDao
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.BookNoteEntity
import com.openshelf.booktracker.data.local.entity.ReadingSessionEntity
import com.openshelf.booktracker.data.local.entity.ReadingStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class BookRepository(
    private val bookDao: BookDao,
    private val sessionDao: SessionDao,
    private val noteDao: NoteDao,
    private val onDataChanged: () -> Unit = {}
) {

    private fun notifyChanged() = onDataChanged()

    // ---- Books ----
    fun observeAllBooks(): Flow<List<BookEntity>> = bookDao.observeAll()
    fun observeBooksByStatus(status: ReadingStatus): Flow<List<BookEntity>> =
        bookDao.observeByStatus(status)
    fun searchBooks(query: String): Flow<List<BookEntity>> = bookDao.search(query)
    fun observeBook(id: Long): Flow<BookEntity?> = bookDao.observeById(id)

    suspend fun addBook(book: BookEntity): Long {
        val id = bookDao.insert(book)
        notifyChanged()
        return id
    }

    suspend fun updateBook(book: BookEntity) {
        bookDao.update(book.copy(updatedAt = System.currentTimeMillis()))
        notifyChanged()
    }

    suspend fun deleteBook(book: BookEntity) {
        bookDao.delete(book)
        notifyChanged()
    }

    suspend fun updateProgress(bookId: Long, newPage: Int) {
        val book = bookDao.getById(bookId) ?: return
        val clamped = newPage.coerceIn(0, book.totalPages)
        if (clamped == book.currentPage) return

        val now = System.currentTimeMillis()
        val delta = clamped - book.currentPage

        bookDao.updateProgress(bookId, clamped, now)

        if (delta > 0) {
            sessionDao.insert(
                ReadingSessionEntity(
                    bookId = bookId,
                    startedAt = now,
                    endedAt = now,
                    durationSeconds = 0L,
                    startPage = book.currentPage,
                    endPage = clamped,
                    pagesRead = delta
                )
            )
        }

        val reachedEnd = clamped >= book.totalPages && book.totalPages > 0
        when {
            reachedEnd && book.status != ReadingStatus.COMPLETED -> {
                bookDao.update(
                    book.copy(
                        currentPage = clamped,
                        status = ReadingStatus.COMPLETED,
                        finishDate = LocalDate.now(),
                        updatedAt = now
                    )
                )
            }
            !reachedEnd && book.status == ReadingStatus.COMPLETED -> {
                bookDao.update(
                    book.copy(
                        currentPage = clamped,
                        status = ReadingStatus.CURRENTLY_READING,
                        finishDate = null,
                        updatedAt = now
                    )
                )
            }
        }
        notifyChanged()
    }

    suspend fun updateStatus(bookId: Long, status: ReadingStatus) {
        val book = bookDao.getById(bookId) ?: return
        val now = System.currentTimeMillis()
        val updated = when (status) {
            ReadingStatus.COMPLETED -> book.copy(
                status = status,
                currentPage = book.totalPages,
                finishDate = book.finishDate ?: LocalDate.now(),
                updatedAt = now
            )
            ReadingStatus.CURRENTLY_READING -> book.copy(
                status = status,
                finishDate = null,
                startDate = book.startDate ?: LocalDate.now(),
                updatedAt = now
            )
            ReadingStatus.WANT_TO_READ -> book.copy(
                status = status,
                finishDate = null,
                updatedAt = now
            )
        }
        bookDao.update(updated)
        notifyChanged()
    }

    // ---- Sessions ----
    fun observeSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>> =
        sessionDao.observeForBook(bookId)

    fun observeRecentSessions(limit: Int = 20): Flow<List<ReadingSessionEntity>> =
        sessionDao.observeRecent(limit)

    fun observeTotalDurationSeconds(): Flow<Long> = sessionDao.observeTotalDurationSeconds()
    fun observeTotalPagesRead(): Flow<Int> = sessionDao.observeTotalPagesRead()
    fun observeDistinctReadingDays(): Flow<List<String>> = sessionDao.observeDistinctReadingDays()

    fun todayStartMillis(): Long =
        LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun observeTodayPagesRead(): Flow<Int> =
        sessionDao.observePagesReadSince(todayStartMillis())

    fun observeTodayDurationSeconds(): Flow<Long> =
        sessionDao.observeDurationSecondsSince(todayStartMillis())

    fun observeReadingDaysLastYear(): Flow<List<SessionDao.DayAggregate>> =
        sessionDao.observePerDayAggregate(
            System.currentTimeMillis() - 365L * 24 * 60 * 60 * 1000
        )

    suspend fun logSession(session: ReadingSessionEntity): Long {
        val id = sessionDao.insert(session)
        notifyChanged()
        return id
    }

    // ---- Notes ----
    fun observeNotesForBook(bookId: Long): Flow<List<BookNoteEntity>> =
        noteDao.observeForBook(bookId)

    suspend fun addNote(note: BookNoteEntity): Long {
        val id = noteDao.insert(note)
        notifyChanged()
        return id
    }

    suspend fun updateNote(note: BookNoteEntity) {
        noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
        notifyChanged()
    }

    suspend fun deleteNote(note: BookNoteEntity) {
        noteDao.delete(note)
        notifyChanged()
    }

    // ---- Stats rollup ----
    data class StatsSnapshot(
        val completedCount: Int,
        val totalPagesInLibrary: Int,
        val totalBookCount: Int,
        val totalPagesRead: Int,
        val totalDurationSeconds: Long,
        val distinctDays: List<String>
    )

    fun observeStats(): Flow<StatsSnapshot> = combine(
        bookDao.observeCompletedCount(),
        bookDao.observeTotalPagesInLibrary(),
        bookDao.observeTotalBookCount(),
        sessionDao.observeTotalPagesRead(),
        sessionDao.observeTotalDurationSeconds(),
        sessionDao.observeDistinctReadingDays()
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        StatsSnapshot(
            completedCount = arr[0] as Int,
            totalPagesInLibrary = arr[1] as Int,
            totalBookCount = arr[2] as Int,
            totalPagesRead = arr[3] as Int,
            totalDurationSeconds = arr[4] as Long,
            distinctDays = arr[5] as List<String>
        )
    }

    // ---- Backup helpers ----
    suspend fun getAllForBackup(): Triple<List<BookEntity>, List<ReadingSessionEntity>, List<BookNoteEntity>> =
        Triple(bookDao.getAllOnce(), sessionDao.getAllOnce(), noteDao.getAllOnce())

    suspend fun replaceAll(
        books: List<BookEntity>,
        sessions: List<ReadingSessionEntity>,
        notes: List<BookNoteEntity>
    ) {
        sessionDao.clearAll()
        bookDao.insertAll(books)
        sessionDao.insertAll(sessions)
        noteDao.insertAll(notes)
        notifyChanged()
    }
}