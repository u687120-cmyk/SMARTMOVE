const config = require('../config/environment');

/**
 * Health Service.
 * Provides system diagnostics and uptime metadata.
 */
class HealthService {
  getHealthStatus() {
    return {
      running: true,
      environment: config.nodeEnv,
      timestamp: new Date().toISOString(),
      service: 'SMARTMOVE AI Backend',
      version: '1.0.0',
    };
  }
}

module.exports = new HealthService();
