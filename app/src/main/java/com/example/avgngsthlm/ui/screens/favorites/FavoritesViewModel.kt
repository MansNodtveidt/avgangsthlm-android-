package com.example.avgngsthlm.ui.screens.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.avgngsthlm.data.AppSettings
import com.example.avgngsthlm.data.local.db.AppDatabase
import com.example.avgngsthlm.data.local.entity.Favorite
import com.example.avgngsthlm.data.repository.AutoRuleRepository
import com.example.avgngsthlm.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val favoriteRepo = FavoriteRepository(db.favoriteDao())
    private val ruleRepo = AutoRuleRepository(db.autoRuleDao())

    val favorites: StateFlow<List<Favorite>> = favoriteRepo.allFavorites
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedFavoriteId: StateFlow<Int> = AppSettings.selectedFavoriteIdFlow(app)
        .stateIn(viewModelScope, SharingStarted.Lazily, -1)

    val autoModeEnabled: StateFlow<Boolean> = AppSettings.autoModeFlow(app)
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun deleteFavorite(favorite: Favorite) {
        viewModelScope.launch {
            ruleRepo.deleteByFavoriteId(favorite.id)
            favoriteRepo.delete(favorite)
        }
    }

    fun selectFavorite(id: Int) {
        viewModelScope.launch {
            AppSettings.setSelectedFavoriteId(getApplication(), id)
        }
    }
}
