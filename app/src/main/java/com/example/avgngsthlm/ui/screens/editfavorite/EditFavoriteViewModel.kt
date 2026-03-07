package com.example.avgngsthlm.ui.screens.editfavorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.avgngsthlm.data.local.db.AppDatabase
import com.example.avgngsthlm.data.local.entity.Favorite
import com.example.avgngsthlm.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditFavoriteUiState(
    val name: String = "",
    val stopName: String = "",
    val lineFilter: String = "",
    val directionFilter: String = "",
    val loaded: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false
)

class EditFavoriteViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {

    private val repo = FavoriteRepository(AppDatabase.getInstance(app).favoriteDao())
    private val favoriteId: Int = checkNotNull(savedStateHandle["favoriteId"])
    private var original: Favorite? = null

    private val _uiState = MutableStateFlow(EditFavoriteUiState())
    val uiState: StateFlow<EditFavoriteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val fav = repo.getById(favoriteId) ?: return@launch
            original = fav
            _uiState.update {
                it.copy(
                    name = fav.name,
                    stopName = fav.stopName,
                    lineFilter = fav.lineFilter ?: "",
                    directionFilter = fav.directionFilter ?: "",
                    loaded = true
                )
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update { it.copy(name = v) }
    fun onLineFilterChange(v: String) = _uiState.update { it.copy(lineFilter = v) }
    fun onDirectionFilterChange(v: String) = _uiState.update { it.copy(directionFilter = v) }

    fun save() {
        val orig = original ?: return
        val s = _uiState.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repo.update(
                orig.copy(
                    name = s.name.trim(),
                    lineFilter = s.lineFilter.trim().ifBlank { null },
                    directionFilter = s.directionFilter.trim().ifBlank { null }
                )
            )
            _uiState.update { it.copy(isSaving = false, saved = true) }
        }
    }
}
