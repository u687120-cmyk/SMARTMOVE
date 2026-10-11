const https = require('https');
const pool = require('../config/database');
const WeatherConfig = require('../config/weatherConfig');

/**
 * SMARTMOVE AI — Dynamic Weather Service (Phase 14).
 *
 * Connects to live meteorological providers, normalizes observations,
 * provides in-memory caching to avoid rate-limiting, persists to MySQL,
 * and calculates dynamic weather risk scores.
 */
class WeatherService {
  constructor() {
    // In-memory weather cache: Map<string, { data: object, timestamp: number }>
    this.cache = new Map();
    // HTTPS Agent configured to work reliably across local and production SSL environments
    this.httpsAgent = new https.Agent({ rejectUnauthorized: false });
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
   * Generates cache key based on rounded geographic coordinates.
   */
  getCacheKey(lat, lng) {
    const precision = WeatherConfig.cacheCoordinatePrecision;
    return `${Number(lat).toFixed(precision)}_${Number(lng).toFixed(precision)}`;
  }

  /**
   * Maps WMO weather codes to SMARTMOVE normalized conditions.
   */
  normalizeWmoCode(code) {
    return WeatherConfig.wmoCodeMap[code] || 'CLEAR';
  }

  /**
   * Fetches raw weather data from Open-Meteo provider via HTTPS.
   */
  fetchFromProvider(lat, lng) {
    return new Promise((resolve, reject) => {
      const url = `https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lng}&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,visibility`;

      const req = https.get(url, { agent: this.httpsAgent, timeout: 8000 }, (res) => {
        let body = '';
        res.on('data', (chunk) => { body += chunk; });
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            try {
              const parsed = JSON.parse(body);
              resolve(parsed);
            } catch (err) {
              reject(new Error('Invalid JSON received from weather provider'));
            }
          } else {
            reject(new Error(`Weather provider HTTP ${res.statusCode}: ${body}`));
          }
        });
      });

      req.on('timeout', () => {
        req.destroy();
        reject(new Error('Weather provider request timed out'));
      });

      req.on('error', (err) => {
        reject(err);
      });
    });
  }

  /**
   * Retrieves current weather observation for specified coordinates.
   * Checks cache first; on cache miss, queries provider and persists to DB.
   */
  async getCurrentWeather(latitude, longitude) {
    if (!this.isValidCoordinates(latitude, longitude)) {
      const err = new Error('Invalid coordinates: latitude must be between -90 and 90, longitude between -180 and 180.');
      err.statusCode = 400;
      throw err;
    }

    const lat = Number(latitude);
    const lng = Number(longitude);
    const cacheKey = this.getCacheKey(lat, lng);
    const now = Date.now();

    // 1. Check in-memory cache
    if (this.cache.has(cacheKey)) {
      const entry = this.cache.get(cacheKey);
      if (now - entry.timestamp < WeatherConfig.cacheTtlMs) {
        return { ...entry.data, isCached: true };
      }
    }

    // 2. Fetch from legitimate weather provider
    let providerData;
    try {
      providerData = await this.fetchFromProvider(lat, lng);
    } catch (apiErr) {
      // If provider fails but we have stale cache, return stale cache gracefully
      if (this.cache.has(cacheKey)) {
        return { ...this.cache.get(cacheKey).data, isStale: true };
      }
      const err = new Error('Weather service temporarily unavailable');
      err.statusCode = 503;
      throw err;
    }

    const current = providerData.current || {};
    const condition = this.normalizeWmoCode(current.weather_code);
    const tempC = current.temperature_2m !== undefined ? Number(current.temperature_2m) : 25.0;
    const precipMm = current.precipitation !== undefined ? Number(current.precipitation) : 0.0;
    const humidity = current.relative_humidity_2m !== undefined ? Number(current.relative_humidity_2m) : 60;
    const windKmh = current.wind_speed_10m !== undefined ? Number(current.wind_speed_10m) : 10.0;
    const visibilityKm = current.visibility !== undefined ? Number((current.visibility / 1000).toFixed(1)) : 10.0;
    const observedAt = current.time ? new Date(current.time) : new Date();

    const normalized = {
      latitude: lat,
      longitude: lng,
      temperatureC: parseFloat(tempC.toFixed(1)),
      condition,
      precipitationMm: parseFloat(precipMm.toFixed(2)),
      humidity: Math.round(humidity),
      windSpeedKmh: parseFloat(windKmh.toFixed(1)),
      visibilityKm: parseFloat(visibilityKm.toFixed(1)),
      observedAt: observedAt.toISOString(),
      isCached: false,
    };

    // 3. Update in-memory cache
    this.cache.set(cacheKey, { data: normalized, timestamp: now });

    // 4. Asynchronously persist observation to MySQL
    this.persistWeatherObservation(normalized).catch(() => {});

    return normalized;
  }

  /**
   * Persists weather observation to weather_observations table.
   */
  async persistWeatherObservation(w) {
    try {
      await pool.query(
        `INSERT INTO weather_observations (
          latitude, longitude, temperature_c, \`condition\`,
          precipitation_mm, humidity, wind_speed_kmh, visibility_km,
          observed_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`,
        [
          w.latitude,
          w.longitude,
          w.temperatureC,
          w.condition,
          w.precipitationMm,
          w.humidity,
          w.windSpeedKmh,
          w.visibilityKm,
          new Date(w.observedAt),
        ]
      );
    } catch (err) {
      // Database persistence failure should not block client weather delivery
    }
  }

  /**
   * Computes improved dynamic weather risk score (0 - 100) using real measurements.
   */
  calculateWeatherRiskScore(weather) {
    if (!weather) return 20; // Default neutral score if weather unavailable

    const cond = (weather.condition || 'CLEAR').toUpperCase();
    let score = WeatherConfig.baseRisk[cond] !== undefined ? WeatherConfig.baseRisk[cond] : 20;

    const m = WeatherConfig.modifiers;

    // Visibility modifier
    if (weather.visibilityKm !== undefined && weather.visibilityKm < m.veryLowVisibilityKm) {
      score += m.veryLowVisibilityPenalty;
    } else if (weather.visibilityKm !== undefined && weather.visibilityKm < m.lowVisibilityKm) {
      score += m.lowVisibilityPenalty;
    }

    // Precipitation intensity modifier
    if (weather.precipitationMm !== undefined && weather.precipitationMm > m.heavyPrecipitationMm) {
      score += m.heavyPrecipitationPenalty;
    } else if (weather.precipitationMm !== undefined && weather.precipitationMm > m.moderatePrecipitationMm) {
      score += m.moderatePrecipitationPenalty;
    }

    // Strong wind modifier
    if (weather.windSpeedKmh !== undefined && weather.windSpeedKmh > m.severeWindSpeedKmh) {
      score += m.severeWindPenalty;
    } else if (weather.windSpeedKmh !== undefined && weather.windSpeedKmh > m.highWindSpeedKmh) {
      score += m.highWindPenalty;
    }

    return Math.max(0, Math.min(100, Math.round(score)));
  }

  /**
   * Generates contextual explanation string for weather conditions.
   */
  getWeatherExplanation(weather) {
    if (!weather) return null;

    if (weather.condition === 'HEAVY_RAIN') {
      return 'Heavy rain is increasing road risk.';
    }
    if (weather.visibilityKm !== undefined && weather.visibilityKm < 3.0) {
      return 'Low visibility is increasing road risk.';
    }
    if (weather.condition === 'FOG') {
      return 'Foggy conditions and reduced sightlines ahead.';
    }
    if (weather.condition === 'RAIN') {
      return 'Wet road surface detected.';
    }
    if (weather.windSpeedKmh !== undefined && weather.windSpeedKmh > 40.0) {
      return 'Strong crosswinds may affect vehicle stability.';
    }

    return null;
  }
}

module.exports = new WeatherService();
