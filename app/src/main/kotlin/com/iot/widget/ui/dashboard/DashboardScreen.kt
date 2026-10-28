package com.iot.widget.ui.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.DetailType
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Scene
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.windowCard
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes

/**
 * UI layer · the dashboard (reference index.html main panel).
 *
 * `hiltViewModel()` gives this screen its ViewModel (created once, kept across rotation).
 * `collectAsStateWithLifecycle()` reads the ViewModel's StateFlow as Compose state and stops
 * collecting while the app is in the background; whenever the state changes, Compose re-runs
 * ("recomposes") only the parts that read the changed values.
 */
@Composable
fun DashboardScreen(
    onOpenDetail: (DetailType) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        onScene = viewModel::onScene,
        onToggle = viewModel::onToggle,
        onBrightness = viewModel::onBrightnessChange,
        onOpenDetail = onOpenDetail,
    )
}

/** Stateless part: draws [state] and reports taps. Previews and tests can call it directly. */
@Composable
fun DashboardContent(
    state: DashboardUiState,
    onScene: (Scene) -> Unit,
    onToggle: (DeviceId, Boolean) -> Unit,
    onBrightness: (Int) -> Unit,
    onOpenDetail: (DetailType) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DeskColors.Bg)
            .windowInsetsPadding(WindowInsets.systemBars) // stay clear of status/navigation bars
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp)
                .fillMaxWidth()
                .windowCard(DeskShapes.Window)
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Header()
            if (state.loaded && !state.canControl) ViewOnlyBanner()
            // scene chips wait for the permission too, so they never flash disabled for an Admin
            if (state.loaded) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SceneChip(Scene.Work, Bi.BriefcaseFill, R.string.scene_work, state, onScene)
                SceneChip(Scene.Meet, Bi.CameraVideoFill, R.string.scene_meet, state, onScene)
                SceneChip(Scene.Leave, Bi.MoonStarsFill, R.string.scene_leave, state, onScene)
            }
            if (state.loaded) {
                state.devices.forEach { card ->
                    DeviceCard(
                        card = card,
                        onToggle = if (state.canControl && card.online) ({ on: Boolean -> onToggle(card.id, on) }) else null,
                        onOpen = { onOpenDetail(card.detail) },
                        dimmer = if (card.id == DeviceId.DeskLight) {
                            { DimmerRow(state.brightness, enabled = state.lightOn && state.canControl && card.online, onChange = onBrightness) }
                        } else {
                            null
                        },
                    )
                }
            }
            MqttConsole(state.lastMessage, state.pingMs)
            FooterStats(state.temperature, state.humidity, state.power)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.SceneChip(
    scene: Scene,
    icon: Bi,
    label: Int,
    state: DashboardUiState,
    onScene: (Scene) -> Unit,
) {
    NeuChipButton(
        text = stringResource(label),
        icon = icon,
        active = state.activeScene == scene,
        onClick = { onScene(scene) },
        modifier = Modifier.weight(1f),
        enabled = state.canControl,
    )
}

/** Shown to users who can look but not change anything (Guest). */
@Composable
private fun ViewOnlyBanner() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        BiIcon(Bi.Eye, tint = DeskColors.TextMuted, size = 12.sp)
        Text(stringResource(R.string.view_only_banner), fontSize = 11.sp, color = DeskColors.TextMuted)
    }
}

/** Title bar: grid icon, "SMART DESK HUB" and the pulsing green MQTT badge. */
@Composable
private fun Header() {
    Column {
        Row(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .padding(start = 2.dp, end = 2.dp, top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            BiIcon(Bi.Grid1x2Fill, tint = DeskColors.TextMuted, size = 12.sp)
            Text(
                stringResource(R.string.app_title_upper),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeskColors.TextMuted,
                maxLines = 1,
            )
            MqttBadge()
        }
        HorizontalDivider(thickness = 1.dp, color = Color(0x0D000000))
    }
}

@Composable
private fun MqttBadge() {
    // rememberInfiniteTransition: an animation that loops forever (the CSS @keyframes pulse, 2 s)
    val pulse = rememberInfiniteTransition(label = "pulse")
    val phase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase",
    )
    Row(
        modifier = Modifier
            .background(Color(0x1F00B33C), RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .scale(1f - 0.15f * phase)
                .graphicsLayer { alpha = 1f - 0.6f * phase }
                .dropShadow(CircleShape, Shadow(radius = 6.dp, color = DeskColors.Green))
                .background(DeskColors.Green, CircleShape),
        )
        Text(
            stringResource(R.string.mqtt_badge),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeskColors.Green,
        )
    }
}
