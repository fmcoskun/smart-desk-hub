package com.iot.widget.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.iot.widget.control.DeskCommands
import com.iot.widget.data.DeskRepository
import com.iot.widget.data.SessionRepository
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Scene
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Widget layer · what a tap on the widget does ([ToggleAction], [SceneAction]) and how widget
 * code reaches the app's singletons. Classes Android creates by itself (widgets, their
 * callbacks) cannot use @Inject; an @EntryPoint is Hilt's door for them.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun desk(): DeskRepository
    fun session(): SessionRepository
    fun commands(): DeskCommands
}

private fun commands(context: Context) = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).commands()

/** A tap on a switch row: the same rules as the Dashboard ([DeskCommands.toggle]). */
class ToggleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[DEVICE]?.let(DeviceId::valueOf) ?: return
        val on = parameters[ON] ?: return
        commands(context).toggle(id, on)
        // redraw this widget at once; SmartDeskApp's refresher (debounced) updates the others
        SmartDeskWidget().update(context, glanceId)
    }

    companion object {
        val DEVICE = ActionParameters.Key<String>("device")
        val ON = ActionParameters.Key<Boolean>("on")
    }
}

/** A tap on Work / Meet / Leave; the widget has no slider, so the LED level is sent at once. */
class SceneAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val scene = parameters[SCENE]?.let(Scene::valueOf) ?: return
        commands(context).scene(scene, publishDim = true)
        SmartDeskWidget().update(context, glanceId)
    }

    companion object {
        val SCENE = ActionParameters.Key<String>("scene")
    }
}
