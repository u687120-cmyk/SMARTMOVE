import { calculateRisk, PASSENGER_PROFILES } from './risk-engine.js';

const API_BASE = 'http://localhost:5000/api';

/** Helper fetch with graceful fallback */
async function apiFetch(endpoint, options = {}) {
  try {
    const res = await fetch(`${API_BASE}${endpoint}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return await res.json();
  } catch (err) {
    console.warn(`[SmartMove API fallback: ${endpoint}]`, err.message);
    return null;
  }
}

export const weatherService = {
  async current(latitude = 18.5204, longitude = 73.8567) {
    const data = await apiFetch(`/weather/current?lat=${latitude}&lon=${longitude}`);
    if (data && data.success && data.data) {
      return data.data;
    }
    return {
      latitude,
      longitude,
      temperature: 25,
      condition: 'Clear',
      precipitation: 0,
      humidity: 58,
      wind: 9,
      visibility: 10,
      timestamp: new Date().toISOString(),
    };
  },
};

export const trafficService = {
  async routeConditions() {
    return {
      safest: { traffic: 18, hardBraking: 10, weather: 10, roadHazards: 12, accidentHistory: 15, speedPattern: 12 },
      fastest: { traffic: 62, hardBraking: 48, weather: 10, roadHazards: 35, accidentHistory: 30, speedPattern: 52 },
      balanced: { traffic: 32, hardBraking: 24, weather: 10, roadHazards: 20, accidentHistory: 20, speedPattern: 24 },
    };
  },
};

export const liveRouteService = {
  async getRecommendations(origin, destination, passengerProfile = 'HEALTHY') {
    const payload = {
      origin: {
        latitude: origin?.latitude || 18.5204,
        longitude: origin?.longitude || 73.8567,
      },
      destination: {
        latitude: destination?.latitude || 18.5314,
        longitude: destination?.longitude || 73.8446,
      },
      passengerProfile,
    };

    const res = await apiFetch('/routes/recommend', {
      method: 'POST',
      body: JSON.stringify(payload),
    });

    if (res && res.success && res.data) {
      return res.data;
    }

    return null;
  },
};

export const hazardService = {
  async report({ type, latitude = null, longitude = null }) {
    const payload = {
      type: type || 'POTHOLE',
      latitude: latitude || 18.5204,
      longitude: longitude || 73.8567,
      severity: 'HIGH',
      locationName: 'Reported by Driver on Route',
    };
    const res = await apiFetch('/hazards', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    if (res && res.success) return res.data;

    return {
      hazard_id: `hz_${Date.now()}`,
      type,
      latitude,
      longitude,
      severity: 'HIGH',
      reported_at: new Date().toISOString(),
      last_confirmed_at: new Date().toISOString(),
      confidence: 60,
      status: 'ACTIVE',
    };
  },

  async confirm(hazardId, response) {
    const res = await apiFetch(`/hazards/${hazardId}/confirm`, {
      method: 'POST',
      body: JSON.stringify({ confirmationType: response === 'fixed' ? 'FIXED' : 'STILL_PRESENT' }),
    });
    if (res && res.success) return res.data;

    return {
      hazardId,
      response,
      status: response === 'fixed' ? 'UNDER_REVIEW' : 'ACTIVE',
      confidence: response === 'fixed' ? 40 : 75,
    };
  },
};

export const civicGrievanceService = {
  async sendSensorSpike(eventData) {
    return await apiFetch('/complaints/sensor-event', {
      method: 'POST',
      body: JSON.stringify(eventData),
    });
  },

  async sendCleanPass(passData) {
    return await apiFetch('/complaints/clean-pass', {
      method: 'POST',
      body: JSON.stringify(passData),
    });
  },

  async simulateDemoSpike(lat = 18.5204, lng = 73.8567, deviceId) {
    return await apiFetch('/complaints/simulate-spike', {
      method: 'POST',
      body: JSON.stringify({ lat, lng, deviceId }),
    });
  },

  async fastForward7Days() {
    return await apiFetch('/complaints/fast-forward-days', {
      method: 'POST',
      body: JSON.stringify({ days: 7 }),
    });
  },

  async getDashboard() {
    const res = await apiFetch('/complaints/public-dashboard');
    if (res && res.success) return res.data;

    return {
      metrics: {
        totalHazardsDetected: 14,
        activeHazards: 8,
        resolvedHazards: 6,
        complaintsDispatched: 5,
        remindersIssued: 2,
        escalationsIssued: 1,
        complaintsResolved: 4,
        totalPassiveReadings: 42,
        totalCleanPasses: 28,
        accountabilityRate: '100%',
      },
      recentComplaints: [],
      recentTimeline: [],
    };
  },
};

export async function getRouteRisk(routeId, profileKey = 'HEALTHY') {
  const conditions = await trafficService.routeConditions();
  return calculateRisk(conditions[routeId] || conditions.safest, profileKey);
}
