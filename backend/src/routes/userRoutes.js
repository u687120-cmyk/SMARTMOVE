const express = require('express');
const router = express.Router();
const userController = require('../controllers/userController');
const authMiddleware = require('../middleware/authMiddleware');

// All user routes require authentication
router.use(authMiddleware);

// Profile
router.get('/me', userController.getProfile);
router.put('/me', userController.updateProfile);

// Vehicles
router.get('/me/vehicles', userController.getVehicles);
router.post('/me/vehicles', userController.addVehicle);
router.put('/me/vehicles/:id', userController.updateVehicle);
router.delete('/me/vehicles/:id', userController.deleteVehicle);

module.exports = router;
