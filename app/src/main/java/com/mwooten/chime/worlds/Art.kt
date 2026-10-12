package com.mwooten.chime.worlds

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.mwooten.chime.data.Companion
import com.mwooten.chime.data.Settings

/**
 * How the user's character is rendered inside a mode, so the same Pocket Star (or Moth, or Puff)
 * shows up everywhere in that mode's own medium.
 * - SHINY: the full glossy character (Simple mode, modern UIs).
 * - FLAT: a 2D drawn version: flat fills, ink outline, no glow (paper, notes, chat avatars).
 * - PIXEL: a chunky low-res sprite of the same character (video game and old-computer modes).
 */
enum class ArtStyle { SHINY, FLAT, PIXEL }

/**
 * The user's character (their chosen look, color and personality) in a given [style].
 * [glow] = false drops the halo and floor shadow (for small avatars in lists).
 * [pixels] sets the PIXEL grid width (e.g. 24 = very chunky, 40 = finer).
 * Taps go to [onTap]; it is never draggable here (modes place it).
 */
@Composable
fun CharacterArt(
    size: Dp,
    style: ArtStyle = ArtStyle.SHINY,
    modifier: Modifier = Modifier,
    who: Companion? = null,
    level: Float = 0f,
    listening: Boolean = false,
    glow: Boolean = true,
    pixels: Int = 32,
    onTap: () -> Unit = {},
) {
    val ctx = LocalContext.current
    CharacterView(
        look = Settings.look(ctx), finish = Settings.finish(ctx), who = who ?: Settings.companion(ctx),
        level = level, listening = listening, size = size, draggable = false, modifier = modifier,
        style = style, showGlow = glow, pixels = pixels, onTap = onTap,
    )
}
