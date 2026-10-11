/**
 * SMARTMOVE AI — Route Comparison & Recommendation Configuration (Phase 16)
 *
 * Centralized decision weights, detour thresholds, high-risk segment rules,
 * and deterministic candidate route profiles.
 */

const RouteConfig = {
  // Decision scoring weights for route comparison
  // Balances safety (primary), travel time, and distance
  weights: {
    safety: 0.60,       // 60% weight on safety score (100 - riskScore)
    travelTime: 0.25,   // 25% weight on time efficiency
    distance: 0.15,     // 15% weight on distance efficiency
  },

  // Maximum acceptable detour: 25% additional travel time over the fastest route
  maxAcceptableDetourPercent: 25,

  // Threshold for high-risk segment penalty
  highRiskSegmentThreshold: 65,

  // Phase 17: Dynamic En-Route Rerouting & Safety Prompts Thresholds
  rerouteThresholds: {
    riskIncreaseThreshold: 15,            // Trigger reroute when current route worsens by >= 15 points
    safetyImprovementMinThreshold: 15,     // Alternative must be at least 15 points safer
    hazardAheadMaxDistanceMeters: 1000,    // Route-relevant hazard proximity limit (meters)
    maxAcceptableDetourPercent: 25,        // Alternative cannot exceed 25% extra travel time
    cooldownSeconds: 120,                  // 2-minute cooldown before repeating prompt
  },

  // Candidate route archetypes for simulated routing provider
  candidateRoutes: [
    {
      routeId: 'route-a',
      name: 'Via FC Road Arterial',
      description: 'Central urban corridor with frequent signals and high commercial activity',
      distanceKm: 8.2,
      baseDurationMinutes: 24,
      segments: [
        { segmentId: 'seg-a1', name: 'FC Road North', lengthKm: 2.0, baseTraffic: 'HIGH', hazardType: 'POTHOLE', hazardSeverity: 'HIGH' },
        { segmentId: 'seg-a2', name: 'Deccan Gymkhana', lengthKm: 2.2, baseTraffic: 'HIGH', hazardType: 'CONSTRUCTION', hazardSeverity: 'MEDIUM' },
        { segmentId: 'seg-a3', name: 'Karve Road Junction', lengthKm: 2.0, baseTraffic: 'MODERATE', hazardType: null, hazardSeverity: null },
        { segmentId: 'seg-a4', name: 'Kothrud Approach', lengthKm: 2.0, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
      ],
    },
    {
      routeId: 'route-b',
      name: 'Via SB Road Bypass',
      description: 'Intermediate arterial bypass avoiding core city congestion',
      distanceKm: 9.1,
      baseDurationMinutes: 27,
      segments: [
        { segmentId: 'seg-b1', name: 'Senapati Bapat Road', lengthKm: 2.5, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
        { segmentId: 'seg-b2', name: 'Law College Road', lengthKm: 2.3, baseTraffic: 'LOW', hazardType: 'POTHOLE', hazardSeverity: 'LOW' },
        { segmentId: 'seg-b3', name: 'Paud Road Flyover', lengthKm: 2.3, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
        { segmentId: 'seg-b4', name: 'Mayur Colony Link', lengthKm: 2.0, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
      ],
    },
    {
      routeId: 'route-c',
      name: 'Via Outer University Link',
      description: 'Perimeter ring road with free-flow traffic and minimal road friction',
      distanceKm: 10.5,
      baseDurationMinutes: 29,
      segments: [
        { segmentId: 'seg-c1', name: 'University Outer Circle', lengthKm: 3.0, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
        { segmentId: 'seg-c2', name: 'Pashan Link Road', lengthKm: 3.0, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
        { segmentId: 'seg-c3', name: 'Bavdhan Connector', lengthKm: 2.5, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
        { segmentId: 'seg-c4', name: 'Chandani Chowk Approach', lengthKm: 2.0, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
      ],
    },
  ],
};

module.exports = RouteConfig;
