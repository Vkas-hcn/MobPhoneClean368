package com.carefree.and.joyous

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.carefree.and.joyous.ui.theme.MobPhoneClean368Theme
import com.carefree.and.joyous.ui.CleanPictureScreen

class CleanPictureCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MobPhoneClean368Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CleanPictureScreen(
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}