package com.darkxvenom.airbeats.ui.component

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.nameDataStore by preferencesDataStore("user_name_preferences")

@Singleton
class NamePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val NAME_SET_KEY = stringPreferencesKey("name_set")
    }

    val userName: Flow<String> = context.nameDataStore.data
        .map { preferences ->
            preferences[USER_NAME_KEY] ?: ""
        }

    val isNameSet: Flow<Boolean> = context.nameDataStore.data
        .map { preferences ->
            preferences[NAME_SET_KEY]?.toBoolean() ?: false
        }

    suspend fun saveUserName(name: String) {
        context.nameDataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
            preferences[NAME_SET_KEY] = "true"
        }
    }
}