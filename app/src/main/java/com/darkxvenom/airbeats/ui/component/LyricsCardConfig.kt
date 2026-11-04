/*
 * AirBeats Project Original (2026)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.darkxvenom.airbeats.ui.component

import androidx.annotation.StringRes
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.darkxvenom.airbeats.R

// ─────────────────────────────────────────────────────────────────────────────
// Layout styles — each value represents a distinct visual layout for the card
// Adding a new style = add an entry here + composable in LyricsCardLayouts
// ─────────────────────────────────────────────────────────────────────────────

enum class LyricsLayoutStyle(
    val displayName: String,
    @StringRes val descriptionRes: Int,
    @StringRes val titleRes: Int,
) {
    GlassCard(
        displayName = "Glass Card",
        descriptionRes = R.string.ui_extra_liquid_glass_panel,
        titleRes = R.string.lyrics_layout_glass_card,
    ),
    Minimal(
        displayName = "Minimal",
        descriptionRes = R.string.ui_extra_clean_and_distraction_free,
        titleRes = R.string.lyrics_layout_minimal,
    ),
    CoverFocused(
        displayName = "Cover Focus",
        descriptionRes = R.string.ui_extra_prominent_album_cover_art,
        titleRes = R.string.lyrics_layout_cover_focus,
    ),
    Centered(
        displayName = "Centered",
        descriptionRes = R.string.ui_extra_lyrics_front_and_center,
        titleRes = R.string.lyrics_layout_centered,
    ),
    BlurWash(
        displayName = "Blur Wash",
        descriptionRes = R.string.ui_extra_ultra_blurred_backdrop,
        titleRes = R.string.lyrics_layout_blur_wash,
    ),
    StreamingModern(
        displayName = "Streaming",
        descriptionRes = R.string.ui_extra_modern_music_app_aesthetic,
        titleRes = R.string.lyrics_layout_streaming,
    ),
}

// ─────────────────────────────────────────────────────────────────────────────
// Background type for layouts accepting background variations
// ─────────────────────────────────────────────────────────────────────────────

enum class LyricsBackgroundType(val displayName: String) {
    AlbumArt("Album Art"),
    SolidDark("Dark"),
    SolidLight("Light"),
    Gradient("Gradient"),
}

// ─────────────────────────────────────────────────────────────────────────────
// LyricsCardConfig — estado inmutable del usuario.
// Se pasa a LyricsCardByLayout y a LyricsShareCarouselSheet.
// Modifica con .copy(...) para aplicar cambios sin mutación.
// ─────────────────────────────────────────────────────────────────────────────

data class LyricsCardConfig(

    /** Qué template visual se renderiza en la tarjeta */
    val layoutStyle: LyricsLayoutStyle = LyricsLayoutStyle.GlassCard,

    /** Estilo de vidrio/colores/blur; solo los layouts que usan cloudy/liquidGlass lo consumen */
    val glassStyle: LyricsGlassStyle = LyricsGlassStyle.FrostedDark,

    /**
     * Multiplicador sobre el tamaño de fuente calculado automáticamente.
     * Rango recomendado: 0.6f – 1.5f
     */
    val textSizeMultiplier: Float = 1f,

    /** Alineación del bloque de letra */
    val textAlign: TextAlign = TextAlign.Center,

    /** Visibilidad de elementos dentro de la tarjeta */
    val showTitle: Boolean = true,
    val showArtist: Boolean = true,
    val showCoverArt: Boolean = true,
    val showBranding: Boolean = true,

    /** Tipo de fondo (consumido por Minimal y StreamingModern) */
    val backgroundType: LyricsBackgroundType = LyricsBackgroundType.AlbumArt,

    /**
     * Padding interno de la tarjeta.
     * Rango recomendado: 12.dp – 36.dp
     */
    val cardPadding: Dp = 24.dp,
)
