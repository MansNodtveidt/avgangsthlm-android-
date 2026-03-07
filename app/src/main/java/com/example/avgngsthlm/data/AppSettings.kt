package com.example.avgngsthlm.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("app_settings")

object AppSettings {

    private val AUTO_MODE_ENABLED = booleanPreferencesKey("auto_mode_enabled")
    private val SELECTED_FAVORITE_ID = intPreferencesKey("selected_favorite_id")

    fun autoModeFlow(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { it[AUTO_MODE_ENABLED] ?: false }

    fun selectedFavoriteIdFlow(context: Context): Flow<Int> =
        context.settingsDataStore.data.map { it[SELECTED_FAVORITE_ID] ?: -1 }

    suspend fun isAutoModeEnabled(context: Context): Boolean =
        context.settingsDataStore.data.first()[AUTO_MODE_ENABLED] ?: false

    suspend fun setAutoModeEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_MODE_ENABLED] = enabled }
    }

    suspend fun getSelectedFavoriteId(context: Context): Int =
        context.settingsDataStore.data.first()[SELECTED_FAVORITE_ID] ?: -1

    suspend fun setSelectedFavoriteId(context: Context, id: Int) {
        context.settingsDataStore.edit { it[SELECTED_FAVORITE_ID] = id }
    }
}
