package app.discair.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.model.GenrePage
import app.discair.ui.common.AlbumCover
import app.discair.ui.common.ArtistPhoto
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPageBar
import app.discair.ui.common.BentoRow
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTileHeader
import app.discair.ui.common.BentoTileLabel
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoType
import app.discair.ui.common.LoadView
import app.discair.ui.common.Loaded
import app.discair.ui.common.bentoTileEnter
import app.discair.ui.common.bentoTones
import app.discair.ui.common.plural
import app.discair.ui.common.sharedCover
import app.discair.ui.theme.LocalPalette

private val Side = BentoRules.SIDE_MARGIN.dp
private val Gap = BentoRules.GRID_GAP.dp

/**
 * Bento's Genre page (Layout.Bento). Spec: BENTO_THEME.md step B3 (album-artist-genre, frame 4).
 * Sky hero tile with genre name and counts, Artists tile with round photos, and 2-column Albums tile.
 */
@Composable
internal fun BentoGenre(route: GenreRoute, loaded: Loaded<GenrePage>, open: Opener) {
    val p = LocalPalette.current
    val t = bentoTones()
    val pageKey = "bento-genre:${route.tag}"
    var allArtists by rememberSaveable(route.tag) { mutableStateOf(false) }
    var allAlbums by rememberSaveable(route.tag) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(p.ink)) {
        BentoPageBar(onBack = open.back, title = "Genre")
        LoadView(loaded) { page ->
            val artists = page.sections.artists.items
            val albums = page.sections.albums.items
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp + LocalBottomClearance.current),
                verticalArrangement = Arrangement.spacedBy(Gap),
            ) {
                item(key = "head") {
                    BentoTile(
                        Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(0, pageKey),
                        tone = BentoTone.Sky,
                    ) {
                        BentoTileLabel("Genre", tone = BentoTone.Sky)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            route.name,
                            style = BentoType.pageTitle(),
                            color = t.onTone,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${plural(artists.size, "artist")} · ${plural(albums.size, "album")}",
                            style = BentoType.small(),
                            color = t.onToneMuted,
                        )
                    }
                }

                if (artists.isEmpty() && albums.isEmpty()) {
                    item(key = "empty") {
                        BentoTile(Modifier.padding(horizontal = Side).fillMaxWidth()) {
                            Text("Nothing found for this genre.", style = BentoType.body(), color = p.textMuted)
                        }
                    }
                }

                if (artists.isNotEmpty()) {
                    item(key = "artists") {
                        val shown = if (allArtists) artists else artists.take(5)
                        BentoTile(
                            Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(1, pageKey),
                            tone = BentoTone.Surface,
                        ) {
                            BentoTileHeader(
                                "Artists",
                                actionLabel = if (!allArtists && artists.size > 5) "See all (${artists.size})" else null,
                                onAction = { allArtists = true },
                            )
                            Spacer(Modifier.height(6.dp))
                            shown.forEachIndexed { i, a ->
                                BentoRow(
                                    title = a.name,
                                    subtitle = if (a.owned) "In your library" else "Not in your library",
                                    showDivider = i < shown.lastIndex,
                                    onClick = { open.artist(a.name, a.key, a.id, null) },
                                    lead = { ArtistPhoto(a.name, null, Modifier.size(44.dp), circle = true) },
                                    trail = { Icon(BentoIcons.Chevron, null, tint = p.textMuted, modifier = Modifier.size(18.dp)) },
                                )
                            }
                        }
                    }
                }

                if (albums.isNotEmpty()) {
                    item(key = "albums") {
                        val shown = if (allAlbums) albums else albums.take(12)
                        BentoTile(
                            Modifier.padding(horizontal = Side).fillMaxWidth().bentoTileEnter(2, pageKey),
                            tone = BentoTone.Surface,
                        ) {
                            BentoTileHeader(
                                "Albums",
                                actionLabel = if (!allAlbums && albums.size > 12) "See all (${albums.size})" else null,
                                onAction = { allAlbums = true },
                            )
                            Spacer(Modifier.height(10.dp))
                            shown.chunked(2).forEachIndexed { rIdx, pair ->
                                if (rIdx > 0) Spacer(Modifier.height(Gap))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Gap)) {
                                    pair.forEach { a ->
                                        val said = listOfNotNull(a.name, a.artist.ifBlank { null }, a.year?.toString(), if (a.owned) "in your library" else null).joinToString(", ")
                                        Column(
                                            Modifier.weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable(role = Role.Button) { open.album(a.key, a.artist, a.name, a.id, a.year) }
                                                .semantics(mergeDescendants = true) { contentDescription = said },
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            AlbumCover(
                                                a.artist, a.name, null,
                                                Modifier.fillMaxWidth().aspectRatio(1f)
                                                    .clip(RoundedCornerShape(BentoRules.COVER_TILE_RADIUS.dp))
                                                    .sharedCover(a.key),
                                                rect = true,
                                            )
                                            Text(a.name, style = BentoType.rowTitle(), color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                listOfNotNull(a.artist.ifBlank { null }, a.year?.toString()).joinToString(" · "),
                                                style = BentoType.small(), color = p.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            )
                                            if (a.owned) BentoTag("In library", tone = BentoTone.Leaf)
                                        }
                                    }
                                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
