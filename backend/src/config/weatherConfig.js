/**
 * SMARTMOVE AI — Weather Engine Configuration (Phase 14)
 *
 * Centralized mapping of meteorological conditions, risk scores,
 * modifiers, and caching thresholds.
 */

const WeatherConfig = {
  // Base risk contribution for normalized weather conditions (0 - 100)
  baseRisk: {
    CLEAR: 10,
    CLOUDY: 20,
    RAIN: 55,
    HEAVY_RAIN: 80,
    FOG: 75,
  },

  // Modifiers applied when real-time sensor/provider data is available
  modifiers: {
    // Visibility degradation (km)
    veryLowVisibilityKm: 1.0, // < 1 km
    veryLowVisibilityPenalty: 20,
    lowVisibilityKm: 3.0,     // < 3 km
    lowVisibilityPenalty: 10,

    // Intense precipitation (mm)
    heavyPrecipitationMm: 15.0, // > 15 mm
    heavyPrecipitationPenalty: 20,
    moderatePrecipitationMm: 5.0, // > 5 mm
    moderatePrecipitationPenalty: 10,

    // Strong wind conditions (km/h)
    severeWindSpeedKmh: 45.0, // > 45 km/h
    severeWindPenalty: 15,
    highWindSpeedKmh: 30.0,   // > 30 km/h
    highWindPenalty: 5,
  },

  // In-memory cache time-to-live: 10 minutes (in milliseconds)
  cacheTtlMs: 10 * 60 * 1000,

  // Coordinate rounding precision for cache keys (~1.1 km grid)
  cacheCoordinatePrecision: 2,

  // Fallback geographic coordinates if client location unavailable (Pune, India)
  defaultLocation: {
    latitude: 18.5204,
    longitude: 73.8567,
    city: 'Pune',
  },

  // Mapping WMO Weather Interpretation Codes (used by Open-Meteo & standard meteorology)
  wmoCodeMap: {
    0: 'CLEAR',      // Clear sky
    1: 'CLEAR',      // Mainly clear
    2: 'CLOUDY',     // Partly cloudy
    3: 'CLOUDY',     // Overcast
    45: 'FOG',       // Fog
    48: 'FOG',       // Depositing rime fog
    51: 'RAIN',      // Light drizzle
    53: 'RAIN',      // Moderate drizzle
    55: 'RAIN',      // Dense drizzle
    61: 'RAIN',      // Slight rain
    63: 'RAIN',      // Moderate rain
    65: 'HEAVY_RAIN',// Heavy rain
    66: 'RAIN',      // Light freezing rain
    67: 'HEAVY_RAIN',// Heavy freezing rain
    71: 'RAIN',      // Slight snow fall
    73: 'RAIN',      // Moderate snow fall
    75: 'HEAVY_RAIN',// Heavy snow fall
    77: 'RAIN',      // Snow grains
    80: 'RAIN',      // Slight rain showers
    81: 'RAIN',      // Moderate rain showers
    82: 'HEAVY_RAIN',// Violent rain showers
    85: 'RAIN',      // Slight snow showers
    86: 'HEAVY_RAIN',// Heavy snow showers
    95: 'HEAVY_RAIN',// Thunderstorm
    96: 'HEAVY_RAIN',// Thunderstorm with slight hail
    99: 'HEAVY_RAIN',// Thunderstorm with heavy hail
  },

  // Descriptions for normalized conditions
  conditionDisplay: {
    CLEAR: { label: 'Clear', icon: '☀️' },
    CLOUDY: { label: 'Cloudy', icon: '⛅' },
    RAIN: { label: 'Rain', icon: '🌧️' },
    HEAVY_RAIN: { label: 'Heavy Rain', icon: '⛈️' },
    FOG: { label: 'Fog', icon: '🌫️' },
  },
};

module.exports = WeatherConfig;
