package com.iwsocorp.vobynotes.core.data.anki

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.iwsocorp.vobynotes.MainActivity
import com.iwsocorp.vobynotes.R
import com.iwsocorp.vobynotes.ui.widget.OPEN_FRAGMENT

private const val EXPORT_CHANNEL_ID = "export_anki_channel"
private const val EXPORT_NOTIFICATION_ID = 1001
const val OPEN_ANKI = "open_anki"

fun NotificationManager.updateProgress(
    context: Context,
    title: String,
    message: String,
    progress: Int
) {
    val channel = NotificationChannel(
        EXPORT_CHANNEL_ID,
        "Export Anki",
        NotificationManager.IMPORTANCE_LOW
    ).apply {
        description = "Export progress to Anki"
    }

    createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, EXPORT_CHANNEL_ID)
        .setSmallIcon(R.drawable.vocabulens_logo)
        .setContentTitle(title)
        .setContentText(message)
        .setContentIntent(pendingIntent(context))
        .setOnlyAlertOnce(true)
        .setProgress(100, progress, false)
        .setOngoing(progress in 0..99)
        .build()

    notify(EXPORT_NOTIFICATION_ID, notification)
}

fun NotificationManager.showExportFinished(
    context: Context
) {
    val notification = NotificationCompat.Builder(context, EXPORT_CHANNEL_ID)
        .setSmallIcon(R.drawable.vocabulens_logo)
        .setContentTitle("Export finished")
        .setContentText("All notes were successfully sent to Anki")
        .setContentIntent(pendingIntent(context))
        .setAutoCancel(true)
        .build()

    notify(EXPORT_NOTIFICATION_ID, notification)
}

fun NotificationManager.showExportFailed(context: Context, error: String) {
    val notification = NotificationCompat.Builder(context, EXPORT_CHANNEL_ID)
        .setSmallIcon(R.drawable.vocabulens_logo)
        .setContentTitle("Export failed")
        .setContentText(error)
        .setContentIntent(pendingIntent(context))
        .setAutoCancel(true)
        .setOngoing(false)
        .build()

    notify(EXPORT_NOTIFICATION_ID, notification)
}

private fun pendingIntent(context: Context): PendingIntent? {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(OPEN_FRAGMENT, OPEN_ANKI)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    return pendingIntent
}