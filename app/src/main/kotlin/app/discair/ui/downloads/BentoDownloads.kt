package app.discair.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.data.RowBusy
import app.discair.model.AcquisitionJob
import app.discair.model.Tone
import app.discair.ui.common.AlbumCover
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoTracker
import app.discair.ui.common.BentoType
import app.discair.ui.common.ReleaseEditionCovers
import app.discair.ui.theme.LocalPalette
import app.discair.ui.theme.Palette

/*
 * Bento's Downloads tiles (B6). Spec: BENTO_THEME.md section 10 (`downloads-stats-server`).
 * DownloadsScreen reaches this behind isBento(); the stream, actions, and confirmation sheet are shared.
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BentoJobTile(
    job: AcquisitionJob,
    busy: RowBusy?,
    secondsLeft: Int?,
    canPlay: Boolean,
    arrivedAt: String?,
    openAlbum: () -> Unit,
    onPlay: () -> Unit,
    onSkip: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val info = job.info
    val (reached, failedAt) = GalleryDownloadsRules.tracker(job)
    val tileTone = when {
        failedAt != null -> BentoTone.Warm
        job.isImported -> BentoTone.Surface
        else -> BentoTone.Surface
    }
    BentoTile(
        modifier = modifier.fillMaxWidth().padding(horizontal = BentoRules.SIDE_MARGIN.dp),
        tone = tileTone,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumCover(
                    job.artistName, job.albumName, null,
                    Modifier.size(56.dp)
                        .clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp))
                        .clickable(role = Role.Button, onClickLabel = "Open album", onClick = openAlbum),
                    rect = true,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        job.title,
                        style = BentoType.rowTitle(),
                        color = p.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(role = Role.Button, onClickLabel = "Open album", onClick = openAlbum),
                    )
                    Text(
                        IndexDownloadsRules.meta(job),
                        style = BentoType.small(),
                        color = p.textMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IndexDownloadsRules.figure(job)?.let { fig ->
                    if (fig == "!") {
                        BentoTag("Failed", tone = BentoTone.Warm)
                    } else {
                        Text("$fig%", style = BentoType.rank(), color = p.control)
                    }
                }
            }

            job.releaseGroup?.let { ReleaseEditionCovers(it) }

            BentoTracker(reachedSteps = reached, failedStep = failedAt)

            Text(
                listOfNotNull(
                    info.label,
                    IndexDownloadsRules.progress(job),
                    arrivedAt?.takeIf { job.isImported }?.let { "arrived $it" },
                ).joinToString(" · "),
                style = BentoType.small(),
                color = bentoToneColor(info.tone, p),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )

            listOfNotNull(job.message, job.error).forEach { msg ->
                Text(msg, style = BentoType.small(), color = p.textMuted)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (job.isImported && !job.isSong) {
                    BentoButton("Play", onPlay, icon = BentoIcons.Play, small = true, enabled = canPlay, tone = tileTone)
                }
                secondsLeft?.let { left ->
                    BentoButton(
                        when {
                            busy == RowBusy.Skipping -> "Requesting source switch…"
                            left > 0 -> "Use next source (${left}s)"
                            else -> "Switching source…"
                        },
                        onSkip,
                        kind = BentoButtonKind.Outline,
                        small = true,
                        enabled = left > 0 && busy == null,
                        tone = tileTone,
                    )
                }
                if (job.canRetry) {
                    BentoButton(
                        if (busy == RowBusy.Retrying) "Trying again…" else "Try again",
                        onRetry,
                        icon = BentoIcons.Refresh,
                        small = true,
                        enabled = busy == null,
                        tone = tileTone,
                    )
                }
                if (job.canDismiss) {
                    BentoButton(
                        if (busy == RowBusy.Dismissing) "Dismissing…" else "Dismiss",
                        onDismiss,
                        kind = BentoButtonKind.Outline,
                        small = true,
                        enabled = busy == null,
                        tone = tileTone,
                    )
                }
            }
        }
    }
}

private fun bentoToneColor(tone: Tone, p: Palette): Color = when (tone) {
    Tone.Ok -> p.ok
    Tone.Error -> p.chartRedText
    Tone.Caution -> p.caution
    Tone.Active -> p.control
    Tone.Muted -> p.textMuted
}
