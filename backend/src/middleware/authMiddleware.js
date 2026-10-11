const jwt = require('jsonwebtoken');
const config = require('../config/environment');

/**
 * Authentication Middleware.
 * Intercepts incoming requests, verifies Bearer JWT token,
 * and attaches decoded user context (userId, email) to req.user.
 */
function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      message: 'Authentication required. Authorization header missing or malformed.',
    });
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, config.jwt.secret);
    req.user = {
      id: decoded.userId,
      email: decoded.email,
    };
    next();
  } catch (error) {
    return res.status(401).json({
      success: false,
      message: 'Invalid or expired authentication token.',
    });
  }
}

/**
 * Optional Authentication Middleware.
 * Attaches user if valid token exists, else falls back to default traveler user ID 1.
 */
function optionalAuthMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;

  if (authHeader && authHeader.startsWith('Bearer ')) {
    const token = authHeader.split(' ')[1];
    try {
      const decoded = jwt.verify(token, config.jwt.secret);
      req.user = {
        id: decoded.userId,
        email: decoded.email,
      };
      return next();
    } catch (e) {}
  }

  // Fallback to guest user ID 1
  req.user = { id: 1, email: 'traveler@smartmove.ai' };
  next();
}

module.exports = authMiddleware;
module.exports.optionalAuth = optionalAuthMiddleware;

