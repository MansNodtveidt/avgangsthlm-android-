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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// Dag/natt-färger via color-resurser (values/ och values-night/)
private val colorSurface = ColorProvider(R.color.widget_surface)
private val colorOnSurface = ColorProvider(R.color.widget_on_surface)
private val colorOnSurfaceVariant = ColorProvider(R.color.widget_on_surface_variant)
private val colorError = ColorProvider(R.color.widget_error)

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
    val favoriteName = prefs[WidgetKeys.FAVORITE_NAME] ?: ""
    val line = prefs[WidgetKeys.LINE] ?: ""
    val direction = prefs[WidgetKeys.DIRECTION] ?: ""
    val lastUpdated = prefs[WidgetKeys.LAST_UPDATED] ?: ""
    val error = prefs[WidgetKeys.ERROR]
    val autoModeActive = prefs[WidgetKeys.AUTO_MODE_ACTIVE] ?: false
    val autoModeLabel = prefs[WidgetKeys.AUTO_MODE_LABEL] ?: ""
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

                // Rad 1 — linje → riktning (liten text)
                val lineRow = buildString {
                    if (line.isNotEmpty()) append("Linje $line")
                    if (direction.isNotEmpty()) {
                        if (line.isNotEmpty()) append(" → ") else append("→ ")
                        append(direction)
                    }
                }
                if (lineRow.isNotEmpty()) {
                    Text(
                        text = lineRow,
                        style = TextStyle(
                            color = colorOnSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))
                }

                // Rad 2 — Nästa (stor text + minuter kvar)
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = if (nextTime.isNotEmpty()) "Nästa: $nextTime" else "Nästa: --:--",
                        style = TextStyle(
                            color = colorOnSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val nextLabel = minutesLabel(nextTime)
                    if (nextLabel.isNotEmpty()) {
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = nextLabel,
                            style = TextStyle(
                                color = colorOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        )
                    }
                }

                // Rad 3 — Sen (medelstor text + minuter kvar)
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(
                        text = if (nextNextTime.isNotEmpty()) "Sen: $nextNextTime" else "Sen: Ingen fler",
                        style = TextStyle(
                            color = colorOnSurface,
                            fontSize = 16.sp
                        )
                    )
                    val nextNextLabel = minutesLabel(nextNextTime)
                    if (nextNextLabel.isNotEmpty()) {
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            text = nextNextLabel,
                            style = TextStyle(
                                color = colorOnSurfaceVariant,
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
                    val footerText = buildString {
                        if (autoModeActive && autoModeLabel.isNotEmpty()) {
                            append("Läge: $autoModeLabel")
                        } else if (favoriteName.isNotEmpty()) {
                            append(favoriteName)
                        }
                        if (lastUpdated.isNotEmpty()) append(" • Uppdaterad $lastUpdated")
                    }
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
