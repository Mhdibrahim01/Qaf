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
        setContent {
            WordBooksTheme {
                WordBooksAppShell()
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
