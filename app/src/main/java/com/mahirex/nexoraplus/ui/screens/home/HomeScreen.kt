package com.mahirex.nexoraplus.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.data.fortune.FortuneData
import com.mahirex.nexoraplus.data.fortune.FortuneEngine
import com.mahirex.nexoraplus.data.repo.toDef
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.occasions.OccasionResolver
import com.mahirex.nexoraplus.occasions.ResolvedOccasion
import com.mahirex.nexoraplus.occasions.ResolvedUserEvent
import com.mahirex.nexoraplus.occasions.UserEventResolver
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.GradientHeader
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.LocalProfile
import com.mahirex.nexoraplus.ui.components.OccasionList
import com.mahirex.nexoraplus.ui.components.SectionTitle
import com.mahirex.nexoraplus.ui.components.SkeletonList
import com.mahirex.nexoraplus.ui.components.loc
import com.mahirex.nexoraplus.ui.components.rememberDateFormatter
import com.mahirex.nexoraplus.ui.designsystem.Space
import com.mahirex.nexoraplus.ui.nav.Routes
import com.mahirex.nexoraplus.ui.nav.goTop
import com.mahirex.nexoraplus.ui.theme.NexoraTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import java.time.LocalTime

data class HomeState(
    val loaded: Boolean = false,
    val accountName: String = "",
    val today: Long = DateEngine.today(),
    val occasions: List<ResolvedOccasion> = emptyList(),
    val events: List<ResolvedUserEvent> = emptyList(),
    val fortuneIndex: Int = 0,
    val quoteIndex: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(c: AppContainer) : BaseViewModel(c) {
    private val defs = c.profile.flatMapLatest { c.calendarRepository.occasionDefs(it.occasionRegionIds) }
    private val events = perAccount { c.calendarRepository.userEvents(it) }
        .map { list -> list.mapNotNull { it.toDef() } }

    val state = combine(
        c.profile, todayFlow(), defs, events, c.accountRepository.activeAccount
    ) { profile, today, defs, events, account ->
        HomeState(
            loaded = account != null,
            accountName = account?.name.orEmpty(),
            today = today,
            occasions = OccasionResolver(profile.engine).resolve(defs, today, today),
            events = UserEventResolver.resolve(events, profile.engine, today, today),
            fortuneIndex = FortuneEngine.fortuneIndex(account?.id ?: 0L, today, FortuneData.couplets.size),
            quoteIndex = FortuneEngine.quoteIndex(account?.id ?: 0L, today, FortuneData.quoteRes.size)
        )
    }.asState(HomeState())

    init {
        // The day's fortune is written to the history as soon as the home screen shows it.
        viewModelScope.launch {
            combine(accountId, todayFlow()) { id, day -> id to day }.collect { (id, day) ->
                if (id != 0L) c.fortuneRepository.record(id, day, FortuneEngine.fortuneIndex(id, day, FortuneData.couplets.size))
            }
        }
    }
}

@Composable
fun HomeScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: HomeViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val profile = LocalProfile.current
    val fmt = rememberDateFormatter(profile)

    if (!state.loaded) {
        Column(Modifier.fillMaxSize()) { SkeletonList(count = 4) }
        return
    }

    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        in 17..20 -> R.string.greeting_evening
        else -> R.string.greeting_night
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = Space.s3),
        verticalArrangement = Arrangement.spacedBy(Space.s1)
    ) {
        item {
            GradientHeader {
                val onHeader = NexoraTheme.colors.onHeader
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(loc(greeting), style = MaterialTheme.typography.titleMedium, color = onHeader)
                        TextButton(onClick = { nav.navigate(Routes.ACCOUNTS) }) {
                            Text(
                                state.accountName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = onHeader,
                                maxLines = 1
                            )
                        }
                    }
                    IconButton(onClick = { nav.navigate(Routes.SEARCH) }) {
                        Icon(Icons.Rounded.Search, stringResource(R.string.search), tint = onHeader)
                    }
                }
                Text(
                    fmt.full(state.today, profile.primary),
                    style = MaterialTheme.typography.titleLarge,
                    color = onHeader
                )
                profile.secondary.forEach { type ->
                    Text(
                        fmt.short(state.today, type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onHeader.copy(alpha = 0.92f)
                    )
                }
            }
        }

        item {
            LayeredCard(Modifier.padding(horizontal = Space.s2), onClick = { nav.goTop(Routes.CALENDAR) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
                    Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary)
                    SectionTitle(stringResource(R.string.today_in_calendar))
                }
                profile.allCalendars.forEach { type ->
                    val label = when (type) {
                        com.mahirex.nexoraplus.calendar.CalendarType.JALALI -> R.string.cal_jalali
                        com.mahirex.nexoraplus.calendar.CalendarType.HIJRI -> R.string.cal_hijri
                        com.mahirex.nexoraplus.calendar.CalendarType.GREGORIAN -> R.string.cal_gregorian
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
                        Text(loc(label) + ":", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(fmt.short(state.today, type), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.occasions.isEmpty() && state.events.isEmpty()) {
                    Text(
                        stringResource(R.string.no_occasions_today),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Space.s1)
                    )
                } else {
                    Column(Modifier.padding(top = Space.s1), verticalArrangement = Arrangement.spacedBy(Space.half)) {
                        OccasionList(state.occasions)
                        state.events.forEach {
                            Text(
                                stringResource(R.string.my_event_prefix, it.def.title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }

        item {
            val (first, second) = FortuneData.couplets[state.fortuneIndex]
            LayeredCard(
                Modifier.padding(horizontal = Space.s2),
                accent = MaterialTheme.colorScheme.tertiary,
                onClick = { nav.navigate(Routes.FORTUNE) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.tertiary)
                    SectionTitle(stringResource(R.string.daily_fortune))
                }
                Text(
                    first + "\n" + second,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Rtl)
                )
                Text(
                    stringResource(FortuneData.meaningRes[state.fortuneIndex]),
                    modifier = Modifier.padding(top = Space.s1),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            LayeredCard(Modifier.padding(horizontal = Space.s2), accent = MaterialTheme.colorScheme.secondary) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
                    Icon(Icons.Rounded.FormatQuote, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(Space.s4))
                    Text(
                        stringResource(FortuneData.quoteRes[state.quoteIndex]),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
