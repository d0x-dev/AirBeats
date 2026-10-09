package com.darkxvenom.airbeats.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.time.LocalDateTime
import java.time.ZoneOffset

val DynamicThemeKey = booleanPreferencesKey("dynamicTheme")
val DynamicBackgroundKey = booleanPreferencesKey("dynamicBackground")
val ThemeAccentColorKey = intPreferencesKey("themeAccentColor")
val ThemeColorEffectKey = stringPreferencesKey("themeColorEffect")

enum class ThemeColorEffect(val title: String, val description: String) {
    NONE("None", "Default balanced tonal appearance"),
    VIBRANT("Vibrant", "High energy & maximum chromatic saturation"),
    EXPRESSIVE("Expressive", "Artistic with playful secondary & tertiary hues"),
    FRUIT_SALAD("Fruit Salad", "Playful complementary fruit palette"),
    RAINBOW("Rainbow", "Spirited spectrum dynamic tones"),
    FIDELITY("Fidelity", "Faithfully mirrors the exact accent color"),
    CONTENT("Content", "Content-focused balanced aesthetic"),
    MONOCHROME("Monochrome", "Sleek modern greyscale tonal styling"),
    NEUTRAL("Neutral", "Quiet, calm, and understated chromatic tones"),
}

val DarkModeKey = stringPreferencesKey("darkMode")
val LiquidGlassKey = booleanPreferencesKey("enableLiquidGlass")
val DynamicIslandKey = booleanPreferencesKey("enableDynamicIsland")
val DynamicIslandOffsetXKey = intPreferencesKey("dynamicIslandOffsetX")
val DynamicIslandOffsetYKey = intPreferencesKey("dynamicIslandOffsetY")
val DynamicIslandWidthKey = intPreferencesKey("dynamicIslandWidth")
val DynamicIslandHeightKey = intPreferencesKey("dynamicIslandHeight")
val DynamicIslandLandscapeOffsetXKey = intPreferencesKey("dynamicIslandLandscapeOffsetX")
val DynamicIslandLandscapeOffsetYKey = intPreferencesKey("dynamicIslandLandscapeOffsetY")
val DynamicIslandLandscapeWidthKey = intPreferencesKey("dynamicIslandLandscapeWidth")
val DynamicIslandLandscapeHeightKey = intPreferencesKey("dynamicIslandLandscapeHeight")
val DynamicIslandBgColorKey = intPreferencesKey("dynamicIslandBgColor")
val DynamicIslandAccentColorKey = intPreferencesKey("dynamicIslandAccentColor")
val DynamicIslandTextColorKey = intPreferencesKey("dynamicIslandTextColor")
val DynamicIslandLiquidGlassKey = booleanPreferencesKey("dynamicIslandLiquidGlass")
val FrostedGlassCardsButtonsKey = booleanPreferencesKey("frostedGlassCardsButtons")

val UserNameKey = stringPreferencesKey("user_name")

val PureBlackKey = booleanPreferencesKey("pureBlack")
val ReduceAnimationsKey = booleanPreferencesKey("reduceAnimations")
val DefaultOpenTabKey = stringPreferencesKey("defaultOpenTab")
val SlimNavBarKey = booleanPreferencesKey("slimNavBar")
val ShowGalaxySliderKey = booleanPreferencesKey("showGalaxySlider")
val GridItemsSizeKey = stringPreferencesKey("gridItemSize")
val SliderStyleKey = stringPreferencesKey("sliderStyle")
val PlayerScreenStyleKey = stringPreferencesKey("playerScreenStyle")
val HomeScreenStyleKey = stringPreferencesKey("homeScreenStyle")
val NavBarStyleKey = stringPreferencesKey("navBarStyle")
val HideVideoKey = booleanPreferencesKey("hideVideo")

val ColourfullPlayerColorKey = intPreferencesKey("colourfullPlayerColor")

enum class SliderStyle {
    DEFAULT,
    SQUIGGLY,
    SLIM,
}

enum class HomeScreenStyle {
    CLASSIC, PLAYFUL, SPOTIFY, APPLE, NEW_CLASSIC, MATERIAL
}

enum class NavBarStyle {
    LIQUID_GLASS, SPOTIFY, APPLE, NEW_CLASSIC, MATERIAL
}

val HiddenHomeSectionsKey = stringSetPreferencesKey("hiddenHomeSections")

enum class MaterialHomeSection(val id: String, val title: String, val subtitle: String) {
    HERO("hero", "Hero Greeting", "Greeting, date, and infinite radio quick start"),
    QUICK_TILES("quick_tiles", "Quick Access", "Quick tiles for favorites, mixes, and recent tracks"),
    TASTE_STRIP("taste_strip", "Taste Strip", "Genre and mood exploration chips"),
    QUICK_PICKS("quick_picks", "Quick Picks", "Personalized recommendations for you"),
    BECAUSE_YOU_LISTEN_TO("because_you_listen_to", "Because You Listen To", "Similar songs based on your listening history"),
    FRESH_FINDS("fresh_finds", "Fresh Finds", "New tracks and undiscovered gems"),
    JUMP_BACK_IN("jump_back_in", "Jump Back In", "Recently played tracks and listening history"),
    MIXES("mixed_for_you", "Mixes To Explore", "Artist radios and endless mixes"),
    SPOTLIGHT("spotlight_hero", "Artist Spotlight", "Featured artist card with instant radio"),
    TOP_ARTISTS("top_artists", "Top Artists", "Your favorite and recommended artists"),
    HEAVY_ROTATION("heavy_rotation", "Heavy Rotation", "Frequently played tracks"),
    ALBUMS("albums_in_rotation", "Albums For You", "Top albums and recommended collections"),
    CHARTS("trending_charts", "Trending Charts", "Top ranked and trending music charts"),
    NEW_RELEASES("new_releases", "New Releases", "Fresh album and single drops");

    companion object {
        fun fromId(id: String?): MaterialHomeSection? =
            entries.firstOrNull { it.id == id }
    }
}

enum class LyricsScreenStyle {
    LYRICS_1,
    LYRICS_2
}

enum class PlayerScreenStyle {
    MATERIAL,
    IOS_STYLED,
    MODERN,
    SPOTIFY,
    CLASSIC,
    APPLE,
    PAPER,
    LIQUID,
    CLOUDGLOW,
    FROST,
    FOLD,
    GROOVE,
    POPSY,
    MINIMAL,
    COLOURFULL,
    GALAXY
}

const val SYSTEM_DEFAULT = "SYSTEM_DEFAULT"
val ContentLanguageKey = stringPreferencesKey("contentLanguage")
val ContentCountryKey = stringPreferencesKey("contentCountry")
val EnableKugouKey = booleanPreferencesKey("enableKugou")
val EnableLrcLibKey = booleanPreferencesKey("enableLrclib")
val EnableSimpMusicLyricsKey = booleanPreferencesKey("enableSimpMusic")
val EnableUnisonLyricsKey = booleanPreferencesKey("enableUnison")
val EnableYouLyLyricsKey = booleanPreferencesKey("enableYouLy")
val EnableMegalobizLyricsKey = booleanPreferencesKey("enableMegalobiz")
val EnablePaxsenixLyricsKey = booleanPreferencesKey("enablePaxsenix")
val EnablePortatoLyricsKey = booleanPreferencesKey("enablePortato")
val EnableBetterLyricsKey = booleanPreferencesKey("enableBetterLyrics")
val EnableYouTubeSubtitleLyricsKey = booleanPreferencesKey("enableYouTubeSubtitles")
val EnableYouTubeMusicLyricsKey = booleanPreferencesKey("enableYouTubeMusicLyrics")
val MusicProviderKey = stringPreferencesKey("musicProvider")
val EnableJioSaavnKey = booleanPreferencesKey("enableJioSaavn")
val AudDTokenKey = stringPreferencesKey("audd_api_token")
val HideExplicitKey = booleanPreferencesKey("hideExplicit")
val LastNewReleaseCheckKey = longPreferencesKey("last_new_release_check")
val minPlaybackDurKey = intPreferencesKey("minPlaybackDur")
val ProxyEnabledKey = booleanPreferencesKey("proxyEnabled")
val ProxyUrlKey = stringPreferencesKey("proxyUrl")
val ProxyTypeKey = stringPreferencesKey("proxyType")
val YtmSyncKey = booleanPreferencesKey("ytmSync")

val AudioQualityKey = stringPreferencesKey("audioQuality")

enum class AudioQuality {
    AUTO,
    HIGH,
    MEDIUM,
    LOW,
}

val BitPerfectEnabledKey = booleanPreferencesKey("bit_perfect_enabled")
val StreamingQualityPresetKey = intPreferencesKey("streaming_quality_preset")

object QualityTiers {
    const val QUALITY_DOLBY_ATMOS = 28 // Dolby Atmos (Spatial Immersive Audio)
    const val QUALITY_MAX_HI_RES = 27  // Up to 24-bit / 192 kHz
    const val QUALITY_HI_RES_96 = 7    // Up to 24-bit / 96 kHz
    const val QUALITY_CD_LOSSLESS = 6  // 16-bit / 44.1 kHz FLAC
    const val QUALITY_MP3_320 = 5      // 320 kbps MP3
    const val QUALITY_DATA_SAVER = 4   // 96 kbps HE-AAC
    const val QUALITY_YOUTUBE = -1     // YouTube Music Native (AAC / Opus)
}

val DownloadQualityKey = stringPreferencesKey("downloadQuality")

val PersistentQueueKey = booleanPreferencesKey("persistentQueue")
val PermanentShuffleKey = booleanPreferencesKey("permanentShuffle")
val SkipSilenceKey = booleanPreferencesKey("skipSilence")
val AudioNormalizationKey = booleanPreferencesKey("audioNormalization")
val AutoLoadMoreKey = booleanPreferencesKey("autoLoadMore")
val SimilarContent = booleanPreferencesKey("similarContent")
val AutoSkipNextOnErrorKey = booleanPreferencesKey("autoSkipNextOnError")
val SkipUncachedPartKey = booleanPreferencesKey("skipUncachedPart")
val StopMusicOnTaskClearKey = booleanPreferencesKey("stopMusicOnTaskClear")
val CrossfadeKey = intPreferencesKey("crossfade")

val MaxImageCacheSizeKey = intPreferencesKey("maxImageCacheSize")
val MaxSongCacheSizeKey = intPreferencesKey("maxSongCacheSize")

val DisableLoadMoreWhenRepeatAllKey = booleanPreferencesKey("disableLoadMoreWhenRepeatAll")
val ScrobbleDelayPercentKey = floatPreferencesKey("scrobbleDelayPercent")
val ScrobbleMinSongDurationKey = intPreferencesKey("scrobbleMinSongDuration")
val ScrobbleDelaySecondsKey = intPreferencesKey("scrobbleDelaySeconds")
val EnableLastFMScrobblingKey = booleanPreferencesKey("enableLastFMScrobbling")
val LastFMUseNowPlaying = booleanPreferencesKey("lastFMUseNowPlaying")
val LastFmApiKey = stringPreferencesKey("lastfm_api_key")
val LastFmApiSecret = stringPreferencesKey("lastfm_api_secret")
val LastFmSessionKey = stringPreferencesKey("lastfm_session_key")
val LastFmUsername = stringPreferencesKey("lastfm_username")
val AutoBackupToStorageKey = booleanPreferencesKey("auto_backup_to_storage")
val AudioOffload = booleanPreferencesKey("audioOffload")

val PlayerTextAlignmentKey = stringPreferencesKey("playerTextAlignment")

val RotateBackgroundKey = booleanPreferencesKey("rotate_background")


val SmallButtonsShapeKey = stringPreferencesKey("small_buttons_shape")
const val DefaultSmallButtonsShape = "Pill"

val PauseListenHistoryKey = booleanPreferencesKey("pauseListenHistory")
val PauseSearchHistoryKey = booleanPreferencesKey("pauseSearchHistory")
val DisableScreenshotKey = booleanPreferencesKey("disableScreenshot")

val DiscordTokenKey = stringPreferencesKey("discordToken")
val DiscordInfoDismissedKey = booleanPreferencesKey("discordInfoDismissed")
val DiscordUsernameKey = stringPreferencesKey("discordUsername")
val DiscordNameKey = stringPreferencesKey("discordName")
val EnableDiscordRPCKey = booleanPreferencesKey("discordRPCEnable")

val ChipSortTypeKey = stringPreferencesKey("chipSortType")
val SongSortTypeKey = stringPreferencesKey("songSortType")
val SongSortDescendingKey = booleanPreferencesKey("songSortDescending")
val PlaylistSongSortTypeKey = stringPreferencesKey("playlistSongSortType")
val PlaylistSongSortDescendingKey = booleanPreferencesKey("playlistSongSortDescending")
val AutoPlaylistSongSortTypeKey = stringPreferencesKey("autoPlaylistSongSortType")
val AutoPlaylistSongSortDescendingKey = booleanPreferencesKey("autoPlaylistSongSortDescending")
val ArtistSortTypeKey = stringPreferencesKey("artistSortType")
val ArtistSortDescendingKey = booleanPreferencesKey("artistSortDescending")
val AlbumSortTypeKey = stringPreferencesKey("albumSortType")
val AlbumSortDescendingKey = booleanPreferencesKey("albumSortDescending")
val PlaylistSortTypeKey = stringPreferencesKey("playlistSortType")
val PlaylistSortDescendingKey = booleanPreferencesKey("playlistSortDescending")
val ArtistSongSortTypeKey = stringPreferencesKey("artistSongSortType")
val ArtistSongSortDescendingKey = booleanPreferencesKey("artistSongSortDescending")
val MixSortTypeKey = stringPreferencesKey("mixSortType")
val MixSortDescendingKey = booleanPreferencesKey("albumSortDescending")

val SongFilterKey = stringPreferencesKey("songFilter")
val ArtistFilterKey = stringPreferencesKey("artistFilter")
val AlbumFilterKey = stringPreferencesKey("albumFilter")

val LyricsScrollKey = booleanPreferencesKey("lyricsScrollKey")
val LyricsTextSizeKey = floatPreferencesKey("lyricsTextSizeKey")
val LyricsLineSpacingKey = floatPreferencesKey("lyricsLineSpacingKey")
val LyricsRomanizeJapaneseKey = booleanPreferencesKey("lyricsRomanizeJapaneseKey")
val LyricsRomanizeKoreanKey = booleanPreferencesKey("lyricsRomanizeKoreanKey")
val UseSystemFontKey = booleanPreferencesKey("useSystemFont")
val AppFontKey = stringPreferencesKey("appFont")

val DiscordUseDetailsKey = booleanPreferencesKey("discordUseDetails")


val ArtistViewTypeKey = stringPreferencesKey("artistViewType")
val AlbumViewTypeKey = stringPreferencesKey("albumViewType")
val PlaylistViewTypeKey = stringPreferencesKey("playlistViewType")

val PlaylistEditLockKey = booleanPreferencesKey("playlistEditLock")
val QuickPicksKey = stringPreferencesKey("discover")
val PreferredLyricsProviderKey = stringPreferencesKey("lyricsProvider")
val QueueEditLockKey = booleanPreferencesKey("queueEditLock")

val LyricFontSizeKey = intPreferencesKey("lyricFontSize")
val fullScreenLyricsKey = booleanPreferencesKey("fullScreenLyrics")
val AnimateLyricsKey = booleanPreferencesKey("animate_lyrics")
val LyricsScreenStyleKey = stringPreferencesKey("lyrics_screen_style")
val EnableNewLyricsScreenKey = booleanPreferencesKey("enable_new_lyrics_screen")

val EnableSwipeBackGestureKey = booleanPreferencesKey("enableSwipeBackGesture")
val EnableTabSwipeGestureKey = booleanPreferencesKey("enableTabSwipeGesture")
val EnableMiniPlayerSwipeKey = booleanPreferencesKey("enableMiniPlayerSwipe")
val EnablePlayerDoubleTapSeekKey = booleanPreferencesKey("enablePlayerDoubleTapSeek")


val PlayPauseButtonShapeKey = stringPreferencesKey("playPauseButtonShape")
const val DefaultPlayPauseButtonShape = "Cookie9Sided"

val MiniPlayerThumbnailShapeKey = stringPreferencesKey("miniPlayerThumbnailShape")
const val DefaultMiniPlayerThumbnailShape = "Circle"
enum class LibraryViewType {
    LIST,
    GRID,
    ;

    fun toggle() =
        when (this) {
            LIST -> GRID
            GRID -> LIST
        }
}

enum class SongFilter {
    LIBRARY,
    LIKED,
    DOWNLOADED
}

enum class ArtistFilter {
    LIBRARY,
    LIKED
}

enum class AlbumFilter {
    LIBRARY,
    LIKED
}

enum class SongSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class PlaylistSongSortType {
    CUSTOM,
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class AutoPlaylistSongSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    PLAY_TIME,
}

enum class ArtistSortType {
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    PLAY_TIME,
}

enum class ArtistSongSortType {
    CREATE_DATE,
    NAME,
    PLAY_TIME,
}

enum class AlbumSortType {
    CREATE_DATE,
    NAME,
    ARTIST,
    YEAR,
    SONG_COUNT,
    LENGTH,
    PLAY_TIME,
}

enum class PlaylistSortType {
    CREATE_DATE,
    NAME,
    SONG_COUNT,
    LAST_UPDATED,
}

enum class MixSortType {
    CREATE_DATE,
    NAME,
    LAST_UPDATED,
}

enum class GridItemSize {
    SMALL,
    BIG,
}

enum class MyTopFilter {
    ALL_TIME,
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ;

    fun toTimeMillis(): Long =
        when (this) {
            DAY ->
                LocalDateTime
                    .now()
                    .minusDays(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            WEEK ->
                LocalDateTime
                    .now()
                    .minusWeeks(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            MONTH ->
                LocalDateTime
                    .now()
                    .minusMonths(1)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            YEAR ->
                LocalDateTime
                    .now()
                    .minusMonths(12)
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli()

            ALL_TIME -> 0
        }
}

enum class QuickPicks {
    QUICK_PICKS,
    LAST_LISTEN,
}

enum class PreferredLyricsProvider {
    LRCLIB,
    KUGOU,
    SIMP_MUSIC,
    YOUTUBE_SUBTITLES,
    PAXSENIX,
    UNISON,
    BETTER_LYRICS,
    PORTATO,
    YOULY,
    MEGALOBIZ,
    YOUTUBE_MUSIC,
}

enum class PlayerBackgroundStyle {
    DEFAULT,
    GRADIENT,
    BLUR,
    FLUID,
}


enum class PlayerButtonsStyle {
    DEFAULT,
    PRIMARY,
    TERTIARY
}

val TopSize = stringPreferencesKey("topSize")
val HistoryDuration = floatPreferencesKey("historyDuration")

val PlayerBackgroundStyleKey = stringPreferencesKey("playerBackgroundStyle")
val ShowLyricsKey = booleanPreferencesKey("showLyrics")
val LyricsTextPositionKey = stringPreferencesKey("lyricsTextPosition")
val LyricsClickKey = booleanPreferencesKey("lyricsClick")
val TranslateLyricsKey = booleanPreferencesKey("translateLyrics")

val PlayerVolumeKey = floatPreferencesKey("playerVolume")
val BypassAllAudioEffectsKey = booleanPreferencesKey("bypass_all_audio_effects")
val AudioFxAndDjEnabledKey = booleanPreferencesKey("audio_fx_and_dj_enabled")
val EqualizerEnabledKey = booleanPreferencesKey("equalizerEnabled")
val EqualizerPresetKey = stringPreferencesKey("equalizerPreset")

val DolbyAtmosEnabledKey = booleanPreferencesKey("dolbyAtmosEnabled")
val SpatialAudioEnabledKey = booleanPreferencesKey("spatialAudioEnabled")
val AudioBoostEnabledKey = booleanPreferencesKey("audio_boost_enabled")
val AudioBoostPercentKey = intPreferencesKey("audio_boost_percent")
val EchoEnabledKey = booleanPreferencesKey("echo_enabled")
val EchoDelayMsKey = intPreferencesKey("echo_delay_ms")
val EchoFeedbackKey = floatPreferencesKey("echo_feedback")
val EchoWetMixKey = floatPreferencesKey("echo_wet_mix")
val EchoPingPongKey = booleanPreferencesKey("echo_ping_pong")
val DjFilterSweepKey = floatPreferencesKey("dj_filter_sweep")
val DjFlangerEnabledKey = booleanPreferencesKey("dj_flanger_enabled")
val DjFlangerRateKey = floatPreferencesKey("dj_flanger_rate")
val DjFlangerDepthKey = floatPreferencesKey("dj_flanger_depth")
val DjSaturationKey = floatPreferencesKey("dj_saturation")
val DjTurntableLinkedKey = booleanPreferencesKey("dj_turntable_linked")
val DjTempoSpeedKey = floatPreferencesKey("dj_tempo_speed")
val DjPitchKey = floatPreferencesKey("dj_pitch")
val EightDAudioEnabledKey = booleanPreferencesKey("eight_d_audio_enabled")
val EightDAudioLevelKey = intPreferencesKey("eight_d_audio_level")
val AutomixEnabledKey = booleanPreferencesKey("automixEnabled")
val AutomixPerformanceModeKey = stringPreferencesKey("automixPerformanceMode")
val RepeatModeKey = intPreferencesKey("repeatMode")
val PlayerButtonsStyleKey = stringPreferencesKey("player_buttons_style")

enum class AutomixPerformanceMode(val threads: Int) {
    EFFICIENT(1),
    BALANCED(2),
    PERFORMANCE(4);
}

val SearchSourceKey = stringPreferencesKey("searchSource")
val SwipeThumbnailKey = booleanPreferencesKey("swipeThumbnail")

enum class SearchSource {
    LOCAL,
    ONLINE,
    ;

    fun toggle() =
        when (this) {
            LOCAL -> ONLINE
            ONLINE -> LOCAL
        }
}

val VisitorDataKey = stringPreferencesKey("visitorData")
val DataSyncIdKey = stringPreferencesKey("dataSyncId")
val AccountPhotoUrlKey = stringPreferencesKey("account_photo_url")
val InnerTubeCookieKey = stringPreferencesKey("innerTubeCookie")
val LastBackupTimestampKey = longPreferencesKey("last_backup_timestamp")
val AccountNameKey = stringPreferencesKey("accountName")
val AccountEmailKey = stringPreferencesKey("accountEmail")
val AccountChannelHandleKey = stringPreferencesKey("accountChannelHandle")
val SpotifyCookieKey = stringPreferencesKey("spotifyCookie")
val UseLoginForBrowse = booleanPreferencesKey("useLoginForBrowse")

val LanguageCodeToName =
    mapOf(
        "af" to "Afrikaans",
        "az" to "Azərbaycan",
        "id" to "Bahasa Indonesia",
        "ms" to "Bahasa Malaysia",
        "ca" to "Català",
        "cs" to "Čeština",
        "da" to "Dansk",
        "de" to "Deutsch",
        "et" to "Eesti",
        "en-GB" to "English (UK)",
        "en" to "English (US)",
        "es" to "Español (España)",
        "es-419" to "Español (Latinoamérica)",
        "eu" to "Euskara",
        "fil" to "Filipino",
        "fr" to "Français",
        "fr-CA" to "Français (Canada)",
        "gl" to "Galego",
        "hr" to "Hrvatski",
        "zu" to "IsiZulu",
        "is" to "Íslenska",
        "it" to "Italiano",
        "sw" to "Kiswahili",
        "lt" to "Lietuvių",
        "hu" to "Magyar",
        "nl" to "Nederlands",
        "no" to "Norsk",
        "or" to "Odia",
        "uz" to "O‘zbe",
        "pl" to "Polski",
        "pt-PT" to "Português",
        "pt" to "Português (Brasil)",
        "ro" to "Română",
        "sq" to "Shqip",
        "sk" to "Slovenčina",
        "sl" to "Slovenščina",
        "fi" to "Suomi",
        "sv" to "Svenska",
        "bo" to "Tibetan བོད་སྐད།",
        "vi" to "Tiếng Việt",
        "tr" to "Türkçe",
        "bg" to "Български",
        "ky" to "Кыргызча",
        "kk" to "Қазақ Тілі",
        "mk" to "Македонски",
        "mn" to "Монгол",
        "ru" to "Русский",
        "sr" to "Српски",
        "uk" to "Українська",
        "el" to "Ελληνικά",
        "hy" to "Հայերեն",
        "iw" to "עברית",
        "ur" to "اردو",
        "ar" to "العربية",
        "fa" to "فارسی",
        "ne" to "नेपाली",
        "mr" to "मराठी",
        "hi" to "हिन्दी",
        "bn" to "বাংলা",
        "pa" to "ਪੰਜਾਬੀ",
        "gu" to "ગુજરાતી",
        "ta" to "தமிழ்",
        "te" to "తెలుగు",
        "kn" to "ಕನ್ನಡ",
        "ml" to "മലയാളം",
        "si" to "සිංහල",
        "th" to "ภาษาไทย",
        "lo" to "ລາວ",
        "my" to "ဗမာ",
        "ka" to "ქართული",
        "am" to "አማርኛ",
        "km" to "ខ្មែរ",
        "zh-CN" to "中文 (简体)",
        "zh-TW" to "中文 (繁體)",
        "zh-HK" to "中文 (香港)",
        "ja" to "日本語",
        "ko" to "한국어",
    )

val CountryCodeToName =
    mapOf(
        "DZ" to "Algeria",
        "AR" to "Argentina",
        "AU" to "Australia",
        "AT" to "Austria",
        "AZ" to "Azerbaijan",
        "BH" to "Bahrain",
        "BD" to "Bangladesh",
        "BY" to "Belarus",
        "BE" to "Belgium",
        "BO" to "Bolivia",
        "BA" to "Bosnia and Herzegovina",
        "BR" to "Brazil",
        "BG" to "Bulgaria",
        "KH" to "Cambodia",
        "CA" to "Canada",
        "CL" to "Chile",
        "HK" to "Hong Kong",
        "CO" to "Colombia",
        "CR" to "Costa Rica",
        "HR" to "Croatia",
        "CY" to "Cyprus",
        "CZ" to "Czech Republic",
        "DK" to "Denmark",
        "DO" to "Dominican Republic",
        "EC" to "Ecuador",
        "EG" to "Egypt",
        "SV" to "El Salvador",
        "EE" to "Estonia",
        "FI" to "Finland",
        "FR" to "France",
        "GE" to "Georgia",
        "DE" to "Germany",
        "GH" to "Ghana",
        "GR" to "Greece",
        "GT" to "Guatemala",
        "HN" to "Honduras",
        "HU" to "Hungary",
        "IS" to "Iceland",
        "IN" to "India",
        "ID" to "Indonesia",
        "IQ" to "Iraq",
        "IE" to "Ireland",
        "IL" to "Israel",
        "IT" to "Italy",
        "JM" to "Jamaica",
        "JP" to "Japan",
        "JO" to "Jordan",
        "KZ" to "Kazakhstan",
        "KE" to "Kenya",
        "KR" to "South Korea",
        "KW" to "Kuwait",
        "LA" to "Lao",
        "LV" to "Latvia",
        "LB" to "Lebanon",
        "LY" to "Libya",
        "LI" to "Liechtenstein",
        "LT" to "Lithuania",
        "LU" to "Luxembourg",
        "MK" to "Macedonia",
        "MY" to "Malaysia",
        "MT" to "Malta",
        "MX" to "Mexico",
        "ME" to "Montenegro",
        "MA" to "Morocco",
        "NP" to "Nepal",
        "NL" to "Netherlands",
        "NZ" to "New Zealand",
        "NI" to "Nicaragua",
        "NG" to "Nigeria",
        "NO" to "Norway",
        "OM" to "Oman",
        "PK" to "Pakistan",
        "PA" to "Panama",
        "PG" to "Papua New Guinea",
        "PY" to "Paraguay",
        "PE" to "Peru",
        "PH" to "Philippines",
        "PL" to "Poland",
        "PT" to "Portugal",
        "PR" to "Puerto Rico",
        "QA" to "Qatar",
        "RO" to "Romania",
        "RU" to "Russian Federation",
        "SA" to "Saudi Arabia",
        "SN" to "Senegal",
        "RS" to "Serbia",
        "SG" to "Singapore",
        "SK" to "Slovakia",
        "SI" to "Slovenia",
        "ZA" to "South Africa",
        "ES" to "Spain",
        "LK" to "Sri Lanka",
        "SE" to "Sweden",
        "CH" to "Switzerland",
        "TW" to "Taiwan",
        "TZ" to "Tanzania",
        "TH" to "Thailand",
        "TN" to "Tunisia",
        "TR" to "Turkey",
        "UG" to "Uganda",
        "UA" to "Ukraine",
        "AE" to "United Arab Emirates",
        "GB" to "United Kingdom",
        "US" to "United States",
        "UY" to "Uruguay",
        "VE" to "Venezuela (Bolivarian Republic)",
        "VN" to "Vietnam",
        "YE" to "Yemen",
        "ZW" to "Zimbabwe",
    )

enum class AodStyle {
    CLASSIC, BACKGROUND, MINIMAL, LARGE, SPOTLIGHT
}

enum class AodArtShape {
    ROUNDED, CIRCLE, SQUIRCLE, DIAMOND, HEXAGON, STAR, ARCH, PETAL
}

enum class AodControlStyle {
    ROUNDED, SQUARE, ACCENT, MINIMAL_FLAT
}

val AodStyleKey = stringPreferencesKey("aod_style")
val AodArtShapeKey = stringPreferencesKey("aod_art_shape")
val AodDarknessKey = floatPreferencesKey("aod_darkness")
val AodArtSizeKey = floatPreferencesKey("aod_art_size")
val AodShowTitleKey = booleanPreferencesKey("aod_show_title")
val AodShowArtistKey = booleanPreferencesKey("aod_show_artist")
val AodShowTimeKey = booleanPreferencesKey("aod_show_time_labels")
val AodShowProgressKey = booleanPreferencesKey("aod_show_progress")
val AodShowControlsKey = booleanPreferencesKey("aod_show_controls")
val AodAutoActivationKey = intPreferencesKey("aod_auto_activation_seconds")
val AodFullscreenKey = booleanPreferencesKey("aod_fullscreen_mode")
val AodSpotlightIntensityKey = floatPreferencesKey("aod_spotlight_intensity")
val AodSpotlightPulseKey = booleanPreferencesKey("aod_spotlight_pulse")
val AodTransitionDurationKey = intPreferencesKey("aod_transition_duration")
val AodControlStyleKey = stringPreferencesKey("aod_control_style")
val AodTextScaleKey = floatPreferencesKey("aod_text_scale")
val AodShowClockKey = booleanPreferencesKey("aod_show_clock")
val AodClockFormatKey = booleanPreferencesKey("aod_clock_24h")
val DisableBlurKey = booleanPreferencesKey("disableBlur")

val EnableVoiceAssistantKey = booleanPreferencesKey("enable_voice_assistant")
val VoiceAssistantAutoStartOnBootKey = booleanPreferencesKey("voice_auto_start_boot")
val VoiceAssistantDirectCommandsKey = booleanPreferencesKey("voice_direct_commands")
val VoiceAssistantTtsFeedbackKey = booleanPreferencesKey("voice_tts_feedback")

// ==================== AI INTEGRATION & LYRICS TRANSLATION KEYS ====================
val AiProviderKey = stringPreferencesKey("aiProvider")
val OpenRouterApiKey = stringPreferencesKey("openRouterApiKey")
val OpenRouterBaseUrlKey = stringPreferencesKey("openRouterBaseUrl")
val OpenRouterModelKey = stringPreferencesKey("openRouterModel")
val TranslateLanguageKey = stringPreferencesKey("translateLanguage")
val TranslateModeKey = stringPreferencesKey("translateMode")
val CustomPromptKey = stringPreferencesKey("customPrompt")
val AutoTranslateKey = booleanPreferencesKey("autoTranslate")
val ReplaceOriginalLyricsWithTranslationKey = booleanPreferencesKey("replaceOriginalLyricsWithTranslation")
val DeeplApiKey = stringPreferencesKey("deeplApiKey")
val DeeplFormalityKey = stringPreferencesKey("deeplFormality")
val AiRecommendationsKey = booleanPreferencesKey("aiRecommendations")

val AiTranslationLanguages = linkedMapOf(
    "hi-Latn" to "Hinglish (Hindi in English)",
    "en" to "English (US)",
    "en-GB" to "English (UK)",
    "hi" to "हिन्दी (Hindi)",
    "es" to "Español (Spanish)",
    "fr" to "Français (French)",
    "de" to "Deutsch (German)",
    "ja" to "日本語 (Japanese)",
    "ko" to "한국어 (Korean)",
    "zh" to "中文 (Chinese)",
    "ar" to "العربية (Arabic)",
    "ru" to "Русский (Russian)",
    "pt" to "Português (Portuguese)",
    "it" to "Italiano (Italian)",
    "tr" to "Türkçe (Turkish)",
    "ur" to "اردو (Urdu)",
    "pa" to "ਪੰਜਾਬੀ (Punjabi)",
    "bn" to "বাংলা (Bengali)",
    "mr" to "मराठी (Marathi)",
    "gu" to "ગુજરાતી (Gujarati)",
    "ta" to "தமிழ் (Tamil)",
    "te" to "తెలుగు (Telugu)",
    "kn" to "ಕನ್ನಡ (Kannada)",
    "ml" to "മലയാളം (Malayalam)",
    "id" to "Bahasa Indonesia",
    "ms" to "Bahasa Malaysia",
    "vi" to "Tiếng Việt",
    "th" to "ไทย (Thai)",
    "pl" to "Polski",
    "nl" to "Nederlands",
    "sv" to "Svenska",
    "no" to "Norsk",
    "da" to "Dansk",
    "fi" to "Suomi",
    "el" to "Ελληνικά (Greek)",
    "he" to "עברית (Hebrew)",
    "fa" to "فارسی (Persian)",
    "ro" to "Română",
    "hu" to "Magyar",
    "cs" to "Čeština",
    "sk" to "Slovenčina",
    "uk" to "Українська",
    "bg" to "Български",
    "hr" to "Hrvatski",
    "sr" to "Српски",
    "sl" to "Slovenščina",
    "et" to "Eesti",
    "lv" to "Latviešu",
    "lt" to "Lietuvių",
    "fil" to "Filipino",
    "sw" to "Kiswahili",
    "af" to "Afrikaans",
)

val HasCompletedSetupKey = booleanPreferencesKey("has_completed_setup")
