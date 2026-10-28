package com.iot.widget.ui.navigation

import kotlinx.serialization.Serializable

/**
 * UI layer · navigation destinations. With type-safe Navigation a route is just a
 * @Serializable class; its properties are the screen's arguments.
 */
@Serializable
object DashboardRoute

/** `type` is a DetailType key ("bluetooth", "light", "rig", "privacy"). */
@Serializable
data class DetailRoute(val type: String)

@Serializable
object MoreRoute

@Serializable
object UsersRoute

/** `id` = the user to edit; null opens an empty form for a new user. */
@Serializable
data class UserEditRoute(val id: String? = null)

@Serializable
object AboutRoute

@Serializable
object DevicesRoute

@Serializable
data class DeviceDetailRoute(val id: String)

@Serializable
object AddDeviceRoute

/** `deviceId` pre-filters the list (the "View logs" shortcut of a device page). */
@Serializable
data class LogsRoute(val deviceId: String? = null)

@Serializable
data class LogDetailRoute(val id: Long)

@Serializable
object CamerasRoute

@Serializable
data class CameraRoute(val id: String)

/** `cameraId` pre-filters the list (the "Recordings" button of a camera page). */
@Serializable
data class RecordingsRoute(val cameraId: String? = null)

@Serializable
data class PlayerRoute(val id: Long)

@Serializable
object GuideRoute

@Serializable
data class GuideTopicRoute(val id: String)
