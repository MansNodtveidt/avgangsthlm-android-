package com.example.avgngsthlm.widget

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object WidgetKeys {
    val NEXT_TIME = stringPreferencesKey("w_next_time")
    val NEXT_NEXT_TIME = stringPreferencesKey("w_next_next_time")
    val STOP_NAME = stringPreferencesKey("w_stop_name")
    val FAVORITE_NAME = stringPreferencesKey("w_favorite_name")
    val LINE = stringPreferencesKey("w_line")
    val DIRECTION = stringPreferencesKey("w_direction")
    val LAST_UPDATED = stringPreferencesKey("w_last_updated")
    val ERROR = stringPreferencesKey("w_error")
    val ERROR_SUBTEXT = stringPreferencesKey("w_error_subtext")
    val AUTO_MODE_ACTIVE = booleanPreferencesKey("w_auto_mode_active")
    val AUTO_MODE_LABEL = stringPreferencesKey("w_auto_mode_label")
    val FAVORITE_COUNT = intPreferencesKey("w_favorite_count")
}
