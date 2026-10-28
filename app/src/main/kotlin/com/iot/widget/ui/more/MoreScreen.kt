package com.iot.widget.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.users.Role
import com.iot.widget.ui.components.Pill
import com.iot.widget.ui.components.PillStyle
import com.iot.widget.ui.components.SectionLabel
import com.iot.widget.ui.components.neuRaised
import com.iot.widget.ui.components.windowCard
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.login.InitialsTile
import com.iot.widget.ui.login.roleDescription
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskDimens
import com.iot.widget.ui.theme.DeskShapes

/** UI layer · the "More" tab (spec §5). */
@Composable
fun MoreScreen(onUsers: () -> Unit, onGuide: () -> Unit, onAbout: () -> Unit, viewModel: MoreViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MoreContent(state, onUsers = onUsers, onSwitchUser = viewModel::switchUser, onAbout = onAbout, onGuide = onGuide)
}

@Composable
fun MoreContent(state: MoreUiState, onUsers: () -> Unit, onSwitchUser: () -> Unit, onAbout: () -> Unit, onGuide: () -> Unit = {}) {
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Signed-in user
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                InitialsTile(state.initials)
                Column(Modifier.weight(1f)) {
                    Text(state.userName, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text(stringResource(roleDescription(state.role)), fontSize = 11.sp, color = DeskColors.TextMuted)
                }
                RolePill(state.role)
            }
            SectionLabel(stringResource(R.string.more_section_account))
            if (state.canManageUsers) MoreRow(Bi.PeopleFill, stringResource(R.string.more_users), onUsers)
            MoreRow(Bi.BoxArrowRight, stringResource(R.string.more_switch_user), onSwitchUser)
            SectionLabel(stringResource(R.string.more_section_app))
            MoreRow(Bi.QuestionCircle, stringResource(R.string.more_guide), onGuide)
            MoreRow(Bi.InfoCircle, stringResource(R.string.more_about), onAbout)
        }
    }
}

/** ADMIN in green, the other roles neutral. */
@Composable
fun RolePill(role: Role) = Pill(
    text = stringResource(
        when (role) {
            Role.ADMIN -> R.string.role_admin
            Role.OPERATOR -> R.string.role_operator
            Role.GUEST -> R.string.role_guest
        },
    ),
    style = if (role == Role.ADMIN) PillStyle.On else PillStyle.Neutral,
)

@Composable
private fun MoreRow(icon: Bi, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = DeskDimens.MinTouch)
            .neuRaised(DeskShapes.Card)
            .clickable(role = SemanticsRole.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BiIcon(icon, tint = DeskColors.IconDefault, size = 16.sp)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        BiIcon(Bi.ChevronRight, tint = DeskColors.TextMuted, size = 13.sp)
    }
}
