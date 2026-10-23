package com.mahirex.nexoraplus.ui.screens.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAddCheck
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.designsystem.Space
import com.mahirex.nexoraplus.ui.nav.Routes

private data class MoreItem(val route: String, val label: Int, val icon: ImageVector)

private val moreEntries = listOf(
    MoreItem(Routes.LISTS, R.string.lists_title, Icons.AutoMirrored.Rounded.PlaylistAddCheck),
    MoreItem(Routes.FORTUNE, R.string.daily_fortune, Icons.Rounded.AutoStories),
    MoreItem(Routes.FOCUS, R.string.focus_title, Icons.Rounded.Timer),
    MoreItem(Routes.ACCOUNTS, R.string.accounts_title, Icons.Rounded.People),
    MoreItem(Routes.SEARCH, R.string.search_title, Icons.Rounded.Search),
    MoreItem(Routes.STATS, R.string.stats_title, Icons.Rounded.Insights),
    MoreItem(Routes.SETTINGS, R.string.settings_title, Icons.Rounded.Settings),
    MoreItem(Routes.ABOUT, R.string.about_title, Icons.Rounded.Info)
)

@Composable
fun MoreScreen(nav: NavController) {
    ScreenScaffold(title = stringResource(R.string.nav_more), hasBottomBar = true) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Space.s2),
            verticalArrangement = Arrangement.spacedBy(Space.s1)
        ) {
            items(moreEntries, key = { it.route }) { item ->
                LayeredCard(onClick = { nav.navigate(item.route) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                        Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(item.label), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
