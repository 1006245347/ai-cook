package com.hwj.cook.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.hwj.cook.global.ThemeChatLite
import com.hwj.cook.ui.widget.PlatformAppStart


fun MainViewController() = ComposeUIViewController {

    ThemeChatLite {
        Surface(modifier = Modifier.fillMaxSize()) {
            PlatformAppStart()
        }
    }
}