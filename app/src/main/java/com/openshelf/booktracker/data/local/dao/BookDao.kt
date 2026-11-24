package com.openshelf.booktracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.ReadingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: ReadingStatus): Flow<List<BookEntity>>

    @Query(
        """
        SELECT * FROM books
        WHERE title LIKE '%' || :query || '%'
           OR author LIKE '%' || :query || '%'
           OR genre LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun search(query: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE books SET currentPage = :page, updatedAt = :now WHERE id = :id")
    suspend fun updateProgress(id: Long, page: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE books SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ReadingStatus, now: Long = System.currentTimeMillis())

    // --- Stats aggregates ---
    @Query("SELECT COUNT(*) FROM books WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(currentPage), 0) FROM books")
    fun observeTotalPagesInLibrary(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books")
    fun observeTotalBookCount(): Flow<Int>

    @Query("SELECT * FROM books")
    suspend fun getAllOnce(): List<BookEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(books: List<BookEntity>)
}