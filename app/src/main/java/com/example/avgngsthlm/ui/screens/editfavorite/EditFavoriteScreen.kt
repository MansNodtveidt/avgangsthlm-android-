package com.example.avgngsthlm.ui.screens.editfavorite

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFavoriteScreen(
    onBack: () -> Unit,
    vm: EditFavoriteViewModel = viewModel()
) {
    val state by vm.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Redigera favorit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                }
            )
        }
    ) { padding ->
        if (!state.loaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Namn (överst — enklast att redigera)
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::onNameChange,
                label = { Text("Namn på favorit") },
                placeholder = { Text("T.ex. Skola, Jobb, Hem, Träning") },
                singleLine = true,
                isError = state.name.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // Hållplats (skrivskyddad)
            Text(
                "Hållplats",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.stopName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Text(
                "Hållplatsen kan inte ändras. Skapa en ny favorit för att byta hållplats.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            OutlinedTextField(
                value = state.lineFilter,
                onValueChange = vm::onLineFilterChange,
                label = { Text("Linjefilter (valfritt)") },
                placeholder = { Text("T.ex. 17, 4, 42") },
                supportingText = { Text("Lämna tomt för att visa alla linjer") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.directionFilter,
                onValueChange = vm::onDirectionFilterChange,
                label = { Text("Riktningsfilter (valfritt)") },
                placeholder = { Text("T.ex. Djurgården, Centralen") },
                supportingText = { Text("Lämna tomt för alla riktningar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) { Text("Avbryt") }

                Button(
                    onClick = vm::save,
                    enabled = !state.isSaving && state.name.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Spara ändringar")
                }
            }
        }
    }
}
