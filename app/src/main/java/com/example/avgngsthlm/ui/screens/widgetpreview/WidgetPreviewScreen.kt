package com.example.avgngsthlm.ui.screens.widgetpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPreviewScreen(vm: WidgetPreviewViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val favorites by vm.allFavorites.collectAsState()
    var selectedIndex by remember { mutableStateOf(0) }

    // Load departure data whenever the selected favorite changes
    LaunchedEffect(selectedIndex, favorites) {
        favorites.getOrNull(selectedIndex)?.let { vm.loadFavorite(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Förhandsvisning av widget") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Så här ser widgeten ut på hemskärmen:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Favorite selector
            when {
                favorites.size >= 2 -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = {
                            selectedIndex = if (selectedIndex > 0) selectedIndex - 1 else favorites.size - 1
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Föregående favorit")
                        }
                        Text(
                            text = favorites.getOrNull(selectedIndex)?.name ?: "Ingen favorit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            selectedIndex = (selectedIndex + 1) % favorites.size
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Nästa favorit")
                        }
                    }
                }
                favorites.size == 1 -> {
                    Text(
                        text = favorites[0].name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                // favorites.isEmpty(): nothing shown — WidgetCard will display "no favorites" state
            }

            // Widget-förhandsvisning
            WidgetCard(state = state)

            // Uppdatera-knapp
            Button(
                onClick = vm::refresh,
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Hämta färsk data")
            }

            state.error?.let { err ->
                Text(
                    err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun minutesUntil(departureTime: String): String {
    if (departureTime.isEmpty()) return ""
    return try {
        val now = LocalTime.now()
        val departure = LocalTime.parse(departureTime, DateTimeFormatter.ofPattern("HH:mm"))
        var minutes = ChronoUnit.MINUTES.between(now, departure)
        if (minutes < 0) minutes += 1440
        when {
            minutes < 1  -> "Nu"
            minutes < 60 -> "$minutes min"
            else         -> ""
        }
    } catch (_: Exception) { "" }
}

@Composable
private fun WidgetCard(state: WidgetPreviewUiState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.error != null && state.favorite == null) {
            Text(
                state.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val fav = state.favorite

                // Rad 1 — linje + riktning
                val lineInfo = listOfNotNull(
                    fav?.lineFilter?.let { "Linje $it" },
                    fav?.directionFilter?.let { "→ $it" }
                ).joinToString("  ")
                if (lineInfo.isNotEmpty()) {
                    Text(
                        text = lineInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Rad 2 — Nästa
                val nextTime = state.nextTime.orEmpty()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (nextTime.isNotEmpty()) "Nästa: $nextTime" else "Nästa: --:--",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val nextLabel = minutesUntil(nextTime)
                    if (nextLabel.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = nextLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Rad 3 — Sen
                val nextNextTime = state.nextNextTime.orEmpty()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (nextNextTime.isNotEmpty()) "Sen: $nextNextTime" else "Sen: Ingen fler",
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val nextNextLabel = minutesUntil(nextNextTime)
                    if (nextNextLabel.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = nextNextLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Rad 4 — favorit-namn + uppdaterad
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val footerText = buildString {
                        append(fav?.name ?: "")
                        if (!state.lastUpdated.isNullOrEmpty()) {
                            append(" • Uppdaterad ${state.lastUpdated}")
                        }
                    }
                    Text(
                        text = footerText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Uppdatera",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
