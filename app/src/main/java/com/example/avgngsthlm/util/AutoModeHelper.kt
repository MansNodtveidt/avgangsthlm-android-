package com.example.avgngsthlm.util

import android.annotation.SuppressLint
import com.example.avgngsthlm.data.local.entity.AutoRule
import java.util.Calendar

object AutoModeHelper {

    /** Returnerar den regel som matchar aktuell tid/dag, eller null. */
    fun findActiveRule(rules: List<AutoRule>): AutoRule? {
        val cal = Calendar.getInstance()
        val currentDay = calendarDayToIndex(cal.get(Calendar.DAY_OF_WEEK))
        val currentTime = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        return rules.firstOrNull { rule ->
            val days = rule.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
            currentDay in days && currentTime >= rule.startTime && currentTime <= rule.endTime
        }
    }

    /** Returnerar favoriteId för den regel som matchar aktuell tid/dag, eller null. */
    fun findActiveFavoriteId(rules: List<AutoRule>): Int? = findActiveRule(rules)?.favoriteId

    /** Calendar.DAY_OF_WEEK: SUN=1, MON=2 ... SAT=7 → vår konvention: MÅN=1 ... SÖN=7 */
    private fun calendarDayToIndex(calDay: Int): Int = when (calDay) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }

    /** Veckodagars visningsnamn (index 1–7 = mån–sön) */
    val DAY_NAMES = listOf("Mån", "Tis", "Ons", "Tor", "Fre", "Lör", "Sön")

    fun daysString(daysOfWeek: String): String {
        val days = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
        return when (days) {
            listOf(1, 2, 3, 4, 5, 6, 7) -> "Alla dagar"
            listOf(1, 2, 3, 4, 5)        -> "Mån–Fre"
            listOf(6, 7)                 -> "Lör–Sön"
            else -> days.mapNotNull { DAY_NAMES.getOrNull(it - 1) }.joinToString(", ")
        }
    }

    @SuppressLint("DefaultLocale")
    fun currentTimeString(): String {
        val cal = Calendar.getInstance()
        return String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
}
