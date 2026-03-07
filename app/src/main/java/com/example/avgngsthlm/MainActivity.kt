package com.example.avgngsthlm

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.work.*
import com.example.avgngsthlm.ui.navigation.AppNavigation
import com.example.avgngsthlm.ui.theme.AvgångSthlmTheme
import com.example.avgngsthlm.worker.WidgetUpdateWorker
import java.util.concurrent.TimeUnit

private const val IMMEDIATE_WORK_NAME = "widget_update_immediate"

private const val PREFS_ONBOARDING = "onboarding"
private const val KEY_BATTERY_DIALOG = "hasShownBatteryDialog"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("API", "BASE_URL = ${BuildConfig.BACKEND_URL}")
        scheduleWidgetUpdates()
        triggerImmediateUpdate()
        enableEdgeToEdge()
        setContent {
            AvgångSthlmTheme {
                val showBatteryDialog = remember { mutableStateOf(!batteryDialogShown()) }

                AppNavigation()

                if (showBatteryDialog.value) {
                    BatteryOptimizationDialog(
                        onOpenSettings = {
                            markBatteryDialogShown()
                            showBatteryDialog.value = false
                            launchBatteryOptimizationRequest()
                        },
                        onDismiss = {
                            markBatteryDialogShown()
                            showBatteryDialog.value = false
                        }
                    )
                }
            }
        }
    }

    private fun batteryDialogShown(): Boolean =
        getSharedPreferences(PREFS_ONBOARDING, MODE_PRIVATE)
            .getBoolean(KEY_BATTERY_DIALOG, false)

    private fun markBatteryDialogShown() {
        getSharedPreferences(PREFS_ONBOARDING, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BATTERY_DIALOG, true)
            .apply()
    }

    private fun launchBatteryOptimizationRequest() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${packageName}")
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Fallback på enheter som inte stöder den direkta dialogen
            startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        }
    }

    private fun triggerImmediateUpdate() {
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleWidgetUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            WidgetUpdateWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

@Composable
private fun BatteryOptimizationDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aktivera widget-uppdateringar") },
        text = {
            Text(
                "För att widgeten ska visa aktuella avgångar behöver appen " +
                "få köra i bakgrunden. Tryck på \"Öppna inställningar\" och välj " +
                "Batteri → Ingen begränsning."
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Öppna inställningar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Senare")
            }
        }
    )
}
