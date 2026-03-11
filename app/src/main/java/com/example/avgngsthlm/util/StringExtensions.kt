package com.example.avgngsthlm.util

/** Removes municipality suffixes and transit-type labels from stop names.
 *  E.g. "Liljeholmen T-bana (Stockholm kn)" → "Liljeholmen" */
fun String.cleanStopName(): String =
    replace(Regex("\\s*\\([^)]*kn\\)"), "")
        .replace(Regex("\\s*(T-bana|Tunnelbana|Pendeltåg|Spårväg|Busstation)"), "")
        .trim()
