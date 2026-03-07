package com.example.avgngsthlm.ui.screens.widgetpreview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.avgngsthlm.data.local.db.AppDatabase
import com.example.avgngsthlm.data.local.entity.Favorite
import com.example.avgngsthlm.data.repository.DeparturesRepository
import com.example.avgngsthlm.data.repository.FavoriteRepository
import com.example.avgngsthlm.util.AutoModeHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WidgetPreviewUiState(
    val favorite: Favorite? = null,
    val nextTime: String? = null,
    val nextNextTime: String? = null,
    val lastUpdated: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class WidgetPreviewViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val favoriteRepo = FavoriteRepository(db.favoriteDao())
    private val departuresRepo = DeparturesRepository()

    private val _uiState = MutableStateFlow(WidgetPreviewUiState())
    val uiState: StateFlow<WidgetPreviewUiState> = _uiState.asStateFlow()

    val allFavorites: StateFlow<List<Favorite>> = favoriteRepo.allFavorites
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun loadFavorite(favorite: Favorite) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            departuresRepo.getDepartures(
                siteId = favorite.siteId,
                lineFilter = favorite.lineFilter,
                directionFilter = favorite.directionFilter
            ).onSuccess { departures ->
                _uiState.update {
                    it.copy(
                        favorite = favorite,
                        nextTime = departures.getOrNull(0)?.formattedTime(),
                        nextNextTime = departures.getOrNull(1)?.formattedTime(),
                        lastUpdated = AutoModeHelper.currentTimeString(),
                        isLoading = false,
                        error = null
                    )
                }
            }.onFailure { _ ->
                _uiState.update {
                    it.copy(isLoading = false, favorite = favorite, error = "Kunde inte hämta avgångar")
                }
            }
        }
    }

    /** Re-fetches departure data for the currently displayed favorite. */
    fun refresh() {
        _uiState.value.favorite?.let { loadFavorite(it) }
    }
}
