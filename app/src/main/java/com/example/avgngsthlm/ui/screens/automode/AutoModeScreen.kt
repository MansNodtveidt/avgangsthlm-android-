package com.example.avgngsthlm.ui.screens.automode

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.avgngsthlm.data.local.entity.AutoRule
import com.example.avgngsthlm.util.AutoModeHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoModeScreen(vm: AutoModeViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<AutoRule?>(null) }
    var deleteTarget by remember { mutableStateOf<AutoRule?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Auto mode") }) },
        floatingActionButton = {
            if (state.favorites.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Lägg till regel")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Auto mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Väljer favorit automatiskt baserat på tid och dag",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.autoModeEnabled,
                    onCheckedChange = vm::setAutoModeEnabled
                )
            }

            HorizontalDivider()

            if (state.favorites.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Lägg till favoriter först för att skapa regler.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (state.rules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Inga regler ännu.\nTryck + för att lägga till.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.rules, key = { it.id }) { rule ->
                        val favName = state.favorites.firstOrNull { it.id == rule.favoriteId }?.name ?: "?"
                        AutoRuleItem(
                            rule = rule,
                            favoriteName = favName,
                            onEdit = { editTarget = rule },
                            onDelete = { deleteTarget = rule }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            favorites = state.favorites.map { it.id to it.name },
            onDismiss = { showAddDialog = false },
            onConfirm = { name, favId, days, start, end ->
                vm.addRule(name, favId, days, start, end)
                showAddDialog = false
            }
        )
    }

    editTarget?.let { rule ->
        EditRuleDialog(
            rule = rule,
            favorites = state.favorites.map { it.id to it.name },
            onDismiss = { editTarget = null },
            onConfirm = { name, favId, days, start, end ->
                vm.updateRule(rule.copy(name = name, favoriteId = favId, daysOfWeek = days, startTime = start, endTime = end))
                editTarget = null
            }
        )
    }

    deleteTarget?.let { rule ->
        val favName = state.favorites.firstOrNull { it.id == rule.favoriteId }?.name ?: "?"
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Ta bort?") },
            text = { Text("Är du säker på att du vill ta bort regeln för \"$favName\"?") },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteRule(rule); deleteTarget = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Ta bort") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Avbryt") }
            }
        )
    }
}

@Composable
private fun AutoRuleItem(
    rule: AutoRule,
    favoriteName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (rule.name.isNotEmpty()) {
                    Text(rule.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${AutoModeHelper.daysString(rule.daysOfWeek)} • ${rule.startTime}–${rule.endTime} • $favoriteName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(favoriteName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        AutoModeHelper.daysString(rule.daysOfWeek),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${rule.startTime} – ${rule.endTime}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Redigera regel")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Ta bort regel", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddRuleDialog(
    favorites: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, favId: Int, days: String, start: String, end: String) -> Unit
) {
    var ruleName by remember { mutableStateOf("") }
    var selectedFavId by remember { mutableStateOf(favorites.firstOrNull()?.first ?: -1) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var startTime by remember { mutableStateOf("07:00") }
    var endTime by remember { mutableStateOf("09:00") }
    var favExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ny regel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Regelnamn
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Namn på regel (valfritt)") },
                    placeholder = { Text("T.ex. Morgon till skolan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Välj favorit
                ExposedDropdownMenuBox(
                    expanded = favExpanded,
                    onExpandedChange = { favExpanded = it }
                ) {
                    OutlinedTextField(
                        value = favorites.firstOrNull { it.first == selectedFavId }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Favorit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = favExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = favExpanded,
                        onDismissRequest = { favExpanded = false }
                    ) {
                        favorites.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { selectedFavId = id; favExpanded = false }
                            )
                        }
                    }
                }

                // Veckodagar
                Text("Veckodagar:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { selectedDays = setOf(1, 2, 3, 4, 5) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("Vardagar", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { selectedDays = setOf(6, 7) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("Helg", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { selectedDays = setOf(1, 2, 3, 4, 5, 6, 7) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("Alla", style = MaterialTheme.typography.labelSmall) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AutoModeHelper.DAY_NAMES.forEachIndexed { index, name ->
                        val day = index + 1
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays)
                                    selectedDays - day else selectedDays + day
                            },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Tider
                Text("Tider:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimePickerButton(
                        label = "Från",
                        time = startTime,
                        onTimeSelected = { startTime = it },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerButton(
                        label = "Till",
                        time = endTime,
                        onTimeSelected = { endTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    selectedFavId == -1 -> error = "Välj en favorit"
                    selectedDays.isEmpty() -> error = "Välj minst en dag"
                    else -> onConfirm(ruleName.trim(), selectedFavId, selectedDays.sorted().joinToString(","), startTime, endTime)
                }
            }) { Text("Lägg till") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditRuleDialog(
    rule: AutoRule,
    favorites: List<Pair<Int, String>>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, favId: Int, days: String, start: String, end: String) -> Unit
) {
    var ruleName by remember { mutableStateOf(rule.name) }
    var selectedFavId by remember { mutableStateOf(rule.favoriteId) }
    var selectedDays by remember {
        mutableStateOf(
            rule.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        )
    }
    var startTime by remember { mutableStateOf(rule.startTime) }
    var endTime by remember { mutableStateOf(rule.endTime) }
    var favExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Redigera regel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = ruleName,
                    onValueChange = { ruleName = it },
                    label = { Text("Namn på regel (valfritt)") },
                    placeholder = { Text("T.ex. Morgon till skolan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = favExpanded,
                    onExpandedChange = { favExpanded = it }
                ) {
                    OutlinedTextField(
                        value = favorites.firstOrNull { it.first == selectedFavId }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Favorit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = favExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = favExpanded,
                        onDismissRequest = { favExpanded = false }
                    ) {
                        favorites.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = { selectedFavId = id; favExpanded = false }
                            )
                        }
                    }
                }

                Text("Veckodagar:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { selectedDays = setOf(1, 2, 3, 4, 5) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("Vardagar", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { selectedDays = setOf(6, 7) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("Helg", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = { selectedDays = setOf(1, 2, 3, 4, 5, 6, 7) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("Alla", style = MaterialTheme.typography.labelSmall) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AutoModeHelper.DAY_NAMES.forEachIndexed { index, name ->
                        val day = index + 1
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays)
                                    selectedDays - day else selectedDays + day
                            },
                            label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Tider:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimePickerButton(
                        label = "Från",
                        time = startTime,
                        onTimeSelected = { startTime = it },
                        modifier = Modifier.weight(1f)
                    )
                    TimePickerButton(
                        label = "Till",
                        time = endTime,
                        onTimeSelected = { endTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    selectedFavId == -1 -> error = "Välj en favorit"
                    selectedDays.isEmpty() -> error = "Välj minst en dag"
                    else -> onConfirm(ruleName.trim(), selectedFavId, selectedDays.sorted().joinToString(","), startTime, endTime)
                }
            }) { Text("Spara ändringar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Avbryt") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerButton(
    label: String,
    time: String,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val parts = time.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = modifier
    ) {
        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("$label: $time", style = MaterialTheme.typography.labelMedium)
    }

    if (showPicker) {
        val state = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val h = state.hour.toString().padStart(2, '0')
                    val m = state.minute.toString().padStart(2, '0')
                    onTimeSelected("$h:$m")
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Avbryt") }
            },
            text = { TimePicker(state = state) }
        )
    }
}
