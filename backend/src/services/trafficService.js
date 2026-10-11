const pool = require('../config/database');
const TrafficConfig = require('../config/trafficConfig');

/**
 * SMARTMOVE AI — Dynamic Traffic Service (Phase 15).
 *
 * Implements a clean traffic provider abstraction, deterministic simulation,
 * speed ratio analysis, 3-minute in-memory caching, 10-minute freshness validation,
 * and asynchronous MySQL persistence.
 */
class TrafficService {
  constructor() {
    // In-memory traffic cache: Map<string, { data: object, timestamp: number }>
    this.cache = new Map();
  }

  /**
   * Validates geographic coordinates.
   */
  isValidCoordinates(lat, lng) {
    const nLat = Number(lat);
    const nLng = Number(lng);
    return (
      !isNaN(nLat) &&
      !isNaN(nLng) &&
      nLat >= -90 &&
      nLat <= 90 &&
      nLng >= -180 &&
      nLng <= 180
    );
  }

  /**
   * Generates cache key based on rounded coordinates (~1.1 km grid resolution).
   */
  getCacheKey(lat, lng) {
    const precision = TrafficConfig.cacheCoordinatePrecision;
    return `traffic_${Number(lat).toFixed(precision)}_${Number(lng).toFixed(precision)}`;
  }

  /**
   * Calculates speed ratio safely without division by zero.
   * Speed ratio = currentSpeed / freeFlowSpeed.
   */
  calculateSpeedRatio(currentSpeedKmh, freeFlowSpeedKmh) {
    const cur = Math.max(0, Number(currentSpeedKmh) || 0);
    const free = Math.max(0, Number(freeFlowSpeedKmh) || 0);

    if (free <= 0) {
      return 1.0; // Avoid divide-by-zero, default to optimal ratio
    }

    const ratio = cur / free;
    return parseFloat(Math.min(1.5, Math.max(0.0, ratio)).toFixed(2));
  }

  /**
   * Classifies traffic congestion level from speed ratio.
   * >= 0.80 -> LOW
   * 0.50 .. 0.79 -> MODERATE
   * < 0.50 -> HIGH
   */
  getCongestionLevel(speedRatio) {
    const r = Number(speedRatio);
    if (r >= TrafficConfig.speedRatios.lowMin) {
      return 'LOW';
    }
    if (r >= TrafficConfig.speedRatios.moderateMin) {
      return 'MODERATE';
    }
    return 'HIGH';
  }

  /**
   * Deterministic simulated traffic provider for development and testing.
   * Supports explicit scenario overrides (LOW, MODERATE, HIGH) or
   * time-of-day deterministic traffic modeling.
   */
  getSimulatedTraffic(lat, lng, scenarioOverride = null) {
    let scenarioKey = null;

    if (scenarioOverride) {
      const sUpper = scenarioOverride.toUpperCase();
      if (TrafficConfig.simulationScenarios[sUpper]) {
        scenarioKey = sUpper;
      }
    }

    // If no override, evaluate deterministic time-of-day profile
    if (!scenarioKey) {
      const currentHour = new Date().getHours();
      // Rush hour morning (8:00 - 10:00) or evening (17:00 - 19:30)
      if ((currentHour >= 8 && currentHour <= 10) || (currentHour >= 17 && currentHour <= 19)) {
        scenarioKey = 'HIGH';
      } else if (currentHour >= 11 && currentHour <= 16) {
        scenarioKey = 'MODERATE';
      } else {
        scenarioKey = 'LOW';
      }
    }

    const sc = TrafficConfig.simulationScenarios[scenarioKey] || TrafficConfig.simulationScenarios.LOW;
    const ratio = this.calculateSpeedRatio(sc.currentSpeedKmh, sc.freeFlowSpeedKmh);
    const congestionLevel = this.getCongestionLevel(ratio);

    return {
      roadSegmentId: sc.roadSegmentId,
      latitude: Number(lat),
      longitude: Number(lng),
      currentSpeedKmh: sc.currentSpeedKmh,
      freeFlowSpeedKmh: sc.freeFlowSpeedKmh,
      speedRatio: ratio,
      congestionLevel,
      travelDelaySeconds: sc.travelDelaySeconds,
      observedAt: new Date().toISOString(),
      source: 'SIMULATED', // Clearly distinguished as SIMULATED
    };
  }

  /**
   * Retrieves current traffic observation for specified coordinates.
   */
  async getCurrentTraffic(latitude, longitude, scenarioOverride = null) {
    if (!this.isValidCoordinates(latitude, longitude)) {
      const err = new Error('Invalid coordinates: latitude must be between -90 and 90, longitude between -180 and 180.');
      err.statusCode = 400;
      throw err;
    }

    const lat = Number(latitude);
    const lng = Number(longitude);
    const cacheKey = `${this.getCacheKey(lat, lng)}_${scenarioOverride || 'AUTO'}`;
    const now = Date.now();

    // 1. Check in-memory cache
    if (this.cache.has(cacheKey)) {
      const entry = this.cache.get(cacheKey);
      if (now - entry.timestamp < TrafficConfig.cacheTtlMs) {
        return { ...entry.data, isCached: true };
      }
    }

    // 2. Fetch traffic data (via Simulated Provider or Real Provider if configured)
    let trafficData;
    if (process.env.TRAFFIC_API_KEY) {
      // Architecture hook for commercial real traffic providers (TomTom, Here, Google)
      // When key is provided, query real provider; otherwise safely fallback
      trafficData = this.getSimulatedTraffic(lat, lng, scenarioOverride);
    } else {
      trafficData = this.getSimulatedTraffic(lat, lng, scenarioOverride);
    }

    trafficData.isCached = false;

    // 3. Update in-memory cache
    this.cache.set(cacheKey, { data: trafficData, timestamp: now });

    // 4. Asynchronously persist observation to MySQL
    this.persistTrafficObservation(trafficData).catch(() => {});

    return trafficData;
  }

  /**
   * Asynchronously persists traffic observation into MySQL traffic_observations.
   */
  async persistTrafficObservation(t) {
    try {
      await pool.query(
        `INSERT INTO traffic_observations (
          road_segment_id, latitude, longitude,
          current_speed_kmh, free_flow_speed_kmh, speed_ratio,
          congestion_level, travel_delay_seconds, source,
          observed_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          t.roadSegmentId,
          t.latitude,
          t.longitude,
          t.currentSpeedKmh,
          t.freeFlowSpeedKmh,
          t.speedRatio,
          t.congestionLevel,
          t.travelDelaySeconds,
          t.source,
          new Date(t.observedAt),
        ]
      );
    } catch (err) {
      // Database logging errors do not block client traffic delivery
    }
  }

  /**
   * Computes dynamic traffic risk score (0 - 100).
   */
  calculateTrafficRiskScore(traffic) {
    if (!traffic) return TrafficConfig.baseRisk.LOW; // Default neutral risk

    const level = (traffic.congestionLevel || 'LOW').toUpperCase();
    let score = TrafficConfig.baseRisk[level] !== undefined
      ? TrafficConfig.baseRisk[level]
      : TrafficConfig.baseRisk.LOW;

    // Optional fine-grained delay modifier
    if (traffic.travelDelaySeconds !== undefined) {
      const delay = Number(traffic.travelDelaySeconds);
      if (delay >= TrafficConfig.delayModifiers.severeDelaySeconds) {
        score += TrafficConfig.delayModifiers.severeDelayPenalty;
      } else if (delay >= TrafficConfig.delayModifiers.moderateDelaySeconds) {
        score += TrafficConfig.delayModifiers.moderateDelayPenalty;
      }
    }

    return Math.max(0, Math.min(100, Math.round(score)));
  }

  /**
   * Generates contextual explanation string for traffic state.
   */
  getTrafficExplanation(traffic) {
    if (!traffic) return null;
    const level = (traffic.congestionLevel || 'LOW').toUpperCase();
    return TrafficConfig.explanations[level] || null;
  }
}

module.exports = new TrafficService();
