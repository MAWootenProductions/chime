package com.mwooten.chime.ui

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Settings
import com.mwooten.chime.worlds.Bell
import com.mwooten.chime.worlds.Character
import com.mwooten.chime.worlds.Finish
import com.mwooten.chime.worlds.Look
import kotlinx.coroutines.delay

/**
 * The look of the opening screen, shared by every menu: near-black stage, quiet cards, the
 * companion up top, a Newsreader headline and Instrument Sans for everything you tap.
 */
object Stage {
    val bg = Color(0xFF0F0F12)
    val card = Color(0xFF1A1A1F)
    val ink = Color(0xFFF1EEE7)
    val mute = Color(0xFF8C8A93)
    val line = Color(0xFF2E2E36)
    val ok = Color(0xFF9FD6A0)
    val bad = Color(0xFFE58A5F)

    val ui = TextStyle(fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W500, fontSize = 15.sp, color = ink)
    val head = TextStyle(fontFamily = Fonts.newsreader, fontWeight = FontWeight.W600, fontSize = 30.sp, lineHeight = 36.sp, color = ink)
    val eyebrow = TextStyle(fontFamily = Fonts.instrumentSans, fontWeight = FontWeight.W600, fontSize = 11.sp, letterSpacing = 2.sp, color = mute)
    val body = ui.copy(color = mute, fontSize = 14.sp, lineHeight = 20.sp)
}

/** When the current page appeared (uptime ms). Rows rise in relative to this. */
val LocalStageStart = staticCompositionLocalOf { 0L }

/**
 * Staggered entrance: the [index]th thing on a page fades in and rises 16dp, 40ms after the one
 * before it. Anything composed after the entrance is over (scrolled into view later) just shows.
 */
@Composable
fun Rise(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val start = LocalStageStart.current
    val p = remember { Animatable(if (SystemClock.uptimeMillis() - start > 900L) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (p.value < 1f) {
            val wait = start + index.coerceIn(0, 14) * 40L - SystemClock.uptimeMillis()
            if (wait > 0) delay(wait)
            p.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
        }
    }
    Box(modifier.graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 16.dp.toPx()
    }) { content() }
}

/** Gives non-[StagePage] content (a bottom sheet) its own entrance clock. */
@Composable
fun StageEntrance(content: @Composable () -> Unit) {
    val start = remember { SystemClock.uptimeMillis() }
    CompositionLocalProvider(LocalStageStart provides start) { content() }
}

/**
 * A full-screen menu page: back at the top left, your live companion centered with a soft glow
 * of its color, the headline and subtitle, then [content] in a lazy list. The companion and
 * headline are the first list item, so they scroll away on long pages.
 * [who]/[look]/[finish] override the saved ones (for live previews).
 */
@Composable
fun StagePage(
    title: String,
    onBack: () -> Unit,
    subtitle: String? = null,
    backLabel: String = "Back",
    who: Companion? = null,
    look: Look? = null,
    finish: Finish? = null,
    characterSize: Dp = 120.dp,
    actions: (@Composable RowScope.() -> Unit)? = null,
    belowTitle: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val ctx = LocalContext.current
    val w = who ?: Settings.companion(ctx)
    val l = look ?: Settings.look(ctx)
    val f = finish ?: Settings.finish(ctx)
    val accent by animateColorAsState(Color(f.c), tween(450), label = "stageAccent")
    val start = remember { SystemClock.uptimeMillis() }

    CompositionLocalProvider(LocalStageStart provides start) {
        Column(Modifier.fillMaxSize().background(Stage.bg).systemBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 6.dp).height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹  $backLabel", style = Stage.ui.copy(color = Stage.mute, fontSize = 15.sp),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onBack).padding(horizontal = 12.dp, vertical = 10.dp))
                Spacer(Modifier.weight(1f))
                if (actions != null) actions()
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "stage_header") {
                    Rise(0) {
                        Column(
                            Modifier.fillMaxWidth().drawBehind {
                                val r = characterSize.toPx() * 1.3f
                                val c = Offset(size.width / 2f, characterSize.toPx() / 2f)
                                drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = .20f), accent.copy(alpha = 0f)), c, r), r, c)
                            },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Character(l, f, w, 0f, false, characterSize, draggable = false) { Bell.strike(ctx, w) }
                            Spacer(Modifier.height(6.dp))
                            Text(title, style = Stage.head, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            if (subtitle != null) Text(subtitle, style = Stage.body, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 8.dp, end = 8.dp))
                            if (belowTitle != null) belowTitle()
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                content()
            }
        }
    }
}

/** Press feedback shared by cards and buttons: a small spring down to 97%. */
@Composable
private fun pressScale(src: MutableInteractionSource): Float {
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) .97f else 1f, spring(dampingRatio = .6f, stiffness = 600f), label = "press")
    return s
}

/** A quiet card: 16dp corners, outlined in [selectedColor] when [selected], squishes when pressed. */
@Composable
fun StageCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    selectedColor: Color = Stage.ink,
    enabled: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    spacing: Dp = 4.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val scale = pressScale(src)
    val border by animateColorAsState(if (selected) selectedColor else selectedColor.copy(alpha = 0f), tween(240), label = "cardBorder")
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape).background(Stage.card).border(1.5.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(interactionSource = src, indication = LocalIndication.current, enabled = enabled, onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/** A menu row: title, a muted value with an optional status dot, and a chevron when it navigates. */
@Composable
fun StageRow(
    title: String,
    value: String? = null,
    dot: Color? = null,
    selected: Boolean = false,
    chevron: Boolean = true,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    StageCard(modifier, selected = selected, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            if (leading != null) leading()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = Stage.ui.copy(fontWeight = FontWeight.W600, fontSize = 17.sp))
                if (value != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (dot != null) {
                        val d by animateColorAsState(dot, tween(300), label = "dot")
                        Box(Modifier.size(8.dp).clip(CircleShape).background(d))
                    }
                    Text(value, style = Stage.body)
                }
            }
            if (trailing != null) trailing()
            if (chevron) Text("›", style = Stage.ui.copy(color = Stage.mute, fontSize = 24.sp))
        }
    }
}

/** A small letter-spaced group header. */
@Composable
fun StageLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 2.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = Stage.eyebrow, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = Stage.eyebrow)
    }
}

/** A button: filled with [fill] (dark text) or outlined (text in [tone]). */
@Composable
fun StageButton(
    label: String,
    modifier: Modifier = Modifier,
    fill: Color? = null,
    tone: Color = Stage.ink,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val scale = pressScale(src)
    val bg by animateColorAsState(fill ?: Stage.bg.copy(alpha = 0f), tween(240), label = "btnBg")
    val edge by animateColorAsState(if (fill == null) Stage.line else Stage.line.copy(alpha = 0f), tween(240), label = "btnEdge")
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier.graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else .45f }
            .clip(shape).background(bg).border(1.dp, edge, shape)
            .clickable(interactionSource = src, indication = LocalIndication.current, enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Stage.ui.copy(fontWeight = FontWeight.W600, fontSize = 15.sp, lineHeight = 19.sp, color = if (fill != null) Stage.bg else tone),
            textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** A text field as a 14dp-radius card; the edge brightens while you type. */
@Composable
fun StageField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    singleLine: Boolean = true,
    textStyle: TextStyle? = null,
    cursor: Color = Stage.ink,
    background: Color = Stage.card,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val style = textStyle ?: if (mono) Stage.ui.copy(fontFamily = FontFamily.Monospace, fontSize = 14.sp) else Stage.ui.copy(fontSize = 16.sp)
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    val edge by animateColorAsState(if (focused) Stage.mute else Stage.line, tween(220), label = "fieldEdge")
    val shape = RoundedCornerShape(14.dp)
    Box(modifier.fillMaxWidth().clip(shape).background(background).border(1.dp, edge, shape).padding(horizontal = 16.dp, vertical = 15.dp)) {
        if (value.isEmpty()) Text(placeholder, style = style.copy(color = Stage.mute))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = style,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            interactionSource = src,
            cursorBrush = SolidColor(cursor),
        )
    }
}
