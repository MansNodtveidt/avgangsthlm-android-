package com.example.avgngsthlm.ui.screens.automode

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.avgngsthlm.data.AppSettings
import com.example.avgngsthlm.data.local.db.AppDatabase
import com.example.avgngsthlm.data.local.entity.AutoRule
import com.example.avgngsthlm.data.local.entity.Favorite
import com.example.avgngsthlm.data.repository.AutoRuleRepository
import com.example.avgngsthlm.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AutoModeUiState(
    val rules: List<AutoRule> = emptyList(),
    val favorites: List<Favorite> = emptyList(),
    val autoModeEnabled: Boolean = false
)

class AutoModeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val ruleRepo = AutoRuleRepository(db.autoRuleDao())
    private val favoriteRepo = FavoriteRepository(db.favoriteDao())

    val uiState: StateFlow<AutoModeUiState> = combine(
        ruleRepo.allRules,
        favoriteRepo.allFavorites,
        AppSettings.autoModeFlow(app)
    ) { rules, favorites, autoMode ->
        AutoModeUiState(rules = rules, favorites = favorites, autoModeEnabled = autoMode)
    }.stateIn(viewModelScope, SharingStarted.Lazily, AutoModeUiState())

    fun setAutoModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            AppSettings.setAutoModeEnabled(getApplication(), enabled)
        }
    }

    fun addRule(name: String, favoriteId: Int, daysOfWeek: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            ruleRepo.insert(
                AutoRule(
                    name = name,
                    favoriteId = favoriteId,
                    daysOfWeek = daysOfWeek,
                    startTime = startTime,
                    endTime = endTime
                )
            )
        }
    }

    fun updateRule(rule: AutoRule) {
        viewModelScope.launch { ruleRepo.update(rule) }
    }

    fun deleteRule(rule: AutoRule) {
        viewModelScope.launch { ruleRepo.delete(rule) }
    }
}
