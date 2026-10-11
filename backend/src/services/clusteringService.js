const pool = require('../config/database');
const crypto = require('crypto');
const complaintService = require('./complaintService');
const authorityService = require('./authorityService');

// Haversine Distance in meters
function getDistanceMeters(lat1, lon1, lat2, lon2) {
  const R = 6371e3; // Earth radius in meters
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

class ClusteringService {
  /**
   * Process incoming passive accelerometer spike with distinct device filtering
   */
  async processSensorEvent({
    lat,
    lng,
    magnitude,
    speed,
    deviceId,
    locationName = 'Pune Corridor',
    isSimulated = false,
  }) {
    if (!lat || !lng || !deviceId) {
      throw new Error('Missing required sensor parameters (lat, lng, deviceId).');
    }

    const latitude = parseFloat(lat);
    const longitude = parseFloat(lng);
    const jerk = parseFloat(magnitude) || 12.0;
    const vehicleSpeed = parseFloat(speed) || 20.0;
    const deviceFingerprint = String(deviceId).trim();
    const simulatedFlag = isSimulated ? 1 : 0;

    // 1. Query spatial bounding box (~30m radius)
    const delta = 0.0003;
    const [nearbyHazards] = await pool.query(
      `SELECT * FROM hazards
       WHERE latitude BETWEEN ? AND ?
         AND longitude BETWEEN ? AND ?`,
      [latitude - delta, latitude + delta, longitude - delta, longitude + delta]
    );

    // Exact 20 meter distance match
    const match = nearbyHazards.find(
      (h) => getDistanceMeters(h.latitude, h.longitude, latitude, longitude) <= 20
    );

    let hazardId;
    let isNewHazard = false;
    let wasReopened = false;

    if (match) {
      hazardId = match.id;
      // Reopen logic: if road was RESOLVED, but a new high-G spike occurs -> REOPEN!
      if (match.status === 'RESOLVED') {
        await pool.query(
          `UPDATE hazards
           SET status = 'REOPENED', clean_pass_count = 0, last_confirmed_at = NOW()
           WHERE id = ?`,
          [hazardId]
        );
        wasReopened = true;
      }
    } else {
      // Determine authority and road type for new hazard
      const { roadType, authority } = await authorityService.resolveAuthority(latitude, longitude, locationName);

      const [insertRes] = await pool.query(
        `INSERT INTO hazards (
          type, latitude, longitude, location_name, severity,
          description, status, confidence, road_type, authority_target,
          is_simulated, reported_at, last_confirmed_at
        ) VALUES ('POTHOLE', ?, ?, ?, 'HIGH', 'Detected via vehicle accelerometer shock', 'ACTIVE', 30, ?, ?, ?, NOW(), NOW())`,
        [latitude, longitude, locationName, roadType, authority.name, simulatedFlag]
      );
      hazardId = insertRes.insertId;
      isNewHazard = true;
    }

    // 2. Distinct device check: ek device ko ek hazard par sirf ek baar count karo!
    const [seenRows] = await pool.query(
      `SELECT 1 FROM sensor_events WHERE hazard_id = ? AND device_fingerprint = ? LIMIT 1`,
      [hazardId, deviceFingerprint]
    );

    if (seenRows.length > 0) {
      return {
        hazardId,
        alreadyLoggedByThisDevice: true,
        wasReopened,
        message: 'Spike acknowledged, but device already contributed to this hazard cluster.',
      };
    }

    // 3. Insert unique sensor reading
    const sensorEventId = crypto.randomUUID();
    await pool.query(
      `INSERT INTO sensor_events (id, hazard_id, device_fingerprint, magnitude, speed, latitude, longitude, is_simulated, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())`,
      [sensorEventId, hazardId, deviceFingerprint, jerk, vehicleSpeed, latitude, longitude, simulatedFlag]
    );

    // 4. Calculate distinct devices count (real crowd confidence)
    const [[countRow]] = await pool.query(
      `SELECT COUNT(DISTINCT device_fingerprint) AS distinct_devices FROM sensor_events WHERE hazard_id = ?`,
      [hazardId]
    );
    const distinctCount = Number(countRow.distinct_devices || 1);

    // Map distinct count to 0-100 confidence scale (1 device: 40%, 2 devices: 65%, 3+ devices: 95%)
    const confidenceScore = Math.min(100, distinctCount >= 3 ? 95 : distinctCount === 2 ? 65 : 40);

    await pool.query(
      `UPDATE hazards SET confidence = ?, last_confirmed_at = NOW() WHERE id = ?`,
      [confidenceScore, hazardId]
    );

    // 5. Automatic Verification & Auto-Complaint Trigger (strictly 1 complaint per hazard)
    let autoComplaintResult = null;
    if (distinctCount >= 3) {
      const [complaintExists] = await pool.query(
        `SELECT 1 FROM complaints WHERE hazard_id = ? LIMIT 1`,
        [hazardId]
      );

      if (complaintExists.length === 0) {
        autoComplaintResult = await complaintService.triggerAutoComplaint(hazardId);
      }
    }

    return {
      hazardId,
      isNewHazard,
      wasReopened,
      distinctDevicesConfirmed: distinctCount,
      confidenceScore,
      isVerified: distinctCount >= 3,
      autoComplaintTriggered: !!autoComplaintResult,
      complaintDetails: autoComplaintResult,
      isSimulated: !!simulatedFlag,
    };
  }

  /**
   * Record clean vehicle pass over a known hazard (zero shock pattern)
   * If 5 distinct vehicles pass cleanly -> automatically mark RESOLVED!
   */
  async recordCleanPass({ hazardId, deviceId, speed = 25.0 }) {
    if (!hazardId || !deviceId) {
      throw new Error('hazardId and deviceId are required for clean pass tracking.');
    }

    const deviceFingerprint = String(deviceId).trim();

    // Check if hazard exists
    const [hazards] = await pool.query(`SELECT * FROM hazards WHERE id = ?`, [hazardId]);
    if (hazards.length === 0) throw new Error('Hazard not found.');
    const hazard = hazards[0];

    if (hazard.status === 'RESOLVED') {
      return { hazardId, status: 'RESOLVED', message: 'Hazard already resolved.' };
    }

    // Check if this device already logged a clean pass for this hazard
    const [alreadyPassed] = await pool.query(
      `SELECT 1 FROM clean_passes WHERE hazard_id = ? AND device_fingerprint = ? LIMIT 1`,
      [hazardId, deviceFingerprint]
    );

    if (alreadyPassed.length > 0) {
      return {
        hazardId,
        cleanPassCount: hazard.clean_pass_count,
        message: 'Clean pass already logged by this vehicle.',
      };
    }

    // Insert clean pass
    await pool.query(
      `INSERT INTO clean_passes (id, hazard_id, device_fingerprint, speed, recorded_at)
       VALUES (?, ?, ?, ?, NOW())`,
      [crypto.randomUUID(), hazardId, deviceFingerprint, speed]
    );

    // Count distinct clean passes
    const [[passRow]] = await pool.query(
      `SELECT COUNT(DISTINCT device_fingerprint) AS distinct_passes FROM clean_passes WHERE hazard_id = ?`,
      [hazardId]
    );
    const cleanPassCount = Number(passRow.distinct_passes || 1);

    await pool.query(
      `UPDATE hazards SET clean_pass_count = ? WHERE id = ?`,
      [cleanPassCount, hazardId]
    );

    // Auto-resolve when 5 distinct vehicles pass without jerk!
    let autoResolved = false;
    if (cleanPassCount >= 5) {
      await pool.query(
        `UPDATE hazards SET status = 'RESOLVED', resolved_at = NOW() WHERE id = ?`,
        [hazardId]
      );
      // Update any associated complaint
      await pool.query(
        `UPDATE complaints SET status = 'RESOLVED' WHERE hazard_id = ?`,
        [hazardId]
      );
      autoResolved = true;
    }

    return {
      hazardId,
      cleanPassCount,
      autoResolved,
      status: autoResolved ? 'RESOLVED' : hazard.status,
      message: autoResolved
        ? '5 independent vehicles passed smoothly. Road confirmed repaired! Grievance closed.'
        : `Clean pass recorded (${cleanPassCount}/5 passes towards auto-resolution).`,
    };
  }
}

module.exports = new ClusteringService();
