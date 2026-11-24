package com.openshelf.booktracker.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.openshelf.booktracker.data.local.converter.Converters
import com.openshelf.booktracker.data.local.dao.BookDao
import com.openshelf.booktracker.data.local.dao.NoteDao
import com.openshelf.booktracker.data.local.dao.SessionDao
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.BookNoteEntity
import com.openshelf.booktracker.data.local.entity.ReadingSessionEntity

@Database(
    entities = [
        BookEntity::class,
        ReadingSessionEntity::class,
        BookNoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BookTrackerDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun sessionDao(): SessionDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: BookTrackerDatabase? = null

        fun getInstance(context: Context): BookTrackerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookTrackerDatabase::class.java,
                    "booktracker.db"
                )
                    // Enable foreign-key cascade (Room does not enable by default)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}