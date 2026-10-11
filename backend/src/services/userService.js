const pool = require('../config/database');

/**
 * User & Vehicle Service.
 * Manages user profile updates and isolated driver vehicle CRUD operations.
 */
class UserService {
  async getProfile(userId) {
    const [rows] = await pool.query(
      'SELECT id, full_name, email, phone, created_at, updated_at FROM users WHERE id = ? LIMIT 1',
      [userId]
    );

    if (rows.length === 0) {
      const err = new Error('User not found.');
      err.statusCode = 404;
      throw err;
    }

    const u = rows[0];
    return {
      id: u.id,
      fullName: u.full_name,
      email: u.email,
      phone: u.phone,
      createdAt: u.created_at,
      updatedAt: u.updated_at,
    };
  }

  async updateProfile(userId, { fullName, phone }) {
    if (!fullName || !fullName.trim()) {
      const err = new Error('Full name cannot be empty.');
      err.statusCode = 400;
      throw err;
    }

    await pool.query(
      'UPDATE users SET full_name = ?, phone = ? WHERE id = ?',
      [fullName.trim(), phone ? phone.trim() : null, userId]
    );

    return this.getProfile(userId);
  }

  async getVehicles(userId) {
    const [rows] = await pool.query(
      'SELECT id, vehicle_type, vehicle_number, created_at, updated_at FROM vehicles WHERE user_id = ? ORDER BY id DESC',
      [userId]
    );

    return rows.map((v) => ({
      id: v.id,
      vehicleType: v.vehicle_type,
      vehicleNumber: v.vehicle_number,
      createdAt: v.created_at,
      updatedAt: v.updated_at,
    }));
  }

  async addVehicle(userId, { vehicleType, vehicleNumber }) {
    if (!vehicleType || !vehicleType.trim()) {
      const err = new Error('Vehicle type is required.');
      err.statusCode = 400;
      throw err;
    }

    const allowedTypes = ['Car', 'Motorcycle', 'SUV', 'Truck', 'Commercial'];
    const typeClean = vehicleType.trim();
    if (!allowedTypes.some((t) => t.toLowerCase() === typeClean.toLowerCase())) {
      const err = new Error(`Invalid vehicle type. Allowed: ${allowedTypes.join(', ')}`);
      err.statusCode = 400;
      throw err;
    }

    const [result] = await pool.query(
      'INSERT INTO vehicles (user_id, vehicle_type, vehicle_number) VALUES (?, ?, ?)',
      [userId, typeClean, vehicleNumber ? vehicleNumber.trim() : null]
    );

    return {
      id: result.insertId,
      vehicleType: typeClean,
      vehicleNumber: vehicleNumber ? vehicleNumber.trim() : null,
    };
  }

  async updateVehicle(userId, vehicleId, { vehicleType, vehicleNumber }) {
    // Verify ownership
    const [existing] = await pool.query(
      'SELECT id FROM vehicles WHERE id = ? AND user_id = ? LIMIT 1',
      [vehicleId, userId]
    );

    if (existing.length === 0) {
      const err = new Error('Vehicle not found or unauthorized.');
      err.statusCode = 404;
      throw err;
    }

    const typeClean = vehicleType ? vehicleType.trim() : null;
    const numClean = vehicleNumber ? vehicleNumber.trim() : null;

    await pool.query(
      'UPDATE vehicles SET vehicle_type = COALESCE(?, vehicle_type), vehicle_number = COALESCE(?, vehicle_number) WHERE id = ? AND user_id = ?',
      [typeClean, numClean, vehicleId, userId]
    );

    return {
      id: Number(vehicleId),
      vehicleType: typeClean,
      vehicleNumber: numClean,
    };
  }

  async deleteVehicle(userId, vehicleId) {
    const [result] = await pool.query(
      'DELETE FROM vehicles WHERE id = ? AND user_id = ?',
      [vehicleId, userId]
    );

    if (result.affectedRows === 0) {
      const err = new Error('Vehicle not found or unauthorized.');
      err.statusCode = 404;
      throw err;
    }

    return { success: true, message: 'Vehicle removed successfully' };
  }
}

module.exports = new UserService();
