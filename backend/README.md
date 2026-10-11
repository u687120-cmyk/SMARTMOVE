# SMARTMOVE AI — Backend API

This module is the backend REST API server for SMARTMOVE AI, built using Node.js and Express.js.

## Status: Phase 19 Complete (Final System Integration & SIH Demo Readiness)

Phase 19 delivers **FINAL SYSTEM INTEGRATION & SIH DEMO READINESS**:
- **End-to-End System Integration**: Complete verified pipeline connecting Android (Kotlin/Compose) $\leftrightarrow$ Node.js/Express $\leftrightarrow$ MySQL database.
- **Deterministic SIH Demonstration Mode**: Fully reproducible journey from Origin $\to$ Dynamic Risk Scoring $\to$ Route Recommendation $\to$ Live Navigation $\to$ Environmental Deterioration $\to$ Safety Prompt $\to$ Safer Corridor Switching $\to$ Trip Completion $\to$ Actionable Driving Insights.
- **Comprehensive Regression Suite**: 8 distinct test suites covering unit algorithms, meteorological calls, traffic modeling, multi-route scoring, en-route rerouting, trip completion, full integration, and live REST endpoints.
- **Security & User Isolation**: JWT-backed authentication, driver data isolation, zero exposed credentials, and protected configuration.

Endpoints:
- `GET /api/health`
- `POST /api/auth/signup`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/users/me`
- `POST /api/trips`, `GET /api/trips`, `GET /api/trips/:id`
- `GET /api/analytics`
- `GET /api/hazards/active`, `POST /api/hazards`
- `GET /api/weather/current`
- `GET /api/traffic/current`
- `GET /api/risk/current`
- `POST /api/routes/recommend`
- `POST /api/routes/recalculate`
- `POST /api/routes/switch`

---

## Prerequisites

- **Node.js**: `v18+` (Detected: `v24.15.0`)
- **npm**: `v9+` (Detected: `11.12.1`)

---

## Project Structure

```text
backend/
├── src/
│   ├── config/
│   │   └── environment.js          # Environment variables & runtime settings
│   ├── controllers/
│   │   ├── healthController.js     # Health check handler
│   │   ├── authController.js       # Auth endpoint placeholders
│   │   ├── userController.js       # User profile placeholders
│   │   ├── tripController.js       # Trip logging placeholders
│   │   ├── hazardController.js     # Hazard reporting placeholders
│   │   └── analyticsController.js  # Safety analytics placeholders
│   ├── middleware/
│   │   ├── errorHandler.js         # Centralized error handler
│   │   └── notFoundHandler.js      # 404 handler for unmatched routes
│   ├── routes/
│   │   ├── healthRoutes.js         # GET /api/health
│   │   ├── authRoutes.js           # /api/auth
│   │   ├── userRoutes.js           # /api/users
│   │   ├── tripRoutes.js           # /api/trips
│   │   ├── hazardRoutes.js         # /api/hazards
│   │   └── analyticsRoutes.js      # /api/analytics
│   ├── services/
│   │   ├── healthService.js        # Health diagnostic service
│   │   ├── authService.js          # Auth service placeholder
│   │   ├── userService.js          # User service placeholder
│   │   ├── tripService.js          # Trip service placeholder
│   │   ├── hazardService.js        # Hazard service placeholder
│   │   └── analyticsService.js     # Analytics service placeholder
│   ├── app.js                      # Express application setup & middleware
│   └── server.js                   # Server entrypoint & graceful shutdown
│
├── .env.example                    # Environment template
├── .gitignore                      # Git ignore rules for node_modules and .env
├── package.json                    # Dependencies and scripts
└── README.md                       # Backend documentation
```

---

## Installation & Setup

1. Open your terminal and navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Install dependencies:
   ```bash
   npm install
   # On Windows PowerShell if script execution is restricted:
   npm.cmd install
   ```

3. Create your `.env` file from `.env.example`:
   ```bash
   cp .env.example .env
   # Or on Windows PowerShell:
   Copy-Item .env.example .env
   ```

4. Run the development server (with nodemon):
   ```bash
   npm run dev
   # Or on Windows:
   npm.cmd run dev
   ```

   Or run in production mode:
   ```bash
   npm start
   # Or on Windows:
   npm.cmd start
   ```

---

## Verification & API Endpoints

### 1. Health Check Endpoint
- **URL**: `GET http://localhost:5000/api/health`
- **Response**:
  ```json
  {
    "success": true,
    "message": "SMARTMOVE AI backend is running",
    "environment": "development",
    "timestamp": "2026-09-01T13:50:00.000Z"
  }
  ```

### 2. Unknown Route (404 Handler)
- **URL**: `GET http://localhost:5000/api/unknown-endpoint`
- **Response**:
  ```json
  {
    "success": false,
    "message": "Route not found: GET /api/unknown-endpoint"
  }
  ```

### 3. Prepared Future Endpoints (HTTP 501)
Endpoints under `/api/auth`, `/api/users`, `/api/trips`, `/api/hazards`, and `/api/analytics` return a standard HTTP 501:
```json
{
  "success": false,
  "message": "This endpoint will be implemented in a later phase."
}
```

---

## Current Limitations

- Database connectivity is not yet implemented (scheduled for **Phase 11: MySQL**).
- Authentication, JWT verification, and password hashing will be implemented in **Phase 12**.
- Weather, Traffic, and Dynamic Risk Engine integrations will follow in subsequent phases.

---

## Upcoming Backend Roadmap

- **Phase 11**: MySQL Database Schema & Migrations
- **Phase 12**: Connect Android ↔ REST Backend (Authentication, Trips, Hazards)
- **Phase 13**: Dynamic Risk Engine Integration
- **Phase 14**: Dynamic Weather Integration
- **Phase 15**: Dynamic Traffic Integration
