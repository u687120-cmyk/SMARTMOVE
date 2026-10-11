# SMARTMOVE AI — Database Architecture & Schema

This directory contains the relational database definition for SMARTMOVE AI, powered by **MySQL 8.0+** and the InnoDB engine.

---

## 1. Database Specifications

- **Database Name**: `smartmove_ai`
- **Default Port**: `3306`
- **Engine**: `InnoDB` (ACID compliance & Foreign Key enforcement)
- **Character Set**: `utf8mb4`
- **Collation**: `utf8mb4_unicode_ci`

---

## 2. Entity-Relationship Diagram (Text)

```text
users
 │
 ├── vehicles (ON DELETE CASCADE)
 │
 ├── trips (ON DELETE CASCADE)
 │     ├── vehicle_id (ON DELETE SET NULL)
 │     │
 │     └── risk_events (ON DELETE CASCADE)
 │
 └── hazards (reported_by ON DELETE SET NULL)
       │
       └── hazard_confirmations (ON DELETE CASCADE)
             └── user_id (ON DELETE CASCADE)
```

---

## 3. Core Database Principles

### Dynamic Road Risk (Not Static Roads)
Traditional navigation systems often flag entire roads with permanent labels like "safe" or "dangerous". SMARTMOVE AI rejects static classification. 
Road risk is **inherently dynamic and time-dependent**:
- At 10:00 AM on a sunny morning, a road is **LOW RISK (24/100)**.
- At 5:30 PM with peak commuter congestion, the same road is **MEDIUM RISK (48/100)**.
- At 9:00 PM during heavy rainfall with reported potholes, it becomes **HIGH RISK (72/100)**.

The database stores **trips**, **hazards**, and time-stamped **risk_events** so the dynamic risk engine can calculate real-time road conditions.

### Hazard Lifecycle (No Permanent Deletion)
When a road hazard (e.g. a pothole) is fixed by road crews:
- It transitions from `ACTIVE` $\to$ `UNDER_REVIEW` $\to$ `RESOLVED`.
- It is **never permanently deleted** from the database.
- Historical records remain for telemetry auditing, city infrastructure analytics, and route safety history.
- Live routing queries filter on:
  ```sql
  WHERE status = 'ACTIVE'
  ```

### Real Timestamps (Not Relative Offsets)
The mobile frontend used mock `daysAgo` offsets during prototyping. The production database schema uses real `DATETIME` and `TIMESTAMP` columns (`started_at`, `ended_at`, `reported_at`, `last_confirmed_at`) for accurate analytics calculations and time-series aggregation.

---

## 4. Table Definitions

### 1. `users`
- Primary identity record for drivers.
- Passwords stored strictly as secure cryptographic hashes (`password_hash`), never plaintext.

### 2. `vehicles`
- Registered vehicles belonging to users (`Car`, `SUV`, `Motorcycle`, `Truck`, `Commercial`).
- Foreign Key: `user_id -> users.id` with `ON DELETE CASCADE`.

### 3. `trips`
- Journey history including GPS boundaries, distance (`DECIMAL(10,2)`), duration, telemetry counts (braking, warnings, hazards), and average safety scores.
- Foreign Key: `user_id -> users.id` (`ON DELETE CASCADE`).
- Foreign Key: `vehicle_id -> vehicles.id` (`ON DELETE SET NULL` to ensure trip history is preserved if a vehicle record is deleted).

### 4. `hazards`
- Crowdsourced road obstacles (`POTHOLE`, `ACCIDENT`, `ROAD_BLOCKAGE`, `CONSTRUCTION`, `HEAVY_TRAFFIC`, `OTHER`).
- Severity levels: `LOW`, `MEDIUM`, `HIGH`.
- Lifecycle status: `ACTIVE`, `UNDER_REVIEW`, `RESOLVED`.
- Confidence score: `0` to `100` (`TINYINT UNSIGNED`).
- Foreign Key: `reported_by_user_id -> users.id` (`ON DELETE SET NULL`).

### 5. `hazard_confirmations`
- Community feedback on active hazards (`STILL_PRESENT` vs `FIXED`).
- `UNIQUE KEY (hazard_id, user_id)` prevents vote spamming and allows drivers to update their assessment over time.

### 6. `risk_events`
- Granular high-risk occurrences detected during driving journeys (`HARD_BRAKING`, `SPEED_PATTERN`, `TRAFFIC`, `WEATHER`, `HAZARD`, `ACCIDENT`).
- Attached to specific trips with GPS coordinates and arbitrary JSON metadata.

---

## 5. Setup & Execution

### Prerequisites
- MySQL Server 8.0+ running on `localhost:3306`.

### 4.7 `weather_observations` (Phase 14)
Stores geo-tagged real-time meteorological observations.
- `id` (BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY)
- `latitude` (DECIMAL(9,6) NOT NULL)
- `longitude` (DECIMAL(9,6) NOT NULL)
- `temperature_c` (DECIMAL(4,1) NOT NULL)
- `condition` (ENUM('CLEAR', 'CLOUDY', 'RAIN', 'HEAVY_RAIN', 'FOG'))
- `precipitation_mm` (DECIMAL(5,2) DEFAULT 0.00)
- `humidity` (TINYINT UNSIGNED DEFAULT 0)
- `wind_speed_kmh` (DECIMAL(5,2) DEFAULT 0.00)
- `visibility_km` (DECIMAL(5,2) DEFAULT 10.00)
- `observed_at` (DATETIME NOT NULL)
- `created_at` (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
- Composite Index: `(latitude, longitude, observed_at)`

### 4.8 `traffic_observations` (Phase 15)
Stores timestamped road segment traffic speeds, speed ratios, and congestion levels.
- `id` (BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY)
- `road_segment_id` (VARCHAR(64) NOT NULL)
- `latitude` (DECIMAL(9,6) NOT NULL)
- `longitude` (DECIMAL(9,6) NOT NULL)
- `current_speed_kmh` (DECIMAL(5,1) NOT NULL)
- `free_flow_speed_kmh` (DECIMAL(5,1) NOT NULL)
- `speed_ratio` (DECIMAL(4,2) NOT NULL)
- `congestion_level` (ENUM('LOW', 'MODERATE', 'HIGH'))
- `travel_delay_seconds` (INT UNSIGNED DEFAULT 0)
- `source` (ENUM('SIMULATED', 'REAL') DEFAULT 'SIMULATED')
- `observed_at` (DATETIME NOT NULL)
- `created_at` (TIMESTAMP DEFAULT CURRENT_TIMESTAMP)
- Composite Indexes: `(road_segment_id, observed_at)`, `(latitude, longitude, observed_at)`

---

## 5. Setup & Verification

### Step 1: Create Database & Execute Schema
Using MySQL CLI:
```bash
mysql -u root -p < database/schema.sql
```
Or in PowerShell:
```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < database/schema.sql
```

### Step 2: (Optional) Load Development Seed Data
```bash
mysql -u root -p < database/seed.sql
```

### Step 3: Configure Environment
Copy `.env.example` to `backend/.env` and update your MySQL password:
```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_mysql_password
DB_NAME=smartmove_ai
DB_CONNECTION_LIMIT=10
```

### Step 4: Run Connectivity Test
From the `backend/` directory:
```bash
npm run db:test
```

---

## 6. Planned Future Tables

- **`alerts`** (Phase 16+): Push notification delivery audit log and user alert subscriptions.
