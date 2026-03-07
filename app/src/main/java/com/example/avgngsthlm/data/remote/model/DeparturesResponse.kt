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
    @SerializedName("time") val time: String = "",       // "12:53:00"
    @SerializedName("date") val date: String = "",
    @SerializedName("direction") val direction: String? = null,
    @SerializedName("Product") val products: List<Product>? = null
) {
    // Returnera tid som "HH:MM"
    fun formattedTime(): String = if (time.length >= 5) time.substring(0, 5) else time
}

data class Product(
    @SerializedName("name") val name: String? = null,
    @SerializedName("num") val num: String? = null,
    @SerializedName("catOutS") val category: String? = null,
    @SerializedName("catOutL") val categoryLong: String? = null
)
