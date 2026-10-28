package com.iot.widget.ui.guide

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.guide.GuideTopic
import com.iot.widget.ui.components.IconBox
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.components.TextFieldNeu
import com.iot.widget.ui.detail.InsetBox
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors

/** Callbacks of the guide list, bundled so tests can pass [NONE]. */
data class GuideActions(
    val onBack: () -> Unit,
    val onQuery: (String) -> Unit,
    val onOpen: (String) -> Unit,
    val onTour: () -> Unit,
) {
    companion object {
        val NONE = GuideActions({}, {}, {}, {})
    }
}

/** UI layer · User Guide: search + topic list (spec §6.4). */
@Composable
fun GuideScreen(onBack: () -> Unit, onOpen: (String) -> Unit, viewModel: GuideViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    GuideContent(state.copy(query = query), GuideActions(onBack, viewModel::setQuery, onOpen, viewModel::showTour))
}

@Composable
fun GuideContent(state: GuideUiState, actions: GuideActions) {
    SubPage(title = stringResource(R.string.guide_title_upper), icon = Bi.QuestionCircle, onBack = actions.onBack) {
        TextFieldNeu(value = state.query, onValueChange = actions.onQuery, label = stringResource(R.string.guide_search), capitalization = KeyboardCapitalization.None)
        NeuChipButton(stringResource(R.string.guide_show_tour), Bi.PlayFill, active = false, onClick = actions.onTour, modifier = Modifier.fillMaxWidth())
        if (state.loaded && state.topics.isEmpty()) {
            Text(stringResource(R.string.guide_no_match), fontSize = 13.sp, color = DeskColors.TextMuted)
        } else {
            InsetBox { state.topics.forEach { TopicRow(it) { actions.onOpen(it.id) } } }
        }
    }
}

@Composable
private fun TopicRow(topic: GuideTopic, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBox(iconOf(topic))
        Column(Modifier.weight(1f)) {
            Text(topic.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(topic.summary, fontSize = 12.sp, color = DeskColors.TextMuted)
        }
        BiIcon(Bi.ChevronRight, tint = DeskColors.TextMuted, size = 12.sp)
    }
}

/** The topic's icon by name; an unknown name falls back to "?" instead of crashing. */
internal fun iconOf(topic: GuideTopic): Bi = Bi.entries.firstOrNull { it.name == topic.icon } ?: Bi.QuestionCircle
