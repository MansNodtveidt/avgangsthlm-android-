package com.example.avgngsthlm.data.remote

import com.example.avgngsthlm.data.remote.model.DeparturesResponse
import com.example.avgngsthlm.data.remote.model.PlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SLApiService {

    @GET("departures")
    suspend fun getDepartures(
        @Query("siteId") siteId: String,
        @Query("maxJourneys") maxJourneys: Int = ApiConstants.MAX_JOURNEYS
    ): DeparturesResponse

    @GET("stops")
    suspend fun searchLocations(
        @Query("q") query: String
    ): PlacesResponse
}
