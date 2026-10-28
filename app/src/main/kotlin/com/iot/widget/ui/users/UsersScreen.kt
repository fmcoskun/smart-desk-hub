package com.iot.widget.ui.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.RelativeTime
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.Pill
import com.iot.widget.ui.components.PillStyle
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.components.neuRaised
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.login.InitialsTile
import com.iot.widget.ui.more.RolePill
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes

/** UI layer · the user list (spec §6.5, Admin only): tap a user to edit, "Add user" to create. */
@Composable
fun UsersScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: UsersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SubPage(title = stringResource(R.string.users_title_upper), icon = Bi.PeopleFill, onBack = onBack) {
        state.rows.forEach { row -> UserRow(row, onClick = { onEdit(row.id) }) }
        NeuChipButton(
            text = stringResource(R.string.user_add),
            icon = Bi.PlusLg,
            active = false,
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun UserRow(row: UserRowUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neuRaised(DeskShapes.Card)
            .clickable(role = SemanticsRole.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsTile(row.initials)
        Column(Modifier.weight(1f)) {
            val name = if (row.isMe) stringResource(R.string.user_you, row.name) else row.name
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                stringResource(R.string.user_last_login, relativeLabel(row.lastLogin)),
                fontSize = 11.sp,
                color = DeskColors.TextMuted,
            )
        }
        if (row.active) RolePill(row.role) else Pill(stringResource(R.string.pill_disabled), PillStyle.Off)
    }
}

/** "just now", "5 min ago", "2 hours ago", "yesterday", "3 days ago", "never" (strings.xml). */
@Composable
fun relativeLabel(time: RelativeTime): String = when (time) {
    RelativeTime.Never -> stringResource(R.string.rel_never)
    RelativeTime.Now -> stringResource(R.string.rel_now)
    is RelativeTime.MinutesAgo -> stringResource(R.string.rel_minutes, time.n)
    is RelativeTime.HoursAgo -> pluralStringResource(R.plurals.rel_hours, time.n, time.n)
    RelativeTime.Yesterday -> stringResource(R.string.rel_yesterday)
    is RelativeTime.DaysAgo -> stringResource(R.string.rel_days, time.n)
}
