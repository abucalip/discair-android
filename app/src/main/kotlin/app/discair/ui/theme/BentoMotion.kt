package app.discair.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap

/*
 * Bento's motion values (BENTO_THEME.md section 8). Springy and tactile: tiles pop in, the nav pill stretches,
 * a tile grows into its page. Every duration and spring routes through this object so the phone's Remove animations
 * setting zeroes every tween and turns every spring into snap(). Pure, so BentoMotionTest pins every value.
 */
object BentoMotion {
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedExit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Exit: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

    // 1. Tile entry
    const val TILE_ENTER_DAMPING = 0.72f
    const val TILE_ENTER_STIFFNESS = 380f
    const val TILE_ENTER_SCALE = 0.94f
    const val TILE_ENTER_RISE_DP = 14
    const val TILE_ENTER_FADE_MS = 220
    const val TILE_ENTER_STAGGER_MS = 40
    const val TILE_ENTER_MAX = 10

    // 2 & 3. Tile -> page / Page -> tile
    const val TILE_TO_PAGE_MS = 450
    const val TILE_TO_PAGE_CONTENT_DELAY_MS = 150
    const val TILE_TO_PAGE_CONTENT_FADE_MS = 200
    const val TILE_TO_PAGE_EXIT_FADE_MS = 120
    const val PAGE_TO_TILE_MS = 380

    // 4. Other page changes (tabs)
    const val PAGE_CROSSFADE_MS = 220

    // 5. Press
    const val PRESS_SCALE = 0.97f
    const val PRESS_DAMPING = 0.6f
    const val PRESS_STIFFNESS = 800f

    // 6. Nav pill
    const val NAV_PILL_DAMPING = 0.8f
    const val NAV_PILL_STIFFNESS = 500f
    const val NAV_PILL_COLOR_MS = 200
    const val NAV_LABEL_FADE_MS = 150
    const val NAV_LABEL_DELAY_MS = 60
    const val NAV_LABEL_EXIT_MS = 90

    // 7. Now Playing opens / closes
    const val PLAYER_RISE_MS = 420
    const val PLAYER_FALL_MS = 320

    // 8. Sheets (Material's ModalBottomSheet timing is kept; values pinned for reference/custom animations)
    const val SHEET_IN_DAMPING = 0.85f
    const val SHEET_IN_STIFFNESS = 420f
    const val SHEET_OUT_MS = 240
    const val SHEET_SCRIM_MS = 220

    // 9. Snackbar
    const val SNACK_IN_DAMPING = 0.75f
    const val SNACK_IN_STIFFNESS = 500f
    const val SNACK_FROM_DP = 24
    const val SNACK_FADE_MS = 150
    const val SNACK_HOLD_MS = 4000L

    // 10. Get button morph
    const val GET_MORPH_MS = 260
    const val GET_FADE_IN_MS = 160
    const val GET_FADE_IN_DELAY_MS = 60
    const val GET_FADE_OUT_MS = 100
    const val GET_SPIN_MS = 900

    // 11. Cover stack fan
    const val COVER_STACK_DAMPING = 0.7f
    const val COVER_STACK_STIFFNESS = 300f
    const val COVER_STACK_STAGGER_MS = 60
    const val COVER_STACK_PRESS_OPEN_DEG = 4f
    val COVER_STACK_ANGLES: List<Float> = listOf(-10f, 6f, 0f)

    // 12. Stat count-up
    const val STAT_COUNT_MS = 700

    // 13. Playing bars
    const val BARS_MS = 900
    const val BARS_MIN_DP = 4
    const val BARS_MAX_DP = 14
    const val BARS_REST_DP = 9
    val BARS_PHASE_MS: List<Int> = listOf(0, -300, -600)

    // 14. Pager eyebrow
    const val PAGER_EYEBROW_MS = 200
    const val PAGER_EYEBROW_SLIDE_DP = 8

    /** Bento's nav bar order (decision B7): Home, Library, Discover, Search. */
    val barOrder: List<String> = listOf("Home", "Library", "Discover", "Search")

    /** Delay for tile [index] in the entry stagger, or null past [TILE_ENTER_MAX] (appears immediately). */
    fun tileDelayMs(index: Int): Int? = if (index in 0 until TILE_ENTER_MAX) index * TILE_ENTER_STAGGER_MS else null

    /** Height of one playing bar at [timeMs] into the loop (4dp to 14dp, 900ms alternate). */
    fun barHeightDp(timeMs: Long, phaseMs: Int): Float {
        val period = BARS_MS * 2L
        val t = Math.floorMod(timeMs - phaseMs, period)
        val up = if (t < BARS_MS) t.toFloat() / BARS_MS else 2f - t.toFloat() / BARS_MS
        val eased = CubicBezierEasing(0.42f, 0f, 0.58f, 1f).transform(up)
        return BARS_MIN_DP + (BARS_MAX_DP - BARS_MIN_DP) * eased
    }

    /** Fully qualified calls: inside this object, plain spring()/tween() would call these same functions. */
    fun <T> spring(damping: Float, stiffness: Float, reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) snap() else androidx.compose.animation.core.spring(dampingRatio = damping, stiffness = stiffness)

    fun <T> tween(ms: Int, curve: Easing, reduced: Boolean, delayMs: Int = 0): FiniteAnimationSpec<T> =
        androidx.compose.animation.core.tween(MotionRules.duration(ms, MotionFamily.Standard, reduced), if (reduced) 0 else delayMs, curve)

    fun <T> fade(ms: Int, reduced: Boolean, delayMs: Int = 0): FiniteAnimationSpec<T> =
        tween(ms, LinearEasing, reduced, delayMs)
}
