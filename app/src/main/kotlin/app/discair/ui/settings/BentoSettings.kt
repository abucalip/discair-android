package app.discair.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.discair.AppContainer
import app.discair.AppViewModel
import app.discair.Check
import app.discair.UiState
import app.discair.model.ServerReads
import app.discair.ui.LocalBottomClearance
import app.discair.ui.common.BentoBanner
import app.discair.ui.common.BentoButton
import app.discair.ui.common.BentoButtonKind
import app.discair.ui.common.BentoEyebrow
import app.discair.ui.common.BentoIcons
import app.discair.ui.common.BentoPageBar
import app.discair.ui.common.BentoRules
import app.discair.ui.common.BentoSkeleton
import app.discair.ui.common.BentoTabs
import app.discair.ui.common.BentoTag
import app.discair.ui.common.BentoTile
import app.discair.ui.common.BentoTone
import app.discair.ui.common.BentoTones
import app.discair.ui.common.BentoType
import app.discair.ui.common.Load
import app.discair.ui.common.bentoPress
import app.discair.ui.common.bentoTileEnter
import app.discair.ui.common.bentoTones
import app.discair.ui.downloads.SettingsRules
import app.discair.ui.theme.Appearance
import app.discair.ui.theme.AppearanceRules
import app.discair.ui.theme.FontPairs
import app.discair.ui.theme.Layout
import app.discair.ui.theme.LocalPalette
import app.discair.ui.theme.Palette
import app.discair.ui.theme.Palettes
import app.discair.ui.theme.THEME_SYSTEM
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/*
 * Bento's Settings, Server, and Appearance pages (B6).
 * Spec: BENTO_THEME.md section 10 (`settings` and `downloads-stats-server`).
 * Every row, switch, menu, and choice is shared with the Standard screens.
 */

@Composable
internal fun BentoSettingsRow(
    icon: ImageVector,
    title: String,
    state: String,
    modifier: Modifier,
    stateColor: Color,
    onClick: (() -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    divider: Boolean,
) {
    val p = LocalPalette.current
    val tone = if (stateColor == MaterialTheme.colorScheme.onSurfaceVariant) p.textMuted else stateColor
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth()) {
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).background(p.edge))
        Row(
            Modifier.fillMaxWidth()
                .heightIn(min = BentoRules.ROW_MIN.dp)
                .then(if (onClick != null) Modifier.bentoPress(interaction).clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick) else Modifier)
                .semantics(mergeDescendants = true) { contentDescription = if (state.isEmpty()) title else "$title, $state" }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(p.ink),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = p.control, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = BentoType.rowTitle(), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (state.isNotEmpty()) {
                    Text(state, style = BentoType.small(), color = tone, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            if (trailing != null) trailing()
            else if (onClick != null) Icon(BentoIcons.Chevron, null, tint = p.textMuted, modifier = Modifier.size(BentoRules.ICON_INLINE.dp))
        }
    }
}

@Composable
private fun BentoSettingsGroup(
    title: String,
    index: Int,
    tone: BentoTone = BentoTone.Surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = BentoRules.SIDE_MARGIN.dp).bentoTileEnter(index, "settings"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BentoEyebrow(title, Modifier.padding(start = 8.dp))
        BentoTile(Modifier.fillMaxWidth(), tone = tone, content = content)
    }
}

@Composable
private fun BentoScrollPage(
    title: String,
    onBack: () -> Unit,
    barTitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        BentoPageBar(onBack = onBack, title = barTitle, action = action)
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp + LocalBottomClearance.current),
            verticalArrangement = Arrangement.spacedBy(BentoRules.GRID_GAP.dp),
        ) {
            Text(
                title,
                style = BentoType.pageTitle(),
                color = p.text,
                modifier = Modifier.padding(horizontal = BentoRules.SIDE_MARGIN.dp, vertical = 4.dp).semantics { heading() },
            )
            content()
        }
    }
}

// ----- Settings ---------------------------------------------------------------------------------------------------

@Composable
internal fun BentoSettingsPage(
    state: UiState,
    model: AppViewModel,
    container: AppContainer,
    onBack: () -> Unit,
    onEqualizer: () -> Unit,
    onCrossfade: () -> Unit,
    onAppearance: () -> Unit,
    onServer: () -> Unit,
    onSignOut: () -> Unit,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val version = remember { try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { null } }
    BentoScrollPage("Settings", onBack, barTitle = IndexSettingsRules.versionLine(version)) {
        if (state.librarian is Check.Failed || state.navidrome is Check.Failed) {
            Box(Modifier.padding(horizontal = BentoRules.SIDE_MARGIN.dp)) {
                BentoBanner(
                    title = "Server connection issue",
                    subtitle = listOfNotNull(
                        (state.librarian as? Check.Failed)?.failure?.message?.let { "Librarian: $it" },
                        (state.navidrome as? Check.Failed)?.failure?.message?.let { "Navidrome: $it" },
                    ).joinToString(" · "),
                    onRetry = { model.recheck() },
                )
            }
        }

        BentoSettingsGroup("Sound and look", index = 0) {
            AppearanceRow(container.appearance, onAppearance, divider = false)
            app.discair.ui.PlaybackSettings(container, onEqualizer, onCrossfade, app.discair.ui.PlaybackShow.LookAndSound)
        }

        BentoSettingsGroup("Library", index = 1) {
            val serverLine = SettingsRules.serverLine(state.librarian, state.navidrome)
            SettingsRow(
                BentoIcons.Server, "Server", serverLine.text, onClick = onServer, divider = false,
                stateColor = when (serverLine.tone) {
                    SettingsRules.Tone.Success -> p.ok
                    SettingsRules.Tone.Danger -> p.chartRedText
                    else -> p.textMuted
                },
            )
            app.discair.ui.LibrarySettings()
        }

        BentoSettingsGroup("Listening", index = 2) {
            RadioSettings(divider = false)
            ListenBrainzSettings()
            BlocklistSettings()
        }

        BentoSettingsGroup("Playback", index = 3) {
            app.discair.ui.PlaybackSettings(container, onEqualizer, onCrossfade, app.discair.ui.PlaybackShow.Rest)
        }

        BentoSettingsGroup("Account", index = 4) {
            SettingsRow(BentoIcons.Refresh, "Check connection", "Ask both servers again", onClick = { model.recheck() }, divider = false)
            SettingsRow(BentoIcons.Server, "Edit servers", "Addresses and sign-in", onClick = { model.editServers() })
            Box(Modifier.fillMaxWidth().height(1.dp).background(p.edge))
            val signOutInteraction = remember { MutableInteractionSource() }
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = BentoRules.ROW_MIN.dp)
                    .bentoPress(signOutInteraction)
                    .clickable(interactionSource = signOutInteraction, indication = null, role = Role.Button, onClickLabel = "Sign out", onClick = onSignOut)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(p.dangerDim),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(BentoIcons.SignOut, null, tint = p.chartRedText, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Sign out", style = BentoType.rowTitle(), color = p.chartRedText)
                    Text("Removes both servers from this phone", style = BentoType.small(), color = p.textMuted)
                }
            }
        }
    }
}

// ----- Server -----------------------------------------------------------------------------------------------------

@Composable
internal fun BentoServer(state: Load<ServerReads>, reload: () -> Unit, onBack: () -> Unit) {
    val p = LocalPalette.current
    val t = bentoTones()
    val clockLabel = (state as? Load.Ready)?.let { ready ->
        val zone = remember { ZoneId.systemDefault() }
        val clock = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(ready.value.checkedAtMillis).atZone(zone))
        IndexSettingsRules.checkedLine(clock)
    }
    BentoScrollPage("Server", onBack, barTitle = clockLabel) {
        when (state) {
            Load.Loading -> Column(
                Modifier.padding(horizontal = BentoRules.SIDE_MARGIN.dp),
                verticalArrangement = Arrangement.spacedBy(BentoRules.GRID_GAP.dp),
            ) {
                BentoSkeleton(Modifier.fillMaxWidth().height(92.dp))
                BentoSkeleton(Modifier.fillMaxWidth().height(180.dp))
            }
            is Load.Failed -> Box(Modifier.padding(horizontal = BentoRules.SIDE_MARGIN.dp)) {
                BentoBanner("Could not check services", state.failure.message, onRetry = reload)
            }
            is Load.Ready -> {
                val zone = remember { ZoneId.systemDefault() }
                val clock = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(state.value.checkedAtMillis).atZone(zone))
                val view = remember(state.value) { ServerRules.build(state.value, clock) }
                val summaryTone = when (view.summary.tone) {
                    ServerTone.Success -> BentoTone.Leaf
                    ServerTone.Warning -> BentoTone.Sun
                    ServerTone.Danger -> BentoTone.Warm
                    else -> BentoTone.Surface
                }

                BentoTile(
                    Modifier.fillMaxWidth().padding(horizontal = BentoRules.SIDE_MARGIN.dp).bentoTileEnter(0, "server")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    tone = summaryTone,
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(view.summary.title, style = BentoType.tileTitle(), color = summaryTone.content(p, t))
                            if (view.summary.detail.isNotBlank()) {
                                Text(view.summary.detail, style = BentoType.small(), color = summaryTone.muted(p, t))
                            }
                        }
                        BentoButton("Check now", reload, kind = BentoButtonKind.Outline, small = true, tone = summaryTone)
                    }
                }

                BentoSettingsGroup("Services", index = 1) {
                    view.services.forEachIndexed { i, row ->
                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(p.edge))
                        val meta = IndexSettingsRules.serviceMeta(row.id, row.role, state.value.librarianVersion, state.value.navidromeLatencyMs)
                        Row(
                            Modifier.fillMaxWidth()
                                .heightIn(min = BentoRules.ROW_MIN.dp)
                                .padding(vertical = 8.dp)
                                .semantics(mergeDescendants = true) { contentDescription = "${row.name}, ${row.status.word}. $meta" },
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(row.name, style = BentoType.rowTitle(), color = p.text)
                                if (meta.isNotBlank()) Text(meta, style = BentoType.small(), color = p.textMuted)
                            }
                            val statusTone = when (row.status.tone) {
                                ServerTone.Success -> BentoTone.Leaf
                                ServerTone.Warning -> BentoTone.Sun
                                ServerTone.Danger -> BentoTone.Warm
                                else -> BentoTone.Sky
                            }
                            BentoTag(row.status.word, tone = statusTone)
                        }
                    }
                }

                if (view.activity.isNotEmpty()) {
                    BentoSettingsGroup("Activity", index = 2) {
                        view.activity.forEachIndexed { i, row ->
                            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(p.edge))
                            val calm = row.status.tone == ServerTone.Success || row.status.tone == ServerTone.Neutral
                            Row(
                                Modifier.fillMaxWidth()
                                    .heightIn(min = BentoRules.ROW_MIN.dp)
                                    .padding(vertical = 8.dp)
                                    .semantics(mergeDescendants = true) { contentDescription = "${row.name}, ${row.status.word}. ${row.line}" },
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(row.name, style = BentoType.rowTitle(), color = p.text)
                                    Text(row.line, style = BentoType.small(), color = p.textMuted)
                                }
                                if (!calm) {
                                    BentoTag(
                                        row.status.word,
                                        tone = if (row.status.tone == ServerTone.Danger) BentoTone.Warm else BentoTone.Sun,
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

// ----- Appearance -------------------------------------------------------------------------------------------------

@Composable
internal fun BentoAppearance(appearance: Appearance, onBack: () -> Unit) {
    val p = LocalPalette.current
    val following = appearance.themeChoice == THEME_SYSTEM
    val systemDark = isSystemInDarkTheme()
    var filter by rememberSaveable { mutableStateOf(ThemeGroups.of(Palettes.byId(if (following) appearance.dayTheme else appearance.themeChoice))) }
    val chips = ThemeGroups.chips(Palettes.All, null)
    val group = if (filter in chips) filter else chips.firstOrNull() ?: ThemeGroups.DEFAULT

    BentoScrollPage("Appearance", onBack, barTitle = group) {
        Column(
            Modifier.padding(horizontal = BentoRules.SIDE_MARGIN.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BentoTabs(
                labels = listOf("Follow the phone", "One theme"),
                selectedIndex = if (following) 0 else 1,
                onSelect = { appearance.chooseTheme(if (it == 0) THEME_SYSTEM else appearance.palette(systemDark).id) },
            )
            Text(
                if (following) "Day theme when the phone is light, night theme when it is dark."
                else "One fixed theme, whatever the phone's setting is.",
                style = BentoType.small(),
                color = p.textMuted,
            )
            if (chips.size > 1) {
                BentoEyebrow("Layout", Modifier.padding(top = 4.dp, start = 4.dp))
                BentoTabs(
                    labels = chips,
                    selectedIndex = chips.indexOf(group).coerceAtLeast(0),
                    onSelect = { filter = chips[it] },
                )
            }
        }

        IndexSettingsRules.slots(following).forEachIndexed { slotIdx, (name, light) ->
            val shown = ThemeGroups.shown(Palettes.All, group, light)
            if (shown.isEmpty()) return@forEachIndexed
            val selectedId = when (light) {
                true -> appearance.dayTheme
                false -> appearance.nightTheme
                null -> appearance.themeChoice
            }
            Column(
                Modifier.fillMaxWidth().padding(horizontal = BentoRules.SIDE_MARGIN.dp).bentoTileEnter(slotIdx, "appearance"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BentoEyebrow(name, Modifier.padding(start = 8.dp, top = 4.dp))
                shown.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BentoRules.GRID_GAP.dp)) {
                        row.forEach { theme ->
                            BentoPaletteSwatch(theme, theme.id == selectedId, Modifier.weight(1f)) {
                                when (light) {
                                    true -> appearance.chooseDay(theme.id)
                                    false -> appearance.chooseNight(theme.id)
                                    null -> appearance.chooseTheme(theme.id)
                                }
                            }
                        }
                        repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        BentoSettingsGroup("Type", index = 3) {
            FontPairs.All.forEachIndexed { i, pair ->
                val on = pair.id == appearance.fontPairId
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(p.edge))
                Row(
                    Modifier.fillMaxWidth()
                        .heightIn(min = BentoRules.ROW_MIN.dp)
                        .selectable(selected = on, role = Role.RadioButton) { appearance.chooseFont(pair.id) }
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${pair.label}, ${pair.titleName} and ${pair.bodyName}${if (on) ", selected" else ""}"
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            pair.label,
                            color = p.text,
                            fontFamily = pair.title,
                            fontWeight = pair.titleWeight,
                            fontSize = 16.sp,
                        )
                        Text("${pair.titleName}, ${pair.bodyName}, ${pair.monoName}", style = BentoType.small(), color = p.textMuted)
                    }
                    if (on) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(p.control),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(BentoIcons.Check, null, tint = p.onControl, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Palettes.byId(if (following) appearance.dayTheme else appearance.themeChoice)?.let { c ->
            AppearanceRules.suggestedFont(c, appearance.fontPairId)?.let { f ->
                Text(
                    "${c.label} is drawn with ${f.label} (${f.titleName} and ${f.bodyName}). Your font is unchanged.",
                    style = BentoType.small(),
                    color = p.textMuted,
                    modifier = Modifier.padding(horizontal = BentoRules.SIDE_MARGIN.dp + 4.dp),
                )
            }
        }
    }
}

@Composable
private fun BentoPaletteSwatch(
    theme: Palette,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(BentoRules.TILE_SMALL_RADIUS.dp)
    val tones = if (theme.layout == Layout.Bento) BentoTones.of(theme.id) else null
    Column(
        modifier
            .bentoPress(interaction)
            .clip(shape)
            .background(p.surface)
            .then(if (selected) Modifier.border(2.5.dp, p.control, shape) else Modifier)
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${theme.label}, ${if (theme.light) "light" else "dark"}${if (selected) ", selected" else ""}"
            }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val previewShape = RoundedCornerShape(14.dp)
        Column(
            Modifier.fillMaxWidth().clip(previewShape).background(theme.ink).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val dots = if (tones != null) listOf(tones.warm, tones.sun, tones.leaf, tones.bloom) else listOf(theme.surface, theme.control, theme.accent)
                    dots.forEach { dot ->
                        Box(Modifier.size(14.dp).clip(CircleShape).background(dot))
                    }
                }
                Text("Aa", color = theme.text, style = BentoType.tileName())
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(theme.surface))
                Box(Modifier.weight(0.55f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(theme.control))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(theme.label, style = BentoType.rowTitle(), color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (theme.light) "Day" else "Night", style = BentoType.tileLabel(), color = p.textMuted)
            }
            if (selected) {
                Box(Modifier.size(22.dp).clip(CircleShape).background(p.control), contentAlignment = Alignment.Center) {
                    Icon(BentoIcons.Check, null, tint = p.onControl, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
