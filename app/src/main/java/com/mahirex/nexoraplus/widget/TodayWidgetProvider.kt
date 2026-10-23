package com.mahirex.nexoraplus.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.mahirex.nexoraplus.MainActivity
import com.mahirex.nexoraplus.NexoraApp
import com.mahirex.nexoraplus.R
import com.mahirex.nexoraplus.calendar.DateEngine
import com.mahirex.nexoraplus.core.locale.LocaleUtil
import com.mahirex.nexoraplus.occasions.OccasionResolver
import com.mahirex.nexoraplus.ui.components.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Home-screen widget: today's date in the profile's calendars and today's occasions. */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        refresh(context, manager, appWidgetIds)
    }

    private fun refresh(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val app = context.applicationContext as NexoraApp
        val pending = goAsync()
        app.container.scope.launch(Dispatchers.Default) {
            try {
                val container = app.container
                val profile = container.profile.value
                val localized = LocaleUtil.localizedContext(context.applicationContext, profile.language)
                val formatter = DateFormatter(
                    profile,
                    localized.resources.getStringArray(R.array.jalali_months),
                    localized.resources.getStringArray(R.array.hijri_months)
                )
                val today = DateEngine.today()
                val primaryLine = formatter.full(today, profile.primary)
                val secondaryLine = profile.secondary.joinToString(" · ") { formatter.short(today, it) }

                val defs = container.calendarRepository.occasionDefs(profile.occasionRegionIds).first()
                val titles = OccasionResolver(profile.engine).resolve(defs, today, today)
                    .map { it.def.title(profile.language.base) }
                val occasionLine = if (titles.isEmpty()) localized.getString(R.string.widget_no_occasion)
                else titles.take(2).joinToString(" · ")

                val open = PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                for (id in ids) {
                    val views = RemoteViews(context.packageName, R.layout.widget_today)
                    views.setTextViewText(R.id.widget_date, primaryLine)
                    views.setTextViewText(R.id.widget_secondary, secondaryLine)
                    views.setTextViewText(R.id.widget_occasion, occasionLine)
                    views.setOnClickPendingIntent(R.id.widget_root, open)
                    manager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Redraws every placed widget (called when settings change). */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(android.content.ComponentName(context, TodayWidgetProvider::class.java))
            if (ids.isNotEmpty()) TodayWidgetProvider().refresh(context, manager, ids)
        }
    }
}
