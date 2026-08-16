package com.stride.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.stride.app.navigation.StrideNavHost
import com.stride.core.designsystem.StrideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrideApp()
        }
    }
}

@Composable
private fun StrideApp() {
    StrideTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            StrideNavHost(navController = navController)
        }
    }
}
