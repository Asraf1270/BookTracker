package com.openshelf.booktracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.openshelf.booktracker.data.local.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: ReadingSessionEntity): Long

    /** Session history visible in UI — excludes zero-duration page-delta rows. */
    @Query(
        """
        SELECT * FROM reading_sessions
        WHERE bookId = :bookId AND durationSeconds > 0
        ORDER BY startedAt DESC
        """
    )
    fun observeForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    /** Recent real sessions (for stats "recent activity"). */
    @Query(
        """
        SELECT * FROM reading_sessions
        WHERE durationSeconds > 0
        ORDER BY startedAt DESC LIMIT :limit
        """
    )
    fun observeRecent(limit: Int = 20): Flow<List<ReadingSessionEntity>>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM reading_sessions")
    fun observeTotalDurationSeconds(): Flow<Long>

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions")
    fun observeTotalPagesRead(): Flow<Int>

    @Query(
        """
        SELECT DISTINCT strftime('%Y-%m-%d', startedAt / 1000, 'unixepoch', 'localtime')
        FROM reading_sessions
        ORDER BY 1 DESC
        """
    )
    fun observeDistinctReadingDays(): Flow<List<String>>

    // --- Today's activity ---
    @Query(
        """
        SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions
        WHERE startedAt >= :dayStartMillis
        """
    )
    fun observePagesReadSince(dayStartMillis: Long): Flow<Int>

    @Query(
        """
        SELECT COALESCE(SUM(durationSeconds), 0) FROM reading_sessions
        WHERE startedAt >= :dayStartMillis
        """
    )
    fun observeDurationSecondsSince(dayStartMillis: Long): Flow<Long>

    // --- Per-day aggregate for heatmap ---
    data class DayAggregate(
        val day: String,
        val pages: Int,
        val minutes: Int
    )

    @Query(
        """
        SELECT strftime('%Y-%m-%d', startedAt / 1000, 'unixepoch', 'localtime') AS day,
               COALESCE(SUM(pagesRead), 0) AS pages,
               CAST(COALESCE(SUM(durationSeconds), 0) / 60 AS INTEGER) AS minutes
        FROM reading_sessions
        WHERE startedAt >= :sinceMillis
        GROUP BY day
        ORDER BY day ASC
        """
    )
    fun observePerDayAggregate(sinceMillis: Long): Flow<List<DayAggregate>>

    @Query("SELECT * FROM reading_sessions")
    suspend fun getAllOnce(): List<ReadingSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<ReadingSessionEntity>)

    @Query("DELETE FROM reading_sessions")
    suspend fun clearAll()
}