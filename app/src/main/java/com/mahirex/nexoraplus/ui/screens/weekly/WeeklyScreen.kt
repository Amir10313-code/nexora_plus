package com.mahirex.nexoraplus.ui.screens.weekly

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.data.local.entity.ScheduleCellEntity
import com.mahirex.nexoraplus.data.local.entity.ScheduleSlotEntity
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.ConfirmDialog
import com.mahirex.nexoraplus.ui.components.EmptyState
import com.mahirex.nexoraplus.ui.components.LocalProfile
import com.mahirex.nexoraplus.ui.components.PickTimeDialog
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.SwatchRow
import com.mahirex.nexoraplus.ui.components.rememberDateFormatter
import com.mahirex.nexoraplus.ui.designsystem.Space
import com.mahirex.nexoraplus.ui.theme.NexoraTheme
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class WeeklyViewModel(c: AppContainer) : BaseViewModel(c) {
    val slots = perAccount { c.scheduleRepository.slots(it) }.asState(emptyList<ScheduleSlotEntity>())
    val cells = perAccount { c.scheduleRepository.cells(it) }.asState(emptyList<ScheduleCellEntity>())

    fun addSlot() {
        val id = accountId.value
        if (id != 0L) viewModelScope.launch { c.scheduleRepository.addSlot(id, slots.value) }
    }

    fun updateSlot(slot: ScheduleSlotEntity) {
        viewModelScope.launch { c.scheduleRepository.updateSlot(slot) }
    }

    fun deleteSlot(id: Long) {
        viewModelScope.launch { c.scheduleRepository.deleteSlot(id) }
    }

    fun moveSlot(id: Long, delta: Int) {
        viewModelScope.launch { c.scheduleRepository.moveSlot(slots.value, id, delta) }
    }

    fun setCell(slotId: Long, dayOfWeek: Int, text: String, color: Int?) {
        val id = accountId.value
        if (id != 0L) viewModelScope.launch { c.scheduleRepository.setCell(id, slotId, dayOfWeek, text, color) }
    }

    fun setWeekStart(value: String) {
        viewModelScope.launch { c.settingsStore.setWeekStart(value) }
    }

    fun saveAsDefault() {
        val id = accountId.value
        if (id == 0L) return
        viewModelScope.launch { c.scheduleRepository.savePreset(id, c.settingsStore.current().weekStart) }
    }

    fun resetToDefault() {
        val id = accountId.value
        if (id == 0L) return
        viewModelScope.launch {
            c.scheduleRepository.resetToDefault(id)?.let { c.settingsStore.setWeekStart(it) }
        }
    }
}

private val CELL_WIDTH = 92.dp
private val TIME_WIDTH = 84.dp
private val CELL_HEIGHT = 64.dp

@Composable
fun WeeklyScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: WeeklyViewModel = viewModel(factory = factory)
    val slots by vm.slots.collectAsStateWithLifecycle()
    val cells by vm.cells.collectAsStateWithLifecycle()
    val profile = LocalProfile.current
    val fmt = rememberDateFormatter(profile)
    var showManage by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Pair<ScheduleSlotEntity, Int>?>(null) }

    val days = List(7) { DayOfWeek.of((profile.weekStart.value - 1 + it) % 7 + 1) }
    val cellMap = remember(cells) { cells.associateBy { it.slotId to it.dayOfWeek } }
    val today = remember { DateEngine.dayOfWeek(DateEngine.today()) }

    ScreenScaffold(
        title = stringResource(R.string.nav_weekly),
        hasBottomBar = true,
        actions = {
            IconButton(onClick = { showManage = true }) { Icon(Icons.Rounded.Tune, stringResource(R.string.manage_blocks)) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.s2),
                horizontalArrangement = Arrangement.spacedBy(Space.s1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.week_starts_on), style = MaterialTheme.typography.labelLarge)
                listOf("SAT" to DayOfWeek.SATURDAY, "SUN" to DayOfWeek.SUNDAY, "MON" to DayOfWeek.MONDAY).forEach { (value, dow) ->
                    FilterChip(
                        selected = profile.weekStart == dow,
                        onClick = { vm.setWeekStart(value) },
                        label = { Text(fmt.weekdayFull(dow), maxLines = 1) }
                    )
                }
            }

            if (slots.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.ViewWeek,
                    title = stringResource(R.string.no_blocks_title),
                    message = stringResource(R.string.no_blocks_message),
                    actionLabel = stringResource(R.string.add_block),
                    onAction = { vm.addSlot() }
                )
            } else {
                Box(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                ) {
                    Column(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Space.s2, vertical = Space.s1)) {
                        Row {
                            Box(Modifier.width(TIME_WIDTH).height(Space.s5))
                            days.forEach { dow ->
                                val isToday = dow == today
                                Box(
                                    Modifier.width(CELL_WIDTH).height(Space.s5).padding(1.dp)
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                                            MaterialTheme.shapes.small
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        fmt.weekdayShort(dow),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        slots.forEach { slot ->
                            Row {
                                TimeCell(slot, fmt.timeRange(slot.startMinute, slot.endMinute))
                                days.forEach { dow ->
                                    val cell = cellMap[slot.id to dow.value]
                                    GridCell(slot, cell) { editing = slot to dow.value }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { (slot, dow) ->
        val cell = cellMap[slot.id to dow]
        EditCellDialog(
            initialText = cell?.text.orEmpty(),
            initialColor = cell?.colorArgb,
            onDismiss = { editing = null },
            onSave = { text, color ->
                vm.setCell(slot.id, dow, text, color)
                editing = null
            }
        )
    }

    if (showManage) {
        ManageBlocksSheet(
            slots = slots,
            onDismiss = { showManage = false },
            onAdd = { vm.addSlot() },
            onUpdate = { vm.updateSlot(it) },
            onDelete = { vm.deleteSlot(it) },
            onMove = { id, d -> vm.moveSlot(id, d) },
            onSaveDefault = { vm.saveAsDefault() },
            onReset = { vm.resetToDefault() }
        )
    }
}

@Composable
private fun TimeCell(slot: ScheduleSlotEntity, time: String) {
    Column(
        Modifier.width(TIME_WIDTH).height(CELL_HEIGHT).padding(1.dp)
            .background(Color(slot.colorArgb).copy(alpha = 0.18f), MaterialTheme.shapes.small)
            .border(1.dp, Color(slot.colorArgb).copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .padding(Space.half),
        verticalArrangement = Arrangement.Center
    ) {
        Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        if (slot.label.isNotBlank()) {
            Text(slot.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GridCell(slot: ScheduleSlotEntity, cell: ScheduleCellEntity?, onClick: () -> Unit) {
    val tint = cell?.colorArgb?.let { Color(it) }
    Box(
        Modifier.width(CELL_WIDTH).height(CELL_HEIGHT).padding(1.dp)
            .background(tint?.copy(alpha = 0.22f) ?: MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .border(1.dp, tint?.copy(alpha = 0.7f) ?: MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(Space.half),
        contentAlignment = Alignment.Center
    ) {
        if (cell != null && cell.text.isNotBlank()) {
            Text(
                cell.text, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 3,
                overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EditCellDialog(initialText: String, initialColor: Int?, onDismiss: () -> Unit, onSave: (String, Int?) -> Unit) {
    var text by remember { mutableStateOf(initialText) }
    var color by remember { mutableStateOf(initialColor) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_cell)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                OutlinedTextField(
                    value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.cell_text)) }
                )
                SwatchRow(selected = color ?: 0, onSelect = { color = it })
                TextButton(onClick = { color = null }) { Text(stringResource(R.string.no_color)) }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text, color) }) { Text(stringResource(R.string.save)) } },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave("", null) }) { Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

@Composable
private fun ManageBlocksSheet(
    slots: List<ScheduleSlotEntity>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onUpdate: (ScheduleSlotEntity) -> Unit,
    onDelete: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
    onSaveDefault: () -> Unit,
    onReset: () -> Unit
) {
    var confirmReset by remember { mutableStateOf(false) }
    var savedHint by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s1)
        ) {
            item { Text(stringResource(R.string.manage_blocks), style = MaterialTheme.typography.titleLarge) }
            items(slots, key = { it.id }) { slot ->
                SlotEditor(
                    slot = slot, canMoveUp = slot.id != slots.first().id, canMoveDown = slot.id != slots.last().id,
                    onChange = onUpdate, onMove = { onMove(slot.id, it) }, onDelete = { onDelete(slot.id) }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                    Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Add, null)
                        Text(stringResource(R.string.add_block), modifier = Modifier.padding(start = Space.s1))
                    }
                    OutlinedButton(onClick = { onSaveDefault(); savedHint = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.save_as_default))
                    }
                    if (savedHint) {
                        Text(stringResource(R.string.default_saved), style = MaterialTheme.typography.bodySmall, color = NexoraTheme.colors.success)
                    }
                    OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.reset_to_default), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    if (confirmReset) {
        ConfirmDialog(
            title = stringResource(R.string.reset_to_default),
            message = stringResource(R.string.reset_blocks_message),
            confirmLabel = stringResource(R.string.reset),
            onConfirm = { onReset(); confirmReset = false },
            onDismiss = { confirmReset = false }
        )
    }
}

@Composable
private fun SlotEditor(
    slot: ScheduleSlotEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onChange: (ScheduleSlotEntity) -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val profile = LocalProfile.current
    val fmt = rememberDateFormatter(profile)
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    var label by remember(slot.id) { mutableStateOf(slot.label) }

    Column(
        Modifier.fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .padding(Space.s1),
        verticalArrangement = Arrangement.spacedBy(Space.s1)
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = { label = it; onChange(slot.copy(label = it)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(stringResource(R.string.block_name)) }
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
            OutlinedButton(onClick = { pickStart = true }) { Text(fmt.minuteOfDay(slot.startMinute)) }
            Text("–")
            OutlinedButton(onClick = { pickEnd = true }) { Text(fmt.minuteOfDay(slot.endMinute)) }
        }
        SwatchRow(selected = slot.colorArgb, onSelect = { onChange(slot.copy(colorArgb = it)) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(enabled = canMoveUp, onClick = { onMove(-1) }) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up)) }
            IconButton(enabled = canMoveDown, onClick = { onMove(1) }) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down)) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
        }
    }
    if (pickStart) {
        PickTimeDialog(slot.startMinute, onDismiss = { pickStart = false }) { m ->
            val end = if (slot.endMinute <= m) (m + 60).coerceAtMost(24 * 60) else slot.endMinute
            onChange(slot.copy(startMinute = m, endMinute = maxOf(end, m)))
            pickStart = false
        }
    }
    if (pickEnd) {
        PickTimeDialog(slot.endMinute % (24 * 60), onDismiss = { pickEnd = false }) { m ->
            val end = if (m <= slot.startMinute) (slot.startMinute + 15).coerceAtMost(24 * 60) else m
            onChange(slot.copy(endMinute = end))
            pickEnd = false
        }
    }
}
