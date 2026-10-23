package com.mahirex.nexoraplus.ui.screens.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.calendar.CalendarType
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.calendar.MonthGrid
import com.mahirex.nexoraplus.data.local.entity.UserEventEntity
import com.mahirex.nexoraplus.data.repo.toDef
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.occasions.OccasionDef
import com.mahirex.nexoraplus.occasions.OccasionResolver
import com.mahirex.nexoraplus.occasions.UserEventDef
import com.mahirex.nexoraplus.occasions.UserEventResolver
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.LocalProfile
import com.mahirex.nexoraplus.ui.components.OccasionList
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.SectionTitle
import com.mahirex.nexoraplus.ui.components.WeekdayHeader
import com.mahirex.nexoraplus.ui.components.loc
import com.mahirex.nexoraplus.ui.components.rememberDateFormatter
import com.mahirex.nexoraplus.ui.designsystem.Motion
import com.mahirex.nexoraplus.ui.designsystem.Space
import com.mahirex.nexoraplus.ui.theme.NexoraTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(c: AppContainer) : BaseViewModel(c) {
    val defs = c.profile.flatMapLatest { c.calendarRepository.occasionDefs(it.occasionRegionIds) }
        .asState(emptyList<OccasionDef>())

    val events = perAccount { c.calendarRepository.userEvents(it) }
        .map { list -> list.mapNotNull { it.toDef() } }
        .asState(emptyList<UserEventDef>())

    fun addEvent(title: String, note: String, type: CalendarType, year: Int, month: Int, day: Int, repeat: Boolean) {
        val id = accountId.value
        if (id == 0L || title.isBlank()) return
        viewModelScope.launch {
            c.calendarRepository.saveUserEvent(
                UserEventEntity(
                    accountId = id, title = title.trim(), note = note.trim(), calendarType = type.code,
                    year = if (repeat) null else year, month = month, day = day, repeatYearly = repeat,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch { c.calendarRepository.deleteUserEvent(id) }
    }
}

private fun monthKey(year: Int, month: Int) = year * 12 + (month - 1)

@Composable
fun CalendarScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: CalendarViewModel = viewModel(factory = factory)
    val defs by vm.defs.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val profile = LocalProfile.current
    val fmt = rememberDateFormatter(profile)
    val engine = profile.engine
    val type = profile.primary
    val system = engine.system(type)
    val today = remember { DateEngine.today() }

    val todayCivil = engine.civil(type, today)
    var year by rememberSaveable { mutableStateOf(todayCivil.year) }
    var month by rememberSaveable { mutableStateOf(todayCivil.month) }
    var selected by rememberSaveable { mutableStateOf(today) }
    var showAdd by remember { mutableStateOf(false) }

    // Calendar type or settings may change while the screen is alive: keep the page inside the valid range.
    LaunchedEffect(type) {
        if (year !in system.minYear..system.maxYear) {
            year = todayCivil.year
            month = todayCivil.month
        }
    }

    fun goto(deltaMonths: Int) {
        val (y, m) = MonthGrid.shiftMonth(year, month, deltaMonths)
        if (y in system.minYear..system.maxYear) {
            year = y
            month = m
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.nav_calendar),
        hasBottomBar = true,
        actions = {
            IconButton(onClick = {
                year = todayCivil.year
                month = todayCivil.month
                selected = today
            }) { Icon(Icons.Rounded.Today, stringResource(R.string.today)) }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Rounded.Add, stringResource(R.string.add_event)) }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = Space.s2, vertical = Space.s1),
            verticalArrangement = Arrangement.spacedBy(Space.s1)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { goto(-1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous))
                    }
                    Text(
                        fmt.monthYear(type, year, month),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = { goto(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next))
                    }
                }
            }
            item { WeekdayHeader() }
            item {
                val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                AnimatedContent(
                    targetState = monthKey(year, month),
                    transitionSpec = {
                        val dir = (if (targetState > initialState) 1 else -1) * (if (rtl) -1 else 1)
                        (slideInHorizontally(tween(Motion.NavMs)) { w -> dir * w } + fadeIn(tween(Motion.NavMs))) togetherWith
                            (slideOutHorizontally(tween(Motion.NavMs)) { w -> -dir * w } + fadeOut(tween(Motion.NavMs)))
                    },
                    label = "month"
                ) { key ->
                    val y = Math.floorDiv(key, 12)
                    val m = Math.floorMod(key, 12) + 1
                    MonthPage(
                        year = y, month = m, selected = selected, today = today,
                        defs = defs, events = events, onSelect = { selected = it }
                    )
                }
            }
            item {
                SelectedDayCard(
                    epochDay = selected, defs = defs, events = events,
                    onDelete = { vm.deleteEvent(it) }
                )
            }
        }
    }

    if (showAdd) {
        AddEventDialog(
            epochDay = selected,
            onDismiss = { showAdd = false },
            onSave = { title, note, repeat ->
                val civil = engine.civil(type, selected)
                vm.addEvent(title, note, type, civil.year, civil.month, civil.day, repeat)
                showAdd = false
            }
        )
    }
}

@Composable
private fun MonthPage(
    year: Int,
    month: Int,
    selected: Long,
    today: Long,
    defs: List<OccasionDef>,
    events: List<UserEventDef>,
    onSelect: (Long) -> Unit
) {
    val profile = LocalProfile.current
    val fmt = rememberDateFormatter(profile)
    val engine = profile.engine
    val type = profile.primary
    val system = engine.system(type)

    val weeks = remember(year, month, profile) { MonthGrid.build(system, year, month, profile.weekStart) }
    val start = weeks.first().first().epochDay
    val end = weeks.last().last().epochDay
    val occasions = remember(defs, profile, start, end) { OccasionResolver(engine).groupByDay(defs, start, end) }
    val userEvents = remember(events, profile, start, end) {
        UserEventResolver.resolve(events, engine, start, end).groupBy { it.epochDay }
    }

    Column(Modifier.fillMaxWidth()) {
        weeks.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    val e = day.epochDay
                    val dayOcc = occasions[e].orEmpty()
                    val isHoliday = dayOcc.any { it.def.holiday } || DateEngine.dayOfWeek(e) in profile.weekend
                    val isSelected = e == selected
                    val isToday = e == today
                    val alpha = if (day.inMonth) 1f else 0.35f
                    Column(
                        Modifier
                            .weight(1f)
                            .height(Space.s6 + Space.s2 + Space.s1)
                            .padding(1.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0f))
                            .then(
                                if (isToday) Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), MaterialTheme.shapes.small)
                                else Modifier
                            )
                            .clickable { onSelect(e) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            fmt.number(system.fromEpochDay(e).day),
                            style = MaterialTheme.typography.bodyLarge,
                            color = (if (isHoliday) NexoraTheme.colors.holiday else MaterialTheme.colorScheme.onSurface)
                                .copy(alpha = alpha)
                        )
                        if (profile.secondary.isNotEmpty()) {
                            Text(
                                profile.secondary.joinToString(" ") { fmt.number(engine.civil(it, e).day) },
                                fontSize = 10.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                            )
                        }
                        Row(Modifier.height(6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (day.inMonth) {
                                if (dayOcc.any { it.def.holiday }) Dot(NexoraTheme.colors.holiday)
                                if (dayOcc.any { !it.def.holiday }) Dot(MaterialTheme.colorScheme.secondary)
                                if (userEvents[e].orEmpty().isNotEmpty()) Dot(MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(Modifier.size(5.dp).clip(CircleShape).background(color))
}

@Composable
private fun SelectedDayCard(
    epochDay: Long,
    defs: List<OccasionDef>,
    events: List<UserEventDef>,
    onDelete: (Long) -> Unit
) {
    val profile = LocalProfile.current
    val fmt = rememberDateFormatter(profile)
    val occ = remember(defs, profile, epochDay) { OccasionResolver(profile.engine).resolve(defs, epochDay, epochDay) }
    val mine = remember(events, profile, epochDay) { UserEventResolver.resolve(events, profile.engine, epochDay, epochDay) }

    LayeredCard {
        Text(fmt.full(epochDay, profile.primary), style = MaterialTheme.typography.titleMedium)
        profile.secondary.forEach { type ->
            val label = when (type) {
                CalendarType.JALALI -> R.string.cal_jalali
                CalendarType.HIJRI -> R.string.cal_hijri
                CalendarType.GREGORIAN -> R.string.cal_gregorian
            }
            Text(
                loc(label) + ": " + fmt.short(epochDay, type),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (occ.isEmpty() && mine.isEmpty()) {
            Text(
                stringResource(R.string.no_occasions_day),
                modifier = Modifier.padding(top = Space.s1),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (occ.isNotEmpty()) {
            Box(Modifier.padding(top = Space.s1)) { OccasionList(occ) }
        }
        if (mine.isNotEmpty()) {
            SectionTitle(stringResource(R.string.my_events))
            mine.forEach { r ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(r.def.title, style = MaterialTheme.typography.bodyMedium)
                        if (r.def.note.isNotBlank()) {
                            Text(r.def.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (r.def.repeatYearly) {
                            Text(stringResource(R.string.repeats_yearly), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    IconButton(onClick = { onDelete(r.def.id) }) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEventDialog(epochDay: Long, onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit) {
    val profile = LocalProfile.current
    val fmt = rememberDateFormatter(profile)
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_event)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                Text(fmt.short(epochDay), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = title, onValueChange = { title = it }, singleLine = true,
                    label = { Text(stringResource(R.string.title)) }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note)) }, modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.repeat_yearly), Modifier.weight(1f))
                    Switch(checked = repeat, onCheckedChange = { repeat = it })
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onSave(title, note, repeat) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
