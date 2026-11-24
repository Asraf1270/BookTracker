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
    private val noteDao: NoteDao
) {

    // ---- Books ----
    fun observeAllBooks(): Flow<List<BookEntity>> = bookDao.observeAll()
    fun observeBooksByStatus(status: ReadingStatus): Flow<List<BookEntity>> =
        bookDao.observeByStatus(status)
    fun searchBooks(query: String): Flow<List<BookEntity>> = bookDao.search(query)
    fun observeBook(id: Long): Flow<BookEntity?> = bookDao.observeById(id)

    suspend fun addBook(book: BookEntity): Long = bookDao.insert(book)

    suspend fun updateBook(book: BookEntity) =
        bookDao.update(book.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteBook(book: BookEntity) = bookDao.delete(book)

    /**
     * Update current page and auto-adjust status:
     *  - progress reaches totalPages → COMPLETED + set finishDate
     *  - progress drops below totalPages while previously COMPLETED → CURRENTLY_READING + clear finishDate
     */
    suspend fun updateProgress(bookId: Long, newPage: Int) {
        val book = bookDao.getById(bookId) ?: return
        val clamped = newPage.coerceIn(0, book.totalPages)
        val now = System.currentTimeMillis()

        bookDao.updateProgress(bookId, clamped, now)

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
    }

    // ---- Sessions ----
    fun observeSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>> =
        sessionDao.observeForBook(bookId)
    fun observeRecentSessions(limit: Int = 20): Flow<List<ReadingSessionEntity>> =
        sessionDao.observeRecent(limit)
    fun observeTotalDurationSeconds(): Flow<Long> = sessionDao.observeTotalDurationSeconds()
    fun observeTotalPagesRead(): Flow<Int> = sessionDao.observeTotalPagesRead()
    fun observeDistinctReadingDays(): Flow<List<String>> = sessionDao.observeDistinctReadingDays()

    suspend fun logSession(session: ReadingSessionEntity): Long =
        sessionDao.insert(session)

    // ---- Notes ----
    fun observeNotesForBook(bookId: Long): Flow<List<BookNoteEntity>> =
        noteDao.observeForBook(bookId)
    suspend fun addNote(note: BookNoteEntity): Long = noteDao.insert(note)
    suspend fun updateNote(note: BookNoteEntity) =
        noteDao.update(note.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteNote(note: BookNoteEntity) = noteDao.delete(note)

    // ---- Stats rollup (combine multiple aggregate flows) ----
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
        // Wipe sessions/notes first (FK cascade would also handle it via book deletion)
        sessionDao.clearAll()
        bookDao.insertAll(books)
        sessionDao.insertAll(sessions)
        noteDao.insertAll(notes)
    }
}