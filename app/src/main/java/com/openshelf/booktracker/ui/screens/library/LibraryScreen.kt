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
                            onQuickAddPages = { quickProgressBook = book }
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
}

@Composable
private fun QuickProgressDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var pageInput by remember { mutableStateOf(book.currentPage.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update progress") },
        text = {
            Column {
                Text("${book.title}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter(Char::isDigit) },
                    label = { Text("Current page (of ${book.totalPages})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                pageInput.toIntOrNull()?.let(onConfirm)
            }) { Text("Save") }
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