package com.example.avgngsthlm.data.repository

import com.example.avgngsthlm.data.remote.RetrofitClient
import com.example.avgngsthlm.data.remote.model.Departure
import com.example.avgngsthlm.data.remote.model.LineInfo
import com.example.avgngsthlm.data.remote.model.StopLocation

class DeparturesRepository {

    private val api = RetrofitClient.slApiService

    /**
     * Hämtar nästa avgångar för en hållplats.
     * Filtrerar valfritt på linje och riktning (client-side).
     */
    suspend fun getDepartures(
        siteId: String,
        lineFilter: String? = null,
        directionFilter: String? = null,
        limit: Int = 2
    ): Result<List<Departure>> {
        return try {
            val response = api.getDepartures(siteId = siteId, maxJourneys = 5)
            if (response.errorCode != null) {
                return Result.failure(Exception("API-fel: ${response.errorText}"))
            }
            val departures = response.departures.orEmpty()
                .filter { dep -> dep.time.length >= 5 }
                .filter { dep ->
                    val lineMatch = lineFilter.isNullOrBlank() ||
                            dep.transportNumber == lineFilter ||
                            dep.products?.any { it.num == lineFilter } == true
                    val dirMatch = directionFilter.isNullOrBlank() ||
                            dep.direction?.contains(directionFilter, ignoreCase = true) == true
                    lineMatch && dirMatch
                }
                .take(limit)
            Result.success(departures)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Hämtar råa avgångar (upp till [maxJourneys]) utan filtrering.
     * Används internt för linje- och riktningsupptäckt.
     */
    suspend fun fetchRawDepartures(siteId: String, maxJourneys: Int = 40): Result<List<Departure>> {
        return try {
            val response = api.getDepartures(siteId = siteId, maxJourneys = maxJourneys)
            if (response.errorCode != null) {
                return Result.failure(Exception("API-fel: ${response.errorText}"))
            }
            Result.success(response.departures.orEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Extraherar unika linjer från en lista av avgångar.
     * Sorteras numeriskt (17 < 43 < 171).
     */
    fun extractLines(departures: List<Departure>): List<LineInfo> {
        return departures
            .groupBy { dep ->
                dep.transportNumber
                    ?: dep.products?.firstOrNull()?.num
                    ?: ""
            }
            .filterKeys { it.isNotEmpty() }
            .map { (lineNum, deps) ->
                val product = deps.firstOrNull()?.products?.firstOrNull()
                LineInfo(
                    lineNumber = lineNum,
                    category = product?.category,
                    categoryLong = product?.categoryLong
                )
            }
            .sortedWith(compareBy { it.lineNumber.toIntOrNull() ?: Int.MAX_VALUE })
    }

    /**
     * Extraherar unika riktningar för en given linje från avgångslistan.
     */
    fun extractDirections(departures: List<Departure>, lineNumber: String): List<String> {
        return departures
            .filter { dep ->
                dep.transportNumber == lineNumber ||
                        dep.products?.any { it.num == lineNumber } == true
            }
            .mapNotNull { it.direction }
            .distinct()
    }

    /**
     * Söker efter hållplatser via ResRobot location.name.
     *
     * Stöder två svarsformat:
     * - Platt:  {"StopLocation": [{...}]}
     * - Nästlat: {"stopLocationOrCoordLocation": [{"StopLocation": {...}}]}
     *
     * OBS: Använder extId (t.ex. "740000001") som id, vilket är kompatibelt
     * med ResRobot departureBoard.
     */
    suspend fun searchLocations(query: String): Result<List<StopLocation>> {
        return try {
            val response = api.searchLocations(query = query)

            // Platt format har prioritet; fallback till nästlat format
            val raw: List<StopLocation> = response.stopLocations
                ?: response.locations?.mapNotNull { it.stopLocation }
                ?: emptyList()

            val stops = raw
                .filter { it.extId?.isNotEmpty() == true }
                .map { stop ->
                    // Sätt id = extId så att ViewModel:ens stop.id alltid är ResRobot-kompatibelt
                    stop.copy(id = stop.extId ?: stop.id)
                }

            Result.success(stops)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
