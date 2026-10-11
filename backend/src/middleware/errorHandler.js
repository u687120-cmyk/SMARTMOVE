const config = require('../config/environment');

/**
 * Central Error Handling Middleware.
 * Catches all uncaught errors across the Express pipeline and formats
 * a consistent JSON error response without leaking internal stack traces in production.
 */
function errorHandler(err, req, res, next) {
  const statusCode = err.statusCode || 500;
  const message = err.message || 'Internal server error';

  const response = {
    success: false,
    message: message,
  };

  if (!config.isProduction && err.stack) {
    response.error = {
      details: err.stack,
    };
  }

  res.status(statusCode).json(response);
}

module.exports = errorHandler;
