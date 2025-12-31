package com.alijafari.brik.block.framework

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.alijafari.brik.R
import com.alijafari.brik.main.presentation.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationHelper(private val context: Context) {

    companion object {
        const val NOTIFICATION_ID = 6969
        const val NOTIFICATION_CHANNEL_ID = "foreground_service_channel"
        private const val CHANNEL_NAME = "Block Foreground Service"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun buildNotification(remainingSeconds: Int, totalSeconds: Int): Notification {
        ensureChannel()

        val progress = if (totalSeconds > 0) totalSeconds - remainingSeconds else 0
        val percentage = if (totalSeconds > 0) (progress * 100) / totalSeconds else 0

        val minutesLeft = remainingSeconds / 60
        val secondsLeft = remainingSeconds % 60
        val timeText = String.format(Locale.getDefault(), "%02d:%02d remaining", minutesLeft, secondsLeft)

        val endTime = System.currentTimeMillis() + (remainingSeconds * 1000L)
        val endString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTime))

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_service", true)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(context, 0, intent, flags)

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus Session Active ($percentage%)")
            .setContentText(timeText)
            .setSubText("Ends at $endString")
            .setContentIntent(pending)
            .setProgress(totalSeconds, progress, false)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setFullScreenIntent(pending,true)
            .setShowWhen(false)
            .build()
    }

    fun updateNotification(remainingSeconds: Int, totalSeconds: Int) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(remainingSeconds, totalSeconds))
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows progress and time remaining for the active focus session"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }
}