const express = require('express');
const router = express.Router();
const hazardController = require('../controllers/hazardController');
const authMiddleware = require('../middleware/authMiddleware');
const { optionalAuth } = authMiddleware;

// Active hazards can be viewed publicly
router.get('/active', hazardController.getActiveHazards);
router.get('/my-reports', authMiddleware, hazardController.getMyReports);
router.get('/:id', hazardController.getHazardDetails);

// Reporting allows logged in users or guest travelers
router.post('/', optionalAuth, hazardController.reportHazard);
router.post('/report', optionalAuth, hazardController.reportHazard);
router.post('/:id/confirm', optionalAuth, hazardController.confirmHazard);
router.post('/:id/fixed', optionalAuth, hazardController.resolveHazard);

module.exports = router;
