const tripService = require('../services/tripService');

/**
 * Trip Controller.
 * Handles Trip Creation, Journey History, and Detailed Trip Querying.
 */
exports.createTrip = async (req, res, next) => {
  try {
    const trip = await tripService.createTrip(req.user.id, req.body);
    res.status(201).json({
      success: true,
      message: 'Trip recorded successfully',
      data: trip,
    });
  } catch (error) {
    next(error);
  }
};

exports.getTrips = async (req, res, next) => {
  try {
    const { period } = req.query;
    const trips = await tripService.getTrips(req.user.id, period);
    res.status(200).json({
      success: true,
      data: trips,
    });
  } catch (error) {
    next(error);
  }
};

exports.getTripDetails = async (req, res, next) => {
  try {
    const { id } = req.params;
    const trip = await tripService.getTripById(req.user.id, id);
    res.status(200).json({
      success: true,
      data: trip,
    });
  } catch (error) {
    next(error);
  }
};
