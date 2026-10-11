const assert = require('assert');
const riskService = require('./src/services/riskService');
const RiskConfig = require('./src/config/riskConfig');

function testRiskEngine() {
  console.log('==================================================');
  console.log('  SMARTMOVE AI — Risk Engine Unit & Scenario Tests');
  console.log('==================================================\n');

  // --------------------------------------------------
  // 1. BOUNDARY & CLASSIFICATION TESTS: getRiskLevel()
  // --------------------------------------------------
  console.log('--- 1. Boundary & Classification Tests ---');

  assert.strictEqual(riskService.getRiskLevel(0), 'LOW', 'Score 0 must be LOW');
  console.log('✔ Score 0 -> LOW');

  assert.strictEqual(riskService.getRiskLevel(30), 'LOW', 'Score 30 must be LOW');
  console.log('✔ Score 30 -> LOW (Upper bound of LOW)');

  assert.strictEqual(riskService.getRiskLevel(31), 'MEDIUM', 'Score 31 must be MEDIUM');
  console.log('✔ Score 31 -> MEDIUM (Lower bound of MEDIUM)');

  assert.strictEqual(riskService.getRiskLevel(60), 'MEDIUM', 'Score 60 must be MEDIUM');
  console.log('✔ Score 60 -> MEDIUM (Upper bound of MEDIUM)');

  assert.strictEqual(riskService.getRiskLevel(61), 'HIGH', 'Score 61 must be HIGH');
  console.log('✔ Score 61 -> HIGH (Lower bound of HIGH)');

  assert.strictEqual(riskService.getRiskLevel(100), 'HIGH', 'Score 100 must be HIGH');
  console.log('✔ Score 100 -> HIGH');

  assert.strictEqual(riskService.getRiskLevel(-15), 'LOW', 'Negative score must clamp to LOW');
  console.log('✔ Score -15 -> LOW (Clamped)');

  assert.strictEqual(riskService.getRiskLevel(150), 'HIGH', 'Over-100 score must clamp to HIGH');
  console.log('✔ Score 150 -> HIGH (Clamped)');

  // --------------------------------------------------
  // 2. FACTOR FUNCTIONS TESTING
  // --------------------------------------------------
  console.log('\n--- 2. Factor Functions Testing ---');

  // Traffic factor
  assert.strictEqual(riskService.calculateTrafficRisk('LOW'), 20);
  assert.strictEqual(riskService.calculateTrafficRisk('MODERATE'), 60);
  assert.strictEqual(riskService.calculateTrafficRisk('HIGH'), 90);
  console.log('✔ Traffic Risk: LOW=20, MODERATE=60, HIGH=90');

  // Weather factor
  assert.strictEqual(riskService.calculateWeatherRisk('CLEAR'), 10);
  assert.strictEqual(riskService.calculateWeatherRisk('CLOUDY'), 20);
  assert.strictEqual(riskService.calculateWeatherRisk('RAIN'), 55);
  assert.strictEqual(riskService.calculateWeatherRisk('HEAVY_RAIN'), 80);
  assert.strictEqual(riskService.calculateWeatherRisk('FOG'), 75);
  console.log('✔ Weather Risk: CLEAR=10, CLOUDY=20, RAIN=55, HEAVY_RAIN=80, FOG=75');

  // Braking factor
  assert.strictEqual(riskService.calculateBrakingRisk(0), 10);
  assert.strictEqual(riskService.calculateBrakingRisk(1), 40);
  assert.strictEqual(riskService.calculateBrakingRisk(4), 70);
  assert.strictEqual(riskService.calculateBrakingRisk(8), 90);
  console.log('✔ Braking Risk: 0=10, 1=40, 4=70, 8+=90');

  // Hazard factor calculation & distance decay
  const noHazards = riskService.calculateHazardRisk([]);
  assert.strictEqual(noHazards, 0, 'No active hazards should result in 0 hazard risk');

  const closeHighHazard = riskService.calculateHazardRisk([
    { severity: 'HIGH', status: 'ACTIVE', distanceMeters: 100 }
  ]);
  const farHighHazard = riskService.calculateHazardRisk([
    { severity: 'HIGH', status: 'ACTIVE', distanceMeters: 2500 }
  ]);
  assert(closeHighHazard > farHighHazard, 'Closer hazard must produce higher risk than far hazard');
  console.log(`✔ Distance awareness: 100m HIGH hazard (${closeHighHazard}) > 2500m HIGH hazard (${farHighHazard})`);

  // --------------------------------------------------
  // 3. SCENARIO TESTS
  // --------------------------------------------------
  console.log('\n--- 3. Scenario Tests ---');

  // Scenario 1: Optimal road conditions
  const s1 = riskService.calculateRisk({
    hazardRisk: 0,
    trafficRisk: riskService.calculateTrafficRisk('LOW'),
    weatherRisk: riskService.calculateWeatherRisk('CLEAR'),
    brakingRisk: riskService.calculateBrakingRisk(0),
    eventRisk: riskService.calculateEventRisk(0),
  });
  console.log(`✔ Scenario 1 (Optimal): Score=${s1.score} -> Level=${s1.level}`);
  assert.strictEqual(s1.level, 'LOW', 'Scenario 1 should be LOW risk');
  assert(s1.score <= 30, `Score ${s1.score} must be <= 30`);

  // Scenario 2: Moderate traffic, rain, medium hazard
  const s2 = riskService.calculateRisk({
    hazardRisk: 60,
    trafficRisk: riskService.calculateTrafficRisk('MODERATE'),
    weatherRisk: riskService.calculateWeatherRisk('RAIN'),
    brakingRisk: riskService.calculateBrakingRisk(2),
    eventRisk: riskService.calculateEventRisk(1),
  });
  console.log(`✔ Scenario 2 (Moderate): Score=${s2.score} -> Level=${s2.level}`);
  assert.strictEqual(s2.level, 'MEDIUM', 'Scenario 2 should be MEDIUM risk');
  assert(s2.score > 30 && s2.score <= 60, `Score ${s2.score} must be between 31 and 60`);

  // Scenario 3: High hazard, heavy traffic, heavy rain, multiple hard braking events
  const s3 = riskService.calculateRisk({
    hazardRisk: 90,
    trafficRisk: riskService.calculateTrafficRisk('HIGH'),
    weatherRisk: riskService.calculateWeatherRisk('HEAVY_RAIN'),
    brakingRisk: riskService.calculateBrakingRisk(6),
    eventRisk: riskService.calculateEventRisk(3),
  });
  console.log(`✔ Scenario 3 (Severe): Score=${s3.score} -> Level=${s3.level}`);
  assert.strictEqual(s3.level, 'HIGH', 'Scenario 3 should be HIGH risk');
  assert(s3.score > 60, `Score ${s3.score} must be > 60`);

  // Scenario 4: Hazard Resolution Dynamic Effect
  // Before resolution: Active HIGH hazard + Moderate traffic + Rain
  const activeHazardList = [
    { severity: 'HIGH', status: 'ACTIVE', distanceMeters: 300 }
  ];
  const hazardScoreBefore = riskService.calculateHazardRisk(activeHazardList);
  const riskBefore = riskService.calculateRisk({
    hazardRisk: hazardScoreBefore,
    trafficRisk: riskService.calculateTrafficRisk('MODERATE'),
    weatherRisk: riskService.calculateWeatherRisk('CLEAR'),
    brakingRisk: riskService.calculateBrakingRisk(1),
    eventRisk: riskService.calculateEventRisk(0),
  });

  // After resolution: Hazard transitions to RESOLVED
  const resolvedHazardList = [
    { severity: 'HIGH', status: 'RESOLVED', distanceMeters: 300 }
  ];
  const hazardScoreAfter = riskService.calculateHazardRisk(resolvedHazardList);
  assert.strictEqual(hazardScoreAfter, 0, 'Resolved hazard must produce 0 contribution');

  const riskAfter = riskService.calculateRisk({
    hazardRisk: hazardScoreAfter,
    trafficRisk: riskService.calculateTrafficRisk('MODERATE'),
    weatherRisk: riskService.calculateWeatherRisk('CLEAR'),
    brakingRisk: riskService.calculateBrakingRisk(1),
    eventRisk: riskService.calculateEventRisk(0),
  });

  console.log(`✔ Scenario 4 (Hazard Resolution): Before=${riskBefore.score} -> After=${riskAfter.score}`);
  assert(riskAfter.score < riskBefore.score, 'Risk must decrease when hazard is marked RESOLVED');

  console.log('\n==================================================');
  console.log('  ✅ ALL RISK ENGINE UNIT & SCENARIO TESTS PASSED');
  console.log('==================================================\n');
}

testRiskEngine();
