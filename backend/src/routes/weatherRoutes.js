const express = require('express');
const router = express.Router();
const weatherController = require('../controllers/weatherController');
const authMiddleware = require('../middleware/authMiddleware');

// All weather queries require driver JWT authentication
router.use(authMiddleware);

/**
 * @route   GET /api/weather/current?latitude=...&longitude=...
 * @desc    Get current normalized weather for driver's location
 * @access  Private (Driver JWT)
 */
router.get('/current', weatherController.getCurrentWeather);

module.exports = router;
