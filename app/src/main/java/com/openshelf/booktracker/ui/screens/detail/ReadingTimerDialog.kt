package com.openshelf.booktracker.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class SessionResult(
    val startedAt: Long,
    val endedAt: Long,
    val startPage: Int,
    val endPage: Int
)

@Composable
fun ReadingTimerDialog(
    bookId: Long,
    initialPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onFinish: (SessionResult) -> Unit
) {
    // Timer state
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    // Page state
    var startPageInput by remember { mutableStateOf(initialPage.toString()) }
    var endPageInput by remember { mutableStateOf(initialPage.toString()) }

    // Tick loop
    LaunchedEffect(running) {
        if (running) {
            val tick = System.currentTimeMillis()
            while (running) {
                elapsedMs = System.currentTimeMillis() - startedAt
                delay(200L)
                if (System.currentTimeMillis() - tick < 0) break
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (finished) "Save Session" else "Reading Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Timer display
                Text(
                    text = formatElapsed(elapsedMs),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (!finished) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!running && startedAt == 0L) {
                            Button(
                                onClick = {
                                    startedAt = System.currentTimeMillis()
                                    elapsedMs = 0L
                                    running = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Start")
                            }
                        } else if (running) {
                            OutlinedButton(
                                onClick = { running = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Pause")
                            }
                        } else {
                            Button(
                                onClick = { running = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Resume")
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                running = false
                                finished = true
                            },
                            enabled = startedAt != 0L,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Finish")
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    OutlinedTextField(
                        value = startPageInput,
                        onValueChange = { startPageInput = it.filter(Char::isDigit) },
                        label = { Text("Start page") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = startedAt == 0L,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = endPageInput,
                        onValueChange = { endPageInput = it.filter(Char::isDigit) },
                        label = { Text("End page (of $totalPages)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Duration: ${formatElapsed(elapsedMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (finished) {
                TextButton(onClick = {
                    val start = startPageInput.toIntOrNull() ?: initialPage
                    val end = endPageInput.toIntOrNull() ?: start
                    val now = System.currentTimeMillis()
                    onFinish(
                        SessionResult(
                            startedAt = if (startedAt == 0L) now - elapsedMs else startedAt,
                            endedAt = now,
                            startPage = start.coerceIn(0, totalPages),
                            endPage = end.coerceIn(0, totalPages)
                        )
                    )
                }) { Text("Save") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        dismissButton = if (!finished) null else {
            { TextButton(onClick = { finished = false }) { Text("Back") } }
        }
    )
}

private fun formatElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
           else "%02d:%02d".format(m, s)
}