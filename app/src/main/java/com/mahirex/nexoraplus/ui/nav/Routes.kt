package com.mahirex.nexoraplus.ui.nav

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val WEEKLY = "weekly"
    const val TASKS = "tasks"
    const val MORE = "more"
    const val LISTS = "lists"
    const val FORTUNE = "fortune"
    const val FOCUS = "focus"
    const val ACCOUNTS = "accounts"
    const val SEARCH = "search"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    val bottom = listOf(HOME, CALENDAR, WEEKLY, TASKS, MORE)
}

/** Switches between bottom-bar destinations keeping each one's state. */
fun NavController.goTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
