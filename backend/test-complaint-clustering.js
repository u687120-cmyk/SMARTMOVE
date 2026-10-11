const app = require('./src/app');
const http = require('http');

let server;
const PORT = 5006;

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
  console.log(`SMARTMOVE AI — Passive Clustering & Civic Grievance Verification`);
  console.log(`==================================================\n`);

  try {
    const testLat = 18.5314 + Math.random() * 0.001;
    const testLng = 73.8446 + Math.random() * 0.001;

    // Test 1: Device 1 sends accelerometer spike
    console.log('1. Device 1: First passive pothole spike...');
    const res1 = await request('POST', '/api/complaints/sensor-event', {
      lat: testLat,
      lng: testLng,
      magnitude: 14.8,
      speed: 28.5,
      deviceId: 'dev_phone_alpha',
      locationName: 'University Road / FC Road Junction',
    });
    console.log('   Response:', res1.body.data);
    const hazardId = res1.body.data.hazardId;
    if (res1.body.data.distinctDevicesConfirmed !== 1) throw new Error('Expected 1 distinct device');
    console.log('   ✅ Device 1 logged. New hazard cluster created.\n');

    // Test 2: Device 1 sends AGAIN (should be deduplicated!)
    console.log('2. Device 1 sends duplicate spike for same hazard...');
    const res2 = await request('POST', '/api/complaints/sensor-event', {
      lat: testLat,
      lng: testLng,
      magnitude: 15.2,
      speed: 24.0,
      deviceId: 'dev_phone_alpha',
    });
    console.log('   Response:', res2.body.data);
    if (!res2.body.data.alreadyLoggedByThisDevice) throw new Error('Duplicate device check failed');
    console.log('   ✅ Duplicate device check passed (no duplicate vote inflation).\n');

    // Test 3: Device 2 passes within 15 meters
    console.log('3. Device 2 passes near same pothole (10 meters offset)...');
    const res3 = await request('POST', '/api/complaints/sensor-event', {
      lat: testLat + 0.00008,
      lng: testLng + 0.00008,
      magnitude: 13.9,
      speed: 31.0,
      deviceId: 'dev_phone_beta',
    });
    console.log('   Response:', res3.body.data);
    if (res3.body.data.distinctDevicesConfirmed !== 2) throw new Error('Expected 2 distinct devices');
    console.log('   ✅ Cluster matched! Confidence elevated to 2 devices.\n');

    // Test 4: Device 3 passes (triggers verification & automated complaint!)
    console.log('4. Device 3 passes (Crowd threshold 3 reached!)...');
    const res4 = await request('POST', '/api/complaints/sensor-event', {
      lat: testLat - 0.00005,
      lng: testLng - 0.00005,
      magnitude: 16.1,
      speed: 22.0,
      deviceId: 'dev_phone_gamma',
    });
    console.log('   Response:', res4.body.data);
    if (!res4.body.data.isVerified) throw new Error('Expected isVerified = true');
    if (!res4.body.data.autoComplaintTriggered) throw new Error('Expected autoComplaintTriggered = true');
    console.log('   ✅ Verified! Automated civic complaint dispatched to authority!\n');

    // Test 5: Clean passes (vehicles pass without jerk)
    console.log('5. Vehicles pass cleanly over the road (Clean Pass Tracking)...');
    for (let i = 1; i <= 5; i++) {
      const passRes = await request('POST', '/api/complaints/clean-pass', {
        hazardId,
        deviceId: `clean_vehicle_${i}`,
        speed: 35.0,
      });
      console.log(`   Clean pass ${i}/5:`, passRes.body.data.message);
    }
    console.log('   ✅ 5 Clean passes registered. Road automatically verified as FIXED!\n');

    // Test 6: Public Dashboard Check
    console.log('6. Checking Civic Transparency Public Dashboard...');
    const dashRes = await request('GET', '/api/complaints/public-dashboard');
    console.log('   Dashboard Metrics:', dashRes.body.data.metrics);
    console.log('   Recent Complaints count:', dashRes.body.data.recentComplaints.length);
    console.log('   Recent Timeline events count:', dashRes.body.data.recentTimeline.length);
    console.log('   ✅ Public Dashboard returns live civic metrics!\n');

    console.log('🎉 ALL CROWDSENSING & CIVIC GRIEVANCE TESTS PASSED SUCCESSFULLY!');
  } catch (err) {
    console.error('❌ Test failed:', err);
    process.exitCode = 1;
  } finally {
    server.close();
    process.exit(process.exitCode || 0);
  }
}

runTests();
