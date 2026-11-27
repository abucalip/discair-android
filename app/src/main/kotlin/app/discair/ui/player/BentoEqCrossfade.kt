package app.discair.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.discair.player.PlayerSettings
import app.discair.player.crossfade.CrossfadeSettings
import app.discair.player.crossfade.CrossfadeText
import app.discair.player.eq.EQ_BAND_COUNT
import app.discair.player.eq.EQ_BAND_HZ
import app.discair.player.eq.EQ_MAX_GAIN_DB
import app.discair.player.eq.EQ_PRESETS
import app.discair.player.eq.EQ_STEP_DB
import app.discair.player.eq.EqEditor
import app.discair.player.eq.EqRuntimeStatus
import app.discair.player.eq.EqSettings
import app.discair.player.eq.bandLabel
import app.discair.player.eq.bandSpoken
import app.discair.player.eq.eqStatusLine
import app.discair.player.eq.formatDb
import app.discair.ui.LocalBottomClearance
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoChip
import app.discair.ui.common.BentoIconButton
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPageBar
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoSwitch
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileHeader
import app.discair.ui.common.BentoTileLabel
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.Slider
import app.discair.ui.common.bentoTones
import app.discair.ui.theme.LocalPalette

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Bento's Equalizer screen (BENTO_THEME.md step B5/B6, settings frame 3).
 * Switch tile, preset chips, band controls with slider and ± buttons, and Reset button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BentoEqualizer(
    editor: EqEditor,
    settings: EqSettings,
    status: EqRuntimeStatus,
    onBack: (() -> Unit)?,
    onReset: () -> Unit,
) {
    val p = LocalPalette.current
    val on = settings.config.enabled

    Column(
        Modifier
            .fillMaxWidth()
            .background(p.ink)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp + if (onBack != null) LocalBottomClearance.current else 0.dp),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        if (onBack != null) {
            BentoPageBar(onBack = onBack, title = "Equalizer")
        } else {
            Text(
                "Equalizer",
                style = BentoType.largeTitle(),
                color = p.text,
                modifier = Modifier.padding(horizontal = Side, vertical = 4.dp).semantics { heading() },
            )
        }

        // Enable switch tile
        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth(), tone = BentoTone.Leaf, small = true) {
            Row(
                Modifier.fillMaxWidth().toggleable(value = on, role = Role.Switch, onValueChange = editor::setEnabled),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Equalizer", style = BentoType.tileName(), color = bentoTones().onTone)
                    Text(
                        if (on) "On. Shaping everything you play here." else "Off. Songs play as they are.",
                        style = BentoType.small(), color = bentoTones().onToneMuted,
                    )
                }
                BentoSwitch(checked = on)
            }
        }

        eqStatusLine(on, status)?.let { line ->
            Text(
                line,
                style = BentoType.small(),
                color = p.accent,
                modifier = Modifier.padding(horizontal = Side + 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        // Presets tile
        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth().alpha(if (on) 1f else 0.55f), tone = BentoTone.Surface) {
            BentoTileHeader("Presets")
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EQ_PRESETS.forEach { preset ->
                    BentoChip(
                        label = preset.label,
                        selected = settings.presetId == preset.id,
                        onClick = { if (on) editor.selectPreset(preset.id) },
                        small = true,
                    )
                }
            }
        }

        // Bands tile
        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth().alpha(if (on) 1f else 0.55f), tone = BentoTone.Surface) {
            BentoTileHeader("Bands", actionLabel = "Reset", onAction = { if (on) onReset() })
            Spacer(Modifier.height(10.dp))
            for (i in 0 until EQ_BAND_COUNT) {
                val db = settings.config.bandGainsDb[i]
                val hz = EQ_BAND_HZ[i]
                BentoBandRow(
                    label = bandLabel(hz),
                    spoken = bandSpoken(hz),
                    valueText = formatDb(db),
                    value = db.toFloat(),
                    min = -EQ_MAX_GAIN_DB.toFloat(),
                    max = EQ_MAX_GAIN_DB.toFloat(),
                    enabled = on,
                    onSlide = { editor.setBand(i, it.toDouble()) },
                    onLower = { editor.nudgeBand(i, -1) },
                    onRaise = { editor.nudgeBand(i, 1) },
                )
                if (i < EQ_BAND_COUNT - 1) Spacer(Modifier.height(8.dp))
            }
        }

        // Headroom tile
        val auto = settings.config.preGainAuto
        val pre = settings.config.preGainDb
        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth().alpha(if (on) 1f else 0.55f), tone = BentoTone.Surface, small = true) {
            Row(
                Modifier.fillMaxWidth().toggleable(value = auto, enabled = on, role = Role.Switch, onValueChange = editor::setPreGainAuto),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Automatic headroom", style = BentoType.rowTitle(), color = p.text)
                    Text(
                        if (auto) "Automatic · ${formatDb(settings.config.effectivePreGainDb())}"
                        else "Manual · ${formatDb(pre)}",
                        style = BentoType.small(),
                        color = p.textMuted,
                    )
                }
                BentoSwitch(checked = auto)
            }
            if (!auto) {
                Spacer(Modifier.height(10.dp))
                BentoBandRow(
                    label = "Volume change",
                    spoken = "Volume change",
                    valueText = formatDb(pre),
                    value = pre.toFloat(),
                    min = -EQ_MAX_GAIN_DB.toFloat(),
                    max = 0f,
                    enabled = on,
                    onSlide = { editor.setPreGain(it.toDouble()) },
                    onLower = { editor.nudgePreGain(-1) },
                    onRaise = { editor.nudgePreGain(1) },
                )
            }
        }
    }
}

@Composable
private fun BentoBandRow(
    label: String,
    spoken: String,
    valueText: String,
    value: Float,
    min: Float,
    max: Float,
    enabled: Boolean,
    onSlide: (Float) -> Unit,
    onLower: () -> Unit,
    onRaise: () -> Unit,
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = BentoType.rowTitle(), color = p.text)
            Text(valueText, style = BentoType.time(), color = p.control)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BentoIconButton(BentoIcons.Minus, "Lower $spoken", onLower, small = true, enabled = enabled && value > min)
            Slider(
                value = value,
                onValueChange = onSlide,
                valueRange = min..max,
                enabled = enabled,
                modifier = Modifier.weight(1f).semantics {
                    contentDescription = spoken
                    stateDescription = valueText
                },
            )
            BentoIconButton(BentoIcons.Plus, "Raise $spoken", onRaise, small = true, enabled = enabled && value < max)
        }
    }
}

/**
 * Bento's Crossfade screen (BENTO_THEME.md step B5/B6, settings frame 4).
 * Switch tile, Sky stat tile with big seconds numeral and slider/± controls, and skip-same-album switch tile.
 */
@Composable
internal fun BentoCrossfade(
    settings: PlayerSettings,
    xf: CrossfadeSettings,
    maxBitRate: Int?,
    onBack: (() -> Unit)?,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val on = xf.enabled
    val seconds = CrossfadeSettings.sanitizeSeconds(xf.seconds)

    Column(
        Modifier
            .fillMaxWidth()
            .background(p.ink)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp + if (onBack != null) LocalBottomClearance.current else 0.dp),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        if (onBack != null) {
            BentoPageBar(onBack = onBack, title = "Crossfade")
        } else {
            Text(
                "Crossfade",
                style = BentoType.largeTitle(),
                color = p.text,
                modifier = Modifier.padding(horizontal = Side, vertical = 4.dp).semantics { heading() },
            )
        }

        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth(), tone = BentoTone.Sky, small = true) {
            Row(
                Modifier.fillMaxWidth().toggleable(value = on, role = Role.Switch, onValueChange = { settings.crossfadeEnabled = it }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Crossfade", style = BentoType.tileName(), color = t.onTone)
                    Text(CrossfadeText.switchHelp(on), style = BentoType.small(), color = t.onToneMuted)
                }
                BentoSwitch(checked = on)
            }
        }

        // Big stat numeral + length slider tile
        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth().alpha(if (on) 1f else 0.55f), tone = BentoTone.Surface) {
            BentoTileLabel("Fade length")
            Spacer(Modifier.height(4.dp))
            Text("${seconds}s", style = BentoType.stat(), color = p.text)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BentoIconButton(
                    BentoIcons.Minus,
                    "Shorten crossfade by one second",
                    { settings.crossfadeSeconds = CrossfadeText.nudge(seconds, -1) },
                    small = true,
                    enabled = on && CrossfadeText.canLower(seconds),
                )
                Slider(
                    value = seconds.toFloat(),
                    onValueChange = { settings.crossfadeSeconds = CrossfadeText.fromSlider(it) },
                    valueRange = CrossfadeSettings.MIN_SECONDS.toFloat()..CrossfadeSettings.MAX_SECONDS.toFloat(),
                    steps = CrossfadeSettings.MAX_SECONDS - CrossfadeSettings.MIN_SECONDS - 1,
                    enabled = on,
                    modifier = Modifier.weight(1f).semantics {
                        contentDescription = CrossfadeText.lengthSpoken(seconds)
                        stateDescription = CrossfadeText.length(seconds)
                    },
                )
                BentoIconButton(
                    BentoIcons.Plus,
                    "Lengthen crossfade by one second",
                    { settings.crossfadeSeconds = CrossfadeText.nudge(seconds, 1) },
                    small = true,
                    enabled = on && CrossfadeText.canRaise(seconds),
                )
            }
        }

        // Skip same album switch
        val keep = xf.skipSameAlbum
        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth().alpha(if (on) 1f else 0.55f), tone = BentoTone.Surface, small = true) {
            Row(
                Modifier.fillMaxWidth().toggleable(value = keep, enabled = on, role = Role.Switch, onValueChange = { settings.crossfadeSkipSameAlbum = it }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Keep album order gapless", style = BentoType.rowTitle(), color = p.text)
                    Text(CrossfadeText.keepAlbumsHelp(keep), style = BentoType.small(), color = p.textMuted)
                }
                BentoSwitch(checked = keep)
            }
        }

        CrossfadeText.qualityLine(maxBitRate)?.let { note ->
            BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth(), tone = BentoTone.Warm, small = true) {
                Text(note, style = BentoType.small(), color = t.onTone)
            }
        }
    }
}
