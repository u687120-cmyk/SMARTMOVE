const pool = require('../config/database');

/**
 * Hazard Service.
 * Manages community-reported road obstacles, deterministic confidence
 * scoring algorithms, and dynamic lifecycle state transitions:
 * ACTIVE -> UNDER_REVIEW -> RESOLVED.
 */
class HazardService {
  async getActiveHazards() {
    const [rows] = await pool.query(
      `SELECT
        id, reported_by_user_id, type,
        latitude, longitude, location_name,
        severity, description, status, confidence,
        reported_at, last_confirmed_at, resolved_at,
        created_at
       FROM hazards
       WHERE status = 'ACTIVE' OR status = 'UNDER_REVIEW'
       ORDER BY
        CASE severity
          WHEN 'HIGH' THEN 1
          WHEN 'MEDIUM' THEN 2
          WHEN 'LOW' THEN 3
          ELSE 4
        END ASC,
        confidence DESC,
        last_confirmed_at DESC`
    );

    return rows.map((h) => this.formatHazard(h));
  }

  async getMyReports(userId) {
    const [rows] = await pool.query(
      `SELECT * FROM hazards
       WHERE reported_by_user_id = ?
       ORDER BY created_at DESC`,
      [userId]
    );

    return rows.map((h) => this.formatHazard(h));
  }

  async getHazardById(id) {
    const [rows] = await pool.query(
      `SELECT * FROM hazards WHERE id = ? LIMIT 1`,
      [id]
    );

    if (rows.length === 0) {
      const err = new Error('Hazard not found.');
      err.statusCode = 404;
      throw err;
    }

    return this.formatHazard(rows[0]);
  }

  async reportHazard(userId, { type, latitude, longitude, locationName, severity, description }) {
    const allowedTypes = ['POTHOLE', 'ACCIDENT', 'ROAD_BLOCKAGE', 'CONSTRUCTION', 'HEAVY_TRAFFIC', 'OTHER'];
    const allowedSeverities = ['LOW', 'MEDIUM', 'HIGH'];

    const typeClean = (type || 'OTHER').toUpperCase().trim();
    const severityClean = (severity || 'MEDIUM').toUpperCase().trim();

    if (!allowedTypes.includes(typeClean)) {
      const err = new Error(`Invalid hazard type. Allowed: ${allowedTypes.join(', ')}`);
      err.statusCode = 400;
      throw err;
    }

    if (!allowedSeverities.includes(severityClean)) {
      const err = new Error(`Invalid severity. Allowed: ${allowedSeverities.join(', ')}`);
      err.statusCode = 400;
      throw err;
    }

    const lat = parseFloat(latitude) || 18.5204;
    const lng = parseFloat(longitude) || 73.8567;
    const loc = locationName ? locationName.trim() : 'Pune - Hinjewadi Road';
    const desc = description ? description.trim() : null;

    const [result] = await pool.query(
      `INSERT INTO hazards (
        reported_by_user_id, type,
        latitude, longitude, location_name,
        severity, description, status, confidence,
        reported_at, last_confirmed_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 60, NOW(), NOW())`,
      [userId, typeClean, lat, lng, loc, severityClean, desc]
    );

    // Automatically record original reporter's confirmation
    await pool.query(
      `INSERT INTO hazard_confirmations (hazard_id, user_id, confirmation_type)
       VALUES (?, ?, 'STILL_PRESENT')
       ON DUPLICATE KEY UPDATE confirmation_type = 'STILL_PRESENT'`,
      [result.insertId, userId]
    );

    return this.getHazardById(result.insertId);
  }

  async confirmHazard(userId, hazardId, confirmationType = 'STILL_PRESENT') {
    const isFixed = confirmationType.toUpperCase() === 'FIXED';
    const confType = isFixed ? 'FIXED' : 'STILL_PRESENT';

    // Verify hazard exists
    const current = await this.getHazardById(hazardId);

    // Upsert confirmation vote (enforces 1 vote per user per hazard)
    await pool.query(
      `INSERT INTO hazard_confirmations (hazard_id, user_id, confirmation_type)
       VALUES (?, ?, ?)
       ON DUPLICATE KEY UPDATE confirmation_type = VALUES(confirmation_type)`,
      [hazardId, userId, confType]
    );

    // Count how many users reported FIXED
    const [fixedCountRows] = await pool.query(
      `SELECT COUNT(*) AS fixed_count FROM hazard_confirmations
       WHERE hazard_id = ? AND confirmation_type = 'FIXED'`,
      [hazardId]
    );
    const fixedVotes = Number(fixedCountRows[0].fixed_count);

    // Algorithmic confidence adjustment
    let newConfidence;
    if (isFixed) {
      newConfidence = Math.max(0, current.confidence - 20);
    } else {
      newConfidence = Math.min(100, current.confidence + 10);
    }

    // Lifecycle state evaluation
    let newStatus;
    let resolvedAt = current.resolvedAt;

    if (fixedVotes >= 2 || newConfidence <= 20) {
      newStatus = 'RESOLVED';
      resolvedAt = new Date();
    } else if (newConfidence < 50) {
      newStatus = 'UNDER_REVIEW';
    } else {
      newStatus = 'ACTIVE';
    }

    await pool.query(
      `UPDATE hazards
       SET confidence = ?, status = ?, last_confirmed_at = NOW(), resolved_at = ?
       WHERE id = ?`,
      [newConfidence, newStatus, resolvedAt, hazardId]
    );

    return this.getHazardById(hazardId);
  }

  formatHazard(h) {
    return {
      id: h.id.toString(),
      reportedByUserId: h.reported_by_user_id,
      type: h.type,
      latitude: parseFloat(h.latitude),
      longitude: parseFloat(h.longitude),
      locationName: h.location_name,
      severity: h.severity,
      description: h.description,
      status: h.status,
      confidence: h.confidence,
      reportedAt: h.reported_at,
      lastConfirmedAt: h.last_confirmed_at,
      resolvedAt: h.resolved_at,
      createdAt: h.created_at,
    };
  }
}

module.exports = new HazardService();
