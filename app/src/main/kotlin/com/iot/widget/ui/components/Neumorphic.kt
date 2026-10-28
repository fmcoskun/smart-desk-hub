package com.iot.widget.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.iot.widget.ui.theme.DeskColors

/**
 * UI layer · the neumorphic look as reusable Modifiers.
 *
 * Neumorphism = the surface and its background share one colour, and depth comes only from
 * two shadows: a white one up-left (light source) and a dark one down-right.
 *  - raised: both shadows *outside* the shape → the element pops out
 *  - inset:  both shadows *inside* the shape  → the element looks pressed in
 *
 * A Modifier is a chain of decorations applied in order, so the shadows are drawn first and the
 * background is painted on top of them. Values mirror the CSS box-shadow rules
 * (`-4px -4px 8px light, 4px 4px 8px dark` → distance 4, blur 8).
 */
fun Modifier.neuRaised(
    shape: Shape,
    distance: Dp = 4.dp,
    blur: Dp = 8.dp,
    light: Color = DeskColors.ShadowLight,
    dark: Color = DeskColors.ShadowDark,
    background: Color = DeskColors.Bg,
): Modifier = this
    .dropShadow(shape, Shadow(radius = blur, color = light, offset = DpOffset(-distance, -distance)))
    .dropShadow(shape, Shadow(radius = blur, color = dark, offset = DpOffset(distance, distance)))
    .background(background, shape)

/** Pressed-in surface: background first, then the two shadows drawn inside the shape. */
fun Modifier.neuInset(
    shape: Shape,
    distance: Dp = 3.dp,
    blur: Dp = 6.dp,
    background: Color = DeskColors.Bg,
    dark: Color = DeskColors.ShadowDarkDeep,
    light: Color = DeskColors.ShadowLight,
): Modifier = this
    .background(background, shape)
    .innerShadow(shape, Shadow(radius = blur, color = dark, offset = DpOffset(distance, distance)))
    .innerShadow(shape, Shadow(radius = blur, color = light, offset = DpOffset(-distance, -distance)))

/**
 * The device card (`.switch-holder`): raised AND inset at the same time, which gives the
 * reference its soft "pillow" edge:
 * `-8 -8 15 white70, 10 10 10 black20, inset 8 8 15 white70, inset 10 10 10 black20`.
 */
fun Modifier.switchHolder(shape: Shape): Modifier = this
    .dropShadow(shape, Shadow(radius = 15.dp, color = Color(0xB3FFFFFF), offset = DpOffset((-8).dp, (-8).dp)))
    .dropShadow(shape, Shadow(radius = 10.dp, color = Color(0x33000000), offset = DpOffset(10.dp, 10.dp)))
    .background(DeskColors.Bg, shape)
    .innerShadow(shape, Shadow(radius = 15.dp, color = Color(0xB3FFFFFF), offset = DpOffset(8.dp, 8.dp)))
    .innerShadow(shape, Shadow(radius = 10.dp, color = Color(0x33000000), offset = DpOffset(10.dp, 10.dp)))

/** The main window card (`.widget-window`): a single soft shadow below, `0 10px 20px -10px`. */
fun Modifier.windowCard(shape: Shape): Modifier = this
    .dropShadow(shape, Shadow(radius = 20.dp, color = Color(0x73000000), spread = (-10).dp, offset = DpOffset(0.dp, 10.dp)))
    .background(DeskColors.Bg, shape)
