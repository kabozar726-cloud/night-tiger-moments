package com.nighttiger.moments

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme

class MainActivity : ComponentActivity() {

    private val viewModel: MomentsViewModel by viewModels()

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.onVideoPicked(this, uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Gold,
                    background = DarkBg,
                    surface = CardBg,
                    onBackground = LightText,
                    onSurface = LightText
                )
            ) {
                NightTigerScreen(
                    viewModel = viewModel,
                    onPickVideo = { pickVideo.launch("video/*") }
                )
            }
        }
    }
}
