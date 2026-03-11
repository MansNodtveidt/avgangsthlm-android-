package com.example.avgngsthlm.data.remote

import com.example.avgngsthlm.data.remote.model.SLSitesResponse
import com.example.avgngsthlm.data.remote.model.SLTransportDeparturesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SLTransportService {

    @GET("sites")
    suspend fun searchSites(@Query("q") query: String): SLSitesResponse

    @GET("sites/{siteId}/departures")
    suspend fun getDepartures(
        @Path("siteId") siteId: Int,
        @Query("forecast") forecast: Int = 60
    ): SLTransportDeparturesResponse
}
