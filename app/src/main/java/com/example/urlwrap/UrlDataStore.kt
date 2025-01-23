package com.example.urlwrap

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class UrlDataStore(private val context: Context) {
    companion object {
        private val URL_KEY = stringPreferencesKey("webview_url")
    }

    val getUrl: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[URL_KEY] ?: "https://youtube.com" }

    suspend fun saveUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[URL_KEY] = url
        }
    }
}
