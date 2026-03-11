package com.example.avgngsthlm.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val siteId: String,
    val stopName: String,
    val lineFilter: String? = null,
    val directionFilter: String? = null,
    val slSiteId: String? = null
)
