package com.iot.widget.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.iot.widget.R
import com.iot.widget.domain.ScheduleDays
import com.iot.widget.ui.components.DeviceRow
import com.iot.widget.ui.components.NeuSegmented
import com.iot.widget.ui.components.Pill
import com.iot.widget.ui.components.PillStyle
import com.iot.widget.ui.icons.Bi
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UI layer · Desk Lamp details: brightness, colour temperature, LED schedule, sunset dimming.
 * "07:30"-style times in the schedule editor.
 */
private val HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

/** Called when the schedule changes: on time/days, off time/days, enabled. */
typealias ScheduleChange = (LocalTime, ScheduleDays, LocalTime, ScheduleDays, Boolean) -> Unit

private enum class EditingLine { On, Off }

/**
 * UI layer · Desk Halo LED details: colour temperature, the on/off schedule (tap a line to edit
 * it) with its master switch, and auto-dim at sunset.
 */
@Composable
fun LightDetail(
    state: DetailUiState,
    onColorTemp: (Int) -> Unit,
    onAutoDim: (Boolean) -> Unit,
    onSchedule: ScheduleChange,
    exactAllowed: Boolean = true,
    onAllowExact: () -> Unit = {},
    enabled: Boolean = true,
) {
    val s = state.settings
    // rememberSaveable: an open editor (and which line it edits) survives rotation
    var editing by rememberSaveable { mutableStateOf<EditingLine?>(null) }

    DetailSection(stringResource(R.string.section_color_temperature)) {
        NeuSegmented(
            options = listOf(
                2700 to stringResource(R.string.ct_warm),
                4000 to stringResource(R.string.ct_neutral),
                6500 to stringResource(R.string.ct_cool),
            ),
            selected = s.colorTempK,
            onSelect = onColorTemp,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
        )
    }
    DetailSection(stringResource(R.string.section_schedule)) {
        InsetBox {
            DeviceRow(
                Bi.Sunrise,
                stringResource(R.string.schedule_turn_on),
                daysLabel(s.scheduleOnDays),
                Modifier.clickable(enabled = enabled, role = Role.Button) { editing = EditingLine.On },
            ) { Pill(s.scheduleOn.hm(), if (s.scheduleEnabled) PillStyle.On else PillStyle.Neutral) }
            DeviceRow(
                Bi.Sunset,
                stringResource(R.string.schedule_turn_off),
                daysLabel(s.scheduleOffDays),
                Modifier.clickable(enabled = enabled, role = Role.Button) { editing = EditingLine.Off },
            ) { Pill(s.scheduleOff.hm(), PillStyle.Neutral) }
        }
        OptionRow(stringResource(R.string.option_use_schedule), s.scheduleEnabled, enabled = enabled, onChange = { on ->
            onSchedule(s.scheduleOn, s.scheduleOnDays, s.scheduleOff, s.scheduleOffDays, on)
        })
        if (enabled && !exactAllowed) ExactTimingNotice(onAllowExact)
    }
    DetailSection(null) {
        OptionRow(stringResource(R.string.option_auto_dim), s.autoDim, onAutoDim, enabled)
    }

    when (editing) {
        EditingLine.On -> ScheduleEditorDialog(
            title = stringResource(R.string.schedule_edit_on),
            initialTime = s.scheduleOn,
            initialDays = s.scheduleOnDays,
            onDismiss = { editing = null },
            onSave = { time, days ->
                editing = null
                onSchedule(time, days, s.scheduleOff, s.scheduleOffDays, s.scheduleEnabled)
            },
        )
        EditingLine.Off -> ScheduleEditorDialog(
            title = stringResource(R.string.schedule_edit_off),
            initialTime = s.scheduleOff,
            initialDays = s.scheduleOffDays,
            onDismiss = { editing = null },
            onSave = { time, days ->
                editing = null
                onSchedule(s.scheduleOn, s.scheduleOnDays, time, days, s.scheduleEnabled)
            },
        )
        null -> Unit
    }
}

@Composable
fun daysLabel(days: ScheduleDays): String = when (days) {
    ScheduleDays.WEEKDAYS -> stringResource(R.string.days_weekdays)
    ScheduleDays.EVERY_DAY -> stringResource(R.string.days_every_day)
}

fun LocalTime.hm(): String = HOUR_MINUTE.format(this)
