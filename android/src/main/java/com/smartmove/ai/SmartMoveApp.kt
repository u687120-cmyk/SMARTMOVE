package com.smartmove.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartmove.ai.ui.components.BottomNavDestination
import com.smartmove.ai.ui.components.SmartMoveBottomNavigation
import com.smartmove.ai.ui.navigation.Screen
import com.smartmove.ai.ui.navigation.SmartMoveNavHost
import com.smartmove.ai.ui.theme.SmartMoveBackground

/**
 * Top-level application composable for SMARTMOVE AI.
 * Orchestrates the navigation host, bottom navigation bar visibility, and screen transitions.
 */
@Composable
fun SmartMoveApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Splash.route

    // Show bottom navigation only on designated main screens (Home, History, Analytics, Profile)
    val shouldShowBottomBar = Screen.bottomNavScreens.any { it.route == currentRoute }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartMoveBackground),
        containerColor = SmartMoveBackground,
        bottomBar = {
            if (shouldShowBottomBar) {
                SmartMoveBottomNavigation(
                    currentRoute = currentRoute,
                    onNavigateToDestination = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SmartMoveNavHost(
                navController = navController,
                onStartTripClick = {
                    // Prepared route transition for future trip phase
                    navController.navigate(Screen.Trip.route)
                },
            )
        }
    }
}
