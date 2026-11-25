package com.openshelf.booktracker.ui.screens.library

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.openshelf.booktracker.data.local.entity.BookEntity
import com.openshelf.booktracker.data.local.entity.ReadingStatus
import com.openshelf.booktracker.ui.components.BookCover
import com.openshelf.booktracker.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val CoverColors = listOf(
    0xFF6750A4, 0xFF00897B, 0xFFD81B60, 0xFFF57C00, 0xFF1976D2, 0xFF5D4037
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBookSheet(
    existingBook: BookEntity?,
    onDismiss: () -> Unit,
    onSave: (BookEntity) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(existingBook?.title ?: "") }
    var author by remember { mutableStateOf(existingBook?.author ?: "") }
    var totalPages by remember { mutableStateOf(existingBook?.totalPages?.toString() ?: "") }
    var genre by remember { mutableStateOf(existingBook?.genre ?: "") }
    var coverColor by remember { mutableStateOf(existingBook?.coverColor ?: CoverColors.first()) }
    var coverImagePath by remember { mutableStateOf(existingBook?.coverImageUri) }
    var rating by remember { mutableFloatStateOf(existingBook?.rating ?: 0f) }
    var status by remember { mutableStateOf(existingBook?.status ?: ReadingStatus.WANT_TO_READ) }

    // Image picker
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    ImageUtils.copyToInternalStorage(context, uri)
                }
                if (saved != null) {
                    // Delete old cover if replacing
                    if (existingBook?.coverImageUri != null && coverImagePath != existingBook?.coverImageUri) {
                        ImageUtils.deleteFile(coverImagePath)
                    }
                    coverImagePath = saved
                }
            }
        }
    }

    val canSave = title.isNotBlank() && author.isNotBlank() && (totalPages.toIntOrNull() ?: 0) > 0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (existingBook == null) "Add Book" else "Edit Book",
                style = MaterialTheme.typography.titleLarge
            )

            // ---- Cover picker row ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                BookCover(
                    coverColor = coverColor,
                    coverImageUri = coverImagePath,
                    title = title.ifBlank { "?" },
                    modifier = Modifier.size(width = 72.dp, height = 104.dp),
                    cornerRadius = 8.dp
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Cover image", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { pickImage.launch("image/*") }) {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = "Pick image")
                        }
                        if (coverImagePath != null) {
                            Button(onClick = {
                                ImageUtils.deleteFile(coverImagePath)
                                coverImagePath = null
                            }) { Text("Remove") }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text("Author") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = totalPages,
                onValueChange = { totalPages = it.filter(Char::isDigit) },
                label = { Text("Total pages") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = genre,
                onValueChange = { genre = it },
                label = { Text("Genre (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            StatusDropdown(current = status, onSelect = { status = it })

            // ---- Rating slider ----
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rating", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (rating <= 0f) "Not rated" else "%.1f / 5".format(rating),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = rating,
                    onValueChange = { rating = it },
                    valueRange = 0f..5f,
                    steps = 9     // 0, 0.5, 1.0 … 5.0
                )
            }

            // ---- Color swatches ----
            Text("Fallback cover color", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CoverColors.forEach { argb ->
                    val selected = argb == coverColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .then(
                                if (selected) Modifier.border(
                                    width = 3.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                ) else Modifier
                            )
                            .clickable { coverColor = argb }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    val book = (existingBook ?: BookEntity(
                        title = title.trim(),
                        author = author.trim(),
                        totalPages = totalPages.toInt()
                    )).copy(
                        title = title.trim(),
                        author = author.trim(),
                        totalPages = totalPages.toInt(),
                        genre = genre.trim().ifBlank { null },
                        coverColor = coverColor,
                        coverImageUri = coverImagePath,
                        rating = rating.coerceIn(0f, 5f),
                        status = status,
                        startDate = existingBook?.startDate
                            ?: if (status == ReadingStatus.CURRENTLY_READING) LocalDate.now() else null
                    )
                    onSave(book)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (existingBook == null) "Add to Library" else "Save Changes")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    current: ReadingStatus,
    onSelect: (ReadingStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.pretty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReadingStatus.values().forEach { s ->
                DropdownMenuItem(
                    text = { Text(s.pretty()) },
                    onClick = { onSelect(s); expanded = false }
                )
            }
        }
    }
}

private fun ReadingStatus.pretty(): String = when (this) {
    ReadingStatus.WANT_TO_READ -> "Want to Read"
    ReadingStatus.CURRENTLY_READING -> "Currently Reading"
    ReadingStatus.COMPLETED -> "Completed"
}