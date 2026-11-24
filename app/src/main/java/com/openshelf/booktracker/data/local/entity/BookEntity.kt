package com.openshelf.booktracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.openshelf.booktracker.data.local.entity.ReadingStatus.CURRENTLY_READING
import java.time.LocalDate

enum class ReadingStatus {
    WANT_TO_READ,
    CURRENTLY_READING,
    COMPLETED
}

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPage: Int = 0,
    val genre: String? = null,
    val coverColor: Long = 0xFF6750A4,        // ARGB packed into Long
    val coverImageUri: String? = null,
    val rating: Float = 0f,                    // 0f = unrated, 0.5–5.0
    val status: ReadingStatus = CURRENTLY_READING,
    val startDate: LocalDate? = null,
    val finishDate: LocalDate? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 0.0 .. 1.0, safe against division-by-zero and overflow. */
    val progressFraction: Float
        get() = if (totalPages <= 0) 0f
        else (currentPage.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}