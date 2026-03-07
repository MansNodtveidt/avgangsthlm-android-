package com.example.avgngsthlm.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Svar från SL Transport API (transport.integration.sl.se/v1/sites/{siteId}/departures).
 * Använder samma SiteId-format (t.ex. "9001") som SL Platsuppslag.
 */
data class SLTransportDeparturesResponse(
    val departures: List<SLDeparture>? = null,
    @SerializedName("stop_deviations") val stopDeviations: List<Any>? = null
)

data class SLDeparture(
    /** Riktning/destination, t.ex. "Stockholms Centralstation" */
    val direction: String? = null,
    @SerializedName("direction_code") val directionCode: Int? = null,
    val destination: String? = null,
    /** EXPECTED / CANCELLED / DEPARTED osv. */
    val state: String? = null,
    /** ISO-8601, t.ex. "2024-01-10T12:00:00" */
    val scheduled: String? = null,
    val expected: String? = null,
    /**
     * Användarvänlig tid: "Nu", "3 min", "12:45".
     * Visas direkt i UI utan konvertering.
     */
    val display: String? = null,
    val line: SLLine? = null,
    @SerializedName("stop_area") val stopArea: SLStopArea? = null
)

data class SLLine(
    val id: Int? = null,
    /** Linjenummer som sträng, t.ex. "17", "65", "19" */
    val designation: String? = null,
    /** Trafikslag: "TRAM", "METRO", "BUS", "RAIL", "FERRY", "SHIP" */
    @SerializedName("transport_mode") val transportMode: String? = null,
    /** Linjegrupp, t.ex. "Spårväg City", "Lidingöbanan" */
    @SerializedName("group_of_lines") val groupOfLines: String? = null
)

data class SLStopArea(
    val id: Int? = null,
    val name: String? = null,
    @SerializedName("transport_mode_code") val transportModeCode: String? = null
)
