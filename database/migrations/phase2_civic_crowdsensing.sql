-- ==================================================
-- SMARTMOVE AI — Phase 2 Civic Governance & Crowdsensing Schema
-- Adds: authorities, complaints, complaint_events, sensor_events, clean_passes
-- ==================================================

USE smartmove_ai;

-- 1. AUTHORITIES DIRECTORY
CREATE TABLE IF NOT EXISTS authorities (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  jurisdiction_type ENUM('NHAI', 'PWD', 'MUNICIPAL_CORP', 'GRAM_PANCHAYAT') NOT NULL,
  state VARCHAR(100) NULL,
  district VARCHAR(100) NULL,
  official_email VARCHAR(255) NOT NULL,
  escalation_email VARCHAR(255) NULL,
  helpline VARCHAR(50) NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- 2. ALTER HAZARDS TO ADD CIVIC & PASSIVE FIELDS IF NOT PRESENT
-- MySQL 8.0 supports ALTER TABLE ADD COLUMN IF NOT EXISTS or safe checks
SET @dbname = DATABASE();
SET @tablename = "hazards";

-- Check and add road_type
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = 'road_type'
  ) > 0,
  "SELECT 1",
  "ALTER TABLE hazards ADD COLUMN road_type VARCHAR(50) DEFAULT 'CITY_ROAD'"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Check and add authority_target
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = 'authority_target'
  ) > 0,
  "SELECT 1",
  "ALTER TABLE hazards ADD COLUMN authority_target VARCHAR(100) DEFAULT 'Municipal Corporation'"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Check and add clean_pass_count
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @dbname
      AND TABLE_NAME = @tablename
      AND COLUMN_NAME = 'clean_pass_count'
  ) > 0,
  "SELECT 1",
  "ALTER TABLE hazards ADD COLUMN clean_pass_count INT UNSIGNED DEFAULT 0"
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3. SENSOR EVENTS (Raw Crowdsensing Spikes)
CREATE TABLE IF NOT EXISTS sensor_events (
  id VARCHAR(64) PRIMARY KEY,
  hazard_id BIGINT UNSIGNED NOT NULL,
  device_fingerprint VARCHAR(128) NOT NULL,
  magnitude DECIMAL(6,2) NOT NULL,
  speed DECIMAL(6,2) NOT NULL,
  latitude DECIMAL(10,7) NOT NULL,
  longitude DECIMAL(10,7) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_sensor_events_hazard (hazard_id),
  INDEX idx_sensor_events_device (device_fingerprint),
  CONSTRAINT fk_sensor_hazard
    FOREIGN KEY (hazard_id) REFERENCES hazards(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. CLEAN PASSES (Vehicles passing without jerk - for auto-fixed verification)
CREATE TABLE IF NOT EXISTS clean_passes (
  id VARCHAR(64) PRIMARY KEY,
  hazard_id BIGINT UNSIGNED NOT NULL,
  device_fingerprint VARCHAR(128) NOT NULL,
  speed DECIMAL(6,2) NOT NULL,
  recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_clean_pass_device (hazard_id, device_fingerprint),
  INDEX idx_clean_passes_hazard (hazard_id),
  CONSTRAINT fk_clean_passes_hazard
    FOREIGN KEY (hazard_id) REFERENCES hazards(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- 5. COMPLAINTS TABLE (Official Follow-up loop)
CREATE TABLE IF NOT EXISTS complaints (
  id VARCHAR(64) PRIMARY KEY,
  complaint_code VARCHAR(32) NOT NULL UNIQUE,
  hazard_id BIGINT UNSIGNED NOT NULL,
  authority_id VARCHAR(64) NULL,
  authority_name VARCHAR(150) NOT NULL,
  recipient_email VARCHAR(255) NOT NULL,
  stage ENUM('DAY_0', 'DAY_7', 'DAY_14') NOT NULL DEFAULT 'DAY_0',
  status ENUM('SENT', 'REMINDER_SENT', 'ESCALATED', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'SENT',
  subject VARCHAR(255) NOT NULL,
  body_text TEXT NOT NULL,
  sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_followup_at DATETIME NULL,
  INDEX idx_complaints_hazard (hazard_id),
  INDEX idx_complaints_stage (stage),
  INDEX idx_complaints_status (status),
  CONSTRAINT fk_complaints_hazard
    FOREIGN KEY (hazard_id) REFERENCES hazards(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. COMPLAINT EVENTS (Audit timeline for Public Transparency Dashboard)
CREATE TABLE IF NOT EXISTS complaint_events (
  id VARCHAR(64) PRIMARY KEY,
  complaint_id VARCHAR(64) NOT NULL,
  event_type ENUM('FILED', 'REMINDER_DISPATCHED', 'ESCALATED', 'CLEAN_PASS_DETECTED', 'RESOLVED_BY_CROWD') NOT NULL,
  description TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_cevents_complaint (complaint_id),
  CONSTRAINT fk_cevents_complaint
    FOREIGN KEY (complaint_id) REFERENCES complaints(id)
    ON DELETE CASCADE
) ENGINE=InnoDB;

-- 7. SEED INITIAL CIVIC AUTHORITIES DIRECTORY
INSERT INTO authorities (id, name, jurisdiction_type, state, district, official_email, escalation_email, helpline)
VALUES
  ('auth_nhai_hq', 'National Highways Authority of India (NHAI)', 'NHAI', 'All India', 'Central', 'complaints.nhai@nic.in.mock', 'ministry.morth@nic.in.mock', '1033'),
  ('auth_pwd_mh', 'Maharashtra State PWD', 'PWD', 'Maharashtra', 'State HQ', 'pwd.helpdesk.mh@gov.in.mock', 'sec.pwd.mh@gov.in.mock', '1800-222-793'),
  ('auth_pmc', 'Pune Municipal Corporation (PMC Road Dept)', 'MUNICIPAL_CORP', 'Maharashtra', 'Pune', 'roadrepair@punecorporation.org.mock', 'commissioner@punecorporation.org.mock', '1800-1030-222'),
  ('auth_bbmp', 'Bruhat Bengaluru Mahanagara Palike (BBMP Roads)', 'MUNICIPAL_CORP', 'Karnataka', 'Bengaluru Urban', 'roads.bbmp@bbmp.gov.in.mock', 'comm@bbmp.gov.in.mock', '1533'),
  ('auth_panchayat_generic', 'Rural Roads & Gram Panchayat Division', 'GRAM_PANCHAYAT', 'Generic', 'Rural', 'panchayat.roads@nic.in.mock', 'bdo.office@nic.in.mock', '1800-180-1551')
ON DUPLICATE KEY UPDATE name = VALUES(name);
