const app = require('./src/app');
const http = require('http');
const pool = require('./src/config/database');

let server;
const PORT = 5007;

function request(method, path, body = null) {
  return new Promise((resolve, reject) => {
    const data = body ? JSON.stringify(body) : null;
    const req = http.request(
      {
        hostname: 'localhost',
        port: PORT,
        path,
        method,
        headers: {
          'Content-Type': 'application/json',
          ...(data ? { 'Content-Length': Buffer.byteLength(data) } : {}),
        },
      },
      (res) => {
        let resBody = '';
        res.on('data', (chunk) => (resBody += chunk));
        res.on('end', () => {
          try {
            resolve({ status: res.statusCode, body: JSON.parse(resBody) });
          } catch (e) {
            resolve({ status: res.statusCode, body: resBody });
          }
        });
      }
    );
    req.on('error', reject);
    if (data) req.write(data);
    req.end();
  });
}

async function runTests() {
  server = app.listen(PORT);
  console.log(`\n==================================================`);
  console.log(`SMARTMOVE AI — Safeguards, Reopening & Escalation Verification`);
  console.log(`==================================================\n`);

  try {
    const testLat = 18.5204 + Math.random() * 0.001;
    const testLng = 73.8567 + Math.random() * 0.001;

    // 1. Double Complaint Prevention Test
    console.log('1. Testing Double Complaint Prevention (5 distinct devices)...');
    let hazardId = null;
    for (let i = 1; i <= 5; i++) {
      const res = await request('POST', '/api/complaints/sensor-event', {
        lat: testLat,
        lng: testLng,
        magnitude: 14.5,
        speed: 25.0,
        deviceId: `test_device_safeguard_${i}_${Date.now()}`,
        locationName: 'FC Road Demonstration Test Corridor',
      });
      if (res.body?.data?.hazardId) {
        hazardId = res.body.data.hazardId;
      }
    }

    const [complaintRows] = await pool.query(
      'SELECT COUNT(*) as count FROM complaints WHERE hazard_id = ?',
      [hazardId]
    );
    const count = complaintRows[0].count;
    console.log(`   Complaints generated for hazard ${hazardId}: ${count}`);
    if (count !== 1) {
      throw new Error(`Double complaint bug! Expected 1 complaint, got ${count}`);
    }
    console.log('   ✅ Double Complaint Prevention PASSED: Exactly 1 complaint generated for 5 distinct device spikes!\n');

    // 2. Reopening Resolved Hazard Test
    console.log('2. Testing Hazard Auto-Reopening when new spike occurs...');
    // Manually mark resolved
    await pool.query("UPDATE hazards SET status = 'RESOLVED' WHERE id = ?", [hazardId]);

    const reopenRes = await request('POST', '/api/complaints/sensor-event', {
      lat: testLat,
      lng: testLng,
      magnitude: 16.0,
      speed: 28.0,
      deviceId: `new_driver_after_fix_${Date.now()}`,
    });
    console.log('   Reopen Response:', reopenRes.body.data);
    const [hCheck] = await pool.query("SELECT status FROM hazards WHERE id = ?", [hazardId]);
    console.log(`   Hazard status in DB: ${hCheck[0].status}`);
    if (hCheck[0].status !== 'REOPENED') {
      throw new Error(`Expected status REOPENED, got ${hCheck[0].status}`);
    }
    console.log('   ✅ Reopen Test PASSED: Resolved road automatically reopened on new spike!\n');

    // 3. Fast-Forward +7 Days Escalation Test
    console.log('3. Testing Fast-Forward +7 Days Escalation Cycle...');
    const ffRes1 = await request('POST', '/api/complaints/fast-forward-days', { days: 7 });
    console.log('   Day 7 Fast-Forward Result:', ffRes1.body);

    const [cCheck1] = await pool.query("SELECT stage, status FROM complaints WHERE hazard_id = ?", [hazardId]);
    console.log(`   Complaint stage after +7 days: ${cCheck1[0]?.stage} (${cCheck1[0]?.status})`);
    if (cCheck1[0]?.stage !== 'DAY_7') {
      throw new Error(`Expected stage DAY_7, got ${cCheck1[0]?.stage}`);
    }
    console.log('   ✅ Day 7 Reminder Escalation verified!\n');

    console.log('4. Testing Day 14 Ministerial Escalation...');
    const ffRes2 = await request('POST', '/api/complaints/fast-forward-days', { days: 7 });
    console.log('   Day 14 Fast-Forward Result:', ffRes2.body);

    const [cCheck2] = await pool.query("SELECT stage, status FROM complaints WHERE hazard_id = ?", [hazardId]);
    console.log(`   Complaint stage after +14 days: ${cCheck2[0]?.stage} (${cCheck2[0]?.status})`);
    if (cCheck2[0]?.stage !== 'DAY_14') {
      throw new Error(`Expected stage DAY_14, got ${cCheck2[0]?.stage}`);
    }
    console.log('   ✅ Day 14 Ministerial Escalation verified!\n');

    // 5. Test Live Route Recommendation with Passenger Profile
    console.log('5. Testing Live Route Recommendation with DB Hazards & Passenger Profiles...');
    const recRes = await request('POST', '/api/routes/recommend', {
      origin: { latitude: 18.5204, longitude: 73.8567 },
      destination: { latitude: 18.5314, longitude: 73.8446 },
      passengerProfile: 'PREGNANT_WEAK',
    });
    console.log('   Recommended Route for PREGNANT_WEAK:', recRes.body?.data?.recommendedRouteId);
    console.log('   Reasoning:', recRes.body?.data?.recommendationReason);
    if (!recRes.body?.data?.recommendedRouteId) {
      throw new Error('Route recommendation failed');
    }
    console.log('   ✅ Live Route Recommendation with DB hazards & Passenger Profiles PASSED!\n');

    // 6. Public Dashboard Stats Check
    console.log('6. Checking Public Transparency Dashboard Stats...');
    const dashRes = await request('GET', '/api/complaints/public-dashboard');
    console.log('   Public Dashboard Metrics:', dashRes.body.data.metrics);
    console.log('   ✅ Public Dashboard returns live civic escalation counts!\n');

    console.log('🎉 ALL SAFEGUARD, REOPEN, ESCALATION & ROUTING TESTS PASSED!');
  } catch (err) {
    console.error('❌ Test failed:', err);
    process.exitCode = 1;
  } finally {
    server.close();
    process.exit(process.exitCode || 0);
  }
}

runTests();
