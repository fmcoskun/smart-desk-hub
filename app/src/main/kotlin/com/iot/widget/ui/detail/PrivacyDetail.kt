package com.iot.widget.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.iot.widget.R
import com.iot.widget.ui.components.DeviceRow
import com.iot.widget.ui.components.Pill
import com.iot.widget.ui.components.PillStyle
import com.iot.widget.ui.icons.Bi

/**
 * UI layer · Privacy Guard details: mic and webcam status (BLOCKED while the guard is on,
 * LIVE otherwise) and the two automatic triggers.
 */
@Composable
fun PrivacyDetail(
    state: DetailUiState,
    onOnLeave: (Boolean) -> Unit,
    onOnLock: (Boolean) -> Unit,
    lockNote: @Composable () -> Unit = {},
    enabled: Boolean = true,
) {
    val blocked = state.deviceOn
    val pillText = stringResource(if (blocked) R.string.pill_blocked else R.string.pill_live)
    val pillStyle = if (blocked) PillStyle.Off else PillStyle.On
    DetailSection(stringResource(R.string.section_hardware_status)) {
        InsetBox {
            DeviceRow(Bi.MicFill, stringResource(R.string.hw_microphone), stringResource(R.string.hw_microphone_sub)) {
                Pill(pillText, pillStyle)
            }
            DeviceRow(Bi.WebcamFill, stringResource(R.string.hw_webcam), stringResource(R.string.hw_webcam_sub)) {
                Pill(pillText, pillStyle)
            }
        }
    }
    DetailSection(null) {
        OptionRow(stringResource(R.string.option_privacy_on_leave), state.settings.privacyOnLeave, onOnLeave, enabled)
        OptionRow(stringResource(R.string.option_privacy_on_lock), state.settings.privacyOnLock, onOnLock, enabled)
        lockNote()
    }
}
