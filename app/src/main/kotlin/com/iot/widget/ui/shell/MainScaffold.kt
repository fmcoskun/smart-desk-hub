package com.iot.widget.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iot.widget.R
import com.iot.widget.ui.icons.Bi
import com.iot.widget.ui.navigation.AppNavHost
import com.iot.widget.ui.navigation.DashboardRoute
import com.iot.widget.domain.users.Permission
import com.iot.widget.domain.users.User
import com.iot.widget.domain.users.can
import com.iot.widget.ui.navigation.CamerasRoute
import com.iot.widget.ui.navigation.DevicesRoute
import com.iot.widget.ui.navigation.LogsRoute
import com.iot.widget.ui.navigation.MoreRoute
import com.iot.widget.ui.navigation.UsersRoute
import com.iot.widget.ui.theme.DeskColors

/**
 * UI layer · the signed-in app: screens on top, the bottom bar below. The bar shows only on
 * tab-level pages; sub-pages (device details, About, user editor) take the full screen.
 *
 */
@Composable
fun MainScaffold(user: User) {
    val navController = rememberNavController()
    // currentBackStackEntryAsState: recomposes whenever the current page changes
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val onDashboard = destination?.hasRoute<DashboardRoute>() == true
    val onCameras = destination?.hasRoute<CamerasRoute>() == true
    val onDevices = destination?.hasRoute<DevicesRoute>() == true
    val onLogs = destination?.hasRoute<LogsRoute>() == true
    val canViewLogs = user.can(Permission.VIEW_LOGS)
    val inMoreSection = destination?.hasRoute<MoreRoute>() == true || destination?.hasRoute<UsersRoute>() == true
    val showBar = onDashboard || onCameras || onDevices || onLogs || inMoreSection

    // Standard tab switching: one copy of each tab; leaving a tab saves where you were in it
    // (saveState) and coming back restores that page (restoreState), e.g. More > Users stays open
    fun openTab(route: Any) = navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DeskColors.Bg),
    ) {
        Box(
            Modifier
                .weight(1f)
                // with the bar visible, the bar (not the page) sits above the system navigation bar
                .then(if (showBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
        ) {
            AppNavHost(navController)
        }
        if (showBar) {
            NeuBottomBar(
                tabs = listOfNotNull(
                    BottomTab(stringResource(R.string.tab_dashboard), Bi.Grid1x2Fill, onDashboard) { openTab(DashboardRoute) },
                    BottomTab(stringResource(R.string.tab_cameras), Bi.CameraVideoFill, onCameras) { openTab(CamerasRoute) },
                    BottomTab(stringResource(R.string.tab_devices), Bi.Cpu, onDevices) { openTab(DevicesRoute) },
                    // Logs only for roles that may read them (Guest has no Logs tab, spec §5)
                    if (canViewLogs) BottomTab(stringResource(R.string.tab_logs), Bi.JournalText, onLogs) { openTab(LogsRoute()) } else null,
                    BottomTab(stringResource(R.string.tab_more), Bi.ThreeDots, inMoreSection) { openTab(MoreRoute) },
                ),
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            )
        }
    }
}
