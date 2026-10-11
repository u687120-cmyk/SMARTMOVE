/**
 * SMARTMOVE AI — Dynamic Traffic Engine Configuration (Phase 15)
 *
 * Centralized mapping of speed ratios, congestion tiers, risk scores,
 * travel delay modifiers, and cache/freshness thresholds.
 */

const TrafficConfig = {
  // Speed ratio thresholds: speedRatio = currentSpeedKmh / freeFlowSpeedKmh
  speedRatios: {
    lowMin: 0.80,       // >= 0.80 -> LOW congestion
    moderateMin: 0.50,  // 0.50 .. 0.79 -> MODERATE congestion
    // < 0.50 -> HIGH congestion
  },

  // Base risk scores for traffic tiers (0 - 100)
  baseRisk: {
    LOW: 20,
    MODERATE: 60,
    HIGH: 90,
  },

  // Travel delay penalties added to fine-grained traffic risk
  delayModifiers: {
    severeDelaySeconds: 600,  // > 10 minutes delay
    severeDelayPenalty: 10,
    moderateDelaySeconds: 300, // > 5 minutes delay
    moderateDelayPenalty: 5,
  },

  // Cache settings
  cacheTtlMs: 3 * 60 * 1000,           // 3 minutes cache TTL
  cacheCoordinatePrecision: 2,         // ~1.1 km grid resolution

  // Freshness threshold: observations older than 10 minutes are considered stale
  maxFreshnessMs: 10 * 60 * 1000,      // 10 minutes freshness window

  // Default free-flow speed for urban corridors if unspecified (km/h)
  defaultFreeFlowSpeedKmh: 60.0,

  // Deterministic simulation scenarios for development, testing, and fallback
  simulationScenarios: {
    LOW: {
      currentSpeedKmh: 55.0,
      freeFlowSpeedKmh: 60.0,
      travelDelaySeconds: 30,
      roadSegmentId: 'seg-pune-fc-rd-01',
    },
    MODERATE: {
      currentSpeedKmh: 38.0,
      freeFlowSpeedKmh: 60.0,
      travelDelaySeconds: 240,
      roadSegmentId: 'seg-pune-sb-rd-02',
    },
    HIGH: {
      currentSpeedKmh: 20.0,
      freeFlowSpeedKmh: 60.0,
      travelDelaySeconds: 600,
      roadSegmentId: 'seg-pune-jm-rd-03',
    },
  },

  // Explanations for traffic conditions
  explanations: {
    HIGH: 'Heavy traffic is increasing road risk.',
    MODERATE: 'Moderate traffic may increase travel risk.',
    LOW: 'Traffic is currently light.',
  },

  // UI display metadata
  display: {
    LOW: { label: 'Light', icon: '🚗', badgeColor: '#10B981' },
    MODERATE: { label: 'Moderate', icon: '🚗', badgeColor: '#F59E0B' },
    HIGH: { label: 'Heavy', icon: '🚗', badgeColor: '#EF4444' },
  },
};

module.exports = TrafficConfig;
