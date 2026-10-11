const express = require('express');
const router = express.Router();
const placesController = require('../controllers/placesController');

router.get('/autocomplete', (req, res, next) => placesController.autocomplete(req, res, next));
router.get('/details', (req, res, next) => placesController.getDetails(req, res, next));

module.exports = router;
