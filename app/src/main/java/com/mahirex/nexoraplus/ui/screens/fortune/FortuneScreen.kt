package com.mahirex.nexoraplus.ui.screens.fortune

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.data.fortune.FortuneData
import com.mahirex.nexoraplus.data.fortune.FortuneEngine
import com.mahirex.nexoraplus.data.local.entity.FortuneHistoryEntity
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.LocalProfile
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.SectionTitle
import com.mahirex.nexoraplus.ui.components.rememberDateFormatter
import com.mahirex.nexoraplus.ui.designsystem.Space

class FortuneViewModel(c: AppContainer) : BaseViewModel(c) {
    val history = perAccount { c.fortuneRepository.history(it) }.asState(emptyList<FortuneHistoryEntity>())
    val today = todayFlow().asState(DateEngine.today())
}

@Composable
fun FortuneScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: FortuneViewModel = viewModel(factory = factory)
    val history by vm.history.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val accountId by vm.accountId.collectAsStateWithLifecycle()
    val fmtr = rememberDateFormatter(LocalProfile.current)
    val todayIndex = FortuneEngine.fortuneIndex(accountId, today, FortuneData.couplets.size)
    val past = history.filter { it.epochDay != today }

    ScreenScaffold(title = stringResource(R.string.daily_fortune), onBack = { nav.popBackStack() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s1)
        ) {
            item {
                FortuneCard(fmtr.full(today, LocalProfile.current.primary), todayIndex, big = true)
            }
            item { Text(stringResource(R.string.fortune_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (past.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.fortune_history)) }
                items(past, key = { it.id }) { h ->
                    FortuneCard(fmtr.short(h.epochDay), h.fortuneIndex.coerceIn(0, FortuneData.couplets.size - 1), big = false)
                }
            }
        }
    }
}

@Composable
private fun FortuneCard(dateLabel: String, index: Int, big: Boolean) {
    val (first, second) = FortuneData.couplets[index]
    LayeredCard(accent = MaterialTheme.colorScheme.tertiary) {
        Text(dateLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            first, Modifier.fillMaxWidth().padding(top = Space.s1),
            style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
        Text(
            second, Modifier.fillMaxWidth(),
            style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
        Text(
            stringResource(R.string.fortune_meaning_label), Modifier.padding(top = Space.s1),
            style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary
        )
        Text(stringResource(FortuneData.meaningRes[index]), style = MaterialTheme.typography.bodyMedium)
    }
}
