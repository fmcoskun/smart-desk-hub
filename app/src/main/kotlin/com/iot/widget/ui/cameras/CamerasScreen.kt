package com.iot.widget.ui.cameras

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.SectionLabel
import com.iot.widget.ui.components.windowCard
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes

/** UI layer · the Cameras tab: a 2×2 grid of live feeds (spec §6.6). */
@Composable
fun CamerasScreen(onOpen: (String) -> Unit, onRecordings: () -> Unit, viewModel: CamerasViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CamerasContent(state, onOpen, onRecordings) { tile, modifier ->
        // one player per visible LIVE tile; it is released when the tile leaves (privacy, offline, other tab)
        rememberClipPlayer(tile.clip, loop = true)?.let { VideoSurface(it, modifier) }
    }
}

/**
 * [video] draws the moving picture for a LIVE tile. The real screen passes an ExoPlayer
 * surface; tests pass an empty box, so they run without a video decoder.
 */
@Composable
fun CamerasContent(
    state: CamerasUiState,
    onOpen: (String) -> Unit,
    onRecordings: () -> Unit,
    video: @Composable (CameraTileUi, Modifier) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DeskColors.Bg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            Modifier
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp)
                .fillMaxWidth()
                .windowCard(DeskShapes.Window)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(stringResource(R.string.cameras_title_upper))
                Spacer(Modifier.weight(1f))
                if (state.canViewRecordings) {
                    NeuChipButton(stringResource(R.string.recordings), Bi.Film, active = false, onClick = onRecordings)
                }
            }
            state.tiles.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { tile ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable(role = Role.Button) { onOpen(tile.id) },
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            CameraPicture(tile, state.clock, video = { m -> video(tile, m) }, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                            Text(tile.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
