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
const val OPEN_SEARCH = "open_search"
const val OPEN_SCAN = "open_scan"

class SearchWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
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

            // Klik untuk SearchFragment
            val searchIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(OPEN_FRAGMENT, OPEN_SEARCH)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val searchPending = PendingIntent.getActivity(
                context, appWidgetId + 1, searchIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setOnClickPendingIntent(R.id.widget_root, searchPending)

            // Klik kamera → ScanFragment
            val scanIntent = Intent(context, MainActivity::class.java).apply {
                putExtra(OPEN_FRAGMENT, OPEN_SCAN)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val scanPending = PendingIntent.getActivity(
                context, appWidgetId + 2, scanIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            views.setOnClickPendingIntent(R.id.widget_camera, scanPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}