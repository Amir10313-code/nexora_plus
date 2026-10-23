package com.mahirex.nexoraplus.ui.screens.accounts

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.core.Palette
import com.mahirex.nexoraplus.data.local.entity.AccountEntity
import com.mahirex.nexoraplus.data.repo.AccountRepository
import com.mahirex.nexoraplus.di.AppContainer
import com.mahirex.nexoraplus.ui.BaseViewModel
import com.mahirex.nexoraplus.ui.components.ConfirmDialog
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.SwatchRow
import com.mahirex.nexoraplus.ui.components.loc
import com.mahirex.nexoraplus.ui.designsystem.Space
import kotlinx.coroutines.launch

class AccountsViewModel(c: AppContainer) : BaseViewModel(c) {
    val accounts = c.accountRepository.accounts.asState(emptyList<AccountEntity>())
    fun select(id: Long) = viewModelScope.launch { c.accountRepository.setActive(id) }
    fun create(name: String, color: Int) = viewModelScope.launch { c.accountRepository.create(name, color) }
    fun update(a: AccountEntity) = viewModelScope.launch { c.accountRepository.update(a) }
    fun delete(id: Long) = viewModelScope.launch { c.accountRepository.delete(id) }
}

@Composable
fun AccountsScreen(nav: NavController, factory: ViewModelProvider.Factory) {
    val vm: AccountsViewModel = viewModel(factory = factory)
    val accounts by vm.accounts.collectAsStateWithLifecycle()
    val activeId by vm.accountId.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<AccountEntity?>(null) }
    var deleting by remember { mutableStateOf<AccountEntity?>(null) }
    var limitInfo by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(R.string.accounts_title),
        onBack = { nav.popBackStack() },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (accounts.size >= AccountRepository.MAX_ACCOUNTS) limitInfo = true
                else editing = AccountEntity(name = "", colorArgb = Palette.at(accounts.size), createdAt = 0L, sortOrder = accounts.size)
            }) { Icon(Icons.Rounded.Add, stringResource(R.string.add)) }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s1)
        ) {
            item {
                Text(
                    loc(R.string.accounts_count_format, accounts.size, AccountRepository.MAX_ACCOUNTS),
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(accounts, key = { it.id }) { a ->
                val selected = a.id == activeId
                LayeredCard(accent = Color(a.colorArgb), onClick = { vm.select(a.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(Space.s5).clip(CircleShape).background(Color(a.colorArgb)), contentAlignment = Alignment.Center) {
                            Text(a.name.trim().take(1).uppercase(), color = Color.White, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(Modifier.weight(1f).padding(horizontal = Space.s2)) {
                            Text(a.name, style = MaterialTheme.typography.titleMedium)
                            if (selected) Text(stringResource(R.string.account_active), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { editing = a }) { Icon(Icons.Rounded.Edit, stringResource(R.string.edit)) }
                        if (accounts.size > 1) {
                            IconButton(onClick = { deleting = a }) {
                                Icon(Icons.Rounded.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { a ->
        var name by remember(a.id) { mutableStateOf(a.name) }
        var color by remember(a.id) { mutableStateOf(a.colorArgb) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(stringResource(if (a.id == 0L) R.string.add_account else R.string.edit_account)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.s1)) {
                    OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text(stringResource(R.string.account_name)) })
                    SwatchRow(selected = color, onSelect = { color = it })
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    if (a.id == 0L) vm.create(name, color) else vm.update(a.copy(name = name.trim(), colorArgb = color))
                    editing = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    deleting?.let { a ->
        ConfirmDialog(
            title = stringResource(R.string.delete),
            message = stringResource(R.string.confirm_delete_account_message),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { vm.delete(a.id); deleting = null },
            onDismiss = { deleting = null }
        )
    }
    if (limitInfo) {
        AlertDialog(
            onDismissRequest = { limitInfo = false },
            text = { Text(stringResource(R.string.accounts_limit_reached)) },
            confirmButton = { TextButton(onClick = { limitInfo = false }) { Text(stringResource(R.string.ok)) } }
        )
    }
}
