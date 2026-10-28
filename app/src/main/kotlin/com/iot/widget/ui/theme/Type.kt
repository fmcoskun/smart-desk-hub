package com.iot.widget.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.iot.widget.R

/**
 * UI layer · fonts. Both families are bundled in res/font (instead of trusting the phone's
 * default font) so the app looks the same on every brand of phone.
 *
 * The reference CSS also uses weight 600; with only 500/700 loaded the browser renders 600 as
 * 700, so FontWeight.SemiBold below resolves to the Bold file — same result as the reference.
 */
val RobotoFamily = FontFamily(
    Font(R.font.roboto_light, FontWeight.Light),
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.SemiBold),
    Font(R.font.roboto_bold, FontWeight.Bold),
)

/** Monospace font of the MQTT console and the log lines. */
val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

/** Bootstrap Icons, converted from the reference's .woff (Android fonts must be TTF/OTF). */
val BootstrapIconsFamily = FontFamily(Font(R.font.bootstrap_icons))
