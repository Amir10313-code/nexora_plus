package com.mahirex.nexoraplus.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.mahirex.nexoraplus.data.repo.SearchHit
import com.mahirex.nexoraplus.data.repo.SearchKind
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.EmptyState
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.designsystem.Space
import com.mahirex.nexoraplus.ui.nav.Routes
import com.mahirex.nexoraplus.ui.nav.goTop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class SearchViewModel(c: AppContainer) : BaseViewModel(c) {
    val query = MutableStateFlow("")

    val results = combine(query.debounce(250), c.accountRepository.activeAccountId) { q, id -> q.trim() to id }
        .flatMapLatest { (q, id) ->
            flow {
                if (q.length < 2 || id == 0L) emit(emptyList<SearchHit>())
                else emit(c.searchRepository.search(id, q))
            }
        }
        .asState(emptyList<SearchHit>())
}

@Composable
fun SearchScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: SearchViewModel = viewModel(factory = factory)
    val query by vm.query.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.search_title), onBack = { nav.popBackStack() }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = { vm.query.value = it },
                modifier = Modifier.fillMaxWidth().padding(Space.s2), singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) }
            )
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = Space.s2, vertical = Space.s1),
                verticalArrangement = Arrangement.spacedBy(Space.s1)
            ) {
                if (query.trim().length >= 2 && results.isEmpty()) item {
                    EmptyState(Icons.Rounded.Search, stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_message))
                }
                items(results, key = { it.kind.name + it.id }) { hit ->
                    LayeredCard(onClick = {
                        when (hit.kind) {
                            SearchKind.TASK, SearchKind.GOAL, SearchKind.HABIT -> nav.goTop(Routes.TASKS)
                            SearchKind.LIST, SearchKind.LIST_ITEM -> nav.navigate(Routes.LISTS)
                            SearchKind.EVENT -> nav.goTop(Routes.CALENDAR)
                        }
                    }) {
                        Text(kindLabel(hit.kind), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(hit.title, style = MaterialTheme.typography.titleSmall)
                        if (hit.detail.isNotBlank()) Text(hit.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun kindLabel(kind: SearchKind): String = stringResource(
    when (kind) {
        SearchKind.TASK -> R.string.kind_task
        SearchKind.GOAL -> R.string.kind_goal
        SearchKind.HABIT -> R.string.kind_habit
        SearchKind.LIST -> R.string.kind_list
        SearchKind.LIST_ITEM -> R.string.kind_list_item
        SearchKind.EVENT -> R.string.kind_event
    }
)
