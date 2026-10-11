const mysql = require('mysql2/promise');
const config = require('./environment');

/**
 * MySQL Connection Pool for SMARTMOVE AI.
 * Uses mysql2/promise to provide robust async/await query execution.
 * Avoids creating a new connection for every HTTP request.
 */
const pool = mysql.createPool({
  host: config.db.host,
  port: config.db.port,
  user: config.db.user,
  password: config.db.password,
  database: config.db.database,
  waitForConnections: true,
  connectionLimit: config.db.connectionLimit,
  queueLimit: 0,
});

module.exports = pool;
