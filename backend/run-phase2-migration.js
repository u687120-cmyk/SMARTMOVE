const pool = require('./src/config/database');

async function run() {
  const conn = await pool.getConnection();
  try {
    console.log('Running Phase 2 Migration...');

    // 1. Authorities Table
    await conn.query(`
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
    `);
    console.log('✔ Table authorities ready');

    // 2. Add columns to hazards if missing
    const [cols] = await conn.query("SHOW COLUMNS FROM hazards");
    const colNames = cols.map(c => c.Field);

    if (!colNames.includes('road_type')) {
      await conn.query("ALTER TABLE hazards ADD COLUMN road_type VARCHAR(50) DEFAULT 'CITY_ROAD'");
      console.log('✔ Added road_type to hazards');
    }
    if (!colNames.includes('authority_target')) {
      await conn.query("ALTER TABLE hazards ADD COLUMN authority_target VARCHAR(100) DEFAULT 'Municipal Corporation'");
      console.log('✔ Added authority_target to hazards');
    }
    if (!colNames.includes('clean_pass_count')) {
      await conn.query("ALTER TABLE hazards ADD COLUMN clean_pass_count INT UNSIGNED DEFAULT 0");
      console.log('✔ Added clean_pass_count to hazards');
    }

    // 3. Sensor Events
    await conn.query(`
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
    `);
    console.log('✔ Table sensor_events ready');

    // 4. Clean Passes
    await conn.query(`
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
    `);
    console.log('✔ Table clean_passes ready');

    // 5. Complaints
    await conn.query(`
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
    `);
    console.log('✔ Table complaints ready');

    // 6. Complaint Events
    await conn.query(`
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
    `);
    console.log('✔ Table complaint_events ready');

    // 7. Seed Authorities
    await conn.query(`
      INSERT INTO authorities (id, name, jurisdiction_type, state, district, official_email, escalation_email, helpline)
      VALUES
        ('auth_nhai_hq', 'National Highways Authority of India (NHAI)', 'NHAI', 'All India', 'Central', 'complaints.nhai@nic.in.mock', 'ministry.morth@nic.in.mock', '1033'),
        ('auth_pwd_mh', 'Maharashtra State PWD', 'PWD', 'Maharashtra', 'State HQ', 'pwd.helpdesk.mh@gov.in.mock', 'sec.pwd.mh@gov.in.mock', '1800-222-793'),
        ('auth_pmc', 'Pune Municipal Corporation (PMC Road Dept)', 'MUNICIPAL_CORP', 'Maharashtra', 'Pune', 'roadrepair@punecorporation.org.mock', 'commissioner@punecorporation.org.mock', '1800-1030-222'),
        ('auth_bbmp', 'Bruhat Bengaluru Mahanagara Palike (BBMP Roads)', 'MUNICIPAL_CORP', 'Karnataka', 'Bengaluru Urban', 'roads.bbmp@bbmp.gov.in.mock', 'comm@bbmp.gov.in.mock', '1533'),
        ('auth_panchayat_generic', 'Rural Roads & Gram Panchayat Division', 'GRAM_PANCHAYAT', 'Generic', 'Rural', 'panchayat.roads@nic.in.mock', 'bdo.office@nic.in.mock', '1800-180-1551')
      ON DUPLICATE KEY UPDATE name = VALUES(name);
    `);
    console.log('✔ Authorities directory seeded');

    console.log('\n🎉 All Phase 2 Tables and Migrations successfully applied!');
  } finally {
    conn.release();
    process.exit(0);
  }
}

run().catch(err => {
  console.error('Migration failed:', err);
  process.exit(1);
});
