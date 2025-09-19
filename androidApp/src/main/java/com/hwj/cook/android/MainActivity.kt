package com.hwj.cook.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import com.hwj.cook.ui.widget.PlatformAppStart
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        FileKit.init(this)

        setContentView(ComposeView(this).apply {
            consumeWindowInsets=false
            setContent {
                Surface (Modifier.fillMaxSize()){
                    PlatformAppStart()
                }
            }
        })
    }
}


