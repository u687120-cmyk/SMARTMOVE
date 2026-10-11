const pool = require('../config/database');

/**
 * Authority Service:
 * Maps geographic coordinates and OpenStreetMap road classifications
 * to the governing civic authority (NHAI, PWD, Municipal Corporation, Gram Panchayat).
 */
class AuthorityService {
  /**
   * Determine jurisdiction & road type based on location and OSM data
   */
  async resolveAuthority(lat, lng, roadNameHint = '') {
    // 1. Heuristic road category classification
    const nameLower = (roadNameHint || '').toLowerCase();
    let roadType = 'CITY_ROAD';
    let jurisdictionType = 'MUNICIPAL_CORP';

    if (nameLower.includes('national highway') || nameLower.includes('nh ') || nameLower.startsWith('nh-') || nameLower.startsWith('nh')) {
      roadType = 'NATIONAL_HIGHWAY';
      jurisdictionType = 'NHAI';
    } else if (nameLower.includes('state highway') || nameLower.includes('sh ') || nameLower.startsWith('sh-') || nameLower.startsWith('sh')) {
      roadType = 'STATE_HIGHWAY';
      jurisdictionType = 'PWD';
    } else if (nameLower.includes('panchayat') || nameLower.includes('rural') || nameLower.includes('village') || nameLower.includes('gaothan')) {
      roadType = 'VILLAGE_ROAD';
      jurisdictionType = 'GRAM_PANCHAYAT';
    }

    // 2. Query directory
    const [rows] = await pool.query(
      `SELECT * FROM authorities WHERE jurisdiction_type = ? LIMIT 1`,
      [jurisdictionType]
    );

    if (rows.length > 0) {
      return {
        roadType,
        authority: rows[0],
      };
    }

    // Default fallback: Municipal Corporation
    const [defaultRows] = await pool.query(
      `SELECT * FROM authorities WHERE jurisdiction_type = 'MUNICIPAL_CORP' LIMIT 1`
    );

    return {
      roadType,
      authority: defaultRows[0] || {
        id: 'auth_fallback',
        name: 'Local Municipal Corporation (Road Maintenance Dept)',
        jurisdiction_type: 'MUNICIPAL_CORP',
        official_email: 'roads.grievance@localgov.in.mock',
        escalation_email: 'commissioner@localgov.in.mock',
        helpline: '1800-SMARTMOVE',
      },
    };
  }

  async getAllAuthorities() {
    const [rows] = await pool.query(`SELECT * FROM authorities ORDER BY name ASC`);
    return rows;
  }
}

module.exports = new AuthorityService();
