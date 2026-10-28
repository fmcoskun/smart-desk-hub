package com.iot.widget.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * UI layer · design tokens. Every colour of the reference design (reference/web-ui/style.css,
 * the `:root` variables plus a few literals) lives here, so no screen hard-codes a colour.
 * Color(0xAARRGGBB): the first byte is opacity, e.g. 0xD9 = 85 %.
 */
object DeskColors {
    val Bg = Color(0xFFE8E8E8)              // --bg
    val Text = Color(0xFF2C3036)            // --text
    val TextMuted = Color(0xFF757D8A)       // --text-muted
    val ShadowLight = Color(0xD9FFFFFF)     // --shadow-light   rgba(255,255,255,0.85)
    val ShadowDark = Color(0x29000000)      // --shadow-dark    rgba(0,0,0,0.16)
    val ShadowDarkDeep = Color(0x3D000000)  // --shadow-dark-deep rgba(0,0,0,0.24)
    val Green = Color(0xFF00B33C)           // --accent-green
    val GreenGlow = Color(0x7300B33C)       // --accent-green-glow rgba(0,179,60,0.45)
    val Red = Color(0xFFE63946)             // --accent-red
    val GreenText = Color(0xFF007F2B)       // active scene chip / segment text
    val InsetBg = Color(0xFFE1E1E1)         // .telemetry-drawer / .inset-box
    val IconDefault = Color(0xFF3B414D)     // .icon-box
    val KnobOff = Color(0xFFEEEEEE)         // switch knob (OFF) and slider thumb
    val KnobOffText = Color(0xFF616161)
    val LogText = Color(0xFF333D47)         // .telemetry-log
    val SliderTrack = Color(0xFFE2E2E2)     // .slider-track-wrap
}
