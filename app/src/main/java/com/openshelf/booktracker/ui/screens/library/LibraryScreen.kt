package com.openshelf.booktracker.ui.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openshelf.booktracker.BookTrackerApp
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.ReadingStatus
import com.openshelf.booktracker.ui.components.BookCard
import com.openshelf.booktracker.ui.components.EmptyStateView
import com.openshelf.booktracker.util.ViewModelFactory
import androidx.compose.material3.OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val factory = remember {
        ViewModelFactory(context.applicationContext as BookTrackerApp)
    }
    val vm: LibraryViewModel = viewModel(factory = factory)
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val selectedStatus by vm.selectedStatus.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf<BookEntity?>(null) }
    var quickProgressBook by remember { mutableStateOf<BookEntity?>(null) }
    var pendingDeleteBook by remember { mutableStateOf<BookEntity?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Library") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add book")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = vm::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search by title, author, genre…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // Status tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReadingStatus.values().forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { vm.selectStatus(status) },
                        label = { Text(status.shortLabel()) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // List
            if (uiState.books.isEmpty() && !uiState.isLoading) {
                EmptyStateView(
                    icon = Icons.Outlined.MenuBook,
                    title = emptyTitle(selectedStatus),
                    message = emptyMessage(selectedStatus),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.books, key = { it.id }) { book ->
                        BookCard(
                            book = book,
                            onClick = {
                                editingBook = null
                                onBookClick(book.id)
                            },
                            onQuickAddPages = { quickProgressBook = book },
                            onEdit = { editingBook = book },
                            onMarkCompleted = {
                                vm.updateStatus(book.id, ReadingStatus.COMPLETED)
                            },
                            onDelete = { pendingDeleteBook = book }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditBookSheet(
            existingBook = null,
            onDismiss = { showAddSheet = false },
            onSave = { vm.addBook(it); showAddSheet = false }
        )
    }

    editingBook?.let { book ->
        AddEditBookSheet(
            existingBook = book,
            onDismiss = { editingBook = null },
            onSave = { vm.updateBook(it); editingBook = null }
        )
    }

    quickProgressBook?.let { book ->
        QuickProgressDialog(
            book = book,
            onDismiss = { quickProgressBook = null },
            onConfirm = { newPage ->
                vm.updateProgress(book.id, newPage)
                quickProgressBook = null
            }
        )
    }

    pendingDeleteBook?.let { book ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDeleteBook = null },
            title = { Text("Delete book?") },
            text = { Text("Deletes \"${book.title}\" along with its sessions and notes.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    vm.deleteBook(book)
                    pendingDeleteBook = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDeleteBook = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun QuickProgressDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    // Working page value — starts at current, updated by quick buttons or text field
    var pageInput by remember { mutableStateOf(book.currentPage.toString()) }
    val currentValue = pageInput.toIntOrNull() ?: book.currentPage

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update progress") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Currently ${book.currentPage} of ${book.totalPages} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick-add buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 10, 20).forEach { delta ->
                        OutlinedButton(
                            onClick = {
                                val next = (currentValue + delta).coerceAtMost(book.totalPages)
                                pageInput = next.toString()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text("+$delta")
                        }
                    }
                }

                // Custom exact page
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter(Char::isDigit) },
                    label = { Text("Set page (of ${book.totalPages})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live preview
                val clamped = currentValue.coerceIn(0, book.totalPages)
                val pct = if (book.totalPages > 0) (clamped * 100) / book.totalPages else 0
                Text(
                    "Will set to $clamped / ${book.totalPages} ($pct%)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val final = currentValue.coerceIn(0, book.totalPages)
                    onConfirm(final)
                },
                enabled = pageInput.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun ReadingStatus.shortLabel(): String = when (this) {
    ReadingStatus.WANT_TO_READ -> "Want"
    ReadingStatus.CURRENTLY_READING -> "Reading"
    ReadingStatus.COMPLETED -> "Done"
}

private fun emptyTitle(status: ReadingStatus): String = when (status) {
    ReadingStatus.WANT_TO_READ -> "Nothing on the wish list"
    ReadingStatus.CURRENTLY_READING -> "No book in progress"
    ReadingStatus.COMPLETED -> "No finished books yet"
}

private fun emptyMessage(status: ReadingStatus): String = when (status) {
    ReadingStatus.WANT_TO_READ -> "Tap + to add a book you'd like to read."
    ReadingStatus.CURRENTLY_READING -> "Pick a book and start reading."
    ReadingStatus.COMPLETED -> "Finish a book to see it here."
}