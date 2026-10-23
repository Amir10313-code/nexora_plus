package com.mahirex.nexoraplus.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.screens.accounts.AccountsViewModel
import com.mahirex.nexoraplus.ui.screens.calendar.CalendarViewModel
import com.mahirex.nexoraplus.ui.screens.focus.FocusViewModel
import com.mahirex.nexoraplus.ui.screens.fortune.FortuneViewModel
import com.mahirex.nexoraplus.ui.screens.home.HomeViewModel
import com.mahirex.nexoraplus.ui.screens.lists.ListsViewModel
import com.mahirex.nexoraplus.ui.screens.search.SearchViewModel
import com.mahirex.nexoraplus.ui.screens.settings.SettingsViewModel
import com.mahirex.nexoraplus.ui.screens.stats.StatsViewModel
import com.mahirex.nexoraplus.ui.screens.tasks.TasksViewModel
import com.mahirex.nexoraplus.ui.screens.weekly.WeeklyViewModel

/** Hand-written factory: every ViewModel receives the [AppContainer]. */
class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container)
        modelClass.isAssignableFrom(CalendarViewModel::class.java) -> CalendarViewModel(container)
        modelClass.isAssignableFrom(WeeklyViewModel::class.java) -> WeeklyViewModel(container)
        modelClass.isAssignableFrom(TasksViewModel::class.java) -> TasksViewModel(container)
        modelClass.isAssignableFrom(ListsViewModel::class.java) -> ListsViewModel(container)
        modelClass.isAssignableFrom(FortuneViewModel::class.java) -> FortuneViewModel(container)
        modelClass.isAssignableFrom(FocusViewModel::class.java) -> FocusViewModel(container)
        modelClass.isAssignableFrom(AccountsViewModel::class.java) -> AccountsViewModel(container)
        modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(container)
        modelClass.isAssignableFrom(StatsViewModel::class.java) -> StatsViewModel(container)
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container)
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    } as T
}
