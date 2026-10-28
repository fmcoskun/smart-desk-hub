package com.iot.widget.ui.users

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.iot.widget.domain.users.RuleViolation
import com.iot.widget.ui.components.NeuChipButton
import com.iot.widget.ui.components.NeuSegmented
import com.iot.widget.ui.components.SubPage
import com.iot.widget.ui.components.TextFieldNeu
import com.iot.widget.ui.detail.DetailSection
import com.iot.widget.ui.detail.OptionRow
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.icons.BiIcon
import com.iot.widget.ui.theme.DeskColors
import com.iot.widget.ui.theme.DeskDimens

/**
 * UI layer · add or edit a user. When the ViewModel reports saved/deleted, the page closes.
 * LaunchedEffect(key) runs its block once per key change — here: once when saving succeeds.
 */
@Composable
fun UserEditScreen(onDone: () -> Unit, viewModel: UserEditViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onDone()
    }
    UserEditContent(
        state = state,
        onBack = onDone,
        onName = viewModel::setName,
        onRole = viewModel::setRole,
        onActive = viewModel::setActive,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
    )
}

@Composable
fun UserEditContent(
    state: UserEditUiState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onRole: (Role) -> Unit,
    onActive: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    // rememberSaveable: the open confirmation survives a rotation
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(if (state.isNew) R.string.user_new_title_upper else R.string.user_edit_title_upper)
    SubPage(title = title, icon = Bi.PersonBadge, onBack = onBack) {
        if (!state.loaded) return@SubPage
        if (state.notFound) {
            Text(stringResource(R.string.rule_user_gone), fontSize = 13.sp, color = DeskColors.TextMuted)
            return@SubPage
        }
        DetailSection(stringResource(R.string.field_name_upper)) {
            TextFieldNeu(value = state.name, onValueChange = onName, label = stringResource(R.string.field_name))
        }
        DetailSection(stringResource(R.string.field_role_upper)) {
            NeuSegmented(
                options = listOf(
                    Role.ADMIN to stringResource(R.string.role_admin_label),
                    Role.OPERATOR to stringResource(R.string.role_operator_label),
                    Role.GUEST to stringResource(R.string.role_guest_label),
                ),
                selected = state.role,
                onSelect = onRole,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        DetailSection(null) {
            OptionRow(stringResource(R.string.option_active), state.active, onActive)
        }
        state.error?.let { Text(stringResource(it.messageRes()), fontSize = 12.sp, color = DeskColors.Red) }
        NeuChipButton(
            text = stringResource(R.string.action_save),
            icon = null,
            active = true,
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!state.isNew && !state.isMe) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = DeskDimens.MinTouch)
                    .clickable(role = SemanticsRole.Button) { confirmDelete = true }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BiIcon(Bi.Trash, tint = DeskColors.Red, size = 14.sp)
                Text(stringResource(R.string.action_delete_user), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeskColors.Red)
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = DeskColors.Bg,
            title = { Text(stringResource(R.string.delete_confirm, state.name), fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.action_delete), color = DeskColors.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel), color = DeskColors.TextMuted)
                }
            },
        )
    }
}

/** One user-facing sentence per rule (strings.xml); also used by the device page. */
fun RuleViolation.messageRes(): Int = when (this) {
    RuleViolation.NAME_EMPTY -> R.string.rule_name_empty
    RuleViolation.NAME_TOO_LONG -> R.string.rule_name_too_long
    RuleViolation.NAME_TAKEN -> R.string.rule_name_taken
    RuleViolation.SELF_DELETE -> R.string.rule_self_delete
    RuleViolation.SELF_DEACTIVATE -> R.string.rule_self_deactivate
    RuleViolation.SELF_DEMOTE -> R.string.rule_self_demote
    RuleViolation.LAST_ADMIN -> R.string.rule_last_admin
    RuleViolation.NOT_ALLOWED -> R.string.rule_not_allowed
    RuleViolation.USER_GONE -> R.string.rule_user_gone
}
