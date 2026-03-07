package com.example.avgngsthlm.data.remote.model

data class LineInfo(
    val lineNumber: String,
    val category: String?,     // "MT", "BUS", "TRAM", "TRAIN", "FERRY"
    val categoryLong: String?  // "Metro", "Buss", etc.
) {
    val displayName: String
        get() {
            val type = friendlyCategory(category) ?: categoryLong ?: ""
            return if (type.isNotEmpty()) "$lineNumber · $type" else lineNumber
        }

    companion object {
        fun friendlyCategory(code: String?): String? = when (code?.uppercase()) {
            // ResRobot-koder
            "MT" -> "Tunnelbana"
            "BUS", "BUS1", "BUS2" -> "Buss"
            "TRAM", "ST" -> "Spårväg"
            "TRAIN", "PT" -> "Pendeltåg"
            "FERRY", "SHIP" -> "Båt"
            // SL Transport API transport_mode-värden
            "METRO" -> "Tunnelbana"
            "RAIL" -> "Pendeltåg"
            else -> null
        }
    }
}
