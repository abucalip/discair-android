package app.discair.ui.home

import app.discair.ui.common.BentoTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** BENTO_THEME.md step B2: pins Bento Home's greeting by hour, tile order, empty tile omission, and tone rotation. */
class BentoHomeRulesTest {
    @Test fun greetingReflectsHourOfDay() {
        assertEquals("Tuesday morning", BentoHomeRules.greeting("Tuesday", 8))
        assertEquals("Tuesday afternoon", BentoHomeRules.greeting("Tuesday", 14))
        assertEquals("Tuesday evening", BentoHomeRules.greeting("Tuesday", 19))
        assertEquals("Tuesday night", BentoHomeRules.greeting("Tuesday", 23))
        assertEquals("Tuesday night", BentoHomeRules.greeting("Tuesday", 2))
    }

    @Test fun visibleTilesFollowSpecOrderAndOmitEmptySections() {
        val all = BentoHomeRules.visibleTiles(
            BentoHomeRules.Availability(
                hasBanner = true,
                hasPickOrLoading = true,
                hasHeroTrio = true,
                hasNewlyAdded = true,
                hasPlayedOrGenres = true,
                hasMostPlayed = true,
                hasDiscoverTeaser = true,
                hasRadioSection = true,
                hasInsight = true,
                hasArrivals = true,
                hasLibraryLine = true,
            ),
        )
        assertEquals(BentoHomeRules.TileKind.entries, all)

        val sparse = BentoHomeRules.visibleTiles(
            BentoHomeRules.Availability(
                hasBanner = false,
                hasPickOrLoading = true,
                hasHeroTrio = false,
                hasNewlyAdded = false,
                hasPlayedOrGenres = true,
                hasMostPlayed = false,
                hasDiscoverTeaser = false,
                hasRadioSection = false,
                hasInsight = false,
                hasArrivals = true,
                hasLibraryLine = true,
            ),
        )
        assertEquals(
            listOf(
                BentoHomeRules.TileKind.Pick,
                BentoHomeRules.TileKind.PlayedAndGenres,
                BentoHomeRules.TileKind.Arrivals,
                BentoHomeRules.TileKind.LibraryLine,
            ),
            sparse,
        )
    }

    @Test fun toneRotationNeverRepeatsAnAdjacentNeighbour() {
        val tones = BentoHomeRules.rotateTones(12)
        assertEquals(12, tones.size)
        for (i in 1 until tones.size) {
            assertNotEquals("tone at $i repeated ${i - 1}", tones[i - 1], tones[i])
        }
        assertEquals(BentoTone.Warm, tones[0])
        assertEquals(BentoTone.Sun, tones[1])
        assertEquals(BentoTone.Leaf, tones[2])
        assertEquals(BentoTone.Bloom, tones[3])
        assertEquals(BentoTone.Sky, tones[4])
        assertEquals(BentoTone.Warm, tones[5])
        assertTrue(BentoHomeRules.rotateTones(0).isEmpty())
    }

    @Test fun libraryLineFormatsCountsAndOmitsZeros() {
        assertEquals("1,284 albums · 312 artists", BentoHomeRules.libraryLine(1284, 312))
        assertEquals("1 album", BentoHomeRules.libraryLine(1, null))
        assertEquals("4 artists", BentoHomeRules.libraryLine(0, 4))
        assertNull(BentoHomeRules.libraryLine(null, null))
    }
}
