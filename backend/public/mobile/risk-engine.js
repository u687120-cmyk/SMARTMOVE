/**
 * SmartMove AI — Dynamic Risk Engine with Passenger Sensitivity & Civic Weighting
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
      roadHazards: 0.45,   // 2.25x weight on potholes & speed breakers
      hardBraking: 0.30,   // High penalty on aggressive stops
      traffic: 0.10,
      weather: 0.05,
      accidentHistory: 0.05,
      speedPattern: 0.05,
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
      traffic: 0.45,       // 2.25x penalty on congestion
      speedPattern: 0.25,
      roadHazards: 0.10,
      hardBraking: 0.10,
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
