package com.alainmtz.work_group_tasks.ui.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class Theme {
    LIGHT,
    DARK,
    SYSTEM
}

class ThemeSettings(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")

    val theme: Flow<Theme> = context.dataStore.data
        .map { preferences ->
            Theme.valueOf(preferences[themeKey] ?: Theme.SYSTEM.name)
        }

    suspend fun setTheme(theme: Theme) {
        context.dataStore.edit {
            it[themeKey] = theme.name
        }
    }
}
