const trafficService = require('../services/trafficService');

/**
 * Traffic Controller (Phase 15).
 * Handles current road segment traffic and congestion requests.
 */
exports.getCurrentTraffic = async (req, res, next) => {
  try {
    const { latitude, longitude, scenario } = req.query;

    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json({
        success: false,
        message: 'Invalid coordinates: latitude and longitude are required.',
      });
    }

    if (!trafficService.isValidCoordinates(latitude, longitude)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid coordinates: latitude must be between -90 and 90, longitude between -180 and 180.',
      });
    }

    const trafficData = await trafficService.getCurrentTraffic(
      latitude,
      longitude,
      scenario
    );

    res.status(200).json({
      success: true,
      data: trafficData,
    });
  } catch (error) {
    const status = error.statusCode || 500;
    res.status(status).json({
      success: false,
      message: error.message || 'Traffic service temporarily unavailable',
    });
  }
};
