package com.mahirex.nexoraplus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.occasions.OccasionCategory
import com.mahirex.nexoraplus.occasions.ResolvedOccasion
import com.mahirex.nexoraplus.ui.designsystem.Space
import com.mahirex.nexoraplus.ui.theme.NexoraTheme

@Composable
fun categoryLabel(category: OccasionCategory): String = loc(
    when (category) {
        OccasionCategory.NATIONAL -> R.string.cat_national
        OccasionCategory.RELIGIOUS -> R.string.cat_religious
        OccasionCategory.GLOBAL -> R.string.cat_global
        OccasionCategory.OFFICIAL -> R.string.cat_official
    }
)

/** Occasions of one day: the category on the start side, holidays in the holiday colour. */
@Composable
fun OccasionList(items: List<ResolvedOccasion>, modifier: Modifier = Modifier) {
    val language = LocalProfile.current.language.base
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.half)) {
        items.forEach { r ->
            val def = r.def
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Space.s1)) {
                Text(
                    categoryLabel(def.category) + ":",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Space.half / 2)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        def.title(language),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (def.holiday) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (def.holiday) NexoraTheme.colors.holiday else MaterialTheme.colorScheme.onSurface,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (def.holiday || !def.verified) {
                        val tag = buildList {
                            if (def.holiday) add(loc(R.string.holiday))
                            if (!def.verified) add(loc(R.string.unverified))
                        }.joinToString(" · ")
                        Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

