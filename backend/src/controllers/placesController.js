const placesService = require('../services/placesService');

/**
 * Places Controller — Autocomplete & Details.
 */
class PlacesController {

  async autocomplete(req, res, next) {
    try {
      const { input, sessionToken } = req.query;
      const predictions = await placesService.autocomplete(input, sessionToken);
      res.status(200).json({
        success: true,
        data: predictions,
      });
    } catch (err) {
      next(err);
    }
  }

  async getDetails(req, res, next) {
    try {
      const { placeId } = req.query;
      if (!placeId) {
        return res.status(400).json({
          success: false,
          error: { message: 'placeId query parameter is required' }
        });
      }
      const details = await placesService.getDetails(placeId);
      res.status(200).json({
        success: true,
        data: details,
      });
    } catch (err) {
      next(err);
    }
  }
}

module.exports = new PlacesController();
