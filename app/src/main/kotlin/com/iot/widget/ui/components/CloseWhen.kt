package com.iot.widget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.first

/**
 * UI layer · closes a page once [condition] becomes true (e.g. "the device was deleted").
 *
 * The back callbacks are wrapped in `dropUnlessResumed`, which ignores a call made while the
 * page is not RESUMED — during its slide-in animation, for example. Calling them right away
 * could therefore be lost and leave an empty page behind, so this waits until the page is
 * RESUMED before closing.
 */
@Composable
fun CloseWhen(condition: Boolean, onClose: () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(condition, lifecycle) {
        if (!condition) return@LaunchedEffect
        lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
        close()
    }
}
