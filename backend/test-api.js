const http = require('http');
const jwt = require('jsonwebtoken');
const app = require('./src/app');
const config = require('./src/config/environment');

const TEST_PORT = 5006;

function request(options, postData) {
  return new Promise((resolve, reject) => {
    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => { body += chunk; });
      res.on('end', () => {
        let json = null;
        try { json = JSON.parse(body); } catch (_) {}
        resolve({ statusCode: res.statusCode, headers: res.headers, body, json });
      });
    });
    req.on('error', reject);
    if (postData) {
      req.write(typeof postData === 'string' ? postData : JSON.stringify(postData));
    }
    req.end();
  });
}

async function runTests() {
  console.log('==================================================');
  console.log('  SMARTMOVE AI — Backend API Test Suite');
  console.log('==================================================');

  const server = app.listen(TEST_PORT, async () => {
    try {
      // 1. Health Check
      const health = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/health',
        method: 'GET',
      });
      console.log('✔ GET /api/health -> Status:', health.statusCode);
      if (health.statusCode !== 200 || !health.json.success) throw new Error('Health check failed');

      // 2. 404 Route Not Found
      const notFound = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/non-existent-route',
        method: 'GET',
      });
      console.log('✔ GET /api/non-existent-route -> Status:', notFound.statusCode);
      if (notFound.statusCode !== 404 || notFound.json.success !== false) throw new Error('404 failed');

      // 3. Auth Validation: Signup with missing fields
      const badSignup = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/auth/signup',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      }, { email: 'bad' });
      console.log('✔ POST /api/auth/signup (invalid body) -> Status:', badSignup.statusCode);
      if (badSignup.statusCode !== 400 || badSignup.json.success !== false) throw new Error('Validation failed');

      // 4. Protected Route without Token (Auth Middleware)
      const unauthProfile = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/users/me',
        method: 'GET',
      });
      console.log('✔ GET /api/users/me (no token) -> Status:', unauthProfile.statusCode);
      if (unauthProfile.statusCode !== 401) throw new Error('Auth middleware bypass detected');

      // 5. Protected Route with Invalid Token
      const invalidTokenProfile = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/users/me',
        method: 'GET',
        headers: { Authorization: 'Bearer invalid.token.payload' },
      });
      console.log('✔ GET /api/users/me (invalid token) -> Status:', invalidTokenProfile.statusCode);
      if (invalidTokenProfile.statusCode !== 401) throw new Error('Invalid token accepted');

      // 6. JWT Token Generation & Decoding Validation
      const testPayload = { userId: 99, email: 'driver@smartmove.ai' };
      const generatedToken = jwt.sign(testPayload, config.jwt.secret, { expiresIn: '1h' });
      const decoded = jwt.verify(generatedToken, config.jwt.secret);
      if (decoded.userId !== 99 || decoded.email !== 'driver@smartmove.ai') {
        throw new Error('JWT sign/verify mismatch');
      }
      console.log('✔ JWT Sign & Verify verification succeeded');

      // 7. Logout acknowledgement
      const logout = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/auth/logout',
        method: 'POST',
      });
      console.log('✔ POST /api/auth/logout -> Status:', logout.statusCode);
      if (logout.statusCode !== 200 || !logout.json.success) throw new Error('Logout failed');

      // 8. Risk API: GET /api/risk/current without Token -> 401
      const unauthRisk = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/risk/current',
        method: 'GET',
      });
      console.log('✔ GET /api/risk/current (no token) -> Status:', unauthRisk.statusCode);
      if (unauthRisk.statusCode !== 401) throw new Error('Risk endpoint allowed unauthenticated request');

      // 9. Risk API: GET /api/risk/current with Valid Token -> 200
      const authRisk = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/risk/current',
        method: 'GET',
        headers: { Authorization: `Bearer ${generatedToken}` },
      });
      console.log('✔ GET /api/risk/current (authenticated) -> Status:', authRisk.statusCode);
      if (authRisk.statusCode !== 200 || !authRisk.json.success) throw new Error('Risk endpoint failed with valid token');

      const riskData = authRisk.json.data;
      if (typeof riskData.score !== 'number' || !['LOW', 'MEDIUM', 'HIGH'].includes(riskData.level)) {
        throw new Error('Risk response format invalid');
      }
      if (!riskData.recommendation || !riskData.factors || !Array.isArray(riskData.reasons)) {
        throw new Error('Risk response missing required fields');
      }
      console.log(`✔ Risk Engine Response: Score=${riskData.score}, Level=${riskData.level}, Recommendation="${riskData.recommendation}"`);

      // 10. Risk API with dynamic scenario overrides: High traffic & heavy rain -> Higher score
      const severeRisk = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/risk/current?traffic=HIGH&weather=HEAVY_RAIN',
        method: 'GET',
        headers: { Authorization: `Bearer ${generatedToken}` },
      });
      console.log('✔ GET /api/risk/current?traffic=HIGH&weather=HEAVY_RAIN -> Status:', severeRisk.statusCode);
      if (severeRisk.statusCode !== 200 || severeRisk.json.data.score <= riskData.score) {
        throw new Error('Risk score did not increase under severe conditions');
      }
      console.log(`✔ Dynamic Risk Scaling verified: Base=${riskData.score} -> Severe=${severeRisk.json.data.score}`);

      // 11. Weather API: GET /api/weather/current without Token -> 401
      const unauthWeather = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/weather/current?latitude=18.5204&longitude=73.8567',
        method: 'GET',
      });
      console.log('✔ GET /api/weather/current (no token) -> Status:', unauthWeather.statusCode);
      if (unauthWeather.statusCode !== 401) throw new Error('Weather endpoint allowed unauthenticated request');

      // 12. Weather API: GET /api/weather/current with Invalid Coordinates -> 400
      const badWeather = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/weather/current?latitude=999&longitude=73.8567',
        method: 'GET',
        headers: { Authorization: `Bearer ${generatedToken}` },
      });
      console.log('✔ GET /api/weather/current (invalid coords) -> Status:', badWeather.statusCode);
      if (badWeather.statusCode !== 400) throw new Error('Weather endpoint accepted invalid coordinates');

      // 13. Weather API: GET /api/weather/current with Valid Coordinates -> 200
      const authWeather = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/weather/current?latitude=18.5204&longitude=73.8567',
        method: 'GET',
        headers: { Authorization: `Bearer ${generatedToken}` },
      });
      console.log('✔ GET /api/weather/current (valid coords) -> Status:', authWeather.statusCode);
      if (authWeather.statusCode !== 200 || !authWeather.json.success) throw new Error('Weather endpoint failed');

      const wData = authWeather.json.data;
      if (typeof wData.temperatureC !== 'number' || !wData.condition || !wData.observedAt) {
        throw new Error('Weather response missing required fields');
      }
      console.log(`✔ Live Weather API Response: Temp=${wData.temperatureC}°C, Condition=${wData.condition}, Visibility=${wData.visibilityKm}km`);

      // 14. Risk API incorporating Live Geographic Weather
      const geoRisk = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/risk/current?latitude=18.5204&longitude=73.8567',
        method: 'GET',
        headers: { Authorization: `Bearer ${generatedToken}` },
      });
      console.log('✔ GET /api/risk/current?latitude=18.5204&longitude=73.8567 -> Status:', geoRisk.statusCode);
      if (geoRisk.statusCode !== 200 || !geoRisk.json.data.weatherAvailable) {
        throw new Error('Risk endpoint failed to incorporate real geographic weather');
      }
      console.log(`✔ Geo-located Risk verified with real weather: WeatherFactor=${geoRisk.json.data.factors.weather}, OverallScore=${geoRisk.json.data.score}`);

      // 15. Test GET /api/traffic/current without token (Expect 401)
      const noAuthTraffic = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/traffic/current?latitude=18.5204&longitude=73.8567',
        method: 'GET',
      });
      console.log('✔ GET /api/traffic/current (no token) -> Status:', noAuthTraffic.statusCode);
      if (noAuthTraffic.statusCode !== 401) {
        throw new Error(`Expected 401 for unauthenticated traffic request, got ${noAuthTraffic.statusCode}`);
      }

      // 16. Test GET /api/traffic/current with invalid coordinates (Expect 400)
      const badCoordsTraffic = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/traffic/current?latitude=999&longitude=73.8567',
        method: 'GET',
        headers: { Authorization: `Bearer ${generatedToken}` },
      });
      console.log('✔ GET /api/traffic/current (invalid coords) -> Status:', badCoordsTraffic.statusCode);
      if (badCoordsTraffic.statusCode !== 400) {
        throw new Error(`Expected 400 for out-of-range coordinates, got ${badCoordsTraffic.statusCode}`);
      }

      // 17. Test GET /api/traffic/current with valid coordinates (Expect 200)
      const validTraffic = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/traffic/current?latitude=18.5204&longitude=73.8567',
        method: 'GET',
        headers: { Authorization: `Bearer ${generatedToken}` },
      });
      console.log('✔ GET /api/traffic/current (valid coords) -> Status:', validTraffic.statusCode);
      if (validTraffic.statusCode !== 200 || !validTraffic.json.data) {
        throw new Error(`Expected 200 with traffic payload, got ${validTraffic.statusCode}`);
      }
      const tData = validTraffic.json.data;
      console.log(`✔ Live Traffic API Response: Speed=${tData.currentSpeedKmh}/${tData.freeFlowSpeedKmh} km/h, Ratio=${tData.speedRatio}, Congestion=${tData.congestionLevel}, Source=${tData.source}`);

      // 18. Verify Traffic integrated into dynamic risk response
      if (!geoRisk.json.data.trafficAvailable || !geoRisk.json.data.traffic) {
        throw new Error('Risk response missing dynamic traffic payload');
      }
      console.log(`✔ Geo-located Risk verified with dynamic traffic: TrafficFactor=${geoRisk.json.data.factors.traffic}, Congestion=${geoRisk.json.data.traffic.congestionLevel}`);

      // 19. Test POST /api/routes/recommend without token (Expect 401)
      const noAuthRoutes = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/routes/recommend',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      }, {
        origin: { latitude: 18.5204, longitude: 73.8567 },
        destination: { latitude: 18.5314, longitude: 73.8446 },
      });
      console.log('✔ POST /api/routes/recommend (no token) -> Status:', noAuthRoutes.statusCode);
      if (noAuthRoutes.statusCode !== 401) {
        throw new Error(`Expected 401 for unauthenticated route recommendation, got ${noAuthRoutes.statusCode}`);
      }

      // 20. Test POST /api/routes/recommend with invalid coordinates (Expect 400)
      const badCoordsRoutes = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/routes/recommend',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${generatedToken}`,
        },
      }, {
        origin: { latitude: 999, longitude: 73.8567 },
        destination: { latitude: 18.5314, longitude: 73.8446 },
      });
      console.log('✔ POST /api/routes/recommend (invalid coords) -> Status:', badCoordsRoutes.statusCode);
      if (badCoordsRoutes.statusCode !== 400) {
        throw new Error(`Expected 400 for out-of-range coordinates, got ${badCoordsRoutes.statusCode}`);
      }

      // 21. Test POST /api/routes/recommend with valid coordinates (Expect 200)
      const validRoutes = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/routes/recommend',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${generatedToken}`,
        },
      }, {
        origin: { latitude: 18.5204, longitude: 73.8567 },
        destination: { latitude: 18.5314, longitude: 73.8446 },
      });
      console.log('✔ POST /api/routes/recommend (valid coords) -> Status:', validRoutes.statusCode);
      if (validRoutes.statusCode !== 200 || !validRoutes.json.data) {
        throw new Error(`Expected 200 with route recommendations, got ${validRoutes.statusCode}`);
      }
      const rData = validRoutes.json.data;
      console.log(`✔ Route Recommendation API: Recommended=${rData.recommendedRouteId}, Safest=${rData.safestRouteId}, Fastest=${rData.fastestRouteId}, CandidateCount=${rData.routes.length}`);
      console.log(`✔ Rationale: "${rData.recommendationReason}"`);

      // 22. Test POST /api/routes/recalculate without token (Expect 401)
      const noAuthRecalc = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/routes/recalculate',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      }, {
        currentRouteId: 'route-a',
      });
      console.log('✔ POST /api/routes/recalculate (no token) -> Status:', noAuthRecalc.statusCode);
      if (noAuthRecalc.statusCode !== 401) {
        throw new Error(`Expected 401 for unauthenticated en-route recalculation, got ${noAuthRecalc.statusCode}`);
      }

      // 23. Test POST /api/routes/recalculate without currentRouteId (Expect 400)
      const badRecalc = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/routes/recalculate',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${generatedToken}`,
        },
      }, {});
      console.log('✔ POST /api/routes/recalculate (missing routeId) -> Status:', badRecalc.statusCode);
      if (badRecalc.statusCode !== 400) {
        throw new Error(`Expected 400 for missing currentRouteId, got ${badRecalc.statusCode}`);
      }

      // 24. Test POST /api/routes/recalculate valid (Expect 200)
      const validRecalc = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/routes/recalculate',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${generatedToken}`,
        },
      }, {
        currentRouteId: 'route-a',
        origin: { latitude: 18.5204, longitude: 73.8567 },
        destination: { latitude: 18.5314, longitude: 73.8446 },
      });
      console.log('✔ POST /api/routes/recalculate (valid request) -> Status:', validRecalc.statusCode);
      if (validRecalc.statusCode !== 200 || !validRecalc.json.data) {
        throw new Error(`Expected 200 for en-route recalculation, got ${validRecalc.statusCode}`);
      }
      console.log(`✔ En-Route Recalculation API: ShouldReroute=${validRecalc.json.data.shouldReroute}, CurrentRisk=${validRecalc.json.data.currentRoute.riskScore}`);

      // 25. Test POST /api/routes/switch valid (Expect 200)
      const validSwitch = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/routes/switch',
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${generatedToken}`,
        },
      }, {
        previousRouteId: 'route-a',
        newRouteId: 'route-b',
        reason: 'Driver accepted safer route prompt',
        oldRisk: 72,
        newRisk: 34,
      });
      console.log('✔ POST /api/routes/switch (valid request) -> Status:', validSwitch.statusCode);
      if (validSwitch.statusCode !== 200 || !validSwitch.json.data) {
        throw new Error(`Expected 200 for route switch, got ${validSwitch.statusCode}`);
      }
      console.log(`✔ Route Switch API: NewRouteId=${validSwitch.json.data.selectedRouteId}`);

      // 26. Test GET /api/trips without token (Expect 401)
      const noAuthTrips = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/trips',
        method: 'GET',
      });
      console.log('✔ GET /api/trips (no token) -> Status:', noAuthTrips.statusCode);
      if (noAuthTrips.statusCode !== 401) {
        throw new Error(`Expected 401 for unauthenticated trips, got ${noAuthTrips.statusCode}`);
      }

      // 27. Test GET /api/analytics without token (Expect 401)
      const noAuthAnalytics = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/analytics',
        method: 'GET',
      });
      console.log('✔ GET /api/analytics (no token) -> Status:', noAuthAnalytics.statusCode);
      if (noAuthAnalytics.statusCode !== 401) {
        throw new Error(`Expected 401 for unauthenticated analytics, got ${noAuthAnalytics.statusCode}`);
      }

      // 28. Test POST /api/trips without token (Expect 401)
      const noAuthTripCreate = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/trips',
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
      }, {
        startLocation: 'Deccan',
        destinationLocation: 'Kothrud',
      });
      console.log('✔ POST /api/trips (no token) -> Status:', noAuthTripCreate.statusCode);
      if (noAuthTripCreate.statusCode !== 401) {
        throw new Error(`Expected 401 for unauthenticated trip creation, got ${noAuthTripCreate.statusCode}`);
      }

      // 29. Test GET /api/trips/1 without token (Expect 401)
      const noAuthTripDetails = await request({
        hostname: 'localhost',
        port: TEST_PORT,
        path: '/api/trips/1',
        method: 'GET',
      });
      console.log('✔ GET /api/trips/1 (no token) -> Status:', noAuthTripDetails.statusCode);
      if (noAuthTripDetails.statusCode !== 401) {
        throw new Error(`Expected 401 for unauthenticated trip details, got ${noAuthTripDetails.statusCode}`);
      }

      console.log('\n==================================================');
      console.log('  ✅ ALL BACKEND API, RISK, WEATHER, TRAFFIC, REROUTE & TRIP TESTS PASSED');
      console.log('==================================================');
    } catch (err) {
      console.error('\n❌ Test Failure:', err.message);
      process.exitCode = 1;
    } finally {
      server.close();
    }
  });
}

runTests();
