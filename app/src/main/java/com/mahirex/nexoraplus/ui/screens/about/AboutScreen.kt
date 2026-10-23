package com.mahirex.nexoraplus.ui.screens.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.ui.components.InfoRow
import com.mahirex.nexoraplus.ui.components.LayeredCard
import com.mahirex.nexoraplus.ui.components.ScreenScaffold
import com.mahirex.nexoraplus.ui.components.localizeDigits
import com.mahirex.nexoraplus.ui.designsystem.Space

@Composable
fun AboutScreen(nav: NavController) {
    val context = LocalContext.current
    val version = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
    }
    ScreenScaffold(title = stringResource(R.string.about_title), onBack = { nav.popBackStack() }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(Space.s2),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Space.s1)
        ) {
            LayeredCard {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.about_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                InfoRow(stringResource(R.string.about_version), localizeDigits(version))
            }
            LayeredCard(accent = MaterialTheme.colorScheme.secondary) {
                Text(stringResource(R.string.about_offline), style = MaterialTheme.typography.bodyMedium)
            }
            LayeredCard(accent = MaterialTheme.colorScheme.tertiary) {
                Text(stringResource(R.string.about_sources), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
