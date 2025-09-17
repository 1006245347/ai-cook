package com.hwj.cook.except

import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.coroutines.FlowSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@OptIn(ExperimentalSettingsApi::class)
class DataSettings : KoinComponent {
    val settingsCache: FlowSettings by inject()
}