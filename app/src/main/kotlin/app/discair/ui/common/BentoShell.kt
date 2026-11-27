package app.discair.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.discair.player.PlayerController
import app.discair.player.PlayerUiState
import app.discair.ui.player.playerSubtitle
import app.discair.ui.theme.BentoMotion
import app.discair.ui.theme.LocalPalette
import app.discair.ui.theme.Palette
import app.discair.ui.theme.rememberReducedMotion
import java.time.LocalDateTime
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Pure rules for Bento's shell (BENTO_THEME.md 7.3 & 9): greeting by day/hour, nav order (Home, Library, Discover, Search),
 * and mini-player progress.
 */
object BentoShellRules {
    /** "Tuesday evening" for a given weekday name and 24-hour clock hour. */
    fun greeting(dayOfWeek: String, hour: Int): String {
        val part = when (hour.coerceIn(0, 23)) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..20 -> "evening"
            else -> "night"
        }
        return "$dayOfWeek $part"
    }

    fun greeting(now: LocalDateTime = LocalDateTime.now()): String =
        greeting(now.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale.ENGLISH), now.hour)

    /** Bento's bar order (decision B7): 0 Home, 1 Library, 2 Discover, 3 Search, -1 other. */
    fun selectedCell(home: Boolean, library: Boolean, discover: Boolean, search: Boolean): Int = when {
        home -> 0
        library -> 1
        discover -> 2
        search -> 3
        else -> -1
    }

    fun progress(positionMs: Long, durationMs: Long): Float =
        if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

// ---------------------------------------------------------------- navigation transitions (#4)

object BentoNav {
    fun enter(reduced: Boolean): EnterTransition =
        if (reduced) EnterTransition.None else fadeIn(BentoMotion.fade(BentoMotion.PAGE_CROSSFADE_MS, reduced))

    fun exit(reduced: Boolean): ExitTransition =
        if (reduced) ExitTransition.None else fadeOut(BentoMotion.fade(BentoMotion.PAGE_CROSSFADE_MS, reduced))
}

// ---------------------------------------------------------------- player flight & floating dock (#6 & #7)

/** Bento's own player flight state (never shared with Gallery or Index). */
val BentoFlight = GalleryPlayerFlight()

/** Motion #7: dock slides down by its own height while Now Playing is up, then rises back on close. */
@Composable
fun Modifier.bentoDockMotion(): Modifier {
    val flight = BentoFlight
    val reduced = rememberReducedMotion()
    LaunchedEffect(flight.playerUp) {
        if (flight.playerUp) {
            flight.dock.animateTo(1f, BentoMotion.tween(BentoMotion.PLAYER_RISE_MS, BentoMotion.Emphasized, reduced))
        } else {
            flight.dock.animateTo(0f, BentoMotion.tween(BentoMotion.PLAYER_FALL_MS, BentoMotion.EmphasizedExit, reduced))
        }
    }
    return graphicsLayer { translationY = flight.dock.value * size.height }
}

/**
 * Bento mini player tile (BENTO_THEME.md 7.3): white `surface` tile, radius 24, padding 8, float shadow,
 * 48dp cover (radius 14), title 15/700 over artist 13, solid 40dp Pause circle, 3dp `accent` progress line along bottom.
 */
@Composable
fun BentoMiniPlayer(
    state: PlayerUiState,
    controller: PlayerController,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.current ?: return
    val p = LocalPalette.current
    val t = bentoTones()
    val measure = Modifier.onGloballyPositioned { c ->
        if (BentoFlight.dock.value == 0f && !BentoFlight.dock.isRunning) BentoFlight.mini = c.boundsInWindow()
    }
    val shape = RoundedCornerShape(BentoRules.MINI_PLAYER_RADIUS.dp)
    val fraction = BentoShellRules.progress(state.positionMs, state.durationMs)
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = BentoRules.NAV_BAR_MARGIN.dp)
            .padding(bottom = BentoRules.NAV_BAR_GAP.dp)
            .bentoPress(interaction)
            .shadow(12.dp, shape, ambientColor = t.shadowColor, spotColor = t.shadowColor)
            .clip(shape)
            .background(p.surface)
            .drawBehind {
                val h = BentoRules.MINI_PLAYER_PROGRESS.dp.toPx()
                drawRect(p.accent, Offset(0f, size.height - h), Size(size.width * fraction, h))
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = "Open now playing", onClick = onOpen)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoCover(
                item.coverUrl.ifBlank { null },
                measure.size(BentoRules.MINI_PLAYER_COVER.dp),
                sizeKind = BentoCoverSize.Tile,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.title,
                    style = BentoType.rowTitle(),
                    color = p.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(
                    playerSubtitle(state),
                    style = BentoType.small(),
                    color = p.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BentoIconButton(
                icon = if (state.isPlaying) BentoIcons.Pause else BentoIcons.Play,
                description = if (state.isPlaying) "Pause" else "Play",
                onClick = controller::playPause,
                solid = true,
                small = true,
            )
        }
    }
}

/**
 * Bento floating pill navigation bar (BENTO_THEME.md 7.3 & 8.2 #6, decision B7):
 * 64dp dark pill (`bar`), radius 32, float shadow, 12dp horizontal margin.
 * Destinations in Bento order: Home, Library, Discover, Search.
 * Active cell expands into a 48dp `control` pill with glyph + label; idle cells are 56x48 `barIcon` glyphs.
 */
@Composable
fun BentoNavBar(
    palette: Palette,
    homeSelected: Boolean,
    discoverSelected: Boolean,
    librarySelected: Boolean,
    onHome: () -> Unit,
    onDiscover: () -> Unit,
    onLibrary: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    searchSelected: Boolean = false,
) {
    val tones = BentoTones.of(palette.id)
    val shape = RoundedCornerShape(BentoRules.NAV_BAR_RADIUS.dp)
    Box(
        modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.32f to palette.ink.copy(alpha = 0.86f),
                    1f to palette.ink,
                ),
            )
            .navigationBarsPadding()
            .padding(start = BentoRules.NAV_BAR_MARGIN.dp, end = BentoRules.NAV_BAR_MARGIN.dp, bottom = 14.dp, top = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(BentoRules.NAV_BAR_HEIGHT.dp)
                .shadow(14.dp, shape, ambientColor = tones.shadowColor, spotColor = tones.shadowColor)
                .clip(shape)
                .background(tones.bar)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BentoNavCell(palette, tones, "Home", BentoIcons.Home, homeSelected, onHome)
            BentoNavCell(palette, tones, "Library", BentoIcons.Lib, librarySelected, onLibrary)
            BentoNavCell(palette, tones, "Discover", BentoIcons.Disc, discoverSelected, onDiscover)
            BentoNavCell(palette, tones, "Search", BentoIcons.Search, searchSelected, onSearch)
        }
    }
}

@Composable
private fun BentoNavCell(
    palette: Palette,
    tones: BentoTones,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val reduced = rememberReducedMotion()
    val pillFill by animateColorAsState(
        if (selected) palette.control else Color.Transparent,
        BentoMotion.fade(BentoMotion.NAV_PILL_COLOR_MS, reduced),
        label = "bentoNavFill",
    )
    val iconColor by animateColorAsState(
        if (selected) palette.onControl else tones.barIcon,
        BentoMotion.fade(BentoMotion.NAV_PILL_COLOR_MS, reduced),
        label = "bentoNavIcon",
    )
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .bentoPress(interaction)
            .height(48.dp)
            .widthIn(min = 56.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(pillFill)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .animateContentSize(BentoMotion.spring(BentoMotion.NAV_PILL_DAMPING, BentoMotion.NAV_PILL_STIFFNESS, reduced))
            .padding(horizontal = if (selected) 18.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(BentoRules.ICON.dp))
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(BentoMotion.fade(BentoMotion.NAV_LABEL_FADE_MS, reduced, BentoMotion.NAV_LABEL_DELAY_MS)) +
                expandHorizontally(BentoMotion.spring(BentoMotion.NAV_PILL_DAMPING, BentoMotion.NAV_PILL_STIFFNESS, reduced)),
            exit = fadeOut(BentoMotion.fade(BentoMotion.NAV_LABEL_EXIT_MS, reduced)) +
                shrinkHorizontally(BentoMotion.spring(BentoMotion.NAV_PILL_DAMPING, BentoMotion.NAV_PILL_STIFFNESS, reduced)),
        ) {
            Row {
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = BentoType.navLabel(),
                    color = palette.onControl,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- top bars

/**
 * Bento Home / top-level header (BENTO_THEME.md 7.3): Greeting over Large title (34/36), 22dp top padding;
 * right: 44dp `surface` circle Downloads (with 18dp `accent` badge) and Settings icon buttons.
 */
@Composable
fun BentoHomeHeader(
    title: String,
    active: Int,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
    greeting: String? = BentoShellRules.greeting(),
    showDiscover: Boolean = false,
    onDiscover: () -> Unit = {},
) {
    val p = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .background(p.ink)
            .statusBarsPadding()
            .padding(start = BentoRules.SIDE_MARGIN.dp, end = BentoRules.SIDE_MARGIN.dp, top = 22.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (!greeting.isNullOrBlank()) {
                Text(greeting, style = BentoType.greeting(), color = p.textMuted, maxLines = 1)
            }
            Text(
                title,
                style = BentoType.largeTitle(),
                color = p.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        if (showDiscover) {
            BentoIconButton(BentoIcons.Disc, "Discover", onDiscover, inHeader = true)
        }
        Box {
            BentoIconButton(
                BentoIcons.Download,
                if (active == 0) "Downloads" else "Downloads, $active active",
                onDownloads,
                inHeader = true,
            )
            if (active > 0) BentoBadge(active, Modifier.align(Alignment.TopEnd))
        }
        BentoIconButton(BentoIcons.Gear, "Settings", onSettings, inHeader = true)
    }
}

/**
 * Bento stack page top bar: 44dp `surface` circle Back button left, optional 13sp page name centered, optional right action.
 */
@Composable
fun BentoPageBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .background(p.ink)
            .statusBarsPadding()
            .padding(horizontal = BentoRules.SIDE_MARGIN.dp, vertical = 10.dp),
    ) {
        BentoIconButton(BentoIcons.Back, "Back", onBack, inHeader = true, modifier = Modifier.align(Alignment.CenterStart))
        if (!title.isNullOrBlank()) {
            Text(
                title,
                style = BentoType.small().copy(fontWeight = FontWeight.SemiBold),
                color = p.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 56.dp),
            )
        }
        if (action != null) {
            Box(Modifier.align(Alignment.CenterEnd)) { action() }
        }
    }
}

// ---------------------------------------------------------------- snackbar (#9) & sheet handle

/**
 * Motion #9: 52dp pill in `deep` with `onDeep` text and `deepEyebrow` 800 Undo; springs in from 24dp below (0.75/500), holds 4s.
 */
@Composable
fun BentoSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val reduced = rememberReducedMotion()
    val current = hostState.currentSnackbarData
    val a11y = LocalAccessibilityManager.current
    LaunchedEffect(current) {
        if (current == null) return@LaunchedEffect
        val base = when (current.visuals.duration) {
            SnackbarDuration.Short -> BentoMotion.SNACK_HOLD_MS
            SnackbarDuration.Long -> 10_000L
            SnackbarDuration.Indefinite -> Long.MAX_VALUE
        }
        val hold = a11y?.calculateRecommendedTimeoutMillis(
            base, containsIcons = false, containsText = true, containsControls = current.visuals.actionLabel != null,
        ) ?: base
        delay(hold)
        current.dismiss()
    }
    val last = remember { arrayOfNulls<SnackbarData>(1) }
    if (current != null) last[0] = current
    val off = with(LocalDensity.current) { BentoMotion.SNACK_FROM_DP.dp.roundToPx() }
    Box(modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = current != null,
            enter = if (reduced) EnterTransition.None else
                slideInVertically(BentoMotion.spring(BentoMotion.SNACK_IN_DAMPING, BentoMotion.SNACK_IN_STIFFNESS, reduced)) { off } +
                    fadeIn(BentoMotion.fade(BentoMotion.SNACK_FADE_MS, reduced)),
            exit = if (reduced) ExitTransition.None else
                fadeOut(BentoMotion.fade(BentoMotion.SNACK_FADE_MS, reduced)) +
                    slideOutVertically(BentoMotion.tween(BentoMotion.SNACK_FADE_MS, BentoMotion.Exit, reduced)) { off },
        ) {
            last[0]?.let { BentoSnackbar(it) }
        }
    }
}

@Composable
fun BentoSnackbar(data: SnackbarData) {
    val t = bentoTones()
    val shape = RoundedCornerShape(26.dp)
    Box(Modifier.padding(horizontal = BentoRules.NAV_BAR_MARGIN.dp, vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = BentoRules.SNACK_HEIGHT.dp)
                .shadow(10.dp, shape, ambientColor = t.shadowColor, spotColor = t.shadowColor)
                .clip(shape)
                .background(t.deep)
                .padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                data.visuals.message,
                Modifier.weight(1f).padding(vertical = 10.dp),
                color = t.onDeep,
                style = BentoType.body().copy(fontWeight = FontWeight.Medium),
            )
            data.visuals.actionLabel?.let { label ->
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .clickable(role = Role.Button, onClick = { data.performAction() })
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = t.deepEyebrow,
                        style = BentoType.button().copy(fontWeight = FontWeight.ExtraBold),
                    )
                }
            }
        }
    }
}

/** Bento sheet drag handle: 40x5 rounded pill in `edgeStrong` at 50%, 10dp top padding. */
@Composable
fun BentoSheetHandle() {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 12.dp)
            .size(width = 40.dp, height = 5.dp)
            .clip(RoundedCornerShape(2.5.dp))
            .background(LocalPalette.current.edgeStrong.copy(alpha = 0.5f)),
    )
}
