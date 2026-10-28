package com.iot.widget.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.domain.ScheduleDays
import com.iot.widget.ui.components.NeuSegmented
import com.iot.widget.ui.theme.DeskColors
import java.time.LocalTime

/**
 * UI layer · dialog to edit one schedule line: a 24-hour clock picker plus Weekdays / Every day.
 * Material 3's TimePicker does the clock; its colours come from the reference tokens.
 *
 * `rememberSaveable { mutableStateOf(...) }` keeps the dialog's draft values across
 * recompositions and rotation (the time picker state is saveable too); nothing is saved
 * until "Save".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorDialog(
    title: String,
    initialTime: LocalTime,
    initialDays: ScheduleDays,
    onDismiss: () -> Unit,
    onSave: (LocalTime, ScheduleDays) -> Unit,
) {
    val time = rememberTimePickerState(initialTime.hour, initialTime.minute, is24Hour = true)
    var days by rememberSaveable { mutableStateOf(initialDays) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeskColors.Bg,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TimePicker(
                    state = time,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = DeskColors.InsetBg,
                        selectorColor = DeskColors.Green,
                        timeSelectorSelectedContainerColor = DeskColors.Green.copy(alpha = 0.15f),
                        timeSelectorSelectedContentColor = DeskColors.GreenText,
                        timeSelectorUnselectedContainerColor = DeskColors.InsetBg,
                    ),
                )
                NeuSegmented(
                    options = listOf(
                        ScheduleDays.WEEKDAYS to stringResource(R.string.days_weekdays),
                        ScheduleDays.EVERY_DAY to stringResource(R.string.days_every_day),
                    ),
                    selected = days,
                    onSelect = { days = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(LocalTime.of(time.hour, time.minute), days) }) {
                Text(stringResource(R.string.action_save), color = DeskColors.GreenText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = DeskColors.TextMuted)
            }
        },
    )
}
