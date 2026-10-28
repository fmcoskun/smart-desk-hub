package com.iot.widget.ui.shell

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Switching account replaces the signed-in UI. Its back-stack ViewModels live outside the
 * composition, so they must be cleared explicitly or they keep running for the old account.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountScopeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    class Counting : ViewModel() {
        override fun onCleared() {
            cleared++
        }

        companion object {
            var cleared = 0
        }
    }

    @Test
    fun switchUserClearsOldBackStack() {
        Counting.cleared = 0
        var account by mutableStateOf("ayse")
        rule.setContent {
            AccountScope(account) {
                val nav = rememberNavController()
                NavHost(nav, startDestination = "a") { composable("a") { viewModel<Counting>() } }
            }
        }
        rule.waitForIdle()
        assertEquals(0, Counting.cleared)
        account = "guest" // "Switch user"
        rule.waitForIdle()
        assertEquals(1, Counting.cleared)
    }
}
