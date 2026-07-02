package com.example.jugglersettingtool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.jugglersettingtool.theme.JugglerSettingToolTheme
import com.example.jugglersettingtool.ui.JugglerViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: JugglerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this)[JugglerViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            JugglerSettingToolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(sharedViewModel = viewModel)
                }
            }
        }
    }
}
