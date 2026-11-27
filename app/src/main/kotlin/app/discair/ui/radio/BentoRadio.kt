package app.discair.ui.radio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.model.RadioDownload
import app.discair.model.RadioNow
import app.discair.model.RadioPlay
import app.discair.model.RadioStation
import app.discair.model.radioLogoRequest
import app.discair.model.radioNowText
import app.discair.player.PlayerController
import app.discair.player.PlayerUiState
import app.discair.player.QueueItem
import app.discair.player.RadioLive
import app.discair.player.RadioPlayback
import app.discair.player.SleepTimer
import app.discair.ui.LocalBottomClearance
import app.discair.ui.common.BentoBanner
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoEyebrow
import app.discair.ui.common.BentoIconButton
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPageBar
import app.discair.ui.common.BentoPlayingBars
import app.discair.ui.common.BentoRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoSkeleton
import app.discair.ui.common.BentoTabs
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileLabel
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.Cover
import app.discair.ui.common.Load
import app.discair.ui.common.LocalRepository
import app.discair.ui.common.bentoPress
import app.discair.ui.common.bentoTileEnter
import app.discair.ui.common.bentoTones
import app.discair.ui.common.rememberLoad
import app.discair.ui.theme.LocalPalette

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Bento's Radio page (BENTO_THEME.md step B5, radio frames 1 & 3): Stations and Heard chips, stations as rotating pastel
 * tone tiles with live playing bars on the active station, and Heard on radio rows with In library / Get / Requested states.
 */
@Composable
internal fun BentoRadioPage(
    onBack: () -> Unit,
    stations: Load<List<RadioStation>>,
    onReload: () -> Unit,
    message: String?,
    favourite: (RadioStation) -> Boolean,
    now: (RadioStation) -> RadioNow?,
    onAirId: String?,
    playing: Boolean,
    onPlay: (RadioStation) -> Unit,
    onFavourite: (RadioStation) -> Unit,
) {
    val p = LocalPalette.current
    val repo = LocalRepository.current
    val radio = LocalRadio.current
    val live by radio.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val plays = rememberLoad("bento_radio_plays") { repo.radioPlays() }

    Column(Modifier.fillMaxSize().background(p.ink)) {
        BentoPageBar(onBack = onBack, title = "Radio")
        Text(
            "Radio",
            style = BentoType.largeTitle(),
            color = p.text,
            modifier = Modifier.padding(horizontal = Side, vertical = 4.dp).semantics { heading() },
        )
        BentoTabs(
            labels = listOf("Stations", "Heard on radio"),
            selectedIndex = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = Side, vertical = 8.dp),
        )
        if (tab == 0) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = Side, end = Side, bottom = 24.dp + LocalBottomClearance.current),
                verticalArrangement = Arrangement.spacedBy(Gap),
            ) {
                message?.let { msg ->
                    item {
                        BentoTile(Modifier.fillMaxWidth(), tone = BentoTone.Warm, small = true) {
                            Text(msg, style = BentoType.small(), color = bentoTones().onTone, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        }
                    }
                }
                when (stations) {
                    Load.Loading -> items(3) { BentoSkeleton(Modifier.fillMaxWidth().height(92.dp)) }
                    is Load.Failed -> item { BentoBanner("Stations unavailable", stations.failure.message, onRetry = onReload) }
                    is Load.Ready -> {
                        if (stations.value.isEmpty()) {
                            item {
                                BentoTile(Modifier.fillMaxWidth()) {
                                    Text("No radio stations configured in Navidrome.", style = BentoType.body(), color = p.textMuted)
                                }
                            }
                        } else {
                            val pairs = stations.value.chunked(2)
                            items(pairs.size, key = { "st-pair:${pairs[it].first().id}" }) { pIdx ->
                                val pair = pairs[pIdx]
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                                    pair.forEachIndexed { col, st ->
                                        val idx = pIdx * 2 + col
                                        val isOnAir = st.id == onAirId
                                        val isFav = favourite(st)
                                        val tone = if (isOnAir) BentoTone.Sun else BentoTone.rotate(idx)
                                        val t = bentoTones()
                                        val nowLine = stripText(st, now(st)).ifBlank { null }
                                        BentoTile(
                                            Modifier.weight(1f).bentoTileEnter(idx, "bento-radio-stations"),
                                            tone = tone,
                                            small = true,
                                            onClick = { onPlay(st) },
                                            clickLabel = "Play ${st.name}",
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                val logo = st.logoUrl?.let { radioLogoRequest(repo.absolute(it), 128) }
                                                Box(
                                                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(t.chip),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (logo != null) {
                                                        Cover(logo, Modifier.fillMaxSize(), rect = true)
                                                    } else {
                                                        Text(
                                                            st.name.take(2).uppercase(),
                                                            style = BentoType.tileName(),
                                                            color = t.onTone,
                                                        )
                                                    }
                                                }
                                                if (isOnAir) {
                                                    BentoPlayingBars(playing)
                                                } else {
                                                    BentoIconButton(
                                                        icon = BentoIcons.Heart,
                                                        description = if (isFav) "Remove ${st.name} from favourites" else "Add ${st.name} to favourites",
                                                        onClick = { onFavourite(st) },
                                                        solid = isFav,
                                                        small = true,
                                                        tone = tone,
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(14.dp))
                                            Text(st.name, style = BentoType.tileName(), color = t.onTone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                nowLine ?: if (isOnAir) "On air now" else "Tap to listen",
                                                style = BentoType.tileLabel(),
                                                color = t.onToneMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            BentoHeardBody(plays.state, plays.reload, live) { radio.download(it) }
        }
    }
}

/** Standalone Heard on radio route (opened from Home or Live Radio). */
@Composable
internal fun BentoHeard(
    onBack: () -> Unit,
    plays: Load<List<RadioPlay>>,
    onReload: () -> Unit,
    live: RadioLive,
    onDownload: (Long) -> Unit,
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize().background(p.ink)) {
        BentoPageBar(onBack = onBack, title = "Heard on radio")
        Text(
            "Heard on radio",
            style = BentoType.largeTitle(),
            color = p.text,
            modifier = Modifier.padding(horizontal = Side, vertical = 4.dp).semantics { heading() },
        )
        BentoHeardBody(plays, onReload, live, onDownload)
    }
}

@Composable
private fun BentoHeardBody(
    plays: Load<List<RadioPlay>>,
    onReload: () -> Unit,
    live: RadioLive,
    onDownload: (Long) -> Unit,
) {
    val p = LocalPalette.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Side, end = Side, bottom = 24.dp + LocalBottomClearance.current),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        live.message?.let { msg ->
            item(key = "msg") {
                BentoTile(Modifier.fillMaxWidth(), tone = BentoTone.Warm, small = true) {
                    Text(msg, style = BentoType.small(), color = bentoTones().onTone, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        }
        when (plays) {
            Load.Loading -> items(3) { BentoSkeleton(Modifier.fillMaxWidth().height(84.dp), small = true) }
            is Load.Failed -> item { BentoBanner("Could not load heard songs", plays.failure.message, onRetry = onReload) }
            is Load.Ready -> {
                if (plays.value.isEmpty()) {
                    item {
                        BentoTile(Modifier.fillMaxWidth()) {
                            Text(
                                "Nothing heard yet. A song is logged once you have listened to it for about 30 seconds.",
                                style = BentoType.body(), color = p.textMuted,
                            )
                        }
                    }
                } else {
                    item(key = "heard-list") {
                        BentoTile(Modifier.fillMaxWidth(), tone = BentoTone.Surface) {
                            plays.value.forEachIndexed { idx, play ->
                                val dl = live.downloadFor(play.id, play.grabbed, play.inLibrary)
                                val sub = listOfNotNull(play.artist, play.stationName.takeIf { it.isNotBlank() })
                                    .joinToString(" · ")
                                    .ifBlank { "Artist not named" }
                                val fail = live.failed[play.id]
                                BentoRow(
                                    title = play.title,
                                    subtitle = if (fail != null) "$sub · $fail" else sub,
                                    showDivider = idx < plays.value.lastIndex,
                                    trail = {
                                        if (play.hasArtist) {
                                            BentoRadioDownloadControl(dl) { onDownload(play.id) }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoRadioDownloadControl(state: RadioDownload, onClick: () -> Unit) {
    when (state) {
        RadioDownload.Waiting -> BentoButton("Get", onClick, kind = BentoButtonKind.Outline, small = true, enabled = false)
        RadioDownload.Ready -> BentoButton("Get", onClick, small = true, icon = BentoIcons.Get)
        RadioDownload.Working -> BentoButton("Getting", {}, kind = BentoButtonKind.Outline, small = true, enabled = false)
        RadioDownload.Done -> BentoTag("Requested", tone = BentoTone.Sky)
        RadioDownload.Owned -> BentoTag("In library", tone = BentoTone.Leaf)
        RadioDownload.Failed -> BentoButton("Try again", onClick, kind = BentoButtonKind.Outline, small = true)
    }
}

/**
 * Bento's Live Radio player (BENTO_THEME.md step B5, radio frame 2).
 */
@Composable
internal fun BentoRadioNow(
    state: PlayerUiState,
    controller: PlayerController,
    radio: RadioPlayback,
    sleep: SleepTimer,
    live: RadioLive,
    item: QueueItem,
    artwork: String?,
    status: String?,
    onClose: () -> Unit,
    onOpenHeard: () -> Unit,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val (headline, under) = CoverStackRadioRules.headline(item.title, live.now, item.artist)
    val by = GalleryRadioRules.byLine(live.now, under)
    val playId = live.now?.playId
    Column(
        Modifier
            .fillMaxSize()
            .background(p.ink)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = Side),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            BentoIconButton(BentoIcons.Down, "Close radio player", onClose, inHeader = true)
            Text("Live radio", style = BentoType.small().copy(fontWeight = FontWeight.SemiBold), color = p.textMuted)
            BentoButton("Heard", onOpenHeard, kind = BentoButtonKind.Outline, small = true)
        }

        // Station / song cover tile
        BentoTile(Modifier.weight(1f).fillMaxWidth(), tone = BentoTone.Sun) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(212.dp).clip(RoundedCornerShape(24.dp)).background(t.chip),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (artwork != null) {
                            Cover(artwork, Modifier.fillMaxSize(), rect = true)
                        } else {
                            Icon(BentoIcons.Radio, null, tint = p.control, modifier = Modifier.size(64.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoPlayingBars(state.isPlaying)
                        Text(status?.takeIf { it.isNotBlank() } ?: "Live", style = BentoType.small(), color = t.onTone)
                    }
                }
            }
        }

        // Info + controls directly on ink
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp)) {
            BentoEyebrow(IndexRadioRules.airLine(item.title, state.isPlaying))
            Spacer(Modifier.height(4.dp))
            Text(
                headline,
                style = BentoType.heroTitle(),
                color = p.text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite; heading() },
            )
            if (by.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(by, style = BentoType.body(), color = p.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                val fav = live.favourite
                BentoIconButton(
                    icon = BentoIcons.Heart,
                    description = if (fav) "Remove favourite" else "Add favourite",
                    onClick = radio::toggleFavourite,
                    solid = fav,
                )
                val playInt = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .bentoPress(playInt)
                        .size(BentoRules.PLAY_CIRCLE.dp)
                        .clip(CircleShape)
                        .background(p.control)
                        .clickable(interactionSource = playInt, indication = null, role = Role.Button, onClick = controller::playPause)
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
                if (live.proxied && playId != null) {
                    BentoRadioDownloadControl(live.download) { radio.download(playId) }
                } else {
                    BentoIconButton(BentoIcons.Radio, "Heard on radio", onOpenHeard)
                }
            }
        }
    }
}
