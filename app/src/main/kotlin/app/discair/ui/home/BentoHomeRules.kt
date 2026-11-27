package app.discair.ui.home

import app.discair.ui.common.BentoShellRules
import app.discair.ui.common.BentoTone

/**
 * Pure rules for Bento's Home screen (BENTO_THEME.md section 9).
 * Unit-tested in BentoHomeRulesTest without a device: greeting by hour, tile order, empty tiles dropped,
 * and tone rotation never repeating an adjacent tone.
 */
object BentoHomeRules {
    const val EMPTY_LISTENING = "No listening yet. Play a few albums and the artists you come back to show up here."
    const val NEWLY_ADDED_SHOWN = 4
    const val GENRES_SHOWN = 4
    const val MOST_PLAYED_SHOWN = 3
    const val RADIO_STATIONS_SHOWN = 4
    const val ARRIVALS_SHOWN = 2

    enum class TileKind {
        Banner,
        Pick,
        HeroTrio,
        NewlyAdded,
        PlayedAndGenres,
        MostPlayed,
        DiscoverTeaser,
        RadioSection,
        ThisWeek,
        Arrivals,
        LibraryLine,
    }

    data class Availability(
        val hasBanner: Boolean = false,
        val hasPickOrLoading: Boolean = true,
        val hasHeroTrio: Boolean = true,
        val hasNewlyAdded: Boolean = false,
        val hasPlayedOrGenres: Boolean = false,
        val hasMostPlayed: Boolean = false,
        val hasDiscoverTeaser: Boolean = false,
        val hasRadioSection: Boolean = false,
        val hasInsight: Boolean = false,
        val hasArrivals: Boolean = false,
        val hasLibraryLine: Boolean = true,
    )

    /** Greeting for [dayOfWeek] and 24-hour [hour]: morning (5–11), afternoon (12–16), evening (17–20), night (21–4). */
    fun greeting(dayOfWeek: String, hour: Int): String = BentoShellRules.greeting(dayOfWeek, hour)

    /** Visible tile sequence in spec order; any tile whose data is empty is omitted so the grid closes up. */
    fun visibleTiles(a: Availability): List<TileKind> = buildList {
        if (a.hasBanner) add(TileKind.Banner)
        if (a.hasPickOrLoading) add(TileKind.Pick)
        if (a.hasHeroTrio) add(TileKind.HeroTrio)
        if (a.hasNewlyAdded) add(TileKind.NewlyAdded)
        if (a.hasPlayedOrGenres) add(TileKind.PlayedAndGenres)
        if (a.hasMostPlayed) add(TileKind.MostPlayed)
        if (a.hasDiscoverTeaser) add(TileKind.DiscoverTeaser)
        if (a.hasRadioSection) add(TileKind.RadioSection)
        if (a.hasInsight) add(TileKind.ThisWeek)
        if (a.hasArrivals) add(TileKind.Arrivals)
        if (a.hasLibraryLine) add(TileKind.LibraryLine)
    }

    /**
     * Assigns decorative pastel tones in rotation (warm, sun, leaf, bloom, sky) to [count] tiles,
     * guaranteeing no two adjacent tiles share a tone.
     */
    fun rotateTones(count: Int, startOffset: Int = 0): List<BentoTone> {
        if (count <= 0) return emptyList()
        val ring = BentoTone.Rotation
        return List(count) { i -> ring[Math.floorMod(startOffset + i, ring.size)] }
    }

    /** Formats "Artist · 2024" or "Artist" when year is null. */
    fun artistYear(artist: String, year: Int?): String = if (year != null) "$artist · $year" else artist

    /** Formats library counts line: "1,284 albums · 312 artists" (gracefully omits missing parts). */
    fun libraryLine(albums: Int?, artists: Int?): String? {
        val parts = buildList {
            if (albums != null && albums > 0) add("%,d %s".format(albums, if (albums == 1) "album" else "albums"))
            if (artists != null && artists > 0) add("%,d %s".format(artists, if (artists == 1) "artist" else "artists"))
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    /** Extracts 5 recent weekly bar heights from daily listen counts if available (empty list if none). */
    fun weeklyBars(listensByDay: List<Int>): List<Float> {
        if (listensByDay.isEmpty()) return emptyList()
        val padded = if (listensByDay.size >= 35) listensByDay.takeLast(35) else listensByDay
        val chunkSize = (padded.size / 5).coerceAtLeast(1)
        val chunks = padded.chunked(chunkSize).takeLast(5)
        return chunks.map { it.sum().toFloat() }
    }
}
