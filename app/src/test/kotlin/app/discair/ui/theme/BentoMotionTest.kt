package app.discair.ui.theme

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** BENTO_THEME.md section 8: pins Bento's motion constants and verifies Remove animations zeroes every tween and turns every spring into snap(). */
class BentoMotionTest {
    @Test fun motionConstantsMatchSection8Spec() {
        assertEquals(0.72f, BentoMotion.TILE_ENTER_DAMPING, 0f)
        assertEquals(380f, BentoMotion.TILE_ENTER_STIFFNESS, 0f)
        assertEquals(0.94f, BentoMotion.TILE_ENTER_SCALE, 0f)
        assertEquals(14, BentoMotion.TILE_ENTER_RISE_DP)
        assertEquals(220, BentoMotion.TILE_ENTER_FADE_MS)
        assertEquals(40, BentoMotion.TILE_ENTER_STAGGER_MS)
        assertEquals(10, BentoMotion.TILE_ENTER_MAX)

        assertEquals(450, BentoMotion.TILE_TO_PAGE_MS)
        assertEquals(380, BentoMotion.PAGE_TO_TILE_MS)
        assertEquals(220, BentoMotion.PAGE_CROSSFADE_MS)

        assertEquals(0.97f, BentoMotion.PRESS_SCALE, 0f)
        assertEquals(0.6f, BentoMotion.PRESS_DAMPING, 0f)
        assertEquals(800f, BentoMotion.PRESS_STIFFNESS, 0f)

        assertEquals(0.8f, BentoMotion.NAV_PILL_DAMPING, 0f)
        assertEquals(500f, BentoMotion.NAV_PILL_STIFFNESS, 0f)
        assertEquals(420, BentoMotion.PLAYER_RISE_MS)
        assertEquals(320, BentoMotion.PLAYER_FALL_MS)
        assertEquals(4000L, BentoMotion.SNACK_HOLD_MS)
        assertEquals(260, BentoMotion.GET_MORPH_MS)
        assertEquals(900, BentoMotion.GET_SPIN_MS)
        assertEquals(700, BentoMotion.STAT_COUNT_MS)
        assertEquals(900, BentoMotion.BARS_MS)
        assertEquals(listOf(-10f, 6f, 0f), BentoMotion.COVER_STACK_ANGLES)
        assertEquals(listOf(0, -300, -600), BentoMotion.BARS_PHASE_MS)
    }

    @Test fun reducedMotionZeroesTweensAndSnapsSprings() {
        val springReduced = BentoMotion.spring<Float>(0.72f, 380f, reduced = true)
        assertTrue(springReduced is SnapSpec<*>)
        val springNormal = BentoMotion.spring<Float>(0.72f, 380f, reduced = false)
        assertTrue(springNormal is SpringSpec<*>)

        for (ms in listOf(
            BentoMotion.TILE_ENTER_FADE_MS,
            BentoMotion.TILE_TO_PAGE_MS,
            BentoMotion.PAGE_TO_TILE_MS,
            BentoMotion.PAGE_CROSSFADE_MS,
            BentoMotion.PLAYER_RISE_MS,
            BentoMotion.PLAYER_FALL_MS,
            BentoMotion.GET_MORPH_MS,
            BentoMotion.STAT_COUNT_MS,
        )) {
            val reducedTween = BentoMotion.tween<Float>(ms, BentoMotion.Emphasized, reduced = true, delayMs = 60) as TweenSpec<*>
            assertEquals(0, reducedTween.durationMillis)
            assertEquals(0, reducedTween.delay)
            val normalTween = BentoMotion.tween<Float>(ms, BentoMotion.Emphasized, reduced = false, delayMs = 60) as TweenSpec<*>
            assertEquals(ms, normalTween.durationMillis)
            assertEquals(60, normalTween.delay)
        }
    }

    @Test fun tileDelayStaggersTenTilesAndReturnsNullPastMax() {
        assertEquals(0, BentoMotion.tileDelayMs(0))
        assertEquals(360, BentoMotion.tileDelayMs(9))
        assertNull(BentoMotion.tileDelayMs(10))
        assertNull(BentoMotion.tileDelayMs(-1))
    }

    @Test fun playingBarsSwingBetweenFourAndFourteenDp() {
        assertEquals(4f, BentoMotion.barHeightDp(0, 0), 0.01f)
        assertEquals(14f, BentoMotion.barHeightDp(900, 0), 0.01f)
        assertEquals(4f, BentoMotion.barHeightDp(1800, 0), 0.01f)
        assertEquals(9, BentoMotion.BARS_REST_DP)
    }
}
