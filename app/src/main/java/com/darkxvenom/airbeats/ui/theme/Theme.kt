package com.darkxvenom.airbeats.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.darkxvenom.airbeats.constants.PlayerBackgroundStyle
import com.google.material.color.dynamiccolor.DynamicScheme
import com.google.material.color.hct.Hct
import com.google.material.color.scheme.SchemeTonalSpot
import com.google.material.color.score.Score
import androidx.compose.runtime.saveable.Saver
import com.darkxvenom.airbeats.constants.AppFont

val DefaultThemeColor = Color(0xFF4285F4)

val ColorSaver = Saver<Color, Int>(
    save = { it.toArgb() },
    restore = { Color(it) }
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AirBeatsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    expressive: Boolean = true,
    appFont: AppFont = AppFont.LINOTTE,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val typography = remember(appFont) {
        buildAppTypography(appFont)
    }

    val colorScheme = remember(darkTheme, pureBlack, themeColor) {
        if (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) {
                dynamicDarkColorScheme(context).pureBlack(pureBlack, darkTheme)
            } else {
                dynamicLightColorScheme(context).pureBlack(false, darkTheme)
            }
        } else {
            SchemeTonalSpot(Hct.fromInt(themeColor.toArgb()), darkTheme, 0.0)
                .toColorScheme()
                .pureBlack(pureBlack, darkTheme)
        }
    }

    val motionScheme = if (expressive) {
        MotionScheme.expressive()
    } else {
        MotionScheme.standard()
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = MaterialTheme.shapes,
        motionScheme = motionScheme,
        content = content
    )
}

fun Bitmap.toSoftwareBitmap(): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, false) ?: this
    } else {
        this
    }
}

fun Bitmap.extractThemeColor(): Color {
    val softwareBitmap = toSoftwareBitmap()
    val colorsToPopulation = Palette.from(softwareBitmap)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val softwareBitmap = toSoftwareBitmap()
    val extractedColors = Palette.from(softwareBitmap)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xFF4285F4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

object PlayerColorExtractor {
    object Config {
        const val MAX_COLOR_COUNT = 32
        const val BITMAP_AREA = 8000
        const val IMAGE_SIZE = 200
    }

    fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int = Color(0xFF595959).toArgb()
    ): List<Color> {
        val extractedColors = palette.swatches
            .associate { it.rgb to it.population }

        val orderedColors = Score.score(extractedColors, 2, fallbackColor, true)
            .sortedByDescending { Color(it).luminance() }

        return if (orderedColors.size >= 2) {
            listOf(Color(orderedColors[0]), Color(orderedColors[1]))
        } else {
            listOf(Color(0xFF595959), Color(0xFF0D0D0D))
        }
    }
}

object PlayerSliderColors {
    @Composable
    fun getSliderColors(
        textButtonColor: Color,
        playerBackground: PlayerBackgroundStyle,
        useDarkTheme: Boolean,
    ) = androidx.compose.material3.SliderDefaults.colors(
        thumbColor = if (playerBackground != PlayerBackgroundStyle.DEFAULT || useDarkTheme) textButtonColor else MaterialTheme.colorScheme.primary,
        activeTrackColor = if (playerBackground != PlayerBackgroundStyle.DEFAULT || useDarkTheme) textButtonColor else MaterialTheme.colorScheme.primary,
        inactiveTrackColor = if (playerBackground != PlayerBackgroundStyle.DEFAULT || useDarkTheme) textButtonColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
    )
}

fun DynamicScheme.toColorScheme() =
    ColorScheme(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(primary),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainerLowest = Color(surfaceContainerLowest),
    )

fun ColorScheme.pureBlack(
    pureBlack: Boolean,
    darkTheme: Boolean
): ColorScheme {
    if (pureBlack && darkTheme) {
        return copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF161616),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF121212),
            surfaceContainerHigh = Color(0xFF1E1E1E),
            surfaceContainerHighest = Color(0xFF262626),
        )
    }
    return this
}
