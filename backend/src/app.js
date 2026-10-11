const express = require('express');
const path = require('path');
const cors = require('cors');
const config = require('./config/environment');

// Import Route Handlers
const healthRoutes = require('./routes/healthRoutes');
const authRoutes = require('./routes/authRoutes');
const userRoutes = require('./routes/userRoutes');
const tripRoutes = require('./routes/tripRoutes');
const hazardRoutes = require('./routes/hazardRoutes');
const analyticsRoutes = require('./routes/analyticsRoutes');
const riskRoutes = require('./routes/riskRoutes');
const weatherRoutes = require('./routes/weatherRoutes');
const trafficRoutes = require('./routes/trafficRoutes');
const routeRoutes = require('./routes/routeRoutes');
const placesRoutes = require('./routes/placesRoutes');
const complaintRoutes = require('./routes/complaintRoutes');

// Import Error & 404 Middleware
const errorHandler = require('./middleware/errorHandler');
const notFoundHandler = require('./middleware/notFoundHandler');

const app = express();

// Serve static frontend web client from public folder
app.use(express.static(path.join(__dirname, '../public')));
// Single Source of Truth: Serve root mobile frontend directly without copy-paste
app.use('/mobile', express.static(path.join(__dirname, '../../')));

// --------------------------------------------------
// 1. GLOBAL MIDDLEWARE
// --------------------------------------------------

// CORS configuration (Permissive for local development & Android emulator)
app.use(
  cors({
    origin: (origin, callback) => {
      // Allow non-browser requests (e.g. mobile apps, curl, Postman)
      if (!origin) return callback(null, true);
      if (config.allowedOrigins.includes(origin) || !config.isProduction) {
        return callback(null, true);
      }
      callback(new Error('CORS request blocked by security policy'));
    },
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization'],
    credentials: true,
  })
);

// JSON body parser with safety limit
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// Basic Request Logging in development
if (!config.isProduction) {
  app.use((req, res, next) => {
    const start = Date.now();
    res.on('finish', () => {
      const duration = Date.now() - start;
      console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl} ${res.statusCode} - ${duration}ms`);
    });
    next();
  });
}

// --------------------------------------------------
// 2. API ROUTES
// --------------------------------------------------
app.use('/api/health', healthRoutes);
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/trips', tripRoutes);
app.use('/api/hazards', hazardRoutes);
app.use('/api/analytics', analyticsRoutes);
app.use('/api/risk', riskRoutes);
app.use('/api/weather', weatherRoutes);
app.use('/api/traffic', trafficRoutes);
app.use('/api/routes', routeRoutes);
app.use('/api/places', placesRoutes);
app.use('/api/complaints', complaintRoutes);

// Root redirect / ping
app.get('/', (req, res) => {
  res.status(200).json({
    success: true,
    message: 'Welcome to SMARTMOVE AI REST API',
    documentation: '/api/health',
  });
});

// --------------------------------------------------
// 3. 404 & ERROR HANDLING MIDDLEWARE
// --------------------------------------------------
app.use(notFoundHandler);
app.use(errorHandler);

module.exports = app;
