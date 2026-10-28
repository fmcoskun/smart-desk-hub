package com.iot.widget.ui.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.log.EventLog
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.can
import com.iot.widget.domain.users.initials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI layer · state of the More tab: who is signed in and which rows they may see (see [MoreViewModel]).
 */
data class MoreUiState(
    val userName: String = "",
    val initials: String = "",
    val role: Role = Role.GUEST,
    val canManageUsers: Boolean = false,
)

/** UI layer · the "More" tab: who is signed in, the user manager (Admin only), switch user. */
@HiltViewModel
class MoreViewModel @Inject constructor(
    private val session: SessionRepository,
    private val eventLog: EventLog,
) : ViewModel() {

    val uiState: StateFlow<MoreUiState> = session.currentUser
        .map { user ->
            if (user == null) MoreUiState() else MoreUiState(user.name, initials(user.name), user.role, user.can(Permission.MANAGE_USERS))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoreUiState())

    /** AppRoot sees the empty session and shows Login; no navigation needed here. */
    fun switchUser() {
        viewModelScope.launch {
            session.currentUser.first()?.let { eventLog.record(it.name, "signed out") }
            session.signOut()
        }
    }
}
