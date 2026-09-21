package com.cyclediary.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cyclediary.app.ui.CycleDiaryApp
import com.cyclediary.app.ui.theme.CycleDiaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CycleDiaryTheme {
                CycleDiaryApp()
            }
        }
    }
}
