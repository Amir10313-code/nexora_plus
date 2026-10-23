package com.mahirex.nexoraplus.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.core.Palette
import com.mahirex.nexoraplus.data.local.entity.GoalEntity
import com.mahirex.nexoraplus.data.local.entity.HabitEntity
import com.mahirex.nexoraplus.data.local.entity.TaskEntity
import com.mahirex.nexoraplus.data.repo.Streaks
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.ColorDot
import com.mahirex.nexoraplus.ui.components.ConfirmDialog
import com.mahirex.nexoraplus.ui.components.EmptyState
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.PickDateDialog
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.SwatchRow
import com.mahirex.nexoraplus.ui.components.LocalProfile
import com.mahirex.nexoraplus.ui.components.loc
import com.mahirex.nexoraplus.ui.components.rememberDateFormatter
import com.mahirex.nexoraplus.ui.designsystem.Space
import com.mahirex.nexoraplus.ui.theme.NexoraTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TasksViewModel(c: AppContainer) : BaseViewModel(c) {
    val tasks = perAccount { c.plannerRepository.tasks(it) }.asState(emptyList<TaskEntity>())
    val goals = perAccount { c.plannerRepository.goals(it) }.asState(emptyList<GoalEntity>())
    val habits = perAccount { c.plannerRepository.habits(it) }.asState(emptyList<HabitEntity>())
    val logs = perAccount { c.plannerRepository.habitLogs(it) }
        .map { list -> list.groupBy({ it.habitId }, { it.epochDay }).mapValues { it.value.toSet() } }
        .asState(emptyMap<Long, Set<Long>>())
    val today = todayFlow().asState(com.mahirex.nexoraplus.calendar.DateEngine.today())

    fun saveTask(t: TaskEntity) = viewModelScope.launch { c.plannerRepository.saveTask(t) }
    fun setTaskDone(t: TaskEntity, done: Boolean) = viewModelScope.launch { c.plannerRepository.setTaskDone(t, done) }
    fun deleteTask(id: Long) = viewModelScope.launch { c.plannerRepository.deleteTask(id) }

    fun saveGoal(g: GoalEntity) = viewModelScope.launch { c.plannerRepository.saveGoal(g) }
    fun deleteGoal(id: Long) = viewModelScope.launch { c.plannerRepository.deleteGoal(id) }

    fun saveHabit(h: HabitEntity) = viewModelScope.launch { c.plannerRepository.saveHabit(h) }
    fun deleteHabit(id: Long) = viewModelScope.launch { c.plannerRepository.deleteHabit(id) }
    fun toggleHabit(habitId: Long) = viewModelScope.launch { c.plannerRepository.toggleHabit(habitId, today.value) }
}

private fun sortedTasks(list: List<TaskEntity>): List<TaskEntity> = list.sortedWith(
    compareByDescending<TaskEntity> { it.pinned }
        .thenBy { it.done }
        .thenByDescending { it.priority }
        .thenBy { it.dueEpochDay ?: Long.MAX_VALUE }
        .thenByDescending { it.createdAt }
)

@Composable
fun TasksScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: TasksViewModel = viewModel(factory = factory)
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val goals by vm.goals.collectAsStateWithLifecycle()
    val habits by vm.habits.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val accountId by vm.accountId.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editTask by remember { mutableStateOf<TaskEntity?>(null) }
    var editGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var editHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var deleting by remember { mutableStateOf<(() -> Unit)?>(null) }

    val now = System.currentTimeMillis()

    ScreenScaffold(
        title = stringResource(R.string.nav_tasks),
        hasBottomBar = true,
        floatingActionButton = {
            FloatingActionButton(onClick = {
                when (tab) {
                    0 -> editTask = TaskEntity(accountId = accountId, title = "", createdAt = now)
                    1 -> editGoal = GoalEntity(accountId = accountId, title = "", createdAt = now)
                    else -> editHabit = HabitEntity(accountId = accountId, title = "", colorArgb = Palette.at(habits.size), createdAt = now)
                }
            }) { Icon(Icons.Rounded.Add, stringResource(R.string.add)) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                listOf(R.string.tab_tasks, R.string.tab_goals, R.string.tab_habits).forEachIndexed { i, label ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(label)) })
                }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(Space.s2),
                verticalArrangement = Arrangement.spacedBy(Space.s1)
            ) {
                when (tab) {
                    0 -> {
                        if (tasks.isEmpty()) item { EmptyState(Icons.Rounded.TaskAlt, stringResource(R.string.empty_tasks_title), stringResource(R.string.empty_tasks_message)) }
                        items(sortedTasks(tasks), key = { it.id }) { t ->
                            TaskRow(
                                t, today, onToggle = { vm.setTaskDone(t, it) }, onPin = { vm.saveTask(t.copy(pinned = !t.pinned)) },
                                onEdit = { editTask = t }, onDelete = { deleting = { vm.deleteTask(t.id) } }
                            )
                        }
                    }
                    1 -> {
                        if (goals.isEmpty()) item { EmptyState(Icons.Rounded.Flag, stringResource(R.string.empty_goals_title), stringResource(R.string.empty_goals_message)) }
                        items(goals.sortedWith(compareByDescending<GoalEntity> { it.pinned }.thenBy { it.done }.thenByDescending { it.createdAt }), key = { it.id }) { g ->
                            GoalRow(
                                g, onDone = { vm.saveGoal(g.copy(done = it, progress = if (it) 100 else g.progress)) },
                                onPin = { vm.saveGoal(g.copy(pinned = !g.pinned)) },
                                onEdit = { editGoal = g }, onDelete = { deleting = { vm.deleteGoal(g.id) } }
                            )
                        }
                    }
                    else -> {
                        if (habits.isEmpty()) item { EmptyState(Icons.Rounded.CheckCircle, stringResource(R.string.empty_habits_title), stringResource(R.string.empty_habits_message)) }
                        items(habits.sortedWith(compareByDescending<HabitEntity> { it.pinned }.thenByDescending { it.createdAt }), key = { it.id }) { h ->
                            HabitRow(
                                h, logs[h.id].orEmpty(), today, onToggle = { vm.toggleHabit(h.id) },
                                onPin = { vm.saveHabit(h.copy(pinned = !h.pinned)) },
                                onEdit = { editHabit = h }, onDelete = { deleting = { vm.deleteHabit(h.id) } }
                            )
                        }
                    }
                }
            }
        }
    }

    editTask?.let { t -> TaskDialog(t, onDismiss = { editTask = null }, onSave = { vm.saveTask(it); editTask = null }) }
    editGoal?.let { g -> GoalDialog(g, onDismiss = { editGoal = null }, onSave = { vm.saveGoal(it); editGoal = null }) }
    editHabit?.let { h -> HabitDialog(h, onDismiss = { editHabit = null }, onSave = { vm.saveHabit(it); editHabit = null }) }
    deleting?.let { action ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            message = stringResource(R.string.confirm_delete_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { action(); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun priorityColor(priority: Int): Color = when (priority) {
    2 -> NexoraTheme.colors.holiday
    0 -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun priorityLabel(priority: Int): String = stringResource(
    when (priority) {
        2 -> R.string.priority_high
        0 -> R.string.priority_low
        else -> R.string.priority_normal
    }
)

@Composable
private fun RowActions(pinned: Boolean, onPin: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    IconButton(onClick = onPin) {
        Icon(
            Icons.Rounded.PushPin, stringResource(R.string.pin),
            tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    }
    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit)) }
    IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
}

@Composable
private fun TaskRow(
    t: TaskEntity, today: Long, onToggle: (Boolean) -> Unit, onPin: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val fmtr = rememberDateFormatter(LocalProfile.current)
    val overdue = !t.done && t.dueEpochDay != null && t.dueEpochDay < today
    LayeredCard(accent = priorityColor(t.priority)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = t.done, onCheckedChange = onToggle)
            Column(Modifier.weight(1f)) {
                Text(
                    t.title, style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (t.done) TextDecoration.LineThrough else null,
                    color = if (t.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (t.note.isNotBlank()) Text(t.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
                    Text(priorityLabel(t.priority), style = MaterialTheme.typography.labelSmall, color = priorityColor(t.priority))
                    if (t.dueEpochDay != null) {
                        Text(
                            (if (overdue) stringResource(R.string.overdue) + " · " else "") + fmtr.short(t.dueEpochDay),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            RowActions(t.pinned, onPin, onEdit, onDelete)
        }
    }
}

@Composable
private fun GoalRow(g: GoalEntity, onDone: (Boolean) -> Unit, onPin: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val fmtr = rememberDateFormatter(LocalProfile.current)
    LayeredCard(accent = MaterialTheme.colorScheme.secondary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = g.done, onCheckedChange = onDone)
            Column(Modifier.weight(1f)) {
                Text(
                    g.title, style = MaterialTheme.typography.titleSmall,
                    textDecoration = if (g.done) TextDecoration.LineThrough else null
                )
                if (g.note.isNotBlank()) Text(g.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (g.targetEpochDay != null) {
                    Text(
                        stringResource(R.string.target) + ": " + fmtr.short(g.targetEpochDay),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RowActions(g.pinned, onPin, onEdit, onDelete)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
            LinearProgressIndicator(progress = { g.progress / 100f }, modifier = Modifier.weight(1f))
            Text(loc(R.string.percent_format, g.progress), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun HabitRow(
    h: HabitEntity, days: Set<Long>, today: Long, onToggle: () -> Unit, onPin: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit
) {
    val color = Color(h.colorArgb)
    val doneToday = today in days
    val last7 = Streaks.lastDays(days, today)
    LayeredCard(accent = color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (doneToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    stringResource(R.string.mark_today), tint = color, modifier = Modifier.size(Space.s4)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(h.title, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.half), verticalAlignment = Alignment.CenterVertically) {
                    last7.forEach { logged -> ColorDot(if (logged) color else MaterialTheme.colorScheme.outlineVariant, size = Space.s1 + Space.half) }
                    Text(
                        loc(R.string.streak_format, Streaks.current(days, today)),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = Space.s1)
                    )
                }
            }
            RowActions(h.pinned, onPin, onEdit, onDelete)
        }
    }
}

@Composable
private fun TaskDialog(initial: TaskEntity, onDismiss: () -> Unit, onSave: (TaskEntity) -> Unit) {
    val fmtr = rememberDateFormatter(LocalProfile.current)
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var priority by remember { mutableIntStateOf(initial.priority) }
    var due by remember { mutableStateOf(initial.dueEpochDay) }
    var pinned by remember { mutableStateOf(initial.pinned) }
    var pickDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.add_task else R.string.edit_task)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.title)) })
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.note)) })
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
                    listOf(0, 1, 2).forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(priorityLabel(p), maxLines = 1) })
                    }
                }
                OutlinedButton(onClick = { pickDate = true }, Modifier.fillMaxWidth()) {
                    Text(due?.let { stringResource(R.string.due_date) + ": " + fmtr.short(it) } ?: stringResource(R.string.set_due_date))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pin), Modifier.weight(1f))
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onSave(initial.copy(title = title.trim(), note = note.trim(), priority = priority, dueEpochDay = due, pinned = pinned))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
    if (pickDate) {
        PickDateDialog(due, onDismiss = { pickDate = false }, onPick = { due = it; pickDate = false }, onClear = { due = null; pickDate = false })
    }
}

@Composable
private fun GoalDialog(initial: GoalEntity, onDismiss: () -> Unit, onSave: (GoalEntity) -> Unit) {
    val fmtr = rememberDateFormatter(LocalProfile.current)
    var title by remember { mutableStateOf(initial.title) }
    var note by remember { mutableStateOf(initial.note) }
    var progress by remember { mutableIntStateOf(initial.progress) }
    var target by remember { mutableStateOf(initial.targetEpochDay) }
    var pinned by remember { mutableStateOf(initial.pinned) }
    var pickDate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.add_goal else R.string.edit_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.title)) })
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.note)) })
                Text(stringResource(R.string.progress) + ": " + loc(R.string.percent_format, progress), style = MaterialTheme.typography.labelLarge)
                Slider(value = progress.toFloat(), onValueChange = { progress = it.toInt() }, valueRange = 0f..100f)
                OutlinedButton(onClick = { pickDate = true }, Modifier.fillMaxWidth()) {
                    Text(target?.let { stringResource(R.string.target) + ": " + fmtr.short(it) } ?: stringResource(R.string.set_target_date))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pin), Modifier.weight(1f))
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onSave(initial.copy(title = title.trim(), note = note.trim(), progress = progress, targetEpochDay = target, pinned = pinned, done = progress >= 100))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
    if (pickDate) {
        PickDateDialog(target, onDismiss = { pickDate = false }, onPick = { target = it; pickDate = false }, onClear = { target = null; pickDate = false })
    }
}

@Composable
private fun HabitDialog(initial: HabitEntity, onDismiss: () -> Unit, onSave: (HabitEntity) -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var color by remember { mutableIntStateOf(initial.colorArgb) }
    var pinned by remember { mutableStateOf(initial.pinned) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.add_habit else R.string.edit_habit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.title)) })
                SwatchRow(selected = color, onSelect = { color = it })
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.pin), Modifier.weight(1f))
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onSave(initial.copy(title = title.trim(), colorArgb = color, pinned = pinned)) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
