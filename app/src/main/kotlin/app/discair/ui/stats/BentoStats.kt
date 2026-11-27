package app.discair.ui.stats

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.discair.data.ArtistSort
import app.discair.ui.common.BentoPairRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoTabs
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileLabel
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.ChartRules
import app.discair.ui.common.Load
import app.discair.ui.common.LocalRepository
import app.discair.ui.common.bentoTileEnter
import app.discair.ui.common.bentoTones
import app.discair.ui.common.rememberLoad
import app.discair.ui.theme.BentoMotion
import app.discair.ui.theme.LocalPalette
import app.discair.ui.theme.rememberReducedMotion

/*
 * Bento's Library stats tiles (B6). Spec: BENTO_THEME.md section 10 (`downloads-stats-server`).
 * StatsScreen reaches these behind isBento(); the ranges, series and charts are shared.
 */

@Composable
internal fun BentoStatsFigures(albums: Int?) {
    val repo = LocalRepository.current
    val artists = rememberLoad("stats_artist_total") { repo.artists("", ArtistSort.Name, null, null).total }
    val artistTotal = (artists.state as? Load.Ready)?.value
    val entries = buildList {
        albums?.let { add(Triple("Albums", it, BentoTone.Leaf)) }
        artistTotal?.let { add(Triple("Artists", it, BentoTone.Sun)) }
    }
    if (entries.isEmpty()) return

    BentoPairRow(
        Modifier.padding(horizontal = BentoRules.SIDE_MARGIN.dp),
        left = {
            val (label, count, tone) = entries[0]
            BentoStatTile(label, count, tone, Modifier.weight(1f).bentoTileEnter(0, "stats"))
        },
        right = {
            if (entries.size > 1) {
                val (label, count, tone) = entries[1]
                BentoStatTile(label, count, tone, Modifier.weight(1f).bentoTileEnter(1, "stats"))
            } else {
                Spacer(Modifier.weight(1f))
            }
        },
    )
}

@Composable
private fun BentoStatTile(label: String, count: Int, tone: BentoTone, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val t = bentoTones()
    val reduced = rememberReducedMotion()
    var arrived by rememberSaveable("bento_stat_$label") { mutableStateOf(false) }
    LaunchedEffect(count) { arrived = true }
    val shown by animateIntAsState(
        targetValue = if (reduced || arrived) count else 0,
        animationSpec = BentoMotion.tween(BentoMotion.STAT_COUNT_MS, BentoMotion.Emphasized, reduced),
        label = "bentoStatCount",
    )
    val finalText = ChartRules.formatCount(count.toDouble())
    BentoTile(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "$label, $finalText" },
        tone = tone,
    ) {
        BentoTileLabel(label, tone = tone)
        Text(
            ChartRules.formatCount(shown.toDouble()),
            style = BentoType.stat(),
            color = tone.content(p, t),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
internal fun BentoRanges(selected: StatsRange, onSelect: (StatsRange) -> Unit) {
    BentoTabs(
        labels = StatsRange.entries.map { CoverStackStatsRules.rangeLabel(it) },
        selectedIndex = StatsRange.entries.indexOf(selected),
        onSelect = { onSelect(StatsRange.entries[it]) },
        modifier = Modifier.padding(horizontal = BentoRules.SIDE_MARGIN.dp),
    )
}

@Composable
internal fun BentoFigure(figure: String, caption: String) {
    val p = LocalPalette.current
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        Text(figure, style = BentoType.stat(), color = p.text)
        Text(caption, style = BentoType.small(), color = p.textMuted, modifier = Modifier.padding(bottom = 6.dp))
    }
}

@Composable
internal fun BentoStatsCard(content: @Composable () -> Unit) {
    BentoTile(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BentoRules.SIDE_MARGIN.dp),
        tone = BentoTone.Surface,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}
