package com.iot.widget.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * UI layer · shape and size tokens from style.css. CSS pixels map 1:1 to dp here, because the
 * old WebView page used width=device-width (1 CSS px = 1 dp).
 */
object DeskShapes {
    val Window = RoundedCornerShape(28.dp)      // .widget-window, .detail-panel
    val Card = RoundedCornerShape(18.dp)        // --card-radius (.switch-holder)
    val Drawer = RoundedCornerShape(14.dp)      // .telemetry-drawer, .inset-box
    val Chip = RoundedCornerShape(12.dp)        // .scene-chip
    val Segment = RoundedCornerShape(10.dp)     // .seg-btn
    val IconBox = RoundedCornerShape(10.dp)     // .icon-box
    val Log = RoundedCornerShape(8.dp)          // .telemetry-log
}

object DeskDimens {
    /** Minimum size of anything tappable (spec: 44 px; Android's own guideline is 48 dp). */
    val MinTouch = 44.dp
}
