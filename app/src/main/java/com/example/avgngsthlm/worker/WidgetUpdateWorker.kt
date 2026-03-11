package com.example.avgngsthlm.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.avgngsthlm.R
import com.example.avgngsthlm.data.AppSettings
import com.example.avgngsthlm.data.local.db.AppDatabase
import com.example.avgngsthlm.data.local.entity.Favorite
import com.example.avgngsthlm.data.repository.AutoRuleRepository
import com.example.avgngsthlm.data.repository.FavoriteRepository
import com.example.avgngsthlm.data.repository.SLTransportRepository
import com.example.avgngsthlm.util.AutoModeHelper
import com.example.avgngsthlm.widget.AvgangWidget
import com.example.avgngsthlm.widget.WidgetKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class WidgetUpdateWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "widget_update_periodic"
        private const val TAG = "AvgangWidget"
        private const val CHANNEL_ID = "widget_update"
        private const val NOTIFICATION_ID = 1001
    }

    private val db = AppDatabase.getInstance(context)
    private val favoriteRepo = FavoriteRepository(db.favoriteDao())
    private val ruleRepo = AutoRuleRepository(db.autoRuleDao())
    private val slTransportRepo = SLTransportRepository()

    /**
     * Required for WorkManager to promote this worker to a foreground service
     * when the system (e.g. Samsung on mobile data) would otherwise block
     * background network access.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Uppdaterar avgångar")
            .setSmallIcon(R.drawable.ic_refresh)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        val manager = applicationContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Widget-uppdateringar",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { setShowBadge(false) }
            )
        }
    }

    override suspend fun doWork(): Result {
        // Promote to foreground service so Samsung's background network
        // restrictions don't block DNS / socket on mobile data.
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.w(TAG, "setForeground skipped: ${e.message}")
        }

        return withContext(Dispatchers.IO) {
            Log.d(TAG, "doWork: started (attempt ${runAttemptCount + 1})")
            try {
                val favorites = favoriteRepo.allFavorites.first()
                val (favorite, activeRuleName) = resolveActiveFavorite(favorites)
                if (favorite == null) {
                    Log.w(TAG, "doWork: no active favorite found")
                    updateWidgetError("Ingen favorit vald", "Öppna appen för att välja")
                    return@withContext Result.failure()
                }
                Log.d(TAG, "doWork: using favorite '${favorite.name}' siteId=${favorite.siteId} line=${favorite.lineFilter} dir=${favorite.directionFilter}")
                fetchAndUpdateWidget(favorite, favorites.size, activeRuleName)
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "doWork: unexpected exception — ${e.message}", e)
                updateWidgetError("Kunde inte hämta data", "Försök igen senare")
                Result.retry()
            }
        }
    }

    /** Returns the favorite to display and the active rule's name (empty string if none). */
    private suspend fun resolveActiveFavorite(favorites: List<Favorite>): Pair<Favorite?, String> {
        val autoEnabled = AppSettings.isAutoModeEnabled(context)
        Log.d(TAG, "resolveActiveFavorite: autoEnabled=$autoEnabled, ${favorites.size} favorites in DB")

        return if (autoEnabled) {
            val rules = ruleRepo.allRules.first()
            val activeRule = AutoModeHelper.findActiveRule(rules)
            val found = favorites.firstOrNull { it.id == activeRule?.favoriteId }
                ?: favorites.firstOrNull { it.id == AppSettings.getSelectedFavoriteId(context) }
            Log.d(TAG, "resolveActiveFavorite: auto-mode activeRule=${activeRule?.id} → found=${found?.name}")
            found to (activeRule?.name?.takeIf { it.isNotEmpty() } ?: "")
        } else {
            val widgetPrefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val idx = widgetPrefs.getInt("widget_favorite_index", 0)
                .coerceIn(0, (favorites.size - 1).coerceAtLeast(0))
            val found = favorites.getOrNull(idx) ?: favorites.firstOrNull()
            Log.d(TAG, "resolveActiveFavorite: manual idx=$idx → found=${found?.name} siteId=${found?.siteId}")
            found to ""
        }
    }

    private suspend fun fetchAndUpdateWidget(favorite: Favorite, favoritesCount: Int, activeRuleName: String) {
        val autoEnabled = AppSettings.isAutoModeEnabled(context)
        val now = AutoModeHelper.currentTimeString()

        Log.d(TAG, "fetchAndUpdateWidget: GET SL Transport departures for '${favorite.name}'")

        slTransportRepo.getDepartures(favorite).onSuccess { departures ->
            Log.d(TAG, "fetchAndUpdateWidget: success — ${departures.size} departures")
            if (departures.isEmpty()) {
                Log.w(TAG, "fetchAndUpdateWidget: 0 departures after filtering (line=${favorite.lineFilter} dir=${favorite.directionFilter})")
                updateWidgetError("Inga avgångar", "Inga fler avgångar idag")
                return@onSuccess
            }
            val next     = departures.getOrNull(0)
            val nextNext = departures.getOrNull(1)
            val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(AvgangWidget::class.java)
            Log.d(TAG, "fetchAndUpdateWidget: updating ${glanceIds.size} widget instance(s)")
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                    prefs.toMutablePreferences().apply {
                        this[WidgetKeys.NEXT_TIME]           = next?.clockTime ?: ""
                        this[WidgetKeys.NEXT_NEXT_TIME]      = nextNext?.clockTime ?: ""
                        this[WidgetKeys.NEXT_DELAY]          = next?.delayMinutes?.toInt() ?: 0
                        this[WidgetKeys.NEXT_NEXT_DELAY]     = nextNext?.delayMinutes?.toInt() ?: 0
                        this[WidgetKeys.NEXT_CANCELLED]      = next?.isCancelled ?: false
                        this[WidgetKeys.NEXT_NEXT_CANCELLED] = nextNext?.isCancelled ?: false
                        this[WidgetKeys.FAVORITE_NAME]       = favorite.name
                        this[WidgetKeys.STOP_NAME]           = favorite.stopName
                        this[WidgetKeys.LINE]                = favorite.lineFilter ?: ""
                        this[WidgetKeys.DIRECTION]           = favorite.directionFilter ?: ""
                        this[WidgetKeys.LAST_UPDATED]        = now
                        this[WidgetKeys.AUTO_MODE_ACTIVE]    = autoEnabled
                        this[WidgetKeys.AUTO_MODE_LABEL]     = if (autoEnabled) activeRuleName.ifEmpty { favorite.name } else ""
                        this[WidgetKeys.FAVORITE_COUNT]      = favoritesCount
                        remove(WidgetKeys.ERROR)
                    }
                }
                AvgangWidget().update(context, glanceId)
            }
        }.onFailure { e ->
            Log.e(TAG, "fetchAndUpdateWidget: getDepartures failed — ${e.message}", e)
            val (title, sub) = when (e) {
                is java.net.UnknownHostException,
                is java.net.ConnectException,
                is java.net.SocketTimeoutException -> "Ingen anslutning" to "Kontrollera internet"
                else -> "Kunde inte hämta data" to "Försök igen senare"
            }
            updateWidgetError(title, sub)
        }
    }

    private suspend fun updateWidgetError(message: String, subtext: String = "") {
        Log.w(TAG, "updateWidgetError: $message")
        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(AvgangWidget::class.java)
        glanceIds.forEach { glanceId ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[WidgetKeys.ERROR] = message
                    this[WidgetKeys.ERROR_SUBTEXT] = subtext
                }
            }
            AvgangWidget().update(context, glanceId)
        }
    }
}
