const pool = require('./src/config/database');
const config = require('./src/config/environment');

async function testDatabaseConnection() {
  console.log(`==================================================`);
  console.log(`  SMARTMOVE AI — Database Connectivity Test`);
  console.log(`  Target Host: ${config.db.host}:${config.db.port}`);
  console.log(`  Target DB:   ${config.db.database}`);
  console.log(`  User:        ${config.db.user}`);
  console.log(`==================================================`);

  let connection;
  try {
    connection = await pool.getConnection();
    console.log('Connecting to MySQL...');

    const [rows] = await connection.query('SELECT 1 AS test_val, NOW() AS server_time, VERSION() AS mysql_version');
    console.log('✅ Connection established successfully!');
    console.log('   Query Result:   ', rows[0]);
    console.log('   Server Time:    ', rows[0].server_time);
    console.log('   MySQL Version:  ', rows[0].mysql_version);
    console.log('==================================================');
  } catch (error) {
    console.error('❌ Database connection failed!');
    console.error('   Code:    ', error.code);
    console.error('   Message: ', error.message);
    console.error('\nTroubleshooting suggestions:');
    console.error('1. Verify that the MySQL service is running (e.g. `Get-Service *mysql*`).');
    console.error('2. Ensure database "smartmove_ai" is created:');
    console.error('   mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS smartmove_ai;"');
    console.error('3. Check credentials in backend/.env (DB_USER, DB_PASSWORD).');
    process.exitCode = 1;
  } finally {
    if (connection) connection.release();
    await pool.end();
  }
}

testDatabaseConnection();
