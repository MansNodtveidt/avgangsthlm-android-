package com.example.avgngsthlm.ui.screens.departures

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.avgngsthlm.data.local.db.AppDatabase
import com.example.avgngsthlm.data.local.entity.Favorite
import com.example.avgngsthlm.data.repository.DepartureRow
import com.example.avgngsthlm.data.repository.FavoriteRepository
import com.example.avgngsthlm.data.repository.SLTransportRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class DeparturesUiState(
    val favorite: Favorite? = null,
    val departures: List<DepartureRow> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: String? = null
)

class DeparturesViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val favoriteRepo = FavoriteRepository(db.favoriteDao())
    private val slRepo = SLTransportRepository()

    private val _uiState = MutableStateFlow(DeparturesUiState())
    val uiState: StateFlow<DeparturesUiState> = _uiState.asStateFlow()

    val allFavorites: StateFlow<List<Favorite>> = favoriteRepo.allFavorites
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun load(favorite: Favorite) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, favorite = favorite) }
            slRepo.getDepartures(favorite)
                .onSuccess { rows ->
                    val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    _uiState.update {
                        it.copy(departures = rows, isLoading = false, error = null, lastUpdated = now)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Kunde inte hämta avgångar")
                    }
                }
        }
    }

    fun loadSilent(favorite: Favorite) {
        viewModelScope.launch {
            slRepo.getDepartures(favorite)
                .onSuccess { rows ->
                    val now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    _uiState.update {
                        it.copy(departures = rows, error = null, lastUpdated = now)
                    }
                }
            // On failure keep showing existing data — no error shown
        }
    }

    fun refresh() {
        _uiState.value.favorite?.let { load(it) }
    }
}
