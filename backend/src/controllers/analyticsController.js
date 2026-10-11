const analyticsService = require('../services/analyticsService');

/**
 * Analytics Controller.
 * Exposes SQL-aggregated driver safety analytics for today/week/month/year.
 */
exports.getPeriodAnalytics = async (req, res, next) => {
  try {
    const { period } = req.query;
    const analytics = await analyticsService.getPeriodAnalytics(req.user.id, period);

    res.status(200).json({
      success: true,
      data: analytics,
    });
  } catch (error) {
    next(error);
  }
};
