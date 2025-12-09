package com.alainmtz.work_group_tasks.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alainmtz.work_group_tasks.ui.settings.Theme
import com.alainmtz.work_group_tasks.ui.settings.ThemeSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val themeSettings = ThemeSettings(application)

    val theme = themeSettings.theme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Theme.SYSTEM
        )

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            themeSettings.setTheme(theme)
        }
    }
}
