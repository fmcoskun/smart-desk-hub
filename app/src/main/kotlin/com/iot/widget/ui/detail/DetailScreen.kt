package com.iot.widget.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.DetailType
import com.iot.widget.domain.PlugTimer
import com.iot.widget.ui.components.NeuSwitch
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.components.rememberNotificationPermissionRequest
import com.iot.widget.ui.components.neuInset
import com.iot.widget.ui.components.neuRaised
import com.iot.widget.ui.components.windowCard
import com.iot.widget.ui.dashboard.DashboardViewModel
import com.iot.widget.ui.dashboard.icon
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes
import java.util.Locale

/**
 * UI layer · a full-screen detail page (reference `.detail-panel`) built on SubPage: Back button,
 * device icon and title, then the content of the selected device. Controls are disabled for
 * users who may not control devices (Guest). The slide-in animation is set up in AppNavHost.
 */
@Composable
fun DetailScreen(onBack: () -> Unit, viewModel: DetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceId = DashboardViewModel.DETAIL_OF.entries.first { it.value == viewModel.type }.key
    // The one runtime uppercase in the app: the name is user data (renamable in step 3),
    // so it cannot be pre-written in strings.xml. Locale.ROOT avoids the Turkish "İ".
    SubPage(title = state?.deviceName.orEmpty().uppercase(Locale.ROOT), icon = deviceId.icon, onBack = onBack) {
        val s = state ?: return@SubPage
        val enabled = s.canControl && s.deviceOnline
        if (!s.deviceOnline) {
            Text(stringResource(R.string.status_offline_card), fontSize = 12.sp, color = DeskColors.Red)
        }
        // Turning an automation on asks for the notification permission first (Android 13+)
        val askNotifications = rememberNotificationPermissionRequest()
        // Re-checked every time the screen resumes, e.g. after returning from Settings
        var exactAllowed by remember { mutableStateOf(viewModel.exactAlarmsAllowed()) }
        LifecycleResumeEffect(Unit) {
            exactAllowed = viewModel.exactAlarmsAllowed()
            onPauseOrDispose { }
        }
        val context = LocalContext.current
        val openAlarmSettings = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName)),
                )
            }
        }
        when (s.type) {
            DetailType.Bluetooth -> BluetoothDetail(s, viewModel::setAudioOutput, viewModel::setVolume, enabled)
            DetailType.Light -> LightDetail(
                state = s,
                onColorTemp = viewModel::setColorTemp,
                onAutoDim = { on -> if (on) askNotifications(); viewModel.setAutoDim(on) },
                onSchedule = { on, onDays, off, offDays, scheduleOn ->
                    if (scheduleOn) askNotifications()
                    viewModel.setSchedule(on, onDays, off, offDays, scheduleOn)
                },
                exactAllowed = exactAllowed,
                onAllowExact = openAlarmSettings,
                enabled = enabled,
            )
            DetailType.Rig -> PlugDetail(
                state = s,
                onTimer = { timer ->
                    if (timer != PlugTimer.Off) askNotifications()
                    viewModel.setPlugTimer(timer)
                },
                exactAllowed = exactAllowed,
                onAllowExact = openAlarmSettings,
                enabled = enabled,
            )
            DetailType.Privacy -> PrivacyDetail(
                state = s,
                onOnLeave = viewModel::setPrivacyOnLeave,
                onOnLock = { on -> if (on) askNotifications(); viewModel.setPrivacyOnLock(on) },
                lockNote = {
                    Text(stringResource(R.string.privacy_lock_note), fontSize = 11.sp, color = DeskColors.TextMuted)
                },
                enabled = enabled,
            )
        }
    }
}

// ---- Building blocks shared by the four detail contents ----------------------------------

/** A titled block (`.detail-section`). */
@Composable
fun DetailSection(label: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (label != null) com.iot.widget.ui.components.SectionLabel(label)
        content()
    }
}

/** Recessed grey container for lists and stats (`.inset-box`). */
@Composable
fun InsetBox(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neuInset(
                DeskShapes.Drawer,
                distance = 4.dp,
                blur = 8.dp,
                background = DeskColors.InsetBg,
                dark = Color(0x2E000000),
                light = Color(0xB3FFFFFF),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/**
 * Shown under time-based settings while exact alarms are not allowed: says why the automation
 * may run late and offers to open the "Alarms & reminders" setting (spec §9c).
 */
@Composable
fun ExactTimingNotice(onAllow: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.exact_timing_off), fontSize = 11.sp, color = DeskColors.TextMuted, modifier = Modifier.weight(1f))
        com.iot.widget.ui.components.NeuChipButton(
            text = stringResource(R.string.exact_timing_allow),
            icon = Bi.Clock,
            active = false,
            onClick = onAllow,
        )
    }
}

/** Label on the left, full-size switch on the right (`.option-row`). */
@Composable
fun OptionRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        NeuSwitch(checked = checked, onCheckedChange = if (enabled) onChange else null, contentDescription = label)
    }
}
