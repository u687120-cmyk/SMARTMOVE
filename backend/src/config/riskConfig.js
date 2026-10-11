/**
 * SMARTMOVE AI — Risk Engine Configuration
 *
 * Centralized weights, thresholds, and condition mappings for the dynamic risk engine.
 * Keeping weights in one location ensures transparency, auditability, and easy tuning.
 */

const RiskConfig = {
  // Weights (must sum to 1.0)
  weights: {
    hazard: 0.30,
    traffic: 0.25,
    weather: 0.15,
    hardBraking: 0.15,
    riskEvents: 0.15,
  },

  // Score thresholds for risk classification (0 - 100)
  thresholds: {
    lowMax: 30,
    mediumMax: 60,
  },

  // Hazard severity base scores
  hazardSeverityScores: {
    LOW: 30,
    MEDIUM: 60,
    HIGH: 90,
  },

  // Distance multipliers for hazard proximity
  distanceMultipliers: [
    { maxDistanceMeters: 200, multiplier: 1.0 },   // Very close: full impact
    { maxDistanceMeters: 500, multiplier: 0.85 },  // Close: high impact
    { maxDistanceMeters: 1000, multiplier: 0.60 }, // Moderate distance
    { maxDistanceMeters: 2000, multiplier: 0.35 }, // Far: low impact
    { maxDistanceMeters: Infinity, multiplier: 0.10 }, // Minimal ambient impact
  ],

  // Simulated traffic condition base scores (Interface for Phase 15 replacement)
  trafficScores: {
    LOW: 20,
    MODERATE: 60,
    HIGH: 90,
  },

  // Simulated weather condition base scores (Interface for Phase 14 replacement)
  weatherScores: {
    CLEAR: 10,
    CLOUDY: 20,
    RAIN: 55,
    HEAVY_RAIN: 80,
    FOG: 75,
  },

  // Hard braking event count scaling (events within recent time window)
  hardBrakingScores: [
    { maxCount: 0, score: 10 },
    { maxCount: 2, score: 40 },
    { maxCount: 5, score: 70 },
    { maxCount: Infinity, score: 90 },
  ],

  // Accident & other risk event count scaling (within recent time window)
  eventRiskScores: [
    { maxCount: 0, score: 10 },
    { maxCount: 2, score: 40 },
    { maxCount: 5, score: 70 },
    { maxCount: Infinity, score: 90 },
  ],

  // Time decay window for live events (in minutes)
  timeWindowMinutes: 30,

  // Standard recommendations based on risk classification
  recommendations: {
    LOW: 'Road conditions look safe right now.',
    MEDIUM: 'Drive carefully. Conditions ahead may increase risk.',
    HIGH: 'High-risk conditions detected ahead. Consider a safer route.',
  },
};

module.exports = RiskConfig;
