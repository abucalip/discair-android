package app.discair.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.data.Shelves
import app.discair.model.FeedbackAction
import app.discair.model.GenreTile
import app.discair.model.GetState
import app.discair.model.InsightCard
import app.discair.model.InsightNarrative
import app.discair.model.Pick
import app.discair.model.RadioDownload
import app.discair.model.RadioPlay
import app.discair.model.RadioStation
import app.discair.model.ShelfAlbum
import app.discair.model.radioLogoRequest
import app.discair.ui.Opener
import app.discair.ui.common.AlbumCover
import app.discair.ui.common.ArtistPhoto
import app.discair.ui.common.BentoBanner
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoTracker
import app.discair.ui.common.BentoChip
import app.discair.ui.common.BentoCover
import app.discair.ui.common.BentoCoverSize
import app.discair.ui.common.BentoCoverStack
import app.discair.ui.common.BentoEyebrow
import app.discair.ui.common.BentoGetButton
import app.discair.ui.common.BentoIconButton
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPairRow
import app.discair.ui.common.BentoRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoSkeleton
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileHeader
import app.discair.ui.common.BentoTileLabel
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.Cover
import app.discair.ui.common.Load
import app.discair.ui.common.LocalAcquisitions
import app.discair.ui.common.LocalRepository
import app.discair.ui.common.ModalBottomSheet
import app.discair.ui.common.bentoTileEnter
import app.discair.ui.common.bentoTones
import app.discair.ui.common.pageMetadata
import app.discair.ui.common.plural
import app.discair.ui.common.rememberAlbumYear
import app.discair.ui.common.rememberLoad
import app.discair.ui.common.routeKey
import app.discair.ui.common.sharedCover
import app.discair.ui.discover.lensName
import app.discair.ui.player.LocalPlayActions
import app.discair.ui.radio.LocalRadio
import app.discair.ui.theme.BentoMotion
import app.discair.ui.theme.LocalPalette
import app.discair.ui.theme.rememberReducedMotion
import java.time.Instant

/*
 * Bento's Home (Layout.Bento). Spec: BENTO_THEME.md section 9.
 * Rounded tiles in a 2-column grid (10dp gap, 14dp side margins), five pastel tones in rotation,
 * one deep Discover teaser, swipeable Pick pager with morphing Get button, fanned cover stack, and stat count-up.
 */
@Composable
internal fun BentoHome(
    data: GalleryHomeData,
    listensTotal: Int?,
    listensByDay: List<Int>,
    open: Opener,
    feedback: (FeedbackAction, Pick) -> Unit,
    onRadio: () -> Unit,
    onDiscover: () -> Unit,
    onLibrary: () -> Unit,
    onStats: () -> Unit,
    onServer: () -> Unit,
    onDownloads: () -> Unit,
) {
    val repo = LocalRepository.current
    val now = remember { Instant.now() }
    val newest = rememberLoad("shelf_newest_20") { repo.shelf(Shelves.Newest, 20).items }
    val recent = rememberLoad("shelf_recent_20") { repo.shelf(Shelves.Recent, 20).items }
    val frequent = rememberLoad("shelf_frequent_20") { repo.shelf(Shelves.Frequent, 20).items }
    val stations = rememberLoad("radio_stations") { repo.radioStations() }
    val plays = rememberLoad("radio_plays") { repo.radioPlays() }
    val artistPage = rememberLoad("home_artist_counts") { repo.artists("", app.discair.data.ArtistSort.Albums, true, null) }

    val newestList = (newest.state as? Load.Ready)?.value.orEmpty()
    val recentList = (recent.state as? Load.Ready)?.value.orEmpty()
    val frequentList = (frequent.state as? Load.Ready)?.value.orEmpty()
    val stationList = (stations.state as? Load.Ready)?.value.orEmpty()
    val playList = (plays.state as? Load.Ready)?.value.orEmpty()
    val genreList = (data.genres as? Load.Ready)?.value.orEmpty()
    val artists = HomeArtists.from(recentList, frequentList)
    val counts = (artistPage.state as? Load.Ready)?.value?.items.orEmpty().associate { it.name.lowercase() to it.albumCount }
    val artistTotal = (artistPage.state as? Load.Ready)?.value?.total
    val listeningKnown = recent.state !is Load.Loading && frequent.state !is Load.Loading

    val bottom = 28.dp + app.discair.ui.LocalBottomClearance.current
    val margin = BentoRules.SIDE_MARGIN.dp
    val gap = BentoRules.GRID_GAP.dp

    val availability = BentoHomeRules.Availability(
        hasBanner = data.banner != null,
        hasPickOrLoading = data.picks is Load.Loading || data.picks is Load.Failed || data.pickList.isNotEmpty(),
        hasHeroTrio = true,
        hasNewlyAdded = newestList.isNotEmpty(),
        hasPlayedOrGenres = recentList.isNotEmpty() || genreList.isNotEmpty(),
        hasMostPlayed = frequentList.isNotEmpty(),
        hasDiscoverTeaser = data.pickList.isNotEmpty(),
        hasRadioSection = stationList.isNotEmpty(),
        hasInsight = data.insight != null,
        hasArrivals = data.arrivals.isNotEmpty(),
        hasLibraryLine = BentoHomeRules.libraryLine(data.hero?.count, artistTotal) != null,
    )
    val visible = BentoHomeRules.visibleTiles(availability)

    LazyColumn(
        Modifier.fillMaxSize().background(LocalPalette.current.ink),
        contentPadding = PaddingValues(start = margin, end = margin, top = 4.dp, bottom = bottom),
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        var tileIdx = 0
        visible.forEach { kind ->
            val idx = tileIdx++
            when (kind) {
                BentoHomeRules.TileKind.Banner -> item(key = "banner") {
                    data.banner?.let {
                        BentoBanner(
                            title = it,
                            onRetry = data.retryPicks,
                            modifier = Modifier.bentoTileEnter(idx, "bento_home").clickable(role = Role.Button, onClick = onServer),
                        )
                    }
                }

                BentoHomeRules.TileKind.Pick -> item(key = "pick") {
                    BentoPickTile(data.picks, data.pickList, data.retryPicks, open, feedback, Modifier.bentoTileEnter(idx, "bento_home"))
                }

                BentoHomeRules.TileKind.HeroTrio -> item(key = "hero_trio") {
                    BentoHeroTrio(
                        artist = artists.firstOrNull(),
                        artistAlbums = artists.firstOrNull()?.let { counts[it.name.lowercase()] },
                        listeningKnown = listeningKnown,
                        station = stationList.firstOrNull(),
                        listensTotal = listensTotal,
                        weeklyBars = BentoHomeRules.weeklyBars(listensByDay),
                        open = open,
                        onRadio = onRadio,
                        onStats = onStats,
                        modifier = Modifier.bentoTileEnter(idx, "bento_home"),
                    )
                }

                BentoHomeRules.TileKind.NewlyAdded -> item(key = "newly_added") {
                    BentoNewlyAddedTile(newestList.take(BentoHomeRules.NEWLY_ADDED_SHOWN), open, onLibrary, Modifier.bentoTileEnter(idx, "bento_home"))
                }

                BentoHomeRules.TileKind.PlayedAndGenres -> item(key = "played_genres") {
                    BentoPlayedAndGenresRow(recentList, genreList, open, onLibrary, Modifier.bentoTileEnter(idx, "bento_home"))
                }

                BentoHomeRules.TileKind.MostPlayed -> item(key = "most_played") {
                    BentoMostPlayedTile(frequentList.take(BentoHomeRules.MOST_PLAYED_SHOWN), open, Modifier.bentoTileEnter(idx, "bento_home"))
                }

                BentoHomeRules.TileKind.DiscoverTeaser -> item(key = "discover_teaser") {
                    BentoDiscoverTeaserTile(data.pickList, onDiscover, Modifier.bentoTileEnter(idx, "bento_home"))
                }

                BentoHomeRules.TileKind.RadioSection -> item(key = "radio_section") {
                    BentoRadioSectionTile(stationList, playList, now, onRadio, Modifier.bentoTileEnter(idx, "bento_home"))
                }

                BentoHomeRules.TileKind.ThisWeek -> item(key = "this_week") {
                    data.insight?.let { BentoThisWeekTile(it, open, Modifier.bentoTileEnter(idx, "bento_home")) }
                }

                BentoHomeRules.TileKind.Arrivals -> item(key = "arrivals") {
                    BentoArrivalsTile(data.arrivals.take(BentoHomeRules.ARRIVALS_SHOWN), open, onDownloads, Modifier.bentoTileEnter(idx, "bento_home"))
                }

                BentoHomeRules.TileKind.LibraryLine -> item(key = "library_line") {
                    BentoHomeRules.libraryLine(data.hero?.count, artistTotal)?.let { line ->
                        BentoLibraryLineTile(line, onStats, Modifier.bentoTileEnter(idx, "bento_home"))
                    }
                }
            }
        }
    }
}

// ----- Pick tile (span2, white, swipeable HorizontalPager) ------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BentoPickTile(
    state: Load<List<Pick>>,
    picks: List<Pick>,
    retry: () -> Unit,
    open: Opener,
    feedback: (FeedbackAction, Pick) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val hub = LocalAcquisitions.current
    val reduced = rememberReducedMotion()
    var menuPick by remember { mutableStateOf<Pick?>(null) }

    when (state) {
        Load.Loading -> BentoSkeleton(modifier.fillMaxWidth().height(176.dp))
        is Load.Failed -> BentoBanner(state.failure.message, onRetry = retry, modifier = modifier)
        is Load.Ready -> {
            if (picks.isEmpty()) return
            val pager = rememberPagerState { picks.size }
            val slidePx = with(LocalDensity.current) { BentoMotion.PAGER_EYEBROW_SLIDE_DP.dp.roundToPx() }
            BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Surface) {
                HorizontalPager(
                    state = pager,
                    modifier = Modifier.fillMaxWidth().semantics {
                        if (picks.size > 1) stateDescription = "Swipe left or right for another pick"
                    },
                ) { page ->
                    val pick = picks.getOrNull(page) ?: return@HorizontalPager
                    val year = rememberAlbumYear(pick.artist, pick.album, pick.year)
                    val key = routeKey(pick.artist, pick.album)
                    val openAlbum = { open.album(key, pick.artist, pick.album, null, year) }
                    val getState = hub.getState(pick.artist, pick.album)
                    var saved by remember(pick) { mutableStateOf(false) }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                        AlbumCover(
                            pick.artist, pick.album, null,
                            Modifier
                                .size(124.dp)
                                .clip(RoundedCornerShape(BentoRules.COVER_LARGE_RADIUS.dp))
                                .then(if (page == pager.settledPage) Modifier.sharedCover(key) else Modifier)
                                .clickable(role = Role.Button, onClickLabel = "Open ${pick.album}", onClick = openAlbum),
                            rect = true,
                        )
                        Column(
                            Modifier.weight(1f).heightIn(min = 124.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                AnimatedContent(
                                    targetState = page + 1,
                                    transitionSpec = {
                                        (slideInVertically(BentoMotion.fade(BentoMotion.PAGER_EYEBROW_MS, reduced)) { slidePx } +
                                            fadeIn(BentoMotion.fade(BentoMotion.PAGER_EYEBROW_MS, reduced))) togetherWith
                                            (slideOutVertically(BentoMotion.fade(BentoMotion.PAGER_EYEBROW_MS, reduced)) { -slidePx } +
                                                fadeOut(BentoMotion.fade(BentoMotion.PAGER_EYEBROW_MS, reduced)))
                                    },
                                    label = "bentoPickCounter",
                                ) { n ->
                                    BentoEyebrow("Recommended · $n/${picks.size}")
                                }
                                Text(
                                    pick.album,
                                    style = BentoType.heroTitle(),
                                    color = p.text,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.clickable(role = Role.Button, onClick = openAlbum),
                                )
                                Text(
                                    BentoHomeRules.artistYear(pick.artist, year),
                                    style = BentoType.small(),
                                    color = p.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (getState !is GetState.Requested) {
                                    Box(Modifier.padding(top = 2.dp)) {
                                        BentoTag("Not in your library", tone = BentoTone.Warm)
                                    }
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth().padding(top = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BentoGetButton(
                                    state = getState,
                                    onGet = { hub.getAlbum(pick.artist, pick.album, pageMetadata(pick.artist, pick.album, year, pick.releaseGroupMbid)) },
                                    small = true,
                                    idleLabel = "Get",
                                )
                                BentoIconButton(
                                    icon = if (saved) BentoIcons.BookmarkFilled else BentoIcons.Bookmark,
                                    description = if (saved) "Saved" else "Save",
                                    onClick = { if (!saved) { saved = true; feedback(FeedbackAction.Save, pick) } },
                                    small = true,
                                    enabled = !saved,
                                )
                                BentoIconButton(
                                    icon = BentoIcons.More,
                                    description = "More for ${pick.album}",
                                    onClick = { menuPick = pick },
                                    small = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    menuPick?.let { pick ->
        ModalBottomSheet(onDismissRequest = { menuPick = null }) {
            Column(
                Modifier.fillMaxWidth().padding(start = BentoRules.SIDE_MARGIN.dp, end = BentoRules.SIDE_MARGIN.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    AlbumCover(pick.artist, pick.album, null, Modifier.size(54.dp).clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp)), rect = true)
                    Column(Modifier.weight(1f)) {
                        BentoEyebrow(pick.artist)
                        Text(pick.album, style = BentoType.tileName(), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                BentoRow(
                    title = "Not this album",
                    subtitle = "Removes it from your picks",
                    showDivider = true,
                    onClick = { menuPick = null; feedback(FeedbackAction.Dismiss, pick) },
                    lead = { BentoIconButton(BentoIcons.Close, "Not this album", { menuPick = null; feedback(FeedbackAction.Dismiss, pick) }) },
                )
                BentoRow(
                    title = "Never suggest this artist",
                    subtitle = "${pick.artist} leaves every list",
                    onClick = { menuPick = null; feedback(FeedbackAction.DismissArtist, pick) },
                    lead = { BentoIconButton(BentoIcons.Ban, "Never suggest this artist", { menuPick = null; feedback(FeedbackAction.DismissArtist, pick) }) },
                )
            }
        }
    }
}

// ----- Hero trio: tall Warm Artist + Sun Radio + Leaf Stat ------------------------------------------------------

@Composable
private fun BentoHeroTrio(
    artist: HomeArtist?,
    artistAlbums: Int?,
    listeningKnown: Boolean,
    station: RadioStation?,
    listensTotal: Int?,
    weeklyBars: List<Float>,
    open: Opener,
    onRadio: () -> Unit,
    onStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val radio = LocalRadio.current
    val reduced = rememberReducedMotion()

    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(BentoRules.GRID_GAP.dp)) {
        // Left: Tall Warm Artist tile
        BentoTile(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            tone = BentoTone.Warm,
            padding = 10.dp,
            onClick = artist?.let { a -> { open.artist(a.name, null, a.id.ifBlank { null }, null) } },
            clickLabel = artist?.let { "Open ${it.name}" },
        ) {
            if (artist != null) {
                ArtistPhoto(
                    artist.name, null,
                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(BentoRules.ARTIST_TILE_RADIUS.dp)),
                    rect = true,
                )
                Spacer(Modifier.height(10.dp))
                BentoTileLabel("Artist you return to", tone = BentoTone.Warm)
                Text(
                    artist.name,
                    style = BentoType.tileName(),
                    color = t.onTone,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                GalleryHomeRules.albumCount(artistAlbums)?.let {
                    Text(it, style = BentoType.small(), color = t.onToneMuted, modifier = Modifier.padding(top = 2.dp))
                }
            } else {
                BentoTileLabel("Artists", tone = BentoTone.Warm)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (listeningKnown) BentoHomeRules.EMPTY_LISTENING else "Loading…",
                    style = BentoType.body(),
                    color = t.onTone,
                )
            }
        }

        // Right column: Sun Radio tile + Leaf Stat tile
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BentoRules.GRID_GAP.dp)) {
            BentoTile(
                modifier = Modifier.fillMaxWidth(),
                tone = BentoTone.Sun,
                small = true,
                onClick = onRadio,
                clickLabel = "Open Radio",
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Icon(BentoIcons.Radio, null, tint = t.onTone, modifier = Modifier.size(22.dp))
                    if (station != null) {
                        BentoIconButton(
                            BentoIcons.Play, "Play ${station.name}",
                            onClick = { radio.play(station); onRadio() },
                            solid = true, small = true,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                BentoTileLabel(if (station != null) "Radio · ${station.name}" else "Radio", tone = BentoTone.Sun)
                Text(
                    if (station != null) "On air now" else "Stations",
                    style = BentoType.tileName(),
                    color = t.onTone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // Leaf Stat tile (motion #12 count-up on first arrival)
            var counted by rememberSaveable { mutableStateOf(false) }
            val targetCount = listensTotal ?: 0
            LaunchedEffect(targetCount) { counted = true }
            val shownCount by animateIntAsState(
                targetValue = if (counted || reduced) targetCount else 0,
                animationSpec = BentoMotion.tween(BentoMotion.STAT_COUNT_MS, BentoMotion.Emphasized, reduced),
                label = "bentoStatCount",
            )
            BentoTile(
                modifier = Modifier.fillMaxWidth(),
                tone = BentoTone.Leaf,
                small = true,
                onClick = onStats,
                clickLabel = "Open Library stats",
            ) {
                BentoTileLabel("This month", tone = BentoTone.Leaf)
                Text(
                    if (listensTotal != null) "$shownCount" else "—",
                    style = BentoType.stat(),
                    color = t.onTone,
                    modifier = Modifier.padding(top = 2.dp).semantics {
                        if (listensTotal != null) contentDescription = "$listensTotal plays this month"
                    },
                )
                if (weeklyBars.isNotEmpty()) {
                    val maxBar = (weeklyBars.maxOrNull() ?: 1f).coerceAtLeast(1f)
                    Row(
                        Modifier.fillMaxWidth().height(20.dp).padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        weeklyBars.forEachIndexed { i, v ->
                            val frac = (v / maxBar).coerceIn(0.18f, 1f)
                            Box(
                                Modifier.weight(1f).fillMaxHeight(frac)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (i == weeklyBars.lastIndex) p.control else t.onTone.copy(alpha = 0.30f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----- Newly added (span2, white) ------------------------------------------------------------------------------

@Composable
private fun BentoNewlyAddedTile(albums: List<ShelfAlbum>, open: Opener, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Surface) {
        BentoTileHeader("Newly added", actionLabel = "See all", onAction = onSeeAll)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            albums.forEach { album ->
                val key = routeKey(album.artist, album.name)
                Column(
                    Modifier.weight(1f).clickable(role = Role.Button) {
                        open.album(key, album.artist, album.name, album.id.ifBlank { null }, album.year)
                    },
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    AlbumCover(
                        album.artist, album.name, album.artworkUrl,
                        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(BentoRules.COVER_TILE_RADIUS.dp)),
                        rect = true,
                    )
                    Text(album.name, style = BentoType.small(), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            repeat((BentoHomeRules.NEWLY_ADDED_SHOWN - albums.size).coerceAtLeast(0)) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ----- Recently played (Bloom cover stack) + Genres (Sky chips) ------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BentoPlayedAndGenresRow(
    recent: List<ShelfAlbum>,
    genres: List<GenreTile>,
    open: Opener,
    onLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = bentoTones()
    val shownGenres = remember(genres) { genres.take(BentoHomeRules.GENRES_SHOWN) }
    BentoPairRow(
        modifier = modifier,
        left = {
            if (recent.isNotEmpty()) {
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val latest = recent.first()
                BentoTile(
                    modifier = Modifier.weight(1f),
                    tone = BentoTone.Bloom,
                    small = true,
                    onClick = { open.album(routeKey(latest.artist, latest.name), latest.artist, latest.name, latest.id.ifBlank { null }, latest.year) },
                    clickLabel = "Open ${latest.name}",
                ) {
                    BentoTileLabel("Recently played", tone = BentoTone.Bloom)
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        BentoCoverStack(recent.take(3).map { it.artworkUrl }, coverSize = 58.dp, pressed = pressed)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(latest.name, style = BentoType.tileName(), color = t.onTone, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(latest.artist, style = BentoType.small(), color = t.onToneMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
        right = {
            if (shownGenres.isNotEmpty()) {
                BentoTile(modifier = Modifier.weight(1f), tone = BentoTone.Sky, small = true, onClick = onLibrary) {
                    BentoTileLabel("Genres", tone = BentoTone.Sky)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        shownGenres.forEach { g ->
                            BentoChip(g.name, selected = false, onClick = { open.genre(g.tag, g.name) }, small = true, tone = BentoTone.Sky)
                        }
                    }
                }
            }
        },
    )
}

// ----- Most played (span2, white) ------------------------------------------------------------------------------

@Composable
private fun BentoMostPlayedTile(albums: List<ShelfAlbum>, open: Opener, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val play = LocalPlayActions.current
    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Surface) {
        BentoTileHeader("Most played")
        Spacer(Modifier.height(4.dp))
        albums.forEachIndexed { idx, album ->
            val key = routeKey(album.artist, album.name)
            val id = album.id.ifBlank { null }
            BentoRow(
                title = album.name,
                subtitle = BentoHomeRules.artistYear(album.artist, album.year),
                showDivider = idx < albums.lastIndex,
                onClick = { open.album(key, album.artist, album.name, id, album.year) },
                lead = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${idx + 1}", style = BentoType.rank(), color = p.control, modifier = Modifier.width(18.dp))
                        AlbumCover(
                            album.artist, album.name, album.artworkUrl,
                            Modifier.size(46.dp).clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp)),
                            rect = true,
                        )
                    }
                },
                trail = {
                    if (id != null) {
                        BentoIconButton(BentoIcons.Play, "Play ${album.name}", { play.playAlbum(id) }, small = true)
                    }
                },
            )
        }
    }
}

// ----- Discover teaser (span2, deep) ---------------------------------------------------------------------------

@Composable
private fun BentoDiscoverTeaserTile(picks: List<Pick>, onDiscover: () -> Unit, modifier: Modifier = Modifier) {
    val t = bentoTones()
    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Deep, onClick = onDiscover, clickLabel = "Open Discover") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.width(74.dp).height(58.dp), contentAlignment = Alignment.CenterStart) {
                picks.getOrNull(1)?.let { second ->
                    AlbumCover(
                        second.artist, second.album, null,
                        Modifier.offset(x = 20.dp).size(50.dp).clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp)),
                        rect = true,
                    )
                }
                picks.firstOrNull()?.let { first ->
                    AlbumCover(
                        first.artist, first.album, null,
                        Modifier.size(54.dp).clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp)),
                        rect = true,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                BentoEyebrow("Discover · Daily picks", tone = BentoTone.Deep)
                Text(
                    "${plural(picks.size, "album")} picked for you",
                    style = BentoType.tileTitle(),
                    color = t.onDeep,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(BentoIcons.Chevron, null, tint = t.deepEyebrow, modifier = Modifier.size(22.dp))
        }
    }
}

// ----- Radio section (span2, white) ----------------------------------------------------------------------------

@Composable
private fun BentoRadioSectionTile(
    stations: List<RadioStation>,
    plays: List<RadioPlay>,
    now: Instant,
    onRadio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val repo = LocalRepository.current
    val radio = LocalRadio.current
    val live by radio.state.collectAsState()
    val plates = GalleryHomeRules.plates(stations, plays, now).take(BentoHomeRules.RADIO_STATIONS_SHOWN)
    val newest = GalleryHomeRules.newestPlays(plays).take(2)
    val tones = BentoHomeRules.rotateTones(plates.size)

    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Surface) {
        BentoTileHeader("Radio", actionLabel = "All", onAction = onRadio)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            plates.forEachIndexed { idx, plate ->
                val tone = tones[idx]
                BentoTile(
                    modifier = Modifier.weight(1f),
                    tone = tone,
                    small = true,
                    padding = 8.dp,
                    onClick = { radio.play(plate.station); onRadio() },
                    clickLabel = "Play ${plate.station.name}",
                ) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(p.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(GalleryHomeRules.initials(plate.station.name), style = BentoType.tileName(), color = p.text, maxLines = 1)
                        Cover(radioLogoRequest(repo.absolute(plate.station.logoUrl), 128), Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), rect = true)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(plate.station.name, style = BentoType.small(), color = tone.content(p, bentoTones()), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (plate.heard.isNotEmpty()) {
                        Text(
                            plate.heard,
                            style = BentoType.tileLabel(),
                            color = if (plate.today) p.control else tone.muted(p, bentoTones()),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (newest.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            BentoEyebrow(GalleryHomeRules.heardHeading(newest.first(), now))
            newest.forEachIndexed { i, play ->
                val dl = live.downloadFor(play.id, play.grabbed, play.inLibrary)
                val shown = play.copy(inLibrary = dl == RadioDownload.Owned, grabbed = dl == RadioDownload.Done || dl == RadioDownload.Working)
                val trailing = GalleryHomeRules.trailing(shown)
                val isOwned = trailing == GalleryHomeRules.Trailing.InLibrary
                val gs: GetState? = when (trailing) {
                    GalleryHomeRules.Trailing.InLibrary -> GetState.Idle
                    GalleryHomeRules.Trailing.Requested -> if (dl == RadioDownload.Working) GetState.Pending else GetState.Requested(null)
                    GalleryHomeRules.Trailing.Get -> GetState.Idle
                    GalleryHomeRules.Trailing.None -> null
                }
                BentoRow(
                    title = play.title,
                    subtitle = GalleryHomeRules.playLine(play),
                    showDivider = i < newest.lastIndex,
                    lead = {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp)).background(p.ink), contentAlignment = Alignment.Center) {
                            Icon(BentoIcons.MusicNote, null, tint = p.control, modifier = Modifier.size(20.dp))
                        }
                    },
                    trail = {
                        if (gs != null) BentoGetButton(gs, { radio.download(play.id) }, small = true, owned = isOwned)
                    },
                )
            }
        }
    }
}

// ----- This week insight (span2, sun) --------------------------------------------------------------------------

@Composable
private fun BentoThisWeekTile(card: InsightCard, open: Opener, modifier: Modifier = Modifier) {
    val t = bentoTones()
    var why by remember(card.id) { mutableStateOf(false) }
    val artist = card.subject.artist
    val title = card.subject.title
    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Sun) {
        BentoEyebrow("${lensName(card.kind)} · this week", tone = BentoTone.Sun)
        Spacer(Modifier.height(6.dp))
        Text(card.title, style = BentoType.tileTitle(), color = t.onTone)
        if (card.body.isNotBlank()) {
            Text(card.body, style = BentoType.body(), color = t.onToneMuted, modifier = Modifier.padding(top = 4.dp))
        }
        if (why) {
            Text(InsightNarrative.why(card), style = BentoType.small(), color = t.onTone, modifier = Modifier.padding(top = 8.dp))
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (artist != null && title != null) {
                BentoButton("Play", { open.album(routeKey(artist, title), artist, title, null, card.subject.year) }, small = true, icon = BentoIcons.Play, tone = BentoTone.Sun)
            }
            BentoButton(if (why) "Hide why" else "Why this?", { why = !why }, kind = BentoButtonKind.Outline, small = true, tone = BentoTone.Sun)
        }
    }
}

// ----- Arrivals (span2, white) ---------------------------------------------------------------------------------

@Composable
private fun BentoArrivalsTile(rows: List<ArrivalRow>, open: Opener, onDownloads: () -> Unit, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Surface) {
        BentoTileHeader("Arrivals", actionLabel = "Downloads", onAction = onDownloads)
        Spacer(Modifier.height(4.dp))
        rows.forEachIndexed { idx, row ->
            val job = row.job
            val live = row.stage < 4 && !row.failed
            val pct = job.progress?.percent?.takeIf { live }
            Column {
                BentoRow(
                    title = job.album,
                    subtitle = row.detail,
                    showDivider = !(live || row.failed) && idx < rows.lastIndex,
                    onClick = { open.album(routeKey(job.artist, job.album), job.artist, job.album, null, null) },
                    lead = { AlbumCover(job.artist, job.album, null, Modifier.size(46.dp).clip(RoundedCornerShape(BentoRules.COVER_ROW_RADIUS.dp)), rect = true) },
                    trail = {
                        when {
                            pct != null -> Text("${Math.round(pct)}%", style = BentoType.time(), color = p.control)
                            row.stage >= 4 && !row.failed -> Text("In library", style = BentoType.buttonSmall(), color = p.ok)
                            row.failed -> Text("Failed", style = BentoType.buttonSmall(), color = p.chartRedText)
                        }
                    },
                )
                if (live || row.failed) {
                    BentoTracker(
                        reachedSteps = row.stage.coerceIn(0, 4),
                        failedStep = if (row.failed) row.stage.coerceIn(0, 3) else null,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    if (idx < rows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(p.edge))
                }
            }
        }
    }
}

// ----- Library line (span2, leaf) ------------------------------------------------------------------------------

@Composable
private fun BentoLibraryLineTile(line: String, onStats: () -> Unit, modifier: Modifier = Modifier) {
    val t = bentoTones()
    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Leaf, small = true, onClick = onStats, clickLabel = "Open Library stats") {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(line, style = BentoType.tileName(), color = t.onTone, modifier = Modifier.weight(1f))
            Icon(BentoIcons.Chevron, null, tint = t.onTone, modifier = Modifier.size(20.dp))
        }
    }
}
