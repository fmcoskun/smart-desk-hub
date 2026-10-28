package com.iot.widget.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * "Deleted → close the page" can arrive while the page is not RESUMED (e.g. during the slide
 * animation). The navigation callback ignores calls then, so the close must wait, not be lost.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CloseWhenTest {

    @get:Rule
    val rule = createComposeRule()

    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun deletedWhileNotResumedStillCloses() {
        val owner = Owner().apply { registry.currentState = Lifecycle.State.STARTED }
        var closed = 0
        rule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                // like dropUnlessResumed { popBackStack() }: ignored unless RESUMED
                CloseWhen(true) { if (owner.lifecycle.currentState == Lifecycle.State.RESUMED) closed++ }
            }
        }
        rule.waitForIdle()
        assertEquals(0, closed)
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        rule.waitForIdle()
        assertEquals(1, closed)
    }
}
