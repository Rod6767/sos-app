package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.SosScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SosViewModel

class MainActivity : ComponentActivity() {
    private val sosViewModel: SosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                SosScreen(
                    viewModel = sosViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

