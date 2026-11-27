package app.discair.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Bento's rounded 24x24 icon set (BENTO_THEME.md section 6). Uses a 1.8f rounded stroke for outlines;
 * only Play, Pause, Next, Previous, More, HeartFilled and BookmarkFilled are solid fills.
 * Property names match [ArtIcons] one for one, plus Ban, RepeatOne, MusicNote, Spark and Grid.
 */
object BentoIcons {
    private fun build(name: String, paths: Array<out String>, filled: Boolean): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach {
            val nodes = PathParser().parsePathString(it).toNodes()
            if (filled) b.addPath(nodes, fill = SolidColor(Color.Black))
            else b.addPath(
                nodes, fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }
        return b.build()
    }

    private fun stroked(name: String, vararg paths: String) = build(name, paths, filled = false)
    private fun filled(name: String, vararg paths: String) = build(name, paths, filled = true)

    val Home = stroked("bento_home",
        "M4 11l8-7 8 7v7.5a1.5 1.5 0 0 1-1.5 1.5h-4v-5.5a1 1 0 0 0-1-1h-3a1 1 0 0 0-1 1V20h-4A1.5 1.5 0 0 1 4 18.5z",
    )
    val Disc = stroked("bento_disc",
        "M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z",
        "M18.5 16l.7 1.8 1.8.7-1.8.7-.7 1.8-.7-1.8-1.8-.7 1.8-.7z",
    )
    val Lib = stroked("bento_lib",
        "M5 4.5v15M9.5 4.5v15M14 5.5l4.5 14",
    )
    val Search = stroked("bento_search",
        "M4.5,11a6.5,6.5 0 1,0 13,0a6.5,6.5 0 1,0 -13,0",
        "M16 16l4.5 4.5",
    )
    val Play = filled("bento_play",
        "M7.2 4.8c0-.7.76-1.14 1.37-.78l10.8 6.4c.6.35.6 1.22 0 1.57l-10.8 6.4c-.61.36-1.37-.08-1.37-.79z",
    )
    val Pause = filled("bento_pause",
        "M7 5.5a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1z",
        "M13 5.5a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1z",
    )
    val Next = filled("bento_next",
        "M5.5 5.4c0-.66.74-1.06 1.29-.69l8.6 5.9c.48.33.48 1.05 0 1.38l-8.6 5.9c-.55.37-1.29-.03-1.29-.69z",
        "M17 5.5a1 1 0 0 1 1-1h.8a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1H18a1 1 0 0 1-1-1z",
    )
    val Previous = filled("bento_prev",
        "M18.5 5.4c0-.66-.74-1.06-1.29-.69l-8.6 5.9c-.48.33-.48 1.05 0 1.38l8.6 5.9c.55.37 1.29-.03 1.29-.69z",
        "M4.2 5.5a1 1 0 0 1 1-1H6a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-.8a1 1 0 0 1-1-1z",
    )
    val Download = stroked("bento_dl",
        "M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0",
        "M12 7.5v8M8.5 12.5L12 16l3.5-3.5",
    )
    val Gear = stroked("bento_gear",
        "M9,12a3,3 0 1,0 6,0a3,3 0 1,0 -6,0",
        "M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3M5.3 5.3l2.1 2.1M16.6 16.6l2.1 2.1M5.3 18.7l2.1-2.1M16.6 7.4l2.1-2.1",
    )
    val More = filled("bento_more",
        "M3.4,12a1.6,1.6 0 1,0 3.2,0a1.6,1.6 0 1,0 -3.2,0",
        "M10.4,12a1.6,1.6 0 1,0 3.2,0a1.6,1.6 0 1,0 -3.2,0",
        "M17.4,12a1.6,1.6 0 1,0 3.2,0a1.6,1.6 0 1,0 -3.2,0",
    )
    val Chevron = stroked("bento_chev", "M9.5 5.5l6.5 6.5-6.5 6.5")
    val Back = stroked("bento_back", "M14.5 5.5L8 12l6.5 6.5")
    val Down = stroked("bento_down", "M5.5 9.5L12 16l6.5-6.5")
    val Up = stroked("bento_up", "M5.5 14.5L12 8l6.5 6.5")
    val Radio = stroked("bento_radio",
        "M5,8h14a2.5,2.5 0 0 1 2.5,2.5v7a2.5,2.5 0 0 1 -2.5,2.5h-14a2.5,2.5 0 0 1 -2.5,-2.5v-7a2.5,2.5 0 0 1 2.5,-2.5z",
        "M7 8l9-4",
        "M6,14a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0",
        "M14 12.5h4M14 15.5h4",
    )
    val Get = stroked("bento_get",
        "M7 18a4.5 4.5 0 0 1-.6-8.96 6 6 0 0 1 11.7 1.5A3.8 3.8 0 0 1 17.5 18M12 11v8M9 16.5l3 3 3-3",
    )
    val Heart = stroked("bento_heart",
        "M12 20s-7-4.3-7-9.5A4 4 0 0 1 12 8a4 4 0 0 1 7 2.5C19 15.7 12 20 12 20z",
    )
    val HeartFilled = filled("bento_heart_filled",
        "M12 20s-7-4.3-7-9.5A4 4 0 0 1 12 8a4 4 0 0 1 7 2.5C19 15.7 12 20 12 20z",
    )
    val Bookmark = stroked("bento_bookmark",
        "M7 4h10a1.5 1.5 0 0 1 1.5 1.5V20l-6.5-4.2L5.5 20V5.5A1.5 1.5 0 0 1 7 4z",
    )
    val BookmarkFilled = filled("bento_bookmark_filled",
        "M7 4h10a1.5 1.5 0 0 1 1.5 1.5V20l-6.5-4.2L5.5 20V5.5A1.5 1.5 0 0 1 7 4z",
    )
    val Check = stroked("bento_check", "M5 12.5l4.5 4.5L19 7.5")
    val Close = stroked("bento_close", "M6 6l12 12M18 6L6 18")
    val Queue = stroked("bento_queue", "M4 7h16M4 12h16M4 17h10")
    val QueueAdd = stroked("bento_qadd", "M4 7h12M4 12h12M4 17h7M18 14v6M15 17h6")
    val Shuffle = stroked("bento_shuffle",
        "M4 7h3l10 10h3M4 17h3l3-3M14 10l3-3h3M18 5l2 2-2 2M18 15l2 2-2 2",
    )
    val Repeat = stroked("bento_repeat",
        "M5 11V9a2 2 0 0 1 2-2h11M16 4l3 3-3 3M19 13v2a2 2 0 0 1-2 2H6M8 20l-3-3 3-3",
    )
    val RepeatOne = stroked("bento_repeat_one",
        "M5 11V9a2 2 0 0 1 2-2h11M16 4l3 3-3 3M19 13v2a2 2 0 0 1-2 2H6M8 20l-3-3 3-3",
        "M11,10l1.2,-0.8v5.3",
    )
    val Quote = stroked("bento_quote", "M5.5 6h13a1.5 1.5 0 0 1 1.5 1.5v7a1.5 1.5 0 0 1-1.5 1.5H10l-4 3v-3h-.5A1.5 1.5 0 0 1 4 14.5v-7A1.5 1.5 0 0 1 5.5 6z")
    val Tune = stroked("bento_tune",
        "M6 4v16M12 4v16M18 4v16",
        "M4,9a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
        "M10,15a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
        "M16,8a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
    )
    val Sun = stroked("bento_sun",
        "M8,12a4,4 0 1,0 8,0a4,4 0 1,0 -8,0",
        "M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4",
    )
    val Server = stroked("bento_server",
        "M5.5,4h13a1.5,1.5 0 0 1 1.5,1.5v4a1.5,1.5 0 0 1 -1.5,1.5h-13a1.5,1.5 0 0 1 -1.5,-1.5v-4a1.5,1.5 0 0 1 1.5,-1.5z",
        "M5.5,13h13a1.5,1.5 0 0 1 1.5,1.5v4a1.5,1.5 0 0 1 -1.5,1.5h-13a1.5,1.5 0 0 1 -1.5,-1.5v-4a1.5,1.5 0 0 1 1.5,-1.5z",
        "M7.5 7.5h.01M7.5 16.5h.01",
    )
    val Swap = stroked("bento_swap", "M4 8h13M14 5l3 3-3 3M20 16H7M10 13l-3 3 3 3")
    val Wave = stroked("bento_wave", "M4 12h2l2-6 3 12 3-9 2 5 1-2h3")
    val Sort = stroked("bento_sort", "M7 5v14M4 16l3 3 3-3M14 7h6M14 12h4M14 17h2")
    val Plus = stroked("bento_plus", "M12 5v14M5 12h14")
    val Minus = stroked("bento_minus", "M5 12h14")
    val Paths = stroked("bento_paths",
        "M4,18a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
        "M16,6a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
        "M8 18h5a3 3 0 0 0 0-6h-2a3 3 0 0 1 0-6h5",
    )
    val Info = stroked("bento_info",
        "M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0",
        "M12 11v5M12 8h.01",
    )
    val SignOut = stroked("bento_out", "M10 5H5v14h5M14 8l4 4-4 4M18 12H9")
    val Warning = stroked("bento_warning", "M12 4l9 16H3z", "M12 10v4M12 17h.01")
    val Delete = stroked("bento_delete", "M5 7h14M10 7V4h4v3M7 7l1 13h8l1-13M10 11v6M14 11v6")
    val Refresh = stroked("bento_refresh", "M19 12a7 7 0 1 1-2.05-4.95M19 4v4h-4")
    val Place = stroked("bento_place",
        "M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11z",
        "M10,10a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
    )
    val Album = stroked("bento_album",
        "M3,12a9,9 0 1,0 18,0a9,9 0 1,0 -18,0",
        "M9.5,12a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0",
    )
    val Photos = stroked("bento_photos",
        "M5,5h14a1.5,1.5 0 0 1 1.5,1.5v11a1.5,1.5 0 0 1 -1.5,1.5h-14a1.5,1.5 0 0 1 -1.5,-1.5v-11a1.5,1.5 0 0 1 1.5,-1.5z",
        "M4 16l5-5 4 4 3-3 4 4M14.5 8.5h.01",
    )
    val MusicNote = stroked("bento_music_note",
        "M9 18V6l10-2v12",
        "M5,18a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
        "M15,16a2,2 0 1,0 4,0a2,2 0 1,0 -4,0",
    )
    // Extra icons named in BENTO_THEME.md section 6
    val Ban = stroked("bento_ban",
        "M3.5,12a8.5,8.5 0 1,0 17,0a8.5,8.5 0 1,0 -17,0",
        "M6 6l12 12",
    )
    val Spark = stroked("bento_spark",
        "M12 3l1.8 6.2L20 11l-6.2 1.8L12 19l-1.8-6.2L4 11l6.2-1.8z",
    )
    val Grid = stroked("bento_grid",
        "M5,4h4.5a1.5,1.5 0 0 1 1.5,1.5v4.5a1.5,1.5 0 0 1 -1.5,1.5h-4.5a1.5,1.5 0 0 1 -1.5,-1.5v-4.5a1.5,1.5 0 0 1 1.5,-1.5z",
        "M14.5,4h4.5a1.5,1.5 0 0 1 1.5,1.5v4.5a1.5,1.5 0 0 1 -1.5,1.5h-4.5a1.5,1.5 0 0 1 -1.5,-1.5v-4.5a1.5,1.5 0 0 1 1.5,-1.5z",
        "M5,13.5h4.5a1.5,1.5 0 0 1 1.5,1.5v4.5a1.5,1.5 0 0 1 -1.5,1.5h-4.5a1.5,1.5 0 0 1 -1.5,-1.5v-4.5a1.5,1.5 0 0 1 1.5,-1.5z",
        "M14.5,13.5h4.5a1.5,1.5 0 0 1 1.5,1.5v4.5a1.5,1.5 0 0 1 -1.5,1.5h-4.5a1.5,1.5 0 0 1 -1.5,-1.5v-4.5a1.5,1.5 0 0 1 1.5,-1.5z",
    )
}
