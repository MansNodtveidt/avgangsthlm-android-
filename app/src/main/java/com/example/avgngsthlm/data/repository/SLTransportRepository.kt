package com.example.avgngsthlm.data.repository

import android.util.Log
import com.example.avgngsthlm.data.local.entity.Favorite
import com.example.avgngsthlm.data.remote.RetrofitClient
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val TAG = "SL_API"
private val FMT_HM = DateTimeFormatter.ofPattern("HH:mm")

data class DepartureRow(
    val clockTime: String,          // realtime "HH:mm" (always displayed)
    val scheduledClockTime: String, // scheduled "HH:mm" (for strikethrough when delayed)
    val minutesDisplay: String,     // human-readable "3 min" / "Nu"
    val delayMinutes: Long,
    val isCancelled: Boolean,
    val line: String,
    val direction: String
)

class SLTransportRepository {

    private val api = RetrofitClient.slApiService

    suspend fun getDepartures(favorite: Favorite): Result<List<DepartureRow>> = runCatching {
        val response = api.getDepartures(siteId = favorite.siteId, maxJourneys = 20)
        if (response.errorCode != null) {
            throw Exception("API-fel: ${response.errorText}")
        }
        val all = response.departures.orEmpty()
        Log.d(TAG, "getDepartures: siteId=${favorite.siteId} total=${all.size}")

        all
            .filter { dep ->
                favorite.lineFilter.isNullOrBlank() ||
                    dep.transportNumber?.equals(favorite.lineFilter?.trim(), ignoreCase = true) == true ||
                    dep.products?.any { it.num?.equals(favorite.lineFilter?.trim(), ignoreCase = true) == true } == true
            }
            .filter { dep ->
                favorite.directionFilter.isNullOrBlank() ||
                    dep.direction?.contains(favorite.directionFilter?.trim() ?: "", ignoreCase = true) == true
            }
            .take(3)
            .map { dep ->
                val scheduledClock = dep.time.take(5)           // "HH:MM"
                val realtimeClock  = dep.rtTime?.take(5) ?: scheduledClock
                val delayMinutes   = if (dep.rtTime != null) calcDelayMinutes(dep.time, dep.rtTime) else 0L

                Log.d(TAG, "  line=${dep.transportNumber} sched=$scheduledClock rt=$realtimeClock" +
                    " delay=$delayMinutes cancelled=${dep.cancelled}")

                DepartureRow(
                    clockTime          = realtimeClock,
                    scheduledClockTime = scheduledClock,
                    minutesDisplay     = minutesUntil(realtimeClock),
                    delayMinutes       = delayMinutes,
                    isCancelled        = dep.cancelled,
                    line               = dep.transportNumber ?: dep.products?.firstOrNull()?.num ?: "",
                    direction          = dep.direction ?: ""
                )
            }
    }

    private fun calcDelayMinutes(scheduled: String, realtime: String): Long {
        return try {
            val s = LocalTime.parse(scheduled.take(5), FMT_HM)
            val r = LocalTime.parse(realtime.take(5), FMT_HM)
            var mins = ChronoUnit.MINUTES.between(s, r)
            if (mins < -720) mins += 1440   // handle midnight crossing
            mins
        } catch (_: Exception) { 0L }
    }

    private fun minutesUntil(clockTime: String): String {
        if (clockTime.isEmpty()) return ""
        return try {
            val now = LocalTime.now()
            val dep = LocalTime.parse(clockTime, FMT_HM)
            var minutes = ChronoUnit.MINUTES.between(now, dep)
            if (minutes < 0) minutes += 1440
            when {
                minutes < 1  -> "Nu"
                minutes < 60 -> "$minutes min"
                else         -> ""
            }
        } catch (_: Exception) { "" }
    }
}
