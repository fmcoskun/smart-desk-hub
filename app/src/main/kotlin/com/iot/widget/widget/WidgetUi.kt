package com.iot.widget.widget

import com.iot.widget.domain.DeskState
import com.iot.widget.domain.DeviceId
import com.iot.widget.domain.Scene
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.User
import com.iot.widget.domain.users.can

/** Who is looking: may switch (CONTROL), Guest (view only), or nobody signed in. */
enum class WidgetMode { CONTROL, GUEST, SIGNED_OUT }

data class WidgetRow(val id: DeviceId, val name: String, val isOn: Boolean, val online: Boolean)

data class WidgetUi(val rows: List<WidgetRow>, val activeScene: Scene?, val mode: WidgetMode)

/**
 * Widget layer · the desk as the home-screen widget shows it. A pure function, so what the
 * widget draws for each role and device state is unit-tested without a launcher.
 */
fun widgetUi(desk: DeskState, user: User?): WidgetUi = WidgetUi(
    // the four dashboard switches, in dashboard order, with their (renamable) names
    rows = DeviceId.entries.mapNotNull { id ->
        desk.devices[id]?.let { WidgetRow(id, it.name, it.isOn, it.online) }
    },
    activeScene = desk.settings.activeScene,
    mode = when {
        user == null -> WidgetMode.SIGNED_OUT
        user.can(Permission.CONTROL_DEVICES) -> WidgetMode.CONTROL
        else -> WidgetMode.GUEST
    },
)
