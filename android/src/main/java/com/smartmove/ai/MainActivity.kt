package com.smartmove.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.smartmove.ai.ui.auth.ForgotPasswordScreen
import com.smartmove.ai.ui.auth.LoginScreen
import com.smartmove.ai.ui.auth.SignupScreen
import com.smartmove.ai.ui.home.HomeScreen
import com.smartmove.ai.ui.splash.SplashScreen
import com.smartmove.ai.ui.theme.SmartMoveBackground
import com.smartmove.ai.ui.theme.SmartMoveTheme

/**
 * SMARTMOVE AI main entry point activity.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartMoveTheme {
                Surface(color = SmartMoveBackground) {
                    SmartMoveApp()
                }
            }
        }
    }
}

@Preview(name = "Splash Preview", showBackground = true)
@Composable
private fun SplashScreenPreview() {
    SmartMoveTheme {
        SplashScreen(onSplashFinished = {})
    }
}

@Preview(name = "Login Preview", showBackground = true)
@Composable
private fun LoginScreenPreview() {
    SmartMoveTheme {
        LoginScreen(
            onLoginSuccess = {},
            onNavigateToSignup = {},
            onNavigateToForgotPassword = {},
        )
    }
}

@Preview(name = "Sign Up Preview", showBackground = true)
@Composable
private fun SignupScreenPreview() {
    SmartMoveTheme {
        SignupScreen(
            onSignupSuccess = {},
            onNavigateToLogin = {},
        )
    }
}

@Preview(name = "Forgot Password Preview", showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    SmartMoveTheme {
        ForgotPasswordScreen(
            onNavigateBackToLogin = {},
        )
    }
}

@Preview(name = "Home Preview", showBackground = true)
@Composable
private fun HomeScreenPreview() {
    SmartMoveTheme {
        HomeScreen()
    }
}

@Preview(name = "Profile Preview", showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    SmartMoveTheme {
        com.smartmove.ai.ui.profile.ProfileScreen(
            onLogout = {},
        )
    }
}

@Preview(name = "History Preview", showBackground = true)
@Composable
private fun HistoryScreenPreview() {
    SmartMoveTheme {
        com.smartmove.ai.ui.history.HistoryScreen(
            onTripClick = {},
            onStartTripClick = {},
        )
    }
}

@Preview(name = "Trip Details Preview", showBackground = true)
@Composable
private fun TripDetailsScreenPreview() {
    SmartMoveTheme {
        com.smartmove.ai.ui.history.TripDetailsScreen(
            tripId = "trip-001",
            onBackClick = {},
        )
    }
}

@Preview(name = "Analytics Preview", showBackground = true)
@Composable
private fun AnalyticsScreenPreview() {
    SmartMoveTheme {
        com.smartmove.ai.ui.analytics.AnalyticsScreen(
            onStartTripClick = {},
        )
    }
}

@Preview(name = "Trip Screen Preview", showBackground = true)
@Composable
private fun TripScreenPreview() {
    SmartMoveTheme {
        com.smartmove.ai.ui.trip.TripScreen(
            onBackClick = {},
            onEndTripConfirm = {},
            onNotificationsClick = {},
        )
    }
}

@Preview(name = "Trip Summary Preview", showBackground = true)
@Composable
private fun TripSummaryScreenPreview() {
    SmartMoveTheme {
        com.smartmove.ai.ui.trip.TripSummaryScreen(
            onDoneClick = {},
        )
    }
}

@Preview(name = "Alerts Preview", showBackground = true)
@Composable
private fun AlertsScreenPreview() {
    SmartMoveTheme {
        com.smartmove.ai.ui.alerts.AlertsScreen(
            onBackClick = {},
            onHazardClick = {},
            onReportHazardClick = {},
        )
    }
}

@Preview(name = "Report Hazard Preview", showBackground = true)
@Composable
private fun ReportHazardScreenPreview() {
    SmartMoveTheme {
        com.smartmove.ai.ui.alerts.ReportHazardScreen(
            onBackClick = {},
            onReportSubmitted = {},
        )
    }
}

@Preview(name = "Hazard Details Preview", showBackground = true)
@Composable
private fun HazardDetailsScreenPreview() {
    SmartMoveTheme {
        com.smartmove.ai.ui.alerts.HazardDetailsScreen(
            hazardId = "hz-001",
            onBackClick = {},
        )
    }
}
