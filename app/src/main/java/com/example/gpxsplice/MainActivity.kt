package com.example.gpxsplice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.gpxsplice.ui.GpxSplitApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GpxSplitApp(
                    document = null,
                    onPickFile = {},
                    onShareFiles = {},
                    onShareZip = {},
                    errorMessage = null,
                )
            }
        }
    }
}
