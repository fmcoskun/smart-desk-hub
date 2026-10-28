package com.iot.widget.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle

/**
 * UI layer · the app theme. Material 3 components are still used (dialogs, time picker), but
 * their default purple palette is replaced with the reference tokens, so nothing on screen
 * looks "stock Material".
 *
 * CompositionLocalProvider sets a default value that every composable below can read; here it
 * makes every Text use Roboto in the reference text colour unless told otherwise.
 */
@Composable
fun SmartDeskTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = DeskColors.Green,
        onPrimary = DeskColors.Bg,
        secondary = DeskColors.GreenText,
        background = DeskColors.Bg,
        onBackground = DeskColors.Text,
        surface = DeskColors.Bg,
        onSurface = DeskColors.Text,
        surfaceVariant = DeskColors.InsetBg,
        onSurfaceVariant = DeskColors.TextMuted,
        surfaceContainerHigh = DeskColors.Bg,
        surfaceContainerHighest = DeskColors.InsetBg,
        error = DeskColors.Red,
    )
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = RobotoFamily),
        displayMedium = base.displayMedium.copy(fontFamily = RobotoFamily),
        displaySmall = base.displaySmall.copy(fontFamily = RobotoFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = RobotoFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = RobotoFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = RobotoFamily),
        titleLarge = base.titleLarge.copy(fontFamily = RobotoFamily),
        titleMedium = base.titleMedium.copy(fontFamily = RobotoFamily),
        titleSmall = base.titleSmall.copy(fontFamily = RobotoFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = RobotoFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = RobotoFamily),
        bodySmall = base.bodySmall.copy(fontFamily = RobotoFamily),
        labelLarge = base.labelLarge.copy(fontFamily = RobotoFamily),
        labelMedium = base.labelMedium.copy(fontFamily = RobotoFamily),
        labelSmall = base.labelSmall.copy(fontFamily = RobotoFamily),
    )
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = RobotoFamily, color = DeskColors.Text),
            content = content,
        )
    }
}
