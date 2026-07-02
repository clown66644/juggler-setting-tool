package com.example.jugglersettingtool

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.jugglersettingtool.ui.JugglerViewModel
import com.example.jugglersettingtool.ui.screens.AiScreen
import com.example.jugglersettingtool.ui.screens.CalendarScreen
import com.example.jugglersettingtool.ui.screens.CounterScreen
import com.example.jugglersettingtool.ui.screens.ExpectationScreen
import com.example.jugglersettingtool.ui.screens.HistoryScreen
import com.example.jugglersettingtool.ui.screens.SettingsScreen
import com.example.jugglersettingtool.ui.screens.JugglerSimulatorScreen
import com.example.jugglersettingtool.ui.screens.WelcomeScreen

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Counter, "カウント", Icons.Default.Build, "Counter"),
    BottomNavItem(Screen.Expectation, "期待値", Icons.Default.PlayArrow, "Expectation"),
    BottomNavItem(Screen.AiEstimate, "AI推測", Icons.Default.Info, "AI"),
    BottomNavItem(Screen.Calendar, "カレンダー", Icons.Default.DateRange, "Calendar"),
    BottomNavItem(Screen.History, "履歴", Icons.Default.List, "History"),
    BottomNavItem(Screen.Settings, "設定", Icons.Default.Settings, "Settings")
)

@Composable
fun MainNavigation(sharedViewModel: JugglerViewModel) {

    // ウェルカム画面表示フラグがONなら機種選択ウェルカム画面を表示
    if (sharedViewModel.showWelcomeScreen) {
        WelcomeScreen(viewModel = sharedViewModel)
    } else {
        val currentScreen = sharedViewModel.currentScreen
        Scaffold(
            bottomBar = {
                if (currentScreen != Screen.Simulator) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentScreen == item.screen,
                                onClick = { sharedViewModel.navigateTo(item.screen) },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.contentDescription
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (currentScreen == Screen.Simulator) PaddingValues(0.dp) else innerPadding)
            ) {
                when (currentScreen) {
                    Screen.Counter -> CounterScreen(viewModel = sharedViewModel)
                    Screen.Expectation -> ExpectationScreen(viewModel = sharedViewModel)
                    Screen.AiEstimate -> AiScreen(viewModel = sharedViewModel)
                    Screen.Calendar -> CalendarScreen(viewModel = sharedViewModel)
                    Screen.History -> HistoryScreen(viewModel = sharedViewModel)
                    Screen.Settings -> SettingsScreen(viewModel = sharedViewModel)
                    Screen.Simulator -> JugglerSimulatorScreen(viewModel = sharedViewModel)
                }
            }
        }
    }
}
