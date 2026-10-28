package com.iot.widget.ui.users

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.SessionRepository
import com.iot.widget.data.UserRepository
import com.iot.widget.data.log.EventLog
import com.iot.widget.domain.Names
import com.iot.widget.domain.users.Role
import com.iot.widget.domain.users.RuleResult
import com.iot.widget.domain.users.RuleViolation
import com.iot.widget.domain.users.User
import com.iot.widget.domain.users.UserRules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** The editor's draft plus what happened to it. `saved`/`deleted` tell the screen to close. */
data class UserEditUiState(
    val loaded: Boolean = false,
    val name: String = "",
    val role: Role = Role.OPERATOR,
    val active: Boolean = true,
    val isNew: Boolean = true,
    val isMe: Boolean = false,
    val error: RuleViolation? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val notFound: Boolean = false,
)

/**
 * UI layer · add / edit / delete one user. The form is a *draft* in this ViewModel; nothing is
 * written until Save, and only if [UserRules] allow it. The rules are checked here as well as
 * hidden in the UI, so a non-admin can never change users even if they reach this screen.
 */
@HiltViewModel
class UserEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val users: UserRepository,
    private val session: SessionRepository,
    private val eventLog: EventLog,
) : ViewModel() {

    /** Route argument `id` (UserEditRoute); null = a new user. */
    private val editedId: String? = savedStateHandle.get<String>("id")

    /** Chosen once: saving the same new user twice must write the same row, never a second one. */
    private val newUserId: String = UUID.randomUUID().toString()

    /** The save or delete in progress; a second tap meanwhile is ignored (no duplicate users). */
    private var pending: Job? = null

    // A plain MutableStateFlow: typing must update the field immediately, without a database trip
    private val _uiState = MutableStateFlow(UserEditUiState(isNew = editedId == null))
    val uiState: StateFlow<UserEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val me = session.currentUser.first()
            val user = editedId?.let { users.byId(it) }
            _uiState.update {
                if (editedId != null && user == null) {
                    // an old link, or a user deleted on another screen
                    it.copy(loaded = true, notFound = true)
                } else if (user == null) {
                    it.copy(loaded = true)
                } else {
                    it.copy(loaded = true, name = user.name, role = user.role, active = user.active, isMe = user.id == me?.id)
                }
            }
        }
    }

    // `update { }` changes a StateFlow atomically; any edit also clears an old error message
    fun setName(name: String) = _uiState.update { it.copy(name = name, error = null) }
    fun setRole(role: Role) = _uiState.update { it.copy(role = role, error = null) }
    fun setActive(active: Boolean) = _uiState.update { it.copy(active = active, error = null) }

    fun save() {
        if (isBusyOrDone()) return
        pending = viewModelScope.launch {
            val actor = session.currentUser.first() ?: return@launch reject(RuleViolation.NOT_ALLOWED)
            val all = users.all()
            val before = editedId?.let { id -> all.firstOrNull { it.id == id } }
            // editing someone who was deleted meanwhile: saving would silently create a new user
            if (editedId != null && before == null) return@launch reject(RuleViolation.USER_GONE)
            val draft = _uiState.value
            val after = User(
                id = before?.id ?: newUserId,
                name = Names.clean(draft.name),
                role = draft.role,
                active = draft.active,
                lastLoginAtMillis = before?.lastLoginAtMillis,
            )
            when (val result = UserRules.checkSave(actor, before, after, all)) {
                is RuleResult.Rejected -> reject(result.reason)
                RuleResult.Ok -> {
                    users.save(after)
                    eventLog.record(actor.name, (if (before == null) "created user " else "updated user ") + after.name)
                    _uiState.update { it.copy(saved = true) }
                }
            }
        }
    }

    fun delete() {
        if (isBusyOrDone()) return
        pending = viewModelScope.launch {
            val actor = session.currentUser.first() ?: return@launch reject(RuleViolation.NOT_ALLOWED)
            val all = users.all()
            val target = all.firstOrNull { it.id == editedId } ?: return@launch
            when (val result = UserRules.checkDelete(actor, target, all)) {
                is RuleResult.Rejected -> reject(result.reason)
                RuleResult.Ok -> {
                    users.delete(target.id)
                    eventLog.record(actor.name, "deleted user " + target.name)
                    _uiState.update { it.copy(deleted = true) }
                }
            }
        }
    }

    private fun isBusyOrDone(): Boolean =
        pending?.isActive == true || _uiState.value.saved || _uiState.value.deleted

    private fun reject(reason: RuleViolation) = _uiState.update { it.copy(error = reason) }
}
