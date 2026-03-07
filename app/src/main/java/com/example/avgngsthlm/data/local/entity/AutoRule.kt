package com.example.avgngsthlm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "auto_rules")
data class AutoRule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String = "",       // e.g. "Morgon till skolan"
    val favoriteId: Int,
    val daysOfWeek: String,  // "1,2,3,4,5" (mån=1 ... sön=7)
    val startTime: String,   // "07:00"
    val endTime: String      // "09:00"
)
