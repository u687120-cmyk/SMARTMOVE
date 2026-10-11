# Walkthrough - Phase 14: Dynamic Risk Engine

I have implemented the core logic for the SMARTMOVE AI Dynamic Risk Engine. This engine replaces static road labels with a real-time, weighted safety assessment based on current conditions.

## Changes Made

### 1. Backend Risk Logic
- **`riskService.js`**: The heart of the engine. It calculates a risk score (0-100) using a weighted formula:
    - **30% Hazards**: Checks for active nearby potholes, accidents, etc.
    - **25% Traffic**: Uses speed ratios to determine congestion risk.
    - **15% Weather**: Considers precipitation and visibility.
    - **15% Hard Braking**: Analyzes recent user events in the area.
    - **15% Other Events**: Includes historical patterns and reports.
- **`trafficService.js` & `weatherService.js`**: Provided simulated data providers for traffic and weather conditions to support Phase 14.
- **`riskController.js` & `riskRoutes.js`**: Exposed the engine via a new `/api/risk` endpoint, protected by JWT.

### 2. Android Integration
- **`RiskApi.kt` & `RiskRepository.kt`**: Added the networking layer to fetch risk data from the backend.
- **`HomeViewModel.kt`**: Updated the dashboard to fetch real-time risk based on the user's location.
- **`TripViewModel.kt`**: Integrated the risk engine into the active trip experience. The "Simulate Road Change" button now triggers a live backend refresh to demonstrate dynamic safety updates.

### 3. Testing & Scenarios
- **`test-risk.js`**: Created a scenario test suite to verify that:
    - Clear roads result in **LOW** risk.
    - Adding a high-severity hazard increases the risk.
    - Marking a hazard as **RESOLVED** correctly removes its influence from the engine.

## Risk Formula Implemented
```text
Total Risk = (HazardScore * 0.30) + (TrafficScore * 0.25) + (WeatherScore * 0.15) + (BrakingScore * 0.15) + (OtherScore * 0.15)
```

## Risk Level Thresholds
- **LOW**: 0–39
- **MODERATE**: 40–69
- **HIGH**: 70–100

## Verification Results
- **Backend Status**: Engine implemented and routes registered. Scenario tests are ready for local execution.
- **Android Status**: `app:assembleDebug` completed successfully. ViewModels are wired to the real API.

> [!NOTE]
> To run the backend tests locally, ensure your MySQL credentials in `backend/.env` are correct, then run `npm run risk:test`.
