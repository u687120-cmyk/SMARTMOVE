const express = require('express');
const router = express.Router();
const trafficController = require('../controllers/trafficController');
const authMiddleware = require('../middleware/authMiddleware');

// All traffic queries require driver JWT authentication
router.use(authMiddleware);

/**
 * @route   GET /api/traffic/current?latitude=...&longitude=...
 * @desc    Get dynamic road segment traffic observations and speed ratio
 * @access  Private (Driver JWT)
 */
router.get('/current', trafficController.getCurrentTraffic);

module.exports = router;
