package app.discair.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.discair.lyrics.LyricsUiState
import app.discair.lyrics.lyricsFailureText
import app.discair.player.PlayerController
import app.discair.player.PlayerUiState
import app.discair.player.QueueItem
import app.discair.player.RepeatMode
import app.discair.player.qualityLabel
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoCover
import app.discair.ui.common.BentoCoverSize
import app.discair.ui.common.BentoEyebrow
import app.discair.ui.common.BentoFlight
import app.discair.ui.common.BentoIconButton
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPairRow
import app.discair.ui.common.BentoPlayingBars
import app.discair.ui.common.BentoRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.Cover
import app.discair.ui.common.TransparentSystemBars
import app.discair.ui.common.bentoPress
import app.discair.ui.common.bentoTones
import app.discair.ui.theme.BentoMotion
import app.discair.ui.theme.LocalPalette
import app.discair.ui.theme.rememberReducedMotion
import kotlinx.coroutines.launch

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Motion #7: Bento's full-screen Now Playing dialog frame. Rises in 420ms Emphasized while the dock slides down,
 * falls in 320ms EmphasizedExit while the dock returns. Instant under Remove animations.
 */
@Composable
internal fun BentoPlayerFrame(onDismiss: () -> Unit, content: @Composable (close: () -> Unit) -> Unit) {
    val reduced = rememberReducedMotion()
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        BentoFlight.playerUp = true
        onDispose { BentoFlight.playerUp = false }
    }

    val close: () -> Unit = {
        if (!closing) {
            closing = true
            BentoFlight.playerUp = false
            if (reduced) {
                onDismiss()
            } else {
                visible.targetState = false
                scope.launch {
                    kotlinx.coroutines.delay(BentoMotion.PLAYER_FALL_MS.toLong())
                    onDismiss()
                }
            }
        }
    }

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false),
    ) {
        TransparentSystemBars()
        AnimatedVisibility(
            visibleState = visible,
            enter = if (reduced) EnterTransition.None else
                slideInVertically(BentoMotion.tween(BentoMotion.PLAYER_RISE_MS, BentoMotion.Emphasized, reduced)) { it } +
                    fadeIn(BentoMotion.fade(220, reduced)),
            exit = if (reduced) ExitTransition.None else
                slideOutVertically(BentoMotion.tween(BentoMotion.PLAYER_FALL_MS, BentoMotion.EmphasizedExit, reduced)) { it } +
                    fadeOut(BentoMotion.fade(200, reduced)),
        ) {
            content(close)
        }
    }
}

/**
 * Bento's Now Playing, Queue, and Lyrics screen (BENTO_THEME.md step B5, player frames 1–3).
 * View 0: Player (rounded 28dp cover tile, leaf quality tag, 6dp control seek bar, 76dp Play circle, 4 tool tiles).
 * View 1: Lyrics (real synced/plain lyrics in a clean Bento tile).
 * View 2: Queue (playing track highlighted with BentoPlayingBars, upcoming tracks with tap-to-jump and remove).
 */
@Composable
internal fun BentoPlayer(
    state: PlayerUiState,
    controller: PlayerController,
    view: Int,
    onView: (Int) -> Unit,
    onClose: () -> Unit,
    onSignIn: () -> Unit,
    onEqualizer: () -> Unit,
    onCrossfade: () -> Unit,
    onOpenArtist: (QueueItem) -> Unit,
    onOpenAlbum: (QueueItem) -> Unit,
    input: CoverStackPlayerInput,
) {
    val p = LocalPalette.current
    val item = state.current ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(p.ink)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Side, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BentoIconButton(
                icon = if (view == 0) BentoIcons.Down else BentoIcons.Back,
                description = if (view == 0) "Close now playing" else "Back to player",
                onClick = { if (view == 0) onClose() else onView(0) },
                inHeader = true,
            )
            Text(
                when (view) {
                    1 -> "Lyrics"
                    2 -> "Queue"
                    else -> "Now playing"
                },
                style = BentoType.small().copy(fontWeight = FontWeight.SemiBold),
                color = p.textMuted,
                modifier = Modifier.semantics { heading() },
            )
            BentoIconButton(
                icon = if (view == 2) BentoIcons.MusicNote else BentoIcons.Queue,
                description = if (view == 2) "Player" else "Queue",
                onClick = { onView(if (view == 2) 0 else 2) },
                inHeader = true,
            )
        }

        when (view) {
            1 -> BentoLyricsView(item, input, Modifier.weight(1f))
            2 -> BentoQueueView(state, controller, Modifier.weight(1f))
            else -> BentoNowPlayingMain(
                state = state,
                item = item,
                controller = controller,
                line = input.line,
                onSignIn = onSignIn,
                onQueue = { onView(2) },
                onLyrics = { onView(1) },
                onEqualizer = onEqualizer,
                onCrossfade = onCrossfade,
                onOpenArtist = onOpenArtist,
                onOpenAlbum = onOpenAlbum,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BentoNowPlayingMain(
    state: PlayerUiState,
    item: QueueItem,
    controller: PlayerController,
    line: LineUi,
    onSignIn: () -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    onEqualizer: () -> Unit,
    onCrossfade: () -> Unit,
    onOpenArtist: (QueueItem) -> Unit,
    onOpenAlbum: (QueueItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    var dragging by remember { mutableStateOf<Float?>(null) }
    val duration = state.durationMs
    val fraction = dragging ?: if (duration > 0) (state.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val shown = if (dragging != null) (fraction * duration).toLong() else state.positionMs
    val quality = qualityLabel(item)

    Column(
        modifier.fillMaxWidth().padding(horizontal = Side),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        // Cover tile (28dp radius, centered square artwork)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight, 332.dp)
                val shape = RoundedCornerShape(BentoRules.TILE_LARGE_RADIUS.dp)
                Box(
                    Modifier
                        .size(side)
                        .clip(shape)
                        .background(p.surface)
                        .onGloballyPositioned { BentoFlight.cover = it },
                ) {
                    Cover(playerArtworkUrl(item.coverUrl), Modifier.fillMaxSize(), rect = true)
                    if (quality != null) {
                        BentoTag(quality, Modifier.align(Alignment.BottomStart).padding(12.dp), tone = BentoTone.Leaf)
                    }
                }
            }
        }

        // Title + artist/album + seek + transport tile
        BentoTile(Modifier.fillMaxWidth(), tone = BentoTone.Surface) {
            Text(
                item.title,
                style = BentoType.tileTitle().copy(fontSize = 22.sp, lineHeight = 25.sp),
                color = p.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; heading() },
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.artist.isNotBlank()) {
                    Text(
                        item.artist,
                        style = BentoType.body().copy(fontWeight = FontWeight.SemiBold),
                        color = p.control,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                            .clickable(role = Role.Button, onClickLabel = "Open artist") { onOpenArtist(item) },
                    )
                }
                if (item.artist.isNotBlank() && item.album.isNotBlank()) {
                    Text("·", style = BentoType.body(), color = p.textMuted)
                }
                if (item.album.isNotBlank()) {
                    Text(
                        item.album,
                        style = BentoType.body(),
                        color = p.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                            .clickable(role = Role.Button, onClickLabel = "Open album") { onOpenAlbum(item) },
                    )
                }
            }

            if (state.status != null || state.buffering) {
                Spacer(Modifier.height(6.dp))
                Text(
                    playerSubtitle(state),
                    style = BentoType.small(),
                    color = p.accent,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            if (state.signInRequired) {
                Spacer(Modifier.height(6.dp))
                BentoButton("Sign in again", { controller.dismissSignIn(); onSignIn() }, kind = BentoButtonKind.Outline, small = true)
            }
            if (state.waitingForNetwork) {
                Spacer(Modifier.height(6.dp))
                BentoButton("Retry now", controller::retryNow, kind = BentoButtonKind.Outline, small = true)
            }

            Spacer(Modifier.height(6.dp))
            SeekBar(
                fraction = fraction,
                enabled = duration > 0,
                onChange = { dragging = it },
                onFinish = { dragging?.let { controller.seekTo((it * duration).toLong()) }; dragging = null },
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Seek"
                    stateDescription = seekDescription(shown, duration)
                },
                peaks = line.peaks,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(shown), style = BentoType.time(), color = p.textMuted)
                Text(if (duration > 0) formatTime(duration) else "--:--", style = BentoType.time(), color = p.textMuted)
            }

            Spacer(Modifier.height(10.dp))
            // Transport row with 76dp Play circle
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val shuffle = state.queue.shuffle
                BentoIconButton(
                    icon = BentoIcons.Shuffle,
                    description = if (shuffle) "Shuffle on" else "Shuffle off",
                    onClick = { controller.setShuffle(!shuffle) },
                    solid = shuffle,
                )
                BentoIconButton(
                    icon = BentoIcons.Previous,
                    description = "Previous song",
                    onClick = controller::previous,
                )
                val playInteraction = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .bentoPress(playInteraction)
                        .size(BentoRules.PLAY_CIRCLE.dp)
                        .clip(CircleShape)
                        .background(p.control)
                        .clickable(interactionSource = playInteraction, indication = null, role = Role.Button, onClick = controller::playPause)
                        .semantics { contentDescription = if (state.isPlaying) "Pause" else "Play" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.isPlaying) BentoIcons.Pause else BentoIcons.Play,
                        null,
                        tint = p.onControl,
                        modifier = Modifier.size(BentoRules.ICON_PLAY_CIRCLE.dp),
                    )
                }
                BentoIconButton(
                    icon = BentoIcons.Next,
                    description = "Next song",
                    onClick = controller::next,
                    enabled = state.queue.items.size > 1,
                )
                val repeat = state.queue.repeat
                BentoIconButton(
                    icon = if (repeat == RepeatMode.One) BentoIcons.RepeatOne else BentoIcons.Repeat,
                    description = when (repeat) {
                        RepeatMode.Off -> "Repeat off"
                        RepeatMode.All -> "Repeat all"
                        RepeatMode.One -> "Repeat one"
                    },
                    onClick = controller::cycleRepeat,
                    solid = repeat != RepeatMode.Off,
                )
            }
        }

        // Four tool tiles: Queue / Lyrics / EQ / Crossfade
        Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolTile("Queue", BentoIcons.Queue, BentoTone.Warm, onQueue, Modifier.weight(1f))
            ToolTile("Lyrics", BentoIcons.Quote, BentoTone.Bloom, onLyrics, Modifier.weight(1f))
            ToolTile("EQ", BentoIcons.Tune, BentoTone.Leaf, onEqualizer, Modifier.weight(1f))
            ToolTile("Crossfade", BentoIcons.Swap, BentoTone.Sky, onCrossfade, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToolTile(
    label: String,
    icon: ImageVector,
    tone: BentoTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = bentoTones()
    BentoTile(
        modifier = modifier,
        tone = tone,
        small = true,
        padding = 10.dp,
        onClick = onClick,
        clickLabel = label,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, tint = t.onTone, modifier = Modifier.size(20.dp))
            Text(label, style = BentoType.tileLabel(), color = t.onTone, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BentoQueueView(state: PlayerUiState, controller: PlayerController, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val items = state.queue.items
    val currentIdx = state.queue.index
    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = Side, end = Side, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        if (items.isEmpty()) {
            item {
                BentoTile(Modifier.fillMaxWidth()) {
                    Text("The queue is empty.", style = BentoType.body(), color = p.textMuted)
                }
            }
        } else {
            item(key = "queue-card") {
                BentoTile(Modifier.fillMaxWidth(), tone = BentoTone.Surface) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val upcoming = GalleryPlayerRules.upcoming(currentIdx, items.size)
                        Text("${items.size} in queue", style = BentoType.tileTitle(), color = p.text)
                        if (upcoming.isNotEmpty()) {
                            BentoButton(
                                "Clear",
                                { upcoming.asReversed().forEach { controller.remove(it) } },
                                kind = BentoButtonKind.Outline,
                                small = true,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    items.forEachIndexed { idx, q ->
                        val active = idx == currentIdx
                        BentoRow(
                            title = q.title,
                            subtitle = listOfNotNull(q.artist.ifBlank { null }, q.album.ifBlank { null }).joinToString(" · "),
                            active = active,
                            showDivider = idx < items.lastIndex,
                            onClick = { controller.skipTo(idx) },
                            lead = {
                                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                    if (active) {
                                        BentoPlayingBars(state.isPlaying)
                                    } else {
                                        BentoCover(q.coverUrl.ifBlank { null }, Modifier.size(44.dp), sizeKind = BentoCoverSize.Row)
                                    }
                                }
                            },
                            trail = {
                                if (!active) {
                                    BentoIconButton(
                                        BentoIcons.Close,
                                        "Remove ${q.title} from queue",
                                        { controller.remove(idx) },
                                        small = true,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoLyricsView(item: QueueItem, input: CoverStackPlayerInput, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val listState = rememberLazyListState()
    LaunchedEffect(input.currentLine) {
        if (input.currentLine >= 0) {
            listState.animateScrollToItem((input.currentLine - 2).coerceAtLeast(0))
        }
    }
    Column(modifier.fillMaxWidth().padding(horizontal = Side, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(Gap)) {
        BentoTile(Modifier.fillMaxWidth(), tone = BentoTone.Bloom, small = true) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                BentoCover(item.coverUrl.ifBlank { null }, Modifier.size(48.dp), sizeKind = BentoCoverSize.Row)
                Column(Modifier.weight(1f)) {
                    Text(item.title, style = BentoType.tileName(), color = bentoTones().onTone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.artist, style = BentoType.small(), color = bentoTones().onToneMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        BentoTile(Modifier.weight(1f).fillMaxWidth(), tone = BentoTone.Surface) {
            when (val l = input.lyricsState) {
                LyricsUiState.Idle, is LyricsUiState.Loading -> Text("Loading lyrics…", style = BentoType.body(), color = p.textMuted)
                is LyricsUiState.None -> Text("No lyrics found for this song.", style = BentoType.body(), color = p.textMuted)
                is LyricsUiState.Unsupported -> Text("This server does not serve lyrics.", style = BentoType.body(), color = p.textMuted)
                is LyricsUiState.Failed -> {
                    Text(lyricsFailureText(l.problem), style = BentoType.body(), color = p.textMuted)
                    Spacer(Modifier.height(10.dp))
                    BentoButton("Try again", input.onTryAgain, kind = BentoButtonKind.Outline, small = true)
                }
                is LyricsUiState.Ready -> {
                    val lines = l.lyrics.lines
                    val timed = l.lyrics.timed
                    val activeStyle = BentoType.tileTitle()
                    val bodyStyle = BentoType.body().copy(fontSize = 16.sp, lineHeight = 24.sp)
                    if (lines.isEmpty()) {
                        Text("No lyrics text available.", style = BentoType.body(), color = p.textMuted)
                    } else {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            itemsIndexed(lines) { idx, lyric ->
                                if (lyric.text.isBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                } else {
                                    val active = timed && idx == input.currentLine
                                    Text(
                                        lyric.text,
                                        style = if (active) activeStyle else bodyStyle,
                                        color = if (active) p.control else p.text.copy(alpha = if (timed) 0.65f else 0.92f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
