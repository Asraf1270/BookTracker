package com.openshelf.booktracker.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val version: Int = 1,
    val exportedAt: Long,
    val books: List<BookBackup>,
    val sessions: List<SessionBackup>,
    val notes: List<NoteBackup>
)

@Serializable
data class BookBackup(
    val id: Long,
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPage: Int,
    val genre: String? = null,
    val coverColor: Long,
    val coverImageUri: String? = null,
    val rating: Float,
    val status: String,
    val startDate: String? = null,
    val finishDate: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class SessionBackup(
    val id: Long,
    val bookId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationSeconds: Long,
    val startPage: Int,
    val endPage: Int,
    val pagesRead: Int
)

@Serializable
data class NoteBackup(
    val id: Long,
    val bookId: Long,
    val type: String,
    val content: String,
    val pageNumber: Int? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)