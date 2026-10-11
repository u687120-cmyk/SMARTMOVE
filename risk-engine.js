/**
 * SmartMove AI — Dynamic Risk Engine with Passenger Sensitivity & Civic Weighting
 * All weight matrices strictly sum to 1.00 (100% normalized).
 */

export const BASE_WEIGHTS = Object.freeze({
  traffic: 0.20,
  hardBraking: 0.20,
  weather: 0.15,
  roadHazards: 0.20,
  accidentHistory: 0.15,
  speedPattern: 0.10,
});

export const PASSENGER_PROFILES = Object.freeze({
  HEALTHY: {
    label: 'Standard / Healthy',
    description: 'Balanced risk distribution',
    weights: {
      traffic: 0.20,
      hardBraking: 0.20,
      weather: 0.15,
      roadHazards: 0.20,
      accidentHistory: 0.15,
      speedPattern: 0.10,
    },
  },
  PREGNANT_WEAK: {
    label: 'Pregnant / Fracture / Weak',
    description: 'Priority: Zero vertical jerks & minimal braking shock',
    weights: {
      roadHazards: 0.40,   // 2x penalty on potholes & speed bumps
      hardBraking: 0.25,   // Heavy penalty on sudden emergency stops
      traffic: 0.15,
      accidentHistory: 0.10,
      speedPattern: 0.05,
      weather: 0.05,
    },
  },
  FAMILY_ELDERLY_CHILD: {
    label: 'Family with Elderly / Children',
    description: 'Priority: Night visibility, safe corridors, low highway speeds',
    weights: {
      accidentHistory: 0.25,
      roadHazards: 0.25,
      speedPattern: 0.20,
      weather: 0.15,
      traffic: 0.10,
      hardBraking: 0.05,
    },
  },
  AMBULANCE_EMERGENCY: {
    label: 'Emergency / Ambulance',
    description: 'Priority: Fastest transit time & bottleneck evasion',
    weights: {
      traffic: 0.40,       // 2x penalty on congestion bottlenecks
      speedPattern: 0.25,
      hardBraking: 0.15,
      roadHazards: 0.10,
      weather: 0.05,
      accidentHistory: 0.05,
    },
  },
});

export function getProfileWeights(profileKey = 'HEALTHY') {
  return PASSENGER_PROFILES[profileKey]?.weights || BASE_WEIGHTS;
}

export function calculateRisk(conditions, profileKey = 'HEALTHY') {
  const weights = getProfileWeights(profileKey);
  const sources = Object.keys(weights);
  const total = sources.reduce(
    (score, source) => score + (Number(conditions[source]) || 0) * (weights[source] || 0),
    0
  );
  const score = Math.round(Math.min(100, Math.max(0, total)));
  return {
    score,
    level: riskLevel(score),
    profile: PASSENGER_PROFILES[profileKey] || PASSENGER_PROFILES.HEALTHY,
    contributors: getTopContributors(conditions, weights),
  };
}

export function riskLevel(score) {
  if (score <= 30) return 'low';
  if (score <= 60) return 'medium';
  return 'high';
}

export function getTopContributors(conditions, weights = BASE_WEIGHTS) {
  return Object.entries(weights)
    .map(([name, weight]) => ({ name, impact: Math.round((Number(conditions[name]) || 0) * weight) }))
    .sort((a, b) => b.impact - a.impact)
    .slice(0, 2);
}
