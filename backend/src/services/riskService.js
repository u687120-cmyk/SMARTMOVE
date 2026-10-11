const pool = require('../config/database');
const RiskConfig = require('../config/riskConfig');
const weatherService = require('./weatherService');
const WeatherConfig = require('../config/weatherConfig');
const trafficService = require('./trafficService');
const TrafficConfig = require('../config/trafficConfig');

/**
 * SMARTMOVE AI — Dynamic Risk Engine Service (Phase 13, 14 & 15).
 *
 * Implements transparent, weighted multi-factor road risk evaluation.
 * Evaluates active hazards, hard braking patterns, recent risk events,
 * live meteorological observations, and dynamic traffic flow into a real-time 0-100 score.
 */
class RiskService {

  /**
   * Clamps numeric values strictly between min and max.
   */
  clamp(val, min = 0, max = 100) {
    if (isNaN(val)) return min;
    return Math.max(min, Math.min(max, val));
  }

  /**
   * Centralized risk classification function.
   * 0–30: LOW
   * 31–60: MEDIUM
   * 61–100: HIGH
   */
  getRiskLevel(score) {
    const clamped = this.clamp(score);
    if (clamped <= RiskConfig.thresholds.lowMax) {
      return 'LOW';
    }
    if (clamped <= RiskConfig.thresholds.mediumMax) {
      return 'MEDIUM';
    }
    return 'HIGH';
  }

  /**
   * Centralized recommendation generator.
   */
  getRecommendation(level) {
    return RiskConfig.recommendations[level] || RiskConfig.recommendations.MEDIUM;
  }

  /**
   * Resolves distance awareness multiplier for hazard proximity.
   */
  getDistanceMultiplier(distanceMeters) {
    const dist = (distanceMeters !== undefined && distanceMeters !== null) ? Number(distanceMeters) : 500;
    for (const rule of RiskConfig.distanceMultipliers) {
      if (dist <= rule.maxDistanceMeters) {
        return rule.multiplier;
      }
    }
    return 0.10;
  }

  /**
   * Calculates hazard risk contribution from ACTIVE hazards only.
   * Filters out RESOLVED hazards.
   */
  calculateHazardRisk(hazards = []) {
    // Only ACTIVE hazards contribute to current risk
    const active = hazards.filter(
      (h) => h && (h.status === 'ACTIVE' || h.status === 'UNDER_REVIEW')
    );

    if (active.length === 0) {
      return 0;
    }

    // Calculate individual scores with distance awareness
    const scores = active.map((h) => {
      const baseScore = RiskConfig.hazardSeverityScores[h.severity?.toUpperCase()] || RiskConfig.hazardSeverityScores.MEDIUM;
      const multiplier = this.getDistanceMultiplier(h.distanceMeters !== undefined ? h.distanceMeters : 500);
      return baseScore * multiplier;
    });

    // Highest hazard forms the base
    scores.sort((a, b) => b - a);
    let totalScore = scores[0];

    // Each additional hazard increments the cumulative risk
    for (let i = 1; i < scores.length; i++) {
      totalScore += scores[i] * 0.15;
    }

    return Math.round(this.clamp(totalScore, 0, 100));
  }

  /**
   * Calculates traffic risk factor.
   * Supports both normalized traffic objects and condition strings.
   */
  calculateTrafficRisk(trafficOrCondition = 'MODERATE') {
    if (typeof trafficOrCondition === 'object' && trafficOrCondition !== null) {
      return trafficService.calculateTrafficRiskScore(trafficOrCondition);
    }
    const cond = (trafficOrCondition || 'MODERATE').toUpperCase().trim();
    const score = TrafficConfig.baseRisk[cond] !== undefined
      ? TrafficConfig.baseRisk[cond]
      : (RiskConfig.trafficScores[cond] || 20);
    return score;
  }

  /**
   * Calculates weather risk factor.
   * Supports both normalized weather objects (with visibility/precipitation/wind)
   * and condition strings.
   */
  calculateWeatherRisk(weatherOrCondition = 'CLEAR') {
    if (typeof weatherOrCondition === 'object' && weatherOrCondition !== null) {
      return weatherService.calculateWeatherRiskScore(weatherOrCondition);
    }
    const cond = (weatherOrCondition || 'CLEAR').toUpperCase().trim();
    const score = WeatherConfig.baseRisk[cond] !== undefined
      ? WeatherConfig.baseRisk[cond]
      : (RiskConfig.weatherScores[cond] || 20);
    return score;
  }

  /**
   * Calculates hard braking factor from recent braking events.
   * Uses recent time-window scaling.
   */
  calculateBrakingRisk(eventCount = 0) {
    const count = Math.max(0, parseInt(eventCount, 10) || 0);
    for (const tier of RiskConfig.hardBrakingScores) {
      if (count <= tier.maxCount) {
        return tier.score;
      }
    }
    return 90;
  }

  /**
   * Calculates recent accident / risk event factor.
   */
  calculateEventRisk(eventCount = 0) {
    const count = Math.max(0, parseInt(eventCount, 10) || 0);
    for (const tier of RiskConfig.eventRiskScores) {
      if (count <= tier.maxCount) {
        return tier.score;
      }
    }
    return 90;
  }

  /**
   * Computes the overall weighted risk score, classification, and explanation.
   */
  calculateRisk({
    hazardRisk = 0,
    trafficRisk = 20,
    weatherRisk = 10,
    brakingRisk = 10,
    eventRisk = 10,
    hazardCount = 0,
    trafficCondition = 'LOW',
    weatherCondition = 'CLEAR',
    trafficData = null,
    weatherData = null,
    trafficAvailable = true,
    weatherAvailable = true,
    brakingCount = 0,
    eventCount = 0,
  } = {}) {
    const h = this.clamp(hazardRisk);
    const t = this.clamp(trafficRisk);
    const w = this.clamp(weatherRisk);
    const b = this.clamp(brakingRisk);
    const e = this.clamp(eventRisk);

    const weightedScore =
      h * RiskConfig.weights.hazard +
      t * RiskConfig.weights.traffic +
      w * RiskConfig.weights.weather +
      b * RiskConfig.weights.hardBraking +
      e * RiskConfig.weights.riskEvents;

    const finalScore = Math.round(this.clamp(weightedScore, 0, 100));
    const level = this.getRiskLevel(finalScore);
    const recommendation = this.getRecommendation(level);

    // Build human-readable explanations (WHY the risk is at this level)
    const reasons = [];
    if (h >= 60) {
      reasons.push(hazardCount > 1 ? `${hazardCount} active road hazards detected ahead` : 'High-severity road hazard reported nearby');
    } else if (h > 20) {
      reasons.push('Road hazard reported in vicinity');
    }

    // Traffic reason based on dynamic traffic metrics
    if (trafficData) {
      const trafficExp = trafficService.getTrafficExplanation(trafficData);
      if (trafficExp && trafficData.congestionLevel !== 'LOW') {
        reasons.push(trafficExp);
      }
    } else if (trafficCondition) {
      if (t >= 80) {
        reasons.push('Heavy traffic congestion');
      } else if (t >= 50) {
        reasons.push('Moderate traffic flow');
      }
    }

    // Weather reason based on actual meteorological data
    if (weatherData) {
      const weatherExplanation = weatherService.getWeatherExplanation(weatherData);
      if (weatherExplanation) {
        reasons.push(weatherExplanation);
      }
    } else if (weatherCondition) {
      if (w >= 70) {
        reasons.push(`Adverse weather: ${weatherCondition.toLowerCase().replace('_', ' ')}`);
      } else if (w >= 40) {
        reasons.push('Wet or rainy conditions');
      }
    }

    if (b >= 60) {
      reasons.push('Frequent hard braking patterns detected recently');
    }

    if (e >= 60) {
      reasons.push('Recent accident or safety event recorded');
    }

    if (reasons.length === 0) {
      reasons.push('Clear driving conditions and optimal road safety');
    }

    return {
      score: finalScore,
      level,
      recommendation,
      factors: {
        hazards: h,
        traffic: t,
        weather: w,
        hardBraking: b,
        events: e,
      },
      trafficAvailable,
      traffic: trafficData ? {
        roadSegmentId: trafficData.roadSegmentId,
        currentSpeedKmh: trafficData.currentSpeedKmh,
        freeFlowSpeedKmh: trafficData.freeFlowSpeedKmh,
        speedRatio: trafficData.speedRatio,
        congestionLevel: trafficData.congestionLevel,
        travelDelaySeconds: trafficData.travelDelaySeconds,
        source: trafficData.source,
      } : null,
      weatherAvailable,
      weather: weatherData ? {
        temperatureC: weatherData.temperatureC,
        condition: weatherData.condition,
        precipitationMm: weatherData.precipitationMm,
        visibilityKm: weatherData.visibilityKm,
        windSpeedKmh: weatherData.windSpeedKmh,
      } : null,
      reasons,
    };
  }

  /**
   * Fetches real database events within recent time window, combines with
   * live meteorological and dynamic traffic observations, and computes live risk state.
   */
  async getCurrentRiskForUser(userId, options = {}) {
    // 1. Fetch active hazards from database (status ACTIVE or UNDER_REVIEW)
    let activeHazards = [];
    try {
      const [rows] = await pool.query(
        `SELECT id, type, severity, status, latitude, longitude
         FROM hazards
         WHERE status = 'ACTIVE' OR status = 'UNDER_REVIEW'
         ORDER BY confidence DESC`
      );
      // Assign default demo proximity distance (300m - 800m) for nearby evaluation
      activeHazards = rows.map((r, idx) => ({
        ...r,
        distanceMeters: 300 + idx * 250,
      }));
    } catch (err) {
      // Fallback if DB offline during development
      activeHazards = [];
    }

    // 2. Fetch recent hard braking events (within last 30 minutes)
    let recentBrakingCount = 0;
    try {
      const [bRows] = await pool.query(
        `SELECT COUNT(*) AS count
         FROM risk_events
         WHERE event_type = 'HARD_BRAKING'
           AND recorded_at >= DATE_SUB(NOW(), INTERVAL ? MINUTE)`,
        [RiskConfig.timeWindowMinutes]
      );
      recentBrakingCount = Number(bRows[0]?.count || 0);
    } catch (err) {
      recentBrakingCount = 0;
    }

    // 3. Fetch recent accident & safety events (within last 30 minutes)
    let recentEventCount = 0;
    try {
      const [eRows] = await pool.query(
        `SELECT COUNT(*) AS count
         FROM risk_events
         WHERE event_type IN ('ACCIDENT', 'HAZARD', 'SPEED_PATTERN')
           AND recorded_at >= DATE_SUB(NOW(), INTERVAL ? MINUTE)`,
        [RiskConfig.timeWindowMinutes]
      );
      recentEventCount = Number(eRows[0]?.count || 0);
    } catch (err) {
      recentEventCount = 0;
    }

    // 4. Live Weather Integration (Phase 14)
    let weatherData = null;
    let weatherAvailable = false;
    let weatherCondition = 'CLEAR';
    let weatherRisk = 20;

    const lat = options.latitude !== undefined ? Number(options.latitude) : WeatherConfig.defaultLocation.latitude;
    const lng = options.longitude !== undefined ? Number(options.longitude) : WeatherConfig.defaultLocation.longitude;

    if (options.weatherCondition) {
      // Explicit override for test scenarios
      weatherCondition = options.weatherCondition.toUpperCase();
      weatherRisk = this.calculateWeatherRisk(weatherCondition);
      weatherAvailable = true;
    } else if (weatherService.isValidCoordinates(lat, lng)) {
      try {
        weatherData = await weatherService.getCurrentWeather(lat, lng);
        weatherCondition = weatherData.condition;
        weatherRisk = weatherService.calculateWeatherRiskScore(weatherData);
        weatherAvailable = true;
      } catch (wErr) {
        // Fallback: neutral default risk factor, mark weather unavailable
        weatherAvailable = false;
        weatherCondition = null;
        weatherRisk = 20;
      }
    }

    // 5. Dynamic Traffic Integration (Phase 15)
    let trafficData = null;
    let trafficAvailable = false;
    let trafficCondition = 'LOW';
    let trafficRisk = 20;

    if (options.trafficCondition) {
      // Explicit override for scenario tests
      trafficCondition = options.trafficCondition.toUpperCase();
      trafficRisk = this.calculateTrafficRisk(trafficCondition);
      trafficAvailable = true;
    } else if (trafficService.isValidCoordinates(lat, lng)) {
      try {
        trafficData = await trafficService.getCurrentTraffic(lat, lng);
        trafficCondition = trafficData.congestionLevel;
        trafficRisk = trafficService.calculateTrafficRiskScore(trafficData);
        trafficAvailable = true;
      } catch (tErr) {
        // Fallback: neutral default risk factor, mark traffic unavailable
        trafficAvailable = false;
        trafficCondition = null;
        trafficRisk = 20;
      }
    }

    // 6. Compute sub-factor scores
    const hazardRisk = this.calculateHazardRisk(activeHazards);
    const brakingRisk = this.calculateBrakingRisk(recentBrakingCount);
    const eventRisk = this.calculateEventRisk(recentEventCount);

    // 7. Calculate combined risk
    return this.calculateRisk({
      hazardRisk,
      trafficRisk,
      weatherRisk,
      brakingRisk,
      eventRisk,
      hazardCount: activeHazards.length,
      trafficCondition,
      weatherCondition,
      trafficData,
      weatherData,
      trafficAvailable,
      weatherAvailable,
      brakingCount: recentBrakingCount,
      eventCount: recentEventCount,
    });
  }
}

module.exports = new RiskService();
