package com.iot.widget.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Activity-scoped holder of one ViewModelStore per signed-in account. It survives rotation
 * (it is itself a ViewModel), and drops the previous account's store as soon as another
 * account — or nobody — is signed in.
 */
class AccountScopes : ViewModel() {
    private var userId: String? = null
    private var store = ViewModelStore()

    fun storeFor(id: String): ViewModelStore {
        if (id != userId) {
            store.clear() // onCleared() for every ViewModel of the previous account
            store = ViewModelStore()
            userId = id
        }
        return store
    }

    /** Signed out: nothing of the last account keeps running behind the login screen. */
    fun clear() {
        store.clear()
        userId = null
    }

    override fun onCleared() = store.clear()
}

/**
 * UI layer · gives everything inside [content] a ViewModelStore that belongs to [userId].
 *
 * Why: the Navigation library keeps each screen's ViewModels in the store it finds in
 * `LocalViewModelStoreOwner` (normally the Activity's). Throwing the old account's screens away
 * (`key(user.id)`) does not clear that store, so their ViewModels — and the flows they collect —
 * would keep running for an account nobody uses any more. With a store per account, switching
 * account clears all of them at once. The default factory comes from the Activity, so
 * Hilt's `hiltViewModel()` works unchanged for the screens: each navigation entry brings its own
 * saved state. (A ViewModel created directly on this owner, outside a screen, would get the
 * Activity's saved state, shared across accounts — none is created that way.)
 */
@Composable
fun AccountScope(userId: String, content: @Composable () -> Unit) {
    val parent = checkNotNull(LocalViewModelStoreOwner.current)
    val scopes: AccountScopes = viewModel(viewModelStoreOwner = parent)
    val owner = remember(userId, parent) {
        val store = scopes.storeFor(userId)
        object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
            override val viewModelStore: ViewModelStore = store
            override val defaultViewModelProviderFactory: ViewModelProvider.Factory =
                (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory ?: ViewModelProvider.NewInstanceFactory()
            override val defaultViewModelCreationExtras: CreationExtras =
                (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
        }
    }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { content() }
}

/** Clears the last account's store (call when nobody is signed in). */
@Composable
fun ClearAccountScopes() {
    val parent = checkNotNull(LocalViewModelStoreOwner.current)
    val scopes: AccountScopes = viewModel(viewModelStoreOwner = parent)
    remember(scopes) { scopes.clear() }
}
