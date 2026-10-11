const pool = require('../config/database');

/**
 * Analytics Service.
 * Calculates dynamic safety metrics and driving aggregates
 * directly from completed trip records using SQL aggregation.
 */
class AnalyticsService {
  async getPeriodAnalytics(userId, period = 'today') {
    const p = (period || 'today').toLowerCase();

    let dateFilterSql = '';
    if (p === 'today') {
      dateFilterSql = 'AND started_at >= CURDATE()';
    } else if (p === 'week') {
      dateFilterSql = 'AND started_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)';
    } else if (p === 'month') {
      dateFilterSql = 'AND started_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)';
    } else if (p === 'year') {
      dateFilterSql = 'AND started_at >= DATE_SUB(NOW(), INTERVAL 365 DAY)';
    }

    const [aggregates] = await pool.query(
      `SELECT
        COUNT(*) AS total_trips,
        COALESCE(SUM(distance_km), 0) AS total_distance,
        COALESCE(AVG(average_risk), 0) AS avg_risk,
        COALESCE(AVG(safety_score), 100) AS avg_safety,
        COALESCE(SUM(warning_count), 0) AS total_warnings,
        COALESCE(SUM(hazard_count), 0) AS total_hazards,
        COALESCE(SUM(hard_braking_count), 0) AS total_hard_braking,
        SUM(CASE WHEN average_risk < 35 THEN 1 ELSE 0 END) AS low_risk_trips,
        SUM(CASE WHEN average_risk >= 35 AND average_risk <= 65 THEN 1 ELSE 0 END) AS medium_risk_trips,
        SUM(CASE WHEN average_risk > 65 THEN 1 ELSE 0 END) AS high_risk_trips
       FROM trips
       WHERE user_id = ? ${dateFilterSql}`,
      [userId]
    );

    const row = aggregates[0];
    const totalTrips = Number(row.total_trips);
    const avgRisk = Math.round(Number(row.avg_risk));
    const avgSafety = Math.round(Number(row.avg_safety));
    const hardBraking = Number(row.total_hard_braking);
    const hazards = Number(row.total_hazards);
    const warnings = Number(row.total_warnings);

    // Fetch rerouting events for this user in this period from risk_events
    let reroutesCount = 0;
    try {
      const dateFilterRiskSql = dateFilterSql.replace(/started_at/g, 'recorded_at');
      const [rerouteRows] = await pool.query(
        `SELECT COUNT(*) AS reroutes
         FROM risk_events
         WHERE user_id = ? AND metadata_json LIKE '%ROUTE_SWITCH%' ${dateFilterRiskSql}`,
        [userId]
      );
      reroutesCount = Number(rerouteRows[0]?.reroutes || 0);
    } catch (e) {
      // Non-fatal fallback
    }

    // Generate useful safety suggestions based on ACTUAL data (Phase 18 Step 8)
    const suggestions = [];
    if (totalTrips === 0) {
      suggestions.push("Not enough trip data yet. Complete trips to generate personalized safety insights.");
    } else {
      if (hardBraking >= totalTrips * 2) {
        suggestions.push("Try maintaining a greater following distance to reduce sudden braking.");
      } else if (hardBraking === 0 && totalTrips >= 2) {
        suggestions.push("Smooth deceleration! Zero hard braking events recorded in this period.");
      }

      if (avgRisk >= 60) {
        suggestions.push("Your recent trips show increased safety events. Consider reducing speed in high-risk areas.");
      } else if (avgRisk <= 25 && totalTrips >= 1) {
        suggestions.push("Your recent trips show improved safety performance with consistently low risk.");
      }

      if (reroutesCount > 0) {
        suggestions.push(`Dynamic rerouting successfully diverted around high-risk corridors on ${reroutesCount} occasion${reroutesCount > 1 ? 's' : ''}.`);
      }

      if (suggestions.length === 0) {
        suggestions.push("Drive attentively and maintain safe following distances in varying road conditions.");
      }
    }

    return {
      period: p,
      totalDistanceKm: parseFloat(Number(row.total_distance).toFixed(1)),
      averageRisk: avgRisk,
      averageSafetyScore: avgSafety,
      totalTrips,
      totalWarnings: warnings,
      totalHazards: hazards,
      totalHardBraking: hardBraking,
      reroutesCount,
      riskBreakdown: {
        low: Number(row.low_risk_trips || 0),
        medium: Number(row.medium_risk_trips || 0),
        high: Number(row.high_risk_trips || 0),
      },
      hasSufficientData: totalTrips >= 2,
      trendNote: totalTrips < 2 ? "Not enough trip data yet" : null,
      suggestions,
    };
  }
}

module.exports = new AnalyticsService();
