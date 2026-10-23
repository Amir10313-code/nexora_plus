package com.mahirex.nexoraplus.ui.screens.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.data.repo.Streaks
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.SectionTitle
import com.mahirex.nexoraplus.ui.components.loc
import com.mahirex.nexoraplus.ui.designsystem.Space
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

private class Planner(
    val tasks: List<com.mahirex.nexoraplus.data.local.entity.TaskEntity>,
    val goals: List<com.mahirex.nexoraplus.data.local.entity.GoalEntity>,
    val habits: List<com.mahirex.nexoraplus.data.local.entity.HabitEntity>,
    val logs: List<com.mahirex.nexoraplus.data.local.entity.HabitLogEntity>
)

data class StatsState(
    val tasksTotal: Int = 0,
    val tasksDone: Int = 0,
    val tasksOverdue: Int = 0,
    val goalsTotal: Int = 0,
    val goalsDone: Int = 0,
    val habitsCount: Int = 0,
    val bestCurrentStreak: Int = 0,
    val longestStreak: Int = 0,
    val focusMinutesTotal: Int = 0,
    val focusMinutesWeek: Int = 0,
    val focusSessions: Int = 0,
    val listsCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(c: AppContainer) : BaseViewModel(c) {
    private val source: Flow<StatsState> = c.accountRepository.activeAccountId.flatMapLatest { id ->
        val planner = combine(
            c.plannerRepository.tasks(id), c.plannerRepository.goals(id), c.plannerRepository.habits(id),
            c.plannerRepository.habitLogs(id)
        ) { tasks, goals, habits, logs -> Planner(tasks, goals, habits, logs) }
        combine(planner, c.focusHistoryRepository.sessions(id), c.checklistRepository.lists(id)) { p, sessions, lists ->
            val tasks = p.tasks
            val goals = p.goals
            val habits = p.habits
            val logs = p.logs
            val today = DateEngine.today()
            val byHabit = logs.groupBy({ it.habitId }, { it.epochDay }).mapValues { it.value.toSet() }
            val weekStartMillis = java.time.LocalDate.ofEpochDay(today - 6).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            StatsState(
                tasksTotal = tasks.size,
                tasksDone = tasks.count { it.done },
                tasksOverdue = tasks.count { !it.done && it.dueEpochDay != null && it.dueEpochDay < today },
                goalsTotal = goals.size,
                goalsDone = goals.count { it.done },
                habitsCount = habits.size,
                bestCurrentStreak = habits.maxOfOrNull { Streaks.current(byHabit[it.id].orEmpty(), today) } ?: 0,
                longestStreak = habits.maxOfOrNull { Streaks.longest(byHabit[it.id].orEmpty()) } ?: 0,
                focusMinutesTotal = sessions.sumOf { it.actualMinutes },
                focusMinutesWeek = sessions.filter { it.startedAt >= weekStartMillis }.sumOf { it.actualMinutes },
                focusSessions = sessions.size,
                listsCount = lists.size
            )
        }
    }
    val state = source.asState(StatsState())
}

@Composable
fun StatsScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: StatsViewModel = viewModel(factory = factory)
    val s by vm.state.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.stats_title), onBack = { nav.popBackStack() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s1)
        ) {
            item {
                LayeredCard {
                    SectionTitle(stringResource(R.string.tab_tasks))
                    Progress(s.tasksDone, s.tasksTotal)
                    Text(loc(R.string.stats_done_of, s.tasksDone, s.tasksTotal), style = MaterialTheme.typography.bodyMedium)
                    Text(loc(R.string.stats_overdue, s.tasksOverdue), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                LayeredCard(accent = MaterialTheme.colorScheme.secondary) {
                    SectionTitle(stringResource(R.string.tab_goals))
                    Progress(s.goalsDone, s.goalsTotal)
                    Text(loc(R.string.stats_done_of, s.goalsDone, s.goalsTotal), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                LayeredCard(accent = MaterialTheme.colorScheme.tertiary) {
                    SectionTitle(stringResource(R.string.tab_habits))
                    Text(loc(R.string.stats_habits_count, s.habitsCount), style = MaterialTheme.typography.bodyMedium)
                    Text(loc(R.string.stats_current_streak, s.bestCurrentStreak), style = MaterialTheme.typography.bodyMedium)
                    Text(loc(R.string.stats_longest_streak, s.longestStreak), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                LayeredCard {
                    SectionTitle(stringResource(R.string.focus_title))
                    Text(loc(R.string.stats_focus_week, s.focusMinutesWeek), style = MaterialTheme.typography.bodyMedium)
                    Text(loc(R.string.stats_focus_total, s.focusMinutesTotal), style = MaterialTheme.typography.bodyMedium)
                    Text(loc(R.string.stats_focus_sessions, s.focusSessions), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item {
                LayeredCard(accent = MaterialTheme.colorScheme.secondary) {
                    SectionTitle(stringResource(R.string.lists_title))
                    Text(loc(R.string.stats_lists_count, s.listsCount), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun Progress(done: Int, total: Int) {
    LinearProgressIndicator(
        progress = { if (total == 0) 0f else done / total.toFloat() },
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.half)
    )
}
