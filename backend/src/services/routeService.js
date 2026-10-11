const RouteConfig = require('../config/routeConfig');
const riskService = require('./riskService');
const weatherService = require('./weatherService');
const trafficService = require('./trafficService');
const pool = require('../config/database');

/**
 * SMARTMOVE AI — Dynamic Route Service (Phase 16).
 *
 * Implements route candidate evaluation, multi-segment risk aggregation,
 * high-risk segment penalties, safety scoring, detour bounds,
 * and transparent recommendation rationale.
 */
const https = require('https');

class RouteService {

  /**
   * Validates coordinate boundary limits.
   */
  isValidCoordinates(lat, lng) {
    if (lat === null || lat === undefined || lng === null || lng === undefined) return false;
    const nLat = Number(lat);
    const nLng = Number(lng);
    return !isNaN(nLat) && !isNaN(nLng) && nLat >= -90 && nLat <= 90 && nLng >= -180 && nLng <= 180;
  }

  /**
   * Helper to perform HTTPS GET requests returning JSON.
   */
  async _fetchJson(url) {
    return new Promise((resolve, reject) => {
      https.get(url, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          try {
            resolve(JSON.parse(data));
          } catch (e) {
            reject(new Error(`Failed to parse JSON response: ${e.message}`));
          }
        });
      }).on('error', (err) => {
        reject(err);
      });
    });
  }

  /**
   * Generates candidate routes for specified origin and destination.
   * Uses Google Directions API when available, otherwise falls back to deterministic simulation.
   */
  async getCandidateRoutes(origin, destination, overrides = {}) {
    const config = require('../config/environment');
    const apiKey = config.googleMapsApiKey;

    if (apiKey && origin && destination) {
      try {
        const originStr = (typeof origin === 'object' && origin.lat !== undefined)
          ? `${origin.lat},${origin.lng}`
          : encodeURIComponent(String(origin));
        const destStr = (typeof destination === 'object' && destination.lat !== undefined)
          ? `${destination.lat},${destination.lng}`
          : encodeURIComponent(String(destination));

        const url = `https://maps.googleapis.com/maps/api/directions/json?origin=${originStr}&destination=${destStr}&alternatives=true&key=${apiKey}`;
        const data = await this._fetchJson(url);

        if (data.status === 'OK' && Array.isArray(data.routes) && data.routes.length > 0) {
          return data.routes.map((r, index) => {
            const leg = r.legs && r.legs[0] ? r.legs[0] : {};
            const distanceKm = leg.distance ? Number((leg.distance.value / 1000).toFixed(1)) : 12.0;
            const durationMins = leg.duration_in_traffic
              ? Math.round(leg.duration_in_traffic.value / 60)
              : leg.duration
                ? Math.round(leg.duration.value / 60)
                : 25;

            // Extract road segments from steps
            const segments = (leg.steps || []).map((step, sIdx) => {
              const cleanInstruction = (step.html_instructions || '')
                .replace(/<[^>]*>?/gm, ' ')
                .replace(/\s+/g, ' ')
                .trim();
              const stepKm = step.distance ? Number((step.distance.value / 1000).toFixed(1)) : 1.0;
              return {
                segmentId: `step-${index}-${sIdx}`,
                name: cleanInstruction || `Segment ${sIdx + 1}`,
                lengthKm: stepKm > 0 ? stepKm : 0.5,
                baseTraffic: 'LOW',
                currentTraffic: 'MODERATE',
                hazardType: null,
                hazardSeverity: null,
                hazardStatus: null,
              };
            });

            const candidate = {
              routeId: index === 0 ? 'route-primary' : `route-alt-${index}`,
              name: r.summary ? `Via ${r.summary}` : `Route ${index + 1}`,
              description: leg.start_address && leg.end_address
                ? `${leg.start_address.split(',')[0]} → ${leg.end_address.split(',')[0]}`
                : `Driving route via ${r.summary || 'main road'}`,
              lengthKm: distanceKm,
              estimatedMinutes: durationMins,
              origin,
              destination,
              overviewPolyline: r.overview_polyline ? r.overview_polyline.points : null,
              source: 'GOOGLE_DIRECTIONS',
              segments: segments.length > 0 ? segments : [
                {
                  segmentId: `seg-${index}-1`,
                  name: r.summary || 'Main Highway',
                  lengthKm: distanceKm,
                  baseTraffic: 'LOW',
                  currentTraffic: 'MODERATE',
                  hazardType: null,
                  hazardSeverity: null,
                  hazardStatus: null,
                }
              ]
            };

            if (overrides[candidate.routeId]) {
              Object.assign(candidate, overrides[candidate.routeId]);
            }
            return candidate;
          });
        }
      } catch (err) {
        console.warn(`[RouteService] Google Directions API warning: ${err.message}`);
      }
    }

    // Fallback: Deterministic templates
    const candidateTemplates = JSON.parse(JSON.stringify(RouteConfig.candidateRoutes));

    return candidateTemplates.map((r) => {
      if (overrides[r.routeId]) {
        Object.assign(r, overrides[r.routeId]);
      }
      return {
        ...r,
        origin,
        destination,
        source: 'SIMULATED',
      };
    });
  }

  /**
   * Evaluates an individual road segment using the existing Risk Engine formula
   * with passenger sensitivity weighting and live DB hazards.
   */
  evaluateSegmentRisk({ segment, weatherRisk, activeHazards = [], passengerProfile = 'HEALTHY' }) {
    // 1. Traffic factor from segment current/base traffic
    const trafficCond = (segment.currentTraffic || segment.baseTraffic || 'LOW').toUpperCase();
    const trafficRisk = riskService.calculateTrafficRisk(trafficCond);

    // 2. Hazard factor on this segment from template AND live MySQL DB hazards
    let hazardRisk = 10;
    let detectedHazardType = segment.hazardType || null;
    let detectedSeverity = segment.hazardSeverity || null;

    if (segment.hazardType && segment.hazardSeverity) {
      const isResolved = segment.hazardStatus === 'RESOLVED';
      if (!isResolved) {
        hazardRisk = segment.hazardSeverity === 'HIGH' ? 85 : segment.hazardSeverity === 'MEDIUM' ? 55 : 30;
      }
    }

    // Check if any live DB hazard matches this segment corridor
    const segNameLower = (segment.name || '').toLowerCase();
    const matchingDbHazard = activeHazards.find(h => {
      const loc = (h.location_name || '').toLowerCase();
      return segNameLower.includes(loc) || loc.includes(segNameLower) || (h.severity === 'HIGH' && segNameLower.includes('main'));
    });

    if (matchingDbHazard) {
      detectedHazardType = matchingDbHazard.type || 'POTHOLE';
      detectedSeverity = matchingDbHazard.severity || 'HIGH';
      hazardRisk = Math.max(hazardRisk, detectedSeverity === 'HIGH' ? 90 : 60);
    }

    // 3. Passenger Profile sensitivity weighting
    let wHazard = 0.30;
    let wTraffic = 0.25;
    let wWeather = 0.15;
    let wBraking = 0.15;
    let wEvents = 0.15;

    if (passengerProfile === 'PREGNANT_WEAK') {
      wHazard = 0.45;   // 1.5x penalty on potholes & vertical jolts
      wBraking = 0.25;  // Severe penalty on harsh deceleration
      wTraffic = 0.15;
      wWeather = 0.08;
      wEvents = 0.07;
    } else if (passengerProfile === 'AMBULANCE_EMERGENCY') {
      wTraffic = 0.45;  // 1.8x penalty on traffic bottlenecks
      wHazard = 0.15;
      wBraking = 0.15;
      wWeather = 0.13;
      wEvents = 0.12;
    } else if (passengerProfile === 'FAMILY_ELDERLY_CHILD') {
      wHazard = 0.35;
      wTraffic = 0.20;
      wWeather = 0.20;
      wBraking = 0.13;
      wEvents = 0.12;
    }

    const h = riskService.clamp(hazardRisk);
    const t = riskService.clamp(trafficRisk);
    const w = riskService.clamp(weatherRisk);
    const b = 10;
    const e = 10;

    const rawScore =
      h * wHazard +
      t * wTraffic +
      w * wWeather +
      b * wBraking +
      e * wEvents;

    const segmentRisk = Math.round(riskService.clamp(rawScore, 0, 100));
    return {
      segmentId: segment.segmentId,
      name: segment.name,
      lengthKm: segment.lengthKm,
      traffic: trafficCond,
      hazardType: detectedHazardType,
      hazardSeverity: detectedSeverity,
      riskScore: segmentRisk,
    };
  }

  /**
   * Aggregates segment risks into an overall route risk score
   * with high-risk segment penalty.
   */
  calculateRouteRisk(evaluatedSegments) {
    if (!evaluatedSegments || evaluatedSegments.length === 0) {
      return 20;
    }

    const scores = evaluatedSegments.map((s) => s.riskScore);
    const avgScore = scores.reduce((sum, val) => sum + val, 0) / scores.length;
    const maxScore = Math.max(...scores);

    // High-risk segment penalty:
    // If any individual segment is severe (>= threshold), it heavily weights the route risk
    let combinedRisk;
    if (maxScore >= RouteConfig.highRiskSegmentThreshold) {
      combinedRisk = Math.round(0.65 * avgScore + 0.35 * maxScore);
    } else {
      combinedRisk = Math.round(avgScore);
    }

    return Math.max(0, Math.min(100, combinedRisk));
  }

  /**
   * Converts a risk score into a 0 - 100 safety score.
   */
  calculateSafetyScore(riskScore) {
    return Math.max(0, Math.min(100, 100 - riskScore));
  }

  /**
   * Computes recommendation scores and evaluates routes.
   */
  async compareAndRecommendRoutes(origin, destination, options = {}) {
    // 1. Fetch current weather for the origin/corridor
    let weatherRisk = 20;
    try {
      if (options.weatherCondition) {
        weatherRisk = riskService.calculateWeatherRisk(options.weatherCondition);
      } else if (weatherService.isValidCoordinates(origin.latitude, origin.longitude)) {
        const wData = await weatherService.getCurrentWeather(origin.latitude, origin.longitude);
        weatherRisk = weatherService.calculateWeatherRiskScore(wData);
      }
    } catch (_) {
      weatherRisk = 20;
    }

    // 2. Fetch active and reopened hazards from MySQL database
    let dbHazards = [];
    try {
      const [hRows] = await pool.query(
        "SELECT * FROM hazards WHERE status IN ('ACTIVE', 'UNDER_REVIEW', 'REOPENED')"
      );
      dbHazards = hRows || [];
    } catch (e) {
      console.warn('[RouteService] Notice querying live hazards:', e.message);
    }

    const passengerProfile = options.passengerProfile || 'HEALTHY';

    // 3. Fetch candidate routes
    const candidateRoutes = await this.getCandidateRoutes(origin, destination, options.routeOverrides);

    // 4. Evaluate each route with live hazards and passenger profile sensitivity
    const evaluatedRoutes = candidateRoutes.map((r) => {
      const evaluatedSegments = (r.segments || []).map((seg) =>
        this.evaluateSegmentRisk({
          segment: seg,
          weatherRisk,
          activeHazards: dbHazards,
          passengerProfile,
        })
      );

      const routeRisk = this.calculateRouteRisk(evaluatedSegments);
      const riskLevel = riskService.getRiskLevel(routeRisk);
      const safetyScore = this.calculateSafetyScore(routeRisk);

      // Count hazards and determine predominant traffic
      const activeHazardsCount = evaluatedSegments.filter((s) => s.hazardType && s.hazardSeverity).length;
      const trafficLevels = evaluatedSegments.map((s) => s.traffic);
      const predominantTraffic = trafficLevels.includes('HIGH') ? 'HIGH' : trafficLevels.includes('MODERATE') ? 'MODERATE' : 'LOW';

      return {
        routeId: r.routeId,
        name: r.name,
        description: r.description,
        distanceKm: r.distanceKm,
        durationMinutes: r.durationMinutes || r.baseDurationMinutes,
        riskScore: routeRisk,
        riskLevel,
        safetyScore,
        traffic: predominantTraffic,
        hazards: activeHazardsCount,
        source: r.source,
        segments: evaluatedSegments,
      };
    });

    if (evaluatedRoutes.length === 0) {
      const err = new Error('No suitable routes found');
      err.statusCode = 404;
      throw err;
    }

    // 4. Calculate baseline benchmarks (min duration, min distance)
    const minDuration = Math.min(...evaluatedRoutes.map((r) => r.durationMinutes));
    const minDistance = Math.min(...evaluatedRoutes.map((r) => r.distanceKm));

    // 5. Score and rank routes
    const scoredRoutes = evaluatedRoutes.map((r) => {
      // Time score (100 = fastest, decreases as duration increases)
      const extraTimeRatio = minDuration > 0 ? (r.durationMinutes - minDuration) / minDuration : 0;
      const timeScore = Math.max(0, 100 - extraTimeRatio * 100);

      // Distance score (100 = shortest)
      const extraDistRatio = minDistance > 0 ? (r.distanceKm - minDistance) / minDistance : 0;
      const distScore = Math.max(0, 100 - extraDistRatio * 100);

      // Weighted combination: 60% safety + 25% travel time + 15% distance
      let recScore =
        r.safetyScore * RouteConfig.weights.safety +
        timeScore * RouteConfig.weights.travelTime +
        distScore * RouteConfig.weights.distance;

      // Detour Penalty Check:
      // If detour > 25%, penalize recommendation score unless safety gain is massive
      const detourPercent = extraTimeRatio * 100;
      const fastestRoute = evaluatedRoutes.find((x) => x.durationMinutes === minDuration);
      const riskReduction = fastestRoute ? fastestRoute.riskScore - r.riskScore : 0;

      if (detourPercent > RouteConfig.maxAcceptableDetourPercent && riskReduction < 40) {
        recScore -= 20; // Detour penalty
      }

      return {
        ...r,
        recommendationScore: Math.round(Math.max(0, Math.min(100, recScore))),
      };
    });

    // Sort by recommendationScore descending
    scoredRoutes.sort((a, b) => b.recommendationScore - a.recommendationScore);

    // Identify winners
    const recommended = scoredRoutes[0];
    const safest = [...scoredRoutes].sort((a, b) => a.riskScore - b.riskScore)[0];
    const fastest = [...scoredRoutes].sort((a, b) => a.durationMinutes - b.durationMinutes)[0];
    const shortest = [...scoredRoutes].sort((a, b) => a.distanceKm - b.distanceKm)[0];

    // 6. Generate human-readable recommendation reasons
    const recommendationReason = this.generateRecommendationReason(recommended, fastest, safest);
    const routesWithReasons = scoredRoutes.map((r) => ({
      ...r,
      reason: r.routeId === recommended.routeId ? recommendationReason : this.generateRouteSummaryReason(r, fastest),
    }));

    return {
      recommendedRouteId: recommended.routeId,
      safestRouteId: safest.routeId,
      fastestRouteId: fastest.routeId,
      shortestRouteId: shortest.routeId,
      recommendationReason,
      routes: routesWithReasons,
    };
  }

  /**
   * Generates natural-language reason for why a route was recommended.
   */
  generateRecommendationReason(recommended, fastest, safest) {
    if (recommended.routeId === fastest.routeId && recommended.routeId === safest.routeId) {
      return 'Recommended as the fastest and safest corridor with optimal driving conditions.';
    }

    if (recommended.routeId !== fastest.routeId) {
      const extraMin = recommended.durationMinutes - fastest.durationMinutes;
      if (recommended.hazards < fastest.hazards && recommended.traffic !== 'HIGH') {
        return `Recommended because it avoids high-risk hazards with only ${extraMin} extra minutes.`;
      }
      if (recommended.traffic === 'LOW' && fastest.traffic === 'HIGH') {
        return `Recommended because traffic is significantly lighter with only ${extraMin} extra minutes.`;
      }
      return `Recommended because it has lower current road risk with only ${extraMin} extra minutes.`;
    }

    if (recommended.routeId === safest.routeId) {
      return 'Recommended because it has the lowest current road risk.';
    }

    return 'Recommended for optimal balance of road safety and travel efficiency.';
  }

  /**
   * Generates contextual summary reason for non-recommended routes.
   */
  generateRouteSummaryReason(route, fastest) {
    if (route.riskScore >= 65) {
      return route.hazards > 0
        ? 'Higher current risk due to active road hazards and traffic congestion.'
        : 'Higher current risk due to heavy traffic congestion.';
    }
    if (route.durationMinutes > fastest.durationMinutes * 1.25) {
      return 'Safer corridor but requires an excessive travel time detour.';
    }
    return 'Alternative candidate corridor with moderate road safety.';
  }

  /**
   * Phase 17: Evaluates active route during a trip and detects meaningful deterioration.
   * Compares the current route against alternatives and decides if an en-route safety prompt is warranted.
   */
  async evaluateEnRouteReroute(currentRouteId, origin, destination, currentCoordinates, options = {}) {
    // 1. Evaluate all candidate corridors under current environmental conditions
    const comparison = await this.compareAndRecommendRoutes(origin, destination, options);
    const { routes } = comparison;

    // 2. Locate current route
    const currentRoute = routes.find((r) => r.routeId === currentRouteId) || routes[0];

    // 3. Find candidate alternatives
    const alternatives = routes.filter((r) => r.routeId !== currentRoute.routeId);
    if (alternatives.length === 0) {
      return {
        shouldReroute: false,
        isPromptSuppressed: false,
        currentRoute,
        reason: 'No alternative routes available.',
      };
    }

    // 4. Select best alternative (highest recommendationScore)
    const bestAlternative = alternatives[0];

    // 5. Evaluate deterioration criteria:
    // - Criterion A: Current route is HIGH risk (>= 61) and Alternative is safer
    // - Criterion B: Alternative is >= 15 points safer (riskReduction >= 15)
    // - Criterion C: Heavy traffic or hazards ahead on current corridor
    const riskReduction = currentRoute.riskScore - bestAlternative.riskScore;
    const timeDifferenceMinutes = bestAlternative.durationMinutes - currentRoute.durationMinutes;
    const detourPercent = currentRoute.durationMinutes > 0
      ? (timeDifferenceMinutes / currentRoute.durationMinutes) * 100
      : 0;

    const isCurrentRouteSevere = currentRoute.riskScore >= 61;
    const isAlternativeMeaningfullySafer = riskReduction >= RouteConfig.rerouteThresholds.safetyImprovementMinThreshold;
    const isDetourAcceptable = detourPercent <= RouteConfig.rerouteThresholds.maxAcceptableDetourPercent || (riskReduction >= 30 && timeDifferenceMinutes <= 8);

    // Check hazard ahead presence on current route
    const activeHazardsOnCurrent = (currentRoute.segments || [])
      .filter((s) => s.hazardType && s.hazardSeverity && s.hazardSeverity !== 'LOW');
    const hasSevereHazardAhead = activeHazardsOnCurrent.length > 0;

    // Decide whether to offer reroute
    const shouldReroute =
      (isCurrentRouteSevere || isAlternativeMeaningfullySafer || hasSevereHazardAhead) &&
      bestAlternative.riskScore < currentRoute.riskScore &&
      isDetourAcceptable;

    // 6. Cooldown / de-duplication check
    let isPromptSuppressed = false;
    if (options.lastPromptTimestamp && options.lastPromptRouteId === bestAlternative.routeId) {
      const elapsedSeconds = (Date.now() - Number(options.lastPromptTimestamp)) / 1000;
      if (elapsedSeconds < RouteConfig.rerouteThresholds.cooldownSeconds) {
        isPromptSuppressed = true;
      }
    }

    // 7. Assemble safety prompt
    let safetyPrompt = null;
    if (shouldReroute && !isPromptSuppressed) {
      const extraTimeText = timeDifferenceMinutes > 0 ? `+${timeDifferenceMinutes} min` : `${Math.abs(timeDifferenceMinutes)} min faster`;
      const hazardText = hasSevereHazardAhead
        ? `${activeHazardsOnCurrent[0].hazardType.replace('_', ' ')} reported ahead.`
        : currentRoute.traffic === 'HIGH'
        ? 'Heavy traffic congestion reported ahead.'
        : 'Higher road risk detected ahead.';

      safetyPrompt = {
        title: 'SAFETY UPDATE',
        headline: hasSevereHazardAhead ? 'Hazard Detected Ahead' : 'Traffic Deterioration Ahead',
        message: `${hazardText} Current route risk: ${currentRoute.riskLevel} (${currentRoute.riskScore}/100). A safer alternative is available via ${bestAlternative.name}.`,
        summary: `${extraTimeText} • Risk: ${bestAlternative.riskScore} (${bestAlternative.riskLevel})`,
        delayMinutes: Math.max(0, timeDifferenceMinutes),
        currentRisk: currentRoute.riskScore,
        currentRiskLevel: currentRoute.riskLevel,
        currentRouteId: currentRoute.routeId,
        alternativeRouteId: bestAlternative.routeId,
        alternativeRouteName: bestAlternative.name,
        alternativeRisk: bestAlternative.riskScore,
        alternativeRiskLevel: bestAlternative.riskLevel,
        alternativeDurationMinutes: bestAlternative.durationMinutes,
        alternativeDistanceKm: bestAlternative.distanceKm,
        actionLabel: 'SWITCH ROUTE',
        dismissLabel: 'KEEP CURRENT ROUTE',
        timestamp: Date.now(),
      };
    }

    return {
      shouldReroute: shouldReroute && !isPromptSuppressed,
      isPromptSuppressed,
      currentRoute,
      alternativeRoute: bestAlternative,
      safetyPrompt,
      allRoutes: routes,
      reason: shouldReroute
        ? `Safer alternative ${bestAlternative.name} offers lower risk (${bestAlternative.riskScore} vs ${currentRoute.riskScore}).`
        : 'Current route remains within safe and acceptable travel parameters.',
    };
  }

  /**
   * Records a route switch event for an active trip.
   */
  async recordRouteSwitch(tripId, userId, switchData) {
    if (!tripId || !userId) return null;
    try {
      const queryPromise = pool.query(
        `INSERT INTO risk_events (trip_id, user_id, event_type, risk_score, latitude, longitude, metadata_json)
         VALUES (?, ?, 'OTHER', ?, ?, ?, ?)`,
        [
          tripId,
          userId,
          switchData.newRisk || 20,
          switchData.latitude || 18.5204,
          switchData.longitude || 73.8567,
          JSON.stringify({
            action: 'ROUTE_SWITCH',
            previousRouteId: switchData.previousRouteId,
            newRouteId: switchData.newRouteId,
            reason: switchData.reason || 'Driver accepted safer route prompt',
            oldRisk: switchData.oldRisk,
            newRisk: switchData.newRisk,
            oldEtaMinutes: switchData.oldEtaMinutes,
            newEtaMinutes: switchData.newEtaMinutes,
            switchedAt: new Date().toISOString(),
          }),
        ]
      );

      const timeoutPromise = new Promise((_, reject) =>
        setTimeout(() => reject(new Error('Database query timed out')), 1200)
      );

      const [rows] = await Promise.race([queryPromise, timeoutPromise]);
      return rows ? rows.insertId : null;
    } catch (err) {
      console.warn('[RouteService] Failed to persist route switch event:', err.message);
      return null;
    }
  }
}

module.exports = new RouteService();
