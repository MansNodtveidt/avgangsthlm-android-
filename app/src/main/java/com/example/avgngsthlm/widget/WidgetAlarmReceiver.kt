package com.example.avgngsthlm.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.avgngsthlm.worker.WidgetUpdateWorker

class WidgetAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "alarm fired — triggering widget update")
        val request = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueue(request)
        // Self-reschedule so the chain continues even if WorkManager's
        // periodic job was killed by Samsung's battery optimiser
        scheduleAlarm(context)
    }

    companion object {
        private const val TAG = "AvgangWidget"
        private const val INTERVAL_MS = 15 * 60 * 1000L  // 15 minutes

        fun scheduleAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = pendingIntent(context)
            val triggerAt = System.currentTimeMillis() + INTERVAL_MS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                Log.d(TAG, "scheduleAlarm: exact alarm set (+15 min)")
            } else {
                // Fires within ~15 min window, still works in Doze
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                Log.d(TAG, "scheduleAlarm: inexact alarm set (+15 min)")
            }
        }

        private fun pendingIntent(context: Context) = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WidgetAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
