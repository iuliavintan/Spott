package com.example.spott

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.spott.ui.drafts.SpottApp
import com.example.spott.ui.theme.SpottTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as SpottApplication).draftRepository
        setContent { SpottTheme { SpottApp(repository) } }
    }
}
