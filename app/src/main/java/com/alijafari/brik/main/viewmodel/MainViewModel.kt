package com.alijafari.brik.main.viewmodel

import android.Manifest
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alijafari.brik.BRIK
import com.alijafari.brik.R
import com.alijafari.brik.block.domain.repository.SessionRepository
import com.alijafari.brik.block.framework.AdminManagerReceiver
import com.alijafari.brik.block.framework.BlockService
import com.alijafari.brik.block.framework.BlockService.Companion.EXTRA_DURATION_SECONDS
import com.alijafari.brik.block.framework.BlockService.Companion.INTENT_START
import com.alijafari.brik.main.domain.model.PermissionRequirement
import com.alijafari.brik.utils.PermissionEvent
import com.alijafari.brik.utils.PermissionType
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class MainViewModel(private val context: Application) : AndroidViewModel(context) {

    private val app = context as BRIK

    private val _sessionActive = mutableStateOf(false)
    val sessionActive: State<Boolean> = _sessionActive

    private val _selectedDuration = mutableIntStateOf(30 * 60)
    val selectedDuration: State<Int> = _selectedDuration

    private val _totalSeconds = MutableStateFlow(0)
    val totalSeconds: StateFlow<Int> = _totalSeconds

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds

    private var activeJob: Job? = null
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
        _selectedDuration.intValue = duration
    }

    fun bindSessionRepository(repo: SessionRepository) {
        totalJob?.cancel()
        activeJob?.cancel()
        remainingJob?.cancel()

        activeJob = viewModelScope.launch {
            repo.isSessionActive.collect {
                _sessionActive.value = it
            }
        }
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

    private val _permissionEvent = Channel<PermissionEvent>()
    val permissionEvent = _permissionEvent.receiveAsFlow()

    var missingPermissions by mutableStateOf<List<PermissionRequirement>>(emptyList())
        private set

    fun refreshPermissions() {
        viewModelScope.launch {
            val list = mutableListOf<PermissionRequirement>()

            if (isMiUi()) {
                val hasWarned = app.preferencesRepository.readMiuiAutoStartWarned().first()
                if (!hasWarned) {
                    list.add(
                        PermissionRequirement(
                            PermissionType.MIUI_AUTO_START,
                            "Auto Start",
                            "Needed to resume session on device reboot.",
                            R.drawable.ic_layer
                        ) {
                            onPermissionClicked(
                                PermissionType.MIUI_AUTO_START
                            )
                        }
                    )
                }
            }

            if (!Settings.canDrawOverlays(app)) {
                list.add(
                    PermissionRequirement(
                        PermissionType.OVERLAY,
                        "Appear on Top",
                        "Needed to block apps effectively.",
                        R.drawable.ic_layer
                    ) {
                        onPermissionClicked(
                            PermissionType.OVERLAY
                        )
                    }
                )
            }

            val dpm = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComp = ComponentName(app, AdminManagerReceiver::class.java)
            if (!dpm.isAdminActive(adminComp)) {
                list.add(
                    PermissionRequirement(
                        PermissionType.DEVICE_ADMIN,
                        "Device Admin",
                        "Prevents the app from being uninstalled.",
                        R.drawable.ic_admin
                    ) {
                        onPermissionClicked(
                            PermissionType.DEVICE_ADMIN
                        )
                    }
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    list.add(
                        PermissionRequirement(
                            PermissionType.NOTIFICATIONS,
                            "Notifications",
                            "Keep you updated on your session status.",
                            R.drawable.ic_notif
                        ) {
                            onPermissionClicked(
                                PermissionType.NOTIFICATIONS
                            )
                        })
                }
            }

            val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(app.packageName)) {
                list.add(
                    PermissionRequirement(
                        PermissionType.BATTERY_OPTIMIZATION,
                        "Battery Optimization",
                        "Prevents the system from killing the app.",
                        R.drawable.ic_battery
                    ) {
                        onPermissionClicked(PermissionType.BATTERY_OPTIMIZATION)
                    })
            }

            missingPermissions = list
        }
    }

    fun onPermissionClicked(type: PermissionType) {
        viewModelScope.launch {
            if (type == PermissionType.MIUI_AUTO_START) {
                app.preferencesRepository.saveMiuiAutoStartWarned(true)
                _permissionEvent.send(PermissionEvent.ShowToast("Please grant Auto Start permission"))
            }
            _permissionEvent.send(PermissionEvent.LaunchIntent(type))
        }
    }

    private fun isMiUi() = android.os.Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
}