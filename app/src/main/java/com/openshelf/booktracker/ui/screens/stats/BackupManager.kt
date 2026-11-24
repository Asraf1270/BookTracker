package com.openshelf.booktracker.ui.screens.stats

import com.openshelf.booktracker.data.backup.BackupPayload
import com.openshelf.booktracker.data.backup.BookBackup
import com.openshelf.booktracker.data.backup.NoteBackup
import com.openshelf.booktracker.data.backup.SessionBackup
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.BookNoteEntity
import com.openshelf.booktracker.data.local.entity.ReadingSessionEntity
import kotlinx.serialization.json.Json

object BackupManager {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(
        books: List<BookEntity>,
        sessions: List<ReadingSessionEntity>,
        notes: List<BookNoteEntity>
    ): String {
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            books = books.map { it.toBackup() },
            sessions = sessions.map { it.toBackup() },
            notes = notes.map { it.toBackup() }
        )
        return json.encodeToString(BackupPayload.serializer(), payload)
    }

    fun decode(text: String): BackupPayload =
        json.decodeFromString(BackupPayload.serializer(), text)

    private fun BookEntity.toBackup() = BookBackup(
        id = id, title = title, author = author,
        totalPages = totalPages, currentPage = currentPage,
        genre = genre, coverColor = coverColor,
        coverImageUri = coverImageUri, rating = rating,
        status = status.name,
        startDate = startDate?.toString(),
        finishDate = finishDate?.toString(),
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun ReadingSessionEntity.toBackup() = SessionBackup(
        id = id, bookId = bookId,
        startedAt = startedAt, endedAt = endedAt,
        durationSeconds = durationSeconds,
        startPage = startPage, endPage = endPage, pagesRead = pagesRead
    )

    private fun BookNoteEntity.toBackup() = NoteBackup(
        id = id, bookId = bookId, type = type.name,
        content = content, pageNumber = pageNumber,
        tags = tags, createdAt = createdAt, updatedAt = updatedAt
    )
}