package com.mahirex.nexoraplus.ui.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.ui.designsystem.Motion
import com.mahirex.nexoraplus.ui.screens.about.AboutScreen
import com.mahirex.nexoraplus.ui.screens.accounts.AccountsScreen
import com.mahirex.nexoraplus.ui.screens.calendar.CalendarScreen
import com.mahirex.nexoraplus.ui.screens.focus.FocusScreen
import com.mahirex.nexoraplus.ui.screens.fortune.FortuneScreen
import com.mahirex.nexoraplus.ui.screens.home.HomeScreen
import com.mahirex.nexoraplus.ui.screens.lists.ListsScreen
import com.mahirex.nexoraplus.ui.screens.more.MoreScreen
import com.mahirex.nexoraplus.ui.screens.search.SearchScreen
import com.mahirex.nexoraplus.ui.screens.settings.SettingsScreen
import com.mahirex.nexoraplus.ui.screens.stats.StatsScreen
import com.mahirex.nexoraplus.ui.screens.tasks.TasksScreen
import com.mahirex.nexoraplus.ui.screens.weekly.WeeklyScreen

private data class BottomItem(val route: String, val label: Int, val icon: ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.HOME, R.string.nav_home, Icons.Rounded.Home),
    BottomItem(Routes.CALENDAR, R.string.nav_calendar, Icons.Rounded.CalendarMonth),
    BottomItem(Routes.WEEKLY, R.string.nav_weekly, Icons.Rounded.ViewWeek),
    BottomItem(Routes.TASKS, R.string.nav_tasks, Icons.Rounded.TaskAlt),
    BottomItem(Routes.MORE, R.string.nav_more, Icons.Rounded.MoreHoriz)
)

@Composable
fun NexoraNavHost(factory: ViewModelProvider.Factory) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in Routes.bottom

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = route == item.route,
                            onClick = { if (route != item.route) nav.goTop(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label), maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(Motion.NavMs)) + scaleIn(tween(Motion.NavMs), initialScale = 0.97f) },
            exitTransition = { fadeOut(tween(Motion.ShortMs)) },
            popEnterTransition = { fadeIn(tween(Motion.NavMs)) + scaleIn(tween(Motion.NavMs), initialScale = 0.97f) },
            popExitTransition = { fadeOut(tween(Motion.ShortMs)) }
        ) {
            composable(Routes.HOME) { HomeScreen(nav, factory) }
            composable(Routes.CALENDAR) { CalendarScreen(nav, factory) }
            composable(Routes.WEEKLY) { WeeklyScreen(nav, factory) }
            composable(Routes.TASKS) { TasksScreen(nav, factory) }
            composable(Routes.MORE) { MoreScreen(nav) }
            composable(Routes.LISTS) { ListsScreen(nav, factory) }
            composable(Routes.FORTUNE) { FortuneScreen(nav, factory) }
            composable(Routes.FOCUS) { FocusScreen(nav, factory) }
            composable(Routes.ACCOUNTS) { AccountsScreen(nav, factory) }
            composable(Routes.SEARCH) { SearchScreen(nav, factory) }
            composable(Routes.STATS) { StatsScreen(nav, factory) }
            composable(Routes.SETTINGS) { SettingsScreen(nav, factory) }
            composable(Routes.ABOUT) { AboutScreen(nav) }
        }
    }
}
