package com.iot.widget.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.detail.DetailSection
import com.iot.widget.ui.detail.InsetBox
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.JetBrainsMonoFamily
import java.util.Locale

/** UI layer · one guide topic: title, paragraphs, numbered steps. */
@Composable
fun GuideTopicScreen(onBack: () -> Unit, viewModel: GuideTopicViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val t = (state as? GuideTopicState.Loaded)?.topic
    SubPage(title = t?.title.orEmpty().uppercase(Locale.ROOT), icon = t?.let(::iconOf) ?: Bi.QuestionCircle, onBack = onBack) {
        if (state == GuideTopicState.NotFound) {
            Text(stringResource(R.string.guide_topic_not_found), fontSize = 13.sp, color = DeskColors.TextMuted)
        }
        if (t == null) return@SubPage
        t.paragraphs.forEach { Text(it, fontSize = 14.sp, lineHeight = 21.sp) }
        DetailSection(stringResource(R.string.guide_steps_upper)) {
            InsetBox {
                t.steps.forEachIndexed { i, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${i + 1}.", fontFamily = JetBrainsMonoFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeskColors.GreenText)
                        Text(step, fontSize = 13.sp, color = DeskColors.LogText)
                    }
                }
            }
        }
    }
}
