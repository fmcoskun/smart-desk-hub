package com.iot.widget

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.iot.widget.ui.shell.AppRoot
import com.iot.widget.ui.theme.SmartDeskTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The only Activity. In a Compose app the Activity is just a host: it sets the window up
 * and calls setContent { } once; every screen after that is a @Composable function.
 *
 * @AndroidEntryPoint lets Hilt inject into this Activity and into the ViewModels it creates.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the status/navigation bars (light bars, dark icons: matches the grey design).
        // Screens add the system-bar padding themselves.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            SmartDeskTheme {
                AppRoot()
            }
        }
    }
}
