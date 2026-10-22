package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.navigation.WordBooksAppShell
import com.example.ui.theme.WordBooksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighestSupportedRefreshRate()
        setContent {
            WordBooksTheme {
                WordBooksAppShell()
            }
        }
    }

    /** Ask Android for the fastest mode available at the display's current resolution. */
    @Suppress("DEPRECATION")
    private fun requestHighestSupportedRefreshRate() {
        val display = windowManager.defaultDisplay ?: return
        val currentMode = display.mode ?: return
        val fastestMode = display.supportedModes
            .asSequence()
            .filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
            .maxByOrNull { it.refreshRate }
            ?: return

        if (fastestMode.refreshRate > currentMode.refreshRate) {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = fastestMode.modeId
                preferredRefreshRate = fastestMode.refreshRate
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WordBooksPreview() {
    WordBooksTheme {
        WordBooksAppShell()
    }
}
