package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.AppViewModel
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AddServerScreen
import com.example.ui.screens.AdvancedSettingsScreen
import com.example.ui.screens.EditServerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LogScreen
import com.example.ui.screens.ServerManagementScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onServerManagementClick = { navController.navigate("server_management") },
                                onAboutClick = { navController.navigate("about") },
                                onLogClick = { navController.navigate("logs") },
                                onAdvancedSettingsClick = { navController.navigate("advanced_settings") }
                            )
                        }
                        composable("server_management") {
                            ServerManagementScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() },
                                onAddServerClick = { navController.navigate("add_server") },
                                onEditServerClick = { serverId -> navController.navigate("edit_server/$serverId") }
                            )
                        }
                        composable("add_server") {
                            AddServerScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("edit_server/{serverId}") { backStackEntry ->
                            val serverIdStr = backStackEntry.arguments?.getString("serverId")
                            val serverId = serverIdStr?.toIntOrNull()
                            if (serverId != null) {
                                EditServerScreen(
                                    serverId = serverId,
                                    viewModel = viewModel,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                        }
                        composable("about") {
                            AboutScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("logs") {
                            LogScreen(
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable("advanced_settings") {
                            AdvancedSettingsScreen(
                                viewModel = viewModel,
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
