package com.iot.widget.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.UserRepository
import com.iot.widget.domain.RelativeTime
import com.iot.widget.domain.relativeTime
import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.initials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * UI layer · one row of the Users list (see [UsersViewModel]).
 */
data class UserRowUi(
    val id: String,
    val name: String,
    val initials: String,
    val role: Role,
    val active: Boolean,
    val lastLogin: RelativeTime,
    val isMe: Boolean,
)

data class UsersUiState(val rows: List<UserRowUi> = emptyList())

/** UI layer · the user list (spec §6.5); editing happens in [UserEditViewModel]. */
@HiltViewModel
class UsersViewModel @Inject constructor(
    users: UserRepository,
    session: SessionRepository,
    clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<UsersUiState> = combine(users.users, session.currentUser) { all, me ->
        val now = clock.instant()
        UsersUiState(
            all.map { u ->
                UserRowUi(
                    id = u.id,
                    name = u.name,
                    initials = initials(u.name),
                    role = u.role,
                    active = u.active,
                    lastLogin = relativeTime(now, u.lastLoginAtMillis?.let(Instant::ofEpochMilli), clock.zone),
                    isMe = u.id == me?.id,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsersUiState())
}
