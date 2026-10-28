package com.iot.widget.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.UserRepository
import com.iot.widget.data.log.EventLog
import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.initials
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI layer · one demo account card on the sign-in screen (see [LoginViewModel]).
 */
data class AccountUi(val id: String, val name: String, val role: Role, val active: Boolean, val initials: String)

data class LoginUiState(val accounts: List<AccountUi> = emptyList())

/** UI layer · the demo account picker (spec §6.3). Signing in only stores who is signed in. */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val users: UserRepository,
    private val session: SessionRepository,
    private val eventLog: EventLog,
) : ViewModel() {

    val uiState: StateFlow<LoginUiState> = users.users
        .map { all -> LoginUiState(all.map { AccountUi(it.id, it.name, it.role, it.active, initials(it.name)) }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LoginUiState())

    fun signIn(id: String) {
        viewModelScope.launch {
            // AppRoot observes the session and switches to the main screens by itself
            session.signIn(id)?.let { eventLog.record(it.name, "signed in") }
        }
    }
}
