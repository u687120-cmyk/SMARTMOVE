const pool = require('../config/database');

/**
 * Trip Service.
 * Manages journey logging, history retrieval with date filtering,
 * and ownership-isolated trip inspections.
 */
class TripService {
  async createTrip(userId, tripData) {
    const {
      vehicleId,
      startLatitude,
      startLongitude,
      startLocation,
      destinationLatitude,
      destinationLongitude,
      destinationLocation,
      startedAt,
      endedAt,
      distanceKm = 0,
      durationMinutes = 0,
      averageRisk = 0,
      safetyScore = 100,
      warningCount = 0,
      hazardCount = 0,
      hardBrakingCount = 0,
      trafficCondition = 'Moderate',
      weatherCondition = 'Clear',
    } = tripData;

    // Range validation
    const dist = parseFloat(distanceKm);
    const dur = parseInt(durationMinutes, 10);
    const risk = parseInt(averageRisk, 10);
    const score = parseInt(safetyScore, 10);

    if (isNaN(dist) || dist < 0) {
      const err = new Error('Distance must be a non-negative number.');
      err.statusCode = 400;
      throw err;
    }
    if (isNaN(dur) || dur < 0) {
      const err = new Error('Duration must be a non-negative integer.');
      err.statusCode = 400;
      throw err;
    }
    if (isNaN(risk) || risk < 0 || risk > 100) {
      const err = new Error('Average risk must be an integer between 0 and 100.');
      err.statusCode = 400;
      throw err;
    }
    if (isNaN(score) || score < 0 || score > 100) {
      const err = new Error('Safety score must be an integer between 0 and 100.');
      err.statusCode = 400;
      throw err;
    }

    const startLat = parseFloat(startLatitude) || 18.5204;
    const startLng = parseFloat(startLongitude) || 73.8567;
    const destLat = parseFloat(destinationLatitude) || 18.5912;
    const destLng = parseFloat(destinationLongitude) || 73.7389;

    const startLoc = (startLocation && startLocation.trim()) || 'Current Location';
    const destLoc = (destinationLocation && destinationLocation.trim()) || 'Destination';

    const startTime = startedAt ? new Date(startedAt) : new Date();
    const endTime = endedAt ? new Date(endedAt) : new Date();

    const [result] = await pool.query(
      `INSERT INTO trips (
        user_id, vehicle_id,
        start_latitude, start_longitude, start_location,
        destination_latitude, destination_longitude, destination_location,
        started_at, ended_at,
        distance_km, duration_minutes,
        average_risk, safety_score,
        warning_count, hazard_count, hard_braking_count,
        traffic_condition, weather_condition
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        userId,
        vehicleId || null,
        startLat,
        startLng,
        startLoc,
        destLat,
        destLng,
        destLoc,
        startTime,
        endTime,
        dist,
        dur,
        risk,
        score,
        parseInt(warningCount, 10) || 0,
        parseInt(hazardCount, 10) || 0,
        parseInt(hardBrakingCount, 10) || 0,
        trafficCondition,
        weatherCondition,
      ]
    );

    return this.getTripById(userId, result.insertId);
  }

  async getTrips(userId, period = 'all') {
    let dateFilterSql = '';
    const params = [userId];

    const p = (period || 'all').toLowerCase();
    if (p === 'today') {
      dateFilterSql = 'AND started_at >= CURDATE()';
    } else if (p === 'week') {
      dateFilterSql = 'AND started_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)';
    } else if (p === 'month') {
      dateFilterSql = 'AND started_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)';
    } else if (p === 'year') {
      dateFilterSql = 'AND started_at >= DATE_SUB(NOW(), INTERVAL 365 DAY)';
    }

    const [rows] = await pool.query(
      `SELECT
        id, user_id, vehicle_id,
        start_latitude, start_longitude, start_location,
        destination_latitude, destination_longitude, destination_location,
        started_at, ended_at,
        distance_km, duration_minutes,
        average_risk, safety_score,
        warning_count, hazard_count, hard_braking_count,
        traffic_condition, weather_condition,
        created_at
       FROM trips
       WHERE user_id = ? ${dateFilterSql}
       ORDER BY started_at DESC`,
      params
    );

    return rows.map((t) => this.formatTrip(t));
  }

  async getTripById(userId, tripId) {
    const [rows] = await pool.query(
      `SELECT * FROM trips WHERE id = ? AND user_id = ? LIMIT 1`,
      [tripId, userId]
    );

    if (rows.length === 0) {
      const err = new Error('Trip not found.');
      err.statusCode = 404;
      throw err;
    }

    const trip = this.formatTrip(rows[0]);

    // Query granular risk events for this trip if available
    try {
      const [events] = await pool.query(
        `SELECT id, event_type, risk_score, latitude, longitude, recorded_at, metadata_json
         FROM risk_events
         WHERE trip_id = ? AND user_id = ?
         ORDER BY recorded_at ASC`,
        [tripId, userId]
      );
      trip.riskEvents = events.map((e) => ({
        id: e.id,
        eventType: e.event_type,
        riskScore: e.risk_score,
        latitude: parseFloat(e.latitude),
        longitude: parseFloat(e.longitude),
        recordedAt: e.recorded_at,
        metadata: typeof e.metadata_json === 'string' ? JSON.parse(e.metadata_json) : e.metadata_json,
      }));
      trip.reroutesCount = trip.riskEvents.filter(
        (e) => e.metadata && e.metadata.action === 'ROUTE_SWITCH'
      ).length;
      trip.safetyEventsCount = trip.riskEvents.length;
    } catch (e) {
      trip.riskEvents = [];
      trip.reroutesCount = 0;
      trip.safetyEventsCount = (trip.warningCount || 0) + (trip.hardBrakingCount || 0);
    }

    // Generate individual trip safety suggestion based on actual performance (Phase 18)
    if (trip.hardBrakingCount > 2) {
      trip.suggestion = "Try maintaining a greater following distance to reduce sudden braking.";
    } else if (trip.averageRisk >= 60) {
      trip.suggestion = "Your trip showed elevated road risk. Consider alternative bypass routes during peak hours.";
    } else if (trip.safetyScore >= 90) {
      trip.suggestion = "Excellent safety performance on this journey!";
    } else {
      trip.suggestion = "Smooth driving throughout this corridor.";
    }

    return trip;
  }

  formatTrip(t) {
    return {
      id: t.id,
      userId: t.user_id,
      vehicleId: t.vehicle_id,
      startLatitude: parseFloat(t.start_latitude),
      startLongitude: parseFloat(t.start_longitude),
      startLocation: t.start_location,
      destinationLatitude: parseFloat(t.destination_latitude),
      destinationLongitude: parseFloat(t.destination_longitude),
      destinationLocation: t.destination_location,
      startedAt: t.started_at,
      endedAt: t.ended_at,
      distanceKm: parseFloat(t.distance_km),
      durationMinutes: t.duration_minutes,
      averageRisk: t.average_risk,
      safetyScore: t.safety_score,
      warningCount: t.warning_count,
      hazardCount: t.hazard_count,
      hardBrakingCount: t.hard_braking_count,
      trafficCondition: t.traffic_condition,
      weatherCondition: t.weather_condition,
      createdAt: t.created_at,
    };
  }
}

module.exports = new TripService();
