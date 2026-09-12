package com.darkxvenom.airbeats.extensions

inline fun <T> tryOrNull(block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        null
    }
