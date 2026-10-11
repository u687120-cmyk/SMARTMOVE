const weatherService = require('../services/weatherService');

/**
 * Weather Controller (Phase 14).
 * Handles requests for current meteorological conditions by geographic location.
 */
exports.getCurrentWeather = async (req, res, next) => {
  try {
    const { latitude, longitude } = req.query;

    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json({
        success: false,
        message: 'Invalid coordinates: latitude and longitude are required.',
      });
    }

    if (!weatherService.isValidCoordinates(latitude, longitude)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid coordinates: latitude must be between -90 and 90, longitude between -180 and 180.',
      });
    }

    const weatherData = await weatherService.getCurrentWeather(latitude, longitude);

    res.status(200).json({
      success: true,
      data: weatherData,
    });
  } catch (error) {
    const status = error.statusCode || 500;
    res.status(status).json({
      success: false,
      message: error.message || 'Weather service temporarily unavailable',
    });
  }
};
