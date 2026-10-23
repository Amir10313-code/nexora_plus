package com.mahirex.nexoraplus.ui.screens.focus

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.data.local.entity.FocusSessionEntity
import com.mahirex.nexoraplus.data.prefs.ActiveFocus
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.LocalDigits
import com.mahirex.nexoraplus.ui.components.LocalProfile
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.SectionTitle
import com.mahirex.nexoraplus.ui.components.loc
import com.mahirex.nexoraplus.ui.components.rememberDateFormatter
import com.mahirex.nexoraplus.ui.designsystem.Space
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class FocusViewModel(c: AppContainer) : BaseViewModel(c) {
    val active = c.focusRepository.active.asState(null as ActiveFocus?)
    val sessions = perAccount { c.focusHistoryRepository.sessions(it) }.asState(emptyList<FocusSessionEntity>())

    fun dndGranted(): Boolean = c.focusRepository.dndAccessGranted()
    fun start(minutes: Int, useDnd: Boolean) = viewModelScope.launch { c.focusRepository.start(accountId.value, minutes, useDnd) }
    fun stop() = viewModelScope.launch { c.focusRepository.finish() }
    fun finishIfDue() = viewModelScope.launch { c.focusRepository.finishIfDue() }
}

private val PRESETS = listOf(15, 25, 45, 60, 90)

@Composable
fun FocusScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: FocusViewModel = viewModel(factory = factory)
    val active by vm.active.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var minutes by rememberSaveable { mutableIntStateOf(25) }
    var useDnd by rememberSaveable { mutableStateOf(false) }
    var dndGranted by remember { mutableStateOf(vm.dndGranted()) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Re-check the permission on return from system settings and end an overdue session.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            dndGranted = vm.dndGranted()
            vm.finishIfDue()
        }
    }
    // Ticks once a second only while a session is running and the screen is visible.
    LaunchedEffect(active, lifecycle) {
        if (active != null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    nowMillis = System.currentTimeMillis()
                    val a = active
                    if (a != null && nowMillis >= a.startedAtMillis + a.plannedMinutes * 60_000L) {
                        vm.finishIfDue()
                    }
                    delay(1_000L)
                }
            }
        }
    }

    val fmtr = rememberDateFormatter(LocalProfile.current)
    val digits = LocalDigits.current

    ScreenScaffold(title = stringResource(R.string.focus_title), onBack = { nav.popBackStack() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s1),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                val a = active
                val totalMs = (a?.plannedMinutes ?: minutes) * 60_000L
                val remaining = if (a != null) (a.startedAtMillis + totalMs - nowMillis).coerceIn(0L, totalMs) else totalMs
                val fraction = if (totalMs > 0) remaining.toFloat() / totalMs else 1f
                val remainingSec = ((remaining + 999) / 1000).toInt()
                val label = digits.localize(
                    String.format(java.util.Locale.ROOT, "%02d:%02d", remainingSec / 60, remainingSec % 60)
                )
                val track = MaterialTheme.colorScheme.surfaceVariant
                val arc = MaterialTheme.colorScheme.primary
                Box(Modifier.size(Space.s6 * 5).padding(Space.s2), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        val inset = stroke.width / 2
                        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                        drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
                        drawArc(arc, -90f, 360f * fraction, false, Offset(inset, inset), arcSize, style = stroke)
                    }
                    Text(label, style = MaterialTheme.typography.displayMedium)
                }
            }
            if (active == null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.s1), modifier = Modifier.fillMaxWidth()) {
                        PRESETS.forEach { m ->
                            FilterChip(
                                selected = minutes == m, onClick = { minutes = m },
                                label = { Text(digits.format(m), maxLines = 1) }
                            )
                        }
                    }
                }
                item { Text(stringResource(R.string.minutes_unit), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item {
                    LayeredCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.focus_dnd), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    stringResource(if (dndGranted) R.string.focus_dnd_hint else R.string.focus_dnd_needs_access),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useDnd && dndGranted,
                                onCheckedChange = { want ->
                                    if (want && !dndGranted) {
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }
                                    useDnd = want
                                }
                            )
                        }
                        if (!dndGranted) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                },
                                modifier = Modifier.padding(top = Space.s1)
                            ) { Text(stringResource(R.string.focus_open_settings)) }
                        }
                    }
                }
                item {
                    Button(onClick = { nowMillis = System.currentTimeMillis(); vm.start(minutes, useDnd && dndGranted) }, Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.focus_start))
                    }
                }
            } else {
                item {
                    Button(onClick = { vm.stop() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.focus_stop)) }
                }
            }

            item { Spacer(Modifier.height(Space.s1)) }
            item { SectionTitle(stringResource(R.string.focus_history), Modifier.fillMaxWidth()) }
            if (sessions.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.focus_history_empty), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(sessions, key = { it.id }) { s ->
                val day = Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
                LayeredCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(fmtr.short(day), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(if (s.completed) R.string.focus_completed else R.string.focus_stopped_early),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (s.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(loc(R.string.minutes_format, s.actualMinutes), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
