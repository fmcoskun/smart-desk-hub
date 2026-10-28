package com.iot.widget.ui.cameras

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors
import java.util.Locale

/** UI layer · one camera, full width (spec §6.6). */
@Composable
fun CameraScreen(onBack: () -> Unit, onRecordings: (String) -> Unit, viewModel: CameraViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CameraContent(
        state,
        onBack = onBack,
        onSnapshot = viewModel::snapshot,
        onRecordings = { state.tile?.id?.let(onRecordings) },
    ) { modifier ->
        val tile = state.tile ?: return@CameraContent
        rememberClipPlayer(tile.clip, loop = true)?.let { VideoSurface(it, modifier) }
    }
}

@Composable
fun CameraContent(
    state: CameraUiState,
    onBack: () -> Unit,
    onSnapshot: () -> Unit,
    onRecordings: () -> Unit,
    video: @Composable (Modifier) -> Unit,
) {
    val tile = state.tile
    val snackbar = remember { SnackbarHostState() }
    val saved = stringResource(R.string.camera_snapshot_saved)
    // The count lives in the ViewModel and survives Back/rotation; the page remembers the last
    // count it confirmed (rememberSaveable), so only a NEW snapshot shows the message. A second
    // snapshot replaces the first message and restarts its timer.
    var confirmed by rememberSaveable { mutableIntStateOf(state.snapshots) }
    LaunchedEffect(state.snapshots) {
        if (state.snapshots > confirmed) {
            confirmed = state.snapshots
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(saved, duration = SnackbarDuration.Short)
        }
    }
    Box(Modifier.fillMaxSize()) {
        SubPage(title = tile?.name.orEmpty().uppercase(Locale.ROOT), icon = Bi.CameraVideoFill, onBack = onBack) {
            if (tile == null) return@SubPage
            CameraPicture(tile, state.clock, video, Modifier.fillMaxWidth().aspectRatio(16f / 9f), scale = 1.4f)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeuChipButton(
                    stringResource(R.string.camera_snapshot),
                    Bi.CameraFill,
                    active = false,
                    onClick = onSnapshot,
                    modifier = Modifier.weight(1f),
                    enabled = tile.feed == FeedState.LIVE,
                )
                if (state.canViewRecordings) {
                    NeuChipButton(stringResource(R.string.recordings), Bi.Film, active = false, onClick = onRecordings, modifier = Modifier.weight(1f))
                }
            }
        }
        SnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(bottom = 24.dp),
        ) { data ->
            Snackbar(data, containerColor = DeskColors.Text, contentColor = DeskColors.Bg)
        }
    }
}
