package com.example.avgngsthlm.ui.screens.addfavorite

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFavoriteScreen(
    onBack: () -> Unit,
    vm: AddFavoriteViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()
    val stopQuery by vm.stopQuery.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    BackHandler(enabled = !vm.isFirstStep()) { vm.goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ny favorit") },
                navigationIcon = {
                    IconButton(onClick = { if (vm.isFirstStep()) onBack() else vm.goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Stegindikator ───────────────────────────────────
            StepIndicator(current = state.stepNumber, total = state.totalSteps)

            HorizontalDivider()

            // ── Steginnehåll (animerat) ─────────────────────────
            AnimatedContent(
                targetState = state.step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "wizard_step"
            ) { step ->
                when (step) {
                    WizardStep.STOP -> StopStep(
                        stopQuery = stopQuery,
                        state = state,
                        onQueryChange = vm::onStopQueryChange,
                        onSelectStop = vm::selectStop,
                        onNext = vm::proceedFromStop
                    )
                    WizardStep.LINE -> LineStep(
                        state = state,
                        onSelectLine = vm::selectLine,
                        onNext = vm::proceedFromLine,
                        onRetry = vm::retryCurrentStep
                    )
                    WizardStep.DIRECTION -> DirectionStep(
                        state = state,
                        onSelectDirection = vm::selectDirection,
                        onNext = vm::proceedFromDirection,
                        onRetry = vm::retryCurrentStep
                    )
                    WizardStep.NAME -> NameStep(
                        state = state,
                        onNameChange = vm::onNameChange,
                        onSave = vm::saveFavorite
                    )
                }
            }
        }
    }
}

// ── Stegindikator ────────────────────────────────────────────────────────────

@Composable
private fun StepIndicator(current: Int, total: Int) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Steg $current av $total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            val stepLabel = when (current) {
                1 -> "Välj hållplats"
                2 -> "Välj linje"
                3 -> "Välj riktning"
                4 -> "Namnge favorit"
                else -> ""
            }
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { current.toFloat() / total },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Steg 1 — Hållplats ───────────────────────────────────────────────────────

@Composable
private fun StopStep(
    stopQuery: String,
    state: AddFavoriteUiState,
    onQueryChange: (String) -> Unit,
    onSelectStop: (com.example.avgngsthlm.data.remote.model.StopLocation) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Sök din hållplats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = stopQuery,
                onValueChange = onQueryChange,
                label = { Text("Hållplats") },
                placeholder = { Text("T.ex. T-Centralen, Odenplan…") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (state.isSearchingStop) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                },
                singleLine = true
            )

            if (state.stopResults.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                        items(state.stopResults, key = { it.id }) { stop ->
                            ListItem(
                                headlineContent = { Text(stop.name) },
                                supportingContent = { Text("ID: ${stop.id}", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.clickable { onSelectStop(stop) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            state.selectedStop?.let { stop ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Vald hållplats",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                stop.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            state.error?.let { err ->
                ErrorCard(message = err, onRetry = null)
            }
        }

        BottomNavButtons(
            onNext = onNext,
            nextEnabled = state.selectedStop != null,
            nextLabel = "Nästa"
        )
    }
}

// ── Steg 2 — Linje ───────────────────────────────────────────────────────────

@Composable
private fun LineStep(
    state: AddFavoriteUiState,
    onSelectLine: (com.example.avgngsthlm.data.remote.model.LineInfo) -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                "Välj linje",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Linjer som avgår från ${state.selectedStop?.name ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.isLoadingLines -> LoadingBox("Hämtar linjer…")
                state.error != null -> ErrorCard(message = state.error, onRetry = onRetry)
                state.availableLines.isEmpty() -> EmptyBox("Inga linjer tillgängliga")
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.availableLines, key = { it.lineNumber }) { line ->
                        LineCard(
                            line = line,
                            selected = line.lineNumber == state.selectedLine?.lineNumber,
                            onClick = { onSelectLine(line) }
                        )
                    }
                }
            }
        }

        BottomNavButtons(
            onNext = onNext,
            nextEnabled = state.selectedLine != null && !state.isLoadingLines,
            nextLabel = "Nästa"
        )
    }
}

@Composable
private fun LineCard(
    line: com.example.avgngsthlm.data.remote.model.LineInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(width = 48.dp, height = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = line.lineNumber,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.surface
                    )
                }
            }
            Column {
                Text(
                    text = com.example.avgngsthlm.data.remote.model.LineInfo.friendlyCategory(line.category)
                        ?: line.categoryLong ?: "Linje",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Linje ${line.lineNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Spacer(Modifier.weight(1f))
                RadioButton(selected = true, onClick = null)
            }
        }
    }
}

// ── Steg 3 — Riktning ────────────────────────────────────────────────────────

@Composable
private fun DirectionStep(
    state: AddFavoriteUiState,
    onSelectDirection: (String) -> Unit,
    onNext: () -> Unit,
    onRetry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Välj riktning",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            state.selectedLine?.let { line ->
                Text(
                    "Linje ${line.lineNumber} från ${state.selectedStop?.name ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))

            when {
                state.isLoadingDirections -> LoadingBox("Hämtar riktningar…")
                state.error != null -> ErrorCard(message = state.error, onRetry = onRetry)
                state.availableDirections.isEmpty() -> EmptyBox("Inga riktningar hittades")
                else -> state.availableDirections.forEach { dir ->
                    DirectionCard(
                        direction = dir,
                        selected = dir == state.selectedDirection,
                        onClick = { onSelectDirection(dir) }
                    )
                }
            }
        }

        BottomNavButtons(
            onNext = onNext,
            nextEnabled = state.selectedDirection != null && !state.isLoadingDirections,
            nextLabel = "Nästa"
        )
    }
}

@Composable
private fun DirectionCard(direction: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mot $direction",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            RadioButton(selected = selected, onClick = null)
        }
    }
}

// ── Steg 4 — Namn ────────────────────────────────────────────────────────────

@Composable
private fun NameStep(
    state: AddFavoriteUiState,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Namnge favoriten",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // Sammanfattning
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SummaryRow("Hållplats", state.selectedStop?.name ?: "–")
                    SummaryRow("Linje", state.selectedLine?.displayName ?: "–")
                    SummaryRow("Riktning", state.selectedDirection?.let { "Mot $it" } ?: "Alla riktningar")
                }
            }

            OutlinedTextField(
                value = state.favoriteName,
                onValueChange = onNameChange,
                label = { Text("Namn") },
                placeholder = { Text("T.ex. Skola, Jobb, Hem, Träning") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.error?.contains("namn") == true
            )

            Text(
                "Föreslaget namn baserat på dina val. Du kan ändra det fritt.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            state.error?.let { err ->
                Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onSave,
            enabled = !state.isSaving && state.favoriteName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("Spara favorit")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

// ── Gemensamma komponenter ────────────────────────────────────────────────────

@Composable
private fun BottomNavButtons(
    onNext: () -> Unit,
    nextEnabled: Boolean,
    nextLabel: String
) {
    HorizontalDivider()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Button(onClick = onNext, enabled = nextEnabled) {
            Text(nextLabel)
        }
    }
}

@Composable
private fun LoadingBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyBox(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: (() -> Unit)?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (onRetry != null) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Försök igen")
                }
            }
        }
    }
}
