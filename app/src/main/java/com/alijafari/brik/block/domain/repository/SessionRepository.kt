package com.alijafari.brik.block.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {

    val totalSeconds: StateFlow<Int>
    val remainingSeconds: StateFlow<Int>
    val isSessionActive: StateFlow<Boolean>

    fun startSession(totalSeconds: Int)
    fun stopSession()
    fun updateRemaining(remainingSeconds: Int)
    fun extend(extraMillis : Long)
}