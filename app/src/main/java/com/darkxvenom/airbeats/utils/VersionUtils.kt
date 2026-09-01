package com.darkxvenom.airbeats.utils

object VersionUtils {
    /**
     * Returns true if [version1] is greater than [version2].
     * Expects standard semantic version strings like "6.0.0", "6.0.10", etc.
     */
    fun isVersionGreater(version1: String, version2: String): Boolean {
        val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 > p2) return true
            if (p1 < p2) return false
        }
        return false // versions are equal
    }
}
