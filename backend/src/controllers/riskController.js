const riskService = require('../services/riskService');
const weatherService = require('../services/weatherService');

/**
 * Risk Controller (Phase 13 & 14).
 * Exposes current road risk state for the authenticated driver.
 */
exports.getCurrentRisk = async (req, res, next) => {
  try {
    const { traffic, weather, latitude, longitude } = req.query;

    const overrides = {};
    if (traffic) overrides.trafficCondition = traffic;
    if (weather) overrides.weatherCondition = weather;

    if (latitude !== undefined && longitude !== undefined) {
      if (!weatherService.isValidCoordinates(latitude, longitude)) {
        return res.status(400).json({
          success: false,
          message: 'Invalid coordinates: latitude must be between -90 and 90, longitude between -180 and 180.',
        });
      }
      overrides.latitude = Number(latitude);
      overrides.longitude = Number(longitude);
    }

    const riskData = await riskService.getCurrentRiskForUser(
      req.user.id,
      overrides
    );

    res.status(200).json({
      success: true,
      data: riskData,
    });
  } catch (error) {
    res.status(500).json({
      success: false,
      message: 'Unable to calculate current risk',
    });
  }
};
