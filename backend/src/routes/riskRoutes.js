const express = require('express');
const router = express.Router();
const riskController = require('../controllers/riskController');
const authMiddleware = require('../middleware/authMiddleware');

// All risk queries require user authentication
router.use(authMiddleware);

/**
 * @route   GET /api/risk/current
 * @desc    Get dynamic road risk score, classification, and explanation
 * @access  Private (Driver JWT)
 */
router.get('/current', riskController.getCurrentRisk);

module.exports = router;
