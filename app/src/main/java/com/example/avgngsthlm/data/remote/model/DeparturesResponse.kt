package com.example.avgngsthlm.data.remote.model

import com.google.gson.annotations.SerializedName

data class DeparturesResponse(
    @SerializedName("Departure") val departures: List<Departure>? = null,
    @SerializedName("errorCode") val errorCode: String? = null,
    @SerializedName("errorText") val errorText: String? = null
)

data class Departure(
    @SerializedName("name") val name: String = "",
    @SerializedName("transportNumber") val transportNumber: String? = null,
    @SerializedName("stop") val stop: String = "",
    @SerializedName("stopid") val stopId: String = "",
    @SerializedName("time") val time: String = "",       // scheduled "HH:MM:SS"
    @SerializedName("date") val date: String = "",
    @SerializedName("rtTime") val rtTime: String? = null, // realtime "HH:MM:SS", null if on schedule
    @SerializedName("rtDate") val rtDate: String? = null,
    @SerializedName("cancelled") val cancelled: Boolean = false,
    @SerializedName("direction") val direction: String? = null,
    @SerializedName("Product") val products: List<Product>? = null
) {
    fun formattedTime(): String = if (time.length >= 5) time.substring(0, 5) else time
}

data class Product(
    @SerializedName("name") val name: String? = null,
    @SerializedName("num") val num: String? = null,
    @SerializedName("catOutS") val category: String? = null,
    @SerializedName("catOutL") val categoryLong: String? = null
)
