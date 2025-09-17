package com.hwj.cook.data.local

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.coroutines.FlowSettings
import com.russhwolf.settings.coroutines.toFlowSettings
import java.util.prefs.Preferences

actual class SettingsFactory {
    actual fun createSettings(): FlowSettings {
        val prefs = Preferences.userRoot()
        return PreferencesSettings(prefs).toFlowSettings()
    }
}