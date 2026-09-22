package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AvatarMainScreen
import com.example.ui.CharacterSelectionScreen
import com.example.ui.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AvatarViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AvatarAppNavigation()
                }
            }
        }
    }
}

@Composable
fun AvatarAppNavigation() {
    val navController = rememberNavController()
    val viewModel: AvatarViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "selection"
    ) {
        composable("selection") {
            CharacterSelectionScreen(
                viewModel = viewModel,
                onStartConversation = { navController.navigate("conversation") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("conversation") {
            AvatarMainScreen(
                viewModel = viewModel,
                onBackToSelection = { navController.popBackStack() },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

