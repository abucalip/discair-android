package app.discair.ui.theme

import androidx.compose.ui.graphics.Color
import app.discair.ui.NavRules
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoTones
import app.discair.ui.settings.ThemeGroups
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins Bento's three palettes, five tones per palette, contrast ratios, nav order, and picker grouping. */
class BentoTest {
    private fun lin(v: Float): Double = v.toDouble().let { if (it <= 0.03928) it / 12.92 else ((it + 0.055) / 1.055).pow(2.4) }
    private fun lum(c: Color) = 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    private fun contrast(a: Color, b: Color): Double {
        val x = lum(a); val y = lum(b)
        return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05)
    }

    @Test fun threePalettesInBentoGroupWithDayAndNightSlots() {
        val bento = ThemeGroups.shown(Palettes.All, "Bento", null)
        assertEquals(listOf("bento", "bento_riviera", "bento_licorice"), bento.map { it.id })
        assertEquals(listOf("bento", "bento_riviera"), ThemeGroups.shown(Palettes.All, "Bento", true).map { it.id })
        assertEquals(listOf("bento_licorice"), ThemeGroups.shown(Palettes.All, "Bento", false).map { it.id })
        assertTrue(bento.all { it.layout == Layout.Bento && it.suggestedFontId == "bento" })
    }

    @Test fun sorbetLicoriceAndRivieraPinSpecHexValues() {
        assertEquals(Color(0xFFF4F0E8), Palettes.Bento.ink)
        assertEquals(Color(0xFFFFFFFF), Palettes.Bento.surface)
        assertEquals(Color(0xFFB8321A), Palettes.Bento.accent)
        assertEquals(Color(0xFF121110), Palettes.BentoLicorice.ink)
        assertEquals(Color(0xFF1C1A18), Palettes.BentoLicorice.surface)
        assertEquals(Color(0xFFFF8E6A), Palettes.BentoLicorice.accent)
        assertEquals(Color(0xFFEFF3F1), Palettes.BentoRiviera.ink)
        assertEquals(Color(0xFF2440C8), Palettes.BentoRiviera.accent)
    }

    @Test fun tonesMeetContrastOnEveryBentoPalette() {
        for (p in listOf(Palettes.Bento, Palettes.BentoLicorice, Palettes.BentoRiviera)) {
            val t = BentoTones.of(p.id)
            for ((name, fill) in listOf("warm" to t.warm, "sun" to t.sun, "leaf" to t.leaf, "bloom" to t.bloom, "sky" to t.sky)) {
                val c = contrast(t.onTone, fill)
                assertTrue("${p.label} onTone on $name is %.2f, needs >= 4.5".format(c), c >= 4.5)
                val cm = contrast(t.onToneMuted, fill)
                assertTrue("${p.label} onToneMuted on $name is %.2f, needs >= 4.5".format(cm), cm >= 4.5)
            }
            assertTrue("${p.label} onDeep on deep", contrast(t.onDeep, t.deep) >= 7.0)
            assertTrue("${p.label} barIcon on bar", contrast(t.barIcon, t.bar) >= 7.0)
        }
    }

    @Test fun navBarUsesBentoOrderAndMotionFamilyIsStandard() {
        assertEquals(listOf("Home", "Library", "Discover", "Search"), BentoRules.cells)
        assertTrue(NavRules.discoverInBar(Layout.Bento))
        assertFalse(NavRules.playerButtonShown(Layout.Bento))
        assertEquals(MotionFamily.Standard, MotionRules.familyFor(Layout.Bento))
    }

    @Test fun bentoFontPairIsGabaritoAndFigtree() {
        val f = FontPairs.byId("bento")!!
        assertEquals("Gabarito", f.titleName)
        assertEquals("Figtree", f.bodyName)
    }

    @Test fun toneRotationCyclesFivePastelTones() {
        assertEquals(5, BentoTone.Rotation.size)
        assertEquals(BentoTone.Warm, BentoTone.rotate(0))
        assertEquals(BentoTone.Sky, BentoTone.rotate(4))
        assertEquals(BentoTone.Warm, BentoTone.rotate(5))
    }
}
