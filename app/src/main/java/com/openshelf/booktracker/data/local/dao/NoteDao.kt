package com.openshelf.booktracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.openshelf.booktracker.data.local.entity.BookNoteEntity
import com.openshelf.booktracker.data.local.entity.NoteType
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM book_notes WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun observeForBook(bookId: Long): Flow<List<BookNoteEntity>>

    @Query("SELECT * FROM book_notes WHERE bookId = :bookId AND type = :type ORDER BY createdAt DESC")
    fun observeForBookByType(bookId: Long, type: NoteType): Flow<List<BookNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(note: BookNoteEntity): Long

    @Update
    suspend fun update(note: BookNoteEntity)

    @Delete
    suspend fun delete(note: BookNoteEntity)

    @Query("SELECT * FROM book_notes")
    suspend fun getAllOnce(): List<BookNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<BookNoteEntity>)
}