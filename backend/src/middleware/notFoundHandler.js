/**
 * 404 Route Not Found Middleware.
 * Intercepts requests that do not match any defined API endpoint.
 */
function notFoundHandler(req, res, next) {
  res.status(404).json({
    success: false,
    message: `Route not found: ${req.method} ${req.originalUrl}`,
  });
}

module.exports = notFoundHandler;
