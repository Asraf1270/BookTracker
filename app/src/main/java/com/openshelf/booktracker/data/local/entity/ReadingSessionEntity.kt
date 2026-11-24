package com.openshelf.booktracker.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId"), Index("startedAt")]
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val bookId: Long,
    val startedAt: Long,             // epoch millis
    val endedAt: Long,               // epoch millis
    val durationSeconds: Long,       // endedAt - startedAt, stored for cheap SUM
    val startPage: Int,
    val endPage: Int,
    val pagesRead: Int               // endPage - startPage (can be 0 if just re-reading)
)