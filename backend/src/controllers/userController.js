const userService = require('../services/userService');

/**
 * User Controller.
 * Handles Driver Profile and Vehicle Management.
 */
exports.getProfile = async (req, res, next) => {
  try {
    const profile = await userService.getProfile(req.user.id);
    res.status(200).json({
      success: true,
      data: profile,
    });
  } catch (error) {
    next(error);
  }
};

exports.updateProfile = async (req, res, next) => {
  try {
    const { fullName, phone } = req.body;
    const updated = await userService.updateProfile(req.user.id, { fullName, phone });
    res.status(200).json({
      success: true,
      message: 'Profile updated successfully',
      data: updated,
    });
  } catch (error) {
    next(error);
  }
};

exports.getVehicles = async (req, res, next) => {
  try {
    const vehicles = await userService.getVehicles(req.user.id);
    res.status(200).json({
      success: true,
      data: vehicles,
    });
  } catch (error) {
    next(error);
  }
};

exports.addVehicle = async (req, res, next) => {
  try {
    const { vehicleType, vehicleNumber } = req.body;
    const created = await userService.addVehicle(req.user.id, { vehicleType, vehicleNumber });
    res.status(201).json({
      success: true,
      message: 'Vehicle added successfully',
      data: created,
    });
  } catch (error) {
    next(error);
  }
};

exports.updateVehicle = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { vehicleType, vehicleNumber } = req.body;
    const updated = await userService.updateVehicle(req.user.id, id, { vehicleType, vehicleNumber });
    res.status(200).json({
      success: true,
      message: 'Vehicle updated successfully',
      data: updated,
    });
  } catch (error) {
    next(error);
  }
};

exports.deleteVehicle = async (req, res, next) => {
  try {
    const { id } = req.params;
    await userService.deleteVehicle(req.user.id, id);
    res.status(200).json({
      success: true,
      message: 'Vehicle deleted successfully',
    });
  } catch (error) {
    next(error);
  }
};
