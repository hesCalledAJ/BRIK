package com.alijafari.brik.main.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alijafari.brik.R
import com.alijafari.brik.block.domain.repository.SessionRepository
import com.alijafari.brik.block.framework.AdminManagerReceiver
import com.alijafari.brik.block.framework.BlockService
import com.alijafari.brik.block.framework.BlockService.Companion.EXTRA_DURATION_SECONDS
import com.alijafari.brik.block.framework.BlockService.Companion.INTENT_START
import com.alijafari.brik.main.domain.model.PermissionRequirement
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _sessionActive = mutableStateOf(false)
    val sessionActive: State<Boolean> = _sessionActive

    private val _selectedDuration = mutableIntStateOf(30*60)
    val selectedDuration: State<Int> = _selectedDuration

    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds: StateFlow<Int> = _totalSeconds

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private var totalJob: Job? = null
    private var remainingJob: Job? = null

    fun sessionStart() {
        val serviceIntent = Intent(app, BlockService::class.java)
        serviceIntent.action = INTENT_START
        serviceIntent.putExtra(EXTRA_DURATION_SECONDS, selectedDuration.value)
        _sessionActive.value = true
        app.startService(serviceIntent)
    }

    fun setDuration(duration: Int) {
        _selectedDuration.value = duration
    }

    fun bindSessionRepository(repo: SessionRepository) {
        totalJob?.cancel()
        remainingJob?.cancel()

        totalJob = viewModelScope.launch {
            repo.totalSeconds.collect {
                _totalSeconds.value = it
            }
        }
        remainingJob = viewModelScope.launch {
            repo.remainingSeconds.collect {
                _remainingSeconds.value = it
            }
        }
    }

    fun unbindSessionRepository() {
        totalJob?.cancel()
        remainingJob?.cancel()
        totalJob = null
        remainingJob = null
        _totalSeconds.value = 0
        _remainingSeconds.value = 0
    }

    var missingPermissions by mutableStateOf<List<PermissionRequirement>>(emptyList())
        private set

    @SuppressLint("BatteryLife")
    fun refreshPermissions(context: Activity) {
        val list = mutableListOf<PermissionRequirement>()
        if (!Settings.canDrawOverlays(context)) {
            list.add(
                PermissionRequirement(
                    "Appear on Top",
                    "Needed to block apps effectively.",
                    R.drawable.ic_layer,
                    false
                ) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                })
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComp = ComponentName(context, AdminManagerReceiver::class.java)
        if (!dpm.isAdminActive(adminComp)) {
            list.add(
                PermissionRequirement(
                    "Device Admin",
                    "Prevents the app from being uninstalled.",
                    R.drawable.ic_admin,
                    false
                ) {
                    val comp = ComponentName(context, AdminManagerReceiver::class.java)
                    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                    }
                    context.startActivity(intent)
                })
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                list.add(
                    PermissionRequirement(
                        "Notifications",
                        "Keep you updated on your session status.",
                        R.drawable.ic_notif,
                        false
                    ) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    })
            }
        }
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            list.add(
                PermissionRequirement(
                    "Battery Optimization",
                    "Prevents the system from killing the app.",
                    R.drawable.ic_battery,
                    false
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:${context.packageName}".toUri()
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            )
        }
        missingPermissions = list
    }
}