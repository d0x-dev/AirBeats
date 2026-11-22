/*
 * Copyright (C) 2026 DxV Studio ~~ Dark
 *
 * Originally created by DxV Studio ~~ Dark.
 * Credits: DxV Studio, AirBeats, and Dark.
 *
 * This file is part of iTune and is licensed under the GNU General Public License v3.0 (GPL-3.0).
 * Any copying, modification, or redistribution of this code must strictly comply with the
 * terms of the project LICENSE file and retain full author credit.
 */

package com.darkxvenom.itune.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.darkxvenom.airbeats.R

enum class AppFont(val label: String, val key: String) {
    SYSTEM("System Default", "system"),
    SF_PRO("SF Pro Display", "sf_pro"),
    LINOTTE("Linotte", "linotte"),
    POPPINS("Poppins", "poppins"),
    SANS_SERIF("Sans Serif", "sans_serif"),
    SERIF("Serif", "serif"),
    MONOSPACE("Monospace", "monospace");
    // Originally created by DxV Studio ~~ Dark | Credits: DxV Studio, AirBeats, Dark — Licensed under GPL-3.0.

    fun family(): FontFamily = when (this) {
        SYSTEM -> FontFamily.Default
        SF_PRO -> FontFamily.SansSerif
        LINOTTE -> FontFamily(Font(R.font.linotte))
        POPPINS -> FontFamily(
            Font(R.font.poppins_regular, FontWeight.Normal),
            Font(R.font.poppins_medium, FontWeight.Medium),
            Font(R.font.poppins_bold, FontWeight.Bold),
        )
        SANS_SERIF -> FontFamily.SansSerif
        SERIF -> FontFamily.Serif
        MONOSPACE -> FontFamily.Monospace
    }

    companion object {
        fun fromKey(key: String?): AppFont = entries.firstOrNull {
            it.key.equals(key, ignoreCase = true) || it.label.equals(key, ignoreCase = true)
        } ?: SYSTEM
    }
}
