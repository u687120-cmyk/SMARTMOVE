# Android Application — SMARTMOVE AI

This module is the Android application for SMARTMOVE AI, built using Kotlin, Jetpack Compose, and Material 3.

## Status: Phase 9 Complete (Alerts + Hazards Reporting)

Phase 9 establishes the complete **SMARTMOVE AI Alerts & Hazard Reporting** suite, featuring prioritized road hazards, confidence scoring, community verification (`YES, STILL THERE` / `FIXED`), deterministic lifecycle progression (`ACTIVE` → `UNDER_REVIEW` → `RESOLVED`), streamlined user reporting, and unified hazard integration across Home, Trip, and Alerts screens.

### Architectural Layout

```text
com.smartmove.ai/
├── MainActivity.kt                      # Main Activity entry point with Compose previews
├── SmartMoveApp.kt                      # Top-level scaffold orchestrating navigation and bottom bar
│
└── ui/
    ├── theme/
    │   ├── Color.kt                     # Design tokens (#0B1220 background, #2563EB primary, etc.)
    │   ├── Type.kt                      # Typography hierarchy (display, headline, body, label)
    │   ├── Shape.kt                     # Corner radii (buttons: 12dp, cards: 16dp)
    │   ├── Spacing.kt                   # Spacing tokens (4dp, 8dp, 12dp, 16dp, 24dp, 48dp touch)
    │   └── Theme.kt                     # Material 3 dark color scheme & custom composition locals
    │
    ├── components/
    │   ├── SmartMoveButton.kt           # Primary button with loading indicator & 52dp touch target
    │   ├── SmartMoveOutlinedButton.kt   # Secondary outlined button
    │   ├── SmartMoveCard.kt             # Automotive-styled container card with subtle border
    │   ├── SmartMoveTopBar.kt           # Reusable app bar with branding & back navigation
    │   ├── RiskStatusCard.kt            # Hero safety card (🟢 LOW / 🟡 MEDIUM / 🔴 HIGH)
    │   ├── ActiveAlertCard.kt           # Real-time alert card (Safe confirmation / Hazard alert)
    │   ├── SectionTitle.kt              # Clean section title with optional action
    │   └── SmartMoveBottomNavigation.kt # Bottom navigation (Home, History, Analytics, Profile)
    │
    ├── auth/
    │   ├── AuthComponents.kt            # SmartMoveTextField, SmartMovePasswordField, AuthHeader, PasswordStrengthIndicator
    │   ├── AuthValidation.kt            # Pure validation logic (email regex, password length, strength)
    │   ├── LoginScreen.kt               # Login screen with validation, loading indicator, and demo auth
    │   ├── SignupScreen.kt              # Sign Up screen with vehicle selection and terms agreement
    │   └── ForgotPasswordScreen.kt      # Password recovery with confirmation success state
    │
    ├── home/
    │   ├── HomeUiState.kt               # Decoupled UI state models (Alerts, Conditions, Overview)
    │   └── HomeScreen.kt                # Real Home Dashboard connected directly to HazardRepository
    │
    ├── history/
    │   ├── TripHistoryModel.kt          # TripHistoryItem, RiskTimelinePoint, HistoryFilter, and demo dataset
    │   ├── TripHistoryCard.kt           # Reusable history item card
    │   ├── HistoryScreen.kt             # History screen with period filter chips and LazyColumn
    │   └── TripDetailsScreen.kt         # Comprehensive Trip Details with dynamic risk timeline
    │
    ├── analytics/
    │   ├── AnalyticsModel.kt            # AnalyticsPeriod, RiskBreakdown, AnalyticsTrendPoint, and AnalyticsUiState
    │   ├── AnalyticsCalculator.kt       # Pure dynamic aggregation layer (SUM, AVG, COUNT, ratios)
    │   └── AnalyticsScreen.kt           # Safety Analytics Screen with period selector and trend visualizers
    │
    ├── trip/
    │   ├── TripModel.kt                 # RoutePoint, MapHazard, RoadAheadItem, and TripUiState
    │   ├── SimulatedMap.kt              # Lightweight Canvas road map with vehicle, route, and hazards
    │   ├── TripScreen.kt                # Active Trip Screen with telemetry, road risk, and end action
    │   └── TripSummaryScreen.kt         # Post-trip summary showing completed journey metrics
    │
    ├── alerts/
    │   ├── HazardModel.kt               # HazardType, HazardStatus, HazardItem, and HazardUiState
    │   ├── HazardRepository.kt          # In-memory central repository with confidence/lifecycle engine
    │   ├── HazardAlertCard.kt           # Reusable card for active alerts
    │   ├── AlertsScreen.kt              # Prioritized active alerts list & My Reports
    │   ├── ReportHazardScreen.kt        # Rapid hazard reporting UI
    │   └── HazardDetailsScreen.kt       # Full hazard intelligence & community verification buttons
    │
    ├── profile/
    │   ├── ProfileUiState.kt            # Decoupled profile state & distance units enum
    │   └── ProfileScreen.kt             # Complete Profile Screen with edit dialogs & logout flow
    │
    ├── navigation/
    │   ├── Screen.kt                    # Type-safe route definitions (including alerts, report_hazard, hazard_details/{id})
    │   └── SmartMoveNavigation.kt       # Navigation host with all prepared destinations
    │
    └── splash/
        └── SplashScreen.kt              # Splash screen with auto-transition to Login
```

### Alerts & Hazards Features

1. **Hazard Repository & Lifecycle Engine**:
   - Single source of truth for hazards across Home, Alerts, and Active Trip.
   - Lifecycle: `ACTIVE` → `UNDER_REVIEW` → `RESOLVED`.
   - Confidence scoring: Initial report 60%. Community confirmations (`YES, STILL THERE`) increase confidence (+10%). Fixed reports (`FIXED / GONE`) decrease confidence (-20%).
   - When confidence drops below 50%: `UNDER_REVIEW`.
   - When fixed reports reach threshold ($\ge 2$ or $\le 20\%$): `RESOLVED`.
   - Resolved hazards are removed from active broadcasts but preserved for historical analysis.
2. **Prioritized Alerts Screen**:
   - Sorted by severity: `HIGH` $\to$ `MEDIUM` $\to$ `LOW`, then by confidence/recency.
   - Features `MY REPORTS` section showing user-submitted hazards and lifecycle badges.
3. **Rapid Hazard Reporting Screen**:
   - Quick selection across 6 hazard types: `🕳️ Pothole`, `💥 Accident`, `🚧 Road Blockage`, `🏗️ Construction`, `🚗 Heavy Traffic`, `⚠️ Other`.
   - Severity selection: `LOW`, `MEDIUM`, `HIGH`.
   - Simulated location and optional description.
   - Submission immediately updates active broadcasts.
4. **Hazard Details & Community Verification**:
   - Comprehensive telemetry, location, confidence bar, and lifecycle status.
   - Community action buttons: `[ YES, STILL THERE ]` and `[ FIXED / GONE ]`.
5. **Home & Active Trip Cross-Integration**:
   - Home's `ROAD ALERTS` card dynamically reflects top active hazard from `HazardRepository`.
   - Active Trip screen provides a direct `+ REPORT` shortcut to report hazards on the road.
