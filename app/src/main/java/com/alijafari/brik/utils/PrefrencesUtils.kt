package com.alijafari.brik.utils

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesRepository(
    private val context: Context
) {

    private val Context.dataStore by preferencesDataStore(name = "prefs")

    private object PreferencesKeys {
        val SESSION_ENDTIME_MILLIS =
            longPreferencesKey("session_endtime")
        val MIUI_AUTOSTART_WARNED =
            booleanPreferencesKey("miui_autostart_warned")
    }

    suspend fun saveSessionEndtime(endTimeMillis: Long) {
        context.dataStore.edit {
            it[PreferencesKeys.SESSION_ENDTIME_MILLIS] = endTimeMillis
        }
    }
    suspend fun saveMiuiAutoStartWarned(warned : Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.MIUI_AUTOSTART_WARNED] = warned
        }
    }
    fun readMiuiAutoStartWarned(): Flow<Boolean> =
        context.dataStore.data.map {
            it[PreferencesKeys.MIUI_AUTOSTART_WARNED] ?: false
        }
    fun readLastSessionEndTime(): Flow<Long> =
        context.dataStore.data.map {
            it[PreferencesKeys.SESSION_ENDTIME_MILLIS] ?: 0L
        }
}
