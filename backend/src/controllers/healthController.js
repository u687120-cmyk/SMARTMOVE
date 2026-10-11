const healthService = require('../services/healthService');

/**
 * Health Controller.
 * Handles GET /api/health requests to confirm system readiness.
 */
exports.getHealth = (req, res) => {
  const status = healthService.getHealthStatus();

  res.status(200).json({
    success: true,
    message: 'SMARTMOVE AI backend is running',
    environment: status.environment,
    timestamp: status.timestamp,
  });
};
