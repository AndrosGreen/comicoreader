package com.androsgreen.comicreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.androsgreen.comicreader.ui.ComicReaderApp
import com.androsgreen.comicreader.ui.theme.ComicReaderTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ComicReaderTheme {
                ComicReaderApp()
            }
        }
    }
}
