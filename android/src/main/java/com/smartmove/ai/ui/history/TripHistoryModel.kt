package com.smartmove.ai.ui.history

import com.smartmove.ai.ui.components.RiskLevel

/**
 * Filter intervals for trip history.
 */
enum class HistoryFilter(val label: String) {
    ALL("ALL"),
    TODAY("TODAY"),
    WEEK("WEEK"),
    MONTH("MONTH");
}

/**
 * A single point along a trip's risk timeline demonstrating dynamic condition changes.
 */
data class RiskTimelinePoint(
    val stage: String,           // e.g. "Start", "Highway", "City Core", "Arrival"
    val time: String,            // e.g. "10:32 AM"
    val riskScore: Int,          // 0-100
    val riskLevel: RiskLevel,
    val note: String? = null,
)

/**
 * Comprehensive data model for a completed trip.
 * Designed to seamlessly transition to SQL/Room/REST API backend entities in future phases.
 */
data class TripHistoryItem(
    val id: String,
    val startLocation: String,
    val destination: String,
    val dateDisplay: String,
    val daysAgo: Int,            // 0 = Today, 1-6 = This week, 7-29 = This month, >=30 = Older
    val startTime: String,
    val endTime: String,
    val distanceKm: Double,
    val durationMinutes: Int,
    val averageRisk: Int,
    val riskLevel: RiskLevel,
    val safetyScore: Int,        // 0-100 (Higher = safer)
    val warningCount: Int,
    val hazardCount: Int,
    val hardBrakingCount: Int,
    val trafficCondition: String,
    val weatherCondition: String,
    val timeline: List<RiskTimelinePoint> = emptyList(),
)

/**
 * UI State for the Trip History Screen.
 */
data class HistoryUiState(
    val allTrips: List<TripHistoryItem> = emptyList(),
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    /**
     * Dynamically filters trips based on the selected time period.
     */
    val filteredTrips: List<TripHistoryItem>
        get() = when (selectedFilter) {
            HistoryFilter.ALL -> allTrips
            HistoryFilter.TODAY -> allTrips.filter { it.daysAgo == 0 }
            HistoryFilter.WEEK -> allTrips.filter { it.daysAgo <= 6 }
            HistoryFilter.MONTH -> allTrips.filter { it.daysAgo <= 29 }
        }
}

/**
 * Realistic demo data provider covering multiple time periods and varied driving conditions.
 */
object TripHistoryDataProvider {

    fun getTrips(filter: HistoryFilter): List<TripHistoryItem> {
        return HistoryUiState(allTrips = getDemoTrips(), selectedFilter = filter).filteredTrips
    }

    fun getDemoTrips(): List<TripHistoryItem> = listOf(
        // TODAY (3 trips)
        TripHistoryItem(
            id = "trip-001",
            startLocation = "Pune",
            destination = "Hinjewadi",
            dateDisplay = "Today",
            daysAgo = 0,
            startTime = "10:32 AM",
            endTime = "11:04 AM",
            distanceKm = 24.6,
            durationMinutes = 32,
            averageRisk = 24,
            riskLevel = RiskLevel.LOW,
            safetyScore = 92,
            warningCount = 1,
            hazardCount = 0,
            hardBrakingCount = 2,
            trafficCondition = "Low",
            weatherCondition = "Clear",
            timeline = listOf(
                RiskTimelinePoint("Start", "10:32 AM", 18, RiskLevel.LOW, "Smooth departure"),
                RiskTimelinePoint("Highway Exit", "10:45 AM", 28, RiskLevel.LOW, "Moderate traffic flow"),
                RiskTimelinePoint("IT Park Congestion", "10:55 AM", 46, RiskLevel.MEDIUM, "Minor queue at junction"),
                RiskTimelinePoint("Arrival", "11:04 AM", 16, RiskLevel.LOW, "Arrived safely"),
            ),
        ),
        TripHistoryItem(
            id = "trip-002",
            startLocation = "Hinjewadi",
            destination = "Kothrud",
            dateDisplay = "Today",
            daysAgo = 0,
            startTime = "01:15 PM",
            endTime = "01:43 PM",
            distanceKm = 14.2,
            durationMinutes = 28,
            averageRisk = 42,
            riskLevel = RiskLevel.MEDIUM,
            safetyScore = 78,
            warningCount = 2,
            hazardCount = 1,
            hardBrakingCount = 3,
            trafficCondition = "Moderate",
            weatherCondition = "Clear",
            timeline = listOf(
                RiskTimelinePoint("Start", "01:15 PM", 22, RiskLevel.LOW),
                RiskTimelinePoint("Bypass Junction", "01:25 PM", 54, RiskLevel.MEDIUM, "Pothole detected"),
                RiskTimelinePoint("Chandani Chowk", "01:35 PM", 48, RiskLevel.MEDIUM, "Braking wave"),
                RiskTimelinePoint("Arrival", "01:43 PM", 26, RiskLevel.LOW),
            ),
        ),
        TripHistoryItem(
            id = "trip-003",
            startLocation = "Kothrud",
            destination = "Deccan Gymkhana",
            dateDisplay = "Today",
            daysAgo = 0,
            startTime = "04:30 PM",
            endTime = "04:45 PM",
            distanceKm = 8.5,
            durationMinutes = 15,
            averageRisk = 18,
            riskLevel = RiskLevel.LOW,
            safetyScore = 96,
            warningCount = 0,
            hazardCount = 0,
            hardBrakingCount = 0,
            trafficCondition = "Low",
            weatherCondition = "Clear",
            timeline = listOf(
                RiskTimelinePoint("Start", "04:30 PM", 14, RiskLevel.LOW),
                RiskTimelinePoint("FC Road", "04:38 PM", 20, RiskLevel.LOW),
                RiskTimelinePoint("Arrival", "04:45 PM", 16, RiskLevel.LOW),
            ),
        ),

        // THIS WEEK (3 trips: daysAgo 2, 3, 4)
        TripHistoryItem(
            id = "trip-004",
            startLocation = "Pune",
            destination = "Chakan MIDC",
            dateDisplay = "2 days ago",
            daysAgo = 2,
            startTime = "08:10 AM",
            endTime = "09:05 AM",
            distanceKm = 38.0,
            durationMinutes = 55,
            averageRisk = 68,
            riskLevel = RiskLevel.HIGH,
            safetyScore = 62,
            warningCount = 4,
            hazardCount = 2,
            hardBrakingCount = 5,
            trafficCondition = "Heavy",
            weatherCondition = "Rain",
            timeline = listOf(
                RiskTimelinePoint("Start", "08:10 AM", 30, RiskLevel.LOW),
                RiskTimelinePoint("Nashik Phata", "08:25 AM", 74, RiskLevel.HIGH, "Heavy rain & braking"),
                RiskTimelinePoint("Bhosari Flyover", "08:42 AM", 65, RiskLevel.HIGH, "Waterlogging on left lane"),
                RiskTimelinePoint("Arrival", "09:05 AM", 40, RiskLevel.MEDIUM),
            ),
        ),
        TripHistoryItem(
            id = "trip-005",
            startLocation = "Shivajinagar",
            destination = "Viman Nagar",
            dateDisplay = "3 days ago",
            daysAgo = 3,
            startTime = "11:20 AM",
            endTime = "11:40 AM",
            distanceKm = 12.0,
            durationMinutes = 20,
            averageRisk = 22,
            riskLevel = RiskLevel.LOW,
            safetyScore = 94,
            warningCount = 0,
            hazardCount = 0,
            hardBrakingCount = 1,
            trafficCondition = "Low",
            weatherCondition = "Clear",
            timeline = listOf(
                RiskTimelinePoint("Start", "11:20 AM", 18, RiskLevel.LOW),
                RiskTimelinePoint("Airport Road", "11:30 AM", 24, RiskLevel.LOW),
                RiskTimelinePoint("Arrival", "11:40 AM", 15, RiskLevel.LOW),
            ),
        ),
        TripHistoryItem(
            id = "trip-006",
            startLocation = "Pune",
            destination = "Lonavala",
            dateDisplay = "4 days ago",
            daysAgo = 4,
            startTime = "06:00 AM",
            endTime = "07:02 AM",
            distanceKm = 65.3,
            durationMinutes = 62,
            averageRisk = 52,
            riskLevel = RiskLevel.MEDIUM,
            safetyScore = 74,
            warningCount = 2,
            hazardCount = 1,
            hardBrakingCount = 3,
            trafficCondition = "Moderate",
            weatherCondition = "Foggy",
            timeline = listOf(
                RiskTimelinePoint("Start", "06:00 AM", 20, RiskLevel.LOW),
                RiskTimelinePoint("Expressway Toll", "06:20 AM", 45, RiskLevel.MEDIUM),
                RiskTimelinePoint("Khandala Ghat", "06:45 AM", 70, RiskLevel.HIGH, "Dense morning fog"),
                RiskTimelinePoint("Arrival", "07:02 AM", 35, RiskLevel.MEDIUM),
            ),
        ),

        // THIS MONTH (3 trips: daysAgo 12, 18, 25)
        TripHistoryItem(
            id = "trip-007",
            startLocation = "Hadapsar",
            destination = "Magarpatta City",
            dateDisplay = "12 days ago",
            daysAgo = 12,
            startTime = "09:40 AM",
            endTime = "10:02 AM",
            distanceKm = 6.8,
            durationMinutes = 22,
            averageRisk = 25,
            riskLevel = RiskLevel.LOW,
            safetyScore = 91,
            warningCount = 1,
            hazardCount = 0,
            hardBrakingCount = 1,
            trafficCondition = "Moderate",
            weatherCondition = "Clear",
            timeline = listOf(
                RiskTimelinePoint("Start", "09:40 AM", 20, RiskLevel.LOW),
                RiskTimelinePoint("Magarpatta Gate", "09:52 AM", 32, RiskLevel.MEDIUM),
                RiskTimelinePoint("Arrival", "10:02 AM", 18, RiskLevel.LOW),
            ),
        ),
        TripHistoryItem(
            id = "trip-008",
            startLocation = "Pune",
            destination = "Talegaon",
            dateDisplay = "18 days ago",
            daysAgo = 18,
            startTime = "02:15 PM",
            endTime = "03:03 PM",
            distanceKm = 35.5,
            durationMinutes = 48,
            averageRisk = 72,
            riskLevel = RiskLevel.HIGH,
            safetyScore = 58,
            warningCount = 5,
            hazardCount = 2,
            hardBrakingCount = 6,
            trafficCondition = "Heavy",
            weatherCondition = "Rain",
            timeline = listOf(
                RiskTimelinePoint("Start", "02:15 PM", 35, RiskLevel.MEDIUM),
                RiskTimelinePoint("Dehu Road", "02:35 PM", 78, RiskLevel.HIGH, "Multiple potholes reported"),
                RiskTimelinePoint("Talegaon Toll", "02:50 PM", 68, RiskLevel.HIGH),
                RiskTimelinePoint("Arrival", "03:03 PM", 42, RiskLevel.MEDIUM),
            ),
        ),
        TripHistoryItem(
            id = "trip-009",
            startLocation = "Baner",
            destination = "Wakad",
            dateDisplay = "25 days ago",
            daysAgo = 25,
            startTime = "05:10 PM",
            endTime = "05:36 PM",
            distanceKm = 11.2,
            durationMinutes = 26,
            averageRisk = 38,
            riskLevel = RiskLevel.MEDIUM,
            safetyScore = 82,
            warningCount = 1,
            hazardCount = 0,
            hardBrakingCount = 2,
            trafficCondition = "Moderate",
            weatherCondition = "Clear",
            timeline = listOf(
                RiskTimelinePoint("Start", "05:10 PM", 25, RiskLevel.LOW),
                RiskTimelinePoint("Bhumkar Chowk", "05:22 PM", 50, RiskLevel.MEDIUM),
                RiskTimelinePoint("Arrival", "05:36 PM", 28, RiskLevel.LOW),
            ),
        ),

        // OLDER (1 trip: daysAgo 45)
        TripHistoryItem(
            id = "trip-010",
            startLocation = "Pune",
            destination = "Satara",
            dateDisplay = "45 days ago",
            daysAgo = 45,
            startTime = "07:00 AM",
            endTime = "08:45 AM",
            distanceKm = 112.0,
            durationMinutes = 105,
            averageRisk = 26,
            riskLevel = RiskLevel.LOW,
            safetyScore = 90,
            warningCount = 2,
            hazardCount = 1,
            hardBrakingCount = 3,
            trafficCondition = "Low",
            weatherCondition = "Clear",
            timeline = listOf(
                RiskTimelinePoint("Start", "07:00 AM", 20, RiskLevel.LOW),
                RiskTimelinePoint("Shirwal Toll", "07:45 AM", 28, RiskLevel.LOW),
                RiskTimelinePoint("Khambatki Ghat", "08:15 AM", 45, RiskLevel.MEDIUM, "Ghat section caution"),
                RiskTimelinePoint("Arrival", "08:45 AM", 18, RiskLevel.LOW),
            ),
        ),
    )
}
