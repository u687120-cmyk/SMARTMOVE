-- ==================================================
-- SMARTMOVE AI — Development Seed Data
-- FOR LOCAL DEVELOPMENT TESTING ONLY
-- DO NOT USE IN PRODUCTION
-- ==================================================

USE smartmove_ai;

-- 1. Demo Driver Account
-- Password hash is a synthetic bcrypt placeholder for development only
INSERT INTO users (id, full_name, email, phone, password_hash)
VALUES (
  1,
  'Ujjwal',
  'user@email.com',
  '+91 98765 43210',
  '$2b$10$DEVELOPMENT_SYNTHETIC_MOCK_HASH_DO_NOT_USE_IN_PROD'
)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- 2. Demo Vehicle
INSERT INTO vehicles (id, user_id, vehicle_type, vehicle_number)
VALUES (
  1,
  1,
  'Car',
  'MH 12 AB 1234'
)
ON DUPLICATE KEY UPDATE vehicle_number = VALUES(vehicle_number);

-- 3. Demo Trips (matching historical journeys)
INSERT INTO trips (
  id, user_id, vehicle_id,
  start_latitude, start_longitude, start_location,
  destination_latitude, destination_longitude, destination_location,
  started_at, ended_at, distance_km, duration_minutes,
  average_risk, safety_score, warning_count, hazard_count, hard_braking_count,
  traffic_condition, weather_condition
)
VALUES
(
  1, 1, 1,
  18.5204000, 73.8567000, 'Kothrud, Pune',
  18.5912000, 73.7389000, 'Hinjewadi Phase 1, Pune',
  DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 82 MINUTE),
  16.40, 38,
  24, 91, 1, 1, 0,
  'Moderate', 'Clear'
),
(
  2, 1, 1,
  18.5912000, 73.7389000, 'Hinjewadi Phase 1, Pune',
  18.5314000, 73.8446000, 'Shivajinagar, Pune',
  DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 45 MINUTE),
  18.20, 45,
  48, 76, 3, 2, 2,
  'Heavy', 'Light Rain'
)
ON DUPLICATE KEY UPDATE distance_km = VALUES(distance_km);

-- 4. Demo Hazards (matching active alerts)
INSERT INTO hazards (
  id, reported_by_user_id, type,
  latitude, longitude, location_name,
  severity, description, status, confidence,
  reported_at, last_confirmed_at
)
VALUES
(
  1, 1, 'ACCIDENT',
  18.5980000, 73.7420000, 'Pune - Hinjewadi Bypass (Near Flyover)',
  'HIGH', 'Multi-vehicle collision on right lane. Expect significant delays.',
  'ACTIVE', 92,
  DATE_SUB(NOW(), INTERVAL 8 MINUTE), DATE_SUB(NOW(), INTERVAL 2 MINUTE)
),
(
  2, 1, 'POTHOLE',
  18.5940000, 73.7400000, 'Pune - Hinjewadi Road (Phase 1 Entry)',
  'MEDIUM', 'Deep crater across middle lane. Vehicles braking abruptly.',
  'ACTIVE', 87,
  DATE_SUB(NOW(), INTERVAL 12 MINUTE), DATE_SUB(NOW(), INTERVAL 4 MINUTE)
),
(
  3, NULL, 'CONSTRUCTION',
  18.5910000, 73.7650000, 'Wakad Bridge Underpass',
  'LOW', 'Road surfacing and lane narrowing on left shoulder.',
  'ACTIVE', 75,
  DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 15 MINUTE)
),
(
  4, 1, 'ROAD_BLOCKAGE',
  18.5580000, 73.7920000, 'Baner Road',
  'LOW', 'Obstruction cleared by municipal teams.',
  'RESOLVED', 10,
  DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)
)
ON DUPLICATE KEY UPDATE confidence = VALUES(confidence);

-- 5. Demo Hazard Confirmations
INSERT INTO hazard_confirmations (hazard_id, user_id, confirmation_type)
VALUES
(1, 1, 'STILL_PRESENT'),
(2, 1, 'STILL_PRESENT'),
(4, 1, 'FIXED')
ON DUPLICATE KEY UPDATE confirmation_type = VALUES(confirmation_type);
