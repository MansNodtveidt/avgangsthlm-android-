package com.example.avgngsthlm.ui.screens.favorites

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.avgngsthlm.data.local.entity.Favorite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onAddFavorite: () -> Unit,
    onHelp: () -> Unit,
    onEditFavorite: (Int) -> Unit,
    vm: FavoritesViewModel = viewModel()
) {
    val favorites by vm.favorites.collectAsState()
    val selectedId by vm.selectedFavoriteId.collectAsState()
    var deleteTarget by remember { mutableStateOf<Favorite?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favoriter") },
                actions = {
                    IconButton(onClick = onHelp) {
                        Icon(Icons.Default.Info, contentDescription = "Hjälp & guide")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFavorite) {
                Icon(Icons.Default.Add, contentDescription = "Lägg till favorit")
            }
        }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Inga favoriter ännu.\nTryck + för att lägga till.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(favorites, key = { it.id }) { fav ->
                    FavoriteItem(
                        favorite = fav,
                        isSelected = fav.id == selectedId,
                        onSelect = { vm.selectFavorite(fav.id) },
                        onEdit = { onEditFavorite(fav.id) },
                        onDelete = { deleteTarget = fav }
                    )
                }
            }
        }
    }

    deleteTarget?.let { fav ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Ta bort favorit") },
            text = { Text("Vill du ta bort \"${fav.name}\"? Tillhörande auto-regler tas också bort.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteFavorite(fav)
                    deleteTarget = null
                }) { Text("Ta bort") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Avbryt") }
            }
        )
    }
}

@Composable
private fun FavoriteItem(
    favorite: Favorite,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onEdit,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSelect) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.RadioButtonChecked
                    else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Välj favorit",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favorite.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = favorite.stopName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val extras = listOfNotNull(
                    favorite.lineFilter?.let { "Linje $it" },
                    favorite.directionFilter?.let { "→ $it" }
                ).joinToString("  ")
                if (extras.isNotEmpty()) {
                    Text(
                        text = extras,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Ta bort",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
