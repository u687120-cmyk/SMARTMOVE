const pool = require('./src/config/database');

async function patchDb() {
  const conn = await pool.getConnection();
  try {
    console.log('Applying Safety & Safeguard Patches...');

    // 1. Ensure complaints table has UNIQUE constraint on hazard_id
    try {
      await conn.query(`
        ALTER TABLE complaints
        ADD CONSTRAINT uk_complaint_hazard UNIQUE (hazard_id)
      `);
      console.log('✔ Added UNIQUE constraint on complaints(hazard_id) to strictly prevent double complaints');
    } catch (e) {
      if (e.code === 'ER_DUP_KEYNAME') {
        console.log('✔ UNIQUE constraint on complaints(hazard_id) already exists');
      } else {
        console.warn('Notice on complaints constraint:', e.message);
      }
    }

    // 2. Add is_simulated to sensor_events and hazards
    const [hCols] = await conn.query("SHOW COLUMNS FROM hazards");
    if (!hCols.some(c => c.Field === 'is_simulated')) {
      await conn.query("ALTER TABLE hazards ADD COLUMN is_simulated BOOLEAN DEFAULT FALSE");
      console.log('✔ Added is_simulated to hazards');
    }

    const [sCols] = await conn.query("SHOW COLUMNS FROM sensor_events");
    if (!sCols.some(c => c.Field === 'is_simulated')) {
      await conn.query("ALTER TABLE sensor_events ADD COLUMN is_simulated BOOLEAN DEFAULT FALSE");
      console.log('✔ Added is_simulated to sensor_events');
    }

    // 3. Update status enum or support REOPENED status
    await conn.query(`
      ALTER TABLE hazards
      MODIFY COLUMN status ENUM('ACTIVE', 'UNDER_REVIEW', 'RESOLVED', 'REOPENED') NOT NULL DEFAULT 'ACTIVE'
    `);
    console.log('✔ Supported REOPENED status in hazards');

    console.log('\n🎉 Database Safeguard Patches Applied Successfully!');
  } finally {
    conn.release();
    process.exit(0);
  }
}

patchDb().catch(err => {
  console.error('Patch failed:', err);
  process.exit(1);
});
