package com.druanlabs.didicheck.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("didi_settings")

class SettingsStore(private val context: Context) {
    private val notificationsKey = booleanPreferencesKey("gentle_nudge_enabled")
    private val morningKey = booleanPreferencesKey("gentle_morning_enabled")
    private val eveningKey = booleanPreferencesKey("gentle_evening_enabled")
    private val defaultRoutineKey = stringPreferencesKey("default_routine_id")

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[notificationsKey] ?: false }

    val morningReminderEnabled: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[morningKey] ?: (prefs[notificationsKey] ?: false)
        }

    val eveningReminderEnabled: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[eveningKey] ?: (prefs[notificationsKey] ?: false)
        }

    val defaultRoutineId: Flow<String?> =
        context.dataStore.data.map { it[defaultRoutineKey] }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[notificationsKey] = enabled
            it[morningKey] = enabled
            it[eveningKey] = enabled
        }
    }

    suspend fun setMorningReminderEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[morningKey] = enabled
            it[notificationsKey] = enabled || (it[eveningKey] == true)
        }
    }

    suspend fun setEveningReminderEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[eveningKey] = enabled
            it[notificationsKey] = enabled || (it[morningKey] == true)
        }
    }

    suspend fun setDefaultRoutineId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank()) {
                prefs.remove(defaultRoutineKey)
            } else {
                prefs[defaultRoutineKey] = id
            }
        }
    }
}
