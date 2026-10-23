package com.mahirex.nexoraplus

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mahirex.nexoraplus.core.settings.ThemeMode
import com.mahirex.nexoraplus.ui.AppViewModelFactory
import com.mahirex.nexoraplus.ui.components.LocalDigits
import com.mahirex.nexoraplus.ui.components.LocalProfile
import com.mahirex.nexoraplus.ui.nav.NexoraNavHost
import com.mahirex.nexoraplus.ui.theme.NexoraTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as NexoraApp).container
        val factory = AppViewModelFactory(container)

        setContent {
            val settings by container.settingsState.collectAsStateWithLifecycle()
            val profile by container.profile.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings?.themeMode ?: ThemeMode.SYSTEM) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark }
                )
                onDispose { }
            }

            NexoraTheme(darkTheme = dark) {
                if (settings == null) {
                    // Settings are still loading: show an empty surface instead of a wrong-themed flash.
                    Box(Modifier.fillMaxSize().background(Color(if (dark) 0xFF11111A else 0xFFF6F6FB)))
                } else {
                    CompositionLocalProvider(
                        LocalProfile provides profile,
                        LocalDigits provides profile.digits
                    ) {
                        NexoraNavHost(factory)
                    }
                }
            }
        }
    }
}
