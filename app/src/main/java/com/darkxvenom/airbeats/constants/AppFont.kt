package com.darkxvenom.airbeats.constants

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.darkxvenom.airbeats.R

enum class AppFont(val title: String, val key: String) {
    SYSTEM("System Default", "system"),
    LINOTTE("Linotte", "linotte"),
    POPPINS("Poppins", "poppins"),
    SF_PRO("SF Pro Display", "sf_pro"),
    ANYBODY("Anybody", "anybody"),
    GREAT_VIBES("Great Vibes", "great_vibes"),
    SANS_SERIF("Sans-Serif", "sans_serif"),
    SERIF("Serif", "serif"),
    MONOSPACE("Monospace", "monospace"),
    CURSIVE("Cursive", "cursive");

    fun getFontFamily(): FontFamily {
        return when (this) {
            SYSTEM -> FontFamily.Default
            LINOTTE -> FontFamily(Font(R.font.linotte))
            POPPINS -> FontFamily(
                Font(R.font.poppins_regular, FontWeight.Normal),
                Font(R.font.poppins_medium, FontWeight.Medium),
                Font(R.font.poppins_bold, FontWeight.Bold)
            )
            SF_PRO -> FontFamily(Font(R.font.sfprodisplaybold, FontWeight.Bold))
            ANYBODY -> FontFamily(Font(R.font.anybody))
            GREAT_VIBES -> FontFamily(Font(R.font.great_vibes))
            SANS_SERIF -> FontFamily.SansSerif
            SERIF -> FontFamily.Serif
            MONOSPACE -> FontFamily.Monospace
            CURSIVE -> FontFamily.Cursive
        }
    }

    companion object {
        fun fromKey(key: String?): AppFont {
            return entries.find { it.key == key } ?: LINOTTE
        }
    }
}
