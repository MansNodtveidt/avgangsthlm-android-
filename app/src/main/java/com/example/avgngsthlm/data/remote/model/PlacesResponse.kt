package com.example.avgngsthlm.data.remote.model

import com.google.gson.annotations.SerializedName

data class PlacesResponse(
    /** ResRobot v2.1 – inlinkat format: {"stopLocationOrCoordLocation": [{"StopLocation": {...}}]} */
    @SerializedName("stopLocationOrCoordLocation") val locations: List<LocationWrapper>? = null,
    /** ResRobot – platt format: {"StopLocation": [{...}]} */
    @SerializedName("StopLocation") val stopLocations: List<StopLocation>? = null
)

data class LocationWrapper(
    @SerializedName("StopLocation") val stopLocation: StopLocation? = null
)

data class StopLocation(
    @SerializedName("id") val id: String = "",
    @SerializedName("extId") val extId: String? = null,
    @SerializedName("name") val name: String = "",
    @SerializedName("lon") val lon: Double? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("products") val products: Int? = null
)
