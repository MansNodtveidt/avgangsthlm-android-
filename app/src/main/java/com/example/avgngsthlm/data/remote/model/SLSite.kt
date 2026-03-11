package com.example.avgngsthlm.data.remote.model

data class SLSitesResponse(
    val sites: List<SLSite>? = null
)

data class SLSite(
    val id: Int? = null,
    val name: String? = null
)
