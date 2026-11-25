package com.openshelf.booktracker.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.ReadingStatus
import com.openshelf.booktracker.data.repository.BookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<BookEntity> = emptyList(),
    val selectedStatus: ReadingStatus = ReadingStatus.CURRENTLY_READING,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repo: BookRepository
) : ViewModel() {

    private val _selectedStatus = MutableStateFlow(ReadingStatus.CURRENTLY_READING)
    val selectedStatus: StateFlow<ReadingStatus> = _selectedStatus.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<LibraryUiState> =
        combine(_selectedStatus, _searchQuery) { status, query -> status to query }
            .flatMapLatest { (status, query) ->
                val flow = if (query.isBlank()) {
                    repo.observeBooksByStatus(status)
                } else {
                    repo.searchBooks(query)
                }
                combine(flow, _selectedStatus, _searchQuery) { books, s, q ->
                    LibraryUiState(books = books, selectedStatus = s, searchQuery = q, isLoading = false)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = LibraryUiState()
            )

    fun selectStatus(status: ReadingStatus) {
        _selectedStatus.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addBook(book: BookEntity) {
        viewModelScope.launch { repo.addBook(book) }
    }

    fun updateBook(book: BookEntity) {
        viewModelScope.launch { repo.updateBook(book) }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch { repo.deleteBook(book) }
    }

    /** Quick "+N pages" or custom page entry from the Library popup. */
    fun updateProgress(bookId: Long, newPage: Int) {
        viewModelScope.launch { repo.updateProgress(bookId, newPage) }
    }

    /** Long-press "Mark as Completed" menu action. */
    fun updateStatus(bookId: Long, status: ReadingStatus) {
        viewModelScope.launch { repo.updateStatus(bookId, status) }
    }
}