package com.openshelf.booktracker.ui.screens.stats

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openshelf.booktracker.BookTrackerApp
import com.openshelf.booktracker.util.ViewModelFactory
import androidx.compose.ui.draw.clip
import com.openshelf.booktracker.ui.components.StreakHeatmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as BookTrackerApp
    val factory = remember { ViewModelFactory(app) }
    val vm: StatsViewModel = viewModel(factory = factory)

    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    // SAF: create backup
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { vm.exportTo(context.contentResolver, it) }
    }

    // SAF: pick backup to restore
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { vm.importFrom(context.contentResolver, it) }
    }

    var menuOpen by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(message) {
        if (message != null) {
            showMessageDialog = message
            vm.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Export backup…") },
                            onClick = {
                                menuOpen = false
                                exportLauncher.launch("booktracker-backup-${System.currentTimeMillis()}.json")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Import backup…") },
                            onClick = {
                                menuOpen = false
                                importLauncher.launch(arrayOf("application/json", "text/*"))
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Overview") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Goals") })
            }

            when (selectedTab) {
                0 -> OverviewTab(uiState)
                1 -> GoalsTab(uiState, onSave = vm::saveGoals)
            }
        }
    }

    showMessageDialog?.let { msg ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showMessageDialog = null },
            title = { Text("Notice") },
            text = { Text(msg) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showMessageDialog = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun OverviewTab(uiState: StatsUiState) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Books\nfinished", uiState.completedCount.toString(), Modifier.weight(1f))
                StatTile("Total\nbooks", uiState.totalBookCount.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Pages\nread", uiState.totalPagesRead.toString(), Modifier.weight(1f))
                StatTile("Minutes\nread", (uiState.totalDurationSeconds / 60).toString(), Modifier.weight(1f))
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Current streak", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "${uiState.currentStreakDays} day${if (uiState.currentStreakDays == 1) "" else "s"}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Reading activity",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Last 12 months · pages read per day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    StreakHeatmap(days = uiState.readingDays)
                }
            }
        }
        item {
            val goalPages = uiState.goals.dailyPageGoal
            val goalMins = uiState.goals.dailyMinuteGoal
            if (goalPages > 0 || goalMins > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Today",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))

                        if (goalPages > 0) {
                            val p = (uiState.todayPages.toFloat() / goalPages).coerceIn(0f, 1f)
                            DailyProgressRow(
                                label = "Pages",
                                current = uiState.todayPages.toString(),
                                target = goalPages.toString(),
                                progress = p
                            )
                            Spacer(Modifier.height(10.dp))
                        }

                        if (goalMins > 0) {
                            val todayMin = (uiState.todayDurationSeconds / 60).toInt()
                            val p = (todayMin.toFloat() / goalMins).coerceIn(0f, 1f)
                            DailyProgressRow(
                                label = "Minutes",
                                current = todayMin.toString(),
                                target = goalMins.toString(),
                                progress = p
                            )
                        }
                    }
                }
            }
        }
        if (uiState.goals.yearlyBookGoal > 0) {
            item {
                val progress = (uiState.completedCount.toFloat() / uiState.goals.yearlyBookGoal)
                    .coerceIn(0f, 1f)
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Yearly goal",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${uiState.completedCount} / ${uiState.goals.yearlyBookGoal} books",
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(8.dp)
                                .let { it }.padding(0.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        if (uiState.recentBooks.isNotEmpty()) {
            item { Text("Recently updated", style = MaterialTheme.typography.titleMedium) }
            items(uiState.recentBooks.size) { idx ->
                val b = uiState.recentBooks[idx]
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(b.title, style = MaterialTheme.typography.titleSmall)
                        Text("${b.author} · ${b.progressPercent}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalsTab(uiState: StatsUiState, onSave: (Int, Int, Int) -> Unit) {
    var yearly by remember(uiState.goals.yearlyBookGoal) {
        mutableStateOf(uiState.goals.yearlyBookGoal.toString())
    }
    var dailyPages by remember(uiState.goals.dailyPageGoal) {
        mutableStateOf(uiState.goals.dailyPageGoal.toString())
    }
    var dailyMinutes by remember(uiState.goals.dailyMinuteGoal) {
        mutableStateOf(uiState.goals.dailyMinuteGoal.toString())
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Reading goals", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = yearly,
            onValueChange = { yearly = it.filter(Char::isDigit) },
            label = { Text("Books per year") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dailyPages,
            onValueChange = { dailyPages = it.filter(Char::isDigit) },
            label = { Text("Pages per day") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = dailyMinutes,
            onValueChange = { dailyMinutes = it.filter(Char::isDigit) },
            label = { Text("Minutes per day") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                onSave(
                    yearly.toIntOrNull() ?: 0,
                    dailyPages.toIntOrNull() ?: 0,
                    dailyMinutes.toIntOrNull() ?: 0
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save goals") }
    }
}

@Composable
private fun DailyProgressRow(
    label: String,
    current: String,
    target: String,
    progress: Float
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                "$current / $target",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )
    }
}