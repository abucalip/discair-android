package app.discair.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.model.DiscoveryEntry
import app.discair.model.DiscoveryList
import app.discair.model.InsightCard
import app.discair.model.InsightNarrative
import app.discair.model.InsightsResponse
import app.discair.model.TasteCard
import app.discair.model.TasteCardState
import app.discair.model.TasteProfiles
import app.discair.model.TasteResponse
import app.discair.model.pickReason
import app.discair.ui.LocalBottomClearance
import app.discair.ui.Opener
import app.discair.ui.album.BentoActionSheet
import app.discair.ui.common.AlbumCover
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoEyebrow
import app.discair.ui.common.BentoGetButton
import app.discair.ui.common.BentoIconButton
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoProgress
import app.discair.ui.common.BentoRankedBars
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoTabs
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileHeader
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.GalleryListsRules
import app.discair.ui.common.LocalAcquisitions
import app.discair.ui.common.SaveState
import app.discair.ui.common.bentoTileEnter
import app.discair.ui.common.bentoTones
import app.discair.ui.common.rememberAlbumYear
import app.discair.ui.common.routeKey
import app.discair.ui.common.sharedCover
import app.discair.ui.theme.LocalPalette

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Bento's Discover header: lens chips in [DiscoveryList] order (the shell already draws the top bar "Discover").
 */
@Composable
internal fun BentoDiscoverHead(list: DiscoveryList, onSelect: (DiscoveryList) -> Unit) {
    BentoTabs(
        labels = DiscoveryList.entries.map { it.label },
        selectedIndex = DiscoveryList.entries.indexOf(list).coerceAtLeast(0),
        onSelect = { onSelect(DiscoveryList.entries[it]) },
        modifier = Modifier.fillMaxWidth().padding(start = Side, end = Side, bottom = 10.dp),
    )
}

/**
 * Bento's Discover lists (BENTO_THEME.md step B4, discover frame 1).
 * The first entry in a lens renders as a hero pick tile with its reason, Get morph button, Save button, and 3-dot sheet
 * ("Not this album", "Never suggest this artist"); remaining entries render in clean rounded tiles.
 */
@Composable
internal fun BentoDiscoverList(
    list: DiscoveryList,
    sections: List<FlowDiscoverRules.GridSection>,
    open: Opener,
    saves: Map<String, SaveState>,
    defaultSaved: Boolean,
    onSave: (DiscoveryEntry) -> Unit,
    onDismiss: (DiscoveryEntry, Boolean) -> Unit,
    empty: @Composable () -> Unit,
) {
    val p = LocalPalette.current
    if (sections.all { it.title == null && it.entries.isEmpty() }) {
        empty()
        return
    }
    val pageKey = "bento-discover:${list.name}"
    val sharedOwner = remember(sections) {
        buildMap {
            for (sec in sections) for (e in sec.entries) {
                putIfAbsent(routeKey(e.item.artistName, e.item.album), FlowDiscoverRules.itemKey(sec, e))
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(p.ink),
        contentPadding = PaddingValues(start = Side, end = Side, top = 2.dp, bottom = 24.dp + LocalBottomClearance.current),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        var tileIdx = 0
        sections.forEachIndexed { sIdx, section ->
            if (section.title != null) {
                item(key = "sec-head:$sIdx:${section.title}") {
                    Row(
                        Modifier.fillMaxWidth().padding(top = if (sIdx == 0) 2.dp else 10.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(section.title, style = BentoType.tileTitle(), color = p.text, modifier = Modifier.semantics { heading() })
                        GalleryListsRules.countLine(section.count)?.let { cnt ->
                            Text(cnt, style = BentoType.small(), color = p.textMuted)
                        }
                    }
                }
            }
            section.note?.let { note ->
                item(key = "sec-note:$sIdx:${section.title}") {
                    BentoTile(Modifier.fillMaxWidth(), small = true) {
                        Text(note, style = BentoType.body(), color = p.textMuted)
                    }
                }
            }
            val startIdx = tileIdx
            tileIdx += section.entries.size
            val rotation = listOf(BentoTone.Leaf, BentoTone.Bloom, BentoTone.Warm, BentoTone.Sky, BentoTone.Sun)
            val heroEntry = if (sIdx == 0 && section.entries.isNotEmpty()) section.entries.first() else null
            val restEntries = if (heroEntry != null) section.entries.drop(1) else section.entries
            if (heroEntry != null) {
                item(key = FlowDiscoverRules.itemKey(section, heroEntry)) {
                    val albumKey = routeKey(heroEntry.item.artistName, heroEntry.item.album)
                    val sharedKey = albumKey.takeIf { sharedOwner[it] == FlowDiscoverRules.itemKey(section, heroEntry) }
                    val saveState = saves[heroEntry.key] ?: if (defaultSaved) SaveState.Saved else SaveState.Idle
                    BentoDiscoverEntryTile(
                        entry = heroEntry,
                        hero = true,
                        tone = BentoTone.Sun,
                        sharedKey = sharedKey,
                        open = open,
                        saveState = saveState,
                        onSave = { onSave(heroEntry) },
                        onDismiss = { artistToo -> onDismiss(heroEntry, artistToo) },
                        modifier = Modifier.animateItem().bentoTileEnter(startIdx, pageKey),
                    )
                }
            }
            val pairs = restEntries.chunked(2)
            items(pairs.size, key = { "pair:$sIdx:${FlowDiscoverRules.itemKey(section, pairs[it].first())}" }) { pIdx ->
                val pair = pairs[pIdx]
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                    pair.forEachIndexed { col, entry ->
                        val i = pIdx * 2 + col
                        val albumKey = routeKey(entry.item.artistName, entry.item.album)
                        val sharedKey = albumKey.takeIf { sharedOwner[it] == FlowDiscoverRules.itemKey(section, entry) }
                        val saveState = saves[entry.key] ?: if (defaultSaved) SaveState.Saved else SaveState.Idle
                        BentoDiscoverEntryTile(
                            entry = entry,
                            hero = false,
                            tone = rotation[i % rotation.size],
                            sharedKey = sharedKey,
                            open = open,
                            saveState = saveState,
                            onSave = { onSave(entry) },
                            onDismiss = { artistToo -> onDismiss(entry, artistToo) },
                            modifier = Modifier.weight(1f).bentoTileEnter(startIdx + 1 + i, pageKey),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BentoDiscoverEntryTile(
    entry: DiscoveryEntry,
    hero: Boolean,
    tone: BentoTone,
    sharedKey: String?,
    open: Opener,
    saveState: SaveState,
    onSave: () -> Unit,
    onDismiss: (artistToo: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val hub = LocalAcquisitions.current
    val item = entry.item
    val artist = item.artistName
    val year = rememberAlbumYear(artist, item.album, item.year)
    val openAlbum = { open.album(routeKey(artist, item.album), artist, item.album, null, item.year) }
    var menuOpen by remember(entry.key) { mutableStateOf(false) }

    if (menuOpen) {
        BentoActionSheet(
            eyebrow = "Recommendation",
            title = item.album,
            cover = { AlbumCover(artist, item.album, null, Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)), rect = true) },
            items = listOf(
                Triple(if (saveState == SaveState.Saved) "Remove saved" else "Save for later", BentoIcons.Bookmark, onSave),
                Triple("Open artist · $artist", BentoIcons.MusicNote) { open.artist(artist, null, null, null) },
                Triple("Not this album", BentoIcons.Close) { onDismiss(false) },
                Triple("Never suggest this artist", BentoIcons.Ban) { onDismiss(true) },
            ),
            onDismiss = { menuOpen = false },
        )
    }

    val fg = tone.content(p, t)
    val muted = tone.muted(p, t)
    val reason = pickReason(item.because, item.similarTo).ifBlank { null }
    val genre = GalleryListsRules.entryGenre(item.genres, item.genreRowLabel)

    BentoTile(modifier.fillMaxWidth(), tone = tone, small = !hero, padding = if (hero) 14.dp else 10.dp) {
        if (hero) {
            AlbumCover(
                artist, item.album, null,
                Modifier.fillMaxWidth().aspectRatio(1.35f)
                    .clip(RoundedCornerShape(BentoRules.COVER_LARGE_RADIUS.dp))
                    .then(if (sharedKey != null) Modifier.sharedCover(sharedKey) else Modifier)
                    .clickable(role = Role.Button, onClickLabel = "Open ${item.album}", onClick = openAlbum),
                rect = true,
            )
            Spacer(Modifier.height(10.dp))
            if (!reason.isNullOrBlank()) {
                Text(reason, style = BentoType.small(), color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
            }
            Text(
                item.album,
                style = BentoType.heroTitle(),
                color = fg,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(role = Role.Button, onClick = openAlbum),
            )
            Text(
                listOfNotNull(artist, year?.toString(), genre).joinToString(" · "),
                style = BentoType.small(),
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(role = Role.Button) { open.artist(artist, null, null, null) },
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                BentoGetButton(
                    state = hub.getState(artist, item.album),
                    onGet = { hub.getAlbum(artist, item.album, entry.raw) },
                    small = true,
                    tone = tone,
                )
                BentoButton(
                    label = when (saveState) {
                        SaveState.Saved -> "Saved"
                        SaveState.Pending -> "Saving…"
                        SaveState.Failed -> "Try again"
                        SaveState.Idle -> "Save"
                    },
                    onClick = onSave,
                    kind = BentoButtonKind.Outline,
                    small = true,
                    icon = if (saveState == SaveState.Saved) BentoIcons.Check else BentoIcons.Bookmark,
                    enabled = saveState != SaveState.Pending,
                    tone = tone,
                )
                BentoIconButton(
                    icon = BentoIcons.More,
                    description = "Options for ${item.album}",
                    onClick = { menuOpen = true },
                    small = true,
                    tone = tone,
                )
            }
        } else {
            AlbumCover(
                artist, item.album, null,
                Modifier.fillMaxWidth().aspectRatio(1f)
                    .clip(RoundedCornerShape(BentoRules.COVER_TILE_RADIUS.dp))
                    .then(if (sharedKey != null) Modifier.sharedCover(sharedKey) else Modifier)
                    .clickable(role = Role.Button, onClickLabel = "Open ${item.album}", onClick = openAlbum),
                rect = true,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                item.album,
                style = BentoType.rowTitle(),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(role = Role.Button, onClick = openAlbum),
            )
            Text(
                listOfNotNull(artist, year?.toString()).joinToString(" · "),
                style = BentoType.small(),
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(role = Role.Button) { open.artist(artist, null, null, null) },
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BentoGetButton(
                    state = hub.getState(artist, item.album),
                    onGet = { hub.getAlbum(artist, item.album, entry.raw) },
                    small = true,
                    tone = tone,
                )
                BentoIconButton(
                    icon = BentoIcons.More,
                    description = "Options for ${item.album}",
                    onClick = { menuOpen = true },
                    small = true,
                    tone = tone,
                )
            }
        }
    }
}

/**
 * Bento's Insights tab (BENTO_THEME.md step B4, discover frame 2).
 * Taste tile (ranked bars + full TasteSheet trigger) followed by insight cards in rotating pastel tone tiles.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BentoInsights(
    response: InsightsResponse,
    hidden: Set<String>,
    onDismiss: (InsightCard) -> Unit,
    taste: TasteResponse?,
    tasteLoaded: Boolean,
    showTaste: Boolean,
    open: Opener,
) {
    val p = LocalPalette.current
    val pageKey = "bento-insights"
    LazyColumn(
        Modifier.fillMaxSize().background(p.ink),
        contentPadding = PaddingValues(start = Side, end = Side, top = 2.dp, bottom = 24.dp + LocalBottomClearance.current),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        if (showTaste) {
            item(key = "taste") {
                BentoTasteTile(taste?.profiles, tasteLoaded, Modifier.bentoTileEnter(0, pageKey))
            }
        }
        val off = mutableListOf<String>()
        val nothing = mutableListOf<String>()
        var cardIdx = 0
        for (kind in lensKinds(response)) {
            val lens = response.lenses[kind]
            val cards = lens?.cards.orEmpty().filter { it.id !in hidden }
            when {
                lens?.enabled != true -> off += lensName(kind)
                cards.isEmpty() -> nothing += lensName(kind)
                else -> {
                    val start = cardIdx
                    cardIdx += cards.size
                    items(cards.size, key = { cards[it].id }) { i ->
                        val card = cards[i]
                        val tone = if (kind == "anniversary") BentoTone.Sun else BentoTone.rotate(start + i)
                        BentoInsightTile(
                            card = card,
                            tone = tone,
                            open = open,
                            onDismiss = onDismiss,
                            modifier = Modifier.animateItem().bentoTileEnter(1 + start + i, pageKey),
                        )
                    }
                }
            }
        }
        FlowDiscoverRules.insightsNote(off, nothing)?.let { note ->
            item(key = "note") {
                BentoTile(Modifier.fillMaxWidth(), small = true) {
                    Text(note, style = BentoType.small(), color = p.textMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BentoInsightTile(
    card: InsightCard,
    tone: BentoTone,
    open: Opener,
    onDismiss: (InsightCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = bentoTones()
    var why by remember(card.id) { mutableStateOf(false) }
    val artist = card.subject.artist
    val title = card.subject.title
    val openAlbum: (() -> Unit)? = if (artist != null && title != null) {
        { open.album(routeKey(artist, title), artist, title, null, card.subject.year) }
    } else null

    BentoTile(modifier.fillMaxWidth(), tone = tone) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            if (artist != null && title != null) {
                AlbumCover(
                    artist, title, null,
                    Modifier.size(64.dp).clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp)),
                    rect = true,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BentoEyebrow(lensName(card.kind), tone = tone)
                Text(card.title, style = BentoType.tileName(), color = t.onTone, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (!artist.isNullOrBlank()) Text(artist, style = BentoType.small(), color = t.onToneMuted, maxLines = 1)
            }
        }
        if (card.body.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(card.body, style = BentoType.body(), color = t.onTone)
        }
        for (run in card.missingRuns) {
            val span = if (run.firstPosition == run.lastPosition) "${run.firstPosition}" else "${run.firstPosition} to ${run.lastPosition}"
            Spacer(Modifier.height(6.dp))
            Text(
                "Missing $span" + if (run.labels.isEmpty()) "" else ": " + run.labels.joinToString(", "),
                style = BentoType.small(), color = t.onToneMuted,
            )
        }
        if (why) {
            Spacer(Modifier.height(8.dp))
            Text(InsightNarrative.why(card), style = BentoType.small(), color = t.onToneMuted)
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (openAlbum != null) {
                BentoButton("Open album", openAlbum, small = true, icon = BentoIcons.Play, tone = tone)
            }
            BentoButton(if (why) "Hide why" else "Why this?", { why = !why }, kind = BentoButtonKind.Outline, small = true, tone = tone)
            if (card.dismissible) {
                BentoButton(
                    "Dismiss",
                    { onDismiss(card) },
                    Modifier.semantics { contentDescription = "Dismiss ${card.title}" },
                    kind = BentoButtonKind.Outline,
                    small = true,
                    tone = tone,
                )
            }
        }
    }
}

@Composable
private fun BentoTasteTile(profiles: TasteProfiles?, loaded: Boolean, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val t = bentoTones()
    var sheet by remember { mutableStateOf(false) }
    val state = TasteCard.state(profiles)

    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Surface) {
        BentoTileHeader(
            title = "Your taste",
            actionLabel = if (state !is TasteCardState.Empty && profiles != null) "Details" else null,
            onAction = { sheet = true },
        )
        Spacer(Modifier.height(8.dp))
        when (state) {
            TasteCardState.Empty -> {
                Text(
                    if (loaded) "Taste needs listening history." else "Reading your saved profile…",
                    style = BentoType.rowTitle(), color = p.text,
                )
                if (loaded) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Connect ListenBrainz or keep playing. Once a window has enough listens your taste profile will appear here.",
                        style = BentoType.small(), color = p.textMuted,
                    )
                }
            }
            is TasteCardState.Thin -> {
                Text("Still getting to know you.", style = BentoType.rowTitle(), color = p.text)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Only ${state.have} listens in the recent window. Needs ${state.need} to measure a shift.",
                    style = BentoType.small(), color = p.textMuted,
                )
                Spacer(Modifier.height(10.dp))
                BentoProgress(TasteCard.progress(state.have, state.need).toFloat())
            }
            is TasteCardState.Ready -> {
                Text(state.headline.text, style = BentoType.body(), color = p.text, maxLines = 3, overflow = TextOverflow.Ellipsis)
                val w = profiles?.window(state.headline.window)
                val top = if (w == null || w.isLowSample("genre")) emptyList() else w.entries("genre").take(GalleryListsRules.BARS_SHOWN)
                if (top.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    BentoRankedBars(
                        items = top.map { it.value to it.share.toFloat() },
                        tone = BentoTone.Surface,
                        valueText = { idx -> "${TasteCard.percent(top[idx].share)}%" },
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(GalleryListsRules.listensLine(state.windowListens, state.allTimeListens), style = BentoType.small(), color = p.textMuted)
            }
        }
    }
    if (sheet && profiles != null) TasteSheet(profiles) { sheet = false }
}
