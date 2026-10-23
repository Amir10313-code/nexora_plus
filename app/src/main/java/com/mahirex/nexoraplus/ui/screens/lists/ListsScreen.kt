package com.mahirex.nexoraplus.ui.screens.lists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mahirex.nexoraplus.data.local.entity.ChecklistEntity
import com.mahirex.nexoraplus.data.local.entity.ChecklistItemEntity
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.ConfirmDialog
import com.mahirex.nexoraplus.ui.components.EmptyState
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.SwatchRow
import com.mahirex.nexoraplus.ui.components.loc
import com.mahirex.nexoraplus.ui.designsystem.Space
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ListsViewModel(c: AppContainer) : BaseViewModel(c) {
    val lists = perAccount { c.checklistRepository.lists(it) }.asState(emptyList<ChecklistEntity>())
    val items = perAccount { c.checklistRepository.items(it) }
        .map { all -> all.groupBy { it.checklistId } }
        .asState(emptyMap<Long, List<ChecklistItemEntity>>())

    fun saveList(l: ChecklistEntity) = viewModelScope.launch { c.checklistRepository.saveList(l) }
    fun deleteList(id: Long) = viewModelScope.launch { c.checklistRepository.deleteList(id) }
    fun addItem(listId: Long, text: String, order: Int) = viewModelScope.launch { c.checklistRepository.addItem(listId, text, order) }
    fun setDone(item: ChecklistItemEntity, done: Boolean) = viewModelScope.launch { c.checklistRepository.setItemDone(item, done) }
    fun deleteItem(id: Long) = viewModelScope.launch { c.checklistRepository.deleteItem(id) }
}

@Composable
fun ListsScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: ListsViewModel = viewModel(factory = factory)
    val lists by vm.lists.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val accountId by vm.accountId.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<ChecklistEntity?>(null) }
    var deleting by remember { mutableStateOf<ChecklistEntity?>(null) }

    ScreenScaffold(
        title = stringResource(R.string.lists_title),
        onBack = { nav.popBackStack() },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = ChecklistEntity(accountId = accountId, title = "", colorArgb = Palette.at(lists.size), createdAt = System.currentTimeMillis())
            }) { Icon(Icons.Rounded.Add, stringResource(R.string.add)) }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s1)
        ) {
            if (lists.isEmpty()) item {
                EmptyState(Icons.AutoMirrored.Rounded.PlaylistAddCheck, stringResource(R.string.empty_lists_title), stringResource(R.string.empty_lists_message))
            }
            items(lists.sortedWith(compareByDescending<ChecklistEntity> { it.pinned }.thenByDescending { it.createdAt }), key = { it.id }) { list ->
                ListCard(
                    list, items[list.id].orEmpty(),
                    onAdd = { text -> vm.addItem(list.id, text, (items[list.id]?.maxOfOrNull { it.sortOrder } ?: -1) + 1) },
                    onToggle = { item, done -> vm.setDone(item, done) },
                    onDeleteItem = { vm.deleteItem(it.id) },
                    onPin = { vm.saveList(list.copy(pinned = !list.pinned)) },
                    onEdit = { editing = list },
                    onDelete = { deleting = list }
                )
            }
        }
    }

    editing?.let { l ->
        ListDialog(l, onDismiss = { editing = null }, onSave = { vm.saveList(it); editing = null })
    }
    deleting?.let { l ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            message = stringResource(R.string.confirm_delete_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { vm.deleteList(l.id); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun ListCard(
    list: ChecklistEntity,
    items: List<ChecklistItemEntity>,
    onAdd: (String) -> Unit,
    onToggle: (ChecklistItemEntity, Boolean) -> Unit,
    onDeleteItem: (ChecklistItemEntity) -> Unit,
    onPin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = Color(list.colorArgb)
    val done = items.count { it.done }
    var newText by remember(list.id) { mutableStateOf("") }
    LayeredCard(accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(list.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    loc(R.string.items_done_format, done, items.size),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPin) {
                Icon(
                    Icons.Rounded.PushPin, stringResource(R.string.pin),
                    tint = if (list.pinned) accent else MaterialTheme.colorScheme.outline
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit)) }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error) }
        }
        if (items.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { done / items.size.toFloat() },
                modifier = Modifier.fillMaxWidth().padding(vertical = Space.half),
                color = accent
            )
        }
        items.sortedWith(compareBy<ChecklistItemEntity> { it.done }.thenBy { it.sortOrder }).forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.done, onCheckedChange = { onToggle(item, it) })
                Text(
                    item.text, Modifier.weight(1f),
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { onDeleteItem(item) }) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                newText, { newText = it }, Modifier.weight(1f), singleLine = true,
                placeholder = { Text(stringResource(R.string.add_item_hint)) }
            )
            IconButton(enabled = newText.isNotBlank(), onClick = { onAdd(newText.trim()); newText = "" }) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add))
            }
        }
    }
}

@Composable
private fun ListDialog(initial: ChecklistEntity, onDismiss: () -> Unit, onSave: (ChecklistEntity) -> Unit) {
    var title by remember { mutableStateOf(initial.title) }
    var color by remember { mutableStateOf(initial.colorArgb) }
    var pinned by remember { mutableStateOf(initial.pinned) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == 0L) R.string.add_list else R.string.edit_list)) },
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
