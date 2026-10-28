package com.iot.widget.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iot.widget.R
import com.iot.widget.domain.users.Role
import com.iot.widget.ui.components.neuRaised
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskShapes

/** UI layer · Login (spec §6.3): pick a demo account; disabled accounts are shown but not selectable. */
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LoginContent(state, onPick = viewModel::signIn)
}

@Composable
fun LoginContent(state: LoginUiState, onPick: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(DeskColors.Bg)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .neuRaised(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) { BiIcon(Bi.Grid1x2Fill, tint = DeskColors.Green, size = 24.sp) }
            Text(stringResource(R.string.app_name), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.login_subtitle), fontSize = 13.sp, color = DeskColors.TextMuted)
            Spacer(Modifier.height(4.dp))
            state.accounts.forEach { AccountCard(it, onPick) }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.login_footer),
                fontSize = 11.sp,
                color = DeskColors.TextMuted,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AccountCard(account: AccountUi, onPick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .neuRaised(DeskShapes.Card)
            // alpha after the shadows: an alpha layer clips to its bounds and would cut them off
            .alpha(if (account.active) 1f else 0.4f)
            // enabled = false: the card is announced as disabled and ignores taps
            .clickable(enabled = account.active, role = SemanticsRole.Button) { onPick(account.id) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InitialsTile(account.initials)
        Column(Modifier.weight(1f)) {
            Text(account.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(
                if (account.active) stringResource(roleDescription(account.role)) else stringResource(R.string.login_disabled),
                fontSize = 11.sp,
                color = DeskColors.TextMuted,
            )
        }
        BiIcon(Bi.ChevronRight, tint = DeskColors.TextMuted, size = 14.sp)
    }
}

/** Round-cornered tile with a person's initials, used on Login, More and Users. */
@Composable
fun InitialsTile(initials: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(36.dp)
            .neuRaised(RoundedCornerShape(11.dp), distance = 3.dp, blur = 6.dp),
        contentAlignment = Alignment.Center,
    ) { Text(initials, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DeskColors.GreenText) }
}

fun roleDescription(role: Role): Int = when (role) {
    Role.ADMIN -> R.string.role_admin_description
    Role.OPERATOR -> R.string.role_operator_description
    Role.GUEST -> R.string.role_guest_description
}
