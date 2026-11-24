package com.openshelf.booktracker.data.local.converter

import androidx.room.TypeConverter
import com.openshelf.booktracker.data.local.entity.NoteType
import com.openshelf.booktracker.data.local.entity.ReadingStatus
import java.time.LocalDate

class Converters {

    // LocalDate <-> ISO-8601 string ("2024-11-23")
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    // ReadingStatus <-> String (name)
    @TypeConverter
    fun readingStatusToString(status: ReadingStatus): String = status.name

    @TypeConverter
    fun stringToReadingStatus(value: String): ReadingStatus =
        runCatching { ReadingStatus.valueOf(value) }.getOrDefault(ReadingStatus.WANT_TO_READ)

    // NoteType <-> String
    @TypeConverter
    fun noteTypeToString(type: NoteType): String = type.name

    @TypeConverter
    fun stringToNoteType(value: String): NoteType =
        runCatching { NoteType.valueOf(value) }.getOrDefault(NoteType.NOTE)

    // List<String> <-> delimited string
    // Uses \u001F (unit separator) so tags can contain commas
    @TypeConverter
    fun tagListToString(tags: List<String>): String = tags.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToTagList(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        const val SEPARATOR = "\u001F"
    }
}