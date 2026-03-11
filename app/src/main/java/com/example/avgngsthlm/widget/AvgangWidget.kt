package com.example.avgngsthlm.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.avgngsthlm.MainActivity
import com.example.avgngsthlm.R
import com.example.avgngsthlm.util.cleanStopName
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Dag/natt-färger via color-resurser (values/ och values-night/)
private val colorSurface = ColorProvider(R.color.widget_surface)
private val colorOnSurface = ColorProvider(R.color.widget_on_surface)
private val colorOnSurfaceVariant = ColorProvider(R.color.widget_on_surface_variant)
private val colorError = ColorProvider(R.color.widget_error)
private val colorWarning = ColorProvider(R.color.widget_warning)

class AvgangWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            WidgetContent(prefs)
        }
    }
}

/** Returns a minutes-remaining label for a "HH:mm" departure string. */
private fun minutesLabel(timeStr: String): String {
    if (timeStr.isEmpty() || timeStr == "--:--") return ""
    return try {
        val fmt  = DateTimeFormatter.ofPattern("HH:mm")
        val now  = LocalTime.now()
        val dep  = LocalTime.parse(timeStr, fmt)
        val mins = ChronoUnit.MINUTES.between(now, dep).let { if (it < 0) it + 1440 else it }
        when {
            mins < 1  -> "Nu"
            mins < 60 -> "$mins min"
            else      -> ""
        }
    } catch (_: Exception) {
        ""
    }
}

@Composable
private fun WidgetContent(prefs: Preferences) {
    val nextTime = prefs[WidgetKeys.NEXT_TIME] ?: ""
    val nextNextTime = prefs[WidgetKeys.NEXT_NEXT_TIME] ?: ""
    val nextDelay = prefs[WidgetKeys.NEXT_DELAY] ?: 0
    val nextNextDelay = prefs[WidgetKeys.NEXT_NEXT_DELAY] ?: 0
    val nextCancelled = prefs[WidgetKeys.NEXT_CANCELLED] ?: false
    val nextNextCancelled = prefs[WidgetKeys.NEXT_NEXT_CANCELLED] ?: false
    val favoriteName = (prefs[WidgetKeys.FAVORITE_NAME] ?: "").cleanStopName()
    val line = prefs[WidgetKeys.LINE] ?: ""
    val direction = (prefs[WidgetKeys.DIRECTION] ?: "").cleanStopName()
    val lastUpdated = prefs[WidgetKeys.LAST_UPDATED] ?: ""
    val error = prefs[WidgetKeys.ERROR]
    val autoModeActive = prefs[WidgetKeys.AUTO_MODE_ACTIVE] ?: false
    val autoModeLabel = (prefs[WidgetKeys.AUTO_MODE_LABEL] ?: "").cleanStopName()
    val favoriteCount = prefs[WidgetKeys.FAVORITE_COUNT] ?: 1

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colorSurface)
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            if (error != null) {
                val errorSubtext = prefs[WidgetKeys.ERROR_SUBTEXT] ?: ""
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = error,
                        style = TextStyle(
                            color = colorOnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (errorSubtext.isNotEmpty()) {
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = errorSubtext,
                            style = TextStyle(
                                color = colorOnSurfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                    Spacer(GlanceModifier.height(12.dp))
                    Image(
                        provider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = "Uppdatera",
                        modifier = GlanceModifier
                            .size(22.dp)
                            .clickable(actionRunCallback<RefreshWidgetCallback>())
                    )
                }
            } else {
                // Nav-rad — favorit-namn med pilar (visas bara i manuellt läge med 2+ favoriter)
                if (favoriteCount > 1 && !autoModeActive) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_arrow_left),
                            contentDescription = "Föregående",
                            modifier = GlanceModifier
                                .size(32.dp)
                                .clickable(actionRunCallback<PrevFavoriteAction>())
                        )
                        Text(
                            text = favoriteName,
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(
                                color = colorOnSurface,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                        Image(
                            provider = ImageProvider(R.drawable.ic_arrow_right),
                            contentDescription = "Nästa",
                            modifier = GlanceModifier
                                .size(32.dp)
                                .clickable(actionRunCallback<NextFavoriteAction>())
                        )
                    }
                    Spacer(GlanceModifier.height(2.dp))
                }

                // Rad 1 — favorit-namn (visas bara när nav-raden inte är aktiv)
                if (favoriteCount <= 1 || autoModeActive) {
                    val nameLabel = if (autoModeActive && autoModeLabel.isNotEmpty()) autoModeLabel else favoriteName
                    if (nameLabel.isNotEmpty()) {
                        Text(
                            text = nameLabel,
                            style = TextStyle(
                                color = colorOnSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Spacer(GlanceModifier.height(2.dp))
                    }
                }

                // Rad 2 — Nästa (stor text + status)
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    val nextDisplayTime = if (nextCancelled) "--:--"
                                         else if (nextTime.isNotEmpty()) nextTime else "--:--"
                    Text(
                        text = "Nästa: $nextDisplayTime",
                        style = TextStyle(
                            color = if (nextCancelled) colorError else colorOnSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val nextStatusLabel = when {
                        nextCancelled   -> "❌ Inställd"
                        nextDelay >= 2  -> "⚠️ +${nextDelay} min"
                        else            -> minutesLabel(nextTime)
                    }
                    if (nextStatusLabel.isNotEmpty()) {
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = nextStatusLabel,
                            style = TextStyle(
                                color = when {
                                    nextCancelled  -> colorError
                                    nextDelay >= 2 -> colorWarning
                                    else           -> colorOnSurfaceVariant
                                },
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Rad 3 — Sen (medelstor text + status)
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    val nextNextDisplayTime = if (nextNextCancelled) "--:--"
                                             else if (nextNextTime.isNotEmpty()) nextNextTime else ""
                    Text(
                        text = if (nextNextDisplayTime.isNotEmpty()) "Sen: $nextNextDisplayTime" else "Sen: Ingen fler",
                        style = TextStyle(
                            color = if (nextNextCancelled) colorError else colorOnSurface,
                            fontSize = 16.sp
                        )
                    )
                    val nextNextStatusLabel = when {
                        nextNextCancelled   -> "❌ Inställd"
                        nextNextDelay >= 2  -> "⚠️ +${nextNextDelay} min"
                        nextNextTime.isNotEmpty() -> minutesLabel(nextNextTime)
                        else                -> ""
                    }
                    if (nextNextStatusLabel.isNotEmpty()) {
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = nextNextStatusLabel,
                            style = TextStyle(
                                color = when {
                                    nextNextCancelled  -> colorError
                                    nextNextDelay >= 2 -> colorWarning
                                    else               -> colorOnSurfaceVariant
                                },
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                Spacer(GlanceModifier.defaultWeight())

                // Rad 4 — label + uppdaterad + refresh-knapp
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    val footerText = if (lastUpdated.isNotEmpty()) "Uppdaterad $lastUpdated" else ""
                    Text(
                        text = footerText,
                        style = TextStyle(
                            color = colorOnSurfaceVariant,
                            fontSize = 10.sp
                        ),
                        modifier = GlanceModifier.defaultWeight()
                    )
                    Image(
                        provider = ImageProvider(R.drawable.ic_refresh),
                        contentDescription = "Uppdatera",
                        modifier = GlanceModifier
                            .size(18.dp)
                            .clickable(actionRunCallback<RefreshWidgetCallback>())
                    )
                }
            }
        }
    }
}
