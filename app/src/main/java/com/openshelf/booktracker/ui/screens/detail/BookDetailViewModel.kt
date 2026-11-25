package com.openshelf.booktracker.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.BookNoteEntity
import com.openshelf.booktracker.data.local.entity.NoteType
import com.openshelf.booktracker.data.local.entity.ReadingSessionEntity
import com.openshelf.booktracker.data.repository.BookRepository
import com.openshelf.booktracker.ui.navigation.Screen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<BookNoteEntity> = emptyList(),
    val quotes: List<BookNoteEntity> = emptyList()
)

class BookDetailViewModel(
    private val repo: BookRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val bookId: Long = savedStateHandle.get<Long>(Screen.BookDetail.ARG_BOOK_ID) ?: -1L

    val book: StateFlow<BookEntity?> = repo.observeBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sessions: StateFlow<List<ReadingSessionEntity>> =
        repo.observeSessionsForBook(bookId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notesState: StateFlow<NotesUiState> =
        repo.observeNotesForBook(bookId)
            .map { list ->
                NotesUiState(
                    notes = list.filter { it.type == NoteType.NOTE },
                    quotes = list.filter { it.type == NoteType.QUOTE }
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                NotesUiState()
            )

    // -------- Progress --------
    fun updatePage(newPage: Int) {
        viewModelScope.launch { repo.updateProgress(bookId, newPage) }
    }

    // -------- Sessions --------
    fun logSession(
        startedAt: Long,
        endedAt: Long,
        startPage: Int,
        endPage: Int
    ) {
        viewModelScope.launch {
            val duration = (endedAt - startedAt).coerceAtLeast(0L) / 1000L
            val pages = (endPage - startPage).coerceAtLeast(0)
            repo.logSession(
                ReadingSessionEntity(
                    bookId = bookId,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    durationSeconds = duration,
                    startPage = startPage,
                    endPage = endPage,
                    pagesRead = pages
                )
            )
            if (endPage > startPage) {
                repo.updateProgress(bookId, endPage)
            }
        }
    }

    // -------- Notes --------
    fun addNote(type: NoteType, content: String, pageNumber: Int?, tags: List<String>) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repo.addNote(
                BookNoteEntity(
                    bookId = bookId,
                    type = type,
                    content = content.trim(),
                    pageNumber = pageNumber,
                    tags = tags
                )
            )
        }
    }

    fun updateNote(note: BookNoteEntity) {
        viewModelScope.launch { repo.updateNote(note) }
    }

    fun deleteNote(note: BookNoteEntity) {
        viewModelScope.launch { repo.deleteNote(note) }
    }

    // -------- Book-level --------
    fun deleteBook(onDeleted: () -> Unit) {
        val current = book.value ?: return
        viewModelScope.launch {
            repo.deleteBook(current)
            onDeleted()
        }
    }

    // Update Rating
    fun updateRating(newRating: Float) {
        val current = book.value ?: return
        viewModelScope.launch {
            repo.updateBook(current.copy(rating = newRating.coerceIn(0f, 5f)))
        }
    }
}