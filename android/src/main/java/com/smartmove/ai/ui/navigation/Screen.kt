package com.smartmove.ai.ui.navigation

/**
 * Type-safe route definitions for SMARTMOVE AI.
 * Centralizes all application destinations and eliminates hard-coded route strings.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val isBottomNavDestination: Boolean = false,
) {
    // Auth Flow
    data object Splash : Screen(route = "splash", title = "Splash")
    data object Login : Screen(route = "login", title = "Sign In")
    data object Signup : Screen(route = "signup", title = "Create Account")
    data object ForgotPassword : Screen(route = "forgot_password", title = "Reset Password")

    // Main App Screens
    data object Home : Screen(route = "home", title = "Home", isBottomNavDestination = true)
    data object WebMap : Screen(route = "web_map", title = "Google Map", isBottomNavDestination = true)
    data object History : Screen(route = "history", title = "Trip History", isBottomNavDestination = true)
    data object Analytics : Screen(route = "analytics", title = "Safety Analytics", isBottomNavDestination = true)
    data object Profile : Screen(route = "profile", title = "User Profile", isBottomNavDestination = true)

    // Trip & Navigation Screens (opened on-demand, not permanent bottom nav)
    data object TripSetup : Screen(route = "trip_setup", title = "Plan Trip")
    data object Trip : Screen(route = "trip", title = "Active Trip")
    data object TripSummary : Screen(route = "trip_summary", title = "Trip Complete")
    data object TripDetails : Screen(route = "trip_details/{tripId}", title = "Trip Details") {
        fun createRoute(tripId: String): String = "trip_details/$tripId"
    }
    data object Alerts : Screen(route = "alerts", title = "Safety Alerts")
    data object ReportHazard : Screen(route = "report_hazard", title = "Report Hazard")
    data object HazardDetails : Screen(route = "hazard_details/{hazardId}", title = "Hazard Details") {
        fun createRoute(hazardId: String): String = "hazard_details/$hazardId"
    }

    companion object {
        val bottomNavScreens = listOf(Home, WebMap, History, Analytics, Profile)

        fun fromRoute(route: String?): Screen {
            return when {
                route == Splash.route -> Splash
                route == Login.route -> Login
                route == Signup.route -> Signup
                route == ForgotPassword.route -> ForgotPassword
                route == Home.route -> Home
                route == WebMap.route -> WebMap
                route == History.route -> History
                route == Analytics.route -> Analytics
                route == Profile.route -> Profile
                route == Trip.route -> Trip
                route == TripSummary.route -> TripSummary
                route != null && route.startsWith("trip_details") -> TripDetails
                route == Alerts.route -> Alerts
                route == ReportHazard.route -> ReportHazard
                route != null && route.startsWith("hazard_details") -> HazardDetails
                else -> Home
            }
        }
    }
}
