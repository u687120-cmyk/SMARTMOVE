const express = require('express');
const router = express.Router();
const complaintController = require('../controllers/complaintController');

// Public Civic Transparency Dashboard
router.get('/public-dashboard', complaintController.getPublicDashboard);

// Ingestion for passive crowdsensing spikes
router.post('/sensor-event', complaintController.handleSensorEvent);

// Ingestion for clean passes (zero-shock passing -> auto-fixed)
router.post('/clean-pass', complaintController.handleCleanPass);

// Demo Switch: Simulate Pothole Spike on Demand (For Hackathon Judges)
router.post('/simulate-spike', complaintController.simulatePotholeSpike);

// Follow-up cron / manual test trigger (Day 0 -> Day 7 -> Day 14)
router.post('/trigger-followups', complaintController.triggerFollowUps);

// Fast-forward demo trigger: Simulates +7 days to show Day 7 reminders & Day 14 escalations live!
router.post('/fast-forward-days', complaintController.fastForwardDays);

module.exports = router;
