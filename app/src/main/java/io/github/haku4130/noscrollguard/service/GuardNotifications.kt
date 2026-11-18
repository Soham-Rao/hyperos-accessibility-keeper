package io.github.haku4130.noscrollguard.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import io.github.haku4130.noscrollguard.Consent
import io.github.haku4130.noscrollguard.GuardApp
import io.github.haku4130.noscrollguard.ui.MainActivity

object GuardNotifications {
    const val ID_ONGOING = 1
    private const val ONGOING = "keeper_running"
    private const val EVENTS = "keeper_events"
    fun ensureChannels(context: Context) {
        context.getSystemService(NotificationManager::class.java).apply {
            createNotificationChannel(NotificationChannel(ONGOING, "Accessibility Keeper", NotificationManager.IMPORTANCE_LOW))
            createNotificationChannel(NotificationChannel(EVENTS, "Recovery activity", NotificationManager.IMPORTANCE_LOW))
        }
    }
    private fun open(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    fun ongoing(context: Context): Notification = NotificationCompat.Builder(context, ONGOING)
        .setContentTitle("Accessibility Keeper")
        .setContentText(when {
            !Consent.accepted(context) -> "Recovery is off • review safety to continue"
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED -> "Recovery permission missing • tap for setup"
            GuardApp.pauseState(context).isPaused() -> "Protection paused • tap for controls"
            else -> "Monitoring your selected apps • tap for controls"
        })
        .setOnlyAlertOnce(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setSmallIcon(android.R.drawable.ic_lock_idle_lock).setContentIntent(open(context)).setOngoing(true).build()
    fun refreshOngoing(context: Context) {
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (manager.activeNotifications.any { it.id == ID_ONGOING }) manager.notify(ID_ONGOING, ongoing(context))
        }
    }
    fun notifyRepair(context: Context, text: String, openPackage: String? = null) {
        val launch = openPackage?.let { context.packageManager.getLaunchIntentForPackage(it) }
        val tap = launch?.let { PendingIntent.getActivity(context, 10, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) } ?: open(context)
        val notification = NotificationCompat.Builder(context, EVENTS)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentTitle("Accessibility recovery").setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_dialog_info).setContentIntent(tap).setAutoCancel(true).build()
        runCatching { context.getSystemService(NotificationManager::class.java).notify(2, notification) }
    }
}
