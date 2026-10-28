package com.iot.widget.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.iot.widget.data.OnboardingRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.domain.users.User
import com.iot.widget.ui.login.LoginScreen
import com.iot.widget.ui.onboarding.OnboardingContent
import com.iot.widget.ui.theme.DeskColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * UI layer · the root of the app: what to show for the current session (see [AppRoot]).
 */
sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState

    /** Signed in, but the welcome tour has not been shown on this device yet. */
    data class Onboarding(val user: User) : SessionState
    /** [replayTour]: "Show welcome tour" was tapped; the tour lies on top of the app. */
    data class SignedIn(val user: User, val replayTour: Boolean = false) : SessionState
}

/** UI layer · turns the session and the "tour seen" flag into the states the root screen needs. */
@HiltViewModel
class SessionViewModel @Inject constructor(
    session: SessionRepository,
    private val onboarding: OnboardingRepository,
) : ViewModel() {
    // runningFold: each new state may depend on the previous one. "Tour not seen" while the
    // same user is already inside the app means a replay (shown on top), not a first run.
    val state: StateFlow<SessionState> = combine(session.currentUser, onboarding.seen) { user, seen -> user to seen }
        .runningFold<Pair<User?, Boolean>, SessionState>(SessionState.Loading) { previous, (user, seen) ->
            when {
                user == null -> SessionState.SignedOut
                seen -> SessionState.SignedIn(user)
                previous is SessionState.SignedIn && previous.user.id == user.id -> SessionState.SignedIn(user, replayTour = true)
                else -> SessionState.Onboarding(user)
            }
        }
        // Eagerly, not WhileSubscribed: the fold remembers the previous state (replay vs first
        // run). Restarting it after the app was in the background would begin again at Loading,
        // drop the signed-in screens and lose their back stack. This ViewModel lives as long as
        // the Activity, so keeping it running costs nothing.
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    /** Skip or Get started: never show the tour again on this device (until "Show welcome tour"). */
    fun finishOnboarding() {
        viewModelScope.launch { onboarding.markSeen() }
    }
}

/**
 * UI layer · the top of the screen tree: Login when nobody is signed in, the welcome tour the
 * first time on this device, the app otherwise.
 * Signing in or out only changes the session; this composable reacts to it, so no screen has
 * to "navigate to Login" by hand.
 */
@Composable
fun AppRoot(viewModel: SessionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        SessionState.Loading -> Box(Modifier.fillMaxSize().background(DeskColors.Bg))
        SessionState.SignedOut -> {
            ClearAccountScopes() // the last account's screens stop behind the login screen
            LoginScreen()
        }
        is SessionState.Onboarding -> OnboardingContent(onFinish = viewModel::finishOnboarding)
        is SessionState.SignedIn -> Box(Modifier.fillMaxSize()) {
            // stays composed under a replayed tour (Back to where you were), but hidden from
            // TalkBack meanwhile, as it is hidden from touch
            Box(if (s.replayTour) Modifier.clearAndSetSemantics {} else Modifier) { SignedInContent(s.user) }
            if (s.replayTour) OnboardingContent(onFinish = viewModel::finishOnboarding, replay = true)
        }
    }
}

@Composable
private fun SignedInContent(user: User) {
    // key(user.id): switching to another account starts the app UI fresh (new back stack);
    // AccountScope also clears the old account's screen ViewModels (see AccountScope.kt)
    AccountScope(user.id) { key(user.id) { MainScaffold(user) } }
}
