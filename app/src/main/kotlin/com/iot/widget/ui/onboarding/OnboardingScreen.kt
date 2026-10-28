package com.iot.widget.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iot.widget.R
import com.iot.widget.ui.components.IconBox
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.windowCard
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes
import kotlinx.coroutines.launch

/** One page of the welcome tour. */
private data class TourPage(val icon: Bi, @StringRes val title: Int, @StringRes val body: Int)

/** The four pages of spec §6.4, in order. */
private val PAGES = listOf(
    TourPage(Bi.BriefcaseFill, R.string.onb_scenes_title, R.string.onb_scenes_body),
    TourPage(Bi.LightbulbFill, R.string.onb_devices_title, R.string.onb_devices_body),
    TourPage(Bi.CameraVideoFill, R.string.onb_cameras_title, R.string.onb_cameras_body),
    TourPage(Bi.JournalText, R.string.onb_logs_title, R.string.onb_logs_body),
)

/**
 * UI layer · the welcome tour shown once per device after the first sign-in (spec §5, §6.4).
 *
 * HorizontalPager = a swipeable row of pages; its state remembers the current page, and
 * `animateScrollToPage` moves it from buttons too. "Last page" is judged on `settledPage` (the
 * page the pager has come to rest on), not `currentPage`, which already flips half-way through
 * a swipe — otherwise a quick second tap on Next could finish the tour before page 4 is seen.
 *
 * BackHandler takes over the system Back gesture only while it is enabled: on pages 2–4 Back
 * goes one page back. On page 1 of the first-run tour Android handles it as usual (leaving the
 * app) — Back never skips it; a tour replayed from the User Guide ([replay]) simply ends.
 */
@Composable
fun OnboardingContent(onFinish: () -> Unit, replay: Boolean = false) {
    val pager = rememberPagerState { PAGES.size }
    val scope = rememberCoroutineScope()
    val last = pager.settledPage == PAGES.lastIndex
    BackHandler(enabled = pager.currentPage > 0 || replay) {
        if (pager.currentPage == 0) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
    }
    Box(
        Modifier
            .fillMaxSize()
            // a replayed tour lies on top of the app: taps must not reach the screen below
            .pointerInput(Unit) { detectTapGestures { } }
            .background(DeskColors.Bg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(
            Modifier
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 24.dp)
                .fillMaxSize()
                .windowCard(DeskShapes.Window)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onFinish) {
                    Text(stringResource(R.string.onboarding_skip), color = DeskColors.TextMuted, fontWeight = FontWeight.Medium)
                }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index ->
                val page = PAGES[index]
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
                ) {
                    IconBox(page.icon, active = true, boxSize = 88.dp, iconSize = 38.sp, corner = 26.dp)
                    Text(stringResource(page.title), fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(stringResource(page.body), fontSize = 15.sp, color = DeskColors.TextMuted, textAlign = TextAlign.Center, lineHeight = 22.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                PAGES.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == pager.currentPage) 10.dp else 8.dp)
                            .background(if (i == pager.currentPage) DeskColors.Green else DeskColors.TextMuted.copy(alpha = 0.35f), CircleShape),
                    )
                }
            }
            NeuChipButton(
                stringResource(if (last) R.string.onboarding_get_started else R.string.onboarding_next),
                null,
                active = true,
                onClick = {
                    when {
                        last -> onFinish()
                        !pager.isScrollInProgress -> scope.launch { pager.animateScrollToPage(pager.settledPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
