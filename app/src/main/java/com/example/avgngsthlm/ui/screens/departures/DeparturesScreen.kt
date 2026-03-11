package com.example.avgngsthlm.ui.screens.departures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.avgngsthlm.data.repository.DepartureRow
import com.example.avgngsthlm.util.cleanStopName
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesScreen(vm: DeparturesViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val favorites by vm.allFavorites.collectAsState()
    val pagerState = rememberPagerState(pageCount = { favorites.size.coerceAtLeast(1) })

    // First load shows spinner; background refreshes every 15 s are silent
    var isFirstLoad by remember { mutableStateOf(true) }
    LaunchedEffect(pagerState.currentPage, favorites) {
        isFirstLoad = true
        while (true) {
            favorites.getOrNull(pagerState.currentPage)?.let { fav ->
                if (isFirstLoad) {
                    vm.load(fav)
                    isFirstLoad = false
                } else {
                    vm.loadSilent(fav)
                }
            }
            delay(15_000)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Avgångar") }) }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Lägg till favoriter för att se avgångar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Departure pager ────────────────────────────────────────
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { _ ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            when {
                                state.error != null -> {
                                    Text(
                                        text = state.error!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                !state.isLoading && state.departures.isEmpty() -> {
                                    Text(
                                        "Inga avgångar hittades",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                else -> {
                                    state.departures.forEachIndexed { index, dep ->
                                        DepartureCard(dep = dep, index = index)
                                    }
                                }
                            }

                            if (!state.lastUpdated.isNullOrEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Uppdaterad ${state.lastUpdated}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ── Page indicator dots ────────────────────────────────────
                if (favorites.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        favorites.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        color = if (index == pagerState.currentPage)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        shape = CircleShape
                                    )
                            )
                            if (index < favorites.size - 1) Spacer(Modifier.width(6.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartureCard(dep: DepartureRow, index: Int) {
    val timeFontSize = when (index) {
        0 -> 24.sp
        1 -> 20.sp
        else -> 17.sp
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Line badge
            if (dep.line.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = dep.line,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Direction + delay
            Column(modifier = Modifier.weight(1f)) {
                val cleanDirection = dep.direction.cleanStopName()
                Text(
                    text = if (cleanDirection.isNotEmpty()) "Mot $cleanDirection" else "",
                    style = MaterialTheme.typography.bodyMedium
                )
                val delayText = when {
                    dep.isCancelled -> "❌ Inställd"
                    dep.delayMinutes >= 2 -> "⚠️ Försenad ${dep.delayMinutes} min"
                    dep.delayMinutes <= -1 -> "🏃 Tidig ${-dep.delayMinutes} min"
                    else -> null
                }
                if (delayText != null) {
                    Text(
                        text = delayText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            dep.isCancelled -> MaterialTheme.colorScheme.error
                            dep.delayMinutes >= 2 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                }
            }

            // Clock time + minutes / delay info
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (dep.isCancelled) "--:--" else dep.clockTime,
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (dep.isCancelled) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface
                )
                if (!dep.isCancelled && dep.delayMinutes >= 2 && dep.scheduledClockTime.isNotEmpty()) {
                    Text(
                        text = dep.scheduledClockTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                } else if (dep.minutesDisplay.isNotEmpty() && !dep.isCancelled) {
                    Text(
                        text = dep.minutesDisplay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
