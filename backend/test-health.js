const http = require('http');
const app = require('./src/app');

// Run on test port 5005
const PORT = 5005;
const server = app.listen(PORT, async () => {
  console.log(`Test server running on port ${PORT}`);

  try {
    // Test 1: GET /api/health
    const healthRes = await makeRequest(`http://localhost:${PORT}/api/health`);
    console.log('1. Health Check Status:', healthRes.statusCode);
    console.log('   Health Response:', healthRes.body);
    if (healthRes.statusCode !== 200) throw new Error('Health check failed with status ' + healthRes.statusCode);
    const healthJson = JSON.parse(healthRes.body);
    if (!healthJson.success || healthJson.message !== 'SMARTMOVE AI backend is running') {
      throw new Error('Unexpected health response body');
    }

    // Test 2: GET /api/unknown-endpoint (404)
    const notFoundRes = await makeRequest(`http://localhost:${PORT}/api/unknown-endpoint`);
    console.log('2. 404 Status:', notFoundRes.statusCode);
    console.log('   404 Response:', notFoundRes.body);
    if (notFoundRes.statusCode !== 404) throw new Error('Expected 404 for unknown route');
    const notFoundJson = JSON.parse(notFoundRes.body);
    if (notFoundJson.success !== false) throw new Error('Expected success: false for 404');

    // Test 3: GET /api/trips (501 Placeholder)
    const tripsRes = await makeRequest(`http://localhost:${PORT}/api/trips`);
    console.log('3. Placeholder Status:', tripsRes.statusCode);
    console.log('   501 Response:', tripsRes.body);
    if (tripsRes.statusCode !== 501) throw new Error('Expected 501 for unimplemented route');

    console.log('\n✅ ALL BACKEND HEALTH & ROUTE TESTS PASSED SUCCESSFULLY!');
  } catch (err) {
    console.error('❌ Test failed:', err.message);
    process.exitCode = 1;
  } finally {
    server.close(() => {
      console.log('Test server closed.');
    });
  }
});

function makeRequest(url) {
  return new Promise((resolve, reject) => {
    http.get(url, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        resolve({ statusCode: res.statusCode, body: data });
      });
    }).on('error', reject);
  });
}
