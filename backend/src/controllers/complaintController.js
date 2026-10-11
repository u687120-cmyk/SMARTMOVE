const complaintService = require('../services/complaintService');
const clusteringService = require('../services/clusteringService');

/**
 * Complaint & Public Dashboard Controller
 */
exports.getPublicDashboard = async (req, res, next) => {
  try {
    const dashboard = await complaintService.getPublicDashboard();
    res.status(200).json({
      success: true,
      data: dashboard,
    });
  } catch (error) {
    next(error);
  }
};

exports.triggerFollowUps = async (req, res, next) => {
  try {
    const results = await complaintService.processFollowUps();
    res.status(200).json({
      success: true,
      message: `Follow-up cycle processed for ${results.length} complaints`,
      data: results,
    });
  } catch (error) {
    next(error);
  }
};

exports.fastForwardDays = async (req, res, next) => {
  try {
    const days = parseInt(req.body.days, 10) || 7;
    const results = await complaintService.fastForwardDays(days);
    res.status(200).json({
      success: true,
      message: `Fast-forwarded ${days} days! Escalation and follow-up loops executed.`,
      data: results,
    });
  } catch (error) {
    next(error);
  }
};

exports.handleSensorEvent = async (req, res, next) => {
  try {
    const result = await clusteringService.processSensorEvent(req.body);
    res.status(200).json({
      success: true,
      data: result,
    });
  } catch (error) {
    next(error);
  }
};

exports.handleCleanPass = async (req, res, next) => {
  try {
    const result = await clusteringService.recordCleanPass(req.body);
    res.status(200).json({
      success: true,
      data: result,
    });
  } catch (error) {
    next(error);
  }
};

exports.simulatePotholeSpike = async (req, res, next) => {
  try {
    const { lat = 18.5204, lng = 73.8567, deviceId = 'demo_device_' + Math.floor(Math.random() * 1000) } = req.body;
    const result = await clusteringService.processSensorEvent({
      lat,
      lng,
      magnitude: 14.5,
      speed: 25.0,
      deviceId,
      locationName: 'FC Road / Pune University Road',
      isSimulated: true, // Transparently flag simulated events
    });
    res.status(200).json({
      success: true,
      message: 'Demo pothole spike simulated successfully (tagged is_simulated)!',
      data: result,
    });
  } catch (error) {
    next(error);
  }
};
