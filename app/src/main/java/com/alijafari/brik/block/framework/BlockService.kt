package com.alijafari.brik.block.framework

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.alijafari.brik.R
import com.alijafari.brik.block.domain.repository.SessionRepository
import com.alijafari.brik.block.helpers.OverlayManager
import com.alijafari.brik.main.presentation.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class BlockService : Service(), SessionRepository {
    companion object {
        const val INTENT_START = "start"
        const val EXTRA_DURATION_SECONDS = "duration"
        const val NOTIFICATION_ID = 6969
        const val NOTIFICATION_CHANNEL_ID = "foreground_service_channel"
        private const val CHANNEL_NAME = "Block Foreground Service"
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): BlockService = this@BlockService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private val _totalSeconds = MutableStateFlow(0)
    private val _remainingSeconds = MutableStateFlow(0)
    override val totalSeconds: StateFlow<Int> = _totalSeconds
    override val remainingSeconds: StateFlow<Int> = _remainingSeconds

    val sessionTimer: SessionTimerImpl = SessionTimerImpl()

    private val notificationManager: NotificationManager
        get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY
        if (intent.action == INTENT_START) {
            val duration = intent.getIntExtra(EXTRA_DURATION_SECONDS, 60)
            OverlayManager(applicationContext).startOverlay()
            startSession(duration)
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        return START_STICKY
    }

    override fun startSession(totalSeconds: Int) {
        _totalSeconds.value = totalSeconds
        _remainingSeconds.value = totalSeconds

        sessionTimer.start(totalSeconds * 1000L)
        sessionTimer.addOnTickListener { millis ->
            _remainingSeconds.value = (millis / 1000).toInt()
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    override fun stopSession() {
        sessionTimer.stop()
        _totalSeconds.value = 0
        _remainingSeconds.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun updateRemaining(remainingSeconds: Int) {

    }

    override fun extend(extraMillis: Long) {
        sessionTimer.extend(extraMillis)
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        val remaining = _remainingSeconds.value
        val contentText = if (remaining > 0) "$remaining seconds remaining" else "Session inactive"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_service", true)
        }
        val flags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(this, 0, intent, flags)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Blocking session")
            .setContentText(contentText)
            .setContentIntent(pending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Essential channel for blocking session" }
        notificationManager.createNotificationChannel(channel)
    }
}