const hazardService = require('../services/hazardService');

/**
 * Hazard Controller.
 * Handles Active Hazard Retrieval, Reporting, Community Confirmations, and Driver Reports.
 */
exports.getActiveHazards = async (req, res, next) => {
  try {
    const hazards = await hazardService.getActiveHazards();
    res.status(200).json({
      success: true,
      data: hazards,
    });
  } catch (error) {
    next(error);
  }
};

exports.getMyReports = async (req, res, next) => {
  try {
    const reports = await hazardService.getMyReports(req.user.id);
    res.status(200).json({
      success: true,
      data: reports,
    });
  } catch (error) {
    next(error);
  }
};

exports.getHazardDetails = async (req, res, next) => {
  try {
    const { id } = req.params;
    const hazard = await hazardService.getHazardById(id);
    res.status(200).json({
      success: true,
      data: hazard,
    });
  } catch (error) {
    next(error);
  }
};

exports.reportHazard = async (req, res, next) => {
  try {
    const hazard = await hazardService.reportHazard(req.user.id, req.body);
    res.status(201).json({
      success: true,
      message: 'Road hazard reported successfully',
      data: hazard,
    });
  } catch (error) {
    next(error);
  }
};

exports.confirmHazard = async (req, res, next) => {
  try {
    const { id } = req.params;
    const { confirmationType = 'STILL_PRESENT' } = req.body;
    const updated = await hazardService.confirmHazard(req.user.id, id, confirmationType);
    res.status(200).json({
      success: true,
      message: 'Hazard confirmation recorded',
      data: updated,
    });
  } catch (error) {
    next(error);
  }
};

exports.resolveHazard = async (req, res, next) => {
  try {
    const { id } = req.params;
    const updated = await hazardService.confirmHazard(req.user.id, id, 'FIXED');
    res.status(200).json({
      success: true,
      message: 'Hazard marked as FIXED',
      data: updated,
    });
  } catch (error) {
    next(error);
  }
};
