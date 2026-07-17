package com.localchat.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private object Keys {
        val OLLAMA_HOST = stringPreferencesKey("ollama_host")
        val LAST_MODEL = stringPreferencesKey("last_model")
    }

    companion object {
        const val DEFAULT_HOST = "http://127.0.0.1:11434"
    }

    val ollamaHost: Flow<String> = context.dataStore.data.map { it[Keys.OLLAMA_HOST] ?: DEFAULT_HOST }
    val lastModel: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_MODEL] }

    suspend fun setOllamaHost(host: String) {
        context.dataStore.edit { it[Keys.OLLAMA_HOST] = host.trimEnd('/') }
    }

    suspend fun setLastModel(model: String) {
        context.dataStore.edit { it[Keys.LAST_MODEL] = model }
    }
}
