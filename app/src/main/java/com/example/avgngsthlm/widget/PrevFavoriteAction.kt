package com.example.avgngsthlm.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.avgngsthlm.data.local.db.AppDatabase
import com.example.avgngsthlm.data.repository.FavoriteRepository
import kotlinx.coroutines.flow.first

class PrevFavoriteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val favorites = FavoriteRepository(AppDatabase.getInstance(context).favoriteDao())
            .allFavorites.first()
        if (favorites.isEmpty()) return
        val current = prefs.getInt("widget_favorite_index", 0)
        val prev = if (current - 1 < 0) favorites.size - 1 else current - 1
        prefs.edit().putInt("widget_favorite_index", prev).apply()
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<com.example.avgngsthlm.worker.WidgetUpdateWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        )
    }
}
