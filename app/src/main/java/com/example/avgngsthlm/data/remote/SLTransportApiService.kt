package com.example.avgngsthlm.data.remote

import com.example.avgngsthlm.data.remote.model.SLTransportDeparturesResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SLTransportApiService {

    /**
     * SL Transport API – hämtar avgångar för en hållplats.
     * Kräver ingen API-nyckel.
     *
     * @param siteId SiteId från SL Platsuppslag (t.ex. "9001" för T-Centralen)
     * @param forecast Antal minuter framåt att hämta avgångar för (default 60)
     */
    @GET("sites/{siteId}/departures")
    suspend fun getDepartures(
        @Path("siteId") siteId: String,
        @Query("forecast") forecast: Int = 60
    ): SLTransportDeparturesResponse
}
