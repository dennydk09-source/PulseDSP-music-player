package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.example.ai.configureAppCheck
import com.example.ui.PulseApp
import com.example.ui.theme.PulseDspTheme
import com.example.viewmodel.PulseViewModel

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or ViewModel creation.
        configureAppCheck(this, intent)

        // STEP 2: Full edge-to-edge dark Neon Cyber-Audio system bars.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            PulseDspTheme {
                val pulseViewModel: PulseViewModel = viewModel()
                PulseApp(viewModel = pulseViewModel)
            }
        }
    }
}
