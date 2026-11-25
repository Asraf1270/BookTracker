package com.openshelf.booktracker.ui.screens.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openshelf.booktracker.BookTrackerApp
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.BookNoteEntity
import com.openshelf.booktracker.data.local.entity.NoteType
import com.openshelf.booktracker.data.local.entity.ReadingStatus
import com.openshelf.booktracker.ui.components.BookCover
import com.openshelf.booktracker.ui.components.EmptyStateView
import com.openshelf.booktracker.ui.components.ProgressRing
import com.openshelf.booktracker.util.ViewModelFactory

private enum class NotesFilter { ALL, NOTES, QUOTES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as BookTrackerApp
    val factory = remember { ViewModelFactory(app) }
    val vm: BookDetailViewModel = viewModel(factory = factory)

    val book by vm.book.collectAsStateWithLifecycle()
    val notesState by vm.notesState.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()

    var showProgressDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<BookNoteEntity?>(null) }
    var pendingDeleteNote by remember { mutableStateOf<BookNoteEntity?>(null) }
    var pendingDeleteBook by remember { mutableStateOf(false) }
    var notesFilter by remember { mutableStateOf(NotesFilter.ALL) }
    var showEditSheet by remember { mutableStateOf(false) }
    val current = book

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(current?.title ?: "Book") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit book")
                    }
                    IconButton(onClick = { pendingDeleteBook = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete book")
                    }
                }
            )
        },
        floatingActionButton = {
            if (current != null) {
                FloatingActionButton(onClick = { showAddNoteDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add note")
                }
            }
        }
    ) { innerPadding ->

        if (current == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Book not found", style = MaterialTheme.typography.titleMedium)
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeaderCard(book = current, onEditRating = { showRatingDialog = true }) }
            item { ProgressCard(current) { showProgressDialog = true } }
            item {
                ActionsRow(
                    onQuickAdd = { delta ->
                        vm.updatePage((current.currentPage + delta).coerceAtMost(current.totalPages))
                    },
                    onCustom = { showProgressDialog = true },
                    onTimer = { showTimerDialog = true }
                )
            }

            if (sessions.isNotEmpty()) {
                item {
                    val totalMin = sessions.sumOf { it.durationSeconds } / 60
                    val totalPages = sessions.sumOf { it.pagesRead }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Sessions", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${sessions.size} sessions · $totalPages pages · $totalMin min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(Modifier.height(4.dp))
                            // Latest 5 sessions inline
                            sessions.take(5).forEach { s ->
                                SessionRow(session = s)
                            }
                            if (sessions.size > 5) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "+ ${sessions.size - 5} more",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = notesFilter == NotesFilter.ALL,
                        onClick = { notesFilter = NotesFilter.ALL },
                        label = { Text("All (${notesState.notes.size + notesState.quotes.size})") }
                    )
                    FilterChip(
                        selected = notesFilter == NotesFilter.NOTES,
                        onClick = { notesFilter = NotesFilter.NOTES },
                        label = { Text("Notes (${notesState.notes.size})") }
                    )
                    FilterChip(
                        selected = notesFilter == NotesFilter.QUOTES,
                        onClick = { notesFilter = NotesFilter.QUOTES },
                        label = { Text("Quotes (${notesState.quotes.size})") }
                    )
                }
            }

            val visible = when (notesFilter) {
                NotesFilter.ALL -> notesState.notes + notesState.quotes
                NotesFilter.NOTES -> notesState.notes
                NotesFilter.QUOTES -> notesState.quotes
            }

            if (visible.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Filled.Edit,
                        title = "No ${if (notesFilter == NotesFilter.QUOTES) "quotes" else "notes"} yet",
                        message = "Tap + to add one."
                    )
                }
            } else {
                items(visible, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onEdit = { editingNote = note },
                        onDelete = { pendingDeleteNote = note }
                    )
                }
            }
        }
    }

    // ---- Dialogs ----

    if (showProgressDialog && current != null) {
        PageProgressDialog(
            book = current,
            onDismiss = { showProgressDialog = false },
            onConfirm = { page -> vm.updatePage(page); showProgressDialog = false }
        )
    }

    if (showTimerDialog && current != null) {
        ReadingTimerDialog(
            bookId = current.id,
            initialPage = current.currentPage,
            totalPages = current.totalPages,
            onDismiss = { showTimerDialog = false },
            onFinish = { result ->
                vm.logSession(
                    startedAt = result.startedAt,
                    endedAt = result.endedAt,
                    startPage = result.startPage,
                    endPage = result.endPage
                )
                showTimerDialog = false
            }
        )
    }

    if (showRatingDialog && current != null) {
        RatingDialog(
            initial = current.rating,
            onDismiss = { showRatingDialog = false },
            onSave = { newRating ->
                vm.updateRating(newRating)
                showRatingDialog = false
            }
        )
    }

    if (showAddNoteDialog) {
        NoteEditorDialog(
            initial = null,
            totalPages = current?.totalPages ?: 0,
            onDismiss = { showAddNoteDialog = false },
            onSave = { type, content, page, tags ->
                vm.addNote(type, content, page, tags)
                showAddNoteDialog = false
            }
        )
    }

    editingNote?.let { note ->
        NoteEditorDialog(
            initial = note,
            totalPages = current?.totalPages ?: 0,
            onDismiss = { editingNote = null },
            onSave = { type, content, page, tags ->
                vm.updateNote(
                    note.copy(type = type, content = content, pageNumber = page, tags = tags)
                )
                editingNote = null
            }
        )
    }

    pendingDeleteNote?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDeleteNote = null },
            title = { Text("Delete note?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteNote(note); pendingDeleteNote = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteNote = null }) { Text("Cancel") }
            }
        )
    }

    if (pendingDeleteBook) {
        AlertDialog(
            onDismissRequest = { pendingDeleteBook = false },
            title = { Text("Delete book?") },
            text = { Text("Deletes the book, its sessions, and its notes.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteBook { pendingDeleteBook = false; onBack() }
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteBook = false }) { Text("Cancel") }
            }
        )
    }
}

// ---------------- Sub-composables ----------------

@Composable
private fun HeaderCard(
    book: BookEntity,
    onEditRating: () -> Unit
) {
    val coverColor = Color(book.coverColor)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            BookCover(
                coverColor = book.coverColor,
                coverImageUri = book.coverImageUri,
                title = book.title,
                modifier = Modifier.size(width = 88.dp, height = 128.dp),
                cornerRadius = 10.dp
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    book.author,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = coverColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            book.status.pretty(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = coverColor
                        )
                    }
                    if (book.genre != null) {
                        Text(
                            book.genre,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onEditRating() }
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (book.rating >= i) Icons.Filled.Star
                                          else Icons.Filled.StarBorder,
                            contentDescription = "Rate",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (book.rating <= 0f) "Tap to rate"
                        else "%.1f".format(book.rating),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(book: BookEntity, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(progress = book.progressFraction, size = 72.dp, strokeWidth = 8.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${book.currentPage} / ${book.totalPages} pages",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${book.progressPercent}% complete",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { book.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(book.coverColor)
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit progress")
            }
        }
    }
}

@Composable
private fun ActionsRow(
    onQuickAdd: (Int) -> Unit,
    onCustom: () -> Unit,
    onTimer: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(5, 10, 20).forEach { delta ->
                OutlinedButton(
                    onClick = { onQuickAdd(delta) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Text("+$delta")
                }
            }
            OutlinedButton(
                onClick = onCustom,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 6.dp)
            ) {
                Text("Custom")
            }
        }
        OutlinedButton(
            onClick = onTimer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Start Reading Timer")
        }
    }
}

@Composable
private fun NoteCard(
    note: BookNoteEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (note.type == NoteType.QUOTE) "Quote" else "Note",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (note.pageNumber != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "· p.${note.pageNumber}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                note.content,
                style = if (note.type == NoteType.QUOTE) MaterialTheme.typography.bodyLarge
                        else MaterialTheme.typography.bodyMedium
            )
            if (note.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    note.tags.joinToString(" · ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PageProgressDialog(
    book: BookEntity,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var pageInput by remember { mutableStateOf(book.currentPage.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update page") },
        text = {
            OutlinedTextField(
                value = pageInput,
                onValueChange = { pageInput = it.filter(Char::isDigit) },
                label = { Text("Current page (of ${book.totalPages})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { pageInput.toIntOrNull()?.let(onConfirm) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RatingDialog(
    initial: Float,
    onDismiss: () -> Unit,
    onSave: (Float) -> Unit
) {
    var rating by remember { mutableFloatStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this book") },
        text = {
            Column {
                Text(
                    if (rating <= 0f) "Not rated" else "%.1f / 5".format(rating),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 0f..5f,
                    steps = 9
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(rating) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NoteEditorDialog(
    initial: BookNoteEntity?,
    totalPages: Int,
    onDismiss: () -> Unit,
    onSave: (NoteType, String, Int?, List<String>) -> Unit
) {
    var type by remember { mutableStateOf(initial?.type ?: NoteType.NOTE) }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var pageInput by remember { mutableStateOf(initial?.pageNumber?.toString() ?: "") }
    var tagsInput by remember { mutableStateOf(initial?.tags?.joinToString(",") ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add" else "Edit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == NoteType.NOTE,
                        onClick = { type = NoteType.NOTE },
                        label = { Text("Note") }
                    )
                    FilterChip(
                        selected = type == NoteType.QUOTE,
                        onClick = { type = NoteType.QUOTE },
                        label = { Text("Quote") }
                    )
                }
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (type == NoteType.QUOTE) "Quote text" else "Note text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter(Char::isDigit) },
                    label = { Text("Page (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagsInput,
                    onValueChange = { tagsInput = it },
                    label = { Text("Tags (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val page = pageInput.toIntOrNull()?.takeIf { it in 1..totalPages }
                    val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(type, content, page, tags)
                },
                enabled = content.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SessionRow(session: com.openshelf.booktracker.data.local.entity.ReadingSessionEntity) {
    val date = java.time.Instant.ofEpochMilli(session.startedAt)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
    val minutes = session.durationSeconds / 60
    val label = when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${session.durationSeconds}s"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d")),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "pp. ${session.startPage} → ${session.endPage} (+${session.pagesRead})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun Divider(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.material3.HorizontalDivider(color = color)
}

private fun ReadingStatus.pretty(): String = when (this) {
    ReadingStatus.WANT_TO_READ -> "Want to Read"
    ReadingStatus.CURRENTLY_READING -> "Reading"
    ReadingStatus.COMPLETED -> "Completed"
}