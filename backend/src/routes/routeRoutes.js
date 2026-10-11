const express = require('express');
const router = express.Router();
const routeController = require('../controllers/routeController');
const authMiddleware = require('../middleware/authMiddleware');

const { optionalAuth } = authMiddleware;

/**
 * @route   POST /api/routes/recommend
 * @desc    Compare candidate routes and recommend safest practical route
 * @access  Public / Optional Auth
 */
router.post('/recommend', optionalAuth, routeController.recommendRoutes);

// Private routes require driver JWT authentication
router.use(authMiddleware);

/**
 * @route   POST /api/routes/recalculate
 * @desc    Periodically evaluate active route and determine if en-route rerouting is warranted
 * @access  Private (Driver JWT)
 */
router.post('/recalculate', routeController.recalculateEnRoute);

/**
 * @route   POST /api/routes/switch
 * @desc    Confirm route switch during an active trip and record event
 * @access  Private (Driver JWT)
 */
router.post('/switch', routeController.switchRoute);

module.exports = router;
