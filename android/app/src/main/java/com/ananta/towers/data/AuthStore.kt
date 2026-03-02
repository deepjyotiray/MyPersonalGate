package com.ananta.towers.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("auth")

class AuthStore(private val context: Context) {
    private val TOKEN_KEY = stringPreferencesKey("token")
    private val ROLE_KEY = stringPreferencesKey("role")
    private val NAME_KEY = stringPreferencesKey("name")

    val token: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }
    val role: Flow<String?> = context.dataStore.data.map { it[ROLE_KEY] }
    val name: Flow<String?> = context.dataStore.data.map { it[NAME_KEY] }

    suspend fun saveSession(token: String, role: String, name: String) {
        context.dataStore.edit {
            it[TOKEN_KEY] = token
            it[ROLE_KEY] = role
            it[NAME_KEY] = name
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.clear() }
    }
}
