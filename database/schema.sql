-- ==================================================
-- SMARTMOVE AI — Relational Database Schema
-- Database: smartmove_ai
-- Engine: InnoDB
-- Character Set: utf8mb4 (Full Unicode support)
-- ==================================================

CREATE DATABASE IF NOT EXISTS smartmove_ai
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE smartmove_ai;

-- --------------------------------------------------
-- 1. USERS TABLE
-- Stores driver credentials and core profile metadata.
-- Passwords must ALWAYS be hashed (e.g. bcrypt/argon2).
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(255) NOT NULL UNIQUE,
  phone VARCHAR(20) NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_users_email (email)
) ENGINE=InnoDB;

-- --------------------------------------------------
-- 2. VEHICLES TABLE
-- Stores driver vehicle specifications.
-- Deleting a user cascades to remove their registered vehicles.
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS vehicles (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  vehicle_type VARCHAR(50) NOT NULL,
  vehicle_number VARCHAR(30) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_vehicles_user_id (user_id),
  CONSTRAINT fk_vehicles_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------
-- 3. TRIPS TABLE
-- Historical record of completed driving journeys.
-- Uses real DATETIME timestamps instead of offsets.
-- Vehicle reference is SET NULL on vehicle deletion to protect trip history.
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS trips (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT UNSIGNED NOT NULL,
  vehicle_id BIGINT UNSIGNED NULL,
  start_latitude DECIMAL(10,7) NOT NULL,
  start_longitude DECIMAL(10,7) NOT NULL,
  start_location VARCHAR(255) NOT NULL,
  destination_latitude DECIMAL(10,7) NOT NULL,
  destination_longitude DECIMAL(10,7) NOT NULL,
  destination_location VARCHAR(255) NOT NULL,
  started_at DATETIME NOT NULL,
  ended_at DATETIME NULL,
  distance_km DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  duration_minutes INT UNSIGNED NOT NULL DEFAULT 0,
  average_risk TINYINT UNSIGNED NOT NULL DEFAULT 0,
  safety_score TINYINT UNSIGNED NOT NULL DEFAULT 100,
  warning_count INT UNSIGNED NOT NULL DEFAULT 0,
  hazard_count INT UNSIGNED NOT NULL DEFAULT 0,
  hard_braking_count INT UNSIGNED NOT NULL DEFAULT 0,
  traffic_condition VARCHAR(30) NULL,
  weather_condition VARCHAR(50) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_trips_user_id (user_id),
  INDEX idx_trips_started_at (started_at),
  CONSTRAINT fk_trips_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_trips_vehicle
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------
-- 4. HAZARDS TABLE
-- Community-observed road obstacles with dynamic lifecycle.
-- ACTIVE -> UNDER_REVIEW -> RESOLVED.
-- Deleting a user account sets reported_by to NULL so the historical hazard record persists.
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS hazards (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  reported_by_user_id BIGINT UNSIGNED NULL,
  type ENUM('POTHOLE', 'ACCIDENT', 'ROAD_BLOCKAGE', 'CONSTRUCTION', 'HEAVY_TRAFFIC', 'OTHER') NOT NULL,
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  location_name VARCHAR(255) NOT NULL,
  severity ENUM('LOW', 'MEDIUM', 'HIGH') NOT NULL,
  description TEXT NULL,
  status ENUM('ACTIVE', 'UNDER_REVIEW', 'RESOLVED') NOT NULL DEFAULT 'ACTIVE',
  confidence TINYINT UNSIGNED NOT NULL DEFAULT 60,
  reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_confirmed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_at DATETIME NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT chk_hazard_confidence CHECK (confidence BETWEEN 0 AND 100),
  INDEX idx_hazards_coordinates (latitude, longitude),
  INDEX idx_hazards_status (status),
  INDEX idx_hazards_severity (severity),
  INDEX idx_hazards_reported_at (reported_at),
  CONSTRAINT fk_hazards_reported_by
    FOREIGN KEY (reported_by_user_id) REFERENCES users(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------
-- 5. HAZARD CONFIRMATIONS TABLE
-- Records community verification votes ("STILL_PRESENT" vs "FIXED").
-- Enforces UNIQUE(hazard_id, user_id) to prevent duplicate vote flooding.
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS hazard_confirmations (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  hazard_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  confirmation_type ENUM('STILL_PRESENT', 'FIXED') NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_hazard_user (hazard_id, user_id),
  INDEX idx_confirmations_hazard (hazard_id),
  INDEX idx_confirmations_user (user_id),
  CONSTRAINT fk_confirmations_hazard
    FOREIGN KEY (hazard_id) REFERENCES hazards(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_confirmations_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------
-- 6. RISK EVENTS TABLE
-- Granular telemetry events detected during an active trip.
-- Feeds the dynamic risk scoring engine.
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS risk_events (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  trip_id BIGINT UNSIGNED NOT NULL,
  user_id BIGINT UNSIGNED NOT NULL,
  event_type ENUM('HARD_BRAKING', 'SPEED_PATTERN', 'TRAFFIC', 'WEATHER', 'HAZARD', 'ACCIDENT', 'OTHER') NOT NULL,
  risk_score TINYINT UNSIGNED NOT NULL DEFAULT 0,
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  metadata_json JSON NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_risk_score CHECK (risk_score BETWEEN 0 AND 100),
  INDEX idx_risk_events_trip_id (trip_id),
  INDEX idx_risk_events_user_id (user_id),
  INDEX idx_risk_events_event_type (event_type),
  INDEX idx_risk_events_recorded_at (recorded_at),
  CONSTRAINT fk_risk_events_trip
    FOREIGN KEY (trip_id) REFERENCES trips(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE,
  CONSTRAINT fk_risk_events_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB;

-- --------------------------------------------------
-- 7. WEATHER OBSERVATIONS TABLE (Phase 14)
-- Real-time environmental and meteorological observations by location.
-- Influences the dynamic risk engine and supports weather/risk correlation.
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS weather_observations (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  latitude DECIMAL(9,6) NOT NULL,
  longitude DECIMAL(9,6) NOT NULL,
  temperature_c DECIMAL(4,1) NOT NULL,
  `condition` ENUM('CLEAR', 'CLOUDY', 'RAIN', 'HEAVY_RAIN', 'FOG') NOT NULL,
  precipitation_mm DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  humidity TINYINT UNSIGNED NOT NULL DEFAULT 0,
  wind_speed_kmh DECIMAL(5,2) NOT NULL DEFAULT 0.00,
  visibility_km DECIMAL(5,2) NOT NULL DEFAULT 10.00,
  observed_at DATETIME NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_weather_location_time (latitude, longitude, observed_at),
  INDEX idx_weather_observed_at (observed_at)
) ENGINE=InnoDB;

-- --------------------------------------------------
-- 8. TRAFFIC OBSERVATIONS TABLE (Phase 15)
-- Dynamic road segment traffic flow and congestion observations.
-- Eliminates static road danger classifications; reflects live conditions.
-- --------------------------------------------------
CREATE TABLE IF NOT EXISTS traffic_observations (
  id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  road_segment_id VARCHAR(64) NOT NULL,
  latitude DECIMAL(9,6) NOT NULL,
  longitude DECIMAL(9,6) NOT NULL,
  current_speed_kmh DECIMAL(5,1) NOT NULL,
  free_flow_speed_kmh DECIMAL(5,1) NOT NULL,
  speed_ratio DECIMAL(4,2) NOT NULL,
  congestion_level ENUM('LOW', 'MODERATE', 'HIGH') NOT NULL,
  travel_delay_seconds INT UNSIGNED NOT NULL DEFAULT 0,
  source ENUM('SIMULATED', 'REAL') NOT NULL DEFAULT 'SIMULATED',
  observed_at DATETIME NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_traffic_segment_time (road_segment_id, observed_at),
  INDEX idx_traffic_location_time (latitude, longitude, observed_at),
  INDEX idx_traffic_observed_at (observed_at)
) ENGINE=InnoDB;


