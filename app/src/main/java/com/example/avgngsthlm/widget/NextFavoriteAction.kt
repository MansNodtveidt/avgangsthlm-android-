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

class NextFavoriteAction : ActionCallback {
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
        val next = (current + 1) % favorites.size
        prefs.edit().putInt("widget_favorite_index", next).apply()
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<com.example.avgngsthlm.worker.WidgetUpdateWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        )
    }
}
