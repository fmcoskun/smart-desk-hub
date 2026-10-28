package com.iot.widget.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.iot.widget.ui.cameras.CameraScreen
import com.iot.widget.ui.cameras.CamerasScreen
import com.iot.widget.ui.dashboard.DashboardScreen
import com.iot.widget.ui.detail.DetailScreen
import com.iot.widget.ui.devices.AddDeviceScreen
import com.iot.widget.ui.devices.DeviceDetailScreen
import com.iot.widget.ui.devices.DevicesScreen
import com.iot.widget.ui.guide.GuideScreen
import com.iot.widget.ui.guide.GuideTopicScreen
import com.iot.widget.ui.logs.LogDetailScreen
import com.iot.widget.ui.logs.LogsScreen
import com.iot.widget.ui.more.AboutScreen
import com.iot.widget.ui.recordings.PlayerScreen
import com.iot.widget.ui.recordings.RecordingsScreen
import com.iot.widget.ui.more.MoreScreen
import com.iot.widget.ui.users.UserEditScreen
import com.iot.widget.ui.users.UsersScreen

/** Sub-pages slide in from the right in 300 ms (CSS `cubic-bezier(0.4, 0, 0.2, 1)` = FastOutSlowIn). */
private const val SLIDE_MS = 300

/**
 * UI layer · the navigation graph of the signed-in app: which route shows which screen. The
 * NavController keeps the back stack, so the Android back button/gesture pops back by itself.
 *
 * Transitions: tab roots (Dashboard, Cameras, Devices, Logs, More) never move; a sub-page slides in over them from the
 * right and slides back out on "Back" — the motion of the reference's sliding panel.
 */
@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = DashboardRoute,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
    ) {
        composable<DashboardRoute> {
            // Each destination is a LifecycleOwner: it is RESUMED only while fully on screen and
            // not animating. Ignoring taps otherwise stops a quick double tap from opening two
            // pages, or from popping one page too many (which would leave a blank screen).
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DashboardScreen(onOpenDetail = { type ->
                if (lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(DetailRoute(type.key))
            })
        }
        subPage<DetailRoute> {
            // dropUnlessResumed = the same guard as above, as a ready-made helper
            DetailScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<DevicesRoute> {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DevicesScreen(
                onOpen = { id -> if (lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(DeviceDetailRoute(id)) },
                onAdd = dropUnlessResumed { navController.navigate(AddDeviceRoute) },
            )
        }
        subPage<DeviceDetailRoute> {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            DeviceDetailScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onViewLogs = { id ->
                    // a tab switch, not a sub-page: drop the Devices stack (no saveState) so the
                    // Devices tab opens its list again instead of this filtered Logs page
                    if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                        navController.navigate(LogsRoute(id)) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        subPage<AddDeviceRoute> {
            AddDeviceScreen(onDone = dropUnlessResumed { navController.popBackStack() })
        }
        composable<CamerasRoute> {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            CamerasScreen(
                onOpen = { id -> if (lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(CameraRoute(id)) },
                onRecordings = dropUnlessResumed { navController.navigate(RecordingsRoute()) },
            )
        }
        subPage<CameraRoute> {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            CameraScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onRecordings = { id -> if (lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(RecordingsRoute(id)) },
            )
        }
        subPage<RecordingsRoute> {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            RecordingsScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onOpen = { id -> if (lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(PlayerRoute(id)) },
            )
        }
        subPage<PlayerRoute> {
            PlayerScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<LogsRoute> {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            LogsScreen(onOpen = { id -> if (lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(LogDetailRoute(id)) })
        }
        subPage<LogDetailRoute> {
            LogDetailScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<MoreRoute> {
            MoreScreen(
                onUsers = dropUnlessResumed { navController.navigate(UsersRoute) },
                onGuide = dropUnlessResumed { navController.navigate(GuideRoute) },
                onAbout = dropUnlessResumed { navController.navigate(AboutRoute) },
            )
        }
        subPage<GuideRoute> {
            val lifecycle = LocalLifecycleOwner.current.lifecycle
            GuideScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onOpen = { id -> if (lifecycle.currentState == Lifecycle.State.RESUMED) navController.navigate(GuideTopicRoute(id)) },
            )
        }
        subPage<GuideTopicRoute> {
            GuideTopicScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        subPage<AboutRoute> {
            AboutScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        subPage<UsersRoute> {
            UsersScreen(
                onBack = dropUnlessResumed { navController.popBackStack() },
                onAdd = dropUnlessResumed { navController.navigate(UserEditRoute()) },
                onEdit = { id -> if (navController.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) navController.navigate(UserEditRoute(id)) },
            )
        }
        subPage<UserEditRoute> {
            UserEditScreen(onDone = dropUnlessResumed { navController.popBackStack() })
        }
    }
}

/** A destination that slides in from the right (detail pages, About, Users, user editor). */
private inline fun <reified T : Any> NavGraphBuilder.subPage(
    noinline content: @Composable (androidx.navigation.NavBackStackEntry) -> Unit,
) = composable<T>(
    enterTransition = { slideInHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it } },
    popExitTransition = { slideOutHorizontally(tween(SLIDE_MS, easing = FastOutSlowInEasing)) { it } },
) { entry -> content(entry) }
