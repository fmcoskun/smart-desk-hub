package com.iot.widget.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskDimens
import kotlin.math.roundToInt

/**
 * UI layer · the recessed dimmer of the reference (`.slider-track-wrap` + white-bordered knob).
 *
 * It reuses Material 3's Slider for the hard parts (dragging, tapping, accessibility actions)
 * and only replaces how the track and the thumb are drawn (the `track` / `thumb` slots).
 * The whole control is 44 dp tall so it is easy to grab; the visible track is 12 dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** Called once when the finger is lifted (e.g. seek only then, not on every drag step). */
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(it.roundToInt()) },
        valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interaction,
        modifier = modifier.height(DeskDimens.MinTouch),
        thumb = {
            Box(
                Modifier
                    .size(28.dp)
                    .neuRaised(
                        shape = CircleShape,
                        distance = 2.dp,
                        blur = 5.dp,
                        dark = Color(0x4D000000),
                        background = DeskColors.KnobOff,
                    )
                    .border(2.dp, Color.White, CircleShape),
            )
        },
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .neuInset(
                        shape = RoundedCornerShape(10.dp),
                        distance = 3.dp,
                        blur = 6.dp,
                        background = DeskColors.SliderTrack,
                        dark = Color(0x40000000),
                        light = Color(0xCCFFFFFF),
                    ),
            )
        },
    )
}
