const express = require('express');
const router = express.Router();
const healthController = require('../controllers/healthController');

/**
 * @route   GET /api/health
 * @desc    Check backend status and uptime
 * @access  Public
 */
router.get('/', healthController.getHealth);

module.exports = router;
