package app.discair.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import app.discair.model.GetState
import app.discair.ui.theme.BentoMotion
import app.discair.ui.theme.Layout
import app.discair.ui.theme.LocalFontPair
import app.discair.ui.theme.LocalPalette
import app.discair.ui.theme.LocalThemePalette
import app.discair.ui.theme.Palette
import app.discair.ui.theme.rememberReducedMotion
import kotlinx.coroutines.delay

/**
 * Bento's measurements (BENTO_THEME.md sections 3 and 7; 1 CSS px = 1dp). Pure, so tests can pin them.
 */
object BentoRules {
    const val SIDE_MARGIN = 14f
    const val GRID_GAP = 10f
    const val TILE_LARGE_RADIUS = 28f
    const val TILE_LARGE_PADDING = 14f
    const val TILE_SMALL_RADIUS = 22f
    const val TILE_SMALL_PADDING = 12f
    const val BUTTON_HEIGHT = 44f
    const val BUTTON_SMALL_HEIGHT = 36f
    const val BUTTON_RADIUS = 22f
    const val CHIP_HEIGHT = 40f
    const val CHIP_SMALL_HEIGHT = 32f
    const val CHIP_RADIUS = 20f
    const val ICON_BUTTON = 44f
    const val ICON_BUTTON_SMALL = 40f
    const val ICON = 22f
    const val ICON_INLINE = 18f
    const val ICON_TRANSPORT = 28f
    const val ICON_PLAY_CIRCLE = 30f
    const val PLAY_CIRCLE = 76f
    const val COVER_LARGE_RADIUS = 18f
    const val COVER_TILE_RADIUS = 14f
    const val COVER_ROW_RADIUS = 12f
    const val COVER_SMALL_RADIUS = 10f
    const val ARTIST_TILE_RADIUS = 20f
    const val ROW_MIN = 60f
    const val TOUCH_MIN = 48f
    const val SEARCH_HEIGHT = 48f
    const val SWITCH_WIDTH = 52f
    const val SWITCH_HEIGHT = 32f
    const val SEEK_HEIGHT = 6f
    const val MINI_PLAYER_RADIUS = 24f
    const val MINI_PLAYER_COVER = 48f
    const val MINI_PLAYER_PAUSE = 40f
    const val MINI_PLAYER_PROGRESS = 3f
    const val NAV_BAR_HEIGHT = 64f
    const val NAV_BAR_RADIUS = 32f
    const val NAV_BAR_MARGIN = 12f
    const val NAV_BAR_GAP = 10f
    const val BADGE_HEIGHT = 18f
    const val SHEET_TOP_RADIUS = 28f
    const val SNACK_HEIGHT = 52f

    /** The nav bar's destinations in Bento order (decision B7). */
    val cells: List<String> = BentoMotion.barOrder

    /** The scrim behind a sheet: rgba(10, 10, 10, .45). */
    val SCRIM = Color(10, 10, 10).copy(alpha = 0.45f)
}

/**
 * Bento's five decorative tile tones plus dock/deep/chip colours (BENTO_THEME.md 4.1 & 4.2, decision B1).
 * Keyed by palette id so [Palette] gains no new fields.
 */
class BentoTones(
    val warm: Color,
    val sun: Color,
    val leaf: Color,
    val bloom: Color,
    val sky: Color,
    val onTone: Color,
    val onToneMuted: Color,
    val bar: Color,
    val barIcon: Color,
    val deep: Color,
    val onDeep: Color,
    val deepEyebrow: Color,
    val chip: Color,
    val shadowColor: Color,
) {
    val rotation: List<Color> get() = listOf(warm, sun, leaf, bloom, sky)

    companion object {
        private fun c(hex: Long) = Color(0xFF000000 or hex)

        val Sorbet = BentoTones(
            warm = c(0xF7D0BD), sun = c(0xF3E3A1), leaf = c(0xCDE7D6), bloom = c(0xDCD3F2), sky = c(0xCBDDF2),
            onTone = c(0x1D1B18), onToneMuted = c(0x4A453E),
            bar = c(0x1D1B18), barIcon = c(0xE9E3D8),
            deep = c(0x1D1B18), onDeep = c(0xF4F0E8), deepEyebrow = c(0xF3B8A3),
            chip = c(0xFFFFFF), shadowColor = Color(29, 27, 24),
        )

        val Licorice = BentoTones(
            warm = c(0x3A241B), sun = c(0x36301A), leaf = c(0x1F3328), bloom = c(0x2A2540), sky = c(0x1F2E2E),
            onTone = c(0xF3EEE6), onToneMuted = c(0xD3C9BC),
            bar = c(0x26231F), barIcon = c(0xD8D1C5),
            deep = c(0x26231F), onDeep = c(0xF3EEE6), deepEyebrow = c(0xFFB59C),
            chip = c(0x121110), shadowColor = Color(0, 0, 0),
        )

        val Riviera = BentoTones(
            warm = c(0xFFD2CA), sun = c(0xF2EC9B), leaf = c(0xBFE3DC), bloom = c(0xD3DCFA), sky = c(0xCFEAF2),
            onTone = c(0x13201E), onToneMuted = c(0x36433F),
            bar = c(0x13201E), barIcon = c(0xDDE6E3),
            deep = c(0x13201E), onDeep = c(0xEFF3F1), deepEyebrow = c(0xF2EC9B),
            chip = c(0xFFFFFF), shadowColor = Color(19, 32, 30),
        )

        val byId: Map<String, BentoTones> = mapOf(
            "bento" to Sorbet,
            "bento_licorice" to Licorice,
            "bento_riviera" to Riviera,
        )

        fun of(id: String?): BentoTones = byId[id] ?: Sorbet
    }
}

/** Named tile surface/tone roles. The five pastel tones rotate in order; never repeat two identical tones side by side. */
enum class BentoTone {
    Warm, Sun, Leaf, Bloom, Sky, Surface, Deep;

    fun fill(p: Palette, t: BentoTones): Color = when (this) {
        Warm -> t.warm
        Sun -> t.sun
        Leaf -> t.leaf
        Bloom -> t.bloom
        Sky -> t.sky
        Surface -> p.surface
        Deep -> t.deep
    }

    fun content(p: Palette, t: BentoTones): Color = when (this) {
        Surface -> p.text
        Deep -> t.onDeep
        else -> t.onTone
    }

    fun muted(p: Palette, t: BentoTones): Color = when (this) {
        Surface -> p.textMuted
        Deep -> t.onDeep.copy(alpha = 0.78f)
        else -> t.onToneMuted
    }

    fun eyebrow(p: Palette, t: BentoTones): Color = when (this) {
        Deep -> t.deepEyebrow
        Surface -> p.control
        else -> t.onToneMuted
    }

    fun chipFill(p: Palette, t: BentoTones): Color = when (this) {
        Surface -> p.ink
        Deep -> t.onDeep.copy(alpha = 0.14f)
        else -> t.chip
    }

    fun divider(p: Palette): Color = when (this) {
        Surface -> p.edge
        Deep -> Color.White.copy(alpha = 0.14f)
        else -> Color.Black.copy(alpha = 0.08f)
    }

    companion object {
        val Rotation: List<BentoTone> = listOf(Warm, Sun, Leaf, Bloom, Sky)
        fun rotate(index: Int): BentoTone = Rotation[Math.floorMod(index, Rotation.size)]
    }
}

/** True while the owner's theme is Bento. Every Bento branch in shared controls is guarded by this. */
@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun isBento(): Boolean = LocalPalette.current.layout == Layout.Bento

/** The Bento tones for the owner's chosen palette. */
@Composable
fun bentoTones(): BentoTones = BentoTones.of((LocalThemePalette.current ?: LocalPalette.current).id)

/**
 * Bento's 17 text styles (BENTO_THEME.md 5.3). Reads the faces through [LocalFontPair] so another font pair
 * chosen in Appearance keeps Bento's layout (D4).
 */
object BentoType {
    @Composable
    private fun title(weight: Int, size: Float, line: Float, tracking: Float = 0f, features: String? = null): TextStyle {
        val f = LocalFontPair.current
        return TextStyle(
            fontFamily = f.title, fontWeight = FontWeight(weight),
            fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em,
            fontFeatureSettings = features,
        )
    }

    @Composable
    private fun body(weight: Int, size: Float, line: Float, tracking: Float = 0f, features: String? = null): TextStyle {
        val f = LocalFontPair.current
        return TextStyle(
            fontFamily = f.body, fontWeight = FontWeight(weight),
            fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.em,
            fontFeatureSettings = features,
        )
    }

    @Composable fun greeting() = body(600, 13f, 16f)
    @Composable fun largeTitle() = title(800, 34f, 36f, -0.02f)
    @Composable fun pageTitle() = title(800, 36f, 37f, -0.02f)
    @Composable fun heroTitle() = title(800, 30f, 31f, -0.01f)
    @Composable fun tileTitle() = title(800, 20f, 21f)
    @Composable fun tileName() = title(800, 18f, 20f)
    @Composable fun stat() = title(900, 40f, 40f, -0.01f, features = "tnum")
    @Composable fun rank() = title(800, 18f, 20f, features = "tnum")
    @Composable fun rowTitle() = body(700, 15f, 20f)
    @Composable fun body() = body(400, 14f, 20f)
    @Composable fun small() = body(500, 13f, 17f)
    @Composable fun eyebrow() = body(700, 12f, 15f, 0.06f)
    @Composable fun tileLabel() = body(600, 12f, 15f)
    @Composable fun button() = body(700, 15f, 18f)
    @Composable fun buttonSmall() = body(700, 13f, 16f)
    @Composable fun chip() = body(700, 14f, 18f)
    @Composable fun navLabel() = body(700, 15f, 18f)
    @Composable fun time() = body(500, 12.5f, 16f, features = "tnum")
}

// ---------------------------------------------------------------- motion modifiers (#1 & #5)

/** Motion #5: tactile press scale (0.97, spring 0.6/800) via graphicsLayer so only this tile redraws. */
@Composable
fun Modifier.bentoPress(interaction: MutableInteractionSource, enabled: Boolean = true): Modifier {
    val reduced = rememberReducedMotion()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) BentoMotion.PRESS_SCALE else 1f,
        animationSpec = BentoMotion.spring(BentoMotion.PRESS_DAMPING, BentoMotion.PRESS_STIFFNESS, reduced),
        label = "bentoPress",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Motion #1: tile entry stagger. Runs once on first arrival (rememberSaveable flag), never on Back return.
 * Tiles past index 10 snap immediately.
 */
@Composable
fun Modifier.bentoTileEnter(index: Int, pageKey: String = "page"): Modifier {
    val reduced = rememberReducedMotion()
    var arrived by rememberSaveable(pageKey, index) { mutableStateOf(false) }
    val delayMs = BentoMotion.tileDelayMs(index)
    val shouldAnimate = !reduced && !arrived && delayMs != null
    val anim = remember(pageKey, index) { Animatable(if (shouldAnimate) 0f else 1f) }
    LaunchedEffect(pageKey, index, reduced) {
        if (shouldAnimate && delayMs != null) {
            if (delayMs > 0) delay(delayMs.toLong())
            anim.animateTo(1f, BentoMotion.spring(BentoMotion.TILE_ENTER_DAMPING, BentoMotion.TILE_ENTER_STIFFNESS, reduced))
            arrived = true
        } else {
            anim.snapTo(1f)
            arrived = true
        }
    }
    return graphicsLayer {
        val p = anim.value
        val s = lerp(BentoMotion.TILE_ENTER_SCALE, 1f, p)
        scaleX = s
        scaleY = s
        translationY = (1f - p) * BentoMotion.TILE_ENTER_RISE_DP.dp.toPx()
        alpha = (p * 1.6f).coerceIn(0f, 1f)
    }
}

// ---------------------------------------------------------------- tiles

/**
 * A Bento tile: rounded container on [tone], no border and no shadow. Large tiles default to 28dp radius / 14dp padding;
 * small tiles to 22dp / 12dp.
 */
@Composable
fun BentoTile(
    modifier: Modifier = Modifier,
    tone: BentoTone = BentoTone.Surface,
    small: Boolean = false,
    padding: Dp = if (small) BentoRules.TILE_SMALL_PADDING.dp else BentoRules.TILE_LARGE_PADDING.dp,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val shape = RoundedCornerShape(if (small) BentoRules.TILE_SMALL_RADIUS.dp else BentoRules.TILE_LARGE_RADIUS.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressMod = if (onClick != null) Modifier.bentoPress(interaction) else Modifier
    val clickMod = if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = clickLabel, onClick = onClick) else Modifier
    Column(
        modifier = modifier.then(pressMod).clip(shape).background(tone.fill(p, t)).then(clickMod).padding(padding),
        content = content,
    )
}

/** A two-column row of Bento tiles with Bento's 10dp gap. */
@Composable
fun BentoPairRow(
    modifier: Modifier = Modifier,
    left: @Composable RowScope.() -> Unit,
    right: @Composable RowScope.() -> Unit,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BentoRules.GRID_GAP.dp)) {
        left()
        right()
    }
}

/** Tile eyebrow (caps, 12sp 700 +0.06em tracking). */
@Composable
fun BentoEyebrow(text: String, modifier: Modifier = Modifier, tone: BentoTone = BentoTone.Surface, color: Color? = null) {
    val p = LocalPalette.current
    val t = bentoTones()
    Text(
        text.uppercase(),
        modifier = modifier,
        style = BentoType.eyebrow(),
        color = color ?: tone.eyebrow(p, t),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Tile label ("Radio", "This month") in Figtree 600 12sp. */
@Composable
fun BentoTileLabel(text: String, modifier: Modifier = Modifier, tone: BentoTone = BentoTone.Surface) {
    val p = LocalPalette.current
    val t = bentoTones()
    Text(text, modifier = modifier, style = BentoType.tileLabel(), color = tone.muted(p, t), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** Tile heading ("Newly added", "Most played") with optional trailing link ("See all"). */
@Composable
fun BentoTileHeader(
    title: String,
    modifier: Modifier = Modifier,
    tone: BentoTone = BentoTone.Surface,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            title,
            style = BentoType.tileTitle(),
            color = tone.content(p, t),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (actionLabel != null && onAction != null) {
            Box(
                Modifier.heightIn(min = 36.dp).clip(RoundedCornerShape(18.dp))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, style = BentoType.buttonSmall(), color = if (tone == BentoTone.Deep) t.deepEyebrow else p.control)
            }
        }
    }
}

// ---------------------------------------------------------------- artwork & cover stack

enum class BentoCoverSize(val radiusDp: Float) {
    Large(BentoRules.COVER_LARGE_RADIUS),
    Tile(BentoRules.COVER_TILE_RADIUS),
    Row(BentoRules.COVER_ROW_RADIUS),
    Small(BentoRules.COVER_SMALL_RADIUS),
}

@Composable
fun BentoCover(
    url: String?,
    modifier: Modifier = Modifier,
    sizeKind: BentoCoverSize = BentoCoverSize.Large,
) {
    val shape = RoundedCornerShape(sizeKind.radiusDp.dp)
    Cover(url, modifier.clip(shape), rect = true)
}

/** Motion #11: three fanned covers at -10deg, +6deg, 0deg; opens 4deg wider on press. */
@Composable
fun BentoCoverStack(
    urls: List<String?>,
    modifier: Modifier = Modifier,
    coverSize: Dp = 64.dp,
    pressed: Boolean = false,
) {
    val reduced = rememberReducedMotion()
    val shown = remember(urls) { urls.take(3).reversed() }
    val baseAngles = BentoMotion.COVER_STACK_ANGLES
    val anims = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(reduced) {
        if (reduced) {
            anims.forEachIndexed { i, a -> a.snapTo(baseAngles[i]) }
        } else {
            anims.forEachIndexed { i, a ->
                if (i > 0) delay(BentoMotion.COVER_STACK_STAGGER_MS.toLong())
                a.animateTo(baseAngles[i], BentoMotion.spring(BentoMotion.COVER_STACK_DAMPING, BentoMotion.COVER_STACK_STIFFNESS, false))
            }
        }
    }
    val openExtra by animateFloatAsState(
        if (pressed && !reduced) BentoMotion.COVER_STACK_PRESS_OPEN_DEG else 0f,
        BentoMotion.spring(BentoMotion.PRESS_DAMPING, BentoMotion.PRESS_STIFFNESS, reduced),
        label = "stackOpen",
    )
    val t = bentoTones()
    Box(modifier.height(coverSize + 14.dp).width(coverSize + 36.dp), contentAlignment = Alignment.Center) {
        shown.forEachIndexed { idx, url ->
            val slot = (3 - shown.size + idx).coerceIn(0, 2)
            val rot = anims[slot].value + when (slot) {
                0 -> -openExtra
                1 -> openExtra
                else -> 0f
            }
            val offsetX = when (slot) {
                0 -> (-14).dp
                1 -> 12.dp
                else -> 0.dp
            }
            val shape = RoundedCornerShape(BentoRules.COVER_TILE_RADIUS.dp)
            Box(
                Modifier
                    .offset(x = offsetX)
                    .size(coverSize)
                    .graphicsLayer { rotationZ = rot }
                    .then(if (slot == 2) Modifier.shadow(10.dp, shape, ambientColor = t.shadowColor, spotColor = t.shadowColor) else Modifier)
                    .clip(shape)
                    .background(LocalPalette.current.raised),
            ) {
                Cover(url, Modifier.size(coverSize), rect = true)
            }
        }
    }
}

// ---------------------------------------------------------------- buttons, icon buttons & get morph (#10)

enum class BentoButtonKind { Primary, Outline, Danger }

@Composable
fun BentoButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: BentoButtonKind = BentoButtonKind.Primary,
    small: Boolean = false,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    tone: BentoTone = BentoTone.Surface,
) {
    BentoButtonBody(kind, onClick, modifier, enabled, small, tone) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(BentoRules.ICON_INLINE.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun BentoButtonBody(
    kind: BentoButtonKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    small: Boolean = false,
    tone: BentoTone = BentoTone.Surface,
    content: @Composable RowScope.() -> Unit,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val interaction = remember { MutableInteractionSource() }
    val fill = when {
        !enabled -> p.raised
        kind == BentoButtonKind.Primary -> p.control
        kind == BentoButtonKind.Danger -> p.chartRedText
        else -> tone.chipFill(p, t)
    }
    val fg = when {
        !enabled -> p.textMuted
        kind == BentoButtonKind.Primary -> p.onControl
        kind == BentoButtonKind.Danger -> Color.White
        else -> p.control
    }
    val h = if (small) BentoRules.BUTTON_SMALL_HEIGHT.dp else BentoRules.BUTTON_HEIGHT.dp
    val pad = if (small) 14.dp else 18.dp
    val shape = RoundedCornerShape(BentoRules.BUTTON_RADIUS.dp)
    Row(
        modifier = modifier
            .bentoPress(interaction, enabled)
            .heightIn(min = h)
            .clip(shape)
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = pad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides fg) {
            ProvideTextStyle(if (small) BentoType.buttonSmall() else BentoType.button()) { content() }
        }
    }
}

@Composable
fun BentoIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    solid: Boolean = false,
    small: Boolean = false,
    inHeader: Boolean = false,
    tone: BentoTone = BentoTone.Surface,
    enabled: Boolean = true,
    tint: Color? = null,
    fillOverride: Color? = null,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val interaction = remember { MutableInteractionSource() }
    val fill = fillOverride ?: when {
        !enabled -> p.raised
        solid -> p.control
        inHeader -> p.surface
        else -> tone.chipFill(p, t)
    }
    val iconTint = tint ?: when {
        !enabled -> p.textMuted
        solid -> p.onControl
        tone == BentoTone.Deep -> t.onDeep
        tone != BentoTone.Surface -> t.onTone
        else -> p.control
    }
    val circle = if (small) BentoRules.ICON_BUTTON_SMALL.dp else BentoRules.ICON_BUTTON.dp
    val glyph = if (small) BentoRules.ICON_INLINE.dp else BentoRules.ICON.dp
    Box(
        modifier = modifier
            .bentoPress(interaction, enabled)
            .size(circle)
            .clip(CircleShape)
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(glyph))
    }
}

/**
 * Motion #10: Get button with morphing width across Idle / Pending / Requested / Owned / Failed.
 * The pending refresh icon turns once per 900ms only while pending and resumed.
 */
@Composable
fun BentoGetButton(
    state: GetState,
    onGet: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = true,
    tone: BentoTone = BentoTone.Surface,
    idleLabel: String = "Get",
    owned: Boolean = false,
) {
    val p = LocalPalette.current
    val reduced = rememberReducedMotion()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    if (owned) {
        Row(
            modifier.heightIn(min = if (small) BentoRules.BUTTON_SMALL_HEIGHT.dp else BentoRules.BUTTON_HEIGHT.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(BentoIcons.Check, null, tint = p.ok, modifier = Modifier.size(16.dp))
            Text("In library", style = BentoType.buttonSmall(), color = p.ok)
        }
        return
    }
    AnimatedContent(
        targetState = state,
        modifier = modifier,
        transitionSpec = {
            (fadeIn(BentoMotion.fade(BentoMotion.GET_FADE_IN_MS, reduced, BentoMotion.GET_FADE_IN_DELAY_MS)) togetherWith
                fadeOut(BentoMotion.fade(BentoMotion.GET_FADE_OUT_MS, reduced))) using
                SizeTransform(clip = false) { _, _ -> BentoMotion.tween(BentoMotion.GET_MORPH_MS, BentoMotion.Emphasized, reduced) }
        },
        label = "bentoGetMorph",
    ) { s ->
        when (s) {
            GetState.Idle -> BentoButton(idleLabel, onGet, kind = BentoButtonKind.Primary, small = small, icon = BentoIcons.Get, tone = tone)
            GetState.Pending -> {
                val angle = if (!reduced && resumed) {
                    val inf = rememberInfiniteTransition(label = "bentoGetSpin")
                    val a by inf.animateFloat(
                        0f, 360f,
                        infiniteRepeatable(tween(BentoMotion.GET_SPIN_MS, easing = LinearEasing), RepeatMode.Restart),
                        label = "angle",
                    )
                    a
                } else 0f
                BentoButtonBody(BentoButtonKind.Outline, onClick = {}, enabled = false, small = small, tone = tone) {
                    Icon(
                        BentoIcons.Refresh, null, tint = p.control,
                        modifier = Modifier.size(BentoRules.ICON_INLINE.dp).graphicsLayer { rotationZ = angle },
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Getting", color = p.textMuted)
                }
            }
            is GetState.Requested -> Row(
                Modifier.heightIn(min = if (small) BentoRules.BUTTON_SMALL_HEIGHT.dp else BentoRules.BUTTON_HEIGHT.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(BentoIcons.Check, null, tint = p.textMuted, modifier = Modifier.size(16.dp))
                Text("Requested", style = BentoType.buttonSmall(), color = p.textMuted)
            }
            is GetState.Failed -> BentoButtonBody(BentoButtonKind.Outline, onClick = onGet, small = small, tone = tone) {
                Text("Try again", color = p.chartRedText)
            }
        }
    }
}

// ---------------------------------------------------------------- chips, tags, tabs

@Composable
fun BentoChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    tone: BentoTone? = null,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val interaction = remember { MutableInteractionSource() }
    val fill = when {
        selected -> t.deep
        tone != null -> tone.chipFill(p, t)
        else -> p.surface
    }
    val fg = if (selected) t.onDeep else p.text
    val h = if (small) BentoRules.CHIP_SMALL_HEIGHT.dp else BentoRules.CHIP_HEIGHT.dp
    val shape = RoundedCornerShape(BentoRules.CHIP_RADIUS.dp)
    Box(
        modifier = modifier
            .bentoPress(interaction)
            .heightIn(min = h)
            .clip(shape)
            .background(fill)
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = if (small) 12.dp else 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = if (small) BentoType.small().copy(fontWeight = FontWeight.Bold) else BentoType.chip(), color = fg, maxLines = 1)
    }
}

/** Bento chip strip for SegmentedTabs (lenses, Album tabs, Library sort, presets, Paths kinds). */
@Composable
fun BentoTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { i, label ->
            BentoChip(label, selected = i == selectedIndex, onClick = { onSelect(i) })
        }
    }
}

/** Small pill tag: 12sp Figtree 600 on `warm` ("Not in your library") or `leaf` ("FLAC 16/44.1"). */
@Composable
fun BentoTag(
    text: String,
    modifier: Modifier = Modifier,
    tone: BentoTone = BentoTone.Warm,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(tone.fill(p, t)).padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = BentoType.tileLabel(), color = tone.content(p, t), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 18dp pill badge in `accent` with `onAccent` 11sp 700 count. */
@Composable
fun BentoBadge(count: Int, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Box(
        modifier.height(BentoRules.BADGE_HEIGHT.dp).widthIn(min = BentoRules.BADGE_HEIGHT.dp)
            .clip(RoundedCornerShape(9.dp)).background(p.accent).padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else "$count",
            color = p.onAccent,
            style = BentoType.eyebrow().copy(fontSize = 11.sp, lineHeight = 13.sp, letterSpacing = 0.em),
        )
    }
}

// ---------------------------------------------------------------- rows, fields, switch, progress

@Composable
fun BentoRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tone: BentoTone = BentoTone.Surface,
    active: Boolean = false,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
    lead: (@Composable () -> Unit)? = null,
    trail: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val interaction = remember { MutableInteractionSource() }
    val clickMod = if (onClick != null) Modifier.bentoPress(interaction).clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick) else Modifier
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = BentoRules.ROW_MIN.dp).then(clickMod).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            lead?.invoke()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = BentoType.rowTitle(),
                    color = if (active) p.accent else tone.content(p, t),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = BentoType.small(),
                        color = tone.muted(p, t),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            trail?.invoke()
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(tone.divider(p)))
    }
}

/** 48dp pill search field on `surface` with 2dp inset `control` ring when focused. */
@Composable
fun BentoSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(BentoRules.SEARCH_HEIGHT.dp)
            .clip(shape)
            .background(p.surface)
            .then(if (focused) Modifier.border(2.dp, p.control, shape) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(BentoIcons.Search, null, tint = p.control, modifier = Modifier.size(20.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, style = BentoType.body(), color = p.textMuted, maxLines = 1)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = BentoType.body().copy(color = p.text),
                cursorBrush = SolidColor(p.control),
                interactionSource = interaction,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty() && onClear != null) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClear)
                    .semantics { contentDescription = "Clear search" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(BentoIcons.Close, null, tint = p.textMuted, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** 52x32 pill switch: off = `raised` + `surface` knob; on = `control` + `onControl` knob. */
@Composable
fun BentoSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val reduced = rememberReducedMotion()
    val knobX by animateDpAsState(
        if (checked) 20.dp else 0.dp,
        BentoMotion.spring(BentoMotion.NAV_PILL_DAMPING, BentoMotion.NAV_PILL_STIFFNESS, reduced),
        label = "bentoSwitchKnob",
    )
    val trackColor by animateColorAsState(
        if (checked) p.control else p.raised,
        BentoMotion.fade(160, reduced),
        label = "bentoSwitchTrack",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.size(width = BentoRules.SWITCH_WIDTH.dp, height = BentoRules.SWITCH_HEIGHT.dp)
            .clip(shape).background(trackColor).padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier.offset(x = knobX).size(24.dp).clip(CircleShape)
                .background(if (checked) p.onControl else p.surface),
        )
    }
}

/** 6dp rounded progress track (`raised` with `control` fill). */
@Composable
fun BentoProgress(fraction: Float, modifier: Modifier = Modifier, color: Color? = null) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(3.dp)
    Box(modifier.fillMaxWidth().height(BentoRules.SEEK_HEIGHT.dp).clip(shape).background(p.raised)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(shape).background(color ?: p.control))
    }
}

/** Four 6dp rounded segments, 4dp apart, with 11sp step labels underneath (BENTO_THEME.md 7.2). */
@Composable
fun BentoTracker(
    reachedSteps: Int,
    failedStep: Int? = null,
    labels: List<String> = listOf("Find", "Fetch", "Import", "Ready"),
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { idx, _ ->
                val fill = when {
                    failedStep == idx -> p.chartRedText
                    idx < reachedSteps -> p.control
                    else -> p.raised
                }
                Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(fill))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { idx, name ->
                val textColor = when {
                    failedStep == idx -> p.chartRedText
                    idx < reachedSteps -> p.text
                    else -> p.textMuted
                }
                Text(
                    name,
                    style = BentoType.tileLabel().copy(fontSize = 11.sp, lineHeight = 14.sp),
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 12dp rounded ranked bars on `chip` track: first in `control`, rest in `onTone` at 35%. */
@Composable
fun BentoRankedBars(
    items: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    tone: BentoTone = BentoTone.Surface,
    valueText: ((Int) -> String)? = null,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    val maxVal = (items.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(0.01f)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEachIndexed { idx, (label, v) ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = BentoType.small().copy(fontWeight = FontWeight.SemiBold), color = tone.content(p, t), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (valueText != null) Text(valueText(idx), style = BentoType.time(), color = tone.muted(p, t))
                }
                val shape = RoundedCornerShape(6.dp)
                Box(Modifier.fillMaxWidth().height(12.dp).clip(shape).background(if (tone == BentoTone.Surface) p.raised else t.chip)) {
                    Box(
                        Modifier.fillMaxWidth((v / maxVal).coerceIn(0.04f, 1f)).fillMaxHeight()
                            .clip(shape)
                            .background(if (idx == 0) p.control else t.onTone.copy(alpha = 0.35f)),
                    )
                }
            }
        }
    }
}

/** Rounded columns (`8 8 4 4`), `chart3` for history and `accent` for the current period. */
@Composable
fun BentoColumnChart(
    values: List<Float>,
    labels: List<String> = emptyList(),
    highlightLast: Boolean = true,
    modifier: Modifier = Modifier,
    barHeight: Dp = 96.dp,
) {
    val p = LocalPalette.current
    val maxVal = (values.maxOfOrNull { it } ?: 1f).coerceAtLeast(0.01f)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().height(barHeight),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEachIndexed { i, v ->
                val frac = (v / maxVal).coerceIn(0.08f, 1f)
                val shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                Box(
                    Modifier.weight(1f).fillMaxHeight(frac).clip(shape)
                        .background(if (highlightLast && i == values.lastIndex) p.accent else p.chart3),
                )
            }
        }
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEach { lbl ->
                    Text(lbl, style = BentoType.tileLabel().copy(fontSize = 11.sp), color = p.textMuted, maxLines = 1, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Warm small-tile banner for offline / service issues with Retry outline button. */
@Composable
fun BentoBanner(
    title: String,
    subtitle: String? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val t = bentoTones()
    BentoTile(modifier.fillMaxWidth(), tone = BentoTone.Warm, small = true) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(BentoIcons.Warning, null, tint = t.onTone, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = BentoType.rowTitle(), color = t.onTone)
                if (!subtitle.isNullOrBlank()) Text(subtitle, style = BentoType.small(), color = t.onToneMuted)
            }
            if (onRetry != null) {
                BentoButton("Retry", onRetry, kind = BentoButtonKind.Outline, small = true, tone = BentoTone.Warm)
            }
        }
    }
}

/** Flat `raised` skeleton block with Bento's tile radius and no shimmer. */
@Composable
fun BentoSkeleton(modifier: Modifier = Modifier, small: Boolean = false) {
    val shape = RoundedCornerShape(if (small) BentoRules.TILE_SMALL_RADIUS.dp else BentoRules.TILE_LARGE_RADIUS.dp)
    Box(modifier.clip(shape).background(LocalPalette.current.raised))
}

/**
 * Motion #13: three 3dp playing bars in `accent`, oscillating 4dp..14dp over 900ms with 0 / -300 / -600ms phases.
 * Stops automatically when paused or lifecycle is not RESUMED; static at 9dp when reduced.
 */
@Composable
fun BentoPlayingBars(playing: Boolean, modifier: Modifier = Modifier, color: Color? = null) {
    val p = LocalPalette.current
    val reduced = rememberReducedMotion()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animate = playing && !reduced && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val barColor = color ?: p.accent
    val heights = if (animate) {
        val inf = rememberInfiniteTransition(label = "bentoBars")
        BentoMotion.BARS_PHASE_MS.mapIndexed { idx, phase ->
            val h by inf.animateFloat(
                initialValue = BentoMotion.BARS_MIN_DP.toFloat(),
                targetValue = BentoMotion.BARS_MAX_DP.toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(BentoMotion.BARS_MS),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(phase),
                ),
                label = "bar$idx",
            )
            h
        }
    } else {
        listOf(BentoMotion.BARS_REST_DP.toFloat(), BentoMotion.BARS_REST_DP.toFloat(), BentoMotion.BARS_REST_DP.toFloat())
    }
    Row(
        modifier.height(BentoMotion.BARS_MAX_DP.dp).clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { h ->
            Box(Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(1.5.dp)).background(barColor))
        }
    }
}

/** Bento body for ConfirmSheet: eyebrow, tile title, body copy, safe outline pill button then destructive danger pill button. */
@Composable
fun BentoConfirmBody(spec: ConfirmSpec, onKeep: () -> Unit, onConfirm: () -> Unit) {
    val p = LocalPalette.current
    Column(
        Modifier.fillMaxWidth().padding(start = BentoRules.SIDE_MARGIN.dp, end = BentoRules.SIDE_MARGIN.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            spec.eyebrow?.let { BentoEyebrow(it) }
            Text(spec.title, style = BentoType.tileTitle(), color = p.text)
        }
        Text(spec.message, style = BentoType.body(), color = p.textMuted)
        ConfirmRules.actions(spec).forEach { action ->
            when (action) {
                is ConfirmAction.Keep -> BentoButton(action.label, onKeep, Modifier.fillMaxWidth(), kind = BentoButtonKind.Outline)
                is ConfirmAction.Delete -> BentoButton(action.label, onConfirm, Modifier.fillMaxWidth(), kind = BentoButtonKind.Danger)
            }
        }
    }
}
