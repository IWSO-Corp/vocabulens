package com.iwsocorp.vobynotes.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R

const val OPEN_FRAGMENT = "open_fragment"
const val SEARCH = "search"

class SearchWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // update semua widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_search)

            // Intent untuk membuka MainActivity + flag tujuan fragment
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(OPEN_FRAGMENT, SEARCH)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Klik root widget → buka SearchFragment
            views.setOnClickPendingIntent(R.id.widget_search_root, pendingIntent)

            // Apply ke widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

}
