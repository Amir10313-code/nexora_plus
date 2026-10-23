package com.mahirex.nexoraplus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.calendar.MonthGrid
import com.mahirex.nexoraplus.ui.designsystem.Space
import com.mahirex.nexoraplus.ui.theme.NexoraTheme
import java.time.DayOfWeek

/** Weekday names in the order of the profile's week start. */
@Composable
fun WeekdayHeader(modifier: Modifier = Modifier) {
    val profile = LocalProfile.current
    val fmt = rememberDateFormatter(profile)
    Row(modifier.fillMaxWidth()) {
        for (i in 0 until 7) {
            val dow = DayOfWeek.of((profile.weekStart.value - 1 + i) % 7 + 1)
            Text(
                fmt.weekdayShort(dow),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = if (dow in profile.weekend) NexoraTheme.colors.holiday else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** Date picker for the profile's primary calendar (Jalali, Hijri or Gregorian). */
@Composable
fun PickDateDialog(
    initialEpochDay: Long?,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
    onClear: (() -> Unit)? = null
) {
    val profile = LocalProfile.current
    val fmt = rememberDateFormatter(profile)
    val engine = profile.engine
    val type = profile.primary
    val system = engine.system(type)
    val today = remember { DateEngine.today() }
    val startCivil = engine.civil(type, initialEpochDay ?: today)
    var year by remember { mutableIntStateOf(startCivil.year.coerceIn(system.minYear, system.maxYear)) }
    var month by remember { mutableIntStateOf(startCivil.month) }
    var selected by remember { mutableStateOf(initialEpochDay) }

    fun move(delta: Int) {
        val (y, m) = MonthGrid.shiftMonth(year, month, delta)
        if (y in system.minYear..system.maxYear) {
            year = y
            month = m
        }
    }

    val weeks = remember(year, month, profile) { MonthGrid.build(system, year, month, profile.weekStart) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = selected != null, onClick = { selected?.let(onPick) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = onClear) { Text(stringResource(R.string.clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { move(-12) }) { Text("«", style = MaterialTheme.typography.titleMedium) }
                    IconButton(onClick = { move(-1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous)) }
                    Text(
                        fmt.monthYear(type, year, month),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                    IconButton(onClick = { move(1) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next)) }
                    IconButton(onClick = { move(12) }) { Text("»", style = MaterialTheme.typography.titleMedium) }
                }
                WeekdayHeader()
                weeks.forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            val isSelected = day.epochDay == selected
                            val isToday = day.epochDay == today
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .clip(CircleShape)
                                    .then(if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary) else Modifier)
                                    .then(
                                        if (isToday && !isSelected) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary), CircleShape)
                                        else Modifier
                                    )
                                    .then(if (day.inMonth) Modifier.clickable { selected = day.epochDay } else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day.inMonth) {
                                    val isWeekend = DateEngine.dayOfWeek(day.epochDay) in profile.weekend
                                    Text(
                                        fmt.number(system.fromEpochDay(day.epochDay).day),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isWeekend -> NexoraTheme.colors.holiday
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

/** Time picker (24 hour). [onPick] receives minutes after midnight. */
@Composable
fun PickTimeDialog(initialMinute: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val state = rememberTimePickerState(
        initialHour = (initialMinute / 60).coerceIn(0, 23),
        initialMinute = initialMinute % 60,
        is24Hour = true
    )
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.padding(Space.s3), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.ok)) }
                }
            }
        }
    }
}
