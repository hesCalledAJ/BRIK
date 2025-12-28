package com.alijafari.brik.block.framework

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.alijafari.brik.block.domain.repository.SessionRepository
import com.alijafari.brik.block.helpers.OverlayManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BlockService : Service(), SessionRepository {
    companion object {
        const val INTENT_START = "start"
        const val EXTRA_DURATION_SECONDS = "duration"
    }

    private val binder = LocalBinder()
    private lateinit var notificationHelper: NotificationHelper

    inner class LocalBinder : Binder() {
        fun getService(): BlockService = this@BlockService
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private val _isSessionActive = MutableStateFlow(false)
    private val _totalSeconds = MutableStateFlow(0)
    private val _remainingSeconds = MutableStateFlow(0)
    override val totalSeconds: StateFlow<Int> = _totalSeconds
    override val isSessionActive: StateFlow<Boolean> = _isSessionActive
    override val remainingSeconds: StateFlow<Int> = _remainingSeconds

    val sessionTimer: SessionTimerImpl = SessionTimerImpl()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY
        if (intent.action == INTENT_START) {
            val duration = intent.getIntExtra(EXTRA_DURATION_SECONDS, 60)
            OverlayManager(applicationContext).startOverlay()
            startSession(duration)
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notificationHelper.buildNotification(_remainingSeconds.value, _totalSeconds.value)
            )
        }
        return START_STICKY
    }

    override fun startSession(totalSeconds: Int) {
        _totalSeconds.value = totalSeconds
        _isSessionActive.value = true
        _remainingSeconds.value = totalSeconds

        sessionTimer.start(totalSeconds * 1000L)
        sessionTimer.addOnTickListener { millis ->
            val remaining = (millis / 1000).toInt()
            _remainingSeconds.value = remaining
            notificationHelper.updateNotification(remaining, _totalSeconds.value)
        }
    }

    override fun stopSession() {
        sessionTimer.stop()
        _isSessionActive.value = false
        _totalSeconds.value = 0
        _remainingSeconds.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun updateRemaining(remainingSeconds: Int) {}

    override fun extend(extraMillis: Long) {
        sessionTimer.extend(extraMillis)
    }
}