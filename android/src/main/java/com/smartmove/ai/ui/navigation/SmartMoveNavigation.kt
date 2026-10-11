package com.smartmove.ai.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartmove.ai.ui.alerts.AlertsScreen
import com.smartmove.ai.ui.alerts.HazardDetailsScreen
import com.smartmove.ai.ui.alerts.ReportHazardScreen
import com.smartmove.ai.ui.analytics.AnalyticsScreen
import com.smartmove.ai.ui.auth.ForgotPasswordScreen
import com.smartmove.ai.ui.auth.LoginScreen
import com.smartmove.ai.ui.auth.SignupScreen
import com.smartmove.ai.ui.components.SmartMoveCard
import com.smartmove.ai.ui.history.HistoryScreen
import com.smartmove.ai.ui.history.TripDetailsScreen
import com.smartmove.ai.ui.home.HomeScreen
import com.smartmove.ai.ui.profile.ProfileScreen
import com.smartmove.ai.ui.splash.SplashScreen
import com.smartmove.ai.ui.trip.TripScreen
import com.smartmove.ai.ui.trip.TripSummaryScreen
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveTextMuted
import com.smartmove.ai.ui.theme.SmartMoveTextPrimary
import com.smartmove.ai.ui.theme.SmartMoveTextSecondary
import com.smartmove.ai.ui.theme.SmartMoveTheme

import com.smartmove.ai.ui.trip.TripSetupScreen
import com.smartmove.ai.ui.trip.TripUiState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Main Navigation Host for SMARTMOVE AI.
 * Handles transitions between Splash, Login, Signup, Forgot Password, Home, TripSetup, and active Trip.
 */
@Composable
fun SmartMoveNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onStartTripClick: () -> Unit = {},
) {
    var activeTripState by remember { mutableStateOf(TripUiState()) }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier,
    ) {
        // 1. Splash Screen Destination -> Navigates to Home if token valid, else Login
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 2. Login Screen Destination
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Screen.Signup.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
            )
        }

        // 3. Sign Up Screen Destination
        composable(route = Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
            )
        }

        // 4. Forgot Password Screen Destination
        composable(route = Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onNavigateBackToLogin = {
                    navController.popBackStack()
                },
            )
        }

        // 5. Main Home Destination
        composable(route = Screen.Home.route) {
            HomeScreen(
                onStartTripClick = {
                    navController.navigate(Screen.TripSetup.route)
                },
                onViewAnalyticsClick = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Alerts.route)
                },
                onReportHazardClick = {
                    navController.navigate(Screen.ReportHazard.route)
                },
                onOpenWebMapClick = {
                    navController.navigate(Screen.WebMap.route)
                },
            )
        }

        // 5.2 Dedicated Real-Time Google Maps Platform & Trip Web Screen
        composable(route = Screen.WebMap.route) {
            com.smartmove.ai.ui.map.SmartMoveWebScreen()
        }

        // 5.5 Pre-Trip Planner / Setup Screen (GPS, Places Autocomplete, Routes, Participants)
        composable(route = Screen.TripSetup.route) {
            TripSetupScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onStartTripWithData = { configuredState ->
                    activeTripState = configuredState
                    navController.navigate(Screen.Trip.route)
                }
            )
        }

        // Prepared Bottom Navigation Screens (Phase 4+)
        // 6. Trip History Screen Destination
        composable(route = Screen.History.route) {
            HistoryScreen(
                onTripClick = { tripId ->
                    navController.navigate(Screen.TripDetails.createRoute(tripId))
                },
                onStartTripClick = {
                    navController.navigate(Screen.TripSetup.route)
                },
            )
        }

        // 7. Trip Details Screen Destination
        composable(route = Screen.TripDetails.route) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            TripDetailsScreen(
                tripId = tripId,
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }

        composable(route = Screen.Analytics.route) {
            AnalyticsScreen(
                onStartTripClick = {
                    navController.navigate(Screen.TripSetup.route)
                },
            )
        }

        composable(route = Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 8. Active Trip Screen
        composable(route = Screen.Trip.route) {
            TripScreen(
                initialState = activeTripState,
                onBackClick = {
                    navController.popBackStack()
                },
                onEndTripConfirm = {
                    navController.navigate(Screen.TripSummary.route) {
                        popUpTo(Screen.Trip.route) { inclusive = true }
                    }
                },
                onNotificationsClick = {
                    navController.navigate(Screen.Alerts.route)
                },
                onReportHazardClick = {
                    navController.navigate(Screen.ReportHazard.route)
                },
            )
        }

        // 9. Trip Summary Screen
        composable(route = Screen.TripSummary.route) {
            TripSummaryScreen(
                onDoneClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
            )
        }

        // 10. Safety Alerts Screen
        composable(route = Screen.Alerts.route) {
            AlertsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onHazardClick = { hazardId ->
                    navController.navigate(Screen.HazardDetails.createRoute(hazardId))
                },
                onReportHazardClick = {
                    navController.navigate(Screen.ReportHazard.route)
                },
            )
        }

        // 11. Report Hazard Screen
        composable(route = Screen.ReportHazard.route) {
            ReportHazardScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onReportSubmitted = {
                    navController.navigate(Screen.Alerts.route) {
                        popUpTo(Screen.Alerts.route) { inclusive = true }
                    }
                },
            )
        }

        // 12. Hazard Details Screen
        composable(route = Screen.HazardDetails.route) { backStackEntry ->
            val hazardId = backStackEntry.arguments?.getString("hazardId") ?: ""
            HazardDetailsScreen(
                hazardId = hazardId,
                onBackClick = {
                    navController.popBackStack()
                },
            )
        }
    }
}

/**
 * Minimalist placeholder card for prepared routes.
 */
@Composable
private fun ScreenPlaceholder(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SmartMoveBackground)
            .padding(SmartMoveTheme.spacing.default),
        contentAlignment = Alignment.Center,
    ) {
        SmartMoveCard(
            contentPadding = SmartMoveTheme.spacing.large,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = SmartMoveTextPrimary,
            )
            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.small))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = SmartMoveTextSecondary,
            )
            Spacer(modifier = Modifier.height(SmartMoveTheme.spacing.medium))
            Text(
                text = "Planned for future phases.",
                style = MaterialTheme.typography.labelSmall,
                color = SmartMoveTextMuted,
            )
        }
    }
}
