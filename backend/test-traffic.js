const assert = require('assert');
const trafficService = require('./src/services/trafficService');
const TrafficConfig = require('./src/config/trafficConfig');

async function testTrafficEngine() {
  console.log('==================================================');
  console.log('  SMARTMOVE AI — Traffic Service Unit & Flow Tests');
  console.log('==================================================\n');

  // --------------------------------------------------
  // 1. COORDINATE VALIDATION TESTS
  // --------------------------------------------------
  console.log('--- 1. Coordinate Validation Tests ---');

  assert.strictEqual(trafficService.isValidCoordinates(18.5204, 73.8567), true);
  assert.strictEqual(trafficService.isValidCoordinates(0, 0), true);
  assert.strictEqual(trafficService.isValidCoordinates(-90, -180), true);
  assert.strictEqual(trafficService.isValidCoordinates(90, 180), true);

  assert.strictEqual(trafficService.isValidCoordinates(90.1, 73.8567), false, 'Lat > 90 must fail');
  assert.strictEqual(trafficService.isValidCoordinates(-90.1, 73.8567), false, 'Lat < -90 must fail');
  assert.strictEqual(trafficService.isValidCoordinates(18.5204, 180.1), false, 'Lng > 180 must fail');
  assert.strictEqual(trafficService.isValidCoordinates(18.5204, -180.1), false, 'Lng < -180 must fail');
  assert.strictEqual(trafficService.isValidCoordinates('bad', 73.8567), false, 'String lat must fail');
  console.log('✔ Coordinate validation correctly validates range [-90..90], [-180..180]');

  // --------------------------------------------------
  // 2. SPEED RATIO & SAFE CALCULATION TESTS
  // --------------------------------------------------
  console.log('\n--- 2. Speed Ratio & Safe Division Tests ---');

  assert.strictEqual(trafficService.calculateSpeedRatio(55, 60), 0.92);
  assert.strictEqual(trafficService.calculateSpeedRatio(42, 60), 0.70);
  assert.strictEqual(trafficService.calculateSpeedRatio(24, 60), 0.40);

  // Divide-by-zero protection
  assert.strictEqual(trafficService.calculateSpeedRatio(40, 0), 1.0, 'Zero freeFlowSpeed must safely return 1.0 without throwing');
  assert.strictEqual(trafficService.calculateSpeedRatio(40, -10), 1.0, 'Negative freeFlowSpeed must safely return 1.0 without throwing');

  // Negative current speed normalization
  assert.strictEqual(trafficService.calculateSpeedRatio(-15, 60), 0.0, 'Negative currentSpeed must clamp to 0.0');
  console.log('✔ Speed ratio calculation verified with divide-by-zero and negative speed protection');

  // --------------------------------------------------
  // 3. CONGESTION CLASSIFICATION TESTS
  // --------------------------------------------------
  console.log('\n--- 3. Congestion Level Classification Tests ---');

  assert.strictEqual(trafficService.getCongestionLevel(0.92), 'LOW', 'Ratio 0.92 must be LOW');
  assert.strictEqual(trafficService.getCongestionLevel(0.80), 'LOW', 'Ratio 0.80 must be LOW (boundary)');
  assert.strictEqual(trafficService.getCongestionLevel(0.79), 'MODERATE', 'Ratio 0.79 must be MODERATE');
  assert.strictEqual(trafficService.getCongestionLevel(0.50), 'MODERATE', 'Ratio 0.50 must be MODERATE (boundary)');
  assert.strictEqual(trafficService.getCongestionLevel(0.49), 'HIGH', 'Ratio 0.49 must be HIGH');
  assert.strictEqual(trafficService.getCongestionLevel(0.20), 'HIGH', 'Ratio 0.20 must be HIGH');
  console.log('✔ Speed ratio thresholds correctly mapped: >=0.80 LOW, 0.50..0.79 MODERATE, <0.50 HIGH');

  // --------------------------------------------------
  // 4. TRAFFIC RISK SCORE TESTS
  // --------------------------------------------------
  console.log('\n--- 4. Traffic Risk Score & Delay Penalty Tests ---');

  const lowRisk = trafficService.calculateTrafficRiskScore({ congestionLevel: 'LOW', travelDelaySeconds: 30 });
  assert.strictEqual(lowRisk, 20, 'Base LOW traffic risk must be 20');

  const modRisk = trafficService.calculateTrafficRiskScore({ congestionLevel: 'MODERATE', travelDelaySeconds: 120 });
  assert.strictEqual(modRisk, 60, 'Base MODERATE traffic risk must be 60');

  const highRisk = trafficService.calculateTrafficRiskScore({ congestionLevel: 'HIGH', travelDelaySeconds: 120 });
  assert.strictEqual(highRisk, 90, 'Base HIGH traffic risk must be 90');

  // Delay modifier test
  const severeDelayRisk = trafficService.calculateTrafficRiskScore({ congestionLevel: 'HIGH', travelDelaySeconds: 700 });
  assert.strictEqual(severeDelayRisk, 90 + 10, 'Delay >600s must add +10 penalty, clamped to 100');
  console.log('✔ Traffic risk scores match required values: LOW=20, MODERATE=60, HIGH=90 (clamped 0..100)');

  // --------------------------------------------------
  // 5. PROVIDER ABSTRACTION & SOURCE IDENTIFICATION
  // --------------------------------------------------
  console.log('\n--- 5. Provider Abstraction & Source Transparency ---');

  const simTraffic = trafficService.getSimulatedTraffic(18.5204, 73.8567, 'MODERATE');
  assert.strictEqual(simTraffic.source, 'SIMULATED', 'Simulated provider must clearly return source: SIMULATED');
  assert.strictEqual(simTraffic.congestionLevel, 'MODERATE');
  assert(simTraffic.observedAt, 'observedAt timestamp must exist');
  console.log(`✔ Provider Transparency: source="${simTraffic.source}", segment="${simTraffic.roadSegmentId}"`);

  // --------------------------------------------------
  // 6. DYNAMIC TRAFFIC TRANSITION TESTS (LOW -> MODERATE -> HIGH -> LOW)
  // --------------------------------------------------
  console.log('\n--- 6. Dynamic Traffic State Transitions ---');

  // Scenario A: Morning free-flow (LOW)
  const scA = trafficService.getSimulatedTraffic(18.5204, 73.8567, 'LOW');
  assert.strictEqual(scA.congestionLevel, 'LOW');
  const riskA = trafficService.calculateTrafficRiskScore(scA);
  assert.strictEqual(riskA, 20);
  console.log(`✔ Scenario A (Free-Flow): Speed=${scA.currentSpeedKmh}/${scA.freeFlowSpeedKmh} -> Level=${scA.congestionLevel}, Risk=${riskA}`);

  // Scenario B: Midday buildup (MODERATE)
  const scB = trafficService.getSimulatedTraffic(18.5204, 73.8567, 'MODERATE');
  assert.strictEqual(scB.congestionLevel, 'MODERATE');
  const riskB = trafficService.calculateTrafficRiskScore(scB);
  assert.strictEqual(riskB, 60);
  console.log(`✔ Scenario B (Midday Buildup): Speed=${scB.currentSpeedKmh}/${scB.freeFlowSpeedKmh} -> Level=${scB.congestionLevel}, Risk=${riskB}`);

  // Scenario C: Evening peak rush (HIGH)
  const scC = trafficService.getSimulatedTraffic(18.5204, 73.8567, 'HIGH');
  assert.strictEqual(scC.congestionLevel, 'HIGH');
  const riskC = trafficService.calculateTrafficRiskScore(scC);
  assert(riskC >= 90);
  console.log(`✔ Scenario C (Rush Hour Peak): Speed=${scC.currentSpeedKmh}/${scC.freeFlowSpeedKmh} -> Level=${scC.congestionLevel}, Risk=${riskC}`);

  // Scenario D: Night clearing (returns to LOW)
  const scD = trafficService.getSimulatedTraffic(18.5204, 73.8567, 'LOW');
  assert.strictEqual(scD.congestionLevel, 'LOW');
  const riskD = trafficService.calculateTrafficRiskScore(scD);
  assert.strictEqual(riskD, 20);
  console.log(`✔ Scenario D (Traffic Cleared): Speed=${scD.currentSpeedKmh}/${scD.freeFlowSpeedKmh} -> Level=${scD.congestionLevel}, Risk=${riskD}`);
  assert(riskD < riskC, 'Traffic risk must dynamically drop when congestion clears');

  // --------------------------------------------------
  // 7. IN-MEMORY CACHING TESTS
  // --------------------------------------------------
  console.log('\n--- 7. In-Memory Caching Tests ---');

  const firstCall = await trafficService.getCurrentTraffic(18.5204, 73.8567, 'LOW');
  assert.strictEqual(firstCall.isCached, false, 'First call must be a fresh computation');

  const secondCall = await trafficService.getCurrentTraffic(18.5204, 73.8567, 'LOW');
  assert.strictEqual(secondCall.isCached, true, 'Immediate second call must be served from cache');
  console.log('✔ Second request served from 3-minute in-memory cache (isCached: true)');

  // --------------------------------------------------
  // 8. CONTEXTUAL EXPLANATIONS
  // --------------------------------------------------
  console.log('\n--- 8. Contextual Explanations ---');

  assert.strictEqual(trafficService.getTrafficExplanation({ congestionLevel: 'HIGH' }), 'Heavy traffic is increasing road risk.');
  assert.strictEqual(trafficService.getTrafficExplanation({ congestionLevel: 'MODERATE' }), 'Moderate traffic may increase travel risk.');
  assert.strictEqual(trafficService.getTrafficExplanation({ congestionLevel: 'LOW' }), 'Traffic is currently light.');
  console.log('✔ Human-readable explanations generated dynamically for all congestion tiers');

  console.log('\n==================================================');
  console.log('  ✅ ALL TRAFFIC SERVICE TESTS PASSED SUCCESSFULLY');
  console.log('==================================================\n');
}

testTrafficEngine().catch((err) => {
  console.error('\n❌ Traffic Engine Test Failed:', err);
  process.exit(1);
});
